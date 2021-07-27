package crushftp.user;

import com.crushftp.client.VRL;
import crushftp.handlers.CIProperties;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.UserTools;
import crushftp.server.ServerSessionAJAX5_2;
import crushftp.server.ServerStatus;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.Vector;

public class SQLUsers extends UserProvider {
  static URLClassLoader cl = null;
  
  static Class drvCls = null;
  
  static Driver driver = null;
  
  public Properties settings = new Properties();
  
  public String lastDriver = "";
  
  public static boolean loadEvents4 = true;
  
  public static boolean for_menu_buttons = false;
  
  static Vector freeConnections = new Vector();
  
  Properties cache = new Properties();
  
  Properties userCache = new Properties();
  
  Vector cacheRevolver = new Vector();
  
  static Class class$0;
  
  public SQLUsers() {
    setDefaults(this.settings);
  }
  
  public static void setDefaults(Properties settings2) {
    settings2.put("db_debug", "true");
    settings2.put("db_read_only", "false");
    settings2.put("db_driver_file", "./mysql-connector-java-5.0.4-bin.jar");
    settings2.put("db_driver", "org.gjt.mm.mysql.Driver");
    settings2.put("db_url", "jdbc:mysql://127.0.0.1:3306/crushftp?autoReconnect=true");
    settings2.put("db_user", "crushftp");
    settings2.put("db_pass", "");
    settings2.put("db_user_query", "SELECT * FROM USERS WHERE SERVER_GROUP=? and UPPER(USERNAME)=UPPER(?)");
    settings2.put("db_user_insert", "INSERT INTO USERS (USERNAME,PASSWORD,SERVER_GROUP) VALUES(?,?,?)");
    settings2.put("db_user_update", "UPDATE USERS SET USERNAME=?, PASSWORD=? WHERE UPPER(USERNAME)=UPPER(?) AND SERVER_GROUP=?");
    settings2.put("db_user_delete", "DELETE FROM USERS WHERE UPPER(USERNAME)=UPPER(?) AND SERVER_GROUP=?");
    settings2.put("db_users_query", "SELECT * FROM USERS WHERE SERVER_GROUP=? order by USERNAME");
    settings2.put("db_user_inheritance_query", "SELECT * FROM INHERITANCE WHERE SERVER_GROUP=?");
    settings2.put("db_user_inheritance_delete", "DELETE FROM INHERITANCE WHERE USERID=?");
    settings2.put("db_user_inheritance_insert", "INSERT INTO INHERITANCE (USERID,INHERIT_USERNAME,SORT_ORDER) VALUES(?,?,?)");
    settings2.put("db_user_inheritance_id_query", "SELECT * FROM INHERITANCE WHERE USERID=?");
    settings2.put("db_user_properties_query", "SELECT * FROM USER_PROPERTIES WHERE USERID=?");
    settings2.put("db_user_properties_delete", "DELETE FROM USER_PROPERTIES WHERE USERID=?");
    settings2.put("db_user_properties_insert", "INSERT INTO USER_PROPERTIES (USERID,PROP_NAME,PROP_VALUE) VALUES(?,?,?)");
    settings2.put("db_user_user_properties_id_query", "SELECT * FROM USER_PROPERTIES WHERE USERID=?");
    settings2.put("db_user_events_delete", "DELETE FROM EVENTS5 WHERE USERID=?");
    settings2.put("db_user_events_insert", "INSERT INTO EVENTS5 (USERID,EVENT_NAME,PROP_NAME,PROP_VALUE) VALUES(?,?,?,?)");
    settings2.put("db_user_events_id_query", "SELECT * FROM EVENTS5 WHERE USERID=?");
    settings2.put("db_user_domain_root_list_delete", "DELETE FROM DOMAIN_ROOT_LIST WHERE USERID=?");
    settings2.put("db_user_domain_root_list_insert", "INSERT INTO DOMAIN_ROOT_LIST (USERID,DOMAIN,PATH,SORT_ORDER) VALUES(?,?,?,?)");
    settings2.put("db_user_domain_root_list_id_query", "SELECT * FROM DOMAIN_ROOT_LIST WHERE USERID=?");
    settings2.put("db_user_ip_restrictions_delete", "DELETE FROM IP_RESTRICTIONS WHERE USERID=?");
    settings2.put("db_user_ip_restrictions_insert", "INSERT INTO IP_RESTRICTIONS (USERID,START_IP,TYPE,STOP_IP,SORT_ORDER) VALUES(?,?,?,?,?)");
    settings2.put("db_user_ip_restrictions_id_query", "SELECT * FROM IP_RESTRICTIONS WHERE USERID=?");
    settings2.put("db_user_web_buttons_delete", "DELETE FROM WEB_BUTTONS WHERE USERID=?");
    settings2.put("db_user_web_buttons_insert", "INSERT INTO WEB_BUTTONS (USERID,SQL_FIELD_KEY,SQL_FIELD_VALUE,FOR_MENU,FOR_CONTEXT_MENU,SORT_ORDER) VALUES(?,?,?,?,?,?)");
    settings2.put("db_user_web_buttons_id_query", "SELECT * FROM WEB_BUTTONS WHERE USERID=?");
    settings2.put("db_user_web_customizations_delete", "DELETE FROM WEB_CUSTOMIZATIONS WHERE USERID=?");
    settings2.put("db_user_web_customizations_insert", "INSERT INTO WEB_CUSTOMIZATIONS (USERID,SQL_FIELD_KEY,SQL_FIELD_VALUE,SORT_ORDER) VALUES(?,?,?,?)");
    settings2.put("db_user_web_customizations_id_query", "SELECT * FROM WEB_CUSTOMIZATIONS WHERE USERID=?");
    settings2.put("db_user_vfs_delete", "DELETE FROM VFS WHERE USERID=?");
    settings2.put("db_user_vfs_properties_delete", "DELETE FROM VFS_PROPERTIES WHERE USERID=?");
    settings2.put("db_user_vfs_vdir_insert", "INSERT INTO VFS (USERID,URL,TYPE,PATH,SORT_ORDER) VALUES(?,?,?,?,?)");
    settings2.put("db_user_vfs_insert", "INSERT INTO VFS (USERID,URL,TYPE,PATH,SORT_ORDER) VALUES(?,?,?,?,?)");
    settings2.put("db_user_vfs_properties_insert", "INSERT INTO VFS_PROPERTIES (USERID,PATH,PROP_NAME,PROP_VALUE) VALUES(?,?,?,?)");
    settings2.put("db_user_vfs_id_query", "SELECT * FROM VFS WHERE USERID=?");
    settings2.put("db_user_vfs_properties_id_query", "SELECT * FROM VFS_PROPERTIES WHERE USERID=?");
    settings2.put("db_user_vfs_permissions_delete", "DELETE FROM VFS_PERMISSIONS WHERE USERID=?");
    settings2.put("db_user_vfs_permissions_insert", "INSERT INTO VFS_PERMISSIONS (USERID,PATH,PRIVS) VALUES(?,?,?)");
    settings2.put("db_user_vfs_permissions_query", "SELECT * FROM VFS_PERMISSIONS WHERE USERID=?");
    settings2.put("db_user_vfs_permissions_id_query", "SELECT * FROM VFS_PERMISSIONS WHERE USERID=?");
    settings2.put("db_groups_query", "SELECT * FROM GROUPS G, USERS U WHERE G.USERID = U.USERID AND G.SERVER_GROUP=?");
    settings2.put("db_groups_delete", "DELETE FROM GROUPS WHERE SERVER_GROUP=?");
    settings2.put("db_groups_insert", "INSERT INTO GROUPS (GROUPNAME, USERID, SERVER_GROUP) VALUES(?,?,?)");
    settings2.put("db_modified_query", "SELECT * FROM MODIFIED_TIMES WHERE SERVER_GROUP=? AND PROP_NAME=?");
    settings2.put("db_modified_insert", "INSERT INTO MODIFIED_TIMES (SERVER_GROUP,PROP_NAME,PROP_VALUE) VALUES(?,?,?)");
    settings2.put("db_modified_delete", "DELETE FROM MODIFIED_TIMES WHERE SERVER_GROUP=? AND PROP_NAME=?");
  }
  
  public void setSettings(Properties p) {
    fixSql(p);
    this.settings.putAll(p);
  }
  
  public static void fixSql(Properties settings) {
    Enumeration keys = settings.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      String val = settings.getProperty(key);
      if (val.startsWith("DELETE USERS "))
        val = "DELETE FROM USERS " + val.substring("DELETE USERS ".length()); 
      if (val.equals("SELECT * FROM USERS WHERE SERVER_GROUP=?"))
        val = "SELECT * FROM USERS WHERE SERVER_GROUP=? order by USERNAME"; 
      if (val.equals("SELECT * FROM USERS WHERE SERVER_GROUP=? and USERNAME=?"))
        val = "SELECT * FROM USERS WHERE SERVER_GROUP=? and UPPER(USERNAME)=UPPER(?)"; 
      if (val.equals("SELECT * FROM USERS WHERE SERVER_GROUP=? and USERNAME=UPPER(?)"))
        val = "SELECT * FROM USERS WHERE SERVER_GROUP=? and UPPER(USERNAME)=UPPER(?)"; 
      if (val.equals("DELETE FROM USERS WHERE USERNAME=? AND SERVER_GROUP=?"))
        val = "DELETE FROM USERS WHERE UPPER(USERNAME)=UPPER(?) AND SERVER_GROUP=?"; 
      if (val.equals("UPDATE USERS SET USERNAME=?, PASSWORD=? WHERE USERNAME=? AND SERVER_GROUP=?"))
        val = "UPDATE USERS SET USERNAME=?, PASSWORD=? WHERE UPPER(USERNAME)=UPPER(?) AND SERVER_GROUP=?"; 
      if (val.equals("INSERT INTO WEB_BUTTONS (USERID,SQL_FIELD_KEY,SQL_FIELD_VALUE,SORT_ORDER) VALUES(?,?,?,?)"))
        val = "INSERT INTO WEB_BUTTONS (USERID,SQL_FIELD_KEY,SQL_FIELD_VALUE,FOR_MENU,FOR_CONTEXT_MENU,SORT_ORDER) VALUES(?,?,?,?,?,?)"; 
      settings.put(key, val);
    } 
  }
  
  public Properties buildVFS(String serverGroup, String username) {
    CIProperties virtual = new CIProperties();
    Properties permissions0 = new Properties();
    msg("Connecting to db.");
    Connection conn = null;
    try {
      conn = getConnection();
      Properties user = findUser(conn, serverGroup, username);
      if (user == null)
        return null; 
      String id = user.getProperty("id");
      Statement statement = conn.createStatement();
      loadTable(id, "VFS_PERMISSIONS", permissions0, statement, true, null, null, "PATH", "PRIVS");
      Vector permissions = new Vector();
      permissions.addElement(permissions0);
      virtual.put("vfs_permissions_object", permissions);
      Properties vfs = new Properties();
      loadTable(id, "VFS", vfs, statement, false, "SORT_ORDER", null, "", "");
      Properties vfs_properties = new Properties();
      try {
        loadTable(id, "VFS_PROPERTIES", vfs_properties, statement, false, null, null, "", "");
      } catch (Throwable e) {
        msg(e);
      } 
      Vector vfs_properties_list = (Vector)vfs_properties.get("VFS_PROPERTIES");
      if (vfs_properties_list == null)
        vfs_properties_list = new Vector(); 
      statement.close();
      Vector v = (Vector)vfs.get("VFS");
      if (v != null)
        for (int x = 0; x < v.size(); x++) {
          Properties p = v.elementAt(x);
          String path = p.getProperty("path");
          for (int xx = 0; xx < vfs_properties_list.size(); xx++) {
            Properties properties = vfs_properties_list.elementAt(xx);
            if (properties.getProperty("path").equals(p.getProperty("path")))
              p.put(properties.getProperty("prop_name"), properties.getProperty("prop_value")); 
          } 
          if (!path.startsWith("/"))
            path = "/" + path; 
          p.remove("path");
          p.put("url", Common.url_decode(p.getProperty("url")));
          Properties pp = new Properties();
          pp.put("name", Common.last(path));
          if (!pp.getProperty("name").equals("") && !pp.getProperty("name").equals("/")) {
            if (p.getProperty("type").equalsIgnoreCase("vdir")) {
              pp.put("type", "DIR");
            } else {
              pp.put("type", "FILE");
              Vector vv = new Vector();
              vv.addElement(p);
              pp.put("vItems", vv);
            } 
            pp.put("virtualPath", path);
            virtual.put(path, pp);
          } 
        }  
      if (!virtual.containsKey("/")) {
        Properties p = new Properties();
        p.put("virtualPath", "/");
        p.put("path", "/");
        p.put("name", "VFS");
        p.put("type", "DIR");
        p.put("vItems", new Vector());
        virtual.put("/", p);
      } 
    } catch (Throwable e) {
      msg(e);
      virtual = null;
      try {
        conn.close();
      } catch (Exception exception) {}
    } finally {
      releaseConnection(conn);
    } 
    return virtual;
  }
  
  private Properties findUser(Connection conn, String serverGroup, String username) throws Exception {
    Properties user = null;
    ResultSet rs = null;
    PreparedStatement ps = null;
    try {
      ps = conn.prepareStatement(this.settings.getProperty("db_user_query"));
      msg("Connected.");
      ps.setString(1, serverGroup);
      ps.setString(2, username);
      rs = ps.executeQuery();
      msg("Querying DB for user:" + this.settings.getProperty("db_user_query"));
      if (rs.next()) {
        msg("Found user.");
        user = new Properties();
        user.put("id", rs.getString("USERID"));
        user.put("username", rs.getString("USERNAME"));
        user.put("password", (rs.getString("PASSWORD") == null) ? "" : rs.getString("PASSWORD"));
      } 
      rs.close();
      msg("Done loading user info.");
    } finally {
      ps.close();
    } 
    return user;
  }
  
  public Properties loadGroups(String serverGroup) {
    msg("Loading groups from DB");
    Properties groups = new Properties();
    Connection conn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    Statement s = null;
    try {
      msg("Connecting to db.");
      conn = getConnection();
      s = conn.createStatement();
      ps = conn.prepareStatement(this.settings.getProperty("db_groups_query"));
      msg("Connected.");
      ps.setString(1, serverGroup);
      rs = ps.executeQuery();
      while (rs.next()) {
        Vector v = (Vector)groups.get(rs.getString("GROUPNAME"));
        if (v == null)
          v = new Vector(); 
        groups.put(rs.getString("GROUPNAME"), v);
        v.addElement(rs.getString("USERNAME"));
      } 
    } catch (Throwable e) {
      msg(e);
      groups = null;
    } finally {
      try {
        if (rs != null)
          rs.close(); 
      } catch (Exception exception) {}
      try {
        if (s != null)
          s.close(); 
      } catch (Exception exception) {}
      try {
        if (ps != null)
          ps.close(); 
      } catch (Exception exception) {}
      try {
        conn.close();
      } catch (Exception exception) {}
      releaseConnection(conn);
    } 
    return groups;
  }
  
  public void writeGroups(String serverGroup, Properties groups) {
    if (this.settings.getProperty("db_read_only", "false").equals("true"))
      return; 
    msg("Writing groups to DB");
    Connection conn = null;
    PreparedStatement ps = null;
    PreparedStatement insertGroup = null;
    try {
      msg("Connecting to db.");
      conn = getConnection();
      ResultSet rs = null;
      ps = conn.prepareStatement(this.settings.getProperty("db_groups_delete"));
      ps.setString(1, serverGroup);
      msg("Connected.");
      ps.executeUpdate();
      ps.close();
      ps = conn.prepareStatement(this.settings.getProperty("db_users_query"));
      msg("Connected.");
      ps.setString(1, serverGroup);
      rs = ps.executeQuery();
      String id = "-1";
      msg("Querying DB for user:" + this.settings.getProperty("db_users_query"));
      insertGroup = conn.prepareStatement(this.settings.getProperty("db_groups_insert"));
      while (rs.next()) {
        String username = rs.getString("USERNAME");
        id = rs.getString("USERID");
        Enumeration keys = groups.keys();
        while (keys.hasMoreElements()) {
          String key = keys.nextElement().toString();
          Vector v = (Vector)groups.get(key);
          if (v.indexOf(username) >= 0) {
            msg("Adding user (" + id + ") to group:" + key);
            insertGroup.setString(1, key);
            insertGroup.setString(2, id);
            insertGroup.setString(3, serverGroup);
            insertGroup.executeUpdate();
          } 
        } 
      } 
      insertGroup.close();
      rs.close();
    } catch (Throwable e) {
      msg(e);
      try {
        conn.close();
      } catch (Exception exception) {}
    } finally {
      try {
        if (ps != null)
          ps.close(); 
        if (insertGroup != null)
          insertGroup.close(); 
      } catch (SQLException sQLException) {}
      releaseConnection(conn);
    } 
  }
  
  public void writeVFS(String serverGroup, String username, Properties virtual) {
    writeVFS(serverGroup, username, virtual, true);
  }
  
  private void writeVFS(String serverGroup, String username, Properties virtual, boolean deleteFirst) {
    if (this.settings.getProperty("db_read_only", "false").equals("true"))
      return; 
    Connection conn = null;
    PreparedStatement ps = null;
    PreparedStatement ps2 = null;
    try {
      msg("Connecting to db.");
      conn = getConnection();
      Properties user = findUser(conn, serverGroup, username);
      if (user == null)
        return; 
      String id = user.getProperty("id");
      Vector permissions = (Vector)virtual.get("vfs_permissions_object");
      if (permissions != null) {
        Properties permissions0 = new Properties();
        permissions0.put("/", "(read)(view)(resume)");
        if (permissions != null && permissions.size() > 0)
          permissions0 = permissions.elementAt(0); 
        if (deleteFirst) {
          msg("Deleting user db_user_vfs_permissions_delete:" + id);
          ps = conn.prepareStatement(this.settings.getProperty("db_user_vfs_permissions_delete"));
          ps.setString(1, id);
          ps.executeUpdate();
          ps.close();
        } 
        msg("Writing user db_user_vfs_permissions_insert:" + id);
        ps = conn.prepareStatement(this.settings.getProperty("db_user_vfs_permissions_insert"));
        Enumeration enumeration = permissions0.keys();
        while (enumeration.hasMoreElements()) {
          String key = enumeration.nextElement().toString();
          ps.setString(1, id);
          ps.setString(2, key.toUpperCase());
          ps.setString(3, permissions0.getProperty(key));
          ps.execute();
        } 
        ps.close();
      } 
      if (deleteFirst) {
        msg("Deleting user db_user_vfs_delete:" + id);
        ps = conn.prepareStatement(this.settings.getProperty("db_user_vfs_delete"));
        ps.setString(1, id);
        ps.executeUpdate();
        ps.close();
        msg("Deleting user db_user_vfs_properties_delete:" + id);
        try {
          ps = conn.prepareStatement(this.settings.getProperty("db_user_vfs_properties_delete"));
          ps.setString(1, id);
          ps.executeUpdate();
          ps.close();
        } catch (Throwable e) {
          msg(e);
        } 
      } 
      Enumeration keys = virtual.keys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement().toString();
        if (key.equals("vfs_permissions_object"))
          continue; 
        Properties pp = (Properties)virtual.get(key);
        String virtualPath = pp.getProperty("virtualPath");
        if (pp.getProperty("type").equalsIgnoreCase("DIR")) {
          msg("Writing VFS (dir) item " + virtualPath + ":" + this.settings.getProperty("db_user_vfs_vdir_insert"));
          ps2 = conn.prepareStatement(this.settings.getProperty("db_user_vfs_vdir_insert"));
          ps2.setString(1, id);
          ps2.setString(2, "");
          ps2.setString(3, "vdir");
          ps2.setString(4, virtualPath);
          ps2.setString(5, "0");
          ps2.execute();
          ps2.close();
          continue;
        } 
        msg("Writing VFS (file) item " + virtualPath + ":" + this.settings.getProperty("db_user_vfs_insert"));
        Vector v = (Vector)pp.get("vItems");
        for (int x = 0; x < v.size(); x++) {
          Properties p = v.elementAt(x);
          p.put("path", virtualPath);
          String sql_insert = this.settings.getProperty("db_user_vfs_insert");
          ps2 = conn.prepareStatement(sql_insert);
          String[] fields = sql_insert.substring(sql_insert.indexOf("(") + 1, sql_insert.indexOf(")")).split(",");
          for (int xx = 0; xx < fields.length; xx++) {
            if (fields[xx].trim().equalsIgnoreCase("USERID")) {
              ps2.setString(xx + 1, id);
            } else if (fields[xx].trim().equalsIgnoreCase("SORT_ORDER")) {
              ps2.setString(xx + 1, (new StringBuffer(String.valueOf(x))).toString());
            } else {
              String key2 = fields[xx].trim();
              if (key2.toUpperCase().startsWith("ORACLE_"))
                key2 = key2.substring("ORACLE_".length()); 
              if (key2.toUpperCase().startsWith("SQL_FIELD_"))
                key2 = key2.substring("SQL_FIELD_".length()); 
              ps2.setString(xx + 1, p.getProperty(key2.toLowerCase()));
            } 
          } 
          ps2.execute();
          ps2.close();
          sql_insert = this.settings.getProperty("db_user_vfs_properties_insert");
          ps2 = conn.prepareStatement(sql_insert);
          Enumeration keys2 = p.keys();
          while (keys2.hasMoreElements()) {
            String key2 = keys2.nextElement().toString();
            if (key2.equalsIgnoreCase("userid") || key2.equalsIgnoreCase("url") || key2.equalsIgnoreCase("path") || key2.equalsIgnoreCase("type") || key2.equalsIgnoreCase("sort_order"))
              continue; 
            String val2 = p.get(key2).toString();
            ps2.setString(1, id);
            ps2.setString(2, virtualPath);
            ps2.setString(3, key2);
            ps2.setString(4, val2);
            try {
              ps2.execute();
            } catch (Throwable e) {
              msg(e);
            } 
          } 
          ps2.close();
        } 
      } 
    } catch (Throwable e) {
      msg(e);
      try {
        conn.close();
      } catch (Exception exception) {}
    } finally {
      try {
        if (ps != null)
          ps.close(); 
        if (ps2 != null)
          ps2.close(); 
      } catch (SQLException sQLException) {}
      releaseConnection(conn);
    } 
  }
  
  public Properties loadInheritance(String serverGroup) {
    msg("Loading inheritance from DB");
    Properties inheritanceProp = new Properties();
    Connection conn = null;
    PreparedStatement ps = null;
    Statement statement = null;
    Statement s = null;
    try {
      String m1 = this.cache.getProperty(String.valueOf(serverGroup) + "_INHERITANCE");
      String m2 = getModified(serverGroup, "INHERITANCE");
      String cacheId = String.valueOf(serverGroup) + "_INHERITANCE";
      if (m1 != null && m2 != null && m1.equals(m2) && this.userCache.containsKey(cacheId)) {
        inheritanceProp = (Properties)this.userCache.get(cacheId);
        inheritanceProp = (Properties)Common.CLONE(inheritanceProp);
        msg("Loaded inheritance from cache.");
      } else {
        if (m1 != null && m2 != null && !m1.equals(m2)) {
          resetCache(serverGroup, "USERS");
          this.cache.put(String.valueOf(serverGroup) + "_INHERITANCE", getModified(serverGroup, "INHERITANCE"));
        } 
        if (m1 == null && m2 != null)
          this.cache.put(String.valueOf(serverGroup) + "_INHERITANCE", m2); 
        msg("Connecting to db.");
        conn = getConnection();
        ResultSet rs = null;
        s = conn.createStatement();
        ps = conn.prepareStatement(this.settings.getProperty("db_users_query"));
        msg("Connected.");
        ps.setString(1, serverGroup);
        rs = ps.executeQuery();
        String id = "-1";
        msg("Querying DB for user:" + this.settings.getProperty("db_user_query"));
        statement = conn.createStatement();
        while (rs.next()) {
          String username = rs.getString("USERNAME");
          id = rs.getString("USERID");
          msg("Loading user inheritance...");
          Properties p = new Properties();
          loadTable(id, "INHERITANCE", p, statement, false, "SORT_ORDER", null, "", "");
          Vector v = (Vector)p.remove("INHERITANCE");
          Vector inheritance = new Vector();
          if (v != null)
            for (int x = 0; x < v.size(); x++)
              inheritance.addElement(((Properties)v.elementAt(x)).getProperty("inherit_username"));  
          inheritanceProp.put(username, inheritance);
        } 
        rs.close();
        ps.close();
        s.close();
        statement.close();
        cacheItem(cacheId, inheritanceProp);
      } 
    } catch (Throwable e) {
      msg(e);
      try {
        conn.close();
      } catch (Exception exception) {}
    } finally {
      try {
        if (ps != null)
          ps.close(); 
        if (s != null)
          s.close(); 
        if (statement != null)
          statement.close(); 
      } catch (SQLException sQLException) {}
      releaseConnection(conn);
    } 
    return inheritanceProp;
  }
  
  public Properties loadUser(String serverGroup, String username, Properties inheritance, boolean flattenUser) {
    msg("Loading user from DB:" + username);
    boolean found_user = false;
    Connection conn = null;
    Properties user = new Properties();
    if (username.trim().length() > 0) {
      PreparedStatement ps = null;
      Statement statement = null;
      try {
        String m1 = this.cache.getProperty(String.valueOf(serverGroup) + "_USERS");
        String m2 = getModified(serverGroup, "USERS");
        String cacheId = String.valueOf(serverGroup) + "_" + username.toUpperCase() + "_" + flattenUser;
        if (m1 != null && m2 != null && m1.equals(m2) && this.userCache.containsKey(cacheId)) {
          user = (Properties)this.userCache.get(cacheId);
          user = (Properties)Common.CLONE(user);
          msg("Loaded user from cache.");
          found_user = true;
        } else {
          if (m1 != null && m2 != null && !m1.equals(m2)) {
            resetCache(serverGroup, "USERS");
            this.cache.put(String.valueOf(serverGroup) + "_USERS", getModified(serverGroup, "USERS"));
          } 
          if (m1 == null && m2 != null)
            this.cache.put(String.valueOf(serverGroup) + "_USERS", m2); 
          msg("Connecting to db.");
          conn = getConnection();
          ResultSet rs = null;
          ps = conn.prepareStatement(this.settings.getProperty("db_user_query"));
          msg("Connected.");
          ps.setString(1, serverGroup);
          ps.setString(2, username);
          rs = ps.executeQuery();
          String id = "-1";
          msg("Querying DB for user:" + this.settings.getProperty("db_user_query"));
          if (rs.next()) {
            msg("Found user.");
            found_user = true;
            id = rs.getString("USERID");
            user.put("username", rs.getString("USERNAME"));
            user.put("password", (rs.getString("PASSWORD") == null) ? "" : rs.getString("PASSWORD"));
          } 
          rs.close();
          msg("Done loading user info.");
          statement = conn.createStatement();
          if (id.equals("-1")) {
            user = null;
          } else {
            Enumeration keys = inheritance.keys();
            Vector ichain = null;
            while (keys.hasMoreElements()) {
              String key = keys.nextElement().toString();
              if (key.equalsIgnoreCase(username)) {
                ichain = (Vector)inheritance.get(key);
                break;
              } 
            } 
            if (ichain == null || ichain.size() == 0) {
              ichain = new Vector();
              ichain.addElement("default");
            } 
            if (ichain.size() == 1 && !ichain.elementAt(0).toString().equals("default"))
              ichain.insertElementAt("default", 0); 
            for (int x = 0; x < ichain.size(); x++) {
              String iusername = ichain.elementAt(x).toString();
              msg("Loading inherited user settings...:" + iusername);
              ps.setString(1, serverGroup);
              ps.setString(2, iusername);
              rs = ps.executeQuery();
              msg("Querying DB for inherited userid:" + this.settings.getProperty("db_user_query"));
              if (rs.next()) {
                msg("Got inherited userid...:" + rs.getString("USERID"));
                Properties properties1 = new Properties();
                loadUserItems(rs.getString("USERID"), properties1, statement);
                properties1.remove("password");
                Properties originalUser = (Properties)user.clone();
                UserTools.mergeWebCustomizations(user, properties1);
                UserTools.mergeLinkedVFS(user, properties1);
                try {
                  boolean needWrite = false;
                  if (user.getProperty("account_expire", "").equals("") && !properties1.getProperty("account_expire", "").equals("") && !properties1.getProperty("account_expire_rolling_days", "0").equals("") && Integer.parseInt(properties1.getProperty("account_expire_rolling_days", "0")) > 0) {
                    GregorianCalendar gc = new GregorianCalendar();
                    gc.setTime(new Date());
                    gc.add(5, Integer.parseInt(properties1.getProperty("account_expire_rolling_days")));
                    properties1.put("account_expire", (new SimpleDateFormat("MM/dd/yy hh:mm aa")).format(gc.getTime()));
                    originalUser.put("account_expire", properties1.getProperty("account_expire"));
                    originalUser.put("account_expire_rolling_days", properties1.getProperty("account_expire_rolling_days"));
                    needWrite = true;
                  } 
                  if (user.getProperty("account_expire", "").equals("") && !properties1.getProperty("account_expire", "").equals("")) {
                    GregorianCalendar gc = new GregorianCalendar();
                    gc.setTime(new Date());
                    gc.add(5, Integer.parseInt(properties1.getProperty("expire_password_days")));
                    properties1.put("expire_password_when", (new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa")).format(gc.getTime()));
                    originalUser.put("expire_password_when", properties1.getProperty("expire_password_when"));
                    originalUser.put("expire_password_days", properties1.getProperty("expire_password_days"));
                    originalUser.put("expire_password", properties1.getProperty("expire_password"));
                    needWrite = true;
                  } 
                  if (needWrite)
                    writeUser(serverGroup, username, originalUser); 
                } catch (Exception e) {
                  Common.debug(1, e);
                } 
                if (flattenUser)
                  user.putAll(properties1); 
              } 
              rs.close();
            } 
            user.put("username", username);
            user.put("user_name", username);
            Properties pp = new Properties();
            loadUserItems(id, pp, statement);
            pp.remove("password");
            user.putAll(pp);
          } 
          ps.close();
          statement.close();
          if (found_user)
            cacheItem(cacheId, user); 
        } 
      } catch (Throwable e) {
        msg(e);
        try {
          conn.close();
        } catch (Exception exception) {}
      } finally {
        try {
          if (ps != null)
            ps.close(); 
          if (statement != null)
            statement.close(); 
        } catch (SQLException sQLException) {}
        releaseConnection(conn);
      } 
    } 
    if (found_user) {
      msg("Loaded user.");
    } else {
      msg("User not found.");
    } 
    return user;
  }
  
  public void updateUser(String serverGroup, String username1, String username2, String password) {
    if (this.settings.getProperty("db_read_only", "false").equals("true"))
      return; 
    if (username2 != null) {
      msg("Updating user in DB:" + username2);
      Connection conn = null;
      if (username1.trim().length() > 0 && username2.trim().length() > 0) {
        PreparedStatement ps = null;
        try {
          msg("Connecting to db.");
          conn = getConnection();
          ps = conn.prepareStatement(this.settings.getProperty("db_user_update"));
          msg("Connected.");
          ps.setString(1, username2);
          ps.setString(2, password);
          ps.setString(3, username1);
          ps.setString(4, serverGroup);
          ps.executeUpdate();
          ps.close();
        } catch (Throwable e) {
          msg(e);
          try {
            conn.close();
          } catch (Exception exception) {}
          throw new RuntimeException(e);
        } finally {
          try {
            if (ps != null)
              ps.close(); 
          } catch (SQLException sQLException) {}
          releaseConnection(conn);
        } 
        resetCache(serverGroup, "USERS");
      } 
    } 
  }
  
  public void writeUser(String serverGroup, String username, Properties user) {
    if (this.settings.getProperty("db_read_only", "false").equals("true"))
      return; 
    msg("Writing user to DB:" + username);
    user = (Properties)user.clone();
    Vector lvsv = (Vector)user.get("linked_vfs");
    if (lvsv == null)
      lvsv = new Vector(); 
    String lvs = "";
    for (int x = 0; x < lvsv.size(); x++)
      lvs = String.valueOf(lvs) + lvsv.elementAt(x).toString().trim() + ","; 
    if (lvs.length() > 0)
      lvs = lvs.substring(0, lvs.length() - 1); 
    user.put("linked_vfs", lvs);
    if (lvsv.size() == 0)
      user.remove("linked_vfs"); 
    Connection conn = null;
    if (username.trim().length() > 0) {
      PreparedStatement ps = null;
      PreparedStatement ps2 = null;
      PreparedStatement event_delete = null;
      PreparedStatement event_insert = null;
      PreparedStatement ps_insert_user_properties = null;
      try {
        msg("Connecting to db.");
        conn = getConnection();
        ResultSet rs = null;
        ps = conn.prepareStatement(this.settings.getProperty("db_user_query"));
        msg("Connected.");
        ps.setString(1, serverGroup);
        ps.setString(2, username);
        rs = ps.executeQuery();
        String id = "-1";
        msg("Querying DB for user:" + this.settings.getProperty("db_user_query"));
        if (rs.next()) {
          id = rs.getString("USERID");
          msg("Found user:" + id);
          rs.close();
          updateUser(serverGroup, username, username, user.getProperty("password"));
        } else {
          rs.close();
          ps2 = conn.prepareStatement(this.settings.getProperty("db_user_insert"));
          String pass = user.getProperty("password", "");
          if (pass.equals(""))
            pass = Common.makeBoundary(); 
          ps2.setString(1, username);
          ps2.setString(2, pass);
          ps2.setString(3, serverGroup);
          ps2.execute();
          ps2.close();
          rs = ps.executeQuery();
          rs.next();
          id = rs.getString("USERID");
          msg("Created new user:" + id);
          rs.close();
        } 
        ps.close();
        deleteUserProperties(id, conn);
        ps_insert_user_properties = conn.prepareStatement(this.settings.getProperty("db_user_properties_insert"));
        Enumeration keys = user.keys();
        while (keys.hasMoreElements()) {
          String key = keys.nextElement().toString();
          Object val = user.get(key);
          if (key.equalsIgnoreCase("EVENTS")) {
            msg("Writing table:" + key);
            Vector v = (Vector)user.get(key);
            if (this.settings.getProperty("db_user_" + key.toLowerCase() + "_delete").toUpperCase().indexOf("EVENTS5") >= 0) {
              event_delete = conn.prepareStatement(this.settings.getProperty("db_user_" + key.toLowerCase() + "_delete"));
              event_delete.setString(1, id);
              event_delete.executeUpdate();
              event_delete.close();
              event_insert = conn.prepareStatement(this.settings.getProperty("db_user_" + key.toLowerCase() + "_insert"));
              for (int i = 0; i < v.size(); i++) {
                Properties event = v.elementAt(i);
                Enumeration event_keys = event.keys();
                while (event_keys.hasMoreElements()) {
                  String event_key = event_keys.nextElement().toString();
                  String event_val = "";
                  if (event.get(event_key) instanceof Vector || event.get(event_key) instanceof Properties) {
                    event_val = ServerStatus.thisObj.common_code.getXMLString(event.get(event_key), event_key, null);
                  } else {
                    event_val = event.getProperty(event_key, "");
                  } 
                  if (event_val.length() > 2000)
                    event_val = event_val.substring(0, 1999); 
                  String event_name = event.getProperty("name", "");
                  if (event_name.equals(""))
                    event_name = "noname_" + Common.makeBoundary(); 
                  event_insert.setString(1, id);
                  event_insert.setString(2, event_name);
                  event_insert.setString(3, event_key);
                  event_insert.setString(4, event_val);
                  event_insert.executeUpdate();
                } 
              } 
              event_insert.close();
              continue;
            } 
            doTableDeleteInsert(id, v, conn, this.settings.getProperty("db_user_" + key.toLowerCase() + "_delete"), this.settings.getProperty("db_user_" + key.toLowerCase() + "_insert"));
            continue;
          } 
          if (key.equalsIgnoreCase("IP_RESTRICTIONS")) {
            msg("Writing table:" + key);
            Vector v = (Vector)user.get(key);
            doTableDeleteInsert(id, v, conn, this.settings.getProperty("db_user_" + key.toLowerCase() + "_delete"), this.settings.getProperty("db_user_" + key.toLowerCase() + "_insert"));
            continue;
          } 
          if (key.equalsIgnoreCase("WEB_BUTTONS")) {
            msg("Writing table:" + key);
            Vector v = (Vector)user.get(key);
            ServerSessionAJAX5_2.fixButtons(v);
            if (for_menu_buttons || username.equalsIgnoreCase("default")) {
              try {
                doTableDeleteInsert(id, v, conn, this.settings.getProperty("db_user_" + key.toLowerCase() + "_delete"), this.settings.getProperty("db_user_" + key.toLowerCase() + "_insert"));
              } catch (SQLException e) {
                Log.log("USER", 0, "SQL table WEB_BUTTONS is missing required columns.  Please update the schema.  Buttons not saved.");
              } 
              continue;
            } 
            Log.log("USER", 1, "SQL table WEB_BUTTONS is missing required columns.  Please update the schema.");
            continue;
          } 
          if (key.equalsIgnoreCase("WEB_CUSTOMIZATIONS")) {
            msg("Writing table:" + key);
            Vector v = (Vector)user.get(key);
            doTableDeleteInsert(id, v, conn, this.settings.getProperty("db_user_" + key.toLowerCase() + "_delete"), this.settings.getProperty("db_user_" + key.toLowerCase() + "_insert"));
            continue;
          } 
          if (key.equalsIgnoreCase("DOMAIN_ROOT_LIST")) {
            msg("Writing table:" + key);
            Vector v = (Vector)user.get(key);
            doTableDeleteInsert(id, v, conn, this.settings.getProperty("db_user_" + key.toLowerCase() + "_delete"), this.settings.getProperty("db_user_" + key.toLowerCase() + "_insert"));
            continue;
          } 
          msg("Writing property:" + key + "=" + val);
          ps_insert_user_properties.setString(1, id);
          ps_insert_user_properties.setString(2, key);
          ps_insert_user_properties.setString(3, val.toString());
          if (!key.equals("password"))
            ps_insert_user_properties.execute(); 
        } 
        ps_insert_user_properties.close();
        msg("Finished writing user.");
      } catch (Throwable e) {
        msg(e);
        try {
          conn.close();
        } catch (Exception exception) {}
        throw new RuntimeException(e);
      } finally {
        try {
          if (ps != null)
            ps.close(); 
          if (ps2 != null)
            ps2.close(); 
          if (event_delete != null)
            event_delete.close(); 
          if (event_insert != null)
            event_insert.close(); 
          if (ps_insert_user_properties != null)
            ps_insert_user_properties.close(); 
        } catch (SQLException sQLException) {}
        releaseConnection(conn);
      } 
    } 
    resetCache(serverGroup, "USERS");
  }
  
  public void writeInheritance(String serverGroup, Properties inheritance) {
    if (this.settings.getProperty("db_read_only", "false").equals("true"))
      return; 
    msg("Writing inheritance to DB");
    Connection conn = null;
    PreparedStatement ps = null;
    PreparedStatement deleteInheritance = null;
    PreparedStatement insertInheritance = null;
    try {
      msg("Connecting to db.");
      conn = getConnection();
      ResultSet rs = null;
      ps = conn.prepareStatement(this.settings.getProperty("db_users_query"));
      msg("Connected.");
      ps.setString(1, serverGroup);
      rs = ps.executeQuery();
      String id = "-1";
      msg("Querying DB for users:" + this.settings.getProperty("db_users_query"));
      deleteInheritance = conn.prepareStatement(this.settings.getProperty("db_user_inheritance_delete"));
      insertInheritance = conn.prepareStatement(this.settings.getProperty("db_user_inheritance_insert"));
      while (rs.next()) {
        String username = rs.getString("USERNAME");
        id = rs.getString("USERID");
        msg("Deleting old user inheritance for userid:" + id);
        deleteInheritance.setString(1, id);
        deleteInheritance.execute();
        Vector v = (Vector)inheritance.get(username);
        if (v != null) {
          Vector addedUsers = new Vector();
          for (int x = 0; x < v.size(); x++) {
            msg("Inserting user inheritance for userid:" + id + "  " + x);
            insertInheritance.setString(1, id);
            insertInheritance.setString(2, v.elementAt(x).toString());
            insertInheritance.setString(3, (new StringBuffer(String.valueOf(x))).toString());
            if (addedUsers.indexOf(v.elementAt(x).toString()) < 0)
              insertInheritance.execute(); 
            addedUsers.addElement(v.elementAt(x).toString());
          } 
        } 
      } 
      insertInheritance.close();
      deleteInheritance.close();
      rs.close();
      ps.close();
    } catch (Throwable e) {
      msg(e);
      try {
        conn.close();
      } catch (Exception exception) {}
    } finally {
      try {
        if (ps != null)
          ps.close(); 
        if (deleteInheritance != null)
          deleteInheritance.close(); 
        if (insertInheritance != null)
          insertInheritance.close(); 
      } catch (SQLException sQLException) {}
      releaseConnection(conn);
    } 
    resetCache(serverGroup, "INHERITANCE");
  }
  
  public void deleteUser(String serverGroup, String username) {
    if (this.settings.getProperty("db_read_only", "false").equals("true"))
      return; 
    msg("Deleting user in DB:" + username);
    Connection conn = null;
    if (username.trim().length() > 0) {
      PreparedStatement ps = null;
      try {
        msg("Connecting to db.");
        conn = getConnection();
        ResultSet rs = null;
        ps = conn.prepareStatement(this.settings.getProperty("db_user_query"));
        msg("Connected.");
        ps.setString(1, serverGroup);
        ps.setString(2, username);
        rs = ps.executeQuery();
        String id = "-1";
        msg("Querying DB for user:" + this.settings.getProperty("db_user_query"));
        boolean found_user = false;
        if (rs.next()) {
          msg("Found user.");
          id = rs.getString("USERID");
          found_user = true;
        } 
        rs.close();
        msg("Done loading user info.");
        ps.close();
        if (found_user) {
          msg("Deleting user db_user_inheritance_delete:" + id);
          ps = conn.prepareStatement(this.settings.getProperty("db_user_inheritance_delete"));
          ps.setString(1, id);
          ps.executeUpdate();
          ps.close();
          deleteUserProperties(id, conn);
          msg("Deleting user db_user_vfs_delete:" + id);
          ps = conn.prepareStatement(this.settings.getProperty("db_user_vfs_delete"));
          ps.setString(1, id);
          ps.executeUpdate();
          ps.close();
          msg("Deleting user db_user_vfs_permissions_delete:" + id);
          ps = conn.prepareStatement(this.settings.getProperty("db_user_vfs_permissions_delete"));
          ps.setString(1, id);
          ps.executeUpdate();
          ps.close();
          msg("Deleting user db_user_delete:" + username + ":" + serverGroup);
          ps = conn.prepareStatement(this.settings.getProperty("db_user_delete"));
          ps.setString(1, username);
          ps.setString(2, serverGroup);
          ps.executeUpdate();
          ps.close();
        } 
      } catch (Throwable e) {
        msg(e);
        try {
          conn.close();
        } catch (Exception exception) {}
      } finally {
        try {
          if (ps != null)
            ps.close(); 
        } catch (SQLException sQLException) {}
        releaseConnection(conn);
      } 
    } 
    resetCache(serverGroup, "USERS");
  }
  
  public Vector loadUserList(String serverGroup) {
    msg("Loading user list from DB...");
    Vector v = new Vector();
    Connection conn = null;
    PreparedStatement ps = null;
    try {
      msg("Connecting to db.");
      conn = getConnection();
      ResultSet rs = null;
      ps = conn.prepareStatement(this.settings.getProperty("db_users_query"));
      msg("Connected.");
      ps.setString(1, serverGroup);
      rs = ps.executeQuery();
      msg("Querying DB for user:" + this.settings.getProperty("db_user_query"));
      while (rs.next()) {
        msg("Found user.");
        v.addElement(rs.getString("USERNAME"));
      } 
      rs.close();
      ps.close();
    } catch (Throwable e) {
      Log.log("USER", 0, e);
    } finally {
      try {
        if (ps != null)
          ps.close(); 
      } catch (SQLException sQLException) {}
      releaseConnection(conn);
    } 
    return v;
  }
  
  public void addFolder(String serverGroup, String username, String path, String name) {
    Properties virtual = new Properties();
    Properties p = new Properties();
    p.put("type", "DIR");
    if (!path.endsWith("/"))
      path = String.valueOf(path) + "/"; 
    if (path.startsWith("/"))
      path = path.substring(1); 
    if (path.equals("/"))
      path = ""; 
    p.put("virtualPath", path);
    writeVFS(serverGroup, username, virtual, false);
  }
  
  public void addItem(String serverGroup, String username, String path, String name, String url, String type, Properties moreItems, boolean encrypted, String encrypted_class) throws Exception {
    if (this.settings.getProperty("db_read_only", "false").equals("true"))
      return; 
    if (type.toUpperCase().equals("DIR") && !url.endsWith("/"))
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
          Class c = ServerStatus.clasLoader.loadClass(p.getProperty("encrypted_class").trim());
          Constructor cons = c.getConstructor(new Class[] { (new Properties()).getClass(), (new String()).getClass() });
          cons.newInstance(new Object[] { p, "encrypt" });
        } catch (Exception ee) {
          Log.log("USER", 1, ee);
        } 
      } else {
        p.put("url", ServerStatus.thisObj.common_code.encode_pass(p.getProperty("url"), "DES"));
      } 
    } 
    Properties virtual = new Properties();
    Properties pp = new Properties();
    pp.put("type", "DIR");
    if (!path.endsWith("/"))
      path = String.valueOf(path) + "/"; 
    if (path.startsWith("/"))
      path = path.substring(1); 
    if (path.equals("/"))
      path = ""; 
    pp.put("virtualPath", path);
    pp.put("vItems", v);
    writeVFS(serverGroup, username, virtual, false);
  }
  
  private void deleteUserProperties(String id, Connection conn) throws Exception {
    if (this.settings.getProperty("db_read_only", "false").equals("true"))
      return; 
    msg("Deleting user db_user_properties_delete:" + id);
    PreparedStatement ps = null;
    try {
      ps = conn.prepareStatement(this.settings.getProperty("db_user_properties_delete"));
      ps.setString(1, id);
      ps.executeUpdate();
      ps.close();
      msg("Deleting user db_user_events_delete:" + id);
      ps = conn.prepareStatement(this.settings.getProperty("db_user_events_delete"));
      ps.setString(1, id);
      try {
        ps.executeUpdate();
      } catch (Exception exception) {}
      ps.close();
      ps = conn.prepareStatement(Common.replace_str(this.settings.getProperty("db_user_events_delete"), "DELETE FROM EVENTS WHERE USERID", "DELETE FROM EVENTS5 WHERE USERID"));
      ps.setString(1, id);
      try {
        ps.executeUpdate();
      } catch (Exception exception) {}
      ps.close();
      msg("Deleting user db_user_domain_root_list_delete:" + id);
      ps = conn.prepareStatement(this.settings.getProperty("db_user_domain_root_list_delete"));
      ps.setString(1, id);
      ps.executeUpdate();
      ps.close();
      msg("Deleting user db_user_ip_restrictions_delete:" + id);
      ps = conn.prepareStatement(this.settings.getProperty("db_user_ip_restrictions_delete"));
      ps.setString(1, id);
      ps.executeUpdate();
      ps.close();
      msg("Deleting user db_user_web_buttons_delete:" + id);
      ps = conn.prepareStatement(this.settings.getProperty("db_user_web_buttons_delete"));
      ps.setString(1, id);
      ps.executeUpdate();
      ps.close();
      msg("Deleting user db_user_web_customizations_delete:" + id);
      ps = conn.prepareStatement(this.settings.getProperty("db_user_web_customizations_delete"));
      ps.setString(1, id);
      ps.executeUpdate();
      ps.close();
      msg("Deleting user db_groups_delete:" + id);
      ps = conn.prepareStatement(this.settings.getProperty("db_groups_delete"));
      ps.setString(1, id);
      ps.executeUpdate();
      ps.close();
    } finally {
      if (ps != null)
        ps.close(); 
    } 
  }
  
  private void doTableDeleteInsert(String id, Vector v, Connection conn, String sql_delete, String sql_insert) throws Exception {
    if (this.settings.getProperty("db_read_only", "false").equals("true"))
      return; 
    PreparedStatement ps2 = null;
    try {
      ps2 = conn.prepareStatement(sql_delete);
      ps2.setString(1, id);
      ps2.execute();
      ps2.close();
      if (v == null)
        return; 
      ps2 = conn.prepareStatement(sql_insert);
      for (int x = 0; x < v.size(); x++) {
        Properties p = v.elementAt(x);
        String[] fields = sql_insert.substring(sql_insert.indexOf("(") + 1, sql_insert.indexOf(")")).split(",");
        for (int xx = 0; xx < fields.length; xx++) {
          if (fields[xx].trim().equalsIgnoreCase("USERID")) {
            ps2.setString(xx + 1, id);
          } else if (fields[xx].trim().equalsIgnoreCase("SORT_ORDER")) {
            ps2.setString(xx + 1, (new StringBuffer(String.valueOf(x))).toString());
          } else {
            String key = fields[xx].trim();
            if (key.toUpperCase().startsWith("ORACLE_"))
              key = key.substring("ORACLE_".length()); 
            if (key.toUpperCase().startsWith("SQL_FIELD_"))
              key = key.substring("SQL_FIELD_".length()); 
            ps2.setString(xx + 1, p.getProperty(key.toLowerCase()));
          } 
        } 
        ps2.execute();
      } 
      ps2.close();
    } finally {
      if (ps2 != null)
        ps2.close(); 
    } 
  }
  
  private void loadUserItems(String id, Properties user, Statement s) {
    msg("Loading user properties...");
    String formerPass = user.getProperty("password", "");
    loadTable(id, "USER_PROPERTIES", user, s, true, null, null, "PROP_NAME", "PROP_VALUE");
    user.put("password", formerPass);
    Vector v = new Vector();
    String[] lvs = user.getProperty("linked_vfs", "").split(",");
    for (int x = 0; x < lvs.length; x++) {
      if (!lvs[x].trim().equals(""))
        v.addElement(lvs[x].trim()); 
    } 
    user.put("linked_vfs", v);
    if (v.size() == 0)
      user.remove("linked_vfs"); 
    msg("Loading events...");
    loadTable(id, "EVENTS5", user, s, true, "event_name", "event_name", "prop_name", "prop_value");
    if (user.get("EVENTS5") == null && loadEvents4) {
      loadTable(id, "EVENTS", user, s, false, null, null, "", "");
    } else if (user.containsKey("EVENTS5")) {
      user.put("events", user.remove("EVENTS5"));
    } 
    msg("Loading ip_restrictions...");
    loadTable(id, "IP_RESTRICTIONS", user, s, false, null, null, "", "");
    msg("Loading web_buttons...");
    loadTable(id, "WEB_BUTTONS", user, s, false, "SORT_ORDER", null, "", "");
    if (user.containsKey("web_buttons")) {
      Vector buttons = (Vector)user.get("web_buttons");
      if (buttons.size() > 0) {
        Properties button = buttons.elementAt(0);
        if (button.containsKey("for_menu"))
          for_menu_buttons = true; 
      } 
    } 
    if (user.getProperty("user_name", "").equalsIgnoreCase("default") && !user.containsKey("web_buttons")) {
      if (class$0 == null)
        try {
        
        } catch (ClassNotFoundException classNotFoundException) {
          throw new NoClassDefFoundError(null.getMessage());
        }  
      Properties defaultUser = (Properties)Common.readXMLObject((class$0 = Class.forName("crushftp.server.VFS")).getResource("/assets/default_user.xml"));
      user.put("web_buttons", defaultUser.get("web_buttons"));
    } 
    msg("Loading web_customizations...");
    loadTable(id, "WEB_CUSTOMIZATIONS", user, s, false, null, null, "", "");
    msg("Loading domain_root_list...");
    loadTable(id, "DOMAIN_ROOT_LIST", user, s, false, null, null, "", "");
  }
  
  private void loadTable(String userid, String table, Properties user, Statement s, boolean propertyMode, String orderby, String groupby, String prop_name, String prop_value) {
    Vector v = new Vector();
    try {
      String tableKey = table.toLowerCase();
      if (tableKey.equalsIgnoreCase("events5"))
        tableKey = "events"; 
      String _sql = get("db_user_" + tableKey + "_id_query");
      _sql = Common.replace_str(_sql, "?", "'" + userid.replace('\'', '_') + "'");
      if (orderby != null)
        _sql = String.valueOf(_sql) + " ORDER BY " + orderby; 
      msg("Querying " + table + ":" + _sql);
      ResultSet rs = s.executeQuery(_sql);
      Properties lastProp = new Properties();
      String lastGroupByVal = "";
      while (rs.next()) {
        if (propertyMode) {
          String key = rs.getString(prop_name);
          String val = rs.getString(prop_value);
          if (val == null)
            val = ""; 
          if (groupby == null) {
            if (key != null)
              user.put(key, val); 
          } else {
            String altKeyName = groupby;
            String groupByVal = (rs.getString(groupby) == null) ? "" : rs.getString(groupby);
            if (altKeyName.toUpperCase().startsWith("ORACLE_"))
              altKeyName = altKeyName.substring("ORACLE_".length()); 
            if (altKeyName.toUpperCase().startsWith("SQL_FIELD_"))
              altKeyName = altKeyName.substring("SQL_FIELD_".length()); 
            if (lastGroupByVal.equals(""))
              lastGroupByVal = groupByVal; 
            if (!lastGroupByVal.equals(groupByVal)) {
              v.addElement(lastProp);
              lastProp = new Properties();
            } 
            if (key != null) {
              lastProp.put(key, val);
              if (val.indexOf(" type=\"vector\">") >= 0 || val.indexOf(" type=\"properties\">") >= 0)
                lastProp.put(key, Common.readXMLObject(new ByteArrayInputStream(val.getBytes("UTF8")))); 
              lastProp.put(altKeyName, groupByVal);
            } 
            lastGroupByVal = groupByVal;
          } 
          msg("Got:" + key + "=" + val);
          continue;
        } 
        Properties saver = new Properties();
        try {
          int columnCount = rs.getMetaData().getColumnCount();
          for (int x = 1; x <= columnCount; x++) {
            String key = rs.getMetaData().getColumnName(x);
            if (key.toUpperCase().startsWith("ORACLE_"))
              key = key.substring("ORACLE_".length()); 
            if (key.toUpperCase().startsWith("SQL_FIELD_"))
              key = key.substring("SQL_FIELD_".length()); 
            String val = rs.getString(x);
            if (val == null)
              val = ""; 
            saver.put(key.toLowerCase(), val);
          } 
        } catch (Throwable ee) {
          msg(ee);
        } 
        v.addElement(saver);
        msg("Got:" + saver);
      } 
      rs.close();
      msg("Finished with table load.");
      msg(table);
      if (groupby != null && !lastGroupByVal.equals(""))
        v.addElement(lastProp); 
      if (!propertyMode || (propertyMode && groupby != null)) {
        if (table.equalsIgnoreCase("WEB_BUTTONS") || table.equalsIgnoreCase("IP_RESTRICTIONS") || table.equalsIgnoreCase("DOMAIN_ROOT_LIST") || table.equalsIgnoreCase("EVENTS") || table.equalsIgnoreCase("WEB_CUSTOMIZATIONS"))
          table = table.toLowerCase(); 
        if (v.size() > 0)
          user.put(table, v); 
      } 
    } catch (Throwable e) {
      if (table.equalsIgnoreCase("events")) {
        loadEvents4 = false;
      } else {
        msg(e);
      } 
    } 
  }
  
  private String get(String key) {
    return this.settings.getProperty(key, "");
  }
  
  private void msg(String s) {
    if (this.settings.getProperty("db_debug").equals("true"))
      Log.log("USER", 0, "SQL:" + s); 
  }
  
  private void msg(Throwable e) {
    if (this.settings.getProperty("db_debug").equals("true"))
      Log.log("USER", 0, e); 
  }
  
  private void releaseConnection(Connection conn) {
    try {
      if (conn != null && !conn.isClosed())
        freeConnections.addElement(conn); 
    } catch (SQLException sQLException) {}
  }
  
  private Connection getConnection() throws Throwable {
    String[] db_drv_files = get("db_driver_file").split(";");
    URL[] urls = new URL[db_drv_files.length];
    for (int x = 0; x < db_drv_files.length; x++)
      urls[x] = (new File(db_drv_files[x])).toURI().toURL(); 
    if (cl == null || !this.lastDriver.equals(get("db_driver_file"))) {
      cl = new URLClassLoader(urls);
      drvCls = Class.forName(get("db_driver"), true, cl);
      driver = drvCls.newInstance();
    } 
    this.lastDriver = get("db_driver_file");
    synchronized (freeConnections) {
      if (freeConnections.size() > 0)
        return freeConnections.remove(0); 
    } 
    Properties props = new Properties();
    props.setProperty("user", get("db_user"));
    props.setProperty("password", ServerStatus.thisObj.common_code.decode_pass(Common.url_decode(get("db_pass"))));
    return driver.connect(get("db_url"), props);
  }
  
  private void cacheItem(String cacheId, Object obj) {
    this.userCache.put(cacheId, Common.CLONE(obj));
    this.cacheRevolver.addElement(cacheId);
    while (this.cacheRevolver.size() > 200) {
      String removeId = this.cacheRevolver.remove(0).toString();
      this.userCache.remove(removeId);
    } 
  }
  
  private void resetCache(String serverGroup, String prop_name) {
    this.userCache.clear();
    Connection conn = null;
    PreparedStatement ps = null;
    try {
      msg("Connecting to db.");
      conn = getConnection();
      ps = conn.prepareStatement(this.settings.getProperty("db_modified_delete"));
      msg("Connected.");
      ps.setString(1, serverGroup);
      ps.setString(2, prop_name);
      ps.executeUpdate();
      ps.close();
      ps = conn.prepareStatement(this.settings.getProperty("db_modified_insert"));
      ps.setString(1, serverGroup);
      ps.setString(2, prop_name);
      ps.setString(3, (new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")).format(new Date()));
      ps.executeUpdate();
      ps.close();
    } catch (Throwable e) {
      msg(e);
      try {
        conn.close();
      } catch (Exception exception) {}
    } finally {
      try {
        if (ps != null)
          ps.close(); 
      } catch (SQLException sQLException) {}
      releaseConnection(conn);
    } 
  }
  
  private String getModified(String serverGroup, String prop_name) {
    String val = null;
    Connection conn = null;
    PreparedStatement ps = null;
    try {
      msg("Connecting to db.");
      conn = getConnection();
      ps = conn.prepareStatement(this.settings.getProperty("db_modified_query"));
      msg("Connected.");
      ps.setString(1, serverGroup);
      ps.setString(2, prop_name);
      ResultSet rs = ps.executeQuery();
      while (rs.next())
        val = rs.getString("PROP_VALUE"); 
      rs.close();
      ps.close();
    } catch (Throwable e) {
      msg(e);
      try {
        conn.close();
      } catch (Exception exception) {}
    } finally {
      try {
        if (ps != null)
          ps.close(); 
      } catch (SQLException sQLException) {}
      releaseConnection(conn);
    } 
    return val;
  }
}
