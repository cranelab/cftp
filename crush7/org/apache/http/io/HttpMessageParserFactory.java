package org.apache.http.io;

import org.apache.http.config.MessageConstraints;

public interface HttpMessageParserFactory<T extends org.apache.http.HttpMessage> {
  HttpMessageParser<T> create(SessionInputBuffer paramSessionInputBuffer, MessageConstraints paramMessageConstraints);
}
