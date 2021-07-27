package com.hierynomus.smbj.transport.tcp.direct;

import com.hierynomus.protocol.Packet;
import com.hierynomus.protocol.PacketData;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.protocol.commons.socket.ProxySocketFactory;
import com.hierynomus.protocol.transport.PacketHandlers;
import com.hierynomus.protocol.transport.TransportException;
import com.hierynomus.protocol.transport.TransportLayer;
import com.hierynomus.smbj.transport.PacketReader;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;
import javax.net.SocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectTcpTransport<D extends PacketData<?>, P extends Packet<?>> implements TransportLayer<P> {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  
  private final PacketHandlers<D, P> handlers;
  
  private final ReentrantLock writeLock = new ReentrantLock();
  
  private SocketFactory socketFactory = new ProxySocketFactory();
  
  private int soTimeout;
  
  private Socket socket;
  
  private BufferedOutputStream output;
  
  private PacketReader<D> packetReaderThread;
  
  private static final int INITIAL_BUFFER_SIZE = 9000;
  
  public DirectTcpTransport(SocketFactory socketFactory, int soTimeout, PacketHandlers<D, P> handlers) {
    this.soTimeout = soTimeout;
    this.socketFactory = socketFactory;
    this.handlers = handlers;
  }
  
  public void write(P packet) throws TransportException {
    this.logger.trace("Acquiring write lock to send packet << {} >>", packet);
    this.writeLock.lock();
    try {
      if (!isConnected())
        throw new TransportException(String.format("Cannot write %s as transport is disconnected", new Object[] { packet })); 
      try {
        this.logger.debug("Writing packet {}", packet);
        Buffer<?> packetData = (Buffer<?>)this.handlers.getSerializer().write(packet);
        writeDirectTcpPacketHeader(packetData.available());
        writePacketData(packetData);
        this.output.flush();
        this.logger.trace("Packet {} sent, lock released.", packet);
      } catch (IOException ioe) {
        throw new TransportException(ioe);
      } 
    } finally {
      this.writeLock.unlock();
    } 
  }
  
  public void connect(InetSocketAddress remoteAddress) throws IOException {
    String remoteHostname = remoteAddress.getHostString();
    this.socket = this.socketFactory.createSocket(remoteHostname, remoteAddress.getPort());
    initWithSocket(remoteHostname);
  }
  
  private void initWithSocket(String remoteHostname) throws IOException {
    this.socket.setSoTimeout(this.soTimeout);
    this.output = new BufferedOutputStream(this.socket.getOutputStream(), 9000);
    this.packetReaderThread = new DirectTcpPacketReader<>(remoteHostname, this.socket.getInputStream(), this.handlers.getPacketFactory(), this.handlers.getReceiver());
    this.packetReaderThread.start();
  }
  
  public void disconnect() throws IOException {
    this.writeLock.lock();
    try {
      if (!isConnected())
        return; 
      this.packetReaderThread.stop();
      if (this.socket.getInputStream() != null)
        this.socket.getInputStream().close(); 
      if (this.output != null) {
        this.output.close();
        this.output = null;
      } 
      if (this.socket != null) {
        this.socket.close();
        this.socket = null;
      } 
    } finally {
      this.writeLock.unlock();
    } 
  }
  
  public boolean isConnected() {
    return (this.socket != null && this.socket.isConnected() && !this.socket.isClosed());
  }
  
  public void setSocketFactory(SocketFactory socketFactory) {
    this.socketFactory = socketFactory;
  }
  
  public void setSoTimeout(int soTimeout) {
    this.soTimeout = soTimeout;
  }
  
  private void writePacketData(Buffer<?> packetData) throws IOException {
    this.output.write(packetData.array(), packetData.rpos(), packetData.available());
  }
  
  private void writeDirectTcpPacketHeader(int size) throws IOException {
    this.output.write(0);
    this.output.write((byte)(size >> 16));
    this.output.write((byte)(size >> 8));
    this.output.write((byte)(size & 0xFF));
  }
}
