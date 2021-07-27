package net.engio.mbassy.dispatch;

import net.engio.mbassy.bus.MessagePublication;
import net.engio.mbassy.subscription.MessageEnvelope;

public class EnvelopedMessageDispatcher extends DelegatingMessageDispatcher {
  public EnvelopedMessageDispatcher(IMessageDispatcher dispatcher) {
    super(dispatcher);
  }
  
  public void dispatch(MessagePublication publication, Object message, Iterable listeners) {
    getDelegate().dispatch(publication, new MessageEnvelope(message), listeners);
  }
}
