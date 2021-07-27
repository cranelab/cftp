package com.hierynomus.mssmb2.copy;

import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;

public class CopyChunkResponse {
  private long chunksWritten;
  
  private long chunkBytesWritten;
  
  private long totalBytesWritten;
  
  public CopyChunkResponse() {}
  
  public CopyChunkResponse(long chunksWritten, long chunkBytesWritten, long totalBytesWritten) {
    this.chunksWritten = chunksWritten;
    this.chunkBytesWritten = chunkBytesWritten;
    this.totalBytesWritten = totalBytesWritten;
  }
  
  public long getChunksWritten() {
    return this.chunksWritten;
  }
  
  public long getChunkBytesWritten() {
    return this.chunkBytesWritten;
  }
  
  public long getTotalBytesWritten() {
    return this.totalBytesWritten;
  }
  
  public final void read(SMBBuffer in) throws Buffer.BufferException {
    this.chunksWritten = in.readUInt32();
    this.chunkBytesWritten = in.readUInt32();
    this.totalBytesWritten = in.readUInt32();
  }
}
