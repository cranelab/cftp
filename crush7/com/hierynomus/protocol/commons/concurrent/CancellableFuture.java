package com.hierynomus.protocol.commons.concurrent;

import com.hierynomus.smbj.common.SMBRuntimeException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CancellableFuture<V> extends AFuture<V> {
  private final AFuture<V> wrappedFuture;
  
  private final CancelCallback callback;
  
  private final AtomicBoolean cancelled = new AtomicBoolean(false);
  
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  
  public CancellableFuture(AFuture<V> wrappedFuture, CancelCallback cc) {
    this.wrappedFuture = wrappedFuture;
    this.callback = cc;
  }
  
  public boolean cancel(boolean mayInterruptIfRunning) {
    this.lock.writeLock().lock();
    try {
      if (isDone() || this.cancelled.getAndSet(true))
        return false; 
      this.callback.cancel();
      return true;
    } catch (Throwable t) {
      this.cancelled.set(false);
      throw (SMBRuntimeException)SMBRuntimeException.Wrapper.wrap(t);
    } finally {
      this.lock.writeLock().unlock();
    } 
  }
  
  public boolean isCancelled() {
    this.lock.readLock().lock();
    try {
      return this.cancelled.get();
    } finally {
      this.lock.readLock().unlock();
    } 
  }
  
  public boolean isDone() {
    this.lock.readLock().lock();
    try {
      return !(!this.cancelled.get() && !this.wrappedFuture.isDone());
    } finally {
      this.lock.readLock().unlock();
    } 
  }
  
  public V get() throws InterruptedException, ExecutionException {
    return this.wrappedFuture.get();
  }
  
  public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return this.wrappedFuture.get(timeout, unit);
  }
  
  public static interface CancelCallback {
    void cancel();
  }
}
