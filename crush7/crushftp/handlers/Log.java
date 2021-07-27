package crushftp.handlers;

import com.crushftp.client.Common;
import com.crushftp.client.VRL;
import crushftp.server.ServerStatus;
import java.util.Date;
import java.util.Properties;

public class Log {
  public static boolean log(String tag, int level, String s) {
    if (ServerStatus.IG("log_debug_level") >= level) {
      if (s.trim().length() > 0) {
        if (ServerStatus.thisObj != null && ServerStatus.thisObj.logDateFormat != null)
          s = String.valueOf(ServerStatus.thisObj.logDateFormat.format(new Date())) + "|" + s; 
        Properties p = new Properties();
        p.put("tag", tag);
        p.put("level", (new StringBuffer(String.valueOf(level))).toString());
        if (s.indexOf("url=") >= 0) {
          String url = s.substring(s.indexOf("url=") + 4).trim();
          if (url.indexOf(",") >= 0)
            url = url.substring(0, url.indexOf(",")).trim(); 
          try {
            VRL vrl = new VRL(url);
            if (vrl.getUsername() != null && !vrl.getUsername().equals("")) {
              String url2 = String.valueOf(vrl.getProtocol()) + "://" + vrl.getUsername() + ":********" + "@" + vrl.getHost() + ":" + vrl.getPort() + vrl.getPath();
              s = Common.replace_str(s, url, url2);
            } 
          } catch (Exception e) {
            e.printStackTrace();
          } 
        } 
        if (s.indexOf("password=") >= 0) {
          String url = s.substring(s.indexOf("password=") + 4).trim();
          if (url.indexOf(",") >= 0)
            url = url.substring(0, url.indexOf(",")).trim(); 
          String url2 = "password=***********";
          s = Common.replace_str(s, url, url2);
        } 
        p.put("data", s);
        Common.log.addElement(p);
      } 
      return true;
    } 
    return false;
  }
  
  public static boolean log(String tag, int level, Throwable e) {
    if (ServerStatus.IG("log_debug_level") >= level) {
      log(tag, level, Thread.currentThread().getName());
      log(tag, level, e.toString());
      StackTraceElement[] ste = e.getStackTrace();
      for (int x = 0; x < ste.length; x++)
        log(tag, level, String.valueOf(ste[x].getClassName()) + "." + ste[x].getMethodName() + ":" + ste[x].getLineNumber()); 
      if (level >= 2)
        e.printStackTrace(); 
      return true;
    } 
    return false;
  }
  
  public static boolean log(String tag, int level, Exception e) {
    return log(tag, level, e);
  }
}
