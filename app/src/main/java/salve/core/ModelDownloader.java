package salve.core;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Descarga modelos definidos en assets/config/models.json a:
 *   /Android/data/<pkg>/files/models/<filename>
 *
 * - Reanudación por Range
 * - Redirecciones 30x (incl. 307/308)
 * - Evita re-descargar si ya existe (>=1MB)
 * - Overlay de progreso opcional
 * - Checksum SHA-256 (streaming)
 * - Chequeo de espacio libre con margen
 * - Asíncrono con cancelación y reintentos
 */
@SuppressLint("StaticFieldLeak")
public class ModelDownloader {

    private static final String TAG = "ModelDownloader";
    private static final int BUF = 256 * 1024; // 256 KiB
    private static final int HTTP_REQUESTED_RANGE_NOT_SATISFIABLE = 416;

    // Verificación / espacio
    private static final boolean DELETE_ON_BAD_CHECKSUM = true;
    private static final long   MIN_MARGIN_BYTES = 50L << 20; // 50 MB
    private static final double MARGIN_RATIO     = 0.05;      // +5%

    // Red / reintentos
    private static final int CONNECT_TIMEOUT_MS = 25000;
    private static final int READ_TIMEOUT_MS    = 25000;
    private static final int MAX_REDIRECTS      = 5;
    private static final int MAX_RETRIES        = 3;     // NUEVO
    private static final long BASE_BACKOFF_MS   = 1200;  // NUEVO

    // ===== Overlay auto (opcional) =====
    private static WeakReference<Activity> sOverlayActivity;
    private static Overlay sOverlay;

    /** Habilita la barra temporal arriba de Salve. Llama esto (p.ej. en onCreate). */
    public static void enableAutoOverlay(Activity activity) {
        sOverlayActivity = new WeakReference<>(activity);
        if (sOverlay == null) sOverlay = new Overlay(activity);
    }

    /** Deshabilita/oculta la barra de progreso si estuviera visible. */
    public static void disableAutoOverlay() {
        if (sOverlay != null) sOverlay.hideImmediate();
        sOverlay = null;
        sOverlayActivity = null;
    }

    // ===== Listener =====
    public interface Listener {
        void onProgress(String id, int percent);
        void onComplete(String id, File file);
        void onError(String id, Exception e);
        default void onDetail(String id, int index, int total, long bytes, long totalBytes, double mbPerSec) {}
        default void onAllDone() {}
    }

    // ===== NUEVO: Task handle para cancelación =====
    public static class TaskHandle {
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        public void cancel() { cancelled.set(true); }
        private boolean isCancelled() { return cancelled.get(); }
    }

    // ===== NUEVO: Ejecutores =====
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    // === helpers de destino unificado ===
    private static File modelsDir(android.content.Context ctx) {
        return ModelStore.dir(ctx); // /Android/data/<pkg>/files/models
    }

    private static File outputFor(android.content.Context ctx, String fileNameFromUrl) {
        return new File(modelsDir(ctx), fileNameFromUrl);
    }

    private static String fileNameFromUrl(String url, String fallbackId) {
        try {
            String path = new URL(url).getPath();
            int s = path.lastIndexOf('/');
            String name = (s >= 0 ? path.substring(s + 1) : path);
            if (name == null || name.trim().isEmpty()) {
                return fallbackId + ".gguf";
            }
            return name;
        } catch (Exception e) {
            return fallbackId + ".gguf";
        }
    }

    // ===== NUEVO: precheck para saber si falta algo sin descargar =====
    public static boolean precheckAll(android.content.Context ctx, InputStream jsonStream) {
        try {
            byte[] raw = readAll(jsonStream);
            JSONObject root = new JSONObject(new String(raw));
            JSONArray items = root.getJSONArray("items");
            for (int i = 0; i < items.length(); i++) {
                JSONObject it = items.getJSONObject(i);
                String id       = it.getString("id");
                String url      = it.getString("url");
                String filename = it.optString("filename", null);
                if (filename == null || filename.trim().isEmpty()) filename = fileNameFromUrl(url, id);
                long   size     = it.optLong("sizeBytes", -1L);
                String sha256   = optStringOrNull(it, "sha256");

                File out = outputFor(ctx, filename);
                if (!out.exists() || out.length() <= (1L << 20)) return true; // falta
                if (sha256 != null) {
                    String got = sha256Hex(out);
                    if (!equalsHex(sha256, got)) return true; // corrupto
                } else if (size > 0 && out.length() != size) {
                    return true; // tamaño no coincide
                }
            }
            return false; // todo presente y válido
        } catch (Exception e) {
            Log.w(TAG, "precheckAll error: " + e.getMessage());
            return true; // ante duda, indicar que puede faltar algo
        }
    }

    /** Sincrónico (compat). Mantengo tu firma. */
    public static void downloadAll(android.content.Context ctx, InputStream jsonStream, Listener cb) {
        TaskHandle th = new TaskHandle();
        downloadAllInternal(ctx, jsonStream, cb, th, false);
    }

    /** NUEVO: Asíncrono con cancelación. */
    public static TaskHandle downloadAllAsync(android.content.Context ctx, InputStream jsonStream, Listener cb) {
        TaskHandle th = new TaskHandle();
        EXEC.execute(() -> downloadAllInternal(ctx, jsonStream, cb, th, true));
        return th;
    }

    private static void downloadAllInternal(android.content.Context ctx, InputStream jsonStream, Listener cb,
                                            TaskHandle th, boolean asyncCaller) {
        try {
            byte[] raw = readAll(jsonStream);
            JSONObject root = new JSONObject(new String(raw));
            JSONArray items = root.getJSONArray("items");

            File base = modelsDir(ctx);
            if (!base.exists() && !base.mkdirs()) {
                throw new RuntimeException("No se pudo crear carpeta: " + base.getAbsolutePath());
            }

            final int total = items.length();
            ensureOverlayAttachedIfEnabled();
            if (sOverlay != null) sOverlay.show();

            for (int i = 0; i < total; i++) {
                if (th.isCancelled()) break;

                JSONObject it = items.getJSONObject(i);
                String id       = it.getString("id");
                String url      = it.getString("url");
                String filename = it.optString("filename", null);
                if (filename == null || filename.trim().isEmpty()) filename = fileNameFromUrl(url, id);
                long   expectedSize = it.optLong("sizeBytes", -1L);
                String sha256       = optStringOrNull(it, "sha256");

                File out = outputFor(ctx, filename);
                downloadOneWithRetries(url, out, id, i + 1, total, expectedSize, sha256, cb, th);
            }

            if (!th.isCancelled()) runMain(() -> { if (cb != null) cb.onAllDone(); });
            if (sOverlay != null) sOverlay.hideSmooth();
        } catch (Exception e) {
            Log.e(TAG, "Error en downloadAll", e);
            runMain(() -> {
                if (cb != null) cb.onError("all", e);
                if (sOverlay != null) sOverlay.setError("Error descargando modelos");
            });
        }
    }

    // ===== NUEVO: reintentos con backoff =====
    private static void downloadOneWithRetries(String urlStr, File outFile, String id, int index, int total,
                                               long expectedTotalBytes, String expectedSha256,
                                               Listener cb, TaskHandle th) {
        int attempt = 0;
        while (attempt < MAX_RETRIES && !th.isCancelled()) {
            try {
                downloadOne(urlStr, outFile, id, index, total, expectedTotalBytes, expectedSha256, cb, th);
                return; // ok
            } catch (Exception e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    Log.e(TAG, "Fallo definitivo " + id + ": " + e.getMessage(), e);
                    final Exception ex = e;
                    runMain(() -> {
                        if (cb != null) cb.onError(id, ex);
                        if (sOverlay != null) sOverlay.setError("Error en " + id);
                    });
                    return;
                }
                long sleep = (long) (BASE_BACKOFF_MS * Math.pow(2, attempt - 1));
                Log.w(TAG, "Reintentando " + id + " intento " + (attempt + 1) + "/" + MAX_RETRIES + " en " + sleep + "ms");
                try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
            }
        }
    }

    /** Descarga 1 archivo (posiblemente con reanudación), checksum streaming y archivo .part */
    private static void downloadOne(String urlStr, File outFile, String id, int index, int total,
                                    long expectedTotalBytes, String expectedSha256, Listener cb, TaskHandle th) throws Exception {

        // Si ya existe con tamaño razonable, valida y devuelve
        if (outFile.exists() && outFile.length() > (1L << 20)) {
            if (expectedSha256 != null) {
                if (sOverlay != null) sOverlay.setTitle("Verificando archivo…");
                String got = sha256Hex(outFile);
                if (!equalsHex(expectedSha256, got)) {
                    Log.w(TAG, "Checksum inválido en existente (" + id + ")");
                    if (DELETE_ON_BAD_CHECKSUM) outFile.delete();
                } else {
                    final File f = outFile;
                    runMain(() -> {
                        if (cb != null) cb.onComplete(id, f);
                        if (sOverlay != null) sOverlay.setSnapshot(id, index, total, f.length(), f.length(), 0);
                    });
                    return;
                }
            } else {
                final File f = outFile;
                runMain(() -> {
                    if (cb != null) cb.onComplete(id, f);
                    if (sOverlay != null) sOverlay.setSnapshot(id, index, total, f.length(), f.length(), 0);
                });
                return;
            }
        }

        // Asegurar carpeta
        File parent = outFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new RuntimeException("No se pudo crear carpeta: " + parent.getAbsolutePath());
        }

        // Archivo temporal .part para atomicidad
        File part = new File(outFile.getAbsolutePath() + ".part");

        long existing = part.exists() ? part.length() : 0L;

        // Espacio libre si conocemos tamaño
        if (expectedTotalBytes > 0) {
            long need   = Math.max(0, expectedTotalBytes - existing);
            long margin = Math.max(MIN_MARGIN_BYTES, (long) (expectedTotalBytes * MARGIN_RATIO));
            long free   = getAvailableBytes(parent != null ? parent : outFile);
            if (free < need + margin) {
                String msg = String.format(Locale.US,
                        "Espacio insuficiente: libre=%s, requerido=%s (+%s margen)",
                        human(free), human(need), human(margin));
                if (sOverlay != null) sOverlay.setError("Espacio insuficiente");
                throw new RuntimeException(msg);
            }
        }

        String effectiveUrl = urlStr;
        int redirects = 0;

        while (redirects < MAX_REDIRECTS && !th.isCancelled()) {
            HttpURLConnection conn = (HttpURLConnection) new URL(effectiveUrl).openConnection();
            try {
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) SalveDownloader/1.3");
                conn.setRequestProperty("Accept", "*/*");
                if (existing > 0) conn.setRequestProperty("Range", "bytes=" + existing + "-");

                int code = conn.getResponseCode();
                Log.d(TAG, id + " -> " + effectiveUrl + " [HTTP " + code + "]");

                // Redirecciones
                if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                        code == HttpURLConnection.HTTP_MOVED_TEMP ||
                        code == HttpURLConnection.HTTP_SEE_OTHER ||
                        code == 307 || code == 308) {
                    String loc = conn.getHeaderField("Location");
                    if (loc == null) throw new RuntimeException("Redirección sin Location");
                    effectiveUrl = resolveRedirect(effectiveUrl, loc);
                    redirects++;
                    continue;
                }

                // 416 -> rango inválido, reiniciar limpio
                if (code == HTTP_REQUESTED_RANGE_NOT_SATISFIABLE) {
                    Log.w(TAG, "HTTP 416: Range inválido; reiniciando limpio.");
                    if (part.exists() && !part.delete()) {
                        Log.w(TAG, "No se pudo borrar parcial 416: " + part.getAbsolutePath());
                    }
                    existing = 0;
                    effectiveUrl = urlStr;
                    redirects++;
                    continue;
                }

                // Si pedimos Range y el server responde 200 (ignora Range) → reiniciar limpio
                if (existing > 0 && code == HttpURLConnection.HTTP_OK) {
                    Log.w(TAG, "Servidor ignoró Range. Reiniciando descarga limpia.");
                    if (part.exists() && !part.delete()) {
                        Log.w(TAG, "No se pudo borrar parcial: " + part.getAbsolutePath());
                    }
                    existing = 0;
                }

                if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
                    String body = readHttpError(conn);
                    throw new RuntimeException(
                            "HTTP " + code + " al pedir " + effectiveUrl + (body != null ? (" | " + body) : "")
                    );
                }

                long contentLen = conn.getContentLengthLong();
                long totalBytes = (contentLen > 0 ? contentLen + existing :
                        (expectedTotalBytes > 0 ? expectedTotalBytes : -1));

                // Progreso indeterminado si no sabemos total
                final boolean indeterminate = totalBytes <= 0;
                if (sOverlay != null) {
                    sOverlay.setTitle("Descargando modelos LLM…");
                }

                // Abrir flujo entrada
                try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
                     BufferedOutputStream out = new BufferedOutputStream(
                             new java.io.FileOutputStream(part, existing > 0))) {

                    // SHA256 streaming si lo tenemos
                    MessageDigest md = null;
                    if (expectedSha256 != null && existing == 0) {
                        md = MessageDigest.getInstance("SHA-256");
                    }

                    byte[] buf = new byte[BUF];
                    long written = existing;
                    int read, last = -1;

                    long lastTick = System.nanoTime();
                    long lastBytes = written;

                    while ((read = in.read(buf)) != -1) {
                        if (th.isCancelled()) throw new InterruptedException("cancelled");
                        out.write(buf, 0, read);
                        if (md != null) md.update(buf, 0, read);
                        written += read;

                        if (!indeterminate && written > 0 && totalBytes > 0) {
                            int p = (int) Math.min(100, (written * 100 / totalBytes));
                            if (p != last) {
                                final int fp = p;
                                last = p;
                                runMain(() -> { if (cb != null) cb.onProgress(id, fp); });
                            }
                        }

                        long now = System.nanoTime();
                        if ((now - lastTick) >= 500_000_000L) {
                            long dBytes = written - lastBytes;
                            double mbps = (dBytes / 1024.0 / 1024.0) / ((now - lastTick) / 1_000_000_000.0);
                            final long fWritten = written, fTotal = totalBytes;
                            runMain(() -> {
                                if (cb != null) cb.onDetail(id, index, total, fWritten, fTotal, mbps);
                                if (sOverlay != null) sOverlay.update(id, index, total, fWritten, fTotal, mbps);
                            });
                            lastTick = now;
                            lastBytes = written;
                        }
                    }
                    out.flush();

                    // Si no pudimos hacer streaming digest (porque reanudado) hacemos verificación al final
                    if (expectedSha256 != null) {
                        String got;
                        if (md != null && existing == 0) {
                            got = toHex(md.digest());
                        } else {
                            // reanudado: no podemos confiar en digest parcial → calcula sobre .part
                            got = sha256Hex(part);
                        }
                        if (!equalsHex(expectedSha256, got)) {
                            String msg = "Checksum inválido (" + id + ") expected=" + expectedSha256 + " got=" + got;
                            if (sOverlay != null) sOverlay.setError("Checksum inválido");
                            if (DELETE_ON_BAD_CHECKSUM) part.delete();
                            throw new RuntimeException(msg);
                        }
                    }

                    // Mover .part -> definitivo (atómico en mismo volumen)
                    if (outFile.exists() && !outFile.delete()) {
                        throw new RuntimeException("No se pudo reemplazar archivo final");
                    }
                    if (!part.renameTo(outFile)) {
                        throw new RuntimeException("No se pudo mover parcial a destino");
                    }

                    final File f = outFile;
                    runMain(() -> {
                        if (cb != null) cb.onComplete(id, f);
                        if (sOverlay != null) sOverlay.update(id, index, total,
                                f.length(), (totalBytes > 0 ? totalBytes : f.length()), 0);
                    });
                    return; // listo 1 ítem
                }
            } finally {
                try { conn.disconnect(); } catch (Exception ignore) {}
            }
        }
        if (th.isCancelled()) throw new InterruptedException("cancelled");
        throw new RuntimeException("Demasiadas redirecciones");
    }

    /** Resuelve Location relativo/absoluto contra la URL base. */
    private static String resolveRedirect(String base, String loc) {
        try {
            URL baseUrl = new URL(base);
            URL newUrl  = new URL(baseUrl, loc);
            return newUrl.toString();
        } catch (Exception e) {
            return loc;
        }
    }

    /** Lee body de error (acorta HTML largos para el log). */
    private static String readHttpError(HttpURLConnection conn) {
        try (InputStream es = conn.getErrorStream()) {
            if (es == null) return null;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int n;
            while ((n = es.read(b)) > 0) bos.write(b, 0, n);
            String s = bos.toString();
            if (s.length() > 300) s = s.substring(0, 300) + "…";
            return s.replaceAll("\\s+", " ").trim();
        } catch (Exception ignore) {
            return null;
        }
    }

    /** Lee completamente un InputStream a byte[]. */
    private static byte[] readAll(InputStream is) throws Exception {
        try (InputStream in = is; ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        }
    }

    // ===== Utils: checksum, espacio, hex, human =====
    private static String sha256Hex(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (DigestInputStream dis = new DigestInputStream(new FileInputStream(file), md)) {
            byte[] b = new byte[BUF];
            while (dis.read(b) != -1) { /* consume */ }
        }
        return toHex(md.digest());
    }

    private static String toHex(byte[] digest) {
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte x : digest) sb.append(String.format(Locale.US, "%02x", x));
        return sb.toString();
    }

    private static boolean equalsHex(String a, String b) {
        if (a == null || b == null) return false;
        return a.replace(" ", "").trim().equalsIgnoreCase(b.replace(" ", "").trim());
    }

    private static long getAvailableBytes(File path) {
        try {
            StatFs stat = new StatFs(path.getAbsolutePath());
            long blk  = stat.getAvailableBlocksLong();
            long size = stat.getBlockSizeLong();
            return blk * size;
        } catch (Throwable t) {
            return Long.MAX_VALUE;
        }
    }

    private static String human(long bytes) {
        if (bytes <= 0 || bytes == Long.MAX_VALUE) return "?";
        double mb = bytes / (1024.0 * 1024.0);
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb);
        double gb = mb / 1024.0;
        return String.format(Locale.US, "%.2f GB", gb);
    }

    private static String optStringOrNull(JSONObject o, String key) {
        String v = o.optString(key, null);
        return (v != null && v.length() > 0) ? v : null;
    }

    // ===== Overlay inline =====
    private static class Overlay {
        private final Handler main = new Handler(Looper.getMainLooper());
        private final WeakReference<Activity> actRef;

        private FrameLayout root;
        private View card;
        private TextView tvTitle, tvDetail, tvNumbers;
        private ProgressBar bar;
        private boolean attached = false;

        Overlay(Activity activity) { this.actRef = new WeakReference<>(activity); }

        void ensure(ViewGroup parent) {
            if (attached) return;

            root = new FrameLayout(parent.getContext());
            FrameLayout.LayoutParams rootLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            root.setLayoutParams(rootLp);

            card = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            card.setLayoutParams(cardLp);

            FrameLayout cardInner = new FrameLayout(parent.getContext());
            cardInner.setPadding(dp(12), dp(12), dp(12), dp(12));
            cardInner.setBackgroundColor(0xCC121212);

            tvTitle = new TextView(parent.getContext());
            tvTitle.setText("Descargando modelos LLM…");
            tvTitle.setTextSize(14);
            tvTitle.setPadding(0, 0, 0, dp(2));
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

            tvDetail = new TextView(parent.getContext());
            tvDetail.setTextSize(12);
            tvDetail.setPadding(0, dp(18), 0, dp(2));

            bar = new ProgressBar(parent.getContext(), null, android.R.attr.progressBarStyleHorizontal);
            FrameLayout.LayoutParams barLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(10));
            bar.setLayoutParams(barLp);
            bar.setMax(100);

            tvNumbers = new TextView(parent.getContext());
            tvNumbers.setTextSize(11);
            FrameLayout.LayoutParams numsLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            numsLp.topMargin = dp(18 + 10 + 6);
            tvNumbers.setLayoutParams(numsLp);

            cardInner.addView(tvTitle);
            cardInner.addView(bar);
            cardInner.addView(tvDetail);
            cardInner.addView(tvNumbers);

            root.addView(cardInner);
            parent.addView(root, 0); // arriba
            root.setVisibility(View.GONE);
            attached = true;
        }

        void show() { main.post(() -> {
            Activity a = actRef.get();
            if (a == null) return;
            ViewGroup parent = a.findViewById(android.R.id.content);
            ensure(parent);
            if (root.getVisibility() != View.VISIBLE) {
                root.setVisibility(View.VISIBLE);
                AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
                fadeIn.setDuration(200);
                root.startAnimation(fadeIn);
            }
        }); }

        void hideSmooth() { main.post(() -> {
            if (!attached) return;
            AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
            fadeOut.setDuration(250);
            root.startAnimation(fadeOut);
            main.postDelayed(() -> root.setVisibility(View.GONE), 260);
        }); }

        void hideImmediate() { main.post(() -> { if (attached) root.setVisibility(View.GONE); }); }

        void setTitle(String t) { main.post(() -> { if (tvTitle != null) tvTitle.setText(t); }); }
        void setError(String t) { main.post(() -> { if (tvTitle != null) tvTitle.setText("⚠ " + t); }); }

        void setSnapshot(String id, int index, int total, long bytes, long totalBytes, double mbps) {
            update(id, index, total, bytes, totalBytes, mbps);
        }

        void update(String id, int index, int total, long bytes, long totalBytes, double mbps) {
            main.post(() -> {
                show();
                if (tvDetail != null) tvDetail.setText("Modelo: " + id + " (" + index + "/" + total + ")");
                int pct = (totalBytes > 0) ? (int) Math.min(100, (bytes * 100 / totalBytes)) : 0;
                if (bar != null) {
                    bar.setIndeterminate(totalBytes <= 0);
                    bar.setProgress(pct);
                }
                if (tvNumbers != null) tvNumbers.setText(human(bytes) + " / " + human(totalBytes) +
                        "  •  " + (totalBytes > 0 ? (pct + "%") : "…") +
                        "  •  " + (mbps > 0 ? String.format(Locale.US, "%.1f MB/s", mbps) : ""));
            });
        }

        private int dp(int v) {
            Activity a = actRef.get();
            if (a == null) return v;
            float d = a.getResources().getDisplayMetrics().density;
            return (int) (v * d);
        }
    }

    private static void ensureOverlayAttachedIfEnabled() {
        if (sOverlay == null && sOverlayActivity != null) {
            Activity a = sOverlayActivity.get();
            if (a != null) sOverlay = new Overlay(a);
        }
        if (sOverlay != null && sOverlayActivity != null) {
            Activity a = sOverlayActivity.get();
            if (a != null) {
                ViewGroup parent = a.findViewById(android.R.id.content);
                sOverlay.ensure(parent);
            }
        }
    }

    private static void runMain(Runnable r) { MAIN.post(r); }
}
