package com.hierynomus.mssmb2.messages;

import com.hierynomus.msdtyp.FileTime;
import com.hierynomus.msdtyp.MsDataTypes;
import com.hierynomus.mssmb2.SMB2Dialect;
import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;
import java.util.UUID;

public class SMB2NegotiateResponse extends SMB2Packet {
  private int securityMode;
  
  private SMB2Dialect dialect;
  
  private UUID serverGuid;
  
  private long capabilities;
  
  private int maxTransactSize;
  
  private int maxReadSize;
  
  private int maxWriteSize;
  
  private FileTime systemTime;
  
  private FileTime serverStartTime;
  
  private byte[] gssToken;
  
  protected void readMessage(SMBBuffer buffer) throws Buffer.BufferException {
    buffer.skip(2);
    this.securityMode = buffer.readUInt16();
    this.dialect = SMB2Dialect.lookup(buffer.readUInt16());
    int negotiateContextCount = readNegotiateContextCount(buffer);
    this.serverGuid = MsDataTypes.readGuid(buffer);
    this.capabilities = buffer.readUInt32();
    this.maxTransactSize = buffer.readUInt32AsInt();
    this.maxReadSize = buffer.readUInt32AsInt();
    this.maxWriteSize = buffer.readUInt32AsInt();
    this.systemTime = MsDataTypes.readFileTime(buffer);
    this.serverStartTime = MsDataTypes.readFileTime(buffer);
    int securityBufferOffset = buffer.readUInt16();
    int securityBufferLength = buffer.readUInt16();
    int negotiateContextOffset = readNegotiateContextOffset(buffer);
    this.gssToken = readSecurityBuffer(buffer, securityBufferOffset, securityBufferLength);
    readNegotiateContextList(buffer, negotiateContextOffset, negotiateContextCount);
  }
  
  void readNegotiateContextList(SMBBuffer buffer, int negotiateContextOffset, int negotiateContextCount) {
    if (this.dialect == SMB2Dialect.SMB_3_1_1) {
      buffer.rpos(negotiateContextOffset);
      throw new UnsupportedOperationException("Cannot read NegotiateContextList yet");
    } 
  }
  
  private byte[] readSecurityBuffer(SMBBuffer buffer, int securityBufferOffset, int securityBufferLength) throws Buffer.BufferException {
    if (securityBufferLength > 0) {
      buffer.rpos(securityBufferOffset);
      return buffer.readRawBytes(securityBufferLength);
    } 
    return new byte[0];
  }
  
  private int readNegotiateContextOffset(SMBBuffer buffer) throws Buffer.BufferException {
    if (this.dialect == SMB2Dialect.SMB_3_1_1)
      return buffer.readUInt16(); 
    buffer.skip(2);
    return 0;
  }
  
  private int readNegotiateContextCount(Buffer<?> buffer) throws Buffer.BufferException {
    if (this.dialect == SMB2Dialect.SMB_3_1_1)
      return buffer.readUInt16(); 
    buffer.skip(2);
    return 0;
  }
  
  public byte[] getGssToken() {
    return this.gssToken;
  }
  
  public int getSecurityMode() {
    return this.securityMode;
  }
  
  public SMB2Dialect getDialect() {
    return this.dialect;
  }
  
  public UUID getServerGuid() {
    return this.serverGuid;
  }
  
  public long getCapabilities() {
    return this.capabilities;
  }
  
  public int getMaxTransactSize() {
    return this.maxTransactSize;
  }
  
  public int getMaxReadSize() {
    return this.maxReadSize;
  }
  
  public int getMaxWriteSize() {
    return this.maxWriteSize;
  }
  
  public FileTime getSystemTime() {
    return this.systemTime;
  }
  
  public FileTime getServerStartTime() {
    return this.serverStartTime;
  }
}
