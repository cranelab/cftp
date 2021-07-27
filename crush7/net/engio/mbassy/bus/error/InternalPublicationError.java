package net.engio.mbassy.bus.error;

import net.engio.mbassy.bus.IMessagePublication;

public class InternalPublicationError extends PublicationError {
  public InternalPublicationError(Throwable cause, String message, IMessagePublication publication) {
    super(cause, message, publication);
  }
  
  public InternalPublicationError(Throwable cause, String message) {
    super(cause, message);
  }
}
