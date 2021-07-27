package com.hierynomus.smbj.share;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msdtyp.SecurityInformation;
import com.hierynomus.mserref.NtStatus;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.FileInformationClass;
import com.hierynomus.msfscc.FileSystemInformationClass;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2Dialect;
import com.hierynomus.mssmb2.SMB2FileId;
import com.hierynomus.mssmb2.SMB2ImpersonationLevel;
import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.mssmb2.messages.SMB2Close;
import com.hierynomus.mssmb2.messages.SMB2CreateRequest;
import com.hierynomus.mssmb2.messages.SMB2CreateResponse;
import com.hierynomus.mssmb2.messages.SMB2Flush;
import com.hierynomus.mssmb2.messages.SMB2IoctlRequest;
import com.hierynomus.mssmb2.messages.SMB2IoctlResponse;
import com.hierynomus.mssmb2.messages.SMB2QueryDirectoryRequest;
import com.hierynomus.mssmb2.messages.SMB2QueryDirectoryResponse;
import com.hierynomus.mssmb2.messages.SMB2QueryInfoRequest;
import com.hierynomus.mssmb2.messages.SMB2QueryInfoResponse;
import com.hierynomus.mssmb2.messages.SMB2ReadRequest;
import com.hierynomus.mssmb2.messages.SMB2ReadResponse;
import com.hierynomus.mssmb2.messages.SMB2SetInfoRequest;
import com.hierynomus.mssmb2.messages.SMB2WriteRequest;
import com.hierynomus.mssmb2.messages.SMB2WriteResponse;
import com.hierynomus.protocol.commons.concurrent.Futures;
import com.hierynomus.protocol.transport.TransportException;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.common.SMBRuntimeException;
import com.hierynomus.smbj.common.SmbPath;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.connection.NegotiatedProtocol;
import com.hierynomus.smbj.io.ArrayByteChunkProvider;
import com.hierynomus.smbj.io.ByteChunkProvider;
import com.hierynomus.smbj.io.EmptyByteChunkProvider;
import com.hierynomus.smbj.session.Session;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Share implements AutoCloseable {
  private static final SMB2FileId ROOT_ID = new SMB2FileId(new byte[] { -1, -1, -1, -1, -1, -1, -1, -1 }, new byte[] { -1, -1, -1, -1, -1, -1, -1, -1 });
  
  private static final StatusHandler SUCCESS_OR_SYMLINK = new StatusHandler() {
      public boolean isSuccess(long statusCode) {
        return (statusCode == NtStatus.STATUS_SUCCESS.getValue() || statusCode == NtStatus.STATUS_STOPPED_ON_SYMLINK
          .getValue());
      }
    };
  
  private static final StatusHandler SUCCESS_OR_NO_MORE_FILES_OR_NO_SUCH_FILE = new StatusHandler() {
      public boolean isSuccess(long statusCode) {
        return (statusCode == NtStatus.STATUS_SUCCESS.getValue() || statusCode == NtStatus.STATUS_NO_MORE_FILES
          .getValue() || statusCode == NtStatus.STATUS_NO_SUCH_FILE
          .getValue());
      }
    };
  
  private static final StatusHandler SUCCESS_OR_EOF = new StatusHandler() {
      public boolean isSuccess(long statusCode) {
        return (statusCode == NtStatus.STATUS_SUCCESS.getValue() || statusCode == NtStatus.STATUS_END_OF_FILE
          .getValue());
      }
    };
  
  private static final StatusHandler SUCCESS_OR_CLOSED = new StatusHandler() {
      public boolean isSuccess(long statusCode) {
        return (statusCode == NtStatus.STATUS_SUCCESS.getValue() || statusCode == NtStatus.STATUS_FILE_CLOSED
          .getValue());
      }
    };
  
  protected final SmbPath smbPath;
  
  protected final TreeConnect treeConnect;
  
  private final long treeId;
  
  protected Session session;
  
  private final SMB2Dialect dialect;
  
  private final int readBufferSize;
  
  private final long readTimeout;
  
  private final int writeBufferSize;
  
  private final long writeTimeout;
  
  private final int transactBufferSize;
  
  private final long transactTimeout;
  
  private final long sessionId;
  
  private final AtomicBoolean disconnected = new AtomicBoolean(false);
  
  Share(SmbPath smbPath, TreeConnect treeConnect) {
    this.smbPath = smbPath;
    this.treeConnect = treeConnect;
    this.session = treeConnect.getSession();
    Connection connection = treeConnect.getConnection();
    NegotiatedProtocol negotiatedProtocol = connection.getNegotiatedProtocol();
    this.dialect = negotiatedProtocol.getDialect();
    SmbConfig config = connection.getConfig();
    this.readBufferSize = Math.min(config.getReadBufferSize(), negotiatedProtocol.getMaxReadSize());
    this.readTimeout = config.getReadTimeout();
    this.writeBufferSize = Math.min(config.getWriteBufferSize(), negotiatedProtocol.getMaxWriteSize());
    this.writeTimeout = config.getWriteTimeout();
    this.transactBufferSize = Math.min(config.getTransactBufferSize(), negotiatedProtocol.getMaxTransactSize());
    this.transactTimeout = config.getTransactTimeout();
    this.sessionId = this.session.getSessionId();
    this.treeId = treeConnect.getTreeId();
  }
  
  public void close() throws IOException {
    if (!this.disconnected.getAndSet(true))
      this.treeConnect.close(); 
  }
  
  public boolean isConnected() {
    return !this.disconnected.get();
  }
  
  public SmbPath getSmbPath() {
    return this.smbPath;
  }
  
  public TreeConnect getTreeConnect() {
    return this.treeConnect;
  }
  
  int getReadBufferSize() {
    return this.readBufferSize;
  }
  
  long getReadTimeout() {
    return this.readTimeout;
  }
  
  int getWriteBufferSize() {
    return this.writeBufferSize;
  }
  
  SMB2FileId openFileId(SmbPath path, SMB2ImpersonationLevel impersonationLevel, Set<AccessMask> accessMask, Set<FileAttributes> fileAttributes, Set<SMB2ShareAccess> shareAccess, SMB2CreateDisposition createDisposition, Set<SMB2CreateOptions> createOptions) {
    return createFile(path, impersonationLevel, accessMask, fileAttributes, shareAccess, createDisposition, createOptions).getFileId();
  }
  
  SMB2CreateResponse createFile(SmbPath path, SMB2ImpersonationLevel impersonationLevel, Set<AccessMask> accessMask, Set<FileAttributes> fileAttributes, Set<SMB2ShareAccess> shareAccess, SMB2CreateDisposition createDisposition, Set<SMB2CreateOptions> createOptions) {
    SMB2CreateRequest cr = new SMB2CreateRequest(this.dialect, this.sessionId, this.treeId, impersonationLevel, accessMask, fileAttributes, shareAccess, createDisposition, createOptions, path);
    SMB2CreateResponse resp = sendReceive(cr, "Create", path, getCreateStatusHandler(), this.transactTimeout);
    return resp;
  }
  
  protected StatusHandler getCreateStatusHandler() {
    return SUCCESS_OR_SYMLINK;
  }
  
  void flush(SMB2FileId fileId) throws SMBApiException {
    SMB2Flush flushReq = new SMB2Flush(this.dialect, fileId, this.sessionId, this.treeId);
    sendReceive(flushReq, "Flush", fileId, StatusHandler.SUCCESS, this.writeTimeout);
  }
  
  void closeFileId(SMB2FileId fileId) throws SMBApiException {
    SMB2Close closeReq = new SMB2Close(this.dialect, this.sessionId, this.treeId, fileId);
    sendReceive(closeReq, "Close", fileId, SUCCESS_OR_CLOSED, this.transactTimeout);
  }
  
  SMB2QueryInfoResponse queryInfo(SMB2FileId fileId, SMB2QueryInfoRequest.SMB2QueryInfoType infoType, Set<SecurityInformation> securityInfo, FileInformationClass fileInformationClass, FileSystemInformationClass fileSystemInformationClass) {
    SMB2QueryInfoRequest qreq = new SMB2QueryInfoRequest(this.dialect, this.sessionId, this.treeId, fileId, infoType, fileInformationClass, fileSystemInformationClass, null, securityInfo);
    return sendReceive(qreq, "QueryInfo", fileId, StatusHandler.SUCCESS, this.transactTimeout);
  }
  
  void setInfo(SMB2FileId fileId, SMB2SetInfoRequest.SMB2InfoType infoType, Set<SecurityInformation> securityInfo, FileInformationClass fileInformationClass, byte[] buffer) {
    SMB2SetInfoRequest qreq = new SMB2SetInfoRequest(this.dialect, this.sessionId, this.treeId, infoType, fileId, fileInformationClass, securityInfo, buffer);
    sendReceive(qreq, "SetInfo", fileId, StatusHandler.SUCCESS, this.transactTimeout);
  }
  
  SMB2QueryDirectoryResponse queryDirectory(SMB2FileId fileId, Set<SMB2QueryDirectoryRequest.SMB2QueryDirectoryFlags> flags, FileInformationClass informationClass, String searchPattern) {
    SMB2QueryDirectoryRequest qdr = new SMB2QueryDirectoryRequest(this.dialect, this.sessionId, this.treeId, fileId, informationClass, flags, 0L, searchPattern, this.transactBufferSize);
    return sendReceive(qdr, "Query directory", fileId, SUCCESS_OR_NO_MORE_FILES_OR_NO_SUCH_FILE, this.transactTimeout);
  }
  
  SMB2WriteResponse write(SMB2FileId fileId, ByteChunkProvider provider) {
    SMB2WriteRequest wreq = new SMB2WriteRequest(this.dialect, fileId, this.sessionId, this.treeId, provider, this.writeBufferSize);
    return sendReceive(wreq, "Write", fileId, StatusHandler.SUCCESS, this.writeTimeout);
  }
  
  SMB2ReadResponse read(SMB2FileId fileId, long offset, int length) {
    return receive(
        readAsync(fileId, offset, length), "Read", fileId, SUCCESS_OR_EOF, this.readTimeout);
  }
  
  Future<SMB2ReadResponse> readAsync(SMB2FileId fileId, long offset, int length) {
    SMB2ReadRequest rreq = new SMB2ReadRequest(this.dialect, fileId, this.sessionId, this.treeId, offset, Math.min(length, this.readBufferSize));
    return send(rreq);
  }
  
  private static final EmptyByteChunkProvider EMPTY = new EmptyByteChunkProvider(0L);
  
  public byte[] ioctl(long ctlCode, boolean isFsCtl, byte[] inData) {
    return ioctl(ROOT_ID, ctlCode, isFsCtl, inData, 0, inData.length);
  }
  
  public byte[] ioctl(long ctlCode, boolean isFsCtl, byte[] inData, int inOffset, int inLength) {
    return ioctl(ROOT_ID, ctlCode, isFsCtl, inData, inOffset, inLength);
  }
  
  public int ioctl(long ctlCode, boolean isFsCtl, byte[] inData, int inOffset, int inLength, byte[] outData, int outOffset, int outLength) {
    return ioctl(ROOT_ID, ctlCode, isFsCtl, inData, inOffset, inLength, outData, outOffset, outLength);
  }
  
  byte[] ioctl(SMB2FileId fileId, long ctlCode, boolean isFsCtl, byte[] inData, int inOffset, int inLength) {
    return ioctl(fileId, ctlCode, isFsCtl, inData, inOffset, inLength, -1);
  }
  
  byte[] ioctl(SMB2FileId fileId, long ctlCode, boolean isFsCtl, byte[] inData, int inOffset, int inLength, int maxOutputResponse) {
    SMB2IoctlResponse response = ioctl(fileId, ctlCode, isFsCtl, new ArrayByteChunkProvider(inData, inOffset, inLength, 0L), maxOutputResponse);
    return response.getOutputBuffer();
  }
  
  int ioctl(SMB2FileId fileId, long ctlCode, boolean isFsCtl, byte[] inData, int inOffset, int inLength, byte[] outData, int outOffset, int outLength) {
    SMB2IoctlResponse response = ioctl(fileId, ctlCode, isFsCtl, new ArrayByteChunkProvider(inData, inOffset, inLength, 0L), outLength);
    int length = 0;
    if (outData != null) {
      byte[] outputBuffer = response.getOutputBuffer();
      length = Math.min(outLength, outputBuffer.length);
      System.arraycopy(outputBuffer, 0, outData, outOffset, length);
    } 
    return length;
  }
  
  SMB2IoctlResponse ioctl(SMB2FileId fileId, long ctlCode, boolean isFsCtl, ByteChunkProvider inputData, int maxOutputResponse) {
    Future<SMB2IoctlResponse> fut = ioctlAsync(fileId, ctlCode, isFsCtl, inputData, maxOutputResponse);
    return receive(fut, "IOCTL", fileId, StatusHandler.SUCCESS, this.transactTimeout);
  }
  
  public Future<SMB2IoctlResponse> ioctlAsync(long ctlCode, boolean isFsCtl, ByteChunkProvider inputData) {
    return ioctlAsync(ROOT_ID, ctlCode, isFsCtl, inputData, -1);
  }
  
  Future<SMB2IoctlResponse> ioctlAsync(SMB2FileId fileId, long ctlCode, boolean isFsCtl, ByteChunkProvider inputData, int maxOutputResponse) {
    int maxResponse;
    ByteChunkProvider inData = (inputData == null) ? EMPTY : inputData;
    if (inData.bytesLeft() > this.transactBufferSize)
      throw new SMBRuntimeException("Input data size exceeds maximum allowed by server: " + inData.bytesLeft() + " > " + this.transactBufferSize); 
    if (maxOutputResponse < 0) {
      maxResponse = this.transactBufferSize;
    } else {
      if (maxOutputResponse > this.transactBufferSize)
        throw new SMBRuntimeException("Output data size exceeds maximum allowed by server: " + maxOutputResponse + " > " + this.transactBufferSize); 
      maxResponse = maxOutputResponse;
    } 
    SMB2IoctlRequest ioreq = new SMB2IoctlRequest(this.dialect, this.sessionId, this.treeId, ctlCode, fileId, inData, isFsCtl, maxResponse);
    return send(ioreq);
  }
  
  private <T extends SMB2Packet> T sendReceive(SMB2Packet request, String name, Object target, StatusHandler statusHandler, long timeout) {
    Future<T> fut = send(request);
    return receive(fut, name, target, statusHandler, timeout);
  }
  
  private <T extends SMB2Packet> Future<T> send(SMB2Packet request) {
    if (!isConnected())
      throw new SMBRuntimeException(getClass().getSimpleName() + " has already been closed"); 
    try {
      return this.session.send(request);
    } catch (TransportException e) {
      throw new SMBRuntimeException(e);
    } 
  }
  
  <T extends SMB2Packet> T receive(Future<T> fut, String name, Object target, StatusHandler statusHandler, long timeout) {
    T resp = receive(fut, timeout);
    long status = resp.getHeader().getStatusCode();
    if (!statusHandler.isSuccess(status))
      throw new SMBApiException(resp.getHeader(), name + " failed for " + target); 
    return resp;
  }
  
  <T extends SMB2Packet> T receive(Future<T> fut, long timeout) {
    SMB2Packet sMB2Packet;
    try {
      if (timeout > 0L) {
        sMB2Packet = Futures.<SMB2Packet, TransportException>get(fut, timeout, TimeUnit.MILLISECONDS, TransportException.Wrapper);
      } else {
        sMB2Packet = Futures.<SMB2Packet, TransportException>get(fut, TransportException.Wrapper);
      } 
    } catch (TransportException e) {
      throw new SMBRuntimeException(e);
    } 
    return (T)sMB2Packet;
  }
}
