package javax.mail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.BitSet;
import java.util.Locale;

public class URLName {
  protected String fullURL;
  
  private String protocol;
  
  private String username;
  
  private String password;
  
  private String host;
  
  private InetAddress hostAddress;
  
  private boolean hostAddressKnown = false;
  
  private int port = -1;
  
  private String file;
  
  private String ref;
  
  private int hashCode = 0;
  
  private static boolean doEncode = true;
  
  static {
    try {
      doEncode = !Boolean.getBoolean("mail.URLName.dontencode");
    } catch (Exception exception) {}
  }
  
  public URLName(String protocol, String host, int port, String file, String username, String password) {
    this.protocol = protocol;
    this.host = host;
    this.port = port;
    int refStart;
    if (file != null && (refStart = file.indexOf('#')) != -1) {
      this.file = file.substring(0, refStart);
      this.ref = file.substring(refStart + 1);
    } else {
      this.file = file;
      this.ref = null;
    } 
    this.username = doEncode ? encode(username) : username;
    this.password = doEncode ? encode(password) : password;
  }
  
  public URLName(URL url) {
    this(url.toString());
  }
  
  public URLName(String url) {
    parseString(url);
  }
  
  public String toString() {
    if (this.fullURL == null) {
      StringBuffer tempURL = new StringBuffer();
      if (this.protocol != null) {
        tempURL.append(this.protocol);
        tempURL.append(":");
      } 
      if (this.username != null || this.host != null) {
        tempURL.append("//");
        if (this.username != null) {
          tempURL.append(this.username);
          if (this.password != null) {
            tempURL.append(":");
            tempURL.append(this.password);
          } 
          tempURL.append("@");
        } 
        if (this.host != null)
          tempURL.append(this.host); 
        if (this.port != -1) {
          tempURL.append(":");
          tempURL.append(Integer.toString(this.port));
        } 
        if (this.file != null)
          tempURL.append("/"); 
      } 
      if (this.file != null)
        tempURL.append(this.file); 
      if (this.ref != null) {
        tempURL.append("#");
        tempURL.append(this.ref);
      } 
      this.fullURL = tempURL.toString();
    } 
    return this.fullURL;
  }
  
  protected void parseString(String url) {
    this.protocol = this.file = this.ref = this.host = this.username = this.password = null;
    this.port = -1;
    int len = url.length();
    int protocolEnd = url.indexOf(':');
    if (protocolEnd != -1)
      this.protocol = url.substring(0, protocolEnd); 
    if (url.regionMatches(protocolEnd + 1, "//", 0, 2)) {
      int portindex;
      String fullhost = null;
      int fileStart = url.indexOf('/', protocolEnd + 3);
      if (fileStart != -1) {
        fullhost = url.substring(protocolEnd + 3, fileStart);
        if (fileStart + 1 < len) {
          this.file = url.substring(fileStart + 1);
        } else {
          this.file = "";
        } 
      } else {
        fullhost = url.substring(protocolEnd + 3);
      } 
      int i = fullhost.indexOf('@');
      if (i != -1) {
        String fulluserpass = fullhost.substring(0, i);
        fullhost = fullhost.substring(i + 1);
        int passindex = fulluserpass.indexOf(':');
        if (passindex != -1) {
          this.username = fulluserpass.substring(0, passindex);
          this.password = fulluserpass.substring(passindex + 1);
        } else {
          this.username = fulluserpass;
        } 
      } 
      if (fullhost.length() > 0 && fullhost.charAt(0) == '[') {
        portindex = fullhost.indexOf(':', fullhost.indexOf(']'));
      } else {
        portindex = fullhost.indexOf(':');
      } 
      if (portindex != -1) {
        String portstring = fullhost.substring(portindex + 1);
        if (portstring.length() > 0)
          try {
            this.port = Integer.parseInt(portstring);
          } catch (NumberFormatException nfex) {
            this.port = -1;
          }  
        this.host = fullhost.substring(0, portindex);
      } else {
        this.host = fullhost;
      } 
    } else if (protocolEnd + 1 < len) {
      this.file = url.substring(protocolEnd + 1);
    } 
    int refStart;
    if (this.file != null && (refStart = this.file.indexOf('#')) != -1) {
      this.ref = this.file.substring(refStart + 1);
      this.file = this.file.substring(0, refStart);
    } 
  }
  
  public int getPort() {
    return this.port;
  }
  
  public String getProtocol() {
    return this.protocol;
  }
  
  public String getFile() {
    return this.file;
  }
  
  public String getRef() {
    return this.ref;
  }
  
  public String getHost() {
    return this.host;
  }
  
  public String getUsername() {
    return doEncode ? decode(this.username) : this.username;
  }
  
  public String getPassword() {
    return doEncode ? decode(this.password) : this.password;
  }
  
  public URL getURL() throws MalformedURLException {
    return new URL(getProtocol(), getHost(), getPort(), getFile());
  }
  
  public boolean equals(Object obj) {
    if (!(obj instanceof URLName))
      return false; 
    URLName u2 = (URLName)obj;
    if (this.protocol != u2.protocol && (this.protocol == null || 
      !this.protocol.equals(u2.protocol)))
      return false; 
    InetAddress a1 = getHostAddress(), a2 = u2.getHostAddress();
    if (a1 != null && a2 != null) {
      if (!a1.equals(a2))
        return false; 
    } else if (this.host != null && u2.host != null) {
      if (!this.host.equalsIgnoreCase(u2.host))
        return false; 
    } else if (this.host != u2.host) {
      return false;
    } 
    if (this.username != u2.username && (this.username == null || 
      !this.username.equals(u2.username)))
      return false; 
    String f1 = (this.file == null) ? "" : this.file;
    String f2 = (u2.file == null) ? "" : u2.file;
    if (!f1.equals(f2))
      return false; 
    if (this.port != u2.port)
      return false; 
    return true;
  }
  
  public int hashCode() {
    if (this.hashCode != 0)
      return this.hashCode; 
    if (this.protocol != null)
      this.hashCode += this.protocol.hashCode(); 
    InetAddress addr = getHostAddress();
    if (addr != null) {
      this.hashCode += addr.hashCode();
    } else if (this.host != null) {
      this.hashCode += this.host.toLowerCase(Locale.ENGLISH).hashCode();
    } 
    if (this.username != null)
      this.hashCode += this.username.hashCode(); 
    if (this.file != null)
      this.hashCode += this.file.hashCode(); 
    this.hashCode += this.port;
    return this.hashCode;
  }
  
  private synchronized InetAddress getHostAddress() {
    if (this.hostAddressKnown)
      return this.hostAddress; 
    if (this.host == null)
      return null; 
    try {
      this.hostAddress = InetAddress.getByName(this.host);
    } catch (UnknownHostException ex) {
      this.hostAddress = null;
    } 
    this.hostAddressKnown = true;
    return this.hostAddress;
  }
  
  static BitSet dontNeedEncoding = new BitSet(256);
  
  static final int caseDiff = 32;
  
  static {
    int i;
    for (i = 97; i <= 122; i++)
      dontNeedEncoding.set(i); 
    for (i = 65; i <= 90; i++)
      dontNeedEncoding.set(i); 
    for (i = 48; i <= 57; i++)
      dontNeedEncoding.set(i); 
    dontNeedEncoding.set(32);
    dontNeedEncoding.set(45);
    dontNeedEncoding.set(95);
    dontNeedEncoding.set(46);
    dontNeedEncoding.set(42);
  }
  
  static String encode(String s) {
    if (s == null)
      return null; 
    for (int i = 0; i < s.length(); i++) {
      int c = s.charAt(i);
      if (c == 32 || !dontNeedEncoding.get(c))
        return _encode(s); 
    } 
    return s;
  }
  
  private static String _encode(String s) {
    int maxBytesPerChar = 10;
    StringBuffer out = new StringBuffer(s.length());
    ByteArrayOutputStream buf = new ByteArrayOutputStream(maxBytesPerChar);
    OutputStreamWriter writer = new OutputStreamWriter(buf);
    for (int i = 0; i < s.length(); i++) {
      int c = s.charAt(i);
      if (dontNeedEncoding.get(c)) {
        if (c == 32)
          c = 43; 
        out.append((char)c);
      } else {
        try {
          writer.write(c);
          writer.flush();
        } catch (IOException e) {
          buf.reset();
        } 
        byte[] ba = buf.toByteArray();
        for (int j = 0; j < ba.length; j++) {
          out.append('%');
          char ch = Character.forDigit(ba[j] >> 4 & 0xF, 16);
          if (Character.isLetter(ch))
            ch = (char)(ch - 32); 
          out.append(ch);
          ch = Character.forDigit(ba[j] & 0xF, 16);
          if (Character.isLetter(ch))
            ch = (char)(ch - 32); 
          out.append(ch);
        } 
        buf.reset();
      } 
    } 
    return out.toString();
  }
  
  static String decode(String s) {
    if (s == null)
      return null; 
    if (indexOfAny(s, "+%") == -1)
      return s; 
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '+':
          sb.append(' ');
          break;
        case '%':
          try {
            sb.append((char)Integer.parseInt(s
                  .substring(i + 1, i + 3), 16));
          } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Illegal URL encoded value: " + s
                
                .substring(i, i + 3));
          } 
          i += 2;
          break;
        default:
          sb.append(c);
          break;
      } 
    } 
    String result = sb.toString();
    try {
      byte[] inputBytes = result.getBytes("8859_1");
      result = new String(inputBytes);
    } catch (UnsupportedEncodingException unsupportedEncodingException) {}
    return result;
  }
  
  private static int indexOfAny(String s, String any) {
    return indexOfAny(s, any, 0);
  }
  
  private static int indexOfAny(String s, String any, int start) {
    try {
      int len = s.length();
      for (int i = start; i < len; i++) {
        if (any.indexOf(s.charAt(i)) >= 0)
          return i; 
      } 
      return -1;
    } catch (StringIndexOutOfBoundsException e) {
      return -1;
    } 
  }
}
