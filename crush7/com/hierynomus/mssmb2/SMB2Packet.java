package com.hierynomus.mssmb2;

import com.hierynomus.mserref.NtStatus;
import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;
import com.hierynomus.smb.SMBPacket;

public class SMB2Packet extends SMBPacket<SMB2Header> {
  public static final int SINGLE_CREDIT_PAYLOAD_SIZE = 65536;
  
  protected int structureSize;
  
  private SMBBuffer buffer;
  
  private SMB2Error error;
  
  private int messageStartPos;
  
  private int messageEndPos;
  
  protected SMB2Packet() {
    super(new SMB2Header());
  }
  
  protected SMB2Packet(int structureSize, SMB2Dialect dialect, SMB2MessageCommandCode messageType) {
    this(structureSize, dialect, messageType, 0L, 0L);
  }
  
  protected SMB2Packet(int structureSize, SMB2Dialect dialect, SMB2MessageCommandCode messageType, long sessionId) {
    this(structureSize, dialect, messageType, sessionId, 0L);
  }
  
  protected SMB2Packet(int structureSize, SMB2Dialect dialect, SMB2MessageCommandCode messageType, long sessionId, long treeId) {
    super(new SMB2Header());
    this.structureSize = structureSize;
    this.header.setDialect(dialect);
    this.header.setMessageType(messageType);
    this.header.setSessionId(sessionId);
    this.header.setTreeId(treeId);
  }
  
  public long getSequenceNumber() {
    return this.header.getMessageId();
  }
  
  public int getStructureSize() {
    return this.structureSize;
  }
  
  public SMBBuffer getBuffer() {
    return this.buffer;
  }
  
  public int getMessageStartPos() {
    return this.messageStartPos;
  }
  
  public int getMessageEndPos() {
    return this.messageEndPos;
  }
  
  public void write(SMBBuffer buffer) {
    this.header.writeTo(buffer);
    writeTo(buffer);
  }
  
  protected void writeTo(SMBBuffer buffer) {
    throw new UnsupportedOperationException("Should be implemented by specific message type");
  }
  
  public final void read(SMBBuffer buffer) throws Buffer.BufferException {
    this.buffer = buffer;
    this.messageStartPos = buffer.rpos();
    this.header.readFrom(buffer);
    if (isSuccess(this.header.getStatus())) {
      readMessage(buffer);
    } else {
      readError(buffer);
    } 
    this.messageEndPos = buffer.rpos();
  }
  
  protected void readError(SMBBuffer buffer) throws Buffer.BufferException {
    this.error = (new SMB2Error()).read(this.header, buffer);
  }
  
  protected void readMessage(SMBBuffer buffer) throws Buffer.BufferException {
    throw new UnsupportedOperationException("Should be implemented by specific message type");
  }
  
  protected boolean isSuccess(NtStatus status) {
    return (status.isSuccess() && status != NtStatus.STATUS_PENDING);
  }
  
  public boolean isIntermediateAsyncResponse() {
    return (EnumWithValue.EnumUtils.isSet(this.header.getFlags(), SMB2MessageFlag.SMB2_FLAGS_ASYNC_COMMAND) && this.header.getStatus() == NtStatus.STATUS_PENDING);
  }
  
  public int getMaxPayloadSize() {
    return 65536;
  }
  
  public int getCreditsAssigned() {
    return getHeader().getCreditCharge();
  }
  
  public void setCreditsAssigned(int creditsAssigned) {
    getHeader().setCreditCharge(creditsAssigned);
  }
  
  public SMB2Error getError() {
    return this.error;
  }
  
  public String toString() {
    return this.header.getMessage() + " with message id << " + this.header.getMessageId() + " >>";
  }
}
