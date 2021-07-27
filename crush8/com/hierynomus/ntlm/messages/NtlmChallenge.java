package com.hierynomus.ntlm.messages;

import com.hierynomus.msdtyp.MsDataTypes;
import com.hierynomus.protocol.commons.Charsets;
import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.protocol.commons.buffer.Endian;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NtlmChallenge extends NtlmPacket {
  private static final Logger logger = LoggerFactory.getLogger(NtlmChallenge.class);
  
  private int targetNameLen;
  
  private int targetNameBufferOffset;
  
  private EnumSet<NtlmNegotiateFlag> negotiateFlags;
  
  private byte[] serverChallenge;
  
  private WindowsVersion version;
  
  private int targetInfoLen;
  
  private int targetInfoBufferOffset;
  
  private String targetName;
  
  private Map<AvId, Object> targetInfo = new HashMap<>();
  
  private byte[] rawTargetInfo;
  
  public void read(Buffer.PlainBuffer buffer) throws Buffer.BufferException {
    buffer.readString(Charsets.UTF_8, 8);
    buffer.readUInt32();
    readTargetNameFields(buffer);
    this.negotiateFlags = EnumWithValue.EnumUtils.toEnumSet(buffer.readUInt32(), NtlmNegotiateFlag.class);
    this.serverChallenge = buffer.readRawBytes(8);
    buffer.skip(8);
    readTargetInfoFields(buffer);
    readVersion(buffer);
    readTargetName(buffer);
    readTargetInfo(buffer);
  }
  
  private void readTargetInfo(Buffer.PlainBuffer buffer) throws Buffer.BufferException {
    if (this.targetInfoLen > 0) {
      AvId avId;
      buffer.rpos(this.targetInfoBufferOffset);
      this.rawTargetInfo = buffer.readRawBytes(this.targetInfoLen);
      buffer.rpos(this.targetInfoBufferOffset);
      while (true) {
        int l = buffer.readUInt16();
        avId = EnumWithValue.EnumUtils.<AvId>valueOf(l, AvId.class, null);
        logger.trace("NTLM channel contains {}({}) TargetInfo", avId, Integer.valueOf(l));
        int avLen = buffer.readUInt16();
        switch (avId) {
          case MsvAvEOL:
            return;
          case MsvAvNbComputerName:
          case MsvAvNdDomainName:
          case MsvAvDnsComputerName:
          case MsvAvDnsDomainName:
          case MsvAvDnsTreeName:
          case MsvAvTargetName:
            this.targetInfo.put(avId, buffer.readString(Charsets.UTF_16LE, avLen / 2));
            continue;
          case MsvAvFlags:
            this.targetInfo.put(avId, Long.valueOf(buffer.readUInt32(Endian.LE)));
            continue;
          case MsvAvTimestamp:
            this.targetInfo.put(avId, MsDataTypes.readFileTime(buffer));
            continue;
          case MsvAvSingleHost:
          case MsvChannelBindings:
            continue;
        } 
        break;
      } 
      throw new IllegalStateException("Encountered unhandled AvId: " + avId);
    } 
  }
  
  private void readTargetName(Buffer.PlainBuffer buffer) throws Buffer.BufferException {
    if (this.targetNameLen > 0) {
      buffer.rpos(this.targetNameBufferOffset);
      this.targetName = buffer.readString(Charsets.UTF_16LE, this.targetNameLen / 2);
    } 
  }
  
  private void readVersion(Buffer.PlainBuffer buffer) throws Buffer.BufferException {
    if (this.negotiateFlags.contains(NtlmNegotiateFlag.NTLMSSP_NEGOTIATE_VERSION)) {
      this.version = (new WindowsVersion()).readFrom(buffer);
      logger.debug("Windows version = {}", this.version);
    } else {
      buffer.skip(8);
    } 
  }
  
  private void readTargetNameFields(Buffer.PlainBuffer buffer) throws Buffer.BufferException {
    this.targetNameLen = buffer.readUInt16();
    buffer.skip(2);
    this.targetNameBufferOffset = buffer.readUInt32AsInt();
  }
  
  private void readTargetInfoFields(Buffer.PlainBuffer buffer) throws Buffer.BufferException {
    if (this.negotiateFlags.contains(NtlmNegotiateFlag.NTLMSSP_NEGOTIATE_TARGET_INFO)) {
      this.targetInfoLen = buffer.readUInt16();
      buffer.skip(2);
      this.targetInfoBufferOffset = buffer.readUInt32AsInt();
    } else {
      buffer.skip(8);
    } 
  }
  
  public String getTargetName() {
    return this.targetName;
  }
  
  public byte[] getServerChallenge() {
    return this.serverChallenge;
  }
  
  public EnumSet<NtlmNegotiateFlag> getNegotiateFlags() {
    return this.negotiateFlags;
  }
  
  public byte[] getTargetInfo() {
    return this.rawTargetInfo;
  }
  
  public Object getAvPairObject(AvId key) {
    return this.targetInfo.get(key);
  }
  
  public String getAvPairString(AvId key) {
    Object obj = this.targetInfo.get(key);
    if (obj == null)
      return null; 
    return String.valueOf(obj);
  }
  
  public WindowsVersion getVersion() {
    return this.version;
  }
}
