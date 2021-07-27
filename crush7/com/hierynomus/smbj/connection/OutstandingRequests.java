package com.hierynomus.smbj.connection;

import com.hierynomus.smbj.common.SMBRuntimeException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class OutstandingRequests {
  private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  
  private Map<Long, Request> lookup = new HashMap<Long, Request>();
  
  private Map<UUID, Request> cancelLookup = new HashMap<UUID, Request>();
  
  boolean isOutstanding(Long messageId) {
    this.lock.readLock().lock();
    try {
      return this.lookup.containsKey(messageId);
    } finally {
      this.lock.readLock().unlock();
    } 
  }
  
  Request getRequestByMessageId(Long messageId) {
    this.lock.readLock().lock();
    try {
      return this.lookup.get(messageId);
    } finally {
      this.lock.readLock().unlock();
    } 
  }
  
  Request getRequestByCancelId(UUID cancelId) {
    this.lock.readLock().lock();
    try {
      return this.cancelLookup.get(cancelId);
    } finally {
      this.lock.readLock().unlock();
    } 
  }
  
  Request receivedResponseFor(Long messageId) {
    this.lock.writeLock().lock();
    try {
      Request r = this.lookup.remove(messageId);
      if (r == null)
        throw new SMBRuntimeException("Unable to find outstanding request for messageId " + messageId); 
      this.cancelLookup.remove(r.getCancelId());
      return r;
    } finally {
      this.lock.writeLock().unlock();
    } 
  }
  
  void registerOutstanding(Request request) {
    this.lock.writeLock().lock();
    try {
      this.lookup.put(Long.valueOf(request.getMessageId()), request);
      this.cancelLookup.put(request.getCancelId(), request);
    } finally {
      this.lock.writeLock().unlock();
    } 
  }
  
  void handleError(Throwable t) {
    this.lock.writeLock().lock();
    try {
      for (Long id : new HashSet(this.lookup.keySet())) {
        Request removed = this.lookup.remove(id);
        this.cancelLookup.remove(removed.getCancelId());
        removed.getPromise().deliverError(t);
      } 
    } finally {
      this.lock.writeLock().unlock();
    } 
  }
}
