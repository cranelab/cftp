package com.sun.mail.smtp;

import com.sun.mail.util.ASCIIUtility;
import com.sun.mail.util.BASE64DecoderStream;
import com.sun.mail.util.BASE64EncoderStream;
import com.sun.mail.util.MailLogger;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StreamTokenizer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;

public class DigestMD5 {
  private MailLogger logger;
  
  private MessageDigest md5;
  
  private String uri;
  
  private String clientResponse;
  
  public DigestMD5(MailLogger logger) {
    this.logger = logger.getLogger(getClass(), "DEBUG DIGEST-MD5");
    logger.config("DIGEST-MD5 Loaded");
  }
  
  public byte[] authClient(String host, String user, String passwd, String realm, String serverChallenge) throws IOException {
    SecureRandom random;
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    OutputStream b64os = new BASE64EncoderStream(bos, 2147483647);
    try {
      random = new SecureRandom();
      this.md5 = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException ex) {
      this.logger.log(Level.FINE, "NoSuchAlgorithmException", ex);
      throw new IOException(ex.toString());
    } 
    StringBuffer result = new StringBuffer();
    this.uri = "smtp/" + host;
    String nc = "00000001";
    String qop = "auth";
    byte[] bytes = new byte[32];
    this.logger.fine("Begin authentication ...");
    Map<String, String> map = tokenize(serverChallenge);
    if (realm == null) {
      String text = map.get("realm");
      realm = (text != null) ? (new StringTokenizer(text, ",")).nextToken() : host;
    } 
    String nonce = map.get("nonce");
    random.nextBytes(bytes);
    b64os.write(bytes);
    b64os.flush();
    String cnonce = bos.toString("iso-8859-1");
    bos.reset();
    this.md5.update(this.md5.digest(
          ASCIIUtility.getBytes(user + ":" + realm + ":" + passwd)));
    this.md5.update(ASCIIUtility.getBytes(":" + nonce + ":" + cnonce));
    this.clientResponse = toHex(this.md5.digest()) + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":";
    this.md5.update(ASCIIUtility.getBytes("AUTHENTICATE:" + this.uri));
    this.md5.update(ASCIIUtility.getBytes(this.clientResponse + toHex(this.md5.digest())));
    result.append("username=\"" + user + "\"");
    result.append(",realm=\"" + realm + "\"");
    result.append(",qop=" + qop);
    result.append(",nc=" + nc);
    result.append(",nonce=\"" + nonce + "\"");
    result.append(",cnonce=\"" + cnonce + "\"");
    result.append(",digest-uri=\"" + this.uri + "\"");
    result.append(",response=" + toHex(this.md5.digest()));
    if (this.logger.isLoggable(Level.FINE))
      this.logger.fine("Response => " + result.toString()); 
    b64os.write(ASCIIUtility.getBytes(result.toString()));
    b64os.flush();
    return bos.toByteArray();
  }
  
  public boolean authServer(String serverResponse) throws IOException {
    Map<String, String> map = tokenize(serverResponse);
    this.md5.update(ASCIIUtility.getBytes(":" + this.uri));
    this.md5.update(ASCIIUtility.getBytes(this.clientResponse + toHex(this.md5.digest())));
    String text = toHex(this.md5.digest());
    if (!text.equals(map.get("rspauth"))) {
      if (this.logger.isLoggable(Level.FINE))
        this.logger.fine("Expected => rspauth=" + text); 
      return false;
    } 
    return true;
  }
  
  private Map<String, String> tokenize(String serverResponse) throws IOException {
    Map<String, String> map = new HashMap<String, String>();
    byte[] bytes = serverResponse.getBytes("iso-8859-1");
    String key = null;
    StreamTokenizer tokens = new StreamTokenizer(new InputStreamReader(new BASE64DecoderStream(new ByteArrayInputStream(bytes, 4, bytes.length - 4)), "iso-8859-1"));
    tokens.ordinaryChars(48, 57);
    tokens.wordChars(48, 57);
    int ttype;
    while ((ttype = tokens.nextToken()) != -1) {
      switch (ttype) {
        case -3:
          if (key == null)
            key = tokens.sval; 
        case 34:
          if (this.logger.isLoggable(Level.FINE))
            this.logger.fine("Received => " + key + "='" + tokens.sval + "'"); 
          if (map.containsKey(key)) {
            map.put(key, (String)map.get(key) + "," + tokens.sval);
          } else {
            map.put(key, tokens.sval);
          } 
          key = null;
      } 
    } 
    return map;
  }
  
  private static char[] digits = new char[] { 
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
      'a', 'b', 'c', 'd', 'e', 'f' };
  
  private static String toHex(byte[] bytes) {
    char[] result = new char[bytes.length * 2];
    for (int index = 0, i = 0; index < bytes.length; index++) {
      int temp = bytes[index] & 0xFF;
      result[i++] = digits[temp >> 4];
      result[i++] = digits[temp & 0xF];
    } 
    return new String(result);
  }
}
