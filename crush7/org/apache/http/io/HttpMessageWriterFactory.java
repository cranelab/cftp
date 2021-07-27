package org.apache.http.io;

public interface HttpMessageWriterFactory<T extends org.apache.http.HttpMessage> {
  HttpMessageWriter<T> create(SessionOutputBuffer paramSessionOutputBuffer);
}
