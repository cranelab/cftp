package com.hierynomus.mssmb2.messages;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2Dialect;
import com.hierynomus.mssmb2.SMB2Functions;
import com.hierynomus.mssmb2.SMB2ImpersonationLevel;
import com.hierynomus.mssmb2.SMB2MessageCommandCode;
import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.smb.SMBBuffer;
import java.util.Set;

public class SMB2CreateRequest extends SMB2Packet {
  private final Set<FileAttributes> fileAttributes;
  
  private final Set<SMB2ShareAccess> shareAccess;
  
  private final SMB2CreateDisposition createDisposition;
  
  private final Set<SMB2CreateOptions> createOptions;
  
  private final String fileName;
  
  private final Set<AccessMask> accessMask;
  
  private final SMB2ImpersonationLevel impersonationLevel;
  
  public SMB2CreateRequest(SMB2Dialect smbDialect, long sessionId, long treeId, SMB2ImpersonationLevel impersonationLevel, Set<AccessMask> accessMask, Set<FileAttributes> fileAttributes, Set<SMB2ShareAccess> shareAccess, SMB2CreateDisposition createDisposition, Set<SMB2CreateOptions> createOptions, String fileName) {
    super(57, smbDialect, SMB2MessageCommandCode.SMB2_CREATE, sessionId, treeId);
    this.impersonationLevel = EnumWithValue.EnumUtils.<SMB2ImpersonationLevel>ensureNotNull(impersonationLevel, SMB2ImpersonationLevel.Identification);
    this.accessMask = accessMask;
    this.fileAttributes = EnumWithValue.EnumUtils.ensureNotNull(fileAttributes, FileAttributes.class);
    this.shareAccess = EnumWithValue.EnumUtils.ensureNotNull(shareAccess, SMB2ShareAccess.class);
    this.createDisposition = EnumWithValue.EnumUtils.<SMB2CreateDisposition>ensureNotNull(createDisposition, SMB2CreateDisposition.FILE_SUPERSEDE);
    this.createOptions = EnumWithValue.EnumUtils.ensureNotNull(createOptions, SMB2CreateOptions.class);
    this.fileName = fileName;
  }
  
  protected void writeTo(SMBBuffer buffer) {
    byte[] nameBytes;
    buffer.putUInt16(this.structureSize);
    buffer.putByte((byte)0);
    buffer.putByte((byte)0);
    buffer.putUInt32(this.impersonationLevel.getValue());
    buffer.putReserved(8);
    buffer.putReserved(8);
    buffer.putUInt32(EnumWithValue.EnumUtils.toLong(this.accessMask));
    buffer.putUInt32(EnumWithValue.EnumUtils.toLong(this.fileAttributes));
    buffer.putUInt32(EnumWithValue.EnumUtils.toLong(this.shareAccess));
    buffer.putUInt32(this.createDisposition.getValue());
    buffer.putUInt32(EnumWithValue.EnumUtils.toLong(this.createOptions));
    int offset = 64 + this.structureSize - 1;
    if (this.fileName == null || this.fileName.trim().length() == 0) {
      buffer.putUInt16(offset);
      buffer.putUInt16(0);
      nameBytes = new byte[1];
    } else {
      nameBytes = SMB2Functions.unicode(this.fileName);
      buffer.putUInt16(offset);
      buffer.putUInt16(nameBytes.length);
    } 
    buffer.putUInt32(0L);
    buffer.putUInt32(0L);
    buffer.putRawBytes(nameBytes);
  }
  
  public String getFileName() {
    return this.fileName;
  }
}
