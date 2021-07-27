package com.hierynomus.ntlm.messages;

import com.hierynomus.ntlm.functions.NtlmFunctions;
import com.hierynomus.protocol.commons.Charsets;
import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.protocol.commons.buffer.Endian;

public class NtlmAuthenticate extends NtlmPacket {
  private static byte[] EMPTY = new byte[0];
  
  private byte[] lmResponse;
  
  private byte[] ntResponse;
  
  private byte[] userName;
  
  private byte[] domainName;
  
  private byte[] workstation;
  
  private byte[] encryptedRandomSessionKey;
  
  private long negotiateFlags;
  
  private boolean useMic;
  
  private byte[] mic;
  
  public NtlmAuthenticate(byte[] lmResponse, byte[] ntResponse, String userName, String domainName, String workstation, byte[] encryptedRandomSessionKey, long negotiateFlags, boolean useMic) {
    this.lmResponse = ensureNotNull(lmResponse);
    this.ntResponse = ensureNotNull(ntResponse);
    this.userName = ensureNotNull(userName);
    this.domainName = ensureNotNull(domainName);
    this.workstation = ensureNotNull(workstation);
    this.encryptedRandomSessionKey = ensureNotNull(encryptedRandomSessionKey);
    this.negotiateFlags = negotiateFlags;
    this.useMic = useMic;
  }
  
  public void write(Buffer.PlainBuffer buffer) {
    writeAutentificateMessage(buffer);
    if (this.useMic)
      buffer.putRawBytes(this.mic); 
    buffer.putRawBytes(this.lmResponse);
    buffer.putRawBytes(this.ntResponse);
    buffer.putRawBytes(this.domainName);
    buffer.putRawBytes(this.userName);
    buffer.putRawBytes(this.workstation);
    buffer.putRawBytes(this.encryptedRandomSessionKey);
  }
  
  public void setMic(byte[] mic) {
    this.mic = mic;
  }
  
  public void writeAutentificateMessage(Buffer.PlainBuffer buffer) {
    buffer.putString("NTLMSSP\000", Charsets.UTF_8);
    buffer.putUInt32(3L);
    int offset = 64;
    if (this.useMic)
      offset += 16; 
    if (EnumWithValue.EnumUtils.isSet(this.negotiateFlags, NtlmNegotiateFlag.NTLMSSP_NEGOTIATE_VERSION))
      offset += 8; 
    offset = writeOffsettedByteArrayFields(buffer, this.lmResponse, offset);
    offset = writeOffsettedByteArrayFields(buffer, this.ntResponse, offset);
    offset = writeOffsettedByteArrayFields(buffer, this.domainName, offset);
    offset = writeOffsettedByteArrayFields(buffer, this.userName, offset);
    offset = writeOffsettedByteArrayFields(buffer, this.workstation, offset);
    if (EnumWithValue.EnumUtils.isSet(this.negotiateFlags, NtlmNegotiateFlag.NTLMSSP_NEGOTIATE_KEY_EXCH)) {
      offset = writeOffsettedByteArrayFields(buffer, this.encryptedRandomSessionKey, offset);
    } else {
      offset = writeOffsettedByteArrayFields(buffer, EMPTY, offset);
    } 
    buffer.putUInt32(this.negotiateFlags);
    if (EnumWithValue.EnumUtils.isSet(this.negotiateFlags, NtlmNegotiateFlag.NTLMSSP_NEGOTIATE_VERSION))
      buffer.putRawBytes(getVersion()); 
  }
  
  public byte[] getVersion() {
    Buffer.PlainBuffer plainBuffer = new Buffer.PlainBuffer(Endian.LE);
    plainBuffer.putByte((byte)6);
    plainBuffer.putByte((byte)1);
    plainBuffer.putUInt16(7600);
    byte[] reserved = { 0, 0, 0 };
    plainBuffer.putRawBytes(reserved);
    plainBuffer.putByte((byte)15);
    return plainBuffer.getCompactData();
  }
  
  private int writeOffsettedByteArrayFields(Buffer.PlainBuffer buffer, byte[] array, int offset) {
    byte[] _array = (array != null) ? array : EMPTY;
    buffer.putUInt16(_array.length);
    buffer.putUInt16(_array.length);
    buffer.putUInt32(offset);
    return offset + _array.length;
  }
  
  private byte[] ensureNotNull(byte[] possiblyNull) {
    return (possiblyNull != null) ? possiblyNull : EMPTY;
  }
  
  private byte[] ensureNotNull(String possiblyNull) {
    return (possiblyNull != null) ? NtlmFunctions.unicode(possiblyNull) : EMPTY;
  }
}
