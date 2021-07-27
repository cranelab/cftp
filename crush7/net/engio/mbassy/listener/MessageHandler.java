package net.engio.mbassy.listener;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import net.engio.mbassy.common.ReflectionUtils;
import net.engio.mbassy.dispatch.HandlerInvocation;
import net.engio.mbassy.dispatch.el.ElFilter;

public class MessageHandler {
  private final Method handler;
  
  private final IMessageFilter[] filter;
  
  private final String condition;
  
  private final int priority;
  
  private final Class<? extends HandlerInvocation> invocation;
  
  private final Invoke invocationMode;
  
  private final boolean isEnvelope;
  
  private final Class[] handledMessages;
  
  private final boolean acceptsSubtypes;
  
  private final MessageListener listenerConfig;
  
  private final boolean isSynchronized;
  
  public static final class Properties {
    public static final String HandlerMethod = "handler";
    
    public static final String InvocationMode = "invocationMode";
    
    public static final String Filter = "filter";
    
    public static final String Condition = "condition";
    
    public static final String Enveloped = "envelope";
    
    public static final String HandledMessages = "messages";
    
    public static final String IsSynchronized = "synchronized";
    
    public static final String Listener = "listener";
    
    public static final String AcceptSubtypes = "subtypes";
    
    public static final String Priority = "priority";
    
    public static final String Invocation = "invocation";
    
    public static final Map<String, Object> Create(Method handler, Handler handlerConfig, IMessageFilter[] filter, MessageListener listenerConfig) {
      if (handler == null)
        throw new IllegalArgumentException("The message handler configuration may not be null"); 
      if (filter == null)
        filter = new IMessageFilter[0]; 
      Enveloped enveloped = ReflectionUtils.<Enveloped>getAnnotation(handler, Enveloped.class);
      Class[] handledMessages = (enveloped != null) ? enveloped.messages() : handler.getParameterTypes();
      handler.setAccessible(true);
      Map<String, Object> properties = new HashMap<String, Object>();
      properties.put("handler", handler);
      if (handlerConfig.condition().length() > 0) {
        if (!ElFilter.isELAvailable())
          throw new IllegalStateException("A handler uses an EL filter but no EL implementation is available."); 
        IMessageFilter[] expandedFilter = new IMessageFilter[filter.length + 1];
        for (int i = 0; i < filter.length; i++)
          expandedFilter[i] = filter[i]; 
        expandedFilter[filter.length] = new ElFilter();
        filter = expandedFilter;
      } 
      properties.put("filter", filter);
      properties.put("condition", cleanEL(handlerConfig.condition()));
      properties.put("priority", Integer.valueOf(handlerConfig.priority()));
      properties.put("invocation", handlerConfig.invocation());
      properties.put("invocationMode", handlerConfig.delivery());
      properties.put("envelope", Boolean.valueOf((enveloped != null)));
      properties.put("subtypes", Boolean.valueOf(!handlerConfig.rejectSubtypes()));
      properties.put("listener", listenerConfig);
      properties.put("synchronized", Boolean.valueOf((ReflectionUtils.getAnnotation(handler, Synchronized.class) != null)));
      properties.put("messages", handledMessages);
      return properties;
    }
    
    private static String cleanEL(String expression) {
      if (!expression.trim().startsWith("${") && !expression.trim().startsWith("#{"))
        expression = "${" + expression + "}"; 
      return expression;
    }
  }
  
  public MessageHandler(Map<String, Object> properties) {
    validate(properties);
    this.handler = (Method)properties.get("handler");
    this.filter = (IMessageFilter[])properties.get("filter");
    this.condition = (String)properties.get("condition");
    this.priority = ((Integer)properties.get("priority")).intValue();
    this.invocation = (Class<? extends HandlerInvocation>)properties.get("invocation");
    this.invocationMode = (Invoke)properties.get("invocationMode");
    this.isEnvelope = ((Boolean)properties.get("envelope")).booleanValue();
    this.acceptsSubtypes = ((Boolean)properties.get("subtypes")).booleanValue();
    this.listenerConfig = (MessageListener)properties.get("listener");
    this.isSynchronized = ((Boolean)properties.get("synchronized")).booleanValue();
    this.handledMessages = (Class[])properties.get("messages");
  }
  
  private void validate(Map<String, Object> properties) {
    Object[][] expectedProperties = { { "handler", Method.class }, { "priority", Integer.class }, { "invocation", Class.class }, { "filter", IMessageFilter[].class }, { "condition", String.class }, { "envelope", Boolean.class }, { "messages", Class[].class }, { "synchronized", Boolean.class }, { "listener", MessageListener.class }, { "subtypes", Boolean.class } };
    for (Object[] property : expectedProperties) {
      if (properties.get(property[0]) == null || !((Class)property[1]).isAssignableFrom(properties.get(property[0]).getClass()))
        throw new IllegalArgumentException("Property " + property[0] + " was expected to be not null and of type " + property[1] + " but was: " + properties
            .get(property[0])); 
    } 
  }
  
  public <A extends java.lang.annotation.Annotation> A getAnnotation(Class<A> annotationType) {
    return ReflectionUtils.getAnnotation(this.handler, annotationType);
  }
  
  public boolean isSynchronized() {
    return this.isSynchronized;
  }
  
  public boolean useStrongReferences() {
    return this.listenerConfig.useStrongReferences();
  }
  
  public boolean isFromListener(Class listener) {
    return this.listenerConfig.isFromListener(listener);
  }
  
  public boolean isAsynchronous() {
    return this.invocationMode.equals(Invoke.Asynchronously);
  }
  
  public boolean isFiltered() {
    return (this.filter.length > 0 || (this.condition != null && this.condition.trim().length() > 0));
  }
  
  public int getPriority() {
    return this.priority;
  }
  
  public Method getMethod() {
    return this.handler;
  }
  
  public IMessageFilter[] getFilter() {
    return this.filter;
  }
  
  public String getCondition() {
    return this.condition;
  }
  
  public Class[] getHandledMessages() {
    return this.handledMessages;
  }
  
  public boolean isEnveloped() {
    return this.isEnvelope;
  }
  
  public Class<? extends HandlerInvocation> getHandlerInvocation() {
    return this.invocation;
  }
  
  public boolean handlesMessage(Class<?> messageType) {
    for (Class<?> handledMessage : this.handledMessages) {
      if (handledMessage.equals(messageType))
        return true; 
      if (handledMessage.isAssignableFrom(messageType) && acceptsSubtypes())
        return true; 
    } 
    return false;
  }
  
  public boolean acceptsSubtypes() {
    return this.acceptsSubtypes;
  }
}
