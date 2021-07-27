package net.engio.mbassy.subscription;

public class MessageEnvelope {
  private Object message;
  
  public MessageEnvelope(Object message) {
    this.message = message;
  }
  
  public <T> T getMessage() {
    return (T)this.message;
  }
}
