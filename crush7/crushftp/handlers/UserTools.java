package crushftp.handlers;

import com.crushftp.client.Base64;
import com.crushftp.client.Common;
import com.crushftp.client.VRL;
import com.crushftp.client.Worker;
import crushftp.gui.LOC;
import crushftp.server.ServerStatus;
import crushftp.server.VFS;
import crushftp.user.SQLUsers;
import crushftp.user.UserProvider;
import crushftp.user.XMLUsers;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class UserTools {
  public static Vector anyPassTokens = new Vector();
  
  public static Properties anyPassTokensTime = new Properties();
  
  public static String db_class = "XML";
  
  public static UserTools ut = new UserTools();
  
  public static Object userExpirePasswordLock = new Object();
  
  private static UserProvider up = new XMLUsers();
  
  public static SimpleDateFormat expire_vfs = new SimpleDateFormat("MM/dd/yyyy hh:mm aa", Locale.US);
  
  public static Object user_lock = new Object();
  
  static Class class$0;
  
  static {
    String token = Common.makeBoundary(20);
    anyPassTokens.addElement(token);
    anyPassTokensTime.put(token, (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
  }
  
  public UserTools() {
    initUserProvider();
  }
  
  public void forceMemoryReload(String username) {
    for (int x = ServerStatus.siVG("user_list").size() - 1; x >= 0; x--) {
      try {
        SessionCrush thisSession = (SessionCrush)((Properties)ServerStatus.siVG("user_list").elementAt(x)).get("session");
        Properties user = ut.getUser(thisSession.uiSG("listen_ip_port"), thisSession.uiSG("user_name"), true);
        VFS uVFS = ut.getVFS(thisSession.uiSG("listen_ip_port"), thisSession.uiSG("user_name"));
        if (thisSession.user != null && user != null) {
          String root_dir = thisSession.user.getProperty("root_dir");
          thisSession.user.putAll(user);
          thisSession.user.put("root_dir", root_dir);
          if (thisSession.uVFS != null && thisSession.uVFS.username.equalsIgnoreCase(thisSession.uiSG("user_name"))) {
            thisSession.uVFS.homes = uVFS.homes;
            thisSession.uVFS.permissions = uVFS.permissions;
            setupVFSLinking(thisSession.uiSG("listen_ip_port"), thisSession.uiSG("user_name"), thisSession.uVFS, thisSession.user);
          } 
          uVFS.disconnect();
          uVFS.free();
        } 
      } catch (Exception e) {
        Log.log("SERVER", 1, e);
      } 
    } 
  }
  
  public void forceMemoryReloadUser(String username) {
    for (int x = ServerStatus.siVG("user_list").size() - 1; x >= 0; x--) {
      try {
        SessionCrush thisSession = (SessionCrush)((Properties)ServerStatus.siVG("user_list").elementAt(x)).get("session");
        if (thisSession.uiSG("user_name").equalsIgnoreCase(username)) {
          Properties user = ut.getUser(thisSession.uiSG("listen_ip_port"), thisSession.uiSG("user_name"), true);
          VFS uVFS = ut.getVFS(thisSession.uiSG("listen_ip_port"), thisSession.uiSG("user_name"));
          if (thisSession.user != null && user != null) {
            String root_dir = thisSession.user.getProperty("root_dir");
            thisSession.user.putAll(user);
            thisSession.user.put("root_dir", root_dir);
            if (thisSession.uVFS != null && thisSession.uVFS.username.equalsIgnoreCase(thisSession.uiSG("user_name"))) {
              thisSession.uVFS.homes = uVFS.homes;
              thisSession.uVFS.permissions = uVFS.permissions;
              setupVFSLinking(thisSession.uiSG("listen_ip_port"), thisSession.uiSG("user_name"), thisSession.uVFS, thisSession.user);
            } 
            uVFS.disconnect();
            uVFS.free();
          } 
        } 
      } catch (Exception e) {
        Log.log("SERVER", 1, e);
      } 
    } 
  }
  
  public static void setupVFSLinking(String serverGroup, String username, VFS uVFS, Properties user) {
    if (uVFS == null || uVFS.homes == null)
      return; 
    Properties virtual = uVFS.homes.elementAt(0);
    if (!virtual.containsKey("/")) {
      Properties pp = new Properties();
      pp.put("virtualPath", "/");
      pp.put("name", "VFS");
      pp.put("type", "DIR");
      pp.put("vItems", new Vector());
      virtual.put("/", pp);
    } 
    Properties permissions = ((Vector)virtual.get("vfs_permissions_object")).elementAt(0);
    if (!permissions.containsKey("/"))
      permissions.put("/", "(read)(view)(resume)"); 
    getExtraVFS(serverGroup, username, uVFS, user);
    Log.log("LOGIN", 2, "Getting linked VFSs: " + user.get("linked_vfs"));
    if (user.get("linked_vfs") instanceof String) {
      String[] s = user.get("linked_vfs").toString().split(",");
      Vector v = new Vector();
      for (int i = 0; i < s.length; i++) {
        if (!s[i].trim().equals(""))
          v.addElement(s[i].trim()); 
      } 
      user.put("linked_vfs", v);
    } 
    Vector linked_vfs = (Vector)user.get("linked_vfs");
    if (linked_vfs == null)
      linked_vfs = new Vector(); 
    Vector alreadyAdded = new Vector();
    alreadyAdded.addElement(uVFS.user_info.getProperty("vfs_username", ""));
    for (int x = 0; x < linked_vfs.size(); x++) {
      if (!linked_vfs.elementAt(x).toString().trim().equals(""))
        try {
          if (alreadyAdded.indexOf(linked_vfs.elementAt(x).toString()) < 0) {
            VFS tempVFS = ut.getVFS(serverGroup, linked_vfs.elementAt(x).toString());
            uVFS.addLinkedVFS(tempVFS);
            alreadyAdded.addElement(linked_vfs.elementAt(x).toString());
            Properties tempUser = ut.getUser(serverGroup, linked_vfs.elementAt(x).toString(), false);
            getExtraVFS(serverGroup, linked_vfs.elementAt(x).toString(), uVFS, tempUser);
          } 
        } catch (Exception e) {
          Log.log("LOGIN", 1, e);
        }  
    } 
    if (!ServerStatus.thisObj.server_info.getProperty("enterprise_level", "0").equals("0") && user.getProperty("allow_user_shares", "").equals("true"))
      try {
        Properties tempUser = ut.getUser(serverGroup, String.valueOf(username) + ".SHARED", false);
        if (tempUser != null && !tempUser.getProperty("username", "").equalsIgnoreCase("TEMPLATE")) {
          VFS tempVFS = ut.getVFS(serverGroup, String.valueOf(username) + ".SHARED");
          if (tempVFS != null)
            uVFS.addLinkedVFS(tempVFS); 
        } 
      } catch (Exception e) {
        Log.log("LOGIN", 1, e);
      }  
  }
  
  public static void getExtraVFS(String serverGroup, String username, VFS uVFS, Properties user) {
    Log.log("LOGIN", 2, "Getting extra VFSs: " + ((user == null) ? "null" : (String)user.get("extra_vfs")));
    String inheritedUsername = ut.getEndUsernameVFS(serverGroup, username);
    Vector allUsers = new Vector();
    refreshUserList("extra_vfs", allUsers);
    Vector extra_vfs = new Vector();
    for (int x = 0; x < allUsers.size(); x++) {
      if (allUsers.elementAt(x).toString().toLowerCase().startsWith(String.valueOf(inheritedUsername.toLowerCase()) + "~"))
        extra_vfs.addElement(allUsers.elementAt(x).toString().substring((String.valueOf(inheritedUsername.toLowerCase()) + "~").length())); 
    } 
    Object[] evi = extra_vfs.toArray();
    Arrays.sort(evi);
    extra_vfs.removeAllElements();
    int i;
    for (i = 0; i < evi.length; i++)
      extra_vfs.addElement(evi[i]); 
    if (extra_vfs.size() > 0)
      user.put("extra_vfs", extra_vfs); 
    if (uVFS != null)
      for (i = 0; i < extra_vfs.size(); i++) {
        if (!extra_vfs.elementAt(i).toString().trim().equals(""))
          try {
            VFS tempVFS = ut.getVFS("extra_vfs", String.valueOf(inheritedUsername) + "~" + extra_vfs.elementAt(i).toString());
            uVFS.addLinkedVFS(tempVFS);
          } catch (Exception e) {
            Log.log("LOGIN", 1, e);
          }  
      }  
  }
  
  public VFS getVFS(String serverGroup, String username) {
    String inheritedUsername = getEndUsernameVFS(serverGroup, username);
    Properties virtual = up.buildVFS(serverGroup, inheritedUsername);
    Enumeration keys = virtual.keys();
    boolean needWrite = false;
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      if (key.equals("vfs_permissions_object"))
        continue; 
      Properties vp = (Properties)virtual.get(key);
      Vector v = (Vector)vp.get("vItems");
      for (int x = 0; v != null && x < v.size(); x++) {
        Properties p = v.elementAt(x);
        if (!p.getProperty("expires_on", "").trim().equals(""))
          try {
            long expire = expire_vfs.parse(p.getProperty("expires_on", "0")).getTime();
            if (expire < System.currentTimeMillis() && expire > 0L) {
              virtual.remove(key);
              Properties permission = ((Vector)virtual.get("vfs_permissions_object")).elementAt(0);
              permission.remove(key.toUpperCase());
              needWrite = true;
            } 
          } catch (ParseException e) {
            Log.log("SERVER", 1, e);
          }  
      } 
    } 
    if (needWrite)
      writeVFS(serverGroup, username, virtual); 
    VFS tempVFS = VFS.getVFS(virtual);
    tempVFS.user_info.put("vfs_username", inheritedUsername);
    return tempVFS;
  }
  
  public Properties getVirtualVFS(String serverGroup, String username) {
    return up.buildVFS(serverGroup, username);
  }
  
  private String getEndUsernameVFS(String serverGroup, String username) {
    String vfs_user = "";
    Properties inheritance = up.loadInheritance(serverGroup);
    Enumeration keys = inheritance.keys();
    Vector ichain = null;
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      if (key.equalsIgnoreCase(username)) {
        ichain = (Vector)inheritance.get(key);
        break;
      } 
    } 
    if (ichain != null) {
      Properties user = (Properties)Common.CLONE(up.loadUser(serverGroup, username, inheritance, false));
      vfs_user = username;
      if (user != null && !user.containsKey("root_dir"))
        try {
          for (int x = ichain.size() - 1; x >= 0; x--) {
            Properties p = up.loadUser(serverGroup, ichain.elementAt(x).toString(), inheritance, false);
            if (p.containsKey("root_dir") && !user.containsKey("root_dir")) {
              vfs_user = ichain.elementAt(x).toString();
              break;
            } 
          } 
        } catch (Exception e) {
          Log.log("USER_OBJ", 1, e);
        }  
    } 
    if (vfs_user.equals(""))
      vfs_user = username; 
    return vfs_user;
  }
  
  public Properties getUser(String serverGroup, String username, boolean flattenUser, boolean getVfsNotUserVar) {
    return getUser(serverGroup, username, flattenUser);
  }
  
  public Properties getUser(String serverGroup, String username, boolean flattenUser) {
    if (username.equals(""))
      return null; 
    initUserProvider();
    Properties inheritance2 = getInheritance(serverGroup);
    Properties defaults = null;
    synchronized (user_lock) {
      defaults = up.loadUser(serverGroup, "default", inheritance2, true);
      if (defaults == null || !defaults.getProperty("defaultsVersion", "").equals(String.valueOf(ServerStatus.version_info_str) + ServerStatus.sub_version_info_str)) {
        if (class$0 == null)
          try {
          
          } catch (ClassNotFoundException classNotFoundException) {
            throw new NoClassDefFoundError(null.getMessage());
          }  
        Properties p = (Properties)Common.readXMLObject((class$0 = Class.forName("crushftp.server.VFS")).getResource("/assets/default_user.xml"));
        if (defaults == null)
          defaults = p; 
        p.putAll(defaults);
        p.put("defaultsVersion", String.valueOf(ServerStatus.version_info_str) + ServerStatus.sub_version_info_str);
        try {
          p.put("password", "MD5:" + Common.getMD5(new ByteArrayInputStream(Common.makeBoundary().getBytes())));
          writeUser(serverGroup, "default", p);
        } catch (Exception e) {
          Common.debug(2, e);
        } 
        defaults = p;
      } 
    } 
    Properties theUser = up.loadUser(serverGroup, username, inheritance2, flattenUser);
    if (theUser == null && !username.equals("template"))
      theUser = getUser(serverGroup, "template", true); 
    if (theUser != null) {
      Vector events = (Vector)theUser.get("events");
      if (events != null)
        for (int x = 0; x < events.size(); x++) {
          Properties p = events.elementAt(x);
          if (!p.getProperty("linkUser", "").equals("") && flattenUser) {
            Properties pp = getLinkedEvent(serverGroup, p);
            p.putAll(pp);
          } 
        }  
    } 
    return theUser;
  }
  
  public Properties getLinkedEvent(String serverGroup, Properties event) {
    Properties saver = event;
    try {
      String linkUser = event.getProperty("linkUser", "");
      String linkEvent = event.getProperty("linkEvent", "");
      if (!linkUser.equals("")) {
        Properties the_user = getUser(serverGroup, linkUser, false);
        Vector copy_event_list = (Vector)the_user.get("events");
        for (int x = 0; x < copy_event_list.size(); x++) {
          Properties p = copy_event_list.elementAt(x);
          if (p.getProperty("name").equals(linkEvent)) {
            saver = p;
            break;
          } 
        } 
        saver.put("linkUser", linkUser);
        saver.put("linkEvent", linkEvent);
      } 
      if (saver == null)
        saver = event; 
    } catch (Exception exception) {}
    return saver;
  }
  
  public String getEndUserProperty(String serverGroup, String username, String key, String defaultValue) {
    try {
      Properties the_user = null;
      try {
        the_user = getUser(serverGroup, username, false);
      } catch (Exception e) {
        Log.log("USER_OBJ", 3, e);
      } 
      return the_user.getProperty(key, defaultValue);
    } catch (Exception e) {
      Log.log("USER_OBJ", 1, e);
      return defaultValue;
    } 
  }
  
  public synchronized void put_in_user(String serverGroup, String username, String key, String val, boolean backup, boolean replicate) {
    try {
      Properties the_user = getUser(serverGroup, username, false);
      if (the_user.getProperty("username", "").equalsIgnoreCase("template") && !username.equalsIgnoreCase("template"))
        return; 
      the_user.put(key, val);
      writeUser(serverGroup, username, the_user, replicate, backup);
    } catch (Exception exception) {}
    synchronized (Common.xmlCache) {
      Common.xmlCache.clear();
    } 
  }
  
  public static synchronized void purgeOldBackups(int maxCount) {
    try {
      Log.log("USER_OBJ", 0, "UserBackupPurge:Looking for old user folders to delete from here:" + System.getProperty("crushftp.backup") + "backup/");
      (new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/")).mkdirs();
      File[] folders = (new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/")).listFiles();
      Log.log("USER_OBJ", 0, "UserBackupPurge:Folder has " + folders.length + " items.");
      if (folders.length >= maxCount) {
        Vector validItems = new Vector();
        Arrays.sort(folders, new null.counts());
        for (int x = 0; x < folders.length; x++) {
          File f = folders[x];
          if (f.isDirectory() && f.getName().toUpperCase().indexOf("USERS-") >= 0) {
            validItems.addElement(f);
          } else if (f.isFile() && f.getName().endsWith(".zip")) {
            validItems.addElement(f);
          } 
        } 
        Log.log("USER_OBJ", 0, "UserBackupPurge:Folder has " + validItems.size() + " user items, max is " + maxCount + ".");
        while (validItems.size() > 0 && validItems.size() >= maxCount) {
          File f = validItems.elementAt(0);
          Log.log("USER_OBJ", 0, "UserBackupPurge:Purging old backup users from:" + f.getAbsolutePath());
          Common.recurseDelete(f.getAbsolutePath(), false);
          validItems.removeElementAt(0);
        } 
      } 
    } catch (Exception e) {
      Log.log("USER_OBJ", 0, e);
    } 
  }
  
  public static void expireUserVFSTask(Properties user, String serverGroup, String username) throws Exception {
    if (!user.getProperty("account_expire_task", "").equals("")) {
      VFS tempVFS = ut.getVFS(serverGroup, username);
      Vector items = new Vector();
      Vector folderItems = new Vector();
      Vector foundItems = new Vector();
      tempVFS.appendListing("/", foundItems, "", 99, 10000, true, null, null, null);
      tempVFS.disconnect();
      tempVFS.free();
      while (foundItems.size() > 0) {
        Properties item = foundItems.remove(0);
        VRL vrl = new VRL(item.getProperty("url"));
        if (vrl.getProtocol().equalsIgnoreCase("virtual"))
          continue; 
        item.put("the_file_name", item.getProperty("name"));
        item.put("the_file_path", String.valueOf(item.getProperty("root_dir")) + item.getProperty("name"));
        item.put("the_file_size", item.getProperty("size", "0"));
        if (item.getProperty("type", "").equalsIgnoreCase("DIR")) {
          folderItems.addElement(item);
          continue;
        } 
        items.addElement(item);
      } 
      while (folderItems.size() > 0)
        items.insertElementAt(folderItems.remove(0), 0); 
      Log.log("USER_OBJ", 0, "Executing CrushTask " + user.getProperty("account_expire_task", "") + " with VFS items:" + items.size());
      Properties event = new Properties();
      event.put("event_plugin_list", user.getProperty("account_expire_task", ""));
      event.put("name", "ExpiringUser:" + username + ":" + user.getProperty("account_expire", ""));
      Properties info = new Properties();
      info.put("user", user);
      info.put("user_info", user);
      ServerStatus.thisObj.events6.doEventPlugin(info, event, null, items);
    } 
  }
  
  public static Properties getGroups(String serverGroup) {
    return up.loadGroups(serverGroup);
  }
  
  public static void writeGroups(String serverGroup, Properties groups) {
    writeGroups(serverGroup, groups, true);
  }
  
  public static void writeGroups(String serverGroup, Properties groups, boolean replicate) {
    up.writeGroups(serverGroup, groups);
    Properties p = new Properties();
    p.put("serverGroup", serverGroup);
    p.put("groups", groups);
    if (replicate)
      SharedSessionReplicated.send(Common.makeBoundary(), "crushftp.handlers.writeGroups", "info", p); 
  }
  
  public static void refreshUserList(String serverGroup, Vector current_user_group_listing) {
    current_user_group_listing.removeAllElements();
    Vector listing = up.loadUserList(serverGroup);
    current_user_group_listing.addAll(listing);
    if (current_user_group_listing.indexOf("default") < 0)
      synchronized (user_lock) {
        try {
          up.writeUser(serverGroup, "default", ut.getUser(serverGroup, "default", false), false);
          current_user_group_listing.addElement("default");
        } catch (Exception e) {
          Log.log("USER_OBJ", 0, e);
        } 
      }  
    boolean fixedOne = false;
    Properties inheritance = up.loadInheritance(serverGroup);
    Enumeration keys = inheritance.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      Vector ichain = (Vector)inheritance.get(key);
      for (int xx = 0; xx < ichain.size(); xx++) {
        String tempUser = ichain.elementAt(xx).toString();
        for (int x = 0; x < listing.size(); x++) {
          String username = listing.elementAt(x).toString();
          if (tempUser.equalsIgnoreCase(username))
            if (!tempUser.equals(username)) {
              ichain.setElementAt((E)username, xx);
              fixedOne = true;
            }  
        } 
      } 
    } 
    if (fixedOne)
      writeInheritance(serverGroup, inheritance); 
    fixedOne = false;
    Properties groups = up.loadGroups(serverGroup);
    keys = groups.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      Vector group = (Vector)groups.get(key);
      for (int xx = 0; xx < group.size(); xx++) {
        String tempUser = group.elementAt(xx).toString();
        for (int x = 0; x < listing.size(); x++) {
          String username = listing.elementAt(x).toString();
          if (tempUser.equalsIgnoreCase(username))
            if (!tempUser.equals(username)) {
              group.setElementAt((E)username, xx);
              fixedOne = true;
            }  
        } 
      } 
    } 
    if (fixedOne)
      writeGroups(serverGroup, groups); 
  }
  
  public static Vector findUserEmail(String serverGroup, String email) {
    return up.findUserEmail(serverGroup, email);
  }
  
  public static Properties generateEmptyVirtual() {
    Properties virtual = new Properties();
    Vector v = new Vector();
    Properties p = new Properties();
    p.put("/", "(read)(view)(resume)");
    v.addElement(p);
    virtual.put("vfs_permissions_object", v);
    return virtual;
  }
  
  public static Properties getInheritance(String serverGroup) {
    return up.loadInheritance(serverGroup);
  }
  
  public static void changeUsername(String serverGroup, String username1, String username2, String password) {
    up.updateUser(serverGroup, username1, username2, password);
  }
  
  public static void writeUser(String serverGroup, String username, Properties user) {
    writeUser(serverGroup, username, user, true, true);
  }
  
  public static void writeUser(String serverGroup, String username, Properties user, boolean replicate, boolean backup) {
    synchronized (user_lock) {
      user.remove("extra_vfs");
      up.writeUser(serverGroup, username, user, backup);
    } 
    Properties p = new Properties();
    p.put("serverGroup", serverGroup);
    p.put("username", username);
    p.put("user", user);
    p.put("backup", (new StringBuffer(String.valueOf(backup))).toString());
    if (replicate)
      SharedSessionReplicated.send(Common.makeBoundary(), "crushftp.handlers.writeUser", "info", p); 
  }
  
  public static void stripKeys(Properties tempUser, String theUsername, Vector users, Properties inheritance, Properties groups, Properties defaults) {
    Enumeration keys = tempUser.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      if (!(tempUser.get(key) instanceof String))
        continue; 
      String val = tempUser.get(key).toString();
      if (val.indexOf("@") >= 0 && !key.equalsIgnoreCase("email") && !key.startsWith("x_")) {
        String parentUsername = val.substring(val.lastIndexOf("@") + 1);
        if (parentUsername.length() < 40 && !parentUsername.trim().equals("") && !parentUsername.equals(theUsername)) {
          val = val.substring(0, val.lastIndexOf("@"));
          if (users.indexOf(parentUsername) >= 0) {
            Vector inheritanceUser = (Vector)inheritance.get(theUsername);
            if (inheritanceUser == null)
              inheritanceUser = new Vector(); 
            inheritance.put(theUsername, inheritanceUser);
            if (inheritanceUser.indexOf(parentUsername) < 0)
              inheritanceUser.addElement(parentUsername); 
            if (!groups.containsKey(parentUsername))
              groups.put(parentUsername, new Vector()); 
            Vector v = (Vector)groups.get(parentUsername);
            if (v.indexOf(theUsername) < 0)
              v.addElement(theUsername); 
          } 
          tempUser.remove(key);
        } 
      } 
      if (defaults.get(key) != null && defaults.get(key).toString().equals(val))
        tempUser.remove(key); 
    } 
  }
  
  public static void writeInheritance(String serverGroup, Properties inheritance) {
    writeInheritance(serverGroup, inheritance, true);
  }
  
  public static void writeInheritance(String serverGroup, Properties inheritance, boolean replicate) {
    up.writeInheritance(serverGroup, inheritance);
    Properties p = new Properties();
    p.put("serverGroup", serverGroup);
    p.put("inheritance", inheritance);
    if (replicate)
      SharedSessionReplicated.send(Common.makeBoundary(), "crushftp.handlers.writeInheritance", "info", p); 
  }
  
  public static void deleteUser(String serverGroup, String username) {
    deleteUser(serverGroup, username, true);
  }
  
  public static void deleteUser(String serverGroup, String username, boolean replicate) {
    up.deleteUser(serverGroup, username);
    Properties p = new Properties();
    p.put("serverGroup", serverGroup);
    p.put("username", username);
    if (replicate)
      SharedSessionReplicated.send(Common.makeBoundary(), "crushftp.handlers.deleteUser", "info", p); 
  }
  
  public static String convertUsers(boolean allUsers, Vector users, String serverGroup, String username) {
    Properties inheritance = getInheritance(serverGroup);
    Properties groups = getGroups(serverGroup);
    Properties defaults = up.loadUser(serverGroup, "default", inheritance, true);
    if (allUsers) {
      for (int x = 0; x < users.size(); x++) {
        username = users.elementAt(x).toString();
        if (!username.equalsIgnoreCase("default")) {
          Log.log("USER_OBJ", 0, "Converting user..." + username);
          try {
            File f = new File(get_real_path_to_user(serverGroup, username));
            if (!groups.containsKey(f.getParentFile().getName()))
              groups.put(f.getParentFile().getName(), new Vector()); 
            Vector v = (Vector)groups.get(f.getParentFile().getName());
            if (v.indexOf(username) < 0)
              v.addElement(username); 
            f.renameTo(new File(String.valueOf(System.getProperty("crushftp.users")) + "/" + serverGroup + "/" + username));
          } catch (Exception e) {
            Log.log("USER_OBJ", 0, e);
          } 
          Properties properties = ut.getUser(serverGroup, username, false);
          if (properties != null) {
            boolean bool = (properties.getProperty("root_dir", "").indexOf("@") >= 0);
            stripKeys(properties, username, users, inheritance, groups, defaults);
            writeInheritance(serverGroup, inheritance);
            String str = ServerStatus.thisObj.common_code.decode_pass(properties.getProperty("password", ""));
            str = ServerStatus.thisObj.common_code.encode_pass(str, ServerStatus.SG("password_encryption"), "");
            properties.put("password", str);
            if (!bool)
              properties.put("root_dir", "/"); 
            properties.put("userVersion", "6");
            writeUser(serverGroup, username, properties);
            Log.log("USER_OBJ", 0, "Converted user:" + username);
          } 
        } 
      } 
      writeGroups(serverGroup, groups);
      return "Users have been updated.\r\nYou will only see GUI items that this user is overriding from the default username.\r\nClick the 'Show All' button to make other changes.\r\n";
    } 
    if (username.equalsIgnoreCase("default"))
      return "default user cannot be converted"; 
    Properties tempUser = ut.getUser(serverGroup, username, false);
    boolean inheritVFS = (tempUser.getProperty("root_dir", "").indexOf("@") >= 0);
    stripKeys(tempUser, username, users, inheritance, groups, defaults);
    writeInheritance(serverGroup, inheritance);
    if (!inheritVFS)
      tempUser.put("root_dir", "/"); 
    tempUser.put("userVersion", "6");
    String the_password = ServerStatus.thisObj.common_code.decode_pass(tempUser.getProperty("password", ""));
    the_password = ServerStatus.thisObj.common_code.encode_pass(the_password, ServerStatus.SG("password_encryption"), "");
    tempUser.put("password", the_password);
    writeUser(serverGroup, username, tempUser);
    writeGroups(serverGroup, groups);
    return "User has been updated.";
  }
  
  public static void initUserProvider() {
    if (ServerStatus.server_settings.getProperty("externalSqlUsers", "").equals("true") && ServerStatus.server_settings.getProperty("xmlUsers", "").equals("true"))
      ServerStatus.server_settings.put("externalSqlUsers", "false"); 
    if (ServerStatus.server_settings.getProperty("externalSqlUsers", "").equals("true"))
      db_class = "SQL"; 
    if (ServerStatus.server_settings.getProperty("xmlUsers", "true").equals("true")) {
      db_class = "XML";
    } else if (ServerStatus.server_settings.getProperty("xmlUsers", "").equals("false")) {
      db_class = "SQL";
    } else {
      db_class = ServerStatus.server_settings.getProperty("xmlUsers", "").trim();
    } 
    if (db_class.equalsIgnoreCase("XML") || db_class.equals("")) {
      if (up == null || !(up instanceof XMLUsers))
        up = new XMLUsers(); 
    } else if (db_class.equals("SQL")) {
      if (up == null || !(up instanceof SQLUsers)) {
        up = new SQLUsers();
        Properties sqlItems = (Properties)ServerStatus.server_settings.get("sqlItems");
        sqlItems.remove("debug");
        sqlItems.remove("read_only");
        ((SQLUsers)up).setSettings(sqlItems);
        ServerStatus.thisObj.save_server_settings(true);
      } 
    } else {
      try {
        Class c = ServerStatus.clasLoader.loadClass(db_class);
        Constructor cons = c.getConstructor(new Class[] { (new Properties()).getClass(), (new String()).getClass() });
        up = (UserProvider)cons.newInstance(null);
      } catch (Exception e) {
        Log.log("SERVER", 0, "Can't load class:" + db_class);
        Log.log("SERVER", 0, e);
      } 
    } 
  }
  
  public static String convertUsersSQLXML(String fromMode, String toMode, String serverGroup) {
    String msg = "";
    if (serverGroup == null || serverGroup.equals("CANCELLED"))
      return "Cancelled"; 
    initUserProvider();
    UserProvider up1 = new XMLUsers();
    UserProvider up2 = new XMLUsers();
    if (fromMode.equals("SQL")) {
      up1 = new SQLUsers();
      ((SQLUsers)up1).setSettings((Properties)ServerStatus.server_settings.get("sqlItems"));
    } 
    if (toMode.equals("SQL")) {
      up2 = new SQLUsers();
      ((SQLUsers)up2).setSettings((Properties)ServerStatus.server_settings.get("sqlItems"));
    } 
    Vector user_list = up.loadUserList(serverGroup);
    Properties inheritance = up1.loadInheritance(serverGroup);
    Properties defaults = up1.loadUser(serverGroup, "default", inheritance, true);
    msg = String.valueOf(msg) + "Converted " + inheritance.size() + " Inheritance rules.\r\n";
    for (int x = 0; x < user_list.size(); x++) {
      String username = user_list.elementAt(x).toString();
      Properties user = up1.loadUser(serverGroup, username, inheritance, false);
      if (user != null) {
        VFS uVFS = VFS.getVFS(up1.buildVFS(serverGroup, username));
        if (!username.equals("default"))
          stripUser(user, defaults); 
        user.put("userVersion", "6");
        if (user.containsKey("first_name"))
          user.put("email", user.getProperty("email", "")); 
        up2.writeUser(serverGroup, username, user, true);
        up2.writeVFS(serverGroup, username, uVFS.homes.elementAt(0));
      } 
    } 
    msg = String.valueOf(msg) + "Converted " + user_list.size() + " users.\r\n";
    up2.writeInheritance(serverGroup, inheritance);
    up2.writeGroups(serverGroup, up1.loadGroups(serverGroup));
    return msg;
  }
  
  public static void stripUser(Properties user, Properties defaults) {
    Enumeration keys = user.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      if (user.get(key) instanceof String)
        if (!key.equals("root_dir") && 
          defaults.getProperty(key, "").equals(user.getProperty(key)))
          user.remove(key);  
    } 
  }
  
  public static Properties getAllowedUsers(String admin_username, String serverGroup, Vector list) {
    Properties info = new Properties();
    Vector allowed_users = new Vector();
    boolean defaultUserEditable = false;
    Properties groups = getGroups(serverGroup);
    Vector groupUsers = (Vector)groups.get(admin_username);
    if (groupUsers == null)
      groupUsers = new Vector(); 
    for (int x = 0; x < groupUsers.size(); x++)
      groupUsers.setElementAt(groupUsers.elementAt(x).toString(), x); 
    Properties inheritance = getInheritance(serverGroup);
    Properties inheritance2 = new Properties();
    for (int i = 0; i < list.size(); i++) {
      String username = list.elementAt(i).toString();
      if (groupUsers.indexOf(username) >= 0) {
        allowed_users.addElement(username);
        if (username.equals("default"))
          defaultUserEditable = true; 
        if (inheritance.containsKey(username))
          inheritance2.put(username, inheritance.get(username)); 
      } 
    } 
    if (!defaultUserEditable)
      allowed_users.addElement("default"); 
    info.put("default_edittable", (new StringBuffer(String.valueOf(defaultUserEditable))).toString());
    info.put("list", allowed_users);
    info.put("inheritance", inheritance2);
    return info;
  }
  
  public static void mergeWebCustomizations(Properties newUser, Properties user) {
    if (newUser.containsKey("web_customizations") && user.containsKey("web_customizations")) {
      Vector newUser_v = (Vector)newUser.get("web_customizations");
      Vector tempUser_v = (Vector)user.get("web_customizations");
      for (int xx = 0; xx < tempUser_v.size(); xx++) {
        boolean found = false;
        Properties tempUser_p = tempUser_v.elementAt(xx);
        for (int xxx = 0; xxx < newUser_v.size(); xxx++) {
          Properties newUser_p = newUser_v.elementAt(xxx);
          if (newUser_p.getProperty("key").equals(tempUser_p.getProperty("key"))) {
            found = true;
            newUser_v.setElementAt(tempUser_p, xxx);
            break;
          } 
        } 
        if (!found)
          newUser_v.addElement(tempUser_p); 
      } 
      user.remove("web_customizations");
    } 
  }
  
  public static void mergeLinkedVFS(Properties newUser, Properties user) {
    if (newUser.containsKey("linked_vfs") && user.containsKey("linked_vfs")) {
      Vector newUser_v = (Vector)newUser.get("linked_vfs");
      Vector tempUser_v = (Vector)user.get("linked_vfs");
      for (int xx = 0; xx < tempUser_v.size(); xx++) {
        if (newUser_v.indexOf(tempUser_v.elementAt(xx)) < 0)
          newUser_v.addElement(tempUser_v.elementAt(xx)); 
      } 
      user.remove("linked_vfs");
    } 
  }
  
  public Properties verify_user(ServerStatus server_status_frame, String the_user, String the_password, String listen_ip_port, int user_number, String user_ip, int user_port, Properties server_item, Properties loginReason) {
    return verify_user(server_status_frame, the_user, the_password, listen_ip_port, user_number, user_ip, user_port, server_item, loginReason, false);
  }
  
  public Properties verify_user(ServerStatus server_status_frame, String the_user, String the_password, String serverGroup, int user_number, String user_ip, int user_port, Properties server_item, Properties loginReason, boolean anyPass) {
    if (the_user.indexOf("\\") >= 0)
      the_user = the_user.substring(the_user.indexOf("\\") + 1); 
    if (the_password.startsWith("SHA:") || the_password.startsWith("SHA512:") || the_password.startsWith("SHA3:") || the_password.startsWith("MD5:") || the_password.startsWith("MD5S2:") || the_password.startsWith("CRYPT3:") || the_password.startsWith("BCRYPT:") || the_password.startsWith("MD5CRYPT:") || the_password.startsWith("SHA512CRYPT:"))
      return null; 
    Properties user = null;
    if (!ServerStatus.BG("blank_passwords") && the_password.trim().equals("") && !anyPass && !the_user.equalsIgnoreCase("ANONYMOUS"))
      return null; 
    try {
      user = ut.getUser(serverGroup, the_user, true);
    } catch (Exception e) {
      Log.log("USER_OBJ", 2, e);
    } 
    if (user != null) {
      if (!user.getProperty("account_expire_rolling_days", "0").equals("") && Integer.parseInt(user.getProperty("account_expire_rolling_days", "0")) < 0) {
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(new Date());
        int days = Integer.parseInt(user.getProperty("account_expire_rolling_days"));
        days *= -1;
        gc.add(5, days);
        put_in_user(serverGroup, the_user, "account_expire_rolling_days", (new StringBuffer(String.valueOf(days))).toString(), true, true);
        put_in_user(serverGroup, the_user, "account_expire", (new SimpleDateFormat("MM/dd/yy hh:mm aa", Locale.US)).format(gc.getTime()), true, true);
        user = ut.getUser(serverGroup, the_user, true);
      } 
      if (!user.getProperty("expire_password_days", "0").equals("") && Integer.parseInt(user.getProperty("expire_password_days", "0")) < 0) {
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(new Date());
        int days = Integer.parseInt(user.getProperty("expire_password_days"));
        days *= -1;
        gc.add(5, days);
        put_in_user(serverGroup, the_user, "expire_password_days", (new StringBuffer(String.valueOf(days))).toString(), true, true);
        put_in_user(serverGroup, the_user, "expire_password_when", (new SimpleDateFormat("MM/dd/yy hh:mm aa", Locale.US)).format(gc.getTime()), true, true);
        user = ut.getUser(serverGroup, the_user, true);
      } 
      loginReason.put("reason", "valid user");
      if (anyPass && user.getProperty("username").equalsIgnoreCase(the_user))
        return user; 
      if (the_password.startsWith("NTLM:")) {
        try {
          if (validateMd4(the_user, the_password, user.getProperty("password")))
            return user; 
        } catch (Exception e) {
          Log.log("USER_OBJ", 1, e);
        } 
        the_password = Common.makeBoundary();
      } 
      if (user.getProperty("username").equalsIgnoreCase(the_user) && (ServerStatus.thisObj.common_code.decode_pass(user.getProperty("password")).equals(the_password) || ServerStatus.thisObj.common_code.encode_pass(the_password, "SHA", user.getProperty("salt", "")).equals(user.getProperty("password")) || ServerStatus.thisObj.common_code.encode_pass(the_password, "SHA512", user.getProperty("salt", "")).equals(user.getProperty("password")) || ServerStatus.thisObj.common_code.encode_pass(the_password, "SHA3", user.getProperty("salt", "")).equals(user.getProperty("password")) || ServerStatus.thisObj.common_code.encode_pass(the_password, "MD5", user.getProperty("salt", "")).equals(user.getProperty("password")) || (user.getProperty("password").startsWith("MD5S2:") && ServerStatus.thisObj.common_code.encode_pass(String.valueOf(user.getProperty("password").substring(6, 8)) + the_password, "MD5", user.getProperty("salt", "")).substring(4).equalsIgnoreCase(user.getProperty("password").substring(8))) || ServerStatus.thisObj.common_code.encode_pass(the_password, "MD4", user.getProperty("salt", "")).equals(user.getProperty("password")) || ServerStatus.thisObj.common_code.crypt3(the_password, user.getProperty("password")).equals(user.getProperty("password")) || ServerStatus.thisObj.common_code.bcrypt(the_password, user.getProperty("password")).equals(user.getProperty("password")) || ServerStatus.thisObj.common_code.md5crypt(the_password, user.getProperty("password")).equals(user.getProperty("password")) || ServerStatus.thisObj.common_code.sha512crypt(the_password, user.getProperty("password")).equals(user.getProperty("password"))))
        return user; 
      if (user.getProperty("username").equalsIgnoreCase(the_user) && (user.getProperty("auto_set_pass", "false").equals("true") || ServerStatus.thisObj.common_code.decode_pass(user.getProperty("password")).equals("-AUTO-SET-ON-LOGIN-") || ServerStatus.thisObj.common_code.encode_pass("-AUTO-SET-ON-LOGIN-", "SHA", user.getProperty("salt", "")).equals(user.getProperty("password")) || ServerStatus.thisObj.common_code.encode_pass("-AUTO-SET-ON-LOGIN-", "SHA512", user.getProperty("salt", "")).equals(user.getProperty("password")) || ServerStatus.thisObj.common_code.encode_pass("-AUTO-SET-ON-LOGIN-", "SHA3", user.getProperty("salt", "")).equals(user.getProperty("password")) || ServerStatus.thisObj.common_code.encode_pass("-AUTO-SET-ON-LOGIN-", "MD5", user.getProperty("salt", "")).equals(user.getProperty("password")))) {
        Log.log("SERVER", 0, String.valueOf(the_user) + " logging in to change expired password...");
        if (ServerStatus.thisObj.common_code.decode_pass(user.getProperty("password")).equals(the_password) || ServerStatus.thisObj.common_code.encode_pass(the_password, "SHA", user.getProperty("salt", "")).equals(user.getProperty("password")) || ServerStatus.thisObj.common_code.encode_pass(the_password, "SHA512", user.getProperty("salt", "")).equals(user.getProperty("password")) || ServerStatus.thisObj.common_code.encode_pass(the_password, "SHA3", user.getProperty("salt", "")).equals(user.getProperty("password")) || ServerStatus.thisObj.common_code.encode_pass(the_password, "MD5", user.getProperty("salt", "")).equals(user.getProperty("password"))) {
          Log.log("SERVER", 0, String.valueOf(the_user) + " logging in with expired password...");
        } else if (Common.checkPasswordRequirements(the_password, user.getProperty("password_history", "")).equals("")) {
          Log.log("SERVER", 0, String.valueOf(the_user) + " password is being changed...");
          synchronized (userExpirePasswordLock) {
            put_in_user(serverGroup, the_user, "password", ServerStatus.thisObj.common_code.encode_pass(the_password, ServerStatus.SG("password_encryption"), user.getProperty("salt", "")), true, true);
            put_in_user(serverGroup, the_user, "password_history", Common.getPasswordHistory(the_password, user.getProperty("password_history", "")), true, true);
            Calendar gc = new GregorianCalendar();
            gc.setTime(new Date());
            gc.add(5, Integer.parseInt(user.getProperty("expire_password_days")));
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa", Locale.US);
            String s = sdf.format(gc.getTime());
            Log.log("SERVER", 0, String.valueOf(the_user) + " logging in with new password, changing it..." + user.getProperty("expire_password_when") + " to " + s);
            put_in_user(serverGroup, the_user, "expire_password_when", s, true, true);
            put_in_user(serverGroup, the_user, "auto_set_pass", "false", true, true);
          } 
          loginReason.put("changePassword", "true");
        } 
        try {
          user = ut.getUser(serverGroup, the_user, true);
        } catch (Exception exception) {}
        return user;
      } 
      if (user.getProperty("username").equalsIgnoreCase(the_user) && the_user.equalsIgnoreCase("ANONYMOUS"))
        return user; 
      if (user.getProperty("username").equalsIgnoreCase(the_user) && (ServerStatus.thisObj.common_code.decode_pass(user.getProperty("password")).equals(Common.url_decode(the_password)) || ServerStatus.thisObj.common_code.encode_pass(Common.url_decode(the_password), "SHA", user.getProperty("salt", "")).equals(user.getProperty("password")) || ServerStatus.thisObj.common_code.encode_pass(Common.url_decode(the_password), "SHA512", user.getProperty("salt", "")).equals(user.getProperty("password")) || ServerStatus.thisObj.common_code.encode_pass(Common.url_decode(the_password), "SHA3", user.getProperty("salt", "")).equals(user.getProperty("password")) || ServerStatus.thisObj.common_code.encode_pass(Common.url_decode(the_password), "MD5", user.getProperty("salt", "")).equals(user.getProperty("password")) || (user.getProperty("password").startsWith("MD5S2:") && ServerStatus.thisObj.common_code.encode_pass(String.valueOf(user.getProperty("password").substring(6, 8)) + the_password, "MD5", user.getProperty("salt", "")).substring(4).equalsIgnoreCase(user.getProperty("password").substring(8)))))
        return user; 
      if (user.getProperty("username").equalsIgnoreCase("TEMPLATE"))
        return user; 
      if (!user.getProperty("failure_count_max", "0").equals("0") && !user.getProperty("failure_count_max", "0").equals("")) {
        int failure_count = 0;
        if (!user.getProperty("failure_count", "0").equals(""))
          failure_count = Integer.parseInt(user.getProperty("failure_count", "0")); 
        failure_count++;
        int max = Integer.parseInt(user.getProperty("failure_count_max", "0"));
        if (max < 0) {
          max *= -1;
          put_in_user(serverGroup, the_user, "failure_count_max", (new StringBuffer(String.valueOf(max))).toString(), true, true);
        } 
        put_in_user(serverGroup, the_user, "failure_count", (new StringBuffer(String.valueOf(failure_count))).toString(), true, true);
        if (failure_count >= max) {
          put_in_user(serverGroup, the_user, "max_logins", "-1", true, true);
          if (!user.getProperty("disabled_account_task", "").equals("")) {
            Properties user_f = user;
            int failure_count_f = failure_count;
            String the_user_f = the_user;
            try {
              Worker.startWorker(new Runnable(this, user_f, the_user_f, failure_count_f) {
                    final UserTools this$0;
                    
                    private final Properties val$user_f;
                    
                    private final String val$the_user_f;
                    
                    private final int val$failure_count_f;
                    
                    public void run() {
                      Properties event = new Properties();
                      event.put("event_plugin_list", this.val$user_f.getProperty("disabled_account_task", ""));
                      event.put("name", "DisabledUser:" + this.val$the_user_f + ":" + this.val$failure_count_f);
                      Vector items = new Vector();
                      items.addElement(this.val$user_f);
                      this.val$user_f.put("url", "virtual://user/" + this.val$the_user_f);
                      ServerStatus.thisObj.events6.doEventPlugin(null, event, null, items);
                    }
                  });
            } catch (Exception e) {
              Log.log("SERVER", 0, e);
            } 
          } 
        } 
      } 
    } 
    return null;
  }
  
  public boolean validateMd4(String the_user, String the_password, String user_hashed_pass) throws Exception {
    String[] parts = the_password.split(":");
    String alg = parts[1];
    String domain = new String(Base64.decode(parts[2]), "UTF8");
    byte[] challenge = Base64.decode(parts[3]);
    byte[] encpass1 = Base64.decode(parts[4]);
    if (!user_hashed_pass.toUpperCase().startsWith("MD4:")) {
      user_hashed_pass = ServerStatus.thisObj.common_code.decode_pass(user_hashed_pass);
      if (user_hashed_pass.equals("") || user_hashed_pass.startsWith("MD5:") || user_hashed_pass.startsWith("BCRYPT:") || user_hashed_pass.startsWith("SHA:") || user_hashed_pass.startsWith("SHA512:") || user_hashed_pass.startsWith("SHA3:") || user_hashed_pass.startsWith("3CRYPT:") || user_hashed_pass.startsWith("MD5CRYPT:") || user_hashed_pass.startsWith("SHA512CRYPT:") || user_hashed_pass.startsWith("MD5S2:")) {
        Properties md4_hashes = (Properties)ServerStatus.thisObj.server_info.get("md4_hashes");
        if (md4_hashes == null)
          md4_hashes = new Properties(); 
        String md4_user = ServerStatus.thisObj.common_code.encode_pass(the_user, "MD4", "").substring("MD4:".length());
        if (!md4_hashes.getProperty(md4_user, "").equals("")) {
          user_hashed_pass = "MD4:" + md4_hashes.getProperty(md4_user, "");
        } else {
          return false;
        } 
      } else {
        user_hashed_pass = ServerStatus.thisObj.common_code.encode_pass(user_hashed_pass, "MD4", "");
      } 
    } 
    String tp = user_hashed_pass.substring("MD4:".length());
    byte[] md4pass = Base64.decode(tp);
    byte[] encpass2 = (byte[])null;
    if (alg.equals("1") && encpass1.length == 64) {
      byte[] b = encpass1;
      encpass1 = new byte[encpass1.length];
      Mac hmacMD5 = Mac.getInstance("HMACMD5");
      SecretKeySpec key = new SecretKeySpec(md4pass, 0, md4pass.length, "MD5");
      hmacMD5.init(key);
      byte[] h2 = hmacMD5.doFinal((String.valueOf(the_user.toUpperCase()) + domain).getBytes("UnicodeLittleUnmarked"));
      byte[] b2 = new byte[b.length - 8];
      System.arraycopy(challenge, 0, b2, 0, 8);
      System.arraycopy(b, 16, b2, 8, b.length - 16);
      hmacMD5 = Mac.getInstance("HMACMD5");
      hmacMD5.init(new SecretKeySpec(h2, 0, h2.length, "MD5"));
      encpass2 = hmacMD5.doFinal(b2);
      encpass1 = new byte[16];
      System.arraycopy(b, 0, encpass1, 0, encpass1.length);
    } else if (alg.equals("1")) {
      byte[] p21 = new byte[21];
      System.arraycopy(md4pass, 0, p21, 0, md4pass.length);
      encpass2 = DesEncrypter.blockEncrypt(p21, challenge);
    } else if (alg.equals("2")) {
      Mac hmacMD5 = Mac.getInstance("HMACMD5");
      SecretKeySpec key = new SecretKeySpec(md4pass, 0, md4pass.length, "MD5");
      hmacMD5.init(key);
      encpass2 = hmacMD5.doFinal((String.valueOf(the_user.toUpperCase()) + domain).getBytes("UnicodeLittleUnmarked"));
    } 
    boolean ok = (encpass1.length == encpass2.length);
    for (int x = 0; x < encpass2.length && ok; x++) {
      if (encpass1[x] != encpass2[x])
        ok = false; 
    } 
    return ok;
  }
  
  public static String get_real_path_to_user(String serverGroup, String username) {
    return XMLUsers.findUser(String.valueOf(System.getProperty("crushftp.users")) + serverGroup + "/", username);
  }
  
  public static void addFolder(String serverGroup, String username, String path, String name) {
    addFolder(serverGroup, username, path, name, true);
  }
  
  public static void addFolder(String serverGroup, String username, String path, String name, boolean replicate) {
    up.addFolder(serverGroup, username, path, name);
    Properties p = new Properties();
    p.put("serverGroup", serverGroup);
    p.put("username", username);
    p.put("path", path);
    p.put("name", name);
    if (replicate)
      SharedSessionReplicated.send(Common.makeBoundary(), "crushftp.handlers.addFolder", "info", p); 
  }
  
  public static void addItem(String serverGroup, String username, String path, String name, String url, String type, Properties moreItems, boolean encrypted, String encrypted_class) throws Exception {
    addItem(serverGroup, username, path, name, url, type, moreItems, encrypted, encrypted_class, true);
  }
  
  public static void addItem(String serverGroup, String username, String path, String name, String url, String type, Properties moreItems, boolean encrypted, String encrypted_class, boolean replicate) throws Exception {
    up.addItem(serverGroup, username, path, name, url, type, moreItems, encrypted, encrypted_class);
    Properties p = new Properties();
    p.put("serverGroup", serverGroup);
    p.put("username", username);
    p.put("path", path);
    p.put("name", name);
    p.put("url", url);
    p.put("type", type);
    p.put("moreItems", moreItems);
    p.put("encrypted", (new StringBuffer(String.valueOf(encrypted))).toString());
    p.put("encrypted_class", encrypted_class);
    if (replicate)
      SharedSessionReplicated.send(Common.makeBoundary(), "crushftp.handlers.addItem", "info", p); 
  }
  
  public static String addPriv(String serverGroup, String username, String path, String priv, int homeIndex, VFS tempVFS) {
    try {
      Properties item = tempVFS.get_item(path);
      if (item.getProperty("type", "DIR").equalsIgnoreCase("DIR") && !path.endsWith("/"))
        path = String.valueOf(path) + "/"; 
      if (item.getProperty("is_virtual", "").equals("true") && priv.indexOf("(write)") >= 0 && item.getProperty("VFS_real_path", "").equals(""))
        return LOC.G("Cannot allow write access to virtual folder."); 
      if (item.getProperty("is_virtual", "").equals("true") && priv.indexOf("(makedir)") >= 0 && item.getProperty("VFS_real_path", "").equals(""))
        return LOC.G("Cannot allow make directory access to a virtual folder."); 
    } catch (Exception exception) {}
    if (priv.indexOf("(inherited)") >= 0)
      priv = Common.replace_str(priv, "(inherited)", ""); 
    ((Properties)tempVFS.permissions.elementAt(homeIndex)).put(getUpper(path), priv);
    if (priv.trim().equals(""))
      ((Properties)tempVFS.permissions.elementAt(homeIndex)).remove(getUpper(path)); 
    writeVFS(serverGroup, username, tempVFS);
    return "";
  }
  
  public static void updatePrivpath(String serverGroup, String username, String oldPath, String newPath, Properties item, String privs, VFS tempVFS) {
    String priv = "";
    try {
      if (item == null)
        item = tempVFS.get_item(oldPath); 
      if (item.getProperty("type", "DIR").equalsIgnoreCase("DIR") && !newPath.endsWith("/"))
        newPath = String.valueOf(newPath) + "/"; 
      priv = item.getProperty("privs", "");
    } catch (Exception exception) {}
    if (privs != null)
      priv = privs; 
    if (priv.indexOf("(inherited)") < 0) {
      priv = Common.replace_str(priv, "(inherited)", "");
      ((Properties)tempVFS.permissions.elementAt(0)).remove(getUpper(oldPath));
      ((Properties)tempVFS.permissions.elementAt(0)).remove(String.valueOf(getUpper(oldPath)) + "/");
      ((Properties)tempVFS.permissions.elementAt(0)).put(getUpper(newPath), priv);
      ((Properties)tempVFS.permissions.elementAt(0)).put(String.valueOf(getUpper(newPath)) + "/", priv);
      writeVFS(serverGroup, username, tempVFS);
    } 
  }
  
  public static void writeVFS(String serverGroup, String username, VFS uVFS) {
    writeVFS(serverGroup, username, uVFS.homes.elementAt(0));
  }
  
  public static void writeVFS(String serverGroup, String username, Properties virtual) {
    writeVFS(serverGroup, username, virtual, true);
  }
  
  public static void writeVFS(String serverGroup, String username, Properties virtual, boolean replicate) {
    up.writeVFS(serverGroup, username, virtual);
    Properties p = new Properties();
    p.put("serverGroup", serverGroup);
    p.put("username", username);
    p.put("virtual", virtual);
    if (replicate)
      SharedSessionReplicated.send(Common.makeBoundary(), "crushftp.handlers.writeVFS", "info", p); 
  }
  
  public static String getUpper(String path) {
    String path2 = path;
    if (System.getProperty("crushftp.priv_upper", "true").equals("true"))
      path2 = path2.toUpperCase(); 
    return path2;
  }
  
  public static void loadPermissions(VFS tempVFS) {}
  
  public static Vector vItemLoad(String path) {
    Vector v = (Vector)Common.readXMLObject(path);
    if (v != null)
      for (int x = 0; x < v.size(); x++) {
        Properties p = v.elementAt(x);
        if (p.getProperty("encrypted", "false").equals("true"))
          if (!p.getProperty("encrypted_class", "").trim().equals("")) {
            try {
              Class c = ServerStatus.clasLoader.loadClass(p.getProperty("encrypted_class").trim());
              Constructor cons = c.getConstructor(new Class[] { (new Properties()).getClass(), (new String()).getClass() });
              cons.newInstance(new Object[] { p, "decrypt" });
            } catch (Exception e) {
              Log.log("USER_OBJ", 1, e);
            } 
          } else {
            p.put("url", (new Common()).decode_pass(p.getProperty("url")));
          }  
      }  
    return v;
  }
  
  public Vector get_virtual_list_fake(VFS tempVFS, String path, String serverGroup, String parentUser) {
    Vector listing = new Vector();
    try {
      tempVFS.getListing(listing, path);
      if (parentUser != null) {
        boolean ok = false;
        for (int x = 0; x < listing.size(); x++) {
          Properties p = listing.elementAt(x);
          p = tempVFS.get_item(String.valueOf(p.getProperty("root_dir")) + p.getProperty("name"));
          ok = parentPathOK(serverGroup, parentUser, p.getProperty("url"));
          if (ok)
            break; 
        } 
        if (!ok)
          listing = new Vector(); 
      } 
    } catch (Exception e) {
      listing.addElement(e);
      Log.log("USER_OBJ", 1, e);
    } finally {
      if (tempVFS != null)
        tempVFS.disconnect(); 
    } 
    return listing;
  }
  
  public static boolean testLimitedAdminAccess(Object o, String parentUser, String serverGroup) throws Exception {
    boolean ok = true;
    try {
      if (o == null)
        return true; 
      Vector v = null;
      if (o instanceof Properties) {
        v = new Vector();
        v.addElement(o);
      } else {
        v = (Vector)o;
      } 
      for (int x = 0; x < v.size(); x++) {
        Properties item = (Properties)v.elementAt(x);
        if (item.containsKey("url") && !parentPathOK(serverGroup, parentUser, item.getProperty("url")))
          ok = false; 
        if (item.containsKey("events")) {
          Vector vv = (Vector)item.get("events");
          for (int xx = 0; xx < vv.size(); xx++) {
            Properties event = vv.elementAt(xx);
            if (event.getProperty("event_action_list", "").indexOf("plugin") >= 0)
              ok = false; 
          } 
        } 
        if (!item.getProperty("admin_group_name", "").equals(""))
          ok = false; 
        if (item.getProperty("site", "").toUpperCase().indexOf("(CONNECT)") >= 0)
          ok = false; 
        if (item.getProperty("site", "").toUpperCase().indexOf("(USER_ADMIN)") >= 0)
          ok = false; 
        if (item.getProperty("site", "").toUpperCase().indexOf("(SITE_QUIT)") >= 0)
          ok = false; 
        if (item.getProperty("site", "").toUpperCase().indexOf("(SITE_KICK)") >= 0)
          ok = false; 
        if (item.getProperty("site", "").toUpperCase().indexOf("(SITE_KICKBAN)") >= 0)
          ok = false; 
        if (item.getProperty("site", "").toUpperCase().indexOf("(SITE_USERS)") >= 0)
          ok = false; 
        if (item.getProperty("site", "").toUpperCase().indexOf("(SITE_PLUGIN)") >= 0)
          ok = false; 
        if (item.getProperty("site", "").toUpperCase().indexOf("(USER") >= 0)
          ok = false; 
        if (item.getProperty("site", "").toUpperCase().indexOf("(SERVER") >= 0)
          ok = false; 
        if (item.getProperty("site", "").toUpperCase().indexOf("(JOB") >= 0)
          ok = false; 
        if (item.getProperty("site", "").toUpperCase().indexOf("(LOG") >= 0)
          ok = false; 
        if (item.getProperty("site", "").toUpperCase().indexOf("(REPORT") >= 0)
          ok = false; 
        if (item.getProperty("site", "").toUpperCase().indexOf("(PREF") >= 0)
          ok = false; 
        if (item.getProperty("site", "").toUpperCase().indexOf("(SHARE") >= 0)
          ok = false; 
      } 
    } catch (Exception e) {
      Log.log("USER_OBJ", 0, e);
      throw e;
    } 
    return ok;
  }
  
  public static boolean parentPathOK(String serverGroup, String parentUser, String url) {
    if (parentUser == null)
      return true; 
    if (!url.toUpperCase().startsWith("FILE:/"))
      return true; 
    Vector listing2 = new Vector();
    VFS tempVFS = null;
    try {
      tempVFS = VFS.getVFS(up.buildVFS(serverGroup, parentUser));
      tempVFS.getListing(listing2, "/");
    } catch (Exception e) {
      Exception exception1;
      Log.log("USER_OBJ", 1, exception1);
    } finally {
      if (tempVFS != null)
        tempVFS.disconnect(); 
    } 
    if (tempVFS != null)
      tempVFS.disconnect(); 
    return false;
  }
  
  public static Properties waitResponse(Properties p, int timeout) {
    long start = System.currentTimeMillis();
    while (System.currentTimeMillis() - start < (timeout * 1000)) {
      if (p.getProperty("status", "").equals("done"))
        return p; 
      try {
        Thread.sleep(10L);
      } catch (InterruptedException interruptedException) {}
    } 
    Properties p2 = (Properties)p.clone();
    if (p2.containsKey("password"))
      p2.put("password", "*******"); 
    if (p2.containsKey("reset_token"))
      p2.put("reset_token", "*******"); 
    Log.log("DMZ", 0, "Timeout waiting for response (" + timeout + "):" + p2);
    return null;
  }
  
  public static Vector buildPublicKeys(String username, Properties user) throws IOException {
    String keyStr = user.getProperty("ssh_public_keys", "");
    Log.log("SSH_SERVER", 2, keyStr);
    if (keyStr.trim().equalsIgnoreCase("DMZ") || keyStr.trim().startsWith("DMZ:"))
      if (Common.System2.get("crushftp.dmz.queue.sock") != null) {
        Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
        Properties action = new Properties();
        action.put("type", "GET:USER_SSH_KEYS");
        action.put("id", Common.makeBoundary());
        action.put("username", username);
        action.put("need_response", "true");
        if (keyStr.trim().startsWith("DMZ:")) {
          action.put("linkedServer", keyStr.substring(keyStr.indexOf(":") + 1).trim());
        } else {
          action.put("linkedServer", "MainUsers");
        } 
        queue.addElement(action);
        action = waitResponse(action, 60);
        if (action != null && action.containsKey("public_keys"))
          return (Vector)action.get("public_keys"); 
        return new Vector();
      }  
    BufferedReader br = new BufferedReader(new StringReader(keyStr));
    String s = "";
    Vector keysVec = new Vector();
    String simpleUsername = username;
    if (simpleUsername.indexOf("\\") >= 0)
      simpleUsername = simpleUsername.substring(simpleUsername.indexOf("\\") + 1); 
    if (simpleUsername.indexOf("/") >= 0)
      simpleUsername = simpleUsername.substring(simpleUsername.indexOf("/") + 1); 
    if (simpleUsername.startsWith("$ASCII$"))
      simpleUsername = simpleUsername.substring("$ASCII$".length()); 
    Log.log("SSH_SERVER", 2, "publicKey_username:" + simpleUsername);
    if (keyStr.toUpperCase().indexOf("SSH2 PUBLIC KEY") >= 0) {
      String[] arrayOfString = keyStr.split(";;;");
      for (int i = 0; i < arrayOfString.length; i++) {
        if (!arrayOfString[i].trim().equals(""))
          keysVec.addElement(arrayOfString[i].trim()); 
      } 
    } else {
      while ((s = br.readLine()) != null) {
        Log.log("SSH_SERVER", 2, s);
        if (s.endsWith("/") || s.endsWith("\\")) {
          File[] files = (new File(s)).listFiles();
          if (files != null)
            for (int i = 0; i < files.length; i++) {
              Log.log("SSH_SERVER", 2, files[i].toString());
              if (files[i].length() < 512000L && !files[i].getName().toUpperCase().startsWith(".DS_"))
                if (files[i].getName().toUpperCase().startsWith(String.valueOf(simpleUsername.toUpperCase()) + "_") || files[i].getName().toUpperCase().equals(simpleUsername.toUpperCase()) || files[i].getName().toUpperCase().equals(String.valueOf(simpleUsername.toUpperCase()) + ".PUB"))
                  keysVec.addElement(files[i].getPath());  
            }  
          continue;
        } 
        keysVec.addElement(s);
      } 
    } 
    for (int x = 0; x < keysVec.size(); x++) {
      String data = keysVec.elementAt(x).toString();
      if ((new File(data)).exists() && (new File(data)).isFile()) {
        RandomAccessFile in = null;
        try {
          in = new RandomAccessFile(data, "r");
          byte[] b = new byte[(int)in.length()];
          in.readFully(b);
          keysVec.setElementAt(new String(b, "UTF8"), x);
        } finally {
          in.close();
        } 
      } 
    } 
    Enumeration keys = Common.System2.keys();
    while (keys.hasMoreElements()) {
      String propKey = keys.nextElement().toString();
      if (propKey.startsWith("j2ssh.publickeys.") && (propKey.startsWith("j2ssh.publickeys." + username.toUpperCase() + "_") || propKey.equals("j2ssh.publickeys." + username.toUpperCase()))) {
        Properties p = (Properties)Common.System2.get(propKey);
        byte[] b = (byte[])p.get("bytes");
        keysVec.addElement(new String(b));
      } 
    } 
    return keysVec;
  }
  
  public static void addAnyPassToken(String token) {
    if (anyPassTokens.indexOf(token) < 0) {
      anyPassTokensTime.put(token, (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      anyPassTokens.insertElementAt(token, 0);
    } 
  }
  
  public static boolean checkPassword(String pass) {
    for (int x = anyPassTokens.size() - 1; x >= 0; x--) {
      String token = anyPassTokens.elementAt(x).toString();
      if (x > 0)
        if (System.currentTimeMillis() - Long.parseLong(anyPassTokensTime.getProperty(token, "0")) > 28800000L) {
          anyPassTokens.remove(x);
          anyPassTokensTime.remove(token);
        }  
      if (pass.equals(token))
        return true; 
    } 
    return false;
  }
  
  public static boolean isValidUsername(String username) {
    Vector server_groups = ServerStatus.VG("server_groups");
    for (int x = 0; x < server_groups.size(); x++) {
      String serverGroup = server_groups.elementAt(x).toString();
      Vector user_list = new Vector();
      refreshUserList(serverGroup, user_list);
      for (int xx = 0; xx < user_list.size(); xx++) {
        if (user_list.elementAt(xx).toString().equals(username))
          return true; 
      } 
    } 
    return false;
  }
}
