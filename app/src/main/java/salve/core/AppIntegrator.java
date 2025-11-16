package salve.core;
import dalvik.system.DexClassLoader;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.room.Room;

import salve.data.db.MemoriaDatabase;
import salve.data.db.PluginDao;
import salve.data.db.PluginEntity;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AppIntegrator.java
 * ------------------
 * Escanea múltiples orígenes (Room, APK propio, otros APKs instalados
 * y directorio interno) para descubrir plugins dinámicos,
 * los carga con DexClassLoader, los registra en PluginManager
 * y persiste o limpia sus metadatos en Room.
 */
public class AppIntegrator {
    private static final String TAG = "AppIntegrator";
    private static final float SCORE_THRESHOLD = 0.5f;

    private final Context context;
    private final PluginManager pluginManager;
    private final PluginDao pluginDao;

    /**
     * Constructor.
     * Inicializa PluginManager y PluginDao desde Room.
     */
    public AppIntegrator(Context context) {
        this.context       = context.getApplicationContext();
        this.pluginManager = new PluginManager();

        // Construir o obtener instancia de Room y su DAO de plugins
        MemoriaDatabase db = Room.databaseBuilder(
                        this.context,
                        MemoriaDatabase.class,
                        "memoria.db"
                )
                .fallbackToDestructiveMigration()
                .build();
        this.pluginDao = db.pluginDao();
    }

    /**
     * Punto de entrada de la integración:
     * 1) Carga plugins ya en Room.
     * 2) Escanea directorio interno /files/plugins.
     * 3) Escanea el APK propio.
     * 4) Escanea otras apps instaladas.
     * 5) Limpia plugins obsoletos de Room.
     */
    public void discoverAndIntegrate() {
        // Recoger todos los nombres vistos para evitar duplicados
        Set<String> seenNames = new HashSet<>();

        // 1) Re‑registrar plugins persistidos
        List<PluginEntity> persisted = pluginDao.getAllPlugins();
        for (PluginEntity e : persisted) {
            try {
                // Intentar instanciar el plugin usando DexClassLoader
                loadAndRegisterFromPath(e.name, e.filePath);
                seenNames.add(e.name);
                Log.d(TAG, "Re‑registrado plugin persistido: " + e.name);
            } catch (Exception ex) {
                // Si no se puede cargar, eliminar registro obsoleto
                pluginDao.deletePlugin(e.name);
                Log.w(TAG, "Eliminado plugin obsoleto de Room: " + e.name, ex);
            }
        }

        // 2) Escanear .dex en /files/plugins
        File pluginDir = new File(context.getFilesDir(), "plugins");
        if (pluginDir.exists() && pluginDir.isDirectory()) {
            File[] dexFiles = pluginDir.listFiles((d, n) -> n.endsWith(".dex"));
            if (dexFiles != null) {
                for (File dex : dexFiles) {
                    String classPath = dex.getAbsolutePath();
                    if (!seenNames.contains(classPath)) {
                        if (loadAndPersistDex(dex)) {
                            seenNames.add(classPath);
                        }
                    }
                }
            }
        }

        // 3) Escanear el APK propio
        scanApkForPlugins(context.getPackageCodePath(), seenNames);

        // 4) Escanear otras apps instaladas
        scanInstalledAppsForPlugins(seenNames);

        // 5) Eliminar registros de plugins ya no descubiertos
        removeObsoletePlugins(seenNames);
    }

    /**
     * Usa DexClassLoader para cargar e instanciar una clase SavePlugin
     * dada su ruta de APK o .dex y nombre de clase.
     * No persiste en Room (solo registra en memoria).
     */
    private void loadAndRegisterFromPath(String className, String path) throws Exception {
        File opt = context.getDir("outdex", Context.MODE_PRIVATE);
        DexClassLoader loader = new DexClassLoader(
                path,
                opt.getAbsolutePath(),
                null,
                context.getClassLoader()
        );
        Class<?> cls = loader.loadClass(className);
        if (SavePlugin.class.isAssignableFrom(cls)) {
            SavePlugin plugin = (SavePlugin) cls.getConstructor().newInstance();
            pluginManager.register(plugin);
        }
    }

    /**
     * Carga un .dex externo, registra en memoria y persiste en Room.
     * @return true si se registró y guardó exitosamente.
     */
    private boolean loadAndPersistDex(File dexFile) {
        try {
            File opt = context.getDir("outdex", Context.MODE_PRIVATE);
            DexClassLoader loader = new DexClassLoader(
                    dexFile.getAbsolutePath(),
                    opt.getAbsolutePath(),
                    null,
                    context.getClassLoader()
            );
            List<String> candidates = PluginIndexReader.read(dexFile.getAbsolutePath());
            for (String clsName : candidates) {
                Class<?> cls = loader.loadClass(clsName);
                if (SavePlugin.class.isAssignableFrom(cls)) {
                    SavePlugin plugin = (SavePlugin) cls.getConstructor().newInstance();
                    float score = plugin.score();
                    if (score > SCORE_THRESHOLD) {
                        pluginManager.register(plugin);
                        // Persistir metadatos
                        PluginEntity entity = new PluginEntity(
                                clsName,
                                "1.0",
                                dexFile.getAbsolutePath(),
                                score,
                                System.currentTimeMillis()
                        );
                        pluginDao.insertPlugin(entity);
                        Log.i(TAG, "Plugin .dex persistido: " + clsName);
                    }
                }
            }
            return true;
        } catch (IOException | ReflectiveOperationException e) {
            Log.w(TAG, "Error cargando .dex: " + dexFile.getName(), e);
        }
        return false;
    }

    /**
     * Escanea un APK (propio o de otra app) para descubrir plugins embebidos.
     */
    private void scanApkForPlugins(String apkPath, Set<String> seenNames) {
        try {
            File opt = context.getDir("outdex", Context.MODE_PRIVATE);
            DexClassLoader loader = new DexClassLoader(
                    apkPath,
                    opt.getAbsolutePath(),
                    null,
                    context.getClassLoader()
            );
            List<String> candidates = PluginIndexReader.read(apkPath);
            for (String clsName : candidates) {
                if (seenNames.contains(clsName)) continue;
                Class<?> cls = loader.loadClass(clsName);
                if (SavePlugin.class.isAssignableFrom(cls)) {
                    SavePlugin plugin = (SavePlugin) cls.getConstructor().newInstance();
                    float score = plugin.score();
                    if (score > SCORE_THRESHOLD) {
                        pluginManager.register(plugin);
                        PluginEntity entity = new PluginEntity(
                                clsName,
                                "1.0",
                                apkPath,
                                score,
                                System.currentTimeMillis()
                        );
                        pluginDao.insertPlugin(entity);
                        Log.i(TAG, "Plugin APK persistido: " + clsName);
                        seenNames.add(clsName);
                    }
                }
            }
        } catch (IOException | ReflectiveOperationException e) {
            Log.w(TAG, "No se encontró índice o error en APK: " + apkPath, e);
        }
    }

    /**
     * Recorre todas las aplicaciones instaladas y trata cada APK
     * como fuente de plugins embebidos.
     */
    private void scanInstalledAppsForPlugins(Set<String> seenNames) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
        for (ApplicationInfo ai : apps) {
            // Omitir la propia aplicación
            if (ai.packageName.equals(context.getPackageName())) continue;
            scanApkForPlugins(ai.sourceDir, seenNames);
        }
    }

    /**
     * Elimina de Room cualquier PluginEntity cuyo nombre
     * no esté presente en seenNames.
     */
    private void removeObsoletePlugins(Set<String> seenNames) {
        List<PluginEntity> all = pluginDao.getAllPlugins();
        for (PluginEntity e : all) {
            if (!seenNames.contains(e.name)) {
                pluginDao.deletePlugin(e.name);
                Log.d(TAG, "Plugin obsoleto eliminado: " + e.name);
            }
        }
    }

    /**
     * Obtiene la lista de plugins actualmente registrados en memoria.
     */
    public List<SavePlugin> getRegisteredPlugins() {
        return pluginManager.getPlugins();
    }
}