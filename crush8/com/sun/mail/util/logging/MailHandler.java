package com.sun.mail.util.logging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileTypeMap;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessageContext;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.SendFailedException;
import javax.mail.Service;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeUtility;
import javax.mail.util.ByteArrayDataSource;

public class MailHandler extends Handler {
  private static final Filter[] EMPTY_FILTERS = new Filter[0];
  
  private static final Formatter[] EMPTY_FORMATTERS = new Formatter[0];
  
  private static final int MIN_HEADER_SIZE = 1024;
  
  private static final int offValue = Level.OFF.intValue();
  
  private static final PrivilegedAction<Object> MAILHANDLER_LOADER = new GetAndSetContext(MailHandler.class);
  
  private static final ThreadLocal<Integer> MUTEX = new ThreadLocal<Integer>();
  
  private static final Integer MUTEX_PUBLISH = Integer.valueOf(-2);
  
  private static final Integer MUTEX_REPORT = Integer.valueOf(-4);
  
  private static final Integer MUTEX_LINKAGE = Integer.valueOf(-8);
  
  private volatile boolean sealed;
  
  private boolean isWriting;
  
  private Properties mailProps;
  
  private Authenticator auth;
  
  private Session session;
  
  private int[] matched;
  
  private LogRecord[] data;
  
  private int size;
  
  private int capacity;
  
  private Comparator<? super LogRecord> comparator;
  
  private Formatter subjectFormatter;
  
  private Level pushLevel;
  
  private Filter pushFilter;
  
  private volatile Filter filter;
  
  private volatile Level logLevel = Level.ALL;
  
  private volatile Filter[] attachmentFilters;
  
  private String encoding;
  
  private Formatter formatter;
  
  private Formatter[] attachmentFormatters;
  
  private Formatter[] attachmentNames;
  
  private FileTypeMap contentTypes;
  
  private volatile ErrorManager errorManager = defaultErrorManager();
  
  public MailHandler() {
    init((Properties)null);
    this.sealed = true;
    checkAccess();
  }
  
  public MailHandler(int capacity) {
    init((Properties)null);
    this.sealed = true;
    setCapacity0(capacity);
  }
  
  public MailHandler(Properties props) {
    if (props == null)
      throw new NullPointerException(); 
    init(props);
    this.sealed = true;
    setMailProperties0(props);
  }
  
  public boolean isLoggable(LogRecord record) {
    int levelValue = getLevel().intValue();
    if (record.getLevel().intValue() < levelValue || levelValue == offValue)
      return false; 
    Filter body = getFilter();
    if (body == null || body.isLoggable(record)) {
      setMatchedPart(-1);
      return true;
    } 
    return isAttachmentLoggable(record);
  }
  
  public void publish(LogRecord record) {
    if (tryMutex()) {
      try {
        if (isLoggable(record)) {
          record.getSourceMethodName();
          publish0(record);
        } 
      } catch (LinkageError JDK8152515) {
        reportLinkageError(JDK8152515, 1);
      } finally {
        releaseMutex();
      } 
    } else {
      reportUnPublishedError(record);
    } 
  }
  
  private void publish0(LogRecord record) {
    Message msg;
    boolean priority;
    synchronized (this) {
      if (this.size == this.data.length && this.size < this.capacity)
        grow(); 
      if (this.size < this.data.length) {
        this.matched[this.size] = getMatchedPart();
        this.data[this.size] = record;
        this.size++;
        priority = isPushable(record);
        if (priority || this.size >= this.capacity) {
          msg = writeLogRecords(1);
        } else {
          msg = null;
        } 
      } else {
        priority = false;
        msg = null;
      } 
    } 
    if (msg != null)
      send(msg, priority, 1); 
  }
  
  private void reportUnPublishedError(LogRecord record) {
    Integer idx = MUTEX.get();
    if (idx == null || idx.intValue() > MUTEX_REPORT.intValue()) {
      MUTEX.set(MUTEX_REPORT);
      try {
        String msg;
        if (record != null) {
          Formatter f = createSimpleFormatter();
          msg = "Log record " + record.getSequenceNumber() + " was not published. " + head(f) + format(f, record) + tail(f, "");
        } else {
          msg = null;
        } 
        Exception e = new IllegalStateException("Recursive publish detected by thread " + Thread.currentThread());
        reportError(msg, e, 1);
      } finally {
        if (idx != null) {
          MUTEX.set(idx);
        } else {
          MUTEX.remove();
        } 
      } 
    } 
  }
  
  private boolean tryMutex() {
    if (MUTEX.get() == null) {
      MUTEX.set(MUTEX_PUBLISH);
      return true;
    } 
    return false;
  }
  
  private void releaseMutex() {
    MUTEX.remove();
  }
  
  private int getMatchedPart() {
    Integer idx = MUTEX.get();
    if (idx == null || idx.intValue() >= (readOnlyAttachmentFilters()).length)
      idx = MUTEX_PUBLISH; 
    return idx.intValue();
  }
  
  private void setMatchedPart(int index) {
    if (MUTEX_PUBLISH.equals(MUTEX.get()))
      MUTEX.set(Integer.valueOf(index)); 
  }
  
  private void clearMatches(int index) {
    assert Thread.holdsLock(this);
    for (int r = 0; r < this.size; r++) {
      if (this.matched[r] >= index)
        this.matched[r] = MUTEX_PUBLISH.intValue(); 
    } 
  }
  
  public void postConstruct() {}
  
  public void preDestroy() {
    push(false, 3);
  }
  
  public void push() {
    push(true, 2);
  }
  
  public void flush() {
    push(false, 2);
  }
  
  public void close() {
    try {
      checkAccess();
      Message msg = null;
      synchronized (this) {
        try {
          msg = writeLogRecords(3);
        } finally {
          this.logLevel = Level.OFF;
          if (this.capacity > 0)
            this.capacity = -this.capacity; 
          if (this.size == 0 && this.data.length != 1) {
            this.data = new LogRecord[1];
            this.matched = new int[this.data.length];
          } 
        } 
      } 
      if (msg != null)
        send(msg, false, 3); 
    } catch (LinkageError JDK8152515) {
      reportLinkageError(JDK8152515, 3);
    } 
  }
  
  public void setLevel(Level newLevel) {
    if (newLevel == null)
      throw new NullPointerException(); 
    checkAccess();
    synchronized (this) {
      if (this.capacity > 0)
        this.logLevel = newLevel; 
    } 
  }
  
  public Level getLevel() {
    return this.logLevel;
  }
  
  public ErrorManager getErrorManager() {
    checkAccess();
    return this.errorManager;
  }
  
  public void setErrorManager(ErrorManager em) {
    checkAccess();
    setErrorManager0(em);
  }
  
  private void setErrorManager0(ErrorManager em) {
    if (em == null)
      throw new NullPointerException(); 
    try {
      synchronized (this) {
        this.errorManager = em;
        super.setErrorManager(em);
      } 
    } catch (RuntimeException runtimeException) {
    
    } catch (LinkageError linkageError) {}
  }
  
  public Filter getFilter() {
    return this.filter;
  }
  
  public void setFilter(Filter newFilter) {
    checkAccess();
    synchronized (this) {
      if (newFilter != this.filter)
        clearMatches(-1); 
      this.filter = newFilter;
    } 
  }
  
  public synchronized String getEncoding() {
    return this.encoding;
  }
  
  public void setEncoding(String encoding) throws UnsupportedEncodingException {
    checkAccess();
    setEncoding0(encoding);
  }
  
  private void setEncoding0(String e) throws UnsupportedEncodingException {
    if (e != null)
      try {
        if (!Charset.isSupported(e))
          throw new UnsupportedEncodingException(e); 
      } catch (IllegalCharsetNameException icne) {
        throw new UnsupportedEncodingException(e);
      }  
    synchronized (this) {
      this.encoding = e;
    } 
  }
  
  public synchronized Formatter getFormatter() {
    return this.formatter;
  }
  
  public synchronized void setFormatter(Formatter newFormatter) throws SecurityException {
    checkAccess();
    if (newFormatter == null)
      throw new NullPointerException(); 
    this.formatter = newFormatter;
  }
  
  public final synchronized Level getPushLevel() {
    return this.pushLevel;
  }
  
  public final synchronized void setPushLevel(Level level) {
    checkAccess();
    if (level == null)
      throw new NullPointerException(); 
    if (this.isWriting)
      throw new IllegalStateException(); 
    this.pushLevel = level;
  }
  
  public final synchronized Filter getPushFilter() {
    return this.pushFilter;
  }
  
  public final synchronized void setPushFilter(Filter filter) {
    checkAccess();
    if (this.isWriting)
      throw new IllegalStateException(); 
    this.pushFilter = filter;
  }
  
  public final synchronized Comparator<? super LogRecord> getComparator() {
    return this.comparator;
  }
  
  public final synchronized void setComparator(Comparator<? super LogRecord> c) {
    checkAccess();
    if (this.isWriting)
      throw new IllegalStateException(); 
    this.comparator = c;
  }
  
  public final synchronized int getCapacity() {
    assert this.capacity != Integer.MIN_VALUE && this.capacity != 0 : this.capacity;
    return Math.abs(this.capacity);
  }
  
  public final synchronized Authenticator getAuthenticator() {
    checkAccess();
    return this.auth;
  }
  
  public final void setAuthenticator(Authenticator auth) {
    setAuthenticator0(auth);
  }
  
  public final void setAuthenticator(char... password) {
    if (password == null) {
      setAuthenticator0((Authenticator)null);
    } else {
      setAuthenticator0(DefaultAuthenticator.of(new String(password)));
    } 
  }
  
  private void setAuthenticator0(Authenticator auth) {
    Session settings;
    checkAccess();
    synchronized (this) {
      if (this.isWriting)
        throw new IllegalStateException(); 
      this.auth = auth;
      settings = updateSession();
    } 
    verifySettings(settings);
  }
  
  public final void setMailProperties(Properties props) {
    setMailProperties0(props);
  }
  
  private void setMailProperties0(Properties props) {
    Session settings;
    checkAccess();
    props = (Properties)props.clone();
    synchronized (this) {
      if (this.isWriting)
        throw new IllegalStateException(); 
      this.mailProps = props;
      settings = updateSession();
    } 
    verifySettings(settings);
  }
  
  public final Properties getMailProperties() {
    Properties props;
    checkAccess();
    synchronized (this) {
      props = this.mailProps;
    } 
    return (Properties)props.clone();
  }
  
  public final Filter[] getAttachmentFilters() {
    return (Filter[])readOnlyAttachmentFilters().clone();
  }
  
  public final void setAttachmentFilters(Filter... filters) {
    checkAccess();
    if (filters.length == 0) {
      filters = emptyFilterArray();
    } else {
      filters = copyOf(filters, filters.length, Filter[].class);
    } 
    synchronized (this) {
      if (this.attachmentFormatters.length != filters.length)
        throw attachmentMismatch(this.attachmentFormatters.length, filters.length); 
      if (this.isWriting)
        throw new IllegalStateException(); 
      if (this.size != 0)
        for (int i = 0; i < filters.length; i++) {
          if (filters[i] != this.attachmentFilters[i]) {
            clearMatches(i);
            break;
          } 
        }  
      this.attachmentFilters = filters;
    } 
  }
  
  public final Formatter[] getAttachmentFormatters() {
    Formatter[] formatters;
    synchronized (this) {
      formatters = this.attachmentFormatters;
    } 
    return (Formatter[])formatters.clone();
  }
  
  public final void setAttachmentFormatters(Formatter... formatters) {
    checkAccess();
    if (formatters.length == 0) {
      formatters = emptyFormatterArray();
    } else {
      formatters = copyOf(formatters, formatters.length, Formatter[].class);
      for (int i = 0; i < formatters.length; i++) {
        if (formatters[i] == null)
          throw new NullPointerException(atIndexMsg(i)); 
      } 
    } 
    synchronized (this) {
      if (this.isWriting)
        throw new IllegalStateException(); 
      this.attachmentFormatters = formatters;
      alignAttachmentFilters();
      alignAttachmentNames();
    } 
  }
  
  public final Formatter[] getAttachmentNames() {
    Formatter[] formatters;
    synchronized (this) {
      formatters = this.attachmentNames;
    } 
    return (Formatter[])formatters.clone();
  }
  
  public final void setAttachmentNames(String... names) {
    Formatter[] formatters;
    checkAccess();
    if (names.length == 0) {
      formatters = emptyFormatterArray();
    } else {
      formatters = new Formatter[names.length];
    } 
    for (int i = 0; i < names.length; i++) {
      String name = names[i];
      if (name != null) {
        if (name.length() > 0) {
          formatters[i] = TailNameFormatter.of(name);
        } else {
          throw new IllegalArgumentException(atIndexMsg(i));
        } 
      } else {
        throw new NullPointerException(atIndexMsg(i));
      } 
    } 
    synchronized (this) {
      if (this.attachmentFormatters.length != names.length)
        throw attachmentMismatch(this.attachmentFormatters.length, names.length); 
      if (this.isWriting)
        throw new IllegalStateException(); 
      this.attachmentNames = formatters;
    } 
  }
  
  public final void setAttachmentNames(Formatter... formatters) {
    checkAccess();
    if (formatters.length == 0) {
      formatters = emptyFormatterArray();
    } else {
      formatters = copyOf(formatters, formatters.length, Formatter[].class);
    } 
    for (int i = 0; i < formatters.length; i++) {
      if (formatters[i] == null)
        throw new NullPointerException(atIndexMsg(i)); 
    } 
    synchronized (this) {
      if (this.attachmentFormatters.length != formatters.length)
        throw attachmentMismatch(this.attachmentFormatters.length, formatters.length); 
      if (this.isWriting)
        throw new IllegalStateException(); 
      this.attachmentNames = formatters;
    } 
  }
  
  public final synchronized Formatter getSubject() {
    return this.subjectFormatter;
  }
  
  public final void setSubject(String subject) {
    if (subject != null) {
      setSubject(TailNameFormatter.of(subject));
    } else {
      checkAccess();
      throw new NullPointerException();
    } 
  }
  
  public final void setSubject(Formatter format) {
    checkAccess();
    if (format == null)
      throw new NullPointerException(); 
    synchronized (this) {
      if (this.isWriting)
        throw new IllegalStateException(); 
      this.subjectFormatter = format;
    } 
  }
  
  protected void reportError(String msg, Exception ex, int code) {
    try {
      if (msg != null) {
        this.errorManager.error(Level.SEVERE.getName()
            .concat(": ").concat(msg), ex, code);
      } else {
        this.errorManager.error(null, ex, code);
      } 
    } catch (RuntimeException GLASSFISH_21258) {
      reportLinkageError(GLASSFISH_21258, code);
    } catch (LinkageError GLASSFISH_21258) {
      reportLinkageError(GLASSFISH_21258, code);
    } 
  }
  
  private void checkAccess() {
    if (this.sealed)
      LogManagerProperties.checkLogManagerAccess(); 
  }
  
  final String contentTypeOf(String head) {
    if (!isEmpty(head)) {
      int MAX_CHARS = 25;
      if (head.length() > 25)
        head = head.substring(0, 25); 
      try {
        String charset = getEncodingName();
        ByteArrayInputStream in = new ByteArrayInputStream(head.getBytes(charset));
        assert in.markSupported() : in.getClass().getName();
        return URLConnection.guessContentTypeFromStream(in);
      } catch (IOException IOE) {
        reportError(IOE.getMessage(), IOE, 5);
      } 
    } 
    return null;
  }
  
  final String contentTypeOf(Formatter f) {
    if (f != null)
      for (Class<?> k = f.getClass(); k != Formatter.class; 
        k = k.getSuperclass()) {
        String name = k.getName().toLowerCase(Locale.ENGLISH);
        int idx = name.indexOf('$') + 1;
        for (; (idx = name.indexOf("ml", idx)) > -1; idx += 2) {
          if (idx > 0) {
            if (name.charAt(idx - 1) == 'x')
              return "application/xml"; 
            if (idx > 1 && name.charAt(idx - 2) == 'h' && name
              .charAt(idx - 1) == 't')
              return "text/html"; 
          } 
        } 
      }  
    return "text/plain";
  }
  
  final boolean isMissingContent(Message msg, Throwable t) {
    Object ccl = getAndSetContextClassLoader(MAILHANDLER_LOADER);
    try {
      msg.writeTo(new ByteArrayOutputStream(1024));
    } catch (RuntimeException RE) {
      throw RE;
    } catch (Exception noContent) {
      String txt = noContent.getMessage();
      if (!isEmpty(txt))
        for (; t != null; t = t.getCause()) {
          if (noContent.getClass() == t.getClass() && txt
            .equals(t.getMessage()))
            return true; 
        }  
    } finally {
      getAndSetContextClassLoader(ccl);
    } 
    return false;
  }
  
  private void reportError(Message msg, Exception ex, int code) {
    try {
      try {
        this.errorManager.error(toRawString(msg), ex, code);
      } catch (RuntimeException re) {
        reportError(toMsgString(re), ex, code);
      } catch (Exception e) {
        reportError(toMsgString(e), ex, code);
      } 
    } catch (LinkageError GLASSFISH_21258) {
      reportLinkageError(GLASSFISH_21258, code);
    } 
  }
  
  private void reportLinkageError(Throwable le, int code) {
    if (le == null)
      throw new NullPointerException(String.valueOf(code)); 
    Integer idx = MUTEX.get();
    if (idx == null || idx.intValue() > MUTEX_LINKAGE.intValue()) {
      MUTEX.set(MUTEX_LINKAGE);
      try {
        Thread.currentThread().getUncaughtExceptionHandler()
          .uncaughtException(Thread.currentThread(), le);
      } catch (RuntimeException runtimeException) {
      
      } catch (LinkageError linkageError) {
      
      } finally {
        if (idx != null) {
          MUTEX.set(idx);
        } else {
          MUTEX.remove();
        } 
      } 
    } 
  }
  
  private String getContentType(String name) {
    assert Thread.holdsLock(this);
    String type = this.contentTypes.getContentType(name);
    if ("application/octet-stream".equalsIgnoreCase(type))
      return null; 
    return type;
  }
  
  private String getEncodingName() {
    String charset = getEncoding();
    if (charset == null)
      charset = MimeUtility.getDefaultJavaCharset(); 
    return charset;
  }
  
  private void setContent(MimeBodyPart part, CharSequence buf, String type) throws MessagingException {
    String charset = getEncodingName();
    if (type != null && !"text/plain".equalsIgnoreCase(type)) {
      type = contentWithEncoding(type, charset);
      try {
        DataSource source = new ByteArrayDataSource(buf.toString(), type);
        part.setDataHandler(new DataHandler(source));
      } catch (IOException IOE) {
        reportError(IOE.getMessage(), IOE, 5);
        part.setText(buf.toString(), charset);
      } 
    } else {
      part.setText(buf.toString(), MimeUtility.mimeCharset(charset));
    } 
  }
  
  private String contentWithEncoding(String type, String encoding) {
    assert encoding != null;
    try {
      ContentType ct = new ContentType(type);
      ct.setParameter("charset", MimeUtility.mimeCharset(encoding));
      encoding = ct.toString();
      if (!isEmpty(encoding))
        type = encoding; 
    } catch (MessagingException ME) {
      reportError(type, ME, 5);
    } 
    return type;
  }
  
  private synchronized void setCapacity0(int newCapacity) {
    checkAccess();
    if (newCapacity <= 0)
      throw new IllegalArgumentException("Capacity must be greater than zero."); 
    if (this.isWriting)
      throw new IllegalStateException(); 
    if (this.capacity < 0) {
      this.capacity = -newCapacity;
    } else {
      this.capacity = newCapacity;
    } 
  }
  
  private Filter[] readOnlyAttachmentFilters() {
    return this.attachmentFilters;
  }
  
  private static Formatter[] emptyFormatterArray() {
    return EMPTY_FORMATTERS;
  }
  
  private static Filter[] emptyFilterArray() {
    return EMPTY_FILTERS;
  }
  
  private boolean alignAttachmentNames() {
    assert Thread.holdsLock(this);
    boolean fixed = false;
    int expect = this.attachmentFormatters.length;
    int current = this.attachmentNames.length;
    if (current != expect) {
      this.attachmentNames = copyOf(this.attachmentNames, expect, Formatter[].class);
      fixed = (current != 0);
    } 
    if (expect == 0) {
      this.attachmentNames = emptyFormatterArray();
      assert this.attachmentNames.length == 0;
    } else {
      for (int i = 0; i < expect; i++) {
        if (this.attachmentNames[i] == null)
          this.attachmentNames[i] = TailNameFormatter.of(
              toString(this.attachmentFormatters[i])); 
      } 
    } 
    return fixed;
  }
  
  private boolean alignAttachmentFilters() {
    assert Thread.holdsLock(this);
    boolean fixed = false;
    int expect = this.attachmentFormatters.length;
    int current = this.attachmentFilters.length;
    if (current != expect) {
      this.attachmentFilters = copyOf(this.attachmentFilters, expect, Filter[].class);
      clearMatches(current);
      fixed = (current != 0);
      Filter body = this.filter;
      if (body != null)
        for (int i = current; i < expect; i++)
          this.attachmentFilters[i] = body;  
    } 
    if (expect == 0) {
      this.attachmentFilters = emptyFilterArray();
      assert this.attachmentFilters.length == 0;
    } 
    return fixed;
  }
  
  private static int[] copyOf(int[] a, int len) {
    int[] copy = new int[len];
    System.arraycopy(a, 0, copy, 0, Math.min(len, a.length));
    return copy;
  }
  
  private static <T, U> T[] copyOf(U[] a, int len, Class<? extends T[]> type) {
    T[] copy = (T[])Array.newInstance(type.getComponentType(), len);
    System.arraycopy(a, 0, copy, 0, Math.min(len, a.length));
    return copy;
  }
  
  private void reset() {
    assert Thread.holdsLock(this);
    if (this.size < this.data.length) {
      Arrays.fill((Object[])this.data, 0, this.size, (Object)null);
    } else {
      Arrays.fill((Object[])this.data, (Object)null);
    } 
    this.size = 0;
  }
  
  private void grow() {
    assert Thread.holdsLock(this);
    int len = this.data.length;
    int newCapacity = len + (len >> 1) + 1;
    if (newCapacity > this.capacity || newCapacity < len)
      newCapacity = this.capacity; 
    assert len != this.capacity : len;
    this.data = copyOf(this.data, newCapacity, LogRecord[].class);
    this.matched = copyOf(this.matched, newCapacity);
  }
  
  private synchronized void init(Properties props) {
    assert this.errorManager != null;
    String p = getClass().getName();
    this.mailProps = new Properties();
    Object ccl = getAndSetContextClassLoader(MAILHANDLER_LOADER);
    try {
      this.contentTypes = FileTypeMap.getDefaultFileTypeMap();
    } finally {
      getAndSetContextClassLoader(ccl);
    } 
    initErrorManager(p);
    initLevel(p);
    initFilter(p);
    initCapacity(p);
    initAuthenticator(p);
    initEncoding(p);
    initFormatter(p);
    initComparator(p);
    initPushLevel(p);
    initPushFilter(p);
    initSubject(p);
    initAttachmentFormaters(p);
    initAttachmentFilters(p);
    initAttachmentNames(p);
    if (props == null && LogManagerProperties.fromLogManager(p.concat(".verify")) != null)
      verifySettings(initSession()); 
    intern();
  }
  
  private void intern() {
    assert Thread.holdsLock(this);
    try {
      Map<Object, Object> seen = new HashMap<Object, Object>();
      try {
        intern(seen, this.errorManager);
      } catch (SecurityException se) {
        reportError(se.getMessage(), se, 4);
      } 
      try {
        Object object1 = this.filter;
        Object object2 = intern(seen, object1);
        if (object2 != object1 && object2 instanceof Filter)
          this.filter = (Filter)object2; 
        object1 = this.formatter;
        object2 = intern(seen, object1);
        if (object2 != object1 && object2 instanceof Formatter)
          this.formatter = (Formatter)object2; 
      } catch (SecurityException se) {
        reportError(se.getMessage(), se, 4);
      } 
      Object canidate = this.subjectFormatter;
      Object result = intern(seen, canidate);
      if (result != canidate && result instanceof Formatter)
        this.subjectFormatter = (Formatter)result; 
      canidate = this.pushFilter;
      result = intern(seen, canidate);
      if (result != canidate && result instanceof Filter)
        this.pushFilter = (Filter)result; 
      for (int i = 0; i < this.attachmentFormatters.length; i++) {
        canidate = this.attachmentFormatters[i];
        result = intern(seen, canidate);
        if (result != canidate && result instanceof Formatter)
          this.attachmentFormatters[i] = (Formatter)result; 
        canidate = this.attachmentFilters[i];
        result = intern(seen, canidate);
        if (result != canidate && result instanceof Filter)
          this.attachmentFilters[i] = (Filter)result; 
        canidate = this.attachmentNames[i];
        result = intern(seen, canidate);
        if (result != canidate && result instanceof Formatter)
          this.attachmentNames[i] = (Formatter)result; 
      } 
    } catch (Exception skip) {
      reportError(skip.getMessage(), skip, 4);
    } catch (LinkageError skip) {
      reportError(skip.getMessage(), new InvocationTargetException(skip), 4);
    } 
  }
  
  private Object intern(Map<Object, Object> m, Object o) throws Exception {
    Object key;
    Object use;
    if (o == null)
      return null; 
    if (o.getClass().getName().equals(TailNameFormatter.class.getName())) {
      key = o;
    } else {
      key = o.getClass().getConstructor(new Class[0]).newInstance(new Object[0]);
    } 
    if (key.getClass() == o.getClass()) {
      Object found = m.get(key);
      if (found == null) {
        boolean right = key.equals(o);
        boolean left = o.equals(key);
        if (right && left) {
          found = m.put(o, o);
          if (found != null) {
            reportNonDiscriminating(key, found);
            found = m.remove(key);
            if (found != o) {
              reportNonDiscriminating(key, found);
              m.clear();
            } 
          } 
        } else if (right != left) {
          reportNonSymmetric(o, key);
        } 
        use = o;
      } else if (o.getClass() == found.getClass()) {
        use = found;
      } else {
        reportNonDiscriminating(o, found);
        use = o;
      } 
    } else {
      use = o;
    } 
    return use;
  }
  
  private static Formatter createSimpleFormatter() {
    return Formatter.class.cast(new SimpleFormatter());
  }
  
  private static boolean isEmpty(String s) {
    return (s == null || s.length() == 0);
  }
  
  private static boolean hasValue(String name) {
    return (!isEmpty(name) && !"null".equalsIgnoreCase(name));
  }
  
  private void initAttachmentFilters(String p) {
    assert Thread.holdsLock(this);
    assert this.attachmentFormatters != null;
    String list = LogManagerProperties.fromLogManager(p.concat(".attachment.filters"));
    if (!isEmpty(list)) {
      String[] names = list.split(",");
      Filter[] a = new Filter[names.length];
      for (int i = 0; i < a.length; i++) {
        names[i] = names[i].trim();
        if (!"null".equalsIgnoreCase(names[i]))
          try {
            a[i] = LogManagerProperties.newFilter(names[i]);
          } catch (SecurityException SE) {
            throw SE;
          } catch (Exception E) {
            reportError(E.getMessage(), E, 4);
          }  
      } 
      this.attachmentFilters = a;
      if (alignAttachmentFilters())
        reportError("Attachment filters.", 
            attachmentMismatch("Length mismatch."), 4); 
    } else {
      this.attachmentFilters = emptyFilterArray();
      alignAttachmentFilters();
    } 
  }
  
  private void initAttachmentFormaters(String p) {
    assert Thread.holdsLock(this);
    String list = LogManagerProperties.fromLogManager(p.concat(".attachment.formatters"));
    if (!isEmpty(list)) {
      Formatter[] a;
      String[] names = list.split(",");
      if (names.length == 0) {
        a = emptyFormatterArray();
      } else {
        a = new Formatter[names.length];
      } 
      for (int i = 0; i < a.length; i++) {
        names[i] = names[i].trim();
        if (!"null".equalsIgnoreCase(names[i])) {
          try {
            a[i] = LogManagerProperties.newFormatter(names[i]);
            if (a[i] instanceof TailNameFormatter) {
              Exception CNFE = new ClassNotFoundException(a[i].toString());
              reportError("Attachment formatter.", CNFE, 4);
              a[i] = createSimpleFormatter();
            } 
          } catch (SecurityException SE) {
            throw SE;
          } catch (Exception E) {
            reportError(E.getMessage(), E, 4);
            a[i] = createSimpleFormatter();
          } 
        } else {
          Exception NPE = new NullPointerException(atIndexMsg(i));
          reportError("Attachment formatter.", NPE, 4);
          a[i] = createSimpleFormatter();
        } 
      } 
      this.attachmentFormatters = a;
    } else {
      this.attachmentFormatters = emptyFormatterArray();
    } 
  }
  
  private void initAttachmentNames(String p) {
    assert Thread.holdsLock(this);
    assert this.attachmentFormatters != null;
    String list = LogManagerProperties.fromLogManager(p.concat(".attachment.names"));
    if (!isEmpty(list)) {
      String[] names = list.split(",");
      Formatter[] a = new Formatter[names.length];
      for (int i = 0; i < a.length; i++) {
        names[i] = names[i].trim();
        if (!"null".equalsIgnoreCase(names[i])) {
          try {
            try {
              a[i] = LogManagerProperties.newFormatter(names[i]);
            } catch (ClassNotFoundException literal) {
              a[i] = TailNameFormatter.of(names[i]);
            } catch (ClassCastException literal) {
              a[i] = TailNameFormatter.of(names[i]);
            } 
          } catch (SecurityException SE) {
            throw SE;
          } catch (Exception E) {
            reportError(E.getMessage(), E, 4);
          } 
        } else {
          Exception NPE = new NullPointerException(atIndexMsg(i));
          reportError("Attachment names.", NPE, 4);
        } 
      } 
      this.attachmentNames = a;
      if (alignAttachmentNames())
        reportError("Attachment names.", 
            attachmentMismatch("Length mismatch."), 4); 
    } else {
      this.attachmentNames = emptyFormatterArray();
      alignAttachmentNames();
    } 
  }
  
  private void initAuthenticator(String p) {
    assert Thread.holdsLock(this);
    String name = LogManagerProperties.fromLogManager(p.concat(".authenticator"));
    if (name != null && !"null".equalsIgnoreCase(name))
      if (name.length() != 0) {
        try {
          this
            .auth = LogManagerProperties.<Authenticator>newObjectFrom(name, Authenticator.class);
        } catch (SecurityException SE) {
          throw SE;
        } catch (ClassNotFoundException literalAuth) {
          this.auth = DefaultAuthenticator.of(name);
        } catch (ClassCastException literalAuth) {
          this.auth = DefaultAuthenticator.of(name);
        } catch (Exception E) {
          reportError(E.getMessage(), E, 4);
        } 
      } else {
        this.auth = DefaultAuthenticator.of(name);
      }  
  }
  
  private void initLevel(String p) {
    assert Thread.holdsLock(this);
    try {
      String val = LogManagerProperties.fromLogManager(p.concat(".level"));
      if (val != null) {
        this.logLevel = Level.parse(val);
      } else {
        this.logLevel = Level.WARNING;
      } 
    } catch (SecurityException SE) {
      throw SE;
    } catch (RuntimeException RE) {
      reportError(RE.getMessage(), RE, 4);
      this.logLevel = Level.WARNING;
    } 
  }
  
  private void initFilter(String p) {
    assert Thread.holdsLock(this);
    try {
      String name = LogManagerProperties.fromLogManager(p.concat(".filter"));
      if (hasValue(name))
        this.filter = LogManagerProperties.newFilter(name); 
    } catch (SecurityException SE) {
      throw SE;
    } catch (Exception E) {
      reportError(E.getMessage(), E, 4);
    } 
  }
  
  private void initCapacity(String p) {
    assert Thread.holdsLock(this);
    int DEFAULT_CAPACITY = 1000;
    try {
      String value = LogManagerProperties.fromLogManager(p.concat(".capacity"));
      if (value != null) {
        setCapacity0(Integer.parseInt(value));
      } else {
        setCapacity0(1000);
      } 
    } catch (SecurityException SE) {
      throw SE;
    } catch (RuntimeException RE) {
      reportError(RE.getMessage(), RE, 4);
    } 
    if (this.capacity <= 0)
      this.capacity = 1000; 
    this.data = new LogRecord[1];
    this.matched = new int[this.data.length];
  }
  
  private void initEncoding(String p) {
    assert Thread.holdsLock(this);
    try {
      String e = LogManagerProperties.fromLogManager(p.concat(".encoding"));
      if (e != null)
        setEncoding0(e); 
    } catch (SecurityException SE) {
      throw SE;
    } catch (UnsupportedEncodingException UEE) {
      reportError(UEE.getMessage(), UEE, 4);
    } catch (RuntimeException RE) {
      reportError(RE.getMessage(), RE, 4);
    } 
  }
  
  private ErrorManager defaultErrorManager() {
    ErrorManager em;
    try {
      em = super.getErrorManager();
    } catch (RuntimeException ignore) {
      em = null;
    } catch (LinkageError ignore) {
      em = null;
    } 
    if (em == null)
      em = new ErrorManager(); 
    return em;
  }
  
  private void initErrorManager(String p) {
    assert Thread.holdsLock(this);
    try {
      String name = LogManagerProperties.fromLogManager(p.concat(".errorManager"));
      if (name != null)
        setErrorManager0(LogManagerProperties.newErrorManager(name)); 
    } catch (SecurityException SE) {
      throw SE;
    } catch (Exception E) {
      reportError(E.getMessage(), E, 4);
    } 
  }
  
  private void initFormatter(String p) {
    assert Thread.holdsLock(this);
    try {
      String name = LogManagerProperties.fromLogManager(p.concat(".formatter"));
      if (hasValue(name)) {
        Formatter f = LogManagerProperties.newFormatter(name);
        assert f != null;
        if (!(f instanceof TailNameFormatter)) {
          this.formatter = f;
        } else {
          this.formatter = createSimpleFormatter();
        } 
      } else {
        this.formatter = createSimpleFormatter();
      } 
    } catch (SecurityException SE) {
      throw SE;
    } catch (Exception E) {
      reportError(E.getMessage(), E, 4);
      this.formatter = createSimpleFormatter();
    } 
  }
  
  private void initComparator(String p) {
    assert Thread.holdsLock(this);
    try {
      String name = LogManagerProperties.fromLogManager(p.concat(".comparator"));
      String reverse = LogManagerProperties.fromLogManager(p.concat(".comparator.reverse"));
      if (hasValue(name)) {
        this.comparator = LogManagerProperties.newComparator(name);
        if (Boolean.parseBoolean(reverse)) {
          assert this.comparator != null : "null";
          this.comparator = LogManagerProperties.reverseOrder(this.comparator);
        } 
      } else if (!isEmpty(reverse)) {
        throw new IllegalArgumentException("No comparator to reverse.");
      } 
    } catch (SecurityException SE) {
      throw SE;
    } catch (Exception E) {
      reportError(E.getMessage(), E, 4);
    } 
  }
  
  private void initPushLevel(String p) {
    assert Thread.holdsLock(this);
    try {
      String val = LogManagerProperties.fromLogManager(p.concat(".pushLevel"));
      if (val != null)
        this.pushLevel = Level.parse(val); 
    } catch (RuntimeException RE) {
      reportError(RE.getMessage(), RE, 4);
    } 
    if (this.pushLevel == null)
      this.pushLevel = Level.OFF; 
  }
  
  private void initPushFilter(String p) {
    assert Thread.holdsLock(this);
    try {
      String name = LogManagerProperties.fromLogManager(p.concat(".pushFilter"));
      if (hasValue(name))
        this.pushFilter = LogManagerProperties.newFilter(name); 
    } catch (SecurityException SE) {
      throw SE;
    } catch (Exception E) {
      reportError(E.getMessage(), E, 4);
    } 
  }
  
  private void initSubject(String p) {
    assert Thread.holdsLock(this);
    String name = LogManagerProperties.fromLogManager(p.concat(".subject"));
    if (hasValue(name)) {
      try {
        this.subjectFormatter = LogManagerProperties.newFormatter(name);
      } catch (SecurityException SE) {
        throw SE;
      } catch (ClassNotFoundException literalSubject) {
        this.subjectFormatter = TailNameFormatter.of(name);
      } catch (ClassCastException literalSubject) {
        this.subjectFormatter = TailNameFormatter.of(name);
      } catch (Exception E) {
        this.subjectFormatter = TailNameFormatter.of(name);
        reportError(E.getMessage(), E, 4);
      } 
    } else if (name != null) {
      this.subjectFormatter = TailNameFormatter.of(name);
    } 
    if (this.subjectFormatter == null)
      this.subjectFormatter = TailNameFormatter.of(""); 
  }
  
  private boolean isAttachmentLoggable(LogRecord record) {
    Filter[] filters = readOnlyAttachmentFilters();
    for (int i = 0; i < filters.length; i++) {
      Filter f = filters[i];
      if (f == null || f.isLoggable(record)) {
        setMatchedPart(i);
        return true;
      } 
    } 
    return false;
  }
  
  private boolean isPushable(LogRecord record) {
    assert Thread.holdsLock(this);
    int value = getPushLevel().intValue();
    if (value == offValue || record.getLevel().intValue() < value)
      return false; 
    Filter push = getPushFilter();
    if (push == null)
      return true; 
    int match = getMatchedPart();
    if ((match == -1 && getFilter() == push) || (match >= 0 && this.attachmentFilters[match] == push))
      return true; 
    return push.isLoggable(record);
  }
  
  private void push(boolean priority, int code) {
    if (tryMutex()) {
      try {
        Message msg = writeLogRecords(code);
        if (msg != null)
          send(msg, priority, code); 
      } catch (LinkageError JDK8152515) {
        reportLinkageError(JDK8152515, code);
      } finally {
        releaseMutex();
      } 
    } else {
      reportUnPublishedError(null);
    } 
  }
  
  private void send(Message msg, boolean priority, int code) {
    try {
      envelopeFor(msg, priority);
      Object ccl = getAndSetContextClassLoader(MAILHANDLER_LOADER);
      try {
        Transport.send(msg);
      } finally {
        getAndSetContextClassLoader(ccl);
      } 
    } catch (RuntimeException re) {
      reportError(msg, re, code);
    } catch (Exception e) {
      reportError(msg, e, code);
    } 
  }
  
  private void sort() {
    assert Thread.holdsLock(this);
    if (this.comparator != null)
      try {
        if (this.size != 1) {
          Arrays.sort(this.data, 0, this.size, this.comparator);
        } else if (this.comparator.compare(this.data[0], this.data[0]) != 0) {
          throw new IllegalArgumentException(this.comparator
              .getClass().getName());
        } 
      } catch (RuntimeException RE) {
        reportError(RE.getMessage(), RE, 5);
      }  
  }
  
  private Message writeLogRecords(int code) {
    try {
      synchronized (this) {
        if (this.size > 0 && !this.isWriting) {
          this.isWriting = true;
          try {
            return writeLogRecords0();
          } finally {
            this.isWriting = false;
            if (this.size > 0)
              reset(); 
          } 
        } 
      } 
    } catch (RuntimeException re) {
      reportError(re.getMessage(), re, code);
    } catch (Exception e) {
      reportError(e.getMessage(), e, code);
    } 
    return null;
  }
  
  private Message writeLogRecords0() throws Exception {
    assert Thread.holdsLock(this);
    sort();
    if (this.session == null)
      initSession(); 
    MimeMessage msg = new MimeMessage(this.session);
    msg.setDescription(descriptionFrom(this.comparator, this.pushLevel, this.pushFilter));
    MimeBodyPart[] parts = new MimeBodyPart[this.attachmentFormatters.length];
    StringBuilder[] buffers = new StringBuilder[parts.length];
    String contentType = null;
    StringBuilder buf = null;
    appendSubject(msg, head(this.subjectFormatter));
    MimeBodyPart body = createBodyPart();
    Formatter bodyFormat = getFormatter();
    Filter bodyFilter = getFilter();
    Locale lastLocale = null;
    for (int ix = 0; ix < this.size; ix++) {
      boolean formatted = false;
      int match = this.matched[ix];
      LogRecord r = this.data[ix];
      this.data[ix] = null;
      Locale locale = localeFor(r);
      appendSubject(msg, format(this.subjectFormatter, r));
      Filter lmf = null;
      if (bodyFilter == null || match == -1 || parts.length == 0 || (match < -1 && bodyFilter
        .isLoggable(r))) {
        lmf = bodyFilter;
        if (buf == null) {
          buf = new StringBuilder();
          String head = head(bodyFormat);
          buf.append(head);
          contentType = contentTypeOf(head);
        } 
        formatted = true;
        buf.append(format(bodyFormat, r));
        if (locale != null && !locale.equals(lastLocale))
          appendContentLang(body, locale); 
      } 
      for (int k = 0; k < parts.length; k++) {
        Filter af = this.attachmentFilters[k];
        if (af == null || lmf == af || match == k || (match < k && af
          .isLoggable(r))) {
          if (lmf == null && af != null)
            lmf = af; 
          if (parts[k] == null) {
            parts[k] = createBodyPart(k);
            buffers[k] = new StringBuilder();
            buffers[k].append(head(this.attachmentFormatters[k]));
            appendFileName(parts[k], head(this.attachmentNames[k]));
          } 
          formatted = true;
          appendFileName(parts[k], format(this.attachmentNames[k], r));
          buffers[k].append(format(this.attachmentFormatters[k], r));
          if (locale != null && !locale.equals(lastLocale))
            appendContentLang(parts[k], locale); 
        } 
      } 
      if (formatted) {
        if (locale != null && !locale.equals(lastLocale))
          appendContentLang(msg, locale); 
      } else {
        reportFilterError(r);
      } 
      lastLocale = locale;
    } 
    this.size = 0;
    for (int i = parts.length - 1; i >= 0; i--) {
      if (parts[i] != null) {
        appendFileName(parts[i], tail(this.attachmentNames[i], "err"));
        buffers[i].append(tail(this.attachmentFormatters[i], ""));
        if (buffers[i].length() > 0) {
          String name = parts[i].getFileName();
          if (isEmpty(name)) {
            name = toString(this.attachmentFormatters[i]);
            parts[i].setFileName(name);
          } 
          setContent(parts[i], buffers[i], getContentType(name));
        } else {
          setIncompleteCopy(msg);
          parts[i] = null;
        } 
        buffers[i] = null;
      } 
    } 
    if (buf != null) {
      buf.append(tail(bodyFormat, ""));
    } else {
      buf = new StringBuilder(0);
    } 
    appendSubject(msg, tail(this.subjectFormatter, ""));
    MimeMultipart multipart = new MimeMultipart();
    String altType = getContentType(bodyFormat.getClass().getName());
    setContent(body, buf, (altType == null) ? contentType : altType);
    multipart.addBodyPart(body);
    for (int j = 0; j < parts.length; j++) {
      if (parts[j] != null)
        multipart.addBodyPart(parts[j]); 
    } 
    msg.setContent(multipart);
    return msg;
  }
  
  private void verifySettings(Session session) {
    try {
      if (session != null) {
        Properties props = session.getProperties();
        Object check = props.put("verify", "");
        if (check instanceof String) {
          String value = (String)check;
          if (hasValue(value))
            verifySettings0(session, value); 
        } else if (check != null) {
          verifySettings0(session, check.getClass().toString());
        } 
      } 
    } catch (LinkageError JDK8152515) {
      reportLinkageError(JDK8152515, 4);
    } 
  }
  
  private void verifySettings0(Session session, String verify) {
    String msg, atn[];
    assert verify != null : (String)null;
    if (!"local".equals(verify) && !"remote".equals(verify) && 
      !"limited".equals(verify) && !"resolve".equals(verify)) {
      reportError("Verify must be 'limited', local', 'resolve' or 'remote'.", new IllegalArgumentException(verify), 4);
      return;
    } 
    MimeMessage abort = new MimeMessage(session);
    if (!"limited".equals(verify)) {
      msg = "Local address is " + InternetAddress.getLocalAddress(session) + '.';
      try {
        Charset.forName(getEncodingName());
      } catch (RuntimeException RE) {
        UnsupportedEncodingException UEE = new UnsupportedEncodingException(RE.toString());
        UEE.initCause(RE);
        reportError(msg, UEE, 5);
      } 
    } else {
      msg = "Skipping local address check.";
    } 
    synchronized (this) {
      appendSubject(abort, head(this.subjectFormatter));
      appendSubject(abort, tail(this.subjectFormatter, ""));
      atn = new String[this.attachmentNames.length];
      for (int i = 0; i < atn.length; i++) {
        atn[i] = head(this.attachmentNames[i]);
        if (atn[i].length() == 0) {
          atn[i] = tail(this.attachmentNames[i], "");
        } else {
          atn[i] = atn[i].concat(tail(this.attachmentNames[i], ""));
        } 
      } 
    } 
    setIncompleteCopy(abort);
    envelopeFor(abort, true);
    try {
      abort.saveChanges();
    } catch (MessagingException ME) {
      reportError(msg, ME, 5);
    } 
    try {
      InternetAddress[] arrayOfInternetAddress;
      Transport t;
      Address[] all = abort.getAllRecipients();
      if (all == null)
        arrayOfInternetAddress = new InternetAddress[0]; 
      try {
        Address[] any = (arrayOfInternetAddress.length != 0) ? (Address[])arrayOfInternetAddress : abort.getFrom();
        if (any != null && any.length != 0) {
          t = session.getTransport(any[0]);
          session.getProperty("mail.transport.protocol");
        } else {
          MessagingException me = new MessagingException("No recipient or from address.");
          reportError(msg, me, 4);
          throw me;
        } 
      } catch (MessagingException protocol) {
        Object ccl = getAndSetContextClassLoader(MAILHANDLER_LOADER);
        try {
          t = session.getTransport();
        } catch (MessagingException fail) {
          throw attach(protocol, fail);
        } finally {
          getAndSetContextClassLoader(ccl);
        } 
      } 
      String local = null;
      if ("remote".equals(verify)) {
        MessagingException closed = null;
        t.connect();
        try {
          try {
            local = getLocalHost(t);
            t.sendMessage(abort, (Address[])arrayOfInternetAddress);
          } finally {
            try {
              t.close();
            } catch (MessagingException ME) {
              closed = ME;
            } 
          } 
          reportUnexpectedSend(abort, verify, null);
        } catch (SendFailedException sfe) {
          Address[] recip = sfe.getInvalidAddresses();
          if (recip != null && recip.length != 0) {
            setErrorContent(abort, verify, sfe);
            reportError(abort, sfe, 4);
          } 
          recip = sfe.getValidSentAddresses();
          if (recip != null && recip.length != 0)
            reportUnexpectedSend(abort, verify, sfe); 
        } catch (MessagingException ME) {
          if (!isMissingContent(abort, ME)) {
            setErrorContent(abort, verify, ME);
            reportError(abort, ME, 4);
          } 
        } 
        if (closed != null) {
          setErrorContent(abort, verify, closed);
          reportError(abort, closed, 3);
        } 
      } else {
        String protocol = t.getURLName().getProtocol();
        String mailHost = session.getProperty("mail." + protocol + ".host");
        if (isEmpty(mailHost)) {
          mailHost = session.getProperty("mail.host");
        } else {
          session.getProperty("mail.host");
        } 
        session.getProperty("mail." + protocol + ".port");
        session.getProperty("mail." + protocol + ".user");
        session.getProperty("mail.user");
        session.getProperty("mail." + protocol + ".localport");
        local = session.getProperty("mail." + protocol + ".localhost");
        if (isEmpty(local)) {
          local = session.getProperty("mail." + protocol + ".localaddress");
        } else {
          session.getProperty("mail." + protocol + ".localaddress");
        } 
        if ("resolve".equals(verify))
          try {
            String transportHost = t.getURLName().getHost();
            if (!isEmpty(transportHost)) {
              verifyHost(transportHost);
              if (!transportHost.equalsIgnoreCase(mailHost))
                verifyHost(mailHost); 
            } else {
              verifyHost(mailHost);
            } 
          } catch (IOException IOE) {
            MessagingException ME = new MessagingException(msg, IOE);
            setErrorContent(abort, verify, ME);
            reportError(abort, ME, 4);
          } catch (RuntimeException RE) {
            MessagingException ME = new MessagingException(msg, RE);
            setErrorContent(abort, verify, RE);
            reportError(abort, ME, 4);
          }  
      } 
      if (!"limited".equals(verify)) {
        try {
          if (!"remote".equals(verify))
            local = getLocalHost(t); 
          verifyHost(local);
        } catch (IOException IOE) {
          MessagingException ME = new MessagingException(msg, IOE);
          setErrorContent(abort, verify, ME);
          reportError(abort, ME, 4);
        } catch (RuntimeException RE) {
          MessagingException ME = new MessagingException(msg, RE);
          setErrorContent(abort, verify, ME);
          reportError(abort, ME, 4);
        } 
        try {
          Object ccl = getAndSetContextClassLoader(MAILHANDLER_LOADER);
          try {
            MimeBodyPart body;
            String bodyContentType;
            MimeMultipart multipart = new MimeMultipart();
            MimeBodyPart[] ambp = new MimeBodyPart[atn.length];
            synchronized (this) {
              bodyContentType = contentTypeOf(getFormatter());
              body = createBodyPart();
              for (int j = 0; j < atn.length; j++) {
                ambp[j] = createBodyPart(j);
                ambp[j].setFileName(atn[j]);
                atn[j] = getContentType(atn[j]);
              } 
            } 
            body.setDescription(verify);
            setContent(body, "", bodyContentType);
            multipart.addBodyPart(body);
            for (int i = 0; i < ambp.length; i++) {
              ambp[i].setDescription(verify);
              setContent(ambp[i], "", atn[i]);
            } 
            abort.setContent(multipart);
            abort.saveChanges();
            abort.writeTo(new ByteArrayOutputStream(1024));
          } finally {
            getAndSetContextClassLoader(ccl);
          } 
        } catch (IOException IOE) {
          MessagingException ME = new MessagingException(msg, IOE);
          setErrorContent(abort, verify, ME);
          reportError(abort, ME, 5);
        } 
      } 
      if (arrayOfInternetAddress.length != 0) {
        verifyAddresses((Address[])arrayOfInternetAddress);
      } else {
        throw new MessagingException("No recipient addresses.");
      } 
      Address[] from = abort.getFrom();
      Address sender = abort.getSender();
      if (sender instanceof InternetAddress)
        ((InternetAddress)sender).validate(); 
      if (abort.getHeader("From", ",") != null && from.length != 0) {
        verifyAddresses(from);
        for (int i = 0; i < from.length; i++) {
          if (from[i].equals(sender)) {
            MessagingException ME = new MessagingException("Sender address '" + sender + "' equals from address.");
            throw new MessagingException(msg, ME);
          } 
        } 
      } else if (sender == null) {
        MessagingException ME = new MessagingException("No from or sender address.");
        throw new MessagingException(msg, ME);
      } 
      verifyAddresses(abort.getReplyTo());
    } catch (RuntimeException RE) {
      setErrorContent(abort, verify, RE);
      reportError(abort, RE, 4);
    } catch (Exception ME) {
      setErrorContent(abort, verify, ME);
      reportError(abort, ME, 4);
    } 
  }
  
  private static InetAddress verifyHost(String host) throws IOException {
    InetAddress a;
    if (isEmpty(host)) {
      a = InetAddress.getLocalHost();
    } else {
      a = InetAddress.getByName(host);
    } 
    if (a.getCanonicalHostName().length() == 0)
      throw new UnknownHostException(); 
    return a;
  }
  
  private static void verifyAddresses(Address[] all) throws AddressException {
    if (all != null)
      for (int i = 0; i < all.length; i++) {
        Address a = all[i];
        if (a instanceof InternetAddress)
          ((InternetAddress)a).validate(); 
      }  
  }
  
  private void reportUnexpectedSend(MimeMessage msg, String verify, Exception cause) {
    MessagingException write = new MessagingException("An empty message was sent.", cause);
    setErrorContent(msg, verify, write);
    reportError(msg, write, 4);
  }
  
  private void setErrorContent(MimeMessage msg, String verify, Throwable t) {
    try {
      MimeBodyPart body;
      String subjectType, msgDesc;
      synchronized (this) {
        body = createBodyPart();
        msgDesc = descriptionFrom(this.comparator, this.pushLevel, this.pushFilter);
        subjectType = getClassId(this.subjectFormatter);
      } 
      body.setDescription("Formatted using " + ((t == null) ? Throwable.class
          .getName() : t
          .getClass().getName()) + ", filtered with " + verify + ", and named by " + subjectType + '.');
      setContent(body, toMsgString(t), "text/plain");
      MimeMultipart multipart = new MimeMultipart();
      multipart.addBodyPart(body);
      msg.setContent(multipart);
      msg.setDescription(msgDesc);
      setAcceptLang(msg);
      msg.saveChanges();
    } catch (MessagingException ME) {
      MimeBodyPart body;
      reportError("Unable to create body.", (Exception)body, 4);
    } catch (RuntimeException RE) {
      reportError("Unable to create body.", RE, 4);
    } 
  }
  
  private Session updateSession() {
    Session settings;
    assert Thread.holdsLock(this);
    if (this.mailProps.getProperty("verify") != null) {
      settings = initSession();
      assert settings == this.session : this.session;
    } else {
      this.session = null;
      settings = null;
    } 
    return settings;
  }
  
  private Session initSession() {
    assert Thread.holdsLock(this);
    String p = getClass().getName();
    LogManagerProperties proxy = new LogManagerProperties(this.mailProps, p);
    this.session = Session.getInstance(proxy, this.auth);
    return this.session;
  }
  
  private void envelopeFor(Message msg, boolean priority) {
    setAcceptLang(msg);
    setFrom(msg);
    if (!setRecipient(msg, "mail.to", Message.RecipientType.TO))
      setDefaultRecipient(msg, Message.RecipientType.TO); 
    setRecipient(msg, "mail.cc", Message.RecipientType.CC);
    setRecipient(msg, "mail.bcc", Message.RecipientType.BCC);
    setReplyTo(msg);
    setSender(msg);
    setMailer(msg);
    setAutoSubmitted(msg);
    if (priority)
      setPriority(msg); 
    try {
      msg.setSentDate(new Date());
    } catch (MessagingException ME) {
      reportError(ME.getMessage(), ME, 5);
    } 
  }
  
  private MimeBodyPart createBodyPart() throws MessagingException {
    assert Thread.holdsLock(this);
    MimeBodyPart part = new MimeBodyPart();
    part.setDisposition("inline");
    part.setDescription(descriptionFrom(getFormatter(), 
          getFilter(), this.subjectFormatter));
    setAcceptLang(part);
    return part;
  }
  
  private MimeBodyPart createBodyPart(int index) throws MessagingException {
    assert Thread.holdsLock(this);
    MimeBodyPart part = new MimeBodyPart();
    part.setDisposition("attachment");
    part.setDescription(descriptionFrom(this.attachmentFormatters[index], this.attachmentFilters[index], this.attachmentNames[index]));
    setAcceptLang(part);
    return part;
  }
  
  private String descriptionFrom(Comparator<?> c, Level l, Filter f) {
    return "Sorted using " + ((c == null) ? "no comparator" : c
      .getClass().getName()) + ", pushed when " + l.getName() + ", and " + ((f == null) ? "no push filter" : f
      
      .getClass().getName()) + '.';
  }
  
  private String descriptionFrom(Formatter f, Filter filter, Formatter name) {
    return "Formatted using " + getClassId(f) + ", filtered with " + ((filter == null) ? "no filter" : filter
      
      .getClass().getName()) + ", and named by " + 
      getClassId(name) + '.';
  }
  
  private String getClassId(Formatter f) {
    if (f instanceof TailNameFormatter)
      return String.class.getName(); 
    return f.getClass().getName();
  }
  
  private String toString(Formatter f) {
    String name = f.toString();
    if (!isEmpty(name))
      return name; 
    return getClassId(f);
  }
  
  private void appendFileName(Part part, String chunk) {
    if (chunk != null) {
      if (chunk.length() > 0)
        appendFileName0(part, chunk); 
    } else {
      reportNullError(5);
    } 
  }
  
  private void appendFileName0(Part part, String chunk) {
    try {
      chunk = chunk.replaceAll("[\\x00-\\x1F\\x7F]+", "");
      String old = part.getFileName();
      part.setFileName((old != null) ? old.concat(chunk) : chunk);
    } catch (MessagingException ME) {
      reportError(ME.getMessage(), ME, 5);
    } 
  }
  
  private void appendSubject(Message msg, String chunk) {
    if (chunk != null) {
      if (chunk.length() > 0)
        appendSubject0(msg, chunk); 
    } else {
      reportNullError(5);
    } 
  }
  
  private void appendSubject0(Message msg, String chunk) {
    try {
      chunk = chunk.replaceAll("[\\x00-\\x1F\\x7F]+", "");
      String charset = getEncodingName();
      String old = msg.getSubject();
      assert msg instanceof MimeMessage : msg;
      ((MimeMessage)msg).setSubject((old != null) ? old.concat(chunk) : chunk, 
          MimeUtility.mimeCharset(charset));
    } catch (MessagingException ME) {
      reportError(ME.getMessage(), ME, 5);
    } 
  }
  
  private Locale localeFor(LogRecord r) {
    Locale l;
    ResourceBundle rb = r.getResourceBundle();
    if (rb != null) {
      l = rb.getLocale();
      if (l == null || isEmpty(l.getLanguage()))
        l = Locale.getDefault(); 
    } else {
      l = null;
    } 
    return l;
  }
  
  private void appendContentLang(MimePart p, Locale l) {
    try {
      String lang = LogManagerProperties.toLanguageTag(l);
      if (lang.length() != 0) {
        String header = p.getHeader("Content-Language", (String)null);
        if (isEmpty(header)) {
          p.setHeader("Content-Language", lang);
        } else if (!header.equalsIgnoreCase(lang)) {
          lang = ",".concat(lang);
          int idx = 0;
          while ((idx = header.indexOf(lang, idx)) > -1) {
            idx += lang.length();
            if (idx == header.length() || header
              .charAt(idx) == ',')
              break; 
          } 
          if (idx < 0) {
            int len = header.lastIndexOf("\r\n\t");
            if (len < 0) {
              len = 20 + header.length();
            } else {
              len = header.length() - len + 8;
            } 
            if (len + lang.length() > 76) {
              header = header.concat("\r\n\t".concat(lang));
            } else {
              header = header.concat(lang);
            } 
            p.setHeader("Content-Language", header);
          } 
        } 
      } 
    } catch (MessagingException ME) {
      reportError(ME.getMessage(), ME, 5);
    } 
  }
  
  private void setAcceptLang(Part p) {
    try {
      String lang = LogManagerProperties.toLanguageTag(Locale.getDefault());
      if (lang.length() != 0)
        p.setHeader("Accept-Language", lang); 
    } catch (MessagingException ME) {
      reportError(ME.getMessage(), ME, 5);
    } 
  }
  
  private void reportFilterError(LogRecord record) {
    assert Thread.holdsLock(this);
    Formatter f = createSimpleFormatter();
    String msg = "Log record " + record.getSequenceNumber() + " was filtered from all message parts.  " + head(f) + format(f, record) + tail(f, "");
    String txt = getFilter() + ", " + Arrays.<Filter>asList(readOnlyAttachmentFilters());
    reportError(msg, new IllegalArgumentException(txt), 5);
  }
  
  private void reportNonSymmetric(Object o, Object found) {
    reportError("Non symmetric equals implementation.", new IllegalArgumentException(o
          .getClass().getName() + " is not equal to " + found
          .getClass().getName()), 4);
  }
  
  private void reportNonDiscriminating(Object o, Object found) {
    reportError("Non discriminating equals implementation.", new IllegalArgumentException(o
          .getClass().getName() + " should not be equal to " + found
          .getClass().getName()), 4);
  }
  
  private void reportNullError(int code) {
    reportError("null", new NullPointerException(), code);
  }
  
  private String head(Formatter f) {
    try {
      return f.getHead(this);
    } catch (RuntimeException RE) {
      reportError(RE.getMessage(), RE, 5);
      return "";
    } 
  }
  
  private String format(Formatter f, LogRecord r) {
    try {
      return f.format(r);
    } catch (RuntimeException RE) {
      reportError(RE.getMessage(), RE, 5);
      return "";
    } 
  }
  
  private String tail(Formatter f, String def) {
    try {
      return f.getTail(this);
    } catch (RuntimeException RE) {
      reportError(RE.getMessage(), RE, 5);
      return def;
    } 
  }
  
  private void setMailer(Message msg) {
    try {
      String value;
      Class<?> mail = MailHandler.class;
      Class<?> k = getClass();
      if (k == mail) {
        value = mail.getName();
      } else {
        try {
          value = MimeUtility.encodeText(k.getName());
        } catch (UnsupportedEncodingException E) {
          reportError(E.getMessage(), E, 5);
          value = k.getName().replaceAll("[^\\x00-\\x7F]", "\032");
        } 
        value = MimeUtility.fold(10, mail.getName() + " using the " + value + " extension.");
      } 
      msg.setHeader("X-Mailer", value);
    } catch (MessagingException ME) {
      reportError(ME.getMessage(), ME, 5);
    } 
  }
  
  private void setPriority(Message msg) {
    try {
      msg.setHeader("Importance", "High");
      msg.setHeader("Priority", "urgent");
      msg.setHeader("X-Priority", "2");
    } catch (MessagingException ME) {
      reportError(ME.getMessage(), ME, 5);
    } 
  }
  
  private void setIncompleteCopy(Message msg) {
    try {
      msg.setHeader("Incomplete-Copy", "");
    } catch (MessagingException ME) {
      reportError(ME.getMessage(), ME, 5);
    } 
  }
  
  private void setAutoSubmitted(Message msg) {
    if (allowRestrictedHeaders())
      try {
        msg.setHeader("auto-submitted", "auto-generated");
      } catch (MessagingException ME) {
        reportError(ME.getMessage(), ME, 5);
      }  
  }
  
  private void setFrom(Message msg) {
    String from = getSession(msg).getProperty("mail.from");
    if (from != null) {
      try {
        InternetAddress[] arrayOfInternetAddress = InternetAddress.parse(from, false);
        if (arrayOfInternetAddress.length > 0)
          if (arrayOfInternetAddress.length == 1) {
            msg.setFrom(arrayOfInternetAddress[0]);
          } else {
            msg.addFrom((Address[])arrayOfInternetAddress);
          }  
      } catch (MessagingException ME) {
        reportError(ME.getMessage(), ME, 5);
        setDefaultFrom(msg);
      } 
    } else {
      setDefaultFrom(msg);
    } 
  }
  
  private void setDefaultFrom(Message msg) {
    try {
      msg.setFrom();
    } catch (MessagingException ME) {
      reportError(ME.getMessage(), ME, 5);
    } 
  }
  
  private void setDefaultRecipient(Message msg, Message.RecipientType type) {
    try {
      Address a = InternetAddress.getLocalAddress(getSession(msg));
      if (a != null) {
        msg.setRecipient(type, a);
      } else {
        MimeMessage m = new MimeMessage(getSession(msg));
        m.setFrom();
        Address[] from = m.getFrom();
        if (from.length > 0) {
          msg.setRecipients(type, from);
        } else {
          throw new MessagingException("No local address.");
        } 
      } 
    } catch (MessagingException ME) {
      reportError("Unable to compute a default recipient.", ME, 5);
    } catch (RuntimeException RE) {
      reportError("Unable to compute a default recipient.", RE, 5);
    } 
  }
  
  private void setReplyTo(Message msg) {
    String reply = getSession(msg).getProperty("mail.reply.to");
    if (!isEmpty(reply))
      try {
        InternetAddress[] arrayOfInternetAddress = InternetAddress.parse(reply, false);
        if (arrayOfInternetAddress.length > 0)
          msg.setReplyTo((Address[])arrayOfInternetAddress); 
      } catch (MessagingException ME) {
        reportError(ME.getMessage(), ME, 5);
      }  
  }
  
  private void setSender(Message msg) {
    assert msg instanceof MimeMessage : msg;
    String sender = getSession(msg).getProperty("mail.sender");
    if (!isEmpty(sender))
      try {
        InternetAddress[] address = InternetAddress.parse(sender, false);
        if (address.length > 0) {
          ((MimeMessage)msg).setSender(address[0]);
          if (address.length > 1)
            reportError("Ignoring other senders.", 
                tooManyAddresses((Address[])address, 1), 5); 
        } 
      } catch (MessagingException ME) {
        reportError(ME.getMessage(), ME, 5);
      }  
  }
  
  private AddressException tooManyAddresses(Address[] address, int offset) {
    Object l = Arrays.<Address>asList(address).subList(offset, address.length);
    return new AddressException(l.toString());
  }
  
  private boolean setRecipient(Message msg, String key, Message.RecipientType type) {
    String value = getSession(msg).getProperty(key);
    boolean containsKey = (value != null);
    if (!isEmpty(value))
      try {
        InternetAddress[] arrayOfInternetAddress = InternetAddress.parse(value, false);
        if (arrayOfInternetAddress.length > 0)
          msg.setRecipients(type, (Address[])arrayOfInternetAddress); 
      } catch (MessagingException ME) {
        reportError(ME.getMessage(), ME, 5);
      }  
    return containsKey;
  }
  
  private String toRawString(Message msg) throws MessagingException, IOException {
    if (msg != null) {
      Object ccl = getAndSetContextClassLoader(MAILHANDLER_LOADER);
      try {
        int nbytes = Math.max(msg.getSize() + 1024, 1024);
        ByteArrayOutputStream out = new ByteArrayOutputStream(nbytes);
        msg.writeTo(out);
        return out.toString("US-ASCII");
      } finally {
        getAndSetContextClassLoader(ccl);
      } 
    } 
    return null;
  }
  
  private String toMsgString(Throwable t) {
    if (t == null)
      return "null"; 
    String charset = getEncodingName();
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
      PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, charset));
      pw.println(t.getMessage());
      t.printStackTrace(pw);
      pw.flush();
      pw.close();
      return out.toString(charset);
    } catch (RuntimeException unexpected) {
      return t.toString() + ' ' + unexpected.toString();
    } catch (Exception badMimeCharset) {
      return t.toString() + ' ' + badMimeCharset.toString();
    } 
  }
  
  private Object getAndSetContextClassLoader(Object ccl) {
    if (ccl != GetAndSetContext.NOT_MODIFIED)
      try {
        PrivilegedAction<?> pa;
        if (ccl instanceof PrivilegedAction) {
          pa = (PrivilegedAction)ccl;
        } else {
          pa = new GetAndSetContext(ccl);
        } 
        return AccessController.doPrivileged(pa);
      } catch (SecurityException securityException) {} 
    return GetAndSetContext.NOT_MODIFIED;
  }
  
  private static RuntimeException attachmentMismatch(String msg) {
    return new IndexOutOfBoundsException(msg);
  }
  
  private static RuntimeException attachmentMismatch(int expected, int found) {
    return attachmentMismatch("Attachments mismatched, expected " + expected + " but given " + found + '.');
  }
  
  private static MessagingException attach(MessagingException required, Exception optional) {
    if (optional != null && !required.setNextException(optional) && 
      optional instanceof MessagingException) {
      MessagingException head = (MessagingException)optional;
      if (head.setNextException(required))
        return head; 
    } 
    return required;
  }
  
  private String getLocalHost(Service s) {
    try {
      return LogManagerProperties.getLocalHost(s);
    } catch (SecurityException securityException) {
    
    } catch (NoSuchMethodException noSuchMethodException) {
    
    } catch (LinkageError linkageError) {
    
    } catch (Exception ex) {
      reportError(s.toString(), ex, 4);
    } 
    return null;
  }
  
  private Session getSession(Message msg) {
    if (msg == null)
      throw new NullPointerException(); 
    return (new MessageContext(msg)).getSession();
  }
  
  private boolean allowRestrictedHeaders() {
    return LogManagerProperties.hasLogManager();
  }
  
  private static String atIndexMsg(int i) {
    return "At index: " + i + '.';
  }
  
  private static final class DefaultAuthenticator extends Authenticator {
    private final String pass;
    
    static Authenticator of(String pass) {
      return new DefaultAuthenticator(pass);
    }
    
    private DefaultAuthenticator(String pass) {
      assert pass != null;
      this.pass = pass;
    }
    
    protected final PasswordAuthentication getPasswordAuthentication() {
      return new PasswordAuthentication(getDefaultUserName(), this.pass);
    }
  }
  
  private static final class GetAndSetContext implements PrivilegedAction<Object> {
    public static final Object NOT_MODIFIED = GetAndSetContext.class;
    
    private final Object source;
    
    GetAndSetContext(Object source) {
      this.source = source;
    }
    
    public final Object run() {
      ClassLoader loader;
      Thread current = Thread.currentThread();
      ClassLoader ccl = current.getContextClassLoader();
      if (this.source == null) {
        loader = null;
      } else if (this.source instanceof ClassLoader) {
        loader = (ClassLoader)this.source;
      } else if (this.source instanceof Class) {
        loader = ((Class)this.source).getClassLoader();
      } else if (this.source instanceof Thread) {
        loader = ((Thread)this.source).getContextClassLoader();
      } else {
        assert !(this.source instanceof Class) : this.source;
        loader = this.source.getClass().getClassLoader();
      } 
      if (ccl != loader) {
        current.setContextClassLoader(loader);
        return ccl;
      } 
      return NOT_MODIFIED;
    }
  }
  
  private static final class TailNameFormatter extends Formatter {
    private final String name;
    
    static Formatter of(String name) {
      return new TailNameFormatter(name);
    }
    
    private TailNameFormatter(String name) {
      assert name != null;
      this.name = name;
    }
    
    public final String format(LogRecord record) {
      return "";
    }
    
    public final String getTail(Handler h) {
      return this.name;
    }
    
    public final boolean equals(Object o) {
      if (o instanceof TailNameFormatter)
        return this.name.equals(((TailNameFormatter)o).name); 
      return false;
    }
    
    public final int hashCode() {
      return getClass().hashCode() + this.name.hashCode();
    }
    
    public final String toString() {
      return this.name;
    }
  }
}
