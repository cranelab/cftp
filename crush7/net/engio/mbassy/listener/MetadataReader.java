package net.engio.mbassy.listener;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.engio.mbassy.common.IPredicate;
import net.engio.mbassy.common.ReflectionUtils;
import net.engio.mbassy.subscription.MessageEnvelope;

public class MetadataReader {
  private static final IPredicate<Method> AllMessageHandlers = new IPredicate<Method>() {
      public boolean apply(Method target) {
        return (ReflectionUtils.getAnnotation(target, Handler.class) != null);
      }
    };
  
  private final Map<Class<? extends IMessageFilter>, IMessageFilter> filterCache = new HashMap<Class<? extends IMessageFilter>, IMessageFilter>();
  
  private IMessageFilter[] getFilter(Method method, Handler subscription) {
    Filter[] filterDefinitions = collectFilters(method, subscription);
    if (filterDefinitions.length == 0)
      return null; 
    IMessageFilter[] filters = new IMessageFilter[filterDefinitions.length];
    int i = 0;
    for (Filter filterDef : filterDefinitions) {
      IMessageFilter filter = this.filterCache.get(filterDef.value());
      if (filter == null)
        try {
          filter = filterDef.value().newInstance();
          this.filterCache.put(filterDef.value(), filter);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }  
      filters[i] = filter;
      i++;
    } 
    return filters;
  }
  
  private Filter[] collectFilters(Method method, Handler subscription) {
    List<Filter> filters = new ArrayList<Filter>((subscription.filters()).length);
    Collections.addAll(filters, subscription.filters());
    Annotation[] annotations = method.getAnnotations();
    for (int i = 0; i < (method.getAnnotations()).length; i++) {
      Class<? extends Annotation> annotationType = annotations[i].annotationType();
      IncludeFilters repeated = annotationType.<IncludeFilters>getAnnotation(IncludeFilters.class);
      if (repeated != null)
        Collections.addAll(filters, repeated.value()); 
      Filter filter = annotationType.<Filter>getAnnotation(Filter.class);
      if (filter != null)
        filters.add(filter); 
    } 
    return filters.<Filter>toArray(new Filter[filters.size()]);
  }
  
  public MessageListener getMessageListener(Class<?> target) {
    MessageListener listenerMetadata = new MessageListener(target);
    Method[] allHandlers = ReflectionUtils.getMethods(AllMessageHandlers, target);
    int length = allHandlers.length;
    for (int i = 0; i < length; i++) {
      Method handler = allHandlers[i];
      if (!ReflectionUtils.containsOverridingMethod(allHandlers, handler)) {
        Handler handlerConfig = ReflectionUtils.<Handler>getAnnotation(handler, Handler.class);
        if (handlerConfig.enabled() && isValidMessageHandler(handler)) {
          Method overriddenHandler = ReflectionUtils.getOverridingMethod(handler, target);
          Map<String, Object> handlerProperties = MessageHandler.Properties.Create((overriddenHandler == null) ? handler : overriddenHandler, handlerConfig, 
              
              getFilter(handler, handlerConfig), listenerMetadata);
          MessageHandler handlerMetadata = new MessageHandler(handlerProperties);
          listenerMetadata.addHandler(handlerMetadata);
        } 
      } 
    } 
    return listenerMetadata;
  }
  
  private boolean isValidMessageHandler(Method handler) {
    if (handler == null || ReflectionUtils.getAnnotation(handler, Handler.class) == null)
      return false; 
    if ((handler.getParameterTypes()).length != 1) {
      System.out.println("Found no or more than one parameter in messageHandler [" + handler.getName() + "]. A messageHandler must define exactly one parameter");
      return false;
    } 
    Enveloped envelope = ReflectionUtils.<Enveloped>getAnnotation(handler, Enveloped.class);
    if (envelope != null && !MessageEnvelope.class.isAssignableFrom(handler.getParameterTypes()[0])) {
      System.out.println("Message envelope configured but no subclass of MessageEnvelope found as parameter");
      return false;
    } 
    if (envelope != null && (envelope.messages()).length == 0) {
      System.out.println("Message envelope configured but message types defined for handler");
      return false;
    } 
    return true;
  }
}
