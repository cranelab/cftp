package com.sun.mail.util;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class WriteTimeoutSocket extends Socket {
  private final Socket socket;
  
  private final ScheduledExecutorService ses;
  
  private final int timeout;
  
  public WriteTimeoutSocket(Socket socket, int timeout) throws IOException {
    this.socket = socket;
    this.ses = Executors.newScheduledThreadPool(1);
    this.timeout = timeout;
  }
  
  public WriteTimeoutSocket(int timeout) throws IOException {
    this(new Socket(), timeout);
  }
  
  public WriteTimeoutSocket(InetAddress address, int port, int timeout) throws IOException {
    this(timeout);
    this.socket.connect(new InetSocketAddress(address, port));
  }
  
  public WriteTimeoutSocket(InetAddress address, int port, InetAddress localAddress, int localPort, int timeout) throws IOException {
    this(timeout);
    this.socket.bind(new InetSocketAddress(localAddress, localPort));
    this.socket.connect(new InetSocketAddress(address, port));
  }
  
  public WriteTimeoutSocket(String host, int port, int timeout) throws IOException {
    this(timeout);
    this.socket.connect(new InetSocketAddress(host, port));
  }
  
  public WriteTimeoutSocket(String host, int port, InetAddress localAddress, int localPort, int timeout) throws IOException {
    this(timeout);
    this.socket.bind(new InetSocketAddress(localAddress, localPort));
    this.socket.connect(new InetSocketAddress(host, port));
  }
  
  public void connect(SocketAddress remote) throws IOException {
    this.socket.connect(remote, 0);
  }
  
  public void connect(SocketAddress remote, int timeout) throws IOException {
    this.socket.connect(remote, timeout);
  }
  
  public void bind(SocketAddress local) throws IOException {
    this.socket.bind(local);
  }
  
  public SocketAddress getRemoteSocketAddress() {
    return this.socket.getRemoteSocketAddress();
  }
  
  public SocketAddress getLocalSocketAddress() {
    return this.socket.getLocalSocketAddress();
  }
  
  public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
    this.socket.setPerformancePreferences(connectionTime, latency, bandwidth);
  }
  
  public SocketChannel getChannel() {
    return this.socket.getChannel();
  }
  
  public InetAddress getInetAddress() {
    return this.socket.getInetAddress();
  }
  
  public InetAddress getLocalAddress() {
    return this.socket.getLocalAddress();
  }
  
  public int getPort() {
    return this.socket.getPort();
  }
  
  public int getLocalPort() {
    return this.socket.getLocalPort();
  }
  
  public InputStream getInputStream() throws IOException {
    return this.socket.getInputStream();
  }
  
  public synchronized OutputStream getOutputStream() throws IOException {
    return new TimeoutOutputStream(this.socket.getOutputStream(), this.ses, this.timeout);
  }
  
  public void setTcpNoDelay(boolean on) throws SocketException {
    this.socket.setTcpNoDelay(on);
  }
  
  public boolean getTcpNoDelay() throws SocketException {
    return this.socket.getTcpNoDelay();
  }
  
  public void setSoLinger(boolean on, int linger) throws SocketException {
    this.socket.setSoLinger(on, linger);
  }
  
  public int getSoLinger() throws SocketException {
    return this.socket.getSoLinger();
  }
  
  public void sendUrgentData(int data) throws IOException {
    this.socket.sendUrgentData(data);
  }
  
  public void setOOBInline(boolean on) throws SocketException {
    this.socket.setOOBInline(on);
  }
  
  public boolean getOOBInline() throws SocketException {
    return this.socket.getOOBInline();
  }
  
  public void setSoTimeout(int timeout) throws SocketException {
    this.socket.setSoTimeout(timeout);
  }
  
  public int getSoTimeout() throws SocketException {
    return this.socket.getSoTimeout();
  }
  
  public void setSendBufferSize(int size) throws SocketException {
    this.socket.setSendBufferSize(size);
  }
  
  public int getSendBufferSize() throws SocketException {
    return this.socket.getSendBufferSize();
  }
  
  public void setReceiveBufferSize(int size) throws SocketException {
    this.socket.setReceiveBufferSize(size);
  }
  
  public int getReceiveBufferSize() throws SocketException {
    return this.socket.getReceiveBufferSize();
  }
  
  public void setKeepAlive(boolean on) throws SocketException {
    this.socket.setKeepAlive(on);
  }
  
  public boolean getKeepAlive() throws SocketException {
    return this.socket.getKeepAlive();
  }
  
  public void setTrafficClass(int tc) throws SocketException {
    this.socket.setTrafficClass(tc);
  }
  
  public int getTrafficClass() throws SocketException {
    return this.socket.getTrafficClass();
  }
  
  public void setReuseAddress(boolean on) throws SocketException {
    this.socket.setReuseAddress(on);
  }
  
  public boolean getReuseAddress() throws SocketException {
    return this.socket.getReuseAddress();
  }
  
  public void close() throws IOException {
    try {
      this.socket.close();
    } finally {
      this.ses.shutdownNow();
    } 
  }
  
  public void shutdownInput() throws IOException {
    this.socket.shutdownInput();
  }
  
  public void shutdownOutput() throws IOException {
    this.socket.shutdownOutput();
  }
  
  public String toString() {
    return this.socket.toString();
  }
  
  public boolean isConnected() {
    return this.socket.isConnected();
  }
  
  public boolean isBound() {
    return this.socket.isBound();
  }
  
  public boolean isClosed() {
    return this.socket.isClosed();
  }
  
  public boolean isInputShutdown() {
    return this.socket.isInputShutdown();
  }
  
  public boolean isOutputShutdown() {
    return this.socket.isOutputShutdown();
  }
  
  public FileDescriptor getFileDescriptor$() {
    try {
      Method m = Socket.class.getDeclaredMethod("getFileDescriptor$", new Class[0]);
      return (FileDescriptor)m.invoke(this.socket, new Object[0]);
    } catch (Exception ex) {
      return null;
    } 
  }
}
