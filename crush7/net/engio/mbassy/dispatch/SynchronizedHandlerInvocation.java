package net.engio.mbassy.dispatch;

import net.engio.mbassy.bus.MessagePublication;
import net.engio.mbassy.subscription.AbstractSubscriptionContextAware;

public class SynchronizedHandlerInvocation extends AbstractSubscriptionContextAware implements IHandlerInvocation<Object, Object> {
  private IHandlerInvocation delegate;
  
  public SynchronizedHandlerInvocation(IHandlerInvocation delegate) {
    super(delegate.getContext());
    this.delegate = delegate;
  }
  
  public void invoke(Object listener, Object message, MessagePublication publication) {
    synchronized (listener) {
      this.delegate.invoke(listener, message, publication);
    } 
  }
}
