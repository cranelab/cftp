package crushftp.handlers;

import com.crushftp.client.Common;
import java.io.IOException;
import java.util.Properties;

public class LoggingProvider {
  public static Object log_lock = new Object();
  
  public String newTags = "(SERVER),(UPDATE),(PREVIEW),(SMTP),(USER),(SEARCH),(STAT),(REPORT),(SSH_SERVER),(HTTP_SERVER),(DAV_SERVER),(FTP_SERVER),(AS2_SERVER),(EVENT),(LIST),(DOWNLOAD),(UPLOAD),(VFS),(SERVERBEAT),(ACCESS),(QUOTA),(PLUGIN),(AUTH),(BAN),(SYNC),(ALERT),(GENERAL),(FILE_CLIENT),(FTP_CLIENT),(S3_CLIENT),(SSH_CLIENT),(TUNNEL),(DMZ)";
  
  public void checkForLogRoll() {}
  
  public void append_log(String log_data, String check_data, boolean file_only) {}
  
  public Properties getLogSegment(long start, long len, String log_file) throws IOException {
    return null;
  }
  
  public void shutdown() throws IOException {}
  
  public void checkLogPath() {}
  
  public boolean checkFilters(String filtersStr, String log_data) {
    if (filtersStr.trim().equals(""))
      return true; 
    String[] filters = filtersStr.split(",");
    for (int x = 0; x < filters.length; x++) {
      if (Common.do_search(filters[x], log_data, false, 0))
        return false; 
    } 
    return true;
  }
}
