package crushftp.handlers;

import crushftp.server.ServerStatus;

public class ShutdownHandler extends Thread {
  boolean shutdown = false;
  
  public ShutdownHandler() {
    Runtime.getRuntime().addShutdownHook(this);
  }
  
  public synchronized void run() {
    if (!this.shutdown) {
      SharedSession.shutdown();
      ServerStatus.thisObj.statTools.stopDB();
      ServerStatus.thisObj.searchTools.stopDB();
    } 
    this.shutdown = true;
  }
}
