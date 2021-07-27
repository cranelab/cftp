package com.hierynomus.smbj.transport.tcp.async;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PacketBufferReader {
  private static final int NO_PACKET_LENGTH = -1;
  
  private static final int HEADER_SIZE = 4;
  
  private static final int READ_BUFFER_CAPACITY = 9000;
  
  private final ByteBuffer readBuffer;
  
  private int currentPacketLength = -1;
  
  public PacketBufferReader() {
    this.readBuffer = ByteBuffer.allocate(9000);
    this.readBuffer.order(ByteOrder.BIG_ENDIAN);
  }
  
  public byte[] readNext() {
    byte[] result;
    this.readBuffer.flip();
    if (isAwaitingHeader()) {
      result = readPacketHeaderAndBody();
    } else {
      result = readPacketBody();
    } 
    this.readBuffer.compact();
    return result;
  }
  
  public ByteBuffer getBuffer() {
    return this.readBuffer;
  }
  
  private boolean isAwaitingHeader() {
    return (this.currentPacketLength == -1);
  }
  
  private byte[] readPacketHeaderAndBody() {
    if (!ensureBytesAvailable(4))
      return null; 
    this.currentPacketLength = this.readBuffer.getInt() & 0xFFFFFF;
    return readPacketBody();
  }
  
  private byte[] readPacketBody() {
    if (!ensureBytesAvailable(this.currentPacketLength))
      return null; 
    byte[] buf = new byte[this.currentPacketLength];
    this.readBuffer.get(buf);
    this.currentPacketLength = -1;
    return buf;
  }
  
  private boolean ensureBytesAvailable(int bytesNeeded) {
    return (this.readBuffer.remaining() >= bytesNeeded);
  }
}
