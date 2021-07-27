package com.hierynomus.smbj.event;

abstract class SessionEvent implements SMBEvent {
  private long sessionId;
  
  public SessionEvent(long sessionId) {
    this.sessionId = sessionId;
  }
  
  public long getSessionId() {
    return this.sessionId;
  }
  
  public boolean equals(Object o) {
    if (this == o)
      return true; 
    if (o == null || getClass() != o.getClass())
      return false; 
    SessionEvent that = (SessionEvent)o;
    return (this.sessionId == that.sessionId);
  }
  
  public int hashCode() {
    return (int)(this.sessionId ^ this.sessionId >>> 32L);
  }
}
