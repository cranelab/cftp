package com.crushftp.client;

import com.didisoft.pgp.PGPLib;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class EncryptedOutputStream extends OutputStream {
  OutputStream out = null;
  
  byte[] publicKeyBytes = null;
  
  boolean closed = false;
  
  boolean closeStream = false;
  
  PGPLib pgp = new PGPLib();
  
  byte[] b1 = new byte[1];
  
  Socket sock1 = null;
  
  Socket sock2 = null;
  
  public EncryptedOutputStream(OutputStream out2, String publicKey, boolean closeStream) throws IOException {
    this.pgp.setUseExpiredKeys(true);
    this.closeStream = closeStream;
    this.publicKeyBytes = new byte[(int)(new File(publicKey)).length()];
    FileInputStream inFile = null;
    try {
      inFile = new FileInputStream(new File(publicKey));
      inFile.read(this.publicKeyBytes);
    } finally {
      if (inFile != null)
        inFile.close(); 
    } 
    ServerSocket ss = new ServerSocket(0);
    try {
      this.sock1 = new Socket("127.0.0.1", ss.getLocalPort());
      this.sock2 = ss.accept();
    } finally {
      ss.close();
    } 
    this.out = this.sock1.getOutputStream();
    Thread t = new Thread(new Runnable(this, out2) {
          final EncryptedOutputStream this$0;
          
          private final OutputStream val$out2;
          
          public void run() {
            try {
              this.this$0.pgp.encryptStream(this.this$0.sock2.getInputStream(), "", new ByteArrayInputStream(this.this$0.publicKeyBytes), this.val$out2, true, false);
            } catch (Exception e) {
              e.printStackTrace();
            } 
            try {
              this.val$out2.close();
              this.this$0.sock2.close();
            } catch (Exception e) {
              e.printStackTrace();
            } 
          }
        });
    t.setName(String.valueOf(Thread.currentThread().getName()) + "_EncryptedOutputStream");
    t.start();
  }
  
  public void write(int i) throws IOException {
    this.b1[0] = (byte)i;
    write(this.b1, 0, 1);
  }
  
  public void write(byte[] b) throws IOException {
    write(b, 0, b.length);
  }
  
  public void write(byte[] b, int off, int len) throws IOException {
    this.out.write(b, off, len);
  }
  
  public void close() throws IOException {
    if (this.closed)
      return; 
    this.out.flush();
    if (this.closeStream)
      this.out.close(); 
    this.sock1.close();
    this.sock2.close();
    this.closed = true;
  }
}
