package com.crushftp.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.Properties;
import java.util.Vector;

public class DMZQueueSocket extends Socket {
  Socket sock = null;
  
  InputStream in2 = null;
  
  OutputStream out2 = null;
  
  public static Vector data_socks_active = new Vector();
  
  public static Vector data_socks_available = new Vector();
  
  String sock_id = Common.makeBoundary(8);
  
  public DMZQueueSocket(Socket sock) throws IOException {
    this.sock = sock;
  }
  
  public void bind(SocketAddress bindpoint) throws IOException {
    this.sock.bind(bindpoint);
  }
  
  public synchronized void close() throws IOException {
    close2();
  }
  
  public synchronized void close2() throws IOException {
    System.out.println(String.valueOf(System.currentTimeMillis()) + ":" + Character.MIN_VALUE + ":SOCK_CLOSE_START:");
    this.sock_id = Common.makeBoundary(8);
    this.in2 = null;
    this.out2 = null;
    if (Common.dmz_mode) {
      Vector data_sock_available = (Vector)Common.System2.get("crushftp.dmz.data_sock_available");
      Properties p = new Properties();
      p.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      p.put("sock", this);
      data_sock_available.insertElementAt(p, 0);
    } 
    System.out.println(String.valueOf(System.currentTimeMillis()) + ":" + Character.MIN_VALUE + ":SOCK_CLOSE_END:");
  }
  
  public synchronized void disconnect() throws IOException {
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
    if (this.in2 == null)
      this.in2 = new DMZQueueSocket$1$CheckedInputStream(this, this.sock.getInputStream()); 
    return this.in2;
  }
  
  public OutputStream getOutputStream() throws IOException {
    if (this.out2 == null)
      this.out2 = new DMZQueueSocket$1$CheckedOutputStream(this, this.sock.getOutputStream()); 
    return this.out2;
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
