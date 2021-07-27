package crushftp.handlers;

import com.crushftp.client.Base64;
import com.crushftp.client.MD4;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class DesEncrypter {
  Cipher ecipher;
  
  Cipher dcipher;
  
  public DesEncrypter(String key, boolean base64) {
    try {
      key = getHash(key, base64, "SHA");
      doInit(key);
    } catch (Exception exception) {}
  }
  
  public DesEncrypter(String key) {
    try {
      key = getHash(key, false, "SHA");
      doInit(key);
    } catch (Exception exception) {}
  }
  
  public void doInit(String key) throws Exception {
    while (key.length() / 8.0F != (key.length() / 8))
      key = String.valueOf(key) + "Z"; 
    DESKeySpec desKeySpec = new DESKeySpec(key.getBytes());
    SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
    SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
    this.ecipher = Cipher.getInstance("DES");
    this.dcipher = Cipher.getInstance("DES");
    this.ecipher.init(1, secretKey);
    this.dcipher.init(2, secretKey);
  }
  
  public String getHash(String key, boolean base64, String method) throws Exception {
    if (method.equals("MD4")) {
      MD4 mD4 = new MD4();
      byte[] b = key.getBytes("UnicodeLittleUnmarked");
      mD4.update(b, 0, b.length);
      if (base64)
        return Base64.encodeBytes(mD4.digest()); 
      return new String(mD4.digest());
    } 
    MessageDigest md = MessageDigest.getInstance(method);
    md.update(key.getBytes());
    if (method.equals("MD5")) {
      String hash = (new BigInteger(1, md.digest())).toString(16).toLowerCase();
      while (hash.length() < 32)
        hash = "0" + hash; 
      return hash;
    } 
    if (method.equals("SHA512")) {
      String hash = (new BigInteger(1, md.digest())).toString(16).toLowerCase();
      while (hash.length() < 128)
        hash = "0" + hash; 
      return hash;
    } 
    if (base64)
      return Base64.encodeBytes(md.digest()); 
    return new String(md.digest(), "UTF8");
  }
  
  public String encrypt(String str, String method) {
    return encrypt(str, method, false);
  }
  
  public String encrypt(String str, String method, boolean base64) {
    if (method.equals("SHA") || str.startsWith("SHA:") || method.equals("SHA512") || str.startsWith("SHA512:") || method.equals("MD5") || str.startsWith("MD5:") || method.equals("MD4") || str.startsWith("MD4:"))
      try {
        if (str.startsWith("SHA:") || str.startsWith("SHA512:") || str.startsWith("MD5:") || str.startsWith("MD4:") || str.startsWith("CRYPT3:") || str.startsWith("BCRYPT:") || str.startsWith("MD5CRYPT:"))
          return str; 
        if (base64)
          return String.valueOf(method) + ":" + getHash(str, base64, method).trim(); 
        return String.valueOf(method) + ":" + Base64.encodeBytes(getHash(str, base64, method).getBytes()).trim();
      } catch (Exception exception) {
        return null;
      }  
    if (method.equals("CRYPT3") || str.startsWith("CRYPT3:"))
      try {
        if (str.startsWith("CRYPT3:"))
          return str; 
        if (base64)
          return "SHA:" + getHash(str, base64, "SHA").trim(); 
        return "SHA:" + Base64.encodeBytes(getHash(str, base64, "SHA").getBytes()).trim();
      } catch (Exception exception) {
        return null;
      }  
    if (method.equals("BCRYPT") || str.startsWith("BCRYPT:")) {
      try {
        if (str.startsWith("BCRYPT:"))
          return str; 
      } catch (Exception exception) {}
      return null;
    } 
    if (method.equals("MD5CRYPT") || str.startsWith("MD5CRYPT:")) {
      try {
        if (str.startsWith("MD5CRYPT:"))
          return str; 
      } catch (Exception exception) {}
      return null;
    } 
    try {
      byte[] utf8 = str.getBytes("UTF8");
      byte[] enc = this.ecipher.doFinal(utf8);
      return Base64.encodeBytes(enc);
    } catch (BadPaddingException badPaddingException) {
    
    } catch (IllegalBlockSizeException illegalBlockSizeException) {
    
    } catch (IOException iOException) {}
    return null;
  }
  
  public String decrypt(String str) {
    if (str.startsWith("SHA:"))
      return str; 
    if (str.startsWith("SHA512:"))
      return str; 
    if (str.startsWith("MD5:"))
      return str; 
    if (str.startsWith("MD4:"))
      return str; 
    if (str.startsWith("CRYPT3:"))
      return str; 
    if (str.startsWith("BCRYPT:"))
      return str; 
    if (str.startsWith("MD5CRYPT:"))
      return str; 
    try {
      byte[] dec = Base64.decode(str);
      byte[] utf8 = this.dcipher.doFinal(dec);
      return new String(utf8, "UTF8");
    } catch (BadPaddingException badPaddingException) {
    
    } catch (IllegalBlockSizeException illegalBlockSizeException) {
    
    } catch (IOException iOException) {}
    return null;
  }
  
  public static byte[] blockEncrypt(byte[] b, byte[] ch) throws Exception {
    Cipher encrypt_des = Cipher.getInstance("DES");
    byte[] encrypted_bytes = new byte[24];
    encrypt_des.init(1, new SecretKeySpec(keyMaker(b, 0), 0, 8, "DES"));
    System.arraycopy(encrypt_des.doFinal(ch), 0, encrypted_bytes, 0, 8);
    encrypt_des.init(1, new SecretKeySpec(keyMaker(b, 7), 0, 8, "DES"));
    System.arraycopy(encrypt_des.doFinal(ch), 0, encrypted_bytes, 8, 8);
    encrypt_des.init(1, new SecretKeySpec(keyMaker(b, 14), 0, 8, "DES"));
    System.arraycopy(encrypt_des.doFinal(ch), 0, encrypted_bytes, 16, 8);
    return encrypted_bytes;
  }
  
  public static byte[] keyMaker(byte[] b, int loc) {
    byte[] key = new byte[8];
    key[0] = (byte)(b[loc + 0] >> 1);
    key[1] = (byte)((b[loc + 0] & 0x1) << 6 | (b[loc + 1] & 0xFF) >> 2);
    key[2] = (byte)((b[loc + 1] & 0x3) << 5 | (b[loc + 2] & 0xFF) >> 3);
    key[3] = (byte)((b[loc + 2] & 0x7) << 4 | (b[loc + 3] & 0xFF) >> 4);
    key[4] = (byte)((b[loc + 3] & 0xF) << 3 | (b[loc + 4] & 0xFF) >> 5);
    key[5] = (byte)((b[loc + 4] & 0x1F) << 2 | (b[loc + 5] & 0xFF) >> 6);
    key[6] = (byte)((b[loc + 5] & 0x3F) << 1 | (b[loc + 6] & 0xFF) >> 7);
    key[7] = (byte)(b[loc + 6] & Byte.MAX_VALUE);
    for (int i = 0; i < 8; i++)
      key[i] = (byte)(key[i] << 1); 
    return key;
  }
}
