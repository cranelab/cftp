package com.hierynomus.protocol.commons.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Futures {
  public static <T, E extends Throwable> T get(Future<T> future, ExceptionWrapper<E> wrapper) throws E {
    try {
      return future.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw (E)wrapper.wrap(e);
    } catch (ExecutionException e) {
      throw (E)wrapper.wrap(e);
    } 
  }
  
  public static <T, E extends Throwable> T get(Future<T> future, long timeout, TimeUnit unit, ExceptionWrapper<E> wrapper) throws E {
    try {
      return future.get(timeout, unit);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw (E)wrapper.wrap(e);
    } catch (ExecutionException e) {
      throw (E)wrapper.wrap(e);
    } catch (TimeoutException e) {
      throw (E)wrapper.wrap(e);
    } 
  }
}
