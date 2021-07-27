package net.engio.mbassy.bus.error;

public class MessageBusException extends Exception {
  public MessageBusException(String message) {
    super(message);
  }
  
  public MessageBusException(String message, Throwable cause) {
    super(message, cause);
  }
  
  public MessageBusException(Throwable cause) {
    super(cause);
  }
}
