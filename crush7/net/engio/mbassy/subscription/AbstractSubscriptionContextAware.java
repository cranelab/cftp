package net.engio.mbassy.subscription;

public class AbstractSubscriptionContextAware implements ISubscriptionContextAware {
  private final SubscriptionContext context;
  
  public AbstractSubscriptionContextAware(SubscriptionContext context) {
    this.context = context;
  }
  
  public final SubscriptionContext getContext() {
    return this.context;
  }
}
