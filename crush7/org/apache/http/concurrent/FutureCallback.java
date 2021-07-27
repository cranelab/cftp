package org.apache.http.concurrent;

public interface FutureCallback<T> {
  void completed(T paramT);
  
  void failed(Exception paramException);
  
  void cancelled();
}
