package com.hierynomus.smbj.transport;

import com.hierynomus.protocol.transport.PacketHandlers;
import com.hierynomus.protocol.transport.TransportLayer;
import com.hierynomus.smbj.SmbConfig;

public interface TransportLayerFactory<D extends com.hierynomus.protocol.PacketData<?>, P extends com.hierynomus.protocol.Packet<?>> {
  TransportLayer<P> createTransportLayer(PacketHandlers<D, P> paramPacketHandlers, SmbConfig paramSmbConfig);
}
