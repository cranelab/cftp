package com.hierynomus.security;

public interface Cipher {
  void init(CryptMode paramCryptMode, byte[] paramArrayOfbyte) throws SecurityException;
  
  int update(byte[] paramArrayOfbyte1, int paramInt1, int paramInt2, byte[] paramArrayOfbyte2, int paramInt3) throws SecurityException;
  
  int doFinal(byte[] paramArrayOfbyte, int paramInt) throws SecurityException;
  
  void reset();
  
  public enum CryptMode {
    ENCRYPT, DECRYPT;
  }
}
