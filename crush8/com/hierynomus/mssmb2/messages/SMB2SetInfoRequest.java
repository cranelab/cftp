package com.hierynomus.mssmb2.messages;

import com.hierynomus.msdtyp.SecurityInformation;
import com.hierynomus.msfscc.FileInformationClass;
import com.hierynomus.mssmb2.SMB2Dialect;
import com.hierynomus.mssmb2.SMB2FileId;
import com.hierynomus.mssmb2.SMB2MessageCommandCode;
import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.smb.SMBBuffer;
import java.util.Set;

public class SMB2SetInfoRequest extends SMB2Packet {
  private final SMB2FileId fileId;
  
  private final SMB2InfoType infoType;
  
  private final FileInformationClass fileInfoClass;
  
  private final byte[] buffer;
  
  private final Set<SecurityInformation> securityInformation;
  
  public SMB2SetInfoRequest(SMB2Dialect negotiatedDialect, long sessionId, long treeId, SMB2InfoType infoType, SMB2FileId fileId, FileInformationClass fileInfoClass, Set<SecurityInformation> securityInformation, byte[] buffer) {
    super(33, negotiatedDialect, SMB2MessageCommandCode.SMB2_SET_INFO, sessionId, treeId);
    this.fileId = fileId;
    this.infoType = infoType;
    this.fileInfoClass = fileInfoClass;
    this.buffer = (buffer == null) ? new byte[0] : buffer;
    this.securityInformation = securityInformation;
  }
  
  protected void writeTo(SMBBuffer smbBuffer) {
    smbBuffer.putUInt16(this.structureSize);
    smbBuffer.putByte((byte)(int)this.infoType.getValue());
    smbBuffer.putByte((this.fileInfoClass == null) ? 0 : (byte)(int)this.fileInfoClass.getValue());
    int offset = 96;
    smbBuffer.putUInt32(this.buffer.length);
    smbBuffer.putUInt16(offset);
    smbBuffer.putReserved2();
    smbBuffer.putUInt32((this.securityInformation == null) ? 0L : EnumWithValue.EnumUtils.<SecurityInformation>toLong(this.securityInformation));
    this.fileId.write(smbBuffer);
    smbBuffer.putRawBytes(this.buffer);
  }
  
  public enum SMB2InfoType implements EnumWithValue<SMB2InfoType> {
    SMB2_0_INFO_FILE(1L),
    SMB2_0_INFO_FILESYSTEM(2L),
    SMB2_0_INFO_SECURITY(3L),
    SMB2_0_INFO_QUOTA(4L);
    
    private long value;
    
    SMB2InfoType(long value) {
      this.value = value;
    }
    
    public long getValue() {
      return this.value;
    }
  }
}
