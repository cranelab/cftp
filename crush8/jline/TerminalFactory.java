package jline;

import java.lang.reflect.Constructor;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import jline.internal.Configuration;
import jline.internal.Log;
import jline.internal.Preconditions;

public class TerminalFactory {
  public static final String JLINE_TERMINAL = "jline.terminal";
  
  public static final String AUTO = "auto";
  
  public static final String UNIX = "unix";
  
  public static final String OSV = "osv";
  
  public static final String WIN = "win";
  
  public static final String WINDOWS = "windows";
  
  public static final String FREEBSD = "freebsd";
  
  public static final String NONE = "none";
  
  public static final String OFF = "off";
  
  public static final String FALSE = "false";
  
  private static Terminal term = null;
  
  public static synchronized Terminal create() {
    return create(null);
  }
  
  public static synchronized Terminal create(String ttyDevice) {
    Terminal t;
    if (Log.TRACE)
      Log.trace(new Object[] { new Throwable("CREATE MARKER") }); 
    String type = Configuration.getString("jline.terminal");
    if (type == null) {
      type = "auto";
      if ("dumb".equals(System.getenv("TERM"))) {
        String emacs = System.getenv("EMACS");
        String insideEmacs = System.getenv("INSIDE_EMACS");
        if (emacs == null || insideEmacs == null)
          type = "none"; 
      } 
    } 
    Log.debug(new Object[] { "Creating terminal; type=", type });
    try {
      String tmp = type.toLowerCase();
      if (tmp.equals("unix")) {
        t = getFlavor(Flavor.UNIX);
      } else if (tmp.equals("osv")) {
        t = getFlavor(Flavor.OSV);
      } else if (tmp.equals("win") || tmp.equals("windows")) {
        t = getFlavor(Flavor.WINDOWS);
      } else if (tmp.equals("none") || tmp.equals("off") || tmp.equals("false")) {
        t = new UnsupportedTerminal();
      } else if (tmp.equals("auto")) {
        String os = Configuration.getOsName();
        Flavor flavor = Flavor.UNIX;
        if (os.contains("windows")) {
          flavor = Flavor.WINDOWS;
        } else if (System.getenv("OSV_CPUS") != null) {
          flavor = Flavor.OSV;
        } 
        t = getFlavor(flavor, ttyDevice);
      } else {
        try {
          t = (Terminal)Thread.currentThread().getContextClassLoader().loadClass(type).newInstance();
        } catch (Exception e) {
          throw new IllegalArgumentException(MessageFormat.format("Invalid terminal type: {0}", new Object[] { type }), e);
        } 
      } 
    } catch (Exception e) {
      Log.error(new Object[] { "Failed to construct terminal; falling back to unsupported", e });
      t = new UnsupportedTerminal();
    } 
    Log.debug(new Object[] { "Created Terminal: ", t });
    try {
      t.init();
    } catch (Throwable e) {
      Log.error(new Object[] { "Terminal initialization failed; falling back to unsupported", e });
      return new UnsupportedTerminal();
    } 
    return t;
  }
  
  public static synchronized void reset() {
    term = null;
  }
  
  public static synchronized void resetIf(Terminal t) {
    if (t == term)
      reset(); 
  }
  
  public enum Type {
    AUTO, WINDOWS, UNIX, OSV, NONE;
  }
  
  public static synchronized void configure(String type) {
    Preconditions.checkNotNull(type);
    System.setProperty("jline.terminal", type);
  }
  
  public static synchronized void configure(Type type) {
    Preconditions.checkNotNull(type);
    configure(type.name().toLowerCase());
  }
  
  public enum Flavor {
    WINDOWS, UNIX, OSV;
  }
  
  private static final Map<Flavor, Class<? extends Terminal>> FLAVORS = new HashMap<Flavor, Class<? extends Terminal>>();
  
  static {
    registerFlavor(Flavor.WINDOWS, (Class)AnsiWindowsTerminal.class);
    registerFlavor(Flavor.UNIX, (Class)UnixTerminal.class);
    registerFlavor(Flavor.OSV, (Class)OSvTerminal.class);
  }
  
  public static synchronized Terminal get(String ttyDevice) {
    if (term == null)
      term = create(ttyDevice); 
    return term;
  }
  
  public static synchronized Terminal get() {
    return get(null);
  }
  
  public static Terminal getFlavor(Flavor flavor) throws Exception {
    return getFlavor(flavor, null);
  }
  
  public static Terminal getFlavor(Flavor flavor, String ttyDevice) throws Exception {
    Class<? extends Terminal> type = FLAVORS.get(flavor);
    Terminal result = null;
    if (type != null) {
      if (ttyDevice != null) {
        Constructor<?> ttyDeviceConstructor = type.getConstructor(new Class[] { String.class });
        if (ttyDeviceConstructor != null) {
          result = (Terminal)ttyDeviceConstructor.newInstance(new Object[] { ttyDevice });
        } else {
          result = type.newInstance();
        } 
      } else {
        result = type.newInstance();
      } 
    } else {
      throw new InternalError();
    } 
    return result;
  }
  
  public static void registerFlavor(Flavor flavor, Class<? extends Terminal> type) {
    FLAVORS.put(flavor, type);
  }
}
