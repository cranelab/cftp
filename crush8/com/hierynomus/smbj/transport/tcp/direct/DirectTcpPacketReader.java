package com.hierynomus.smbj.transport.tcp.direct;

import com.hierynomus.protocol.PacketData;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.protocol.commons.buffer.Endian;
import com.hierynomus.protocol.transport.PacketFactory;
import com.hierynomus.protocol.transport.PacketReceiver;
import com.hierynomus.protocol.transport.TransportException;
import com.hierynomus.smbj.transport.PacketReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class DirectTcpPacketReader<D extends PacketData<?>> extends PacketReader<D> {
  private final PacketFactory<D> packetFactory;
  
  public DirectTcpPacketReader(String host, InputStream in, PacketFactory<D> packetFactory, PacketReceiver<D> handler) {
    super(host, in, handler);
    this.packetFactory = packetFactory;
  }
  
  private D readPacket(int packetLength) throws IOException, Buffer.BufferException {
    byte[] buf = new byte[packetLength];
    readFully(buf);
    return this.packetFactory.read(buf);
  }
  
  protected D doRead() throws TransportException {
    try {
      int packetLength = readTcpHeader();
      return readPacket(packetLength);
    } catch (TransportException e) {
      throw e;
    } catch (IOException|com.hierynomus.protocol.commons.buffer.Buffer.BufferException e) {
      throw new TransportException(e);
    } 
  }
  
  private int readTcpHeader() throws IOException, Buffer.BufferException {
    byte[] tcpHeader = new byte[4];
    readFully(tcpHeader);
    Buffer.PlainBuffer plainBuffer = new Buffer.PlainBuffer(tcpHeader, Endian.BE);
    plainBuffer.readByte();
    int packetLength = plainBuffer.readUInt24();
    return packetLength;
  }
  
  private void readFully(byte[] buffer) throws IOException {
    int toRead = buffer.length;
    int offset = 0;
    while (toRead > 0) {
      int bytesRead = this.in.read(buffer, offset, toRead);
      if (bytesRead == -1)
        throw new TransportException(new EOFException("EOF while reading packet")); 
      toRead -= bytesRead;
      offset += bytesRead;
    } 
  }
}
