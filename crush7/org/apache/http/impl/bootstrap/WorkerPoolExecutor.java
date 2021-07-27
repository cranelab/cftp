package org.apache.http.impl.bootstrap;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class WorkerPoolExecutor extends ThreadPoolExecutor {
  private final Map<Worker, Boolean> workerSet;
  
  public WorkerPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    this.workerSet = new ConcurrentHashMap<Worker, Boolean>();
  }
  
  protected void beforeExecute(Thread t, Runnable r) {
    if (r instanceof Worker)
      this.workerSet.put((Worker)r, Boolean.TRUE); 
  }
  
  protected void afterExecute(Runnable r, Throwable t) {
    if (r instanceof Worker)
      this.workerSet.remove(r); 
  }
  
  public Set<Worker> getWorkers() {
    return new HashSet<Worker>(this.workerSet.keySet());
  }
}
