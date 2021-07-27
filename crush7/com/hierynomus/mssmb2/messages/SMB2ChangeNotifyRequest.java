package com.hierynomus.mssmb2.messages;

import com.hierynomus.mssmb2.SMB2CompletionFilter;
import com.hierynomus.mssmb2.SMB2Dialect;
import com.hierynomus.mssmb2.SMB2FileId;
import com.hierynomus.mssmb2.SMB2MessageCommandCode;
import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.smb.SMBBuffer;
import java.util.Set;

public class SMB2ChangeNotifyRequest extends SMB2Packet {
  private static final int SMB2_WATCH_TREE = 1;
  
  private final Set<SMB2CompletionFilter> completionFilter;
  
  private final SMB2FileId fileId;
  
  private final long outputBufferLength;
  
  private final boolean recursive;
  
  public SMB2ChangeNotifyRequest(SMB2Dialect smbDialect, long sessionId, long treeId, SMB2FileId fileId, Set<SMB2CompletionFilter> completionFilter, long outputBufferLength, boolean recursive) {
    super(32, smbDialect, SMB2MessageCommandCode.SMB2_CHANGE_NOTIFY, sessionId, treeId);
    this.fileId = fileId;
    this.completionFilter = completionFilter;
    this.outputBufferLength = outputBufferLength;
    this.recursive = recursive;
  }
  
  protected void writeTo(SMBBuffer buffer) {
    buffer.putUInt16(this.structureSize);
    int flags = this.recursive ? 1 : 0;
    buffer.putUInt16(flags);
    buffer.putUInt32(this.outputBufferLength);
    this.fileId.write(buffer);
    buffer.putUInt32(EnumWithValue.EnumUtils.toLong(this.completionFilter));
    buffer.putReserved4();
  }
}
