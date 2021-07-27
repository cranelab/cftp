package net.engio.mbassy.dispatch;

import net.engio.mbassy.bus.MessagePublication;
import net.engio.mbassy.subscription.ISubscriptionContextAware;

public interface IMessageDispatcher extends ISubscriptionContextAware {
  void dispatch(MessagePublication paramMessagePublication, Object paramObject, Iterable paramIterable);
  
  IHandlerInvocation getInvocation();
}
