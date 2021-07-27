package org.apache.http.io;

import java.io.IOException;
import org.apache.http.HttpException;

public interface HttpMessageParser<T extends org.apache.http.HttpMessage> {
  T parse() throws IOException, HttpException;
}
