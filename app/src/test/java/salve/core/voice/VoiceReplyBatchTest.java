package salve.core.voice;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class VoiceReplyBatchTest {
    @Test public void acknowledgementAndIntermediateResultsProduceOnlyTheLastReply() {
        List<String> replies = new ArrayList<>();
        VoiceReplyBatch<String> batch = new VoiceReplyBatch<>(replies::add);
        assertTrue(batch.offer("Lo calculo"));
        assertTrue(batch.offer("Resultado provisional"));
        assertTrue(batch.offer("Resultado verificado"));
        assertTrue(replies.isEmpty());
        batch.complete();
        assertEquals(1, replies.size());
        assertEquals("Resultado verificado", replies.get(0));
    }

    @Test public void retainedContinuationCanFinishAfterItsOwner() {
        List<String> replies = new ArrayList<>();
        VoiceReplyBatch<String> batch = new VoiceReplyBatch<>(replies::add);
        assertTrue(batch.offer("Preparando"));
        assertTrue(batch.retain());
        batch.complete();
        assertTrue(replies.isEmpty());
        assertTrue(batch.offer("Terminado"));
        batch.complete();
        assertEquals(1, replies.size());
        assertEquals("Terminado", replies.get(0));
    }

    @Test public void continuationCanRetainFurtherWorkBeforeItCompletes() {
        List<String> replies = new ArrayList<>();
        VoiceReplyBatch<String> batch = new VoiceReplyBatch<>(replies::add);
        assertTrue(batch.retain());
        batch.complete();
        assertTrue(batch.retain());
        assertTrue(batch.offer("Paso intermedio"));
        batch.complete();
        assertTrue(replies.isEmpty());
        assertTrue(batch.offer("Resultado final"));
        batch.complete();
        assertEquals(1, replies.size());
        assertEquals("Resultado final", replies.get(0));
    }

    @Test public void noReplyStillCompletesExactlyOnceWithNull() {
        List<String> replies = new ArrayList<>();
        VoiceReplyBatch<String> batch = new VoiceReplyBatch<>(replies::add);
        batch.complete();
        batch.complete();
        assertEquals(1, replies.size());
        assertNull(replies.get(0));
    }

    @Test public void completedBatchRejectsLateRepliesAndRetains() {
        List<String> replies = new ArrayList<>();
        VoiceReplyBatch<String> batch = new VoiceReplyBatch<>(replies::add);
        batch.offer("Final");
        batch.complete();
        assertFalse(batch.offer("Demasiado tarde"));
        assertFalse(batch.retain());
        batch.complete();
        assertEquals(1, replies.size());
        assertEquals("Final", replies.get(0));
    }

    @Test public void callbackCanReenterAndDoesNotHoldTheBatchLock() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        AtomicReference<VoiceReplyBatch<String>> holder = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();
        VoiceReplyBatch<String> batch = new VoiceReplyBatch<>(reply -> {
            calls.incrementAndGet();
            assertFalse(holder.get().offer("reentrant"));
            holder.get().complete();
            try {
                Future<Boolean> otherThread = executor.submit(() -> holder.get().retain());
                assertFalse(otherThread.get(2, TimeUnit.SECONDS));
            } catch (Exception error) {
                throw new AssertionError("Callback must run outside the batch lock", error);
            }
        });
        holder.set(batch);
        try {
            batch.complete();
            assertEquals(1, calls.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test public void concurrentWorkersAndOwnerFinishWithOneFinalCallback() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<Integer> result = new AtomicReference<>();
        VoiceReplyBatch<Integer> batch = new VoiceReplyBatch<>(reply -> {
            result.set(reply);
            calls.incrementAndGet();
        });
        List<Future<?>> workers = new ArrayList<>();
        try {
            for (int i = 0; i < 32; i++) {
                assertTrue(batch.retain());
                final int value = i;
                workers.add(executor.submit(() -> {
                    start.await();
                    assertTrue(batch.offer(value));
                    batch.complete();
                    return null;
                }));
            }
            start.countDown();
            batch.complete();
            for (Future<?> worker : workers) worker.get(3, TimeUnit.SECONDS);
            batch.complete();
            assertEquals(1, calls.get());
            assertNotNull(result.get());
            assertTrue(result.get() >= 0 && result.get() < 32);
            assertFalse(batch.offer(100));
            assertFalse(batch.retain());
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test public void callbackFailureDoesNotReopenOrDeliverAgain() {
        AtomicInteger calls = new AtomicInteger();
        VoiceReplyBatch<String> batch = new VoiceReplyBatch<>(reply -> {
            calls.incrementAndGet();
            throw new IllegalStateException("consumer failed");
        });
        try {
            batch.complete();
            fail("Callback exception should be observable by the caller");
        } catch (IllegalStateException expected) {
            assertEquals("consumer failed", expected.getMessage());
        }
        batch.complete();
        assertEquals(1, calls.get());
        assertFalse(batch.retain());
    }

    @Test(expected = NullPointerException.class)
    public void callbackIsRequired() {
        new VoiceReplyBatch<String>(null);
    }
}
