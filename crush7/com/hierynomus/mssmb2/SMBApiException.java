package com.hierynomus.mssmb2;

import com.hierynomus.mserref.NtStatus;
import com.hierynomus.smbj.common.SMBRuntimeException;

public class SMBApiException extends SMBRuntimeException {
  private final NtStatus status;
  
  private final SMB2MessageCommandCode failedCommand;
  
  private long statusCode;
  
  public SMBApiException(NtStatus status, SMB2MessageCommandCode failedCommand, Throwable t) {
    super(t);
    this.status = status;
    this.statusCode = status.getValue();
    this.failedCommand = failedCommand;
  }
  
  public SMBApiException(SMB2Header header, String message) {
    super(message);
    this.status = header.getStatus();
    this.statusCode = header.getStatusCode();
    this.failedCommand = header.getMessage();
  }
  
  public SMBApiException(SMB2Header header, String message, Throwable t) {
    super(message, t);
    this.status = header.getStatus();
    this.statusCode = header.getStatusCode();
    this.failedCommand = header.getMessage();
  }
  
  public NtStatus getStatus() {
    return this.status;
  }
  
  public long getStatusCode() {
    return this.statusCode;
  }
  
  public SMB2MessageCommandCode getFailedCommand() {
    return this.failedCommand;
  }
  
  public String getMessage() {
    return this.status + "(" + this.status.getValue() + "/" + this.statusCode + "): " + super.getMessage();
  }
}
