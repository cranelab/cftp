package com.hierynomus.smbj.common;

import com.hierynomus.protocol.commons.concurrent.ExceptionWrapper;
import java.io.IOException;

public class SMBException extends IOException {
  public static final ExceptionWrapper<SMBException> Wrapper = new ExceptionWrapper<SMBException>() {
      public SMBException wrap(Throwable throwable) {
        if (throwable instanceof SMBException)
          return (SMBException)throwable; 
        return new SMBException(throwable);
      }
    };
  
  public SMBException(String message) {
    super(message);
  }
  
  public SMBException(Throwable t) {
    super(t);
  }
}
