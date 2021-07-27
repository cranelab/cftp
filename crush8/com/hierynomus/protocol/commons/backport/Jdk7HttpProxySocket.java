package com.hierynomus.protocol.commons.backport;

import com.hierynomus.protocol.commons.Charsets;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

public class Jdk7HttpProxySocket extends Socket {
  private Proxy httpProxy = null;
  
  public Jdk7HttpProxySocket(Proxy proxy) {
    super((proxy.type() == Proxy.Type.HTTP) ? Proxy.NO_PROXY : proxy);
    if (proxy.type() == Proxy.Type.HTTP)
      this.httpProxy = proxy; 
  }
  
  public void connect(SocketAddress endpoint, int timeout) throws IOException {
    if (this.httpProxy != null) {
      connectHttpProxy(endpoint, timeout);
    } else {
      super.connect(endpoint, timeout);
    } 
  }
  
  private void connectHttpProxy(SocketAddress endpoint, int timeout) throws IOException {
    super.connect(this.httpProxy.address(), timeout);
    if (!(endpoint instanceof InetSocketAddress))
      throw new SocketException("Expected an InetSocketAddress to connect to, got: " + endpoint); 
    InetSocketAddress isa = (InetSocketAddress)endpoint;
    String httpConnect = "CONNECT " + isa.getHostName() + ":" + isa.getPort() + " HTTP/1.0\n\n";
    getOutputStream().write(httpConnect.getBytes(Charsets.UTF_8));
    checkAndFlushProxyResponse();
  }
  
  private void checkAndFlushProxyResponse() throws IOException {
    InputStream socketInput = getInputStream();
    byte[] tmpBuffer = new byte[512];
    int len = socketInput.read(tmpBuffer, 0, tmpBuffer.length);
    if (len == 0)
      throw new SocketException("Empty response from proxy"); 
    String proxyResponse = new String(tmpBuffer, 0, len, "UTF-8");
    if (proxyResponse.contains("200")) {
      int avail = socketInput.available();
      while (avail > 0)
        avail = (int)(avail - socketInput.skip(avail)); 
    } else {
      throw new SocketException("Fail to create Socket\nResponse was:" + proxyResponse);
    } 
  }
}
