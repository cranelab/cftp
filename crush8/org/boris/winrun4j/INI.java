package org.boris.winrun4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class INI {
  public static final String MAIN_CLASS = ":main.class";
  
  public static final String SERVICE_CLASS = ":service.class";
  
  public static final String MODULE_NAME = "WinRun4J:module.name";
  
  public static final String MODULE_INI = "Winrun4J:module.ini";
  
  public static final String MODULE_DIR = "WinRun4J:module.dir";
  
  public static final String INI_DIR = "WinRun4J:ini.dir";
  
  public static final String WORKING_DIR = ":working.directory";
  
  public static final String SINGLE_INSTANCE = ":single.instance";
  
  public static final String DDE_ENABLED = ":dde.enabled";
  
  public static final String DDE_WINDOW_CLASS = ":dde.window.class";
  
  public static final String DDE_SERVER_NAME = ":dde.server.name";
  
  public static final String DDE_TOPIC = ":dde.topic";
  
  public static final String SERVICE_ID = ":service.id";
  
  public static final String SERVICE_NAME = ":service.name";
  
  public static final String SERVICE_DESCRIPTION = ":service.description";
  
  public static final String SERVICE_CONTROLS = ":service.controls";
  
  public static final String SERVICE_STARTUP = ":service.startup";
  
  public static final String SERVICE_DEPENDENCY = ":service.dependency";
  
  public static final String SERVICE_USER = ":service.user";
  
  public static final String SERVICE_PWD = ":service.password";
  
  public static final String SERVICE_LOAD_ORDER_GROUP = ":service.loadordergroup";
  
  static {
    PInvoke.bind(INI.class);
  }
  
  public static String getProperty(String key) {
    long k = NativeHelper.toNativeString(key, false);
    long r = NativeHelper.call(0L, "INI_GetProperty", new long[] { k });
    String res = null;
    if (r != 0L)
      res = NativeHelper.getString(r, 4096L, false); 
    NativeHelper.free(new long[] { k });
    return res;
  }
  
  public static String getProperty(String key, String defaultValue) {
    String res = getProperty(key);
    return (res != null) ? res : defaultValue;
  }
  
  public static String[] getPropertyKeys() {
    long d = NativeHelper.call(0L, "INI_GetDictionary", new long[0]);
    int n = NativeHelper.getInt(d);
    long keyPtr = NativeHelper.getInt(d + (Native.IS_64 ? 16L : 12L));
    String[] res = new String[n];
    int offset;
    for (int i = 0; i < n; i++, offset += NativeHelper.PTR_SIZE) {
      long ptr = NativeHelper.getPointer(keyPtr + offset);
      res[i] = NativeHelper.getString(ptr, 260L, false);
    } 
    return res;
  }
  
  public static Map<String, String> getProperties() {
    Map<String, String> props = new HashMap<String, String>();
    String[] keys = getPropertyKeys();
    for (int i = 0; i < keys.length; i++)
      props.put(keys[i], getProperty(keys[i])); 
    return props;
  }
  
  public static String[] getNumberedEntries(String baseKey) {
    String v;
    ArrayList<String> l = new ArrayList();
    int i = 1;
    do {
      v = getProperty(baseKey + "." + i);
      if (v == null)
        continue; 
      l.add(v);
      ++i;
    } while (i <= 10 || v != null);
    return l.<String>toArray(new String[l.size()]);
  }
}
