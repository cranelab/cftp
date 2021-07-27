package net.engio.mbassy.subscription;

import java.util.Collection;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.engio.mbassy.bus.MessagePublication;
import net.engio.mbassy.dispatch.IMessageDispatcher;

public class Subscription {
  private final UUID id = UUID.randomUUID();
  
  protected final Collection<Object> listeners;
  
  private final IMessageDispatcher dispatcher;
  
  private final SubscriptionContext context;
  
  private final CopyOnWriteArrayList<Runnable> onSubscription = new CopyOnWriteArrayList<Runnable>();
  
  Subscription(SubscriptionContext context, IMessageDispatcher dispatcher, Collection<Object> listeners) {
    this.context = context;
    this.dispatcher = dispatcher;
    this.listeners = listeners;
  }
  
  public boolean belongsTo(Class listener) {
    return this.context.getHandler().isFromListener(listener);
  }
  
  public boolean contains(Object listener) {
    return this.listeners.contains(listener);
  }
  
  public boolean handlesMessageType(Class<?> messageType) {
    return this.context.getHandler().handlesMessage(messageType);
  }
  
  public Class[] getHandledMessageTypes() {
    return this.context.getHandler().getHandledMessages();
  }
  
  public void publish(MessagePublication publication, Object message) {
    if (!this.listeners.isEmpty())
      this.dispatcher.dispatch(publication, message, this.listeners); 
  }
  
  public int getPriority() {
    return this.context.getHandler().getPriority();
  }
  
  public void subscribe(Object o) {
    this.listeners.add(o);
    for (Runnable callback : (Runnable[])this.onSubscription.<Runnable>toArray(new Runnable[0]))
      callback.run(); 
  }
  
  public boolean unsubscribe(Object existingListener) {
    return this.listeners.remove(existingListener);
  }
  
  public int size() {
    return this.listeners.size();
  }
  
  public Handle getHandle() {
    return new Handle();
  }
  
  public static final Comparator<Subscription> SubscriptionByPriorityDesc = new Comparator<Subscription>() {
      public int compare(Subscription o1, Subscription o2) {
        int byPriority = Integer.valueOf(o2.getPriority()).compareTo(Integer.valueOf(o1.getPriority()));
        return (byPriority == 0) ? o2.id.compareTo(o1.id) : byPriority;
      }
    };
  
  public class Handle {
    void onSubscription(Runnable handler) {
      Subscription.this.onSubscription.add(handler);
    }
  }
}
