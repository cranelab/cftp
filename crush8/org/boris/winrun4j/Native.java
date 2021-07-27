package org.boris.winrun4j;

import java.nio.ByteBuffer;

public class Native {
  public static final boolean IS_64 = "amd64".equals(System.getProperty("os.arch"));
  
  public static native long loadLibrary(String paramString);
  
  public static native void freeLibrary(long paramLong);
  
  public static native long getProcAddress(long paramLong, String paramString);
  
  public static native long malloc(int paramInt);
  
  public static native void free(long paramLong);
  
  public static native ByteBuffer fromPointer(long paramLong1, long paramLong2);
  
  public static native boolean bind(Class paramClass, String paramString1, String paramString2, long paramLong);
  
  public static native long newGlobalRef(Object paramObject);
  
  public static native void deleteGlobalRef(long paramLong);
  
  public static native long getObjectId(Object paramObject);
  
  public static native Object getObject(long paramLong);
  
  public static native long getMethodId(Class paramClass, String paramString1, String paramString2, boolean paramBoolean);
}
