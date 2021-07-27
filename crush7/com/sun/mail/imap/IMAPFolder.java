package com.sun.mail.imap;

import com.sun.mail.iap.BadCommandException;
import com.sun.mail.iap.CommandFailedException;
import com.sun.mail.iap.ConnectionException;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.iap.ResponseHandler;
import com.sun.mail.imap.protocol.FLAGS;
import com.sun.mail.imap.protocol.FetchItem;
import com.sun.mail.imap.protocol.FetchResponse;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.sun.mail.imap.protocol.Item;
import com.sun.mail.imap.protocol.ListInfo;
import com.sun.mail.imap.protocol.MODSEQ;
import com.sun.mail.imap.protocol.MailboxInfo;
import com.sun.mail.imap.protocol.MessageSet;
import com.sun.mail.imap.protocol.Status;
import com.sun.mail.imap.protocol.UID;
import com.sun.mail.imap.protocol.UIDSet;
import com.sun.mail.util.MailLogger;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.FolderNotFoundException;
import javax.mail.Message;
import javax.mail.MessageRemovedException;
import javax.mail.MessagingException;
import javax.mail.Quota;
import javax.mail.ReadOnlyFolderException;
import javax.mail.StoreClosedException;
import javax.mail.UIDFolder;
import javax.mail.event.MailEvent;
import javax.mail.event.MessageChangedEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;
import javax.mail.search.SearchException;
import javax.mail.search.SearchTerm;

public class IMAPFolder extends Folder implements UIDFolder, ResponseHandler {
  protected volatile String fullName;
  
  protected String name;
  
  protected int type;
  
  protected char separator;
  
  protected Flags availableFlags;
  
  protected Flags permanentFlags;
  
  protected volatile boolean exists;
  
  protected boolean isNamespace = false;
  
  protected volatile String[] attributes;
  
  protected volatile IMAPProtocol protocol;
  
  protected MessageCache messageCache;
  
  protected final Object messageCacheLock = new Object();
  
  protected Hashtable<Long, IMAPMessage> uidTable;
  
  protected static final char UNKNOWN_SEPARATOR = '￿';
  
  private volatile boolean opened = false;
  
  private boolean reallyClosed = true;
  
  private static final int RUNNING = 0;
  
  private static final int IDLE = 1;
  
  private static final int ABORTING = 2;
  
  private int idleState = 0;
  
  private IdleManager idleManager;
  
  private volatile int total = -1;
  
  private volatile int recent = -1;
  
  private int realTotal = -1;
  
  private long uidvalidity = -1L;
  
  private long uidnext = -1L;
  
  private volatile long highestmodseq = -1L;
  
  private boolean doExpungeNotification = true;
  
  private Status cachedStatus = null;
  
  private long cachedStatusTime = 0L;
  
  private boolean hasMessageCountListener = false;
  
  protected MailLogger logger;
  
  private MailLogger connectionPoolLogger;
  
  public static class FetchProfileItem extends FetchProfile.Item {
    protected FetchProfileItem(String name) {
      super(name);
    }
    
    public static final FetchProfileItem HEADERS = new FetchProfileItem("HEADERS");
    
    @Deprecated
    public static final FetchProfileItem SIZE = new FetchProfileItem("SIZE");
    
    public static final FetchProfileItem MESSAGE = new FetchProfileItem("MESSAGE");
    
    public static final FetchProfileItem INTERNALDATE = new FetchProfileItem("INTERNALDATE");
  }
  
  protected IMAPFolder(String fullName, char separator, IMAPStore store, Boolean isNamespace) {
    super(store);
    if (fullName == null)
      throw new NullPointerException("Folder name is null"); 
    this.fullName = fullName;
    this.separator = separator;
    this
      .logger = new MailLogger(getClass(), "DEBUG IMAP", store.getSession());
    this.connectionPoolLogger = store.getConnectionPoolLogger();
    this.isNamespace = false;
    if (separator != Character.MAX_VALUE && separator != '\000') {
      int i = this.fullName.indexOf(separator);
      if (i > 0 && i == this.fullName.length() - 1) {
        this.fullName = this.fullName.substring(0, i);
        this.isNamespace = true;
      } 
    } 
    if (isNamespace != null)
      this.isNamespace = isNamespace.booleanValue(); 
  }
  
  protected IMAPFolder(ListInfo li, IMAPStore store) {
    this(li.name, li.separator, store, (Boolean)null);
    if (li.hasInferiors)
      this.type |= 0x2; 
    if (li.canOpen)
      this.type |= 0x1; 
    this.exists = true;
    this.attributes = li.attrs;
  }
  
  protected void checkExists() throws MessagingException {
    if (!this.exists && !exists())
      throw new FolderNotFoundException(this, this.fullName + " not found"); 
  }
  
  protected void checkClosed() {
    if (this.opened)
      throw new IllegalStateException("This operation is not allowed on an open folder"); 
  }
  
  protected void checkOpened() throws FolderClosedException {
    assert Thread.holdsLock(this);
    if (!this.opened) {
      if (this.reallyClosed)
        throw new IllegalStateException("This operation is not allowed on a closed folder"); 
      throw new FolderClosedException(this, "Lost folder connection to server");
    } 
  }
  
  protected void checkRange(int msgno) throws MessagingException {
    if (msgno < 1)
      throw new IndexOutOfBoundsException("message number < 1"); 
    if (msgno <= this.total)
      return; 
    synchronized (this.messageCacheLock) {
      try {
        keepConnectionAlive(false);
      } catch (ConnectionException cex) {
        throw new FolderClosedException(this, cex.getMessage());
      } catch (ProtocolException pex) {
        throw new MessagingException(pex.getMessage(), pex);
      } 
    } 
    if (msgno > this.total)
      throw new IndexOutOfBoundsException(msgno + " > " + this.total); 
  }
  
  private void checkFlags(Flags flags) throws MessagingException {
    assert Thread.holdsLock(this);
    if (this.mode != 2)
      throw new IllegalStateException("Cannot change flags on READ_ONLY folder: " + this.fullName); 
  }
  
  public synchronized String getName() {
    if (this.name == null)
      try {
        this.name = this.fullName.substring(this.fullName
            .lastIndexOf(getSeparator()) + 1);
      } catch (MessagingException messagingException) {} 
    return this.name;
  }
  
  public String getFullName() {
    return this.fullName;
  }
  
  public synchronized Folder getParent() throws MessagingException {
    char c = getSeparator();
    int index;
    if ((index = this.fullName.lastIndexOf(c)) != -1)
      return ((IMAPStore)this.store).newIMAPFolder(this.fullName
          .substring(0, index), c); 
    return new DefaultFolder((IMAPStore)this.store);
  }
  
  public synchronized boolean exists() throws MessagingException {
    final String lname;
    ListInfo[] li = null;
    if (this.isNamespace && this.separator != '\000') {
      lname = this.fullName + this.separator;
    } else {
      lname = this.fullName;
    } 
    li = (ListInfo[])doCommand(new ProtocolCommand() {
          public Object doCommand(IMAPProtocol p) throws ProtocolException {
            return p.list("", lname);
          }
        });
    if (li != null) {
      int i = findName(li, lname);
      this.fullName = (li[i]).name;
      this.separator = (li[i]).separator;
      int len = this.fullName.length();
      if (this.separator != '\000' && len > 0 && this.fullName
        .charAt(len - 1) == this.separator)
        this.fullName = this.fullName.substring(0, len - 1); 
      this.type = 0;
      if ((li[i]).hasInferiors)
        this.type |= 0x2; 
      if ((li[i]).canOpen)
        this.type |= 0x1; 
      this.exists = true;
      this.attributes = (li[i]).attrs;
    } else {
      this.exists = this.opened;
      this.attributes = null;
    } 
    return this.exists;
  }
  
  private int findName(ListInfo[] li, String lname) {
    int i;
    for (i = 0; i < li.length && 
      !(li[i]).name.equals(lname); i++);
    if (i >= li.length)
      i = 0; 
    return i;
  }
  
  public Folder[] list(String pattern) throws MessagingException {
    return doList(pattern, false);
  }
  
  public Folder[] listSubscribed(String pattern) throws MessagingException {
    return doList(pattern, true);
  }
  
  private synchronized Folder[] doList(final String pattern, final boolean subscribed) throws MessagingException {
    checkExists();
    if (this.attributes != null && !isDirectory())
      return new Folder[0]; 
    final char c = getSeparator();
    ListInfo[] li = (ListInfo[])doCommandIgnoreFailure(new ProtocolCommand() {
          public Object doCommand(IMAPProtocol p) throws ProtocolException {
            if (subscribed)
              return p.lsub("", IMAPFolder.this.fullName + c + pattern); 
            return p.list("", IMAPFolder.this.fullName + c + pattern);
          }
        });
    if (li == null)
      return new Folder[0]; 
    int start = 0;
    if (li.length > 0 && (li[0]).name.equals(this.fullName + c))
      start = 1; 
    IMAPFolder[] folders = new IMAPFolder[li.length - start];
    IMAPStore st = (IMAPStore)this.store;
    for (int i = start; i < li.length; i++)
      folders[i - start] = st.newIMAPFolder(li[i]); 
    return (Folder[])folders;
  }
  
  public synchronized char getSeparator() throws MessagingException {
    if (this.separator == Character.MAX_VALUE) {
      ListInfo[] li = null;
      li = (ListInfo[])doCommand(new ProtocolCommand() {
            public Object doCommand(IMAPProtocol p) throws ProtocolException {
              if (p.isREV1())
                return p.list(IMAPFolder.this.fullName, ""); 
              return p.list("", IMAPFolder.this.fullName);
            }
          });
      if (li != null) {
        this.separator = (li[0]).separator;
      } else {
        this.separator = '/';
      } 
    } 
    return this.separator;
  }
  
  public synchronized int getType() throws MessagingException {
    if (this.opened) {
      if (this.attributes == null)
        exists(); 
    } else {
      checkExists();
    } 
    return this.type;
  }
  
  public synchronized boolean isSubscribed() {
    final String lname;
    ListInfo[] li = null;
    if (this.isNamespace && this.separator != '\000') {
      lname = this.fullName + this.separator;
    } else {
      lname = this.fullName;
    } 
    try {
      li = (ListInfo[])doProtocolCommand(new ProtocolCommand() {
            public Object doCommand(IMAPProtocol p) throws ProtocolException {
              return p.lsub("", lname);
            }
          });
    } catch (ProtocolException protocolException) {}
    if (li != null) {
      int i = findName(li, lname);
      return (li[i]).canOpen;
    } 
    return false;
  }
  
  public synchronized void setSubscribed(final boolean subscribe) throws MessagingException {
    doCommandIgnoreFailure(new ProtocolCommand() {
          public Object doCommand(IMAPProtocol p) throws ProtocolException {
            if (subscribe) {
              p.subscribe(IMAPFolder.this.fullName);
            } else {
              p.unsubscribe(IMAPFolder.this.fullName);
            } 
            return null;
          }
        });
  }
  
  public synchronized boolean create(final int type) throws MessagingException {
    char c = Character.MIN_VALUE;
    if ((type & 0x1) == 0)
      c = getSeparator(); 
    final char sep = c;
    Object ret = doCommandIgnoreFailure(new ProtocolCommand() {
          public Object doCommand(IMAPProtocol p) throws ProtocolException {
            if ((type & 0x1) == 0) {
              p.create(IMAPFolder.this.fullName + sep);
            } else {
              p.create(IMAPFolder.this.fullName);
              if ((type & 0x2) != 0) {
                ListInfo[] li = p.list("", IMAPFolder.this.fullName);
                if (li != null && !(li[0]).hasInferiors) {
                  p.delete(IMAPFolder.this.fullName);
                  throw new ProtocolException("Unsupported type");
                } 
              } 
            } 
            return Boolean.TRUE;
          }
        });
    if (ret == null)
      return false; 
    boolean retb = exists();
    if (retb)
      notifyFolderListeners(1); 
    return retb;
  }
  
  public synchronized boolean hasNewMessages() throws MessagingException {
    final String lname;
    synchronized (this.messageCacheLock) {
      if (this.opened) {
        try {
          keepConnectionAlive(true);
        } catch (ConnectionException cex) {
          throw new FolderClosedException(this, cex.getMessage());
        } catch (ProtocolException pex) {
          throw new MessagingException(pex.getMessage(), pex);
        } 
        return (this.recent > 0);
      } 
    } 
    ListInfo[] li = null;
    if (this.isNamespace && this.separator != '\000') {
      lname = this.fullName + this.separator;
    } else {
      lname = this.fullName;
    } 
    li = (ListInfo[])doCommandIgnoreFailure(new ProtocolCommand() {
          public Object doCommand(IMAPProtocol p) throws ProtocolException {
            return p.list("", lname);
          }
        });
    if (li == null)
      throw new FolderNotFoundException(this, this.fullName + " not found"); 
    int i = findName(li, lname);
    if ((li[i]).changeState == 1)
      return true; 
    if ((li[i]).changeState == 2)
      return false; 
    try {
      Status status = getStatus();
      if (status.recent > 0)
        return true; 
      return false;
    } catch (BadCommandException bex) {
      return false;
    } catch (ConnectionException cex) {
      throw new StoreClosedException(this.store, cex.getMessage());
    } catch (ProtocolException pex) {
      throw new MessagingException(pex.getMessage(), pex);
    } 
  }
  
  public synchronized Folder getFolder(String name) throws MessagingException {
    if (this.attributes != null && !isDirectory())
      throw new MessagingException("Cannot contain subfolders"); 
    char c = getSeparator();
    return ((IMAPStore)this.store).newIMAPFolder(this.fullName + c + name, c);
  }
  
  public synchronized boolean delete(boolean recurse) throws MessagingException {
    checkClosed();
    if (recurse) {
      Folder[] f = list();
      for (int i = 0; i < f.length; i++)
        f[i].delete(recurse); 
    } 
    Object ret = doCommandIgnoreFailure(new ProtocolCommand() {
          public Object doCommand(IMAPProtocol p) throws ProtocolException {
            p.delete(IMAPFolder.this.fullName);
            return Boolean.TRUE;
          }
        });
    if (ret == null)
      return false; 
    this.exists = false;
    this.attributes = null;
    notifyFolderListeners(2);
    return true;
  }
  
  public synchronized boolean renameTo(final Folder f) throws MessagingException {
    checkClosed();
    checkExists();
    if (f.getStore() != this.store)
      throw new MessagingException("Can't rename across Stores"); 
    Object ret = doCommandIgnoreFailure(new ProtocolCommand() {
          public Object doCommand(IMAPProtocol p) throws ProtocolException {
            p.rename(IMAPFolder.this.fullName, f.getFullName());
            return Boolean.TRUE;
          }
        });
    if (ret == null)
      return false; 
    this.exists = false;
    this.attributes = null;
    notifyFolderRenamedListeners(f);
    return true;
  }
  
  public synchronized void open(int mode) throws MessagingException {
    open(mode, (ResyncData)null);
  }
  
  public synchronized List<MailEvent> open(int mode, ResyncData rd) throws MessagingException {
    checkClosed();
    MailboxInfo mi = null;
    this.protocol = ((IMAPStore)this.store).getProtocol(this);
    List<MailEvent> openEvents = null;
    synchronized (this.messageCacheLock) {
      this.protocol.addResponseHandler(this);
      try {
        if (rd != null)
          if (rd == ResyncData.CONDSTORE) {
            if (!this.protocol.isEnabled("CONDSTORE") && 
              !this.protocol.isEnabled("QRESYNC"))
              if (this.protocol.hasCapability("CONDSTORE")) {
                this.protocol.enable("CONDSTORE");
              } else {
                this.protocol.enable("QRESYNC");
              }  
          } else if (!this.protocol.isEnabled("QRESYNC")) {
            this.protocol.enable("QRESYNC");
          }  
        if (mode == 1) {
          mi = this.protocol.examine(this.fullName, rd);
        } else {
          mi = this.protocol.select(this.fullName, rd);
        } 
      } catch (CommandFailedException cex) {
        try {
          checkExists();
          if ((this.type & 0x1) == 0)
            throw new MessagingException("folder cannot contain messages"); 
          throw new MessagingException(cex.getMessage(), cex);
        } finally {
          this.exists = false;
          this.attributes = null;
          this.type = 0;
          releaseProtocol(true);
        } 
      } catch (ProtocolException pex) {
        try {
          this.protocol.logout();
        } catch (ProtocolException protocolException) {
        
        } finally {
          releaseProtocol(false);
          throw new MessagingException(pex.getMessage(), pex);
        } 
      } 
      if (mi.mode != mode && (
        mode != 2 || mi.mode != 1 || 
        !((IMAPStore)this.store).allowReadOnlySelect()))
        try {
          this.protocol.close();
          releaseProtocol(true);
        } catch (ProtocolException pex) {
          try {
            this.protocol.logout();
          } catch (ProtocolException protocolException) {
          
          } finally {
            releaseProtocol(false);
          } 
        } finally {
          throw new ReadOnlyFolderException(this, "Cannot open in desired mode");
        }  
      this.opened = true;
      this.reallyClosed = false;
      this.mode = mi.mode;
      this.availableFlags = mi.availableFlags;
      this.permanentFlags = mi.permanentFlags;
      this.total = this.realTotal = mi.total;
      this.recent = mi.recent;
      this.uidvalidity = mi.uidvalidity;
      this.uidnext = mi.uidnext;
      this.highestmodseq = mi.highestmodseq;
      this.messageCache = new MessageCache(this, (IMAPStore)this.store, this.total);
      if (mi.responses != null) {
        openEvents = new ArrayList<MailEvent>();
        for (IMAPResponse ir : mi.responses) {
          if (ir.keyEquals("VANISHED")) {
            String[] s = ir.readAtomStringList();
            if (s == null || s.length != 1 || 
              !s[0].equalsIgnoreCase("EARLIER"))
              continue; 
            String uids = ir.readAtom();
            UIDSet[] uidset = UIDSet.parseUIDSets(uids);
            long[] luid = UIDSet.toArray(uidset, this.uidnext);
            if (luid != null && luid.length > 0)
              openEvents.add(new MessageVanishedEvent(this, luid)); 
            continue;
          } 
          if (ir.keyEquals("FETCH")) {
            assert ir instanceof FetchResponse : "!ir instanceof FetchResponse";
            Message msg = processFetchResponse((FetchResponse)ir);
            if (msg != null)
              openEvents.add(new MessageChangedEvent(this, 1, msg)); 
          } 
        } 
      } 
    } 
    this.exists = true;
    this.attributes = null;
    this.type = 1;
    notifyConnectionListeners(1);
    return openEvents;
  }
  
  public synchronized void fetch(Message[] msgs, FetchProfile fp) throws MessagingException {
    boolean isRev1;
    FetchItem[] fitems;
    synchronized (this.messageCacheLock) {
      checkOpened();
      isRev1 = this.protocol.isREV1();
      fitems = this.protocol.getFetchItems();
    } 
    StringBuffer command = new StringBuffer();
    boolean first = true;
    boolean allHeaders = false;
    if (fp.contains(FetchProfile.Item.ENVELOPE)) {
      command.append(getEnvelopeCommand());
      first = false;
    } 
    if (fp.contains(FetchProfile.Item.FLAGS)) {
      command.append(first ? "FLAGS" : " FLAGS");
      first = false;
    } 
    if (fp.contains(FetchProfile.Item.CONTENT_INFO)) {
      command.append(first ? "BODYSTRUCTURE" : " BODYSTRUCTURE");
      first = false;
    } 
    if (fp.contains(UIDFolder.FetchProfileItem.UID)) {
      command.append(first ? "UID" : " UID");
      first = false;
    } 
    if (fp.contains(FetchProfileItem.HEADERS)) {
      allHeaders = true;
      if (isRev1) {
        command.append(first ? "BODY.PEEK[HEADER]" : " BODY.PEEK[HEADER]");
      } else {
        command.append(first ? "RFC822.HEADER" : " RFC822.HEADER");
      } 
      first = false;
    } 
    if (fp.contains(FetchProfileItem.MESSAGE)) {
      allHeaders = true;
      if (isRev1) {
        command.append(first ? "BODY.PEEK[]" : " BODY.PEEK[]");
      } else {
        command.append(first ? "RFC822" : " RFC822");
      } 
      first = false;
    } 
    if (fp.contains(FetchProfile.Item.SIZE) || fp
      .contains(FetchProfileItem.SIZE)) {
      command.append(first ? "RFC822.SIZE" : " RFC822.SIZE");
      first = false;
    } 
    if (fp.contains(FetchProfileItem.INTERNALDATE)) {
      command.append(first ? "INTERNALDATE" : " INTERNALDATE");
      first = false;
    } 
    String[] hdrs = null;
    if (!allHeaders) {
      hdrs = fp.getHeaderNames();
      if (hdrs.length > 0) {
        if (!first)
          command.append(" "); 
        command.append(createHeaderCommand(hdrs, isRev1));
      } 
    } 
    for (int i = 0; i < fitems.length; i++) {
      if (fp.contains(fitems[i].getFetchProfileItem())) {
        if (command.length() != 0)
          command.append(" "); 
        command.append(fitems[i].getName());
      } 
    } 
    Utility.Condition condition = new IMAPMessage.FetchProfileCondition(fp, fitems);
    synchronized (this.messageCacheLock) {
      checkOpened();
      MessageSet[] msgsets = Utility.toMessageSetSorted(msgs, condition);
      if (msgsets == null)
        return; 
      Response[] r = null;
      List<Response> v = new ArrayList<Response>();
      try {
        r = getProtocol().fetch(msgsets, command.toString());
      } catch (ConnectionException cex) {
        throw new FolderClosedException(this, cex.getMessage());
      } catch (CommandFailedException commandFailedException) {
      
      } catch (ProtocolException pex) {
        throw new MessagingException(pex.getMessage(), pex);
      } 
      if (r == null)
        return; 
      for (int j = 0; j < r.length; j++) {
        if (r[j] != null)
          if (!(r[j] instanceof FetchResponse)) {
            v.add(r[j]);
          } else {
            FetchResponse f = (FetchResponse)r[j];
            IMAPMessage msg = getMessageBySeqNumber(f.getNumber());
            int count = f.getItemCount();
            boolean unsolicitedFlags = false;
            for (int k = 0; k < count; k++) {
              Item item = f.getItem(k);
              if (item instanceof Flags && (
                !fp.contains(FetchProfile.Item.FLAGS) || msg == null)) {
                unsolicitedFlags = true;
              } else if (msg != null) {
                msg.handleFetchItem(item, hdrs, allHeaders);
              } 
            } 
            if (msg != null)
              msg.handleExtensionFetchItems(f.getExtensionItems()); 
            if (unsolicitedFlags)
              v.add(f); 
          }  
      } 
      if (!v.isEmpty()) {
        Response[] responses = new Response[v.size()];
        v.toArray(responses);
        handleResponses(responses);
      } 
    } 
  }
  
  protected String getEnvelopeCommand() {
    return "ENVELOPE INTERNALDATE RFC822.SIZE";
  }
  
  protected IMAPMessage newIMAPMessage(int msgnum) {
    return new IMAPMessage(this, msgnum);
  }
  
  private String createHeaderCommand(String[] hdrs, boolean isRev1) {
    StringBuffer sb;
    if (isRev1) {
      sb = new StringBuffer("BODY.PEEK[HEADER.FIELDS (");
    } else {
      sb = new StringBuffer("RFC822.HEADER.LINES (");
    } 
    for (int i = 0; i < hdrs.length; i++) {
      if (i > 0)
        sb.append(" "); 
      sb.append(hdrs[i]);
    } 
    if (isRev1) {
      sb.append(")]");
    } else {
      sb.append(")");
    } 
    return sb.toString();
  }
  
  public synchronized void setFlags(Message[] msgs, Flags flag, boolean value) throws MessagingException {
    checkOpened();
    checkFlags(flag);
    if (msgs.length == 0)
      return; 
    synchronized (this.messageCacheLock) {
      try {
        IMAPProtocol p = getProtocol();
        MessageSet[] ms = Utility.toMessageSetSorted(msgs, null);
        if (ms == null)
          throw new MessageRemovedException("Messages have been removed"); 
        p.storeFlags(ms, flag, value);
      } catch (ConnectionException cex) {
        throw new FolderClosedException(this, cex.getMessage());
      } catch (ProtocolException pex) {
        throw new MessagingException(pex.getMessage(), pex);
      } 
    } 
  }
  
  public synchronized void setFlags(int start, int end, Flags flag, boolean value) throws MessagingException {
    checkOpened();
    Message[] msgs = new Message[end - start + 1];
    int i = 0;
    for (int n = start; n <= end; n++)
      msgs[i++] = getMessage(n); 
    setFlags(msgs, flag, value);
  }
  
  public synchronized void setFlags(int[] msgnums, Flags flag, boolean value) throws MessagingException {
    checkOpened();
    Message[] msgs = new Message[msgnums.length];
    for (int i = 0; i < msgnums.length; i++)
      msgs[i] = getMessage(msgnums[i]); 
    setFlags(msgs, flag, value);
  }
  
  public synchronized void close(boolean expunge) throws MessagingException {
    close(expunge, false);
  }
  
  public synchronized void forceClose() throws MessagingException {
    close(false, true);
  }
  
  private void close(boolean expunge, boolean force) throws MessagingException {
    assert Thread.holdsLock(this);
    synchronized (this.messageCacheLock) {
      if (!this.opened && this.reallyClosed)
        throw new IllegalStateException("This operation is not allowed on a closed folder"); 
      this.reallyClosed = true;
      if (!this.opened)
        return; 
      boolean reuseProtocol = true;
      try {
        waitIfIdle();
        if (force) {
          this.logger.log(Level.FINE, "forcing folder {0} to close", this.fullName);
          if (this.protocol != null)
            this.protocol.disconnect(); 
        } else if (((IMAPStore)this.store).isConnectionPoolFull()) {
          this.logger.fine("pool is full, not adding an Authenticated connection");
          if (expunge && this.protocol != null)
            this.protocol.close(); 
          if (this.protocol != null)
            this.protocol.logout(); 
        } else if (!expunge && this.mode == 2) {
          try {
            if (this.protocol != null && this.protocol
              .hasCapability("UNSELECT")) {
              this.protocol.unselect();
            } else if (this.protocol != null) {
              boolean selected = true;
              try {
                this.protocol.examine(this.fullName);
              } catch (CommandFailedException ex) {
                selected = false;
              } 
              if (selected && this.protocol != null)
                this.protocol.close(); 
            } 
          } catch (ProtocolException pex2) {
            reuseProtocol = false;
          } 
        } else if (this.protocol != null) {
          this.protocol.close();
        } 
      } catch (ProtocolException pex) {
        throw new MessagingException(pex.getMessage(), pex);
      } finally {
        if (this.opened)
          cleanup(reuseProtocol); 
      } 
    } 
  }
  
  private void cleanup(boolean returnToPool) {
    assert Thread.holdsLock(this.messageCacheLock);
    releaseProtocol(returnToPool);
    this.messageCache = null;
    this.uidTable = null;
    this.exists = false;
    this.attributes = null;
    this.opened = false;
    this.idleState = 0;
    this.messageCacheLock.notifyAll();
    notifyConnectionListeners(3);
  }
  
  public synchronized boolean isOpen() {
    synchronized (this.messageCacheLock) {
      if (this.opened)
        try {
          keepConnectionAlive(false);
        } catch (ProtocolException protocolException) {} 
    } 
    return this.opened;
  }
  
  public synchronized Flags getPermanentFlags() {
    if (this.permanentFlags == null)
      return null; 
    return (Flags)this.permanentFlags.clone();
  }
  
  public synchronized int getMessageCount() throws MessagingException {
    synchronized (this.messageCacheLock) {
      if (this.opened)
        try {
          keepConnectionAlive(true);
          return this.total;
        } catch (ConnectionException cex) {
          throw new FolderClosedException(this, cex.getMessage());
        } catch (ProtocolException pex) {
          throw new MessagingException(pex.getMessage(), pex);
        }  
    } 
    checkExists();
    try {
      Status status = getStatus();
      return status.total;
    } catch (BadCommandException bex) {
      IMAPProtocol p = null;
      try {
        p = getStoreProtocol();
        MailboxInfo minfo = p.examine(this.fullName);
        p.close();
        return minfo.total;
      } catch (ProtocolException pex) {
        throw new MessagingException(pex.getMessage(), pex);
      } finally {
        releaseStoreProtocol(p);
      } 
    } catch (ConnectionException cex) {
      throw new StoreClosedException(this.store, cex.getMessage());
    } catch (ProtocolException pex) {
      throw new MessagingException(pex.getMessage(), pex);
    } 
  }
  
  public synchronized int getNewMessageCount() throws MessagingException {
    synchronized (this.messageCacheLock) {
      if (this.opened)
        try {
          keepConnectionAlive(true);
          return this.recent;
        } catch (ConnectionException cex) {
          throw new FolderClosedException(this, cex.getMessage());
        } catch (ProtocolException pex) {
          throw new MessagingException(pex.getMessage(), pex);
        }  
    } 
    checkExists();
    try {
      Status status = getStatus();
      return status.recent;
    } catch (BadCommandException bex) {
      IMAPProtocol p = null;
      try {
        p = getStoreProtocol();
        MailboxInfo minfo = p.examine(this.fullName);
        p.close();
        return minfo.recent;
      } catch (ProtocolException pex) {
        throw new MessagingException(pex.getMessage(), pex);
      } finally {
        releaseStoreProtocol(p);
      } 
    } catch (ConnectionException cex) {
      throw new StoreClosedException(this.store, cex.getMessage());
    } catch (ProtocolException pex) {
      throw new MessagingException(pex.getMessage(), pex);
    } 
  }
  
  public synchronized int getUnreadMessageCount() throws MessagingException {
    if (!this.opened) {
      checkExists();
      try {
        Status status = getStatus();
        return status.unseen;
      } catch (BadCommandException bex) {
        return -1;
      } catch (ConnectionException cex) {
        throw new StoreClosedException(this.store, cex.getMessage());
      } catch (ProtocolException pex) {
        throw new MessagingException(pex.getMessage(), pex);
      } 
    } 
    Flags f = new Flags();
    f.add(Flags.Flag.SEEN);
    try {
      synchronized (this.messageCacheLock) {
        int[] matches = getProtocol().search(new FlagTerm(f, false));
        return matches.length;
      } 
    } catch (ConnectionException cex) {
      throw new FolderClosedException(this, cex.getMessage());
    } catch (ProtocolException pex) {
      throw new MessagingException(pex.getMessage(), pex);
    } 
  }
  
  public synchronized int getDeletedMessageCount() throws MessagingException {
    if (!this.opened) {
      checkExists();
      return -1;
    } 
    Flags f = new Flags();
    f.add(Flags.Flag.DELETED);
    try {
      synchronized (this.messageCacheLock) {
        int[] matches = getProtocol().search(new FlagTerm(f, true));
        return matches.length;
      } 
    } catch (ConnectionException cex) {
      throw new FolderClosedException(this, cex.getMessage());
    } catch (ProtocolException pex) {
      throw new MessagingException(pex.getMessage(), pex);
    } 
  }
  
  private Status getStatus() throws ProtocolException {
    int statusCacheTimeout = ((IMAPStore)this.store).getStatusCacheTimeout();
    if (statusCacheTimeout > 0 && this.cachedStatus != null && 
      System.currentTimeMillis() - this.cachedStatusTime < statusCacheTimeout)
      return this.cachedStatus; 
    IMAPProtocol p = null;
    try {
      p = getStoreProtocol();
      Status s = p.status(this.fullName, (String[])null);
      if (statusCacheTimeout > 0) {
        this.cachedStatus = s;
        this.cachedStatusTime = System.currentTimeMillis();
      } 
      return s;
    } finally {
      releaseStoreProtocol(p);
    } 
  }
  
  public synchronized Message getMessage(int msgnum) throws MessagingException {
    checkOpened();
    checkRange(msgnum);
    return this.messageCache.getMessage(msgnum);
  }
  
  public synchronized Message[] getMessages() throws MessagingException {
    checkOpened();
    int total = getMessageCount();
    Message[] msgs = new Message[total];
    for (int i = 1; i <= total; i++)
      msgs[i - 1] = this.messageCache.getMessage(i); 
    return msgs;
  }
  
  public synchronized void appendMessages(Message[] msgs) throws MessagingException {
    checkExists();
    int maxsize = ((IMAPStore)this.store).getAppendBufferSize();
    for (int i = 0; i < msgs.length; i++) {
      final MessageLiteral mos;
      Message m = msgs[i];
      Date d = m.getReceivedDate();
      if (d == null)
        d = m.getSentDate(); 
      final Date dd = d;
      final Flags f = m.getFlags();
      try {
        mos = new MessageLiteral(m, (m.getSize() > maxsize) ? 0 : maxsize);
      } catch (IOException ex) {
        throw new MessagingException("IOException while appending messages", ex);
      } catch (MessageRemovedException mrex) {}
      doCommand(new ProtocolCommand() {
            public Object doCommand(IMAPProtocol p) throws ProtocolException {
              p.append(IMAPFolder.this.fullName, f, dd, mos);
              return null;
            }
          });
    } 
  }
  
  public synchronized AppendUID[] appendUIDMessages(Message[] msgs) throws MessagingException {
    checkExists();
    int maxsize = ((IMAPStore)this.store).getAppendBufferSize();
    AppendUID[] uids = new AppendUID[msgs.length];
    for (int i = 0; i < msgs.length; i++) {
      final MessageLiteral mos;
      Message m = msgs[i];
      try {
        mos = new MessageLiteral(m, (m.getSize() > maxsize) ? 0 : maxsize);
      } catch (IOException ex) {
        throw new MessagingException("IOException while appending messages", ex);
      } catch (MessageRemovedException mrex) {}
      Date d = m.getReceivedDate();
      if (d == null)
        d = m.getSentDate(); 
      final Date dd = d;
      final Flags f = m.getFlags();
      AppendUID auid = (AppendUID)doCommand(new ProtocolCommand() {
            public Object doCommand(IMAPProtocol p) throws ProtocolException {
              return p.appenduid(IMAPFolder.this.fullName, f, dd, mos);
            }
          });
      uids[i] = auid;
    } 
    return uids;
  }
  
  public synchronized Message[] addMessages(Message[] msgs) throws MessagingException {
    checkOpened();
    MimeMessage[] arrayOfMimeMessage = new MimeMessage[msgs.length];
    AppendUID[] uids = appendUIDMessages(msgs);
    for (int i = 0; i < uids.length; i++) {
      AppendUID auid = uids[i];
      if (auid != null && 
        auid.uidvalidity == this.uidvalidity)
        try {
          arrayOfMimeMessage[i] = (MimeMessage)getMessageByUID(auid.uid);
        } catch (MessagingException messagingException) {} 
    } 
    return (Message[])arrayOfMimeMessage;
  }
  
  public synchronized void copyMessages(Message[] msgs, Folder folder) throws MessagingException {
    copymoveMessages(msgs, folder, false);
  }
  
  public synchronized AppendUID[] copyUIDMessages(Message[] msgs, Folder folder) throws MessagingException {
    return copymoveUIDMessages(msgs, folder, false);
  }
  
  public synchronized void moveMessages(Message[] msgs, Folder folder) throws MessagingException {
    copymoveMessages(msgs, folder, true);
  }
  
  public synchronized AppendUID[] moveUIDMessages(Message[] msgs, Folder folder) throws MessagingException {
    return copymoveUIDMessages(msgs, folder, true);
  }
  
  private synchronized void copymoveMessages(Message[] msgs, Folder folder, boolean move) throws MessagingException {
    checkOpened();
    if (msgs.length == 0)
      return; 
    if (folder.getStore() == this.store) {
      synchronized (this.messageCacheLock) {
        try {
          IMAPProtocol p = getProtocol();
          MessageSet[] ms = Utility.toMessageSet(msgs, null);
          if (ms == null)
            throw new MessageRemovedException("Messages have been removed"); 
          if (move) {
            p.move(ms, folder.getFullName());
          } else {
            p.copy(ms, folder.getFullName());
          } 
        } catch (CommandFailedException cfx) {
          if (cfx.getMessage().indexOf("TRYCREATE") != -1)
            throw new FolderNotFoundException(folder, folder
                
                .getFullName() + " does not exist"); 
          throw new MessagingException(cfx.getMessage(), cfx);
        } catch (ConnectionException cex) {
          throw new FolderClosedException(this, cex.getMessage());
        } catch (ProtocolException pex) {
          throw new MessagingException(pex.getMessage(), pex);
        } 
      } 
    } else {
      if (move)
        throw new MessagingException("Move between stores not supported"); 
      super.copyMessages(msgs, folder);
    } 
  }
  
  private synchronized AppendUID[] copymoveUIDMessages(Message[] msgs, Folder folder, boolean move) throws MessagingException {
    checkOpened();
    if (msgs.length == 0)
      return null; 
    if (folder.getStore() != this.store)
      throw new MessagingException(move ? "can't moveUIDMessages to a different store" : "can't copyUIDMessages to a different store"); 
    FetchProfile fp = new FetchProfile();
    fp.add(UIDFolder.FetchProfileItem.UID);
    fetch(msgs, fp);
    synchronized (this.messageCacheLock) {
      CopyUID cuid;
      IMAPProtocol p = getProtocol();
      MessageSet[] ms = Utility.toMessageSet(msgs, null);
      if (ms == null)
        throw new MessageRemovedException("Messages have been removed"); 
      if (move) {
        cuid = p.moveuid(ms, folder.getFullName());
      } else {
        cuid = p.copyuid(ms, folder.getFullName());
      } 
      long[] srcuids = UIDSet.toArray(cuid.src);
      long[] dstuids = UIDSet.toArray(cuid.dst);
      Message[] srcmsgs = getMessagesByUID(srcuids);
      AppendUID[] result = new AppendUID[msgs.length];
      for (int i = 0; i < msgs.length; i++) {
        int j = i;
        do {
          if (msgs[i] == srcmsgs[j]) {
            result[i] = new AppendUID(cuid.uidvalidity, dstuids[j]);
            break;
          } 
          j++;
          if (j < srcmsgs.length)
            continue; 
          j = 0;
        } while (j != i);
      } 
      return result;
    } 
  }
  
  public synchronized Message[] expunge() throws MessagingException {
    return expunge((Message[])null);
  }
  
  public synchronized Message[] expunge(Message[] msgs) throws MessagingException {
    IMAPMessage[] rmsgs;
    checkOpened();
    if (msgs != null) {
      FetchProfile fp = new FetchProfile();
      fp.add(UIDFolder.FetchProfileItem.UID);
      fetch(msgs, fp);
    } 
    synchronized (this.messageCacheLock) {
      this.doExpungeNotification = false;
      try {
        IMAPProtocol p = getProtocol();
        if (msgs != null) {
          p.uidexpunge(Utility.toUIDSet(msgs));
        } else {
          p.expunge();
        } 
      } catch (CommandFailedException cfx) {
        if (this.mode != 2)
          throw new IllegalStateException("Cannot expunge READ_ONLY folder: " + this.fullName); 
        throw new MessagingException(cfx.getMessage(), cfx);
      } catch (ConnectionException cex) {
        throw new FolderClosedException(this, cex.getMessage());
      } catch (ProtocolException pex) {
        throw new MessagingException(pex.getMessage(), pex);
      } finally {
        this.doExpungeNotification = true;
      } 
      if (msgs != null) {
        rmsgs = this.messageCache.removeExpungedMessages(msgs);
      } else {
        rmsgs = this.messageCache.removeExpungedMessages();
      } 
      if (this.uidTable != null)
        for (int i = 0; i < rmsgs.length; i++) {
          IMAPMessage m = rmsgs[i];
          long uid = m.getUID();
          if (uid != -1L)
            this.uidTable.remove(Long.valueOf(uid)); 
        }  
      this.total = this.messageCache.size();
    } 
    if (rmsgs.length > 0)
      notifyMessageRemovedListeners(true, (Message[])rmsgs); 
    return (Message[])rmsgs;
  }
  
  public synchronized Message[] search(SearchTerm term) throws MessagingException {
    checkOpened();
    try {
      IMAPMessage[] arrayOfIMAPMessage;
      Message[] matchMsgs = null;
      synchronized (this.messageCacheLock) {
        int[] matches = getProtocol().search(term);
        if (matches != null)
          arrayOfIMAPMessage = getMessagesBySeqNumbers(matches); 
      } 
      return (Message[])arrayOfIMAPMessage;
    } catch (CommandFailedException cfx) {
      return super.search(term);
    } catch (SearchException sex) {
      if (((IMAPStore)this.store).throwSearchException())
        throw sex; 
      return super.search(term);
    } catch (ConnectionException cex) {
      throw new FolderClosedException(this, cex.getMessage());
    } catch (ProtocolException pex) {
      throw new MessagingException(pex.getMessage(), pex);
    } 
  }
  
  public synchronized Message[] search(SearchTerm term, Message[] msgs) throws MessagingException {
    checkOpened();
    if (msgs.length == 0)
      return msgs; 
    try {
      IMAPMessage[] arrayOfIMAPMessage;
      Message[] matchMsgs = null;
      synchronized (this.messageCacheLock) {
        IMAPProtocol p = getProtocol();
        MessageSet[] ms = Utility.toMessageSetSorted(msgs, null);
        if (ms == null)
          throw new MessageRemovedException("Messages have been removed"); 
        int[] matches = p.search(ms, term);
        if (matches != null)
          arrayOfIMAPMessage = getMessagesBySeqNumbers(matches); 
      } 
      return (Message[])arrayOfIMAPMessage;
    } catch (CommandFailedException cfx) {
      return super.search(term, msgs);
    } catch (SearchException sex) {
      return super.search(term, msgs);
    } catch (ConnectionException cex) {
      throw new FolderClosedException(this, cex.getMessage());
    } catch (ProtocolException pex) {
      throw new MessagingException(pex.getMessage(), pex);
    } 
  }
  
  public synchronized Message[] getSortedMessages(SortTerm[] term) throws MessagingException {
    return getSortedMessages(term, (SearchTerm)null);
  }
  
  public synchronized Message[] getSortedMessages(SortTerm[] term, SearchTerm sterm) throws MessagingException {
    checkOpened();
    try {
      IMAPMessage[] arrayOfIMAPMessage;
      Message[] matchMsgs = null;
      synchronized (this.messageCacheLock) {
        int[] matches = getProtocol().sort(term, sterm);
        if (matches != null)
          arrayOfIMAPMessage = getMessagesBySeqNumbers(matches); 
      } 
      return (Message[])arrayOfIMAPMessage;
    } catch (CommandFailedException cfx) {
      throw new MessagingException(cfx.getMessage(), cfx);
    } catch (SearchException sex) {
      throw new MessagingException(sex.getMessage(), sex);
    } catch (ConnectionException cex) {
      throw new FolderClosedException(this, cex.getMessage());
    } catch (ProtocolException pex) {
      throw new MessagingException(pex.getMessage(), pex);
    } 
  }
  
  public synchronized void addMessageCountListener(MessageCountListener l) {
    super.addMessageCountListener(l);
    this.hasMessageCountListener = true;
  }
  
  public synchronized long getUIDValidity() throws MessagingException {
    if (this.opened)
      return this.uidvalidity; 
    IMAPProtocol p = null;
    Status status = null;
    try {
      p = getStoreProtocol();
      String[] item = { "UIDVALIDITY" };
      status = p.status(this.fullName, item);
    } catch (BadCommandException bex) {
      throw new MessagingException("Cannot obtain UIDValidity", bex);
    } catch (ConnectionException cex) {
      throwClosedException(cex);
    } catch (ProtocolException pex) {
      throw new MessagingException(pex.getMessage(), pex);
    } finally {
      releaseStoreProtocol(p);
    } 
    return status.uidvalidity;
  }
  
  public synchronized long getUIDNext() throws MessagingException {
    if (this.opened)
      return this.uidnext; 
    IMAPProtocol p = null;
    Status status = null;
    try {
      p = getStoreProtocol();
      String[] item = { "UIDNEXT" };
      status = p.status(this.fullName, item);
    } catch (BadCommandException bex) {
      throw new MessagingException("Cannot obtain UIDNext", bex);
    } catch (ConnectionException cex) {
      throwClosedException(cex);
    } catch (ProtocolException pex) {
      throw new MessagingException(pex.getMessage(), pex);
    } finally {
      releaseStoreProtocol(p);
    } 
    return status.uidnext;
  }
  
  public synchronized Message getMessageByUID(long uid) throws MessagingException {
    checkOpened();
    IMAPMessage m = null;
    try {
      synchronized (this.messageCacheLock) {
        Long l = Long.valueOf(uid);
        if (this.uidTable != null) {
          m = this.uidTable.get(l);
          if (m != null)
            return m; 
        } else {
          this.uidTable = new Hashtable<Long, IMAPMessage>();
        } 
        getProtocol().fetchSequenceNumber(uid);
        if (this.uidTable != null) {
          m = this.uidTable.get(l);
          if (m != null)
            return m; 
        } 
      } 
    } catch (ConnectionException cex) {
      throw new FolderClosedException(this, cex.getMessage());
    } catch (ProtocolException pex) {
      throw new MessagingException(pex.getMessage(), pex);
    } 
    return m;
  }
  
  public synchronized Message[] getMessagesByUID(long start, long end) throws MessagingException {
    Message[] msgs;
    checkOpened();
    try {
      synchronized (this.messageCacheLock) {
        if (this.uidTable == null)
          this.uidTable = new Hashtable<Long, IMAPMessage>(); 
        long[] ua = getProtocol().fetchSequenceNumbers(start, end);
        List<Message> ma = new ArrayList<Message>();
        for (int i = 0; i < ua.length; i++) {
          Message m = this.uidTable.get(Long.valueOf(ua[i]));
          if (m != null)
            ma.add(m); 
        } 
        msgs = ma.<Message>toArray(new Message[ma.size()]);
      } 
    } catch (ConnectionException cex) {
      throw new FolderClosedException(this, cex.getMessage());
    } catch (ProtocolException pex) {
      throw new MessagingException(pex.getMessage(), pex);
    } 
    return msgs;
  }
  
  public synchronized Message[] getMessagesByUID(long[] uids) throws MessagingException {
    checkOpened();
    try {
      synchronized (this.messageCacheLock) {
        long[] unavailUids = uids;
        if (this.uidTable != null) {
          List<Long> v = new ArrayList<Long>();
          for (long uid : uids) {
            if (!this.uidTable.containsKey(Long.valueOf(uid)))
              v.add(Long.valueOf(uid)); 
          } 
          int vsize = v.size();
          unavailUids = new long[vsize];
          for (int j = 0; j < vsize; j++)
            unavailUids[j] = ((Long)v.get(j)).longValue(); 
        } else {
          this.uidTable = new Hashtable<Long, IMAPMessage>();
        } 
        if (unavailUids.length > 0)
          getProtocol().fetchSequenceNumbers(unavailUids); 
        Message[] msgs = new Message[uids.length];
        for (int i = 0; i < uids.length; i++)
          msgs[i] = this.uidTable.get(Long.valueOf(uids[i])); 
        return msgs;
      } 
    } catch (ConnectionException cex) {
      throw new FolderClosedException(this, cex.getMessage());
    } catch (ProtocolException pex) {
      throw new MessagingException(pex.getMessage(), pex);
    } 
  }
  
  public synchronized long getUID(Message message) throws MessagingException {
    if (message.getFolder() != this)
      throw new NoSuchElementException("Message does not belong to this folder"); 
    checkOpened();
    if (!(message instanceof IMAPMessage))
      throw new MessagingException("message is not an IMAPMessage"); 
    IMAPMessage m = (IMAPMessage)message;
    long uid;
    if ((uid = m.getUID()) != -1L)
      return uid; 
    synchronized (this.messageCacheLock) {
      try {
        IMAPProtocol p = getProtocol();
        m.checkExpunged();
        UID u = p.fetchUID(m.getSequenceNumber());
        if (u != null) {
          uid = u.uid;
          m.setUID(uid);
          if (this.uidTable == null)
            this.uidTable = new Hashtable<Long, IMAPMessage>(); 
          this.uidTable.put(Long.valueOf(uid), m);
        } 
      } catch (ConnectionException cex) {
        throw new FolderClosedException(this, cex.getMessage());
      } catch (ProtocolException pex) {
        throw new MessagingException(pex.getMessage(), pex);
      } 
    } 
    return uid;
  }
  
  private Message[] createMessagesForUIDs(long[] uids) {
    IMAPMessage[] msgs = new IMAPMessage[uids.length];
    for (int i = 0; i < uids.length; i++) {
      IMAPMessage m = null;
      if (this.uidTable != null)
        m = this.uidTable.get(Long.valueOf(uids[i])); 
      if (m == null) {
        m = newIMAPMessage(-1);
        m.setUID(uids[i]);
        m.setExpunged(true);
      } 
      msgs[i++] = m;
    } 
    return (Message[])msgs;
  }
  
  public synchronized long getHighestModSeq() throws MessagingException {
    if (this.opened)
      return this.highestmodseq; 
    IMAPProtocol p = null;
    Status status = null;
    try {
      p = getStoreProtocol();
      if (!p.hasCapability("CONDSTORE"))
        throw new BadCommandException("CONDSTORE not supported"); 
      String[] item = { "HIGHESTMODSEQ" };
      status = p.status(this.fullName, item);
    } catch (BadCommandException bex) {
      throw new MessagingException("Cannot obtain HIGHESTMODSEQ", bex);
    } catch (ConnectionException cex) {
      throwClosedException(cex);
    } catch (ProtocolException pex) {
      throw new MessagingException(pex.getMessage(), pex);
    } finally {
      releaseStoreProtocol(p);
    } 
    return status.highestmodseq;
  }
  
  public synchronized Message[] getMessagesByUIDChangedSince(long start, long end, long modseq) throws MessagingException {
    checkOpened();
    try {
      synchronized (this.messageCacheLock) {
        IMAPProtocol p = getProtocol();
        if (!p.hasCapability("CONDSTORE"))
          throw new BadCommandException("CONDSTORE not supported"); 
        int[] nums = p.uidfetchChangedSince(start, end, modseq);
        return (Message[])getMessagesBySeqNumbers(nums);
      } 
    } catch (ConnectionException cex) {
      throw new FolderClosedException(this, cex.getMessage());
    } catch (ProtocolException pex) {
      throw new MessagingException(pex.getMessage(), pex);
    } 
  }
  
  public Quota[] getQuota() throws MessagingException {
    return (Quota[])doOptionalCommand("QUOTA not supported", new ProtocolCommand() {
          public Object doCommand(IMAPProtocol p) throws ProtocolException {
            return p.getQuotaRoot(IMAPFolder.this.fullName);
          }
        });
  }
  
  public void setQuota(final Quota quota) throws MessagingException {
    doOptionalCommand("QUOTA not supported", new ProtocolCommand() {
          public Object doCommand(IMAPProtocol p) throws ProtocolException {
            p.setQuota(quota);
            return null;
          }
        });
  }
  
  public ACL[] getACL() throws MessagingException {
    return (ACL[])doOptionalCommand("ACL not supported", new ProtocolCommand() {
          public Object doCommand(IMAPProtocol p) throws ProtocolException {
            return p.getACL(IMAPFolder.this.fullName);
          }
        });
  }
  
  public void addACL(ACL acl) throws MessagingException {
    setACL(acl, false);
  }
  
  public void removeACL(final String name) throws MessagingException {
    doOptionalCommand("ACL not supported", new ProtocolCommand() {
          public Object doCommand(IMAPProtocol p) throws ProtocolException {
            p.deleteACL(IMAPFolder.this.fullName, name);
            return null;
          }
        });
  }
  
  public void addRights(ACL acl) throws MessagingException {
    setACL(acl, '+');
  }
  
  public void removeRights(ACL acl) throws MessagingException {
    setACL(acl, '-');
  }
  
  public Rights[] listRights(final String name) throws MessagingException {
    return (Rights[])doOptionalCommand("ACL not supported", new ProtocolCommand() {
          public Object doCommand(IMAPProtocol p) throws ProtocolException {
            return p.listRights(IMAPFolder.this.fullName, name);
          }
        });
  }
  
  public Rights myRights() throws MessagingException {
    return (Rights)doOptionalCommand("ACL not supported", new ProtocolCommand() {
          public Object doCommand(IMAPProtocol p) throws ProtocolException {
            return p.myRights(IMAPFolder.this.fullName);
          }
        });
  }
  
  private void setACL(final ACL acl, final char mod) throws MessagingException {
    doOptionalCommand("ACL not supported", new ProtocolCommand() {
          public Object doCommand(IMAPProtocol p) throws ProtocolException {
            p.setACL(IMAPFolder.this.fullName, mod, acl);
            return null;
          }
        });
  }
  
  public synchronized String[] getAttributes() throws MessagingException {
    checkExists();
    if (this.attributes == null)
      exists(); 
    return (this.attributes == null) ? new String[0] : (String[])this.attributes.clone();
  }
  
  public void idle() throws MessagingException {
    idle(false);
  }
  
  public void idle(boolean once) throws MessagingException {
    synchronized (this) {
      if (this.protocol != null && this.protocol.getChannel() != null)
        throw new MessagingException("idle method not supported with SocketChannels"); 
    } 
    if (!startIdle((IdleManager)null))
      return; 
    do {
    
    } while (handleIdle(once));
    int minidle = ((IMAPStore)this.store).getMinIdleTime();
    if (minidle > 0)
      try {
        Thread.sleep(minidle);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }  
  }
  
  boolean startIdle(final IdleManager im) throws MessagingException {
    assert !Thread.holdsLock(this);
    synchronized (this) {
      checkOpened();
      if (im != null && this.idleManager != null && im != this.idleManager)
        throw new MessagingException("Folder already being watched by another IdleManager"); 
      Boolean started = (Boolean)doOptionalCommand("IDLE not supported", new ProtocolCommand() {
            public Object doCommand(IMAPProtocol p) throws ProtocolException {
              if (IMAPFolder.this.idleState == 1 && im != null && im == IMAPFolder.this
                .idleManager)
                return Boolean.TRUE; 
              if (IMAPFolder.this.idleState == 0) {
                p.idleStart();
                IMAPFolder.this.logger.finest("startIdle: set to IDLE");
                IMAPFolder.this.idleState = 1;
                IMAPFolder.this.idleManager = im;
                return Boolean.TRUE;
              } 
              try {
                IMAPFolder.this.messageCacheLock.wait();
              } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
              } 
              return Boolean.FALSE;
            }
          });
      this.logger.log(Level.FINEST, "startIdle: return {0}", started);
      return started.booleanValue();
    } 
  }
  
  boolean handleIdle(boolean once) throws MessagingException {
    Response r = null;
    while (true) {
      while (true) {
        r = this.protocol.readIdleResponse();
        try {
          synchronized (this.messageCacheLock) {
            if (r.isBYE() && r.isSynthetic() && this.idleState == 1) {
              Exception ex = r.getException();
              if (ex instanceof InterruptedIOException && ((InterruptedIOException)ex).bytesTransferred == 0) {
                if (ex instanceof java.net.SocketTimeoutException) {
                  this.logger.finest("handleIdle: ignoring socket timeout");
                  r = null;
                } else {
                  this.logger.finest("handleIdle: interrupting IDLE");
                  IdleManager im = this.idleManager;
                  if (im != null) {
                    this.logger.finest("handleIdle: request IdleManager to abort");
                    im.requestAbort(this);
                  } else {
                    this.logger.finest("handleIdle: abort IDLE");
                    this.protocol.idleAbort();
                    this.idleState = 2;
                  } 
                } 
                break;
              } 
            } 
            boolean done = true;
            try {
              if (this.protocol == null || 
                !this.protocol.processIdleResponse(r))
                return false; 
              done = false;
            } finally {
              if (done) {
                this.logger.finest("handleIdle: set to RUNNING");
                this.idleState = 0;
                this.idleManager = null;
                this.messageCacheLock.notifyAll();
              } 
            } 
            if (once && 
              this.idleState == 1) {
              try {
                this.protocol.idleAbort();
              } catch (Exception exception) {}
              this.idleState = 2;
            } 
          } 
        } catch (ConnectionException cex) {
          throw new FolderClosedException(this, cex.getMessage());
        } catch (ProtocolException pex) {
          throw new MessagingException(pex.getMessage(), pex);
        } 
        break;
      } 
      if (r != null && !this.protocol.hasResponse())
        return true; 
    } 
  }
  
  void waitIfIdle() throws ProtocolException {
    assert Thread.holdsLock(this.messageCacheLock);
    while (this.idleState != 0) {
      if (this.idleState == 1) {
        IdleManager im = this.idleManager;
        if (im != null) {
          this.logger.finest("waitIfIdle: request IdleManager to abort");
          im.requestAbort(this);
        } else {
          this.logger.finest("waitIfIdle: abort IDLE");
          this.protocol.idleAbort();
          this.idleState = 2;
        } 
      } else {
        this.logger.log(Level.FINEST, "waitIfIdle: idleState {0}", Integer.valueOf(this.idleState));
      } 
      try {
        if (this.logger.isLoggable(Level.FINEST))
          this.logger.finest("waitIfIdle: wait to be not idle: " + 
              Thread.currentThread()); 
        this.messageCacheLock.wait();
        if (this.logger.isLoggable(Level.FINEST))
          this.logger.finest("waitIfIdle: wait done, idleState " + this.idleState + ": " + 
              Thread.currentThread()); 
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new ProtocolException("Interrupted waitIfIdle", ex);
      } 
    } 
  }
  
  void idleAbort() {
    synchronized (this.messageCacheLock) {
      if (this.idleState == 1 && this.protocol != null) {
        this.protocol.idleAbort();
        this.idleState = 2;
      } 
    } 
  }
  
  void idleAbortWait() {
    synchronized (this.messageCacheLock) {
      if (this.idleState == 1 && this.protocol != null) {
        this.protocol.idleAbort();
        this.idleState = 2;
        try {
          do {
          
          } while (handleIdle(false));
        } catch (Exception ex) {
          this.logger.log(Level.FINEST, "Exception in idleAbortWait", ex);
        } 
        this.logger.finest("IDLE aborted");
      } 
    } 
  }
  
  SocketChannel getChannel() {
    return (this.protocol != null) ? this.protocol.getChannel() : null;
  }
  
  public Map<String, String> id(final Map<String, String> clientParams) throws MessagingException {
    checkOpened();
    return (Map<String, String>)doOptionalCommand("ID not supported", new ProtocolCommand() {
          public Object doCommand(IMAPProtocol p) throws ProtocolException {
            return p.id(clientParams);
          }
        });
  }
  
  public synchronized long getStatusItem(String item) throws MessagingException {
    if (!this.opened) {
      checkExists();
      IMAPProtocol p = null;
      Status status = null;
      try {
        p = getStoreProtocol();
        String[] items = { item };
        status = p.status(this.fullName, items);
        return status.getItem(item);
      } catch (BadCommandException bex) {
        return -1L;
      } catch (ConnectionException cex) {
        throw new StoreClosedException(this.store, cex.getMessage());
      } catch (ProtocolException pex) {
        throw new MessagingException(pex.getMessage(), pex);
      } finally {
        releaseStoreProtocol(p);
      } 
    } 
    return -1L;
  }
  
  public void handleResponse(Response r) {
    assert Thread.holdsLock(this.messageCacheLock);
    if (r.isOK() || r.isNO() || r.isBAD() || r.isBYE())
      ((IMAPStore)this.store).handleResponseCode(r); 
    if (r.isBYE()) {
      if (this.opened)
        cleanup(false); 
      return;
    } 
    if (r.isOK()) {
      r.skipSpaces();
      if (r.readByte() == 91) {
        String s = r.readAtom();
        if (s.equalsIgnoreCase("HIGHESTMODSEQ"))
          this.highestmodseq = r.readLong(); 
      } 
      r.reset();
      return;
    } 
    if (!r.isUnTagged())
      return; 
    if (!(r instanceof IMAPResponse)) {
      this.logger.fine("UNEXPECTED RESPONSE : " + r.toString());
      return;
    } 
    IMAPResponse ir = (IMAPResponse)r;
    if (ir.keyEquals("EXISTS")) {
      int exists = ir.getNumber();
      if (exists <= this.realTotal)
        return; 
      int count = exists - this.realTotal;
      Message[] msgs = new Message[count];
      this.messageCache.addMessages(count, this.realTotal + 1);
      int oldtotal = this.total;
      this.realTotal += count;
      this.total += count;
      if (this.hasMessageCountListener) {
        for (int i = 0; i < count; i++)
          msgs[i] = this.messageCache.getMessage(++oldtotal); 
        notifyMessageAddedListeners(msgs);
      } 
    } else if (ir.keyEquals("EXPUNGE")) {
      int seqnum = ir.getNumber();
      Message[] msgs = null;
      if (this.doExpungeNotification && this.hasMessageCountListener) {
        msgs = new Message[] { getMessageBySeqNumber(seqnum) };
        if (msgs[0] == null)
          msgs = null; 
      } 
      this.messageCache.expungeMessage(seqnum);
      this.realTotal--;
      if (msgs != null)
        notifyMessageRemovedListeners(false, msgs); 
    } else if (ir.keyEquals("VANISHED")) {
      String[] s = ir.readAtomStringList();
      if (s == null) {
        String uids = ir.readAtom();
        UIDSet[] uidset = UIDSet.parseUIDSets(uids);
        this.realTotal = (int)(this.realTotal - UIDSet.size(uidset));
        long[] luid = UIDSet.toArray(uidset);
        Message[] msgs = createMessagesForUIDs(luid);
        for (Message m : msgs) {
          if (m.getMessageNumber() > 0)
            this.messageCache.expungeMessage(m.getMessageNumber()); 
        } 
        if (this.doExpungeNotification && this.hasMessageCountListener)
          notifyMessageRemovedListeners(true, msgs); 
      } 
    } else if (ir.keyEquals("FETCH")) {
      assert ir instanceof FetchResponse : "!ir instanceof FetchResponse";
      Message msg = processFetchResponse((FetchResponse)ir);
      if (msg != null)
        notifyMessageChangedListeners(1, msg); 
    } else if (ir.keyEquals("RECENT")) {
      this.recent = ir.getNumber();
    } 
  }
  
  private Message processFetchResponse(FetchResponse fr) {
    IMAPMessage msg = getMessageBySeqNumber(fr.getNumber());
    if (msg != null) {
      boolean notify = false;
      UID uid = fr.<UID>getItem(UID.class);
      if (uid != null && msg.getUID() != uid.uid) {
        msg.setUID(uid.uid);
        if (this.uidTable == null)
          this.uidTable = new Hashtable<Long, IMAPMessage>(); 
        this.uidTable.put(Long.valueOf(uid.uid), msg);
        notify = true;
      } 
      MODSEQ modseq = fr.<MODSEQ>getItem(MODSEQ.class);
      if (modseq != null && msg._getModSeq() != modseq.modseq) {
        msg.setModSeq(modseq.modseq);
        notify = true;
      } 
      FLAGS flags = fr.<FLAGS>getItem(FLAGS.class);
      if (flags != null) {
        msg._setFlags(flags);
        notify = true;
      } 
      msg.handleExtensionFetchItems(fr.getExtensionItems());
      if (!notify)
        msg = null; 
    } 
    return msg;
  }
  
  void handleResponses(Response[] r) {
    for (int i = 0; i < r.length; i++) {
      if (r[i] != null)
        handleResponse(r[i]); 
    } 
  }
  
  protected synchronized IMAPProtocol getStoreProtocol() throws ProtocolException {
    this.connectionPoolLogger.fine("getStoreProtocol() borrowing a connection");
    return ((IMAPStore)this.store).getFolderStoreProtocol();
  }
  
  protected synchronized void throwClosedException(ConnectionException cex) throws FolderClosedException, StoreClosedException {
    if ((this.protocol != null && cex.getProtocol() == this.protocol) || (this.protocol == null && !this.reallyClosed))
      throw new FolderClosedException(this, cex.getMessage()); 
    throw new StoreClosedException(this.store, cex.getMessage());
  }
  
  protected IMAPProtocol getProtocol() throws ProtocolException {
    assert Thread.holdsLock(this.messageCacheLock);
    waitIfIdle();
    if (this.protocol == null)
      throw new ConnectionException("Connection closed"); 
    return this.protocol;
  }
  
  public Object doCommand(ProtocolCommand cmd) throws MessagingException {
    try {
      return doProtocolCommand(cmd);
    } catch (ConnectionException cex) {
      throwClosedException(cex);
    } catch (ProtocolException pex) {
      throw new MessagingException(pex.getMessage(), pex);
    } 
    return null;
  }
  
  public Object doOptionalCommand(String err, ProtocolCommand cmd) throws MessagingException {
    try {
      return doProtocolCommand(cmd);
    } catch (BadCommandException bex) {
      throw new MessagingException(err, bex);
    } catch (ConnectionException cex) {
      throwClosedException(cex);
    } catch (ProtocolException pex) {
      throw new MessagingException(pex.getMessage(), pex);
    } 
    return null;
  }
  
  public Object doCommandIgnoreFailure(ProtocolCommand cmd) throws MessagingException {
    try {
      return doProtocolCommand(cmd);
    } catch (CommandFailedException cfx) {
      return null;
    } catch (ConnectionException cex) {
      throwClosedException(cex);
    } catch (ProtocolException pex) {
      throw new MessagingException(pex.getMessage(), pex);
    } 
    return null;
  }
  
  protected synchronized Object doProtocolCommand(ProtocolCommand cmd) throws ProtocolException {
    if (this.protocol != null)
      synchronized (this.messageCacheLock) {
        return cmd.doCommand(getProtocol());
      }  
    IMAPProtocol p = null;
    try {
      p = getStoreProtocol();
      return cmd.doCommand(p);
    } finally {
      releaseStoreProtocol(p);
    } 
  }
  
  protected synchronized void releaseStoreProtocol(IMAPProtocol p) {
    if (p != this.protocol) {
      ((IMAPStore)this.store).releaseFolderStoreProtocol(p);
    } else {
      this.logger.fine("releasing our protocol as store protocol?");
    } 
  }
  
  protected void releaseProtocol(boolean returnToPool) {
    if (this.protocol != null) {
      this.protocol.removeResponseHandler(this);
      if (returnToPool) {
        ((IMAPStore)this.store).releaseProtocol(this, this.protocol);
      } else {
        this.protocol.disconnect();
        ((IMAPStore)this.store).releaseProtocol(this, null);
      } 
      this.protocol = null;
    } 
  }
  
  protected void keepConnectionAlive(boolean keepStoreAlive) throws ProtocolException {
    assert Thread.holdsLock(this.messageCacheLock);
    if (this.protocol == null)
      return; 
    if (System.currentTimeMillis() - this.protocol.getTimestamp() > 1000L) {
      waitIfIdle();
      if (this.protocol != null)
        this.protocol.noop(); 
    } 
    if (keepStoreAlive && ((IMAPStore)this.store).hasSeparateStoreConnection()) {
      IMAPProtocol p = null;
      try {
        p = ((IMAPStore)this.store).getFolderStoreProtocol();
        if (System.currentTimeMillis() - p.getTimestamp() > 1000L)
          p.noop(); 
      } finally {
        ((IMAPStore)this.store).releaseFolderStoreProtocol(p);
      } 
    } 
  }
  
  protected IMAPMessage getMessageBySeqNumber(int seqnum) {
    if (seqnum > this.messageCache.size()) {
      if (this.logger.isLoggable(Level.FINE))
        this.logger.fine("ignoring message number " + seqnum + " outside range " + this.messageCache
            .size()); 
      return null;
    } 
    return this.messageCache.getMessageBySeqnum(seqnum);
  }
  
  protected IMAPMessage[] getMessagesBySeqNumbers(int[] seqnums) {
    IMAPMessage[] msgs = new IMAPMessage[seqnums.length];
    int nulls = 0;
    for (int i = 0; i < seqnums.length; i++) {
      msgs[i] = getMessageBySeqNumber(seqnums[i]);
      if (msgs[i] == null)
        nulls++; 
    } 
    if (nulls > 0) {
      IMAPMessage[] nmsgs = new IMAPMessage[seqnums.length - nulls];
      for (int k = 0, j = 0; k < msgs.length; k++) {
        if (msgs[k] != null)
          nmsgs[j++] = msgs[k]; 
      } 
      msgs = nmsgs;
    } 
    return msgs;
  }
  
  private boolean isDirectory() {
    return ((this.type & 0x2) != 0);
  }
  
  public static interface ProtocolCommand {
    Object doCommand(IMAPProtocol param1IMAPProtocol) throws ProtocolException;
  }
}
