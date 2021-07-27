package com.hierynomus.smbj.common;

import java.util.Arrays;

public class Check {
  public static void ensureEquals(byte[] real, byte[] expected, String errorMessage) {
    if (!Arrays.equals(real, expected))
      throw new IllegalArgumentException(errorMessage); 
  }
}
