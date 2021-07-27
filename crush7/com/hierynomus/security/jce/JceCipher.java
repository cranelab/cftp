package com.hierynomus.security.jce;

import com.hierynomus.security.Cipher;
import com.hierynomus.security.SecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

public class JceCipher implements Cipher {
  private Cipher cipher;
  
  JceCipher(String name, Provider jceProvider, String providerName) throws SecurityException {
    try {
      if (jceProvider != null) {
        this.cipher = Cipher.getInstance(name, jceProvider);
      } else if (providerName != null) {
        this.cipher = Cipher.getInstance(name, providerName);
      } else {
        this.cipher = Cipher.getInstance(name);
      } 
    } catch (NoSuchAlgorithmException e) {
      throw new SecurityException(e);
    } catch (NoSuchPaddingException e) {
      throw new SecurityException(e);
    } catch (NoSuchProviderException e) {
      throw new SecurityException(e);
    } 
  }
  
  public void init(Cipher.CryptMode cryptMode, byte[] bytes) throws SecurityException {
    try {
      if (Cipher.CryptMode.DECRYPT == cryptMode) {
        this.cipher.init(2, new SecretKeySpec(bytes, this.cipher.getAlgorithm().split("/")[0]));
      } else {
        this.cipher.init(1, new SecretKeySpec(bytes, this.cipher.getAlgorithm().split("/")[0]));
      } 
    } catch (InvalidKeyException e) {
      throw new SecurityException(e);
    } 
  }
  
  public int update(byte[] in, int inOff, int bytes, byte[] out, int outOff) throws SecurityException {
    try {
      return this.cipher.update(in, inOff, bytes, out, outOff);
    } catch (ShortBufferException e) {
      throw new SecurityException(e);
    } 
  }
  
  public int doFinal(byte[] out, int outOff) throws SecurityException {
    try {
      return this.cipher.doFinal(out, outOff);
    } catch (BadPaddingException e) {
      throw new SecurityException(e);
    } catch (IllegalBlockSizeException e) {
      throw new SecurityException(e);
    } catch (ShortBufferException e) {
      throw new SecurityException(e);
    } 
  }
  
  public void reset() {}
}
