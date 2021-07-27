package net.engio.mbassy.bus.common;

import net.engio.mbassy.bus.IMessagePublication;

public interface PubSubSupport<T> extends RuntimeProvider {
  void subscribe(Object paramObject);
  
  boolean unsubscribe(Object paramObject);
  
  IMessagePublication publish(T paramT);
}
