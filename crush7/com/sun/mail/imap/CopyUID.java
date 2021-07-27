package com.sun.mail.imap;

import com.sun.mail.imap.protocol.UIDSet;

public class CopyUID {
  public long uidvalidity = -1L;
  
  public UIDSet[] src;
  
  public UIDSet[] dst;
  
  public CopyUID(long uidvalidity, UIDSet[] src, UIDSet[] dst) {
    this.uidvalidity = uidvalidity;
    this.src = src;
    this.dst = dst;
  }
}
