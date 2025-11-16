package salve.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Gestiona la lista de experimentos creativos pendientes, en curso y completados.
 * La agenda funciona como un backlog priorizado que puede consultarse por otros
 * módulos (MotorConversacional, tablero de control) para conocer el estado de
 * la exploración autónoma.
 *
 * Mejoras:
 * - Metadatos por experimento (modelo LLM, herramientas sugeridas, claves de memoria, señales).
 * - Overrides de prioridad (sin modificar ExperimentoCreativo).
 * - Selección del siguiente experimento listo considerando herramientas/memoria.
 * - Resúmenes detallados con integración LLM/memoria/herramientas.
 *
 * Nota: No depende de la forma interna de ExperimentoCreativo, solo usa getters existentes.
 */
public class AgendaInvestigacion {

    private final List<ExperimentoCreativo> backlog;

    /** Metadatos ligados a un experimento para coordinación con LLM + memoria + herramientas. */
    public static class MetaExperimento {
        public String modelId;                 // p.ej. "qwen2.5-3b-instruct" o "phi-4"
        public List<String> herramientas;      // nombres de herramientas a emplear
        public List<String> memoryKeys;        // claves/paths recuperados de Namecheap
        public double urgenciaLLM;             // 0..1 (alta = 1)
        public double confianzaLLM;            // 0..1
        public double novedadMemoria;          // 0..1 (cuánto nuevo aporta la memoria)
        public boolean herramientasDisponibles; // true si están presentes/operativas
        public String origen;                  // p.ej. "router-intent", "usuario", "observación"
        public long creadoEnMs;
        public long actualizadoEnMs;

        public MetaExperimento() {
            this.herramientas = new ArrayList<>();
            this.memoryKeys = new ArrayList<>();
            this.creadoEnMs = System.currentTimeMillis();
            this.actualizadoEnMs = this.creadoEnMs;
        }

        public MetaExperimento copy() {
            MetaExperimento m = new MetaExperimento();
            m.modelId = modelId;
            m.herramientas = new ArrayList<>(herramientas != null ? herramientas : Collections.emptyList());
            m.memoryKeys = new ArrayList<>(memoryKeys != null ? memoryKeys : Collections.emptyList());
            m.urgenciaLLM = clamp01(urgenciaLLM);
            m.confianzaLLM = clamp01(confianzaLLM);
            m.novedadMemoria = clamp01(novedadMemoria);
            m.herramientasDisponibles = herramientasDisponibles;
            m.origen = origen;
            m.creadoEnMs = creadoEnMs;
            m.actualizadoEnMs = actualizadoEnMs;
            return m;
        }
    }

    /** Overrides locales de prioridad (no toca la instancia de ExperimentoCreativo). */
    private final Map<String, Integer> priorityOverrideById = new HashMap<>();
    /** Metadatos por experimento. */
    private final Map<String, MetaExperimento> metaById = new HashMap<>();

    public AgendaInvestigacion() {
        this.backlog = new ArrayList<>();
    }

    public static class ResumenAgenda {
        private final int pendientes;
        private final int enCurso;
        private final int completados;
        private final List<String> temasPrioritarios;

        public ResumenAgenda(int pendientes,
                             int enCurso,
                             int completados,
                             List<String> temasPrioritarios) {
            this.pendientes = pendientes;
            this.enCurso = enCurso;
            this.completados = completados;
            this.temasPrioritarios = temasPrioritarios == null
                    ? Collections.emptyList()
                    : new ArrayList<>(temasPrioritarios);
        }

        public int getPendientes() {
            return pendientes;
        }

        public int getEnCurso() {
            return enCurso;
        }

        public int getCompletados() {
            return completados;
        }

        public List<String> getTemasPrioritarios() {
            return Collections.unmodifiableList(temasPrioritarios);
        }
    }

    // ===================== API original (conservada) =====================

    public synchronized void agendarExperimento(ExperimentoCreativo experimento) {
        if (experimento == null) return;
        backlog.add(experimento);
        ordenarBacklog();
    }

    public synchronized List<ExperimentoCreativo> obtenerPendientes() {
        List<ExperimentoCreativo> pendientes = new ArrayList<>();
        for (ExperimentoCreativo experimento : backlog) {
            if (experimento.getEstado() == ExperimentoCreativo.Estado.PENDIENTE
                    || experimento.getEstado() == ExperimentoCreativo.Estado.EN_CURSO) {
                pendientes.add(experimento);
            }
        }
        pendientes.sort(this::compararPorPrioridadEfectiva);
        return pendientes;
    }

    public synchronized List<ExperimentoCreativo> obtenerCompletados() {
        List<ExperimentoCreativo> completados = new ArrayList<>();
        for (ExperimentoCreativo experimento : backlog) {
            if (experimento.getEstado() == ExperimentoCreativo.Estado.COMPLETADO) {
                completados.add(experimento);
            }
        }
        return Collections.unmodifiableList(completados);
    }

    public synchronized Optional<ExperimentoCreativo> buscarPorId(String id) {
        if (id == null) return Optional.empty();
        for (ExperimentoCreativo experimento : backlog) {
            if (id.equals(experimento.getId())) {
                return Optional.of(experimento);
            }
        }
        return Optional.empty();
    }

    public synchronized void registrarNota(String id, String nota) {
        buscarPorId(id).ifPresent(experimento -> experimento.registrarNota(nota));
    }

    public synchronized ResumenAgenda generarResumen() {
        int pendientes = 0;
        int enCurso = 0;
        int completados = 0;
        List<ExperimentoCreativo> ordenados = new ArrayList<>(backlog);
        ordenados.sort(this::compararPorPrioridadEfectiva);
        List<String> temasPrioritarios = new ArrayList<>();
        for (ExperimentoCreativo experimento : ordenados) {
            switch (experimento.getEstado()) {
                case EN_CURSO:
                    enCurso++;
                    break;
                case COMPLETADO:
                    completados++;
                    break;
                case PENDIENTE:
                default:
                    pendientes++;
                    break;
            }
            if (temasPrioritarios.size() < 3
                    && (experimento.getEstado() == ExperimentoCreativo.Estado.PENDIENTE
                    || experimento.getEstado() == ExperimentoCreativo.Estado.EN_CURSO)) {
                temasPrioritarios.add(experimento.getTema());
            }
        }
        return new ResumenAgenda(pendientes, enCurso, completados, temasPrioritarios);
    }

    // ===================== Extensiones LLM/Memoria/Herramientas =====================

    /**
     * Adjunta/actualiza metadatos provenientes del router LLM, memoria (Namecheap) o capa de herramientas.
     * Si no existe el experimento, no hace nada (evita crear duplicados).
     */
    public synchronized void adjuntarMeta(String experimentoId, MetaExperimento meta) {
        if (experimentoId == null || meta == null) return;
        Optional<ExperimentoCreativo> exp = buscarPorId(experimentoId);
        if (!exp.isPresent()) return;
        meta.actualizadoEnMs = System.currentTimeMillis();
        metaById.put(experimentoId, meta.copy());
        // Opcional: al adjuntar meta, reordenamos por si hay overrides activos
        ordenarBacklog();
    }

    /**
     * Devuelve una copia de los metadatos del experimento (si existen).
     */
    public synchronized Optional<MetaExperimento> getMeta(String experimentoId) {
        if (experimentoId == null) return Optional.empty();
        MetaExperimento m = metaById.get(experimentoId);
        return Optional.ofNullable(m == null ? null : m.copy());
    }

    /**
     * Establece un override de prioridad basado en señales (LLM urgencia/confianza, novedad de memoria,
     * disponibilidad de herramientas). No modifica la instancia de ExperimentoCreativo.
     *
     * @param experimentoId id del experimento
     * @param urgenciaLLM   0..1
     * @param novedadMem    0..1
     * @param herramientasOk true si herramientas están disponibles
     * @param pesoBaseOverride valor base (ej. 100) para convertir señales a prioridad efectiva
     */
    public synchronized void reforzarPrioridadDesdeSenales(String experimentoId,
                                                           double urgenciaLLM,
                                                           double novedadMem,
                                                           boolean herramientasOk,
                                                           int pesoBaseOverride) {
        if (experimentoId == null) return;
        Optional<ExperimentoCreativo> exp = buscarPorId(experimentoId);
        if (!exp.isPresent()) return;

        // Fórmula simple: base * (0.6*urgencia + 0.3*novedad + 0.1*tools)
        double score = 0.6 * clamp01(urgenciaLLM) + 0.3 * clamp01(novedadMem) + 0.1 * (herramientasOk ? 1.0 : 0.0);
        int override = Math.max(0, (int) Math.round(pesoBaseOverride * score));
        priorityOverrideById.put(experimentoId, override);
        ordenarBacklog();
    }

    /**
     * Elimina el override de prioridad (vuelve a la prioridad del experimento).
     */
    public synchronized void limpiarOverrideDePrioridad(String experimentoId) {
        if (experimentoId == null) return;
        priorityOverrideById.remove(experimentoId);
        ordenarBacklog();
    }

    /**
     * Siguiente experimento listo para ejecución:
     * - Estado PENDIENTE o EN_CURSO
     * - Si tiene meta y requiere herramientas, se privilegian los que reportan herramientas disponibles
     * - Ordenado por prioridad efectiva (override si existe > prioridad original)
     */
    public synchronized Optional<ExperimentoCreativo> nextForExecution() {
        List<ExperimentoCreativo> candidatos = new ArrayList<>();
        for (ExperimentoCreativo e : backlog) {
            if (e.getEstado() == ExperimentoCreativo.Estado.PENDIENTE
                    || e.getEstado() == ExperimentoCreativo.Estado.EN_CURSO) {
                candidatos.add(e);
            }
        }
        if (candidatos.isEmpty()) return Optional.empty();

        // Preferir los que (si tienen meta) declaran herramientas disponibles
        Set<String> preferidos = new HashSet<>();
        for (ExperimentoCreativo e : candidatos) {
            MetaExperimento m = metaById.get(e.getId());
            if (m != null && (m.herramientas != null && !m.herramientas.isEmpty()) && m.herramientasDisponibles) {
                preferidos.add(e.getId());
            }
        }

        candidatos.sort((a, b) -> {
            boolean aPref = preferidos.contains(a.getId());
            boolean bPref = preferidos.contains(b.getId());
            if (aPref != bPref) return aPref ? -1 : 1; // los preferidos primero
            return compararPorPrioridadEfectiva(a, b);
        });

        return Optional.of(candidatos.get(0));
    }

    /**
     * Resumen extendido: añade ids, prioridad efectiva y metadatos clave (modelo, herramientas, memoria).
     */
    public synchronized String generarResumenDetallado() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Agenda (detallada) ===\n");
        List<ExperimentoCreativo> ordenados = new ArrayList<>(backlog);
        ordenados.sort(this::compararPorPrioridadEfectiva);
        for (ExperimentoCreativo e : ordenados) {
            int prioEfectiva = prioridadEfectiva(e);
            MetaExperimento m = metaById.get(e.getId());
            sb.append("- [").append(e.getEstado()).append("] ")
                    .append(e.getTema()).append(" (id=").append(e.getId()).append(") ")
                    .append("prio=").append(prioEfectiva)
                    .append(m != null ? (" | model=" + nullSafe(m.modelId)) : "")
                    .append(m != null && m.herramientas != null && !m.herramientas.isEmpty()
                            ? (" | tools=" + String.join(",", m.herramientas)) : "")
                    .append(m != null && m.memoryKeys != null && !m.memoryKeys.isEmpty()
                            ? (" | mem=" + String.join(",", m.memoryKeys)) : "")
                    .append("\n");
        }
        return sb.toString();
    }

    // ===================== Internos =====================

    private void ordenarBacklog() {
        backlog.sort(this::compararPorPrioridadEfectiva);
    }

    /** Comparador que respeta override si existe; si no, usa prioridad del experimento (desc). */
    private int compararPorPrioridadEfectiva(ExperimentoCreativo a, ExperimentoCreativo b) {
        int pa = prioridadEfectiva(a);
        int pb = prioridadEfectiva(b);
        // Mayor primero
        int cmp = Integer.compare(pb, pa);
        if (cmp != 0) return cmp;
        // desempate estable por fecha/metadatos si existen
        MetaExperimento ma = metaById.get(a.getId());
        MetaExperimento mb = metaById.get(b.getId());
        long ta = ma != null ? ma.creadoEnMs : 0L;
        long tb = mb != null ? mb.creadoEnMs : 0L;
        return Long.compare(ta, tb);
    }

    /** Prioridad efectiva = max(override, prioridad original). */
    private int prioridadEfectiva(ExperimentoCreativo e) {
        Integer ov = priorityOverrideById.get(e.getId());
        int base = safePrioridad(e.getPrioridad());
        return Math.max(base, ov != null ? ov : Integer.MIN_VALUE);
    }

    private static int safePrioridad(int p) {
        // evita overflow si en algún momento alguien usa valores negativos muy bajos
        if (p < Integer.MIN_VALUE / 2) return Integer.MIN_VALUE / 2;
        if (p > Integer.MAX_VALUE / 2) return Integer.MAX_VALUE / 2;
        return p;
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
