package com.sun.mail.auth;

import com.sun.mail.util.BASE64DecoderStream;
import com.sun.mail.util.BASE64EncoderStream;
import com.sun.mail.util.MailLogger;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.logging.Level;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

public class Ntlm {
  private byte[] type1;
  
  private byte[] type3;
  
  private SecretKeyFactory fac;
  
  private Cipher cipher;
  
  private MD4 md4;
  
  private String hostname;
  
  private String ntdomain;
  
  private String username;
  
  private String password;
  
  private MailLogger logger;
  
  private void init0() {
    this.type1 = new byte[256];
    this.type3 = new byte[256];
    System.arraycopy(new byte[] { 78, 84, 76, 77, 83, 83, 80, 0, 1 }, 0, this.type1, 0, 9);
    this.type1[12] = 3;
    this.type1[13] = -78;
    this.type1[28] = 32;
    System.arraycopy(new byte[] { 78, 84, 76, 77, 83, 83, 80, 0, 3 }, 0, this.type3, 0, 9);
    this.type3[12] = 24;
    this.type3[14] = 24;
    this.type3[20] = 24;
    this.type3[22] = 24;
    this.type3[32] = 64;
    this.type3[60] = 1;
    this.type3[61] = -126;
    try {
      this.fac = SecretKeyFactory.getInstance("DES");
      this.cipher = Cipher.getInstance("DES/ECB/NoPadding");
      this.md4 = new MD4();
    } catch (NoSuchPaddingException e) {
      assert false;
    } catch (NoSuchAlgorithmException e) {
      assert false;
    } 
  }
  
  public Ntlm(String ntdomain, String hostname, String username, String password, MailLogger logger) {
    int i = hostname.indexOf('.');
    if (i != -1)
      hostname = hostname.substring(0, i); 
    i = username.indexOf('\\');
    if (i != -1) {
      ntdomain = username.substring(0, i).toUpperCase(Locale.ENGLISH);
      username = username.substring(i + 1);
    } else if (ntdomain == null) {
      ntdomain = "";
    } 
    this.ntdomain = ntdomain;
    this.hostname = hostname;
    this.username = username;
    this.password = password;
    this.logger = logger.getLogger(getClass(), "DEBUG NTLM");
    init0();
  }
  
  private void copybytes(byte[] dest, int destpos, String src, String enc) {
    try {
      byte[] x = src.getBytes(enc);
      System.arraycopy(x, 0, dest, destpos, x.length);
    } catch (UnsupportedEncodingException e) {
      assert false;
    } 
  }
  
  public String generateType1Msg(int flags) {
    int dlen = this.ntdomain.length();
    this.type1[16] = (byte)(dlen % 256);
    this.type1[17] = (byte)(dlen / 256);
    this.type1[18] = this.type1[16];
    this.type1[19] = this.type1[17];
    if (dlen == 0)
      this.type1[13] = (byte)(this.type1[13] & 0xFFFFFFEF); 
    int hlen = this.hostname.length();
    this.type1[24] = (byte)(hlen % 256);
    this.type1[25] = (byte)(hlen / 256);
    this.type1[26] = this.type1[24];
    this.type1[27] = this.type1[25];
    copybytes(this.type1, 32, this.hostname, "iso-8859-1");
    copybytes(this.type1, hlen + 32, this.ntdomain, "iso-8859-1");
    this.type1[20] = (byte)((hlen + 32) % 256);
    this.type1[21] = (byte)((hlen + 32) / 256);
    byte[] msg = new byte[32 + hlen + dlen];
    System.arraycopy(this.type1, 0, msg, 0, 32 + hlen + dlen);
    if (this.logger.isLoggable(Level.FINE))
      this.logger.fine("type 1 message: " + toHex(msg)); 
    String result = null;
    try {
      result = new String(BASE64EncoderStream.encode(msg), "iso-8859-1");
    } catch (UnsupportedEncodingException e) {
      assert false;
    } 
    return result;
  }
  
  private byte[] makeDesKey(byte[] input, int off) {
    int[] in = new int[input.length];
    for (int i = 0; i < in.length; i++)
      in[i] = (input[i] < 0) ? (input[i] + 256) : input[i]; 
    byte[] out = new byte[8];
    out[0] = (byte)in[off + 0];
    out[1] = (byte)(in[off + 0] << 7 & 0xFF | in[off + 1] >> 1);
    out[2] = (byte)(in[off + 1] << 6 & 0xFF | in[off + 2] >> 2);
    out[3] = (byte)(in[off + 2] << 5 & 0xFF | in[off + 3] >> 3);
    out[4] = (byte)(in[off + 3] << 4 & 0xFF | in[off + 4] >> 4);
    out[5] = (byte)(in[off + 4] << 3 & 0xFF | in[off + 5] >> 5);
    out[6] = (byte)(in[off + 5] << 2 & 0xFF | in[off + 6] >> 6);
    out[7] = (byte)(in[off + 6] << 1 & 0xFF);
    return out;
  }
  
  private byte[] calcLMHash() throws GeneralSecurityException {
    byte[] magic = { 75, 71, 83, 33, 64, 35, 36, 37 };
    byte[] pwb = null;
    try {
      pwb = this.password.toUpperCase(Locale.ENGLISH).getBytes("iso-8859-1");
    } catch (UnsupportedEncodingException ex) {
      assert false;
    } 
    byte[] pwb1 = new byte[14];
    int len = this.password.length();
    if (len > 14)
      len = 14; 
    System.arraycopy(pwb, 0, pwb1, 0, len);
    DESKeySpec dks1 = new DESKeySpec(makeDesKey(pwb1, 0));
    DESKeySpec dks2 = new DESKeySpec(makeDesKey(pwb1, 7));
    SecretKey key1 = this.fac.generateSecret(dks1);
    SecretKey key2 = this.fac.generateSecret(dks2);
    this.cipher.init(1, key1);
    byte[] out1 = this.cipher.doFinal(magic, 0, 8);
    this.cipher.init(1, key2);
    byte[] out2 = this.cipher.doFinal(magic, 0, 8);
    byte[] result = new byte[21];
    System.arraycopy(out1, 0, result, 0, 8);
    System.arraycopy(out2, 0, result, 8, 8);
    return result;
  }
  
  private byte[] calcNTHash() throws GeneralSecurityException {
    byte[] pw = null;
    try {
      pw = this.password.getBytes("UnicodeLittleUnmarked");
    } catch (UnsupportedEncodingException e) {
      assert false;
    } 
    byte[] out = this.md4.digest(pw);
    byte[] result = new byte[21];
    System.arraycopy(out, 0, result, 0, 16);
    return result;
  }
  
  private byte[] calcResponse(byte[] key, byte[] text) throws GeneralSecurityException {
    assert key.length == 21;
    DESKeySpec dks1 = new DESKeySpec(makeDesKey(key, 0));
    DESKeySpec dks2 = new DESKeySpec(makeDesKey(key, 7));
    DESKeySpec dks3 = new DESKeySpec(makeDesKey(key, 14));
    SecretKey key1 = this.fac.generateSecret(dks1);
    SecretKey key2 = this.fac.generateSecret(dks2);
    SecretKey key3 = this.fac.generateSecret(dks3);
    this.cipher.init(1, key1);
    byte[] out1 = this.cipher.doFinal(text, 0, 8);
    this.cipher.init(1, key2);
    byte[] out2 = this.cipher.doFinal(text, 0, 8);
    this.cipher.init(1, key3);
    byte[] out3 = this.cipher.doFinal(text, 0, 8);
    byte[] result = new byte[24];
    System.arraycopy(out1, 0, result, 0, 8);
    System.arraycopy(out2, 0, result, 8, 8);
    System.arraycopy(out3, 0, result, 16, 8);
    return result;
  }
  
  public String generateType3Msg(String challenge) {
    try {
      byte[] type2 = null;
      try {
        type2 = BASE64DecoderStream.decode(challenge.getBytes("us-ascii"));
      } catch (UnsupportedEncodingException ex) {
        assert false;
      } 
      byte[] nonce = new byte[8];
      System.arraycopy(type2, 24, nonce, 0, 8);
      int ulen = this.username.length() * 2;
      this.type3[38] = (byte)(ulen % 256);
      this.type3[36] = (byte)(ulen % 256);
      this.type3[39] = (byte)(ulen / 256);
      this.type3[37] = (byte)(ulen / 256);
      int dlen = this.ntdomain.length() * 2;
      this.type3[30] = (byte)(dlen % 256);
      this.type3[28] = (byte)(dlen % 256);
      this.type3[31] = (byte)(dlen / 256);
      this.type3[29] = (byte)(dlen / 256);
      int hlen = this.hostname.length() * 2;
      this.type3[46] = (byte)(hlen % 256);
      this.type3[44] = (byte)(hlen % 256);
      this.type3[47] = (byte)(hlen / 256);
      this.type3[45] = (byte)(hlen / 256);
      int l = 64;
      copybytes(this.type3, l, this.ntdomain, "UnicodeLittleUnmarked");
      this.type3[32] = (byte)(l % 256);
      this.type3[33] = (byte)(l / 256);
      l += dlen;
      copybytes(this.type3, l, this.username, "UnicodeLittleUnmarked");
      this.type3[40] = (byte)(l % 256);
      this.type3[41] = (byte)(l / 256);
      l += ulen;
      copybytes(this.type3, l, this.hostname, "UnicodeLittleUnmarked");
      this.type3[48] = (byte)(l % 256);
      this.type3[49] = (byte)(l / 256);
      l += hlen;
      byte[] lmhash = calcLMHash();
      byte[] lmresponse = calcResponse(lmhash, nonce);
      byte[] nthash = calcNTHash();
      byte[] ntresponse = calcResponse(nthash, nonce);
      System.arraycopy(lmresponse, 0, this.type3, l, 24);
      this.type3[16] = (byte)(l % 256);
      this.type3[17] = (byte)(l / 256);
      l += 24;
      System.arraycopy(ntresponse, 0, this.type3, l, 24);
      this.type3[24] = (byte)(l % 256);
      this.type3[25] = (byte)(l / 256);
      l += 24;
      this.type3[56] = (byte)(l % 256);
      this.type3[57] = (byte)(l / 256);
      byte[] msg = new byte[l];
      System.arraycopy(this.type3, 0, msg, 0, l);
      if (this.logger.isLoggable(Level.FINE))
        this.logger.fine("type 3 message: " + toHex(msg)); 
      String result = null;
      try {
        result = new String(BASE64EncoderStream.encode(msg), "iso-8859-1");
      } catch (UnsupportedEncodingException e) {
        assert false;
      } 
      return result;
    } catch (GeneralSecurityException ex) {
      this.logger.log(Level.FINE, "GeneralSecurityException", ex);
      return "";
    } 
  }
  
  private static char[] hex = new char[] { 
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
      'A', 'B', 'C', 'D', 'E', 'F' };
  
  private static String toHex(byte[] b) {
    StringBuffer sb = new StringBuffer(b.length * 3);
    for (int i = 0; i < b.length; i++)
      sb.append(hex[b[i] >> 4 & 0xF]).append(hex[b[i] & 0xF]).append(' '); 
    return sb.toString();
  }
}
