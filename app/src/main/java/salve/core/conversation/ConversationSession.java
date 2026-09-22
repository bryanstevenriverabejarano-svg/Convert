package salve.core.conversation;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Memoria de corto plazo de una conversación.
 *
 * No se persiste: evita convertir automáticamente cada frase en un recuerdo a
 * largo plazo. La persistencia selectiva pertenece al futuro MemoryService.
 */
public final class ConversationSession {
    private static final int DEFAULT_MAX_MESSAGES = 16;
    private static final int DEFAULT_MAX_CHARS = 8000;

    private final int maxMessages;
    private final int maxChars;
    private final Clock clock;
    private int contentChars;
    private final Deque<ChatMessage> messages = new ArrayDeque<>();
    private SessionLocation location;

    public ConversationSession() {
        this(DEFAULT_MAX_MESSAGES);
    }

    public ConversationSession(int maxMessages) {
        this(maxMessages, DEFAULT_MAX_CHARS);
    }

    public ConversationSession(int maxMessages, int maxChars) {
        this(maxMessages, maxChars, Clock.systemUTC());
    }

    public ConversationSession(int maxMessages, int maxChars, Clock clock) {
        if (maxMessages < 2) {
            throw new IllegalArgumentException("maxMessages debe ser al menos 2");
        }
        if (maxChars < 256) throw new IllegalArgumentException("maxChars debe ser al menos 256");
        this.maxMessages = maxMessages;
        this.maxChars = maxChars;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public synchronized void addUser(String content) {
        if (location != null && location.isDeniedBy(content)) location = null;
        SessionLocation declared = SessionLocation.fromUser(content, clock.millis());
        if (declared != null) location = declared;
        add(ChatMessage.Role.USER, content);
    }

    public synchronized void addAssistant(String content) {
        add(ChatMessage.Role.ASSISTANT, content);
    }

    private void add(ChatMessage.Role role, String content) {
        if (content == null || content.trim().isEmpty()) return;
        if (content.length() > maxChars) {
            String marker = "\n[Contenido truncado por el límite de contexto]";
            content = content.substring(0, maxChars - marker.length()) + marker;
        }
        messages.addLast(new ChatMessage(role, content, clock.millis()));
        contentChars += content.length();
        while (messages.size() > maxMessages || contentChars > maxChars) {
            contentChars -= messages.removeFirst().getContent().length();
        }
    }

    public synchronized List<ChatMessage> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }

    public synchronized boolean hasPriorContext() {
        return !messages.isEmpty();
    }

    public synchronized String asPromptTranscript() {
        StringBuilder out = new StringBuilder();
        for (ChatMessage message : messages) {
            out.append(message.getRole() == ChatMessage.Role.USER ? "USUARIO: " : "SALVE: ")
                    .append(message.getContent())
                    .append('\n');
        }
        return out.toString().trim();
    }

    /** Only current, explicitly declared location; survives transcript eviction but not clear/restart. */
    public synchronized String relevantLocationContext(String query) {
        if (location != null && !location.isCurrent(clock.millis())) location = null;
        if (!SessionLocation.relevantTo(query)) return "";
        return location == null
                ? "UBICACIÓN TEMPORAL: no hay una ubicación actual declarada y vigente en esta sesión. "
                    + "Una residencia guardada o una declaración antigua no confirma dónde está el usuario ahora. "
                    + "No presentes ubicaciones del historial como actuales sin una nueva confirmación del usuario."
                : location.promptContext();
    }

    public synchronized void clear() {
        messages.clear();
        contentChars = 0;
        location = null;
    }
}
