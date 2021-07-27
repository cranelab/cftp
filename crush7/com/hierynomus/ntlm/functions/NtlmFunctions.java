package com.hierynomus.ntlm.functions;

import com.hierynomus.msdfsc.messages.StandardCharsets;
import com.hierynomus.msdtyp.MsDataTypes;
import com.hierynomus.ntlm.NtlmException;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.protocol.commons.buffer.Endian;
import com.hierynomus.security.Cipher;
import com.hierynomus.security.Mac;
import com.hierynomus.security.MessageDigest;
import com.hierynomus.security.SecurityException;
import com.hierynomus.security.SecurityProvider;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Random;

public class NtlmFunctions {
  static final byte[] LMOWFv1_SECRET = new byte[] { 75, 71, 83, 33, 64, 35, 36, 37 };
  
  public static final Charset UNICODE = StandardCharsets.UTF_16LE;
  
  private final Random random;
  
  private final SecurityProvider securityProvider;
  
  public NtlmFunctions(Random random, SecurityProvider securityProvider) {
    this.random = random;
    this.securityProvider = securityProvider;
  }
  
  public byte[] NTOWFv2(String password, String username, String userDomain) {
    byte[] keyBytes = NTOWFv1(password, username, userDomain);
    byte[] usernameBytes = unicode(username.toUpperCase());
    byte[] userDomainBytes = unicode(userDomain);
    return hmac_md5(keyBytes, new byte[][] { usernameBytes, userDomainBytes });
  }
  
  public byte[] LMOWFv2(String password, String username, String userDomain) {
    return NTOWFv2(password, username, userDomain);
  }
  
  public byte[] NTOWFv1(String password, String username, String userDomain) {
    byte[] bytes = unicode(password);
    try {
      MessageDigest md4 = this.securityProvider.getDigest("MD4");
      md4.update(bytes);
      return md4.digest();
    } catch (SecurityException e) {
      throw new NtlmException(e);
    } 
  }
  
  public static byte[] unicode(String string) {
    return (string == null) ? new byte[0] : string.getBytes(UNICODE);
  }
  
  public byte[] hmac_md5(byte[] key, byte[]... message) {
    try {
      Mac hmacMD5 = this.securityProvider.getMac("HmacMD5");
      hmacMD5.init(key);
      byte b;
      int i;
      byte[][] arrayOfByte;
      for (i = (arrayOfByte = message).length, b = 0; b < i; ) {
        byte[] aMessage = arrayOfByte[b];
        hmacMD5.update(aMessage);
        b++;
      } 
      return hmacMD5.doFinal();
    } catch (SecurityException e) {
      throw new NtlmException(e);
    } 
  }
  
  public byte[] LMOWFv1(String password, String username, String userDomain) {
    try {
      byte[] bytes = password.toUpperCase().getBytes("US-ASCII");
      if (bytes.length != 14)
        bytes = Arrays.copyOf(bytes, 14); 
      Cipher leftCipher = getDESCipher(Arrays.copyOfRange(bytes, 0, 7));
      Cipher rightCipher = getDESCipher(Arrays.copyOfRange(bytes, 7, 14));
      byte[] lmHash = new byte[16];
      int outOff = leftCipher.update(LMOWFv1_SECRET, 0, LMOWFv1_SECRET.length, lmHash, 0);
      outOff += leftCipher.doFinal(lmHash, outOff);
      outOff += rightCipher.update(LMOWFv1_SECRET, 0, LMOWFv1_SECRET.length, lmHash, outOff);
      outOff += rightCipher.doFinal(lmHash, outOff);
      if (outOff != 16)
        throw new NtlmException("Incorrect lmHash calculated"); 
      return lmHash;
    } catch (UnsupportedEncodingException e) {
      throw new NtlmException(e);
    } catch (SecurityException e) {
      throw new NtlmException(e);
    } 
  }
  
  public byte[] getNTLMv2ClientChallenge(byte[] targetInformation) {
    byte[] challengeFromClient = new byte[8];
    this.random.nextBytes(challengeFromClient);
    long nowAsFileTime = MsDataTypes.nowAsFileTime();
    byte[] l_targetInfo = (targetInformation == null) ? new byte[0] : targetInformation;
    Buffer.PlainBuffer ccBuf = new Buffer.PlainBuffer(Endian.LE);
    ccBuf.putByte((byte)1);
    ccBuf.putByte((byte)1);
    ccBuf.putUInt16(0);
    ccBuf.putUInt32(0L);
    ccBuf.putLong(nowAsFileTime);
    ccBuf.putRawBytes(challengeFromClient);
    ccBuf.putUInt32(0L);
    ccBuf.putRawBytes(l_targetInfo);
    ccBuf.putUInt32(0L);
    return ccBuf.getCompactData();
  }
  
  public byte[] getNTLMv2Response(byte[] responseKeyNT, byte[] serverChallenge, byte[] ntlmv2ClientChallenge) {
    byte[] ntProofStr = hmac_md5(responseKeyNT, new byte[][] { serverChallenge, ntlmv2ClientChallenge });
    byte[] ntChallengeResponse = new byte[ntProofStr.length + ntlmv2ClientChallenge.length];
    System.arraycopy(ntProofStr, 0, ntChallengeResponse, 0, ntProofStr.length);
    System.arraycopy(ntlmv2ClientChallenge, 0, ntChallengeResponse, ntProofStr.length, ntlmv2ClientChallenge.length);
    return ntChallengeResponse;
  }
  
  public byte[] encryptRc4(byte[] key, byte[] val) throws NtlmException {
    Cipher c = getRC4Cipher(key);
    byte[] out = new byte[val.length];
    try {
      int bytes = c.update(val, 0, val.length, out, 0);
      c.doFinal(out, bytes);
    } catch (SecurityException e) {
      throw new NtlmException(e);
    } 
    return out;
  }
  
  private static byte[] setupKey(byte[] key56) {
    byte[] key = new byte[8];
    key[0] = (byte)(key56[0] >> 1 & 0xFF);
    key[1] = 
      (byte)(((key56[0] & 0x1) << 6 | (key56[1] & 0xFF) >> 2 & 0xFF) & 0xFF);
    key[2] = 
      (byte)(((key56[1] & 0x3) << 5 | (key56[2] & 0xFF) >> 3 & 0xFF) & 0xFF);
    key[3] = 
      (byte)(((key56[2] & 0x7) << 4 | (key56[3] & 0xFF) >> 4 & 0xFF) & 0xFF);
    key[4] = 
      (byte)(((key56[3] & 0xF) << 3 | (key56[4] & 0xFF) >> 5 & 0xFF) & 0xFF);
    key[5] = 
      (byte)(((key56[4] & 0x1F) << 2 | (key56[5] & 0xFF) >> 6 & 0xFF) & 0xFF);
    key[6] = 
      (byte)(((key56[5] & 0x3F) << 1 | (key56[6] & 0xFF) >> 7 & 0xFF) & 0xFF);
    key[7] = (byte)(key56[6] & Byte.MAX_VALUE);
    for (int i = 0; i < key.length; i++)
      key[i] = (byte)(key[i] << 1); 
    return key;
  }
  
  private Cipher getDESCipher(byte[] key) {
    try {
      Cipher cipher = this.securityProvider.getCipher("DES/ECB/NoPadding");
      cipher.init(Cipher.CryptMode.ENCRYPT, setupKey(key));
      return cipher;
    } catch (SecurityException e) {
      throw new NtlmException(e);
    } 
  }
  
  private Cipher getRC4Cipher(byte[] key) {
    try {
      Cipher cipher = this.securityProvider.getCipher("RC4");
      cipher.init(Cipher.CryptMode.ENCRYPT, key);
      return cipher;
    } catch (SecurityException e) {
      throw new NtlmException(e);
    } 
  }
}
