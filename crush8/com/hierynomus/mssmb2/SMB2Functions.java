package com.hierynomus.mssmb2;

import com.hierynomus.protocol.commons.Charsets;

public class SMB2Functions {
  private static final byte[] EMPTY_BYTES = new byte[0];
  
  public static byte[] unicode(String s) {
    if (s == null)
      return EMPTY_BYTES; 
    return s.getBytes(Charsets.UTF_16LE);
  }
}
