package com.crushftp.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.Vector;
import javax.net.ssl.SSLSocketFactory;

public class DMZSocket extends Socket {
  Socket sock = null;
  
  public DMZSocket(VRL u, String dmz_item) throws IOException {
    this.sock = getDmzHostPortSock(dmz_item);
    String host_port = String.valueOf(u.getHost()) + ":" + u.getPort();
    Common.sockLog(this.sock, "Got DMZSocket:" + dmz_item + ":" + host_port);
    this.sock.getOutputStream().write("E".getBytes());
    this.sock.getOutputStream().write((host_port.getBytes("UTF8")).length);
    this.sock.getOutputStream().write(host_port.getBytes("UTF8"));
    this.sock.getOutputStream().flush();
  }
  
  public static Socket getDmzHostPortSock(String dmz_item) throws IOException {
    Vector dmzs = (Vector)Common.System2.get("crushftp.dmz.hosts");
    if (dmzs == null || dmzs.size() == 0)
      throw new IOException("No DMZs available."); 
    dmzs = (Vector)dmzs.clone();
    String name_host_port = "";
    if (dmz_item.equalsIgnoreCase("true")) {
      name_host_port = dmzs.elementAt((int)(Math.random() * dmzs.size() - 1.0D)).toString();
    } else {
      for (int x = 0; x < dmzs.size(); x++) {
        String dmz_name_host_port = dmzs.elementAt(x).toString();
        if (dmz_name_host_port.split(":")[0].trim().equalsIgnoreCase(dmz_item.trim())) {
          name_host_port = dmz_name_host_port;
          break;
        } 
      } 
    } 
    if (name_host_port.equals(""))
      throw new IOException("DMZ instance not found " + dmz_item + " in active list:" + dmzs); 
    Socket sock = null;
    if (System.getProperty("crushftp.dmz.ssl", "true").equals("true") && name_host_port.split(":")[3].equals("false")) {
      SSLSocketFactory factory = (SSLSocketFactory)Common.System2.get("crushftp.dmz.factory");
      sock = factory.createSocket(name_host_port.split(":")[1], Integer.parseInt(name_host_port.split(":")[2]));
      Common.sockLog(sock, "Got DMZHostPortSockSSL:" + dmz_item + ":" + name_host_port);
    } else {
      if (name_host_port.split(":")[3].equals("true")) {
        sock = new Socket("127.0.0.1", Integer.parseInt(name_host_port.split(":")[2]));
      } else {
        sock = new Socket(name_host_port.split(":")[1], Integer.parseInt(name_host_port.split(":")[2]));
      } 
      Common.sockLog(sock, "Got DMZHostPortSockPlain:" + dmz_item + ":" + name_host_port);
    } 
    return sock;
  }
  
  public void bind(SocketAddress bindpoint) throws IOException {
    this.sock.bind(bindpoint);
  }
  
  public synchronized void close() throws IOException {
    this.sock.close();
  }
  
  public void connect(SocketAddress endpoint, int timeout) throws IOException {
    this.sock.connect(endpoint, timeout);
  }
  
  public void connect(SocketAddress endpoint) throws IOException {
    this.sock.connect(endpoint);
  }
  
  public SocketChannel getChannel() {
    return this.sock.getChannel();
  }
  
  public InetAddress getInetAddress() {
    return this.sock.getInetAddress();
  }
  
  public InputStream getInputStream() throws IOException {
    return this.sock.getInputStream();
  }
  
  public boolean getKeepAlive() throws SocketException {
    return this.sock.getKeepAlive();
  }
  
  public InetAddress getLocalAddress() {
    return this.sock.getLocalAddress();
  }
  
  public int getLocalPort() {
    return this.sock.getLocalPort();
  }
  
  public SocketAddress getLocalSocketAddress() {
    return this.sock.getLocalSocketAddress();
  }
  
  public boolean getOOBInline() throws SocketException {
    return this.sock.getOOBInline();
  }
  
  public OutputStream getOutputStream() throws IOException {
    return this.sock.getOutputStream();
  }
  
  public int getPort() {
    return this.sock.getPort();
  }
  
  public synchronized int getReceiveBufferSize() throws SocketException {
    return this.sock.getReceiveBufferSize();
  }
  
  public SocketAddress getRemoteSocketAddress() {
    return this.sock.getRemoteSocketAddress();
  }
  
  public boolean getReuseAddress() throws SocketException {
    return this.sock.getReuseAddress();
  }
  
  public synchronized int getSendBufferSize() throws SocketException {
    return this.sock.getSendBufferSize();
  }
  
  public int getSoLinger() throws SocketException {
    return this.sock.getSoLinger();
  }
  
  public synchronized int getSoTimeout() throws SocketException {
    return this.sock.getSoTimeout();
  }
  
  public boolean getTcpNoDelay() throws SocketException {
    return this.sock.getTcpNoDelay();
  }
  
  public int getTrafficClass() throws SocketException {
    return this.sock.getTrafficClass();
  }
  
  public boolean isBound() {
    return this.sock.isBound();
  }
  
  public boolean isClosed() {
    return this.sock.isClosed();
  }
  
  public boolean isConnected() {
    return this.sock.isConnected();
  }
  
  public boolean isInputShutdown() {
    return this.sock.isInputShutdown();
  }
  
  public boolean isOutputShutdown() {
    return this.sock.isOutputShutdown();
  }
  
  public void sendUrgentData(int data) throws IOException {
    this.sock.sendUrgentData(data);
  }
  
  public void setKeepAlive(boolean on) throws SocketException {
    this.sock.setKeepAlive(on);
  }
  
  public void setOOBInline(boolean on) throws SocketException {
    this.sock.setOOBInline(on);
  }
  
  public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
    this.sock.setPerformancePreferences(connectionTime, latency, bandwidth);
  }
  
  public synchronized void setReceiveBufferSize(int size) throws SocketException {
    this.sock.setReceiveBufferSize(size);
  }
  
  public void setReuseAddress(boolean on) throws SocketException {
    this.sock.setReuseAddress(on);
  }
  
  public synchronized void setSendBufferSize(int size) throws SocketException {
    this.sock.setSendBufferSize(size);
  }
  
  public void setSoLinger(boolean on, int linger) throws SocketException {
    this.sock.setSoLinger(on, linger);
  }
  
  public synchronized void setSoTimeout(int timeout) throws SocketException {
    this.sock.setSoTimeout(timeout);
  }
  
  public void setTcpNoDelay(boolean on) throws SocketException {
    this.sock.setTcpNoDelay(on);
  }
  
  public void setTrafficClass(int tc) throws SocketException {
    this.sock.setTrafficClass(tc);
  }
  
  public void shutdownInput() throws IOException {
    this.sock.shutdownInput();
  }
  
  public void shutdownOutput() throws IOException {
    this.sock.shutdownOutput();
  }
  
  public String toString() {
    return this.sock.toString();
  }
}
