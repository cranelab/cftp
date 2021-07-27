package net.engio.mbassy.bus.error;

public class MissingPropertyException extends RuntimeException {
  public MissingPropertyException(String message) {
    super(message);
  }
}
