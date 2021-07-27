package crushftp.server;

import com.crushftp.client.Common;
import crushftp.gui.LOC;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.SharedSessionReplicated;
import crushftp.handlers.UserTools;
import crushftp.reports5.ReportTools;
import crushftp.server.daemon.DMZServer;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class AdminControls {
  public static Object runInstanceAction(Properties request) throws Exception {
    request.remove("instance");
    if (request.getProperty("command").equals("setServerItem"))
      return setServerItem(request); 
    if (request.getProperty("command").equals("getUser"))
      return getUser(request, false, null, null); 
    if (request.getProperty("command").equals("getPublicKeys"))
      return getPublicKeys(request); 
    if (request.getProperty("command").equals("setUserItem"))
      return setUserItem(request, null); 
    if (request.getProperty("command").equals("getUserList"))
      return getUserList(request, false, null, null); 
    if (request.getProperty("command").equals("getUserXML"))
      return getUserXML(request, false, null); 
    if (request.getProperty("command").equals("getUserXMLListing"))
      return getUserXMLListing(request, false, null, null); 
    if (request.getProperty("command").equals("getAdminXMLListing"))
      return getAdminXMLListing(request, null); 
    if (request.getProperty("command").equals("getLog"))
      return getLog(request); 
    if (request.getProperty("command").equals("adminAction"))
      return adminAction(request); 
    if (request.getProperty("command").equals("updateNow"))
      return updateNow(request); 
    if (request.getProperty("command").equals("updateWebNow"))
      return updateWebNow(request); 
    if (request.getProperty("command").equals("updateNowProgress"))
      return updateNowProgress(request); 
    if (request.getProperty("command").equals("cancelUpdateProgress"))
      return cancelUpdateProgress(request); 
    if (request.getProperty("command").equals("pgpGenerateKeyPair"))
      return pgpGenerateKeyPair(request); 
    if (request.getProperty("command").equals("runReport"))
      return runReport(request); 
    if (request.getProperty("command").equals("testReportSchedule"))
      return testReportSchedule(request); 
    if (request.getProperty("command").equals("testJobSchedule"))
      return testJobSchedule(request); 
    if (request.getProperty("command").equals("testSMTP"))
      return testSMTP(request); 
    if (request.getProperty("command").equals("sendEventEmail"))
      return sendEventEmail(request); 
    if (request.getProperty("command").equals("migrateUsersVFS"))
      return migrateUsersVFS(request); 
    if (request.getProperty("command").equals("convertUsers"))
      return convertUsers(request); 
    if (request.getProperty("command").equals("generateSSL"))
      return generateSSL(request); 
    if (request.getProperty("command").equals("testKeystore"))
      return testKeystore(request); 
    if (request.getProperty("command").equals("generateFileKey"))
      return generateFileKey(request); 
    if (request.getProperty("command").equals("testDB"))
      return testDB(request); 
    if (request.getProperty("command").equals("pluginMethodCall"))
      return pluginMethodCall(request); 
    if (request.getProperty("command").equals("convertXMLSQLUsers"))
      return convertXMLSQLUsers(request); 
    if (request.getProperty("command").equals("registerCrushFTP"))
      return registerCrushFTP(request); 
    if (request.getProperty("command").equals("importUsers"))
      return importUsers(request); 
    if (request.getProperty("command").equals("sendPassEmail"))
      return sendPassEmail(request); 
    if (request.getProperty("command").equals("getTempAccounts"))
      return getTempAccounts(request); 
    if (request.getProperty("command").equals("addTempAccount"))
      return addTempAccount(request); 
    if (request.getProperty("command").equals("removeTempAccount"))
      return removeTempAccount(request); 
    if (request.getProperty("command").equals("getTempAccountFiles"))
      return getTempAccountFiles(request); 
    if (request.getProperty("command").equals("removeTempAccountFile"))
      return removeTempAccountFile(request); 
    if (request.getProperty("command").equals("addTempAccountFile"))
      return addTempAccountFile(request); 
    return "";
  }
  
  public static Object getServerItem(String admin_group_name, Properties request, boolean limitedAdmin) {
    try {
      String[] keys = request.getProperty("key").split("/");
      String last_key = "";
      Object o = null;
      try {
        for (int x = 0; x < keys.length; x++) {
          String key = keys[x];
          last_key = key;
          if (key.equals("server_settings")) {
            if (request.getProperty("instance", "").equals("")) {
              o = ServerStatus.server_settings;
            } else {
              String id = Common.makeBoundary();
              DMZServer.sendCommand(request.getProperty("instance", ""), new Properties(), "GET:SERVER_SETTINGS", id);
              Properties p = DMZServer.getResponse(id, 10);
              o = p.get("data");
            } 
            if (request.getProperty("defaults", "false").equals("true"))
              o = ServerStatus.thisObj.default_settings; 
            if (limitedAdmin) {
              o = ServerStatus.thisObj.default_settings.clone();
              ((Properties)o).put("CustomForms", stripUnrelatedAdminItems("CustomForms", admin_group_name));
              ((Properties)o).put("tunnels", stripUnrelatedAdminItems("tunnels", admin_group_name));
              ((Properties)o).put("email_templates", stripUnrelatedAdminItems("email_templates", admin_group_name));
              ((Properties)o).put("random_password_length", ServerStatus.SG("random_password_length"));
              ((Properties)o).put("min_password_length", ServerStatus.SG("min_password_length"));
              ((Properties)o).put("min_password_numbers", ServerStatus.SG("min_password_numbers"));
              ((Properties)o).put("min_password_lowers", ServerStatus.SG("min_password_lowers"));
              ((Properties)o).put("min_password_uppers", ServerStatus.SG("min_password_uppers"));
              ((Properties)o).put("min_password_specials", ServerStatus.SG("min_password_specials"));
              ((Properties)o).put("password_history_count", ServerStatus.SG("password_history_count"));
              ((Properties)o).put("unsafe_password_chars", ServerStatus.SG("unsafe_password_chars"));
              ((Properties)o).put("blank_passwords", ServerStatus.SG("blank_passwords"));
              ((Properties)o).put("user_default_folder_privs", ServerStatus.SG("user_default_folder_privs"));
            } 
          } else if (key.equals("server_info")) {
            if (request.getProperty("instance", "").equals("")) {
              o = ServerStatus.thisObj.server_info.clone();
            } else {
              o = null;
              String id = Common.makeBoundary();
              DMZServer.sendCommand(request.getProperty("instance", ""), request, "GET:SERVER_INFO", id);
              Properties p = DMZServer.getResponse(id, 10);
              o = p.get("data");
            } 
            if (limitedAdmin)
              o = new Properties(); 
          } else if (o instanceof Properties) {
            o = ((Properties)o).get(key);
          } else if (o instanceof Vector) {
            o = ((Vector)o).elementAt(Integer.parseInt(key));
          } 
        } 
      } catch (Exception e) {
        return "FAILURE:" + e.toString();
      } 
      if (last_key.equals("user_list") || last_key.equals("recent_user_list"))
        o = stripUserList(o); 
      if (last_key.equals("server_info")) {
        ((Properties)o).remove("plugins");
        ((Properties)o).remove("running_tasks");
        ((Properties)o).remove("user_list");
        ((Properties)o).remove("recent_user_list");
      } 
      if (o instanceof Properties)
        o = stripUser(o); 
      return o;
    } catch (Exception e) {
      return e.toString();
    } 
  }
  
  public static Vector stripUnrelatedAdminItems(String key, String admin_group_name) {
    Vector v = (Vector)ServerStatus.VG(key).clone();
    for (int xx = v.size() - 1; xx >= 0; xx--) {
      Properties pp = v.elementAt(xx);
      if (!pp.getProperty("name", "").toUpperCase().startsWith(admin_group_name.toUpperCase()))
        v.removeElementAt(xx); 
    } 
    if (v.size() == 0)
      v = ServerStatus.VG(key); 
    return v;
  }
  
  public static String getStatHistory(Properties request) throws Exception {
    StringBuffer xml = new StringBuffer();
    String[] params = request.getProperty("params").split(",");
    for (int x = 0; x < params.length; x++) {
      String param = params[x].trim();
      Properties si = null;
      if (request.getProperty("instance", "").equals("")) {
        si = ServerStatus.thisObj.server_info;
      } else {
        String id = Common.makeBoundary();
        DMZServer.sendCommand(request.getProperty("instance", ""), request, "GET:SERVER_INFO", id);
        Properties p = DMZServer.getResponse(id, 10);
        si = (Properties)p.get("data");
      } 
      Vector v = (Vector)si.get(String.valueOf(param) + "_history");
      if (v == null) {
        v = new Vector();
        v.addElement(si.getProperty(param));
      } 
      int loc = v.size() - 1;
      int intervals = Integer.parseInt(request.getProperty("priorIntervals", "1"));
      xml.append("<" + param + ">");
      while (intervals > 0 && loc >= 0) {
        xml.append(v.elementAt(loc).toString()).append((intervals > 1 || loc > 0) ? "," : "");
        intervals--;
        loc--;
      } 
      xml.append("</" + param + ">");
    } 
    String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
    response = String.valueOf(response) + "<result><response_data>" + xml + "</response_data></result>";
    return response;
  }
  
  public static Vector getJobsSummary(Properties request) throws Exception {
    if (ServerStatus.siIG("enterprise_level") <= 0)
      return new Vector(); 
    Properties si = null;
    if (request.getProperty("instance", "").equals("")) {
      si = ServerStatus.thisObj.server_info;
    } else {
      String id = Common.makeBoundary();
      DMZServer.sendCommand(request.getProperty("instance", ""), request, "GET:SERVER_INFO", id);
      Properties p = DMZServer.getResponse(id, 10);
      si = (Properties)p.get("data");
    } 
    Vector jobs = (Vector)si.get("running_tasks");
    Vector vv = new Vector();
    for (int x = 0; x < jobs.size(); x++) {
      Properties job = jobs.elementAt(x);
      Properties summaryJob = new Properties();
      summaryJob.put("name", "");
      summaryJob.put("start", job.getProperty("start", ""));
      summaryJob.put("id", job.getProperty("id", ""));
      summaryJob.put("log_file", job.getProperty("log_file", ""));
      summaryJob.put("stop", job.getProperty("stop", ""));
      summaryJob.put("status", job.getProperty("status", ""));
      Properties settings = (Properties)job.get("settings");
      if (settings == null)
        settings = new Properties(); 
      Properties set2 = new Properties();
      set2.put("pluginName", settings.getProperty("pluginName", ""));
      set2.put("subItem", settings.getProperty("subItem", ""));
      set2.put("scheduleName", settings.getProperty("scheduleName", ""));
      set2.put("name", settings.getProperty("name", ""));
      set2.put("id", settings.getProperty("id", ""));
      summaryJob.put("settings", set2);
      vv.addElement(summaryJob);
    } 
    return vv;
  }
  
  public static Vector getJobInfo(Properties request) throws Exception {
    if (ServerStatus.siIG("enterprise_level") <= 0)
      return new Vector(); 
    Properties si = null;
    if (request.getProperty("instance", "").equals("")) {
      si = ServerStatus.thisObj.server_info;
    } else {
      String id = Common.makeBoundary();
      DMZServer.sendCommand(request.getProperty("instance", ""), request, "GET:SERVER_INFO", id);
      Properties p = DMZServer.getResponse(id, 10);
      si = (Properties)p.get("data");
    } 
    Vector jobs = (Vector)si.get("running_tasks");
    Vector vv = new Vector();
    for (int x = 0; x < jobs.size(); x++) {
      Properties job = jobs.elementAt(x);
      if (job.getProperty("id").equals(request.getProperty("job_id")))
        vv.addElement(job); 
    } 
    return vv;
  }
  
  public static Vector getSessionList(Properties request) throws Exception {
    Properties si = null;
    if (request.getProperty("instance", "").equals("")) {
      si = ServerStatus.thisObj.server_info;
    } else {
      String id = Common.makeBoundary();
      DMZServer.sendCommand(request.getProperty("instance", ""), request, "GET:SERVER_INFO", id);
      Properties p = DMZServer.getResponse(id, 10);
      si = (Properties)p.get("data");
    } 
    Vector v = (Vector)((Vector)si.get(request.getProperty("session_list"))).clone();
    Vector vv = new Vector();
    for (int x = 0; x < v.size(); x++) {
      Properties user_info = v.elementAt(x);
      Properties p = new Properties();
      p.put("user_name", user_info.getProperty("user_name"));
      p.put("user_number", user_info.getProperty("user_number"));
      p.put("user_ip", user_info.getProperty("user_ip"));
      p.put("user_protocol", user_info.getProperty("user_protocol"));
      p.put("current_dir", user_info.getProperty("current_dir"));
      vv.addElement(p);
    } 
    return vv;
  }
  
  public static String setServerItem(Properties request) {
    String status = "OK";
    try {
      if (!request.getProperty("instance", "").equals("")) {
        String id = Common.makeBoundary();
        String instance = request.remove("instance").toString();
        DMZServer.sendCommand(instance, request, "RUN:INSTANCE_ACTION", id);
        Properties p = DMZServer.getResponse(id, 10);
        if (request.getProperty("key").indexOf("server_settings/") >= 0) {
          String id2 = Common.makeBoundary();
          DMZServer.sendCommand(instance, new Properties(), "GET:SERVER_SETTINGS", id2);
          Properties pp = DMZServer.getResponse(id2, 10);
          SharedSessionReplicated.send("", "WRITE_PREFS", instance, pp.get("data"));
          Thread.sleep(200L);
          Common.write_server_settings((Properties)pp.get("data"), instance);
        } 
        return p.get("data").toString();
      } 
      String[] keys = request.getProperty("key").split("/");
      Object o = null;
      boolean restartServers = false;
      try {
        for (int x = 0; x < keys.length - 1; x++) {
          String key = keys[x];
          if (key.equals("server_settings")) {
            o = ServerStatus.server_settings;
          } else if (key.equals("server_info")) {
            o = ServerStatus.thisObj.server_info;
          } else if (o instanceof Properties) {
            o = ((Properties)o).get(key);
          } else if (o instanceof Vector) {
            o = ((Vector)o).elementAt(Integer.parseInt(key));
          } 
        } 
        String lastKey = keys[keys.length - 1];
        String secondLastKey = keys[keys.length - 2];
        Object new_o = null;
        if (!request.getProperty("data_type").equals("text")) {
          new_o = Common.readXMLObject(new ByteArrayInputStream(Common.url_decode(request.getProperty("data").replace('+', ' ')).getBytes("UTF8")));
        } else {
          new_o = Common.url_decode(request.getProperty("data", "").replace('+', ' '));
        } 
        if (o instanceof Properties) {
          if (lastKey.equals("server_prefs") && (new_o instanceof Properties || new_o instanceof Vector)) {
            Common.updateObject(new_o, o);
          } else if (new_o instanceof Properties || new_o instanceof Vector) {
            Common.updateObject(new_o, ((Properties)o).get(lastKey));
          } else {
            ((Properties)o).put(lastKey, new_o.toString());
          } 
        } else if (o instanceof Vector) {
          Vector v = (Vector)o;
          if (request.getProperty("data_action", "").equals("reset")) {
            if (request.getProperty("key").indexOf("/plugins/") >= 0) {
              if (new_o == null)
                new_o = new Vector(); 
              Common.updateObject(new_o, v);
            } else {
              v.removeAllElements();
              if (new_o == null)
                new_o = new Vector(); 
              v.addAll((Vector)new_o);
            } 
          } else {
            int i = Integer.parseInt(lastKey);
            if (request.getProperty("data_action", "").equals("remove")) {
              v.remove(i);
            } else if (i > v.size() - 1) {
              v.addElement(new_o);
              if (v == (Vector)ServerStatus.server_settings.get("server_list"))
                ServerStatus.thisObj.start_this_server(i); 
            } else if (new_o instanceof Properties || new_o instanceof Vector) {
              Common.updateObject(new_o, v.elementAt(i));
            } else {
              v.setElementAt(new_o.toString(), i);
            } 
          } 
          if (request.getProperty("key").indexOf("/plugins/") >= 0)
            ServerStatus.thisObj.common_code.loadPluginsSync(ServerStatus.server_settings, ServerStatus.thisObj.server_info); 
          if (secondLastKey.equals("server_list"))
            restartServers = true; 
        } 
      } catch (Exception e) {
        status = "FAILURE:" + e.toString();
      } 
      if (restartServers) {
        ServerStatus.thisObj.stop_all_servers();
        ServerStatus.thisObj.start_all_servers();
      } 
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      Log.log("HTTP_SERVER", 1, request.getProperty("key"));
      status = e.toString();
    } 
    ServerStatus.thisObj.save_server_settings(false);
    return status;
  }
  
  public static Object getUser(Properties request, boolean limitedAdmin, ServerSession thisSession, Properties session) {
    if (limitedAdmin)
      request.put("serverGroup", thisSession.server_item.getProperty("linkedServer")); 
    String status = "OK";
    String username = Common.url_decode(request.getProperty("username").replace('+', ' '));
    try {
      if (!request.getProperty("instance", "").equals("")) {
        String id = Common.makeBoundary();
        String instance = request.remove("instance").toString();
        DMZServer.sendCommand(instance, request, "RUN:INSTANCE_ACTION", id);
        Properties p = DMZServer.getResponse(id, 10);
        return p.get("data");
      } 
      VFS uVFS = UserTools.ut.getVFS(request.getProperty("serverGroup"), username);
      Properties new_user = UserTools.ut.getUser(request.getProperty("serverGroup"), username, false);
      if (new_user == null || !new_user.getProperty("username", "not found").equalsIgnoreCase(username))
        throw new Exception("User not found:" + username); 
      if (limitedAdmin) {
        Vector list = new Vector();
        Properties info = (Properties)session.get("user_admin_info");
        list = (Vector)info.get("list");
        if (!username.equals(thisSession.SG("admin_group_name"))) {
          if (new_user != null && !username.equals("default") && list.indexOf(username) < 0)
            return new Properties(); 
          if (list.indexOf(username) < 0 && !username.equals(thisSession.SG("admin_group_name")))
            throw new Exception("Username " + username + " not found."); 
        } 
      } 
      UserTools.getExtraVFS(request.getProperty("serverGroup"), username, null, new_user);
      Vector vfs_items = new Vector();
      Properties virtual = uVFS.homes.elementAt(0);
      Enumeration keys = virtual.keys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement().toString();
        if (key.equals("vfs_permissions_object"))
          continue; 
        Properties p = (Properties)virtual.get(key);
        String virtualPath = p.getProperty("virtualPath");
        if (virtualPath.equals(""))
          continue; 
        Properties dir_item = new Properties();
        if (p.getProperty("type").equalsIgnoreCase("DIR")) {
          if (!virtualPath.endsWith("/"))
            virtualPath = String.valueOf(virtualPath) + "/"; 
          dir_item.put("url", "");
          dir_item.put("type", "DIR");
        } else {
          Vector vItems = (Vector)p.get("vItems");
          dir_item = (Properties)((Properties)vItems.elementAt(0)).clone();
        } 
        dir_item.put("path", Common.all_but_last(virtualPath));
        dir_item.put("name", p.getProperty("name"));
        Vector wrapper = new Vector();
        wrapper.addElement(dir_item);
        vfs_items.addElement(wrapper);
      } 
      String pass = new_user.getProperty("password", "");
      if (!pass.startsWith("SHA:") && !pass.startsWith("SHA512:") && !pass.startsWith("MD5:") && !pass.startsWith("CRYPT3:") && !pass.startsWith("BCRYPT:") && !pass.startsWith("MD5CRYPT:")) {
        pass = ServerStatus.thisObj.common_code.decode_pass(pass);
        new_user.put("password", pass);
      } 
      if (!new_user.getProperty("userVersion", "").equals("6") && !new_user.getProperty("as2EncryptKeystorePassword", "").equals("")) {
        new_user.put("as2EncryptKeystorePassword", ServerStatus.thisObj.common_code.encode_pass(new_user.getProperty("as2EncryptKeystorePassword", ""), "DES"));
        new_user.put("as2EncryptKeyPassword", ServerStatus.thisObj.common_code.encode_pass(new_user.getProperty("as2EncryptKeyPassword", ""), "DES"));
      } 
      Properties user_items = new Properties();
      user_items.put("user", new_user);
      if (uVFS.permissions == null)
        uVFS.permissions = new Vector(); 
      if (uVFS.permissions.size() == 0)
        uVFS.permissions.addElement(new Properties()); 
      user_items.put("permissions", uVFS.permissions.elementAt(0));
      user_items.put("vfs_items", vfs_items);
      if (new_user.containsKey("web_buttons")) {
        Vector buttons = (Vector)new_user.get("web_buttons");
        ServerSessionAJAX5_2.fixButtons(buttons);
      } 
      return user_items;
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      status = e.toString();
      return status;
    } 
  }
  
  public static Object getPublicKeys(Properties request) {
    return "";
  }
  
  public static String setUserItem(Properties request, ServerSession thisSession) {
    String status = "OK";
    try {
      String s = handleInstance(request);
      if (s != null)
        return s; 
      try {
        if (request.getProperty("xmlItem", "").equals("groups")) {
          Properties groups = (Properties)Common.readXMLObjectError(new ByteArrayInputStream(Common.url_decode(request.getProperty("groups").replace('+', ' ')).getBytes("UTF8")));
          if (groups == null)
            groups = new Properties(); 
          UserTools.writeGroups(request.getProperty("serverGroup"), groups);
        } else if (request.getProperty("xmlItem", "").equals("inheritance")) {
          Properties inheritance = (Properties)Common.readXMLObjectError(new ByteArrayInputStream(Common.url_decode(request.getProperty("inheritance").replace('+', ' ')).getBytes("UTF8")));
          if (inheritance == null)
            inheritance = new Properties(); 
          UserTools.writeInheritance(request.getProperty("serverGroup"), inheritance);
        } else if (request.getProperty("xmlItem", "").equals("user")) {
          if (!request.containsKey("usernames"))
            request.put("usernames", request.getProperty("username", "")); 
          String[] usernames = Common.url_decode(request.getProperty("usernames").replace('+', ' ')).split(";");
          for (int x = 0; x < usernames.length; x++) {
            String username = usernames[x].trim();
            if (!username.equals("")) {
              if (request.getProperty("data_action").equals("delete")) {
                if (request.getProperty("expire_user", "false").equals("true")) {
                  Properties user = UserTools.ut.getUser(request.getProperty("serverGroup"), username, false);
                  if (user != null)
                    UserTools.expireUserVFSTask(user, request.getProperty("serverGroup"), username); 
                } 
                UserTools.deleteUser(request.getProperty("serverGroup"), username);
              } else {
                Properties new_user = new Properties();
                if (!request.getProperty("user", "").equals(""))
                  new_user = (Properties)Common.readXMLObject(new ByteArrayInputStream(Common.url_decode(request.getProperty("user").replace('+', ' ')).getBytes("UTF8"))); 
                new_user.put("userVersion", "6");
                if (new_user.containsKey("password")) {
                  String pass = new_user.getProperty("password", "");
                  if (!pass.startsWith("SHA:") && !pass.startsWith("SHA512:") && !pass.startsWith("MD5:") && !pass.startsWith("CRYPT3:") && !pass.startsWith("BCRYPT:") && !pass.startsWith("MD5CRYPT:")) {
                    pass = ServerStatus.thisObj.common_code.encode_pass(pass, ServerStatus.SG("password_encryption"));
                    new_user.put("password", pass);
                  } else {
                    new_user.put("password", pass);
                  } 
                } 
                if (request.getProperty("data_action").equals("update")) {
                  Properties user = UserTools.ut.getUser(request.getProperty("serverGroup"), username, false);
                  Common.updateObject(new_user, user);
                  new_user = user;
                } 
                if (request.getProperty("data_action").equals("update_remove")) {
                  Properties user = UserTools.ut.getUser(request.getProperty("serverGroup"), username, false);
                  String[] keys = request.getProperty("update_remove_key", "").split(";");
                  for (int xx = 0; xx < keys.length; xx++)
                    user.remove(keys[xx]); 
                  new_user = user;
                } 
                UserTools.writeUser(request.getProperty("serverGroup"), username, new_user);
                UserTools.ut.getUser(request.getProperty("serverGroup"), username, true);
                if (request.containsKey("vfs_items") || request.containsKey("permissions"))
                  UserTools.writeVFS(request.getProperty("serverGroup"), username, processVFSSubmission(request, username, false, thisSession)); 
              } 
              UserTools.ut.forceMemoryReload(username);
            } 
          } 
        } else {
          status = "Unknown xmlItem:" + request.getProperty("xmlitem");
        } 
      } catch (Exception e) {
        Log.log("HTTP_SERVER", 1, e);
        status = "FAILURE:" + e.toString();
      } 
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      status = e.toString();
    } 
    return status;
  }
  
  public static Properties getUserList(Properties request, boolean limitedAdmin, ServerSession thisSession, Properties session) {
    if (!request.getProperty("instance", "").equals("")) {
      String id = Common.makeBoundary();
      String instance = request.remove("instance").toString();
      DMZServer.sendCommand(instance, request, "RUN:INSTANCE_ACTION", id);
      try {
        Properties p = DMZServer.getResponse(id, 10);
        return (Properties)p.get("data");
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
        return null;
      } 
    } 
    if (limitedAdmin)
      request.put("serverGroup", thisSession.server_item.getProperty("linkedServer")); 
    Vector list = new Vector();
    UserTools.refreshUserList(request.getProperty("serverGroup"), list);
    if (limitedAdmin) {
      String groupName = thisSession.SG("admin_group_name");
      if (groupName.equals("") || groupName.equals("admin_group_name"))
        groupName = thisSession.uiSG("user_name"); 
      Properties info = UserTools.getAllowedUsers(groupName, request.getProperty("serverGroup"), list);
      Properties info2 = UserTools.getAllowedUsers("pendingSelfRegistration", request.getProperty("serverGroup"), list);
      list = (Vector)info.get("list");
      Vector list2 = (Vector)info2.get("list");
      for (int x = 0; x < list2.size(); x++) {
        String tempUsername = list2.elementAt(x).toString();
        if (list.indexOf(tempUsername) < 0)
          list.addElement(tempUsername); 
      } 
      session.put("user_admin_info", info);
    } 
    Properties user_list = new Properties();
    user_list.put("user_list", list);
    return user_list;
  }
  
  static Object getUserXML(Properties request, boolean limitedAdmin, Properties session) {
    try {
      if (!request.getProperty("instance", "").equals("")) {
        String id = Common.makeBoundary();
        String instance = request.remove("instance").toString();
        DMZServer.sendCommand(instance, request, "RUN:INSTANCE_ACTION", id);
        Properties p = DMZServer.getResponse(id, 10);
        return p.get("data");
      } 
      Properties obj = null;
      if (request.getProperty("xmlItem", "").equals("group")) {
        if (!limitedAdmin) {
          obj = UserTools.getGroups(request.getProperty("serverGroup"));
        } else {
          obj = new Properties();
        } 
      } else if (request.getProperty("xmlItem", "").equals("inheritance")) {
        if (!limitedAdmin) {
          obj = UserTools.getInheritance(request.getProperty("serverGroup"));
        } else {
          Properties info = (Properties)session.get("user_admin_info");
          obj = (Properties)info.get("inheritance");
        } 
      } 
      Properties result = new Properties();
      result.put("result_item", obj);
      return result;
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      return e.toString();
    } 
  }
  
  public static Properties getUserXMLListing(Properties request, boolean limitedAdmin, ServerSession thisSession, Properties session) throws Exception {
    if (!request.getProperty("instance", "").equals("")) {
      String id = Common.makeBoundary();
      String instance = request.remove("instance").toString();
      DMZServer.sendCommand(instance, request, "RUN:INSTANCE_ACTION", id);
      Properties p = DMZServer.getResponse(id, 10);
      return (Properties)p.get("data");
    } 
    if (limitedAdmin)
      request.put("serverGroup", thisSession.server_item.getProperty("linkedServer")); 
    String username = Common.url_decode(request.getProperty("username", "").replace('+', ' '));
    String parentUser = null;
    if (limitedAdmin) {
      Properties info = (Properties)session.get("user_admin_info");
      Vector list = (Vector)info.get("list");
      if (list.indexOf(username) < 0)
        throw new Exception("Username " + username + " not found."); 
      String groupName = thisSession.SG("admin_group_name");
      if (groupName.equals("") || groupName.equals("admin_group_name"))
        groupName = thisSession.uiSG("user_name"); 
      parentUser = groupName;
    } 
    String path = Common.url_decode(request.getProperty("path", "").replace('+', ' '));
    if (!path.startsWith("/"))
      path = "/" + path; 
    if (!path.endsWith("/"))
      path = String.valueOf(path) + "/"; 
    path = Common.dots(path);
    VFS tempVFS = processVFSSubmission(request, username, limitedAdmin, thisSession);
    Vector listing = UserTools.ut.get_virtual_list_fake(tempVFS, path, request.getProperty("serverGroup"), parentUser);
    return getListingInfo(listing, path);
  }
  
  public static Properties getAdminXMLListing(Properties request, ServerSession thisSession) throws Exception {
    if (!request.getProperty("instance", "").equals("")) {
      String id = Common.makeBoundary();
      String instance = request.remove("instance").toString();
      DMZServer.sendCommand(instance, request, "RUN:INSTANCE_ACTION", id);
      Properties p = DMZServer.getResponse(id, 10);
      return (Properties)p.get("data");
    } 
    String path = Common.url_decode(request.getProperty("path", "").replace('+', ' '));
    if (path.startsWith("///") && !path.startsWith("////"))
      path = "/" + path; 
    if (!path.startsWith("/"))
      path = "/" + path; 
    if (!path.endsWith("/"))
      path = String.valueOf(path) + "/"; 
    if (!(new File(path)).exists() && Common.machine_is_x())
      path = "/Volumes" + path; 
    path = Common.dots(path);
    File[] items = (File[])null;
    if (path.equals("/") && Common.machine_is_x()) {
      try {
        File[] other_volumes = (new File("/Volumes/")).listFiles();
        if (other_volumes == null) {
          other_volumes = new File[1];
          other_volumes[0] = new File("/");
        } 
        items = new File[other_volumes.length];
        for (int i = 0; i < other_volumes.length; i++)
          items[i] = other_volumes[i]; 
      } catch (Exception exception) {}
    } else if (path.equals("/") && Common.machine_is_windows()) {
      items = File.listRoots();
    } else {
      items = (new File(path)).listFiles();
    } 
    Vector listing = new Vector();
    for (int x = 0; x < items.length; x++) {
      Properties p = new Properties();
      p.put("name", items[x].getName());
      p.put("path", Common.all_but_last(items[x].getPath()));
      p.put("type", items[x].isDirectory() ? "DIR" : "FILE");
      p.put("size", (new StringBuffer(String.valueOf(items[x].length()))).toString());
      if (Common.machine_is_windows() && path.equals("/")) {
        p.put("name", items[x].getPath().substring(0, 2));
        p.put("path", "/");
      } 
      p.put("privs", "(read)(view)");
      p.put("owner", "user");
      p.put("group", "group");
      p.put("permissionsNum", "777");
      p.put("keywords", "");
      p.put("num_items", "1");
      p.put("preview", "");
      p.put("owner", "");
      p.put("owner", "");
      p.put("root_dir", p.getProperty("path"));
      p.put("url", (new StringBuffer(String.valueOf(items[x].toURI().toURL().toExternalForm()))).toString());
      listing.addElement(p);
    } 
    return getListingInfo(listing, path);
  }
  
  public static Properties getLog(Properties request) throws IOException {
    if (!request.getProperty("instance", "").equals("")) {
      String id = Common.makeBoundary();
      String instance = request.remove("instance").toString();
      DMZServer.sendCommand(instance, request, "RUN:INSTANCE_ACTION", id);
      try {
        Properties p = DMZServer.getResponse(id, 10);
        return (Properties)p.get("data");
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
        return null;
      } 
    } 
    return ServerStatus.thisObj.loggingProvider.getLogSegment(Long.parseLong(request.getProperty("segment_start", "0")), Long.parseLong(request.getProperty("segment_len", "32768")), request.getProperty("log_file", ""));
  }
  
  static String buildXML(Object o, String key, String status) {
    String xml = "";
    if (o instanceof String) {
      status = o.toString();
      o = null;
    } 
    try {
      if (o != null) {
        xml = ServerStatus.thisObj.common_code.getXMLString(o, key, null);
        if (xml.startsWith("<?"))
          xml = xml.substring(xml.indexOf("?>") + 2).trim(); 
      } 
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
    String response_type = "";
    if (o == null || o instanceof String)
      response_type = "text"; 
    if (o instanceof Properties) {
      response_type = "properties";
    } else if (o instanceof Vector) {
      response_type = "vector";
    } 
    String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
    response = String.valueOf(response) + "<result><response_status>" + status + "</response_status><response_type>" + response_type + "</response_type><response_data>" + xml + "</response_data></result>";
    return response;
  }
  
  public static Object adminAction(Properties request) {
    String status = "OK";
    try {
      if (!request.getProperty("instance", "").equals("")) {
        String id = Common.makeBoundary();
        String instance = request.remove("instance").toString();
        DMZServer.sendCommand(instance, request, "RUN:INSTANCE_ACTION", id);
        Properties p = DMZServer.getResponse(id, 10);
        return p.get("data");
      } 
      Vector indexes = new Vector();
      String[] indexesStr = request.getProperty("index", "").split(",");
      for (int x = 0; x < indexesStr.length; x++)
        indexes.addElement(indexesStr[x].trim()); 
      Vector v = new Vector();
      v.addAll(ServerStatus.siVG("user_list"));
      if (request.getProperty("action", "").equals("kick")) {
        for (int i = v.size() - 1; i >= 0; i--) {
          Properties user_info = v.elementAt(i);
          for (int xx = 0; xx < indexes.size(); xx++) {
            if (user_info.getProperty("user_number").equals(indexes.elementAt(xx).toString())) {
              status = String.valueOf(status) + ServerStatus.thisObj.kick(user_info, true) + "\r\n";
              indexes.remove(xx);
              break;
            } 
          } 
        } 
      } else if (request.getProperty("action", "").equals("ban") || request.getProperty("action", "").equals("temporaryBan")) {
        v.addAll(ServerStatus.siVG("recent_user_list"));
        for (int i = v.size() - 1; i >= 0; i--) {
          Properties user_info = v.elementAt(i);
          for (int xx = 0; xx < indexes.size(); xx++) {
            if (user_info.getProperty("user_number").equals(indexes.elementAt(xx).toString())) {
              if (request.getProperty("action", "").equals("ban")) {
                status = String.valueOf(status) + ServerStatus.thisObj.ban(user_info, 0) + ",";
              } else if (request.getProperty("action", "").equals("temporaryBan")) {
                status = String.valueOf(status) + ServerStatus.thisObj.ban(user_info, Integer.parseInt(request.getProperty("banTimeout"))) + ",";
              } 
              status = String.valueOf(status) + ServerStatus.thisObj.kick(user_info, true) + ",";
              indexes.remove(xx);
              break;
            } 
          } 
        } 
      } else if (request.getProperty("action", "").equals("getUserInfo")) {
        v.addAll(ServerStatus.siVG("recent_user_list"));
        for (int i = v.size() - 1; i >= 0; i--) {
          Properties user_info = v.elementAt(i);
          for (int xx = 0; xx < indexes.size(); xx++) {
            if (user_info.getProperty("user_number").equals(indexes.elementAt(xx).toString())) {
              Properties user_info2 = (Properties)user_info.clone();
              return stripUser(user_info2);
            } 
          } 
        } 
      } else if (request.getProperty("action", "").equals("startAllServers")) {
        ServerStatus.thisObj.start_all_servers();
      } else if (request.getProperty("action", "").equals("stopAllServers")) {
        ServerStatus.thisObj.stop_all_servers();
        Thread.sleep(1000L);
        ServerStatus.thisObj.stop_all_servers();
      } else if (request.getProperty("action", "").equals("startServer")) {
        ServerStatus.thisObj.start_this_server(Integer.parseInt(request.getProperty("index")));
      } else if (request.getProperty("action", "").equals("stopServer")) {
        ServerStatus.thisObj.stop_this_server(Integer.parseInt(request.getProperty("index")));
      } else if (request.getProperty("action", "").equals("restartServer")) {
        ServerStatus.thisObj.stop_this_server(Integer.parseInt(request.getProperty("index")));
        Thread.sleep(1000L);
        ServerStatus.thisObj.start_this_server(Integer.parseInt(request.getProperty("index")));
      } else if (request.getProperty("action", "").equals("allStats")) {
        ServerStatus.thisObj.reset_server_login_counts();
        ServerStatus.thisObj.reset_server_bytes_in_out();
        ServerStatus.thisObj.reset_upload_download_counter();
      } else if (request.getProperty("action", "").equals("loginStats")) {
        ServerStatus.thisObj.reset_server_login_counts();
      } else if (request.getProperty("action", "").equals("uploadDownloadStats")) {
        ServerStatus.thisObj.reset_upload_download_counter();
      } else if (request.getProperty("action", "").equals("transferStats")) {
        ServerStatus.thisObj.reset_server_bytes_in_out();
      } else if (!request.getProperty("action", "").equals("serverStats")) {
        if (request.getProperty("action", "").equals("newFolder")) {
          if (Common.machine_is_x() && !(new File(request.getProperty("path"))).exists())
            request.put("path", "/Volumes" + request.getProperty("path")); 
          if (!(new File(String.valueOf(request.getProperty("path")) + request.getProperty("name"))).mkdir())
            throw new Exception("New Folder Failed!"); 
          Common.updateOSXInfo(String.valueOf(request.getProperty("path")) + request.getProperty("name"));
        } else if (request.getProperty("action", "").equals("renameItem")) {
          if (Common.machine_is_x() && !(new File(request.getProperty("path"))).exists())
            request.put("path", "/Volumes" + request.getProperty("path")); 
          if (!(new File(String.valueOf(request.getProperty("path")) + request.getProperty("name"))).renameTo(new File(String.valueOf(request.getProperty("path")) + request.getProperty("newName"))))
            throw new Exception("Rename Failed!"); 
        } else if (request.getProperty("action", "").equals("duplicateItem")) {
          if (Common.machine_is_x() && !(new File(request.getProperty("path"))).exists())
            request.put("path", "/Volumes" + request.getProperty("path")); 
          Vector list = new Vector();
          Common.getAllFileListing(list, (new File(String.valueOf(request.getProperty("path")) + request.getProperty("name"))).getCanonicalPath(), 5, true);
          if (list.size() > 100)
            throw new Exception("Too many items to allow duplicate!"); 
          Common.recurseCopy((new File(String.valueOf(request.getProperty("path")) + request.getProperty("name"))).getCanonicalPath(), String.valueOf((new File(String.valueOf(request.getProperty("path")) + request.getProperty("name"))).getCanonicalPath()) + "_tmp_" + Common.makeBoundary(), false);
        } else if (request.getProperty("action", "").equals("deleteItem")) {
          if (Common.machine_is_x() && !(new File(request.getProperty("path"))).exists())
            request.put("path", "/Volumes" + request.getProperty("path")); 
          if (!(new File(String.valueOf(request.getProperty("path")) + request.getProperty("name"))).delete())
            throw new Exception("Delete Failed. (Folders must be empty to be deleted.)"); 
        } 
      } 
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      status = e.toString();
    } 
    return status;
  }
  
  public static String updateNow(Properties request) {
    String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
    String xml = "";
    try {
      String s = handleInstance(request);
      if (s != null)
        return s; 
      ServerStatus.thisObj.do_auto_update_early(false);
      xml = "Success";
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      xml = e.toString();
    } 
    response = String.valueOf(response) + "<result><response>" + xml + "</response></result>";
    return response;
  }
  
  public static String updateWebNow(Properties request) {
    String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
    String xml = "";
    try {
      String s = handleInstance(request);
      if (s != null)
        return s; 
      ServerStatus.thisObj.do_auto_update_early(true);
      xml = "Success";
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      xml = e.toString();
    } 
    response = String.valueOf(response) + "<result><response>" + xml + "</response></result>";
    return response;
  }
  
  public static String updateNowProgress(Properties request) {
    String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
    response = String.valueOf(response) + "<result><response>";
    try {
      String s = handleInstance(request);
      if (s != null)
        return s; 
      response = String.valueOf(response) + "<currentLoc>" + ServerStatus.thisObj.updateHandler.updateCurrentLoc + "</currentLoc>";
      response = String.valueOf(response) + "<maximumLoc>" + ServerStatus.thisObj.updateHandler.updateMaxSize + "</maximumLoc>";
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      response = String.valueOf(response) + e.toString();
    } 
    response = String.valueOf(response) + "</response></result>";
    return response;
  }
  
  public static String cancelUpdateProgress(Properties request) {
    String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
    response = String.valueOf(response) + "<result><response>";
    try {
      String s = handleInstance(request);
      if (s != null)
        return s; 
      ServerStatus.thisObj.updateHandler.cancel();
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      response = String.valueOf(response) + e.toString();
    } 
    response = String.valueOf(response) + "</response></result>";
    return response;
  }
  
  public static String pgpGenerateKeyPair(Properties request) {
    String xml = "";
    try {
      String s = handleInstance(request);
      if (s != null)
        return s; 
      Common.generateKeyPair(Common.url_decode(request.getProperty("pgpPivateKeyPathGenerate").replace('+', ' ')), Integer.parseInt(request.getProperty("pgpKeySizeGenerate")), Integer.parseInt(request.getProperty("pgpKeyDaysGenerate")), Common.url_decode(request.getProperty("pgpPrivateKeyPasswordGenerate")), Common.url_decode(request.getProperty("pgpCommonNameGenerate").replace('+', ' ')));
      xml = "Success";
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      xml = e.toString();
    } 
    String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
    response = String.valueOf(response) + "<result><response>" + xml + "</response></result>";
    return response;
  }
  
  public static String runReport(Properties request) {
    String xml = "";
    try {
      String s = handleInstance(request);
      if (s != null)
        return s; 
      Object reportItem = ServerStatus.thisObj.rt.getReportItem(request.getProperty("reportName"), ServerStatus.server_settings, ServerStatus.thisObj.server_info);
      Properties params = request;
      Vector v = new Vector();
      if (request.containsKey("usernames")) {
        String[] usernames = request.getProperty("usernames").split(",");
        for (int x = 0; x < usernames.length; x++) {
          s = usernames[x].trim();
          if (!s.equals(""))
            v.addElement(s); 
        } 
        request.put("usernames", v);
      } 
      request.put("usernames", v);
      params.put("server_info", ServerStatus.thisObj.server_info);
      params.put("server_settings", ServerStatus.server_settings);
      xml = ServerStatus.thisObj.rt.writeReport("", "", new Properties(), params, reportItem, ServerStatus.server_settings, ServerStatus.thisObj.server_info, request.getProperty("export", "false").equals("true"), request.getProperty("reportName"), request);
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      xml = "<html><body><h1>" + e.toString() + "</h1></body></html>";
    } 
    return xml;
  }
  
  public static String handleInstance(Properties request) throws Exception {
    if (!request.getProperty("instance", "").equals("")) {
      String id = Common.makeBoundary();
      String instance = request.remove("instance").toString();
      DMZServer.sendCommand(instance, request, "RUN:INSTANCE_ACTION", id);
      Properties p = DMZServer.getResponse(id, 10);
      return p.get("data").toString();
    } 
    return null;
  }
  
  public static String testReportSchedule(Properties request) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request);
      if (s != null)
        return s; 
      Vector schedules = ServerStatus.VG("reportSchedules");
      Properties params = schedules.elementAt(Integer.parseInt(request.getProperty("scheduleIndex")));
      Properties config = (Properties)params.get("config");
      config = (Properties)config.clone();
      config.put("server_settings", ServerStatus.server_settings);
      config.put("server_info", ServerStatus.thisObj.server_info);
      Properties status = new Properties();
      String dir = params.getProperty("reportFolder");
      if (!dir.endsWith("/"))
        dir = String.valueOf(dir) + "/"; 
      String filename = params.getProperty("reportFilename");
      filename = ServerStatus.thisObj.rt.replaceVars(filename, params, config);
      if (params.getProperty("reportOverwrite").equals("false") && (new File(String.valueOf(dir) + filename)).exists()) {
        response = String.valueOf(response) + "The report file already exists.";
      } else {
        if (config.get("usernames") == null)
          config.put("usernames", new Vector()); 
        ReportTools.skipEmail(config.getProperty("reportName"), config);
        ServerStatus.thisObj.rt.writeReport(filename, dir, status, config, ServerStatus.thisObj.rt.getReportItem(config.getProperty("reportName"), ServerStatus.server_settings, ServerStatus.thisObj.server_info), ServerStatus.server_settings, ServerStatus.thisObj.server_info, config.getProperty("export", "").equals("true"), config.getProperty("reportName"), params);
        ReportTools.unSkipEmail(config.getProperty("reportName"), config);
        response = String.valueOf(response) + LOC.G("Report written to") + ":" + dir + filename;
      } 
      response = String.valueOf(response) + "</response></commandResult>";
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
    return response;
  }
  
  public static Vector runnignSchedules = new Vector();
  
  public static String testJobSchedule(Properties request) {
    if (ServerStatus.siIG("enterprise_level") <= 0)
      return "ERROR:Enterprise License only feature."; 
    String response = "";
    try {
      String s = handleInstance(request);
      if (s != null)
        return s; 
      Vector schedules = ServerStatus.VG("schedules");
      Properties params = (Properties)((Properties)schedules.elementAt(Integer.parseInt(request.getProperty("scheduleIndex")))).clone();
      Worker.startWorker(new Runnable(params) {
            private final Properties val$params;
            
            public void run() {
              Properties event = new Properties();
              event.putAll(this.val$params);
              event.put("event_plugin_list", this.val$params.getProperty("plugin"));
              event.put("name", "ScheduledPluginEvent:" + this.val$params.getProperty("scheduleName"));
              if (AdminControls.runnignSchedules.indexOf(this.val$params.getProperty("scheduleName")) < 0)
                try {
                  AdminControls.runnignSchedules.addElement(this.val$params.getProperty("scheduleName"));
                  ServerStatus.thisObj.events6.doEventPlugin(event, null, new Vector());
                } finally {
                  AdminControls.runnignSchedules.remove(this.val$params.getProperty("scheduleName"));
                }  
            }
          });
      if (runnignSchedules.indexOf(params.getProperty("scheduleName")) < 0) {
        response = String.valueOf(response) + "Job Started";
      } else {
        response = String.valueOf(response) + "Job is already running, stop existing job first.";
      } 
      Log.log("SERVER", 1, "Job Schedule:" + params.getProperty("scheduleName") + ":" + response);
      response = "<commandResult><response>" + response + "</response></commandResult>";
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
    return response;
  }
  
  public static String testSMTP(Properties request) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request);
      if (s != null)
        return s; 
      Properties p = request;
      String results = Common.send_mail(ServerStatus.SG("discovered_ip"), p.getProperty("to", ""), p.getProperty("cc", ""), p.getProperty("bcc", ""), p.getProperty("from", ""), p.getProperty("subject", ""), p.getProperty("body", ""), p.getProperty("server", ""), p.getProperty("user", ""), p.getProperty("pass", ""), p.getProperty("ssl", "").equals("true"), p.getProperty("html", "").equals("true"), null);
      response = String.valueOf(response) + Common.url_encode(results);
      response = String.valueOf(response) + "</response></commandResult>";
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
    return response;
  }
  
  public static String sendEventEmail(Properties request) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request);
      if (s != null)
        return s; 
      String from = Common.replace_str(request.getProperty("email_from"), "%user_email%", request.getProperty("user_email"));
      String to = Common.replace_str(request.getProperty("email_to"), "%user_email%", request.getProperty("user_email"));
      String cc = Common.replace_str(request.getProperty("email_cc"), "%user_email%", request.getProperty("user_email"));
      String bcc = Common.replace_str(request.getProperty("email_bcc"), "%user_email%", request.getProperty("user_email"));
      if ((String.valueOf(to.trim()) + cc.trim() + bcc.trim()).equals("")) {
        response = String.valueOf(response) + LOC.G("Test failed.  You have no recipients.") + "\r\n";
      } else if (from.trim().equals("")) {
        response = String.valueOf(response) + LOC.G("Test failed.  You have no from address.") + "\r\n";
      } else {
        Properties email_info = new Properties();
        email_info.put("server", ServerStatus.SG("smtp_server"));
        email_info.put("user", ServerStatus.SG("smtp_user"));
        email_info.put("pass", ServerStatus.SG("smtp_pass"));
        email_info.put("ssl", ServerStatus.SG("smtp_ssl"));
        email_info.put("html", ServerStatus.SG("smtp_html"));
        email_info.put("from", from);
        email_info.put("to", to);
        email_info.put("cc", cc);
        email_info.put("bcc", bcc);
        email_info.put("subject", request.getProperty("email_subject"));
        email_info.put("body", request.getProperty("email_body"));
        ServerStatus.thisObj.sendEmail(email_info);
        response = String.valueOf(response) + email_info.getProperty("results", "") + "\r\n";
      } 
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString()) + "\r\n";
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String migrateUsersVFS(Properties request) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request);
      if (s != null)
        return s; 
      response = String.valueOf(response) + ServerStatus.thisObj.common_code.migrateUsersVFS(request.getProperty("serverGroup"), request.getProperty("findPath"), request.getProperty("replacePath"));
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString()) + "\r\n";
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String convertUsers(Properties request) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request);
      if (s != null)
        return s; 
      Vector users = new Vector();
      String[] usersStr = request.getProperty("users").split(";");
      for (int x = 0; x < usersStr.length; x++) {
        if (!usersStr[x].trim().equals(""))
          users.addElement(usersStr[x].trim()); 
      } 
      response = String.valueOf(response) + UserTools.convertUsers(request.getProperty("allUsers").equalsIgnoreCase("true"), users, request.getProperty("serverGroup"), request.getProperty("username", ""));
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString()) + "\r\n";
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String generateFileKey(Properties request) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request);
      if (s != null)
        return s; 
      String path = request.getProperty("keyPath");
      if (path.trim().equals("") || !(new File(path)).exists() || !(new File(path)).isDirectory()) {
        response = String.valueOf(response) + "The path specified was not found.";
      } else {
        if (!path.startsWith("/"))
          path = "/" + path; 
        if (!path.endsWith("/"))
          path = String.valueOf(path) + "/"; 
        if ((new File(String.valueOf(path) + "CrushFTP.key")).exists()) {
          response = String.valueOf(response) + "Key file already exist.  CrushFTP will not overwrite an existing key file.";
        } else {
          try {
            Common.buildPrivateKeyFile(path);
            response = String.valueOf(response) + "Success.  CrushFTP.key secret key file generated.\r\n\r\n" + path;
          } catch (Exception ee) {
            Log.log("HTTP_SERVER", 0, ee);
            response = String.valueOf(response) + "Error:" + ee.getMessage();
          } 
        } 
      } 
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString());
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String testDB(Properties request) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request);
      if (s != null)
        return s; 
      Connection conn = null;
      String[] db_drv_files = request.getProperty("db_driver_file").split(";");
      URL[] urls = new URL[db_drv_files.length];
      for (int x = 0; x < db_drv_files.length; x++)
        urls[x] = (new File(db_drv_files[x])).toURI().toURL(); 
      ClassLoader cl = new URLClassLoader(urls);
      Class drvCls = Class.forName(request.getProperty("db_driver"), true, cl);
      Driver driver = (Driver)drvCls.newInstance();
      Properties props = new Properties();
      props.setProperty("user", request.getProperty("db_user"));
      props.setProperty("password", ServerStatus.thisObj.common_code.decode_pass(request.getProperty("db_pass")));
      conn = driver.connect(request.getProperty("db_url"), props);
      conn.close();
      response = String.valueOf(response) + "Success";
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString());
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String pluginMethodCall(Properties request) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request);
      if (s != null)
        return s; 
      Object parent = Common.getPlugin(request.getProperty("pluginName"), null, request.getProperty("pluginSubItem", ""));
      if (parent == null && request.getProperty("pluginSubItem", "").equals(""))
        parent = Common.getPlugin(request.getProperty("pluginName"), null, "false"); 
      Method method = parent.getClass().getMethod(request.getProperty("method", "testSettings"), new Class[] { (new Properties()).getClass() });
      response = String.valueOf(response) + method.invoke(parent, new Object[] { request }).toString();
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString());
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String convertXMLSQLUsers(Properties request) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request);
      if (s != null)
        return s; 
      UserTools.convertUsersSQLXML(request.getProperty("fromMode"), request.getProperty("toMode"), request.getProperty("serverGroup"));
      response = String.valueOf(response) + "Success.";
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString());
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String registerCrushFTP(Properties request) {
    String response = "<commandResult><response>";
    try {
      String instance = request.getProperty("instance", "");
      String s = handleInstance(request);
      if (s != null) {
        String id2 = Common.makeBoundary();
        DMZServer.sendCommand(instance, new Properties(), "GET:SERVER_SETTINGS", id2);
        Properties p = DMZServer.getResponse(id2, 10);
        Common.write_server_settings((Properties)p.get("data"), instance);
        return s;
      } 
      String registration_name = Common.url_encode_all(request.getProperty("registration_name").toUpperCase().trim());
      String registration_email = request.getProperty("registration_email").toUpperCase().trim();
      String registration_code = request.getProperty("registration_code").trim();
      if (ServerStatus.thisObj.common_code.register(registration_name, registration_email, registration_code)) {
        String v = ServerStatus.thisObj.common_code.getRegistrationAccess("V", registration_code);
        if (v != null && (v.equals("4") || v.equals("5"))) {
          response = String.valueOf(response) + "CrushFTP " + v + " needs an upgrade license for CrushFTP 6.  http://www.crushftp.com/pricing.html";
        } else {
          ServerStatus.put_in("registration_name", registration_name);
          ServerStatus.put_in("registration_email", registration_email);
          ServerStatus.put_in("registration_code", registration_code);
          ServerStatus.server_settings.put("max_max_users", ServerStatus.thisObj.common_code.getRegistrationAccess("MAX", registration_code));
          ServerStatus.server_settings.put("max_users", ServerStatus.thisObj.common_code.getRegistrationAccess("MAX", registration_code));
          response = String.valueOf(response) + "Registration Information Accepted";
        } 
        ServerStatus.thisObj.prefsProvider.check_code();
        ServerStatus.thisObj.save_server_settings(false);
      } else {
        response = String.valueOf(response) + LOC.G("Invalid Name, Email or Code!");
      } 
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString());
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String importUsers(Properties request) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request);
      if (s != null)
        return s; 
      String the_dir = request.getProperty("the_dir");
      if (request.getProperty("user_type").equals("Import CrushFTP3 Users...")) {
        ServerStatus.thisObj.common_code.ConvertCrushFTP3Users(the_dir, String.valueOf(request.getProperty("serverGroup")) + "/", ServerStatus.SG("password_encryption"), "");
      } else if (request.getProperty("user_type").equals("Import Folders As Users...")) {
        ServerStatus.thisObj.common_code.ConvertFolderUsers(the_dir, String.valueOf(request.getProperty("serverGroup")) + "/");
      } else if (request.getProperty("user_type").equals("Import Mac OS X Users...")) {
        ServerStatus.thisObj.common_code.ConvertOSXUsers(String.valueOf(request.getProperty("serverGroup")) + "/");
      } else if (request.getProperty("user_type").equals("Import Serv-U Users...")) {
        ServerStatus.thisObj.common_code.ConvertServUUsers(the_dir, String.valueOf(request.getProperty("serverGroup")) + "/");
      } else if (request.getProperty("user_type").equals("Import BulletProof Users...")) {
        ServerStatus.thisObj.common_code.ConvertBPFTPsers(the_dir, String.valueOf(request.getProperty("serverGroup")) + "/");
      } else if (request.getProperty("user_type").equals("Import Rumpus Users...")) {
        ServerStatus.thisObj.common_code.ConvertRumpusUsers(the_dir, String.valueOf(request.getProperty("serverGroup")) + "/");
      } else if (request.getProperty("user_type").equals("Import ProFTPD Users...")) {
        ServerStatus.thisObj.common_code.ConvertPasswdUsers(the_dir, String.valueOf(request.getProperty("serverGroup")) + "/", "CRYPT3:");
      } else if (request.getProperty("user_type").equals("Import VSFTPD Users...")) {
        ServerStatus.thisObj.common_code.ConvertPasswdUsers(the_dir, String.valueOf(request.getProperty("serverGroup")) + "/", "MD5CRYPT:");
      } else if (request.getProperty("user_type").equals("Import ProFTPD Groups...")) {
        ServerStatus.thisObj.common_code.ConvertProFTPDGroups(request.getProperty("serverGroup"), the_dir, String.valueOf(request.getProperty("serverGroup")) + "/");
      } else if (request.getProperty("user_type").equals("Import Tab Delimited Text...")) {
        ServerStatus.thisObj.common_code.convertTabDelimited(the_dir, String.valueOf(request.getProperty("serverGroup")) + "/");
      } else if (request.getProperty("user_type").equals("Import FileZilla Users...")) {
        ServerStatus.thisObj.common_code.convertFilezilla(the_dir, String.valueOf(request.getProperty("serverGroup")) + "/");
      } else {
        throw new Exception("User import type not supported:" + request.getProperty("user_type"));
      } 
      response = String.valueOf(response) + "SUCCESS: Users imported.";
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 0, e);
      response = String.valueOf(response) + e.toString();
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String sendPassEmail(Properties request) {
    Vector email_templates = ServerStatus.VG("email_templates");
    String subject = ServerStatus.SG("emailReminderSubjectText");
    String body = ServerStatus.SG("emailReminderBodyText");
    String from = ServerStatus.SG("smtp_from");
    String cc = "";
    String bcc = "";
    String templateName = Common.url_decode(request.getProperty("email_template", "").replace('+', ' ')).trim();
    Common.debug(2, "Looking up template info for admin pass email:" + templateName);
    for (int x = 0; x < email_templates.size(); x++) {
      Properties email_template = email_templates.elementAt(x);
      if (email_template.getProperty("name", "").equals(templateName)) {
        Common.debug(2, "Found template:" + email_template);
        subject = email_template.getProperty("emailSubject", "");
        body = email_template.getProperty("emailBody", "");
        from = email_template.getProperty("emailFrom", from);
        cc = email_template.getProperty("emailCC", "");
        bcc = email_template.getProperty("emailBCC", "");
      } 
    } 
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request);
      if (s != null)
        return s; 
      String to = request.getProperty("user_email");
      if (from.equals(""))
        from = to; 
      if (ServerStatus.SG("smtp_server").equals("")) {
        response = String.valueOf(response) + LOC.G("This server is not configured to send email password reminders.") + "\r\n";
      } else if (!to.equals("")) {
        Properties userTemp = UserTools.ut.getUser(request.getProperty("serverGroup"), request.getProperty("user_name"), false);
        body = Common.replace_str(body, "%user_name%", request.getProperty("user_name"));
        body = Common.replace_str(body, "%username%", request.getProperty("user_name"));
        body = Common.replace_str(body, "%user_pass%", request.getProperty("user_pass"));
        body = Common.replace_str(body, "%user_password%", request.getProperty("user_pass"));
        body = Common.replace_str(body, "%user_email%", request.getProperty("user_email"));
        body = Common.replace_str(body, "%user_first_name%", request.getProperty("user_first_name"));
        body = Common.replace_str(body, "%user_last_name%", request.getProperty("user_last_name"));
        body = Common.replace_str(body, "{user_name}", request.getProperty("user_name"));
        body = Common.replace_str(body, "{username}", request.getProperty("user_name"));
        body = Common.replace_str(body, "{user_pass}", request.getProperty("user_pass"));
        body = Common.replace_str(body, "{user_password}", request.getProperty("user_pass"));
        body = Common.replace_str(body, "{user_email}", request.getProperty("user_email"));
        body = Common.replace_str(body, "{user_first_name}", request.getProperty("user_first_name"));
        body = Common.replace_str(body, "{user_last_name}", request.getProperty("user_last_name"));
        body = ServerStatus.change_vars_to_values_static(body, userTemp, new Properties(), null);
        subject = Common.replace_str(subject, "%user_name%", request.getProperty("user_name"));
        subject = Common.replace_str(subject, "%username%", request.getProperty("user_name"));
        subject = Common.replace_str(subject, "%user_pass%", request.getProperty("user_pass"));
        subject = Common.replace_str(subject, "%user_password%", request.getProperty("user_pass"));
        subject = Common.replace_str(subject, "%user_email%", request.getProperty("user_email"));
        subject = Common.replace_str(subject, "%user_first_name%", request.getProperty("user_first_name"));
        subject = Common.replace_str(subject, "%user_last_name%", request.getProperty("user_last_name"));
        subject = Common.replace_str(subject, "{user_name}", request.getProperty("user_name"));
        subject = Common.replace_str(subject, "{username}", request.getProperty("user_name"));
        subject = Common.replace_str(subject, "{user_pass}", request.getProperty("user_pass"));
        subject = Common.replace_str(subject, "{user_password}", request.getProperty("user_pass"));
        subject = Common.replace_str(subject, "{user_email}", request.getProperty("user_email"));
        subject = Common.replace_str(subject, "{user_first_name}", request.getProperty("user_first_name"));
        subject = Common.replace_str(subject, "{user_last_name}", request.getProperty("user_last_name"));
        subject = ServerStatus.change_vars_to_values_static(subject, userTemp, new Properties(), null);
        Properties email_info = new Properties();
        email_info.put("server", ServerStatus.SG("smtp_server"));
        email_info.put("user", ServerStatus.SG("smtp_user"));
        email_info.put("pass", ServerStatus.SG("smtp_pass"));
        email_info.put("ssl", ServerStatus.SG("smtp_ssl"));
        email_info.put("html", ServerStatus.SG("smtp_html"));
        email_info.put("from", from);
        email_info.put("to", to);
        email_info.put("cc", cc);
        email_info.put("bcc", bcc);
        email_info.put("subject", subject);
        email_info.put("body", body);
        ServerStatus.thisObj.sendEmail(email_info);
        response = String.valueOf(response) + email_info.getProperty("results", "") + "\r\n";
        if (response.toUpperCase().indexOf("ERROR") < 0)
          response = "<commandResult><response>" + LOC.G("An email was just sent to the email address associated with this user.") + "\r\n"; 
        Log.log("HTTP_SERVER", 0, String.valueOf(LOC.G("Password Emailed to user:")) + request.getProperty("user_name") + "  " + to + "   " + LOC.G("Email Result:") + response);
      } 
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString()) + "\r\n";
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String testKeystore(Properties request) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request);
      if (s != null)
        return s; 
      if (!request.getProperty("keystorePath").equals("builtin")) {
        RandomAccessFile testIn = new RandomAccessFile(request.getProperty("keystorePath"), "r");
        if (testIn.length() == 0L)
          throw new IOException("Keystore file not found:" + request.getProperty("keystorePath")); 
        testIn.close();
      } 
      SSLServerSocket ss = (SSLServerSocket)ServerStatus.thisObj.common_code.getServerSocket(0, "127.0.0.1", request.getProperty("keystorePath"), request.getProperty("keystorePass"), request.getProperty("keyPass"), "", false, 10, false);
      SSLSocketFactory factory = (new Common()).getSSLContext("builtin", "builtin", "", "", "TLS", false, true).getSocketFactory();
      SSLSocket s1 = (SSLSocket)factory.createSocket(new Socket("127.0.0.1", ss.getLocalPort()), "127.0.0.1", ss.getLocalPort(), true);
      SSLSocket s2 = (SSLSocket)ss.accept();
      ss.close();
      s1.setSoTimeout(1000);
      s2.setSoTimeout(1000);
      s2.setUseClientMode(false);
      s1.setUseClientMode(true);
      (new Thread(new Runnable(s1) {
            private final SSLSocket val$s1;
            
            public void run() {
              try {
                this.val$s1.startHandshake();
              } catch (Exception exception) {}
            }
          })).start();
      (new Thread(new Runnable(s2) {
            private final SSLSocket val$s2;
            
            public void run() {
              try {
                this.val$s2.startHandshake();
              } catch (Exception exception) {}
            }
          })).start();
      s1.getOutputStream().write("1".getBytes());
      s2.getOutputStream().write("1".getBytes());
      s1.close();
      s2.close();
      response = String.valueOf(response) + "Cert test successful.";
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "Cert test failed:" + Common.url_encode(ee.toString());
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String generateSSL(Properties request) {
    String response = "<commandResult><response>";
    boolean error = false;
    String keystorepath = String.valueOf(request.getProperty("keystorepath")) + request.getProperty("alias") + ".jks";
    try {
      String s = handleInstance(request);
      if (s != null)
        return s; 
      Vector exec_cmd = new Vector();
      if (Common.machine_is_windows()) {
        exec_cmd.addElement(String.valueOf(System.getProperties().getProperty("sun.boot.library.path")) + "\\keytool");
      } else {
        exec_cmd.addElement("keytool");
      } 
      exec_cmd.addElement("-genkey");
      exec_cmd.addElement("-alias");
      exec_cmd.addElement(request.getProperty("alias"));
      exec_cmd.addElement("-keyalg");
      exec_cmd.addElement("RSA");
      exec_cmd.addElement("-validity");
      exec_cmd.addElement(request.getProperty("days"));
      exec_cmd.addElement("-keysize");
      exec_cmd.addElement(request.getProperty("bits"));
      exec_cmd.addElement("-storetype");
      exec_cmd.addElement("JKS");
      exec_cmd.addElement("-keystore");
      exec_cmd.addElement(keystorepath);
      String common_name = request.getProperty("common_name");
      String city_name = request.getProperty("city");
      String state_code = request.getProperty("state");
      String country_code = request.getProperty("country");
      String org_unit = request.getProperty("organizational_unit");
      String org = request.getProperty("organization");
      exec_cmd.addElement("-dname");
      exec_cmd.addElement("CN=" + common_name + ", OU=" + org_unit + ", O=" + org + ", L=" + city_name + ", ST=" + state_code + ", C=" + country_code);
      exec_cmd.addElement("-keypass");
      exec_cmd.addElement(request.getProperty("password1"));
      exec_cmd.addElement("-storepass");
      exec_cmd.addElement(request.getProperty("password1"));
      String pretty = "";
      String[] cmds = new String[exec_cmd.size()];
      for (int x = 0; x < exec_cmd.size(); x++) {
        cmds[x] = exec_cmd.elementAt(x).toString();
        pretty = String.valueOf(pretty) + cmds[x] + " ";
      } 
      pretty = Common.replace_str(pretty, request.getProperty("password1"), "***");
      response = String.valueOf(response) + "Executing: " + pretty + "\r\n";
      if (!request.getProperty("password1").equals(request.getProperty("password2")))
        throw new Exception("Verify passwords, mismatched."); 
      Process proc = Runtime.getRuntime().exec(cmds);
      BufferedReader br1 = new BufferedReader(new InputStreamReader(proc.getInputStream()));
      BufferedReader br2 = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
      String result = "";
      while ((result = br1.readLine()) != null)
        response = String.valueOf(response) + Common.url_encode(result) + "\r\n"; 
      while ((result = br2.readLine()) != null) {
        error = true;
        response = String.valueOf(response) + Common.url_encode(result) + "\r\n";
      } 
      proc.waitFor();
      if (proc.exitValue() != 0)
        error = true; 
      try {
        proc.destroy();
      } catch (Exception exception) {}
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString()) + "\r\n";
      error = true;
    } 
    if (error) {
      response = String.valueOf(response) + "\r\n" + Common.url_encode("ERROR:" + keystorepath + " failed to be generated.") + "\r\n";
    } else {
      response = String.valueOf(response) + "\r\n" + Common.url_encode(String.valueOf(keystorepath) + " has been generated.") + "\r\n";
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static Object stripUserList(Object o) {
    Vector v = (Vector)((Vector)o).clone();
    Vector vv = new Vector();
    for (int x = 0; x < v.size(); x++) {
      Object o2 = stripUser(v.elementAt(x));
      ((Properties)o2).remove("user_log");
      vv.addElement(o2);
    } 
    return vv;
  }
  
  public static Object stripUser(Object o) {
    Properties p2 = (Properties)((Properties)o).clone();
    p2.remove("stat");
    p2.remove("session");
    p2.remove("session_commands");
    p2.remove("session_uploads");
    p2.remove("session_downloads");
    p2.remove("failed_commands");
    p2.remove("lastUploadStats");
    p2.remove("current_password");
    p2.remove("post_parameters");
    return p2;
  }
  
  public static VFS processVFSSubmission(Properties request, String username, boolean limitedAdmin, ServerSession thisSession) throws Exception {
    Properties virtual = UserTools.generateEmptyVirtual();
    Properties permission0 = null;
    if (request.containsKey("permissions")) {
      permission0 = (Properties)Common.readXMLObjectError(new ByteArrayInputStream(Common.url_decode(Common.replace_str(request.getProperty("permissions").replace('+', ' '), "%26", "&amp;")).getBytes("UTF8")));
    } else {
      VFS tempVFS = UserTools.ut.getVFS(request.getProperty("serverGroup"), username);
      permission0 = tempVFS.getPermission0();
    } 
    Vector permissions = new Vector();
    permissions.addElement(permission0);
    virtual.put("vfs_permissions_object", permissions);
    if (request.containsKey("vfs_items")) {
      Vector vfs_items = (Vector)Common.readXMLObjectError(new ByteArrayInputStream(Common.url_decode(Common.replace_str(request.getProperty("vfs_items").replace('+', ' '), "%26", "&amp;")).getBytes("UTF8")));
      for (int x = 0; vfs_items != null && x < vfs_items.size(); x++) {
        Properties p = vfs_items.elementAt(x);
        Vector v = (Vector)p.get("vfs_item");
        if (!p.getProperty("name").equals("VFS") || !p.getProperty("path").equals("")) {
          Log.log("HTTP_SERVER", 2, (String)p);
          String path = Common.dots(String.valueOf(p.getProperty("path").substring(1)) + p.getProperty("name"));
          if (limitedAdmin) {
            String groupName = thisSession.SG("admin_group_name");
            if (groupName.equals("") || groupName.equals("admin_group_name"))
              groupName = thisSession.uiSG("user_name"); 
            Properties properties = new Properties();
            properties.put("virtualPath", "/" + path);
            properties.put("name", p.getProperty("name"));
            if (v.size() == 0 || (v.size() == 1 && ((Properties)v.elementAt(0)).getProperty("url", "").equals(""))) {
              properties.put("type", "DIR");
              virtual.put("/" + path, properties);
            } else if (UserTools.parentPathOK(request.getProperty("serverGroup"), groupName, ((Properties)v.elementAt(0)).getProperty("url", ""))) {
              properties.put("type", "FILE");
              properties.put("vItems", v);
              virtual.put("/" + path, properties);
            } else {
              Log.log("HTTP_SERVER", 0, new Date() + ":User " + thisSession.uiSG("user_name") + " Violated Security Constraint for a USER_ADMIN:" + groupName + ".  URL of VFS item not allowed:" + ((Properties)v.elementAt(0)).getProperty("url", ""));
            } 
          } else {
            Properties properties = new Properties();
            properties.put("virtualPath", "/" + path);
            virtual.put("/" + path, properties);
            properties.put("name", p.getProperty("name"));
            if (v.size() == 0 || (v.size() == 1 && ((Properties)v.elementAt(0)).getProperty("url", "").equals(""))) {
              properties.put("type", "DIR");
            } else {
              properties.put("type", "FILE");
              properties.put("vItems", v);
            } 
          } 
        } 
      } 
      Properties pp = new Properties();
      pp.put("type", "DIR");
      pp.put("virtualPath", "");
      pp.put("name", "VFS");
      virtual.put("/", pp);
    } else {
      VFS tempVFS = UserTools.ut.getVFS(request.getProperty("serverGroup"), username);
      Properties virtual2 = tempVFS.homes.elementAt(0);
      virtual2.remove("vfs_permissions_object");
      virtual.putAll(virtual2);
    } 
    return VFS.getVFS(virtual);
  }
  
  public static Properties getListingInfo(Vector listing, String the_dir) {
    Vector items = new Vector();
    try {
      for (int i = 0; i < listing.size(); i++) {
        Properties list_item = listing.elementAt(i);
        Log.log("HTTP_SERVER", 3, "Adding:" + list_item.getProperty("name"));
        list_item.put("preview", "0");
        list_item.put("sizeFormatted", Common.format_bytes(list_item.getProperty("size")));
        list_item.put("modified", list_item.getProperty("modified", "0"));
        list_item.remove("url");
        list_item.put("itemType", list_item.getProperty("type"));
        list_item.put("root_dir", list_item.getProperty("root_dir", "/"));
        items.addElement(list_item);
      } 
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
    Common.do_sort(items, "name");
    for (int x = 0; x < items.size(); x++) {
      Properties lp = items.elementAt(x);
      if (lp.getProperty("dir", "").indexOf("\"") >= 0)
        lp.put("dir", lp.getProperty("dir", "").replaceAll("\\\"", "%22")); 
      if (lp.getProperty("name", "").indexOf("\"") >= 0)
        lp.put("name", lp.getProperty("name", "").replaceAll("\\\"", "%22")); 
      if (lp.getProperty("name", "").endsWith(" ") || lp.getProperty("name", "").startsWith(" "))
        lp.put("name", lp.getProperty("name", "").replaceAll(" ", "%20")); 
      if (lp.getProperty("path", "").indexOf("\"") >= 0)
        lp.put("path", lp.getProperty("path", "").replaceAll("\\\"", "%22")); 
      if (lp.getProperty("root_dir", "").indexOf("\"") >= 0)
        lp.put("root_dir", lp.getProperty("root_dir", "").replaceAll("\\\"", "%22")); 
      String itemName = lp.getProperty("name");
      String itemPath = String.valueOf(the_dir) + lp.getProperty("name");
      String root_dir = lp.getProperty("root_dir");
      String href_path = String.valueOf(lp.getProperty("root_dir")) + lp.getProperty("name");
      if (href_path.startsWith("//") && !href_path.startsWith("////"))
        href_path = "//" + href_path; 
      lp.put("source", "/WebInterface/function/?command=getPreview&size=3&path=" + itemPath);
      lp.put("href_path", href_path);
      lp.put("root_dir", root_dir);
      lp.put("name", itemName);
    } 
    Properties listingProp = new Properties();
    listingProp.put("privs", "(read)(view)");
    listingProp.put("path", the_dir);
    listingProp.put("listing", items);
    return listingProp;
  }
  
  public static String getTempAccounts(Properties request) {
    try {
      String s = handleInstance(request);
      if (s != null)
        return s; 
      Vector items = new Vector();
      File[] accounts = (new File(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/")).listFiles();
      if (accounts != null)
        for (int x = 0; x < accounts.length; x++) {
          try {
            File f = accounts[x];
            if (f.getName().indexOf(",,") >= 0 && f.isDirectory()) {
              String[] tokens = f.getName().split(",,");
              Properties pp = new Properties();
              for (int xx = 0; xx < tokens.length; xx++) {
                String key = tokens[xx].substring(0, tokens[xx].indexOf("="));
                String val = tokens[xx].substring(tokens[xx].indexOf("=") + 1);
                pp.put(key.toUpperCase(), val);
              } 
              Properties info = (Properties)Common.readXMLObject(String.valueOf(f.getPath()) + "/INFO.XML");
              info.putAll(pp);
              Properties ppp = new Properties();
              ppp.put("info", info);
              ppp.put("tempaccount_user", info.get("U"));
              ppp.put("tempaccount_pass", info.get("P"));
              ppp.put("tempaccount_folder", f.getName());
              items.addElement(ppp);
            } 
          } catch (Exception e) {
            Log.log("SERVER", 1, e);
          } 
        }  
      return ServerStatus.thisObj.common_code.getXMLString(items, "temp_accounts", null);
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      return null;
    } 
  }
  
  public static String addTempAccount(Properties request) {
    request.put("tempaccount_user", Common.dots(request.getProperty("tempaccount_user")));
    request.put("tempaccount_pass", Common.dots(request.getProperty("tempaccount_pass")));
    request.put("tempaccount_folder", Common.dots(request.getProperty("tempaccount_folder")));
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request);
      if (s != null)
        return s; 
      (new File(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + request.getProperty("tempaccount_folder") + "/VFS/")).mkdirs();
      (new File(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "storage/" + request.getProperty("tempaccount_user") + request.getProperty("tempaccount_pass"))).mkdirs();
      Object permissions = Common.readXMLObject(new ByteArrayInputStream(Common.url_decode(request.getProperty("permissions").replace('+', ' ')).getBytes("UTF8")));
      Properties info = (Properties)Common.readXMLObject(new ByteArrayInputStream(Common.url_decode(request.getProperty("info").replace('+', ' ')).getBytes("UTF8")));
      ServerStatus.thisObj.common_code.writeXMLObject(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + request.getProperty("tempaccount_folder") + "/VFS.XML", permissions, "VFS");
      ServerStatus.thisObj.common_code.writeXMLObject(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + request.getProperty("tempaccount_folder") + "/INFO.XML", info, "INFO");
      String[] part_names = request.getProperty("tempaccount_folder").split(",,");
      Date td = (new SimpleDateFormat("MM/dd/yyyy hh:mm aa")).parse(info.getProperty("expire"));
      String fname = "";
      for (int x = 0; x < part_names.length; x++) {
        if (!part_names[x].startsWith("ex=")) {
          fname = String.valueOf(fname) + part_names[x];
        } else {
          fname = String.valueOf(fname) + "ex=" + (new SimpleDateFormat("MMddyyyyHHmm")).format(td);
        } 
        if (x < part_names.length)
          fname = String.valueOf(fname) + ",,"; 
      } 
      (new File(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + request.getProperty("tempaccount_folder"))).renameTo(new File(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + fname));
      response = String.valueOf(response) + "Success.";
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "ERROR:" + ee.toString();
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String removeTempAccount(Properties request) {
    request.put("tempaccount_user", Common.dots(request.getProperty("tempaccount_user")));
    request.put("tempaccount_pass", Common.dots(request.getProperty("tempaccount_pass")));
    request.put("tempaccount_folder", Common.dots(request.getProperty("tempaccount_folder")));
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request);
      if (s != null)
        return s; 
      Common.recurseDelete(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + request.getProperty("tempaccount_folder"), false);
      Common.recurseDelete(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "storage/" + request.getProperty("tempaccount_user") + request.getProperty("tempaccount_pass"), false);
      response = String.valueOf(response) + "Success.";
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "ERROR:" + ee.toString();
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String getTempAccountFiles(Properties request) {
    request.put("tempaccount_user", Common.dots(request.getProperty("tempaccount_user")));
    request.put("tempaccount_pass", Common.dots(request.getProperty("tempaccount_pass")));
    request.put("tempaccount_folder", Common.dots(request.getProperty("tempaccount_folder")));
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request);
      if (s != null)
        return s; 
      Properties p = new Properties();
      Vector containedFiles = new Vector();
      Vector refFiles = new Vector();
      p.put("containedFiles", containedFiles);
      p.put("refFiles", refFiles);
      File[] files = (new File(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + request.getProperty("tempaccount_folder") + "/VFS/")).listFiles();
      if (files != null)
        for (int x = 0; x < files.length; x++) {
          if ((new File(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "storage/" + request.getProperty("tempaccount_user") + request.getProperty("tempaccount_pass") + "/" + files[x].getName())).exists()) {
            containedFiles.addElement(files[x].getName());
          } else {
            refFiles.addElement(files[x].getName());
          } 
        }  
      return ServerStatus.thisObj.common_code.getXMLString(p, "temp_accounts_files", null);
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "ERROR:" + ee.toString();
      response = String.valueOf(response) + "</response></commandResult>";
      return response;
    } 
  }
  
  public static String removeTempAccountFile(Properties request) {
    request.put("tempaccount_user", Common.dots(request.getProperty("tempaccount_user")));
    request.put("tempaccount_pass", Common.dots(request.getProperty("tempaccount_pass")));
    request.put("tempaccount_file", Common.dots(request.getProperty("tempaccount_file")));
    request.put("tempaccount_folder", Common.dots(request.getProperty("tempaccount_folder")));
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request);
      if (s != null)
        return s; 
      Common.recurseDelete(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + request.getProperty("tempaccount_folder") + "/VFS/" + request.getProperty("tempaccount_file"), false);
      Common.recurseDelete(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "storage/" + request.getProperty("tempaccount_user") + request.getProperty("tempaccount_pass") + "/" + request.getProperty("tempaccount_file"), false);
      response = String.valueOf(response) + "Success.";
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "ERROR:" + ee.toString();
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String addTempAccountFile(Properties request) {
    request.put("tempaccount_user", Common.dots(request.getProperty("tempaccount_user")));
    request.put("tempaccount_pass", Common.dots(request.getProperty("tempaccount_pass")));
    request.put("tempaccount_file", Common.dots(request.getProperty("tempaccount_file")));
    request.put("tempaccount_folder", Common.dots(request.getProperty("tempaccount_folder")));
    String response = "<commandResult><response>";
    try {
      File fileItem = new File(request.getProperty("tempaccount_file"));
      if (!fileItem.exists()) {
        response = String.valueOf(response) + "ERROR:File does not exist.";
      } else {
        String userHome = String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + request.getProperty("tempaccount_folder") + "/";
        String userStorage = String.valueOf(ServerStatus.SG("temp_accounts_path")) + "storage/" + request.getProperty("tempaccount_user") + request.getProperty("tempaccount_pass") + "/";
        if (request.getProperty("tempaccount_reference", "false").equals("false"))
          Common.recurseCopyThreaded(fileItem.getPath(), String.valueOf(userStorage) + fileItem.getName() + (fileItem.isDirectory() ? "/" : ""), true, false); 
        Properties vItem = new Properties();
        if (request.getProperty("tempaccount_reference", "false").equals("false")) {
          vItem.put("url", (new File(String.valueOf(userStorage) + fileItem.getName())).toURI().toURL().toExternalForm());
        } else {
          vItem.put("url", fileItem.toURI().toURL().toExternalForm());
        } 
        vItem.put("type", fileItem.isDirectory() ? "dir" : "file");
        Vector v = new Vector();
        v.addElement(vItem);
        ServerStatus.thisObj.common_code.writeXMLObject(String.valueOf(userHome) + "VFS/" + fileItem.getName(), v, "VFS");
        if (request.getProperty("tempaccount_reference", "false").equals("false"))
          Thread.sleep(500L); 
        response = String.valueOf(response) + "Success.";
      } 
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "ERROR:" + ee.toString();
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static void purgeSync(Properties request, VFS uVFS, String root_dir) throws Exception {}
}
