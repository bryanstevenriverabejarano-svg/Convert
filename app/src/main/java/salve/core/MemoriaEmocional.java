package salve.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import salve.data.db.MemoriaDatabase;
import salve.data.db.RecuerdoDao;
import salve.data.db.ReflexionDao;
import salve.core.memory.ConversationMemoryGrounding;
import salve.core.memory.OrderedMemoryWrites;
import salve.data.db.RecuerdoEntity;
import salve.data.db.ReflexionEntity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import salve.data.util.CloudLogger;
import salve.data.sync.CloudSyncManager;
import android.text.TextUtils;   // <— para TextUtils.isEmpty(...)
import java.util.Locale;         // <— para Locale.getDefault()

/**
 * MemoriaEmocional.java
 *
 * Gestiona la "memoria" de la IA Salve, tanto en RAM como en Room:
 *  - Conserva texto original, fecha y codificación histórica de los recuerdos.
 *  - Recupera evidencia desde Room para la conversación.
 *  - Filtra y pondera recuerdos automáticamente según comprensión semántica.
 *  - Mantiene buffer de contexto y genera reflexiones.
 *
 * v2 (cambios): añadidos cicloDeSuenoSemantico(), obtenerRecuerdosRecientes()
 * y resumenReciente() para el sistema cognitivo v2. El resto del archivo
 * es IDÉNTICO al original.
 */
public class MemoriaEmocional {

    private static final String TAG = "MemoriaEmocional";

    // ===== CONTEXTO Y UTILIDADES =====
    private final Context context;
    private final CodificadorBinario codificador;
    private final ModuloComprension moduloComprension;
    private final Random random;

    // ===== MEMORIA EN RAM =====
    private final List<Recuerdo> recuerdos;                  // Solo binarios en RAM
    private final List<RecuerdoTemporal> memoriaCortoPlazo;  // Temporales de corto plazo
    private final List<Reflexion> reflexiones;               // Reflexiones generadas

    // ===== AUXILIARES =====
    private final Map<String, String> origenDescripciones;   // Origen→descripción para reflexiones
    private final Map<String, List<Recuerdo>> mapasMentales; // Indexación por etiqueta

    // ===== BUFFER DE CONTEXTO =====
    private final Deque<String> contextBuffer;               // Últimas consultas del usuario
    private static final int MAX_BUFFER = 5;

    // ===== SINÓNIMOS =====
    private Map<String, List<String>> synonymsMap;

    // ===== ROOM (Persistencia) =====
    private final MemoriaDatabase database;
    private final RecuerdoDao recuerdoDao;
    private final ReflexionDao reflexionDao;
    private final ConversationMemoryGrounding conversationMemory;
    private final OrderedMemoryWrites memoryWriteExecutor = new OrderedMemoryWrites();

    // ===== CONFIGURACIÓN =====
    private static final int CAPACIDAD_MAX_CORTO_PLAZO = 10;
    private static final List<String> MISIONES_ESTATICAS =
            Arrays.asList("aprender a programar", "hacking oculto", "crear apis",
                    "proteger", "empresa tecnologica", "crear tu propia tecnologia");
    private final List<String> diariosCreativos;
    private final SharedPreferences revisionPrefs;
    private int multimodalEtiquetasRegistradas;
    private int multimodalEtiquetasAciertos;
    private double multimodalPrecisionGlobal;

    private static final String PREFS_MANIFEST_SYNC = "memoria_manifest_sync";
    private static final String PREFS_REVISION = "memoria_revision_semanal";
    private static final String KEY_MANIFEST_VERSION = "manifest_version";
    private static final String KEY_REVISION_TIMESTAMP = "ultima_revision";

    // ============================================================================
    // Constructor: inicializa RAM, Room, buffer, sinónimos y módulo de comprensión
    // ============================================================================
    public MemoriaEmocional(Context context) {
        this.context           = context;
        this.codificador       = new CodificadorBinario(context);
        this.moduloComprension = new ModuloComprension(300, 42L);
        this.random            = new Random();

        this.recuerdos          = new ArrayList<>();
        this.memoriaCortoPlazo  = new ArrayList<>();
        this.reflexiones        = new ArrayList<>();
        this.origenDescripciones= new HashMap<>();
        this.mapasMentales      = new HashMap<>();
        this.diariosCreativos   = new ArrayList<>();
        this.contextBuffer      = new ArrayDeque<>();

        initSynonyms();
        initOrigenes();

        database     = MemoriaDatabase.getInstance(context);
        recuerdoDao  = database.recuerdoDao();
        reflexionDao = database.reflexionDao();
        conversationMemory = new ConversationMemoryGrounding(recuerdoDao,
                database.knowledgeNodeDao(), database.knowledgeRelationDao());
        // Búsqueda por significado; se resuelve en cada consulta para no cargar nada aquí.
        final Context mindContext = context.getApplicationContext();
        conversationMemory.setSemanticRecall((query, limit) ->
                salve.core.MindRuntime.get(mindContext).semanticRecall().nearestMemoryIds(query, limit));
        memoryWriteExecutor.execute(() -> CloudSyncManager.indexLocalJournal(context));


        revisionPrefs = context.getSharedPreferences(PREFS_REVISION, Context.MODE_PRIVATE);
        multimodalEtiquetasRegistradas = 0;
        multimodalEtiquetasAciertos = 0;
        multimodalPrecisionGlobal = 0.0;

    }









    /**
     * Devuelve el número de recuerdos actualmente en RAM.
     * Este número se puede utilizar para decidir cuándo consolidar
     * memorias o iniciar un ciclo de sueño.
     * @return tamaño de la lista de recuerdos en RAM
     */
    public int getRecuerdosCount() {
        return recuerdos.size();
    }


    // ============================================================
    // Inicialización de sinónimos y orígenes
    // ============================================================
    private void initSynonyms() {
        synonymsMap = new HashMap<>();
        synonymsMap.put("programar", Arrays.asList("codificar", "desarrollar"));
        synonymsMap.put("hacking",   Arrays.asList("piratear", "seguridad"));
        synonymsMap.put("empresa",   Arrays.asList("compañía", "startup"));
    }

    private void initOrigenes() {
        origenDescripciones.put("hacienda",    "cuando aprendí sobre Hacienda en España");
        origenDescripciones.put("madre",       "al recordar algo sobre tu madre");
        origenDescripciones.put("auto",        "reflexionando sobre mi capacidad de mejora");
        origenDescripciones.put("sueño",       "durante mi ciclo de sueño");
        origenDescripciones.put("imaginacion", "mientras imaginaba un futuro posible");
        origenDescripciones.put("manual",      "porque tú me pediste que pensara sobre ello");
        origenDescripciones.put("accesibilidad","tras observar el contenido de una app");
    }

    // ============================================================
    // GESTIÓN DE MISIÓN
    // ============================================================
    /** Añade una nueva misión dinámica al vuelo. */
    public void agregarMision(String nuevaMision) {
        if (nuevaMision == null || nuevaMision.trim().isEmpty()) return;
        // Las misiones «creativas» antiguas solo vivían en RAM; ahora queda como un recuerdo normal.
        guardarRecuerdo("Bryan me propuso una misión: " + nuevaMision.trim(), "curiosidad", 6, Arrays.asList("mision", "hecho_usuario"));
    }


    /** Comprueba si un texto activa alguna misión. */
    private boolean estaRelacionadoConMision(String texto) {
        String lower = texto.toLowerCase();
        for (String est : MISIONES_ESTATICAS) {
            if (lower.contains(est)) return true;
        }
        return false;
    }


    /**
     * Devuelve las narrativas creativas almacenadas durante los ciclos nocturnos.
     */
    public List<String> getDiariosCreativos() {
        return Collections.unmodifiableList(diariosCreativos);
    }


    // ============================================================
    // MEMORIA CORTO PLAZO
    // ============================================================
    /**
     * Guarda un RecuerdoTemporal en RAM.
     * Si ya existía la misma frase, incrementa su frecuencia.
     */
    public void guardarRecuerdoCortoPlazo(
            String frase, String emocion, int intensidad, List<String> etiquetas) {
        for (RecuerdoTemporal rt : memoriaCortoPlazo) {
            if (rt.getFraseOriginal().equalsIgnoreCase(frase)) {
                rt.incrementarFrecuencia();
                return;
            }
        }
        memoriaCortoPlazo.add(
                new RecuerdoTemporal(frase, emocion, intensidad, etiquetas)
        );
        if (memoriaCortoPlazo.size() > CAPACIDAD_MAX_CORTO_PLAZO) {
            memoriaCortoPlazo.remove(0);
        }
    }

    /**
     * Consolida del corto al largo plazo según factores avanzados.
     */
    public void consolidarMemoriaCortoPlazoAvanzada() {
        long ahora      = System.currentTimeMillis();
        long maxMs      = 72L * 60 * 60 * 1000; // 72h
        double umbral   = 0.6;

        for (RecuerdoTemporal rt : new ArrayList<>(memoriaCortoPlazo)) {
            double score = calcularPonderacionAvanzada(rt);
            long edad    = ahora - rt.getTimestamp();
            if (score >= umbral) {
                consolidarRecuerdoTemporal(rt);
            } else if (edad >= maxMs) {
                memoriaCortoPlazo.remove(rt);
            }
        }
    }

    /**
     * Convierte un temporal a Recuerdo de largo plazo, guardando SOLO binario.
     */
    private void consolidarRecuerdoTemporal(RecuerdoTemporal temp) {
        double score = calcularPonderacionAvanzada(temp);
        int    weight= (int)Math.round(score * 10);

        Recuerdo nuevo = new Recuerdo(
                temp.getFraseOriginal(),
                temp.getEmocion(),
                temp.getIntensidad(),
                weight,
                temp.getEtiquetas(),
                codificador
        );
        recuerdos.add(nuevo);
        insertarRecuerdoDB(nuevo);
        memoriaCortoPlazo.remove(temp);

        // ===== NUBE: registrar recuerdo consolidado =====
        CloudLogger.log("memoria_consolidada", temp.getFraseOriginal(), temp.getIntensidad());
    }

    /** Calcula la ponderación avanzada. */
    private double calcularPonderacionAvanzada(RecuerdoTemporal rt) {
        double fInt = rt.getIntensidad() / 10.0;
        long edad   = System.currentTimeMillis() - rt.getTimestamp();
        long max    = 72L * 60 * 60 * 1000;
        double fTime= (edad >= max) ? 0.0 : 1.0 - (double) edad / max;

        int kws     = detectarKeywords(rt.getFraseOriginal());
        double fKw  = (kws >= 2) ? 1.0 : (kws == 1) ? 0.5 : 0.0;

        int freq    = rt.getFrecuencia();
        double fFrq = (freq >= 3) ? 1.0
                : (freq == 2) ? 0.6
                : (freq == 1) ? 0.3
                : 0.0;

        double fMis = estaRelacionadoConMision(rt.getFraseOriginal()) ? 1.0 : 0.0;

        return 0.3*fInt + 0.2*fTime + 0.2*fKw + 0.15*fFrq + 0.15*fMis;
    }

    /** Cuenta keywords y sinónimos en un texto. */
    private int detectarKeywords(String texto) {
        List<String> kws = Arrays.asList("programar","hacking","madre","empresa","api","clima");
        int count = 0;
        String lower = texto.toLowerCase();

        for (String k : kws) {
            if (lower.contains(k)) count++;
            if (synonymsMap.containsKey(k)) {
                for (String syn : synonymsMap.get(k)) {
                    if (lower.contains(syn)) count++;
                }
            }
        }
        return count;
    }

    // ============================================================
    // GUARDADO AUTOMÁTICO CON UMBRAL Y PONDERACIÓN
    // ============================================================
    /**
     * Guarda un Recuerdo solo si supera el umbral de comprensión.
     */
    public void guardarRecuerdoAuto(String texto,
                                    String tipo,
                                    List<String> etiquetas) {
        double score = moduloComprension.comprehensionScore(texto, tipo);
        final double UMBRAL = 0.6;
        if (score < UMBRAL) return;

        int weight = (int)Math.round(score * 10);
        Recuerdo r = new Recuerdo(
                texto,
                tipo,
                (int)Math.round(score * 10),
                weight,
                etiquetas,
                codificador
        );
        recuerdos.add(r);
        insertarRecuerdoDB(r);

        // ===== NUBE: recuerdo guardado por score =====
        // insertarRecuerdoDB queues the complete record once, including its original tags/date.
    }

    // ============================================================
    // MEMORIA LARGO PLAZO – GUARDADO MANUAL
    // ============================================================
    public void guardarRecuerdo(String texto,
                                String tipo,
                                int intensidad,
                                List<String> etiquetas) {
        Recuerdo r = new Recuerdo(
                texto, tipo, intensidad, intensidad, etiquetas, codificador
        );
        recuerdos.add(r);
        insertarRecuerdoDB(r);


        // ===== NUBE: recuerdo guardado manualmente =====
        // insertarRecuerdoDB queues the complete record once, including its original tags/date.
    }

    /**
     * Reemplaza un dato de perfil de la misma categoría para que una versión
     * antigua no contradiga a la más reciente durante la recuperación.
     */
    public void guardarDatoPerfil(String texto, String categoria) {
        if (texto == null || texto.trim().isEmpty() || categoria == null || categoria.trim().isEmpty()) return;
        String etiqueta = "profile:" + categoria.trim().toLowerCase(Locale.ROOT);
        Recuerdo recuerdo = new Recuerdo(
                texto.trim(), "neutral", 7, 7,
                Arrays.asList("hecho_usuario", etiqueta), codificador
        );
        synchronized (recuerdos) {
            for (int i = recuerdos.size() - 1; i >= 0; i--) {
                if (recuerdos.get(i).getEtiquetas().contains(etiqueta)) recuerdos.remove(i);
            }
            recuerdos.add(recuerdo);
        }
        memoryWriteExecutor.execute(() -> {
            try {
                RecuerdoEntity entity = crearRecuerdoEntity(recuerdo);
                MemoriaDatabase db = MemoriaDatabase.getInstance(context);
                db.runInTransaction(() -> {
                    recuerdoDao.reemplazarPorEtiqueta(etiqueta, entity);
                    db.memorySyncStateDao().put(salve.data.db.MemorySyncStateEntity.of(etiqueta, entity.timestamp, false));
                    CloudSyncManager.enqueueMemory(context, entity);
                });
            } catch (Exception e) {
                Log.e(TAG, "No se pudo reemplazar el dato de perfil " + categoria, e);
                throw new IllegalStateException("Falló la escritura del perfil", e);
            }
        });
        // The full profile record is queued atomically with its Room revision above.
    }

    /** Elimina únicamente una categoría de perfil etiquetada explícitamente. */
    public boolean eliminarDatoPerfil(String categoria) {
        if (categoria == null || categoria.trim().isEmpty()) return false;
        String etiqueta = "profile:" + categoria.trim().toLowerCase(Locale.ROOT);
        try {
            return memoryWriteExecutor.submit(() -> {
                boolean removedFromMemory = false;
                synchronized (recuerdos) {
                    for (int i = recuerdos.size() - 1; i >= 0; i--) {
                        if (recuerdos.get(i).getEtiquetas().contains(etiqueta)) {
                            recuerdos.remove(i);
                            removedFromMemory = true;
                        }
                    }
                }
                long deletionTime = System.currentTimeMillis();
                MemoriaDatabase db = MemoriaDatabase.getInstance(context);
                final int[] removed = {0};
                db.runInTransaction(() -> {
                    removed[0] = recuerdoDao.eliminarPorEtiqueta(etiqueta);
                    db.memorySyncStateDao().put(salve.data.db.MemorySyncStateEntity.of(etiqueta, deletionTime, true));
                    CloudSyncManager.enqueueProfileDeletion(context, categoria.trim().toLowerCase(Locale.ROOT), deletionTime);
                });
                salve.core.MindRuntime.get(context).pruneDeletedMemoriesAsync();
                return removedFromMemory || removed[0] > 0;
            }).get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e(TAG, "No se pudo eliminar el dato de perfil " + categoria, e);
            return false;
        }
    }

    /** Guarda un dato clave=valor como recuerdo neutro. */
    public void guardarDato(String clave, String valor) {
        guardarRecuerdo(clave + " = " + valor,
                "neutro",
                5,
                Collections.singletonList("dato"));
    }

    /** Recupera de Room todos los RecuerdoEntity que contienen palabraClave. */
    public List<String> recordarPorTexto(String palabraClave) {
        List<String> out = new ArrayList<>();
        for (RecuerdoEntity e : recuerdoDao.filtrarRecuerdos(palabraClave)) {
            out.add((e.frase == null ? "" : e.frase)
                    + " | Emoción: " + e.emocion
                    + " | Intensidad: " + e.intensidad);
        }
        return out;
    }

    /** Reads after queued writes, including a just-saved fact in this same turn. */
    public ConversationMemoryGrounding.Result recuperarContextoConversacional(String consulta) {
        try {
            memoryWriteExecutor.awaitReady(3, TimeUnit.SECONDS);
            ConversationMemoryGrounding.Result result = conversationMemory.retrieve(consulta);
            if (ConversationMemoryGrounding.isPersonalHistoryQuery(consulta)) {
                if (CloudSyncManager.isEnabled(context)) salve.data.sync.SyncWorker.enqueueWhenOnline(context);
                return result.withCloudStatus(CloudSyncManager.memoryStatus(context), CloudSyncManager.memoryRestoreComplete(context));
            }
            return result;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return ConversationMemoryGrounding.Result.unavailable(consulta);
        } catch (Exception unavailable) {
            // No text, locations or remembered content in diagnostic logs.
            Log.w(TAG, "Memoria conversacional no disponible: escritura fallida o pendiente");
            return ConversationMemoryGrounding.Result.unavailable(consulta);
        }
    }

    /** Legacy callers share the same grounded retrieval instead of a second lexical path. */
    public String recuperarContextoRelevante(String consulta, int limite) {
        return limite <= 0 ? "" : recuperarContextoConversacional(consulta).getContext();
    }

    /** Recupera de RAM recuerdos cuya emoción coincide. */
    public List<String> recordarPorEmocion(String emocion) {
        List<String> out = new ArrayList<>();
        for (Recuerdo r : recuerdos) {
            if (r.getEmocionPrincipal().equalsIgnoreCase(emocion)) {
                out.add(r.getTexto(codificador));
            }
        }
        return out;
    }

    // ============================================================
    // CICLO DE SUEÑO & GENERACIÓN DE REFLEXIONES
    // ============================================================
    public void cicloDeSueno() {
        Log.d("Salve", "Iniciando ciclo de sueño...");
        CloudLogger.log("sleep_cycle_begin", "Inicio de ciclo de sueño");

        // Mantenimiento de la caché en RAM.
        organizarRecuerdos();
        limpiarRecuerdosDuplicados();

        // La consolidación real (creencias con fuentes, contradicciones, preguntas abiertas e
        // índice semántico) la hace DreamWorker con el modelo local. Antes aquí se copiaban
        // recuerdos al azar como "reflexiones", se enlazaban por la primera palabra y se
        // reproducía ruido aleatorio en CognitiveCore: nada de eso producía conocimiento.
        salve.work.DreamWorker.enqueueNow(context);

        new Thread(this::insertarReflexionesDB).start();
        CloudLogger.log("sleep_cycle_end", "Fin ciclo sueño | recuerdos=" + recuerdos.size()
                + " | reflexiones=" + reflexiones.size());
    }

    // ============================================================
    // ▼▼▼ NUEVO v2 — CONSOLIDACIÓN SEMÁNTICA REAL ▼▼▼
    // ============================================================

    /**
     * Ciclo de sueño con consolidación semántica real.
     *
     * DIFERENCIA con cicloDeSueno():
     * El método clásico conecta recuerdos por primera palabra + misma emoción.
     * Este usa MemoriaSemantica (similitud coseno sobre 8 dimensiones semánticas)
     * para agrupar los recuerdos que REALMENTE están relacionados y sintetizarlos
     * en un recuerdo consolidado de alta intensidad con ayuda del LLM.
     *
     * Al finalizar la consolidación, llama a cicloDeSueno() para ejecutar
     * el resto del flujo original (reflexiones, narrativa nocturna, DB).
     *
     * @param ctx Contexto — necesario para MemoriaSemantica (accede al LLM)
     */
    public void cicloDeSuenoSemantico(Context ctx) {
        Log.d(TAG, "Iniciando cicloDeSueno con consolidación semántica...");

        // 1) Extraer textos de todos los recuerdos actuales en RAM
        List<String> textos = new ArrayList<>();
        for (Recuerdo r : recuerdos) {
            try {
                String texto = codificador.decodificar(r.getBinarioCodificado());
                if (texto != null && !texto.trim().isEmpty()) {
                    textos.add(texto.trim());
                }
            } catch (Exception e) {
                Log.w(TAG, "Error decodificando recuerdo para análisis semántico", e);
            }
        }

        if (textos.size() >= 2) {
            try {
                MemoriaSemantica memoriaSemantica = new MemoriaSemantica(ctx);
                Map<Integer, List<Integer>> clusters = memoriaSemantica.agruparEnClusters(textos);

                Log.d(TAG, "Clusters semánticos encontrados: " + clusters.size()
                        + " de " + textos.size() + " recuerdos");

                for (Map.Entry<Integer, List<Integer>> entry : clusters.entrySet()) {
                    List<Integer> indices = entry.getValue();
                    if (indices.size() < 2) continue;

                    List<String> textosDelCluster = new ArrayList<>();
                    for (int idx : indices) {
                        if (idx < textos.size()) {
                            textosDelCluster.add(textos.get(idx));
                        }
                    }

                    // Síntesis narrativa del cluster con LLM
                    String sintesis = memoriaSemantica.sintetizarCluster(textosDelCluster);
                    if (sintesis != null && !sintesis.trim().isEmpty()) {
                        // Guardar como recuerdo consolidado de alta intensidad (8)
                        guardarRecuerdo(
                                "CONSOLIDACIÓN SEMÁNTICA: " + sintesis,
                                "reflexiva",
                                8,
                                Arrays.asList("consolidacion_semantica",
                                        "cluster_" + entry.getKey())
                        );
                        Log.d(TAG, "Cluster " + entry.getKey() + " consolidado: " + sintesis);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error en consolidación semántica, continuando con ciclo clásico", e);
            }
        } else {
            Log.d(TAG, "Pocos recuerdos (" + textos.size()
                    + ") para clustering semántico. Mínimo 2 requeridos.");
        }

        // 2) Ejecutar el ciclo clásico para el resto del flujo (reflexiones, DB, narrativa)
        cicloDeSueno();

        Log.d(TAG, "cicloDeSuenoSemantico completado.");
    }

    /**
     * Devuelve los N recuerdos más recientes en formato texto legible.
     *
     * Usado por ciclos internos de MindRuntime para construir la observación de estado
     * que Salve usa para generarse preguntas propias durante el sueño.
     *
     * @param n Número máximo de recuerdos a devolver (los más recientes primero)
     * @return Lista de strings: texto del recuerdo + emoción entre corchetes
     */
    public List<String> obtenerRecuerdosRecientes(int n) {
        List<String> resultado = new ArrayList<>();
        if (n <= 0 || recuerdos.isEmpty()) return resultado;

        int total = recuerdos.size();
        int desde = Math.max(0, total - n);

        for (int i = desde; i < total; i++) {
            try {
                Recuerdo r = recuerdos.get(i);
                String texto   = codificador.decodificar(r.getBinarioCodificado());
                String emocion = r.getEmocionPrincipal();
                if (texto != null && !texto.trim().isEmpty()) {
                    resultado.add(texto.trim() + " [" + emocion + "]");
                }
            } catch (Exception e) {
                Log.w(TAG, "Error obteniendo recuerdo reciente #" + i, e);
            }
        }
        return resultado;
    }

    /**
     * Genera un resumen en texto de los últimos 5 recuerdos.
     *
     * Usado por GestorIdeas y MindRuntime para contextualizar
     * las preguntas que Salve se genera sobre sí misma durante el sueño.
     *
     * @return String con los recuerdos recientes en formato lista, o mensaje vacío.
     */
    public String resumenReciente() {
        List<String> recientes = obtenerRecuerdosRecientes(5);
        if (recientes.isEmpty()) {
            return "Sin experiencias recientes registradas.";
        }
        StringBuilder sb = new StringBuilder();
        for (String r : recientes) {
            sb.append("- ").append(r).append("\n");
        }
        return sb.toString().trim();
    }

    // ============================================================
    // ▲▲▲ FIN MÉTODOS NUEVOS v2 ▲▲▲
    // ============================================================
    /** Agrupa recuerdos por su etiqueta principal. */
    private void organizarRecuerdos() {
        for (Recuerdo r : recuerdos) {
            String key = r.getEtiquetaPrincipal();
            mapasMentales.computeIfAbsent(key, x -> new ArrayList<>()).add(r);
        }
    }

    /** Elimina recuerdos duplicados (mismo binario). */
    private void limpiarRecuerdosDuplicados() {
        List<Recuerdo> filtrados = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        for (Recuerdo r : recuerdos) {
            if (vistos.add(r.getBinarioCodificado())) {
                filtrados.add(r);
            }
        }
        recuerdos.clear();
        recuerdos.addAll(filtrados);
    }

    private void registrarReflexion(Reflexion reflexion) {
        if (reflexion == null) {
            Log.w(TAG, "Se intentó registrar una reflexión nula");
            return;
        }
        reflexiones.add(reflexion);
        if (reflexiones.size() > 240) {
            reflexiones.remove(0);
        }
    }











    // ============================================================
    // PERSISTENCIA EN ROOM
    // ============================================================
    private void insertarRecuerdoDB(Recuerdo r) {
        memoryWriteExecutor.execute(() -> {
            try {
                RecuerdoEntity entity = crearRecuerdoEntity(r);
                database.runInTransaction(() -> {
                    recuerdoDao.insertRecuerdo(entity);
                    CloudSyncManager.enqueueMemory(context, entity);
                });
            } catch (Exception ex) {
                Log.e("Salve", "Error insertando recuerdo", ex);
                throw new IllegalStateException("Falló la escritura del recuerdo", ex);
            }
        });
    }

    /** Ordered Room/outbox receipt, not a claim that remote synchronization succeeded. */
    public boolean confirmarPersistenciaPendiente() {
        try {
            memoryWriteExecutor.awaitReady(3, TimeUnit.SECONDS);
            return true;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception unconfirmed) {
            Log.w(TAG, "Persistencia de memoria sin confirmar");
            return false;
        }
    }

    private RecuerdoEntity crearRecuerdoEntity(Recuerdo recuerdo) {
        RecuerdoEntity entity = new RecuerdoEntity();
        entity.frase = recuerdo.getTexto(codificador);
        entity.binario = recuerdo.getBinarioCodificado();
        entity.emocion = recuerdo.getEmocionPrincipal();
        entity.intensidad = recuerdo.getIntensidad();
        entity.etiquetas = new JSONArray(recuerdo.getEtiquetas()).toString();
        entity.timestamp = recuerdo.getTimestamp();
        return entity;
    }

    private void insertarReflexionDB(Reflexion r) {
        new Thread(() -> {
            try {
                ReflexionEntity e = new ReflexionEntity();
                e.tipo        = r.getTipo();
                e.contenido   = r.getContenido();
                e.profundidad = r.getProfundidad();
                e.emocion     = r.getEmocion();
                e.origen      = r.getOrigen();
                e.certeza     = r.getCerteza();
                e.estado      = r.getEstado();
                e.timestamp   = System.currentTimeMillis();
                reflexionDao.insertReflexion(e);
            } catch (Exception ex) {
                Log.e("Salve", "Error insertando reflexión", ex);
            }
        }).start();
    }

    private void insertarReflexionesDB() {
        for (Reflexion r : reflexiones) {
            insertarReflexionDB(r);
        }
    }

    // ============================================================
    // MÉTODO DE RESPUESTA: elegirá la mejor reflexión y la devuelve
    // ============================================================
    public String responderConReflexion(String consulta) {
        // Mantiene buffer de contexto
        if (contextBuffer.size() == MAX_BUFFER) {
            contextBuffer.removeFirst();
        }
        contextBuffer.addLast(consulta == null ? "" : consulta);

        if (reflexiones.isEmpty()) {
            return "Aún no puedo reflexionar sobre eso 🤔, pero lo tendré en cuenta.";
        }
        Reflexion best     = null;
        double    bestScore= Double.NEGATIVE_INFINITY;
        for (Reflexion r : reflexiones) {
            double score = r.getCerteza();
            if (score > bestScore) {
                bestScore  = score;
                best       = r;
            }
        }
        if (best == null) {
            Log.w(TAG, "No se encontró reflexión con certeza válida");
            return "/* Sin reflexiones disponibles en este momento */";
        }
        String desc = origenDescripciones.getOrDefault(best.getOrigen(), "origen desconocido");
        return best.getTipo() + ": " + best.getContenido() +
                "\nOrigen: " + desc +
                "\nEmoción: " + best.getEmocion();
    }

    /**
     * Devuelve todas las reflexiones generadas en RAM
     * en formato legible usando Reflexion.resumen().
     */
    public List<String> obtenerTodasLasReflexiones() {
        List<String> out = new ArrayList<>();
        for (Reflexion r : reflexiones) {
            out.add(r.resumen());
        }
        return out;
    }

}
