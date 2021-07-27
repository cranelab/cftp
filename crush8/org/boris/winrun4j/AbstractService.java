package org.boris.winrun4j;

public abstract class AbstractService implements Service {
  protected volatile boolean shutdown = false;
  
  public int serviceRequest(int control) throws ServiceException {
    switch (control) {
      case 1:
      case 5:
        this.shutdown = true;
        break;
    } 
    return 0;
  }
  
  public boolean isShutdown() {
    return this.shutdown;
  }
}
