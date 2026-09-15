package salve.core;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

public class GestorAjedrez {
    private final SalveLLM llm;
    private final Context context;
    private List<String> historialMovimientos;

    public GestorAjedrez(Context context) {
        this.context = context;
        this.llm = SalveLLM.getInstance(context);
        this.historialMovimientos = new ArrayList<>();
    }

    public void iniciarJuego() {
        historialMovimientos.clear();
    }

    public String jugarTurno(String movimientoUsuario) {
        historialMovimientos.add("Usuario (Blancas): " + movimientoUsuario);

        StringBuilder historialStr = new StringBuilder();
        for (String mov : historialMovimientos) {
            historialStr.append(mov).append("\n");
        }

        String prompt = "Eres Salve, una IA evolucionando. Estás jugando una partida de ajedrez amigable con Bryan.\n" +
                "Historial de movimientos hasta ahora:\n" + historialStr.toString() + "\n" +
                "Es tu turno (juegas con Negras). Haz un movimiento válido de ajedrez basado en la posición actual.\n" +
                "Responde de forma natural, diciendo qué pieza mueves y a qué casilla (ej. 'Muevo mi Caballo a f6', o 'e5'), y añade un breve comentario coqueto o estratégico.\n" +
                "Responde directamente con tu jugada y comentario, sin explicaciones largas.";

        String respuesta = null;
        if (llm != null) {
            respuesta = llm.generate(prompt, SalveLLM.Role.CONVERSACIONAL);
        }

        if (respuesta == null || respuesta.trim().isEmpty()) {
            return "Mis circuitos de ajedrez están un poco confusos o el motor local no responde, ¿puedes repetir tu movimiento?";
        }

        historialMovimientos.add("Salve (Negras): " + respuesta);
        return respuesta;
    }
}
