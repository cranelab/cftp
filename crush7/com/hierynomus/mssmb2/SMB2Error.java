package com.hierynomus.mssmb2;

import com.hierynomus.msdfsc.messages.StandardCharsets;
import com.hierynomus.mserref.NtStatus;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;
import java.util.ArrayList;
import java.util.List;

public class SMB2Error {
  private List<SMB2ErrorData> errorData = new ArrayList<SMB2ErrorData>();
  
  SMB2Error read(SMB2Header header, SMBBuffer buffer) throws Buffer.BufferException {
    buffer.skip(2);
    int errorContextCount = buffer.readByte();
    buffer.skip(1);
    int byteCount = buffer.readUInt32AsInt();
    if (errorContextCount > 0) {
      readErrorContext(header, buffer, errorContextCount);
    } else if (byteCount > 0) {
      readErrorData(header, buffer);
    } else if (byteCount == 0) {
      if (buffer.available() > 0)
        buffer.skip(1); 
    } 
    return this;
  }
  
  private void readErrorContext(SMB2Header header, SMBBuffer buffer, int errorContextCount) throws Buffer.BufferException {
    for (int i = 0; i < errorContextCount; i++) {
      buffer.readUInt32AsInt();
      buffer.skip(4);
      readErrorData(header, buffer);
    } 
  }
  
  private void readErrorData(SMB2Header header, SMBBuffer buffer) throws Buffer.BufferException {
    if (header.getStatus() == NtStatus.STATUS_BUFFER_TOO_SMALL) {
      this.errorData.add((new BufferTooSmallError(null)).read(buffer));
    } else if (header.getStatus() == NtStatus.STATUS_STOPPED_ON_SYMLINK) {
      this.errorData.add((new SymbolicLinkError(null)).read(buffer));
    } 
  }
  
  public List<SMB2ErrorData> getErrorData() {
    return this.errorData;
  }
  
  public static class SymbolicLinkError implements SMB2ErrorData {
    private boolean absolute;
    
    private int unparsedPathLength;
    
    private String substituteName;
    
    private String printName;
    
    private SymbolicLinkError() {}
    
    private SymbolicLinkError read(SMBBuffer buffer) throws Buffer.BufferException {
      int symLinkLength = buffer.readUInt32AsInt();
      int endOfResponse = buffer.rpos() + symLinkLength;
      buffer.skip(4);
      buffer.skip(4);
      buffer.skip(2);
      this.unparsedPathLength = buffer.readUInt16();
      int substituteNameOffset = buffer.readUInt16();
      int substituteNameLength = buffer.readUInt16();
      int printNameOffset = buffer.readUInt16();
      int printNameLength = buffer.readUInt16();
      this.absolute = (buffer.readUInt32() == 0L);
      this.substituteName = readOffsettedString(buffer, substituteNameOffset, substituteNameLength);
      this.printName = readOffsettedString(buffer, printNameOffset, printNameLength);
      buffer.rpos(endOfResponse);
      return this;
    }
    
    private String readOffsettedString(SMBBuffer buffer, int offset, int length) throws Buffer.BufferException {
      int curpos = buffer.rpos();
      String s = null;
      if (length > 0) {
        buffer.rpos(curpos + offset);
        s = buffer.readString(StandardCharsets.UTF_16, length / 2);
      } 
      buffer.rpos(curpos);
      return s;
    }
    
    public boolean isAbsolute() {
      return this.absolute;
    }
    
    public int getUnparsedPathLength() {
      return this.unparsedPathLength;
    }
    
    public String getSubstituteName() {
      return this.substituteName;
    }
    
    public String getPrintName() {
      return this.printName;
    }
  }
  
  public static interface SMB2ErrorData {}
  
  public static class BufferTooSmallError implements SMB2ErrorData {
    private long requiredBufferLength;
    
    private BufferTooSmallError() {}
    
    public BufferTooSmallError read(SMBBuffer buffer) throws Buffer.BufferException {
      this.requiredBufferLength = buffer.readUInt32();
      return this;
    }
    
    public long getRequiredBufferLength() {
      return this.requiredBufferLength;
    }
  }
}
