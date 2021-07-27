package com.hierynomus.smbj.transport.tcp.tunnel;

import com.hierynomus.protocol.Packet;
import com.hierynomus.protocol.transport.PacketHandlers;
import com.hierynomus.protocol.transport.TransportLayer;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.transport.TransportLayerFactory;

public class TunnelTransportFactory<P extends Packet<?>> implements TransportLayerFactory<P> {
  private TransportLayerFactory<P> tunnelFactory;
  
  private String tunnelHost;
  
  private int tunnelPort;
  
  public TunnelTransportFactory(TransportLayerFactory<P> tunnelFactory, String tunnelHost, int tunnelPort) {
    this.tunnelFactory = tunnelFactory;
    this.tunnelHost = tunnelHost;
    this.tunnelPort = tunnelPort;
  }
  
  public TransportLayer<P> createTransportLayer(PacketHandlers<P> handlers, SmbConfig config) {
    return new TunnelTransport<P>(this.tunnelFactory.createTransportLayer(handlers, config), this.tunnelHost, this.tunnelPort);
  }
}
