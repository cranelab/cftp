package com.hierynomus.smbj.share;

import com.hierynomus.mserref.NtStatus;
import com.hierynomus.msfscc.fileinformation.FileEndOfFileInformation;
import com.hierynomus.msfscc.fileinformation.FileStandardInformation;
import com.hierynomus.mssmb2.SMB2FileId;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.mssmb2.copy.CopyChunkRequest;
import com.hierynomus.mssmb2.copy.CopyChunkResponse;
import com.hierynomus.mssmb2.messages.SMB2IoctlResponse;
import com.hierynomus.mssmb2.messages.SMB2ReadResponse;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.protocol.transport.TransportException;
import com.hierynomus.smb.SMBBuffer;
import com.hierynomus.smbj.ProgressListener;
import com.hierynomus.smbj.common.SMBRuntimeException;
import com.hierynomus.smbj.io.ArrayByteChunkProvider;
import com.hierynomus.smbj.io.ByteChunkProvider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class File extends DiskEntry {
  private static final Logger logger = LoggerFactory.getLogger(File.class);
  
  private final SMB2Writer writer;
  
  private static final int FSCTL_SRV_REQUEST_RESUME_KEY = 1310840;
  
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
    return getOutputStream(false);
  }
  
  public OutputStream getOutputStream(boolean append) {
    return getOutputStream((ProgressListener)null, append);
  }
  
  public OutputStream getOutputStream(ProgressListener listener) {
    return getOutputStream(listener, false);
  }
  
  public OutputStream getOutputStream(ProgressListener listener, boolean append) {
    return this.writer.getOutputStream(listener, append ? ((FileStandardInformation)getFileInformation(FileStandardInformation.class)).getEndOfFile() : 0L);
  }
  
  public int read(byte[] buffer, long fileOffset) {
    return read(buffer, fileOffset, 0, buffer.length);
  }
  
  public int read(byte[] buffer, long fileOffset, int offset, int length) {
    SMB2ReadResponse response = this.share.read(this.fileId, fileOffset, length);
    if (response.getHeader().getStatusCode() == NtStatus.STATUS_END_OF_FILE.getValue())
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
  
  public void remoteCopyTo(File destination) throws Buffer.BufferException, TransportException {
    if (destination.share != this.share)
      throw new SMBRuntimeException("Remote copy is only possible between files on the same server"); 
    long fileSize = ((FileStandardInformation)getFileInformation(FileStandardInformation.class)).getEndOfFile();
    remoteCopyTo(0L, destination, 0L, fileSize);
  }
  
  public void remoteCopyTo(long offset, File destination, long destinationOffset, long length) throws Buffer.BufferException, TransportException {
    if (destination.share != this.share)
      throw new SMBRuntimeException("Remote copy is only possible between files on the same server"); 
    remoteFileCopy(this, offset, destination, destinationOffset, length);
  }
  
  private static void remoteFileCopy(File source, long sourceOffset, File destination, long destinationOffset, long length) throws Buffer.BufferException, TransportException {
    byte[] resumeKey = source.getResumeKey();
    long maxChunkSize = 1048576L;
    long maxChunkCount = 16L;
    long maxRequestSize = maxChunkCount * maxChunkSize;
    long srcOff = sourceOffset;
    long dstOff = destinationOffset;
    long remaining = length;
    while (remaining > 0L) {
      CopyChunkRequest request = new CopyChunkRequest(resumeKey, createCopyChunks(srcOff, dstOff, remaining, maxChunkCount, maxChunkSize, maxRequestSize));
      SMB2IoctlResponse ioctlResponse = copyChunk(source.share, destination, request);
      CopyChunkResponse response = new CopyChunkResponse();
      response.read(new SMBBuffer(ioctlResponse.getOutputBuffer()));
      long status = ioctlResponse.getHeader().getStatusCode();
      if (status == NtStatus.STATUS_INVALID_PARAMETER.getValue()) {
        maxChunkCount = response.getChunksWritten();
        long maxSizePerChunk = response.getChunkBytesWritten();
        long maxSizePerRequest = response.getTotalBytesWritten();
        maxChunkSize = Math.min(maxSizePerChunk, maxSizePerRequest);
        continue;
      } 
      long bytesWritten = response.getTotalBytesWritten();
      srcOff += bytesWritten;
      dstOff += bytesWritten;
      remaining -= bytesWritten;
    } 
  }
  
  private byte[] getResumeKey() throws Buffer.BufferException {
    byte[] response = ioctl(1310840, true, new byte[0], 0, 0);
    return Arrays.copyOf(response, 24);
  }
  
  private static List<CopyChunkRequest.Chunk> createCopyChunks(long srcOffset, long dstOffset, long length, long maxChunkCount, long maxChunkSize, long maxRequestSize) {
    List<CopyChunkRequest.Chunk> chunks = new ArrayList<>();
    long remaining = length;
    int chunkCount = 0;
    int totalSize = 0;
    long srcOff = srcOffset;
    long dstOff = dstOffset;
    while (remaining > 0L && chunkCount < maxChunkCount && totalSize < maxRequestSize) {
      long chunkSize = Math.min(remaining, maxChunkSize);
      chunks.add(new CopyChunkRequest.Chunk(srcOff, dstOff, chunkSize));
      chunkCount++;
      totalSize = (int)(totalSize + chunkSize);
      srcOff += chunkSize;
      dstOff += chunkSize;
      remaining -= chunkSize;
    } 
    return chunks;
  }
  
  private static final StatusHandler COPY_CHUNK_ALLOWED_STATUS_VALUES = new StatusHandler() {
      public boolean isSuccess(long statusCode) {
        return (statusCode == NtStatus.STATUS_SUCCESS.getValue() || statusCode == NtStatus.STATUS_INVALID_PARAMETER.getValue());
      }
    };
  
  private static SMB2IoctlResponse copyChunk(Share share, File target, CopyChunkRequest request) {
    SMBBuffer buffer = new SMBBuffer();
    request.write(buffer);
    byte[] data = buffer.getCompactData();
    SMB2IoctlResponse response = share.<SMB2IoctlResponse>receive(share
        .ioctlAsync(target.fileId, CopyChunkRequest.getCtlCode(), true, new ArrayByteChunkProvider(data, 0, data.length, 0L), -1), "IOCTL", target.fileId, COPY_CHUNK_ALLOWED_STATUS_VALUES, share


        
        .getReadTimeout());
    if (response.getError() != null)
      throw new SMBApiException(response.getHeader(), "FSCTL_SRV_COPYCHUNK failed"); 
    return response;
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
    return "File{fileId=" + this.fileId + ", fileName='" + this.fileName + '\'' + '}';
  }
}
