package org.apache.tvm;

/**
 * Type codes used by the packed function interface.
 *
 * The numeric values follow TVM's runtime definition so native responses can be decoded.
 */
public final class TVMTypeCode {
    private TVMTypeCode() { }

    public static final int kDLInt = 0;
    public static final int kDLUInt = 1;
    public static final int kDLFloat = 2;
    public static final int kTVMOpaqueHandle = 3;
    public static final int kTVMObjectHandle = 4;
    public static final int kTVMModuleHandle = 5;
    public static final int kTVMPackedFuncHandle = 6;
    public static final int kTVMNDArrayHandle = 7;
    public static final int kTVMNullptr = 8;
    public static final int kTVMDataType = 9;
    public static final int kTVMDevice = 10;
    public static final int kTVMStr = 11;
    public static final int kTVMBytes = 12;
}
