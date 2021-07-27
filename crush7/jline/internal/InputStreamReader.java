package jline.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;

public class InputStreamReader extends Reader {
  private InputStream in;
  
  private static final int BUFFER_SIZE = 8192;
  
  private boolean endOfInput = false;
  
  CharsetDecoder decoder;
  
  ByteBuffer bytes = ByteBuffer.allocate(8192);
  
  public InputStreamReader(InputStream in) {
    super(in);
    this.in = in;
    this
      .decoder = Charset.defaultCharset().newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
    this.bytes.limit(0);
  }
  
  public InputStreamReader(InputStream in, String enc) throws UnsupportedEncodingException {
    super(in);
    if (enc == null)
      throw new NullPointerException(); 
    this.in = in;
    try {
      this
        .decoder = Charset.forName(enc).newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
    } catch (IllegalArgumentException e) {
      throw (UnsupportedEncodingException)(new UnsupportedEncodingException(enc))
        .initCause(e);
    } 
    this.bytes.limit(0);
  }
  
  public InputStreamReader(InputStream in, CharsetDecoder dec) {
    super(in);
    dec.averageCharsPerByte();
    this.in = in;
    this.decoder = dec;
    this.bytes.limit(0);
  }
  
  public InputStreamReader(InputStream in, Charset charset) {
    super(in);
    this.in = in;
    this
      .decoder = charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
    this.bytes.limit(0);
  }
  
  public void close() throws IOException {
    synchronized (this.lock) {
      this.decoder = null;
      if (this.in != null) {
        this.in.close();
        this.in = null;
      } 
    } 
  }
  
  public String getEncoding() {
    if (!isOpen())
      return null; 
    return this.decoder.charset().name();
  }
  
  public int read() throws IOException {
    synchronized (this.lock) {
      if (!isOpen())
        throw new IOException("InputStreamReader is closed."); 
      char[] buf = new char[4];
      return (read(buf, 0, 4) != -1) ? Character.codePointAt(buf, 0) : -1;
    } 
  }
  
  public int read(char[] buf, int offset, int length) throws IOException {
    synchronized (this.lock) {
      if (!isOpen())
        throw new IOException("InputStreamReader is closed."); 
      if (offset < 0 || offset > buf.length - length || length < 0)
        throw new IndexOutOfBoundsException(); 
      if (length == 0)
        return 0; 
      CharBuffer out = CharBuffer.wrap(buf, offset, length);
      CoderResult result = CoderResult.UNDERFLOW;
      boolean needInput = !this.bytes.hasRemaining();
      while (out.hasRemaining()) {
        if (needInput) {
          try {
            if (this.in.available() == 0 && out
              .position() > offset)
              break; 
          } catch (IOException iOException) {}
          int to_read = this.bytes.capacity() - this.bytes.limit();
          int off = this.bytes.arrayOffset() + this.bytes.limit();
          int was_red = this.in.read(this.bytes.array(), off, to_read);
          if (was_red == -1) {
            this.endOfInput = true;
            break;
          } 
          if (was_red == 0)
            break; 
          this.bytes.limit(this.bytes.limit() + was_red);
          needInput = false;
        } 
        result = this.decoder.decode(this.bytes, out, false);
        if (result.isUnderflow()) {
          if (this.bytes.limit() == this.bytes.capacity()) {
            this.bytes.compact();
            this.bytes.limit(this.bytes.position());
            this.bytes.position(0);
          } 
          needInput = true;
        } 
      } 
      if (result == CoderResult.UNDERFLOW && this.endOfInput) {
        result = this.decoder.decode(this.bytes, out, true);
        this.decoder.flush(out);
        this.decoder.reset();
      } 
      if (result.isMalformed())
        throw new MalformedInputException(result.length()); 
      if (result.isUnmappable())
        throw new UnmappableCharacterException(result.length()); 
      return (out.position() - offset == 0) ? -1 : (out.position() - offset);
    } 
  }
  
  private boolean isOpen() {
    return (this.in != null);
  }
  
  public boolean ready() throws IOException {
    synchronized (this.lock) {
      if (this.in == null)
        throw new IOException("InputStreamReader is closed."); 
      try {
        return (this.bytes.hasRemaining() || this.in.available() > 0);
      } catch (IOException e) {
        return false;
      } 
    } 
  }
}
