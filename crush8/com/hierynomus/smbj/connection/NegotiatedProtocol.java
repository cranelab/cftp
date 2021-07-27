package com.hierynomus.smbj.connection;

import com.hierynomus.mssmb2.SMB2Dialect;

public class NegotiatedProtocol {
  private SMB2Dialect dialect;
  
  private int maxTransactSize;
  
  private int maxReadSize;
  
  private int maxWriteSize;
  
  public NegotiatedProtocol(SMB2Dialect dialect, int maxTransactSize, int maxReadSize, int maxWriteSize, boolean supportsMultiCredit) {
    this.dialect = dialect;
    this.maxTransactSize = supportsMultiCredit ? maxTransactSize : Math.max(maxTransactSize, 65536);
    this.maxReadSize = supportsMultiCredit ? maxReadSize : Math.max(maxReadSize, 65536);
    this.maxWriteSize = supportsMultiCredit ? maxWriteSize : Math.max(maxWriteSize, 65536);
  }
  
  public SMB2Dialect getDialect() {
    return this.dialect;
  }
  
  public int getMaxTransactSize() {
    return this.maxTransactSize;
  }
  
  public int getMaxReadSize() {
    return this.maxReadSize;
  }
  
  public int getMaxWriteSize() {
    return this.maxWriteSize;
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder("NegotiatedProtocol{");
    sb.append("dialect=").append(this.dialect);
    sb.append(", maxTransactSize=").append(this.maxTransactSize);
    sb.append(", maxReadSize=").append(this.maxReadSize);
    sb.append(", maxWriteSize=").append(this.maxWriteSize);
    sb.append('}');
    return sb.toString();
  }
}
