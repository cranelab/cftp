package crushftp.handlers;

import com.crushftp.client.Common;
import com.crushftp.client.File_B;
import crushftp.db.SearchHandler;
import crushftp.server.ServerSessionAJAX;
import crushftp.server.ServerStatus;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.Vector;

public class TaskBridge {
  public static String doShare(String body, Vector itemsFound, Properties info) throws Exception {
    String original_share_key = "";
    String share_key = "";
    if (body.indexOf("{share:") >= 0) {
      share_key = body.substring(body.indexOf("{share:") + 1, body.indexOf("}", body.indexOf("{share:")));
      original_share_key = share_key;
    } else {
      share_key = body.substring(body.indexOf("{share_") + 1, body.indexOf("}", body.indexOf("{share_")));
      original_share_key = share_key;
      share_key = share_key.replace('_', ':');
    } 
    SimpleDateFormat date_time = null;
    Vector web_customizations = null;
    String user_name = "default";
    String linkedServer = null;
    Properties user = null;
    if (info != null) {
      SessionCrush the_session = (SessionCrush)info.get("ServerSessionObject");
      if (the_session == null)
        the_session = (SessionCrush)info.get("ServerSession"); 
      if (the_session != null) {
        date_time = the_session.date_time;
        web_customizations = (Vector)the_session.user.get("web_customizations");
        user_name = the_session.uiSG("user_name");
        linkedServer = the_session.server_item.getProperty("linkedServer");
        user = the_session.user;
      } 
    } 
    if (user_name.equals("default") && (share_key.split(":")).length > 3)
      user_name = share_key.split(":")[3]; 
    SimpleDateFormat ex1 = new SimpleDateFormat("MM/dd/yyyy HH:mm");
    Calendar cal = new GregorianCalendar();
    cal.setTime(new Date());
    cal.add(5, Integer.parseInt(share_key.split(":")[2]));
    Properties request = new Properties();
    request.put("expire", ex1.format(cal.getTime()));
    String share_paths = "";
    for (int x = 0; x < itemsFound.size(); x++) {
      Properties item = itemsFound.elementAt(x);
      share_paths = String.valueOf(share_paths) + Common.url_encode(item.getProperty("the_file_path")) + "\r\n";
      item.put("privs", "(read)(view)(resume)(write)(delete)(slideshow)(rename)(makedir)(deletedir)(share)(inherited)");
    } 
    request.put("paths", share_paths);
    request.put("baseUrl", "");
    request.put("direct_link", "false");
    request.put("emailTo", "");
    request.put("emailCc", "");
    request.put("emailBcc", "");
    request.put("emailFrom", "");
    request.put("emailSubject", "");
    request.put("emailBody", "{web_link}");
    request.put("sendEmail", "false");
    request.put("shareUsername", "false");
    request.put("allowUploads", "false");
    request.put("attach", "false");
    if (share_key.split(":")[1].equals("reference")) {
      request.put("publishType", "reference");
    } else if (share_key.split(":")[1].equals("move")) {
      request.put("publishType", "move");
    } else if (share_key.split(":")[1].equals("copy")) {
      request.put("publishType", "copy");
    } 
    if ((share_key.split(":")).length > 4 && share_key.split(":")[4].equalsIgnoreCase("full"))
      request.put("allowUploads", "true"); 
    String response = ServerSessionAJAX.createShare(itemsFound, request, web_customizations, user_name, linkedServer, user, date_time, null);
    String share_msg = Common.url_decode(response.substring(response.indexOf("<message>") + 9, response.indexOf("</message>")));
    String share_url = Common.url_decode(response.substring(response.indexOf("<url>") + 5, response.indexOf("</url>")));
    if (share_msg.indexOf("{share_complete}") < 0)
      share_url = ""; 
    body = Common.replace_str(body, "{" + original_share_key + "}", share_url);
    body = Common.replace_str(body, "{" + original_share_key + "_message}", share_msg);
    body = Common.replace_str(body, "{" + original_share_key + ":message}", share_msg);
    for (int i = 0; i < itemsFound.size(); i++) {
      Properties item = itemsFound.elementAt(i);
      item.putAll((Properties)request.clone());
    } 
    return body;
  }
  
  public static String convertFormEmail(String the_line, Vector lastUploadStats) {
    Properties formEmail = Common.buildFormEmail(ServerStatus.server_settings, lastUploadStats);
    return Common.replaceFormVariables(formEmail, the_line);
  }
  
  public static String convertSessionVars(String the_line, Properties info) {
    SessionCrush the_session = null;
    if (info != null) {
      the_session = (SessionCrush)info.get("ServerSessionObject");
      if (the_session == null)
        the_session = (SessionCrush)info.get("ServerSession"); 
    } 
    if (the_session != null)
      the_line = ServerStatus.thisObj.change_vars_to_values(the_line, the_session); 
    return the_line;
  }
  
  public static String convertCustomData(String the_line, String r1, String r2) {
    while (the_line.indexOf(String.valueOf(r1) + "customData_") >= 0) {
      String custom = the_line.substring(the_line.indexOf(String.valueOf(r1) + "customData_") + (String.valueOf(r1) + "customData_").length());
      custom = custom.substring(0, custom.indexOf(r2));
      Properties customData = (Properties)ServerStatus.server_settings.get("customData");
      String val = customData.getProperty(custom, "");
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "customData_" + custom + r2, val);
    } 
    return the_line;
  }
  
  public static String convertUserManager(String the_line, String r1, String r2) {
    while (the_line.indexOf(String.valueOf(r1) + "group_start") >= 0 && the_line.indexOf(String.valueOf(r1) + "group_end" + r2) >= 0) {
      String params = the_line.substring(the_line.indexOf(String.valueOf(r1) + "group_start") + (String.valueOf(r1) + "group_start").length());
      params = params.substring(0, params.indexOf("}"));
      String result_type = "user_name:MainUsers";
      if (params.indexOf(":") >= 0)
        result_type = params.substring(params.indexOf(":") + 1).trim(); 
      String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "group_start" + params + r2) + (String.valueOf(r1) + "group_start" + params + r2).length(), the_line.indexOf(String.valueOf(r1) + "group_end" + r2));
      String r = "";
      Properties groups = UserTools.getGroups(result_type.split(":")[1]);
      if (groups != null) {
        Vector users = (Vector)groups.get(inner.trim());
        if (users != null) {
          String separator = result_type.split(":")[2].trim();
          for (int xx = 0; xx < users.size(); xx++) {
            if (result_type.split(":")[0].equals("user_name")) {
              r = String.valueOf(r) + users.elementAt(xx).toString();
            } else {
              Properties u = UserTools.ut.getUser(result_type.split(":")[1], users.elementAt(xx).toString(), true);
              if (u == null || 
                Integer.parseInt(u.getProperty("max_logins", "0")) < 0)
                continue; 
              if (!u.getProperty(result_type.split(":")[0], "").equals(""))
                r = String.valueOf(r) + u.getProperty(result_type.split(":")[0], ""); 
            } 
            r = String.valueOf(r) + separator;
            continue;
          } 
          if (r.endsWith(separator))
            r = r.substring(0, r.length() - 1); 
        } 
      } 
      the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "group_start" + params + r2))) + r + the_line.substring(the_line.indexOf(String.valueOf(r1) + "group_end" + r2) + (String.valueOf(r1) + "group_end" + r2).length());
    } 
    while (the_line.indexOf(String.valueOf(r1) + "reload_start") >= 0 && the_line.indexOf(String.valueOf(r1) + "reload_end" + r2) >= 0) {
      String reload_user = the_line.substring(the_line.indexOf(String.valueOf(r1) + "reload_start" + r2) + (String.valueOf(r1) + "reload_start" + r2).length(), the_line.indexOf(String.valueOf(r1) + "reload_end" + r2));
      UserTools.ut.forceMemoryReloadUser(reload_user);
      the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "reload_start" + r2))) + the_line.substring(the_line.indexOf(String.valueOf(r1) + "reload_end" + r2) + (String.valueOf(r1) + "reload_end" + r2).length());
    } 
    return the_line;
  }
  
  public static String convertServerInfo(String the_line, String r1, String r2, Properties info, String original_the_line) {
    Enumeration keys = ServerStatus.thisObj.server_info.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      String val = ServerStatus.thisObj.server_info.getProperty(key);
      if (the_line.indexOf(String.valueOf(r1) + "server_" + key + r2) >= 0) {
        boolean was_equal = original_the_line.equals(the_line);
        the_line = Common.replace_str(the_line, String.valueOf(r1) + "server_" + key + r2, (new StringBuffer(String.valueOf(val))).toString());
        if (was_equal && the_line.indexOf(r1) < 0)
          info.put("last_scope", "static"); 
      } 
    } 
    return the_line;
  }
  
  public static String convertPreview(Properties item) {
    try {
      return Common.dots(String.valueOf(SearchHandler.getPreviewPath(item, "1", 1)) + "../index.txt");
    } catch (Exception e) {
      Common.log("SERVER", 1, e);
      return (String)e;
    } 
  }
  
  public static String sendEmail(String to, String cc, String bcc, String from, String reply_to, String subject, String body, File_B[] files) {
    return sendEmail(to, cc, bcc, from, reply_to, subject, body, files, new Vector());
  }
  
  public static String sendEmail(String to, String cc, String bcc, String from, String reply_to, String subject, String body, File_B[] files, Vector fileMimeTypes) {
    return Common.send_mail(ServerStatus.SG("discovered_ip"), to, cc, bcc, from, reply_to, subject, body, ServerStatus.SG("smtp_server"), ServerStatus.SG("smtp_user"), ServerStatus.SG("smtp_pass"), ServerStatus.BG("smtp_ssl"), ServerStatus.BG("smtp_html"), files, fileMimeTypes);
  }
  
  public static void runPluginMessageAlert(String msg, Properties info) {
    ServerStatus.thisObj.runAlerts(msg, info, null, null);
  }
}
