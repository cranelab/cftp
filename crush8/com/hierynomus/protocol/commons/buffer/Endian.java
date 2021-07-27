package com.hierynomus.protocol.commons.buffer;

import com.hierynomus.protocol.commons.Charsets;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

public abstract class Endian {
  private static final byte[] NULL_TERMINATOR = new byte[] { 0, 0 };
  
  public static final Endian LE = new Little();
  
  public static final Endian BE = new Big();
  
  <T extends Buffer<T>> String readNullTerminatedUtf16String(Buffer<T> buffer, Charset charset) throws Buffer.BufferException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] bytes = new byte[2];
    buffer.readRawBytes(bytes);
    while (bytes[0] != 0 || bytes[1] != 0) {
      baos.write(bytes, 0, 2);
      buffer.readRawBytes(bytes);
    } 
    return new String(baos.toByteArray(), charset);
  }
  
  <T extends Buffer<T>> String readUtf16String(Buffer<T> buffer, int length, Charset charset) throws Buffer.BufferException {
    byte[] stringBytes = new byte[length * 2];
    buffer.readRawBytes(stringBytes);
    return new String(stringBytes, charset);
  }
  
  <T extends Buffer<T>> void writeNullTerminatedUtf16String(Buffer<T> buffer, String string) {
    writeUtf16String(buffer, string);
    buffer.putRawBytes(NULL_TERMINATOR);
  }
  
  public abstract <T extends Buffer<T>> void writeUInt16(Buffer<T> paramBuffer, int paramInt);
  
  public abstract <T extends Buffer<T>> int readUInt16(Buffer<T> paramBuffer) throws Buffer.BufferException;
  
  public abstract <T extends Buffer<T>> void writeUInt24(Buffer<T> paramBuffer, int paramInt);
  
  public abstract <T extends Buffer<T>> int readUInt24(Buffer<T> paramBuffer) throws Buffer.BufferException;
  
  public abstract <T extends Buffer<T>> void writeUInt32(Buffer<T> paramBuffer, long paramLong);
  
  public abstract <T extends Buffer<T>> long readUInt32(Buffer<T> paramBuffer) throws Buffer.BufferException;
  
  public abstract <T extends Buffer<T>> void writeUInt64(Buffer<T> paramBuffer, long paramLong);
  
  public abstract <T extends Buffer<T>> long readUInt64(Buffer<T> paramBuffer) throws Buffer.BufferException;
  
  public abstract <T extends Buffer<T>> void writeLong(Buffer<T> paramBuffer, long paramLong);
  
  public abstract <T extends Buffer<T>> long readLong(Buffer<T> paramBuffer) throws Buffer.BufferException;
  
  public abstract <T extends Buffer<T>> void writeUtf16String(Buffer<T> paramBuffer, String paramString);
  
  public abstract <T extends Buffer<T>> String readUtf16String(Buffer<T> paramBuffer, int paramInt) throws Buffer.BufferException;
  
  public abstract <T extends Buffer<T>> String readNullTerminatedUtf16String(Buffer<T> paramBuffer) throws Buffer.BufferException;
  
  private static class Big extends Endian {
    private Big() {}
    
    public <T extends Buffer<T>> void writeUInt16(Buffer<T> buffer, int uint16) {
      if (uint16 < 0 || uint16 > 65535)
        throw new IllegalArgumentException("Invalid uint16 value: " + uint16); 
      buffer.putRawBytes(new byte[] { (byte)(uint16 >> 8), (byte)uint16 });
    }
    
    public <T extends Buffer<T>> int readUInt16(Buffer<T> buffer) throws Buffer.BufferException {
      byte[] b = buffer.readRawBytes(2);
      return b[0] << 8 & 0xFF00 | b[1] & 0xFF;
    }
    
    public <T extends Buffer<T>> void writeUInt24(Buffer<T> buffer, int uint24) {
      if (uint24 < 0 || uint24 > 16777215)
        throw new IllegalArgumentException("Invalid uint24 value: " + uint24); 
      buffer.putRawBytes(new byte[] { (byte)(uint24 >> 16), (byte)(uint24 >> 8), (byte)uint24 });
    }
    
    public <T extends Buffer<T>> int readUInt24(Buffer<T> buffer) throws Buffer.BufferException {
      byte[] b = buffer.readRawBytes(3);
      return b[0] << 16 & 0xFF0000 | b[1] << 8 & 0xFF00 | b[2] & 0xFF;
    }
    
    public <T extends Buffer<T>> void writeUInt32(Buffer<T> buffer, long uint32) {
      if (uint32 < 0L || uint32 > 4294967295L)
        throw new IllegalArgumentException("Invalid uint32 value: " + uint32); 
      buffer.putRawBytes(new byte[] { (byte)(int)(uint32 >> 24L), (byte)(int)(uint32 >> 16L), (byte)(int)(uint32 >> 8L), (byte)(int)uint32 });
    }
    
    public <T extends Buffer<T>> long readUInt32(Buffer<T> buffer) throws Buffer.BufferException {
      byte[] b = buffer.readRawBytes(4);
      return (b[0] << 24) & 0xFF000000L | (b[1] << 16) & 0xFF0000L | (b[2] << 8) & 0xFF00L | b[3] & 0xFFL;
    }
    
    public <T extends Buffer<T>> void writeUInt64(Buffer<T> buffer, long uint64) {
      if (uint64 < 0L)
        throw new IllegalArgumentException("Invalid uint64 value: " + uint64); 
      writeLong(buffer, uint64);
    }
    
    public <T extends Buffer<T>> long readUInt64(Buffer<T> buffer) throws Buffer.BufferException {
      long uint64 = (readUInt32(buffer) << 32L) + (readUInt32(buffer) & 0xFFFFFFFFL);
      if (uint64 < 0L)
        throw new Buffer.BufferException("Cannot handle values > 9223372036854775807"); 
      return uint64;
    }
    
    public <T extends Buffer<T>> void writeLong(Buffer<T> buffer, long longVal) {
      buffer.putRawBytes(new byte[] { (byte)(int)(longVal >> 56L), (byte)(int)(longVal >> 48L), (byte)(int)(longVal >> 40L), (byte)(int)(longVal >> 32L), (byte)(int)(longVal >> 24L), (byte)(int)(longVal >> 16L), (byte)(int)(longVal >> 8L), (byte)(int)longVal });
    }
    
    public <T extends Buffer<T>> long readLong(Buffer<T> buffer) throws Buffer.BufferException {
      long result = 0L;
      byte[] b = buffer.readRawBytes(8);
      for (int i = 0; i < 8; i++) {
        result <<= 8L;
        result |= (b[i] & 0xFF);
      } 
      return result;
    }
    
    public <T extends Buffer<T>> String readUtf16String(Buffer<T> buffer, int length) throws Buffer.BufferException {
      return readUtf16String(buffer, length, Charsets.UTF_16BE);
    }
    
    public <T extends Buffer<T>> String readNullTerminatedUtf16String(Buffer<T> buffer) throws Buffer.BufferException {
      return readNullTerminatedUtf16String(buffer, Charsets.UTF_16BE);
    }
    
    public <T extends Buffer<T>> void writeUtf16String(Buffer<T> buffer, String string) {
      byte[] bytes = string.getBytes(Charsets.UTF_16BE);
      buffer.putRawBytes(bytes);
    }
    
    public String toString() {
      return "big endian";
    }
  }
  
  private static class Little extends Endian {
    private Little() {}
    
    public <T extends Buffer<T>> void writeUInt16(Buffer<T> buffer, int uint16) {
      if (uint16 < 0 || uint16 > 65535)
        throw new IllegalArgumentException("Invalid uint16 value: " + uint16); 
      buffer.putRawBytes(new byte[] { (byte)uint16, (byte)(uint16 >> 8) });
    }
    
    public <T extends Buffer<T>> int readUInt16(Buffer<T> buffer) throws Buffer.BufferException {
      byte[] b = buffer.readRawBytes(2);
      return b[0] & 0xFF | b[1] << 8 & 0xFF00;
    }
    
    public <T extends Buffer<T>> void writeUInt24(Buffer<T> buffer, int uint24) {
      if (uint24 < 0 || uint24 > 16777215)
        throw new IllegalArgumentException("Invalid uint24 value: " + uint24); 
      buffer.putRawBytes(new byte[] { (byte)uint24, (byte)(uint24 >> 8), (byte)(uint24 >> 16) });
    }
    
    public <T extends Buffer<T>> int readUInt24(Buffer<T> buffer) throws Buffer.BufferException {
      byte[] b = buffer.readRawBytes(3);
      return b[0] & 0xFF | b[1] << 8 & 0xFF00 | b[2] << 16 & 0xFF0000;
    }
    
    public <T extends Buffer<T>> void writeUInt32(Buffer<T> buffer, long uint32) {
      if (uint32 < 0L || uint32 > 4294967295L)
        throw new IllegalArgumentException("Invalid uint32 value: " + uint32); 
      buffer.putRawBytes(new byte[] { (byte)(int)uint32, (byte)(int)(uint32 >> 8L), (byte)(int)(uint32 >> 16L), (byte)(int)(uint32 >> 24L) });
    }
    
    public <T extends Buffer<T>> long readUInt32(Buffer<T> buffer) throws Buffer.BufferException {
      byte[] b = buffer.readRawBytes(4);
      return b[0] & 0xFFL | (b[1] << 8) & 0xFF00L | (b[2] << 16) & 0xFF0000L | (b[3] << 24) & 0xFF000000L;
    }
    
    public <T extends Buffer<T>> void writeUInt64(Buffer<T> buffer, long uint64) {
      if (uint64 < 0L)
        throw new IllegalArgumentException("Invalid uint64 value: " + uint64); 
      writeLong(buffer, uint64);
    }
    
    public <T extends Buffer<T>> long readUInt64(Buffer<T> buffer) throws Buffer.BufferException {
      long uint64 = (readUInt32(buffer) & 0xFFFFFFFFL) + (readUInt32(buffer) << 32L);
      if (uint64 < 0L)
        throw new Buffer.BufferException("Cannot handle values > 9223372036854775807"); 
      return uint64;
    }
    
    public <T extends Buffer<T>> void writeLong(Buffer<T> buffer, long longVal) {
      buffer.putRawBytes(new byte[] { (byte)(int)longVal, (byte)(int)(longVal >> 8L), (byte)(int)(longVal >> 16L), (byte)(int)(longVal >> 24L), (byte)(int)(longVal >> 32L), (byte)(int)(longVal >> 40L), (byte)(int)(longVal >> 48L), (byte)(int)(longVal >> 56L) });
    }
    
    public <T extends Buffer<T>> long readLong(Buffer<T> buffer) throws Buffer.BufferException {
      long result = 0L;
      byte[] bytes = buffer.readRawBytes(8);
      for (int i = 7; i >= 0; i--) {
        result <<= 8L;
        result |= (bytes[i] & 0xFF);
      } 
      return result;
    }
    
    public <T extends Buffer<T>> String readUtf16String(Buffer<T> buffer, int length) throws Buffer.BufferException {
      return readUtf16String(buffer, length, Charsets.UTF_16LE);
    }
    
    public <T extends Buffer<T>> String readNullTerminatedUtf16String(Buffer<T> buffer) throws Buffer.BufferException {
      return readNullTerminatedUtf16String(buffer, Charsets.UTF_16LE);
    }
    
    public <T extends Buffer<T>> void writeUtf16String(Buffer<T> buffer, String string) {
      byte[] bytes = string.getBytes(Charsets.UTF_16LE);
      buffer.putRawBytes(bytes);
    }
    
    public String toString() {
      return "little endian";
    }
  }
}
