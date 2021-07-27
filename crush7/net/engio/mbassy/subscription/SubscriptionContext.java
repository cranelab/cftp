package net.engio.mbassy.subscription;

import java.util.Collection;
import net.engio.mbassy.bus.BusRuntime;
import net.engio.mbassy.bus.common.RuntimeProvider;
import net.engio.mbassy.bus.error.IPublicationErrorHandler;
import net.engio.mbassy.bus.error.PublicationError;
import net.engio.mbassy.listener.MessageHandler;

public class SubscriptionContext implements RuntimeProvider {
  private final MessageHandler handler;
  
  private final Collection<IPublicationErrorHandler> errorHandlers;
  
  private final BusRuntime runtime;
  
  public SubscriptionContext(BusRuntime runtime, MessageHandler handler, Collection<IPublicationErrorHandler> errorHandlers) {
    this.runtime = runtime;
    this.handler = handler;
    this.errorHandlers = errorHandlers;
  }
  
  public MessageHandler getHandler() {
    return this.handler;
  }
  
  public Collection<IPublicationErrorHandler> getErrorHandlers() {
    return this.errorHandlers;
  }
  
  public BusRuntime getRuntime() {
    return this.runtime;
  }
  
  public final void handleError(PublicationError error) {
    for (IPublicationErrorHandler errorHandler : this.errorHandlers)
      errorHandler.handleError(error); 
  }
}
