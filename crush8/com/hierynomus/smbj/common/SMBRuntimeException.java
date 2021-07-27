package com.hierynomus.smbj.common;

import com.hierynomus.protocol.commons.concurrent.ExceptionWrapper;

public class SMBRuntimeException extends RuntimeException {
  public static final ExceptionWrapper<SMBRuntimeException> Wrapper = new ExceptionWrapper<SMBRuntimeException>() {
      public SMBRuntimeException wrap(Throwable throwable) {
        if (throwable instanceof SMBRuntimeException)
          return (SMBRuntimeException)throwable; 
        return new SMBRuntimeException(throwable);
      }
    };
  
  public SMBRuntimeException(Throwable t) {
    super(t);
  }
  
  public SMBRuntimeException(String msg) {
    super(msg);
  }
  
  public SMBRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }
}
