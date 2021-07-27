package com.hierynomus.smbj.session;

import com.hierynomus.mssmb2.SMB2Dialect;
import com.hierynomus.mssmb2.SMB2Header;
import com.hierynomus.mssmb2.SMB2MessageFlag;
import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.security.Mac;
import com.hierynomus.security.SecurityException;
import com.hierynomus.security.SecurityProvider;
import com.hierynomus.smb.SMBBuffer;
import com.hierynomus.smb.SMBHeader;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketSignatory {
  private static final Logger logger = LoggerFactory.getLogger(PacketSignatory.class);
  
  private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
  
  private SMB2Dialect dialect;
  
  private SecurityProvider securityProvider;
  
  private String algorithm;
  
  private byte[] secretKey;
  
  PacketSignatory(SMB2Dialect dialect, SecurityProvider securityProvider) {
    this.dialect = dialect;
    this.securityProvider = securityProvider;
  }
  
  void init(byte[] secretKey) {
    if (this.dialect.isSmb3x())
      throw new IllegalStateException("Cannot set a signing key (yet) for SMB3.x"); 
    this.algorithm = "HmacSHA256";
    this.secretKey = secretKey;
  }
  
  boolean isInitialized() {
    return (this.secretKey != null);
  }
  
  SMB2Packet sign(SMB2Packet packet) {
    if (this.secretKey != null)
      return new SignedPacketWrapper(packet); 
    logger.debug("Not wrapping {} as signed, as no key is set.", packet.getHeader().getMessage());
    return packet;
  }
  
  public boolean verify(SMB2Packet packet) {
    try {
      SMBBuffer buffer = packet.getBuffer();
      Mac mac = getMac(this.secretKey, this.algorithm, this.securityProvider);
      mac.update(buffer.array(), packet.getMessageStartPos(), 48);
      mac.update(SMB2Header.EMPTY_SIGNATURE);
      mac.update(buffer.array(), 64, packet.getMessageEndPos() - 64);
      byte[] signature = mac.doFinal();
      byte[] receivedSignature = packet.getHeader().getSignature();
      for (int i = 0; i < 16; i++) {
        if (signature[i] != receivedSignature[i]) {
          logger.error("Signatures for packet {} do not match (received: {}, calculated: {})", new Object[] { packet, Arrays.toString(receivedSignature), Arrays.toString(signature) });
          return false;
        } 
      } 
      return true;
    } catch (SecurityException e) {
      throw new IllegalStateException(e);
    } 
  }
  
  private static Mac getMac(byte[] secretKey, String algorithm, SecurityProvider securityProvider) throws SecurityException {
    Mac mac = securityProvider.getMac(algorithm);
    mac.init(secretKey);
    return mac;
  }
  
  public class SignedPacketWrapper extends SMB2Packet {
    private final SMB2Packet wrappedPacket;
    
    SignedPacketWrapper(SMB2Packet packet) {
      this.wrappedPacket = packet;
    }
    
    public int getMaxPayloadSize() {
      return this.wrappedPacket.getMaxPayloadSize();
    }
    
    public void write(SMBBuffer buffer) {
      try {
        this.wrappedPacket.getHeader().setFlag(SMB2MessageFlag.SMB2_FLAGS_SIGNED);
        int packetStartPos = buffer.wpos();
        SigningBuffer signingBuffer = new SigningBuffer(buffer);
        this.wrappedPacket.write(signingBuffer);
        byte[] signature = signingBuffer.mac.doFinal();
        System.arraycopy(signature, 0, buffer.array(), packetStartPos + 48, 16);
      } catch (SecurityException e) {
        throw new IllegalStateException(e);
      } 
    }
    
    private class SigningBuffer extends SMBBuffer {
      private SMBBuffer wrappedBuffer;
      
      private final Mac mac;
      
      SigningBuffer(SMBBuffer wrappedBuffer) throws SecurityException {
        this.wrappedBuffer = wrappedBuffer;
        this.mac = PacketSignatory.getMac(PacketSignatory.this.secretKey, PacketSignatory.this.algorithm, PacketSignatory.this.securityProvider);
      }
      
      public Buffer<SMBBuffer> putByte(byte b) {
        this.mac.update(b);
        this.wrappedBuffer.putByte(b);
        return this;
      }
      
      public Buffer<SMBBuffer> putBuffer(Buffer<? extends Buffer<?>> buffer) {
        this.mac.update(buffer.array(), buffer.rpos(), buffer.available());
        this.wrappedBuffer.putBuffer(buffer);
        return this;
      }
      
      public Buffer<SMBBuffer> putRawBytes(byte[] buf, int offset, int length) {
        this.mac.update(buf, offset, length);
        this.wrappedBuffer.putRawBytes(buf, offset, length);
        return this;
      }
    }
    
    public SMB2Header getHeader() {
      return this.wrappedPacket.getHeader();
    }
    
    public long getSequenceNumber() {
      return this.wrappedPacket.getSequenceNumber();
    }
    
    public int getStructureSize() {
      return this.wrappedPacket.getStructureSize();
    }
    
    public String toString() {
      return this.wrappedPacket.toString();
    }
    
    public SMB2Packet getPacket() {
      return this.wrappedPacket.getPacket();
    }
  }
}
