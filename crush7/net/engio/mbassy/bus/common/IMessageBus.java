package net.engio.mbassy.bus.common;

public interface IMessageBus<T, P extends net.engio.mbassy.bus.publication.ISyncAsyncPublicationCommand> extends GenericMessagePublicationSupport<T, P> {
  P post(T paramT);
  
  boolean hasPendingMessages();
  
  void shutdown();
}
