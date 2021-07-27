package com.hierynomus.smbj.paths;

import com.hierynomus.mserref.NtStatus;

public class PathResolveException extends Exception {
  private final long status;
  
  public PathResolveException(long status) {
    this.status = status;
  }
  
  public PathResolveException(long status, String message) {
    super(message);
    this.status = status;
  }
  
  public PathResolveException(Throwable cause) {
    super(cause);
    this.status = NtStatus.STATUS_OTHER.getValue();
  }
  
  public long getStatusCode() {
    return this.status;
  }
  
  public NtStatus getStatus() {
    return NtStatus.valueOf(this.status);
  }
}
