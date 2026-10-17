package salve.core.compute;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import salve.core.ModelResult;

/** Executable contract checks without Android, network, models or external test dependencies. */
public final class ComputeRouterChecks {
    private static final ComputeNode A = new ComputeNode("a", "PC", "https://pc.example:8443", "x".repeat(40));
    private static final ComputeNode B = new ComputeNode("b", "GPU", "https://gpu.example", "y".repeat(40));
    private static class Transport implements ComputeRouter.Transport {
        int probes, generations; String selected, output = "remoto";
        boolean failHealth, failGenerate, cancel; int slotsA = 1, slotsB = 2, capacity = 48000;
        Runnable onGenerate = () -> {};
        public ComputeRouter.Health health(ComputeNode n, BooleanSupplier stopped) throws IOException {
            probes++;
            if (failHealth) throw new IOException();
            return new ComputeRouter.Health("modelo", n.id.equals("a") ? slotsA : slotsB, capacity, 2048);
        }
        public String generate(ComputeNode n, ComputeRouter.Health health, String id, String role, String prompt,
                BooleanSupplier stopped) throws IOException {
            generations++; selected = n.id; UUID.fromString(id);
            onGenerate.run();
            if (cancel) throw new CancellationException();
            if (failGenerate) throw new IOException();
            return output;
        }
    }
    private static class Case {
        Transport transport = new Transport(); long now; int local;
        AtomicBoolean stopped = new AtomicBoolean();
        ComputeRouter router = new ComputeRouter(transport, () -> now);
        ModelResult run(boolean enabled, String role, String prompt, ComputeNode... nodes) {
            return router.generate(Arrays.asList(nodes), enabled, role, prompt, stopped::get,
                    () -> { local++; return ModelResult.success("local", 0); });
        }
    }
    private static void check(boolean value, String name) { if (!value) throw new AssertionError(name); }
    public static void main(String[] args) { run(); System.out.println("Compute router: 19 scenarios passed"); }
    public static void run() {
        Case c = new Case();
        check(c.run(false, "NARRADOR", "cuento", A).getText().equals("local") && c.transport.probes == 0, "disabled is offline");
        c = new Case(); c.run(true, "CONVERSACIONAL", "hola", A);
        check(c.local == 1 && c.transport.probes == 0, "short chat stays local");
        c = new Case(); c.run(true, "NARRADOR", "cuento");
        check(c.local == 1 && c.transport.probes == 0, "no nodes falls back");
        c = new Case(); ModelResult r = c.run(true, "NARRADOR", "cuento", A, B);
        check(c.local == 0 && "b".equals(c.transport.selected) && r.getProvider().contains("GPU"), "select free capacity and attribute result");
        c = new Case(); c.transport.slotsA = 0; c.transport.slotsB = 0; c.run(true, "REFLEXION", "recuerdos", A, B);
        check(c.local == 1 && c.transport.generations == 0, "busy nodes never run");
        c = new Case(); c.transport.capacity = 2; c.run(true, "NARRADOR", "cuento", A);
        check(c.local == 1 && c.transport.generations == 0, "capacity mismatch");
        c = new Case(); c.transport.failHealth = true; c.run(true, "NARRADOR", "cuento", A);
        check(c.local == 1 && c.transport.generations == 0, "unavailable health");
        c = new Case(); c.transport.failGenerate = true; c.run(true, "NARRADOR", "cuento", A, B);
        check(c.local == 1 && c.transport.generations == 1, "one remote generation then local on error");
        c = new Case(); c.transport.failHealth = true; c.run(true, "NARRADOR", "cuento", A); c.transport.failHealth = false;
        c.run(true, "NARRADOR", "cuento", A); check(c.transport.probes == 1, "circuit open avoids repeated timeout");
        c.now = 60001; c.run(true, "NARRADOR", "cuento", A); check(c.transport.generations == 1, "circuit recovers");
        c = new Case(); c.stopped.set(true); r = c.run(true, "NARRADOR", "cuento", A);
        check(c.transport.probes == 0 && c.local == 0 && r.getStatus() == ModelResult.Status.CANCELLED, "cancel before starts nothing");
        c = new Case(); c.transport.cancel = true; r = c.run(true, "NARRADOR", "cuento", A);
        check(c.local == 0 && r.getStatus() == ModelResult.Status.CANCELLED, "cancel during does not fallback");
        c = new Case(); c.transport.onGenerate = () -> {}; AtomicBoolean revoked = c.stopped;
        c.transport.onGenerate = () -> revoked.set(true); r = c.run(true, "NARRADOR", "cuento", A);
        check(c.local == 0 && r.getStatus() == ModelResult.Status.CANCELLED, "revoked result discarded");
        c = new Case(); c.transport.output = " "; c.run(true, "SINTETIZADOR", "texto", A);
        check(c.local == 1, "empty output fails over");
        c = new Case(); c.transport.output = "x".repeat(64001); c.run(true, "SINTETIZADOR", "texto", A);
        check(c.local == 1, "oversized output fails over");
        c = new Case(); c.run(true, "CONVERSACIONAL", "x".repeat(6000), A);
        check(c.local == 0 && c.transport.generations == 1, "long context delegates");
        c = new Case(); c.run(true, "NARRADOR", "x".repeat(48001), A);
        check(c.local == 1 && c.transport.probes == 0, "oversized input never sent");
        for (String invalid : Arrays.asList("http://host", "https://user:pass@host", "https://host/path", "https://host?q=x", "https://host#f", "https://host:0")) {
            try { ComputeNode.endpoint(invalid); throw new AssertionError("accepted " + invalid); }
            catch (IllegalArgumentException expected) { }
        }
        check(ComputeNode.endpoint("https://192.168.1.10:8443/").equals("https://192.168.1.10:8443"), "explicit LAN TLS node allowed");
        c = new Case(); c.run(true, "shell", "code", A);
        check(c.local == 1 && c.transport.probes == 0, "unknown role never delegated");
    }
}
