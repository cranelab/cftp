package com.sun.mail.imap.protocol;

import com.sun.mail.auth.Ntlm;
import com.sun.mail.iap.Argument;
import com.sun.mail.iap.BadCommandException;
import com.sun.mail.iap.ByteArray;
import com.sun.mail.iap.CommandFailedException;
import com.sun.mail.iap.ConnectionException;
import com.sun.mail.iap.Literal;
import com.sun.mail.iap.LiteralException;
import com.sun.mail.iap.ParsingException;
import com.sun.mail.iap.Protocol;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.ACL;
import com.sun.mail.imap.AppendUID;
import com.sun.mail.imap.CopyUID;
import com.sun.mail.imap.ResyncData;
import com.sun.mail.imap.Rights;
import com.sun.mail.imap.SortTerm;
import com.sun.mail.imap.Utility;
import com.sun.mail.util.ASCIIUtility;
import com.sun.mail.util.BASE64EncoderStream;
import com.sun.mail.util.MailLogger;
import com.sun.mail.util.PropUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import javax.mail.Flags;
import javax.mail.Quota;
import javax.mail.internet.MimeUtility;
import javax.mail.search.SearchException;
import javax.mail.search.SearchTerm;

public class IMAPProtocol extends Protocol {
  private boolean connected = false;
  
  private boolean rev1 = false;
  
  private boolean referralException;
  
  private boolean noauthdebug = true;
  
  private boolean authenticated;
  
  private Map<String, String> capabilities;
  
  private List<String> authmechs;
  
  protected SearchSequence searchSequence;
  
  protected String[] searchCharsets;
  
  protected Set<String> enabled;
  
  private String name;
  
  private SaslAuthenticator saslAuthenticator;
  
  private String proxyAuthUser;
  
  private ByteArray ba;
  
  private static final byte[] CRLF = new byte[] { 13, 10 };
  
  private static final FetchItem[] fetchItems = new FetchItem[0];
  
  private volatile String idleTag;
  
  public IMAPProtocol(String name, String host, int port, Properties props, boolean isSSL, MailLogger logger) throws IOException, ProtocolException {
    super(host, port, props, "mail." + name, isSSL, logger);
    try {
      this.name = name;
      this
        .noauthdebug = !PropUtil.getBooleanProperty(props, "mail.debug.auth", false);
      this.referralException = PropUtil.getBooleanProperty(props, this.prefix + ".referralexception", false);
      if (this.capabilities == null)
        capability(); 
      if (hasCapability("IMAP4rev1"))
        this.rev1 = true; 
      this.searchCharsets = new String[2];
      this.searchCharsets[0] = "UTF-8";
      this.searchCharsets[1] = MimeUtility.mimeCharset(
          MimeUtility.getDefaultJavaCharset());
      this.connected = true;
    } finally {
      if (!this.connected)
        disconnect(); 
    } 
  }
  
  public IMAPProtocol(InputStream in, PrintStream out, Properties props, boolean debug) throws IOException {
    super(in, out, props, debug);
    this.name = "imap";
    this
      .noauthdebug = !PropUtil.getBooleanProperty(props, "mail.debug.auth", false);
    if (this.capabilities == null)
      this.capabilities = new HashMap<String, String>(); 
    this.searchCharsets = new String[2];
    this.searchCharsets[0] = "UTF-8";
    this.searchCharsets[1] = MimeUtility.mimeCharset(
        MimeUtility.getDefaultJavaCharset());
    this.connected = true;
  }
  
  public FetchItem[] getFetchItems() {
    return fetchItems;
  }
  
  public void capability() throws ProtocolException {
    Response[] r = command("CAPABILITY", null);
    Response response = r[r.length - 1];
    if (response.isOK()) {
      this.capabilities = new HashMap<String, String>(10);
      this.authmechs = new ArrayList<String>(5);
      for (int i = 0, len = r.length; i < len; i++) {
        if (r[i] instanceof IMAPResponse) {
          IMAPResponse ir = (IMAPResponse)r[i];
          if (ir.keyEquals("CAPABILITY"))
            parseCapabilities(ir); 
        } 
      } 
    } 
    handleResult(response);
  }
  
  protected void setCapabilities(Response r) {
    byte b;
    while ((b = r.readByte()) > 0 && b != 91);
    if (b == 0)
      return; 
    String s = r.readAtom();
    if (!s.equalsIgnoreCase("CAPABILITY"))
      return; 
    this.capabilities = new HashMap<String, String>(10);
    this.authmechs = new ArrayList<String>(5);
    parseCapabilities(r);
  }
  
  protected void parseCapabilities(Response r) {
    String s;
    while ((s = r.readAtom()) != null) {
      if (s.length() == 0) {
        if (r.peekByte() == 93)
          break; 
        r.skipToken();
        continue;
      } 
      this.capabilities.put(s.toUpperCase(Locale.ENGLISH), s);
      if (s.regionMatches(true, 0, "AUTH=", 0, 5)) {
        this.authmechs.add(s.substring(5));
        if (this.logger.isLoggable(Level.FINE))
          this.logger.fine("AUTH: " + s.substring(5)); 
      } 
    } 
  }
  
  protected void processGreeting(Response r) throws ProtocolException {
    if (r.isBYE()) {
      checkReferral(r);
      throw new ConnectionException(this, r);
    } 
    if (r.isOK()) {
      this.referralException = PropUtil.getBooleanProperty(this.props, this.prefix + ".referralexception", false);
      if (this.referralException)
        checkReferral(r); 
      setCapabilities(r);
      return;
    } 
    assert r instanceof IMAPResponse;
    IMAPResponse ir = (IMAPResponse)r;
    if (ir.keyEquals("PREAUTH")) {
      this.authenticated = true;
      setCapabilities(r);
    } else {
      disconnect();
      throw new ConnectionException(this, r);
    } 
  }
  
  private void checkReferral(Response r) throws IMAPReferralException {
    String s = r.getRest();
    if (s.startsWith("[")) {
      int i = s.indexOf(' ');
      if (i > 0 && s.substring(1, i).equalsIgnoreCase("REFERRAL")) {
        String url, msg;
        int j = s.indexOf(']');
        if (j > 0) {
          url = s.substring(i + 1, j);
          msg = s.substring(j + 1).trim();
        } else {
          url = s.substring(i + 1);
          msg = "";
        } 
        if (r.isBYE())
          disconnect(); 
        throw new IMAPReferralException(msg, url);
      } 
    } 
  }
  
  public boolean isAuthenticated() {
    return this.authenticated;
  }
  
  public boolean isREV1() {
    return this.rev1;
  }
  
  protected boolean supportsNonSyncLiterals() {
    return hasCapability("LITERAL+");
  }
  
  public Response readResponse() throws IOException, ProtocolException {
    IMAPResponse r = new IMAPResponse(this);
    if (r.keyEquals("FETCH"))
      r = new FetchResponse(r, getFetchItems()); 
    return r;
  }
  
  public boolean hasCapability(String c) {
    if (c.endsWith("*")) {
      c = c.substring(0, c.length() - 1).toUpperCase(Locale.ENGLISH);
      Iterator<String> it = this.capabilities.keySet().iterator();
      while (it.hasNext()) {
        if (((String)it.next()).startsWith(c))
          return true; 
      } 
      return false;
    } 
    return this.capabilities.containsKey(c.toUpperCase(Locale.ENGLISH));
  }
  
  public Map<String, String> getCapabilities() {
    return this.capabilities;
  }
  
  public void disconnect() {
    super.disconnect();
    this.authenticated = false;
  }
  
  public void noop() throws ProtocolException {
    this.logger.fine("IMAPProtocol noop");
    simpleCommand("NOOP", null);
  }
  
  public void logout() throws ProtocolException {
    try {
      Response[] r = command("LOGOUT", null);
      this.authenticated = false;
      notifyResponseHandlers(r);
    } finally {
      disconnect();
    } 
  }
  
  public void login(String u, String p) throws ProtocolException {
    Argument args = new Argument();
    args.writeString(u);
    args.writeString(p);
    Response[] r = null;
    try {
      if (this.noauthdebug && isTracing()) {
        this.logger.fine("LOGIN command trace suppressed");
        suspendTracing();
      } 
      r = command("LOGIN", args);
    } finally {
      resumeTracing();
    } 
    notifyResponseHandlers(r);
    if (this.noauthdebug && isTracing())
      this.logger.fine("LOGIN command result: " + r[r.length - 1]); 
    handleLoginResult(r[r.length - 1]);
    setCapabilities(r[r.length - 1]);
    this.authenticated = true;
  }
  
  public synchronized void authlogin(String u, String p) throws ProtocolException {
    List<Response> v = new ArrayList<Response>();
    String tag = null;
    Response r = null;
    boolean done = false;
    try {
      if (this.noauthdebug && isTracing()) {
        this.logger.fine("AUTHENTICATE LOGIN command trace suppressed");
        suspendTracing();
      } 
      try {
        tag = writeCommand("AUTHENTICATE LOGIN", null);
      } catch (Exception ex) {
        r = Response.byeResponse(ex);
        done = true;
      } 
      OutputStream os = getOutputStream();
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      OutputStream b64os = new BASE64EncoderStream(bos, 2147483647);
      boolean first = true;
      while (!done) {
        try {
          r = readResponse();
          if (r.isContinuation()) {
            String s;
            if (first) {
              s = u;
              first = false;
            } else {
              s = p;
            } 
            b64os.write(ASCIIUtility.getBytes(s));
            b64os.flush();
            bos.write(CRLF);
            os.write(bos.toByteArray());
            os.flush();
            bos.reset();
          } else if (r.isTagged() && r.getTag().equals(tag)) {
            done = true;
          } else if (r.isBYE()) {
            done = true;
          } 
        } catch (Exception ioex) {
          r = Response.byeResponse(ioex);
          done = true;
        } 
        v.add(r);
      } 
    } finally {
      resumeTracing();
    } 
    Response[] responses = v.<Response>toArray(new Response[v.size()]);
    notifyResponseHandlers(responses);
    if (this.noauthdebug && isTracing())
      this.logger.fine("AUTHENTICATE LOGIN command result: " + r); 
    handleLoginResult(r);
    setCapabilities(r);
    this.authenticated = true;
  }
  
  public synchronized void authplain(String authzid, String u, String p) throws ProtocolException {
    List<Response> v = new ArrayList<Response>();
    String tag = null;
    Response r = null;
    boolean done = false;
    try {
      if (this.noauthdebug && isTracing()) {
        this.logger.fine("AUTHENTICATE PLAIN command trace suppressed");
        suspendTracing();
      } 
      try {
        tag = writeCommand("AUTHENTICATE PLAIN", null);
      } catch (Exception ex) {
        r = Response.byeResponse(ex);
        done = true;
      } 
      OutputStream os = getOutputStream();
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      OutputStream b64os = new BASE64EncoderStream(bos, 2147483647);
      while (!done) {
        try {
          r = readResponse();
          if (r.isContinuation()) {
            String nullByte = "\000";
            String s = ((authzid == null) ? "" : authzid) + "\000" + u + "\000" + p;
            b64os.write(ASCIIUtility.getBytes(s));
            b64os.flush();
            bos.write(CRLF);
            os.write(bos.toByteArray());
            os.flush();
            bos.reset();
          } else if (r.isTagged() && r.getTag().equals(tag)) {
            done = true;
          } else if (r.isBYE()) {
            done = true;
          } 
        } catch (Exception ioex) {
          r = Response.byeResponse(ioex);
          done = true;
        } 
        v.add(r);
      } 
    } finally {
      resumeTracing();
    } 
    Response[] responses = v.<Response>toArray(new Response[v.size()]);
    notifyResponseHandlers(responses);
    if (this.noauthdebug && isTracing())
      this.logger.fine("AUTHENTICATE PLAIN command result: " + r); 
    handleLoginResult(r);
    setCapabilities(r);
    this.authenticated = true;
  }
  
  public synchronized void authntlm(String authzid, String u, String p) throws ProtocolException {
    List<Response> v = new ArrayList<Response>();
    String tag = null;
    Response r = null;
    boolean done = false;
    String type1Msg = null;
    int flags = PropUtil.getIntProperty(this.props, "mail." + this.name + ".auth.ntlm.flags", 0);
    String domain = this.props.getProperty("mail." + this.name + ".auth.ntlm.domain", "");
    Ntlm ntlm = new Ntlm(domain, getLocalHost(), u, p, this.logger);
    try {
      if (this.noauthdebug && isTracing()) {
        this.logger.fine("AUTHENTICATE NTLM command trace suppressed");
        suspendTracing();
      } 
      try {
        tag = writeCommand("AUTHENTICATE NTLM", null);
      } catch (Exception ex) {
        r = Response.byeResponse(ex);
        done = true;
      } 
      OutputStream os = getOutputStream();
      boolean first = true;
      while (!done) {
        try {
          r = readResponse();
          if (r.isContinuation()) {
            String s;
            if (first) {
              s = ntlm.generateType1Msg(flags);
              first = false;
            } else {
              s = ntlm.generateType3Msg(r.getRest());
            } 
            os.write(ASCIIUtility.getBytes(s));
            os.write(CRLF);
            os.flush();
          } else if (r.isTagged() && r.getTag().equals(tag)) {
            done = true;
          } else if (r.isBYE()) {
            done = true;
          } 
        } catch (Exception ioex) {
          r = Response.byeResponse(ioex);
          done = true;
        } 
        v.add(r);
      } 
    } finally {
      resumeTracing();
    } 
    Response[] responses = v.<Response>toArray(new Response[v.size()]);
    notifyResponseHandlers(responses);
    if (this.noauthdebug && isTracing())
      this.logger.fine("AUTHENTICATE NTLM command result: " + r); 
    handleLoginResult(r);
    setCapabilities(r);
    this.authenticated = true;
  }
  
  public synchronized void authoauth2(String u, String p) throws ProtocolException {
    List<Response> v = new ArrayList<Response>();
    String tag = null;
    Response r = null;
    boolean done = false;
    try {
      if (this.noauthdebug && isTracing()) {
        this.logger.fine("AUTHENTICATE XOAUTH2 command trace suppressed");
        suspendTracing();
      } 
      try {
        Argument args = new Argument();
        args.writeAtom("XOAUTH2");
        if (hasCapability("SASL-IR")) {
          String resp = "user=" + u + "\001auth=Bearer " + p + "\001\001";
          byte[] ba = BASE64EncoderStream.encode(
              ASCIIUtility.getBytes(resp));
          String irs = ASCIIUtility.toString(ba, 0, ba.length);
          args.writeAtom(irs);
        } 
        tag = writeCommand("AUTHENTICATE", args);
      } catch (Exception ex) {
        r = Response.byeResponse(ex);
        done = true;
      } 
      OutputStream os = getOutputStream();
      while (!done) {
        try {
          r = readResponse();
          if (r.isContinuation()) {
            String resp = "user=" + u + "\001auth=Bearer " + p + "\001\001";
            byte[] b = BASE64EncoderStream.encode(
                ASCIIUtility.getBytes(resp));
            os.write(b);
            os.write(CRLF);
            os.flush();
          } else if (r.isTagged() && r.getTag().equals(tag)) {
            done = true;
          } else if (r.isBYE()) {
            done = true;
          } 
        } catch (Exception ioex) {
          r = Response.byeResponse(ioex);
          done = true;
        } 
        v.add(r);
      } 
    } finally {
      resumeTracing();
    } 
    Response[] responses = v.<Response>toArray(new Response[v.size()]);
    notifyResponseHandlers(responses);
    if (this.noauthdebug && isTracing())
      this.logger.fine("AUTHENTICATE XOAUTH2 command result: " + r); 
    handleLoginResult(r);
    setCapabilities(r);
    this.authenticated = true;
  }
  
  public void sasllogin(String[] allowed, String realm, String authzid, String u, String p) throws ProtocolException {
    String serviceHost;
    List<String> v;
    boolean useCanonicalHostName = PropUtil.getBooleanProperty(this.props, "mail." + this.name + ".sasl.usecanonicalhostname", false);
    if (useCanonicalHostName) {
      serviceHost = getInetAddress().getCanonicalHostName();
    } else {
      serviceHost = this.host;
    } 
    if (this.saslAuthenticator == null)
      try {
        Class<?> sac = Class.forName("com.sun.mail.imap.protocol.IMAPSaslAuthenticator");
        Constructor<?> c = sac.getConstructor(new Class[] { IMAPProtocol.class, String.class, Properties.class, MailLogger.class, String.class });
        this.saslAuthenticator = (SaslAuthenticator)c.newInstance(new Object[] { this, this.name, this.props, this.logger, serviceHost });
      } catch (Exception ex) {
        this.logger.log(Level.FINE, "Can't load SASL authenticator", ex);
        return;
      }  
    if (allowed != null && allowed.length > 0) {
      v = new ArrayList<String>(allowed.length);
      for (int i = 0; i < allowed.length; i++) {
        if (this.authmechs.contains(allowed[i]))
          v.add(allowed[i]); 
      } 
    } else {
      v = this.authmechs;
    } 
    String[] mechs = v.<String>toArray(new String[v.size()]);
    try {
      if (this.noauthdebug && isTracing()) {
        this.logger.fine("SASL authentication command trace suppressed");
        suspendTracing();
      } 
      if (this.saslAuthenticator.authenticate(mechs, realm, authzid, u, p)) {
        if (this.noauthdebug && isTracing())
          this.logger.fine("SASL authentication succeeded"); 
        this.authenticated = true;
      } else if (this.noauthdebug && isTracing()) {
        this.logger.fine("SASL authentication failed");
      } 
    } finally {
      resumeTracing();
    } 
  }
  
  OutputStream getIMAPOutputStream() {
    return getOutputStream();
  }
  
  protected void handleLoginResult(Response r) throws ProtocolException {
    if (hasCapability("LOGIN-REFERRALS") && (
      !r.isOK() || this.referralException))
      checkReferral(r); 
    handleResult(r);
  }
  
  public void proxyauth(String u) throws ProtocolException {
    Argument args = new Argument();
    args.writeString(u);
    simpleCommand("PROXYAUTH", args);
    this.proxyAuthUser = u;
  }
  
  public String getProxyAuthUser() {
    return this.proxyAuthUser;
  }
  
  public void unauthenticate() throws ProtocolException {
    if (!hasCapability("X-UNAUTHENTICATE"))
      throw new BadCommandException("UNAUTHENTICATE not supported"); 
    simpleCommand("UNAUTHENTICATE", null);
    this.authenticated = false;
  }
  
  @Deprecated
  public void id(String guid) throws ProtocolException {
    Map<String, String> gmap = new HashMap<String, String>();
    gmap.put("GUID", guid);
    id(gmap);
  }
  
  public void startTLS() throws ProtocolException {
    try {
      startTLS("STARTTLS");
    } catch (ProtocolException pex) {
      this.logger.log(Level.FINE, "STARTTLS ProtocolException", pex);
      throw pex;
    } catch (Exception ex) {
      this.logger.log(Level.FINE, "STARTTLS Exception", ex);
      Response[] r = { Response.byeResponse(ex) };
      notifyResponseHandlers(r);
      disconnect();
      throw new ProtocolException("STARTTLS failure", ex);
    } 
  }
  
  public void compress() throws ProtocolException {
    try {
      startCompression("COMPRESS DEFLATE");
    } catch (ProtocolException pex) {
      this.logger.log(Level.FINE, "COMPRESS ProtocolException", pex);
      throw pex;
    } catch (Exception ex) {
      this.logger.log(Level.FINE, "COMPRESS Exception", ex);
      Response[] r = { Response.byeResponse(ex) };
      notifyResponseHandlers(r);
      disconnect();
      throw new ProtocolException("COMPRESS failure", ex);
    } 
  }
  
  public MailboxInfo select(String mbox) throws ProtocolException {
    return select(mbox, (ResyncData)null);
  }
  
  public MailboxInfo select(String mbox, ResyncData rd) throws ProtocolException {
    mbox = BASE64MailboxEncoder.encode(mbox);
    Argument args = new Argument();
    args.writeString(mbox);
    if (rd != null)
      if (rd == ResyncData.CONDSTORE) {
        if (!hasCapability("CONDSTORE"))
          throw new BadCommandException("CONDSTORE not supported"); 
        args.writeArgument((new Argument()).writeAtom("CONDSTORE"));
      } else {
        if (!hasCapability("QRESYNC"))
          throw new BadCommandException("QRESYNC not supported"); 
        args.writeArgument(resyncArgs(rd));
      }  
    Response[] r = command("SELECT", args);
    MailboxInfo minfo = new MailboxInfo(r);
    notifyResponseHandlers(r);
    Response response = r[r.length - 1];
    if (response.isOK())
      if (response.toString().indexOf("READ-ONLY") != -1) {
        minfo.mode = 1;
      } else {
        minfo.mode = 2;
      }  
    handleResult(response);
    return minfo;
  }
  
  public MailboxInfo examine(String mbox) throws ProtocolException {
    return examine(mbox, (ResyncData)null);
  }
  
  public MailboxInfo examine(String mbox, ResyncData rd) throws ProtocolException {
    mbox = BASE64MailboxEncoder.encode(mbox);
    Argument args = new Argument();
    args.writeString(mbox);
    if (rd != null)
      if (rd == ResyncData.CONDSTORE) {
        if (!hasCapability("CONDSTORE"))
          throw new BadCommandException("CONDSTORE not supported"); 
        args.writeArgument((new Argument()).writeAtom("CONDSTORE"));
      } else {
        if (!hasCapability("QRESYNC"))
          throw new BadCommandException("QRESYNC not supported"); 
        args.writeArgument(resyncArgs(rd));
      }  
    Response[] r = command("EXAMINE", args);
    MailboxInfo minfo = new MailboxInfo(r);
    minfo.mode = 1;
    notifyResponseHandlers(r);
    handleResult(r[r.length - 1]);
    return minfo;
  }
  
  private static Argument resyncArgs(ResyncData rd) {
    Argument cmd = new Argument();
    cmd.writeAtom("QRESYNC");
    Argument args = new Argument();
    args.writeNumber(rd.getUIDValidity());
    args.writeNumber(rd.getModSeq());
    UIDSet[] uids = Utility.getResyncUIDSet(rd);
    if (uids != null)
      args.writeString(UIDSet.toString(uids)); 
    cmd.writeArgument(args);
    return cmd;
  }
  
  public void enable(String cap) throws ProtocolException {
    if (!hasCapability("ENABLE"))
      throw new BadCommandException("ENABLE not supported"); 
    Argument args = new Argument();
    args.writeAtom(cap);
    simpleCommand("ENABLE", args);
    if (this.enabled == null)
      this.enabled = new HashSet<String>(); 
    this.enabled.add(cap.toUpperCase(Locale.ENGLISH));
  }
  
  public boolean isEnabled(String cap) {
    if (this.enabled == null)
      return false; 
    return this.enabled.contains(cap.toUpperCase(Locale.ENGLISH));
  }
  
  public void unselect() throws ProtocolException {
    if (!hasCapability("UNSELECT"))
      throw new BadCommandException("UNSELECT not supported"); 
    simpleCommand("UNSELECT", null);
  }
  
  public Status status(String mbox, String[] items) throws ProtocolException {
    if (!isREV1() && !hasCapability("IMAP4SUNVERSION"))
      throw new BadCommandException("STATUS not supported"); 
    mbox = BASE64MailboxEncoder.encode(mbox);
    Argument args = new Argument();
    args.writeString(mbox);
    Argument itemArgs = new Argument();
    if (items == null)
      items = Status.standardItems; 
    for (int i = 0, len = items.length; i < len; i++)
      itemArgs.writeAtom(items[i]); 
    args.writeArgument(itemArgs);
    Response[] r = command("STATUS", args);
    Status status = null;
    Response response = r[r.length - 1];
    if (response.isOK())
      for (int j = 0, k = r.length; j < k; j++) {
        if (r[j] instanceof IMAPResponse) {
          IMAPResponse ir = (IMAPResponse)r[j];
          if (ir.keyEquals("STATUS")) {
            if (status == null) {
              status = new Status(ir);
            } else {
              Status.add(status, new Status(ir));
            } 
            r[j] = null;
          } 
        } 
      }  
    notifyResponseHandlers(r);
    handleResult(response);
    return status;
  }
  
  public void create(String mbox) throws ProtocolException {
    mbox = BASE64MailboxEncoder.encode(mbox);
    Argument args = new Argument();
    args.writeString(mbox);
    simpleCommand("CREATE", args);
  }
  
  public void delete(String mbox) throws ProtocolException {
    mbox = BASE64MailboxEncoder.encode(mbox);
    Argument args = new Argument();
    args.writeString(mbox);
    simpleCommand("DELETE", args);
  }
  
  public void rename(String o, String n) throws ProtocolException {
    o = BASE64MailboxEncoder.encode(o);
    n = BASE64MailboxEncoder.encode(n);
    Argument args = new Argument();
    args.writeString(o);
    args.writeString(n);
    simpleCommand("RENAME", args);
  }
  
  public void subscribe(String mbox) throws ProtocolException {
    Argument args = new Argument();
    mbox = BASE64MailboxEncoder.encode(mbox);
    args.writeString(mbox);
    simpleCommand("SUBSCRIBE", args);
  }
  
  public void unsubscribe(String mbox) throws ProtocolException {
    Argument args = new Argument();
    mbox = BASE64MailboxEncoder.encode(mbox);
    args.writeString(mbox);
    simpleCommand("UNSUBSCRIBE", args);
  }
  
  public ListInfo[] list(String ref, String pattern) throws ProtocolException {
    return doList("LIST", ref, pattern);
  }
  
  public ListInfo[] lsub(String ref, String pattern) throws ProtocolException {
    return doList("LSUB", ref, pattern);
  }
  
  protected ListInfo[] doList(String cmd, String ref, String pat) throws ProtocolException {
    ref = BASE64MailboxEncoder.encode(ref);
    pat = BASE64MailboxEncoder.encode(pat);
    Argument args = new Argument();
    args.writeString(ref);
    args.writeString(pat);
    Response[] r = command(cmd, args);
    ListInfo[] linfo = null;
    Response response = r[r.length - 1];
    if (response.isOK()) {
      List<ListInfo> v = new ArrayList<ListInfo>(1);
      for (int i = 0, len = r.length; i < len; i++) {
        if (r[i] instanceof IMAPResponse) {
          IMAPResponse ir = (IMAPResponse)r[i];
          if (ir.keyEquals(cmd)) {
            v.add(new ListInfo(ir));
            r[i] = null;
          } 
        } 
      } 
      if (v.size() > 0)
        linfo = v.<ListInfo>toArray(new ListInfo[v.size()]); 
    } 
    notifyResponseHandlers(r);
    handleResult(response);
    return linfo;
  }
  
  public void append(String mbox, Flags f, Date d, Literal data) throws ProtocolException {
    appenduid(mbox, f, d, data, false);
  }
  
  public AppendUID appenduid(String mbox, Flags f, Date d, Literal data) throws ProtocolException {
    return appenduid(mbox, f, d, data, true);
  }
  
  public AppendUID appenduid(String mbox, Flags f, Date d, Literal data, boolean uid) throws ProtocolException {
    mbox = BASE64MailboxEncoder.encode(mbox);
    Argument args = new Argument();
    args.writeString(mbox);
    if (f != null) {
      if (f.contains(Flags.Flag.RECENT)) {
        f = new Flags(f);
        f.remove(Flags.Flag.RECENT);
      } 
      args.writeAtom(createFlagList(f));
    } 
    if (d != null)
      args.writeString(INTERNALDATE.format(d)); 
    args.writeBytes(data);
    Response[] r = command("APPEND", args);
    notifyResponseHandlers(r);
    handleResult(r[r.length - 1]);
    if (uid)
      return getAppendUID(r[r.length - 1]); 
    return null;
  }
  
  private AppendUID getAppendUID(Response r) {
    if (!r.isOK())
      return null; 
    byte b;
    while ((b = r.readByte()) > 0 && b != 91);
    if (b == 0)
      return null; 
    String s = r.readAtom();
    if (!s.equalsIgnoreCase("APPENDUID"))
      return null; 
    long uidvalidity = r.readLong();
    long uid = r.readLong();
    return new AppendUID(uidvalidity, uid);
  }
  
  public void check() throws ProtocolException {
    simpleCommand("CHECK", null);
  }
  
  public void close() throws ProtocolException {
    simpleCommand("CLOSE", null);
  }
  
  public void expunge() throws ProtocolException {
    simpleCommand("EXPUNGE", null);
  }
  
  public void uidexpunge(UIDSet[] set) throws ProtocolException {
    if (!hasCapability("UIDPLUS"))
      throw new BadCommandException("UID EXPUNGE not supported"); 
    simpleCommand("UID EXPUNGE " + UIDSet.toString(set), null);
  }
  
  public BODYSTRUCTURE fetchBodyStructure(int msgno) throws ProtocolException {
    Response[] r = fetch(msgno, "BODYSTRUCTURE");
    notifyResponseHandlers(r);
    Response response = r[r.length - 1];
    if (response.isOK())
      return FetchResponse.<BODYSTRUCTURE>getItem(r, msgno, BODYSTRUCTURE.class); 
    if (response.isNO())
      return null; 
    handleResult(response);
    return null;
  }
  
  public BODY peekBody(int msgno, String section) throws ProtocolException {
    return fetchBody(msgno, section, true);
  }
  
  public BODY fetchBody(int msgno, String section) throws ProtocolException {
    return fetchBody(msgno, section, false);
  }
  
  protected BODY fetchBody(int msgno, String section, boolean peek) throws ProtocolException {
    if (section == null)
      section = ""; 
    String body = (peek ? "BODY.PEEK[" : "BODY[") + section + "]";
    return fetchSectionBody(msgno, section, body);
  }
  
  public BODY peekBody(int msgno, String section, int start, int size) throws ProtocolException {
    return fetchBody(msgno, section, start, size, true, (ByteArray)null);
  }
  
  public BODY fetchBody(int msgno, String section, int start, int size) throws ProtocolException {
    return fetchBody(msgno, section, start, size, false, (ByteArray)null);
  }
  
  public BODY peekBody(int msgno, String section, int start, int size, ByteArray ba) throws ProtocolException {
    return fetchBody(msgno, section, start, size, true, ba);
  }
  
  public BODY fetchBody(int msgno, String section, int start, int size, ByteArray ba) throws ProtocolException {
    return fetchBody(msgno, section, start, size, false, ba);
  }
  
  protected BODY fetchBody(int msgno, String section, int start, int size, boolean peek, ByteArray ba) throws ProtocolException {
    this.ba = ba;
    if (section == null)
      section = ""; 
    String body = (peek ? "BODY.PEEK[" : "BODY[") + section + "]<" + String.valueOf(start) + "." + String.valueOf(size) + ">";
    return fetchSectionBody(msgno, section, body);
  }
  
  protected BODY fetchSectionBody(int msgno, String section, String body) throws ProtocolException {
    Response[] r = fetch(msgno, body);
    notifyResponseHandlers(r);
    Response response = r[r.length - 1];
    if (response.isOK()) {
      List<BODY> bl = FetchResponse.getItems(r, msgno, BODY.class);
      if (bl.size() == 1)
        return bl.get(0); 
      if (this.logger.isLoggable(Level.FINEST))
        this.logger.finest("got " + bl.size() + " BODY responses for section " + section); 
      for (BODY br : bl) {
        if (this.logger.isLoggable(Level.FINEST))
          this.logger.finest("got BODY section " + br.getSection()); 
        if (br.getSection().equalsIgnoreCase(section))
          return br; 
      } 
      return null;
    } 
    if (response.isNO())
      return null; 
    handleResult(response);
    return null;
  }
  
  protected ByteArray getResponseBuffer() {
    ByteArray ret = this.ba;
    this.ba = null;
    return ret;
  }
  
  public RFC822DATA fetchRFC822(int msgno, String what) throws ProtocolException {
    Response[] r = fetch(msgno, (what == null) ? "RFC822" : ("RFC822." + what));
    notifyResponseHandlers(r);
    Response response = r[r.length - 1];
    if (response.isOK())
      return FetchResponse.<RFC822DATA>getItem(r, msgno, RFC822DATA.class); 
    if (response.isNO())
      return null; 
    handleResult(response);
    return null;
  }
  
  public Flags fetchFlags(int msgno) throws ProtocolException {
    Flags flags = null;
    Response[] r = fetch(msgno, "FLAGS");
    for (int i = 0, len = r.length; i < len; i++) {
      if (r[i] != null && r[i] instanceof FetchResponse && ((FetchResponse)r[i])
        
        .getNumber() == msgno) {
        FetchResponse fr = (FetchResponse)r[i];
        if ((flags = fr.<Item>getItem((Class)FLAGS.class)) != null) {
          r[i] = null;
          break;
        } 
      } 
    } 
    notifyResponseHandlers(r);
    handleResult(r[r.length - 1]);
    return flags;
  }
  
  public UID fetchUID(int msgno) throws ProtocolException {
    Response[] r = fetch(msgno, "UID");
    notifyResponseHandlers(r);
    Response response = r[r.length - 1];
    if (response.isOK())
      return FetchResponse.<UID>getItem(r, msgno, UID.class); 
    if (response.isNO())
      return null; 
    handleResult(response);
    return null;
  }
  
  public MODSEQ fetchMODSEQ(int msgno) throws ProtocolException {
    Response[] r = fetch(msgno, "MODSEQ");
    notifyResponseHandlers(r);
    Response response = r[r.length - 1];
    if (response.isOK())
      return FetchResponse.<MODSEQ>getItem(r, msgno, MODSEQ.class); 
    if (response.isNO())
      return null; 
    handleResult(response);
    return null;
  }
  
  public void fetchSequenceNumber(long uid) throws ProtocolException {
    Response[] r = fetch(String.valueOf(uid), "UID", true);
    notifyResponseHandlers(r);
    handleResult(r[r.length - 1]);
  }
  
  public long[] fetchSequenceNumbers(long start, long end) throws ProtocolException {
    Response[] r = fetch(String.valueOf(start) + ":" + ((end == -1L) ? "*" : 
        
        String.valueOf(end)), "UID", true);
    List<UID> v = new ArrayList<UID>();
    for (int i = 0, len = r.length; i < len; i++) {
      if (r[i] != null && r[i] instanceof FetchResponse) {
        FetchResponse fr = (FetchResponse)r[i];
        UID u;
        if ((u = fr.<Item>getItem(UID.class)) != null)
          v.add(u); 
      } 
    } 
    notifyResponseHandlers(r);
    handleResult(r[r.length - 1]);
    long[] lv = new long[v.size()];
    for (int j = 0; j < v.size(); j++)
      lv[j] = ((UID)v.get(j)).uid; 
    return lv;
  }
  
  public void fetchSequenceNumbers(long[] uids) throws ProtocolException {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < uids.length; i++) {
      if (i > 0)
        sb.append(","); 
      sb.append(String.valueOf(uids[i]));
    } 
    Response[] r = fetch(sb.toString(), "UID", true);
    notifyResponseHandlers(r);
    handleResult(r[r.length - 1]);
  }
  
  public int[] uidfetchChangedSince(long start, long end, long modseq) throws ProtocolException {
    String msgSequence = String.valueOf(start) + ":" + ((end == -1L) ? "*" : String.valueOf(end));
    Response[] r = command("UID FETCH " + msgSequence + " (FLAGS) (CHANGEDSINCE " + 
        String.valueOf(modseq) + ")", null);
    List<Integer> v = new ArrayList<Integer>();
    for (int i = 0, len = r.length; i < len; i++) {
      if (r[i] != null && r[i] instanceof FetchResponse) {
        FetchResponse fr = (FetchResponse)r[i];
        v.add(Integer.valueOf(fr.getNumber()));
      } 
    } 
    notifyResponseHandlers(r);
    handleResult(r[r.length - 1]);
    int vsize = v.size();
    int[] matches = new int[vsize];
    for (int j = 0; j < vsize; j++)
      matches[j] = ((Integer)v.get(j)).intValue(); 
    return matches;
  }
  
  public Response[] fetch(MessageSet[] msgsets, String what) throws ProtocolException {
    return fetch(MessageSet.toString(msgsets), what, false);
  }
  
  public Response[] fetch(int start, int end, String what) throws ProtocolException {
    return fetch(String.valueOf(start) + ":" + String.valueOf(end), what, false);
  }
  
  public Response[] fetch(int msg, String what) throws ProtocolException {
    return fetch(String.valueOf(msg), what, false);
  }
  
  private Response[] fetch(String msgSequence, String what, boolean uid) throws ProtocolException {
    if (uid)
      return command("UID FETCH " + msgSequence + " (" + what + ")", null); 
    return command("FETCH " + msgSequence + " (" + what + ")", null);
  }
  
  public void copy(MessageSet[] msgsets, String mbox) throws ProtocolException {
    copyuid(MessageSet.toString(msgsets), mbox, false);
  }
  
  public void copy(int start, int end, String mbox) throws ProtocolException {
    copyuid(String.valueOf(start) + ":" + String.valueOf(end), mbox, false);
  }
  
  public CopyUID copyuid(MessageSet[] msgsets, String mbox) throws ProtocolException {
    return copyuid(MessageSet.toString(msgsets), mbox, true);
  }
  
  public CopyUID copyuid(int start, int end, String mbox) throws ProtocolException {
    return copyuid(String.valueOf(start) + ":" + String.valueOf(end), mbox, true);
  }
  
  private CopyUID copyuid(String msgSequence, String mbox, boolean uid) throws ProtocolException {
    if (uid && !hasCapability("UIDPLUS"))
      throw new BadCommandException("UIDPLUS not supported"); 
    mbox = BASE64MailboxEncoder.encode(mbox);
    Argument args = new Argument();
    args.writeAtom(msgSequence);
    args.writeString(mbox);
    Response[] r = command("COPY", args);
    notifyResponseHandlers(r);
    handleResult(r[r.length - 1]);
    if (uid)
      return getCopyUID(r); 
    return null;
  }
  
  public void move(MessageSet[] msgsets, String mbox) throws ProtocolException {
    moveuid(MessageSet.toString(msgsets), mbox, false);
  }
  
  public void move(int start, int end, String mbox) throws ProtocolException {
    moveuid(String.valueOf(start) + ":" + String.valueOf(end), mbox, false);
  }
  
  public CopyUID moveuid(MessageSet[] msgsets, String mbox) throws ProtocolException {
    return moveuid(MessageSet.toString(msgsets), mbox, true);
  }
  
  public CopyUID moveuid(int start, int end, String mbox) throws ProtocolException {
    return moveuid(String.valueOf(start) + ":" + String.valueOf(end), mbox, true);
  }
  
  private CopyUID moveuid(String msgSequence, String mbox, boolean uid) throws ProtocolException {
    if (!hasCapability("MOVE"))
      throw new BadCommandException("MOVE not supported"); 
    if (uid && !hasCapability("UIDPLUS"))
      throw new BadCommandException("UIDPLUS not supported"); 
    mbox = BASE64MailboxEncoder.encode(mbox);
    Argument args = new Argument();
    args.writeAtom(msgSequence);
    args.writeString(mbox);
    Response[] r = command("MOVE", args);
    notifyResponseHandlers(r);
    handleResult(r[r.length - 1]);
    if (uid)
      return getCopyUID(r); 
    return null;
  }
  
  protected CopyUID getCopyUID(Response[] rr) {
    for (int i = rr.length - 1; i >= 0; i--) {
      Response r = rr[i];
      if (r != null && r.isOK()) {
        byte b;
        while ((b = r.readByte()) > 0 && b != 91);
        if (b != 0) {
          String s = r.readAtom();
          if (s.equalsIgnoreCase("COPYUID")) {
            long uidvalidity = r.readLong();
            String src = r.readAtom();
            String dst = r.readAtom();
            return new CopyUID(uidvalidity, 
                UIDSet.parseUIDSets(src), UIDSet.parseUIDSets(dst));
          } 
        } 
      } 
    } 
    return null;
  }
  
  public void storeFlags(MessageSet[] msgsets, Flags flags, boolean set) throws ProtocolException {
    storeFlags(MessageSet.toString(msgsets), flags, set);
  }
  
  public void storeFlags(int start, int end, Flags flags, boolean set) throws ProtocolException {
    storeFlags(String.valueOf(start) + ":" + String.valueOf(end), flags, set);
  }
  
  public void storeFlags(int msg, Flags flags, boolean set) throws ProtocolException {
    storeFlags(String.valueOf(msg), flags, set);
  }
  
  private void storeFlags(String msgset, Flags flags, boolean set) throws ProtocolException {
    Response[] r;
    if (set) {
      r = command("STORE " + msgset + " +FLAGS " + 
          createFlagList(flags), null);
    } else {
      r = command("STORE " + msgset + " -FLAGS " + 
          createFlagList(flags), null);
    } 
    notifyResponseHandlers(r);
    handleResult(r[r.length - 1]);
  }
  
  protected String createFlagList(Flags flags) {
    StringBuffer sb = new StringBuffer();
    sb.append("(");
    Flags.Flag[] sf = flags.getSystemFlags();
    boolean first = true;
    for (int i = 0; i < sf.length; i++) {
      String s;
      Flags.Flag f = sf[i];
      if (f == Flags.Flag.ANSWERED) {
        s = "\\Answered";
      } else if (f == Flags.Flag.DELETED) {
        s = "\\Deleted";
      } else if (f == Flags.Flag.DRAFT) {
        s = "\\Draft";
      } else if (f == Flags.Flag.FLAGGED) {
        s = "\\Flagged";
      } else if (f == Flags.Flag.RECENT) {
        s = "\\Recent";
      } else if (f == Flags.Flag.SEEN) {
        s = "\\Seen";
      } else {
        continue;
      } 
      if (first) {
        first = false;
      } else {
        sb.append(' ');
      } 
      sb.append(s);
      continue;
    } 
    String[] uf = flags.getUserFlags();
    for (int j = 0; j < uf.length; j++) {
      if (first) {
        first = false;
      } else {
        sb.append(' ');
      } 
      sb.append(uf[j]);
    } 
    sb.append(")");
    return sb.toString();
  }
  
  public int[] search(MessageSet[] msgsets, SearchTerm term) throws ProtocolException, SearchException {
    return search(MessageSet.toString(msgsets), term);
  }
  
  public int[] search(SearchTerm term) throws ProtocolException, SearchException {
    return search("ALL", term);
  }
  
  private int[] search(String msgSequence, SearchTerm term) throws ProtocolException, SearchException {
    if (SearchSequence.isAscii(term))
      try {
        return issueSearch(msgSequence, term, (String)null);
      } catch (IOException iOException) {} 
    for (int i = 0; i < this.searchCharsets.length; i++) {
      if (this.searchCharsets[i] != null)
        try {
          return issueSearch(msgSequence, term, this.searchCharsets[i]);
        } catch (CommandFailedException cfx) {
          this.searchCharsets[i] = null;
        } catch (IOException ioex) {
        
        } catch (ProtocolException pex) {
          throw pex;
        } catch (SearchException sex) {
          throw sex;
        }  
    } 
    throw new SearchException("Search failed");
  }
  
  private int[] issueSearch(String msgSequence, SearchTerm term, String charset) throws ProtocolException, SearchException, IOException {
    Response[] r;
    Argument args = getSearchSequence().generateSequence(term, (charset == null) ? null : 
        
        MimeUtility.javaCharset(charset));
    args.writeAtom(msgSequence);
    if (charset == null) {
      r = command("SEARCH", args);
    } else {
      r = command("SEARCH CHARSET " + charset, args);
    } 
    Response response = r[r.length - 1];
    int[] matches = null;
    if (response.isOK()) {
      List<Integer> v = new ArrayList<Integer>();
      for (int i = 0, len = r.length; i < len; i++) {
        if (r[i] instanceof IMAPResponse) {
          IMAPResponse ir = (IMAPResponse)r[i];
          if (ir.keyEquals("SEARCH")) {
            int num;
            while ((num = ir.readNumber()) != -1)
              v.add(Integer.valueOf(num)); 
            r[i] = null;
          } 
        } 
      } 
      int vsize = v.size();
      matches = new int[vsize];
      for (int j = 0; j < vsize; j++)
        matches[j] = ((Integer)v.get(j)).intValue(); 
    } 
    notifyResponseHandlers(r);
    handleResult(response);
    return matches;
  }
  
  protected SearchSequence getSearchSequence() {
    if (this.searchSequence == null)
      this.searchSequence = new SearchSequence(); 
    return this.searchSequence;
  }
  
  public int[] sort(SortTerm[] term, SearchTerm sterm) throws ProtocolException, SearchException {
    if (!hasCapability("SORT*"))
      throw new BadCommandException("SORT not supported"); 
    if (term == null || term.length == 0)
      throw new BadCommandException("Must have at least one sort term"); 
    Argument args = new Argument();
    Argument sargs = new Argument();
    for (int i = 0; i < term.length; i++)
      sargs.writeAtom(term[i].toString()); 
    args.writeArgument(sargs);
    args.writeAtom("UTF-8");
    if (sterm != null) {
      try {
        args.append(
            getSearchSequence().generateSequence(sterm, "UTF-8"));
      } catch (IOException ioex) {
        throw new SearchException(ioex.toString());
      } 
    } else {
      args.writeAtom("ALL");
    } 
    Response[] r = command("SORT", args);
    Response response = r[r.length - 1];
    int[] matches = null;
    if (response.isOK()) {
      List<Integer> v = new ArrayList<Integer>();
      for (int j = 0, len = r.length; j < len; j++) {
        if (r[j] instanceof IMAPResponse) {
          IMAPResponse ir = (IMAPResponse)r[j];
          if (ir.keyEquals("SORT")) {
            int num;
            while ((num = ir.readNumber()) != -1)
              v.add(Integer.valueOf(num)); 
            r[j] = null;
          } 
        } 
      } 
      int vsize = v.size();
      matches = new int[vsize];
      for (int k = 0; k < vsize; k++)
        matches[k] = ((Integer)v.get(k)).intValue(); 
    } 
    notifyResponseHandlers(r);
    handleResult(response);
    return matches;
  }
  
  public Namespaces namespace() throws ProtocolException {
    if (!hasCapability("NAMESPACE"))
      throw new BadCommandException("NAMESPACE not supported"); 
    Response[] r = command("NAMESPACE", null);
    Namespaces namespace = null;
    Response response = r[r.length - 1];
    if (response.isOK())
      for (int i = 0, len = r.length; i < len; i++) {
        if (r[i] instanceof IMAPResponse) {
          IMAPResponse ir = (IMAPResponse)r[i];
          if (ir.keyEquals("NAMESPACE")) {
            if (namespace == null)
              namespace = new Namespaces(ir); 
            r[i] = null;
          } 
        } 
      }  
    notifyResponseHandlers(r);
    handleResult(response);
    return namespace;
  }
  
  public Quota[] getQuotaRoot(String mbox) throws ProtocolException {
    if (!hasCapability("QUOTA"))
      throw new BadCommandException("GETQUOTAROOT not supported"); 
    mbox = BASE64MailboxEncoder.encode(mbox);
    Argument args = new Argument();
    args.writeString(mbox);
    Response[] r = command("GETQUOTAROOT", args);
    Response response = r[r.length - 1];
    Map<String, Quota> tab = new HashMap<String, Quota>();
    if (response.isOK())
      for (int i = 0, len = r.length; i < len; i++) {
        if (r[i] instanceof IMAPResponse) {
          IMAPResponse ir = (IMAPResponse)r[i];
          if (ir.keyEquals("QUOTAROOT")) {
            ir.readAtomString();
            String root = null;
            while ((root = ir.readAtomString()) != null && root
              .length() > 0)
              tab.put(root, new Quota(root)); 
            r[i] = null;
          } else if (ir.keyEquals("QUOTA")) {
            Quota quota = parseQuota(ir);
            Quota q = tab.get(quota.quotaRoot);
            if (q != null && q.resources != null) {
              int newl = q.resources.length + quota.resources.length;
              Quota.Resource[] newr = new Quota.Resource[newl];
              System.arraycopy(q.resources, 0, newr, 0, q.resources.length);
              System.arraycopy(quota.resources, 0, newr, q.resources.length, quota.resources.length);
              quota.resources = newr;
            } 
            tab.put(quota.quotaRoot, quota);
            r[i] = null;
          } 
        } 
      }  
    notifyResponseHandlers(r);
    handleResult(response);
    return (Quota[])tab.values().toArray((Object[])new Quota[tab.size()]);
  }
  
  public Quota[] getQuota(String root) throws ProtocolException {
    if (!hasCapability("QUOTA"))
      throw new BadCommandException("QUOTA not supported"); 
    Argument args = new Argument();
    args.writeString(root);
    Response[] r = command("GETQUOTA", args);
    Quota quota = null;
    List<Quota> v = new ArrayList<Quota>();
    Response response = r[r.length - 1];
    if (response.isOK())
      for (int i = 0, len = r.length; i < len; i++) {
        if (r[i] instanceof IMAPResponse) {
          IMAPResponse ir = (IMAPResponse)r[i];
          if (ir.keyEquals("QUOTA")) {
            quota = parseQuota(ir);
            v.add(quota);
            r[i] = null;
          } 
        } 
      }  
    notifyResponseHandlers(r);
    handleResult(response);
    return v.<Quota>toArray(new Quota[v.size()]);
  }
  
  public void setQuota(Quota quota) throws ProtocolException {
    if (!hasCapability("QUOTA"))
      throw new BadCommandException("QUOTA not supported"); 
    Argument args = new Argument();
    args.writeString(quota.quotaRoot);
    Argument qargs = new Argument();
    if (quota.resources != null)
      for (int i = 0; i < quota.resources.length; i++) {
        qargs.writeAtom((quota.resources[i]).name);
        qargs.writeNumber((quota.resources[i]).limit);
      }  
    args.writeArgument(qargs);
    Response[] r = command("SETQUOTA", args);
    Response response = r[r.length - 1];
    notifyResponseHandlers(r);
    handleResult(response);
  }
  
  private Quota parseQuota(Response r) throws ParsingException {
    String quotaRoot = r.readAtomString();
    Quota q = new Quota(quotaRoot);
    r.skipSpaces();
    if (r.readByte() != 40)
      throw new ParsingException("parse error in QUOTA"); 
    List<Quota.Resource> v = new ArrayList<Quota.Resource>();
    while (r.peekByte() != 41) {
      String name = r.readAtom();
      if (name != null) {
        long usage = r.readLong();
        long limit = r.readLong();
        Quota.Resource res = new Quota.Resource(name, usage, limit);
        v.add(res);
      } 
    } 
    r.readByte();
    q.resources = v.<Quota.Resource>toArray(new Quota.Resource[v.size()]);
    return q;
  }
  
  public void setACL(String mbox, char modifier, ACL acl) throws ProtocolException {
    if (!hasCapability("ACL"))
      throw new BadCommandException("ACL not supported"); 
    mbox = BASE64MailboxEncoder.encode(mbox);
    Argument args = new Argument();
    args.writeString(mbox);
    args.writeString(acl.getName());
    String rights = acl.getRights().toString();
    if (modifier == '+' || modifier == '-')
      rights = modifier + rights; 
    args.writeString(rights);
    Response[] r = command("SETACL", args);
    Response response = r[r.length - 1];
    notifyResponseHandlers(r);
    handleResult(response);
  }
  
  public void deleteACL(String mbox, String user) throws ProtocolException {
    if (!hasCapability("ACL"))
      throw new BadCommandException("ACL not supported"); 
    mbox = BASE64MailboxEncoder.encode(mbox);
    Argument args = new Argument();
    args.writeString(mbox);
    args.writeString(user);
    Response[] r = command("DELETEACL", args);
    Response response = r[r.length - 1];
    notifyResponseHandlers(r);
    handleResult(response);
  }
  
  public ACL[] getACL(String mbox) throws ProtocolException {
    if (!hasCapability("ACL"))
      throw new BadCommandException("ACL not supported"); 
    mbox = BASE64MailboxEncoder.encode(mbox);
    Argument args = new Argument();
    args.writeString(mbox);
    Response[] r = command("GETACL", args);
    Response response = r[r.length - 1];
    List<ACL> v = new ArrayList<ACL>();
    if (response.isOK())
      for (int i = 0, len = r.length; i < len; i++) {
        if (r[i] instanceof IMAPResponse) {
          IMAPResponse ir = (IMAPResponse)r[i];
          if (ir.keyEquals("ACL")) {
            ir.readAtomString();
            String name = null;
            while ((name = ir.readAtomString()) != null) {
              String rights = ir.readAtomString();
              if (rights == null)
                break; 
              ACL acl = new ACL(name, new Rights(rights));
              v.add(acl);
            } 
            r[i] = null;
          } 
        } 
      }  
    notifyResponseHandlers(r);
    handleResult(response);
    return v.<ACL>toArray(new ACL[v.size()]);
  }
  
  public Rights[] listRights(String mbox, String user) throws ProtocolException {
    if (!hasCapability("ACL"))
      throw new BadCommandException("ACL not supported"); 
    mbox = BASE64MailboxEncoder.encode(mbox);
    Argument args = new Argument();
    args.writeString(mbox);
    args.writeString(user);
    Response[] r = command("LISTRIGHTS", args);
    Response response = r[r.length - 1];
    List<Rights> v = new ArrayList<Rights>();
    if (response.isOK())
      for (int i = 0, len = r.length; i < len; i++) {
        if (r[i] instanceof IMAPResponse) {
          IMAPResponse ir = (IMAPResponse)r[i];
          if (ir.keyEquals("LISTRIGHTS")) {
            ir.readAtomString();
            ir.readAtomString();
            String rights;
            while ((rights = ir.readAtomString()) != null)
              v.add(new Rights(rights)); 
            r[i] = null;
          } 
        } 
      }  
    notifyResponseHandlers(r);
    handleResult(response);
    return v.<Rights>toArray(new Rights[v.size()]);
  }
  
  public Rights myRights(String mbox) throws ProtocolException {
    if (!hasCapability("ACL"))
      throw new BadCommandException("ACL not supported"); 
    mbox = BASE64MailboxEncoder.encode(mbox);
    Argument args = new Argument();
    args.writeString(mbox);
    Response[] r = command("MYRIGHTS", args);
    Response response = r[r.length - 1];
    Rights rights = null;
    if (response.isOK())
      for (int i = 0, len = r.length; i < len; i++) {
        if (r[i] instanceof IMAPResponse) {
          IMAPResponse ir = (IMAPResponse)r[i];
          if (ir.keyEquals("MYRIGHTS")) {
            ir.readAtomString();
            String rs = ir.readAtomString();
            if (rights == null)
              rights = new Rights(rs); 
            r[i] = null;
          } 
        } 
      }  
    notifyResponseHandlers(r);
    handleResult(response);
    return rights;
  }
  
  public synchronized void idleStart() throws ProtocolException {
    if (!hasCapability("IDLE"))
      throw new BadCommandException("IDLE not supported"); 
    List<Response> v = new ArrayList<Response>();
    boolean done = false;
    Response r = null;
    try {
      this.idleTag = writeCommand("IDLE", null);
    } catch (LiteralException lex) {
      v.add(lex.getResponse());
      done = true;
    } catch (Exception ex) {
      v.add(Response.byeResponse(ex));
      done = true;
    } 
    while (!done) {
      try {
        r = readResponse();
      } catch (IOException ioex) {
        r = Response.byeResponse(ioex);
      } catch (ProtocolException pex) {
        continue;
      } 
      v.add(r);
      if (r.isContinuation() || r.isBYE())
        done = true; 
    } 
    Response[] responses = v.<Response>toArray(new Response[v.size()]);
    r = responses[responses.length - 1];
    notifyResponseHandlers(responses);
    if (!r.isContinuation())
      handleResult(r); 
  }
  
  public synchronized Response readIdleResponse() {
    if (this.idleTag == null)
      return null; 
    Response r = null;
    try {
      r = readResponse();
    } catch (IOException ioex) {
      r = Response.byeResponse(ioex);
    } catch (ProtocolException pex) {
      r = Response.byeResponse(pex);
    } 
    return r;
  }
  
  public boolean processIdleResponse(Response r) throws ProtocolException {
    Response[] responses = new Response[1];
    responses[0] = r;
    boolean done = false;
    notifyResponseHandlers(responses);
    if (r.isBYE())
      done = true; 
    if (r.isTagged() && r.getTag().equals(this.idleTag))
      done = true; 
    if (done)
      this.idleTag = null; 
    handleResult(r);
    return !done;
  }
  
  private static final byte[] DONE = new byte[] { 68, 79, 78, 69, 13, 10 };
  
  public void idleAbort() {
    OutputStream os = getOutputStream();
    try {
      os.write(DONE);
      os.flush();
    } catch (Exception ex) {
      this.logger.log(Level.FINEST, "Exception aborting IDLE", ex);
    } 
  }
  
  public Map<String, String> id(Map<String, String> clientParams) throws ProtocolException {
    if (!hasCapability("ID"))
      throw new BadCommandException("ID not supported"); 
    Response[] r = command("ID", ID.getArgumentList(clientParams));
    ID id = null;
    Response response = r[r.length - 1];
    if (response.isOK())
      for (int i = 0, len = r.length; i < len; i++) {
        if (r[i] instanceof IMAPResponse) {
          IMAPResponse ir = (IMAPResponse)r[i];
          if (ir.keyEquals("ID")) {
            if (id == null)
              id = new ID(ir); 
            r[i] = null;
          } 
        } 
      }  
    notifyResponseHandlers(r);
    handleResult(response);
    return (id == null) ? null : id.getServerParams();
  }
}
