package com.hierynomus.msdfsc.messages;

import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;
import java.util.ArrayList;
import java.util.List;

public class DFSReferralV34 extends DFSReferral {
  private static final int SIZE = 34;
  
  DFSReferralV34() {}
  
  DFSReferralV34(int version, DFSReferral.ServerType serverType, int referralEntryFlags, int ttl, String dfsPath, String dfsAlternatePath, String path) {
    super(version, serverType, referralEntryFlags);
    this.ttl = ttl;
    this.dfsPath = dfsPath;
    this.dfsAlternatePath = dfsAlternatePath;
    this.path = path;
  }
  
  DFSReferralV34(int version, DFSReferral.ServerType serverType, int referralEntryFlags, int ttl, String specialName, List<String> expandedNames) {
    super(version, serverType, referralEntryFlags);
    this.ttl = ttl;
    this.specialName = specialName;
    this.expandedNames = expandedNames;
  }
  
  protected void readReferral(SMBBuffer buffer, int referralStartPos) throws Buffer.BufferException {
    this.ttl = buffer.readUInt32AsInt();
    if (!EnumWithValue.EnumUtils.isSet(this.referralEntryFlags, DFSReferral.ReferralEntryFlags.NameListReferral)) {
      this.dfsPath = readOffsettedString(buffer, referralStartPos, buffer.readUInt16());
      this.dfsAlternatePath = readOffsettedString(buffer, referralStartPos, buffer.readUInt16());
      this.path = readOffsettedString(buffer, referralStartPos, buffer.readUInt16());
      buffer.skip(16);
    } else {
      this.specialName = readOffsettedString(buffer, referralStartPos, buffer.readUInt16());
      int nrNames = buffer.readUInt16();
      int firstExpandedNameOffset = buffer.readUInt16();
      this.expandedNames = new ArrayList<String>(nrNames);
      int curPos = buffer.rpos();
      buffer.rpos(referralStartPos + firstExpandedNameOffset);
      for (int i = 0; i < nrNames; i++)
        this.expandedNames.add(buffer.readNullTerminatedString(StandardCharsets.UTF_16)); 
      buffer.rpos(curPos);
    } 
  }
  
  int writeReferral(SMBBuffer buffer, int entryStartPos, int bufferDataOffset) {
    int offset = bufferDataOffset;
    buffer.putUInt32(this.ttl);
    if (!EnumWithValue.EnumUtils.isSet(this.referralEntryFlags, DFSReferral.ReferralEntryFlags.NameListReferral)) {
      buffer.putUInt16(offset - entryStartPos);
      offset += (this.dfsPath.length() + 1) * 2;
      buffer.putUInt16(offset - entryStartPos);
      offset += (this.dfsAlternatePath.length() + 1) * 2;
      buffer.putUInt16(offset - entryStartPos);
      offset += (this.path.length() + 1) * 2;
      buffer.putReserved4();
      buffer.putReserved4();
      buffer.putReserved4();
      buffer.putReserved4();
      return offset;
    } 
    buffer.putUInt16(offset - entryStartPos);
    offset += (this.specialName.length() + 1) * 2;
    buffer.putUInt16(this.expandedNames.size());
    buffer.putUInt16(offset - entryStartPos);
    for (String expandedName : this.expandedNames)
      offset += (expandedName.length() + 1) * 2; 
    return offset;
  }
  
  void writeOffsettedData(SMBBuffer buffer) {
    if (!EnumWithValue.EnumUtils.isSet(this.referralEntryFlags, DFSReferral.ReferralEntryFlags.NameListReferral)) {
      buffer.putNullTerminatedString(this.dfsPath, StandardCharsets.UTF_16);
      buffer.putNullTerminatedString(this.dfsAlternatePath, StandardCharsets.UTF_16);
      buffer.putNullTerminatedString(this.path, StandardCharsets.UTF_16);
    } else {
      buffer.putNullTerminatedString(this.specialName, StandardCharsets.UTF_16);
      for (String expandedName : this.expandedNames)
        buffer.putNullTerminatedString(expandedName, StandardCharsets.UTF_16); 
    } 
  }
  
  protected int determineSize() {
    return 34;
  }
}
