package net.engio.mbassy.dispatch;

import net.engio.mbassy.bus.MessagePublication;
import net.engio.mbassy.subscription.ISubscriptionContextAware;

public interface IHandlerInvocation<HANDLER, MESSAGE> extends ISubscriptionContextAware {
  void invoke(HANDLER paramHANDLER, MESSAGE paramMESSAGE, MessagePublication paramMessagePublication);
}
