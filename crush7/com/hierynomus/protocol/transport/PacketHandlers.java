package com.hierynomus.protocol.transport;

import com.hierynomus.protocol.Packet;

public class PacketHandlers<P extends Packet<?>> {
  private final PacketSerializer<P, ?> serializer;
  
  private final PacketReceiver<P> receiver;
  
  private final PacketFactory<P> packetFactory;
  
  public PacketHandlers(PacketSerializer<P, ?> serializer, PacketReceiver<P> receiver, PacketFactory<P> packetFactory) {
    this.serializer = serializer;
    this.receiver = receiver;
    this.packetFactory = packetFactory;
  }
  
  public PacketSerializer<P, ?> getSerializer() {
    return this.serializer;
  }
  
  public PacketReceiver<P> getReceiver() {
    return this.receiver;
  }
  
  public PacketFactory<P> getPacketFactory() {
    return this.packetFactory;
  }
}
