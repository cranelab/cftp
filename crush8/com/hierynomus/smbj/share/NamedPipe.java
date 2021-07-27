package com.hierynomus.smbj.share;

import com.hierynomus.msfscc.fsctl.FsCtlPipePeekResponse;
import com.hierynomus.mssmb2.SMB2FileId;
import com.hierynomus.mssmb2.messages.SMB2ReadResponse;
import com.hierynomus.mssmb2.messages.SMB2WriteResponse;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;
import com.hierynomus.smbj.common.SMBRuntimeException;
import com.hierynomus.smbj.common.SmbPath;
import com.hierynomus.smbj.io.ArrayByteChunkProvider;
import java.io.Closeable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamedPipe implements Closeable {
  private static final long FSCTL_PIPE_PEEK = 1130508L;
  
  private static final long FSCTL_PIPE_TRANSCEIVE = 1163287L;
  
  protected final Logger logger = LoggerFactory.getLogger(getClass());
  
  protected PipeShare share;
  
  protected SMB2FileId fileId;
  
  protected SmbPath name;
  
  NamedPipe(SMB2FileId fileId, PipeShare share, SmbPath name) {
    this.share = share;
    this.fileId = fileId;
    this.name = name;
  }
  
  public String getName() {
    return this.name.getPath();
  }
  
  public SMB2FileId getFileId() {
    return this.fileId;
  }
  
  public int write(byte[] buffer) {
    return write(buffer, 0, buffer.length);
  }
  
  public int write(byte[] buffer, int offset, int length) {
    ArrayByteChunkProvider provider = new ArrayByteChunkProvider(buffer, offset, length, 0L);
    this.logger.debug("Writing to {} from offset {}", this.name, Long.valueOf(provider.getOffset()));
    SMB2WriteResponse wresp = this.share.write(this.fileId, provider);
    return (int)wresp.getBytesWritten();
  }
  
  public int read(byte[] buffer) {
    return read(buffer, 0, buffer.length);
  }
  
  public int read(byte[] buffer, int offset, int length) {
    SMB2ReadResponse response = this.share.read(this.fileId, 0L, length);
    byte[] data = response.getData();
    int bytesRead = Math.min(length, data.length);
    System.arraycopy(data, 0, buffer, offset, bytesRead);
    return bytesRead;
  }
  
  public byte[] transact(byte[] inBuffer) {
    return ioctl(1163287L, true, inBuffer, 0, inBuffer.length);
  }
  
  public int transact(byte[] inBuffer, byte[] outBuffer) {
    return transact(inBuffer, 0, inBuffer.length, outBuffer, 0, outBuffer.length);
  }
  
  public int transact(byte[] inBuffer, int inOffset, int inLength, byte[] outBuffer, int outOffset, int outLength) {
    return ioctl(1163287L, true, inBuffer, inOffset, inLength, outBuffer, outOffset, outLength);
  }
  
  public FsCtlPipePeekResponse peek() {
    return peek(0);
  }
  
  public FsCtlPipePeekResponse peek(int maxDataSize) {
    byte[] output = this.share.ioctl(this.fileId, 1130508L, true, null, 0, 0, 24 + maxDataSize);
    try {
      SMBBuffer buffer = new SMBBuffer(output);
      FsCtlPipePeekResponse peekResponse = new FsCtlPipePeekResponse();
      peekResponse.read(buffer);
      return peekResponse;
    } catch (com.hierynomus.protocol.commons.buffer.Buffer.BufferException e) {
      throw new SMBRuntimeException(e);
    } 
  }
  
  public byte[] ioctl(long ctlCode, boolean isFsCtl, byte[] inData, int inOffset, int inLength) {
    return this.share.ioctl(this.fileId, ctlCode, isFsCtl, inData, inOffset, inLength);
  }
  
  public int ioctl(long ctlCode, boolean isFsCtl, byte[] inData, int inOffset, int inLength, byte[] outData, int outOffset, int outLength) {
    return this.share.ioctl(this.fileId, ctlCode, isFsCtl, inData, inOffset, inLength, outData, outOffset, outLength);
  }
  
  public void close() {
    this.share.closeFileId(this.fileId);
  }
  
  public void closeSilently() {
    try {
      close();
    } catch (Exception e) {
      this.logger.warn("Pipe close failed for {},{},{}", new Object[] { this.name, this.share, this.fileId, e });
    } 
  }
}
