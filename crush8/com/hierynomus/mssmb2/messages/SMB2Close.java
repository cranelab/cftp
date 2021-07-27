package com.hierynomus.mssmb2.messages;

import com.hierynomus.msdtyp.FileTime;
import com.hierynomus.msdtyp.MsDataTypes;
import com.hierynomus.mssmb2.SMB2Dialect;
import com.hierynomus.mssmb2.SMB2FileId;
import com.hierynomus.mssmb2.SMB2MessageCommandCode;
import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;

public class SMB2Close extends SMB2Packet {
  private SMB2FileId fileId;
  
  private FileTime creationTime;
  
  private FileTime lastAccessTime;
  
  private FileTime lastWriteTime;
  
  private FileTime changeTime;
  
  private long allocationSize;
  
  private long size;
  
  private byte[] fileAttributes;
  
  public SMB2Close() {}
  
  public SMB2Close(SMB2Dialect smbDialect, long sessionId, long treeId, SMB2FileId fileId) {
    super(24, smbDialect, SMB2MessageCommandCode.SMB2_CLOSE, sessionId, treeId);
    this.fileId = fileId;
  }
  
  protected void writeTo(SMBBuffer buffer) {
    buffer.putUInt16(this.structureSize);
    buffer.putUInt16(1);
    buffer.putReserved4();
    this.fileId.write(buffer);
  }
  
  protected void readMessage(SMBBuffer buffer) throws Buffer.BufferException {
    buffer.readUInt16();
    buffer.readUInt16();
    buffer.skip(4);
    this.creationTime = MsDataTypes.readFileTime(buffer);
    this.lastAccessTime = MsDataTypes.readFileTime(buffer);
    this.lastWriteTime = MsDataTypes.readFileTime(buffer);
    this.changeTime = MsDataTypes.readFileTime(buffer);
    this.allocationSize = buffer.readUInt64();
    this.size = buffer.readUInt64();
    this.fileAttributes = buffer.readRawBytes(4);
  }
  
  public FileTime getCreationTime() {
    return this.creationTime;
  }
  
  public FileTime getLastAccessTime() {
    return this.lastAccessTime;
  }
  
  public FileTime getLastWriteTime() {
    return this.lastWriteTime;
  }
  
  public FileTime getChangeTime() {
    return this.changeTime;
  }
  
  public long getAllocationSize() {
    return this.allocationSize;
  }
  
  public long getSize() {
    return this.size;
  }
  
  public byte[] getFileAttributes() {
    return this.fileAttributes;
  }
  
  public void setFileId(SMB2FileId fileId) {
    this.fileId = fileId;
  }
}
