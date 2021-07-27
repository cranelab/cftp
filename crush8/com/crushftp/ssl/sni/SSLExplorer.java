package com.crushftp.ssl.sni;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLProtocolException;

public final class SSLExplorer {
  public static final int RECORD_HEADER_SIZE = 5;
  
  public static final int getRequiredSize(ByteBuffer source) {
    ByteBuffer input = source.duplicate();
    if (input.remaining() < 5)
      throw new BufferUnderflowException(); 
    byte firstByte = input.get();
    byte secondByte = input.get();
    byte thirdByte = input.get();
    if ((firstByte & 0x80) != 0 && thirdByte == 1)
      return 5; 
    return ((input.get() & 0xFF) << 8 | input.get() & 0xFF) + 5;
  }
  
  public static final int getRequiredSize(byte[] source, int offset, int length) throws IOException {
    ByteBuffer byteBuffer = ByteBuffer.wrap(source, offset, length).asReadOnlyBuffer();
    return getRequiredSize(byteBuffer);
  }
  
  public static final SSLCapabilities explore(ByteBuffer source) throws IOException {
    ByteBuffer input = source.duplicate();
    if (input.remaining() < 5)
      throw new BufferUnderflowException(); 
    byte firstByte = input.get();
    byte secondByte = input.get();
    byte thirdByte = input.get();
    if ((firstByte & 0x80) != 0 && thirdByte == 1)
      return exploreV2HelloRecord(input, firstByte, secondByte, thirdByte); 
    if (firstByte == 22)
      return exploreTLSRecord(input, firstByte, secondByte, thirdByte); 
    throw new SSLException("Not handshake record");
  }
  
  public static final SSLCapabilities explore(byte[] source, int offset, int length) throws IOException {
    ByteBuffer byteBuffer = ByteBuffer.wrap(source, offset, length).asReadOnlyBuffer();
    return explore(byteBuffer);
  }
  
  private static SSLCapabilities exploreV2HelloRecord(ByteBuffer input, byte firstByte, byte secondByte, byte thirdByte) throws IOException {
    try {
      if (thirdByte != 1)
        throw new SSLException("Unsupported or Unrecognized SSL record"); 
      byte helloVersionMajor = input.get();
      byte helloVersionMinor = input.get();
      return new SSLCapabilitiesImpl((byte)0, (byte)2, helloVersionMajor, helloVersionMinor, Collections.emptyList());
    } catch (BufferUnderflowException bufe) {
      throw new SSLProtocolException("Invalid handshake record");
    } 
  }
  
  private static SSLCapabilities exploreTLSRecord(ByteBuffer input, byte firstByte, byte secondByte, byte thirdByte) throws IOException {
    if (firstByte != 22)
      throw new SSLException("Not handshake record"); 
    byte recordMajorVersion = secondByte;
    byte recordMinorVersion = thirdByte;
    int recordLength = getInt16(input);
    if (recordLength > input.remaining())
      throw new BufferUnderflowException(); 
    try {
      return exploreHandshake(input, recordMajorVersion, recordMinorVersion, recordLength);
    } catch (BufferUnderflowException bufe) {
      throw new SSLProtocolException("Invalid handshake record");
    } 
  }
  
  private static SSLCapabilities exploreHandshake(ByteBuffer input, byte recordMajorVersion, byte recordMinorVersion, int recordLength) throws IOException {
    byte handshakeType = input.get();
    if (handshakeType != 1)
      throw new IllegalStateException("Not initial handshaking"); 
    int handshakeLength = getInt24(input);
    if (handshakeLength > recordLength - 4)
      throw new SSLException("Handshake message spans multiple records"); 
    input = input.duplicate();
    input.limit(handshakeLength + input.position());
    return exploreClientHello(input, recordMajorVersion, recordMinorVersion);
  }
  
  private static SSLCapabilities exploreClientHello(ByteBuffer input, byte recordMajorVersion, byte recordMinorVersion) throws IOException {
    List<?> snList = Collections.emptyList();
    byte helloMajorVersion = input.get();
    byte helloMinorVersion = input.get();
    int position = input.position();
    input.position(position + 32);
    ignoreByteVector8(input);
    ignoreByteVector16(input);
    ignoreByteVector8(input);
    if (input.remaining() > 0)
      snList = exploreExtensions(input); 
    return new SSLCapabilitiesImpl(recordMajorVersion, recordMinorVersion, helloMajorVersion, helloMinorVersion, snList);
  }
  
  private static List exploreExtensions(ByteBuffer input) throws IOException {
    int length = getInt16(input);
    while (length > 0) {
      int extType = getInt16(input);
      int extLen = getInt16(input);
      if (extType == 0)
        return exploreSNIExt(input, extLen); 
      ignoreByteVector(input, extLen);
      length -= extLen + 4;
    } 
    return Collections.emptyList();
  }
  
  private static List exploreSNIExt(ByteBuffer input, int extLen) throws IOException {
    Map<Object, Object> sniMap = new LinkedHashMap<Object, Object>();
    int remains = extLen;
    if (extLen >= 2) {
      int listLen = getInt16(input);
      if (listLen == 0 || listLen + 2 != extLen)
        throw new SSLProtocolException("Invalid server name indication extension"); 
      remains -= 2;
      while (remains > 0) {
        SNIServerName serverName;
        int code = getInt8(input);
        int snLen = getInt16(input);
        if (snLen > remains)
          throw new SSLProtocolException("Not enough data to fill declared vector size"); 
        byte[] encoded = new byte[snLen];
        input.get(encoded);
        switch (code) {
          case 0:
            if (encoded.length == 0)
              throw new SSLProtocolException("Empty HostName in server name indication"); 
            serverName = new SNIHostName(encoded);
            break;
          default:
            serverName = new UnknownServerName(code, encoded);
            break;
        } 
        if (sniMap.put((new StringBuilder(String.valueOf(serverName.getType()))).toString(), serverName) != null)
          throw new SSLProtocolException("Duplicated server name of type " + serverName.getType()); 
        remains -= encoded.length + 3;
      } 
    } else if (extLen == 0) {
      throw new SSLProtocolException("Not server name indication extension in client");
    } 
    if (remains != 0)
      throw new SSLProtocolException("Invalid server name indication extension"); 
    return Collections.unmodifiableList(new ArrayList(sniMap.values()));
  }
  
  private static int getInt8(ByteBuffer input) {
    return input.get();
  }
  
  private static int getInt16(ByteBuffer input) {
    return (input.get() & 0xFF) << 8 | input.get() & 0xFF;
  }
  
  private static int getInt24(ByteBuffer input) {
    return (input.get() & 0xFF) << 16 | (input.get() & 0xFF) << 8 | input.get() & 0xFF;
  }
  
  private static void ignoreByteVector8(ByteBuffer input) {
    ignoreByteVector(input, getInt8(input));
  }
  
  private static void ignoreByteVector16(ByteBuffer input) {
    ignoreByteVector(input, getInt16(input));
  }
  
  private static void ignoreByteVector24(ByteBuffer input) {
    ignoreByteVector(input, getInt24(input));
  }
  
  private static void ignoreByteVector(ByteBuffer input, int length) {
    if (length != 0) {
      int position = input.position();
      input.position(position + length);
    } 
  }
  
  private static class UnknownServerName extends SNIServerName {
    UnknownServerName(int code, byte[] encoded) {
      super(code, encoded);
    }
  }
  
  private static final class SSLCapabilitiesImpl extends SSLCapabilities {
    private static final Map versionMap = new HashMap<Object, Object>(5);
    
    private final String recordVersion;
    
    private final String helloVersion;
    
    List sniNames;
    
    static {
      versionMap.put("2", "SSLv2Hello");
      versionMap.put("768", "SSLv3");
      versionMap.put("769", "TLSv1");
      versionMap.put("770", "TLSv1.1");
      versionMap.put("771", "TLSv1.2");
    }
    
    SSLCapabilitiesImpl(byte recordMajorVersion, byte recordMinorVersion, byte helloMajorVersion, byte helloMinorVersion, List sniNames) {
      int version = recordMajorVersion << 8 | recordMinorVersion;
      this.recordVersion = (versionMap.get((new StringBuilder(String.valueOf(version))).toString()) != null) ? (String)versionMap.get((new StringBuilder(String.valueOf(version))).toString()) : unknownVersion(recordMajorVersion, recordMinorVersion);
      version = helloMajorVersion << 8 | helloMinorVersion;
      this.helloVersion = (versionMap.get((new StringBuilder(String.valueOf(version))).toString()) != null) ? (String)versionMap.get((new StringBuilder(String.valueOf(version))).toString()) : unknownVersion(helloMajorVersion, helloMinorVersion);
      this.sniNames = sniNames;
    }
    
    public String getRecordVersion() {
      return this.recordVersion;
    }
    
    public String getHelloVersion() {
      return this.helloVersion;
    }
    
    public List getServerNames() {
      if (!this.sniNames.isEmpty())
        return Collections.unmodifiableList(this.sniNames); 
      return this.sniNames;
    }
    
    private static String unknownVersion(byte major, byte minor) {
      return "Unknown-" + major + "." + minor;
    }
  }
}
