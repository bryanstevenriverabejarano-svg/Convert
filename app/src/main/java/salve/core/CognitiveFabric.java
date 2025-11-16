package salve.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * La "fábrica cognitiva" coordina memoria, grafo y bitácora para que Salve
 * pueda consolidar una infraestructura profunda de aprendizaje. Expone
 * utilidades para registrar planos de aprendizaje y diagnosticar brechas.
 */
public class CognitiveFabric {

    public enum Capa {
        MEMORIA,
        GRAFO,
        BITACORA,
        METRICAS
    }

    private final MemoriaEmocional memoria;
    private final GrafoConocimientoVivo grafo;
    private final BitacoraExploracionCreativa bitacora;
    private final PanelMetricasCreatividad panel;

    public CognitiveFabric(MemoriaEmocional memoria,
                           BitacoraExploracionCreativa bitacora) {
        this.memoria = memoria;
        this.grafo = memoria == null ? null : memoria.getGrafoConocimiento();
        this.panel = memoria == null ? null : memoria.getPanelMetricas();
        if (bitacora != null) {
            this.bitacora = bitacora;
        } else {
            this.bitacora = new BitacoraExploracionCreativa(grafo);
        }
    }

    public BlueprintAprendizajeContinuo registrarBlueprint(BlueprintAprendizajeContinuo blueprint,
                                                           String fuenteNarrativa) {
        if (memoria == null || blueprint == null) {
            return blueprint;
        }
        memoria.registrarBlueprintAprendizaje(blueprint);
        if (bitacora != null) {
            bitacora.registrarEntradaEnlazada(
                    "aprendizaje_continuo",
                    "Se generó un blueprint para " + blueprint.getObjetivo(),
                    fuenteNarrativa == null ? blueprint.resumenCorto() : fuenteNarrativa,
                    blueprint.getEtiquetasSugeridas(),
                    blueprint.getImpactoCreativo(),
                    blueprint.getNodosClave(),
                    seleccionarMisionesRelacionadas(memoria.getMisiones()),
                    blueprint.getEtapas(),
                    null
            );
        }
        if (grafo != null) {
            grafo.registrarBlueprintAprendizaje(blueprint);
        }
        return blueprint;
    }

    public PanelMetricasCreatividad.AprendizajeSnapshot obtenerPulsoAprendizaje() {
        return panel == null ? null : panel.obtenerSnapshotAprendizaje();
    }

    public String construirNarrativaAprendizaje() {
        PanelMetricasCreatividad.AprendizajeSnapshot snapshot = obtenerPulsoAprendizaje();
        if (snapshot == null) {
            return "El panel aún no cuenta con ciclos de aprendizaje registrados.";
        }
        return String.format(Locale.getDefault(),
                "Pulso de aprendizaje → ciclos %d | auto-generados %.0f%% | multimodales %.0f%% | confianza media %.2f",
                snapshot.totalCiclos,
                snapshot.tasaAutogenerados() * 100.0,
                snapshot.tasaMultimodal() * 100.0,
                snapshot.confianzaPromedio);
    }

    private List<String> seleccionarMisionesRelacionadas(List<String> misiones) {
        if (misiones == null || misiones.isEmpty()) {
            return Collections.singletonList("mision_creativa_general");
        }
        List<String> seleccion = new ArrayList<>();
        for (String mision : misiones) {
            if (mision == null) {
                continue;
            }
            seleccion.add(mision);
            if (seleccion.size() >= 3) {
                break;
            }
        }
        return seleccion.isEmpty()
                ? Collections.singletonList("mision_creativa_general")
                : seleccion;
    }
}
