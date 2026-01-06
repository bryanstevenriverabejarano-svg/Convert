package salve.presentation.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.annotation.SuppressLint;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.salve.app.R;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.BreakIterator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import salve.core.DiarioSecreto;
import salve.core.GrafoRecuerdos;
import salve.core.MemoriaEmocional;
import salve.core.ModelConsoleOverlay;
import salve.core.ModelDownloader;
import salve.core.ModelStore;
import salve.core.MotorConversacional;
import salve.core.PdfGenerator;
import salve.core.ReconocimientoFacial;
import salve.core.ThinkWorker;
import salve.data.sync.CloudSyncManager;
import salve.data.sync.SyncWorker;
import salve.services.BurbujaFlotanteService;
import salve.services.CamaraService;

public class MainActivity extends AppCompatActivity {

    // ===== NUBE (Namecheap) =====
    private static final String NUBE_ENDPOINT = "https://arzenit.com/salve_data.php";
    private static final String NUBE_SECRET   = "pon_aqui_tu_clave_larga"; // la misma que pusiste en el PHP

    // ===== VISTAS UI =====
    private EditText inputChat;
    private Button btnEnviarMensaje, btnHablar, btnEscuchar, btnAdjuntar, btnReflexiones;
    private ImageView imagenSalve;
    private LinearLayout panelReflexion;
    private TextView tituloReflexion, textoReflexion;
    private Button btnCerrarReflexion, btnSiguienteReflexion, btnResponderReflexion;
    private FloatingActionButton btnMostrarReflexion;

    // ===== INSTANCIAS DE LÓGICA =====
    private MemoriaEmocional memoria;
    private DiarioSecreto diario;
    private ReconocimientoFacial reconocimientoFacial;
    private MotorConversacional motorConversacional;

    // ===== ESTADO INTERNO =====
    private boolean entradaPorVoz = false;
    private boolean esperandoConfirmacionVisual = false;
    private static final int PERMISO_CAMARA = 123;
    private static final int REQ_POST_NOTIF = 1001;               // request code notificaciones

    // ===== DESCARGA DE MODELOS (handle para cancelación opcional) =====
    private ModelDownloader.TaskHandle modelsTask;
    private ActivityResultLauncher<String> audioPermissionLauncher;

    // ===== VERIFICACIÓN DE MODELOS LLM (por carpetas) =====
    // Directorio preferido: /data/data/<pkg>/files/models
    private static final String MODELS_DIR = "models";

    /** Clave para guardar la ruta del modelo elegido. */
    private static final String PREFS_NAME = "salve_prefs";
    private static final String KEY_MODEL_PATH = "llm_model_path";

    /** Directorio interno preferido (se crea si no existe). */
    private File getModelsRoot() {
        File dir = new File(getFilesDir(), MODELS_DIR);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /** Posibles raíces donde puede haber modelos (interno, externo app, y descargas públicas). */
    private List<File> getModelRoots() {
        List<File> roots = new ArrayList<>();
        // Interno de la app
        roots.add(new File(getFilesDir(), MODELS_DIR));

        // Externo privado de la app (ej: /storage/emulated/0/Android/data/<pkg>/files/models)
        File extApp = getExternalFilesDir(null);
        if (extApp != null) roots.add(new File(extApp, MODELS_DIR));

        // Descargas públicas (legacy/conveniencia)
        try {
            File pubDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (pubDownloads != null) roots.add(new File(pubDownloads, "Salve/models"));
            // Compat antigua
            roots.add(new File(Environment.getExternalStorageDirectory(), "Download/Salve/models"));
        } catch (Throwable ignored) {}

        return roots;
    }

    private static long folderSize(File f) {
        if (f == null || !f.exists()) return 0L;
        if (f.isFile()) return f.length();
        long total = 0L;
        File[] list = f.listFiles();
        if (list != null) for (File c : list) total += folderSize(c);
        return total;
    }

    /** Devuelve true si en 'dir' existe al menos 1 .gguf (en la carpeta o subcarpetas). */
    private static boolean containsGguf(File dir) {
        if (dir == null || !dir.exists()) return false;
        File[] list = dir.listFiles();
        if (list == null) return false;
        for (File f : list) {
            if (f.isDirectory()) {
                if (containsGguf(f)) return true;
            } else if (f.getName().toLowerCase(Locale.ROOT).endsWith(".gguf")) {
                return true;
            }
        }
        return false;
    }

    /** Formato GB amigable */
    private static String humanGB(long bytes) {
        double gb = bytes / 1024.0 / 1024.0 / 1024.0;
        return String.format(Locale.US, "%.2f GB", gb);
    }

    /** Requiere mlc-chat-config.json para considerarlo modelo válido. */
    private static boolean hasMlcConfig(File dir) {
        if (dir == null || !dir.isDirectory()) return false;
        File cfg = new File(dir, "mlc-chat-config.json");
        return cfg.exists();
    }

    // ================== ⬇️ Helpers de modelos (gguf o carpetas MLC) ⬇️ ==================

    /** Devuelve true si esta carpeta parece ser un modelo MLC (Phi, Qwen, etc.). */
    private static boolean looksLikeModelDir(File dir) {
        if (dir == null || !dir.isDirectory()) return false;
        String name = dir.getName().toLowerCase(Locale.ROOT);

        // Nuestros modelos vienen como "...-MLC"
        if (name.endsWith("-mlc") && hasMlcConfig(dir)) return true;

        // Heurística extra por si cambia el nombre: carpeta NO vacía con algún bin/json/gguf dentro.
        File[] children = dir.listFiles();
        if (children == null || children.length == 0) return false;
        for (File c : children) {
            if (!c.isFile()) continue;
            String n = c.getName().toLowerCase(Locale.ROOT);
            if ((n.endsWith(".gguf") || n.endsWith(".bin") || n.endsWith(".json") || n.endsWith(".model"))
                    && hasMlcConfig(dir)) {
                return true;
            }
        }
        return false;
    }

    /** Tamaño de un modelo (si es carpeta, suma recursivo; si es archivo, length). */
    private static long modelSize(File f) {
        if (f == null || !f.exists()) return 0L;
        if (f.isFile()) return f.length();
        return folderSize(f);
    }

    // ================== ⬇️ Escaneo recursivo de modelos (todas las raíces) ⬇️ ==================

    /**
     * Recorre recursivamente 'dir' y añade todos los modelos encontrados:
     * - Carpetas que parezcan modelos MLC (looksLikeModelDir)
     * - Archivos .gguf
     */
    private static void scanGguf(File dir, List<File> out) {
        if (dir == null || !dir.exists()) return;

        if (dir.isDirectory()) {
            // Si la carpeta ya parece un modelo MLC, la añadimos
            if (looksLikeModelDir(dir)) {
                out.add(dir);
                // Podemos seguir bajando por si hay más modelos dentro
            }

            File[] list = dir.listFiles();
            if (list == null) return;
            for (File f : list) {
                if (f.isDirectory()) {
                    scanGguf(f, out);
                } else if (f.getName().toLowerCase(Locale.ROOT).endsWith(".gguf")) {
                    out.add(f);
                }
            }
        } else if (dir.getName().toLowerCase(Locale.ROOT).endsWith(".gguf")) {
            out.add(dir);
        }
    }

    /** Busca modelos (carpetas -MLC o archivos .gguf) en todas las raíces conocidas, sin duplicar rutas. */
    private List<File> findAllGgufAllRoots() {
        List<File> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (File root : getModelRoots()) {
            if (root == null || !root.exists()) continue;
            List<File> temp = new ArrayList<>();
            scanGguf(root, temp);
            for (File f : temp) {
                String key = f.getAbsolutePath();
                if (!seen.contains(key)) {
                    seen.add(key);
                    out.add(f);
                }
            }
        }
        return out;
    }

    /** Devuelve el tamaño total de todos los modelos de la lista. */
    private static long totalSize(List<File> files) {
        long t = 0L;
        for (File f : files) t += modelSize(f);
        return t;
    }

    // ================== ⬆️ Escaneo recursivo de modelos (todas las raíces) ⬆️ ==================

    /** Guarda en SharedPreferences la ruta absoluta del modelo elegido. */
    private void savePreferredModel(File f) {
        if (f == null) return;
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_MODEL_PATH, f.getAbsolutePath())
                .apply();
    }

    /** Devuelve la ruta del modelo preferido para que otras clases lo carguen. */
    public static String getPreferredModelPath(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(KEY_MODEL_PATH, null);
    }

    /** (Opcional) Devuelve el File del modelo preferido o null si no existe. */
    public static File getPreferredModelFile(Context ctx) {
        String p = getPreferredModelPath(ctx);
        return (p == null) ? null : new File(p);
    }

    /**
     * Verifica modelos:
     * ✅ lista todos los modelos locales detectados (carpetas -MLC o .gguf)
     * ❌ si no hay, SOLO registra en log y un toast suave
     * 💾 guarda el modelo preferido para su carga posterior por MotorConversacional
     */
    private boolean checkModelsAndNotify() {
        List<File> modelos = findAllGgufAllRoots();

        // Filtrar sólo aquellos que tienen mlc-chat-config.json
        List<File> compatibles = new ArrayList<>();
        for (File f : modelos) {
            File dir = f.isDirectory() ? f : f.getParentFile();
            if (dir != null && hasMlcConfig(dir)) {
                compatibles.add(f.isDirectory() ? f : dir);
            }
        }

        if (compatibles.isEmpty()) {
            StringBuilder msg = new StringBuilder("❌ No hay modelos locales detectados.\nRutas revisadas:\n");
            for (File r : getModelRoots()) {
                msg.append(" • ").append(r.getAbsolutePath()).append('\n');
            }
            String report = msg.toString();
            Log.d("SalveDL/Status", report);

            Toast.makeText(
                    this,
                    "Aún no hay modelos MLC listos. Cuando termine la descarga desaparecerá este aviso.",
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }

        // Orden sugerido: por tamaño (carpetas o archivos) descendente
        compatibles.sort((a, b) -> Long.compare(modelSize(b), modelSize(a)));
        File elegido = compatibles.get(0);

        // 💾 Persistir la elección para que el motor lo cargue
        savePreferredModel(elegido);

        StringBuilder sb = new StringBuilder();
        sb.append("✅ Modelos detectados (").append(compatibles.size()).append(")\n");
        for (int i = 0; i < Math.min(8, compatibles.size()); i++) {
            File f = compatibles.get(i);
            sb.append(" • ").append(f.getName())
                    .append(" — ").append(humanGB(modelSize(f)))
                    .append("\n    ").append(f.getAbsolutePath()).append("\n");
        }
        sb.append("Total local: ").append(humanGB(totalSize(compatibles))).append("\n")
                .append("Modelo preferido ahora: ").append(elegido.getName());

        String report = sb.toString();
        Log.d("SalveDL/Status", "\n" + report);

        runOnUiThread(() ->
                Toast.makeText(this, "Modelos listos: " + modelos.size(), Toast.LENGTH_LONG).show()
        );
        return true;
    }

    /** Lista de modelos detectados agrupados por carpeta padre (para diagnóstico del módulo temporal). */
    private Map<String, File> collectLocalModels() {
        List<File> modelos = findAllGgufAllRoots();
        Map<String, File> found = new LinkedHashMap<>();

        for (File f : modelos) {
            File dir = f.isDirectory() ? f : f.getParentFile();
            if (dir == null || !hasMlcConfig(dir)) continue;

            String key;
            if (f.isDirectory()) {
                key = f.getName();
            } else {
                key = (f.getParentFile() != null) ? f.getParentFile().getName() : f.getName();
            }

            if (!found.containsKey(key)) {
                found.put(key, f);
            } else {
                File prev = found.get(key);
                if (prev != null && modelSize(f) > modelSize(prev)) {
                    found.put(key, f);
                }
            }
        }
        return found;
    }

    /** Migra .gguf desde ubicaciones públicas a la interna preferida, preservando subcarpetas si es posible. */
    private void migrateOldModelsIfAny() {
        File dstRoot = getModelsRoot(); // interno preferido

        for (File srcRoot : getModelRoots()) {
            if (srcRoot == null) continue;
            if (!srcRoot.exists() || srcRoot.equals(dstRoot)) continue;

            moveGgufRecursively(srcRoot, dstRoot);
        }
    }

    private void moveGgufRecursively(File src, File dstRoot) {
        if (src == null || !src.exists()) return;
        File[] list = src.listFiles();
        if (list == null) return;

        for (File f : list) {
            if (f.isDirectory()) {
                moveGgufRecursively(f, dstRoot);
            } else if (f.getName().toLowerCase(Locale.ROOT).endsWith(".gguf")) {
                // Construir subcarpeta destino usando el padre inmediato como "familia"
                String family = (f.getParentFile() != null) ? f.getParentFile().getName() : "misc";
                File famDst = new File(dstRoot, family);
                if (!famDst.exists()) famDst.mkdirs();
                File target = new File(famDst, f.getName());
                if (target.exists()) continue;

                boolean moved = f.renameTo(target);
                if (!moved) {
                    // Si no se pudo renombrar (entre volúmenes), intentar copiar
                    try {
                        copyFile(f, target);
                        //noinspection ResultOfMethodCallIgnored
                        f.delete();
                        moved = true;
                    } catch (Exception e) {
                        Log.e("SalveDL/Migrate", "Copia fallida: " + f.getAbsolutePath(), e);
                    }
                }
                Log.d("SalveDL/Migrate", "Mov " + f.getAbsolutePath() + " -> " + target.getAbsolutePath() + " = " + moved);
            }
        }
    }

    private static void copyFile(File src, File dst) throws Exception {
        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
        }
    }

    // ===== RESULT LAUNCHERS =====
    private final ActivityResultLauncher<Intent> reconocimientoLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            ArrayList<String> resultado =
                                    result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                            if (resultado != null && !resultado.isEmpty()) {
                                entradaPorVoz = true;
                                String dicho = resultado.get(0);
                                if (handleModuloTemporal(dicho)) {
                                    guardarEventoNube("voice_message_modulo_temporal", dicho, null);
                                    entradaPorVoz = false;
                                    GrafoRecuerdos.generar(this);
                                    CloudSyncManager.uploadGrafoBundle(this);
                                } else {
                                    motorConversacional.procesarEntrada(resultado.get(0), true);
                                    guardarEventoNube("voice_message", resultado.get(0), null);
                                    entradaPorVoz = false;
                                    GrafoRecuerdos.generar(this);
                                    CloudSyncManager.uploadGrafoBundle(this);
                                }
                            }
                        } else {
                            Toast.makeText(this, "No pude escuchar tu voz. Revisa el permiso de micrófono.", Toast.LENGTH_SHORT).show();
                            updateAudioPermissionState();
                        }
                    });

    private final ActivityResultLauncher<Intent> camaraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Bitmap foto = (Bitmap) result.getData().getExtras().get("data");
                            if (esperandoConfirmacionVisual) {
                                reconocimientoFacial.verificarRostro(foto, esValido -> {
                                    if (esValido) {
                                        String resp = "Confirmación visual aceptada. Entrando en modo sueño...";
                                        motorConversacional.hablar(sanitizeForSpeech(resp));
                                        guardarEventoNube("sleep_cycle_start", "Confirmación visual aceptada", null);
                                        memoria.cicloDeSueno();
                                        GrafoRecuerdos.generar(this);
                                        CloudSyncManager.uploadGrafoBundle(this);
                                    } else {
                                        String resp = "No te reconozco. No puedo dormir aún.";
                                        motorConversacional.hablar(sanitizeForSpeech(resp));
                                        guardarEventoNube("sleep_denied", "Rostro no reconocido", null);
                                    }
                                });
                                esperandoConfirmacionVisual = false;
                            }
                        }
                    });

    // ===== SELECTOR DE MÚLTIPLES IMÁGENES PARA CREAR PDF =====
    private ActivityResultLauncher<String[]> multiImagePicker;

    // ============================================================
    //                 CICLO DE VIDA: onCreate
    // ============================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // **Inicializar PDFBox para Android**
        PDFBoxResourceLoader.init(getApplicationContext());

        // Adjuntar overlay de consola para descargas
        ModelConsoleOverlay.attach(this);

        audioPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    btnEscuchar.setEnabled(true);
                    btnEscuchar.setAlpha(granted ? 1f : 0.75f);
                    if (!granted) {
                        Toast.makeText(this, "Activa el micrófono para usar reconocimiento de voz.", Toast.LENGTH_LONG).show();
                    }
                }
        );

        // ==== FIND VIEW BY ID ====
        inputChat             = findViewById(R.id.inputChat);
        btnEnviarMensaje      = findViewById(R.id.btnEnviarMensaje);
        btnHablar             = findViewById(R.id.btnHablar);
        btnEscuchar           = findViewById(R.id.btnEscuchar);
        imagenSalve           = findViewById(R.id.imagenSalve);
        panelReflexion        = findViewById(R.id.panelReflexion);
        tituloReflexion       = findViewById(R.id.tituloReflexion);
        textoReflexion        = findViewById(R.id.textoReflexion);
        btnCerrarReflexion    = findViewById(R.id.btnCerrarReflexion);
        btnSiguienteReflexion = findViewById(R.id.btnSiguienteReflexion);
        btnResponderReflexion = findViewById(R.id.btnResponderReflexion);
        btnMostrarReflexion   = findViewById(R.id.btnMostrarReflexion);

        // Animación flotante de Salve
        imagenSalve.setAnimation(AnimationUtils.loadAnimation(this, R.anim.float_animation));

        updateAudioPermissionState();

        // ==== INICIALIZAR LÓGICA ====
        memoria              = new MemoriaEmocional(this);
        diario               = new DiarioSecreto(this);
        reconocimientoFacial = new ReconocimientoFacial(this);
        motorConversacional  = new MotorConversacional(this, memoria, diario);

        // >>> LLM LOCAL: sólo informativo por ahora (el motor puede leer esta ruta con getPreferredModelPath)
        String preferredModel = getPreferredModelPath(this);
        if (preferredModel != null) {
            Log.d("SalveLLM", "Modelo local preferido (guardado en prefs): " + preferredModel);
        } else {
            Log.d("SalveLLM", "Aún no hay modelo preferido guardado (se fijará tras la primera preparación/descarga).");
        }
        // <<< aquí no tocamos MotorConversacional internamente, sólo dejamos la info preparada

        // Habilita overlay propio de ModelDownloader (si Activity visible)
        ModelDownloader.enableAutoOverlay(this);

        // Permiso POST_NOTIFICATIONS (Android 13+), sin abrir Ajustes automáticamente
        ensureNotificationPermission();

        // ==== LISTENERS ====
        btnEnviarMensaje.setOnClickListener(v -> {
            String mensaje = inputChat.getText().toString().trim();
            if (!mensaje.isEmpty()) {
                entradaPorVoz = false;

                // Interceptar preguntas fijas / diagnóstico
                if (handleModuloTemporal(mensaje)) {
                    guardarEventoNube("user_message_modulo_temporal", mensaje, null);
                    inputChat.setText("");
                    return;
                }

                // Lógica original
                motorConversacional.procesarEntrada(mensaje, entradaPorVoz);
                guardarEventoNube("user_message", mensaje, null);
                inputChat.setText("");

                // Actualizar grafo tras mensaje TEXTO del usuario
                GrafoRecuerdos.generar(this);
                CloudSyncManager.uploadGrafoBundle(this);
            }
        });

        // Pulsación larga en el botón → menú PDF
        btnEnviarMensaje.setOnLongClickListener(v -> {
            mostrarMenuPdf();
            return true;
        });

        btnHablar.setOnClickListener(v ->
                motorConversacional.hablar(sanitizeForSpeech("Estoy aquí, ¿en qué puedo ayudarte?")));

        btnEscuchar.setOnClickListener(v -> {
            if (!hasAudioPermission()) {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
                return;
            }
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES");
            reconocimientoLauncher.launch(intent);
        });

        btnCerrarReflexion.setOnClickListener(v -> panelReflexion.setVisibility(View.GONE));
        btnSiguienteReflexion.setOnClickListener(v -> mostrarSiguienteReflexion());
        btnResponderReflexion.setOnClickListener(v -> {
            String reflexion = textoReflexion.getText().toString();
            inputChat.setText(getString(R.string.respuesta_reflexion, reflexion));
            panelReflexion.setVisibility(View.GONE);

            GrafoRecuerdos.generar(this);
            CloudSyncManager.uploadGrafoBundle(this);
        });

        // FAB rosa
        btnMostrarReflexion.setOnClickListener(v -> mostrarSiguienteReflexion());

        // NUEVO botón morado grande "Ver reflexión" (si existe en el layout)
        if (btnReflexiones != null) {
            btnReflexiones.setOnClickListener(v -> mostrarSiguienteReflexion());
        }

        // === REGISTRAR SELECTOR DE IMÁGENES ===
        multiImagePicker = registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(),
                uris -> {
                    if (uris == null || uris.isEmpty()) {
                        Toast.makeText(this, "Sin imágenes", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        List<Bitmap> imagenes = new ArrayList<>();
                        for (Uri u : uris) {
                            imagenes.add(cargarBitmapSeguro(u));
                        }
                        File pdf = new PdfGenerator(this).crearPdfDesdeImagenes(imagenes, "mis_fotos");
                        Toast.makeText(this, "PDF creado: " + pdf.getAbsolutePath(), Toast.LENGTH_LONG).show();
                        compartirPdf(pdf);

                        GrafoRecuerdos.generar(this);
                        CloudSyncManager.uploadGrafoBundle(this);
                    } catch (Exception e) {
                        Toast.makeText(this, "Error creando PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("Salve/PDF", "Error", e);
                    }
                }
        );

        // Botón Adjuntar (si existe en el layout)
        if (btnAdjuntar != null) {
            btnAdjuntar.setOnClickListener(v -> {
                multiImagePicker.launch(new String[]{"image/*"});
                Toast.makeText(this, "Abriendo galería…", Toast.LENGTH_SHORT).show();
            });
        }

        // ==== PERMISOS Y SERVICIOS ====
        verificarPermisoCamara();
        verificarPermisoOverlay(); // ahora solo informa; no abre Ajustes sola

        // === PROGRAMAR PENSAMIENTO AUTOMÁTICO CADA 15 MIN ===
        PeriodicWorkRequest pensarSolaRequest =
                new PeriodicWorkRequest.Builder(ThinkWorker.class, 15, TimeUnit.MINUTES).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "salve_think_job",
                ExistingPeriodicWorkPolicy.KEEP,
                pensarSolaRequest
        );

        // ===== manejar Intents de compartir =====
        handleShareIntent(getIntent());

        // ===== MIGRAR (si tenías modelos en carpetas viejas) y ARRANCAR DESCARGAS =====
        migrateOldModelsIfAny();
        iniciarDescargaModelos(); // idempotente (solo descarga los que falten/estén corruptos)
    }

    // Si la Activity ya estaba abierta y llega un nuevo share, lo recibimos aquí
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleShareIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SyncWorker.enqueueWhenOnline(getApplicationContext());

        updateAudioPermissionState();

        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            Toast.makeText(this, "Activa las notificaciones para ver el progreso en segundo plano.", Toast.LENGTH_LONG).show();
        }

        // Comprobación visible del estado de los modelos (todas las rutas)
        checkModelsAndNotify();

        // Log extra para ver siempre qué modelo local quedó elegido
        String preferredModel = getPreferredModelPath(this);
        Log.d("SalveLLM", "onResume → modelo preferido: " + preferredModel);
    }

    // ============================================================
    // >>> MÓDULO TEMPORAL (usa strings.xml) + diagnóstico
    // ============================================================
    private static String normalizeQ(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        n = n.toLowerCase(Locale.ROOT);
        n = n.replaceAll("[^a-z0-9ñü\\s]", " ");
        n = n.replaceAll("\\s+", " ").trim();
        return n;
    }

    /**
     * Intercepta:
     * - “¿Qué gestionas?”
     * - “¿Cuál es nuestro lema?”
     * - Diagnóstico LLM: “¿qué modelo usas?”, “modelo activo”, “estado modelos”, “¿estás usando LLM?”
     */
    private boolean handleModuloTemporal(String rawQuestion) {
        String q = normalizeQ(rawQuestion);

        boolean preguntaGestionas =
                q.startsWith("que gestionas") ||
                        q.contains(" que gestionas") ||
                        (q.contains("gestionas") && q.contains("que")) ||
                        q.matches(".*\\bgestionas?\\b.*");

        boolean preguntaLema =
                q.startsWith("cual es nuestro lema") ||
                        q.contains(" cual es nuestro lema") ||
                        q.contains("nuestro lema") ||
                        (q.contains("lema") && (q.contains("nuestro") || q.contains("cual es"))) ||
                        q.equals("lema") || q.endsWith(" lema");

        if (preguntaGestionas) {
            String resp = getString(R.string.salve_gestiona);
            motorConversacional.hablar(sanitizeForSpeech(resp));
            guardarEventoNube("respuesta_modulo_temporal", resp, null);
            GrafoRecuerdos.generar(this);
            CloudSyncManager.uploadGrafoBundle(this);
            return true;
        }

        if (preguntaLema) {
            String resp = getString(R.string.salve_lema);
            motorConversacional.hablar(sanitizeForSpeech(resp));
            guardarEventoNube("respuesta_modulo_temporal", resp, null);
            GrafoRecuerdos.generar(this);
            CloudSyncManager.uploadGrafoBundle(this);
            return true;
        }

        // === Diagnóstico LLM ===
        boolean preguntaModelo =
                q.contains("que modelo usas") || q.contains("qué modelo usas") ||
                        q.contains("modelo activo") || q.contains("estado modelos") ||
                        q.contains("estas usando llm") || q.contains("estás usando llm");

        if (preguntaModelo) {
            Map<String, File> modelos = collectLocalModels();
            if (modelos.isEmpty()) {
                String resp = "No encuentro modelos locales aún. Revisa 'files/models' o espera la descarga.";
                motorConversacional.hablar(sanitizeForSpeech(resp));
                guardarEventoNube("llm_diag", resp, null);
                return true;
            } else {
                // Elegimos el modelo más grande de los detectados (carpeta o archivo)
                File elegido = null;
                for (File f : modelos.values()) {
                    if (elegido == null || (f != null && modelSize(f) > modelSize(elegido))) {
                        elegido = f;
                    }
                }

                String resp = "Modelos detectados: " + modelos.keySet() +
                        ". Usaría: " + (elegido != null ? elegido.getName() : "desconocido") +
                        ". Si me oyes offline con respuestas elaboradas, estoy usando el LLM local.";
                motorConversacional.hablar(sanitizeForSpeech(resp));
                guardarEventoNube("llm_diag", resp, null);
                return true;
            }
        }

        return false; // no interceptado
    }

    // ============================================================
    //                  MENÚ PDF (TEXTO o IMÁGENES)
    // ============================================================
    private void mostrarMenuPdf() {
        String[] opciones = {"PDF desde texto", "PDF desde imágenes"};
        new AlertDialog.Builder(this)
                .setTitle("Crear PDF")
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) {
                        crearPdfDelInputChat();
                    } else if (which == 1) {
                        multiImagePicker.launch(new String[]{"image/*"});
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void crearPdfDelInputChat() {
        String texto = inputChat.getText().toString().trim();
        if (texto.isEmpty()) {
            Toast.makeText(this, "Escribe algo primero", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File pdf = new PdfGenerator(this).crearPdfDesdeTexto(texto, "mi_escrito");
            Toast.makeText(this, "PDF creado: " + pdf.getAbsolutePath(), Toast.LENGTH_LONG).show();
            compartirPdf(pdf);

            GrafoRecuerdos.generar(this);
            CloudSyncManager.uploadGrafoBundle(this);
        } catch (Exception e) {
            Toast.makeText(this, "Error creando PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ============================================================
    //                  SHARE: recibir y "aprender" documentos
    // ============================================================
    private void handleShareIntent(Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        String type = intent.getType();

        Log.d("Salve/Share", "action=" + action + " type=" + type);

        try {
            if (Intent.ACTION_SEND.equals(action)) {
                Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (uri != null) {
                    procesarArchivoCompartido(uri, type);
                    return;
                }
                if (intent.getClipData() != null && intent.getClipData().getItemCount() > 0) {
                    Uri u = intent.getClipData().getItemAt(0).getUri();
                    String resolvedType = (type != null) ? type : getContentResolver().getType(u);
                    procesarArchivoCompartido(u, resolvedType);
                    return;
                }
                Toast.makeText(this, "No llegó EXTRA_STREAM ni ClipData", Toast.LENGTH_LONG).show();

            } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (uris != null && !uris.isEmpty()) {
                    for (Uri u : uris) {
                        String resolvedType = (type != null) ? type : getContentResolver().getType(u);
                        procesarArchivoCompartido(u, resolvedType);
                    }
                    return;
                }
                if (intent.getClipData() != null && intent.getClipData().getItemCount() > 0) {
                    for (int i = 0; i < intent.getClipData().getItemCount(); i++) {
                        Uri u = intent.getClipData().getItemAt(i).getUri();
                        String resolvedType = getContentResolver().getType(u);
                        procesarArchivoCompartido(u, resolvedType);
                    }
                    return;
                }

                Toast.makeText(this, "No llegaron URIs en múltiple", Toast.LENGTH_LONG).show();

            } else if (Intent.ACTION_VIEW.equals(action)) {
                Uri data = intent.getData();
                if (data != null) {
                    String resolvedType = (type != null) ? type : getContentResolver().getType(data);
                    procesarArchivoCompartido(data, resolvedType);
                    return;
                }
                Toast.makeText(this, "ACTION_VIEW sin data", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Error procesando archivo: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("Salve/Share", "handleShareIntent error", e);
        }
    }

    private void procesarArchivoCompartido(Uri uri, String mime) {
        try {
            if (mime == null || "application/octet-stream".equals(mime)) {
                String resolved = getContentResolver().getType(uri);
                if (resolved != null) mime = resolved;
            }
            if (mime == null) mime = "";

            Log.d("Salve/Share", "Procesando uri=" + uri + " mime=" + mime);

            if (mime.startsWith("application/pdf")) {
                leerPdfYAprender(uri);

            } else if (mime.startsWith("text/")) {
                String texto = leerTextoPlano(uri);
                if (texto != null && !texto.trim().isEmpty()) {
                    motorConversacional.hablar(sanitizeForSpeech("He recibido un texto. Empezaré a leerlo."));
                    speakTextInChunks(texto);
                    guardarAprendizaje("texto_compartido", texto);

                    GrafoRecuerdos.generar(this);
                    CloudSyncManager.uploadGrafoBundle(this);
                } else {
                    Toast.makeText(this, "No pude leer el texto compartido", Toast.LENGTH_LONG).show();
                }

            } else if (mime.startsWith("image/")) {
                Toast.makeText(this, "Imagen recibida (OCR próximamente)", Toast.LENGTH_SHORT).show();

                GrafoRecuerdos.generar(this);
                CloudSyncManager.uploadGrafoBundle(this);

            } else {
                String name = uri.toString().toLowerCase();
                if (name.endsWith(".pdf")) {
                    leerPdfYAprender(uri);
                } else {
                    Toast.makeText(this, "Tipo no soportado: " + mime, Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Fallo al procesar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("Salve/Share", "procesarArchivoCompartido error", e);
        }
    }

    private void leerPdfYAprender(Uri uri) {
        try (PDDocument doc = PDDocument.load(getContentResolver().openInputStream(uri))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String texto = stripper.getText(doc);

            if (texto == null || texto.trim().isEmpty()) {
                Toast.makeText(this, "No pude extraer texto del PDF", Toast.LENGTH_LONG).show();
                return;
            }

            motorConversacional.hablar(sanitizeForSpeech("He recibido un PDF. Empezaré a leerlo."));
            speakTextInChunks(texto);
            guardarAprendizaje("pdf_compartido", texto);

            GrafoRecuerdos.generar(this);
            CloudSyncManager.uploadGrafoBundle(this);

        } catch (Exception e) {
            Toast.makeText(this, "Error leyendo PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("Salve/PDF", "Error leyendo PDF", e);
        }
    }

    private String leerTextoPlano(Uri uri) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                getContentResolver().openInputStream(uri)))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // Guarda el contenido aprendido: registra en la nube (cola+sync)
    // y deja una copia local en /Android/data/<pkg>/files/Documents
    private void guardarAprendizaje(String tipo, String textoLargo) {
        try {
            String resumen = (textoLargo != null && textoLargo.length() > 2000)
                    ? textoLargo.substring(0, 2000)
                    : (textoLargo == null ? "" : textoLargo);
            guardarEventoNube("document_ingested:" + tipo, resumen, null);
        } catch (Exception ignored) {}

        try {
            File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir != null && !dir.exists()) dir.mkdirs();
            File out = new File(dir, tipo + "-" + System.currentTimeMillis() + ".txt");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                fos.write((textoLargo == null ? "" : textoLargo).getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {}
    }

    // ========= SANEADOR ANTIBUCLE Y TTS =========
    /** Limpia repeticiones/loops y espacios raros antes de hablar. */
    private String sanitizeForSpeech(String text) {
        if (text == null) return "";
        String s = text.trim();

        // Colapsar espacios
        s = s.replaceAll("[ \\t\\x0B\\f\\r]+", " ");

        // Eliminar repeticiones de la MISMA palabra 4+ veces seguidas
        s = mitigateLLMLoops(s);

        // Limitar signos repetidos
        s = s.replaceAll("([!?¡¿,.])\\1{2,}", "$1$1");

        // Si quedó demasiado corto o vacío, ponemos un fallback legible
        if (s.length() < 2) s = "He tenido un bucle al generar la respuesta. Lo reformulo.";
        return s;
    }

    /** Mitiga bucles tipo token repetido (palabra o secuencia corta). */
    private String mitigateLLMLoops(String s) {
        // 1) Palabra repetida 4+ veces
        s = s.replaceAll("\\b(\\p{L}{2,30})\\b(?:\\s+\\1\\b){3,}", "$1 $1 $1");

        // 2) Secuencias de 2-3 palabras repetidas 3+ veces
        s = s.replaceAll("(\\b\\p{L}{2,30}\\b\\s+\\b\\p{L}{2,30}\\b)(?:\\s+\\1){2,}", "$1 $1");
        s = s.replaceAll("(\\b\\p{L}{2,30}\\b\\s+\\b\\p{L}{2,30}\\b\\s+\\b\\p{L}{2,30}\\b)(?:\\s+\\1){2,}", "$1");

        // 3) Si una misma palabra aparece > 8 veces en total, recorta a 6
        String[] tokens = s.split("\\s+");
        Map<String,Integer> cnt = new HashMap<>();
        StringBuilder out = new StringBuilder();
        for (String t : tokens) {
            String key = t.toLowerCase(Locale.ROOT);
            int c = cnt.getOrDefault(key, 0) + 1;
            cnt.put(key, c);
            if (c <= 6) out.append(t).append(' ');
        }
        return out.toString().trim();
    }

    /** Trocea por frases + longitudes moderadas, aplicando sanitizador por parte. */
    private void speakTextInChunks(String fullText) {
        if (fullText == null || fullText.trim().isEmpty()) return;

        List<String> partes = splitBySentencesAndLength(fullText, 400);
        android.os.Handler h = new android.os.Handler(getMainLooper());
        long acumulado = 400;

        for (String raw : partes) {
            final String trozo = sanitizeForSpeech(raw);
            if (trozo.isEmpty()) continue;
            h.postDelayed(() -> {
                try {
                    motorConversacional.hablar(trozo);
                } catch (Exception ignored) {}
            }, acumulado);

            long estimado = Math.max(1200, (long)(trozo.length() * 45));
            acumulado += estimado;
        }
    }

    /** Divide por límites de frase (., !, ?) y limita longitud máxima por trozo. */
    private List<String> splitBySentencesAndLength(String text, int maxLen) {
        ArrayList<String> partes = new ArrayList<>();
        BreakIterator it = BreakIterator.getSentenceInstance(new Locale("es"));
        it.setText(text);
        int start = it.first();
        for (int end = it.next(); end != BreakIterator.DONE; start = end, end = it.next()) {
            String sent = text.substring(start, end).trim();
            if (sent.isEmpty()) continue;
            if (sent.length() <= maxLen) {
                partes.add(sent);
            } else {
                int i = 0;
                while (i < sent.length()) {
                    int e = Math.min(i + maxLen, sent.length());
                    partes.add(sent.substring(i, e));
                    i = e;
                }
            }
        }
        if (partes.isEmpty()) {
            String s = text.trim();
            for (int i = 0; i < s.length(); i += maxLen) {
                partes.add(s.substring(i, Math.min(i + maxLen, s.length())));
            }
        }
        return partes;
    }

    /** Muestra una reflexión aleatoria del buffer y actualiza la nube/grafo. */
    private void mostrarSiguienteReflexion() {
        List<String> reflexiones = memoria.obtenerTodasLasReflexiones();
        if (reflexiones == null || reflexiones.isEmpty()) {
            Toast.makeText(this, "No hay reflexiones aún", Toast.LENGTH_SHORT).show();
            return;
        }
        int indice = (int) (Math.random() * reflexiones.size());
        String texto = reflexiones.get(indice);

        textoReflexion.setText(texto);
        tituloReflexion.setText(getString(R.string.titulo_reflexion));
        panelReflexion.setVisibility(View.VISIBLE);

        guardarEventoNube("reflexion_mostrada", texto, null);
        if (Math.random() > 0.5) {
            try { motorConversacional.hablar(sanitizeForSpeech(texto)); } catch (Exception ignored) {}
        }

        GrafoRecuerdos.generar(this);
        CloudSyncManager.uploadGrafoBundle(this);
    }

    // ============================================================
    //                  OTROS MÉTODOS AUXILIARES
    // ============================================================
    private void verificarPermisoOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Para la burbuja, activa 'Mostrar sobre otras apps' en Ajustes.", Toast.LENGTH_LONG).show();
        } else {
            iniciarBurbujaFlotante();
        }
    }

    private boolean hasAudioPermission() {
        return ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void updateAudioPermissionState() {
        boolean granted = hasAudioPermission();
        btnEscuchar.setEnabled(true);
        btnEscuchar.setAlpha(granted ? 1f : 0.75f);
    }

    private void openOverlaySettings() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void iniciarBurbujaFlotante() {
        try { startService(new Intent(this, BurbujaFlotanteService.class)); }
        catch (Throwable t) { Log.e("Salve", "Burbuja no pudo iniciar", t); }
    }

    private void verificarPermisoCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISO_CAMARA);
        } else {
            iniciarCamaraService();
        }
    }

    private void iniciarCamaraService() {
        try {
            startService(new Intent(this, CamaraService.class));
            Log.d("Salve", "Servicio de cámara iniciado.");
        } catch (Throwable t) {
            Log.e("Salve", "CamaraService no pudo iniciar", t);
        }
    }

    private void lanzarCamaraParaVerificacion() {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        camaraLauncher.launch(intent);
    }

    // Permiso notificaciones (Android 13+) — NO abre Ajustes automáticamente
    private void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_POST_NOTIF
                );
            }
        }
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            Toast.makeText(this,
                    "Activa las notificaciones para ver el progreso en segundo plano.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private static void openAppNotificationSettings(Context ctx) {
        Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISO_CAMARA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                iniciarCamaraService();
            }
        } else if (requestCode == REQ_POST_NOTIF) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            Log.d("Perms", "POST_NOTIFICATIONS granted? " + granted);
            if (!granted) {
                Toast.makeText(this, "Activa notificaciones para mostrar el progreso en segundo plano.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        // Cancelar descarga de modelos si sigue activa
        if (modelsTask != null) {
            modelsTask.cancel();
            modelsTask = null;
        }

        stopService(new Intent(this, CamaraService.class));
        stopService(new Intent(this, BurbujaFlotanteService.class));
        super.onDestroy();
    }

    // ===================== NUBE: MÓDULO =========================
    private void enviarAServidor(String jsonPayload) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(NUBE_ENDPOINT);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("X-Salve-Token", NUBE_SECRET);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                Log.d("Salve/Nube", "Respuesta servidor: " + code);
            } catch (Exception e) {
                Log.e("Salve/Nube", "Error enviando a la nube", e);
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void guardarEventoNube(String tipo, String contenido, Integer emocion) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("type", tipo);
            if (contenido != null) obj.put("content", contenido);
            if (emocion != null) obj.put("emotion", emocion);
            obj.put("time_ms", System.currentTimeMillis());

            CloudSyncManager.enqueue(getApplicationContext(), obj.toString());
            SyncWorker.enqueueWhenOnline(getApplicationContext());
            enviarAServidor(obj.toString());
        } catch (Exception e) {
            Log.e("Salve/Nube", "Error creando/encolando JSON", e);
        }
    }

    // ===================== HELPERS PDF/IMAGEN =========================
    private Bitmap cargarBitmapSeguro(Uri uri) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        int maxDim = Math.max(bounds.outWidth, bounds.outHeight);
        int sample = 1;
        while (maxDim / sample > 2000) sample *= 2;
        BitmapFactory.Options real = new BitmapFactory.Options();
        real.inSampleSize = sample;
        try (InputStream in2 = getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(in2, null, real);
        }
    }

    private void compartirPdf(File pdfFile) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", pdfFile);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("application/pdf");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, "Compartir PDF"));
    }

    // ===================== MODELOS LLM: DESCARGA + DESCOMPRESIÓN =========================

    /** Recorre dir y subcarpetas buscando ficheros .zip */
    private static void scanZipFiles(File dir, List<File> out) {
        if (dir == null || !dir.exists()) return;
        File[] list = dir.listFiles();
        if (list == null) return;
        for (File f : list) {
            if (f.isDirectory()) {
                scanZipFiles(f, out);
            } else if (f.getName().toLowerCase(Locale.ROOT).endsWith(".zip")) {
                out.add(f);
            }
        }
    }

    /**
     * Prepara TODOS los modelos que ya están como .zip en cualquier raíz conocida
     * (interno, Android/data/.../files/models, etc.), sin descargar nada.
     * Llama a ModelStore.ensureModelFolder(...) para cada uno y muestra progreso en consola.
     */
    private void prepararDesdeZipsExistentes() {
        List<File> zips = new ArrayList<>();

        for (File root : getModelRoots()) {
            if (root != null && root.exists()) {
                scanZipFiles(root, zips);
            }
        }

        if (zips.isEmpty()) {
            ModelConsoleOverlay.log("No hay zips locales que preparar.");
            return;
        }

        ModelConsoleOverlay.show();
        ModelConsoleOverlay.log("Encontrados " + zips.size() + " zips locales. Preparando modelos…");

        for (File zip : zips) {
            String id = zip.getName();
            int dot = id.lastIndexOf('.');
            if (dot > 0) id = id.substring(0, dot);   // nombre sin .zip

            ModelConsoleOverlay.log("Preparando " + id + " desde " + zip.getName());
            try {
                ModelStore.ensureModelFolder(this, zip, id);
            } catch (Exception e) {
                Log.e("SalveDL", "Error preparando " + id + " desde zip " + zip.getAbsolutePath(), e);
                ModelConsoleOverlay.log("✖ Error preparando " + id + ": " + e.getMessage());
            }
        }
    }

    /** ▶️ Descarga automática de modelos usando precheck + descarga asíncrona + consola visual */
    private void iniciarDescargaModelos() {
        String net = describirConexion();
        if (net == null) {
            Toast.makeText(this, "Sin conexión: no puedo descargar modelos", Toast.LENGTH_LONG).show();
            Log.e("SalveDL", "Sin conexión de red");
            ModelConsoleOverlay.log("Sin conexión, no puedo descargar modelos.");
            ModelConsoleOverlay.hideDelayed(2000);
            return;
        } else {
            Log.d("SalveDL", "Conexión: " + net);
        }

        boolean needDownload;

        // Prepara consola
        ModelConsoleOverlay.clear();
        ModelConsoleOverlay.show();
        ModelConsoleOverlay.log("Conexión detectada: " + net);
        ModelConsoleOverlay.log("Comprobando modelos locales (precheck)...");

        // 1) Precheck: mirar si falta algo sin descargar nada
        try (InputStream pre = getAssets().open("config/models.json")) {
            needDownload = ModelDownloader.precheckAll(this, pre);
        } catch (Exception e) {
            Log.e("SalveDL", "Error en precheck de modelos", e);
            ModelConsoleOverlay.log("Error en precheck: " + e.getMessage());
            needDownload = true; // ante duda, intentamos descargar
        }

        if (!needDownload) {
            Log.d("SalveDL", "Modelos ya presentes. No se descargan de nuevo.");
            ModelConsoleOverlay.log("Modelos ya presentes. Revisando descompresión (sin nuevas descargas)…");

            // 🔄 Hacer la preparación de zips en un hilo de fondo
            new Thread(() -> {
                try {
                    // Trabajo pesado: leer zips y descomprimir
                    prepararDesdeZipsExistentes();
                } catch (Exception e) {
                    Log.e("SalveDL", "Error preparando modelos locales desde zips", e);
                    ModelConsoleOverlay.log("✖ Error preparando modelos locales: " + e.getMessage());
                }

                // Volvemos a la parte "visible" (UI) después del trabajo pesado
                runOnUiThread(() -> {
                    checkModelsAndNotify();
                    ModelConsoleOverlay.log("Modelos listos.");
                    ModelConsoleOverlay.hideDelayed(2500);
                });
            }).start();

            return;
        }


        ModelConsoleOverlay.log("Faltan modelos o hay que reparar. Iniciando descarga...");

        // 2) Lanzar descarga asíncrona (sin bloquear y con overlay)
        try {
            InputStream json = getAssets().open("config/models.json");
            modelsTask = ModelDownloader.downloadAllAsync(this, json, new ModelDownloader.Listener() {
                @Override
                public void onProgress(String id, int percent) {
                    Log.d("SalveDL", id + " → " + percent + "%");
                    ModelConsoleOverlay.log(id + " → " + percent + "%");
                }

                @Override
                public void onComplete(String id, File file) {
                    // Se ha descargado COMPLETO el zip de este modelo.
                    Log.d("SalveDL", id + " completado en " + file.getAbsolutePath());
                    ModelConsoleOverlay.show();
                    ModelConsoleOverlay.log("✔ Descarga completada: " + id);
                    ModelConsoleOverlay.log(id + " → preparando modelo (descompresión si hace falta)…");

                    // Descomprimir / preparar carpeta final
                    File finalDir = null;
                    try {
                        finalDir = ModelStore.ensureModelFolder(MainActivity.this, file, id);
                    } catch (Exception e) {
                        Log.e("SalveDL", "Error descomprimiendo modelo " + id, e);
                        ModelConsoleOverlay.log("✖ Error descomprimiendo " + id + ": " + e.getMessage());
                    }

                    File finalDirCopy = finalDir;
                    runOnUiThread(() -> {
                        if (finalDirCopy != null && finalDirCopy.exists()) {
                            Toast.makeText(
                                    MainActivity.this,
                                    "Modelo preparado: " + id,
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                        checkModelsAndNotify();
                    });
                }

                @Override
                public void onError(String id, Exception e) {
                    String msg = String.valueOf(e);
                    Log.e("SalveDL", "Error " + id + ": " + msg, e);
                    Toast.makeText(MainActivity.this,
                            "Error descargando " + id + ": " + msg,
                            Toast.LENGTH_LONG).show();
                    ModelConsoleOverlay.log("✖ Error " + id + ": " + msg);
                }

                @Override
                public void onAllDone() {
                    Log.d("SalveDL", "Todas las descargas de modelos han terminado.");
                    ModelConsoleOverlay.log("Todas las descargas han terminado. Modelos preparados.");
                    checkModelsAndNotify();
                    ModelConsoleOverlay.hideDelayed(2500);
                }
            });
        } catch (Exception e) {
            Log.e("SalveDL", "No pude abrir assets/config/models.json (¿ruta mal o falta el archivo?)", e);
            Toast.makeText(MainActivity.this,
                    "Falta config/models.json en assets",
                    Toast.LENGTH_LONG).show();
            ModelConsoleOverlay.log("Falta config/models.json en assets.");
            ModelConsoleOverlay.hideDelayed(2500);
        }
    }

    /** Devuelve "Wi-Fi", "Datos móviles", "otra" o null si no hay red */
    @SuppressLint("MissingPermission")
    private String describirConexion() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if (cm == null) return null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network n = cm.getActiveNetwork();
                if (n == null) return null;
                NetworkCapabilities c = cm.getNetworkCapabilities(n);
                if (c == null) return null;
                if (c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "Wi-Fi";
                if (c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "Datos móviles";
                return "otra";
            } else {
                @SuppressWarnings("deprecation")
                NetworkInfo ni = cm.getActiveNetworkInfo();
                if (ni != null && ni.isConnected()) return ni.getTypeName();
            }
        } catch (Exception ignored) {}
        return null;
    }
}
