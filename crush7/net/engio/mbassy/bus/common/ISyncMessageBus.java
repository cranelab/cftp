package net.engio.mbassy.bus.common;

public interface ISyncMessageBus<T, P extends net.engio.mbassy.bus.publication.IPublicationCommand> extends PubSubSupport<T>, ErrorHandlingSupport, GenericMessagePublicationSupport<T, P> {}
