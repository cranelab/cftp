package crushftp.handlers;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public final class MD5Crypt {
  private static final String SALTCHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
  
  private static final String itoa64 = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
  
  public static void main(String[] argv) {
    if (argv.length < 1 || argv.length > 3) {
      System.err.println("Usage: MD5Crypt [-apache] password salt");
      System.exit(1);
    } 
    if (argv.length == 3) {
      System.err.println(apacheCrypt(argv[1], argv[2]));
    } else if (argv.length == 2) {
      System.err.println(crypt(argv[0], argv[1]));
    } else {
      System.err.println(crypt(argv[0]));
    } 
    System.exit(0);
  }
  
  private static final String to64(long v, int size) {
    StringBuffer result = new StringBuffer();
    while (--size >= 0) {
      result.append("./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".charAt((int)(v & 0x3FL)));
      v >>>= 6L;
    } 
    return result.toString();
  }
  
  private static final void clearbits(byte[] bits) {
    for (int i = 0; i < bits.length; i++)
      bits[i] = 0; 
  }
  
  private static final int bytes2u(byte inp) {
    return inp & 0xFF;
  }
  
  private static MessageDigest getMD5() {
    try {
      return MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException ex) {
      throw new RuntimeException(ex);
    } 
  }
  
  public static final String crypt(String password) {
    StringBuffer salt = new StringBuffer();
    Random randgen = new Random();
    while (salt.length() < 8) {
      int index = (int)(randgen.nextFloat() * "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".length());
      salt.append("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".substring(index, index + 1));
    } 
    return crypt(password, salt.toString());
  }
  
  public static final String crypt(String password, String salt) {
    return crypt(password, salt, "$1$");
  }
  
  public static final String apacheCrypt(String password) {
    StringBuffer salt = new StringBuffer();
    Random randgen = new Random();
    while (salt.length() < 8) {
      int index = (int)(randgen.nextFloat() * "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".length());
      salt.append("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".substring(index, index + 1));
    } 
    return apacheCrypt(password, salt.toString());
  }
  
  public static final String apacheCrypt(String password, String salt) {
    return crypt(password, salt, "$apr1$");
  }
  
  public static final String crypt(String password, String salt, String magic) {
    if (salt.startsWith(magic))
      salt = salt.substring(magic.length()); 
    if (salt.indexOf('$') != -1)
      salt = salt.substring(0, salt.indexOf('$')); 
    if (salt.length() > 8)
      salt = salt.substring(0, 8); 
    MessageDigest ctx = getMD5();
    ctx.update(password.getBytes());
    ctx.update(magic.getBytes());
    ctx.update(salt.getBytes());
    MessageDigest ctx1 = getMD5();
    ctx1.update(password.getBytes());
    ctx1.update(salt.getBytes());
    ctx1.update(password.getBytes());
    byte[] finalState = ctx1.digest();
    for (int pl = password.length(); pl > 0; pl -= 16)
      ctx.update(finalState, 0, (pl > 16) ? 16 : pl); 
    clearbits(finalState);
    int i;
    for (i = password.length(); i != 0; i >>>= 1) {
      if ((i & 0x1) != 0) {
        ctx.update(finalState, 0, 1);
      } else {
        ctx.update(password.getBytes(), 0, 1);
      } 
    } 
    finalState = ctx.digest();
    for (i = 0; i < 1000; i++) {
      ctx1.reset();
      if ((i & 0x1) != 0) {
        ctx1.update(password.getBytes());
      } else {
        ctx1.update(finalState, 0, 16);
      } 
      if (i % 3 != 0)
        ctx1.update(salt.getBytes()); 
      if (i % 7 != 0)
        ctx1.update(password.getBytes()); 
      if ((i & 0x1) != 0) {
        ctx1.update(finalState, 0, 16);
      } else {
        ctx1.update(password.getBytes());
      } 
      finalState = ctx1.digest();
    } 
    StringBuffer result = new StringBuffer();
    result.append(magic);
    result.append(salt);
    result.append("$");
    long l = (bytes2u(finalState[0]) << 16 | bytes2u(finalState[6]) << 8 | bytes2u(finalState[12]));
    result.append(to64(l, 4));
    l = (bytes2u(finalState[1]) << 16 | bytes2u(finalState[7]) << 8 | bytes2u(finalState[13]));
    result.append(to64(l, 4));
    l = (bytes2u(finalState[2]) << 16 | bytes2u(finalState[8]) << 8 | bytes2u(finalState[14]));
    result.append(to64(l, 4));
    l = (bytes2u(finalState[3]) << 16 | bytes2u(finalState[9]) << 8 | bytes2u(finalState[15]));
    result.append(to64(l, 4));
    l = (bytes2u(finalState[4]) << 16 | bytes2u(finalState[10]) << 8 | bytes2u(finalState[5]));
    result.append(to64(l, 4));
    l = bytes2u(finalState[11]);
    result.append(to64(l, 2));
    clearbits(finalState);
    return result.toString();
  }
  
  public static final boolean verifyPassword(String plaintextPass, String md5CryptText) {
    if (md5CryptText.startsWith("$1$"))
      return md5CryptText.equals(crypt(plaintextPass, md5CryptText)); 
    if (md5CryptText.startsWith("$apr1$"))
      return md5CryptText.equals(apacheCrypt(plaintextPass, md5CryptText)); 
    throw new RuntimeException("Bad md5CryptText");
  }
}
