package com.hierynomus.protocol.transport;

public interface PacketReceiver<P extends com.hierynomus.protocol.Packet<?>> {
  void handle(P paramP) throws TransportException;
  
  void handleError(Throwable paramThrowable);
}
