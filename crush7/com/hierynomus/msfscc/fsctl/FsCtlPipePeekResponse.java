package com.hierynomus.msfscc.fsctl;

import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.protocol.commons.buffer.Buffer;

public class FsCtlPipePeekResponse {
  public static final int STRUCTURE_SIZE = 24;
  
  private PipeState state;
  
  private long readDataAvailable;
  
  private long numberOfMessages;
  
  private long messageLength;
  
  private byte[] data;
  
  public FsCtlPipePeekResponse() {}
  
  public FsCtlPipePeekResponse(PipeState state, long readDataAvailable, long numberOfMessages, long messageLength, byte[] data) {
    this.state = state;
    this.readDataAvailable = readDataAvailable;
    this.numberOfMessages = numberOfMessages;
    this.messageLength = messageLength;
    this.data = data;
  }
  
  public PipeState getState() {
    return this.state;
  }
  
  public long getReadDataAvailable() {
    return this.readDataAvailable;
  }
  
  public long getNumberOfMessages() {
    return this.numberOfMessages;
  }
  
  public long getMessageLength() {
    return this.messageLength;
  }
  
  public byte[] getData() {
    return this.data;
  }
  
  public void read(Buffer buffer) throws Buffer.BufferException {
    this.state = EnumWithValue.EnumUtils.<PipeState>valueOf(buffer.readUInt32(), PipeState.class, null);
    this.readDataAvailable = buffer.readUInt32();
    this.numberOfMessages = buffer.readUInt32();
    this.messageLength = buffer.readUInt32();
    this.data = buffer.readRawBytes(buffer.available());
  }
  
  public enum PipeState implements EnumWithValue<PipeState> {
    FILE_PIPE_CONNECTED_STATE(3),
    FILE_PIPE_CLOSING_STATE(4);
    
    private final int value;
    
    PipeState(int value) {
      this.value = value;
    }
    
    public long getValue() {
      return this.value;
    }
  }
}
