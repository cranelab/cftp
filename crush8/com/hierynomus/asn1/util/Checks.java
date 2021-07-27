package com.hierynomus.asn1.util;

public class Checks {
  public static void checkState(boolean state, String messageFormat, Object... args) {
    if (!state)
      throw new IllegalStateException(String.format(messageFormat, args)); 
  }
  
  public static void checkArgument(boolean bool, String messageFormat, Object... args) {
    if (!bool)
      throw new IllegalArgumentException(String.format(messageFormat, args)); 
  }
}
