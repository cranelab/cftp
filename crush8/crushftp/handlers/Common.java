package crushftp.handlers;

import com.crushftp.client.AS2Client;
import com.crushftp.client.AzureClient;
import com.crushftp.client.Base64;
import com.crushftp.client.CustomClient;
import com.crushftp.client.FTPClient;
import com.crushftp.client.FileClient;
import com.crushftp.client.File_B;
import com.crushftp.client.File_S;
import com.crushftp.client.File_U;
import com.crushftp.client.GDriveClient;
import com.crushftp.client.GenericClient;
import com.crushftp.client.HTTPClient;
import com.crushftp.client.HadoopClient;
import com.crushftp.client.Mailer;
import com.crushftp.client.MemoryClient;
import com.crushftp.client.OutputStreamCloser;
import com.crushftp.client.RFileClient;
import com.crushftp.client.S3Client;
import com.crushftp.client.S3CrushClient;
import com.crushftp.client.SFTPClient;
import com.crushftp.client.SMB3Client;
import com.crushftp.client.SMBClient;
import com.crushftp.client.TrustManagerCustom;
import com.crushftp.client.VRL;
import com.crushftp.client.WebDAVClient;
import com.crushftp.client.Worker;
import com.crushftp.client.ZipClient;
import com.crushftp.tunnel2.Tunnel2;
import com.didisoft.pgp.PGPLib;
import com.didisoft.pgp.inspect.PGPInspectLib;
import crushftp.db.SearchTools;
import crushftp.db.StatTools;
import crushftp.gui.LOC;
import crushftp.server.QuickConnect;
import crushftp.server.RETR_handler;
import crushftp.server.STOR_handler;
import crushftp.server.ServerStatus;
import crushftp.server.VFS;
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
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.XSLTransformer;

public class Common {
  public static Object writeLock = new Object();
  
  static String CRLF = "\r\n";
  
  static Properties pluginCache = new Properties();
  
  static Properties pluginCacheTime = new Properties();
  
  static String registration_name = "";
  
  static String registration_email = "";
  
  static String registration_code = "";
  
  public static boolean base64Decode = true;
  
  static PGPLib pgp = null;
  
  public static boolean addedBC = false;
  
  static Vector local_ips = null;
  
  public static Object onserversocklock = new Object();
  
  static SecureRandom rn = new SecureRandom();
  
  static {
    if (!addedBC) {
      addedBC = true;
      try {
        Security.setProperty("crypto.policy", "unlimited");
      } catch (Exception exception) {}
      Security.addProvider((Provider)new BouncyCastleProvider());
    } 
    rn.nextBytes((String.valueOf(Runtime.getRuntime().maxMemory()) + Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory()).getBytes());
  }
  
  public static int V() {
    return 8;
  }
  
  public boolean register(String registration_name, String registration_email, String registration_code) {
    Common.registration_name = registration_name.toUpperCase().trim();
    Common.registration_email = registration_email.toUpperCase().trim();
    Common.registration_code = registration_code;
    DesEncrypter crypt = new DesEncrypter("crushftp:" + registration_name + registration_email, false);
    String s = crypt.decrypt(registration_code);
    if (s == null || !s.startsWith("(")) {
      crypt = new DesEncrypter("crushftp:" + registration_name + registration_email, true);
      s = crypt.decrypt(registration_code);
    } 
    if (s != null && s.startsWith("(") && s.indexOf(")") > s.indexOf("(")) {
      int pos = s.indexOf("(DATE:");
      if (pos >= 0)
        try {
          SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
          Date d = sdf.parse(s.substring(pos + "(DATE:".length(), s.indexOf(")", pos)));
          if ((new Date()).getTime() > d.getTime()) {
            Log.log("SERVER", 0, "Temporary license has expired.");
            return false;
          } 
          Log.log("SERVER", 0, "Temporary license will expire on:" + d.toString());
        } catch (Exception e) {
          Log.log("SERVER", 1, e);
        }  
      return true;
    } 
    if (s != null && s.startsWith("[") && s.indexOf("]") > s.indexOf("[")) {
      int pos = s.indexOf("[DATE:");
      if (pos >= 0)
        try {
          SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
          Date d = sdf.parse(s.substring(pos + "[DATE:".length(), s.indexOf("]", pos)));
          if ((new Date()).getTime() > d.getTime()) {
            Log.log("SERVER", 0, "Temporary license has expired.");
            return false;
          } 
          Log.log("SERVER", 0, "Temporary license will expire on:" + d.toString());
        } catch (Exception e) {
          Log.log("SERVER", 1, e);
        }  
      return true;
    } 
    return false;
  }
  
  public String getRegistrationAccess(String key, String code) {
    DesEncrypter crypt = new DesEncrypter("crushftp:" + registration_name + registration_email, base64Decode);
    String s = crypt.decrypt(code);
    if (s == null) {
      crypt = new DesEncrypter("crushftp:" + registration_name + registration_email, false);
      s = crypt.decrypt(code);
    } 
    if (findRegistrationValue(key, s, "(", ")") != null) {
      s = findRegistrationValue(key, s, "(", ")");
    } else {
      s = findRegistrationValue(key, s, "[", "]");
    } 
    String blackList = "";
    blackList = String.valueOf(blackList) + "gZZZVip+1PUm/FwxLE1+/ogwcLA9pzLD\r\n";
    blackList = String.valueOf(blackList) + "mnhVl3sUUqUFADJukeYLvA==\r\n";
    blackList = String.valueOf(blackList) + "4RJ6S3zfM5URyl8YQBdhpw==\r\n";
    blackList = String.valueOf(blackList) + "eNul0xzgonKJ6pGWrU7XmdKkG/J4Rvec\r\n";
    blackList = String.valueOf(blackList) + "4oRzZ/kng2mL8DOCmNOe0g==\r\n";
    blackList = String.valueOf(blackList) + "N+WrqZQ9IAcAUo4pY2kqQqmpwcdKX9MK\r\n";
    blackList = String.valueOf(blackList) + "UkayxZ/KNlO8N+BekoHkgXvIsl12DopG\r\n";
    blackList = String.valueOf(blackList) + "6hB28j0II/3FjVimQ2yMhzyFUYZ5xK55\r\n";
    if (registration_name.equalsIgnoreCase("KCN") || registration_email.equalsIgnoreCase("CREW") || registration_email.toUpperCase().indexOf("TEAMARN") >= 0 || blackList.indexOf(code) >= 0)
      (new Thread(new null.waiting(this))).start(); 
    return s;
  }
  
  public String findRegistrationValue(String key, String s, String c1, String c2) {
    if (s.toUpperCase().indexOf(String.valueOf(c1) + key.toUpperCase() + "=") >= 0) {
      s = s.substring(s.toUpperCase().indexOf(String.valueOf(c1) + key.toUpperCase() + "="), s.indexOf(c2, s.toUpperCase().indexOf(String.valueOf(c1) + key.toUpperCase() + "=")));
      s = s.substring(("(" + key.toUpperCase() + "=").length());
      if (s.equals("100"))
        s = "32768"; 
      return s;
    } 
    return null;
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
    if (System.getProperties().getProperty("os.name", "").toUpperCase().equals("MAC OS X"))
      return true; 
    return false;
  }
  
  public static boolean machine_is_x_10_5_plus() {
    if (machine_is_x()) {
      String[] version = System.getProperties().getProperty("os.version", "").split("\\.");
      if (Integer.parseInt(version[0]) >= 10 && Integer.parseInt(version[1]) >= 5)
        return true; 
    } 
    return false;
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
  
  public static boolean machine_is_linux() {
    if (System.getProperties().getProperty("os.name", "").toUpperCase().indexOf("LINUX") >= 0)
      return true; 
    if (System.getProperties().getProperty("os.name", "").toUpperCase().indexOf("HP-UX") >= 0)
      return true; 
    return false;
  }
  
  public static boolean machine_is_unix() {
    if (System.getProperties().getProperty("os.name", "").toUpperCase().indexOf("UNIX") >= 0)
      return true; 
    return false;
  }
  
  public static boolean machine_is_windows() {
    if (System.getProperties().getProperty("os.name", "").toUpperCase().indexOf("NDOWS") >= 0)
      return true; 
    return false;
  }
  
  public static boolean machine_is_solaris() {
    try {
      if (System.getProperties().getProperty("os.name").toUpperCase().indexOf("SUNOS") >= 0)
        return true; 
      return false;
    } catch (Exception exception) {
      return false;
    } 
  }
  
  public static boolean debug(int level, String s) {
    return Log.log("GENERAL", level, s);
  }
  
  public static boolean debug(int level, Throwable e) {
    return Log.log("GENERAL", level, e);
  }
  
  public static boolean debug(int level, Exception e) {
    return Log.log("GENERAL", level, e);
  }
  
  public static String format_string(String heading, String c) {
    String total_string = "";
    String original_string = String.valueOf(c.trim()) + CRLF;
    c = original_string.substring(0, original_string.indexOf("\r")).trim();
    while (c.length() > 0) {
      total_string = String.valueOf(total_string) + heading + c + CRLF;
      original_string = String.valueOf(original_string.substring(original_string.indexOf("\r"), original_string.length()).trim()) + CRLF;
      c = original_string.substring(0, original_string.indexOf("\r")).trim();
    } 
    return total_string;
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
  
  public static void setFileText(String s, String file) throws IOException {
    RandomAccessFile f = new RandomAccessFile(new File_S(file), "rw");
    f.setLength(0L);
    f.write(s.getBytes("UTF8"));
    f.close();
  }
  
  public static String getFileText(String file) throws IOException {
    if (!(new File_S(file)).exists())
      return null; 
    RandomAccessFile f = new RandomAccessFile(new File_S(file), "r");
    byte[] b = new byte[(int)f.length()];
    f.readFully(b);
    f.close();
    return new String(b, "UTF8");
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
      String s2 = s.replace('+', 'þ');
      s2 = URLDecoder.decode(s2, "UTF8");
      s = s2.replace('þ', '+');
    } catch (Exception e) {
      try {
        String s2 = "";
        for (int i = 0; i < s.length(); i++) {
          if (s.charAt(i) == '%' && i + 3 < s.length()) {
            try {
              s2 = String.valueOf(s2) + URLDecoder.decode(s.substring(i, i + 3), "UTF8");
              i += 2;
            } catch (IllegalArgumentException ee) {
              s2 = String.valueOf(s2) + s.charAt(i);
            } 
          } else {
            s2 = String.valueOf(s2) + s.charAt(i);
          } 
        } 
        s = s2;
      } catch (Exception ee) {
        Log.log("SERVER", 0, ee);
      } 
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
      return_str = URLEncoder.encode(master_string, "utf-8");
      for (int x = 0; x < OK_chars.length(); x++) {
        String s = URLEncoder.encode((new StringBuffer(String.valueOf(OK_chars.charAt(x)))).toString(), "utf-8");
        return_str = replace_str(return_str, s, (new StringBuffer(String.valueOf(OK_chars.charAt(x)))).toString());
      } 
      return_str = return_str.replaceAll("\\+", "%20");
      return return_str;
    } catch (Exception exception) {
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
  
  public static int count_str(String master_string, String search_data) {
    int count = 0;
    int start_loc = 0;
    int end_loc = 0;
    try {
      start_loc = master_string.indexOf(search_data);
      while (start_loc >= 0) {
        count++;
        end_loc = start_loc + search_data.length();
        start_loc = master_string.indexOf(search_data, end_loc);
      } 
    } catch (Exception exception) {}
    return count;
  }
  
  public String discover_ip() {
    Socket get_ip_socket = null;
    try {
      get_ip_socket = new Socket();
      get_ip_socket.setSoTimeout(5000);
      get_ip_socket.connect(new InetSocketAddress("checkip.dyndns.org", 80));
      BufferedReader gis = new BufferedReader(new InputStreamReader(get_ip_socket.getInputStream()));
      BufferedOutputStream gos = new BufferedOutputStream(get_ip_socket.getOutputStream());
      gos.write(("GET / HTTP/1.0" + CRLF + CRLF).getBytes("UTF8"));
      gos.flush();
      String data = "";
      while (data.toUpperCase().indexOf("ADDRESS") < 0) {
        data = gis.readLine();
        if (data == null)
          throw new IOException("no response from server"); 
      } 
      gis.close();
      gos.close();
      get_ip_socket.close();
      String the_ip = String.valueOf(data) + CRLF;
      int start_loc = the_ip.indexOf("Address:") + 8;
      int endLoc = the_ip.indexOf("\n", start_loc);
      the_ip = the_ip.substring(start_loc, endLoc).trim();
      if (the_ip.indexOf("<") >= 0)
        the_ip = the_ip.substring(0, the_ip.indexOf("<")).trim(); 
      Log.log("SERVER", 2, "Auto IP lookup:" + the_ip);
      if (the_ip.indexOf("0.0.0.0") < 0)
        return the_ip; 
    } catch (IOException ee) {
      Log.log("SERVER", 1, ee);
    } finally {
      if (get_ip_socket != null)
        try {
          get_ip_socket.close();
        } catch (Exception exception) {} 
    } 
    try {
      try {
        Thread.sleep(1000L);
      } catch (Exception exception) {}
      get_ip_socket = new Socket();
      get_ip_socket.setSoTimeout(5000);
      get_ip_socket.connect(new InetSocketAddress("www.crushftp.com", 80));
      BufferedReader gis = new BufferedReader(new InputStreamReader(get_ip_socket.getInputStream()));
      BufferedOutputStream gos = new BufferedOutputStream(get_ip_socket.getOutputStream());
      gos.write(("GET /ip.jsp HTTP/1.0" + CRLF + CRLF).getBytes("UTF8"));
      gos.flush();
      String data = "";
      while (data.toUpperCase().indexOf("ADDRESS") < 0) {
        data = gis.readLine();
        if (data == null)
          throw new IOException("no response from server"); 
      } 
      gis.close();
      gos.close();
      get_ip_socket.close();
      String the_ip = String.valueOf(data) + CRLF;
      int start_loc = the_ip.indexOf("Address:") + 8;
      int endLoc = the_ip.indexOf("\n", start_loc);
      the_ip = the_ip.substring(start_loc, endLoc).trim();
      if (the_ip.indexOf("<") >= 0)
        the_ip = the_ip.substring(0, the_ip.indexOf("<")).trim(); 
      Log.log("SERVER", 2, "Auto IP lookup:" + the_ip);
      return the_ip;
    } catch (IOException ee) {
      Log.log("SERVER", 1, ee);
    } finally {
      if (get_ip_socket != null)
        try {
          get_ip_socket.close();
        } catch (Exception exception) {} 
    } 
    return "0.0.0.0";
  }
  
  public String encode_pass(String raw, String method) {
    DesEncrypter crypt = new DesEncrypter("crushftp", base64Decode);
    String s = crypt.encrypt(raw, method, base64Decode, "");
    return s;
  }
  
  public String encode_pass(String raw, String method, String salt) {
    DesEncrypter crypt = new DesEncrypter("crushftp", base64Decode);
    String s = crypt.encrypt(raw, method, base64Decode, salt);
    return s;
  }
  
  public String crypt3(String raw, String hash) {
    if (hash.startsWith("CRYPT3:"))
      return "CRYPT3:" + Crypt3.crypt(hash.substring(7, 9), raw); 
    return "";
  }
  
  public String bcrypt(String raw, String hash) {
    if (hash.startsWith("BCRYPT:"))
      return "BCRYPT:" + BCrypt.hashpw(raw, hash.substring(7)); 
    return "";
  }
  
  public String pbkdf2sha256(String plaintext_password, String existing_hashed) {
    if (existing_hashed.startsWith("PBKDF2SHA256:"))
      return "PBKDF2SHA256:" + PBKDF2WithHmacSHA256.hashpw(plaintext_password, existing_hashed.substring("PBKDF2SHA256:".length())); 
    return "";
  }
  
  public String md5crypt(String raw, String hash) {
    if (hash.startsWith("MD5CRYPT:"))
      return "MD5CRYPT:" + MD5Crypt.crypt(raw, hash.substring(12, 20)); 
    return "";
  }
  
  public String sha512crypt(String raw, String hash) {
    if (hash.startsWith("SHA512CRYPT:"))
      return "SHA512CRYPT:" + SHA512Crypt.Sha512_crypt(raw, hash.substring("SHA512CRYPT:".length()), 0); 
    return "";
  }
  
  public String decode_pass(String raw) {
    DesEncrypter crypt = new DesEncrypter("crushftp", base64Decode);
    String s = crypt.decrypt(raw);
    if (s == null) {
      crypt = new DesEncrypter("crushftp", false);
      s = crypt.decrypt(raw);
    } 
    if (s == null)
      s = decode_pass3(raw); 
    return s;
  }
  
  public static String encode_pass3(String the_raw_password) {
    if (the_raw_password == null)
      return ""; 
    if (the_raw_password.equals(""))
      return ""; 
    String new_pass = "";
    for (int parse_loc = 0; parse_loc < the_raw_password.length(); parse_loc++) {
      int the_char = the_raw_password.charAt(parse_loc);
      the_char += the_raw_password.length() + parse_loc + 13;
      while (the_char > 127)
        the_char -= 106; 
      new_pass = String.valueOf(new_pass) + (char)the_char;
    } 
    new_pass = url_encode(new_pass);
    return new_pass;
  }
  
  public static String decode_pass3(String the_encoded_password) {
    if (the_encoded_password == null)
      return ""; 
    if (the_encoded_password.equals(""))
      return ""; 
    the_encoded_password = replace_str(the_encoded_password, "%0D", "\r\n");
    the_encoded_password = url_decode3(the_encoded_password);
    String new_pass = "";
    for (int parse_loc = 0; parse_loc < the_encoded_password.length(); parse_loc++) {
      int the_char = the_encoded_password.charAt(parse_loc);
      the_char -= the_encoded_password.length() + parse_loc + 13;
      while (the_char < 22)
        the_char += 106; 
      new_pass = String.valueOf(new_pass) + (char)the_char;
    } 
    return new_pass;
  }
  
  public static boolean check_local_ip(String ip_check_str) throws Exception {
    if (local_ips == null) {
      local_ips = new Vector();
      Properties ip_item = new Properties();
      ip_item.put("type", "A");
      ip_item.put("start_ip", "192.168.0.0");
      ip_item.put("stop_ip", "192.168.255.255");
      local_ips.addElement(ip_item);
      ip_item = new Properties();
      ip_item.put("type", "A");
      ip_item.put("start_ip", "172.16.0.0");
      ip_item.put("stop_ip", "172.31.255.255");
      local_ips.addElement(ip_item);
      ip_item = new Properties();
      ip_item.put("type", "A");
      ip_item.put("start_ip", "10.0.0.0");
      ip_item.put("stop_ip", "10.255.255.255");
      local_ips.addElement(ip_item);
    } 
    return check_ip(local_ips, ip_check_str);
  }
  
  public static boolean check_ip(Vector allow_list, String ip_check_str) throws Exception {
    boolean allow_all = !(allow_list != null && allow_list.size() != 0);
    try {
      for (int x = 0; x < allow_list.size(); x++) {
        Properties ip_data = allow_list.elementAt(x);
        if (ip_check_str.indexOf(".") >= 0) {
          allow_all = (ip_data.getProperty("type", "A").equals("A") && ip_data.getProperty("start_ip", "0.0.0.0").equals("0.0.0.0") && ip_data.getProperty("stop_ip", "255.255.255.255").equals("255.255.255.255") && allow_list.size() == 1);
        } else if (ip_check_str.indexOf(":") >= 0) {
          allow_all = (ip_data.getProperty("type", "A").equals("A") && ipv6_num(ip_data.getProperty("start_ip", "::0")).equals(ipv6_num("::0")) && ipv6_num(ip_data.getProperty("stop_ip", "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")).equals(ipv6_num("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")) && allow_list.size() == 1);
        } 
        if (ip_check_str.contains(".") && ip_data.getProperty("start_ip", "").indexOf(".") >= 0) {
          Boolean is_ip_allowed = is_ip_allowed(ip_check_str, ip_data);
          if (is_ip_allowed != null)
            return is_ip_allowed.booleanValue(); 
        } else if (ip_check_str.contains(":")) {
          String start_ip = ip_data.getProperty("start_ip");
          String stop_ip = ip_data.getProperty("stop_ip");
          if (start_ip.equals("0.0.0.0") && stop_ip.equals("255.255.255.255") && ip_data.getProperty("type").equals("A")) {
            start_ip = "::0";
            stop_ip = "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff";
          } 
          if (start_ip.indexOf(":") >= 0 && is_ipv6_in_range(ip_check_str, start_ip, stop_ip)) {
            if (ip_data.getProperty("type").equals("A"))
              return true; 
            return false;
          } 
        } 
      } 
    } catch (Exception e) {
      if (e.indexOf("Interrupted") >= 0)
        throw e; 
    } 
    return allow_all;
  }
  
  public static boolean is_ipv6_in_range(String ipv6, String start_ipv6, String end_ipv6) {
    BigInteger ip6_num = ipv6_num(ipv6);
    BigInteger start_ip_num = ipv6_num(start_ipv6);
    BigInteger end_ip_num = ipv6_num(end_ipv6);
    if (start_ip_num.compareTo(ip6_num) <= 0 && end_ip_num.compareTo(ip6_num) >= 0)
      return true; 
    return false;
  }
  
  public static BigInteger ipv6_num(String ipv6) {
    try {
      return new BigInteger(1, Inet6Address.getByName(ipv6).getAddress());
    } catch (UnknownHostException e) {
      Log.log("SERVER", 0, e);
      return new BigInteger("-1");
    } 
  }
  
  public static Boolean is_ip_allowed(String ip_check_str, Properties ip_data) {
    long part1_start, part2_start, part3_start, part1_end, part2_end, part3_end, part4 = 0L;
    long part4_start = 0L;
    long part4_end = 0L;
    int part4_loc = 0;
    int part1_loc = ip_check_str.indexOf(".");
    int part2_loc = ip_check_str.indexOf(".", part1_loc + 1);
    int part3_loc = ip_check_str.indexOf(".", part2_loc + 1);
    long part1 = Long.parseLong(ip_check_str.substring(0, part1_loc));
    long part2 = Long.parseLong(ip_check_str.substring(part1_loc + 1, part2_loc));
    long part3 = Long.parseLong(ip_check_str.substring(part2_loc + 1, part3_loc));
    part4 = Long.parseLong(ip_check_str.substring(part3_loc + 1, ip_check_str.length()));
    String ip_str = ip_data.getProperty("start_ip");
    part1_loc = ip_str.indexOf(".");
    part2_loc = ip_str.indexOf(".", part1_loc + 1);
    part3_loc = ip_str.indexOf(".", part2_loc + 1);
    try {
      part1_start = Long.parseLong(ip_str.substring(0, part1_loc));
    } catch (Exception e) {
      part1_start = 0L;
    } 
    try {
      part2_start = Long.parseLong(ip_str.substring(part1_loc + 1, part2_loc));
    } catch (Exception e) {
      part2_start = 0L;
    } 
    try {
      part3_start = Long.parseLong(ip_str.substring(part2_loc + 1, part3_loc));
    } catch (Exception e) {
      part3_start = 0L;
    } 
    try {
      part4_start = Long.parseLong(ip_str.substring(part3_loc + 1, ip_str.length()));
    } catch (Exception e) {
      part4_start = 0L;
    } 
    ip_str = ip_data.getProperty("stop_ip");
    part1_loc = ip_str.indexOf(".");
    part2_loc = ip_str.indexOf(".", part1_loc + 1);
    part3_loc = ip_str.indexOf(".", part2_loc + 1);
    part4_loc = ip_str.length();
    try {
      part1_end = Long.parseLong(ip_str.substring(0, part1_loc));
    } catch (Exception e) {
      part1_end = 255L;
    } 
    try {
      part2_end = Long.parseLong(ip_str.substring(part1_loc + 1, part2_loc));
    } catch (Exception e) {
      part2_end = 255L;
    } 
    try {
      part3_end = Long.parseLong(ip_str.substring(part2_loc + 1, part3_loc));
    } catch (Exception e) {
      part3_end = 255L;
    } 
    try {
      part4_end = Long.parseLong(ip_str.substring(part3_loc + 1, part4_loc));
    } catch (Exception e) {
      part4_end = 255L;
    } 
    long the_ip = (part1 << 24L) + (part2 << 16L) + (part3 << 8L) + part4;
    long the_ip_start = (part1_start << 24L) + (part2_start << 16L) + (part3_start << 8L) + part4_start;
    long the_ip_end = (part1_end << 24L) + (part2_end << 16L) + (part3_end << 8L) + part4_end;
    if (the_ip >= the_ip_start && the_ip <= the_ip_end)
      return ip_data.getProperty("type", "A").equals("A") ? Boolean.TRUE : Boolean.FALSE; 
    return null;
  }
  
  public void remove_expired_bans(Vector ip_list) {
    Vector temp = new Vector();
    for (int x = ip_list.size() - 1; x >= 0; x--) {
      Properties ip_data = ip_list.elementAt(x);
      if (ip_data.getProperty("type").toUpperCase().equals("T")) {
        long timer = Long.parseLong(ip_data.getProperty("timeout", "0"));
        if (timer < (new Date()).getTime()) {
          ip_list.remove(ip_data);
          QuickConnect.ip_cache.clear();
        } 
      } 
      if (ServerStatus.BG("v9_beta") && ip_data.getProperty("type").toUpperCase().equals("D"))
        temp.add(ip_data); 
    } 
    if (ServerStatus.BG("v9_beta") && ServerStatus.IG("max_denied_ips") < temp.size())
      ip_list.removeAll(temp.subList(0, temp.size() - ServerStatus.IG("max_denied_ips"))); 
  }
  
  public boolean check_date_expired_roll(String account_expire_field_str) {
    return check_date_expired(account_expire_field_str, System.currentTimeMillis());
  }
  
  public boolean check_date_expired(String expire_field_str, long time) {
    if (expire_field_str == null)
      return false; 
    if (expire_field_str.equals("") || expire_field_str.equals("0") || expire_field_str.equals("account_expire"))
      return false; 
    try {
      if (get_expired_date_format(expire_field_str).parse(expire_field_str).getTime() < time)
        return true; 
    } catch (Exception e) {
      return true;
    } 
    return false;
  }
  
  public SimpleDateFormat get_expired_date_format(String expire_field_str) {
    SimpleDateFormat sdf = null;
    if (expire_field_str != null && expire_field_str.indexOf("/") >= 0 && expire_field_str.indexOf(":") >= 0) {
      if (expire_field_str.substring(expire_field_str.lastIndexOf("/") + 1, expire_field_str.indexOf(" ")).length() == 2 && expire_field_str.indexOf(":") == expire_field_str.lastIndexOf(":"))
        sdf = new SimpleDateFormat("MM/dd/yy hh:mm aa", Locale.US); 
      if (expire_field_str.substring(expire_field_str.lastIndexOf("/") + 1, expire_field_str.indexOf(" ")).length() == 4 && expire_field_str.indexOf(":") == expire_field_str.lastIndexOf(":"))
        sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm aa", Locale.US); 
      if (expire_field_str.substring(expire_field_str.lastIndexOf("/") + 1, expire_field_str.indexOf(" ")).length() == 4 && expire_field_str.indexOf(":") != expire_field_str.lastIndexOf(":"))
        sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa", Locale.US); 
    } else if (expire_field_str != null && expire_field_str.indexOf("/") >= 0) {
      sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
    } else {
      sdf = new SimpleDateFormat("MMddyyyyHHmm", Locale.US);
    } 
    return sdf;
  }
  
  public static boolean check_day_of_week(String allow_list, Date the_day) {
    String allow_date = "none";
    String today_date = (new SimpleDateFormat("EEE", Locale.US)).format(the_day).toUpperCase();
    if (today_date.equals("SUN"))
      allow_date = "1"; 
    if (today_date.equals("MON"))
      allow_date = "2"; 
    if (today_date.equals("TUE"))
      allow_date = "3"; 
    if (today_date.equals("WED"))
      allow_date = "4"; 
    if (today_date.equals("THU"))
      allow_date = "5"; 
    if (today_date.equals("FRI"))
      allow_date = "6"; 
    if (today_date.equals("SAT"))
      allow_date = "7"; 
    if (allow_list.indexOf(allow_date) >= 0)
      return true; 
    return false;
  }
  
  public static int check_protocol(String protocol, String allowed_protocols) {
    String[] aps = allowed_protocols.split(",");
    for (int x = 0; x < aps.length; x++) {
      if (aps[x].split(":")[0].equalsIgnoreCase(protocol)) {
        String s = aps[x];
        if ((s.split(":")).length > 1) {
          s = s.split(":")[1];
        } else {
          s = "0";
        } 
        if (s.equals("0"))
          s = "32768"; 
        return Integer.parseInt(s);
      } 
    } 
    return -1;
  }
  
  public String format_message(String code_string, String the_message) {
    boolean use_old_method = false;
    if (code_string.endsWith("-")) {
      code_string = code_string.substring(0, code_string.length() - 1);
      use_old_method = true;
    } 
    the_message = the_message.trim();
    String original_message = the_message;
    if (the_message.trim().lastIndexOf("\n") >= 0) {
      the_message = the_message.substring(0, the_message.trim().lastIndexOf("\n"));
    } else {
      return String.valueOf(code_string) + " " + the_message;
    } 
    String return_string = "";
    if (the_message.length() > 0) {
      int temp_loc = 0;
      int temp_loc2 = 0;
      int skip_len = 1;
      while (temp_loc >= 0) {
        temp_loc = the_message.indexOf("\n");
        temp_loc2 = the_message.indexOf("\\n");
        if (temp_loc2 >= 0 && (temp_loc2 < temp_loc || temp_loc < 0)) {
          temp_loc = temp_loc2;
          skip_len = 2;
        } else {
          skip_len = 1;
        } 
        if (temp_loc >= 0) {
          return_string = String.valueOf(return_string) + code_string + "-" + the_message.substring(0, temp_loc).trim() + CRLF;
          the_message = the_message.substring(temp_loc + skip_len, the_message.length()).trim();
        } 
      } 
      return_string = String.valueOf(return_string) + code_string + "-" + the_message.substring(0, the_message.length()).trim() + CRLF;
    } 
    if (use_old_method) {
      return_string = String.valueOf(return_string) + code_string + "-" + original_message.substring(original_message.trim().lastIndexOf("\n")).trim() + CRLF;
    } else {
      return_string = String.valueOf(return_string) + code_string + " " + original_message.substring(original_message.trim().lastIndexOf("\n")).trim() + CRLF;
    } 
    return return_string;
  }
  
  public static boolean filter_check(String type, String the_dir, String filters) {
    if (the_dir.endsWith("/"))
      the_dir = the_dir.substring(0, the_dir.length() - 1); 
    if (the_dir.indexOf("/") >= 0)
      the_dir = the_dir.substring(the_dir.lastIndexOf("/") + 1); 
    int parse_loc = filters.indexOf(":" + type + "C:");
    while (parse_loc >= 0) {
      String search_data = filters.substring(parse_loc + 4, filters.indexOf(";", parse_loc));
      boolean bool = (search_data.startsWith("!") && !search_data.trim().equals("!"));
      if (bool)
        search_data = search_data.substring(1); 
      if (the_dir.indexOf(search_data) >= 0 && !bool)
        return false; 
      if (bool)
        return (the_dir.indexOf(search_data) >= 0); 
      parse_loc = filters.indexOf(":" + type + "C:", parse_loc + 4);
    } 
    parse_loc = filters.indexOf(":" + type + "S:");
    Properties opposite_cache = new Properties();
    while (parse_loc >= 0) {
      String search_data = filters.substring(parse_loc + 4, filters.indexOf(";", parse_loc));
      boolean bool = (search_data.startsWith("!") && !search_data.trim().equals("!"));
      if (bool) {
        search_data = search_data.substring(1);
        if (!the_dir.startsWith(search_data) && !opposite_cache.getProperty("result", "false").equals("true")) {
          opposite_cache.put("result", "false");
        } else {
          opposite_cache.put("result", "true");
        } 
      } 
      if (the_dir.startsWith(search_data) && !bool)
        return false; 
      parse_loc = filters.indexOf(":" + type + "S:", parse_loc + 4);
    } 
    if (opposite_cache.containsKey("result") && opposite_cache.getProperty("result", "false").equals("false"))
      return false; 
    opposite_cache.clear();
    parse_loc = filters.indexOf(":" + type + "E:");
    filters = filters.toUpperCase();
    the_dir = the_dir.toUpperCase();
    while (parse_loc >= 0) {
      String search_data = filters.substring(parse_loc + 4, filters.indexOf(";", parse_loc));
      boolean bool = (search_data.startsWith("!") && !search_data.trim().equals("!"));
      if (bool) {
        search_data = search_data.substring(1);
        if (!the_dir.endsWith(search_data) && !opposite_cache.getProperty("result", "false").equals("true")) {
          opposite_cache.put("result", "false");
        } else {
          opposite_cache.put("result", "true");
        } 
      } 
      if (the_dir.endsWith(search_data) && !bool)
        return false; 
      parse_loc = filters.indexOf(":" + type + "E:", parse_loc + 4);
    } 
    if (opposite_cache.containsKey("result") && opposite_cache.getProperty("result", "false").equals("false"))
      return false; 
    parse_loc = filters.indexOf(":" + type + "R:");
    boolean opposite = false;
    while (parse_loc >= 0) {
      String search_data = filters.substring(parse_loc + 4, filters.indexOf(";", parse_loc));
      opposite = (search_data.startsWith("!") && !search_data.trim().equals("!"));
      if (opposite)
        search_data = search_data.substring(1); 
      boolean simple = search_data.startsWith("~");
      if (simple)
        search_data = search_data.substring(1); 
      Pattern pattern = null;
      try {
        pattern = com.crushftp.client.Common.getPattern(search_data, true);
      } catch (Exception e) {
        Log.log("SERVER", 1, e);
      } 
      if (simple && com.crushftp.client.Common.do_search(search_data, the_dir, false, 0))
        return opposite; 
      if (pattern != null && pattern.matcher(the_dir).matches())
        return opposite; 
      parse_loc = filters.indexOf(":" + type + "R:", parse_loc + 4);
    } 
    if (opposite)
      return false; 
    return true;
  }
  
  public String play_sound(String in_str) {
    while (in_str.indexOf("<SOUND>") >= 0) {
      String sound_file = in_str.substring(in_str.indexOf("<SOUND>") + "<SOUND>".length(), in_str.indexOf("</SOUND>"));
      (new Sounds()).loadSound(new File_S(sound_file));
      in_str = String.valueOf(in_str.substring(0, in_str.indexOf("<SOUND>"))) + in_str.substring(in_str.indexOf("</SOUND>") + "</SOUND>".length());
    } 
    return in_str;
  }
  
  public static String space_encode(String in_str) {
    while (in_str.indexOf("<SPACE>") >= 0) {
      String data = in_str.substring(in_str.indexOf("<SPACE>") + "<SPACE>".length(), in_str.indexOf("</SPACE>"));
      data = replace_str(data, " ", "%20");
      in_str = String.valueOf(in_str.substring(0, in_str.indexOf("<SPACE>"))) + data + in_str.substring(in_str.indexOf("</SPACE>") + "</SPACE>".length());
    } 
    return in_str;
  }
  
  public static String url_encoder(String in_str) {
    while (in_str.indexOf("<URL>") >= 0) {
      String data = in_str.substring(in_str.indexOf("<URL>") + "<URL>".length(), in_str.indexOf("</URL>"));
      data = url_encode(data);
      in_str = String.valueOf(in_str.substring(0, in_str.indexOf("<URL>"))) + data + in_str.substring(in_str.indexOf("</URL>") + "</URL>".length());
    } 
    return in_str;
  }
  
  public static String reverse_ip(String in_str) {
    while (in_str.indexOf("<REVERSE_IP>") >= 0) {
      String data = in_str.substring(in_str.indexOf("<REVERSE_IP>") + "<REVERSE_IP>".length(), in_str.indexOf("</REVERSE_IP>"));
      try {
        data = InetAddress.getByName(data).getHostName();
      } catch (Exception exception) {}
      in_str = String.valueOf(in_str.substring(0, in_str.indexOf("<REVERSE_IP>"))) + data + in_str.substring(in_str.indexOf("</REVERSE_IP>") + "</REVERSE_IP>".length());
    } 
    return in_str;
  }
  
  public static String cut(String item) {
    item = item.replace('\\', '/');
    item = item.substring(0, item.length() - 1);
    return item;
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
  
  public static void writeFileFromJar(String src, String dst, boolean preservePath) {
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
  
  public static void write_service_conf(String filename, int ram_megabytes) {
    try {
      RandomAccessFile wrapper = new RandomAccessFile(new File_S(filename), "rw");
      byte[] b = new byte[(int)wrapper.length()];
      wrapper.readFully(b);
      String wrapperStr = new String(b, "UTF8");
      wrapperStr = replace_str(wrapperStr, "%WORKING.DIR%", String.valueOf(replace_str((new File_S("./")).getCanonicalPath(), "\\", "\\\\")) + "\\" + "\\");
      wrapperStr = replace_str(wrapperStr, "%CRUSHFTP.VERSION%", (new StringBuffer(String.valueOf(V()))).toString());
      wrapperStr = replace_str(wrapperStr, "%MAX_MEMORY%", (new StringBuffer(String.valueOf(ram_megabytes))).toString());
      wrapperStr = replace_str(wrapperStr, "wrapper.tray=false", "wrapper.tray=false\r\nwrapper.ping.timeout=300");
      wrapperStr = replace_str(wrapperStr, "%JAVA%", "java");
      wrapperStr = replace_str(wrapperStr, "=CrushFTP.jar", "=plugins/lib/CrushFTPJarProxy.jar");
      wrapper.setLength(0L);
      wrapper.seek(0L);
      wrapper.write(wrapperStr.getBytes("UTF8"));
      wrapper.close();
    } catch (Exception e) {
      Log.log("SERVER", 0, e);
    } 
  }
  
  public static void update_service_memory(int MB) {
    if (machine_is_windows())
      try {
        String filename = "./service/wrapper.conf";
        RandomAccessFile wrapper = new RandomAccessFile(new File_S(filename), "rw");
        byte[] b = new byte[(int)wrapper.length()];
        wrapper.readFully(b);
        String wrapperStr = new String(b, "UTF8");
        wrapperStr = String.valueOf(wrapperStr.substring(0, wrapperStr.indexOf("-Xmx") + 4)) + MB + wrapperStr.substring(wrapperStr.toUpperCase().indexOf("M", wrapperStr.indexOf("-Xmx") + 4));
        wrapper.setLength(0L);
        wrapper.seek(0L);
        wrapper.write(wrapperStr.getBytes("UTF8"));
        wrapper.close();
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
      }  
  }
  
  public static boolean test_elevate() throws Exception {
    try {
      com.crushftp.client.Common.check_exec();
      Process proc = Runtime.getRuntime().exec(new String[] { (new File_S("./service/elevate.exe")).getCanonicalPath(), "-c", "-w", "net", "user" }, (String[])null, new File_S("./service/"));
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      com.crushftp.client.Common.streamCopier(null, null, proc.getInputStream(), baos, false, true, true);
      proc.destroy();
      String data = new String(baos.toByteArray());
      if (data.toUpperCase().indexOf("NOT A VALID") >= 0 || data.toUpperCase().indexOf("ACCESS DENIED") >= 0)
        return false; 
      return true;
    } catch (Throwable t) {
      Log.log("SERVER", 0, t);
      return false;
    } 
  }
  
  public boolean install_windows_service(int ram_megabytes) {
    boolean ok = true;
    if (machine_is_windows()) {
      try {
        write_wrapper_files(ram_megabytes);
        (new File_S("./service/tmp")).mkdirs();
        String bat = "setlocal\r\n";
        bat = String.valueOf(bat) + "set TEMP=" + (new File_S("./service/tmp/")).getCanonicalPath() + "\r\n";
        bat = String.valueOf(bat) + "set TMP=" + (new File_S("./service/tmp/")).getCanonicalPath() + "\r\n";
        String javaPath = String.valueOf(System.getProperty("java.home")) + "\\bin\\java";
        bat = String.valueOf(bat) + "\"" + javaPath + "\" -Xmx30m -jar wrapper.jar -i wrapper.conf\r\n";
        bat = String.valueOf(bat) + "\"" + javaPath + "\" -Xmx30m -jar wrapper.jar -i wrapper_update.conf\r\n";
        bat = String.valueOf(bat) + "net start \"CrushFTP Server\"\r\n";
        RandomAccessFile out = new RandomAccessFile(new File_S("./service/service.bat"), "rw");
        out.setLength(0L);
        out.write(bat.getBytes());
        out.close();
        if (test_elevate()) {
          com.crushftp.client.Common.check_exec();
          Process proc = Runtime.getRuntime().exec(new String[] { (new File_S("./service/elevate.exe")).getCanonicalPath(), "-c", "-w", "service.bat" }, (String[])null, new File_S("./service/"));
          BufferedReader proc_in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
          String data = "";
          while ((data = proc_in.readLine()) != null) {
            Log.log("SERVER", 0, data);
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
          com.crushftp.client.Common.check_exec();
          Process proc = Runtime.getRuntime().exec(new String[] { "cmd", "/C", "service.bat" }, (String[])null, new File_S("./service/"));
          BufferedReader proc_in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
          String data = "";
          while ((data = proc_in.readLine()) != null) {
            Log.log("SERVER", 0, data);
            if (data.toUpperCase().indexOf("ACCESS DENIED") >= 0)
              ok = false; 
          } 
          proc_in.close();
          proc.waitFor();
        } 
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
      } 
      (new File_S("./service/elevate.exe")).delete();
      (new File_S("./service/service.bat")).delete();
    } 
    return ok;
  }
  
  public String install_windows_service_username(String domainuser, String domainpass) {
    boolean ok = false;
    String result = "";
    if (machine_is_windows()) {
      try {
        String bat = "setlocal\r\n";
        bat = String.valueOf(bat) + "sc.exe config \"CrushFTP Server\" obj= \"" + domainuser + "\" password= \"" + domainpass + "\"\r\n";
        RandomAccessFile out = new RandomAccessFile(new File_S("./service/service.bat"), "rw");
        out.setLength(0L);
        out.write(bat.getBytes());
        out.close();
        if (test_elevate()) {
          com.crushftp.client.Common.check_exec();
          Process proc = Runtime.getRuntime().exec(new String[] { (new File_S("./service/elevate.exe")).getCanonicalPath(), "-c", "-w", "service.bat" }, (String[])null, new File_S("./service/"));
          BufferedReader proc_in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
          String data = "";
          while ((data = proc_in.readLine()) != null) {
            Log.log("SERVER", 0, data);
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
          com.crushftp.client.Common.check_exec();
          Process proc = Runtime.getRuntime().exec(new String[] { "cmd", "/C", "service.bat" }, (String[])null, new File_S("./service/"));
          BufferedReader proc_in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
          String data = "";
          while ((data = proc_in.readLine()) != null) {
            Log.log("SERVER", 0, data);
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
        Log.log("SERVER", 0, e);
      } 
      (new File_S("./service/service.bat")).delete();
      (new File_S("./service/elevate.exe")).delete();
    } 
    if (ok)
      return ""; 
    return result;
  }
  
  public static void write_wrapper_files(int ram_megabytes) throws Exception {
    writeFileFromJar("service/lib/core/commons/commons-cli-2-SNAPSHOT.jar", "./", true);
    writeFileFromJar("service/lib/core/commons/commons-collections-3.2.jar", "./", true);
    writeFileFromJar("service/lib/core/commons/commons-configuration-1.8.jar", "./", true);
    writeFileFromJar("service/lib/core/commons/commons-io-1.3.1.jar", "./", true);
    writeFileFromJar("service/lib/core/commons/commons-lang-2.4.jar", "./", true);
    writeFileFromJar("service/lib/core/commons/commons-logging-1.1.jar", "./", true);
    writeFileFromJar("service/lib/core/commons/commons-vfs2-2.0.jar", "./", true);
    writeFileFromJar("service/lib/core/groovy/groovy-all-1.8.6.jar", "./", true);
    writeFileFromJar("service/lib/core/jna/jna-3.4.1.jar", "./", true);
    writeFileFromJar("service/lib/core/jna/platform-3.4.1.jar", "./", true);
    writeFileFromJar("service/lib/core/netty/netty-3.5.1.Final.jar", "./", true);
    writeFileFromJar("service/lib/core/regex/jrexx-1.1.1.jar", "./", true);
    writeFileFromJar("service/lib/core/yajsw/ahessian.jar", "./", true);
    writeFileFromJar("service/wrapper.jar", "./", true);
    writeFileFromJar("service/wrapperApp.jar", "./", true);
    writeFileFromJar("service/wrapper.conf", "./", true);
    writeFileFromJar("service/wrapper_update.conf", "./", true);
    writeFileFromJar("service/elevate.exe", "./", true);
    write_service_conf("./service/wrapper.conf", ram_megabytes);
    write_service_conf("./service/wrapper_update.conf", ram_megabytes);
  }
  
  public static boolean remove_windows_service() {
    boolean ok = true;
    if (machine_is_windows()) {
      try {
        write_wrapper_files(512);
        (new File_S("./service/tmp")).mkdirs();
        String bat = "setlocal\r\n";
        bat = String.valueOf(bat) + "set TEMP=" + (new File_S("./service/tmp/")).getCanonicalPath() + "\r\n";
        bat = String.valueOf(bat) + "set TMP=" + (new File_S("./service/tmp/")).getCanonicalPath() + "\r\n";
        String javaPath = String.valueOf(System.getProperty("java.home")) + "\\bin\\java";
        bat = String.valueOf(bat) + "\"" + javaPath + "\" -Xmx30m -jar wrapper.jar -r wrapper.conf\r\n";
        bat = String.valueOf(bat) + "\"" + javaPath + "\" -Xmx30m -jar wrapper.jar -r wrapper_update.conf\r\n";
        RandomAccessFile out = new RandomAccessFile(new File_S("./service/service.bat"), "rw");
        out.setLength(0L);
        out.write(bat.getBytes());
        out.close();
        if (test_elevate()) {
          com.crushftp.client.Common.check_exec();
          Process proc = Runtime.getRuntime().exec(new String[] { (new File_S("./service/elevate.exe")).getCanonicalPath(), "-c", "-w", "service.bat" }, (String[])null, new File_S("./service/"));
          BufferedReader proc_in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
          String data = "";
          while ((data = proc_in.readLine()) != null) {
            Log.log("SERVER", 0, data);
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
          com.crushftp.client.Common.check_exec();
          Process proc = Runtime.getRuntime().exec(new String[] { "cmd", "/C", "service.bat" }, (String[])null, new File_S("./service/"));
          BufferedReader proc_in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
          String data = "";
          while ((data = proc_in.readLine()) != null) {
            Log.log("SERVER", 0, data);
            if (data.toUpperCase().indexOf("ACCESS DENIED") >= 0)
              ok = false; 
          } 
          proc_in.close();
          proc.waitFor();
        } 
        if (ok)
          recurseDelete("./service/", false); 
      } catch (Exception e) {
        Log.log("SERVER", 1, e);
      } 
      (new File_S("./service/elevate.exe")).delete();
      (new File_S("./service/service.bat")).delete();
    } 
    return ok;
  }
  
  public static void remove_osx_service() {
    if (machine_is_x()) {
      try {
        (new File("/System/Library/StartupItems/CrushFTP/Resources/English.lproj/Localizable.strings")).delete();
      } catch (Exception exception) {}
      try {
        (new File("/System/Library/StartupItems/CrushFTP/StartupParameters.plist/")).delete();
      } catch (Exception exception) {}
      try {
        (new File("/System/Library/StartupItems/CrushFTP/CrushFTP")).delete();
      } catch (Exception exception) {}
      try {
        (new File("/System/Library/StartupItems/CrushFTP/Resources/English.lproj/")).delete();
      } catch (Exception exception) {}
      try {
        (new File("/System/Library/StartupItems/CrushFTP/Resources/")).delete();
      } catch (Exception exception) {}
      try {
        (new File("/System/Library/StartupItems/CrushFTP/")).delete();
      } catch (Exception exception) {}
      try {
        RandomAccessFile out = new RandomAccessFile(String.valueOf(System.getProperty("crushftp.prefs")) + "crushftp_exec_root.sh", "rw");
        out.setLength(0L);
        if ((new File("/Library/LaunchDaemons/com.crushftp.CrushFTP.plist")).exists()) {
          out.write("launchctl stop com.crushftp.CrushFTP\n".getBytes("UTF8"));
          out.write("launchctl remove com.crushftp.CrushFTP\n".getBytes("UTF8"));
        } 
        if ((new File("/Library/LaunchDaemons/com.crushftp.CrushFTPUpdate.plist")).exists()) {
          out.write("launchctl stop com.crushftp.CrushFTPUpdate\n".getBytes("UTF8"));
          out.write("launchctl remove com.crushftp.CrushFTPUpdate\n".getBytes("UTF8"));
        } 
        out.close();
        File_S f = new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "crushftp_exec_root.sh");
        exec(new String[] { "chmod", "+x", f.getCanonicalPath() });
        exec(new String[] { "osascript", "-e", "do shell script \"" + f.getCanonicalPath() + "\" with administrator privileges" });
        (new File("/Library/LaunchDaemons/com.crushftp.CrushFTP.plist")).delete();
        (new File("/Library/LaunchDaemons/com.crushftp.CrushFTPUpdate.plist")).delete();
        f.delete();
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
      } 
    } 
  }
  
  public String install_osx_service() {
    if (machine_is_x())
      try {
        String plist = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<!DOCTYPE plist PUBLIC \"-//Apple Computer//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\r\n<plist version=\"1.0\">\r\n\t<dict>\r\n\t\t<key>Label</key>\r\n\t\t<string>com.crushftp.CrushFTP</string>\r\n\t\t<key>ProgramArguments</key>\r\n\t\t<array>\r\n\t\t\t<string>" + (new File_S(String.valueOf(System.getProperty("crushftp.home")) + System.getProperty("crushftp.executable"))).getCanonicalPath() + "</string>\r\n\t\t\t<string>-d</string>\r\n" + "\t\t</array>\r\n" + "\t\t<key>RunAtLoad</key>\r\n" + "\t\t<true/>\r\n" + "\t</dict>\r\n" + "</plist>\r\n";
        RandomAccessFile plist_file = new RandomAccessFile(new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "com.crushftp.CrushFTP.plist"), "rw");
        plist_file.setLength(0L);
        plist_file.write(plist.getBytes("UTF8"));
        plist_file.close();
        String plist2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<!DOCTYPE plist PUBLIC \"-//Apple Computer//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\r\n<plist version=\"1.0\">\r\n\t<dict>\r\n\t\t<key>Label</key>\r\n\t\t<string>com.crushftp.CrushFTPUpdate</string>\r\n\t\t<key>ProgramArguments</key>\r\n\t\t<array>\r\n\t\t\t<string>" + (new File_S(System.getProperty("crushftp.home"))).getCanonicalPath() + "/daemonUpdate.sh</string>\r\n" + "\t\t</array>\r\n" + "\t\t<key>RunAtLoad</key>\r\n" + "\t\t<false/>\r\n" + "\t</dict>\r\n" + "</plist>\r\n";
        plist_file = new RandomAccessFile(new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "com.crushftp.CrushFTPUpdate.plist"), "rw");
        plist_file.setLength(0L);
        plist_file.write(plist2.getBytes("UTF8"));
        plist_file.close();
        String path_to_crush = String.valueOf((new File_S(System.getProperty("crushftp.home"))).getCanonicalPath()) + "/";
        String daemon = "#!/bin/sh\n";
        daemon = String.valueOf(daemon) + "echo CrushFTPUpdate starting...\n";
        daemon = String.valueOf(daemon) + "cd \"" + path_to_crush + "\"\n";
        daemon = String.valueOf(daemon) + "\"" + System.getProperty("java.home") + "/bin/java\" -cp plugins/lib/CrushFTPRestart.jar CrushFTPRestart\n";
        daemon = String.valueOf(daemon) + "echo CrushFTPUpdate stopped.\n";
        RandomAccessFile daemon_file = new RandomAccessFile(new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "daemonUpdate.sh"), "rw");
        daemon_file.setLength(0L);
        daemon_file.write(daemon.getBytes("UTF8"));
        daemon_file.close();
        RandomAccessFile out = new RandomAccessFile(String.valueOf(System.getProperty("crushftp.prefs")) + "crushftp_exec_root.sh", "rw");
        out.setLength(0L);
        out.write(("mv \"" + (new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "daemonUpdate.sh")).getCanonicalPath() + "\" \"" + path_to_crush + "daemonUpdate.sh\"\n").getBytes("UTF8"));
        out.write(("mv \"" + (new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "com.crushftp.CrushFTP.plist")).getCanonicalPath() + "\" /Library/LaunchDaemons/com.crushftp.CrushFTP.plist\n").getBytes("UTF8"));
        out.write(("mv \"" + (new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "com.crushftp.CrushFTPUpdate.plist")).getCanonicalPath() + "\" /Library/LaunchDaemons/com.crushftp.CrushFTPUpdate.plist\n").getBytes("UTF8"));
        out.write(("chmod 755 \"" + (new File_S(String.valueOf(path_to_crush) + "daemonUpdate.sh")).getCanonicalPath() + "\"\n").getBytes("UTF8"));
        out.write("chmod 700 /Library/LaunchDaemons/com.crushftp.CrushFTP.plist\n".getBytes("UTF8"));
        out.write("chown root /Library/LaunchDaemons/com.crushftp.CrushFTP.plist\n".getBytes("UTF8"));
        out.write("chgrp wheel /Library/LaunchDaemons/com.crushftp.CrushFTP.plist\n".getBytes("UTF8"));
        out.write("chmod 700 /Library/LaunchDaemons/com.crushftp.CrushFTPUpdate.plist\n".getBytes("UTF8"));
        out.write("chown root /Library/LaunchDaemons/com.crushftp.CrushFTPUpdate.plist\n".getBytes("UTF8"));
        out.write("chgrp wheel /Library/LaunchDaemons/com.crushftp.CrushFTPUpdate.plist\n".getBytes("UTF8"));
        out.write("launchctl load -F -w /Library/LaunchDaemons/com.crushftp.CrushFTP.plist\n".getBytes("UTF8"));
        out.write("launchctl load -F -w /Library/LaunchDaemons/com.crushftp.CrushFTPUpdate.plist\n".getBytes("UTF8"));
        out.write("launchctl start com.crushftp.CrushFTP\n".getBytes("UTF8"));
        out.close();
        File_S f = new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "crushftp_exec_root.sh");
        exec(new String[] { "chmod", "+x", (new File_S(String.valueOf(System.getProperty("crushftp.home")) + System.getProperty("crushftp.executable"))).getCanonicalPath() });
        exec(new String[] { "chmod", "+x", f.getCanonicalPath() });
        exec(new String[] { "osascript", "-e", "do shell script \"" + f.getCanonicalPath() + "\" with administrator privileges" });
        f.delete();
        return "";
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
        return e.toString();
      }  
    return "error: not OS X";
  }
  
  public static void checkForUpdate(Properties info) {
    try {
      String url = "https://www.crushftp.com/version" + V() + (info.getProperty("check_build", "false").equals("true") ? "_build" : "") + ".html";
      URLConnection urlc = (new URL(url)).openConnection();
      InputStream in = (InputStream)urlc.getContent();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      com.crushftp.client.Common.streamCopier(in, baos, false, true, true);
      String html = new String(baos.toByteArray(), "UTF8");
      String serverVersion = html.substring(0, (html.indexOf("\r") > 0) ? html.indexOf("\r") : html.indexOf("\n")).trim();
      html = html.substring(html.indexOf("<")).trim();
      info.put("version", serverVersion);
      info.put("html", html);
    } catch (Exception e) {
      Log.log("SERVER", 1, e);
      Log.log("SERVER", 0, "Unable to check for update. " + e.toString());
    } 
  }
  
  public static String decode64(String data) {
    byte[] stuff = (byte[])null;
    try {
      stuff = Base64.decode(data);
    } catch (Exception exception) {}
    try {
      data = new String(stuff, "UTF8");
    } catch (UnsupportedEncodingException unsupportedEncodingException) {}
    return data;
  }
  
  public static String encode64(String data) {
    try {
      data = Base64.encodeBytes(data.getBytes("UTF8"));
    } catch (Exception exception) {}
    return data;
  }
  
  public ServerSocket getServerSocket(int serverPort, String listen_ip, String KEYSTORE, String keystorepass, String keypass, String disabled_ciphers, boolean needClientAuth, int backlog) throws Exception {
    return getServerSocket(serverPort, listen_ip, KEYSTORE, keystorepass, keypass, disabled_ciphers, needClientAuth, backlog, true, true, null);
  }
  
  public ServerSocket getServerSocket(int serverPort, String listen_ip, String KEYSTORE, String keystorepass, String keypass, String disabled_ciphers, boolean needClientAuth, int backlog, boolean allowBuiltIn, boolean bind_all, SSLContext ssl_context) throws Exception {
    ServerSocketFactory ssf = null;
    if (ssl_context != null) {
      ssf = ssl_context.getServerSocketFactory();
    } else {
      ssf = getSSLContext(KEYSTORE, String.valueOf(KEYSTORE) + "_trust", keystorepass, keypass, "TLS", needClientAuth, true, allowBuiltIn).getServerSocketFactory();
    } 
    SSLServerSocket serverSocket = null;
    try {
      if (listen_ip != null) {
        if (listen_ip.equalsIgnoreCase("lookup"))
          listen_ip = "0.0.0.0"; 
        boolean all_numbers = true;
        for (int x = 0; x < listen_ip.length(); x++) {
          if (listen_ip.charAt(x) >= ':')
            all_numbers = false; 
        } 
        if (!all_numbers || !bind_all)
          serverSocket = (SSLServerSocket)ssf.createServerSocket(serverPort, backlog, InetAddress.getByName(listen_ip)); 
      } 
    } catch (SocketException e) {
      Log.log("SERVER", 2, e);
    } 
    if (serverSocket == null)
      serverSocket = (SSLServerSocket)ssf.createServerSocket(serverPort, backlog); 
    setEnabledCiphers(disabled_ciphers, null, serverSocket);
    serverSocket.setNeedClientAuth(needClientAuth);
    configureSSLTLSSocket(serverSocket);
    return serverSocket;
  }
  
  public static void configureSSLTLSSocket(Object sock) {
    com.crushftp.client.Common.configureSSLTLSSocket(sock, ServerStatus.SG("tls_version"));
  }
  
  public static void setEnabledCiphers(String disabled_ciphers, SSLSocket sock, SSLServerSocket serverSock) {
    setEnabledCiphers(disabled_ciphers, sock, serverSock, null);
  }
  
  public static String[] setEnabledCiphers(String disabled_ciphers, SSLSocket sock, SSLServerSocket serverSock, SSLSocketFactory factory) {
    String[] ciphers = (String[])null;
    if (disabled_ciphers.equals("")) {
      if (sock != null)
        ciphers = sock.getSupportedCipherSuites(); 
      if (serverSock != null)
        ciphers = serverSock.getSupportedCipherSuites(); 
      if (factory != null)
        ciphers = factory.getSupportedCipherSuites(); 
    } else {
      disabled_ciphers = disabled_ciphers.toUpperCase();
      Vector enabled_ciphers = new Vector();
      if (sock != null)
        ciphers = sock.getSupportedCipherSuites(); 
      if (serverSock != null)
        ciphers = serverSock.getSupportedCipherSuites(); 
      if (factory != null)
        ciphers = factory.getSupportedCipherSuites(); 
      int x;
      for (x = 0; x < ciphers.length; x++) {
        if (disabled_ciphers.indexOf("(" + ciphers[x].toUpperCase() + ")") < 0 && ciphers[x].toUpperCase().indexOf("EXPORT") < 0)
          enabled_ciphers.addElement(ciphers[x]); 
      } 
      try {
        SSLParameters sslp = null;
        if (sock != null)
          sslp = sock.getSSLParameters(); 
        if (serverSock != null) {
          if (class$0 == null)
            try {
            
            } catch (ClassNotFoundException classNotFoundException) {
              throw new NoClassDefFoundError(null.getMessage());
            }  
          Method getSSLParameters = (class$0 = Class.forName("javax.net.ssl.SSLServerSocket")).getDeclaredMethod("getSSLParameters", null);
          sslp = (SSLParameters)getSSLParameters.invoke(serverSock, null);
        } 
        if (class$1 == null)
          try {
          
          } catch (ClassNotFoundException classNotFoundException) {
            throw new NoClassDefFoundError(null.getMessage());
          }  
        Method setUseCipherSuitesOrder = (class$1 = Class.forName("javax.net.ssl.SSLParameters")).getDeclaredMethod("setUseCipherSuitesOrder", new Class[] { boolean.class });
        setUseCipherSuitesOrder.invoke(sslp, new Object[] { new Boolean(true) });
        Vector enabled_ciphers2 = new Vector();
        int i;
        for (i = 1; i < 100; i++) {
          int pos = disabled_ciphers.indexOf(String.valueOf(i) + ";");
          if (pos >= 0) {
            String cipher = disabled_ciphers.substring(pos, disabled_ciphers.indexOf(")", pos));
            cipher = cipher.substring(cipher.indexOf(";") + 1).trim();
            if (enabled_ciphers.indexOf(cipher) >= 0)
              enabled_ciphers2.addElement(cipher); 
          } 
        } 
        for (i = 0; i < enabled_ciphers.size(); i++) {
          if (enabled_ciphers2.indexOf(enabled_ciphers.elementAt(i).toString()) < 0)
            enabled_ciphers2.addElement(enabled_ciphers.elementAt(i).toString()); 
        } 
        enabled_ciphers = enabled_ciphers2;
      } catch (Exception e) {
        Log.log("SERVER", 2, e);
      } 
      ciphers = new String[enabled_ciphers.size()];
      for (x = 0; x < enabled_ciphers.size(); x++)
        ciphers[x] = enabled_ciphers.elementAt(x).toString(); 
      if (sock != null)
        sock.setEnabledCipherSuites(ciphers); 
      if (serverSock != null)
        serverSock.setEnabledCipherSuites(ciphers); 
    } 
    return ciphers;
  }
  
  public static boolean providerAdded = false;
  
  public static boolean fips140 = false;
  
  static final String XML_master = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><userfile type=\"properties\">\t<password>Acf9C+U0B0UvQiwbMd9Km+uEHaQO/nLf</password>\t<root_dir>/</root_dir>\t<version>1.0</version>\t<max_logins>0</max_logins></userfile>";
  
  static final String XML_VFS = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"vector\">\t<VFS_subitem type=\"properties\">\t\t<url>FILE://</url>\t\t<type>dir</type>\t</VFS_subitem></VFS>";
  
  static final String XML_VFS_ITEM = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"properties\">\t<item name=\"/\">(read)(view)(resume)</item></VFS>";
  
  public SSLContext getSSLContext(String KEYSTORE, String TRUSTSTORE, String keystorepass, String keypass, String secureType, boolean needClientAuth, boolean acceptAnyCert) throws Exception {
    return getSSLContext(KEYSTORE, TRUSTSTORE, keystorepass, keypass, keystorepass, keypass, secureType, needClientAuth, acceptAnyCert, true);
  }
  
  public SSLContext getSSLContext(String KEYSTORE, String TRUSTSTORE, String keystorepass, String keypass, String secureType, boolean needClientAuth, boolean acceptAnyCert, boolean allowBuiltIn) throws Exception {
    return getSSLContext(KEYSTORE, TRUSTSTORE, keystorepass, keypass, keystorepass, keypass, secureType, needClientAuth, acceptAnyCert, allowBuiltIn);
  }
  
  public SSLContext getSSLContext(String KEYSTORE, String TRUSTSTORE, String keystorepass, String keypass, String truststorepass, String trustpass, String secureType, boolean needClientAuth, boolean acceptAnyCert, boolean allowBuiltIn) throws Exception {
    if (TRUSTSTORE == null)
      TRUSTSTORE = KEYSTORE; 
    String className = System.getProperty("crushftp.sslprovider", "");
    try {
      if (!providerAdded && !className.equals("")) {
        Log.log("SERVER", 0, "Adding SSL provider:" + className);
        Provider provider = (Provider)ServerStatus.clasLoader.loadClass(className).newInstance();
        Security.addProvider(provider);
        providerAdded = true;
      } 
    } catch (Exception e) {
      throw new Exception("Failed loading security provider " + className, e);
    } 
    if (ServerStatus.BG("fips140") && !fips140) {
      Class c = ServerStatus.clasLoader.loadClass("com.sun.net.ssl.internal.ssl.Provider");
      if (class$2 == null)
        try {
        
        } catch (ClassNotFoundException classNotFoundException) {
          throw new NoClassDefFoundError(null.getMessage());
        }  
      false[class$2] = class$2 = Class.forName("java.lang.String");
      Constructor cons = (new Class[1]).getConstructor(new Class[1]);
      cons.newInstance(new Object[] { "SunPKCS11-NSS" });
      fips140 = true;
    } 
    KeyStore keystore = null;
    KeyStore truststore = null;
    String keystoreFormat = "JKS";
    if (KEYSTORE.toUpperCase().indexOf("PKCS12") >= 0 || KEYSTORE.toUpperCase().indexOf("PFX") >= 0 || KEYSTORE.toUpperCase().indexOf("P12") >= 0)
      keystoreFormat = "pkcs12"; 
    if (KEYSTORE.equalsIgnoreCase("PKCS11") && fips140) {
      keystoreFormat = "PKCS11";
      keystore = KeyStore.getInstance(keystoreFormat);
      keystore.load(null, decode_pass(keystorepass).toCharArray());
      truststore = KeyStore.getInstance(keystoreFormat);
      acceptAnyCert = false;
    } 
    if (keystore == null) {
      keystore = KeyStore.getInstance(keystoreFormat);
      if (KEYSTORE.equals("builtin")) {
        keystore.load(getClass().getResource("/assets/builtin").openStream(), "crushftp".toCharArray());
      } else if (com.crushftp.client.Common.System2.containsKey("crushftp.keystores." + KEYSTORE.toUpperCase().replace('\\', '/'))) {
        Properties p = (Properties)com.crushftp.client.Common.System2.get("crushftp.keystores." + KEYSTORE.toUpperCase().replace('\\', '/'));
        keystore.load(new ByteArrayInputStream((byte[])p.get("bytes")), decode_pass(keystorepass).toCharArray());
      } else {
        InputStream in = null;
        try {
          in = new FileInputStream(new File_S(KEYSTORE));
          keystore.load(in, decode_pass(keystorepass).toCharArray());
        } catch (Exception e) {
          if (!allowBuiltIn)
            throw e; 
          Log.log("SERVER", 0, "Couldn't load keystore " + KEYSTORE + ", ignoring it.");
          Log.log("SERVER", 0, e);
          keystore.load(getClass().getResource("/assets/builtin").openStream(), "crushftp".toCharArray());
          KEYSTORE = "builtin";
        } finally {
          if (in != null)
            in.close(); 
        } 
      } 
      if (truststore == null)
        truststore = KeyStore.getInstance(keystoreFormat); 
      if (KEYSTORE.equals("builtin")) {
        truststore.load(getClass().getResource("/assets/builtin").openStream(), "crushftp".toCharArray());
      } else if (needClientAuth) {
        if (com.crushftp.client.Common.System2.containsKey("crushftp.keystores." + TRUSTSTORE.toUpperCase().replace('\\', '/'))) {
          Properties p = (Properties)com.crushftp.client.Common.System2.get("crushftp.keystores." + TRUSTSTORE.toUpperCase().replace('\\', '/'));
          truststore.load(new ByteArrayInputStream((byte[])p.get("bytes")), decode_pass(keystorepass).toCharArray());
        } else {
          InputStream in = null;
          try {
            if (!(new File_S(TRUSTSTORE)).exists()) {
              Log.log("SERVER", 0, "Couldn't find truststore " + TRUSTSTORE + ", ignoring it.");
              truststore.load(getClass().getResource("/assets/builtin").openStream(), "crushftp".toCharArray());
              TRUSTSTORE = "builtin";
            } else {
              in = new FileInputStream(new File_S(TRUSTSTORE));
              truststore.load(in, decode_pass(truststorepass).toCharArray());
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
        kmf.init(keystore, decode_pass(keypass).toCharArray());
      } catch (Exception e) {
        kmf.init(keystore, decode_pass(keystorepass).toCharArray());
      } 
    } 
    TrustManager[] trustAllCerts = { new TrustManagerCustom(null, true, true) };
    SSLContext sslc = null;
    if (secureType.trim().equalsIgnoreCase("TLS")) {
      sslc = SSLContext.getInstance("TLS");
    } else {
      sslc = SSLContext.getInstance(secureType);
    } 
    if (needClientAuth) {
      TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmf.init(truststore);
      if (ServerStatus.BG("trust_expired_client_cert")) {
        sslc.init(kmf.getKeyManagers(), new TrustManager[] { new TrustManagerCustom((X509TrustManager)tmf.getTrustManagers()[0], false, true) }new SecureRandom());
      } else {
        sslc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
      } 
    } else if (acceptAnyCert) {
      sslc.init(kmf.getKeyManagers(), trustAllCerts, new SecureRandom());
    } else {
      TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmf.init(keystore);
      if (fips140) {
        sslc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), SecureRandom.getInstance("PKCS11"));
      } else {
        sslc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
      } 
    } 
    return sslc;
  }
  
  public KeyPair getPrivateKey(KeyStore keystore, String alias, char[] password) {
    try {
      Key key = keystore.getKey(alias, password);
      if (key instanceof PrivateKey) {
        Certificate cert = keystore.getCertificate(alias);
        PublicKey publicKey = cert.getPublicKey();
        return new KeyPair(publicKey, (PrivateKey)key);
      } 
    } catch (Exception e) {
      Log.log("SERVER", 1, e);
    } 
    return null;
  }
  
  public static String makeBoundary() {
    return makeBoundary(11);
  }
  
  public static String makeBoundary(int len) {
    String chars = "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    String rand = "";
    for (int i = 0; i < len; i++)
      rand = String.valueOf(rand) + chars.charAt(rn.nextInt(chars.length())); 
    return rand;
  }
  
  public void writeAdminUser(String curUser, String password, String serverGroup, boolean localhostOnly) {
    try {
      Properties user_prop = (Properties)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><userfile type=\"properties\">\t<password>Acf9C+U0B0UvQiwbMd9Km+uEHaQO/nLf</password>\t<root_dir>/</root_dir>\t<version>1.0</version>\t<max_logins>0</max_logins></userfile>".getBytes("UTF8"))).getRootElement());
      user_prop.put("site", "(CONNECT)(WEB_ADMIN)");
      user_prop.put("ignore_max_logins", "true");
      user_prop.put("max_logins_ip", "8");
      user_prop.put("max_idle_time", "0");
      user_prop.put("password", encode_pass(password, "DES", ""));
      if (localhostOnly) {
        Vector ips = (Vector)user_prop.get("ip_restrictions");
        ips.removeAllElements();
        Properties p = new Properties();
        p.put("start_ip", "127.0.0.1");
        p.put("stop_ip", "127.0.0.1");
        p.put("type", "A");
        ips.addElement(p);
      } 
      UserTools.writeUser(serverGroup, curUser, user_prop);
    } catch (Exception e) {
      Log.log("SERVER", 0, e);
      e.printStackTrace();
    } 
  }
  
  public void writeNewUser(String curUser, String curPassword, String root_dir, String permissions, String templateUser, String notes, String email, String listen_ip_port) {
    String pathOut = String.valueOf(System.getProperties().getProperty("crushftp.users")) + listen_ip_port + "/";
    try {
      Properties user_prop = (Properties)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><userfile type=\"properties\">\t<password>Acf9C+U0B0UvQiwbMd9Km+uEHaQO/nLf</password>\t<root_dir>/</root_dir>\t<version>1.0</version>\t<max_logins>0</max_logins></userfile>".getBytes("UTF8"))).getRootElement());
      Vector user_vfs = (Vector)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"vector\">\t<VFS_subitem type=\"properties\">\t\t<url>FILE://</url>\t\t<type>dir</type>\t</VFS_subitem></VFS>".getBytes("UTF8"))).getRootElement());
      Properties user_vfs_item = (Properties)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"properties\">\t<item name=\"/\">(read)(view)(resume)</item></VFS>".getBytes("UTF8"))).getRootElement());
      if (!templateUser.trim().equals(""))
        try {
          System.out.println("Loading template user " + templateUser + ".");
          Properties p = UserTools.ut.getUser(listen_ip_port, templateUser, false);
          if (p != null) {
            Enumeration keys = p.keys();
            while (keys.hasMoreElements()) {
              String key = keys.nextElement().toString();
              try {
                user_prop.put(key, p.get(key));
              } catch (Exception exception) {}
            } 
          } else {
            System.out.println("Template user " + templateUser + " not found.");
          } 
        } catch (Exception e) {
          e.printStackTrace();
        }  
      (new File_S(String.valueOf(pathOut) + curUser)).mkdirs();
      (new File_S(String.valueOf(pathOut) + curUser + "/VFS/")).mkdirs();
      String url = (new File_S(root_dir)).toURI().toURL().toExternalForm();
      if (!url.endsWith("/"))
        url = String.valueOf(url) + "/"; 
      ((Properties)user_vfs.elementAt(0)).put("url", url);
      user_prop.put("password", encode_pass(curPassword, "DES", ""));
      if (notes != null)
        user_prop.put("notes", notes); 
      if (email != null)
        user_prop.put("email", email); 
      user_vfs_item.put("/" + (new File_S(root_dir)).getName().toUpperCase() + "/", permissions);
      (new File_S(String.valueOf(pathOut) + curUser)).mkdirs();
      updateOSXInfo(String.valueOf(pathOut) + curUser);
      (new File_S(String.valueOf(pathOut) + curUser + "/VFS/")).mkdirs();
      updateOSXInfo(String.valueOf(pathOut) + curUser + "/VFS/");
      writeXMLObject(String.valueOf(pathOut) + curUser + "/user.XML", user_prop, "userfile");
      writeXMLObject(String.valueOf(pathOut) + curUser + "/VFS/" + (new File_S(root_dir)).getName(), user_vfs, "VFS");
      writeXMLObject(String.valueOf(pathOut) + curUser + "/VFS.XML", user_vfs_item, "VFS");
    } catch (Exception e) {
      e.printStackTrace();
    } 
  }
  
  public void ConvertOSXUsers(String user_path) throws Exception {
    ConvertFolderUsers("/Users/", user_path);
  }
  
  public void ConvertFolderUsers(String dir, String user_path) throws Exception {
    if (!dir.endsWith("/"))
      dir = String.valueOf(dir) + "/"; 
    String pathOut = String.valueOf(System.getProperties().getProperty("crushftp.users")) + user_path;
    String[] dirList = (new File_U(dir)).list();
    for (int x = 0; x < dirList.length; x++) {
      String curUser = dirList[x];
      Properties user_prop = (Properties)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><userfile type=\"properties\">\t<password>Acf9C+U0B0UvQiwbMd9Km+uEHaQO/nLf</password>\t<root_dir>/</root_dir>\t<version>1.0</version>\t<max_logins>0</max_logins></userfile>".getBytes("UTF8"))).getRootElement());
      Vector user_vfs = (Vector)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"vector\">\t<VFS_subitem type=\"properties\">\t\t<url>FILE://</url>\t\t<type>dir</type>\t</VFS_subitem></VFS>".getBytes("UTF8"))).getRootElement());
      Properties user_vfs_item = (Properties)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"properties\">\t<item name=\"/\">(read)(view)(resume)</item></VFS>".getBytes("UTF8"))).getRootElement());
      if (!curUser.equals("Shared") && (new File_U(String.valueOf(dir) + curUser)).isDirectory()) {
        ((Properties)user_vfs.elementAt(0)).put("url", (new File_U(String.valueOf(dir) + curUser)).toURI().toURL().toExternalForm());
        user_vfs_item.put("/" + curUser + "/", "(read)(write)(view)(delete)(resume)(rename)(makedir)(deletedir)");
        (new File_S(String.valueOf(pathOut) + curUser)).mkdirs();
        updateOSXInfo(String.valueOf(pathOut) + curUser);
        (new File_S(String.valueOf(pathOut) + curUser + "/VFS/")).mkdirs();
        updateOSXInfo(String.valueOf(pathOut) + curUser + "/VFS/");
        writeXMLObject(String.valueOf(pathOut) + curUser + "/user.XML", user_prop, "userfile");
        writeXMLObject(String.valueOf(pathOut) + curUser + "/VFS/" + curUser, user_vfs, "VFS");
        writeXMLObject(String.valueOf(pathOut) + curUser + "/VFS.XML", user_vfs_item, "VFS");
      } 
    } 
  }
  
  public void ConvertCrushFTP3Users(String dir, String user_path, String encryption_mode, String parentUser) throws Exception {
    if (!dir.endsWith("/"))
      dir = String.valueOf(dir) + "/"; 
    String pathOut = String.valueOf(System.getProperties().getProperty("crushftp.users")) + user_path;
    String[] dirList = (new File_S(dir)).list();
    for (int x = 0; x < dirList.length; x++) {
      String item = dirList[x];
      String parentTemp = (new File_S(String.valueOf(dir) + item)).getParentFile().getName();
      if (parentTemp.toUpperCase().startsWith("USERS_"))
        parentTemp = ""; 
      if ((new File_S(String.valueOf(dir) + item)).isDirectory())
        ConvertCrushFTP3Users(String.valueOf(dir) + item, user_path, encryption_mode, parentTemp); 
      if (item.toUpperCase().equals("0.XML")) {
        String curUser = (new File_S(dir)).getName();
        String user_str = "";
        RandomAccessFile in = new RandomAccessFile(new File_S(String.valueOf(dir) + item), "r");
        byte[] b = new byte[(int)in.length()];
        in.readFully(b);
        in.close();
        user_str = new String(b, "UTF8");
        user_str = replace_str(user_str, "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\r\n<userfile>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><userfile type=\"properties\">");
        user_str = replace_str(user_str, "<userfile_setting>", "");
        user_str = replace_str(user_str, "</userfile_setting>", "");
        user_str = replace_str(user_str, "\t\t<ip_restrictions>\r\n\t\t</ip_restrictions>\r\n", "");
        user_str = replace_str(user_str, "VERSION_S:1", "(SITE_VERSION)");
        user_str = replace_str(user_str, "USERS_S:1", "(SITE_USERS)");
        user_str = replace_str(user_str, "KICK_S:1", "(SITE_KICK)");
        user_str = replace_str(user_str, "KICKBAN_S:1", "(SITE_KICKBAN)");
        user_str = replace_str(user_str, "PASS_S:1", "(SITE_PASS)");
        user_str = replace_str(user_str, "ZIP_S:1", "(SITE_ZIP)");
        user_str = replace_str(user_str, "HIDE_S:1", "");
        user_str = replace_str(user_str, "QUIT_S:1", "(SITE_QUIT)");
        user_str = replace_str(user_str, "IRC_OPIRC_SEARCHIRC_INVITE", "");
        user_str = replace_str(user_str, "SEARCH", "");
        user_str = replace_str(user_str, "*connect*", "(CONNECT)(WEB_ADMIN)");
        user_str = replace_str(user_str, "*user_activity*", "(USER_ACTIVITY)");
        user_str = replace_str(user_str, "*kick_user*", "(KICK_USER)(PASSIVE_KICK_USER)");
        user_str = replace_str(user_str, "*passive_kick_user*", "");
        user_str = replace_str(user_str, "*ban_user*", "(BAN_USER)");
        user_str = replace_str(user_str, "*add_log*", "(ADD_LOG)");
        user_str = replace_str(user_str, "*server_settings*", "(SERVER_SETTINGS)(SERVER_SETTINGS_WRITE)");
        user_str = replace_str(user_str, "*get_user*", "(GET_USER_LIST_AND_INHERITANCE)(GET_USER)");
        user_str = replace_str(user_str, "*get_user_list_and_inheritance*", "");
        user_str = replace_str(user_str, "*write_user*", "(WRITE_USER)(WRITE_INHERITANCE)(GET_LOCAL_LISTING)(GET_REAL_LOCAL_LISTING)");
        user_str = replace_str(user_str, "*delete_user_parts*", "(DELETE_USER)");
        user_str = replace_str(user_str, "*change_name*", "(CHANGE_NAME)");
        user_str = replace_str(user_str, "*modify_mirrors*", "");
        user_str = replace_str(user_str, "*get_reports*", "(GET_REPORTS)");
        user_str = replace_str(user_str, "*pause_user*", "(PAUSE_USER)");
        user_str = replace_str(user_str, "*start_server*", "(START_SERVER)(STOP_SERVER)(STOP_SERVER_KICK)");
        user_str = replace_str(user_str, "*stop_server*", "");
        user_str = replace_str(user_str, "*stop_server_kick*", "");
        user_str = replace_str(user_str, "*user_admin_connect*", "(USER_ADMIN_CONNECT)");
        user_str = replace_str(user_str, "<dirs>", "<dirs_subitem type=\"properties\">");
        user_str = replace_str(user_str, "</dirs>", "</dirs_subitem>");
        user_str = replace_str(user_str, "<more_items></more_items>", "");
        user_str = replace_str(user_str, "<email_event>", "<events_subitem type=\"properties\">");
        user_str = replace_str(user_str, "</email_event>", "</events_subitem>");
        try {
          user_str = String.valueOf(user_str.substring(0, user_str.indexOf("<events_subitem type=\"properties\">"))) + "<events type=\"vector\">" + user_str.substring(user_str.indexOf("<events_subitem type=\"properties\">"));
          user_str = String.valueOf(user_str.substring(0, user_str.lastIndexOf("</events_subitem>") + "</events_subitem>".length())) + "</events>" + user_str.substring(user_str.lastIndexOf("</events_subitem>") + "</events_subitem>".length());
        } catch (Exception exception) {}
        user_str = replace_str(user_str, "<ip_restrictions>", "<ip_restrictions_subitem type=\"properties\">");
        user_str = replace_str(user_str, "</ip_restrictions>", "</ip_restrictions_subitem>");
        try {
          user_str = String.valueOf(user_str.substring(0, user_str.indexOf("<ip_restrictions_subitem type=\"properties\">"))) + "<ip_restrictions type=\"vector\">" + user_str.substring(user_str.indexOf("<ip_restrictions_subitem type=\"properties\">"));
          user_str = String.valueOf(user_str.substring(0, user_str.lastIndexOf("</ip_restrictions_subitem>") + "</ip_restrictions_subitem>".length())) + "</ip_restrictions>" + user_str.substring(user_str.lastIndexOf("</ip_restrictions_subitem>") + "</ip_restrictions_subitem>".length());
        } catch (Exception exception) {}
        try {
          user_str = String.valueOf(user_str.substring(0, user_str.indexOf("<dirs_subitem type=\"properties\">"))) + "<dirs type=\"vector\">" + user_str.substring(user_str.indexOf("<dirs_subitem type=\"properties\">"));
          user_str = String.valueOf(user_str.substring(0, user_str.lastIndexOf("</dirs_subitem>") + "</dirs_subitem>".length())) + "</dirs>" + user_str.substring(user_str.lastIndexOf("</dirs_subitem>") + "</dirs_subitem>".length());
          while (user_str.indexOf("<more_items>") >= 0)
            user_str = String.valueOf(user_str.substring(0, user_str.indexOf("<more_items>"))) + user_str.substring(user_str.indexOf("</more_items>") + "</more_items>".length()); 
        } catch (Exception exception) {}
        String password = user_str.substring(user_str.indexOf("<password>") + "<password>".length(), user_str.indexOf("</password>"));
        user_str = String.valueOf(user_str.substring(0, user_str.indexOf("<password>"))) + user_str.substring(user_str.indexOf("</password>") + "</password>".length());
        user_str = replace_str(user_str, "&", "%26");
        user_str = replace_str(user_str, "%20", " ");
        user_str = replace_str(user_str, "%%", "%");
        user_str = replace_str(user_str, "%3C", "&lt;");
        user_str = replace_str(user_str, "%3E", "&gt;");
        user_str = replace_str(user_str, "%2F", "/");
        Properties user_prop = (Properties)getElements(getSaxBuilder().build(new ByteArrayInputStream(user_str.getBytes("UTF8"))).getRootElement());
        Properties user_vfs_item = (Properties)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"properties\">\t<item name=\"/\">(read)(view)(resume)</item></VFS>".getBytes("UTF8"))).getRootElement());
        user_prop.put("password", encode_pass(decode_pass3(password), encryption_mode, ""));
        user_prop.put("root_dir", "/");
        try {
          if (user_prop.get("events") == null)
            user_prop.put("events", new Vector()); 
          Vector events = (Vector)user_prop.get("events");
          for (int i = 0; i < events.size(); i++) {
            Properties p = events.elementAt(i);
            String s = p.getProperty("email_command_data", "");
            p.remove("email_command_data");
            if (s.equalsIgnoreCase("QUIT"))
              p.put("event_user_action_list", "(disconnect)"); 
            if (s.equalsIgnoreCase("STOR"))
              p.put("event_user_action_list", "(upload)"); 
            if (s.equalsIgnoreCase("RETR"))
              p.put("event_user_action_list", "(download)"); 
            if (s.equalsIgnoreCase("PASS"))
              p.put("event_user_action_list", "(connect)"); 
            p.put("smtp_user", p.getProperty("user_name", ""));
            p.put("smtp_pass", p.getProperty("user_pass", ""));
            p.remove("user_name");
            p.remove("user_pass");
            p.put("body", "<LINE>" + p.getProperty("body", "") + "</LINE>");
            p.put("event_dir_data", p.getProperty("email_dir_data", ""));
            p.remove("email_dir_data");
            p.put("event_action_list", "(send_email)");
            p.put("event_if_list", "");
            p.put("event_always_cb", "true");
            p.put("event_after_list", "");
            p.put("event_after_cb", "false");
            p.put("event_now_cb", "true");
            p.put("event_if_cb", "false");
            p.put("event_plugin_list", "");
          } 
        } catch (Exception e) {
          Log.log("SERVER", 1, e);
        } 
        Enumeration keys = user_prop.keys();
        while (keys.hasMoreElements()) {
          String key = keys.nextElement().toString();
          Object val = user_prop.get(key);
          if (val instanceof String)
            if (!parentUser.equals("") && val.toString().startsWith("i*")) {
              user_prop.put(key, String.valueOf(val.toString().substring(2)) + "@" + parentUser);
            } else if (val.toString().startsWith("i*")) {
              user_prop.put(key, val.toString().substring(2));
            }  
          if (key.equals("inherit_email_event") && val.toString().equals("1") && !parentUser.equals("")) {
            user_prop.put("inherit_events", "@" + parentUser);
            continue;
          } 
          if (key.equals("inherit_ip_restrictions") && val.toString().equals("1") && !parentUser.equals("")) {
            user_prop.put("inherit_ip_restrictions", "@" + parentUser);
            continue;
          } 
          if (key.equals("inherit_dirs") && val.toString().equals("1") && !parentUser.equals(""))
            user_prop.put("root_dir", String.valueOf(user_prop.getProperty("root_dir")) + "@" + parentUser); 
        } 
        String userSubdir = "";
        File_S parent = (File_S)(new File_S(String.valueOf(dir) + item)).getParentFile();
        while (true) {
          parent = (File_S)parent.getParentFile();
          if (parent.getName().toUpperCase().startsWith("USERS_"))
            break; 
          userSubdir = String.valueOf(parent.getName()) + "/" + userSubdir;
        } 
        Vector dirs = (Vector)user_prop.get("dirs");
        user_prop.remove("dirs");
        if (dirs == null)
          dirs = new Vector(); 
        (new File_S(String.valueOf(pathOut) + userSubdir + curUser + "/VFS/")).mkdirs();
        updateOSXInfo(String.valueOf(pathOut) + userSubdir + curUser + "/VFS/");
        Properties dir_items = new Properties();
        for (int xx = 0; xx < dirs.size(); xx++) {
          Properties p = dirs.elementAt(xx);
          if (p.getProperty("type").equals("RD")) {
            Vector user_vfs = (Vector)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"vector\">\t<VFS_subitem type=\"properties\">\t\t<url>FILE://</url>\t\t<type>dir</type>\t</VFS_subitem></VFS>".getBytes("UTF8"))).getRootElement());
            String root_dir = url_decode(p.getProperty("root_dir"));
            root_dir = replace_str(root_dir, "//", "/");
            root_dir = replace_str(root_dir, "//", "/");
            String item_name = url_decode(p.getProperty("name"));
            String subdir = "";
            if (item_name.equals("")) {
              item_name = last(url_decode(p.getProperty("dir")));
              item_name = item_name.substring(0, item_name.length() - 1);
              subdir = url_decode(p.getProperty("dir"));
              subdir = subdir.substring(0, subdir.length() - item_name.length() + 1);
              String s = String.valueOf(subdir) + item_name;
              if (!s.startsWith("/"))
                s = "/" + s; 
              if (!s.endsWith("/"))
                s = String.valueOf(s) + "/"; 
              dir_items.put(s, "dir");
            } else {
              subdir = url_decode(p.getProperty("dir"));
              root_dir = String.valueOf(root_dir) + item_name;
              root_dir = replace_str(root_dir, "//", "/");
              root_dir = replace_str(root_dir, "//", "/");
              ((Properties)user_vfs.elementAt(0)).put("type", "file");
            } 
            String theUrl = String.valueOf((new File_S(root_dir)).toURI().toURL().toExternalForm()) + ((new File_S(root_dir)).isDirectory() ? "/" : "");
            theUrl = replace_str(theUrl, "//", "/");
            theUrl = replace_str(theUrl, "//", "/");
            if (theUrl.startsWith("file:/")) {
              theUrl = replace_str(theUrl, "file:/", "FILE://");
            } else {
              theUrl = replace_str(theUrl, "FILE:/", "FILE://");
            } 
            ((Properties)user_vfs.elementAt(0)).put("url", theUrl);
            (new File_S(String.valueOf(pathOut) + userSubdir + curUser + "/VFS/" + subdir)).mkdirs();
            updateOSXInfo(String.valueOf(pathOut) + userSubdir + curUser + "/VFS/" + subdir);
            writeXMLObject(String.valueOf(pathOut) + userSubdir + curUser + "/VFS/" + subdir + item_name, user_vfs, "VFS");
          } 
        } 
        ConvertCrushFTP3Users_R_Item(dirs, String.valueOf(pathOut) + userSubdir, curUser, user_vfs_item, dir_items);
        ConvertCrushFTP3Users_R_Item(dirs, String.valueOf(pathOut) + userSubdir, curUser, user_vfs_item, dir_items);
        writeXMLObject(String.valueOf(pathOut) + userSubdir + curUser + "/VFS.XML", user_vfs_item, "VFS");
        writeXMLObject(String.valueOf(pathOut) + userSubdir + curUser + "/user.XML", user_prop, "userfile");
      } 
    } 
  }
  
  public void ConvertCrushFTP3Users_R_Item(Vector dirs, String pathOut, String curUser, Properties user_vfs_item, Properties dir_items) {
    for (int xx = 0; xx < dirs.size(); xx++) {
      Properties p = dirs.elementAt(xx);
      if (p.getProperty("type").equals("R")) {
        long l;
        String subdir = url_decode(p.getProperty("dir"));
        String item_name1 = url_decode(p.getProperty("name"));
        String item_name2 = url_decode(p.getProperty("data"));
        if (!(new File_S(String.valueOf(pathOut) + curUser + "/VFS/" + subdir + item_name2)).exists())
          (new File_S(String.valueOf(pathOut) + curUser + "/VFS/" + subdir + item_name1)).renameTo(new File_S(String.valueOf(pathOut) + curUser + "/VFS/" + subdir + item_name2)); 
        String privs = p.getProperty("privs");
        String new_privs = "";
        if (privs.indexOf("r") >= 0)
          new_privs = String.valueOf(new_privs) + "(read)"; 
        if (privs.indexOf("w") >= 0)
          new_privs = String.valueOf(new_privs) + "(write)"; 
        if (privs.indexOf("v") >= 0)
          new_privs = String.valueOf(new_privs) + "(view)"; 
        if (privs.indexOf("d") >= 0)
          new_privs = String.valueOf(new_privs) + "(delete)"; 
        if (privs.indexOf("x") >= 0)
          new_privs = String.valueOf(new_privs) + "(resume)"; 
        if (privs.indexOf("n") >= 0)
          new_privs = String.valueOf(new_privs) + "(rename)"; 
        if (privs.indexOf("k") >= 0)
          new_privs = String.valueOf(new_privs) + "(makedir)"; 
        if (privs.indexOf("m") >= 0)
          new_privs = String.valueOf(new_privs) + "(deletedir)"; 
        if (privs.indexOf("h") >= 0)
          new_privs = String.valueOf(new_privs) + "(invisible)"; 
        if (privs.indexOf("f") >= 0)
          new_privs = String.valueOf(new_privs) + "(ratio)"; 
        if (privs.indexOf("e") >= 0)
          new_privs = String.valueOf(new_privs) + "(stealupload)"; 
        if (privs.indexOf("b") >= 0)
          new_privs = String.valueOf(new_privs) + "(bypassqueue)"; 
        String quota = "";
        if (privs.indexOf("q") >= 0)
          l = Long.parseLong(privs.substring(privs.indexOf("q") + 1)) / 1024L; 
        if (privs.indexOf("q") >= 0)
          new_privs = String.valueOf(new_privs) + "(quota" + l + ")"; 
        String s = String.valueOf(subdir) + item_name1;
        if (!s.startsWith("/"))
          s = "/" + s; 
        if (!s.endsWith("/"))
          s = String.valueOf(s) + "/"; 
        user_vfs_item.put(String.valueOf((String.valueOf(subdir) + item_name2).toUpperCase()) + (dir_items.containsKey(s) ? "/" : ""), new_privs);
      } 
    } 
  }
  
  public void convertTabDelimited(String pathToWebStartFile, String user_path) throws Exception {
    String pathOut = String.valueOf(System.getProperties().getProperty("crushftp.users")) + user_path;
    String serverGroup = user_path;
    if (serverGroup.endsWith("/"))
      serverGroup = serverGroup.substring(0, serverGroup.length() - 1); 
    Properties groups = UserTools.getGroups(serverGroup);
    Properties inheritance = UserTools.getInheritance(serverGroup);
    BufferedReader br = null;
    try {
      br = new BufferedReader(new InputStreamReader(new FileInputStream(new File_S(pathToWebStartFile))));
      String data = "";
      while ((data = br.readLine()) != null) {
        StringTokenizer get_em = new StringTokenizer(data, "\t");
        String curUser = get_em.nextToken().trim();
        String curPassword = get_em.nextToken();
        if (!curPassword.startsWith("MD5:") && !curPassword.startsWith("MD5CRYPT:") && !curPassword.startsWith("PBKDF2SHA256:") && !curPassword.startsWith("SHA512CRYPT:") && !curPassword.startsWith("BCRYPT:") && !curPassword.startsWith("CRYPT3:") && !curPassword.startsWith("MD4:") && !curPassword.startsWith("SHA512:") && !curPassword.startsWith("SHA256:") && !curPassword.startsWith("SHA3:"))
          curPassword = encode_pass(curPassword, ServerStatus.SG("password_encryption"), "").trim(); 
        String root_dir = get_em.nextToken().trim();
        root_dir = replace_str(root_dir, "ROOT:", "/");
        if ((root_dir.split(":")).length > 2)
          root_dir = root_dir.replace(':', '/'); 
        String permissions = "(read)(write)(view)(delete)(resume)(rename)(makedir)(deletedir)";
        if (get_em.hasMoreElements())
          permissions = get_em.nextToken().trim(); 
        if (permissions.indexOf("(") < 0 && permissions.indexOf(")") < 0)
          permissions = "(read)(write)(view)(delete)(resume)(rename)(makedir)(deletedir)"; 
        String email = "";
        String first_name = "";
        String last_name = "";
        String group = "";
        String notes = "";
        String salt = "";
        if (get_em.hasMoreElements())
          email = get_em.nextToken().trim(); 
        if (get_em.hasMoreElements())
          first_name = get_em.nextToken().trim(); 
        if (get_em.hasMoreElements())
          last_name = get_em.nextToken().trim(); 
        if (get_em.hasMoreElements())
          group = get_em.nextToken().trim(); 
        if (get_em.hasMoreElements())
          notes = get_em.nextToken().trim(); 
        if (get_em.hasMoreElements())
          salt = get_em.nextToken().trim(); 
        Properties user_prop = (Properties)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><userfile type=\"properties\">\t<password>Acf9C+U0B0UvQiwbMd9Km+uEHaQO/nLf</password>\t<root_dir>/</root_dir>\t<version>1.0</version>\t<max_logins>0</max_logins></userfile>".getBytes("UTF8"))).getRootElement());
        if ((new File_S(String.valueOf(pathOut) + curUser + "/user.XML")).exists())
          user_prop = (Properties)readXMLObject(String.valueOf(pathOut) + curUser + "/user.XML"); 
        Vector user_vfs = (Vector)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"vector\">\t<VFS_subitem type=\"properties\">\t\t<url>FILE://</url>\t\t<type>dir</type>\t</VFS_subitem></VFS>".getBytes("UTF8"))).getRootElement());
        Properties user_vfs_item = (Properties)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"properties\">\t<item name=\"/\">(read)(view)(resume)</item></VFS>".getBytes("UTF8"))).getRootElement());
        if ((new File_S(String.valueOf(pathOut) + curUser + "/VFS.XML")).exists())
          user_vfs_item = (Properties)readXMLObject(String.valueOf(pathOut) + curUser + "/VFS.XML"); 
        String newUrl = (new File_S(root_dir)).toURI().toURL().toExternalForm();
        (new File_S(root_dir)).mkdirs();
        if (!newUrl.endsWith("/") && !newUrl.endsWith("\\"))
          newUrl = String.valueOf(newUrl) + "/"; 
        if (newUrl.toLowerCase().startsWith("file:/") && !newUrl.toLowerCase().startsWith("file://"))
          newUrl = "FILE://" + newUrl.substring("file:/".length()); 
        ((Properties)user_vfs.elementAt(0)).put("url", newUrl);
        user_prop.put("password", curPassword);
        if (!salt.equals(""))
          user_prop.put("salt", salt); 
        if (!email.equals(""))
          user_prop.put("email", email); 
        if (!first_name.equals(""))
          user_prop.put("first_name", first_name); 
        if (!last_name.equals(""))
          user_prop.put("last_name", last_name); 
        if (!notes.equals(""))
          user_prop.put("notes", notes); 
        if (!group.equals("")) {
          Vector v = (Vector)groups.get(group);
          if (v == null)
            v = new Vector(); 
          groups.put(group, v);
          v.addElement(curUser.toUpperCase());
          v = (Vector)inheritance.get(curUser);
          if (v == null)
            v = new Vector(); 
          v.addElement(group);
          inheritance.put(curUser, v);
        } 
        user_vfs_item.put("/" + (new File_S(root_dir)).getName().toUpperCase() + "/", permissions);
        (new File_S(String.valueOf(pathOut) + curUser)).mkdirs();
        updateOSXInfo(String.valueOf(pathOut) + curUser);
        (new File_S(String.valueOf(pathOut) + curUser + "/VFS/")).mkdirs();
        updateOSXInfo(String.valueOf(pathOut) + curUser + "/VFS/");
        writeXMLObject(String.valueOf(pathOut) + curUser + "/user.XML", user_prop, "userfile");
        writeXMLObject(String.valueOf(pathOut) + curUser + "/VFS/" + (new File_S(root_dir)).getName(), user_vfs, "VFS");
        writeXMLObject(String.valueOf(pathOut) + curUser + "/VFS.XML", user_vfs_item, "VFS");
      } 
    } finally {
      br.close();
      UserTools.writeGroups(serverGroup, groups);
      UserTools.writeInheritance(serverGroup, inheritance);
    } 
  }
  
  public static String importCSV(Properties request, String serverGroup) throws Exception {
    String results = "";
    if (serverGroup.endsWith("/"))
      serverGroup = serverGroup.substring(0, serverGroup.length() - 1); 
    Properties groups = UserTools.getGroups(serverGroup);
    Properties inheritance = UserTools.getInheritance(serverGroup);
    Properties xref = new Properties();
    for (int x = 0; x < 50; x++) {
      if (request.containsKey("col" + x))
        xref.put((new StringBuffer(String.valueOf(x))).toString(), request.getProperty("col" + x)); 
    } 
    BufferedReader br = null;
    Vector current_user_group_listing = new Vector();
    UserTools.refreshUserList(serverGroup, current_user_group_listing);
    String last_line = "";
    try {
      br = new BufferedReader(new InputStreamReader(new FileInputStream(new File_S(request.getProperty("the_dir")))));
      String data = "";
      int loops = 0;
      while ((data = br.readLine()) != null) {
        last_line = data;
        loops++;
        if (loops == 1 && request.getProperty("first_header", "false").equalsIgnoreCase("true"))
          continue; 
        String[] parts = data.split(request.getProperty("csv_separator"));
        Properties ref = new Properties();
        int loop = 0;
        while (loop < parts.length) {
          String s = parts[loop].trim();
          if (!xref.getProperty((new StringBuffer(String.valueOf(loop))).toString()).equals(""))
            if (xref.getProperty((new StringBuffer(String.valueOf(loop))).toString()).equals("linked_vfs")) {
              Vector v = new Vector();
              for (int i = 0; i < (s.split(",")).length; i++) {
                if (!s.split(",")[i].trim().equals(""))
                  v.addElement(s.split(",")[i].trim()); 
              } 
              ref.put(xref.getProperty((new StringBuffer(String.valueOf(loop))).toString()), v);
            } else {
              ref.put(xref.getProperty((new StringBuffer(String.valueOf(loop))).toString()), s.trim());
            }  
          loop++;
        } 
        boolean has_pass = ref.containsKey("user_pass");
        Object object1 = ref.remove("user_pass");
        if (request.getProperty("password_type", "").equalsIgnoreCase("md5saltedhash")) {
          String salt = "";
          int chars = Integer.parseInt(request.getProperty("salted_x_char"));
          if (chars < 0) {
            salt = object1.substring(0, Math.abs(chars));
            object1 = object1.substring(salt.length());
          } else {
            salt = object1.substring(object1.length() - Math.abs(chars));
            object1 = object1.substring(0, object1.length() - salt.length());
          } 
          object1 = "MD5:" + object1.toLowerCase();
          if (chars < 0)
            salt = "!" + salt; 
          ref.put("salt", salt);
        } else {
          if (request.getProperty("password_type", "").equalsIgnoreCase("md5hash")) {
            object1 = "MD5:" + object1.toLowerCase();
          } else if (request.getProperty("password_type", "").equalsIgnoreCase("md5crypt")) {
            object1 = "MD5CRYPT:" + object1;
          } else if (request.getProperty("password_type", "").equalsIgnoreCase("pbkdf2sha256")) {
            object1 = "PBKDF2SHA256:" + object1;
          } else if (request.getProperty("password_type", "").equalsIgnoreCase("sha512crypt")) {
            object1 = "SHA512CRYPT:" + object1;
          } else if (request.getProperty("password_type", "").equalsIgnoreCase("bcrypt")) {
            object1 = "BCRYPT:" + object1;
          } else if (request.getProperty("password_type", "").equalsIgnoreCase("sha256")) {
            object1 = "SHA256:" + object1.toLowerCase();
          } else if (request.getProperty("password_type", "").equalsIgnoreCase("sha512")) {
            object1 = "SHA512:" + object1.toLowerCase();
          } 
          if (!object1.startsWith("MD5:") && !object1.startsWith("MD5CRYPT:") && !object1.startsWith("PBKDF2SHA256:") && !object1.startsWith("SHA512CRYPT:") && !object1.startsWith("BCRYPT:") && !object1.startsWith("CRYPT3:") && !object1.startsWith("MD4:") && !object1.startsWith("SHA512:") && !object1.startsWith("SHA256:") && !object1.startsWith("SHA3:"))
            object1 = ServerStatus.thisObj.common_code.encode_pass((String)object1, ServerStatus.SG("password_encryption"), "").trim(); 
        } 
        if (has_pass)
          ref.put("password", object1); 
        Object object2 = ref.remove("home_folder");
        Properties virtual = null;
        if (!object2.equals("null")) {
          Object object = "(read)(write)(view)(delete)(resume)(rename)(makedir)(deletedir)";
          if (!ref.getProperty("permissions", "").equals(""))
            object = ref.remove("permissions"); 
          if (object.indexOf("(") < 0 && object.indexOf(")") < 0)
            object = "(read)(write)(view)(delete)(resume)(rename)(makedir)(deletedir)"; 
          virtual = UserTools.ut.getVirtualVFS(serverGroup, ref.getProperty("user_name"));
          Vector vfs_permissions_object = (Vector)virtual.get("vfs_permissions_object");
          if (vfs_permissions_object == null) {
            vfs_permissions_object = new Vector();
            vfs_permissions_object.add(new Properties());
          } 
          ((Properties)vfs_permissions_object.elementAt(0)).put("/" + (new File_S((String)object2)).getName().toUpperCase() + "/", object);
          if (!((Properties)vfs_permissions_object.elementAt(0)).containsKey("/"))
            ((Properties)vfs_permissions_object.elementAt(0)).put("/", "(read)(view)(resume)"); 
          String newUrl = null;
          String item_name = null;
          Properties vfs_prop = new Properties();
          if (object2.toLowerCase().startsWith("s3:") || object2.toLowerCase().startsWith("s3crush:")) {
            Object object4 = object2;
            VRL vrl = new VRL((String)object4);
            item_name = vrl.getName();
            vfs_prop.put("secretKeyID", vrl.getUsername());
            vfs_prop.put("secretKey", vrl.getPassword());
            vfs_prop.put("type", "DIR");
          } else if (object2.toLowerCase().startsWith("file:") || object2.toLowerCase().startsWith("ftp:") || object2.toLowerCase().startsWith("ftpes:") || object2.toLowerCase().startsWith("ftps:") || object2.toLowerCase().startsWith("sftp:") || object2.toLowerCase().startsWith("http:") || object2.toLowerCase().startsWith("https:") || object2.toLowerCase().startsWith("webdav:") || object2.toLowerCase().startsWith("webdavs:") || object2.toLowerCase().startsWith("smb:") || object2.toLowerCase().startsWith("smb3:")) {
            Object object4 = object2;
            VRL vrl = new VRL((String)object4);
            item_name = vrl.getName();
            vfs_prop.put("type", "DIR");
          } else {
            newUrl = (new File_S((String)object2)).toURI().toURL().toExternalForm();
            (new File_S((String)object2)).mkdirs();
            if (!newUrl.endsWith("/") && !newUrl.endsWith("\\"))
              newUrl = String.valueOf(newUrl) + "/"; 
            if (newUrl.toLowerCase().startsWith("file:/") && !newUrl.toLowerCase().startsWith("file://"))
              newUrl = "FILE://" + newUrl.substring("file:/".length()); 
            item_name = (new File_S((String)object2)).getName();
            vfs_prop.put("type", "FILE");
          } 
          if (item_name.endsWith("/"))
            item_name = item_name.substring(0, item_name.length() - 1); 
          Vector user_vfs = new Vector();
          user_vfs.addElement(vfs_prop);
          vfs_prop.put("url", newUrl);
          Properties pp = new Properties();
          pp.put("virtualPath", "/" + item_name);
          pp.put("name", item_name);
          pp.put("type", "FILE");
          pp.put("vItems", user_vfs);
          virtual.put("/" + item_name, pp);
          ref.put("root_dir", "/");
        } 
        if (ref.getProperty("user_name") == null)
          throw new Exception("Username is required for import."); 
        Properties user_prop = UserTools.ut.getUser(serverGroup, ref.getProperty("user_name"), false);
        boolean add_all = false;
        if (user_prop == null) {
          user_prop = ref;
        } else {
          add_all = true;
        } 
        if (user_prop.containsKey("linked_vfs")) {
          if (user_prop.get("linked_vfs") instanceof String) {
            String[] s = user_prop.get("linked_vfs").toString().split(",");
            Vector vector = new Vector();
            for (int j = 0; j < s.length; j++) {
              if (!s[j].trim().equals(""))
                vector.addElement(s[j].trim()); 
            } 
            user_prop.put("linked_vfs", vector);
          } 
          Vector v = (Vector)user_prop.get("linked_vfs");
          Vector v2 = (Vector)ref.get("linked_vfs");
          for (int i = 0; v2 != null && i < v2.size(); i++) {
            if (v.indexOf(v2.elementAt(i)) < 0)
              v.addElement(v2.elementAt(i)); 
          } 
          ref.put("linked_vfs", v);
        } 
        if (add_all)
          user_prop.putAll(ref); 
        Object object3 = ref.remove("userGroup");
        if (object3.equalsIgnoreCase("null"))
          object3 = ""; 
        if (object3.equals("") && !request.getProperty("default_group", "").equals("all") && !request.getProperty("default_group", "").equals("notingroup"))
          object3 = request.getProperty("default_group"); 
        if (!object3.equals("")) {
          Vector v = (Vector)groups.get(object3);
          if (v == null)
            v = new Vector(); 
          groups.put(object3, v);
          v.addElement(ref.getProperty("user_name"));
          if (current_user_group_listing.indexOf(object3) >= 0) {
            v = (Vector)inheritance.get(ref.getProperty("user_name"));
            if (v == null)
              v = new Vector(); 
            v.addElement(object3);
            inheritance.put(ref.getProperty("user_name"), v);
          } 
        } 
        if (!ref.getProperty("copy_user", "").equals("")) {
          Properties tmp_user = UserTools.ut.getUser(serverGroup, ref.getProperty("copy_user", ""), false);
          if (tmp_user != null) {
            tmp_user.putAll(ref);
            tmp_user = ref;
          } 
        } 
        UserTools.writeUser(serverGroup, ref.getProperty("user_name"), ref);
        if (virtual != null)
          UserTools.writeVFS(serverGroup, ref.getProperty("user_name"), virtual); 
      } 
    } catch (Exception e) {
      Log.log("SERVER", 0, "Last line:" + last_line);
      Log.log("SERVER", 0, e);
      throw new Exception(e + ":" + last_line);
    } finally {
      br.close();
      UserTools.writeGroups(serverGroup, groups);
      UserTools.writeInheritance(serverGroup, inheritance);
    } 
    return results;
  }
  
  public void convertWingFTP(String pathToXml, String user_path, String prefix) throws Exception {
    String pathOut = String.valueOf(System.getProperties().getProperty("crushftp.users")) + user_path;
    String serverGroup = user_path;
    if (serverGroup.endsWith("/"))
      serverGroup = serverGroup.substring(0, serverGroup.length() - 1); 
    File_S[] files = (File_S[])(new File_S(pathToXml)).listFiles();
    Exception exception = null;
    for (int x = 0; x < files.length; x++) {
      File_S f = files[x];
      if (f.getName().toUpperCase().endsWith(".XML")) {
        String curUser = "";
        try {
          Document doc = getSaxBuilder().build(f);
          Element USER = doc.getRootElement().getChild("USER");
          List props = USER.getChildren();
          Properties user_prop = (Properties)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><userfile type=\"properties\">\t<password>Acf9C+U0B0UvQiwbMd9Km+uEHaQO/nLf</password>\t<root_dir>/</root_dir>\t<version>1.0</version>\t<max_logins>0</max_logins></userfile>".getBytes("UTF8"))).getRootElement());
          Properties user_prop2 = new Properties();
          Properties vfs_prop2 = new Properties();
          for (int xx = 0; xx < props.size(); xx++) {
            Element prop = props.get(xx);
            if (prop.getName().equalsIgnoreCase("folder")) {
              List folders = prop.getChildren();
              for (int xxx = 0; xxx < folders.size(); xxx++) {
                Element vfse = folders.get(xxx);
                vfs_prop2.put(vfse.getName().toLowerCase(), vfse.getText());
              } 
            } else {
              user_prop2.put(prop.getName().toLowerCase(), prop.getText());
            } 
          } 
          curUser = user_prop2.getProperty("username");
          user_prop.put("user_name", user_prop2.getProperty("username"));
          user_prop.put("password", String.valueOf(prefix) + user_prop2.getProperty("password"));
          user_prop.put("max_logins", user_prop2.getProperty("maxconnection"));
          user_prop.put("max_logins_ip", user_prop2.getProperty("connectionperip"));
          if (!user_prop2.getProperty("enableaccount", "1").equals("1"))
            user_prop.put("max_logins", "-1"); 
          if (!user_prop2.getProperty("enableexpire", "0").equals("1")) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            SimpleDateFormat sdf2 = new SimpleDateFormat("MMddyyyyHHmm", Locale.US);
            user_prop.put("account_expire", sdf2.format(sdf.parse(user_prop2.getProperty("expiretime"))));
            user_prop.put("account_expire_delete", "false");
          } 
          user_prop.put("speed_limit_download", user_prop2.getProperty("maxdownloadspeedperuser"));
          user_prop.put("speed_limit_upload", user_prop2.getProperty("maxuploadspeedperuser"));
          user_prop.put("ssh_public_keys", user_prop2.getProperty("sshpublickeypath"));
          user_prop.put("notes", String.valueOf(user_prop.getProperty("notes", "")) + " name:" + user_prop2.getProperty("notesname"));
          user_prop.put("notes", String.valueOf(user_prop.getProperty("notes", "")) + " address:" + user_prop2.getProperty("notesaddress"));
          user_prop.put("notes", String.valueOf(user_prop.getProperty("notes", "")) + " zip:" + user_prop2.getProperty("noteszipcode"));
          user_prop.put("notes", String.valueOf(user_prop.getProperty("notes", "")) + " phone:" + user_prop2.getProperty("notesphone"));
          user_prop.put("notes", String.valueOf(user_prop.getProperty("notes", "")) + " fax:" + user_prop2.getProperty("notesfax"));
          user_prop.put("notes", String.valueOf(user_prop.getProperty("notes", "")) + " memo:" + user_prop2.getProperty("notesmemo"));
          user_prop.put("email", user_prop2.getProperty("notesemail"));
          Vector user_vfs = (Vector)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"vector\">\t<VFS_subitem type=\"properties\">\t\t<url>FILE://</url>\t\t<type>dir</type>\t</VFS_subitem></VFS>".getBytes("UTF8"))).getRootElement());
          Properties user_vfs_item = (Properties)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"properties\">\t<item name=\"/\">(read)(view)(resume)</item></VFS>".getBytes("UTF8"))).getRootElement());
          File_S home_folder = new File_S(vfs_prop2.getProperty("path"));
          String newUrl = home_folder.toURI().toURL().toExternalForm();
          if (!newUrl.endsWith("/") && !newUrl.endsWith("\\"))
            newUrl = String.valueOf(newUrl) + "/"; 
          ((Properties)user_vfs.elementAt(0)).put("url", newUrl);
          String permissions = "";
          if (vfs_prop2.getProperty("file_read", "").equals("1"))
            permissions = String.valueOf(permissions) + "(read)"; 
          if (vfs_prop2.getProperty("file_write", "").equals("1"))
            permissions = String.valueOf(permissions) + "(write)"; 
          if (vfs_prop2.getProperty("file_append", "").equals("1"))
            permissions = String.valueOf(permissions) + "(resume)"; 
          if (vfs_prop2.getProperty("file_delete", "").equals("1"))
            permissions = String.valueOf(permissions) + "(delete)"; 
          if (vfs_prop2.getProperty("file_rename", "").equals("1"))
            permissions = String.valueOf(permissions) + "(rename)"; 
          if (vfs_prop2.getProperty("directory_list", "").equals("1"))
            permissions = String.valueOf(permissions) + "(view)"; 
          if (vfs_prop2.getProperty("directory_rename", "").equals("1"))
            permissions = String.valueOf(permissions) + "(rename)"; 
          if (vfs_prop2.getProperty("directory_make", "").equals("1"))
            permissions = String.valueOf(permissions) + "(makedirectory)"; 
          if (vfs_prop2.getProperty("directory_delete", "").equals("1"))
            permissions = String.valueOf(permissions) + "(deletedirectory)"; 
          user_vfs_item.put("/" + home_folder.getName().toUpperCase() + "/", permissions);
          (new File_S(String.valueOf(pathOut) + user_prop.getProperty("user_name"))).mkdirs();
          updateOSXInfo(String.valueOf(pathOut) + user_prop.getProperty("user_name"));
          (new File_S(String.valueOf(pathOut) + user_prop.getProperty("user_name") + "/VFS/")).mkdirs();
          updateOSXInfo(String.valueOf(pathOut) + user_prop.getProperty("user_name") + "/VFS/");
          writeXMLObject(String.valueOf(pathOut) + user_prop.getProperty("user_name") + "/user.XML", user_prop, "userfile");
          writeXMLObject(String.valueOf(pathOut) + user_prop.getProperty("user_name") + "/VFS/" + home_folder.getName(), user_vfs, "VFS");
          writeXMLObject(String.valueOf(pathOut) + user_prop.getProperty("user_name") + "/VFS.XML", user_vfs_item, "VFS");
        } catch (Exception e) {
          if (exception == null)
            exception = e; 
          Log.log("SERVER", 0, "WingFTP:" + curUser);
          Log.log("SERVER", 0, e);
        } 
      } 
    } 
    if (exception != null)
      throw exception; 
  }
  
  public void convertFilezilla(String pathToXml, String user_path) throws Exception {
    String pathOut = String.valueOf(System.getProperties().getProperty("crushftp.users")) + user_path;
    String serverGroup = user_path;
    if (serverGroup.endsWith("/"))
      serverGroup = serverGroup.substring(0, serverGroup.length() - 1); 
    Properties groups = UserTools.getGroups(serverGroup);
    Properties inheritance = UserTools.getInheritance(serverGroup);
    try {
      Document doc = getSaxBuilder().build(new File_S(pathToXml));
      Element rootElement = doc.getRootElement();
      List roots = rootElement.getChildren();
      for (int x = 0; x < roots.size(); x++) {
        Element rootItem = roots.get(x);
        if (!rootItem.getName().equalsIgnoreCase("Settings"))
          if (rootItem.getName().equalsIgnoreCase("Groups")) {
            List groupList = rootItem.getChildren();
            for (int xx = 0; xx < groupList.size(); xx++) {
              Element groupItem = groupList.get(xx);
              String curUser = groupItem.getAttributeValue("Name");
              Log.log("SERVER", 0, "Importing filezilla group:" + curUser);
              Vector v = (Vector)groups.get(curUser);
              if (v == null)
                v = new Vector(); 
              groups.put(curUser, v);
              (new File_S(String.valueOf(pathOut) + curUser)).mkdirs();
              updateOSXInfo(String.valueOf(pathOut) + curUser);
              (new File_S(String.valueOf(pathOut) + curUser + "/VFS/")).mkdirs();
              updateOSXInfo(String.valueOf(pathOut) + curUser + "/VFS/");
              Properties user_prop = convertFileZillaUser(groupItem, curUser, pathOut, groups, inheritance);
              user_prop.put("max_logins", "-1");
              user_prop.put("password", "");
              writeXMLObject(String.valueOf(pathOut) + curUser + "/user.XML", user_prop, "userfile");
            } 
          } else if (rootItem.getName().equalsIgnoreCase("Users")) {
            List userList = rootItem.getChildren();
            for (int xx = 0; xx < userList.size(); xx++) {
              Element userItem = userList.get(xx);
              String curUser = userItem.getAttributeValue("Name");
              Log.log("SERVER", 0, "Importing filezilla user:" + curUser);
              (new File_S(String.valueOf(pathOut) + curUser)).mkdirs();
              updateOSXInfo(String.valueOf(pathOut) + curUser);
              (new File_S(String.valueOf(pathOut) + curUser + "/VFS/")).mkdirs();
              updateOSXInfo(String.valueOf(pathOut) + curUser + "/VFS/");
              Properties user_prop = convertFileZillaUser(userItem, curUser, pathOut, groups, inheritance);
              writeXMLObject(String.valueOf(pathOut) + curUser + "/user.XML", user_prop, "userfile");
            } 
          }  
      } 
    } finally {
      UserTools.writeGroups(serverGroup, groups);
      UserTools.writeInheritance(serverGroup, inheritance);
    } 
  }
  
  public Properties convertFileZillaUser(Element userElement, String curUser, String pathOut, Properties groups, Properties inheritance) throws Exception {
    Properties user_prop = new Properties();
    user_prop.put("version", "1.0");
    user_prop.put("userVersion", "6");
    user_prop.put("root_dir", "/");
    Properties user_vfs_item = (Properties)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"properties\">\t<item name=\"/\">(read)(view)(resume)</item></VFS>".getBytes("UTF8"))).getRootElement());
    List options = userElement.getChildren();
    for (int x = 0; x < options.size(); x++) {
      Element option = options.get(x);
      String optionName = option.getAttributeValue("Name");
      if (optionName == null)
        optionName = option.getName(); 
      Log.log("SERVER", 0, "Importing filezilla user:" + curUser + " Options:" + optionName);
      if (optionName.equalsIgnoreCase("User Limit")) {
        user_prop.put("max_logins", option.getText());
      } else if (optionName.equalsIgnoreCase("IP Limit")) {
        user_prop.put("max_logins_ip", option.getText());
      } else if (optionName.equalsIgnoreCase("Enabled") && option.getText().equals("0")) {
        user_prop.put("max_logins", "-1");
      } else if (optionName.equalsIgnoreCase("Comments")) {
        user_prop.put("notes", option.getText());
      } else if (optionName.equalsIgnoreCase("ForceSsl")) {
        user_prop.put("require_encryption", (new StringBuffer(String.valueOf(!option.getText().equals("0")))).toString());
      } else if (optionName.equalsIgnoreCase("Pass")) {
        user_prop.put("password", "MD5:" + option.getText().toLowerCase());
      } else if (optionName.equalsIgnoreCase("Group")) {
        if (!option.getText().equals("")) {
          Vector v = (Vector)inheritance.get(curUser);
          if (v == null)
            v = new Vector(); 
          v.addElement(option.getText());
          inheritance.put(curUser, v);
          v = (Vector)groups.get(option.getText());
          if (v != null)
            v.addElement(curUser); 
        } 
      } else if (optionName.equalsIgnoreCase("Permissions")) {
        List permissions = option.getChildren();
        for (int xx = 0; xx < permissions.size(); xx++) {
          Vector user_vfs = (Vector)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"vector\">\t<VFS_subitem type=\"properties\">\t\t<url>FILE://</url>\t\t<type>dir</type>\t</VFS_subitem></VFS>".getBytes("UTF8"))).getRootElement());
          Element permission = permissions.get(xx);
          String dir = permission.getAttributeValue("Dir").replace('\\', '/');
          Log.log("SERVER", 0, "Importing filezilla user:" + curUser + " folder permission:" + dir);
          if (dir.indexOf(":u") >= 0)
            dir = dir.substring(0, dir.indexOf(":u")); 
          if (!dir.endsWith("/"))
            dir = String.valueOf(dir) + "/"; 
          ((Properties)user_vfs.elementAt(0)).put("url", "file://" + dir);
          String privs = "";
          List permissionOptions = permission.getChildren();
          for (int xxx = 0; xxx < permissionOptions.size(); xxx++) {
            Element perm = permissionOptions.get(xxx);
            if (perm.getAttributeValue("Name") != null)
              if (perm.getAttributeValue("Name").equals("FileRead") && perm.getText().equals("1")) {
                privs = String.valueOf(privs) + "(read)";
              } else if (perm.getAttributeValue("Name").equals("FileWrite") && perm.getText().equals("1")) {
                privs = String.valueOf(privs) + "(write)(rename)";
              } else if (perm.getAttributeValue("Name").equals("FileDelete") && perm.getText().equals("1")) {
                privs = String.valueOf(privs) + "(delete)";
              } else if (perm.getAttributeValue("Name").equals("FileAppend") && perm.getText().equals("1")) {
                privs = String.valueOf(privs) + "(resume)";
              } else if (perm.getAttributeValue("Name").equals("DirCreate") && perm.getText().equals("1")) {
                privs = String.valueOf(privs) + "(makedir)";
              } else if (perm.getAttributeValue("Name").equals("DirDelete") && perm.getText().equals("1")) {
                privs = String.valueOf(privs) + "(deletedir)";
              } else if (perm.getAttributeValue("Name").equals("DirList") && perm.getText().equals("1")) {
                privs = String.valueOf(privs) + "(view)";
              }  
          } 
          Log.log("SERVER", 0, "Importing filezilla user:" + curUser + " folder permission:" + dir + " privs:" + privs);
          user_vfs_item.put("/" + (new File_S(dir)).getName().toUpperCase() + "/", privs);
          writeXMLObject(String.valueOf(pathOut) + curUser + "/VFS/" + (new File_S(dir)).getName(), user_vfs, "VFS");
        } 
        writeXMLObject(String.valueOf(pathOut) + curUser + "/VFS.XML", user_vfs_item, "VFS");
      } 
    } 
    return user_prop;
  }
  
  public void ConvertPasswdUsers(String path, String user_path, String prefix) throws Exception {
    String pathOut = String.valueOf(System.getProperties().getProperty("crushftp.users")) + user_path;
    BufferedReader in = new BufferedReader(new FileReader(new File_S(path)));
    String the_user = "";
    int line = 1;
    while ((the_user = in.readLine()) != null) {
      try {
        StringTokenizer get_em = new StringTokenizer(the_user, ":");
        int tokenCount = 0;
        while (get_em.hasMoreElements()) {
          get_em.nextToken();
          tokenCount++;
        } 
        get_em = new StringTokenizer(the_user, ":");
        String curUser = get_em.nextToken();
        String curPassword = String.valueOf(prefix) + get_em.nextToken().trim();
        get_em.nextToken();
        get_em.nextToken();
        if (tokenCount >= 10) {
          get_em.nextToken();
          get_em.nextToken();
        } 
        String fullName = get_em.nextToken();
        String root_dir = get_em.nextToken();
        String root_dir2 = get_em.nextToken();
        if (!root_dir.startsWith("/") && root_dir2.startsWith("/") && root_dir2.indexOf("passwd") < 0 && root_dir2.indexOf("/bin") < 0)
          root_dir = root_dir2; 
        if (!root_dir.endsWith("/"))
          root_dir = String.valueOf(root_dir) + "/"; 
        Properties user_prop = (Properties)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><userfile type=\"properties\">\t<password>Acf9C+U0B0UvQiwbMd9Km+uEHaQO/nLf</password>\t<root_dir>/</root_dir>\t<version>1.0</version>\t<max_logins>0</max_logins></userfile>".getBytes("UTF8"))).getRootElement());
        Vector user_vfs = (Vector)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"vector\">\t<VFS_subitem type=\"properties\">\t\t<url>FILE://</url>\t\t<type>dir</type>\t</VFS_subitem></VFS>".getBytes("UTF8"))).getRootElement());
        Properties user_vfs_item = (Properties)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"properties\">\t<item name=\"/\">(read)(view)(resume)</item></VFS>".getBytes("UTF8"))).getRootElement());
        ((Properties)user_vfs.elementAt(0)).put("url", (new File_S(root_dir)).toURI().toURL().toExternalForm());
        user_prop.put("password", curPassword);
        user_prop.put("notes", "Name: " + fullName);
        user_prop.put("site", "(SITE_PASS)");
        user_vfs_item.put("/" + (new File_S(root_dir)).getName() + "/", "(read)(write)(view)(delete)(resume)(rename)(makedir)(deletedir)");
        (new File_S(String.valueOf(pathOut) + curUser)).mkdirs();
        updateOSXInfo(String.valueOf(pathOut) + curUser);
        (new File_S(String.valueOf(pathOut) + curUser + "/VFS/")).mkdirs();
        updateOSXInfo(String.valueOf(pathOut) + curUser + "/VFS/");
        writeXMLObject(String.valueOf(pathOut) + curUser + "/user.XML", user_prop, "userfile");
        writeXMLObject(String.valueOf(pathOut) + curUser + "/VFS/" + (new File_S(root_dir)).getName(), user_vfs, "VFS");
        writeXMLObject(String.valueOf(pathOut) + curUser + "/VFS.XML", user_vfs_item, "VFS");
      } catch (Exception e) {
        Log.log("SERVER", 0, "IMPORT ERROR: (line " + line + ") " + the_user);
        Log.log("SERVER", 1, e);
      } 
      line++;
    } 
  }
  
  int proFTPDLineCount = 0;
  
  public void ConvertProFTPDGroups(String serverGroup, String path, String user_path) {
    Properties inheritance = UserTools.getInheritance(serverGroup);
    this.proFTPDLineCount = 0;
    try {
      Vector groups = new Vector();
      BufferedReader in = new BufferedReader(new FileReader(new File_S(path)));
      String the_user = "";
      while ((the_user = in.readLine()) != null)
        groups.addElement(the_user); 
      in.close();
      Thread t1 = new Thread(new null.grouper(this, groups, user_path, serverGroup));
      t1.start();
      Thread t2 = new Thread(new null.grouper(this, groups, user_path, serverGroup));
      t2.start();
      Thread t3 = new Thread(new null.grouper(this, groups, user_path, serverGroup));
      t3.start();
      while (groups.size() > 0)
        Thread.sleep(1000L); 
      Thread.sleep(10000L);
    } catch (Exception e) {
      Log.log("SERVER", 0, e);
    } 
  }
  
  public void ConvertRumpusUsers(String pathToRumpustFile, String user_path) throws Exception {
    String pathOut = String.valueOf(System.getProperties().getProperty("crushftp.users")) + user_path;
    String user_info = "";
    RandomAccessFile user_is = new RandomAccessFile(new File_S(pathToRumpustFile), "r");
    int user_data_len = (int)user_is.length();
    byte[] temp_array = new byte[user_data_len];
    user_is.read(temp_array);
    user_is.close();
    user_info = new String(temp_array, 0, user_data_len, "UTF8");
    user_info = String.valueOf(user_info.trim()) + "\r";
    int loc = 0;
    while (loc >= 0) {
      if (user_info.indexOf("\r", loc) < 0)
        break; 
      String the_user = String.valueOf(user_info.substring(loc, user_info.indexOf("\r", loc)).trim()) + "\t";
      loc = user_info.indexOf("\r", loc);
      if (loc > 0)
        loc++; 
      StringTokenizer get_em = new StringTokenizer(the_user, "\t");
      String curUser = get_em.nextToken();
      String curPassword = get_em.nextToken();
      if (curPassword.toUpperCase().startsWith("MCRYPT:"))
        curPassword = "-AUTO-SET-ON-LOGIN-"; 
      if (curUser.toUpperCase().equals("ANONYMOUS")) {
        curPassword = "";
      } else {
        curPassword = encode_pass(curPassword, ServerStatus.SG("password_encryption"), "");
      } 
      String root_dir = get_em.nextToken();
      if (root_dir.indexOf(":") >= 0) {
        root_dir = root_dir.substring(root_dir.indexOf(":"));
        root_dir = "/Volumes" + root_dir;
        root_dir = root_dir.replace(':', '/');
      } 
      String permissions = get_em.nextToken().toUpperCase();
      String p2 = "(resume)";
      if (permissions.charAt(1) == 'Y')
        p2 = String.valueOf(p2) + "(read)"; 
      if (permissions.charAt(2) == 'Y')
        p2 = String.valueOf(p2) + "(write)(rename)"; 
      if (permissions.charAt(3) == 'Y')
        p2 = String.valueOf(p2) + "(delete)(rename)"; 
      if (permissions.charAt(4) == 'Y')
        p2 = String.valueOf(p2) + "(makedir)(rename)"; 
      if (permissions.charAt(5) == 'Y')
        p2 = String.valueOf(p2) + "(deletedir)"; 
      if (permissions.charAt(7) == 'Y')
        p2 = String.valueOf(p2) + "(view)"; 
      permissions = p2;
      if (get_em.hasMoreTokens())
        get_em.nextToken(); 
      if (get_em.hasMoreTokens())
        get_em.nextToken(); 
      if (get_em.hasMoreTokens())
        get_em.nextToken(); 
      String max_logins = "0";
      if (get_em.hasMoreTokens())
        max_logins = get_em.nextToken().substring(1); 
      String transfer_speed = "0";
      if (get_em.hasMoreTokens())
        transfer_speed = get_em.nextToken().substring(1); 
      if (get_em.hasMoreTokens())
        get_em.nextToken(); 
      Properties user_prop = (Properties)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><userfile type=\"properties\">\t<password>Acf9C+U0B0UvQiwbMd9Km+uEHaQO/nLf</password>\t<root_dir>/</root_dir>\t<version>1.0</version>\t<max_logins>0</max_logins></userfile>".getBytes("UTF8"))).getRootElement());
      Vector user_vfs = (Vector)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"vector\">\t<VFS_subitem type=\"properties\">\t\t<url>FILE://</url>\t\t<type>dir</type>\t</VFS_subitem></VFS>".getBytes("UTF8"))).getRootElement());
      Properties user_vfs_item = (Properties)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"properties\">\t<item name=\"/\">(read)(view)(resume)</item></VFS>".getBytes("UTF8"))).getRootElement());
      ((Properties)user_vfs.elementAt(0)).put("url", (new File_S(root_dir)).toURI().toURL().toExternalForm());
      user_prop.put("password", curPassword);
      user_prop.put("speed_limit_download", transfer_speed);
      user_prop.put("speed_limit_upload", transfer_speed);
      user_prop.put("max_logins_ip", max_logins);
      user_vfs_item.put("/" + (new File_S(root_dir)).getName() + "/", permissions);
      (new File_S(String.valueOf(pathOut) + curUser)).mkdirs();
      updateOSXInfo(String.valueOf(pathOut) + curUser);
      (new File_S(String.valueOf(pathOut) + curUser + "/VFS/")).mkdirs();
      updateOSXInfo(String.valueOf(pathOut) + curUser + "/VFS/");
      writeXMLObject(String.valueOf(pathOut) + curUser + "/user.XML", user_prop, "userfile");
      writeXMLObject(String.valueOf(pathOut) + curUser + "/VFS/" + (new File_S(root_dir)).getName(), user_vfs, "VFS");
      writeXMLObject(String.valueOf(pathOut) + curUser + "/VFS.XML", user_vfs_item, "VFS");
    } 
  }
  
  public void ConvertServUUsers(String pathIn, String user_path) throws Exception {
    String pathOut = String.valueOf(System.getProperties().getProperty("crushftp.users")) + user_path;
    String curUser = "";
    String permissions = "";
    String homeDir = "";
    File_S curFile = new File_S(pathIn);
    BufferedReader in = new BufferedReader(new FileReader(new File_S(curFile)));
    boolean fisrtRun = true;
    Properties user_prop = (Properties)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><userfile type=\"properties\">\t<password>Acf9C+U0B0UvQiwbMd9Km+uEHaQO/nLf</password>\t<root_dir>/</root_dir>\t<version>1.0</version>\t<max_logins>0</max_logins></userfile>".getBytes("UTF8"))).getRootElement());
    Vector user_vfs = (Vector)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"vector\">\t<VFS_subitem type=\"properties\">\t\t<url>FILE://</url>\t\t<type>dir</type>\t</VFS_subitem></VFS>".getBytes("UTF8"))).getRootElement());
    Properties user_vfs_item = (Properties)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"properties\">\t<item name=\"/\">(read)(view)(resume)</item></VFS>".getBytes("UTF8"))).getRootElement());
    while (in.ready()) {
      String curLine = in.readLine();
      if (curLine.startsWith("[USER=") && !fisrtRun) {
        if (!homeDir.trim().equals("")) {
          ((Properties)user_vfs.elementAt(0)).put("url", (new File_S(homeDir)).toURI().toURL().toExternalForm());
          user_vfs_item.put("/" + (new File_S(homeDir)).getName() + "/", permissions);
        } 
        (new File_S(String.valueOf(pathOut) + curUser)).mkdirs();
        updateOSXInfo(String.valueOf(pathOut) + curUser);
        (new File_S(String.valueOf(pathOut) + curUser + "/VFS/")).mkdirs();
        updateOSXInfo(String.valueOf(pathOut) + curUser + "/VFS/");
        writeXMLObject(String.valueOf(pathOut) + curUser + "/user.XML", user_prop, "userfile");
        if (!homeDir.trim().equals(""))
          writeXMLObject(String.valueOf(pathOut) + curUser + "/VFS/" + (new File_S(homeDir)).getName(), user_vfs, "VFS"); 
        writeXMLObject(String.valueOf(pathOut) + curUser + "/VFS.XML", user_vfs_item, "VFS");
        curUser = "";
        homeDir = "";
        permissions = "";
        user_prop = (Properties)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><userfile type=\"properties\">\t<password>Acf9C+U0B0UvQiwbMd9Km+uEHaQO/nLf</password>\t<root_dir>/</root_dir>\t<version>1.0</version>\t<max_logins>0</max_logins></userfile>".getBytes("UTF8"))).getRootElement());
        user_vfs = (Vector)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"vector\">\t<VFS_subitem type=\"properties\">\t\t<url>FILE://</url>\t\t<type>dir</type>\t</VFS_subitem></VFS>".getBytes("UTF8"))).getRootElement());
        user_vfs_item = (Properties)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"properties\">\t<item name=\"/\">(read)(view)(resume)</item></VFS>".getBytes("UTF8"))).getRootElement());
      } 
      if (curLine.startsWith("[USER=")) {
        fisrtRun = false;
        curUser = curLine.substring(6, curLine.indexOf("]"));
        if (curUser.indexOf("|") >= 0)
          curUser = curUser.substring(0, curUser.indexOf("|")); 
        if (curUser.endsWith("@1"))
          curUser = curUser.substring(0, curUser.length() - 2); 
      } 
      if (curLine.startsWith("HomeDir=")) {
        String finalDirName = "";
        homeDir = curLine.substring(8).trim();
        finalDirName = homeDir.substring(homeDir.lastIndexOf("\\") + 1);
        if (finalDirName.trim().equals("")) {
          finalDirName = "_" + replace_str(homeDir, ":\\", "_").trim();
        } else {
          homeDir = String.valueOf(homeDir) + "/";
        } 
        homeDir = "/" + homeDir;
        homeDir = replace_str(homeDir, ":\\", "^^");
        homeDir = replace_str(homeDir, "\\", "/");
        homeDir = replace_str(homeDir, "^^", ":\\/");
      } 
      if (curLine.startsWith("Note")) {
        String note = curLine.substring(curLine.indexOf("=") + 1);
        if (note.startsWith("\""))
          note = note.substring(1); 
        if (note.endsWith("\""))
          note = note.substring(0, note.length() - 1); 
        user_prop.put("notes", String.valueOf(user_prop.getProperty("notes", "")) + note + "\r\n");
      } 
      if (curLine.startsWith("Access")) {
        String perms = curLine.substring(curLine.indexOf(",") + 1).toUpperCase();
        if (perms.indexOf("R") >= 0)
          permissions = String.valueOf(permissions) + "(read)"; 
        if (perms.indexOf("D") >= 0)
          permissions = String.valueOf(permissions) + "(deletedir)"; 
        if (perms.indexOf("W") >= 0)
          permissions = String.valueOf(permissions) + "(write)(rename)"; 
        if (perms.indexOf("M") >= 0)
          permissions = String.valueOf(permissions) + "(delete)"; 
        if (perms.indexOf("C") >= 0)
          permissions = String.valueOf(permissions) + "(makedir)"; 
        if (perms.indexOf("L") >= 0)
          permissions = String.valueOf(permissions) + "(view)(resume)"; 
      } 
    } 
    in.close();
  }
  
  public void ConvertGene6Users(String pathIn, String user_path) throws Exception {
    String pathOut = String.valueOf(System.getProperties().getProperty("crushftp.users")) + user_path;
    File_S curFile = new File_S(pathIn);
    if (!curFile.isDirectory())
      curFile = (File_S)curFile.getParentFile(); 
    Log.log("SERVER", 0, "Using user folder:" + curFile + " for Gene6 import");
    File_S[] items = (File_S[])curFile.listFiles();
    for (int loop = 0; loop < items.length; loop++) {
      if (!items[loop].isDirectory() && items[loop].getName().toUpperCase().endsWith(".INI")) {
        BufferedReader in = new BufferedReader(new FileReader(items[loop]));
        try {
          Properties user_prop = (Properties)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><userfile type=\"properties\">\t<password>Acf9C+U0B0UvQiwbMd9Km+uEHaQO/nLf</password>\t<root_dir>/</root_dir>\t<version>1.0</version>\t<max_logins>0</max_logins></userfile>".getBytes("UTF8"))).getRootElement());
          Vector user_vfs = (Vector)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"vector\">\t<VFS_subitem type=\"properties\">\t\t<url>FILE://</url>\t\t<type>dir</type>\t</VFS_subitem></VFS>".getBytes("UTF8"))).getRootElement());
          Properties user_vfs_item = (Properties)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"properties\">\t<item name=\"/\">(read)(view)(resume)</item></VFS>".getBytes("UTF8"))).getRootElement());
          String curUser = items[loop].getName().substring(0, items[loop].getName().lastIndexOf("."));
          String permissions = "";
          String homeDir = "";
          while (in.ready()) {
            String curLine = in.readLine();
            if (curLine == null)
              break; 
            if (curLine.startsWith("AccessList")) {
              curLine = curLine.substring(curLine.indexOf(",") + 1);
              homeDir = curLine.substring(0, curLine.lastIndexOf(","));
              homeDir = homeDir.substring(0, homeDir.lastIndexOf(","));
              String[] perms = curLine.substring(homeDir.length() + 1).toUpperCase().split(",");
              if (perms[0].indexOf("R") >= 0)
                permissions = String.valueOf(permissions) + "(read)"; 
              if (perms[0].indexOf("A") >= 0)
                permissions = String.valueOf(permissions) + "(resume)"; 
              if (perms[1].indexOf("R") >= 0)
                permissions = String.valueOf(permissions) + "(deletedir)"; 
              if (perms[0].indexOf("W") >= 0)
                permissions = String.valueOf(permissions) + "(write)(rename)"; 
              if (perms[0].indexOf("D") >= 0)
                permissions = String.valueOf(permissions) + "(delete)"; 
              if (perms[1].indexOf("M") >= 0)
                permissions = String.valueOf(permissions) + "(makedir)"; 
              if (perms[1].indexOf("F") >= 0 || perms[1].indexOf("D") >= 0)
                permissions = String.valueOf(permissions) + "(view)"; 
            } 
            if (curLine.startsWith("Notes")) {
              String note = curLine.substring(curLine.indexOf("=") + 1);
              if (note.startsWith("\""))
                note = note.substring(1); 
              if (note.endsWith("\""))
                note = note.substring(0, note.length() - 1); 
              user_prop.put("notes", String.valueOf(user_prop.getProperty("notes", "")) + note + "\r\n");
            } 
            if (curLine.startsWith("Email")) {
              String email = curLine.substring(curLine.indexOf("=") + 1);
              if (email.startsWith("\""))
                email = email.substring(1); 
              if (email.endsWith("\""))
                email = email.substring(0, email.length() - 1); 
              user_prop.put("email", email);
            } 
            if (curLine.startsWith("Password=")) {
              String pass = curLine.substring(curLine.indexOf("=") + 1);
              if (pass.startsWith("\""))
                pass = pass.substring(1); 
              if (pass.endsWith("\""))
                pass = pass.substring(0, pass.length() - 1); 
              if (pass.length() > 10) {
                user_prop.put("password", "MD5:" + pass.substring(4).toLowerCase());
                continue;
              } 
              user_prop.put("password", "");
            } 
          } 
          if (!homeDir.trim().equals("")) {
            ((Properties)user_vfs.elementAt(0)).put("url", (new File_S(homeDir)).toURI().toURL().toExternalForm());
            user_vfs_item.put("/" + (new File_S(homeDir)).getName() + "/", permissions);
          } 
          (new File_S(String.valueOf(pathOut) + curUser)).mkdirs();
          updateOSXInfo(String.valueOf(pathOut) + curUser);
          (new File_S(String.valueOf(pathOut) + curUser + "/VFS/")).mkdirs();
          updateOSXInfo(String.valueOf(pathOut) + curUser + "/VFS/");
          writeXMLObject(String.valueOf(pathOut) + curUser + "/user.XML", user_prop, "userfile");
          if (!homeDir.trim().equals(""))
            writeXMLObject(String.valueOf(pathOut) + curUser + "/VFS/" + (new File_S(homeDir)).getName(), user_vfs, "VFS"); 
          writeXMLObject(String.valueOf(pathOut) + curUser + "/VFS.XML", user_vfs_item, "VFS");
          Log.log("SERVER", 0, "Wrote user:" + curUser);
        } finally {
          in.close();
        } 
      } 
    } 
  }
  
  public void ConvertBPFTPsers(String pathIn, String user_path) throws Exception {
    String pathOut = String.valueOf(System.getProperties().getProperty("crushftp.users")) + user_path;
    String permissions = "";
    String homeDir = "";
    File_S curFile = new File_S(pathIn);
    BufferedReader in = new BufferedReader(new FileReader(curFile));
    Properties user_prop = (Properties)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><userfile type=\"properties\">\t<password>Acf9C+U0B0UvQiwbMd9Km+uEHaQO/nLf</password>\t<root_dir>/</root_dir>\t<version>1.0</version>\t<max_logins>0</max_logins></userfile>".getBytes("UTF8"))).getRootElement());
    Vector user_vfs = (Vector)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"vector\">\t<VFS_subitem type=\"properties\">\t\t<url>FILE://</url>\t\t<type>dir</type>\t</VFS_subitem></VFS>".getBytes("UTF8"))).getRootElement());
    Properties user_vfs_item = (Properties)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"properties\">\t<item name=\"/\">(read)(view)(resume)</item></VFS>".getBytes("UTF8"))).getRootElement());
    Properties user = new Properties();
    while (in.ready()) {
      String curLine = in.readLine();
      if (curLine.trim().equals("") && user.size() > 0) {
        if (!homeDir.trim().equals("")) {
          ((Properties)user_vfs.elementAt(0)).put("url", (new File_S(homeDir)).toURI().toURL().toExternalForm());
          user_vfs_item.put("/" + (new File_S(homeDir)).getName() + "/", permissions);
        } 
        user_prop.putAll(user);
        String curUser = user.getProperty("user_name");
        (new File_S(String.valueOf(pathOut) + curUser)).mkdirs();
        updateOSXInfo(String.valueOf(pathOut) + curUser);
        (new File_S(String.valueOf(pathOut) + curUser + "/VFS/")).mkdirs();
        updateOSXInfo(String.valueOf(pathOut) + curUser + "/VFS/");
        writeXMLObject(String.valueOf(pathOut) + curUser + "/user.XML", user_prop, "userfile");
        if (!homeDir.trim().equals(""))
          writeXMLObject(String.valueOf(pathOut) + curUser + "/VFS/" + (new File_S(homeDir)).getName(), user_vfs, "VFS"); 
        writeXMLObject(String.valueOf(pathOut) + curUser + "/VFS.XML", user_vfs_item, "VFS");
        homeDir = "";
        permissions = "";
        user = new Properties();
        user_prop = (Properties)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><userfile type=\"properties\">\t<password>Acf9C+U0B0UvQiwbMd9Km+uEHaQO/nLf</password>\t<root_dir>/</root_dir>\t<version>1.0</version>\t<max_logins>0</max_logins></userfile>".getBytes("UTF8"))).getRootElement());
        user_vfs = (Vector)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"vector\">\t<VFS_subitem type=\"properties\">\t\t<url>FILE://</url>\t\t<type>dir</type>\t</VFS_subitem></VFS>".getBytes("UTF8"))).getRootElement());
        user_vfs_item = (Properties)getElements(getSaxBuilder().build(new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VFS type=\"properties\">\t<item name=\"/\">(read)(view)(resume)</item></VFS>".getBytes("UTF8"))).getRootElement());
      } 
      if (curLine.indexOf("=") > 0) {
        String key = curLine.split("=")[0];
        String val = "";
        if ((curLine.split("=")).length > 1)
          val = curLine.split("=")[1]; 
        if (key.equalsIgnoreCase("PASS")) {
          user.put("password", encode_pass(val, ServerStatus.SG("password_encryption"), ""));
        } else if (key.equalsIgnoreCase("LOGIN")) {
          user.put("user_name", val);
        } else if (key.equalsIgnoreCase("MaxUsers")) {
          user.put("max_logins", val);
        } else if (key.equalsIgnoreCase("MaxSpeedRcv")) {
          user.put("speed_limit_upload", val);
        } else if (key.equalsIgnoreCase("MaxSpeedSnd")) {
          user.put("speed_limit_download", val);
        } else if (key.equalsIgnoreCase("TimeOut")) {
          user.put("max_idle_time", (new StringBuffer(String.valueOf(Integer.parseInt(val) / 60))).toString());
        } else if (key.equalsIgnoreCase("Dir0")) {
          homeDir = "/" + val.replace('\\', '/');
        } else if (key.equalsIgnoreCase("Attr0")) {
          String perms = curLine.substring(curLine.indexOf(",") + 1).toUpperCase();
          if (perms.indexOf("R") >= 0)
            permissions = String.valueOf(permissions) + "(read)"; 
          if (perms.indexOf("W") >= 0)
            permissions = String.valueOf(permissions) + "(write)(rename)"; 
          if (perms.indexOf("D") >= 0)
            permissions = String.valueOf(permissions) + "(delete)"; 
          if (perms.indexOf("A") >= 0)
            permissions = String.valueOf(permissions) + "(resume)"; 
          if (perms.indexOf("M") >= 0)
            permissions = String.valueOf(permissions) + "(makedir)"; 
          if (perms.indexOf("L") >= 0)
            permissions = String.valueOf(permissions) + "(view)"; 
          if (perms.indexOf("K") >= 0)
            permissions = String.valueOf(permissions) + "(deletedir)"; 
        } 
        user_prop.put("notes", String.valueOf(user_prop.getProperty("notes", "")) + curLine + "\r\n");
      } 
    } 
    in.close();
  }
  
  public String migrateUsersVFS(String the_server, String from, String to) {
    from = from.replace('\\', '/');
    to = to.replace('\\', '/');
    if (!from.startsWith("/"))
      from = "/" + from; 
    if (!to.startsWith("/"))
      to = "/" + to; 
    String results = "";
    Vector v = new Vector();
    UserTools.refreshUserList(the_server, v);
    String lastItem = "";
    for (int x = 0; x < v.size(); x++) {
      String username = v.elementAt(x).toString();
      String userpath = UserTools.get_real_path_to_user(the_server, username);
      Vector vv = new Vector();
      try {
        int fixedEntries = 0;
        getAllFileListing(vv, String.valueOf(userpath) + "VFS", 99, false);
        for (int xx = 0; xx < vv.size(); xx++) {
          File_S f = vv.elementAt(xx);
          lastItem = f.toString();
          if (f.isFile()) {
            int replaced = 0;
            Vector proplist = (Vector)readXMLObject(f.getCanonicalPath());
            if (proplist != null)
              for (int xxx = 0; xxx < proplist.size(); xxx++) {
                Properties p = proplist.elementAt(xxx);
                String url = p.getProperty("url", "");
                if (url.toUpperCase().startsWith("FILE:/" + from.toUpperCase())) {
                  url = "FILE:/" + to + url.substring(("FILE:/" + from).length());
                  p.put("url", url);
                  replaced++;
                  fixedEntries++;
                } 
              }  
            if (replaced > 0)
              writeXMLObject(f.getCanonicalPath(), proplist, "VFS"); 
          } 
        } 
        if (fixedEntries > 0)
          results = String.valueOf(results) + LOC.G("Fixed $0 entries in user $1.", (new StringBuffer(String.valueOf(fixedEntries))).toString(), username) + "\r\n"; 
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
        Log.log("SERVER", 0, lastItem);
      } 
    } 
    results = String.valueOf(results) + LOC.G("Finished!");
    return results;
  }
  
  public String setServerStatus(Properties server_item, String the_ip) {
    String statusMessage = "";
    if (server_item.getProperty("serverType", "FTP").toUpperCase().equals("FTPS")) {
      statusMessage = "(Implicit SSL)";
    } else if (server_item.getProperty("serverType", "FTP").toUpperCase().equals("FTP")) {
      statusMessage = "(" + (server_item.getProperty("explicit_ssl", "false").toUpperCase().equals("TRUE") ? " SSL" : "") + (server_item.getProperty("explicit_tls", "false").toUpperCase().equals("TRUE") ? " TLS" : "") + " )";
    } else if (server_item.getProperty("serverType", "FTP").toUpperCase().equals("SFTP")) {
      statusMessage = "( SSH )";
    } else if (server_item.getProperty("serverType", "FTP").toUpperCase().equals("HTTPS")) {
      if (server_item.getProperty("allow_webdav", "true").equalsIgnoreCase("true")) {
        statusMessage = "( Web, WebDAV SSL )";
      } else {
        statusMessage = "( Web, SSL )";
      } 
    } else if (server_item.getProperty("serverType", "FTP").toUpperCase().equals("HTTP")) {
      if (server_item.getProperty("allow_webdav", "true").equalsIgnoreCase("true")) {
        statusMessage = "( Web, WebDAV)";
      } else {
        statusMessage = "( Web )";
      } 
    } 
    String port = ":" + server_item.getProperty("port", "21");
    if (server_item.getProperty("serverType", "FTP").toLowerCase().equals("ftp") && port.equals(":21"))
      port = ""; 
    if (server_item.getProperty("serverType", "FTP").toLowerCase().equals("http") && port.equals(":80"))
      port = ""; 
    if (server_item.getProperty("serverType", "FTP").toLowerCase().equals("https") && port.equals(":443"))
      port = ""; 
    if (server_item.getProperty("serverType", "FTP").toLowerCase().equals("sftp") && port.equals(":22"))
      port = ""; 
    statusMessage = String.valueOf(server_item.getProperty("server_item_name", " ")) + server_item.getProperty("serverType", "FTP").toLowerCase() + "://" + the_ip + port + "/ " + (statusMessage.equals("( )") ? "" : statusMessage);
    return statusMessage;
  }
  
  public void sortPlugins(Vector plugins) {
    if (plugins == null)
      plugins = new Vector(); 
    String[] pluginNames = new String[plugins.size()];
    for (int x = 0; x < plugins.size(); x++) {
      Vector pluginPrefs = null;
      if (plugins.elementAt(x) instanceof Vector) {
        pluginPrefs = plugins.elementAt(x);
      } else {
        pluginPrefs = new Vector();
        pluginPrefs.addElement(plugins.elementAt(x));
      } 
      pluginNames[x] = ((Properties)pluginPrefs.elementAt(0)).getProperty("pluginName");
    } 
    Arrays.sort((Object[])pluginNames);
    Vector pluginNamesVec = new Vector();
    for (int i = 0; i < pluginNames.length; i++)
      pluginNamesVec.addElement(pluginNames[i]); 
    Vector plugins2 = (Vector)plugins.clone();
    for (int j = 0; j < plugins2.size(); j++) {
      Vector pluginPrefs = null;
      if (plugins2.elementAt(j) instanceof Vector) {
        pluginPrefs = plugins2.elementAt(j);
      } else {
        pluginPrefs = new Vector();
        pluginPrefs.addElement(plugins2.elementAt(j));
      } 
      Properties pluginPref = pluginPrefs.elementAt(0);
      String pluginName = pluginPref.getProperty("pluginName");
      plugins.setElementAt(pluginPrefs, pluginNamesVec.indexOf(pluginName));
    } 
  }
  
  public void loadPlugins(Properties server_settings, Properties server_info) {
    Vector plugins = (Vector)server_settings.get("plugins");
    if (plugins == null)
      plugins = new Vector(); 
    sortPlugins(plugins);
    server_settings.put("plugins", plugins);
    Vector si_plugins = (Vector)server_info.get("plugins");
    if (si_plugins == null)
      si_plugins = new Vector(); 
    server_info.put("plugins", si_plugins);
    si_plugins.removeAllElements();
    if ((new File_S(String.valueOf(System.getProperty("crushftp.plugins")) + "plugins/")).exists()) {
      String[] list = (new File_S(String.valueOf(System.getProperty("crushftp.plugins")) + "plugins/")).list();
      if (list != null)
        for (int i = 0; i < list.length; i++) {
          File_S test = new File_S(String.valueOf(System.getProperty("crushftp.plugins")) + "plugins/" + list[i]);
          try {
            if (test.getName().toUpperCase().endsWith(".JAR")) {
              String pluginName = test.getName().substring(0, test.getName().length() - 4);
              boolean foundIt = false;
              for (int xx = 0; xx < plugins.size(); xx++) {
                if (plugins.elementAt(xx) instanceof Vector) {
                  Vector v = plugins.elementAt(xx);
                  Properties p = v.elementAt(0);
                  if (p.getProperty("pluginName").equals(pluginName))
                    foundIt = true; 
                } else {
                  Properties p = (Properties)plugins.elementAt(xx);
                  if (p.getProperty("pluginName").equals(pluginName))
                    foundIt = true; 
                } 
              } 
              if (!foundIt) {
                Vector v = new Vector();
                Properties p = new Properties();
                p.put("pluginName", pluginName);
                v.addElement(p);
                plugins.addElement(v);
              } 
            } 
          } catch (Exception e) {
            Log.log("SERVER", 1, e);
          } 
        }  
    } 
    for (int x = 0; x < plugins.size(); x++) {
      try {
        Vector pluginPrefs = null;
        if (plugins.elementAt(x) instanceof Vector) {
          pluginPrefs = plugins.elementAt(x);
        } else {
          pluginPrefs = new Vector();
          pluginPrefs.addElement(plugins.elementAt(x));
        } 
        Vector siPluginPrefs = new Vector();
        for (int xx = 0; xx < pluginPrefs.size(); xx++) {
          Properties pluginPref = pluginPrefs.elementAt(xx);
          String pluginName = pluginPref.getProperty("pluginName");
          getPlugin(pluginName, (new File_S(String.valueOf(System.getProperty("crushftp.plugins")) + "plugins/")).toURI().toURL().toExternalForm(), pluginPref.getProperty("subItem", ""));
          setPluginSettings(getPlugin(pluginName, null, pluginPref.getProperty("subItem", "")), getPluginPrefs(pluginName, pluginPref));
          Properties pp = (Properties)pluginPref.clone();
          pp.put("plugin", getPlugin(pluginName, null, pluginPref.getProperty("subItem", "")));
          pp.put("pluginName", pluginName);
          pp.put("subItem", pluginPref.getProperty("subItem", ""));
          siPluginPrefs.addElement(pp);
        } 
        if (siPluginPrefs != null)
          si_plugins.addElement(siPluginPrefs); 
      } catch (Exception e) {
        Log.log("SERVER", 1, e);
      } 
    } 
  }
  
  public void loadPluginsSync(Properties server_settings, Properties server_info) {
    Vector plugins = (Vector)server_settings.get("plugins");
    if (plugins == null)
      plugins = new Vector(); 
    server_settings.put("plugins", plugins);
    Vector si_plugins = (Vector)server_info.get("plugins");
    if (si_plugins == null)
      si_plugins = new Vector(); 
    server_info.put("plugins", si_plugins);
    for (int x = 0; x < plugins.size(); x++) {
      try {
        Vector pluginPrefs = null;
        if (plugins.elementAt(x) instanceof Vector) {
          pluginPrefs = plugins.elementAt(x);
        } else {
          pluginPrefs = new Vector();
          pluginPrefs.addElement(plugins.elementAt(x));
        } 
        Vector siPluginPrefs = si_plugins.elementAt(x);
        for (int xx = 0; xx < pluginPrefs.size(); xx++) {
          Properties pluginPref = pluginPrefs.elementAt(xx);
          if (siPluginPrefs.size() <= xx) {
            String pluginName = pluginPref.getProperty("pluginName");
            getPlugin(pluginName, (new File_S(String.valueOf(System.getProperty("crushftp.plugins")) + "plugins/")).toURI().toURL().toExternalForm(), pluginPref.getProperty("subItem", ""));
            setPluginSettings(getPlugin(pluginName, null, pluginPref.getProperty("subItem", "")), getPluginPrefs(pluginName, pluginPref));
            Properties pp = (Properties)pluginPref.clone();
            pp.put("plugin", getPlugin(pluginName, null, pluginPref.getProperty("subItem", "")));
            pp.put("pluginName", pluginName);
            pp.put("subItem", pluginPref.getProperty("subItem", ""));
            siPluginPrefs.addElement(pp);
          } else {
            Properties siPluginPref = siPluginPrefs.elementAt(xx);
            if (!(String.valueOf(pluginPref.getProperty("pluginName")) + "_" + pluginPref.getProperty("subItem", "")).equals(String.valueOf(siPluginPref.getProperty("pluginName")) + "_" + siPluginPref.getProperty("subItem", "")))
              pluginCache.put(String.valueOf(pluginPref.getProperty("pluginName")) + "_" + pluginPref.getProperty("subItem", ""), pluginCache.remove(String.valueOf(siPluginPref.getProperty("pluginName")) + "_" + siPluginPref.getProperty("subItem", ""))); 
            siPluginPref.put("subItem", pluginPref.getProperty("subItem", ""));
            setPluginSettings(siPluginPref.get("plugin"), pluginPref);
          } 
        } 
      } catch (Exception e) {
        Log.log("SERVER", 1, e);
      } 
    } 
  }
  
  public void loadURLPlugins(Properties server_settings, Properties server_info, String url) {
    Vector plugins = (Vector)server_settings.get("plugins");
    if (plugins == null)
      plugins = new Vector(); 
    server_settings.put("plugins", plugins);
    Vector si_plugins = (Vector)server_info.get("plugins");
    if (si_plugins == null)
      si_plugins = new Vector(); 
    server_info.put("plugins", si_plugins);
    si_plugins.removeAllElements();
    for (int x = 0; x < plugins.size(); x++) {
      Vector pluginPrefs = null;
      if (plugins.elementAt(x) instanceof Vector) {
        pluginPrefs = plugins.elementAt(x);
      } else {
        pluginPrefs = new Vector();
        pluginPrefs.addElement(plugins.elementAt(x));
      } 
      try {
        for (int xx = 0; xx < pluginPrefs.size(); xx++) {
          Properties pluginPref = pluginPrefs.elementAt(xx);
          String pluginName = pluginPref.getProperty("pluginName");
          setPluginSettings(getPlugin(pluginName, url, pluginPref.getProperty("subItem", "")), getPluginPrefs(pluginName, pluginPref));
          Properties p = (Properties)pluginPref.clone();
          p.put("plugin", getPlugin(pluginName, url, pluginPref.getProperty("subItem", "")));
          p.put("pluginName", pluginName);
          si_plugins.addElement(p);
        } 
      } catch (Exception exception) {}
    } 
  }
  
  public static Object getPlugin(String pluginName, String u, String subItem) throws Exception {
    Object o = pluginCache.get(String.valueOf(pluginName) + "_" + subItem);
    if (o != null)
      return o; 
    if (u == null)
      return null; 
    if (!u.endsWith("/"))
      u = String.valueOf(u) + "/"; 
    URL url = new URL(String.valueOf(u) + pluginName + ".jar");
    Class c = null;
    if (System.getProperty("crushftp.jarproxy", "false").equals("true")) {
      c = Class.forName(String.valueOf(pluginName) + ".Start");
    } else {
      c = (new URLClassLoader(new URL[] { url }, ServerStatus.clasLoader)).loadClass(String.valueOf(pluginName) + ".Start");
    } 
    Constructor cons = c.getConstructor(null);
    o = cons.newInstance(null);
    pluginCache.put(String.valueOf(pluginName) + "_" + subItem, o);
    return o;
  }
  
  public void setPluginSettings(Object o, Properties p) throws Exception {
    Method setSettings = o.getClass().getMethod("setSettings", new Class[] { (new Properties()).getClass() });
    setSettings.invoke(o, new Object[] { p });
  }
  
  public Properties getPluginSettings(Object o) throws Exception {
    Method getSettings = o.getClass().getMethod("getSettings", null);
    Properties p = (Properties)getSettings.invoke(o, null);
    String pluginName = o.getClass().getName();
    p.put("pluginName", pluginName.substring(0, pluginName.indexOf(".")));
    return p;
  }
  
  public Properties getPluginPrefs(String pluginName, Properties pluginPrefs) throws Exception {
    Properties defaultPrefs = getPluginDefaultPrefs(pluginName, pluginPrefs.getProperty("subItem", ""));
    defaultPrefs.put("subItem", "");
    if (pluginPrefs == null)
      pluginPrefs = defaultPrefs; 
    Enumeration e = defaultPrefs.keys();
    while (e.hasMoreElements()) {
      String key = e.nextElement().toString();
      if (!pluginPrefs.containsKey(key))
        pluginPrefs.put(key, defaultPrefs.get(key)); 
    } 
    e = pluginPrefs.keys();
    while (e.hasMoreElements()) {
      String key = e.nextElement().toString();
      if (!defaultPrefs.containsKey(key))
        pluginPrefs.remove(key); 
    } 
    return pluginPrefs;
  }
  
  public Properties getPluginDefaultPrefs(String pluginName, String subItem) throws Exception {
    Object o = getPlugin(pluginName, null, subItem);
    Method getDefaults = o.getClass().getMethod("getDefaults", null);
    Properties p = (Properties)getDefaults.invoke(o, null);
    p.put("pluginName", pluginName);
    return p;
  }
  
  public static Properties runPlugin(String pluginName, Properties args, String subItem) throws Exception {
    Object o = getPlugin(pluginName, null, subItem);
    if (o != null) {
      Method run = o.getClass().getMethod("run", new Class[] { (new Properties()).getClass() });
      return (Properties)run.invoke(o, new Object[] { args });
    } 
    return null;
  }
  
  public static void runOtherPlugins(Properties info, boolean debug, Properties settings) {
    if (settings.getProperty("subItem").indexOf("~") >= 0) {
      Vector plugins = (Vector)ServerStatus.server_settings.get("plugins");
      if (plugins != null)
        for (int x = 0; x < plugins.size(); x++) {
          Vector pluginPrefs = null;
          if (plugins.elementAt(x) instanceof Vector) {
            pluginPrefs = plugins.elementAt(x);
          } else {
            pluginPrefs = new Vector();
            pluginPrefs.addElement(plugins.elementAt(x));
          } 
          for (int xx = 0; xx < pluginPrefs.size(); xx++) {
            if (!(pluginPrefs.elementAt(xx) instanceof String)) {
              Properties pluginPref = pluginPrefs.elementAt(xx);
              String subitem = "";
              if ((settings.getProperty("subItem").split("~")).length > 1)
                subitem = settings.getProperty("subItem").split("~")[1]; 
              if (pluginPref.getProperty("pluginName").equals(settings.getProperty("subItem").split("~")[0]) && pluginPref.getProperty("subItem").equals(subitem)) {
                if (debug)
                  Log.log("PLUGIN", 2, String.valueOf(pluginPref.getProperty("pluginName")) + " : " + pluginPref.getProperty("subItem", "")); 
                try {
                  info.put("ran_other_plugin", "true");
                  info.put("override_enabled", "true");
                  runPlugin(pluginPref.getProperty("pluginName"), info, pluginPref.getProperty("subItem", ""));
                  info.put("override_enabled", "false");
                } catch (Exception e) {
                  Log.log("PLUGIN", 1, e);
                } 
              } 
            } 
          } 
        }  
    } 
  }
  
  public Vector getPluginList() {
    Vector v = new Vector();
    if ((new File_S(String.valueOf(System.getProperty("crushftp.plugins")) + "plugins/")).exists()) {
      String[] list = (new File_S(String.valueOf(System.getProperty("crushftp.plugins")) + "plugins/")).list();
      for (int x = 0; x < list.length; x++) {
        File_S test = new File_S(String.valueOf(System.getProperty("crushftp.plugins")) + "plugins/" + list[x]);
        if (test.getName().toUpperCase().endsWith(".JAR"))
          v.addElement(test.getName().substring(0, test.getName().indexOf("."))); 
      } 
    } 
    return v;
  }
  
  public static void OSXPermissionsGrant() throws Exception {
    RandomAccessFile out = new RandomAccessFile(new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "crushftp_suid_root.sh"), "rw");
    out.setLength(0L);
    out.write("#! /bin/bash\n".getBytes("UTF8"));
    out.write(("/bin/chmod u+s \"" + (new File_S(String.valueOf(System.getProperty("crushftp.home")) + System.getProperty("crushftp.executable"))).getCanonicalPath() + "\"\n").getBytes("UTF8"));
    out.write(("/usr/sbin/chown root \"" + (new File_S(String.valueOf(System.getProperty("crushftp.home")) + System.getProperty("crushftp.executable"))).getCanonicalPath() + "\"\n").getBytes("UTF8"));
    out.close();
    File_S f = new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "crushftp_suid_root.sh");
    exec(new String[] { "chmod", "+x", f.getCanonicalPath() });
    exec(new String[] { "osascript", "-e", "do shell script \"" + f.getCanonicalPath() + "\" with administrator privileges" });
    f.delete();
  }
  
  public static Object readXMLObject(URL url) {
    Object result = null;
    try {
      Document doc = getSaxBuilder().build(url);
      result = getElements(doc.getRootElement());
    } catch (Exception e) {
      Log.log("SERVER", 0, e);
    } 
    return result;
  }
  
  public static Object readXMLObject(InputStream in) throws Exception {
    in = com.crushftp.client.Common.sanitizeXML(in);
    Object result = null;
    try {
      Document doc = getSaxBuilder().build(in);
      result = getElements(doc.getRootElement());
    } finally {
      try {
        in.close();
      } catch (IOException e) {
        Log.log("SERVER", 1, e);
      } 
    } 
    return result;
  }
  
  public static SAXBuilder getSaxBuilder() {
    SAXBuilder sb = new SAXBuilder();
    sb.setExpandEntities(false);
    sb.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    return sb;
  }
  
  public static Object readXMLObjectError(InputStream in) throws Exception {
    in = com.crushftp.client.Common.sanitizeXML(in);
    Object result = null;
    Document doc = getSaxBuilder().build(in);
    result = getElements(doc.getRootElement());
    in.close();
    return result;
  }
  
  static Properties recent_corrupt_users = new Properties();
  
  public static Object readXMLObject_U(File file) {
    return readXMLObject_restricted(new File_B(new File_U(file)));
  }
  
  public static Object readXMLObject(File file) {
    return readXMLObject_restricted(new File_B(new File_S(file)));
  }
  
  public static Object readXMLObject_restricted(File_B file) {
    Object result = null;
    InputStream in = null;
    Exception ee = null;
    if (System.currentTimeMillis() - Long.parseLong(recent_corrupt_users.getProperty((String)file, "0")) < 30000L) {
      Log.log("SERVER", 0, "CORRUPT user.XML file:" + file);
      return null;
    } 
    for (int x = 0; x < 3; x++) {
      try {
        in = new FileInputStream(file);
        result = readXMLObject(in);
        ee = null;
        recent_corrupt_users.remove(file);
        break;
      } catch (Exception e) {
        ee = e;
        recent_corrupt_users.put(file, (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      } finally {
        try {
          if (in != null)
            in.close(); 
        } catch (Exception exception) {}
      } 
      try {
        Thread.sleep(1000L);
      } catch (InterruptedException interruptedException) {}
    } 
    if (ee != null) {
      Log.log("SERVER", 0, (String)file);
      Log.log("SERVER", 0, ee);
    } 
    return result;
  }
  
  public String readXMLDocumentAndConvert(URL url, String xslt) {
    Log.log("SERVER", 2, xslt);
    try {
      xslt = (new File_S(String.valueOf(System.getProperty("crushftp.web")) + xslt)).getAbsolutePath();
      Document doc = getSaxBuilder().build(url);
      XMLOutputter xx = new XMLOutputter();
      Format formatter = Format.getPrettyFormat();
      formatter.setExpandEmptyElements(true);
      formatter.setIndent("\t");
      xx.setFormat(formatter);
      String s = "";
      InputStream in = null;
      try {
        if (xslt != null && xslt.length() > 0) {
          in = new FileInputStream(new File_S(xslt));
          XSLTransformer transformer = new XSLTransformer(in);
          doc = transformer.transform(doc);
        } 
        s = xx.outputString(doc);
      } finally {
        if (in != null)
          in.close(); 
      } 
      doc.removeContent();
      doc = null;
      return s;
    } catch (Exception e) {
      Log.log("SERVER", 1, e);
      return null;
    } 
  }
  
  public static Properties xmlCache = new Properties();
  
  static long xmlLastCacheClean = System.currentTimeMillis();
  
  public static Object readXMLObject_U(String path) {
    return readXMLObject(new File_B(new File_U(path)));
  }
  
  public static Object readXMLObject(String path) {
    return readXMLObject(new File_B(new File_S(path)));
  }
  
  public static Object readXMLObject(File_B f) {
    String path = null;
    try {
      path = f.getCanonicalPath();
    } catch (IOException e) {
      Log.log("SERVER", 0, e);
    } 
    synchronized (xmlCache) {
      if (System.currentTimeMillis() - xmlLastCacheClean > 30000L) {
        Enumeration keys = xmlCache.keys();
        while (keys.hasMoreElements()) {
          String key = keys.nextElement().toString();
          Properties p = (Properties)xmlCache.get(key);
          if (System.currentTimeMillis() - Long.parseLong(p.getProperty("time")) > 60000L)
            xmlCache.remove(key); 
        } 
        xmlLastCacheClean = System.currentTimeMillis();
      } 
      if (xmlCache.containsKey(path)) {
        Properties p = (Properties)xmlCache.get(path);
        if (f.exists() && f.lastModified() == Long.parseLong(p.getProperty("modified")))
          return com.crushftp.client.Common.CLONE(p.get("object")); 
        xmlCache.remove(path);
      } 
    } 
    try {
      if (f.exists()) {
        Object o = readXMLObject_restricted(new File_B(path));
        if (o != null) {
          Properties p = new Properties();
          p.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
          p.put("modified", (new StringBuffer(String.valueOf(f.lastModified()))).toString());
          p.put("object", com.crushftp.client.Common.CLONE(o));
          xmlCache.put(path, p);
        } 
        return o;
      } 
    } catch (Exception e) {
      Log.log("SERVER", 0, "ERROR:" + path);
      Log.log("SERVER", 0, e);
    } 
    return null;
  }
  
  public static Object getElements(Element element) {
    Object result = null;
    if (element.getAttributeValue("type", "string").equalsIgnoreCase("properties")) {
      result = new Properties();
      List items2 = element.getChildren();
      if (items2.size() == 0)
        return result; 
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
        return result; 
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
  
  public static String getXMLString(Object obj, String root, String xslt) throws Exception {
    return getXMLString(obj, root, xslt, true);
  }
  
  public static String getXMLString(Object obj, String root, String xslt, boolean pretty) throws Exception {
    if (obj instanceof Properties && !((Properties)obj).getProperty("export", "").equals("true"))
      xslt = null; 
    if (xslt != null && xslt.length() > 0)
      if ((new File_S(String.valueOf(System.getProperty("crushftp.web")) + xslt)).exists()) {
        xslt = (new File_S(String.valueOf(System.getProperty("crushftp.web")) + xslt)).getAbsolutePath();
      } else {
        xslt = null;
      }  
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
    String s = "";
    InputStream in = null;
    try {
      if (xslt != null && xslt.length() > 0) {
        in = new FileInputStream(new File_S(xslt));
        XSLTransformer transformer = new XSLTransformer(in);
        doc = transformer.transform(doc);
      } 
      s = xx.outputString(doc);
    } finally {
      if (in != null)
        in.close(); 
    } 
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
  
  public String transformXML(String xml, String xslt, Properties params) throws Exception {
    Document doc = getSaxBuilder().build((new File_S(xml)).toURI().toURL());
    addParameters(doc, params);
    xslt = (new File_S(xslt)).getAbsolutePath();
    XMLOutputter xx = new XMLOutputter();
    Format formatter = Format.getPrettyFormat();
    formatter.setExpandEmptyElements(true);
    formatter.setIndent("\t");
    xx.setFormat(formatter);
    String s = "";
    if (xslt != null && xslt.length() > 0) {
      XSLTransformer transformer = new XSLTransformer(xslt);
      doc = transformer.transform(doc);
    } 
    s = xx.outputString(doc);
    doc.removeContent();
    doc = null;
    return s;
  }
  
  public static void writeXMLObject(String path, Object obj, String root) throws Exception {
    writeXMLObject(new File_B(new File_S(path)), obj, root);
  }
  
  public static void writeXMLObject_U(String path, Object obj, String root) throws Exception {
    writeXMLObject(new File_B(new File_U(path)), obj, root);
  }
  
  private static void writeXMLObject(File_B f, Object obj, String root) throws Exception {
    String xml = getXMLString(obj, root, null);
    xmlCache.remove(f.getCanonicalPath());
    RandomAccessFile eraser = new RandomAccessFile(f, "rw");
    eraser.setLength(0L);
    eraser.write(xml.getBytes("UTF8"));
    eraser.close();
    updateOSXInfo(f, "");
  }
  
  public static void updateOSXInfo(String path) {
    updateOSXInfo(path, "");
  }
  
  public static void updateOSXInfo(String path, String param) {
    updateOSXInfo(new File_B(new File_S(path)), param);
  }
  
  public static void updateOSXInfo_U(String path, String param) {
    updateOSXInfo(new File_B(new File_U(path)), param);
  }
  
  private static void updateOSXInfo(File_B f, String param) {
    if (machine_is_windows())
      return; 
    try {
      Log.log("SERVER", 3, "Changing default owner/group/priv param=" + param + " owner=" + ServerStatus.SG("default_system_owner") + " group=" + ServerStatus.SG("default_system_group") + " path=" + f.getCanonicalPath());
      if (f.exists() && !machine_is_windows() && (!ServerStatus.SG("default_system_owner").equals("") || !ServerStatus.SG("default_system_group").equals(""))) {
        GenericClient c = getClient(getBaseUrl(f.toURI().toURL().toString()), "", new Vector());
        if (!ServerStatus.SG("default_system_owner").equals(""))
          c.setOwner(f.getCanonicalPath(), ServerStatus.SG("default_system_owner"), param); 
        if (!ServerStatus.SG("default_system_group").equals(""))
          c.setGroup(f.getCanonicalPath(), ServerStatus.SG("default_system_group"), param); 
        c.setMod(f.getCanonicalPath(), "775", param);
      } 
    } catch (Exception e) {
      Log.log("SERVER", 2, e);
    } 
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
    if (url.toUpperCase().startsWith("SMB3:"))
      return new SMB3Client(url, logHeader, logQueue); 
    if (url.toUpperCase().startsWith("AS2:") || url.toUpperCase().indexOf("VFS_AS2") >= 0)
      return new AS2Client(url, logHeader, logQueue); 
    if (url.toUpperCase().startsWith("HADOOP:"))
      return new HadoopClient(url, logHeader, logQueue); 
    if (url.toUpperCase().startsWith("AZURE:"))
      return new AzureClient(url, logHeader, logQueue); 
    if (url.toUpperCase().startsWith("CUSTOM."))
      return new CustomClient(url, logHeader, logQueue); 
    return null;
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
      } else if (obj == null || !(obj instanceof BufferedReader)) {
        if (obj == null || !(obj instanceof java.io.BufferedWriter))
          if (obj == null || !(obj instanceof SessionCrush))
            if (obj != null) {
              Object object = obj;
              try {
                element.setText((String)object);
              } catch (Exception e) {
                element.setText(url_encode((String)object));
              } 
            }   
      } 
    } catch (Exception e) {
      Log.log("SERVER", 1, e);
    } 
  }
  
  public static void deepClone(Object dest, Object src) {
    if (src != null && src instanceof Properties) {
      Properties p = (Properties)src;
      Enumeration e = p.keys();
      while (e.hasMoreElements()) {
        String key = e.nextElement().toString();
        Object val = p.get(key);
        if (val instanceof String) {
          ((Properties)dest).put(key, val);
          continue;
        } 
        if (val instanceof Properties) {
          Properties pp = new Properties();
          ((Properties)dest).put(key, pp);
          deepClone(pp, val);
          continue;
        } 
        if (val instanceof Vector) {
          Vector vv = new Vector();
          ((Properties)dest).put(key, vv);
          deepClone(vv, val);
          continue;
        } 
        ((Properties)dest).put(key, val);
      } 
    } else if (src != null && src instanceof Vector) {
      Vector v = (Vector)src;
      for (int x = 0; x < v.size(); x++) {
        Object val = v.elementAt(x);
        if (val instanceof String) {
          ((Vector)dest).addElement(val);
        } else if (val instanceof Properties) {
          Properties pp = new Properties();
          ((Vector)dest).addElement(pp);
          deepClone(pp, val);
        } else if (val instanceof Vector) {
          Vector vv = new Vector();
          ((Vector)dest).addElement(vv);
          deepClone(vv, val);
        } else {
          ((Vector)dest).addElement(val);
        } 
      } 
    } 
  }
  
  public static String checkPasswordRequirements(String pass, String history, Properties password_rules) {
    if (pass.startsWith("MD5:") || pass.startsWith("MD5S2:") || pass.startsWith("MD4:") || pass.startsWith("SHA:") || pass.startsWith("SHA512:") || pass.startsWith("SHA256:") || pass.startsWith("SHA3:"))
      return ""; 
    String msg = "";
    if (pass.length() < Integer.parseInt(password_rules.getProperty("min_password_length")))
      msg = String.valueOf(msg) + "Password must be at least " + password_rules.getProperty("min_password_length") + " characters.\r\n"; 
    String chars = "0123456789";
    int count = 0;
    int x;
    for (x = 0; x < chars.length(); x++)
      count += count_str(pass, (new StringBuffer(String.valueOf(chars.charAt(x)))).toString()); 
    if (count < Integer.parseInt(password_rules.getProperty("min_password_numbers")))
      msg = String.valueOf(msg) + "Password must have at least " + password_rules.getProperty("min_password_numbers") + " number characters.\r\n"; 
    chars = "abcdefghijklmnopqrstuvwxyzáéúíóäëüïöâêûîôåàèùìòãõñ";
    count = 0;
    for (x = 0; x < chars.length(); x++)
      count += count_str(pass, (new StringBuffer(String.valueOf(chars.charAt(x)))).toString()); 
    if (count < Integer.parseInt(password_rules.getProperty("min_password_lowers")))
      msg = String.valueOf(msg) + "Password must have at least " + password_rules.getProperty("min_password_lowers") + " lower case characters.\r\n"; 
    chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZÁÉÚÍÓÄËÜÏÖÂÊÛÎÔÅÀÈÙÌÒÃÕÑ";
    count = 0;
    for (x = 0; x < chars.length(); x++)
      count += count_str(pass, (new StringBuffer(String.valueOf(chars.charAt(x)))).toString()); 
    if (count < Integer.parseInt(password_rules.getProperty("min_password_uppers")))
      msg = String.valueOf(msg) + "Password must have at least " + password_rules.getProperty("min_password_uppers") + " upper case characters.\r\n"; 
    chars = "!@#$%^&*()_+=-{}][|:;?<>,.";
    count = 0;
    for (x = 0; x < chars.length(); x++)
      count += count_str(pass, (new StringBuffer(String.valueOf(chars.charAt(x)))).toString()); 
    if (count < Integer.parseInt(password_rules.getProperty("min_password_specials")))
      msg = String.valueOf(msg) + "Password must have at least " + password_rules.getProperty("min_password_specials") + " special characters.\r\n"; 
    chars = password_rules.getProperty("unsafe_password_chars");
    count = 0;
    for (x = 0; x < chars.length(); x++)
      count += count_str(pass, (new StringBuffer(String.valueOf(chars.charAt(x)))).toString()); 
    if (count > 0)
      msg = String.valueOf(msg) + "Password cannot contain URL unsafe chars: " + password_rules.getProperty("unsafe_password_chars") + "\r\n"; 
    try {
      String md5 = getMD5(new ByteArrayInputStream(("crushftp" + pass).getBytes("UTF8")));
      if (history.toUpperCase().indexOf(md5.toUpperCase()) >= 0)
        msg = String.valueOf(msg) + "Password cannot be one of your recent passwords.\r\n"; 
    } catch (Exception exception) {}
    return msg;
  }
  
  public static String getPasswordHistory(String pass, String history, Properties password_rules) {
    String newHistory = "";
    try {
      String md5 = getMD5(new ByteArrayInputStream(("crushftp" + pass).getBytes("UTF8")));
      if (history.indexOf(md5) < 0)
        history = String.valueOf(md5) + "," + history; 
      for (int x = 0; x < Integer.parseInt(password_rules.getProperty("password_history_count")) && x < (history.split(",")).length; x++)
        newHistory = String.valueOf(newHistory) + history.split(",")[x] + ","; 
      if (newHistory.length() > 0)
        newHistory = newHistory.substring(0, newHistory.length() - 1); 
    } catch (Exception exception) {}
    return newHistory;
  }
  
  public static String[] pads = new String[] { 
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
  
  private static boolean isSymbolicLink_OFF(String link_name) {
    return isSymbolicLink(new File_B(new File_S(link_name)));
  }
  
  public static boolean isSymbolicLink_U(String link_name) {
    return isSymbolicLink(new File_B(new File_U(link_name)));
  }
  
  private static boolean isSymbolicLink(File_B f) {
    if (machine_is_windows())
      return false; 
    try {
      if (!f.getAbsolutePath().equals(f.getCanonicalPath()))
        return true; 
    } catch (IOException iOException) {}
    return false;
  }
  
  public static void recurseDelete_U(String real_path, boolean test_mode) {
    recurseDelete(new File_B(new File_U(real_path)), test_mode);
  }
  
  public static void recurseDelete(String real_path, boolean test_mode) {
    recurseDelete(new File_B(new File_S(real_path)), test_mode);
  }
  
  private static void recurseDelete(File_B f, boolean test_mode) {
    String real_path = f.getPath();
    try {
      if ((new File_B(real_path)).getCanonicalPath().equals((new File_B(System.getProperty("crushftp.prefs"))).getCanonicalPath()) || (new File_B(real_path)).getCanonicalPath().equals((new File_B(System.getProperty("crushftp.home"))).getCanonicalPath()) || (new File_B(real_path)).getCanonicalPath().equals((new File_S("./")).getCanonicalPath()))
        if ((new File_B(real_path)).getCanonicalPath().indexOf("CrushFTP_temp") < 0) {
          Log.log("SERVER", 0, new Exception("Invalid delete attempted!"));
          return;
        }  
    } catch (Exception e) {
      Log.log("SERVER", 0, e);
      return;
    } 
    if (real_path.trim().equals("/"))
      return; 
    if (real_path.trim().equals("~"))
      return; 
    if (real_path.indexOf(":") >= 0 && real_path.length() < 4)
      return; 
    f = new File_B(real_path);
    try {
      real_path = String.valueOf(f.getCanonicalPath()) + "/";
      f = new File_B(real_path);
    } catch (Exception exception) {}
    if (f.isDirectory()) {
      String[] files = f.list();
      for (int x = 0; x < files.length; x++) {
        File_B f2 = new File_B(String.valueOf(real_path) + files[x]);
        if (!isSymbolicLink(f2)) {
          if (f2.isDirectory())
            recurseDelete(new File_B(String.valueOf(real_path) + files[x] + "/"), test_mode); 
          if (test_mode) {
            Log.log("SERVER", 0, "*****************" + LOC.G("DELETE") + ":" + f2);
          } else {
            f2.delete();
          } 
        } 
      } 
    } 
    if (test_mode) {
      Log.log("SERVER", 0, "*****************" + LOC.G("DELETE") + ":" + f);
    } else {
      f.delete();
    } 
  }
  
  public static void recurseDelete(VRL vrl1, boolean test_mode, GenericClient c1, int depth) throws Exception {
    if (vrl1.getProtocol().equalsIgnoreCase("file") && vrl1.getPath().trim().equals("/"))
      return; 
    if (vrl1.getProtocol().equalsIgnoreCase("file") && vrl1.getPath().trim().equals("~"))
      return; 
    if (vrl1.getProtocol().equalsIgnoreCase("file") && vrl1.getPath().indexOf(":") >= 0 && vrl1.getPath().length() < 4)
      return; 
    boolean close1 = false;
    if (c1 == null) {
      c1 = getClient(getBaseUrl(vrl1.toString()), "", com.crushftp.client.Common.log);
      close1 = true;
    } 
    if (c1.stat(vrl1.getPath()).getProperty("type").equalsIgnoreCase("DIR")) {
      Vector list = new Vector();
      c1.list(vrl1.getPath(), list);
      for (int x = 0; x < list.size(); x++) {
        Properties p2 = list.elementAt(x);
        if (p2.getProperty("type").equalsIgnoreCase("DIR"))
          recurseDelete(new VRL(String.valueOf(vrl1.toString()) + p2.getProperty("name") + "/"), test_mode, c1, depth + 1); 
        if (test_mode) {
          Log.log("SERVER", 0, "*****************" + LOC.G("DELETE") + ":" + vrl1.getProtocol() + "://" + vrl1.getUsername() + "@" + vrl1.getHost() + "/" + vrl1.getPath() + p2.getProperty("name"));
        } else {
          c1.delete(String.valueOf(vrl1.getPath()) + p2.getProperty("name"));
        } 
      } 
    } 
    if (test_mode) {
      Log.log("SERVER", 0, "*****************" + LOC.G("DELETE") + ":" + vrl1.getProtocol() + "://" + vrl1.getUsername() + "@" + vrl1.getHost() + "/" + vrl1.getPath());
    } else {
      c1.delete(vrl1.getPath());
    } 
    if (depth == 0 && close1) {
      c1.logout();
      c1.close();
    } 
  }
  
  public static long recurseSize_U(String real_path, long size, SessionCrush theSession) {
    return recurseSize(new File_B(new File_U(real_path)), size, theSession);
  }
  
  private static long recurseSize(String real_path, long size, SessionCrush theSession) {
    return recurseSize(new File_B(new File_S(real_path)), size, theSession);
  }
  
  private static long recurseSize(File_B f, long size, SessionCrush theSession) {
    String real_path = null;
    try {
      real_path = f.getCanonicalPath();
      if (theSession != null && !theSession.not_done)
        throw new RuntimeException("Session is dead..."); 
      if (real_path.trim().equals("/"))
        return size; 
      if (real_path.trim().equals("~"))
        return size; 
      if (real_path.indexOf(":") >= 0 && real_path.length() < 4)
        return size; 
      f = new File_B(real_path);
      real_path = String.valueOf(f.getCanonicalPath()) + "/";
      f = new File_B(real_path);
    } catch (IOException e) {
      e.printStackTrace();
    } 
    if (f.isDirectory()) {
      String[] files = f.list();
      for (int x = 0; x < files.length; x++) {
        File_B f2 = new File_B(String.valueOf(real_path) + files[x]);
        if (!isSymbolicLink(f2))
          if (f2.isDirectory()) {
            size = recurseSize(new File_B(String.valueOf(real_path) + files[x] + "/"), size, theSession);
          } else {
            size += f2.length();
          }  
      } 
    } 
    size += f.length();
    return size;
  }
  
  public static void getAllFileListing(Vector list, String path, int depth, boolean includeFolders) throws Exception {
    File_S item = new File_S(path);
    if (item.isFile()) {
      list.addElement(item);
    } else {
      appendListing(path, list, "", depth, includeFolders);
    } 
  }
  
  public static void getAllFileListing_U(Vector list, String path, int depth, boolean includeFolders) throws Exception {
    File_U item = new File_U(path);
    if (item.isFile()) {
      list.addElement(item);
    } else {
      appendListing_U(path, list, "", depth, includeFolders);
    } 
  }
  
  public static void appendListing(String path, Vector list, String dir, int depth, boolean includeFolders) throws Exception {
    appendListing(path, list, dir, depth, includeFolders, true);
  }
  
  public static void appendListing_U(String path, Vector list, String dir, int depth, boolean includeFolders) throws Exception {
    appendListing(path, list, dir, depth, includeFolders, false);
  }
  
  private static void appendListing(String path, Vector list, String dir, int depth, boolean includeFolders, boolean is_server_File) throws Exception {
    String[] items;
    if (depth == 0)
      return; 
    depth--;
    if (!path.endsWith("/"))
      path = String.valueOf(path) + "/"; 
    if (is_server_File) {
      items = (new File_S(String.valueOf(path) + dir)).list();
    } else {
      items = (new File_U(String.valueOf(path) + dir)).list();
    } 
    if (items == null)
      return; 
    for (int x = 0; x < items.length; x++) {
      File item;
      if (is_server_File) {
        item = new File_S(String.valueOf(path) + dir + items[x]);
      } else {
        item = new File_U(String.valueOf(path) + dir + items[x]);
      } 
      if (item.isFile() || includeFolders)
        list.addElement(item); 
      if (item.isDirectory())
        appendListing(path, list, String.valueOf(dir) + items[x] + "/", depth, includeFolders, is_server_File); 
    } 
    if (items.length == 0)
      if (is_server_File) {
        list.addElement(new File_S(String.valueOf(path) + dir));
      } else {
        list.addElement(new File_U(String.valueOf(path) + dir));
      }  
  }
  
  public static void updateObjectLogOnly(Object source, String path, StringBuffer log_summary) {
    Vector log = new Vector();
    log.addElement(String.valueOf(path) + " " + source);
    while (log.size() > 0) {
      String s = log.remove(0);
      Log.log("SERVER", 0, "SETTINGS:" + s);
      log_summary.append(s).append("<br/>\r\n");
    } 
  }
  
  public static void updateObjectLog(Object source, Object dest, StringBuffer log_summary) {
    Vector log = new Vector();
    updateObjectLog(source, dest, "", log, true, log_summary);
    while (log.size() > 0)
      Log.log("SERVER", 0, "SETTINGS:" + log.remove(0)); 
  }
  
  public static void updateObjectLog(Object source, Object dest, String path, boolean update, StringBuffer log_summary) {
    Vector log = new Vector();
    updateObjectLog(source, dest, path, log, update, log_summary);
    while (log.size() > 0)
      Log.log("SERVER", 0, "SETTINGS:" + log.remove(0)); 
  }
  
  public static void updateObjectLog(Object source, Object dest, String path, Vector log, boolean update, StringBuffer log_summary) {
    if (source instanceof Properties) {
      Enumeration the_list = ((Properties)source).propertyNames();
      while (the_list.hasMoreElements()) {
        String cur = the_list.nextElement().toString();
        Object sourceO = ((Properties)source).get(cur);
        Object destO = ((Properties)dest).get(cur);
        if (destO == null || destO instanceof String) {
          if (destO == null || (destO != null && !destO.toString().equals(sourceO.toString()))) {
            Object object1 = sourceO;
            Object object2 = destO;
            if (cur.equals("password"))
              object1 = "*********"; 
            if (cur.equals("password"))
              object2 = "*********"; 
            if (cur.equals("url"))
              try {
                object1 = (new VRL((String)object1)).safe();
                object2 = (new VRL((String)object2)).safe();
              } catch (Exception exception) {} 
            String s = String.valueOf(path) + ":" + cur + ", new=" + object1 + " old=" + object2;
            log.addElement(s);
            if (log_summary != null)
              log_summary.append(s).append("<br/>\r\n"); 
          } 
          if (update)
            ((Properties)dest).put(cur, sourceO); 
          continue;
        } 
        updateObjectLog(sourceO, destO, String.valueOf(path) + "/" + cur, log, update, log_summary);
      } 
    } else if (source instanceof Vector) {
      while (((Vector)source).size() < ((Vector)dest).size()) {
        Object delO = ((Vector)dest).elementAt(((Vector)dest).size() - 1);
        String s = String.valueOf(path) + ":remove " + (((Vector)dest).size() - 1) + "=" + delO;
        log.addElement(s);
        if (log_summary != null)
          log_summary.append(s).append("<br/>\r\n"); 
        if (update) {
          ((Vector)dest).removeElementAt(((Vector)dest).size() - 1);
          continue;
        } 
        break;
      } 
      for (int x = 0; x < ((Vector)source).size(); x++) {
        if (x > ((Vector)dest).size() - 1) {
          Object object = ((Vector)source).elementAt(x);
          String s = String.valueOf(path) + ":add " + x + "=" + object;
          log.addElement(s);
          if (log_summary != null)
            log_summary.append(s).append("<br/>\r\n"); 
          if (update) {
            ((Vector)dest).addElement("");
            ((Vector)dest).setElementAt(object, x);
          } 
        } 
        Object sourceO = ((Vector)source).elementAt(x);
        Object destO = (x > ((Vector)dest).size() - 1) ? null : ((Vector)dest).elementAt(x);
        if (destO == null || destO instanceof String) {
          if (destO != null && !destO.toString().equals(sourceO.toString())) {
            String s = String.valueOf(path) + ":" + x + "=" + sourceO;
            log.addElement(s);
            if (log_summary != null)
              log_summary.append(s).append("<br/>\r\n"); 
          } 
          if (update)
            ((Vector)dest).setElementAt(sourceO, x); 
        } else {
          updateObjectLog(sourceO, destO, String.valueOf(path) + "/" + x, log, update, log_summary);
        } 
      } 
    } 
  }
  
  static String isOSXApp = "";
  
  public static boolean OSXApp() {
    if (isOSXApp.equals(""))
      isOSXApp = (new StringBuffer(String.valueOf((new File_S(System.getProperty("crushftp.executable"))).exists()))).toString(); 
    return isOSXApp.equals("true");
  }
  
  public static void killSystemProperties() {
    System.getProperties().remove("crushftp.home");
    System.getProperties().remove("crushftp.users");
    System.getProperties().remove("crushftp.prefs");
    System.getProperties().remove("crushftp.log");
    System.getProperties().remove("crushftp.plugins");
    System.getProperties().remove("crushftp.web");
    System.getProperties().remove("crushftp.stats");
    System.getProperties().remove("crushftp.sync");
    System.getProperties().remove("crushftp.search");
    System.getProperties().remove("crushftp.backup");
  }
  
  public static void initSystemProperties(boolean osxAppOK) {
    System.setProperty("crushftp.executable", "../../MacOS/CrushFTP.command");
    System.setProperty("crushftp.osxprefix", "../../../../");
    String backupLocation = "/Library/Application Support/CrushFTP/";
    if (OSXApp()) {
      (new File_S(backupLocation)).mkdirs();
      if (!(new File_S(backupLocation)).exists())
        backupLocation = String.valueOf(System.getProperty("user.home")) + backupLocation; 
    } 
    if (System.getProperty("crushftp.home") == null)
      System.setProperty("crushftp.home", "./"); 
    System.setProperty("sshtools.home", System.getProperty("crushftp.home"));
    if (OSXApp() && osxAppOK && (new File_S(String.valueOf(System.getProperty("crushftp.home")) + System.getProperty("crushftp.osxprefix") + "plugins/")).exists())
      (new File_S(String.valueOf(System.getProperty("crushftp.home")) + System.getProperty("crushftp.osxprefix") + "plugins/")).renameTo(new File_S(String.valueOf(System.getProperty("crushftp.home")) + "plugins/")); 
    if (System.getProperty("crushftp.users") == null)
      if (OSXApp() && osxAppOK) {
        System.setProperty("crushftp.users", String.valueOf(System.getProperty("crushftp.home")) + System.getProperty("crushftp.osxprefix") + "users/");
      } else {
        System.setProperty("crushftp.users", String.valueOf(System.getProperty("crushftp.home")) + "users/");
      }  
    if (System.getProperty("crushftp.prefs") == null)
      if (OSXApp() && osxAppOK) {
        System.setProperty("crushftp.prefs", String.valueOf(System.getProperty("crushftp.home")) + System.getProperty("crushftp.osxprefix"));
      } else {
        System.setProperty("crushftp.prefs", System.getProperty("crushftp.home"));
      }  
    if (System.getProperty("crushftp.log") == null)
      if (OSXApp() && osxAppOK) {
        System.setProperty("crushftp.log", String.valueOf(System.getProperty("crushftp.home")) + System.getProperty("crushftp.osxprefix") + "CrushFTP.log");
      } else {
        System.setProperty("crushftp.log", String.valueOf(System.getProperty("crushftp.home")) + "CrushFTP.log");
      }  
    if (System.getProperty("crushftp.plugins") == null)
      if (OSXApp() && osxAppOK) {
        System.setProperty("crushftp.plugins", System.getProperty("crushftp.home"));
      } else {
        System.setProperty("crushftp.plugins", System.getProperty("crushftp.home"));
      }  
    if (System.getProperty("crushftp.web") == null)
      if (OSXApp() && osxAppOK) {
        System.setProperty("crushftp.web", String.valueOf(System.getProperty("crushftp.home")) + System.getProperty("crushftp.osxprefix"));
      } else {
        System.setProperty("crushftp.web", System.getProperty("crushftp.home"));
      }  
    if (System.getProperty("crushftp.stats") == null)
      if (OSXApp() && osxAppOK) {
        System.setProperty("crushftp.stats", String.valueOf(System.getProperty("crushftp.home")) + System.getProperty("crushftp.osxprefix"));
      } else {
        System.setProperty("crushftp.stats", System.getProperty("crushftp.home"));
      }  
    if (System.getProperty("crushftp.sync") == null)
      if (OSXApp() && osxAppOK) {
        System.setProperty("crushftp.sync", String.valueOf(System.getProperty("crushftp.home")) + System.getProperty("crushftp.osxprefix"));
      } else {
        System.setProperty("crushftp.sync", System.getProperty("crushftp.home"));
      }  
    if (System.getProperty("crushftp.search") == null)
      if (OSXApp() && osxAppOK) {
        System.setProperty("crushftp.search", String.valueOf(System.getProperty("crushftp.home")) + System.getProperty("crushftp.osxprefix"));
      } else {
        System.setProperty("crushftp.search", System.getProperty("crushftp.home"));
      }  
    if (System.getProperty("crushftp.backup") == null)
      if (OSXApp() && osxAppOK) {
        System.setProperty("crushftp.backup", backupLocation);
      } else {
        System.setProperty("crushftp.backup", System.getProperty("crushftp.home"));
      }  
  }
  
  public static long lastPrefBackup = 0L;
  
  public static void write_server_settings(Properties prefs, String instance) throws Exception {
    synchronized (writeLock) {
      if (instance == null || instance.equals("")) {
        instance = "";
      } else if (!instance.startsWith("_")) {
        instance = "_" + instance;
      } 
      (new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/")).mkdirs();
      if ((new Date()).getTime() - lastPrefBackup > 300000L) {
        (new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/prefs" + instance + "199.XML")).delete();
        for (int x = 198; x >= 0; x--) {
          try {
            (new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/prefs" + instance + x + ".XML")).renameTo(new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/prefs" + instance + (x + 1) + ".XML"));
          } catch (Exception exception) {}
        } 
        lastPrefBackup = (new Date()).getTime();
      } 
      writeXMLObject(String.valueOf(System.getProperty("crushftp.prefs")) + "prefs" + instance + ".saved.XML", prefs, "server_prefs");
      copy(String.valueOf(System.getProperty("crushftp.prefs")) + "prefs" + instance + ".XML", String.valueOf(System.getProperty("crushftp.backup")) + "backup/prefs" + instance + "0.XML", true);
      (new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "prefs" + instance + ".XML")).delete();
      int loops = 0;
      while (!(new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "prefs" + instance + ".saved.XML")).renameTo(new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "prefs" + instance + ".XML")) && loops++ < 100)
        Thread.sleep(100L); 
      updateOSXInfo(String.valueOf(System.getProperty("crushftp.prefs")) + "prefs" + instance + ".XML");
      updateOSXInfo(String.valueOf(System.getProperty("crushftp.backup")) + "backup/prefs" + instance + "0.XML");
    } 
  }
  
  public static void startMultiThreadZipper(VFS uVFS, RETR_handler retr, String path, int msDelay, boolean singleThread, Vector activeThreads) throws Exception {
    if (singleThread) {
      try {
        uVFS.getListing(retr.zipFiles, path, 999, 10000, true);
      } catch (Exception exception) {}
      retr.zipping = true;
    } else {
      null.multithreadZip mz = new null.multithreadZip(uVFS, retr, path, activeThreads);
      activeThreads.addElement(mz);
      Worker.startWorker(mz);
      Thread.sleep(msDelay);
      retr.activeZipThreads = activeThreads;
      retr.zipping = true;
    } 
  }
  
  public static void unzip(String path, GenericClient c, SessionCrush thisSession, String basePath) throws Exception {
    String path1 = all_but_last(path);
    ZipInputStream zin = new ZipInputStream(new FileInputStream(new File_S(path)));
    try {
      Vector folders = new Vector();
      ZipEntry entry;
      while ((entry = zin.getNextEntry()) != null) {
        String path2 = entry.getName();
        path2 = path2.replace('\\', '/');
        path2 = path2.replace('\\', '/');
        path2 = com.crushftp.client.Common.dots(path2);
        if (path2.startsWith("/"))
          path2 = path2.substring(1); 
        if (entry.isDirectory()) {
          (new File_S(String.valueOf(path1) + path2)).mkdirs();
          folders.addElement(String.valueOf(entry.getTime()) + ":" + path1 + path2);
          thisSession.setFolderPrivs(c, thisSession.uVFS.get_item(String.valueOf(basePath) + path2));
          continue;
        } 
        (new File_S(all_but_last(String.valueOf(path1) + path2))).mkdirs();
        thisSession.setFolderPrivs(c, thisSession.uVFS.get_item(all_but_last(String.valueOf(basePath) + path2)));
        byte[] b = new byte[32768];
        int bytes_read = 0;
        RandomAccessFile out = null;
        try {
          out = new RandomAccessFile(new File_S(String.valueOf(path1) + path2), "rw");
          while (bytes_read >= 0) {
            bytes_read = zin.read(b);
            if (bytes_read > 0)
              out.write(b, 0, bytes_read); 
          } 
        } finally {
          out.close();
        } 
        thisSession.setFolderPrivs(c, thisSession.uVFS.get_item(String.valueOf(basePath) + path2));
        boolean bool = ServerStatus.BG("disable_mdtm_modifications");
        if (!thisSession.SG("disable_mdtm_modifications").equals("") && ServerStatus.BG("v9_beta"))
          bool = thisSession.BG("disable_mdtm_modifications"); 
        if (!bool)
          (new File_S(String.valueOf(path1) + path2)).setLastModified(entry.getTime()); 
      } 
      boolean disable_mdtm_modifications = ServerStatus.BG("disable_mdtm_modifications");
      if (!thisSession.SG("disable_mdtm_modifications").equals("") && ServerStatus.BG("v9_beta"))
        disable_mdtm_modifications = thisSession.BG("disable_mdtm_modifications"); 
      if (!disable_mdtm_modifications)
        for (int x = folders.size() - 1; x >= 0; x--) {
          String s = folders.elementAt(x).toString();
          (new File_S(s.substring(s.indexOf(":") + 1))).setLastModified(Long.parseLong(s.substring(0, s.indexOf(":"))));
        }  
    } catch (Exception e) {
      zin.close();
      throw e;
    } 
    zin.close();
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
  
  public void purgeOldBackups(int count) {
    try {
      String root = String.valueOf(System.getProperty("crushftp.backup")) + "backup/";
      String[] list = (new File_S(root)).list();
      if (list == null)
        return; 
      if (list.length < count)
        return; 
      int deletedCount = 0;
      for (int x = 0; x < list.length; x++) {
        File_S f = new File_S(String.valueOf(root) + list[x]);
        String name = f.getName();
        if (name.startsWith("users-")) {
          String date = name.substring(name.indexOf("-") + 1, name.indexOf("_"));
          SimpleDateFormat sdf = new SimpleDateFormat("MMddyyyy", Locale.US);
          if ((new Date()).getTime() - sdf.parse(date).getTime() > 604800000L) {
            if (list.length - deletedCount < count)
              return; 
            deletedCount++;
            recurseDelete(f.getAbsolutePath(), false);
          } 
        } 
      } 
    } catch (Exception e) {
      Log.log("SERVER", 1, e);
    } 
  }
  
  public static Properties removeNonStrings(Properties p) {
    Enumeration e = p.keys();
    while (e.hasMoreElements()) {
      String key = e.nextElement().toString();
      if (!(p.get(key) instanceof String))
        p.remove(key); 
    } 
    return p;
  }
  
  public static Properties setupReportDates(Properties params, String show, String startDate, String endDate) {
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);
    if (!show.equals("")) {
      if (show.toUpperCase().endsWith("DAY") || show.toUpperCase().endsWith("DAYS")) {
        if (show.equalsIgnoreCase("LAST DAY"))
          show = "Last 1 day"; 
        show = show.substring(show.indexOf(" ") + 1, show.lastIndexOf(" "));
        int days = Integer.parseInt(show);
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        calendar.add(5, -1 * days);
        params.put("startDate", sdf.format(calendar.getTime()));
      } 
      if (show.toUpperCase().endsWith("HOUR") || show.toUpperCase().endsWith("HOURS")) {
        if (show.equalsIgnoreCase("LAST HOUR"))
          show = "Last 1 hour"; 
        show = show.substring(show.indexOf(" ") + 1, show.lastIndexOf(" "));
        int hours = Integer.parseInt(show);
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        calendar.add(10, -1 * hours);
        params.put("startDate", sdf.format(calendar.getTime()));
      } 
    } else {
      params.put("startDate", startDate);
    } 
    if (endDate.trim().equals("")) {
      Calendar calendar = new GregorianCalendar();
      calendar.setTime(new Date());
      calendar.add(5, 1);
      endDate = sdf.format(calendar.getTime());
    } 
    params.put("endDate", endDate);
    return params;
  }
  
  public static boolean haveWriteAccess() {
    File_S f = new File_S(String.valueOf(System.getProperty("crushftp.home")) + "/writeable.tmp");
    try {
      RandomAccessFile ra = new RandomAccessFile(f, "rw");
      ra.close();
    } catch (Exception exception) {}
    boolean ok = f.exists();
    f.delete();
    return ok;
  }
  
  public static Socket getSTORSocket(SessionCrush thisSession, STOR_handler stor_files, String upload_item, boolean httpUpload, String user_dir, boolean random_access, long start_resume_loc, Properties metaInfo) throws Exception {
    Socket local_s = null;
    while (stor_files.active2.getProperty("active", "").equals("true"))
      Thread.sleep(1L); 
    Properties dir_item = null;
    dir_item = thisSession.uVFS.get_item(String.valueOf(user_dir) + upload_item);
    if (dir_item == null)
      dir_item = thisSession.uVFS.get_item_parent(String.valueOf(user_dir) + upload_item); 
    Socket data_sock = null;
    synchronized (onserversocklock) {
      for (int x = 0; x < 10; x++) {
        ServerSocket ss = new ServerSocket(0);
        try {
          ss.setSoTimeout(5000);
          data_sock = new Socket("127.0.0.1", ss.getLocalPort());
          local_s = ss.accept();
          break;
        } catch (IOException e) {
          if (data_sock != null)
            data_sock.close(); 
          if (x >= 9)
            throw e; 
        } finally {
          ss.close();
        } 
      } 
    } 
    thisSession.uiPUT("file_transfer_mode", "BINARY");
    stor_files.httpUpload = httpUpload;
    try {
      stor_files.c.close();
    } catch (Exception exception) {}
    String the_dir = String.valueOf(user_dir) + upload_item;
    Properties p = new Properties();
    p.put("the_dir", the_dir);
    thisSession.runPlugin("transfer_path", p);
    the_dir = p.getProperty("the_dir", the_dir);
    stor_files.init_vars(the_dir, start_resume_loc, thisSession, dir_item, "STOR", false, random_access, metaInfo, data_sock);
    Worker.startWorker(stor_files);
    return local_s;
  }
  
  public static Socket getRETRSocket(SessionCrush thisSession, RETR_handler retr_files, long start_resume_loc, String upload_item, boolean httpDownload) throws Exception {
    Socket local_s = null;
    String path = String.valueOf(thisSession.uiSG("current_dir")) + upload_item;
    if (path.indexOf(":filetree") >= 0 && ServerStatus.BG("allow_filetree"))
      path = path.substring(0, path.indexOf(":filetree")); 
    Properties item = thisSession.uVFS.get_item(path);
    VRL otherFile = new VRL(item.getProperty("url"));
    Socket data_sock = null;
    synchronized (onserversocklock) {
      for (int x = 0; x < 10; x++) {
        ServerSocket ss = new ServerSocket(0);
        try {
          ss.setSoTimeout(5000);
          data_sock = new Socket("127.0.0.1", ss.getLocalPort());
          local_s = ss.accept();
          break;
        } catch (IOException e) {
          if (data_sock != null)
            data_sock.close(); 
          if (x >= 9)
            throw e; 
        } finally {
          ss.close();
        } 
      } 
    } 
    thisSession.uiPUT("file_transfer_mode", "BINARY");
    try {
      retr_files.c.close();
    } catch (Exception exception) {}
    retr_files.data_os = data_sock.getOutputStream();
    retr_files.httpDownload = httpDownload;
    String the_dir = String.valueOf(thisSession.uiSG("current_dir")) + upload_item;
    Properties p = new Properties();
    p.put("the_dir", the_dir);
    thisSession.runPlugin("transfer_path", p);
    the_dir = p.getProperty("the_dir", the_dir);
    retr_files.init_vars(the_dir, start_resume_loc, -1L, thisSession, item, false, "", otherFile, data_sock);
    Worker.startWorker(retr_files);
    return local_s;
  }
  
  public static Properties getConnectedSockets() throws Exception {
    Properties sockProp = new Properties();
    Socket sock1 = null;
    Socket sock2 = null;
    for (int x = 0; x < 10; x++) {
      ServerSocket ss = new ServerSocket(0);
      try {
        ss.setSoTimeout(5000);
        sock1 = new Socket("127.0.0.1", ss.getLocalPort());
        sock2 = ss.accept();
        break;
      } catch (IOException e) {
        if (sock1 != null)
          sock1.close(); 
        if (x >= 9)
          throw e; 
      } finally {
        ss.close();
      } 
    } 
    sockProp.put("sock1", sock1);
    sockProp.put("sock2", sock2);
    return sockProp;
  }
  
  public static String normalize2(String s) {
    if (ServerStatus.BG("normalize_utf8"))
      return Normalizer.normalize(s, Normalizer.Form.NFC); 
    return s;
  }
  
  public static void doSign(String certPath, String keystorePass, String jarFile) throws Exception {}
  
  public static void copy(String src, String dst, boolean overwrite) throws Exception {
    copy(new File_B(new File_S(src)), new File_B(new File_S(dst)), overwrite);
  }
  
  public static void copy_U(String src, String dst, boolean overwrite) throws Exception {
    copy(new File_B(new File_U(src)), new File_B(new File_U(dst)), overwrite);
  }
  
  private static void copy(File_B src, File_B dst, boolean overwrite) throws Exception {
    if (src.isDirectory()) {
      dst.mkdirs();
      return;
    } 
    if (dst.exists() && !overwrite)
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
    dst.setLastModified(src.lastModified());
  }
  
  public static void copy(VRL vrl1, VRL vrl2, GenericClient c1, GenericClient c2, boolean overwrite) throws Exception {
    Properties stat1 = c1.stat(vrl1.getPath());
    Log.log("SERVER", 2, "copy vrl1_path:" + vrl1.getPath());
    Log.log("SERVER", 2, "copy vrl:" + vrl1.safe() + " to " + vrl2.safe());
    Properties stat1_safe = (Properties)stat1.clone();
    if (stat1_safe.containsKey("url"))
      stat1_safe.put("url", (new VRL(stat1_safe.getProperty("url"))).safe()); 
    Log.log("SERVER", 2, "copy stat1:" + stat1_safe);
    if (stat1.getProperty("type").equalsIgnoreCase("DIR")) {
      c2.makedirs(vrl2.getPath());
      return;
    } 
    Properties stat2 = c2.stat(vrl2.getPath());
    Properties stat2_safe = null;
    if (stat2 != null)
      stat2_safe = (Properties)stat2.clone(); 
    if (stat2_safe != null && stat2_safe.containsKey("url"))
      stat2_safe.put("url", (new VRL(stat2_safe.getProperty("url"))).safe()); 
    Log.log("SERVER", 2, "copy stat2:" + stat2_safe);
    if (stat2 != null && !overwrite)
      return; 
    InputStream in = null;
    OutputStream out = null;
    try {
      in = c1.download(vrl1.getPath(), 0L, -1L, true);
      out = c2.upload(vrl2.getPath(), 0L, true, true);
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
    if (c1 instanceof S3Client && c2 instanceof S3Client) {
      String uploaded_by = ((S3Client)c1).getUploadedByMetadata(vrl1.getPath());
      c2.setConfig("uploaded_by", uploaded_by);
    } 
    c2.mdtm(vrl2.getPath(), Long.parseLong(stat1.getProperty("modified")));
  }
  
  public static void recurseCopyThreaded(String src, String dst, boolean overwrite, boolean move) throws Exception {
    recurseCopyThreaded(new File_B(new File_S(src)), new File_B(new File_S(dst)), overwrite, move);
  }
  
  public static void recurseCopyThreaded_U(String src, String dst, boolean overwrite, boolean move) throws Exception {
    recurseCopyThreaded(new File_B(new File_U(src)), new File_B(new File_U(dst)), overwrite, move);
  }
  
  private static void recurseCopyThreaded(File_B f_src, File_B f_dst, boolean overwrite, boolean move) throws Exception {
    Worker.startWorker(new null.Threader(f_src, f_dst, overwrite, move));
  }
  
  public static void recurseCopy(String src, String dst, boolean overwrite) throws Exception {
    recurseCopy(new File_B(new File_S(src)), new File_B(new File_S(dst)), overwrite);
  }
  
  public static void recurseCopy_U(String src, String dst, boolean overwrite) throws Exception {
    recurseCopy(new File_B(new File_U(src)), new File_B(new File_U(dst)), overwrite);
  }
  
  public static void recurseCopy(File_B f_src, File_B f_dst, boolean overwrite) throws Exception {
    String src_real_path = f_src.getCanonicalPath();
    String dst_real_path = f_dst.getCanonicalPath();
    if (src_real_path.trim().equals("/"))
      return; 
    if (src_real_path.trim().equals("~"))
      return; 
    if (src_real_path.indexOf(":") >= 0 && src_real_path.length() < 4)
      return; 
    if (dst_real_path.trim().equals("/"))
      return; 
    if (dst_real_path.trim().equals("~"))
      return; 
    if (dst_real_path.indexOf(":") >= 0 && dst_real_path.length() < 4)
      return; 
    File_B f = new File_B(src_real_path);
    try {
      src_real_path = String.valueOf(f.getCanonicalPath()) + "/";
      f = new File_B(src_real_path);
    } catch (Exception exception) {}
    copy(f, new File_B(dst_real_path), overwrite);
    if (f.isDirectory()) {
      String[] files = f.list();
      if (files != null) {
        if (!dst_real_path.endsWith("/") && !dst_real_path.endsWith("\\"))
          dst_real_path = String.valueOf(dst_real_path) + "/"; 
        for (int x = 0; x < files.length; x++) {
          File_B f2 = new File_B(String.valueOf(src_real_path) + files[x]);
          if (!isSymbolicLink(f2)) {
            copy(f2.getCanonicalPath(), String.valueOf(dst_real_path) + files[x], overwrite);
            if (f2.isDirectory())
              recurseCopy(new File_B(String.valueOf(src_real_path) + files[x] + "/"), new File_B(String.valueOf(dst_real_path) + files[x] + "/"), overwrite); 
          } 
        } 
      } 
    } 
  }
  
  public static void recurseCopy(VRL vrl1, VRL vrl2, GenericClient c1, GenericClient c2, int depth, boolean overwrite, StringBuffer status) throws Exception {
    if (vrl1.getProtocol().equalsIgnoreCase("file") && vrl1.getPath().trim().equals("/"))
      return; 
    if (vrl1.getProtocol().equalsIgnoreCase("file") && vrl1.getPath().trim().equals("~"))
      return; 
    if (vrl1.getProtocol().equalsIgnoreCase("file") && vrl1.getPath().indexOf(":") >= 0 && vrl1.getPath().length() < 4)
      return; 
    if (vrl2.getProtocol().equalsIgnoreCase("file") && vrl2.getPath().trim().equals("/"))
      return; 
    if (vrl2.getProtocol().equalsIgnoreCase("file") && vrl2.getPath().trim().equals("~"))
      return; 
    if (vrl2.getProtocol().equalsIgnoreCase("file") && vrl2.getPath().indexOf(":") >= 0 && vrl2.getPath().length() < 4)
      return; 
    boolean close1 = false;
    boolean close2 = false;
    if (c1 == null) {
      c1 = getClient(getBaseUrl(vrl1.toString()), "", com.crushftp.client.Common.log);
      c1.login(vrl1.getUsername(), vrl1.getPassword(), null);
      close1 = true;
    } 
    if (c2 == null) {
      c2 = getClient(getBaseUrl(vrl2.toString()), "", com.crushftp.client.Common.log);
      c2.login(vrl2.getUsername(), vrl2.getPassword(), null);
      close2 = true;
    } 
    copy(vrl1, vrl2, c1, c2, overwrite);
    if (c1.stat(vrl1.getPath()).getProperty("type").equalsIgnoreCase("DIR")) {
      Vector list = new Vector();
      synchronized (status) {
        if (status.toString().equals("CANCELLED"))
          throw new Exception("CANCELLED"); 
        status.setLength(0);
        status.append("Getting list:" + vrl1.safe()).append("...");
      } 
      c1.list(vrl1.getPath().endsWith("/") ? vrl1.getPath() : (String.valueOf(vrl1.getPath()) + "/"), list);
      for (int x = 0; x < list.size(); x++) {
        Properties p1 = list.elementAt(x);
        VRL vrl2_copy = new VRL(String.valueOf(url_decode(vrl2.toString())) + p1.getProperty("name"));
        synchronized (status) {
          if (status.toString().equals("CANCELLED"))
            throw new Exception("CANCELLED"); 
          status.setLength(0);
          status.append("Copying:" + vrl2_copy.safe()).append("...");
        } 
        copy(new VRL(url_decode(p1.getProperty("url"))), vrl2_copy, c1, c2, overwrite);
        if (p1.getProperty("type").equalsIgnoreCase("DIR"))
          recurseCopy(new VRL(url_decode(p1.getProperty("url"))), new VRL(String.valueOf(url_decode(vrl2.toString())) + p1.getProperty("name") + "/"), c1, c2, depth + 1, overwrite, status); 
      } 
    } 
    if (depth == 0) {
      if (close1) {
        c1.close();
        c1.logout();
      } 
      if (close2) {
        c2.close();
        c2.logout();
      } 
    } 
  }
  
  public static boolean filterDir(String path, Vector filters) {
    if (filters == null || path.equals("/"))
      return true; 
    boolean ok = true;
    for (int x = 0; x < filters.size(); x++) {
      Properties p = filters.elementAt(x);
      String method = p.getProperty("searchPath", "");
      if (method.equals("contains") && path.toUpperCase().indexOf(p.getProperty("path").toUpperCase()) < 0)
        ok = false; 
      if (method.equals("starts with") && !path.toUpperCase().startsWith(p.getProperty("path").toUpperCase()))
        ok = false; 
      if (method.equals("ends with") && !path.toUpperCase().endsWith(p.getProperty("path").toUpperCase()))
        ok = false; 
      if (method.equals("equals") && !path.toUpperCase().equals(p.getProperty("path").toUpperCase()))
        ok = false; 
    } 
    return ok;
  }
  
  public static String exec(String[] c) throws Exception {
    com.crushftp.client.Common.check_exec();
    Process proc = Runtime.getRuntime().exec(c);
    BufferedReader br1 = new BufferedReader(new InputStreamReader(proc.getInputStream()));
    String result = "";
    String lastLine = "";
    while ((result = br1.readLine()) != null) {
      if (!result.trim().equals(""))
        lastLine = result; 
    } 
    br1.close();
    proc.waitFor();
    try {
      proc.destroy();
    } catch (Exception exception) {}
    return lastLine;
  }
  
  public static String free_space(String in_str) {
    while (in_str.indexOf("<FREESPACE>") >= 0) {
      String data = in_str.substring(in_str.indexOf("<FREESPACE>") + "<FREESPACE>".length(), in_str.indexOf("</FREESPACE>"));
      data = com.crushftp.client.Common.format_bytes_short2(get_free_disk_space(data));
      in_str = String.valueOf(in_str.substring(0, in_str.indexOf("<FREESPACE>"))) + data + in_str.substring(in_str.indexOf("</FREESPACE>") + "</FREESPACE>".length());
    } 
    return in_str;
  }
  
  public static long get_free_disk_space(String disk) {
    String line = "";
    String totalData = "";
    try {
      if (machine_is_windows()) {
        if (disk.length() == 1)
          disk = String.valueOf(disk) + ":"; 
        if (!disk.endsWith("\\"))
          disk = String.valueOf(disk) + "\\"; 
        com.crushftp.client.Common.check_exec();
        Process process = Runtime.getRuntime().exec(new String[] { "cmd", "/C", "dir", disk });
        BufferedReader br1 = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String result = "";
        while ((result = br1.readLine()) != null) {
          if (!result.trim().equals("")) {
            totalData = String.valueOf(totalData) + result + "\r\n";
            if (result.toLowerCase().indexOf("bytes fr") >= 0)
              line = result.toLowerCase().trim(); 
          } 
        } 
        br1.close();
        process.waitFor();
        try {
          process.destroy();
        } catch (Exception exception) {}
        line = line.toLowerCase();
        line = line.substring(line.lastIndexOf(")") + 1, line.lastIndexOf("bytes")).trim();
        line = replace_str(line, ",", "").trim();
        line = replace_str(line, ".", "").trim();
        return Long.parseLong(line);
      } 
      if (machine_is_x()) {
        line = exec(new String[] { "df", "-b", disk });
        StringTokenizer stringTokenizer = new StringTokenizer(line);
        stringTokenizer.nextElement();
        stringTokenizer.nextElement();
        stringTokenizer.nextElement();
        long size = Long.parseLong(stringTokenizer.nextElement().toString());
        return size * 512L;
      } 
      if (machine_is_solaris()) {
        line = exec(new String[] { "df", "-k", disk });
        StringTokenizer stringTokenizer = new StringTokenizer(line);
        stringTokenizer.nextElement();
        stringTokenizer.nextElement();
        stringTokenizer.nextElement();
        return Long.parseLong(stringTokenizer.nextElement().toString()) * 1024L;
      } 
      com.crushftp.client.Common.check_exec();
      Process proc = Runtime.getRuntime().exec(new String[] { "df", disk });
      BufferedReader proc_in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
      String data = "";
      while ((data = proc_in.readLine()) != null) {
        Log.log("SERVER", 2, data);
        if (!data.trim().equals("")) {
          line = data;
          totalData = String.valueOf(totalData) + data + "\r\n";
        } 
      } 
      proc_in.close();
      proc_in = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
      data = "";
      while ((data = proc_in.readLine()) != null) {
        Log.log("SERVER", 2, data);
        if (!data.trim().equals(""))
          totalData = String.valueOf(totalData) + "ERROR:" + data + "\r\n"; 
      } 
      proc_in.close();
      proc.waitFor();
      StringTokenizer st = new StringTokenizer(line);
      Vector tokens = new Vector();
      while (st.hasMoreElements())
        tokens.addElement(st.nextElement()); 
      boolean isnumber = false;
      try {
        Long.parseLong(tokens.elementAt(0).toString().trim());
        isnumber = true;
      } catch (Exception exception) {}
      if (isnumber)
        tokens.insertElementAt("nothing", 0); 
      String device = tokens.remove(0).toString();
      String blocks = tokens.remove(0).toString();
      String used = tokens.remove(0).toString();
      String avail = tokens.remove(0).toString();
      if (avail.indexOf("%") >= 0)
        avail = used; 
      if (avail.endsWith("K"))
        avail = avail.substring(0, avail.length() - 1); 
      long free = Long.parseLong(avail);
      return free * 1024L;
    } catch (Exception e) {
      Log.log("SERVER", 1, "Format not understood:" + line);
      Log.log("SERVER", 1, "totalData:" + totalData);
      Log.log("SERVER", 1, e);
      return -1L;
    } 
  }
  
  public static String replaceFormVariables(Properties form_email, String s) {
    Enumeration keys = form_email.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      s = replace_str(s, key.trim(), com.crushftp.client.Common.dots(form_email.getProperty(key)));
    } 
    return s;
  }
  
  public static Properties buildFormEmail(Properties server_settings, Vector lastUploadStats) {
    Properties form_email = new Properties();
    if (lastUploadStats != null)
      for (int xx = 0; xx < lastUploadStats.size(); xx++) {
        String web_upload_form = "";
        Properties uploadStat = lastUploadStats.elementAt(xx);
        Properties metaInfo = (Properties)uploadStat.get("metaInfo");
        if (metaInfo != null) {
          String id = metaInfo.getProperty("UploadFormId", "");
          Properties customForm = null;
          Vector customForms = (Vector)server_settings.get("CustomForms");
          if (customForms != null) {
            for (int x = 0; x < customForms.size(); x++) {
              Properties p = customForms.elementAt(x);
              if (p.getProperty("id", "").equals(id)) {
                customForm = p;
                break;
              } 
            } 
            if (customForm != null) {
              if (!customForm.containsKey("entries"))
                customForm.put("entries", new Vector()); 
              Vector entries = (Vector)customForm.get("entries");
              for (int i = 0; i < entries.size(); i++) {
                Properties p = entries.elementAt(i);
                if (!p.getProperty("type").equals("label")) {
                  web_upload_form = String.valueOf(web_upload_form) + p.getProperty("name", "").trim() + ":" + metaInfo.getProperty(p.getProperty("name", "").trim()) + "\r\n\r\n";
                  String val = metaInfo.getProperty(p.getProperty("name", "").trim());
                  if (val != null)
                    form_email.put("%" + p.getProperty("name", "").trim() + "%", val); 
                } 
              } 
            } else {
              web_upload_form = String.valueOf(web_upload_form) + metaInfo;
              Enumeration keys = metaInfo.keys();
              while (keys.hasMoreElements()) {
                String key = keys.nextElement().toString();
                String val = metaInfo.getProperty(key);
                form_email.put("%" + key.trim() + "%", val);
              } 
            } 
          } 
        } 
      }  
    return form_email;
  }
  
  public static void do_sort(Vector listing, String sort_type) {
    do_sort(listing, sort_type, "name");
  }
  
  public static void do_sort(Vector listing, String sort_type, String key) {
    if (!sort_type.equals("type")) {
      String[] names = new String[listing.size()];
      for (int x = 0; x < listing.size(); x++)
        names[x] = String.valueOf(((Properties)listing.elementAt(x)).getProperty(key, "")) + ";:;" + x; 
      Arrays.sort((Object[])names);
      Vector listing2 = (Vector)listing.clone();
      listing.removeAllElements();
      for (int i = 0; i < names.length; i++)
        listing.addElement(listing2.elementAt(Integer.parseInt(names[i].split(";:;")[1]))); 
    } else {
      int x;
      for (x = 0; x < listing.size(); x++) {
        Properties item = listing.elementAt(x);
        for (int xx = x; xx < listing.size(); xx++) {
          Properties item2 = listing.elementAt(xx);
          if (item2.getProperty("type").equals("DIR"))
            if (item2.getProperty("name").toUpperCase().compareTo(item.getProperty("name").toUpperCase()) < 0 || item.getProperty("type").equals("FILE")) {
              listing.setElementAt(item2, x);
              listing.setElementAt(item, xx);
              item = item2;
            }  
        } 
      } 
      for (x = 0; x < listing.size(); x++) {
        Properties item = listing.elementAt(x);
        if (item.getProperty("type").equals("FILE"))
          for (int xx = x; xx < listing.size(); xx++) {
            Properties item2 = listing.elementAt(xx);
            if (item2.getProperty("name").toUpperCase().compareTo(item.getProperty("name").toUpperCase()) < 0) {
              listing.setElementAt(item2, x);
              listing.setElementAt(item, xx);
              item = item2;
            } 
          }  
      } 
    } 
  }
  
  public static String chop(String dir) {
    if (dir.equals("/"))
      return ""; 
    return String.valueOf(dir.substring(0, dir.lastIndexOf("/", dir.length() - 2))) + "/";
  }
  
  public static String getMD5End(String s) {
    try {
      MessageDigest md5 = MessageDigest.getInstance("MD5");
      md5.update(s.getBytes("UTF8"));
      s = (new BigInteger(1, md5.digest())).toString(16).toUpperCase();
      return s.substring(s.length() - 6, s.length());
    } catch (Exception e) {
      Log.log("SERVER", 1, e);
      return null;
    } 
  }
  
  public static String getPartialIp(String ip) {
    String part = "";
    if (ip.indexOf(".") >= 1) {
      for (int x = 0; x < ServerStatus.IG("trusted_ip_parts"); x++)
        part = String.valueOf(part) + ip.split("\\.")[x] + "."; 
      if (part.endsWith("."))
        part = part.substring(0, part.length() - 1); 
    } 
    return part;
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
  
  public static boolean deepNotEquals(Object dest, Object src) {
    boolean result = false;
    if (src != null && src instanceof Properties) {
      Properties p = (Properties)src;
      Enumeration e = p.keys();
      while (e.hasMoreElements()) {
        String key = e.nextElement().toString();
        Object val = p.get(key);
        if (val instanceof Properties) {
          Properties pp = (Properties)((Properties)dest).get(key);
          result |= deepNotEquals(pp, val);
          continue;
        } 
        if (val instanceof Vector) {
          Vector vv = (Vector)((Properties)dest).get(key);
          result |= deepNotEquals(vv, val);
          continue;
        } 
        if (!((Properties)dest).getProperty(key, "").equals(((Properties)src).getProperty(key, "")))
          result = true; 
      } 
    } else if (src != null && src instanceof Vector) {
      Vector v = (Vector)src;
      for (int x = 0; x < v.size(); x++) {
        Object val = v.elementAt(x);
        if (val instanceof Properties) {
          Properties pp = ((Vector)dest).elementAt(x);
          result |= deepNotEquals(pp, val);
        } else if (val instanceof Vector) {
          Vector vv = ((Vector)dest).elementAt(x);
          result |= deepNotEquals(vv, val);
        } else if (!((Vector)dest).elementAt(x).toString().equals(((Vector)src).elementAt(x).toString())) {
          result = true;
        } 
      } 
    } 
    return result;
  }
  
  public static void diffObjects(Object o1, Object o2, Vector log, String path, boolean swapOldNew) {
    if (o2 != null && o2 instanceof Properties) {
      Properties p = (Properties)o2;
      Enumeration e = p.keys();
      while (e.hasMoreElements()) {
        String key = e.nextElement().toString();
        Object val = p.get(key);
        if (val instanceof Properties) {
          Properties pp = null;
          try {
            pp = (Properties)((Properties)o1).get(key);
          } catch (ClassCastException ee) {
            log.addElement("key:" + path + "/" + key + "    Unknown Class Difference");
          } 
          diffObjects(pp, val, log, String.valueOf(path) + "/" + key, swapOldNew);
          continue;
        } 
        if (val instanceof Vector) {
          Vector vv = null;
          try {
            vv = (Vector)((Properties)o1).get(key);
          } catch (ClassCastException ee) {
            log.addElement("key:" + path + "/" + key + "    Unknown Class Difference");
          } 
          diffObjects(vv, val, log, String.valueOf(path) + "/" + key, swapOldNew);
          continue;
        } 
        String val1 = "";
        String val2 = "";
        try {
          if (o1 != null)
            val1 = ((Properties)o1).getProperty(key, ""); 
        } catch (ClassCastException ee) {
          log.addElement("key:" + path + "/" + key + "    Unknown Class Difference");
        } 
        try {
          if (o2 != null)
            val2 = ((Properties)o2).getProperty(key, ""); 
        } catch (ClassCastException ee) {
          log.addElement("key:" + path + "/" + key + "    Unknown Class Difference");
        } 
        if (swapOldNew) {
          String t = val2;
          val2 = val1;
          val1 = t;
        } 
        if (!val1.equals(val2))
          log.addElement("key:" + path + "/" + key + "    Previous:" + val2 + " New:" + val1); 
      } 
    } else if (o2 != null && o2 instanceof Vector) {
      Vector v = (Vector)o2;
      for (int x = 0; x < v.size(); x++) {
        Object val = v.elementAt(x);
        if (val instanceof Properties) {
          Properties pp = null;
          try {
            if (o1 != null && ((Vector)o1).size() > x)
              pp = ((Vector)o1).elementAt(x); 
          } catch (ClassCastException ee) {
            log.addElement("key:" + path + "/" + x + "    Unknown Class Difference");
          } 
          diffObjects(pp, val, log, String.valueOf(path) + "/" + x, swapOldNew);
        } else if (val instanceof Vector) {
          Vector vv = null;
          try {
            if (o1 != null && ((Vector)o1).size() > x)
              vv = ((Vector)o1).elementAt(x); 
          } catch (ClassCastException ee) {
            log.addElement("key:" + path + "/" + x + "    Unknown Class Difference");
          } 
          diffObjects(vv, val, log, String.valueOf(path) + "/" + x, swapOldNew);
        } else {
          String val1 = "";
          String val2 = "";
          try {
            if (o1 != null && ((Vector)o1).size() > x)
              val1 = ((Vector)o1).elementAt(x).toString(); 
          } catch (ClassCastException ee) {
            log.addElement("key:" + path + "/" + x + "    Unknown Class Difference");
          } 
          try {
            if (o2 != null && ((Vector)o2).size() > x)
              val2 = ((Vector)o2).elementAt(x).toString(); 
          } catch (ClassCastException ee) {
            log.addElement("key:" + path + "/" + x + "    Unknown Class Difference");
          } 
          if (swapOldNew) {
            String t = val2;
            val2 = val1;
            val1 = t;
          } 
          if (!val1.equals(val2))
            log.addElement("key:" + path + "/" + x + "    Previous:" + val2 + " New:" + val1); 
        } 
      } 
    } else if (o2 == null) {
      log.addElement("key:" + path + "/    New");
    } 
  }
  
  public static String getMD5(InputStream in) throws Exception {
    MessageDigest m = MessageDigest.getInstance("MD5");
    byte[] b = new byte[1048576];
    int bytesRead = 0;
    while (bytesRead >= 0) {
      bytesRead = in.read(b);
      if (bytesRead >= 0)
        m.update(b, 0, bytesRead); 
    } 
    in.close();
    String s = (new BigInteger(1, m.digest())).toString(16).toLowerCase();
    while (s.length() < 32)
      s = "0" + s; 
    return s;
  }
  
  public static void getMD5(InputStream in, Vector md5s, boolean chunked, boolean forward, long length, long localSize) throws Exception {
    BufferedInputStream bin = new BufferedInputStream(in);
    bin.mark("CRUSHFTP_ENCRYPTED_kHBeMxiWj7Sb4PdqJ8".length() * 2);
    byte[] b = new byte["CRUSHFTP_ENCRYPTED_kHBeMxiWj7Sb4PdqJ8".length()];
    int bytesRead = bin.read(b);
    String header = "";
    if (bytesRead > 0)
      header = new String(b, 0, bytesRead, "UTF8"); 
    if (header.equals("CRUSHFTP_PGPChunkedStream:dBa3Em7W4N:") && chunked) {
      bin.skip("0                                        ".length());
      byte[] b1 = new byte[1];
      ByteArrayOutputStream clearBytes = new ByteArrayOutputStream();
      while (true) {
        int bytes = -1;
        clearBytes.reset();
        do {
          bytes = bin.read(b1);
          if (bytes <= 0)
            continue; 
          clearBytes.write(b1);
        } while (bytes >= 0 && b1[0] != 13);
        if (bytes < 0)
          break; 
        String[] segment = (new String(clearBytes.toByteArray(), "UTF8")).split(":");
        long pos = Integer.parseInt(segment[0].trim());
        int clearSize = Integer.parseInt(segment[1].trim());
        int chunkSize = Integer.parseInt(segment[2].trim());
        int paddingSize = Integer.parseInt(segment[3].trim());
        String md5Hash = segment[4].trim();
        bytes = 0;
        while (chunkSize > 0 && bytes >= 0) {
          byte[] chunk = new byte[chunkSize];
          bytes = bin.read(chunk);
          if (bytes >= 0)
            chunkSize -= bytes; 
        } 
        bytes = 0;
        while (paddingSize > 0 && bytes >= 0) {
          byte[] chunk = new byte[paddingSize];
          bytes = bin.read(chunk);
          if (bytes >= 0)
            paddingSize -= bytes; 
        } 
        md5s.addElement(String.valueOf(pos) + "-" + (pos + clearSize) + ":" + md5Hash);
      } 
      bin.close();
    } else {
      bin.reset();
      Tunnel2.getMd5s(bin, chunked, forward, length, md5s, new StringBuffer(), localSize);
      bin.close();
    } 
    in.close();
  }
  
  public static String parseSyncPart(String privs, String part) {
    if (privs.indexOf("(syncName") >= 0) {
      String key = "(SYNC" + part.toUpperCase();
      if (privs.toUpperCase().indexOf(key) < 0)
        return ""; 
      int pos = privs.toUpperCase().indexOf(key) + key.length() + 1;
      return privs.substring(pos, privs.indexOf(")", pos));
    } 
    return null;
  }
  
  public static void trackSync(String action, String path1, String path2, boolean isDir, long size, long modified, String root_dir, String privs, String clientid, String md5Str) throws Exception {
    if (privs.indexOf("(sync") >= 0 && !com.crushftp.client.Common.dmz_mode) {
      Log.log("SYNC", 2, "Track Sync:" + action + " path1=" + path1 + " path2=" + path2 + " clientid=" + clientid);
      String syncIDTemp = parseSyncPart(privs, "name").toUpperCase();
      if (path2 == null)
        path2 = path1; 
      String item_path1 = path1.substring(root_dir.length() - 1);
      String item_path2 = path2.substring(root_dir.length() - 1);
      if (action.equalsIgnoreCase("delete")) {
        SyncTools.addJournalEntry(syncIDTemp, item_path1, action, clientid, md5Str);
      } else if (action.equalsIgnoreCase("change")) {
        SyncTools.addJournalEntry(syncIDTemp, item_path1, action, clientid, md5Str);
      } else if (action.equalsIgnoreCase("rename")) {
        SyncTools.addJournalEntry(syncIDTemp, String.valueOf(item_path1) + ";" + item_path2, action, clientid, md5Str);
      } 
    } 
  }
  
  public static void publishPendingSyncs(Vector pendingSyncs) throws Exception {
    while (pendingSyncs.size() > 0) {
      Properties pendingSync = pendingSyncs.remove(0);
      trackSync(pendingSync.getProperty("action"), pendingSync.getProperty("path1"), pendingSync.getProperty("path2"), pendingSync.getProperty("isDir").equalsIgnoreCase("true"), Long.parseLong(pendingSync.getProperty("size")), Long.parseLong(pendingSync.getProperty("modified")), pendingSync.getProperty("root_dir"), pendingSync.getProperty("privs"), pendingSync.getProperty("clientid"), pendingSync.getProperty("md5Str"));
    } 
  }
  
  public static void trackPendingSync(Vector pendingSyncs, String action, String path1, String path2, boolean isDir, long size, long modified, String root_dir, String privs, String clientid, String md5Str) throws Exception {
    if (privs.indexOf("(sync") >= 0 && !com.crushftp.client.Common.dmz_mode) {
      Properties pendingSync = new Properties();
      pendingSync.put("action", action);
      if (path1 != null)
        pendingSync.put("path1", path1); 
      if (path2 != null)
        pendingSync.put("path2", path2); 
      pendingSync.put("isDir", (new StringBuffer(String.valueOf(isDir))).toString());
      pendingSync.put("size", (new StringBuffer(String.valueOf(size))).toString());
      pendingSync.put("modified", (new StringBuffer(String.valueOf(modified))).toString());
      pendingSync.put("root_dir", root_dir);
      pendingSync.put("privs", privs);
      if (clientid != null)
        pendingSync.put("clientid", clientid); 
      if (md5Str != null)
        pendingSync.put("md5Str", md5Str); 
      pendingSyncs.addElement(pendingSync);
    } 
  }
  
  public static void trackSyncRevision(GenericClient c, VRL vrl, String path, String root_dir, String privs, boolean renameMove, Properties info) throws Exception {
    if (privs.indexOf("(sync") >= 0 && !vrl.getName().equals(".DS_Store") && !com.crushftp.client.Common.dmz_mode) {
      if (path.startsWith(root_dir))
        path = path.substring(root_dir.length() - 1); 
      String revPath = parseSyncPart(privs, "revisionsPath");
      if (revPath.equals(""))
        return; 
      int revCount = Integer.parseInt(parseSyncPart(privs, "revisions"));
      if (revCount == 0)
        return; 
      if ((new File_U(String.valueOf(revPath) + path + "/" + revCount)).exists())
        recurseDelete((new File_U(String.valueOf(revPath) + path + "/" + revCount)).getCanonicalPath(), false); 
      for (int xx = revCount - 1; xx >= 0; xx--)
        (new File_U(String.valueOf(revPath) + path + "/" + xx)).renameTo(new File_U(String.valueOf(revPath) + path + "/" + (xx + 1))); 
      (new File_U(String.valueOf(revPath) + path + "/0/")).mkdirs();
      if (!(c instanceof FileClient))
        renameMove = false; 
      if (!renameMove || !c.rename(vrl.getPath(), String.valueOf(revPath) + path + "/0/" + vrl.getName()))
        if (c.stat(vrl.getPath()) != null)
          if (c instanceof S3CrushClient) {
            String rawXML = ((S3CrushClient)c).getRawXmlPath(path);
            recurseCopy_U(rawXML, (new File_U(String.valueOf(revPath) + path + "/0/" + vrl.getName())).getCanonicalPath(), false);
            (new File_U(rawXML)).delete();
          } else {
            recurseCopy_U(vrl.getPath(), (new File_U(String.valueOf(revPath) + path + "/0/" + vrl.getName())).getCanonicalPath(), false);
          }   
      if (info != null)
        writeXMLObject(String.valueOf(revPath) + path + "/0/" + "info.XML", info, "info"); 
      return;
    } 
  }
  
  public static Vector getSyncTableData(String syncIDTemp, long rid, String table, String clientid, String root_dir, VFS uVFS) throws IOException {
    return SyncTools.getSyncTableData(syncIDTemp.toUpperCase(), rid, table, clientid, root_dir, uVFS);
  }
  
  public static void buildPrivateKeyFile(String path) throws Exception {
    SecretKey key = KeyGenerator.getInstance("AES").generateKey();
    FileOutputStream out = new FileOutputStream(new File_S(path) + "CrushFTP.key");
    ObjectOutputStream s = new ObjectOutputStream(out);
    s.writeObject(key);
    s.flush();
    s.close();
  }
  
  private static final byte[] iv = new byte[] { 
      -114, 18, 57, -100, 7, 114, 111, 90, -114, 18, 
      57, -100, 7, 114, 111, 90 };
  
  static Class class$0;
  
  static Class class$1;
  
  static Class class$2;
  
  public static OutputStream getEncryptedStream(OutputStream out, String keyLocation, long streamPosition, boolean ascii) throws Exception {
    if (keyLocation.replace('\\', '/').endsWith("/")) {
      ObjectInputStream ois = null;
      if (!(new File_S(keyLocation)).exists())
        buildPrivateKeyFile(keyLocation); 
      ois = new ObjectInputStream(new FileInputStream(new File_S(String.valueOf(keyLocation) + "CrushFTP.key")));
      SecretKey key = (SecretKey)ois.readObject();
      ois.close();
      AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);
      Cipher ecipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      ecipher.init(1, key, paramSpec);
      if (streamPosition == 0L)
        out.write("CRUSHFTP_ENCRYPTED_kHBeMxiWj7Sb4PdqJ80                                        ".getBytes("UTF8")); 
      return new BufferedOutputStream(new CipherOutputStream(out, ecipher));
    } 
    if (streamPosition > 0L)
      throw new Exception("Can't resume encrypted PGP files."); 
    if (pgp == null)
      pgp = new PGPLib(); 
    pgp.setUseExpiredKeys(true);
    ByteArrayOutputStream baos_key = new ByteArrayOutputStream();
    boolean pbe = false;
    if (keyLocation.toLowerCase().startsWith("password:")) {
      pbe = true;
    } else {
      if (keyLocation.indexOf(":") < 0 || keyLocation.indexOf(":") < 3)
        keyLocation = "FILE://" + keyLocation; 
      VRL key_vrl = new VRL(keyLocation);
      GenericClient c_key = com.crushftp.client.Common.getClient(getBaseUrl(key_vrl.toString()), "CrushFTP", new Vector());
      c_key.login(key_vrl.getUsername(), key_vrl.getPassword(), "");
      com.crushftp.client.Common.streamCopier(null, null, c_key.download(key_vrl.getPath(), 0L, -1L, true), baos_key, false, true, true);
      c_key.logout();
    } 
    String keyLocationF = keyLocation;
    boolean pbeF = pbe;
    Properties socks = getConnectedSockets();
    Socket sock1 = (Socket)socks.remove("sock1");
    Properties status = new Properties();
    Worker.startWorker(new Runnable(socks, baos_key, ascii, pbeF, keyLocationF, out, status) {
          private final Properties val$socks;
          
          private final ByteArrayOutputStream val$baos_key;
          
          private final boolean val$ascii;
          
          private final boolean val$pbeF;
          
          private final String val$keyLocationF;
          
          private final OutputStream val$out;
          
          private final Properties val$status;
          
          public void run() {
            try {
              Socket sock2 = (Socket)this.val$socks.remove("sock2");
              ByteArrayInputStream bytesIn = new ByteArrayInputStream(this.val$baos_key.toByteArray());
              Common.pgp.setCompression("UNCOMPRESSED");
              if (this.val$ascii)
                Common.pgp.setAsciiVersionHeader("CRUSHFTP#                                        "); 
              if (this.val$pbeF) {
                Common.pgp.encryptStreamPBE(sock2.getInputStream(), "", this.val$keyLocationF.substring(this.val$keyLocationF.indexOf(":") + 1), this.val$out, this.val$ascii, false);
              } else {
                Common.pgp.encryptStream(sock2.getInputStream(), "", bytesIn, this.val$out, this.val$ascii, false);
              } 
              bytesIn.close();
              this.val$status.put("status", "SUCCESS");
            } catch (Exception e) {
              Log.log("SERVER", 1, e);
              this.val$status.put("error", e);
              this.val$status.put("status", "ERROR");
            } 
          }
        }String.valueOf(Thread.currentThread().getName()) + ":PGP Encrypt Streamer");
    return new OutputStreamCloser(sock1.getOutputStream(), status, null, null, !ascii, ascii, out);
  }
  
  public static InputStream getDecryptedStream(InputStream in, String oldKeyLocation, String keyLocation, String pass) throws Exception {
    BufferedInputStream bin = new BufferedInputStream(in);
    bin.mark("CRUSHFTP_ENCRYPTED_kHBeMxiWj7Sb4PdqJ8".length() * 2);
    byte[] b_header = new byte["CRUSHFTP_ENCRYPTED_kHBeMxiWj7Sb4PdqJ8".length()];
    int bytesRead = bin.read(b_header);
    byte[] b_stream_header = new byte[6];
    (new ByteArrayInputStream(b_header)).read(b_stream_header);
    String header = "";
    if (bytesRead > 0)
      header = new String(b_header, 0, bytesRead, "UTF8"); 
    if (!oldKeyLocation.equals("") && header.equals("CRUSHFTP_ENCRYPTED_kHBeMxiWj7Sb4PdqJ8")) {
      ObjectInputStream ois = null;
      ois = new ObjectInputStream(new FileInputStream(new File_S(String.valueOf(oldKeyLocation) + "CrushFTP.key")));
      SecretKey key = (SecretKey)ois.readObject();
      ois.close();
      AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);
      Cipher dcipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      dcipher.init(2, key, paramSpec);
      bin.skip("0                                        ".length());
      return new BufferedInputStream(new CipherInputStream(bin, dcipher));
    } 
    if (!keyLocation.equals("") && (header.toUpperCase().startsWith("-----BEGIN PGP MESSAGE-----") || (new PGPInspectLib()).isPGPData(b_stream_header))) {
      bin.reset();
      if (pgp == null)
        pgp = new PGPLib(); 
      pgp.setUseExpiredKeys(true);
      ByteArrayOutputStream baos_key = new ByteArrayOutputStream();
      boolean pbe = false;
      if (keyLocation.toLowerCase().startsWith("password:")) {
        pbe = true;
      } else {
        if (keyLocation.indexOf(":") < 0 || keyLocation.indexOf(":") < 3)
          keyLocation = "FILE://" + keyLocation; 
        VRL key_vrl = new VRL(keyLocation);
        GenericClient c_key = com.crushftp.client.Common.getClient(getBaseUrl(key_vrl.toString()), "CrushFTP", new Vector());
        c_key.login(key_vrl.getUsername(), key_vrl.getPassword(), "");
        com.crushftp.client.Common.streamCopier(null, null, c_key.download(key_vrl.getPath(), 0L, -1L, true), baos_key, false, true, true);
        c_key.logout();
      } 
      String keyLocationF = keyLocation;
      boolean pbeF = pbe;
      Properties socks = getConnectedSockets();
      Socket sock2 = (Socket)socks.remove("sock2");
      Worker.startWorker(new Runnable(socks, baos_key, pbeF, bin, keyLocationF, pass) {
            private final Properties val$socks;
            
            private final ByteArrayOutputStream val$baos_key;
            
            private final boolean val$pbeF;
            
            private final BufferedInputStream val$bin;
            
            private final String val$keyLocationF;
            
            private final String val$pass;
            
            public void run() {
              OutputStream out4 = null;
              try {
                Socket sock1 = (Socket)this.val$socks.remove("sock1");
                ByteArrayInputStream bytesIn = new ByteArrayInputStream(this.val$baos_key.toByteArray());
                Common.pgp.setCompression("UNCOMPRESSED");
                out4 = sock1.getOutputStream();
                if (this.val$pbeF) {
                  Common.pgp.decryptStreamPBE(this.val$bin, this.val$keyLocationF.substring(this.val$keyLocationF.indexOf(":") + 1), out4);
                } else {
                  Common.pgp.decryptStream(this.val$bin, bytesIn, ServerStatus.thisObj.common_code.decode_pass(this.val$pass), out4);
                } 
                bytesIn.close();
              } catch (Exception e) {
                Log.log("SERVER", 1, e);
              } 
              try {
                if (out4 != null)
                  out4.close(); 
              } catch (IOException iOException) {}
            }
          }String.valueOf(Thread.currentThread().getName()) + ":PGP Decrypt Streamer");
      return sock2.getInputStream();
    } 
    bin.reset();
    return bin;
  }
  
  public static Properties urlDecodePost(Properties p) {
    Enumeration keys = p.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      if (p.get(key) instanceof String)
        p.put(key, url_decode(p.get(key).toString().replace('+', ' '))); 
    } 
    return p;
  }
  
  public static void verifyOSXVolumeMounted(String url) {
    try {
      if (machine_is_x()) {
        VRL vrl = new VRL(url);
        if (vrl.getProtocol().equalsIgnoreCase("FILE")) {
          String vol_path = (new VRL(url)).getPath();
          if (vol_path.startsWith("/Volumes/")) {
            vol_path = vol_path.substring(0, vol_path.indexOf("/", "/Volumes/".length() + 1));
            for (int x = 0; !(new File_S(vol_path)).exists() && x < 30; x++)
              Thread.sleep(1000L); 
            if (!(new File_S(vol_path)).exists())
              throw new Exception("Volume not mounted:" + vol_path); 
          } 
        } 
      } 
    } catch (Exception e) {
      throw new RuntimeException(e);
    } 
  }
  
  public static String dots(String s) {
    return com.crushftp.client.Common.dots(s);
  }
  
  public static String getBaseUrl(String s) {
    String s_original = s;
    if (s.indexOf(":") < 0) {
      s = "/";
    } else if (s.indexOf("@") > 0 && s.indexOf(":") != s.lastIndexOf(":") && !s.toLowerCase().startsWith("file:")) {
      if (s.indexOf("@") != s.lastIndexOf("@")) {
        s = s.substring(0, s.indexOf("/", s.indexOf("@")) + 1);
      } else if (s.lastIndexOf("@") > s.lastIndexOf(":")) {
        s = s.substring(0, s.indexOf("/", s.lastIndexOf("@")) + 1);
      } else if (s.length() > 8 && s.indexOf(":", 8) != s.lastIndexOf(":")) {
        s = s.substring(0, s.indexOf("/", s.indexOf(":", 8)) + 1);
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
    if (s.toUpperCase().startsWith("S3:/")) {
      String tmp_path = (new VRL(s_original)).getPath();
      if (tmp_path.length() > 1) {
        tmp_path = tmp_path.substring(1, tmp_path.indexOf("/", 1) + 1);
        s = String.valueOf(s) + tmp_path;
      } 
    } 
    if (s.toUpperCase().startsWith("AZURE:/"))
      s = s_original; 
    return s;
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
    com.crushftp.client.Common.streamCopier(sock1, sock2, in, out, async, closeInput, closeOutput);
  }
  
  public static String send_mail(String server_ip, String to_user, String cc_user, String bcc_user, String from_user, String subject, String body, String smtp_server, String smtp_user, String smtp_pass, boolean smtp_ssl, boolean html, File_B[] attachments) {
    return send_mail(server_ip, to_user, cc_user, bcc_user, from_user, null, subject, body, smtp_server, smtp_user, smtp_pass, smtp_ssl, html, attachments);
  }
  
  public static String send_mail(String server_ip, String to_user, String cc_user, String bcc_user, String from_user, String reply_to_user, String subject, String body, String smtp_server, String smtp_user, String smtp_pass, boolean smtp_ssl, boolean html, File_B[] attachments) {
    try {
      return Mailer.send_mail(server_ip, to_user, cc_user, bcc_user, from_user, reply_to_user, subject, body, smtp_server, smtp_user, smtp_pass, smtp_ssl, html, attachments);
    } catch (Throwable e) {
      Log.log("SMTP", 1, e);
      return "ERROR:" + e.toString();
    } 
  }
  
  public static String send_mail(String server_ip, String to_user, String cc_user, String bcc_user, String from_user, String subject, String body, String smtp_server, String smtp_user, String smtp_pass, boolean smtp_ssl, boolean html) {
    return send_mail(server_ip, to_user, cc_user, bcc_user, from_user, null, subject, body, smtp_server, smtp_user, smtp_pass, smtp_ssl, html, null);
  }
  
  public static String send_mail(String server_ip, String to_user, String cc_user, String bcc_user, String from_user, String reply_to_user, String subject, String body, String smtp_server, String smtp_user, String smtp_pass, boolean smtp_ssl, boolean html) {
    return send_mail(server_ip, to_user, cc_user, bcc_user, from_user, reply_to_user, subject, body, smtp_server, smtp_user, smtp_pass, smtp_ssl, html, null);
  }
  
  public static boolean compare_with_hack_username(String username, String hack_username) {
    username = username.trim();
    hack_username = hack_username.trim();
    if (username.equalsIgnoreCase("anonymous"))
      return false; 
    if (hack_username.indexOf('*') >= 0 || hack_username.indexOf('!') >= 0 || hack_username.indexOf('?') >= 0 || hack_username.indexOf('.') >= 0 || hack_username.indexOf('$') >= 0 || hack_username.indexOf('^') >= 0)
      if (com.crushftp.client.Common.do_search(hack_username.toUpperCase(), username.toUpperCase(), false, 0))
        return true;  
    if (username.equalsIgnoreCase(hack_username))
      return true; 
    return false;
  }
  
  public static void send_change_pass_email(SessionCrush session) {
    Properties template = get_email_template("Change Pass Email");
    if (template != null) {
      Properties email_info = new Properties();
      email_info.put("server", ServerStatus.SG("smtp_server"));
      email_info.put("user", ServerStatus.SG("smtp_user"));
      email_info.put("pass", ServerStatus.SG("smtp_pass"));
      email_info.put("ssl", ServerStatus.SG("smtp_ssl"));
      email_info.put("html", ServerStatus.SG("smtp_html"));
      email_info.put("from", ServerStatus.thisObj.change_vars_to_values(template.getProperty("emailFrom"), session));
      email_info.put("reply_to", ServerStatus.thisObj.change_vars_to_values(template.getProperty("emailReplyTo"), session));
      email_info.put("to", session.SG("email"));
      email_info.put("cc", ServerStatus.thisObj.change_vars_to_values(template.getProperty("emailCC"), session));
      email_info.put("bcc", ServerStatus.thisObj.change_vars_to_values(template.getProperty("emailBCC"), session));
      email_info.put("subject", ServerStatus.thisObj.change_vars_to_values(template.getProperty("emailSubject"), session));
      email_info.put("body", ServerStatus.thisObj.change_vars_to_values(template.getProperty("emailBody"), session));
      ServerStatus.thisObj.sendEmail(email_info);
    } 
  }
  
  public static Properties get_email_template(String template_name) {
    Vector email_templates = ServerStatus.VG("email_templates");
    if (email_templates != null)
      for (int x = 0; x < email_templates.size(); x++) {
        Properties email_template = email_templates.elementAt(x);
        if (email_template.getProperty("name", "").replace('+', ' ').equals(template_name))
          return email_template; 
      }  
    return null;
  }
  
  public static void send_otp_for_auth_sms(String otp_to, String otp_token) throws Exception {
    String otp_url = ServerStatus.SG("otp_url");
    otp_url = replace_str(otp_url, "{otp_from}", url_encode(ServerStatus.SG("otp_from")));
    otp_url = replace_str(otp_url, "{otp_to}", url_encode(otp_to));
    otp_url = replace_str(otp_url, "{otp_token}", url_encode(otp_token));
    otp_url = replace_str(otp_url, "{otp_username}", url_encode(ServerStatus.SG("otp_username")));
    otp_url = replace_str(otp_url, "{otp_password}", url_encode(ServerStatus.SG("otp_password")));
    otp_url = replace_str(otp_url, "{otp_extra1}", url_encode(ServerStatus.SG("otp_extra1")));
    otp_url = replace_str(otp_url, "{otp_extra2}", url_encode(ServerStatus.SG("otp_extra2")));
    otp_url = ServerStatus.change_vars_to_values_static(otp_url, null, null, null);
    VRL otp_vrl = new VRL(otp_url);
    HttpURLConnection urlc = (HttpURLConnection)(new URL(otp_url)).openConnection();
    urlc.setDoOutput(true);
    if (otp_vrl.getUsername() != null && otp_vrl.getUsername().trim().length() > 0) {
      String basicAuth = "Basic " + new String(Base64.encodeBytes((String.valueOf(otp_vrl.getUsername()) + ":" + otp_vrl.getPassword()).getBytes()));
      urlc.setRequestProperty("Authorization", basicAuth);
    } 
    urlc.setRequestMethod(ServerStatus.SG("otp_url_verb"));
    String post = ServerStatus.SG("otp_post");
    post = replace_str(post, "{otp_from}", url_encode(ServerStatus.SG("otp_from")));
    post = replace_str(post, "{otp_to}", url_encode(otp_to));
    post = replace_str(post, "{otp_token}", url_encode(otp_token));
    post = replace_str(post, "{otp_username}", url_encode(ServerStatus.SG("otp_username")));
    post = replace_str(post, "{otp_password}", url_encode(ServerStatus.SG("otp_password")));
    post = replace_str(post, "{otp_extra1}", url_encode(ServerStatus.SG("otp_extra1")));
    post = replace_str(post, "{otp_extra2}", url_encode(ServerStatus.SG("otp_extra2")));
    post = ServerStatus.change_vars_to_values_static(post, null, null, null);
    if (ServerStatus.SG("otp_url_verb").equalsIgnoreCase("POST")) {
      OutputStream out = urlc.getOutputStream();
      out.write(post.getBytes("UTF8"));
      out.close();
    } 
    if (urlc.getResponseCode() > 299) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      streamCopier(urlc.getInputStream(), baos, false, true, true);
      String result = new String(baos.toByteArray());
      urlc.disconnect();
      result = String.valueOf(urlc.getResponseCode()) + ":" + result;
      Log.log("LOGIN", 1, "CHALLENGE_OTP : Error :" + result);
      throw new Exception(result);
    } 
    urlc.disconnect();
  }
  
  public static Comparator get_file_last_modified_Comparator() {
    return new null.counts();
  }
  
  public static Vector get_urls_from_VFS(Properties virtual) {
    Vector urls = new Vector();
    Enumeration e = virtual.propertyNames();
    while (e.hasMoreElements()) {
      String key = (String)e.nextElement();
      if (key.equals("vfs_permissions_object"))
        continue; 
      Properties p = (Properties)virtual.get(key);
      if (p.containsKey("vItems")) {
        Vector vItems = (Vector)p.get("vItems");
        if (vItems != null && !vItems.isEmpty()) {
          Properties pp = vItems.get(0);
          urls.add(VRL.fileFix(pp.getProperty("url")));
        } 
      } 
    } 
    return urls;
  }
  
  public static File_B[] convert_files_to_files_both(File[] files) {
    File_B[] items = new File_B[files.length];
    for (int x = 0; x < files.length; x++)
      items[x] = new File_B(files[x]); 
    return items;
  }
  
  public static Properties get_vfs_posix_settings(String privs, boolean is_file) {
    Properties vfs_posix_settings = new Properties();
    String posix = "(posix:";
    if (!is_file)
      posix = "(dir_posix:"; 
    if (privs.indexOf(posix) > 0) {
      String[] vfs_posix = (String[])null;
      int start_index = privs.indexOf(posix) + posix.length();
      int end_index = privs.indexOf(")", start_index);
      vfs_posix = privs.substring(start_index, end_index).split(":");
      if (vfs_posix != null && vfs_posix.length == 3) {
        vfs_posix_settings.put("vfs_privs", vfs_posix[0]);
        vfs_posix_settings.put("vfs_owner", vfs_posix[1]);
        vfs_posix_settings.put("vfs_group", vfs_posix[2]);
      } 
    } 
    return vfs_posix_settings;
  }
  
  public void set_defaults(Properties default_settings) {
    default_settings.put("rid", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
    default_settings.put("listing_buffer_count", "500");
    default_settings.put("listing_multithreaded", "false");
    default_settings.put("registration_name", "crush");
    default_settings.put("registration_email", "ftp");
    default_settings.put("registration_code", "crushftp:(MAX=5)(V=5)");
    default_settings.put("ftp_welcome_message", "Welcome to CrushFTP!");
    default_settings.put("server_start_message", "CrushFTP Server Ready!");
    default_settings.put("ssh_comments", "http://www.crushftp.com/");
    default_settings.put("http_server_header", "CrushFTP HTTP Server");
    default_settings.put("username_uppercase", "false");
    default_settings.put("allow_session_caching", "true");
    default_settings.put("allow_session_caching_on_exit", "true");
    default_settings.put("replicated_users_sync", "false");
    default_settings.put("tls_version", "SSLv2Hello,TLSv1,TLSv1.1,TLSv1.2");
    default_settings.put("relaxed_event_grouping", "false");
    default_settings.put("syslog_protocol", "udp");
    default_settings.put("syslog_host", "127.0.0.1");
    default_settings.put("syslog_port", "1514");
    default_settings.put("check_all_recursive_deletes", "false");
    default_settings.put("acl_cache_timeout", "60");
    default_settings.put("max_threads", "2000");
    default_settings.put("allow_local_ip_pasv", "true");
    default_settings.put("allow_local_ip_pasv_any", "false");
    default_settings.put("Access-Control-Allow-Origin", "");
    default_settings.put("X-Frame-Options", "SAMEORIGIN");
    default_settings.put("http_session_timeout", "60");
    default_settings.put("log_debug_level", "0");
    default_settings.put("domain_cookie", "");
    default_settings.put("block_access", "");
    default_settings.put("count_dir_items", "false");
    default_settings.put("list_zip_app", "true");
    default_settings.put("zip_icon_preview_allowed", "false");
    default_settings.put("allow_auto_save", "true");
    default_settings.put("force_ipv4", "false");
    default_settings.put("allow_zipstream", "true");
    default_settings.put("allow_filetree", "true");
    default_settings.put("create_home_folder", "false");
    default_settings.put("allow_x_forwarded_host", "true");
    default_settings.put("recent_user_count", "100");
    default_settings.put("tunnel_ram_cache", "128");
    default_settings.put("s3_buffer", "5");
    default_settings.put("track_user_md4_hashes", "false");
    default_settings.put("jailproxy", "true");
    default_settings.put("learning_proxy", "false");
    default_settings.put("rfc_proxy", "false");
    default_settings.put("event_thread_timeout", "60");
    default_settings.put("exif_keywords", "false");
    default_settings.put("trusted_ip_parts", "4");
    default_settings.put("disable_stats", "false");
    default_settings.put("sync_history_days", "30");
    default_settings.put("fix_slashes", "true");
    default_settings.put("smtp_helo_ip", "");
    default_settings.put("zip64", "false");
    default_settings.put("zip64_always", "false");
    default_settings.put("mdtm_gmt", "false");
    default_settings.put("instant_chmod_chown_chgrp", "false");
    default_settings.put("7_token_proxy", "false");
    default_settings.put("command_flush_interval", "10");
    default_settings.put("sort_listings", "false");
    default_settings.put("case_sensitive_list_search", "false");
    default_settings.put("change_remote_password", "true");
    default_settings.put("expire_emailed_passwords", "false");
    default_settings.put("ignore_web_anonymous", "false");
    default_settings.put("ignore_web_anonymous_proxy", "false");
    default_settings.put("test_proxy_dir", "true");
    default_settings.put("lowercase_usernames", "false");
    default_settings.put("deny_secure_active_mode", "false");
    default_settings.put("event_empty_files", "true");
    default_settings.put("allow_nlst_empty", "true");
    default_settings.put("ssh_debug_logging", "");
    default_settings.put("ssh_close_all", "false");
    default_settings.put("socketpool_timeout", "20");
    default_settings.put("log_roll_time", "00:00");
    default_settings.put("zipCompressionLevel", "Best");
    default_settings.put("log_transfer_speeds", "true");
    default_settings.put("smtp_subject_utf8", "true");
    default_settings.put("log_location", System.getProperty("crushftp.log", "./CrushFTP.log"));
    default_settings.put("user_log_location", String.valueOf(all_but_last(System.getProperty("crushftp.log", "./CrushFTP.log"))) + "logs/session_logs/");
    default_settings.put("logging_provider", "");
    default_settings.put("extended_logging", "false");
    default_settings.put("temp_accounts_path", "./TempAccounts/");
    if (OSXApp())
      default_settings.put("temp_accounts_path", String.valueOf(System.getProperty("crushftp.home")) + "../../../../TempAccounts/"); 
    default_settings.put("previews_path", System.getProperty("crushftp.home"));
    if (OSXApp())
      default_settings.put("previews_path", String.valueOf(System.getProperty("crushftp.home")) + "../../../../"); 
    default_settings.put("temp_accounts_length", "4");
    default_settings.put("char_encoding", "UTF-8");
    default_settings.put("deny_localhost_admin", "false");
    default_settings.put("line_separator_crlf", "false");
    default_settings.put("file_transfer_mode", "BINARY");
    default_settings.put("fileEncryption", "false");
    default_settings.put("fileDecryption", "false");
    default_settings.put("fileEncryptionKey", "");
    default_settings.put("filePublicEncryptionKey", "");
    default_settings.put("fileDecryptionKey", "");
    default_settings.put("fileDecryptionKeyPass", "");
    default_settings.put("password_reset_message_browser", "If your account was found, an email has been sent to you.  Follow the email's instructions.  The email's link will expire in 10 minutes.");
    default_settings.put("password_reset_message_browser_bad", "If your account was found, an email has been sent to you.  Follow the email's instructions.  The email's link will expire in 10 minutes.");
    default_settings.put("password_reset_subject", "Password Reset");
    default_settings.put("password_reset_message", "<html><body>Your password reset can be completed by clicking the following URL.<br><br><a href=\"{url}\">{url}</a> <br><br><b>This link will expire in 10 minutes.</b></body></html>");
    default_settings.put("proxyActivePorts", "1025-65535");
    default_settings.put("disable_dir_filter", "true");
    default_settings.put("event_reuse", "true");
    default_settings.put("event_asynch", "false");
    default_settings.put("event_batching", "true");
    default_settings.put("needClientAuth", "false");
    default_settings.put("client_cert_auth", "true");
    default_settings.put("epsveprt", "true");
    default_settings.put("allow_mlst", "true");
    default_settings.put("cert_path", "builtin");
    default_settings.put("globalKeystorePass", "crushftp");
    default_settings.put("globalKeystoreCertPass", "crushftp");
    default_settings.put("disabled_ciphers", "(TLS_RSA_WITH_AES_128_CBC_SHA)(TLS_RSA_WITH_AES_256_CBC_SHA)(TLS_DHE_RSA_WITH_AES_128_CBC_SHA)(TLS_DHE_RSA_WITH_AES_256_CBC_SHA)(SSL_RSA_WITH_3DES_EDE_CBC_SHA)(SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA)(SSL_RSA_WITH_DES_CBC_SHA)(SSL_DHE_RSA_WITH_DES_CBC_SHA)(SSL_RSA_EXPORT_WITH_RC4_40_MD5)(SSL_RSA_EXPORT_WITH_DES40_CBC_SHA)(SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA)(SSL_RSA_WITH_NULL_MD5)(SSL_RSA_WITH_NULL_SHA)(TLS_ECDH_ECDSA_WITH_NULL_SHA)(TLS_ECDH_RSA_WITH_NULL_SHA)(TLS_ECDHE_ECDSA_WITH_NULL_SHA)(TLS_ECDHE_RSA_WITH_NULL_SHA)(SSL_DH_anon_WITH_RC4_128_MD5)(TLS_DH_anon_WITH_AES_128_CBC_SHA)(TLS_DH_anon_WITH_AES_256_CBC_SHA)(SSL_DH_anon_WITH_3DES_EDE_CBC_SHA)(SSL_DH_anon_WITH_DES_CBC_SHA)(TLS_ECDH_anon_WITH_RC4_128_SHA)(TLS_ECDH_anon_WITH_AES_128_CBC_SHA)(TLS_ECDH_anon_WITH_AES_256_CBC_SHA)(TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA)(SSL_DH_anon_EXPORT_WITH_RC4_40_MD5)(SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA)(TLS_ECDH_anon_WITH_NULL_SHA)");
    default_settings.put("user_backup_count", "100");
    default_settings.put("proxyDownloadRepository", "./");
    default_settings.put("proxyUploadRepository", "./");
    default_settings.put("proxyKeepDownloads", "false");
    default_settings.put("proxyKeepUploads", "false");
    default_settings.put("proxy_socket_mode", "passive");
    default_settings.put("invalid_usernames_seconds", "60");
    default_settings.put("default_system_owner", "");
    default_settings.put("default_system_group", "");
    default_settings.put("default_logo", "logo.png");
    default_settings.put("default_title", "CrushFTP WebInterface");
    default_settings.put("webFooterText", "");
    default_settings.put("web404Text", "The selected resource was not found.");
    default_settings.put("emailReminderSubjectText", LOC.G("Password Reminder"));
    default_settings.put("emailReminderBodyText", String.valueOf(LOC.G("Your password is : ")) + "%user_pass%" + "\r\n\r\n" + LOC.G("Requested from IP:") + "%user_ip%");
    Vector email_templates = new Vector();
    Properties email_template = new Properties();
    email_templates.addElement(email_template);
    email_template.put("emailBody", "Your username is:{user_name}<br>Your password is:{user_pass}<br>");
    email_template.put("emailSubject", "New Account Information");
    email_template.put("name", "New Account");
    default_settings.put("email_templates", email_templates);
    default_settings.put("reportSchedules", new Vector());
    default_settings.put("miniURLs", new Vector());
    default_settings.put("miniURLs_dmz", new Vector());
    default_settings.put("proxyRules", new Vector());
    default_settings.put("alerts", new Vector());
    default_settings.put("tunnels", new Vector());
    default_settings.put("tunnels_dmz", new Vector());
    default_settings.put("monitored_folders", new Vector());
    default_settings.put("sqlItems", new Properties());
    default_settings.put("customData", new Properties());
    default_settings.put("externalSqlUsers", "false");
    default_settings.put("sql_prefix", "");
    default_settings.put("xmlUsers", "true");
    default_settings.put("site_ack", "true");
    default_settings.put("version", "1.0");
    default_settings.put("total_server_bytes_transfered", "0K");
    default_settings.put("total_server_bytes_sent", "0K");
    default_settings.put("total_server_bytes_sent_long", "0");
    default_settings.put("total_server_bytes_received", "0K");
    default_settings.put("total_server_bytes_received_long", "0");
    default_settings.put("failed_logins", "0");
    default_settings.put("successful_logins", "0");
    default_settings.put("uploaded_files", "0");
    default_settings.put("downloaded_files", "0");
    default_settings.put("roll_daily_logs", "true");
    default_settings.put("last_login_date_time", "<none>");
    default_settings.put("last_login_ip", "<none>");
    default_settings.put("last_login_user", "<none>");
    default_settings.put("discovered_ip", "0.0.0.0");
    default_settings.put("auto_ip_discovery", "true");
    default_settings.put("discover_ip_refresh", "60");
    default_settings.put("beep_connect", "false");
    default_settings.put("slow_directory_scanners", "true");
    default_settings.put("sftp_recurse_delete", "false");
    default_settings.put("disable_referer_cookie", "false");
    default_settings.put("disable_mdtm_modifications", "false");
    default_settings.put("delete_partial_uploads", "false");
    default_settings.put("password_encryption", "DES");
    default_settings.put("newversion", "true");
    default_settings.put("lsla2", "false");
    default_settings.put("posix", (new StringBuffer(String.valueOf(!machine_is_windows()))).toString());
    default_settings.put("allow_directory_caching", "false");
    default_settings.put("deny_fxp", "false");
    default_settings.put("deny_reserved_ports", "true");
    default_settings.put("allow_gzip", "true");
    default_settings.put("display_alt_logo", "false");
    default_settings.put("hide_email_password", "false");
    Vector preview_configs = new Vector();
    default_settings.put("preview_configs", preview_configs);
    Vector server_groups = new Vector();
    server_groups.addElement("MainUsers");
    default_settings.put("server_groups", server_groups);
    Vector server_list = new Vector();
    Properties server_item = new Properties();
    server_item.put("serverType", "FTP");
    server_item.put("ip", "lookup");
    server_item.put("port", "21");
    server_item.put("ftp_aware_router", "true");
    server_item.put("require_encryption", "false");
    server_item.put("explicit_ssl", "true");
    server_item.put("explicit_tls", "true");
    server_item.put("linkedServer", "MainUsers");
    server_list.addElement(server_item);
    server_item = new Properties();
    server_item.put("serverType", "HTTP");
    server_item.put("ip", "lookup");
    server_item.put("port", "8080");
    server_item.put("new_http", "true");
    server_item.put("require_encryption", "false");
    server_item.put("explicit_ssl", "true");
    server_item.put("explicit_tls", "true");
    server_item.put("linkedServer", "MainUsers");
    server_list.addElement(server_item);
    server_item = new Properties();
    server_item.put("serverType", "HTTP");
    server_item.put("ip", "lookup");
    server_item.put("port", "9090");
    server_item.put("new_http", "true");
    server_item.put("require_encryption", "false");
    server_item.put("explicit_ssl", "true");
    server_item.put("explicit_tls", "true");
    server_item.put("linkedServer", "MainUsers");
    server_list.addElement(server_item);
    server_item = new Properties();
    server_item.put("serverType", "HTTPS");
    server_item.put("ip", "lookup");
    server_item.put("port", "443");
    server_item.put("new_http", "true");
    server_item.put("require_encryption", "false");
    server_item.put("explicit_ssl", "true");
    server_item.put("explicit_tls", "true");
    server_item.put("linkedServer", "MainUsers");
    server_list.addElement(server_item);
    server_item = new Properties();
    server_item.put("serverType", "SFTP");
    server_item.put("ip", "lookup");
    server_item.put("port", "2222");
    server_item.put("ssh_cipher_list", "aes128-ctr,aes192-ctr,aes256-ctr,3des-ctr,3des-cbc,blowfish-cbc,arcfour,arcfour128,arcfour256,aes128-gcm@openssh.com,aes256-gcm@openssh.com");
    server_item.put("key_exchanges", "curve25519-sha256@libssh.org,diffie-hellman-group-exchange-sha256,diffie-hellman-group18-sha512,diffie-hellman-group17-sha512,diffie-hellman-group16-sha512,diffie-hellman-group15-sha512,diffie-hellman-group14-sha256,diffie-hellman-group14-sha1,ecdh-sha2-nistp256,ecdh-sha2-nistp384,ecdh-sha2-nistp521,diffie-hellman-group-exchange-sha1,rsa2048-sha256,rsa1024-sha1");
    server_item.put("ssh_mac_list", "hmac-sha256,hmac-sha2-256,hmac-sha256@ssh.com,hmac-sha2-256-etm@openssh.com,hmac-sha2-256-96,hmac-sha512,hmac-sha2-512,hmac-sha512@ssh.com,hmac-sha2-512-etm@openssh.com,hmac-sha2-512-96,hmac-sha1,hmac-sha1-etm@openssh.com,hmac-sha1-96,hmac-ripemd160,hmac-ripemd160@openssh.com,hmac-ripemd160-etm@openssh.com,hmac-md5,hmac-md5-etm@openssh.com,hmac-md5-96");
    server_item.put("ftp_welcome_message", "");
    server_item.put("ssh_rsa_key", String.valueOf(System.getProperty("crushftp.prefs")) + "ssh_host_rsa_key");
    server_item.put("ssh_dsa_key", String.valueOf(System.getProperty("crushftp.prefs")) + "ssh_host_dsa_key");
    server_item.put("ssh_dsa_enabled", "true");
    server_item.put("ssh_rsa_enabled", "true");
    server_item.put("ssh_transfer_threads", "2");
    server_item.put("ssh_accept_threads", "2");
    server_item.put("ssh_connect_threads", "2");
    server_item.put("ssh_require_password", "false");
    server_item.put("ssh_require_publickey", "false");
    server_item.put("ssh_text_encoding", "UTF8");
    server_item.put("ssh_session_timeout", "300");
    server_item.put("linkedServer", "MainUsers");
    server_list.addElement(server_item);
    default_settings.put("server_list", server_list);
    default_settings.put("plugins", new Vector());
    default_settings.put("CustomForms", new Vector());
    default_settings.put("stats_min", "10");
    default_settings.put("stats_transfer_days", "60");
    default_settings.put("stats_session_days", "90");
    default_settings.put("last_download_wait", "0");
    default_settings.put("last_upload_wait", "0");
    default_settings.put("server_download_queue_size", "0");
    default_settings.put("server_upload_queue_size", "0");
    default_settings.put("server_download_queue_size_max", "0");
    default_settings.put("server_upload_queue_size_max", "0");
    default_settings.put("user_log_buffer", "500");
    default_settings.put("filter_log_text", "");
    default_settings.put("recycle_path", "");
    default_settings.put("recycle", "false");
    default_settings.put("max_users", "5");
    default_settings.put("max_max_users", "5");
    default_settings.put("max_server_download_speed", "0");
    default_settings.put("max_server_upload_speed", "0");
    default_settings.put("bandwidth_immune_ips", "");
    default_settings.put("blank_passwords", "false");
    default_settings.put("smtp_server", "");
    default_settings.put("smtp_ssl", "false");
    default_settings.put("smtp_html", "true");
    default_settings.put("smtp_report_html", "true");
    default_settings.put("smtp_user", "");
    default_settings.put("smtp_pass", "");
    default_settings.put("smtp_from", "");
    default_settings.put("filename_filters_str", "");
    Properties ip_data = new Properties();
    ip_data.put("type", "A");
    ip_data.put("start_ip", "0.0.0.0");
    ip_data.put("stop_ip", "255.255.255.255");
    Vector ip_vec = new Vector();
    ip_vec.addElement(ip_data);
    default_settings.put("ip_restrictions", ip_vec);
    Properties login_page_data = new Properties();
    login_page_data.put("domain", "*");
    login_page_data.put("page", "login.html");
    Vector login_page_vec = new Vector();
    login_page_vec.addElement(login_page_data);
    default_settings.put("login_page_list", login_page_vec);
    default_settings.put("login_custom_script", "");
    default_settings.put("login_header", "");
    default_settings.put("login_footer", "");
    default_settings.put("day_of_week_allow", "1234567");
    default_settings.put("log_allow_str", "(ERROR)(START)(STOP)(QUIT_SERVER)(RUN_SERVER)(KICK)(BAN)(DENIAL)(ACCEPT)(DISCONNECT)(USER)(PASS)(SYST)(NOOP)(SIZE)(MDTM)(RNFR)(RNTO)(PWD)(CWD)(TYPE)(REST)(DELE)(MKD)(RMD)(MACB)(ABOR)(RETR)(STOR)(APPE)(LIST)(NLST)(CDUP)(PASV)(PORT)(AUTH)(PBSZ)(PROT)(SITE)(QUIT)(GET)(PUT)(DELETE)(MOVE)(STAT)(HELP)(PAUSE_RESUME)(PROXY)(MLSD)(MLST)(EPSV)(EPRT)(OPTS)(POST)(WEBINTERFACE)(STOU)(DELETE)(MOVE)(PROPFIND)(MKCOL)(PUT)(LOCK)(MLSD)(MLST)");
    default_settings.put("write_to_log", "true");
    default_settings.put("binary_mode", "false");
    default_settings.put("binary_mode_stor", "false");
    default_settings.put("show_date_time", "true");
    default_settings.put("roll_log", "true");
    default_settings.put("roll_log_size", "10");
    default_settings.put("roll_log_count", "30");
    default_settings.put("hammer_attempts_http", "100");
    default_settings.put("hammer_banning_http", "3");
    default_settings.put("ban_timeout_http", "5");
    default_settings.put("hammer_attempts", "100");
    default_settings.put("hammer_banning", "10");
    default_settings.put("ban_timeout", "5");
    default_settings.put("chammer_attempts", "100");
    default_settings.put("chammer_banning", "30");
    default_settings.put("cban_timeout", "5");
    default_settings.put("phammer_attempts", "15");
    default_settings.put("phammer_banning", "30");
    default_settings.put("pban_timeout", "0");
    default_settings.put("hban_timeout", "0");
    default_settings.put("hack_usernames", "administrator,admin,root");
    default_settings.put("never_ban", "127.0.0.1");
    default_settings.put("miniURLHost", "http://www.domain.com:8080/");
    default_settings.put("user_default_folder_privs", "(read)(view)(resume)");
    default_settings.put("log_date_format", "MM/dd/yyyy HH:mm:ss.SSS");
    default_settings.put("localization", "English");
    default_settings.put("ssh_encoding", "UTF8");
    default_settings.put("webdav_timezone", "0");
    default_settings.put("random_password_length", "6");
    default_settings.put("min_password_length", "3");
    default_settings.put("min_password_numbers", "0");
    default_settings.put("min_password_lowers", "0");
    default_settings.put("min_password_uppers", "0");
    default_settings.put("min_password_specials", "0");
    default_settings.put("password_history_count", "0");
    default_settings.put("unsafe_password_chars", "@#%/:\\");
    default_settings.put("unsafe_filename_chars", "");
    default_settings.put("restart_script", "");
    default_settings.put("prefs_version", "6");
    default_settings.put("allow_ssh_0_byte_file", "true");
    default_settings.put("ssh_randomaccess", "false");
    default_settings.put("log_roll_date_format", "yyyyMMdd_HHmmss");
    default_settings.put("log_roll_rename_hours", "0");
    default_settings.put("bytes_label", "Bytes");
    default_settings.put("bytes_label_short", "B");
    default_settings.put("terrabytes_label", "Terra");
    default_settings.put("gigabytes_label", "Giga");
    default_settings.put("megabytes_label", "Mega");
    default_settings.put("kilobytes_label", "Kilo");
    default_settings.put("terrabytes_label_short", "T");
    default_settings.put("gigabytes_label_short", "G");
    default_settings.put("megabytes_label_short", "M");
    default_settings.put("kilobytes_label_short", "K");
    default_settings.put("resolve_inheritance", "true");
    default_settings.put("make_dir_uploads", "false");
    default_settings.put("temp_account_share_web_forms", "false");
    default_settings.put("temp_account_share_web_buttons", "true");
    default_settings.put("temp_account_share_web_customizations", "true");
    default_settings.put("temp_account_share_web_css", "true");
    default_settings.put("temp_account_share_web_javascript", "true");
    default_settings.put("stop_listing_on_login_failure", "true");
    default_settings.put("acl_mode", "2");
    default_settings.put("acl_lookup_tool", "plugins/lib/aclchk.exe");
    if (machine_is_windows()) {
      default_settings.put("serverbeat_command", "netsh");
    } else {
      default_settings.put("serverbeat_command", "/sbin/ifconfig");
    } 
    default_settings.put("serverbeat_ifup_command", "/sbin/ifup");
    default_settings.put("serverbeat_ifdown_command", "/sbin/ifdown");
    default_settings.put("serverbeat_post_command", "");
    default_settings.put("ssh_header", "CrushFTPSSHD");
    default_settings.put("logging_db_url", "jdbc:mysql://127.0.0.1:3306/crushftp?autoReconnect=true");
    default_settings.put("logging_db_driver_file", "./mysql-connector-java-5.0.4-bin.jar");
    default_settings.put("logging_db_driver", "org.gjt.mm.mysql.Driver");
    default_settings.put("logging_db_user", "crushftp");
    default_settings.put("logging_db_pass", "");
    default_settings.put("logging_db_insert", "insert into CRUSHFTP_LOG (LOG_MILLIS,LOG_TAG,LOG_DATA,LOG_ROW_NUM) values(?,?,?,?)");
    default_settings.put("logging_db_query_count", "select max(LOG_ROW_NUM) from CRUSHFTP_LOG");
    default_settings.put("logging_db_query", "select LOG_DATA,LOG_MILLIS,LOG_ROW_NUM from CRUSHFTP_LOG where LOG_ROW_NUM >= ? and LOG_ROW_NUM <= ? order by LOG_ROW_NUM");
    default_settings.put("custom_delete_msg", "\"%user_the_command_data%\" delete successful.");
    default_settings.put("trust_expired_client_cert", "false");
    default_settings.put("email_reset_token", "false");
    default_settings.put("direct_link_access", "false");
    default_settings.put("rnto_overwrite", "false");
    default_settings.put("jobs_location", System.getProperty("crushftp.prefs"));
    default_settings.put("resume_idle_job_delay", "30");
    default_settings.put("password_salt_location", "");
    default_settings.put("replicate_session_host_port", "");
    default_settings.put("search_max_content_kb", "2");
    default_settings.put("find_list_previews", "true");
    default_settings.put("generic_ftp_responses", "false");
    default_settings.put("block_hack_username_immediately", "false");
    default_settings.put("recent_user_log_days", "7");
    default_settings.put("recent_job_log_days", "7");
    default_settings.put("recent_temp_job_log_days", "7");
    default_settings.put("recent_job_days", "7");
    default_settings.put("recent_temp_job_days", "7");
    default_settings.put("csrf", "true");
    default_settings.put("lsla_year", "false");
    default_settings.put("s3_threads_upload", "3");
    default_settings.put("s3_threads_download", "3");
    default_settings.put("csrf_flipped", "false");
    default_settings.put("syslog_encoding", "UTF8");
    default_settings.put("make_upload_parent_folders", "false");
    default_settings.put("search_keywords_also", "true");
    default_settings.put("file_client_not_found_error", "true");
    default_settings.put("max_event_threads", "100");
    default_settings.put("write_session_logs", "true");
    default_settings.put("http_buffer", "10");
    default_settings.put("memcache", "false");
    default_settings.put("normalize_utf8", "true");
    default_settings.put("track_last_logins", "false");
    default_settings.put("allow_session_caching_memory", "false");
    default_settings.put("ssl_renegotiation_blocked", "true");
    default_settings.put("reset_token_timeout", "10");
    default_settings.put("send_dot_dot_list_secure", "true");
    default_settings.put("send_dot_dot_list_sftp", "true");
    default_settings.put("exif_listings", "false");
    default_settings.put("max_job_summary_scan", "300");
    default_settings.put("s3_max_buffer_download", "100");
    default_settings.put("calculate_transfer_usage_listings", "false");
    default_settings.put("audit_job_logs", "false");
    default_settings.put("whitelist_web_commands", "batchComplete,logout,getXMLListing");
    default_settings.put("proxy_list_max", "0");
    default_settings.put("allow_save_pass_phone", "false");
    default_settings.put("hide_ftp_quota_log", "false");
    default_settings.put("multi_journal", "false");
    default_settings.put("hash_algorithm", "MD5");
    default_settings.put("ssh_rename_overwrite", "false");
    default_settings.put("plugin_log_call", "false");
    default_settings.put("single_job_scheduler_serverbeat", "true");
    default_settings.put("fips140", "false");
    default_settings.put("max_url_length", "99999");
    default_settings.put("ssh_runtime_exception", "false");
    default_settings.put("strip_windows_domain_webdav", "true");
    default_settings.put("single_report_scheduler_serverbeat", "true");
    default_settings.put("include_ftp_nlst_path", "false");
    default_settings.put("store_job_items", "true");
    default_settings.put("s3crush_replicated", "false");
    default_settings.put("replicate_jobs", "false");
    default_settings.put("replicate_sessions", "true");
    default_settings.put("auto_fix_stats_sessions", "true");
    default_settings.put("temp_accounts_account_expire_task", "");
    default_settings.put("separate_speeds_by_username_ip", "false");
    default_settings.put("update_proxy_type", "");
    default_settings.put("update_proxy_host", "");
    default_settings.put("update_proxy_port", "");
    default_settings.put("update_proxy_user", "");
    default_settings.put("update_proxy_pass", "");
    default_settings.put("pasv_bind_all", "false");
    default_settings.put("cookie_expire_hours", "0");
    default_settings.put("job_scheduler_enabled", "true");
    default_settings.put("minimum_speed_warn_seconds", "10");
    default_settings.put("minimum_speed_alert_seconds", "30");
    default_settings.put("file_encrypt_ascii", "false");
    default_settings.put("smtp_subject_encoded", "false");
    default_settings.put("replicated_server_ips", "*");
    default_settings.put("dmz_stat_caching", "false");
    default_settings.put("direct_link_to_webinterface", "false");
    default_settings.put("stor_pooling", "true");
    default_settings.put("tls_version_client", "SSLv2Hello,TLSv1,TLSv1.1,TLSv1.2");
    default_settings.put("tunnel_minimum_version", "3.4.0");
    default_settings.put("delete_threads", "40");
    default_settings.put("drop_folder_rename_new", "false");
    default_settings.put("serverbeat_plumb", "true");
    default_settings.put("serverbeat_unplumb", "true");
    default_settings.put("serverbeat_relative_timing", "true");
    default_settings.put("windows_character_encoding_process", "windows-1252");
    default_settings.put("memory_log_interval", "600");
    default_settings.put("dump_threads_log_interval", "-1");
    default_settings.put("Strict-Transport-Security", "");
    default_settings.put("replicated_vfs_url", "");
    default_settings.put("replicated_vfs_root_url", "");
    default_settings.put("replicated_vfs_user", "");
    default_settings.put("replicated_vfs_pass", "");
    default_settings.put("replicated_vfs_ping_interval", "60");
    default_settings.put("replicated_auto_play_journal", "true");
    default_settings.put("startup_delay", "0");
    default_settings.put("s3_sha256", "false");
    default_settings.put("block_bad_ftp_socket_paths", "true");
    default_settings.put("as2_mic_alg", "optional, sha-256, sha1, md5");
    default_settings.put("temp_account_bad_timeout", "30");
    default_settings.put("s3_ignore_partial", "false");
    default_settings.put("expire_password_email_token_only", "false");
    default_settings.put("replicate_jobs_sync", "false");
    default_settings.put("block_client_renegotiation", "true");
    default_settings.put("webdav_agent_learning", "true");
    default_settings.put("debug_socks_log", "false");
    default_settings.put("run_alerts_dmz", "false");
    default_settings.put("http_header1", "");
    default_settings.put("http_header2", "");
    default_settings.put("http_header3", "");
    default_settings.put("http_header4", "");
    default_settings.put("http_header5", "");
    default_settings.put("http_header6", "");
    default_settings.put("http_header7", "");
    default_settings.put("http_header8", "");
    default_settings.put("http_header9", "");
    default_settings.put("http_header10", "");
    default_settings.put("http_header11", "");
    default_settings.put("http_header12", "");
    default_settings.put("http_header13", "");
    default_settings.put("http_header14", "");
    default_settings.put("http_header15", "");
    default_settings.put("http_header16", "");
    default_settings.put("http_header17", "");
    default_settings.put("http_header18", "");
    default_settings.put("http_header19", "");
    default_settings.put("http_header20", "");
    default_settings.put("otp_url", "https://{otp_username}:{otp_password}@api.twilio.com/2010-04-01/Accounts/{otp_username}/Messages.json");
    default_settings.put("otp_username", "");
    default_settings.put("otp_password", "");
    default_settings.put("otp_extra1", "");
    default_settings.put("otp_extra2", "");
    default_settings.put("otp_url_verb", "POST");
    default_settings.put("otp_from", "");
    default_settings.put("otp_post", "To={otp_to}&From={otp_from}&Body={otp_token}");
    default_settings.put("otp_validated_logins", "false");
    default_settings.put("otp_token_timeout", "60000");
    default_settings.put("html5_chunk_timeout", "600");
    default_settings.put("crushauth_httponly", "true");
    default_settings.put("recursive_rename_event", "false");
    default_settings.put("recursive_delete_event", "false");
    default_settings.put("vfs_cache_interval", "60");
    default_settings.put("vfs_cache_enabled", "false");
    default_settings.put("expired_accounts_notify_now", "false");
    default_settings.put("expired_passwords_notify_now", "false");
    default_settings.put("stop_dmz_ports_internal_down", "false");
    default_settings.put("job_max_runtime_hours", "");
    default_settings.put("job_max_runtime_minutes", "");
    default_settings.put("task_max_runtime_hours", "");
    default_settings.put("task_max_runtime_minutes", "");
    default_settings.put("crushftp_smtp_sasl", "false");
    default_settings.put("allow_router_ban", "false");
    default_settings.put("report_prefix", "");
    default_settings.put("replicate_reports", "false");
    default_settings.put("serverbeat_dmz_master", "false");
    default_settings.put("synchronized_sftp", "true");
    default_settings.put("dmz_log_in_internal_server", "false");
    default_settings.put("track_jobs_for_reports", "false");
    default_settings.put("admin_ips", "127.0.0.1,*");
    default_settings.put("ignore_ssh_closefile_download_error", "false");
    default_settings.put("http_redirect_base", "DISABLED");
    default_settings.put("search_file_contents_also", "false");
    default_settings.put("multi_journal_timeout", "30000");
    default_settings.put("unsafe_filename_chars_rename", "");
    default_settings.put("webinterface_redirect_with_password", "true");
    default_settings.put("share_attached_file_size_limit", "10");
    default_settings.put("max_http_header_length", "2000");
    default_settings.put("pgp_check_downloads", "true");
    default_settings.put("sha3_keccak", "false");
    default_settings.put("ignore_failed_directory_listings", "false");
    default_settings.put("max_items_dir", "-1");
    default_settings.put("sftp_mkdir_exist_silent", "false");
    default_settings.put("log_ftp_client_listings", "true");
    default_settings.put("single_preview_serverbeat", "false");
    default_settings.put("max_denied_ips", "100");
    default_settings.put("as2_sha256", "sha-256");
    default_settings.put("always_validate_plugins_for_dmz_lookup", "true");
    default_settings.put("send_dmz_error_events_to_internal", "true");
    default_settings.put("v9_beta", "false");
    PreviewWorker.getDefaults(default_settings);
    StatTools.setDefaults(default_settings);
    SearchTools.setDefaults(default_settings);
    SyncTools.setDefaults(default_settings);
  }
}
