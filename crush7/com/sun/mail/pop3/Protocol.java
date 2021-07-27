package com.sun.mail.pop3;

import com.sun.mail.util.LineInputStream;
import com.sun.mail.util.MailLogger;
import com.sun.mail.util.PropUtil;
import com.sun.mail.util.SharedByteArrayOutputStream;
import com.sun.mail.util.SocketFetcher;
import com.sun.mail.util.TraceInputStream;
import com.sun.mail.util.TraceOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;

class Protocol {
  private Socket socket;
  
  private String host;
  
  private Properties props;
  
  private String prefix;
  
  private BufferedReader input;
  
  private PrintWriter output;
  
  private TraceInputStream traceInput;
  
  private TraceOutputStream traceOutput;
  
  private MailLogger logger;
  
  private MailLogger traceLogger;
  
  private String apopChallenge;
  
  private Map<String, String> capabilities;
  
  private boolean pipelining;
  
  private boolean noauthdebug;
  
  private boolean traceSuspended;
  
  private static final int POP3_PORT = 110;
  
  private static final String CRLF = "\r\n";
  
  private static final int SLOP = 128;
  
  Protocol(String host, int port, MailLogger logger, Properties props, String prefix, boolean isSSL) throws IOException {
    Response r;
    this.apopChallenge = null;
    this.capabilities = null;
    this.noauthdebug = true;
    this.host = host;
    this.props = props;
    this.prefix = prefix;
    this.logger = logger;
    this.traceLogger = logger.getSubLogger("protocol", null);
    this.noauthdebug = !PropUtil.getBooleanProperty(props, "mail.debug.auth", false);
    boolean enableAPOP = getBoolProp(props, prefix + ".apop.enable");
    boolean disableCapa = getBoolProp(props, prefix + ".disablecapa");
    try {
      if (port == -1)
        port = 110; 
      if (logger.isLoggable(Level.FINE))
        logger.fine("connecting to host \"" + host + "\", port " + port + ", isSSL " + isSSL); 
      this.socket = SocketFetcher.getSocket(host, port, props, prefix, isSSL);
      initStreams();
      r = simpleCommand(null);
    } catch (IOException ioe) {
      try {
        this.socket.close();
      } finally {
        throw ioe;
      } 
    } 
    if (!r.ok)
      try {
        this.socket.close();
      } finally {
        throw new IOException("Connect failed");
      }  
    if (enableAPOP && r.data != null) {
      int challStart = r.data.indexOf('<');
      int challEnd = r.data.indexOf('>', challStart);
      if (challStart != -1 && challEnd != -1)
        this.apopChallenge = r.data.substring(challStart, challEnd + 1); 
      logger.log(Level.FINE, "APOP challenge: {0}", this.apopChallenge);
    } 
    if (!disableCapa)
      setCapabilities(capa()); 
    this
      .pipelining = (hasCapability("PIPELINING") || PropUtil.getBooleanProperty(props, prefix + ".pipelining", false));
    if (this.pipelining)
      logger.config("PIPELINING enabled"); 
  }
  
  private final synchronized boolean getBoolProp(Properties props, String prop) {
    boolean val = PropUtil.getBooleanProperty(props, prop, false);
    if (this.logger.isLoggable(Level.CONFIG))
      this.logger.config(prop + ": " + val); 
    return val;
  }
  
  private void initStreams() throws IOException {
    boolean quote = PropUtil.getBooleanProperty(this.props, "mail.debug.quote", false);
    this
      .traceInput = new TraceInputStream(this.socket.getInputStream(), this.traceLogger);
    this.traceInput.setQuote(quote);
    this
      .traceOutput = new TraceOutputStream(this.socket.getOutputStream(), this.traceLogger);
    this.traceOutput.setQuote(quote);
    this.input = new BufferedReader(new InputStreamReader(this.traceInput, "iso-8859-1"));
    this.output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(this.traceOutput, "iso-8859-1")));
  }
  
  protected void finalize() throws Throwable {
    try {
      if (this.socket != null)
        quit(); 
    } finally {
      super.finalize();
    } 
  }
  
  synchronized void setCapabilities(InputStream in) {
    if (in == null) {
      this.capabilities = null;
      return;
    } 
    this.capabilities = new HashMap<String, String>(10);
    BufferedReader r = null;
    try {
      r = new BufferedReader(new InputStreamReader(in, "us-ascii"));
    } catch (UnsupportedEncodingException ex) {
      assert false;
    } 
    while (true) {
      try {
        String s;
        if ((s = r.readLine()) != null) {
          String cap = s;
          int i = cap.indexOf(' ');
          if (i > 0)
            cap = cap.substring(0, i); 
          this.capabilities.put(cap.toUpperCase(Locale.ENGLISH), s);
          continue;
        } 
      } catch (IOException iOException) {}
      try {
        in.close();
      } catch (IOException iOException) {}
      break;
    } 
  }
  
  synchronized boolean hasCapability(String c) {
    return (this.capabilities != null && this.capabilities
      .containsKey(c.toUpperCase(Locale.ENGLISH)));
  }
  
  synchronized Map<String, String> getCapabilities() {
    return this.capabilities;
  }
  
  synchronized String login(String user, String password) throws IOException {
    boolean batch = (this.pipelining && this.socket instanceof javax.net.ssl.SSLSocket);
    try {
      Response r;
      if (this.noauthdebug && isTracing()) {
        this.logger.fine("authentication command trace suppressed");
        suspendTracing();
      } 
      String dpw = null;
      if (this.apopChallenge != null)
        dpw = getDigest(password); 
      if (this.apopChallenge != null && dpw != null) {
        r = simpleCommand("APOP " + user + " " + dpw);
      } else if (batch) {
        String cmd = "USER " + user;
        batchCommandStart(cmd);
        issueCommand(cmd);
        cmd = "PASS " + password;
        batchCommandContinue(cmd);
        issueCommand(cmd);
        r = readResponse();
        if (!r.ok) {
          String err = (r.data != null) ? r.data : "USER command failed";
          readResponse();
          batchCommandEnd();
          return err;
        } 
        r = readResponse();
        batchCommandEnd();
      } else {
        r = simpleCommand("USER " + user);
        if (!r.ok)
          return (r.data != null) ? r.data : "USER command failed"; 
        r = simpleCommand("PASS " + password);
      } 
      if (this.noauthdebug && isTracing())
        this.logger.log(Level.FINE, "authentication command {0}", r.ok ? "succeeded" : "failed"); 
      if (!r.ok)
        return (r.data != null) ? r.data : "login failed"; 
      return null;
    } finally {
      resumeTracing();
    } 
  }
  
  private String getDigest(String password) {
    byte[] digest;
    String key = this.apopChallenge + password;
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      digest = md.digest(key.getBytes("iso-8859-1"));
    } catch (NoSuchAlgorithmException nsae) {
      return null;
    } catch (UnsupportedEncodingException uee) {
      return null;
    } 
    return toHex(digest);
  }
  
  private static char[] digits = new char[] { 
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
      'a', 'b', 'c', 'd', 'e', 'f' };
  
  private static String toHex(byte[] bytes) {
    char[] result = new char[bytes.length * 2];
    for (int index = 0, i = 0; index < bytes.length; index++) {
      int temp = bytes[index] & 0xFF;
      result[i++] = digits[temp >> 4];
      result[i++] = digits[temp & 0xF];
    } 
    return new String(result);
  }
  
  synchronized boolean quit() throws IOException {
    boolean ok = false;
    try {
      Response r = simpleCommand("QUIT");
      ok = r.ok;
    } finally {
      close();
    } 
    return ok;
  }
  
  void close() {
    try {
      this.socket.close();
    } catch (IOException iOException) {
    
    } finally {
      this.socket = null;
      this.input = null;
      this.output = null;
    } 
  }
  
  synchronized Status stat() throws IOException {
    Response r = simpleCommand("STAT");
    Status s = new Status();
    if (!r.ok)
      throw new IOException("STAT command failed: " + r.data); 
    if (r.data != null)
      try {
        StringTokenizer st = new StringTokenizer(r.data);
        s.total = Integer.parseInt(st.nextToken());
        s.size = Integer.parseInt(st.nextToken());
      } catch (RuntimeException runtimeException) {} 
    return s;
  }
  
  synchronized int list(int msg) throws IOException {
    Response r = simpleCommand("LIST " + msg);
    int size = -1;
    if (r.ok && r.data != null)
      try {
        StringTokenizer st = new StringTokenizer(r.data);
        st.nextToken();
        size = Integer.parseInt(st.nextToken());
      } catch (RuntimeException runtimeException) {} 
    return size;
  }
  
  synchronized InputStream list() throws IOException {
    Response r = multilineCommand("LIST", 128);
    return r.bytes;
  }
  
  synchronized InputStream retr(int msg, int size) throws IOException {
    Response r;
    boolean batch = (size == 0 && this.pipelining);
    if (batch) {
      String cmd = "LIST " + msg;
      batchCommandStart(cmd);
      issueCommand(cmd);
      cmd = "RETR " + msg;
      batchCommandContinue(cmd);
      issueCommand(cmd);
      r = readResponse();
      if (r.ok && r.data != null)
        try {
          StringTokenizer st = new StringTokenizer(r.data);
          st.nextToken();
          size = Integer.parseInt(st.nextToken());
          if (size > 1073741824 || size < 0) {
            size = 0;
          } else {
            if (this.logger.isLoggable(Level.FINE))
              this.logger.fine("pipeline message size " + size); 
            size += 128;
          } 
        } catch (RuntimeException runtimeException) {} 
      r = readResponse();
      if (r.ok)
        r.bytes = readMultilineResponse(size + 128); 
      batchCommandEnd();
    } else {
      String cmd = "RETR " + msg;
      multilineCommandStart(cmd);
      issueCommand(cmd);
      r = readResponse();
      if (!r.ok) {
        multilineCommandEnd();
        return null;
      } 
      if (size <= 0 && r.data != null)
        try {
          StringTokenizer st = new StringTokenizer(r.data);
          String s = st.nextToken();
          String octets = st.nextToken();
          if (octets.equals("octets")) {
            size = Integer.parseInt(s);
            if (size > 1073741824 || size < 0) {
              size = 0;
            } else {
              if (this.logger.isLoggable(Level.FINE))
                this.logger.fine("guessing message size: " + size); 
              size += 128;
            } 
          } 
        } catch (RuntimeException runtimeException) {} 
      r.bytes = readMultilineResponse(size);
      multilineCommandEnd();
    } 
    if (r.ok && 
      size > 0 && this.logger.isLoggable(Level.FINE))
      this.logger.fine("got message size " + r.bytes.available()); 
    return r.bytes;
  }
  
  synchronized boolean retr(int msg, OutputStream os) throws IOException {
    int b;
    String cmd = "RETR " + msg;
    multilineCommandStart(cmd);
    issueCommand(cmd);
    Response r = readResponse();
    if (!r.ok) {
      multilineCommandEnd();
      return false;
    } 
    Throwable terr = null;
    int lastb = 10;
    try {
      while ((b = this.input.read()) >= 0) {
        if (lastb == 10 && b == 46) {
          b = this.input.read();
          if (b == 13) {
            b = this.input.read();
            break;
          } 
        } 
        if (terr == null)
          try {
            os.write(b);
          } catch (IOException ex) {
            this.logger.log(Level.FINE, "exception while streaming", ex);
            terr = ex;
          } catch (RuntimeException ex) {
            this.logger.log(Level.FINE, "exception while streaming", ex);
            terr = ex;
          }  
        lastb = b;
      } 
    } catch (InterruptedIOException iioex) {
      try {
        this.socket.close();
      } catch (IOException iOException) {}
      throw iioex;
    } 
    if (b < 0)
      throw new EOFException("EOF on socket"); 
    if (terr != null) {
      if (terr instanceof IOException)
        throw (IOException)terr; 
      if (terr instanceof RuntimeException)
        throw (RuntimeException)terr; 
      assert false;
    } 
    multilineCommandEnd();
    return true;
  }
  
  synchronized InputStream top(int msg, int n) throws IOException {
    Response r = multilineCommand("TOP " + msg + " " + n, 0);
    return r.bytes;
  }
  
  synchronized boolean dele(int msg) throws IOException {
    Response r = simpleCommand("DELE " + msg);
    return r.ok;
  }
  
  synchronized String uidl(int msg) throws IOException {
    Response r = simpleCommand("UIDL " + msg);
    if (!r.ok)
      return null; 
    int i = r.data.indexOf(' ');
    if (i > 0)
      return r.data.substring(i + 1); 
    return null;
  }
  
  synchronized boolean uidl(String[] uids) throws IOException {
    Response r = multilineCommand("UIDL", 15 * uids.length);
    if (!r.ok)
      return false; 
    LineInputStream lis = new LineInputStream(r.bytes);
    String line = null;
    while ((line = lis.readLine()) != null) {
      int i = line.indexOf(' ');
      if (i < 1 || i >= line.length())
        continue; 
      int n = Integer.parseInt(line.substring(0, i));
      if (n > 0 && n <= uids.length)
        uids[n - 1] = line.substring(i + 1); 
    } 
    try {
      r.bytes.close();
    } catch (IOException iOException) {}
    return true;
  }
  
  synchronized boolean noop() throws IOException {
    Response r = simpleCommand("NOOP");
    return r.ok;
  }
  
  synchronized boolean rset() throws IOException {
    Response r = simpleCommand("RSET");
    return r.ok;
  }
  
  synchronized boolean stls() throws IOException {
    if (this.socket instanceof javax.net.ssl.SSLSocket)
      return true; 
    Response r = simpleCommand("STLS");
    if (r.ok)
      try {
        this.socket = SocketFetcher.startTLS(this.socket, this.host, this.props, this.prefix);
        initStreams();
      } catch (IOException ioex) {
        try {
          this.socket.close();
        } finally {
          this.socket = null;
          this.input = null;
          this.output = null;
        } 
        IOException sioex = new IOException("Could not convert socket to TLS");
        sioex.initCause(ioex);
        throw sioex;
      }  
    return r.ok;
  }
  
  synchronized boolean isSSL() {
    return this.socket instanceof javax.net.ssl.SSLSocket;
  }
  
  synchronized InputStream capa() throws IOException {
    Response r = multilineCommand("CAPA", 128);
    if (!r.ok)
      return null; 
    return r.bytes;
  }
  
  private Response simpleCommand(String cmd) throws IOException {
    simpleCommandStart(cmd);
    issueCommand(cmd);
    Response r = readResponse();
    simpleCommandEnd();
    return r;
  }
  
  private void issueCommand(String cmd) throws IOException {
    if (this.socket == null)
      throw new IOException("Folder is closed"); 
    if (cmd != null) {
      cmd = cmd + "\r\n";
      this.output.print(cmd);
      this.output.flush();
    } 
  }
  
  private Response readResponse() throws IOException {
    String line = null;
    try {
      line = this.input.readLine();
    } catch (InterruptedIOException iioex) {
      try {
        this.socket.close();
      } catch (IOException iOException) {}
      throw new EOFException(iioex.getMessage());
    } catch (SocketException ex) {
      try {
        this.socket.close();
      } catch (IOException iOException) {}
      throw new EOFException(ex.getMessage());
    } 
    if (line == null) {
      this.traceLogger.finest("<EOF>");
      throw new EOFException("EOF on socket");
    } 
    Response r = new Response();
    if (line.startsWith("+OK")) {
      r.ok = true;
    } else if (line.startsWith("-ERR")) {
      r.ok = false;
    } else {
      throw new IOException("Unexpected response: " + line);
    } 
    int i;
    if ((i = line.indexOf(' ')) >= 0)
      r.data = line.substring(i + 1); 
    return r;
  }
  
  private Response multilineCommand(String cmd, int size) throws IOException {
    multilineCommandStart(cmd);
    issueCommand(cmd);
    Response r = readResponse();
    if (!r.ok) {
      multilineCommandEnd();
      return r;
    } 
    r.bytes = readMultilineResponse(size);
    multilineCommandEnd();
    return r;
  }
  
  private InputStream readMultilineResponse(int size) throws IOException {
    int b;
    SharedByteArrayOutputStream buf = new SharedByteArrayOutputStream(size);
    int lastb = 10;
    try {
      while ((b = this.input.read()) >= 0) {
        if (lastb == 10 && b == 46) {
          b = this.input.read();
          if (b == 13) {
            b = this.input.read();
            break;
          } 
        } 
        buf.write(b);
        lastb = b;
      } 
    } catch (InterruptedIOException iioex) {
      try {
        this.socket.close();
      } catch (IOException iOException) {}
      throw iioex;
    } 
    if (b < 0)
      throw new EOFException("EOF on socket"); 
    return buf.toStream();
  }
  
  protected boolean isTracing() {
    return this.traceLogger.isLoggable(Level.FINEST);
  }
  
  private void suspendTracing() {
    if (this.traceLogger.isLoggable(Level.FINEST)) {
      this.traceInput.setTrace(false);
      this.traceOutput.setTrace(false);
    } 
  }
  
  private void resumeTracing() {
    if (this.traceLogger.isLoggable(Level.FINEST)) {
      this.traceInput.setTrace(true);
      this.traceOutput.setTrace(true);
    } 
  }
  
  private void simpleCommandStart(String command) {}
  
  private void simpleCommandEnd() {}
  
  private void multilineCommandStart(String command) {}
  
  private void multilineCommandEnd() {}
  
  private void batchCommandStart(String command) {}
  
  private void batchCommandContinue(String command) {}
  
  private void batchCommandEnd() {}
}
