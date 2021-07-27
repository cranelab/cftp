package com.crushftp.client;

import java.io.IOException;
import java.util.Vector;

public class Worker2 implements Runnable {
  Runnable q = null;
  
  String threadName = null;
  
  public static Vector availableWorkers = Worker.availableWorkers;
  
  public static Vector busyWorkers = Worker.busyWorkers;
  
  public static Object dumpLock = new Object();
  
  public void run() {
    while (true) {
      try {
        if (this.threadName != null)
          Thread.currentThread().setName("W:" + this.threadName); 
        this.q.run();
      } catch (Exception e) {
        Common.log("SERVER", 1, e);
      } 
      this.threadName = null;
      Thread.currentThread().setName("W:Idle");
      while (busyWorkers.contains(this))
        busyWorkers.remove(this); 
      this.q = null;
      synchronized (this) {
        availableWorkers.addElement(this);
        try {
          wait();
        } catch (InterruptedException interruptedException) {}
      } 
    } 
  }
  
  public static boolean startWorker(Runnable q) throws IOException {
    return startWorker(q, null);
  }
  
  public static boolean startWorker(Runnable q, String threadName) throws IOException {
    Worker2 ww = null;
    for (int loops = 0; loops < 60; loops++) {
      synchronized (availableWorkers) {
        if (availableWorkers.size() > 0)
          ww = availableWorkers.remove(0); 
      } 
      if (ww != null) {
        ww.q = q;
        ww.threadName = threadName;
        busyWorkers.addElement(ww);
        if (busyWorkers.size() > Worker.max_busy_workers)
          Worker.max_busy_workers = busyWorkers.size(); 
        synchronized (ww) {
          ww.notify();
        } 
        return true;
      } 
      if (busyWorkers.size() + availableWorkers.size() < Integer.parseInt(System.getProperty("crushftp.max_threads", "800")) - 1) {
        ww = new Worker2();
        ww.q = q;
        ww.threadName = threadName;
        busyWorkers.addElement(ww);
        (new Thread(ww)).start();
        return true;
      } 
      try {
        Thread.sleep(1000L);
      } catch (InterruptedException interruptedException) {}
      if (loops > 59) {
        Common.log("SERVER", 0, "No threads left!  Busy:" + busyWorkers.size() + " Available:" + availableWorkers.size() + " Max:" + Integer.parseInt(System.getProperty("crushftp.max_threads", "800")));
        if (System.currentTimeMillis() - Worker.lastDump > 60000L)
          synchronized (dumpLock) {
            if (System.currentTimeMillis() - Worker.lastDump > 60000L) {
              Worker.lastDump = System.currentTimeMillis();
              Common.log("SERVER", 0, Common.dumpStack("No more workers:" + System.getProperty("crushftp.max_threads", "800")));
              Worker.lastDump = System.currentTimeMillis();
            } 
          }  
        throw new IOException("No threads available in pool.");
      } 
    } 
    return false;
  }
}
