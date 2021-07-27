package com.hierynomus.smbj.io;

import java.io.IOException;

public class ArrayByteChunkProvider extends ByteChunkProvider {
  private final byte[] data;
  
  private int bufferOffset;
  
  private int remaining;
  
  public ArrayByteChunkProvider(byte[] data, long fileOffset) {
    this(data, 0, data.length, fileOffset);
  }
  
  public ArrayByteChunkProvider(byte[] data, int offset, int length, long fileOffset) {
    this.data = data;
    this.offset = fileOffset;
    this.bufferOffset = offset;
    this.remaining = length;
  }
  
  public boolean isAvailable() {
    return (this.remaining > 0);
  }
  
  protected int getChunk(byte[] chunk) throws IOException {
    int write = chunk.length;
    if (write > this.remaining)
      write = this.remaining; 
    System.arraycopy(this.data, this.bufferOffset, chunk, 0, write);
    this.bufferOffset += write;
    this.remaining -= write;
    return write;
  }
  
  public int bytesLeft() {
    return this.remaining;
  }
}
