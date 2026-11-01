package salve.core.voice;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.SystemClock;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Opt-in, bounded foreground diagnostic recorder. No PCM, microphone access,
 * cloud upload or background service. Never record secrets or prompts here.
 */
public final class VoiceLabReport {
    private static final int MAX_EVENTS = 500;
    private final List<Event> events = new ArrayList<>();
    private final long start = SystemClock.elapsedRealtime();
    private boolean enabled;
    private int dropped;

    private static final class Event {
        final long millis;
        final long token;
        final String category;
        final String detail;
        Event(long millis, long token, String category, String detail) {
            this.millis = millis; this.token = token;
            this.category = category; this.detail = detail;
        }
    }

    public synchronized void start() {
        events.clear();
        dropped = 0;
        enabled = true;
    }

    public synchronized void stop() { enabled = false; }

    public synchronized boolean isEnabled() { return enabled; }

    /** Event codes only. Transcripts require separate explicit consent and sanitization. */
    public synchronized void event(long token, String category, String detail) {
        if (!enabled) return;
        if (events.size() >= MAX_EVENTS) { dropped++; return; }
        events.add(new Event(SystemClock.elapsedRealtime() - start, token,
                safe(category, 32), safe(detail, 100)));
    }

    private static String safe(String text, int max) {
        if (text == null) return "-";
        String cleaned = text.replaceAll("[^a-zA-Z0-9_.,:;= /-]", "?");
        return cleaned.substring(0, Math.min(max, cleaned.length()));
    }

    /** Caller chooses a private destination; never exports automatically. */
    public synchronized void writePdf(File destination) throws IOException {
        if (destination == null) throw new IllegalArgumentException("destination");
        PdfDocument pdf = new PdfDocument();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(11f);
        try {
            int pageNo = 0, line = 0;
            Canvas canvas = null;
            PdfDocument.Page page = null;
            List<String> lines = new ArrayList<>();
            lines.add("SALVE VoiceLab - diagnostic report");
            lines.add("Events: " + events.size() + " | dropped: " + dropped);
            lines.add("No raw audio or conversation transcripts included.");
            lines.add("Results are observations, not proof of acoustic root cause.");
            for (Event e : events) {
                lines.add(String.format(Locale.US, "%d ms | turn %d | %s | %s",
                        e.millis, e.token, e.category, e.detail));
            }
            for (String entry : lines) {
                // Wrap at a conservative width to keep text within A4 margins.
                for (int i = 0; i < entry.length(); i += 82) {
                    if (page == null || line >= 56) {
                        if (page != null) pdf.finishPage(page);
                        page = pdf.startPage(new PdfDocument.PageInfo.Builder(595, 842, ++pageNo).create());
                        canvas = page.getCanvas();
                        line = 0;
                    }
                    canvas.drawText(entry.substring(i, Math.min(i + 82, entry.length())),
                            32, 42 + line * 13, paint);
                    line++;
                }
            }
            if (page != null) pdf.finishPage(page);
            try (FileOutputStream out = new FileOutputStream(destination)) { pdf.writeTo(out); }
        } finally { pdf.close(); }
    }
}
