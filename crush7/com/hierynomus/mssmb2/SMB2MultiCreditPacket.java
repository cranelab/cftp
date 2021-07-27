package com.hierynomus.mssmb2;

public class SMB2MultiCreditPacket extends SMB2Packet {
  private int maxPayloadSize;
  
  public SMB2MultiCreditPacket(int structureSize, SMB2Dialect dialect, SMB2MessageCommandCode messageType, long sessionId, long treeId, int maxPayloadSize) {
    super(structureSize, dialect, messageType, sessionId, treeId);
    this.maxPayloadSize = maxPayloadSize;
  }
  
  public int getMaxPayloadSize() {
    return this.maxPayloadSize;
  }
  
  protected int getPayloadSize() {
    return Math.min(this.maxPayloadSize, 65536 * getCreditsAssigned());
  }
}
