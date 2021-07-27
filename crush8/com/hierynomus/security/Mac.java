package com.hierynomus.security;

public interface Mac {
  void init(byte[] paramArrayOfbyte) throws SecurityException;
  
  void update(byte paramByte);
  
  void update(byte[] paramArrayOfbyte);
  
  void update(byte[] paramArrayOfbyte, int paramInt1, int paramInt2);
  
  byte[] doFinal();
  
  void reset();
}
