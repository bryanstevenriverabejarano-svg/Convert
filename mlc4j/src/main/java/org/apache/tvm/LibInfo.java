package org.apache.tvm;

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
}
