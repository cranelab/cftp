package net.engio.mbassy.listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.engio.mbassy.common.IPredicate;
import net.engio.mbassy.common.ReflectionUtils;

public class MessageListener<T> {
  public static IPredicate<MessageHandler> ForMessage(final Class<?> messageType) {
    return new IPredicate<MessageHandler>() {
        public boolean apply(MessageHandler target) {
          return target.handlesMessage(messageType);
        }
      };
  }
  
  private ArrayList<MessageHandler> handlers = new ArrayList<MessageHandler>();
  
  private Class<T> listenerDefinition;
  
  private Listener listenerAnnotation;
  
  public MessageListener(Class<T> listenerDefinition) {
    this.listenerDefinition = listenerDefinition;
    this.listenerAnnotation = ReflectionUtils.<Listener>getAnnotation(listenerDefinition, Listener.class);
  }
  
  public boolean isFromListener(Class listener) {
    return this.listenerDefinition.equals(listener);
  }
  
  public boolean useStrongReferences() {
    return (this.listenerAnnotation != null && this.listenerAnnotation.references().equals(References.Strong));
  }
  
  public MessageListener addHandlers(Collection<? extends MessageHandler> c) {
    this.handlers.addAll(c);
    return this;
  }
  
  public boolean addHandler(MessageHandler messageHandler) {
    return this.handlers.add(messageHandler);
  }
  
  public MessageHandler[] getHandlers() {
    MessageHandler[] asArray = new MessageHandler[this.handlers.size()];
    return this.handlers.<MessageHandler>toArray(asArray);
  }
  
  public List<MessageHandler> getHandlers(IPredicate<MessageHandler> filter) {
    List<MessageHandler> matching = new ArrayList<MessageHandler>();
    for (MessageHandler handler : this.handlers) {
      if (filter.apply(handler))
        matching.add(handler); 
    } 
    return matching;
  }
  
  public boolean handles(Class<?> messageType) {
    return !getHandlers(ForMessage(messageType)).isEmpty();
  }
  
  public Class<T> getListerDefinition() {
    return this.listenerDefinition;
  }
}
