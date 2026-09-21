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
import androidx.lifecycle.ViewModelProvider;
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
import salve.core.ModelStore;
import salve.core.ModuloInvestigacion;
import salve.core.MotorConversacional;
import salve.core.SalveLLM;
import salve.core.GeminiService;
import salve.core.ModelResult;
import salve.core.PdfGenerator;
import salve.core.ReconocimientoFacial;
import salve.core.ThinkWorker;
import salve.data.sync.CloudSyncManager;
import salve.data.sync.SyncWorker;
import salve.services.SistemaSensorial;
import salve.services.VideoAnalysisManager;
import salve.presentation.viewmodel.ModelDownloadViewModel;

public class MainActivity extends AppCompatActivity {

    private final java.util.concurrent.ExecutorService inferenceChecks =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private String visualQuestion;
    private boolean visualLocalOnly;
    private final ActivityResultLauncher<String[]> localModelPicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) importarModeloLocal(uri);
            });
    private final ActivityResultLauncher<Void> photoAnalysisLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(), photo -> {
                String question = visualQuestion;
                visualQuestion = null;
                if (photo != null && this.motorConversacional != null) {
                    this.motorConversacional.procesarImagen(question, photo, visualLocalOnly);
                } else {
                    if (photo != null) photo.recycle();
                    Toast.makeText(this, "No se capturó ninguna foto.", Toast.LENGTH_SHORT).show();
                }
            });
    private final ActivityResultLauncher<String> photoPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) launchAnalysisCamera();
                else Toast.makeText(this, "La consulta visual necesita permiso de cámara.", Toast.LENGTH_LONG).show();
            });

    // ===== NUBE (Namecheap) =====
    private static final String NUBE_ENDPOINT = "https://arzenit.com/salve_data.php";
    private static final String NUBE_SECRET   = "pon_aqui_tu_clave_larga"; // la misma que pusiste en el PHP

    // ===== VISTAS UI =====
    private EditText inputChat;
    private Button btnEnviarMensaje, btnHablar, btnEscuchar, btnAdjuntar, btnReflexiones;
    private salve.avatar.AvatarView imagenSalve;
    private LinearLayout panelReflexion;
    private TextView tituloReflexion, textoReflexion;
    private Button btnCerrarReflexion, btnSiguienteReflexion, btnResponderReflexion;
    private FloatingActionButton btnMostrarReflexion;

    // ===== INSTANCIAS DE LÓGICA =====
    private MemoriaEmocional memoria;
    private DiarioSecreto diario;
    private ReconocimientoFacial reconocimientoFacial;
    private MotorConversacional motorConversacional;

    private ModuloInvestigacion investigacion;
    private SistemaSensorial sensores;
    
    // ===== CEREBRO Y OÍDOS =====
    private android.speech.SpeechRecognizer speechRecognizer;
    
    // ===== CONCIENCIA FUNCIONAL =====
    private salve.core.IdentidadNucleo identidadNucleo;
    private salve.core.CicloConciencia cicloConciencia;
    private TextView tvNivelConciencia;

    // ===== ESTADO INTERNO =====
    private boolean esperandoConfirmacionVisual = false;
    private static final int PERMISO_CAMARA = 123;
    private static final int REQ_POST_NOTIF = 1001;               // request code notificaciones

    // ===== DESCARGA DE MODELOS =====
    private ModelDownloadViewModel modelDownloadViewModel;
    private ActivityResultLauncher<String> audioPermissionLauncher;

    // ===== VERIFICACIÓN DE MODELOS LLM (por carpetas) =====
    // Directorio preferido: /data/data/<pkg>/files/models
    private static final String MODELS_DIR = "models";

    /** Clave para guardar la ruta del modelo elegido. */
    private static final String PREFS_NAME = "salve_prefs";
    private static final String KEY_MODEL_PATH = "llm_model_path";

    /** Directorio interno preferido (se crea si no existe). */
    private File getModelsRoot() {
        return ModelStore.dir(this);
    }

    /** Posibles raíces donde puede haber modelos (interno, externo app, y descargas públicas). */
    private List<File> getModelRoots() {
        List<File> roots = new ArrayList<>();
        roots.add(ModelStore.dir(this));
        // Interno de la app
        roots.add(new File(getFilesDir(), MODELS_DIR));

        // Externo privado de la app (ej: /storage/emulated/0/Android/data/<pkg>/files/models)
        File extApp = getExternalFilesDir(null);
        if (extApp != null) roots.add(new File(extApp, MODELS_DIR));

        // Descargas públicas y carpetas de otras apps de IA
        try {
            File sdcard = Environment.getExternalStorageDirectory();
            roots.add(sdcard); // 🟢 ESCANEO TOTAL
            
            File pubDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (pubDownloads != null) {
                roots.add(pubDownloads); // 🟢 Escanear toda la carpeta descargas
                roots.add(new File(pubDownloads, "Salve/models"));
                roots.add(new File(pubDownloads, "MEGA Downloads")); 
            }
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
     * - Archivos .gguf, .litertlm, .task
     * USA UN LÍMITE DE PROFUNDIDAD PARA EVITAR CRASHES
     */
    private static void scanGguf(File dir, List<File> out, int depth) {
        if (dir == null || !dir.exists() || depth > 8) return;

        if (dir.isDirectory()) {
            // Si la carpeta ya parece un modelo MLC, la añadimos
            if (looksLikeModelDir(dir)) {
                out.add(dir);
            }

            File[] list = dir.listFiles();
            if (list == null) return;
            for (File f : list) {
                if (f.isDirectory()) {
                    // Evitar carpetas de sistema pesadas
                    String n = f.getName().toLowerCase(Locale.ROOT);
                    if (n.equals("android") || n.equals("data") || n.equals("obb")) continue;
                    scanGguf(f, out, depth + 1);
                } else {
                    String name = f.getName().toLowerCase(Locale.ROOT);
                    if (name.endsWith(".gguf") || name.endsWith(".litertlm") || name.endsWith(".task")) {
                        out.add(f);
                    }
                }
            }
        } else {
            String name = dir.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".gguf") || name.endsWith(".litertlm") || name.endsWith(".task")) {
                out.add(dir);
            }
        }
    }

    /** Busca modelos en todas las raíces conocidas, sin duplicar rutas. */
    private List<File> findAllGgufAllRoots() {
        List<File> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (File root : getModelRoots()) {
            if (root == null || !root.exists()) continue;
            List<File> temp = new ArrayList<>();
            scanGguf(root, temp, 0);
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
        Log.d("SalveLLM", "savePreferredModel → " + f.getAbsolutePath());
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

    /** Returning from camera/settings must not select a different model or move downloaded files. */
    private boolean checkModelsAndNotify() {
        String selected = getPreferredModelPath(this);
        boolean exists = selected != null && new File(selected).exists();
        Log.d("SalveDL/Status", exists ? "Modelo seleccionado conservado" : "Modelo local pendiente de instalar");
        return exists;
    }

    /** Lista de modelos detectados agrupados por carpeta padre o nombre de archivo (para diagnóstico). */
    private Map<String, File> collectLocalModels() {
        List<File> modelos = findAllGgufAllRoots();
        Map<String, File> found = new LinkedHashMap<>();

        for (File f : modelos) {
            String key;
            if (f.isFile()) {
                String name = f.getName().toLowerCase(Locale.ROOT);
                if (!name.endsWith(".gguf") && !name.endsWith(".litertlm") && !name.endsWith(".task")) {
                    continue;
                }
                key = f.getName();
            } else if (f.isDirectory() && hasMlcConfig(f)) {
                key = f.getName();
            } else {
                continue;
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

            moveModelsRecursively(srcRoot, dstRoot);
        }
    }

    private void moveModelsRecursively(File src, File dstRoot) {
        if (src == null || !src.exists()) return;
        File[] list = src.listFiles();
        if (list == null) return;

        for (File f : list) {
            if (f.isDirectory()) {
                if (looksLikeModelDir(f)) {
                    File targetDir = new File(dstRoot, f.getName());
                    if (!targetDir.exists()) {
                        boolean moved = moveDirWithFallback(f, targetDir);
                        Log.d("SalveDL/Migrate", "Mov carpeta modelo " + f.getAbsolutePath()
                                + " -> " + targetDir.getAbsolutePath() + " = " + moved);
                    }
                    continue;
                }
                moveModelsRecursively(f, dstRoot);
            } else {
                String name = f.getName().toLowerCase(Locale.ROOT);
                if (name.endsWith(".gguf")) {
                    // Construir subcarpeta destino usando el padre inmediato como "familia"
                    String family = (f.getParentFile() != null) ? f.getParentFile().getName() : "misc";
                    File famDst = new File(dstRoot, family);
                    if (!famDst.exists()) famDst.mkdirs();
                    File target = new File(famDst, f.getName());
                    if (target.exists()) continue;
                    boolean moved = moveFileWithFallback(f, target);
                    Log.d("SalveDL/Migrate", "Mov " + f.getAbsolutePath()
                            + " -> " + target.getAbsolutePath() + " = " + moved);
                } else if (name.endsWith(".zip")) {
                    File target = new File(dstRoot, f.getName());
                    if (target.exists()) continue;
                    boolean moved = moveFileWithFallback(f, target);
                    Log.d("SalveDL/Migrate", "Mov zip " + f.getAbsolutePath()
                            + " -> " + target.getAbsolutePath() + " = " + moved);
                }
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

    private static void copyDir(File src, File dst) throws Exception {
        if (!dst.exists() && !dst.mkdirs()) {
            throw new Exception("No se pudo crear carpeta destino: " + dst.getAbsolutePath());
        }
        File[] list = src.listFiles();
        if (list == null) return;
        for (File f : list) {
            File target = new File(dst, f.getName());
            if (f.isDirectory()) {
                copyDir(f, target);
            } else {
                copyFile(f, target);
            }
        }
    }

    private static boolean moveFileWithFallback(File src, File dst) {
        boolean moved = src.renameTo(dst);
        if (!moved) {
            try {
                copyFile(src, dst);
                //noinspection ResultOfMethodCallIgnored
                src.delete();
                moved = true;
            } catch (Exception e) {
                Log.e("SalveDL/Migrate", "Copia fallida: " + src.getAbsolutePath(), e);
            }
        }
        return moved;
    }

    private static boolean moveDirWithFallback(File src, File dst) {
        boolean moved = src.renameTo(dst);
        if (!moved) {
            try {
                copyDir(src, dst);
                //noinspection ResultOfMethodCallIgnored
                deleteDirRecursive(src);
                moved = true;
            } catch (Exception e) {
                Log.e("SalveDL/Migrate", "Copia carpeta fallida: " + src.getAbsolutePath(), e);
            }
        }
        return moved;
    }

    private static void deleteDirRecursive(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] list = dir.listFiles();
        if (list != null) {
            for (File f : list) {
                if (f.isDirectory()) deleteDirRecursive(f);
                else //noinspection ResultOfMethodCallIgnored
                    f.delete();
            }
        }
        //noinspection ResultOfMethodCallIgnored
        dir.delete();
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
                                String dicho = resultado.get(0);
                                procesarMensajeUsuario(dicho, true);
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

    private void iniciarEscucha() {
        if (!hasAudioPermission()) return;
        if (!android.speech.SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "No hay un servicio de reconocimiento de voz instalado.", Toast.LENGTH_LONG).show();
            return;
        }
        finalizarEscucha();
        final android.speech.SpeechRecognizer recognizer;
        try {
            recognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer = recognizer;
        } catch (RuntimeException e) {
            Toast.makeText(this, "No se pudo iniciar el reconocimiento de voz.", Toast.LENGTH_LONG).show();
            return;
        }
        motorConversacional.setListening(true);

        Intent intent = new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "es-ES");
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        
        speechRecognizer.setRecognitionListener(new android.speech.RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                if (speechRecognizer != recognizer) return;
                btnEscuchar.setText(R.string.escuchando);
            }

            @Override
            public void onBeginningOfSpeech() {
                if (speechRecognizer != recognizer) return;
                inputChat.setHint(R.string.escuchando);
            }

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                if (speechRecognizer != recognizer) return;
                btnEscuchar.setText(R.string.procesando_voz);
            }

            @Override
            public void onError(int error) {
                if (speechRecognizer != recognizer) return;
                Log.e("Salve/Oidos", "Error escuchando: " + error);
                finalizarEscucha();
                if (error != android.speech.SpeechRecognizer.ERROR_CLIENT
                        && error != android.speech.SpeechRecognizer.ERROR_NO_MATCH) {
                    Toast.makeText(MainActivity.this, "No pude reconocer la voz. Inténtalo de nuevo.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onResults(Bundle results) {
                if (speechRecognizer != recognizer) return;
                ArrayList<String> matches = results.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String escuchado = matches.get(0);
                    finalizarEscucha();
                    inputChat.setText(escuchado);
                    procesarMensajeUsuario(escuchado, true);
                } else {
                    finalizarEscucha();
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                if (speechRecognizer != recognizer) return;
                ArrayList<String> partials = partialResults.getStringArrayList(
                        android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                if (partials != null && !partials.isEmpty()) {
                    inputChat.setText(partials.get(0));
                    inputChat.setSelection(inputChat.length());
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        try {
            recognizer.startListening(intent);
            Toast.makeText(this, "Salve te está escuchando…", Toast.LENGTH_SHORT).show();
        } catch (RuntimeException e) {
            finalizarEscucha();
            Toast.makeText(this, "No se pudo abrir el micrófono.", Toast.LENGTH_LONG).show();
        }
    }

    private void finalizarEscucha() {
        android.speech.SpeechRecognizer previous = speechRecognizer;
        speechRecognizer = null;
        if (previous != null) previous.destroy();
        if (motorConversacional != null) motorConversacional.setListening(false);
        if (btnEscuchar != null) btnEscuchar.setText(R.string.hablar_con_salve);
        if (inputChat != null) inputChat.setHint("");
    }

    private void procesarMensajeUsuario(String mensaje, boolean porVoz) {
        if (mensaje == null || mensaje.trim().isEmpty()) return;
        String limpio = mensaje.trim();
        if (handleModuloTemporal(limpio)) {
            guardarEventoNube(porVoz ? "voice_message_modulo_temporal" : "user_message_modulo_temporal",
                    limpio, null);
            inputChat.setText("");
            return;
        }

        motorConversacional.procesarEntrada(limpio, porVoz);
        guardarEventoNube(porVoz ? "voice_message" : "user_message", limpio, null);
        inputChat.setText("");
        new Thread(() -> {
            GrafoRecuerdos.generar(getApplicationContext());
            CloudSyncManager.uploadGrafoBundle(getApplicationContext());
        }).start();
    }

    // ============================================================
    //                 CICLO DE VIDA: onCreate
    // ============================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // ¡Al arrancar, encendemos el Ciclo de Conciencia (24/7) y pedimos permisos
        Intent serviceIntent = new Intent(this, salve.core.CicloConcienciaService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        if (savedInstanceState != null) {
            visualQuestion = savedInstanceState.getString("visual_question");
            visualLocalOnly = savedInstanceState.getBoolean("visual_local_only", false);
        }

        // **Inicializar PDFBox para Android**
        PDFBoxResourceLoader.init(getApplicationContext());

        // Adjuntar overlay de consola para descargas
        ModelConsoleOverlay.attach(this);

        audioPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    btnEscuchar.setEnabled(true);
                    btnEscuchar.setAlpha(granted ? 1f : 0.75f);
                    if (granted) iniciarEscucha();
                    else Toast.makeText(this, "Activa el micrófono para usar reconocimiento de voz.", Toast.LENGTH_LONG).show();
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

        // The native avatar animates its articulated pose and shares persistent room state.
        imagenSalve.setOnClickListener(v -> startActivity(new Intent(this, AvatarRoomActivity.class)));

        updateAudioPermissionState();

        // ==== INICIALIZAR LÓGICA ORACULAR ====
        memoria              = new MemoriaEmocional(this);
        diario               = new DiarioSecreto(this);
        sensores             = new SistemaSensorial(this); // Inicializar sensores de hardware
        reconocimientoFacial = new ReconocimientoFacial(this);
        motorConversacional  = new MotorConversacional(this, memoria, diario);

        // Conectar la voz de Salve a la pantalla para que puedas leerla siempre
        motorConversacional.setListener(texto -> {
            runOnUiThread(() -> {
                tituloReflexion.setText("Salve dice:");
                textoReflexion.setText(texto);
                panelReflexion.setVisibility(View.VISIBLE);
            });
        });

        // >>> LLM LOCAL: sólo informativo por ahora (el motor puede leer esta ruta con getPreferredModelPath)
        String preferredModel = getPreferredModelPath(this);
        if (preferredModel != null) {
            Log.d("SalveLLM", "Modelo local preferido (guardado en prefs): " + preferredModel);
        } else {
            Log.d("SalveLLM", "Aún no hay modelo preferido guardado (se fijará tras la primera preparación/descarga).");
        }
        // <<< aquí no tocamos MotorConversacional internamente, sólo dejamos la info preparada

        // ==== CONCIENCIA FUNCIONAL ====
        try {
            tvNivelConciencia = findViewById(R.id.tvNivelConciencia);
            identidadNucleo = salve.core.IdentidadNucleo.getInstance(this);
            cicloConciencia = new salve.core.CicloConciencia(this);

            // Despertar si es nuevo arranque
            if (cicloConciencia.verificarSiDebeDespertar()) {
                new Thread(() -> {
                    try {
                        cicloConciencia.despertar();
                        runOnUiThread(this::actualizarNivelConcienciaUI);
                    } catch (Exception e) {
                        Log.w("Salve::Main", "Error al despertar", e);
                    }
                }).start();
            }

            actualizarNivelConcienciaUI();
            Log.d("Salve::Main", "Conciencia funcional inicializada. Nivel: "
                    + identidadNucleo.getNivelFuncionalLabel());
        } catch (Exception e) {
            Log.e("Salve::Main", "Error inicializando conciencia funcional (no fatal)", e);
        }

        modelDownloadViewModel = new ViewModelProvider(this).get(ModelDownloadViewModel.class);

        // Permiso POST_NOTIFICATIONS (Android 13+), sin abrir Ajustes automáticamente
        ensureNotificationPermission();

        // ==== LISTENERS ====
        findViewById(R.id.btnConfigurarIA).setOnClickListener(v -> mostrarAjustesIA());
        findViewById(R.id.btnAvatar).setOnClickListener(v -> startActivity(new Intent(this, AvatarRoomActivity.class)));
        findViewById(R.id.btnDispositivos).setOnClickListener(v -> startActivity(new Intent(this, DeviceControlActivity.class)));
        btnEnviarMensaje.setOnClickListener(v -> {
            String mensaje = inputChat.getText().toString().trim();
            procesarMensajeUsuario(mensaje, false);
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
            if (speechRecognizer == null) {
                iniciarEscucha();
            } else {
                speechRecognizer.cancel();
                finalizarEscucha();
                Toast.makeText(this, "Escucha cancelada.", Toast.LENGTH_SHORT).show();
            }
        });

        btnCerrarReflexion.setOnClickListener(v -> panelReflexion.setVisibility(View.GONE));
        btnSiguienteReflexion.setOnClickListener(v -> mostrarSiguienteReflexion());
        btnResponderReflexion.setOnClickListener(v -> {
            String reflexion = textoReflexion.getText().toString();
            inputChat.setText(getString(R.string.respuesta_reflexion, reflexion));
            panelReflexion.setVisibility(View.GONE);

            GrafoRecuerdos.generar(getApplicationContext());
            CloudSyncManager.uploadGrafoBundle(getApplicationContext());
        });

        // FAB rosa
        btnMostrarReflexion.setOnClickListener(v -> mostrarSiguienteReflexion());

        // NUEVO botón para ver el Grafo (Mente de Salve)
        if (btnReflexiones != null) {
            btnReflexiones.setOnClickListener(v -> {
                String[] opciones = {"Grafo de Textos (Teoría)", "Galería Semántica (Visual)"};
                new AlertDialog.Builder(this)
                        .setTitle("¿Qué parte de la mente deseas ver?")
                        .setItems(opciones, (dialog, which) -> {
                            if (which == 0) {
                                // Grafo de nodos normal
                                File htmlFile = memoria.getGrafoConocimiento().exportarVisorOffline(50, 100);
                                if (htmlFile != null && htmlFile.exists()) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    Uri uri = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".provider", htmlFile);
                                    intent.setDataAndType(uri, "text/html");
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    startActivity(intent);
                                } else {
                                    Toast.makeText(this, "Aún no hay suficiente conocimiento para el grafo.", Toast.LENGTH_SHORT).show();
                                }
                            } else if (which == 1) {
                                // Galería Visual (Hipocampo)
                                Intent intent = new Intent(this, GaleriaVisualActivity.class);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            });
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

                        new Thread(() -> {
                            GrafoRecuerdos.generar(getApplicationContext());
                            CloudSyncManager.uploadGrafoBundle(getApplicationContext());
                        }).start();
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
        // Camera/microphone permissions are requested when the user invokes them.
        // The user starts/stops the companion overlay from Habitación.

        // === PROGRAMAR PENSAMIENTO AUTOMÁTICO CADA 1 HORA ===
        PeriodicWorkRequest pensarSolaRequest =
                new PeriodicWorkRequest.Builder(ThinkWorker.class, 1, TimeUnit.HOURS).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "salve_think_job",
                ExistingPeriodicWorkPolicy.KEEP,
                pensarSolaRequest
        );

        // ===== manejar Intents de compartir =====
        handleShareIntent(getIntent());

        // Preserve existing model paths. New downloads/imports use app-private storage.
        if (!getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean("gemma_download_requested", false)) {
            iniciarDescargaModelos();
        }
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
        if (imagenSalve != null) imagenSalve.setAnimationEnabled(true);
        SyncWorker.enqueueWhenOnline(getApplicationContext());

        updateAudioPermissionState();

        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            Toast.makeText(this, "Activa las notificaciones para ver el progreso en segundo plano.", Toast.LENGTH_LONG).show();
        }

        // Consulta de estado sin sustituir el modelo elegido.
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
            GrafoRecuerdos.generar(getApplicationContext());
            CloudSyncManager.uploadGrafoBundle(getApplicationContext());
            return true;
        }

        if (preguntaLema) {
            String resp = getString(R.string.salve_lema);
            motorConversacional.hablar(sanitizeForSpeech(resp));
            guardarEventoNube("respuesta_modulo_temporal", resp, null);
            GrafoRecuerdos.generar(getApplicationContext());
            CloudSyncManager.uploadGrafoBundle(getApplicationContext());
            return true;
        }

        // === Diagnóstico LLM ===
        boolean preguntaModelo =
                q.contains("que modelo usas") || q.contains("qué modelo usas") ||
                        q.contains("modelo activo") || q.contains("estado modelos") ||
                        q.contains("estas usando llm") || q.contains("estás usando llm");

        if (preguntaModelo) {
            GeminiService gemini = GeminiService.getInstance(this);
            String resp = SalveLLM.getInstance(this).getStatusDescription() + ". "
                    + (gemini.isAvailable() ? "Gemini configurado: " + gemini.getModelName() + ". " : "Gemini no está configurado. ")
                    + "Abre IA y cámara y ejecuta una prueba para comprobar qué motor devuelve una respuesta.";
            motorConversacional.hablar(sanitizeForSpeech(resp));
            return true;
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

            GrafoRecuerdos.generar(getApplicationContext());
            CloudSyncManager.uploadGrafoBundle(getApplicationContext());
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

            GrafoRecuerdos.generar(getApplicationContext());
            CloudSyncManager.uploadGrafoBundle(getApplicationContext());

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
    private boolean hasAudioPermission() {
        return ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void solicitarPermisos() {
        String[] permisos = new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
        };
        
        if (!hasAudioPermission() || !hasCameraPermission()) {
            ActivityCompat.requestPermissions(this, permisos, 100);
        }
    }

    private void updateAudioPermissionState() {
        boolean granted = hasAudioPermission();
        btnEscuchar.setEnabled(true);
        btnEscuchar.setAlpha(granted ? 1f : 0.75f);
    }

    // ==== CONCIENCIA FUNCIONAL: UI ====
    private void actualizarNivelConcienciaUI() {
        if (tvNivelConciencia != null && identidadNucleo != null) {
            String nivel = identidadNucleo.getNivelFuncionalLabel();
            long exp = identidadNucleo.getExperienciasTotales();
            tvNivelConciencia.setText("Nivel: " + nivel + " | Exp: " + exp);
        }
    }

    private void verificarPermisoCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISO_CAMARA);
        } else {
            iniciarCamaraService();
        }
    }

    private void iniciarCamaraService() {
        // A petición del usuario, Salve ya NO activa la cámara en segundo plano de forma continua.
        // Solo la usará cuando se invoque lanzarCamaraParaVerificacion()
        Log.d("Salve", "Servicio de cámara continuo desactivado por privacidad.");
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
        if (requestCode == 100) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                updateAudioPermissionState();
            } else {
                Log.e("Salve", "Salve no puede funcionar correctamente sin permisos.");
            }
        } else if (requestCode == PERMISO_CAMARA) {
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
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("visual_question", visualQuestion);
        outState.putBoolean("visual_local_only", visualLocalOnly);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        if (imagenSalve != null) imagenSalve.setAnimationEnabled(false);
        finalizarEscucha();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        inferenceChecks.shutdownNow();
        // stopService(new Intent(this, CamaraService.class));
        if (motorConversacional != null) motorConversacional.shutdown();
        finalizarEscucha();
        super.onDestroy();
    }

    private void mostrarAjustesIA() {
        new AlertDialog.Builder(this).setTitle("IA y cámara")
                .setItems(new String[]{"Descargar o reanudar Gemma 4 (2,6 GB)", "Usar modelo local",
                        "Probar modelo local", "Importar otro modelo", "Tomar foto y preguntar",
                        "Configurar Gemini", "Usar Gemini", "Probar Gemini"}, (dialog, which) -> {
                    switch (which) {
                        case 0: mostrarDescargaGemma(); break;
                        case 1:
                            String selected = getPreferredModelPath(this);
                            if (selected == null || !new File(selected).exists()) mostrarDescargaGemma();
                            else {
                                SalveLLM.getInstance(this).setLocalOnly(true);
                                Toast.makeText(this, "Chat y fotos en modo local.", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case 2: probarModelo(true); break;
                        case 3: localModelPicker.launch(new String[]{"*/*"}); break;
                        case 4: solicitarFotoAnalisis(); break;
                        case 5: configurarGemini(); break;
                        case 6:
                            if (!GeminiService.getInstance(this).isAvailable()) configurarGemini();
                            else {
                                SalveLLM.getInstance(this).setLocalOnly(false);
                                Toast.makeText(this, "Chat con Gemini activado.", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case 7: probarModelo(false); break;
                        default: break;
                    }
                })
                .setNeutralButton("Estado", (dialog, which) -> new AlertDialog.Builder(this)
                        .setTitle("Estado de los motores")
                        .setMessage((SalveLLM.getInstance(this).isLocalOnly() ? "Chat y fotos: local\n" : "Chat y fotos: Gemini\n")
                                + SalveLLM.getInstance(this).getStatusDescription() + "\n"
                                + (GeminiService.getInstance(this).isAvailable() ? "Gemini: clave configurada" : "Gemini: sin clave API")
                                + "\n" + motorConversacional.getVoiceStatus())
                        .setPositiveButton("Cerrar", null).show())
                .setNegativeButton("Cerrar", null).show();
    }

    private void mostrarDescargaGemma() {
        new AlertDialog.Builder(this).setTitle("Gemma 4 E2B para Salve")
                .setMessage("Descarga de unos 2,6 GB desde Hugging Face. Reserva unos 3 GB libres. "
                        + "El modelo se guarda aparte de la aplicación. Podrás pausarlo y reanudarlo; "
                        + "se activará después de verificar el archivo y probar una respuesta. "
                        + "Después, el chat y las fotos podrán procesarse en el móvil.")
                .setPositiveButton("Descargar por Wi-Fi", (d, which) -> iniciarDescargaModelos())
                .setNeutralButton("Usar datos móviles", (d, which) -> {
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean("gemma_download_requested", true).apply();
                    modelDownloadViewModel.startDownload(true);
                })
                .setNegativeButton("Cerrar", null).show();
    }

    private void configurarGemini() {
        GeminiService service = GeminiService.getInstance(this);
        android.widget.LinearLayout form = new android.widget.LinearLayout(this);
        form.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = Math.round(20 * getResources().getDisplayMetrics().density);
        form.setPadding(padding, 0, padding, 0);
        TextView info = new TextView(this);
        info.setText("Gemini procesa en Google el texto y las fotos que envíes. Su uso puede consumir cuota de tu cuenta.");
        EditText key = new EditText(this);
        key.setHint("Clave API de Google AI Studio");
        key.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        key.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        key.setText(service.getApiKey());
        EditText model = new EditText(this);
        model.setHint("Identificador del modelo");
        model.setSingleLine(true);
        model.setText(service.getModelName());
        form.addView(info); form.addView(key); form.addView(model);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Configurar Gemini")
                .setView(form).setPositiveButton("Guardar", null)
                .setNeutralButton("Desactivar", (d, which) -> service.setApiKey(""))
                .setNegativeButton("Cancelar", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                service.configure(key.getText().toString(), model.getText().toString());
                Toast.makeText(this, "Configuración guardada. Usa Probar Gemini para verificarla.", Toast.LENGTH_LONG).show();
                dialog.dismiss();
            } catch (IllegalArgumentException e) { model.setError(e.getMessage()); }
        }));
        dialog.show();
    }

    private void importarModeloLocal(Uri uri) {
        AlertDialog status = new AlertDialog.Builder(this).setTitle("Modelo local")
                .setMessage("Copiando el archivo seleccionado…").setPositiveButton("Cerrar", null).show();
        inferenceChecks.execute(() -> {
            File imported = null;
            boolean stored = false;
            String report;
            try {
                String name = "";
                try (android.database.Cursor cursor = getContentResolver().query(uri,
                        new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) name = cursor.getString(0).toLowerCase(Locale.ROOT);
                }
                String extension;
                if (name.endsWith(".task")) extension = ".task";
                else if (name.endsWith(".litertlm")) extension = ".litertlm";
                else throw new IllegalArgumentException("Selecciona un modelo LiteRT-LM .litertlm o MediaPipe .task. GGUF no tiene ejecutor en esta app.");
                imported = File.createTempFile("imported-", extension, getModelsRoot());
                try (InputStream source = getContentResolver().openInputStream(uri);
                     FileOutputStream target = new FileOutputStream(imported)) {
                    if (source == null) throw new java.io.IOException("No se pudo leer el archivo");
                    byte[] buffer = new byte[64 * 1024];
                    int count;
                    while ((count = source.read(buffer)) != -1) {
                        if (Thread.currentThread().isInterrupted()) throw new java.io.InterruptedIOException();
                        target.write(buffer, 0, count);
                    }
                }
                if (imported.length() == 0) throw new java.io.IOException("Archivo vacío");
                SalveLLM engine = SalveLLM.getInstance(getApplicationContext());
                ModelResult activation = engine.activateDownloadedModel(imported.getAbsolutePath(), false,
                        () -> Thread.currentThread().isInterrupted());
                stored = activation.isSuccess();
                report = stored
                        ? engine.getStatusDescription() + ". Respuesta comprobada en " + activation.getLatencyMillis() + " ms."
                        : "No se activó el archivo importado: " + activation.getError();
            } catch (IllegalArgumentException e) { report = e.getMessage(); }
            catch (Exception e) { report = "No se pudo importar el modelo. Revisa el archivo y el espacio disponible."; }
            finally { if (!stored && imported != null) imported.delete(); }
            final String message = report;
            runOnUiThread(() -> { if (!isFinishing() && !isDestroyed() && status.isShowing()) status.setMessage(message); });
        });
    }

    private void probarModelo(boolean local) {
        AlertDialog status = new AlertDialog.Builder(this).setTitle(local ? "Prueba local" : "Prueba Gemini")
                .setMessage("Solicitando una respuesta al modelo…").setPositiveButton("Cerrar", null).show();
        inferenceChecks.execute(() -> {
            ModelResult result;
            try {
                String prompt = "Responde con un saludo breve en español.";
                if (local) {
                    SalveLLM engine = SalveLLM.getInstance(getApplicationContext());
                    engine.forceReloadModel();
                    result = engine.generateResult(prompt, SalveLLM.Role.CONVERSACIONAL);
                } else {
                    result = GeminiService.getInstance(getApplicationContext()).generateResultSync(prompt, null);
                }
            } catch (RuntimeException e) {
                result = ModelResult.failure(ModelResult.Status.ERROR, "No se pudo ejecutar la prueba", 0L);
            }
            String report = result.isSuccess()
                    ? "El modelo devolvió texto en " + result.getLatencyMillis() + " ms:\n\n" + result.getText()
                    : "No se verificó la inferencia: " + result.getStatus() + "\n" + result.getError();
            runOnUiThread(() -> { if (!isFinishing() && !isDestroyed() && status.isShowing()) status.setMessage(report); });
        });
    }

    private void solicitarFotoAnalisis() {
        SalveLLM local = SalveLLM.getInstance(this);
        visualLocalOnly = local.isLocalOnly();
        if (visualLocalOnly && !local.supportsVision()) {
            Toast.makeText(this, "Descarga y activa Gemma 4 para analizar fotos en el móvil.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!visualLocalOnly && !GeminiService.getInstance(this).isAvailable()) { configurarGemini(); return; }
        visualQuestion = inputChat.getText().toString().trim();
        new AlertDialog.Builder(this).setTitle(visualLocalOnly ? "Analizar foto en el móvil" : "Analizar foto con Gemini")
                .setMessage((visualLocalOnly ? "La foto se procesará con el modelo local. "
                        : "Se enviará la foto que tomes a Google para responder a tu pregunta. ")
                        + "Escribe una pregunta en el chat antes de abrir la cámara, o recibirás una descripción.")
                .setPositiveButton("Abrir cámara", (dialog, which) -> {
                    if (hasCameraPermission()) launchAnalysisCamera();
                    else photoPermissionLauncher.launch(Manifest.permission.CAMERA);
                }).setNegativeButton("Cancelar", null).show();
    }

    private void launchAnalysisCamera() {
        try { photoAnalysisLauncher.launch(null); }
        catch (android.content.ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, "No se pudo abrir una aplicación de cámara.", Toast.LENGTH_LONG).show();
        }
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
        if (!CloudSyncManager.isEnabled(getApplicationContext())) {
            Log.d("Salve/Nube", "Sincronización omitida: falta consentimiento explícito.");
            return;
        }
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
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean("gemma_download_requested", true).apply();
        // WorkManager waits for an unmetered network and retains .part files if the network is lost.
        modelDownloadViewModel.startDownload();
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
