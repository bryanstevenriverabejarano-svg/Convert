package salve.core.conversation;

import java.util.Objects;

/** Un mensaje inmutable de una sesión conversacional. */
public final class ChatMessage {
    public enum Role { USER, ASSISTANT }

    private final Role role;
    private final String content;
    private final long timestamp;

    public ChatMessage(Role role, String content, long timestamp) {
        this.role = Objects.requireNonNull(role, "role");
        this.content = content == null ? "" : content.trim();
        this.timestamp = timestamp;
    }

    public Role getRole() { return role; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
}
