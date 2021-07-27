package com.crushftp.client;

import java.io.IOException;
import java.util.Vector;

public class Worker implements Runnable {
  Runnable q = null;
  
  boolean active = false;
  
  String threadName = null;
  
  Thread thisThread = null;
  
  public static long lastDump = 0L;
  
  public static Vector availableWorkers = new Vector();
  
  public static Vector busyWorkers = new Vector();
  
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
      Common.log("SERVER", 1, e);
    } 
    this.threadName = null;
    Thread.currentThread().setName("W:Idle");
    while (busyWorkers.contains(this))
      busyWorkers.remove(this); 
    this.active = false;
    this.q = null;
    while (availableWorkers.contains(this))
      availableWorkers.remove(this); 
    availableWorkers.addElement(this);
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
    synchronized (availableWorkers) {
      for (int x = 0; x < availableWorkers.size(); x++) {
        Worker worker = availableWorkers.elementAt(x);
        if (worker.setWorker(q)) {
          worker.threadName = threadName;
          while (availableWorkers.contains(worker))
            availableWorkers.remove(worker); 
          while (busyWorkers.contains(worker))
            busyWorkers.remove(worker); 
          busyWorkers.addElement(worker);
          worker.active = true;
          return true;
        } 
      } 
    } 
    if (busyWorkers.size() + availableWorkers.size() >= Integer.parseInt(System.getProperty("crushftp.max_threads", "800"))) {
      Common.log("SERVER", 0, "No threads left!  Busy:" + busyWorkers.size() + " Available:" + availableWorkers.size() + " Max:" + Integer.parseInt(System.getProperty("crushftp.max_threads", "800")));
      if (System.currentTimeMillis() - lastDump > 60000L) {
        Common.log("SERVER", 0, Common.dumpStack("No more workers:" + System.getProperty("crushftp.max_threads", "800")));
        lastDump = System.currentTimeMillis();
      } 
      throw new IOException("No threads available in pool.");
    } 
    Worker ww = new Worker();
    ww.setWorker(q);
    ww.threadName = threadName;
    busyWorkers.addElement(ww);
    ww.thisThread = new Thread(ww);
    ww.thisThread.start();
    ww.active = true;
    return true;
  }
}
