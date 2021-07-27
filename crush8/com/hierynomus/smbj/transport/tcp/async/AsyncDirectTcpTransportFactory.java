package com.hierynomus.smbj.transport.tcp.async;

import com.hierynomus.protocol.Packet;
import com.hierynomus.protocol.PacketData;
import com.hierynomus.protocol.transport.PacketHandlers;
import com.hierynomus.protocol.transport.TransportLayer;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.common.SMBRuntimeException;
import com.hierynomus.smbj.transport.TransportLayerFactory;
import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutorService;

public class AsyncDirectTcpTransportFactory<D extends PacketData<?>, P extends Packet<?>> implements TransportLayerFactory<D, P> {
  private static final AsynchronousChannelGroup DEFAULT_CHANNEL_GROUP = null;
  
  private final AsynchronousChannelGroup group;
  
  public TransportLayer<P> createTransportLayer(PacketHandlers<D, P> handlers, SmbConfig config) {
    try {
      return new AsyncDirectTcpTransport<>(config.getSoTimeout(), handlers, this.group);
    } catch (IOException e) {
      throw new SMBRuntimeException(e);
    } 
  }
  
  public AsyncDirectTcpTransportFactory() {
    this(DEFAULT_CHANNEL_GROUP);
  }
  
  public AsyncDirectTcpTransportFactory(ExecutorService executor) {
    this(createGroup(executor));
  }
  
  public AsyncDirectTcpTransportFactory(AsynchronousChannelGroup group) {
    this.group = group;
  }
  
  private static AsynchronousChannelGroup createGroup(ExecutorService executor) {
    try {
      return AsynchronousChannelGroup.withThreadPool(executor);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } 
  }
}
