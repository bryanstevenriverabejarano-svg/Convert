package salve.core.cognition;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Capability discovery for Android actions delegated to the existing adapters.
 * These checks are NOT language understanding: they only bound which *types* of
 * external effects the model may propose. The LLM supplies the plan and target;
 * the controller checks intent and target against this turn before execution.
 * No text from a page, a tool result or the model grants new permissions.
 */
public final class DeviceActionIntent {
    public enum Kind { OPEN_APP, NAVIGATE, PLAY_MUSIC, SEARCH_MUSIC, CALL }
    private DeviceActionIntent() { }

    private static final Pattern APP = Pattern.compile(
            "\\b(?:abre|abrir|abreme|abrirme|abras|abrieras|lanza|lanzar|inicia|iniciar|arranca|arrancar)\\b");
    private static final Pattern MAP = Pattern.compile(
            "\\b(?:llevame|llevanos|guiame|navega|navegar|ruta|indicaciones|maps|gps)\\b"
            + "|\\b(?:como|por donde)\\s+(?:llego|llegamos|voy|ir|puedo llegar)\\b");
    private static final Pattern MUSIC = Pattern.compile(
            "\\b(?:pon|ponme|reproduce|reproducir|escuchar|escuchemos|toca|suena|"
            + "escucharia|quiero escuchar)\\b.*\\b(?:musica|cancion|canciones|"
            + "tema|temas|album|disco|spotify|salsa|reggaeton|rock|pop|jazz|"
            + "playlist|lista|rap|bachata|merengue)\\b"
            + "|\\bspotify\\b.*\\b(?:pon|ponme|reproduce|escuchar|busca)\\b");
    private static final Pattern SEARCH_MUSIC = Pattern.compile(
            "\\b(?:busca|buscame|encuentra|muestra)\\b.*\\b(?:spotify|musica|"
            + "canciones|album|playlist|cancion)\\b");
    private static final Pattern CALL = Pattern.compile(
            "\\b(?:llama|llamale|llamar|telefonea|telefonear|marca|marcar)\\b"
            + ".*\\b(?:a|al|numero|telefono|movil)\\b");
    private static final Pattern META = Pattern.compile(
            "^(?:que|como|por que|puedes explicar|explica|dime que|investiga|"
            + "cuando|cual es|cuales son)\\b");
    private static final Pattern DANGEROUS = Pattern.compile(
            "\\b(?:envia|mandale|escribele|dile)\\b.*\\b(?:whatsapp|mensaje)\\b");

    // Explicit prohibitions override otherwise matching action words. A phrase
    // quoted inside a prohibition is not permission to execute that action.
    private static final Pattern NEGATED = Pattern.compile(
            "\\b(?:no|nunca|jamas)\\s+(?:(?:me|nos|le|les)\\s+)?"
            + "(?:abras|abre|abrir|abreme|abrirme|inicia|lances|lanza|"
            + "llama|llames|llamar|marques|marca|navegues|navega|llevame|"
            + "pongas|pon|ponme|reproduzcas|reproduce|busques|busca)\\b");

    /**
     * La clase de acción la pide la frase exacta o, si no, su modelo de significado la entendió en
     * este turno («quiero escuchar a Queen»). El destino lo sigue comprobando {@link #checkedTarget}
     * contra la frase real. Llamar no se entiende por significado: va siempre por su frase exacta.
     */
    public static boolean offered(Kind kind, String userInput) {
        if (offeredExact(kind, userInput)) return true;
        String tool = tool(kind);
        return tool != null && ActionMeaning.canonicalFor(userInput, tool) != null;
    }

    static String tool(Kind kind) {
        switch (kind) {
            case OPEN_APP: return "device.open_app";
            case NAVIGATE: return "device.navigate";
            case PLAY_MUSIC: return "device.play_music";
            case SEARCH_MUSIC: return "device.search_music";
            default: return null;
        }
    }

    static boolean offeredExact(Kind kind, String userInput) {
        String s = normalize(userInput);
        if (s.isEmpty() || s.length() > 450 || NEGATED.matcher(s).find()
                || (kind != Kind.NAVIGATE && META.matcher(s).find())) return false;
        switch (kind) {
            case OPEN_APP:
                // Ordinary chat about opening apps does not authorize navigation.
                return APP.matcher(s).find()
                        && !DANGEROUS.matcher(s).find()
                        && !WhatsAppDraftIntent.offered(userInput);
            case NAVIGATE: return MAP.matcher(s).find();
            case PLAY_MUSIC: return MUSIC.matcher(s).find();
            case SEARCH_MUSIC: return SEARCH_MUSIC.matcher(s).find();
            case CALL: return CALL.matcher(s).find();
            default: return false;
        }
    }

    /** A low-risk proposed target must be mentioned by the real user, not invented. */
    public static String checkedTarget(Kind kind, String userInput, String target) {
        if (!offered(kind, userInput)) {
            throw new IllegalArgumentException("El usuario no autorizó esta clase de acción.");
        }
        if (target == null || target.trim().length() < 2 || target.length() > 120
                || target.indexOf('\n') >= 0 || target.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Falta un destino válido.");
        }
        String goal = normalize(userInput);
        String requested = normalize(target);
        if (requested.length() < 2 || !(" " + goal + " ").contains(" " + requested + " ")) {
            throw new IllegalArgumentException("El destino no aparece en la orden original.");
        }
        return target.trim();
    }

    private static String normalize(String raw) {
        if (raw == null) return "";
        return Normalizer.normalize(raw.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-z0-9+]+", " ")
                .replaceAll("\\s+", " ").trim();
    }
}
