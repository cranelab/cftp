package javax.mail.event;

import java.util.EventListener;

public interface MessageCountListener extends EventListener {
  void messagesAdded(MessageCountEvent paramMessageCountEvent);
  
  void messagesRemoved(MessageCountEvent paramMessageCountEvent);
}
