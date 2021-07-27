package com.hierynomus.smbj.share;

import com.hierynomus.mserref.NtStatus;
import com.hierynomus.msfscc.fileinformation.FileEndOfFileInformation;
import com.hierynomus.mssmb2.SMB2FileId;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.mssmb2.messages.SMB2ReadResponse;
import com.hierynomus.smbj.ProgressListener;
import com.hierynomus.smbj.io.ByteChunkProvider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class File extends DiskEntry {
  private static final Logger logger = LoggerFactory.getLogger(File.class);
  
  private final SMB2Writer writer;
  
  File(SMB2FileId fileId, DiskShare diskShare, String fileName) {
    super(fileId, diskShare, fileName);
    this.writer = new SMB2Writer(diskShare, fileId, fileName);
  }
  
  public int write(byte[] buffer, long fileOffset) {
    return this.writer.write(buffer, fileOffset);
  }
  
  public int write(byte[] buffer, long fileOffset, int offset, int length) {
    return this.writer.write(buffer, fileOffset, offset, length);
  }
  
  public int write(ByteChunkProvider provider) {
    return this.writer.write(provider);
  }
  
  public int write(ByteChunkProvider provider, ProgressListener progressListener) {
    return this.writer.write(provider, progressListener);
  }
  
  public OutputStream getOutputStream() {
    return this.writer.getOutputStream();
  }
  
  public OutputStream getOutputStream(ProgressListener listener) {
    return this.writer.getOutputStream(listener);
  }
  
  public int read(byte[] buffer, long fileOffset) {
    return read(buffer, fileOffset, 0, buffer.length);
  }
  
  public int read(byte[] buffer, long fileOffset, int offset, int length) {
    SMB2ReadResponse response = this.share.read(this.fileId, fileOffset, length);
    if (response.getHeader().getStatus() == NtStatus.STATUS_END_OF_FILE)
      return -1; 
    byte[] data = response.getData();
    int bytesRead = Math.min(length, data.length);
    System.arraycopy(data, 0, buffer, offset, bytesRead);
    return bytesRead;
  }
  
  Future<SMB2ReadResponse> readAsync(long offset, int length) {
    return this.share.readAsync(this.fileId, offset, length);
  }
  
  public void read(OutputStream destStream) throws IOException {
    read(destStream, (ProgressListener)null);
  }
  
  public void read(OutputStream destStream, ProgressListener progressListener) throws IOException {
    InputStream is = getInputStream(progressListener);
    byte[] buf = new byte[this.share.getReadBufferSize()];
    int numRead;
    while ((numRead = is.read(buf)) != -1)
      destStream.write(buf, 0, numRead); 
    is.close();
  }
  
  public void setLength(long endOfFile) throws SMBApiException {
    FileEndOfFileInformation endOfFileInfo = new FileEndOfFileInformation(endOfFile);
    setFileInformation(endOfFileInfo);
  }
  
  public InputStream getInputStream() {
    return getInputStream((ProgressListener)null);
  }
  
  public InputStream getInputStream(ProgressListener listener) {
    return new FileInputStream(this, this.share.getReadBufferSize(), this.share.getReadTimeout(), listener);
  }
  
  public String toString() {
    return "File{fileId=" + 
      this.fileId + 
      ", fileName='" + this.fileName + '\'' + 
      '}';
  }
}
