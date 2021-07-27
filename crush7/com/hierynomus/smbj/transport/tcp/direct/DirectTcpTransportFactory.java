package com.hierynomus.smbj.transport.tcp.direct;

import com.hierynomus.protocol.Packet;
import com.hierynomus.protocol.transport.PacketHandlers;
import com.hierynomus.protocol.transport.TransportLayer;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.transport.TransportLayerFactory;

public class DirectTcpTransportFactory<P extends Packet<?>> implements TransportLayerFactory<P> {
  public TransportLayer<P> createTransportLayer(PacketHandlers<P> handlers, SmbConfig config) {
    return new DirectTcpTransport<P>(config.getSocketFactory(), config.getSoTimeout(), handlers);
  }
}
