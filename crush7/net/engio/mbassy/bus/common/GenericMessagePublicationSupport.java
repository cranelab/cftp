package net.engio.mbassy.bus.common;

public interface GenericMessagePublicationSupport<T, P extends net.engio.mbassy.bus.publication.IPublicationCommand> extends PubSubSupport<T>, ErrorHandlingSupport {
  P post(T paramT);
}
