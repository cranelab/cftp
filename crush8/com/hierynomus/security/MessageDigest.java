package com.hierynomus.security;

public interface MessageDigest {
  void update(byte[] paramArrayOfbyte);
  
  byte[] digest();
  
  void reset();
}
