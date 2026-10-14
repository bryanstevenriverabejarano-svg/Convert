package salve.core.mind;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Dobles de prueba deterministas: almacén en memoria, reloj manual y generador con guion. */
final class MindTestSupport {
    private MindTestSupport() {}

    static final class MemoryStore implements SelfModelJournal.Store {
        String content;
        boolean failRead;
        boolean failWrite;
        int writes;
        @Override public String read() throws Exception {
            if (failRead) throw new java.io.IOException("ilegible");
            return content;
        }
        @Override public void write(String json) throws Exception {
            if (failWrite) throw new java.io.IOException("disco lleno");
            content = json;
            writes++;
        }
    }

    static final class Clock {
        final AtomicLong now = new AtomicLong(1_800_000_000_000L);
        long get() { return now.get(); }
        void advanceHours(long hours) { now.addAndGet(hours * 3_600_000L); }
    }

    static final class ScriptedGenerator implements MindGenerator {
        final Deque<String> outputs = new ArrayDeque<>();
        final List<String> prompts = new ArrayList<>();
        ScriptedGenerator then(String output) { outputs.add(output); return this; }
        @Override public String generate(String prompt) throws Exception {
            prompts.add(prompt);
            if (outputs.isEmpty()) throw new IllegalStateException("Sin salida preparada");
            String next = outputs.poll();
            if (next == null || next.equals("<FAIL>")) throw new IllegalStateException("fallo simulado");
            return next;
        }
    }

    static SelfModelJournal journal(MemoryStore store, Clock clock) {
        return new SelfModelJournal(store, clock::get);
    }

    static List<String> list(String... values) {
        List<String> out = new ArrayList<>();
        for (String v : values) out.add(v);
        return out;
    }
}
