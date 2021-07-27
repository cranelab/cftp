package com.hierynomus.smbj.connection;

import com.hierynomus.smbj.session.Session;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

class SessionTable {
  private ReentrantLock lock = new ReentrantLock();
  
  private Map<Long, Session> lookup = new HashMap<Long, Session>();
  
  void registerSession(Long id, Session session) {
    this.lock.lock();
    try {
      this.lookup.put(id, session);
    } finally {
      this.lock.unlock();
    } 
  }
  
  Session find(Long id) {
    this.lock.lock();
    try {
      return this.lookup.get(id);
    } finally {
      this.lock.unlock();
    } 
  }
  
  Session sessionClosed(Long id) {
    this.lock.lock();
    try {
      return this.lookup.remove(id);
    } finally {
      this.lock.unlock();
    } 
  }
  
  boolean isActive(Long id) {
    this.lock.lock();
    try {
      return this.lookup.containsKey(id);
    } finally {
      this.lock.unlock();
    } 
  }
  
  Collection<Session> activeSessions() {
    this.lock.lock();
    try {
      return new ArrayList(this.lookup.values());
    } finally {
      this.lock.unlock();
    } 
  }
}
