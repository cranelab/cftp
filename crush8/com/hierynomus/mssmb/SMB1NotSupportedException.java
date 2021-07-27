package com.hierynomus.mssmb;

import com.hierynomus.protocol.transport.TransportException;

public class SMB1NotSupportedException extends TransportException {
  public SMB1NotSupportedException() {
    super("SMBv1 is not supported by SMBJ");
  }
}
