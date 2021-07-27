package com.hierynomus.smbj.session;

import com.hierynomus.smbj.share.Share;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class TreeConnectTable {
  private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  
  private Map<Long, Share> lookupById = new HashMap<>();
  
  private Map<String, Share> lookupByShareName = new HashMap<>();
  
  void register(Share share) {
    this.lock.writeLock().lock();
    try {
      this.lookupById.put(Long.valueOf(share.getTreeConnect().getTreeId()), share);
      this.lookupByShareName.put(share.getTreeConnect().getShareName(), share);
    } finally {
      this.lock.writeLock().unlock();
    } 
  }
  
  Collection<Share> getOpenTreeConnects() {
    this.lock.readLock().lock();
    try {
      return new ArrayList(this.lookupById.values());
    } finally {
      this.lock.readLock().unlock();
    } 
  }
  
  Share getTreeConnect(long treeConnectId) {
    this.lock.readLock().lock();
    try {
      return this.lookupById.get(Long.valueOf(treeConnectId));
    } finally {
      this.lock.readLock().unlock();
    } 
  }
  
  Share getTreeConnect(String shareName) {
    this.lock.readLock().lock();
    try {
      return this.lookupByShareName.get(shareName);
    } finally {
      this.lock.readLock().unlock();
    } 
  }
  
  void closed(long treeConnectId) {
    this.lock.writeLock().lock();
    try {
      Share share = this.lookupById.remove(Long.valueOf(treeConnectId));
      if (share != null)
        this.lookupByShareName.remove(share.getTreeConnect().getShareName()); 
    } finally {
      this.lock.writeLock().unlock();
    } 
  }
}
