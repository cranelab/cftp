package com.hierynomus.security.bc;

import com.hierynomus.protocol.commons.Factory;
import com.hierynomus.security.Cipher;
import com.hierynomus.security.SecurityException;
import java.util.HashMap;
import java.util.Map;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.engines.DESEngine;
import org.bouncycastle.crypto.engines.RC4Engine;
import org.bouncycastle.crypto.params.DESedeParameters;
import org.bouncycastle.crypto.params.KeyParameter;

public class BCCipherFactory {
  private static final Map<String, Factory<Cipher>> lookup = new HashMap<>();
  
  static {
    lookup.put("DES/ECB/NoPadding", new Factory<Cipher>() {
          public Cipher create() {
            return new BCCipherFactory.BCBlockCipher(new BufferedBlockCipher((BlockCipher)new DESEngine())) {
                protected CipherParameters createParams(byte[] key) {
                  return (CipherParameters)new DESedeParameters(key);
                }
              };
          }
        });
    lookup.put("RC4", new Factory<Cipher>() {
          public Cipher create() {
            return new BCCipherFactory.BCStreamCipher((StreamCipher)new RC4Engine()) {
                protected CipherParameters createParams(byte[] key) {
                  return (CipherParameters)new KeyParameter(key);
                }
              };
          }
        });
  }
  
  public static Cipher create(String name) {
    Factory<Cipher> cipherFactory = lookup.get(name);
    if (cipherFactory == null)
      throw new IllegalArgumentException("Unknown Cipher " + name); 
    return cipherFactory.create();
  }
  
  private static abstract class BCBlockCipher implements Cipher {
    private BufferedBlockCipher wrappedCipher;
    
    BCBlockCipher(BufferedBlockCipher bufferedBlockCipher) {
      this.wrappedCipher = bufferedBlockCipher;
    }
    
    public void init(Cipher.CryptMode cryptMode, byte[] bytes) {
      this.wrappedCipher.init((cryptMode == Cipher.CryptMode.ENCRYPT), createParams(bytes));
    }
    
    public int update(byte[] in, int inOff, int bytes, byte[] out, int outOff) {
      return this.wrappedCipher.processBytes(in, inOff, bytes, out, outOff);
    }
    
    public int doFinal(byte[] out, int outOff) throws SecurityException {
      try {
        return this.wrappedCipher.doFinal(out, outOff);
      } catch (InvalidCipherTextException e) {
        throw new SecurityException(e);
      } 
    }
    
    public void reset() {
      this.wrappedCipher.reset();
    }
    
    protected abstract CipherParameters createParams(byte[] param1ArrayOfbyte);
  }
  
  private static abstract class BCStreamCipher implements Cipher {
    private StreamCipher streamCipher;
    
    BCStreamCipher(StreamCipher streamCipher) {
      this.streamCipher = streamCipher;
    }
    
    public void init(Cipher.CryptMode cryptMode, byte[] bytes) {
      this.streamCipher.init((cryptMode == Cipher.CryptMode.ENCRYPT), createParams(bytes));
    }
    
    protected abstract CipherParameters createParams(byte[] param1ArrayOfbyte);
    
    public int update(byte[] in, int inOff, int bytes, byte[] out, int outOff) {
      return this.streamCipher.processBytes(in, inOff, bytes, out, outOff);
    }
    
    public int doFinal(byte[] out, int outOff) {
      this.streamCipher.reset();
      return 0;
    }
    
    public void reset() {
      this.streamCipher.reset();
    }
  }
}
