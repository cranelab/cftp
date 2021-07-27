package org.apache.http.conn.socket;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import org.apache.http.protocol.HttpContext;

public interface LayeredConnectionSocketFactory extends ConnectionSocketFactory {
  Socket createLayeredSocket(Socket paramSocket, String paramString, int paramInt, HttpContext paramHttpContext) throws IOException, UnknownHostException;
}
