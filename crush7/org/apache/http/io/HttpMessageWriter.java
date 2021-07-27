package org.apache.http.io;

import java.io.IOException;
import org.apache.http.HttpException;

public interface HttpMessageWriter<T extends org.apache.http.HttpMessage> {
  void write(T paramT) throws IOException, HttpException;
}
