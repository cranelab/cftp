package net.engio.mbassy.listener;

import net.engio.mbassy.subscription.SubscriptionContext;

public interface IMessageFilter<M> {
  boolean accepts(M paramM, SubscriptionContext paramSubscriptionContext);
}
