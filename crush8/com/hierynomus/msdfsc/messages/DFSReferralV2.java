package com.hierynomus.msdfsc.messages;

import com.hierynomus.protocol.commons.Charsets;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;

public class DFSReferralV2 extends DFSReferral {
  private static final int SIZE = 22;
  
  DFSReferralV2() {}
  
  DFSReferralV2(int version, DFSReferral.ServerType serverType, int ttl, String dfsPath, String dfsAlternatePath, String path) {
    super(version, serverType, 0);
    this.ttl = ttl;
    this.dfsPath = dfsPath;
    this.dfsAlternatePath = dfsAlternatePath;
    this.path = path;
  }
  
  protected void readReferral(SMBBuffer buffer, int referralStartPos) throws Buffer.BufferException {
    this.referralEntryFlags = 0L;
    buffer.readUInt32AsInt();
    this.ttl = buffer.readUInt32AsInt();
    int dfsPathOffset = buffer.readUInt16();
    int dfsAlternatePathOffset = buffer.readUInt16();
    int networkAddressOffset = buffer.readUInt16();
    this.dfsPath = readOffsettedString(buffer, referralStartPos, dfsPathOffset);
    this.dfsAlternatePath = readOffsettedString(buffer, referralStartPos, dfsAlternatePathOffset);
    this.path = readOffsettedString(buffer, referralStartPos, networkAddressOffset);
  }
  
  int writeReferral(SMBBuffer buffer, int entryStartPos, int bufferDataOffset) {
    int offset = bufferDataOffset;
    buffer.putUInt32(0L);
    buffer.putUInt32(this.ttl);
    buffer.putUInt16(offset - entryStartPos);
    offset += (this.dfsPath.length() + 1) * 2;
    buffer.putUInt16(offset - entryStartPos);
    offset += (this.dfsAlternatePath.length() + 1) * 2;
    buffer.putUInt16(offset - entryStartPos);
    offset += (this.path.length() + 1) * 2;
    return offset;
  }
  
  void writeOffsettedData(SMBBuffer buffer) {
    buffer.putNullTerminatedString(this.dfsPath, Charsets.UTF_16);
    buffer.putNullTerminatedString(this.dfsAlternatePath, Charsets.UTF_16);
    buffer.putNullTerminatedString(this.path, Charsets.UTF_16);
  }
  
  protected int determineSize() {
    return 22;
  }
}
