package javax.mail.event;

import java.util.EventListener;

public interface TransportListener extends EventListener {
  void messageDelivered(TransportEvent paramTransportEvent);
  
  void messageNotDelivered(TransportEvent paramTransportEvent);
  
  void messagePartiallyDelivered(TransportEvent paramTransportEvent);
}
