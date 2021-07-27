package com.hierynomus.msdtyp;

import com.hierynomus.msdtyp.ace.ACE;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ACL {
  public static final byte ACL_REVISION = 2;
  
  public static final byte ACL_REVISION_DS = 4;
  
  private byte revision;
  
  private List<ACE> aces;
  
  public ACL(byte revision, List<ACE> aces) {
    this.revision = revision;
    this.aces = (aces == null) ? Collections.<ACE>emptyList() : aces;
  }
  
  public void write(SMBBuffer buffer) {
    int startPos = buffer.wpos();
    buffer.putByte(this.revision);
    buffer.putReserved1();
    int sizePos = buffer.wpos();
    buffer.wpos(sizePos + 2);
    buffer.putUInt16(this.aces.size());
    buffer.putReserved2();
    for (ACE ace : this.aces)
      ace.write(buffer); 
    int endPos = buffer.wpos();
    buffer.wpos(sizePos);
    buffer.putUInt16(endPos - startPos);
    buffer.wpos(endPos);
  }
  
  public static ACL read(SMBBuffer buffer) throws Buffer.BufferException {
    byte revision = buffer.readByte();
    buffer.skip(1);
    buffer.readUInt16();
    int aceCount = buffer.readUInt16();
    buffer.skip(2);
    List<ACE> aces = new ArrayList<>(aceCount);
    for (int i = 0; i < aceCount; i++)
      aces.add(ACE.read(buffer)); 
    return new ACL(revision, aces);
  }
  
  public byte getRevision() {
    return this.revision;
  }
  
  public void setRevision(byte revision) {
    this.revision = revision;
  }
  
  public List<ACE> getAces() {
    return this.aces;
  }
  
  public String toString() {
    return "ACL{revision=" + this.revision + ", aceCount=" + this.aces
      
      .size() + ", aces=" + this.aces + '}';
  }
}
