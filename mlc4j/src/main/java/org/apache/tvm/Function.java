package org.apache.tvm;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around a TVM packed function handle.
 */
public final class Function implements AutoCloseable {
    private static final String TAG = "TVMFunction";

    private final long handle;

    private Function(long handle) {
        this.handle = handle;
    }

    public static Function getGlobal(@NonNull String name) {
        LibInfo.ensureLoaded();
        long handle = LibInfo.tvmFuncGetGlobal(name);
        if (handle == 0L) {
            throw new IllegalStateException("Global function \"" + name + "\" not found in runtime.");
        }
        return new Function(handle);
    }

    public TVMValue invoke(Object... args) {
        LibInfo.ensureLoaded();
        if (args != null) {
            for (Object arg : args) {
                pushArg(arg);
            }
        }
        TVMValue ret = new TVMValue();
        LibInfo.tvmFuncCall(handle, ret);
        return ret;
    }

    private void pushArg(Object arg) {
        if (arg == null) {
            LibInfo.tvmFuncPushArgHandle(0L, TVMTypeCode.kTVMNullptr);
            return;
        }
        if (arg instanceof Integer) {
            LibInfo.tvmFuncPushArgLong(((Integer) arg).longValue());
        } else if (arg instanceof Long) {
            LibInfo.tvmFuncPushArgLong((Long) arg);
        } else if (arg instanceof Double) {
            LibInfo.tvmFuncPushArgDouble((Double) arg);
        } else if (arg instanceof Float) {
            LibInfo.tvmFuncPushArgDouble(((Float) arg).doubleValue());
        } else if (arg instanceof String) {
            LibInfo.tvmFuncPushArgString((String) arg);
        } else if (arg instanceof byte[]) {
            LibInfo.tvmFuncPushArgBytes((byte[]) arg);
        } else if (arg instanceof Function) {
            LibInfo.tvmFuncPushArgHandle(((Function) arg).handle, TVMTypeCode.kTVMPackedFuncHandle);
        } else {
            throw new IllegalArgumentException("Unsupported arg type for TVM call: " + arg.getClass().getName());
        }
    }

    @Override
    public void close() {
        if (handle != 0L) {
            try {
                LibInfo.tvmFuncFree(handle);
            } catch (Throwable t) {
                Log.w(TAG, "Failed to free TVM function handle: " + t.getMessage());
            }
        }
    }

    public static List<String> listGlobalNames() {
        LibInfo.ensureLoaded();
        String[] names = LibInfo.tvmFuncListGlobalNames();
        List<String> result = new ArrayList<>();
        if (names != null) {
            for (String name : names) {
                if (name != null) {
                    result.add(name);
                }
            }
        }
        return result;
    }
}
