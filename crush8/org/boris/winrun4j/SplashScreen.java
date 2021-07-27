package org.boris.winrun4j;

public class SplashScreen {
  public static long getWindowHandle() {
    return NativeHelper.call(0L, "SplashScreen_GetWindowHandle", new long[0]);
  }
  
  public static void close() {
    NativeHelper.call(0L, "SplashScreen_Close", new long[0]);
  }
  
  public static void setText(String text, int x, int y) {
    long ptr = NativeHelper.toNativeString(text, false);
    NativeHelper.call(0L, "SplashScreen_SetText", new long[] { ptr, x, y });
    NativeHelper.free(new long[] { ptr });
  }
  
  public static void setTextFont(String text, int height) {
    long ptr = NativeHelper.toNativeString(text, false);
    NativeHelper.call(0L, "SplashScreen_SetTextFont", new long[] { ptr, height });
    NativeHelper.free(new long[] { ptr });
  }
  
  public static void setTextColor(int r, int g, int b) {
    NativeHelper.call(0L, "SplashScreen_SetTextColor", new long[] { r, g, b });
  }
  
  public static void setTextBgColor(int r, int g, int b) {
    NativeHelper.call(0L, "SplashScreen_SetBbColor", new long[] { r, g, b });
  }
}
