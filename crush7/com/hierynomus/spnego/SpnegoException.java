package com.hierynomus.spnego;

import java.io.IOException;

public class SpnegoException extends Exception {
  public SpnegoException(String message) {
    super(message);
  }
  
  public SpnegoException(String message, IOException e) {
    super(message, e);
  }
}
