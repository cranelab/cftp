package net.engio.mbassy.bus;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.engio.mbassy.bus.common.PubSubSupport;
import net.engio.mbassy.bus.error.MissingPropertyException;

public class BusRuntime {
  private PubSubSupport provider;
  
  private Map<String, Object> properties = new HashMap<String, Object>();
  
  public BusRuntime(PubSubSupport provider) {
    this.provider = provider;
  }
  
  public <T> T get(String key) {
    if (!contains(key))
      throw new MissingPropertyException("The property " + key + " is not available in this runtime"); 
    return (T)this.properties.get(key);
  }
  
  public PubSubSupport getProvider() {
    return this.provider;
  }
  
  public Collection<String> getKeys() {
    return this.properties.keySet();
  }
  
  public BusRuntime add(String key, Object property) {
    this.properties.put(key, property);
    return this;
  }
  
  public boolean contains(String key) {
    return this.properties.containsKey(key);
  }
}
