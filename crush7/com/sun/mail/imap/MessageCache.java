package com.sun.mail.imap;

import com.sun.mail.util.MailLogger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import javax.mail.Message;

public class MessageCache {
  private IMAPMessage[] messages;
  
  private int[] seqnums;
  
  private int size;
  
  private IMAPFolder folder;
  
  private MailLogger logger;
  
  private static final int SLOP = 64;
  
  MessageCache(IMAPFolder folder, IMAPStore store, int size) {
    this.folder = folder;
    this.logger = folder.logger.getSubLogger("messagecache", "DEBUG IMAP MC", store
        .getMessageCacheDebug());
    if (this.logger.isLoggable(Level.CONFIG))
      this.logger.config("create cache of size " + size); 
    ensureCapacity(size, 1);
  }
  
  MessageCache(int size, boolean debug) {
    this.folder = null;
    this
      .logger = new MailLogger(getClass(), "messagecache", "DEBUG IMAP MC", debug, System.out);
    if (this.logger.isLoggable(Level.CONFIG))
      this.logger.config("create DEBUG cache of size " + size); 
    ensureCapacity(size, 1);
  }
  
  public int size() {
    return this.size;
  }
  
  public IMAPMessage getMessage(int msgnum) {
    if (msgnum < 1 || msgnum > this.size)
      throw new ArrayIndexOutOfBoundsException("message number (" + msgnum + ") out of bounds (" + this.size + ")"); 
    IMAPMessage msg = this.messages[msgnum - 1];
    if (msg == null) {
      if (this.logger.isLoggable(Level.FINE))
        this.logger.fine("create message number " + msgnum); 
      msg = this.folder.newIMAPMessage(msgnum);
      this.messages[msgnum - 1] = msg;
      if (seqnumOf(msgnum) <= 0) {
        this.logger.fine("it's expunged!");
        msg.setExpunged(true);
      } 
    } 
    return msg;
  }
  
  public IMAPMessage getMessageBySeqnum(int seqnum) {
    int msgnum = msgnumOf(seqnum);
    if (msgnum < 0) {
      if (this.logger.isLoggable(Level.FINE))
        this.logger.fine("no message seqnum " + seqnum); 
      return null;
    } 
    return getMessage(msgnum);
  }
  
  public void expungeMessage(int seqnum) {
    int msgnum = msgnumOf(seqnum);
    if (msgnum < 0) {
      if (this.logger.isLoggable(Level.FINE))
        this.logger.fine("expunge no seqnum " + seqnum); 
      return;
    } 
    IMAPMessage msg = this.messages[msgnum - 1];
    if (msg != null) {
      if (this.logger.isLoggable(Level.FINE))
        this.logger.fine("expunge existing " + msgnum); 
      msg.setExpunged(true);
    } 
    if (this.seqnums == null) {
      this.logger.fine("create seqnums array");
      this.seqnums = new int[this.messages.length];
      int i;
      for (i = 1; i < msgnum; i++)
        this.seqnums[i - 1] = i; 
      this.seqnums[msgnum - 1] = 0;
      for (i = msgnum + 1; i <= this.seqnums.length; i++)
        this.seqnums[i - 1] = i - 1; 
    } else {
      this.seqnums[msgnum - 1] = 0;
      for (int i = msgnum + 1; i <= this.seqnums.length; i++) {
        assert this.seqnums[i - 1] != 1;
        if (this.seqnums[i - 1] > 0)
          this.seqnums[i - 1] = this.seqnums[i - 1] - 1; 
      } 
    } 
  }
  
  public IMAPMessage[] removeExpungedMessages() {
    this.logger.fine("remove expunged messages");
    List<IMAPMessage> mlist = new ArrayList<IMAPMessage>();
    int oldnum = 1;
    int newnum = 1;
    while (oldnum <= this.size) {
      if (seqnumOf(oldnum) <= 0) {
        IMAPMessage m = getMessage(oldnum);
        mlist.add(m);
      } else {
        if (newnum != oldnum) {
          this.messages[newnum - 1] = this.messages[oldnum - 1];
          if (this.messages[newnum - 1] != null)
            this.messages[newnum - 1].setMessageNumber(newnum); 
        } 
        newnum++;
      } 
      oldnum++;
    } 
    this.seqnums = null;
    shrink(newnum, oldnum);
    IMAPMessage[] rmsgs = new IMAPMessage[mlist.size()];
    if (this.logger.isLoggable(Level.FINE))
      this.logger.fine("return " + rmsgs.length); 
    mlist.toArray(rmsgs);
    return rmsgs;
  }
  
  public IMAPMessage[] removeExpungedMessages(Message[] msgs) {
    this.logger.fine("remove expunged messages");
    List<IMAPMessage> mlist = new ArrayList<IMAPMessage>();
    int[] mnum = new int[msgs.length];
    for (int i = 0; i < msgs.length; i++)
      mnum[i] = msgs[i].getMessageNumber(); 
    Arrays.sort(mnum);
    int oldnum = 1;
    int newnum = 1;
    int mnumi = 0;
    boolean keepSeqnums = false;
    while (oldnum <= this.size) {
      if (mnumi < mnum.length && oldnum == mnum[mnumi] && 
        
        seqnumOf(oldnum) <= 0) {
        IMAPMessage m = getMessage(oldnum);
        mlist.add(m);
        while (mnumi < mnum.length && mnum[mnumi] <= oldnum)
          mnumi++; 
      } else {
        if (newnum != oldnum) {
          this.messages[newnum - 1] = this.messages[oldnum - 1];
          if (this.messages[newnum - 1] != null)
            this.messages[newnum - 1].setMessageNumber(newnum); 
          if (this.seqnums != null)
            this.seqnums[newnum - 1] = this.seqnums[oldnum - 1]; 
        } 
        if (this.seqnums != null && this.seqnums[newnum - 1] != newnum)
          keepSeqnums = true; 
        newnum++;
      } 
      oldnum++;
    } 
    if (!keepSeqnums)
      this.seqnums = null; 
    shrink(newnum, oldnum);
    IMAPMessage[] rmsgs = new IMAPMessage[mlist.size()];
    if (this.logger.isLoggable(Level.FINE))
      this.logger.fine("return " + rmsgs.length); 
    mlist.toArray(rmsgs);
    return rmsgs;
  }
  
  private void shrink(int newend, int oldend) {
    this.size = newend - 1;
    if (this.logger.isLoggable(Level.FINE))
      this.logger.fine("size now " + this.size); 
    if (this.size == 0) {
      this.messages = null;
      this.seqnums = null;
    } else if (this.size > 64 && this.size < this.messages.length / 2) {
      this.logger.fine("reallocate array");
      IMAPMessage[] newm = new IMAPMessage[this.size + 64];
      System.arraycopy(this.messages, 0, newm, 0, this.size);
      this.messages = newm;
      if (this.seqnums != null) {
        int[] news = new int[this.size + 64];
        System.arraycopy(this.seqnums, 0, news, 0, this.size);
        this.seqnums = news;
      } 
    } else {
      if (this.logger.isLoggable(Level.FINE))
        this.logger.fine("clean " + newend + " to " + oldend); 
      for (int msgnum = newend; msgnum < oldend; msgnum++) {
        this.messages[msgnum - 1] = null;
        if (this.seqnums != null)
          this.seqnums[msgnum - 1] = 0; 
      } 
    } 
  }
  
  public void addMessages(int count, int newSeqNum) {
    if (this.logger.isLoggable(Level.FINE))
      this.logger.fine("add " + count + " messages"); 
    ensureCapacity(this.size + count, newSeqNum);
  }
  
  private void ensureCapacity(int newsize, int newSeqNum) {
    if (this.messages == null) {
      this.messages = new IMAPMessage[newsize + 64];
    } else if (this.messages.length < newsize) {
      if (this.logger.isLoggable(Level.FINE))
        this.logger.fine("expand capacity to " + newsize); 
      IMAPMessage[] newm = new IMAPMessage[newsize + 64];
      System.arraycopy(this.messages, 0, newm, 0, this.messages.length);
      this.messages = newm;
      if (this.seqnums != null) {
        int[] news = new int[newsize + 64];
        System.arraycopy(this.seqnums, 0, news, 0, this.seqnums.length);
        for (int i = this.size; i < news.length; i++)
          news[i] = newSeqNum++; 
        this.seqnums = news;
        if (this.logger.isLoggable(Level.FINE))
          this.logger.fine("message " + newsize + " has sequence number " + this.seqnums[newsize - 1]); 
      } 
    } else if (newsize < this.size) {
      if (this.logger.isLoggable(Level.FINE))
        this.logger.fine("shrink capacity to " + newsize); 
      for (int msgnum = newsize + 1; msgnum <= this.size; msgnum++) {
        this.messages[msgnum - 1] = null;
        if (this.seqnums != null)
          this.seqnums[msgnum - 1] = -1; 
      } 
    } 
    this.size = newsize;
  }
  
  public int seqnumOf(int msgnum) {
    if (this.seqnums == null)
      return msgnum; 
    if (this.logger.isLoggable(Level.FINE))
      this.logger.fine("msgnum " + msgnum + " is seqnum " + this.seqnums[msgnum - 1]); 
    return this.seqnums[msgnum - 1];
  }
  
  private int msgnumOf(int seqnum) {
    if (this.seqnums == null)
      return seqnum; 
    if (seqnum < 1) {
      if (this.logger.isLoggable(Level.FINE))
        this.logger.fine("bad seqnum " + seqnum); 
      return -1;
    } 
    for (int msgnum = seqnum; msgnum <= this.size; msgnum++) {
      if (this.seqnums[msgnum - 1] == seqnum)
        return msgnum; 
      if (this.seqnums[msgnum - 1] > seqnum)
        break; 
    } 
    return -1;
  }
}
