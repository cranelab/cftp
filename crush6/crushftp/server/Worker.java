package crushftp.server;

import com.crushftp.client.Common;
import crushftp.handlers.Log;
import java.io.IOException;

public class Worker implements Runnable {
  Runnable q = null;
  
  boolean active = false;
  
  String threadName = null;
  
  Thread thisThread = null;
  
  public static long lastDump = 0L;
  
  public void run() {
    this.thisThread = Thread.currentThread();
    try {
      while (this.q == null)
        Thread.sleep(100L); 
      while (!this.active)
        Thread.sleep(10L); 
      if (this.threadName != null)
        Thread.currentThread().setName("W:" + this.threadName); 
      this.q.run();
    } catch (InterruptedException interruptedException) {
    
    } catch (Exception e) {
      Log.log("SERVER", 1, e);
    } 
    this.threadName = null;
    Thread.currentThread().setName("W:Idle");
    while (ServerStatus.thisObj.busyWorkers.contains(this))
      ServerStatus.thisObj.busyWorkers.remove(this); 
    this.active = false;
    this.q = null;
    while (ServerStatus.thisObj.availableWorkers.contains(this))
      ServerStatus.thisObj.availableWorkers.remove(this); 
    ServerStatus.thisObj.availableWorkers.addElement(this);
    while (this.q != null)
      Thread.sleep(100L); 
    while (this.active)
      Thread.sleep(10L); 
    if (this.threadName != null)
      Thread.currentThread().setName("W:" + this.threadName); 
    this.q.run();
  }
  
  synchronized boolean setWorker(Runnable q) {
    if (this.q != null)
      return false; 
    this.active = false;
    this.q = q;
    return true;
  }
  
  public boolean isBusy() {
    return (this.q != null);
  }
  
  public static boolean startWorker(Runnable q) throws IOException {
    return startWorker(q, null);
  }
  
  public static boolean startWorker(Runnable q, String threadName) throws IOException {
    synchronized (ServerStatus.thisObj.availableWorkers) {
      for (int x = 0; x < ServerStatus.thisObj.availableWorkers.size(); x++) {
        Worker worker = ServerStatus.thisObj.availableWorkers.elementAt(x);
        if (worker.setWorker(q)) {
          worker.threadName = threadName;
          while (ServerStatus.thisObj.availableWorkers.contains(worker))
            ServerStatus.thisObj.availableWorkers.remove(worker); 
          while (ServerStatus.thisObj.busyWorkers.contains(worker))
            ServerStatus.thisObj.busyWorkers.remove(worker); 
          ServerStatus.thisObj.busyWorkers.addElement(worker);
          worker.active = true;
          return true;
        } 
      } 
    } 
    if (ServerStatus.thisObj.busyWorkers.size() + ServerStatus.thisObj.availableWorkers.size() >= ServerStatus.IG("max_threads")) {
      Log.log("SERVER", 0, "No threads left!  Busy:" + ServerStatus.thisObj.busyWorkers.size() + " Available:" + ServerStatus.thisObj.availableWorkers.size() + " Max:" + ServerStatus.IG("max_threads"));
      if (System.currentTimeMillis() - lastDump > 60000L) {
        Log.log("SERVER", 0, Common.dumpStack(""));
        lastDump = System.currentTimeMillis();
      } 
      throw new IOException("No threads available in pool.");
    } 
    Worker ww = new Worker();
    ww.setWorker(q);
    ww.threadName = threadName;
    ServerStatus.thisObj.busyWorkers.addElement(ww);
    ww.thisThread = new Thread(ww);
    ww.thisThread.start();
    ww.active = true;
    return true;
  }
}
