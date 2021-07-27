package com.sun.mail.util;

import java.io.OutputStream;

public class BEncoderStream extends BASE64EncoderStream {
  public BEncoderStream(OutputStream out) {
    super(out, 2147483647);
  }
  
  public static int encodedLength(byte[] b) {
    return (b.length + 2) / 3 * 4;
  }
}
