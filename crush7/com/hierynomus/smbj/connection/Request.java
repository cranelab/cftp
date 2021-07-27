package com.hierynomus.smbj.connection;

import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.protocol.commons.concurrent.AFuture;
import com.hierynomus.protocol.commons.concurrent.CancellableFuture;
import com.hierynomus.protocol.commons.concurrent.Promise;
import com.hierynomus.smbj.common.SMBRuntimeException;
import java.util.Date;
import java.util.UUID;

class Request {
  private final Promise<SMB2Packet, SMBRuntimeException> promise;
  
  private final long messageId;
  
  private final UUID cancelId;
  
  private final Date timestamp;
  
  private long asyncId;
  
  public long getAsyncId() {
    return this.asyncId;
  }
  
  public void setAsyncId(long asyncId) {
    this.asyncId = asyncId;
  }
  
  public Request(long messageId, UUID cancelId) {
    this.messageId = messageId;
    this.cancelId = cancelId;
    this.timestamp = new Date();
    this.promise = new Promise<SMB2Packet, SMBRuntimeException>(String.valueOf(messageId), SMBRuntimeException.Wrapper);
  }
  
  Promise<SMB2Packet, SMBRuntimeException> getPromise() {
    return this.promise;
  }
  
  long getMessageId() {
    return this.messageId;
  }
  
  <T extends SMB2Packet> AFuture<T> getFuture(CancellableFuture.CancelCallback callback) {
    return new CancellableFuture<T>((AFuture)this.promise.future(), callback);
  }
  
  UUID getCancelId() {
    return this.cancelId;
  }
  
  public Date getTimestamp() {
    return this.timestamp;
  }
}
