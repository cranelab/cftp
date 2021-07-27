package net.engio.mbassy.bus.config;

import java.util.Collection;
import net.engio.mbassy.bus.error.IPublicationErrorHandler;

public interface IBusConfiguration {
  IBusConfiguration setProperty(String paramString, Object paramObject);
  
  <T> T getProperty(String paramString, T paramT);
  
  boolean hasProperty(String paramString);
  
  <T extends Feature> T getFeature(Class<T> paramClass);
  
  IBusConfiguration addFeature(Feature paramFeature);
  
  BusConfiguration addPublicationErrorHandler(IPublicationErrorHandler paramIPublicationErrorHandler);
  
  Collection<IPublicationErrorHandler> getRegisteredPublicationErrorHandlers();
  
  public static final class Properties {
    public static final String BusId = "bus.id";
    
    public static final String PublicationErrorHandlers = "bus.handlers.error";
    
    public static final String AsynchronousHandlerExecutor = "bus.handlers.async-executor";
  }
}
