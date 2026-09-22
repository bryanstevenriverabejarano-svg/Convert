package salve.core.memory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class OrderedMemoryWritesTest {
    @Test public void barrierMakesOrderedWritesVisibleToImmediateRead() throws Exception {
        try (OrderedMemoryWrites writes = new OrderedMemoryWrites()) {
            List<String> state = new ArrayList<>();
            writes.execute(() -> state.add("primero"));
            Future<String> second = writes.submit(() -> {
                state.add("segundo");
                return state.get(0);
            });
            writes.execute(() -> state.add("tercero"));
            writes.awaitReady(2, TimeUnit.SECONDS);
            assertEquals("primero", second.get(2, TimeUnit.SECONDS));
            assertEquals(Arrays.asList("primero", "segundo", "tercero"), state);
        }
    }

    @Test public void executeFailureIsReportedByReadBarrier() throws Exception {
        try (OrderedMemoryWrites writes = new OrderedMemoryWrites()) {
            RuntimeException failure = new IllegalArgumentException("write failed");
            writes.execute(() -> { throw failure; });
            assertSame(failure, barrierFailure(writes).getCause());
        }
    }

    @Test public void subsequentSuccessAndAnotherFailureCannotEraseFirstFailure() throws Exception {
        try (OrderedMemoryWrites writes = new OrderedMemoryWrites()) {
            RuntimeException first = new IllegalStateException("first failure");
            AtomicBoolean successfulWriteRan = new AtomicBoolean();
            writes.execute(() -> { throw first; });
            writes.execute(() -> successfulWriteRan.set(true));
            writes.execute(() -> { throw new IllegalStateException("later failure"); });
            assertSame(first, barrierFailure(writes).getCause());
            assertTrue(successfulWriteRan.get());
            assertSame(first, barrierFailure(writes).getCause());
        }
    }

    @Test public void failedSubmittedDeletionReachesFutureAndLaterBarrier() throws Exception {
        try (OrderedMemoryWrites writes = new OrderedMemoryWrites()) {
            IOException failure = new IOException("delete failed");
            Future<Boolean> deletion = writes.submit(() -> { throw failure; });
            try {
                deletion.get(2, TimeUnit.SECONDS);
                fail("Deletion should fail");
            } catch (ExecutionException expected) { assertSame(failure, expected.getCause()); }
            assertSame(failure, barrierFailure(writes).getCause());
        }
    }

    @Test public void timeoutDoesNotPretendMemoryIsReadyOrPoisonLaterRead() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        try (OrderedMemoryWrites writes = new OrderedMemoryWrites()) {
            CountDownLatch started = new CountDownLatch(1);
            AtomicBoolean stored = new AtomicBoolean();
            writes.submit(() -> {
                started.countDown();
                release.await();
                stored.set(true);
                return null;
            });
            assertTrue(started.await(2, TimeUnit.SECONDS));
            try {
                writes.awaitReady(10, TimeUnit.MILLISECONDS);
                fail("The queued write has not completed");
            } catch (TimeoutException expected) { assertFalse(stored.get()); }
            release.countDown();
            writes.awaitReady(2, TimeUnit.SECONDS);
            assertTrue(stored.get());
        } finally { release.countDown(); }
    }

    @Test public void waitingReaderCanBeInterruptedAndQueueCanStillDrain() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        try (OrderedMemoryWrites writes = new OrderedMemoryWrites()) {
            CountDownLatch writerStarted = new CountDownLatch(1);
            writes.submit(() -> { writerStarted.countDown(); release.await(); return null; });
            assertTrue(writerStarted.await(2, TimeUnit.SECONDS));
            AtomicBoolean interrupted = new AtomicBoolean();
            AtomicReference<Throwable> unexpected = new AtomicReference<>();
            CountDownLatch readerStarted = new CountDownLatch(1);
            Thread reader = new Thread(() -> {
                readerStarted.countDown();
                try { writes.awaitReady(5, TimeUnit.SECONDS); }
                catch (InterruptedException expected) { interrupted.set(true); }
                catch (Throwable error) { unexpected.set(error); }
            });
            reader.start();
            try {
                assertTrue(readerStarted.await(2, TimeUnit.SECONDS));
                reader.interrupt();
                reader.join(2000);
                assertFalse(reader.isAlive());
                assertTrue(interrupted.get());
                assertEquals(null, unexpected.get());
            } finally { reader.interrupt(); release.countDown(); reader.join(2000); }
            writes.awaitReady(2, TimeUnit.SECONDS);
        } finally { release.countDown(); }
    }

    @Test public void shutdownInterruptsRunningWriteAndCancelsQueuedFuture() throws Exception {
        try (OrderedMemoryWrites writes = new OrderedMemoryWrites()) {
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch interrupted = new CountDownLatch(1);
            writes.execute(() -> {
                started.countDown();
                try { new CountDownLatch(1).await(); }
                catch (InterruptedException expected) { interrupted.countDown(); }
            });
            assertTrue(started.await(2, TimeUnit.SECONDS));
            Future<String> pending = writes.submit(() -> "should not run");
            writes.shutdownNow();
            assertTrue(interrupted.await(2, TimeUnit.SECONDS));
            assertTrue(pending.isCancelled());
        }
    }

    @Test public void freshQueueIsRequiredToRecoverFromStickyFailure() throws Exception {
        try (OrderedMemoryWrites broken = new OrderedMemoryWrites()) {
            broken.execute(() -> { throw new IllegalStateException("write failed"); });
            barrierFailure(broken);
        }
        try (OrderedMemoryWrites fresh = new OrderedMemoryWrites()) {
            fresh.awaitReady(2, TimeUnit.SECONDS);
            assertEquals("saved", fresh.submit(() -> "saved").get(2, TimeUnit.SECONDS));
            fresh.awaitReady(2, TimeUnit.SECONDS);
        }
    }

    private static IllegalStateException barrierFailure(OrderedMemoryWrites writes) throws Exception {
        try {
            writes.awaitReady(2, TimeUnit.SECONDS);
            throw new AssertionError("Memory read must report the earlier write failure");
        } catch (IllegalStateException expected) { return expected; }
    }
}
