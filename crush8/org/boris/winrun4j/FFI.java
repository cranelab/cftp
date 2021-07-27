package org.boris.winrun4j;

import java.nio.ByteBuffer;

public class FFI {
  private static final boolean is64 = Native.IS_64;
  
  public static final int ABI_SYSV = 1;
  
  public static final int ABI_STDCALL = 2;
  
  public static final int ABI_WIN64 = 1;
  
  public static final int FFI_TYPE_VOID = 0;
  
  public static final int FFI_TYPE_INT = 1;
  
  public static final int FFI_TYPE_FLOAT = 2;
  
  public static final int FFI_TYPE_DOUBLE = 3;
  
  public static final int FFI_TYPE_LONGDOUBLE = 3;
  
  public static final int FFI_TYPE_UINT8 = 5;
  
  public static final int FFI_TYPE_SINT8 = 6;
  
  public static final int FFI_TYPE_UINT16 = 7;
  
  public static final int FFI_TYPE_SINT16 = 8;
  
  public static final int FFI_TYPE_UINT32 = 9;
  
  public static final int FFI_TYPE_SINT32 = 10;
  
  public static final int FFI_TYPE_UINT64 = 11;
  
  public static final int FFI_TYPE_SINT64 = 12;
  
  public static final int FFI_TYPE_STRUCT = 13;
  
  public static final int FFI_TYPE_POINTER = 14;
  
  public static long call(long lib, String function, long[] args) {
    long proc = Native.getProcAddress(lib, function);
    if (proc == 0L)
      throw new RuntimeException("Invalid function: " + function); 
    return call(proc, args);
  }
  
  public static long call(long proc, long[] args) {
    CIF cif = CIF.prepare(is64 ? 1 : 2, args.length);
    long rvalue = Native.malloc(8);
    long avalue = 0L;
    long pvalue = 0L;
    if (args.length > 0) {
      int size = args.length * NativeHelper.PTR_SIZE;
      avalue = Native.malloc(size);
      pvalue = Native.malloc(size);
      ByteBuffer vb = NativeHelper.getBuffer(avalue, size);
      ByteBuffer pb = NativeHelper.getBuffer(pvalue, size);
      for (int i = 0; i < args.length; i++) {
        if (is64) {
          vb.putLong(args[i]);
          pb.putLong(avalue + (i * NativeHelper.PTR_SIZE));
        } else {
          vb.putInt((int)args[i]);
          pb.putInt((int)(avalue + (i * NativeHelper.PTR_SIZE)));
        } 
      } 
    } 
    call(cif.get(), proc, rvalue, pvalue);
    ByteBuffer rb = NativeHelper.getBuffer(rvalue, 8);
    long result = rb.getLong();
    NativeHelper.free(new long[] { rvalue, avalue, pvalue });
    cif.destroy();
    return result;
  }
  
  public static native int prepare(long paramLong1, int paramInt1, int paramInt2, long paramLong2, long paramLong3);
  
  public static native void call(long paramLong1, long paramLong2, long paramLong3, long paramLong4);
  
  public static native long prepareClosure(long paramLong1, long paramLong2, long paramLong3);
  
  public static native void freeClosure(long paramLong);
  
  public static class CIF {
    private long cif;
    
    private long[] ffi_types;
    
    private long return_type;
    
    private long atypes;
    
    public static CIF prepare(int abi, int argc) {
      int[] types = new int[argc];
      for (int i = 0; i < argc; i++)
        types[i] = 14; 
      return prepare(abi, types);
    }
    
    public static CIF prepare(int abi, int[] types) {
      CIF c = new CIF();
      int sizeOfCif = 30;
      c.cif = Native.malloc(sizeOfCif);
      c.ffi_types = new long[types.length];
      for (int i = 0; i < types.length; i++)
        c.ffi_types[i] = makeType(types[i]); 
      c.return_type = makeType(14);
      int argSize = (types.length + 1) * NativeHelper.PTR_SIZE;
      c.atypes = Native.malloc(argSize);
      ByteBuffer ab = NativeHelper.getBuffer(c.atypes, argSize);
      for (int j = 0; j < types.length; j++) {
        if (FFI.is64) {
          ab.putLong(c.ffi_types[j]);
        } else {
          ab.putInt((int)c.ffi_types[j]);
        } 
      } 
      if (FFI.is64) {
        ab.putLong(0L);
      } else {
        ab.putInt(0);
      } 
      int res = FFI.prepare(c.cif, abi, types.length, c.return_type, c.atypes);
      if (res != 0) {
        NativeHelper.free(new long[] { c.cif, c.return_type, c.atypes });
        NativeHelper.free(c.ffi_types);
        throw new RuntimeException("Invalid FFI types for function");
      } 
      return c;
    }
    
    private static long makeType(int type) {
      long ffi_type = Native.malloc(24);
      int size = 0;
      switch (type) {
        case 14:
          size = NativeHelper.PTR_SIZE;
          break;
        case 3:
        case 12:
          size = 8;
          break;
      } 
      ByteBuffer bb = NativeHelper.getBuffer(ffi_type, 24);
      if (FFI.is64) {
        bb.putLong(size);
      } else {
        bb.putInt(size);
      } 
      bb.putShort((short)size);
      bb.putShort((short)type);
      return ffi_type;
    }
    
    public void destroy() {
      if (this.cif != 0L) {
        NativeHelper.free(new long[] { this.cif, this.return_type, this.atypes });
        NativeHelper.free(this.ffi_types);
        this.cif = this.return_type = this.atypes = 0L;
      } 
    }
    
    public long get() {
      return this.cif;
    }
  }
}
