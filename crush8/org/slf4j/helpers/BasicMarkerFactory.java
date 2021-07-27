package org.slf4j.helpers;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.IMarkerFactory;
import org.slf4j.Marker;

public class BasicMarkerFactory implements IMarkerFactory {
  Map markerMap = new HashMap<Object, Object>();
  
  public synchronized Marker getMarker(String name) {
    if (name == null)
      throw new IllegalArgumentException("Marker name cannot be null"); 
    Marker marker = (Marker)this.markerMap.get(name);
    if (marker == null) {
      marker = new BasicMarker(name);
      this.markerMap.put(name, marker);
    } 
    return marker;
  }
  
  public synchronized boolean exists(String name) {
    if (name == null)
      return false; 
    return this.markerMap.containsKey(name);
  }
  
  public boolean detachMarker(String name) {
    if (name == null)
      return false; 
    return (this.markerMap.remove(name) != null);
  }
  
  public Marker getDetachedMarker(String name) {
    return new BasicMarker(name);
  }
}
