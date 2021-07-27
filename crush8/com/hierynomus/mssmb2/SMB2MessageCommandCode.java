package com.hierynomus.mssmb2;

public enum SMB2MessageCommandCode {
  SMB2_NEGOTIATE(0),
  SMB2_SESSION_SETUP(1),
  SMB2_LOGOFF(2),
  SMB2_TREE_CONNECT(3),
  SMB2_TREE_DISCONNECT(4),
  SMB2_CREATE(5),
  SMB2_CLOSE(6),
  SMB2_FLUSH(7),
  SMB2_READ(8),
  SMB2_WRITE(9),
  SMB2_LOCK(10),
  SMB2_IOCTL(11),
  SMB2_CANCEL(12),
  SMB2_ECHO(13),
  SMB2_QUERY_DIRECTORY(14),
  SMB2_CHANGE_NOTIFY(15),
  SMB2_QUERY_INFO(16),
  SMB2_SET_INFO(17),
  SMB2_OPLOCK_BREAK(18);
  
  private static final SMB2MessageCommandCode[] cache;
  
  private int value;
  
  static {
    cache = new SMB2MessageCommandCode[19];
    for (SMB2MessageCommandCode smb2MessageCommandCode : values())
      cache[smb2MessageCommandCode.getValue()] = smb2MessageCommandCode; 
  }
  
  SMB2MessageCommandCode(int value) {
    this.value = value;
  }
  
  public int getValue() {
    return this.value;
  }
  
  public static SMB2MessageCommandCode lookup(int value) {
    return cache[value];
  }
}
