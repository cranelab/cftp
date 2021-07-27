package com.hierynomus.smbj.transport;

import com.hierynomus.protocol.transport.PacketHandlers;
import com.hierynomus.protocol.transport.TransportLayer;
import com.hierynomus.smbj.SmbConfig;

public interface TransportLayerFactory<P extends com.hierynomus.protocol.Packet<?>> {
  TransportLayer<P> createTransportLayer(PacketHandlers<P> paramPacketHandlers, SmbConfig paramSmbConfig);
}
