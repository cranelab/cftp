package com.hierynomus.protocol.transport;

public interface PacketReceiver<D extends com.hierynomus.protocol.PacketData<?>> {
  void handle(D paramD) throws TransportException;
  
  void handleError(Throwable paramThrowable);
}
