package crushftp.user;

import com.crushftp.client.VRL;
import crushftp.handlers.CIProperties;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.UserTools;
import crushftp.server.ServerStatus;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class XMLUsers extends UserProvider {
  public static Object user_lock = new Object();
  
  public static Properties user_case_lookup = new Properties();
  
  public static Object groupLoc = new Object();
  
  public static Object inheritanceLoc = new Object();
  
  private static String getUserPath(String serverGroup, String username) {
    String path = String.valueOf(System.getProperty("crushftp.users")) + serverGroup + "/" + username + "/";
    String path2 = user_case_lookup.getProperty(path.toUpperCase());
    if (!(new File(path)).exists() && (Common.machine_is_unix() || Common.machine_is_linux()))
      if (path2 != null && (new File(path2)).exists()) {
        path = path2;
      } else {
        path2 = findUser(String.valueOf(System.getProperty("crushftp.users")) + serverGroup + "/", username);
        if (path2 != null) {
          user_case_lookup.put(path.toUpperCase(), path2);
          path = path2;
        } 
      }  
    return path;
  }
  
  public static String findUser(String path, String username) {
    if (username.equals(""))
      return null; 
    File[] list = (new File(path)).listFiles();
    int x;
    for (x = 0; list != null && x < list.length; x++) {
      if (list[x].isDirectory())
        if (list[x].getName().equalsIgnoreCase(username))
          return String.valueOf(list[x].getPath()) + "/";  
    } 
    if (System.getProperty("crushftp.recurse_user_search", "false").equals("false"))
      return null; 
    for (x = 0; list != null && x < list.length; x++) {
      if (list[x].isDirectory()) {
        if (list[x].getName().equalsIgnoreCase(username))
          return String.valueOf(list[x].getPath()) + "/"; 
        if (!list[x].getName().equalsIgnoreCase("VFS")) {
          Log.log("USER_OBJ", 2, "Searching for user " + username + ", searching inside of user " + list[x].getName() + " located here:" + list[x].getPath() + "/");
          String result = findUser(String.valueOf(list[x].getPath()) + "/", username);
          if (result != null)
            return result; 
        } 
      } 
    } 
    return null;
  }
  
  public static CIProperties buildVFSXML(String vfsHome) {
    if (!vfsHome.endsWith("/"))
      vfsHome = String.valueOf(vfsHome) + "/"; 
    CIProperties virtual = new CIProperties();
    Properties permissions0 = (Properties)Common.readXMLObject(String.valueOf(vfsHome) + "VFS.XML");
    if (permissions0 == null) {
      permissions0 = new Properties();
      permissions0.put("/", "(read)(view)(resume)");
    } 
    Vector permissions = new Vector();
    Enumeration keys = permissions0.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      Object o = permissions0.remove(key);
      permissions0.put(key.toUpperCase(), o);
    } 
    permissions.addElement(permissions0);
    virtual.put("vfs_permissions_object", permissions);
    Vector list = new Vector();
    String rootPath = "";
    try {
      rootPath = String.valueOf((new File(String.valueOf(vfsHome) + "VFS/")).getCanonicalPath().replace('\\', '/')) + "/";
      list.addElement(new File(rootPath));
      Common.getAllFileListing(list, String.valueOf(vfsHome) + "VFS/", 20, true);
    } catch (Exception e) {
      Log.log("USER_OBJ", 0, e);
    } 
    if (!(new File(String.valueOf(vfsHome) + "VFS/")).exists()) {
      Properties p = new Properties();
      p.put("virtualPath", "/");
      p.put("name", "VFS");
      p.put("type", "DIR");
      p.put("vItems", new Vector());
      virtual.put("/", p);
    } else {
      for (int x = 0; x < list.size(); x++) {
        File f = list.elementAt(x);
        if (!f.getName().equals(".DS_Store")) {
          Properties p = new Properties();
          p.put("name", f.getName());
          if (f.isDirectory()) {
            p.put("type", "DIR");
          } else {
            p.put("type", "FILE");
            Vector v = UserTools.vItemLoad(f.getPath());
            if (v == null)
              v = new Vector(); 
            for (int xx = 0; xx < v.size(); xx++) {
              Properties pp = v.elementAt(xx);
              if (pp.getProperty("url", "").toLowerCase().startsWith("file:/") && !pp.getProperty("url", "").toLowerCase().startsWith("file://")) {
                if (!pp.getProperty("url", "").toLowerCase().endsWith("/"))
                  pp.put("url", String.valueOf(pp.getProperty("url")) + "/"); 
                pp.put("url", "file://" + pp.getProperty("url").substring("file:/".length()));
              } 
            } 
            p.put("vItems", v);
          } 
          try {
            String itemPath = f.getCanonicalPath().replace('\\', '/');
            p.put("virtualPath", itemPath.substring(rootPath.length() - 1));
            if (x == 0) {
              virtual.put("/", p);
            } else {
              virtual.put(itemPath.substring(rootPath.length() - 1), p);
            } 
          } catch (Exception e) {
            Log.log("USER_OBJ", 0, e);
          } 
        } 
      } 
    } 
    return virtual;
  }
  
  public Properties buildVFS(String serverGroup, String username) {
    return buildVFSXML(getUserPath(serverGroup, username));
  }
  
  public Properties loadGroups(String serverGroup) {
    Properties groups2 = null;
    String groupsPath = String.valueOf(System.getProperty("crushftp.users")) + serverGroup + "/groups.XML";
    if ((new File(groupsPath)).exists())
      try {
        groups2 = (Properties)Common.readXMLObject(groupsPath);
      } catch (ClassCastException classCastException) {} 
    if (groups2 == null)
      groups2 = new Properties(); 
    return groups2;
  }
  
  public void writeGroups(String serverGroup, Properties groups) {
    synchronized (groupLoc) {
      (new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/")).mkdirs();
      (new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/groups99.XML")).delete();
      for (int x = 98; x >= 0; x--) {
        try {
          (new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/groups" + x + ".XML")).renameTo(new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/groups" + (x + 1) + ".XML"));
        } catch (Exception exception) {}
      } 
      try {
        Common.writeXMLObject(String.valueOf(System.getProperty("crushftp.users")) + serverGroup + "/groups.XML.new", groups, "groups");
        (new File(String.valueOf(System.getProperty("crushftp.users")) + serverGroup + "/groups.XML")).renameTo(new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/groups0.XML"));
        (new File(String.valueOf(System.getProperty("crushftp.users")) + serverGroup + "/groups.XML")).delete();
        int loops = 0;
        while (!(new File(String.valueOf(System.getProperty("crushftp.users")) + serverGroup + "/groups.XML.new")).renameTo(new File(String.valueOf(System.getProperty("crushftp.users")) + serverGroup + "/groups.XML")) && loops++ < 100)
          Thread.sleep(100L); 
      } catch (Exception ee) {
        Log.log("USER_OBJ", 1, ee);
      } 
    } 
  }
  
  public void writeVFS(String serverGroup, String username, Properties virtual) {
    try {
      Vector permissions = (Vector)virtual.get("vfs_permissions_object");
      Properties permissions0 = new Properties();
      permissions0.put("/", "(read)(view)(resume)");
      if (permissions != null && permissions.size() > 0)
        permissions0 = permissions.elementAt(0); 
      Common.writeXMLObject(String.valueOf(getUserPath(serverGroup, username)) + "VFS.XML", permissions0, "VFS");
    } catch (Exception ee) {
      Log.log("USER_OBJ", 0, ee);
    } 
    try {
      Common.recurseDelete(String.valueOf(getUserPath(serverGroup, username)) + "VFS/", false);
      (new File(String.valueOf(getUserPath(serverGroup, username)) + "VFS/")).mkdir();
      Enumeration keys = virtual.keys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement().toString();
        if (key.equals("vfs_permissions_object"))
          continue; 
        Properties p = (Properties)virtual.get(key);
        String virtualPath = p.getProperty("virtualPath");
        if (p.getProperty("type").equalsIgnoreCase("DIR")) {
          (new File(String.valueOf(getUserPath(serverGroup, username)) + "VFS" + virtualPath)).mkdirs();
          continue;
        } 
        (new File(String.valueOf(getUserPath(serverGroup, username)) + "VFS" + Common.all_but_last(virtualPath))).mkdirs();
        Vector v = (Vector)p.get("vItems");
        for (int xx = 0; xx < v.size(); xx++) {
          Properties pp = v.elementAt(xx);
          if (pp.getProperty("encrypted", "false").equals("true"))
            if (!pp.getProperty("encrypted_class", "").trim().equals("")) {
              try {
                Class c = Thread.currentThread().getContextClassLoader().loadClass(pp.getProperty("encrypted_class").trim());
                Constructor cons = c.getConstructor(new Class[] { (new Properties()).getClass(), (new String()).getClass() });
                cons.newInstance(new Object[] { pp, "encrypt" });
              } catch (Exception ee) {
                Log.log("USER_OBJ", 1, ee);
              } 
            } else {
              pp.put("url", ServerStatus.thisObj.common_code.encode_pass(pp.getProperty("url"), "DES", ""));
            }  
        } 
        Common.writeXMLObject(String.valueOf(getUserPath(serverGroup, username)) + "VFS" + virtualPath, v, "VFS");
      } 
      Common.updateOSXInfo(getUserPath(serverGroup, username));
    } catch (Exception ee) {
      Log.log("USER_OBJ", 0, ee);
    } 
  }
  
  public Properties loadInheritance(String serverGroup) {
    Properties inheritance2 = null;
    String inheritancePath = String.valueOf(System.getProperty("crushftp.users")) + serverGroup + "/inheritance.XML";
    if ((new File(inheritancePath)).exists())
      try {
        inheritance2 = (Properties)Common.readXMLObject(inheritancePath);
      } catch (ClassCastException classCastException) {} 
    if (inheritance2 == null) {
      inheritance2 = new Properties();
      inheritance2.put("default", new Vector());
      writeInheritance(serverGroup, inheritance2);
    } 
    return inheritance2;
  }
  
  public Properties loadUser(String serverGroup, String username, Properties inheritance, boolean flattenUser) {
    Properties user = read_user_no_cache(serverGroup, username);
    Properties newUser = read_user_no_cache(serverGroup, "default");
    if (user == null)
      return null; 
    Properties originalUser = (Properties)user.clone();
    boolean needWrite = fixExpireAccount(user, originalUser, newUser);
    needWrite |= fixExpirePassword(user, originalUser, newUser);
    if (needWrite)
      writeUser(serverGroup, username, originalUser, false); 
    if (!flattenUser)
      return user; 
    Enumeration keys = inheritance.keys();
    Vector ichain = null;
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      if (key.equalsIgnoreCase(username)) {
        ichain = (Vector)inheritance.get(key);
        break;
      } 
    } 
    if (ichain != null)
      for (int x = 0; x < ichain.size(); x++) {
        Properties p = read_user_no_cache(serverGroup, ichain.elementAt(x).toString());
        UserTools.mergeWebCustomizations(newUser, p);
        UserTools.mergeLinkedVFS(newUser, p);
        try {
          needWrite = fixExpireAccount(user, originalUser, p);
          needWrite |= fixExpirePassword(user, originalUser, p);
          if (needWrite)
            writeUser(serverGroup, username, originalUser, true); 
        } catch (Exception e) {
          Common.debug(1, e);
        } 
        if (p != null)
          newUser.putAll(p); 
      }  
    UserTools.mergeWebCustomizations(newUser, user);
    newUser.putAll(user);
    return newUser;
  }
  
  public boolean fixExpirePassword(Properties user, Properties originalUser, Properties p) {
    if (user != null && user.getProperty("expire_password", "").equals("") && !p.getProperty("expire_password", "").equals("") && !p.getProperty("expire_password", "").equals("false")) {
      int days = Integer.parseInt(p.getProperty("expire_password_days"));
      originalUser.put("expire_password_days", (new StringBuffer(String.valueOf(days))).toString());
      if (days < 0) {
        days *= -1;
        originalUser.put("expire_password_days", (new StringBuffer(String.valueOf(days))).toString());
      } 
      GregorianCalendar gc = new GregorianCalendar();
      gc.setTime(new Date());
      gc.add(5, days);
      p.put("expire_password_when", (new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa", Locale.US)).format(gc.getTime()));
      originalUser.put("expire_password_when", (new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa", Locale.US)).format(gc.getTime()));
      originalUser.put("expire_password", p.getProperty("expire_password"));
      return true;
    } 
    return false;
  }
  
  public boolean fixExpireAccount(Properties user, Properties originalUser, Properties p) {
    if (user != null && user.getProperty("account_expire", "").equals("") && !p.getProperty("account_expire", "").equals("") && !p.getProperty("account_expire_rolling_days", "0").equals("") && Math.abs(Integer.parseInt(p.getProperty("account_expire_rolling_days", "0"))) > 0) {
      GregorianCalendar gc = new GregorianCalendar();
      gc.setTime(new Date());
      int days = Integer.parseInt(p.getProperty("account_expire_rolling_days"));
      originalUser.put("account_expire_rolling_days", (new StringBuffer(String.valueOf(days))).toString());
      if (days < 0) {
        days *= -1;
        originalUser.put("account_expire_rolling_days", (new StringBuffer(String.valueOf(days))).toString());
      } 
      gc.add(5, days);
      originalUser.put("account_expire_task", p.getProperty("account_expire_task", ""));
      originalUser.put("account_expire_delete", p.getProperty("account_expire_delete", "false"));
      originalUser.put("account_expire", (new SimpleDateFormat("MM/dd/yy hh:mm aa", Locale.US)).format(gc.getTime()));
      originalUser.put("account_expire", (new SimpleDateFormat("MM/dd/yy hh:mm aa", Locale.US)).format(gc.getTime()));
      return true;
    } 
    return false;
  }
  
  public void updateUser(String serverGroup, String username1, String username2, String password) {
    if (username2 != null) {
      File rnfr = new File(getUserPath(serverGroup, username1));
      File rnto = new File(getUserPath(serverGroup, username2));
      rnfr.renameTo(rnto);
    } 
  }
  
  public void writeUser(String serverGroup, String username, Properties user, boolean backup) {
    try {
      if (backup)
        writeBackupUser(serverGroup, username); 
    } catch (Exception ee) {
      Log.log("USER_OBJ", 0, ee);
    } 
    try {
      String user_dir = getUserPath(serverGroup, username);
      (new File(user_dir)).mkdirs();
      if (!(new File(user_dir)).getCanonicalFile().getName().equals(username)) {
        String tmp = Common.makeBoundary(3);
        if (!user_dir.endsWith("/"))
          user_dir = String.valueOf(user_dir) + "/"; 
        (new File(user_dir)).renameTo(new File(String.valueOf(user_dir.substring(0, user_dir.length() - 1)) + tmp));
        (new File(String.valueOf(user_dir.substring(0, user_dir.length() - 1)) + tmp)).renameTo(new File(user_dir));
      } 
      Common.updateOSXInfo(user_dir);
      user.put("version", "1.0");
      Common c = null;
      if (ServerStatus.thisObj != null) {
        c = ServerStatus.thisObj.common_code;
      } else {
        c = new Common();
      } 
      (new File(String.valueOf(user_dir) + "user_old.XML")).delete();
      String uid = Common.makeBoundary(6);
      Common.writeXMLObject(String.valueOf(user_dir) + "user_" + uid + ".XML", user, "userfile");
      synchronized (user_lock) {
        if ((new File(String.valueOf(user_dir) + "user_" + uid + ".XML")).length() > 0L)
          Common.copy(String.valueOf(user_dir) + "user_" + uid + ".XML", String.valueOf(user_dir) + "user.XML", true); 
        (new File(String.valueOf(user_dir) + "user_" + uid + ".XML")).delete();
      } 
      Common.updateOSXInfo(user_dir);
    } catch (Exception ee) {
      Log.log("USER_OBJ", 0, ee);
      throw new RuntimeException(ee);
    } 
  }
  
  private void writeBackupUser(String serverGroup, String username) throws Exception {
    UserTools.purgeOldBackups(ServerStatus.IG("user_backup_count"));
    SimpleDateFormat sdf = new SimpleDateFormat("MMddyyyy_HHmmss", Locale.US);
    String path = getUserPath(serverGroup, username);
    if (path == null || !(new File(path)).exists())
      return; 
    (new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/")).mkdirs();
    ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(String.valueOf(System.getProperty("crushftp.backup")) + "backup/" + username + "-" + sdf.format(new Date()) + ".zip"));
    Vector zipFiles = new Vector();
    Common.getAllFileListing(zipFiles, path, 10, false);
    for (int x = zipFiles.size() - 1; x >= 0; x--) {
      if (Common.last(zipFiles.elementAt(x).toString()).equals(".DS_Store")) {
        zipFiles.removeElementAt(x);
      } else if (((File)zipFiles.elementAt(x)).length() > 1048576L) {
        zipFiles.removeElementAt(x);
      } 
    } 
    int offset = (new File(path)).getCanonicalPath().length();
    for (int xx = 0; xx < zipFiles.size(); xx++) {
      File item = (File)zipFiles.elementAt(xx);
      if (item.isDirectory()) {
        zout.putNextEntry(new ZipEntry(String.valueOf(item.getCanonicalPath().substring(offset)) + "/"));
      } else if (item.isFile()) {
        zout.putNextEntry(new ZipEntry(item.getCanonicalPath().substring(offset)));
        RandomAccessFile in = new RandomAccessFile(item.getAbsolutePath(), "r");
        byte[] b = new byte[(int)in.length()];
        in.readFully(b);
        in.close();
        zout.write(b);
      } 
      zout.closeEntry();
    } 
    zout.finish();
    zout.close();
  }
  
  public void writeInheritance(String serverGroup, Properties inheritance) {
    synchronized (inheritanceLoc) {
      (new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/")).mkdirs();
      (new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/inheritance99.XML")).delete();
      for (int x = 98; x >= 0; x--) {
        try {
          (new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/inheritance" + x + ".XML")).renameTo(new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/inheritance" + (x + 1) + ".XML"));
        } catch (Exception exception) {}
      } 
      try {
        Common.writeXMLObject(String.valueOf(System.getProperty("crushftp.users")) + serverGroup + "/inheritance.XML.new", inheritance, "inheritance");
        (new File(String.valueOf(System.getProperty("crushftp.users")) + serverGroup + "/inheritance.XML")).renameTo(new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/inheritance0.XML"));
        (new File(String.valueOf(System.getProperty("crushftp.users")) + serverGroup + "/inheritance.XML")).delete();
        int loops = 0;
        while (!(new File(String.valueOf(System.getProperty("crushftp.users")) + serverGroup + "/inheritance.XML.new")).renameTo(new File(String.valueOf(System.getProperty("crushftp.users")) + serverGroup + "/inheritance.XML")) && loops++ < 100)
          Thread.sleep(100L); 
      } catch (Exception ee) {
        Log.log("USER_OBJ", 1, ee);
      } 
    } 
  }
  
  public void deleteUser(String serverGroup, String username) {
    Common.recurseDelete(getUserPath(serverGroup, username), false);
  }
  
  public Properties read_user_no_cache(String serverGroup, String username) {
    username = username.replaceAll("/", "");
    username = username.replaceAll("\\\\", "");
    String real_path = "";
    try {
      real_path = getUserPath(serverGroup, username);
      Common common = null;
      if (ServerStatus.thisObj != null)
        common = ServerStatus.thisObj.common_code; 
      if (common == null)
        common = new Common(); 
      Properties p = null;
      for (int x = 0; p == null && x < 5 && (new File(real_path)).exists(); x++) {
        try {
          synchronized (user_lock) {
            p = (Properties)Common.readXMLObject(String.valueOf(real_path) + "user.XML");
          } 
          if (p == null && !username.equals("template") && !username.equals("anonymous") && !username.equals("") && !username.endsWith(".SHARED") && !username.equals("default"))
            Thread.sleep(200L); 
        } catch (Exception e) {
          if (username.equals("template") || username.equals("anonymous") || username.equals("") || username.endsWith(".SHARED") || username.equals("default"))
            throw e; 
          if (x >= 4)
            throw e; 
        } 
      } 
      Log.log("USER_OBJ", 3, "Finding path to username:" + username + ":" + real_path);
      username = (new File(real_path)).getCanonicalFile().getName();
      Log.log("USER_OBJ", 3, "Got path to username:" + username + ":" + real_path);
      if (p == null)
        return null; 
      p.put("username", username);
      p.put("user_name", username);
      p.put("real_path_to_user", real_path);
      return p;
    } catch (Exception e) {
      if (username.equals("default")) {
        Properties p = (Properties)Common.readXMLObject((new Common()).getClass().getResource("/assets/default_user.xml"));
        try {
          writeUser(serverGroup, "default", p, false);
        } catch (Exception ee) {
          Log.log("USER_OBJ", 0, ee);
        } 
        return p;
      } 
      if (!username.equals("template") && !username.equals("anonymous") && !username.equals("") && !username.endsWith(".SHARED")) {
        Log.log("USER_OBJ", 1, "Username not found:" + username + ":" + real_path);
        Log.log("USER_OBJ", 2, e);
      } 
      return null;
    } 
  }
  
  public Vector loadUserList(String serverGroup) {
    Vector listing = new Vector();
    File dir = new File(String.valueOf(System.getProperty("crushftp.users")) + serverGroup + "/");
    loadUserListRecurse(listing, dir.getPath());
    Collections.sort(listing, new Comparator(this) {
          final XMLUsers this$0;
          
          public int compare(Object a, Object b) {
            return a.toString().toUpperCase().compareTo(b.toString().toUpperCase());
          }
        });
    return listing;
  }
  
  public static Object username_lookup_lock = new Object();
  
  public Vector findUserEmail(String serverGroup, String email) {
    synchronized (username_lookup_lock) {
      Vector listing = loadUserList(serverGroup);
      Vector matchingUsernames = new Vector();
      for (int x = 0; x < listing.size(); x++) {
        try {
          Thread.sleep(1L);
          Thread.sleep((x / 100));
        } catch (InterruptedException interruptedException) {}
        String path = String.valueOf(System.getProperty("crushftp.users")) + serverGroup + "/" + listing.elementAt(x) + "/user.XML";
        if ((new File(path)).exists())
          try {
            if (listing.elementAt(x).toString().equalsIgnoreCase(email)) {
              Properties user = null;
              synchronized (user_lock) {
                user = (Properties)Common.readXMLObject(path);
              } 
              user.put("username", listing.elementAt(x));
              user.put("user_name", listing.elementAt(x));
              matchingUsernames.addElement(user);
            } else if (email.indexOf("@") >= 0) {
              RandomAccessFile in = null;
              byte[] b = (byte[])null;
              synchronized (user_lock) {
                in = new RandomAccessFile(path, "r");
                b = new byte[(int)in.length()];
                in.read(b);
                in.close();
              } 
              String s = (new String(b, "UTF8")).toUpperCase();
              if (s.indexOf(email.toUpperCase()) > 0) {
                Properties user = (Properties)Common.readXMLObject(new ByteArrayInputStream(b));
                user.put("username", listing.elementAt(x));
                user.put("user_name", listing.elementAt(x));
                matchingUsernames.addElement(user);
              } 
            } 
          } catch (Exception e) {
            Log.log("SERVER", 2, e);
          }  
      } 
      return matchingUsernames;
    } 
  }
  
  private void loadUserListRecurse(Vector listing, String path) {
    File dir = new File(path);
    File[] item_list = dir.listFiles();
    if (item_list != null)
      for (int x = 0; x < item_list.length; x++) {
        if (item_list[x].isDirectory() && !item_list[x].getName().equalsIgnoreCase("VFS")) {
          listing.addElement(item_list[x].getName());
          loadUserListRecurse(listing, item_list[x].getPath());
        } 
      }  
  }
  
  public void addFolder(String serverGroup, String username, String path, String name) {
    if (!path.endsWith("/"))
      path = String.valueOf(path) + "/"; 
    if (path.startsWith("/"))
      path = path.substring(1); 
    if (path.equals("/"))
      path = ""; 
    (new File(String.valueOf(getUserPath(serverGroup, username)) + "VFS/" + path + name)).mkdirs();
    Common.updateOSXInfo(String.valueOf(getUserPath(serverGroup, username)) + "VFS");
  }
  
  public void addItem(String serverGroup, String username, String path, String name, String url, String type, Properties moreItems, boolean encrypted, String encrypted_class) throws Exception {
    if (type.equalsIgnoreCase("DIR") && !url.endsWith("/"))
      url = String.valueOf(url) + "/"; 
    if (!path.endsWith("/"))
      path = String.valueOf(path) + "/"; 
    if (path.startsWith("/"))
      path = path.substring(1); 
    if (path.equals("/"))
      path = ""; 
    VRL vv = new VRL(url);
    Vector v = new Vector();
    Properties p = new Properties();
    v.addElement(p);
    p.put("type", type.toUpperCase());
    p.put("url", vv.toString());
    p.putAll(moreItems);
    if (encrypted) {
      p.put("encrypted", "true");
      p.put("encrypted_class", encrypted_class);
      if (!p.getProperty("encrypted_class", "").trim().equals("")) {
        try {
          Class c = Thread.currentThread().getContextClassLoader().loadClass(p.getProperty("encrypted_class").trim());
          Constructor cons = c.getConstructor(new Class[] { (new Properties()).getClass(), (new String()).getClass() });
          cons.newInstance(new Object[] { p, "encrypt" });
        } catch (Exception ee) {
          Log.log("USER_OBJ", 1, ee);
        } 
      } else {
        p.put("url", ServerStatus.thisObj.common_code.encode_pass(p.getProperty("url"), "DES", ""));
      } 
    } 
    Common.writeXMLObject(String.valueOf(getUserPath(serverGroup, username)) + "VFS/" + path + name, v, "VFS");
  }
}
