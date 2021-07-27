package com.hierynomus.protocol.commons.buffer;

import com.hierynomus.protocol.commons.ByteArrayUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Buffer<T extends Buffer<T>> {
  private static final Logger logger = LoggerFactory.getLogger(Buffer.class);
  
  public static final int DEFAULT_SIZE = 256;
  
  public static final int MAX_SIZE = 1073741824;
  
  private byte[] data;
  
  private Endian endianness;
  
  protected int rpos;
  
  protected int wpos;
  
  public static class BufferException extends Exception {
    public BufferException(String message) {
      super(message);
    }
  }
  
  public static class PlainBuffer extends Buffer<PlainBuffer> {
    public PlainBuffer(Endian endiannes) {
      super(endiannes);
    }
    
    public PlainBuffer(Buffer<?> from) {
      super(from);
    }
    
    public PlainBuffer(byte[] data, Endian endianness) {
      super(data, endianness);
    }
    
    public PlainBuffer(int size, Endian endianness) {
      super(size, endianness);
    }
  }
  
  protected static int getNextPowerOf2(int i) {
    int j = 1;
    while (j < i) {
      j <<= 1;
      if (j <= 0)
        throw new IllegalArgumentException("Cannot get next power of 2; " + i + " is too large"); 
    } 
    return j;
  }
  
  public Buffer(Endian endiannes) {
    this(256, endiannes);
  }
  
  public Buffer(Buffer<?> from) {
    this.data = new byte[this.wpos = from.wpos - from.rpos];
    this.endianness = from.endianness;
    System.arraycopy(from.data, from.rpos, this.data, 0, this.wpos);
  }
  
  public Buffer(byte[] data, Endian endianness) {
    this(data, true, endianness);
  }
  
  public Buffer(int size, Endian endianness) {
    this(new byte[getNextPowerOf2(size)], false, endianness);
  }
  
  private Buffer(byte[] data, boolean read, Endian endianness) {
    this.data = data;
    this.endianness = endianness;
    this.rpos = 0;
    this.wpos = read ? data.length : 0;
  }
  
  public byte[] array() {
    return this.data;
  }
  
  public int available() {
    return this.wpos - this.rpos;
  }
  
  public void clear() {
    this.rpos = 0;
    this.wpos = 0;
  }
  
  public int rpos() {
    return this.rpos;
  }
  
  public void rpos(int rpos) {
    this.rpos = rpos;
  }
  
  public int wpos() {
    return this.wpos;
  }
  
  public void wpos(int wpos) {
    ensureCapacity(wpos - this.wpos);
    this.wpos = wpos;
  }
  
  protected void ensureAvailable(int a) throws BufferException {
    if (available() < a)
      throw new BufferException("Underflow"); 
  }
  
  public void ensureCapacity(int capacity) {
    if (this.data.length - this.wpos < capacity) {
      int cw = this.wpos + capacity;
      byte[] tmp = new byte[getNextPowerOf2(cw)];
      System.arraycopy(this.data, 0, tmp, 0, this.data.length);
      this.data = tmp;
    } 
  }
  
  public void compact() {
    logger.debug("Compacting...");
    if (available() > 0)
      System.arraycopy(this.data, this.rpos, this.data, 0, this.wpos - this.rpos); 
    this.wpos -= this.rpos;
    this.rpos = 0;
  }
  
  public byte[] getCompactData() {
    int len = available();
    if (len > 0) {
      byte[] b = new byte[len];
      System.arraycopy(this.data, this.rpos, b, 0, len);
      return b;
    } 
    return new byte[0];
  }
  
  public boolean readBoolean() throws BufferException {
    return (readByte() != 0);
  }
  
  public Buffer<T> putBoolean(boolean b) {
    return putByte(b ? 1 : 0);
  }
  
  public byte readByte() throws BufferException {
    ensureAvailable(1);
    return this.data[this.rpos++];
  }
  
  public Buffer<T> putByte(byte b) {
    ensureCapacity(1);
    this.data[this.wpos++] = b;
    return this;
  }
  
  public byte[] readRawBytes(int length) throws BufferException {
    byte[] bytes = new byte[length];
    readRawBytes(bytes);
    return bytes;
  }
  
  public void readRawBytes(byte[] buf) throws BufferException {
    readRawBytes(buf, 0, buf.length);
  }
  
  public void readRawBytes(byte[] buf, int offset, int length) throws BufferException {
    ensureAvailable(length);
    System.arraycopy(this.data, this.rpos, buf, offset, length);
    this.rpos += length;
  }
  
  public Buffer<T> putRawBytes(byte[] buf) {
    return putRawBytes(buf, 0, buf.length);
  }
  
  public Buffer<T> putRawBytes(byte[] buf, int offset, int length) {
    ensureCapacity(length);
    System.arraycopy(buf, offset, this.data, this.wpos, length);
    this.wpos += length;
    return this;
  }
  
  public Buffer<T> putBuffer(Buffer<? extends Buffer<?>> buffer) {
    if (buffer != null) {
      int r = buffer.available();
      ensureCapacity(r);
      System.arraycopy(buffer.data, buffer.rpos, this.data, this.wpos, r);
      this.wpos += r;
    } 
    return this;
  }
  
  public int readUInt16() throws BufferException {
    return readUInt16(this.endianness);
  }
  
  public int readUInt16(Endian endianness) throws BufferException {
    return endianness.readUInt16(this);
  }
  
  public Buffer<T> putUInt16(int uint16) {
    return putUInt16(uint16, this.endianness);
  }
  
  public Buffer<T> putUInt16(int uint16, Endian endianness) {
    endianness.writeUInt16(this, uint16);
    return this;
  }
  
  public int readUInt24() throws BufferException {
    return readUInt24(this.endianness);
  }
  
  public int readUInt24(Endian endianness) throws BufferException {
    return endianness.readUInt24(this);
  }
  
  public Buffer<T> putUInt24(int uint24) {
    return putUInt24(uint24, this.endianness);
  }
  
  public Buffer<T> putUInt24(int uint24, Endian endianness) {
    endianness.writeUInt24(this, uint24);
    return this;
  }
  
  public int readUInt32AsInt() throws BufferException {
    return (int)readUInt32();
  }
  
  public long readUInt32() throws BufferException {
    return readUInt32(this.endianness);
  }
  
  public long readUInt32(Endian endianness) throws BufferException {
    return endianness.readUInt32(this);
  }
  
  public Buffer<T> putUInt32(long uint32) {
    return putUInt32(uint32, this.endianness);
  }
  
  public Buffer<T> putUInt32(long uint32, Endian endianness) {
    endianness.writeUInt32(this, uint32);
    return this;
  }
  
  public long readUInt64() throws BufferException {
    return readUInt64(this.endianness);
  }
  
  public long readUInt64(Endian endianness) throws BufferException {
    return endianness.readUInt64(this);
  }
  
  public Buffer<T> putUInt64(long uint64) {
    return putUInt64(uint64, this.endianness);
  }
  
  public Buffer<T> putUInt64(long uint64, Endian endianness) {
    endianness.writeUInt64(this, uint64);
    return this;
  }
  
  public Buffer<T> putLong(long longVal) {
    return putLong(longVal, this.endianness);
  }
  
  public Buffer<T> putLong(long longVal, Endian endianness) {
    endianness.writeLong(this, longVal);
    return this;
  }
  
  public long readLong() throws BufferException {
    return readLong(this.endianness);
  }
  
  public long readLong(Endian endianness) throws BufferException {
    return endianness.readLong(this);
  }
  
  public String readString(String encoding, int length) throws BufferException {
    return readString(Charset.forName(encoding), length, this.endianness);
  }
  
  public String readString(Charset charset, int length) throws BufferException {
    return readString(charset, length, this.endianness);
  }
  
  private String readString(Charset charset, int length, Endian endianness) throws BufferException {
    switch (charset.name()) {
      case "UTF-16":
        return endianness.readUtf16String(this, length);
      case "UTF-16LE":
        return Endian.LE.readUtf16String(this, length);
      case "UTF-16BE":
        return Endian.BE.readUtf16String(this, length);
      case "UTF-8":
        return new String(readRawBytes(length), charset);
    } 
    throw new UnsupportedCharsetException(charset.name());
  }
  
  public String readNullTerminatedString(Charset charset) throws BufferException {
    return readNullTerminatedString(charset, this.endianness);
  }
  
  private String readNullTerminatedString(Charset charset, Endian endianness) throws BufferException {
    ByteArrayOutputStream baos;
    byte b;
    switch (charset.name()) {
      case "UTF-16":
        return endianness.readNullTerminatedUtf16String(this);
      case "UTF-16LE":
        return Endian.LE.readNullTerminatedUtf16String(this);
      case "UTF-16BE":
        return Endian.BE.readNullTerminatedUtf16String(this);
      case "UTF-8":
        baos = new ByteArrayOutputStream();
        b = readByte();
        while (b != 0) {
          baos.write(b);
          b = readByte();
        } 
        return new String(baos.toByteArray(), charset);
    } 
    throw new UnsupportedCharsetException(charset.name());
  }
  
  public Buffer<T> putString(String string, Charset charset) {
    return putString(string, charset, this.endianness);
  }
  
  private Buffer<T> putString(String string, Charset charset, Endian endianness) {
    byte[] bytes;
    switch (charset.name()) {
      case "UTF-16":
        endianness.writeUtf16String(this, string);
        return this;
      case "UTF-16LE":
        Endian.LE.writeUtf16String(this, string);
        return this;
      case "UTF-16BE":
        Endian.BE.writeUtf16String(this, string);
        return this;
      case "UTF-8":
        bytes = string.getBytes(charset);
        putRawBytes(bytes);
        return this;
    } 
    throw new UnsupportedCharsetException(charset.name());
  }
  
  public Buffer<T> putNullTerminatedString(String string, Charset charset) {
    return putNullTerminatedString(string, charset, this.endianness);
  }
  
  private Buffer<T> putNullTerminatedString(String string, Charset charset, Endian endianness) {
    byte[] bytes;
    switch (charset.name()) {
      case "UTF-16":
        endianness.writeNullTerminatedUtf16String(this, string);
        return this;
      case "UTF-16LE":
        Endian.LE.writeNullTerminatedUtf16String(this, string);
        return this;
      case "UTF-16BE":
        Endian.BE.writeNullTerminatedUtf16String(this, string);
        return this;
      case "UTF-8":
        bytes = string.getBytes(charset);
        putRawBytes(bytes);
        putByte((byte)0);
        return this;
    } 
    throw new UnsupportedCharsetException(charset.name());
  }
  
  public Buffer<T> skip(int length) throws BufferException {
    ensureAvailable(length);
    this.rpos += length;
    return this;
  }
  
  public String printHex() {
    return ByteArrayUtils.printHex(array(), rpos(), available());
  }
  
  public String toString() {
    return "Buffer [rpos=" + this.rpos + ", wpos=" + this.wpos + ", size=" + this.data.length + "]";
  }
  
  public InputStream asInputStream() {
    return new InputStream() {
        public int read() throws IOException {
          try {
            return Buffer.this.readByte() & 0xFF;
          } catch (BufferException e) {
            throw new IOException(e);
          } 
        }
        
        public int read(byte[] b) throws IOException {
          try {
            Buffer.this.readRawBytes(b);
            return b.length;
          } catch (BufferException e) {
            throw new IOException(e);
          } 
        }
        
        public int read(byte[] b, int off, int len) throws IOException {
          return super.read(b, off, len);
        }
        
        public long skip(long n) {
          Buffer.this.rpos((int)n);
          return n;
        }
        
        public int available() {
          return Buffer.this.available();
        }
      };
  }
}
