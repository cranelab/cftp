package net.engio.mbassy.bus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.engio.mbassy.bus.common.DeadMessage;
import net.engio.mbassy.bus.common.PubSubSupport;
import net.engio.mbassy.bus.config.ConfigurationError;
import net.engio.mbassy.bus.config.Feature;
import net.engio.mbassy.bus.config.IBusConfiguration;
import net.engio.mbassy.bus.error.IPublicationErrorHandler;
import net.engio.mbassy.bus.error.PublicationError;
import net.engio.mbassy.subscription.Subscription;
import net.engio.mbassy.subscription.SubscriptionManager;

public abstract class AbstractPubSubSupport<T> implements PubSubSupport<T> {
  private final List<IPublicationErrorHandler> errorHandlers = new ArrayList<IPublicationErrorHandler>();
  
  private final MessagePublication.Factory publicationFactory;
  
  private final SubscriptionManager subscriptionManager;
  
  private final BusRuntime runtime;
  
  public static final String ERROR_HANDLER_MSG = "INFO: No error handler has been configured to handle exceptions during publication.\nPublication error handlers can be added by IBusConfiguration.addPublicationErrorHandler()\nFalling back to console logger.";
  
  public AbstractPubSubSupport(IBusConfiguration configuration) {
    this.errorHandlers.addAll(configuration.getRegisteredPublicationErrorHandlers());
    if (this.errorHandlers.isEmpty()) {
      this.errorHandlers.add(new IPublicationErrorHandler.ConsoleLogger());
      System.out.println("INFO: No error handler has been configured to handle exceptions during publication.\nPublication error handlers can be added by IBusConfiguration.addPublicationErrorHandler()\nFalling back to console logger.");
    } 
    this
      
      .runtime = (new BusRuntime(this)).add("bus.handlers.error", configuration.getRegisteredPublicationErrorHandlers()).add("bus.id", configuration.getProperty("bus.id", UUID.randomUUID().toString()));
    Feature.SyncPubSub pubSubFeature = configuration.<Feature.SyncPubSub>getFeature(Feature.SyncPubSub.class);
    if (pubSubFeature == null)
      throw ConfigurationError.MissingFeature(Feature.SyncPubSub.class); 
    this
      .subscriptionManager = pubSubFeature.getSubscriptionManagerProvider().createManager(pubSubFeature.getMetadataReader(), pubSubFeature.getSubscriptionFactory(), this.runtime);
    this.publicationFactory = pubSubFeature.getPublicationFactory();
  }
  
  protected MessagePublication.Factory getPublicationFactory() {
    return this.publicationFactory;
  }
  
  public Collection<IPublicationErrorHandler> getRegisteredErrorHandlers() {
    return Collections.unmodifiableCollection(this.errorHandlers);
  }
  
  public boolean unsubscribe(Object listener) {
    return this.subscriptionManager.unsubscribe(listener);
  }
  
  public void subscribe(Object listener) {
    this.subscriptionManager.subscribe(listener);
  }
  
  public BusRuntime getRuntime() {
    return this.runtime;
  }
  
  protected MessagePublication createMessagePublication(T message) {
    Collection<Subscription> subscriptions = getSubscriptionsByMessageType(message.getClass());
    if ((subscriptions == null || subscriptions.isEmpty()) && 
      !message.getClass().equals(DeadMessage.class)) {
      subscriptions = getSubscriptionsByMessageType(DeadMessage.class);
      return getPublicationFactory().createPublication(this.runtime, subscriptions, new DeadMessage(message));
    } 
    return getPublicationFactory().createPublication(this.runtime, subscriptions, message);
  }
  
  protected Collection<Subscription> getSubscriptionsByMessageType(Class messageType) {
    return this.subscriptionManager.getSubscriptionsByMessageType(messageType);
  }
  
  protected void handlePublicationError(PublicationError error) {
    for (IPublicationErrorHandler errorHandler : this.errorHandlers) {
      try {
        errorHandler.handleError(error);
      } catch (Throwable ex) {
        ex.printStackTrace();
      } 
    } 
  }
  
  public String toString() {
    return getClass().getSimpleName() + "(" + this.runtime.<String>get("bus.id") + ")";
  }
}
