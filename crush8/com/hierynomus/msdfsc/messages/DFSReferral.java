package com.hierynomus.msdfsc.messages;

import com.hierynomus.protocol.commons.Charsets;
import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;
import java.util.List;

public abstract class DFSReferral {
  private int versionNumber;
  
  int ttl;
  
  private ServerType serverType;
  
  long referralEntryFlags;
  
  protected String path;
  
  String dfsPath;
  
  String dfsAlternatePath;
  
  String specialName;
  
  List<String> expandedNames;
  
  public enum ServerType implements EnumWithValue<ServerType> {
    LINK(0L),
    ROOT(1L);
    
    private long value;
    
    ServerType(long value) {
      this.value = value;
    }
    
    public long getValue() {
      return this.value;
    }
  }
  
  public enum ReferralEntryFlags implements EnumWithValue<ReferralEntryFlags> {
    NameListReferral(2L),
    TargetSetBoundary(4L);
    
    private long value;
    
    ReferralEntryFlags(long v) {
      this.value = v;
    }
    
    public long getValue() {
      return this.value;
    }
  }
  
  DFSReferral() {}
  
  DFSReferral(int version, ServerType serverType, int referralEntryFlags) {
    this.versionNumber = version;
    this.serverType = serverType;
    this.referralEntryFlags = referralEntryFlags;
  }
  
  public String toString() {
    return "DFSReferral[path=" + this.path + ",dfsPath=" + this.dfsPath + ",dfsAlternatePath=" + this.dfsAlternatePath + ",specialName=" + this.specialName + ",ttl=" + this.ttl + "]";
  }
  
  protected abstract void readReferral(SMBBuffer paramSMBBuffer, int paramInt) throws Buffer.BufferException;
  
  static DFSReferral factory(SMBBuffer buffer) throws Buffer.BufferException {
    int versionNumber = buffer.readUInt16();
    buffer.rpos(buffer.rpos() - 2);
    switch (versionNumber) {
      case 1:
        return (new DFSReferralV1()).read(buffer);
      case 2:
        return (new DFSReferralV2()).read(buffer);
      case 3:
      case 4:
        return (new DFSReferralV34()).read(buffer);
    } 
    throw new IllegalArgumentException("Incorrect version number " + versionNumber + " while parsing DFS Referrals");
  }
  
  String readOffsettedString(SMBBuffer buffer, int referralStart, int offset) throws Buffer.BufferException {
    int curr = buffer.rpos();
    buffer.rpos(referralStart + offset);
    String s = buffer.readNullTerminatedString(Charsets.UTF_16);
    buffer.rpos(curr);
    return s;
  }
  
  final DFSReferral read(SMBBuffer buffer) throws Buffer.BufferException {
    int start = buffer.rpos();
    this.versionNumber = buffer.readUInt16();
    int size = buffer.readUInt16();
    this.serverType = EnumWithValue.EnumUtils.<ServerType>valueOf(buffer.readUInt16(), ServerType.class, null);
    this.referralEntryFlags = buffer.readUInt16();
    readReferral(buffer, start);
    buffer.rpos(start + size);
    return this;
  }
  
  final int writeTo(SMBBuffer buffer, int bufferDataOffset) {
    int startPos = buffer.wpos();
    buffer.putUInt16(this.versionNumber);
    buffer.putUInt16(determineSize());
    buffer.putUInt16((int)this.serverType.value);
    buffer.putUInt16((int)this.referralEntryFlags);
    return writeReferral(buffer, startPos, bufferDataOffset);
  }
  
  abstract int writeReferral(SMBBuffer paramSMBBuffer, int paramInt1, int paramInt2);
  
  abstract void writeOffsettedData(SMBBuffer paramSMBBuffer);
  
  protected abstract int determineSize();
  
  public int getVersionNumber() {
    return this.versionNumber;
  }
  
  public int getTtl() {
    return this.ttl;
  }
  
  public ServerType getServerType() {
    return this.serverType;
  }
  
  public long getReferralEntryFlags() {
    return this.referralEntryFlags;
  }
  
  public String getPath() {
    return this.path;
  }
  
  public String getDfsPath() {
    return this.dfsPath;
  }
  
  public String getDfsAlternatePath() {
    return this.dfsAlternatePath;
  }
  
  public String getSpecialName() {
    return this.specialName;
  }
  
  public List<String> getExpandedNames() {
    return this.expandedNames;
  }
  
  public void setDfsPath(String dfsPath) {
    this.dfsPath = dfsPath;
  }
}
