package crushftp.handlers.log;

import com.crushftp.client.File_S;
import crushftp.handlers.LoggingProvider;
import crushftp.server.ServerStatus;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.Properties;

public class LoggingProviderSQL extends LoggingProvider {
  Driver driver = null;
  
  Connection conn = null;
  
  PreparedStatement ps = null;
  
  long row_num = 0L;
  
  public void checkForLogRoll() {}
  
  public void append_log(String log_data, String check_data, boolean file_only) {
    if (!ServerStatus.BG("write_to_log"))
      return; 
    log_data = String.valueOf(log_data.trim()) + "\r\n";
    boolean ok = true;
    if (log_data.indexOf("/WebInterface/") >= 0)
      if (ServerStatus.SG("log_allow_str").indexOf("(WEBINTERFACE)") < 0)
        ok = false;  
    if ((ok && ServerStatus.SG("log_allow_str").indexOf("(" + check_data + ")") >= 0) || this.newTags.indexOf("(" + check_data + ")") >= 0)
      if (checkFilters(ServerStatus.SG("filter_log_text"), log_data))
        logDB(log_data, check_data);  
  }
  
  public Properties getLogSegment(long start, long len, String log_file) throws IOException {
    long lenOriginal = len;
    if (this.row_num - start > 1000L) {
      if (len > 1L) {
        len /= 1048576L;
        len *= 1000L;
      } 
      start += lenOriginal - len;
    } else {
      start++;
    } 
    Properties log = new Properties();
    log.put("log_start_date", "");
    log.put("log_end_date", "");
    log.put("log_start", "0");
    log.put("log_end", "0");
    log.put("log_max", "0");
    log.put("log_data", "");
    String startDate = "";
    String endDate = "";
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    if (len <= 0L || len > 52428800L)
      len = 1048576L; 
    PreparedStatement ps2 = null;
    ResultSet rs = null;
    try {
      synchronized (log_lock) {
        if (this.conn == null)
          this.conn = getConnection(); 
        ps2 = this.conn.prepareStatement(ServerStatus.SG("logging_db_query"));
        ps2.setLong(1, start);
        ps2.setLong(2, start + len);
        rs = ps2.executeQuery();
        long time = 0L;
        long row_num_temp = 0L;
        while (rs.next()) {
          String log_data = rs.getString(1).trim();
          time = rs.getLong(2);
          row_num_temp = rs.getLong(3);
          baos.write(log_data.getBytes("UTF8"));
          baos.write("\r\n".getBytes());
          if (startDate.equals(""))
            startDate = ServerStatus.thisObj.logDateFormat.format(new Date(time)); 
        } 
        endDate = ServerStatus.thisObj.logDateFormat.format(new Date(time));
        log.put("log_start", (new StringBuffer(String.valueOf(start))).toString());
        log.put("log_end", (new StringBuffer(String.valueOf(row_num_temp))).toString());
        log.put("log_max", (new StringBuffer(String.valueOf(this.row_num))).toString());
        log.put("log_segment", new String(baos.toByteArray(), "UTF8"));
        log.put("log_start_date", (new StringBuffer(String.valueOf(startDate))).toString());
        log.put("log_end_date", (new StringBuffer(String.valueOf(endDate))).toString());
      } 
    } catch (Exception e) {
      System.out.println(new Date());
      e.printStackTrace();
      try {
        this.conn.close();
      } catch (Exception exception) {}
      this.conn = null;
    } finally {
      try {
        if (rs != null)
          rs.close(); 
        if (ps2 != null)
          ps2.close(); 
      } catch (Exception exception) {}
    } 
    return log;
  }
  
  private void logDB(String line, String tag) {
    synchronized (log_lock) {
      try {
        if (this.conn == null) {
          if (this.ps != null)
            this.ps.close(); 
          this.ps = null;
          this.conn = getConnection();
          Statement st = this.conn.createStatement();
          ResultSet rs = st.executeQuery(ServerStatus.SG("logging_db_query_count"));
          rs.next();
          this.row_num = rs.getLong(1);
          rs.close();
          st.close();
        } 
        if (this.ps == null)
          this.ps = this.conn.prepareStatement(ServerStatus.SG("logging_db_insert")); 
        this.ps.setLong(1, System.currentTimeMillis());
        this.ps.setString(2, tag);
        this.ps.setString(3, line);
        this.ps.setLong(4, this.row_num + 1L);
        this.ps.executeUpdate();
        this.row_num++;
      } catch (Exception e) {
        System.out.println(new Date());
        e.printStackTrace();
        try {
          if (this.ps != null)
            this.ps.close(); 
        } catch (Exception exception) {}
        try {
          if (this.conn != null)
            this.conn.close(); 
        } catch (Exception exception) {}
        this.ps = null;
        this.conn = null;
      } 
    } 
  }
  
  public void shutdown() throws IOException {}
  
  public Connection getConnection() throws Exception {
    Connection conn = null;
    String db_url = ServerStatus.SG("logging_db_url");
    try {
      String pass = ServerStatus.thisObj.common_code.decode_pass(ServerStatus.SG("logging_db_pass"));
      if (!ServerStatus.SG("logging_db_driver_file").equals("")) {
        String[] db_drv_files = ServerStatus.SG("logging_db_driver_file").split(";");
        URL[] urls = new URL[db_drv_files.length];
        for (int x = 0; x < db_drv_files.length; x++)
          urls[x] = (new File_S(db_drv_files[x])).toURI().toURL(); 
        AccessController.doPrivileged(new PrivilegedAction(this, urls) {
              final LoggingProviderSQL this$0;
              
              private final URL[] val$urls;
              
              public Object run() {
                try {
                  this.this$0.driver = (Driver)Class.forName(ServerStatus.SG("logging_db_driver"), true, new URLClassLoader(this.val$urls)).newInstance();
                } catch (Exception e) {
                  System.out.println(new Date());
                  e.printStackTrace();
                } 
                return null;
              }
            });
        Properties props = new Properties();
        props.setProperty("user", ServerStatus.SG("logging_db_user"));
        props.setProperty("password", pass);
        conn = this.driver.connect(db_url, props);
      } else {
        Class.forName(ServerStatus.SG("logging_db_driver"));
        conn = DriverManager.getConnection(db_url, ServerStatus.SG("logging_db_user"), pass);
      } 
      conn.setAutoCommit(true);
    } catch (Exception e) {
      System.out.println(new Date());
      e.printStackTrace();
    } 
    return conn;
  }
}
