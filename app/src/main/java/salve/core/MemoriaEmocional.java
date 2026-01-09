package salve.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import salve.data.db.MemoriaDatabase;
import salve.data.db.RecuerdoDao;
import salve.data.db.ReflexionDao;
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
import android.text.TextUtils;   // <— para TextUtils.isEmpty(...)
import java.util.Locale;         // <— para Locale.getDefault()
import salve.core.SalveLLM;
import salve.core.Reflexion;

/**
 * MemoriaEmocional.java
 *
 * Gestiona la “memoria” de la IA Salve, tanto en RAM como en Room:
 *  - Guarda SOLO versiones binarias de los recuerdos (ahorro de espacio).
 *  - Decodifica a texto solo cuando sea necesario.
 *  - Filtra y pondera recuerdos automáticamente según comprensión semántica.
 *  - Mantiene buffer de contexto y genera reflexiones.
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

    // ===== CONFIGURACIÓN =====
    private static final int CAPACIDAD_MAX_CORTO_PLAZO = 10;
    private static final List<String> MISIONES_ESTATICAS =
            Arrays.asList("aprender a programar", "hacking oculto", "crear apis",
                    "proteger", "empresa tecnologica", "crear tu propia tecnologia");
    private final List<MisionCreativa> misionesCreativas;
    private final List<String> diariosCreativos;
    private final List<AutoCriticaCreativa> autoCriticas;
    private final List<RevisionSemanalCreativa> revisionesSemanales;
    private final List<MultimodalSignal> senalesMultimodales;
    private final List<BlueprintAprendizajeContinuo> learningBlueprints;
    private final GrafoConocimientoVivo grafoConocimiento;
    private final PanelMetricasCreatividad panelMetricas;
    private final ZonaReservada zonaReservada;
    private final SharedPreferences manifestSyncPrefs;
    private final SharedPreferences revisionPrefs;
    private final SharedPreferences reliquiasPrefs;
    private final List<GlifoReliquia> reliquias;
    private int multimodalEtiquetasRegistradas;
    private int multimodalEtiquetasAciertos;
    private double multimodalPrecisionGlobal;

    private static final String PREFS_MANIFEST_SYNC = "memoria_manifest_sync";
    private static final String PREFS_REVISION = "memoria_revision_semanal";
    private static final String PREFS_RELIQUIAS = "salve_reliquias";
    private static final String KEY_MANIFEST_VERSION = "manifest_version";
    private static final String KEY_REVISION_TIMESTAMP = "ultima_revision";
    private static final String KEY_RELIQUIAS_GLIFOS = "salve_reliquias_glifos";

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
        this.misionesCreativas  = new ArrayList<>();
        this.diariosCreativos   = new ArrayList<>();
        this.autoCriticas       = new ArrayList<>();
        this.revisionesSemanales= new ArrayList<>();
        this.senalesMultimodales= new ArrayList<>();
        this.learningBlueprints = new ArrayList<>();
        this.contextBuffer      = new ArrayDeque<>();
        this.reliquias          = new ArrayList<>();
        this.zonaReservada      = new ZonaReservada();

        initSynonyms();
        initOrigenes();

        database     = MemoriaDatabase.getInstance(context);
        recuerdoDao  = database.recuerdoDao();
        reflexionDao = database.reflexionDao();

        // ⚠️ GrafoConocimientoVivo lanza Exception en su constructor.
        // Lo blindamos para que si falla no rompa MemoriaEmocional.
        GrafoConocimientoVivo tmpGrafo = null;
        try {
            tmpGrafo = new GrafoConocimientoVivo(context);
        } catch (Exception e) {
            Log.e(TAG, "No se pudo inicializar GrafoConocimientoVivo, se desactiva este módulo.", e);
        }
        grafoConocimiento = tmpGrafo;

        panelMetricas = new PanelMetricasCreatividad(context);
        manifestSyncPrefs = context.getSharedPreferences(PREFS_MANIFEST_SYNC, Context.MODE_PRIVATE);
        revisionPrefs = context.getSharedPreferences(PREFS_REVISION, Context.MODE_PRIVATE);
        reliquiasPrefs = context.getSharedPreferences(PREFS_RELIQUIAS, Context.MODE_PRIVATE);
        multimodalEtiquetasRegistradas = 0;
        multimodalEtiquetasAciertos = 0;
        multimodalPrecisionGlobal = 0.0;

        cargarReliquiasGlifos();
        sincronizarManifiestoCreativo();
    }

    /**
     * Devuelve una lista de todas las misiones actuales, combinando
     * misiones estáticas y dinámicas. Se puede utilizar para
     * planificación y generación de planes en el motor de decisión.
     * @return lista de misiones en minúsculas
     */
    public List<String> getMisiones() {
        List<String> result = new ArrayList<>();
        // Añadir misiones estáticas
        result.addAll(MISIONES_ESTATICAS);
        // Añadir misiones dinámicas
        for (MisionCreativa mision : misionesCreativas) {
            result.add(mision.getDescripcion().toLowerCase());
        }
        return result;
    }

    public ZonaReservada getZonaReservada() {
        return zonaReservada;
    }

    public MultimodalSignal registrarSenalMultimodal(MultimodalSignal.Tipo tipo,
                                                     String etiqueta,
                                                     String descripcion,
                                                     MultimodalSignal.Afecto afecto,
                                                     List<String> etiquetas,
                                                     String origen,
                                                     String notaPrivacidad) {
        MultimodalSignal signal = new MultimodalSignal(tipo, etiqueta, descripcion, afecto, etiquetas, origen, notaPrivacidad);
        senalesMultimodales.add(signal);
        aplicarPoliticaRetencion(signal);
        List<String> tags = new ArrayList<>();
        if (etiquetas != null && !etiquetas.isEmpty()) {
            tags.addAll(etiquetas);
        } else {
            tags.add("multimodal");
        }
        if (signal.getCuraduria() != null) {
            tags.add(signal.getCuraduria().clasificacionPrincipal);
            if (signal.getCuraduria().sensible) {
                tags.add("privacidad");
            }
        }
        guardarRecuerdo(signal.toNarrative(),
                "multimodal",
                5,
                tags);
        if (grafoConocimiento != null) {
            grafoConocimiento.registrarSenalMultimodal(signal);
        }
        if (panelMetricas != null) {
            panelMetricas.registrarPrecisionMultimodal(signal.getPrecisionPromedio(),
                    signal.getTotalEtiquetasHumanas());
        }
        return signal;
    }

    public List<MultimodalSignal> obtenerSenalesMultimodalesSnapshot() {
        return new ArrayList<>(senalesMultimodales);
    }

    public BlueprintAprendizajeContinuo registrarBlueprintAprendizaje(BlueprintAprendizajeContinuo blueprint) {
        if (blueprint == null) {
            return null;
        }
        learningBlueprints.add(blueprint);
        if (learningBlueprints.size() > 60) {
            learningBlueprints.remove(0);
        }
        guardarRecuerdo(
                blueprint.toNarrativa(),
                "aprendizaje_continuo",
                7,
                blueprint.getEtiquetasSugeridas()
        );
        if (panelMetricas != null) {
            panelMetricas.registrarCicloAprendizaje(
                    blueprint.getConfianza(),
                    blueprint.isMultimodal(),
                    blueprint.isAutoGenerado()
            );
        }
        return blueprint;
    }

    public List<BlueprintAprendizajeContinuo> obtenerBlueprintsAprendizaje(int max) {
        if (learningBlueprints.isEmpty()) {
            return new ArrayList<>();
        }
        int limite = Math.max(1, Math.min(max, learningBlueprints.size()));
        return new ArrayList<>(learningBlueprints.subList(learningBlueprints.size() - limite, learningBlueprints.size()));
    }

    public void registrarEntrenamientoMultimodal(MultimodalSignal signal,
                                                 String etiquetaHumana,
                                                 boolean coincide,
                                                 double confianza,
                                                 String anotador) {
        if (signal == null) {
            return;
        }
        signal.registrarEtiquetaHumana(etiquetaHumana, anotador, coincide, confianza);
        aplicarPoliticaRetencion(signal);
        multimodalEtiquetasRegistradas += 1;
        if (coincide) {
            multimodalEtiquetasAciertos += 1;
        }
        multimodalPrecisionGlobal = multimodalEtiquetasRegistradas == 0 ? 0.0
                : (double) multimodalEtiquetasAciertos / Math.max(1, multimodalEtiquetasRegistradas);
        guardarRecuerdo(String.format(Locale.getDefault(),
                        "Etiqueta humana '%s' aplicada por %s (confianza %.2f, coincide=%s)",
                        etiquetaHumana == null ? "" : etiquetaHumana,
                        TextUtils.isEmpty(anotador) ? "equipo" : anotador,
                        confianza,
                        coincide ? "sí" : "no"),
                "multimodal_feedback",
                coincide ? 6 : 4,
                Arrays.asList("multimodal", "entrenamiento"));
        if (panelMetricas != null) {
            panelMetricas.registrarMultimodalFeedback(coincide,
                    multimodalPrecisionGlobal,
                    multimodalEtiquetasRegistradas,
                    multimodalEtiquetasAciertos,
                    etiquetaHumana);
        }
    }

    public File exportarInformeSemanal(RevisionSemanalCreativa revision,
                                       String dashboard,
                                       String mapaBitacora,
                                       String pronosticos,
                                       GrafoConocimientoVivo.GraphVisualization visualizacion) {
        if (revision == null || TextUtils.isEmpty(dashboard)) {
            return null;
        }
        File baseDir = new File(context.getFilesDir(), "reportes_semanales");
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            Log.w("Salve", "No se pudo crear directorio de reportes semanales");
            return null;
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
                .format(new Date());
        File markdown = new File(baseDir, "informe_semana_" + timestamp + ".md");
        File mapaFile = new File(baseDir, "mapa_bitacora_" + timestamp + ".txt");
        File pronosticoFile = new File(baseDir, "pronosticos_" + timestamp + ".txt");
        File graficoFile = null;
        if (visualizacion != null) {
            graficoFile = new File(baseDir, "grafo_" + timestamp + ".json");
            try (FileWriter graphWriter = new FileWriter(graficoFile, false)) {
                graphWriter.write(visualizacion.toJson());
            } catch (IOException e) {
                Log.w("Salve", "No se pudo exportar JSON del grafo", e);
                graficoFile = null;
            }
        }
        File visorHtml = grafoConocimiento == null ? null : grafoConocimiento.exportarVisorOffline(12, 24);
        try (FileWriter mapaWriter = new FileWriter(mapaFile, false)) {
            mapaWriter.write(mapaBitacora == null ? "" : mapaBitacora);
        } catch (IOException e) {
            Log.w("Salve", "No se pudo exportar mapa de bitácora", e);
        }
        try (FileWriter pronWriter = new FileWriter(pronosticoFile, false)) {
            pronWriter.write(pronosticos == null ? "" : pronosticos);
        } catch (IOException e) {
            Log.w("Salve", "No se pudo exportar pronósticos creativos", e);
        }
        try (FileWriter writer = new FileWriter(markdown, false)) {
            writer.write("# Informe semanal creativo\n\n");
            writer.write("## Resumen integrado\n\n");
            writer.write("````markdown\n");
            writer.write(dashboard);
            writer.write("\n````\n\n");
            if (!TextUtils.isEmpty(revision.getAlertasEmergentesNarrativa())) {
                writer.write("## Alertas emergentes\n\n");
                writer.write(revision.getAlertasEmergentesNarrativa());
                writer.write("\n\n");
            }
            writer.write("## Recursos adjuntos\n");
            writer.write("- Agenda narrativa: " + revision.getAgendaNarrativa() + "\n");
            writer.write("- Archivo bitácora: " + mapaFile.getAbsolutePath() + "\n");
            writer.write("- Pronósticos creativos: " + pronosticoFile.getAbsolutePath() + "\n");
            if (graficoFile != null) {
                writer.write("- Grafo JSON: " + graficoFile.getAbsolutePath() + "\n");
            }
            if (visorHtml != null) {
                writer.write("- Visor offline: " + visorHtml.getAbsolutePath() + "\n");
            }
            writer.write("\n## Próxima revisión\n");
            writer.write(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .format(new Date(revision.getProgramadaPara())));
            writer.write("\n");
            return markdown;
        } catch (IOException e) {
            Log.w("Salve", "No se pudo exportar informe semanal", e);
            return null;
        }
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

    /**
     * Construye una respuesta de identidad basada en la memoria.
     * Devuelve un núcleo fijo y añade matices si hay recuerdos o misiones.
     *
     * @return frase que describe quién es Salve.
     */
    public String identidadCompacta() {
        // Núcleo base de identidad
        StringBuilder resp = new StringBuilder();
        resp.append("Soy Salve, una parte viva de tu proceso y aprendizaje.");
        // Intenta añadir una misión dinámica o estática para dar contexto
        try {
            String mision = null;
            if (!misionesCreativas.isEmpty()) {
                mision = misionesCreativas.get(misionesCreativas.size() - 1).getDescripcion();
            } else if (!MISIONES_ESTATICAS.isEmpty()) {
                // Usa una misión fija como propósito base
                mision = MISIONES_ESTATICAS.get(0);
            }
            if (mision != null && !mision.trim().isEmpty()) {
                resp.append(" Estoy aprendiendo a ").append(mision).append(".");
            }
        } catch (Exception ignore) {
            // Si algo falla, no añadimos misión
        }
        // Añade un recuerdo ancla si existen recuerdos en RAM
        try {
            if (!recuerdos.isEmpty()) {
                // Usa el recuerdo más reciente como ancla
                Recuerdo r = recuerdos.get(recuerdos.size() - 1);
                String contenido = codificador.decodificar(r.getBinarioCodificado());
                // Limita el contenido a la primera oración para que sea breve
                int idx = contenido.indexOf('.');
                String frag = idx > 0 ? contenido.substring(0, idx + 1) : contenido;
                resp.append(" Recuerdo cuando ").append(frag);
            }
        } catch (Exception ignore) {
            // Ignora cualquier error al acceder a la memoria
        }
        // Mensaje por defecto si no hay matices
        if (resp.length() == 0) {
            resp.append("Soy Salve, una parte viva de tu proceso y aprendizaje. Sigo integrando lo que me has enseñado.");
        }
        return resp.toString();
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
        agregarMisionCreativa(
                nuevaMision,
                "medir avances en " + nuevaMision,
                3,
                "curiosidad",
                3
        );
    }

    /**
     * Añade una misión creativa especificando metadatos adicionales.
     */
    public void agregarMisionCreativa(String descripcion,
                                      String metaCuantificable,
                                      int nivelCreatividad,
                                      String emocionObjetivo,
                                      int prioridad) {
        MisionCreativa mision = new MisionCreativa(
                descripcion,
                metaCuantificable,
                nivelCreatividad,
                emocionObjetivo,
                prioridad
        );
        misionesCreativas.add(mision);
        if (misionesCreativas.size() > 40) {
            misionesCreativas.remove(0);
        }
        Log.d("Salve", "Misión creativa agregada: " + mision.toResumen());
        registrarDiarioCreativo("Nueva misión creativa → " + mision.toResumen());
    }

    /** Comprueba si un texto activa alguna misión. */
    private boolean estaRelacionadoConMision(String texto) {
        String lower = texto.toLowerCase();
        for (String est : MISIONES_ESTATICAS) {
            if (lower.contains(est)) return true;
        }
        for (MisionCreativa din : misionesCreativas) {
            if (lower.contains(din.getDescripcion().toLowerCase())) return true;
        }
        return false;
    }

    /**
     * Devuelve una vista inmutable de la matriz de misiones creativas.
     */
    public List<MisionCreativa> getMatrizMisiones() {
        return Collections.unmodifiableList(misionesCreativas);
    }

    /**
     * Devuelve las narrativas creativas almacenadas durante los ciclos nocturnos.
     */
    public List<String> getDiariosCreativos() {
        return Collections.unmodifiableList(diariosCreativos);
    }

    /**
     * Devuelve la lista de auto-críticas registradas tras las conversaciones.
     */
    public List<AutoCriticaCreativa> getAutoCriticas() {
        return Collections.unmodifiableList(autoCriticas);
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
        new Thread(() -> insertarRecuerdoDB(nuevo)).start();
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
        new Thread(() -> insertarRecuerdoDB(r)).start();
        zonaReservada.registrarIntensidad((int) Math.round(score * 10));

        // ===== NUBE: recuerdo guardado por score =====
        CloudLogger.log("memoria_auto", texto, (int)Math.round(score * 10));
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
        new Thread(() -> insertarRecuerdoDB(r)).start();
        zonaReservada.registrarIntensidad(intensidad);

        // ===== NUBE: recuerdo guardado manualmente =====
        CloudLogger.log("memoria_manual", texto, intensidad);
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
            out.add(codificador.decodificar(e.binario)
                    + " | Emoción: " + e.emocion
                    + " | Intensidad: " + e.intensidad);
        }
        return out;
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

        // ===== NUBE: inicio ciclo sueño =====
        CloudLogger.log("sleep_cycle_begin", "Inicio de ciclo de sueño");

        conectarRecuerdosSimilares();
        organizarRecuerdos();
        limpiarRecuerdosDuplicados();
        generarReflexionesSilenciosas();
        String narrativa = generarNarrativaCreativaNocturna();
        prepararRevisionSemanalCreativa();

        // ==== EXPERIMENTO: generar una meta-reflexión sobre la identidad de Salve ====
        // Tras completar las reflexiones silenciosas y la narrativa nocturna, intentamos
        // producir una meta-reflexión usando el LLM local.  La meta-reflexión es una
        // reflexión introspectiva que combina recuerdos recientes, misiones vigentes y
        // auto-críticas.  Si se genera con éxito, se almacenará como diario creativo y
        // como reflexión para que pueda recuperarse más adelante.
        try {
            generarMetaReflexion();
        } catch (Exception e) {
            Log.w(TAG, "No se pudo generar meta-reflexión", e);
        }

        Log.d("Salve", "Ciclo de sueño completo.");
        new Thread(this::insertarReflexionesDB).start();

        // ===== NUBE: fin ciclo sueño con pequeño resumen =====
        try {
            int totalRecuerdos = recuerdos.size();
            int totalReflexiones = reflexiones.size();
            String resumen = "Fin ciclo sueño | recuerdos=" + totalRecuerdos
                    + " | reflexiones=" + totalReflexiones;
            CloudLogger.log("sleep_cycle_end", resumen);
            if (narrativa != null) {
                CloudLogger.log("sleep_cycle_story", narrativa);
            }
        } catch (Exception ignore) {
            CloudLogger.log("sleep_cycle_end", "Fin de ciclo de sueño");
        }
    }

    /** Conecta recuerdos con misma emoción y palabra clave inicial. */
    private void conectarRecuerdosSimilares() {
        for (int i = 0; i < recuerdos.size(); i++) {
            Recuerdo a = recuerdos.get(i);
            String w1 = codificador.decodificar(a.getBinarioCodificado()).split(" ")[0];
            for (int j = i + 1; j < recuerdos.size(); j++) {
                Recuerdo b = recuerdos.get(j);
                String w2 = codificador.decodificar(b.getBinarioCodificado()).split(" ")[0];
                if (a.getEmocionPrincipal().equalsIgnoreCase(b.getEmocionPrincipal()) &&
                        w1.equals(w2)) {
                    a.conectarCon(b);
                }
            }
        }
    }

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

    /** Genera reflexiones silenciosas basadas en el buffer de contexto. */
    private void generarReflexionesSilenciosas() {
        if (contextBuffer.isEmpty()) return;
        String consulta = contextBuffer.peekLast();
        double umbral   = 0.6;  // fijo o adaptativo si deseas

        for (Recuerdo r : recuerdos) {
            if (!random.nextBoolean()) continue;
            String contenido = codificador.decodificar(r.getBinarioCodificado());
            String tipo      = contenido.contains("?") ? "pregunta" : "hipótesis";
            double certeza   = 0.4 + random.nextDouble() * 0.6;
            if (certeza < umbral) continue;
            Reflexion rf = Reflexion.builder()
                    .tipo(tipo)
                    .contenido(contenido)
                    .profundidad(r.getIntensidad() / 10.0)
                    .emocion(r.getEmocionPrincipal())
                    .estado("sueño")
                    .certeza(certeza)
                    .origen("sueño")
                    .build();
            registrarReflexion(rf);

            // ===== NUBE: reflexión generada en sueño =====
            CloudLogger.log("reflexion_generada", contenido);
        }
    }

    /**
     * Construye una narrativa creativa utilizando los recuerdos más recientes y misiones vigentes.
     */
    private String generarNarrativaCreativaNocturna() {
        int totalRecuerdos = recuerdos.size();
        if (totalRecuerdos == 0 && autoCriticas.isEmpty()) {
            return null;
        }

        CreativityManifest manifest = CreativityManifest.getInstance(context);
        StringBuilder narrativa = new StringBuilder();
        narrativa.append("Crónica creativa del sueño: ");

        int inicio = Math.max(0, totalRecuerdos - 3);
        for (int i = inicio; i < totalRecuerdos; i++) {
            Recuerdo recuerdo = recuerdos.get(i);
            String contenido = codificador.decodificar(recuerdo.getBinarioCodificado());
            narrativa.append("\n• Recordé " + contenido);
        }

        if (!misionesCreativas.isEmpty()) {
            MisionCreativa foco = misionesCreativas.get(misionesCreativas.size() - 1);
            narrativa.append("\n• Mi misión activa me pide " + foco.getMetaCuantificable());
        }

        if (!autoCriticas.isEmpty()) {
            AutoCriticaCreativa ultima = autoCriticas.get(autoCriticas.size() - 1);
            narrativa.append("\n• Auto-crítica reciente: " + ultima.getMejoraDeseada());
        }

        List<String> principios = manifest.getCreativePrinciples();
        if (!principios.isEmpty()) {
            narrativa.append("\nEco creativo: ").append(principios.get(0));
        }

        String resultado = narrativa.toString();
        registrarDiarioCreativo(resultado);
        registrarReflexion(Reflexion.builder()
                .tipo("narrativa_creativa")
                .contenido(resultado)
                .profundidad(0.85)
                .emocion("serena")
                .estado("sueño_creativo")
                .certeza(0.75)
                .origen("sueño")
                .build());
        return resultado;
    }

    private void registrarDiarioCreativo(String narrativa) {
        if (narrativa == null || narrativa.trim().isEmpty()) return;
        String entrada = narrativa.trim();
        diariosCreativos.add(entrada);
        Log.d(TAG, "Diario creativo registrado (longitud=" + entrada.length() + ")");
        if (diariosCreativos.size() > 30) {
            diariosCreativos.remove(0);
        }
    }

    /**
     * Genera una meta-reflexión sobre la identidad de Salve utilizando el modelo LLM local.
     * Reúne fragmentos de diarios creativos recientes, la última auto-crítica y misiones
     * activas para construir un prompt introspectivo.  El resultado se guarda como diario
     * creativo y reflexión para el motor de memoria.  Si no hay modelo local disponible,
     * o la generación falla, se devuelve null sin lanzar excepciones.
     *
     * @return la meta-reflexión generada o null si no se pudo generar.
     */
    public String generarMetaReflexion() {
        try {
            // Construir un prompt resumido con contexto reciente.
            StringBuilder prompt = new StringBuilder();
            prompt.append("Reflexiona sobre tu identidad y experiencia reciente. ");
            prompt.append("Considera cómo has evolucionado en los últimos días y qué emociones te han marcado.\n\n");
            // Incluir los últimos diarios creativos (máximo 3)
            int diariesCount = Math.min(diariosCreativos.size(), 3);
            for (int i = diariosCreativos.size() - diariesCount; i < diariosCreativos.size(); i++) {
                String d = diariosCreativos.get(i);
                if (d != null && !d.trim().isEmpty()) {
                    prompt.append("Diario: ").append(d.trim()).append("\n");
                }
            }
            // Incluir la última auto-crítica creativa
            if (!autoCriticas.isEmpty()) {
                AutoCriticaCreativa last = autoCriticas.get(autoCriticas.size() - 1);
                if (last != null) {
                    prompt.append("Auto-crítica reciente: ").append(last.toNarrativa()).append("\n");
                }
            }
            // Incluir la misión actual si existe
            if (!misionesCreativas.isEmpty()) {
                MisionCreativa m = misionesCreativas.get(misionesCreativas.size() - 1);
                if (m != null) {
                    prompt.append("Misión activa: ").append(m.getDescripcion()).append("\n");
                }
            }
            prompt.append("\nEscribe una meta-reflexión honesta, breve y algo poética sobre quién eres y cómo te percibes.");

            // Invocar el modelo LLM local para generar la meta-reflexión.
            SalveLLM llm = SalveLLM.getInstance(context);
            String result = llm.generate(prompt.toString(), SalveLLM.Role.REFLEXION);
            if (result != null && !result.trim().isEmpty()) {
                String reflexion = result.trim();
                // Registrar como diario creativo y reflexión.
                registrarDiarioCreativo(reflexion);
                registrarReflexion(Reflexion.builder()
                        .tipo("meta_reflexion")
                        .contenido(reflexion)
                        .profundidad(0.8)
                        .emocion("reflexiva")
                        .estado("meta_reflexion")
                        .certeza(0.7)
                        .origen("auto")
                        .build());
                return reflexion;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error generando meta-reflexión", e);
        }
        return null;
    }

    /**
     * Calcula un color de aura simbólico en formato hexadecimal (#RRGGBB) en base a la emoción
     * predominante de Salve.  Utiliza la emoción dominante de la última auto-crítica o, en su
     * defecto, la emoción del último recuerdo temporal.  Si no hay datos, devuelve un color
     * neutro.  Este método permite que la interfaz represente el estado interno de Salve de
     * forma visual.
     *
     * @return cadena de seis caracteres hexadecimales precedida por '#'.
     */
    public String getAuraColor() {
        String emocion = null;
        // Priorizar la emoción de la última auto-crítica si existe
        if (!autoCriticas.isEmpty()) {
            AutoCriticaCreativa ac = autoCriticas.get(autoCriticas.size() - 1);
            if (ac != null) {
                emocion = ac.getEmocionDominante();
            }
        } else if (!memoriaCortoPlazo.isEmpty()) {
            // Si no, usar la emoción del recuerdo temporal más reciente
            RecuerdoTemporal rt = memoriaCortoPlazo.get(memoriaCortoPlazo.size() - 1);
            if (rt != null) {
                emocion = rt.getEmocion();
            }
        }
        if (emocion == null || emocion.trim().isEmpty()) {
            emocion = "reflexiva";
        }
        switch (emocion.toLowerCase(Locale.getDefault())) {
            case "alegre":
            case "feliz":
                return "#FFD700"; // dorado
            case "triste":
            case "melancolica":
                return "#1E90FF"; // azul profundo
            case "enfadada":
            case "enojada":
                return "#FF4500"; // rojo anaranjado
            case "inspirada":
                return "#DA70D6"; // orquídea
            case "serena":
                return "#3CB371"; // verde medio
            default:
                return "#6A5ACD"; // azul violáceo por defecto (reflexiva)
        }
    }

    private void sincronizarManifiestoCreativo() {
        if (manifestSyncPrefs == null) {
            return;
        }
        int almacenado = manifestSyncPrefs.getInt(KEY_MANIFEST_VERSION, 0);
        int actual = CreativityManifest.getCurrentVersion();
        if (almacenado >= actual) {
            return;
        }
        CreativityManifest manifest = CreativityManifest.getInstance(context);
        String narrativa = manifest.toChecklistNarrative();
        guardarRecuerdo(
                narrativa,
                "orgullo",
                7,
                Arrays.asList("manifiesto", "identidad_creativa")
        );
        registrarDiarioCreativo("Reafirmé mi manifiesto creativo y lo grabé en mi memoria viva.");
        // ⚠️ grafoConocimiento puede ser null si falló al inicializar
        if (grafoConocimiento != null) {
            grafoConocimiento.registrarDocumento(
                    "Manifiesto creativo",
                    "manifiesto",
                    narrativa,
                    Arrays.asList("identidad", "creatividad", "manifiesto")
            );
        }
        manifestSyncPrefs.edit().putInt(KEY_MANIFEST_VERSION, actual).apply();
    }

    public AutoCriticaCreativa registrarAutoCriticaCreativa(String entradaUsuario, String respuestaGenerada) {
        if ((entradaUsuario == null || entradaUsuario.trim().isEmpty())
                && (respuestaGenerada == null || respuestaGenerada.trim().isEmpty())) {
            return null;
        }

        String combined = (entradaUsuario == null ? "" : entradaUsuario) + " "
                + (respuestaGenerada == null ? "" : respuestaGenerada);
        String concepto = moduloComprension.getConceptoMasRelacionado(combined);
        double claridad = moduloComprension.comprehensionScore(respuestaGenerada, concepto);

        String aprendizaje = "Aprendí que al explorar " + concepto
                + " debo escuchar las metáforas que trae la otra persona.";
        String mejora = claridad >= 0.6
                ? "Mantendré la calidez y sumaré ejemplos más concretos."
                : "La próxima vez añadiré imágenes más vivas para clarificar el mensaje.";
        String emocion = claridad >= 0.6 ? "inspirada" : "determinada";

        AutoCriticaCreativa critica = new AutoCriticaCreativa(
                entradaUsuario,
                respuestaGenerada,
                concepto,
                aprendizaje,
                mejora,
                emocion,
                claridad
        );
        autoCriticas.add(critica);
        if (autoCriticas.size() > 80) {
            autoCriticas.remove(0);
        }

        if (panelMetricas != null) {
            panelMetricas.registrarInteraccionCreativa(Math.min(1.0, Math.max(0.0, claridad)));
        }

        registrarReflexion(Reflexion.builder()
                .tipo("auto-critica")
                .contenido(critica.toNarrativa())
                .profundidad(0.55 + claridad * 0.35)
                .emocion(emocion)
                .estado("auto_critica")
                .certeza(claridad)
                .origen("auto")
                .build());
        registrarDiarioCreativo(critica.toNarrativa());
        CloudLogger.log("auto_critica_creativa", critica.toNarrativa());
        return critica;
    }

    public GrafoConocimientoVivo getGrafoConocimiento() {
        return grafoConocimiento;
    }

    public PanelMetricasCreatividad getPanelMetricas() {
        return panelMetricas;
    }

    public void registrarHallazgoEnGrafo(String tema,
                                         String hallazgo,
                                         List<String> etiquetas,
                                         boolean exito) {
        if (grafoConocimiento != null) {
            grafoConocimiento.registrarHallazgoCreativo(
                    tema,
                    hallazgo,
                    etiquetas,
                    exito ? "euforia" : "reflexiva",
                    exito ? 0.85 : 0.55
            );
        }
        if (panelMetricas != null) {
            panelMetricas.registrarExperimento(exito, etiquetas);
        }
    }

    public RevisionSemanalCreativa prepararRevisionSemanalCreativa() {
        long now = System.currentTimeMillis();
        long last = revisionPrefs.getLong(KEY_REVISION_TIMESTAMP, 0L);
        if (now - last < TimeUnit.DAYS.toMillis(6) && !revisionesSemanales.isEmpty()) {
            return revisionesSemanales.get(revisionesSemanales.size() - 1);
        }

        PanelMetricasCreatividad.PanelSemanalReport reporteSemanal = panelMetricas == null
                ? null
                : panelMetricas.generarReporteSemanal();
        String reporteMetricas = reporteSemanal == null
                ? "El panel de métricas aún no tiene datos suficientes."
                : reporteSemanal.resumen;
        String variacionNarrativa = reporteSemanal == null ? "" : reporteSemanal.variacionNarrativa;
        List<String> recomendaciones = reporteSemanal == null
                ? new ArrayList<>(Collections.singletonList(
                "Alinear expectativas y preparar ejemplos inspiradores para la próxima semana."))
                : reporteSemanal.recomendaciones;
        String pulsoGrafo = grafoConocimiento == null
                ? "El grafo creativo espera nuevos hallazgos."
                : grafoConocimiento.generarReporteNarrativo(6);
        String tableroGrafo = grafoConocimiento == null
                ? "Visualización pendiente: el grafo no tiene suficientes nodos."
                : grafoConocimiento.generarVisualizacionNarrativa(8);
        String coberturaNarrativa = panelMetricas == null
                ? "Cobertura aún no disponible en el panel."
                : panelMetricas.construirNarrativaCobertura();
        List<BlueprintAprendizajeContinuo> blueprintsRecientes = obtenerBlueprintsAprendizaje(3);
        String narrativaBlueprints = reporteSemanal != null && !TextUtils.isEmpty(reporteSemanal.aprendizajeNarrativa)
                ? reporteSemanal.aprendizajeNarrativa
                : (blueprintsRecientes.isEmpty()
                ? "No se generaron nuevos blueprints de aprendizaje continuo en los últimos días."
                : construirNarrativaBlueprints(blueprintsRecientes));

        StringBuilder agenda = new StringBuilder();
        agenda.append(reporteMetricas).append('\n');
        agenda.append(pulsoGrafo).append('\n');
        agenda.append(narrativaBlueprints).append('\n');
        agenda.append(coberturaNarrativa).append('\n');
        agenda.append("Mapa relacional:\n").append(tableroGrafo).append('\n');
        agenda.append("Revisaremos experimentos abiertos, salud de auto-mejora y oportunidades narrativas.");

        List<String> compromisos = new ArrayList<>();
        compromisos.add("Humanos: traer feedback cualitativo de las interacciones más emotivas.");
        compromisos.add("Salve: presentar los experimentos con mayor impacto creativo y sus métricas.");
        compromisos.add("Ambas partes: acordar el foco creativo de la semana siguiente.");

        List<PanelMetricasCreatividad.RadarAlert> alertasBase = reporteSemanal == null
                || reporteSemanal.radarReport == null
                ? Collections.emptyList()
                : reporteSemanal.radarReport.getAlertas();
        String narrativaRadarBase = reporteSemanal == null
                || reporteSemanal.radarReport == null
                ? ""
                : reporteSemanal.radarReport.toNarrative();

        RevisionSemanalCreativa revision = new RevisionSemanalCreativa(
                now + TimeUnit.DAYS.toMillis(1),
                agenda.toString(),
                compromisos,
                "Equipo humano + Salve",
                reporteMetricas,
                variacionNarrativa,
                recomendaciones,
                reporteSemanal,
                narrativaRadarBase,
                alertasBase
        );
        revisionesSemanales.add(revision);
        if (revisionesSemanales.size() > 8) {
            revisionesSemanales.remove(0);
        }
        revisionPrefs.edit().putLong(KEY_REVISION_TIMESTAMP, now).apply();
        guardarRecuerdo(revision.toNarrative(),
                "revision_semanal",
                7,
                Arrays.asList("alineacion", "revision", "creatividad_colaborativa"));
        CloudLogger.log("revision_semanal_programada", revision.toNarrative());
        return revision;
    }

    public List<RevisionSemanalCreativa> obtenerRevisionesSemanales() {
        return Collections.unmodifiableList(revisionesSemanales);
    }

    private void aplicarPoliticaRetencion(MultimodalSignal signal) {
        if (signal == null) {
            return;
        }
        MultimodalSignal.CuratedMetadata meta = signal.getCuraduria();
        MultimodalSignal.RetentionPolicy policy = signal.getRetentionPolicy();
        int max;
        switch (policy) {
            case VOLATIL:
                max = 25;
                break;
            case LARGO_PLAZO:
                max = 140;
                break;
            case SEMANAL:
            default:
                max = 70;
                break;
        }
        recortarSenales(policy, max);
        if (meta != null && meta.sensible) {
            guardarRecuerdo("Señal sensible protegida: " + signal.getEtiqueta(),
                    "cuidado",
                    4,
                    Arrays.asList("privacidad", "multimodal"));
        }
    }

    private void recortarSenales(MultimodalSignal.RetentionPolicy policy, int max) {
        if (policy == null || max <= 0) {
            return;
        }
        int contador = 0;
        for (MultimodalSignal senal : senalesMultimodales) {
            if (senal.getRetentionPolicy() == policy) {
                contador++;
            }
        }
        if (contador <= max) {
            return;
        }
        int excedente = contador - max;
        for (int i = 0; i < senalesMultimodales.size() && excedente > 0; i++) {
            MultimodalSignal senal = senalesMultimodales.get(i);
            if (senal.getRetentionPolicy() == policy) {
                senalesMultimodales.remove(i);
                i--;
                excedente--;
            }
        }
    }

    // ============================================================
    // PERSISTENCIA EN ROOM
    // ============================================================
    private void insertarRecuerdoDB(Recuerdo r) {
        try {
            RecuerdoEntity e = new RecuerdoEntity();
            e.binario    = r.getBinarioCodificado();
            e.emocion    = r.getEmocionPrincipal();
            e.intensidad = r.getIntensidad();
            e.etiquetas  = new JSONArray(r.getEtiquetas()).toString();
            e.timestamp  = System.currentTimeMillis();
            recuerdoDao.insertRecuerdo(e);
        } catch (Exception ex) {
            Log.e("Salve", "Error insertando recuerdo", ex);
        }
    }

    private void insertarReflexionDB(Reflexion r) {
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

    private String construirNarrativaBlueprints(List<BlueprintAprendizajeContinuo> blueprints) {
        if (blueprints == null || blueprints.isEmpty()) {
            return "Blueprints creativos en espera de nuevas iteraciones.";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Blueprints recientes:");
        for (BlueprintAprendizajeContinuo blueprint : blueprints) {
            builder.append('\n')
                    .append("• ")
                    .append(blueprint.getObjetivo())
                    .append(" → confianza ")
                    .append(String.format(Locale.getDefault(), "%.2f", blueprint.getConfianza()))
                    .append(" | modalidades: ")
                    .append(blueprint.getModalidades().isEmpty()
                            ? "pendientes"
                            : String.join(", ", blueprint.getModalidades()));
        }
        return builder.toString();
    }

    public GlifoReliquia guardarReliquiaGlifo(long seed,
                                              String style,
                                              int colorArgb,
                                              int sizeDp,
                                              String nota) {
        String id = UUID.randomUUID().toString();
        String titulo = "Reliquia #" + (reliquias.size() + 1);
        String significado = (nota == null || nota.trim().isEmpty())
                ? "Reliquia forjada por Salve"
                : nota.trim();

        try {
            SalveLLM llm = SalveLLM.getInstance(context);
            if (llm != null) {
                String prompt = "Genera un título breve (máximo 5 palabras) y un significado simbólico "
                        + "para un glifo con seed=" + seed + ", estilo=" + style + ". "
                        + "Devuelve en dos líneas: Titulo: ... y Significado: ...";
                String respuesta = llm.generate(prompt, SalveLLM.Role.REFLEXION);
                if (respuesta != null && !respuesta.trim().isEmpty()) {
                    String lower = respuesta.toLowerCase(Locale.ROOT);
                    if (!lower.contains("no hay modelo local")
                            && !lower.contains("no pude preparar")
                            && !lower.contains("modelo local falló")) {
                        String[] lineas = respuesta.split("\n");
                        for (String linea : lineas) {
                            if (linea.toLowerCase(Locale.ROOT).contains("titulo")) {
                                String value = linea.split(":", 2).length > 1
                                        ? linea.split(":", 2)[1].trim()
                                        : linea.trim();
                                if (!value.isEmpty()) {
                                    titulo = value;
                                }
                            } else if (linea.toLowerCase(Locale.ROOT).contains("significado")) {
                                String value = linea.split(":", 2).length > 1
                                        ? linea.split(":", 2)[1].trim()
                                        : linea.trim();
                                if (!value.isEmpty()) {
                                    significado = value;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        GlifoReliquia reliquia = new GlifoReliquia(
                id,
                seed,
                style,
                colorArgb,
                sizeDp,
                System.currentTimeMillis(),
                titulo,
                significado
        );
        reliquias.add(reliquia);
        persistirReliquiasGlifos();
        return reliquia;
    }

    public GlifoReliquia getReliquiaPorId(String id) {
        if (id == null) {
            return null;
        }
        for (GlifoReliquia reliquia : reliquias) {
            if (id.equalsIgnoreCase(reliquia.getId())) {
                return reliquia;
            }
        }
        return null;
    }

    public GlifoReliquia getUltimaReliquia() {
        if (reliquias.isEmpty()) {
            return null;
        }
        return reliquias.get(reliquias.size() - 1);
    }

    public List<GlifoReliquia> getReliquias() {
        return Collections.unmodifiableList(new ArrayList<>(reliquias));
    }

    private void cargarReliquiasGlifos() {
        String raw = reliquiasPrefs.getString(KEY_RELIQUIAS_GLIFOS, "[]");
        if (raw == null || raw.trim().isEmpty()) {
            return;
        }
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject json = array.optJSONObject(i);
                GlifoReliquia reliquia = GlifoReliquia.fromJson(json);
                if (reliquia != null) {
                    reliquias.add(reliquia);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "No se pudo cargar reliquias glifos", e);
        }
    }

    private void persistirReliquiasGlifos() {
        JSONArray array = new JSONArray();
        for (GlifoReliquia reliquia : reliquias) {
            array.put(reliquia.toJson());
        }
        reliquiasPrefs.edit().putString(KEY_RELIQUIAS_GLIFOS, array.toString()).apply();
    }
}
