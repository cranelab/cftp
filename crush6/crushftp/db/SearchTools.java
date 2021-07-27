package crushftp.db;

import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.server.ServerStatus;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

public class SearchTools {
  static URLClassLoader cl = null;
  
  static Class drvCls = null;
  
  static Driver driver = null;
  
  public boolean mysql = false;
  
  public boolean derby = false;
  
  public static boolean started = false;
  
  public static void setDefaults(Properties p) {
    p.put("search_debug", "true");
    p.put("search_db_driver_file", "");
    p.put("search_db_driver", "org.apache.derby.jdbc.EmbeddedDriver");
    p.put("search_db_url", "jdbc:derby:" + System.getProperty("crushftp.search") + "searchDB;create=true");
    p.put("search_db_user", "app");
    p.put("search_db_pass", "");
    p.put("search_db_query", "SELECT * FROM SEARCH_INFO WHERE ITEM_PATH LIKE ?");
    p.put("search_db_insert", "INSERT INTO SEARCH_INFO (ITEM_PATH, ITEM_TYPE, ITEM_SIZE, ITEM_MODIFIED, ITEM_KEYWORDS) VALUES (?,?,?,?,?)");
    p.put("search_db_update", "UPDATE SEARCH_INFO SET ITEM_SIZE = ?, ITEM_MODIFIED = ?, ITEM_KEYWORDS = ? WHERE ITEM_PATH = ?");
    p.put("search_db_delete", "DELETE FROM SEARCH_INFO WHERE ITEM_PATH LIKE ?");
    p.put("search_index_usernames", "");
    p.put("search_index_interval", "60");
  }
  
  public synchronized void init() {
    System.setProperty("derby.stream.error.field", "crushftp.db.StatTools.DEV_NULL");
    this.mysql = (ServerStatus.SG("search_db_driver").toUpperCase().indexOf("MYSQL") >= 0);
    this.derby = (ServerStatus.SG("search_db_driver").toUpperCase().indexOf("DERBY") >= 0);
    if (this.derby && !(new File(String.valueOf(System.getProperty("crushftp.search")) + "searchDB/")).exists()) {
      String sql = "CREATE TABLE SEARCH_INFO(ITEM_PATH VARCHAR(1000) DEFAULT NULL PRIMARY KEY,ITEM_TYPE VARCHAR(10) DEFAULT NULL,ITEM_SIZE VARCHAR(20) DEFAULT NULL,ITEM_MODIFIED VARCHAR(20) DEFAULT NULL,ITEM_KEYWORDS VARCHAR(2000) DEFAULT NULL)\n";
      started = true;
      executeSql(sql, new Object[0]);
    } 
    started = true;
  }
  
  public void stopDB() {
    try {
      if (this.derby)
        DriverManager.getConnection("jdbc:derby:;shutdown=true"); 
    } catch (Exception e) {
      Log.log("SEARCH", 0, e);
    } 
    started = false;
  }
  
  public int executeSql(String sql, Object[] values) {
    int count = 0;
    msg("Connecting to db, executing sql:" + sql);
    if (this.mysql)
      sql = Common.replace_str(sql, "as bigint", "as unsigned"); 
    Connection conn = null;
    PreparedStatement ps = null;
    try {
      conn = getConnection();
      ps = conn.prepareStatement(sql);
      for (int x = 0; x < values.length; x++) {
        if (values[x] instanceof String) {
          ps.setString(x + 1, (String)values[x]);
        } else if (values[x] instanceof Date) {
          ps.setTimestamp(x + 1, new Timestamp(((Date)values[x]).getTime()));
        } else if (values[x] == null) {
          ps.setString(x + 1, (String)null);
        } 
      } 
      count = ps.executeUpdate();
      ps.close();
    } catch (Throwable e) {
      msg(e);
    } 
    if (ps != null)
      try {
        ps.close();
      } catch (Exception exception) {} 
    if (conn != null)
      try {
        conn.close();
      } catch (Exception exception) {} 
    return count;
  }
  
  public Vector executeSqlQuery(String sql, Object[] values, boolean includeColumns, boolean rowCount) {
    return executeSqlQuery(sql, values, new Boolean(includeColumns), new Boolean(rowCount));
  }
  
  public Vector executeSqlQuery(String sql, Object[] values, Boolean includeColumnsB, Boolean rowCount) {
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
    String valuesStr = "";
    for (int x = 0; x < values.length; x++)
      valuesStr = String.valueOf(valuesStr) + values[x].toString() + ","; 
    msg("Connecting to db, executing sql:" + sql + " : " + valuesStr);
    Vector results = new Vector();
    Connection conn = null;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    PreparedStatement ps = null;
    try {
      conn = getConnection();
      boolean fixLimit = false;
      if (this.derby)
        if (sql.indexOf("limit ?") >= 0) {
          sql = sql.replaceAll("limit \\?", "").trim();
          fixLimit = true;
        }  
      ps = conn.prepareStatement(sql);
      for (int i = 0; i < values.length; i++) {
        if (this.derby && fixLimit && i == values.length - 1) {
          ps.setMaxRows(((Integer)values[i]).intValue());
          break;
        } 
        if (values[i] instanceof String) {
          ps.setString(i + 1, (String)values[i]);
        } else if (values[i] instanceof Integer) {
          ps.setInt(i + 1, ((Integer)values[i]).intValue());
        } else if (values[i] instanceof Date) {
          ps.setTimestamp(i + 1, new Timestamp(((Date)values[i]).getTime()));
        } 
      } 
      ResultSet rs = ps.executeQuery();
      Vector cols = new Vector();
      boolean rows = rowCount.booleanValue();
      int row_count = 0;
      while (rs.next()) {
        row_count++;
        if (rows) {
          results.addElement("");
          continue;
        } 
        Properties p = new Properties();
        for (int j = 0; j < rs.getMetaData().getColumnCount(); j++) {
          String key = rs.getMetaData().getColumnLabel(j + 1);
          if (rs.getMetaData().getColumnTypeName(j + 1).equalsIgnoreCase("TIMESTAMP")) {
            try {
              p.put(key, sdf.format(new Date(rs.getTimestamp(j + 1).getTime())));
            } catch (Exception exception) {}
          } else if (rs.getMetaData().getColumnTypeName(j + 1).equalsIgnoreCase("DOUBLE")) {
            try {
              p.put(key, (new StringBuffer(String.valueOf(rs.getLong(j + 1)))).toString());
            } catch (Exception exception) {}
          } else {
            try {
              p.put(key, rs.getString(j + 1));
            } catch (Exception exception) {}
          } 
          if (results.size() == 0)
            cols.addElement(key); 
        } 
        if (results.size() == 0 && includeColumnsB.booleanValue())
          results.addElement(cols); 
        results.addElement(p);
      } 
      msg("Got DB results:" + results.size());
    } catch (Throwable e) {
      msg(e);
    } 
    if (ps != null)
      try {
        ps.close();
      } catch (Exception exception) {} 
    if (conn != null)
      try {
        conn.close();
      } catch (Exception exception) {} 
    return results;
  }
  
  public String get(String key) {
    return ServerStatus.SG(key);
  }
  
  public void msg(String s) {
    if (ServerStatus.BG("search_debug"))
      Log.log("SEARCH", 0, "Search:" + s); 
  }
  
  public void msg(Throwable e) {
    if (ServerStatus.BG("search_debug"))
      Log.log("SEARCH", 0, e); 
  }
  
  public Connection getConnection() throws Throwable {
    if (!started)
      init(); 
    Connection conn = null;
    try {
      if (!get("search_db_driver_file").equals("")) {
        String[] db_drv_files = get("search_db_driver_file").split(";");
        URL[] urls = new URL[db_drv_files.length];
        for (int x = 0; x < db_drv_files.length; x++)
          urls[x] = (new File(db_drv_files[x])).toURI().toURL(); 
        if (cl == null) {
          cl = new URLClassLoader(urls);
          drvCls = Class.forName(get("search_db_driver"), true, cl);
          driver = drvCls.newInstance();
        } 
        Properties props = new Properties();
        props.setProperty("user", get("search_db_user"));
        props.setProperty("password", ServerStatus.thisObj.common_code.decode_pass(get("search_db_pass")));
        conn = driver.connect(get("search_db_url"), props);
      } else {
        Class.forName(get("search_db_driver"));
        conn = DriverManager.getConnection(get("search_db_url"), get("search_db_user"), ServerStatus.thisObj.common_code.decode_pass(get("search_db_pass")));
      } 
      conn.setAutoCommit(true);
    } catch (Exception e) {
      msg(e);
    } 
    return conn;
  }
}
