package com.hierynomus.smbj.io;

import com.hierynomus.smbj.common.SMBRuntimeException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class InputStreamByteChunkProvider extends ByteChunkProvider {
  private BufferedInputStream is;
  
  public InputStreamByteChunkProvider(InputStream is) {
    if (is instanceof BufferedInputStream) {
      this.is = (BufferedInputStream)is;
    } else {
      this.is = new BufferedInputStream(is);
    } 
  }
  
  protected int getChunk(byte[] chunk) throws IOException {
    if (this.is == null)
      return -1; 
    int count = 0;
    int read;
    while (count < 65536 && (read = this.is.read(chunk, count, 65536 - count)) != -1)
      count += read; 
    return count;
  }
  
  public int bytesLeft() {
    try {
      if (this.is != null)
        return this.is.available(); 
      return -1;
    } catch (IOException e) {
      throw new SMBRuntimeException(e);
    } 
  }
  
  public boolean isAvailable() {
    return (bytesLeft() > 0);
  }
  
  public void close() throws IOException {
    if (this.is != null)
      try {
        this.is.close();
      } finally {
        this.is = null;
      }  
  }
}
