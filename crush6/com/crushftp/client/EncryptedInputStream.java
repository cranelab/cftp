package com.crushftp.client;

import com.didisoft.pgp.PGPLib;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class EncryptedInputStream extends InputStream {
  byte[] privateKeyBytes = null;
  
  String privateKeyPassword = null;
  
  InputStream in = null;
  
  boolean closed = false;
  
  long pos = 0L;
  
  long endPos = -1L;
  
  boolean closeStream = false;
  
  PGPLib pgp = new PGPLib();
  
  byte[] b1 = new byte[1];
  
  Socket sock1 = null;
  
  Socket sock2 = null;
  
  public EncryptedInputStream(InputStream in2, long endPos, String privateKey, String privateKeyPassword2, boolean closeStream) throws IOException {
    this.pgp.setUseExpiredKeys(true);
    this.endPos = endPos;
    this.closeStream = closeStream;
    this.privateKeyBytes = new byte[(int)(new File(privateKey)).length()];
    FileInputStream inFile = null;
    try {
      inFile = new FileInputStream(new File(privateKey));
      inFile.read(this.privateKeyBytes);
    } finally {
      if (inFile != null)
        inFile.close(); 
    } 
    this.privateKeyPassword = privateKeyPassword2;
    ServerSocket ss = new ServerSocket(0);
    try {
      this.sock1 = new Socket("127.0.0.1", ss.getLocalPort());
      this.sock2 = ss.accept();
    } finally {
      ss.close();
    } 
    this.in = this.sock1.getInputStream();
    Thread t = new Thread(new Runnable(this, in2) {
          final EncryptedInputStream this$0;
          
          private final InputStream val$in2;
          
          public void run() {
            try {
              this.this$0.pgp.decryptStream(this.val$in2, new ByteArrayInputStream(this.this$0.privateKeyBytes), this.this$0.privateKeyPassword, this.this$0.sock2.getOutputStream());
            } catch (Exception e) {
              e.printStackTrace();
            } 
            try {
              this.val$in2.close();
              this.this$0.sock2.close();
            } catch (Exception e) {
              e.printStackTrace();
            } 
          }
        });
    t.setName(String.valueOf(Thread.currentThread().getName()) + "_EncryptedInputStream");
    t.start();
  }
  
  public int read() throws IOException {
    int bytesRead = this.in.read(this.b1, 0, 1);
    if (bytesRead >= 0) {
      this.pos++;
      return this.b1[0] & 0xFF;
    } 
    return -1;
  }
  
  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }
  
  public int read(byte[] b, int off, int len) throws IOException {
    if (this.pos >= this.endPos && this.endPos >= 0L)
      return -1; 
    int i = this.in.read(b, off, len);
    if (i > 0)
      this.pos += i; 
    if (this.endPos > 0L && this.pos > this.endPos)
      i = (int)(i - this.pos - this.endPos); 
    return i;
  }
  
  public void close() throws IOException {
    if (this.closed)
      return; 
    if (this.closeStream)
      this.in.close(); 
    this.sock1.close();
    this.sock2.close();
    this.closed = true;
  }
}
