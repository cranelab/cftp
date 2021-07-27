package com.crushftp.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.cert.X509Certificate;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class UnsafeSSLSocketFactory extends SSLSocketFactory {
  private SSLSocketFactory factory;
  
  public UnsafeSSLSocketFactory() {
    TrustManager[] trustAllCerts = { new X509TrustManager(this) {
          final UnsafeSSLSocketFactory this$0;
          
          public X509Certificate[] getAcceptedIssuers() {
            return null;
          }
          
          public void checkClientTrusted(X509Certificate[] certs, String authType) {}
          
          public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        } };
    try {
      SSLContext sslcontext = SSLContext.getInstance("TLS");
      sslcontext.init(null, trustAllCerts, null);
      this.factory = sslcontext.getSocketFactory();
    } catch (Exception e) {
      Common.log("SERVER", 0, e);
    } 
  }
  
  public static SocketFactory getDefault() {
    return new UnsafeSSLSocketFactory();
  }
  
  public Socket createSocket() throws IOException {
    return this.factory.createSocket();
  }
  
  public Socket createSocket(Socket socket, String s, int i, boolean flag) throws IOException {
    return this.factory.createSocket(socket, s, i, flag);
  }
  
  public Socket createSocket(InetAddress inaddr, int i, InetAddress inaddr1, int j) throws IOException {
    return this.factory.createSocket(inaddr, i, inaddr1, j);
  }
  
  public Socket createSocket(InetAddress inaddr, int i) throws IOException {
    return this.factory.createSocket(inaddr, i);
  }
  
  public Socket createSocket(String s, int i, InetAddress inaddr, int j) throws IOException {
    return this.factory.createSocket(s, i, inaddr, j);
  }
  
  public Socket createSocket(String s, int i) throws IOException {
    return this.factory.createSocket(s, i);
  }
  
  public String[] getDefaultCipherSuites() {
    return this.factory.getDefaultCipherSuites();
  }
  
  public String[] getSupportedCipherSuites() {
    return this.factory.getSupportedCipherSuites();
  }
}
