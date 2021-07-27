package org.boris.winrun4j;

public class RegistryException extends Exception {
  public RegistryException() {}
  
  public RegistryException(String message, Throwable cause) {
    super(message, cause);
  }
  
  public RegistryException(String message) {
    super(message);
  }
  
  public RegistryException(Throwable cause) {
    super(cause);
  }
}
