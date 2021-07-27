package com.hierynomus.mssmb2;

import com.hierynomus.protocol.commons.ByteArrayUtils;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;

public class SMB2FileId {
  private byte[] persistentHandle;
  
  private byte[] volatileHandle;
  
  public SMB2FileId() {
    this.persistentHandle = new byte[] { -1, -1, -1, -1, -1, -1, -1, -1 };
    this.volatileHandle = new byte[] { -1, -1, -1, -1, -1, -1, -1, -1 };
  }
  
  public SMB2FileId(byte[] persistentHandle, byte[] volatileHandle) {
    this.persistentHandle = persistentHandle;
    this.volatileHandle = volatileHandle;
  }
  
  public void write(SMBBuffer buffer) {
    buffer.putRawBytes(this.persistentHandle);
    buffer.putRawBytes(this.volatileHandle);
  }
  
  public static SMB2FileId read(SMBBuffer buffer) throws Buffer.BufferException {
    return new SMB2FileId(buffer.readRawBytes(8), buffer.readRawBytes(8));
  }
  
  public String toString() {
    return "SMB2FileId{persistentHandle=" + 
      ByteArrayUtils.printHex(this.persistentHandle) + 
      '}';
  }
}
