package com.hierynomus.protocol.commons.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TransformedFuture<T, V> extends AFuture<V> {
  private AFuture<T> wrapped;
  
  private AFuture.Function<T, V> function;
  
  public TransformedFuture(AFuture<T> wrapped, AFuture.Function<T, V> function) {
    this.wrapped = wrapped;
    this.function = function;
  }
  
  public boolean cancel(boolean mayInterruptIfRunning) {
    return this.wrapped.cancel(mayInterruptIfRunning);
  }
  
  public boolean isCancelled() {
    return this.wrapped.isCancelled();
  }
  
  public boolean isDone() {
    return this.wrapped.isDone();
  }
  
  public V get() throws InterruptedException, ExecutionException {
    return this.function.apply(this.wrapped.get());
  }
  
  public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return this.function.apply(this.wrapped.get(timeout, unit));
  }
}
