package salve.core.devicecontrol;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import salve.core.conversation.ChatMessage;

/**
 * Bounded follow-ups to an explicit music request. Uses only recent user text plus
 * Salve's immediately preceding question about a song. Never trusts a web page,
 * model output or an old memory as authorization to control another app.
 */
public final class MusicFollowupCommand {
    public enum Kind { TRACK, FAVORITES }

    public final Kind kind;
    public final String target;

    private static final Pattern CHOOSE = Pattern.compile(
            "^(?:si )?(?:pon|ponme|reproduce) (?:la que (?:tu )?quieras|cualquiera|algo que (?:tu )?quieras)$"
            + "|^(?:elige|escoge) (?:tu|cualquiera)$");
    private static final Pattern REFERENCE = Pattern.compile(
            "^(?:si )?(?:pon|ponme|reproduce) (?:esa|la) (?:cancion|musica|tema)$");
    private static final Pattern RESERVED = Pattern.compile(
            "^(?:no|nunca|espera|para|cancela|cancelar|sigue|gracias|vale|ok|si|hola|bueno|"
            + "ahora|cuentame|haz|explica|porque|por que|como|que|quiero)\\b.*");

    private MusicFollowupCommand(Kind kind, String target) {
        this.kind = kind;
        this.target = target;
    }

    public static MusicFollowupCommand parse(String current, List<ChatMessage> history) {
        if (current == null || history == null || history.size() < 3) return null;
        String cleaned = current.trim();
        if (cleaned.length() > 100 || cleaned.isEmpty() || cleaned.indexOf('\n') >= 0) return null;
        String n = normalize(cleaned);
        if (n.startsWith("no ") || n.startsWith("nunca ")) return null;

        int last = history.size() - 1;
        ChatMessage tail = history.get(last);
        if (tail.getRole() != ChatMessage.Role.USER
                || !normalize(tail.getContent()).equals(n)) return null;

        // Only the most recent few turns may authorize a music follow-up.
        int first = Math.max(0, last - 7);
        int question = -1;
        for (int i = last - 1; i >= first; i--) {
            ChatMessage message = history.get(i);
            if (message.getRole() == ChatMessage.Role.ASSISTANT
                    && asksWhichSong(message.getContent())) {
                question = i;
                break;
            }
        }
        if (question < 0) return null;

        boolean requestedMusic = false;
        for (int i = question - 1; i >= first; i--) {
            ChatMessage message = history.get(i);
            if (message.getRole() != ChatMessage.Role.USER) continue;
            AppActionCommand action = AppActionCommand.parse(message.getContent());
            if (action == null) continue;
            switch (action.type) {
                case MUSIC: case PLAYLIST: case LIKED: case MUSIC_SEARCH:
                    requestedMusic = true; break;
                case OPEN_APP:
                    requestedMusic = "spotify".equals(normalize(action.target)); break;
                default: break;
            }
            if (requestedMusic) break;
        }
        if (!requestedMusic) return null;

        if (CHOOSE.matcher(n).matches()) return new MusicFollowupCommand(Kind.FAVORITES, "Tus me gusta");

        if (REFERENCE.matcher(n).matches()) {
            for (int i = last - 1; i > question; i--) {
                ChatMessage message = history.get(i);
                if (message.getRole() == ChatMessage.Role.USER && isSongCandidate(message.getContent())) {
                    return new MusicFollowupCommand(Kind.TRACK, message.getContent().trim());
                }
            }
            return null;
        }

        // "Lluvia Cepeda": the answer to "¿qué canción quieres?" is not chat.
        if (history.get(last - 1).getRole() == ChatMessage.Role.ASSISTANT
                && asksWhichSong(history.get(last - 1).getContent())
                && isSongCandidate(cleaned)) {
            return new MusicFollowupCommand(Kind.TRACK, cleaned);
        }
        return null;
    }

    private static boolean asksWhichSong(String content) {
        String n = normalize(content);
        return (n.contains("que cancion") || n.contains("cual cancion")
                || n.contains("que musica") || n.contains("cual musica")
                || n.contains("que te gustaria que ponga")
                || n.contains("que quieres escuchar"));
    }

    private static boolean isSongCandidate(String candidate) {
        if (candidate == null || candidate.trim().isEmpty() || candidate.length() > 80) return false;
        String n = normalize(candidate);
        return n.matches("[a-z0-9]+(?: [a-z0-9]+){0,7}")
                && !RESERVED.matcher(n).matches()
                && AppActionCommand.parse(candidate) == null;
    }

    private static String normalize(String raw) {
        return Normalizer.normalize(raw.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-z0-9]+", " ").trim().replaceAll(" +", " ");
    }
}
