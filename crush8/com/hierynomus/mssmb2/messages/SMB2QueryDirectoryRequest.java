package com.hierynomus.mssmb2.messages;

import com.hierynomus.msfscc.FileInformationClass;
import com.hierynomus.mssmb2.SMB2Dialect;
import com.hierynomus.mssmb2.SMB2FileId;
import com.hierynomus.mssmb2.SMB2MessageCommandCode;
import com.hierynomus.mssmb2.SMB2MultiCreditPacket;
import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.smb.SMBBuffer;
import java.util.Set;

public class SMB2QueryDirectoryRequest extends SMB2MultiCreditPacket {
  private FileInformationClass fileInformationClass;
  
  private final Set<SMB2QueryDirectoryFlags> flags;
  
  private final long fileIndex;
  
  private final SMB2FileId fileId;
  
  private final String searchPattern;
  
  public SMB2QueryDirectoryRequest(SMB2Dialect smbDialect, long sessionId, long treeId, SMB2FileId fileId, FileInformationClass fileInformationClass, Set<SMB2QueryDirectoryFlags> flags, long fileIndex, String searchPattern, int maxBufferSize) {
    super(33, smbDialect, SMB2MessageCommandCode.SMB2_QUERY_DIRECTORY, sessionId, treeId, maxBufferSize);
    this.fileInformationClass = fileInformationClass;
    this.flags = flags;
    this.fileIndex = fileIndex;
    this.fileId = fileId;
    this.searchPattern = (searchPattern == null) ? "*" : searchPattern;
  }
  
  protected void writeTo(SMBBuffer buffer) {
    buffer.putUInt16(this.structureSize);
    buffer.putByte((byte)(int)this.fileInformationClass.getValue());
    buffer.putByte((byte)(int)EnumWithValue.EnumUtils.<SMB2QueryDirectoryFlags>toLong(this.flags));
    buffer.putUInt32(this.fileIndex);
    this.fileId.write(buffer);
    int offset = 96;
    buffer.putUInt16(offset);
    buffer.putUInt16(this.searchPattern.length() * 2);
    buffer.putUInt32(Math.min(getMaxPayloadSize(), 65536 * getCreditsAssigned()));
    buffer.putString(this.searchPattern);
  }
  
  public enum SMB2QueryDirectoryFlags implements EnumWithValue<SMB2QueryDirectoryFlags> {
    SMB2_RESTART_SCANS(1L),
    SMB2_RETURN_SINGLE_ENTRY(2L),
    SMB2_INDEX_SPECIFIED(4L),
    SMB2_REOPEN(16L);
    
    private long value;
    
    SMB2QueryDirectoryFlags(long value) {
      this.value = value;
    }
    
    public long getValue() {
      return this.value;
    }
  }
}
