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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
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
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.SecretKeySpec;
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
import org.apache.commons.codec.binary.Base32;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

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
  
  public static long mimesModified = 0L;
  
  public static Properties mimes = new Properties();
  
  public static boolean dmz_mode = false;
  
  public static char[] encryption_password = "crushftp".toCharArray();
  
  private static Properties smtp_access_tokens = new Properties();
  
  static long UID_GLOBAL = System.currentTimeMillis();
  
  static {
    rn.nextBytes((String.valueOf(Runtime.getRuntime().maxMemory()) + Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory()).getBytes());
  }
  
  public static synchronized long uidg() {
    while (System.currentTimeMillis() == UID_GLOBAL) {
      try {
        Thread.sleep(1L);
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
        if (url.indexOf("@") >= 0) {
          int loc = url.indexOf(",", url.indexOf("@"));
          if (loc < 0)
            loc = url.indexOf(", "); 
          url = url.substring(0, loc).trim();
        } else {
          url = url.substring(0, url.indexOf(",")).trim();
        }  
      try {
        VRL vrl = new VRL(url);
        String url2 = String.valueOf(vrl.getProtocol()) + "://" + vrl.getUsername() + ":********" + "@" + vrl.getHost() + ":" + vrl.getPort() + vrl.getPath();
        s = replace_str(s, url, url2);
      } catch (Exception exception) {}
    } else if (s.indexOf("://") >= 0 && s.indexOf("@", s.indexOf("://")) >= 0) {
      int colslashslash = s.indexOf("://");
      int at = s.indexOf("@", colslashslash);
      String inner = s.substring(colslashslash, at);
      if (inner.indexOf(":") >= 0)
        s = replace_str(s, inner, String.valueOf(inner.substring(0, inner.indexOf(":") + 1)) + "************"); 
    } 
    if (s.indexOf("password=") >= 0) {
      String url = s.substring(s.indexOf("password=") + 9).trim();
      if (url.indexOf(",") >= 0)
        if (url.indexOf("@") >= 0) {
          int loc = url.indexOf(",", url.indexOf("@"));
          if (loc < 0)
            loc = url.indexOf(", "); 
          url = url.substring(0, loc).trim();
        } else {
          url = url.substring(0, url.indexOf(",")).trim();
        }  
      s = replace_str(s, url, "***********");
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
      if (level >= 2)
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
    if (KEYSTORE.equals("PKCS11"))
      return getFips(); 
    if (KEYSTORE.endsWith("cacerts") && needClientAuth)
      needClientAuth = false; 
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
          if (!(new File_S(KEYSTORE)).exists()) {
            log("SERVER", 0, "Couldn't find keystore " + KEYSTORE + ", ignoring it.");
            keystore.load(getClass().getResource("/assets/builtin").openStream(), "crushftp".toCharArray());
            KEYSTORE = "builtin";
          } else {
            in = new FileInputStream(new File_S(KEYSTORE));
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
            if (!(new File_S(TRUSTSTORE)).exists()) {
              log("SERVER", 0, "Couldn't find truststore " + TRUSTSTORE + ", ignoring it.");
              truststore.load(getClass().getResource("/assets/builtin").openStream(), "crushftp".toCharArray());
              TRUSTSTORE = "builtin";
            } else {
              in = new FileInputStream(new File_S(TRUSTSTORE));
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
  
  private static SSLContext getFips() throws Exception {
    Class c = Thread.currentThread().getContextClassLoader().loadClass("com.sun.net.ssl.internal.ssl.Provider");
    if (class$0 == null)
      try {
      
      } catch (ClassNotFoundException classNotFoundException) {
        throw new NoClassDefFoundError(null.getMessage());
      }  
    false[class$0] = class$0 = Class.forName("java.lang.String");
    Constructor cons = (new Class[1]).getConstructor(new Class[1]);
    cons.newInstance(new Object[] { "SunPKCS11-NSS" });
    String keystoreFormat = "PKCS11";
    KeyStore keystore = KeyStore.getInstance(keystoreFormat);
    keystore.load(null, "".toCharArray());
    KeyStore truststore = KeyStore.getInstance(keystoreFormat);
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(keystore);
    SSLContext sslc = SSLContext.getInstance("TLS");
    kmf.init(null, "".toCharArray());
    sslc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), SecureRandom.getInstance("PKCS11"));
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
    } else {
      type = replace_str(type, "{localPort}", p.getProperty("localPort"));
      type = replace_str(type, "%localPort%", p.getProperty("localPort"));
      s = type.split(";");
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
  
  public static int getRandomInt() {
    return rn.nextInt();
  }
  
  public static String dots(String s) {
    boolean uncFix = (s.indexOf(":////") > 0);
    s = s.replace('\\', '/');
    String s2 = "";
    while (s.indexOf("%") >= 0 && !s.equals(s2)) {
      s2 = s;
      s = url_decode(s);
      s = s.replace('\\', '/');
    } 
    if (s.startsWith("../"))
      s = s.substring(2); 
    if (s.endsWith(".."))
      s = String.valueOf(s) + "/"; 
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
    if (s.toLowerCase().indexOf("s3:/") < 0 && s.toLowerCase().indexOf("s3crush:/") < 0) {
      if (!uncFix)
        s = replace_str(s, "://", ":\\~~\\~~"); 
      boolean unc = s.startsWith("////");
      while (s.indexOf("//") >= 0)
        s = replace_str(s, "//", "/"); 
      if (unc)
        s = "///" + s; 
      if (!uncFix)
        s = replace_str(s, ":\\~~\\~~", "://"); 
    } 
    if (uncFix)
      s = String.valueOf(s.substring(0, s.indexOf(":") + 1)) + "///" + s.substring(s.indexOf(":") + 1); 
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
  
  public static String first(String item) {
    item = item.replace('\\', '/');
    item = item.substring(0, item.indexOf("/", 1));
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
  
  public static ServerSocket getSSLServerSocket(int serverPort, String listen_ip, boolean acceptAnyCert, String keystore_path, String keystore_pass, String keystore_key_pass) throws Exception {
    SSLContext sslc = SSLContext.getInstance("TLS");
    KeyStore keystore = KeyStore.getInstance("JKS");
    InputStream keystore_in = null;
    if (keystore_path == null || keystore_path.equals("") || keystore_path.equalsIgnoreCase("builtin")) {
      if (class$1 == null)
        try {
        
        } catch (ClassNotFoundException classNotFoundException) {
          throw new NoClassDefFoundError(null.getMessage());
        }  
      keystore_in = (class$1 = Class.forName("com.crushftp.client.Common")).getResource("/assets/builtin").openStream();
    } else {
      keystore_in = new FileInputStream(keystore_path);
    } 
    keystore.load(keystore_in, keystore_pass.toCharArray());
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(keystore, keystore_key_pass.toCharArray());
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
    streamCopier(null, null, in, out, true, true, true);
  }
  
  public static void streamCopier(InputStream in, OutputStream out, boolean async) throws InterruptedException {
    streamCopier(null, null, in, out, async, true, true);
  }
  
  public static void streamCopier(InputStream in, OutputStream out, boolean async, boolean closeInput, boolean closeOutput) throws InterruptedException {
    streamCopier(null, null, in, out, async, closeInput, closeOutput);
  }
  
  public static void streamCopier(Socket sock1, Socket sock2, InputStream in, OutputStream out, boolean async, boolean closeInput, boolean closeOutput) throws InterruptedException {
    Runnable r = new Runnable(in, out, closeInput, closeOutput, sock1, sock2) {
        private final InputStream val$in;
        
        private final OutputStream val$out;
        
        private final boolean val$closeInput;
        
        private final boolean val$closeOutput;
        
        private final Socket val$sock1;
        
        private final Socket val$sock2;
        
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
            if (e.getMessage() == null || (!e.getMessage().equalsIgnoreCase("Socket closed") && !e.getMessage().equalsIgnoreCase("Connection reset")))
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
            if (this.val$closeInput && this.val$closeOutput) {
              try {
                if (this.val$sock1 != null)
                  this.val$sock1.close(); 
              } catch (Exception exception) {}
              try {
                if (this.val$sock2 != null)
                  this.val$sock2.close(); 
              } catch (Exception exception) {}
            } 
          } 
        }
      };
    try {
      if (async) {
        Worker.startWorker(r);
      } else {
        r.run();
      } 
    } catch (Exception e) {
      log("SERVER", 0, e);
    } 
  }
  
  public static void copyStreams(InputStream in, Object out, boolean closeInput, boolean closeOutput) throws IOException {
    RandomAccessFile raf = null;
    BufferedOutputStream outStream = null;
    try {
      if (out instanceof RandomAccessFile) {
        raf = (RandomAccessFile)out;
      } else {
        outStream = new BufferedOutputStream((OutputStream)out);
      } 
      BufferedInputStream inStream = new BufferedInputStream(in);
      byte[] b = new byte[32768];
      int bytesRead = 0;
      while (bytesRead >= 0) {
        bytesRead = inStream.read(b);
        if (bytesRead > 0) {
          if (raf != null) {
            raf.write(b, 0, bytesRead);
            continue;
          } 
          outStream.write(b, 0, bytesRead);
        } 
      } 
      if (raf == null)
        outStream.flush(); 
    } finally {
      if (closeInput)
        in.close(); 
      if (closeOutput)
        if (raf != null) {
          raf.close();
        } else {
          outStream.close();
        }  
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
    String currentAuth = "";
    String ca = null;
    for (int x = 0; x < 100 && ca == null; x++) {
      try {
        String header = urlc.getHeaderField(x);
        if (header != null) {
          if (header.indexOf("currentAuth") >= 0)
            currentAuth = header; 
          if (header.indexOf("CrushAuth") >= 0)
            ca = header; 
        } 
      } catch (Exception exception) {}
    } 
    if (ca != null) {
      ca = ca.substring(ca.indexOf("CrushAuth=") + "CrushAuth=".length(), ca.indexOf(";", ca.indexOf("CrushAuth="))).trim();
    } else if (currentAuth != null && currentAuth.indexOf("currentAuth=") >= 0) {
      String c2f = currentAuth.substring(currentAuth.indexOf("currentAuth=") + "currentAuth=".length(), currentAuth.indexOf(";", currentAuth.indexOf("currentAuth="))).trim();
      urlc = (HttpURLConnection)(new URL(url)).openConnection();
      urlc.setRequestMethod("POST");
      urlc.setUseCaches(false);
      urlc.setDoOutput(true);
      urlc.getOutputStream().write(("command=getCrushAuth&c2f=" + c2f).getBytes("UTF8"));
      urlc.getResponseCode();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      streamCopier(urlc.getInputStream(), baos, false, true, true);
      ca = new String(baos.toByteArray());
      ca = ca.substring(ca.indexOf("<auth>") + "<auth>".length(), ca.indexOf("</auth>")).trim();
      ca = ca.substring("CrushAuth=".length());
    } 
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
    return getClientSingle(url, logHeader, logQueue);
  }
  
  public static GenericClient getClientReplication(String real_url, String logHeader, Vector logQueue) {
    GenericClient c = getClientSingle(getBaseUrl(real_url), logHeader, logQueue);
    if (c == null) {
      if (logQueue != null)
        logQueue.addElement("URL not understood:" + real_url); 
      log("SERVER", 1, "URL not understood:" + real_url);
    } 
    if (!System.getProperty("crushftp.replicated_vfs_root_url", "").equals("")) {
      Vector clients = new Vector();
      Vector vItems = new Vector();
      Properties originalvItem = new Properties();
      if (!real_url.endsWith("/"))
        real_url = all_but_last(real_url); 
      originalvItem.put("url", real_url);
      VRL root_vrl = new VRL(System.getProperty("crushftp.replicated_vfs_root_url"));
      String[] vrls = System.getProperty("crushftp.replicated_vfs_url").split(",");
      System.getProperties().put("crushftp.replicated_vfs", "true");
      clients.addElement(c);
      vItems.addElement(originalvItem);
      if (real_url.toUpperCase().startsWith(System.getProperty("crushftp.replicated_vfs_root_url", "").toUpperCase()))
        for (int x = 0; x < vrls.length; x++) {
          if (!vrls[x].trim().equals("")) {
            VRL vrl = new VRL(vrls[x].trim());
            String relative_path = (new VRL(originalvItem.getProperty("url"))).toString().substring(root_vrl.toString().length());
            Properties vItem = new Properties();
            VRL vrl2 = null;
            try {
              vrl2 = new VRL(String.valueOf(vrl.getProtocol()) + "://" + VRL.vrlEncode(System.getProperty("crushftp.replicated_vfs_user")) + ":" + VRL.vrlEncode(encryptDecrypt(System.getProperty("crushftp.replicated_vfs_pass"), false)) + "@" + vrl.getHost() + ":" + vrl.getPort() + vrl.getPath() + relative_path);
              vItem.put("url", String.valueOf(vrl.getProtocol()) + "://" + VRL.vrlEncode(System.getProperty("crushftp.replicated_vfs_user")) + ":" + VRL.vrlEncode(encryptDecrypt(System.getProperty("crushftp.replicated_vfs_pass"), false)) + "@" + vrl.getHost() + ":" + vrl.getPort() + vrl.getPath() + relative_path);
            } catch (Exception e) {
              log("SERVER", 1, e);
            } 
            GenericClient c2 = getClientSingle(getBaseUrl(vItem.getProperty("url")), logHeader, logQueue);
            c2.setConfig("replicated_login_user", vrl2.getUsername());
            c2.setConfig("replicated_login_pass", vrl2.getPassword());
            clients.addElement(c2);
            vItems.addElement(vItem);
          } 
        }  
      if (clients.size() > 1)
        c = new GenericClientMulti(logHeader, log, originalvItem, vItems, clients, System.getProperty("crushftp.replicated_auto_play_journal").equals("true")); 
    } 
    return c;
  }
  
  public static GenericClient getClientSingle(String url, String logHeader, Vector logQueue) {
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
    if ((url.toUpperCase().startsWith("HTTP:") || url.toUpperCase().startsWith("HTTPS:")) && logHeader != null && logHeader.indexOf("CACHED") >= 0)
      return new HTTPBufferedClient(url, logHeader, logQueue); 
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
    if (url.toUpperCase().startsWith("GSTORAGE:"))
      return new GStorageClient(url, logHeader, logQueue); 
    if (url.toUpperCase().startsWith("CITRIX:"))
      return new CitrixClient(url, logHeader, logQueue); 
    if (url.toUpperCase().startsWith("SMB:"))
      return new SMBClient(url, logHeader, logQueue); 
    if (url.toUpperCase().startsWith("SMB3:"))
      return new SMB3Client(url, logHeader, logQueue); 
    if (url.toUpperCase().startsWith("HADOOP:"))
      return new HadoopClient(url, logHeader, logQueue); 
    if (url.toUpperCase().startsWith("AZURE:"))
      return new AzureClient(url, logHeader, logQueue); 
    if (url.toUpperCase().startsWith("GLACIER:"))
      return new GlacierClient(url, logHeader, logQueue); 
    if (url.toUpperCase().startsWith("DROPBOX:"))
      return new DropBoxClient(url, logHeader, logQueue); 
    if (url.toUpperCase().startsWith("B2:"))
      return new B2Client(url, logHeader, logQueue); 
    if (url.toUpperCase().startsWith("CUSTOM."))
      return new CustomClient(url, logHeader, logQueue); 
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
  
  public static Socket grabDataSock(Properties requestSock) throws Exception {
    Vector data_sock_available = (Vector)System2.get("crushftp.dmz.data_sock_available");
    Object data_sock_available_lock = System2.get("crushftp.dmz.data_sock_available_lock");
    if (data_sock_available == null) {
      data_sock_available = (Vector)System2.get("crushftp.dmz.queue.sock");
      data_sock_available.addElement(requestSock);
      long start = System.currentTimeMillis();
      int wait = 10;
      while (System.currentTimeMillis() - start < 30000L) {
        try {
          if (requestSock.containsKey("socket"))
            break; 
          Thread.sleep(wait);
          if (wait < 100)
            wait += 10; 
        } catch (InterruptedException interruptedException) {}
      } 
      data_sock_available.remove(requestSock);
      if (requestSock.get("socket") == null)
        throw new IOException("failure: Waited 30 seconds for DMZ socket, giving up (v8)."); 
      return (Socket)requestSock.remove("socket");
    } 
    if (System2.get("crushftp.dmz.socket_lookup") != null) {
      Properties sockets = getConnectedSocks(false);
      Socket queue_sock = (Socket)sockets.remove("sock1");
      Socket client_sock = (Socket)sockets.remove("sock2");
      Properties socket_lookup = (Properties)System2.get("crushftp.dmz.socket_lookup");
      Vector qwrite_queue = (Vector)System2.get("crushftp.dmz.qwrite_queue");
      Properties sock_info = new Properties();
      sock_info.put("socket", queue_sock);
      sock_info.put("out", queue_sock.getOutputStream());
      socket_lookup.put(queue_sock, sock_info);
      Properties p = new Properties();
      p.put("command", "create");
      p.put("sock", queue_sock);
      p.put("port", requestSock.getProperty("port"));
      qwrite_queue.addElement(p);
      startQueueSocket(queue_sock, (String)queue_sock);
      return client_sock;
    } 
    Socket use_this_sock = null;
    synchronized (data_sock_available_lock) {
      int x = data_sock_available.size() - 1;
      if (x >= 0) {
        Properties p = data_sock_available.elementAt(x);
        long time = Long.parseLong(p.getProperty("time"));
        Socket sock = (Socket)p.get("sock");
        sockLog(sock, "data sock being used:" + (System.currentTimeMillis() - time) + "ms old.  data_sock_available size=" + data_sock_available.size());
        data_sock_available.remove(p);
        use_this_sock = sock;
      } 
    } 
    if (use_this_sock != null) {
      use_this_sock.setSoTimeout(0);
      use_this_sock.getOutputStream().write((String.valueOf(requestSock.getProperty("port")) + "                                                                                                           ").substring(0, 100).getBytes());
      use_this_sock.getOutputStream().flush();
      sockLog(use_this_sock, "data sock loop used..alerting waiter.  data_sock_available size=" + data_sock_available.size());
    } 
    return use_this_sock;
  }
  
  public static void startQueueSocket(Socket sock, String sock_name) {
    try {
      Worker.startWorker(new Runnable(sock, sock_name) {
            private final Socket val$sock;
            
            private final String val$sock_name;
            
            public void run() {
              Vector qwrite_queue = (Vector)Common.System2.get("crushftp.dmz.qwrite_queue");
              int bytes_read = 0;
              try {
                byte[] b1 = new byte[1048576];
                InputStream in = this.val$sock.getInputStream();
                while (bytes_read >= 0) {
                  bytes_read = in.read(b1);
                  if (bytes_read >= 0) {
                    byte[] b2 = new byte[bytes_read];
                    System.arraycopy(b1, 0, b2, 0, bytes_read);
                    Properties properties = new Properties();
                    properties.put("command", "data");
                    properties.put("sock", this.val$sock_name);
                    properties.put("b", b2);
                    properties.put("len", (new StringBuffer(String.valueOf(bytes_read))).toString());
                    while (qwrite_queue.size() > 5000)
                      Thread.sleep(100L); 
                    qwrite_queue.addElement(properties);
                    continue;
                  } 
                  Properties p = new Properties();
                  p.put("command", "close");
                  p.put("sock", this.val$sock_name);
                  qwrite_queue.addElement(p);
                } 
              } catch (IOException e) {
                Common.log("SERVER", 2, e);
              } catch (InterruptedException interruptedException) {}
              try {
                this.val$sock.close();
              } catch (IOException e) {
                e.printStackTrace();
              } 
            }
          });
    } catch (IOException e) {
      log("SERVER", 1, e);
    } 
  }
  
  public static Socket getSocket(String protocol, VRL u, String use_dmz, String sticky_token) throws IOException {
    return getSocket(protocol, u, use_dmz, sticky_token, 0);
  }
  
  public static Socket getSocket(String protocol, VRL u, String use_dmz, String sticky_token, int timeout) throws IOException {
    if (use_dmz.toLowerCase().startsWith("variable:"))
      use_dmz = use_dmz.substring("variable:".length()); 
    if ((System2.containsKey("crushftp.dmz.queue.sock") || System2.containsKey("crushftp.dmz.data_sock_available")) && (use_dmz.equals("") || use_dmz.equalsIgnoreCase("false") || use_dmz.equalsIgnoreCase("null") || use_dmz.equalsIgnoreCase("no") || use_dmz.startsWith("socks://") || use_dmz.startsWith("http://") || use_dmz.equalsIgnoreCase("internal://"))) {
      Vector socket_queue = (Vector)System2.get("crushftp.dmz.queue.sock");
      if (socket_queue == null)
        socket_queue = (Vector)System2.get("crushftp.dmz.queue"); 
      log("DMZ", 2, "GET:SOCKET:Requesting socket connection from internal server out of the pool using port:" + u.getPort());
      Properties mySock = new Properties();
      mySock.put("type", "GET:SOCKET");
      mySock.put("port", (new StringBuffer(String.valueOf(u.getPort()))).toString());
      if (use_dmz.equalsIgnoreCase("internal://"))
        mySock.put("port", String.valueOf(u.getHost()) + ":" + u.getPort()); 
      mySock.put("data", new Properties());
      mySock.put("id", String.valueOf(makeBoundary(10)) + (new Date()).getTime());
      mySock.put("sticky_token", sticky_token);
      long start_wait = System.currentTimeMillis();
      mySock.put("created", (new StringBuffer(String.valueOf(start_wait))).toString());
      mySock.put("need_response", "true");
      long start = System.currentTimeMillis();
      int wait = 10;
      Vector data_sock_available = (Vector)System2.get("crushftp.dmz.data_sock_available");
      while (System.currentTimeMillis() - start < 30000L) {
        try {
          Socket socket = grabDataSock(mySock);
          if (socket != null) {
            mySock.put("socket", socket);
            break;
          } 
          Thread.sleep(wait);
          if (wait < 100)
            wait += 10; 
        } catch (Exception exception) {}
        if (data_sock_available != null && System.currentTimeMillis() - start > 5000L && System.currentTimeMillis() - start < 5500L)
          log("SERVER", 2, "DMZ is bored waiting for sockets from the internal server...data_sock_available size=" + data_sock_available.size()); 
      } 
      if (mySock.get("socket") == null)
        throw new IOException("failure: Waited 30 seconds for DMZ socket, giving up."); 
      Socket sock = (Socket)mySock.remove("socket");
      sockLog(sock, "Waited for DMZ socket:" + (System.currentTimeMillis() - start_wait) + "ms");
      return sock;
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
              if (!sb.toString().trim().startsWith("200") && !sb.toString().trim().startsWith("HTTP/1.1 200") && !sb.toString().trim().startsWith("HTTP/1.0 200")) {
                sock.close();
                throw new IOException(sb.toString());
              } 
            } 
            sockLog(sock, "Using socket from pool:" + use_dmz);
          } else {
            String host = u.getHost();
            if (host.indexOf("~") >= 0) {
              sock = new Socket(host.split("~")[1], u.getPort(), InetAddress.getByName(host.split("~")[0]), 0);
            } else if (timeout == 0) {
              sock = new Socket(host, u.getPort());
            } else {
              sock = new Socket();
              sock.connect(new InetSocketAddress(host, u.getPort()), timeout);
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
    try {
      sockLog(sock, "Disconnect " + u.getHost() + " / socktimeout=" + socketTimeout);
      sock.close();
    } catch (IOException e) {
      log("SERVER", 1, e);
    } 
  }
  
  public static long getDecryptedSize(String the_file_path) throws Exception {
    File_S f = new File_S(the_file_path);
    if (f.isDirectory())
      return -1L; 
    RandomAccessFile in = new RandomAccessFile(f, "r");
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
    return (new File_S(the_file_path)).length();
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
  
  public static void generateKeyPair(String privateKeyPath, int keySize, int days, String password, String commonName, String[] cyphers) throws IOException {
    String[] hashingAlgorithms = { "SHA1", "SHA256", "SHA384", "SHA512", "MD5" };
    String[] compressions = { "ZIP", "ZLIB", "UNCOMPRESSED" };
    try {
      PGPKeyPair pGPKeyPair = PGPKeyPair.generateKeyPair(keySize, commonName, "RSA", password, compressions, hashingAlgorithms, cyphers, days);
      pGPKeyPair.exportPrivateKey(privateKeyPath, true);
      pGPKeyPair.exportPublicKey(String.valueOf(privateKeyPath.substring(0, privateKeyPath.lastIndexOf("."))) + ".pub", true);
    } catch (Exception e) {
      throw new IOException(e);
    } 
  }
  
  public static void generateKeyPair(String privateKeyPath, int keySize, int days, String password, String commonName) throws IOException {
    String[] cyphers = { "CAST5", "AES_128", "AES_192", "AES_256", "TWOFISH" };
    generateKeyPair(privateKeyPath, keySize, days, password, commonName, cyphers);
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
    } else {
      if (s.toUpperCase().startsWith("GLACIER:/") && !s.endsWith("/"))
        s = String.valueOf(s) + "/"; 
      if (s.indexOf("@") > 0 && s.indexOf(":") != s.lastIndexOf(":") && !s.toLowerCase().startsWith("file:") && !s.toLowerCase().startsWith("azure:")) {
        if (s.lastIndexOf("@") > s.lastIndexOf(":") && s.indexOf("@") == s.lastIndexOf("@")) {
          s = s.substring(0, s.indexOf("/", s.lastIndexOf("@")) + 1);
        } else {
          s = s.substring(0, s.indexOf("/", s.indexOf(":", s.indexOf(":", 8)) + 1) + 1);
        } 
      } else if (s.toLowerCase().startsWith("file:/") && !s.toLowerCase().startsWith("file://")) {
        s = s.substring(0, s.indexOf("/", s.indexOf(":") + 1) + 1);
      } else if (s.toLowerCase().startsWith("file://") && !s.toLowerCase().startsWith("file:///")) {
        s = s.substring(0, s.indexOf("/", s.indexOf(":") + 2) + 1);
      } else if (s.toLowerCase().startsWith("azure:")) {
        int index = -1;
        if (s.contains("@file.core.windows.net/")) {
          index = s.indexOf("@file.core.windows.net/") + "@file.core.windows.net/".length();
        } else if (s.contains("@blob.core.windows.net/")) {
          index = s.indexOf("@blob.core.windows.net/") + "@blob.core.windows.net/".length();
        } 
        if (index >= 0) {
          if (s.indexOf("/", index) < 0 && !s.endsWith("/"))
            s = String.valueOf(s) + "/"; 
          s = s.substring(0, s.indexOf("/", index));
        } 
      } else {
        s = s.substring(0, s.indexOf("/", s.indexOf(":") + 3) + 1);
      } 
    } 
    if ((s.toUpperCase().startsWith("S3:/") && s3_root_path) || s.toUpperCase().startsWith("GSTORAGE:/")) {
      String tmp_path = (new VRL(s_original)).getPath();
      if (tmp_path.length() > 1) {
        tmp_path = tmp_path.substring(1, tmp_path.indexOf("/", 1) + 1);
        s = String.valueOf(s) + tmp_path;
      } 
    } 
    return s;
  }
  
  public static InputStream sanitizeXML(InputStream in) throws Exception {
    if (in instanceof ByteArrayInputStream) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      streamCopier(in, baos, false, true, true);
      String xml_str = new String(baos.toByteArray());
      if (xml_str.indexOf("<!") >= 0)
        throw new Exception("XML syntax not allowed."); 
      in = new ByteArrayInputStream(baos.toByteArray());
    } 
    return in;
  }
  
  public static Object readXMLObject(InputStream in) {
    Object result = null;
    try {
      in = sanitizeXML(in);
      Document doc = getSaxBuilder().build(in);
      result = getElements(doc.getRootElement());
      in.close();
    } catch (Exception e) {
      log("", 0, e);
    } 
    return result;
  }
  
  public static Object readXMLObject(File_S file) {
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
      if ((new File_S(path)).exists())
        return readXMLObject(new File_S(path)); 
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
    RandomAccessFile eraser = new RandomAccessFile(new File_S(path), "rw");
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
  
  public static void copy(String src, String dst, boolean overwrite) throws Exception {
    if ((new File_S(src)).isDirectory()) {
      (new File_S(dst)).mkdirs();
      return;
    } 
    if ((new File_S(dst)).exists() && !overwrite)
      return; 
    RandomAccessFile in = null;
    RandomAccessFile out = null;
    try {
      in = new RandomAccessFile(new File_S(src), "r");
      out = new RandomAccessFile(new File_S(dst), "rw");
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
    (new File_S(dst)).setLastModified((new File_S(src)).lastModified());
  }
  
  public static boolean zip(String root_dir, Vector zipFiles, String outputPath) throws Exception {
    root_dir = String.valueOf((new File_S(root_dir)).getCanonicalPath().replace('\\', '/')) + "/";
    ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(new File_S(outputPath)));
    zout.setLevel(9);
    for (int xx = 0; xx < zipFiles.size(); xx++) {
      Properties item = zipFiles.elementAt(xx);
      File_S file = new File_S((new VRL(item.getProperty("url"))).getPath());
      if (file.isDirectory()) {
        String itemName = (String.valueOf(file.getCanonicalPath().substring(root_dir.length())) + "/").replace('\\', '/');
        zout.putNextEntry(new ZipEntry(itemName));
      } else if (file.isFile()) {
        String itemName = file.getCanonicalPath().substring(root_dir.length()).replace('\\', '/');
        if (itemName.indexOf(":") >= 0)
          itemName = itemName.substring(itemName.indexOf(":") + 1).trim(); 
        zout.putNextEntry(new ZipEntry(itemName));
        RandomAccessFile in = new RandomAccessFile(file, "r");
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
    return encryptDecrypt(s, encrypt, new String(encryption_password));
  }
  
  public static String encryptDecrypt(String s, boolean encrypt, String key) throws Exception {
    if (s == null || s.equals(""))
      return ""; 
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
    File_S f = new File_S(real_path);
    try {
      real_path = String.valueOf(f.getCanonicalPath()) + "/";
      f = new File_S(real_path);
    } catch (Exception e) {
      log("", 1, e);
    } 
    if (f.isDirectory()) {
      String[] files = f.list();
      for (int x = 0; x < files.length; x++) {
        File_S f2 = new File_S(String.valueOf(real_path) + files[x]);
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
  
  public static boolean recurseDelete_U(String real_path, boolean test_mode) {
    if (real_path.trim().equals("/"))
      return false; 
    if (real_path.trim().equals("~"))
      return false; 
    if (real_path.indexOf(":") >= 0 && real_path.length() < 4)
      return false; 
    File_U f = new File_U(real_path);
    try {
      real_path = String.valueOf(f.getCanonicalPath()) + "/";
      f = new File_U(real_path);
    } catch (Exception e) {
      log("", 1, e);
    } 
    if (f.isDirectory()) {
      String[] files = f.list();
      for (int x = 0; x < files.length; x++) {
        File_U f2 = new File_U(String.valueOf(real_path) + files[x]);
        if (!isSymbolicLink(f2.getAbsolutePath())) {
          if (f2.isDirectory())
            recurseDelete_U(String.valueOf(real_path) + files[x] + "/", test_mode); 
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
      return f.delete();
    } 
    return true;
  }
  
  public static long recurseSize(String real_path, long size) {
    if (real_path.trim().equals("/"))
      return size; 
    if (real_path.trim().equals("~"))
      return size; 
    if (real_path.indexOf(":") >= 0 && real_path.length() < 4)
      return size; 
    File_S f = new File_S(real_path);
    try {
      real_path = String.valueOf(f.getCanonicalPath()) + "/";
      f = new File_S(real_path);
    } catch (IOException e) {
      e.printStackTrace();
    } 
    if (f.isDirectory()) {
      String[] files = f.list();
      for (int x = 0; x < files.length; x++) {
        File_S f2 = new File_S(String.valueOf(real_path) + files[x]);
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
    while (s.length() < 32)
      s = "0" + s; 
    return s;
  }
  
  public static void getAllFileListing_S(Vector list, String path, int depth, boolean includeFolders) throws Exception {
    File_S item = new File_S(path);
    if (item.isFile()) {
      list.addElement(item);
    } else {
      appendListing_S(path, list, "", depth, includeFolders);
    } 
  }
  
  public static void appendListing_S(String path, Vector list, String dir, int depth, boolean includeFolders) throws Exception {
    if (depth == 0)
      return; 
    depth--;
    if (!path.endsWith("/"))
      path = String.valueOf(path) + "/"; 
    String[] items = (new File_S(String.valueOf(path) + dir)).list();
    if (items == null)
      return; 
    for (int x = 0; x < items.length; x++) {
      File_S item = new File_S(String.valueOf(path) + dir + items[x]);
      if (item.isFile() || includeFolders) {
        if (item.lastModified() < 172800000L) {
          item.setLastModified((new SimpleDateFormat("MM/dd/yy")).parse("04/10/1998").getTime());
          item = new File_S(String.valueOf(path) + dir + items[x]);
        } 
        list.addElement(item);
      } 
      if (item.isDirectory())
        appendListing_S(path, list, String.valueOf(dir) + items[x] + "/", depth, includeFolders); 
    } 
    if (items.length == 0)
      list.addElement(new File_S(String.valueOf(path) + dir)); 
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
    long localSize = (new File_S(localVrl.getPath())).length();
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
                Tunnel2.getLocalMd5s(new File_S(this.val$localVrl.getPath()), true, this.val$status2, this.val$chunksF2);
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
  
  public static boolean do_searches(String filters, String data, boolean single, int iteration) {
    String[] multiple_filter = filters.split(",");
    for (int x = 0; x < multiple_filter.length; x++) {
      String filter = multiple_filter[x];
      if (!filter.equals("") && do_search(filter, data, single, iteration))
        return true; 
    } 
    return false;
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
      return (new File_S(vrl.getPath())).getCanonicalPath(); 
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
    if (trust_path != null && trust_path.equals("PKCS11")) {
      try {
        ss = (SSLSocket)getFips().getSocketFactory().createSocket(sock, host, port, true);
      } catch (Exception e) {
        log("SERVER", 2, e);
        throw new IOException(e);
      } 
      return ss;
    } 
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
          trust_pfx.load(new FileInputStream(new File_S(trust_path)), pass.toCharArray());
          trust = trust_pfx;
        } catch (Exception e1) {
          try {
            pass = encryptDecrypt(pass2, false);
            trust_pfx.load(new FileInputStream(new File_S(trust_path)), pass.toCharArray());
            trust = trust_pfx;
          } catch (Exception e2) {
            try {
              pass = encryptDecrypt(pass1, false);
              trust_jks.load(new FileInputStream(new File_S(trust_path)), pass.toCharArray());
              trust = trust_jks;
            } catch (Exception e3) {
              try {
                pass = encryptDecrypt(pass2, false);
                trust_jks.load(new FileInputStream(new File_S(trust_path)), pass.toCharArray());
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
    while (the_line.indexOf(String.valueOf(r1) + "encode_all_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "encode_all_end" + r2) >= 0) {
      String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "encode_all_start" + r2) + (String.valueOf(r1) + "encode_all_start" + r2).length(), the_line.indexOf(String.valueOf(r1) + "encode_all_end" + r2));
      the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "encode_all_start" + r2))) + url_encode_all(inner) + the_line.substring(the_line.indexOf(String.valueOf(r1) + "encode_all_end" + r2) + (String.valueOf(r1) + "encode_all_end" + r2).length());
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
      if (loc2 < 0)
        loc2 = (inner.split(loc1)).length + loc2; 
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
      if (the_line.indexOf(String.valueOf(r1) + "decrypt_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "decrypt_end" + r2) >= 0) {
        String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "decrypt_start" + r2) + (String.valueOf(r1) + "decrypt_start" + r2).length(), the_line.indexOf(String.valueOf(r1) + "decrypt_end" + r2));
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "decrypt_start" + r2))) + encryptDecrypt(inner, false) + the_line.substring(the_line.indexOf(String.valueOf(r1) + "decrypt_end" + r2) + (String.valueOf(r1) + "decrypt_end" + r2).length());
        found = true;
      } 
      if (the_line.indexOf(String.valueOf(r1) + "base64_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "base64_end" + r2) >= 0) {
        String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "base64_start" + r2) + (String.valueOf(r1) + "base64_start" + r2).length(), the_line.indexOf(String.valueOf(r1) + "base64_end" + r2));
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "base64_start" + r2))) + Base64.encodeBytes(inner.getBytes("UTF8")) + the_line.substring(the_line.indexOf(String.valueOf(r1) + "base64_end" + r2) + (String.valueOf(r1) + "base64_end" + r2).length());
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
      if (the_line.indexOf(String.valueOf(r1) + "1char_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "1char_end" + r2) >= 0) {
        String end_str = String.valueOf(r1) + "1char_end" + r2;
        int end_pos = findEnd(String.valueOf(r1) + "1char_start", end_str, the_line);
        String inner = textFunctions(the_line.substring(the_line.indexOf(String.valueOf(r1) + "1char_start" + r2) + (String.valueOf(r1) + "1char_start" + r2).length(), end_pos), r1, r2);
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "1char_start" + r2))) + inner.substring(0, inner.length() - 1) + the_line.substring(end_pos + end_str.length());
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
        if (loc1 < 0) {
          loc1 = inner.length() + loc1;
          if (loc2 > 0)
            loc2 = loc1 + loc2; 
        } 
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
        loc1 = replace_str(loc1, "~.|~", "{");
        loc1 = replace_str(loc1, "~|.~", "}");
        if (loc1.equals("."))
          loc1 = "\\."; 
        if (loc1.equals("|"))
          loc1 = "\\|"; 
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
        loc1 = replace_str(loc1, "~..~", ":");
        loc1 = replace_str(loc1, "~.|~", "{");
        loc1 = replace_str(loc1, "~|.~", "}");
        String loc2 = "";
        if ((params.split(":")).length > 2)
          loc2 = params.split(":")[2]; 
        loc2 = replace_str(loc2, "~..~", ":");
        loc2 = replace_str(loc2, "~.|~", "{");
        loc2 = replace_str(loc2, "~|.~", "}");
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
      if (the_line.indexOf(String.valueOf(r1) + "geoip_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "geoip_end" + r2) >= 0) {
        String end_str = String.valueOf(r1) + "geoip_end" + r2;
        int end_pos = findEnd(String.valueOf(r1) + "geoip_start", end_str, the_line);
        String inner = textFunctions(the_line.substring(the_line.indexOf(String.valueOf(r1) + "geoip_start" + r2) + (String.valueOf(r1) + "geoip_start" + r2).length(), end_pos), r1, r2);
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "geoip_start" + r2))) + geo_ip_lookup(inner) + the_line.substring(end_pos + end_str.length());
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
      if (the_line.indexOf(String.valueOf(r1) + "base64url_start" + r2) >= 0 && the_line.indexOf(String.valueOf(r1) + "base64url_end" + r2) >= 0) {
        String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "base64url_start" + r2) + (String.valueOf(r1) + "base64url_start" + r2).length(), the_line.indexOf(String.valueOf(r1) + "base64url_end" + r2));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        VRL vrl = new VRL(inner);
        GenericClient c = getClient(vrl.toString(), "", new Vector());
        c.login(vrl.getUsername(), vrl.getPassword(), "");
        streamCopier(c.download(vrl.getPath(), 0L, -1L, true), baos, false, true, true);
        c.logout();
        the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "base64url_start" + r2))) + Base64.encodeBytes(baos.toByteArray()) + the_line.substring(the_line.indexOf(String.valueOf(r1) + "base64url_end" + r2) + (String.valueOf(r1) + "base64url_end" + r2).length());
        found = true;
      } 
    } 
    return the_line;
  }
  
  public static String geo_ip_lookup(String ip) throws Exception {
    StringBuffer csv = new StringBuffer();
    String ip_f = ip;
    StringBuffer status = new StringBuffer();
    Worker.startWorker(new Runnable(ip_f, csv, status) {
          private final String val$ip_f;
          
          private final StringBuffer val$csv;
          
          private final StringBuffer val$status;
          
          public void run() {
            try {
              HttpURLConnection u = (HttpURLConnection)(new URL("https://freegeoip.net/csv/" + this.val$ip_f)).openConnection();
              InputStream uin = u.getInputStream();
              int bytes_read = 0;
              byte[] b = new byte[1024];
              while (bytes_read >= 0) {
                bytes_read = uin.read(b);
                if (bytes_read > 0)
                  this.val$csv.append(new String(b, 0, bytes_read)); 
              } 
              uin.close();
            } catch (Exception e) {
              Common.log("SERVER", 1, e);
            } 
            this.val$status.append("done");
          }
        }"GEO_IP_LOOKUP:" + ip);
    for (int x = 0; x < 100 && status.length() == 0; x++)
      Thread.sleep(100L); 
    String ip_country = "";
    String ip_state = "";
    String ip_city = "";
    if (csv.length() > 10) {
      ip_country = csv.toString().split(",")[2];
      ip_state = csv.toString().split(",")[3];
      ip_city = csv.toString().split(",")[5];
    } 
    return String.valueOf(ip_country) + "," + ip_state + "," + ip_city;
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
    if (s.endsWith(".ICO"))
      return "image/x-icon"; 
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
  
  public static String format_time(long seconds) {
    int secs = (int)seconds;
    int hours = 0;
    int mins = 0;
    if (secs > 60) {
      String str1 = (new StringBuffer(String.valueOf(secs / 60.0D / 60.0D))).toString();
      hours = Integer.parseInt(str1.substring(0, str1.indexOf(".")));
      secs -= hours * 60 * 60;
      String str2 = (new StringBuffer(String.valueOf(secs / 60.0D))).toString();
      mins = Integer.parseInt(str2.substring(0, str2.indexOf(".")));
      secs -= mins * 60;
    } 
    if (hours < 0)
      hours = 0; 
    if (mins < 0)
      mins = 0; 
    if (secs < 0)
      secs = 0; 
    String hoursStr = (new StringBuffer(String.valueOf(hours))).toString();
    String minsStr = (new StringBuffer(String.valueOf(mins))).toString();
    String secsStr = (new StringBuffer(String.valueOf(secs))).toString();
    if (hours < 10)
      hoursStr = "0" + hours; 
    if (mins < 10)
      minsStr = "0" + mins; 
    if (secs < 10)
      secsStr = "0" + secs; 
    return String.valueOf(hoursStr) + ":" + minsStr + ":" + secsStr;
  }
  
  public static String format_time_pretty(long seconds) {
    int secs = (int)seconds;
    int hours = 0;
    int mins = 0;
    if (secs > 60) {
      String str1 = (new StringBuffer(String.valueOf(secs / 60.0D / 60.0D))).toString();
      hours = Integer.parseInt(str1.substring(0, str1.indexOf(".")));
      secs -= hours * 60 * 60;
      String str2 = (new StringBuffer(String.valueOf(secs / 60.0D))).toString();
      mins = Integer.parseInt(str2.substring(0, str2.indexOf(".")));
      secs -= mins * 60;
    } 
    if (hours < 0)
      hours = 0; 
    if (mins < 0)
      mins = 0; 
    if (secs < 0)
      secs = 0; 
    String hoursStr = (new StringBuffer(String.valueOf(hours))).toString();
    String minsStr = (new StringBuffer(String.valueOf(mins))).toString();
    String secsStr = (new StringBuffer(String.valueOf(secs))).toString();
    if (hours < 10)
      hoursStr = "0" + hours; 
    if (mins < 10)
      minsStr = "0" + mins; 
    if (secs < 10)
      secsStr = "0" + secs; 
    String s = "";
    if (!hoursStr.equals("00"))
      s = String.valueOf(s) + hoursStr + " hr, "; 
    if (!minsStr.equals("00"))
      s = String.valueOf(s) + minsStr + " min, "; 
    s = String.valueOf(s) + secsStr + " sec.";
    return s;
  }
  
  public static String format_bytes_short2(String bytes) {
    try {
      return format_bytes_short2(Long.parseLong(bytes));
    } catch (Exception exception) {
      return bytes;
    } 
  }
  
  public static String format_bytes_short2(long bytes) {
    boolean neg = (bytes < 0L);
    if (bytes < 0L)
      bytes = Math.abs(bytes); 
    String return_str = "";
    try {
      long tb = 1099511627776L;
      if (bytes > tb) {
        return_str = String.valueOf((int)((float)bytes / (float)tb * 100.0F) / 100.0F) + " " + System.getProperty("crushftp.terrabytes_label_short", "T") + System.getProperty("bytes_label_short", "B");
      } else if (bytes > 1073741824L) {
        return_str = String.valueOf((int)((float)bytes / 1.07374182E9F * 100.0F) / 100.0F) + " " + System.getProperty("crushftp.gigabytes_label_short", "G") + System.getProperty("bytes_label_short", "B");
      } else if (bytes > 1048576L) {
        return_str = String.valueOf((int)((float)bytes / 1048576.0F * 100.0F) / 100.0F) + " " + System.getProperty("crushftp.megabytes_label_short", "M") + System.getProperty("bytes_label_short", "B");
      } else if (bytes > 1024L) {
        return_str = String.valueOf((int)((float)bytes / 1024.0F * 100.0F) / 100.0F) + " " + System.getProperty("crushftp.kilobytes_label_short", "K") + System.getProperty("bytes_label_short", "B");
      } else {
        return_str = String.valueOf(bytes) + " " + System.getProperty("bytes_label_short", "B");
      } 
    } catch (Exception exception) {}
    if (neg)
      return_str = "-" + return_str; 
    return return_str;
  }
  
  public static String format_bytes2(String byte_amount) {
    String return_str = "";
    try {
      long bytes = Long.parseLong(byte_amount);
      long tb = 1099511627776L;
      if (bytes > tb) {
        return_str = String.valueOf((int)((float)bytes / (float)tb * 100.0F) / 100.0F) + " " + System.getProperty("crushftp.terrabytes_label", "Terra") + System.getProperty("bytes_label", "Bytes");
      } else if (bytes > 1073741824L) {
        return_str = String.valueOf((int)((float)bytes / 1.07374182E9F * 100.0F) / 100.0F) + " " + System.getProperty("crushftp.gigabytes_label", "Giga") + System.getProperty("bytes_label", "Bytes");
      } else if (bytes > 1048576L) {
        return_str = String.valueOf((int)((float)bytes / 1048576.0F * 100.0F) / 100.0F) + " " + System.getProperty("crushftp.megabytes_label", "Mega") + System.getProperty("bytes_label", "Bytes");
      } else if (bytes > 1024L) {
        return_str = String.valueOf((int)((float)bytes / 1024.0F * 100.0F) / 100.0F) + " " + System.getProperty("crushftp.kilobytes_label", "Kilo") + System.getProperty("bytes_label", "Bytes");
      } else {
        return_str = String.valueOf(bytes) + " " + System.getProperty("bytes_label", "Bytes").toLowerCase();
      } 
    } catch (Exception exception) {}
    return return_str;
  }
  
  public static void updateMimes() throws Exception {
    long mimesModifiedNew = (new File_S(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/mime_types.txt")).lastModified();
    if (mimesModified != mimesModifiedNew) {
      mimesModified = mimesModifiedNew;
      mimes = new Properties();
      try {
        BufferedReader mimeIn = new BufferedReader(new FileReader(new File_S(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/mime_types.txt")));
        String s = mimeIn.readLine();
        while (s != null) {
          if (!s.startsWith("#"))
            try {
              mimes.put(s.substring(0, s.indexOf(" ")).trim().toUpperCase(), s.substring(s.indexOf(" ") + 1).trim());
            } catch (Exception exception) {} 
          s = mimeIn.readLine();
        } 
        mimeIn.close();
      } catch (Exception e) {
        e.printStackTrace();
      } 
    } 
  }
  
  public static void check_exec() {
    if (!System.getProperty("crushftp.security.exec", "true").equals("true"))
      throw new RuntimeException("Executing external processes not allowed."); 
  }
  
  public static String dumpStack(String info) {
    String result = new Date() + "\r\nJava:" + System.getProperty("java.version") + " from:" + System.getProperty("java.home") + " " + System.getProperty("sun.arch.data.model") + " bit  OS:" + System.getProperties().getProperty("os.name") + "\r\n";
    result = String.valueOf(result) + "Server Memory Stats: Max=" + format_bytes_short(Runtime.getRuntime().maxMemory()) + ", Free=" + format_bytes_short(Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory()) + "\r\n";
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
    try {
      result = String.valueOf(result) + "Working dir:" + (new File("./")).getCanonicalPath() + "\r\n";
    } catch (Exception e) {
      result = String.valueOf(result) + "Working dir:" + e + "\r\n";
    } 
    try {
      result = String.valueOf(result) + "statsDB folder size:" + format_bytes_short(recurseSize("./statsDB", 0L)) + "\r\n";
    } catch (Exception e) {
      result = String.valueOf(result) + "statsDB folder size:" + e + "\r\n";
    } 
    try {
      result = String.valueOf(result) + "sessions.obj size:" + format_bytes_short(recurseSize("./sessions.obj", 0L)) + "\r\n";
    } catch (Exception e) {
      result = String.valueOf(result) + "sessions.obj size:" + e + "\r\n";
    } 
    result = String.valueOf(result) + "\r\n#######################Start Thread Dump (" + threads2.length + ")#######################\r\n";
    for (x = 0; x < threads2.length; x++) {
      String data = "---------------------------------------------------------------------------------------------------";
      result = String.valueOf(result) + data + "\r\n";
      data = threads2[x].getName();
      result = String.valueOf(result) + data + "\r\n";
      StackTraceElement[] ste = threads2[x].getStackTrace();
      for (int xx = 0; xx < ste.length; xx++) {
        data = "\t\t" + ste[xx].getClassName() + "." + ste[xx].getMethodName() + "(" + ste[xx].getFileName() + ":" + ste[xx].getLineNumber() + ")";
        result = String.valueOf(result) + data + "\r\n";
      } 
    } 
    result = String.valueOf(result) + "#######################End Thread Dump (" + threads2.length + ")#######################\r\n";
    System.out.println(result);
    return result;
  }
  
  public static void sockLog(Socket sock, String msg) {
    if (!System.getProperty("crushftp.debug_socks_log", "false").equals("true"))
      return; 
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy HH:mm:ss.SSS");
    try {
      log("DMZ", 0, String.valueOf(sdf.format(new Date())) + "|DEBUG_SOCKETS|" + sock + "|" + msg);
    } catch (Exception e) {
      log("SERVER", 1, e);
    } 
  }
  
  public static String send_mail(String server_ip, String to_user, String cc_user, String bcc_user, String from_user, String subject, String body, String smtp_server, String smtp_user, String smtp_pass, boolean smtp_ssl, boolean html, File_B[] attachments) {
    return send_mail(server_ip, to_user, cc_user, bcc_user, from_user, null, subject, body, smtp_server, smtp_user, smtp_pass, smtp_ssl, html, attachments, new Vector());
  }
  
  public static String send_mail(String server_ip, String to_user, String cc_user, String bcc_user, String from_user, String reply_to_user, String subject, String body, String smtp_server, String smtp_user, String smtp_pass, boolean smtp_ssl, boolean html, File_B[] attachments) {
    return send_mail(server_ip, to_user, cc_user, bcc_user, from_user, reply_to_user, subject, body, smtp_server, smtp_user, smtp_pass, smtp_ssl, html, attachments, new Vector());
  }
  
  public static String send_mail(String server_ip, String to_user, String cc_user, String bcc_user, String from_user, String reply_to_user, String subject, String body, String smtp_server, String smtp_user, String smtp_pass, boolean smtp_ssl, boolean html, File_B[] attachments, Vector fileMimeTypes) {
    return send_mail(server_ip, to_user, cc_user, bcc_user, from_user, reply_to_user, subject, body, smtp_server, smtp_user, smtp_pass, smtp_ssl, html, attachments, fileMimeTypes, new Vector());
  }
  
  public static String send_mail(String server_ip, String to_user, String cc_user, String bcc_user, String from_user, String reply_to_user, String subject, String body, String smtp_server, String smtp_user, String smtp_pass, boolean smtp_ssl, boolean html, File_B[] attachments, Vector fileMimeTypes, Vector remoteFiles) {
    try {
      return Mailer.send_mail(server_ip, to_user, cc_user, bcc_user, from_user, reply_to_user, subject, body, smtp_server, smtp_user, smtp_pass, smtp_ssl, html, attachments, fileMimeTypes, remoteFiles);
    } catch (Throwable e) {
      log("SMTP", 1, e);
      return "ERROR:" + e.toString();
    } 
  }
  
  public static String send_mail(String server_ip, String to_user, String cc_user, String bcc_user, String from_user, String subject, String body, String smtp_server, String smtp_user, String smtp_pass, boolean smtp_ssl, boolean html) {
    return send_mail(server_ip, to_user, cc_user, bcc_user, from_user, null, subject, body, smtp_server, smtp_user, smtp_pass, smtp_ssl, html, null, new Vector());
  }
  
  public static String send_mail(String server_ip, String to_user, String cc_user, String bcc_user, String from_user, String reply_to_user, String subject, String body, String smtp_server, String smtp_user, String smtp_pass, boolean smtp_ssl, boolean html) {
    return send_mail(server_ip, to_user, cc_user, bcc_user, from_user, reply_to_user, subject, body, smtp_server, smtp_user, smtp_pass, smtp_ssl, html, null, new Vector());
  }
  
  public static SAXBuilder getSaxBuilder() {
    SAXBuilder sb = new SAXBuilder();
    sb.setExpandEntities(false);
    sb.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    return sb;
  }
  
  public static String bytesToHex(byte[] b) {
    char[] hex = "0123456789ABCDEF".toCharArray();
    String s = "";
    for (int x = 0; x < b.length; x++)
      s = String.valueOf(s) + hex[(b[x] & 0xFF) >>> 4] + hex[b[x] & 0xFF & 0xF]; 
    return s;
  }
  
  public static byte[] hexToBytes(String s) {
    byte[] b = new byte[s.length() / 2];
    for (int x = 0; x < s.length(); x += 2)
      b[x / 2] = (byte)((Character.digit(s.charAt(x), 16) << 4) + Character.digit(s.charAt(x + 1), 16)); 
    return b;
  }
  
  public static String getCpuUsage() {
    try {
      Object bean = ManagementFactory.getOperatingSystemMXBean();
      Method getProcessCpuLoad = bean.getClass().getMethod("getProcessCpuLoad", new Class[0]);
      getProcessCpuLoad.setAccessible(true);
      String s = (new StringBuffer(String.valueOf((int)(Float.parseFloat(getProcessCpuLoad.invoke(bean, (Object[])new Class[0]).toString()) * 100.0D)))).toString();
      Method getSystemCpuLoad = bean.getClass().getMethod("getSystemCpuLoad", new Class[0]);
      getSystemCpuLoad.setAccessible(true);
      s = String.valueOf(s) + ":" + (int)(Float.parseFloat(getSystemCpuLoad.invoke(bean, (Object[])new Class[0]).toString()) * 100.0D);
      return s;
    } catch (Throwable throwable) {
      return "";
    } 
  }
  
  public static void writeFileFromJar_plain(String src, String dst, boolean preservePath) {
    InputStream in = null;
    RandomAccessFile out = null;
    try {
      if (!(new File_S(src)).exists()) {
        in = (new Common()).getClass().getResourceAsStream(src);
        if (preservePath) {
          (new File_S(all_but_last(String.valueOf(dst) + src))).mkdirs();
        } else if (src.indexOf("/") >= 0) {
          src = last(src);
        } 
        out = new RandomAccessFile(new File_S(String.valueOf(dst) + src), "rw");
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
  
  public static void writeFileFromJar_assets(String src, String dst, boolean preservePath) {
    try {
      if (!(new File_S(src)).exists()) {
        InputStream in = (new Common()).getClass().getResourceAsStream("/assets/crushftp/" + src);
        if (preservePath) {
          (new File_S(all_but_last(String.valueOf(dst) + src))).mkdirs();
        } else if (src.indexOf("/") >= 0) {
          src = last(src);
        } 
        RandomAccessFile out = new RandomAccessFile(new File_S(String.valueOf(dst) + src), "rw");
        int bytes = 0;
        byte[] b = new byte[32768];
        while (bytes >= 0) {
          bytes = in.read(b);
          if (bytes > 0)
            out.write(b, 0, bytes); 
        } 
        in.close();
        out.close();
      } 
    } catch (Exception e) {
      e.printStackTrace();
    } 
  }
  
  public static void write_service_conf(String filename, int ram_megabytes, String main_class, String service_name, String main_jar) {
    if (main_class.equals("CrushClientWinService"))
      main_class = "com.crushftp.client.Client"; 
    if (main_class.equals("CrushTunnelWinService"))
      main_class = "com.crushftp.client.Client"; 
    try {
      (new File_S("./service")).mkdir();
      String service_ini = "service.class=" + main_class + "\r\n";
      service_ini = String.valueOf(service_ini) + "service.id=" + service_name + "\r\n";
      service_ini = String.valueOf(service_ini) + "service.name=" + service_name + "\r\n";
      service_ini = String.valueOf(service_ini) + "service.description=" + service_name + "\r\n";
      if (service_name.toUpperCase().indexOf("RESTART") >= 0) {
        service_ini = String.valueOf(service_ini) + "service.startup=demand\r\n";
      } else {
        service_ini = String.valueOf(service_ini) + "service.startup=auto\r\n";
      } 
      service_ini = String.valueOf(service_ini) + "classpath.1=" + main_jar + "\r\n";
      service_ini = String.valueOf(service_ini) + "working.directory=../\r\n";
      service_ini = String.valueOf(service_ini) + "log=wrapper.log\r\n";
      service_ini = String.valueOf(service_ini) + "log.roll.size=10\r\n";
      service_ini = String.valueOf(service_ini) + "vm.heapsize.preferred=" + ram_megabytes + "\r\n";
      service_ini = String.valueOf(service_ini) + "vm.sysfirst=true\r\n";
      service_ini = String.valueOf(service_ini) + "vm.location=.\\java\\bin\\server\\jvm.dll|%JAVA_HOME%\\bin\\server\\jvm.dll|%JAVA_PATH%\\bin\\server\\jvm.dll\r\n";
      if (!System.getProperty("java.version").startsWith("1.4") && !System.getProperty("java.version").startsWith("1.5") && !System.getProperty("java.version").startsWith("1.6") && !System.getProperty("java.version").startsWith("1.7") && !System.getProperty("java.version").startsWith("1.8"))
        service_ini = String.valueOf(service_ini) + "vmarg.1=--add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED\r\n"; 
      service_ini = String.valueOf(service_ini) + "arg.1=-d\r\n";
      RandomAccessFile wrapper = new RandomAccessFile(new File_S(filename), "rw");
      wrapper.setLength(0L);
      wrapper.seek(0L);
      wrapper.write(service_ini.getBytes("UTF8"));
      wrapper.close();
    } catch (Exception e) {
      log("SERVER", 0, e);
    } 
  }
  
  public static void update_service_memory(int MB, String service_name) {
    if (machine_is_windows()) {
      try {
        String filename = "./service/" + service_name + "Service.ini";
        RandomAccessFile wrapper = new RandomAccessFile(new File_S(filename), "rw");
        byte[] b = new byte[(int)wrapper.length()];
        wrapper.readFully(b);
        String wrapperStr = new String(b, "UTF8");
        int pos = wrapperStr.indexOf("vm.heapsize.preferred=");
        wrapperStr = String.valueOf(wrapperStr.substring(0, pos + "vm.heapsize.preferred=".length())) + MB + wrapperStr.substring(wrapperStr.indexOf("\r\n", pos));
        wrapper.setLength(0L);
        wrapper.seek(0L);
        wrapper.write(wrapperStr.getBytes("UTF8"));
        wrapper.close();
      } catch (Exception e) {
        log("SERVER", 0, e);
      } 
    } else if (machine_is_linux() || machine_is_x()) {
      try {
        String filename = "./crushftp_init.sh";
        if (machine_is_x())
          filename = "./CrushFTP.command"; 
        RandomAccessFile wrapper = new RandomAccessFile(new File_S(filename), "rw");
        byte[] b = new byte[(int)wrapper.length()];
        wrapper.readFully(b);
        String wrapperStr = new String(b, "UTF8");
        int pos = wrapperStr.indexOf("-Xmx");
        while (pos > 0) {
          wrapperStr = String.valueOf(wrapperStr.substring(0, pos + "-Xmx".length())) + MB + "m" + wrapperStr.substring(wrapperStr.indexOf(" ", pos));
          pos = wrapperStr.indexOf("-Xmx", pos + 1);
        } 
        wrapper.setLength(0L);
        wrapper.seek(0L);
        wrapper.write(wrapperStr.getBytes("UTF8"));
        wrapper.close();
      } catch (Exception e) {
        log("SERVER", 0, e);
      } 
    } 
  }
  
  public static void set_encryption_password(String pass) {
    encryption_password = pass.toCharArray();
  }
  
  public static boolean test_elevate() throws Exception {
    try {
      check_exec();
      Process proc = Runtime.getRuntime().exec(new String[] { (new File_S("./service/elevate.exe")).getCanonicalPath(), "-c", "-w", "net", "user" }, (String[])null, new File_S("./service/"));
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      streamCopier(null, null, proc.getInputStream(), baos, false, true, true);
      proc.destroy();
      String data = new String(baos.toByteArray());
      if (data.toUpperCase().indexOf("NOT A VALID") >= 0 || data.toUpperCase().indexOf("ACCESS DENIED") >= 0)
        return false; 
      return true;
    } catch (Throwable t) {
      log("SERVER", 0, t);
      return false;
    } 
  }
  
  public static boolean install_windows_service(int ram_megabytes, String service_name, String main_jar) {
    return install_windows_service(ram_megabytes, service_name, main_jar, true);
  }
  
  public static boolean install_windows_service(int ram_megabytes, String service_name, String main_jar, boolean start) {
    boolean ok = true;
    if (machine_is_windows()) {
      try {
        write_wrapper_files(ram_megabytes, Integer.parseInt(System.getProperty("sun.arch.data.model", "64")), service_name, main_jar);
        String bat = "";
        bat = String.valueOf(bat) + service_name + "Service.exe --WinRun4J:RegisterService\r\n";
        if (service_name.equals("CrushFTP"))
          bat = String.valueOf(bat) + "CrushFTPRestart.exe --WinRun4J:RegisterService\r\n"; 
        if (start)
          bat = String.valueOf(bat) + "net start \"" + service_name + " Server\"\r\n"; 
        RandomAccessFile out = new RandomAccessFile(new File_S("./service/service.bat"), "rw");
        out.setLength(0L);
        out.write(bat.getBytes());
        out.close();
        if (test_elevate()) {
          check_exec();
          Process proc = Runtime.getRuntime().exec(new String[] { (new File_S("./service/elevate.exe")).getCanonicalPath(), "-c", "-w", "service.bat" }, (String[])null, new File_S("./service/"));
          BufferedReader proc_in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
          String data = "";
          while ((data = proc_in.readLine()) != null) {
            log("SERVER", 0, data);
            if (data.toUpperCase().indexOf("NOT A VALID") >= 0 || data.toUpperCase().indexOf("ACCESS DENIED") >= 0)
              ok = false; 
          } 
          proc_in.close();
          proc.waitFor();
        } else {
          ok = false;
        } 
        if (!ok) {
          ok = true;
          check_exec();
          Process proc = Runtime.getRuntime().exec(new String[] { "cmd", "/C", "service.bat" }, (String[])null, new File_S("./service/"));
          BufferedReader proc_in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
          String data = "";
          while ((data = proc_in.readLine()) != null) {
            log("SERVER", 0, data);
            if (data.toUpperCase().indexOf("ACCESS DENIED") >= 0)
              ok = false; 
          } 
          proc_in.close();
          proc.waitFor();
        } 
      } catch (Exception e) {
        log("SERVER", 0, e);
      } 
      (new File_S("./service/elevate.exe")).delete();
      (new File_S("./service/service.bat")).delete();
    } 
    return ok;
  }
  
  public static String install_windows_service_username(String domainuser, String domainpass, String service_name) {
    boolean ok = false;
    String result = "";
    if (machine_is_windows()) {
      try {
        String bat = "setlocal\r\n";
        bat = String.valueOf(bat) + "sc.exe config \"" + service_name + " Server\" obj= \"" + domainuser + "\" password= \"" + domainpass + "\"\r\n";
        RandomAccessFile out = new RandomAccessFile(new File_S("./service/service.bat"), "rw");
        out.setLength(0L);
        out.write(bat.getBytes());
        out.close();
        if (test_elevate()) {
          check_exec();
          Process proc = Runtime.getRuntime().exec(new String[] { (new File_S("./service/elevate.exe")).getCanonicalPath(), "-c", "-w", "service.bat" }, (String[])null, new File_S("./service/"));
          BufferedReader proc_in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
          String data = "";
          while ((data = proc_in.readLine()) != null) {
            log("SERVER", 0, data);
            if (data.toUpperCase().indexOf("INVALID") >= 0 || data.toUpperCase().indexOf("ACCESS DENIED") >= 0)
              ok = false; 
            if (data.toUpperCase().indexOf("SUCCESS") >= 0)
              ok = true; 
            result = String.valueOf(result) + data + "\r\n";
          } 
          proc_in.close();
          proc.waitFor();
        } else {
          ok = false;
        } 
        if (!ok) {
          check_exec();
          Process proc = Runtime.getRuntime().exec(new String[] { "cmd", "/C", "service.bat" }, (String[])null, new File_S("./service/"));
          BufferedReader proc_in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
          String data = "";
          while ((data = proc_in.readLine()) != null) {
            log("SERVER", 0, data);
            if (data.toUpperCase().indexOf("ACCESS DENIED") >= 0)
              ok = false; 
            if (data.toUpperCase().indexOf("INVALID") >= 0)
              ok = false; 
            if (data.toUpperCase().indexOf("SUCCESS") >= 0)
              ok = true; 
            result = String.valueOf(result) + data + "\r\n";
          } 
          proc_in.close();
          proc.waitFor();
        } 
      } catch (Exception e) {
        log("SERVER", 0, e);
      } 
      (new File_S("./service/service.bat")).delete();
      (new File_S("./service/elevate.exe")).delete();
    } 
    if (ok)
      return ""; 
    return result;
  }
  
  public static void write_wrapper_files(int ram_megabytes, int bit, String service_name, String main_jar) throws Exception {
    writeFileFromJar_assets("service/CrushFTPService" + bit + ".exe", "./", true);
    if (service_name.equals("CrushFTP"))
      copy((new File_B("service/CrushFTPService" + bit + ".exe")).getPath(), (new File_B("service/CrushFTPRestart.exe")).getPath(), true); 
    (new File_S("service/CrushFTPService" + bit + ".exe")).renameTo(new File_S("service/" + service_name + "Service.exe"));
    copyStreams(new ByteArrayInputStream(getElevate()), new FileOutputStream("./service/elevate.exe", false), true, true);
    write_service_conf("./service/" + service_name + "Service.ini", ram_megabytes, String.valueOf(service_name) + "WinService", String.valueOf(service_name) + " Server", main_jar);
    if (service_name.equals("CrushFTP"))
      write_service_conf("./service/CrushFTPRestart.ini", ram_megabytes, "CrushFTPWinServiceRestart", "CrushFTPRestart", main_jar); 
  }
  
  public static boolean remove_windows_service(String service_name, String main_jar) {
    boolean ok = true;
    if (machine_is_windows()) {
      try {
        write_wrapper_files(512, Integer.parseInt(System.getProperty("sun.arch.data.model", "64")), service_name, main_jar);
        String bat = "net stop \"" + service_name + " Server\"\r\n";
        bat = String.valueOf(bat) + service_name + "Service.exe --WinRun4J:UnregisterService\r\n";
        if (service_name.equals("CrushFTP"))
          bat = String.valueOf(bat) + "CrushFTPRestart.exe --WinRun4J:UnregisterService\r\n"; 
        RandomAccessFile out = new RandomAccessFile(new File_S("./service/service.bat"), "rw");
        out.setLength(0L);
        out.write(bat.getBytes());
        out.close();
        if (test_elevate()) {
          check_exec();
          Process proc = Runtime.getRuntime().exec(new String[] { (new File_S("./service/elevate.exe")).getCanonicalPath(), "-c", "-w", "service.bat" }, (String[])null, new File_S("./service/"));
          BufferedReader proc_in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
          String data = "";
          while ((data = proc_in.readLine()) != null) {
            log("SERVER", 0, data);
            if (data.toUpperCase().indexOf("NOT A VALID") >= 0 || data.toUpperCase().indexOf("ACCESS DENIED") >= 0)
              ok = false; 
          } 
          proc_in.close();
          proc.waitFor();
        } else {
          ok = false;
        } 
        if (!ok) {
          ok = true;
          check_exec();
          Process proc = Runtime.getRuntime().exec(new String[] { "cmd", "/C", "service.bat" }, (String[])null, new File_S("./service/"));
          BufferedReader proc_in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
          String data = "";
          while ((data = proc_in.readLine()) != null) {
            log("SERVER", 0, data);
            if (data.toUpperCase().indexOf("ACCESS DENIED") >= 0)
              ok = false; 
          } 
          proc_in.close();
          proc.waitFor();
        } 
        if (ok)
          recurseDelete("./service/", false); 
      } catch (Exception e) {
        log("SERVER", 1, e);
      } 
      (new File_S("./service/elevate.exe")).delete();
      (new File_S("./service/service.bat")).delete();
    } 
    return ok;
  }
  
  public static void stopDaemon(boolean silent, String daemon_name) {
    String results = "";
    try {
      Process proc = null;
      if (machine_is_windows()) {
        copyStreams(new ByteArrayInputStream(getElevate()), new FileOutputStream("./service/elevate.exe", false), true, true);
        check_exec();
        if (test_elevate()) {
          proc = Runtime.getRuntime().exec(new String[] { (new File_S("./service/elevate.exe")).getCanonicalPath(), "-c", "-w", "net", "stop", String.valueOf(daemon_name) + " Server" }, (String[])null, new File_S("./service/"));
        } else {
          proc = Runtime.getRuntime().exec(new String[] { "net", "stop", String.valueOf(daemon_name) + " Server" }, (String[])null, new File_S("./service/"));
        } 
      } else if (machine_is_x()) {
        RandomAccessFile out = new RandomAccessFile(new File_S(String.valueOf(daemon_name) + "_exec_root.sh"), "rw");
        out.setLength(0L);
        out.write(("launchctl stop com.crushftp." + daemon_name + "\n").getBytes("UTF8"));
        out.close();
        exec(new String[] { "chmod", "+x", (new File_S(String.valueOf(daemon_name) + "_exec_root.sh")).getCanonicalPath() });
        check_exec();
        proc = Runtime.getRuntime().exec(new String[] { "osascript", "-e", "do shell script \"" + (new File_S(String.valueOf(daemon_name) + "_exec_root.sh")).getCanonicalPath() + "\" with administrator privileges" });
      } 
      BufferedReader proc_in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
      String data = "";
      boolean ok = true;
      while ((data = proc_in.readLine()) != null) {
        results = String.valueOf(results) + data + "\r\n";
        if (data.toUpperCase().indexOf("ACCESS DENIED") >= 0 || data.toUpperCase().indexOf("NOT A VALID") >= 0)
          ok = false; 
        log("SERVER", 0, data);
      } 
      proc_in.close();
      proc_in = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
      data = "";
      while ((data = proc_in.readLine()) != null) {
        results = String.valueOf(results) + data + "\r\n";
        if (data.toUpperCase().indexOf("ACCESS DENIED") >= 0 || data.toUpperCase().indexOf("NOT A VALID") >= 0)
          ok = false; 
        log("SERVER", 0, data);
      } 
      if (ok && !silent) {
        JOptionPane.showMessageDialog(null, "Stopped\r\n\r\n" + results);
      } else if (!silent) {
        JOptionPane.showMessageDialog(null, "Failure:\r\n\r\n" + results);
      } 
    } catch (Exception e) {
      e.printStackTrace();
      if (!silent)
        JOptionPane.showMessageDialog(null, String.valueOf(e.toString()) + "\r\n\r\n" + results); 
    } 
    if ((new File_S(String.valueOf(daemon_name) + "_exec_root.sh")).exists())
      (new File_S(String.valueOf(daemon_name) + "_exec_root.sh")).delete(); 
    if ((new File_S("service/elevate.exe")).exists())
      (new File_S("service/elevate.exe")).delete(); 
    (new File_S("./service/elevate.exe")).delete();
  }
  
  public static void startDaemon(boolean silent, String daemon_name) {
    String results = "";
    try {
      Process proc = null;
      check_exec();
      if (machine_is_windows()) {
        copyStreams(new ByteArrayInputStream(getElevate()), new FileOutputStream("./service/elevate.exe", false), true, true);
        if (test_elevate()) {
          proc = Runtime.getRuntime().exec(new String[] { (new File_S("./service/elevate.exe")).getCanonicalPath(), "-c", "-w", "net", "start", String.valueOf(daemon_name) + " Server" }, (String[])null, new File_S("./service/"));
        } else {
          proc = Runtime.getRuntime().exec(new String[] { "net", "stop", String.valueOf(daemon_name) + " Server" }, (String[])null, new File_S("./service/"));
        } 
      } else if (machine_is_x()) {
        RandomAccessFile out = new RandomAccessFile(new File_S(String.valueOf(daemon_name) + "_exec_root.sh"), "rw");
        out.setLength(0L);
        out.write(("launchctl start com.crushftp." + daemon_name + "\n").getBytes("UTF8"));
        out.close();
        exec(new String[] { "chmod", "+x", (new File_S(String.valueOf(daemon_name) + "_exec_root.sh")).getCanonicalPath() });
        proc = Runtime.getRuntime().exec(new String[] { "osascript", "-e", "do shell script \"" + (new File_S(String.valueOf(daemon_name) + "_exec_root.sh")).getCanonicalPath() + "\" with administrator privileges" });
      } 
      BufferedReader proc_in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
      String data = "";
      boolean ok = true;
      while ((data = proc_in.readLine()) != null) {
        results = String.valueOf(results) + data + "\r\n";
        if (data.toUpperCase().indexOf("ACCESS DENIED") >= 0 || data.toUpperCase().indexOf("NOT A VALID") >= 0)
          ok = false; 
        log("SERVER", 0, data);
      } 
      proc_in.close();
      proc_in = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
      data = "";
      while ((data = proc_in.readLine()) != null) {
        results = String.valueOf(results) + data + "\r\n";
        if (data.toUpperCase().indexOf("ACCESS DENIED") >= 0 || data.toUpperCase().indexOf("NOT A VALID") >= 0)
          ok = false; 
        log("SERVER", 0, data);
      } 
      if (ok && !silent) {
        JOptionPane.showMessageDialog(null, "Started\r\n\r\n" + results);
      } else if (!silent) {
        JOptionPane.showMessageDialog(null, "Failure:\r\n\r\n" + results);
      } 
    } catch (Exception e) {
      e.printStackTrace();
      if (!silent)
        JOptionPane.showMessageDialog(null, String.valueOf(e.toString()) + "\r\n\r\n" + results); 
    } 
    if ((new File_S(String.valueOf(daemon_name) + "_exec_root.sh")).exists())
      (new File_S(String.valueOf(daemon_name) + "_exec_root.sh")).delete(); 
    if ((new File_S("service/elevate.exe")).exists())
      (new File_S("service/elevate.exe")).delete(); 
    (new File_S("./service/elevate.exe")).delete();
  }
  
  public static void loadPersistentVariables() {
    if ((new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "persistent_job_variables.XML")).exists()) {
      Properties persistent_variables = (Properties)readXMLObject(String.valueOf(System.getProperty("crushftp.prefs")) + "persistent_job_variables.XML");
      System2.put("persistent_variables", persistent_variables);
      persistent_variables.put("time", (new StringBuffer(String.valueOf((new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "persistent_job_variables.XML")).lastModified()))).toString());
    } 
  }
  
  public static String encodeBase32(byte[] b) {
    return (new Base32()).encodeToString(b);
  }
  
  public static int totp_calculateCode(byte[] key, long time) throws Exception {
    byte[] data = new byte[8];
    long time2 = time;
    for (int i = 8; i-- > 0; time2 >>>= 8L)
      data[i] = (byte)(int)time2; 
    Mac mac = Mac.getInstance("HmacSHA1");
    mac.init(new SecretKeySpec(key, mac.getAlgorithm()));
    byte[] hash = mac.doFinal(data);
    int loc = hash[hash.length - 1] & 0xF;
    long shortened = 0L;
    for (int j = 0; j <= 3; j++) {
      shortened <<= 8L;
      shortened |= (hash[loc + j] & 0xFF);
    } 
    return (int)(shortened & 0x7FFFFFFFL) % (int)Math.pow(10.0D, 6.0D);
  }
  
  public static boolean totp_checkCode(String secret, long code, long l) throws Exception {
    byte[] decodedKey = (new Base32()).decode(secret.toUpperCase());
    int window = 10;
    for (int i = -((window - 1) / 2); i <= window / 2; i++) {
      if (totp_calculateCode(decodedKey, l / TimeUnit.SECONDS.toMillis(30L) + i) == code)
        return true; 
    } 
    return false;
  }
  
  public static boolean isNumeric(String s) {
    try {
      Long.parseLong(s.trim());
      return true;
    } catch (Exception exception) {
      return false;
    } 
  }
  
  public static byte[] getElevate() {
    String b64 = "";
    b64 = String.valueOf(b64) + "H4sIAAAAAAAA/+1XbWxbVxk+dtwPpx9uWTNaBNqt101lqhMnTkbS2CyV69DQZPGa1mm7Re2NfW58k+t73fuRD0akIrcIy4pWkBAS4sdAjTQ+JPojPwoClokxt4hN7AdjQoUVqT9apYhojdZUCr0859zrJKYt7aRJI";
    b64 = String.valueOf(b64) + "MSxzj3v13ne9z3n+J739hw/R2oIIT502ybkInFaB3l4u4S++YmfbyYz/rd3XvR0v73zcFY2hLyuDeliTkiLqqqZwiAVdEsVZFXY39sn5LQMrd+0qXaXizFWN/KjiLnmTqXPv3z0TjOnfXdinD9+p4mPL7rjCT4ekt";
    b64 = String.valueOf(b64) + "NZZl+JJZkgpNvjJe2PGz0V2VUS8GzwrKsltWDWOrLvbsNjC7rgZslor6P2YCXWViaHnUW58APC1ucMN2S2y+PywNu1TyMG4uCeXvMIi/eoDU72/xt1vUnHTYyn/W5AtU7cqxtCOlmvG3qauLFBwBPdXG3X8Wjb/v/";
    b64 = String.valueOf(b64) + "2P9TGtxPydfRvoZ9Hn0F/B/0K+ofoZAch23c4tq3uOIhxGD21YwVHwC9EniJpTj1FDFLPj+IRooLOEwqNTCR0SjKwoETHT0N37BLLvEBexPwwaSVHyYCrJRyRtX4iAsMEJwDNsTc5Wg5yFVoTowaqnXs5RSzwo5Ar";
    b64 = String.valueOf(b64) + "4FRomb2GZxAYJuQ6lzWQMRdZQKQa0HLgVcQadP1m+XwBeAb3NMQzZvk6UYTICNnj6ilGlrnMsTLgHx5NGh4zPBLL9fnx/GY4joonQ81z+yxHNDkGy8viq1zxnAGeznfG5FgTkA2Cd9aV8ojHud5aXtVK60YOFvg0x";
    b64 = String.valueOf(b64) + "2a7IXKvbCdYnAbfFfU+68ms2E6n+TyD+/o4qzNy3x26fzz3ngxn/T7ZiNIPODO9wK2sm0H2uv8HAzZDQNvLPTDcUR4b5Sd/t7u3X3V39vNcGgLCgEtZoCr0GKeqfTvXXRDyp51LruossMicyOvd3SUkzuf3uf9Sgu";
    b64 = String.valueOf(b64) + "wtvl7sH3ekdPOFlF23cUsHKc1HZtvnzQPJ5FTP0gvJVDJLHiOkmFiy69ZDfXb2+80YzFrbWsq+BmpubSr7DYyF2Y6kXRdl1JsbobTrfEz7ODeHMnsSo2tx7Niwx64joH4Hz1+7mcHV3JeKfNRftJaK1vWitVC0bha";
    b64 = String.valueOf(b64) + "t+Yht1+1iMb3f/pfiwGLgTACXtFSqkWICGfNL7W+ZT7ffNT8jxYLE8t7+YzwOo7kPuHYTZ5ht+1vW3zAOe09IG8pSrIEEzp4jDCbuhS5wpgBmqs8nFfYKY19ps5asJ0qXCpcFqXAnbfnLiSUP9HPHwY5Ya4qTS3O9";
    b64 = String.valueOf(b64) + "bdaCta1iozKbBW7zbJt1c0VhMcVNrvhcm3Xd2rpRkGJjhEmvc+m6cmKeEaU/3Thk23abNR84+2cIGPwaQJl+BBM481uIpLaI5W+bXAqc/QW44UBkFqpjU5Pnb/24nDgfBUg58VrHGjZc2LatA8MsQ7YOFgdmIperF";
    b64 = String.valueOf(b64) + "mx3+zvmLr5ggdvvgl5bSMx452qkYiQen/uDVGgXxmo5w3zOFwculgbmb/iA9jp79E+d/d4/bDuZ9cGLXbcFewPMQHvZ2OBoiokZltjMwmMdLJHA2VcQR2S2NLlYuGx/KP309rtd30yVFlPFyWt2ncCnFxPY2A9gVl";
    b64 = String.valueOf(b64) + "Oe2k92J3EOP8sVgTOXCTt584XZjaXFX10D5PR0YXbzdNK2FqXpYuKqA1HquTrl21fquQaj95nRVLffOz01vr70xhs3vKVbyODWNLblrzW3zkc+mqsp/HoBy1uO+9i/2lxfrmHv4rm15Rp2p0VmC5NLHqkcrxVI4Nu";
    b64 = String.valueOf(b64) + "zU77fsP+KVIyvx1pMJc7jBO/ZwnMrLVrr+pxTjkO4s826Yu7ARpsbhm3bumLXbYeCE9tApOaO3fjOkm2XWlrBZfMI036v7Nv1UoW49KkOMpx+/QC0/fZ72VbGjjDq1a2gVEbtZpTFqAuwGh4DVXgzOnzX/Sc1b1+5";
    b64 = String.valueOf(b64) + "p59hdzbqxJOrZF+GbA9k2VUyE7Lm1QXuJ1wz/IwkxmUzqWtpahjkJW+3lhaVTp1SskA6NT0nmj1QiEO0n5BTni9Rs1s0zISuazohxxifUEdlXVNzVDVToi6LgwpMm0lc0Qx6QFQzCiXnavpF2QRan6wOKbR3cJimT";
    b64 = String.valueOf(b64) + "XLU8bVPUTRs4D6GFbd0HTj7ZR0Gmj4Bn1u5XMvlANUtq8A+mDj0fKI70lSfURSU2Z6+LFWUxDhNWyZNjPeTvgOJ7or6olcay+uyakqEdHpOyNogIQdqKqKcMZrWTQfnv6t5K59Gp9k2tmKs1nsIe3skHyDP3kfu99";
    b64 = String.valueOf(b64) + "0jXJazl+2rcJb3rmi2PEmIvIrPe5vxTOF2OoFnghwC1YWb9XnwXXh2gmbtl76/33W+4Dycr4zPuTg+/Dz/EsXvPWxGH+5e3a0MOjGyG7kLHKuCCPkJtwnjZIVJGx8H+adhFB9PHvfmzPObdwLRiOAox67UffW4wxW";
    b64 = String.valueOf(b64) + "gWqBYXTEEnUqexNyKr/286kjzGPJVdVa86l4PocZhUTq15T5oBazHSv3gzGM6kyOzWnaC44TJ+lX+Uss1WsVPI2KLoId5Zx+BG2HfyyMacmsnZXm+upxldfWyUk+Mk2cwvxv0EJ/J8sgjFgcvS0w3t2qZQH6IfpBX";
    b64 = String.valueOf(b64) + "4QLPla2ZgEwV/IRVlgbnKK+zdfinvJpxvpM9yI/V9PfuJCG7+IfxYWhFXpEpq9aNNb/vgi/63HhOEUapbsiaGgs21oeDAlXTWgavkFjwyOHOUGtQMEy8FURFU2ksOEGN4HNf3FQbFQ2D5gaVCQEAqhELWrq610hna";
    b64 = String.valueOf(b64) + "U40Qjk5rWuGJpmhtJbbKxq5+tHGoIB3iyxRw0yt9sagTN0yzC5V0h4Rq4nN8kcNvIt02ZxgjD+q01MWwGkmqcujskKHqMEVqzTO2wuuu+koVbjSrzAyFhSNLnVUG6F60BFb8r40e1XHgpKoGNSRNjieGh7gKtqwKq";
    b64 = String.valueOf(b64) + "Jow3JWjEPsedGUB2UF6oenWWXOFo/ji/m8IqdFloETiWHl85qOQHr7hK5MLPgybQo3Nja3fCHU2NL8bKg5km4JiS0SDYXDrRlKaSQTkcKTQaHhAdMjLY2R1sE2MdSSacN0aTATaqVNmZDY1NwcbmpqQSkkVqZHG6r";
    b64 = String.valueOf(b64) + "DiTZUBc0llTMC5p534n+m/ROkCMvDABQAAA==";
    try {
      return Base64.decode(b64);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    } 
  }
  
  public static Properties google_renew_tokens(String refresh_token, String google_client_id, String google_client_secret) throws Exception {
    if (smtp_access_tokens.containsKey(String.valueOf(google_client_id) + "~" + google_client_secret + "~" + refresh_token)) {
      Properties acces_token = (Properties)smtp_access_tokens.get(String.valueOf(google_client_id) + "~" + google_client_secret + "~" + refresh_token);
      if (System.currentTimeMillis() - Long.parseLong(acces_token.getProperty("time")) < (Long.parseLong(acces_token.getProperty("expires_in")) - 300L) * 1000L)
        return acces_token; 
    } 
    String full_form = "client_id=" + google_client_id;
    full_form = String.valueOf(full_form) + "&client_secret=" + google_client_secret;
    full_form = String.valueOf(full_form) + "&refresh_token=" + refresh_token;
    full_form = String.valueOf(full_form) + "&grant_type=refresh_token";
    URLConnection urlc = URLConnection.openConnection(new VRL("https://oauth2.googleapis.com/token"), new Properties());
    urlc.setDoOutput(true);
    urlc.setRequestMethod("POST");
    OutputStream out = urlc.getOutputStream();
    out.write(full_form.getBytes("UTF8"));
    out.close();
    int code = urlc.getResponseCode();
    String result = consumeResponse(urlc.getInputStream());
    if (code < 200 || code > 299)
      throw new IOException(result); 
    Properties p = parse_json_reply(result);
    if (p.containsKey("access_token")) {
      String expire_in = p.getProperty("expires_in");
      if (expire_in.endsWith(","))
        expire_in = expire_in.substring(0, expire_in.length() - 1); 
      p.put("expires_in", expire_in);
      p.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      smtp_access_tokens.put(String.valueOf(google_client_id) + "~" + google_client_secret + "~" + refresh_token, p);
    } 
    return p;
  }
  
  public static Properties google_get_refresh_token(String oauth_access_code, String server_url, String google_client_id, String google_client_secret) throws Exception {
    try {
      String full_form = "code=" + URLEncoder.encode(oauth_access_code, "UTF-8");
      full_form = String.valueOf(full_form) + "&client_id=" + google_client_id;
      full_form = String.valueOf(full_form) + "&client_secret=" + google_client_secret;
      full_form = String.valueOf(full_form) + "&redirect_uri=" + server_url;
      full_form = String.valueOf(full_form) + "&grant_type=authorization_code";
      byte[] b = full_form.getBytes("UTF8");
      URLConnection urlc = URLConnection.openConnection(new VRL("https://oauth2.googleapis.com/token"), new Properties());
      urlc.setDoOutput(true);
      urlc.setRequestMethod("POST");
      OutputStream out = urlc.getOutputStream();
      out.write(full_form.getBytes("UTF8"));
      out.close();
      String result = consumeResponse(urlc.getInputStream());
      int code = urlc.getResponseCode();
      if (code < 200 || code > 299)
        throw new Exception("Error :" + result); 
      JSONObject obj = (JSONObject)JSONValue.parse(result);
      Properties p = new Properties();
      if (obj.containsKey("refresh_token") && obj.containsKey("access_token")) {
        String refresh_token = (String)obj.get("refresh_token");
        String access_token = (String)obj.get("access_token");
        p.put("refresh_token", refresh_token);
        p.put("access_token", access_token);
        return p;
      } 
      p.put("error", result);
      return p;
    } catch (Exception e) {
      log("SERVER", 1, e);
      throw e;
    } 
  }
  
  public static Properties get_smtp_oauth_refresh_token(String oauth_url, String oauth_access_code, String server_url, String oauth_client_id, String oauth_client_secret, String oauth_client_scope) {
    try {
      String full_form = "&client_id=" + oauth_client_id;
      full_form = String.valueOf(full_form) + "&code=" + oauth_access_code;
      full_form = String.valueOf(full_form) + "&scope=" + oauth_client_scope.replaceAll(" ", "%20");
      full_form = String.valueOf(full_form) + "&redirect_uri=" + server_url;
      full_form = String.valueOf(full_form) + "&grant_type=authorization_code";
      full_form = String.valueOf(full_form) + "&client_secret=" + oauth_client_secret;
      byte[] b = full_form.getBytes("UTF8");
      URLConnection urlc = URLConnection.openConnection(new VRL(String.valueOf(oauth_url) + "token"), new Properties());
      urlc.setDoOutput(true);
      urlc.setRequestMethod("POST");
      OutputStream out = urlc.getOutputStream();
      out.write(full_form.getBytes("UTF8"));
      out.close();
      urlc.getResponseCode();
      String result = consumeResponse(urlc.getInputStream());
      String refresh_token = (String)((JSONObject)JSONValue.parse(result)).get("refresh_token");
      String access_token = (String)((JSONObject)JSONValue.parse(result)).get("access_token");
      Properties p = new Properties();
      p.put("refresh_token", refresh_token);
      p.put("access_token", access_token);
      return p;
    } catch (Exception e) {
      log("SERVER", 1, e);
      return null;
    } 
  }
  
  public static Properties smtp_oauth_renew_tokens(String refresh_token, String oauth_client_id, String oauth_client_secret, String oauth_url) throws Exception {
    String full_form = "client_id=" + oauth_client_id;
    full_form = String.valueOf(full_form) + "&client_secret=" + oauth_client_secret;
    full_form = String.valueOf(full_form) + "&refresh_token=" + refresh_token;
    full_form = String.valueOf(full_form) + "&grant_type=refresh_token";
    URLConnection urlc = URLConnection.openConnection(new VRL(String.valueOf(oauth_url) + "token"), new Properties());
    urlc.setDoOutput(true);
    urlc.setRequestMethod("POST");
    urlc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    OutputStream out = urlc.getOutputStream();
    out.write(full_form.getBytes("UTF8"));
    out.close();
    int code = urlc.getResponseCode();
    String result = consumeResponse(urlc.getInputStream());
    if (code < 200 || code > 299)
      throw new IOException(result); 
    String access_token = (String)((JSONObject)JSONValue.parse(result)).get("access_token");
    Properties p = new Properties();
    p.put("refresh_token", refresh_token);
    p.put("access_token", access_token);
    return p;
  }
  
  public static Properties parse_json_reply(String result) {
    BufferedReader br = new BufferedReader(new StringReader(result));
    String line = "";
    Properties p = new Properties();
    try {
      while ((line = br.readLine()) != null) {
        if (line.indexOf(":") < 0)
          continue; 
        String key = line.split(":")[0].trim();
        if (key.indexOf("\"") >= 0)
          key = key.substring(1, key.lastIndexOf("\"")); 
        String val = line.split(":")[1].trim();
        if (val.indexOf("\"") >= 0)
          val = val.substring(1, val.lastIndexOf("\"")); 
        p.put(key, val);
      } 
    } catch (Exception exception) {}
    return p;
  }
  
  public static String xss_strip(String keyword) {
    keyword = url_decode(keyword);
    if (keyword.toUpperCase().indexOf("<") >= 0 && keyword.toUpperCase().indexOf(">") >= 0)
      keyword = "INVALID"; 
    if (keyword.toUpperCase().indexOf("ONKEY") >= 0 && keyword.toUpperCase().indexOf("(") >= 0)
      keyword = "INVALID"; 
    if (keyword.toUpperCase().indexOf("ONMOUSE") >= 0 && keyword.toUpperCase().indexOf("(") >= 0)
      keyword = "INVALID"; 
    if (keyword.toUpperCase().indexOf("ONCLICK") >= 0 && keyword.toUpperCase().indexOf("(") >= 0)
      keyword = "INVALID"; 
    if (keyword.toUpperCase().indexOf("ONDBCLICK") >= 0 && keyword.toUpperCase().indexOf("(") >= 0)
      keyword = "INVALID"; 
    if (keyword.toUpperCase().indexOf("ONCONTEXT") >= 0 && keyword.toUpperCase().indexOf("(") >= 0)
      keyword = "INVALID"; 
    if (keyword.toUpperCase().indexOf("ONFOCUS") >= 0 && keyword.toUpperCase().indexOf("(") >= 0)
      keyword = "INVALID"; 
    if (keyword.toUpperCase().indexOf("ONCHANGE") >= 0 && keyword.toUpperCase().indexOf("(") >= 0)
      keyword = "INVALID"; 
    if (keyword.toUpperCase().indexOf("ONLOAD") >= 0 && keyword.toUpperCase().indexOf("(") >= 0)
      keyword = "INVALID"; 
    if (keyword.toUpperCase().indexOf("EVAL") >= 0 && keyword.toUpperCase().indexOf("(") >= 0)
      keyword = "INVALID"; 
    if (keyword.toUpperCase().indexOf("FUNCTION") >= 0 && keyword.toUpperCase().indexOf("(") >= 0)
      keyword = "INVALID"; 
    if (keyword.toUpperCase().indexOf("<SCRIPT") >= 0)
      keyword = "INVALID"; 
    return keyword;
  }
}
