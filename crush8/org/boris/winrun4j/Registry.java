package org.boris.winrun4j;

public class Registry {
  public static final int REG_NONE = 0;
  
  public static final int REG_SZ = 1;
  
  public static final int REG_EXPAND_SZ = 2;
  
  public static final int REG_BINARY = 3;
  
  public static final int REG_DWORD = 4;
  
  public static final int REG_DWORD_LITTLE_ENDIAN = 4;
  
  public static final int REG_DWORD_BIG_ENDIAN = 5;
  
  public static final int REG_LINK = 6;
  
  public static final int REG_MULTI_SZ = 7;
  
  public static final int REG_RESOURCE_LIST = 8;
  
  public static final int REG_FULL_RESOURCE_DESCRIPTOR = 9;
  
  public static final int REG_RESOURCE_REQUIREMENTS_LIST = 10;
  
  public static final int REG_QWORD = 11;
  
  public static final int REG_QWORD_LITTLE_ENDIAN = 11;
  
  static {
    PInvoke.bind(Registry.class, "advapi32.dll");
  }
  
  public static byte[] queryValueEx(long hKey, String valueName, int maxLen) {
    PInvoke.ByteArrayBuilder bb = new PInvoke.ByteArrayBuilder();
    PInvoke.UIntPtr len = new PInvoke.UIntPtr(maxLen);
    int res = queryValueEx(hKey, valueName, 0L, (PInvoke.UIntPtr)null, bb, len);
    if (res == 0)
      return bb.toArray(); 
    return null;
  }
  
  public static QUERY_INFO queryInfoKey(long hKey) {
    StringBuilder lpClass = new StringBuilder();
    PInvoke.UIntPtr lpcClass = new PInvoke.UIntPtr(255L);
    PInvoke.UIntPtr lpcSubKeys = new PInvoke.UIntPtr();
    PInvoke.UIntPtr lpcMaxSubKeyLen = new PInvoke.UIntPtr();
    PInvoke.UIntPtr lpcMaxClassLen = new PInvoke.UIntPtr();
    PInvoke.UIntPtr lpcValues = new PInvoke.UIntPtr();
    PInvoke.UIntPtr lpcMaxValueNameLen = new PInvoke.UIntPtr();
    PInvoke.UIntPtr lpcMaxValueLen = new PInvoke.UIntPtr();
    PInvoke.UIntPtr lpcbSecurityDescriptor = new PInvoke.UIntPtr();
    FILETIME lastWriteTime = null;
    int res = queryInfoKey(hKey, lpClass, lpcClass, 0L, lpcSubKeys, lpcMaxSubKeyLen, lpcMaxClassLen, lpcValues, lpcMaxValueNameLen, lpcMaxValueLen, lpcbSecurityDescriptor, lastWriteTime);
    if (res != 0)
      return null; 
    QUERY_INFO info = new QUERY_INFO();
    info.keyClass = lpClass.toString();
    info.subKeyCount = lpcSubKeys.intValue();
    info.maxSubkeyLen = lpcMaxSubKeyLen.intValue();
    info.maxClassLen = lpcMaxClassLen.intValue();
    info.valueCount = lpcValues.intValue();
    info.maxValueNameLen = lpcMaxValueNameLen.intValue();
    info.maxValueLen = lpcMaxValueLen.intValue();
    info.cbSecurityDescriptor = lpcbSecurityDescriptor.intValue();
    info.lastWriteTime = lastWriteTime;
    return info;
  }
  
  @DllImport(entryPoint = "RegCloseKey")
  public static native int closeKey(long paramLong);
  
  @DllImport(entryPoint = "RegCreateKeyW")
  public static native int createKey(long paramLong, String paramString, @Out PInvoke.UIntPtr paramUIntPtr);
  
  @DllImport(entryPoint = "RegDeleteKeyW")
  public static native long deleteKey(long paramLong, String paramString);
  
  @DllImport(entryPoint = "RegDeleteValueW")
  public static native long deleteValue(long paramLong, String paramString);
  
  @DllImport(entryPoint = "RegOpenKeyExW")
  public static native int openKeyEx(long paramLong1, String paramString, int paramInt, long paramLong2, PInvoke.UIntPtr paramUIntPtr);
  
  @DllImport(entryPoint = "RegEnumKeyExW")
  public static native int enumKeyEx(long paramLong1, int paramInt, StringBuilder paramStringBuilder, PInvoke.UIntPtr paramUIntPtr, long paramLong2, long paramLong3, long paramLong4, FILETIME paramFILETIME);
  
  @DllImport(entryPoint = "RegEnumValue")
  public static native int enumValue(long paramLong1, int paramInt, StringBuilder paramStringBuilder, PInvoke.UIntPtr paramUIntPtr1, long paramLong2, PInvoke.UIntPtr paramUIntPtr2, PInvoke.ByteArrayBuilder paramByteArrayBuilder, PInvoke.IntPtr paramIntPtr);
  
  @DllImport(entryPoint = "RegQueryValueEx")
  public static native int queryValueEx(long paramLong1, String paramString, long paramLong2, PInvoke.UIntPtr paramUIntPtr1, PInvoke.ByteArrayBuilder paramByteArrayBuilder, PInvoke.UIntPtr paramUIntPtr2);
  
  @DllImport(entryPoint = "RegQueryValueEx")
  public static native int queryValueEx(long paramLong1, String paramString, long paramLong2, PInvoke.UIntPtr paramUIntPtr1, StringBuilder paramStringBuilder, PInvoke.UIntPtr paramUIntPtr2);
  
  @DllImport(entryPoint = "RegQueryInfoKey")
  public static native int queryInfoKey(long paramLong1, StringBuilder paramStringBuilder, PInvoke.UIntPtr paramUIntPtr1, long paramLong2, PInvoke.UIntPtr paramUIntPtr2, PInvoke.UIntPtr paramUIntPtr3, PInvoke.UIntPtr paramUIntPtr4, PInvoke.UIntPtr paramUIntPtr5, PInvoke.UIntPtr paramUIntPtr6, PInvoke.UIntPtr paramUIntPtr7, PInvoke.UIntPtr paramUIntPtr8, FILETIME paramFILETIME);
  
  @DllImport(entryPoint = "RegSetValueEx")
  public static native long setValueEx(long paramLong, String paramString, int paramInt1, int paramInt2, byte[] paramArrayOfbyte, int paramInt3);
  
  public static class QUERY_INFO {
    public String keyClass;
    
    public int subKeyCount;
    
    public int maxSubkeyLen;
    
    public int maxClassLen;
    
    public int valueCount;
    
    public int maxValueNameLen;
    
    public int maxValueLen;
    
    public int cbSecurityDescriptor;
    
    public Registry.FILETIME lastWriteTime;
  }
  
  public static class FILETIME implements PInvoke.Struct {
    public int dwLowDateTime;
    
    public int dwHighDateTime;
  }
}
