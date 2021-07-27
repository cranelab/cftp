package com.hierynomus.msdtyp.ace;

import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;
import java.util.Set;

public class AceHeader {
  private AceType aceType;
  
  private Set<AceFlags> aceFlags;
  
  private int aceSize;
  
  AceHeader() {}
  
  AceHeader(AceType aceType, Set<AceFlags> aceFlags) {
    this.aceType = aceType;
    this.aceFlags = aceFlags;
  }
  
  public void writeTo(SMBBuffer buffer) {
    writeTo(buffer, this.aceSize);
  }
  
  void writeTo(SMBBuffer buffer, int aceSize) {
    buffer.putByte((byte)(int)this.aceType.getValue());
    buffer.putByte((byte)(int)EnumWithValue.EnumUtils.<AceFlags>toLong(this.aceFlags));
    buffer.putUInt16(aceSize);
  }
  
  static AceHeader readFrom(SMBBuffer buffer) throws Buffer.BufferException {
    AceType aceType = EnumWithValue.EnumUtils.<AceType>valueOf(buffer.readByte(), AceType.class, null);
    Set<AceFlags> aceFlags = EnumWithValue.EnumUtils.toEnumSet(buffer.readByte(), AceFlags.class);
    int aceSize = buffer.readUInt16();
    AceHeader header = new AceHeader(aceType, aceFlags);
    header.aceSize = aceSize;
    return header;
  }
  
  public int getAceSize() {
    return this.aceSize;
  }
  
  public AceType getAceType() {
    return this.aceType;
  }
  
  public Set<AceFlags> getAceFlags() {
    return this.aceFlags;
  }
  
  public String toString() {
    return "AceHeader{aceType=" + 
      this.aceType + 
      ", aceFlags=" + this.aceFlags + 
      ", aceSize=" + this.aceSize + 
      '}';
  }
}
