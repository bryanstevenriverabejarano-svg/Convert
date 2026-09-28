package salve.core;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.FutureTask;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class VerifiedModelFileTest {
    private static final String URL = "https://huggingface.co/test/model/resolve/abcdef/model.litertlm";
    private static final byte[] DATA = "a real file transfer fixture".getBytes(StandardCharsets.UTF_8);
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    private File target() { return new File(folder.getRoot(), "model.litertlm"); }
    private File partial() { return new File(target() + ".part"); }
    private static String sha(byte[] bytes) throws Exception {
        StringBuilder hex = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes)) hex.append(String.format("%02x", b & 255));
        return hex.toString();
    }
    private Response.Builder response(Request request, int code, byte[] bytes) {
        return new Response.Builder().request(request).code(code).message("fixture").protocol(Protocol.HTTP_1_1)
                .body(ResponseBody.create(MediaType.get("application/octet-stream"), bytes));
    }
    private VerifiedModelFile downloader(Interceptor transport) {
        return new VerifiedModelFile(new OkHttpClient.Builder().addInterceptor(transport).build());
    }
    private void fetch(VerifiedModelFile downloader) throws Exception {
        downloader.download(URL, target(), DATA.length, sha(DATA), (b, t, v) -> {}, () -> false);
    }

    @Test public void publishesOnlyAfterWholeFileVerification() throws Exception {
        VerifiedModelFile d = downloader(chain -> response(chain.request(), 200, DATA).build());
        d.download(URL, target(), DATA.length, sha(DATA), (b, t, v) -> assertFalse(target().exists()), () -> false);
        assertArrayEquals(DATA, Files.readAllBytes(target().toPath()));
        assertFalse(partial().exists());
    }

    @Test public void resumes206AndHashesBothOldAndNewBytes() throws Exception {
        Files.write(partial().toPath(), Arrays.copyOf(DATA, 5));
        fetch(downloader(chain -> {
            assertEquals("bytes=5-", chain.request().header("Range"));
            assertEquals("identity", chain.request().header("Accept-Encoding"));
            return response(chain.request(), 206, Arrays.copyOfRange(DATA, 5, DATA.length))
                    .header("Content-Range", "bytes 5-" + (DATA.length - 1) + "/" + DATA.length).build();
        }));
        assertArrayEquals(DATA, Files.readAllBytes(target().toPath()));
    }

    @Test public void restartsIfServerIgnoresRangeWith200() throws Exception {
        Files.write(partial().toPath(), Arrays.copyOf(DATA, 5));
        fetch(downloader(chain -> response(chain.request(), 200, DATA).build()));
        assertArrayEquals(DATA, Files.readAllBytes(target().toPath()));
    }

    @Test public void rejectsWrongRangeWithoutChangingSavedPrefix() throws Exception {
        Files.write(partial().toPath(), Arrays.copyOf(DATA, 5));
        assertThrows(IOException.class, () -> fetch(downloader(chain -> response(chain.request(), 206, DATA)
                .header("Content-Range", "bytes 0-" + (DATA.length - 1) + "/" + DATA.length).build())));
        assertEquals(5L, partial().length());
        assertFalse(target().exists());
    }

    @Test public void retriesRejectedRangeFromZeroOnce() throws Exception {
        Files.write(partial().toPath(), Arrays.copyOf(DATA, 5));
        AtomicInteger requests = new AtomicInteger();
        fetch(downloader(chain -> {
            if (requests.incrementAndGet() == 1) return response(chain.request(), 416, new byte[0]).build();
            assertNull(chain.request().header("Range"));
            return response(chain.request(), 200, DATA).build();
        }));
        assertEquals(2, requests.get());
    }

    @Test public void wrongChecksumDoesNotReplacePreviousFile() throws Exception {
        byte[] previous = "previous model".getBytes(StandardCharsets.UTF_8);
        Files.write(target().toPath(), previous);
        byte[] wrong = DATA.clone(); wrong[0]++;
        assertThrows(IOException.class, () -> fetch(downloader(chain -> response(chain.request(), 200, wrong).build())));
        assertArrayEquals(previous, Files.readAllBytes(target().toPath()));
        assertFalse(partial().exists());
    }

    @Test public void truncatedOrOversizedResponsesNeverBecomeReady() throws Exception {
        for (byte[] payload : new byte[][] { Arrays.copyOf(DATA, 3), Arrays.copyOf(DATA, DATA.length + 4) }) {
            assertThrows(IOException.class, () -> fetch(downloader(chain -> response(chain.request(), 200, payload).build())));
            assertFalse(target().exists());
        }
    }

    @Test public void verifiesSavedFileWithoutAnyNetworkRequest() throws Exception {
        Files.write(target().toPath(), DATA);
        fetch(downloader(chain -> { throw new AssertionError("Valid files need no HTTP request"); }));
    }

    @Test public void corruptSavedFileIsRedownloadedEvenIfSizeMatches() throws Exception {
        byte[] wrong = DATA.clone(); wrong[0]++;
        Files.write(target().toPath(), wrong);
        fetch(downloader(chain -> response(chain.request(), 200, DATA).build()));
        assertArrayEquals(DATA, Files.readAllBytes(target().toPath()));
    }

    @Test public void completePartIsVerifiedAndPublishedWithoutDownloadingAgain() throws Exception {
        Files.write(partial().toPath(), DATA);
        fetch(downloader(chain -> { throw new AssertionError("Complete .part needs no HTTP request"); }));
        assertTrue(target().exists());
    }

    @Test public void pauseRetainsPartialAndDoesNotPublish() throws Exception {
        AtomicBoolean cancel = new AtomicBoolean();
        VerifiedModelFile d = downloader(chain -> response(chain.request(), 200, DATA).build());
        assertThrows(InterruptedIOException.class, () -> d.download(URL, target(), DATA.length, sha(DATA),
                (b, t, v) -> cancel.set(true), cancel::get));
        assertFalse(target().exists());
        assertTrue(partial().exists());
    }

    @Test public void pauseCancelsAnOutstandingHttpCall() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        VerifiedModelFile d = downloader(chain -> {
            connected.countDown();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (!chain.call().isCanceled() && System.nanoTime() < deadline) {
                try { Thread.sleep(10); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException(e); }
            }
            if (!chain.call().isCanceled()) throw new AssertionError("Pause did not cancel HTTP");
            throw new IOException("Cancelled HTTP call");
        });
        Thread download = new Thread(() -> {
            try { fetch(d); } catch (Throwable e) { failure.set(e); }
        });
        download.start();
        try {
            assertTrue(connected.await(2, TimeUnit.SECONDS));
            d.cancel();
            download.join(2_000);
            assertFalse("Pause must not wait for the socket timeout", download.isAlive());
            assertTrue(failure.get() instanceof IOException);
            assertFalse(target().exists());
        } finally { d.cancel(); download.interrupt(); download.join(1_000); }
    }

    @Test public void immediateResumeWaitsForPreviousFileLockToClose() throws Exception {
        FutureTask<Void> resumed = new FutureTask<>(() -> {
            fetch(downloader(chain -> response(chain.request(), 200, DATA).build()));
            return null;
        });
        Thread download = new Thread(resumed);
        try (java.io.RandomAccessFile previous = new java.io.RandomAccessFile(target() + ".lock", "rw")) {
            try (java.nio.channels.FileLock lock = previous.getChannel().lock()) {
                download.start();
                Thread.sleep(150);
                assertFalse("Resume must wait instead of failing on a closing worker", resumed.isDone());
            }
            resumed.get(2, TimeUnit.SECONDS);
            assertArrayEquals(DATA, Files.readAllBytes(target().toPath()));
        } finally { download.interrupt(); download.join(1_000); }
    }

    @Test public void validatesRedirectBeforeContactingUnauthorizedHost() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        assertThrows(IOException.class, () -> fetch(downloader(chain -> {
            requests.incrementAndGet();
            return response(chain.request(), 302, new byte[0]).header("Location", "https://example.org/model").build();
        })));
        assertEquals(1, requests.get());
    }

    @Test public void followsPublishedHuggingFaceCdnAndKeepsRange() throws Exception {
        Files.write(partial().toPath(), Arrays.copyOf(DATA, 5));
        List<String> hosts = new ArrayList<>();
        fetch(downloader(chain -> {
            hosts.add(chain.request().url().host());
            assertEquals("bytes=5-", chain.request().header("Range"));
            if (hosts.size() == 1) return response(chain.request(), 302, new byte[0])
                    .header("Location", "https://us.aws.cdn.hf.co/model").build();
            return response(chain.request(), 206, Arrays.copyOfRange(DATA, 5, DATA.length))
                    .header("Content-Range", "bytes 5-" + (DATA.length - 1) + "/" + DATA.length).build();
        }));
        assertEquals(Arrays.asList("huggingface.co", "us.aws.cdn.hf.co"), hosts);
    }

    @Test public void rejectsMissingChecksumBeforeConnecting() throws Exception {
        VerifiedModelFile d = downloader(chain -> { throw new AssertionError("Invalid catalog"); });
        assertThrows(IOException.class, () -> d.download(URL, target(), DATA.length, null, (b,t,v)->{}, ()->false));
    }

    @Test public void rejectsHtmlEvenWhenLengthMatches() throws Exception {
        assertThrows(IOException.class, () -> fetch(downloader(chain -> response(chain.request(), 200, DATA)
                .body(ResponseBody.create(MediaType.get("text/html"), DATA)).build())));
        assertFalse(target().exists());
    }
}
