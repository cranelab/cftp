package net.engio.mbassy.dispatch;

import net.engio.mbassy.subscription.AbstractSubscriptionContextAware;

public abstract class DelegatingMessageDispatcher extends AbstractSubscriptionContextAware implements IMessageDispatcher {
  private final IMessageDispatcher delegate;
  
  public DelegatingMessageDispatcher(IMessageDispatcher delegate) {
    super(delegate.getContext());
    this.delegate = delegate;
  }
  
  protected IMessageDispatcher getDelegate() {
    return this.delegate;
  }
  
  public IHandlerInvocation getInvocation() {
    return this.delegate.getInvocation();
  }
}
