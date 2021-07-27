package com.crushftp.client;

import com.crushftp.tunnel2.Tunnel2;
import com.didisoft.pgp.PGPLib;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

public class GenericClient {
  GenericClient thisObj = null;
  
  InputStream in = null;
  
  OutputStream out = null;
  
  InputStream in3 = null;
  
  OutputStream out3 = null;
  
  String url = null;
  
  Properties config = new Properties();
  
  public static final String[] months = new String[] { 
      "not zero based", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", 
      "Oct", "Nov", "Dec" };
  
  public final SimpleDateFormat yyyymmddHHmm = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
  
  public final SimpleDateFormat rfc1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
  
  public final SimpleDateFormat mm = new SimpleDateFormat("MM", Locale.US);
  
  public final SimpleDateFormat mmm = new SimpleDateFormat("MMM", Locale.US);
  
  public final SimpleDateFormat dd = new SimpleDateFormat("dd", Locale.US);
  
  public final SimpleDateFormat yyyy = new SimpleDateFormat("yyyy", Locale.US);
  
  public final SimpleDateFormat hhmm = new SimpleDateFormat("HH:mm", Locale.US);
  
  static Common common_code = null;
  
  PGPLib pgp = null;
  
  Vector logQueue = null;
  
  String logHeader = "";
  
  Properties statCache = null;
  
  public static Object tunnelLock = new Object();
  
  public GenericClient(String logHeader, Vector logQueue) {
    this.thisObj = this;
    this.logHeader = logHeader;
    this.logQueue = logQueue;
  }
  
  public void log(String s) {
    if (this.logQueue != null) {
      this.logQueue.addElement(String.valueOf(Common.last(getClass().getName().replace('.', '/'))) + ":" + this.logHeader + s);
    } else {
      System.out.print(String.valueOf(Common.last(getClass().getName().replace('.', '/'))) + ":" + new Date() + ":" + this.logHeader + s + "\r\n");
    } 
  }
  
  public void setCache(Properties statCache) {
    this.statCache = statCache;
  }
  
  public Properties getCache() {
    return this.statCache;
  }
  
  public void log(Throwable e) {
    log(Thread.currentThread().getName());
    log((String)e);
    StackTraceElement[] ste = e.getStackTrace();
    for (int x = 0; x < ste.length; x++)
      log(String.valueOf(ste[x].getClassName()) + "." + ste[x].getMethodName() + ":" + ste[x].getLineNumber()); 
  }
  
  public void log(Exception e) {
    log(Thread.currentThread().getName());
    log((String)e);
    StackTraceElement[] ste = e.getStackTrace();
    for (int x = 0; x < ste.length; x++)
      log(String.valueOf(ste[x].getClassName()) + "." + ste[x].getMethodName() + ":" + ste[x].getLineNumber()); 
  }
  
  public String login(String username, String password, String clientid) throws Exception {
    if (password.startsWith("DES:"))
      try {
        password = Common.encryptDecrypt(password.substring(4), false);
      } catch (Exception e) {
        password = Common.encryptDecrypt(password.replace('\\', '/').substring(4), false);
      }  
    return login2(username, password, clientid);
  }
  
  public String login2(String username, String password, String clientid) throws Exception {
    return "";
  }
  
  public void setupConfig(Properties prefs, Properties item) {
    Enumeration keys = prefs.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      if (key.startsWith("config_")) {
        setConfig(key.substring("config_".length()), prefs.get(key));
        continue;
      } 
      if (!this.config.containsKey(key))
        setConfig(key, prefs.get(key)); 
    } 
    if (!prefs.getProperty("keystore_path", "").equals(""))
      setConfig("keystore_path", prefs.getProperty("keystore_path", "")); 
    if (!prefs.getProperty("trustore_path", "").equals(""))
      setConfig("trustore_path", prefs.getProperty("trustore_path", "")); 
    if (!prefs.getProperty("keystore_pass", "").equals(""))
      setConfig("keystore_pass", prefs.getProperty("keystore_pass", "")); 
    if (!prefs.getProperty("key_pass", "").equals(""))
      setConfig("key_pass", prefs.getProperty("key_pass", "")); 
    if (!prefs.getProperty("acceptAnyCert", "").equals(""))
      setConfig("acceptAnyCert", prefs.getProperty("acceptAnyCert", "false")); 
    if (!prefs.getProperty("ssh_private_key", "").equals(""))
      setConfig("ssh_private_key", prefs.getProperty("ssh_private_key", "")); 
    if (!prefs.getProperty("ssh_private_key_pass", "").equals(""))
      setConfig("ssh_private_key_pass", prefs.getProperty("ssh_private_key_pass", "")); 
    if (!prefs.getProperty("ssh_two_factor", "").equals(""))
      setConfig("ssh_two_factor", prefs.getProperty("ssh_two_factor", "")); 
    if (item != null) {
      setConfig("use_dmz", item.getProperty("use_dmz", "false"));
      setConfig("pasv", item.getProperty("pasv", "true"));
      setConfig("no_os400", item.getProperty("no_os400", "false"));
    } 
  }
  
  public static Properties copyConnectionItems(Properties item) {
    Properties tempPrefs = new Properties();
    tempPrefs.put("ssh_private_key", item.getProperty("ssh_private_key", ""));
    tempPrefs.put("ssh_private_key_pass", item.getProperty("ssh_private_key_pass", ""));
    tempPrefs.put("ssh_two_factor", item.getProperty("ssh_two_factor", "false"));
    tempPrefs.put("keystore_path", item.getProperty("keystore_path", ""));
    tempPrefs.put("trustore_path", item.getProperty("trustore_path", ""));
    tempPrefs.put("keystore_pass", item.getProperty("keystore_pass", ""));
    tempPrefs.put("key_pass", item.getProperty("key_pass", ""));
    tempPrefs.put("acceptAnyCert", item.getProperty("acceptAnyCert", "false"));
    return tempPrefs;
  }
  
  public void logout() throws Exception {
    close();
  }
  
  public Vector list(String path, Vector list) throws Exception {
    return null;
  }
  
  public InputStream download(String path, long startPos, long endPos, boolean binary) throws Exception {
    checkTunnel();
    String originalUrl = this.url;
    if (getConfig("tunnel_active", "false").equals("true")) {
      log("Using tunnel:" + this.url + "  -->  " + getConfig("tunnel_url", this.url));
      this.url = getConfig("tunnel_url", this.url);
    } 
    try {
      if (getConfig("haDownload", "false").equals("true")) {
        this.in = new HADownload(this, path, startPos, endPos, binary, Integer.parseInt(getConfig("haDownloadDelay", "10")));
      } else {
        this.in = download2(path, startPos, endPos, binary);
      } 
    } finally {
      this.url = originalUrl;
    } 
    return this.in;
  }
  
  public InputStream getLimitedInputStream(InputStream in_tmp1, long startPos, long endPos) {
    return new null.InputWrapper(this, in_tmp1, startPos, endPos);
  }
  
  public void checkTunnel() {
    if (getConfig("use_tunnel", "false").equals("true") && getConfig("no_tunnels", "false").equals("false") && getConfig("tunnel_active", "false").equals("false"))
      synchronized (tunnelLock) {
        Tunnel2.setLog(this.logQueue);
        try {
          VRL vrl = new VRL(this.url);
          if (vrl.getProtocol().equalsIgnoreCase("http") || vrl.getProtocol().equalsIgnoreCase("https")) {
            Tunnel2 t = new Tunnel2(this.url, vrl.getUsername(), vrl.getPassword(), false);
            t.setAuth(this.config.getProperty("crushAuth", ""));
            Properties serverTunnel = (Properties)getConfig("serverTunnel");
            if (serverTunnel == null)
              serverTunnel = t.getTunnel(); 
            if (serverTunnel == null || serverTunnel.size() == 0)
              setConfig("no_tunnels", "true"); 
            if (serverTunnel != null && serverTunnel.size() > 0) {
              setConfig("tunnel_active", "true");
              setConfig("serverTunnel", serverTunnel);
              Properties tunnelInfo = new Properties();
              setConfig("tunnelInfo", tunnelInfo);
              if (serverTunnel != null && serverTunnel.size() > 0) {
                tunnelInfo.put("tunnelActive", "true");
                tunnelInfo.put("serverTunnel", serverTunnel);
                t.setTunnel(serverTunnel);
                ServerSocket ss = new ServerSocket(0);
                int tunnelPort = ss.getLocalPort();
                serverTunnel.put("localPort", (new StringBuffer(String.valueOf(tunnelPort))).toString());
                tunnelInfo.put("localPort", (new StringBuffer(String.valueOf(tunnelPort))).toString());
                setConfig("tunnel_url", "http://127.0.0.1:" + tunnelPort + "/");
                ss.close();
                log("Starting CrushTunnel:" + serverTunnel);
                (new Thread(new Runnable(this, tunnelInfo, t) {
                      final GenericClient this$0;
                      
                      private final Properties val$tunnelInfo;
                      
                      private final Tunnel2 val$t;
                      
                      public void run() {
                        this.val$tunnelInfo.put("emptyTunnelCount", "0");
                        Thread.currentThread().setName(String.valueOf(System.getProperty("crushsync.appname", "CrushSync")) + ":TunnelManager");
                        try {
                          this.val$t.startThreads();
                          while (this.val$t.isActive() && Integer.parseInt(this.val$tunnelInfo.getProperty("emptyTunnelCount", "0")) < 30) {
                            Thread.sleep(1000L);
                            if (this.val$t.getQueueCount() <= 1) {
                              this.val$tunnelInfo.put("emptyTunnelCount", (new StringBuffer(String.valueOf(Integer.parseInt(this.val$tunnelInfo.getProperty("emptyTunnelCount", "0")) + 1))).toString());
                            } else {
                              this.val$tunnelInfo.put("emptyTunnelCount", "0");
                            } 
                            this.this$0.setConfig("tunnel_status", "(Tunnel is Active: Out=" + this.val$t.getSends() + ", In=" + this.val$t.getGets() + ")");
                          } 
                        } catch (Exception e) {
                          this.this$0.log(e);
                        } finally {
                          this.this$0.setConfig("tunnel_status", "");
                          this.this$0.setConfig("tunnel_active", "false");
                          this.this$0.log("Closing idle CrushTunnel:active=" + this.val$t.isActive());
                          if (this.val$t.isActive())
                            this.val$t.stopThisTunnel(); 
                          this.this$0.log("Closing idle CrushTunnel:closed");
                          this.val$tunnelInfo.put("tunnelActive", "false");
                        } 
                      }
                    })).start();
                log("Waiting for tunnel to start...");
                while (!t.isReady() && t.isActive())
                  Thread.sleep(100L); 
                log("Tunnel started:ready=" + t.isReady() + ":active=" + t.isActive());
              } 
            } 
          } 
        } catch (Exception e) {
          log(e);
        } 
      }  
  }
  
  protected InputStream download2(String path, long startPos, long endPos, boolean binary) throws Exception {
    String path2 = path;
    this.in3 = download3(path, startPos, endPos, binary);
    InputStream in3f1 = this.in3;
    if (Common.System2.get("crushftp.dmz.queue.sock") == null && getConfig("pgpDecryptDownload", "false").equals("true")) {
      if (this.pgp == null)
        this.pgp = new PGPLib(); 
      this.pgp.setUseExpiredKeys(true);
      Properties socks = Common.getConnectedSocks(false);
      Socket sock1 = (Socket)socks.get("sock1");
      Socket sock2 = (Socket)socks.get("sock2");
      (new Thread(new Runnable(this, in3f1, sock1) {
            final GenericClient this$0;
            
            private final InputStream val$in3f1;
            
            private final Socket val$sock1;
            
            public void run() {
              try {
                String keyLocation = this.this$0.getConfig("pgpPrivateKeyDownloadPath").toString();
                if (keyLocation.indexOf(":") < 0 || keyLocation.indexOf(":") < 3)
                  keyLocation = "FILE://" + keyLocation; 
                VRL key_vrl = new VRL(keyLocation);
                ByteArrayOutputStream baos_key = new ByteArrayOutputStream();
                GenericClient c_key = Common.getClient(Common.getBaseUrl(key_vrl.toString()), "CrushFTP", new Vector());
                Common.streamCopier(c_key.download(key_vrl.getPath(), 0L, -1L, true), baos_key, false, true, true);
                c_key.logout();
                ByteArrayInputStream bytesIn = new ByteArrayInputStream(baos_key.toByteArray());
                this.this$0.pgp.setCompression("UNCOMPRESSED");
                this.this$0.pgp.decryptStream(this.val$in3f1, bytesIn, this.this$0.getConfig("pgpPrivateKeyDownloadPassword").toString(), this.val$sock1.getOutputStream());
                bytesIn.close();
              } catch (Exception e) {
                e.printStackTrace();
                try {
                  this.val$in3f1.close();
                } catch (IOException e1) {
                  e1.printStackTrace();
                } 
              } 
            }
          })).start();
      this.in3 = sock2.getInputStream();
    } 
    InputStream in3f2 = this.in3;
    if (Common.System2.get("crushftp.dmz.queue.sock") == null && getConfig("pgpEncryptDownload", "false").equals("true")) {
      if (this.pgp == null)
        this.pgp = new PGPLib(); 
      this.pgp.setUseExpiredKeys(true);
      Properties socks = Common.getConnectedSocks(false);
      Socket sock1 = (Socket)socks.get("sock1");
      Socket sock2 = (Socket)socks.get("sock2");
      (new Thread(new Runnable(this, in3f2, path2, sock1) {
            final GenericClient this$0;
            
            private final InputStream val$in3f2;
            
            private final String val$path2;
            
            private final Socket val$sock1;
            
            public void run() {
              try {
                String keyLocation = this.this$0.getConfig("pgpPublicKeyDownloadPath").toString();
                if (keyLocation.indexOf(":") < 0 || keyLocation.indexOf(":") < 3)
                  keyLocation = "FILE://" + keyLocation; 
                VRL key_vrl = new VRL(keyLocation);
                ByteArrayOutputStream baos_key = new ByteArrayOutputStream();
                GenericClient c_key = Common.getClient(Common.getBaseUrl(key_vrl.toString()), "CrushFTP", new Vector());
                Common.streamCopier(c_key.download(key_vrl.getPath(), 0L, -1L, true), baos_key, false, true, true);
                c_key.logout();
                ByteArrayInputStream bytesIn = new ByteArrayInputStream(baos_key.toByteArray());
                this.this$0.pgp.setCompression("UNCOMPRESSED");
                this.this$0.pgp.encryptStream(this.val$in3f2, Common.last(this.val$path2), bytesIn, this.val$sock1.getOutputStream(), this.this$0.getConfig("pgpAsciiDownload", "false").equals("true"), false);
                bytesIn.close();
              } catch (Exception e) {
                e.printStackTrace();
                try {
                  this.val$in3f2.close();
                } catch (IOException e1) {
                  e1.printStackTrace();
                } 
              } 
            }
          })).start();
      this.in3 = sock2.getInputStream();
    } 
    return this.in3;
  }
  
  protected void setupTimeout(Properties byteCount, Socket transfer_socket) {
    if (!this.config.getProperty("timeout", "0").equals("0")) {
      byteCount.put("transfer_socket2", transfer_socket);
      Thread t = new Thread(new Runnable(this, byteCount) {
            final GenericClient this$0;
            
            private final Properties val$byteCount;
            
            public void run() {
              Socket transfer_socket2 = (Socket)this.val$byteCount.remove("transfer_socket2");
              Thread.currentThread().setName("FTPClientTimeout:" + this.this$0.config.getProperty("timeout", "0") + ":" + transfer_socket2);
              long lastByteCount = Long.parseLong(this.val$byteCount.getProperty("b", "0"));
              while (this.val$byteCount.getProperty("status", "").equals("") && !transfer_socket2.isClosed()) {
                int secs = Integer.parseInt(this.this$0.config.getProperty("timeout", "0"));
                if (Long.parseLong(this.val$byteCount.getProperty("b", "0")) == lastByteCount) {
                  Common.log("FTP_CLIENT", 0, "Connection timeout:" + Thread.currentThread().getName());
                  try {
                    transfer_socket2.close();
                  } catch (IOException iOException) {}
                  return;
                } 
                lastByteCount = Long.parseLong(this.val$byteCount.getProperty("b", "0"));
                for (int x = 0; x < secs && this.val$byteCount.getProperty("status", "").equals(""); x++) {
                  try {
                    Thread.sleep(1000L);
                  } catch (InterruptedException interruptedException) {}
                } 
              } 
            }
          });
      t.start();
    } 
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary) throws Exception {
    return null;
  }
  
  public OutputStream upload(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    checkTunnel();
    String originalUrl = this.url;
    if (getConfig("tunnel_active", "false").equals("true")) {
      log("Using tunnel:" + this.url + "  -->  " + getConfig("tunnel_url", this.url));
      this.url = getConfig("tunnel_url", this.url);
    } 
    try {
      if (getConfig("haUpload", "false").equals("true")) {
        this.out = new HAUpload(this, path, startPos, truncate, binary, Integer.parseInt(getConfig("haUploadPriorWriteCount", "130")), Integer.parseInt(getConfig("haUploadDelay", "10")));
      } else {
        this.out = upload2(path, startPos, truncate, binary);
      } 
    } finally {
      this.url = originalUrl;
    } 
    return this.out;
  }
  
  public boolean upload_0_byte(String path) throws Exception {
    upload3(path, 0L, true, true).close();
    return true;
  }
  
  protected OutputStream upload2(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    String path2 = path;
    this.out3 = upload3(path, startPos, truncate, binary);
    OutputStream out3f1 = this.out3;
    if (Common.System2.get("crushftp.dmz.queue.sock") == null && getConfig("pgpEncryptUpload", "false").equals("true")) {
      if (this.pgp == null)
        this.pgp = new PGPLib(); 
      this.pgp.setUseExpiredKeys(true);
      Properties socks = Common.getConnectedSocks(false);
      Socket sock1 = (Socket)socks.remove("sock1");
      Socket sock2 = (Socket)socks.remove("sock2");
      Properties status = new Properties();
      (new Thread(new Runnable(this, sock1, path2, out3f1, status) {
            final GenericClient this$0;
            
            private final Socket val$sock1;
            
            private final String val$path2;
            
            private final OutputStream val$out3f1;
            
            private final Properties val$status;
            
            public void run() {
              try {
                byte[] publicBytes = new byte[(int)(new File(this.this$0.getConfig("pgpPublicKeyUploadPath").toString())).length()];
                FileInputStream inFile = null;
                try {
                  inFile = new FileInputStream(new File(this.this$0.getConfig("pgpPublicKeyUploadPath").toString()));
                  inFile.read(publicBytes);
                } finally {
                  if (inFile != null)
                    inFile.close(); 
                } 
                ByteArrayInputStream bytesIn = new ByteArrayInputStream(publicBytes);
                this.this$0.pgp.setCompression("UNCOMPRESSED");
                if (this.this$0.getConfig("pgpAsciiUpload", "false").equals("true"))
                  this.this$0.pgp.setAsciiVersionHeader("CRUSHFTP#                                        "); 
                this.this$0.pgp.encryptStream(this.val$sock1.getInputStream(), Common.last(this.val$path2), bytesIn, this.val$out3f1, this.this$0.getConfig("pgpAsciiUpload", "false").equals("true"), false);
                bytesIn.close();
                this.val$status.put("status", "SUCCESS");
              } catch (Exception e) {
                this.val$status.put("error", e);
                this.val$status.put("status", "ERROR");
                try {
                  this.val$out3f1.close();
                } catch (IOException e1) {
                  e1.printStackTrace();
                } 
              } 
            }
          })).start();
      this.out3 = new OutputStreamCloser(sock2.getOutputStream(), status, this.thisObj, path, getConfig("pgpAsciiUpload", "false").equals("false"), getConfig("pgpAsciiUpload", "false").equals("true"), out3f1);
    } 
    OutputStream out3f2 = this.out3;
    if (Common.System2.get("crushftp.dmz.queue.sock") == null && getConfig("pgpDecryptUpload", "false").equals("true")) {
      if (this.pgp == null)
        this.pgp = new PGPLib(); 
      this.pgp.setUseExpiredKeys(true);
      Properties socks = Common.getConnectedSocks(false);
      Socket sock1 = (Socket)socks.get("sock1");
      Socket sock2 = (Socket)socks.get("sock2");
      Properties status = new Properties();
      (new Thread(new Runnable(this, sock1, out3f2, status) {
            final GenericClient this$0;
            
            private final Socket val$sock1;
            
            private final OutputStream val$out3f2;
            
            private final Properties val$status;
            
            public void run() {
              try {
                byte[] privateKeyBytes = new byte[(int)(new File(this.this$0.getConfig("pgpPrivateKeyUploadPath").toString())).length()];
                FileInputStream inFile = null;
                try {
                  inFile = new FileInputStream(new File(this.this$0.getConfig("pgpPrivateKeyUploadPath").toString()));
                  inFile.read(privateKeyBytes);
                } finally {
                  if (inFile != null)
                    inFile.close(); 
                } 
                ByteArrayInputStream bytesIn = new ByteArrayInputStream(privateKeyBytes);
                this.this$0.pgp.setCompression("UNCOMPRESSED");
                this.this$0.pgp.decryptStream(this.val$sock1.getInputStream(), bytesIn, this.this$0.getConfig("pgpPrivateKeyUploadPassword", "").toString(), this.val$out3f2);
                bytesIn.close();
                this.val$status.put("status", "SUCCESS");
              } catch (Exception e) {
                this.val$status.put("error", e);
                this.val$status.put("status", "ERROR");
                try {
                  this.val$out3f2.close();
                } catch (IOException e1) {
                  e1.printStackTrace();
                } 
              } 
            }
          })).start();
      this.out3 = new OutputStreamCloser(sock2.getOutputStream(), status, this.thisObj, path, false, false, out3f2);
    } 
    return this.out3;
  }
  
  protected OutputStream upload3(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    return null;
  }
  
  public boolean delete(String path) throws Exception {
    return false;
  }
  
  public boolean makedir(String path) throws Exception {
    return false;
  }
  
  public boolean makedirs(String path) throws Exception {
    return false;
  }
  
  public boolean rename(String rnfr, String rnto) throws Exception {
    return false;
  }
  
  public Properties stat(String path) throws Exception {
    return null;
  }
  
  public void setMod(String path, String val, String param) throws Exception {}
  
  public void setOwner(String path, String val, String param) throws Exception {}
  
  public void setGroup(String path, String val, String param) throws Exception {}
  
  public boolean mdtm(String path, long modified) throws Exception {
    return false;
  }
  
  public void close() throws Exception {
    if (this.in != null) {
      this.in.close();
      this.in = null;
    } 
    if (this.out != null) {
      this.out.close();
      this.out = null;
    } 
  }
  
  public String doCommand(String command) throws Exception {
    return "";
  }
  
  public void setConfigObj(Properties config) {
    config.putAll(this.config);
    this.config = config;
  }
  
  public void setConfig(String key, Object o) {
    if (o == null) {
      this.config.remove(key);
    } else {
      this.config.put(key, o);
    } 
  }
  
  public String getConfig(String key, String s) {
    return this.config.getProperty(key, s);
  }
  
  public Object getConfig(String key) {
    return this.config.get(key);
  }
  
  public long getLength(String path) throws Exception {
    Properties p = stat(path);
    if (p == null)
      return 0L; 
    return Long.parseLong(p.getProperty("size", "0"));
  }
  
  public long getLastModified(String path) throws Exception {
    Properties p = stat(path);
    return Long.parseLong(p.getProperty("modified", "0"));
  }
  
  public static String u(String s) throws Exception {
    String r = Common.makeBoundary(20);
    s = s.replaceAll(" ", r);
    s = URLEncoder.encode(s, "utf-8");
    s = s.replaceAll(r, "%20");
    return s;
  }
  
  public static void writeEntry(String key, String val, BufferedOutputStream dos, String boundary) throws Exception {
    dos.write((String.valueOf(boundary) + "\r\n").getBytes("UTF8"));
    dos.write(("Content-Disposition: form-data; name=\"" + key + "\"\r\n").getBytes("UTF8"));
    dos.write("\r\n".getBytes("UTF8"));
    dos.write((String.valueOf(val) + "\r\n").getBytes("UTF8"));
  }
  
  public static void writeEnd(BufferedOutputStream dos, String boundary) throws Exception {
    dos.write((String.valueOf(boundary) + "--\r\n").getBytes("UTF8"));
    dos.flush();
    dos.close();
  }
  
  public static Properties parseStat(String data) throws Exception {
    data = data.trim();
    if (data.equals(""))
      return null; 
    StringTokenizer st = new StringTokenizer(data);
    Properties item = new Properties();
    item.put("privs", st.nextToken());
    if (item.getProperty("privs").toUpperCase().startsWith("D")) {
      item.put("permissions", "drwxrwxrwx");
      item.put("type", "DIR");
    } else {
      item.put("permissions", "-rwxrwxrwx");
      item.put("type", "FILE");
    } 
    item.put("count", st.nextToken());
    item.put("num_items", item.getProperty("count"));
    item.put("owner", st.nextToken());
    item.put("group", st.nextToken());
    item.put("size", st.nextToken());
    String dateStr = st.nextToken();
    item.put("modified", dateStr);
    SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
    Date d = yyyyMMddHHmmss.parse(dateStr);
    SimpleDateFormat mmm = new SimpleDateFormat("MMM", Locale.US);
    item.put("month", mmm.format(d));
    item.put("day", st.nextToken());
    String year = st.nextToken();
    item.put("time_or_year", year);
    String root_dir = st.nextToken();
    root_dir = data.substring(data.indexOf(" " + year + " " + root_dir) + (" " + year + " ").length());
    root_dir = root_dir.replaceAll("&amp;", "&").replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("%5C", "\\");
    item.put("path", Common.all_but_last(root_dir));
    item.put("name", Common.last(root_dir));
    setFileDateInfo(item, d);
    if (item.getProperty("type").equalsIgnoreCase("DIR"))
      item.put("size", "1"); 
    return item;
  }
  
  public static Properties parseDmzStat(String data) throws Exception {
    data = data.trim();
    if (data.equals(""))
      return null; 
    String[] items = data.split(";");
    Properties item = new Properties();
    for (int x = 0; x < items.length; x++) {
      if (!items[x].trim().equals("")) {
        String[] parts = items[x].split("=");
        String key = parts[0];
        String val = "";
        if (parts.length > 1)
          val = parts[1]; 
        item.put(key, Common.url_decode(val));
      } 
    } 
    return item;
  }
  
  public static void setFileDateInfo(Properties dir_item, Date itemDate) {
    SimpleDateFormat mm = new SimpleDateFormat("MM", Locale.US);
    SimpleDateFormat dd = new SimpleDateFormat("dd", Locale.US);
    SimpleDateFormat yyyy = new SimpleDateFormat("yyyy", Locale.US);
    SimpleDateFormat hhmm = new SimpleDateFormat("HH:mm", Locale.US);
    dir_item.put("modified", (new StringBuffer(String.valueOf(itemDate.getTime()))).toString());
    dir_item.put("month", months[Integer.parseInt(mm.format(itemDate))]);
    dir_item.put("day", dd.format(itemDate));
    String time_or_year = hhmm.format(itemDate);
    if (!yyyy.format(itemDate).equals(yyyy.format(new Date())))
      time_or_year = yyyy.format(itemDate); 
    dir_item.put("time_or_year", time_or_year);
  }
}
