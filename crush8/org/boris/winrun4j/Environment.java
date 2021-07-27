package org.boris.winrun4j;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Properties;

public class Environment {
  static {
    PInvoke.bind(Environment.class, "kernel32.dll");
  }
  
  private static final long library = Native.loadLibrary("kernel32.dll");
  
  public static String expandEnvironmentString(String var) {
    if (var == null)
      return null; 
    long str = NativeHelper.toNativeString(var, true);
    long buf = Native.malloc(4096);
    long res = NativeHelper.call(library, "ExpandEnvironmentStringsW", new long[] { str, buf, 4096L });
    String rs = null;
    if (res > 0L && res <= 4096L)
      rs = NativeHelper.getString(buf, 4096L, true); 
    Native.free(str);
    Native.free(buf);
    return rs;
  }
  
  public static String[] getCommandLine() {
    long res = NativeHelper.call(library, "GetCommandLineW", new long[0]);
    String s = NativeHelper.getString(res, 1024L, true);
    boolean inQuote = false;
    ArrayList<String> args = new ArrayList();
    StringBuilder sb = new StringBuilder();
    int len = s.length();
    for (int i = 0; i < len; i++) {
      char c = s.charAt(i);
      if (c == '"')
        inQuote = !inQuote; 
      if (c == ' ') {
        if (inQuote) {
          sb.append(c);
        } else {
          args.add(sb.toString());
          sb.setLength(0);
        } 
      } else {
        sb.append(c);
      } 
    } 
    if (sb.length() > 0)
      args.add(sb.toString()); 
    return args.<String>toArray(new String[args.size()]);
  }
  
  public static Properties getEnvironmentVariables() {
    long buf = NativeHelper.call(library, "GetEnvironmentStringsW", new long[0]);
    ByteBuffer bb = NativeHelper.getBuffer(buf, 32767);
    Properties p = new Properties();
    while (true) {
      String s = NativeHelper.getString(bb, true);
      if (s == null || s.length() == 0)
        break; 
      int idx = s.indexOf('=');
      p.put(s.substring(0, idx), s.substring(idx + 1));
    } 
    NativeHelper.call(library, "FreeEnvironmentStringsW", new long[] { buf });
    return p;
  }
  
  public static String getEnv(String name) {
    StringBuilder sb = new StringBuilder();
    PInvoke.UIntPtr ptr = new PInvoke.UIntPtr(4096L);
    int res = GetEnvironmentVariable(name, sb, ptr);
    if (res > 0)
      return sb.toString(); 
    return null;
  }
  
  public static File[] getLogicalDrives() {
    int len = 1024;
    long buf = Native.malloc(len);
    long res = NativeHelper.call(library, "GetLogicalDriveStringsW", new long[] { len, buf });
    ByteBuffer bb = NativeHelper.getBuffer(buf, (int)(res + 1L) << 1);
    ArrayList<File> drives = new ArrayList();
    StringBuilder sb = new StringBuilder();
    while (true) {
      char c = bb.getChar();
      if (c == '\000') {
        if (sb.length() == 0)
          break; 
        drives.add(new File(sb.toString()));
        sb.setLength(0);
        continue;
      } 
      sb.append(c);
    } 
    Native.free(buf);
    return drives.<File>toArray(new File[drives.size()]);
  }
  
  @DllImport("kernel32.dll")
  public static native int GetEnvironmentVariable(String paramString, StringBuilder paramStringBuilder, PInvoke.UIntPtr paramUIntPtr);
  
  @DllImport("kernel32.dll")
  public static native boolean GetVersionEx(OSVERSIONINFOEX paramOSVERSIONINFOEX);
  
  public static class OSVERSIONINFOEX implements PInvoke.Struct {
    public int sizeOf;
    
    public int majorVersion;
    
    public int minorVersion;
    
    public int buildNumber;
    
    public int platformId;
    
    @MarshalAs(sizeConst = 128)
    public String csdVersion;
    
    public short servicePackMajor;
    
    public short servicePackMinor;
    
    public short suiteMask;
    
    public byte productType;
    
    public byte reserved;
  }
}
