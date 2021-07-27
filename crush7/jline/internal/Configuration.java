package jline.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Map;
import java.util.Properties;

public class Configuration {
  public static final String JLINE_CONFIGURATION = "jline.configuration";
  
  public static final String JLINE_RC = ".jline.rc";
  
  private static volatile Properties properties;
  
  private static Properties initProperties() {
    URL url = determineUrl();
    Properties props = new Properties();
    try {
      loadProperties(url, props);
    } catch (FileNotFoundException e) {
      Log.debug(new Object[] { "Unable to read configuration: ", e.toString() });
    } catch (IOException e) {
      Log.warn(new Object[] { "Unable to read configuration from: ", url, e });
    } 
    return props;
  }
  
  private static void loadProperties(URL url, Properties props) throws IOException {
    Log.debug(new Object[] { "Loading properties from: ", url });
    InputStream input = url.openStream();
    try {
      props.load(new BufferedInputStream(input));
    } finally {
      try {
        input.close();
      } catch (IOException iOException) {}
    } 
    if (Log.DEBUG) {
      Log.debug(new Object[] { "Loaded properties:" });
      for (Map.Entry<Object, Object> entry : props.entrySet()) {
        Log.debug(new Object[] { "  ", entry.getKey(), "=", entry.getValue() });
      } 
    } 
  }
  
  private static URL determineUrl() {
    String tmp = System.getProperty("jline.configuration");
    if (tmp != null)
      return Urls.create(tmp); 
    File file = new File(getUserHome(), ".jline.rc");
    return Urls.create(file);
  }
  
  public static void reset() {
    Log.debug(new Object[] { "Resetting" });
    properties = null;
    getProperties();
  }
  
  public static Properties getProperties() {
    if (properties == null)
      properties = initProperties(); 
    return properties;
  }
  
  public static String getString(String name, String defaultValue) {
    Preconditions.checkNotNull(name);
    String value = System.getProperty(name);
    if (value == null) {
      value = getProperties().getProperty(name);
      if (value == null)
        value = defaultValue; 
    } 
    return value;
  }
  
  public static String getString(String name) {
    return getString(name, null);
  }
  
  public static boolean getBoolean(String name) {
    return getBoolean(name, false);
  }
  
  public static boolean getBoolean(String name, boolean defaultValue) {
    String value = getString(name);
    if (value == null)
      return defaultValue; 
    return (value.length() == 0 || value
      .equalsIgnoreCase("1") || value
      .equalsIgnoreCase("on") || value
      .equalsIgnoreCase("true"));
  }
  
  public static int getInteger(String name, int defaultValue) {
    String str = getString(name);
    if (str == null)
      return defaultValue; 
    return Integer.parseInt(str);
  }
  
  public static long getLong(String name, long defaultValue) {
    String str = getString(name);
    if (str == null)
      return defaultValue; 
    return Long.parseLong(str);
  }
  
  public static String getLineSeparator() {
    return System.getProperty("line.separator");
  }
  
  public static File getUserHome() {
    return new File(System.getProperty("user.home"));
  }
  
  public static String getOsName() {
    return System.getProperty("os.name").toLowerCase();
  }
  
  public static boolean isWindows() {
    return getOsName().startsWith("windows");
  }
  
  public static boolean isHpux() {
    return getOsName().startsWith("hp");
  }
  
  public static String getFileEncoding() {
    return System.getProperty("file.encoding");
  }
  
  public static String getEncoding() {
    for (String envOption : new String[] { "LC_ALL", "LC_CTYPE", "LANG" }) {
      String envEncoding = extractEncodingFromCtype(System.getenv(envOption));
      if (envEncoding != null)
        try {
          if (Charset.isSupported(envEncoding))
            return envEncoding; 
        } catch (IllegalCharsetNameException e) {} 
    } 
    return getString("input.encoding", Charset.defaultCharset().name());
  }
  
  static String extractEncodingFromCtype(String ctype) {
    if (ctype != null && ctype.indexOf('.') > 0) {
      String encodingAndModifier = ctype.substring(ctype.indexOf('.') + 1);
      if (encodingAndModifier.indexOf('@') > 0)
        return encodingAndModifier.substring(0, encodingAndModifier.indexOf('@')); 
      return encodingAndModifier;
    } 
    return null;
  }
}
