package com.hierynomus.protocol.commons.socket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import javax.net.SocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxySocketFactory extends SocketFactory {
  private static final Logger logger = LoggerFactory.getLogger(ProxySocketFactory.class);
  
  public static final int DEFAULT_CONNECT_TIMEOUT = 5000;
  
  private Proxy proxy;
  
  private int connectTimeout;
  
  public ProxySocketFactory() {
    this(Proxy.NO_PROXY, 5000);
  }
  
  public ProxySocketFactory(String proxyAddress, int proxyPort) {
    this(getHttpProxy(proxyAddress, proxyPort), 5000);
  }
  
  public ProxySocketFactory(Proxy proxy) {
    this(proxy, 5000);
  }
  
  public ProxySocketFactory(int connectTimeout) {
    this(Proxy.NO_PROXY, connectTimeout);
  }
  
  public ProxySocketFactory(Proxy proxy, int connectTimeout) {
    this.proxy = proxy;
    this.connectTimeout = connectTimeout;
  }
  
  public Socket createSocket() throws IOException {
    return new Socket(this.proxy);
  }
  
  public Socket createSocket(String address, int port) throws IOException {
    return createSocket(new InetSocketAddress(address, port), (InetSocketAddress)null);
  }
  
  public Socket createSocket(String address, int port, InetAddress localAddress, int localPort) throws IOException {
    return createSocket(new InetSocketAddress(address, port), new InetSocketAddress(localAddress, localPort));
  }
  
  public Socket createSocket(InetAddress address, int port) throws IOException {
    return createSocket(new InetSocketAddress(address, port), (InetSocketAddress)null);
  }
  
  public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
    return createSocket(new InetSocketAddress(address, port), new InetSocketAddress(localAddress, localPort));
  }
  
  private Socket createSocket(InetSocketAddress address, InetSocketAddress bindAddress) throws IOException {
    Socket socket = new Socket(this.proxy);
    if (bindAddress != null)
      socket.bind(bindAddress); 
    logger.debug("Connecting to {}", address);
    socket.connect(address, this.connectTimeout);
    return socket;
  }
  
  private static Proxy getHttpProxy(String proxyAddress, int proxyPort) {
    return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyAddress, proxyPort));
  }
}
