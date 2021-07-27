package com.hierynomus.msdfsc;

import com.hierynomus.mserref.NtStatus;
import com.hierynomus.smbj.share.PathResolveException;

public class DFSException extends PathResolveException {
  public DFSException(NtStatus status, String message) {
    super(status, message);
  }
  
  public DFSException(NtStatus status) {
    super(status);
  }
  
  public DFSException(Throwable cause) {
    super(cause);
  }
}
