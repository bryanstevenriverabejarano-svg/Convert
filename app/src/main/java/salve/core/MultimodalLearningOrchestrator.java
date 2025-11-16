package salve.core;

import android.text.TextUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Orquesta ciclos de aprendizaje multimodal tomando señales de memoria,
 * métricas del panel y hallazgos del grafo para que Salve pueda crear sus
 * propios planes de auto-entrenamiento.
 */
public class MultimodalLearningOrchestrator {

    private final MemoriaEmocional memoria;
    private final CognitiveFabric fabric;

    public MultimodalLearningOrchestrator(MemoriaEmocional memoria,
                                          BitacoraExploracionCreativa bitacora) {
        this.memoria = memoria;
        this.fabric = new CognitiveFabric(memoria, bitacora);
    }

    public BlueprintAprendizajeContinuo proponerBlueprintDesdeAutoMejora(String foco,
                                                                         ValidationSandbox.ValidationReport validacion,
                                                                         PanelMetricasCreatividad.RadarReport radarReport) {
        List<MultimodalSignal> señales = memoria.obtenerSenalesMultimodalesSnapshot();
        PanelMetricasCreatividad.AprendizajeSnapshot snapshot = fabric.obtenerPulsoAprendizaje();
        double confianzaBase = calcularConfianzaBase(snapshot, validacion);
        List<String> modalidades = seleccionarModalidades(señales);
        List<String> nodos = new ArrayList<>();
        if (!TextUtils.isEmpty(foco)) {
            nodos.add(foco);
        }
        nodos.addAll(recuperarNodosDesdeSeñales(señales));
        List<String> etapas = construirEtapasAutoMejora(foco, radarReport, señales);
        boolean multimodal = modalidades.size() > 1;
        String narrativa = construirNarrativa("auto-mejora", foco, snapshot, radarReport, señales);
        BlueprintAprendizajeContinuo blueprint = BlueprintAprendizajeContinuo.crear(
                TextUtils.isEmpty(foco) ? "Consolidar refactorizaciones" : "Profundizar en " + foco,
                modalidades,
                nodos,
                etapas,
                confianzaBase,
                multimodal,
                true,
                narrativa
        );
        return fabric.registrarBlueprint(blueprint, narrativa);
    }

    public BlueprintAprendizajeContinuo proponerBlueprintDesdeInvestigacion(String objetivo,
                                                                            ResultadoExperimento resultado) {
        List<MultimodalSignal> señales = memoria.obtenerSenalesMultimodalesSnapshot();
        PanelMetricasCreatividad.AprendizajeSnapshot snapshot = fabric.obtenerPulsoAprendizaje();
        double confianza = Math.min(0.95, 0.55 + (snapshot == null ? 0.0 : snapshot.confianzaPromedio * 0.3));
        List<String> modalidades = seleccionarModalidades(señales);
        if (modalidades.isEmpty()) {
            modalidades.add("textual");
        }
        List<String> nodos = new ArrayList<>();
        if (!TextUtils.isEmpty(objetivo)) {
            nodos.add(objetivo);
        }
        if (resultado != null) {
            String tema = extraerTema(resultado);
            if (!TextUtils.isEmpty(tema)) {
                nodos.add(tema);
            }
        }
        nodos.addAll(recuperarNodosDesdeSeñales(señales));
        List<String> etapas = new ArrayList<>();
        etapas.add("Revisar experimentos previos asociados al objetivo");
        etapas.add("Sintetizar datos multimodales relevantes en la memoria");
        etapas.add("Entrenar clasificadores ligeros con nuevas etiquetas humanas");
        String narrativa = construirNarrativa("investigación", objetivo, snapshot, null, señales);
        BlueprintAprendizajeContinuo blueprint = BlueprintAprendizajeContinuo.crear(
                TextUtils.isEmpty(objetivo) ? "Exploración multimodal" : objetivo,
                modalidades,
                nodos,
                etapas,
                confianza,
                modalidades.size() > 1,
                true,
                narrativa
        );
        return fabric.registrarBlueprint(blueprint, narrativa);
    }

    /**
     * Intenta obtener un “tema”/título del resultado mediante reflexión,
     * probando getters/campos comunes (getTema, getTopic, getTitulo, etc.).
     * No rompe compilación aunque la clase no tenga esos métodos.
     */
    private String extraerTema(Object resultado) {
        if (resultado == null) return null;

        // 1) Probar getters
        String[] posiblesGetters = {
                "getTema", "tema",
                "getTopic", "topic",
                "getTitulo", "titulo",
                "getNombre", "nombre",
                "getLabel", "label",
                "getTag", "tag",
                "getAsunto", "asunto",
                "getId", "id"
        };
        for (String m : posiblesGetters) {
            try {
                Method method = resultado.getClass().getMethod(m);
                Object val = method.invoke(resultado);
                if (val != null) {
                    String s = String.valueOf(val).trim();
                    if (!s.isEmpty()) return s;
                }
            } catch (Exception ignore) { }
        }

        // 2) Probar campos directos
        String[] posiblesCampos = {
                "tema", "topic", "titulo", "nombre", "label", "tag", "asunto"
        };
        for (String c : posiblesCampos) {
            try {
                Field f = resultado.getClass().getDeclaredField(c);
                f.setAccessible(true);
                Object val = f.get(resultado);
                if (val != null) {
                    String s = String.valueOf(val).trim();
                    if (!s.isEmpty()) return s;
                }
            } catch (Exception ignore) { }
        }

        return null;
    }

    private double calcularConfianzaBase(PanelMetricasCreatividad.AprendizajeSnapshot snapshot,
                                         ValidationSandbox.ValidationReport validacion) {
        double confianza = 0.55;
        if (snapshot != null) {
            confianza += Math.min(0.2, snapshot.confianzaPromedio * 0.2);
            confianza += snapshot.tasaAutogenerados() * 0.1;
        }
        if (validacion != null && validacion.success) {
            confianza += 0.1;
        }
        return Math.max(0.4, Math.min(0.95, confianza));
    }

    private List<String> seleccionarModalidades(List<MultimodalSignal> señales) {
        if (señales == null || señales.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> modalidades = new LinkedHashSet<>();
        int limite = Math.min(6, señales.size());
        for (int i = señales.size() - 1; i >= 0 && modalidades.size() < 4 && limite > 0; i--, limite--) {
            MultimodalSignal señal = señales.get(i);
            modalidades.add(señal.getTipo().name().toLowerCase(Locale.getDefault()));
        }
        return new ArrayList<>(modalidades);
    }

    private List<String> recuperarNodosDesdeSeñales(List<MultimodalSignal> señales) {
        if (señales == null || señales.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Integer> conteo = new HashMap<>();
        for (MultimodalSignal señal : señales) {
            for (String etiqueta : señal.getEtiquetas()) {
                if (etiqueta == null) continue;
                String key = etiqueta.trim().toLowerCase(Locale.getDefault());
                if (key.isEmpty()) continue;
                conteo.merge(key, 1, Integer::sum);
            }
        }
        List<Map.Entry<String, Integer>> ordenado = new ArrayList<>(conteo.entrySet());
        ordenado.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        List<String> nodos = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : ordenado) {
            nodos.add(entry.getKey());
            if (nodos.size() >= 4) {
                break;
            }
        }
        return nodos;
    }

    private List<String> construirEtapasAutoMejora(String foco,
                                                   PanelMetricasCreatividad.RadarReport radarReport,
                                                   List<MultimodalSignal> señales) {
        List<String> etapas = new ArrayList<>();
        etapas.add("Mapear patrones de código relacionados con " + (TextUtils.isEmpty(foco) ? "el foco detectado" : foco));
        if (radarReport != null && radarReport.hasCriticalAlerts()) {
            etapas.add("Correlacionar alertas del radar con experimentos recientes");
        }
        if (señales != null && !señales.isEmpty()) {
            etapas.add("Extraer rasgos multimodales frecuentes para sintetizar datasets locales");
        }
        etapas.add("Generar pruebas sintéticas y entrenar clasificadores ligeros");
        etapas.add("Documentar aprendizajes en la bitácora cognitiva");
        return etapas;
    }

    private String construirNarrativa(String origen,
                                      String foco,
                                      PanelMetricasCreatividad.AprendizajeSnapshot snapshot,
                                      PanelMetricasCreatividad.RadarReport radarReport,
                                      List<MultimodalSignal> señales) {
        StringBuilder builder = new StringBuilder();
        builder.append("Origen: ").append(origen).append(" | Foco: ")
                .append(TextUtils.isEmpty(foco) ? "cobertura creativa" : foco).append('\n');
        if (snapshot != null) {
            builder.append(String.format(Locale.getDefault(),
                    "Pulso previo → ciclos %d, autogenerados %.0f%%, multimodales %.0f%%, confianza %.2f\n",
                    snapshot.totalCiclos,
                    snapshot.tasaAutogenerados() * 100,
                    snapshot.tasaMultimodal() * 100,
                    snapshot.confianzaPromedio));
        }
        if (radarReport != null && radarReport.hasCriticalAlerts()) {
            builder.append("Radar alerta degradaciones → ").append(radarReport.getResumen()).append('\n');
        }
        if (señales != null && !señales.isEmpty()) {
            builder.append("Señales multimodales analizadas: ");
            int limite = Math.min(3, señales.size());
            for (int i = señales.size() - limite; i < señales.size(); i++) {
                if (i < 0) continue;
                builder.append('\n').append("• ").append(señales.get(i).toNarrative());
            }
        }
        builder.append("Objetivo: construir datasets y rutinas que Salve pueda recrear sin apoyo externo.");
        return builder.toString();
    }
}
