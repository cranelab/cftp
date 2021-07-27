package net.engio.mbassy.dispatch;

import net.engio.mbassy.bus.MessagePublication;
import net.engio.mbassy.bus.error.PublicationError;
import net.engio.mbassy.subscription.AbstractSubscriptionContextAware;
import net.engio.mbassy.subscription.SubscriptionContext;

public abstract class HandlerInvocation<HANDLER, MESSAGE> extends AbstractSubscriptionContextAware implements IHandlerInvocation<HANDLER, MESSAGE> {
  public HandlerInvocation(SubscriptionContext context) {
    super(context);
  }
  
  protected final void handlePublicationError(MessagePublication publication, PublicationError error) {
    publication.markError(error);
    getContext().handleError(error);
  }
}
