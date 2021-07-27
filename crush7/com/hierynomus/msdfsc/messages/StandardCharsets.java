package com.hierynomus.msdfsc.messages;

import java.nio.charset.Charset;

public class StandardCharsets {
  public static final Charset UTF_8 = Charset.forName("UTF-8");
  
  public static final Charset UTF_16 = Charset.forName("UTF-16");
  
  public static final Charset UTF_16LE = Charset.forName("UTF-16LE");
  
  public static final Charset UTF_16BE = Charset.forName("UTF-16BE");
  
  private StandardCharsets() {
    throw new UnsupportedOperationException();
  }
}
