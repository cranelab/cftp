package com.hierynomus.smbj.share;

import com.hierynomus.mserref.NtStatus;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.mssmb2.messages.SMB2ReadResponse;
import com.hierynomus.protocol.commons.concurrent.Futures;
import com.hierynomus.protocol.transport.TransportException;
import com.hierynomus.smbj.ProgressListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FileInputStream extends InputStream {
  private final long readTimeout;
  
  private File file;
  
  private long offset = 0L;
  
  private int curr = 0;
  
  private byte[] buf;
  
  private ProgressListener progressListener;
  
  private boolean isClosed;
  
  private Future<SMB2ReadResponse> nextResponse;
  
  private static final Logger logger = LoggerFactory.getLogger(FileInputStream.class);
  
  private int bufferSize;
  
  FileInputStream(File file, int bufferSize, long readTimeout, ProgressListener progressListener) {
    this.file = file;
    this.bufferSize = bufferSize;
    this.progressListener = progressListener;
    this.readTimeout = readTimeout;
  }
  
  public int read() throws IOException {
    if (this.buf == null || this.curr >= this.buf.length)
      loadBuffer(); 
    if (this.isClosed)
      return -1; 
    return this.buf[this.curr++] & 0xFF;
  }
  
  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }
  
  public int read(byte[] b, int off, int len) throws IOException {
    if (this.buf == null || this.curr >= this.buf.length)
      loadBuffer(); 
    if (this.isClosed)
      return -1; 
    int l = (this.buf.length - this.curr > len) ? len : (this.buf.length - this.curr);
    System.arraycopy(this.buf, this.curr, b, off, l);
    this.curr += l;
    return l;
  }
  
  public void close() throws IOException {
    this.isClosed = true;
    this.file = null;
    this.buf = null;
  }
  
  public int available() throws IOException {
    return 0;
  }
  
  public long skip(long n) throws IOException {
    if (this.buf == null) {
      this.offset += n;
    } else if (this.curr + n < this.buf.length) {
      this.curr = (int)(this.curr + n);
    } else {
      this.offset += this.curr + n - this.buf.length;
      this.buf = null;
      this.nextResponse = null;
    } 
    return n;
  }
  
  private void loadBuffer() throws IOException {
    if (this.nextResponse == null)
      this.nextResponse = sendRequest(); 
    SMB2ReadResponse res = Futures.<SMB2ReadResponse, TransportException>get(this.nextResponse, this.readTimeout, TimeUnit.MILLISECONDS, TransportException.Wrapper);
    if (res.getHeader().getStatus() == NtStatus.STATUS_SUCCESS) {
      this.buf = res.getData();
      this.curr = 0;
      this.offset += res.getDataLength();
      if (this.progressListener != null)
        this.progressListener.onProgressChanged(this.offset, -1L); 
    } 
    if (res.getHeader().getStatus() == NtStatus.STATUS_END_OF_FILE) {
      logger.debug("EOF, {} bytes read", Long.valueOf(this.offset));
      this.isClosed = true;
      return;
    } 
    if (res.getHeader().getStatus() != NtStatus.STATUS_SUCCESS)
      throw new SMBApiException(res.getHeader(), "Read failed for " + this); 
    this.nextResponse = sendRequest();
  }
  
  private Future<SMB2ReadResponse> sendRequest() throws IOException {
    return this.file.readAsync(this.offset, this.bufferSize);
  }
}
