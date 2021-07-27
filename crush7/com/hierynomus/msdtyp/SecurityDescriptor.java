package com.hierynomus.msdtyp;

import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;
import java.util.EnumSet;
import java.util.Set;

public class SecurityDescriptor {
  private Set<Control> control;
  
  private SID ownerSid;
  
  private SID groupSid;
  
  private ACL sacl;
  
  private ACL dacl;
  
  public SecurityDescriptor() {}
  
  public SecurityDescriptor(Set<Control> control, SID ownerSid, SID groupSid, ACL sacl, ACL dacl) {
    this.control = control;
    this.ownerSid = ownerSid;
    this.groupSid = groupSid;
    this.sacl = sacl;
    this.dacl = dacl;
  }
  
  public void write(SMBBuffer buffer) {
    int ownerOffset, groupOffset, saclOffset, daclOffset, startPos = buffer.wpos();
    buffer.putByte((byte)1);
    buffer.putByte((byte)0);
    EnumSet<Control> c = EnumSet.copyOf(this.control);
    c.add(Control.SR);
    if (this.sacl != null)
      c.add(Control.SP); 
    if (this.dacl != null)
      c.add(Control.DP); 
    buffer.putUInt16((int)EnumWithValue.EnumUtils.<Control>toLong(this.control));
    int offsetsPos = buffer.wpos();
    buffer.putUInt32(0L);
    buffer.putUInt32(0L);
    buffer.putUInt32(0L);
    buffer.putUInt32(0L);
    if (this.ownerSid != null) {
      ownerOffset = buffer.wpos() - startPos;
      this.ownerSid.write(buffer);
    } else {
      ownerOffset = 0;
    } 
    if (this.groupSid != null) {
      groupOffset = buffer.wpos() - startPos;
      this.groupSid.write(buffer);
    } else {
      groupOffset = 0;
    } 
    if (this.sacl != null) {
      saclOffset = buffer.wpos() - startPos;
      this.sacl.write(buffer);
    } else {
      saclOffset = 0;
    } 
    if (this.dacl != null) {
      daclOffset = buffer.wpos() - startPos;
      this.dacl.write(buffer);
    } else {
      daclOffset = 0;
    } 
    int endPos = buffer.wpos();
    buffer.wpos(offsetsPos);
    buffer.putUInt32(ownerOffset);
    buffer.putUInt32(groupOffset);
    buffer.putUInt32(saclOffset);
    buffer.putUInt32(daclOffset);
    buffer.wpos(endPos);
  }
  
  public static SecurityDescriptor read(SMBBuffer buffer) throws Buffer.BufferException {
    int startPos = buffer.rpos();
    buffer.readByte();
    buffer.readByte();
    EnumSet<Control> control = EnumWithValue.EnumUtils.toEnumSet(buffer.readUInt16(), Control.class);
    int ownerOffset = buffer.readUInt32AsInt();
    int groupOffset = buffer.readUInt32AsInt();
    int saclOffset = buffer.readUInt32AsInt();
    int daclOffset = buffer.readUInt32AsInt();
    SID ownerSid = null;
    if (ownerOffset > 0) {
      buffer.rpos(startPos + ownerOffset);
      ownerSid = SID.read(buffer);
    } 
    SID groupSid = null;
    if (groupOffset > 0) {
      buffer.rpos(startPos + groupOffset);
      groupSid = SID.read(buffer);
    } 
    ACL sacl = null;
    if (saclOffset > 0) {
      buffer.rpos(startPos + saclOffset);
      sacl = ACL.read(buffer);
    } 
    ACL dacl = null;
    if (daclOffset > 0) {
      buffer.rpos(startPos + daclOffset);
      dacl = ACL.read(buffer);
    } 
    return new SecurityDescriptor(control, ownerSid, groupSid, sacl, dacl);
  }
  
  public Set<Control> getControl() {
    return this.control;
  }
  
  public SID getOwnerSid() {
    return this.ownerSid;
  }
  
  public SID getGroupSid() {
    return this.groupSid;
  }
  
  public ACL getSacl() {
    return this.sacl;
  }
  
  public ACL getDacl() {
    return this.dacl;
  }
  
  public String toString() {
    return "SecurityDescriptor{control=" + 
      this.control + 
      ", ownerSid=" + this.ownerSid + 
      ", groupSid=" + this.groupSid + 
      ", sacl=" + this.sacl + 
      ", dacl=" + this.dacl + 
      '}';
  }
  
  public enum Control implements EnumWithValue<Control> {
    NONE(0L),
    OD(1L),
    GD(2L),
    DP(4L),
    DD(8L),
    SP(16L),
    SD(32L),
    SS(64L),
    DT(128L),
    DC(256L),
    SC(512L),
    DI(1024L),
    SI(2048L),
    PD(4096L),
    PS(8192L),
    RM(16384L),
    SR(32768L);
    
    private long value;
    
    Control(long value) {
      this.value = value;
    }
    
    public long getValue() {
      return this.value;
    }
  }
}
