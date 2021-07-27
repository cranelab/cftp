package com.hierynomus.msdfsc.messages;

import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;

public class DFSReferralV1 extends DFSReferral {
  DFSReferralV1() {}
  
  DFSReferralV1(int version, DFSReferral.ServerType serverType, String path) {
    super(version, serverType, 0);
    this.path = path;
  }
  
  public void readReferral(SMBBuffer buffer, int referralStartPos) throws Buffer.BufferException {
    this.referralEntryFlags = 0L;
    this.path = buffer.readNullTerminatedString(StandardCharsets.UTF_16);
  }
  
  int writeReferral(SMBBuffer buffer, int entryStartPos, int bufferDataOffset) {
    buffer.putNullTerminatedString(this.path, StandardCharsets.UTF_16);
    return bufferDataOffset;
  }
  
  void writeOffsettedData(SMBBuffer buffer) {}
  
  protected int determineSize() {
    return 8 + (this.path.length() + 1) * 2;
  }
}
