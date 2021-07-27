package com.hierynomus.smbj.transport.tcp.async;

import com.hierynomus.protocol.Packet;
import com.hierynomus.protocol.PacketData;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.protocol.transport.PacketHandlers;
import com.hierynomus.protocol.transport.TransportException;
import com.hierynomus.protocol.transport.TransportLayer;
import com.hierynomus.smbj.common.SMBRuntimeException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncDirectTcpTransport<D extends PacketData<?>, P extends Packet<?>> implements TransportLayer<P> {
  private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
  
  private static final int DIRECT_HEADER_SIZE = 4;
  
  private final Logger logger = LoggerFactory.getLogger(getClass());
  
  private final PacketHandlers<D, P> handlers;
  
  private final AsynchronousSocketChannel socketChannel;
  
  private final AsyncPacketReader<D> packetReader;
  
  private final AtomicBoolean connected;
  
  private int soTimeout = 0;
  
  private final Queue<ByteBuffer> writeQueue;
  
  private AtomicBoolean writingNow;
  
  public AsyncDirectTcpTransport(int soTimeout, PacketHandlers<D, P> handlers, AsynchronousChannelGroup group) throws IOException {
    this.soTimeout = soTimeout;
    this.handlers = handlers;
    this.socketChannel = AsynchronousSocketChannel.open(group);
    this
      .packetReader = new AsyncPacketReader<>(this.socketChannel, handlers.getPacketFactory(), handlers.getReceiver());
    this.writeQueue = new LinkedBlockingQueue<>();
    this.connected = new AtomicBoolean(false);
    this.writingNow = new AtomicBoolean(false);
  }
  
  public void write(P packet) throws TransportException {
    ByteBuffer bufferToSend = prepareBufferToSend(packet);
    this.logger.trace("Sending packet << {} >>", packet);
    writeOrEnqueue(bufferToSend);
  }
  
  private void writeOrEnqueue(ByteBuffer buffer) {
    synchronized (this) {
      this.writeQueue.add(buffer);
      if (!this.writingNow.getAndSet(true))
        startAsyncWrite(); 
    } 
  }
  
  public void connect(InetSocketAddress remoteAddress) throws IOException {
    String remoteHostname = remoteAddress.getHostString();
    try {
      Future<Void> connectFuture = this.socketChannel.connect(remoteAddress);
      connectFuture.get(5000L, TimeUnit.MILLISECONDS);
      this.connected.set(true);
    } catch (ExecutionException|java.util.concurrent.TimeoutException e) {
      throw (TransportException)TransportException.Wrapper.wrap(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw (TransportException)TransportException.Wrapper.wrap(e);
    } 
    this.packetReader.start(remoteHostname, this.soTimeout);
  }
  
  public void disconnect() throws IOException {
    this.connected.set(false);
    this.socketChannel.close();
  }
  
  public boolean isConnected() {
    return this.connected.get();
  }
  
  public void setSoTimeout(int soTimeout) {
    this.soTimeout = soTimeout;
  }
  
  private void startAsyncWrite() {
    if (!isConnected())
      throw new IllegalStateException("Transport is not connected"); 
    ByteBuffer toSend = this.writeQueue.peek();
    this.socketChannel.write(toSend, this.soTimeout, TimeUnit.MILLISECONDS, (Object)null, new CompletionHandler<Integer, Object>() {
          public void completed(Integer result, Object attachment) {
            AsyncDirectTcpTransport.this.logger.trace("Written {} bytes to async transport", result);
            startNextWriteIfWaiting();
          }
          
          public void failed(Throwable exc, Object attachment) {
            try {
              if (exc instanceof java.nio.channels.ClosedChannelException) {
                AsyncDirectTcpTransport.this.connected.set(false);
              } else {
                startNextWriteIfWaiting();
              } 
            } finally {
              AsyncDirectTcpTransport.this.handlers.getReceiver().handleError(exc);
            } 
          }
          
          private void startNextWriteIfWaiting() {
            synchronized (AsyncDirectTcpTransport.this) {
              ByteBuffer head = AsyncDirectTcpTransport.this.writeQueue.peek();
              if (head != null && head.hasRemaining()) {
                AsyncDirectTcpTransport.this.startAsyncWrite();
              } else if (head != null) {
                AsyncDirectTcpTransport.this.writeQueue.remove();
                startNextWriteIfWaiting();
              } else {
                AsyncDirectTcpTransport.this.writingNow.set(false);
              } 
            } 
          }
        });
  }
  
  private ByteBuffer prepareBufferToSend(P packet) {
    Buffer<?> packetData = (Buffer<?>)this.handlers.getSerializer().write(packet);
    int dataSize = packetData.available();
    ByteBuffer toSend = ByteBuffer.allocate(dataSize + 4);
    toSend.order(ByteOrder.BIG_ENDIAN);
    toSend.putInt(packetData.available());
    toSend.put(packetData.array(), packetData.rpos(), packetData.available());
    toSend.flip();
    try {
      packetData.skip(dataSize);
    } catch (com.hierynomus.protocol.commons.buffer.Buffer.BufferException e) {
      throw (SMBRuntimeException)SMBRuntimeException.Wrapper.wrap(e);
    } 
    return toSend;
  }
}
