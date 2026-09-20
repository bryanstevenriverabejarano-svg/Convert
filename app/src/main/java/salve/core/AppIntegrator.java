package salve.core;
import dalvik.system.DexClassLoader;
import android.content.Context;
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
 * Descubre únicamente plugins incluidos en el APK propio firmado,
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
     * Carga únicamente metadatos cuyo origen sea el APK propio y elimina
     * registros dinámicos antiguos. No escanea ni ejecuta .dex descargados.
     */
    public void discoverAndIntegrate() {
        // Recoger todos los nombres vistos para evitar duplicados
        Set<String> seenNames = new HashSet<>();
        String ownApkPath = context.getPackageCodePath();

        // 1) Re‑registrar plugins persistidos
        List<PluginEntity> persisted = pluginDao.getAllPlugins();
        for (PluginEntity e : persisted) {
            if (!ownApkPath.equals(e.filePath)) {
                pluginDao.deletePlugin(e.name);
                Log.w(TAG, "Plugin dinámico eliminado; producción solo permite el APK propio: " + e.name);
                continue;
            }
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

        // Escanear exclusivamente el APK instalado y firmado.
        scanApkForPlugins(ownApkPath, seenNames);

        // 4) Eliminar registros de plugins ya no descubiertos. Nunca se carga
        // codigo desde APKs de terceros instalados en el dispositivo.
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
