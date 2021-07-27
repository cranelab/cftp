package org.apache.http.impl.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import org.apache.http.MessageConstraintException;
import org.apache.http.config.MessageConstraints;
import org.apache.http.io.BufferInfo;
import org.apache.http.io.HttpTransportMetrics;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.util.Args;
import org.apache.http.util.Asserts;
import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.CharArrayBuffer;

public class SessionInputBufferImpl implements SessionInputBuffer, BufferInfo {
  private final HttpTransportMetricsImpl metrics;
  
  private final byte[] buffer;
  
  private final ByteArrayBuffer linebuffer;
  
  private final int minChunkLimit;
  
  private final MessageConstraints constraints;
  
  private final CharsetDecoder decoder;
  
  private InputStream instream;
  
  private int bufferpos;
  
  private int bufferlen;
  
  private CharBuffer cbuf;
  
  public SessionInputBufferImpl(HttpTransportMetricsImpl metrics, int buffersize, int minChunkLimit, MessageConstraints constraints, CharsetDecoder chardecoder) {
    Args.notNull(metrics, "HTTP transport metrcis");
    Args.positive(buffersize, "Buffer size");
    this.metrics = metrics;
    this.buffer = new byte[buffersize];
    this.bufferpos = 0;
    this.bufferlen = 0;
    this.minChunkLimit = (minChunkLimit >= 0) ? minChunkLimit : 512;
    this.constraints = (constraints != null) ? constraints : MessageConstraints.DEFAULT;
    this.linebuffer = new ByteArrayBuffer(buffersize);
    this.decoder = chardecoder;
  }
  
  public SessionInputBufferImpl(HttpTransportMetricsImpl metrics, int buffersize) {
    this(metrics, buffersize, buffersize, null, null);
  }
  
  public void bind(InputStream instream) {
    this.instream = instream;
  }
  
  public boolean isBound() {
    return (this.instream != null);
  }
  
  public int capacity() {
    return this.buffer.length;
  }
  
  public int length() {
    return this.bufferlen - this.bufferpos;
  }
  
  public int available() {
    return capacity() - length();
  }
  
  private int streamRead(byte[] b, int off, int len) throws IOException {
    Asserts.notNull(this.instream, "Input stream");
    return this.instream.read(b, off, len);
  }
  
  public int fillBuffer() throws IOException {
    if (this.bufferpos > 0) {
      int i = this.bufferlen - this.bufferpos;
      if (i > 0)
        System.arraycopy(this.buffer, this.bufferpos, this.buffer, 0, i); 
      this.bufferpos = 0;
      this.bufferlen = i;
    } 
    int off = this.bufferlen;
    int len = this.buffer.length - off;
    int l = streamRead(this.buffer, off, len);
    if (l == -1)
      return -1; 
    this.bufferlen = off + l;
    this.metrics.incrementBytesTransferred(l);
    return l;
  }
  
  public boolean hasBufferedData() {
    return (this.bufferpos < this.bufferlen);
  }
  
  public void clear() {
    this.bufferpos = 0;
    this.bufferlen = 0;
  }
  
  public int read() throws IOException {
    while (!hasBufferedData()) {
      int noRead = fillBuffer();
      if (noRead == -1)
        return -1; 
    } 
    return this.buffer[this.bufferpos++] & 0xFF;
  }
  
  public int read(byte[] b, int off, int len) throws IOException {
    if (b == null)
      return 0; 
    if (hasBufferedData()) {
      int i = Math.min(len, this.bufferlen - this.bufferpos);
      System.arraycopy(this.buffer, this.bufferpos, b, off, i);
      this.bufferpos += i;
      return i;
    } 
    if (len > this.minChunkLimit) {
      int read = streamRead(b, off, len);
      if (read > 0)
        this.metrics.incrementBytesTransferred(read); 
      return read;
    } 
    while (!hasBufferedData()) {
      int noRead = fillBuffer();
      if (noRead == -1)
        return -1; 
    } 
    int chunk = Math.min(len, this.bufferlen - this.bufferpos);
    System.arraycopy(this.buffer, this.bufferpos, b, off, chunk);
    this.bufferpos += chunk;
    return chunk;
  }
  
  public int read(byte[] b) throws IOException {
    if (b == null)
      return 0; 
    return read(b, 0, b.length);
  }
  
  public int readLine(CharArrayBuffer charbuffer) throws IOException {
    Args.notNull(charbuffer, "Char array buffer");
    int maxLineLen = this.constraints.getMaxLineLength();
    int noRead = 0;
    boolean retry = true;
    while (retry) {
      int pos = -1;
      for (int i = this.bufferpos; i < this.bufferlen; i++) {
        if (this.buffer[i] == 10) {
          pos = i;
          break;
        } 
      } 
      if (maxLineLen > 0) {
        int currentLen = this.linebuffer.length() + ((pos > 0) ? pos : this.bufferlen) - this.bufferpos;
        if (currentLen >= maxLineLen)
          throw new MessageConstraintException("Maximum line length limit exceeded"); 
      } 
      if (pos != -1) {
        if (this.linebuffer.isEmpty())
          return lineFromReadBuffer(charbuffer, pos); 
        retry = false;
        int len = pos + 1 - this.bufferpos;
        this.linebuffer.append(this.buffer, this.bufferpos, len);
        this.bufferpos = pos + 1;
        continue;
      } 
      if (hasBufferedData()) {
        int len = this.bufferlen - this.bufferpos;
        this.linebuffer.append(this.buffer, this.bufferpos, len);
        this.bufferpos = this.bufferlen;
      } 
      noRead = fillBuffer();
      if (noRead == -1)
        retry = false; 
    } 
    if (noRead == -1 && this.linebuffer.isEmpty())
      return -1; 
    return lineFromLineBuffer(charbuffer);
  }
  
  private int lineFromLineBuffer(CharArrayBuffer charbuffer) throws IOException {
    int len = this.linebuffer.length();
    if (len > 0) {
      if (this.linebuffer.byteAt(len - 1) == 10)
        len--; 
      if (len > 0 && 
        this.linebuffer.byteAt(len - 1) == 13)
        len--; 
    } 
    if (this.decoder == null) {
      charbuffer.append(this.linebuffer, 0, len);
    } else {
      ByteBuffer bbuf = ByteBuffer.wrap(this.linebuffer.buffer(), 0, len);
      len = appendDecoded(charbuffer, bbuf);
    } 
    this.linebuffer.clear();
    return len;
  }
  
  private int lineFromReadBuffer(CharArrayBuffer charbuffer, int position) throws IOException {
    int pos = position;
    int off = this.bufferpos;
    this.bufferpos = pos + 1;
    if (pos > off && this.buffer[pos - 1] == 13)
      pos--; 
    int len = pos - off;
    if (this.decoder == null) {
      charbuffer.append(this.buffer, off, len);
    } else {
      ByteBuffer bbuf = ByteBuffer.wrap(this.buffer, off, len);
      len = appendDecoded(charbuffer, bbuf);
    } 
    return len;
  }
  
  private int appendDecoded(CharArrayBuffer charbuffer, ByteBuffer bbuf) throws IOException {
    if (!bbuf.hasRemaining())
      return 0; 
    if (this.cbuf == null)
      this.cbuf = CharBuffer.allocate(1024); 
    this.decoder.reset();
    int len = 0;
    while (bbuf.hasRemaining()) {
      CoderResult coderResult = this.decoder.decode(bbuf, this.cbuf, true);
      len += handleDecodingResult(coderResult, charbuffer, bbuf);
    } 
    CoderResult result = this.decoder.flush(this.cbuf);
    len += handleDecodingResult(result, charbuffer, bbuf);
    this.cbuf.clear();
    return len;
  }
  
  private int handleDecodingResult(CoderResult result, CharArrayBuffer charbuffer, ByteBuffer bbuf) throws IOException {
    if (result.isError())
      result.throwException(); 
    this.cbuf.flip();
    int len = this.cbuf.remaining();
    while (this.cbuf.hasRemaining())
      charbuffer.append(this.cbuf.get()); 
    this.cbuf.compact();
    return len;
  }
  
  public String readLine() throws IOException {
    CharArrayBuffer charbuffer = new CharArrayBuffer(64);
    int l = readLine(charbuffer);
    if (l != -1)
      return charbuffer.toString(); 
    return null;
  }
  
  public boolean isDataAvailable(int timeout) throws IOException {
    return hasBufferedData();
  }
  
  public HttpTransportMetrics getMetrics() {
    return this.metrics;
  }
}
