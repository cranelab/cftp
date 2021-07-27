package crushftp.reports5;

import com.crushftp.client.Common;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.UserTools;
import crushftp.server.ServerSession;
import crushftp.server.ServerStatus;
import crushftp.server.VFS;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class UserFolderSizes {
  Properties server_info = null;
  
  Properties server_settings = null;
  
  public void init(Properties server_settings, Properties server_info) {
    this.server_settings = server_settings;
    this.server_info = server_info;
  }
  
  class sorter implements Comparator {
    Properties allItems;
    
    String sort;
    
    final UserFolderSizes this$0;
    
    sorter(UserFolderSizes this$0) {
      this.this$0 = this$0;
      this.allItems = null;
      this.sort = null;
    }
    
    public void setObj(Properties allItems, String sort) {
      this.allItems = allItems;
      this.sort = sort;
    }
    
    public int compare(Object p1, Object p2) {
      String val1 = ((Properties)p1).getProperty(this.sort, "0");
      String val2 = ((Properties)p2).getProperty(this.sort, "0");
      try {
        if (Float.parseFloat(val1) > Float.parseFloat(val2))
          return -1; 
      } catch (Exception exception) {}
      try {
        if (Float.parseFloat(val1) < Float.parseFloat(val2))
          return 1; 
      } catch (Exception exception) {}
      return val1.compareTo(val2) * 1;
    }
  }
  
  public void generate(Properties stats, Properties params, StringBuffer sb, Properties status) {
    try {
      Common.setupReportDates(params, params.getProperty("show", ""), params.getProperty("startDate"), params.getProperty("endDate"));
      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa");
      SimpleDateFormat mmddyyyy = new SimpleDateFormat("MM/dd/yyyy");
      long start = mmddyyyy.parse(params.getProperty("startDate", "1/1/2000")).getTime();
      long end = mmddyyyy.parse(params.getProperty("endDate", "1/1/2100")).getTime();
      Vector usernames = (Vector)params.get("usernames");
      ReportTools.fixListUsernames(usernames);
      sorter cd1 = new sorter(this);
      Properties userDetails = new Properties();
      cd1.setObj(userDetails, "username");
      Vector sgs = (Vector)this.server_settings.get("server_groups");
      for (int i = 0; i < sgs.size(); i++) {
        String server = sgs.elementAt(i).toString();
        Vector current_user_group_listing = new Vector();
        UserTools.refreshUserList(server, current_user_group_listing);
        int pos = 0;
        for (int x = 0; x < current_user_group_listing.size(); x++) {
          pos++;
          try {
            status.put("status", String.valueOf((int)(pos / current_user_group_listing.size() * 100.0F)) + "%");
          } catch (Exception exception) {}
          String username = current_user_group_listing.elementAt(x).toString();
          boolean user_ok = false;
          for (int xx = 0; xx < usernames.size(); xx++) {
            if (Common.do_search(usernames.elementAt(xx).toString().toUpperCase(), username.toUpperCase(), false, 0))
              user_ok = true; 
          } 
          if (user_ok || usernames.size() <= 0) {
            VFS uVFS = UserTools.ut.getVFS(server, username);
            Properties listerStatus = new Properties();
            listerStatus.put("status", "running");
            Vector v = new Vector();
            Thread listIt = new Thread(new UserFolderSizes$1$lister(this, v, params, uVFS, listerStatus));
            listIt.setName("Reports:UserFolderSizes:getting file list");
            listIt.setPriority(1);
            listIt.start();
            long filesSize = 0L;
            long fileCount = 0L;
            String firstDir = null;
            while (!listerStatus.getProperty("status", "").equals("") || v.size() > 0) {
              if (v.size() == 0) {
                Thread.sleep(100L);
                continue;
              } 
              Properties p = v.elementAt(0);
              v.removeElementAt(0);
              if (p != null) {
                boolean ok = true;
                if (firstDir == null)
                  firstDir = p.getProperty("root_dir", ""); 
                String method = params.getProperty("searchPath", "");
                if (method.equals("contains") && p.getProperty("root_dir", "").toUpperCase().indexOf(params.getProperty("path").toUpperCase()) < 0)
                  ok = false; 
                if (method.equals("starts with") && !p.getProperty("root_dir", "").toUpperCase().startsWith(params.getProperty("path").toUpperCase()))
                  ok = false; 
                if (method.equals("ends with") && !p.getProperty("root_dir", "").toUpperCase().endsWith(params.getProperty("path").toUpperCase()))
                  ok = false; 
                if (method.equals("equals") && !p.getProperty("root_dir", "").toUpperCase().equals(params.getProperty("path").toUpperCase()))
                  ok = false; 
                if (ok) {
                  filesSize += Long.parseLong(p.getProperty("size", "0"));
                  fileCount++;
                } 
              } 
            } 
            Properties user = new Properties();
            user.put("username", username);
            user.put("fileCount", (new StringBuffer(String.valueOf(fileCount))).toString());
            user.put("fileSize", (new StringBuffer(String.valueOf(filesSize))).toString());
            user.put("fileSizeFormatted", Common.format_bytes_short(filesSize));
            if (firstDir == null)
              firstDir = "/"; 
            long total_quota = ServerSession.get_total_quota(firstDir, uVFS, new Properties());
            user.put("quota", (new StringBuffer(String.valueOf(total_quota))).toString());
            user.put("quotaFormatted", Common.format_bytes_short(total_quota));
            if (total_quota == -12345L) {
              user.put("quota", "");
              user.put("quotaFormatted", "unlimited");
            } 
            userDetails.put(username, user);
            uVFS.disconnect();
            uVFS.free();
          } 
        } 
      } 
      Vector users = doSort(userDetails, cd1);
      Properties results = new Properties();
      results.put("users", users);
      results.put("export", params.getProperty("export", ""));
      results.put("params", Common.removeNonStrings(params).toString());
      results.put("paramsObj", Common.removeNonStrings(params));
      if (users.size() > 0)
        sb.append(ServerStatus.thisObj.common_code.getXMLString(results, "results", "WebInterface/Reports/UserFolderSizes.xsl")); 
    } catch (Exception e) {
      Log.log("REPORT", 1, e);
    } 
  }
  
  public Vector doSort(Properties item, sorter c) {
    Enumeration e = item.keys();
    Vector v = new Vector();
    while (e.hasMoreElements()) {
      String key = e.nextElement().toString();
      v.addElement((Properties)item.get(key));
    } 
    Object[] objs = v.toArray();
    Arrays.sort(objs, c);
    v.removeAllElements();
    for (int x = 0; x < objs.length; x++)
      v.addElement(objs[x]); 
    return v;
  }
  
  public void updateItems(Properties p, Properties details, String username, SimpleDateFormat sdf, long start, long end, Properties params) {
    long date = Long.parseLong(p.getProperty("date"));
    if (date < start || date > end)
      return; 
    if (details.get(username.toUpperCase()) == null) {
      Properties pp = new Properties();
      pp.putAll(p);
      pp.remove("speed");
      pp.remove("ip");
      pp.remove("date");
      pp.put("username", username);
      pp.put("files", new Vector());
      pp.put("filesDetail", new Vector());
      details.put(username.toUpperCase(), pp);
    } 
    Properties itemDetails = (Properties)details.get(username.toUpperCase());
    boolean ok = true;
    String method = params.getProperty("searchFilename");
    if (method.equals("contains") && p.getProperty("name", "").toUpperCase().indexOf(params.getProperty("filename").toUpperCase()) < 0)
      ok = false; 
    if (method.equals("starts with") && !p.getProperty("name", "").toUpperCase().startsWith(params.getProperty("filename").toUpperCase()))
      ok = false; 
    if (method.equals("ends with") && !p.getProperty("name", "").toUpperCase().endsWith(params.getProperty("filename").toUpperCase()))
      ok = false; 
    if (method.equals("equals") && !p.getProperty("name", "").toUpperCase().equals(params.getProperty("filename").toUpperCase()))
      ok = false; 
    method = params.getProperty("searchPath");
    if (method.equals("contains") && p.getProperty("path", "").toUpperCase().indexOf(params.getProperty("path").toUpperCase()) < 0)
      ok = false; 
    if (method.equals("starts with") && !p.getProperty("path", "").toUpperCase().startsWith(params.getProperty("path").toUpperCase()))
      ok = false; 
    if (method.equals("ends with") && !p.getProperty("path", "").toUpperCase().endsWith(params.getProperty("path").toUpperCase()))
      ok = false; 
    if (method.equals("equals") && !p.getProperty("path", "").toUpperCase().equals(params.getProperty("path").toUpperCase()))
      ok = false; 
    if (ok) {
      Vector files = (Vector)itemDetails.get("files");
      Vector filesDetail = (Vector)itemDetails.get("filesDetail");
      if (files.indexOf(p.getProperty("url")) < 0) {
        files.addElement(p.getProperty("url"));
        p.put("dates", new Vector());
        Vector dates = (Vector)p.get("dates");
        String dateStr = sdf.format(new Date(date));
        dates.addElement(dateStr);
        filesDetail.addElement(p);
      } else {
        p = filesDetail.elementAt(files.indexOf(p.getProperty("url")));
        Vector dates = (Vector)p.get("dates");
        String dateStr = sdf.format(new Date(date));
        dates.addElement(dateStr);
      } 
    } 
  }
}
