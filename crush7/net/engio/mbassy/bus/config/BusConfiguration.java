package net.engio.mbassy.bus.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.engio.mbassy.bus.error.IPublicationErrorHandler;

public class BusConfiguration implements IBusConfiguration {
  private final Map<Object, Object> properties = new HashMap<Object, Object>();
  
  private final List<IPublicationErrorHandler> publicationErrorHandlers = new ArrayList<IPublicationErrorHandler>();
  
  public IBusConfiguration setProperty(String name, Object value) {
    this.properties.put(name, value);
    return this;
  }
  
  public <T> T getProperty(String name, T defaultValue) {
    return this.properties.containsKey(name) ? (T)this.properties.get(name) : defaultValue;
  }
  
  public boolean hasProperty(String name) {
    return this.properties.containsKey(name);
  }
  
  public <T extends Feature> T getFeature(Class<T> feature) {
    return (T)this.properties.get(feature);
  }
  
  public IBusConfiguration addFeature(Feature feature) {
    this.properties.put(feature.getClass(), feature);
    return this;
  }
  
  public final BusConfiguration addPublicationErrorHandler(IPublicationErrorHandler handler) {
    this.publicationErrorHandlers.add(handler);
    return this;
  }
  
  public Collection<IPublicationErrorHandler> getRegisteredPublicationErrorHandlers() {
    return Collections.unmodifiableCollection(this.publicationErrorHandlers);
  }
}
