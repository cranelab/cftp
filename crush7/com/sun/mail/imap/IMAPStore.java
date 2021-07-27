package com.sun.mail.imap;

import com.sun.mail.iap.BadCommandException;
import com.sun.mail.iap.CommandFailedException;
import com.sun.mail.iap.ConnectionException;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.iap.ResponseHandler;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.sun.mail.imap.protocol.IMAPReferralException;
import com.sun.mail.imap.protocol.ListInfo;
import com.sun.mail.imap.protocol.Namespaces;
import com.sun.mail.util.MailConnectException;
import com.sun.mail.util.MailLogger;
import com.sun.mail.util.PropUtil;
import com.sun.mail.util.SocketConnectException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import javax.mail.AuthenticationFailedException;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Quota;
import javax.mail.QuotaAwareStore;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.StoreClosedException;
import javax.mail.URLName;

public class IMAPStore extends Store implements QuotaAwareStore, ResponseHandler {
  public static final int RESPONSE = 1000;
  
  public static final String ID_NAME = "name";
  
  public static final String ID_VERSION = "version";
  
  public static final String ID_OS = "os";
  
  public static final String ID_OS_VERSION = "os-version";
  
  public static final String ID_VENDOR = "vendor";
  
  public static final String ID_SUPPORT_URL = "support-url";
  
  public static final String ID_ADDRESS = "address";
  
  public static final String ID_DATE = "date";
  
  public static final String ID_COMMAND = "command";
  
  public static final String ID_ARGUMENTS = "arguments";
  
  public static final String ID_ENVIRONMENT = "environment";
  
  protected final String name;
  
  protected final int defaultPort;
  
  protected final boolean isSSL;
  
  private final int blksize;
  
  private boolean ignoreSize;
  
  private final int statusCacheTimeout;
  
  private final int appendBufferSize;
  
  private final int minIdleTime;
  
  private volatile int port = -1;
  
  protected String host;
  
  protected String user;
  
  protected String password;
  
  protected String proxyAuthUser;
  
  protected String authorizationID;
  
  protected String saslRealm;
  
  private Namespaces namespaces;
  
  private boolean enableStartTLS = false;
  
  private boolean requireStartTLS = false;
  
  private boolean usingSSL = false;
  
  private boolean enableSASL = false;
  
  private String[] saslMechanisms;
  
  private boolean forcePasswordRefresh = false;
  
  private boolean enableResponseEvents = false;
  
  private boolean enableImapEvents = false;
  
  private String guid;
  
  private boolean throwSearchException = false;
  
  private boolean peek = false;
  
  private boolean closeFoldersOnStoreFailure = true;
  
  private boolean enableCompress = false;
  
  private boolean finalizeCleanClose = false;
  
  private volatile boolean connectionFailed = false;
  
  private volatile boolean forceClose = false;
  
  private final Object connectionFailedLock = new Object();
  
  private boolean debugusername;
  
  private boolean debugpassword;
  
  protected MailLogger logger;
  
  private boolean messageCacheDebug;
  
  private volatile Constructor<?> folderConstructor = null;
  
  private volatile Constructor<?> folderConstructorLI = null;
  
  private final ConnectionPool pool;
  
  static class ConnectionPool {
    private Vector<IMAPProtocol> authenticatedConnections = new Vector<IMAPProtocol>();
    
    private Vector<IMAPFolder> folders;
    
    private boolean storeConnectionInUse = false;
    
    private long lastTimePruned;
    
    private final boolean separateStoreConnection;
    
    private final long clientTimeoutInterval;
    
    private final long serverTimeoutInterval;
    
    private final int poolSize;
    
    private final long pruningInterval;
    
    private final MailLogger logger;
    
    private static final int RUNNING = 0;
    
    private static final int IDLE = 1;
    
    private static final int ABORTING = 2;
    
    private int idleState = 0;
    
    private IMAPProtocol idleProtocol;
    
    ConnectionPool(String name, MailLogger plogger, Session session) {
      this.lastTimePruned = System.currentTimeMillis();
      boolean debug = PropUtil.getBooleanSessionProperty(session, "mail." + name + ".connectionpool.debug", false);
      this.logger = plogger.getSubLogger("connectionpool", "DEBUG IMAP CP", debug);
      int size = PropUtil.getIntSessionProperty(session, "mail." + name + ".connectionpoolsize", -1);
      if (size > 0) {
        this.poolSize = size;
        if (this.logger.isLoggable(Level.CONFIG))
          this.logger.config("mail.imap.connectionpoolsize: " + this.poolSize); 
      } else {
        this.poolSize = 1;
      } 
      int connectionPoolTimeout = PropUtil.getIntSessionProperty(session, "mail." + name + ".connectionpooltimeout", -1);
      if (connectionPoolTimeout > 0) {
        this.clientTimeoutInterval = connectionPoolTimeout;
        if (this.logger.isLoggable(Level.CONFIG))
          this.logger.config("mail.imap.connectionpooltimeout: " + this.clientTimeoutInterval); 
      } else {
        this.clientTimeoutInterval = 45000L;
      } 
      int serverTimeout = PropUtil.getIntSessionProperty(session, "mail." + name + ".servertimeout", -1);
      if (serverTimeout > 0) {
        this.serverTimeoutInterval = serverTimeout;
        if (this.logger.isLoggable(Level.CONFIG))
          this.logger.config("mail.imap.servertimeout: " + this.serverTimeoutInterval); 
      } else {
        this.serverTimeoutInterval = 1800000L;
      } 
      int pruning = PropUtil.getIntSessionProperty(session, "mail." + name + ".pruninginterval", -1);
      if (pruning > 0) {
        this.pruningInterval = pruning;
        if (this.logger.isLoggable(Level.CONFIG))
          this.logger.config("mail.imap.pruninginterval: " + this.pruningInterval); 
      } else {
        this.pruningInterval = 60000L;
      } 
      this
        .separateStoreConnection = PropUtil.getBooleanSessionProperty(session, "mail." + name + ".separatestoreconnection", false);
      if (this.separateStoreConnection)
        this.logger.config("dedicate a store connection"); 
    }
  }
  
  private ResponseHandler nonStoreResponseHandler = new ResponseHandler() {
      public void handleResponse(Response r) {
        if (r.isOK() || r.isNO() || r.isBAD() || r.isBYE())
          IMAPStore.this.handleResponseCode(r); 
        if (r.isBYE())
          IMAPStore.this.logger.fine("IMAPStore non-store connection dead"); 
      }
    };
  
  public IMAPStore(Session session, URLName url) {
    this(session, url, "imap", false);
  }
  
  protected IMAPStore(Session session, URLName url, String name, boolean isSSL) {
    super(session, url);
    if (url != null)
      name = url.getProtocol(); 
    this.name = name;
    if (!isSSL)
      isSSL = PropUtil.getBooleanSessionProperty(session, "mail." + name + ".ssl.enable", false); 
    if (isSSL) {
      this.defaultPort = 993;
    } else {
      this.defaultPort = 143;
    } 
    this.isSSL = isSSL;
    this.debug = session.getDebug();
    this.debugusername = PropUtil.getBooleanSessionProperty(session, "mail.debug.auth.username", true);
    this.debugpassword = PropUtil.getBooleanSessionProperty(session, "mail.debug.auth.password", false);
    this
      .logger = new MailLogger(getClass(), "DEBUG " + name.toUpperCase(Locale.ENGLISH), session);
    boolean partialFetch = PropUtil.getBooleanSessionProperty(session, "mail." + name + ".partialfetch", true);
    if (!partialFetch) {
      this.blksize = -1;
      this.logger.config("mail.imap.partialfetch: false");
    } else {
      this.blksize = PropUtil.getIntSessionProperty(session, "mail." + name + ".fetchsize", 16384);
      if (this.logger.isLoggable(Level.CONFIG))
        this.logger.config("mail.imap.fetchsize: " + this.blksize); 
    } 
    this.ignoreSize = PropUtil.getBooleanSessionProperty(session, "mail." + name + ".ignorebodystructuresize", false);
    if (this.logger.isLoggable(Level.CONFIG))
      this.logger.config("mail.imap.ignorebodystructuresize: " + this.ignoreSize); 
    this.statusCacheTimeout = PropUtil.getIntSessionProperty(session, "mail." + name + ".statuscachetimeout", 1000);
    if (this.logger.isLoggable(Level.CONFIG))
      this.logger.config("mail.imap.statuscachetimeout: " + this.statusCacheTimeout); 
    this.appendBufferSize = PropUtil.getIntSessionProperty(session, "mail." + name + ".appendbuffersize", -1);
    if (this.logger.isLoggable(Level.CONFIG))
      this.logger.config("mail.imap.appendbuffersize: " + this.appendBufferSize); 
    this.minIdleTime = PropUtil.getIntSessionProperty(session, "mail." + name + ".minidletime", 10);
    if (this.logger.isLoggable(Level.CONFIG))
      this.logger.config("mail.imap.minidletime: " + this.minIdleTime); 
    String s = session.getProperty("mail." + name + ".proxyauth.user");
    if (s != null) {
      this.proxyAuthUser = s;
      if (this.logger.isLoggable(Level.CONFIG))
        this.logger.config("mail.imap.proxyauth.user: " + this.proxyAuthUser); 
    } 
    this.enableStartTLS = PropUtil.getBooleanSessionProperty(session, "mail." + name + ".starttls.enable", false);
    if (this.enableStartTLS)
      this.logger.config("enable STARTTLS"); 
    this.requireStartTLS = PropUtil.getBooleanSessionProperty(session, "mail." + name + ".starttls.required", false);
    if (this.requireStartTLS)
      this.logger.config("require STARTTLS"); 
    this.enableSASL = PropUtil.getBooleanSessionProperty(session, "mail." + name + ".sasl.enable", false);
    if (this.enableSASL)
      this.logger.config("enable SASL"); 
    if (this.enableSASL) {
      s = session.getProperty("mail." + name + ".sasl.mechanisms");
      if (s != null && s.length() > 0) {
        if (this.logger.isLoggable(Level.CONFIG))
          this.logger.config("SASL mechanisms allowed: " + s); 
        List<String> v = new ArrayList<String>(5);
        StringTokenizer st = new StringTokenizer(s, " ,");
        while (st.hasMoreTokens()) {
          String m = st.nextToken();
          if (m.length() > 0)
            v.add(m); 
        } 
        this.saslMechanisms = new String[v.size()];
        v.toArray(this.saslMechanisms);
      } 
    } 
    s = session.getProperty("mail." + name + ".sasl.authorizationid");
    if (s != null) {
      this.authorizationID = s;
      this.logger.log(Level.CONFIG, "mail.imap.sasl.authorizationid: {0}", this.authorizationID);
    } 
    s = session.getProperty("mail." + name + ".sasl.realm");
    if (s != null) {
      this.saslRealm = s;
      this.logger.log(Level.CONFIG, "mail.imap.sasl.realm: {0}", this.saslRealm);
    } 
    this.forcePasswordRefresh = PropUtil.getBooleanSessionProperty(session, "mail." + name + ".forcepasswordrefresh", false);
    if (this.forcePasswordRefresh)
      this.logger.config("enable forcePasswordRefresh"); 
    this.enableResponseEvents = PropUtil.getBooleanSessionProperty(session, "mail." + name + ".enableresponseevents", false);
    if (this.enableResponseEvents)
      this.logger.config("enable IMAP response events"); 
    this.enableImapEvents = PropUtil.getBooleanSessionProperty(session, "mail." + name + ".enableimapevents", false);
    if (this.enableImapEvents)
      this.logger.config("enable IMAP IDLE events"); 
    this.messageCacheDebug = PropUtil.getBooleanSessionProperty(session, "mail." + name + ".messagecache.debug", false);
    this.guid = session.getProperty("mail." + name + ".yahoo.guid");
    if (this.guid != null)
      this.logger.log(Level.CONFIG, "mail.imap.yahoo.guid: {0}", this.guid); 
    this.throwSearchException = PropUtil.getBooleanSessionProperty(session, "mail." + name + ".throwsearchexception", false);
    if (this.throwSearchException)
      this.logger.config("throw SearchException"); 
    this.peek = PropUtil.getBooleanSessionProperty(session, "mail." + name + ".peek", false);
    if (this.peek)
      this.logger.config("peek"); 
    this.closeFoldersOnStoreFailure = PropUtil.getBooleanSessionProperty(session, "mail." + name + ".closefoldersonstorefailure", true);
    if (this.closeFoldersOnStoreFailure)
      this.logger.config("closeFoldersOnStoreFailure"); 
    this.enableCompress = PropUtil.getBooleanSessionProperty(session, "mail." + name + ".compress.enable", false);
    if (this.enableCompress)
      this.logger.config("enable COMPRESS"); 
    this.finalizeCleanClose = PropUtil.getBooleanSessionProperty(session, "mail." + name + ".finalizecleanclose", false);
    if (this.finalizeCleanClose)
      this.logger.config("close connection cleanly in finalize"); 
    s = session.getProperty("mail." + name + ".folder.class");
    if (s != null) {
      this.logger.log(Level.CONFIG, "IMAP: folder class: {0}", s);
      try {
        ClassLoader cl = getClass().getClassLoader();
        Class<?> folderClass = null;
        try {
          folderClass = Class.forName(s, false, cl);
        } catch (ClassNotFoundException ex1) {
          folderClass = Class.forName(s);
        } 
        Class<?>[] c = new Class[] { String.class, char.class, IMAPStore.class, Boolean.class };
        this.folderConstructor = folderClass.getConstructor(c);
        Class<?>[] c2 = new Class[] { ListInfo.class, IMAPStore.class };
        this.folderConstructorLI = folderClass.getConstructor(c2);
      } catch (Exception ex) {
        this.logger.log(Level.CONFIG, "IMAP: failed to load folder class", ex);
      } 
    } 
    this.pool = new ConnectionPool(name, this.logger, session);
  }
  
  protected synchronized boolean protocolConnect(String host, int pport, String user, String password) throws MessagingException {
    IMAPProtocol protocol = null;
    if (host == null || password == null || user == null) {
      if (this.logger.isLoggable(Level.FINE))
        this.logger.fine("protocolConnect returning false, host=" + host + ", user=" + 
            
            traceUser(user) + ", password=" + 
            tracePassword(password)); 
      return false;
    } 
    if (pport != -1) {
      this.port = pport;
    } else {
      this.port = PropUtil.getIntSessionProperty(this.session, "mail." + this.name + ".port", this.port);
    } 
    if (this.port == -1)
      this.port = this.defaultPort; 
    try {
      boolean poolEmpty;
      synchronized (this.pool) {
        poolEmpty = this.pool.authenticatedConnections.isEmpty();
      } 
      if (poolEmpty) {
        if (this.logger.isLoggable(Level.FINE))
          this.logger.fine("trying to connect to host \"" + host + "\", port " + this.port + ", isSSL " + this.isSSL); 
        protocol = newIMAPProtocol(host, this.port);
        if (this.logger.isLoggable(Level.FINE))
          this.logger.fine("protocolConnect login, host=" + host + ", user=" + 
              
              traceUser(user) + ", password=" + 
              tracePassword(password)); 
        protocol.addResponseHandler(this.nonStoreResponseHandler);
        login(protocol, user, password);
        protocol.removeResponseHandler(this.nonStoreResponseHandler);
        protocol.addResponseHandler(this);
        this.usingSSL = protocol.isSSL();
        this.host = host;
        this.user = user;
        this.password = password;
        synchronized (this.pool) {
          this.pool.authenticatedConnections.addElement(protocol);
        } 
      } 
    } catch (IMAPReferralException ex) {
      IMAPReferralException iMAPReferralException1;
      if (protocol != null)
        protocol.disconnect(); 
      protocol = null;
      throw new ReferralException(iMAPReferralException1.getUrl(), iMAPReferralException1.getMessage());
    } catch (CommandFailedException cex) {
      if (protocol != null)
        protocol.disconnect(); 
      protocol = null;
      Response r = cex.getResponse();
      throw new AuthenticationFailedException((r != null) ? r
          .getRest() : cex.getMessage());
    } catch (ProtocolException pex) {
      if (protocol != null)
        protocol.disconnect(); 
      protocol = null;
      throw new MessagingException(pex.getMessage(), pex);
    } catch (SocketConnectException scex) {
      throw new MailConnectException(scex);
    } catch (IOException ioex) {
      throw new MessagingException(ioex.getMessage(), ioex);
    } 
    return true;
  }
  
  protected IMAPProtocol newIMAPProtocol(String host, int port) throws IOException, ProtocolException {
    return new IMAPProtocol(this.name, host, port, this.session
        .getProperties(), this.isSSL, this.logger);
  }
  
  private void login(IMAPProtocol p, String u, String pw) throws ProtocolException {
    String authzid;
    if ((this.enableStartTLS || this.requireStartTLS) && !p.isSSL())
      if (p.hasCapability("STARTTLS")) {
        p.startTLS();
        p.capability();
      } else if (this.requireStartTLS) {
        this.logger.fine("STARTTLS required but not supported by server");
        throw new ProtocolException("STARTTLS required but not supported by server");
      }  
    if (p.isAuthenticated())
      return; 
    preLogin(p);
    if (this.guid != null) {
      Map<String, String> gmap = new HashMap<String, String>();
      gmap.put("GUID", this.guid);
      p.id(gmap);
    } 
    p.getCapabilities().put("__PRELOGIN__", "");
    if (this.authorizationID != null) {
      authzid = this.authorizationID;
    } else if (this.proxyAuthUser != null) {
      authzid = this.proxyAuthUser;
    } else {
      authzid = null;
    } 
    if (this.enableSASL)
      try {
        p.sasllogin(this.saslMechanisms, this.saslRealm, authzid, u, pw);
        if (!p.isAuthenticated())
          throw new CommandFailedException("SASL authentication failed"); 
      } catch (UnsupportedOperationException unsupportedOperationException) {} 
    if (!p.isAuthenticated())
      authenticate(p, authzid, u, pw); 
    if (this.proxyAuthUser != null)
      p.proxyauth(this.proxyAuthUser); 
    if (p.hasCapability("__PRELOGIN__"))
      try {
        p.capability();
      } catch (ConnectionException cex) {
        throw cex;
      } catch (ProtocolException protocolException) {} 
    if (this.enableCompress && 
      p.hasCapability("COMPRESS=DEFLATE"))
      p.compress(); 
  }
  
  private void authenticate(IMAPProtocol p, String authzid, String user, String password) throws ProtocolException {
    String defaultAuthenticationMechanisms = "PLAIN LOGIN NTLM XOAUTH2";
    String mechs = this.session.getProperty("mail." + this.name + ".auth.mechanisms");
    if (mechs == null)
      mechs = defaultAuthenticationMechanisms; 
    StringTokenizer st = new StringTokenizer(mechs);
    while (st.hasMoreTokens()) {
      String m = st.nextToken();
      m = m.toUpperCase(Locale.ENGLISH);
      if (mechs == defaultAuthenticationMechanisms) {
        String dprop = "mail." + this.name + ".auth." + m.toLowerCase(Locale.ENGLISH) + ".disable";
        boolean disabled = PropUtil.getBooleanSessionProperty(this.session, dprop, m
            .equals("XOAUTH2"));
        if (disabled) {
          if (this.logger.isLoggable(Level.FINE))
            this.logger.fine("mechanism " + m + " disabled by property: " + dprop); 
          continue;
        } 
      } 
      if (!p.hasCapability("AUTH=" + m) && (
        !m.equals("LOGIN") || !p.hasCapability("AUTH-LOGIN"))) {
        this.logger.log(Level.FINE, "mechanism {0} not supported by server", m);
        continue;
      } 
      if (m.equals("PLAIN")) {
        p.authplain(authzid, user, password);
        continue;
      } 
      if (m.equals("LOGIN")) {
        p.authlogin(user, password);
        continue;
      } 
      if (m.equals("NTLM")) {
        p.authntlm(authzid, user, password);
      } else {
        if (m.equals("XOAUTH2")) {
          p.authoauth2(user, password);
          continue;
        } 
        this.logger.log(Level.FINE, "no authenticator for mechanism {0}", m);
        continue;
      } 
      return;
    } 
    if (!p.hasCapability("LOGINDISABLED")) {
      p.login(user, password);
      return;
    } 
    throw new ProtocolException("No login methods supported!");
  }
  
  protected void preLogin(IMAPProtocol p) throws ProtocolException {}
  
  public synchronized boolean isSSL() {
    return this.usingSSL;
  }
  
  public synchronized void setUsername(String user) {
    this.user = user;
  }
  
  public synchronized void setPassword(String password) {
    this.password = password;
  }
  
  IMAPProtocol getProtocol(IMAPFolder folder) throws MessagingException {
    IMAPProtocol p = null;
    while (p == null) {
      synchronized (this.pool) {
        if (this.pool.authenticatedConnections.isEmpty() || (this.pool
          .authenticatedConnections.size() == 1 && (this.pool
          .separateStoreConnection || this.pool.storeConnectionInUse))) {
          this.logger.fine("no connections in the pool, creating a new one");
          try {
            if (this.forcePasswordRefresh)
              refreshPassword(); 
            p = newIMAPProtocol(this.host, this.port);
            p.addResponseHandler(this.nonStoreResponseHandler);
            login(p, this.user, this.password);
            p.removeResponseHandler(this.nonStoreResponseHandler);
          } catch (Exception ex1) {
            if (p != null)
              try {
                p.disconnect();
              } catch (Exception exception) {} 
            p = null;
          } 
          if (p == null)
            throw new MessagingException("connection failure"); 
        } else {
          if (this.logger.isLoggable(Level.FINE))
            this.logger.fine("connection available -- size: " + this.pool
                .authenticatedConnections.size()); 
          p = this.pool.authenticatedConnections.lastElement();
          this.pool.authenticatedConnections.removeElement(p);
          long lastUsed = System.currentTimeMillis() - p.getTimestamp();
          if (lastUsed > this.pool.serverTimeoutInterval)
            try {
              p.removeResponseHandler(this);
              p.addResponseHandler(this.nonStoreResponseHandler);
              p.noop();
              p.removeResponseHandler(this.nonStoreResponseHandler);
              p.addResponseHandler(this);
            } catch (ProtocolException pex) {
              try {
                p.removeResponseHandler(this.nonStoreResponseHandler);
                p.disconnect();
              } finally {
                p = null;
                continue;
              } 
            }  
          if (this.proxyAuthUser != null && 
            !this.proxyAuthUser.equals(p.getProxyAuthUser()) && p
            .hasCapability("X-UNAUTHENTICATE"))
            try {
              p.removeResponseHandler(this);
              p.addResponseHandler(this.nonStoreResponseHandler);
              p.unauthenticate();
              login(p, this.user, this.password);
              p.removeResponseHandler(this.nonStoreResponseHandler);
              p.addResponseHandler(this);
            } catch (ProtocolException pex) {
              try {
                p.removeResponseHandler(this.nonStoreResponseHandler);
                p.disconnect();
              } finally {
                p = null;
                continue;
              } 
            }  
          p.removeResponseHandler(this);
        } 
        timeoutConnections();
        if (folder != null) {
          if (this.pool.folders == null)
            this.pool.folders = new Vector(); 
          this.pool.folders.addElement(folder);
        } 
      } 
    } 
    return p;
  }
  
  private IMAPProtocol getStoreProtocol() throws ProtocolException {
    IMAPProtocol p = null;
    while (p == null) {
      synchronized (this.pool) {
        waitIfIdle();
        if (this.pool.authenticatedConnections.isEmpty()) {
          this.pool.logger.fine("getStoreProtocol() - no connections in the pool, creating a new one");
          try {
            if (this.forcePasswordRefresh)
              refreshPassword(); 
            p = newIMAPProtocol(this.host, this.port);
            login(p, this.user, this.password);
          } catch (Exception ex1) {
            if (p != null)
              try {
                p.logout();
              } catch (Exception exception) {} 
            p = null;
          } 
          if (p == null)
            throw new ConnectionException("failed to create new store connection"); 
          p.addResponseHandler(this);
          this.pool.authenticatedConnections.addElement(p);
        } else {
          if (this.pool.logger.isLoggable(Level.FINE))
            this.pool.logger.fine("getStoreProtocol() - connection available -- size: " + this.pool
                
                .authenticatedConnections.size()); 
          p = this.pool.authenticatedConnections.firstElement();
          if (this.proxyAuthUser != null && 
            !this.proxyAuthUser.equals(p.getProxyAuthUser()) && p
            .hasCapability("X-UNAUTHENTICATE")) {
            p.unauthenticate();
            login(p, this.user, this.password);
          } 
        } 
        if (this.pool.storeConnectionInUse) {
          try {
            p = null;
            this.pool.wait();
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ProtocolException("Interrupted getStoreProtocol", ex);
          } 
        } else {
          this.pool.storeConnectionInUse = true;
          this.pool.logger.fine("getStoreProtocol() -- storeConnectionInUse");
        } 
        timeoutConnections();
      } 
    } 
    return p;
  }
  
  IMAPProtocol getFolderStoreProtocol() throws ProtocolException {
    IMAPProtocol p = getStoreProtocol();
    p.removeResponseHandler(this);
    p.addResponseHandler(this.nonStoreResponseHandler);
    return p;
  }
  
  private void refreshPassword() {
    InetAddress addr;
    if (this.logger.isLoggable(Level.FINE))
      this.logger.fine("refresh password, user: " + traceUser(this.user)); 
    try {
      addr = InetAddress.getByName(this.host);
    } catch (UnknownHostException e) {
      addr = null;
    } 
    PasswordAuthentication pa = this.session.requestPasswordAuthentication(addr, this.port, this.name, null, this.user);
    if (pa != null) {
      this.user = pa.getUserName();
      this.password = pa.getPassword();
    } 
  }
  
  boolean allowReadOnlySelect() {
    return PropUtil.getBooleanSessionProperty(this.session, "mail." + this.name + ".allowreadonlyselect", false);
  }
  
  boolean hasSeparateStoreConnection() {
    return this.pool.separateStoreConnection;
  }
  
  MailLogger getConnectionPoolLogger() {
    return this.pool.logger;
  }
  
  boolean getMessageCacheDebug() {
    return this.messageCacheDebug;
  }
  
  boolean isConnectionPoolFull() {
    synchronized (this.pool) {
      if (this.pool.logger.isLoggable(Level.FINE))
        this.pool.logger.fine("connection pool current size: " + this.pool
            .authenticatedConnections.size() + "   pool size: " + this.pool
            .poolSize); 
      return (this.pool.authenticatedConnections.size() >= this.pool.poolSize);
    } 
  }
  
  void releaseProtocol(IMAPFolder folder, IMAPProtocol protocol) {
    synchronized (this.pool) {
      if (protocol != null)
        if (!isConnectionPoolFull()) {
          protocol.addResponseHandler(this);
          this.pool.authenticatedConnections.addElement(protocol);
          if (this.logger.isLoggable(Level.FINE))
            this.logger.fine("added an Authenticated connection -- size: " + this.pool
                
                .authenticatedConnections.size()); 
        } else {
          this.logger.fine("pool is full, not adding an Authenticated connection");
          try {
            protocol.logout();
          } catch (ProtocolException protocolException) {}
        }  
      if (this.pool.folders != null)
        this.pool.folders.removeElement(folder); 
      timeoutConnections();
    } 
  }
  
  private void releaseStoreProtocol(IMAPProtocol protocol) {
    boolean failed;
    if (protocol == null) {
      cleanup();
      return;
    } 
    synchronized (this.connectionFailedLock) {
      failed = this.connectionFailed;
      this.connectionFailed = false;
    } 
    synchronized (this.pool) {
      this.pool.storeConnectionInUse = false;
      this.pool.notifyAll();
      this.pool.logger.fine("releaseStoreProtocol()");
      timeoutConnections();
    } 
    assert !Thread.holdsLock(this.pool);
    if (failed)
      cleanup(); 
  }
  
  void releaseFolderStoreProtocol(IMAPProtocol protocol) {
    if (protocol == null)
      return; 
    protocol.removeResponseHandler(this.nonStoreResponseHandler);
    protocol.addResponseHandler(this);
    synchronized (this.pool) {
      this.pool.storeConnectionInUse = false;
      this.pool.notifyAll();
      this.pool.logger.fine("releaseFolderStoreProtocol()");
      timeoutConnections();
    } 
  }
  
  private void emptyConnectionPool(boolean force) {
    synchronized (this.pool) {
      int index = this.pool.authenticatedConnections.size() - 1;
      for (; index >= 0; index--) {
        try {
          IMAPProtocol p = this.pool.authenticatedConnections.elementAt(index);
          p.removeResponseHandler(this);
          if (force) {
            p.disconnect();
          } else {
            p.logout();
          } 
        } catch (ProtocolException protocolException) {}
      } 
      this.pool.authenticatedConnections.removeAllElements();
    } 
    this.pool.logger.fine("removed all authenticated connections from pool");
  }
  
  private void timeoutConnections() {
    synchronized (this.pool) {
      if (System.currentTimeMillis() - this.pool.lastTimePruned > this.pool
        .pruningInterval && this.pool
        .authenticatedConnections.size() > 1) {
        if (this.pool.logger.isLoggable(Level.FINE)) {
          this.pool.logger.fine("checking for connections to prune: " + (
              System.currentTimeMillis() - this.pool.lastTimePruned));
          this.pool.logger.fine("clientTimeoutInterval: " + this.pool
              .clientTimeoutInterval);
        } 
        int index = this.pool.authenticatedConnections.size() - 1;
        for (; index > 0; index--) {
          IMAPProtocol p = this.pool.authenticatedConnections.elementAt(index);
          if (this.pool.logger.isLoggable(Level.FINE))
            this.pool.logger.fine("protocol last used: " + (
                System.currentTimeMillis() - p.getTimestamp())); 
          if (System.currentTimeMillis() - p.getTimestamp() > this.pool
            .clientTimeoutInterval) {
            this.pool.logger.fine("authenticated connection timed out, logging out the connection");
            p.removeResponseHandler(this);
            this.pool.authenticatedConnections.removeElementAt(index);
            try {
              p.logout();
            } catch (ProtocolException protocolException) {}
          } 
        } 
        this.pool.lastTimePruned = System.currentTimeMillis();
      } 
    } 
  }
  
  int getFetchBlockSize() {
    return this.blksize;
  }
  
  boolean ignoreBodyStructureSize() {
    return this.ignoreSize;
  }
  
  Session getSession() {
    return this.session;
  }
  
  int getStatusCacheTimeout() {
    return this.statusCacheTimeout;
  }
  
  int getAppendBufferSize() {
    return this.appendBufferSize;
  }
  
  int getMinIdleTime() {
    return this.minIdleTime;
  }
  
  boolean throwSearchException() {
    return this.throwSearchException;
  }
  
  boolean getPeek() {
    return this.peek;
  }
  
  public synchronized boolean hasCapability(String capability) throws MessagingException {
    IMAPProtocol p = null;
    try {
      p = getStoreProtocol();
      return p.hasCapability(capability);
    } catch (ProtocolException pex) {
      throw new MessagingException(pex.getMessage(), pex);
    } finally {
      releaseStoreProtocol(p);
    } 
  }
  
  public void setProxyAuthUser(String user) {
    this.proxyAuthUser = user;
  }
  
  public String getProxyAuthUser() {
    return this.proxyAuthUser;
  }
  
  public synchronized boolean isConnected() {
    if (!super.isConnected())
      return false; 
    IMAPProtocol p = null;
    try {
      p = getStoreProtocol();
      p.noop();
    } catch (ProtocolException protocolException) {
    
    } finally {
      releaseStoreProtocol(p);
    } 
    return super.isConnected();
  }
  
  public synchronized void close() throws MessagingException {
    if (!super.isConnected())
      return; 
    IMAPProtocol protocol = null;
    try {
      boolean isEmpty;
      synchronized (this.pool) {
        isEmpty = this.pool.authenticatedConnections.isEmpty();
      } 
      if (isEmpty) {
        this.pool.logger.fine("close() - no connections ");
        cleanup();
        return;
      } 
      protocol = getStoreProtocol();
      synchronized (this.pool) {
        this.pool.authenticatedConnections.removeElement(protocol);
      } 
      protocol.logout();
    } catch (ProtocolException pex) {
      ProtocolException protocolException1;
      throw new MessagingException(protocolException1.getMessage(), protocolException1);
    } finally {
      releaseStoreProtocol(protocol);
    } 
  }
  
  protected void finalize() throws Throwable {
    if (!this.finalizeCleanClose) {
      synchronized (this.connectionFailedLock) {
        this.connectionFailed = true;
        this.forceClose = true;
      } 
      this.closeFoldersOnStoreFailure = true;
    } 
    try {
      close();
    } finally {
      super.finalize();
    } 
  }
  
  private synchronized void cleanup() {
    boolean force;
    if (!super.isConnected()) {
      this.logger.fine("IMAPStore cleanup, not connected");
      return;
    } 
    synchronized (this.connectionFailedLock) {
      force = this.forceClose;
      this.forceClose = false;
      this.connectionFailed = false;
    } 
    if (this.logger.isLoggable(Level.FINE))
      this.logger.fine("IMAPStore cleanup, force " + force); 
    if (!force || this.closeFoldersOnStoreFailure) {
      List<IMAPFolder> foldersCopy = null;
      boolean done = true;
      while (true) {
        synchronized (this.pool) {
          if (this.pool.folders != null) {
            done = false;
            foldersCopy = this.pool.folders;
            this.pool.folders = null;
          } else {
            done = true;
          } 
        } 
        if (done)
          break; 
        for (int i = 0, fsize = foldersCopy.size(); i < fsize; i++) {
          IMAPFolder f = foldersCopy.get(i);
          try {
            if (force) {
              this.logger.fine("force folder to close");
              f.forceClose();
            } else {
              this.logger.fine("close folder");
              f.close(false);
            } 
          } catch (MessagingException messagingException) {
          
          } catch (IllegalStateException illegalStateException) {}
        } 
      } 
    } 
    synchronized (this.pool) {
      emptyConnectionPool(force);
    } 
    try {
      super.close();
    } catch (MessagingException messagingException) {}
    this.logger.fine("IMAPStore cleanup done");
  }
  
  public synchronized Folder getDefaultFolder() throws MessagingException {
    checkConnected();
    return new DefaultFolder(this);
  }
  
  public synchronized Folder getFolder(String name) throws MessagingException {
    checkConnected();
    return newIMAPFolder(name, '￿');
  }
  
  public synchronized Folder getFolder(URLName url) throws MessagingException {
    checkConnected();
    return newIMAPFolder(url.getFile(), '￿');
  }
  
  protected IMAPFolder newIMAPFolder(String fullName, char separator, Boolean isNamespace) {
    IMAPFolder f = null;
    if (this.folderConstructor != null)
      try {
        Object[] o = { fullName, Character.valueOf(separator), this, isNamespace };
        f = (IMAPFolder)this.folderConstructor.newInstance(o);
      } catch (Exception ex) {
        this.logger.log(Level.FINE, "exception creating IMAPFolder class", ex);
      }  
    if (f == null)
      f = new IMAPFolder(fullName, separator, this, isNamespace); 
    return f;
  }
  
  protected IMAPFolder newIMAPFolder(String fullName, char separator) {
    return newIMAPFolder(fullName, separator, (Boolean)null);
  }
  
  protected IMAPFolder newIMAPFolder(ListInfo li) {
    IMAPFolder f = null;
    if (this.folderConstructorLI != null)
      try {
        Object[] o = { li, this };
        f = (IMAPFolder)this.folderConstructorLI.newInstance(o);
      } catch (Exception ex) {
        this.logger.log(Level.FINE, "exception creating IMAPFolder class LI", ex);
      }  
    if (f == null)
      f = new IMAPFolder(li, this); 
    return f;
  }
  
  public Folder[] getPersonalNamespaces() throws MessagingException {
    Namespaces ns = getNamespaces();
    if (ns == null || ns.personal == null)
      return super.getPersonalNamespaces(); 
    return namespaceToFolders(ns.personal, (String)null);
  }
  
  public Folder[] getUserNamespaces(String user) throws MessagingException {
    Namespaces ns = getNamespaces();
    if (ns == null || ns.otherUsers == null)
      return super.getUserNamespaces(user); 
    return namespaceToFolders(ns.otherUsers, user);
  }
  
  public Folder[] getSharedNamespaces() throws MessagingException {
    Namespaces ns = getNamespaces();
    if (ns == null || ns.shared == null)
      return super.getSharedNamespaces(); 
    return namespaceToFolders(ns.shared, (String)null);
  }
  
  private synchronized Namespaces getNamespaces() throws MessagingException {
    checkConnected();
    IMAPProtocol p = null;
    if (this.namespaces == null)
      try {
        p = getStoreProtocol();
        this.namespaces = p.namespace();
      } catch (BadCommandException badCommandException) {
      
      } catch (ConnectionException cex) {
        throw new StoreClosedException(this, cex.getMessage());
      } catch (ProtocolException pex) {
        throw new MessagingException(pex.getMessage(), pex);
      } finally {
        releaseStoreProtocol(p);
      }  
    return this.namespaces;
  }
  
  private Folder[] namespaceToFolders(Namespaces.Namespace[] ns, String user) {
    Folder[] fa = new Folder[ns.length];
    for (int i = 0; i < fa.length; i++) {
      String name = (ns[i]).prefix;
      if (user == null) {
        int len = name.length();
        if (len > 0 && name.charAt(len - 1) == (ns[i]).delimiter)
          name = name.substring(0, len - 1); 
      } else {
        name = name + user;
      } 
      fa[i] = newIMAPFolder(name, (ns[i]).delimiter, 
          Boolean.valueOf((user == null)));
    } 
    return fa;
  }
  
  public synchronized Quota[] getQuota(String root) throws MessagingException {
    checkConnected();
    Quota[] qa = null;
    IMAPProtocol p = null;
    try {
      p = getStoreProtocol();
      qa = p.getQuotaRoot(root);
    } catch (BadCommandException bex) {
      throw new MessagingException("QUOTA not supported", bex);
    } catch (ConnectionException cex) {
      throw new StoreClosedException(this, cex.getMessage());
    } catch (ProtocolException pex) {
      throw new MessagingException(pex.getMessage(), pex);
    } finally {
      releaseStoreProtocol(p);
    } 
    return qa;
  }
  
  public synchronized void setQuota(Quota quota) throws MessagingException {
    checkConnected();
    IMAPProtocol p = null;
    try {
      p = getStoreProtocol();
      p.setQuota(quota);
    } catch (BadCommandException bex) {
      throw new MessagingException("QUOTA not supported", bex);
    } catch (ConnectionException cex) {
      throw new StoreClosedException(this, cex.getMessage());
    } catch (ProtocolException pex) {
      throw new MessagingException(pex.getMessage(), pex);
    } finally {
      releaseStoreProtocol(p);
    } 
  }
  
  private void checkConnected() {
    assert Thread.holdsLock(this);
    if (!super.isConnected())
      throw new IllegalStateException("Not connected"); 
  }
  
  public void handleResponse(Response r) {
    if (r.isOK() || r.isNO() || r.isBAD() || r.isBYE())
      handleResponseCode(r); 
    if (r.isBYE()) {
      this.logger.fine("IMAPStore connection dead");
      synchronized (this.connectionFailedLock) {
        this.connectionFailed = true;
        if (r.isSynthetic())
          this.forceClose = true; 
      } 
      return;
    } 
  }
  
  public void idle() throws MessagingException {
    IMAPProtocol p = null;
    assert !Thread.holdsLock(this.pool);
    synchronized (this) {
      checkConnected();
    } 
    boolean needNotification = false;
    try {
      synchronized (this.pool) {
        p = getStoreProtocol();
        if (this.pool.idleState != 0) {
          try {
            this.pool.wait();
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new MessagingException("idle interrupted", ex);
          } 
          return;
        } 
        p.idleStart();
        needNotification = true;
        this.pool.idleState = 1;
        this.pool.idleProtocol = p;
      } 
      while (true) {
        Response r = p.readIdleResponse();
        synchronized (this.pool) {
          if (r == null || !p.processIdleResponse(r)) {
            this.pool.idleState = 0;
            this.pool.idleProtocol = null;
            this.pool.notifyAll();
            needNotification = false;
            break;
          } 
        } 
        if (this.enableImapEvents && r.isUnTagged())
          notifyStoreListeners(1000, r.toString()); 
      } 
      int minidle = getMinIdleTime();
      if (minidle > 0)
        try {
          Thread.sleep(minidle);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }  
    } catch (BadCommandException bex) {
      throw new MessagingException("IDLE not supported", bex);
    } catch (ConnectionException cex) {
      throw new StoreClosedException(this, cex.getMessage());
    } catch (ProtocolException pex) {
      throw new MessagingException(pex.getMessage(), pex);
    } finally {
      if (needNotification)
        synchronized (this.pool) {
          this.pool.idleState = 0;
          this.pool.idleProtocol = null;
          this.pool.notifyAll();
        }  
      releaseStoreProtocol(p);
    } 
  }
  
  private void waitIfIdle() throws ProtocolException {
    assert Thread.holdsLock(this.pool);
    while (this.pool.idleState != 0) {
      if (this.pool.idleState == 1) {
        this.pool.idleProtocol.idleAbort();
        this.pool.idleState = 2;
      } 
      try {
        this.pool.wait();
      } catch (InterruptedException ex) {
        throw new ProtocolException("Interrupted waitIfIdle", ex);
      } 
    } 
  }
  
  public synchronized Map<String, String> id(Map<String, String> clientParams) throws MessagingException {
    checkConnected();
    Map<String, String> serverParams = null;
    IMAPProtocol p = null;
    try {
      p = getStoreProtocol();
      serverParams = p.id(clientParams);
    } catch (BadCommandException bex) {
      throw new MessagingException("ID not supported", bex);
    } catch (ConnectionException cex) {
      throw new StoreClosedException(this, cex.getMessage());
    } catch (ProtocolException pex) {
      throw new MessagingException(pex.getMessage(), pex);
    } finally {
      releaseStoreProtocol(p);
    } 
    return serverParams;
  }
  
  void handleResponseCode(Response r) {
    if (this.enableResponseEvents)
      notifyStoreListeners(1000, r.toString()); 
    String s = r.getRest();
    boolean isAlert = false;
    if (s.startsWith("[")) {
      int i = s.indexOf(']');
      if (i > 0 && s.substring(0, i + 1).equalsIgnoreCase("[ALERT]"))
        isAlert = true; 
      s = s.substring(i + 1).trim();
    } 
    if (isAlert) {
      notifyStoreListeners(1, s);
    } else if (r.isUnTagged() && s.length() > 0) {
      notifyStoreListeners(2, s);
    } 
  }
  
  private String traceUser(String user) {
    return this.debugusername ? user : "<user name suppressed>";
  }
  
  private String tracePassword(String password) {
    return this.debugpassword ? password : ((password == null) ? "<null>" : "<non-null>");
  }
}
