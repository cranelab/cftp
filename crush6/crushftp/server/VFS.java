package crushftp.server;

import com.crushftp.client.Common;
import com.crushftp.client.GenericClient;
import com.crushftp.client.GenericClientMulti;
import com.crushftp.client.VRL;
import crushftp.handlers.CIProperties;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.UserTools;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

public class VFS implements Serializable {
  private static final long serialVersionUID = 1L;
  
  public Vector homes = new Vector();
  
  transient Properties clientCacheFree = new Properties();
  
  transient Properties clientCacheUsed = new Properties();
  
  transient Vector connectedVFSItems = new Vector();
  
  public Vector permissions = new Vector();
  
  public Properties cacheItem = new Properties();
  
  public Properties cacheItemStamp = new Properties();
  
  public Properties cacheList = new Properties();
  
  public Properties cacheListStamp = new Properties();
  
  public static Properties quotaCache = new Properties();
  
  public static Vector activeQuotaChecks = new Vector();
  
  public long cacheLife = 10L;
  
  public long cacheLifeGetItem = 259200L;
  
  public Properties cacheVFS = new Properties();
  
  public Properties cacheVFSStamp = new Properties();
  
  public long cacheVFSLife = 10L;
  
  public String username = "";
  
  public String password = "";
  
  public String ip = "";
  
  public int port = -1;
  
  public String protocol = "";
  
  public Properties user_info = new Properties();
  
  public Properties user = new Properties();
  
  public ServerSession thisSession = null;
  
  public boolean singlePool = false;
  
  SimpleDateFormat month = new SimpleDateFormat("MMM ", Locale.US);
  
  SimpleDateFormat day = new SimpleDateFormat("dd", Locale.US);
  
  SimpleDateFormat year = new SimpleDateFormat("yyyy", Locale.US);
  
  SimpleDateFormat mm = new SimpleDateFormat("MM", Locale.US);
  
  SimpleDateFormat yyyy = new SimpleDateFormat("yyyy", Locale.US);
  
  SimpleDateFormat hhmm = new SimpleDateFormat("HH:mm", Locale.US);
  
  public static VFS getVFS(Properties virtual) {
    if (!virtual.containsKey("/")) {
      Properties pp = new Properties();
      pp.put("virtualPath", "/");
      pp.put("name", "VFS");
      pp.put("type", "DIR");
      pp.put("vItems", new Vector());
      virtual.put("/", pp);
    } 
    VFS tempVFS = new VFS();
    tempVFS.configVirtual(virtual);
    return tempVFS;
  }
  
  protected VFS() {
    this.singlePool = System.getProperty("crushftp.vfs.singlePool", "false").equals("true");
  }
  
  private void configVirtual(Properties virtual) {
    this.homes.clear();
    this.homes.addElement(virtual);
    this.permissions = (Vector)virtual.get("vfs_permissions_object");
  }
  
  public ServerSession getServerSession() {
    return this.thisSession;
  }
  
  public GenericClient getClient(Properties item) throws Exception {
    if (this.homes.size() > 1 && this.user.getProperty("sync_vfs", "true").equals("true")) {
      String root_path = getRootVFS(String.valueOf(item.getProperty("root_dir")) + item.getProperty("name"), -1);
      int count = 0;
      for (int x = 0; x < this.homes.size(); x++) {
        String root_path2 = getRootVFS(String.valueOf(item.getProperty("root_dir")) + item.getProperty("name"), x);
        if (root_path.equals(root_path2))
          count++; 
      } 
      if (count <= 1)
        return getClientSingle(item); 
      return getClientMulti(item);
    } 
    return getClientSingle(item);
  }
  
  public GenericClient getClientMulti(Properties item) throws Exception {
    Properties originalvItem = (Properties)item.get("vItem");
    String get_item_path = String.valueOf(item.getProperty("root_dir")) + item.getProperty("name");
    Vector clients = new Vector();
    Vector vItems = new Vector();
    for (int x = 0; this.homes.size() > 1 && x < this.homes.size(); x++) {
      item = get_item(get_item_path, x);
      if (item == null)
        item = get_item_parent(get_item_path, x); 
      if (!(new VRL(item.getProperty("url"))).getProtocol().equalsIgnoreCase("virtual")) {
        GenericClient c = getClientSingle(item);
        clients.addElement(c);
        vItems.addElement(item.get("vItem"));
      } 
    } 
    String logHeader = "PROXY:";
    if (this.thisSession != null)
      logHeader = "[" + this.thisSession.server_item.getProperty("serverType", "ftp") + ":" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] " + "PROXY" + " : "; 
    return new GenericClientMulti(logHeader, Common.log, originalvItem, vItems, clients);
  }
  
  public GenericClient getClientSingle(Properties item) throws Exception {
    boolean needLogin = false;
    GenericClient c = null;
    Vector vU = null;
    Vector vF = null;
    synchronized (this.connectedVFSItems) {
      String baseURL = Common.getBaseUrl(item.getProperty("url"));
      if (!this.clientCacheUsed.containsKey(baseURL))
        this.clientCacheUsed.put(baseURL, new Vector()); 
      if (!this.clientCacheFree.containsKey(baseURL))
        this.clientCacheFree.put(baseURL, new Vector()); 
      vU = (Vector)this.clientCacheUsed.get(baseURL);
      vF = (Vector)this.clientCacheFree.get(baseURL);
      int loop = 0;
      while (this.singlePool && vF.size() == 0 && vU.size() > 0) {
        Thread.sleep(1000L);
        if (loop++ == 10) {
          Log.log("VFS", 0, "Waiting for free connection...");
          loop = 0;
        } 
      } 
      if (vF.size() > 0) {
        c = vF.remove(0);
      } else {
        needLogin = true;
        Log.log("VFS", 2, "Create new GenericClient to url:" + baseURL);
        String logHeader = "PROXY:";
        if (this.thisSession != null)
          logHeader = "[" + this.thisSession.server_item.getProperty("serverType", "ftp") + ":" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] " + "PROXY" + " : "; 
        c = Common.getClient(baseURL, logHeader, Common.log);
        if (c == null)
          Log.log("SERVER", 0, "Error creating client for:" + item); 
      } 
      c.setConfig("clientid", (this.thisSession == null) ? null : this.thisSession.uiSG("clientid"));
      c.setConfig("item", item);
      c.setConfig("s3_buffer", (new StringBuffer(String.valueOf(ServerStatus.IG("s3_buffer")))).toString());
      if (this.thisSession != null) {
        c.setConfig("proxy_user_ip", this.thisSession.user_info.getProperty("user_ip"));
        c.setConfig("proxy_user_port", this.thisSession.user_info.getProperty("user_port"));
        c.setConfig("proxy_user_protocol", this.thisSession.user_info.getProperty("user_protocol"));
        c.setConfig("proxy_bind_ip", this.thisSession.user_info.getProperty("bind_ip"));
        c.setConfig("proxy_bind_port", this.thisSession.user_info.getProperty("bind_port"));
      } 
      if (ServerStatus.BG("count_dir_items"))
        c.setConfig("count_dir_items", "true"); 
      Enumeration keys = item.keys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement().toString();
        c.setConfig(key, item.get(key));
        if (key.equals("ssh_private_key_pass"))
          c.setConfig(key, ServerStatus.thisObj.common_code.decode_pass(item.getProperty(key))); 
      } 
      if (c instanceof com.crushftp.client.AS2Client)
        c.setConfig("uVFS", this); 
      if ((this.thisSession != null && !this.thisSession.user.getProperty("filePublicEncryptionKey", "").equals("")) || ServerStatus.BG("fileEncryption")) {
        c.setConfig("checkEncryptedHeader", "true");
      } else {
        c.setConfig("checkEncryptedHeader", "false");
      } 
      String privs = item.getProperty("privs", "");
      for (int x = 0; x < (privs.split("\\(")).length; x++) {
        String priv = privs.split("\\(")[x];
        if (!priv.equals("")) {
          priv = priv.substring(0, priv.length() - 1).trim();
          if (priv.indexOf("=") >= 0)
            c.setConfig(priv.split("=")[0], priv.substring(priv.indexOf("=") + 1)); 
        } 
      } 
    } 
    if (needLogin) {
      VRL vrl = new VRL(item.getProperty("url"));
      if (Common.System2.get("crushftp.dmz.queue") != null)
        c.setCache(this.cacheList); 
      c.login(vrl.getUsername(), vrl.getPassword(), (String)c.getConfig("clientid"));
    } 
    vU.addElement(c);
    return c;
  }
  
  public GenericClient releaseClient(GenericClient c) throws Exception {
    if (c == null)
      return null; 
    if (c instanceof GenericClientMulti) {
      Vector clients = ((GenericClientMulti)c).clients;
      while (clients.size() > 0) {
        GenericClient cc = clients.remove(0);
        releaseClientItem(cc);
      } 
      return null;
    } 
    return releaseClientItem(c);
  }
  
  public GenericClient releaseClientItem(GenericClient c) throws Exception {
    if (c == null)
      return null; 
    Properties item = (Properties)c.getConfig("item");
    c.setConfig("uVFS", null);
    if (item != null) {
      String baseURL = Common.getBaseUrl(item.getProperty("url"));
      if (!this.clientCacheUsed.containsKey(baseURL))
        this.clientCacheUsed.put(baseURL, new Vector()); 
      if (!this.clientCacheFree.containsKey(baseURL))
        this.clientCacheFree.put(baseURL, new Vector()); 
      Vector vU = (Vector)this.clientCacheUsed.get(baseURL);
      Vector vF = (Vector)this.clientCacheFree.get(baseURL);
      c.setConfig("item", null);
      vU.remove(c);
      if (c.getConfig("error", "").equals(""))
        vF.addElement(c); 
    } 
    return null;
  }
  
  public void setUserPassIpPortProtocol(String username, String password, String ip, int port, String protocol, Properties user_info, Properties user, ServerSession thisSession) {
    this.username = username;
    this.password = password;
    this.ip = ip;
    this.port = port;
    this.protocol = protocol;
    this.user_info = user_info;
    this.user = user;
    this.thisSession = thisSession;
  }
  
  public void addLinkedVFS(VFS tempVFS) {
    this.homes.addAll(tempVFS.homes);
    this.permissions.addAll(tempVFS.permissions);
  }
  
  public void reset() {
    this.cacheItem.clear();
    this.cacheItemStamp.clear();
  }
  
  public void resetAll() {
    quotaCache.clear();
  }
  
  public boolean isReal(String path) {
    path = fixPath(path);
    return isVFSFolder(path);
  }
  
  public void free() {
    if (this.connectedVFSItems == null)
      return; 
    synchronized (this.connectedVFSItems) {
      while (this.connectedVFSItems.contains(this))
        this.connectedVFSItems.remove(this); 
    } 
  }
  
  public void disconnect() {
    if (this.clientCacheFree == null || this.connectedVFSItems == null)
      return; 
    synchronized (this.connectedVFSItems) {
      if (this.connectedVFSItems.size() == 0) {
        Enumeration keys = this.clientCacheFree.keys();
        while (keys.hasMoreElements()) {
          String key = keys.nextElement().toString();
          Vector v = (Vector)this.clientCacheFree.get(key);
          Vector vTmp = (Vector)this.clientCacheUsed.get(key);
          v.addAll(vTmp);
          vTmp.clear();
          while (v.size() > 0) {
            GenericClient c = v.remove(0);
            c.setConfig("uVFS", null);
            c.setConfig("item", null);
            try {
              c.logout();
            } catch (Exception e) {
              Log.log("VFS", 2, e);
            } 
          } 
          this.clientCacheFree.remove(key);
          this.clientCacheUsed.remove(key);
        } 
      } 
    } 
  }
  
  public Properties get_item(String path) throws Exception {
    return get_item(path, -1);
  }
  
  public Properties get_item(String path, int homeItem) throws Exception {
    Properties dir_item = null;
    path = fixPath(path);
    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1); 
    if (!path.startsWith("/"))
      path = "/" + path; 
    String pathOriginal = path;
    for (int x = 0; x < this.homes.size(); x++) {
      if (homeItem >= 0)
        x = homeItem; 
      Properties tempVirtual = this.homes.elementAt(x);
      path = getRootVFS(pathOriginal, x);
      Properties p = (Properties)tempVirtual.get(path);
      Log.log("VFS", 3, "get_item:" + path);
      if (p != null && p.getProperty("type", "DIR").equalsIgnoreCase("FILE")) {
        Properties vItem = vItemPick((Vector)p.get("vItems"));
        GenericClient c = getClientSingle(vItem);
        try {
          String tempPath = pathOriginal.substring(path.length());
          if (tempPath.equals("") && vItem.getProperty("type", "DIR").equalsIgnoreCase("DIR")) {
            VRL vrl = new VRL(vItem.getProperty("url"));
            dir_item = new Properties();
            dir_item.put("url", String.valueOf(vItem.getProperty("url")) + tempPath);
            dir_item.put("local", "false");
            dir_item.put("protocol", vrl.getProtocol());
            dir_item.put("dir", path);
            String name = Common.last(path);
            if (name.endsWith("/"))
              name = name.substring(0, name.length() - 1); 
            dir_item.put("name", Common.last(path));
            dir_item.put("root_dir", Common.all_but_last(path));
            dir_item.put("type", vItem.getProperty("type", "DIR").toUpperCase());
            dir_item.put("permissions", "drwxrwxrwx");
            dir_item.put("num_items", "1");
            dir_item.put("owner", "owner");
            dir_item.put("group", "group");
            dir_item.put("size", "1");
            Date d = new Date();
            dir_item.put("modified", (new StringBuffer(String.valueOf(d.getTime()))).toString());
            dir_item.put("month", this.month.format(d));
            dir_item.put("day", this.day.format(d));
            dir_item.put("time_or_year", this.year.format(d));
          } else {
            if (tempPath.startsWith("/") && vItem.getProperty("url").endsWith("/"))
              tempPath = tempPath.substring(1); 
            dir_item = c.stat((new VRL(String.valueOf(vItem.getProperty("url")) + Common.url_encode(tempPath))).getPath());
            if (dir_item != null && tempPath.equals(""))
              dir_item.put("name", p.getProperty("name")); 
          } 
        } finally {
          c = releaseClient(c);
        } 
        if (dir_item != null)
          dir_item.put("vItem", vItem); 
      } else if (p != null && p.getProperty("type", "DIR").equalsIgnoreCase("DIR") && path.equals(pathOriginal)) {
        dir_item = new Properties();
        dir_item.put("name", p.getProperty("name"));
        dir_item.put("url", "virtual://" + path);
        dir_item.put("type", "DIR");
        dir_item.put("owner", "owner");
        dir_item.put("group", "group");
        dir_item.put("root_dir", "/");
        dir_item.put("num_items", "1");
        dir_item.put("month", "1");
        dir_item.put("day", "1");
        dir_item.put("time_or_year", "1970");
        dir_item.put("permissions", "drwxrwxrwx");
        dir_item.put("size", "1");
      } 
      if (dir_item != null) {
        String parentPath = Common.all_but_last(pathOriginal);
        if (!parentPath.startsWith("/"))
          parentPath = "/" + parentPath; 
        dir_item.put("root_dir", parentPath.equals("") ? "/" : parentPath);
        setPermissions(dir_item);
        dir_item.put("protocol", (new VRL(dir_item.getProperty("url"))).getProtocol());
      } 
      if (dir_item != null || 
        homeItem >= 0)
        break; 
    } 
    return dir_item;
  }
  
  public void getListing(Vector list, String path2) throws Exception {
    if (path2.equals(""))
      path2 = "/"; 
    String path = path2;
    for (int x = 0; x < this.homes.size(); x++) {
      Properties tempVirtual = this.homes.elementAt(x);
      String parentPath = getRootVFS(path, x);
      Properties dir_item = get_item(parentPath, x);
      if (dir_item != null) {
        Vector list2 = new Vector();
        Properties status = new Properties();
        if ((new VRL(dir_item.getProperty("url"))).getProtocol().equalsIgnoreCase("VIRTUAL")) {
          Enumeration keys = tempVirtual.keys();
          while (keys.hasMoreElements()) {
            String key = keys.nextElement().toString();
            if (!key.equals("/") && !path.equals("") && key.toUpperCase().startsWith(path.toUpperCase()))
              if (key.substring(path.length()).indexOf("/") < 0) {
                Properties p = (Properties)tempVirtual.get(key);
                Properties item = new Properties();
                item.put("name", p.getProperty("name"));
                item.put("type", p.getProperty("type"));
                Date itemDate = new Date();
                if (p.getProperty("type").equalsIgnoreCase("DIR")) {
                  item.put("permissions", "drwxrwxrwx");
                  item.put("size", "1");
                  item.put("url", "virtual://" + path + p.getProperty("name") + "/");
                } else {
                  item.put("permissions", "-rwxrwxrwx");
                  item.put("size", "1");
                  item.put("url", "virtual://" + path + p.getProperty("name"));
                  Vector vItems = (Vector)p.get("vItems");
                  if (vItems != null && vItems.size() > 0) {
                    Properties actualItem = vItems.elementAt(0);
                    VRL avrl = new VRL(actualItem.getProperty("url"));
                    if (avrl.getProtocol().equalsIgnoreCase("FILE")) {
                      GenericClient c = getClientSingle(actualItem);
                      if (c != null)
                        try {
                          Properties stat = c.stat(avrl.getPath());
                          if (stat != null) {
                            item.put("size", stat.getProperty("size", "1"));
                            if (stat.containsKey("modified"))
                              itemDate = new Date(Long.parseLong(stat.getProperty("modified"))); 
                          } 
                        } finally {
                          releaseClient(c);
                        }  
                    } 
                  } 
                } 
                item.put("link", "false");
                item.put("num_items", "1");
                item.put("owner", "user");
                item.put("group", "group");
                item.put("protocol", "virtual");
                item.put("modified", (new StringBuffer(String.valueOf(itemDate.getTime()))).toString());
                item.put("month", GenericClient.months[Integer.parseInt(this.mm.format(itemDate))]);
                item.put("day", this.day.format(itemDate));
                String time_or_year = this.hhmm.format(itemDate);
                if (!this.yyyy.format(itemDate).equals(this.yyyy.format(new Date())))
                  time_or_year = this.yyyy.format(itemDate); 
                item.put("time_or_year", time_or_year);
                list2.addElement(item);
              }  
          } 
          status.put("status", "DONE");
        } else {
          Runnable r = new Runnable(this, dir_item, path, parentPath, list2, status) {
              final VFS this$0;
              
              private final Properties val$dir_item;
              
              private final String val$path;
              
              private final String val$parentPath;
              
              private final Vector val$list2;
              
              private final Properties val$status;
              
              public void run() {
                try {
                  GenericClient c = this.this$0.getClient(this.val$dir_item);
                  try {
                    String urlpath = (new VRL(this.val$dir_item.getProperty("url"))).getPath();
                    if (urlpath.endsWith("/"))
                      urlpath = urlpath.substring(0, urlpath.length() - 1); 
                    c.list(String.valueOf(urlpath) + this.val$path.substring(this.val$parentPath.length()), this.val$list2);
                  } catch (Exception e) {
                    c.setConfig("error", e.toString());
                    this.val$status.put("error", e);
                  } finally {
                    c = this.this$0.releaseClient(c);
                  } 
                } catch (Exception e) {
                  Common.debug(1, e);
                } 
                this.val$status.put("status", "DONE");
              }
            };
          if (ServerStatus.BG("listing_multithreaded")) {
            Worker.startWorker(r, String.valueOf(Thread.currentThread().getName()) + ":List VFS");
          } else {
            r.run();
          } 
        } 
        boolean pathIsFromVFS = isReal(path);
        String parentPath2 = parentPath;
        while (list2.size() > 0 || status.size() == 0) {
          if (list2.size() == 0) {
            Thread.sleep(100L);
            continue;
          } 
          Properties pp = list2.remove(0);
          if (pathIsFromVFS)
            if (!pp.getProperty("name").equals(".DS_Store")) {
              if (!parentPath2.endsWith("/"))
                parentPath2 = String.valueOf(parentPath2) + "/"; 
              try {
                Properties tempItem = get_item(String.valueOf(parentPath2) + pp.getProperty("name"), x);
                if (tempItem != null) {
                  pp.put("type", tempItem.getProperty("type", "DIR").toUpperCase());
                  pp.put("size", tempItem.getProperty("size"));
                  pp.put("modified", tempItem.getProperty("modified", "0"));
                  if (tempItem.getProperty("type", "DIR").equalsIgnoreCase("DIR"))
                    pp.put("permissions", "drwxrwxrwx"); 
                } 
              } catch (Exception e) {
                if (ServerStatus.BG("stop_listing_on_login_failure") && e.toLowerCase().indexOf("failure") > 0 && this.user != null && this.user.size() > 0 && !this.username.equals(""))
                  throw e; 
                pp.put("type", "DIR");
                pp.put("permissions", "drwxrwxrwx");
                Log.log("VFS", 2, e);
              } 
            }  
          String parentPath3 = Common.all_but_last(String.valueOf(path) + pp.getProperty("name"));
          pp.put("root_dir", parentPath3.equals("") ? "/" : parentPath3);
          try {
            setPermissions(pp);
          } catch (Exception ee) {
            Log.log("VFS", 1, (String)pp);
            Log.log("VFS", 1, ee);
          } 
          list.add(pp);
        } 
        if (status.containsKey("error"))
          throw (Exception)status.get("error"); 
      } 
    } 
  }
  
  public void getListing(Vector list, String path, int depth, int maxSize, boolean includeFolders) throws Exception {
    getListing(list, path, depth, maxSize, includeFolders, null);
  }
  
  public void getListing(Vector list, String path, int depth, int maxSize, boolean includeFolders, Vector filters) throws Exception {
    getListing(list, path, depth, maxSize, includeFolders, filters, null);
  }
  
  public void getListing(Vector list, String path, int depth, int maxSize, boolean includeFolders, Vector filters, RETR_handler retr) throws Exception {
    Properties item = null;
    try {
      item = get_item(path);
    } catch (Exception e) {
      Log.log("VFS", 1, e);
    } 
    if (item.getProperty("type", "").equalsIgnoreCase("FILE")) {
      list.addElement(item);
    } else {
      appendListing(path, list, "", depth, maxSize, includeFolders, filters, retr);
    } 
  }
  
  public void appendListing(String path, Vector list, String dir, int depth, int maxSize, boolean includeFolders, Vector filters, RETR_handler retr) throws Exception {
    Vector aDir = new Vector();
    if (depth > 0) {
      depth--;
      if (!path.endsWith("/"))
        path = String.valueOf(path) + "/"; 
      if (Common.filterDir(String.valueOf(path) + dir, filters))
        getListing(aDir, String.valueOf(path) + dir); 
      for (int x = 0; x < aDir.size(); x++) {
        int loops = 0;
        while (list.size() > maxSize) {
          Thread.sleep(100L);
          if (retr != null && !retr.active)
            return; 
          if (retr == null && loops++ > 6000)
            return; 
        } 
        Properties item = aDir.elementAt(x);
        if (item.getProperty("type", "").equalsIgnoreCase("FILE")) {
          list.addElement(item);
        } else if (item.getProperty("type", "").equalsIgnoreCase("DIR")) {
          appendListing(path, list, String.valueOf(dir) + item.getProperty("name") + "/", depth, maxSize, includeFolders, filters, retr);
        } 
      } 
    } 
    try {
      if (aDir.size() == 0 || includeFolders)
        list.addElement(get_item(String.valueOf(path) + dir)); 
    } catch (Exception exception) {}
  }
  
  public Properties get_item_parent(String path) throws Exception {
    return get_item_parent(path, -1);
  }
  
  public Properties get_item_parent(String path, int homeItem) throws Exception {
    Log.log("VFS", 3, "get_item_parent:path=" + path);
    String parentPath = getRootVFS(path, homeItem);
    if (!parentPath.startsWith("/"))
      parentPath = "/" + parentPath; 
    if (!parentPath.endsWith("/"))
      parentPath = String.valueOf(parentPath) + "/"; 
    Log.log("VFS", 3, "get_item_parent:parentPath=" + parentPath);
    Properties dir_item = get_item(parentPath, homeItem);
    Log.log("VFS", 3, "get_item_parent:got root VFS dir item with privs=" + dir_item);
    if (dir_item.getProperty("root_dir", "").equals("/") && dir_item.getProperty("url", "").toUpperCase().endsWith("/VFS/"))
      dir_item.put("privs", ""); 
    dir_item.put("name", Common.last(path));
    String newPath = path.substring(path.indexOf(parentPath) + parentPath.length());
    Log.log("VFS", 3, "get_item_parent:newPath=" + newPath);
    String temp_url = String.valueOf(dir_item.getProperty("url")) + newPath;
    dir_item.put("url", String.valueOf(dir_item.getProperty("url")) + Common.all_but_last(newPath));
    dir_item.put("root_dir", String.valueOf(parentPath) + Common.all_but_last(newPath));
    dir_item.put("privs", getPriv(path, dir_item));
    dir_item.put("url", temp_url);
    Log.log("VFS", 3, "get_item_parent:contructed dir_item=" + dir_item);
    return dir_item;
  }
  
  public Properties get_fake_item(String path, String expectedType) throws Exception {
    return get_fake_item(path, expectedType, -1);
  }
  
  public Properties get_fake_item(String path, String expectedType, int homeItem) throws Exception {
    return get_item(path, homeItem);
  }
  
  public Properties vItemPick(Vector vItems) {
    Properties p = null;
    Properties p0 = (Properties)((Properties)vItems.elementAt(0)).clone();
    p = p0;
    if (this.username.indexOf("`") >= 0) {
      this.user_info.put("proxy_id", "outgoing_ftp");
      p = vItems.elementAt(1);
      this.protocol = this.username.split("`")[1];
      this.ip = this.username.split("`")[2];
      this.port = Integer.parseInt(this.username.split("`")[3]);
      this.username = this.username.split("`")[0];
    } 
    if (this.user_info != null && !this.user_info.getProperty("proxy_id", "").equals(""))
      for (int x = 0; x < vItems.size(); x++) {
        Properties pp = vItems.elementAt(x);
        if (pp.getProperty("proxy_id", "").equals(this.user_info.getProperty("proxy_id", ""))) {
          if (p.getProperty("ip", "").equals("") || p.getProperty("ip").equals(this.ip))
            p = (Properties)pp.clone(); 
          if (!pp.getProperty("proxy_socket_mode", "").equals(""))
            this.user_info.put("proxy_socket_mode", pp.getProperty("proxy_socket_mode", "")); 
        } 
      }  
    if (!p.getProperty("dumb_proxy", "").equals(""))
      this.user_info.put("dumb_proxy", p.getProperty("dumb_proxy", "")); 
    String url = p.getProperty("url", "");
    url = Common.replace_str(url, "%username%", Common.url_encode(this.username, ""));
    url = Common.replace_str(url, "%user_name%", Common.url_encode(this.username, ""));
    url = Common.replace_str(url, "%password%", Common.url_encode(this.password, ""));
    url = Common.replace_str(url, "%user_pass%", Common.url_encode(this.password, ""));
    url = Common.replace_str(url, "%ip%", this.ip);
    url = Common.replace_str(url, "%port%", (new StringBuffer(String.valueOf(this.port))).toString());
    url = Common.replace_str(url, "%protocol%", this.protocol);
    if (this.thisSession != null)
      url = ServerStatus.change_vars_to_values_static(url, this.thisSession.user, this.thisSession.user_info, this.thisSession); 
    p.put("url", url);
    return p;
  }
  
  public String getPriv(String path, Properties dir_item) {
    if (!path.startsWith("/"))
      path = "/" + path; 
    String originalPath = path;
    Properties permission = getCombinedPermissions();
    path = originalPath;
    String priv2 = getPriv2(originalPath, permission);
    boolean aclPermissions = permission.getProperty("acl_permissions", "false").equals("true");
    Properties dir_item2 = dir_item;
    if (dir_item.getProperty("type", "DIR").equalsIgnoreCase("FILE")) {
      dir_item2 = (Properties)dir_item.clone();
      dir_item2.put("url", Common.all_but_last(dir_item2.getProperty("url")));
    } 
    if (aclPermissions && (new VRL(dir_item.getProperty("url"))).getProtocol().equalsIgnoreCase("file"))
      try {
        String priv = "";
        if (ServerStatus.SG("acl_mode").equals("1")) {
          priv = getAcl(dir_item2, (Vector)permission.get("acl_group_memberships"));
        } else if (ServerStatus.SG("acl_mode").equals("2")) {
          priv = getAcl2(dir_item2, permission.getProperty("acl_domain", ""));
        } 
        if (priv2.indexOf("(slideshow)") >= 0)
          priv = String.valueOf(priv) + "(slideshow)"; 
        if (priv2.indexOf("(share)") >= 0)
          priv = String.valueOf(priv) + "(share)"; 
        return priv;
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
      }  
    if (aclPermissions && dir_item.getProperty("type", "DIR").equalsIgnoreCase("FILE") && (new VRL(dir_item.getProperty("url"))).getProtocol().equalsIgnoreCase("file"))
      try {
        String priv = "";
        if (ServerStatus.SG("acl_mode").equals("1")) {
          priv = getAcl(dir_item, (Vector)permission.get("acl_group_memberships"));
        } else if (ServerStatus.SG("acl_mode").equals("2")) {
          priv = getAcl2(dir_item, permission.getProperty("acl_domain", ""));
        } 
        if (priv2.indexOf("(slideshow)") >= 0)
          priv = String.valueOf(priv) + "(slideshow)"; 
        if (priv2.indexOf("(share)") >= 0)
          priv = String.valueOf(priv) + "(share)"; 
        return priv;
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
      }  
    return priv2;
  }
  
  private String getPriv2(String originalPath, Properties permission) {
    String path = originalPath;
    String priv = "";
    while (!path.equals("")) {
      if ((path.equals("/") && permission.containsKey(UserTools.getUpper(path))) || (!path.equals("/") && permission.containsKey(UserTools.getUpper(path)))) {
        priv = permission.getProperty(UserTools.getUpper(path), "");
        if (!path.equals(originalPath))
          priv = String.valueOf(priv) + "(inherited)"; 
        return priv;
      } 
      path = Common.all_but_last(path);
    } 
    return priv;
  }
  
  public String getAcl(Properties dir_item, Vector memberships) throws Exception {
    Common.debug(2, "ACL:url:" + dir_item.getProperty("url"));
    String localPath = (new VRL(dir_item.getProperty("url"))).getPath().replace('/', '\\');
    if (localPath.startsWith("\\") && !localPath.startsWith("\\\\"))
      localPath = localPath.substring(1); 
    if (this.cacheItemStamp.containsKey(localPath) && System.currentTimeMillis() > Long.parseLong(this.cacheItemStamp.getProperty(localPath, (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString()))) {
      this.cacheItemStamp.remove(localPath);
      this.cacheItem.remove(localPath);
    } 
    if (this.cacheItem.containsKey(localPath))
      return this.cacheItem.getProperty(localPath); 
    Common.debug(2, "ACL: memberships:" + memberships);
    String item_privs = "";
    Common.debug(2, "ACL: accesschk.exe;-accepteula;-dqvu;" + localPath);
    Process proc = Runtime.getRuntime().exec(("accesschk.exe;-accepteula;-dqvu;" + localPath).split(";"));
    BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
    String data = "";
    try {
      Properties rec = null;
      String ug = "";
      String ug_header = "";
      boolean foundAccess = false;
      while (true) {
        data = br.readLine();
        Common.debug(2, "ACL:" + data);
        if (data != null && data.toUpperCase().indexOf("NO MATCHING OBJECTS") >= 0) {
          String path = localPath.replace('\\', '/');
          if (!path.startsWith("/"))
            path = "/" + path; 
          if (path.endsWith("/"))
            path = path.substring(0, path.length() - 1); 
          item_privs = "(resume)";
          break;
        } 
        if (data == null || (!data.startsWith(" ") && !data.startsWith("\t"))) {
          if (rec != null && rec.getProperty("path") != null) {
            String path2 = rec.getProperty("path");
            if (!path2.endsWith("/"))
              path2 = String.valueOf(path2) + "/"; 
            String str1 = localPath.replace('\\', '/');
            if (!str1.startsWith("/"))
              str1 = "/" + str1; 
            if (!str1.endsWith("/"))
              str1 = String.valueOf(str1) + "/"; 
            str1 = Common.all_but_last(str1);
            path2 = path2.substring(str1.length() - 1).toUpperCase();
            if (!foundAccess)
              item_privs = "(invisible)"; 
            item_privs = rec.getProperty("priv", "(invisible)");
          } 
          if (data == null)
            break; 
          foundAccess = false;
          rec = new Properties();
          String path = data.trim().replace('\\', '/');
          if (!path.startsWith("/"))
            path = "/" + path; 
          if (!path.endsWith("/"))
            path = String.valueOf(path) + "/"; 
          rec.put("path", path);
          continue;
        } 
        if (data.trim().startsWith("Medium Mandatory"))
          continue; 
        if (!data.startsWith("\t")) {
          ug = data.substring(4).trim();
          ug_header = data.substring(0, 4).trim();
          continue;
        } 
        data = data.trim();
        String privs = data.toUpperCase();
        if (memberships.indexOf(ug.toUpperCase()) < 0 && memberships.indexOf(ug) < 0)
          continue; 
        foundAccess = true;
        String s = rec.getProperty("priv", "");
        if (ug_header.equals("R"))
          s = String.valueOf(s) + ap(s, "(read)") + ap(s, "(resume)") + ap(s, "(view)") + ap(s, "(slideshow)"); 
        if (ug_header.equals("RW"))
          s = String.valueOf(s) + ap(s, "(read)") + ap(s, "(rename)") + ap(s, "(resume)") + ap(s, "(view)") + ap(s, "(write)") + ap(s, "(makedir)") + ap(s, "(slideshow)"); 
        if (ug_header.equals("W"))
          s = String.valueOf(s) + ap(s, "(resume)") + ap(s, "(rename)") + ap(s, "(write)") + ap(s, "(makedir)"); 
        if (privs.equals("FILE_ADD_FILE"))
          s = String.valueOf(s) + ap(s, "(resume)") + ap(s, "(rename)") + ap(s, "(write)") + ap(s, "(makedir)"); 
        if (privs.equals("FILE_ADD_SUBDIRECTORY"))
          s = String.valueOf(s) + ap(s, "(resume)") + ap(s, "(rename)") + ap(s, "(write)") + ap(s, "(makedir)"); 
        if (privs.equals("FILE_DELETE_CHILD"))
          s = String.valueOf(s) + ap(s, "(delete)") + ap(s, "(deletedir)"); 
        if (privs.equals("DELETE"))
          s = String.valueOf(s) + ap(s, "(delete)") + ap(s, "(deletedir)"); 
        if (privs.equals("FILE_ALL_ACCESS"))
          s = String.valueOf(s) + ap(s, "(read)") + ap(s, "(rename)") + ap(s, "(resume)") + ap(s, "(view)") + ap(s, "(write)") + ap(s, "(makedir)") + ap(s, "(delete)") + ap(s, "(deletedir)") + ap(s, "(slideshow)"); 
        if (ug_header.equals(""))
          s = "(invisible)"; 
        rec.put(ug.toUpperCase(), s);
        rec.put("priv", s);
      } 
    } finally {
      br.close();
      proc.destroy();
    } 
    this.cacheItem.put(localPath, item_privs);
    this.cacheItemStamp.put(localPath, (new StringBuffer(String.valueOf(System.currentTimeMillis() + (1000 * ServerStatus.IG("acl_cache_timeout"))))).toString());
    Common.debug(2, "ACL: priv:" + item_privs);
    return item_privs;
  }
  
  static Vector acl_processes = new Vector();
  
  public String getAcl2(Properties dir_item, String acl_domain) throws Exception {
    Common.debug(2, "ACL:url:" + dir_item.getProperty("url"));
    String localPath = (new VRL(dir_item.getProperty("url"))).getPath().replace('/', '\\');
    if (localPath.startsWith("\\") && !localPath.startsWith("\\\\"))
      localPath = localPath.substring(1); 
    if (this.cacheItemStamp.containsKey(localPath) && System.currentTimeMillis() > Long.parseLong(this.cacheItemStamp.getProperty(localPath, (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString()))) {
      this.cacheItemStamp.remove(localPath);
      this.cacheItem.remove(localPath);
    } 
    if (this.cacheItem.containsKey(localPath))
      return this.cacheItem.getProperty(localPath); 
    String item_privs = "";
    Properties acl = null;
    BufferedReader br = null;
    BufferedReader br2 = null;
    Process proc = null;
    OutputStream out = null;
    synchronized (acl_processes) {
      if (acl_processes.size() > 0) {
        acl = acl_processes.remove(0);
        br = (BufferedReader)acl.get("br");
        br2 = (BufferedReader)acl.get("br2");
        out = (OutputStream)acl.get("out");
        proc = (Process)acl.get("proc");
      } 
    } 
    if (acl == null) {
      String acl_lookup_tool = ServerStatus.SG("acl_lookup_tool");
      Common.debug(2, "ACL:" + acl_lookup_tool + ";" + localPath);
      proc = Runtime.getRuntime().exec(acl_lookup_tool.split(";"));
      out = proc.getOutputStream();
      br = new BufferedReader(new InputStreamReader(proc.getInputStream(), "UTF8"));
      br2 = new BufferedReader(new InputStreamReader(proc.getErrorStream(), "UTF8"));
      acl = new Properties();
      acl.put("proc", proc);
      acl.put("br", br);
      acl.put("br2", br2);
      acl.put("out", out);
    } 
    while (br2.ready())
      Log.log("SERVER", 2, String.valueOf(Thread.currentThread().getName()) + "ACL:ERROR:" + br2.readLine().trim()); 
    String data = "Q:v:\"" + acl_domain + "\":\"" + this.username + "\":\"" + localPath + "\"\r\n";
    Log.log("SERVER", 2, String.valueOf(Thread.currentThread().getName()) + "ACL:SEND:" + data.trim());
    out.write(data.getBytes("UTF8"));
    out.flush();
    data = "";
    try {
      while (br2.ready())
        Log.log("SERVER", 2, String.valueOf(Thread.currentThread().getName()) + "ACL:ERROR:" + br2.readLine().trim()); 
      String resultCode = br.readLine().trim();
      Log.log("SERVER", 2, String.valueOf(Thread.currentThread().getName()) + "ACL:ResultCode:" + resultCode.trim());
      if (resultCode.equals("0x0")) {
        data = br.readLine();
        Log.log("SERVER", 2, String.valueOf(Thread.currentThread().getName()) + "ACL:Lines:" + data.trim());
        int lines = Integer.parseInt(data.trim());
        for (int x = 0; x < lines; x++) {
          data = br.readLine().trim();
          Log.log("SERVER", 2, String.valueOf(Thread.currentThread().getName()) + "ACL:DATA:" + data.trim());
          String[] privs = data.split(":")[1].split("\\|");
          String path = data.substring(data.indexOf(":", data.indexOf(":") + 1) + 1).trim();
          if (path.startsWith("\""))
            path = path.substring(1, path.length() - 1); 
          String s = "";
          Log.log("SERVER", 2, String.valueOf(Thread.currentThread().getName()) + "ACL:PRIVS COUNT:" + privs.length);
          int read_sum = 0;
          int write_sum = 0;
          int full_sum = 0;
          for (int xx = 0; xx < privs.length; xx++) {
            String priv = privs[xx].trim();
            if (priv.equals("READ_CONTROL") || priv.equals("FILE_LIST_DIRECTORY") || priv.equals("FILE_READ_ATTRIBUTES") || priv.equals("FILE_READ_EA") || priv.equals("SYNCHRONIZE"))
              read_sum++; 
            if (priv.equals("READ_CONTROL") || priv.equals("FILE_ADD_FILE") || priv.equals("FILE_WRITE_ATTRIBUTES") || priv.equals("FILE_WRITE_EA") || priv.equals("FILE_ADD_SUBDIRECTORY") || priv.equals("SYNCHRONIZE"))
              write_sum++; 
            if (priv.equals("STANDARD_RIGHTS_REQUIRED") || priv.equals("SYNCHRONIZE"))
              full_sum++; 
            if (priv.equals("FILE_ADD_FILE"))
              s = String.valueOf(s) + ap(s, "(resume)") + ap(s, "(rename)") + ap(s, "(write)") + ap(s, "(makedir)"); 
            if (priv.equals("FILE_LIST_DIRECTORY"))
              s = String.valueOf(s) + ap(s, "(view)"); 
            if (priv.equals("GENERIC_READ"))
              s = String.valueOf(s) + ap(s, "(read)"); 
            if (priv.equals("GENERIC_WRITE"))
              s = String.valueOf(s) + ap(s, "(write)"); 
            if (priv.equals("FILE_ADD_SUBDIRECTORY"))
              s = String.valueOf(s) + ap(s, "(resume)") + ap(s, "(rename)") + ap(s, "(write)") + ap(s, "(makedir)"); 
            if (priv.equals("FILE_DELETE_CHILD"))
              s = String.valueOf(s) + ap(s, "(delete)") + ap(s, "(deletedir)"); 
            if (priv.equals("DELETE"))
              s = String.valueOf(s) + ap(s, "(delete)") + ap(s, "(deletedir)"); 
            if (priv.equals("FILE_ALL_ACCESS") || priv.equals("GENERIC_ALL"))
              s = String.valueOf(s) + ap(s, "(read)") + ap(s, "(rename)") + ap(s, "(resume)") + ap(s, "(view)") + ap(s, "(write)") + ap(s, "(makedir)") + ap(s, "(delete)") + ap(s, "(deletedir)") + ap(s, "(slideshow)"); 
          } 
          if (read_sum == 5)
            s = String.valueOf(s) + ap(s, "(read)"); 
          if (write_sum == 6)
            s = String.valueOf(s) + ap(s, "(write)"); 
          if (full_sum == 2)
            s = String.valueOf(s) + ap(s, "(read)") + ap(s, "(rename)") + ap(s, "(resume)") + ap(s, "(view)") + ap(s, "(write)") + ap(s, "(makedir)") + ap(s, "(delete)") + ap(s, "(deletedir)") + ap(s, "(slideshow)"); 
          if (s.equals(""))
            s = "(invisible)"; 
          item_privs = s;
          Log.log("SERVER", 2, String.valueOf(Thread.currentThread().getName()) + "ACL:PRIVS TOTAL:" + path + ":" + item_privs);
        } 
      } else {
        data = br.readLine();
        Log.log("SERVER", 2, String.valueOf(Thread.currentThread().getName()) + "ACL:Lines:" + data.trim());
        int lines = Integer.parseInt(data.trim());
        for (int x = 0; x < lines; x++) {
          data = br.readLine().trim();
          Log.log("SERVER", 2, String.valueOf(Thread.currentThread().getName()) + "ACL:DATA:" + data.trim());
        } 
      } 
    } catch (Exception e) {
      out.write("QUIT\r\n".getBytes());
      out.flush();
      br.readLine();
      acl.clear();
      br.close();
      br2.close();
      out.close();
      proc.destroy();
      acl = null;
    } finally {
      if (acl != null)
        acl_processes.addElement(acl); 
    } 
    this.cacheItem.put(localPath, item_privs);
    this.cacheItemStamp.put(localPath, (new StringBuffer(String.valueOf(System.currentTimeMillis() + (1000 * ServerStatus.IG("acl_cache_timeout"))))).toString());
    Common.debug(2, "ACL: priv:" + item_privs);
    return item_privs;
  }
  
  public String ap(String s1, String s2) {
    if (s1.indexOf(s2) < 0)
      return s2; 
    return "";
  }
  
  public String getPrivPath(String path) {
    return getPrivPath(path, false, true);
  }
  
  public String getPrivPath(String path, boolean getHomeIndex, boolean inheritQuota) {
    String priv = "";
    if (!path.startsWith("/"))
      path = "/" + path; 
    String originalPath = path;
    while (!path.equals("")) {
      for (int x = 0; x < this.permissions.size(); x++) {
        Properties permission = this.permissions.elementAt(x);
        if (permission.containsKey(UserTools.getUpper(path))) {
          priv = permission.getProperty(UserTools.getUpper(path), "");
          if (priv.indexOf("(real_quota)") < 0 || priv.indexOf("(quota-128974848)") < 0 || !inheritQuota) {
            if (!path.equals(originalPath))
              priv = String.valueOf(priv) + "(inherited)"; 
            if (getHomeIndex)
              return (new StringBuffer(String.valueOf(x))).toString(); 
            return path;
          } 
        } 
      } 
      path = Common.all_but_last(path);
    } 
    if (getHomeIndex)
      return "0"; 
    return path;
  }
  
  public void setPermissions(Properties dir_item) {
    String path = String.valueOf(dir_item.getProperty("root_dir", "/")) + dir_item.getProperty("name", "/");
    if (dir_item.getProperty("type", "DIR").equalsIgnoreCase("DIR"))
      path = String.valueOf(path) + "/"; 
    dir_item.put("is_virtual", (new StringBuffer(String.valueOf(isReal(path)))).toString());
    String privs = getPriv(path, dir_item);
    dir_item.put("privs", privs);
  }
  
  public Properties getPermission0() {
    return this.permissions.elementAt(0);
  }
  
  public Properties getCombinedPermissions() {
    Properties combined = new Properties();
    Properties tempVirtual2 = new Properties();
    for (int x = 0; x < this.permissions.size(); x++) {
      Properties tempVirtual = this.homes.elementAt(x);
      Properties perm = this.permissions.elementAt(x);
      Enumeration enumeration = perm.keys();
      while (enumeration.hasMoreElements()) {
        String key = enumeration.nextElement().toString();
        Properties p = (Properties)tempVirtual.get(key.toLowerCase().substring(0, key.length() - 1));
        if (perm.getProperty(key) != null) {
          if (p == null || p.containsKey("vItems")) {
            combined.put(key, perm.getProperty(key));
            continue;
          } 
          tempVirtual2.put(key, perm.getProperty(key));
        } 
      } 
    } 
    Enumeration keys = tempVirtual2.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      if (!combined.containsKey(key))
        combined.put(key, tempVirtual2.getProperty(key)); 
    } 
    return combined;
  }
  
  public Properties getCombinedVFS() {
    CIProperties combined = new CIProperties();
    for (int x = 0; x < this.homes.size(); x++)
      combined.putAll(this.homes.elementAt(x)); 
    return combined;
  }
  
  public String getRootVFS(String path, int homeItem) {
    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1); 
    if (!path.startsWith("/"))
      path = "/" + path; 
    Properties tempVirtual = null;
    if (homeItem >= 0) {
      tempVirtual = this.homes.elementAt(homeItem);
    } else {
      tempVirtual = getCombinedVFS();
    } 
    while (!tempVirtual.containsKey(path)) {
      path = Common.all_but_last(path);
      if (path.endsWith("/"))
        path = path.substring(0, path.length() - 1); 
      if (!path.startsWith("/"))
        path = "/" + path; 
      if (path.equals("/") && !tempVirtual.containsKey("/"))
        break; 
    } 
    return path;
  }
  
  public boolean isVFSFolder(String path) {
    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1); 
    if (!path.startsWith("/"))
      path = "/" + path; 
    if (path.equals("/"))
      return true; 
    Properties tempVirtual = getCombinedVFS();
    while (!tempVirtual.containsKey(path)) {
      path = Common.all_but_last(path);
      if (path.endsWith("/"))
        path = path.substring(0, path.length() - 1); 
      if (!path.startsWith("/"))
        path = "/" + path; 
      if (path.equals("/") && !tempVirtual.containsKey("/"))
        break; 
    } 
    if (path != null && !path.equals("") && !path.equals("/")) {
      Properties p = (Properties)tempVirtual.get(path);
      return p.getProperty("type", "DIR").equalsIgnoreCase("DIR");
    } 
    return false;
  }
  
  public String fixPath(String path) {
    path = Common.dots(path);
    if (path.toUpperCase().startsWith("FILE:") || path.indexOf(":") == 1 || path.indexOf(":") == 2) {
      path = Common.replace_str(path, ":\\", "/");
      path = Common.replace_str(path, ":/", "/");
    } 
    if (path.startsWith("/"))
      path = path.substring(1); 
    return path;
  }
  
  public static void doCopyVFS(VFS srcVFS, VFS destVFS) {
    if (srcVFS != null && destVFS != null && srcVFS.connectedVFSItems != null && srcVFS.connectedVFSItems.indexOf(destVFS) < 0) {
      destVFS.clientCacheUsed = srcVFS.clientCacheUsed;
      destVFS.clientCacheFree = srcVFS.clientCacheFree;
      destVFS.connectedVFSItems = srcVFS.connectedVFSItems;
      srcVFS.connectedVFSItems.addElement(destVFS);
      Log.log("VFS", 2, "Linking VFS's: " + srcVFS + " <--> " + destVFS + " total list of VFSs now:" + srcVFS.connectedVFSItems.toString());
    } 
  }
}
