package jline.internal;

import java.io.IOException;
import java.io.InputStream;

public class NonBlockingInputStream extends InputStream implements Runnable {
  private InputStream in;
  
  private int ch = -2;
  
  private boolean threadIsReading = false;
  
  private boolean isShutdown = false;
  
  private IOException exception = null;
  
  private boolean nonBlockingEnabled;
  
  public NonBlockingInputStream(InputStream in, boolean isNonBlockingEnabled) {
    this.in = in;
    this.nonBlockingEnabled = isNonBlockingEnabled;
    if (isNonBlockingEnabled) {
      Thread t = new Thread(this);
      t.setName("NonBlockingInputStreamThread");
      t.setDaemon(true);
      t.start();
    } 
  }
  
  public synchronized void shutdown() {
    if (!this.isShutdown && this.nonBlockingEnabled) {
      this.isShutdown = true;
      notify();
    } 
  }
  
  public boolean isNonBlockingEnabled() {
    return (this.nonBlockingEnabled && !this.isShutdown);
  }
  
  public void close() throws IOException {
    this.in.close();
    shutdown();
  }
  
  public int read() throws IOException {
    if (this.nonBlockingEnabled)
      return read(0L, false); 
    return this.in.read();
  }
  
  public int peek(long timeout) throws IOException {
    if (!this.nonBlockingEnabled || this.isShutdown)
      throw new UnsupportedOperationException("peek() cannot be called as non-blocking operation is disabled"); 
    return read(timeout, true);
  }
  
  public int read(long timeout) throws IOException {
    if (!this.nonBlockingEnabled || this.isShutdown)
      throw new UnsupportedOperationException("read() with timeout cannot be called as non-blocking operation is disabled"); 
    return read(timeout, false);
  }
  
  private synchronized int read(long timeout, boolean isPeek) throws IOException {
    if (this.exception != null) {
      assert this.ch == -2;
      IOException toBeThrown = this.exception;
      if (!isPeek)
        this.exception = null; 
      throw toBeThrown;
    } 
    if (this.ch >= -1) {
      assert this.exception == null;
    } else if ((timeout == 0L || this.isShutdown) && !this.threadIsReading) {
      this.ch = this.in.read();
    } else {
      if (!this.threadIsReading) {
        this.threadIsReading = true;
        notify();
      } 
      boolean isInfinite = (timeout <= 0L);
      while (isInfinite || timeout > 0L) {
        long start = System.currentTimeMillis();
        try {
          wait(timeout);
        } catch (InterruptedException interruptedException) {}
        if (this.exception != null) {
          assert this.ch == -2;
          IOException toBeThrown = this.exception;
          if (!isPeek)
            this.exception = null; 
          throw toBeThrown;
        } 
        if (this.ch >= -1) {
          assert this.exception == null;
          break;
        } 
        if (!isInfinite)
          timeout -= System.currentTimeMillis() - start; 
      } 
    } 
    int ret = this.ch;
    if (!isPeek)
      this.ch = -2; 
    return ret;
  }
  
  public int read(byte[] b, int off, int len) throws IOException {
    int c;
    if (b == null)
      throw new NullPointerException(); 
    if (off < 0 || len < 0 || len > b.length - off)
      throw new IndexOutOfBoundsException(); 
    if (len == 0)
      return 0; 
    if (this.nonBlockingEnabled) {
      c = read(0L);
    } else {
      c = this.in.read();
    } 
    if (c == -1)
      return -1; 
    b[off] = (byte)c;
    return 1;
  }
  
  public void run() {
    Log.debug(new Object[] { "NonBlockingInputStream start" });
    boolean needToShutdown = false;
    boolean needToRead = false;
    while (!needToShutdown) {
      synchronized (this) {
        needToShutdown = this.isShutdown;
        needToRead = this.threadIsReading;
        try {
          if (!needToShutdown && !needToRead)
            wait(0L); 
        } catch (InterruptedException interruptedException) {}
      } 
      if (!needToShutdown && needToRead) {
        int charRead = -2;
        IOException failure = null;
        try {
          charRead = this.in.read();
        } catch (IOException e) {
          failure = e;
        } 
        synchronized (this) {
          this.exception = failure;
          this.ch = charRead;
          this.threadIsReading = false;
          notify();
        } 
      } 
    } 
    Log.debug(new Object[] { "NonBlockingInputStream shutdown" });
  }
}
