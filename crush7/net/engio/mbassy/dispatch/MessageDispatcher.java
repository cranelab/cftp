package net.engio.mbassy.dispatch;

import net.engio.mbassy.bus.MessagePublication;
import net.engio.mbassy.subscription.AbstractSubscriptionContextAware;
import net.engio.mbassy.subscription.SubscriptionContext;

public class MessageDispatcher extends AbstractSubscriptionContextAware implements IMessageDispatcher {
  private final IHandlerInvocation invocation;
  
  public MessageDispatcher(SubscriptionContext context, IHandlerInvocation invocation) {
    super(context);
    this.invocation = invocation;
  }
  
  public void dispatch(MessagePublication publication, Object message, Iterable listeners) {
    publication.markDispatched();
    for (Object listener : listeners)
      getInvocation().invoke(listener, message, publication); 
  }
  
  public IHandlerInvocation getInvocation() {
    return this.invocation;
  }
}
