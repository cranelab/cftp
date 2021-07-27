package net.engio.mbassy.dispatch;

import net.engio.mbassy.bus.MessagePublication;
import net.engio.mbassy.listener.IMessageFilter;

public final class FilteredMessageDispatcher extends DelegatingMessageDispatcher {
  private final IMessageFilter[] filter;
  
  public FilteredMessageDispatcher(IMessageDispatcher dispatcher) {
    super(dispatcher);
    this.filter = dispatcher.getContext().getHandler().getFilter();
  }
  
  private boolean passesFilter(Object message) {
    if (this.filter == null)
      return true; 
    for (IMessageFilter<Object> aFilter : this.filter) {
      if (!aFilter.accepts(message, getContext()))
        return false; 
    } 
    return true;
  }
  
  public void dispatch(MessagePublication publication, Object message, Iterable listeners) {
    if (passesFilter(message))
      getDelegate().dispatch(publication, message, listeners); 
  }
}
