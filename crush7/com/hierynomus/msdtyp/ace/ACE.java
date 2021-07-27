package com.hierynomus.msdtyp.ace;

import com.hierynomus.msdtyp.SID;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;

public abstract class ACE {
  private static int HEADER_STRUCTURE_SIZE = 4;
  
  AceHeader aceHeader = new AceHeader();
  
  ACE(AceHeader header) {
    this.aceHeader = header;
  }
  
  public final void write(SMBBuffer buffer) {
    int startPos = buffer.wpos();
    buffer.wpos(startPos + HEADER_STRUCTURE_SIZE);
    writeBody(buffer);
    int endPos = buffer.wpos();
    buffer.wpos(startPos);
    this.aceHeader.writeTo(buffer, endPos - startPos);
    buffer.wpos(endPos);
  }
  
  public static ACE read(SMBBuffer buffer) throws Buffer.BufferException {
    ACE ace;
    int startPos = buffer.rpos();
    AceHeader header = AceHeader.readFrom(buffer);
    switch (header.getAceType()) {
      case null:
        ace = AceType1.read(header, buffer);
        return ace;
      case ACCESS_ALLOWED_CALLBACK_ACE_TYPE:
        ace = AceType3.read(header, buffer, startPos);
        return ace;
      case ACCESS_ALLOWED_CALLBACK_OBJECT_ACE_TYPE:
        ace = AceType4.read(header, buffer, startPos);
        return ace;
      case ACCESS_ALLOWED_OBJECT_ACE_TYPE:
        ace = AceType2.read(header, buffer, startPos);
        return ace;
      case ACCESS_DENIED_ACE_TYPE:
        ace = AceType1.read(header, buffer);
        return ace;
      case ACCESS_DENIED_CALLBACK_ACE_TYPE:
        ace = AceType3.read(header, buffer, startPos);
        return ace;
      case ACCESS_DENIED_CALLBACK_OBJECT_ACE_TYPE:
        ace = AceType4.read(header, buffer, startPos);
        return ace;
      case ACCESS_DENIED_OBJECT_ACE_TYPE:
        ace = AceType2.read(header, buffer, startPos);
        return ace;
      case SYSTEM_AUDIT_ACE_TYPE:
        ace = AceType1.read(header, buffer);
        return ace;
      case SYSTEM_AUDIT_CALLBACK_ACE_TYPE:
        ace = AceType3.read(header, buffer, startPos);
        return ace;
      case SYSTEM_AUDIT_CALLBACK_OBJECT_ACE_TYPE:
        ace = AceType4.read(header, buffer, startPos);
        return ace;
      case SYSTEM_AUDIT_OBJECT_ACE_TYPE:
        ace = AceType4.read(header, buffer, startPos);
        return ace;
      case SYSTEM_MANDATORY_LABEL_ACE_TYPE:
        ace = AceType1.read(header, buffer);
        return ace;
      case SYSTEM_RESOURCE_ATTRIBUTE_ACE_TYPE:
        ace = AceType3.read(header, buffer, startPos);
        return ace;
      case SYSTEM_SCOPED_POLICY_ID_ACE_TYPE:
        ace = AceType1.read(header, buffer);
        return ace;
    } 
    throw new IllegalStateException("Unknown ACE type: " + header.getAceType());
  }
  
  public AceHeader getAceHeader() {
    return this.aceHeader;
  }
  
  protected ACE() {}
  
  abstract void writeBody(SMBBuffer paramSMBBuffer);
  
  public abstract SID getSid();
  
  public abstract long getAccessMask();
}
