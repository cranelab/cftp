package com.hierynomus.protocol.commons.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class PromiseBackedFuture<V> extends AFuture<V> {
  private Promise<V, ?> promise;
  
  public PromiseBackedFuture(Promise<V, ?> promise) {
    this.promise = promise;
  }
  
  public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }
  
  public boolean isCancelled() {
    return false;
  }
  
  public boolean isDone() {
    return this.promise.isDelivered();
  }
  
  public V get() throws ExecutionException {
    try {
      return this.promise.retrieve();
    } catch (Throwable t) {
      throw new ExecutionException(t);
    } 
  }
  
  public V get(long timeout, TimeUnit unit) throws ExecutionException {
    try {
      return this.promise.retrieve(timeout, unit);
    } catch (Throwable t) {
      throw new ExecutionException(t);
    } 
  }
}
