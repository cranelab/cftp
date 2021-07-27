package crushftp.handlers.log;

import crushftp.handlers.LoggingProvider;
import crushftp.server.ServerStatus;
import java.io.IOException;
import java.util.Properties;
import org.productivity.java.syslog4j.Syslog;
import org.productivity.java.syslog4j.SyslogIF;

public class LoggingProviderSyslog extends LoggingProvider {
  SyslogIF syslog = null;
  
  public LoggingProviderSyslog() {
    this.syslog = Syslog.getInstance(ServerStatus.SG("syslog_protocol"));
    this.syslog.getConfig().setHost(ServerStatus.SG("syslog_host"));
    this.syslog.getConfig().setPort(Integer.parseInt(ServerStatus.SG("syslog_port")));
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
    this.syslog.info(log_data);
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
