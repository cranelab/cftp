package org.boris.winrun4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class NativeHelper {
  public static final int PTR_SIZE = Native.IS_64 ? 8 : 4;
  
  public static long call(long library, String fn, long... args) {
    return call(Native.getProcAddress(library, fn), args);
  }
  
  public static long call(long proc, long... args) {
    return FFI.call(proc, args);
  }
  
  public static void free(long... ptrs) {
    for (int i = 0; i < ptrs.length; i++) {
      if (ptrs[i] != 0L)
        Native.free(ptrs[i]); 
    } 
  }
  
  public static ByteBuffer getBuffer(long ptr, int size) {
    return Native.fromPointer(ptr, size).order(ByteOrder.LITTLE_ENDIAN);
  }
  
  public static int getInt(long ptr) {
    return Native.fromPointer(ptr, 4L).order(ByteOrder.LITTLE_ENDIAN).getInt();
  }
  
  public static long getPointer(long ptr) {
    ByteBuffer bb = Native.fromPointer(ptr, PTR_SIZE).order(ByteOrder.LITTLE_ENDIAN);
    return Native.IS_64 ? bb.getLong() : bb.getInt();
  }
  
  public static String getString(ByteBuffer bb, boolean wideChar) {
    StringBuilder sb = new StringBuilder();
    while (bb.hasRemaining()) {
      char c = wideChar ? bb.getChar() : (char)bb.get();
      if (c == '\000')
        break; 
      sb.append(c);
    } 
    return sb.toString();
  }
  
  public static String getString(long ptr, long size, boolean wideChar) {
    ByteBuffer bb = getBuffer(ptr, (int)size);
    return getString(bb, wideChar);
  }
  
  public static long toMultiString(String[] strs, boolean wideChar) {
    if (strs == null || strs.length == 0)
      return 0L; 
    int size = wideChar ? 2 : 1;
    for (int i = 0; i < strs.length; i++) {
      int len = strs[i].length() + 1;
      if (wideChar)
        len <<= 1; 
      size += len;
    } 
    long ptr = Native.malloc(size);
    ByteBuffer bb = getBuffer(ptr, size);
    for (int j = 0; j < strs.length; j++) {
      if (wideChar) {
        char[] c = strs[j].toCharArray();
        for (int k = 0; k < c.length; k++)
          bb.putChar(c[k]); 
      } else {
        byte[] b = strs[j].getBytes();
        for (int k = 0; k < b.length; k++)
          bb.put(b[k]); 
      } 
    } 
    if (wideChar) {
      bb.putShort((short)0);
    } else {
      bb.put((byte)0);
    } 
    return ptr;
  }
  
  public static long toNativeString(Object o, boolean wideChar) {
    if (o == null)
      return 0L; 
    String s = o.toString();
    if (wideChar) {
      char[] c = s.toCharArray();
      int j = c.length * 2 + 2;
      long l = Native.malloc(j);
      ByteBuffer byteBuffer = getBuffer(l, j);
      for (int i = 0; i < c.length; i++)
        byteBuffer.putChar(c[i]); 
      byteBuffer.putChar(false);
      return l;
    } 
    byte[] b = s.getBytes();
    int len = b.length + 1;
    long buf = Native.malloc(len);
    ByteBuffer bb = Native.fromPointer(buf, len);
    bb.put(b);
    bb.put((byte)0);
    return buf;
  }
  
  public static String getString(byte[] buffer, boolean wideChar) {
    if (buffer == null)
      return null; 
    return getString(ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN), wideChar);
  }
  
  public static byte[] toBytes(String str, boolean wideChar) {
    int len = str.length() + 1;
    if (wideChar)
      len <<= 1; 
    byte[] buf = new byte[len];
    ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
    for (int i = 0; i < str.length(); i++) {
      if (wideChar) {
        bb.putChar(str.charAt(i));
      } else {
        bb.put((byte)str.charAt(i));
      } 
    } 
    if (wideChar) {
      bb.putChar(false);
    } else {
      bb.put((byte)0);
    } 
    return buf;
  }
  
  public static void zeroMemory(ByteBuffer b) {
    while (b.hasRemaining())
      b.put((byte)0); 
  }
  
  public static void setInt(long ptr, int value) {
    getBuffer(ptr, 4).putInt(value);
  }
  
  public static void setPointer(long ptr, long value) {
    if (Native.IS_64) {
      getBuffer(ptr, 8).putLong(value);
    } else {
      getBuffer(ptr, 4).putInt((int)value);
    } 
  }
  
  public static String[] getMultiString(ByteBuffer bb, boolean wideChar) {
    ArrayList<String> strs = new ArrayList();
    StringBuilder sb = new StringBuilder();
    while (bb.hasRemaining()) {
      char c = wideChar ? bb.getChar() : (char)bb.get();
      if (c == '\000') {
        if (sb.length() == 0)
          break; 
        strs.add(sb.toString());
        sb.setLength(0);
        continue;
      } 
      sb.append(c);
    } 
    return strs.<String>toArray(new String[strs.size()]);
  }
  
  public static long toNative(byte[] b, int offset, int len) {
    long ptr = Native.malloc(len);
    ByteBuffer bb = getBuffer(ptr, len);
    bb.put(b, offset, len);
    return ptr;
  }
}
