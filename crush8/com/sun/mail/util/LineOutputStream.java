package com.sun.mail.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class LineOutputStream extends FilterOutputStream {
  private static byte[] newline = new byte[2];
  
  static {
    newline[0] = 13;
    newline[1] = 10;
  }
  
  public LineOutputStream(OutputStream out) {
    super(out);
  }
  
  public void writeln(String s) throws IOException {
    byte[] bytes = ASCIIUtility.getBytes(s);
    this.out.write(bytes);
    this.out.write(newline);
  }
  
  public void writeln() throws IOException {
    this.out.write(newline);
  }
}
