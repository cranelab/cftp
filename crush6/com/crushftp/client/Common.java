package com.crushftp.client;

import com.crushftp.tunnel2.Tunnel2;
import com.didisoft.pgp.PGPKeyPair;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.net.ServerSocketFactory;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.script.ScriptEngineManager;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class Common {
  static boolean providerAdded = false;
  
  public static final String pgpChunkedheaderStr = "CRUSHFTP_PGPChunkedStream:dBa3Em7W4N:";
  
  public static final String encryptedNote = "CRUSHFTP_ENCRYPTED_kHBeMxiWj7Sb4PdqJ8";
  
  public static final String encryptedDefaultSize = "0                                        ";
  
  public static Properties socketPool = new Properties();
  
  public static int socketTimeout = 20000;
  
  public static Vector log = null;
  
  public static Properties System2 = new Properties();
  
  static SecureRandom rn = new SecureRandom();
  
  static Properties bad_tls_protocols = new Properties();
  
  static long UID_GLOBAL = System.currentTimeMillis();
  
  static {
    rn.nextBytes((String.valueOf(Runtime.getRuntime().maxMemory()) + Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory()).getBytes());
  }
  
  public static synchronized long uidg() {
    while (System.currentTimeMillis() == UID_GLOBAL) {
      try {
        Thread.sleep(100L);
      } catch (Exception exception) {}
    } 
    UID_GLOBAL = System.currentTimeMillis();
    return UID_GLOBAL;
  }
  
  public static boolean machine_is_mac() {
    try {
      if (System.getProperties().getProperty("os.name").toUpperCase().equals("MAC OS"))
        return true; 
      return false;
    } catch (Exception exception) {
      return false;
    } 
  }
  
  public static boolean machine_is_x() {
    try {
      if (System.getProperties().getProperty("os.name").toUpperCase().equals("MAC OS X"))
        return true; 
      return false;
    } catch (Exception exception) {
      return false;
    } 
  }
  
  public static boolean machine_is_linux() {
    try {
      if (System.getProperties().getProperty("os.name").toUpperCase().indexOf("LINUX") >= 0)
        return true; 
      if (System.getProperties().getProperty("os.name", "").toUpperCase().indexOf("HP-UX") >= 0)
        return true; 
      return false;
    } catch (Exception exception) {
      return false;
    } 
  }
  
  public static boolean machine_is_unix() {
    try {
      if (System.getProperties().getProperty("os.name").toUpperCase().indexOf("UNIX") >= 0)
        return true; 
      return false;
    } catch (Exception exception) {
      return false;
    } 
  }
  
  public static boolean machine_is_windows() {
    try {
      if (System.getProperties().getProperty("os.name").toUpperCase().indexOf("NDOWS") >= 0)
        return true; 
      return false;
    } catch (Exception exception) {
      return false;
    } 
  }
  
  public static boolean machine_is_windows_xp() {
    try {
      if (machine_is_windows() && System.getProperties().getProperty("os.name").toUpperCase().indexOf("XP") >= 0)
        return true; 
      return false;
    } catch (Exception exception) {
      return false;
    } 
  }
  
  public static boolean machine_is_x_10_6_plus() {
    if (machine_is_x()) {
      String[] version = System.getProperties().getProperty("os.version", "").split("\\.");
      if (Integer.parseInt(version[0]) >= 10 && Integer.parseInt(version[1]) >= 6)
        return true; 
      if (Integer.parseInt(version[0]) > 10)
        return true; 
    } 
    return false;
  }
  
  public static boolean showUrl(URL url) {
    boolean result = false;
    if (!result)
      result = openURL(url.toExternalForm()); 
    return result;
  }
  
  public static boolean openURL(String url) {
    try {
      if (machine_is_x()) {
        Class fileMgr = Class.forName("com.apple.eio.FileManager");
        if (class$0 == null)
          try {
          
          } catch (ClassNotFoundException classNotFoundException) {
            throw new NoClassDefFoundError(null.getMessage());
          }  
        false[class$0] = class$0 = Class.forName("java.lang.String");
        Method openURL = "openURL".getDeclaredMethod((String)new Class[1], new Class[1]);
        openURL.invoke(null, new Object[] { url });
      } else if (machine_is_windows()) {
        Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
      } else {
        String[] browsers = { "firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape" };
        String browser = null;
        for (int count = 0; count < browsers.length && browser == null; count++) {
          if (Runtime.getRuntime().exec(new String[] { "which", browsers[count] }).waitFor() == 0)
            browser = browsers[count]; 
          if (browser == null)
            throw new Exception("Could not find web browser"); 
          Runtime.getRuntime().exec(new String[] { browser, url });
        } 
      } 
      return true;
    } catch (Exception e) {
      JOptionPane.showMessageDialog(null, "Error attempting to launch web browser:\n" + e.getLocalizedMessage());
      return false;
    } 
  }
  
  public static boolean isSymbolicLink(String link_name) {
    if (machine_is_windows())
      return false; 
    try {
      File f = new File(link_name);
      if (!f.getAbsolutePath().equals(f.getCanonicalPath()))
        return true; 
    } catch (IOException iOException) {}
    return false;
  }
  
  public static boolean log(String tag, int level, String s) {
    if (s.indexOf("url=") >= 0) {
      String url = s.substring(s.indexOf("url=") + 4).trim();
      if (url.indexOf(",") >= 0)
        url = url.substring(0, url.indexOf(",")).trim(); 
      VRL vrl = new VRL(url);
      String url2 = String.valueOf(vrl.getProtocol()) + "://" + vrl.getUsername() + ":********" + "@" + vrl.getHost() + ":" + vrl.getPort() + vrl.getPath();
      s = replace_str(s, url, url2);
    } else if (s.indexOf("://") >= 0 && s.indexOf("@", s.indexOf("://")) >= 0) {
      int colslashslash = s.indexOf("://");
      int at = s.indexOf("@", colslashslash);
      String inner = s.substring(colslashslash, at);
      if (inner.indexOf(":") >= 0)
        s = replace_str(s, inner, String.valueOf(inner.substring(0, inner.indexOf(":") + 1)) + "************"); 
    } 
    if (s.indexOf("password=") >= 0) {
      String url = s.substring(s.indexOf("password=") + 4).trim();
      if (url.indexOf(",") >= 0)
        url = url.substring(0, url.indexOf(",")).trim(); 
      String url2 = "password=***********";
      s = replace_str(s, url, url2);
    } 
    if (log != null) {
      if (Integer.parseInt(System.getProperty("crushftp.debug", "1")) >= level) {
        Properties p = new Properties();
        p.put("tag", tag);
        p.put("level", (new StringBuffer(String.valueOf(level))).toString());
        p.put("data", s.trim());
        log.addElement(p);
        return true;
      } 
    } else if (Integer.parseInt(System.getProperty("crushtunnel.debug", "0")) >= level) {
      if (s.trim().length() > 0)
        System.out.println(s.trim()); 
      return true;
    } 
    return false;
  }
  
  public static boolean log(String tag, int level, Throwable e) {
    if (Integer.parseInt(System.getProperty("crushtunnel.debug", "0")) >= level) {
      log(tag, level, Thread.currentThread().getName());
      log(tag, level, e.toString());
      StackTraceElement[] ste = e.getStackTrace();
      for (int x = 0; x < ste.length; x++)
        log(tag, level, String.valueOf(ste[x].getClassName()) + "." + ste[x].getMethodName() + ":" + ste[x].getLineNumber()); 
      e.printStackTrace();
      return true;
    } 
    return false;
  }
  
  public static boolean log(String tag, int level, Exception e) {
    return log(tag, level, e);
  }
  
  public static String replace_str(String master_string, String search_data, String replace_data) {
    if (search_data.equals(replace_data) || search_data.equals(""))
      return master_string; 
    int start_loc = 0;
    int end_loc = 0;
    try {
      start_loc = master_string.indexOf(search_data);
      while (start_loc >= 0) {
        end_loc = start_loc + search_data.length();
        master_string = String.valueOf(master_string.substring(0, start_loc)) + replace_data + master_string.substring(end_loc);
        start_loc = master_string.indexOf(search_data, start_loc + replace_data.length());
      } 
    } catch (Exception exception) {}
    return master_string;
  }
  
  public static String normalize2(String s) {
    if (System.getProperty("java.version").startsWith("1.4") || (machine_is_x() && !machine_is_x_10_6_plus()))
      return s; 
    try {
      return Normalizer.normalize(s, Normalizer.Form.NFD);
    } catch (NoClassDefFoundError e) {
      return s;
    } 
  }
  
  public static String url_decode3(String master_string) {
    master_string = replace_str(master_string, "%%", "þ");
    int start_loc = 0;
    try {
      start_loc = master_string.indexOf("%");
      while (start_loc >= 0) {
        String tester = master_string.substring(start_loc + 1, start_loc + 3);
        int val = tester.charAt(0) - 48;
        if (val > 9)
          val -= 7; 
        int val2 = tester.charAt(1) - 48;
        if (val2 > 9)
          val2 -= 7; 
        val = val * 16 + val2;
        master_string = replace_str(master_string, "%" + tester, (char)val);
        start_loc = master_string.indexOf("%", start_loc + 1);
      } 
    } catch (Exception exception) {}
    master_string = replace_str(master_string, "þ", "%");
    return master_string;
  }
  
  public static String url_decode(String s) {
    try {
      if (s.indexOf("% ") < 0 && !s.endsWith("%")) {
        String s2 = s.replace('+', 'þ');
        s2 = URLDecoder.decode(s2, "UTF8");
        s = s2.replace('þ', '+');
      } 
    } catch (Exception e) {
      log("SERVER", 2, e);
    } 
    for (int x = 0; s != null && x < 32; x++) {
      if (x < 9 || x > 13)
        s = s.replace((char)x, '_'); 
    } 
    return s;
  }
  
  public static String url_encode_all(String master_string) {
    String return_str = "";
    for (int x = 0; x < master_string.length(); x++) {
      String temp = Long.toHexString(master_string.charAt(x));
      temp = temp.toUpperCase();
      if (temp.length() == 1)
        temp = "0" + temp; 
      return_str = String.valueOf(return_str) + "%" + temp;
    } 
    return return_str;
  }
  
  public static String url_encode(String master_string) {
    return url_encode(master_string, "");
  }
  
  public static String url_encode(String master_string, String OK_chars) {
    String return_str = "";
    try {
      master_string = replace_str(master_string, "+", "_-_THIS_-_IS_-_A_-_PLUS_-_");
      return_str = URLEncoder.encode(master_string, "utf-8");
      for (int x = 0; x < OK_chars.length(); x++) {
        String s = URLEncoder.encode((new StringBuffer(String.valueOf(OK_chars.charAt(x)))).toString(), "utf-8");
        return_str = replace_str(return_str, s, (new StringBuffer(String.valueOf(OK_chars.charAt(x)))).toString());
      } 
      return_str = replace_str(return_str, "+", "%20");
      return_str = replace_str(return_str, "_-_THIS_-_IS_-_A_-_PLUS_-_", "+");
      return return_str;
    } catch (Exception e) {
      log("SERVER", 2, e);
      return_str = "";
      for (int x = 0; x < master_string.length(); x++) {
        String temp = Long.toHexString(master_string.charAt(x));
        long val = master_string.charAt(x);
        if ((val >= 48L && val <= 57L) || (val >= 65L && val <= 90L) || (val >= 97L && val <= 122L) || val == 46L || val == 95L || OK_chars.indexOf((new StringBuffer(String.valueOf(master_string.charAt(x)))).toString()) >= 0) {
          return_str = String.valueOf(return_str) + master_string.charAt(x);
        } else {
          temp = temp.toUpperCase();
          if (temp.length() == 1)
            temp = "0" + temp; 
          return_str = String.valueOf(return_str) + "%" + temp;
        } 
      } 
      return return_str;
    } 
  }
  
  public ServerSocket getServerSocket(ServerSocketFactory ssf, int serverPort, String listen_ip, String disabled_ciphers, boolean needClientAuth) throws Exception {
    SSLServerSocket serverSocket = null;
    try {
      if (listen_ip != null)
        serverSocket = (SSLServerSocket)ssf.createServerSocket(serverPort, 1000, InetAddress.getByName(listen_ip)); 
    } catch (SocketException e) {
      log("SERVER", 2, e);
    } 
    if (serverSocket == null)
      serverSocket = (SSLServerSocket)ssf.createServerSocket(serverPort, 1000); 
    setEnabledCiphers(disabled_ciphers, null, serverSocket);
    serverSocket.setNeedClientAuth(needClientAuth);
    return serverSocket;
  }
  
  public SSLContext getSSLContext(String KEYSTORE, String TRUSTSTORE, String keystorepass, String keypass, String secureType, boolean needClientAuth, boolean acceptAnyCert) throws Exception {
    if (TRUSTSTORE == null)
      TRUSTSTORE = KEYSTORE; 
    String className = System.getProperty("crushftp.sslprovider", "");
    try {
      if (!providerAdded && !className.equals("")) {
        log("SERVER", 0, "Adding SSL provider:" + className);
        Provider provider = (Provider)Thread.currentThread().getContextClassLoader().loadClass(className).newInstance();
        Security.addProvider(provider);
        providerAdded = true;
      } 
    } catch (Exception e) {
      throw new Exception("Failed loading security provider " + className, e);
    } 
    KeyStore keystore = null;
    KeyStore truststore = null;
    String keystoreFormat = "JKS";
    if (KEYSTORE.toUpperCase().endsWith("PKCS12") || KEYSTORE.toUpperCase().endsWith("P12") || KEYSTORE.toUpperCase().endsWith("PFX"))
      keystoreFormat = "pkcs12"; 
    if (keystore == null) {
      keystore = KeyStore.getInstance(keystoreFormat);
      if (KEYSTORE.equals("builtin")) {
        keystore.load(getClass().getResource("/assets/builtin").openStream(), "crushftp".toCharArray());
      } else if (System2.containsKey("crushftp.keystores." + KEYSTORE.toUpperCase().replace('\\', '/'))) {
        Properties p = (Properties)System2.get("crushftp.keystores." + KEYSTORE.toUpperCase().replace('\\', '/'));
        keystore.load(new ByteArrayInputStream((byte[])p.get("bytes")), p.getProperty("keystorepass", keystorepass).toCharArray());
      } else {
        InputStream in = null;
        try {
          if (!(new File(KEYSTORE)).exists()) {
            log("SERVER", 0, "Couldn't find keystore " + KEYSTORE + ", ignoring it.");
            keystore.load(getClass().getResource("/assets/builtin").openStream(), "crushftp".toCharArray());
            KEYSTORE = "builtin";
          } else {
            in = new FileInputStream(KEYSTORE);
            keystore.load(in, keystorepass.toCharArray());
          } 
        } finally {
          if (in != null)
            in.close(); 
        } 
      } 
      truststore = KeyStore.getInstance(keystoreFormat);
      if (KEYSTORE.equals("builtin")) {
        truststore.load(getClass().getResource("/assets/builtin").openStream(), "crushftp".toCharArray());
      } else if (needClientAuth) {
        if (System2.containsKey("crushftp.keystores." + TRUSTSTORE.toUpperCase().replace('\\', '/'))) {
          Properties p = (Properties)System2.get("crushftp.keystores." + TRUSTSTORE.toUpperCase().replace('\\', '/'));
          truststore.load(new ByteArrayInputStream((byte[])p.get("bytes")), p.getProperty("keystorepass", keystorepass).toCharArray());
        } else {
          InputStream in = null;
          try {
            if (!(new File(TRUSTSTORE)).exists()) {
              log("SERVER", 0, "Couldn't find truststore " + TRUSTSTORE + ", ignoring it.");
              truststore.load(getClass().getResource("/assets/builtin").openStream(), "crushftp".toCharArray());
              TRUSTSTORE = "builtin";
            } else {
              in = new FileInputStream(TRUSTSTORE);
              truststore.load(in, keystorepass.toCharArray());
            } 
          } finally {
            if (in != null)
              in.close(); 
          } 
        } 
      } 
    } 
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    if (KEYSTORE.equals("builtin")) {
      kmf.init(keystore, "crushftp".toCharArray());
      if (needClientAuth)
        kmf.init(truststore, "crushftp".toCharArray()); 
    } else {
      try {
        kmf.init(keystore, keypass.toCharArray());
      } catch (Exception e) {
        kmf.init(keystore, keystorepass.toCharArray());
      } 
    } 
    TrustManager[] trustAllCerts = { new X509TrustManager(this) {
          final Common this$0;
          
          public X509Certificate[] getAcceptedIssuers() {
            return null;
          }
          
          public void checkClientTrusted(X509Certificate[] certs, String authType) {}
          
          public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        } };
    SSLContext sslc = SSLContext.getInstance(secureType);
    if (needClientAuth) {
      if (acceptAnyCert) {
        sslc.init(kmf.getKeyManagers(), trustAllCerts, new SecureRandom());
      } else {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(truststore);
        sslc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
      } 
    } else if (acceptAnyCert) {
      sslc.init(kmf.getKeyManagers(), trustAllCerts, new SecureRandom());
    } else {
      TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmf.init(keystore);
      sslc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
    } 
    return sslc;
  }
  
  public static void setEnabledCiphers(String disabled_ciphers, SSLSocket sock, SSLServerSocket serverSock) {
    if (!disabled_ciphers.equals("")) {
      Vector enabled_ciphers = new Vector();
      String[] ciphers = (String[])null;
      if (sock != null)
        ciphers = sock.getSupportedCipherSuites(); 
      if (serverSock != null)
        ciphers = serverSock.getSupportedCipherSuites(); 
      int x;
      for (x = 0; x < ciphers.length; x++) {
        if (disabled_ciphers.indexOf("(" + ciphers[x] + ")") < 0)
          enabled_ciphers.addElement(ciphers[x]); 
      } 
      ciphers = new String[enabled_ciphers.size()];
      for (x = 0; x < enabled_ciphers.size(); x++)
        ciphers[x] = enabled_ciphers.elementAt(x).toString(); 
      if (sock != null)
        sock.setEnabledCipherSuites(ciphers); 
      if (serverSock != null)
        serverSock.setEnabledCipherSuites(ciphers); 
    } 
  }
  
  public static String winPath(String path) {
    return path.replace('\\', '/');
  }
  
  public static String exec(String[] c) throws Exception {
    Process proc = Runtime.getRuntime().exec(c);
    BufferedReader br1 = null;
    try {
      br1 = new BufferedReader(new InputStreamReader(proc.getInputStream(), "UTF8"));
      String data = "";
      String result1 = "";
      while ((data = br1.readLine()) != null)
        result1 = String.valueOf(result1) + data + "\r\n"; 
      br1.close();
      proc.waitFor();
      return result1;
    } finally {
      br1.close();
    } 
  }
  
  public static String[] getCommandAction(String type, Properties p) {
    String[] s = (String[])null;
    if (type.equals("open_afp")) {
      s = new String[2];
      s[0] = "open";
      s[1] = "afp://127.0.0.1:" + p.getProperty("localPort") + "/";
    } else if (type.equals("open_smb")) {
      s = new String[2];
      s[0] = "open";
      s[1] = "smb://0.0.0.0:" + p.getProperty("localPort") + "/";
    } else if (type.equals("open_vnc")) {
      s = new String[2];
      s[0] = "open";
      s[1] = "vnc://127.0.0.1:" + p.getProperty("localPort") + "/";
    } 
    return s;
  }
  
  public static String makeBoundary(int len) {
    String chars = "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    String rand = "";
    for (int i = 0; i < len; i++)
      rand = String.valueOf(rand) + chars.charAt(rn.nextInt(chars.length())); 
    return rand;
  }
  
  public static String dots(String s) {
    s = s.replace('\\', '/');
    String s2 = "";
    while (s.indexOf("%") >= 0 && !s.equals(s2)) {
      s2 = s;
      s = url_decode(s);
      s = s.replace('\\', '/');
    } 
    if (s.startsWith("../"))
      s = s.substring(2); 
    while (s.indexOf("/./") >= 0) {
      String t = s.substring(0, s.indexOf("/./"));
      t = String.valueOf(t) + s.substring(s.indexOf("/./") + 2);
      s = t;
    } 
    while (s.indexOf("/../") >= 0) {
      String t = s.substring(0, s.indexOf("/../"));
      t = all_but_last(t);
      t = String.valueOf(t) + s.substring(s.indexOf("/../") + 4);
      s = t;
    } 
    if (s.endsWith("/."))
      s = s.substring(0, s.length() - 1); 
    if (s.startsWith("../"))
      s = s.substring(2); 
    while (s.indexOf("//") >= 0)
      s = replace_str(s, "//", "/"); 
    return s;
  }
  
  public static String all_but_last(String item) {
    item = item.replace('\\', '/');
    String master = item;
    item = item.substring(0, item.lastIndexOf("/", item.length() - 2) + 1);
    if (item.equals(""))
      item = master.substring(0, master.lastIndexOf("\\", master.length() - 2) + 1); 
    return item;
  }
  
  public static String last(String item) {
    item = item.replace('\\', '/');
    item = item.substring(item.lastIndexOf("/", item.length() - 2) + 1);
    return item;
  }
  
  static TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
          return null;
        }
        
        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
        
        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
      } };
  
  public static Socket getSSLSocket(String host, int port, boolean acceptAnyCert) throws Exception {
    SSLContext sc = getSSLContext();
    if (acceptAnyCert)
      sc.init(null, trustAllCerts, new SecureRandom()); 
    Socket sock = new Socket(host, port);
    sock = sc.getSocketFactory().createSocket(sock, host, port, true);
    configureSSLTLSSocket(sock, System.getProperty("crushftp.tls_version_client", "SSLv2Hello,TLSv1,TLSv1.1,TLSv1.2"));
    return sock;
  }
  
  public static ServerSocket getSSLServerSocket(int serverPort, String listen_ip, boolean acceptAnyCert) throws Exception {
    SSLContext sslc = SSLContext.getInstance("TLS");
    KeyStore keystore = KeyStore.getInstance("JKS");
    if (class$1 == null)
      try {
      
      } catch (ClassNotFoundException classNotFoundException) {
        throw new NoClassDefFoundError(null.getMessage());
      }  
    class$1.load((class$1 = Class.forName("com.crushftp.client.Common")).getResource("/assets/builtin").openStream(), "crushftp".toCharArray());
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(keystore, "crushftp".toCharArray());
    if (acceptAnyCert) {
      sslc.init(kmf.getKeyManagers(), trustAllCerts, new SecureRandom());
    } else {
      TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmf.init(keystore);
      sslc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
    } 
    SSLServerSocket serverSocket = null;
    if (listen_ip == null) {
      serverSocket = (SSLServerSocket)sslc.getServerSocketFactory().createServerSocket(serverPort, 1000, InetAddress.getByName("0.0.0.0"));
    } else {
      serverSocket = (SSLServerSocket)sslc.getServerSocketFactory().createServerSocket(serverPort, 1000, InetAddress.getByName(listen_ip));
    } 
    return serverSocket;
  }
  
  public static void trustEverything() {
    try {
      SSLContext sc = getSSLContext();
      sc.init(null, trustAllCerts, new SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
      HostnameVerifier hv = new HostnameVerifier() {
          public boolean verify(String urlHostName, SSLSession session) {
            return true;
          }
        };
      HttpsURLConnection.setDefaultHostnameVerifier(hv);
    } catch (Exception e) {
      log("SERVER", 1, e);
    } 
  }
  
  public static SSLContext getSSLContext() {
    SSLContext sc = null;
    try {
      sc = SSLContext.getInstance("TLSv1.2");
    } catch (NoSuchAlgorithmException e1) {
      try {
        sc = SSLContext.getInstance("TLSv1.1");
      } catch (NoSuchAlgorithmException e) {
        try {
          sc = SSLContext.getInstance("TLSv1");
        } catch (NoSuchAlgorithmException e2) {
          try {
            sc = SSLContext.getInstance("SSL");
          } catch (NoSuchAlgorithmException e3) {
            log("SERVER", 0, e3);
          } 
        } 
      } 
    } 
    return sc;
  }
  
  public static void streamCopier(InputStream in, OutputStream out) throws InterruptedException {
    streamCopier(in, out, true, true, true);
  }
  
  public static void streamCopier(InputStream in, OutputStream out, boolean async) throws InterruptedException {
    streamCopier(in, out, async, true, true);
  }
  
  public static void streamCopier(InputStream in, OutputStream out, boolean async, boolean closeInput, boolean closeOutput) throws InterruptedException {
    Runnable r = new Runnable(in, out, closeInput, closeOutput) {
        private final InputStream val$in;
        
        private final OutputStream val$out;
        
        private final boolean val$closeInput;
        
        private final boolean val$closeOutput;
        
        public void run() {
          InputStream inp = this.val$in;
          OutputStream outp = this.val$out;
          try {
            byte[] b = new byte[65535];
            int bytesRead = 0;
            while (bytesRead >= 0) {
              bytesRead = inp.read(b);
              if (bytesRead >= 0)
                outp.write(b, 0, bytesRead); 
            } 
          } catch (Exception e) {
            if (!e.getMessage().equalsIgnoreCase("Socket closed") && !e.getMessage().equalsIgnoreCase("Connection reset"))
              Common.log("SERVER", 2, e); 
          } finally {
            if (this.val$closeInput)
              try {
                inp.close();
              } catch (Exception e) {
                Common.log("SERVER", 1, e);
              }  
            if (this.val$closeOutput)
              try {
                outp.close();
              } catch (Exception e) {
                Common.log("SERVER", 1, e);
              }  
          } 
        }
      };
    if (async) {
      (new Thread(r)).start();
    } else {
      r.run();
    } 
  }
  
  public static String login(String url, String username, String password) throws Exception {
    return login(url, username, password, null);
  }
  
  public static String login(String url, String username, String password, String clientid) throws Exception {
    HttpURLConnection urlc = (HttpURLConnection)(new URL(url)).openConnection();
    urlc.setRequestMethod("POST");
    urlc.setUseCaches(false);
    urlc.setDoOutput(true);
    urlc.getOutputStream().write(("command=login&username=" + username + "&password=" + password + ((clientid != null) ? ("&clientid=" + clientid) : "")).getBytes("UTF8"));
    urlc.getResponseCode();
    String cookies = "";
    for (int x = 0; x < 100; x++) {
      try {
        cookies = urlc.getHeaderField(x);
        if (cookies != null && 
          cookies.indexOf("CrushAuth") >= 0)
          break; 
      } catch (Exception exception) {}
    } 
    String ca = null;
    if (cookies != null && cookies.indexOf("CrushAuth=") >= 0)
      ca = cookies.substring(cookies.indexOf("CrushAuth=") + "CrushAuth=".length(), cookies.indexOf(";", cookies.indexOf("CrushAuth="))).trim(); 
    urlc.disconnect();
    return ca;
  }
  
  public static String consumeResponse(InputStream in) throws Exception {
    byte[] b = new byte[32768];
    int bytesRead = 0;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    while (bytesRead >= 0) {
      bytesRead = in.read(b);
      if (bytesRead > 0)
        baos.write(b, 0, bytesRead); 
    } 
    in.close();
    return new String(baos.toByteArray(), "UTF8");
  }
  
  public static Properties getConnectedSocks(boolean ssl) throws Exception {
    Properties tmpSocks = new Properties();
    Properties socks = new Properties();
    int local_port = 0;
    Socket sock1 = null;
    while (true) {
      long start = System.currentTimeMillis();
      ServerSocket ss = ssl ? getSSLServerSocket(0, "127.0.0.1", true) : new ServerSocket(0);
      (new Thread(new Runnable(tmpSocks, ss) {
            private final Properties val$tmpSocks;
            
            private final ServerSocket val$ss;
            
            public void run() {
              try {
                this.val$tmpSocks.put((new StringBuffer(String.valueOf(this.val$ss.getLocalPort()))).toString(), this.val$ss.accept());
              } catch (Exception e) {
                Common.log("SERVER", 1, e);
              } 
            }
          })).start();
      local_port = ss.getLocalPort();
      sock1 = ssl ? getSSLSocket("127.0.0.1", local_port, true) : new Socket("127.0.0.1", local_port);
      while (!tmpSocks.containsKey((new StringBuffer(String.valueOf(local_port))).toString()) && System.currentTimeMillis() - start < 10000L)
        Thread.sleep(10L); 
      ss.close();
      if (System.currentTimeMillis() - start < 10000L)
        break; 
      sock1.close();
    } 
    Socket sock2 = (Socket)tmpSocks.get((new StringBuffer(String.valueOf(local_port))).toString());
    socks.put("sock1", sock1);
    socks.put("sock2", sock2);
    return socks;
  }
  
  public static String percent(int i1, int i2) {
    return String.valueOf(i1 * 100 / i2) + "%";
  }
  
  public static String format_bytes_short(long bytes) {
    String return_str = "";
    try {
      long tb = 1099511627776L;
      if (bytes > tb) {
        return_str = String.valueOf((int)((float)bytes / (float)tb * 100.0F) / 100.0F) + " TB";
      } else if (bytes > 1073741824L) {
        return_str = String.valueOf((int)((float)bytes / 1.07374182E9F * 100.0F) / 100.0F) + " GB";
      } else if (bytes > 1048576L) {
        return_str = String.valueOf((int)((float)bytes / 1048576.0F * 100.0F) / 100.0F) + " MB";
      } else if (bytes > 1024L) {
        return_str = String.valueOf((int)((float)bytes / 1024.0F * 100.0F) / 100.0F) + " KB";
      } else {
        return_str = String.valueOf(bytes) + " B";
      } 
    } catch (Exception e) {
      log("SERVER", 1, e);
    } 
    return return_str;
  }
  
  protected static final String[] pads = new String[] { 
      "", " ", "  ", "   ", "    ", "     ", "      ", "       ", "        ", "         ", 
      "          ", "           ", "            ", "             ", "              ", "               " };
  
  public static String lpad(String s, int len) {
    if (len - s.length() > 0)
      s = String.valueOf(pads[len - s.length()]) + s; 
    return s;
  }
  
  public static String rpad(String s, int len) {
    if (len - s.length() > 0)
      s = String.valueOf(s) + pads[len - s.length()]; 
    return s;
  }
  
  public static void writeEnd(BufferedOutputStream dos, String boundary) throws Exception {
    dos.write((String.valueOf(boundary) + "--\r\n").getBytes("UTF8"));
    dos.flush();
    dos.close();
  }
  
  public static void writeEntry(String key, String val, BufferedOutputStream dos, String boundary) throws Exception {
    dos.write((String.valueOf(boundary) + "\r\n").getBytes("UTF8"));
    dos.write(("Content-Disposition: form-data; name=\"" + key + "\"\r\n").getBytes("UTF8"));
    dos.write("\r\n".getBytes("UTF8"));
    dos.write(val.getBytes("UTF8"));
    dos.write("\r\n".getBytes("UTF8"));
  }
  
  public static GenericClient getClient(String url, String logHeader, Vector logQueue) {
    if (url.toUpperCase().startsWith("FTP:") || url.toUpperCase().startsWith("FTPS:") || url.toUpperCase().startsWith("FTPES:"))
      return new FTPClient(url, logHeader, logQueue); 
    if (url.toUpperCase().startsWith("FILE:"))
      return new FileClient(url, logHeader, logQueue); 
    if (url.toUpperCase().startsWith("RFILE:"))
      return new RFileClient(url, logHeader, logQueue); 
    if (url.toUpperCase().startsWith("MEMORY:"))
      return new MemoryClient(url, logHeader, logQueue); 
    if (url.toUpperCase().startsWith("ZIP:"))
      return new ZipClient(url, logHeader, logQueue); 
    if (url.toUpperCase().startsWith("SFTP:"))
      return new SFTPClient(url, logHeader, logQueue); 
    if (url.toUpperCase().startsWith("HTTP:") || url.toUpperCase().startsWith("HTTPS:"))
      return new HTTPClient(url, logHeader, logQueue); 
    if (url.toUpperCase().startsWith("WEBDAV:") || url.toUpperCase().startsWith("WEBDAVS:"))
      return new WebDAVClient(url, logHeader, logQueue); 
    if (url.toUpperCase().startsWith("S3:"))
      return new S3Client(url, logHeader, logQueue); 
    if (url.toUpperCase().startsWith("S3CRUSH:"))
      return new S3CrushClient(url, logHeader, logQueue); 
    if (url.toUpperCase().startsWith("GDRIVE:"))
      return new GDriveClient(url, logHeader, logQueue); 
    if (url.toUpperCase().startsWith("SMB:"))
      return new SMBClient(url, logHeader, logQueue); 
    return null;
  }
  
  public static Socket getSockVRL(VRL u) throws Exception {
    int port = u.getPort();
    if (u.getProtocol().equalsIgnoreCase("HTTP") && port < 0)
      port = 80; 
    if (u.getProtocol().equalsIgnoreCase("HTTPS") && port < 0)
      port = 443; 
    if (u.getProtocol().equalsIgnoreCase("FTP") && port < 0)
      port = 21; 
    Socket sock = new Socket(u.getHost(), port);
    if (u.getProtocol().equalsIgnoreCase("HTTPS")) {
      TrustManager[] trustAllCerts = { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
              return null;
            }
            
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
            
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
          } };
      SSLContext sc = getSSLContext();
      sc.init(null, trustAllCerts, new SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
      sock = sc.getSocketFactory().createSocket(sock, u.getHost(), port, true);
      configureSSLTLSSocket(sock, System.getProperty("crushftp.tls_version_client", "SSLv2Hello,TLSv1,TLSv1.1,TLSv1.2"));
      ((SSLSocket)sock).setUseClientMode(true);
      ((SSLSocket)sock).startHandshake();
    } 
    return sock;
  }
  
  public static void configureSSLTLSSocket(Object sock, String list) {
    if (list == null || list.equals("*"))
      return; 
    if (sock instanceof SSLServerSocket) {
      ((SSLServerSocket)sock).setEnabledProtocols(new String[] { "TLSv1" });
    } else if (sock instanceof SSLSocket) {
      ((SSLSocket)sock).setEnabledProtocols(new String[] { "TLSv1" });
    } 
    if (list != null && !list.equals("")) {
      Vector tls = new Vector();
      for (int x = 0; x < (list.split(",")).length; x++) {
        String s = list.split(",")[x];
        if (!s.trim().equals("")) {
          s = s.replace('t', 'T').replace('l', 'L').replace('s', 'S').replace('V', 'v');
          if (s.toUpperCase().indexOf("SSLv2Hello".toUpperCase()) >= 0)
            s = replace_str(s.toUpperCase(), "SSLV2HELLO", "SSLv2Hello"); 
          if (!s.startsWith("TLSv") && !s.startsWith("SSLv"))
            s = "TLSv" + s; 
          if (!bad_tls_protocols.containsKey(s))
            try {
              if (!s.equals("SSLv2Hello"))
                if (sock instanceof SSLServerSocket) {
                  ((SSLServerSocket)sock).setEnabledProtocols(new String[] { s });
                } else if (sock instanceof SSLSocket) {
                  ((SSLSocket)sock).setEnabledProtocols(new String[] { s });
                }  
              tls.addElement(s);
            } catch (IllegalArgumentException e) {
              bad_tls_protocols.put(s, "false");
              log("SERVER", 0, String.valueOf(s) + " not supported:" + e);
            }  
        } 
      } 
      String[] tls_str = new String[tls.size()];
      for (int i = 0; i < tls.size(); i++)
        tls_str[i] = tls.elementAt(i).toString(); 
      if (sock instanceof SSLServerSocket) {
        ((SSLServerSocket)sock).setEnabledProtocols(tls_str);
      } else if (sock instanceof SSLSocket) {
        ((SSLSocket)sock).setEnabledProtocols(tls_str);
        ((SSLSocket)sock).addHandshakeCompletedListener(new HandshakeCompletedListener() {
              public void handshakeCompleted(HandshakeCompletedEvent event) {
                if (System.getProperty("crushftp.ssl_renegotiation_blocked", "true").equals("true"))
                  event.getSocket().setEnabledCipherSuites(new String[0]); 
              }
            });
      } 
    } 
  }
  
  public static Socket getSocket(String protocol, VRL u, String use_dmz, String sticky_token) throws IOException {
    synchronized (socketPool) {
      if (u.getHost().toLowerCase().indexOf("amazonaws.com") < 0) {
        if (!socketPool.containsKey(String.valueOf(sticky_token) + ":" + u.getProtocol() + ":" + u.getHost() + ":" + u.getPort()))
          socketPool.put(String.valueOf(sticky_token) + ":" + u.getProtocol() + ":" + u.getHost() + ":" + u.getPort(), new Vector()); 
        Vector socks = (Vector)socketPool.get(String.valueOf(sticky_token) + ":" + u.getProtocol() + ":" + u.getHost() + ":" + u.getPort());
        while (socks.size() > 0) {
          Properties info = socks.remove(0);
          Socket sock = (Socket)info.remove("sock");
          if (System.currentTimeMillis() - Long.parseLong(info.getProperty("time")) < socketTimeout && !sock.isClosed())
            return sock; 
          sock.close();
        } 
      } 
    } 
    if (System2.containsKey("crushftp.dmz.queue.sock") && (use_dmz.equals("") || use_dmz.equalsIgnoreCase("false") || use_dmz.equalsIgnoreCase("no") || use_dmz.startsWith("socks://") || use_dmz.startsWith("http://") || use_dmz.equalsIgnoreCase("internal://"))) {
      Vector socket_queue = (Vector)System2.get("crushftp.dmz.queue.sock");
      if (socket_queue == null)
        socket_queue = (Vector)System2.get("crushftp.dmz.queue"); 
      log("DMZ", 2, "GET:SOCKET:Requesting socket conenction from internal server out of the pool using port:" + u.getPort());
      Properties mySock = new Properties();
      mySock.put("type", "GET:SOCKET");
      mySock.put("port", (new StringBuffer(String.valueOf(u.getPort()))).toString());
      if (use_dmz.equalsIgnoreCase("internal://"))
        mySock.put("port", String.valueOf(u.getHost()) + ":" + u.getPort()); 
      mySock.put("data", new Properties());
      mySock.put("id", String.valueOf(makeBoundary(10)) + (new Date()).getTime());
      mySock.put("sticky_token", sticky_token);
      mySock.put("created", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      mySock.put("need_response", "true");
      socket_queue.addElement(mySock);
      int loops = 0;
      while (loops++ < 3000) {
        if (mySock.containsKey("socket"))
          break; 
        try {
          Thread.sleep(10L);
        } catch (InterruptedException interruptedException) {}
      } 
      socket_queue.remove(mySock);
      if (mySock.get("socket") == null)
        throw new IOException("failure: Waited 30 seconds for DMZ socket, giving up."); 
      return (Socket)mySock.remove("socket");
    } 
    Exception lastError = null;
    for (int x = 0; x < 3; x++) {
      try {
        Socket sock = null;
        try {
          if (!use_dmz.equals("") && !use_dmz.equals("(current_server)") && !use_dmz.equalsIgnoreCase("false") && !use_dmz.equalsIgnoreCase("no") && !use_dmz.startsWith("socks://") && !use_dmz.startsWith("http://") && !use_dmz.startsWith("internal://"))
            sock = new DMZSocket(u, use_dmz); 
        } catch (IOException e) {
          if (e.toUpperCase().indexOf("DMZ") < 0)
            log("SERVER", 1, e); 
        } 
        if (sock == null)
          if (use_dmz.startsWith("socks://")) {
            sock = new Socket(new Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved((new VRL(use_dmz)).getHost(), (new VRL(use_dmz)).getPort())));
            sock.connect(InetSocketAddress.createUnresolved(u.getHost(), u.getPort()));
          } else if (System.getProperty("java.net.useSystemProxies", "false").equals("true") || use_dmz.startsWith("http://")) {
            Proxy p0 = null;
            if (!use_dmz.startsWith("http://")) {
              List proxies = ProxySelector.getDefault().select(URI.create(String.valueOf(u.getProtocol()) + "://" + u.getHost() + ":" + u.getPort()));
              p0 = proxies.get(0);
            } 
            if (p0 != null && p0.type().toString().equalsIgnoreCase("DIRECT")) {
              sock = new Socket(u.getHost(), u.getPort());
            } else if (p0 != null && p0.type().toString().equalsIgnoreCase("SOCKS")) {
              sock = new Socket(p0);
              sock.connect(InetSocketAddress.createUnresolved(u.getHost(), u.getPort()));
            } else if ((p0 != null && p0.type().toString().equalsIgnoreCase("HTTP")) || use_dmz.startsWith("http://")) {
              VRL vrl = null;
              if (p0 != null) {
                String url_temp = p0.toString().substring(p0.toString().indexOf("@") + 1).trim();
                if (url_temp.indexOf("/") >= 0)
                  url_temp = url_temp.substring(url_temp.indexOf("/") + 1).trim(); 
                System.out.println("Connecting through proxy server:" + url_temp);
                if (url_temp.toUpperCase().startsWith("HTTP://"))
                  vrl = new VRL(String.valueOf(url_temp) + "/"); 
                if (url_temp.startsWith("/")) {
                  vrl = new VRL("http:/" + url_temp + "/");
                } else {
                  vrl = new VRL("http://" + url_temp + "/");
                } 
              } else {
                vrl = new VRL(use_dmz);
              } 
              System.out.println("Connecting through proxy server:" + vrl + " to location:" + u.getHost() + ":" + u.getPort());
              sock = new Socket(vrl.getHost(), vrl.getPort());
              String header = "CONNECT " + u.getHost() + ":" + u.getPort() + " HTTP/1.1\r\n";
              header = String.valueOf(header) + "Host: " + u.getHost() + ":" + u.getPort() + "\r\n";
              header = String.valueOf(header) + "Proxy-Connection: Keep-Alive\r\n";
              header = String.valueOf(header) + "\r\n";
              sock.getOutputStream().write(header.getBytes("UTF8"));
              sock.getOutputStream().flush();
              byte[] b1 = new byte[1];
              StringBuffer sb = new StringBuffer();
              int bytesRead = 0;
              InputStream in = sock.getInputStream();
              while (bytesRead >= 0) {
                bytesRead = in.read(b1);
                if (bytesRead > 0)
                  sb.append(new String(b1)); 
                if (sb.toString().endsWith("\r\n\r\n"))
                  break; 
              } 
              if (!sb.toString().trim().startsWith("200") && !sb.toString().trim().startsWith("HTTP/1.1 200")) {
                sock.close();
                throw new IOException(sb.toString());
              } 
            } 
          } else {
            String host = u.getHost();
            if (host.indexOf("~") >= 0) {
              sock = new Socket(host.split("~")[1], u.getPort(), InetAddress.getByName(host.split("~")[0]), 0);
            } else {
              sock = new Socket(host, u.getPort());
            } 
          }  
        sock.setTcpNoDelay(true);
        return sock;
      } catch (SocketException e) {
        lastError = e;
        if (lastError.toUpperCase().indexOf("REFUSED") >= 0) {
          try {
            Thread.sleep(100L);
          } catch (Exception exception) {}
        } else {
          try {
            Thread.sleep(10000L);
          } catch (Exception exception) {}
        } 
      } 
    } 
    throw new IOException(lastError);
  }
  
  public static void releaseSocket(Socket sock, VRL u, String sticky_token) {
    synchronized (socketPool) {
      if (u.getHost().toLowerCase().indexOf("amazonaws.com") >= 0) {
        try {
          sock.close();
        } catch (IOException e) {
          log("SERVER", 1, e);
        } 
      } else {
        if (sock.isClosed() || sock.isInputShutdown() || sock.isOutputShutdown())
          return; 
        if (!socketPool.containsKey(String.valueOf(sticky_token) + ":" + u.getProtocol() + ":" + u.getHost() + ":" + u.getPort()))
          socketPool.put(String.valueOf(sticky_token) + ":" + u.getProtocol() + ":" + u.getHost() + ":" + u.getPort(), new Vector()); 
        Vector socks = (Vector)socketPool.get(String.valueOf(sticky_token) + ":" + u.getProtocol() + ":" + u.getHost() + ":" + u.getPort());
        Properties info = new Properties();
        info.put("sock", sock);
        info.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
        socks.addElement(info);
      } 
    } 
  }
  
  public static long getDecryptedSize(String the_file_path) throws Exception {
    File f = new File(the_file_path);
    if (f.isDirectory())
      return -1L; 
    RandomAccessFile in = new RandomAccessFile(f.getPath(), "r");
    int headerSize = "CRUSHFTP_ENCRYPTED_kHBeMxiWj7Sb4PdqJ8".length() + "0                                        ".length();
    long size = in.length();
    byte[] b = new byte[headerSize];
    int bytesRead = in.read(b);
    String head = "";
    String tail = "";
    if (bytesRead >= 0)
      head = new String(b, 0, bytesRead, "UTF8"); 
    if (size > 30L) {
      in.seek(in.length() - 30L);
      bytesRead = in.read(b);
      if (bytesRead >= 0)
        tail = new String(b, 0, bytesRead, "UTF8"); 
    } 
    in.close();
    if (tail.indexOf(":::CRUSHFTP") >= 0)
      try {
        if ((tail.split("#")).length > 0) {
          tail = tail.substring(tail.indexOf(":::CRUSHFTP"));
          tail = tail.split("#")[1].trim();
          return Long.parseLong(tail.trim());
        } 
      } catch (NumberFormatException e) {
        log("SERVER", 1, e);
      }  
    if (size < headerSize)
      return -1L; 
    if (head.toUpperCase().startsWith("-----BEGIN PGP MESSAGE-----")) {
      head = head.substring("-----BEGIN PGP MESSAGE-----".length()).trim();
      try {
        if ((head.split("#")).length > 0) {
          head = head.split("#")[1].trim();
          return Long.parseLong(head.trim());
        } 
      } catch (NumberFormatException e) {
        log("SERVER", 1, e);
      } 
      return -1L;
    } 
    if (!head.startsWith("CRUSHFTP_ENCRYPTED_kHBeMxiWj7Sb4PdqJ8") && !head.startsWith("CRUSHFTP_PGPChunkedStream:dBa3Em7W4N:"))
      return -1L; 
    head = head.substring("CRUSHFTP_ENCRYPTED_kHBeMxiWj7Sb4PdqJ8".length());
    return Long.parseLong(head.trim());
  }
  
  public static long getFileSize(String the_file_path) {
    try {
      long size = getDecryptedSize(the_file_path);
      if (size >= 0L)
        return size; 
    } catch (Exception e) {
      log("SERVER", 1, e);
    } 
    return (new File(the_file_path)).length();
  }
  
  public static long pgpOffsetAdjuster(long l, Properties controller) {
    if (controller.getProperty("pgpEncrypt", "false").equals("true") || controller.getProperty("pgpDecrypt", "false").equals("true")) {
      long chunkAndPadding = 1050624L;
      long mbs = l / 1048576L;
      l = mbs * chunkAndPadding;
      l += ("CRUSHFTP_ENCRYPTED_kHBeMxiWj7Sb4PdqJ8".length() + "0                                        ".length());
    } 
    return l;
  }
  
  public static void generateKeyPair(String privateKeyPath, int keySize, int days, String password, String commonName) throws IOException {
    String[] hashingAlgorithms = { "SHA1", "SHA256", "SHA384", "SHA512", "MD5" };
    String[] compressions = { "ZIP", "ZLIB", "UNCOMPRESSED" };
    String[] cyphers = { "CAST5", "AES_128", "AES_192", "AES_256", "TWOFISH" };
    try {
      PGPKeyPair pGPKeyPair = PGPKeyPair.generateKeyPair(keySize, commonName, "RSA", password, compressions, hashingAlgorithms, cyphers, days);
      pGPKeyPair.exportPrivateKey(privateKeyPath, true);
      pGPKeyPair.exportPublicKey(String.valueOf(privateKeyPath.substring(0, privateKeyPath.lastIndexOf("."))) + ".pub", true);
    } catch (Exception e) {
      throw new IOException(e);
    } 
  }
  
  public static long getFreeRam() {
    return Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory();
  }
  
  public static long getChunkSize(InputStream original_is) throws IOException {
    byte[] chunkBytes = new byte[20];
    byte[] b = new byte[1];
    int bytesRead = 0;
    int loc = 0;
    while (bytesRead >= 0) {
      bytesRead = original_is.read(b);
      if (bytesRead > 0) {
        chunkBytes[loc++] = b[0];
      } else {
        return -1L;
      } 
      if (loc > 80 || (loc > 1 && chunkBytes[loc - 2] == 13 && chunkBytes[loc - 1] == 10))
        break; 
    } 
    String data = (new String(chunkBytes, 0, loc, "UTF8")).trim();
    if (data.equals(""))
      return 0L; 
    return Long.parseLong(data.trim(), 16);
  }
  
  public static String getBaseUrl(String s) {
    return getBaseUrl(s, true);
  }
  
  public static String getBaseUrl(String s, boolean s3_root_path) {
    String s_original = s;
    if (s.indexOf(":") < 0) {
      s = "/";
    } else if (s.indexOf("@") > 0 && s.indexOf(":") != s.lastIndexOf(":") && !s.startsWith("file:")) {
      if (s.lastIndexOf("@") > s.lastIndexOf(":")) {
        s = s.substring(0, s.indexOf("/", s.lastIndexOf("@")) + 1);
      } else {
        s = s.substring(0, s.indexOf("/", s.lastIndexOf(":")) + 1);
      } 
    } else if (s.toLowerCase().startsWith("file:/") && !s.toLowerCase().startsWith("file://")) {
      s = s.substring(0, s.indexOf("/", s.indexOf(":") + 1) + 1);
    } else if (s.toLowerCase().startsWith("file://") && !s.toLowerCase().startsWith("file:///")) {
      s = s.substring(0, s.indexOf("/", s.indexOf(":") + 2) + 1);
    } else {
      s = s.substring(0, s.indexOf("/", s.indexOf(":") + 3) + 1);
    } 
    if (s.toUpperCase().startsWith("S3:/") && s3_root_path) {
      String tmp_path = (new VRL(s_original)).getPath();
      if (tmp_path.length() > 1) {
        tmp_path = tmp_path.substring(1, tmp_path.indexOf("/", 1) + 1);
        s = String.valueOf(s) + tmp_path;
      } 
    } 
    return s;
  }
  
  public static Object readXMLObject(InputStream in) {
    Object result = null;
    try {
      Document doc = (new SAXBuilder()).build(in);
      result = getElements(doc.getRootElement());
      in.close();
    } catch (Exception e) {
      log("", 0, e);
    } 
    return result;
  }
  
  public static Object readXMLObject(File file) {
    Object result = null;
    InputStream in = null;
    try {
      in = new FileInputStream(file);
      result = readXMLObject(in);
    } catch (Exception e) {
      log("", 0, e);
    } finally {
      try {
        if (in != null)
          in.close(); 
      } catch (Exception exception) {}
    } 
    return result;
  }
  
  public static Object readXMLObject(String path) {
    try {
      if ((new File(path)).exists())
        return readXMLObject(new File(path)); 
    } catch (Exception e) {
      log("", 0, e);
    } 
    return null;
  }
  
  public static Object getElements(Element element) {
    Object result = null;
    if (element.getAttributeValue("type", "string").equalsIgnoreCase("properties")) {
      result = new Properties();
      List items2 = element.getChildren();
      if (items2.size() == 0)
        return null; 
      for (int x = 0; x < items2.size(); x++) {
        Element element2 = items2.get(x);
        Object o = getElements(element2);
        String keyName = element2.getName();
        keyName = element2.getAttributeValue("name", keyName);
        if (o != null)
          ((Properties)result).put(keyName, o); 
      } 
    } else if (element.getAttributeValue("type", "string").equalsIgnoreCase("vector")) {
      result = new Vector();
      List items2 = element.getChildren();
      if (items2.size() == 0)
        return null; 
      for (int x = 0; x < items2.size(); x++) {
        Element element2 = items2.get(x);
        Object o = getElements(element2);
        if (o != null)
          ((Vector)result).addElement(o); 
      } 
    } else if (element.getAttributeValue("type", "string").equalsIgnoreCase("string")) {
      return element.getText();
    } 
    return result;
  }
  
  public static String getXMLString(Object obj, String root) throws IOException {
    return getXMLString(obj, root, true);
  }
  
  public static String getXMLString(Object obj, String root, boolean pretty) throws IOException {
    Element element = new Element(root);
    addElements(element, obj);
    Document doc = new Document(element);
    XMLOutputter xx = new XMLOutputter();
    Format formatter = null;
    formatter = Format.getPrettyFormat();
    formatter.setExpandEmptyElements(true);
    if (pretty) {
      formatter.setIndent("\t");
    } else {
      formatter.setIndent(" ");
    } 
    xx.setFormat(formatter);
    String s = xx.outputString(doc);
    doc.removeContent();
    doc = null;
    element.removeContent();
    element.detach();
    element = null;
    return s;
  }
  
  public static void addParameters(Document document, Properties params) throws Exception {
    Element root = document.getRootElement();
    Iterator iterator = params.keySet().iterator();
    while (iterator != null && iterator.hasNext()) {
      String name = iterator.next();
      String value = (String)params.get(name);
      Element element = new Element("variable");
      element.setNamespace(Namespace.getNamespace("xsl"));
      element.setAttribute("name", name);
      element.addContent(value);
      root.addContent(1, (Content)element);
    } 
  }
  
  public static void writeXMLObject(String path, Object obj, String root) throws IOException {
    String xml = getXMLString(obj, root);
    RandomAccessFile eraser = new RandomAccessFile(path, "rw");
    eraser.setLength(0L);
    eraser.write(xml.getBytes("UTF8"));
    eraser.close();
  }
  
  public static void addElements(Element element, Object obj) {
    try {
      if (obj != null && obj instanceof Properties) {
        Properties p = (Properties)obj;
        element.setAttribute("type", "properties");
        Enumeration e = p.keys();
        while (e.hasMoreElements()) {
          String key = e.nextElement().toString();
          Object val = p.get(key);
          Element element2 = null;
          try {
            element2 = new Element(key);
          } catch (Exception ee) {
            element2 = new Element("item");
            element2.setAttribute("name", key);
          } 
          element.addContent((Content)element2);
          addElements(element2, val);
        } 
      } else if (obj != null && obj instanceof Vector) {
        Vector v = (Vector)obj;
        element.setAttribute("type", "vector");
        for (int x = 0; x < v.size(); x++) {
          String keyName = element.getName();
          keyName = element.getAttributeValue("name", keyName);
          Element element2 = null;
          try {
            element2 = new Element(String.valueOf(element.getName()) + "_subitem");
          } catch (Exception ee) {
            element2 = new Element("item_subitem");
            element2.setAttribute("name", keyName);
          } 
          element.addContent((Content)element2);
          addElements(element2, v.elementAt(x));
        } 
      } else if (obj != null && obj instanceof VRL) {
        Object object = obj;
        try {
          element.setText((String)object);
        } catch (Exception e) {
          element.setText(URLEncoder.encode((String)object, "utf-8"));
        } 
      } else if (obj == null || !(obj instanceof BufferedReader)) {
        if (obj == null || !(obj instanceof java.io.BufferedWriter))
          if (obj != null) {
            String s = (String)obj;
            try {
              element.setText(s);
            } catch (Exception e) {
              element.setText(URLEncoder.encode(s, "utf-8"));
            } 
          }  
      } 
    } catch (Exception e) {
      log("", 1, e);
    } 
  }
  
  public static void writeFileFromJar(String src, String dst, boolean preservePath) {
    InputStream in = null;
    RandomAccessFile out = null;
    try {
      if (!(new File(src)).exists()) {
        in = (new Common()).getClass().getResourceAsStream(src);
        if (preservePath) {
          (new File(all_but_last(String.valueOf(dst) + src))).mkdirs();
        } else if (src.indexOf("/") >= 0) {
          src = last(src);
        } 
        out = new RandomAccessFile(String.valueOf(dst) + src, "rw");
        int bytes = 0;
        byte[] b = new byte[32768];
        while (bytes >= 0) {
          bytes = in.read(b);
          if (bytes > 0)
            out.write(b, 0, bytes); 
        } 
      } 
    } catch (Exception e) {
      log("", 1, e);
    } finally {
      if (in != null)
        try {
          in.close();
        } catch (Exception e) {
          log("", 1, e);
        }  
      if (out != null)
        try {
          out.close();
        } catch (Exception e) {
          log("", 1, e);
        }  
    } 
  }
  
  public static void copy(String src, String dst, boolean overwrite) throws Exception {
    if ((new File(src)).isDirectory()) {
      (new File(dst)).mkdirs();
      return;
    } 
    if ((new File(dst)).exists() && !overwrite)
      return; 
    RandomAccessFile in = null;
    RandomAccessFile out = null;
    try {
      in = new RandomAccessFile(src, "r");
      out = new RandomAccessFile(dst, "rw");
      out.setLength(0L);
      byte[] b = new byte[32768];
      int bytesRead = 0;
      while (bytesRead >= 0) {
        bytesRead = in.read(b);
        if (bytesRead >= 0)
          out.write(b, 0, bytesRead); 
      } 
      in.close();
      out.close();
    } finally {
      if (in != null)
        in.close(); 
      if (out != null)
        out.close(); 
    } 
    (new File(dst)).setLastModified((new File(src)).lastModified());
  }
  
  public static boolean zip(String root_dir, Vector zipFiles, String outputPath) throws Exception {
    root_dir = String.valueOf((new File(root_dir)).getCanonicalPath().replace('\\', '/')) + "/";
    ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(outputPath));
    zout.setLevel(9);
    for (int xx = 0; xx < zipFiles.size(); xx++) {
      Properties item = zipFiles.elementAt(xx);
      File file = new File((new VRL(item.getProperty("url"))).getPath());
      if (file.isDirectory()) {
        String itemName = (String.valueOf(file.getCanonicalPath().substring(root_dir.length() - 1)) + "/").replace('\\', '/');
        zout.putNextEntry(new ZipEntry(itemName));
      } else if (file.isFile()) {
        String itemName = file.getCanonicalPath().substring(root_dir.length() - 1).replace('\\', '/');
        if (itemName.indexOf(":") >= 0)
          itemName = itemName.substring(itemName.indexOf(":") + 1).trim(); 
        zout.putNextEntry(new ZipEntry(itemName));
        RandomAccessFile in = new RandomAccessFile(file.getCanonicalPath(), "r");
        byte[] b = new byte[65535];
        int bytesRead = 0;
        while (bytesRead >= 0) {
          bytesRead = in.read(b);
          if (bytesRead > 0)
            zout.write(b, 0, bytesRead); 
        } 
        in.close();
      } 
      zout.closeEntry();
    } 
    zout.finish();
    zout.flush();
    zout.close();
    return true;
  }
  
  public static String encryptDecrypt(String s, boolean encrypt) throws Exception {
    if (s == null || s.equals(""))
      return ""; 
    String key = "crushftp";
    while (key.length() / 8.0F != (key.length() / 8))
      key = String.valueOf(key) + "Z"; 
    MessageDigest md = MessageDigest.getInstance("SHA");
    md.update(key.getBytes());
    DESKeySpec desKeySpec = new DESKeySpec(Base64.encodeBytes(md.digest()).getBytes());
    SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
    SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
    Cipher ecipher = Cipher.getInstance("DES");
    Cipher dcipher = Cipher.getInstance("DES");
    ecipher.init(1, secretKey);
    dcipher.init(2, secretKey);
    if (encrypt)
      return Base64.encodeBytes(ecipher.doFinal(s.getBytes("UTF8"))); 
    return new String(dcipher.doFinal(Base64.decode(s.replace(' ', '+'))), "UTF8");
  }
  
  public static void recurseDelete(String real_path, boolean test_mode) {
    if (real_path.trim().equals("/"))
      return; 
    if (real_path.trim().equals("~"))
      return; 
    if (real_path.indexOf(":") >= 0 && real_path.length() < 4)
      return; 
    File f = new File(real_path);
    try {
      real_path = String.valueOf(f.getCanonicalPath()) + "/";
      f = new File(real_path);
    } catch (Exception e) {
      log("", 1, e);
    } 
    if (f.isDirectory()) {
      String[] files = f.list();
      for (int x = 0; x < files.length; x++) {
        File f2 = new File(String.valueOf(real_path) + files[x]);
        if (!isSymbolicLink(f2.getAbsolutePath())) {
          if (f2.isDirectory())
            recurseDelete(String.valueOf(real_path) + files[x] + "/", test_mode); 
          if (test_mode) {
            log("", 0, "*****************DELETE:" + f2);
          } else {
            f2.delete();
          } 
        } else {
          f2.delete();
        } 
      } 
    } 
    if (test_mode) {
      log("", 0, "*****************DELETE:" + f);
    } else {
      f.delete();
    } 
  }
  
  public static long recurseSize(String real_path, long size) {
    if (real_path.trim().equals("/"))
      return size; 
    if (real_path.trim().equals("~"))
      return size; 
    if (real_path.indexOf(":") >= 0 && real_path.length() < 4)
      return size; 
    File f = new File(real_path);
    try {
      real_path = String.valueOf(f.getCanonicalPath()) + "/";
      f = new File(real_path);
    } catch (IOException e) {
      e.printStackTrace();
    } 
    if (f.isDirectory()) {
      String[] files = f.list();
      for (int x = 0; x < files.length; x++) {
        File f2 = new File(String.valueOf(real_path) + files[x]);
        if (!isSymbolicLink(f2.getAbsolutePath()))
          if (f2.isDirectory()) {
            size = recurseSize(String.valueOf(real_path) + files[x] + "/", size);
          } else {
            size += f2.length();
          }  
      } 
    } 
    size += f.length();
    return size;
  }
  
  public static String getMD5(InputStream in) throws Exception {
    MessageDigest m = MessageDigest.getInstance("MD5");
    try {
      in = new BufferedInputStream(in, 1048576);
      byte[] b = new byte[1048576];
      int bytesRead = 0;
      while (bytesRead >= 0) {
        bytesRead = in.read(b);
        if (bytesRead >= 0)
          m.update(b, 0, bytesRead); 
      } 
    } finally {
      in.close();
    } 
    String s = (new BigInteger(1, m.digest())).toString(16).toLowerCase();
    if (s.length() < 32)
      s = "0" + s; 
    return s;
  }
  
  public static void getAllFileListing(Vector list, String path, int depth, boolean includeFolders) throws Exception {
    SnapshotFile item = new SnapshotFile(path);
    if (item.isFile()) {
      list.addElement(item);
    } else {
      appendListing(path, list, "", depth, includeFolders);
    } 
  }
  
  public static void appendListing(String path, Vector list, String dir, int depth, boolean includeFolders) throws Exception {
    if (depth == 0)
      return; 
    depth--;
    if (!path.endsWith("/"))
      path = String.valueOf(path) + "/"; 
    String[] items = (new SnapshotFile(String.valueOf(path) + dir)).list();
    if (items == null)
      return; 
    for (int x = 0; x < items.length; x++) {
      SnapshotFile item = new SnapshotFile(String.valueOf(path) + dir + items[x]);
      if (item.isFile() || includeFolders)
        list.addElement(item); 
      if (item.isDirectory())
        appendListing(path, list, String.valueOf(dir) + items[x] + "/", depth, includeFolders); 
    } 
    if (items.length == 0)
      list.addElement(new SnapshotFile(String.valueOf(path) + dir)); 
  }
  
  public static String getPasswordPrompt(String label) {
    JPasswordField jpf = new JPasswordField(30);
    JPanel messagePanel = new JPanel();
    messagePanel.add(new JLabel(label));
    messagePanel.add(jpf);
    (new Thread(new Runnable(jpf) {
          private final JPasswordField val$jpf;
          
          public void run() {
            for (int x = 0; x < 5; x++) {
              try {
                Thread.sleep(100L);
              } catch (Exception exception) {}
              this.val$jpf.requestFocus();
            } 
          }
        })).start();
    if (JOptionPane.showConfirmDialog(null, messagePanel, "Password", 2) == 0)
      return new String(jpf.getPassword()); 
    return null;
  }
  
  public static void doMD5Comparisons(VRL localVrl, VRL remoteVrl, String direction, Properties statusInfo, String path3, Vector chunksF1, Vector chunksF2, StringBuffer crushAuth, StringBuffer status1, StringBuffer status2, Vector byteRanges) throws InterruptedException {
    Thread keepTunnelActiveThread = null;
    long localSize = (new File(localVrl.getPath())).length();
    try {
      statusInfo.put(String.valueOf(direction) + "Status", String.valueOf(direction) + ": Getting MD5s for " + path3);
      (new Thread(new Runnable(path3, status1, chunksF1, status2, remoteVrl, crushAuth, localSize) {
            private final String val$path3;
            
            private final StringBuffer val$status1;
            
            private final Vector val$chunksF1;
            
            private final StringBuffer val$status2;
            
            private final VRL val$remoteVrl;
            
            private final StringBuffer val$crushAuth;
            
            private final long val$localSize;
            
            public void run() {
              Thread.currentThread().setName("GetRemoteMD5s:" + this.val$path3);
              try {
                Tunnel2.getRemoteMd5s(this.val$remoteVrl.toString(), this.val$path3, this.val$chunksF1, true, this.val$crushAuth, this.val$status1, this.val$localSize);
              } catch (Exception e) {
                e.printStackTrace();
              } finally {
                this.val$status1.append("done");
                if (this.val$chunksF1.size() == 0)
                  this.val$status2.append("skip"); 
              } 
            }
          })).start();
      (new Thread(new Runnable(localVrl, status2, chunksF2, status1) {
            private final VRL val$localVrl;
            
            private final StringBuffer val$status2;
            
            private final Vector val$chunksF2;
            
            private final StringBuffer val$status1;
            
            public void run() {
              Thread.currentThread().setName("GetLocalMD5s:" + this.val$localVrl);
              try {
                Tunnel2.getLocalMd5s(new File(this.val$localVrl.getPath()), true, this.val$status2, this.val$chunksF2);
              } catch (Exception e) {
                e.printStackTrace();
              } finally {
                this.val$status2.append("done");
                if (this.val$chunksF2.size() == 0)
                  this.val$status1.append("skip"); 
              } 
            }
          })).start();
      while (status1.length() == 0 || status2.length() == 0) {
        if (status2.length() > 0 && chunksF1.size() > chunksF2.size()) {
          status1.setLength(0);
          status1.append("done");
        } else if (status1.length() > 0 && chunksF2.size() > chunksF1.size()) {
          status2.setLength(0);
          status2.append("done");
        } 
        Thread.sleep(100L);
      } 
      if (chunksF1.size() > 0) {
        byteRanges.removeAllElements();
        byteRanges.addAll(Tunnel2.compareMd5s(chunksF1, chunksF2, false));
      } 
    } finally {
      status1.append("done");
      status2.append("done");
      if (keepTunnelActiveThread != null)
        keepTunnelActiveThread.interrupt(); 
    } 
  }
  
  public static boolean do_search(String filter, String data, boolean single, int iteration) {
    return do_search(filter, data, single, iteration, false);
  }
  
  public static boolean do_search(String filter, String data, boolean single, int iteration, boolean caseSensitive) {
    boolean opposite = filter.startsWith("!");
    if (opposite)
      filter = filter.substring(1); 
    if (filter.indexOf("*") < 0 && filter.indexOf("?") < 0)
      return opposite ? (!filter.equals(data)) : filter.equals(data); 
    return opposite ? (!doFilter(getPattern(filter, caseSensitive), data)) : doFilter(getPattern(filter, caseSensitive), data);
  }
  
  public static boolean doFilter(Pattern pattern, String data) {
    if (pattern == null)
      return false; 
    boolean result = pattern.matcher(data).matches();
    if (!result)
      result = pattern.matcher(data).find(); 
    return result;
  }
  
  public static Pattern getPattern(String patternStr, boolean caseSensitive) {
    if (patternStr.startsWith("REGEX:")) {
      patternStr = patternStr.substring("REGEX:".length());
    } else {
      patternStr = "^" + patternStr;
      if (!patternStr.endsWith("*") && !patternStr.endsWith("$"))
        patternStr = String.valueOf(patternStr) + "$"; 
      patternStr = replace_str(patternStr, ".", "\\.").replace('?', '.');
      patternStr = replace_str(patternStr, "*", ".*");
    } 
    if (caseSensitive) {
      try {
        return Pattern.compile(patternStr);
      } catch (Exception e) {
        log("SERVER", 1, e);
      } 
    } else {
      try {
        return Pattern.compile(patternStr, 2);
      } catch (Exception e) {
        log("SERVER", 1, e);
      } 
    } 
    return null;
  }
  
  public static String safe_filename_characters(String s) {
    String safe = "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    String s2 = "";
    for (int x = 0; x < s.length(); x++) {
      if (safe.indexOf((new StringBuffer(String.valueOf(s.charAt(x)))).toString()) >= 0) {
        s2 = String.valueOf(s2) + s.charAt(x);
      } else {
        s2 = String.valueOf(s2) + "_";
      } 
    } 
    return s2;
  }
  
  public static String getCanonicalPath(String url) throws IOException {
    VRL vrl = new VRL(url);
    if (vrl.getProtocol().equalsIgnoreCase("file"))
      return (new File(vrl.getPath())).getCanonicalPath(); 
    String tmp_user = vrl.getUsername();
    tmp_user = tmp_user.replace('/', '_');
    tmp_user = tmp_user.replace('\\', '_');
    tmp_user = tmp_user.replace('<', '_');
    tmp_user = tmp_user.replace('>', '_');
    tmp_user = tmp_user.replace('+', '_');
    tmp_user = tmp_user.replace('#', '_');
    tmp_user = tmp_user.replace('%', '_');
    tmp_user = tmp_user.replace('^', '_');
    tmp_user = tmp_user.replace(':', '_');
    tmp_user = tmp_user.replace(';', '_');
    return "/" + vrl.getProtocol().toLowerCase() + "/" + vrl.getHost().toLowerCase() + vrl.getPath();
  }
  
  public static SSLSocket getSSLSocket(String trust_path, String pass1, String pass2, boolean acceptAnyCert, Socket sock, String host, int port) throws IOException {
    SSLSocket ss = null;
    try {
      TrustManager[] trustAllCerts = { new TrustManagerCustom(null, true, true) };
      SSLContext sslc = SSLContext.getInstance("TLS");
      KeyManager[] key_managers = (KeyManager[])null;
      if (trust_path != null && !trust_path.equals("")) {
        KeyStore trust_pfx = KeyStore.getInstance("PKCS12");
        KeyStore trust_jks = KeyStore.getInstance("JKS");
        KeyStore trust = null;
        String pass = "";
        try {
          pass = encryptDecrypt(pass1, false);
          trust_pfx.load(new FileInputStream(trust_path), pass.toCharArray());
          trust = trust_pfx;
        } catch (Exception e1) {
          try {
            pass = encryptDecrypt(pass2, false);
            trust_pfx.load(new FileInputStream(trust_path), pass.toCharArray());
            trust = trust_pfx;
          } catch (Exception e2) {
            try {
              pass = encryptDecrypt(pass1, false);
              trust_jks.load(new FileInputStream(trust_path), pass.toCharArray());
              trust = trust_jks;
            } catch (Exception e3) {
              try {
                pass = encryptDecrypt(pass2, false);
                trust_jks.load(new FileInputStream(trust_path), pass.toCharArray());
                trust = trust_jks;
              } catch (Exception e4) {
                e4.printStackTrace();
              } 
            } 
          } 
        } 
        if (trust != null) {
          KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
          kmf.init(trust, pass.toCharArray());
          key_managers = kmf.getKeyManagers();
        } 
      } 
      if (!acceptAnyCert)
        trustAllCerts = (TrustManager[])null; 
      sslc.init(key_managers, trustAllCerts, new SecureRandom());
      ss = (SSLSocket)sslc.getSocketFactory().createSocket(sock, host, port, true);
      configureSSLTLSSocket(ss, System.getProperty("crushftp.default_tls", "TLSv1,TLSv1.1,TLSv1.2"));
      return ss;
    } catch (Exception e) {
      log("SERVER", 2, e);
      throw new IOException(e);
    } 
  }
  
  public static String textFunctions_old(String the_line, String r1, String r2) throws Exception {
    while (the_line.indexOf(String.valueOf(r1) + "encrypt_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "encrypt_end" + r2) >= 0) {
      String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "encrypt_start" + r2) + (String.valueOf(r1) + "encrypt_start" + r2).length(), the_line.indexOf(String.valueOf(r1) + "encrypt_end" + r2));
      the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "encrypt_start" + r2))) + encryptDecrypt(inner, true) + the_line.substring(the_line.indexOf(String.valueOf(r1) + "encrypt_end" + r2) + (String.valueOf(r1) + "encrypt_end" + r2).length());
    } 
    while (the_line.indexOf(String.valueOf(r1) + "encode_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "encode_end" + r2) >= 0) {
      String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "encode_start" + r2) + (String.valueOf(r1) + "encode_start" + r2).length(), the_line.indexOf(String.valueOf(r1) + "encode_end" + r2));
      the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "encode_start" + r2))) + url_encode(inner) + the_line.substring(the_line.indexOf(String.valueOf(r1) + "encode_end" + r2) + (String.valueOf(r1) + "encode_end" + r2).length());
    } 
    while (the_line.indexOf(String.valueOf(r1) + "decode_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "decode_end" + r2) >= 0) {
      String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "decode_start" + r2) + (String.valueOf(r1) + "decode_start" + r2).length(), the_line.indexOf(String.valueOf(r1) + "decode_end" + r2));
      the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "decode_start" + r2))) + url_decode(inner) + the_line.substring(the_line.indexOf(String.valueOf(r1) + "decode_end" + r2) + (String.valueOf(r1) + "decode_end" + r2).length());
    } 
    while (the_line.indexOf(String.valueOf(r1) + "upper_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "upper_end" + r2) >= 0) {
      String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "upper_start" + r2) + (String.valueOf(r1) + "upper_start" + r2).length(), the_line.indexOf(String.valueOf(r1) + "upper_end" + r2));
      the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "upper_start" + r2))) + inner.toUpperCase() + the_line.substring(the_line.indexOf(String.valueOf(r1) + "upper_end" + r2) + (String.valueOf(r1) + "upper_end" + r2).length());
    } 
    while (the_line.indexOf(String.valueOf(r1) + "lower_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "lower_end" + r2) >= 0) {
      String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "lower_start" + r2) + (String.valueOf(r1) + "lower_start" + r2).length(), the_line.indexOf(String.valueOf(r1) + "lower_end" + r2));
      the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "lower_start" + r2))) + inner.toLowerCase() + the_line.substring(the_line.indexOf(String.valueOf(r1) + "lower_end" + r2) + (String.valueOf(r1) + "lower_end" + r2).length());
    } 
    while (the_line.indexOf(String.valueOf(r1) + "md5_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "md5_end" + r2) >= 0) {
      String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "md5_start" + r2) + (String.valueOf(r1) + "md5_start" + r2).length(), the_line.indexOf(String.valueOf(r1) + "md5_end" + r2));
      the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "md5_start" + r2))) + "MD5:" + getMD5(new ByteArrayInputStream(inner.getBytes("UTF8"))) + the_line.substring(the_line.indexOf(String.valueOf(r1) + "md5_end" + r2) + (String.valueOf(r1) + "md5_end" + r2).length());
    } 
    while (the_line.indexOf(String.valueOf(r1) + "chop_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "chop_end" + r2) >= 0) {
      String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "chop_start" + r2) + (String.valueOf(r1) + "chop_start" + r2).length(), the_line.indexOf(String.valueOf(r1) + "chop_end" + r2));
      the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "chop_start" + r2))) + all_but_last(inner) + the_line.substring(the_line.indexOf(String.valueOf(r1) + "chop_end" + r2) + (String.valueOf(r1) + "chop_end" + r2).length());
    } 
    while (the_line.indexOf(String.valueOf(r1) + "htmlclean_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "htmlclean_end" + r2) >= 0) {
      String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "htmlclean_start" + r2) + (String.valueOf(r1) + "htmlclean_start" + r2).length(), the_line.indexOf(String.valueOf(r1) + "htmlclean_end" + r2));
      the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "htmlclean_start" + r2))) + inner.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", "") + the_line.substring(the_line.indexOf(String.valueOf(r1) + "htmlclean_end" + r2) + (String.valueOf(r1) + "htmlclean_end" + r2).length());
    } 
    while (the_line.indexOf(String.valueOf(r1) + "htmlclean2_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "htmlclean2_end" + r2) >= 0) {
      String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "htmlclean2_start" + r2) + (String.valueOf(r1) + "htmlclean2_start" + r2).length(), the_line.indexOf(String.valueOf(r1) + "htmlclean2_end" + r2));
      while (inner.indexOf("<") >= 0) {
        if (inner.indexOf("<") < 0 || inner.indexOf(">") < 0)
          break; 
        inner = String.valueOf(inner.substring(0, inner.indexOf("<"))) + inner.substring(inner.indexOf(">", inner.indexOf("<")) + 1);
      } 
      the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "htmlclean2_start" + r2))) + inner + the_line.substring(the_line.indexOf(String.valueOf(r1) + "htmlclean2_end" + r2) + (String.valueOf(r1) + "htmlclean2_end" + r2).length());
    } 
    while (the_line.indexOf(String.valueOf(r1) + "last_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "last_end" + r2) >= 0) {
      String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "last_start" + r2) + (String.valueOf(r1) + "last_start" + r2).length(), the_line.indexOf(String.valueOf(r1) + "last_end" + r2));
      the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "last_start" + r2))) + last(inner) + the_line.substring(the_line.indexOf(String.valueOf(r1) + "last_end" + r2) + (String.valueOf(r1) + "last_end" + r2).length());
    } 
    while (the_line.indexOf(String.valueOf(r1) + "trim_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "trim_end" + r2) >= 0) {
      String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "trim_start" + r2) + (String.valueOf(r1) + "trim_start" + r2).length(), the_line.indexOf(String.valueOf(r1) + "trim_end" + r2));
      the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "trim_start" + r2))) + inner.trim() + the_line.substring(the_line.indexOf(String.valueOf(r1) + "trim_end" + r2) + (String.valueOf(r1) + "trim_end" + r2).length());
    } 
    while (the_line.indexOf(String.valueOf(r1) + "sql_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "sql_end" + r2) >= 0) {
      String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "sql_start" + r2) + (String.valueOf(r1) + "sql_start" + r2).length(), the_line.indexOf(String.valueOf(r1) + "sql_end" + r2));
      the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "sql_start" + r2))) + inner.replace('\'', '_').replace('"', '_').replace('%', '_').replace(';', ' ') + the_line.substring(the_line.indexOf(String.valueOf(r1) + "sql_end" + r2) + (String.valueOf(r1) + "sql_end" + r2).length());
    } 
    while (the_line.indexOf(String.valueOf(r1) + "indexof_start") >= 0 && the_line.indexOf(String.valueOf(r1) + "indexof_end" + r2) >= 0) {
      String params = the_line.substring(the_line.indexOf(String.valueOf(r1) + "indexof_start") + (String.valueOf(r1) + "indexof_start").length());
      params = params.substring(0, params.indexOf("}"));
      String search = params.split(":")[1];
      search = replace_str(search, "~..~", ":");
      int loc = Integer.parseInt(params.split(":")[2]);
      String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "indexof_start" + params + r2) + (String.valueOf(r1) + "indexof_start" + params + r2).length(), the_line.indexOf(String.valueOf(r1) + "indexof_end" + r2));
      if (loc < 0) {
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "indexof_start" + params + r2))) + inner.lastIndexOf(search, loc * -1) + the_line.substring(the_line.indexOf(String.valueOf(r1) + "indexof_end" + r2) + (String.valueOf(r1) + "indexof_end" + r2).length());
        continue;
      } 
      the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "indexof_start" + params + r2))) + inner.indexOf(search, loc) + the_line.substring(the_line.indexOf(String.valueOf(r1) + "indexof_end" + r2) + (String.valueOf(r1) + "indexof_end" + r2).length());
    } 
    while (the_line.indexOf(String.valueOf(r1) + "substring_start") >= 0 && the_line.indexOf(String.valueOf(r1) + "substring_end" + r2) >= 0) {
      String params = the_line.substring(the_line.indexOf(String.valueOf(r1) + "substring_start") + (String.valueOf(r1) + "substring_start").length());
      params = params.substring(0, params.indexOf("}"));
      int loc1 = Integer.parseInt(params.split(":")[1]);
      int loc2 = Integer.parseInt(params.split(":")[2]);
      String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "substring_start" + params + r2) + (String.valueOf(r1) + "substring_start" + params + r2).length(), the_line.indexOf(String.valueOf(r1) + "substring_end" + r2));
      if (loc2 < 0)
        loc2 = inner.length(); 
      the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "substring_start" + params + r2))) + inner.substring(loc1, loc2) + the_line.substring(the_line.indexOf(String.valueOf(r1) + "substring_end" + r2) + (String.valueOf(r1) + "substring_end" + r2).length());
    } 
    while (the_line.indexOf(String.valueOf(r1) + "split_start") >= 0 && the_line.indexOf(String.valueOf(r1) + "split_end" + r2) >= 0) {
      String params = the_line.substring(the_line.indexOf(String.valueOf(r1) + "split_start") + (String.valueOf(r1) + "split_start").length());
      params = params.substring(0, params.indexOf("}"));
      String loc1 = params.split(":")[1];
      loc1 = replace_str(loc1, "~..~", ":");
      int loc2 = Integer.parseInt(params.split(":")[2]);
      String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "split_start" + params + r2) + (String.valueOf(r1) + "split_start" + params + r2).length(), the_line.indexOf(String.valueOf(r1) + "split_end" + r2));
      the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "split_start" + params + r2))) + inner.split(loc1)[loc2] + the_line.substring(the_line.indexOf(String.valueOf(r1) + "split_end" + r2) + (String.valueOf(r1) + "split_end" + r2).length());
    } 
    while (the_line.indexOf(String.valueOf(r1) + "replace_start") >= 0 && the_line.indexOf(String.valueOf(r1) + "replace_end" + r2) >= 0) {
      String params = the_line.substring(the_line.indexOf(String.valueOf(r1) + "replace_start") + (String.valueOf(r1) + "replace_start").length());
      params = params.substring(0, params.indexOf("}"));
      String loc1 = params.split(":")[1];
      String loc2 = "";
      if ((params.split(":")).length > 2)
        loc2 = params.split(":")[2]; 
      String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "replace_start" + params + r2) + (String.valueOf(r1) + "replace_start" + params + r2).length(), the_line.lastIndexOf(String.valueOf(r1) + "replace_end" + r2));
      the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "replace_start" + params + r2))) + replace_str(inner, loc1, loc2) + the_line.substring(the_line.lastIndexOf(String.valueOf(r1) + "replace_end" + r2) + (String.valueOf(r1) + "replace_end" + r2).length());
    } 
    while (the_line.indexOf(String.valueOf(r1) + "increment_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "increment_end" + r2) >= 0) {
      String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "increment_start" + r2) + (String.valueOf(r1) + "increment_start" + r2).length(), the_line.indexOf(String.valueOf(r1) + "increment_end" + r2));
      the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "increment_start" + r2))) + (Integer.parseInt(inner.trim()) + 1) + the_line.substring(the_line.indexOf(String.valueOf(r1) + "increment_end" + r2) + (String.valueOf(r1) + "increment_end" + r2).length());
    } 
    while (the_line.indexOf(String.valueOf(r1) + "decrement_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "decrement_end" + r2) >= 0) {
      String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "decrement_start" + r2) + (String.valueOf(r1) + "decrement_start" + r2).length(), the_line.indexOf(String.valueOf(r1) + "decrement_end" + r2));
      the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "decrement_start" + r2))) + (Integer.parseInt(inner.trim()) - 1) + the_line.substring(the_line.indexOf(String.valueOf(r1) + "decrement_end" + r2) + (String.valueOf(r1) + "decrement_end" + r2).length());
    } 
    while (the_line.indexOf(String.valueOf(r1) + "add_start") >= 0 && the_line.indexOf(String.valueOf(r1) + "add_end" + r2) >= 0) {
      String params = the_line.substring(the_line.indexOf(String.valueOf(r1) + "add_start") + (String.valueOf(r1) + "add_start").length());
      params = params.substring(0, params.indexOf("}"));
      long add = Long.parseLong(params.split(":")[1]);
      String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "add_start" + params + r2) + (String.valueOf(r1) + "add_start" + params + r2).length(), the_line.indexOf(String.valueOf(r1) + "add_end" + r2));
      the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "add_start" + params + r2))) + (Long.parseLong(inner.trim()) + add) + the_line.substring(the_line.indexOf(String.valueOf(r1) + "add_end" + r2) + (String.valueOf(r1) + "add_end" + r2).length());
    } 
    while (the_line.indexOf(String.valueOf(r1) + "parse_start") >= 0 && the_line.indexOf(String.valueOf(r1) + "parse_end" + r2) >= 0) {
      String params = the_line.substring(the_line.indexOf(String.valueOf(r1) + "parse_start") + (String.valueOf(r1) + "parse_start").length());
      params = params.substring(0, params.indexOf("}"));
      String parse = params.substring(params.indexOf(":") + 1).trim();
      SimpleDateFormat sdf = new SimpleDateFormat(parse);
      String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "parse_start" + params + r2) + (String.valueOf(r1) + "parse_start" + params + r2).length(), the_line.indexOf(String.valueOf(r1) + "parse_end" + r2));
      the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "parse_start" + params + r2))) + sdf.parse(inner.trim()).getTime() + the_line.substring(the_line.indexOf(String.valueOf(r1) + "parse_end" + r2) + (String.valueOf(r1) + "parse_end" + r2).length());
    } 
    while (the_line.indexOf(String.valueOf(r1) + "math_start") >= 0 && the_line.indexOf(String.valueOf(r1) + "math_end" + r2) >= 0) {
      String params = the_line.substring(the_line.indexOf(String.valueOf(r1) + "math_start") + (String.valueOf(r1) + "math_start").length());
      params = params.substring(0, params.indexOf("}"));
      String result_type = "";
      if (params.indexOf(":") >= 0)
        result_type = params.substring(params.indexOf(":") + 1).trim(); 
      String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "math_start" + params + r2) + (String.valueOf(r1) + "math_start" + params + r2).length(), the_line.indexOf(String.valueOf(r1) + "math_end" + r2));
      String r = "";
      if (result_type.equalsIgnoreCase("d")) {
        r = (new StringBuffer(String.valueOf(Variables.eval_math(inner.trim())))).toString();
      } else if (result_type.equalsIgnoreCase("f")) {
        r = (new StringBuffer(String.valueOf((float)Variables.eval_math(inner.trim())))).toString();
      } else {
        r = (new StringBuffer(String.valueOf((int)Variables.eval_math(inner.trim())))).toString();
      } 
      the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "math_start" + params + r2))) + r + the_line.substring(the_line.indexOf(String.valueOf(r1) + "math_end" + r2) + (String.valueOf(r1) + "math_end" + r2).length());
    } 
    return the_line;
  }
  
  public static String textFunctions(String the_line, String r1, String r2) throws Exception {
    if (System.getProperty("crushftp.old.text", "false").equals("true"))
      return textFunctions_old(the_line, r1, r2); 
    boolean found = true;
    while (found) {
      found = false;
      if (the_line.indexOf(String.valueOf(r1) + "encrypt_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "encrypt_end" + r2) >= 0) {
        String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "encrypt_start" + r2) + (String.valueOf(r1) + "encrypt_start" + r2).length(), the_line.indexOf(String.valueOf(r1) + "encrypt_end" + r2));
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "encrypt_start" + r2))) + encryptDecrypt(inner, true) + the_line.substring(the_line.indexOf(String.valueOf(r1) + "encrypt_end" + r2) + (String.valueOf(r1) + "encrypt_end" + r2).length());
        found = true;
      } 
      if (the_line.indexOf(String.valueOf(r1) + "encode_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "encode_end" + r2) >= 0) {
        String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "encode_start" + r2) + (String.valueOf(r1) + "encode_start" + r2).length(), the_line.indexOf(String.valueOf(r1) + "encode_end" + r2));
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "encode_start" + r2))) + url_encode(inner) + the_line.substring(the_line.indexOf(String.valueOf(r1) + "encode_end" + r2) + (String.valueOf(r1) + "encode_end" + r2).length());
        found = true;
      } 
      if (the_line.indexOf(String.valueOf(r1) + "decode_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "decode_end" + r2) >= 0) {
        String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "decode_start" + r2) + (String.valueOf(r1) + "decode_start" + r2).length(), the_line.indexOf(String.valueOf(r1) + "decode_end" + r2));
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "decode_start" + r2))) + url_decode(inner) + the_line.substring(the_line.indexOf(String.valueOf(r1) + "decode_end" + r2) + (String.valueOf(r1) + "decode_end" + r2).length());
        found = true;
      } 
      if (the_line.indexOf(String.valueOf(r1) + "upper_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "upper_end" + r2) >= 0) {
        String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "upper_start" + r2) + (String.valueOf(r1) + "upper_start" + r2).length(), the_line.indexOf(String.valueOf(r1) + "upper_end" + r2));
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "upper_start" + r2))) + inner.toUpperCase() + the_line.substring(the_line.indexOf(String.valueOf(r1) + "upper_end" + r2) + (String.valueOf(r1) + "upper_end" + r2).length());
        found = true;
      } 
      if (the_line.indexOf(String.valueOf(r1) + "lower_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "lower_end" + r2) >= 0) {
        String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "lower_start" + r2) + (String.valueOf(r1) + "lower_start" + r2).length(), the_line.indexOf(String.valueOf(r1) + "lower_end" + r2));
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "lower_start" + r2))) + inner.toLowerCase() + the_line.substring(the_line.indexOf(String.valueOf(r1) + "lower_end" + r2) + (String.valueOf(r1) + "lower_end" + r2).length());
        found = true;
      } 
      if (the_line.indexOf(String.valueOf(r1) + "md5_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "md5_end" + r2) >= 0) {
        String end_str = String.valueOf(r1) + "md5_end" + r2;
        int end_pos = findEnd(String.valueOf(r1) + "md5_start", end_str, the_line);
        String inner = textFunctions(the_line.substring(the_line.indexOf(String.valueOf(r1) + "md5_start" + r2) + (String.valueOf(r1) + "md5_start" + r2).length(), end_pos), r1, r2);
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "md5_start" + r2))) + "MD5:" + getMD5(new ByteArrayInputStream(inner.getBytes("UTF8"))) + the_line.substring(end_pos + end_str.length());
        found = true;
      } 
      if (the_line.indexOf(String.valueOf(r1) + "chop_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "chop_end" + r2) >= 0) {
        String end_str = String.valueOf(r1) + "chop_end" + r2;
        int end_pos = findEnd(String.valueOf(r1) + "chop_start", end_str, the_line);
        String inner = textFunctions(the_line.substring(the_line.indexOf(String.valueOf(r1) + "chop_start" + r2) + (String.valueOf(r1) + "chop_start" + r2).length(), end_pos), r1, r2);
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "chop_start" + r2))) + all_but_last(inner) + the_line.substring(end_pos + end_str.length());
        found = true;
      } 
      if (the_line.indexOf(String.valueOf(r1) + "htmlclean_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "htmlclean_end" + r2) >= 0) {
        String end_str = String.valueOf(r1) + "htmlclean_end" + r2;
        int end_pos = findEnd(String.valueOf(r1) + "htmlclean_start", end_str, the_line);
        String inner = textFunctions(the_line.substring(the_line.indexOf(String.valueOf(r1) + "htmlclean_start" + r2) + (String.valueOf(r1) + "htmlclean_start" + r2).length(), end_pos), r1, r2);
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "htmlclean_start" + r2))) + inner.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", "") + the_line.substring(end_pos + end_str.length());
        found = true;
      } 
      if (the_line.indexOf(String.valueOf(r1) + "htmlclean2_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "htmlclean2_end" + r2) >= 0) {
        String end_str = String.valueOf(r1) + "htmlclean2_end" + r2;
        int end_pos = findEnd(String.valueOf(r1) + "htmlclean2_start", end_str, the_line);
        String inner = textFunctions(the_line.substring(the_line.indexOf(String.valueOf(r1) + "htmlclean2_start" + r2) + (String.valueOf(r1) + "htmlclean2_start" + r2).length(), end_pos), r1, r2);
        while (inner.indexOf("<") >= 0) {
          if (inner.indexOf("<") < 0 || inner.indexOf(">") < 0)
            break; 
          inner = String.valueOf(inner.substring(0, inner.indexOf("<"))) + inner.substring(inner.indexOf(">", inner.indexOf("<")) + 1);
        } 
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "htmlclean2_start" + r2))) + inner + the_line.substring(end_pos + end_str.length());
        found = true;
      } 
      if (the_line.indexOf(String.valueOf(r1) + "last_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "last_end" + r2) >= 0) {
        String end_str = String.valueOf(r1) + "last_end" + r2;
        int end_pos = findEnd(String.valueOf(r1) + "last_start", end_str, the_line);
        String inner = textFunctions(the_line.substring(the_line.indexOf(String.valueOf(r1) + "last_start" + r2) + (String.valueOf(r1) + "last_start" + r2).length(), end_pos), r1, r2);
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "last_start" + r2))) + last(inner) + the_line.substring(end_pos + end_str.length());
        found = true;
      } 
      if (the_line.indexOf(String.valueOf(r1) + "trim_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "trim_end" + r2) >= 0) {
        String end_str = String.valueOf(r1) + "trim_end" + r2;
        int end_pos = findEnd(String.valueOf(r1) + "trim_start", end_str, the_line);
        String inner = textFunctions(the_line.substring(the_line.indexOf(String.valueOf(r1) + "trim_start" + r2) + (String.valueOf(r1) + "trim_start" + r2).length(), end_pos), r1, r2);
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "trim_start" + r2))) + inner.trim() + the_line.substring(end_pos + end_str.length());
        found = true;
      } 
      if (the_line.indexOf(String.valueOf(r1) + "sql_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "sql_end" + r2) >= 0) {
        String end_str = String.valueOf(r1) + "sql_end" + r2;
        int end_pos = findEnd(String.valueOf(r1) + "sql_start", end_str, the_line);
        String inner = textFunctions(the_line.substring(the_line.indexOf(String.valueOf(r1) + "sql_start" + r2) + (String.valueOf(r1) + "sql_start" + r2).length(), end_pos), r1, r2);
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "sql_start" + r2))) + inner.replace('\'', '_').replace('"', '_').replace('%', '_').replace(';', ' ') + the_line.substring(end_pos + end_str.length());
        found = true;
      } 
      if (the_line.indexOf(String.valueOf(r1) + "indexof_start") >= 0 && the_line.indexOf(String.valueOf(r1) + "indexof_end" + r2) >= 0) {
        String end_str = String.valueOf(r1) + "indexof_end" + r2;
        int end_pos = findEnd(String.valueOf(r1) + "indexof_start", end_str, the_line);
        String params = the_line.substring(the_line.indexOf(String.valueOf(r1) + "indexof_start") + (String.valueOf(r1) + "indexof_start").length());
        params = params.substring(0, params.indexOf("}"));
        String search = params.split(":")[1];
        search = replace_str(search, "~..~", ":");
        int loc = Integer.parseInt(params.split(":")[2]);
        String inner = textFunctions(the_line.substring(the_line.indexOf(String.valueOf(r1) + "indexof_start" + params + r2) + (String.valueOf(r1) + "indexof_start" + params + r2).length(), end_pos), r1, r2);
        if (loc < 0) {
          the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "indexof_start" + params + r2))) + inner.lastIndexOf(search, (loc * -1 == 1) ? (inner.length() - 1) : (loc * -1)) + the_line.substring(end_pos + end_str.length());
        } else {
          the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "indexof_start" + params + r2))) + inner.indexOf(search, loc) + the_line.substring(end_pos + end_str.length());
        } 
        found = true;
      } 
      if (the_line.indexOf(String.valueOf(r1) + "substring_start") >= 0 && the_line.indexOf(String.valueOf(r1) + "substring_end" + r2) >= 0) {
        String end_str = String.valueOf(r1) + "substring_end" + r2;
        int end_pos = findEnd(String.valueOf(r1) + "substring_start", end_str, the_line);
        String params = the_line.substring(the_line.indexOf(String.valueOf(r1) + "substring_start") + (String.valueOf(r1) + "substring_start").length());
        params = params.substring(0, params.indexOf("}"));
        int loc1 = Integer.parseInt(params.split(":")[1]);
        int loc2 = Integer.parseInt(params.split(":")[2]);
        String inner = textFunctions(the_line.substring(the_line.indexOf(String.valueOf(r1) + "substring_start" + params + r2) + (String.valueOf(r1) + "substring_start" + params + r2).length(), end_pos), r1, r2);
        if (loc2 < 0)
          loc2 = inner.length(); 
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "substring_start" + params + r2))) + inner.substring(loc1, loc2) + the_line.substring(end_pos + end_str.length());
        found = true;
      } 
      if (the_line.indexOf(String.valueOf(r1) + "split_start") >= 0 && the_line.indexOf(String.valueOf(r1) + "split_end" + r2) >= 0) {
        String end_str = String.valueOf(r1) + "split_end" + r2;
        int end_pos = findEnd(String.valueOf(r1) + "split_start", end_str, the_line);
        String params = the_line.substring(the_line.indexOf(String.valueOf(r1) + "split_start") + (String.valueOf(r1) + "split_start").length());
        params = params.substring(0, params.indexOf("}"));
        String loc1 = params.split(":")[1];
        loc1 = replace_str(loc1, "~..~", ":");
        int loc2 = Integer.parseInt(params.split(":")[2]);
        String inner = textFunctions(the_line.substring(the_line.indexOf(String.valueOf(r1) + "split_start" + params + r2) + (String.valueOf(r1) + "split_start" + params + r2).length(), end_pos), r1, r2);
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "split_start" + params + r2))) + inner.split(loc1)[loc2] + the_line.substring(end_pos + end_str.length());
        found = true;
      } 
      if (the_line.indexOf(String.valueOf(r1) + "replace_start") >= 0 && the_line.indexOf(String.valueOf(r1) + "replace_end" + r2) >= 0) {
        String end_str = String.valueOf(r1) + "replace_end" + r2;
        int end_pos = findEnd(String.valueOf(r1) + "replace_start", end_str, the_line);
        String params = the_line.substring(the_line.indexOf(String.valueOf(r1) + "replace_start") + (String.valueOf(r1) + "replace_start").length());
        params = params.substring(0, params.indexOf("}"));
        String loc1 = params.split(":")[1];
        String loc2 = "";
        if ((params.split(":")).length > 2)
          loc2 = params.split(":")[2]; 
        String inner = textFunctions(the_line.substring(the_line.indexOf(String.valueOf(r1) + "replace_start" + params + r2) + (String.valueOf(r1) + "replace_start" + params + r2).length(), end_pos), r1, r2);
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "replace_start" + params + r2))) + replace_str(inner, loc1, loc2) + the_line.substring(end_pos + end_str.length());
        found = true;
      } 
      if (the_line.indexOf(String.valueOf(r1) + "increment_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "increment_end" + r2) >= 0) {
        String end_str = String.valueOf(r1) + "increment_end" + r2;
        int end_pos = findEnd(String.valueOf(r1) + "increment_start", end_str, the_line);
        String inner = textFunctions(the_line.substring(the_line.indexOf(String.valueOf(r1) + "increment_start" + r2) + (String.valueOf(r1) + "increment_start" + r2).length(), end_pos), r1, r2);
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "increment_start" + r2))) + (Integer.parseInt(inner.trim()) + 1) + the_line.substring(end_pos + end_str.length());
        found = true;
      } 
      if (the_line.indexOf(String.valueOf(r1) + "decrement_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "decrement_end" + r2) >= 0) {
        String end_str = String.valueOf(r1) + "decrement_end" + r2;
        int end_pos = findEnd(String.valueOf(r1) + "decrement_start", end_str, the_line);
        String inner = textFunctions(the_line.substring(the_line.indexOf(String.valueOf(r1) + "decrement_start" + r2) + (String.valueOf(r1) + "decrement_start" + r2).length(), end_pos), r1, r2);
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "decrement_start" + r2))) + (Integer.parseInt(inner.trim()) - 1) + the_line.substring(end_pos + end_str.length());
        found = true;
      } 
      if (the_line.indexOf(String.valueOf(r1) + "add_start") >= 0 && the_line.indexOf(String.valueOf(r1) + "add_end" + r2) >= 0) {
        String end_str = String.valueOf(r1) + "add_end" + r2;
        int end_pos = findEnd(String.valueOf(r1) + "add_start", end_str, the_line);
        String params = the_line.substring(the_line.indexOf(String.valueOf(r1) + "add_start") + (String.valueOf(r1) + "add_start").length());
        params = params.substring(0, params.indexOf("}"));
        long add = Long.parseLong(params.split(":")[1]);
        String inner = textFunctions(the_line.substring(the_line.indexOf(String.valueOf(r1) + "add_start" + params + r2) + (String.valueOf(r1) + "add_start" + params + r2).length(), end_pos), r1, r2);
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "add_start" + params + r2))) + (Long.parseLong(inner.trim()) + add) + the_line.substring(end_pos + end_str.length());
        found = true;
      } 
      if (the_line.indexOf(String.valueOf(r1) + "parse_start") >= 0 && the_line.indexOf(String.valueOf(r1) + "parse_end" + r2) >= 0) {
        String end_str = String.valueOf(r1) + "parse_end" + r2;
        int end_pos = findEnd(String.valueOf(r1) + "parse_start", end_str, the_line);
        String params = the_line.substring(the_line.indexOf(String.valueOf(r1) + "parse_start") + (String.valueOf(r1) + "parse_start").length());
        params = params.substring(0, params.indexOf("}"));
        String parse = params.substring(params.indexOf(":") + 1).trim();
        parse = replace_str(parse, "~..~", ":");
        SimpleDateFormat sdf = new SimpleDateFormat(parse);
        String inner = textFunctions(the_line.substring(the_line.indexOf(String.valueOf(r1) + "parse_start" + params + r2) + (String.valueOf(r1) + "parse_start" + params + r2).length(), end_pos), r1, r2);
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "parse_start" + params + r2))) + sdf.parse(inner.trim()).getTime() + the_line.substring(end_pos + end_str.length());
        found = true;
      } 
      if (the_line.indexOf(String.valueOf(r1) + "rparse_start") >= 0 && the_line.indexOf(String.valueOf(r1) + "rparse_end" + r2) >= 0) {
        String end_str = String.valueOf(r1) + "rparse_end" + r2;
        int end_pos = findEnd(String.valueOf(r1) + "rparse_start", end_str, the_line);
        String params = the_line.substring(the_line.indexOf(String.valueOf(r1) + "rparse_start") + (String.valueOf(r1) + "rparse_start").length());
        params = params.substring(0, params.indexOf("}"));
        String parse = params.substring(params.indexOf(":") + 1).trim();
        parse = replace_str(parse, "~..~", ":");
        SimpleDateFormat sdf = new SimpleDateFormat(parse);
        String inner = textFunctions(the_line.substring(the_line.indexOf(String.valueOf(r1) + "rparse_start" + params + r2) + (String.valueOf(r1) + "rparse_start" + params + r2).length(), end_pos), r1, r2);
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "rparse_start" + params + r2))) + sdf.format(new Date(Long.parseLong(inner.trim()))) + the_line.substring(end_pos + end_str.length());
        found = true;
      } 
      if (the_line.indexOf(String.valueOf(r1) + "url_start") >= 0 && the_line.indexOf(String.valueOf(r1) + "url_end" + r2) >= 0) {
        String end_str = String.valueOf(r1) + "url_end" + r2;
        int end_pos = findEnd(String.valueOf(r1) + "url_start", end_str, the_line);
        String params = the_line.substring(the_line.indexOf(String.valueOf(r1) + "url_start") + (String.valueOf(r1) + "url_start").length());
        params = params.substring(0, params.indexOf("}"));
        String part = params.substring(params.indexOf(":") + 1).trim();
        String inner = textFunctions(the_line.substring(the_line.indexOf(String.valueOf(r1) + "url_start" + params + r2) + (String.valueOf(r1) + "url_start" + params + r2).length(), end_pos), r1, r2);
        String part_item = "";
        if (part.equalsIgnoreCase("user")) {
          part_item = (new VRL(inner)).getUsername();
        } else if (part.equalsIgnoreCase("pass")) {
          part_item = (new VRL(inner)).getPassword();
        } else if (part.equalsIgnoreCase("path")) {
          part_item = (new VRL(inner)).getPath();
        } else if (part.equalsIgnoreCase("port")) {
          part_item = (new StringBuffer(String.valueOf((new VRL(inner)).getPort()))).toString();
        } else if (part.equalsIgnoreCase("host")) {
          part_item = (new VRL(inner)).getHost();
        } else if (part.equalsIgnoreCase("protocol")) {
          part_item = (new VRL(inner)).getProtocol();
        } else if (part.equalsIgnoreCase("file")) {
          part_item = (new VRL(inner)).getFile();
        } else if (part.equalsIgnoreCase("query")) {
          part_item = (new VRL(inner)).getQuery();
        } 
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "url_start" + params + r2))) + part_item + the_line.substring(end_pos + end_str.length());
        found = true;
      } 
      if (the_line.indexOf(String.valueOf(r1) + "length_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "length_end" + r2) >= 0) {
        String end_str = String.valueOf(r1) + "length_end" + r2;
        int end_pos = findEnd(String.valueOf(r1) + "length_start", end_str, the_line);
        String inner = textFunctions(the_line.substring(the_line.indexOf(String.valueOf(r1) + "length_start" + r2) + (String.valueOf(r1) + "length_start" + r2).length(), end_pos), r1, r2);
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "length_start" + r2))) + inner.length() + the_line.substring(end_pos + end_str.length());
        found = true;
      } 
      if (the_line.indexOf(String.valueOf(r1) + "math_start") >= 0 && the_line.indexOf(String.valueOf(r1) + "math_end" + r2) >= 0) {
        String end_str = String.valueOf(r1) + "math_end" + r2;
        int end_pos = findEnd(String.valueOf(r1) + "math_start", end_str, the_line);
        String params = the_line.substring(the_line.indexOf(String.valueOf(r1) + "math_start") + (String.valueOf(r1) + "math_start").length());
        params = params.substring(0, params.indexOf("}"));
        String result_type = "";
        if (params.indexOf(":") >= 0)
          result_type = params.substring(params.indexOf(":") + 1).trim(); 
        String inner = textFunctions(the_line.substring(the_line.indexOf(String.valueOf(r1) + "math_start" + params + r2) + (String.valueOf(r1) + "math_start" + params + r2).length(), end_pos), r1, r2);
        String r = "";
        if (result_type.equalsIgnoreCase("d")) {
          r = (new StringBuffer(String.valueOf(Variables.eval_math(inner.trim())))).toString();
        } else if (result_type.equalsIgnoreCase("f")) {
          r = (new StringBuffer(String.valueOf((float)Variables.eval_math(inner.trim())))).toString();
        } else {
          r = (new StringBuffer(String.valueOf((int)Variables.eval_math(inner.trim())))).toString();
        } 
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "math_start" + params + r2))) + r + the_line.substring(end_pos + end_str.length());
        found = true;
      } 
    } 
    return the_line;
  }
  
  public static int findEnd(String start, String end, String the_line) {
    int depth = 0;
    for (int x = 0; x < the_line.length(); x++) {
      if (the_line.substring(x).startsWith(start)) {
        depth++;
      } else {
        if (the_line.substring(x).startsWith(end) && depth == 1)
          return x; 
        if (the_line.substring(x).startsWith(end))
          depth--; 
      } 
    } 
    return -1;
  }
  
  static long lastIpLookup = 0L;
  
  static String lastLocalIP = "127.0.0.1";
  
  static Class class$0;
  
  static Class class$1;
  
  public static String getLocalIP() {
    if (System.currentTimeMillis() - lastIpLookup < 60000L)
      return lastLocalIP; 
    lastIpLookup = System.currentTimeMillis();
    String ip = "127.0.0.1";
    try {
      try {
        ip = InetAddress.getLocalHost().getHostAddress();
        lastLocalIP = ip;
      } catch (Exception e) {
        Tunnel2.msg(e);
      } 
      if (!ip.startsWith("127.0"))
        return ip; 
      Enumeration en = NetworkInterface.getNetworkInterfaces();
      while (en.hasMoreElements()) {
        NetworkInterface i = en.nextElement();
        Enumeration en2 = i.getInetAddresses();
        while (en2.hasMoreElements()) {
          InetAddress addr = en2.nextElement();
          if (!addr.isLoopbackAddress() && addr instanceof java.net.Inet4Address && !addr.getHostAddress().startsWith("127.0")) {
            ip = addr.getHostAddress();
            lastLocalIP = ip;
            return ip;
          } 
          if (!addr.isLoopbackAddress() && addr instanceof java.net.Inet4Address)
            ip = addr.getHostAddress(); 
        } 
      } 
    } catch (Exception e) {
      Tunnel2.msg(e);
    } 
    lastLocalIP = ip;
    return ip;
  }
  
  public static Object CLONE(Object o) {
    try {
      byte[] b = CLONE1(o);
      o = CLONE2(b);
    } catch (Exception exception) {}
    return o;
  }
  
  public static String getContentType(String s) {
    s = s.toUpperCase();
    if (s.endsWith(".ZIP"))
      return "application/zip"; 
    if (s.endsWith(".JPG"))
      return "image/jpeg"; 
    if (s.endsWith(".JPEG"))
      return "image/jpeg"; 
    if (s.endsWith(".GIF"))
      return "image/gif"; 
    if (s.endsWith(".PNG"))
      return "image/png"; 
    if (s.endsWith(".BMP"))
      return "image/bmp"; 
    if (s.endsWith(".HTML"))
      return "text/html"; 
    if (s.endsWith(".HTML"))
      return "text/html"; 
    if (s.endsWith(".CSS"))
      return "text/css"; 
    if (s.endsWith(".JS"))
      return "text/javascript"; 
    if (s.endsWith(".XML"))
      return "text/xml"; 
    if (s.endsWith(".TXT"))
      return "text/plain"; 
    if (s.endsWith(".PDF"))
      return "applciation/pdf"; 
    if (s.endsWith(".SWF"))
      return "applciation/x-shockwave-flash"; 
    if (s.endsWith(".WOFF"))
      return "application/font-woff"; 
    if (s.endsWith(".WOFF2"))
      return "application/font-woff"; 
    return "applciation/binary";
  }
  
  public static byte[] CLONE1(Object o) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      ObjectOutputStream tempOut = new ObjectOutputStream(baos);
      tempOut.reset();
      tempOut.writeObject(o);
      tempOut.flush();
      tempOut.close();
    } catch (Exception exception) {}
    return baos.toByteArray();
  }
  
  public static Object CLONE2(byte[] b) {
    Object o = null;
    try {
      ObjectInputStream tempIn = new ObjectInputStream(new ByteArrayInputStream(b));
      o = tempIn.readObject();
      tempIn.close();
    } catch (Exception exception) {}
    return o;
  }
  
  public static void activateFront() {
    if (machine_is_x())
      try {
        (new ScriptEngineManager()).getEngineByName("AppleScript").eval("tell me to activate");
      } catch (Exception e) {
        e.printStackTrace();
      }  
  }
  
  public static String dumpStack(String info) {
    String result = "Server Memory Stats: Max=" + format_bytes_short(Runtime.getRuntime().maxMemory()) + ", Free=" + format_bytes_short(Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory()) + "\r\n";
    Set threadSet = Thread.getAllStackTraces().keySet();
    Thread[] threads1 = threadSet.<Thread>toArray(new Thread[threadSet.size()]);
    Thread[] threads2 = new Thread[threads1.length];
    String[] tmp = new String[threads1.length];
    int x;
    for (x = 0; x < threads1.length; x++)
      tmp[x] = String.valueOf(threads1[x].getName()) + "!~!" + x; 
    Arrays.sort((Object[])tmp);
    for (x = 0; x < tmp.length; x++)
      threads2[x] = threads1[Integer.parseInt(tmp[x].split("!~!")[1])]; 
    result = String.valueOf(result) + info + "\r\n";
    result = String.valueOf(result) + "#######################Start Thread Dump#######################\r\n";
    for (x = 0; x < threads2.length; x++) {
      String data = "---------------------------------------------------------------------------------------------------";
      result = String.valueOf(result) + data + "\r\n";
      data = threads2[x].getName();
      result = String.valueOf(result) + data + "\r\n";
      StackTraceElement[] ste = threads2[x].getStackTrace();
      for (int xx = 0; xx < ste.length; xx++) {
        data = "\t\t" + ste[xx].getClassName() + "." + ste[xx].getMethodName() + ":" + ste[xx].getLineNumber();
        result = String.valueOf(result) + data + "\r\n";
      } 
    } 
    result = String.valueOf(result) + "#######################End Thread Dump#######################\r\n";
    System.out.println(result);
    return result;
  }
}
