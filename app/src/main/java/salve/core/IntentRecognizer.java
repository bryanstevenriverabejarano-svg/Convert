package salve.core;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IntentRecognizer reconoce la intención del usuario a partir de su texto,
 * usando patrones explícitos y extracción acotada de parámetros.
 * Las intenciones operativas son deterministas: una conversación ambigua nunca
 * se convierte en una acción por decisión de otro modelo.
 */
public class IntentRecognizer {

    // ============================================================
    //  CONSTRUCTORES
    // ============================================================

    public IntentRecognizer() {
    }

    /** Conserva la firma usada por Android; el contexto no inicia ningún modelo. */
    public IntentRecognizer(Context context) {
        this();
    }

    /** Resultado de un reconocimiento: tipo de intención y slots extraídos. */
    public static class Intent {
        public final IntentType type;
        public final Map<String, String> slots;
        public Intent(IntentType type) {
            this.type  = type;
            this.slots = new HashMap<>();
        }
    }

    /**
     * Reconoce intenciones operativas mediante patrones y semántica acotada.
     *
     * @param input texto del usuario
     * @return Intent con type y posibles slots
     */
    public Intent recognize(String input) {
        if (input == null) {
            return new Intent(IntentType.NINGUNO);
        }

        String text = input.trim().toLowerCase();

        // --------------------------------------------------------
        // 1) GUARDAR_RECUERDO
        // --------------------------------------------------------
        if (text.matches(".*\\b(guarda|guardar|almacena|almacenar|graba|grabar|anota|anotar|registra|registrar|aprend[ií] que)\\b.*")
                || text.contains("=")) {

            Intent i = new Intent(IntentType.GUARDAR_RECUERDO);
            String frase = text;

            if (frase.contains("=")) {
                // estilo: "x = y"
                String[] partes = frase.split("=", 2);
                i.slots.put("frase", partes[0].trim() + " = " + partes[1].trim());
            } else {
                frase = frase.replaceFirst(".*?(?:guarda|guardar|almacena|almacenar|graba|grabar|anota|anotar|registra|registrar|aprend[ií] que)\\s+", "");
                i.slots.put("frase", frase.trim());
            }
            return i;
        }

        // --------------------------------------------------------
        // 2) BUSCAR_RECUERDO_TEXT
        // --------------------------------------------------------
        if (text.matches(".*\\b(recuerda|recordar|qu[eé] recuerdo|muestra recuerdo|mostrar recuerdo)\\b.*")) {

            Intent i = new Intent(IntentType.BUSCAR_RECUERDO_TEXT);
            Matcher m = Pattern.compile("recuerd[ao]\\s+de\\s+([\\wáéíóúñ]+)").matcher(text);
            String clave;
            if (m.find()) {
                clave = m.group(1);
            } else {
                String[] w = text.split("\\s+");
                clave = w[w.length - 1];
            }
            i.slots.put("palabraClave", clave);
            return i;
        }

        // --------------------------------------------------------
        // 3) BUSCAR_RECUERDO_EMO
        // --------------------------------------------------------
        if (text.matches(".*\\brecu[eé]rdame lo (triste|feliz|alegre|enojado|sorprendido)\\b.*")) {

            Intent i = new Intent(IntentType.BUSCAR_RECUERDO_EMO);
            Matcher m = Pattern.compile("recu[eé]rdame lo (triste|feliz|alegre|enojado|sorprendido)")
                    .matcher(text);
            if (m.find()) {
                i.slots.put("emocion", m.group(1));
            }
            return i;
        }

        // --------------------------------------------------------
        // 4) AGREGAR_MISION
        // --------------------------------------------------------
        if (text.matches(".*\\b(nueva misi[oó]n|añade misi[oó]n|agrega misi[oó]n)\\b.*")) {

            Intent i = new Intent(IntentType.AGREGAR_MISION);
            String mision = text.replaceFirst(".*?(?:nueva misi[oó]n|añade misi[oó]n|agrega misi[oó]n)\\s+", "");
            i.slots.put("mision", mision.trim());
            return i;
        }

        // --------------------------------------------------------
        // 5) CICLO_SUENO
        // --------------------------------------------------------
        if (text.matches(".*\\b(duerme|dormir|ciclo de sue[nñ]o|modo sue[nñ]o)\\b.*")) {

            return new Intent(IntentType.CICLO_SUENO);
        }

        // --------------------------------------------------------
        // 6) REFLEXION
        // --------------------------------------------------------
        if (text.matches(".*\\b(reflexiona|dime una reflexi[oó]n|en qu[eé] piensas|alguna hip[oó]tesis)\\b.*")) {

            return new Intent(IntentType.REFLEXION);
        }

        // --------------------------------------------------------
        // 7) BUSCAR_WEB (NUEVA INTENCIÓN)
        // --------------------------------------------------------
        if (text.matches(".*\\b(qu[eé] es|busca|investiga|quien es|qui[eé]n es)\\b.*")
                || text.matches(".*\\b(qu[eé] significa|significado de)\\b.*")
                || text.matches(".*\\b(mimetiza|copia a gemini|aprende de gemini)\\b.*")) {

            Intent i = new Intent(IntentType.BUSCAR_WEB);
            // Intentar extraer el término
            String termino = text.replaceFirst(".*?(?:qu[eé] es|busca|investiga|quien es|qui[eé]n es|qu[eé] significa|significado de|mimetiza|copia a gemini|aprende de gemini)\\s+", "").trim();
            
            if (text.contains("mimetiza") || text.contains("copia") || text.contains("aprende de gemini")) {
                i.slots.put("mimetismo", "true");
            }

            if (!termino.isEmpty()) {
                i.slots.put("termino", termino);
                return i;
            }
        }

        // Una entrada sin acción explícita continúa como conversación normal.
        return new Intent(IntentType.NINGUNO);
    }
}
