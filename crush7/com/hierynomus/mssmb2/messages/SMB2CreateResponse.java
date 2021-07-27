package com.hierynomus.mssmb2.messages;

import com.hierynomus.msdtyp.FileTime;
import com.hierynomus.msdtyp.MsDataTypes;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.mssmb2.SMB2FileId;
import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;
import java.util.Set;

public class SMB2CreateResponse extends SMB2Packet {
  private FileTime creationTime;
  
  private FileTime lastAccessTime;
  
  private FileTime lastWriteTime;
  
  private FileTime changeTime;
  
  private Set<FileAttributes> fileAttributes;
  
  private SMB2FileId fileId;
  
  protected void readMessage(SMBBuffer buffer) throws Buffer.BufferException {
    buffer.readUInt16();
    buffer.readByte();
    buffer.readByte();
    buffer.readUInt32();
    this.creationTime = MsDataTypes.readFileTime(buffer);
    this.lastAccessTime = MsDataTypes.readFileTime(buffer);
    this.lastWriteTime = MsDataTypes.readFileTime(buffer);
    this.changeTime = MsDataTypes.readFileTime(buffer);
    buffer.readRawBytes(8);
    buffer.readRawBytes(8);
    this.fileAttributes = EnumWithValue.EnumUtils.toEnumSet(buffer.readUInt32(), FileAttributes.class);
    buffer.skip(4);
    this.fileId = SMB2FileId.read(buffer);
    buffer.readUInt32();
    buffer.readUInt32();
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
  
  public Set<FileAttributes> getFileAttributes() {
    return this.fileAttributes;
  }
  
  public SMB2FileId getFileId() {
    return this.fileId;
  }
}
