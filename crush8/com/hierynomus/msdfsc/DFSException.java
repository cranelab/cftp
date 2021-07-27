package com.hierynomus.msdfsc;

import com.hierynomus.smbj.paths.PathResolveException;

public class DFSException extends PathResolveException {
  public DFSException(long status, String message) {
    super(status, message);
  }
  
  public DFSException(long status) {
    super(status);
  }
  
  public DFSException(Throwable cause) {
    super(cause);
  }
}
