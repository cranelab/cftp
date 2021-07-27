package org.apache.http.conn.scheme;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

@Deprecated
public interface LayeredSchemeSocketFactory extends SchemeSocketFactory {
  Socket createLayeredSocket(Socket paramSocket, String paramString, int paramInt, boolean paramBoolean) throws IOException, UnknownHostException;
}
