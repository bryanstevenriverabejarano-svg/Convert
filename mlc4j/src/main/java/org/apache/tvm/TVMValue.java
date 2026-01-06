package org.apache.tvm;

/**
 * Holder for values returned from the TVM packed function calls.
 *
 * The native layer writes into the public fields using JNI, so keep the
 * names stable and visibility wide.
 */
public class TVMValue {
    public int typeCode = TVMTypeCode.kTVMNullptr;
    public long handle = 0L;
    public long vLong = 0L;
    public double vDouble = 0.0d;
    public byte[] vBytes = null;
    public String vStr = null;

    public boolean isNull() {
        return typeCode == TVMTypeCode.kTVMNullptr || typeCode == TVMTypeCode.kTVMOpaqueHandle && handle == 0L;
    }

    public String asString() {
        if (typeCode == TVMTypeCode.kTVMStr) {
            return vStr != null ? vStr : "";
        }
        if (typeCode == TVMTypeCode.kTVMBytes && vBytes != null) {
            return new String(vBytes);
        }
        if (typeCode == TVMTypeCode.kDLInt || typeCode == TVMTypeCode.kDLUInt) {
            return Long.toString(vLong);
        }
        if (typeCode == TVMTypeCode.kDLFloat) {
            return Double.toString(vDouble);
        }
        return vStr != null ? vStr : "";
    }
}
