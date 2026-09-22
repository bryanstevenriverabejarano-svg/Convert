package salve.core.memory;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/** Serializes memory mutations and makes their failures observable before a dependent read. */
public final class OrderedMemoryWrites implements AutoCloseable {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicReference<Throwable> firstFailure = new AtomicReference<>();

    public void execute(Runnable write) {
        Objects.requireNonNull(write, "write");
        executor.execute(() -> {
            try { write.run(); }
            catch (RuntimeException error) {
                // There is no Future for execute. The next read barrier must report this failure.
                firstFailure.compareAndSet(null, error);
            } catch (Error error) {
                firstFailure.compareAndSet(null, error);
                throw error;
            }
        });
    }

    public <T> Future<T> submit(Callable<T> write) {
        Objects.requireNonNull(write, "write");
        return executor.submit(() -> {
            try { return write.call(); }
            catch (Exception | Error error) {
                firstFailure.compareAndSet(null, error);
                throw error;
            }
        });
    }

    /**
     * Waits for mutations accepted before this barrier. A timeout/interruption does not mean
     * memory is empty or ready. Once a write fails, successful writes cannot clear that failure;
     * recovery requires a new queue after the caller has repaired or reloaded the underlying store.
     */
    public void awaitReady(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        Objects.requireNonNull(unit, "unit");
        if (Thread.interrupted()) throw new InterruptedException("Espera de memoria interrumpida");
        Future<?> barrier = executor.submit(() -> {
            Throwable failure = firstFailure.get();
            if (failure != null) throw new IllegalStateException(
                    "Una escritura de memoria anterior falló; la lectura no está confirmada", failure);
        });
        try {
            barrier.get(timeout, unit);
        } catch (InterruptedException | TimeoutException error) {
            barrier.cancel(false);
            throw error;
        } catch (CancellationException error) {
            throw new IllegalStateException("La espera de memoria se canceló al cerrar la cola", error);
        } catch (ExecutionException error) {
            Throwable cause = error.getCause();
            if (cause instanceof IllegalStateException) throw (IllegalStateException) cause;
            throw new IllegalStateException("No se pudo confirmar el estado de la memoria", cause);
        }
    }

    /** Interrupts a running task and releases waiters for queued submissions. */
    public void shutdownNow() {
        for (Runnable pending : executor.shutdownNow()) {
            if (pending instanceof Future<?>) ((Future<?>) pending).cancel(false);
        }
    }

    @Override public void close() { shutdownNow(); }
}
