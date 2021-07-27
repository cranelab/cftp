package com.hierynomus.smbj.transport;

import com.hierynomus.protocol.PacketData;
import com.hierynomus.protocol.transport.PacketReceiver;
import com.hierynomus.protocol.transport.TransportException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PacketReader<D extends PacketData<?>> implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(PacketReader.class);
  
  protected InputStream in;
  
  private PacketReceiver<D> handler;
  
  private AtomicBoolean stopped = new AtomicBoolean(false);
  
  private Thread thread;
  
  public PacketReader(String host, InputStream in, PacketReceiver<D> handler) {
    this.in = in;
    this.handler = handler;
    this.thread = new Thread(this, "Packet Reader for " + host);
    this.thread.setDaemon(true);
  }
  
  public void run() {
    while (true) {
      if (!Thread.currentThread().isInterrupted() && !this.stopped.get()) {
        try {
          readPacket();
          continue;
        } catch (TransportException e) {
          if (!this.stopped.get()) {
            logger.info("PacketReader error, got exception.", e);
            this.handler.handleError(e);
            return;
          } 
        } 
      } else {
        break;
      } 
      if (this.stopped.get())
        logger.info("{} stopped.", this.thread); 
      return;
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
    D packet = doRead();
    logger.debug("Received packet {}", packet);
    this.handler.handle(packet);
  }
  
  protected abstract D doRead() throws TransportException;
  
  public void start() {
    logger.debug("Starting PacketReader on thread: {}", this.thread.getName());
    this.thread.start();
  }
}
