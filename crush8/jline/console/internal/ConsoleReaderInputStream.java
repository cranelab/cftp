package jline.console.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Enumeration;
import jline.console.ConsoleReader;

class ConsoleReaderInputStream extends SequenceInputStream {
  private static InputStream systemIn = System.in;
  
  public static void setIn() throws IOException {
    setIn(new ConsoleReader());
  }
  
  public static void setIn(ConsoleReader reader) {
    System.setIn(new ConsoleReaderInputStream(reader));
  }
  
  public static void restoreIn() {
    System.setIn(systemIn);
  }
  
  public ConsoleReaderInputStream(ConsoleReader reader) {
    super(new ConsoleEnumeration(reader));
  }
  
  private static class ConsoleEnumeration implements Enumeration<InputStream> {
    private final ConsoleReader reader;
    
    private ConsoleReaderInputStream.ConsoleLineInputStream next = null;
    
    private ConsoleReaderInputStream.ConsoleLineInputStream prev = null;
    
    public ConsoleEnumeration(ConsoleReader reader) {
      this.reader = reader;
    }
    
    public InputStream nextElement() {
      if (this.next != null) {
        InputStream n = this.next;
        this.prev = this.next;
        this.next = null;
        return n;
      } 
      return new ConsoleReaderInputStream.ConsoleLineInputStream(this.reader);
    }
    
    public boolean hasMoreElements() {
      if (this.prev != null && this.prev.wasNull == true)
        return false; 
      if (this.next == null)
        this.next = (ConsoleReaderInputStream.ConsoleLineInputStream)nextElement(); 
      return (this.next != null);
    }
  }
  
  private static class ConsoleLineInputStream extends InputStream {
    private final ConsoleReader reader;
    
    private String line = null;
    
    private int index = 0;
    
    private boolean eol = false;
    
    protected boolean wasNull = false;
    
    public ConsoleLineInputStream(ConsoleReader reader) {
      this.reader = reader;
    }
    
    public int read() throws IOException {
      if (this.eol)
        return -1; 
      if (this.line == null)
        this.line = this.reader.readLine(); 
      if (this.line == null) {
        this.wasNull = true;
        return -1;
      } 
      if (this.index >= this.line.length()) {
        this.eol = true;
        return 10;
      } 
      return this.line.charAt(this.index++);
    }
  }
}
