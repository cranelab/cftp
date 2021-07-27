package com.hierynomus.protocol.commons.backport;

public class JavaVersion {
  public static boolean isJava7OrEarlier() {
    String property = System.getProperty("java.specification.version");
    float diff = Float.parseFloat(property) - 1.7F;
    return (diff < 0.01D);
  }
}
