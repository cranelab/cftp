package com.hierynomus.smbj.share;

import com.hierynomus.mssmb2.SMB2FileId;
import com.hierynomus.mssmb2.messages.SMB2WriteResponse;
import com.hierynomus.smbj.ProgressListener;
import com.hierynomus.smbj.io.ArrayByteChunkProvider;
import com.hierynomus.smbj.io.ByteChunkProvider;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SMB2Writer {
  private static final Logger logger = LoggerFactory.getLogger(SMB2Writer.class);
  
  private Share share;
  
  private SMB2FileId fileId;
  
  private String entryName;
  
  public SMB2Writer(Share share, SMB2FileId fileId, String entryName) {
    this.share = share;
    this.fileId = fileId;
    this.entryName = entryName;
  }
  
  public int write(byte[] buffer, long fileOffset) {
    return write(buffer, fileOffset, 0, buffer.length);
  }
  
  public int write(byte[] buffer, long fileOffset, int offset, int length) {
    return write(new ArrayByteChunkProvider(buffer, offset, length, fileOffset), (ProgressListener)null);
  }
  
  public int write(ByteChunkProvider provider) {
    return write(provider, (ProgressListener)null);
  }
  
  public int write(ByteChunkProvider provider, ProgressListener progressListener) {
    int bytesWritten = 0;
    while (provider.isAvailable()) {
      logger.debug("Writing to {} from offset {}", this.entryName, Long.valueOf(provider.getOffset()));
      SMB2WriteResponse wresp = this.share.write(this.fileId, provider);
      bytesWritten = (int)(bytesWritten + wresp.getBytesWritten());
      if (progressListener != null)
        progressListener.onProgressChanged(wresp.getBytesWritten(), provider.getOffset()); 
    } 
    return bytesWritten;
  }
  
  public OutputStream getOutputStream() {
    return getOutputStream(null);
  }
  
  public OutputStream getOutputStream(ProgressListener listener) {
    return new FileOutputStream(
        this, 
        this.share.getWriteBufferSize(), 
        listener);
  }
}
