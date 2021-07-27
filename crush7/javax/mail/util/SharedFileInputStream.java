package javax.mail.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import javax.mail.internet.SharedInputStream;

public class SharedFileInputStream extends BufferedInputStream implements SharedInputStream {
  private static int defaultBufferSize = 2048;
  
  protected RandomAccessFile in;
  
  protected int bufsize;
  
  protected long bufpos;
  
  protected long start = 0L;
  
  protected long datalen;
  
  private boolean master = true;
  
  private SharedFile sf;
  
  static class SharedFile {
    private int cnt;
    
    private RandomAccessFile in;
    
    SharedFile(String file) throws IOException {
      this.in = new RandomAccessFile(file, "r");
    }
    
    SharedFile(File file) throws IOException {
      this.in = new RandomAccessFile(file, "r");
    }
    
    public synchronized RandomAccessFile open() {
      this.cnt++;
      return this.in;
    }
    
    public synchronized void close() throws IOException {
      if (this.cnt > 0 && --this.cnt <= 0)
        this.in.close(); 
    }
    
    public synchronized void forceClose() throws IOException {
      if (this.cnt > 0) {
        this.cnt = 0;
        this.in.close();
      } else {
        try {
          this.in.close();
        } catch (IOException iOException) {}
      } 
    }
    
    protected void finalize() throws Throwable {
      try {
        this.in.close();
      } finally {
        super.finalize();
      } 
    }
  }
  
  private void ensureOpen() throws IOException {
    if (this.in == null)
      throw new IOException("Stream closed"); 
  }
  
  public SharedFileInputStream(File file) throws IOException {
    this(file, defaultBufferSize);
  }
  
  public SharedFileInputStream(String file) throws IOException {
    this(file, defaultBufferSize);
  }
  
  public SharedFileInputStream(File file, int size) throws IOException {
    super(null);
    if (size <= 0)
      throw new IllegalArgumentException("Buffer size <= 0"); 
    init(new SharedFile(file), size);
  }
  
  public SharedFileInputStream(String file, int size) throws IOException {
    super(null);
    if (size <= 0)
      throw new IllegalArgumentException("Buffer size <= 0"); 
    init(new SharedFile(file), size);
  }
  
  private void init(SharedFile sf, int size) throws IOException {
    this.sf = sf;
    this.in = sf.open();
    this.start = 0L;
    this.datalen = this.in.length();
    this.bufsize = size;
    this.buf = new byte[size];
  }
  
  private SharedFileInputStream(SharedFile sf, long start, long len, int bufsize) {
    super(null);
    this.master = false;
    this.sf = sf;
    this.in = sf.open();
    this.start = start;
    this.bufpos = start;
    this.datalen = len;
    this.bufsize = bufsize;
    this.buf = new byte[bufsize];
  }
  
  private void fill() throws IOException {
    if (this.markpos < 0) {
      this.pos = 0;
      this.bufpos += this.count;
    } else if (this.pos >= this.buf.length) {
      if (this.markpos > 0) {
        int sz = this.pos - this.markpos;
        System.arraycopy(this.buf, this.markpos, this.buf, 0, sz);
        this.pos = sz;
        this.bufpos += this.markpos;
        this.markpos = 0;
      } else if (this.buf.length >= this.marklimit) {
        this.markpos = -1;
        this.pos = 0;
        this.bufpos += this.count;
      } else {
        int nsz = this.pos * 2;
        if (nsz > this.marklimit)
          nsz = this.marklimit; 
        byte[] nbuf = new byte[nsz];
        System.arraycopy(this.buf, 0, nbuf, 0, this.pos);
        this.buf = nbuf;
      } 
    } 
    this.count = this.pos;
    int len = this.buf.length - this.pos;
    if (this.bufpos - this.start + this.pos + len > this.datalen)
      len = (int)(this.datalen - this.bufpos - this.start + this.pos); 
    synchronized (this.in) {
      this.in.seek(this.bufpos + this.pos);
      int n = this.in.read(this.buf, this.pos, len);
      if (n > 0)
        this.count = n + this.pos; 
    } 
  }
  
  public synchronized int read() throws IOException {
    ensureOpen();
    if (this.pos >= this.count) {
      fill();
      if (this.pos >= this.count)
        return -1; 
    } 
    return this.buf[this.pos++] & 0xFF;
  }
  
  private int read1(byte[] b, int off, int len) throws IOException {
    int avail = this.count - this.pos;
    if (avail <= 0) {
      fill();
      avail = this.count - this.pos;
      if (avail <= 0)
        return -1; 
    } 
    int cnt = (avail < len) ? avail : len;
    System.arraycopy(this.buf, this.pos, b, off, cnt);
    this.pos += cnt;
    return cnt;
  }
  
  public synchronized int read(byte[] b, int off, int len) throws IOException {
    ensureOpen();
    if ((off | len | off + len | b.length - off + len) < 0)
      throw new IndexOutOfBoundsException(); 
    if (len == 0)
      return 0; 
    int n = read1(b, off, len);
    if (n <= 0)
      return n; 
    while (n < len) {
      int n1 = read1(b, off + n, len - n);
      if (n1 <= 0)
        break; 
      n += n1;
    } 
    return n;
  }
  
  public synchronized long skip(long n) throws IOException {
    ensureOpen();
    if (n <= 0L)
      return 0L; 
    long avail = (this.count - this.pos);
    if (avail <= 0L) {
      fill();
      avail = (this.count - this.pos);
      if (avail <= 0L)
        return 0L; 
    } 
    long skipped = (avail < n) ? avail : n;
    this.pos = (int)(this.pos + skipped);
    return skipped;
  }
  
  public synchronized int available() throws IOException {
    ensureOpen();
    return this.count - this.pos + in_available();
  }
  
  private int in_available() throws IOException {
    return (int)(this.start + this.datalen - this.bufpos + this.count);
  }
  
  public synchronized void mark(int readlimit) {
    this.marklimit = readlimit;
    this.markpos = this.pos;
  }
  
  public synchronized void reset() throws IOException {
    ensureOpen();
    if (this.markpos < 0)
      throw new IOException("Resetting to invalid mark"); 
    this.pos = this.markpos;
  }
  
  public boolean markSupported() {
    return true;
  }
  
  public void close() throws IOException {
    if (this.in == null)
      return; 
    try {
      if (this.master) {
        this.sf.forceClose();
      } else {
        this.sf.close();
      } 
    } finally {
      this.sf = null;
      this.in = null;
      this.buf = null;
    } 
  }
  
  public long getPosition() {
    if (this.in == null)
      throw new RuntimeException("Stream closed"); 
    return this.bufpos + this.pos - this.start;
  }
  
  public synchronized InputStream newStream(long start, long end) {
    if (this.in == null)
      throw new RuntimeException("Stream closed"); 
    if (start < 0L)
      throw new IllegalArgumentException("start < 0"); 
    if (end == -1L)
      end = this.datalen; 
    return new SharedFileInputStream(this.sf, this.start + start, end - start, this.bufsize);
  }
  
  protected void finalize() throws Throwable {
    super.finalize();
    close();
  }
}
