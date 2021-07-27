package com.hierynomus.mssmb2;

import com.hierynomus.mserref.NtStatus;
import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;
import com.hierynomus.smb.SMBHeader;

public class SMB2Header implements SMBHeader {
  public static final byte[] EMPTY_SIGNATURE = new byte[16];
  
  public static final int STRUCTURE_SIZE = 64;
  
  public static final int SIGNATURE_OFFSET = 48;
  
  public static final int SIGNATURE_SIZE = 16;
  
  private SMB2Dialect dialect;
  
  private int creditCharge = 1;
  
  private int creditRequest;
  
  private int creditResponse;
  
  private SMB2MessageCommandCode message;
  
  private long messageId;
  
  private long asyncId;
  
  private long sessionId;
  
  private long treeId;
  
  private NtStatus status;
  
  private long statusCode;
  
  private long flags;
  
  private long nextCommandOffset;
  
  private byte[] signature;
  
  public void writeTo(SMBBuffer buffer) {
    buffer.putRawBytes(new byte[] { -2, 83, 77, 66 });
    buffer.putUInt16(64);
    writeCreditCharge(buffer);
    writeChannelSequenceReserved(buffer);
    buffer.putUInt16(this.message.getValue());
    writeCreditRequest(buffer);
    buffer.putUInt32(this.flags);
    buffer.putUInt32(this.nextCommandOffset);
    buffer.putLong(this.messageId);
    if (EnumWithValue.EnumUtils.isSet(this.flags, SMB2MessageFlag.SMB2_FLAGS_ASYNC_COMMAND)) {
      buffer.putLong(this.asyncId);
    } else {
      buffer.putReserved4();
      buffer.putUInt32(this.treeId);
    } 
    buffer.putLong(this.sessionId);
    buffer.putRawBytes(EMPTY_SIGNATURE);
  }
  
  private void writeChannelSequenceReserved(SMBBuffer buffer) {
    if (this.dialect.isSmb3x()) {
      buffer.putRawBytes(new byte[2]);
      buffer.putReserved(2);
      throw new UnsupportedOperationException("SMB 3.x not yet implemented");
    } 
    buffer.putReserved4();
  }
  
  private void writeCreditRequest(SMBBuffer buffer) {
    buffer.putUInt16(this.creditRequest + this.creditCharge);
  }
  
  private void writeCreditCharge(SMBBuffer buffer) {
    switch (this.dialect) {
      case UNKNOWN:
      case SMB_2_0_2:
        buffer.putReserved(2);
        return;
    } 
    buffer.putUInt16(this.creditCharge);
  }
  
  public void setMessageId(long messageId) {
    this.messageId = messageId;
  }
  
  void setMessageType(SMB2MessageCommandCode messageType) {
    this.message = messageType;
  }
  
  public SMB2MessageCommandCode getMessage() {
    return this.message;
  }
  
  public long getTreeId() {
    return this.treeId;
  }
  
  public void setTreeId(long treeId) {
    this.treeId = treeId;
  }
  
  public long getSessionId() {
    return this.sessionId;
  }
  
  public void setSessionId(long sessionId) {
    this.sessionId = sessionId;
  }
  
  public void setDialect(SMB2Dialect dialect) {
    this.dialect = dialect;
  }
  
  public boolean isFlagSet(SMB2MessageFlag flag) {
    return EnumWithValue.EnumUtils.isSet(this.flags, flag);
  }
  
  public void setFlag(SMB2MessageFlag flag) {
    this.flags |= flag.getValue();
  }
  
  public long getMessageId() {
    return this.messageId;
  }
  
  public void setCreditRequest(int creditRequest) {
    this.creditRequest = creditRequest;
  }
  
  public int getCreditResponse() {
    return this.creditResponse;
  }
  
  public void setAsyncId(long asyncId) {
    this.asyncId = asyncId;
  }
  
  public long getAsyncId() {
    return this.asyncId;
  }
  
  public void readFrom(Buffer<?> buffer) throws Buffer.BufferException {
    buffer.skip(4);
    buffer.skip(2);
    buffer.readUInt16();
    this.statusCode = buffer.readUInt32();
    this.status = EnumWithValue.EnumUtils.<NtStatus>valueOf(this.statusCode, NtStatus.class, NtStatus.UNKNOWN);
    this.message = SMB2MessageCommandCode.lookup(buffer.readUInt16());
    this.creditResponse = buffer.readUInt16();
    this.flags = buffer.readUInt32();
    this.nextCommandOffset = buffer.readUInt32();
    this.messageId = buffer.readLong();
    if (EnumWithValue.EnumUtils.isSet(this.flags, SMB2MessageFlag.SMB2_FLAGS_ASYNC_COMMAND)) {
      this.asyncId = buffer.readLong();
    } else {
      buffer.skip(4);
      this.treeId = buffer.readUInt32();
    } 
    this.sessionId = buffer.readLong();
    this.signature = buffer.readRawBytes(16);
  }
  
  public void setStatus(NtStatus status) {
    this.status = status;
  }
  
  public NtStatus getStatus() {
    return this.status;
  }
  
  public long getStatusCode() {
    return this.statusCode;
  }
  
  public long getFlags() {
    return this.flags;
  }
  
  public void setFlags(long flags) {
    this.flags = flags;
  }
  
  public long getNextCommandOffset() {
    return this.nextCommandOffset;
  }
  
  public void setNextCommandOffset(long nextCommandOffset) {
    this.nextCommandOffset = nextCommandOffset;
  }
  
  public void setCreditCharge(int creditCharge) {
    this.creditCharge = creditCharge;
  }
  
  public String toString() {
    return String.format(
        "dialect=%s, creditCharge=%s, creditRequest=%s, creditResponse=%s, message=%s, messageId=%s, asyncId=%s, sessionId=%s, treeId=%s, status=%s, statusCode=%s, flags=%s, nextCommandOffset=%s", new Object[] { 
          this.dialect, 
          Integer.valueOf(this.creditCharge), 
          Integer.valueOf(this.creditRequest), 
          Integer.valueOf(this.creditResponse), 
          this.message, 
          Long.valueOf(this.messageId), 
          Long.valueOf(this.asyncId), 
          Long.valueOf(this.sessionId), 
          Long.valueOf(this.treeId), 
          this.status, 
          Long.valueOf(this.statusCode), 
          Long.valueOf(this.flags), 
          Long.valueOf(this.nextCommandOffset) });
  }
  
  public int getCreditCharge() {
    return this.creditCharge;
  }
  
  public byte[] getSignature() {
    return this.signature;
  }
}
