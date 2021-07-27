package com.hierynomus.mssmb2.messages;

import com.hierynomus.msdfsc.messages.StandardCharsets;
import com.hierynomus.mserref.NtStatus;
import com.hierynomus.msfscc.FileNotifyAction;
import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;
import java.util.ArrayList;
import java.util.List;

public class SMB2ChangeNotifyResponse extends SMB2Packet {
  List<FileNotifyInfo> fileNotifyInfoList = new ArrayList<FileNotifyInfo>();
  
  protected void readMessage(SMBBuffer buffer) throws Buffer.BufferException {
    buffer.skip(2);
    int outputBufferOffset = buffer.readUInt16();
    int length = buffer.readUInt32AsInt();
    if (outputBufferOffset > 0 && length > 0)
      this.fileNotifyInfoList = readFileNotifyInfo(buffer, outputBufferOffset); 
  }
  
  protected boolean isSuccess(NtStatus status) {
    return !(!super.isSuccess(status) && status != NtStatus.STATUS_NOTIFY_ENUM_DIR);
  }
  
  private List<FileNotifyInfo> readFileNotifyInfo(SMBBuffer buffer, int outputBufferOffset) throws Buffer.BufferException {
    int nextEntryOffset;
    List<FileNotifyInfo> notifyInfoList = new ArrayList<FileNotifyInfo>();
    buffer.rpos(outputBufferOffset);
    int currentPos = buffer.rpos();
    do {
      nextEntryOffset = (int)buffer.readUInt32();
      FileNotifyAction action = EnumWithValue.EnumUtils.<FileNotifyAction>valueOf(buffer.readUInt32(), FileNotifyAction.class, null);
      long fileNameLen = buffer.readUInt32();
      String fileName = buffer.readString(StandardCharsets.UTF_16LE, (int)fileNameLen / 2);
      notifyInfoList.add(new FileNotifyInfo(action, fileName));
      if (nextEntryOffset == 0)
        continue; 
      currentPos += nextEntryOffset;
      buffer.rpos(currentPos);
    } while (nextEntryOffset != 0);
    return notifyInfoList;
  }
  
  public List<FileNotifyInfo> getFileNotifyInfoList() {
    return this.fileNotifyInfoList;
  }
  
  public class FileNotifyInfo {
    FileNotifyAction action;
    
    String fileName;
    
    FileNotifyInfo(FileNotifyAction action, String fileName) {
      this.action = action;
      this.fileName = fileName;
    }
    
    public FileNotifyAction getAction() {
      return this.action;
    }
    
    public String getFileName() {
      return this.fileName;
    }
    
    public String toString() {
      return "FileNotifyInfo{action=" + 
        this.action + 
        ", fileName='" + this.fileName + '\'' + 
        '}';
    }
  }
}
