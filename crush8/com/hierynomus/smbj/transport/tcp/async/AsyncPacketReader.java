package com.hierynomus.smbj.transport.tcp.async;

import com.hierynomus.protocol.PacketData;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.protocol.transport.PacketFactory;
import com.hierynomus.protocol.transport.PacketReceiver;
import com.hierynomus.smbj.transport.PacketReader;
import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncPacketReader<D extends PacketData<?>> {
  private static final Logger logger = LoggerFactory.getLogger(PacketReader.class);
  
  private final PacketFactory<D> packetFactory;
  
  private PacketReceiver<D> handler;
  
  private final AsynchronousSocketChannel channel;
  
  private String remoteHost;
  
  private int soTimeout = 0;
  
  private AtomicBoolean stopped = new AtomicBoolean(false);
  
  public AsyncPacketReader(AsynchronousSocketChannel channel, PacketFactory<D> packetFactory, PacketReceiver<D> handler) {
    this.channel = channel;
    this.packetFactory = packetFactory;
    this.handler = handler;
  }
  
  public void start(String remoteHost, int soTimeout) {
    this.remoteHost = remoteHost;
    this.soTimeout = soTimeout;
    initiateNextRead(new PacketBufferReader());
  }
  
  public void stop() {
    this.stopped.set(true);
  }
  
  private void initiateNextRead(PacketBufferReader bufferReader) {
    if (this.stopped.get()) {
      logger.trace("Stopped, not initiating another read operation.");
      return;
    } 
    logger.trace("Initiating next read");
    this.channel.read(bufferReader.getBuffer(), this.soTimeout, TimeUnit.MILLISECONDS, bufferReader, new CompletionHandler<Integer, PacketBufferReader>() {
          public void completed(Integer bytesRead, PacketBufferReader reader) {
            AsyncPacketReader.logger.trace("Received {} bytes", bytesRead);
            if (bytesRead.intValue() < 0) {
              handleClosedReader();
              return;
            } 
            try {
              processPackets(reader);
              AsyncPacketReader.this.initiateNextRead(reader);
            } catch (RuntimeException e) {
              AsyncPacketReader.this.handleAsyncFailure(e);
            } 
          }
          
          public void failed(Throwable exc, PacketBufferReader attachment) {
            AsyncPacketReader.this.handleAsyncFailure(exc);
          }
          
          private void processPackets(PacketBufferReader reader) {
            for (byte[] packetBytes = reader.readNext(); packetBytes != null; 
              packetBytes = reader.readNext())
              AsyncPacketReader.this.readAndHandlePacket(packetBytes); 
          }
          
          private void handleClosedReader() {
            if (!AsyncPacketReader.this.stopped.get())
              AsyncPacketReader.this.handleAsyncFailure(new EOFException("Connection closed by server")); 
          }
        });
  }
  
  private void readAndHandlePacket(byte[] packetBytes) {
    try {
      D packet = this.packetFactory.read(packetBytes);
      logger.trace("Received packet << {} >>", packet);
      this.handler.handle(packet);
    } catch (com.hierynomus.protocol.commons.buffer.Buffer.BufferException|IOException e) {
      handleAsyncFailure(e);
    } 
  }
  
  private void handleAsyncFailure(Throwable exc) {
    if (isChannelClosedByOtherParty(exc)) {
      logger.trace("Channel to {} closed by other party, closing it locally.", this.remoteHost);
    } else {
      String excClass = exc.getClass().getSimpleName();
      logger.trace("{} on channel to {}, closing channel: {}", new Object[] { excClass, this.remoteHost, exc.getMessage() });
    } 
    closeChannelQuietly();
  }
  
  private boolean isChannelClosedByOtherParty(Throwable exc) {
    return exc instanceof java.nio.channels.AsynchronousCloseException;
  }
  
  private void closeChannelQuietly() {
    try {
      this.channel.close();
    } catch (IOException e) {
      String eClass = e.getClass().getSimpleName();
      logger.debug("{} while closing channel to {} on failure: {}", new Object[] { eClass, this.remoteHost, e.getMessage() });
    } 
  }
}
