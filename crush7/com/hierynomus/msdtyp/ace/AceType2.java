package com.hierynomus.msdtyp.ace;

import com.hierynomus.msdtyp.MsDataTypes;
import com.hierynomus.msdtyp.SID;
import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

class AceType2 extends ACE {
  long accessMask;
  
  SID sid;
  
  UUID objectType;
  
  UUID inheritedObjectType;
  
  AceType2(AceHeader header) {
    super(header);
  }
  
  AceType2(AceHeader header, long accessMask, UUID objectType, UUID inheritedObjectType, SID sid) {
    super(header);
    this.accessMask = accessMask;
    this.sid = sid;
    this.objectType = objectType;
    this.inheritedObjectType = inheritedObjectType;
  }
  
  void writeBody(SMBBuffer buffer) {
    buffer.putUInt32(this.accessMask);
    EnumSet<AceObjectFlags> flags = EnumSet.noneOf(AceObjectFlags.class);
    if (this.objectType != null)
      flags.add(AceObjectFlags.ACE_OBJECT_TYPE_PRESENT); 
    if (this.inheritedObjectType != null)
      flags.add(AceObjectFlags.ACE_INHERITED_OBJECT_TYPE_PRESENT); 
    buffer.putUInt32(EnumWithValue.EnumUtils.toLong(flags));
    if (this.objectType != null) {
      MsDataTypes.putGuid(this.objectType, buffer);
    } else {
      buffer.putReserved(16);
    } 
    if (this.inheritedObjectType != null) {
      MsDataTypes.putGuid(this.inheritedObjectType, buffer);
    } else {
      buffer.putReserved(16);
    } 
    this.sid.write(buffer);
  }
  
  void readBody(SMBBuffer buffer, int aceStartPos) throws Buffer.BufferException {
    this.accessMask = buffer.readUInt32();
    Set<AceObjectFlags> flags = EnumWithValue.EnumUtils.toEnumSet(buffer.readUInt32(), AceObjectFlags.class);
    this.objectType = null;
    if (flags.contains(AceObjectFlags.ACE_OBJECT_TYPE_PRESENT)) {
      this.objectType = MsDataTypes.readGuid(buffer);
    } else {
      buffer.skip(16);
    } 
    this.inheritedObjectType = null;
    if (flags.contains(AceObjectFlags.ACE_INHERITED_OBJECT_TYPE_PRESENT)) {
      this.inheritedObjectType = MsDataTypes.readGuid(buffer);
    } else {
      buffer.skip(16);
    } 
    this.sid = SID.read(buffer);
  }
  
  static AceType2 read(AceHeader header, SMBBuffer buffer, int aceStartPos) throws Buffer.BufferException {
    AceType2 ace = new AceType2(header);
    ace.readBody(buffer, aceStartPos);
    return ace;
  }
  
  public String toString() {
    return String.format(
        "AceType2{type=%s, flags=%s, access=0x%x, objectType=%s, inheritedObjectType=%s, sid=%s}", new Object[] { this.aceHeader.getAceType(), 
          this.aceHeader.getAceFlags(), 
          Long.valueOf(this.accessMask), 
          this.objectType, 
          this.inheritedObjectType, 
          this.sid });
  }
  
  public SID getSid() {
    return this.sid;
  }
  
  public long getAccessMask() {
    return this.accessMask;
  }
  
  public UUID getObjectType() {
    return this.objectType;
  }
  
  public UUID getInheritedObjectType() {
    return this.inheritedObjectType;
  }
}
