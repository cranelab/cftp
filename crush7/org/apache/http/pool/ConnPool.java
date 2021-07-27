package org.apache.http.pool;

import java.util.concurrent.Future;
import org.apache.http.concurrent.FutureCallback;

public interface ConnPool<T, E> {
  Future<E> lease(T paramT, Object paramObject, FutureCallback<E> paramFutureCallback);
  
  void release(E paramE, boolean paramBoolean);
}
