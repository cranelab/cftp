package crushftp.server;

import com.crushftp.client.Common;
import com.crushftp.client.FileClient;
import com.crushftp.client.GenericClient;
import com.crushftp.client.GenericClientMulti;
import com.crushftp.client.HTTPClient;
import com.crushftp.client.S3CrushClient;
import com.crushftp.client.VRL;
import com.crushftp.client.Worker;
import crushftp.handlers.CIProperties;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.SessionCrush;
import crushftp.handlers.UserTools;
import java.io.BufferedReader;
import java.io.IOException;
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
  
  public Vector permissions = new Vector();
  
  public Properties cacheItem = new Properties();
  
  public Properties cacheItemDir = new Properties();
  
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
  
  public SessionCrush thisSession = null;
  
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
  
  public SessionCrush getServerSession() {
    return this.thisSession;
  }
  
  public GenericClient getClient(Properties item) throws Exception {
    if (this.homes.size() > 1 && ((Properties)this.homes.elementAt(0)).size() > 2 && this.user.getProperty("sync_vfs", "true").equals("true")) {
      String root_path = getRootVFS(String.valueOf(item.getProperty("root_dir")) + item.getProperty("name"), -1);
      int count = 0;
      for (int x = 0; x < this.homes.size(); x++) {
        String root_path2 = getRootVFS(String.valueOf(item.getProperty("root_dir")) + item.getProperty("name"), x);
        if (root_path.equals(root_path2))
          count++; 
      } 
      if (count > 1)
        return getClientMulti(item); 
    } 
    if (item.getProperty("privs", "").indexOf("(replicate)") >= 0)
      if (!ServerStatus.SG("replicated_vfs_url").equals("")) {
        GenericClient c = getClientMultiAll(item);
        if (item.getProperty("privs", "").indexOf("(ratio)") >= 0)
          c.setConfig("replicate_content", "false"); 
        return c;
      }  
    return getClientSingle(item);
  }
  
  public GenericClient getClientMultiAll(Properties item) throws Exception {
    if (ServerStatus.siIG("enterprise_level") <= 0)
      throw new Exception("The server does not have an enterprise license, so global replication is not allowed."); 
    Properties originalvItem = (Properties)item.get("vItem");
    Vector clients = new Vector();
    Vector vItems = new Vector();
    vItems.addElement(item.get("vItem"));
    clients.addElement(getClientSingle(item));
    addReplicatedVFSAndClient(item, vItems, clients, false);
    String logHeader = "PROXY:";
    if (this.thisSession != null)
      logHeader = "[" + this.thisSession.server_item.getProperty("serverType", "ftp") + ":" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] " + "PROXY" + " : "; 
    return new GenericClientMulti(logHeader, Common.log, originalvItem, vItems, clients, ServerStatus.BG("replicated_auto_play_journal"));
  }
  
  public void addReplicatedVFSAndClient(Properties item, Vector vItems, Vector clients, boolean fake) throws Exception {
    String get_item_path = String.valueOf(item.getProperty("root_dir")) + item.getProperty("name");
    Properties dir_item = new Properties();
    Properties vItem = new Properties();
    Properties original_vItem = (Properties)item.get("vItem");
    VRL root_vrl = new VRL(ServerStatus.SG("replicated_vfs_root_url"));
    String[] vrls = ServerStatus.SG("replicated_vfs_url").split(",");
    if (!ServerStatus.BG("multi_journal"))
      ServerStatus.server_settings.put("multi_journal", "true"); 
    System.getProperties().put("crushftp.replicated_vfs", "true");
    for (int x = 0; x < vrls.length; x++) {
      if (!vrls[x].trim().equals("")) {
        VRL vrl = new VRL(vrls[x].trim());
        Log.log("SERVER", 2, "Original VFS replicated URL:" + original_vItem.getProperty("url") + " versus root_vrl:" + root_vrl);
        String relative_path = (new VRL(original_vItem.getProperty("url"))).toString().substring(root_vrl.toString().length());
        vItem.put("url", String.valueOf(vrl.getProtocol()) + "://" + VRL.vrlEncode(ServerStatus.SG("replicated_vfs_user")) + ":" + VRL.vrlEncode(ServerStatus.thisObj.common_code.decode_pass(ServerStatus.SG("replicated_vfs_pass"))) + "@" + vrl.getHost() + ":" + vrl.getPort() + vrl.getPath() + relative_path);
        vItem.put("type", "DIR");
        dir_item.putAll(vItem);
        dir_item.put("vItem", vItem);
        dir_item.put("root_dir", Common.all_but_last(get_item_path));
        dir_item.put("name", Common.last(get_item_path));
        if (!fake)
          setPermissions(dir_item); 
        GenericClient c = getClientSingle(dir_item, false, true, 3000);
        c.setConfig("async", (new StringBuffer(String.valueOf((item.getProperty("privs", "").indexOf("(replicate_async)") >= 0)))).toString());
        clients.addElement(c);
        vItems.addElement(dir_item.get("vItem"));
      } 
    } 
  }
  
  public GenericClient getClientMulti(Properties item) throws Exception {
    Properties originalvItem = (Properties)item.get("vItem");
    String get_item_path = String.valueOf(item.getProperty("root_dir")) + item.getProperty("name");
    Vector clients = new Vector();
    Vector vItems = new Vector();
    for (int x = 0; this.homes.size() > 1 && x < this.homes.size(); x++) {
      Properties dir_item = null;
      try {
        dir_item = get_item(get_item_path, x);
      } catch (Exception e) {
        Log.log("SERVER", 1, e);
      } 
      if (dir_item == null) {
        String path = getRootVFS(get_item_path, x);
        if (path.equals("/"))
          continue; 
        Properties tempVirtual = this.homes.elementAt(x);
        Properties p = (Properties)tempVirtual.get(path);
        if (!p.containsKey("vItems"))
          continue; 
        Log.log("VFS", 3, "get_item:" + path);
        dir_item = new Properties();
        Properties vItem = vItemPick((Vector)p.get("vItems"));
        dir_item.putAll(vItem);
        dir_item.put("vItem", vItem);
        dir_item.put("root_dir", Common.all_but_last(get_item_path));
        dir_item.put("name", Common.last(get_item_path));
        setPermissions(dir_item);
      } 
      if (!(new VRL(dir_item.getProperty("url"))).getProtocol().equalsIgnoreCase("virtual")) {
        GenericClient c = getClientSingle(dir_item, false, true, 3000);
        clients.addElement(c);
        vItems.addElement(dir_item.get("vItem"));
      } 
      continue;
    } 
    String url_server = ServerStatus.SG("replicated_vfs_url");
    String url_client = item.getProperty("url");
    if (url_client.toUpperCase().startsWith("FILE:/") && !url_client.toUpperCase().startsWith("FILE://"))
      url_client = "file://" + url_client.substring("file:/".length()); 
    if (item.getProperty("privs", "").indexOf("(replicate)") >= 0 && !url_server.equals("") && url_client.toUpperCase().startsWith(url_server.toUpperCase()))
      addReplicatedVFSAndClient(item, vItems, clients, false); 
    String logHeader = "PROXY:";
    if (this.thisSession != null)
      logHeader = "[" + this.thisSession.server_item.getProperty("serverType", "ftp") + ":" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] " + "PROXY" + " : "; 
    return new GenericClientMulti(logHeader, Common.log, originalvItem, vItems, clients, ServerStatus.BG("replicated_auto_play_journal"));
  }
  
  public GenericClient getClientSingle(Properties item) throws Exception {
    return getClientSingle(item, false);
  }
  
  public GenericClient getClientSingle(Properties item, boolean override_encrypted_header_check) throws Exception {
    return getClientSingle(item, override_encrypted_header_check, false);
  }
  
  public GenericClient getClientSingle(Properties item, boolean override_encrypted_header_check, boolean allow_bad_item) throws Exception {
    return getClientSingle(item, override_encrypted_header_check, allow_bad_item, 30000);
  }
  
  public GenericClient getClientSingle(Properties item, boolean override_encrypted_header_check, boolean allow_bad_item, int timeout) throws Exception {
    boolean needLogin = false;
    GenericClient c = null;
    Vector vU = null;
    Vector vF = null;
    if (this.clientCacheFree == null) {
      this.clientCacheFree = new Properties();
      this.clientCacheUsed = new Properties();
    } 
    String baseURL = Common.getBaseUrl(item.getProperty("url"));
    synchronized (this.clientCacheFree) {
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
        if (c instanceof HTTPClient)
          c.setUrl(baseURL); 
      } else {
        needLogin = true;
        Log.log("VFS", 2, "Create new GenericClient to url:" + (new VRL(baseURL)).safe());
        String logHeader = "PROXY:";
        if (this.thisSession != null)
          logHeader = "[" + this.thisSession.server_item.getProperty("serverType", "ftp") + ":" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] " + "PROXY" + " : "; 
        c = Common.getClient(baseURL, logHeader, Common.log);
        if (c == null)
          Log.log("SERVER", 0, "Error creating client for:" + item); 
      } 
      c.setConfig("timeout", (new StringBuffer(String.valueOf(timeout))).toString());
      c.setConfig("clientid", (this.thisSession == null) ? null : this.thisSession.uiSG("clientid"));
      if (baseURL.startsWith("s3")) {
        VRL v_tmp = new VRL(item.getProperty("url"));
        String tmp_path = v_tmp.getPath();
        if (tmp_path.length() > 1)
          tmp_path = tmp_path.substring(1, tmp_path.indexOf("/", 1)); 
        c.setConfig("clientid", tmp_path);
      } 
      c.setConfig("item", item);
      c.setConfig("s3_buffer", (new StringBuffer(String.valueOf(ServerStatus.IG("s3_buffer")))).toString());
      c.setConfig("dmz_stat_caching", (new StringBuffer(String.valueOf(ServerStatus.BG("dmz_stat_caching")))).toString());
      c.setConfig("s3_max_buffer_download", (new StringBuffer(String.valueOf(ServerStatus.IG("s3_max_buffer_download")))).toString());
      c.setConfig("s3_threads_upload", (new StringBuffer(String.valueOf(ServerStatus.IG("s3_threads_upload")))).toString());
      c.setConfig("s3_threads_download", (new StringBuffer(String.valueOf(ServerStatus.IG("s3_threads_download")))).toString());
      c.setConfig("baseURL", baseURL);
      c.setConfig("vfs_user", this.username);
      c.setConfig("disabled_ciphers", ServerStatus.SG("disabled_ciphers"));
      if (c instanceof S3CrushClient)
        ((S3CrushClient)c).resetBucket(); 
      c.setConfig("SAMLResponse", "");
      if (this.thisSession != null) {
        c.setConfig("SAMLResponse", this.thisSession.user_info.getProperty("SAMLResponse", ""));
        c.setConfig("proxy_user_ip", this.thisSession.user_info.getProperty("user_ip"));
        c.setConfig("proxy_user_port", this.thisSession.user_info.getProperty("user_port"));
        c.setConfig("proxy_user_protocol", this.thisSession.user_info.getProperty("user_protocol"));
        c.setConfig("proxy_bind_ip", this.thisSession.user_info.getProperty("bind_ip"));
        c.setConfig("proxy_bind_port", this.thisSession.user_info.getProperty("bind_port"));
        c.setConfig("proxy_header_user-agent", this.thisSession.user_info.getProperty("header_user-agent"));
        if (this.thisSession.user_info.getProperty("user_protocol").equalsIgnoreCase("SFTP") || this.thisSession.user_info.getProperty("user_protocol").toUpperCase().startsWith("FTP")) {
          c.setConfig("dmz_stat_caching", (new StringBuffer(String.valueOf(ServerStatus.BG("dmz_stat_caching")))).toString());
        } else {
          c.setConfig("dmz_stat_caching", "false");
        } 
      } 
      if (ServerStatus.BG("count_dir_items"))
        c.setConfig("count_dir_items", "true"); 
      if (this.thisSession != null && this.thisSession.BG("dir_calc"))
        c.setConfig("count_dir_size", "true"); 
      Properties vItem = (Properties)item.get("vItem");
      if (vItem != null) {
        Enumeration enumeration = vItem.keys();
        while (enumeration.hasMoreElements()) {
          String key = enumeration.nextElement().toString();
          c.setConfig(key, vItem.get(key));
          if (key.equals("ssh_private_key_pass"))
            c.setConfig(key, ServerStatus.thisObj.common_code.decode_pass(vItem.getProperty(key))); 
        } 
      } 
      Enumeration keys = item.keys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement().toString();
        c.setConfig(key, item.get(key));
        if (key.equals("ssh_private_key_pass"))
          c.setConfig(key, ServerStatus.thisObj.common_code.decode_pass(item.getProperty(key))); 
      } 
      if (c instanceof com.crushftp.client.AS2Client)
        c.setConfig("uVFS", this); 
      c.setConfig("pgpDecryptUpload", null);
      c.setConfig("pgpPrivateKeyUploadPath", null);
      c.setConfig("pgpPrivateKeyUploadPassword", null);
      c.setConfig("pgpEncryptUpload", null);
      c.setConfig("pgpAsciiUpload", null);
      c.setConfig("pgpPublicKeyUploadPath", null);
      c.setConfig("pgpDecryptDownload", null);
      c.setConfig("pgpPrivateKeyDownloadPath", null);
      c.setConfig("pgpPrivateKeyDownloadPassword", null);
      c.setConfig("pgpEncryptDownload", null);
      c.setConfig("pgpAsciiDownload", null);
      c.setConfig("pgpPublicKeyDownloadPath", null);
      c.setConfig("syncName", null);
      c.setConfig("syncRevisionsPath", null);
      c.setConfig("syncUploadOnly", null);
      String privs = item.getProperty("privs", "");
      if (!override_encrypted_header_check && (privs.indexOf("(pgpDecryptDownload=true)") >= 0 || (this.thisSession != null && !this.thisSession.user.getProperty("filePublicEncryptionKey", "").equals("")) || ServerStatus.BG("fileEncryption"))) {
        c.setConfig("checkEncryptedHeader", "true");
      } else {
        c.setConfig("checkEncryptedHeader", "false");
      } 
      for (int x = 0; x < (privs.split("\\(")).length; x++) {
        String priv = privs.split("\\(")[x];
        if (!priv.equals("")) {
          priv = priv.substring(0, priv.length() - 1).trim();
          if (priv.indexOf("=") >= 0)
            c.setConfig(priv.split("=")[0], priv.substring(priv.indexOf("=") + 1)); 
        } 
      } 
      if (privs.indexOf("(resume)") < 0)
        c.setConfig("s3_partial", "false"); 
    } 
    boolean needLoginAuth = true;
    if (needLogin)
      for (int x = 0; x < vU.size() && needLoginAuth; x++) {
        GenericClient c2 = vU.elementAt(x);
        if (c2 instanceof HTTPClient && c2.getConfig("baseURL", "").equals(baseURL) && !c2.getConfig("crushAuth", "").equals("")) {
          c.setConfig("crushAuth", c2.getConfig("crushAuth", ""));
          c.setConfig("clientid", c2.getConfig("clientid"));
          c.setConfig("username", c2.getConfig("username"));
          c.setConfig("password", c2.getConfig("password"));
          c.setConfig("SAMLResponse", c2.getConfig("SAMLResponse"));
          String result = ((HTTPClient)c).doAction("getCrushAuth", null, null);
          if (!c2.getConfig("crushAuth", "").equals("") && result.indexOf(c2.getConfig("crushAuth", "")) >= 0)
            needLoginAuth = false; 
        } 
      }  
    if (needLogin) {
      VRL vrl = new VRL(item.getProperty("url"));
      if (Common.dmz_mode)
        c.setCache(this.cacheList); 
      try {
        c.setConfig("bad_login", "false");
        if (needLoginAuth)
          c.login(vrl.getUsername(), vrl.getPassword(), (String)c.getConfig("clientid")); 
      } catch (Exception e) {
        Log.log("SERVER", 1, e);
        if (!allow_bad_item || System.getProperty("crushftp.isTestCall", "false").equals("true"))
          throw e; 
        c.setConfig("bad_login", "true");
      } 
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
    synchronized (this.clientCacheFree) {
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
        if (c instanceof FileClient)
          ((FileClient)c).freeCache(); 
        vU.remove(c);
        if (c.getConfig("error", "").equals(""))
          vF.addElement(c); 
      } 
    } 
    return null;
  }
  
  public void setUserPassIpPortProtocol(String username, String password, String ip, int port, String protocol, Properties user_info, Properties user, SessionCrush thisSession) {
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
    for (int x = 0; x < tempVFS.homes.size(); x++) {
      Properties temp_virtual = tempVFS.homes.get(x);
      Properties temp_items = new Properties();
      Enumeration e_temp = temp_virtual.propertyNames();
      while (e_temp.hasMoreElements()) {
        String key_temp = (String)e_temp.nextElement();
        if (key_temp.equals("/") || key_temp.equals("vfs_permissions_object"))
          continue; 
        temp_items.put(key_temp, temp_virtual.get(key_temp));
      } 
      if (!temp_items.isEmpty()) {
        Enumeration e_temp_items = temp_items.propertyNames();
        while (e_temp_items.hasMoreElements()) {
          String key_temp_items = (String)e_temp_items.nextElement();
          String temp_item_url = get_url_of_vfs_item((Properties)temp_items.get(key_temp_items));
          if (!temp_item_url.equals(""))
            for (int xx = 0; xx < this.homes.size(); xx++) {
              Properties this_virtual = this.homes.get(xx);
              Enumeration e = this_virtual.propertyNames();
              while (e.hasMoreElements()) {
                String key = (String)e.nextElement();
                if (!key.equals("/") && !key.equals("vfs_permissions_object") && 
                  get_url_of_vfs_item((Properties)this_virtual.get(key)).equals(temp_item_url))
                  temp_virtual.remove(key_temp_items); 
              } 
            }  
        } 
      } 
      if (temp_virtual.size() != 2 || !temp_virtual.containsKey("/") || !temp_virtual.containsKey("vfs_permissions_object")) {
        this.homes.add(temp_virtual);
        this.permissions.add(tempVFS.permissions.get(x));
      } 
    } 
  }
  
  private String get_url_of_vfs_item(Properties p) {
    String url = "";
    if (p.containsKey("vItems")) {
      Vector vItems = (Vector)p.get("vItems");
      if (vItems != null && !vItems.isEmpty()) {
        Properties pp = vItems.get(0);
        url = pp.getProperty("url");
      } 
    } 
    return url;
  }
  
  public void reset() {
    this.cacheItem.clear();
    this.cacheItemStamp.clear();
    this.cacheItemDir.clear();
  }
  
  public void resetAll() {
    quotaCache.clear();
  }
  
  public boolean isReal(String path) {
    path = fixPath(path);
    return isVFSFolder(path);
  }
  
  public void free() {}
  
  public void disconnect() {
    if (this.clientCacheFree == null)
      return; 
    synchronized (this.clientCacheFree) {
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
    Properties combined_perms = getCombinedPermissions();
    for (int x = 0; x < this.homes.size(); x++) {
      if (homeItem >= 0)
        x = homeItem; 
      Properties tempVirtual = this.homes.elementAt(x);
      path = getRootVFS(pathOriginal, x);
      Properties p = (Properties)tempVirtual.get(path);
      Log.log("VFS", 3, "get_item:" + path);
      if (p != null && p.getProperty("type", "DIR").equalsIgnoreCase("FILE")) {
        Properties vItem = null;
        try {
          vItem = vItemPick((Vector)p.get("vItems"));
        } catch (Exception e) {
          Log.log("SERVER", 0, "Invalid VFS item config:" + path + ":" + p);
          Log.log("SERVER", 1, e);
          throw e;
        } 
        GenericClient c = null;
        try {
          try {
            if (combined_perms != null) {
              String priv = null;
              if (priv == null)
                priv = combined_perms.getProperty(path.toUpperCase()); 
              if (priv == null)
                priv = combined_perms.getProperty(String.valueOf(path.toUpperCase()) + "/"); 
              if (priv != null && !vItem.containsKey("privs"))
                vItem.put("privs", priv); 
            } 
            c = getClientSingle(vItem, false);
          } catch (Exception e) {
            Log.log("SERVER", 1, e);
            if ((x > 0 && homeItem >= 0) || System.getProperty("crushftp.isTestCall", "false").equals("true"))
              throw e; 
            if (Common.dmz_mode && path.equals("/internal"))
              throw e; 
            if (x > 0)
              continue; 
          } 
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
            Date d = new Date(0L);
            dir_item.put("modified", (new StringBuffer(String.valueOf(d.getTime()))).toString());
            dir_item.put("month", this.month.format(d));
            dir_item.put("day", this.day.format(d));
            dir_item.put("time_or_year", this.year.format(d));
          } else {
            if (tempPath.startsWith("/") && vItem.getProperty("url").endsWith("/"))
              tempPath = tempPath.substring(1); 
            String s = vItem.getProperty("url");
            if (s.indexOf("%") >= 0 && s.equals(Common.url_decode(s)))
              s = s.replaceAll("%", "%25"); 
            if (c.getConfig("bad_login") == null || c.getConfig("bad_login").equals("false"))
              dir_item = c.stat((new VRL(String.valueOf(s) + Common.url_encode(tempPath))).getPath()); 
            if (dir_item != null && tempPath.equals(""))
              dir_item.put("name", p.getProperty("name")); 
          } 
        } finally {}
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
      continue;
    } 
    return dir_item;
  }
  
  public void getListing(Vector list, String path2) throws Exception {
    if (path2.equals(""))
      path2 = "/"; 
    String path = path2;
    Properties status = new Properties();
    int x;
    for (x = 0; x < this.homes.size(); x++) {
      Properties tempVirtual = this.homes.elementAt(x);
      String parentPath = getRootVFS(path, x);
      Properties dir_item_tmp = null;
      try {
        dir_item_tmp = get_item(parentPath, x);
      } catch (Exception e) {
        if (x == 0 || System.getProperty("crushftp.isTestCall", "false").equals("true"))
          throw e; 
        Log.log("SERVER", 1, e);
      } 
      if (dir_item_tmp != null) {
        Properties dir_item = dir_item_tmp;
        Vector list2 = new Vector();
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
                      GenericClient c = getClientSingle(actualItem, true);
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
                    } else if (avrl.getProtocol().equalsIgnoreCase("SMB")) {
                      GenericClient c = null;
                      try {
                        if (actualItem.getProperty("url", "").indexOf("%user") >= 0 || actualItem.getProperty("url", "").indexOf("{user") >= 0)
                          actualItem.put("url", updateUrlVariables(actualItem.getProperty("url"))); 
                        c = getClientSingle(actualItem, true);
                      } catch (Exception exception) {}
                      if (c != null)
                        try {
                          c.stat(avrl.getPath());
                        } catch (Exception e) {
                          if (avrl.getUsername().indexOf("{username") < 0 && avrl.getUsername().indexOf("%username") < 0)
                            item.put("hide_smb", "true"); 
                          Log.log("SERVER", 1, e);
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
          status.put("status" + x, "DONE");
        } else {
          int x_loop = x;
          Runnable r = new Runnable(this, dir_item, path, parentPath, list2, status, x_loop) {
              final VFS this$0;
              
              private final Properties val$dir_item;
              
              private final String val$path;
              
              private final String val$parentPath;
              
              private final Vector val$list2;
              
              private final Properties val$status;
              
              private final int val$x_loop;
              
              public void run() {
                try {
                  GenericClient c = this.this$0.getClientSingle(this.val$dir_item);
                  try {
                    String urlpath = (new VRL(this.val$dir_item.getProperty("url"))).getPath();
                    if (urlpath.endsWith("/"))
                      urlpath = urlpath.substring(0, urlpath.length() - 1); 
                    c.list(String.valueOf(urlpath) + this.val$path.substring(this.val$parentPath.length()), this.val$list2);
                  } catch (Exception e) {
                    c.setConfig("error", e.toString());
                    this.val$status.put("error" + this.val$x_loop, e);
                    Log.log("SERVER", 2, e);
                  } finally {
                    c = this.this$0.releaseClient(c);
                  } 
                } catch (Exception e) {
                  Common.debug(1, e);
                } 
                this.val$status.put("status" + this.val$x_loop, "DONE");
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
        while (list2.size() > 0 || (!status.containsKey("status" + x) && !status.containsKey("error" + x))) {
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
      } 
    } 
    if (list.size() == 0)
      for (x = 0; x < this.homes.size(); x++) {
        if (status.containsKey("error" + x))
          throw (Exception)status.get("error" + x); 
      }  
    String oversize_msg = "";
    if (ServerStatus.LG("max_items_dir") > 0L && list.size() > ServerStatus.LG("max_items_dir")) {
      oversize_msg = "Directory size too big:" + list.size() + ":" + path2 + ":" + Thread.currentThread().getName();
      Common.do_sort(list, "modified", "modified");
      while (list.size() > ServerStatus.LG("max_items_dir"))
        list.removeElementAt(0); 
    } 
    if (!oversize_msg.equals("")) {
      Properties info = new Properties();
      info.put("alert_timeout", "0");
      info.put("alert_max", "0");
      info.put("alert_msg", oversize_msg);
      ServerStatus.thisObj.runAlerts("big_dir", info, this.user_info, null);
      Log.log("SERVER", 0, oversize_msg);
      System.gc();
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
      appendListing(path, list, "", depth, maxSize, includeFolders, filters, retr, item.get("vItem"), null);
    } 
  }
  
  public void appendListing(String path, Vector list, String dir, int depth, int maxSize, boolean includeFolders, Vector filters, RETR_handler retr, Object vItem) throws Exception {
    appendListing(path, list, dir, depth, maxSize, includeFolders, filters, retr, vItem, null);
  }
  
  public void appendListing(String path, Vector list, String dir, int depth, int maxSize, boolean includeFolders, Vector filters, RETR_handler retr, Object vItem, Properties cur_item) throws Exception {
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
          if (retr != null && !retr.active2.getProperty("active", "").equals("true"))
            return; 
          if (retr == null && loops++ > 6000)
            return; 
        } 
        Properties item = aDir.elementAt(x);
        if (vItem != null)
          item.put("vItem", vItem); 
        if (item.getProperty("type", "").equalsIgnoreCase("FILE")) {
          list.addElement(item);
        } else if (item.getProperty("type", "").equalsIgnoreCase("DIR")) {
          appendListing(path, list, String.valueOf(dir) + item.getProperty("name") + "/", depth, maxSize, includeFolders, filters, retr, vItem, item);
        } 
        if (Thread.currentThread().getName().indexOf(":lister:") >= 0)
          Thread.currentThread().setName(String.valueOf(Thread.currentThread().getName().substring(0, Thread.currentThread().getName().indexOf(":lister:") + ":lister:".length())) + dir + item.getProperty("name")); 
      } 
    } 
    try {
      if (aDir.size() == 0 || includeFolders)
        if (cur_item != null) {
          list.addElement(cur_item);
        } else {
          list.addElement(get_item(String.valueOf(path) + dir));
        }  
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
    dir_item.put("privs", getPriv(path, dir_item, true));
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
    p.put("url", updateUrlVariables(p.getProperty("url", "")));
    return p;
  }
  
  public String updateUrlVariables(String url) {
    url = Common.replace_str(url, "%username%", Common.url_encode(this.username, ""));
    url = Common.replace_str(url, "%user_name%", Common.url_encode(this.username, ""));
    url = Common.replace_str(url, "%password%", Common.url_encode(this.password, ""));
    url = Common.replace_str(url, "%user_pass%", Common.url_encode(this.password, ""));
    try {
      if (this.ip.equals(""))
        this.ip = "0.0.0.0"; 
      url = Common.replace_str(url, "%ip%", this.ip);
      if (this.ip.indexOf(":") < 0) {
        url = Common.replace_str(url, "%ip1%", this.ip.split("\\.")[0]);
        url = Common.replace_str(url, "%ip2%", this.ip.split("\\.")[1]);
        url = Common.replace_str(url, "%ip3%", this.ip.split("\\.")[2]);
        url = Common.replace_str(url, "%ip4%", this.ip.split("\\.")[3]);
      } 
      if ((this.user_info.getProperty("bind_ip", "0.0.0.0").split("\\.")).length > 2) {
        url = Common.replace_str(url, "%bind_ip1%", this.user_info.getProperty("bind_ip", "0.0.0.0").split("\\.")[0]);
        url = Common.replace_str(url, "%bind_ip2%", this.user_info.getProperty("bind_ip", "0.0.0.0").split("\\.")[1]);
        url = Common.replace_str(url, "%bind_ip3%", this.user_info.getProperty("bind_ip", "0.0.0.0").split("\\.")[2]);
        url = Common.replace_str(url, "%bind_ip4%", this.user_info.getProperty("bind_ip", "0.0.0.0").split("\\.")[3]);
      } 
      url = Common.replace_str(url, "%port%", (new StringBuffer(String.valueOf(this.port))).toString());
      url = Common.replace_str(url, "%protocol%", this.protocol);
      url = Common.replace_str(url, "{username}", Common.url_encode(this.username, ""));
      url = Common.replace_str(url, "{user_name}", Common.url_encode(this.username, ""));
      url = Common.replace_str(url, "{vusername}", VRL.vrlEncode(this.username));
      url = Common.replace_str(url, "{vuser_name}", VRL.vrlEncode(this.username));
      if (this.username.indexOf("@") >= 0)
        url = Common.replace_str(url, "{rusername}", this.username.substring(0, this.username.indexOf("@"))); 
      if (this.username.indexOf("@") >= 0)
        url = Common.replace_str(url, "{ruser_name}", this.username.substring(0, this.username.indexOf("@"))); 
      url = Common.replace_str(url, "{password}", Common.url_encode(this.password, ""));
      url = Common.replace_str(url, "{user_pass}", Common.url_encode(this.password, ""));
      url = Common.replace_str(url, "{vpassword}", VRL.vrlEncode(this.password));
      url = Common.replace_str(url, "{vuser_pass}", VRL.vrlEncode(this.password));
      url = Common.replace_str(url, "{ip}", this.ip);
      if (this.ip.indexOf(":") < 0) {
        url = Common.replace_str(url, "{ip1}", this.ip.split("\\.")[0]);
        url = Common.replace_str(url, "{ip2}", this.ip.split("\\.")[1]);
        url = Common.replace_str(url, "{ip3}", this.ip.split("\\.")[2]);
        url = Common.replace_str(url, "{ip4}", this.ip.split("\\.")[3]);
      } 
      if ((this.user_info.getProperty("bind_ip", "0.0.0.0").split("\\.")).length > 2) {
        url = Common.replace_str(url, "{bind_ip1}", this.user_info.getProperty("bind_ip", "0.0.0.0").split("\\.")[0]);
        url = Common.replace_str(url, "{bind_ip2}", this.user_info.getProperty("bind_ip", "0.0.0.0").split("\\.")[1]);
        url = Common.replace_str(url, "{bind_ip3}", this.user_info.getProperty("bind_ip", "0.0.0.0").split("\\.")[2]);
        url = Common.replace_str(url, "{bind_ip4}", this.user_info.getProperty("bind_ip", "0.0.0.0").split("\\.")[3]);
      } 
    } catch (Exception e) {
      Log.log("SERVER", 2, e);
    } 
    url = Common.replace_str(url, "{port}", (new StringBuffer(String.valueOf(this.port))).toString());
    url = Common.replace_str(url, "{protocol}", this.protocol);
    if (this.thisSession != null)
      url = ServerStatus.change_vars_to_values_static(url, this.thisSession.user, this.thisSession.user_info, this.thisSession); 
    url = Common.replace_str(url, "\\\\", "").replace('\\', '/');
    return url;
  }
  
  public String getPriv(String path, Properties dir_item) {
    return getPriv(path, dir_item, false);
  }
  
  public String getPriv(String path, Properties dir_item, boolean parent) {
    if (!path.startsWith("/"))
      path = "/" + path; 
    String originalPath = path;
    Properties permission = getCombinedPermissions();
    path = originalPath;
    String priv2 = getPriv2(originalPath, permission);
    boolean aclPermissions = permission.getProperty("acl_permissions", "false").equals("true");
    if (Common.dmz_mode)
      aclPermissions = false; 
    Properties dir_item2 = dir_item;
    if (dir_item.getProperty("type", "DIR").equalsIgnoreCase("FILE")) {
      dir_item2 = (Properties)dir_item.clone();
      dir_item2.put("url", Common.all_but_last(dir_item2.getProperty("url")));
    } 
    if (aclPermissions && (new VRL(dir_item.getProperty("url"))).getProtocol().equalsIgnoreCase("virtual"))
      try {
        Properties acl_item = null;
        if (dir_item.getProperty("is_virtual", "false").equals("false"))
          acl_item = get_item(originalPath); 
        if (acl_item != null)
          return acl_item.getProperty("privs"); 
      } catch (Exception e) {
        Log.log("SERVER", 1, e);
      }  
    if (aclPermissions && (new VRL(dir_item.getProperty("url"))).getProtocol().equalsIgnoreCase("file"))
      try {
        String priv = "";
        if (ServerStatus.SG("acl_mode").equals("1")) {
          priv = getAcl(dir_item2, (Vector)permission.get("acl_group_memberships"));
        } else if (ServerStatus.SG("acl_mode").equals("2") || parent) {
          priv = getAcl2(dir_item2, permission.getProperty("acl_domain", ""));
        } else if (ServerStatus.SG("acl_mode").equals("3") && !parent) {
          priv = getAcl3(dir_item2, permission.getProperty("acl_domain", ""));
        } 
        if (priv.indexOf("(slideshow)") < 0)
          priv = String.valueOf(priv) + "(slideshow)"; 
        if (priv2.indexOf("(share)") >= 0)
          priv = String.valueOf(priv) + "(share)"; 
        if (priv2.indexOf("(quota") >= 0)
          priv = String.valueOf(priv) + priv2.substring(priv2.indexOf("(quota"), priv2.indexOf(")", priv2.indexOf("(quota"))) + ")"; 
        if (priv2.indexOf("(real_quota)") >= 0)
          priv = String.valueOf(priv) + "(real_quota)"; 
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
        if (priv.indexOf("(slideshow)") < 0)
          priv = String.valueOf(priv) + "(slideshow)"; 
        if (priv2.indexOf("(share)") >= 0)
          priv = String.valueOf(priv) + "(share)"; 
        if (priv2.indexOf("(quota") >= 0)
          priv = String.valueOf(priv) + priv2.substring(priv2.indexOf("(quota"), priv2.indexOf(")", priv2.indexOf("(quota"))) + ")"; 
        if (priv2.indexOf("(real_quota)") >= 0)
          priv = String.valueOf(priv) + "(real_quota)"; 
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
    if (this.cacheItemStamp.containsKey(localPath.toLowerCase()) && System.currentTimeMillis() > Long.parseLong(this.cacheItemStamp.getProperty(localPath.toLowerCase(), (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString()))) {
      this.cacheItemStamp.remove(localPath.toLowerCase());
      this.cacheItem.remove(localPath.toLowerCase());
      this.cacheItemDir.remove(localPath.toLowerCase());
    } 
    if (this.cacheItem.containsKey(localPath.toLowerCase()))
      return this.cacheItem.getProperty(localPath.toLowerCase()); 
    Common.debug(2, "ACL: memberships:" + memberships);
    String item_privs = "";
    Common.debug(2, "ACL: accesschk.exe;-accepteula;-dqvu;" + localPath);
    Common.check_exec();
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
    this.cacheItem.put(localPath.toLowerCase(), item_privs);
    this.cacheItemStamp.put(localPath.toLowerCase(), (new StringBuffer(String.valueOf(System.currentTimeMillis() + (1000 * ServerStatus.IG("acl_cache_timeout"))))).toString());
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
    if (Common.dmz_mode) {
      Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
      Properties action = new Properties();
      action.put("type", "GET:ACL");
      action.put("id", Common.makeBoundary());
      action.put("acl_domain", acl_domain);
      action.put("dir_item", dir_item);
      action.put("username", this.username);
      action.put("localPath", localPath);
      action.put("need_response", "true");
      queue.addElement(action);
      action = UserTools.waitResponse(action, 30);
      if (action != null && action.containsKey("item_privs"))
        item_privs = action.getProperty("item_privs", ""); 
    } else {
      item_privs = getAcl2Proc(dir_item, acl_domain, localPath, this.username);
    } 
    this.cacheItem.put(localPath, item_privs);
    this.cacheItemStamp.put(localPath, (new StringBuffer(String.valueOf(System.currentTimeMillis() + (1000 * ServerStatus.IG("acl_cache_timeout"))))).toString());
    Common.debug(2, "ACL: priv:" + item_privs);
    return item_privs;
  }
  
  public static String getAcl2Proc(Properties dir_item, String acl_domain, String localPath, String username) throws IOException {
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
      Common.check_exec();
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
    String data = "Q:v:\"" + acl_domain + "\":\"" + username + "\":\"" + localPath + "\"\r\n";
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
              s = String.valueOf(s) + ap(s, "(read)") + ap(s, "(slideshow)"); 
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
    return item_privs;
  }
  
  public String getAcl3(Properties dir_item, String acl_domain) throws Exception {
    Common.debug(2, "ACL:url:" + dir_item.getProperty("url"));
    String localPath = (new VRL(dir_item.getProperty("url"))).getPath().replace('/', '\\');
    if (localPath.startsWith("\\") && !localPath.startsWith("\\\\"))
      localPath = localPath.substring(1); 
    if (this.cacheItemStamp.containsKey(localPath.toLowerCase()) && System.currentTimeMillis() > Long.parseLong(this.cacheItemStamp.getProperty(localPath.toLowerCase(), (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString()))) {
      this.cacheItemStamp.remove(localPath.toLowerCase());
      this.cacheItem.remove(localPath.toLowerCase());
      this.cacheItemDir.remove(localPath.toLowerCase());
    } 
    if (this.cacheItem.containsKey(localPath.toLowerCase()))
      return this.cacheItem.getProperty(localPath.toLowerCase()); 
    String localPath2 = localPath.substring(0, localPath.lastIndexOf("\\", localPath.length() - 2) + 1);
    if (this.cacheItemDir.containsKey(localPath.toLowerCase())) {
      Properties diffs = (Properties)this.cacheItemDir.get(localPath.toLowerCase());
      return diffs.getProperty(localPath.toLowerCase());
    } 
    if (this.cacheItemDir.containsKey(localPath2.toLowerCase())) {
      Properties diffs = (Properties)this.cacheItemDir.get(localPath2.toLowerCase());
      if (diffs.containsKey(localPath.toLowerCase()))
        return diffs.getProperty(localPath.toLowerCase()); 
      return diffs.getProperty(localPath2.toLowerCase());
    } 
    String item_privs = "";
    if (Common.dmz_mode) {
      Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
      Properties action = new Properties();
      action.put("type", "GET:ACL");
      action.put("id", Common.makeBoundary());
      action.put("acl_domain", acl_domain);
      action.put("dir_item", dir_item);
      action.put("username", this.username);
      action.put("localPath", localPath);
      action.put("need_response", "true");
      queue.addElement(action);
      action = UserTools.waitResponse(action, 30);
      if (action != null && action.containsKey("item_privs"))
        item_privs = action.getProperty("item_privs", ""); 
    } else {
      Properties diffs = new Properties();
      item_privs = getAcl3Proc(dir_item, acl_domain, localPath, this.username, diffs);
      if (diffs.size() > 0) {
        this.cacheItemDir.put(localPath2.toLowerCase(), diffs);
        this.cacheItemStamp.put(localPath2.toLowerCase(), (new StringBuffer(String.valueOf(System.currentTimeMillis() + (1000 * ServerStatus.IG("acl_cache_timeout"))))).toString());
      } 
    } 
    this.cacheItem.put(localPath.toLowerCase(), item_privs);
    this.cacheItemStamp.put(localPath.toLowerCase(), (new StringBuffer(String.valueOf(System.currentTimeMillis() + (1000 * ServerStatus.IG("acl_cache_timeout"))))).toString());
    Common.debug(2, "ACL: priv:" + item_privs);
    return item_privs;
  }
  
  public static String getAcl3Proc(Properties dir_item, String acl_domain, String localPath, String username, Properties diffs) throws IOException {
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
      Common.check_exec();
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
    String localPath2 = localPath.substring(0, localPath.lastIndexOf("\\", localPath.length() - 2) + 1);
    String data = "Q:v,R=1:\"" + acl_domain + "\":\"" + username + "\":\"" + localPath2 + "\"\r\n";
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
              s = String.valueOf(s) + ap(s, "(read)") + ap(s, "(slideshow)"); 
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
      } else if (resultCode.equals("0x2000000EA")) {
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
              s = String.valueOf(s) + ap(s, "(read)") + ap(s, "(slideshow)"); 
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
          diffs.put(String.valueOf(path.toLowerCase()) + "\\", s);
          Log.log("SERVER", 2, String.valueOf(Thread.currentThread().getName()) + "ACL:PRIVS TOTAL:" + path + ":" + item_privs);
        } 
        if (diffs.containsKey(localPath.toLowerCase())) {
          item_privs = diffs.getProperty(localPath.toLowerCase());
        } else {
          item_privs = diffs.getProperty(localPath2.toLowerCase());
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
    return item_privs;
  }
  
  public static String ap(String s1, String s2) {
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
}
