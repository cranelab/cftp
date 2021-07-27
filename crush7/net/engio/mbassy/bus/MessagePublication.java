package net.engio.mbassy.bus;

import java.util.Collection;
import net.engio.mbassy.bus.common.DeadMessage;
import net.engio.mbassy.bus.common.FilteredMessage;
import net.engio.mbassy.bus.error.PublicationError;
import net.engio.mbassy.subscription.Subscription;

public class MessagePublication implements IMessagePublication {
  private final Collection<Subscription> subscriptions;
  
  private final Object message;
  
  private volatile State state = State.Initial;
  
  private volatile boolean dispatched = false;
  
  private final BusRuntime runtime;
  
  private PublicationError error = null;
  
  protected MessagePublication(BusRuntime runtime, Collection<Subscription> subscriptions, Object message, State initialState) {
    this.runtime = runtime;
    this.subscriptions = subscriptions;
    this.message = message;
    this.state = initialState;
  }
  
  public boolean add(Subscription subscription) {
    return this.subscriptions.add(subscription);
  }
  
  public void execute() {
    this.state = State.Running;
    for (Subscription sub : this.subscriptions)
      sub.publish(this, this.message); 
    this.state = State.Finished;
    if (!this.dispatched)
      if (!isFilteredMessage() && !isDeadMessage()) {
        this.runtime.getProvider().publish(new FilteredMessage(this.message));
      } else if (!isDeadMessage()) {
        this.runtime.getProvider().publish(new DeadMessage(this.message));
      }  
  }
  
  public boolean isFinished() {
    return this.state.equals(State.Finished);
  }
  
  public boolean isRunning() {
    return this.state.equals(State.Running);
  }
  
  public boolean isScheduled() {
    return this.state.equals(State.Scheduled);
  }
  
  public boolean hasError() {
    return (this.error != null);
  }
  
  public PublicationError getError() {
    return this.error;
  }
  
  public void markDispatched() {
    this.dispatched = true;
  }
  
  public void markError(PublicationError error) {
    this.error = error;
  }
  
  public MessagePublication markScheduled() {
    if (this.state.equals(State.Initial))
      this.state = State.Scheduled; 
    return this;
  }
  
  public boolean isDeadMessage() {
    return DeadMessage.class.equals(this.message.getClass());
  }
  
  public boolean isFilteredMessage() {
    return FilteredMessage.class.equals(this.message.getClass());
  }
  
  public Object getMessage() {
    return this.message;
  }
  
  private enum State {
    Initial, Scheduled, Running, Finished;
  }
  
  public static class Factory {
    public MessagePublication createPublication(BusRuntime runtime, Collection<Subscription> subscriptions, Object message) {
      return new MessagePublication(runtime, subscriptions, message, MessagePublication.State.Initial);
    }
  }
}
