package com.hierynomus.msdtyp;

import com.hierynomus.protocol.commons.ByteArrayUtils;
import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SID {
  public enum SidType implements EnumWithValue<SidType> {
    SID_TYPE_NONE(0L, "0"),
    SID_TYPE_USER(1L, "User"),
    SID_TYPE_DOM_GRP(2L, "Domain group"),
    SID_TYPE_DOMAIN(3L, "Domain"),
    SID_TYPE_ALIAS(4L, "Local group"),
    SID_TYPE_WKN_GRP(5L, "Builtin group"),
    SID_TYPE_DELETED(6L, "Deleted"),
    SID_TYPE_INVALID(7L, "Invalid"),
    SID_TYPE_UNKNOWN(8L, "Unknown"),
    SID_TYPE_COMPUTER(9L, "Computer"),
    SID_TYPE_LABEL(10L, "Label");
    
    private long value;
    
    private String name;
    
    SidType(long value, String name) {
      this.value = value;
      this.name = name;
    }
    
    public long getValue() {
      return this.value;
    }
    
    public String getName() {
      return this.name;
    }
  }
  
  public static final SID EVERYONE = new SID((byte)1, new byte[] { 0, 0, 0, 0, 0, 1 }, new long[] { 0L });
  
  private byte revision;
  
  private byte[] sidIdentifierAuthority;
  
  private long[] subAuthorities;
  
  public SID() {}
  
  public SID(byte revision, byte[] sidIdentifierAuthority, long[] subAuthorities) {
    this.revision = revision;
    this.sidIdentifierAuthority = sidIdentifierAuthority;
    this.subAuthorities = subAuthorities;
  }
  
  private static final Pattern SID_REGEX = Pattern.compile("S-([0-9]+)-((?:0x[0-9a-fA-F]+)|(?:[0-9]+))(-[0-9]+)+");
  
  public static SID fromString(String sidString) {
    long identifierAuthorityValue;
    Matcher matcher = SID_REGEX.matcher(sidString);
    if (!matcher.matches())
      throw new IllegalArgumentException("Invalid SID literal: " + sidString); 
    int revision = Integer.parseInt(matcher.group(1));
    String identifierAuthorityString = matcher.group(2);
    if (identifierAuthorityString.startsWith("0x")) {
      identifierAuthorityValue = Long.parseLong(identifierAuthorityString.substring(2), 16);
    } else {
      identifierAuthorityValue = Long.parseLong(identifierAuthorityString);
    } 
    byte[] identifierAuthority = new byte[6];
    identifierAuthority[0] = (byte)(int)(identifierAuthorityValue >> 40L & 0xFFL);
    identifierAuthority[1] = (byte)(int)(identifierAuthorityValue >> 32L & 0xFFL);
    identifierAuthority[2] = (byte)(int)(identifierAuthorityValue >> 24L & 0xFFL);
    identifierAuthority[3] = (byte)(int)(identifierAuthorityValue >> 16L & 0xFFL);
    identifierAuthority[4] = (byte)(int)(identifierAuthorityValue >> 8L & 0xFFL);
    identifierAuthority[5] = (byte)(int)(identifierAuthorityValue & 0xFFL);
    String[] subAuthorityStrings = sidString.substring(matcher.end(2)).split("-");
    long[] subAuthorities = new long[subAuthorityStrings.length - 1];
    for (int i = 0; i < subAuthorities.length; i++)
      subAuthorities[i] = Long.parseLong(subAuthorityStrings[i + 1]); 
    return new SID((byte)revision, identifierAuthority, subAuthorities);
  }
  
  public void write(SMBBuffer buffer) {
    buffer.putByte(this.revision);
    buffer.putByte((byte)this.subAuthorities.length);
    if (this.sidIdentifierAuthority.length > 6)
      throw new IllegalArgumentException("The IdentifierAuthority can not be larger than 6 bytes"); 
    buffer.putRawBytes(this.sidIdentifierAuthority);
    for (long subAuthority : this.subAuthorities)
      buffer.putUInt32(subAuthority); 
  }
  
  public static SID read(SMBBuffer buffer) throws Buffer.BufferException {
    byte revision = buffer.readByte();
    int subAuthorityCount = buffer.readByte();
    byte[] sidIdentifierAuthority = buffer.readRawBytes(6);
    long[] subAuthorities = new long[subAuthorityCount];
    for (int i = 0; i < subAuthorityCount; i++)
      subAuthorities[i] = buffer.readUInt32(); 
    return new SID(revision, sidIdentifierAuthority, subAuthorities);
  }
  
  public int byteCount() {
    return 8 + this.subAuthorities.length * 4;
  }
  
  public String toString() {
    StringBuilder b = new StringBuilder("S-");
    b.append(this.revision & 0xFF).append("-");
    if (this.sidIdentifierAuthority[0] != 0 || this.sidIdentifierAuthority[1] != 0) {
      b.append("0x");
      b.append(ByteArrayUtils.printHex(this.sidIdentifierAuthority, 0, 6));
    } else {
      long shift = 0L;
      long id = 0L;
      for (int i = 5; i > 1; i--) {
        id += (this.sidIdentifierAuthority[i] & 0xFFL) << (int)shift;
        shift += 8L;
      } 
      b.append(id);
    } 
    for (long subAuthority : this.subAuthorities)
      b.append("-").append(subAuthority & 0xFFFFFFFFL); 
    return b.toString();
  }
  
  public byte getRevision() {
    return this.revision;
  }
  
  public byte[] getSidIdentifierAuthority() {
    return this.sidIdentifierAuthority;
  }
  
  public long[] getSubAuthorities() {
    return this.subAuthorities;
  }
  
  public boolean equals(Object o) {
    if (this == o)
      return true; 
    if (o == null || getClass() != o.getClass())
      return false; 
    SID sid = (SID)o;
    if (this.revision != sid.revision)
      return false; 
    if (!Arrays.equals(this.sidIdentifierAuthority, sid.sidIdentifierAuthority))
      return false; 
    return Arrays.equals(this.subAuthorities, sid.subAuthorities);
  }
  
  public int hashCode() {
    int result = this.revision;
    result = 31 * result + Arrays.hashCode(this.sidIdentifierAuthority);
    result = 31 * result + Arrays.hashCode(this.subAuthorities);
    return result;
  }
}
