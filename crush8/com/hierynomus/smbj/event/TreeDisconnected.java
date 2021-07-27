package com.hierynomus.smbj.event;

public class TreeDisconnected extends SessionEvent implements SMBEvent {
  private long treeId;
  
  public TreeDisconnected(long sessionId, long treeId) {
    super(sessionId);
    this.treeId = treeId;
  }
  
  public long getTreeId() {
    return this.treeId;
  }
}
