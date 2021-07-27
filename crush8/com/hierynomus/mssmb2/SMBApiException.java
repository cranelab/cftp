package com.hierynomus.mssmb2;

import com.hierynomus.mserref.NtStatus;
import com.hierynomus.smbj.common.SMBRuntimeException;

public class SMBApiException extends SMBRuntimeException {
  private final SMB2MessageCommandCode failedCommand;
  
  private long statusCode;
  
  public SMBApiException(long status, SMB2MessageCommandCode failedCommand, Throwable t) {
    super(t);
    this.statusCode = status;
    this.failedCommand = failedCommand;
  }
  
  public SMBApiException(long status, SMB2MessageCommandCode failedCommand, String message, Throwable t) {
    super(message, t);
    this.statusCode = status;
    this.failedCommand = failedCommand;
  }
  
  public SMBApiException(SMB2Header header, String message) {
    super(message);
    this.statusCode = header.getStatusCode();
    this.failedCommand = header.getMessage();
  }
  
  public SMBApiException(SMB2Header header, String message, Throwable t) {
    super(message, t);
    this.statusCode = header.getStatusCode();
    this.failedCommand = header.getMessage();
  }
  
  public NtStatus getStatus() {
    return NtStatus.valueOf(this.statusCode);
  }
  
  public long getStatusCode() {
    return this.statusCode;
  }
  
  public SMB2MessageCommandCode getFailedCommand() {
    return this.failedCommand;
  }
  
  public String getMessage() {
    return String.format("%s (0x%08x): %s", new Object[] { getStatus().name(), Long.valueOf(this.statusCode), super.getMessage() });
  }
}
