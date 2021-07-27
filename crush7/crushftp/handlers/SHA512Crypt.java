package crushftp.handlers;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public final class SHA512Crypt {
  private static final String sha512_salt_prefix = "$6$";
  
  private static final String sha512_rounds_prefix = "rounds=";
  
  private static final int SALT_LEN_MAX = 16;
  
  private static final int ROUNDS_DEFAULT = 5000;
  
  private static final int ROUNDS_MIN = 1000;
  
  private static final int ROUNDS_MAX = 999999999;
  
  private static final String SALTCHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
  
  private static final String itoa64 = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
  
  private static MessageDigest getSHA512() {
    try {
      return MessageDigest.getInstance("SHA-512");
    } catch (NoSuchAlgorithmException ex) {
      throw new RuntimeException(ex);
    } 
  }
  
  public static final String Sha512_crypt(String keyStr, String saltStr, int roundsCount) {
    MessageDigest ctx = getSHA512();
    MessageDigest alt_ctx = getSHA512();
    byte[] p_bytes = (byte[])null;
    byte[] s_bytes = (byte[])null;
    int rounds = 5000;
    boolean include_round_count = false;
    if (saltStr != null) {
      if (saltStr.startsWith("$6$"))
        saltStr = saltStr.substring("$6$".length()); 
      if (saltStr.startsWith("rounds=")) {
        String num = saltStr.substring("rounds=".length(), saltStr.indexOf('$'));
        int srounds = Integer.valueOf(num).intValue();
        saltStr = saltStr.substring(saltStr.indexOf('$') + 1);
        rounds = Math.max(1000, Math.min(srounds, 999999999));
        include_round_count = true;
      } 
      if (saltStr.length() > 16)
        saltStr = saltStr.substring(0, 16); 
      if (saltStr.endsWith("$")) {
        saltStr = saltStr.substring(0, saltStr.length() - 1);
      } else if (saltStr.indexOf("$") != -1) {
        saltStr = saltStr.substring(0, saltStr.indexOf("$"));
      } 
    } else {
      Random randgen = new Random();
      StringBuilder saltBuf = new StringBuilder();
      while (saltBuf.length() < 16) {
        int index = (int)(randgen.nextFloat() * "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".length());
        saltBuf.append("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".substring(index, index + 1));
      } 
      saltStr = saltBuf.toString();
    } 
    if (roundsCount != 0)
      rounds = Math.max(1000, Math.min(roundsCount, 999999999)); 
    byte[] key = keyStr.getBytes();
    byte[] salt = saltStr.getBytes();
    ctx.reset();
    ctx.update(key, 0, key.length);
    ctx.update(salt, 0, salt.length);
    alt_ctx.reset();
    alt_ctx.update(key, 0, key.length);
    alt_ctx.update(salt, 0, salt.length);
    alt_ctx.update(key, 0, key.length);
    byte[] alt_result = alt_ctx.digest();
    int cnt;
    for (cnt = key.length; cnt > 64; cnt -= 64)
      ctx.update(alt_result, 0, 64); 
    ctx.update(alt_result, 0, cnt);
    for (cnt = key.length; cnt > 0; cnt >>= 1) {
      if ((cnt & 0x1) != 0) {
        ctx.update(alt_result, 0, 64);
      } else {
        ctx.update(key, 0, key.length);
      } 
    } 
    alt_result = ctx.digest();
    alt_ctx.reset();
    for (cnt = 0; cnt < key.length; cnt++)
      alt_ctx.update(key, 0, key.length); 
    byte[] temp_result = alt_ctx.digest();
    p_bytes = new byte[key.length];
    int cnt2;
    for (cnt2 = 0, cnt = p_bytes.length; cnt >= 64; cnt -= 64) {
      System.arraycopy(temp_result, 0, p_bytes, cnt2, 64);
      cnt2 += 64;
    } 
    System.arraycopy(temp_result, 0, p_bytes, cnt2, cnt);
    alt_ctx.reset();
    for (cnt = 0; cnt < 16 + (alt_result[0] & 0xFF); cnt++)
      alt_ctx.update(salt, 0, salt.length); 
    temp_result = alt_ctx.digest();
    s_bytes = new byte[salt.length];
    for (cnt2 = 0, cnt = s_bytes.length; cnt >= 64; cnt -= 64) {
      System.arraycopy(temp_result, 0, s_bytes, cnt2, 64);
      cnt2 += 64;
    } 
    System.arraycopy(temp_result, 0, s_bytes, cnt2, cnt);
    for (cnt = 0; cnt < rounds; cnt++) {
      ctx.reset();
      if ((cnt & 0x1) != 0) {
        ctx.update(p_bytes, 0, key.length);
      } else {
        ctx.update(alt_result, 0, 64);
      } 
      if (cnt % 3 != 0)
        ctx.update(s_bytes, 0, salt.length); 
      if (cnt % 7 != 0)
        ctx.update(p_bytes, 0, key.length); 
      if ((cnt & 0x1) != 0) {
        ctx.update(alt_result, 0, 64);
      } else {
        ctx.update(p_bytes, 0, key.length);
      } 
      alt_result = ctx.digest();
    } 
    StringBuilder buffer = new StringBuilder("$6$");
    if (include_round_count || rounds != 5000) {
      buffer.append("rounds=");
      buffer.append(rounds);
      buffer.append("$");
    } 
    buffer.append(saltStr);
    buffer.append("$");
    buffer.append(b64_from_24bit(alt_result[0], alt_result[21], alt_result[42], 4));
    buffer.append(b64_from_24bit(alt_result[22], alt_result[43], alt_result[1], 4));
    buffer.append(b64_from_24bit(alt_result[44], alt_result[2], alt_result[23], 4));
    buffer.append(b64_from_24bit(alt_result[3], alt_result[24], alt_result[45], 4));
    buffer.append(b64_from_24bit(alt_result[25], alt_result[46], alt_result[4], 4));
    buffer.append(b64_from_24bit(alt_result[47], alt_result[5], alt_result[26], 4));
    buffer.append(b64_from_24bit(alt_result[6], alt_result[27], alt_result[48], 4));
    buffer.append(b64_from_24bit(alt_result[28], alt_result[49], alt_result[7], 4));
    buffer.append(b64_from_24bit(alt_result[50], alt_result[8], alt_result[29], 4));
    buffer.append(b64_from_24bit(alt_result[9], alt_result[30], alt_result[51], 4));
    buffer.append(b64_from_24bit(alt_result[31], alt_result[52], alt_result[10], 4));
    buffer.append(b64_from_24bit(alt_result[53], alt_result[11], alt_result[32], 4));
    buffer.append(b64_from_24bit(alt_result[12], alt_result[33], alt_result[54], 4));
    buffer.append(b64_from_24bit(alt_result[34], alt_result[55], alt_result[13], 4));
    buffer.append(b64_from_24bit(alt_result[56], alt_result[14], alt_result[35], 4));
    buffer.append(b64_from_24bit(alt_result[15], alt_result[36], alt_result[57], 4));
    buffer.append(b64_from_24bit(alt_result[37], alt_result[58], alt_result[16], 4));
    buffer.append(b64_from_24bit(alt_result[59], alt_result[17], alt_result[38], 4));
    buffer.append(b64_from_24bit(alt_result[18], alt_result[39], alt_result[60], 4));
    buffer.append(b64_from_24bit(alt_result[40], alt_result[61], alt_result[19], 4));
    buffer.append(b64_from_24bit(alt_result[62], alt_result[20], alt_result[41], 4));
    buffer.append(b64_from_24bit((byte)0, (byte)0, alt_result[63], 2));
    ctx.reset();
    return buffer.toString();
  }
  
  private static final String b64_from_24bit(byte B2, byte B1, byte B0, int size) {
    int v = (B2 & 0xFF) << 16 | (B1 & 0xFF) << 8 | B0 & 0xFF;
    StringBuilder result = new StringBuilder();
    while (--size >= 0) {
      result.append("./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".charAt(v & 0x3F));
      v >>>= 6;
    } 
    return result.toString();
  }
  
  public static final boolean verifyPassword(String plaintextPass, String sha512CryptText) {
    if (sha512CryptText.startsWith("$6$"))
      return sha512CryptText.equals(Sha512_crypt(plaintextPass, sha512CryptText, 0)); 
    throw new RuntimeException("Bad sha512CryptText");
  }
  
  public static final boolean verifyHashTextFormat(String sha512CryptText) {
    if (!sha512CryptText.startsWith("$6$"))
      return false; 
    sha512CryptText = sha512CryptText.substring("$6$".length());
    if (sha512CryptText.startsWith("rounds=")) {
      String num = sha512CryptText.substring("rounds=".length(), sha512CryptText.indexOf('$'));
      try {
        int srounds = Integer.valueOf(num).intValue();
      } catch (NumberFormatException ex) {
        return false;
      } 
      sha512CryptText = sha512CryptText.substring(sha512CryptText.indexOf('$') + 1);
    } 
    if (sha512CryptText.indexOf('$') > 17)
      return false; 
    sha512CryptText = sha512CryptText.substring(sha512CryptText.indexOf('$') + 1);
    for (int i = 0; i < sha512CryptText.length(); i++) {
      if ("./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".indexOf(sha512CryptText.charAt(i)) == -1)
        return false; 
    } 
    return true;
  }
}
