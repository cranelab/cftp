package com.hierynomus.protocol.transport;

import java.io.IOException;
import java.net.InetSocketAddress;

public interface TransportLayer<P extends com.hierynomus.protocol.Packet<?>> {
  void write(P paramP) throws TransportException;
  
  void connect(InetSocketAddress paramInetSocketAddress) throws IOException;
  
  void disconnect() throws IOException;
  
  boolean isConnected();
}
