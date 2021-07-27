package crushftp.handlers;

import com.crushftp.client.Base64;
import com.crushftp.client.File_S;
import com.crushftp.client.VRL;
import com.crushftp.client.Worker;
import crushftp.server.ServerStatus;
import crushftp.server.VFS;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

public class SyncTools extends Thread {
  public static String minSyncVersion = "03.12.01";
  
  public static SyncTools dbt = null;
  
  static URLClassLoader cl = null;
  
  static Class drvCls = null;
  
  static Driver driver = null;
  
  Properties settings = new Properties();
  
  public boolean derby = false;
  
  protected static boolean started = false;
  
  static long lastU = 0L;
  
  public static Properties userSyncAgents = new Properties();
  
  SimpleDateFormat modified_sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
  
  public static final Vector connPool = new Vector();
  
  public static final Properties statementCache = new Properties();
  
  public static Properties cachedSyncList = new Properties();
  
  public static final OutputStream DEV_NULL = new OutputStream() {
      public void write(int b) {}
    };
  
  public SyncTools(Properties settings) {
    this.settings = settings;
    init();
  }
  
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
    p.put("syncs_debug", "true");
    p.put("syncs_db_driver_file", "");
    p.put("syncs_db_driver", "org.apache.derby.jdbc.EmbeddedDriver");
    p.put("syncs_db_url", "jdbc:derby:" + System.getProperty("crushftp.sync") + "syncsDB;create=true");
    p.put("syncs_db_url", "jdbc:derby:syncsDB;create=true");
    p.put("syncs_db_user", "app");
    p.put("syncs_db_pass", "");
    p.put("syncs_insert_journal", "INSERT INTO FILE_JOURNAL (RID, SYNC_ID, ITEM_PATH, EVENT_TYPE, EVENT_TIME, CLIENTID, PRIOR_MD5) VALUES (?,?,?,?,?,?,?)");
    p.put("syncs_delete_journal", "DELETE from FILE_JOURNAL where RID = ?");
    p.put("syncs_delete_journal_expired", "DELETE from FILE_JOURNAL where EVENT_TIME < ?");
    p.put("syncs_get_journal", "select * from FILE_JOURNAL where SYNC_ID = ? and RID > ? and (CLIENTID <> ? or CLIENTID is null) order by RID");
  }
  
  public synchronized void init() {
    if (started)
      return; 
    System.getProperties().put("derby.stream.error.field", "crushftp.handlers.SyncTools.DEV_NULL");
    this.derby = (this.settings.getProperty("syncs_db_driver").toUpperCase().indexOf("DERBY") >= 0);
    if (this.settings.getProperty("syncs_db_url").equals("jdbc:derby:syncsDB;create=true") && Common.machine_is_x())
      this.settings.put("syncs_db_url", "jdbc:derby:" + System.getProperty("crushftp.sync") + "syncsDB;create=true"); 
    if (this.settings.getProperty("syncs_get_journal").indexOf("FILE_ITEMS") >= 0) {
      Properties p = new Properties();
      setDefaults(p);
      this.settings.put("syncs_get_journal", p.getProperty("syncs_get_journal"));
    } 
    if (this.derby) {
      String script = "";
      script = String.valueOf(script) + "CREATE TABLE FILE_JOURNAL(RID DOUBLE NOT NULL PRIMARY KEY,SYNC_ID VARCHAR(255) NOT NULL,ITEM_PATH VARCHAR(2000) NOT NULL, EVENT_TYPE VARCHAR(20) NOT NULL,EVENT_TIME TIMESTAMP NOT NULL,CLIENTID VARCHAR(20) NOT NULL, PRIOR_MD5 VARCHAR(50) NOT NULL)\n";
      if (this.derby && !(new File_S(String.valueOf(System.getProperty("crushftp.sync", "./")) + "syncsDB/")).exists()) {
        started = true;
        BufferedReader br = new BufferedReader(new StringReader(script));
        String data = "";
        try {
          while ((data = br.readLine()) != null)
            executeSql(data, new Object[0]); 
        } catch (IOException iOException) {}
      } 
    } 
    Runtime.getRuntime().addShutdownHook(this);
    dbt = this;
    started = true;
  }
  
  public void run() {
    try {
      stopDB();
    } catch (Exception e) {
      msg(e);
    } 
  }
  
  public void stopDB() throws Exception {
    if (!started)
      return; 
    if (this.derby)
      try {
        DriverManager.getConnection("jdbc:derby:;shutdown=true").close();
      } catch (Throwable e) {
        Log.log("SYNC", 3, e);
      }  
    started = false;
  }
  
  public void executeSql(String sql, Object[] values) {
    msg("Connecting to db, executing sql:" + sql);
    Connection conn = null;
    boolean loop = true;
    PreparedStatement ps = null;
    while (loop) {
      loop = false;
      try {
        conn = getConnection();
        if (statementCache.containsKey(sql)) {
          ps = (PreparedStatement)statementCache.remove(sql);
        } else {
          ps = conn.prepareStatement(sql);
        } 
        for (int x = 0; x < values.length; x++) {
          if (values[x] instanceof String) {
            ps.setString(x + 1, (String)values[x]);
          } else if (values[x] instanceof Date) {
            ps.setTimestamp(x + 1, new Timestamp(((Date)values[x]).getTime()));
          } else if (values[x] instanceof Long) {
            ps.setLong(x + 1, ((Long)values[x]).longValue());
          } else if (values[x] == null) {
            ps.setString(x + 1, (String)null);
          } 
        } 
        ps.executeUpdate();
        if (statementCache.containsKey(sql)) {
          ps.close();
          continue;
        } 
        statementCache.put(sql, ps);
      } catch (Throwable e) {
        msg(e);
        if (e.toString().toUpperCase().indexOf("CONNECTION") >= 0)
          loop = true; 
      } 
    } 
  }
  
  public Vector executeSqlQuery(String sql, Object[] values, boolean includeColumns) {
    return executeSqlQuery(sql, values, Boolean.valueOf(includeColumns));
  }
  
  public Vector executeSqlQuery(String sql, Object[] values, Boolean includeColumnsB) {
    msg("Connecting to db, executing sql:" + sql);
    Vector results = new Vector();
    Connection conn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      conn = getConnection();
      msg("Connecting to db, got conenction:" + conn);
      if (statementCache.containsKey(sql)) {
        ps = (PreparedStatement)statementCache.remove(sql);
      } else {
        ps = conn.prepareStatement(sql);
      } 
      msg("Adding values to prepared statement:" + values.length);
      for (int x = 0; x < values.length; x++) {
        if (values[x] instanceof String) {
          ps.setString(x + 1, (String)values[x]);
        } else if (values[x] instanceof Long) {
          ps.setLong(x + 1, ((Long)values[x]).longValue());
        } else if (values[x] instanceof Date) {
          ps.setTimestamp(x + 1, new Timestamp(((Date)values[x]).getTime()));
        } 
      } 
      msg("Executing sync query...");
      rs = ps.executeQuery();
      Vector cols = new Vector();
      msg("Looping through sync query results...");
      while (rs.next()) {
        Properties p = new Properties();
        for (int i = 0; i < ps.getMetaData().getColumnCount(); i++) {
          String key = ps.getMetaData().getColumnLabel(i + 1);
          if (ps.getMetaData().getColumnTypeName(i + 1).equalsIgnoreCase("TIMESTAMP")) {
            try {
              p.put(key, this.modified_sdf.format(new Date(rs.getTimestamp(i + 1).getTime())));
            } catch (Exception e) {
              Common.debug(1, e);
            } 
          } else if (ps.getMetaData().getColumnTypeName(i + 1).equalsIgnoreCase("DOUBLE")) {
            try {
              p.put(key, (new StringBuffer(String.valueOf(rs.getLong(i + 1)))).toString());
            } catch (Exception e) {
              msg(e);
            } 
          } else {
            try {
              String val = rs.getString(i + 1);
              if (val != null)
                p.put(key, val); 
            } catch (Exception e) {
              msg(e);
            } 
          } 
          if (results.size() == 0)
            cols.addElement(key); 
        } 
        if (results.size() == 0 && includeColumnsB.booleanValue())
          results.addElement(cols); 
        results.addElement(p);
      } 
      msg("Done looping through sync query results..." + results.size());
    } catch (Throwable e) {
      msg(e);
    } 
    if (rs != null)
      try {
        rs.close();
      } catch (Exception e) {
        Common.debug(1, e);
      }  
    if (statementCache.containsKey(sql)) {
      try {
        ps.close();
      } catch (Exception e) {
        Common.debug(1, e);
      } 
    } else if (ps != null) {
      statementCache.put(sql, ps);
    } 
    return results;
  }
  
  public String get(String key) {
    return this.settings.getProperty(key, "");
  }
  
  public void msg(String s) {
    if (this.settings.getProperty("syncs_debug").equals("true"))
      Common.debug(0, "SQL:" + s); 
  }
  
  public void msg(Throwable e) {
    Common.debug(0, e);
  }
  
  public Connection getConnection() throws Exception {
    if (!started)
      init(); 
    synchronized (connPool) {
      if (connPool.size() >= 5) {
        Connection c = connPool.remove(0);
        if (!c.isClosed()) {
          connPool.addElement(c);
          return c;
        } 
      } 
    } 
    Connection conn = null;
    String syncs_db_url = get("syncs_db_url");
    try {
      String pass = get("syncs_db_pass");
      pass = ServerStatus.thisObj.common_code.decode_pass(pass);
      if (!get("syncs_db_driver_file").equals("")) {
        String[] db_drv_files = get("syncs_db_driver_file").split(";");
        URL[] urls = new URL[db_drv_files.length];
        for (int x = 0; x < db_drv_files.length; x++)
          urls[x] = (new File_S(db_drv_files[x])).toURI().toURL(); 
        synchronized (this.settings) {
          if (cl == null)
            AccessController.doPrivileged(new PrivilegedAction(this, urls) {
                  final SyncTools this$0;
                  
                  private final URL[] val$urls;
                  
                  public Object run() {
                    try {
                      SyncTools.cl = new URLClassLoader(this.val$urls);
                      SyncTools.drvCls = Class.forName(this.this$0.get("syncs_db_driver"), true, SyncTools.cl);
                      SyncTools.driver = SyncTools.drvCls.newInstance();
                    } catch (Exception e) {
                      this.this$0.msg(e);
                    } 
                    return null;
                  }
                }); 
        } 
        Properties props = new Properties();
        props.setProperty("user", get("syncs_db_user"));
        props.setProperty("password", pass);
        conn = driver.connect(syncs_db_url, props);
      } else {
        Class.forName(get("syncs_db_driver"));
        conn = DriverManager.getConnection(syncs_db_url, get("syncs_db_user"), pass);
      } 
      conn.setAutoCommit(true);
    } catch (Exception e) {
      msg(e);
    } 
    connPool.addElement(conn);
    return conn;
  }
  
  public static Vector getSyncTableData(String syncIDTemp, long rid, String table, String clientid, String root_dir, VFS uVFS) {
    String url2 = "";
    try {
      url2 = uVFS.get_item(root_dir).getProperty("url");
    } catch (Exception e) {
      Log.log("SERVER", 0, e);
    } 
    String url = url2;
    if (table.equalsIgnoreCase("journal")) {
      Vector list = dbt.executeSqlQuery(dbt.get("syncs_get_journal"), new Object[] { syncIDTemp, Long.valueOf(rid), clientid }, false);
      for (int x = 0; x < list.size(); x++) {
        Properties p = list.elementAt(x);
        Log.log("SERVER", 2, "URL:" + url);
        Log.log("SERVER", 2, "path:" + p.getProperty("ITEM_PATH").substring(1));
        Log.log("SERVER", 2, "new path:" + (new VRL(String.valueOf(url) + p.getProperty("ITEM_PATH").substring(1))).getPath());
        Properties dir_item = null;
        try {
          String item_path = p.getProperty("ITEM_PATH");
          if (item_path.startsWith("//"))
            item_path = item_path.substring(1); 
          p.put("ITEM_PATH", item_path);
          dir_item = uVFS.get_item(String.valueOf(uVFS.thisSession.uiSG("root_dir").substring(1)) + item_path);
        } catch (Exception e) {
          Log.log("SERVER", 0, e);
        } 
        if (dir_item == null) {
          dir_item = new Properties();
          dir_item.put("type", p.getProperty("ITEM_PATH").endsWith("/") ? "DIR" : "FILE");
        } 
        p.put("ITEM_MODIFIED", dir_item.getProperty("modified", "0"));
        p.put("ITEM_SIZE", dir_item.getProperty("size", "0"));
        p.put("ITEM_TYPE", dir_item.getProperty("type", "DIR").equalsIgnoreCase("DIR") ? "D" : "F");
      } 
      return list;
    } 
    if (table.equalsIgnoreCase("file")) {
      Vector list2 = null;
      boolean startThread = false;
      if (cachedSyncList.containsKey(url)) {
        list2 = (Vector)cachedSyncList.get(url);
      } else {
        list2 = new Vector();
        cachedSyncList.put(url, list2);
        startThread = true;
      } 
      Vector list = list2;
      try {
        String original_url = url;
        if (startThread)
          Worker.startWorker(new Runnable(uVFS, list, root_dir) {
                private final VFS val$uVFS;
                
                private final Vector val$list;
                
                private final String val$root_dir;
                
                public void run() {
                  try {
                    this.val$uVFS.getListing(this.val$list, this.val$root_dir, 99, 50000, true);
                    this.val$list.addElement("DONE");
                  } catch (Exception e) {
                    Log.log("SYNC", 1, e);
                    this.val$list.addElement("ERROR:" + e.toString());
                  } 
                }
              }"Sync listing:" + url); 
        Vector list3 = new Vector();
        do {
          int loops = 0;
          while (list.size() == 0 && loops++ < 600)
            Thread.sleep(100L); 
          if (loops >= 29) {
            cachedSyncList.remove(url);
            throw new Exception("Timeout waiting for list data...");
          } 
          if (list.size() == 1 && list.elementAt(0) instanceof String) {
            String msg = list.elementAt(0).toString();
            if (list3.size() == 0) {
              cachedSyncList.remove(url);
              if (!msg.equals("DONE"))
                throw new Exception(msg); 
            } 
            break;
          } 
          Properties dir_item = (Properties)list.remove(0);
          Properties p = new Properties();
          String item_path = Common.url_decode(dir_item.getProperty("url")).substring(original_url.length() - 1);
          if (!item_path.startsWith("/"))
            item_path = "/" + item_path; 
          if (item_path.startsWith("//"))
            item_path = item_path.substring(1); 
          p.put("ITEM_PATH", item_path);
          p.put("ITEM_MODIFIED", dir_item.getProperty("modified"));
          p.put("ITEM_SIZE", dir_item.getProperty("size"));
          p.put("ITEM_TYPE", dir_item.getProperty("type").equalsIgnoreCase("DIR") ? "D" : "F");
          list3.addElement(p);
        } while (list3.size() <= 2000);
        return list3;
      } catch (Exception e) {
        Log.log("SYNC", 0, e);
      } 
    } else if (table.startsWith("file_")) {
      int pos = Integer.parseInt(table.substring("file_".length()));
      Properties sync_info = null;
      boolean startThread = false;
      if (cachedSyncList.containsKey(String.valueOf(clientid) + url + uVFS.username)) {
        sync_info = (Properties)cachedSyncList.get(String.valueOf(clientid) + url + uVFS.username);
      } else {
        sync_info = new Properties();
        sync_info.put("temp_list", new Vector());
        sync_info.put("current_list", new Vector());
        cachedSyncList.put(String.valueOf(clientid) + url + uVFS.username, sync_info);
        startThread = true;
      } 
      int last_pos = Integer.parseInt(sync_info.getProperty("last_pos", "0"));
      Vector current_list = (Vector)sync_info.get("current_list");
      sync_info.put("last_pos", (new StringBuffer(String.valueOf(pos))).toString());
      for (int x = last_pos; x < pos; x++)
        current_list.setElementAt("", x); 
      Vector list = (Vector)sync_info.get("temp_list");
      try {
        String original_url = url;
        if (startThread)
          Worker.startWorker(new Runnable(uVFS, list, root_dir) {
                private final VFS val$uVFS;
                
                private final Vector val$list;
                
                private final String val$root_dir;
                
                public void run() {
                  try {
                    this.val$uVFS.getListing(this.val$list, this.val$root_dir, 99, 10000, true);
                    this.val$list.addElement("DONE");
                  } catch (Exception e) {
                    Log.log("SYNC", 1, e);
                    this.val$list.addElement("ERROR:" + e.toString());
                  } 
                }
              }"Sync listing:" + url); 
        Vector list3 = new Vector();
        while (true) {
          int loops = 0;
          while (list.size() == 0 && loops++ < 1200)
            Thread.sleep(100L); 
          if (loops >= 1190 && list.size() == 0) {
            cachedSyncList.remove(url);
            throw new Exception("Timeout waiting for list data...");
          } 
          if (list.size() == 1 && list.elementAt(0) instanceof String) {
            String msg = list.elementAt(0).toString();
            if (list3.size() == 0) {
              cachedSyncList.remove(String.valueOf(clientid) + url + uVFS.username);
              if (!msg.equals("DONE"))
                throw new Exception(msg); 
            } 
            break;
          } 
          while (pos >= current_list.size() && list.size() > 0) {
            Properties dir_item = (Properties)list.remove(0);
            current_list.addElement(dir_item);
          } 
          if (pos < current_list.size()) {
            Properties dir_item = (Properties)current_list.elementAt(pos++);
            String item_path = Common.url_decode(dir_item.getProperty("url")).substring(original_url.length() - 1);
            if (!item_path.startsWith("/"))
              item_path = "/" + item_path; 
            if (item_path.startsWith("//"))
              item_path = item_path.substring(1); 
            Properties p = new Properties();
            p.put("ITEM_PATH", item_path);
            p.put("ITEM_MODIFIED", dir_item.getProperty("modified"));
            p.put("ITEM_SIZE", dir_item.getProperty("size"));
            p.put("ITEM_TYPE", dir_item.getProperty("type").equalsIgnoreCase("DIR") ? "D" : "F");
            list3.addElement(p);
            if (list3.size() >= 1000)
              break; 
          } 
        } 
        return list3;
      } catch (Exception e) {
        Log.log("SYNC", 0, e);
      } 
    } 
    return null;
  }
  
  public static void addJournalEntry(String syncIDTemp, String path, String change, String clientid, String prior_md5) throws Exception {
    Log.log("SYNC", 2, "Event Type:" + change + ":" + path);
    dbt.executeSql(dbt.get("syncs_insert_journal"), new Object[] { Long.valueOf(u()), syncIDTemp, path, change, new Date(), clientid, (new StringBuffer(String.valueOf(prior_md5))).toString() });
  }
  
  public static void deleteJournalItem(String rid) {
    dbt.executeSql(dbt.get("syncs_delete_journal"), new Object[] { Long.valueOf(Long.parseLong(rid)) });
  }
  
  public static void purgeExpired(long time) {
    dbt.executeSql(dbt.get("syncs_delete_journal_expired"), new Object[] { new Date(time) });
  }
  
  public static Properties getAllAgents() {
    Properties all = new Properties();
    Enumeration keys = userSyncAgents.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      all.putAll((Properties)userSyncAgents.get(key));
    } 
    return all;
  }
  
  public static Object getSyncPrefs(Properties request) throws Exception {
    synchronized (userSyncAgents) {
      Properties agents = (Properties)userSyncAgents.get(request.getProperty("user_name").toUpperCase());
      if (agents == null) {
        agents = new Properties();
        userSyncAgents.put(request.getProperty("user_name").toUpperCase(), agents);
      } 
      if (!agents.containsKey(request.getProperty("clientid"))) {
        Properties properties = new Properties();
        properties.put("queue", new Vector());
        agents.put(request.getProperty("clientid"), properties);
      } 
      if (request.getProperty("site").indexOf("(CONNECT)") >= 0)
        agents = getAllAgents(); 
      Properties agent = (Properties)agents.get(request.getProperty("clientid"));
      agent.putAll(request);
      agent.remove("command");
      agent.put("ping", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      agent.put("ip", request.getProperty("user_ip"));
      Vector v = new Vector();
      v.addAll((Vector)agent.get("queue"));
      ((Vector)agent.get("queue")).removeAllElements();
      return v;
    } 
  }
  
  public static void sendSyncResult(Properties request) throws Exception {
    synchronized (userSyncAgents) {
      Properties agents = (Properties)userSyncAgents.get(request.getProperty("user_name").toUpperCase());
      if (agents == null) {
        agents = new Properties();
        userSyncAgents.put(request.getProperty("user_name").toUpperCase(), agents);
      } 
      if (request.getProperty("site").indexOf("(CONNECT)") >= 0)
        agents = getAllAgents(); 
      Properties agent = (Properties)agents.get(request.getProperty("clientid"));
      agent.put("ping", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      Object o = Common.readXMLObject(new ByteArrayInputStream(Base64.decode(request.getProperty("result"))));
      agent.put(request.getProperty("resultid", "0"), o);
    } 
  }
  
  public static Object getSyncAgents(Properties request) throws Exception {
    Vector v = new Vector();
    synchronized (userSyncAgents) {
      Properties agents = (Properties)userSyncAgents.get(request.getProperty("user_name").toUpperCase());
      if (request.getProperty("site").indexOf("(CONNECT)") >= 0)
        agents = getAllAgents(); 
      if (agents != null) {
        Enumeration keys = agents.keys();
        while (keys.hasMoreElements()) {
          String key = keys.nextElement().toString();
          Properties agent = (Properties)agents.get(key);
          if (System.currentTimeMillis() - Long.parseLong(agent.getProperty("ping")) < 45000L)
            v.addElement(agent); 
          if (System.currentTimeMillis() - Long.parseLong(agent.getProperty("ping")) > 30000L) {
            Vector queue = (Vector)agent.get("queue");
            Properties command = new Properties();
            command.put("COMMAND", "NOOP");
            command.put("RESULTID", Common.makeBoundary());
            queue.addElement(command);
          } 
        } 
      } 
    } 
    return v;
  }
  
  public static Object sendSyncCommand(Properties request) throws Exception {
    Object o = new Properties();
    Properties agent = null;
    Vector queue = null;
    Properties command = new Properties();
    synchronized (userSyncAgents) {
      Properties agents = (Properties)userSyncAgents.get(request.getProperty("user_name").toUpperCase());
      if (request.getProperty("site").indexOf("(CONNECT)") >= 0)
        agents = getAllAgents(); 
      agent = (Properties)agents.get(request.getProperty("agentid"));
      queue = (Vector)agent.get("queue");
      Enumeration keys = request.keys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement().toString();
        if (key.startsWith("sync_")) {
          Object val = request.getProperty(key);
          if (key.endsWith("_obj"))
            val = Common.readXMLObject(new ByteArrayInputStream(request.getProperty(key).getBytes("UTF8"))); 
          command.put(key.substring("sync_".length()).toUpperCase(), val);
        } 
      } 
      command.put("RESULTID", Common.makeBoundary());
      queue.addElement(command);
    } 
    int loops = 0;
    while (queue.contains(command) && loops++ < 100)
      Thread.sleep(100L); 
    loops = 0;
    while (!agent.containsKey(command.getProperty("RESULTID")) && loops++ < 100)
      Thread.sleep(100L); 
    if (agent.containsKey(command.getProperty("RESULTID")))
      o = agent.remove(command.getProperty("RESULTID")); 
    return o;
  }
  
  public static Object getSyncXMLList(Properties request) throws Exception {
    Properties agent = null;
    Vector queue = null;
    Properties command = new Properties();
    synchronized (userSyncAgents) {
      Properties agents = (Properties)userSyncAgents.get(request.getProperty("user_name").toUpperCase());
      if (request.getProperty("site").indexOf("(CONNECT)") >= 0)
        agents = getAllAgents(); 
      agent = (Properties)agents.get(request.getProperty("get_from_agentid"));
      queue = (Vector)agent.get("queue");
      command.put("COMMAND", "list_folder");
      command.put("PATH", request.getProperty("path"));
      command.put("PASSWORD", request.getProperty("admin_password"));
      command.put("RESULTID", Common.makeBoundary());
      queue.addElement(command);
    } 
    int loops = 0;
    while (queue.contains(command) && loops++ < 100)
      Thread.sleep(100L); 
    loops = 0;
    while (!agent.containsKey(command.getProperty("RESULTID")) && loops++ < 100)
      Thread.sleep(100L); 
    Vector items = new Vector();
    if (agent.containsKey(command.getProperty("RESULTID")))
      items = (Vector)agent.remove(command.getProperty("RESULTID")); 
    return items;
  }
}
