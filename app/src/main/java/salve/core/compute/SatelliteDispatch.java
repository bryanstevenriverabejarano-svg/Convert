package salve.core.compute;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.function.BooleanSupplier;

import salve.core.ModelResult;
import salve.core.satellite.SatelliteProtocol;

/**
 * El lado de la Salve del dueño: cuando sus nodos no pueden con un cómputo, lo reparte a un satélite
 * a través del nodo (que hace de intermediario). Encola el trabajo y espera el resultado un rato
 * acotado; si no llega, se rinde y la Salve usa su modelo local. No envía memoria, solo el texto.
 */
public final class SatelliteDispatch {
    private static final int POLL_ROUNDS = 20;      // ~20 intentos
    private static final long POLL_PAUSE_MS = 1500; // cada 1,5 s → hasta ~30 s de espera

    private final ComputeHttpTransport transport;

    public SatelliteDispatch(ComputeHttpTransport transport) { this.transport = transport; }

    /**
     * Reparte un cómputo a un satélite del nodo y espera su respuesta. Devuelve el resultado, o null
     * si ningún satélite lo cogió o no respondió a tiempo (para caer al modelo local sin ruido).
     */
    public ModelResult compute(ComputeNode node, String prompt, BooleanSupplier stopped) {
        long start = System.currentTimeMillis();
        try {
            JsonObject enqueued = transport.satelliteEnqueue(node, SatelliteProtocol.enqueueCompute(prompt), stopped);
            if (enqueued == null || !enqueued.has("id")) return null; // nodo sin satélite que ofrezca cómputo
            String taskId = enqueued.get("id").getAsString();
            for (int i = 0; i < POLL_ROUNDS; i++) {
                if (stopped != null && stopped.getAsBoolean()) return null;
                try { Thread.sleep(POLL_PAUSE_MS); } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt(); return null;
                }
                JsonObject reply = transport.satelliteResult(node, taskId, stopped);
                String text = SatelliteProtocol.resultText(reply);
                if (text != null && !text.trim().isEmpty())
                    return ModelResult.success(text, System.currentTimeMillis() - start).withProvider("satélite");
            }
            return null;
        } catch (IOException | RuntimeException offline) {
            return null;
        }
    }
}
