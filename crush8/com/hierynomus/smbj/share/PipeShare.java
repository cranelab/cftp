package com.hierynomus.smbj.share;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.mserref.NtStatus;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.fsctl.FsCtlPipeWaitRequest;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2FileId;
import com.hierynomus.mssmb2.SMB2ImpersonationLevel;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.mssmb2.messages.SMB2IoctlResponse;
import com.hierynomus.smb.SMBBuffer;
import com.hierynomus.smbj.common.SmbPath;
import com.hierynomus.smbj.io.ArrayByteChunkProvider;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class PipeShare extends Share {
  private static final int FSCTL_PIPE_WAIT = 1114136;
  
  public PipeShare(SmbPath smbPath, TreeConnect treeConnect) {
    super(smbPath, treeConnect);
  }
  
  public boolean waitForPipe(String name) {
    return waitForPipe(name, 0L, TimeUnit.MILLISECONDS);
  }
  
  public boolean waitForPipe(String name, long timeout, TimeUnit timeoutUnit) {
    long timeoutMs;
    SMBBuffer buffer = new SMBBuffer();
    (new FsCtlPipeWaitRequest(name, timeout, timeoutUnit, (timeout > 0L))).write(buffer);
    Future<SMB2IoctlResponse> responseFuture = ioctlAsync(1114136L, true, new ArrayByteChunkProvider(buffer.getCompactData(), 0L));
    if (timeout > 0L) {
      timeoutMs = timeoutUnit.toMillis(timeout) + 20L;
    } else {
      timeoutMs = 0L;
    } 
    SMB2IoctlResponse response = receive(responseFuture, timeoutMs);
    long status = response.getHeader().getStatusCode();
    if (status == NtStatus.STATUS_SUCCESS.getValue())
      return true; 
    if (status == NtStatus.STATUS_IO_TIMEOUT.getValue())
      return false; 
    throw new SMBApiException(response.getHeader(), "Error while waiting for pipe " + name);
  }
  
  public NamedPipe open(String name, SMB2ImpersonationLevel impersonationLevel, Set<AccessMask> accessMask, Set<FileAttributes> attributes, Set<SMB2ShareAccess> shareAccesses, SMB2CreateDisposition createDisposition, Set<SMB2CreateOptions> createOptions) {
    SmbPath path = new SmbPath(this.smbPath, name);
    SMB2FileId response = openFileId(path, impersonationLevel, accessMask, attributes, shareAccesses, createDisposition, createOptions);
    return new NamedPipe(response, this, path);
  }
  
  public SMB2FileId openFileId(String name, SMB2ImpersonationLevel impersonationLevel, Set<AccessMask> accessMask, Set<FileAttributes> fileAttributes, Set<SMB2ShareAccess> shareAccess, SMB2CreateDisposition createDisposition, Set<SMB2CreateOptions> createOptions) {
    SmbPath path = new SmbPath(this.smbPath, name);
    return openFileId(path, impersonationLevel, accessMask, fileAttributes, shareAccess, createDisposition, createOptions);
  }
  
  public void closeFileId(SMB2FileId fileId) throws SMBApiException {
    super.closeFileId(fileId);
  }
}
