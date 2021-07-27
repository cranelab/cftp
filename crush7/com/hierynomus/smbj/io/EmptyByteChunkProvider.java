package com.hierynomus.smbj.io;

import java.io.IOException;

public class EmptyByteChunkProvider extends ByteChunkProvider {
  public EmptyByteChunkProvider(long fileOffset) {
    this.offset = fileOffset;
  }
  
  public boolean isAvailable() {
    return false;
  }
  
  protected int getChunk(byte[] chunk) throws IOException {
    return 0;
  }
  
  public int bytesLeft() {
    return 0;
  }
}
