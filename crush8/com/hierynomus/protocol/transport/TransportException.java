package com.hierynomus.protocol.transport;

import com.hierynomus.protocol.commons.concurrent.ExceptionWrapper;
import java.io.IOException;

public class TransportException extends IOException {
  public static final ExceptionWrapper<TransportException> Wrapper = new ExceptionWrapper<TransportException>() {
      public TransportException wrap(Throwable throwable) {
        if (throwable instanceof TransportException)
          return (TransportException)throwable; 
        return new TransportException(throwable);
      }
    };
  
  public TransportException(Throwable ioe) {
    super(ioe);
  }
  
  public TransportException(String s) {
    super(s);
  }
  
  public TransportException(String s, Throwable throwable) {
    super(s, throwable);
  }
}
