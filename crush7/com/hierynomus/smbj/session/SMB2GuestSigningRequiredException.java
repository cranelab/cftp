package com.hierynomus.smbj.session;

import com.hierynomus.smbj.common.SMBRuntimeException;

public class SMB2GuestSigningRequiredException extends SMBRuntimeException {
  public SMB2GuestSigningRequiredException() {
    super("Cannot require message signing when authenticating with a guest account");
  }
}
