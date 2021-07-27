package com.sun.mail.imap;

import com.sun.mail.imap.protocol.UIDSet;

public class ResyncData {
  private long uidvalidity = -1L;
  
  private long modseq = -1L;
  
  private UIDSet[] uids = null;
  
  public static final ResyncData CONDSTORE = new ResyncData(-1L, -1L);
  
  public ResyncData(long uidvalidity, long modseq) {
    this.uidvalidity = uidvalidity;
    this.modseq = modseq;
    this.uids = null;
  }
  
  public ResyncData(long uidvalidity, long modseq, long uidFirst, long uidLast) {
    this.uidvalidity = uidvalidity;
    this.modseq = modseq;
    this.uids = new UIDSet[] { new UIDSet(uidFirst, uidLast) };
  }
  
  public ResyncData(long uidvalidity, long modseq, long[] uids) {
    this.uidvalidity = uidvalidity;
    this.modseq = modseq;
    this.uids = UIDSet.createUIDSets(uids);
  }
  
  public long getUIDValidity() {
    return this.uidvalidity;
  }
  
  public long getModSeq() {
    return this.modseq;
  }
  
  UIDSet[] getUIDSet() {
    return this.uids;
  }
}
