package net.engio.mbassy.bus;

import java.util.concurrent.TimeUnit;
import net.engio.mbassy.bus.common.IMessageBus;
import net.engio.mbassy.bus.config.BusConfiguration;
import net.engio.mbassy.bus.config.Feature;
import net.engio.mbassy.bus.config.IBusConfiguration;
import net.engio.mbassy.bus.error.IPublicationErrorHandler;
import net.engio.mbassy.bus.publication.IPublicationCommand;
import net.engio.mbassy.bus.publication.ISyncAsyncPublicationCommand;
import net.engio.mbassy.bus.publication.SyncAsyncPostCommand;

public class MBassador<T> extends AbstractSyncAsyncMessageBus<T, SyncAsyncPostCommand<T>> implements IMessageBus<T, SyncAsyncPostCommand<T>> {
  public MBassador() {
    this((new BusConfiguration())
        .addFeature(Feature.SyncPubSub.Default())
        .addFeature(Feature.AsynchronousHandlerInvocation.Default())
        .addFeature(Feature.AsynchronousMessageDispatch.Default()));
  }
  
  public MBassador(IPublicationErrorHandler errorHandler) {
    super((new BusConfiguration()).addFeature(Feature.SyncPubSub.Default())
        .addFeature(Feature.AsynchronousHandlerInvocation.Default())
        .addFeature(Feature.AsynchronousMessageDispatch.Default())
        .addPublicationErrorHandler(errorHandler));
  }
  
  public MBassador(IBusConfiguration configuration) {
    super(configuration);
  }
  
  public IMessagePublication publishAsync(T message) {
    return addAsynchronousPublication(createMessagePublication(message));
  }
  
  public IMessagePublication publishAsync(T message, long timeout, TimeUnit unit) {
    return addAsynchronousPublication(createMessagePublication(message), timeout, unit);
  }
  
  public IMessagePublication publish(T message) {
    IMessagePublication publication = createMessagePublication(message);
    try {
      return publication;
    } catch (Throwable e) {
      return publication;
    } finally {
      Exception exception = null;
    } 
  }
  
  public SyncAsyncPostCommand<T> post(T message) {
    return new SyncAsyncPostCommand<T>(this, message);
  }
}
