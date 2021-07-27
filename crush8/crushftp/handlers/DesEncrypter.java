package crushftp.handlers;

import com.crushftp.client.Base64;
import com.crushftp.client.File_S;
import com.crushftp.client.MD4;
import crushftp.server.ServerStatus;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Properties;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.crypto.digests.KeccakDigest;
import org.bouncycastle.jcajce.provider.digest.SHA3;
import org.bouncycastle.util.encoders.Hex;

public class DesEncrypter {
  Cipher ecipher;
  
  Cipher dcipher;
  
  private static Properties salt_hash = new Properties();
  
  public DesEncrypter(String key, boolean base64) {
    try {
      key = getHash(key, base64, "SHA", "", "");
      doInit(key);
    } catch (Exception exception) {}
  }
  
  public DesEncrypter(String key) {
    try {
      key = getHash(key, false, "SHA", "", "");
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
  
  public String getHash(String key, boolean base64, String method, String salt_data, String user_salt) throws Exception {
    byte[] salt_bytes = new byte[0];
    if (salt_data != null && !salt_data.equals("") && (new File_S(salt_data)).exists()) {
      salt_bytes = (byte[])salt_hash.get((new StringBuffer(String.valueOf((new File_S(salt_data)).lastModified()))).toString());
      if (salt_bytes == null) {
        RandomAccessFile raf = new RandomAccessFile(new File_S(salt_data), "r");
        salt_bytes = new byte[(int)raf.length()];
        raf.read(salt_bytes);
        raf.close();
        salt_hash.put((new StringBuffer(String.valueOf((new File_S(salt_data)).lastModified()))).toString(), salt_bytes);
      } 
    } 
    method = method.toUpperCase();
    if (method.equals("MD4")) {
      MD4 mD4 = new MD4();
      byte[] b = key.getBytes("UnicodeLittleUnmarked");
      mD4.update(b, 0, b.length);
      if (base64)
        return Base64.encodeBytes(mD4.digest()); 
      return new String(mD4.digest());
    } 
    MessageDigest md = null;
    SHA3.DigestSHA3 sha3 = null;
    KeccakDigest sha3_keccak = null;
    if (method.startsWith("SHA3")) {
      md = MessageDigest.getInstance("MD5");
      if (ServerStatus.BG("sha3_keccak") || method.equalsIgnoreCase("SHA3_KECCAK")) {
        sha3_keccak = new KeccakDigest(224);
      } else {
        sha3 = new SHA3.DigestSHA3(224);
      } 
    } else {
      md = MessageDigest.getInstance(method);
    } 
    if (user_salt.trim().length() > 0) {
      if (user_salt.startsWith("!")) {
        key = String.valueOf(user_salt.trim().substring(1)) + key;
      } else {
        key = String.valueOf(key) + user_salt.trim();
      } 
      byte[] b = key.getBytes(System.getProperty("crushftp.hash.encoding", "UTF8"));
      int cut_amount = Integer.parseInt(System.getProperty("crushftp.hash.cut", "0"));
      byte[] b2 = b;
      if (cut_amount > 0)
        b2 = Arrays.copyOfRange(b, cut_amount - 1, b.length + 1); 
      if (sha3 != null) {
        sha3.update(b2);
      } else if (sha3_keccak != null) {
        sha3_keccak.update(b2, 0, b2.length);
      } else {
        md.update(b2);
      } 
    } else {
      byte[] b = key.getBytes(System.getProperty("crushftp.hash.encoding", "UTF8"));
      if (sha3 != null) {
        sha3.update(b);
      } else if (sha3_keccak != null) {
        sha3_keccak.update(b, 0, b.length);
      } else {
        md.update(b);
      } 
    } 
    if (salt_bytes.length > 0)
      if (sha3 != null) {
        sha3.update(salt_bytes);
      } else if (sha3_keccak != null) {
        sha3_keccak.update(salt_bytes, 0, salt_bytes.length);
      } else {
        md.update(salt_bytes);
      }  
    if (method.equals("MD5")) {
      String hash = (new BigInteger(1, md.digest())).toString(16).toLowerCase();
      if (hash.length() < 32)
        hash = "0" + hash; 
      return hash;
    } 
    if (method.equals("SHA512")) {
      String hash = (new BigInteger(1, md.digest())).toString(16).toLowerCase();
      while (hash.length() < 128)
        hash = "0" + hash; 
      return hash;
    } 
    if (method.equals("SHA256")) {
      String hash = (new BigInteger(1, md.digest())).toString(16).toLowerCase();
      while (hash.length() < 64)
        hash = "0" + hash; 
      return hash;
    } 
    if (method.equals("SHA3") && sha3 != null)
      return Hex.toHexString(sha3.digest()).toLowerCase(); 
    if (method.startsWith("SHA3") && sha3_keccak != null) {
      byte[] b = new byte[28];
      sha3_keccak.doFinal(b, 0);
      return Hex.toHexString(b).toLowerCase();
    } 
    if (base64)
      return Base64.encodeBytes(md.digest()); 
    return new String(md.digest(), "UTF8");
  }
  
  public String encrypt(String str, String method, boolean base64, String salt) {
    if (method.equals("SHA") || str.startsWith("SHA:") || method.equals("SHA512") || method.equals("SHA256") || str.startsWith("SHA512:") || str.startsWith("SHA256:") || method.equals("SHA3") || str.startsWith("SHA3:") || method.equals("MD5") || str.startsWith("MD5:") || str.startsWith("MD5S2:") || method.equals("MD5S2") || method.equals("MD4") || str.startsWith("MD4:") || method.equals("SHA3_KECCAK") || str.startsWith("SHA3_KECCAK:"))
      try {
        if (str.startsWith("SHA:") || str.startsWith("SHA512:") || str.startsWith("SHA256:") || str.startsWith("SHA3:") || str.startsWith("MD5:") || str.startsWith("MD5S2:") || str.startsWith("MD4:") || str.startsWith("CRYPT3:") || str.startsWith("BCRYPT:") || str.startsWith("MD5CRYPT:") || str.startsWith("PBKDF2SHA256:") || str.startsWith("SHA512CRYPT:") || str.startsWith("SHA3_KECCAK:"))
          return str; 
        if (base64)
          return String.valueOf(method) + ":" + getHash(str, base64, method, ServerStatus.SG("password_salt_location"), salt).trim(); 
        return String.valueOf(method) + ":" + Base64.encodeBytes(getHash(str, base64, method, ServerStatus.SG("password_salt_location"), salt).getBytes()).trim();
      } catch (Exception exception) {
        return null;
      }  
    if (method.equals("CRYPT3") || str.startsWith("CRYPT3:"))
      try {
        if (str.startsWith("CRYPT3:"))
          return str; 
        if (base64)
          return "SHA:" + getHash(str, base64, "SHA", ServerStatus.SG("password_salt_location"), salt).trim(); 
        return "SHA:" + Base64.encodeBytes(getHash(str, base64, "SHA", ServerStatus.SG("password_salt_location"), salt).getBytes()).trim();
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
    if (method.equals("PBKDF2SHA256") || str.startsWith("PBKDF2SHA256:")) {
      try {
        if (str.startsWith("PBKDF2SHA256:"))
          return str; 
      } catch (Exception exception) {}
      return null;
    } 
    if (method.equals("SHA512CRYPT") || str.startsWith("SHA512CRYPT:")) {
      try {
        if (str.startsWith("SHA512CRYPT:"))
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
    if (str.startsWith("SHA256:"))
      return str; 
    if (str.startsWith("SHA3:"))
      return str; 
    if (str.startsWith("MD5:"))
      return str; 
    if (str.startsWith("MD5S2:"))
      return str; 
    if (str.startsWith("MD4:"))
      return str; 
    if (str.startsWith("CRYPT3:"))
      return str; 
    if (str.startsWith("BCRYPT:"))
      return str; 
    if (str.startsWith("MD5CRYPT:"))
      return str; 
    if (str.startsWith("PBKDF2SHA256:"))
      return str; 
    if (str.startsWith("SHA512CRYPT:"))
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
