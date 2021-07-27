package crushftp.handlers.log;

import com.crushftp.client.File_S;
import crushftp.handlers.LoggingProvider;
import crushftp.server.ServerStatus;
import java.io.IOException;
import java.util.Properties;
import java.util.Vector;
import org.productivity.java.syslog4j.Syslog;
import org.productivity.java.syslog4j.SyslogIF;

public class LoggingProviderSyslog extends LoggingProvider {
  Vector syslog_servers = new Vector();
  
  String prefix = "";
  
  public LoggingProviderSyslog() {
    String[] hosts = ServerStatus.SG("syslog_host").split(",");
    for (int x = 0; x < hosts.length; x++) {
      if (!hosts[x].trim().equals("")) {
        SyslogIF syslog = Syslog.getInstance(ServerStatus.SG("syslog_protocol"));
        syslog.getConfig().setHost(hosts[x].trim());
        syslog.getConfig().setPort(Integer.parseInt(ServerStatus.SG("syslog_port")));
        syslog.getConfig().setCharSet(ServerStatus.SG("syslog_encoding"));
        this.syslog_servers.addElement(syslog);
      } 
    } 
    this.prefix = (new File_S(ServerStatus.change_vars_to_values_static(ServerStatus.SG("log_location"), null, null, null))).getName();
    if (this.prefix == null)
      this.prefix = ""; 
    if (this.prefix.toUpperCase().endsWith(".log"))
      this.prefix = this.prefix.substring(0, this.prefix.length() - 4); 
    this.prefix = String.valueOf(this.prefix) + ":";
  }
  
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
        logSyslog(log_data);  
  }
  
  private void logSyslog(String log_data) {
    for (int x = 0; x < this.syslog_servers.size(); x++) {
      SyslogIF syslog = this.syslog_servers.elementAt(x);
      syslog.info(String.valueOf(this.prefix) + log_data);
    } 
  }
  
  public Properties getLogSegment(long start, long len, String log_file) throws IOException {
    Properties log = new Properties();
    log.put("log_start_date", "");
    log.put("log_end_date", "");
    log.put("log_start", "0");
    log.put("log_end", "0");
    log.put("log_max", "0");
    log.put("log_data", "");
    log.put("log_segment", "");
    return log;
  }
  
  public void shutdown() throws IOException {}
}
