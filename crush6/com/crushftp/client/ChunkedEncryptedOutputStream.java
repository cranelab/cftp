package com.crushftp.client;

import com.didisoft.pgp.PGPLib;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChunkedEncryptedOutputStream extends OutputStream {
  OutputStream out = null;
  
  byte[] publicKeyBytes = null;
  
  long fileSize = -1L;
  
  ByteArrayOutputStream clearBytes = new ByteArrayOutputStream();
  
  ByteArrayOutputStream encryptedBytes = new ByteArrayOutputStream();
  
  boolean closed = false;
  
  boolean closeStream = false;
  
  long pos = 0L;
  
  boolean writeHeader = true;
  
  PGPLib pgp = new PGPLib();
  
  public static final int MB1 = 1048576;
  
  public static final int KB2 = 2048;
  
  byte[] b1 = new byte[1];
  
  public ChunkedEncryptedOutputStream(OutputStream out, String publicKey, long fileSize, boolean closeStream, long pos, boolean writeHeader) throws IOException {
    this.pgp.setUseExpiredKeys(true);
    this.out = out;
    this.closeStream = closeStream;
    this.pos = pos;
    this.writeHeader = writeHeader;
    this.publicKeyBytes = new byte[(int)(new File(publicKey)).length()];
    FileInputStream inFile = null;
    try {
      inFile = new FileInputStream(new File(publicKey));
      inFile.read(this.publicKeyBytes);
    } finally {
      if (inFile != null)
        inFile.close(); 
    } 
    this.fileSize = fileSize;
  }
  
  public void write(int i) throws IOException {
    this.b1[0] = (byte)i;
    write(this.b1, 0, 1);
  }
  
  public void write(byte[] b) throws IOException {
    write(b, 0, b.length);
  }
  
  public void write(byte[] b, int off, int len) throws IOException {
    if (this.clearBytes.size() + len > 1048576) {
      int allowedLen = this.clearBytes.size() + len - 1048576;
      this.clearBytes.write(b, off, allowedLen);
      flushBuffer(true);
      this.clearBytes.write(b, off + allowedLen, len - allowedLen);
    } else {
      this.clearBytes.write(b, off, len);
    } 
    if (this.clearBytes.size() == 1048576)
      flushBuffer(true); 
  }
  
  private void flushBuffer(boolean padding) throws IOException {
    if (this.clearBytes.size() == 0)
      return; 
    String md5Hash = "";
    try {
      MessageDigest md5 = MessageDigest.getInstance("MD5");
      md5.update(this.clearBytes.toByteArray());
      md5Hash = (new BigInteger(1, md5.digest())).toString(16).toLowerCase();
    } catch (NoSuchAlgorithmException noSuchAlgorithmException) {}
    this.encryptedBytes.reset();
    encrypt(new ByteArrayInputStream(this.clearBytes.toByteArray()), this.encryptedBytes, false, this.publicKeyBytes, "loc:" + this.pos);
    if (this.pos == 0L && this.writeHeader)
      this.out.write(("CRUSHFTP_PGPChunkedStream:dBa3Em7W4N:" + (String.valueOf(this.fileSize) + "0                                        ".substring(1)).substring(0, "0                                        ".length())).getBytes("UTF8")); 
    int paddingSize = 0;
    String infoHeader = String.valueOf(this.pos) + ":" + this.clearBytes.size() + ":" + this.encryptedBytes.size() + ":" + paddingSize + ":" + md5Hash + "\r";
    if (padding)
      paddingSize = 1050624 - this.encryptedBytes.size() - infoHeader.length(); 
    this.out.write(infoHeader.getBytes("UTF8"));
    if (padding)
      this.encryptedBytes.write(new byte[paddingSize]); 
    this.out.write(this.encryptedBytes.toByteArray());
    this.pos += this.clearBytes.size();
    this.encryptedBytes.reset();
    this.clearBytes.reset();
  }
  
  public void close() throws IOException {
    if (this.closed)
      return; 
    flushBuffer(false);
    this.out.flush();
    if (this.closeStream)
      this.out.close(); 
    this.closed = true;
  }
  
  public void encrypt(InputStream inStream, OutputStream outStream, boolean asciiArmor, byte[] publicBytes, String chunkName) throws IOException {
    try {
      ByteArrayInputStream bytesIn = new ByteArrayInputStream(publicBytes);
      this.pgp.encryptStream(inStream, chunkName, bytesIn, outStream, asciiArmor, false);
      bytesIn.close();
      inStream.close();
    } catch (Exception e) {
      throw new IOException(e);
    } 
  }
}
