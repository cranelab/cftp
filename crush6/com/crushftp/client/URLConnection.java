package com.crushftp.client;

import com.crushftp.tunnel2.DProperties;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Vector;
import java.util.zip.ZipInputStream;
import javax.net.ssl.SSLSocket;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

public class URLConnection {
  String method = "GET";
  
  int responseCode = -1;
  
  String message = null;
  
  Socket sock = null;
  
  VRL u = null;
  
  Properties config = null;
  
  boolean connected = false;
  
  Properties requestProps = new Properties();
  
  boolean doOutput = false;
  
  boolean outputDone = true;
  
  boolean gotHeaders = false;
  
  Properties headers = new Properties();
  
  OutputStream outputProxy = null;
  
  long maxRead = -1L;
  
  long len = -1L;
  
  boolean sendChunked = false;
  
  boolean send_compress = false;
  
  boolean receive_compress = false;
  
  boolean receiveChunked = false;
  
  boolean autoClose = false;
  
  boolean allowPool = true;
  
  boolean headersFinished = false;
  
  ByteArrayOutputStream buffer = new ByteArrayOutputStream();
  
  OutputStream bufferedOut = null;
  
  Date date = new Date();
  
  public SimpleDateFormat sdf_rfc1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
  
  public static Thread socketCleaner = null;
  
  int read_timeout = 0;
  
  String bind_ip = null;
  
  static final String skip_encode_chars = "/%.#@&?!\\=+";
  
  public static Vector cipher_suites = null;
  
  public static String last_cipher = null;
  
  public static String preferred_cipher = null;
  
  protected URLConnection(VRL u, Properties config) {
    this.sdf_rfc1123.setTimeZone(TimeZone.getTimeZone("GMT"));
    this.u = u;
    this.config = config;
    this.requestProps.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
    this.requestProps.put("Cache-Control", "no-cache");
    this.requestProps.put("Pragma", "no-cache");
    this.requestProps.put("User-Agent", "CrushClient" + config.getProperty("protocol", "DAV") + "/6.0 (Generic OS 6.0) Java");
    this.requestProps.put("Accept", "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2");
    Enumeration keys = config.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      if (key.startsWith("proxy_")) {
        String val = config.getProperty(key);
        this.requestProps.put("X-" + key.toUpperCase(), val);
      } 
    } 
    if (socketCleaner == null)
      startSocketCleaner(); 
  }
  
  public static URLConnection openConnection(VRL u, Properties config) {
    return new HttpURLConnection(u, config);
  }
  
  public void disconnect() throws IOException {
    if (this.connected) {
      if (this.autoClose || !this.allowPool || this.sendChunked || this.receiveChunked) {
        if (this.outputProxy != null) {
          synchronized (this.outputProxy) {
            this.sock.close();
          } 
        } else {
          this.sock.close();
        } 
      } else {
        Common.releaseSocket(this.sock, this.u, this.config.getProperty("crushAuth", ""));
      } 
      this.connected = false;
    } 
  }
  
  public void setAllowPool(boolean allowPool) {
    this.allowPool = allowPool;
  }
  
  public void setUseChunkedStreaming(boolean chunked) {
    this.sendChunked = chunked;
  }
  
  public void setSendCompression(boolean send_compress) {
    this.send_compress = send_compress;
  }
  
  public void setReceiveCompression(boolean receive_compress) {
    this.receive_compress = receive_compress;
  }
  
  public void setChunkedStreamingMode(long size) {
    this.sendChunked = true;
  }
  
  public boolean isChunkedSend() {
    return this.sendChunked;
  }
  
  public boolean isChunkedReceive() {
    return this.receiveChunked;
  }
  
  public void setBindIp(String bind_ip) {
    this.bind_ip = bind_ip;
  }
  
  public String getBindIp() {
    return (this.bind_ip == null) ? "0.0.0.0" : this.bind_ip;
  }
  
  public void startSocketCleaner() {
    synchronized (Common.socketPool) {
      if (socketCleaner == null) {
        socketCleaner = new Thread(new Runnable(this) {
              final URLConnection this$0;
              
              public void run() {
                Thread.currentThread().setName("URLConnection socket cleanup thread.");
                while (true) {
                  Common.socketTimeout = Integer.parseInt(System.getProperty("crushftp.socketpooltimeout", (new StringBuffer(String.valueOf(Common.socketTimeout))).toString()));
                  int sleep_time = Common.socketTimeout;
                  if (sleep_time == 0)
                    sleep_time = 1000; 
                  String threadName = "URLConnection socket cleanup thread:";
                  try {
                    synchronized (Common.socketPool) {
                      Enumeration keys = Common.socketPool.keys();
                      while (keys.hasMoreElements()) {
                        String key = keys.nextElement().toString();
                        Vector sockets = (Vector)Common.socketPool.get(key);
                        if (sockets.size() == 0) {
                          Common.socketPool.remove(key);
                          continue;
                        } 
                        if (threadName.length() > 5000) {
                          threadName = String.valueOf(threadName) + ".";
                        } else {
                          threadName = String.valueOf(threadName) + key + "=" + sockets.size() + ",";
                        } 
                        for (int x = sockets.size() - 1; x >= 0; x--) {
                          Properties info = sockets.elementAt(x);
                          if (System.currentTimeMillis() - Long.parseLong(info.getProperty("time")) > sleep_time) {
                            Socket sock = (Socket)info.remove("sock");
                            sockets.remove(x);
                            Common.log("HTTP_CLIENT", 2, sock + ":Closing expired socket.");
                            sock.close();
                          } 
                        } 
                      } 
                    } 
                  } catch (Exception e) {
                    threadName = String.valueOf(threadName) + e;
                  } 
                  Thread.currentThread().setName(threadName);
                  try {
                    Thread.sleep(sleep_time);
                  } catch (Exception exception) {}
                } 
              }
            });
        socketCleaner.start();
      } 
    } 
  }
  
  public void connect() throws IOException {
    if (this.connected)
      return; 
    int port = this.u.getPort();
    if (!this.requestProps.containsKey("Host"))
      this.requestProps.put("Host", this.u.getHost()); 
    if (!this.requestProps.containsKey("Date"))
      this.requestProps.put("Date", this.sdf_rfc1123.format(this.date)); 
    Vector socks = null;
    boolean lastTry = false;
    synchronized (Common.socketPool) {
      socks = (Vector)Common.socketPool.get(String.valueOf(this.u.getProtocol()) + ":" + this.u.getHost() + ":" + this.u.getPort());
    } 
    if (socks == null)
      socks = new Vector(); 
    while (true) {
      this.sock = Common.getSocket(this.config.getProperty("protocol", "DAV"), this.u, this.config.getProperty("use_dmz", "false"), this.config.getProperty("crushAuth", ""));
      try {
        if ((this.u.getProtocol().equalsIgnoreCase("HTTPS") && !(this.sock instanceof SSLSocket)) || (this.u.getProtocol().equalsIgnoreCase("WEBDAVS") && !(this.sock instanceof SSLSocket)))
          if (!this.u.getHost().equals("127.0.0.1") || System.getProperty("crushftp.dmz.ssl", "true").equals("true")) {
            SSLSocket ss = Common.getSSLSocket(this.config.getProperty("trustore_path", this.config.getProperty("keystore_path", "")), this.config.getProperty("keystore_pass", ""), this.config.getProperty("truststore_pass", ""), this.config.getProperty("acceptAnyCert", "true").equalsIgnoreCase("true"), this.sock, this.u.getHost(), port);
            if (preferred_cipher != null) {
              ss.setEnabledCipherSuites(new String[] { preferred_cipher });
            } else {
              Common.setEnabledCiphers(this.config.getProperty("disabled_ciphers", ""), ss, null);
            } 
            ss.setUseClientMode(true);
            ss.startHandshake();
            this.sock = ss;
            if (cipher_suites == null) {
              cipher_suites = new Vector();
              String[] suites = ss.getSupportedCipherSuites();
              for (int x = 0; x < suites.length; x++)
                cipher_suites.addElement(suites[x]); 
            } 
            last_cipher = ss.getSession().getCipherSuite();
          }  
        this.connected = true;
        this.bufferedOut = null;
        OutputStream tmpOut = this.sock.getOutputStream();
        String tmp_path = Common.url_encode(this.u.getPath(), "/%.#@&?!\\=+");
        tmpOut.write((String.valueOf(this.method.toUpperCase().trim()) + " " + tmp_path + (this.receive_compress ? ".zip" : "") + " HTTP/1.1" + "\r\n").getBytes("UTF8"));
        Common.log("HTTP_CLIENT", 2, String.valueOf(this.method.toUpperCase().trim()) + " " + tmp_path + " HTTP/1.1");
        tmpOut.flush();
        Enumeration keys = this.requestProps.keys();
        while (keys.hasMoreElements()) {
          String key = keys.nextElement().toString();
          String val = this.requestProps.getProperty(key);
          tmpOut.write((String.valueOf(key.trim()) + ": " + val + "\r\n").getBytes("UTF8"));
          Common.log("HTTP_CLIENT", 2, String.valueOf(key.trim()) + ": " + val);
        } 
        tmpOut.flush();
        if (this.bufferedOut == null)
          this.bufferedOut = new BufferedOutputStream(tmpOut); 
        if (this.len >= 0L || this.sendChunked)
          closeHeaders(); 
        if (!this.doOutput)
          buildResponseHeaders(); 
        this.sock.setSoTimeout(this.read_timeout);
        break;
      } catch (IOException e) {
        this.sock.close();
        this.connected = false;
        if (lastTry)
          throw e; 
        if (socks.size() == 0)
          lastTry = true; 
      } 
    } 
    this.sock.setSoTimeout(this.read_timeout);
  }
  
  public void setUseCaches(boolean b) {}
  
  public void setDoOutput(boolean doOutput) {
    this.doOutput = doOutput;
    this.outputDone = false;
  }
  
  public VRL getURL() {
    return this.u;
  }
  
  public void setLength(long len) {
    this.len = len;
  }
  
  private void closeHeaders() throws IOException {
    if (this.headersFinished)
      return; 
    if (this.send_compress) {
      this.bufferedOut.write("Content-Encoding: gzip\r\n".getBytes("UTF8"));
      Common.log("HTTP_CLIENT", 2, "Content-Encoding: gzip");
    } 
    if (this.sendChunked) {
      this.bufferedOut.write("Transfer-Encoding: chunked\r\n".getBytes("UTF8"));
      Common.log("HTTP_CLIENT", 2, "Transfer-Encoding: chunked");
    } else if (this.len >= 0L) {
      this.bufferedOut.write(("Content-Length: " + this.len + "\r\n").getBytes("UTF8"));
      Common.log("HTTP_CLIENT", 2, "Content-Length: " + this.len);
    } else if (this.len < 0L) {
      this.bufferedOut.write("Connection: close\r\n".getBytes("UTF8"));
      this.autoClose = true;
      Common.log("HTTP_CLIENT", 2, "Connection: close");
    } 
    this.bufferedOut.write("\r\n".getBytes("UTF8"));
    this.bufferedOut.flush();
    this.headersFinished = true;
  }
  
  private void buildResponseHeaders() throws IOException {
    connect();
    closeHeaders();
    if (this.gotHeaders || (this.doOutput && !this.outputDone))
      return; 
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    int bytesRead = 0;
    byte[] b = new byte[1];
    String headerStr = "";
    int line = 0;
    long start = System.currentTimeMillis();
    try {
      while (bytesRead >= 0) {
        bytesRead = this.sock.getInputStream().read(b);
        if (bytesRead >= 0) {
          baos.write(b);
          headerStr = String.valueOf(headerStr) + new String(b, "UTF8");
          if (!headerStr.equals("\r\n") && headerStr.endsWith("\r\n")) {
            if (line == 0) {
              headerStr = new String(baos.toByteArray(), "UTF8");
              this.responseCode = Integer.parseInt(headerStr.split(" ")[1].trim());
              this.message = headerStr.substring(headerStr.indexOf(" " + this.responseCode + " "));
            } else {
              String key = headerStr.substring(0, headerStr.indexOf(":")).trim();
              String val = headerStr.substring(headerStr.indexOf(":") + 1).trim();
              this.headers.put(key.toUpperCase(), val);
            } 
            headerStr = "";
            line++;
          } 
        } 
        if (headerStr.equals("\r\n"))
          break; 
      } 
    } finally {
      headerStr = new String(baos.toByteArray(), "UTF8");
      Common.log("HTTP_CLIENT", 2, "Waited " + (System.currentTimeMillis() - start) + " ms for header response.");
      Common.log("HTTP_CLIENT", 2, headerStr);
    } 
    if (this.headers.containsKey("CONTENT-LENGTH"))
      this.maxRead = Long.parseLong(this.headers.getProperty("CONTENT-LENGTH")); 
    if (this.headers.containsKey("TRANSFER-ENCODING"))
      this.receiveChunked = (this.headers.getProperty("TRANSFER-ENCODING").toUpperCase().indexOf("CHUNKED") >= 0); 
    this.gotHeaders = true;
  }
  
  public String getHeaderField(String key) {
    return this.headers.getProperty(key.toUpperCase());
  }
  
  public void setRequestMethod(String method) {
    this.method = method.toUpperCase();
  }
  
  public String getRequestMethod() {
    return this.method;
  }
  
  public Date getDate() {
    return this.date;
  }
  
  public void setDate(Date date) {
    this.date = date;
  }
  
  public String getContentType() {
    return this.requestProps.getProperty("Content-Type", "");
  }
  
  public void setRequestProperty(String key, String val) {
    if (val == null) {
      this.requestProps.remove(key);
    } else {
      this.requestProps.put(key, val);
    } 
  }
  
  public int getResponseCode() throws IOException {
    try {
      if (this.outputProxy != null)
        this.outputProxy.close(); 
    } catch (IOException e) {
      if (this.responseCode == -1)
        setDoOutput(false); 
    } 
    buildResponseHeaders();
    return this.responseCode;
  }
  
  public Properties getRequestProps() {
    return this.requestProps;
  }
  
  public String getResponseMessage() throws IOException {
    try {
      if (this.outputProxy != null)
        this.outputProxy.close(); 
    } catch (IOException e) {
      if (this.responseCode == -1)
        setDoOutput(false); 
    } 
    buildResponseHeaders();
    return this.message;
  }
  
  public InputStream getInputStream() throws IOException, SocketTimeoutException {
    connect();
    this.outputDone = true;
    buildResponseHeaders();
    InputStream in = this.sock.getInputStream();
    if (this.receiveChunked)
      in = new UnChunkInputStream(in); 
    if (this.receive_compress) {
      in = new ZipInputStream(in);
      ((ZipInputStream)in).getNextEntry();
      return in;
    } 
    return new null.InputStreamProxy(this, in);
  }
  
  public OutputStream getOutputStream() throws IOException {
    connect();
    this.outputProxy = new null.OutputStreamProxy(this, this.bufferedOut);
    if (this.send_compress)
      this.outputProxy = (OutputStream)new GzipCompressorOutputStream(this.outputProxy); 
    return this.outputProxy;
  }
  
  public void setReadTimeout(int read_timeout) throws IOException {
    this.read_timeout = read_timeout;
    if (this.sock != null)
      this.sock.setSoTimeout(read_timeout); 
  }
  
  public void setDoInput(boolean ignored) {}
  
  public static String consumeResponse(InputStream in) throws IOException {
    byte[] b = DProperties.getArray();
    int bytesRead = 0;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    while (bytesRead >= 0) {
      bytesRead = in.read(b);
      if (bytesRead > 0)
        baos.write(b, 0, bytesRead); 
    } 
    b = DProperties.releaseArray(b);
    in.close();
    String s = new String(baos.toByteArray(), "UTF8");
    Common.log("HTTP_CLIENT", 2, s);
    return s;
  }
}
