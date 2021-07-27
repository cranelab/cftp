package com.crushftp.client;

import java.io.IOException;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Worker2 {
  static ExecutorService executor = Executors.newCachedThreadPool();
  
  public static Vector availableWorkers = Worker.availableWorkers;
  
  public static Vector busyWorkers = Worker.busyWorkers;
  
  public static boolean startWorker(Runnable q) throws IOException {
    return startWorker(q, null);
  }
  
  public static boolean startWorker(Runnable q, String threadName) throws IOException {
    if (busyWorkers.size() > Integer.parseInt(System.getProperty("crushftp.max_threads", "800"))) {
      Common.log("SERVER", 0, "No threads left!  Busy:" + busyWorkers.size() + " Available:" + availableWorkers.size() + " Max:" + Integer.parseInt(System.getProperty("crushftp.max_threads", "800")));
      Worker.lastDump = System.currentTimeMillis();
      Common.log("SERVER", 0, Common.dumpStack("No more workers:" + System.getProperty("crushftp.max_threads", "800")));
      Worker.lastDump = System.currentTimeMillis();
      return false;
    } 
    executor.execute(new Runnable(threadName, q) {
          private final String val$threadName;
          
          private final Runnable val$q;
          
          public void run() {
            try {
              synchronized (Worker2.busyWorkers) {
                if (Worker2.availableWorkers.size() > 0) {
                  Worker2.busyWorkers.addElement(Worker2.availableWorkers.remove(0));
                } else {
                  Worker2.busyWorkers.addElement("");
                } 
              } 
              if (this.val$threadName != null) {
                Thread.currentThread().setName(this.val$threadName);
              } else {
                Thread.currentThread().setName("Worker:active...unamed thread");
              } 
              this.val$q.run();
            } catch (Throwable t) {
              t.printStackTrace();
              Common.log("SERVER", 1, "WORKER_FAILED:" + t);
              Common.log("SERVER", 1, t);
            } finally {
              Thread.currentThread().setName("Worker:Idle");
              synchronized (Worker2.busyWorkers) {
                if (Worker2.busyWorkers.size() > 0)
                  Worker2.availableWorkers.addElement(Worker2.busyWorkers.remove(0)); 
              } 
            } 
          }
        });
    return true;
  }
}
