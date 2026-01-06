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

    private LibInfo() { }

    public static synchronized void ensureLoaded() {
        if (initialized) {
            return;
        }
        System.loadLibrary("tvm4j_runtime_packed");
        nativeLibInit();
        initialized = true;
        Log.i(TAG, "tvm4j_runtime_packed loaded");
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
