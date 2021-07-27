package crushftp.reports5;

import com.crushftp.client.Common;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.UserTools;
import crushftp.server.ServerStatus;
import crushftp.server.VFS;
import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class NewFiles {
  Properties server_info = null;
  
  Properties server_settings = null;
  
  static Properties cachedDirListings = new Properties();
  
  public void init(Properties server_settings, Properties server_info) {
    this.server_settings = server_settings;
    this.server_info = server_info;
  }
  
  class sorter implements Comparator {
    Properties allItems;
    
    String sort;
    
    final NewFiles this$0;
    
    sorter(NewFiles this$0) {
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
      if (cachedDirListings == null || cachedDirListings.size() == 0) {
        try {
          cachedDirListings = (Properties)Common.readXMLObject(String.valueOf(System.getProperty("crushftp.backup")) + "backup/NewFileReportCache.XML");
        } catch (Exception exception) {}
        if (cachedDirListings == null)
          cachedDirListings = new Properties(); 
      } 
      Common.setupReportDates(params, params.getProperty("show", ""), params.getProperty("startDate"), params.getProperty("endDate"));
      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa");
      SimpleDateFormat mmddyyyy = new SimpleDateFormat("MM/dd/yyyy");
      Vector usernames = (Vector)params.get("usernames");
      ReportTools.fixListUsernames(usernames);
      sorter cd1 = new sorter(this);
      Properties userDetails = new Properties();
      cd1.setObj(userDetails, "username");
      Vector servers = (Vector)this.server_settings.get("server_list");
      Vector compeltedServers = new Vector();
      for (int loop = 0; loop < servers.size(); loop++) {
        Properties server_item = servers.elementAt(loop);
        String server = server_item.getProperty("linkedServer", "").equals("") ? (String.valueOf(server_item.getProperty("ip", "lookup")) + "_" + server_item.getProperty("port", "21")) : server_item.getProperty("linkedServer", "");
        Log.log("REPORT", 2, "NewFiles server:" + server);
        if (compeltedServers.indexOf(server) < 0) {
          compeltedServers.addElement(server);
          Vector current_user_group_listing = new Vector();
          UserTools.refreshUserList(server, current_user_group_listing);
          int pos = 0;
          for (int x = 0; x < current_user_group_listing.size(); x++) {
            pos++;
            try {
              status.put("status", String.valueOf((int)(pos / current_user_group_listing.size() * 100.0F)) + "%");
            } catch (Exception exception) {}
            String username = current_user_group_listing.elementAt(x).toString();
            Log.log("REPORT", 2, "NewFiles username:" + username);
            boolean user_ok = false;
            for (int xx = 0; xx < usernames.size(); xx++) {
              if (Common.do_search(usernames.elementAt(xx).toString().toUpperCase(), username.toUpperCase(), false, 0))
                user_ok = true; 
            } 
            if (user_ok || usernames.size() <= 0) {
              VFS uVFS = UserTools.ut.getVFS(server, username);
              Properties user = UserTools.ut.getUser(server, username, true);
              Properties listerStatus = new Properties();
              listerStatus.put("status", "running");
              Vector v = new Vector();
              Thread listIt = new Thread(new NewFiles$1$lister(this, v, params, uVFS, listerStatus));
              listIt.setName("Reports:NewFiles:getting file list");
              listIt.setPriority(1);
              listIt.start();
              long filesSize = 0L;
              long fileCount = 0L;
              Vector validFiles = new Vector();
              while (!listerStatus.getProperty("status", "").equals("") || v.size() > 0) {
                if (v.size() == 0) {
                  Thread.sleep(100L);
                  continue;
                } 
                Properties p = v.elementAt(0);
                v.removeElementAt(0);
                if (p != null) {
                  boolean ok = true;
                  String method = params.getProperty("searchPath", "");
                  if (method.equals("contains") && (String.valueOf(p.getProperty("root_dir", "")) + p.getProperty("name", "")).toUpperCase().indexOf(params.getProperty("path").toUpperCase()) < 0)
                    ok = false; 
                  if (method.equals("starts with") && !(String.valueOf(p.getProperty("root_dir", "")) + p.getProperty("name", "")).toUpperCase().startsWith(params.getProperty("path").toUpperCase()))
                    ok = false; 
                  if (method.equals("ends with") && !(String.valueOf(p.getProperty("root_dir", "")) + p.getProperty("name", "")).toUpperCase().endsWith(params.getProperty("path").toUpperCase()))
                    ok = false; 
                  if (method.equals("equals") && !(String.valueOf(p.getProperty("root_dir", "")) + p.getProperty("name", "")).toUpperCase().equals(params.getProperty("path").toUpperCase()))
                    ok = false; 
                  if (ok) {
                    filesSize += Long.parseLong(p.getProperty("size", "0"));
                    fileCount++;
                    validFiles.addElement(p);
                  } 
                } 
              } 
              Properties user_cache = (Properties)cachedDirListings.get(username.toUpperCase());
              boolean firstRun = false;
              if (user_cache == null)
                user_cache = new Properties(); 
              cachedDirListings.put(username.toUpperCase(), user_cache);
              for (int i = validFiles.size() - 1; i >= 0; i--) {
                Properties p = validFiles.elementAt(i);
                boolean ok = true;
                if (firstRun) {
                  ok = false;
                } else if (p.getProperty("modified").equals(user_cache.getProperty(Common.replace_str(p.getProperty("url"), "#", "___---POUND---___"), ""))) {
                  ok = false;
                } else if (p.getProperty("name", "").startsWith(".")) {
                  ok = false;
                } else if (p.getProperty("type").equalsIgnoreCase("DIR") && !params.getProperty("onlyFolders", "").equals("true")) {
                  ok = false;
                } else if (p.getProperty("type").equalsIgnoreCase("FILE") && params.getProperty("onlyFolders", "").equals("true")) {
                  ok = false;
                } 
                if (p.getProperty("type").equalsIgnoreCase("DIR") && params.getProperty("onlyFolders", "").equals("true") && p.getProperty("root_dir", "").equals("/"))
                  ok = false; 
                if (!ok)
                  validFiles.removeElementAt(i); 
                user_cache.put(Common.replace_str(p.getProperty("url"), "#", "___---POUND---___"), p.getProperty("modified"));
              } 
              Enumeration keys = user_cache.keys();
              while (keys.hasMoreElements()) {
                String path = keys.nextElement().toString();
                String pathOriginal = path;
                if (path.toUpperCase().startsWith("FILE:")) {
                  URL url = new URL(path);
                  path = url.getPath();
                  path = Common.url_decode(Common.replace_str(path, "#", "___---POUND---___"));
                } 
                if (!(new File(path)).exists() && !(new File(pathOriginal.substring("FILE:".length()))).exists())
                  user_cache.remove(pathOriginal); 
              } 
              if (validFiles.size() > 0) {
                String from = replaceVars(params.getProperty("from", ""), user);
                String to = replaceVars(params.getProperty("to", ""), user);
                String cc = replaceVars(params.getProperty("cc", ""), user);
                String bcc = replaceVars(params.getProperty("bcc", ""), user);
                String subject = replaceVars(params.getProperty("subject", ""), user);
                String body = replaceVars(params.getProperty("body", ""), user);
                String the_body_line = "";
                try {
                  the_body_line = body.substring(body.toUpperCase().indexOf("<LINE>") + "<LINE>".length(), body.toUpperCase().indexOf("</LINE>"));
                } catch (Exception exception) {}
                String lineData = "";
                for (int j = 0; j < validFiles.size(); j++) {
                  Properties p = validFiles.elementAt(j);
                  String the_line = the_body_line;
                  the_line = Common.replace_str(the_line, "%the_file_path%", p.getProperty("root_dir", ""));
                  String name = p.getProperty("name", "");
                  String ext = "";
                  if (name.indexOf(".") >= 0) {
                    ext = name.substring(0, name.lastIndexOf("."));
                    name = name.substring(0, name.lastIndexOf("."));
                  } 
                  the_line = Common.replace_str(the_line, "%the_file_name%", name);
                  the_line = Common.replace_str(the_line, "%the_file_ext%", ext);
                  the_line = Common.replace_str(the_line, "%the_file_size%", p.getProperty("size", ""));
                  the_line = Common.replace_str(the_line, "%the_file_size_formatted%", Common.format_bytes_short(Long.parseLong(p.getProperty("size", "0"))));
                  the_line = Common.replace_str(the_line, "%all%", p.toString());
                  the_line = replaceVars(the_line, user);
                  if (!the_line.trim().equals(""))
                    lineData = String.valueOf(lineData) + the_line + "\r\n"; 
                } 
                Log.log("REPORT", 2, "BODY:<LINE>" + lineData + "</LINE>");
                try {
                  body = Common.replace_str(body, body.substring(body.toUpperCase().indexOf("<LINE>"), body.toUpperCase().indexOf("</LINE>") + "</LINE>".length()), lineData);
                } catch (Exception e) {
                  Log.log("REPORT", 1, e);
                } 
                String emailResult = "";
                if (to.startsWith("REPORT_TEST_") || from.startsWith("REPORT_TEST_")) {
                  emailResult = "Email Skipped";
                } else {
                  try {
                    emailResult = Common.send_mail(this.server_settings.getProperty("discovered_ip"), to, cc, bcc, from, subject, body, this.server_settings.getProperty("smtp_server"), this.server_settings.getProperty("smtp_user"), this.server_settings.getProperty("smtp_pass"), this.server_settings.getProperty("smtp_ssl").equals("true"), this.server_settings.getProperty("smtp_html").equals("true"), null);
                  } catch (Exception e) {
                    Log.log("REPORT", 1, e);
                  } 
                } 
                if (emailResult.toUpperCase().indexOf("SUCCESS") < 0 || Log.log("REPORT", 1, "")) {
                  Log.log("REPORT", 0, "RESULT: " + emailResult + "\r\n");
                  Log.log("REPORT", 0, "FROM: " + from + "\r\n");
                  Log.log("REPORT", 0, "TO: " + to + "\r\n");
                  Log.log("REPORT", 0, "CC: " + cc + "\r\n");
                  Log.log("REPORT", 0, "BCC: " + bcc + "\r\n");
                  Log.log("REPORT", 0, "SUBJECT: " + subject + "\r\n");
                  Log.log("REPORT", 0, "BODY: " + body + "\r\n");
                } 
                if (Log.log("REPORT", 1, ""))
                  try {
                    throw new Exception("Who called?");
                  } catch (Exception e) {
                    Log.log("REPORT", 1, e);
                  }  
                uVFS.disconnect();
                uVFS.free();
              } 
              Properties userObj = new Properties();
              userObj.put("username", username);
              userObj.put("fileCount", (new StringBuffer(String.valueOf(validFiles.size()))).toString());
              userDetails.put(username, userObj);
            } 
          } 
        } 
      } 
      Vector users = doSort(userDetails, cd1);
      Properties results = new Properties();
      results.put("users", users);
      results.put("export", params.getProperty("export", ""));
      Properties params2 = (Properties)params.clone();
      params2.remove("body");
      results.put("params", Common.removeNonStrings(params2).toString());
      results.put("paramsObj", Common.removeNonStrings(params));
      sb.append(ServerStatus.thisObj.common_code.getXMLString(results, "results", "WebInterface/Reports/NewFiles.xsl"));
      (new Common()).writeXMLObject(String.valueOf(System.getProperty("crushftp.backup")) + "backup/NewFileReportCache.XML", cachedDirListings, "cached_dir");
    } catch (Exception e) {
      Log.log("REPORT", 1, e);
    } 
  }
  
  public String replaceVars(String data, Properties user) {
    data = Common.replace_str(data, "%user_name%", user.getProperty("user_name"));
    data = Common.replace_str(data, "%user_pass%", ServerStatus.thisObj.common_code.decode_pass(user.getProperty("password")));
    data = Common.replace_str(data, "%user_email%", user.getProperty("email"));
    return data;
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
