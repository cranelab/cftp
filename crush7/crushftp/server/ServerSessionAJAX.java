package crushftp.server;

import com.crushftp.client.AgentUI;
import com.crushftp.client.Base64;
import com.crushftp.client.Common;
import com.crushftp.client.FileClient;
import com.crushftp.client.File_B;
import com.crushftp.client.GDriveClient;
import com.crushftp.client.GenericClient;
import com.crushftp.client.HTTPClient;
import com.crushftp.client.URLConnection;
import com.crushftp.client.VRL;
import com.crushftp.client.Worker;
import com.crushftp.tunnel2.Tunnel2;
import com.crushftp.tunnel3.StreamController;
import crushftp.db.SearchHandler;
import crushftp.gui.LOC;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.PreviewWorker;
import crushftp.handlers.SessionCrush;
import crushftp.handlers.SharedSession;
import crushftp.handlers.SharedSessionReplicated;
import crushftp.handlers.SyncTools;
import crushftp.handlers.UserTools;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

public class ServerSessionAJAX {
  ServerSessionHTTP thisSessionHTTP = null;
  
  public ServerSessionAJAX(ServerSessionHTTP thisSessionHTTP) {
    this.thisSessionHTTP = thisSessionHTTP;
  }
  
  public boolean checkLogin1(Properties request) throws Exception {
    if (this.thisSessionHTTP.thisSession.server_item.getProperty("recaptcha_enabled", "false").equals("true"))
      if (this.thisSessionHTTP.thisSession.server_item.getProperty("recaptcha_version", "1").equals("1")) {
        HttpURLConnection urlc = (HttpURLConnection)(new URL("http://www.google.com/recaptcha/api/verify")).openConnection();
        urlc.setDoOutput(true);
        String s = "privatekey=" + this.thisSessionHTTP.thisSession.server_item.getProperty("recaptcha_private_key", "") + "&remoteip=" + this.thisSessionHTTP.thisSession.uiSG("user_ip") + "&challenge=" + request.getProperty("recaptcha_challenge_field", "") + "&response=" + request.getProperty("recaptcha_response_field", "");
        OutputStream out = urlc.getOutputStream();
        out.write(s.getBytes("UTF8"));
        out.close();
        BufferedReader br = new BufferedReader(new InputStreamReader(urlc.getInputStream()));
        String result = br.readLine();
        br.close();
        urlc.disconnect();
        if (result.equalsIgnoreCase("false"))
          return false; 
      } else if (this.thisSessionHTTP.thisSession.server_item.getProperty("recaptcha_version", "1").equals("2")) {
        HttpURLConnection urlc = (HttpURLConnection)(new URL("https://www.google.com/recaptcha/api/siteverify")).openConnection();
        urlc.setDoOutput(true);
        String s = "secret=" + this.thisSessionHTTP.thisSession.server_item.getProperty("recaptcha_private_key", "") + "&remoteip=" + this.thisSessionHTTP.thisSession.uiSG("user_ip") + "&response=" + request.getProperty("g-recaptcha-response", "");
        OutputStream out = urlc.getOutputStream();
        out.write(s.getBytes("UTF8"));
        out.close();
        BufferedReader br = new BufferedReader(new InputStreamReader(urlc.getInputStream()));
        boolean ok = false;
        String result = "";
        Log.log("SERVER", 2, "Recaptcha:response=" + request.getProperty("g-recaptcha-response", ""));
        while ((result = br.readLine()) != null) {
          if (result.indexOf("true") >= 0)
            ok = true; 
          Log.log("SERVER", 2, "Recaptcha:" + result);
        } 
        br.close();
        urlc.disconnect();
        if (!ok)
          return false; 
      }  
    if (request.getProperty("clientid", "").equalsIgnoreCase("CrushFTPDrive") && ServerStatus.siIG("enterprise_level") <= 0)
      return false; 
    if (ServerStatus.BG("username_uppercase"))
      request.put("username", request.getProperty("username").toUpperCase()); 
    if (ServerStatus.BG("lowercase_usernames"))
      request.put("username", request.getProperty("username").toLowerCase()); 
    this.thisSessionHTTP.thisSession.uiPUT("user_name", "anonymous");
    this.thisSessionHTTP.thisSession.uiPUT("user_name_original", VRL.vrlDecode(this.thisSessionHTTP.thisSession.uiSG("user_name")));
    this.thisSessionHTTP.thisSession.uiPUT("current_password", VRL.vrlDecode(request.getProperty("password")));
    this.thisSessionHTTP.thisSession.uiPUT("user_name", VRL.vrlDecode(request.getProperty("username")));
    this.thisSessionHTTP.thisSession.uiPUT("clientid", request.getProperty("clientid", ""));
    this.thisSessionHTTP.thisSession.uiPUT("user_name_original", this.thisSessionHTTP.thisSession.uiSG("user_name"));
    this.thisSessionHTTP.this_thread.setName(String.valueOf(this.thisSessionHTTP.thisSession.uiSG("user_name")) + ":(" + this.thisSessionHTTP.thisSession.uiSG("user_number") + ")-" + this.thisSessionHTTP.thisSession.uiSG("user_ip") + " (control)");
    this.thisSessionHTTP.thisSession.uiPUT("skip_proxy_check", "false");
    this.thisSessionHTTP.thisSession.runPlugin("beforeLogin", null);
    boolean good = this.thisSessionHTTP.thisSession.login_user_pass();
    this.thisSessionHTTP.setupSession();
    if (!good)
      ServerStatus.siPUT2("failed_logins", ServerStatus.IG("failed_logins") + 1); 
    return good;
  }
  
  public String checkLogin2(String response1, Properties request) {
    String response2 = "";
    if (this.thisSessionHTTP.thisSession.uiBG("password_expired")) {
      if (ServerStatus.BG("expire_password_email_token_only")) {
        response2 = "<loginResult><response>failure</response><message>Password Expired.  You must use the reset password link.</message></loginResult>";
      } else {
        response2 = "<loginResult><response>password_expired</response><message>You must change your password.</message></loginResult>";
      } 
    } else {
      String session_id = this.thisSessionHTTP.thisSession.getId();
      response2 = "<loginResult><response>success</response><c2f>" + session_id.substring(session_id.length() - 4) + "</c2f></loginResult>";
    } 
    this.thisSessionHTTP.createCookieSession(false);
    Enumeration keys = request.keys();
    Properties request2 = (Properties)request.clone();
    while (keys.hasMoreElements()) {
      String key = (String)keys.nextElement();
      String val = request.getProperty(key, "");
      if (key.toUpperCase().indexOf("PASSWORD") >= 0)
        val = "******************"; 
      request2.put(key, val);
      this.thisSessionHTTP.thisSession.user_info.put("post_" + key, val);
    } 
    this.thisSessionHTTP.thisSession.user_info.put("post_parameters", request2);
    this.thisSessionHTTP.thisSession.do_event5("LOGIN", null);
    this.thisSessionHTTP.writeCookieAuth = true;
    SessionCrush session = (SessionCrush)SharedSession.find("crushftp.sessions").get(this.thisSessionHTTP.thisSession.getId());
    if (session == null) {
      response2 = "<loginResult><response>failure</response><message>session expired</message></loginResult>";
    } else {
      session.put("expire_time", "0");
      if (this.thisSessionHTTP.thisSession.IG("max_login_time") != 0)
        session.put("expire_time", (new StringBuffer(String.valueOf(System.currentTimeMillis() + (this.thisSessionHTTP.thisSession.IG("max_login_time") * 60000)))).toString()); 
      if (request.containsKey("clientid")) {
        session.put("clientid", request.getProperty("clientid"));
        this.thisSessionHTTP.thisSession.user_info.put("clientid", request.getProperty("clientid"));
      } 
      session.put("SESSION_RID", this.thisSessionHTTP.thisSession.uiSG("SESSION_RID"));
      if (this.thisSessionHTTP.thisSession.user_info.containsKey("eventResultText"))
        response1 = this.thisSessionHTTP.thisSession.user_info.getProperty("eventResultText"); 
    } 
    return String.valueOf(response1) + response2;
  }
  
  public boolean processItemAnonymous(Properties request, Properties urlRequestItems, String req_id) throws Exception {
    String command = request.getProperty("command", "");
    if (command.equalsIgnoreCase("ping")) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      response = String.valueOf(response) + "<pong>" + System.currentTimeMillis() + "</pong>";
      this.thisSessionHTTP.done = true;
      return writeResponse(response);
    } 
    if (command.equalsIgnoreCase("loginSettings")) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      response = String.valueOf(response) + "<info><save_password>" + ServerStatus.BG("allow_save_pass_phone") + "</save_password></info>";
      this.thisSessionHTTP.done = true;
      return writeResponse(response);
    } 
    if (command.equalsIgnoreCase("discard")) {
      this.thisSessionHTTP.done = true;
      return writeResponse("");
    } 
    if (request.getProperty("encoded", "true").equals("true") && (command.equalsIgnoreCase("login") || request.getProperty("the_action", "").equalsIgnoreCase("login"))) {
      this.thisSessionHTTP.createCookieSession(true);
      request.put("username", request.getProperty("username", "").trim());
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      if (checkLogin1(request)) {
        response = checkLogin2(response, request);
        if (!request.getProperty("redirect", "").equals("")) {
          this.thisSessionHTTP.sendRedirect("/WebInterface/error.html");
          write_command_http("Connection: close");
          write_command_http("Content-Length: 0");
          write_command_http("");
          return true;
        } 
        this.thisSessionHTTP.thisSession.active();
      } else {
        this.thisSessionHTTP.thisSession.uiPUT("user_name", "");
        this.thisSessionHTTP.thisSession.uiPUT("user_name_original", this.thisSessionHTTP.thisSession.uiSG("user_name"));
        if (this.thisSessionHTTP.thisSession.uiSG("plugin_message").indexOf("CHALLENGE:") >= 0) {
          response = String.valueOf(response) + "<loginResult><response>challenge</response><message>" + Common.url_encode(this.thisSessionHTTP.thisSession.uiSG("lastLog")) + "</message></loginResult>";
        } else if (this.thisSessionHTTP.thisSession.uiSG("lastLog").indexOf("<response>") >= 0 && this.thisSessionHTTP.thisSession.uiSG("lastLog").indexOf("<message>") >= 0) {
          String message = this.thisSessionHTTP.thisSession.uiSG("lastLog").substring(this.thisSessionHTTP.thisSession.uiSG("lastLog").indexOf("<message>") + "<message>".length(), this.thisSessionHTTP.thisSession.uiSG("lastLog").indexOf("</message>"));
          response = String.valueOf(response) + "<loginResult><response>failure</response><message>" + Common.url_encode(message) + "</message></loginResult>";
        } else {
          response = String.valueOf(response) + "<loginResult><response>failure</response><message>Check your username or password and try again.\r\n" + Common.url_encode(this.thisSessionHTTP.thisSession.uiSG("lastLog")) + "</message></loginResult>";
        } 
      } 
      return writeResponse(response);
    } 
    if (request.containsKey("u") || request.containsKey("p")) {
      this.thisSessionHTTP.createCookieSession(true);
      this.thisSessionHTTP.thisSession.uiPUT("user_name", "anonymous");
      this.thisSessionHTTP.thisSession.uiPUT("user_name_original", this.thisSessionHTTP.thisSession.uiSG("user_name"));
      request.put("password", request.getProperty("p", ""));
      request.put("username", request.getProperty("u", ""));
      boolean good = checkLogin1(request);
      if (good) {
        checkLogin2("", request);
        String autoPath = request.getProperty("path", "/");
        urlRequestItems.remove("p");
        this.thisSessionHTTP.thisSession.active();
        if (!this.thisSessionHTTP.thisSession.user_info.getProperty("redirect_url", "").equals("")) {
          autoPath = this.thisSessionHTTP.thisSession.user_info.getProperty("redirect_url", "");
          this.thisSessionHTTP.thisSession.user_info.put("redirect_url", "");
        } 
        if (!autoPath.endsWith("/") && !autoPath.equals("") && autoPath.indexOf("/WebInterface/") < 0 && ServerStatus.BG("direct_link_access")) {
          String header0 = this.thisSessionHTTP.headers.elementAt(0).toString();
          header0 = String.valueOf(header0.substring(0, header0.indexOf(" "))) + " " + Common.dots(autoPath) + header0.substring(header0.lastIndexOf(" "));
          this.thisSessionHTTP.headers.setElementAt(header0, 0);
          request.remove("path");
          return false;
        } 
        if (ServerStatus.BG("direct_link_to_webinterface")) {
          this.thisSessionHTTP.sendRedirect("/#" + autoPath);
          write_command_http("Content-Length: 0");
          write_command_http("");
          return true;
        } 
        this.thisSessionHTTP.sendRedirect(autoPath);
        write_command_http("Content-Length: 0");
        write_command_http("");
        return true;
      } 
      if (!this.thisSessionHTTP.thisSession.user_info.getProperty("redirect_url", "").equals("")) {
        this.thisSessionHTTP.sendRedirect(this.thisSessionHTTP.thisSession.user_info.getProperty("redirect_url", ""));
        write_command_http("Content-Length: 0");
        write_command_http("");
        return true;
      } 
      if (!request.getProperty("u", "").equals("") && !request.getProperty("p", "").equals("")) {
        this.thisSessionHTTP.sendRedirect("/WebInterface/login.html?u=" + request.getProperty("u") + "&p=" + request.getProperty("p"));
      } else if (!request.getProperty("u", "").equals("") && request.getProperty("p", "").equals("")) {
        this.thisSessionHTTP.sendRedirect("/WebInterface/login.html?u=" + request.getProperty("u"));
      } else if (request.getProperty("u", "").equals("") && !request.getProperty("p", "").equals("")) {
        this.thisSessionHTTP.sendRedirect("/WebInterface/login.html?p=" + request.getProperty("p"));
      } else if (request.getProperty("u", "").equals("") && request.getProperty("p", "").equals("") && !request.getProperty("path", "").equals("")) {
        this.thisSessionHTTP.sendRedirect("/WebInterface/login.html?path=" + request.getProperty("path"));
      } 
      write_command_http("Content-Length: 0");
      write_command_http("");
      return true;
    } 
    if (command.equalsIgnoreCase("emailpassword") || request.getProperty("the_action", "").equalsIgnoreCase("emailpassword")) {
      String response = doEmailPass(request, this.thisSessionHTTP.thisSession, req_id);
      response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n<emailPass><response>" + response + "</response></emailPass>";
      return writeResponse(response);
    } 
    if (command.equalsIgnoreCase("request_reset")) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      String reset_username_email = Common.url_decode(request.getProperty("reset_username_email"));
      String responseText = "";
      String token = Common.makeBoundary();
      if (Common.System2.get("crushftp.dmz.queue.sock") != null) {
        Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
        Properties action = new Properties();
        action.put("type", "GET:SINGLETON");
        action.put("id", Common.makeBoundary());
        action.put("need_response", "true");
        queue.addElement(action);
        action = UserTools.waitResponse(action, 60);
        Object object = "";
        if (action != null)
          object = action.get("singleton_id"); 
        action = new Properties();
        action.put("type", "GET:RESET_TOKEN");
        action.put("id", Common.makeBoundary());
        action.put("reset_username_email", reset_username_email);
        action.put("currentURL", request.getProperty("currentURL"));
        action.put("need_response", "true");
        action.put("reset_token", token);
        action.put("singleton_id", object);
        queue.addElement(action);
        action = UserTools.waitResponse(action, 60);
        if (action != null)
          Object object1 = action.get("responseText"); 
      } else {
        responseText = doResetToken(reset_username_email, request.getProperty("currentURL"), this.thisSessionHTTP.thisSession.server_item.getProperty("linkedServer"), token, true);
      } 
      response = String.valueOf(response) + "<commandResult><response>" + Common.url_encode(responseText) + "</response></commandResult>";
      return writeResponse(response);
    } 
    if (command.equalsIgnoreCase("reset_password")) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      String responseText = "This password reset link is invalid or expired.";
      if (Common.System2.get("crushftp.dmz.queue.sock") != null) {
        Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
        Properties action = new Properties();
        action.put("type", "GET:RESET_TOKEN_PASS");
        action.put("id", Common.makeBoundary());
        action.put("resetToken", request.getProperty("resetToken"));
        action.put("password1", request.getProperty("password1"));
        action.put("need_response", "true");
        queue.addElement(action);
        action = UserTools.waitResponse(action, 60);
        if (action != null)
          Object object = action.get("responseText"); 
      } else {
        responseText = doResetTokenPass(request.getProperty("resetToken"), this.thisSessionHTTP.thisSession.server_item.getProperty("linkedServer"), request.getProperty("password1"), this.thisSessionHTTP.thisSession.user_info);
      } 
      response = String.valueOf(response) + "<commandResult><response>" + responseText + "</response></commandResult>";
      return writeResponse(response);
    } 
    if (command.equalsIgnoreCase("register_gdrive_api") && ServerStatus.BG("v8_beta")) {
      this.thisSessionHTTP.thisSession.put("gdrive_api_code", request.getProperty("code"));
      this.thisSessionHTTP.sendRedirect("/WebInterface/error.html");
      write_command_http("Content-Length: 0");
      write_command_http("");
      return true;
    } 
    if (command.equalsIgnoreCase("lookup_gdrive_api_code") && ServerStatus.BG("v8_beta")) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      if (this.thisSessionHTTP.thisSession.getProperty("gdrive_api_code") != null) {
        Properties p = GDriveClient.setup_bearer((String)this.thisSessionHTTP.thisSession.remove("gdrive_api_code"), request.getProperty("server_url"));
        response = String.valueOf(response) + "<commandResult><response>" + p.getProperty("refresh_token") + "</response></commandResult>";
      } else {
        response = String.valueOf(response) + "<commandResult><response></response></commandResult>";
      } 
      return writeResponse(response);
    } 
    if (command.equalsIgnoreCase("getSessionTimeout")) {
      long time = Long.parseLong(this.thisSessionHTTP.thisSession.getProperty("last_activity", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString()));
      long timeout = 60L * ServerStatus.LG("http_session_timeout");
      if (this.thisSessionHTTP.thisSession.user != null) {
        long timeout2 = Long.parseLong(this.thisSessionHTTP.thisSession.user.getProperty("max_idle_time", "10"));
        if (timeout2 < 0L) {
          timeout = timeout2 * -1L;
        } else if (timeout2 != 0L && timeout2 < timeout) {
          timeout = 60L * timeout2;
        } 
      } 
      long remaining = timeout - (new Date()).getTime() / 1000L - time / 1000L;
      try {
        if (!this.thisSessionHTTP.thisSession.getProperty("expire_time").equals("0"))
          remaining = (Long.parseLong(this.thisSessionHTTP.thisSession.getProperty("expire_time")) - System.currentTimeMillis()) / 1000L; 
      } catch (Exception exception) {}
      if (!this.thisSessionHTTP.thisSession.uiBG("user_logged_in")) {
        Thread.sleep(10000L);
        if (!this.thisSessionHTTP.thisSession.uiBG("user_logged_in"))
          remaining = 0L; 
      } 
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      response = String.valueOf(response) + "<commandResult><response>" + remaining + "</response></commandResult>";
      return writeResponse(response);
    } 
    if (command.equalsIgnoreCase("encryptPassword")) {
      String pass = VRL.vrlDecode(Common.url_decode(request.getProperty("password")));
      String encryptedPass = ServerStatus.thisObj.common_code.encode_pass(pass, "DES", "");
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      response = String.valueOf(response) + "<result><response>" + Common.url_encode(encryptedPass) + "</response></result>";
      return writeResponse(response);
    } 
    if (command.equalsIgnoreCase("taskResponse")) {
      Vector v = ServerStatus.siVG("running_tasks");
      String result = "Invalid task, or key.";
      for (int x = v.size() - 1; x >= 0; x--) {
        Properties tracker = v.elementAt(x);
        if (tracker.getProperty("id").equals(request.getProperty("job_id", "")) && tracker.containsKey(request.getProperty("task_key", ""))) {
          tracker.put(request.getProperty("task_key"), request.getProperty("task_val"));
          Thread.sleep(2300L);
          result = tracker.getProperty(String.valueOf(request.getProperty("task_key")) + "_result", "No result.");
        } 
      } 
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      response = String.valueOf(response) + "<result><response>" + Common.url_encode(result) + "</response></result>";
      return writeResponse(response);
    } 
    return false;
  }
  
  public static String doResetTokenPass(String resetToken, String linkedServer, String password1, Properties user_info) {
    String responseText = "";
    Properties resetTokens = ServerStatus.siPG("resetTokens");
    if (resetTokens == null)
      resetTokens = new Properties(); 
    ServerStatus.thisObj.server_info.put("resetTokens", resetTokens);
    if (resetTokens.containsKey(resetToken)) {
      Properties reset = (Properties)resetTokens.get(resetToken);
      if (reset.getProperty("site", "").indexOf("(SITE_PASS)") < 0) {
        responseText = "ERROR: Your username does not allow password changes.";
      } else if (!Common.checkPasswordRequirements(Common.url_decode(password1), reset.getProperty("password_history", "")).equals("")) {
        responseText = "ERROR: " + Common.checkPasswordRequirements(Common.url_decode(password1), reset.getProperty("password_history", ""));
      } else {
        resetTokens.remove(resetToken);
        UserTools.ut.put_in_user(linkedServer, reset.getProperty("username"), "password", ServerStatus.thisObj.common_code.encode_pass(Common.url_decode(password1), ServerStatus.SG("password_encryption"), ""), true, true);
        UserTools.ut.put_in_user(linkedServer, reset.getProperty("username"), "password_history", Common.getPasswordHistory(Common.url_decode(password1), reset.getProperty("password_history", "")), true, true);
        if (!reset.getProperty("expire_password_days", "0").equals("0") && !reset.getProperty("expire_password_days", "0").equals("")) {
          Calendar gc = new GregorianCalendar();
          gc.setTime(new Date());
          gc.add(5, Integer.parseInt(reset.getProperty("expire_password_days", "0")));
          SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa", Locale.US);
          String s = sdf.format(gc.getTime());
          UserTools.ut.put_in_user(linkedServer, reset.getProperty("username"), "expire_password_when", s, true, true);
        } 
        responseText = "Password changed.  Please login using your new password.";
        if (user_info.containsKey("user_name"))
          user_info.put("user_name", reset.getProperty("username")); 
        SessionCrush tempSession = new SessionCrush(null, 1, "127.0.0.1", 0, "0.0.0.0", "MainUsers", new Properties());
        tempSession.user = reset;
        tempSession.user_info = user_info;
        ServerStatus.thisObj.runAlerts("password_change", null, user_info, tempSession);
      } 
    } else {
      responseText = "ERROR: The link is invalid or expired.";
    } 
    return responseText;
  }
  
  public static String doResetToken(String reset_username_email, String currentURL, String linkedServer, String token, boolean sendEmail) throws IOException {
    reset_username_email = Common.url_decode(reset_username_email);
    String responseText = "";
    Vector newUsers = new Vector();
    UserTools.refreshUserList(linkedServer, newUsers);
    Vector matchingUsernames = UserTools.findUserEmail(linkedServer, reset_username_email);
    if (UserTools.db_class.equalsIgnoreCase("SQL")) {
      Properties user = UserTools.ut.getUser(linkedServer, reset_username_email, true);
      if (user != null)
        matchingUsernames.addElement(user); 
    } 
    for (int x = matchingUsernames.size() - 1; x >= 0; x--) {
      Properties user = matchingUsernames.elementAt(x);
      if (user.getProperty("email", "").equals("") || (!user.getProperty("username").equalsIgnoreCase(reset_username_email) && !user.getProperty("email", "").equalsIgnoreCase(reset_username_email))) {
        matchingUsernames.removeElementAt(x);
      } else {
        matchingUsernames.setElementAt(user, x);
      } 
    } 
    if (matchingUsernames.size() == 1) {
      Properties user = matchingUsernames.elementAt(0);
      Properties resetTokens = ServerStatus.siPG("resetTokens");
      if (resetTokens == null)
        resetTokens = new Properties(); 
      ServerStatus.thisObj.server_info.put("resetTokens", resetTokens);
      Properties reset = UserTools.ut.getUser(linkedServer, user.getProperty("username"), true);
      reset.put("generated", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      resetTokens.put(token, reset);
      String url = String.valueOf(currentURL) + "?token=" + token;
      String resetMsg = ServerStatus.SG("password_reset_message");
      resetMsg = Common.replace_str(resetMsg, "{url}", url);
      resetMsg = ServerStatus.change_vars_to_values_static(resetMsg, user, new Properties(), null);
      if (sendEmail)
        Common.send_mail(ServerStatus.SG("discovered_ip"), reset.getProperty("email"), "", "", ServerStatus.SG("smtp_from"), ServerStatus.SG("password_reset_subject"), resetMsg, ServerStatus.SG("smtp_server"), ServerStatus.SG("smtp_user"), ServerStatus.SG("smtp_pass"), ServerStatus.SG("smtp_ssl").equals("true"), true, (File_B[])null); 
      responseText = ServerStatus.SG("password_reset_message_browser");
    } else {
      responseText = "Unable to locate this user.";
      Log.log("SERVER", 0, "Unable to locate the user...found matching usernames:" + matchingUsernames);
    } 
    return responseText;
  }
  
  public static String doEmailPass(Properties p, SessionCrush thisSession, String req_id) {
    String lookupUsername = p.getProperty("username");
    Properties lookupUser = null;
    if (Common.System2.get("crushftp.dmz.queue.sock") != null) {
      Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
      Properties action = new Properties();
      action.put("type", "GET:USER");
      action.put("id", Common.makeBoundary());
      action.put("username", lookupUsername);
      action.put("need_response", "true");
      queue.addElement(action);
      action = UserTools.waitResponse(action, 60);
      if (action != null)
        lookupUser = (Properties)action.get("user"); 
    } else {
      thisSession.verify_user(lookupUsername, "", true);
      lookupUser = thisSession.user;
    } 
    thisSession.user = null;
    String result = "";
    String standardError = LOC.G("An email has been sent if the user was found.  If no email is received, then the username / email didn't exist or you are not allowed to have your password emailed to you.");
    if (ServerStatus.SG("smtp_server").equals("")) {
      result = LOC.G("This server is not configured to send email password reminders.");
    } else if (lookupUser == null) {
      lookupUser = new Properties();
      result = standardError;
    } else if (lookupUser.getProperty("site", "").toUpperCase().indexOf("(SITE_EMAILPASSWORD)") >= 0) {
      String pass = lookupUser.getProperty("password", "");
      if (pass.startsWith("SHA:") || pass.startsWith("SHA512:") || pass.startsWith("SHA3:") || pass.startsWith("MD5:") || pass.startsWith("CRYPT3:")) {
        pass = LOC.G("(Your password is encrypted and cannot be revealed.  Please contact your server administrator to have it reset.)");
      } else {
        pass = (new Common()).decode_pass(pass);
      } 
      lookupUser.put("user_name", lookupUser.getProperty("username"));
      String to = lookupUser.getProperty("email", "");
      String from = ServerStatus.SG("smtp_from");
      if (from.equals(""))
        from = to; 
      if (!to.equals("")) {
        if (ServerStatus.BG("expire_emailed_passwords")) {
          Properties tempUser = UserTools.ut.getUser(thisSession.server_item.getProperty("linkedServer"), lookupUsername, false);
          tempUser.put("expire_password_when", "01/01/1978 12:00:00 AM");
          if (!ServerStatus.siBG("dmz_mode"))
            UserTools.writeUser(thisSession.server_item.getProperty("linkedServer"), lookupUsername, tempUser); 
        } 
        String subject = ServerStatus.SG("emailReminderSubjectText");
        String body = ServerStatus.SG("emailReminderBodyText");
        body = Common.replace_str(body, "%user_pass%", pass);
        body = Common.replace_str(body, "{user_pass}", pass);
        Properties user_info2 = (Properties)thisSession.user_info.clone();
        user_info2.putAll(lookupUser);
        subject = ServerStatus.thisObj.change_vars_to_values(subject, lookupUser, user_info2, null);
        body = ServerStatus.thisObj.change_vars_to_values(body, lookupUser, user_info2, null);
        result = Common.send_mail(ServerStatus.SG("discovered_ip"), to, "", "", from, subject, body, ServerStatus.SG("smtp_server"), ServerStatus.SG("smtp_user"), ServerStatus.SG("smtp_pass"), ServerStatus.SG("smtp_ssl").equals("true"), ServerStatus.SG("smtp_html").equals("true"), (File_B[])null);
        thisSession.add_log_formatted(String.valueOf(LOC.G("Password Emailed to user:")) + lookupUsername + "  " + to + "   " + LOC.G("Email Result:") + result, "POST", req_id);
        if (result.toUpperCase().indexOf("ERROR") >= 0) {
          result = LOC.G("An error occured when generating the email.");
        } else {
          result = standardError;
        } 
      } else {
        result = standardError;
      } 
    } else {
      result = standardError;
    } 
    return result;
  }
  
  public boolean getUserName(Properties request) throws Exception {
    if (request.getProperty("command", "").equalsIgnoreCase("getUserName")) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      if (ServerStatus.BG("csrf") && !request.getProperty("c2f", "").equals("")) {
        String session_id = this.thisSessionHTTP.thisSession.getId();
        try {
          if (!request.getProperty("c2f", "").equalsIgnoreCase(session_id.substring(session_id.length() - 4))) {
            this.thisSessionHTTP.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
            response = String.valueOf(response) + "<commandResult><response>FAILURE:Access Denied. (c2f)</response></commandResult>";
            return writeResponse(response);
          } 
        } catch (Exception e) {
          Log.log("HTTP_SERVER", 2, e);
          this.thisSessionHTTP.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
          response = String.valueOf(response) + "<loginResult><response>failure</response></loginResult>";
          return writeResponse(response);
        } 
      } 
      if (this.thisSessionHTTP.thisSession.uiBG("user_logged_in") && !this.thisSessionHTTP.thisSession.uiSG("user_name").equals("")) {
        response = String.valueOf(response) + "<loginResult><response>success</response><username>" + this.thisSessionHTTP.thisSession.uiSG("user_name") + "</username></loginResult>";
      } else {
        response = String.valueOf(response) + "<loginResult><response>failure</response></loginResult>";
      } 
      return writeResponse(response);
    } 
    return false;
  }
  
  public void logout_all() {
    String auth_temp = this.thisSessionHTTP.thisSession.getId();
    this.thisSessionHTTP.thisSession.do_event5("LOGOUT_ALL", null);
    this.thisSessionHTTP.thisSession.uiPUT("user_name_original", "");
    this.thisSessionHTTP.createCookieSession(true);
    this.thisSessionHTTP.done = true;
    if (this.thisSessionHTTP.thisSession.uVFS != null) {
      this.thisSessionHTTP.thisSession.uVFS.free();
      this.thisSessionHTTP.thisSession.uVFS.disconnect();
    } 
    ServerStatus.thisObj.remove_user(this.thisSessionHTTP.thisSession.user_info);
    if (SharedSessionReplicated.send_queues.size() > 0 && ServerStatus.BG("replicate_sessions"))
      SharedSessionReplicated.send(auth_temp, "crushftp.session.remove_user", "user_info", null); 
  }
  
  public boolean processItems(Properties request, Vector byteRanges, String req_id) throws Exception {
    String site = this.thisSessionHTTP.thisSession.SG("site");
    String command = request.getProperty("command", "");
    if (site.indexOf("(CONNECT)") < 0) {
      request.put("serverGroup_original", request.getProperty("serverGroup", ""));
      request.put("serverGroup", this.thisSessionHTTP.thisSession.server_item.getProperty("linkedServer"));
      request.put("serverGroup_backup", request.getProperty("serverGroup", ""));
      String groupName = this.thisSessionHTTP.thisSession.SG("admin_group_name");
      if (groupName.equals("") || groupName.equals("admin_group_name"))
        groupName = this.thisSessionHTTP.thisSession.uiSG("user_name"); 
      this.thisSessionHTTP.thisSession.put("admin_group_name", groupName);
    } 
    String session_id = this.thisSessionHTTP.thisSession.getId();
    if (ServerStatus.BG("csrf") && !request.getProperty("command", "").equals("")) {
      if (request.getProperty("command", "").equals("getUserInfo") && request.getProperty("c2f", "").equals("false"))
        this.thisSessionHTTP.writeCookieAuth = true; 
      if (("," + ServerStatus.SG("whitelist_web_commands") + ",").indexOf("," + request.getProperty("command") + ",") < 0 && this.thisSessionHTTP.thisSession.user_info.getProperty("authorization_header", "false").equals("false"))
        if (!request.getProperty("c2f", "").equalsIgnoreCase(session_id.substring(session_id.length() - 4))) {
          this.thisSessionHTTP.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
          return writeResponse("<commandResult><response>FAILURE:Access Denied. (c2f)</response></commandResult>");
        }  
    } 
    if (command.equalsIgnoreCase("getServerItem")) {
      if (AdminControls.checkRole(command, site))
        return writeResponse(AdminControls.buildXML(AdminControls.getServerItem(this.thisSessionHTTP.thisSession.SG("admin_group_name"), request, site), "result_value", "OK"), false, 200, false, false, false); 
      return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
    } 
    if (command.equalsIgnoreCase("getDashboardItems")) {
      if (AdminControls.checkRole(command, site))
        return writeResponse(AdminControls.buildXML(AdminControls.getDashboardItems(this.thisSessionHTTP.thisSession.SG("admin_group_name"), request, site), "result_value", "OK"), false, 200, false, false, false); 
      return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
    } 
    if (command.equalsIgnoreCase("getStatHistory")) {
      if (AdminControls.checkRole(command, site))
        return writeResponse(AdminControls.getStatHistory(request), false, 200, true, false, false); 
      return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
    } 
    if (command.equalsIgnoreCase("getJobsSummary")) {
      request.put("calling_user", this.thisSessionHTTP.thisSession.uiSG("user_name"));
      if (AdminControls.checkRole(command, site))
        return writeResponse(AdminControls.buildXML(AdminControls.getJobsSummary(request, site), "running_tasks", "OK"), false, 200, false, false, false); 
      return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
    } 
    if (command.equalsIgnoreCase("getJobInfo")) {
      Common.urlDecodePost(request);
      request.put("calling_user", this.thisSessionHTTP.thisSession.uiSG("user_name"));
      if (AdminControls.checkRole(command, site))
        return writeResponse(AdminControls.buildXML(AdminControls.getJobInfo(request, site), "running_tasks", "OK"), false, 200, false, false, false); 
      return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
    } 
    if (command.equalsIgnoreCase("getSessionList")) {
      if (AdminControls.checkRole(command, site))
        return writeResponse(AdminControls.buildXML(AdminControls.getSessionList(request), "session_list", "OK"), false, 200, true, false, false); 
      return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
    } 
    if (command.equalsIgnoreCase("getLog")) {
      Common.urlDecodePost(request);
      if (AdminControls.checkRole(command, site))
        return writeResponse(AdminControls.buildXML(AdminControls.getLog(request, site), "log_data", "OK"), false, 200, false, false, false); 
      return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
    } 
    if (command.equalsIgnoreCase("getJob")) {
      Common.urlDecodePost(request);
      request.put("calling_user", this.thisSessionHTTP.thisSession.uiSG("user_name"));
      if (AdminControls.checkRole(command, site))
        return writeResponse(AdminControls.buildXML(AdminControls.getJob(request, site), "result_value", "OK"), false, 200, false, false, true); 
      if (site.indexOf("(JOB_LIST)") < 0)
        return writeResponse(AdminControls.buildXML(new Vector(), "result_value", "OK"), false, 200, false, false, true); 
      return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
    } 
    String last_activity_time = this.thisSessionHTTP.thisSession.getProperty("last_activity");
    this.thisSessionHTTP.thisSession.active();
    if (command.equalsIgnoreCase("logout")) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      response = String.valueOf(response) + "<commandResult><response>Logged out.</response></commandResult>";
      logout_all();
      writeResponse(response);
      return true;
    } 
    if (command.equalsIgnoreCase("getCrushAuth")) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      response = String.valueOf(response) + "<auth>CrushAuth=" + this.thisSessionHTTP.thisSession.getId() + "</auth>";
      return writeResponse(response);
    } 
    if (command.equalsIgnoreCase("renameJob")) {
      Common.urlDecodePost(request);
      if (AdminControls.checkRole(command, site))
        return writeResponse(AdminControls.buildXML(AdminControls.renameJob(request, site, true), "result_value", "OK"), false, 200, false, false, true); 
      return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
    } 
    if (command.equalsIgnoreCase("removeJob")) {
      Common.urlDecodePost(request);
      if (AdminControls.checkRole(command, site))
        return writeResponse(AdminControls.buildXML(AdminControls.removeJob(request, site, true), "result_value", "OK"), false, 200, false, false, true); 
      return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
    } 
    if (command.equalsIgnoreCase("changeJobStatus")) {
      Common.urlDecodePost(request);
      if (AdminControls.checkRole(command, site))
        return writeResponse(AdminControls.buildXML(AdminControls.changeJobStatus(request, site), "result_value", "OK"), false, 200, false, false, true); 
      return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
    } 
    if (command.equalsIgnoreCase("addJob")) {
      if (AdminControls.checkRole(command, site))
        return writeResponse(AdminControls.buildXML(AdminControls.addJob(request, site, true), "result_value", "OK"), false, 200, false, false, true); 
      return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
    } 
    if (command.equalsIgnoreCase("startTunnel2")) {
      String response = "";
      SharedSession.find("crushftp.usernames").put(String.valueOf(Common.getPartialIp("127.0.0.1")) + "_" + this.thisSessionHTTP.thisSession.getId() + "_user", SharedSession.find("crushftp.usernames").getProperty(String.valueOf(Common.getPartialIp(this.thisSessionHTTP.thisSession.uiSG("user_ip"))) + "_" + this.thisSessionHTTP.thisSession.getId() + "_user"), false);
      SharedSession.find("crushftp.usernames").put(String.valueOf(Common.getPartialIp("127.0.0.1")) + "_" + this.thisSessionHTTP.thisSession.getId() + "_ip", this.thisSessionHTTP.thisSession.uiSG("user_ip"), false);
      String userTunnels = String.valueOf(this.thisSessionHTTP.thisSession.user.getProperty("tunnels", "").trim()) + ",";
      Vector tunnels = (Vector)ServerStatus.VG("tunnels").clone();
      tunnels.addAll(ServerStatus.VG("tunnels_dmz"));
      Properties tunnel = null;
      for (int x = 0; x < tunnels.size(); x++) {
        Properties pp = tunnels.elementAt(x);
        if (userTunnels.indexOf(String.valueOf(pp.getProperty("id")) + ",") >= 0 && pp.getProperty("id").equals(request.getProperty("tunnelId"))) {
          tunnel = (Properties)pp.clone();
          break;
        } 
      } 
      if (tunnel != null) {
        if (ServerStatus.siIG("enterprise_level") <= 0) {
          tunnel.put("channelsOutMax", "1");
          tunnel.put("channelsInMax", "1");
          tunnel.put("channelRampUp", "1");
          tunnel.put("stableSeconds", "1");
        } 
        if (Tunnel2.getTunnel(this.thisSessionHTTP.thisSession.getId()) == null) {
          Tunnel2 t = new Tunnel2(this.thisSessionHTTP.thisSession.getId(), tunnel);
          if (tunnel.getProperty("reverse", "false").equals("true")) {
            t.setAllowReverseMode(true);
            t.startThreads();
          } 
        } 
      } 
      return writeResponse(response);
    } 
    if (command.equalsIgnoreCase("stopTunnel2")) {
      String response = Tunnel2.stopTunnel(this.thisSessionHTTP.thisSession.getId());
      return writeResponse(response);
    } 
    if (command.equalsIgnoreCase("startTunnel3")) {
      String response = "";
      SharedSession.find("crushftp.usernames").put(String.valueOf(Common.getPartialIp("127.0.0.1")) + "_" + this.thisSessionHTTP.thisSession.getId() + "_user", SharedSession.find("crushftp.usernames").getProperty(String.valueOf(Common.getPartialIp(this.thisSessionHTTP.thisSession.uiSG("user_ip"))) + "_" + this.thisSessionHTTP.thisSession.getId() + "_user"), false);
      SharedSession.find("crushftp.usernames").put(String.valueOf(Common.getPartialIp("127.0.0.1")) + "_" + this.thisSessionHTTP.thisSession.getId() + "_ip", this.thisSessionHTTP.thisSession.uiSG("user_ip"), false);
      SharedSession.find("crushftp.usernames").put(String.valueOf(Common.getPartialIp("127.0.0.1")) + "_" + request.getProperty("clientid") + "_ip", this.thisSessionHTTP.thisSession.uiSG("user_ip"), false);
      String userTunnels = String.valueOf(this.thisSessionHTTP.thisSession.user.getProperty("tunnels", "").trim()) + ",";
      Vector tunnels = (Vector)ServerStatus.VG("tunnels").clone();
      tunnels.addAll(ServerStatus.VG("tunnels_dmz"));
      Properties tunnel = null;
      for (int x = 0; x < tunnels.size(); x++) {
        Properties pp = tunnels.elementAt(x);
        if (userTunnels.indexOf(String.valueOf(pp.getProperty("id")) + ",") >= 0 && pp.getProperty("id").equals(request.getProperty("tunnelId"))) {
          tunnel = (Properties)pp.clone();
          break;
        } 
      } 
      if (tunnel != null) {
        if (tunnel.getProperty("configurable", "false").equals("true")) {
          tunnel.put("bindIp", request.getProperty("bindIp"));
          tunnel.put("localPort", request.getProperty("localPort"));
          tunnel.put("destIp", request.getProperty("destIp"));
          tunnel.put("destPort", request.getProperty("destPort"));
          tunnel.put("channelsOutMax", request.getProperty("channelsOutMax"));
          tunnel.put("channelsInMax", request.getProperty("channelsInMax"));
          tunnel.put("reverse", request.getProperty("reverse"));
        } 
        if (ServerStatus.siIG("enterprise_level") <= 0) {
          tunnel.put("channelsOutMax", "1");
          tunnel.put("channelsInMax", "1");
          tunnel.put("channelRampUp", "1");
          tunnel.put("stableSeconds", "1");
        } 
        if (ServerSessionTunnel3.getStreamController(this.thisSessionHTTP.thisSession.getId(), tunnel.getProperty("tunnelId")) == null) {
          StreamController sc = new StreamController(tunnel);
          ServerSessionTunnel3.running_tunnels.put(String.valueOf(this.thisSessionHTTP.thisSession.getId()) + "_" + tunnel.getProperty("tunnelId"), sc);
          sc.startServerTunnel();
          if (tunnel.getProperty("reverse", "false").equals("true"))
            sc.startReverseThreads(); 
          response = "Started";
        } else {
          response = "Already started.";
        } 
      } 
      return writeResponse(response);
    } 
    if (command.equalsIgnoreCase("stopTunnel3")) {
      String response = ServerSessionTunnel3.stopTunnel(this.thisSessionHTTP.thisSession.getId(), request.getProperty("tunnelId"));
      return writeResponse(response);
    } 
    if (command.equalsIgnoreCase("getSyncTableData") && ServerStatus.siIG("enterprise_level") > 0) {
      Properties item2 = this.thisSessionHTTP.thisSession.uVFS.get_item(String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + request.getProperty("path"));
      String privs = item2.getProperty("privs");
      String syncID = Common.parseSyncPart(privs, "name");
      if (syncID != null) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        String uploadOnly = Common.parseSyncPart(privs, "uploadOnly");
        if (uploadOnly.equalsIgnoreCase("true")) {
          oos.writeObject(new Vector());
        } else {
          Object o = null;
          if (Common.System2.get("crushftp.dmz.queue") != null) {
            Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
            Properties action = new Properties();
            action.put("type", "GET:SYNC");
            action.put("id", Common.makeBoundary());
            action.put("username", this.thisSessionHTTP.thisSession.uiSG("user_name"));
            action.put("request", request);
            action.put("syncID", syncID.toUpperCase());
            action.put("clientid", this.thisSessionHTTP.thisSession.uiSG("clientid"));
            action.put("need_response", "true");
            queue.addElement(action);
            action = UserTools.waitResponse(action, 300);
            o = action.remove("object_response");
          } else {
            String vfs_path = request.getProperty("path", "");
            if (vfs_path.equals(""))
              vfs_path = "/"; 
            if (!vfs_path.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
              vfs_path = String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + vfs_path.substring(1); 
            o = Common.getSyncTableData(syncID.toUpperCase(), Long.parseLong(request.getProperty("lastRID")), request.getProperty("table"), this.thisSessionHTTP.thisSession.uiSG("clientid"), vfs_path, this.thisSessionHTTP.thisSession.uVFS);
          } 
          oos.writeObject(o);
        } 
        oos.close();
        write_command_http("HTTP/1.1 200 OK");
        write_command_http("Cache-Control: no-store");
        write_command_http("Content-Type: application/binary");
        this.thisSessionHTTP.write_standard_headers();
        write_command_http("Content-Length: " + baos.size());
        write_command_http("");
        this.thisSessionHTTP.original_os.write(baos.toByteArray());
        this.thisSessionHTTP.original_os.flush();
      } else {
        String error_msg = "Sync was not found for your current folder.";
        write_command_http("HTTP/1.1 404 OK");
        write_command_http("Cache-Control: no-store");
        write_command_http("Content-Type: application/binary");
        this.thisSessionHTTP.write_standard_headers();
        write_command_http("Content-Length: " + error_msg.length() + '\002');
        write_command_http("");
        write_command_http(error_msg);
      } 
      return true;
    } 
    if (command.equalsIgnoreCase("syncConflict") && ServerStatus.siIG("enterprise_level") > 0) {
      Properties item = this.thisSessionHTTP.thisSession.uVFS.get_item(String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + request.getProperty("path"));
      String syncID = Common.parseSyncPart(item.getProperty("privs"), "name");
      if (syncID != null) {
        if (Common.System2.get("crushftp.dmz.queue") != null) {
          Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
          Properties action = new Properties();
          action.put("type", "GET:SYNC");
          action.put("id", Common.makeBoundary());
          action.put("username", this.thisSessionHTTP.thisSession.uiSG("user_name"));
          action.put("request", request);
          action.put("syncID", syncID.toUpperCase());
          action.put("clientid", this.thisSessionHTTP.thisSession.uiSG("clientid"));
          action.put("need_response", "true");
          queue.addElement(action);
          action = UserTools.waitResponse(action, 60);
        } else {
          SyncTools.addJournalEntry(syncID.toUpperCase(), request.getProperty("item_path"), "CONFLICT", "", "");
        } 
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        response = String.valueOf(response) + "<result><response_status>success</response_status></result>";
        return writeResponse(response);
      } 
      String error_msg = "Sync was not found for your current folder.";
      write_command_http("HTTP/1.1 404 OK");
      write_command_http("Cache-Control: no-store");
      write_command_http("Content-Type: application/binary");
      this.thisSessionHTTP.write_standard_headers();
      write_command_http("Content-Length: " + error_msg.length() + '\002');
      write_command_http("");
      write_command_http(error_msg);
      return true;
    } 
    if (command.equalsIgnoreCase("purgeSync") && ServerStatus.siIG("enterprise_level") > 0) {
      Properties item = this.thisSessionHTTP.thisSession.uVFS.get_item(String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + request.getProperty("path"));
      String syncID = Common.parseSyncPart(item.getProperty("privs"), "name");
      if (syncID != null) {
        request.put("syncID", syncID.toUpperCase());
        if (Common.System2.get("crushftp.dmz.queue") != null) {
          Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
          Properties action = new Properties();
          action.put("type", "GET:SYNC");
          action.put("id", Common.makeBoundary());
          action.put("username", this.thisSessionHTTP.thisSession.uiSG("user_name"));
          action.put("request", request);
          action.put("syncID", syncID.toUpperCase());
          action.put("root_dir", this.thisSessionHTTP.thisSession.SG("root_dir"));
          action.put("clientid", this.thisSessionHTTP.thisSession.uiSG("clientid"));
          action.put("need_response", "true");
          queue.addElement(action);
          action = UserTools.waitResponse(action, 60);
        } else {
          AdminControls.purgeSync(request, this.thisSessionHTTP.thisSession.uVFS, this.thisSessionHTTP.thisSession.SG("root_dir"));
        } 
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        response = String.valueOf(response) + "<result><response_status>success</response_status></result>";
        return writeResponse(response);
      } 
      String error_msg = "Sync was not found for your current folder.";
      write_command_http("HTTP/1.1 404 OK");
      write_command_http("Cache-Control: no-store");
      write_command_http("Content-Type: application/binary");
      this.thisSessionHTTP.write_standard_headers();
      write_command_http("Content-Length: " + error_msg.length() + '\002');
      write_command_http("");
      write_command_http(error_msg);
      return true;
    } 
    if (command.equalsIgnoreCase("downloadCrushFTPDrive") && ServerStatus.siIG("enterprise_level") > 0) {
      Common.urlDecodePost(request);
      String appname = Common.dots(request.getProperty("appname", "CrushFTPDrive"));
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      FileInputStream fin = null;
      if ((new File(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/" + appname + "/" + appname + ".jnlp")).exists()) {
        fin = new FileInputStream(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/" + appname + "/" + appname + ".jnlp");
      } else {
        fin = new FileInputStream(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/CrushFTPDrive/CrushFTPDrive.jnlp");
      } 
      Common.streamCopier(fin, baos, false);
      String xml = new String(baos.toByteArray(), "UTF8");
      String line = xml.substring(xml.indexOf("<argument>") + "<argument>".length(), xml.indexOf("</argument>"));
      line = Common.replace_str(line, "%base_url%", this.thisSessionHTTP.getBaseUrl(this.thisSessionHTTP.hostString));
      line = Common.replace_str(line, "%appname%", appname);
      line = Common.replace_str(line, "%user_name%", this.thisSessionHTTP.thisSession.uiSG("user_name"));
      line = Common.url_encode(line);
      xml = String.valueOf(xml.substring(0, xml.indexOf("<argument>") + "<argument>".length())) + line + xml.substring(xml.indexOf("</argument>"));
      xml = Common.replace_str(xml, "%base_url%", this.thisSessionHTTP.getBaseUrl(this.thisSessionHTTP.hostString));
      xml = Common.replace_str(xml, "%appname%", appname);
      xml = Common.replace_str(xml, "%user_name%", this.thisSessionHTTP.thisSession.uiSG("user_name"));
      write_command_http("HTTP/1.1 200 OK");
      write_command_http("Cache-Control: no-store");
      write_command_http("Content-Type: application/x-java-jnlp-file");
      this.thisSessionHTTP.write_standard_headers();
      byte[] utf8 = xml.getBytes("UTF8");
      write_command_http("Content-Length: " + utf8.length);
      write_command_http("Content-Disposition: attachment; filename=\"" + Common.replace_str(Common.url_decode(appname), "\r", "_") + ".jnlp\"");
      write_command_http("X-UA-Compatible: chrome=1");
      write_command_http("");
      this.thisSessionHTTP.original_os.write(utf8);
      this.thisSessionHTTP.original_os.flush();
      return true;
    } 
    if (command.equalsIgnoreCase("downloadSyncAgent") && ServerStatus.siIG("enterprise_level") > 0) {
      Common.urlDecodePost(request);
      String appname = Common.dots(request.getProperty("appname", "CrushSync"));
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      FileInputStream fin = null;
      if ((new File(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/" + appname + "/" + appname + ".jnlp")).exists()) {
        fin = new FileInputStream(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/" + appname + "/" + appname + ".jnlp");
      } else {
        fin = new FileInputStream(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/CrushSync/CrushSync.jnlp");
      } 
      Common.streamCopier(fin, baos, false);
      String pass = request.getProperty("current_password", "").trim();
      if (!pass.equals(""))
        pass = ServerStatus.thisObj.common_code.encode_pass(pass, "DES", ""); 
      String xml = new String(baos.toByteArray(), "UTF8");
      String line = xml.substring(xml.indexOf("<argument>") + "<argument>".length(), xml.indexOf("</argument>"));
      line = Common.replace_str(line, "%base_url%", this.thisSessionHTTP.getBaseUrl(this.thisSessionHTTP.hostString));
      line = Common.replace_str(line, "%appname%", appname);
      line = Common.replace_str(line, "%user_name%", this.thisSessionHTTP.thisSession.uiSG("user_name"));
      line = Common.replace_str(line, "%user_pass%", pass);
      line = Common.replace_str(line, "%admin_pass%", ServerStatus.thisObj.common_code.encode_pass(request.getProperty("admin_pass"), "DES", ""));
      line = Common.url_encode(line);
      xml = String.valueOf(xml.substring(0, xml.indexOf("<argument>") + "<argument>".length())) + line + xml.substring(xml.indexOf("</argument>"));
      xml = Common.replace_str(xml, "%base_url%", this.thisSessionHTTP.getBaseUrl(this.thisSessionHTTP.hostString));
      xml = Common.replace_str(xml, "%appname%", appname);
      xml = Common.replace_str(xml, "%user_name%", this.thisSessionHTTP.thisSession.uiSG("user_name"));
      xml = Common.replace_str(xml, "%user_pass%", pass);
      xml = Common.replace_str(xml, "%admin_pass%", ServerStatus.thisObj.common_code.encode_pass(request.getProperty("admin_pass"), "DES", ""));
      write_command_http("HTTP/1.1 200 OK");
      write_command_http("Cache-Control: no-store");
      write_command_http("Content-Type: application/x-java-jnlp-file");
      this.thisSessionHTTP.write_standard_headers();
      byte[] utf8 = xml.getBytes("UTF8");
      write_command_http("Content-Length: " + utf8.length);
      write_command_http("Content-Disposition: attachment; filename=\"" + Common.replace_str(Common.url_decode(appname), "\r", "_") + ".jnlp\"");
      write_command_http("X-UA-Compatible: chrome=1");
      write_command_http("");
      this.thisSessionHTTP.original_os.write(utf8);
      this.thisSessionHTTP.original_os.flush();
      return true;
    } 
    if (command.equalsIgnoreCase("downloadAttachmentRedirector") && ServerStatus.siIG("enterprise_level") > 0) {
      Common.urlDecodePost(request);
      String appname = Common.dots(request.getProperty("appname", "AttachmentRedirector"));
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      FileInputStream fin = null;
      if ((new File(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/" + appname + "/" + appname + ".jnlp")).exists()) {
        fin = new FileInputStream(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/" + appname + "/" + appname + ".jnlp");
      } else {
        fin = new FileInputStream(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/AttachmentRedirector/AttachmentRedirector.jnlp");
      } 
      Common.streamCopier(fin, baos, false);
      String xml = new String(baos.toByteArray(), "UTF8");
      String line = xml.substring(xml.indexOf("<argument>") + "<argument>".length(), xml.indexOf("</argument>"));
      line = Common.replace_str(line, "%base_url%", this.thisSessionHTTP.getBaseUrl(this.thisSessionHTTP.hostString));
      line = Common.replace_str(line, "%appname%", appname);
      line = Common.replace_str(line, "%user_name%", this.thisSessionHTTP.thisSession.uiSG("user_name"));
      line = Common.url_encode(line);
      xml = String.valueOf(xml.substring(0, xml.indexOf("<argument>") + "<argument>".length())) + line + xml.substring(xml.indexOf("</argument>"));
      xml = Common.replace_str(xml, "%base_url%", this.thisSessionHTTP.getBaseUrl(this.thisSessionHTTP.hostString));
      xml = Common.replace_str(xml, "%appname%", appname);
      xml = Common.replace_str(xml, "%user_name%", this.thisSessionHTTP.thisSession.uiSG("user_name"));
      write_command_http("HTTP/1.1 200 OK");
      write_command_http("Cache-Control: no-store");
      write_command_http("Content-Type: application/x-java-jnlp-file");
      this.thisSessionHTTP.write_standard_headers();
      byte[] utf8 = xml.getBytes("UTF8");
      write_command_http("Content-Length: " + utf8.length);
      write_command_http("Content-Disposition: attachment; filename=\"" + Common.replace_str(Common.url_decode(appname), "\r", "_") + ".jnlp\"");
      write_command_http("X-UA-Compatible: chrome=1");
      write_command_http("");
      this.thisSessionHTTP.original_os.write(utf8);
      this.thisSessionHTTP.original_os.flush();
      return true;
    } 
    if (command.equalsIgnoreCase("getCrushSyncPrefs") && ServerStatus.siIG("enterprise_level") > 0) {
      Vector v = new Vector();
      request.put("user_name", this.thisSessionHTTP.thisSession.uiSG("user_name"));
      request.put("user_ip", this.thisSessionHTTP.thisSession.uiSG("user_ip"));
      request.put("clientid", this.thisSessionHTTP.thisSession.uiSG("clientid"));
      request.put("site", site);
      if (Integer.parseInt(Common.replace_str(request.getProperty("version", "0"), ".", "")) >= Integer.parseInt(Common.replace_str(SyncTools.minSyncVersion, ".", ""))) {
        v = (Vector)SyncTools.getSyncPrefs(request);
      } else {
        Properties p = new Properties();
        p.put("UPDATE_REQUIRED", "true");
        p.put("MIN_VERSION", SyncTools.minSyncVersion);
        v.addElement(p);
      } 
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(v);
      oos.close();
      write_command_http("HTTP/1.1 200 OK");
      write_command_http("Cache-Control: no-store");
      write_command_http("Content-Type: application/binary");
      this.thisSessionHTTP.write_standard_headers();
      write_command_http("Content-Length: " + baos.size());
      write_command_http("");
      this.thisSessionHTTP.original_os.write(baos.toByteArray());
      this.thisSessionHTTP.original_os.flush();
      return true;
    } 
    if (command.equalsIgnoreCase("syncCommandResult") && ServerStatus.siIG("enterprise_level") > 0) {
      request.put("user_name", this.thisSessionHTTP.thisSession.uiSG("user_name"));
      request.put("user_ip", this.thisSessionHTTP.thisSession.uiSG("user_ip"));
      request.put("clientid", this.thisSessionHTTP.thisSession.uiSG("clientid"));
      request.put("site", site);
      SyncTools.sendSyncResult(request);
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      response = String.valueOf(response) + "<result><response_status>success</response_status></result>";
      return writeResponse(response);
    } 
    if (command.equalsIgnoreCase("getSyncAgents") && ServerStatus.siIG("enterprise_level") > 0) {
      if (last_activity_time != null)
        this.thisSessionHTTP.thisSession.put("last_activity", last_activity_time); 
      Common.urlDecodePost(request);
      Vector v = new Vector();
      request.put("user_name", this.thisSessionHTTP.thisSession.uiSG("user_name"));
      request.put("user_ip", this.thisSessionHTTP.thisSession.uiSG("user_ip"));
      request.put("clientid", this.thisSessionHTTP.thisSession.uiSG("clientid"));
      request.put("site", site);
      v = (Vector)SyncTools.getSyncAgents(request);
      return writeResponse(AdminControls.buildXML(v, "agents", "Success"));
    } 
    if (command.equalsIgnoreCase("sendSyncCommand") && ServerStatus.siIG("enterprise_level") > 0) {
      if (last_activity_time != null)
        this.thisSessionHTTP.thisSession.put("last_activity", last_activity_time); 
      Common.urlDecodePost(request);
      Object o = new Properties();
      request.put("user_name", this.thisSessionHTTP.thisSession.uiSG("user_name"));
      request.put("user_ip", this.thisSessionHTTP.thisSession.uiSG("user_ip"));
      request.put("clientid", this.thisSessionHTTP.thisSession.uiSG("clientid"));
      request.put("site", site);
      o = SyncTools.sendSyncCommand(request);
      return writeResponse(AdminControls.buildXML(o, "response", "Success"));
    } 
    if (command.equalsIgnoreCase("getQuota")) {
      String path = Common.url_decode(request.getProperty("path", "").replace('+', ' '));
      if (path.startsWith("///") && !path.startsWith("////"))
        path = "/" + path; 
      if (!path.startsWith("/"))
        path = "/" + path; 
      if (!path.endsWith("/"))
        path = String.valueOf(path) + "/"; 
      String the_dir = Common.dots(path);
      if (!the_dir.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
        the_dir = String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + the_dir.substring(1); 
      String quota = "-12345";
      if (Common.System2.get("crushftp.dmz.queue") != null) {
        Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
        Properties action = new Properties();
        action.put("type", "GET:QUOTA");
        action.put("id", Common.makeBoundary());
        action.put("username", this.thisSessionHTTP.thisSession.uiSG("user_name"));
        action.put("password", this.thisSessionHTTP.thisSession.uiSG("current_password"));
        action.put("request", request);
        String the_dir2 = the_dir;
        if (the_dir2.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
          the_dir2 = the_dir2.substring(this.thisSessionHTTP.thisSession.SG("root_dir").length() - 1); 
        action.put("the_dir", the_dir2);
        action.put("clientid", this.thisSessionHTTP.thisSession.uiSG("clientid"));
        action.put("need_response", "true");
        queue.addElement(action);
        action = UserTools.waitResponse(action, 300);
        quota = action.remove("object_response").toString().trim();
      } else {
        quota = String.valueOf(this.thisSessionHTTP.thisSession.get_quota(the_dir)) + ":" + SessionCrush.get_quota(the_dir, this.thisSessionHTTP.thisSession.uVFS, this.thisSessionHTTP.thisSession.SG("parent_quota_dir"), this.thisSessionHTTP.thisSession.quotaDelta, this.thisSessionHTTP.thisSession, false);
      } 
      return writeResponse((new StringBuffer(String.valueOf(quota))).toString());
    } 
    if (command.equalsIgnoreCase("getMd5s")) {
      String path_str = null;
      try {
        path_str = new String(Base64.decode(request.getProperty("path")));
      } catch (Exception e) {
        path_str = Common.dots(Common.url_decode(request.getProperty("path")));
      } 
      if (Common.System2.get("crushftp.dmz.queue") != null) {
        Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
        Properties action = new Properties();
        action.put("type", "PUT:GETMD5S");
        action.put("id", Common.makeBoundary());
        action.put("username", this.thisSessionHTTP.thisSession.uiSG("user_name"));
        Properties root_item = this.thisSessionHTTP.thisSession.uVFS.get_item(this.thisSessionHTTP.thisSession.SG("root_dir"));
        GenericClient c = this.thisSessionHTTP.thisSession.uVFS.getClient(root_item);
        action.put("crushAuth", c.getConfig("crushAuth"));
        this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
        action.put("user_ip", this.thisSessionHTTP.thisSession.uiSG("user_ip"));
        action.put("request", request);
        action.put("clientid", this.thisSessionHTTP.thisSession.uiSG("clientid"));
        action.put("need_response", "true");
        queue.addElement(action);
        action = UserTools.waitResponse(action, 600);
        this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
        String response = action.remove("object_response").toString();
        return writeResponse(response);
      } 
      Properties item = this.thisSessionHTTP.thisSession.uVFS.get_item(String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + path_str);
      Vector md5s = new Vector();
      Properties request2 = request;
      null.multiThreadMd5 md5Worker = new null.multiThreadMd5(this, item, md5s, request2);
      if (item != null)
        Worker.startWorker(md5Worker); 
      write_command_http("HTTP/1.1 200 OK");
      write_command_http("Cache-Control: no-store");
      write_command_http("Content-Type: text/plain");
      write_command_http("Transfer-Encoding: chunked");
      this.thisSessionHTTP.write_standard_headers();
      write_command_http("");
      while ((item != null && md5Worker.isActive()) || md5s.size() > 0) {
        if (md5s.size() > 0) {
          String md5 = md5s.remove(0).toString();
          write_command_http(Long.toHexString((md5.length() + 2)));
          write_command_http(md5);
          write_command_http("");
          continue;
        } 
        Thread.sleep(100L);
      } 
      write_command_http("0");
      write_command_http("");
      return true;
    } 
    if (command.equalsIgnoreCase("messageForm") && !request.containsKey("registration_username") && !request.containsKey("meta_registration_username")) {
      Enumeration keys = request.keys();
      Properties metaInfo = new Properties();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement().toString();
        if (key.toUpperCase().startsWith("META_")) {
          String val = request.getProperty(key);
          key = key.substring("META_".length());
          metaInfo.put(key, val);
          if (key.toUpperCase().startsWith("GLOBAL_")) {
            if (ServerStatus.thisObj.server_info.get("global_variables") == null)
              ServerStatus.thisObj.server_info.put("global_variables", new Properties()); 
            Properties global_variables = (Properties)ServerStatus.thisObj.server_info.get("global_variables");
            global_variables.put(key, val);
            continue;
          } 
          if (key.toUpperCase().startsWith("USER_INFO_"))
            this.thisSessionHTTP.thisSession.user_info.put(key, val); 
        } 
      } 
      ServerStatus.thisObj.statTools.insertMetaInfo(this.thisSessionHTTP.thisSession.uiSG("SESSION_RID"), metaInfo, "0");
      this.thisSessionHTTP.thisSession.add_log("[" + this.thisSessionHTTP.thisSession.uiSG("user_number") + ":" + this.thisSessionHTTP.thisSession.uiSG("user_name") + ":" + this.thisSessionHTTP.thisSession.uiSG("user_ip") + "] DATA: *messageForm confirmed:" + metaInfo + "*", "HTTP");
      Properties fileItem = new Properties();
      fileItem.put("url", "ftp://127.0.0.1:56789/");
      fileItem.put("the_file_path", "/");
      fileItem.put("the_file_size", "1");
      fileItem.put("event_name", "welcome");
      fileItem.put("the_file_name", "welcome");
      fileItem.putAll(metaInfo);
      Properties info = this.thisSessionHTTP.thisSession.do_event5("WELCOME", fileItem);
      if (info == null)
        info = new Properties(); 
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n<commandResult><response>Success</response><new_job_id>" + info.getProperty("new_job_id", "") + "</new_job_id></commandResult>";
      return writeResponse(response);
    } 
    if (command.equalsIgnoreCase("selfRegistration") || request.containsKey("registration_username") || request.containsKey("meta_registration_username")) {
      String response = "";
      if (Common.System2.get("crushftp.dmz.queue") != null) {
        Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
        Properties action = new Properties();
        action.put("type", "PUT:SELFREGISTRATION");
        action.put("id", Common.makeBoundary());
        action.put("username", this.thisSessionHTTP.thisSession.uiSG("user_name"));
        Properties root_item = this.thisSessionHTTP.thisSession.uVFS.get_item(this.thisSessionHTTP.thisSession.SG("root_dir"));
        GenericClient c = this.thisSessionHTTP.thisSession.uVFS.getClient(root_item);
        action.put("crushAuth", c.getConfig("crushAuth"));
        this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
        action.put("user_ip", this.thisSessionHTTP.thisSession.uiSG("user_ip"));
        action.put("request", request);
        action.put("req_id", req_id);
        action.put("clientid", this.thisSessionHTTP.thisSession.uiSG("clientid"));
        action.put("need_response", "true");
        queue.addElement(action);
        action = UserTools.waitResponse(action, 300);
        this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
        response = action.remove("object_response").toString();
      } else {
        response = selfRegistration(request, this.thisSessionHTTP.thisSession, req_id);
      } 
      return writeResponse("<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n<commandResult><response>" + response + "</response></commandResult>");
    } 
    if (command.equalsIgnoreCase("setMetaInfo")) {
      Enumeration keys = request.keys();
      Properties metaInfo = new Properties();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement().toString();
        if (key.toUpperCase().startsWith("META_")) {
          String val = request.getProperty(key);
          key = key.substring("META_".length());
          metaInfo.put(key, val);
          if (key.toUpperCase().startsWith("GLOBAL_")) {
            if (ServerStatus.thisObj.server_info.get("global_variables") == null)
              ServerStatus.thisObj.server_info.put("global_variables", new Properties()); 
            Properties global_variables = (Properties)ServerStatus.thisObj.server_info.get("global_variables");
            global_variables.put(key, val);
            continue;
          } 
          if (key.toUpperCase().startsWith("USER_INFO_"))
            this.thisSessionHTTP.thisSession.user_info.put(key, val); 
        } 
      } 
      this.thisSessionHTTP.thisSession.put("last_metaInfo", metaInfo);
      this.thisSessionHTTP.thisSession.add_log("[" + this.thisSessionHTTP.thisSession.uiSG("user_number") + ":" + this.thisSessionHTTP.thisSession.uiSG("user_name") + ":" + this.thisSessionHTTP.thisSession.uiSG("user_ip") + "] DATA: *metaInfo confirmed:" + metaInfo + "*", "HTTP");
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n<commandResult><response>Success</response></commandResult>";
      return writeResponse(response);
    } 
    if (command.equalsIgnoreCase("setServerItem")) {
      if (AdminControls.checkRole(command, site))
        return writeResponse(AdminControls.buildXML(null, "response", AdminControls.setServerItem(request, site))); 
      return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
    } 
    if (command.equalsIgnoreCase("getUser")) {
      if (AdminControls.checkRole(command, site))
        return writeResponse(AdminControls.buildXML(AdminControls.getUser(request, site, this.thisSessionHTTP.thisSession), "user_items", "OK"), true, 200, false, false, true); 
      return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
    } 
    if (command.equalsIgnoreCase("setUserItem") && AdminControls.checkRole(command, site)) {
      String data_item = AdminControls.buildXML(null, "response", AdminControls.setUserItem(request, this.thisSessionHTTP.thisSession, site));
      if (data_item.indexOf("<password>") >= 0 && data_item.indexOf("</password>") >= 0) {
        data_item = String.valueOf(data_item.substring(0, data_item.indexOf("<password>") + "<password>".length())) + "*******" + data_item.substring(data_item.indexOf("</password>"));
      } else if (data_item.indexOf("current_password") >= 0) {
        data_item = String.valueOf(data_item.substring(0, data_item.indexOf(":") + 1)) + "*******";
      } else if (data_item.toUpperCase().indexOf("PASSWORD") >= 0) {
        data_item = String.valueOf(data_item.substring(0, data_item.indexOf(":") + 1)) + "*******";
      } 
      return writeResponse(data_item);
    } 
    if (command.equalsIgnoreCase("setUserItem")) {
      String status = "OK";
      try {
        if (site.indexOf("(USER_ADMIN)") < 0)
          throw new Exception("Access Denied."); 
        try {
          Properties info = null;
          if (!this.thisSessionHTTP.thisSession.containsKey("user_admin_info"))
            AdminControls.getUserList(request, site, this.thisSessionHTTP.thisSession); 
          info = (Properties)this.thisSessionHTTP.thisSession.get("user_admin_info");
          Vector list = (Vector)info.get("list");
          boolean writeGroupsInheritance = false;
          if (request.getProperty("xmlItem", "").equals("groups")) {
            String groupName = this.thisSessionHTTP.thisSession.SG("admin_group_name");
            if (groupName.equals("") || groupName.equals("admin_group_name"))
              groupName = this.thisSessionHTTP.thisSession.uiSG("user_name"); 
            Properties groups = (Properties)Common.readXMLObjectError(new ByteArrayInputStream(Common.url_decode(request.getProperty("groups").replace('+', ' ')).getBytes("UTF8")));
            if (groups == null)
              groups = new Properties(); 
            Properties real_groups = UserTools.getGroups(request.getProperty("serverGroup"));
            Enumeration keys = real_groups.keys();
            while (keys.hasMoreElements()) {
              String key = keys.nextElement().toString();
              if (key.toUpperCase().startsWith(String.valueOf(groupName.toUpperCase()) + "_"))
                real_groups.remove(key); 
            } 
            keys = groups.keys();
            while (keys.hasMoreElements()) {
              String key = keys.nextElement().toString();
              real_groups.put(String.valueOf(groupName) + "_" + key, groups.remove(key));
            } 
            UserTools.writeGroups(request.getProperty("serverGroup"), real_groups);
          } else if (request.getProperty("xmlItem", "").equals("inheritance")) {
            String groupName = this.thisSessionHTTP.thisSession.SG("admin_group_name");
            if (groupName.equals("") || groupName.equals("admin_group_name"))
              groupName = this.thisSessionHTTP.thisSession.uiSG("user_name"); 
            Properties inheritance = (Properties)Common.readXMLObjectError(new ByteArrayInputStream(Common.url_decode(request.getProperty("inheritance").replace('+', ' ')).getBytes("UTF8")));
            if (inheritance == null)
              inheritance = new Properties(); 
            Enumeration keys = inheritance.keys();
            while (keys.hasMoreElements()) {
              String key = keys.nextElement().toString();
              if (list.indexOf(key) < 0)
                inheritance.remove(key); 
              Vector sub_users = (Vector)inheritance.get(key);
              for (int x = sub_users.size() - 1; x >= 0; x--) {
                String sub_user = sub_users.elementAt(x).toString();
                if (list.indexOf(sub_user) < 0)
                  sub_users.remove(x); 
              } 
              if (sub_users.size() == 0 || !sub_users.elementAt(0).toString().equals(groupName))
                sub_users.insertElementAt((E)groupName, 0); 
            } 
            Properties real_inheritance = UserTools.getInheritance(request.getProperty("serverGroup"));
            keys = real_inheritance.keys();
            while (keys.hasMoreElements()) {
              String key = keys.nextElement().toString();
              if (list.indexOf(key) >= 0 && inheritance.containsKey(key))
                real_inheritance.remove(key); 
            } 
            real_inheritance.putAll(inheritance);
            UserTools.writeInheritance(request.getProperty("serverGroup"), real_inheritance);
          } else if (request.getProperty("xmlItem", "").equals("user")) {
            if (request.getProperty("serverGroup_original", "").equals("extra_vfs"))
              request.put("serverGroup", "extra_vfs"); 
            if (!request.containsKey("usernames"))
              request.put("usernames", request.getProperty("username", "")); 
            String[] usernames = Common.url_decode(request.getProperty("usernames").replace('+', ' ')).split(";");
            for (int x = 0; x < usernames.length; x++) {
              String username = usernames[x].trim();
              if (!username.equals("")) {
                Properties groups = UserTools.getGroups(request.getProperty("serverGroup"));
                if (groups == null)
                  groups = new Properties(); 
                String groupName = this.thisSessionHTTP.thisSession.SG("admin_group_name");
                if (groupName.equals("") || groupName.equals("admin_group_name"))
                  groupName = this.thisSessionHTTP.thisSession.uiSG("user_name"); 
                Vector group = (Vector)groups.get(groupName);
                Vector pendingSelfRegistration = (Vector)groups.get("pendingSelfRegistration");
                if (group == null)
                  group = new Vector(); 
                groups.put(groupName, group);
                if (pendingSelfRegistration == null) {
                  pendingSelfRegistration = new Vector();
                  groups.put("pendingSelfRegistration", pendingSelfRegistration);
                } 
                if (username.equalsIgnoreCase(groupName))
                  throw new Exception("You cannot edit this user, it is only for reference."); 
                Properties inheritance = UserTools.getInheritance(request.getProperty("serverGroup"));
                if (request.getProperty("data_action").equals("delete")) {
                  if (!username.equalsIgnoreCase("default") || info.getProperty("default_edittable", "false").equals("true")) {
                    if (list.indexOf(username) < 0)
                      throw new Exception("Username " + username + " not found."); 
                    UserTools.deleteUser(request.getProperty("serverGroup"), username);
                    group.remove(username);
                    pendingSelfRegistration.remove(username);
                    inheritance.remove(username);
                    UserTools.writeGroups(request.getProperty("serverGroup"), groups);
                    UserTools.writeInheritance(request.getProperty("serverGroup"), inheritance);
                  } 
                } else {
                  Properties new_user = (Properties)Common.readXMLObject(new ByteArrayInputStream(Common.url_decode(request.getProperty("user").replace('+', ' ')).getBytes("UTF8")));
                  new_user.put("userVersion", "6");
                  Vector linked_vfs = (Vector)new_user.get("linked_vfs");
                  for (int xx = 0; linked_vfs != null && xx < linked_vfs.size(); xx++) {
                    String linked_user = linked_vfs.elementAt(xx).toString().trim();
                    if (list.indexOf(linked_user) < 0)
                      throw new Exception("Linked_VFS username " + linked_user + " not found."); 
                  } 
                  if (new_user.containsKey("password")) {
                    String pass = new_user.getProperty("password", "");
                    if (!pass.startsWith("SHA:") && !pass.startsWith("SHA512:") && !pass.startsWith("SHA3:") && !pass.startsWith("MD5:") && !pass.startsWith("CRYPT3:") && !pass.startsWith("BCRYPT:") && !pass.startsWith("MD5CRYPT:") && !pass.startsWith("SHA512CRYPT:")) {
                      pass = ServerStatus.thisObj.common_code.encode_pass(pass, ServerStatus.SG("password_encryption"), new_user.getProperty("salt", ""));
                      new_user.put("password", pass);
                    } 
                  } 
                  VFS tempVFS = AdminControls.processVFSSubmission(request, username, site, this.thisSessionHTTP.thisSession);
                  Properties user = UserTools.ut.getUser(request.getProperty("serverGroup"), username, false);
                  if (user != null && user.getProperty("username", "").equalsIgnoreCase("TEMPLATE"))
                    user = null; 
                  if (request.getProperty("data_action").equals("update")) {
                    if (list.indexOf(username) < 0)
                      throw new Exception("Username " + username + " not found."); 
                    Common.updateObject(new_user, user);
                    new_user = user;
                    if (pendingSelfRegistration.indexOf(username) >= 0) {
                      pendingSelfRegistration.remove(username);
                      if (group.indexOf(username) < 0)
                        group.addElement(username); 
                      Vector vv = new Vector();
                      vv.addElement(groupName);
                      inheritance.put(username, vv);
                      writeGroupsInheritance = true;
                    } 
                  } else if (user != null) {
                    if (request.getProperty("serverGroup_original", "").equals("extra_vfs")) {
                      if (list.indexOf(username.substring(0, username.lastIndexOf("~"))) < 0)
                        throw new Exception("Username " + username + " not found."); 
                    } else if (list.indexOf(username) < 0) {
                      throw new Exception("Username " + username + " not found.");
                    } 
                    if (username.trim().equalsIgnoreCase("default") && !info.getProperty("default_edittable", "false").equals("true"))
                      throw new Exception("This user is for reference only.  You cannot edit this user."); 
                    if (pendingSelfRegistration.indexOf(username) >= 0) {
                      pendingSelfRegistration.remove(username);
                      if (group.indexOf(username) < 0)
                        group.addElement(username); 
                      Vector vv = new Vector();
                      vv.addElement(groupName);
                      inheritance.put(username, vv);
                      writeGroupsInheritance = true;
                    } 
                  } else if (!request.getProperty("serverGroup_original", "").equals("extra_vfs")) {
                    Vector vv = new Vector();
                    vv.addElement(groupName);
                    inheritance.put(username, vv);
                    pendingSelfRegistration.remove(username);
                    if (group.indexOf(username) < 0)
                      group.addElement(username); 
                    writeGroupsInheritance = true;
                    if (list.indexOf(username) < 0)
                      list.addElement(username); 
                  } 
                  if (UserTools.testLimitedAdminAccess(new_user, groupName, request.getProperty("serverGroup"))) {
                    UserTools.writeUser(request.getProperty("serverGroup"), username, new_user);
                    if (request.containsKey("vfs_items") || request.containsKey("permissions")) {
                      Properties virtual = tempVFS.homes.elementAt(0);
                      Enumeration keys = virtual.keys();
                      boolean ok = true;
                      while (keys.hasMoreElements() && ok) {
                        String key = keys.nextElement().toString();
                        if (key.equals("vfs_permissions_object"))
                          continue; 
                        Properties vfs_item = (Properties)virtual.get(key);
                        if (vfs_item.getProperty("type").equalsIgnoreCase("DIR"))
                          continue; 
                        ok = UserTools.testLimitedAdminAccess(vfs_item.get("vItems"), groupName, request.getProperty("serverGroup_backup", request.getProperty("serverGroup")));
                        if (!ok)
                          Log.log("HTTP_SERVER", 0, new Date() + ":User " + this.thisSessionHTTP.thisSession.uiSG("user_name") + " Violated Security Constraint for a USER_ADMIN:" + groupName); 
                      } 
                      if (ok)
                        UserTools.writeVFS(request.getProperty("serverGroup"), username, tempVFS); 
                    } 
                    if (writeGroupsInheritance) {
                      UserTools.writeGroups(request.getProperty("serverGroup"), groups);
                      UserTools.writeInheritance(request.getProperty("serverGroup"), inheritance);
                    } 
                    UserTools.ut.getUser(request.getProperty("serverGroup"), username, true);
                    UserTools.ut.forceMemoryReload(username);
                  } else {
                    String msg = ":User " + this.thisSessionHTTP.thisSession.uiSG("user_name") + " Violated Security Constraint for a USER_ADMIN:" + groupName;
                    Log.log("HTTP_SERVER", 0, new Date() + msg);
                    throw new Exception(msg);
                  } 
                } 
              } 
            } 
          } else {
            status = "OK";
          } 
        } catch (Exception e) {
          status = "FAILURE:" + e.toString();
        } 
      } catch (Exception e) {
        Log.log("HTTP_SERVER", 1, e);
        status = e.toString();
      } 
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      response = String.valueOf(response) + "<result><response_status>" + status + "</response_status></result>";
      return writeResponse(response);
    } 
    if (command.equals("refreshUser")) {
      if (AdminControls.checkRole(command, site))
        UserTools.ut.forceMemoryReload(request.getProperty("username")); 
      return writeResponse("<commandResult><response>Success</response></commandResult>");
    } 
    if (command.equals("getUserXMLListing")) {
      if (request.getProperty("serverGroup_original", "").equals("extra_vfs"))
        request.put("serverGroup", "extra_vfs"); 
      if (AdminControls.checkRole(command, site)) {
        System.getProperties().put("crushftp.isTestCall", request.getProperty("isTestCall", "false"));
        try {
          Properties listingProp = AdminControls.getUserXMLListing(request, site, this.thisSessionHTTP.thisSession);
          String altList = "";
          String response = "";
          if (!listingProp.getProperty("error", "").equals("")) {
            response = Common.getXMLString(listingProp, "listingInfo", null);
          } else if (request.getProperty("format", "").equalsIgnoreCase("JSON")) {
            altList = AgentUI.getJsonList(listingProp, ServerStatus.BG("exif_listings"), false);
          } else if (request.getProperty("format", "").equalsIgnoreCase("STAT")) {
            altList = AgentUI.getStatList(listingProp);
          } 
          try {
            response = Common.getXMLString(listingProp, "listingInfo", null);
          } catch (Exception e) {
            Log.log("HTTP_SERVER", 1, e);
          } 
          if (!altList.equals(""))
            response = String.valueOf(response.substring(0, response.indexOf("</privs>") + "</privs>".length())) + altList + response.substring(response.indexOf("</privs>") + "</privs>".length()); 
          return writeResponse(response);
        } finally {
          System.getProperties().put("crushftp.isTestCall", "false");
        } 
      } 
      return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
    } 
    if (command.equalsIgnoreCase("getUserList")) {
      if (AdminControls.checkRole(command, site))
        return writeResponse(AdminControls.buildXML(AdminControls.getUserList(request, site, this.thisSessionHTTP.thisSession), "user_list", "OK")); 
      return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
    } 
    if (command.equalsIgnoreCase("getUserXML")) {
      if (AdminControls.checkRole(command, site) || (site.indexOf("(REPORT_EDIT)") >= 0 && request.getProperty("xmlItem", "").equals("group")))
        return writeResponse(AdminControls.buildXML(AdminControls.getUserXML(request, site, this.thisSessionHTTP.thisSession), "result_item", "OK")); 
      return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
    } 
    if (command.equals("getAdminXMLListing")) {
      Properties listingProp = new Properties();
      if (!request.getProperty("get_from_agentid", "").equals("")) {
        Vector items = new Vector();
        String str1 = Common.url_decode(request.getProperty("path", "").replace('+', ' '));
        request.put("admin_password", Common.url_decode(request.getProperty("admin_password").replace('+', ' ')));
        if (str1.startsWith("///") && !str1.startsWith("////"))
          str1 = "/" + str1; 
        if (!str1.startsWith("/"))
          str1 = "/" + str1; 
        if (!str1.endsWith("/"))
          str1 = String.valueOf(str1) + "/"; 
        str1 = Common.dots(str1);
        request.put("path", str1);
        request.put("user_name", this.thisSessionHTTP.thisSession.uiSG("user_name"));
        request.put("user_ip", this.thisSessionHTTP.thisSession.uiSG("user_ip"));
        request.put("clientid", this.thisSessionHTTP.thisSession.uiSG("clientid"));
        request.put("site", site);
        try {
          items = (Vector)SyncTools.getSyncXMLList(request);
        } catch (Exception e) {
          Log.log("HTTP_SERVER", 1, e);
        } 
        listingProp.put("privs", "(read)(view)");
        listingProp.put("path", str1);
        for (int x = 0; x < items.size(); x++) {
          Properties p = items.elementAt(x);
          VRL vrl = new VRL(p.getProperty("url"));
          p.put("href_path", vrl.getPath());
        } 
        listingProp.put("listing", items);
        String altList = "";
        if (listingProp != null && request.getProperty("format", "").equalsIgnoreCase("JSON")) {
          altList = AgentUI.getJsonList(listingProp, ServerStatus.BG("exif_listings"), false);
        } else if (listingProp != null && request.getProperty("format", "").equalsIgnoreCase("STAT")) {
          altList = AgentUI.getStatList(listingProp);
        } 
        String str2 = "";
        try {
          str2 = Common.getXMLString(listingProp, "listingInfo", null);
        } catch (Exception e) {
          Log.log("HTTP_SERVER", 1, e);
        } 
        if (!altList.equals(""))
          str2 = String.valueOf(str2.substring(0, str2.indexOf("</privs>") + "</privs>".length())) + altList + str2.substring(str2.indexOf("</privs>") + "</privs>".length()); 
        return writeResponse(str2);
      } 
      if (site.indexOf("(CONNECT)") >= 0 || site.indexOf("(USER_EDIT)") >= 0 || site.indexOf("(SHARE_EDIT)") >= 0 || site.indexOf("(LOG_ACCESS)") >= 0) {
        request.put("path", Common.url_decode(request.getProperty("path").replace('+', ' ')));
        listingProp = AdminControls.getAdminXMLListing(request, this.thisSessionHTTP.thisSession, site);
        String altList = "";
        if (listingProp != null && request.getProperty("format", "").equalsIgnoreCase("JSON")) {
          altList = AgentUI.getJsonList(listingProp, ServerStatus.BG("exif_listings"), false);
        } else if (listingProp != null && request.getProperty("format", "").equalsIgnoreCase("STAT")) {
          altList = AgentUI.getStatList(listingProp);
        } 
        String str1 = "";
        try {
          str1 = Common.getXMLString(listingProp, "listingInfo", null);
        } catch (Exception e) {
          Log.log("HTTP_SERVER", 1, e);
        } 
        if (!altList.equals(""))
          str1 = String.valueOf(str1.substring(0, str1.indexOf("</privs>") + "</privs>".length())) + altList + str1.substring(str1.indexOf("</privs>") + "</privs>".length()); 
        return writeResponse(str1);
      } 
      String groupName = this.thisSessionHTTP.thisSession.SG("admin_group_name");
      if (groupName.equals(""))
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>"); 
      if (groupName.equals("admin_group_name"))
        groupName = this.thisSessionHTTP.thisSession.uiSG("user_name"); 
      String path = Common.url_decode(request.getProperty("path", "").replace('+', ' '));
      if (path.startsWith("///") && !path.startsWith("////"))
        path = "/" + path; 
      if (!path.startsWith("/"))
        path = "/" + path; 
      if (!path.endsWith("/"))
        path = String.valueOf(path) + "/"; 
      path = Common.dots(path);
      request.put("command", "getXMLListing");
      String response = getXmlListingResponse(groupName, request, path, true, UserTools.ut.getVFS(this.thisSessionHTTP.thisSession.uiSG("listen_ip_port"), groupName));
      return writeResponse(response, request.getProperty("format", "").equalsIgnoreCase("JSONOBJ"));
    } 
    if (command.equalsIgnoreCase("adminAction")) {
      Object result = AdminControls.adminAction(Common.urlDecodePost(request), site);
      if (result instanceof String && result.toString().equals(""))
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>"); 
      return writeResponse(AdminControls.buildXML(result, "result_item", "OK"));
    } 
    if (command.equalsIgnoreCase("checkForUpdate")) {
      if (AdminControls.checkRole(command, site))
        return writeResponse(AdminControls.checkForUpdate(request, site)); 
      return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
    } 
    if (command.equalsIgnoreCase("updateNow")) {
      if (AdminControls.checkRole(command, site))
        return writeResponse(AdminControls.updateNow(request, site)); 
      return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
    } 
    if (command.equalsIgnoreCase("updateWebNow")) {
      if (AdminControls.checkRole(command, site))
        return writeResponse(AdminControls.updateWebNow(request, site)); 
      return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
    } 
    if (command.equalsIgnoreCase("updateNowProgress")) {
      if (AdminControls.checkRole(command, site))
        return writeResponse(AdminControls.updateNowProgress(request, site)); 
      return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
    } 
    if (command.equalsIgnoreCase("cancelUpdateProgress")) {
      if (AdminControls.checkRole(command, site))
        return writeResponse(AdminControls.cancelUpdateProgress(request, site)); 
      return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
    } 
    if (command.equalsIgnoreCase("dumpStack")) {
      if (AdminControls.checkRole(command, site))
        return writeResponse(AdminControls.dumpStack(request, site)); 
      return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
    } 
    if (command.equalsIgnoreCase("dumpHeap")) {
      if (AdminControls.checkRole(command, site))
        return writeResponse(AdminControls.dumpHeap(request, site)); 
      return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
    } 
    if (command.equalsIgnoreCase("setMaxServerMemory")) {
      if (AdminControls.checkRole(command, site)) {
        Common.update_service_memory(Integer.parseInt(request.getProperty("memory", "512")));
      } else {
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
    } else if (command.equalsIgnoreCase("restartProcess")) {
      if (AdminControls.checkRole(command, site)) {
        ServerStatus.thisObj.restart_crushftp();
      } else {
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
    } else {
      if (command.equalsIgnoreCase("system.gc")) {
        if (AdminControls.checkRole(command, site)) {
          System.gc();
          return writeResponse("<commandResult><response>Success</response></commandResult>");
        } 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("pgpGenerateKeyPair")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.pgpGenerateKeyPair(request, site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("runReport")) {
        if (AdminControls.checkRole(command, site)) {
          String xml = AdminControls.runReport(request, site);
          write_command_http("HTTP/1.1 200 OK");
          write_command_http("Cache-Control: no-store");
          write_command_http("Content-Type: text/" + (request.getProperty("export", "false").equals("true") ? "plain" : "html") + ";charset=utf-8");
          this.thisSessionHTTP.write_standard_headers();
          byte[] utf8 = xml.getBytes("UTF8");
          write_command_http("Content-Length: " + utf8.length);
          String appname = String.valueOf(request.getProperty("reportName")) + (request.getProperty("export", "false").equals("true") ? ".csv" : ".html");
          if (request.getProperty("saveReport", "").equalsIgnoreCase("true"))
            write_command_http("Content-Disposition: attachment; filename=\"" + Common.replace_str(Common.url_decode(appname), "\r", "_") + "\""); 
          write_command_http("X-UA-Compatible: chrome=1");
          write_command_http("");
          this.thisSessionHTTP.original_os.write(utf8);
          this.thisSessionHTTP.original_os.flush();
          return true;
        } 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("testReportSchedule")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.testReportSchedule(request, site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("testJobSchedule")) {
        Common.urlDecodePost(request);
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.testJobSchedule(request, site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("testSMTP")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.testSMTP(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("importUsers")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.importUsers(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("sendPassEmail")) {
        if (AdminControls.checkRole(command, site)) {
          Common.urlDecodePost(request);
          if (site.indexOf("(CONNECT)") < 0)
            request.put("serverGroup", this.thisSessionHTTP.thisSession.server_item.getProperty("linkedServer")); 
          String username = request.getProperty("user_name", "");
          Properties info = (Properties)this.thisSessionHTTP.thisSession.get("user_admin_info");
          if (site.indexOf("(CONNECT)") < 0 && site.indexOf("(USER_EDIT)") < 0) {
            Vector list = (Vector)info.get("list");
            if (list.indexOf(username) < 0)
              throw new Exception("Username " + username + " not found."); 
          } 
          return writeResponse(AdminControls.sendPassEmail(request, site));
        } 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("sendEventEmail")) {
        Common.urlDecodePost(request);
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.sendEventEmail(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("getTempAccounts")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.getTempAccounts(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("addTempAccount")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.addTempAccount(request, site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("removeTempAccount")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.removeTempAccount(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("getTempAccountFiles")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.getTempAccountFiles(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("removeTempAccountFile")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.removeTempAccountFile(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("addTempAccountFile")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.addTempAccountFile(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("deleteReplication")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.deleteReplication(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("migrateUsersVFS")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.migrateUsersVFS(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("convertUsers")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.convertUsers(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("generateSSL")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.generateSSL(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("generateCSR")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.generateCSR(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("importReply")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.importReply(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("testKeystore")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.testKeystore(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("listSSL")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.listSSL(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("deleteSSL")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.deleteSSL(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("renameSSL")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.renameSSL(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("exportSSL")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.exportSSL(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("addPrivateSSL")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.addPrivateSSL(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("addPublicSSL")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.addPublicSSL(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("telnetSocket")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.telnetSocket(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("testDB")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.testDB(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("testQuery")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.testQuery(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("pluginMethodCall")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.pluginMethodCall(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("convertXMLSQLUsers")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.convertXMLSQLUsers(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("registerCrushFTP")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.registerCrushFTP(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("restorePrefs")) {
        if (AdminControls.checkRole(command, site))
          return writeResponse(AdminControls.restorePrefs(Common.urlDecodePost(request), site)); 
        return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>");
      } 
      if (command.equalsIgnoreCase("upload") || request.getProperty("the_action", "").equalsIgnoreCase("STOR")) {
        boolean result = false;
        int code = 200;
        String response = "<commandResult><response>";
        Properties activeUpload = (Properties)Common.System2.get("crushftp.activeUpload.info" + this.thisSessionHTTP.thisSession.getId());
        if (activeUpload != null && activeUpload.getProperty("fileupload", "").indexOf("ERROR:") >= 0) {
          response = String.valueOf(response) + activeUpload.getProperty("fileupload", "");
          code = 500;
        } else {
          response = String.valueOf(response) + "Success";
        } 
        this.thisSessionHTTP.done = true;
        response = String.valueOf(response) + "</response><last_md5>" + activeUpload.getProperty("last_md5", "") + "</last_md5></commandResult>";
        result = writeResponse(response, true, code, true, false, true);
        if (this.thisSessionHTTP.chunked)
          Thread.sleep(1000L); 
        return result;
      } 
      if (command.equalsIgnoreCase("upload_0_byte")) {
        if (Common.System2.get("crushftp.dmz.queue") != null) {
          String str = request.getProperty("path");
          if (!str.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
            str = String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + str.substring(1); 
          Properties item = this.thisSessionHTTP.thisSession.uVFS.get_item(Common.all_but_last(str));
          Properties config = new Properties();
          config.put("protocol", "HTTP");
          URLConnection urlc = null;
          int loops = 0;
          while (loops++ < 100) {
            urlc = URLConnection.openConnection(new VRL(Common.getBaseUrl(item.getProperty("url"))), config);
            urlc.setRequestMethod("POST");
            GenericClient c = this.thisSessionHTTP.thisSession.uVFS.getClient(item);
            urlc.setRequestProperty("Cookie", "CrushAuth=" + c.getConfig("crushAuth") + ";");
            urlc.setUseCaches(false);
            urlc.setDoOutput(true);
            byte[] b = ("command=upload_0_byte&c2f=" + c.getConfig("crushAuth", "").substring(c.getConfig("crushAuth", "").length() - 4) + "&path=" + request.getProperty("path", "")).getBytes("UTF8");
            urlc.setLength(b.length);
            OutputStream pout = urlc.getOutputStream();
            pout.write(b);
            pout.flush();
            if (urlc.getResponseCode() == 302) {
              c.setConfig("error", "Logged out.");
              urlc.disconnect();
              this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
              Thread.sleep(100L);
              continue;
            } 
            this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
            break;
          } 
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          Common.streamCopier(urlc.getInputStream(), baos, false, true, true);
          urlc.disconnect();
          return writeResponse(new String(baos.toByteArray()));
        } 
        String error = "";
        String the_dir = Common.url_decode(request.getProperty("path", ""));
        if (the_dir.equals("/"))
          the_dir = this.thisSessionHTTP.thisSession.SG("root_dir"); 
        if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(this.thisSessionHTTP.thisSession.SG("root_dir").toUpperCase()))
          the_dir = String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + the_dir.substring(1); 
        if (this.thisSessionHTTP.thisSession.check_access_privs(the_dir, "STOR")) {
          Properties item = this.thisSessionHTTP.thisSession.uVFS.get_item_parent(the_dir);
          GenericClient c = this.thisSessionHTTP.thisSession.uVFS.getClient(item);
          try {
            c.upload((new VRL(item.getProperty("url"))).getPath(), 0L, true, true).close();
          } catch (Exception e) {
            error = String.valueOf(error) + e;
          } 
          c.close();
          this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
        } 
        String response = "<commandResult><response>" + (error.equals("") ? "OK" : error) + "</response></commandResult>";
        return writeResponse(response);
      } 
      if (command.equalsIgnoreCase("blockUploads")) {
        this.thisSessionHTTP.thisSession.put("blockUploads", "true");
        String response = "<commandResult><response>OK</response></commandResult>";
        return writeResponse(response);
      } 
      if (command.equalsIgnoreCase("unblockUploads")) {
        this.thisSessionHTTP.thisSession.put("blockUploads", "false");
        String response = "<commandResult><response>OK</response></commandResult>";
        return writeResponse(response);
      } 
      if (command.equalsIgnoreCase("getLastUploadError")) {
        String response = "<commandResult><response>";
        Properties activeUpload = (Properties)Common.System2.get("crushftp.activeUpload.info" + this.thisSessionHTTP.thisSession.getId());
        if (activeUpload.getProperty("fileupload", "").indexOf("ERROR:") >= 0) {
          response = String.valueOf(response) + activeUpload.getProperty("fileupload", "");
        } else {
          response = String.valueOf(response) + "Success";
        } 
        this.thisSessionHTTP.done = true;
        response = String.valueOf(response) + "</response></commandResult>";
        boolean result = writeResponse(response);
        if (this.thisSessionHTTP.chunked)
          Thread.sleep(1000L); 
        return result;
      } 
      if (command.equalsIgnoreCase("getPreview")) {
        if (Common.System2.get("crushftp.dmz.queue") != null) {
          String the_dir = request.getProperty("path");
          if (!the_dir.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
            the_dir = String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + the_dir.substring(1); 
          Properties item = this.thisSessionHTTP.thisSession.uVFS.get_item(the_dir);
          Properties config = new Properties();
          config.put("protocol", "HTTP");
          URLConnection urlc = null;
          int loops = 0;
          while (loops++ < 100) {
            urlc = URLConnection.openConnection(new VRL(Common.getBaseUrl(item.getProperty("url"))), config);
            urlc.setRequestMethod("POST");
            GenericClient c = this.thisSessionHTTP.thisSession.uVFS.getClient(item);
            urlc.setRequestProperty("Cookie", "CrushAuth=" + c.getConfig("crushAuth") + ";");
            urlc.setUseCaches(false);
            urlc.setDoOutput(true);
            byte[] arrayOfByte = ("command=getPreview&c2f=" + c.getConfig("crushAuth", "").substring(c.getConfig("crushAuth", "").length() - 4) + "&size=" + request.getProperty("size", "1") + "&frame=" + request.getProperty("frame", "1") + "&object_type=" + request.getProperty("object_type", "image") + "&download=" + request.getProperty("download", "false") + "&path=" + Common.url_encode(request.getProperty("path"))).getBytes("UTF8");
            urlc.setLength(arrayOfByte.length);
            OutputStream pout = urlc.getOutputStream();
            pout.write(arrayOfByte);
            pout.flush();
            if (urlc.getResponseCode() == 302) {
              c.setConfig("error", "Logged out.");
              urlc.disconnect();
              this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
              Thread.sleep(100L);
              continue;
            } 
            this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
            break;
          } 
          write_command_http("HTTP/1.1 " + urlc.getResponseCode() + " OK");
          write_command_http("Content-Type: " + urlc.getHeaderField("CONTENT-TYPE"));
          write_command_http("Content-Length: " + urlc.getHeaderField("CONTENT-LENGTH"));
          this.thisSessionHTTP.write_standard_headers();
          write_command_http("X-UA-Compatible: chrome=1");
          write_command_http("");
          InputStream pin = urlc.getInputStream();
          byte[] b = new byte[32768];
          int max_len = Integer.parseInt(urlc.getHeaderField("CONTENT-LENGTH").trim());
          int bytesRead = 0;
          while (max_len > 0) {
            if (b.length > max_len)
              b = new byte[max_len]; 
            bytesRead = pin.read(b);
            if (bytesRead > 0) {
              max_len -= bytesRead;
              this.thisSessionHTTP.original_os.write(b, 0, bytesRead);
            } 
          } 
          this.thisSessionHTTP.original_os.flush();
          pin.close();
          urlc.disconnect();
        } else {
          request.put("path", Common.replace_str(request.getProperty("path"), "+", "%2B"));
          Common.urlDecodePost(request);
          Vector paths_raw = new Vector();
          Vector paths_updated = new Vector();
          String[] path_str = request.getProperty("path").split(";");
          String path = "";
          for (int x = 0; x < path_str.length; x++) {
            path = path_str[x];
            if (!path.trim().equals("")) {
              paths_raw.addElement(path);
              this.thisSessionHTTP.setupCurrentDir(path);
              Log.log("HTTP_SERVER", 2, "getPreview:" + path);
              if (path.startsWith("@")) {
                path = path.substring(1);
                if (path.indexOf("..") >= 0)
                  path = ""; 
                if (!path.equals("folder")) {
                  if (path.indexOf(".") >= 0)
                    path = path.substring(path.lastIndexOf(".") + 1); 
                  if ((new File(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/images/mimetypes/" + path + ".png")).exists()) {
                    path = "/WebInterface/images/mimetypes/" + path + ".png";
                  } else {
                    path = "file";
                  } 
                } 
                if (!path.startsWith("/"))
                  path = "/WebInterface/images/" + path + ".png/p1/" + request.getProperty("size") + ".png"; 
              } else if (request.getProperty("object_type", "image").equals("exif")) {
                Log.log("HTTP_SERVER", 2, "getPreview2:" + this.thisSessionHTTP.pwd());
                Properties item = this.thisSessionHTTP.thisSession.uVFS.get_item(this.thisSessionHTTP.pwd());
                path = "/WebInterface/images" + SearchHandler.getPreviewPath(item, "1", 1);
                path = String.valueOf(Common.all_but_last(Common.all_but_last(path))) + "info.xml";
                Log.log("HTTP_SERVER", 2, "getPreview3:" + path);
              } else {
                Log.log("HTTP_SERVER", 2, "getPreview2:" + this.thisSessionHTTP.pwd());
                Properties item = this.thisSessionHTTP.thisSession.uVFS.get_item(this.thisSessionHTTP.pwd());
                path = "/WebInterface/images" + SearchHandler.getPreviewPath(item, request.getProperty("size"), Integer.parseInt(request.getProperty("frame", "1")));
                Log.log("HTTP_SERVER", 2, "getPreview3:" + path);
              } 
              paths_updated.addElement(path);
            } 
          } 
          if (paths_updated.size() > 0)
            path = paths_updated.elementAt(0).toString(); 
          String s = "GET " + path + " HTTP/1.1";
          this.thisSessionHTTP.headers.setElementAt(s, 0);
          String downloadFilename = null;
          if (request.getProperty("download", "false").equals("true")) {
            downloadFilename = Common.last(request.getProperty("path"));
            if (downloadFilename.indexOf(".") >= 0)
              downloadFilename = String.valueOf(downloadFilename.substring(0, downloadFilename.lastIndexOf("."))) + ".jpg"; 
          } 
          if (paths_updated.size() == 1) {
            ServerSessionHTTPWI.serveFile(this.thisSessionHTTP, this.thisSessionHTTP.headers, this.thisSessionHTTP.original_os, false, downloadFilename);
          } else {
            write_command_http("HTTP/1.1 200 OK");
            int validSecs = 1;
            write_command_http("Cache-Control: post-check=" + validSecs + ",pre-check=" + (validSecs * 10));
            write_command_http("Content-Type: application/zip");
            this.thisSessionHTTP.write_standard_headers();
            write_command_http("Content-Disposition: attachment; filename=\"images.zip\"");
            write_command_http("X-UA-Compatible: chrome=1");
            write_command_http("Connection: close");
            write_command_http("");
            ZipArchiveOutputStream zaous = new ZipArchiveOutputStream(this.thisSessionHTTP.original_os);
            for (int i = 0; i < paths_raw.size(); i++) {
              path = paths_raw.elementAt(i).toString();
              downloadFilename = Common.last(path);
              if (downloadFilename.indexOf(".") >= 0)
                downloadFilename = String.valueOf(downloadFilename.substring(0, downloadFilename.lastIndexOf("."))) + ".jpg"; 
              ZipArchiveEntry zae = new ZipArchiveEntry(downloadFilename);
              zaous.putArchiveEntry((ArchiveEntry)zae);
              this.thisSessionHTTP.setupCurrentDir(path);
              Properties item = this.thisSessionHTTP.thisSession.uVFS.get_item(this.thisSessionHTTP.pwd());
              File f = new File(String.valueOf(ServerStatus.SG("previews_path")) + SearchHandler.getPreviewPath(item, request.getProperty("size"), Integer.parseInt(request.getProperty("frame", "1"))));
              Common.copyStreams(new FileInputStream(f), zaous, true, false);
              zaous.closeArchiveEntry();
            } 
            zaous.finish();
            zaous.close();
          } 
        } 
        return true;
      } 
      if (command.equalsIgnoreCase("setPreview")) {
        Common.urlDecodePost(request);
        Properties customForm = null;
        boolean found_exif_key = false;
        if (request.getProperty("exif_key", "").startsWith("crushftp_")) {
          Vector customForms = (Vector)ServerStatus.server_settings.get("CustomForms");
          if (customForms != null) {
            for (int i = 0; i < customForms.size(); i++) {
              Properties p = customForms.elementAt(i);
              if (p.getProperty("name", "").equals(request.getProperty("form_id", ""))) {
                customForm = p;
                break;
              } 
            } 
            if (customForm != null) {
              if (!customForm.containsKey("entries"))
                customForm.put("entries", new Vector()); 
              Vector entries = (Vector)customForm.get("entries");
              for (int j = 0; j < entries.size(); j++) {
                Properties p = entries.elementAt(j);
                if (!p.getProperty("type").trim().equals("label")) {
                  if (p.getProperty("name").equals(request.getProperty("exif_key")))
                    found_exif_key = true; 
                  if (p.getProperty("value").indexOf("{user_name}") >= 0 && request.getProperty("exif_key").indexOf("crushftp_locked_user") >= 0)
                    request.put("exif_val", ServerStatus.change_vars_to_values_static(p.getProperty("value", ""), this.thisSessionHTTP.thisSession.user, this.thisSessionHTTP.thisSession.user_info, this.thisSessionHTTP.thisSession)); 
                } 
              } 
            } 
          } 
        } 
        String error_message = "No preview converters found.";
        String[] paths = request.getProperty("path").replace('>', '_').replace('<', '_').split(";");
        for (int x = 0; x < paths.length; x++) {
          String path = paths[x];
          this.thisSessionHTTP.setupCurrentDir(path);
          Properties item = this.thisSessionHTTP.thisSession.uVFS.get_item(this.thisSessionHTTP.pwd());
          String srcFile = Common.dots((new VRL(item.getProperty("url"))).getPath());
          if (!this.thisSessionHTTP.thisSession.check_access_privs(path, "STOR"))
            error_message = "You need upload permissions to edit exif tags on a file:" + request.getProperty("path") + "\r\n"; 
          if (request.getProperty("object_type", "image").equals("exif")) {
            for (int xx = 0; xx < ServerStatus.thisObj.previewWorkers.size(); xx++) {
              PreviewWorker preview = ServerStatus.thisObj.previewWorkers.elementAt(xx);
              if (preview.prefs.getProperty("preview_enabled", "false").equalsIgnoreCase("true") && preview.checkExtension(Common.last(path), item) && !request.getProperty("exif_key").startsWith("crushftp_")) {
                preview.setExifInfo(srcFile, PreviewWorker.getDestPath2(String.valueOf(item.getProperty("url")) + "/p1/"), request.getProperty("exif_key"), request.getProperty("exif_val"));
                error_message = "Success";
                break;
              } 
            } 
            if (ServerStatus.BG("v8_beta") && found_exif_key) {
              Properties metaInfo = PreviewWorker.getMetaInfo(PreviewWorker.getDestPath2(String.valueOf(item.getProperty("url")) + "/p1/"));
              if (metaInfo.getProperty("crushftp_locked_user", "").equals("") || metaInfo.getProperty("crushftp_locked_user", "").equalsIgnoreCase(this.thisSessionHTTP.thisSession.uiSG("user_name")) || this.thisSessionHTTP.thisSession.SG("site").indexOf("(CONNECT)") >= 0) {
                metaInfo.put(request.getProperty("exif_key"), request.getProperty("exif_val"));
                PreviewWorker.setMetaInfo(PreviewWorker.getDestPath2(String.valueOf(item.getProperty("url")) + "/p1/"), metaInfo);
                error_message = "Success";
                break;
              } 
              error_message = "FAILURE: Item already locked.";
              break;
            } 
          } 
          if (!error_message.equals("Success"))
            break; 
        } 
        String response = "<commandResult><response>" + error_message + "</response></commandResult>";
        return writeResponse(response);
      } 
      if (command.equalsIgnoreCase("siteCommand")) {
        Common.urlDecodePost(request);
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        this.thisSessionHTTP.thisSession.uiPUT("the_command", "SITE");
        String the_command_data = request.getProperty("siteCommand", "");
        this.thisSessionHTTP.thisSession.uiPUT("the_command_data", the_command_data);
        String responseText = null;
        if (the_command_data.equalsIgnoreCase("BLOCK_UPLOADS") || the_command_data.startsWith("ABOR")) {
          responseText = "blocked";
          if (SharedSessionReplicated.send_queues.size() > 0) {
            Properties pp = new Properties();
            pp.put("the_command_data", the_command_data);
            pp.put("CrushAuth", this.thisSessionHTTP.thisSession.getId());
            SharedSessionReplicated.send(Common.makeBoundary(), "crushftp.server.ServerSessionAjax.doFileAbortBlock", "info", pp);
          } 
          if (!the_command_data.startsWith("ABOR"))
            this.thisSessionHTTP.thisSession.doFileAbortBlock(the_command_data, true); 
        } else {
          GenericClient c = this.thisSessionHTTP.thisSession.uVFS.getClient(this.thisSessionHTTP.thisSession.uVFS.get_item(this.thisSessionHTTP.thisSession.SG("root_dir")));
          try {
            VRL vrl = new VRL(this.thisSessionHTTP.thisSession.uVFS.get_item(this.thisSessionHTTP.thisSession.SG("root_dir")).getProperty("url"));
            if (ServerStatus.BG("site_ack"))
              responseText = c.doCommand("SITE " + the_command_data); 
            if (responseText == null) {
              Properties activeUpload = (Properties)Common.System2.get("crushftp.activeUpload.info" + this.thisSessionHTTP.thisSession.getId());
              Enumeration keys = activeUpload.keys();
              responseText = "";
              while (keys.hasMoreElements()) {
                String key = keys.nextElement().toString();
                String val = activeUpload.remove(key).toString();
                responseText = String.valueOf(responseText) + key + ":" + val + "\r\n";
              } 
            } else {
              responseText = responseText.substring(4);
            } 
            this.thisSessionHTTP.thisSession.uVFS.reset();
          } finally {
            c = this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
          } 
        } 
        response = String.valueOf(response) + "<commandResult><response>" + responseText + "</response></commandResult>";
        return writeResponse(response);
      } 
      if (command.equalsIgnoreCase("sitePlugin") && site.toUpperCase().indexOf("(SITE_PLUGIN)") >= 0) {
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        this.thisSessionHTTP.thisSession.uiPUT("the_command", "SITE");
        String the_command_data = request.getProperty("siteCommand", "");
        this.thisSessionHTTP.thisSession.uiPUT("the_command_data", the_command_data);
        Properties fileItem = new Properties();
        fileItem.put("url", "ftp://127.0.0.1:56789/");
        fileItem.put("the_file_path", request.getProperty("the_file_path", "/"));
        fileItem.put("the_file_size", "1");
        fileItem.put("event_name", request.getProperty("event"));
        fileItem.put("the_file_name", request.getProperty("the_file_name", "none"));
        fileItem.putAll(request);
        this.thisSessionHTTP.thisSession.do_event5("SITE", fileItem);
        String responseText = fileItem.getProperty("execute_log", "No Result");
        response = String.valueOf(response) + "<commandResult><response>" + responseText + "</response></commandResult>";
        return writeResponse(response);
      } 
      if (command.equalsIgnoreCase("getUploadStatus")) {
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        Properties activeUpload = (Properties)Common.System2.get("crushftp.activeUpload.info" + this.thisSessionHTTP.thisSession.getId());
        String responseText = "";
        if (activeUpload != null) {
          responseText = activeUpload.getProperty(request.getProperty("itemName"));
          if (responseText != null)
            if (responseText.toUpperCase().startsWith("DONE:")) {
              activeUpload.remove(request.getProperty("itemName"));
            } else if (!responseText.toUpperCase().startsWith("PROGRESS:")) {
              if (!responseText.startsWith("ERROR:"))
                responseText = "ERROR:" + responseText; 
            }  
        } 
        response = String.valueOf(response) + "<commandResult><response>" + responseText + "</response></commandResult>";
        return writeResponse(response);
      } 
      if (command.equalsIgnoreCase("getTime")) {
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        response = String.valueOf(response) + "<commandResult><response>" + System.currentTimeMillis() + "</response></commandResult>";
        return writeResponse(response);
      } 
      if (command.equalsIgnoreCase("lookup_form_field")) {
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        Vector customForms = (Vector)ServerStatus.server_settings.get("CustomForms");
        if (customForms != null) {
          Properties customForm = null;
          for (int x = 0; x < customForms.size(); x++) {
            Properties p = customForms.elementAt(x);
            if (p.getProperty("name", "").equals(request.getProperty("form_name"))) {
              customForm = p;
              break;
            } 
          } 
          if (customForm != null) {
            Properties entry = null;
            if (!customForm.containsKey("entries"))
              customForm.put("entries", new Vector()); 
            Vector entries = (Vector)customForm.get("entries");
            for (int xx = 0; xx < entries.size(); xx++) {
              Properties p = entries.elementAt(xx);
              if (!p.getProperty("name", "").trim().equals(request.getProperty("from_element_name"))) {
                entry = p;
                break;
              } 
            } 
            if (entry != null) {
              Vector search_entries = new Vector();
              String q = request.getProperty("q", "");
              if (entry.getProperty("lookup_type", "text").trim().equals(""))
                entry.put("lookup_type", "text"); 
              if (entry.getProperty("lookup_type", "text").trim().equals("text")) {
                File userText = new File(String.valueOf(System.getProperty("crushftp.prefs")) + "forms/" + Common.dots(String.valueOf(request.getProperty("form_element_name")) + "_" + this.thisSessionHTTP.thisSession.uiSG("user_name") + ".txt"));
                Log.log("HTTP_SERVER", 2, "Looking for lookup file:" + userText);
                if (userText.exists()) {
                  BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(userText)));
                  String data = "";
                  while ((data = br.readLine()) != null) {
                    Log.log("HTTP_SERVER", 2, userText + ":Checking data:" + data);
                    String key = data.trim();
                    String val = key;
                    Properties p = new Properties();
                    if (data.indexOf(":") >= 0) {
                      key = data.split(":")[0].trim();
                      if ((data.split(":")).length == 1) {
                        val = "";
                      } else {
                        val = data.split(":")[1].trim();
                      } 
                    } 
                    p.put("key", key);
                    p.put("val", val);
                    if (key.toUpperCase().indexOf(q.toUpperCase()) >= 0)
                      search_entries.addElement(p); 
                  } 
                  br.close();
                } 
                Properties groups = UserTools.getGroups(this.thisSessionHTTP.thisSession.server_item.getProperty("linkedServer"));
                Enumeration keys = groups.keys();
                while (keys.hasMoreElements()) {
                  String group_name = keys.nextElement().toString();
                  Vector v = (Vector)groups.get(group_name);
                  if (v.indexOf(this.thisSessionHTTP.thisSession.uiSG("user_name")) >= 0) {
                    File groupText = new File(String.valueOf(System.getProperty("crushftp.prefs")) + "forms/" + Common.dots(String.valueOf(request.getProperty("form_element_name")) + "_" + group_name + ".txt"));
                    Log.log("HTTP_SERVER", 2, "Looking for lookup file:" + groupText);
                    if (groupText.exists()) {
                      BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(groupText)));
                      String data = "";
                      while ((data = br.readLine()) != null) {
                        Log.log("HTTP_SERVER", 2, groupText + ":Checking data:" + data);
                        String key = data.trim();
                        String val = key;
                        Properties p = new Properties();
                        if (data.indexOf(":") >= 0) {
                          key = data.split(":")[0].trim();
                          if ((data.split(":")).length == 1) {
                            val = "";
                          } else {
                            val = data.split(":")[1].trim();
                          } 
                        } 
                        key = String.valueOf(group_name) + " - " + key;
                        p.put("key", key);
                        p.put("val", val);
                        if (key.toUpperCase().indexOf(q.toUpperCase()) >= 0)
                          search_entries.addElement(p); 
                      } 
                      br.close();
                    } 
                  } 
                } 
                File globalText = new File(String.valueOf(System.getProperty("crushftp.prefs")) + "forms/" + Common.dots(String.valueOf(request.getProperty("form_element_name")) + ".txt"));
                Log.log("HTTP_SERVER", 2, "Looking for lookup file:" + globalText);
                if (globalText.exists() && search_entries.size() == 0) {
                  BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(globalText)));
                  String data = "";
                  while ((data = br.readLine()) != null) {
                    Log.log("HTTP_SERVER", 2, globalText + ":Checking data:" + data);
                    String key = data.trim();
                    String val = key;
                    Properties p = new Properties();
                    if (data.indexOf(":") >= 0) {
                      key = data.split(":")[0].trim();
                      if ((data.split(":")).length == 1) {
                        val = "";
                      } else {
                        val = data.split(":")[1].trim();
                      } 
                    } 
                    p.put("key", key);
                    p.put("val", val);
                    if (key.toUpperCase().indexOf(q.toUpperCase()) >= 0)
                      search_entries.addElement(p); 
                  } 
                  br.close();
                } 
              } else if (entry.getProperty("lookup_type", "").trim().equals("task")) {
                Properties item = (Properties)entry.clone();
                item.put("url", "file://" + entry.getProperty("name") + "/" + q);
                item.put("the_file_name", q);
                item.put("the_file_path", entry.getProperty("name"));
                Vector items = new Vector();
                items.addElement(item);
                Properties event = new Properties();
                event.put("event_plugin_list", entry.getProperty("entry_plugin", ""));
                event.put("name", "FormPlugin:" + request.getProperty("form_name") + ":" + entry.getProperty("entry_plugin", ""));
                Properties info = new Properties();
                boolean async = ServerStatus.BG("event_asynch");
                ServerStatus.server_settings.put("event_async", "false");
                info = ServerStatus.thisObj.events6.doEventPlugin(info, event, this.thisSessionHTTP.thisSession, items);
                ServerStatus.server_settings.put("event_async", (new StringBuffer(String.valueOf(async))).toString());
                items = (Vector)info.get("newItems");
                for (int j = items.size() - 1; j >= 0; j--) {
                  Properties p = items.elementAt(j);
                  if (!p.getProperty("key", "").toUpperCase().startsWith(q.toUpperCase()))
                    items.removeElementAt(j); 
                } 
                search_entries.addAll(items);
              } else if (entry.getProperty("lookup_type", "").trim().equals("plugin")) {
                try {
                  Object parent = Common.getPlugin(request.getProperty("pluginName"), null, request.getProperty("pluginSubItem", ""));
                  if (parent == null && request.getProperty("pluginSubItem", "").equals(""))
                    parent = Common.getPlugin(request.getProperty("pluginName"), null, "false"); 
                  Method method = parent.getClass().getMethod(request.getProperty("method", "lookupList"), new Class[] { (new Properties()).getClass() });
                  Object o = method.invoke(parent, new Object[] { request });
                  response = Common.getXMLString(o, "list", null);
                } catch (Exception ee) {
                  Log.log("HTTP_SERVER", 1, ee);
                  response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString());
                } 
              } 
              Common.do_sort(search_entries, "name", "key");
              StringBuffer r = new StringBuffer();
              r.append("[\r\n");
              for (int i = 0; i < search_entries.size(); i++) {
                Properties p = search_entries.elementAt(i);
                if (i > 0)
                  r.append(",\r\n"); 
                r.append("\t{\"id\":\"" + p.getProperty("val") + "\",\"name\":\"" + p.getProperty("key") + "\"}");
              } 
              r.append("]\r\n");
              response = r.toString();
            } 
          } 
        } 
        return writeResponse(response, true);
      } 
      if (command.equalsIgnoreCase("batchComplete")) {
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        response = String.valueOf(response) + "<commandResult><response>SUCCESS</response></commandResult>";
        this.thisSessionHTTP.thisSession.do_event5("BATCH_COMPLETE", null);
        return writeResponse(response);
      } 
      if (command.equalsIgnoreCase("decrypt")) {
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        String[] names = Common.url_decode(request.getProperty("names")).replace('>', '_').replace('<', '_').split("\r\n");
        String error_message = "";
        for (int x = 0; x < names.length; x++) {
          try {
            String the_dir = names[x];
            if (!the_dir.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
              the_dir = String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + the_dir.substring(1); 
            this.thisSessionHTTP.cd(the_dir);
            if (!this.thisSessionHTTP.thisSession.check_access_privs(this.thisSessionHTTP.pwd(), "RETR") || !this.thisSessionHTTP.thisSession.check_access_privs(this.thisSessionHTTP.pwd(), "STOR")) {
              error_message = String.valueOf(error_message) + "ERROR:You need download, and upload permissions to decrypt a file:" + the_dir + "\r\n";
            } else {
              this.thisSessionHTTP.thisSession.uiPUT("the_command", "DECRYPT");
              this.thisSessionHTTP.thisSession.uiPUT("the_command_data", this.thisSessionHTTP.pwd());
              Properties item = this.thisSessionHTTP.thisSession.uVFS.get_item(the_dir);
              VRL vrl = new VRL(item.getProperty("url"));
              GenericClient c1 = this.thisSessionHTTP.thisSession.uVFS.getClient(item);
              GenericClient c2 = this.thisSessionHTTP.thisSession.uVFS.getClient(item);
              try {
                InputStream in = c1.download(vrl.getPath(), 0L, -1L, true);
                if (!this.thisSessionHTTP.thisSession.user.getProperty("fileEncryptionKey", "").equals("") || !this.thisSessionHTTP.thisSession.user.getProperty("fileDecryptionKey", "").equals("")) {
                  in = Common.getDecryptedStream(in, this.thisSessionHTTP.thisSession.user.getProperty("fileEncryptionKey", ""), this.thisSessionHTTP.thisSession.user.getProperty("fileDecryptionKey", ""), this.thisSessionHTTP.thisSession.user.getProperty("fileDecryptionKeyPass", ""));
                } else if (!ServerStatus.SG("fileEncryptionKey").equals("") || ServerStatus.BG("fileDecryption")) {
                  in = Common.getDecryptedStream(in, ServerStatus.SG("fileEncryptionKey"), ServerStatus.SG("fileDecryptionKey"), ServerStatus.SG("fileDecryptionKeyPass"));
                } 
                OutputStream out = c2.upload(String.valueOf(vrl.getPath()) + ".decrypting", 0L, true, true);
                Common.copyStreams(in, out, true, true);
                c1.rename(String.valueOf(vrl.getPath()) + ".decrypting", vrl.getPath());
                error_message = String.valueOf(error_message) + the_dir + " decrypted.";
              } finally {
                c1 = this.thisSessionHTTP.thisSession.uVFS.releaseClient(c1);
                c2 = this.thisSessionHTTP.thisSession.uVFS.releaseClient(c2);
              } 
            } 
          } catch (Exception e) {
            Log.log("HTTP_SERVER", 0, e);
            error_message = String.valueOf(error_message) + "ERROR:" + e.getMessage() + "\r\n";
          } 
        } 
        response = String.valueOf(response) + "<commandResult><response>" + error_message + "</response></commandResult>";
        this.thisSessionHTTP.thisSession.uVFS.reset();
        return writeResponse(response.replace('%', ' '));
      } 
      if (command.equalsIgnoreCase("encrypt")) {
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        String[] names = Common.url_decode(request.getProperty("names")).replace('>', '_').replace('<', '_').split("\r\n");
        String error_message = "";
        for (int x = 0; x < names.length; x++) {
          String the_dir = names[x];
          if (!the_dir.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
            the_dir = String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + the_dir.substring(1); 
          this.thisSessionHTTP.cd(the_dir);
          if (!this.thisSessionHTTP.thisSession.check_access_privs(this.thisSessionHTTP.pwd(), "RETR") || !this.thisSessionHTTP.thisSession.check_access_privs(this.thisSessionHTTP.pwd(), "STOR")) {
            error_message = String.valueOf(error_message) + "ERROR:You need download, and upload permissions to encrypt a file:" + the_dir + "\r\n";
          } else {
            this.thisSessionHTTP.thisSession.uiPUT("the_command", "DECRYPT");
            this.thisSessionHTTP.thisSession.uiPUT("the_command_data", this.thisSessionHTTP.pwd());
            Properties item = this.thisSessionHTTP.thisSession.uVFS.get_item(the_dir);
            VRL vrl = new VRL(item.getProperty("url"));
            GenericClient c1 = this.thisSessionHTTP.thisSession.uVFS.getClient(item);
            GenericClient c2 = this.thisSessionHTTP.thisSession.uVFS.getClient(item);
            try {
              BufferedInputStream in = new BufferedInputStream(c1.download(vrl.getPath(), 0L, -1L, true));
              in.mark(2000);
              byte[] b = new byte[500];
              int totalBytes = 0;
              int bytesRead = 0;
              ByteArrayOutputStream baos = new ByteArrayOutputStream();
              while (bytesRead >= 0 && totalBytes < 1000) {
                bytesRead = in.read(b);
                if (bytesRead >= 0) {
                  baos.write(b, 0, bytesRead);
                  totalBytes += bytesRead;
                } 
              } 
              in.reset();
              String s = new String(baos.toByteArray(), "UTF8");
              if (s.indexOf("-----BEGIN PGP MESSAGE-----") < 0 || s.indexOf("CRUSHFTP#") < 0) {
                OutputStream out = c2.upload(String.valueOf(vrl.getPath()) + ".encrypting", 0L, true, true);
                if (!this.thisSessionHTTP.thisSession.user.getProperty("filePublicEncryptionKey", "").equals("")) {
                  out = Common.getEncryptedStream(out, this.thisSessionHTTP.thisSession.user.getProperty("filePublicEncryptionKey", ""), 0L, false);
                } else if (ServerStatus.BG("fileEncryption") && !ServerStatus.SG("filePublicEncryptionKey").equals("")) {
                  out = Common.getEncryptedStream(out, ServerStatus.SG("filePublicEncryptionKey"), 0L, ServerStatus.BG("file_encrypt_ascii"));
                } else if (!this.thisSessionHTTP.thisSession.user.getProperty("fileEncryptionKey", "").equals("")) {
                  out = Common.getEncryptedStream(out, this.thisSessionHTTP.thisSession.user.getProperty("fileEncryptionKey", ""), 0L, false);
                } else if (ServerStatus.BG("fileEncryption")) {
                  out = Common.getEncryptedStream(out, ServerStatus.SG("fileEncryptionKey"), 0L, false);
                } 
                Common.copyStreams(in, out, true, true);
                c1.rename(String.valueOf(vrl.getPath()) + ".encrypting", vrl.getPath());
                error_message = String.valueOf(error_message) + the_dir + " encrypted.";
              } else {
                Log.log("HTTP_SERVER", 0, "Ignoring encryption request, already encrypted:" + the_dir);
                error_message = String.valueOf(error_message) + the_dir + " : Ignoring encryption request, already encrypted.";
                in.close();
              } 
            } catch (Exception e) {
              Log.log("HTTP_SERVER", 0, e);
              error_message = String.valueOf(error_message) + "ERROR:" + e.getMessage() + "\r\n";
            } finally {
              c1 = this.thisSessionHTTP.thisSession.uVFS.releaseClient(c1);
              c2 = this.thisSessionHTTP.thisSession.uVFS.releaseClient(c2);
            } 
          } 
        } 
        response = String.valueOf(response) + "<commandResult><response>" + error_message + "</response></commandResult>";
        this.thisSessionHTTP.thisSession.uVFS.reset();
        return writeResponse(response.replace('%', ' '));
      } 
      if (command.equalsIgnoreCase("unzip")) {
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        String[] names = Common.url_decode(request.getProperty("names")).replace('>', '_').replace('<', '_').split("\r\n");
        String error_message = "";
        for (int x = 0; x < names.length; x++) {
          String the_dir = names[x];
          if (the_dir.toUpperCase().endsWith(".ZIP")) {
            if (!the_dir.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
              the_dir = String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + the_dir.substring(1); 
            this.thisSessionHTTP.cd(the_dir);
            if (!this.thisSessionHTTP.thisSession.check_access_privs(this.thisSessionHTTP.pwd(), "RETR") || !this.thisSessionHTTP.thisSession.check_access_privs(this.thisSessionHTTP.pwd(), "STOR") || !this.thisSessionHTTP.thisSession.check_access_privs(Common.all_but_last(this.thisSessionHTTP.pwd()), "MKD")) {
              error_message = String.valueOf(error_message) + "You need download, upload, and make directory permissions to unzip a file:" + the_dir + "\r\n";
            } else {
              this.thisSessionHTTP.thisSession.uiPUT("the_command", "UNZIP");
              this.thisSessionHTTP.thisSession.uiPUT("the_command_data", this.thisSessionHTTP.pwd());
              Properties item = this.thisSessionHTTP.thisSession.uVFS.get_item(the_dir);
              VRL vrl = new VRL(item.getProperty("url"));
              GenericClient c = this.thisSessionHTTP.thisSession.uVFS.getClient(item);
              try {
                Common.unzip(vrl.getPath(), c, this.thisSessionHTTP.thisSession, Common.all_but_last(the_dir));
              } finally {
                c = this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
              } 
            } 
          } 
        } 
        response = String.valueOf(response) + "<commandResult><response>" + error_message + "</response></commandResult>";
        this.thisSessionHTTP.thisSession.uVFS.reset();
        return writeResponse(response.replace('%', ' '));
      } 
      if (command.equalsIgnoreCase("zip")) {
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        String[] names = Common.url_decode(request.getProperty("names")).replace('>', '_').replace('<', '_').split("\r\n");
        String error_message = "";
        Vector zipFiles = new Vector();
        StringBuffer firstItemName = new StringBuffer();
        for (int x = 0; x < names.length; x++) {
          String str = names[x];
          if (!str.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
            str = String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + str.substring(1); 
          this.thisSessionHTTP.cd(str);
          if (!this.thisSessionHTTP.thisSession.check_access_privs(this.thisSessionHTTP.pwd(), "RETR") || !this.thisSessionHTTP.thisSession.check_access_privs(this.thisSessionHTTP.pwd(), "STOR")) {
            error_message = String.valueOf(error_message) + "You need download, upload permissions to zip a file:" + str + "\r\n";
          } else {
            this.thisSessionHTTP.thisSession.uiPUT("the_command", "ZIP");
            this.thisSessionHTTP.thisSession.uiPUT("the_command_data", this.thisSessionHTTP.pwd());
            Properties item = this.thisSessionHTTP.thisSession.uVFS.get_item(str);
            if (firstItemName.length() == 0)
              firstItemName.append(item.getProperty("name")); 
            this.thisSessionHTTP.thisSession.uVFS.getListing(zipFiles, str, 999, 50000, true);
          } 
        } 
        String the_dir = Common.url_decode(request.getProperty("path"));
        if (!the_dir.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
          the_dir = String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + the_dir.substring(1); 
        if (!this.thisSessionHTTP.thisSession.check_access_privs(the_dir, "STOR")) {
          error_message = String.valueOf(error_message) + "You need upload permissions to zip a file:" + request.getProperty("path") + "\r\n";
        } else {
          error_message = String.valueOf(error_message) + "Started zipping...\r\n";
          Properties outputItem = this.thisSessionHTTP.thisSession.uVFS.get_item(the_dir);
          String root_dir = (new VRL(outputItem.getProperty("url"))).getPath();
          Worker.startWorker(new Runnable(this, firstItemName, root_dir, zipFiles) {
                final ServerSessionAJAX this$0;
                
                private final StringBuffer val$firstItemName;
                
                private final String val$root_dir;
                
                private final Vector val$zipFiles;
                
                public void run() {
                  String zipName = String.valueOf(this.val$firstItemName.toString()) + "_" + Common.makeBoundary(3);
                  try {
                    Common.zip(this.val$root_dir, this.val$zipFiles, String.valueOf(this.val$root_dir) + zipName + ".zipping");
                    (new File(String.valueOf(this.val$root_dir) + zipName + ".zipping")).renameTo(new File(String.valueOf(this.val$root_dir) + zipName + ".zip"));
                  } catch (Exception e) {
                    Common.debug(0, e);
                    (new File(String.valueOf(this.val$root_dir) + zipName + ".zipping")).renameTo(new File(String.valueOf(this.val$root_dir) + zipName + ".bad"));
                  } 
                }
              }"Zipping:" + the_dir + ":" + request.getProperty("names"));
        } 
        response = String.valueOf(response) + "<commandResult><response>" + error_message + "</response></commandResult>";
        this.thisSessionHTTP.thisSession.uVFS.reset();
        return writeResponse(response.replace('%', ' '));
      } 
      if (command.equalsIgnoreCase("delete")) {
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        String[] names = Common.url_decode(request.getProperty("names")).replace('>', '_').replace('<', '_').split("\r\n");
        String error_message = "";
        for (int x = 0; x < names.length; x++) {
          String the_dir = names[x];
          if (the_dir.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
            the_dir = the_dir.substring(this.thisSessionHTTP.thisSession.SG("root_dir").length() - 1); 
          this.thisSessionHTTP.cd(the_dir);
          this.thisSessionHTTP.thisSession.uiPUT("the_command", "DELE");
          this.thisSessionHTTP.thisSession.uiPUT("the_command_data", this.thisSessionHTTP.pwd());
          String lastMessage = "";
          for (int xx = 0; xx < 10; xx++) {
            lastMessage = this.thisSessionHTTP.thisSession.do_DELE(true, the_dir);
            if (lastMessage.equals("%DELE-not found%"))
              lastMessage = ""; 
            if (lastMessage.indexOf("%DELE-error%") < 0)
              break; 
            Thread.sleep(1000L);
          } 
          error_message = String.valueOf(error_message) + lastMessage;
        } 
        response = String.valueOf(response) + "<commandResult><response>" + LOC.G(error_message) + "</response></commandResult>";
        this.thisSessionHTTP.thisSession.uVFS.reset();
        return writeResponse(response.replace('%', ' '));
      } 
      if (command.equalsIgnoreCase("rename")) {
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        String the_dir = Common.url_decode(request.getProperty("path"));
        if (the_dir.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
          the_dir = the_dir.substring(this.thisSessionHTTP.thisSession.SG("root_dir").length() - 1); 
        this.thisSessionHTTP.cd(the_dir);
        String item_name = Common.url_decode(request.getProperty("name1", ""));
        this.thisSessionHTTP.thisSession.uiPUT("the_command", "RNFR");
        this.thisSessionHTTP.thisSession.uiPUT("the_command_data", item_name);
        String error_message = this.thisSessionHTTP.thisSession.do_RNFR();
        if (error_message.equals("") || error_message.equals("%RNFR-bad%")) {
          String fromPath = SearchHandler.getPreviewPath(this.thisSessionHTTP.thisSession.rnfr_file, "1", 1);
          String item_name2 = Common.url_decode(request.getProperty("name2", ""));
          this.thisSessionHTTP.thisSession.uiPUT("the_command", "RNTO");
          this.thisSessionHTTP.thisSession.uiPUT("the_command_data", item_name2);
          if (!item_name.equals(item_name2)) {
            error_message = String.valueOf(error_message) + this.thisSessionHTTP.thisSession.do_RNTO(request.getProperty("overwrite", "false").equals("true"));
            if (error_message.equals("") && fromPath != null) {
              item_name2 = String.valueOf(the_dir) + item_name2;
              if (!item_name2.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
                item_name2 = String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + item_name2; 
              Properties rnto = this.thisSessionHTTP.thisSession.uVFS.get_item(item_name2);
              String toPath = SearchHandler.getPreviewPath(rnto, "1", 1);
              if (toPath != null)
                (new File(String.valueOf(ServerStatus.SG("previews_path")) + Common.all_but_last(Common.all_but_last(fromPath)).substring(1))).renameTo(new File(String.valueOf(ServerStatus.SG("previews_path")) + Common.all_but_last(Common.all_but_last(toPath)).substring(1))); 
            } 
            this.thisSessionHTTP.thisSession.uVFS.reset();
          } 
        } 
        response = String.valueOf(response) + "<commandResult><response>" + LOC.G(error_message) + "</response></commandResult>";
        this.thisSessionHTTP.thisSession.uVFS.reset();
        return writeResponse(response.replace('%', ' '));
      } 
      if (command.equalsIgnoreCase("makedir")) {
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        String the_dir = Common.url_decode(request.getProperty("path")).trim();
        the_dir = the_dir.replace(':', '_');
        if (the_dir.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
          the_dir = the_dir.substring(this.thisSessionHTTP.thisSession.SG("root_dir").length() - 1); 
        this.thisSessionHTTP.thisSession.uiPUT("the_command", "MKD");
        this.thisSessionHTTP.thisSession.uiPUT("the_command_data", the_dir);
        String error_message = this.thisSessionHTTP.thisSession.do_MKD(true, the_dir);
        this.thisSessionHTTP.thisSession.uVFS.reset();
        if (error_message.indexOf("%MKD-exists%") >= 0)
          error_message = ""; 
        response = String.valueOf(response) + "<commandResult><response>" + LOC.G(error_message) + "</response></commandResult>";
        return writeResponse(response.replace('%', ' '));
      } 
      if (command.equalsIgnoreCase("stat")) {
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        String the_dir = Common.url_decode(request.getProperty("path"));
        if (the_dir.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
          the_dir = the_dir.substring(this.thisSessionHTTP.thisSession.SG("root_dir").length() - 1); 
        this.thisSessionHTTP.thisSession.uiPUT("the_command", "STAT");
        this.thisSessionHTTP.thisSession.uiPUT("the_command_data", the_dir);
        Properties item = this.thisSessionHTTP.thisSession.uVFS.get_item(String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + the_dir);
        StringBuffer item_str = new StringBuffer();
        if (request.getProperty("calcFolder", "").equals("true")) {
          Vector listing = new Vector();
          long size = 0L;
          this.thisSessionHTTP.thisSession.uVFS.getListing(listing, String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + the_dir, 20, 10000, true, null);
          for (int x = 0; x < listing.size(); x++) {
            Properties p = listing.elementAt(x);
            size += Long.parseLong(p.getProperty("size", "0"));
          } 
          item.put("size", (new StringBuffer(String.valueOf(size))).toString());
        } 
        if (request.getProperty("format", "").equalsIgnoreCase("stat_dmz")) {
          if (item != null)
            item_str.append(AgentUI.formatDmzStat(item)); 
        } else if (item != null && LIST_handler.checkName(item, this.thisSessionHTTP.thisSession, true, true)) {
          LIST_handler.generateLineEntry(item, item_str, false, the_dir, true, this.thisSessionHTTP.thisSession, false);
        } 
        response = String.valueOf(response) + "<commandResult><response>" + item_str.toString().trim() + "</response></commandResult>";
        return writeResponse(response);
      } 
      if (command.equalsIgnoreCase("mdtm")) {
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        String the_dir = Common.url_decode(request.getProperty("path"));
        String the_file = Common.last(the_dir);
        for (int x = 0; x < ServerStatus.SG("unsafe_filename_chars").length(); x++)
          the_file = the_file.replace(ServerStatus.SG("unsafe_filename_chars").charAt(x), '_'); 
        the_dir = String.valueOf(Common.all_but_last(the_dir)) + the_file;
        if (the_dir.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
          the_dir = the_dir.substring(this.thisSessionHTTP.thisSession.SG("root_dir").length() - 1); 
        this.thisSessionHTTP.thisSession.uiPUT("the_command", "MDTM");
        this.thisSessionHTTP.thisSession.uiPUT("the_command_data", String.valueOf(the_dir) + " " + request.getProperty("date"));
        String error_message = this.thisSessionHTTP.thisSession.do_MDTM();
        this.thisSessionHTTP.thisSession.uVFS.reset();
        response = String.valueOf(response) + "<commandResult><response>" + LOC.G(error_message) + "</response></commandResult>";
        return writeResponse(response.replace('%', ' '));
      } 
      if (command.equalsIgnoreCase("publish") || command.equalsIgnoreCase("publish_attach")) {
        String response = "";
        if (command.equalsIgnoreCase("publish_attach") && ServerStatus.siIG("enterprise_level") <= 0) {
          response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
          response = String.valueOf(response) + "<commandResult><response>";
          response = String.valueOf(response) + "The server does not have an enterprise license, so sharing from email is not allowed.\r\n<br/>";
          response = String.valueOf(response) + "</response></commandResult>";
        } else if (Common.System2.get("crushftp.dmz.queue") != null) {
          Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
          Properties action = new Properties();
          action.put("type", "PUT:SHARE");
          action.put("id", Common.makeBoundary());
          action.put("username", this.thisSessionHTTP.thisSession.uiSG("user_name"));
          Properties root_item = this.thisSessionHTTP.thisSession.uVFS.get_item(this.thisSessionHTTP.thisSession.SG("root_dir"));
          GenericClient c = this.thisSessionHTTP.thisSession.uVFS.getClient(root_item);
          action.put("crushAuth", c.getConfig("crushAuth"));
          this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
          action.put("user_ip", this.thisSessionHTTP.thisSession.uiSG("user_ip"));
          action.put("request", request);
          action.put("clientid", this.thisSessionHTTP.thisSession.uiSG("clientid"));
          action.put("need_response", "true");
          queue.addElement(action);
          action = UserTools.waitResponse(action, 300);
          this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
          response = action.remove("object_response").toString();
        } else {
          if (command.equalsIgnoreCase("publish_attach")) {
            request.put("emailTo", "");
            String tempUsername = Common.url_decode(request.getProperty("temp_username", ""));
            String tempPassword = Common.url_decode(request.getProperty("temp_password", ""));
            if (tempUsername.equals("")) {
              tempUsername = Common.makeBoundary(ServerStatus.IG("temp_accounts_length"));
              tempPassword = Common.makeBoundary(ServerStatus.IG("temp_accounts_length"));
            } 
            String tmp_home = String.valueOf(ServerStatus.SG("temp_accounts_path")) + "storage/" + tempUsername + tempPassword + "/";
            (new File(tmp_home)).mkdirs();
            request.put("temp_username", tempUsername);
            request.put("temp_password", tempPassword);
            String fname = request.getProperty("paths");
            if (fname.startsWith("/"))
              fname = fname.substring(1); 
            if (fname.endsWith("/"))
              fname = fname.substring(0, fname.length() - 1); 
            Properties permissions = this.thisSessionHTTP.thisSession.uVFS.getPermission0();
            permissions.put(String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + request.getProperty("paths").substring(1).toUpperCase(), "(read)(write)(makedir)(deletedir)(view)(delete)(resume)(share)(invisible)");
            Properties dir_item = new Properties();
            dir_item.put("url", (new File(tmp_home)).toURI().toURL().toExternalForm());
            dir_item.put("type", "file");
            Vector v = new Vector();
            v.addElement(dir_item);
            Properties virtual = this.thisSessionHTTP.thisSession.uVFS.homes.elementAt(0);
            String path = String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + fname + "/";
            Properties vItem = new Properties();
            vItem.put("virtualPath", path);
            vItem.put("name", fname);
            vItem.put("type", "FILE");
            vItem.put("vItems", v);
            virtual.put(path.substring(0, path.length() - 1), vItem);
          } 
          Vector path_items = new Vector();
          String[] paths = (String[])null;
          if (request.getProperty("paths").indexOf(";") >= 0) {
            paths = Common.url_decode(request.getProperty("paths")).split(";");
          } else {
            paths = Common.url_decode(request.getProperty("paths")).split("\r\n");
          } 
          for (int x = 0; x < paths.length; x++) {
            String the_dir = paths[x].trim();
            if (!the_dir.equals("")) {
              if (the_dir.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
                the_dir = the_dir.substring(this.thisSessionHTTP.thisSession.SG("root_dir").length() - 1); 
              this.thisSessionHTTP.setupCurrentDir(the_dir);
              Log.log("HTTP_SERVER", 2, "Sharing:" + the_dir + "  vs.  " + this.thisSessionHTTP.pwd());
              Properties item = this.thisSessionHTTP.thisSession.uVFS.get_item(this.thisSessionHTTP.pwd());
              Log.log("HTTP_SERVER", 2, "Sharing:" + item);
              VRL vrl = new VRL(item.getProperty("url"));
              Properties stat = null;
              GenericClient c = this.thisSessionHTTP.thisSession.uVFS.getClient(item);
              try {
                c.login(vrl.getUsername(), vrl.getPassword(), null);
                stat = c.stat(vrl.getPath());
              } finally {
                c = this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
              } 
              stat.put("privs", item.getProperty("privs"));
              path_items.addElement(stat);
            } 
          } 
          request.put("emailBody", Common.replace_str(request.getProperty("emailBody", ""), "&amp;", "&"));
          request.put("emailBody", Common.replace_str(request.getProperty("emailBody", ""), "%26amp%3B", "&"));
          String user_name = this.thisSessionHTTP.thisSession.uiSG("user_name");
          response = createShare(path_items, request, (Vector)this.thisSessionHTTP.thisSession.user.get("web_customizations"), user_name, this.thisSessionHTTP.thisSession.server_item.getProperty("linkedServer"), this.thisSessionHTTP.thisSession.user, this.thisSessionHTTP.thisSession.date_time, this.thisSessionHTTP.thisSession, command.equalsIgnoreCase("publish_attach"));
          if (request.containsKey("keywords")) {
            request.put("names", request.getProperty("paths"));
            processKeywordsEdit(request);
          } 
        } 
        return writeResponse(response);
      } 
      if (command.equalsIgnoreCase("customEvent")) {
        String response = "";
        if (Common.System2.get("crushftp.dmz.queue") != null) {
          Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
          Properties action = new Properties();
          action.put("type", "PUT:CUSTOM");
          action.put("id", Common.makeBoundary());
          Properties root_item = this.thisSessionHTTP.thisSession.uVFS.get_item(this.thisSessionHTTP.thisSession.SG("root_dir"));
          GenericClient c = this.thisSessionHTTP.thisSession.uVFS.getClient(root_item);
          action.put("crushAuth", c.getConfig("crushAuth"));
          this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
          action.put("user_ip", this.thisSessionHTTP.thisSession.uiSG("user_ip"));
          action.put("request", request);
          action.put("clientid", this.thisSessionHTTP.thisSession.uiSG("clientid"));
          action.put("need_response", "true");
          queue.addElement(action);
          action = UserTools.waitResponse(action, 300);
          this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
          response = action.remove("object_response").toString();
        } else {
          request = Common.urlDecodePost(request);
          Vector path_items = new Vector();
          String[] paths = (String[])null;
          if (request.getProperty("paths").indexOf("|") >= 0) {
            paths = request.getProperty("paths").split("\\|");
          } else if (request.getProperty("paths").indexOf(";") >= 0) {
            paths = request.getProperty("paths").split(";");
          } else {
            paths = Common.url_decode(request.getProperty("paths")).split("\r\n");
          } 
          String short_folder = paths[0];
          int x;
          for (x = 0; x < paths.length; x++) {
            String the_dir = paths[x].trim();
            if (!the_dir.equals("") && 
              the_dir.length() < short_folder.length())
              short_folder = paths[x]; 
          } 
          short_folder = Common.all_but_last(short_folder);
          for (x = 0; x < paths.length; x++) {
            String the_dir = paths[x].trim();
            if (!the_dir.equals("")) {
              if (the_dir.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
                the_dir = the_dir.substring(this.thisSessionHTTP.thisSession.SG("root_dir").length() - 1); 
              this.thisSessionHTTP.setupCurrentDir(the_dir);
              Log.log("HTTP_SERVER", 2, "Custom:" + the_dir + "  vs.  " + this.thisSessionHTTP.pwd());
              Properties item = this.thisSessionHTTP.thisSession.uVFS.get_item(this.thisSessionHTTP.pwd());
              Log.log("HTTP_SERVER", 2, "Custom:" + item);
              try {
                item.put("root_dir", item.getProperty("root_dir").substring((String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + short_folder).length() - 2));
              } catch (Exception e) {
                Log.log("SERVER", 2, e);
                Log.log("SERVER", 2, "Short_folder:" + short_folder);
                Log.log("SERVER", 2, "root_dir:" + item.getProperty("root_dir"));
                Log.log("SERVER", 2, "session root_dir:" + this.thisSessionHTTP.thisSession.SG("root_dir"));
              } 
              path_items.addElement(item);
            } 
          } 
          response = createCustom(path_items, request, this.thisSessionHTTP.thisSession);
        } 
        return writeResponse(response, request.getProperty("json", "false").equals("true"));
      } 
      if (command.equalsIgnoreCase("cut_paste") || command.equalsIgnoreCase("copy_paste")) {
        if (this.thisSessionHTTP.thisSession.get("paste_ids") == null)
          this.thisSessionHTTP.thisSession.put("paste_ids", new Properties()); 
        Properties paste_ids = (Properties)this.thisSessionHTTP.thisSession.get("paste_ids");
        String paste_uid = Common.makeBoundary();
        StringBuffer status = (new StringBuffer()).append("Starting...");
        paste_ids.put(paste_uid, status);
        String[] names = Common.url_decode(request.getProperty("names")).replace('>', '_').replace('<', '_').split("\r\n");
        String destPath = Common.url_decode(request.getProperty("destPath")).replace('>', '_').replace('<', '_');
        String command2 = command;
        Worker.startWorker(new Runnable(this, status, names, destPath, command2) {
              final ServerSessionAJAX this$0;
              
              private final StringBuffer val$status;
              
              private final String[] val$names;
              
              private final String val$destPath;
              
              private final String val$command2;
              
              public void run() {
                SessionCrush.doPaste(this.this$0.thisSessionHTTP.thisSession, this.val$status, this.val$names, this.val$destPath, this.val$command2);
              }
            });
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        response = String.valueOf(response) + "<commandResult><response>";
        response = String.valueOf(response) + paste_uid;
        response = String.valueOf(response) + "</response></commandResult>";
        return writeResponse(response.replace('%', ' '));
      } 
      if (command.equalsIgnoreCase("paste_status")) {
        Properties paste_ids = (Properties)this.thisSessionHTTP.thisSession.get("paste_ids");
        String paste_uid = (new StringBuffer(String.valueOf(request.getProperty("id")))).toString();
        StringBuffer status = (StringBuffer)paste_ids.get(paste_uid);
        if (status == null)
          status = new StringBuffer(); 
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        synchronized (status) {
          if (status.toString().indexOf("ERROR:") >= 0 || status.toString().indexOf("COMPLETED:") >= 0)
            paste_ids.remove(paste_uid); 
          response = String.valueOf(response) + "<commandResult><response>";
          response = String.valueOf(response) + status.toString();
          response = String.valueOf(response) + "</response></commandResult>";
        } 
        return writeResponse(response.replace('%', ' '));
      } 
      if (command.equalsIgnoreCase("paste_cancel")) {
        Properties paste_ids = (Properties)this.thisSessionHTTP.thisSession.get("paste_ids");
        String paste_uid = (new StringBuffer(String.valueOf(request.getProperty("id")))).toString();
        StringBuffer status = (StringBuffer)paste_ids.get(paste_uid);
        if (status == null)
          status = new StringBuffer(); 
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        synchronized (status) {
          status.setLength(0);
          status.append("CANCELLED");
          response = String.valueOf(response) + "<commandResult><response>";
          response = String.valueOf(response) + status.toString();
          response = String.valueOf(response) + "</response></commandResult>";
        } 
        return writeResponse(response.replace('%', ' '));
      } 
      if (command.equalsIgnoreCase("editKeywords")) {
        String response = processKeywordsEdit(request);
        return writeResponse(response.replace('%', ' '));
      } 
      if (command.equalsIgnoreCase("getKeywords")) {
        String the_dir = Common.url_decode(request.getProperty("path").trim()).trim();
        if (the_dir.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
          the_dir = the_dir.substring(this.thisSessionHTTP.thisSession.SG("root_dir").length() - 1); 
        this.thisSessionHTTP.setupCurrentDir(the_dir);
        Properties item = this.thisSessionHTTP.thisSession.uVFS.get_item(this.thisSessionHTTP.pwd());
        the_dir = SearchHandler.getPreviewPath(item, "1", 1);
        String index = String.valueOf(ServerStatus.SG("previews_path")) + the_dir.substring(1);
        if (!(new File(Common.all_but_last(index))).exists())
          (new File(Common.all_but_last(index))).mkdirs(); 
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if ((new File(String.valueOf(Common.all_but_last(Common.all_but_last(index))) + "index.txt")).exists())
          Common.streamCopier(new FileInputStream(new File(String.valueOf(Common.all_but_last(Common.all_but_last(index))) + "index.txt")), baos, false, true, true); 
        write_command_http("HTTP/1.1 200 OK");
        write_command_http("Cache-Control: no-store");
        write_command_http("Content-Type: text/plain");
        this.thisSessionHTTP.write_standard_headers();
        byte[] utf8 = baos.toByteArray();
        write_command_http("Content-Length: " + utf8.length);
        write_command_http("Content-Disposition: attachment; filename=\"" + item.getProperty("name", "index") + "_keywords.txt\"");
        write_command_http("X-UA-Compatible: chrome=1");
        write_command_http("");
        this.thisSessionHTTP.original_os.write(utf8);
        this.thisSessionHTTP.original_os.flush();
        return true;
      } 
      if (command.equalsIgnoreCase("search")) {
        this.thisSessionHTTP.thisSession.put("search_status", "0/1");
        String the_dir = Common.url_decode(request.getProperty("path"));
        if (the_dir.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
          the_dir = the_dir.substring(this.thisSessionHTTP.thisSession.SG("root_dir").length() - 1); 
        this.thisSessionHTTP.setupCurrentDir(the_dir);
        String[] keywords = Common.url_decode(request.getProperty("keyword").replace('+', ' ')).trim().split(" ");
        boolean exact = request.getProperty("exact", "").equalsIgnoreCase("true");
        boolean all_keywords = request.getProperty("all_keywords", "true").equalsIgnoreCase("true");
        boolean date1 = request.getProperty("date1", "").equalsIgnoreCase("true");
        String date1_action = Common.url_decode(request.getProperty("date1_action", "").replace('+', ' '));
        String date1_value = request.getProperty("date1_value", "");
        boolean date2 = request.getProperty("date2", "").equalsIgnoreCase("true");
        String date2_action = Common.url_decode(request.getProperty("date2_action", "").replace('+', ' '));
        String date2_value = request.getProperty("date2_value", "");
        boolean size1 = request.getProperty("size1", "").equalsIgnoreCase("true");
        String size1_action = Common.url_decode(request.getProperty("size1_action", "").replace('+', ' '));
        String size1_value = request.getProperty("size1_value", "");
        boolean size2 = request.getProperty("size2", "").equalsIgnoreCase("true");
        String size2_action = Common.url_decode(request.getProperty("size2_action", "").replace('+', ' '));
        String size2_value = request.getProperty("size2_value", "");
        boolean type1 = request.getProperty("type1", "").equalsIgnoreCase("true");
        String type1_action = Common.url_decode(request.getProperty("type1_action", "").replace('+', ' '));
        boolean or_type = request.getProperty("include_type", "or").equalsIgnoreCase("or");
        boolean keywords_only = request.getProperty("keywords_only", "false").equals("false");
        this.thisSessionHTTP.thisSession.uiPUT("the_command", "LIST");
        this.thisSessionHTTP.thisSession.uiPUT("the_command_data", the_dir);
        Vector foundItems = new Vector();
        SimpleDateFormat mmddyyyy = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        long processedCount = 0L;
        int errors = 0;
        Vector listing = new Vector();
        SearchHandler searchHandler = new SearchHandler(this.thisSessionHTTP.thisSession, listing, this.thisSessionHTTP.pwd(), Integer.parseInt(request.getProperty("depth", "20")));
        Worker.startWorker(searchHandler);
        Properties root_item = this.thisSessionHTTP.thisSession.uVFS.get_item(this.thisSessionHTTP.thisSession.uiSG("root_dir"));
        while (searchHandler.isActive() || listing.size() > 0) {
          while (searchHandler.isActive() && listing.size() == 0)
            Thread.sleep(100L); 
          if (listing.size() == 0)
            break; 
          Properties pp = listing.elementAt(0);
          listing.removeElementAt(0);
          processedCount++;
          this.thisSessionHTTP.thisSession.put("search_status", String.valueOf(processedCount) + "/" + (listing.size() + processedCount));
          try {
            boolean date_ok = true;
            boolean size_ok = true;
            boolean type_ok = true;
            int name_count = 0;
            if (date1) {
              long modified1 = Long.parseLong(pp.getProperty("modified"));
              long modified2 = mmddyyyy.parse(date1_value).getTime();
              if (date1_action.equalsIgnoreCase("before") && modified2 <= modified1) {
                date_ok = false;
              } else if (date1_action.equalsIgnoreCase("after") && modified2 >= modified1) {
                date_ok = false;
              } 
            } 
            if (date2) {
              long modified1 = Long.parseLong(pp.getProperty("modified"));
              long modified2 = mmddyyyy.parse(date2_value).getTime();
              if (date2_action.equalsIgnoreCase("before") && modified2 <= modified1) {
                date_ok = false;
              } else if (date2_action.equalsIgnoreCase("after") && modified2 >= modified1) {
                date_ok = false;
              } 
            } 
            if (size1) {
              long file_size1 = Long.parseLong(pp.getProperty("size"));
              long file_size2 = Long.parseLong(size1_value) * 1024L;
              if (size1_action.equalsIgnoreCase("bigger than") && file_size2 >= file_size1) {
                size_ok = false;
              } else if (size1_action.equalsIgnoreCase("smaller than") && file_size2 <= file_size1) {
                size_ok = false;
              } 
            } 
            if (size2) {
              long file_size1 = Long.parseLong(pp.getProperty("size"));
              long file_size2 = Long.parseLong(size2_value) * 1024L;
              if (size2_action.equalsIgnoreCase("bigger than") && file_size2 >= file_size1) {
                size_ok = false;
              } else if (size2_action.equalsIgnoreCase("smaller than") && file_size2 <= file_size1) {
                size_ok = false;
              } 
            } 
            if (type1) {
              String item_type1 = pp.getProperty("type");
              if (type1_action.equalsIgnoreCase("file") && !item_type1.equalsIgnoreCase("file")) {
                type_ok = false;
              } else if (type1_action.equalsIgnoreCase("folder") && !item_type1.equalsIgnoreCase("dir")) {
                type_ok = false;
              } 
            } 
            if (date_ok && size_ok && type_ok && keywords.length > 0) {
              if (keywords_only) {
                int loop = 0;
                int last_loc = 0;
                while (loop < keywords.length) {
                  if (name_count > 0 && !all_keywords && or_type)
                    break; 
                  Log.log("HTTP_SERVER", 2, "search item name:" + pp.getProperty("name") + " vs. " + keywords[loop]);
                  if (!exact && !all_keywords && pp.getProperty("name").toUpperCase().indexOf(keywords[loop].toUpperCase().trim()) >= 0)
                    name_count++; 
                  if (exact && pp.getProperty("name").toUpperCase().equals(Common.url_decode(request.getProperty("keyword").replace('+', ' ')).trim()))
                    name_count++; 
                  if (all_keywords && pp.getProperty("name").toUpperCase().indexOf(keywords[loop].toUpperCase().trim(), last_loc) >= 0) {
                    last_loc = pp.getProperty("name").toUpperCase().indexOf(keywords[loop].toUpperCase().trim(), last_loc);
                    name_count++;
                  } 
                  loop++;
                } 
                if (all_keywords && name_count < keywords.length)
                  name_count = 0; 
              } 
              if (name_count == 0 && ServerStatus.BG("search_keywords_also")) {
                Log.log("HTTP_SERVER", 2, "name still not found, trying more...");
                String indexText = pp.getProperty("keywords", "");
                if (!pp.containsKey("keywords"))
                  indexText = SearchHandler.getKeywords(pp); 
                for (int x = 0; x < keywords.length; x++) {
                  if (name_count > 0 && or_type)
                    break; 
                  Log.log("HTTP_SERVER", 2, "search item indexText:" + indexText + " vs. " + keywords[x]);
                  if (!exact && indexText.toUpperCase().indexOf(keywords[x].toUpperCase().trim()) >= 0)
                    name_count++; 
                  if (exact && indexText.toUpperCase().indexOf("\r\n" + keywords[x].toUpperCase().trim() + "\r\n") >= 0)
                    name_count++; 
                } 
              } 
              if (!or_type && name_count < keywords.length) {
                Log.log("HTTP_SERVER", 2, "search item or_type:" + or_type + " name_count:" + name_count + " versus keywrod count:" + keywords.length);
                name_count = 0;
              } 
            } 
            Log.log("HTTP_SERVER", 2, "search item or_type:" + or_type + " name_count:" + name_count + " date_ok:" + date_ok + " size_ok:" + size_ok + " type_ok:" + type_ok);
            if (name_count > 0 && date_ok && size_ok && type_ok) {
              Log.log("HTTP_SERVER", 2, "search item result:" + pp.getProperty("name"));
              if (pp.getProperty("db", "false").equals("true")) {
                Properties ppp = SearchHandler.findItem(pp, this.thisSessionHTTP.thisSession.uVFS, null, this.thisSessionHTTP.thisSession.uiSG("root_dir"));
                Log.log("HTTP_SERVER", 2, "search item result:" + ppp);
                if (ppp == null)
                  continue; 
                pp.putAll(ppp);
              } 
              if (!pp.getProperty("name").equals("VFS") && pp.getProperty("privs", "").indexOf("(invisible)") < 0 && pp.getProperty("privs", "").indexOf("(view)") >= 0) {
                if (pp.getProperty("url", "").equals(root_item.getProperty("url")))
                  continue; 
                Log.log("HTTP_SERVER", 2, "search item adding found item");
                foundItems.addElement(pp);
                String privs = pp.getProperty("privs", "");
                if (privs.indexOf("(comment") >= 0)
                  privs = String.valueOf(privs.substring(0, privs.indexOf("(comment"))) + privs.substring(privs.indexOf(")", privs.indexOf("(comment"))); 
                privs = Common.replace_str(privs, "(inherited)", "");
                String current_dir2 = pp.getProperty("root_dir");
                if (current_dir2.toUpperCase().startsWith(this.thisSessionHTTP.thisSession.SG("root_dir").toUpperCase()))
                  current_dir2 = current_dir2.substring(this.thisSessionHTTP.thisSession.SG("root_dir").length() - 1); 
                pp.put("privs", String.valueOf(privs) + "(comment" + Common.url_encode("Containing Folder:<a href='" + current_dir2 + "'>" + current_dir2 + "</a>") + ")");
              } 
            } 
          } catch (Exception e) {
            Log.log("HTTP_SERVER", 1, e);
            if (errors++ > 1000)
              break; 
          } 
        } 
        Properties listingProp = getListingInfo(foundItems, this.thisSessionHTTP.thisSession.SG("root_dir"), "(VIEW)", false, this.thisSessionHTTP.thisSession.uVFS, false, true, this.thisSessionHTTP.thisSession, true);
        String altList = AgentUI.getJsonListObj(listingProp, ServerStatus.BG("exif_listings"));
        String info = "{\r\n";
        info = String.valueOf(info) + "\t\"privs\" : \"" + listingProp.getProperty("privs", "").trim().replaceAll("\"", "%22").replaceAll("\t", "%09") + "\",\r\n";
        info = String.valueOf(info) + "\t\"path\" : \"" + listingProp.getProperty("path", "").trim().replaceAll("\"", "%22").replaceAll("\t", "%09") + "\",\r\n";
        info = String.valueOf(info) + "\t\"defaultStrings\" : \"" + listingProp.getProperty("defaultStrings", "").trim() + "\",\r\n";
        info = String.valueOf(info) + "\t\"site\" : \"" + listingProp.getProperty("site", "").trim() + "\",\r\n";
        info = String.valueOf(info) + "\t\"quota\" : \"" + listingProp.getProperty("quota", "").trim() + "\",\r\n";
        info = String.valueOf(info) + "\t\"quota_bytes\" : \"" + listingProp.getProperty("quota_bytes", "").trim() + "\",\r\n";
        info = String.valueOf(info) + "\t\"listing\" : " + altList + "\r\n";
        info = String.valueOf(info) + "}\r\n";
        boolean ok = writeResponse(info, false, 200, true, true, true);
        this.thisSessionHTTP.thisSession.put("search_status", "0/1");
        return ok;
      } 
      if (command.equalsIgnoreCase("getSearchStatus")) {
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        response = String.valueOf(response) + "<commandResult><response>" + this.thisSessionHTTP.thisSession.uiSG("search_status").trim() + "</response></commandResult>";
        this.thisSessionHTTP.thisSession.uVFS.reset();
        return writeResponse(response);
      } 
      if (command.equalsIgnoreCase("getTunnels")) {
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        String userTunnels = String.valueOf(this.thisSessionHTTP.thisSession.user.getProperty("tunnels", "").trim()) + ",";
        Vector tunnels = (Vector)ServerStatus.VG("tunnels").clone();
        tunnels.addAll(ServerStatus.VG("tunnels_dmz"));
        ByteArrayOutputStream baot = new ByteArrayOutputStream();
        int lesserSpeed = this.thisSessionHTTP.thisSession.IG("speed_limit_upload");
        if ((lesserSpeed > 0 && lesserSpeed > ServerStatus.IG("max_server_upload_speed") && ServerStatus.IG("max_server_upload_speed") > 0) || lesserSpeed == 0)
          lesserSpeed = ServerStatus.IG("max_server_upload_speed"); 
        for (int x = 0; x < tunnels.size(); x++) {
          ByteArrayOutputStream baot2 = new ByteArrayOutputStream();
          Properties pp = tunnels.elementAt(x);
          if (userTunnels.indexOf(String.valueOf(pp.getProperty("id")) + ",") >= 0 && !pp.getProperty("tunnelType", "HTTP").equals("SSH")) {
            pp.put("max_speed", (new StringBuffer(String.valueOf(lesserSpeed))).toString());
            pp.store(baot2, "");
            String s = new String(baot2.toByteArray(), "UTF8");
            s = Common.url_encode(s);
            baot.write(s.getBytes("UTF8"));
            baot.write(";;;".getBytes());
          } 
        } 
        String tunnelsStr = (new String(baot.toByteArray(), "UTF8")).replace('%', '~');
        response = String.valueOf(response) + "<commandResult><response>" + tunnelsStr + "</response></commandResult>";
        this.thisSessionHTTP.thisSession.uVFS.reset();
        return writeResponse(response);
      } 
      if (command.equalsIgnoreCase("download")) {
        Enumeration keys = request.keys();
        Properties metaInfo = new Properties();
        while (keys.hasMoreElements()) {
          String key = keys.nextElement().toString();
          if (key.toUpperCase().startsWith("META_")) {
            String val = request.getProperty(key);
            key = key.substring("META_".length());
            metaInfo.put(key, val);
            if (key.toUpperCase().startsWith("GLOBAL_")) {
              if (Common.System2.get("global_variables") == null)
                Common.System2.put("global_variables", new Properties()); 
              Properties global_variables = (Properties)Common.System2.get("global_variables");
              global_variables.put(key, val);
              continue;
            } 
            if (key.toUpperCase().startsWith("USER_INFO_"))
              this.thisSessionHTTP.thisSession.user_info.put(key, val); 
          } 
        } 
        this.thisSessionHTTP.thisSession.uiPUT("the_command", "RETR");
        String the_dir = Common.url_decode(request.getProperty("path"));
        if (!the_dir.toUpperCase().startsWith(this.thisSessionHTTP.thisSession.SG("root_dir").toUpperCase()))
          the_dir = String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + (the_dir.startsWith("/") ? the_dir.substring(1) : the_dir); 
        this.thisSessionHTTP.cd(the_dir);
        Properties item = null;
        VRL otherFile = null;
        boolean ok = (this.thisSessionHTTP.thisSession.check_access_privs(this.thisSessionHTTP.pwd(), "RETR") && Common.filter_check("D", Common.last(this.thisSessionHTTP.pwd()), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + this.thisSessionHTTP.SG("file_filter")));
        item = this.thisSessionHTTP.thisSession.uVFS.get_item(this.thisSessionHTTP.pwd());
        if (item != null && item.getProperty("type", "").equalsIgnoreCase("DIR") && !this.thisSessionHTTP.pwd().endsWith("/"))
          ok = false; 
        if (ok)
          otherFile = new VRL(item.getProperty("url")); 
        if (otherFile == null && this.thisSessionHTTP.pwd().toUpperCase().endsWith(".ZIP")) {
          this.thisSessionHTTP.cd(this.thisSessionHTTP.pwd().substring(0, this.thisSessionHTTP.pwd().length() - 4));
          ok = (this.thisSessionHTTP.thisSession.check_access_privs(this.thisSessionHTTP.pwd(), "RETR") && Common.filter_check("D", Common.last(this.thisSessionHTTP.pwd()), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + this.thisSessionHTTP.SG("file_filter")));
          item = this.thisSessionHTTP.thisSession.uVFS.get_item(this.thisSessionHTTP.pwd());
          if (item != null && ok && item.getProperty("privs").indexOf("(read)") < 0)
            ok = false; 
          if (ok) {
            otherFile = new VRL(item.getProperty("url"));
            otherFile = new VRL(String.valueOf(Common.all_but_last(otherFile.toString())) + otherFile.getName() + ".zip");
          } else {
            this.thisSessionHTTP.cd(String.valueOf(this.thisSessionHTTP.pwd()) + ".zip");
          } 
        } 
        if (ok) {
          if (!metaInfo.getProperty("downloadRevision", "").equals("") && ServerStatus.siIG("enterprise_level") > 0) {
            VRL otherFile2 = null;
            int rev = Integer.parseInt(metaInfo.getProperty("downloadRevision", ""));
            while (rev >= 0) {
              String privs = item.getProperty("privs");
              if (privs.indexOf("(sync") >= 0) {
                String path = this.thisSessionHTTP.pwd();
                if (path.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
                  path = path.substring(this.thisSessionHTTP.thisSession.SG("root_dir").length() - 1); 
                if (Common.System2.get("crushftp.dmz.queue") != null) {
                  String fname = Common.last(path);
                  write_command_http("HTTP/1.1 200 OK");
                  int validSecs = 30;
                  write_command_http("Cache-Control: post-check=" + validSecs + ",pre-check=" + (validSecs * 10));
                  write_command_http("Content-Type: application/binary");
                  fname = Common.replace_str(Common.url_decode(fname), "\r", "_");
                  write_command_http("Content-Disposition: attachment; filename=\"" + ((this.thisSessionHTTP.thisSession.uiSG("header_user-agent").toUpperCase().indexOf("MSIE") >= 0 || this.thisSessionHTTP.thisSession.uiSG("header_user-agent").toUpperCase().indexOf("TRIDENT") >= 0) ? Common.url_encode(fname) : fname) + "\"");
                  write_command_http("X-UA-Compatible: chrome=1");
                  write_command_http("Connection: close");
                  write_command_http("");
                  GenericClient c = this.thisSessionHTTP.thisSession.uVFS.getClient(item);
                  try {
                    InputStream in = ((HTTPClient)c).downloadRev(path, rev);
                    try {
                      byte[] b = new byte[32768];
                      int bytesRead = 0;
                      while (bytesRead >= 0) {
                        bytesRead = in.read(b);
                        if (bytesRead > 0) {
                          this.thisSessionHTTP.original_os.write(b, 0, bytesRead);
                          this.thisSessionHTTP.thisSession.active_transfer();
                        } 
                      } 
                      this.thisSessionHTTP.original_os.flush();
                    } finally {
                      in.close();
                      this.thisSessionHTTP.original_os.close();
                    } 
                  } catch (Exception e) {
                    Log.log("HTTP_SERVER", 1, e);
                  } 
                  c.close();
                  this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
                  return true;
                } 
                String revPath = Common.parseSyncPart(privs, "revisionsPath");
                if (!revPath.equals(""))
                  otherFile2 = new VRL(String.valueOf(revPath) + path + "/" + rev + "/" + otherFile.getName()); 
              } 
              if (otherFile2 != null && (new File(otherFile2.getPath())).exists())
                break; 
              otherFile2 = null;
              rev--;
            } 
            if (otherFile2 != null) {
              otherFile = otherFile2;
              item.put("url", otherFile2.toString());
            } 
          } 
        } else {
          boolean ok1 = (this.thisSessionHTTP.thisSession.check_access_privs(Common.all_but_last(this.thisSessionHTTP.pwd()), "RETR") && Common.filter_check("D", Common.last(this.thisSessionHTTP.pwd()), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + this.thisSessionHTTP.SG("file_filter")));
          boolean ok2 = (this.thisSessionHTTP.thisSession.check_access_privs(this.thisSessionHTTP.pwd(), "RETR") && Common.filter_check("D", Common.last(this.thisSessionHTTP.pwd()), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + this.thisSessionHTTP.SG("file_filter")));
          if (ok1 || ok2) {
            write_command_http("HTTP/1.1 404 Not Found");
          } else {
            write_command_http("HTTP/1.1 403 Access Denied.");
          } 
          write_command_http("Content-Length: 0");
          write_command_http("");
          return true;
        } 
        downloadItem(otherFile, item, item.getProperty("name"), byteRanges, request.containsKey("range"), request.getProperty("mimeType", ""));
        return true;
      } 
      if (command.startsWith("downloadAsZip")) {
        this.thisSessionHTTP.thisSession.uiPUT("the_command", "RETR");
        this.thisSessionHTTP.done = true;
        write_command_http("HTTP/1.1 200 OK");
        int validSecs = 30;
        write_command_http("Cache-Control: post-check=" + validSecs + ",pre-check=" + (validSecs * 10));
        write_command_http("Content-Type: application/zip");
        this.thisSessionHTTP.write_standard_headers();
        String paths = Common.url_decode(request.getProperty("paths", ""));
        String[] itemList = paths.split(":");
        String current_dir = request.getProperty("path", "/");
        if (request.getProperty("path_shortening", "true").equals("true")) {
          String commonStartPath = "";
          for (int i = 0; i < itemList.length; i++) {
            String file_path = itemList[i];
            if ((file_path.split("\\/")).length > (commonStartPath.split("\\/")).length)
              commonStartPath = file_path; 
          } 
          while (!commonStartPath.equals("")) {
            commonStartPath = Common.all_but_last(commonStartPath);
            boolean ok = true;
            for (int j = 0; j < itemList.length && ok; j++) {
              String file_path = itemList[j];
              if (!file_path.equals("") && 
                !file_path.startsWith(commonStartPath))
                ok = false; 
            } 
            if (ok)
              break; 
          } 
          current_dir = commonStartPath;
          if (current_dir.equals(""))
            current_dir = "/"; 
        } 
        String fname = "archive.zip";
        if (itemList.length == 1)
          fname = String.valueOf(itemList[0]) + ".zip"; 
        if (fname.indexOf("/") >= 0) {
          fname = Common.last(itemList[0]);
          if (fname.endsWith("/"))
            fname = fname.substring(0, fname.length() - 1); 
          fname = String.valueOf(fname) + ".zip";
        } 
        if (request.getProperty("zipName", "").length() > 0)
          fname = request.getProperty("zipName", ""); 
        fname = Common.replace_str(Common.url_decode(fname), "\r", "_");
        write_command_http("Content-Disposition: attachment; filename=\"" + ((this.thisSessionHTTP.thisSession.uiSG("header_user-agent").toUpperCase().indexOf("MSIE") >= 0 || this.thisSessionHTTP.thisSession.uiSG("header_user-agent").toUpperCase().indexOf("TRIDENT") >= 0) ? Common.url_encode(fname) : fname) + "\"");
        write_command_http("X-UA-Compatible: chrome=1");
        write_command_http("Connection: close");
        write_command_http("");
        if (Common.System2.get("crushftp.dmz.queue") != null) {
          String root_loc = itemList[itemList.length - 1];
          if (!root_loc.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
            root_loc = String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + root_loc.substring(1); 
          Properties properties = this.thisSessionHTTP.thisSession.uVFS.get_item(root_loc);
          GenericClient c = this.thisSessionHTTP.thisSession.uVFS.getClient(properties);
          try {
            InputStream in = ((HTTPClient)c).downloadAsZip(current_dir, paths, request.getProperty("path_shortening", "true").equals("true"));
            try {
              byte[] b = new byte[32768];
              int bytesRead = 0;
              while (bytesRead >= 0) {
                bytesRead = in.read(b);
                if (bytesRead > 0) {
                  this.thisSessionHTTP.original_os.write(b, 0, bytesRead);
                  this.thisSessionHTTP.thisSession.active_transfer();
                } 
              } 
              this.thisSessionHTTP.original_os.flush();
            } finally {
              in.close();
              this.thisSessionHTTP.original_os.close();
            } 
          } catch (Exception e) {
            Log.log("HTTP_SERVER", 1, e);
          } 
          c.close();
          this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
          return true;
        } 
        if (!current_dir.toUpperCase().startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
          current_dir = String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + current_dir.substring(1); 
        Properties item = this.thisSessionHTTP.thisSession.uVFS.get_item("/");
        if (item == null)
          item = new Properties(); 
        Properties sizeLookup = new Properties();
        if (request.getProperty("filters", "").trim().length() > 0) {
          String[] filters = Common.url_decode(request.getProperty("filters", "")).split("\r\n");
          for (int i = 0; i < filters.length; i++) {
            String name = filters[i].split(":")[0];
            String size = filters[i].split(":")[1];
            sizeLookup.put(name, size);
          } 
        } 
        int loc = 0;
        Vector activeThreads = new Vector();
        while (loc < itemList.length) {
          if (itemList[loc].length() > 0) {
            String current_dir2 = itemList[loc];
            if (!current_dir2.toUpperCase().startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
              current_dir2 = String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + current_dir2.substring(1); 
            this.thisSessionHTTP.cd(current_dir2);
            Properties item2 = this.thisSessionHTTP.thisSession.uVFS.get_item(this.thisSessionHTTP.pwd());
            if (item2 != null && this.thisSessionHTTP.thisSession.check_access_privs(this.thisSessionHTTP.pwd(), "RETR")) {
              boolean singleThread = !(sizeLookup.size() <= 0 && ((new VRL(item2.getProperty("url"))).getProtocol().equalsIgnoreCase("file") || (new VRL(item2.getProperty("url"))).getProtocol().equalsIgnoreCase("smb")));
              if (item2.getProperty("type").equalsIgnoreCase("FILE")) {
                this.thisSessionHTTP.retr.zipFiles.addElement(item2);
              } else {
                Common.startMultiThreadZipper(this.thisSessionHTTP.thisSession.uVFS, this.thisSessionHTTP.retr, this.thisSessionHTTP.pwd(), 0, singleThread, activeThreads);
              } 
            } 
          } 
          if (activeThreads.size() >= 10)
            Thread.sleep(100L); 
          loc++;
        } 
        for (int x = this.thisSessionHTTP.retr.zipFiles.size() - 1; x >= 0 && sizeLookup.size() > 0; x--) {
          Properties zitem = this.thisSessionHTTP.retr.zipFiles.elementAt(x);
          String root_dir = zitem.getProperty("root_dir");
          if (root_dir.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
            root_dir = root_dir.substring(this.thisSessionHTTP.thisSession.SG("root_dir").length() - 1); 
          String size = sizeLookup.getProperty(String.valueOf(root_dir) + zitem.getProperty("name"));
          if (size != null)
            if (Long.parseLong(size) == Long.parseLong(zitem.getProperty("size"))) {
              this.thisSessionHTTP.retr.zipFiles.removeElementAt(x);
            } else if (Long.parseLong(size) < Long.parseLong(zitem.getProperty("size"))) {
              zitem.put("rest", size);
            }  
        } 
        VRL otherFile = new VRL(item.getProperty("url"));
        this.thisSessionHTTP.cd(current_dir);
        this.thisSessionHTTP.thisSession.uiPUT("file_transfer_mode", "BINARY");
        this.thisSessionHTTP.retr.data_os = this.thisSessionHTTP.original_os;
        this.thisSessionHTTP.retr.httpDownload = true;
        this.thisSessionHTTP.retr.zipping = true;
        this.thisSessionHTTP.thisSession.uiPUT("no_zip_compression", request.getProperty("no_zip_compression", "false"));
        this.thisSessionHTTP.thisSession.uiPUT("zip64", request.getProperty("zip64", "false"));
        String the_dir = this.thisSessionHTTP.pwd();
        Properties pp = new Properties();
        pp.put("the_dir", the_dir);
        this.thisSessionHTTP.thisSession.runPlugin("transfer_path", pp);
        the_dir = pp.getProperty("the_dir", the_dir);
        this.thisSessionHTTP.retr.init_vars(the_dir, this.thisSessionHTTP.thisSession.uiLG("start_resume_loc"), -1L, this.thisSessionHTTP.thisSession, item, false, "", otherFile, null);
        this.thisSessionHTTP.retr.runOnce = true;
        this.thisSessionHTTP.retr.run();
        return true;
      } 
      if (command.equals("getXMLListing")) {
        String the_dir = Common.url_decode(request.getProperty("path", ""));
        the_dir = Common.dots(the_dir);
        if (the_dir.equals("/"))
          the_dir = this.thisSessionHTTP.thisSession.SG("root_dir"); 
        if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(this.thisSessionHTTP.thisSession.SG("root_dir").toUpperCase()))
          the_dir = String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + the_dir.substring(1); 
        this.thisSessionHTTP.cd(the_dir);
        String response = getXmlListingResponse(this.thisSessionHTTP.thisSession.uiSG("user_name"), request, the_dir, false, this.thisSessionHTTP.thisSession.uVFS);
        return writeResponse(response, false, 200, false, request.getProperty("format", "").equalsIgnoreCase("JSONOBJ"), true);
      } 
      if (command.equals("getHistory")) {
        String response = "";
        if (Common.System2.get("crushftp.dmz.queue") != null) {
          Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
          Properties action = new Properties();
          action.put("type", "PUT:GETHISTORY");
          action.put("id", Common.makeBoundary());
          action.put("username", this.thisSessionHTTP.thisSession.uiSG("user_name"));
          Properties root_item = this.thisSessionHTTP.thisSession.uVFS.get_item(this.thisSessionHTTP.thisSession.SG("root_dir"));
          GenericClient c = this.thisSessionHTTP.thisSession.uVFS.getClient(root_item);
          action.put("crushAuth", c.getConfig("crushAuth"));
          this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
          action.put("user_ip", this.thisSessionHTTP.thisSession.uiSG("user_ip"));
          action.put("request", request);
          action.put("clientid", this.thisSessionHTTP.thisSession.uiSG("clientid"));
          action.put("need_response", "true");
          queue.addElement(action);
          action = UserTools.waitResponse(action, 300);
          this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
          response = action.remove("object_response").toString();
        } else {
          response = getHistory(request, this.thisSessionHTTP.thisSession);
        } 
        return writeResponse(response, false, 200, true, true, true);
      } 
      if (command.equals("manageShares")) {
        String response = "";
        if (Common.System2.get("crushftp.dmz.queue") != null) {
          Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
          Properties action = new Properties();
          action.put("type", "PUT:MANAGESHARES");
          action.put("id", Common.makeBoundary());
          action.put("username", this.thisSessionHTTP.thisSession.uiSG("user_name"));
          Properties root_item = this.thisSessionHTTP.thisSession.uVFS.get_item(this.thisSessionHTTP.thisSession.SG("root_dir"));
          GenericClient c = this.thisSessionHTTP.thisSession.uVFS.getClient(root_item);
          action.put("crushAuth", c.getConfig("crushAuth"));
          this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
          action.put("user_ip", this.thisSessionHTTP.thisSession.uiSG("user_ip"));
          action.put("request", request);
          action.put("clientid", this.thisSessionHTTP.thisSession.uiSG("clientid"));
          action.put("need_response", "true");
          queue.addElement(action);
          action = UserTools.waitResponse(action, 300);
          this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
          response = action.remove("object_response").toString();
        } else {
          response = manageShares(this.thisSessionHTTP.thisSession);
        } 
        return writeResponse(response);
      } 
      if (command.equals("deleteTempAccount")) {
        String response = "";
        if (Common.System2.get("crushftp.dmz.queue") != null) {
          Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
          Properties action = new Properties();
          action.put("type", "PUT:DELETESHARE");
          action.put("id", Common.makeBoundary());
          action.put("username", this.thisSessionHTTP.thisSession.uiSG("user_name"));
          Properties root_item = this.thisSessionHTTP.thisSession.uVFS.get_item(this.thisSessionHTTP.thisSession.SG("root_dir"));
          GenericClient c = this.thisSessionHTTP.thisSession.uVFS.getClient(root_item);
          action.put("crushAuth", c.getConfig("crushAuth"));
          this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
          action.put("user_ip", this.thisSessionHTTP.thisSession.uiSG("user_ip"));
          action.put("request", request);
          action.put("clientid", this.thisSessionHTTP.thisSession.uiSG("clientid"));
          action.put("need_response", "true");
          queue.addElement(action);
          action = UserTools.waitResponse(action, 300);
          this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
          response = action.remove("object_response").toString();
        } else {
          response = deleteShare(request, this.thisSessionHTTP.thisSession);
        } 
        return writeResponse(response);
      } 
      if (command.equals("editTempAccount")) {
        String response = "";
        if (Common.System2.get("crushftp.dmz.queue") != null) {
          Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
          Properties action = new Properties();
          action.put("type", "PUT:EDITSHARE");
          action.put("id", Common.makeBoundary());
          action.put("username", this.thisSessionHTTP.thisSession.uiSG("user_name"));
          Properties root_item = this.thisSessionHTTP.thisSession.uVFS.get_item(this.thisSessionHTTP.thisSession.SG("root_dir"));
          GenericClient c = this.thisSessionHTTP.thisSession.uVFS.getClient(root_item);
          action.put("crushAuth", c.getConfig("crushAuth"));
          this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
          action.put("user_ip", this.thisSessionHTTP.thisSession.uiSG("user_ip"));
          action.put("request", request);
          action.put("clientid", this.thisSessionHTTP.thisSession.uiSG("clientid"));
          action.put("need_response", "true");
          queue.addElement(action);
          action = UserTools.waitResponse(action, 300);
          this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
          response = action.remove("object_response").toString();
        } else {
          response = editShare(request, this.thisSessionHTTP.thisSession);
        } 
        return writeResponse(response);
      } 
      if (command.equals("getCustomForm")) {
        String response = "";
        String name = this.thisSessionHTTP.thisSession.SG(request.getProperty("form"));
        if (name.indexOf(":") < 0 && !name.equals("messageForm"))
          name = String.valueOf(name) + ":" + name; 
        if (!this.thisSessionHTTP.thisSession.getProperty(request.getProperty("form"), "").equals(""))
          name = this.thisSessionHTTP.thisSession.getProperty(request.getProperty("form"), ""); 
        if (name != null && name.length() > 0 && name.indexOf(":") > 0)
          try {
            if (name.indexOf(":") == name.lastIndexOf(":")) {
              name = name.substring(name.lastIndexOf(":") + 1).trim();
            } else {
              name = name.substring(name.lastIndexOf(":", name.lastIndexOf(":") - 1) + 1, name.lastIndexOf(":")).trim();
            } 
            Vector customForms = (Vector)ServerStatus.server_settings.get("CustomForms");
            for (int x = 0; x < customForms.size(); x++) {
              Properties form = customForms.elementAt(x);
              form = (Properties)Common.CLONE(form);
              form.put("always", (new StringBuffer(String.valueOf(!this.thisSessionHTTP.thisSession.SG(request.getProperty("form")).endsWith(":once")))).toString());
              if (form.getProperty("name").equalsIgnoreCase(name)) {
                Vector entries = (Vector)form.get("entries");
                for (int xx = 0; xx < entries.size(); xx++) {
                  Properties entry = entries.elementAt(xx);
                  entry.put("item_type", entry.getProperty("type"));
                  if (this.thisSessionHTTP.thisSession.containsKey(entry.getProperty("name", "label").trim()))
                    entry.put("value", Common.url_decode(this.thisSessionHTTP.thisSession.getProperty(entry.getProperty("name").trim()))); 
                  entry.put("value", ServerStatus.change_vars_to_values_static(entry.getProperty("value", ""), this.thisSessionHTTP.thisSession.user, this.thisSessionHTTP.thisSession.user_info, this.thisSessionHTTP.thisSession));
                  entry.put("label", ServerStatus.change_vars_to_values_static(entry.getProperty("label", ""), this.thisSessionHTTP.thisSession.user, this.thisSessionHTTP.thisSession.user_info, this.thisSessionHTTP.thisSession));
                } 
                try {
                  response = Common.getXMLString(form, "customForm", null);
                } catch (Exception e) {
                  Log.log("HTTP_SERVER", 1, e);
                } 
              } 
            } 
          } catch (Exception e) {
            Log.log("HTTP_SERVER", 1, e);
          }  
        return writeResponse(response);
      } 
      if (command.equals("getUserInfo")) {
        Properties responseProp = new Properties();
        Properties extraCustomizations = (Properties)this.thisSessionHTTP.thisSession.get("extraCustomizations");
        if (extraCustomizations != null)
          responseProp.putAll(extraCustomizations); 
        extraCustomizations = null;
        extraCustomizations = (Properties)this.thisSessionHTTP.thisSession.user.get("extraCustomizations");
        if (extraCustomizations != null)
          responseProp.putAll(extraCustomizations); 
        Vector customizations = null;
        try {
          customizations = (Vector)this.thisSessionHTTP.thisSession.user.get("web_customizations");
          if (customizations == null)
            customizations = (Vector)UserTools.ut.getUser(this.thisSessionHTTP.thisSession.uiSG("listen_ip_port"), "default", false).get("web_customizations"); 
          if (customizations == null)
            customizations = new Vector(); 
          customizations = (Vector)Common.CLONE(customizations);
          boolean hasLogo = false;
          Properties footer = null;
          for (int x = 0; x < customizations.size(); x++) {
            Properties pp = customizations.elementAt(x);
            String key = pp.getProperty("key");
            if (key.startsWith("flash_")) {
              key = key.substring("flash_".length());
              pp.put("key", key);
            } 
            pp.put("value", ServerStatus.thisObj.change_vars_to_values(pp.getProperty("value", ""), this.thisSessionHTTP.thisSession));
            if (key.equals("logo"))
              hasLogo = true; 
            if (key.equals("footer"))
              footer = pp; 
          } 
          if (!hasLogo && !ServerStatus.SG("default_logo").equals("")) {
            Properties pp = new Properties();
            pp.put("key", "logo");
            pp.put("value", ServerStatus.SG("default_logo"));
            customizations.addElement(pp);
          } 
          if (footer == null) {
            footer = new Properties();
            footer.put("key", "footer");
            footer.put("value", "");
            customizations.addElement(footer);
          } 
          footer.put("value", String.valueOf(footer.getProperty("value")) + ServerStatus.SG("webFooterText"));
          if (System.getProperties().get("crushftp.httpCustomizations.global") != null) {
            Properties globalCust = (Properties)System.getProperties().get("crushftp.httpCustomizations.global");
            Enumeration keys = globalCust.keys();
            while (keys.hasMoreElements()) {
              String key = keys.nextElement().toString();
              Properties ppp = new Properties();
              ppp.put("key", key);
              ppp.put("value", globalCust.getProperty(key));
              customizations.addElement(ppp);
            } 
          } 
          if (System.getProperties().get("crushftp.httpCustomizations.user") != null) {
            Properties userCust = (Properties)System.getProperties().get("crushftp.httpCustomizations.user");
            Properties pp = (Properties)userCust.get(this.thisSessionHTTP.thisSession.uiSG("user_name").toUpperCase());
            if (pp != null) {
              Enumeration keys = pp.keys();
              while (keys.hasMoreElements()) {
                String key = keys.nextElement().toString();
                Properties ppp = new Properties();
                ppp.put("key", key);
                ppp.put("value", pp.getProperty(key));
                customizations.addElement(ppp);
              } 
            } 
          } 
          responseProp.put("customizations", customizations);
        } catch (Exception e) {
          Log.log("HTTP_SERVER", 1, e);
        } 
        try {
          Vector buttons = (Vector)this.thisSessionHTTP.thisSession.user.get("web_buttons");
          if (buttons == null)
            buttons = (Vector)UserTools.ut.getUser(this.thisSessionHTTP.thisSession.uiSG("listen_ip_port"), "default", false).get("buttons"); 
          if (buttons == null)
            buttons = new Vector(); 
          buttons = (Vector)buttons.clone();
          addMissingButtons(buttons);
          fixButtons(buttons);
          if (this.thisSessionHTTP.thisSession.BG("hide_download"))
            for (int x = buttons.size() - 1; x >= 0; x--) {
              Properties button = buttons.elementAt(x);
              if (button.getProperty("key", "").startsWith("(download)"))
                buttons.remove(x); 
            }  
          responseProp.put("buttons", buttons);
        } catch (Exception e) {
          Log.log("HTTP_SERVER", 1, e);
        } 
        try {
          Enumeration keys = this.thisSessionHTTP.thisSession.user.keys();
          while (keys.hasMoreElements()) {
            String key = keys.nextElement().toString();
            if (key.startsWith("x_")) {
              Properties pp = new Properties();
              pp.put("key", key.substring(2).toUpperCase().trim());
              pp.put("value", this.thisSessionHTTP.thisSession.user.getProperty(key));
              customizations.addElement(pp);
            } 
          } 
        } catch (Exception e) {
          Log.log("HTTP_SERVER", 1, e);
        } 
        responseProp.put("user_priv_options", site);
        responseProp.put("crushftp_version", ServerStatus.version_info_str.substring(0, ServerStatus.version_info_str.indexOf(".")));
        responseProp.put("crushftp_enterprise", ServerStatus.siSG("enterprise_level"));
        responseProp.put("unique_upload_id", Common.makeBoundary(3));
        responseProp.put("display_alt_logo", ServerStatus.SG("display_alt_logo"));
        responseProp.put("random_password_length", ServerStatus.SG("random_password_length"));
        responseProp.put("min_password_length", ServerStatus.SG("min_password_length"));
        responseProp.put("min_password_numbers", ServerStatus.SG("min_password_numbers"));
        responseProp.put("min_password_lowers", ServerStatus.SG("min_password_lowers"));
        responseProp.put("min_password_uppers", ServerStatus.SG("min_password_uppers"));
        responseProp.put("min_password_specials", ServerStatus.SG("min_password_specials"));
        responseProp.put("account_expire", this.thisSessionHTTP.thisSession.SG("account_expire"));
        responseProp.put("root_dir_name", "");
        if (!this.thisSessionHTTP.thisSession.user.getProperty("EX", "").equals(""))
          responseProp.put("root_dir_name", this.thisSessionHTTP.thisSession.SG("root_dir").replace('/', ' ').trim()); 
        request.put("path", Common.url_decode(request.getProperty("path", "/")));
        String the_dir = request.getProperty("path", "/");
        if (the_dir.equals(""))
          the_dir = "/"; 
        if (!the_dir.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
          the_dir = String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + the_dir.substring(1); 
        Properties site_item = this.thisSessionHTTP.thisSession.uVFS.get_item(the_dir, 0);
        if (site_item != null) {
          if (!request.getProperty("path", "/").equals("/") && !request.getProperty("path", "/").equals("") && !request.getProperty("path", "/").equals("/ftp/")) {
            GenericClient c = this.thisSessionHTTP.thisSession.uVFS.getClient(site_item);
            try {
              c.doCommand("CWD " + request.getProperty("path", "/"));
            } finally {
              c = this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
            } 
          } 
          try {
            if (ServerStatus.BG("allow_impulse"))
              getUserInfo("IMPULSEINFO", site_item, the_dir); 
          } catch (Exception exception) {}
        } 
        responseProp.put("user_name", this.thisSessionHTTP.thisSession.uiSG("user_name"));
        responseProp.put("username", this.thisSessionHTTP.thisSession.uiSG("user_name"));
        responseProp.put("email", this.thisSessionHTTP.thisSession.user.getProperty("email", ""));
        String response = "";
        try {
          response = Common.getXMLString(responseProp, "userInfo", null);
        } catch (Exception e) {
          Log.log("HTTP_SERVER", 1, e);
        } 
        response = Common.replace_str(response, ">/WebInterface/", ">" + this.thisSessionHTTP.proxy + "WebInterface/");
        return writeResponse(response);
      } 
      if (command.equals("changePassword")) {
        String result = "Not Allowed";
        if (site.indexOf("(SITE_PASS)") >= 0) {
          String current_password = Common.url_decode(request.getProperty("current_password", "")).trim();
          String new_password1 = Common.url_decode(request.getProperty("new_password1", "")).trim();
          String new_password2 = Common.url_decode(request.getProperty("new_password2", "")).trim();
          String new_password3 = new_password1;
          String user_password = ServerStatus.thisObj.common_code.decode_pass(this.thisSessionHTTP.thisSession.user.getProperty("password"));
          if (user_password.startsWith("MD5:")) {
            current_password = ServerStatus.thisObj.common_code.encode_pass(current_password, "MD5", this.thisSessionHTTP.thisSession.user.getProperty("salt", ""));
            new_password3 = ServerStatus.thisObj.common_code.encode_pass(new_password3, "MD5", this.thisSessionHTTP.thisSession.user.getProperty("salt", ""));
          } else if (user_password.startsWith("SHA:")) {
            current_password = ServerStatus.thisObj.common_code.encode_pass(current_password, "SHA", this.thisSessionHTTP.thisSession.user.getProperty("salt", ""));
            new_password3 = ServerStatus.thisObj.common_code.encode_pass(new_password3, "SHA", this.thisSessionHTTP.thisSession.user.getProperty("salt", ""));
          } else if (user_password.startsWith("SHA512:")) {
            current_password = ServerStatus.thisObj.common_code.encode_pass(current_password, "SHA512", this.thisSessionHTTP.thisSession.user.getProperty("salt", ""));
            new_password3 = ServerStatus.thisObj.common_code.encode_pass(new_password3, "SHA512", this.thisSessionHTTP.thisSession.user.getProperty("salt", ""));
          } else if (user_password.startsWith("SHA3:")) {
            current_password = ServerStatus.thisObj.common_code.encode_pass(current_password, "SHA3", this.thisSessionHTTP.thisSession.user.getProperty("salt", ""));
            new_password3 = ServerStatus.thisObj.common_code.encode_pass(new_password3, "SHA3", this.thisSessionHTTP.thisSession.user.getProperty("salt", ""));
          } else if (user_password.startsWith("CRYPT3:")) {
            if (ServerStatus.thisObj.common_code.crypt3(current_password, user_password).equals(user_password))
              current_password = user_password; 
          } else if (user_password.startsWith("BCRYPT:")) {
            if (ServerStatus.thisObj.common_code.bcrypt(current_password, user_password).equals(user_password))
              current_password = user_password; 
          } else if (user_password.startsWith("MD5CRYPT:")) {
            if (ServerStatus.thisObj.common_code.md5crypt(current_password, user_password).equals(user_password))
              current_password = user_password; 
          } else if (user_password.startsWith("SHA512CRYPT:")) {
            if (ServerStatus.thisObj.common_code.sha512crypt(current_password, user_password).equals(user_password))
              current_password = user_password; 
          } 
          if (current_password.length() > 0 && new_password1.length() > 0)
            if (current_password.equals(user_password) && new_password1.equals(new_password2) && !new_password3.equals(user_password) && !this.thisSessionHTTP.thisSession.uiSG("user_name").equalsIgnoreCase("anonymous")) {
              this.thisSessionHTTP.thisSession.uiPUT("current_password", user_password);
              result = this.thisSessionHTTP.thisSession.do_ChangePass(this.thisSessionHTTP.thisSession.uiSG("user_name"), new_password1);
            } else if (!current_password.equals(user_password)) {
              result = LOC.G("You did not enter the correct current password.");
            } else if (!new_password1.equals(new_password2)) {
              result = LOC.G("You did not enter the same password for verification the second time.");
            }  
        } 
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        response = String.valueOf(response) + "<commandResult><response>" + result + "</response></commandResult>";
        return writeResponse(response);
      } 
      if (request.getProperty("the_action", "").equalsIgnoreCase("FTPPROXY")) {
        if (site.indexOf("(SITE_WEBFTPPROXY)") >= 0) {
          String ip = request.getProperty("ip", "");
          String username = Common.replace_str(request.getProperty("user", ""), "---PERCENT---", "%");
          String password = Common.replace_str(request.getProperty("pass", ""), "---PERCENT---", "%");
          username = Common.replace_str(username, "---PLUS---", "+");
          password = Common.replace_str(password, "---PLUS---", "+");
          String path = request.getProperty("path", "");
          String port = request.getProperty("port", "21");
          String protocol = request.getProperty("protocol", "ftp").toLowerCase();
          if (!path.endsWith("/"))
            path = String.valueOf(path) + "/"; 
          if (!path.startsWith("/"))
            path = "/" + path; 
          Vector proxyRules = ServerStatus.VG("proxyRules");
          boolean ok = (site.indexOf("(SITE_ADHOCWEBFTPPROXY)") >= 0);
          boolean blacklisted = false;
          for (int x = 0; x < proxyRules.size(); x++) {
            Properties pp = proxyRules.elementAt(x);
            if (Common.do_search(pp.getProperty("host"), ip, false, 0)) {
              String part1 = ServerStatus.thisObj.change_vars_to_values(pp.getProperty("criteria1"), this.thisSessionHTTP.thisSession);
              String part2 = ServerStatus.thisObj.change_vars_to_values(pp.getProperty("criteria2"), this.thisSessionHTTP.thisSession);
              if ((Common.do_search(part1, part2, false, 0) || Common.do_search(part2, part1, false, 0)) && pp.getProperty("condition").equals("=")) {
                ok = true;
                break;
              } 
              if (!Common.do_search(part1, part2, false, 0) && !Common.do_search(part2, part1, false, 0) && pp.getProperty("condition").equals("!=")) {
                ok = true;
                break;
              } 
              if (part1.equals("1") && part2.equals("2") && pp.getProperty("condition").equals("=")) {
                ServerStatus.thisObj.runAlerts("proxy_blacklist", this.thisSessionHTTP.thisSession);
                blacklisted = true;
                break;
              } 
            } 
          } 
          if (ok && !blacklisted) {
            Properties virtual = this.thisSessionHTTP.thisSession.uVFS.homes.elementAt(0);
            Vector v = new Vector();
            Properties dir_item = new Properties();
            dir_item.put("url", String.valueOf(protocol) + "://" + Common.url_encode(username) + ":" + Common.url_encode(password) + "@" + ip + ":" + port + path);
            dir_item.put("type", "DIR");
            dir_item.put("no_stat", "true");
            v.addElement(dir_item);
            Properties vItem = new Properties();
            vItem.put("virtualPath", "/ftp");
            vItem.put("name", "ftp");
            vItem.put("type", "FILE");
            vItem.put("vItems", v);
            virtual.put("/ftp", vItem);
            this.thisSessionHTTP.thisSession.uVFS.reset();
            this.thisSessionHTTP.thisSession.add_log_formatted("Proxy created to URL:ftp://" + Common.url_encode(username) + ":********************@" + ip + path, "POST", req_id);
            this.thisSessionHTTP.sendRedirect("/ftp/");
          } else {
            this.thisSessionHTTP.sendRedirect("/");
          } 
          write_command_http("Connection: close");
          write_command_http("Content-Length: 0");
          write_command_http("");
          return true;
        } 
      } else {
        if (request.getProperty("command", "").equalsIgnoreCase("agentList"))
          return writeResponse(Common.getXMLString(new Vector(), "agents", null)); 
        if (request.getProperty("command", "").equalsIgnoreCase("getServerRoots"))
          return writeResponse("<commandResult><response>FAILURE:Access Denied.</response></commandResult>"); 
      } 
    } 
    return false;
  }
  
  public void addMissingButtons(Vector buttons) throws Exception {
    boolean found = false;
    for (int x = 0; x < buttons.size(); x++) {
      if (buttons.elementAt(x).toString().indexOf("admin") >= 0)
        found = true; 
    } 
    if (!found) {
      Properties properties = new Properties();
      properties.put("key", "(admin):Admin");
      properties.put("value", "");
      properties.put("for_menu", "true");
      properties.put("for_context_menu", "false");
      if (this.thisSessionHTTP.thisSession.SG("site").indexOf("(CONNECT)") >= 0 || this.thisSessionHTTP.thisSession.SG("site").indexOf("(SERVER_VIEW)") >= 0 || this.thisSessionHTTP.thisSession.SG("site").indexOf("(SERVER_EDIT)") >= 0 || this.thisSessionHTTP.thisSession.SG("site").indexOf("(UPDATE_RUN)") >= 0) {
        properties.put("value", String.valueOf(this.thisSessionHTTP.proxy) + "WebInterface/admin/index.html");
      } else if (this.thisSessionHTTP.thisSession.SG("site").indexOf("(USER_ADMIN)") >= 0 || this.thisSessionHTTP.thisSession.SG("site").indexOf("(USER_VIEW)") >= 0 || this.thisSessionHTTP.thisSession.SG("site").indexOf("(USER_EDIT)") >= 0 || this.thisSessionHTTP.thisSession.SG("site").indexOf("(UPDATE_RUN)") >= 0) {
        properties.put("value", String.valueOf(this.thisSessionHTTP.proxy) + "WebInterface/UserManager/index.html");
      } else if (this.thisSessionHTTP.thisSession.SG("site").indexOf("(PREF_VIEW)") >= 0 || this.thisSessionHTTP.thisSession.SG("site").indexOf("(PREF_EDIT)") >= 0) {
        properties.put("value", String.valueOf(this.thisSessionHTTP.proxy) + "WebInterface/Preferences/index.html");
      } else if (this.thisSessionHTTP.thisSession.SG("site").indexOf("(SHARE_VIEW)") >= 0 || this.thisSessionHTTP.thisSession.SG("site").indexOf("(SHARE_EDIT)") >= 0) {
        properties.put("value", String.valueOf(this.thisSessionHTTP.proxy) + "WebInterface/TempAccounts/index.html");
      } else if (this.thisSessionHTTP.thisSession.SG("site").indexOf("(REPORT_VIEW)") >= 0 || this.thisSessionHTTP.thisSession.SG("site").indexOf("(REPORT_EDIT)") >= 0) {
        properties.put("value", String.valueOf(this.thisSessionHTTP.proxy) + "WebInterface/admin/index.html");
      } else if (this.thisSessionHTTP.thisSession.SG("site").indexOf("(JOB_EDIT)") >= 0 || this.thisSessionHTTP.thisSession.SG("site").indexOf("(JOB_VIEW)") >= 0 || this.thisSessionHTTP.thisSession.SG("site").indexOf("(JOB_LIST)") >= 0 || this.thisSessionHTTP.thisSession.SG("site").indexOf("(JOB_LIST_HISTORY)") >= 0) {
        properties.put("value", String.valueOf(this.thisSessionHTTP.proxy) + "WebInterface/Jobs/index.html");
      } 
      if (!properties.getProperty("value", "").equals(""))
        buttons.insertElementAt((E)properties, 0); 
    } 
    Properties button = new Properties();
    button.put("key", "(copyDirectLink):Copy Link");
    button.put("for_menu", "false");
    button.put("for_context_menu", "true");
    button.put("value", "javascript:performAction('copyDirectLink');");
    buttons.insertElementAt((E)button, 0);
  }
  
  public String getXmlListingResponse(String username, Properties request, String the_dir, boolean realPaths, VFS tmpVFS) throws Exception {
    Vector listing = null;
    the_dir = Common.dots(the_dir);
    Properties item = tmpVFS.get_item(the_dir);
    if (item == null)
      item = tmpVFS.get_item_parent(the_dir); 
    String privs = "";
    if (item != null)
      privs = item.getProperty("privs", ""); 
    Properties listingProp = new Properties();
    if (!this.thisSessionHTTP.thisSession.BG("DisallowListingDirectories")) {
      if (item != null && item.getProperty("url", "").indexOf("|") >= 0)
        if (item.getProperty("proxy_item", "false").equals("true")) {
          if (item.getProperty("permissions").charAt(2) != 'w')
            privs = Common.replace_str(privs, "(write)", ""); 
          if (item.getProperty("permissions").charAt(2) == 'w')
            privs = Common.replace_str(privs, "(read)", ""); 
        }  
      listingProp = getListingInfo(listing, the_dir, privs, false, tmpVFS, realPaths, true, this.thisSessionHTTP.thisSession, false);
      if (listingProp.get("listing") == null)
        Log.log("HTTP_SERVER", 2, "getXMLListing:Got listing of:" + ((Vector)listingProp.get("listing")).size()); 
      if ((new VRL(item.getProperty("url", ""))).getProtocol().equalsIgnoreCase("file")) {
        File temp_dir = new File(String.valueOf((new VRL(item.getProperty("url"))).getPath()) + "/.message");
        if (temp_dir.exists()) {
          RandomAccessFile message_is = new RandomAccessFile(temp_dir.getAbsolutePath(), "r");
          byte[] temp_array = new byte[(int)message_is.length()];
          message_is.readFully(temp_array);
          message_is.close();
          listingProp.put("comment", new String(temp_array));
        } 
      } 
    } 
    if (ServerStatus.server_settings.get("defaultStrings") != null && ServerStatus.server_settings.get("defaultStrings") instanceof Properties)
      listingProp.put("defaultStrings", ServerStatus.server_settings.get("defaultStrings")); 
    listingProp.put("site", this.thisSessionHTTP.thisSession.SG("site"));
    listingProp.put("quota", "");
    long quota = -12345L;
    if (Common.System2.get("crushftp.dmz.queue") != null) {
      Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
      Properties action = new Properties();
      action.put("type", "GET:QUOTA");
      action.put("id", Common.makeBoundary());
      action.put("username", this.thisSessionHTTP.thisSession.uiSG("user_name"));
      action.put("password", this.thisSessionHTTP.thisSession.uiSG("current_password"));
      action.put("request", request);
      String the_dir2 = the_dir;
      if (the_dir2.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
        the_dir2 = the_dir2.substring(this.thisSessionHTTP.thisSession.SG("root_dir").length() - 1); 
      action.put("the_dir", the_dir2);
      action.put("clientid", this.thisSessionHTTP.thisSession.uiSG("clientid"));
      action.put("need_response", "true");
      queue.addElement(action);
      action = UserTools.waitResponse(action, 300);
      quota = Long.parseLong(action.remove("object_response").toString().split(":")[0]);
    } else {
      quota = this.thisSessionHTTP.thisSession.get_quota(the_dir);
    } 
    if (quota != -12345L) {
      listingProp.put("quota", Common.format_bytes_short2(quota));
      listingProp.put("quota_bytes", (new StringBuffer(String.valueOf(quota))).toString());
    } 
    listingProp.put("bytes_sent", (new StringBuffer(String.valueOf(this.thisSessionHTTP.thisSession.IG("bytes_sent")))).toString());
    listingProp.put("bytes_received", (new StringBuffer(String.valueOf(this.thisSessionHTTP.thisSession.IG("bytes_received")))).toString());
    listingProp.put("max_upload_size", (new StringBuffer(String.valueOf(this.thisSessionHTTP.thisSession.IG("max_upload_size")))).toString());
    listingProp.put("max_upload_amount_day", (new StringBuffer(String.valueOf(this.thisSessionHTTP.thisSession.IG("max_upload_amount_day")))).toString());
    listingProp.put("max_upload_amount_month", (new StringBuffer(String.valueOf(this.thisSessionHTTP.thisSession.IG("max_upload_amount_month")))).toString());
    listingProp.put("max_upload_amount", (new StringBuffer(String.valueOf(this.thisSessionHTTP.thisSession.IG("max_upload_amount")))).toString());
    listingProp.put("max_upload_amount_available", (new StringBuffer(String.valueOf(this.thisSessionHTTP.thisSession.IG("max_upload_amount") - this.thisSessionHTTP.thisSession.IG("bytes_received")))).toString());
    listingProp.put("max_download_amount", (new StringBuffer(String.valueOf(this.thisSessionHTTP.thisSession.IG("max_download_amount")))).toString());
    listingProp.put("max_download_amount_day", (new StringBuffer(String.valueOf(this.thisSessionHTTP.thisSession.IG("max_download_amount_day")))).toString());
    listingProp.put("max_download_amount_month", (new StringBuffer(String.valueOf(this.thisSessionHTTP.thisSession.IG("max_download_amount_month")))).toString());
    listingProp.put("max_download_amount_available", (new StringBuffer(String.valueOf(this.thisSessionHTTP.thisSession.IG("max_download_amount") - this.thisSessionHTTP.thisSession.IG("bytes_sent")))).toString());
    if (ServerStatus.BG("calculate_transfer_usage_listings")) {
      if (this.thisSessionHTTP.thisSession.IG("max_upload_amount_day") > 0)
        listingProp.put("max_upload_amount_day_available", (new StringBuffer(String.valueOf(this.thisSessionHTTP.thisSession.IG("max_upload_amount_day") * 1024L - ServerStatus.thisObj.statTools.getTransferAmountToday(this.thisSessionHTTP.thisSession.uiSG("user_ip"), this.thisSessionHTTP.thisSession.uiSG("user_name"), this.thisSessionHTTP.thisSession.uiPG("stat"), "uploads", this.thisSessionHTTP.thisSession)))).toString()); 
      if (this.thisSessionHTTP.thisSession.IG("max_upload_amount_month") > 0)
        listingProp.put("max_upload_amount_month_available", (new StringBuffer(String.valueOf(this.thisSessionHTTP.thisSession.IG("max_upload_amount_month") * 1024L - ServerStatus.thisObj.statTools.getTransferAmountThisMonth(this.thisSessionHTTP.thisSession.uiSG("user_ip"), this.thisSessionHTTP.thisSession.uiSG("user_name"), this.thisSessionHTTP.thisSession.uiPG("stat"), "uploads", this.thisSessionHTTP.thisSession)))).toString()); 
      if (this.thisSessionHTTP.thisSession.IG("max_download_amount_day") > 0)
        listingProp.put("max_download_amount_day_available", (new StringBuffer(String.valueOf(this.thisSessionHTTP.thisSession.IG("max_download_amount_day") * 1024L - ServerStatus.thisObj.statTools.getTransferAmountToday(this.thisSessionHTTP.thisSession.uiSG("user_ip"), this.thisSessionHTTP.thisSession.uiSG("user_name"), this.thisSessionHTTP.thisSession.uiPG("stat"), "downloads", this.thisSessionHTTP.thisSession)))).toString()); 
      if (this.thisSessionHTTP.thisSession.IG("max_download_amount_month") > 0)
        listingProp.put("max_download_amount_month_available", (new StringBuffer(String.valueOf(this.thisSessionHTTP.thisSession.IG("max_download_amount_month") * 1024L - ServerStatus.thisObj.statTools.getTransferAmountThisMonth(this.thisSessionHTTP.thisSession.uiSG("user_ip"), this.thisSessionHTTP.thisSession.uiSG("user_name"), this.thisSessionHTTP.thisSession.uiPG("stat"), "downloads", this.thisSessionHTTP.thisSession)))).toString()); 
      if (this.thisSessionHTTP.thisSession.IG("ratio") != 0)
        listingProp.put("ratio_available", (new StringBuffer(String.valueOf(((this.thisSessionHTTP.thisSession.uiLG("bytes_received") + this.thisSessionHTTP.thisSession.uiLG("ratio_bytes_received")) * this.thisSessionHTTP.thisSession.IG("ratio") - this.thisSessionHTTP.thisSession.uiLG("bytes_sent") + this.thisSessionHTTP.thisSession.uiLG("ratio_bytes_sent")) / 1024L))).toString()); 
    } 
    String altList = "";
    if (request.getProperty("format", "").equalsIgnoreCase("JSON")) {
      altList = AgentUI.getJsonList(listingProp, ServerStatus.BG("exif_listings"), false);
    } else if (request.getProperty("format", "").equalsIgnoreCase("JSONOBJ")) {
      altList = AgentUI.getJsonListObj(listingProp, ServerStatus.BG("exif_listings"));
    } else if (request.getProperty("format", "").equalsIgnoreCase("STAT")) {
      altList = AgentUI.getStatList(listingProp);
    } else if (request.getProperty("format", "").equalsIgnoreCase("STAT_DMZ")) {
      altList = AgentUI.getDmzList(listingProp);
    } 
    if (request.getProperty("format", "").equalsIgnoreCase("JSONOBJ")) {
      Properties combinedPermissions = tmpVFS.getCombinedPermissions();
      boolean aclPermissions = combinedPermissions.getProperty("acl_permissions", "false").equals("true");
      privs = listingProp.getProperty("privs", "");
      if (aclPermissions) {
        privs = tmpVFS.getPriv(the_dir, item);
      } else if ((new VRL(item.getProperty("url"))).getProtocol().toUpperCase().startsWith("HTTP")) {
        Vector list2 = new Vector();
        tmpVFS.getListing(list2, Common.all_but_last(the_dir), 1, 9999, true);
        for (int x = 0; x < list2.size(); x++) {
          Properties p = list2.elementAt(x);
          if (p != null && 
            p.getProperty("name", "").equals(item.getProperty("name", ""))) {
            privs = p.getProperty("privs");
            break;
          } 
        } 
      } 
      String info = "{\r\n";
      info = String.valueOf(info) + "\t\"privs\" : \"" + privs.trim().replaceAll("\\r", "%0D").replaceAll("\\n", "%0A").replaceAll("\"", "%22").replaceAll("\t", "%09") + "\",\r\n";
      info = String.valueOf(info) + "\t\"comment\" : \"" + Common.url_encode(listingProp.getProperty("comment", "").trim()) + "\",\r\n";
      info = String.valueOf(info) + "\t\"path\" : \"" + listingProp.getProperty("path", "").trim().replaceAll("\"", "%22").replaceAll("\t", "%09") + "\",\r\n";
      info = String.valueOf(info) + "\t\"defaultStrings\" : \"" + listingProp.getProperty("defaultStrings", "").trim() + "\",\r\n";
      info = String.valueOf(info) + "\t\"site\" : \"" + listingProp.getProperty("site", "").trim() + "\",\r\n";
      info = String.valueOf(info) + "\t\"quota\" : \"" + listingProp.getProperty("quota", "").trim() + "\",\r\n";
      info = String.valueOf(info) + "\t\"quota_bytes\" : \"" + listingProp.getProperty("quota_bytes", "").trim() + "\",\r\n";
      info = String.valueOf(info) + "\t\"bytes_sent\" : \"" + listingProp.getProperty("bytes_sent", "").trim() + "\",\r\n";
      info = String.valueOf(info) + "\t\"bytes_received\" : \"" + listingProp.getProperty("bytes_received", "").trim() + "\",\r\n";
      info = String.valueOf(info) + "\t\"max_upload_amount_day\" : \"" + listingProp.getProperty("max_upload_amount_day", "").trim() + "\",\r\n";
      info = String.valueOf(info) + "\t\"max_upload_amount_month\" : \"" + listingProp.getProperty("max_upload_amount_month", "").trim() + "\",\r\n";
      info = String.valueOf(info) + "\t\"max_upload_amount\" : \"" + listingProp.getProperty("max_upload_amount", "").trim() + "\",\r\n";
      info = String.valueOf(info) + "\t\"max_upload_amount_available\" : \"" + listingProp.getProperty("max_upload_amount_available", "").trim() + "\",\r\n";
      info = String.valueOf(info) + "\t\"max_upload_amount_day_available\" : \"" + listingProp.getProperty("max_upload_amount_day_available", "").trim() + "\",\r\n";
      info = String.valueOf(info) + "\t\"max_upload_amount_month_available\" : \"" + listingProp.getProperty("max_upload_amount_month_available", "").trim() + "\",\r\n";
      info = String.valueOf(info) + "\t\"max_download_amount\" : \"" + listingProp.getProperty("max_download_amount", "").trim() + "\",\r\n";
      info = String.valueOf(info) + "\t\"max_download_amount_day\" : \"" + listingProp.getProperty("max_download_amount_day", "").trim() + "\",\r\n";
      info = String.valueOf(info) + "\t\"max_download_amount_month\" : \"" + listingProp.getProperty("max_download_amount_month", "").trim() + "\",\r\n";
      info = String.valueOf(info) + "\t\"max_download_amount_available\" : \"" + listingProp.getProperty("max_download_amount_available", "").trim() + "\",\r\n";
      info = String.valueOf(info) + "\t\"max_download_amount_day_available\" : \"" + listingProp.getProperty("max_download_amount_day_available", "").trim() + "\",\r\n";
      info = String.valueOf(info) + "\t\"max_download_amount_month_available\" : \"" + listingProp.getProperty("max_download_amount_month_available", "").trim() + "\",\r\n";
      info = String.valueOf(info) + "\t\"ratio_available\" : \"" + listingProp.getProperty("ratio_available", "0").trim() + "\",\r\n";
      info = String.valueOf(info) + "\t\"listing\" : " + altList + "\r\n";
      info = String.valueOf(info) + "}\r\n";
      return info;
    } 
    String response = "";
    try {
      response = Common.getXMLString(listingProp, "listingInfo", null);
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
    if (!altList.equals(""))
      response = String.valueOf(response.substring(0, response.indexOf("</privs>") + "</privs>".length())) + altList + response.substring(response.indexOf("</privs>") + "</privs>".length()); 
    return response;
  }
  
  public static Properties getListingInfo(Vector listing, String the_dir, String privs, boolean ignoreRootDir, VFS tmpVFS, boolean realPaths, boolean hideHidden, SessionCrush thisSession, boolean allowDuplicates) {
    Vector items = new Vector();
    try {
      thisSession.date_time = SessionCrush.updateDateCustomizations(thisSession.date_time, thisSession.user);
      if (listing == null) {
        listing = new Vector();
        Log.log("HTTP_SERVER", 3, "Getting dir listing for:" + thisSession.uiSG("user_name") + " with VFS from:" + tmpVFS);
        Log.log("HTTP_SERVER", 3, Thread.currentThread().getName());
        Log.log("HTTP_SERVER", 3, new Exception("From where?"));
        tmpVFS.getListing(listing, the_dir);
        Log.log("HTTP_SERVER", 3, "Found " + listing.size() + " items in " + the_dir + ".");
      } 
      SimpleDateFormat date_time = (SimpleDateFormat)thisSession.date_time.clone();
      Properties pp = new Properties();
      pp.put("listing", listing);
      thisSession.runPlugin("list", pp);
      Properties name_hash = new Properties();
      if (privs.toUpperCase().indexOf("(VIEW)") >= 0) {
        Log.log("HTTP_SERVER", 3, "Going through listing checking filters...");
        if (thisSession.check_access_privs(the_dir, "LIST")) {
          Calendar cal = new GregorianCalendar();
          for (int i = 0; i < listing.size(); i++) {
            Properties list_item = (Properties)((Properties)listing.elementAt(i)).clone();
            if (!list_item.getProperty("hide_smb", "false").equals("true")) {
              if (thisSession.IG("timezone_offset") != 0) {
                Date d = new Date(Long.parseLong(list_item.getProperty("modified")));
                cal.setTime(d);
                cal.add(11, thisSession.IG("timezone_offset"));
                list_item.put("modified", (new StringBuffer(String.valueOf(cal.getTime().getTime()))).toString());
              } 
              if (Common.filter_check("L", list_item.getProperty("name"), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + thisSession.SG("file_filter")))
                if (name_hash.get(list_item.getProperty("name")) == null && (list_item.getProperty("privs").toLowerCase().indexOf("(invisible)") < 0 || (list_item.getProperty("privs").toLowerCase().indexOf("(invisible)") >= 0 && list_item.getProperty("privs").toLowerCase().indexOf("(inherited)") >= 0) || !hideHidden)) {
                  if (!allowDuplicates)
                    name_hash.put(list_item.getProperty("name"), "DONE"); 
                  if (!list_item.containsKey("preview")) {
                    if (ServerStatus.BG("find_list_previews")) {
                      String preview_the_dir = "/this_dir_does_not_exist";
                      try {
                        if (list_item.getProperty("url").startsWith("virtual://")) {
                          Properties tmpItem = tmpVFS.get_item(String.valueOf(list_item.getProperty("root_dir")) + list_item.getProperty("name"));
                          if (tmpItem != null)
                            list_item = tmpItem; 
                        } 
                        preview_the_dir = SearchHandler.getPreviewPath(list_item, "1", 1);
                      } catch (Exception e) {
                        Log.log("HTTP_SERVER", 1, e);
                        Log.log("HTTP_SERVER", 1, list_item.toString());
                      } 
                      String index = "";
                      if (preview_the_dir != null) {
                        index = String.valueOf(ServerStatus.SG("previews_path")) + preview_the_dir.substring(1);
                        list_item.put("keywords", "");
                      } 
                      if (preview_the_dir != null && (new File(Common.all_but_last(Common.all_but_last(index)))).exists()) {
                        String preview_the_dir_parent = Common.all_but_last(Common.all_but_last(preview_the_dir));
                        int frames = 1;
                        while ((new File(String.valueOf(ServerStatus.SG("previews_path")) + preview_the_dir_parent + "p" + frames)).exists())
                          frames++; 
                        if (list_item.getProperty("name").toUpperCase().endsWith(".ZIP") && !ServerStatus.BG("zip_icon_preview_allowed")) {
                          list_item.put("preview", "0");
                        } else {
                          list_item.put("preview", (new StringBuffer(String.valueOf(frames - 1))).toString());
                        } 
                        String indexText = "";
                        if (ServerStatus.BG("exif_keywords")) {
                          if ((new File(String.valueOf(Common.all_but_last(Common.all_but_last(index))) + "info.xml")).exists()) {
                            Properties info = (Properties)Common.readXMLObject(new File(String.valueOf(Common.all_but_last(Common.all_but_last(index))) + "info.xml"));
                            if (info != null)
                              indexText = info.getProperty("keywords", ""); 
                          } 
                        } else if ((new File(String.valueOf(Common.all_but_last(Common.all_but_last(index))) + "index.txt")).exists()) {
                          RandomAccessFile out = new RandomAccessFile(String.valueOf(Common.all_but_last(Common.all_but_last(index))) + "index.txt", "r");
                          byte[] b = new byte[(int)out.length()];
                          out.readFully(b);
                          out.close();
                          indexText = new String(b);
                        } 
                        if (ServerStatus.BG("exif_listings"))
                          if ((new File(String.valueOf(Common.all_but_last(Common.all_but_last(index))) + "info.xml")).exists()) {
                            Properties info = (Properties)Common.readXMLObject(new File(String.valueOf(Common.all_but_last(Common.all_but_last(index))) + "info.xml"));
                            if (info != null) {
                              list_item.put("width", info.getProperty("imagewidth", ""));
                              list_item.put("height", info.getProperty("imageheight", ""));
                              Enumeration keys = info.keys();
                              while (keys.hasMoreElements()) {
                                String key = (String)keys.nextElement();
                                if (key.startsWith("crushftp_"))
                                  list_item.put(key, info.getProperty(key)); 
                              } 
                            } 
                          }  
                        list_item.put("keywords", (indexText == null) ? "" : indexText);
                        if (!(new File(String.valueOf(ServerStatus.SG("previews_path")) + preview_the_dir_parent + "p1/1.jpg")).exists())
                          list_item.put("preview", "0"); 
                      } else {
                        list_item.put("preview", "0");
                      } 
                    } else {
                      list_item.put("keywords", "");
                      list_item.put("preview", "0");
                    } 
                    if (list_item.getProperty("type", "").equals("DIR") && thisSession.BG("dir_calc")) {
                      Vector inside_a_dir_list = new Vector();
                      thisSession.uVFS.getListing(inside_a_dir_list, String.valueOf(the_dir) + list_item.getProperty("name") + "/");
                      for (int xx = 0; xx < inside_a_dir_list.size(); xx++) {
                        Properties adder = inside_a_dir_list.elementAt(xx);
                        list_item.put("size", "0" + (Long.parseLong(list_item.getProperty("size", "")) + Long.parseLong(adder.getProperty("size"))));
                      } 
                      list_item.put("num_items", inside_a_dir_list.size());
                    } else if (list_item.getProperty("type", "").equals("DIR") && thisSession.BG("dir_calc_count")) {
                      Vector inside_a_dir_list = new Vector();
                      Properties list_item2 = list_item;
                      Properties status = new Properties();
                      Worker.startWorker(new Runnable(thisSession, inside_a_dir_list, the_dir, list_item2, status) {
                            private final SessionCrush val$thisSession;
                            
                            private final Vector val$inside_a_dir_list;
                            
                            private final String val$the_dir;
                            
                            private final Properties val$list_item2;
                            
                            private final Properties val$status;
                            
                            public void run() {
                              try {
                                this.val$thisSession.uVFS.getListing(this.val$inside_a_dir_list, String.valueOf(this.val$the_dir) + this.val$list_item2.getProperty("name") + "/", 1, 1000, true);
                              } catch (Exception e) {
                                Log.log("SERVER", 1, e);
                              } 
                              this.val$status.put("done", "true");
                            }
                          });
                      int count = 0;
                      while (inside_a_dir_list.size() > 0 || !status.containsKey("done")) {
                        if (inside_a_dir_list.size() > 0) {
                          inside_a_dir_list.remove(0);
                          count++;
                          continue;
                        } 
                        Thread.sleep(100L);
                      } 
                      list_item2.put("num_items", count);
                    } 
                    list_item.put("sizeFormatted", Common.format_bytes2(list_item.getProperty("size")));
                    list_item.put("date", String.valueOf(list_item.getProperty("month", "")) + " " + list_item.getProperty("day", "") + " " + list_item.getProperty("time_or_year", ""));
                    Date d = new Date(Long.parseLong(list_item.getProperty("modified", "0")));
                    if (d.getTime() < 30000000L) {
                      list_item.put("dateFormatted", "");
                    } else {
                      list_item.put("dateFormatted", date_time.format(d));
                    } 
                    list_item.put("modified", list_item.getProperty("modified", "0"));
                    if (list_item.getProperty("privs").indexOf("(comment") >= 0) {
                      String comment = Common.url_decode(list_item.getProperty("privs").substring(list_item.getProperty("privs").indexOf("(comment") + 8, list_item.getProperty("privs").indexOf(")", list_item.getProperty("privs").indexOf("(comment"))));
                      list_item.put("comment", ServerStatus.thisObj.change_vars_to_values(comment.trim(), thisSession));
                    } 
                  } 
                  list_item.remove("url");
                  list_item.put("itemType", list_item.getProperty("type"));
                  String the_dir2 = list_item.getProperty("root_dir", "/");
                  if (the_dir2.equals("/"))
                    the_dir2 = thisSession.SG("root_dir"); 
                  if (!ignoreRootDir) {
                    if (the_dir2.startsWith("/") && !the_dir2.toUpperCase().startsWith(thisSession.SG("root_dir").toUpperCase()))
                      the_dir2 = String.valueOf(thisSession.SG("root_dir")) + the_dir2.substring(1); 
                    the_dir2 = the_dir2.substring(thisSession.SG("root_dir").length() - 1);
                  } 
                  list_item.put("root_dir", the_dir2);
                  if (list_item.getProperty("privs").indexOf("(inherited)") < 0 && thisSession.uVFS.getCombinedPermissions().getProperty("acl_permissions", "false").equals("false")) {
                    list_item.put("privs", Common.replace_str(list_item.getProperty("privs", ""), "(delete)", ""));
                    list_item.put("privs", Common.replace_str(list_item.getProperty("privs", ""), "(rename)", ""));
                  } 
                  items.addElement(list_item);
                }  
            } 
          } 
        } 
      } 
    } catch (Exception e) {
      thisSession.uVFS.reset();
      tmpVFS.reset();
      Log.log("HTTP_SERVER", 1, e);
    } 
    Common.do_sort(items, "name");
    for (int x = 0; x < items.size(); x++) {
      Properties lp = items.elementAt(x);
      if (lp.getProperty("dir", "").indexOf("\"") >= 0)
        lp.put("dir", lp.getProperty("dir", "").replaceAll("\\\"", "%22").replaceAll("\t", "%09")); 
      if (lp.getProperty("name", "").indexOf("\"") >= 0)
        lp.put("name", lp.getProperty("name", "").replaceAll("\\\"", "%22").replaceAll("\t", "%09")); 
      if (lp.getProperty("name", "").endsWith(" ") || lp.getProperty("name", "").startsWith(" "))
        lp.put("name", lp.getProperty("name", "").replaceAll(" ", "%20")); 
      if (lp.getProperty("path", "").indexOf("\"") >= 0)
        lp.put("path", lp.getProperty("path", "").replaceAll("\\\"", "%22").replaceAll("\t", "%09")); 
      if (lp.getProperty("root_dir", "").indexOf("\"") >= 0)
        lp.put("root_dir", lp.getProperty("root_dir", "").replaceAll("\\\"", "%22").replaceAll("\t", "%09")); 
      String itemName = lp.getProperty("name");
      String str1 = String.valueOf(the_dir) + lp.getProperty("name");
      if (realPaths)
        try {
          Properties tmpItem = tmpVFS.get_item(str1);
          VRL vrl = new VRL(tmpItem.getProperty("url"));
          lp.put("root_dir", Common.all_but_last(vrl.getPath()));
        } catch (Exception exception) {} 
      String root_dir = lp.getProperty("root_dir");
      String href_path = String.valueOf(lp.getProperty("root_dir")) + lp.getProperty("name");
      if (href_path.startsWith("//") && !href_path.startsWith("////"))
        href_path = "//" + href_path; 
      try {
        lp.put("source", "/WebInterface/function/?command=getPreview&size=3&path=" + str1);
      } catch (Exception exception) {}
      try {
        lp.put("href_path", href_path);
      } catch (Exception exception) {}
      try {
        lp.put("root_dir", root_dir);
      } catch (Exception exception) {}
      try {
        lp.put("name", itemName);
      } catch (Exception exception) {}
    } 
    Properties listingProp = new Properties();
    listingProp.put("privs", privs);
    String itemPath = the_dir;
    try {
      listingProp.put("path", itemPath);
    } catch (Exception exception) {}
    listingProp.put("listing", items);
    return listingProp;
  }
  
  public String downloadItem(VRL otherFile, Properties item, String fileName, Vector byteRanges, boolean simpleRanges, String mimeType) throws Exception {
    if (byteRanges.size() > 0) {
      write_command_http("HTTP/1.1 206 Partial Content");
    } else {
      write_command_http("HTTP/1.1 200 OK");
    } 
    this.thisSessionHTTP.write_standard_headers();
    String byteRangeBoundary = Common.makeBoundary();
    String contentType = "application/binary";
    if (!mimeType.equals(""))
      contentType = mimeType; 
    if (byteRanges.size() <= 1 || simpleRanges) {
      write_command_http("Content-Type: " + contentType);
    } else if (byteRanges.size() > 1) {
      write_command_http("Content-Type: multipart/byteranges; boundary=" + byteRangeBoundary);
    } 
    fileName = Common.replace_str(Common.url_decode(fileName), "\r", "_");
    if (contentType.equals("application/binary"))
      write_command_http("Content-Disposition: attachment; filename=\"" + ((this.thisSessionHTTP.thisSession.uiSG("header_user-agent").toUpperCase().indexOf("MSIE") >= 0 || this.thisSessionHTTP.thisSession.uiSG("header_user-agent").toUpperCase().indexOf("TRIDENT") >= 0) ? Common.url_encode(fileName) : fileName) + "\""); 
    write_command_http("X-UA-Compatible: chrome=1");
    GenericClient c = this.thisSessionHTTP.thisSession.uVFS.getClient(item);
    Properties stat = null;
    try {
      stat = c.stat(otherFile.getPath());
    } finally {
      c = this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
    } 
    write_command_http("Last-Modified: " + this.thisSessionHTTP.sdf_rfc1123.format(new Date(Long.parseLong(stat.getProperty("modified")))));
    write_command_http("ETag: " + Long.parseLong(stat.getProperty("modified")));
    String amountEnd = stat.getProperty("size");
    for (int x = 0; x < byteRanges.size(); x++) {
      Properties pp = byteRanges.elementAt(x);
      if (pp.getProperty("end", "").equals(""))
        pp.put("end", (new StringBuffer(String.valueOf(Long.parseLong(amountEnd) - 1L))).toString()); 
    } 
    if (stat == null && otherFile.getName().toUpperCase().endsWith(".ZIP")) {
      Common.startMultiThreadZipper(this.thisSessionHTTP.thisSession.uVFS, this.thisSessionHTTP.retr, this.thisSessionHTTP.pwd(), 2000, !(!item.getProperty("url").toUpperCase().startsWith("FTP:/") && !item.getProperty("url").toUpperCase().startsWith("HTTP:/")), new Vector());
      write_command_http("Connection: close");
      this.thisSessionHTTP.done = true;
    } else {
      long l = 0L;
      try {
        l = Long.parseLong(stat.getProperty("size"));
      } catch (Exception exception) {}
      if (byteRanges.size() == 1) {
        Properties pp = byteRanges.elementAt(0);
        write_command_http("Content-Range: bytes " + pp.getProperty("start") + "-" + Long.parseLong(pp.getProperty("end")) + "/" + l);
        long calculatedContentLength = Long.parseLong(pp.getProperty("end")) - Long.parseLong(pp.getProperty("start"));
        if (calculatedContentLength == 0L)
          calculatedContentLength = 1L; 
        write_command_http("Content-Length: " + calculatedContentLength);
      } else if (byteRanges.size() <= 1) {
        boolean ok = true;
        if (!this.thisSessionHTTP.thisSession.user.getProperty("filePublicEncryptionKey", "").equals("") || ServerStatus.BG("fileEncryption"))
          if (otherFile != null && otherFile.getProtocol().equalsIgnoreCase("file")) {
            byte[] b = new byte[200];
            try {
              FileInputStream tempIn = new FileInputStream(Common.url_decode(otherFile.getFile()));
              tempIn.read(b);
              tempIn.close();
              String s = new String(b);
              if (s.indexOf("CRUSHFTP#") >= 0) {
                s = s.substring(s.indexOf("CRUSHFTP#") + "CRUSHFTP#".length());
                if (s.indexOf("\r") >= 0)
                  s = s.substring(0, s.indexOf("\r")).trim(); 
                if (s.indexOf("\n") >= 0)
                  s = s.substring(0, s.indexOf("\n")).trim(); 
                if (s.equals(""))
                  ok = false; 
              } 
            } catch (Exception e) {
              ok = false;
              Log.log("SERVER", 1, e);
            } 
          }  
        if (ok) {
          write_command_http("Content-Length: " + l);
        } else {
          this.thisSessionHTTP.done = true;
          write_command_http("Connection: close");
        } 
      } else if (byteRanges.size() > 1) {
        if (simpleRanges) {
          long calculatedContentLength = 0L;
          for (int j = 0; j < byteRanges.size(); j++) {
            Properties pp = byteRanges.elementAt(j);
            calculatedContentLength += Long.parseLong(pp.getProperty("end")) - Long.parseLong(pp.getProperty("start"));
          } 
          write_command_http("Content-Length: " + calculatedContentLength);
        } else {
          long calculatedContentLength = 2L;
          for (int j = 0; j < byteRanges.size(); j++) {
            Properties pp = byteRanges.elementAt(j);
            calculatedContentLength += (("--" + byteRangeBoundary).length() + 2);
            calculatedContentLength += (("Content-Type: " + contentType).length() + 2);
            calculatedContentLength += (("Content-range: bytes " + pp.getProperty("start") + "-" + pp.getProperty("end") + "/" + l).length() + 2);
            calculatedContentLength += 2L;
            calculatedContentLength += Long.parseLong(pp.getProperty("end")) - Long.parseLong(pp.getProperty("start"));
            calculatedContentLength += 2L;
          } 
          calculatedContentLength += (("--" + byteRangeBoundary + "--").length() + 2);
          if (calculatedContentLength == 0L)
            calculatedContentLength = 1L; 
          write_command_http("Content-Length: " + calculatedContentLength);
        } 
      } 
      write_command_http("Accept-Ranges: bytes");
    } 
    if (this.thisSessionHTTP.thisSession.uiSG("header_user-agent").toUpperCase().indexOf("MSIE") >= 0 || this.thisSessionHTTP.thisSession.uiSG("header_user-agent").toUpperCase().indexOf("TRIDENT") >= 0)
      fileName = Common.url_encode(fileName); 
    write_command_http("");
    if (byteRanges.size() == 0) {
      Properties pp = new Properties();
      pp.put("start", "0");
      pp.put("end", "-1");
      byteRanges.addElement(pp);
    } 
    long content_length = 0L;
    try {
      content_length = Long.parseLong(stat.getProperty("size"));
    } catch (Exception exception) {}
    for (int i = 0; i < byteRanges.size(); i++) {
      Properties pp = byteRanges.elementAt(i);
      if (byteRanges.size() > 1 && !simpleRanges) {
        if (i == 0)
          write_command_http(""); 
        write_command_http("--" + byteRangeBoundary);
        write_command_http("Content-Type: " + contentType);
        write_command_http("Content-range: bytes " + pp.getProperty("start") + "-" + pp.getProperty("end") + "/" + content_length);
        write_command_http("");
      } 
      this.thisSessionHTTP.thisSession.uiPUT("file_transfer_mode", "BINARY");
      this.thisSessionHTTP.retr.data_os = this.thisSessionHTTP.original_os;
      this.thisSessionHTTP.retr.httpDownload = true;
      String the_dir = this.thisSessionHTTP.pwd();
      Properties ppp = new Properties();
      ppp.put("the_dir", the_dir);
      this.thisSessionHTTP.thisSession.runPlugin("transfer_path", ppp);
      the_dir = ppp.getProperty("the_dir", the_dir);
      this.thisSessionHTTP.retr.init_vars(the_dir, Long.parseLong(pp.getProperty("start")), Long.parseLong(pp.getProperty("end")) + 1L, this.thisSessionHTTP.thisSession, item, false, "", otherFile, null);
      this.thisSessionHTTP.retr.runOnce = true;
      this.thisSessionHTTP.retr.run();
      if (this.thisSessionHTTP.retr.stop_message.length() > 0)
        return this.thisSessionHTTP.retr.stop_message; 
      if (byteRanges.size() > 1 && !simpleRanges)
        write_command_http(""); 
    } 
    if (byteRanges.size() > 1 && !simpleRanges)
      write_command_http("--" + byteRangeBoundary + "--"); 
    return "";
  }
  
  public static void processAs2HeaderLine(String key, String val, String data, Properties as2Info) {
    as2Info.put(key.trim().toLowerCase(), val.trim());
    if (data.toLowerCase().startsWith("message-id:")) {
      String as2Filename = data.substring(data.indexOf(":") + 1).trim();
      as2Filename = as2Filename.substring(1);
      if (as2Filename.indexOf("@") >= 0)
        as2Filename = as2Filename.substring(0, as2Filename.indexOf("@")); 
      as2Info.put("as2Filename", as2Filename);
    } else if (data.toLowerCase().startsWith("content-type:")) {
      as2Info.put("contentType", data.substring(data.indexOf(":") + 1).trim());
    } else if (data.toLowerCase().startsWith("disposition-notification-options:")) {
      as2Info.put("signMdn", (new StringBuffer(String.valueOf((data.substring(data.indexOf(":") + 1).trim().indexOf("pkcs7-signature") >= 0)))).toString());
    } 
  }
  
  public boolean buildPostItem(Properties request, long http_len_max, Vector headers, String req_id) throws Exception {
    Properties as2Info = new Properties();
    boolean write100Continue = false;
    for (int x = 1; x < headers.size(); x++) {
      String data = headers.elementAt(x).toString();
      String key = data;
      String val = "";
      try {
        val = data.substring(data.indexOf(":") + 1).trim();
        key = data.substring(0, data.indexOf(":")).trim().toLowerCase();
      } catch (Exception e) {
        Log.log("HTTP_SERVER", 3, e);
      } 
      as2Info.put(key, val);
      if (data.toLowerCase().startsWith("expect: 100-continue")) {
        write100Continue = true;
      } else {
        processAs2HeaderLine(key, val, data, as2Info);
      } 
    } 
    if (!as2Info.getProperty("as2-to", "").equals("")) {
      if (!this.thisSessionHTTP.thisSession.uiBG("user_logged_in"))
        return false; 
      if (write100Continue) {
        this.thisSessionHTTP.writeCookieAuth = false;
        write_command_http("HTTP/1.1 100 Continue");
        write_command_http("");
      } 
      Vector payloads = new Vector();
      String messageId = as2Info.getProperty("message-id", "-NONE-");
      if (messageId.startsWith("<"))
        messageId = messageId.substring(1, messageId.length() - 1); 
      String contentType = as2Info.getProperty("contentType");
      boolean mdnResponse = false;
      if (as2Info.getProperty("contentType").toLowerCase().indexOf("disposition-notification") >= 0 || as2Info.getProperty("subject", "").toUpperCase().indexOf("DELIVERY NOTIFICATION") >= 0 || !as2Info.getProperty("mdnbytes", "0").equals("0"))
        mdnResponse = true; 
      if (as2Info.containsKey("filename"))
        as2Info.put("as2Filename", as2Info.getProperty("filename")); 
      this.thisSessionHTTP.thisSession.user_info.putAll(as2Info);
      String data0 = headers.elementAt(0).toString();
      data0 = data0.substring(data0.indexOf(" ") + 1, data0.lastIndexOf(" "));
      if (!data0.endsWith("/"))
        data0 = String.valueOf(data0) + "/"; 
      this.thisSessionHTTP.cd(data0);
      this.thisSessionHTTP.setupSession();
      String the_dir = this.thisSessionHTTP.pwd();
      if (!the_dir.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
        the_dir = String.valueOf(this.thisSessionHTTP.thisSession.SG("root_dir")) + the_dir.substring(1); 
      Properties site_item = this.thisSessionHTTP.thisSession.uVFS.get_item(the_dir);
      String boundary = "";
      String mdn = null;
      As2Msg m = new As2Msg();
      Properties info = new Properties();
      Properties user = this.thisSessionHTTP.thisSession.user;
      Object outData = null;
      boolean inerror = false;
      String keystorePass = user.getProperty("as2EncryptKeystorePassword", this.thisSessionHTTP.thisSession.getProperty("as2EncryptKeystorePassword", ""));
      String signstorePass = user.getProperty("as2SignKeystorePassword", this.thisSessionHTTP.thisSession.getProperty("as2SignKeystorePassword", ""));
      String keyPass = user.getProperty("as2EncryptKeystorePassword", this.thisSessionHTTP.thisSession.getProperty("as2EncryptKeystorePassword", ""));
      if (user.getProperty("userVersion", "4").equals("4") || user.getProperty("userVersion", "4").equals("5")) {
        keystorePass = ServerStatus.thisObj.common_code.encode_pass(keystorePass, "DES", "");
        keyPass = ServerStatus.thisObj.common_code.encode_pass(keyPass, "DES", "");
      } 
      try {
        try {
          GenericClient c = this.thisSessionHTTP.thisSession.uVFS.getClient(site_item);
          try {
            c.doCommand("CWD " + this.thisSessionHTTP.pwd());
          } finally {
            c = this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
          } 
          getUserInfo("USERINFO", site_item, the_dir);
        } catch (Exception e) {
          Log.log("HTTP_SERVER", 2, e);
        } 
        this.thisSessionHTTP.cd(String.valueOf(the_dir) + as2Info.getProperty("as2Filename"));
        Object inData = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        this.thisSessionHTTP.doPutFile(http_len_max, this.thisSessionHTTP.done, headers, baos, String.valueOf(the_dir) + as2Info.getProperty("as2Filename"), false, 0L, null);
        inData = baos.toByteArray();
        String tmpFilename = String.valueOf(System.currentTimeMillis()) + ".as2dump";
        if (Log.log("HTTP_SERVER", 4, "Raw File Data Dumped to disk:" + tmpFilename)) {
          RandomAccessFile tmp = new RandomAccessFile(tmpFilename, "rw");
          tmp.write((byte[])inData);
          tmp.close();
        } 
        this.thisSessionHTTP.done = true;
        this.thisSessionHTTP.keepGoing = false;
        outData = m.decryptData(info, inData, as2Info.getProperty("contentType"), user.getProperty("as2EncryptKeystoreFormat", this.thisSessionHTTP.thisSession.getProperty("as2EncryptKeystoreFormat", "PKCS12")), user.getProperty("as2EncryptKeystorePath", this.thisSessionHTTP.thisSession.getProperty("as2EncryptKeystorePath", ".keystore")), keystorePass, keyPass, user.getProperty("as2EncryptKeyAlias", this.thisSessionHTTP.thisSession.getProperty("as2EncryptKeyAlias", "")));
        info.put("content-disposition", as2Info.getProperty("content-disposition", ""));
        info.put("as2Filename", as2Info.getProperty("as2Filename"));
        String[] dnos = as2Info.getProperty("Disposition-Notification-Options".toLowerCase(), "").split(";");
        String mic_alg = null;
        try {
          for (int j = 0; j < dnos.length && mic_alg == null; j++) {
            if (dnos[j].trim().toLowerCase().startsWith("signed-receipt-micalg")) {
              String[] mic_algs = dnos[j].trim().toLowerCase().split("=")[1].trim().split(",");
              for (int xx = 0; xx < mic_algs.length && mic_alg == null; xx++) {
                if (mic_algs[xx].trim().equalsIgnoreCase("sha-256")) {
                  mic_alg = "sha-256";
                } else if (mic_algs[xx].trim().equalsIgnoreCase("sha1")) {
                  mic_alg = "sha1";
                } else if (mic_algs[xx].trim().equalsIgnoreCase("md5")) {
                  mic_alg = "md5";
                } 
              } 
            } 
          } 
        } catch (Exception e) {
          Log.log("SERVER", 1, e);
        } 
        if (mic_alg == null)
          mic_alg = "sha1"; 
        info.put("mic_alg", mic_alg);
        payloads = m.getPayloadsAndMic(info, outData, user.getProperty("as2SignKeystoreFormat", this.thisSessionHTTP.thisSession.getProperty("as2SignKeystoreFormat", "PKCS12")), user.getProperty("as2SignKeystorePath", this.thisSessionHTTP.thisSession.getProperty("as2SignKeystorePath", "")), signstorePass, user.getProperty("as2SignKeyAlias", this.thisSessionHTTP.thisSession.getProperty("as2SignKeyAlias", "")), mic_alg);
        if (info.getProperty("contentType", "").toLowerCase().indexOf("disposition-notification") >= 0 || mdnResponse) {
          mdnResponse = true;
          if (contentType.toLowerCase().indexOf("disposition-notification") < 0 && info.getProperty("contentType", "").toLowerCase().indexOf("disposition-notification") >= 0)
            contentType = info.getProperty("contentType", ""); 
        } 
      } catch (Exception e) {
        this.thisSessionHTTP.thisSession.user_info.putAll(as2Info);
        Log.log("SERVER", 0, e);
        payloads = new Vector();
        inerror = true;
        this.thisSessionHTTP.thisSession.doErrorEvent(e);
      } 
      if (!mdnResponse) {
        Log.log("AS2_SERVER", 1, "AS2:Payloads:" + payloads.size() + ":" + payloads);
        Log.log("AS2_SERVER", 1, "AS2:info:" + info);
        Log.log("AS2_SERVER", 2, "AS2:as2Info:" + as2Info);
        String disp_state = "automatic-action/MDN-sent-automatically; " + (inerror ? "failed" : "processed");
        mdn = m.createMDN(info.getProperty("mic", ""), info.getProperty("mic_alg", "sha1"), as2Info.getProperty("signMdn", "false").equals("true"), as2Info.getProperty("as2-to", ""), messageId, disp_state, "Received AS2 message.", user.getProperty("as2EncryptKeystoreFormat", this.thisSessionHTTP.thisSession.getProperty("as2EncryptKeystoreFormat", "PKCS12")), user.getProperty("as2EncryptKeystorePath", this.thisSessionHTTP.thisSession.getProperty("as2EncryptKeystorePath", ".keystore")), keystorePass, keyPass, user.getProperty("as2EncryptKeyAlias", this.thisSessionHTTP.thisSession.getProperty("as2EncryptKeyAlias", "")));
        Log.log("AS2_SERVER", 1, "AS2:MDN:" + mdn);
        BufferedReader sr = new BufferedReader(new StringReader(mdn));
        while (boundary.equals(""))
          boundary = sr.readLine().trim(); 
        sr.close();
      } 
      this.thisSessionHTTP.thisSession.user_info.putAll(as2Info);
      for (int i = 0; i < payloads.size(); i++) {
        boolean ok = false;
        if (this.thisSessionHTTP.thisSession.check_access_privs(this.thisSessionHTTP.pwd(), "STOR") && Common.filter_check("U", Common.last(this.thisSessionHTTP.pwd()), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + this.thisSessionHTTP.thisSession.SG("file_filter")))
          ok = true; 
        if (ok) {
          Properties uploadStat1 = null;
          Properties uploadStat2 = null;
          Object o = payloads.elementAt(i);
          if (o instanceof File) {
            File f = (File)o;
            this.thisSessionHTTP.cd(String.valueOf(Common.all_but_last(this.thisSessionHTTP.pwd())) + f.getName());
            Properties result = this.thisSessionHTTP.getStorOutputStream(String.valueOf(Common.all_but_last(this.thisSessionHTTP.pwd())) + f.getName(), 0L, false, null);
            STOR_handler stor = (STOR_handler)result.remove("stor");
            Properties active = (Properties)result.get("active");
            OutputStream of_stream = (OutputStream)result.remove("out");
            Common.copyStreams(new FileInputStream(f), of_stream, true, true);
            while (active.getProperty("active", "").equals("true"))
              Thread.sleep(100L); 
            stor = null;
            uploadStat1 = this.thisSessionHTTP.thisSession.uiPG("lastUploadStat");
          } else {
            Properties payload = (Properties)o;
            byte[] b = (byte[])payload.get("data");
            Properties mdnInfo = null;
            String ext = "";
            if (mdnResponse) {
              ext = ".mdn";
              try {
                try {
                  mdnInfo = m.parseMDN(b, contentType);
                } catch (Exception e) {
                  this.thisSessionHTTP.thisSession.doErrorEvent(e);
                  Log.log("HTTP_SERVER", 1, e);
                } 
                if (mdnInfo == null) {
                  mdnInfo = new Properties();
                  BufferedReader br = new BufferedReader(new StringReader(new String(b)));
                  String line = "";
                  while ((line = br.readLine()) != null) {
                    if (line.indexOf(":") > 0) {
                      String key = line.substring(0, line.indexOf(":")).toLowerCase().trim();
                      mdnInfo.put(key, line.substring(line.indexOf(":") + 1).trim());
                    } 
                  } 
                } 
                payload.put("name", mdnInfo.getProperty("Original-Message-ID".toLowerCase()).replace('<', '_').replace('>', '_').replace('/', '_'));
                b = mdnInfo.toString().getBytes("UTF8");
              } catch (Exception e) {
                this.thisSessionHTTP.thisSession.user_info.putAll(as2Info);
                this.thisSessionHTTP.thisSession.doErrorEvent(e);
                Log.log("HTTP_SERVER", 1, e);
              } 
            } 
            String originalDir = Common.all_but_last(this.thisSessionHTTP.pwd());
            String filename = String.valueOf(originalDir) + payload.getProperty("name", "");
            if (filename.endsWith(".") && ext.startsWith(".")) {
              filename = String.valueOf(filename) + ext.substring(1);
            } else {
              filename = String.valueOf(filename) + ext;
            } 
            this.thisSessionHTTP.cd(filename);
            Properties result = this.thisSessionHTTP.getStorOutputStream(String.valueOf(filename) + ".zipstream", 0L, false, null);
            STOR_handler stor = (STOR_handler)result.remove("stor");
            Properties active = (Properties)result.get("active");
            OutputStream of_stream = (OutputStream)result.remove("out");
            ZipOutputStream zout = new ZipOutputStream(of_stream);
            zout.setLevel(0);
            String tmp_path = filename;
            if (tmp_path.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
              tmp_path = Common.last(tmp_path); 
            zout.putNextEntry(new ZipEntry(tmp_path));
            Common.copyStreams(new ByteArrayInputStream(b), zout, true, false);
            while (!active.getProperty("streamOpenStatus", "").equals("PENDING") && !active.getProperty("streamOpenStatus", "").equals("OPEN"))
              Thread.sleep(100L); 
            zout.closeEntry();
            if (!mdnResponse) {
              String filename2 = String.valueOf(originalDir) + messageId.replace('<', '_').replace('>', '_').replace('/', '_') + ".out.mdn";
              this.thisSessionHTTP.cd(filename2);
              tmp_path = filename2;
              if (tmp_path.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
                tmp_path = Common.last(tmp_path); 
              zout.putNextEntry(new ZipEntry(tmp_path));
              Common.copyStreams(new ByteArrayInputStream(mdn.getBytes("UTF8")), zout, true, false);
              zout.closeEntry();
              uploadStat2 = this.thisSessionHTTP.thisSession.uiPG("lastUploadStat");
            } 
            zout.finish();
            zout.close();
            while (active.getProperty("active", "").equals("true"))
              Thread.sleep(100L); 
            stor = null;
            this.thisSessionHTTP.thisSession.user_info.putAll(as2Info);
            try {
              if (mdnInfo != null) {
                mdnInfo.put("b", b);
                mdnInfo.put("item", this.thisSessionHTTP.thisSession.uVFS.get_item(filename));
                As2Msg.mdnResponses.put(mdnInfo.getProperty("Original-Message-ID".toLowerCase()), mdnInfo);
                if (Common.System2.get("crushftp.dmz.queue") != null) {
                  Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
                  Properties action = new Properties();
                  action.put("type", "PUT:AS2MDN");
                  action.put("id", Common.makeBoundary());
                  action.put("mdnInfo", mdnInfo);
                  action.put("need_response", "false");
                  queue.addElement(action);
                } 
                Thread.sleep(5000L);
                As2Msg.mdnResponses.remove(mdnInfo.getProperty("Original-Message-ID".toLowerCase()));
              } 
            } catch (Exception e) {
              this.thisSessionHTTP.thisSession.doErrorEvent(e);
              Log.log("HTTP_SERVER", 1, e);
            } 
            uploadStat1 = this.thisSessionHTTP.thisSession.uiPG("lastUploadStat");
          } 
          try {
            if (uploadStat1 != null)
              uploadStat1.putAll(as2Info); 
            if (uploadStat2 != null)
              uploadStat2.putAll(as2Info); 
            int loops = 0;
            while (this.thisSessionHTTP.thisSession.uiPG("lastUploadStat") == null && loops++ < 100)
              Thread.sleep(100L); 
            if (this.thisSessionHTTP.thisSession.uiPG("lastUploadStat") == null) {
              this.thisSessionHTTP.thisSession.user_info.putAll(as2Info);
              write_command_http("HTTP/1.1 500 Error");
              write_command_http("Pragma: no-cache");
              this.thisSessionHTTP.write_standard_headers();
              write_command_http("Content-Length: " + ("file transfer error".length() + 2));
              write_command_http("");
              write_command_http("file transfer error");
              return false;
            } 
          } catch (Exception e) {
            this.thisSessionHTTP.thisSession.doErrorEvent(e);
            Log.log("HTTP_SERVER", 0, e);
          } 
        } 
      } 
      String destUrl = null;
      if (!as2Info.getProperty("receipt-delivery-option", "").equals(""))
        destUrl = as2Info.getProperty("receipt-delivery-option", ""); 
      write_command_http("HTTP/1.1 200 OK");
      this.thisSessionHTTP.write_standard_headers();
      write_command_http("From: AS2");
      write_command_http("Message-ID: <AS2-" + (new Date()).getTime() + "-" + Common.makeBoundary(3) + "@" + as2Info.getProperty("as2-to", "") + "_" + as2Info.getProperty("as2-from", "") + ">");
      Thread.sleep(1L);
      write_command_http("Mime-Version: 1.0");
      if (boundary.length() > 0) {
        write_command_http("AS2-To: " + as2Info.getProperty("as2-from", ""));
        write_command_http("AS2-From: " + as2Info.getProperty("as2-to", ""));
        write_command_http("Subject: Message Delivery Notification");
        write_command_http("AS2-Version: 1.1");
        if (as2Info.getProperty("signMdn", "false").equals("true")) {
          write_command_http("Content-Type: multipart/signed; boundary=\"" + boundary.substring(2) + "\"; protocol=\"application/pkcs7-signature\"; report-type=disposition-notification; micalg=" + info.getProperty("mic_alg", "sha1") + "; charset=utf-8");
        } else {
          write_command_http("Content-Type: multipart/report; boundary=\"" + boundary.substring(2) + "\"; report-type=disposition-notification; micalg=" + info.getProperty("mic_alg", "sha1") + "; charset=utf-8");
        } 
      } 
      if (destUrl == null && mdn != null) {
        write_command_http("Content-Length: " + mdn.length());
        write_command_http("");
        write_command_http(mdn);
      } else {
        write_command_http("Content-Length: 0");
        write_command_http("");
        if (mdn != null) {
          String results = m.doAsyncMDNPost(null, "", "", as2Info, false, mdn, boundary, destUrl, user.getProperty("as2EncryptKeystorePath", this.thisSessionHTTP.thisSession.getProperty("as2EncryptKeystorePath", ".keystore")), keystorePass, keyPass, true, "");
          this.thisSessionHTTP.thisSession.add_log("[" + this.thisSessionHTTP.thisSession.uiSG("user_number") + ":" + this.thisSessionHTTP.thisSession.uiSG("user_name") + ":" + this.thisSessionHTTP.thisSession.uiSG("user_ip") + "] DATA: *" + results.trim() + "*", "HTTP");
        } 
      } 
      if (outData != null && outData instanceof File) {
        int loops = 0;
        while (!((File)outData).delete() && loops++ < 10)
          Thread.sleep(100L); 
      } 
      if (this.thisSessionHTTP.thisSession != null)
        if (Common.System2.get("crushftp.dmz.queue") != null) {
          Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
          Properties action = new Properties();
          action.put("type", "PUT:BATCH_COMPLETE");
          action.put("id", Common.makeBoundary());
          Properties root_item = this.thisSessionHTTP.thisSession.uVFS.get_item(this.thisSessionHTTP.thisSession.SG("root_dir"));
          GenericClient c = this.thisSessionHTTP.thisSession.uVFS.getClient(root_item);
          action.put("crushAuth", c.getConfig("crushAuth"));
          this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
          action.put("user_ip", this.thisSessionHTTP.thisSession.uiSG("user_ip"));
          action.put("request", request);
          action.put("need_response", "false");
          queue.addElement(action);
          this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
        } else {
          this.thisSessionHTTP.thisSession.do_event5("BATCH_COMPLETE", null);
        }  
      return false;
    } 
    if (http_len_max < 10240000L) {
      if (as2Info.getProperty("content-type", "").indexOf("multipart") >= 0) {
        String boundary = as2Info.getProperty("content-type", "").substring(as2Info.getProperty("content-type", "").toUpperCase().indexOf("BOUNDARY=") + "BOUNDARY=".length()).trim();
        Vector items = this.thisSessionHTTP.parsePostArguments(boundary, http_len_max, false, req_id);
        for (int i = 0; i < items.size(); i++) {
          Properties pp = items.elementAt(i);
          request.putAll(pp);
        } 
      } else {
        String postData = this.thisSessionHTTP.get_raw_http_command((int)http_len_max);
        String[] postItems = postData.split("&");
        request.put("type", "text");
        boolean noResult = false;
        boolean merged_line = false;
        String merged = "";
        for (int i = 0; i < postItems.length; i++) {
          if (!postItems[i].trim().equals("")) {
            String name = Common.url_decode(postItems[i].substring(0, postItems[i].indexOf("=")));
            String data_item = Common.url_decode(postItems[i].substring(postItems[i].indexOf("=") + 1));
            request.put(name, data_item);
            if (name.toUpperCase().indexOf("PASS") >= 0)
              data_item = "***********"; 
            if (data_item.indexOf("<password>") >= 0 && data_item.indexOf("</password>") >= 0) {
              data_item = String.valueOf(data_item.substring(0, data_item.indexOf("<password>") + "<password>".length())) + "*******" + data_item.substring(data_item.indexOf("</password>"));
            } else if (data_item.toUpperCase().indexOf("PASS") >= 0) {
              data_item = String.valueOf(data_item.substring(0, data_item.indexOf(":") + 1)) + "*******";
            } 
            if (name.equals("command") && data_item.equals("syncCommandResult"))
              noResult = true; 
            if (name.equals("command") && data_item.equals("getServerItem"))
              merged_line = true; 
            if (name.equals("command") && data_item.equals("getJobsSummary"))
              merged_line = true; 
            if (merged_line)
              merged = String.valueOf(merged) + "," + name + ":" + data_item; 
            if (noResult && name.equals("result")) {
              this.thisSessionHTTP.thisSession.add_log_formatted(String.valueOf(name) + ": len=" + data_item.length(), "POST", req_id);
            } else if (!merged_line) {
              this.thisSessionHTTP.thisSession.add_log_formatted(String.valueOf(name) + ":" + data_item, "POST", req_id);
            } 
          } 
        } 
        if (merged_line)
          this.thisSessionHTTP.thisSession.add_log_formatted(merged.substring(1), "POST", req_id); 
      } 
      if (request.getProperty("encoded", "false").equals("true")) {
        Enumeration keys = request.keys();
        while (keys.hasMoreElements()) {
          String key = keys.nextElement().toString();
          request.put(key, Common.url_decode(request.getProperty(key)));
        } 
      } 
    } 
    return true;
  }
  
  public void getUserInfo(String command, Properties site_item, String the_dir) throws Exception {
    String[] lines = new String[0];
    String vfsRootItem = this.thisSessionHTTP.thisSession.uVFS.getRootVFS(the_dir, -1);
    if (the_dir.startsWith(vfsRootItem))
      the_dir = the_dir.substring(vfsRootItem.length() - 1); 
    if ((new VRL(site_item.getProperty("url"))).getProtocol().equalsIgnoreCase("FTP")) {
      GenericClient c = this.thisSessionHTTP.thisSession.uVFS.getClient(site_item);
      try {
        String linesStr = c.doCommand("SITE " + command + " " + the_dir);
        if (linesStr != null)
          lines = linesStr.substring(4).split(";;;"); 
        Properties extraCustomizations = (Properties)this.thisSessionHTTP.thisSession.get("extraCustomizations");
        if (extraCustomizations == null)
          extraCustomizations = new Properties(); 
        for (int x = 0; x < lines.length; x++) {
          String key = lines[x].substring(0, lines[x].indexOf("=")).trim();
          String val = lines[x].substring(lines[x].indexOf("=") + 1).trim();
          extraCustomizations.put(key, val);
        } 
        this.thisSessionHTTP.thisSession.put("extraCustomizations", extraCustomizations);
      } finally {
        c = this.thisSessionHTTP.thisSession.uVFS.releaseClient(c);
      } 
    } 
  }
  
  public void write_command_http(String s) throws Exception {
    write_command_http(s, true);
  }
  
  public void write_command_http(String s, boolean log) throws Exception {
    this.thisSessionHTTP.write_command_http(s, log, true);
  }
  
  public boolean writeResponse(String response) throws Exception {
    return writeResponse(response, true, 200, true, false, true);
  }
  
  public boolean writeResponse(String response, boolean json) throws Exception {
    return writeResponse(response, true, 200, true, json, true);
  }
  
  public boolean writeResponse(String response, boolean log, int code, boolean convertVars, boolean json, boolean log_header) throws Exception {
    boolean acceptsGZIP = false;
    return writeResponse(response, log, code, convertVars, json, acceptsGZIP, log_header);
  }
  
  public boolean writeResponse(String response, boolean log, int code, boolean convertVars, boolean json, boolean acceptsGZIP, boolean log_header) throws Exception {
    if (convertVars)
      response = ServerStatus.thisObj.change_vars_to_values(response, this.thisSessionHTTP.thisSession); 
    write_command_http("HTTP/1.1 " + code + " OK", log_header);
    write_command_http("Cache-Control: no-store", log_header);
    write_command_http("Pragma: no-cache", log_header);
    if (json) {
      write_command_http("Content-Type: application/jsonrequest;charset=utf-8");
    } else {
      write_command_http("Content-Type: text/" + ((response.indexOf("<?xml") >= 0) ? "xml" : "plain") + ";charset=utf-8");
    } 
    if (acceptsGZIP) {
      this.thisSessionHTTP.write_command_http("Vary: Accept-Encoding");
      this.thisSessionHTTP.write_command_http("Content-Encoding: gzip");
      this.thisSessionHTTP.write_command_http("Transfer-Encoding: chunked");
      this.thisSessionHTTP.write_command_http("Date: " + this.thisSessionHTTP.sdf_rfc1123.format(new Date()), log, true);
      this.thisSessionHTTP.write_command_http("Server: " + ServerStatus.SG("http_server_header"), log, true);
      this.thisSessionHTTP.write_command_http("P3P: CP=\"IDC DSP COR ADM DEVi TAIi PSA PSD IVAi IVDi CONi HIS OUR IND CNT\"", log, true);
      if (!ServerStatus.SG("Access-Control-Allow-Origin").equals("")) {
        String origin = this.thisSessionHTTP.headerLookup.getProperty("ORIGIN", "");
        for (int x = 0; x < (ServerStatus.SG("Access-Control-Allow-Origin").split(",")).length; x++) {
          boolean ok = false;
          if (origin.equals("")) {
            ok = true;
          } else if (ServerStatus.SG("Access-Control-Allow-Origin").split(",")[x].toUpperCase().trim().equalsIgnoreCase(origin.toUpperCase().trim())) {
            ok = true;
          } 
          if (ok)
            write_command_http("Access-Control-Allow-Origin: " + ServerStatus.SG("Access-Control-Allow-Origin").split(",")[x].trim()); 
        } 
        write_command_http("Access-Control-Allow-Headers: authorization,content-type");
        write_command_http("Access-Control-Allow-Credentials: true");
        write_command_http("Access-Control-Allow-Methods: GET,POST,OPTIONS,PUT,PROPFIND,DELETE,MKCOL,MOVE,COPY,HEAD,PROPPATCH,LOCK,UNLOCK,ACL,TR");
      } 
      write_command_http("", log);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      byte[] b = response.getBytes("UTF8");
      OutputStream out = new GZIPOutputStream(baos);
      out.write(b);
      ((GZIPOutputStream)out).finish();
      if (baos.size() > 0) {
        this.thisSessionHTTP.original_os.write((String.valueOf(Long.toHexString(baos.size())) + "\r\n").getBytes());
        baos.writeTo(this.thisSessionHTTP.original_os);
        this.thisSessionHTTP.original_os.write("\r\n".getBytes());
        baos.reset();
      } 
      this.thisSessionHTTP.original_os.write("0\r\n\r\n".getBytes());
      this.thisSessionHTTP.original_os.flush();
    } else {
      this.thisSessionHTTP.write_standard_headers(log);
      int len = (response.getBytes("UTF8")).length + 2;
      if (len == 2)
        len = 0; 
      write_command_http("Content-Length: " + len, log_header);
      write_command_http("", log);
      if (len > 0)
        this.thisSessionHTTP.write_command_http(response, log, convertVars); 
    } 
    this.thisSessionHTTP.thisSession.drain_log();
    return true;
  }
  
  public static String processKeywords(SessionCrush thisSession, String[] names, String keywords_raw) throws Exception {
    String response = "";
    String keyword = keywords_raw.trim();
    response = String.valueOf(response) + "<commandResult><response>";
    boolean ok = false;
    for (int x = 0; x < names.length; x++) {
      String the_dir = Common.url_decode(Common.all_but_last(names[x]));
      if (the_dir.startsWith(thisSession.SG("root_dir")))
        the_dir = the_dir.substring(thisSession.SG("root_dir").length() - 1); 
      String path = thisSession.getStandardizedDir(the_dir);
      Properties item = thisSession.uVFS.get_item(String.valueOf(path) + Common.last(names[x]));
      the_dir = SearchHandler.getPreviewPath(item, "1", 1);
      String index = String.valueOf(ServerStatus.SG("previews_path")) + the_dir.substring(1);
      (new File(Common.all_but_last(index))).mkdirs();
      if (ServerStatus.BG("exif_keywords")) {
        String srcFile = Common.dots((new VRL(item.getProperty("url"))).getPath());
        for (int xx = 0; xx < ServerStatus.thisObj.previewWorkers.size(); xx++) {
          PreviewWorker preview = ServerStatus.thisObj.previewWorkers.elementAt(xx);
          if (preview.prefs.getProperty("preview_enabled", "false").equalsIgnoreCase("true") && preview.checkExtension(Common.last(the_dir), item)) {
            preview.setExifInfo(srcFile, PreviewWorker.getDestPath2(String.valueOf(item.getProperty("url")) + "/p1/"), "keywords", Common.url_decode(keywords_raw).trim());
            ok = true;
            break;
          } 
        } 
      } else if ((new File(Common.all_but_last(Common.all_but_last(index)))).exists()) {
        RandomAccessFile out = new RandomAccessFile(String.valueOf(Common.all_but_last(Common.all_but_last(index))) + "index.txt", "rw");
        out.seek(out.length());
        out.write((String.valueOf(keyword) + "\r\n").getBytes());
        out.close();
        ok = true;
      } else {
        response = String.valueOf(response) + "Keyword not added.  This file is not indexed. (" + names[x] + ")\r\n";
      } 
      SearchHandler.buildEntry(item, thisSession.uVFS, false, false);
    } 
    if (ok)
      response = String.valueOf(response) + "Keyword Added.\r\n"; 
    return response;
  }
  
  public static void fixButtons(Vector buttons) {
    for (int x = buttons.size() - 1; x >= 0; x--) {
      Properties button = buttons.elementAt(x);
      button.put("requiredPriv", "");
      if (!button.containsKey("for_menu"))
        button.put("for_menu", button.getProperty("forMenu", "true")); 
      if (!button.containsKey("for_context_menu"))
        button.put("for_context_menu", button.getProperty("forContextMenu", "true")); 
      if (button.getProperty("key").equals("(upload):Crush Uploader") || button.getProperty("value").startsWith("javascript:loadCrushUpplet")) {
        buttons.remove(x);
      } else if (button.getProperty("key").equals("(zip):.ZIP") || button.getProperty("value").startsWith("javascript:zip_items")) {
        button.put("requiredPriv", "(read)");
        button.put("key", "(zip):ZipDownload");
        button.put("value", "javascript:performAction('zip');");
      } else if (button.getProperty("key").equals("(custom):Manage Download Basket") || button.getProperty("value").startsWith("javascript:manageBasket")) {
        button.put("requiredPriv", "(read)");
        button.put("key", "(showbasket):Show Basket");
        button.put("value", "javascript:performAction('showBasket');");
      } else if (button.getProperty("key").equals("(custom):Add To Download Basket")) {
        button.put("requiredPriv", "(read)");
        button.put("key", "(addbasket):Add To Basket");
        button.put("value", "javascript:performAction('addToBasket');");
      } else if (button.getProperty("key").startsWith("(rename):")) {
        button.put("requiredPriv", "(rename)");
      } else if (button.getProperty("key").startsWith("(delete):")) {
        button.put("requiredPriv", "(delete)");
      } else if (button.getProperty("key").startsWith("(download):")) {
        button.put("requiredPriv", "(read)");
      } else if (button.getProperty("key").startsWith("(zip):")) {
        button.put("requiredPriv", "(read)");
      } else if (button.getProperty("key").startsWith("(mkdir):")) {
        button.put("requiredPriv", "(makedir)");
      } else if (button.getProperty("key").startsWith("(upload):")) {
        button.put("requiredPriv", "(write)");
      } else if (button.getProperty("key").startsWith("(search):")) {
        button.put("requiredPriv", "(view)");
      } else if (button.getProperty("key").startsWith("(cut):")) {
        button.put("requiredPriv", "(rename)");
      } else if (button.getProperty("key").startsWith("(copy):")) {
        button.put("requiredPriv", "(read)");
      } else if (button.getProperty("key").startsWith("(paste):")) {
        button.put("requiredPriv", "(write)");
      } else if (button.getProperty("key").startsWith("(slideshow):")) {
        button.put("requiredPriv", "(slideshow)");
      } else if (button.getProperty("key").startsWith("(share):")) {
        button.put("requiredPriv", "(share)");
      } else if (button.getProperty("key").indexOf("Logout") >= 0) {
        button.put("for_menu", "true");
        button.put("for_context_menu", "false");
      } 
      if (button.getProperty("value").indexOf("showPopup") >= 0)
        button.put("value", Common.replace_str(button.getProperty("value"), "showPopup", "performAction")); 
      if (button.getProperty("value").indexOf("showDownloadBasket") >= 0) {
        button.put("value", "javascript:performAction('showBasket');");
        button.put("requiredPriv", "(read)");
      } 
      if (button.getProperty("value").indexOf("addToBasket") >= 0) {
        button.put("value", "javascript:performAction('addToBasket');");
        button.put("requiredPriv", "(read)");
      } 
      if (button.getProperty("value").indexOf("download()") >= 0) {
        button.put("value", "javascript:performAction('download');");
        button.put("requiredPriv", "(read)");
      } 
      if (button.getProperty("value").indexOf("shareOption") >= 0 || button.getProperty("value").indexOf("shareDiv") >= 0) {
        button.put("value", "javascript:performAction('share');");
        button.put("requiredPriv", "(share)");
      } 
      if (button.getProperty("value").indexOf("deleteDiv") >= 0) {
        button.put("value", "javascript:performAction('delete');");
        button.put("requiredPriv", "(delete)");
      } 
      if (button.getProperty("value").indexOf("doCut") >= 0) {
        button.put("value", "javascript:performAction('cut');");
        button.put("requiredPriv", "(rename)");
      } 
      if (button.getProperty("value").indexOf("doPaste") >= 0) {
        button.put("value", "javascript:performAction('paste');");
        button.put("requiredPriv", "(write)");
      } 
      if (button.getProperty("value").indexOf("userOptions") >= 0)
        button.put("value", "javascript:performAction('userOptions');"); 
      if (button.getProperty("value").indexOf("slideshow") >= 0)
        button.put("value", "javascript:performAction('slideshow');"); 
      if (button.getProperty("value").indexOf("makedir") >= 0 || button.getProperty("value").indexOf("createFolder") >= 0)
        button.put("value", "javascript:performAction('createFolder');"); 
      if (button.getProperty("value").indexOf("search") >= 0)
        button.put("value", "javascript:performAction('search');"); 
      if (button.getProperty("value").indexOf("Login") >= 0)
        button.put("value", "javascript:doLogout();"); 
      if (button.getProperty("value").indexOf("/login.html") >= 0)
        button.put("value", "javascript:doLogout();"); 
      if (button.getProperty("value").indexOf("Logout") >= 0)
        button.put("value", "javascript:doLogout();"); 
    } 
  }
  
  public static String createShare(Vector path_items, Properties request, Vector web_customizations, String user_name, String linkedServer, Properties user, SimpleDateFormat date_time, SessionCrush thisSession) throws Exception {
    return createShare(path_items, request, web_customizations, user_name, linkedServer, user, date_time, thisSession, false);
  }
  
  public static String createShare(Vector path_items, Properties request, Vector web_customizations, String user_name, String linkedServer, Properties user, SimpleDateFormat date_time, SessionCrush thisSession, boolean publish_attach) throws Exception {
    if (date_time == null)
      date_time = new SimpleDateFormat("MM/dd/yy", Locale.US); 
    String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
    String[] paths = (String[])null;
    if (request.getProperty("paths").indexOf(";") >= 0) {
      paths = Common.url_decode(request.getProperty("paths")).trim().split(";");
    } else {
      paths = Common.url_decode(request.getProperty("paths")).trim().split("\r\n");
    } 
    String tempUsername = Common.url_decode(request.getProperty("temp_username", ""));
    String tempPassword = Common.url_decode(request.getProperty("temp_password", ""));
    if (tempUsername.equals("")) {
      tempUsername = Common.makeBoundary(ServerStatus.IG("temp_accounts_length"));
      tempPassword = Common.makeBoundary(ServerStatus.IG("temp_accounts_length"));
    } else if (((new File(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "storage/" + tempUsername + tempPassword + "/")).exists() && !publish_attach) || tempUsername.length() + tempPassword.length() < ServerStatus.IG("temp_accounts_length") * 2 || (new File(String.valueOf(System.getProperty("crushftp.users")) + linkedServer + "/" + tempUsername)).exists()) {
      response = String.valueOf(response) + "<commandResult><response>";
      response = String.valueOf(response) + "<username></username>";
      response = String.valueOf(response) + "<password></password>";
      response = String.valueOf(response) + "<message>Denied.</message>";
      response = String.valueOf(response) + "<url></url>";
      response = String.valueOf(response) + "<error_response>denied</error_response>";
      response = String.valueOf(response) + "</response></commandResult>";
      return response;
    } 
    SimpleDateFormat ex1 = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US);
    SimpleDateFormat ex2 = new SimpleDateFormat("MMddyyyyHHmm", Locale.US);
    String shareToDomain = "";
    String shareBodyEmailClient = "";
    String shareFromDomain = "";
    int maxLen = 255;
    if (Common.machine_is_windows())
      maxLen -= (new File(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/")).getCanonicalPath().length(); 
    String flash_shareAllowUploadsPrivs = "(read)(view)(resume)(write)(delete)(slideshow)(rename)(makedir)(deletedir)";
    int maxExpireDays = 0;
    if (web_customizations != null)
      for (int i = 0; i < web_customizations.size(); i++) {
        Properties cust = web_customizations.elementAt(i);
        if (cust.getProperty("key").equals("shareToDomain"))
          shareToDomain = cust.getProperty("value"); 
        if (cust.getProperty("key").equals("shareBodyEmailClient"))
          shareBodyEmailClient = cust.getProperty("value"); 
        if (cust.getProperty("key").equals("shareFromDomain"))
          shareFromDomain = cust.getProperty("value"); 
        if (cust.getProperty("key").equals("flash_shareAllowUploadsPrivs"))
          flash_shareAllowUploadsPrivs = cust.getProperty("value"); 
        if (cust.getProperty("key").equals("EXPIREDAYSMAX"))
          maxExpireDays = Integer.parseInt(cust.getProperty("value").trim()); 
      }  
    Date requestExpire = ex1.parse(request.getProperty("expire", "1/1/1970 00:01").replace('+', ' '));
    Calendar gc = new GregorianCalendar();
    gc.setTime(new Date());
    gc.add(5, maxExpireDays);
    if (maxExpireDays > 0 && (!request.containsKey("expire") || requestExpire.getTime() > gc.getTime().getTime()))
      requestExpire = gc.getTime(); 
    String expire_date = ex2.format(requestExpire);
    request.put("expire", ex1.format(requestExpire));
    String folderName = "u=" + tempUsername + ",,p=" + tempPassword + ",,m=" + user_name + ",,t=TempAccount,,ex=" + expire_date;
    if (request.getProperty("logins", "").trim().equals("-1"))
      request.remove("logins"); 
    if (!request.getProperty("logins", "").equals(""))
      folderName = String.valueOf(folderName) + ",,i=" + request.getProperty("logins", ""); 
    String userHome = String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + folderName + "/";
    String userStorage = String.valueOf(ServerStatus.SG("temp_accounts_path")) + "storage/" + tempUsername + tempPassword + "/";
    Properties permissions = new Properties();
    String msg = "";
    String baseUrl = Common.url_decode(request.getProperty("baseUrl"));
    String webLink = String.valueOf(baseUrl) + "?u=" + tempUsername + "&p=" + tempPassword;
    String webLinkEnd = "?u=" + tempUsername + "&p=" + tempPassword;
    if (request.getProperty("direct_link", "false").equalsIgnoreCase("true") && paths.length == 1 && !paths[0].trim().endsWith("/")) {
      webLink = String.valueOf(webLink) + "&path=/" + Common.url_encode(Common.last(paths[0]));
      webLinkEnd = String.valueOf(webLinkEnd) + "&path=/" + Common.url_encode(Common.last(paths[0]));
    } 
    request.put("account_path", userHome);
    request.put("storage_path", userStorage);
    request.put("master", user_name);
    request.put("created", ex1.format(new Date()));
    request.put("username", tempUsername);
    request.put("password", tempPassword);
    request.put("web_link", webLink);
    request.put("web_link_end", webLinkEnd);
    boolean stop_share = false;
    if (!shareToDomain.equals("")) {
      String emails = String.valueOf(request.getProperty("emailTo")) + "," + request.getProperty("emailCc") + "," + request.getProperty("emailBcc");
      String[] tos = emails.replace('+', ' ').trim().replace(';', ',').split(",");
      boolean ok = true;
      for (int i = 0; i < tos.length && ok; i++) {
        String to = tos[i];
        if (!to.trim().equals(""))
          for (int xx = 0; xx < (shareToDomain.split(",")).length; xx++) {
            if (!to.toUpperCase().trim().endsWith(shareToDomain.split(",")[xx].toUpperCase().trim()))
              ok = false; 
          }  
      } 
      if (!ok)
        msg = "The To, Cc, or Bcc email does not end with one of the required domain(s): " + shareToDomain; 
      if (!msg.equals(""))
        stop_share = true; 
    } 
    if (!shareFromDomain.equals("")) {
      boolean ok = true;
      String from = request.getProperty("emailFrom", "").replace('+', ' ').trim();
      for (int xx = 0; xx < (shareFromDomain.split(",")).length; xx++) {
        if (!from.toUpperCase().trim().endsWith(shareFromDomain.split(",")[xx].toUpperCase().trim()))
          ok = false; 
      } 
      if (!ok)
        msg = String.valueOf(msg) + "The FROM: email does not end with one of the required domain(s): " + shareFromDomain + "."; 
      if (ok) {
        String reply_to = request.getProperty("emailReplyTo", "").replace('+', ' ').trim();
        for (int i = 0; i < (shareFromDomain.split(",")).length; i++) {
          if (!shareFromDomain.split(",")[i].toUpperCase().trim().equals("") && 
            !reply_to.toUpperCase().trim().endsWith(shareFromDomain.split(",")[i].toUpperCase().trim()) && !reply_to.trim().equals(""))
            ok = false; 
        } 
        if (!ok)
          msg = String.valueOf(msg) + "The REPLY TO: email does not end with one of the required domain(s): " + shareFromDomain + "."; 
      } 
      if (!msg.equals(""))
        stop_share = true; 
    } 
    String last_name = Common.url_encode(Common.last(paths[0]));
    Vector files = new Vector();
    Properties lastStat = null;
    long total_size = 0L;
    int x;
    for (x = 0; x < path_items.size() && msg.equals(""); x++) {
      Properties stat = path_items.elementAt(x);
      VRL vrl = new VRL(stat.getProperty("url"));
      String privs = stat.getProperty("privs", "(read)(share)(delete)(view)");
      String pgp_addon = "";
      for (int xx = 0; xx < (privs.split("\\(")).length; xx++) {
        String priv = privs.split("\\(")[xx];
        if (!priv.equals("")) {
          priv = priv.substring(0, priv.length() - 1).trim();
          if (priv.indexOf("=") >= 0 && priv.indexOf("pgp") >= 0)
            pgp_addon = String.valueOf(pgp_addon) + "(" + priv.split("=")[0] + "=" + priv.substring(priv.indexOf("=") + 1) + ")"; 
        } 
      } 
      permissions.put("/", "(read)(view)(resume)(slideshow)" + pgp_addon);
      if (privs.indexOf("(share)") < 0) {
        msg = String.valueOf(msg) + "Not allowed to share these files:" + stat.getProperty("root_dir") + stat.getProperty("name") + "\r\n<br/>";
        stop_share = true;
      } 
      if (request.getProperty("shareUsername", "false").equalsIgnoreCase("true")) {
        if (ServerStatus.siIG("enterprise_level") <= 0) {
          msg = String.valueOf(msg) + "The server does not have an enterprise license, so sharing to usernames is not allowed.\r\n<br/>";
          stop_share = true;
        } else {
          tempUsername = "";
          tempPassword = "";
          webLink = String.valueOf(baseUrl) + "Shares/" + user_name + "/";
          webLinkEnd = "Shares/" + user_name + "/";
          if (request.getProperty("direct_link", "false").equalsIgnoreCase("true") && paths.length == 1 && !paths[0].trim().endsWith("/")) {
            webLink = String.valueOf(baseUrl) + "Shares/" + user_name + "/" + Common.url_encode(Common.last(paths[0]));
            webLinkEnd = String.valueOf(webLinkEnd) + "Shares/" + user_name + "/" + Common.url_encode(Common.last(paths[0]));
          } 
          request.put("master", user_name);
          request.put("username", tempUsername);
          request.put("password", tempPassword);
          request.put("web_link", webLink);
          request.put("web_link_end", webLinkEnd);
          String[] shareUsernames = Common.url_decode(request.getProperty("shareUsernames").replace('+', ' ').trim()).split(",");
          String share_to_emails = "";
          for (int j = 0; j < shareUsernames.length; j++) {
            String toUser = String.valueOf(shareUsernames[j].trim()) + ".SHARED";
            toUser = toUser.replace('/', '_').replace('\\', '_').replace('%', '_').replace(':', '_').replace(';', '_');
            VFS shareVFS = null;
            try {
              if (!UserTools.ut.getUser(linkedServer, toUser, false).getProperty("user_name").equals(toUser))
                throw new NullPointerException(); 
              shareVFS = UserTools.ut.getVFS(linkedServer, toUser);
            } catch (NullPointerException e) {
              Properties sharedUser = new Properties();
              sharedUser.put("password", "");
              sharedUser.put("version", "1.0");
              sharedUser.put("root_dir", "/");
              sharedUser.put("userVersion", "5");
              sharedUser.put("max_logins", "-1");
              UserTools.writeUser(linkedServer, toUser, sharedUser);
              shareVFS = UserTools.ut.getVFS(linkedServer, toUser);
            } 
            if (user != null) {
              String user_email = user.getProperty("email", "");
              if (user_email.equals(""))
                user_email = user.getProperty("ldap_mail", ""); 
              request.put("emailFrom", user_email);
            } 
            request.put("emailCc", "");
            request.put("emailBcc", "");
            UserTools.addPriv(linkedServer, toUser, "/Shares/", "(view)(read)", 0, shareVFS);
            String sPrivs1 = request.getProperty("shareUsernamePermissions").toLowerCase();
            String sPrivs2 = "";
            String[] priv_parts = stat.getProperty("privs").toLowerCase().split("\\(");
            for (int loop = 0; loop < priv_parts.length; loop++) {
              String priv = priv_parts[loop];
              priv = priv.trim();
              if (!priv.equals("")) {
                priv = priv.substring(0, priv.lastIndexOf(")"));
                if (sPrivs1.indexOf("(" + priv + ")") >= 0) {
                  sPrivs2 = String.valueOf(sPrivs2) + "(" + priv + ")";
                } else if (priv.startsWith("quota") && sPrivs1.indexOf("(quota") >= 0) {
                  sPrivs2 = String.valueOf(sPrivs2) + "(" + priv + ")";
                } 
              } 
            } 
            Log.log("HTTP_SERVER", 2, "Requested privs:" + sPrivs1);
            Log.log("HTTP_SERVER", 2, "Adding privs:" + sPrivs2);
            Log.log("HTTP_SERVER", 2, "Adding priv to path:/Shares/" + user_name + "/" + stat.getProperty("name"));
            UserTools.addPriv(linkedServer, toUser, "/Shares/" + user_name + "/" + stat.getProperty("name") + (stat.getProperty("type", "DIR").equalsIgnoreCase("DIR") ? "/" : ""), String.valueOf(sPrivs2) + pgp_addon, 0, shareVFS);
            Properties existingItem = shareVFS.get_item("/Shares/" + user_name + "/" + stat.getProperty("name"));
            UserTools.addFolder(linkedServer, toUser, "/", "Shares");
            UserTools.addFolder(linkedServer, toUser, "/Shares/", user_name);
            Properties moreItems = new Properties();
            moreItems.put("expires_on", UserTools.expire_vfs.format(ex1.parse(request.getProperty("expire").replace('+', ' '))));
            moreItems.put("created_on", UserTools.expire_vfs.format(new Date()));
            moreItems.put("share_comments", request.getProperty("share_comments", ""));
            UserTools.addItem(linkedServer, toUser, "/Shares/" + user_name + "/", stat.getProperty("name"), stat.getProperty("url"), stat.getProperty("type", "FILE"), moreItems, false, "");
            Properties real_to_user = UserTools.ut.getUser(linkedServer, shareUsernames[j].trim(), false);
            if (real_to_user != null && !real_to_user.getProperty("email", "").equals("")) {
              share_to_emails = String.valueOf(share_to_emails) + "," + real_to_user.getProperty("email", "");
            } else if (shareUsernames[j].trim().indexOf("@") >= 0) {
              share_to_emails = String.valueOf(share_to_emails) + "," + shareUsernames[j].trim();
            } else {
              String user_email = user.getProperty("email", "");
              if (user_email.equals(""))
                user_email = user.getProperty("ldap_mail", ""); 
              if (!user_email.equals(""))
                share_to_emails = String.valueOf(share_to_emails) + "," + shareUsernames[j].trim() + user_email.substring(user_email.indexOf("@")); 
            } 
            UserTools.ut.forceMemoryReload(shareUsernames[j].trim());
          } 
          if (share_to_emails.length() > 1)
            share_to_emails = share_to_emails.substring(1); 
          request.put("emailTo", share_to_emails);
          lastStat = stat;
        } 
        request.put("publishType", "directShare");
      } 
      if (stop_share)
        break; 
      String uid = "";
      int same_count = 0;
      for (int i = 0; i < path_items.size() && msg.equals(""); i++) {
        if (x != i) {
          Properties stat2 = path_items.elementAt(i);
          if (stat.getProperty("name").equalsIgnoreCase(stat2.getProperty("name")))
            same_count++; 
        } 
      } 
      if (same_count > 0) {
        uid = "_" + Common.makeBoundary(4);
        if (vrl.getName().indexOf(".") >= 0)
          uid = String.valueOf(uid) + vrl.getName().substring(vrl.getName().lastIndexOf(".")); 
      } 
      if (user != null) {
        Enumeration keys = user.keys();
        while (keys.hasMoreElements()) {
          String key = keys.nextElement().toString();
          if (key.startsWith("ldap_"))
            request.put(key, user.get(key)); 
        } 
        if (ServerStatus.BG("temp_account_share_web_javascript") && !user.getProperty("javascript", "").equals(""))
          request.put("javascript", user.getProperty("javascript", "")); 
        if (ServerStatus.BG("temp_account_share_web_css") && !user.getProperty("css", "").equals(""))
          request.put("css", user.getProperty("css", "")); 
        if (ServerStatus.BG("temp_account_share_web_customizations") && user.get("web_customizations") != null)
          request.put("web_customizations", user.get("web_customizations")); 
        if (ServerStatus.BG("temp_account_share_web_buttons") && user.get("web_buttons") != null)
          request.put("web_buttons", user.get("web_buttons")); 
        if (ServerStatus.BG("temp_account_share_web_forms") && user.get("uploadForm") != null)
          request.put("uploadForm", user.get("uploadForm")); 
        if (ServerStatus.BG("temp_account_share_web_forms") && user.get("messageForm") != null)
          request.put("messageForm", user.get("messageForm")); 
        Vector events = (Vector)user.get("events");
        if (events != null) {
          Vector events2 = new Vector();
          for (int j = 0; j < events.size(); j++) {
            Properties event = events.elementAt(j);
            String event_user_action_list = String.valueOf(event.getProperty("event_user_action_list", "")) + "(";
            if (event_user_action_list.indexOf("(share_") >= 0) {
              event = (Properties)event.clone();
              event.put("linkUser", user.getProperty("username"));
              event.put("linkEvent", event.getProperty("name"));
              event.put("resolveShareEvent", "true");
              events2.addElement(event);
            } 
          } 
          if (events2.size() > 0)
            request.put("events", events2); 
        } 
      } 
      if (request.getProperty("publishType").equalsIgnoreCase("copy")) {
        if (privs.indexOf("(read)") >= 0) {
          (new File(String.valueOf(userHome) + "VFS/")).mkdirs();
          (new File(userStorage)).mkdirs();
          String itemName = vrl.getName();
          if (itemName.endsWith("/"))
            itemName = itemName.substring(0, itemName.length() - 1); 
          if (request.getProperty("allowUploads", "false").equals("true"))
            permissions.put("/" + itemName.toUpperCase() + "/", String.valueOf(flash_shareAllowUploadsPrivs) + pgp_addon); 
          try {
            Common.writeXMLObject(String.valueOf(userHome) + "VFS.XML", permissions, "VFS");
          } catch (Exception exception) {}
          try {
            Common.writeXMLObject(String.valueOf(userHome) + "INFO.XML", request, "INFO");
          } catch (Exception exception) {}
          if (vrl.getProtocol().equalsIgnoreCase("FILE")) {
            Common.recurseCopyThreaded(vrl.getPath(), String.valueOf(userStorage) + vrl.getName() + (stat.getProperty("type", "FILE").equalsIgnoreCase("DIR") ? "/" : ""), true, false);
          } else {
            GenericClient c1 = thisSession.uVFS.getClient(stat);
            c1.login(vrl.getUsername(), vrl.getPassword(), null);
            VRL vrl_dest = new VRL(String.valueOf((new File(String.valueOf(userStorage) + vrl.getName())).toURI().toString()) + (stat.getProperty("type", "FILE").equalsIgnoreCase("DIR") ? "/" : ""));
            Common.recurseCopy(vrl, vrl_dest, c1, null, 0, true, new StringBuffer());
            c1 = thisSession.uVFS.releaseClient(c1);
          } 
          if (request.getProperty("attach_real", "").equalsIgnoreCase("true"))
            files.addElement(new File(vrl.getPath())); 
          Properties vItem = new Properties();
          vItem.put("url", String.valueOf((new File(String.valueOf(userStorage) + vrl.getName())).toURI().toURL().toExternalForm()) + (stat.getProperty("type", "FILE").equalsIgnoreCase("DIR") ? "/" : ""));
          vItem.put("type", stat.getProperty("type", "FILE").toLowerCase());
          Vector v = new Vector();
          v.addElement(vItem);
          try {
            Common.writeXMLObject(String.valueOf(userHome) + "VFS/" + itemName + uid, v, "VFS");
          } catch (Exception exception) {}
        } else {
          msg = String.valueOf(msg) + "Not allowed to read from this location.\r\n<br/>";
          stop_share = true;
        } 
      } else if (request.getProperty("publishType").equalsIgnoreCase("move")) {
        if (privs.indexOf("(read)") >= 0 && privs.indexOf("(delete)") >= 0) {
          (new File(String.valueOf(userHome) + "VFS/")).mkdirs();
          (new File(userStorage)).mkdirs();
          String itemName = "storage";
          if (paths.length > 1 || stat.getProperty("type", "FILE").equalsIgnoreCase("FILE"))
            itemName = vrl.getName(); 
          if (itemName.endsWith("/"))
            itemName = itemName.substring(0, itemName.length() - 1); 
          if (request.getProperty("allowUploads", "false").equals("true"))
            permissions.put("/" + itemName.toUpperCase() + "/", String.valueOf(flash_shareAllowUploadsPrivs) + pgp_addon); 
          try {
            Common.writeXMLObject(String.valueOf(userHome) + "VFS.XML", permissions, "VFS");
          } catch (Exception exception) {}
          try {
            Common.writeXMLObject(String.valueOf(userHome) + "INFO.XML", request, "INFO");
          } catch (Exception exception) {}
          Common.recurseCopyThreaded(vrl.getPath(), String.valueOf(userStorage) + vrl.getName(), true, true);
          if (request.getProperty("attach_real", "").equalsIgnoreCase("true"))
            files.addElement(new File(String.valueOf(userStorage) + vrl.getName())); 
          Properties vItem = new Properties();
          vItem.put("url", (new File(String.valueOf(userStorage) + vrl.getName())).toURI().toURL().toExternalForm());
          vItem.put("type", stat.getProperty("type", "FILE").toLowerCase());
          Vector v = new Vector();
          v.addElement(vItem);
          try {
            Common.writeXMLObject(String.valueOf(userHome) + "VFS/" + itemName + uid, v, "VFS");
          } catch (Exception exception) {}
        } else {
          msg = String.valueOf(msg) + "Not allowed to read and delete from this location.\r\n<br/>";
          stop_share = true;
        } 
      } else if (request.getProperty("publishType").equalsIgnoreCase("reference")) {
        if (privs.indexOf("(read)") >= 0) {
          (new File(String.valueOf(userHome) + "VFS/")).mkdirs();
          (new File(userStorage)).mkdirs();
          String itemName = vrl.getName();
          if (itemName.endsWith("/"))
            itemName = itemName.substring(0, itemName.length() - 1); 
          String privs2 = "";
          if (request.getProperty("allowUploads", "false").equals("true")) {
            String privs3 = flash_shareAllowUploadsPrivs;
            privs3 = Common.replace_str(privs3, "(", "");
            privs3 = Common.replace_str(privs3, ")", ",");
            if (privs3.endsWith(","))
              privs3 = privs3.substring(0, privs3.length() - 1); 
            for (int j = 0; j < (privs3.split(",")).length; j++) {
              String s = "(" + privs3.split(",")[j] + ")";
              if (privs.indexOf(s) >= 0) {
                privs2 = String.valueOf(privs2) + s;
              } else if (s.startsWith("(quota") && privs.indexOf("(quota") >= 0) {
                privs2 = String.valueOf(privs2) + s;
              } 
            } 
            permissions.put("/" + itemName.toUpperCase() + "/", String.valueOf(privs2) + pgp_addon);
          } 
          try {
            Common.writeXMLObject(String.valueOf(userHome) + "VFS.XML", permissions, "VFS");
          } catch (Exception exception) {}
          try {
            Common.writeXMLObject(String.valueOf(userHome) + "INFO.XML", request, "INFO");
          } catch (Exception exception) {}
          if (request.getProperty("attach_real", "").equalsIgnoreCase("true"))
            files.addElement(new File(vrl.getPath())); 
          Properties vItem = new Properties();
          vItem.put("url", vrl.toString());
          if (stat.get("type").equals("DIR") && !vItem.getProperty("url").endsWith("/"))
            vItem.put("url", String.valueOf(vrl.toString()) + "/"); 
          vItem.put("type", stat.get("type"));
          Vector v = new Vector();
          v.addElement(vItem);
          try {
            Common.writeXMLObject(String.valueOf(userHome) + "VFS/" + itemName + uid, v, "VFS");
          } catch (Exception exception) {}
        } else {
          msg = String.valueOf(msg) + "Not allowed to read from this location.\r\n<br/>";
          stop_share = true;
        } 
      } 
      if (stop_share)
        break; 
      lastStat = stat;
      total_size += Long.parseLong(stat.getProperty("size", "0"));
      stat.put("temp_home", (new VRL(String.valueOf((new File(userHome)).getCanonicalPath().replace('\\', '/')) + "/")).toString());
      stat.put("web_link", webLink);
      stat.put("web_link_end", webLinkEnd);
      stat.put("temp_username", tempUsername);
      stat.put("temp_password", tempPassword);
      stat.put("expire_date", expire_date);
      doEventItem(stat, thisSession, "SHARE");
      if (request.getProperty("attach", "").equalsIgnoreCase("true"))
        try {
          String newPath = SearchHandler.getPreviewPath(stat, "2", 1);
          File f = new File(String.valueOf(ServerStatus.SG("previews_path")) + newPath.substring(1));
          if (f.exists())
            files.addElement(f); 
        } catch (Exception e) {
          Log.log("HTTP_SERVER", 2, e);
        }  
    } 
    response = String.valueOf(response) + "<commandResult><response>";
    if (!stop_share) {
      if (thisSession != null)
        try {
          for (x = 0; x < path_items.size() && msg.equals(""); x++) {
            Properties stat = path_items.elementAt(x);
            VRL vrl = new VRL(stat.getProperty("url"));
            stat.put("the_file_path", Common.all_but_last(vrl.getPath()));
            stat.put("the_file_name", Common.last(vrl.getPath()));
            stat.put("the_file_size", stat.getProperty("size"));
            stat.put("the_file_speed", "0");
            Properties metaInfo = new Properties();
            metaInfo.put("username", request.getProperty("username"));
            metaInfo.put("password", request.getProperty("password"));
            metaInfo.put("shareUsernamePermissions", request.getProperty("shareUsernamePermissions", ""));
            metaInfo.put("sendEmail", request.getProperty("sendEmail", ""));
            metaInfo.put("emailSubject", Common.url_decode(request.getProperty("emailSubject", "")));
            if (!request.getProperty("emailTo", "").equals(""))
              metaInfo.put("emailTo", request.getProperty("emailTo", "")); 
            metaInfo.put("web_link", request.getProperty("web_link", ""));
            metaInfo.put("web_link_end", request.getProperty("web_link_end", ""));
            metaInfo.put("publishType", request.getProperty("publishType", ""));
            metaInfo.put("allowUploads", request.getProperty("allowUploads", ""));
            if (!request.getProperty("emailFrom", "").equals(""))
              metaInfo.put("emailFrom", request.getProperty("emailFrom", "")); 
            if (!request.getProperty("emailCc", "").equals(""))
              metaInfo.put("emailCc", request.getProperty("emailCc", "")); 
            metaInfo.put("expire", request.getProperty("expire", ""));
            metaInfo.put("attach", request.getProperty("attach", ""));
            metaInfo.put("share_comments", request.getProperty("share_comments", ""));
            if (!request.getProperty("shareUsernames", "").equals(""))
              metaInfo.put("shareUsernames", request.getProperty("shareUsernames", "")); 
            if (!request.getProperty("shareUsername", "").equals(""))
              metaInfo.put("shareUsername", request.getProperty("shareUsername", "")); 
            Properties tempStat = ServerStatus.thisObj.statTools.add_item_stat(thisSession, stat, "SHARE");
            ServerStatus.thisObj.statTools.insertMetaInfo(thisSession.uiSG("SESSION_RID"), metaInfo, tempStat.getProperty("TRANSFER_RID"));
          } 
        } catch (Exception e) {
          Log.log("SERVER", 1, e);
        }  
      for (x = files.size() - 1; x >= 0; x--) {
        File f = files.elementAt(x);
        if (f.isDirectory() || f.length() > 10485760L)
          files.removeElementAt(x); 
      } 
      File_B[] files2 = (File_B[])null;
      if (files.size() > 0)
        files2 = new File_B[files.size()]; 
      for (int i = 0; i < files.size(); i++)
        files2[i] = new File_B(files.elementAt(i)); 
      String emailFrom = Common.url_decode(request.getProperty("emailFrom", "").replace('+', ' ').trim());
      String emailReplyTo = Common.url_decode(request.getProperty("emailReplyTo", "").replace('+', ' ').trim());
      String emailTo = Common.url_decode(request.getProperty("emailTo", "").replace('+', ' ').trim()).replace(';', ',');
      String emailCc = Common.url_decode(request.getProperty("emailCc", "").replace('+', ' ').trim()).replace(';', ',');
      String emailBcc = Common.url_decode(request.getProperty("emailBcc", "").replace('+', ' ').trim()).replace(';', ',');
      emailTo = Common.replace_str(emailTo, "{from}", emailFrom);
      emailCc = Common.replace_str(emailCc, "{from}", emailFrom);
      emailBcc = Common.replace_str(emailBcc, "{from}", emailFrom);
      emailReplyTo = Common.replace_str(emailReplyTo, "{from}", emailFrom);
      String emailSubject = Common.url_decode(request.getProperty("emailSubject", "").replace('+', ' ').trim());
      String emailBody = String.valueOf(Common.url_decode(request.getProperty("emailBody", ""))) + "\r\n\r\n";
      emailBody = Common.replace_str(emailBody, "&lt;", "<");
      emailBody = Common.replace_str(emailBody, "&gt;", ">");
      if (emailBody.indexOf("<") < 0)
        emailBody = emailBody.replace('+', ' ').trim(); 
      if (shareBodyEmailClient.equals(""))
        shareBodyEmailClient = emailBody; 
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "&lt;", "<");
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "&gt;", ">");
      SimpleDateFormat sdf_time = new SimpleDateFormat("HH:mm", Locale.US);
      Date d = ex2.parse(expire_date);
      String loginCount = request.getProperty("logins", "");
      if (loginCount.trim().equals(""))
        loginCount = "unlimited"; 
      emailBody = Common.replace_str(emailBody, "<LINE>", "{line_start}");
      emailBody = Common.replace_str(emailBody, "<line>", "{line_start}");
      emailBody = Common.replace_str(emailBody, "</LINE>", "{line_end}");
      emailBody = Common.replace_str(emailBody, "</line>", "{line_end}");
      if (emailBody.indexOf("{line_start}") >= 0)
        while (emailBody.indexOf("{line_start}") >= 0 && emailBody.indexOf("{line_end}") >= 0) {
          String line = emailBody.substring(emailBody.indexOf("{line_start}") + "{line_start}".length(), emailBody.indexOf("{line_end}"));
          String lines = "";
          for (int xx = 0; xx < paths.length; xx++) {
            String line2 = line;
            if (!paths[xx].trim().equals("")) {
              line2 = Common.replace_str(line2, "{web_link}", webLink);
              line2 = Common.replace_str(line2, "{web_link_end}", webLinkEnd);
              line2 = Common.replace_str(line2, "{username}", tempUsername);
              line2 = Common.replace_str(line2, "{password}", tempPassword);
              line2 = Common.replace_str(line2, "{user}", tempUsername);
              line2 = Common.replace_str(line2, "{pass}", tempPassword);
              line2 = Common.replace_str(line2, "{url}", Common.url_decode(request.getProperty("baseUrl")));
              line2 = Common.replace_str(line2, "{to}", emailTo);
              line2 = Common.replace_str(line2, "{from}", emailFrom);
              line2 = Common.replace_str(line2, "{reply_to}", emailReplyTo);
              line2 = Common.replace_str(line2, "{cc}", emailCc);
              line2 = Common.replace_str(line2, "{bcc}", emailBcc);
              line2 = Common.replace_str(line2, "{subject}", emailSubject);
              line2 = Common.replace_str(line2, "{paths}", Common.url_decode(request.getProperty("paths")));
              line2 = Common.replace_str(line2, "{path}", Common.all_but_last(paths[xx].trim()));
              line2 = Common.replace_str(line2, "{name}", Common.url_decode(Common.last(paths[xx].trim())));
              line2 = Common.replace_str(line2, "{datetime}", ex1.format(d).trim());
              line2 = Common.replace_str(line2, "{date}", date_time.format(d).trim());
              line2 = Common.replace_str(line2, "{time}", sdf_time.format(d).trim());
              line2 = Common.replace_str(line2, "{comments}", request.getProperty("share_comments", ""));
              if (lastStat != null)
                line2 = Common.replace_str(line2, "{size}", Common.format_bytes_short2(Long.parseLong(lastStat.getProperty("size", "0")))); 
              line2 = Common.replace_str(line2, "{total_size}", Common.format_bytes_short2(total_size));
              line2 = Common.replace_str(line2, "{logins}", loginCount);
              int k;
              for (k = 0; k < 100; k++) {
                String s = "";
                if ((paths[xx].split("/")).length > k)
                  s = paths[xx].split("/")[k]; 
                line2 = Common.replace_str(line2, "{" + k + "path}", s);
              } 
              for (k = 0; k < 100; k++) {
                String s = "";
                int m = (paths[xx].split("/")).length - 1 - k;
                if (m >= 0)
                  s = paths[xx].split("/")[m]; 
                line2 = Common.replace_str(line2, "{path" + k + "}", s);
              } 
              lines = String.valueOf(lines) + line2;
            } 
          } 
          emailBody = Common.replace_str(emailBody, "{line_start}" + line + "{line_end}", lines);
        }  
      int j;
      for (j = 0; j < 100; j++) {
        String s = "";
        if ((paths[0].split("/")).length > j)
          s = paths[0].split("/")[j]; 
        emailBody = Common.replace_str(emailBody, "{" + j + "path}", s);
      } 
      for (j = 0; j < 100; j++) {
        String s = "";
        int k = (paths[0].split("/")).length - 1 - j;
        if (k >= 0)
          s = paths[0].split("/")[k]; 
        emailBody = Common.replace_str(emailBody, "{path" + j + "}", s);
      } 
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "<LINE>", "{line_start}");
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "<line>", "{line_start}");
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "</LINE>", "{line_end}");
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "</line>", "{line_end}");
      if (shareBodyEmailClient.indexOf("{line_start}") >= 0)
        while (shareBodyEmailClient.indexOf("{line_start}") >= 0 && shareBodyEmailClient.indexOf("{line_end}") >= 0) {
          String line = shareBodyEmailClient.substring(shareBodyEmailClient.indexOf("{line_start}") + "{line_start}".length(), shareBodyEmailClient.indexOf("{line_end}"));
          String lines = "";
          for (int xx = 0; xx < paths.length; xx++) {
            String line2 = line;
            if (!paths[xx].trim().equals("")) {
              line2 = Common.replace_str(line2, "{web_link}", webLink);
              line2 = Common.replace_str(line2, "{web_link_end}", webLinkEnd);
              line2 = Common.replace_str(line2, "{username}", tempUsername);
              line2 = Common.replace_str(line2, "{password}", tempPassword);
              line2 = Common.replace_str(line2, "{user}", tempUsername);
              line2 = Common.replace_str(line2, "{pass}", tempPassword);
              line2 = Common.replace_str(line2, "{url}", Common.url_decode(request.getProperty("baseUrl")));
              line2 = Common.replace_str(line2, "{to}", emailTo);
              line2 = Common.replace_str(line2, "{from}", emailFrom);
              line2 = Common.replace_str(line2, "{reply_to}", emailReplyTo);
              line2 = Common.replace_str(line2, "{cc}", emailCc);
              line2 = Common.replace_str(line2, "{bcc}", emailBcc);
              line2 = Common.replace_str(line2, "{subject}", emailSubject);
              line2 = Common.replace_str(line2, "{paths}", Common.url_decode(request.getProperty("paths")));
              line2 = Common.replace_str(line2, "{path}", Common.all_but_last(paths[xx].trim()));
              line2 = Common.replace_str(line2, "{name}", Common.url_decode(Common.last(paths[xx].trim())));
              line2 = Common.replace_str(line2, "{datetime}", ex1.format(d).trim());
              line2 = Common.replace_str(line2, "{date}", date_time.format(d).trim());
              line2 = Common.replace_str(line2, "{time}", sdf_time.format(d).trim());
              line2 = Common.replace_str(line2, "{comments}", request.getProperty("share_comments", ""));
              if (lastStat != null)
                line2 = Common.replace_str(line2, "{size}", Common.format_bytes_short2(Long.parseLong(lastStat.getProperty("size", "0")))); 
              line2 = Common.replace_str(line2, "{total_size}", Common.format_bytes_short2(total_size));
              line2 = Common.replace_str(line2, "{logins}", loginCount);
              int k;
              for (k = 0; k < 100; k++) {
                String s = "";
                if ((paths[xx].split("/")).length > k)
                  s = paths[xx].split("/")[k]; 
                line2 = Common.replace_str(line2, "{" + k + "path}", s);
              } 
              for (k = 0; k < 100; k++) {
                String s = "";
                int m = (paths[xx].split("/")).length - 1 - k;
                if (m >= 0)
                  s = paths[xx].split("/")[m]; 
                line2 = Common.replace_str(line2, "{path" + k + "}", s);
              } 
              lines = String.valueOf(lines) + line2;
            } 
          } 
          shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "{line_start}" + line + "{line_end}", lines);
        }  
      for (j = 0; j < 100; j++) {
        String s = "";
        if ((paths[0].split("/")).length > j)
          s = paths[0].split("/")[j]; 
        shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "{" + j + "path}", s);
      } 
      for (j = 0; j < 100; j++) {
        String s = "";
        int k = (paths[0].split("/")).length - 1 - j;
        if (k >= 0)
          s = paths[0].split("/")[k]; 
        shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "{path" + j + "}", s);
      } 
      emailBody = Common.replace_str(emailBody, "<web_link>", webLink);
      emailBody = Common.replace_str(emailBody, "<web_link_end>", webLinkEnd);
      emailBody = Common.replace_str(emailBody, "<username>", tempUsername);
      emailBody = Common.replace_str(emailBody, "<password>", tempPassword);
      emailBody = Common.replace_str(emailBody, "%user%", tempUsername);
      emailBody = Common.replace_str(emailBody, "%pass%", tempPassword);
      emailBody = Common.replace_str(emailBody, "{user}", tempUsername);
      emailBody = Common.replace_str(emailBody, "{pass}", tempPassword);
      emailBody = Common.replace_str(emailBody, "<url>", Common.url_decode(request.getProperty("baseUrl")));
      emailBody = Common.replace_str(emailBody, "{web_link}", webLink);
      emailBody = Common.replace_str(emailBody, "{web_link_end}", webLinkEnd);
      emailBody = Common.replace_str(emailBody, "{username}", tempUsername);
      emailBody = Common.replace_str(emailBody, "{password}", tempPassword);
      emailBody = Common.replace_str(emailBody, "{url}", Common.url_decode(request.getProperty("baseUrl")));
      emailBody = Common.replace_str(emailBody, "{to}", emailTo);
      emailBody = Common.replace_str(emailBody, "{from}", emailFrom);
      emailBody = Common.replace_str(emailBody, "{reply_to}", emailReplyTo);
      emailBody = Common.replace_str(emailBody, "{cc}", emailCc);
      emailBody = Common.replace_str(emailBody, "{bcc}", emailBcc);
      emailBody = Common.replace_str(emailBody, "{subject}", emailSubject);
      emailBody = Common.replace_str(emailBody, "{paths}", Common.url_decode(request.getProperty("paths")));
      emailBody = Common.replace_str(emailBody, "{name}", Common.url_decode(last_name));
      emailBody = Common.replace_str(emailBody, "{comments}", Common.url_decode(request.getProperty("share_comments")));
      if (lastStat != null)
        emailBody = Common.replace_str(emailBody, "{size}", Common.format_bytes_short2(Long.parseLong(lastStat.getProperty("size", "0")))); 
      emailBody = Common.replace_str(emailBody, "{total_size}", Common.format_bytes_short2(total_size));
      emailBody = Common.replace_str(emailBody, "{logins}", loginCount);
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "<web_link>", webLink);
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "<web_link_end>", webLinkEnd);
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "<username>", tempUsername);
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "<password>", tempPassword);
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "%user%", tempUsername);
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "%pass%", tempPassword);
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "{user}", tempUsername);
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "{pass}", tempPassword);
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "<url>", Common.url_decode(request.getProperty("baseUrl")));
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "{web_link}", webLink);
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "{web_link_end}", webLinkEnd);
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "{username}", tempUsername);
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "{password}", tempPassword);
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "{url}", Common.url_decode(request.getProperty("baseUrl")));
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "{to}", emailTo);
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "{from}", emailFrom);
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "{reply_to}", emailReplyTo);
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "{cc}", emailCc);
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "{bcc}", emailBcc);
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "{subject}", emailSubject);
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "{paths}", Common.url_decode(request.getProperty("paths")));
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "{name}", Common.url_decode(last_name));
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "{comments}", Common.url_decode(request.getProperty("share_comments", "")));
      if (lastStat != null)
        shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "{size}", Common.format_bytes_short2(Long.parseLong(lastStat.getProperty("size", "0")))); 
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "{total_size}", Common.format_bytes_short2(total_size));
      shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "{logins}", loginCount);
      for (j = 0; j < 100; j++) {
        String s = "";
        if ((paths[0].split("/")).length > j)
          s = paths[0].split("/")[j]; 
        emailSubject = Common.replace_str(emailSubject, "{" + j + "path}", s);
      } 
      for (j = 0; j < 100; j++) {
        String s = "";
        int k = (paths[0].split("/")).length - 1 - j;
        if (k >= 0)
          s = paths[0].split("/")[k]; 
        emailSubject = Common.replace_str(emailSubject, "{path" + j + "}", s);
      } 
      emailSubject = Common.replace_str(emailSubject, "{username}", tempUsername);
      emailSubject = Common.replace_str(emailSubject, "{password}", tempPassword);
      emailSubject = Common.replace_str(emailSubject, "{web_link}", webLink);
      emailSubject = Common.replace_str(emailSubject, "{web_link_end}", webLinkEnd);
      emailSubject = Common.replace_str(emailSubject, "{to}", emailTo);
      emailSubject = Common.replace_str(emailSubject, "{from}", emailFrom);
      emailSubject = Common.replace_str(emailSubject, "{reply_to}", emailReplyTo);
      emailSubject = Common.replace_str(emailSubject, "{cc}", emailCc);
      emailSubject = Common.replace_str(emailSubject, "{bcc}", emailBcc);
      emailSubject = Common.replace_str(emailSubject, "{logins}", loginCount);
      emailSubject = Common.textFunctions(emailSubject, "{", "}");
      emailSubject = Common.textFunctions(emailSubject, "[", "]");
      emailSubject = Common.replace_str(emailSubject, "{total_size}", Common.format_bytes_short2(total_size));
      request.put("logins", loginCount);
      try {
        if (user != null)
          date_time = SessionCrush.updateDateCustomizations(date_time, user); 
        emailBody = Common.replace_str(emailBody, "<datetime>", ex1.format(d).trim());
        emailBody = Common.replace_str(emailBody, "<date>", date_time.format(d).trim());
        emailBody = Common.replace_str(emailBody, "<time>", sdf_time.format(d).trim());
        emailBody = Common.replace_str(emailBody, "{datetime}", ex1.format(d).trim());
        emailBody = Common.replace_str(emailBody, "{date}", date_time.format(d).trim());
        emailBody = Common.replace_str(emailBody, "{time}", sdf_time.format(d).trim());
        emailBody = Common.replace_str(emailBody, "{logins}", loginCount);
        emailBody = Common.textFunctions(emailBody, "{", "}");
        emailBody = Common.textFunctions(emailBody, "[", "]");
      } catch (Exception e) {
        Log.log("HTTP_SERVER", 1, e);
      } 
      request.put("date", date_time.format(d).trim());
      request.put("datetime", ex1.format(d).trim());
      request.put("time", sdf_time.format(d).trim());
      try {
        if (user != null)
          date_time = SessionCrush.updateDateCustomizations(date_time, user); 
        request.put("date", date_time.format(d).trim());
        request.put("datetime", ex1.format(d).trim());
        request.put("time", sdf_time.format(d).trim());
        shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "<datetime>", ex1.format(d).trim());
        shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "<date>", date_time.format(d).trim());
        shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "<time>", sdf_time.format(d).trim());
        shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "{datetime}", ex1.format(d).trim());
        shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "{date}", date_time.format(d).trim());
        shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "{time}", sdf_time.format(d).trim());
        shareBodyEmailClient = Common.replace_str(shareBodyEmailClient, "{logins}", loginCount);
        shareBodyEmailClient = Common.textFunctions(shareBodyEmailClient, "{", "}");
        shareBodyEmailClient = Common.textFunctions(shareBodyEmailClient, "[", "]");
      } catch (Exception e) {
        Log.log("HTTP_SERVER", 1, e);
      } 
      if (request.getProperty("sendEmail", "").equals("true")) {
        String result = Common.send_mail(ServerStatus.SG("discovered_ip"), emailTo, emailCc, emailBcc, emailFrom, emailReplyTo, emailSubject, emailBody, ServerStatus.SG("smtp_server"), ServerStatus.SG("smtp_user"), ServerStatus.SG("smtp_pass"), ServerStatus.SG("smtp_ssl").equals("true"), ServerStatus.SG("smtp_html").equals("true"), files2);
        if (result.toUpperCase().indexOf("SUCCESS") < 0) {
          msg = "ERROR: {share_complete} {email_failed} " + msg;
        } else {
          msg = "{share_complete}  {share_email_sent}. &nbsp;&nbsp;&nbsp;" + msg;
        } 
      } else {
        msg = "{share_complete} &nbsp;&nbsp;&nbsp;" + msg;
      } 
      msg = String.valueOf(msg) + "<a href=\"mailto:" + emailTo + "?ignore=false";
      if (!emailCc.trim().equals(""))
        msg = String.valueOf(msg) + "&cc=" + emailCc; 
      if (!emailBcc.trim().equals(""))
        msg = String.valueOf(msg) + "&bcc=" + emailBcc; 
      if (!emailSubject.trim().equals(""))
        msg = String.valueOf(msg) + "&subject=" + Common.url_encode(emailSubject); 
      if (!shareBodyEmailClient.trim().equals(""))
        msg = String.valueOf(msg) + "&body=" + Common.url_encode(shareBodyEmailClient); 
      msg = String.valueOf(msg) + "\">{share_open_in_email_client}</a>";
      if (thisSession != null)
        thisSession.do_event5("BATCH_COMPLETE", null); 
    } else if (request.getProperty("shareUsername", "false").equalsIgnoreCase("false")) {
      msg = "ERROR: " + msg;
    } 
    response = String.valueOf(response) + "<username>" + tempUsername + "</username>";
    response = String.valueOf(response) + "<password>" + tempPassword + "</password>";
    response = String.valueOf(response) + "<expire_date>" + expire_date + "</expire_date>";
    response = String.valueOf(response) + "<expire>" + request.getProperty("expire") + "</expire>";
    response = String.valueOf(response) + "<date>" + request.getProperty("date") + "</date>";
    response = String.valueOf(response) + "<datetime>" + request.getProperty("datetime") + "</datetime>";
    response = String.valueOf(response) + "<time>" + request.getProperty("time") + "</time>";
    response = String.valueOf(response) + "<logins>" + request.getProperty("logins") + "</logins>";
    response = String.valueOf(response) + "<message>" + Common.url_encode(msg) + "</message>";
    response = String.valueOf(response) + "<url>" + Common.url_encode(webLink) + "</url>";
    response = String.valueOf(response) + "<error_response></error_response>";
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static void doEventItem(Properties fileItem, SessionCrush thisSession, String event_type) {
    if (thisSession == null)
      return; 
    fileItem = (Properties)fileItem.clone();
    VRL vrl = new VRL(fileItem.getProperty("url"));
    Log.log("FTP_SERVER", 2, "Tracking " + event_type + ":" + vrl.getPath());
    fileItem.put("the_command", event_type);
    if (fileItem.containsKey("root_dir")) {
      fileItem.put("the_file_path", String.valueOf(fileItem.getProperty("root_dir")) + fileItem.getProperty("name") + (fileItem.getProperty("type").equals("DIR") ? "/" : ""));
    } else {
      fileItem.put("the_file_path", vrl.getPath());
    } 
    fileItem.put("the_command_data", fileItem.getProperty("the_file_path"));
    fileItem.put("the_file_name", fileItem.getProperty("name"));
    fileItem.put("the_file_size", fileItem.getProperty("size"));
    fileItem.put("the_file_speed", "0");
    fileItem.put("the_file_start", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
    fileItem.put("the_file_end", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
    fileItem.put("the_file_error", "");
    fileItem.put("the_file_type", fileItem.getProperty("type", ""));
    fileItem.put("the_file_status", "SUCCESS");
    ServerStatus.thisObj.statTools.add_item_stat(thisSession, fileItem, event_type);
    thisSession.do_event5(event_type, fileItem);
  }
  
  public static String createCustom(Vector path_items, Properties request, SessionCrush thisSession) throws Exception {
    String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
    if (request.getProperty("json", "false").equals("true"))
      response = ""; 
    for (int x = 0; x < path_items.size(); x++) {
      Properties stat = path_items.elementAt(x);
      request.putAll(stat);
      stat.putAll(request);
      doEventItem(stat, thisSession, "CUSTOM");
    } 
    if (thisSession != null)
      thisSession.do_event5("BATCH_COMPLETE", null); 
    if (request.getProperty("json", "false").equals("true")) {
      response = "{\r\n\"success\": true\r\n}\r\n";
    } else {
      response = String.valueOf(response) + "<commandResult><response>SUCCESS</response></commandResult>";
    } 
    return response;
  }
  
  public static String getHistory(Properties request, SessionCrush thisSession) throws Exception {
    String the_dir = Common.url_decode(request.getProperty("path", ""));
    the_dir = Common.dots(the_dir);
    if (the_dir.equals("/"))
      the_dir = thisSession.SG("root_dir"); 
    if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(thisSession.SG("root_dir").toUpperCase()))
      the_dir = String.valueOf(thisSession.SG("root_dir")) + the_dir.substring(1); 
    Properties item = thisSession.uVFS.get_item(the_dir);
    String info = "";
    if (ServerStatus.BG("v8_beta") && item != null && item.getProperty("type", "").equalsIgnoreCase("FILE") && item.getProperty("privs", "").indexOf("(sync") >= 0) {
      thisSession.uiPUT("current_dir", the_dir);
      String path = the_dir;
      String revPath = Common.parseSyncPart(item.getProperty("privs", ""), "revisionsPath");
      FileClient fc = new FileClient("file:///", "", new Vector());
      Vector listing = new Vector();
      for (int rev = 0; rev < 100; rev++) {
        if (!revPath.equals("")) {
          if (path.startsWith(thisSession.SG("root_dir")))
            path = path.substring(thisSession.SG("root_dir").length() - 1); 
          VRL vrl = new VRL(String.valueOf(revPath) + path + "/" + rev + "/" + item.getProperty("name"));
          if (!(new File(vrl.getPath())).exists())
            break; 
          Properties lp = fc.stat(vrl.getPath());
          lp.remove("url");
          lp.put("root_dir", item.getProperty("root_dir"));
          lp.put("href_path", String.valueOf(lp.getProperty("root_dir")) + lp.getProperty("name"));
          lp.put("crushftp_rev", (new StringBuffer(String.valueOf(rev))).toString());
          File info_xml = new File(String.valueOf(revPath) + path + "/" + rev + "/info.XML");
          if (info_xml.exists()) {
            Properties info2 = (Properties)Common.readXMLObject(info_xml);
            if (info2 != null)
              lp.putAll(info2); 
          } 
          listing.addElement(lp);
        } 
      } 
      Properties listingProp = new Properties();
      listingProp.put("listing", listing);
      String altList = AgentUI.getJsonListObj(listingProp, true);
      info = String.valueOf(info) + "{\r\n";
      info = String.valueOf(info) + "\t\"listing\" : " + altList + "\r\n";
      info = String.valueOf(info) + "}\r\n";
    } else {
      info = String.valueOf(info) + "{\r\n";
      info = String.valueOf(info) + "\t\"listing\" : []\r\n";
      info = String.valueOf(info) + "}\r\n";
    } 
    return info;
  }
  
  public static String manageShares(SessionCrush thisSession) throws Exception {
    Vector listing = new Vector();
    try {
      String tempAccountsPath = ServerStatus.SG("temp_accounts_path");
      File[] accounts = (new File(String.valueOf(tempAccountsPath) + "accounts/")).listFiles();
      thisSession.date_time = SessionCrush.updateDateCustomizations(thisSession.date_time, thisSession.user);
      if (accounts != null)
        for (int i = 0; i < accounts.length; i++) {
          try {
            File f = accounts[i];
            if (f.getName().indexOf(",,") >= 0 && f.isDirectory()) {
              String[] tokens = f.getName().split(",,");
              Properties pp = new Properties();
              for (int xx = 0; xx < tokens.length; xx++) {
                String key = tokens[xx].substring(0, tokens[xx].indexOf("="));
                String val = tokens[xx].substring(tokens[xx].indexOf("=") + 1);
                pp.put(key.toUpperCase(), val);
              } 
              if (thisSession.uiSG("user_name").equalsIgnoreCase(pp.getProperty("M"))) {
                SimpleDateFormat sdf1 = new SimpleDateFormat("MMddyyyyHHmm", Locale.US);
                SimpleDateFormat sdf2 = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US);
                Properties info = (Properties)Common.readXMLObject(String.valueOf(f.getPath()) + "/INFO.XML");
                info.putAll(pp);
                info.put("password", pp.getProperty("P"));
                info.remove("type");
                info.remove("master");
                try {
                  info.put("expire", thisSession.date_time.format(sdf1.parse(info.getProperty("EX"))));
                  info.put("expireMillis", (new StringBuffer(String.valueOf(sdf1.parse(info.getProperty("EX")).getTime()))).toString());
                } catch (Exception e) {
                  Log.log("HTTP_SERVER", 1, e);
                } 
                try {
                  info.put("createdMillis", (new StringBuffer(String.valueOf(sdf2.parse(info.getProperty("created")).getTime()))).toString());
                } catch (Exception e) {
                  Log.log("HTTP_SERVER", 1, e);
                } 
                info.remove("EX");
                info.remove("T");
                info.remove("P");
                info.remove("M");
                info.remove("U");
                info.put("downloads", "?");
                info.put("login_allowance", pp.getProperty("I", "-1"));
                try {
                  if (Common.System2.get("crushftp.dmz.queue.sock") != null) {
                    Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
                    Properties action = new Properties();
                    action.put("type", "GET:DOWNLOAD_COUNT");
                    action.put("id", Common.makeBoundary());
                    action.put("username", info.getProperty("username"));
                    action.put("need_response", "true");
                    queue.addElement(action);
                    action = UserTools.waitResponse(action, 60);
                    if (action != null)
                      info.put("downloads", action.get("responseText")); 
                  } else {
                    info.put("downloads", (new StringBuffer(String.valueOf(ServerStatus.thisObj.statTools.getUserDownloadCount(info.getProperty("username"))))).toString());
                  } 
                } catch (Exception e) {
                  Log.log("HTTP_SERVER", 1, e);
                } 
                String details = "";
                Enumeration keys = info.keys();
                while (keys.hasMoreElements()) {
                  String key = keys.nextElement().toString();
                  String val = info.getProperty(key, "");
                  val = Common.url_decode(val);
                  info.put(key, val);
                  details = String.valueOf(details) + key + ":" + val + "\r-------------------------------\r";
                  if (key.startsWith("ldap_"))
                    info.remove(key); 
                } 
                String details2 = details;
                info.put("details", details2);
                info.put("usernameShare", "false");
                listing.addElement(info);
              } 
            } 
          } catch (Exception e) {
            Log.log("HTTP_SERVER", 1, e);
          } 
        }  
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
    Vector user_list = new Vector();
    UserTools.refreshUserList(thisSession.server_item.getProperty("linkedServer"), user_list);
    for (int x = 0; x < user_list.size(); x++) {
      String newUser = Common.dots(user_list.elementAt(x).toString());
      if (newUser.toUpperCase().endsWith(".SHARED")) {
        Log.log("SERVER", 2, "ManageShares:Checking username:" + newUser + " (" + (x + 1) + "/" + user_list.size() + ")");
        VFS tempVFS = UserTools.ut.getVFS(thisSession.server_item.getProperty("linkedServer"), newUser);
        if (tempVFS != null) {
          Vector items = new Vector();
          tempVFS.getListing(items, "/Shares/" + thisSession.uiSG("user_name") + "/");
          for (int xx = 0; xx < items.size(); xx++) {
            Properties item_info2 = items.elementAt(xx);
            Properties item_info = tempVFS.get_item(String.valueOf(item_info2.getProperty("root_dir")) + item_info2.getProperty("name"));
            if (item_info == null)
              item_info = item_info2; 
            VRL vrl = new VRL(item_info.getProperty("url"));
            if (vrl.getProtocol().equalsIgnoreCase("virtual") || (vrl.getProtocol().equalsIgnoreCase("file") && !(new File(vrl.getPath())).exists())) {
              Properties request_fake = new Properties();
              request_fake.put("tempUsername", String.valueOf(newUser.substring(0, newUser.lastIndexOf("."))) + ":" + Common.url_encode((String)vrl));
              deleteShare(request_fake, thisSession);
            } else {
              Log.log("SERVER", 2, "ManageShares:Checking username:" + newUser + ":with item(" + (xx + 1) + ":" + items.size() + ")");
              Properties vItem = (Properties)item_info.get("vItem");
              Properties sharedUser = new Properties();
              sharedUser.put("web_link", "");
              sharedUser.put("username", newUser.substring(0, newUser.lastIndexOf(".")));
              sharedUser.put("password", "");
              sharedUser.put("emailFrom", "");
              sharedUser.put("emailReplyTo", "");
              sharedUser.put("emailTo", "Username Share : " + newUser.substring(0, newUser.lastIndexOf(".")));
              sharedUser.put("emailCc", "");
              sharedUser.put("emailBcc", "");
              sharedUser.put("emailSubject", "");
              sharedUser.put("emailBody", "");
              sharedUser.put("paths", (new VRL(item_info.getProperty("url", ""))).safe());
              if (vItem != null)
                sharedUser.put("expire", vItem.getProperty("expires_on", "never")); 
              if (sharedUser.getProperty("expire", "").trim().equals(""))
                sharedUser.put("expire", "never"); 
              sharedUser.put("details", "");
              sharedUser.put("attach", "false");
              sharedUser.put("usernameShare", "true");
              sharedUser.put("allowUploads", tempVFS.getCombinedPermissions().getProperty(("/Shares/" + thisSession.uiSG("user_name") + "/" + item_info.getProperty("name") + "/").toUpperCase(), "(none)"));
              sharedUser.put("publishType", "Internal Username Share");
              if (vItem != null && vItem.containsKey("created_on")) {
                sharedUser.put("created", vItem.getProperty("created_on"));
                sharedUser.put("createdMillis", (new StringBuffer(String.valueOf(UserTools.expire_vfs.parse(vItem.getProperty("created_on")).getTime()))).toString());
              } else {
                sharedUser.put("created", thisSession.date_time.format(new Date(Long.parseLong(item_info.getProperty("modified")))));
                sharedUser.put("createdMillis", item_info.getProperty("modified"));
              } 
              sharedUser.put("details", "");
              if (vItem != null)
                sharedUser.put("share_comments", vItem.getProperty("share_comments", "")); 
              listing.addElement(sharedUser);
            } 
          } 
        } 
      } 
    } 
    Log.log("SERVER", 2, "ManageShares:list size:" + listing.size());
    String response = "";
    try {
      response = Common.getXMLString(listing, "listingInfo", null);
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
    return response;
  }
  
  public static String deleteShare(Properties request, SessionCrush thisSession) {
    try {
      String tempAccountsPath = ServerStatus.SG("temp_accounts_path");
      File[] accounts = (new File(String.valueOf(tempAccountsPath) + "accounts/")).listFiles();
      String[] tempUsers = request.getProperty("tempUsername").split(";");
      for (int loop = 0; loop < tempUsers.length; loop++) {
        String curTempUser = tempUsers[loop].trim().replace('+', ' ');
        if (curTempUser.indexOf(":") >= 0) {
          String userid = Common.dots(Common.url_decode(curTempUser.substring(0, curTempUser.indexOf(":")))).replace('/', '_').replace('\\', '_');
          Log.log("HTTP_SERVER", 2, "Deleting userid:" + userid);
          String paths = curTempUser.substring(curTempUser.indexOf(":") + 1).trim();
          paths = Common.dots(Common.last(Common.url_decode(paths)));
          Log.log("HTTP_SERVER", 2, "Deleting userid paths:" + paths);
          VFS tempVFS = UserTools.ut.getVFS(thisSession.server_item.getProperty("linkedServer"), String.valueOf(userid) + ".SHARED");
          Properties virtual = tempVFS.homes.elementAt(0);
          Log.log("HTTP_SERVER", 2, "Loaded VFS:" + virtual);
          virtual.remove("/Shares/" + thisSession.uiSG("user_name") + "/" + paths);
          virtual.remove("/Shares/" + thisSession.uiSG("user_name") + "/" + paths.substring(0, paths.length() - 1));
          Properties permissions = ((Vector)virtual.get("vfs_permissions_object")).elementAt(0);
          if (permissions != null) {
            permissions.remove(("/Shares/" + thisSession.uiSG("user_name") + "/" + paths).toUpperCase());
            permissions.remove(("/Shares/" + thisSession.uiSG("user_name") + "/" + paths.substring(0, paths.length() - 1)).toUpperCase());
          } 
          Log.log("HTTP_SERVER", 2, "Removing entry:/Shares/" + thisSession.uiSG("user_name") + "/" + paths);
          Vector tempList = new Vector();
          tempVFS.getListing(tempList, "/Shares/" + thisSession.uiSG("user_name") + "/");
          if (tempList.size() == 0) {
            virtual.remove("/Shares/" + thisSession.uiSG("user_name") + "/");
            virtual.remove("/Shares/" + thisSession.uiSG("user_name"));
            if (permissions != null) {
              permissions.remove(("/Shares/" + thisSession.uiSG("user_name") + "/").toUpperCase());
              permissions.remove(("/Shares/" + thisSession.uiSG("user_name")).toUpperCase());
            } 
          } 
          tempList = new Vector();
          tempVFS.getListing(tempList, "/Shares/");
          if (tempList.size() == 0) {
            virtual.remove("/Shares/");
            virtual.remove("/Shares");
            if (permissions != null) {
              permissions.remove("/Shares/".toUpperCase());
              permissions.remove("/Shares".toUpperCase());
            } 
          } 
          Properties sharedUser = new Properties();
          sharedUser.put("password", "");
          sharedUser.put("version", "1.0");
          sharedUser.put("root_dir", "/");
          sharedUser.put("userVersion", "5");
          sharedUser.put("max_logins", "-1");
          UserTools.writeUser(thisSession.server_item.getProperty("linkedServer"), String.valueOf(userid) + ".SHARED", sharedUser);
          UserTools.writeVFS(thisSession.server_item.getProperty("linkedServer"), String.valueOf(userid) + ".SHARED", tempVFS);
        } else {
          for (int x = 0; accounts != null && x < accounts.length; x++) {
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
                if (thisSession.uiSG("user_name").equalsIgnoreCase(pp.getProperty("M")) && curTempUser.equalsIgnoreCase(pp.getProperty("U"))) {
                  Common.recurseDelete(String.valueOf(tempAccountsPath) + "storage/" + pp.getProperty("U") + pp.getProperty("P"), false);
                  Common.recurseDelete(f.getCanonicalPath(), false);
                } 
              } 
            } catch (Exception e) {
              Log.log("HTTP_SERVER", 1, e);
            } 
          } 
        } 
      } 
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
    String response = "";
    try {
      response = Common.getXMLString(request, "listingInfo", null);
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
    return response;
  }
  
  public static String selfRegistration(Properties request, SessionCrush thisSession, String req_id) {
    String response = "Success";
    try {
      Common.urlDecodePost(request);
      Enumeration keys = request.keys();
      Properties metaInfo = new Properties();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement().toString();
        if (key.toUpperCase().startsWith("META_")) {
          String val = request.getProperty(key);
          key = key.substring("META_".length());
          metaInfo.put(key, val);
          if (key.toUpperCase().startsWith("GLOBAL_")) {
            if (ServerStatus.thisObj.server_info.get("global_variables") == null)
              ServerStatus.thisObj.server_info.put("global_variables", new Properties()); 
            Properties global_variables = (Properties)ServerStatus.thisObj.server_info.get("global_variables");
            global_variables.put(key, val);
            continue;
          } 
          if (key.toUpperCase().startsWith("USER_INFO_"))
            thisSession.user_info.put(key, val); 
        } 
      } 
      Properties newUser = new Properties();
      String username = metaInfo.getProperty("registration_username");
      Properties customForm = null;
      Vector customForms = (Vector)ServerStatus.server_settings.get("CustomForms");
      String pendingSelfRegistration = "pendingSelfRegistration";
      if (customForms != null) {
        for (int x = 0; x < customForms.size(); x++) {
          Properties p = customForms.elementAt(x);
          if (p.getProperty("name", "").equals(metaInfo.getProperty("form_name")) && thisSession.SG("messageForm").indexOf(metaInfo.getProperty("form_name")) >= 0) {
            customForm = p;
            break;
          } 
        } 
        if (customForm != null) {
          if (!customForm.containsKey("entries"))
            customForm.put("entries", new Vector()); 
          Vector entries = (Vector)customForm.get("entries");
          for (int i = 0; i < entries.size(); i++) {
            Properties p = entries.elementAt(i);
            if (!p.getProperty("type").trim().equals("label")) {
              String val = metaInfo.getProperty(p.getProperty("name", "").trim());
              if (val != null && p.getProperty("name").trim().startsWith("registration_")) {
                newUser.put(p.getProperty("name").trim().substring("registration_".length()), val);
              } else if (p.getProperty("name").trim().startsWith("pendingSelfRegistration")) {
                pendingSelfRegistration = val.trim();
              } 
            } 
          } 
        } 
      } 
      newUser.put("root_dir", "/");
      newUser.put("user_name", username);
      newUser.put("max_logins", "-1");
      String originalPass = newUser.getProperty("password", newUser.getProperty("password_hidden", Common.makeBoundary()));
      newUser.put("password", ServerStatus.thisObj.common_code.encode_pass(newUser.getProperty("password", newUser.getProperty("password_hidden", Common.makeBoundary())), ServerStatus.SG("password_encryption"), ""));
      if (!Common.checkPasswordRequirements(originalPass, "").equals("")) {
        response = "Failure:" + Common.checkPasswordRequirements(originalPass, "");
        thisSession.add_log_formatted("Attempt to register a username using a weak password:" + username + ":" + response, "POST", req_id);
      } else if (UserTools.ut.getUser(thisSession.server_item.getProperty("linkedServer"), username, false) == null || UserTools.ut.getUser(thisSession.server_item.getProperty("linkedServer"), username, false).getProperty("username").equals("template")) {
        UserTools.writeUser(thisSession.server_item.getProperty("linkedServer"), username, newUser);
        Properties groups = UserTools.getGroups(thisSession.server_item.getProperty("linkedServer"));
        Vector groupUsers = (Vector)groups.get(pendingSelfRegistration);
        if (groupUsers == null)
          groupUsers = new Vector(); 
        groups.put(pendingSelfRegistration, groupUsers);
        if (groupUsers.indexOf(username) < 0)
          groupUsers.addElement(username); 
        if (!pendingSelfRegistration.equals("pendingSelfRegistration")) {
          Properties inheritance = UserTools.getInheritance(request.getProperty("serverGroup"));
          Vector vv = new Vector();
          vv.addElement(pendingSelfRegistration);
          inheritance.put(username, vv);
          UserTools.writeInheritance(thisSession.server_item.getProperty("linkedServer"), inheritance);
        } else {
          pendingSelfRegistration = "pendingSelfRegistration";
          groupUsers = (Vector)groups.get(pendingSelfRegistration);
          if (groupUsers == null)
            groupUsers = new Vector(); 
          groups.put(pendingSelfRegistration, groupUsers);
          if (groupUsers.indexOf(username) < 0)
            groupUsers.addElement(username); 
        } 
        UserTools.writeGroups(thisSession.server_item.getProperty("linkedServer"), groups);
        ServerStatus.thisObj.statTools.insertMetaInfo(thisSession.uiSG("SESSION_RID"), metaInfo, "0");
        thisSession.add_log("[" + thisSession.uiSG("user_number") + ":" + thisSession.uiSG("user_name") + ":" + thisSession.uiSG("user_ip") + "] DATA: *messageForm confirmed:" + metaInfo + "*", "HTTP");
        Properties fileItem = new Properties();
        fileItem.put("url", "ftp://127.0.0.1:56789/");
        fileItem.put("the_file_path", "/");
        fileItem.put("the_file_size", "1");
        fileItem.put("event_name", "registration");
        fileItem.put("the_file_name", "registration");
        fileItem.putAll(metaInfo);
        fileItem.put("metaInfo", metaInfo);
        thisSession.uiVG("lastUploadStats").addElement(fileItem);
        thisSession.do_event5("WELCOME", fileItem);
      } else {
        thisSession.add_log_formatted("Attempt to register an existing username:" + username, "POST", req_id);
        response = "Failure";
      } 
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
    return response;
  }
  
  public static String editShare(Properties request, SessionCrush thisSession) {
    try {
      String tempAccountsPath = ServerStatus.SG("temp_accounts_path");
      File[] accounts = (new File(String.valueOf(tempAccountsPath) + "accounts/")).listFiles();
      String[] tempUsers = request.getProperty("tempUsername").split(";");
      for (int loop = 0; loop < tempUsers.length; loop++) {
        String curTempUser = tempUsers[loop].trim().replace('+', ' ');
        for (int x = 0; accounts != null && x < accounts.length; x++) {
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
              if (thisSession.uiSG("user_name").equalsIgnoreCase(pp.getProperty("M")) && curTempUser.equalsIgnoreCase(pp.getProperty("U"))) {
                int maxExpireDays = 0;
                Vector web_customizations = (Vector)thisSession.user.get("web_customizations");
                if (web_customizations != null)
                  for (int i = 0; i < web_customizations.size(); i++) {
                    Properties cust = web_customizations.elementAt(i);
                    if (cust.getProperty("key").equals("EXPIREDAYSMAX"))
                      maxExpireDays = Integer.parseInt(cust.getProperty("value").trim()); 
                  }  
                SimpleDateFormat ex1 = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US);
                SimpleDateFormat ex2 = new SimpleDateFormat("MMddyyyyHHmm", Locale.US);
                Date requestExpire = ex1.parse(request.getProperty("expire", "1/1/1970 00:01").replace('+', ' '));
                Calendar gc = new GregorianCalendar();
                gc.setTime(new Date());
                gc.add(5, maxExpireDays);
                if (maxExpireDays > 0 && (!request.containsKey("expire") || requestExpire.getTime() > gc.getTime().getTime()))
                  requestExpire = gc.getTime(); 
                String expire_date = ex2.format(requestExpire);
                request.put("expire", ex1.format(requestExpire));
                String folderName = "u=" + pp.getProperty("U") + ",,p=" + pp.getProperty("P") + ",,m=" + pp.getProperty("M") + ",,t=TempAccount,,ex=" + expire_date;
                if (request.getProperty("logins", "").trim().equals("-1"))
                  request.remove("logins"); 
                if (!request.getProperty("logins", "").equals(""))
                  folderName = String.valueOf(folderName) + ",,i=" + request.getProperty("logins", ""); 
                Properties info = (Properties)Common.readXMLObject(String.valueOf(f.getPath()) + "/INFO.XML");
                info.put("EX", expire_date);
                Common.writeXMLObject(String.valueOf(f.getPath()) + "/INFO.XML", info, "INFO");
                f.renameTo(new File(String.valueOf(Common.all_but_last(f.getPath())) + folderName));
              } 
            } 
          } catch (Exception e) {
            Log.log("HTTP_SERVER", 1, e);
          } 
        } 
      } 
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
    String response = "";
    try {
      response = Common.getXMLString(request, "listingInfo", null);
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
    return response;
  }
  
  public String processKeywordsEdit(Properties request) throws Exception {
    String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
    String[] names = (String[])null;
    if (request.getProperty("names").indexOf(";") >= 0) {
      names = Common.url_decode(request.getProperty("names")).replace('>', '_').replace('<', '_').split(";");
    } else {
      names = Common.url_decode(request.getProperty("names")).replace('>', '_').replace('<', '_').split("\r\n");
    } 
    String keyword = Common.url_decode(request.getProperty("keywords")).trim();
    response = String.valueOf(response) + "<commandResult><response>";
    boolean ok = false;
    for (int x = 0; x < names.length; x++) {
      String the_dir = Common.url_decode(Common.all_but_last(names[x]));
      if (the_dir.startsWith(this.thisSessionHTTP.thisSession.SG("root_dir")))
        the_dir = the_dir.substring(this.thisSessionHTTP.thisSession.SG("root_dir").length() - 1); 
      this.thisSessionHTTP.setupCurrentDir(the_dir);
      Properties item = this.thisSessionHTTP.thisSession.uVFS.get_item(String.valueOf(this.thisSessionHTTP.pwd()) + Common.last(names[x]));
      the_dir = SearchHandler.getPreviewPath(item, "1", 1);
      String index = String.valueOf(ServerStatus.SG("previews_path")) + the_dir.substring(1);
      if (!(new File(Common.all_but_last(index))).exists())
        (new File(Common.all_but_last(index))).mkdirs(); 
      if (ServerStatus.BG("exif_keywords")) {
        String srcFile = Common.dots((new VRL(item.getProperty("url"))).getPath());
        for (int xx = 0; xx < ServerStatus.thisObj.previewWorkers.size(); xx++) {
          PreviewWorker preview = ServerStatus.thisObj.previewWorkers.elementAt(xx);
          if (preview.prefs.getProperty("preview_enabled", "false").equalsIgnoreCase("true") && preview.checkExtension(Common.last(the_dir), item)) {
            preview.setExifInfo(srcFile, PreviewWorker.getDestPath2(String.valueOf(item.getProperty("url")) + "/p1/"), "keywords", Common.url_decode(request.getProperty("keywords")).trim());
            ok = true;
            break;
          } 
        } 
      } else {
        (new File(String.valueOf(Common.all_but_last(Common.all_but_last(index))) + "index.txt")).delete();
        RandomAccessFile out = new RandomAccessFile(String.valueOf(Common.all_but_last(Common.all_but_last(index))) + "index.txt", "rw");
        out.write((String.valueOf(keyword) + "\r\n").getBytes());
        out.close();
        ok = true;
      } 
      SearchHandler.buildEntry(item, this.thisSessionHTTP.thisSession.uVFS, false, false);
    } 
    if (ok)
      response = String.valueOf(response) + "Keywords Edited.\r\n"; 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
}
