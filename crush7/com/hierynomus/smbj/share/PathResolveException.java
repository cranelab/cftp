package com.hierynomus.smbj.share;

import com.hierynomus.mserref.NtStatus;

public class PathResolveException extends Exception {
  private final NtStatus status;
  
  public PathResolveException(NtStatus status) {
    this.status = status;
  }
  
  public PathResolveException(NtStatus status, String message) {
    super(message);
    this.status = status;
  }
  
  public PathResolveException(Throwable cause) {
    super(cause);
    this.status = NtStatus.UNKNOWN;
  }
  
  public NtStatus getStatus() {
    return this.status;
  }
}
