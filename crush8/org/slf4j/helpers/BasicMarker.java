package org.slf4j.helpers;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import org.slf4j.Marker;

public class BasicMarker implements Marker {
  private static final long serialVersionUID = 1803952589649545191L;
  
  private final String name;
  
  private List refereceList;
  
  BasicMarker(String name) {
    if (name == null)
      throw new IllegalArgumentException("A marker name cannot be null"); 
    this.name = name;
  }
  
  public String getName() {
    return this.name;
  }
  
  public synchronized void add(Marker reference) {
    if (reference == null)
      throw new IllegalArgumentException("A null value cannot be added to a Marker as reference."); 
    if (contains(reference))
      return; 
    if (reference.contains(this))
      return; 
    if (this.refereceList == null)
      this.refereceList = new Vector(); 
    this.refereceList.add(reference);
  }
  
  public synchronized boolean hasReferences() {
    return (this.refereceList != null && this.refereceList.size() > 0);
  }
  
  public boolean hasChildren() {
    return hasReferences();
  }
  
  public synchronized Iterator iterator() {
    if (this.refereceList != null)
      return this.refereceList.iterator(); 
    return Collections.EMPTY_LIST.iterator();
  }
  
  public synchronized boolean remove(Marker referenceToRemove) {
    if (this.refereceList == null)
      return false; 
    int size = this.refereceList.size();
    for (int i = 0; i < size; i++) {
      Marker m = this.refereceList.get(i);
      if (referenceToRemove.equals(m)) {
        this.refereceList.remove(i);
        return true;
      } 
    } 
    return false;
  }
  
  public boolean contains(Marker other) {
    if (other == null)
      throw new IllegalArgumentException("Other cannot be null"); 
    if (equals(other))
      return true; 
    if (hasReferences())
      for (int i = 0; i < this.refereceList.size(); i++) {
        Marker ref = this.refereceList.get(i);
        if (ref.contains(other))
          return true; 
      }  
    return false;
  }
  
  public boolean contains(String name) {
    if (name == null)
      throw new IllegalArgumentException("Other cannot be null"); 
    if (this.name.equals(name))
      return true; 
    if (hasReferences())
      for (int i = 0; i < this.refereceList.size(); i++) {
        Marker ref = this.refereceList.get(i);
        if (ref.contains(name))
          return true; 
      }  
    return false;
  }
  
  private static String OPEN = "[ ";
  
  private static String CLOSE = " ]";
  
  private static String SEP = ", ";
  
  public boolean equals(Object obj) {
    if (this == obj)
      return true; 
    if (obj == null)
      return false; 
    if (!(obj instanceof Marker))
      return false; 
    Marker other = (Marker)obj;
    return this.name.equals(other.getName());
  }
  
  public int hashCode() {
    return this.name.hashCode();
  }
  
  public String toString() {
    if (!hasReferences())
      return getName(); 
    Iterator<Marker> it = iterator();
    StringBuffer sb = new StringBuffer(getName());
    sb.append(' ').append(OPEN);
    while (it.hasNext()) {
      Marker reference = it.next();
      sb.append(reference.getName());
      if (it.hasNext())
        sb.append(SEP); 
    } 
    sb.append(CLOSE);
    return sb.toString();
  }
}
