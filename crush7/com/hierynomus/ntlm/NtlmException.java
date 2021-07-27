package com.hierynomus.ntlm;

public class NtlmException extends RuntimeException {
  public NtlmException() {}
  
  public NtlmException(String message) {
    super(message);
  }
  
  public NtlmException(String message, Throwable cause) {
    super(message, cause);
  }
  
  public NtlmException(Throwable cause) {
    super(cause);
  }
}
