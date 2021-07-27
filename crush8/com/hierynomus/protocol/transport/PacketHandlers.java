package com.hierynomus.protocol.transport;

import com.hierynomus.protocol.Packet;
import com.hierynomus.protocol.PacketData;

public class PacketHandlers<D extends PacketData<?>, P extends Packet<?>> {
  private final PacketSerializer<P, ?> serializer;
  
  private final PacketReceiver<D> receiver;
  
  private final PacketFactory<D> packetFactory;
  
  public PacketHandlers(PacketSerializer<P, ?> serializer, PacketReceiver<D> receiver, PacketFactory<D> packetFactory) {
    this.serializer = serializer;
    this.receiver = receiver;
    this.packetFactory = packetFactory;
  }
  
  public PacketSerializer<P, ?> getSerializer() {
    return this.serializer;
  }
  
  public PacketReceiver<D> getReceiver() {
    return this.receiver;
  }
  
  public PacketFactory<D> getPacketFactory() {
    return this.packetFactory;
  }
}
