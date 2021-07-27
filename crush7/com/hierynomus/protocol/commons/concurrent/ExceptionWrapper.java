package com.hierynomus.protocol.commons.concurrent;

public interface ExceptionWrapper<T extends Throwable> {
  T wrap(Throwable paramThrowable);
}
