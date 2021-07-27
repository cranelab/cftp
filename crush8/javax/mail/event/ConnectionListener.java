package javax.mail.event;

import java.util.EventListener;

public interface ConnectionListener extends EventListener {
  void opened(ConnectionEvent paramConnectionEvent);
  
  void disconnected(ConnectionEvent paramConnectionEvent);
  
  void closed(ConnectionEvent paramConnectionEvent);
}
