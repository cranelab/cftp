package javax.mail.internet;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;

class AsciiOutputStream extends OutputStream {
  private boolean breakOnNonAscii;
  
  private int ascii = 0, non_ascii = 0;
  
  private int linelen = 0;
  
  private boolean longLine = false;
  
  private boolean badEOL = false;
  
  private boolean checkEOL = false;
  
  private int lastb = 0;
  
  private int ret = 0;
  
  public AsciiOutputStream(boolean breakOnNonAscii, boolean encodeEolStrict) {
    this.breakOnNonAscii = breakOnNonAscii;
    this.checkEOL = (encodeEolStrict && breakOnNonAscii);
  }
  
  public void write(int b) throws IOException {
    check(b);
  }
  
  public void write(byte[] b) throws IOException {
    write(b, 0, b.length);
  }
  
  public void write(byte[] b, int off, int len) throws IOException {
    len += off;
    for (int i = off; i < len; i++)
      check(b[i]); 
  }
  
  private final void check(int b) throws IOException {
    b &= 0xFF;
    if (this.checkEOL && ((this.lastb == 13 && b != 10) || (this.lastb != 13 && b == 10)))
      this.badEOL = true; 
    if (b == 13 || b == 10) {
      this.linelen = 0;
    } else {
      this.linelen++;
      if (this.linelen > 998)
        this.longLine = true; 
    } 
    if (MimeUtility.nonascii(b)) {
      this.non_ascii++;
      if (this.breakOnNonAscii) {
        this.ret = 3;
        throw new EOFException();
      } 
    } else {
      this.ascii++;
    } 
    this.lastb = b;
  }
  
  public int getAscii() {
    if (this.ret != 0)
      return this.ret; 
    if (this.badEOL)
      return 3; 
    if (this.non_ascii == 0) {
      if (this.longLine)
        return 2; 
      return 1;
    } 
    if (this.ascii > this.non_ascii)
      return 2; 
    return 3;
  }
}
