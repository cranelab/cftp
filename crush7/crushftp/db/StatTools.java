package crushftp.db;

import com.crushftp.client.VRL;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.SessionCrush;
import crushftp.server.ServerStatus;
import java.io.BufferedReader;
import java.io.File;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

public class StatTools {
  static URLClassLoader cl = null;
  
  static Class drvCls = null;
  
  static Driver driver = null;
  
  public Properties settings = new Properties();
  
  public boolean mssql = false;
  
  public boolean mysql = false;
  
  public boolean derby = false;
  
  public static boolean started = false;
  
  public static int port = 3309;
  
  static Vector freeConnections = new Vector();
  
  static Vector usedConnections = new Vector();
  
  public static Object used_lock = new Object();
  
  public boolean disabled = false;
  
  public static final OutputStream DEV_NULL = new OutputStream() {
      public void write(int b) {}
    };
  
  static long lastU = System.currentTimeMillis();
  
  public static synchronized long u() {
    while (lastU == System.currentTimeMillis()) {
      try {
        Thread.sleep(1L);
      } catch (Exception exception) {}
    } 
    lastU = System.currentTimeMillis();
    return lastU;
  }
  
  public static void setDefaults(Properties p) {
    p.put("stats_debug", "true");
    p.put("stats_db_driver_file", "");
    p.put("stats_db_driver", "org.apache.derby.jdbc.EmbeddedDriver");
    p.put("stats_db_url", "jdbc:derby:" + System.getProperty("crushftp.stats") + "statsDB;create=true");
    p.put("stats_db_user", "app");
    p.put("stats_db_pass", "");
    p.put("stats_get_max_sessions_rid", "select max(RID) from SESSIONS");
    p.put("stats_get_max_transfers_rid", "select max(RID) from TRANSFERS");
    p.put("stats_get_max_meta_info_rid", "select max(RID) from META_INFO");
    p.put("stats_get_transfers_sessions", "select * from TRANSFERS where SESSION_RID in (%sessions%)");
    p.put("stats_get_session_rid_sessions", "select RID from SESSIONS where START_TIME > ? and USER_NAME = ?");
    p.put("stats_update_transfers_ignore_size", "update TRANSFERS set IGNORE_SIZE = 'Y' where SESSION_RID in (%sessions%) and START_TIME > ?");
    p.put("stats_insert_sessions", "INSERT INTO SESSIONS (RID, SESSION, SERVER_GROUP, USER_NAME, START_TIME, END_TIME, SUCCESS_LOGIN, IP) VALUES (?,?,?,?,?,?,?,?)");
    p.put("stats_insert_transfers", "INSERT INTO TRANSFERS (RID, SESSION_RID, START_TIME, DIRECTION, PATH, FILE_NAME, URL, SPEED, TRANSFER_SIZE, IGNORE_SIZE) VALUES (?,?,?,?,?,?,?,?,?,'N')");
    p.put("stats_insert_meta_info", "INSERT INTO META_INFO (RID, SESSION_RID, TRANSFER_RID, ITEM_KEY, ITEM_VALUE) VALUES (?,?,?,?,?)");
    p.put("stats_update_sessions", "UPDATE SESSIONS set END_TIME = ? where RID = ?");
    p.put("stats_get_transfers_time", "SELECT RID FROM TRANSFERS WHERE START_TIME < ?");
    p.put("stats_get_sessions_time", "SELECT RID FROM SESSIONS WHERE START_TIME < ?");
    p.put("stats_delete_meta_transfers", "DELETE FROM META_INFO WHERE TRANSFER_RID IN (%transfers%)");
    p.put("stats_delete_transfers_time", "DELETE FROM TRANSFERS WHERE START_TIME < ?");
    p.put("stats_delete_sessions_time", "DELETE FROM SESSIONS WHERE START_TIME < ?");
    p.put("stats_get_transfers_download", "select count(*) from TRANSFERS where direction = 'DOWNLOAD' and session_rid in (select rid from SESSIONS where user_name = ?)");
    p.put("stats_get_meta_info", "select * from META_INFO where LOCATE(UPPER(ITEM_VALUE), UPPER(?)) > 0");
    p.put("stats_get_transfers_period", "select sum(TRANSFER_SIZE) from TRANSFERS t, SESSIONS s where DIRECTION = ? and (IGNORE_SIZE = 'N' or IGNORE_SIZE is null) and t.SESSION_RID = s.RID and USER_NAME = ? and t.START_TIME >= ?");
    p.put("stats_get_transfers_count_period", "select count(TRANSFER_SIZE) from TRANSFERS t, SESSIONS s where DIRECTION = ? and (IGNORE_SIZE = 'N' or IGNORE_SIZE is null) and t.SESSION_RID = s.RID and USER_NAME = ? and t.START_TIME >= ?");
  }
  
  public synchronized void init() {
    this.disabled = ServerStatus.BG("disable_stats");
    System.getProperties().put("derby.stream.error.field", "crushftp.db.StatTools.DEV_NULL");
    this.settings = ServerStatus.server_settings;
    this.mssql = (this.settings.getProperty("stats_db_url").toUpperCase().indexOf("SQLSERVER") >= 0);
    this.mysql = (this.settings.getProperty("stats_db_driver").toUpperCase().indexOf("MYSQL") >= 0);
    this.derby = (this.settings.getProperty("stats_db_driver").toUpperCase().indexOf("DERBY") >= 0);
    if (this.disabled) {
      Log.log("STATISTICS", 0, "Statistics database is disabled.");
      return;
    } 
    String script2 = "CREATE TABLE META_INFO(RID DOUBLE NOT NULL PRIMARY KEY,SESSION_RID DOUBLE NOT NULL,TRANSFER_RID DOUBLE NOT NULL,ITEM_KEY VARCHAR(100) DEFAULT NULL,ITEM_VALUE VARCHAR(2000) DEFAULT NULL)\n";
    script2 = String.valueOf(script2) + "CREATE TABLE SESSIONS(RID DOUBLE NOT NULL PRIMARY KEY,SESSION VARCHAR(100) DEFAULT NULL,SERVER_GROUP VARCHAR(50) DEFAULT NULL,USER_NAME VARCHAR(100) DEFAULT NULL,START_TIME TIMESTAMP DEFAULT NULL,END_TIME TIMESTAMP DEFAULT NULL,SUCCESS_LOGIN VARCHAR(10) DEFAULT NULL,IP VARCHAR(50) DEFAULT NULL)\n";
    script2 = String.valueOf(script2) + "CREATE TABLE TRANSFERS(RID DOUBLE NOT NULL PRIMARY KEY,SESSION_RID DOUBLE NOT NULL,START_TIME TIMESTAMP DEFAULT NULL,DIRECTION VARCHAR(8) DEFAULT NULL,PATH VARCHAR(255) DEFAULT NULL,FILE_NAME VARCHAR(2000) DEFAULT NULL,URL VARCHAR(2000) DEFAULT NULL,SPEED INTEGER DEFAULT NULL,TRANSFER_SIZE DOUBLE DEFAULT NULL,IGNORE_SIZE VARCHAR(1) DEFAULT NULL)\n";
    if (!started)
      (new File(String.valueOf(System.getProperty("crushftp.stats")) + "stats/")).renameTo(new File(String.valueOf(System.getProperty("crushftp.stats")) + "stats_crush5/")); 
    started = true;
    if (this.derby && !(new File(String.valueOf(System.getProperty("crushftp.stats")) + "statsDB/")).exists()) {
      try {
        Log.log("STATISTICS", 0, "Creating statsDB...");
        try {
          createDerbyDB(script2);
        } catch (Exception e) {
          Log.log("STATISTICS", 0, e);
        } 
        Common.recurseDelete(String.valueOf(System.getProperty("crushftp.stats")) + "stats/", false);
        this.settings.put("stats_db_driver", "org.apache.derby.jdbc.EmbeddedDriver");
        this.settings.put("stats_db_url", "jdbc:derby:" + System.getProperty("crushftp.stats") + "statsDB;create=true");
        this.settings.put("stats_db_user", "app");
        this.settings.put("stats_db_pass", "");
        this.derby = true;
        started = true;
        ServerStatus.thisObj.save_server_settings(true);
        Log.log("STATISTICS", 0, "Creation complete.");
      } catch (Throwable e) {
        Log.log("STATISTICS", 0, e);
      } 
      if (this.settings.getProperty("stats_get_session_rid_sessions").indexOf("%now%") >= 0)
        this.settings.put("stats_get_session_rid_sessions", "select RID from SESSIONS where START_TIME > ? and USER_NAME = ?"); 
      if (this.settings.getProperty("stats_update_transfers_ignore_size").indexOf("%now%") >= 0)
        this.settings.put("stats_update_transfers_ignore_size", "update TRANSFERS set IGNORE_SIZE = 'Y' where SESSION_RID in (%sessions%) and START_TIME > ?"); 
    } 
    try {
      executeSqlQuery("select count(*) from SESSIONS", new Object[0], false);
    } catch (Exception e) {
      try {
        createDerbyDB(script2);
      } catch (Throwable throwable) {}
    } 
    try {
      executeSqlQuery("select count(*) from TRANSFERS", new Object[0], false);
    } catch (Exception e) {
      try {
        createDerbyDB(script2);
      } catch (Throwable throwable) {}
    } 
  }
  
  public void createDerbyDB(String script2) throws Throwable {
    Connection conn1 = null;
    Connection conn2 = null;
    try {
      conn1 = getConnection();
      Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
      conn2 = DriverManager.getConnection("jdbc:derby:" + System.getProperty("crushftp.stats") + "statsDB;create=true", "app", "");
      conn2.setAutoCommit(true);
      if (script2 != null) {
        BufferedReader br = new BufferedReader(new StringReader(script2));
        String data = "";
        try {
          while ((data = br.readLine()) != null) {
            PreparedStatement ps = conn2.prepareStatement(data);
            ps.execute();
            ps.close();
          } 
        } catch (Exception e) {
          e.printStackTrace();
        } 
      } 
    } finally {
      try {
        if (conn1 != null)
          conn1.close(); 
      } catch (Exception exception) {}
      try {
        if (conn2 != null)
          conn2.close(); 
      } catch (Exception exception) {}
    } 
  }
  
  public synchronized void stopDB() {
    if (!started)
      return; 
    try {
      if (this.derby)
        DriverManager.getConnection("jdbc:derby:;shutdown=true"); 
    } catch (Throwable e) {
      Log.log("STATISTICS", 3, e);
    } 
    started = false;
  }
  
  public void findMetas(String sql, String[] values, Vector v) {
    if (this.disabled && this.derby)
      return; 
    msg("Connecting to db, executing sql:" + sql);
    Connection conn = null;
    try {
      conn = getConnection();
      PreparedStatement ps_metas = conn.prepareStatement(sql);
      String sessions = ",";
      Properties metas = new Properties();
      try {
        for (int x = 0; x < values.length; x++)
          ps_metas.setString(x + 1, values[x]); 
        ResultSet rs = ps_metas.executeQuery();
        while (rs.next()) {
          Properties meta = (Properties)metas.get(rs.getString("SESSION_RID"));
          if (meta == null)
            meta = new Properties(); 
          meta.put(rs.getString("ITEM_KEY"), rs.getString("ITEM_VALUE"));
          metas.put(rs.getString("SESSION_RID"), meta);
          if (sessions.indexOf("," + rs.getString("SESSION_RID") + ",") < 0)
            sessions = String.valueOf(sessions) + rs.getString("SESSION_RID") + ","; 
        } 
        rs.close();
      } finally {
        ps_metas.close();
      } 
      if (sessions.length() > 1)
        sessions = sessions.substring(1, sessions.length() - 1); 
      sql = Common.replace_str(get("stats_get_transfers_sessions"), "%sessions%", sessions);
      PreparedStatement ps_sessions = conn.prepareStatement(sql);
      try {
        ResultSet rs = ps_sessions.executeQuery();
        while (rs.next()) {
          Properties p = new Properties();
          p.put("url", rs.getString("URL"));
          p.put("metaInfo", metas.get(rs.getString("SESSION_RID")));
          v.addElement(p);
        } 
        rs.close();
      } finally {
        ps_sessions.close();
      } 
    } catch (Throwable e) {
      try {
        conn.close();
      } catch (Exception exception) {}
      msg(e);
    } finally {
      releaseConnection(conn);
    } 
  }
  
  public void executeSql(String sql, Object[] values) {
    if (this.disabled)
      return; 
    msg("Connecting to db, executing sql:" + sql);
    if (this.mysql)
      sql = Common.replace_str(sql, "as bigint", "as unsigned"); 
    Connection conn = null;
    try {
      conn = getConnection();
      PreparedStatement ps = conn.prepareStatement(sql);
      try {
        for (int x = 0; x < values.length; x++) {
          if (values[x] instanceof String) {
            ps.setString(x + 1, (String)values[x]);
          } else if (values[x] instanceof Date) {
            ps.setTimestamp(x + 1, new Timestamp(((Date)values[x]).getTime()));
          } else if (values[x] == null) {
            ps.setString(x + 1, (String)null);
          } 
        } 
        ps.executeUpdate();
      } finally {
        ps.close();
      } 
    } catch (Throwable e) {
      if (e.indexOf("'SESSIONS' does not exist") >= 0) {
        Common.recurseDelete(String.valueOf(System.getProperty("crushftp.stats")) + "statsDB/", false);
        init();
      } 
      try {
        conn.close();
      } catch (Exception exception) {}
      msg(e);
    } finally {
      releaseConnection(conn);
    } 
  }
  
  public Vector executeSqlQuery(String sql, Object[] values, boolean includeColumns) {
    return executeSqlQuery(sql, values, new Boolean(includeColumns));
  }
  
  public Vector executeSqlQuery(String sql, Object[] values, Boolean includeColumnsB) {
    if (this.disabled && this.derby)
      return new Vector(); 
    if (this.mysql)
      sql = Common.replace_str(sql, "as bigint", "as unsigned"); 
    if (this.derby)
      while (sql.indexOf("CASEWHEN") >= 0) {
        int loc1 = sql.indexOf("CASEWHEN");
        int loc2 = sql.indexOf(",", loc1);
        int loc3 = sql.indexOf(",", loc2 + 1);
        int loc4 = sql.indexOf(")", loc3 + 1);
        sql = String.valueOf(sql.substring(0, loc1)) + "CASE WHEN " + sql.substring(loc1 + "CASEWHEN".length(), loc2) + ") THEN " + sql.substring(loc2 + 1, loc3) + " ELSE " + sql.substring(loc3 + 1, loc4) + " END " + sql.substring(loc4 + 1);
      }  
    msg("Connecting to db, executing sql:" + sql);
    Vector results = new Vector();
    Connection conn = null;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    try {
      conn = getConnection();
      boolean fixLimit = false;
      if (this.derby)
        if (sql.indexOf("limit ?") >= 0) {
          sql = sql.replaceAll("limit \\?", "").trim();
          fixLimit = true;
        }  
      PreparedStatement ps = conn.prepareStatement(sql);
      try {
        for (int x = 0; x < values.length; x++) {
          if (this.derby && fixLimit && x == values.length - 1) {
            ps.setMaxRows(((Integer)values[x]).intValue());
            break;
          } 
          if (values[x] instanceof String) {
            ps.setString(x + 1, (String)values[x]);
          } else if (values[x] instanceof Integer) {
            ps.setInt(x + 1, ((Integer)values[x]).intValue());
          } else if (values[x] instanceof Date) {
            ps.setTimestamp(x + 1, new Timestamp(((Date)values[x]).getTime()));
          } 
        } 
        ResultSet rs = ps.executeQuery();
        Vector cols = new Vector();
        while (rs.next()) {
          Properties p = new Properties();
          for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
            String key = rs.getMetaData().getColumnLabel(i + 1);
            if (rs.getMetaData().getColumnTypeName(i + 1).equalsIgnoreCase("TIMESTAMP")) {
              try {
                p.put(key, sdf.format(new Date(rs.getTimestamp(i + 1).getTime())));
              } catch (Exception exception) {}
            } else if (rs.getMetaData().getColumnTypeName(i + 1).equalsIgnoreCase("DOUBLE")) {
              try {
                p.put(key, (new StringBuffer(String.valueOf(rs.getLong(i + 1)))).toString());
              } catch (Exception exception) {}
            } else {
              try {
                p.put(key, rs.getString(i + 1));
              } catch (Exception exception) {}
            } 
            if (results.size() == 0)
              cols.addElement(key); 
          } 
          if (results.size() == 0 && includeColumnsB.booleanValue())
            results.addElement(cols); 
          results.addElement(p);
        } 
      } finally {
        ps.close();
      } 
    } catch (Throwable e) {
      try {
        conn.close();
      } catch (Exception exception) {}
      msg(e);
    } finally {
      releaseConnection(conn);
    } 
    return results;
  }
  
  public String getValue(String sql, Object[] values) {
    msg("Connecting to db, executing sql:" + sql);
    String val = null;
    Connection conn = null;
    try {
      conn = getConnection();
      PreparedStatement ps = conn.prepareStatement(sql);
      try {
        for (int x = 0; x < values.length; x++) {
          if (values[x] instanceof String) {
            ps.setString(x + 1, (String)values[x]);
          } else if (values[x] instanceof Date) {
            ps.setTimestamp(x + 1, new Timestamp(((Date)values[x]).getTime()));
          } 
        } 
        ResultSet rs = ps.executeQuery();
        if (rs.next())
          try {
            val = (new StringBuffer(String.valueOf(rs.getLong(1)))).toString();
          } catch (Exception e) {
            val = rs.getString(1);
          }  
        rs.close();
      } finally {
        ps.close();
      } 
    } catch (Throwable e) {
      try {
        conn.close();
      } catch (Exception exception) {}
      msg(e);
    } finally {
      releaseConnection(conn);
    } 
    return val;
  }
  
  public void insertMetaInfo(String session_rid2, Properties metaInfo, String transfer_rid2) {
    if (this.disabled || metaInfo == null)
      return; 
    String sql = get("stats_insert_meta_info");
    Enumeration keys = metaInfo.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      String val = metaInfo.getProperty(key, "");
      executeSql(sql, (Object[])new String[] { (new StringBuffer(String.valueOf(u()))).toString(), session_rid2, transfer_rid2, key, val });
    } 
  }
  
  public void setIgnore(String user_name, String transfer_type, String duration) {
    if (this.disabled)
      return; 
    msg("Connecting to db:setIgnore");
    Connection conn = null;
    try {
      conn = getConnection();
      PreparedStatement ps = conn.prepareStatement(get("stats_get_session_rid_sessions"));
      String sessions = ",";
      GregorianCalendar gc = new GregorianCalendar();
      try {
        gc.setTimeInMillis(System.currentTimeMillis());
        gc.add(5, Integer.parseInt(duration) * -1);
        ps.setTimestamp(1, new Timestamp(gc.getTime().getTime()));
        ps.setString(2, user_name);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
          if (sessions.indexOf("," + rs.getString("RID") + ",") < 0)
            sessions = String.valueOf(sessions) + rs.getString("RID") + ","; 
        } 
        rs.close();
      } finally {
        ps.close();
      } 
      if (sessions.length() > 1) {
        sessions = sessions.substring(1, sessions.length() - 1);
        PreparedStatement ps_sessions = conn.prepareStatement(Common.replace_str(get("stats_update_transfers_ignore_size"), "%sessions%", sessions));
        ps_sessions.setTimestamp(1, new Timestamp(gc.getTime().getTime()));
        ps_sessions.executeUpdate();
        ps_sessions.close();
      } 
    } catch (Throwable e) {
      try {
        conn.close();
      } catch (Exception exception) {}
      msg(e);
    } finally {
      releaseConnection(conn);
    } 
  }
  
  public String get(String key) {
    return this.settings.getProperty(key, "");
  }
  
  public void msg(String s) {
    if (this.settings.getProperty("stats_debug", "true").equals("true"))
      Log.log("STATISTICS", 2, "SQL:" + s); 
  }
  
  public void msg(Throwable e) {
    if (this.settings.getProperty("stats_debug", "true").equals("true"))
      Log.log("STATISTICS", 1, e); 
  }
  
  private void releaseConnection(Connection conn) {
    try {
      usedConnections.remove(conn);
      if (conn != null && !conn.isClosed())
        freeConnections.addElement(conn); 
    } catch (SQLException sQLException) {}
  }
  
  public Connection getConnection() throws Throwable {
    if (!started)
      init(); 
    Connection conn = null;
    try {
      if (!get("stats_db_driver_file").equals("")) {
        String[] db_drv_files = get("stats_db_driver_file").split(";");
        URL[] urls = new URL[db_drv_files.length];
        for (int x = 0; x < db_drv_files.length; x++)
          urls[x] = (new File(db_drv_files[x])).toURI().toURL(); 
        if (cl == null) {
          cl = new URLClassLoader(urls);
          drvCls = Class.forName(get("stats_db_driver"), true, cl);
          driver = drvCls.newInstance();
        } 
        synchronized (used_lock) {
          while (usedConnections.size() > Integer.parseInt(System.getProperty("crushftp.stats.sql.maxpool", "1000")))
            Thread.sleep(100L); 
          synchronized (freeConnections) {
            if (freeConnections.size() > 0) {
              conn = freeConnections.remove(0);
              usedConnections.addElement(conn);
              return conn;
            } 
          } 
          Properties props = new Properties();
          props.setProperty("user", get("stats_db_user"));
          props.setProperty("password", ServerStatus.thisObj.common_code.decode_pass(get("stats_db_pass")));
          conn = driver.connect(get("stats_db_url"), props);
          usedConnections.addElement(conn);
        } 
      } else {
        synchronized (used_lock) {
          while (usedConnections.size() > Integer.parseInt(System.getProperty("crushftp.stats.sql.maxpool", "1000")))
            Thread.sleep(100L); 
          synchronized (freeConnections) {
            if (freeConnections.size() > 0) {
              conn = freeConnections.remove(0);
              usedConnections.addElement(conn);
              return conn;
            } 
          } 
          Class.forName(get("stats_db_driver"));
          conn = DriverManager.getConnection(get("stats_db_url"), get("stats_db_user"), get("stats_db_pass"));
          usedConnections.addElement(conn);
        } 
      } 
      conn.setAutoCommit(true);
    } catch (Exception e) {
      if (e.toString().indexOf("hsqldb") >= 0) {
        this.settings.put("stats_db_driver_file", "");
        this.settings.put("stats_db_driver", "org.apache.derby.jdbc.EmbeddedDriver");
        this.settings.put("stats_db_url", "jdbc:derby:" + System.getProperty("crushftp.stats") + "statsDB;create=true");
        this.settings.put("stats_db_user", "app");
        started = false;
        init();
        Class.forName(get("stats_db_driver"));
        conn = DriverManager.getConnection(get("stats_db_url"), get("stats_db_user"), get("stats_db_pass"));
        conn.setAutoCommit(true);
      } else {
        msg(e);
      } 
    } 
    return conn;
  }
  
  public int getUserDownloadCount(String username) {
    String sql = ServerStatus.SG("stats_get_transfers_download");
    if (sql.indexOf("transfers") >= 0 && sql.indexOf("sessions") >= 0) {
      sql = Common.replace_str(sql, " from transfers ", " from TRANSFERS ");
      sql = Common.replace_str(sql, " from sessions ", " from SESSIONS ");
      ServerStatus.server_settings.put("stats_get_transfers_download", sql);
    } 
    return Integer.parseInt(getValue(sql, (Object[])new String[] { username }));
  }
  
  public Vector getMatchingMetas(String meta1_value, Properties server_item) {
    Vector matchingUploads = new Vector();
    findMetas(ServerStatus.SG("stats_get_meta_info"), new String[] { meta1_value }, matchingUploads);
    return matchingUploads;
  }
  
  public long getTransferAmountToday(String user_ip, String user_name, Properties userStat, String transfer_type, SessionCrush thisSession) {
    long total = 0L;
    long daySeconds = 86400000L;
    transfer_type = transfer_type.toUpperCase().substring(0, transfer_type.length() - 1);
    String user_or_ip = user_ip;
    if (ServerStatus.SG("stats_get_transfers_period").toUpperCase().indexOf("USER_NAME") >= 0)
      user_or_ip = user_name; 
    String totalStr = getValue(ServerStatus.SG("stats_get_transfers_period"), new Object[] { transfer_type, user_or_ip, new Date(System.currentTimeMillis() - daySeconds) });
    if (totalStr == null)
      totalStr = "0"; 
    total = Long.parseLong(totalStr);
    return total;
  }
  
  public long getTransferCountToday(String user_ip, String user_name, Properties userStat, String transfer_type, SessionCrush thisSession) {
    long total = 0L;
    long daySeconds = 86400000L;
    transfer_type = transfer_type.toUpperCase().substring(0, transfer_type.length() - 1);
    String user_or_ip = user_ip;
    if (ServerStatus.SG("stats_get_transfers_count_period").toUpperCase().indexOf("USER_NAME") >= 0)
      user_or_ip = user_name; 
    String totalStr = getValue(ServerStatus.SG("stats_get_transfers_count_period"), new Object[] { transfer_type, user_or_ip, new Date(System.currentTimeMillis() - daySeconds) });
    if (totalStr == null)
      totalStr = "0"; 
    total = Long.parseLong(totalStr);
    return total;
  }
  
  public long getTransferAmountThisMonth(String user_ip, String user_name, Properties userStat, String transfer_type, SessionCrush thisSession) {
    long total = 0L;
    long monthSeconds = 86400000L;
    monthSeconds *= 30L;
    transfer_type = transfer_type.toUpperCase().substring(0, transfer_type.length() - 1);
    String user_or_ip = user_ip;
    if (ServerStatus.SG("stats_get_transfers_period").toUpperCase().indexOf("USER_NAME") >= 0)
      user_or_ip = user_name; 
    String totalStr = getValue(ServerStatus.SG("stats_get_transfers_period"), new Object[] { transfer_type, user_or_ip, new Date(System.currentTimeMillis() - monthSeconds) });
    if (totalStr == null)
      totalStr = "0"; 
    total = Long.parseLong(totalStr);
    return total;
  }
  
  public long getTransferCountThisMonth(String user_ip, String user_name, Properties userStat, String transfer_type, SessionCrush thisSession) {
    long total = 0L;
    long monthSeconds = 86400000L;
    monthSeconds *= 30L;
    transfer_type = transfer_type.toUpperCase().substring(0, transfer_type.length() - 1);
    String user_or_ip = user_ip;
    if (ServerStatus.SG("stats_get_transfers_count_period").toUpperCase().indexOf("USER_NAME") >= 0)
      user_or_ip = user_name; 
    String totalStr = getValue(ServerStatus.SG("stats_get_transfers_count_period"), new Object[] { transfer_type, user_or_ip, new Date(System.currentTimeMillis() - monthSeconds) });
    if (totalStr == null)
      totalStr = "0"; 
    total = Long.parseLong(totalStr);
    return total;
  }
  
  public void clearMaxTransferAmounts(Properties pp) {
    String user_name = pp.getProperty("user_name");
    long duration = Long.parseLong(pp.getProperty("duration"));
    String transfer_type = pp.getProperty("transfer_type");
    transfer_type = transfer_type.toUpperCase().substring(0, transfer_type.length() - 1);
    setIgnore(user_name, transfer_type, (new StringBuffer(String.valueOf(duration))).toString());
  }
  
  public Properties add_item_stat(SessionCrush theSession, Properties item, String action) {
    Properties data_item = new Properties();
    data_item.put("date", (new Date()).getTime());
    data_item.put("path", item.getProperty("the_file_path", ""));
    data_item.put("name", item.getProperty("the_file_name", ""));
    data_item.put("size", item.getProperty("the_file_size", ""));
    data_item.put("speed", item.getProperty("the_file_speed", ""));
    data_item.put("url", (new VRL(item.getProperty("url", ""))).safe());
    data_item.put("ip", theSession.uiSG("user_ip"));
    data_item.put("sessionID", (theSession.uiSG("sessionID") == null) ? "" : theSession.uiSG("sessionID"));
    long transfer_rid = u();
    data_item.put("TRANSFER_RID", (new StringBuffer(String.valueOf(transfer_rid))).toString());
    if (item.getProperty("the_file_path", "").indexOf("/WebInterface/") < 0 && item.getProperty("the_file_path", "").indexOf(".DS_Store") < 0)
      executeSql(ServerStatus.SG("stats_insert_transfers"), new Object[] { (new StringBuffer(String.valueOf(transfer_rid))).toString(), theSession.user_info.getProperty("SESSION_RID"), new Date(), action, item.getProperty("the_file_path", ""), item.getProperty("the_file_name", ""), item.getProperty("url", ""), item.getProperty("the_file_speed", ""), item.getProperty("the_file_size", "") }); 
    return data_item;
  }
  
  public Properties add_login_stat(Properties server_item, String user_name, String user_ip, boolean success, SessionCrush theSession) throws Exception {
    if (user_name.equals(""))
      return new Properties(); 
    for (int x = 0; x < 10; x++) {
      long rid = u();
      theSession.user_info.put("SESSION_RID", (new StringBuffer(String.valueOf(rid))).toString());
      try {
        String session_id = String.valueOf((new Date()).getTime()) + "_" + Common.makeBoundary(20);
        executeSql(ServerStatus.SG("stats_insert_sessions"), new Object[] { (new StringBuffer(String.valueOf(rid))).toString(), session_id, String.valueOf(server_item.getProperty("ip")) + "_" + server_item.getProperty("port"), user_name, new Date(), new Date(0L), (new StringBuffer(String.valueOf(success))).toString(), user_ip });
        break;
      } catch (Exception e) {
        Thread.sleep(500L);
        if (x > 5)
          Log.log("STATISTICS", 0, e); 
        if (x > 9)
          this.disabled = true; 
      } 
    } 
    return new Properties();
  }
}
