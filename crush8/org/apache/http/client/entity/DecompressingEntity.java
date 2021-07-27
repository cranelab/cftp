package org.apache.http.client.entity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.util.Args;

public class DecompressingEntity extends HttpEntityWrapper {
  private static final int BUFFER_SIZE = 2048;
  
  private final InputStreamFactory inputStreamFactory;
  
  private InputStream content;
  
  public DecompressingEntity(HttpEntity wrapped, InputStreamFactory inputStreamFactory) {
    super(wrapped);
    this.inputStreamFactory = inputStreamFactory;
  }
  
  private InputStream getDecompressingStream() throws IOException {
    InputStream in = this.wrappedEntity.getContent();
    return new LazyDecompressingInputStream(in, this.inputStreamFactory);
  }
  
  public InputStream getContent() throws IOException {
    if (this.wrappedEntity.isStreaming()) {
      if (this.content == null)
        this.content = getDecompressingStream(); 
      return this.content;
    } 
    return getDecompressingStream();
  }
  
  public void writeTo(OutputStream outstream) throws IOException {
    Args.notNull(outstream, "Output stream");
    InputStream instream = getContent();
    try {
      byte[] buffer = new byte[2048];
      int l;
      while ((l = instream.read(buffer)) != -1)
        outstream.write(buffer, 0, l); 
    } finally {
      instream.close();
    } 
  }
  
  public Header getContentEncoding() {
    return null;
  }
  
  public long getContentLength() {
    return -1L;
  }
}
