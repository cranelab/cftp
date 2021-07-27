package org.boris.winrun4j;

public class EventLog {
  public static final int SUCCESS = 0;
  
  public static final int ERROR = 1;
  
  public static final int WARNING = 2;
  
  public static final int INFORMATION = 4;
  
  public static final int AUDIT_SUCCESS = 8;
  
  public static final int AUDIT_FAILURE = 16;
  
  private static long library = Native.loadLibrary("advapi32");
  
  public static boolean report(String source, int type, String msg) {
    long buf = NativeHelper.toNativeString(source, true);
    long h = NativeHelper.call(library, "RegisterEventSourceW", new long[] { 0L, buf });
    long m = NativeHelper.toNativeString(msg, true);
    boolean res = (NativeHelper.call(library, "ReportEventW", new long[] { h, type, 0L, 0L, 0L, 0L, (msg.length() * 2), 0L, m }) == 1L);
    NativeHelper.free(new long[] { buf, m });
    return res;
  }
}
