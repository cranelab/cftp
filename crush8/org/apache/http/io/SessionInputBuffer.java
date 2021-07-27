package org.apache.http.io;

import java.io.IOException;
import org.apache.http.util.CharArrayBuffer;

public interface SessionInputBuffer {
  int read(byte[] paramArrayOfbyte, int paramInt1, int paramInt2) throws IOException;
  
  int read(byte[] paramArrayOfbyte) throws IOException;
  
  int read() throws IOException;
  
  int readLine(CharArrayBuffer paramCharArrayBuffer) throws IOException;
  
  String readLine() throws IOException;
  
  @Deprecated
  boolean isDataAvailable(int paramInt) throws IOException;
  
  HttpTransportMetrics getMetrics();
}
