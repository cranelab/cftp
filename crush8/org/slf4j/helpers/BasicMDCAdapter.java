package org.slf4j.helpers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.spi.MDCAdapter;

public class BasicMDCAdapter implements MDCAdapter {
  private InheritableThreadLocal inheritableThreadLocal = new InheritableThreadLocal();
  
  static boolean isJDK14() {
    try {
      String javaVersion = System.getProperty("java.version");
      return javaVersion.startsWith("1.4");
    } catch (SecurityException se) {
      return false;
    } 
  }
  
  static boolean IS_JDK14 = isJDK14();
  
  public void put(String key, String val) {
    if (key == null)
      throw new IllegalArgumentException("key cannot be null"); 
    Map<?, ?> map = this.inheritableThreadLocal.get();
    if (map == null) {
      map = Collections.synchronizedMap(new HashMap<Object, Object>());
      this.inheritableThreadLocal.set(map);
    } 
    map.put(key, val);
  }
  
  public String get(String key) {
    Map Map = this.inheritableThreadLocal.get();
    if (Map != null && key != null)
      return (String)Map.get(key); 
    return null;
  }
  
  public void remove(String key) {
    Map map = this.inheritableThreadLocal.get();
    if (map != null)
      map.remove(key); 
  }
  
  public void clear() {
    Map map = this.inheritableThreadLocal.get();
    if (map != null) {
      map.clear();
      if (isJDK14()) {
        this.inheritableThreadLocal.set(null);
      } else {
        this.inheritableThreadLocal.remove();
      } 
    } 
  }
  
  public Set getKeys() {
    Map map = this.inheritableThreadLocal.get();
    if (map != null)
      return map.keySet(); 
    return null;
  }
  
  public Map getCopyOfContextMap() {
    Map<?, ?> oldMap = this.inheritableThreadLocal.get();
    if (oldMap != null) {
      Map<?, ?> newMap = Collections.synchronizedMap(new HashMap<Object, Object>());
      synchronized (oldMap) {
        newMap.putAll(oldMap);
      } 
      return newMap;
    } 
    return null;
  }
  
  public void setContextMap(Map<?, ?> contextMap) {
    Map<?, ?> map = Collections.synchronizedMap(new HashMap<Object, Object>(contextMap));
    this.inheritableThreadLocal.set(map);
  }
}
