package org.slf4j.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.log4j.MDC;
import org.slf4j.spi.MDCAdapter;

public class Log4jMDCAdapter implements MDCAdapter {
  public void clear() {
    Map map = MDC.getContext();
    if (map != null)
      map.clear(); 
  }
  
  public String get(String key) {
    return (String)MDC.get(key);
  }
  
  public void put(String key, String val) {
    MDC.put(key, val);
  }
  
  public void remove(String key) {
    MDC.remove(key);
  }
  
  public Map getCopyOfContextMap() {
    Map<?, ?> old = MDC.getContext();
    if (old != null)
      return new HashMap<Object, Object>(old); 
    return null;
  }
  
  public void setContextMap(Map contextMap) {
    Map old = MDC.getContext();
    if (old == null) {
      Iterator<Map.Entry> entrySetIterator = contextMap.entrySet().iterator();
      while (entrySetIterator.hasNext()) {
        Map.Entry mapEntry = entrySetIterator.next();
        MDC.put((String)mapEntry.getKey(), mapEntry.getValue());
      } 
    } else {
      old.clear();
      old.putAll(contextMap);
    } 
  }
}
