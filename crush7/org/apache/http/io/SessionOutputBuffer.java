package org.apache.http.io;

import java.io.IOException;
import org.apache.http.util.CharArrayBuffer;

public interface SessionOutputBuffer {
  void write(byte[] paramArrayOfbyte, int paramInt1, int paramInt2) throws IOException;
  
  void write(byte[] paramArrayOfbyte) throws IOException;
  
  void write(int paramInt) throws IOException;
  
  void writeLine(String paramString) throws IOException;
  
  void writeLine(CharArrayBuffer paramCharArrayBuffer) throws IOException;
  
  void flush() throws IOException;
  
  HttpTransportMetrics getMetrics();
}
