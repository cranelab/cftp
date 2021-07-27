package com.hierynomus.security;

public interface SecurityProvider {
  MessageDigest getDigest(String paramString) throws SecurityException;
  
  Mac getMac(String paramString) throws SecurityException;
  
  Cipher getCipher(String paramString) throws SecurityException;
}
