package net.engio.mbassy.bus;

import net.engio.mbassy.bus.common.ErrorHandlingSupport;
import net.engio.mbassy.bus.common.GenericMessagePublicationSupport;
import net.engio.mbassy.bus.common.PubSubSupport;
import net.engio.mbassy.bus.config.BusConfiguration;
import net.engio.mbassy.bus.config.Feature;
import net.engio.mbassy.bus.config.IBusConfiguration;
import net.engio.mbassy.bus.error.IPublicationErrorHandler;
import net.engio.mbassy.bus.publication.IPublicationCommand;

public class SyncMessageBus<T> extends AbstractPubSubSupport<T> implements PubSubSupport<T>, ErrorHandlingSupport, GenericMessagePublicationSupport<T, SyncMessageBus.SyncPostCommand> {
  public SyncMessageBus() {
    super((new BusConfiguration()).addFeature(Feature.SyncPubSub.Default()));
  }
  
  public SyncMessageBus(IPublicationErrorHandler errorHandler) {
    super((new BusConfiguration()).addFeature(Feature.SyncPubSub.Default()).addPublicationErrorHandler(errorHandler));
  }
  
  public SyncMessageBus(IBusConfiguration configuration) {
    super(configuration);
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
  
  public SyncPostCommand post(T message) {
    return new SyncPostCommand(message);
  }
  
  public class SyncPostCommand implements IPublicationCommand {
    private T message;
    
    public SyncPostCommand(T message) {
      this.message = message;
    }
    
    public IMessagePublication now() {
      return SyncMessageBus.this.publish(this.message);
    }
  }
}
