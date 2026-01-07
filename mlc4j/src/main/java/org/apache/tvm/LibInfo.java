package org.apache.tvm;

import android.system.Os;
import android.system.OsConstants;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * Thin JNI bridge to the packed TVM runtime.
 *
 * The native methods correspond to the exported entry points inside
 * {@code libtvm4j_runtime_packed.so}.
 */
public final class LibInfo {
    private static final String TAG = "LibInfo";
    private static final long PAGE_SIZE_16K = 16 * 1024L;
    private static volatile boolean initialized = false;
    private static final java.util.concurrent.atomic.AtomicBoolean initStarted =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private static final java.util.concurrent.CountDownLatch initLatch =
            new java.util.concurrent.CountDownLatch(1);

    private LibInfo() { }

    public static void ensureLoaded() {
        if (initialized) {
            return;
        }
        if (initStarted.compareAndSet(false, true)) {
            try {
                synchronized (LibInfo.class) {
                    if (!initialized) {
                        if (is16kDevice() && !is16kRuntimeAllowed()) {
                            throw new IllegalStateException(
                                    "TVM runtime disabled on 16KB page-size devices. " +
                                            "Rebuild libtvm4j_runtime_packed.so with 16KB alignment " +
                                            "or set -Dmlc.allow16k=true to override."
                            );
                        }
                        System.loadLibrary("tvm4j_runtime_packed");
                        nativeLibInit();
                        initialized = true;
                        Log.i(TAG, "tvm4j_runtime_packed loaded");
                    }
                }
            } finally {
                initLatch.countDown();
            }
        } else {
            try {
                initLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for TVM runtime init", e);
            }
        }
    }

    public static native void nativeLibInit();

    public static native void shutdown();

    public static native long tvmFuncGetGlobal(@NonNull String name);

    public static native void tvmFuncFree(long handle);

    public static native void tvmFuncPushArgLong(long value);

    public static native void tvmFuncPushArgDouble(double value);

    public static native void tvmFuncPushArgString(@NonNull String value);

    public static native void tvmFuncPushArgBytes(@NonNull byte[] value);

    public static native void tvmFuncPushArgHandle(long handle, int typeCode);

    public static native void tvmFuncCall(long handle, @NonNull TVMValue retValue);

    public static native String[] tvmFuncListGlobalNames();

    public static native String tvmGetLastError();

    private static boolean is16kDevice() {
        try {
            long pageSize = Os.sysconf(OsConstants._SC_PAGESIZE);
            return pageSize == PAGE_SIZE_16K;
        } catch (Exception e) {
            Log.w(TAG, "Unable to detect page size; assuming non-16KB.", e);
            return false;
        }
    }

    private static boolean is16kRuntimeAllowed() {
        return Boolean.parseBoolean(System.getProperty("mlc.allow16k", "false"));
    }
}
