package com.hierynomus.smbj.share;

import com.hierynomus.mserref.NtStatus;

public interface StatusHandler {
  public static final StatusHandler SUCCESS = new StatusHandler() {
      public boolean isSuccess(long statusCode) {
        return (statusCode == NtStatus.STATUS_SUCCESS.getValue());
      }
    };
  
  boolean isSuccess(long paramLong);
}
