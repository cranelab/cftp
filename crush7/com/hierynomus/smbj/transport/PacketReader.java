package com.hierynomus.smbj.transport;

import com.hierynomus.protocol.Packet;
import com.hierynomus.protocol.transport.PacketReceiver;
import com.hierynomus.protocol.transport.TransportException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PacketReader<P extends Packet<?>> implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(PacketReader.class);
  
  protected InputStream in;
  
  private PacketReceiver<P> handler;
  
  private AtomicBoolean stopped = new AtomicBoolean(false);
  
  private Thread thread;
  
  public PacketReader(String host, InputStream in, PacketReceiver<P> handler) {
    this.in = in;
    this.handler = handler;
    this.thread = new Thread(this, "Packet Reader for " + host);
    this.thread.setDaemon(true);
  }
  
  public void run() {
    while (!Thread.currentThread().isInterrupted() && !this.stopped.get()) {
      try {
        readPacket();
      } catch (TransportException e) {
        if (this.stopped.get())
          break; 
        logger.info("PacketReader error, got exception.", e);
        this.handler.handleError(e);
        return;
      } 
    } 
    if (this.stopped.get())
      logger.info("{} stopped.", this.thread); 
  }
  
  public void stop() {
    logger.debug("Stopping PacketReader...");
    this.stopped.set(true);
    this.thread.interrupt();
  }
  
  private void readPacket() throws TransportException {
    P packet = doRead();
    logger.debug("Received packet {}", packet);
    this.handler.handle(packet);
  }
  
  protected abstract P doRead() throws TransportException;
  
  public void start() {
    logger.debug("Starting PacketReader on thread: {}", this.thread.getName());
    this.thread.start();
  }
}
