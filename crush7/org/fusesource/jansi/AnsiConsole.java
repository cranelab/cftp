package org.fusesource.jansi;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import org.fusesource.jansi.internal.CLibrary;

public class AnsiConsole {
  public static final PrintStream system_out = System.out;
  
  public static final PrintStream out = new PrintStream(wrapOutputStream(system_out));
  
  public static final PrintStream system_err = System.err;
  
  public static final PrintStream err = new PrintStream(wrapOutputStream(system_err, CLibrary.STDERR_FILENO));
  
  private static int installed;
  
  public static OutputStream wrapOutputStream(OutputStream stream) {
    return wrapOutputStream(stream, CLibrary.STDOUT_FILENO);
  }
  
  public static OutputStream wrapOutputStream(OutputStream stream, int fileno) {
    if (Boolean.getBoolean("jansi.passthrough"))
      return stream; 
    if (Boolean.getBoolean("jansi.strip"))
      return new AnsiOutputStream(stream); 
    String os = System.getProperty("os.name");
    if (os.startsWith("Windows") && !isCygwin())
      try {
        return new WindowsAnsiOutputStream(stream);
      } catch (Throwable ignore) {
        return new AnsiOutputStream(stream);
      }  
    try {
      boolean forceColored = Boolean.getBoolean("jansi.force");
      int rc = CLibrary.isatty(fileno);
      if (!isCygwin() && !forceColored && rc == 0)
        return new AnsiOutputStream(stream); 
    } catch (NoClassDefFoundError ignore) {
    
    } catch (UnsatisfiedLinkError ignore) {}
    return new FilterOutputStream(stream) {
        public void close() throws IOException {
          write(AnsiOutputStream.REST_CODE);
          flush();
          super.close();
        }
      };
  }
  
  private static boolean isCygwin() {
    String term = System.getenv("TERM");
    return (term != null && term.equals("xterm"));
  }
  
  public static PrintStream out() {
    return out;
  }
  
  public static PrintStream err() {
    return err;
  }
  
  public static synchronized void systemInstall() {
    installed++;
    if (installed == 1) {
      System.setOut(out);
      System.setErr(err);
    } 
  }
  
  public static synchronized void systemUninstall() {
    installed--;
    if (installed == 0) {
      System.setOut(system_out);
      System.setErr(system_err);
    } 
  }
}
