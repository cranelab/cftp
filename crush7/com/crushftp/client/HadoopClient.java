package com.crushftp.client;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import javax.net.ssl.SSLContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class HadoopClient extends GenericClient {
  static Properties resourceIdCache = new Properties();
  
  private String action = "";
  
  private String local_file = "";
  
  LoginContext loginContext = null;
  
  public static void main(String[] args) throws Exception {
    VRL vrl = new VRL(args[0]);
    HadoopClient instance = new HadoopClient(args[0], "HADOOP:", null);
    instance.parseOptions(args);
    instance.login(vrl.getUsername(), vrl.getPassword(), "");
    instance.query();
  }
  
  public void query() {
    try {
      VRL vrl = new VRL(this.url);
      this.loginContext = new LoginContext("KrbLogin", new KerberosCallBackHandler(this, this.config.getProperty("username"), this.config.getProperty("password")));
      this.loginContext.login();
      PrivilegedAction sendAction = new PrivilegedAction(this, vrl) {
          final HadoopClient this$0;
          
          private final VRL val$vrl;
          
          public Object run() {
            long start = System.currentTimeMillis();
            try {
              if (this.this$0.action.equals("list")) {
                Vector v = new Vector();
                this.this$0.list(this.val$vrl.getPath(), v);
                System.out.println(v);
              } else if (this.this$0.action.equals("download")) {
                FileOutputStream fout = new FileOutputStream(this.this$0.local_file, false);
                InputStream tin = this.this$0.download(this.val$vrl.getPath(), 0L, -1L, true);
                Common.streamCopier(tin, fout, false, true, true);
              } else if (this.this$0.action.equals("upload")) {
                FileInputStream fin = new FileInputStream(this.this$0.local_file);
                OutputStream tout = this.this$0.upload(this.val$vrl.getPath(), 0L, true, true);
                Common.streamCopier(fin, tout, false, true, true);
              } 
            } catch (Exception e) {
              e.printStackTrace();
            } 
            System.out.println("Transfer time:" + (System.currentTimeMillis() - start) + "ms");
            System.exit(0);
            return new Boolean(true);
          }
        };
      Subject.doAs(this.loginContext.getSubject(), sendAction);
    } catch (Exception le) {
      le.printStackTrace();
    } 
  }
  
  private void parseOptions(String[] args) {
    for (int i = 1; i < args.length - 1; i++) {
      if (args[i].equals("-l"))
        this.config.put("login_config", args[++i]); 
      if (args[i].equals("-k"))
        this.config.put("kerberos_config", args[++i]); 
      if (args[i].equals("-action")) {
        this.action = args[++i];
        System.out.println("action:" + this.action);
      } 
      if (args[i].equals("-file")) {
        this.local_file = args[++i];
        System.out.println("local_file:" + this.local_file);
      } 
    } 
  }
  
  private CloseableHttpClient createHttpClient() {
    String[] enabled_ciphers = (String[])null;
    if (System.getProperties().containsKey("crushftp.enabled_ciphers"))
      enabled_ciphers = System.getProperty("crushftp.enabled_ciphers").split(","); 
    HttpClientBuilder hcb = HttpClients.custom().setDefaultAuthSchemeRegistry(buildSPNEGOAuthSchemeRegistry()).setDefaultCredentialsProvider(setupCredentialsProvider());
    try {
      SSLContext sslc = null;
      if (this.config.getProperty("acceptAnyCert", "true").equalsIgnoreCase("true")) {
        sslc = (new SSLContextBuilder()).loadTrustMaterial((KeyStore)null, new TrustStrategy(this) {
              final HadoopClient this$0;
              
              public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                return true;
              }
            }).build();
      } else {
        sslc = SSLContexts.createDefault();
      } 
      hcb.setSSLSocketFactory(new SSLConnectionSocketFactory(sslc, System.getProperty("crushftp.tls_version_client", "SSLv2Hello,TLSv1,TLSv1.1,TLSv1.2").split(","), enabled_ciphers, new NoopHostnameVerifier()));
    } catch (Exception e) {
      Common.log("SERVER", 0, e);
    } 
    return hcb.build();
  }
  
  public HadoopClient(String logHeader, Vector logQueue) {
    super(logHeader, logQueue);
  }
  
  public HadoopClient(String url, String logHeader, Vector logQueue) {
    super(logHeader, logQueue);
    this.url = url;
    if (System.getProperty("crushftp.debug", "0").equals("2")) {
      if (Logger.getLogger("org.apache.http").getLevel() != Level.ALL) {
        BasicConfigurator.configure();
        Logger.getLogger("org.apache.http").setLevel(Level.ALL);
      } 
    } else if (Logger.getLogger("org.apache.http") != null && Logger.getLogger("org.apache.http").getLevel() != Level.OFF) {
      BasicConfigurator.resetConfiguration();
      Logger.getLogger("org.apache.http").setLevel(Level.OFF);
    } 
  }
  
  public String login2(String username, String password, String clientid) throws Exception {
    if (Logger.getLogger("org.apache.http") != null)
      if (System.getProperty("crushftp.debug", "0").equals("2")) {
        if (Logger.getLogger("org.apache.http").getLevel() != Level.ALL) {
          BasicConfigurator.configure();
          Logger.getLogger("org.apache.http").setLevel(Level.ALL);
        } 
      } else if (Logger.getLogger("org.apache.http").getLevel() != Level.OFF) {
        BasicConfigurator.resetConfiguration();
        Logger.getLogger("org.apache.http").setLevel(Level.OFF);
      }  
    password = VRL.vrlDecode(password);
    this.config.put("username", username);
    this.config.put("password", password);
    setJavaSecurityProperties(true, this.config.getProperty("login_config"), this.config.getProperty("kerberos_config"));
    this.loginContext = new LoginContext("KrbLogin", new KerberosCallBackHandler(this, this.config.getProperty("username"), this.config.getProperty("password")));
    this.loginContext.login();
    return "Success";
  }
  
  public void logout() throws Exception {
    close();
  }
  
  public Vector list(String path, Vector list) throws Exception {
    if (list == null)
      list = new Vector(); 
    Vector list2 = list;
    boolean inc = false;
    Exception last_e = null;
    Properties status = new Properties();
    for (int x = 0; x < 5; x++) {
      if (VRL.getActiveUrl(this.url, inc) == null)
        break; 
      PrivilegedAction sendAction = new PrivilegedAction(this, status, path, list2) {
          final HadoopClient this$0;
          
          private final Properties val$status;
          
          private final String val$path;
          
          private final Vector val$list2;
          
          public Object run() {
            try {
              this.val$status.put("obj", this.this$0.list4(this.val$path, this.val$list2));
              this.val$status.put("status", "DONE");
            } catch (Exception e) {
              this.val$status.put("error", e);
              this.val$status.put("status", "ERROR");
            } 
            return new Boolean(true);
          }
        };
      Subject.doAs(this.loginContext.getSubject(), sendAction);
      int i = 1;
      while (!status.containsKey("status"))
        Thread.sleep(((i++ < 100) ? i : 100L)); 
      status.remove("status");
      if (status.containsKey("error")) {
        last_e = (Exception)status.remove("error");
        inc = true;
        Thread.sleep(1000L);
      } else {
        last_e = null;
        break;
      } 
    } 
    if (last_e != null)
      throw last_e; 
    return (Vector)status.remove("obj");
  }
  
  private Vector list4(String path, Vector list) throws Exception {
    CloseableHttpClient httpclient = createHttpClient();
    String url_path = format_path_for_url(path);
    String url_str = String.valueOf(url_path) + "?op=LISTSTATUS&user.name=" + this.config.getProperty("username");
    log("List:" + url_str);
    CloseableHttpResponse response = httpclient.execute(new HttpGet((new URL(url_str)).toURI()));
    if (response.getStatusLine().getStatusCode() == 403)
      throw new Exception(response.getStatusLine()); 
    HttpEntity entity = response.getEntity();
    if (entity == null)
      throw new ClientProtocolException("Hadoop: Response contains no content:" + response); 
    String json = Common.consumeResponse(entity.getContent());
    if (response.getStatusLine().getStatusCode() >= 400) {
      log("Hadoop :Status Code :" + response.getStatusLine().getStatusCode() + json + "\r\n");
      httpclient.close();
      throw new Exception(response.getStatusLine());
    } 
    httpclient.close();
    Object obj = ((JSONObject)((JSONObject)JSONValue.parse(json)).get("FileStatuses")).get("FileStatus");
    if (obj instanceof JSONArray) {
      JSONArray ja = (JSONArray)obj;
      for (int x = 0; x < ja.size(); x++) {
        Object obj2 = ja.get(x);
        if (obj2 instanceof JSONObject) {
          Properties stat = parseFileStatus(path, obj2);
          list.addElement(stat);
        } 
      } 
    } 
    return list;
  }
  
  private Properties parseFileStatus(String path, Object obj) throws Exception {
    SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
    Properties item = new Properties();
    JSONObject jo = (JSONObject)obj;
    boolean folder = false;
    if (jo.get("type").equals("DIRECTORY"))
      folder = true; 
    Object[] a = jo.entrySet().toArray();
    for (int i = 0; i < a.length; i++) {
      String key2 = a[i].toString().split("=")[0];
      item.put(key2.trim(), jo.get(key2).trim());
    } 
    Date d = new Date(Long.parseLong(item.getProperty("modificationTime")));
    String line = String.valueOf(folder ? "d" : "-") + "rwxrwxrwx   1    owner   group   " + item.getProperty("length") + "   " + yyyyMMddHHmmss.format(d) + "   " + this.dd.format(d) + " " + this.yyyy.format(d) + " /" + item.getProperty("pathSuffix");
    Properties stat = parseStat(line);
    stat.put("resource_id", item.getProperty("fileId"));
    stat.put("url", String.valueOf(this.url) + path.substring(1) + stat.getProperty("name"));
    resourceIdCache.put(String.valueOf(path) + stat.getProperty("name"), item.getProperty("fileId"));
    if (stat.getProperty("type", "").equalsIgnoreCase("DIR"))
      resourceIdCache.put(String.valueOf(path) + stat.getProperty("name") + "/", item.getProperty("fileId")); 
    return stat;
  }
  
  private Properties getFileStatus(String path) throws Exception {
    CloseableHttpClient httpclient = createHttpClient();
    String url_path = format_path_for_url(path);
    String url_str = String.valueOf(url_path) + "?op=GETFILESTATUS&user.name=" + this.config.getProperty("username");
    log("List:" + url_str);
    CloseableHttpResponse response = httpclient.execute(new HttpGet((new URL(url_str)).toURI()));
    if (response.getStatusLine().getStatusCode() == 403)
      throw new Exception(response.getStatusLine()); 
    HttpEntity entity = response.getEntity();
    if (entity == null)
      throw new ClientProtocolException("Hadoop: Response contains no content:" + response); 
    String json = Common.consumeResponse(entity.getContent());
    if (response.getStatusLine().getStatusCode() >= 400) {
      log("Hadoop :Status Code :" + response.getStatusLine().getStatusCode() + json + "\r\n");
      httpclient.close();
      throw new Exception(response.getStatusLine());
    } 
    Properties stat = parseFileStatus(path, ((JSONObject)JSONValue.parse(json)).get("FileStatus"));
    if (stat.getProperty("name").equals("/"))
      stat.put("name", Common.last(path)); 
    httpclient.close();
    return stat;
  }
  
  protected OutputStream upload3(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    Properties status = new Properties();
    PrivilegedAction sendAction = new PrivilegedAction(this, status, path, startPos, truncate, binary) {
        final HadoopClient this$0;
        
        private final Properties val$status;
        
        private final String val$path;
        
        private final long val$startPos;
        
        private final boolean val$truncate;
        
        private final boolean val$binary;
        
        public Object run() {
          try {
            this.val$status.put("obj", this.this$0.upload4(this.val$path, this.val$startPos, this.val$truncate, this.val$binary));
            this.val$status.put("status", "DONE");
          } catch (Exception e) {
            this.val$status.put("error", e);
            this.val$status.put("status", "ERROR");
          } 
          return new Boolean(true);
        }
      };
    Subject.doAs(this.loginContext.getSubject(), sendAction);
    int i = 1;
    while (!status.containsKey("status"))
      Thread.sleep(((i++ < 100) ? i : 100L)); 
    if (status.containsKey("error"))
      throw (Exception)status.remove("error"); 
    this.out = (OutputStream)status.remove("obj");
    return this.out;
  }
  
  private OutputStream upload4(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    CloseableHttpClient httpclient = createHttpClient();
    CloseableHttpResponse response = null;
    boolean inc = false;
    Exception last_e = null;
    for (int x = 0; x < 5; x++) {
      if (VRL.getActiveUrl(this.url, inc) == null)
        break; 
      String url_str = String.valueOf(format_path_for_url(path)) + "?op=CREATE&permission=777&overwrite=true&user.name=" + this.config.getProperty("username");
      try {
        log("Upload:" + url_str);
        response = httpclient.execute(new HttpPut((new URL(url_str)).toURI()));
        last_e = null;
        break;
      } catch (Exception e) {
        last_e = e;
        inc = true;
        Thread.sleep(1000L);
      } 
    } 
    if (last_e != null)
      throw last_e; 
    HttpEntity entity = response.getEntity();
    if (entity == null)
      throw new ClientProtocolException("Hadoop: Response contains no content"); 
    String json = Common.consumeResponse(entity.getContent());
    String redirect_url = response.getFirstHeader("Location").getValue();
    if (response.getStatusLine().getStatusCode() >= 400) {
      log("Hadoop :Status Code :" + response.getStatusLine().getStatusCode() + json + "\r\n");
      httpclient.close();
      return null;
    } 
    httpclient.close();
    CloseableHttpClient httpclient_data = createHttpClient();
    log("Upload3:" + redirect_url);
    log("Upload4:" + (new URL(redirect_url)).toURI());
    HttpPut httpPutData = new HttpPut((new URL(redirect_url)).toURI());
    Properties socks = Common.getConnectedSocks(false);
    Socket sock1 = (Socket)socks.remove("sock1");
    Socket sock2 = (Socket)socks.remove("sock2");
    InputStreamEntity reqEntity = new InputStreamEntity(sock2.getInputStream(), -1L, ContentType.APPLICATION_OCTET_STREAM);
    reqEntity.setChunked(true);
    httpPutData.setEntity(reqEntity);
    Properties status = new Properties();
    Worker.startWorker(new Runnable(this, httpclient_data, httpPutData, status) {
          final HadoopClient this$0;
          
          private final CloseableHttpClient val$httpclient_data;
          
          private final HttpPut val$httpPutData;
          
          private final Properties val$status;
          
          public void run() {
            try {
              this.val$httpclient_data.execute(this.val$httpPutData);
              this.val$status.put("status", "DONE");
            } catch (Exception e) {
              this.this$0.log(e);
              this.val$status.put("e", e);
              this.val$status.put("status", "ERROR");
              if (e.getCause() != null)
                this.this$0.log("Caused by: " + e.getCause()); 
            } 
          }
        }Thread.currentThread() + ":HADOOP_PUT:" + reqEntity);
    return new null.OutputWrapper(this, sock1.getOutputStream(), status);
  }
  
  public boolean upload_0_byte(String path) throws Exception {
    return true;
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary) throws Exception {
    Properties status = new Properties();
    PrivilegedAction sendAction = new PrivilegedAction(this, status, path, startPos, endPos, binary) {
        final HadoopClient this$0;
        
        private final Properties val$status;
        
        private final String val$path;
        
        private final long val$startPos;
        
        private final long val$endPos;
        
        private final boolean val$binary;
        
        public Object run() {
          try {
            this.val$status.put("obj", this.this$0.download4(this.val$path, this.val$startPos, this.val$endPos, this.val$binary));
            this.val$status.put("status", "DONE");
          } catch (Exception e) {
            this.val$status.put("error", e);
            this.val$status.put("status", "ERROR");
          } 
          return new Boolean(true);
        }
      };
    Subject.doAs(this.loginContext.getSubject(), sendAction);
    int i = 1;
    while (!status.containsKey("status"))
      Thread.sleep(((i++ < 100) ? i : 100L)); 
    if (status.containsKey("error"))
      throw (Exception)status.remove("error"); 
    this.in = (InputStream)status.remove("obj");
    return this.in;
  }
  
  private InputStream download4(String path, long startPos, long endPos, boolean binary) throws Exception {
    CloseableHttpClient httpclient = createHttpClient();
    CloseableHttpResponse response = null;
    boolean inc = false;
    Exception last_e = null;
    for (int x = 0; x < 5; x++) {
      if (VRL.getActiveUrl(this.url, inc) == null)
        break; 
      String url_path = format_path_for_url(path);
      String url_str = String.valueOf(url_path) + "?op=OPEN&buffersize=32768&user.name=" + this.config.getProperty("username");
      try {
        log("Download:" + url_str);
        response = httpclient.execute(new HttpGet((new URL(url_str)).toURI()));
        last_e = null;
        break;
      } catch (Exception e) {
        last_e = e;
        inc = true;
        Thread.sleep(1000L);
      } 
    } 
    if (last_e != null)
      throw last_e; 
    HttpEntity entity = response.getEntity();
    if (entity == null)
      throw new ClientProtocolException("Hadoop: Response contains no content"); 
    if (response.getStatusLine().getStatusCode() >= 400) {
      String json = Common.consumeResponse(entity.getContent());
      log("Hadoop :SatusCode: " + response.getStatusLine().getStatusCode() + json + "\r\n");
      httpclient.close();
      return null;
    } 
    return entity.getContent();
  }
  
  public boolean makedir(String path) throws Exception {
    Properties status = new Properties();
    PrivilegedAction sendAction = new PrivilegedAction(this, status, path) {
        final HadoopClient this$0;
        
        private final Properties val$status;
        
        private final String val$path;
        
        public Object run() {
          try {
            this.val$status.put("obj", (new StringBuffer(String.valueOf(this.this$0.makedir4(this.val$path)))).toString());
            this.val$status.put("status", "DONE");
          } catch (Exception e) {
            this.val$status.put("error", e);
            this.val$status.put("status", "ERROR");
          } 
          return new Boolean(true);
        }
      };
    Subject.doAs(this.loginContext.getSubject(), sendAction);
    int i = 1;
    while (!status.containsKey("status"))
      Thread.sleep(((i++ < 100) ? i : 100L)); 
    if (status.containsKey("error"))
      throw (Exception)status.remove("error"); 
    return status.getProperty("obj").equals("true");
  }
  
  private boolean makedir4(String path) throws Exception {
    CloseableHttpClient httpclient = createHttpClient();
    CloseableHttpResponse response = null;
    boolean inc = false;
    Exception last_e = null;
    for (int x = 0; x < 5; x++) {
      if (VRL.getActiveUrl(this.url, inc) == null)
        break; 
      String url_path = format_path_for_url(path);
      String url_str = String.valueOf(url_path) + "?op=MKDIRS&permission=777&user.name=" + this.config.getProperty("username");
      try {
        log("MakeDir:" + url_str);
        response = httpclient.execute(new HttpPut((new URL(url_str)).toURI()));
        last_e = null;
        break;
      } catch (Exception e) {
        last_e = e;
        inc = true;
        Thread.sleep(1000L);
      } 
    } 
    if (last_e != null)
      throw last_e; 
    HttpEntity entity = response.getEntity();
    if (entity == null)
      throw new ClientProtocolException("Hadoop: Response contains no content"); 
    String json = Common.consumeResponse(entity.getContent());
    httpclient.close();
    Boolean result = (Boolean)((JSONObject)JSONValue.parse(json)).get("boolean");
    return result.booleanValue();
  }
  
  public boolean makedirs(String path) throws Exception {
    Properties status = new Properties();
    PrivilegedAction sendAction = new PrivilegedAction(this, status, path) {
        final HadoopClient this$0;
        
        private final Properties val$status;
        
        private final String val$path;
        
        public Object run() {
          try {
            this.val$status.put("obj", (new StringBuffer(String.valueOf(this.this$0.makedirs4(this.val$path)))).toString());
            this.val$status.put("status", "DONE");
          } catch (Exception e) {
            this.val$status.put("error", e);
            this.val$status.put("status", "ERROR");
          } 
          return new Boolean(true);
        }
      };
    Subject.doAs(this.loginContext.getSubject(), sendAction);
    int i = 1;
    while (!status.containsKey("status"))
      Thread.sleep(((i++ < 100) ? i : 100L)); 
    if (status.containsKey("error"))
      throw (Exception)status.remove("error"); 
    return status.getProperty("obj").equals("true");
  }
  
  private boolean makedirs4(String path) throws Exception {
    boolean ok = true;
    String[] parts = path.split("/");
    String path2 = "";
    for (int x = 0; x < parts.length && ok; x++) {
      path2 = String.valueOf(path2) + parts[x] + "/";
      if (x >= 1) {
        String resourceId = resourceIdCache.getProperty(path2);
        if (resourceId == null)
          ok = makedir(path2); 
      } 
    } 
    return ok;
  }
  
  public boolean delete(String path) throws Exception {
    Properties status = new Properties();
    PrivilegedAction sendAction = new PrivilegedAction(this, status, path) {
        final HadoopClient this$0;
        
        private final Properties val$status;
        
        private final String val$path;
        
        public Object run() {
          try {
            this.val$status.put("obj", (new StringBuffer(String.valueOf(this.this$0.delete4(this.val$path)))).toString());
            this.val$status.put("status", "DONE");
          } catch (Exception e) {
            this.val$status.put("error", e);
            this.val$status.put("status", "ERROR");
          } 
          return new Boolean(true);
        }
      };
    Subject.doAs(this.loginContext.getSubject(), sendAction);
    int i = 1;
    while (!status.containsKey("status"))
      Thread.sleep(((i++ < 100) ? i : 100L)); 
    if (status.containsKey("error"))
      throw (Exception)status.remove("error"); 
    return status.getProperty("obj").equals("true");
  }
  
  private boolean delete4(String path) throws Exception {
    CloseableHttpClient httpclient = createHttpClient();
    CloseableHttpResponse response = null;
    boolean inc = false;
    Exception last_e = null;
    for (int x = 0; x < 5; x++) {
      if (VRL.getActiveUrl(this.url, inc) == null)
        break; 
      String url_path = format_path_for_url(path);
      String url_str = String.valueOf(url_path) + "?op=DELETE&recursive=true&user.name=" + this.config.getProperty("username");
      try {
        log("Delete:" + url_str);
        response = httpclient.execute(new HttpDelete((new URL(url_str)).toURI()));
        last_e = null;
        break;
      } catch (Exception e) {
        last_e = e;
        inc = true;
        Thread.sleep(1000L);
      } 
    } 
    if (last_e != null)
      throw last_e; 
    HttpEntity entity = response.getEntity();
    if (entity == null)
      throw new ClientProtocolException("Hadoop: Response contains no content"); 
    String json = Common.consumeResponse(entity.getContent());
    httpclient.close();
    Boolean result = (Boolean)((JSONObject)JSONValue.parse(json)).get("boolean");
    return result.booleanValue();
  }
  
  public boolean rename(String rnfr, String rnto) throws Exception {
    Properties status = new Properties();
    PrivilegedAction sendAction = new PrivilegedAction(this, status, rnfr, rnto) {
        final HadoopClient this$0;
        
        private final Properties val$status;
        
        private final String val$rnfr;
        
        private final String val$rnto;
        
        public Object run() {
          try {
            this.val$status.put("obj", (new StringBuffer(String.valueOf(this.this$0.rename4(this.val$rnfr, this.val$rnto)))).toString());
            this.val$status.put("status", "DONE");
          } catch (Exception e) {
            this.val$status.put("error", e);
            this.val$status.put("status", "ERROR");
          } 
          return new Boolean(true);
        }
      };
    Subject.doAs(this.loginContext.getSubject(), sendAction);
    int i = 1;
    while (!status.containsKey("status"))
      Thread.sleep(((i++ < 100) ? i : 100L)); 
    if (status.containsKey("error"))
      throw (Exception)status.remove("error"); 
    return status.getProperty("obj").equals("true");
  }
  
  private boolean rename4(String rnfr, String rnto) throws Exception {
    CloseableHttpClient httpclient = createHttpClient();
    CloseableHttpResponse response = null;
    boolean inc = false;
    Exception last_e = null;
    for (int x = 0; x < 5; x++) {
      if (VRL.getActiveUrl(this.url, inc) == null)
        break; 
      String url_path = format_path_for_url(rnfr);
      String url_str = String.valueOf(url_path) + "?op=RENAME&user.name=" + this.config.getProperty("username") + "&destination=" + Common.url_encode(rnto);
      try {
        log("Rename:" + url_str);
        response = httpclient.execute(new HttpPut((new URL(url_str)).toURI()));
        last_e = null;
        break;
      } catch (Exception e) {
        last_e = e;
        inc = true;
        Thread.sleep(1000L);
      } 
    } 
    if (last_e != null)
      throw last_e; 
    HttpEntity entity = response.getEntity();
    if (entity == null)
      throw new ClientProtocolException("Hadoop: Response contains no content"); 
    String json = Common.consumeResponse(entity.getContent());
    Boolean result = (Boolean)((JSONObject)JSONValue.parse(json)).get("boolean");
    return result.booleanValue();
  }
  
  public boolean mdtm(String path, long modified) throws Exception {
    Properties status = new Properties();
    PrivilegedAction sendAction = new PrivilegedAction(this, status, path, modified) {
        final HadoopClient this$0;
        
        private final Properties val$status;
        
        private final String val$path;
        
        private final long val$modified;
        
        public Object run() {
          try {
            this.val$status.put("obj", (new StringBuffer(String.valueOf(this.this$0.mdtm4(this.val$path, this.val$modified)))).toString());
            this.val$status.put("status", "DONE");
          } catch (Exception e) {
            this.val$status.put("error", e);
            this.val$status.put("status", "ERROR");
          } 
          return new Boolean(true);
        }
      };
    Subject.doAs(this.loginContext.getSubject(), sendAction);
    int i = 1;
    while (!status.containsKey("status"))
      Thread.sleep(((i++ < 100) ? i : 100L)); 
    if (status.containsKey("error"))
      throw (Exception)status.remove("error"); 
    return status.getProperty("obj").equals("true");
  }
  
  private boolean mdtm4(String path, long modified) throws Exception {
    CloseableHttpClient httpclient = createHttpClient();
    CloseableHttpResponse response = null;
    boolean inc = false;
    Exception last_e = null;
    for (int x = 0; x < 5; x++) {
      if (VRL.getActiveUrl(this.url, inc) == null)
        break; 
      String url_path = format_path_for_url(path);
      String url_str = String.valueOf(url_path) + "?op=SETTIMES" + "&user.name=" + this.config.getProperty("username") + "&modificationtime=" + modified + "&accesstime=" + modified;
      try {
        log("MDTM:" + url_str);
        response = httpclient.execute(new HttpPut((new URL(url_str)).toURI()));
        last_e = null;
        break;
      } catch (Exception e) {
        last_e = e;
        inc = true;
        Thread.sleep(1000L);
      } 
    } 
    if (last_e != null)
      throw last_e; 
    HttpEntity entity = response.getEntity();
    if (entity == null)
      throw new ClientProtocolException("Hadoop: Response contains no content"); 
    String json = Common.consumeResponse(entity.getContent());
    if (response.getStatusLine().getStatusCode() >= 400) {
      log("Hadoop :Status Code :" + response.getStatusLine().getStatusCode() + json + "\r\n");
      httpclient.close();
      return false;
    } 
    httpclient.close();
    return true;
  }
  
  public Properties stat(String path) throws Exception {
    Properties status = new Properties();
    PrivilegedAction sendAction = new PrivilegedAction(this, path, status) {
        final HadoopClient this$0;
        
        private final String val$path;
        
        private final Properties val$status;
        
        public Object run() {
          try {
            Properties obj = this.this$0.stat4(this.val$path);
            if (obj != null)
              this.val$status.put("obj", obj); 
            this.val$status.put("status", "DONE");
          } catch (Exception e) {
            this.val$status.put("error", e);
            this.val$status.put("status", "ERROR");
          } 
          return new Boolean(true);
        }
      };
    Subject.doAs(this.loginContext.getSubject(), sendAction);
    int x = 1;
    while (!status.containsKey("status")) {
      Thread.sleep(x++);
      if (x > 100)
        x = 100; 
    } 
    if (status.containsKey("error"))
      throw (Exception)status.remove("error"); 
    return (Properties)status.get("obj");
  }
  
  public Properties stat4(String path) throws Exception {
    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1); 
    Properties p = null;
    try {
      p = getFileStatus(path);
    } catch (Exception e) {
      return null;
    } 
    return p;
  }
  
  private String format_path_for_url(String path) {
    if (path.endsWith("/") && !path.equals("/"))
      path = path.substring(0, path.length() - 1); 
    VRL vrl = new VRL(VRL.getActiveUrl(this.url, false));
    String[] path_names = path.split("/");
    String encoded_path = "";
    if (path_names.length > 0) {
      for (int x = 0; x < path_names.length; x++) {
        if (!path_names[x].equals(""))
          encoded_path = String.valueOf(encoded_path) + "/" + Common.url_encode(path_names[x]); 
      } 
    } else {
      encoded_path = path;
    } 
    String url_tmp = "https://" + vrl.getHost() + ":" + vrl.getPort() + "/webhdfs/v1" + encoded_path;
    return url_tmp;
  }
  
  private void setJavaSecurityProperties(boolean debug, String loginConf, String krb5Conf) {
    System.setProperty("java.security.auth.login.config", loginConf);
    System.setProperty("java.security.krb5.conf", krb5Conf);
    System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
    if (debug)
      System.setProperty("sun.security.krb5.debug", "true"); 
  }
  
  private Registry buildSPNEGOAuthSchemeRegistry() {
    return RegistryBuilder.<SPNegoSchemeFactory>create().register("Negotiate", new SPNegoSchemeFactory(true)).build();
  }
  
  private CredentialsProvider setupCredentialsProvider() {
    Credentials use_jaas_creds = new Credentials(this) {
        final HadoopClient this$0;
        
        public String getPassword() {
          return null;
        }
        
        public Principal getUserPrincipal() {
          return null;
        }
      };
    CredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, -1, AuthScope.ANY_REALM), use_jaas_creds);
    return credsProvider;
  }
  
  private class KerberosCallBackHandler implements CallbackHandler {
    private final String user;
    
    private final String password;
    
    final HadoopClient this$0;
    
    public KerberosCallBackHandler(HadoopClient this$0, String user, String password) {
      this.this$0 = this$0;
      this.user = user;
      this.password = password;
    }
    
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
      for (int x = 0; x < callbacks.length; x++) {
        Callback callback = callbacks[x];
        if (callback instanceof NameCallback) {
          NameCallback nc = (NameCallback)callback;
          nc.setName(this.user);
        } else if (callback instanceof PasswordCallback) {
          PasswordCallback pc = (PasswordCallback)callback;
          pc.setPassword(this.password.toCharArray());
        } else {
          throw new UnsupportedCallbackException(callback, "Unknown Callback");
        } 
      } 
    }
  }
}
