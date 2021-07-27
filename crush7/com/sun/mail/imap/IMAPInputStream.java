package com.sun.mail.imap;

import com.sun.mail.iap.ByteArray;
import com.sun.mail.iap.ConnectionException;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.protocol.BODY;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.sun.mail.util.FolderClosedIOException;
import com.sun.mail.util.MessageRemovedIOException;
import java.io.IOException;
import java.io.InputStream;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.MessagingException;

public class IMAPInputStream extends InputStream {
  private IMAPMessage msg;
  
  private String section;
  
  private int pos;
  
  private int blksize;
  
  private int max;
  
  private byte[] buf;
  
  private int bufcount;
  
  private int bufpos;
  
  private boolean lastBuffer;
  
  private boolean peek;
  
  private ByteArray readbuf;
  
  private static final int slop = 64;
  
  public IMAPInputStream(IMAPMessage msg, String section, int max, boolean peek) {
    this.msg = msg;
    this.section = section;
    this.max = max;
    this.peek = peek;
    this.pos = 0;
    this.blksize = msg.getFetchBlockSize();
  }
  
  private void forceCheckExpunged() throws MessageRemovedIOException, FolderClosedIOException {
    synchronized (this.msg.getMessageCacheLock()) {
      try {
        this.msg.getProtocol().noop();
      } catch (ConnectionException cex) {
        throw new FolderClosedIOException(this.msg.getFolder(), cex
            .getMessage());
      } catch (FolderClosedException fex) {
        throw new FolderClosedIOException(fex.getFolder(), fex
            .getMessage());
      } catch (ProtocolException protocolException) {}
    } 
    if (this.msg.isExpunged())
      throw new MessageRemovedIOException(); 
  }
  
  private void fill() throws IOException {
    ByteArray ba;
    int cnt;
    if (this.lastBuffer || (this.max != -1 && this.pos >= this.max)) {
      if (this.pos == 0)
        checkSeen(); 
      this.readbuf = null;
      return;
    } 
    BODY b = null;
    if (this.readbuf == null)
      this.readbuf = new ByteArray(this.blksize + 64); 
    synchronized (this.msg.getMessageCacheLock()) {
      try {
        IMAPProtocol p = this.msg.getProtocol();
        if (this.msg.isExpunged())
          throw new MessageRemovedIOException("No content for expunged message"); 
        int seqnum = this.msg.getSequenceNumber();
        cnt = this.blksize;
        if (this.max != -1 && this.pos + this.blksize > this.max)
          cnt = this.max - this.pos; 
        if (this.peek) {
          b = p.peekBody(seqnum, this.section, this.pos, cnt, this.readbuf);
        } else {
          b = p.fetchBody(seqnum, this.section, this.pos, cnt, this.readbuf);
        } 
      } catch (ProtocolException pex) {
        forceCheckExpunged();
        throw new IOException(pex.getMessage());
      } catch (FolderClosedException fex) {
        throw new FolderClosedIOException(fex.getFolder(), fex
            .getMessage());
      } 
      if (b == null || (ba = b.getByteArray()) == null) {
        forceCheckExpunged();
        throw new IOException("No content");
      } 
    } 
    if (this.pos == 0)
      checkSeen(); 
    this.buf = ba.getBytes();
    this.bufpos = ba.getStart();
    int n = ba.getCount();
    this.lastBuffer = (n < cnt);
    this.bufcount = this.bufpos + n;
    this.pos += n;
  }
  
  public synchronized int read() throws IOException {
    if (this.bufpos >= this.bufcount) {
      fill();
      if (this.bufpos >= this.bufcount)
        return -1; 
    } 
    return this.buf[this.bufpos++] & 0xFF;
  }
  
  public synchronized int read(byte[] b, int off, int len) throws IOException {
    int avail = this.bufcount - this.bufpos;
    if (avail <= 0) {
      fill();
      avail = this.bufcount - this.bufpos;
      if (avail <= 0)
        return -1; 
    } 
    int cnt = (avail < len) ? avail : len;
    System.arraycopy(this.buf, this.bufpos, b, off, cnt);
    this.bufpos += cnt;
    return cnt;
  }
  
  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }
  
  public synchronized int available() throws IOException {
    return this.bufcount - this.bufpos;
  }
  
  private void checkSeen() {
    if (this.peek)
      return; 
    try {
      Folder f = this.msg.getFolder();
      if (f != null && f.getMode() != 1 && 
        !this.msg.isSet(Flags.Flag.SEEN))
        this.msg.setFlag(Flags.Flag.SEEN, true); 
    } catch (MessagingException messagingException) {}
  }
}
