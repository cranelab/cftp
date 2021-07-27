package net.engio.mbassy.bus.error;

public interface IPublicationErrorHandler {
  void handleError(PublicationError paramPublicationError);
  
  public static final class ConsoleLogger implements IPublicationErrorHandler {
    private final boolean printStackTrace;
    
    public ConsoleLogger() {
      this(false);
    }
    
    public ConsoleLogger(boolean printStackTrace) {
      this.printStackTrace = printStackTrace;
    }
    
    public void handleError(PublicationError error) {
      System.out.println(error);
      if (this.printStackTrace && error.getCause() != null)
        error.getCause().printStackTrace(); 
    }
  }
}
