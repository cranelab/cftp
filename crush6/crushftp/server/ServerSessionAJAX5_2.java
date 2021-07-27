package crushftp.server;

import com.crushftp.client.Base64;
import com.crushftp.client.Common;
import com.crushftp.client.GenericClient;
import com.crushftp.client.HTTPClient;
import com.crushftp.client.URLConnection;
import com.crushftp.client.VRL;
import com.crushftp.tunnel2.Tunnel2;
import crushftp.db.SearchHandler;
import crushftp.gui.LOC;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.PreviewWorker;
import crushftp.handlers.SharedSession;
import crushftp.handlers.SyncTools;
import crushftp.handlers.UserTools;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

public class ServerSessionAJAX5_2 {
  ServerSession thisSession = null;
  
  ServerSessionHTTP5_2 thisSessionHTTP = null;
  
  public ServerSessionAJAX5_2(ServerSession thisSession, ServerSessionHTTP5_2 thisSessionHTTP) {
    this.thisSession = thisSession;
    this.thisSessionHTTP = thisSessionHTTP;
  }
  
  public boolean checkLogin1(Properties request) throws Exception {
    if (this.thisSession.server_item.getProperty("recaptcha_enabled", "false").equals("true")) {
      HttpURLConnection urlc = (HttpURLConnection)(new URL("http://www.google.com/recaptcha/api/verify")).openConnection();
      urlc.setDoOutput(true);
      String s = "privatekey=" + this.thisSession.server_item.getProperty("recaptcha_private_key", "") + "&remoteip=" + this.thisSession.uiSG("user_ip") + "&challenge=" + request.getProperty("recaptcha_challenge_field", "") + "&response=" + request.getProperty("recaptcha_response_field", "");
      OutputStream out = urlc.getOutputStream();
      out.write(s.getBytes("UTF8"));
      out.close();
      BufferedReader br = new BufferedReader(new InputStreamReader(urlc.getInputStream()));
      String result = br.readLine();
      br.close();
      urlc.disconnect();
      if (result.equalsIgnoreCase("false"))
        return false; 
    } 
    if (request.getProperty("clientid", "").equalsIgnoreCase("CrushFTPDrive") && ServerStatus.siIG("enterprise_level") <= 0)
      return false; 
    if (ServerStatus.BG("username_uppercase"))
      request.put("username", request.getProperty("username").toUpperCase()); 
    this.thisSession.uiPUT("user_name", "anonymous");
    this.thisSession.uiPUT("user_name_original", VRL.vrlDecode(this.thisSession.uiSG("user_name")));
    this.thisSession.uiPUT("current_password", VRL.vrlDecode(request.getProperty("password")));
    this.thisSession.uiPUT("user_name", VRL.vrlDecode(request.getProperty("username")));
    this.thisSession.uiPUT("clientid", request.getProperty("clientid", ""));
    this.thisSession.uiPUT("user_name_original", this.thisSession.uiSG("user_name"));
    this.thisSessionHTTP.this_thread.setName(String.valueOf(this.thisSession.uiSG("user_name")) + ":(" + this.thisSession.uiSG("user_number") + ")-" + this.thisSession.uiSG("user_ip") + " (control)");
    if (this.thisSession.uVFS != null)
      this.thisSession.uVFS.free(); 
    this.thisSession.uVFS = null;
    this.thisSession.uiPUT("skip_proxy_check", "false");
    this.thisSession.runPlugin("beforeLogin", null);
    boolean good = this.thisSession.login_user_pass();
    this.thisSessionHTTP.setupSession();
    return good;
  }
  
  public String checkLogin2(String response, Properties request) {
    if (this.thisSession.uiBG("password_expired")) {
      response = String.valueOf(response) + "<loginResult><response>password_expired</response><message>You must change your password.  Please login using a new password.</message></loginResult>";
    } else {
      response = String.valueOf(response) + "<loginResult><response>success</response></loginResult>";
    } 
    this.thisSessionHTTP.createCookieSession(false);
    this.thisSession.user_info.put("post_parameters", request);
    this.thisSession.do_event5("LOGIN", null);
    this.thisSessionHTTP.writeCookieAuth = true;
    Properties session = (Properties)SharedSession.find("crushftp.sessions").get(this.thisSession.getId());
    if (session == null) {
      response = String.valueOf(response) + "<loginResult><response>failure</response><message>session expired</message></loginResult>";
    } else {
      session.put("expire_time", "0");
      if (this.thisSession.IG("max_login_time") != 0)
        session.put("expire_time", (new StringBuffer(String.valueOf(System.currentTimeMillis() + (this.thisSession.IG("max_login_time") * 60000)))).toString()); 
      if (request.containsKey("clientid")) {
        session.put("clientid", request.getProperty("clientid"));
        this.thisSession.user_info.put("clientid", request.getProperty("clientid"));
      } 
      session.put("SESSION_RID", this.thisSession.uiSG("SESSION_RID"));
      if (this.thisSession.user_info.containsKey("eventResultText"))
        response = this.thisSession.user_info.getProperty("eventResultText"); 
    } 
    return response;
  }
  
  public boolean processItemAnonymous(Properties request, Properties urlRequestItems) throws Exception {
    if (request.getProperty("command", "").equalsIgnoreCase("ping")) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      response = String.valueOf(response) + "<pong>" + System.currentTimeMillis() + "</pong>";
      this.thisSessionHTTP.done = true;
      return writeResponse(response);
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("discard")) {
      this.thisSessionHTTP.done = true;
      return writeResponse("");
    } 
    if (request.getProperty("encoded", "true").equals("true") && (request.getProperty("command", "").equalsIgnoreCase("login") || request.getProperty("the_action", "").equalsIgnoreCase("login"))) {
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
        SharedSession.find("crushftp.usernames.activity").put(this.thisSession.getId(), (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
      } else {
        this.thisSession.uiPUT("user_name", "");
        this.thisSession.uiPUT("user_name_original", this.thisSession.uiSG("user_name"));
        response = String.valueOf(response) + "<loginResult><response>failure</response><message>Check your username or password and try again.\r\n" + this.thisSession.uiSG("lastLog") + "</message></loginResult>";
      } 
      return writeResponse(response);
    } 
    if (request.containsKey("u") && request.containsKey("p")) {
      this.thisSession.uiPUT("user_name", "anonymous");
      this.thisSession.uiPUT("user_name_original", this.thisSession.uiSG("user_name"));
      request.put("password", request.getProperty("p"));
      request.put("username", request.getProperty("u"));
      boolean good = checkLogin1(request);
      if (good) {
        checkLogin2("", request);
        String autoPath = request.getProperty("path", "/");
        this.thisSession.do_event5("LOGIN", null);
        urlRequestItems.remove("p");
        Properties session = (Properties)SharedSession.find("crushftp.sessions").get(this.thisSession.getId());
        session.putAll(urlRequestItems);
        SharedSession.find("crushftp.usernames.activity").put(this.thisSession.getId(), (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
        if (!this.thisSession.user_info.getProperty("redirect_url", "").equals("")) {
          autoPath = this.thisSession.user_info.getProperty("redirect_url", "");
          this.thisSession.user_info.put("redirect_url", "");
        } 
        if (!autoPath.endsWith("/") && !autoPath.equals("") && autoPath.indexOf("/WebInterface/") < 0 && ServerStatus.BG("direct_link_access")) {
          String header0 = this.thisSessionHTTP.headers.elementAt(0).toString();
          header0 = String.valueOf(header0.substring(0, header0.indexOf(" "))) + " " + Common.dots(autoPath) + header0.substring(header0.lastIndexOf(" "));
          this.thisSessionHTTP.headers.setElementAt(header0, 0);
          request.remove("path");
          return false;
        } 
        this.thisSessionHTTP.sendRedirect(autoPath);
        write_command_http("Content-Length: 0");
        write_command_http("");
        return true;
      } 
      if (!this.thisSession.user_info.getProperty("redirect_url", "").equals("")) {
        this.thisSessionHTTP.sendRedirect(this.thisSession.user_info.getProperty("redirect_url", ""));
        write_command_http("Content-Length: 0");
        write_command_http("");
        return true;
      } 
    } else {
      if (request.getProperty("command", "").equalsIgnoreCase("emailpassword") || request.getProperty("the_action", "").equalsIgnoreCase("emailpassword")) {
        String response = doEmailPass(request, this.thisSession);
        response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n<emailPass><response>" + response + "</response></emailPass>";
        return writeResponse(response);
      } 
      if (request.getProperty("command", "").equalsIgnoreCase("request_reset")) {
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        String reset_username_email = Common.url_decode(request.getProperty("reset_username_email"));
        Vector newUsers = new Vector();
        Vector matchingUsernames = new Vector();
        UserTools.refreshUserList(this.thisSession.server_item.getProperty("linkedServer"), newUsers);
        int x;
        for (x = 0; x < newUsers.size() && matchingUsernames.size() < 1000; x++) {
          String path = String.valueOf(System.getProperty("crushftp.users")) + this.thisSession.server_item.getProperty("linkedServer") + "/" + newUsers.elementAt(x) + "/user.XML";
          if ((new File(path)).exists()) {
            RandomAccessFile in = new RandomAccessFile(path, "r");
            byte[] b = new byte[(int)in.length()];
            in.read(b);
            in.close();
            String s = (new String(b, "UTF8")).toUpperCase();
            if (s.indexOf(reset_username_email.toUpperCase()) > 0) {
              Properties user = (Properties)Common.readXMLObject(new ByteArrayInputStream(b));
              user.put("username", newUsers.elementAt(x));
              user.put("user_name", newUsers.elementAt(x));
              matchingUsernames.addElement(user);
            } 
          } 
        } 
        for (x = matchingUsernames.size() - 1; x >= 0; x--) {
          Properties user = matchingUsernames.elementAt(x);
          if (user.getProperty("email", "").equals("") || (!user.getProperty("username").equalsIgnoreCase(reset_username_email) && !user.getProperty("email", "").equalsIgnoreCase(reset_username_email))) {
            matchingUsernames.removeElementAt(x);
          } else {
            matchingUsernames.setElementAt(user, x);
          } 
        } 
        String responseText = "";
        if (matchingUsernames.size() == 1) {
          Properties user = matchingUsernames.elementAt(0);
          Properties resetTokens = ServerStatus.siPG("resetTokens");
          if (resetTokens == null)
            resetTokens = new Properties(); 
          ServerStatus.thisObj.server_info.put("resetTokens", resetTokens);
          String token = Common.makeBoundary();
          Properties reset = UserTools.ut.getUser(this.thisSession.server_item.getProperty("linkedServer"), user.getProperty("username"), true);
          reset.put("generated", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
          resetTokens.put(token, reset);
          String url = String.valueOf(request.getProperty("currentURL")) + "?username=" + user.getProperty("username") + "&token=" + token;
          String resetMsg = ServerStatus.SG("password_reset_message");
          resetMsg = Common.replace_str(resetMsg, "{url}", url);
          resetMsg = ServerStatus.change_vars_to_values_static(resetMsg, user, new Properties(), null);
          Common.send_mail(ServerStatus.SG("discovered_ip"), reset.getProperty("email"), "", "", ServerStatus.SG("smtp_from"), ServerStatus.SG("password_reset_subject"), resetMsg, ServerStatus.SG("smtp_server"), ServerStatus.SG("smtp_user"), ServerStatus.SG("smtp_pass"), ServerStatus.SG("smtp_ssl").equals("true"), true, null);
          responseText = ServerStatus.SG("password_reset_message_browser");
        } else {
          responseText = "Unable to locate this user.";
        } 
        response = String.valueOf(response) + "<commandResult><response>" + responseText + "</response></commandResult>";
        return writeResponse(response);
      } 
      if (request.getProperty("command", "").equalsIgnoreCase("reset_password")) {
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        Properties resetTokens = ServerStatus.siPG("resetTokens");
        if (resetTokens == null)
          resetTokens = new Properties(); 
        ServerStatus.thisObj.server_info.put("resetTokens", resetTokens);
        String responseText = "This password reset link is invalid or expired.";
        if (resetTokens.containsKey(request.getProperty("resetToken"))) {
          Properties reset = (Properties)resetTokens.get(request.getProperty("resetToken"));
          if (reset.getProperty("site", "").indexOf("(SITE_PASS)") < 0) {
            responseText = "ERROR: Your username does not allow password changes.";
          } else if (!Common.checkPasswordRequirements(Common.url_decode(request.getProperty("password1")), reset.getProperty("password_history", "")).equals("")) {
            responseText = "ERROR: " + Common.checkPasswordRequirements(Common.url_decode(request.getProperty("password1")), reset.getProperty("password_history", ""));
          } else {
            resetTokens.remove(request.getProperty("resetToken"));
            UserTools.ut.put_in_user(this.thisSession.server_item.getProperty("linkedServer"), reset.getProperty("username"), "password", ServerStatus.thisObj.common_code.encode_pass(Common.url_decode(request.getProperty("password1")), ServerStatus.SG("password_encryption")));
            UserTools.ut.put_in_user(this.thisSession.server_item.getProperty("linkedServer"), reset.getProperty("username"), "password_history", Common.getPasswordHistory(Common.url_decode(request.getProperty("password1")), reset.getProperty("password_history", "")));
            Calendar gc = new GregorianCalendar();
            gc.setTime(new Date());
            gc.add(5, Integer.parseInt(reset.getProperty("expire_password_days", "0")));
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa");
            String s = sdf.format(gc.getTime());
            UserTools.ut.put_in_user(this.thisSession.server_item.getProperty("linkedServer"), reset.getProperty("username"), "expire_password_when", s);
            responseText = "Password changed.  Please login using your new password.";
          } 
        } 
        response = String.valueOf(response) + "<commandResult><response>" + responseText + "</response></commandResult>";
        return writeResponse(response);
      } 
      if (request.getProperty("command", "").equalsIgnoreCase("getSessionTimeout")) {
        long time = Long.parseLong(SharedSession.find("crushftp.usernames.activity").getProperty(this.thisSession.getId(), "0"));
        long remaining = (60 * ServerStatus.IG("http_session_timeout")) - (new Date()).getTime() / 1000L - time / 1000L;
        try {
          Properties session = (Properties)SharedSession.find("crushftp.sessions").get(this.thisSession.getId());
          if (session != null)
            if (!session.getProperty("expire_time").equals("0"))
              remaining = (Long.parseLong(session.getProperty("expire_time")) - System.currentTimeMillis()) / 1000L;  
        } catch (Exception exception) {}
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        response = String.valueOf(response) + "<commandResult><response>" + remaining + "</response></commandResult>";
        return writeResponse(response);
      } 
      if (request.getProperty("command", "").equalsIgnoreCase("encryptPassword")) {
        String encryptedPass = ServerStatus.thisObj.common_code.encode_pass(Common.url_decode(request.getProperty("password")), "DES");
        String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
        response = String.valueOf(response) + "<result><response>" + Common.url_encode(encryptedPass) + "</response></result>";
        return writeResponse(response);
      } 
      if (request.getProperty("command", "").equalsIgnoreCase("taskResponse")) {
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
    } 
    return false;
  }
  
  public static String doEmailPass(Properties p, ServerSession thisSession) {
    String lookupUsername = p.getProperty("username");
    thisSession.verify_user(lookupUsername, "", true);
    Properties lookupUser = thisSession.user;
    thisSession.user = null;
    if (thisSession.uVFS != null)
      thisSession.uVFS.free(); 
    thisSession.uVFS = null;
    String result = "";
    String standardError = LOC.G("Sorry this username doesn't exist, or there is no email specified in the account, or you are not allowed to have your password emailed to you.");
    if (ServerStatus.SG("smtp_server").equals("")) {
      result = LOC.G("This server is not configured to send email password reminders.");
    } else if (lookupUser == null) {
      lookupUser = new Properties();
      result = standardError;
    } else if (lookupUser.getProperty("site", "").toUpperCase().indexOf("(SITE_EMAILPASSWORD)") >= 0) {
      String pass = lookupUser.getProperty("password", "");
      if (pass.startsWith("SHA:") || pass.startsWith("SHA512:") || pass.startsWith("MD5:") || pass.startsWith("CRYPT3:")) {
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
        result = Common.send_mail(ServerStatus.SG("discovered_ip"), to, "", "", from, subject, body, ServerStatus.SG("smtp_server"), ServerStatus.SG("smtp_user"), ServerStatus.SG("smtp_pass"), ServerStatus.SG("smtp_ssl").equals("true"), ServerStatus.SG("smtp_html").equals("true"), null);
        thisSession.add_log_formatted(String.valueOf(LOC.G("Password Emailed to user:")) + lookupUsername + "  " + to + "   " + LOC.G("Email Result:") + result, "POST");
        if (result.toUpperCase().indexOf("ERROR") >= 0) {
          result = LOC.G("An error occured when generating the email.");
        } else {
          result = LOC.G("An email was just sent to the email address associated with your username.");
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
      if (this.thisSession.uiBG("user_logged_in") && !this.thisSession.uiSG("user_name").equals("")) {
        response = String.valueOf(response) + "<loginResult><response>success</response><username>" + this.thisSession.uiSG("user_name") + "</username></loginResult>";
      } else {
        response = String.valueOf(response) + "<loginResult><response>failure</response></loginResult>";
      } 
      return writeResponse(response);
    } 
    return false;
  }
  
  public void logout_all() {
    this.thisSession.do_event5("LOGOUT_ALL", null);
    this.thisSession.uiPUT("user_name_original", "");
    this.thisSessionHTTP.createCookieSession(true);
    this.thisSessionHTTP.done = true;
    if (this.thisSession.uVFS != null) {
      this.thisSession.uVFS.free();
      this.thisSession.uVFS.connectedVFSItems.clear();
      this.thisSession.uVFS.disconnect();
    } 
  }
  
  public boolean processItems(Properties request, Vector byteRanges) throws Exception {
    if (this.thisSession.SG("site").indexOf("(CONNECT)") < 0)
      request.put("serverGroup", this.thisSession.server_item.getProperty("linkedServer")); 
    if (request.getProperty("command", "").equalsIgnoreCase("getServerItem") && (this.thisSession.SG("site").indexOf("(CONNECT)") >= 0 || this.thisSession.SG("site").indexOf("(USER_ADMIN)") >= 0))
      return writeResponse(AdminControls.buildXML(AdminControls.getServerItem(this.thisSession.SG("admin_group_name"), request, (this.thisSession.SG("site").indexOf("(CONNECT)") < 0)), "result_value", "OK"), false, 200, false, false); 
    if (request.getProperty("command", "").equalsIgnoreCase("getStatHistory") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.getStatHistory(request), false, 200, true, false); 
    if (request.getProperty("command", "").equalsIgnoreCase("getJobsSummary") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.buildXML(AdminControls.getJobsSummary(request), "running_tasks", "OK"), false, 200, false, false); 
    if (request.getProperty("command", "").equalsIgnoreCase("getJobInfo") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.buildXML(AdminControls.getJobInfo(request), "running_tasks", "OK"), false, 200, false, false); 
    if (request.getProperty("command", "").equalsIgnoreCase("getSessionList") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.buildXML(AdminControls.getSessionList(request), "session_list", "OK"), false, 200, true, false); 
    if (request.getProperty("command", "").equalsIgnoreCase("getLog") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0) {
      urlDecodePost(request);
      return writeResponse(AdminControls.buildXML(AdminControls.getLog(request), "log_data", "OK"), false, 200, false, false);
    } 
    String last_activity_time = SharedSession.find("crushftp.usernames.activity").getProperty(this.thisSession.getId());
    SharedSession.find("crushftp.usernames.activity").put(this.thisSession.getId(), (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
    Properties session = (Properties)SharedSession.find("crushftp.sessions").get(this.thisSession.getId());
    if (request.getProperty("command", "").equalsIgnoreCase("logout")) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      response = String.valueOf(response) + "<commandResult><response>Logged out.</response></commandResult>";
      logout_all();
      writeResponse(response);
      return true;
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("getCrushAuth")) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      response = String.valueOf(response) + "<auth>CrushAuth=" + this.thisSession.getId() + "</auth>";
      return writeResponse(response);
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("startTunnel2")) {
      String response = "";
      SharedSession.find("crushftp.usernames").put(String.valueOf(Common.getPartialIp("127.0.0.1")) + "_" + this.thisSession.getId() + "_user", SharedSession.find("crushftp.usernames").getProperty(String.valueOf(Common.getPartialIp(this.thisSession.uiSG("user_ip"))) + "_" + this.thisSession.getId() + "_user"));
      SharedSession.find("crushftp.usernames").put(String.valueOf(Common.getPartialIp("127.0.0.1")) + "_" + this.thisSession.getId() + "_userProp", SharedSession.find("crushftp.usernames").get(String.valueOf(Common.getPartialIp(this.thisSession.uiSG("user_ip"))) + "_" + this.thisSession.getId() + "_userProp"));
      SharedSession.find("crushftp.usernames").put(String.valueOf(Common.getPartialIp("127.0.0.1")) + "_" + this.thisSession.getId() + "_vfs", SharedSession.find("crushftp.usernames").get(String.valueOf(Common.getPartialIp(this.thisSession.uiSG("user_ip"))) + "_" + this.thisSession.getId() + "_vfs"));
      SharedSession.find("crushftp.usernames").put(String.valueOf(Common.getPartialIp("127.0.0.1")) + "_" + this.thisSession.getId() + "_ip", this.thisSessionHTTP.thisSession.uiSG("user_ip"));
      String userTunnels = String.valueOf(this.thisSessionHTTP.thisSession.user.getProperty("tunnels", "")) + ",";
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
        if (Tunnel2.getTunnel(this.thisSession.getId()) == null) {
          Tunnel2 t = new Tunnel2(this.thisSession.getId(), tunnel);
          if (tunnel.getProperty("reverse", "false").equals("true")) {
            t.setAllowReverseMode(true);
            t.startThreads();
          } 
        } 
      } 
      return writeResponse(response);
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("stopTunnel2")) {
      String response = Tunnel2.stopTunnel(this.thisSession.getId());
      return writeResponse(response);
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("getSyncTableData") && ServerStatus.siIG("enterprise_level") > 0) {
      Properties item2 = this.thisSession.uVFS.get_item(String.valueOf(this.thisSession.SG("root_dir")) + request.getProperty("path"));
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
            action.put("username", this.thisSession.uiSG("user_name"));
            action.put("request", request);
            action.put("syncID", syncID.toUpperCase());
            action.put("clientid", this.thisSession.uiSG("clientid"));
            action.put("need_response", "true");
            queue.addElement(action);
            action = UserTools.waitResponse(action, 300);
            o = action.remove("object_response");
          } else {
            String vfs_path = request.getProperty("path", "");
            if (vfs_path.equals(""))
              vfs_path = "/"; 
            if (!vfs_path.startsWith(this.thisSession.SG("root_dir")))
              vfs_path = String.valueOf(this.thisSession.SG("root_dir")) + vfs_path.substring(1); 
            o = Common.getSyncTableData(syncID.toUpperCase(), Long.parseLong(request.getProperty("lastRID")), request.getProperty("table"), this.thisSession.uiSG("clientid"), this.thisSession.uVFS.get_item(vfs_path).getProperty("url"), vfs_path);
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
    if (request.getProperty("command", "").equalsIgnoreCase("syncConflict") && ServerStatus.siIG("enterprise_level") > 0) {
      Properties item = this.thisSession.uVFS.get_item(String.valueOf(this.thisSession.SG("root_dir")) + request.getProperty("path"));
      String syncID = Common.parseSyncPart(item.getProperty("privs"), "name");
      if (syncID != null) {
        if (Common.System2.get("crushftp.dmz.queue") != null) {
          Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
          Properties action = new Properties();
          action.put("type", "GET:SYNC");
          action.put("id", Common.makeBoundary());
          action.put("username", this.thisSession.uiSG("user_name"));
          action.put("request", request);
          action.put("syncID", syncID.toUpperCase());
          action.put("clientid", this.thisSession.uiSG("clientid"));
          action.put("need_response", "true");
          queue.addElement(action);
          action = UserTools.waitResponse(action, 30);
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
    if (request.getProperty("command", "").equalsIgnoreCase("purgeSync") && ServerStatus.siIG("enterprise_level") > 0) {
      Properties item = this.thisSession.uVFS.get_item(String.valueOf(this.thisSession.SG("root_dir")) + request.getProperty("path"));
      String syncID = Common.parseSyncPart(item.getProperty("privs"), "name");
      if (syncID != null) {
        request.put("syncID", syncID.toUpperCase());
        if (Common.System2.get("crushftp.dmz.queue") != null) {
          Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
          Properties action = new Properties();
          action.put("type", "GET:SYNC");
          action.put("id", Common.makeBoundary());
          action.put("username", this.thisSession.uiSG("user_name"));
          action.put("request", request);
          action.put("syncID", syncID.toUpperCase());
          action.put("root_dir", this.thisSession.SG("root_dir"));
          action.put("clientid", this.thisSession.uiSG("clientid"));
          action.put("need_response", "true");
          queue.addElement(action);
          action = UserTools.waitResponse(action, 30);
        } else {
          AdminControls.purgeSync(request, this.thisSession.uVFS, this.thisSession.SG("root_dir"));
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
    if (request.getProperty("command", "").equalsIgnoreCase("downloadCrushFTPDrive") && ServerStatus.siIG("enterprise_level") > 0) {
      urlDecodePost(request);
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
      line = Common.replace_str(line, "%user_name%", this.thisSession.uiSG("user_name"));
      line = Common.url_encode(line);
      xml = String.valueOf(xml.substring(0, xml.indexOf("<argument>") + "<argument>".length())) + line + xml.substring(xml.indexOf("</argument>"));
      xml = Common.replace_str(xml, "%base_url%", this.thisSessionHTTP.getBaseUrl(this.thisSessionHTTP.hostString));
      xml = Common.replace_str(xml, "%appname%", appname);
      xml = Common.replace_str(xml, "%user_name%", this.thisSession.uiSG("user_name"));
      write_command_http("HTTP/1.1 200 OK");
      write_command_http("Cache-Control: no-store");
      write_command_http("Content-Type: application/x-java-jnlp-file");
      this.thisSessionHTTP.write_standard_headers();
      byte[] utf8 = xml.getBytes("UTF8");
      write_command_http("Content-Length: " + utf8.length);
      write_command_http("Content-Disposition: attachment; filename=\"" + appname + ".jnlp\"");
      write_command_http("X-UA-Compatible: chrome=1");
      write_command_http("");
      this.thisSessionHTTP.original_os.write(utf8);
      this.thisSessionHTTP.original_os.flush();
      return true;
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("downloadSyncAgent") && ServerStatus.siIG("enterprise_level") > 0) {
      urlDecodePost(request);
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
        pass = ServerStatus.thisObj.common_code.encode_pass(pass, "DES"); 
      String xml = new String(baos.toByteArray(), "UTF8");
      String line = xml.substring(xml.indexOf("<argument>") + "<argument>".length(), xml.indexOf("</argument>"));
      line = Common.replace_str(line, "%base_url%", this.thisSessionHTTP.getBaseUrl(this.thisSessionHTTP.hostString));
      line = Common.replace_str(line, "%appname%", appname);
      line = Common.replace_str(line, "%user_name%", this.thisSession.uiSG("user_name"));
      line = Common.replace_str(line, "%user_pass%", pass);
      line = Common.replace_str(line, "%admin_pass%", ServerStatus.thisObj.common_code.encode_pass(request.getProperty("admin_pass"), "DES"));
      line = Common.url_encode(line);
      xml = String.valueOf(xml.substring(0, xml.indexOf("<argument>") + "<argument>".length())) + line + xml.substring(xml.indexOf("</argument>"));
      xml = Common.replace_str(xml, "%base_url%", this.thisSessionHTTP.getBaseUrl(this.thisSessionHTTP.hostString));
      xml = Common.replace_str(xml, "%appname%", appname);
      xml = Common.replace_str(xml, "%user_name%", this.thisSession.uiSG("user_name"));
      xml = Common.replace_str(xml, "%user_pass%", pass);
      xml = Common.replace_str(xml, "%admin_pass%", ServerStatus.thisObj.common_code.encode_pass(request.getProperty("admin_pass"), "DES"));
      write_command_http("HTTP/1.1 200 OK");
      write_command_http("Cache-Control: no-store");
      write_command_http("Content-Type: application/x-java-jnlp-file");
      this.thisSessionHTTP.write_standard_headers();
      byte[] utf8 = xml.getBytes("UTF8");
      write_command_http("Content-Length: " + utf8.length);
      write_command_http("Content-Disposition: attachment; filename=\"" + appname + ".jnlp\"");
      write_command_http("X-UA-Compatible: chrome=1");
      write_command_http("");
      this.thisSessionHTTP.original_os.write(utf8);
      this.thisSessionHTTP.original_os.flush();
      return true;
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("getCrushSyncPrefs") && ServerStatus.siIG("enterprise_level") > 0) {
      Vector v = new Vector();
      request.put("user_name", this.thisSession.uiSG("user_name"));
      request.put("user_ip", this.thisSession.uiSG("user_ip"));
      request.put("clientid", this.thisSession.uiSG("clientid"));
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
    if (request.getProperty("command", "").equalsIgnoreCase("syncCommandResult") && ServerStatus.siIG("enterprise_level") > 0) {
      request.put("user_name", this.thisSession.uiSG("user_name"));
      request.put("user_ip", this.thisSession.uiSG("user_ip"));
      request.put("clientid", this.thisSession.uiSG("clientid"));
      SyncTools.sendSyncResult(request);
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      response = String.valueOf(response) + "<result><response_status>success</response_status></result>";
      return writeResponse(response);
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("getSyncAgents") && ServerStatus.siIG("enterprise_level") > 0) {
      if (last_activity_time != null)
        SharedSession.find("crushftp.usernames.activity").put(this.thisSession.getId(), last_activity_time); 
      urlDecodePost(request);
      Vector v = new Vector();
      request.put("user_name", this.thisSession.uiSG("user_name"));
      request.put("user_ip", this.thisSession.uiSG("user_ip"));
      request.put("clientid", this.thisSession.uiSG("clientid"));
      v = (Vector)SyncTools.getSyncAgents(request);
      return writeResponse(AdminControls.buildXML(v, "agents", "Success"));
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("sendSyncCommand") && ServerStatus.siIG("enterprise_level") > 0) {
      if (last_activity_time != null)
        SharedSession.find("crushftp.usernames.activity").put(this.thisSession.getId(), last_activity_time); 
      urlDecodePost(request);
      Object o = new Properties();
      request.put("user_name", this.thisSession.uiSG("user_name"));
      request.put("user_ip", this.thisSession.uiSG("user_ip"));
      request.put("clientid", this.thisSession.uiSG("clientid"));
      o = SyncTools.sendSyncCommand(request);
      return writeResponse(AdminControls.buildXML(o, "response", "Success"));
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("getMd5s")) {
      Properties item = this.thisSession.uVFS.get_item(String.valueOf(this.thisSession.SG("root_dir")) + new String(Base64.decode(request.getProperty("path"))));
      Vector md5s = new Vector();
      Properties request2 = request;
      null.multiThreadMd5 md5Worker = new null.multiThreadMd5(this, item, md5s, request2);
      if (item != null)
        Worker.startWorker(md5Worker); 
      write_command_http("HTTP/1.1 200 OK");
      write_command_http("Cache-Control: no-store");
      write_command_http("Content-Type: application/binary");
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
    if (request.getProperty("command", "").equalsIgnoreCase("messageForm") && !request.containsKey("registration_username") && !request.containsKey("meta_registration_username")) {
      Enumeration keys = request.keys();
      Properties metaInfo = new Properties();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement().toString();
        if (key.toUpperCase().startsWith("META_")) {
          String val = request.getProperty(key);
          key = key.substring("META_".length());
          metaInfo.put(key, val);
        } 
      } 
      ServerStatus.thisObj.statTools.insertMetaInfo(this.thisSession.uiSG("SESSION_RID"), metaInfo, "0");
      this.thisSession.add_log("[" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] DATA: *messageForm confirmed:" + metaInfo + "*", "HTTP");
      Properties fileItem = new Properties();
      fileItem.put("url", "ftp://127.0.0.1:56789/");
      fileItem.put("the_file_path", "/");
      fileItem.put("the_file_size", "1");
      fileItem.put("event_name", "welcome");
      fileItem.put("the_file_name", "welcome");
      fileItem.putAll(metaInfo);
      this.thisSession.do_event5("WELCOME", fileItem);
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n<commandResult><response>Success</response></commandResult>";
      return writeResponse(response);
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("selfRegistration") || request.containsKey("registration_username") || request.containsKey("meta_registration_username")) {
      urlDecodePost(request);
      Enumeration keys = request.keys();
      Properties metaInfo = new Properties();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement().toString();
        if (key.toUpperCase().startsWith("META_")) {
          String val = request.getProperty(key);
          key = key.substring("META_".length());
          metaInfo.put(key, val);
        } 
      } 
      String response = "Success";
      Properties newUser = new Properties();
      String username = metaInfo.getProperty("registration_username");
      Properties customForm = null;
      Vector customForms = (Vector)ServerStatus.server_settings.get("CustomForms");
      String pendingSelfRegistration = "pendingSelfRegistration";
      if (customForms != null) {
        for (int x = 0; x < customForms.size(); x++) {
          Properties p = customForms.elementAt(x);
          if (p.getProperty("name", "").equals(metaInfo.getProperty("form_name")) && this.thisSession.SG("messageForm").indexOf(metaInfo.getProperty("form_name")) >= 0) {
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
              } else if (p.getProperty("name").trim().equals("pendingSelfRegistration")) {
                pendingSelfRegistration = val.trim();
              } 
            } 
          } 
        } 
      } 
      newUser.put("root_dir", "/");
      newUser.put("user_name", username);
      newUser.put("max_logins", "-1");
      String originalPass = newUser.getProperty("password");
      newUser.put("password", ServerStatus.thisObj.common_code.encode_pass(newUser.getProperty("password"), ServerStatus.SG("password_encryption")));
      if (!Common.checkPasswordRequirements(originalPass, "").equals("")) {
        response = "Failure:" + Common.checkPasswordRequirements(originalPass, "");
        this.thisSession.add_log_formatted("Attempt to register a username using a weak password:" + username + ":" + response, "POST");
      } else if (UserTools.ut.getUser(this.thisSession.server_item.getProperty("linkedServer"), username, false) == null || UserTools.ut.getUser(this.thisSession.server_item.getProperty("linkedServer"), username, false).getProperty("username").equals("template")) {
        Properties groups = UserTools.getGroups(this.thisSession.server_item.getProperty("linkedServer"));
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
          UserTools.writeInheritance(this.thisSession.server_item.getProperty("linkedServer"), inheritance);
        } else {
          pendingSelfRegistration = "pendingSelfRegistration";
          groupUsers = (Vector)groups.get(pendingSelfRegistration);
          if (groupUsers == null)
            groupUsers = new Vector(); 
          groups.put(pendingSelfRegistration, groupUsers);
          if (groupUsers.indexOf(username) < 0)
            groupUsers.addElement(username); 
        } 
        UserTools.writeGroups(this.thisSession.server_item.getProperty("linkedServer"), groups);
        UserTools.writeUser(this.thisSession.server_item.getProperty("linkedServer"), username, newUser);
        ServerStatus.thisObj.statTools.insertMetaInfo(this.thisSession.uiSG("SESSION_RID"), metaInfo, "0");
        this.thisSession.add_log("[" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] DATA: *messageForm confirmed:" + metaInfo + "*", "HTTP");
        Properties fileItem = new Properties();
        fileItem.put("url", "ftp://127.0.0.1:56789/");
        fileItem.put("the_file_path", "/");
        fileItem.put("the_file_size", "1");
        fileItem.put("event_name", "registration");
        fileItem.put("the_file_name", "registration");
        fileItem.putAll(metaInfo);
        fileItem.put("metaInfo", metaInfo);
        this.thisSession.uiVG("lastUploadStats").addElement(fileItem);
        this.thisSession.do_event5("WELCOME", fileItem);
      } else {
        this.thisSession.add_log_formatted("Attempt to register an existing username:" + username, "POST");
        response = "Failure";
      } 
      response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n<commandResult><response>" + response + "</response></commandResult>";
      return writeResponse(response);
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("setServerItem") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.buildXML(null, "response", AdminControls.setServerItem(request))); 
    if (request.getProperty("command", "").equalsIgnoreCase("getUser") && (this.thisSession.SG("site").indexOf("(CONNECT)") >= 0 || this.thisSession.SG("site").indexOf("(USER_ADMIN)") >= 0))
      return writeResponse(AdminControls.buildXML(AdminControls.getUser(request, (this.thisSession.SG("site").indexOf("(CONNECT)") < 0), this.thisSession, session), "user_items", "OK"), true, 200, false, false); 
    if (request.getProperty("command", "").equalsIgnoreCase("setUserItem") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.buildXML(null, "response", AdminControls.setUserItem(request, this.thisSession))); 
    if (request.getProperty("command", "").equalsIgnoreCase("setUserItem") && this.thisSession.SG("site").indexOf("(USER_ADMIN)") >= 0) {
      if (this.thisSession.SG("site").indexOf("(CONNECT)") < 0)
        request.put("serverGroup", this.thisSession.server_item.getProperty("linkedServer")); 
      String status = "OK";
      try {
        try {
          Properties info = (Properties)session.get("user_admin_info");
          Vector list = (Vector)info.get("list");
          boolean writeGroupsInheritance = false;
          if (request.getProperty("xmlItem", "").equals("user")) {
            String username = Common.url_decode(request.getProperty("username", "").replace('+', ' '));
            Properties groups = UserTools.getGroups(request.getProperty("serverGroup"));
            if (groups == null)
              groups = new Properties(); 
            String groupName = this.thisSession.SG("admin_group_name");
            if (groupName.equals("") || groupName.equals("admin_group_name"))
              groupName = this.thisSession.uiSG("user_name"); 
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
              String[] usernames = request.getProperty("usernames").replace('+', ' ').split(";");
              for (int x = 0; x < usernames.length; x++) {
                if (!usernames[x].trim().equals("") && (
                  !usernames[x].trim().equalsIgnoreCase("default") || info.getProperty("default_edittable", "false").equals("true"))) {
                  if (list.indexOf(usernames[x].trim()) < 0)
                    throw new Exception("Username " + usernames[x].trim() + " not found."); 
                  UserTools.deleteUser(request.getProperty("serverGroup"), usernames[x].trim());
                  group.remove(usernames[x].trim());
                  pendingSelfRegistration.remove(usernames[x].trim());
                  inheritance.remove(usernames[x].trim());
                } 
              } 
              UserTools.writeGroups(request.getProperty("serverGroup"), groups);
              UserTools.writeInheritance(request.getProperty("serverGroup"), inheritance);
            } else {
              Properties new_user = (Properties)Common.readXMLObject(new ByteArrayInputStream(Common.url_decode(request.getProperty("user").replace('+', ' ')).getBytes("UTF8")));
              new_user.put("userVersion", "6");
              Vector linked_vfs = (Vector)new_user.get("linked_vfs");
              for (int x = 0; linked_vfs != null && x < linked_vfs.size(); x++) {
                String linked_user = linked_vfs.elementAt(x).toString().trim();
                if (list.indexOf(linked_user) < 0)
                  throw new Exception("Linked_VFS username " + linked_user + " not found."); 
              } 
              String pass = new_user.getProperty("password", "");
              if (!pass.startsWith("SHA:") && !pass.startsWith("SHA512:") && !pass.startsWith("MD5:") && !pass.startsWith("CRYPT3:") && !pass.startsWith("BCRYPT:") && !pass.startsWith("MD5CRYPT:")) {
                pass = ServerStatus.thisObj.common_code.encode_pass(pass, ServerStatus.SG("password_encryption"));
                new_user.put("password", pass);
              } 
              VFS tempVFS = AdminControls.processVFSSubmission(request, username, (this.thisSession.SG("site").indexOf("(CONNECT)") < 0), this.thisSession);
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
                if (list.indexOf(username) < 0)
                  throw new Exception("Username " + username + " not found."); 
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
              } else {
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
                    ok = UserTools.testLimitedAdminAccess(vfs_item.get("vItems"), groupName, request.getProperty("serverGroup"));
                    if (!ok)
                      Log.log("HTTP_SERVER", 0, new Date() + ":User " + this.thisSession.uiSG("user_name") + " Violated Security Constraint for a USER_ADMIN:" + groupName); 
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
                Log.log("HTTP_SERVER", 0, new Date() + ":User " + this.thisSession.uiSG("user_name") + " Violated Security Constraint for a USER_ADMIN:" + groupName);
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
    if (request.getProperty("command", "").equals("refreshUser") && (this.thisSession.SG("site").indexOf("(CONNECT)") >= 0 || this.thisSession.SG("site").indexOf("(USER_ADMIN)") >= 0)) {
      UserTools.ut.forceMemoryReload(request.getProperty("username"));
      return writeResponse("<commandResult><response>Success</response></commandResult>");
    } 
    if (request.getProperty("command", "").equals("getUserXMLListing") && (this.thisSession.SG("site").indexOf("(CONNECT)") >= 0 || this.thisSession.SG("site").indexOf("(USER_ADMIN)") >= 0)) {
      Properties listingProp = AdminControls.getUserXMLListing(request, (this.thisSession.SG("site").indexOf("(CONNECT)") < 0), this.thisSession, session);
      String altList = "";
      if (request.getProperty("format", "").equalsIgnoreCase("JSON")) {
        altList = getJsonList(listingProp);
      } else if (request.getProperty("format", "").equalsIgnoreCase("STAT")) {
        altList = getStatList(listingProp);
      } 
      String response = "";
      try {
        response = ServerStatus.thisObj.common_code.getXMLString(listingProp, "listingInfo", null);
      } catch (Exception e) {
        Log.log("HTTP_SERVER", 1, e);
      } 
      if (!altList.equals(""))
        response = String.valueOf(response.substring(0, response.indexOf("</privs>") + "</privs>".length())) + altList + response.substring(response.indexOf("</privs>") + "</privs>".length()); 
      return writeResponse(response);
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("getUserList") && (this.thisSession.SG("site").indexOf("(CONNECT)") >= 0 || this.thisSession.SG("site").indexOf("(USER_ADMIN)") >= 0))
      return writeResponse(AdminControls.buildXML(AdminControls.getUserList(request, (this.thisSession.SG("site").indexOf("(CONNECT)") < 0), this.thisSession, session), "user_list", "OK")); 
    if (request.getProperty("command", "").equalsIgnoreCase("getUserXML") && (this.thisSession.SG("site").indexOf("(CONNECT)") >= 0 || this.thisSession.SG("site").indexOf("(USER_ADMIN)") >= 0))
      return writeResponse(AdminControls.buildXML(AdminControls.getUserXML(request, (this.thisSession.SG("site").indexOf("(CONNECT)") < 0), session), "result_item", "OK")); 
    if (request.getProperty("command", "").equals("getAdminXMLListing")) {
      Properties listingProp = new Properties();
      if (!request.getProperty("get_from_agentid", "").equals("")) {
        Vector items = new Vector();
        String str1 = Common.url_decode(request.getProperty("path", "").replace('+', ' '));
        if (str1.startsWith("///") && !str1.startsWith("////"))
          str1 = "/" + str1; 
        if (!str1.startsWith("/"))
          str1 = "/" + str1; 
        if (!str1.endsWith("/"))
          str1 = String.valueOf(str1) + "/"; 
        str1 = Common.dots(str1);
        request.put("path", str1);
        request.put("user_name", this.thisSession.uiSG("user_name"));
        request.put("user_ip", this.thisSession.uiSG("user_ip"));
        request.put("clientid", this.thisSession.uiSG("clientid"));
        items = (Vector)SyncTools.getSyncXMLList(request);
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
          altList = getJsonList(listingProp);
        } else if (listingProp != null && request.getProperty("format", "").equalsIgnoreCase("STAT")) {
          altList = getStatList(listingProp);
        } 
        String str2 = "";
        try {
          str2 = ServerStatus.thisObj.common_code.getXMLString(listingProp, "listingInfo", null);
        } catch (Exception e) {
          Log.log("HTTP_SERVER", 1, e);
        } 
        if (!altList.equals(""))
          str2 = String.valueOf(str2.substring(0, str2.indexOf("</privs>") + "</privs>".length())) + altList + str2.substring(str2.indexOf("</privs>") + "</privs>".length()); 
        return writeResponse(str2);
      } 
      if (this.thisSession.SG("site").indexOf("(CONNECT)") >= 0) {
        listingProp = AdminControls.getAdminXMLListing(request, this.thisSession);
        String altList = "";
        if (listingProp != null && request.getProperty("format", "").equalsIgnoreCase("JSON")) {
          altList = getJsonList(listingProp);
        } else if (listingProp != null && request.getProperty("format", "").equalsIgnoreCase("STAT")) {
          altList = getStatList(listingProp);
        } 
        String str1 = "";
        try {
          str1 = ServerStatus.thisObj.common_code.getXMLString(listingProp, "listingInfo", null);
        } catch (Exception e) {
          Log.log("HTTP_SERVER", 1, e);
        } 
        if (!altList.equals(""))
          str1 = String.valueOf(str1.substring(0, str1.indexOf("</privs>") + "</privs>".length())) + altList + str1.substring(str1.indexOf("</privs>") + "</privs>".length()); 
        return writeResponse(str1);
      } 
      String groupName = this.thisSession.SG("admin_group_name");
      if (groupName.equals("") || groupName.equals("admin_group_name"))
        groupName = this.thisSession.uiSG("user_name"); 
      String path = Common.url_decode(request.getProperty("path", "").replace('+', ' '));
      if (path.startsWith("///") && !path.startsWith("////"))
        path = "/" + path; 
      if (!path.startsWith("/"))
        path = "/" + path; 
      if (!path.endsWith("/"))
        path = String.valueOf(path) + "/"; 
      path = Common.dots(path);
      request.put("command", "getXMLListing");
      String response = getXmlListingResponse(groupName, request, path, true, UserTools.ut.getVFS(this.thisSession.uiSG("listen_ip_port"), groupName));
      return writeResponse(response, request.getProperty("format", "").equalsIgnoreCase("JSONOBJ"));
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("adminAction") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.buildXML(AdminControls.adminAction(urlDecodePost(request)), "result_item", "OK")); 
    if (request.getProperty("command", "").equalsIgnoreCase("updateNow") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.updateNow(request)); 
    if (request.getProperty("command", "").equalsIgnoreCase("updateWebNow") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.updateWebNow(request)); 
    if (request.getProperty("command", "").equalsIgnoreCase("updateNowProgress") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.updateNowProgress(request)); 
    if (request.getProperty("command", "").equalsIgnoreCase("cancelUpdateProgress") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.cancelUpdateProgress(request)); 
    if (request.getProperty("command", "").equalsIgnoreCase("dumpStack") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(Common.dumpStack("")); 
    if (request.getProperty("command", "").equalsIgnoreCase("pgpGenerateKeyPair") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.pgpGenerateKeyPair(request)); 
    if (request.getProperty("command", "").equalsIgnoreCase("runReport") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0) {
      String xml = AdminControls.runReport(request);
      write_command_http("HTTP/1.1 200 OK");
      write_command_http("Cache-Control: no-store");
      write_command_http("Content-Type: text/" + (request.getProperty("export", "false").equals("true") ? "plain" : "html") + ";charset=utf-8");
      this.thisSessionHTTP.write_standard_headers();
      byte[] utf8 = xml.getBytes("UTF8");
      write_command_http("Content-Length: " + utf8.length);
      if (request.getProperty("saveReport", "").equalsIgnoreCase("true"))
        write_command_http("Content-Disposition: attachment; filename=\"" + request.getProperty("reportName") + (request.getProperty("export", "false").equals("true") ? ".csv" : ".html") + "\""); 
      write_command_http("X-UA-Compatible: chrome=1");
      write_command_http("");
      this.thisSessionHTTP.original_os.write(utf8);
      this.thisSessionHTTP.original_os.flush();
      return true;
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("testReportSchedule") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.testReportSchedule(request)); 
    if (request.getProperty("command", "").equalsIgnoreCase("testJobSchedule") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.testJobSchedule(request)); 
    if (request.getProperty("command", "").equalsIgnoreCase("testSMTP") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.testSMTP(urlDecodePost(request))); 
    if (request.getProperty("command", "").equalsIgnoreCase("importUsers") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.importUsers(urlDecodePost(request))); 
    if (request.getProperty("command", "").equalsIgnoreCase("sendPassEmail") && (this.thisSession.SG("site").indexOf("(CONNECT)") >= 0 || this.thisSession.SG("site").indexOf("(USER_ADMIN)") >= 0)) {
      urlDecodePost(request);
      if (this.thisSession.SG("site").indexOf("(CONNECT)") < 0)
        request.put("serverGroup", this.thisSession.server_item.getProperty("linkedServer")); 
      String username = request.getProperty("user_name", "");
      Properties info = (Properties)session.get("user_admin_info");
      if (this.thisSession.SG("site").indexOf("(CONNECT)") < 0) {
        Vector list = (Vector)info.get("list");
        if (list.indexOf(username) < 0)
          throw new Exception("Username " + username + " not found."); 
      } 
      return writeResponse(AdminControls.sendPassEmail(request));
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("sendEventEmail") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.sendEventEmail(urlDecodePost(request))); 
    if (request.getProperty("command", "").equalsIgnoreCase("getTempAccounts") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.getTempAccounts(urlDecodePost(request))); 
    if (request.getProperty("command", "").equalsIgnoreCase("addTempAccount") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.addTempAccount(request)); 
    if (request.getProperty("command", "").equalsIgnoreCase("removeTempAccount") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.removeTempAccount(urlDecodePost(request))); 
    if (request.getProperty("command", "").equalsIgnoreCase("getTempAccountFiles") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.getTempAccountFiles(urlDecodePost(request))); 
    if (request.getProperty("command", "").equalsIgnoreCase("removeTempAccountFile") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.removeTempAccountFile(urlDecodePost(request))); 
    if (request.getProperty("command", "").equalsIgnoreCase("addTempAccountFile") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.addTempAccountFile(urlDecodePost(request))); 
    if (request.getProperty("command", "").equalsIgnoreCase("migrateUsersVFS") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.migrateUsersVFS(urlDecodePost(request))); 
    if (request.getProperty("command", "").equalsIgnoreCase("convertUsers") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.convertUsers(urlDecodePost(request))); 
    if (request.getProperty("command", "").equalsIgnoreCase("generateSSL") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.generateSSL(urlDecodePost(request))); 
    if (request.getProperty("command", "").equalsIgnoreCase("testKeystore") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.testKeystore(urlDecodePost(request))); 
    if (request.getProperty("command", "").equalsIgnoreCase("generateFileKey") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.generateFileKey(urlDecodePost(request))); 
    if (request.getProperty("command", "").equalsIgnoreCase("testDB") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.testDB(urlDecodePost(request))); 
    if (request.getProperty("command", "").equalsIgnoreCase("pluginMethodCall") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.pluginMethodCall(urlDecodePost(request))); 
    if (request.getProperty("command", "").equalsIgnoreCase("convertXMLSQLUsers") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.convertXMLSQLUsers(urlDecodePost(request))); 
    if (request.getProperty("command", "").equalsIgnoreCase("registerCrushFTP") && this.thisSession.SG("site").indexOf("(CONNECT)") >= 0)
      return writeResponse(AdminControls.registerCrushFTP(urlDecodePost(request))); 
    if (request.getProperty("command", "").equalsIgnoreCase("upload") || request.getProperty("the_action", "").equalsIgnoreCase("STOR")) {
      boolean result = false;
      int code = 200;
      String response = "<commandResult><response>";
      Properties activeUpload = (Properties)Common.System2.get("crushftp.activeUpload.info" + this.thisSession.getId());
      if (activeUpload.getProperty("fileupload", "").indexOf("ERROR:") >= 0) {
        response = String.valueOf(response) + activeUpload.getProperty("fileupload", "");
        code = 500;
      } else {
        response = String.valueOf(response) + "Success";
      } 
      this.thisSessionHTTP.done = true;
      response = String.valueOf(response) + "</response></commandResult>";
      result = writeResponse(response, true, code, true, false);
      if (this.thisSessionHTTP.chunked)
        Thread.sleep(1000L); 
      return result;
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("upload_0_byte")) {
      String error = "";
      String the_dir = Common.url_decode(request.getProperty("path", ""));
      if (the_dir.equals("/"))
        the_dir = this.thisSession.SG("root_dir"); 
      if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(this.thisSession.SG("root_dir").toUpperCase()))
        the_dir = String.valueOf(this.thisSession.SG("root_dir")) + the_dir.substring(1); 
      if (this.thisSession.check_access_privs(the_dir, "STOR")) {
        Properties item = this.thisSession.uVFS.get_item_parent(the_dir);
        GenericClient c = this.thisSession.uVFS.getClient(item);
        try {
          c.upload((new VRL(item.getProperty("url"))).getPath(), 0L, true, true).close();
        } catch (Exception e) {
          error = String.valueOf(error) + e;
        } 
        c.close();
        this.thisSession.uVFS.releaseClient(c);
      } 
      String response = "<commandResult><response>OK:" + error + "</response></commandResult>";
      return writeResponse(response);
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("blockUploads")) {
      session.put("blockUploads", "true");
      String response = "<commandResult><response>OK</response></commandResult>";
      return writeResponse(response);
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("unblockUploads")) {
      session.remove("blockUploads");
      String response = "<commandResult><response>OK</response></commandResult>";
      return writeResponse(response);
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("getLastUploadError")) {
      String response = "<commandResult><response>";
      Properties activeUpload = (Properties)Common.System2.get("crushftp.activeUpload.info" + this.thisSession.getId());
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
    if (request.getProperty("command", "").equalsIgnoreCase("getPreview")) {
      if (Common.System2.get("crushftp.dmz.queue") != null) {
        String the_dir = request.getProperty("path");
        if (!the_dir.startsWith(this.thisSession.SG("root_dir")))
          the_dir = String.valueOf(this.thisSession.SG("root_dir")) + the_dir.substring(1); 
        Properties item = this.thisSession.uVFS.get_item(the_dir);
        Properties config = new Properties();
        config.put("protocol", "HTTP");
        URLConnection urlc = null;
        int loops = 0;
        while (loops++ < 100) {
          urlc = URLConnection.openConnection(new VRL(Common.getBaseUrl(item.getProperty("url"))), config);
          urlc.setRequestMethod("POST");
          GenericClient c = this.thisSession.uVFS.getClient(item);
          urlc.setRequestProperty("Cookie", "CrushAuth=" + c.getConfig("crushAuth") + ";");
          urlc.setUseCaches(false);
          urlc.setDoOutput(true);
          byte[] arrayOfByte = ("command=getPreview&size=" + request.getProperty("size", "1") + "&frame=" + request.getProperty("frame", "1") + "&object_type=" + request.getProperty("object_type", "image") + "&download=" + request.getProperty("download", "false") + "&path=" + Common.url_encode(request.getProperty("path"))).getBytes("UTF8");
          urlc.setLength(arrayOfByte.length);
          OutputStream pout = urlc.getOutputStream();
          pout.write(arrayOfByte);
          pout.flush();
          if (urlc.getResponseCode() == 302) {
            c.setConfig("error", "Logged out.");
            urlc.disconnect();
            this.thisSession.uVFS.releaseClient(c);
            Thread.sleep(100L);
            continue;
          } 
          this.thisSession.uVFS.releaseClient(c);
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
        urlDecodePost(request);
        Vector paths_raw = new Vector();
        Vector paths_updated = new Vector();
        String[] path_str = request.getProperty("path").split(";");
        String path = "";
        for (int x = 0; x < path_str.length; x++) {
          path = path_str[x];
          if (!path.trim().equals("")) {
            paths_raw.addElement(path);
            this.thisSession.setupCurrentDir(path);
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
              Log.log("HTTP_SERVER", 2, "getPreview2:" + this.thisSession.uiSG("current_dir"));
              Properties item = this.thisSession.uVFS.get_item(this.thisSession.uiSG("current_dir"));
              path = "/WebInterface/images" + SearchHandler.getPreviewPath(item, "1", 1);
              path = String.valueOf(Common.all_but_last(Common.all_but_last(path))) + "info.xml";
              Log.log("HTTP_SERVER", 2, "getPreview3:" + path);
            } else {
              Log.log("HTTP_SERVER", 2, "getPreview2:" + this.thisSession.uiSG("current_dir"));
              Properties item = this.thisSession.uVFS.get_item(this.thisSession.uiSG("current_dir"));
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
          ServerSessionHTTPWI5_2.serveFile(this.thisSessionHTTP, this.thisSessionHTTP.headers, this.thisSessionHTTP.original_os, false, downloadFilename);
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
            this.thisSession.setupCurrentDir(path);
            Properties item = this.thisSession.uVFS.get_item(this.thisSession.uiSG("current_dir"));
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
    if (request.getProperty("command", "").equalsIgnoreCase("setPreview")) {
      urlDecodePost(request);
      String error_message = "No preview converters found.";
      String path = request.getProperty("path");
      this.thisSession.setupCurrentDir(path);
      Properties item = this.thisSession.uVFS.get_item(this.thisSession.uiSG("current_dir"));
      String srcFile = Common.dots((new VRL(item.getProperty("url"))).getPath());
      if (!this.thisSession.check_access_privs(path, "STOR"))
        error_message = "You need upload permissions to edit exif tags on a file:" + request.getProperty("path") + "\r\n"; 
      if (request.getProperty("object_type", "image").equals("exif"))
        for (int xx = 0; xx < ServerStatus.thisObj.previewWorkers.size(); xx++) {
          PreviewWorker preview = ServerStatus.thisObj.previewWorkers.elementAt(xx);
          if (preview.prefs.getProperty("preview_enabled", "false").equalsIgnoreCase("true") && preview.checkExtension(Common.last(path), new File(srcFile))) {
            preview.setExifInfo(srcFile, PreviewWorker.getDestPath(String.valueOf(srcFile) + "/p1/"), request.getProperty("exif_key"), request.getProperty("exif_val"));
            error_message = "Success";
            break;
          } 
        }  
      String response = "<commandResult><response>" + error_message + "</response></commandResult>";
      return writeResponse(response);
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("siteCommand")) {
      urlDecodePost(request);
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      this.thisSession.uiPUT("the_command", "SITE");
      String the_command_data = request.getProperty("siteCommand", "");
      this.thisSession.uiPUT("the_command_data", the_command_data);
      String responseText = null;
      GenericClient c = this.thisSession.uVFS.getClient(this.thisSession.uVFS.get_item(this.thisSession.SG("root_dir")));
      try {
        VRL vrl = new VRL(this.thisSession.uVFS.get_item(this.thisSession.SG("root_dir")).getProperty("url"));
        if (the_command_data.startsWith("PGP_HEADER_SIZE")) {
          the_command_data = the_command_data.substring(the_command_data.indexOf(" ") + 1);
          String size = the_command_data.substring(0, the_command_data.indexOf(" ")).trim();
          the_command_data = the_command_data.substring(the_command_data.indexOf(" ") + 1);
          responseText = c.doCommand("SITE PGP_HEADER_SIZE " + size + " " + vrl.getPath() + Common.dots(the_command_data));
        } else if (ServerStatus.BG("site_ack")) {
          responseText = c.doCommand("SITE " + the_command_data);
        } 
        if (responseText == null) {
          Properties activeUpload = (Properties)Common.System2.get("crushftp.activeUpload.info" + this.thisSession.getId());
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
        this.thisSession.uVFS.reset();
      } finally {
        c = this.thisSession.uVFS.releaseClient(c);
      } 
      response = String.valueOf(response) + "<commandResult><response>" + responseText + "</response></commandResult>";
      return writeResponse(response);
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("sitePlugin") && this.thisSession.SG("site").toUpperCase().indexOf("(SITE_PLUGIN)") >= 0) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      this.thisSession.uiPUT("the_command", "SITE");
      String the_command_data = request.getProperty("siteCommand", "");
      this.thisSession.uiPUT("the_command_data", the_command_data);
      Properties fileItem = new Properties();
      fileItem.put("url", "ftp://127.0.0.1:56789/");
      fileItem.put("the_file_path", request.getProperty("the_file_path", "/"));
      fileItem.put("the_file_size", "1");
      fileItem.put("event_name", request.getProperty("event"));
      fileItem.put("the_file_name", request.getProperty("the_file_name", "none"));
      fileItem.putAll(request);
      this.thisSession.do_event5("SITE", fileItem);
      String responseText = fileItem.getProperty("execute_log", "No Result");
      response = String.valueOf(response) + "<commandResult><response>" + responseText + "</response></commandResult>";
      return writeResponse(response);
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("getUploadStatus")) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      Properties activeUpload = (Properties)Common.System2.get("crushftp.activeUpload.info" + this.thisSession.getId());
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
    if (request.getProperty("command", "").equalsIgnoreCase("lookup_form_field")) {
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
            if (entry.getProperty("lookup_type", "text_file").equals("text_file")) {
              File userText = new File(String.valueOf(System.getProperty("crushftp.prefs")) + "forms/" + Common.dots(String.valueOf(request.getProperty("form_element_name")) + "_" + this.thisSession.uiSG("user_name") + ".txt"));
              if (userText.exists()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(userText)));
                String data = "";
                while ((data = br.readLine()) != null) {
                  String key = data.trim();
                  String val = key;
                  Properties p = new Properties();
                  if (data.indexOf(":") >= 0) {
                    key = data.split(":")[0].trim();
                    val = data.split(":")[1].trim();
                  } 
                  p.put("key", key);
                  p.put("val", val);
                  if (key.toUpperCase().indexOf(q.toUpperCase()) >= 0)
                    search_entries.addElement(p); 
                } 
                br.close();
              } 
              Properties groups = UserTools.getGroups(this.thisSession.server_item.getProperty("linkedServer"));
              Enumeration keys = groups.keys();
              while (keys.hasMoreElements()) {
                String group_name = keys.nextElement().toString();
                Vector v = (Vector)groups.get(group_name);
                if (v.indexOf(this.thisSession.uiSG("user_name")) >= 0) {
                  File groupText = new File(String.valueOf(System.getProperty("crushftp.prefs")) + "forms/" + Common.dots(String.valueOf(request.getProperty("form_element_name")) + "_" + group_name + ".txt"));
                  if (groupText.exists()) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(groupText)));
                    String data = "";
                    while ((data = br.readLine()) != null) {
                      String key = data.trim();
                      String val = key;
                      Properties p = new Properties();
                      if (data.indexOf(":") >= 0) {
                        key = data.split(":")[0].trim();
                        val = data.split(":")[1].trim();
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
              if (globalText.exists() && search_entries.size() == 0) {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(globalText)));
                String data = "";
                while ((data = br.readLine()) != null) {
                  String key = data.trim();
                  String val = key;
                  Properties p = new Properties();
                  if (data.indexOf(":") >= 0) {
                    key = data.split(":")[0].trim();
                    val = data.split(":")[1].trim();
                  } 
                  p.put("key", key);
                  p.put("val", val);
                  if (key.toUpperCase().indexOf(q.toUpperCase()) >= 0)
                    search_entries.addElement(p); 
                } 
                br.close();
              } 
            } else if (!entry.getProperty("lookup_type", "").equals("db_query")) {
              if (entry.getProperty("lookup_type", "").equals("plugin"))
                try {
                  Object parent = Common.getPlugin(request.getProperty("pluginName"), null, request.getProperty("pluginSubItem", ""));
                  if (parent == null && request.getProperty("pluginSubItem", "").equals(""))
                    parent = Common.getPlugin(request.getProperty("pluginName"), null, "false"); 
                  Method method = parent.getClass().getMethod(request.getProperty("method", "lookupList"), new Class[] { (new Properties()).getClass() });
                  Object o = method.invoke(parent, new Object[] { request });
                  response = ServerStatus.thisObj.common_code.getXMLString(o, "list", null);
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
    if (request.getProperty("command", "").equalsIgnoreCase("batchComplete")) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      response = String.valueOf(response) + "<commandResult><response>SUCCESS</response></commandResult>";
      this.thisSession.do_event5("BATCH_COMPLETE", null);
      return writeResponse(response);
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("decrypt")) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      String[] names = Common.url_decode(request.getProperty("names")).split("\r\n");
      String error_message = "";
      for (int x = 0; x < names.length; x++) {
        try {
          String the_dir = names[x];
          if (!the_dir.startsWith(this.thisSession.SG("root_dir")))
            the_dir = String.valueOf(this.thisSession.SG("root_dir")) + the_dir.substring(1); 
          this.thisSession.uiPUT("current_dir", the_dir);
          if (!this.thisSession.check_access_privs(this.thisSession.uiSG("current_dir"), "RETR") || !this.thisSession.check_access_privs(this.thisSession.uiSG("current_dir"), "STOR")) {
            error_message = String.valueOf(error_message) + "ERROR:You need download, and upload permissions to decrypt a file:" + the_dir + "\r\n";
          } else {
            this.thisSession.uiPUT("the_command", "DECRYPT");
            this.thisSession.uiPUT("the_command_data", this.thisSession.uiSG("current_dir"));
            Properties item = this.thisSession.uVFS.get_item(the_dir);
            VRL vrl = new VRL(item.getProperty("url"));
            GenericClient c1 = this.thisSession.uVFS.getClient(item);
            GenericClient c2 = this.thisSession.uVFS.getClient(item);
            try {
              InputStream in = c1.download(vrl.getPath(), 0L, -1L, true);
              if (!this.thisSession.user.getProperty("fileEncryptionKey", "").equals("") || !this.thisSession.user.getProperty("fileDecryptionKey", "").equals("")) {
                in = Common.getDecryptedStream(in, this.thisSession.user.getProperty("fileEncryptionKey", ""), this.thisSession.user.getProperty("fileDecryptionKey", ""), this.thisSession.user.getProperty("fileDecryptionKeyPass", ""));
              } else if (!ServerStatus.SG("fileEncryptionKey").equals("") || ServerStatus.BG("fileDecryption")) {
                in = Common.getDecryptedStream(in, ServerStatus.SG("fileEncryptionKey"), ServerStatus.SG("fileDecryptionKey"), ServerStatus.SG("fileDecryptionKeyPass"));
              } 
              OutputStream out = c2.upload(String.valueOf(vrl.getPath()) + ".decrypting", 0L, true, true);
              Common.copyStreams(in, out, true, true);
              c1.rename(String.valueOf(vrl.getPath()) + ".decrypting", vrl.getPath());
              error_message = String.valueOf(error_message) + the_dir + " decrypted.";
            } finally {
              c1 = this.thisSession.uVFS.releaseClient(c1);
              c2 = this.thisSession.uVFS.releaseClient(c2);
            } 
          } 
        } catch (Exception e) {
          Log.log("HTTP_SERVER", 0, e);
          error_message = String.valueOf(error_message) + "ERROR:" + e.getMessage() + "\r\n";
        } 
      } 
      response = String.valueOf(response) + "<commandResult><response>" + error_message + "</response></commandResult>";
      this.thisSession.uVFS.reset();
      return writeResponse(response.replace('%', ' '));
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("encrypt")) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      String[] names = Common.url_decode(request.getProperty("names")).split("\r\n");
      String error_message = "";
      for (int x = 0; x < names.length; x++) {
        String the_dir = names[x];
        if (!the_dir.startsWith(this.thisSession.SG("root_dir")))
          the_dir = String.valueOf(this.thisSession.SG("root_dir")) + the_dir.substring(1); 
        this.thisSession.uiPUT("current_dir", the_dir);
        if (!this.thisSession.check_access_privs(this.thisSession.uiSG("current_dir"), "RETR") || !this.thisSession.check_access_privs(this.thisSession.uiSG("current_dir"), "STOR")) {
          error_message = String.valueOf(error_message) + "ERROR:You need download, and upload permissions to encrypt a file:" + the_dir + "\r\n";
        } else {
          this.thisSession.uiPUT("the_command", "DECRYPT");
          this.thisSession.uiPUT("the_command_data", this.thisSession.uiSG("current_dir"));
          Properties item = this.thisSession.uVFS.get_item(the_dir);
          VRL vrl = new VRL(item.getProperty("url"));
          GenericClient c1 = this.thisSession.uVFS.getClient(item);
          GenericClient c2 = this.thisSession.uVFS.getClient(item);
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
              if (!this.thisSession.user.getProperty("filePublicEncryptionKey", "").equals("")) {
                out = Common.getEncryptedStream(out, this.thisSession.user.getProperty("filePublicEncryptionKey", ""), 0L);
              } else if (ServerStatus.BG("fileEncryption") && !ServerStatus.SG("filePublicEncryptionKey").equals("")) {
                out = Common.getEncryptedStream(out, ServerStatus.SG("filePublicEncryptionKey"), 0L);
              } else if (!this.thisSession.user.getProperty("fileEncryptionKey", "").equals("")) {
                out = Common.getEncryptedStream(out, this.thisSession.user.getProperty("fileEncryptionKey", ""), 0L);
              } else if (ServerStatus.BG("fileEncryption")) {
                out = Common.getEncryptedStream(out, ServerStatus.SG("fileEncryptionKey"), 0L);
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
            c1 = this.thisSession.uVFS.releaseClient(c1);
            c2 = this.thisSession.uVFS.releaseClient(c2);
          } 
        } 
      } 
      response = String.valueOf(response) + "<commandResult><response>" + error_message + "</response></commandResult>";
      this.thisSession.uVFS.reset();
      return writeResponse(response.replace('%', ' '));
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("unzip")) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      String[] names = Common.url_decode(request.getProperty("names")).split("\r\n");
      String error_message = "";
      for (int x = 0; x < names.length; x++) {
        String the_dir = names[x];
        if (the_dir.toUpperCase().endsWith(".ZIP")) {
          if (!the_dir.startsWith(this.thisSession.SG("root_dir")))
            the_dir = String.valueOf(this.thisSession.SG("root_dir")) + the_dir.substring(1); 
          this.thisSession.uiPUT("current_dir", the_dir);
          if (!this.thisSession.check_access_privs(this.thisSession.uiSG("current_dir"), "RETR") || !this.thisSession.check_access_privs(this.thisSession.uiSG("current_dir"), "STOR") || !this.thisSession.check_access_privs(Common.all_but_last(this.thisSession.uiSG("current_dir")), "MKD")) {
            error_message = String.valueOf(error_message) + "You need download, upload, and make directory permissions to unzip a file:" + the_dir + "\r\n";
          } else {
            this.thisSession.uiPUT("the_command", "UNZIP");
            this.thisSession.uiPUT("the_command_data", this.thisSession.uiSG("current_dir"));
            Properties item = this.thisSession.uVFS.get_item(the_dir);
            VRL vrl = new VRL(item.getProperty("url"));
            GenericClient c = this.thisSession.uVFS.getClient(item);
            try {
              Common.unzip(vrl.getPath(), c, this.thisSession, Common.all_but_last(the_dir));
            } finally {
              c = this.thisSession.uVFS.releaseClient(c);
            } 
          } 
        } 
      } 
      response = String.valueOf(response) + "<commandResult><response>" + error_message + "</response></commandResult>";
      this.thisSession.uVFS.reset();
      return writeResponse(response.replace('%', ' '));
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("zip")) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      String[] names = Common.url_decode(request.getProperty("names")).split("\r\n");
      String error_message = "";
      Vector zipFiles = new Vector();
      StringBuffer firstItemName = new StringBuffer();
      for (int x = 0; x < names.length; x++) {
        String str = names[x];
        if (!str.startsWith(this.thisSession.SG("root_dir")))
          str = String.valueOf(this.thisSession.SG("root_dir")) + str.substring(1); 
        this.thisSession.uiPUT("current_dir", str);
        if (!this.thisSession.check_access_privs(this.thisSession.uiSG("current_dir"), "RETR") || !this.thisSession.check_access_privs(this.thisSession.uiSG("current_dir"), "STOR")) {
          error_message = String.valueOf(error_message) + "You need download, upload permissions to zip a file:" + str + "\r\n";
        } else {
          this.thisSession.uiPUT("the_command", "ZIP");
          this.thisSession.uiPUT("the_command_data", this.thisSession.uiSG("current_dir"));
          Properties item = this.thisSession.uVFS.get_item(str);
          if (firstItemName.length() == 0)
            firstItemName.append(item.getProperty("name")); 
          this.thisSession.uVFS.getListing(zipFiles, str, 999, 50000, true);
        } 
      } 
      String the_dir = Common.url_decode(request.getProperty("path"));
      if (!the_dir.startsWith(this.thisSession.SG("root_dir")))
        the_dir = String.valueOf(this.thisSession.SG("root_dir")) + the_dir.substring(1); 
      if (!this.thisSession.check_access_privs(the_dir, "STOR")) {
        error_message = String.valueOf(error_message) + "You need upload permissions to zip a file:" + request.getProperty("path") + "\r\n";
      } else {
        error_message = String.valueOf(error_message) + "Started zipping...\r\n";
        Properties outputItem = this.thisSession.uVFS.get_item(the_dir);
        String root_dir = (new VRL(outputItem.getProperty("url"))).getPath();
        Worker.startWorker(new Runnable(this, firstItemName, root_dir, zipFiles) {
              final ServerSessionAJAX5_2 this$0;
              
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
      this.thisSession.uVFS.reset();
      return writeResponse(response.replace('%', ' '));
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("delete")) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      String[] names = Common.url_decode(request.getProperty("names")).split("\r\n");
      String error_message = "";
      for (int x = 0; x < names.length; x++) {
        String the_dir = names[x];
        if (the_dir.startsWith(this.thisSession.SG("root_dir")))
          the_dir = the_dir.substring(this.thisSession.SG("root_dir").length() - 1); 
        this.thisSession.uiPUT("current_dir", the_dir);
        this.thisSession.uiPUT("the_command", "DELE");
        this.thisSession.uiPUT("the_command_data", this.thisSession.uiSG("current_dir"));
        String lastMessage = "";
        for (int xx = 0; xx < 10; xx++) {
          lastMessage = this.thisSession.do_DELE(true, the_dir);
          if (lastMessage.equals("%DELE-not found%"))
            lastMessage = ""; 
          if (lastMessage.indexOf("%DELE-error%") < 0)
            break; 
          Thread.sleep(1000L);
        } 
        error_message = String.valueOf(error_message) + lastMessage;
      } 
      response = String.valueOf(response) + "<commandResult><response>" + LOC.G(error_message) + "</response></commandResult>";
      this.thisSession.uVFS.reset();
      return writeResponse(response.replace('%', ' '));
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("rename")) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      String the_dir = Common.url_decode(request.getProperty("path"));
      if (the_dir.startsWith(this.thisSession.SG("root_dir")))
        the_dir = the_dir.substring(this.thisSession.SG("root_dir").length() - 1); 
      this.thisSession.uiPUT("current_dir", the_dir);
      String item_name = Common.url_decode(request.getProperty("name1", ""));
      this.thisSession.uiPUT("the_command", "RNFR");
      this.thisSession.uiPUT("the_command_data", item_name);
      String error_message = this.thisSession.do_RNFR();
      if (error_message.equals("") || error_message.equals("%RNFR-bad%")) {
        String fromPath = SearchHandler.getPreviewPath(this.thisSession.rnfr_file, "1", 1);
        String item_name2 = Common.url_decode(request.getProperty("name2", ""));
        this.thisSession.uiPUT("the_command", "RNTO");
        this.thisSession.uiPUT("the_command_data", item_name2);
        if (!item_name.equals(item_name2)) {
          error_message = String.valueOf(error_message) + this.thisSession.do_RNTO(request.getProperty("overwrite", "false").equals("true"));
          if (error_message.equals("") && fromPath != null) {
            item_name2 = String.valueOf(the_dir) + item_name2;
            if (!item_name2.startsWith(this.thisSession.SG("root_dir")))
              item_name2 = String.valueOf(this.thisSession.SG("root_dir")) + item_name2; 
            Properties rnto = this.thisSession.uVFS.get_item(item_name2);
            String toPath = SearchHandler.getPreviewPath(rnto, "1", 1);
            if (toPath != null)
              (new File(String.valueOf(ServerStatus.SG("previews_path")) + Common.all_but_last(Common.all_but_last(fromPath)).substring(1))).renameTo(new File(String.valueOf(ServerStatus.SG("previews_path")) + Common.all_but_last(Common.all_but_last(toPath)).substring(1))); 
          } 
          this.thisSession.uVFS.reset();
        } 
      } 
      response = String.valueOf(response) + "<commandResult><response>" + LOC.G(error_message) + "</response></commandResult>";
      this.thisSession.uVFS.reset();
      return writeResponse(response.replace('%', ' '));
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("makedir")) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      String the_dir = Common.url_decode(request.getProperty("path"));
      the_dir = the_dir.replace(':', '_');
      if (the_dir.startsWith(this.thisSession.SG("root_dir")))
        the_dir = the_dir.substring(this.thisSession.SG("root_dir").length() - 1); 
      this.thisSession.uiPUT("the_command", "MKD");
      this.thisSession.uiPUT("the_command_data", the_dir);
      String error_message = this.thisSession.do_MKD(true, the_dir);
      this.thisSession.uVFS.reset();
      response = String.valueOf(response) + "<commandResult><response>" + LOC.G(error_message) + "</response></commandResult>";
      return writeResponse(response.replace('%', ' '));
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("stat")) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      String the_dir = Common.url_decode(request.getProperty("path"));
      if (the_dir.startsWith(this.thisSession.SG("root_dir")))
        the_dir = the_dir.substring(this.thisSession.SG("root_dir").length() - 1); 
      this.thisSession.uiPUT("the_command", "STAT");
      this.thisSession.uiPUT("the_command_data", the_dir);
      Properties item = this.thisSession.uVFS.get_item(String.valueOf(this.thisSession.SG("root_dir")) + the_dir);
      StringBuffer item_str = new StringBuffer();
      if (request.getProperty("calcFolder", "").equals("true")) {
        Vector listing = new Vector();
        long size = 0L;
        this.thisSession.uVFS.getListing(listing, String.valueOf(this.thisSession.SG("root_dir")) + the_dir, 20, 10000, true, null);
        for (int x = 0; x < listing.size(); x++) {
          Properties p = listing.elementAt(x);
          size += Long.parseLong(p.getProperty("size", "0"));
        } 
        item.put("size", (new StringBuffer(String.valueOf(size))).toString());
      } 
      if (request.getProperty("format", "").equalsIgnoreCase("stat_dmz")) {
        if (item != null)
          item_str.append(formatDmzStat(item)); 
      } else if (item != null && LIST_handler.checkName(item, this.thisSession, true, true)) {
        LIST_handler.generateLineEntry(item, item_str, false, the_dir, true, this.thisSession, false);
      } 
      response = String.valueOf(response) + "<commandResult><response>" + item_str.toString().trim() + "</response></commandResult>";
      return writeResponse(response);
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("mdtm")) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      String the_dir = Common.url_decode(request.getProperty("path"));
      if (the_dir.startsWith(this.thisSession.SG("root_dir")))
        the_dir = the_dir.substring(this.thisSession.SG("root_dir").length() - 1); 
      this.thisSession.uiPUT("the_command", "MDTM");
      this.thisSession.uiPUT("the_command_data", String.valueOf(the_dir) + " " + request.getProperty("date"));
      String error_message = this.thisSession.do_MDTM();
      this.thisSession.uVFS.reset();
      response = String.valueOf(response) + "<commandResult><response>" + LOC.G(error_message) + "</response></commandResult>";
      return writeResponse(response.replace('%', ' '));
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("publish")) {
      String response = "";
      if (Common.System2.get("crushftp.dmz.queue") != null) {
        Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
        Properties action = new Properties();
        action.put("type", "PUT:SHARE");
        action.put("id", Common.makeBoundary());
        action.put("username", this.thisSession.uiSG("user_name"));
        Properties root_item = this.thisSession.uVFS.get_item(this.thisSession.SG("root_dir"));
        GenericClient c = this.thisSession.uVFS.getClient(root_item);
        action.put("crushAuth", c.getConfig("crushAuth"));
        this.thisSession.uVFS.releaseClient(c);
        action.put("user_ip", this.thisSession.uiSG("user_ip"));
        action.put("request", request);
        action.put("clientid", this.thisSession.uiSG("clientid"));
        action.put("need_response", "true");
        queue.addElement(action);
        action = UserTools.waitResponse(action, 300);
        response = action.remove("object_response").toString();
      } else {
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
            if (the_dir.startsWith(this.thisSession.SG("root_dir")))
              the_dir = the_dir.substring(this.thisSession.SG("root_dir").length() - 1); 
            this.thisSession.setupCurrentDir(the_dir);
            Log.log("HTTP_SERVER", 2, "Sharing:" + the_dir + "  vs.  " + this.thisSession.uiSG("current_dir"));
            Properties item = this.thisSession.uVFS.get_item(this.thisSession.uiSG("current_dir"));
            Log.log("HTTP_SERVER", 2, "Sharing:" + item);
            VRL vrl = new VRL(item.getProperty("url"));
            Properties stat = null;
            GenericClient c = this.thisSession.uVFS.getClient(item);
            try {
              stat = c.stat(vrl.getPath());
            } finally {
              c = this.thisSession.uVFS.releaseClient(c);
            } 
            stat.put("privs", item.getProperty("privs"));
            path_items.addElement(stat);
          } 
        } 
        request.put("emailBody", Common.replace_str(request.getProperty("emailBody"), "&amp;", "&"));
        request.put("emailBody", Common.replace_str(request.getProperty("emailBody"), "%26amp%3B", "&"));
        response = createShare(path_items, request, (Vector)this.thisSession.user.get("web_customizations"), this.thisSession.uiSG("user_name"), this.thisSession.server_item.getProperty("linkedServer"), this.thisSession.user, this.thisSession.date_time);
      } 
      return writeResponse(response);
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("cut_paste") || request.getProperty("command", "").equalsIgnoreCase("copy_paste")) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      String[] names = Common.url_decode(request.getProperty("names")).split("\r\n");
      String destPath = Common.url_decode(request.getProperty("destPath"));
      String msg = "OK";
      for (int x = 0; x < names.length; x++) {
        String the_dir1 = names[x].trim();
        if (the_dir1.startsWith(this.thisSession.SG("root_dir")))
          the_dir1 = the_dir1.substring(this.thisSession.SG("root_dir").length() - 1); 
        this.thisSession.setupCurrentDir(the_dir1);
        Properties item = this.thisSession.uVFS.get_item(this.thisSession.uiSG("current_dir"));
        if (item == null) {
          msg = String.valueOf(msg) + "\r\nItem not found:" + names[x];
        } else {
          VRL vrl = new VRL(item.getProperty("url"));
          Properties stat = null;
          GenericClient c = this.thisSession.uVFS.getClient(item);
          try {
            c.login(vrl.getUsername(), vrl.getPassword(), null);
            stat = c.stat(vrl.getPath());
          } finally {
            c = this.thisSession.uVFS.releaseClient(c);
          } 
          boolean deleteAllowed = this.thisSession.check_access_privs(this.thisSession.uiSG("current_dir"), "DELE");
          if (this.thisSession.check_access_privs(this.thisSession.uiSG("current_dir"), "RETR")) {
            String the_dir2 = Common.url_decode(destPath);
            if (the_dir2.startsWith(this.thisSession.SG("root_dir")))
              the_dir2 = the_dir2.substring(this.thisSession.SG("root_dir").length() - 1); 
            this.thisSession.setupCurrentDir(the_dir2);
            Properties item2 = this.thisSession.uVFS.get_item(this.thisSession.uiSG("current_dir"));
            VRL vrl2 = new VRL(String.valueOf(item2.getProperty("url")) + (item2.getProperty("url").endsWith("/") ? "" : "/"));
            if (this.thisSession.check_access_privs(this.thisSession.uiSG("current_dir"), "STOR")) {
              String addon = "";
              boolean ok = true;
              if ((new VRL(String.valueOf(vrl2.toString()) + vrl.getName() + (stat.getProperty("type").equalsIgnoreCase("DIR") ? "/" : ""))).toString().startsWith(vrl.toString())) {
                ok = false;
                String s1 = (new VRL(String.valueOf(vrl2.toString()) + vrl.getName() + (stat.getProperty("type").equalsIgnoreCase("DIR") ? "/" : ""))).toString();
                String s2 = vrl.toString();
                while (s1.endsWith("/"))
                  s1 = s1.substring(0, s1.length() - 1); 
                while (s2.endsWith("/"))
                  s2 = s2.substring(0, s2.length() - 1); 
                if (s1.equals(s2)) {
                  ok = true;
                  addon = String.valueOf(addon) + "_copy_" + Common.makeBoundary(3);
                } else {
                  msg = String.valueOf(msg) + "\r\n" + LOC.G("Cannot copy item into itself.");
                } 
              } 
              if (ok) {
                this.thisSession.trackAndUpdateUploads(this.thisSession.uiVG("lastUploadStats"), vrl, vrl2, "RENAME");
                SearchHandler.buildEntry(item, this.thisSession.uVFS, true, true);
                the_dir2 = String.valueOf(the_dir2) + vrl.getName() + (stat.getProperty("type").equalsIgnoreCase("DIR") ? "/" : "");
                VRL vrl_dest = new VRL(vrl2 + vrl.getName() + addon + (stat.getProperty("type").equalsIgnoreCase("DIR") ? "/" : ""));
                GenericClient c1 = this.thisSession.uVFS.getClient(item);
                c1.login(vrl.getUsername(), vrl.getPassword(), null);
                GenericClient c2 = this.thisSession.uVFS.getClient(item);
                c2.login(vrl_dest.getUsername(), vrl_dest.getPassword(), null);
                Common.recurseCopy(vrl, vrl_dest, c1, c2, 0, true);
                c1 = this.thisSession.uVFS.releaseClient(c1);
                c2 = this.thisSession.uVFS.releaseClient(c2);
                SearchHandler.buildEntry(item2, this.thisSession.uVFS, false, false);
                if (!the_dir1.startsWith(this.thisSession.SG("root_dir")))
                  the_dir1 = String.valueOf(this.thisSession.SG("root_dir")) + the_dir1.substring(1); 
                if (!the_dir2.startsWith(this.thisSession.SG("root_dir")))
                  the_dir2 = String.valueOf(this.thisSession.SG("root_dir")) + the_dir2.substring(1); 
                if (request.getProperty("command", "").equalsIgnoreCase("cut_paste")) {
                  if (deleteAllowed) {
                    c1 = this.thisSession.uVFS.getClient(item);
                    c1.login(vrl.getUsername(), vrl.getPassword(), null);
                    Common.recurseDelete(vrl, false, c1, 0);
                    c1 = this.thisSession.uVFS.releaseClient(c2);
                    Common.trackSync("RENAME", the_dir1, the_dir2, false, 0L, 0L, this.thisSession.SG("root_dir"), item.getProperty("privs"), this.thisSession.uiSG("clientid"), "");
                  } else {
                    msg = String.valueOf(msg) + "\r\n" + LOC.G("Item $0 copied, but not 'cut' as you did not have delete permissions.", vrl.getName());
                    Common.trackSync("CHANGE", the_dir2, null, true, 0L, 0L, this.thisSession.SG("root_dir"), item.getProperty("privs"), this.thisSession.uiSG("clientid"), "");
                  } 
                } else {
                  Common.trackSync("CHANGE", the_dir2, null, true, 0L, 0L, this.thisSession.SG("root_dir"), item.getProperty("privs"), this.thisSession.uiSG("clientid"), "");
                } 
              } 
            } else {
              msg = String.valueOf(msg) + "\r\n" + LOC.G("Cannot copy $0 because you don't have write permission here.", vrl.getName());
            } 
          } else {
            msg = String.valueOf(msg) + "\r\n" + LOC.G("Cannot copy $0 because you don't have read permission here.", vrl.getName());
          } 
        } 
      } 
      response = String.valueOf(response) + "<commandResult><response>";
      response = String.valueOf(response) + msg;
      response = String.valueOf(response) + "</response></commandResult>";
      return writeResponse(response.replace('%', ' '));
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("editKeywords")) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      String[] names = Common.url_decode(request.getProperty("names")).split("\r\n");
      String keyword = Common.url_decode(request.getProperty("keywords")).trim();
      response = String.valueOf(response) + "<commandResult><response>";
      boolean ok = false;
      for (int x = 0; x < names.length; x++) {
        String the_dir = Common.url_decode(Common.all_but_last(names[x]));
        if (the_dir.startsWith(this.thisSession.SG("root_dir")))
          the_dir = the_dir.substring(this.thisSession.SG("root_dir").length() - 1); 
        this.thisSession.setupCurrentDir(the_dir);
        Properties item = this.thisSession.uVFS.get_item(String.valueOf(this.thisSession.uiSG("current_dir")) + Common.last(names[x]));
        the_dir = SearchHandler.getPreviewPath(item, "1", 1);
        String index = String.valueOf(ServerStatus.SG("previews_path")) + the_dir.substring(1);
        if (!(new File(Common.all_but_last(index))).exists())
          (new File(Common.all_but_last(index))).mkdirs(); 
        if (ServerStatus.BG("exif_keywords")) {
          String srcFile = Common.dots((new VRL(item.getProperty("url"))).getPath());
          for (int xx = 0; xx < ServerStatus.thisObj.previewWorkers.size(); xx++) {
            PreviewWorker preview = ServerStatus.thisObj.previewWorkers.elementAt(xx);
            if (preview.prefs.getProperty("preview_enabled", "false").equalsIgnoreCase("true") && preview.checkExtension(Common.last(the_dir), new File(srcFile))) {
              preview.setExifInfo(srcFile, PreviewWorker.getDestPath(String.valueOf(srcFile) + "/p1/"), "keywords", Common.url_decode(request.getProperty("keywords")).trim());
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
        SearchHandler.buildEntry(item, this.thisSession.uVFS, false, false);
      } 
      if (ok)
        response = String.valueOf(response) + "Keywords Edited.\r\n"; 
      response = String.valueOf(response) + "</response></commandResult>";
      return writeResponse(response.replace('%', ' '));
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("search")) {
      String the_dir = Common.url_decode(request.getProperty("path"));
      if (the_dir.startsWith(this.thisSession.SG("root_dir")))
        the_dir = the_dir.substring(this.thisSession.SG("root_dir").length() - 1); 
      this.thisSession.setupCurrentDir(the_dir);
      String[] keywords = Common.url_decode(request.getProperty("keyword").replace('+', ' ')).trim().split(" ");
      boolean exact = request.getProperty("exact", "").equalsIgnoreCase("true");
      boolean date1 = request.getProperty("date1", "").equalsIgnoreCase("true");
      String date1_action = Common.url_decode(request.getProperty("date1_action", "").replace('+', ' '));
      String date1_value = request.getProperty("date1_value", "");
      boolean date2 = request.getProperty("date2", "").equalsIgnoreCase("true");
      String date2_action = Common.url_decode(request.getProperty("date2_action", "").replace('+', ' '));
      String date2_value = request.getProperty("date2_value", "");
      boolean size1 = request.getProperty("size1", "").equalsIgnoreCase("true");
      String size1_action = Common.url_decode(request.getProperty("size1_action", "").replace('+', ' '));
      String size1_value = request.getProperty("size1_value", "");
      boolean type1 = request.getProperty("type1", "").equalsIgnoreCase("true");
      String type1_action = Common.url_decode(request.getProperty("type1_action", "").replace('+', ' '));
      this.thisSession.uiPUT("the_command", "LIST");
      this.thisSession.uiPUT("the_command_data", the_dir);
      Vector foundItems = new Vector();
      SimpleDateFormat mmddyyyy = new SimpleDateFormat("MM/dd/yyyy");
      long processedCount = 0L;
      int errors = 0;
      Vector listing = new Vector();
      SearchHandler searchHandler = new SearchHandler(this.thisSession, listing);
      Worker.startWorker(searchHandler);
      Properties root_item = this.thisSession.uVFS.get_item(this.thisSession.uiSG("root_dir"));
      while (searchHandler.isActive() || listing.size() > 0) {
        while (searchHandler.isActive() && listing.size() == 0)
          Thread.sleep(100L); 
        if (listing.size() == 0)
          break; 
        Properties pp = listing.elementAt(0);
        listing.removeElementAt(0);
        processedCount++;
        Common.System2.put("crushftp.activeSearch.info" + this.thisSession.getId(), "Searched " + processedCount + " items.");
        try {
          boolean date_ok = true;
          boolean size_ok = true;
          boolean type_ok = true;
          boolean name_ok = true;
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
          if (type1) {
            String item_type1 = pp.getProperty("type");
            if (type1_action.equalsIgnoreCase("file") && !item_type1.equalsIgnoreCase("file")) {
              type_ok = false;
            } else if (type1_action.equalsIgnoreCase("folder") && !item_type1.equalsIgnoreCase("dir")) {
              type_ok = false;
            } 
          } 
          if (date_ok && size_ok && type_ok && keywords.length > 0) {
            name_ok = false;
            if (request.getProperty("keywords_only", "false").equals("false")) {
              int loop = 0;
              while (loop < keywords.length) {
                if (name_ok)
                  break; 
                Log.log("HTTP_SERVER", 2, "search item name:" + pp.getProperty("name") + " vs. " + keywords[loop]);
                if (!exact && pp.getProperty("name").toUpperCase().indexOf(keywords[loop].toUpperCase().trim()) >= 0)
                  name_ok = true; 
                if (exact && pp.getProperty("name").toUpperCase().equalsIgnoreCase(keywords[loop].toUpperCase().trim()))
                  name_ok = true; 
                loop++;
              } 
            } 
            if (!name_ok) {
              Log.log("HTTP_SERVER", 2, "name still not found, trying more...");
              String indexText = pp.getProperty("keywords", "");
              if (!pp.containsKey("keywords"))
                indexText = SearchHandler.getKeywords(pp); 
              for (int x = 0; x < keywords.length; x++) {
                if (name_ok)
                  break; 
                Log.log("HTTP_SERVER", 2, "search item indexText:" + indexText + " vs. " + keywords[x]);
                if (!exact && indexText.toUpperCase().indexOf(keywords[x].toUpperCase().trim()) >= 0)
                  name_ok = true; 
                if (exact && indexText.toUpperCase().indexOf("\r\n" + keywords[x].toUpperCase().trim() + "\r\n") >= 0)
                  name_ok = true; 
              } 
            } 
          } 
          Log.log("HTTP_SERVER", 2, "search item name_ok:" + name_ok + " date_ok:" + date_ok + " size_ok:" + size_ok + " type_ok:" + type_ok);
          if (name_ok && date_ok && size_ok && type_ok) {
            Log.log("HTTP_SERVER", 2, "search item result:" + pp.getProperty("name"));
            if (pp.getProperty("db", "false").equals("true")) {
              Properties ppp = SearchHandler.findItem(pp, this.thisSession.uVFS, null, this.thisSession.uiSG("root_dir"));
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
              if (current_dir2.toUpperCase().startsWith(this.thisSession.SG("root_dir").toUpperCase()))
                current_dir2 = current_dir2.substring(this.thisSession.SG("root_dir").length() - 1); 
              pp.put("privs", String.valueOf(privs) + "(comment" + Common.url_encode("Containing Folder:<a href='" + current_dir2 + "'>" + current_dir2 + "</a>") + ")");
            } 
          } 
        } catch (Exception e) {
          Log.log("HTTP_SERVER", 1, e);
          if (errors++ > 1000)
            break; 
        } 
      } 
      Common.System2.remove("crushftp.activeSearch.info" + this.thisSession.getId());
      Properties listingProp = getListingInfo(foundItems, this.thisSession.SG("root_dir"), "(VIEW)", false, this.thisSession.uVFS, false, true, this.thisSession, true);
      String altList = getJsonListObj(listingProp);
      String info = "{\r\n";
      info = String.valueOf(info) + "\t\"privs\" : \"" + listingProp.getProperty("privs", "").trim().replaceAll("\"", "%22").replaceAll("\t", "%09") + "\",\r\n";
      info = String.valueOf(info) + "\t\"path\" : \"" + listingProp.getProperty("path", "").trim().replaceAll("\"", "%22").replaceAll("\t", "%09") + "\",\r\n";
      info = String.valueOf(info) + "\t\"defaultStrings\" : \"" + listingProp.getProperty("defaultStrings", "").trim() + "\",\r\n";
      info = String.valueOf(info) + "\t\"site\" : \"" + listingProp.getProperty("site", "").trim() + "\",\r\n";
      info = String.valueOf(info) + "\t\"quota\" : \"" + listingProp.getProperty("quota", "").trim() + "\",\r\n";
      info = String.valueOf(info) + "\t\"listing\" : " + altList + "\r\n";
      info = String.valueOf(info) + "}\r\n";
      return writeResponse(info);
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("getSearchStatus")) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      response = String.valueOf(response) + "<commandResult><response>" + Common.System2.getProperty("crushftp.activeSearch.info" + this.thisSession.getId(), "").trim() + "</response></commandResult>";
      this.thisSession.uVFS.reset();
      return writeResponse(response);
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("getTunnels")) {
      String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
      String userTunnels = String.valueOf(this.thisSessionHTTP.thisSession.user.getProperty("tunnels", "")) + ",";
      Vector tunnels = (Vector)ServerStatus.VG("tunnels").clone();
      tunnels.addAll(ServerStatus.VG("tunnels_dmz"));
      ByteArrayOutputStream baot = new ByteArrayOutputStream();
      int lesserSpeed = this.thisSession.IG("speed_limit_upload");
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
      this.thisSession.uVFS.reset();
      return writeResponse(response);
    } 
    if (request.getProperty("command", "").equalsIgnoreCase("download")) {
      Enumeration keys = request.keys();
      Properties metaInfo = new Properties();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement().toString();
        if (key.toUpperCase().startsWith("META_")) {
          String val = request.getProperty(key);
          key = key.substring("META_".length());
          metaInfo.put(key, val);
        } 
      } 
      this.thisSession.uiPUT("the_command", "RETR");
      String the_dir = Common.url_decode(request.getProperty("path"));
      if (!the_dir.toUpperCase().startsWith(this.thisSession.SG("root_dir").toUpperCase()))
        the_dir = String.valueOf(this.thisSession.SG("root_dir")) + (the_dir.startsWith("/") ? the_dir.substring(1) : the_dir); 
      this.thisSession.uiPUT("current_dir", the_dir);
      Properties item = null;
      VRL otherFile = null;
      boolean ok = (this.thisSession.check_access_privs(this.thisSession.uiSG("current_dir"), "RETR") && Common.filter_check("U", Common.last(this.thisSession.uiSG("current_dir")), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + this.thisSessionHTTP.SG("file_filter")));
      item = this.thisSession.uVFS.get_item(this.thisSession.uiSG("current_dir"));
      if (item != null && item.getProperty("type", "").equalsIgnoreCase("DIR") && !this.thisSession.uiSG("current_dir").endsWith("/"))
        ok = false; 
      if (ok)
        otherFile = new VRL(item.getProperty("url")); 
      if (otherFile == null && this.thisSession.uiSG("current_dir").toUpperCase().endsWith(".ZIP")) {
        this.thisSession.uiPUT("current_dir", this.thisSession.uiSG("current_dir").substring(0, this.thisSession.uiSG("current_dir").length() - 4));
        ok = (this.thisSession.check_access_privs(this.thisSession.uiSG("current_dir"), "RETR") && Common.filter_check("U", Common.last(this.thisSession.uiSG("current_dir")), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + this.thisSessionHTTP.SG("file_filter")));
        item = this.thisSession.uVFS.get_item(this.thisSession.uiSG("current_dir"));
        if (item != null && ok && item.getProperty("privs").indexOf("(read)") < 0)
          ok = false; 
        if (ok) {
          otherFile = new VRL(item.getProperty("url"));
          otherFile = new VRL(String.valueOf(Common.all_but_last(otherFile.toString())) + otherFile.getName() + ".zip");
        } else {
          this.thisSession.uiPUT("current_dir", String.valueOf(this.thisSession.uiSG("current_dir")) + ".zip");
        } 
      } 
      if (ok) {
        if (!metaInfo.getProperty("downloadRevision", "").equals("") && ServerStatus.siIG("enterprise_level") > 0) {
          VRL otherFile2 = null;
          int rev = Integer.parseInt(metaInfo.getProperty("downloadRevision", ""));
          while (rev >= 0) {
            String privs = item.getProperty("privs");
            if (privs.indexOf("(sync") >= 0) {
              String path = this.thisSession.uiSG("current_dir");
              if (path.startsWith(this.thisSession.SG("root_dir")))
                path = path.substring(this.thisSession.SG("root_dir").length() - 1); 
              String revPath = Common.parseSyncPart(privs, "revisionsPath");
              if (!revPath.equals(""))
                otherFile2 = new VRL(String.valueOf(revPath) + path + "/" + rev + "/" + otherFile.getName()); 
            } 
            if (otherFile2 != null && (new File(otherFile2.getPath())).exists())
              break; 
            otherFile2 = null;
            rev--;
          } 
          if (otherFile2 != null)
            otherFile = otherFile2; 
        } 
      } else {
        boolean ok1 = (this.thisSession.check_access_privs(Common.all_but_last(this.thisSession.uiSG("current_dir")), "RETR") && Common.filter_check("U", Common.last(this.thisSession.uiSG("current_dir")), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + this.thisSessionHTTP.SG("file_filter")));
        boolean ok2 = (this.thisSession.check_access_privs(this.thisSession.uiSG("current_dir"), "RETR") && Common.filter_check("U", Common.last(this.thisSession.uiSG("current_dir")), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + this.thisSessionHTTP.SG("file_filter")));
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
    if (request.getProperty("command", "").startsWith("downloadAsZip")) {
      this.thisSession.uiPUT("the_command", "RETR");
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
      write_command_http("Content-Disposition: attachment; filename=\"" + ((this.thisSessionHTTP.userAgent.toUpperCase().indexOf("MSIE") >= 0 || this.thisSessionHTTP.userAgent.toUpperCase().indexOf("TRIDENT") >= 0) ? Common.url_encode(fname) : fname) + "\"");
      write_command_http("X-UA-Compatible: chrome=1");
      write_command_http("Connection: close");
      write_command_http("");
      if (Common.System2.get("crushftp.dmz.queue") != null) {
        String root_loc = itemList[itemList.length - 1];
        if (!root_loc.startsWith(this.thisSession.SG("root_dir")))
          root_loc = String.valueOf(this.thisSession.SG("root_dir")) + root_loc.substring(1); 
        Properties properties = this.thisSession.uVFS.get_item(root_loc);
        GenericClient c = this.thisSession.uVFS.getClient(properties);
        try {
          InputStream in = ((HTTPClient)c).downloadAsZip(current_dir, paths);
          Common.copyStreams(in, this.thisSessionHTTP.original_os, true, true);
        } catch (Exception e) {
          Log.log("HTTP_SERVER", 1, e);
        } 
        c.close();
        this.thisSession.uVFS.releaseClient(c);
        return true;
      } 
      if (!current_dir.toUpperCase().startsWith(this.thisSession.SG("root_dir")))
        current_dir = String.valueOf(this.thisSession.SG("root_dir")) + current_dir.substring(1); 
      Properties item = this.thisSession.uVFS.get_item("/");
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
          if (!current_dir2.toUpperCase().startsWith(this.thisSession.SG("root_dir")))
            current_dir2 = String.valueOf(this.thisSession.SG("root_dir")) + current_dir2.substring(1); 
          this.thisSession.uiPUT("current_dir", current_dir2);
          Properties item2 = this.thisSession.uVFS.get_item(this.thisSession.uiSG("current_dir"));
          if (item2 != null && this.thisSession.check_access_privs(this.thisSession.uiSG("current_dir"), "RETR")) {
            boolean singleThread = !(sizeLookup.size() <= 0 && !item2.getProperty("url").toUpperCase().startsWith("FTP:/") && !item2.getProperty("url").toUpperCase().startsWith("HTTP:/"));
            if (item2.getProperty("type").equalsIgnoreCase("FILE")) {
              this.thisSessionHTTP.retr.zipFiles.addElement(item2);
            } else {
              Common.startMultiThreadZipper(this.thisSession.uVFS, this.thisSessionHTTP.retr, this.thisSession.uiSG("current_dir"), 0, singleThread, activeThreads);
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
        if (root_dir.startsWith(this.thisSession.SG("root_dir")))
          root_dir = root_dir.substring(this.thisSession.SG("root_dir").length() - 1); 
        String size = sizeLookup.getProperty(String.valueOf(root_dir) + zitem.getProperty("name"));
        if (size != null)
          if (Long.parseLong(size) == Long.parseLong(zitem.getProperty("size"))) {
            this.thisSessionHTTP.retr.zipFiles.removeElementAt(x);
          } else if (Long.parseLong(size) < Long.parseLong(zitem.getProperty("size"))) {
            zitem.put("rest", size);
          }  
      } 
      VRL otherFile = new VRL(item.getProperty("url"));
      this.thisSession.uiPUT("current_dir", current_dir);
      this.thisSession.uiPUT("file_transfer_mode", "BINARY");
      this.thisSessionHTTP.retr.data_os = this.thisSessionHTTP.original_os;
      this.thisSessionHTTP.retr.httpDownload = true;
      this.thisSessionHTTP.retr.zipping = true;
      this.thisSession.uiPUT("no_zip_compression", request.getProperty("no_zip_compression", "false"));
      String the_dir = this.thisSession.uiSG("current_dir");
      Properties pp = new Properties();
      pp.put("the_dir", the_dir);
      this.thisSession.runPlugin("transfer_path", pp);
      the_dir = pp.getProperty("the_dir", the_dir);
      this.thisSessionHTTP.retr.init_vars(the_dir, this.thisSession.uiLG("start_resume_loc"), -1L, this.thisSession, item, false, "", otherFile);
      this.thisSessionHTTP.retr.runOnce = true;
      this.thisSessionHTTP.retr.run();
      return true;
    } 
    if (request.getProperty("command", "").equals("getXMLListing")) {
      String the_dir = Common.url_decode(request.getProperty("path", ""));
      the_dir = Common.dots(the_dir);
      if (the_dir.equals("/"))
        the_dir = this.thisSession.SG("root_dir"); 
      if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(this.thisSession.SG("root_dir").toUpperCase()))
        the_dir = String.valueOf(this.thisSession.SG("root_dir")) + the_dir.substring(1); 
      this.thisSession.uiPUT("current_dir", the_dir);
      String response = getXmlListingResponse(this.thisSession.uiSG("user_name"), request, the_dir, false, this.thisSession.uVFS);
      return writeResponse(response, request.getProperty("format", "").equalsIgnoreCase("JSONOBJ"));
    } 
    if (request.getProperty("command", "").equals("manageShares")) {
      String response = "";
      if (Common.System2.get("crushftp.dmz.queue") != null) {
        Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
        Properties action = new Properties();
        action.put("type", "PUT:MANAGESHARES");
        action.put("id", Common.makeBoundary());
        action.put("username", this.thisSession.uiSG("user_name"));
        Properties root_item = this.thisSession.uVFS.get_item(this.thisSession.SG("root_dir"));
        GenericClient c = this.thisSession.uVFS.getClient(root_item);
        action.put("crushAuth", c.getConfig("crushAuth"));
        this.thisSession.uVFS.releaseClient(c);
        action.put("user_ip", this.thisSession.uiSG("user_ip"));
        action.put("request", request);
        action.put("clientid", this.thisSession.uiSG("clientid"));
        action.put("need_response", "true");
        queue.addElement(action);
        action = UserTools.waitResponse(action, 300);
        response = action.remove("object_response").toString();
      } else {
        response = manageShares(this.thisSession);
      } 
      return writeResponse(response);
    } 
    if (request.getProperty("command", "").equals("deleteTempAccount")) {
      String response = "";
      if (Common.System2.get("crushftp.dmz.queue") != null) {
        Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
        Properties action = new Properties();
        action.put("type", "PUT:DELETESHARE");
        action.put("id", Common.makeBoundary());
        action.put("username", this.thisSession.uiSG("user_name"));
        Properties root_item = this.thisSession.uVFS.get_item(this.thisSession.SG("root_dir"));
        GenericClient c = this.thisSession.uVFS.getClient(root_item);
        action.put("crushAuth", c.getConfig("crushAuth"));
        this.thisSession.uVFS.releaseClient(c);
        action.put("user_ip", this.thisSession.uiSG("user_ip"));
        action.put("request", request);
        action.put("clientid", this.thisSession.uiSG("clientid"));
        action.put("need_response", "true");
        queue.addElement(action);
        action = UserTools.waitResponse(action, 300);
        response = action.remove("object_response").toString();
      } else {
        response = deleteShare(request, this.thisSession);
      } 
      return writeResponse(response);
    } 
    if (request.getProperty("command", "").equals("getCustomForm")) {
      String response = "";
      String name = this.thisSession.SG(request.getProperty("form"));
      if (name.indexOf(":") < 0 && !name.equals("messageForm"))
        name = String.valueOf(name) + ":" + name; 
      if (!session.getProperty(request.getProperty("form"), "").equals(""))
        name = session.getProperty(request.getProperty("form"), ""); 
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
            form.put("always", (new StringBuffer(String.valueOf(!this.thisSession.SG(request.getProperty("form")).endsWith(":once")))).toString());
            if (form.getProperty("name").equalsIgnoreCase(name)) {
              Vector entries = (Vector)form.get("entries");
              for (int xx = 0; xx < entries.size(); xx++) {
                Properties entry = entries.elementAt(xx);
                entry.put("item_type", entry.getProperty("type"));
                if (session.containsKey(entry.getProperty("name", "label").trim()))
                  entry.put("value", Common.url_decode(session.getProperty(entry.getProperty("name").trim()))); 
                entry.put("value", ServerStatus.change_vars_to_values_static(entry.getProperty("value", ""), this.thisSession.user, this.thisSession.user_info, this.thisSession));
                entry.put("label", ServerStatus.change_vars_to_values_static(entry.getProperty("label", ""), this.thisSession.user, this.thisSession.user_info, this.thisSession));
              } 
              try {
                response = ServerStatus.thisObj.common_code.getXMLString(form, "customForm", null);
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
    if (request.getProperty("command", "").equals("getUserInfo")) {
      Properties responseProp = new Properties();
      Properties extraCustomizations = (Properties)session.get("extraCustomizations");
      if (extraCustomizations != null)
        responseProp.putAll(extraCustomizations); 
      Vector customizations = null;
      try {
        customizations = (Vector)this.thisSession.user.get("web_customizations");
        if (customizations == null)
          customizations = (Vector)UserTools.ut.getUser(this.thisSession.uiSG("listen_ip_port"), "default", false).get("web_customizations"); 
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
          pp.put("value", ServerStatus.thisObj.change_vars_to_values(pp.getProperty("value", ""), this.thisSession));
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
          Properties pp = (Properties)userCust.get(this.thisSession.uiSG("user_name").toUpperCase());
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
        Vector buttons = (Vector)this.thisSession.user.get("web_buttons");
        if (buttons == null)
          buttons = (Vector)UserTools.ut.getUser(this.thisSession.uiSG("listen_ip_port"), "default", false).get("buttons"); 
        if (buttons == null)
          buttons = new Vector(); 
        buttons = (Vector)buttons.clone();
        addMissingButtons(buttons);
        fixButtons(buttons);
        responseProp.put("buttons", buttons);
      } catch (Exception e) {
        Log.log("HTTP_SERVER", 1, e);
      } 
      try {
        Enumeration keys = this.thisSession.user.keys();
        while (keys.hasMoreElements()) {
          String key = keys.nextElement().toString();
          if (key.startsWith("x_")) {
            Properties pp = new Properties();
            pp.put("key", key.substring(2).toUpperCase().trim());
            pp.put("value", this.thisSession.user.getProperty(key));
            customizations.addElement(pp);
          } 
        } 
      } catch (Exception e) {
        Log.log("HTTP_SERVER", 1, e);
      } 
      responseProp.put("user_priv_options", this.thisSession.SG("site"));
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
      request.put("path", Common.url_decode(request.getProperty("path", "/")));
      String the_dir = request.getProperty("path", "/");
      if (the_dir.equals(""))
        the_dir = "/"; 
      if (!the_dir.startsWith(this.thisSession.SG("root_dir")))
        the_dir = String.valueOf(this.thisSession.SG("root_dir")) + the_dir.substring(1); 
      Properties site_item = this.thisSession.uVFS.get_item(the_dir, 0);
      if (site_item != null) {
        if (!request.getProperty("path", "/").equals("/") && !request.getProperty("path", "/").equals("")) {
          GenericClient c = this.thisSession.uVFS.getClient(site_item);
          try {
            c.doCommand("CWD " + request.getProperty("path", "/"));
          } finally {
            c = this.thisSession.uVFS.releaseClient(c);
          } 
        } 
        try {
          if (ServerStatus.BG("allow_impulse"))
            getUserInfo("IMPULSEINFO", site_item, the_dir); 
        } catch (Exception exception) {}
      } 
      responseProp.put("user_name", this.thisSession.uiSG("user_name"));
      responseProp.put("username", this.thisSession.uiSG("user_name"));
      String response = "";
      try {
        response = ServerStatus.thisObj.common_code.getXMLString(responseProp, "userInfo", null);
      } catch (Exception e) {
        Log.log("HTTP_SERVER", 1, e);
      } 
      response = Common.replace_str(response, ">/WebInterface/", ">" + this.thisSessionHTTP.proxy + "WebInterface/");
      return writeResponse(response);
    } 
    if (request.getProperty("command", "").equals("changePassword")) {
      String result = "Not Allowed";
      if (this.thisSession.SG("site").indexOf("(SITE_PASS)") >= 0) {
        String current_password = Common.url_decode(request.getProperty("current_password", "")).trim();
        String new_password1 = Common.url_decode(request.getProperty("new_password1", "")).trim();
        String new_password2 = Common.url_decode(request.getProperty("new_password2", "")).trim();
        String user_password = ServerStatus.thisObj.common_code.decode_pass(this.thisSession.user.getProperty("password"));
        if (user_password.startsWith("MD5:")) {
          current_password = ServerStatus.thisObj.common_code.encode_pass(current_password, "MD5");
        } else if (user_password.startsWith("SHA:")) {
          current_password = ServerStatus.thisObj.common_code.encode_pass(current_password, "SHA");
        } else if (user_password.startsWith("SHA512:")) {
          current_password = ServerStatus.thisObj.common_code.encode_pass(current_password, "SHA512");
        } else if (user_password.startsWith("CRYPT3:")) {
          if (ServerStatus.thisObj.common_code.crypt3(current_password, user_password).equals(user_password))
            current_password = user_password; 
        } else if (user_password.startsWith("BCRYPT:")) {
          if (ServerStatus.thisObj.common_code.bcrypt(current_password, user_password).equals(user_password))
            current_password = user_password; 
        } else if (user_password.startsWith("MD5CRYPT:")) {
          if (ServerStatus.thisObj.common_code.md5crypt(current_password, user_password).equals(user_password))
            current_password = user_password; 
        } 
        if (current_password.length() > 0 && new_password1.length() > 0)
          if (current_password.equals(user_password) && new_password1.equals(new_password2) && !this.thisSession.uiSG("user_name").equalsIgnoreCase("anonymous")) {
            this.thisSession.uiPUT("current_password", user_password);
            result = this.thisSession.do_ChangePass(this.thisSession.uiSG("user_name"), new_password1);
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
    if (request.getProperty("the_action", "").equalsIgnoreCase("FTPPROXY"))
      if (this.thisSession.SG("site").indexOf("(SITE_WEBFTPPROXY)") >= 0) {
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
        boolean ok = (this.thisSession.SG("site").indexOf("(SITE_ADHOCWEBFTPPROXY)") >= 0);
        boolean blacklisted = false;
        for (int x = 0; x < proxyRules.size(); x++) {
          Properties pp = proxyRules.elementAt(x);
          if (Common.do_search(pp.getProperty("host"), ip, false, 0)) {
            String part1 = ServerStatus.thisObj.change_vars_to_values(pp.getProperty("criteria1"), this.thisSession);
            String part2 = ServerStatus.thisObj.change_vars_to_values(pp.getProperty("criteria2"), this.thisSession);
            if ((Common.do_search(part1, part2, false, 0) || Common.do_search(part2, part1, false, 0)) && pp.getProperty("condition").equals("=")) {
              ok = true;
              break;
            } 
            if (!Common.do_search(part1, part2, false, 0) && !Common.do_search(part2, part1, false, 0) && pp.getProperty("condition").equals("!=")) {
              ok = true;
              break;
            } 
            if (part1.equals("1") && part2.equals("2") && pp.getProperty("condition").equals("=")) {
              ServerStatus.thisObj.runAlerts("proxy_blacklist", this.thisSession);
              blacklisted = true;
              break;
            } 
          } 
        } 
        if (ok && !blacklisted) {
          Properties virtual = this.thisSession.uVFS.homes.elementAt(0);
          Vector v = new Vector();
          Properties dir_item = new Properties();
          dir_item.put("url", String.valueOf(protocol) + "://" + Common.url_encode(username) + ":" + Common.url_encode(password) + "@" + ip + ":" + port + path);
          dir_item.put("type", "DIR");
          v.addElement(dir_item);
          Properties vItem = new Properties();
          vItem.put("virtualPath", "/ftp");
          vItem.put("name", "ftp");
          vItem.put("type", "FILE");
          vItem.put("vItems", v);
          virtual.put("/ftp", vItem);
          this.thisSession.uVFS.reset();
          this.thisSession.add_log_formatted("Proxy created to URL:ftp://" + Common.url_encode(username) + ":********************@" + ip + path, "POST");
          this.thisSessionHTTP.sendRedirect("/ftp/");
        } else {
          this.thisSessionHTTP.sendRedirect("/");
        } 
        write_command_http("Connection: close");
        write_command_http("Content-Length: 0");
        write_command_http("");
        return true;
      }  
    return false;
  }
  
  public void addMissingButtons(Vector buttons) throws Exception {
    if (this.thisSession.SG("site").indexOf("(CONNECT)") >= 0 || this.thisSession.SG("site").indexOf("(USER_ADMIN)") >= 0) {
      boolean found = false;
      for (int x = 0; x < buttons.size(); x++) {
        if (buttons.elementAt(x).toString().indexOf("admin") >= 0)
          found = true; 
      } 
      if (!found) {
        Properties properties = new Properties();
        properties.put("key", "(admin):Admin");
        properties.put("for_menu", "true");
        properties.put("for_context_menu", "false");
        if (this.thisSession.SG("site").indexOf("(CONNECT)") >= 0) {
          properties.put("value", String.valueOf(this.thisSessionHTTP.proxy) + "WebInterface/admin/index.html");
        } else {
          properties.put("value", String.valueOf(this.thisSessionHTTP.proxy) + "WebInterface/UserManager/index.html");
        } 
        buttons.insertElementAt((E)properties, 0);
      } 
    } 
    Properties button = new Properties();
    button.put("key", "(copyDirectLink):Copy Link");
    button.put("for_menu", "false");
    button.put("for_context_menu", "true");
    button.put("value", "javascript:performAction('copyDirectLink');");
    buttons.insertElementAt((E)button, 0);
  }
  
  public String getJsonList(Properties listingProp) {
    Vector listing = (Vector)listingProp.remove("listing");
    StringBuffer sb = new StringBuffer();
    sb.append("l = new Array();\r\n");
    String s = "";
    for (int x = 0; x < listing.size(); x++) {
      Properties lp = listing.elementAt(x);
      String eol = "\r\n";
      sb.append("lp = {};\r\n");
      s = "name";
      sb.append("lp." + s + "=\"" + lp.getProperty(s, "").replaceAll("%", "%25").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D") + "\";" + eol);
      s = "dir";
      sb.append("lp." + s + "=\"" + lp.getProperty(s, "").replaceAll("%", "%25").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D") + "\";" + eol);
      s = "type";
      sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol);
      s = "root_dir";
      sb.append("lp." + s + "=\"" + lp.getProperty(s, "").replaceAll("%", "%25").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D") + "\";" + eol);
      s = "source";
      sb.append("lp." + s + "=\"" + lp.getProperty(s, "").replaceAll("%", "%25").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D") + "\";" + eol);
      s = "href_path";
      sb.append("lp." + s + "=\"" + lp.getProperty(s, "").replaceAll("%", "%25").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D") + "\";" + eol);
      s = "privs";
      sb.append("lp." + s + "=\"" + lp.getProperty(s).replaceAll("%", "%25").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D") + "\";" + eol);
      s = "size";
      sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol);
      s = "modified";
      sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol);
      s = "owner";
      sb.append("lp." + s + "=\"" + lp.getProperty(s).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D") + "\";" + eol);
      s = "group";
      sb.append("lp." + s + "=\"" + lp.getProperty(s).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D") + "\";" + eol);
      s = "permissionsNum";
      sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol);
      s = "keywords";
      sb.append("lp." + s + "=\"" + lp.getProperty(s, "").trim().replaceAll("%", "%25").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D") + "\";" + eol);
      s = "permissions";
      sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol);
      s = "num_items";
      sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol);
      s = "preview";
      sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol);
      s = "dateFormatted";
      sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol);
      s = "sizeFormatted";
      sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol);
      sb.append("l[l.length] = lp;" + eol);
    } 
    return "\r\n<listing>" + sb.toString() + "</listing>";
  }
  
  public String getJsonListObj(Properties listingProp) {
    Vector listing = (Vector)listingProp.remove("listing");
    StringBuffer sb = new StringBuffer();
    sb.append("[");
    String s = "";
    String eol = "\r\n";
    int loops = 0;
    for (int x = 0; x < listing.size(); x++) {
      Properties lp = listing.elementAt(x);
      if (x > 0) {
        sb.append(",{").append(eol);
      } else {
        sb.append("{").append(eol);
      } 
      s = "name";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "dir";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "type";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "root_dir";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "source";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "href_path";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "privs";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "size";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "modified";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "owner";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "group";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "permissionsNum";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "keywords";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").trim().replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A").replaceAll("\\\\", "%5C") + "\"," + eol);
      s = "permissions";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "num_items";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "preview";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "dateFormatted";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "sizeFormatted";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"" + eol);
      sb.append("}").append(eol);
      if (loops++ >= 10000) {
        loops = 0;
        Log.log("HTTP_SERVER", 0, "Processed " + x + " items in listing.");
      } 
    } 
    sb.append("]");
    return sb.toString();
  }
  
  public String getStatList(Properties listingProp) {
    Vector listing = (Vector)listingProp.remove("listing");
    StringBuffer sb = new StringBuffer();
    SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
    for (int x = 0; x < listing.size(); x++) {
      Properties lp = listing.elementAt(x);
      sb.append(String.valueOf(lp.getProperty("permissions")) + " " + lp.getProperty("num_items") + " " + lp.getProperty("owner") + " " + lp.getProperty("group") + " " + lp.getProperty("size") + " " + yyyyMMddHHmmss.format(new Date(Long.parseLong(lp.getProperty("modified")))) + " " + lp.getProperty("day") + " " + lp.getProperty("time_or_year") + " " + (String.valueOf(lp.getProperty("root_dir")) + lp.getProperty("name")).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C") + "\r\n");
    } 
    return "\r\n<listing>" + sb.toString() + "</listing>";
  }
  
  public String getDmzList(Properties listingProp) {
    Vector listing = (Vector)listingProp.remove("listing");
    StringBuffer sb = new StringBuffer();
    for (int x = 0; x < listing.size(); x++) {
      Properties lp = listing.elementAt(x);
      sb.append(formatDmzStat(lp)).append("\r\n");
    } 
    return "\r\n<listing>" + sb.toString() + "</listing>";
  }
  
  public String formatDmzStat(Properties lp) {
    Enumeration keys = lp.keys();
    String s = "";
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      String val = (new StringBuffer(String.valueOf(lp.getProperty(key)))).toString();
      s = String.valueOf(s) + key + "=" + Common.url_encode(val) + ";";
    } 
    return s;
  }
  
  public String getXmlListingResponse(String username, Properties request, String the_dir, boolean realPaths, VFS tmpVFS) throws Exception {
    Vector listing = null;
    Properties item = tmpVFS.get_item(the_dir);
    if (item == null)
      item = tmpVFS.get_item_parent(the_dir); 
    String privs = "";
    if (item != null)
      privs = item.getProperty("privs", ""); 
    Properties listingProp = new Properties();
    if (!this.thisSession.BG("DisallowListingDirectories")) {
      if (item != null && item.getProperty("url", "").indexOf("|") >= 0)
        if (item.getProperty("proxy_item", "false").equals("true")) {
          if (item.getProperty("permissions").charAt(2) != 'w')
            privs = Common.replace_str(privs, "(write)", ""); 
          if (item.getProperty("permissions").charAt(2) == 'w')
            privs = Common.replace_str(privs, "(read)", ""); 
        }  
      listingProp = getListingInfo(listing, the_dir, privs, false, tmpVFS, realPaths, true, this.thisSession, false);
      if (listingProp.get("listing") == null)
        Log.log("HTTP_SERVER", 2, "getXMLListing:Got listing of:" + ((Vector)listingProp.get("listing")).size()); 
    } 
    if (ServerStatus.server_settings.get("defaultStrings") != null && ServerStatus.server_settings.get("defaultStrings") instanceof Properties)
      listingProp.put("defaultStrings", ServerStatus.server_settings.get("defaultStrings")); 
    listingProp.put("site", this.thisSession.SG("site"));
    listingProp.put("quota", "");
    long quota = this.thisSession.get_quota(the_dir);
    if (quota != -12345L)
      listingProp.put("quota", Common.format_bytes_short(quota)); 
    String altList = "";
    if (request.getProperty("format", "").equalsIgnoreCase("JSON")) {
      altList = getJsonList(listingProp);
    } else if (request.getProperty("format", "").equalsIgnoreCase("JSONOBJ")) {
      altList = getJsonListObj(listingProp);
    } else if (request.getProperty("format", "").equalsIgnoreCase("STAT")) {
      altList = getStatList(listingProp);
    } else if (request.getProperty("format", "").equalsIgnoreCase("STAT_DMZ")) {
      altList = getDmzList(listingProp);
    } 
    if (request.getProperty("format", "").equalsIgnoreCase("JSONOBJ")) {
      Properties combinedPermissions = tmpVFS.getCombinedPermissions();
      boolean aclPermissions = combinedPermissions.getProperty("acl_permissions", "false").equals("true");
      privs = listingProp.getProperty("privs", "");
      if (aclPermissions)
        privs = tmpVFS.getPriv(the_dir, item); 
      String info = "{\r\n";
      info = String.valueOf(info) + "\t\"privs\" : \"" + privs.trim().replaceAll("\\r", "%0D").replaceAll("\\n", "%0A").replaceAll("\"", "%22").replaceAll("\t", "%09") + "\",\r\n";
      info = String.valueOf(info) + "\t\"path\" : \"" + listingProp.getProperty("path", "").trim().replaceAll("\"", "%22").replaceAll("\t", "%09") + "\",\r\n";
      info = String.valueOf(info) + "\t\"defaultStrings\" : \"" + listingProp.getProperty("defaultStrings", "").trim() + "\",\r\n";
      info = String.valueOf(info) + "\t\"site\" : \"" + listingProp.getProperty("site", "").trim() + "\",\r\n";
      info = String.valueOf(info) + "\t\"quota\" : \"" + listingProp.getProperty("quota", "").trim() + "\",\r\n";
      info = String.valueOf(info) + "\t\"listing\" : " + altList + "\r\n";
      info = String.valueOf(info) + "}\r\n";
      return info;
    } 
    String response = "";
    try {
      response = ServerStatus.thisObj.common_code.getXMLString(listingProp, "listingInfo", null);
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
    if (!altList.equals(""))
      response = String.valueOf(response.substring(0, response.indexOf("</privs>") + "</privs>".length())) + altList + response.substring(response.indexOf("</privs>") + "</privs>".length()); 
    return response;
  }
  
  public static Properties getListingInfo(Vector listing, String the_dir, String privs, boolean ignoreRootDir, VFS tmpVFS, boolean realPaths, boolean hideHidden, ServerSession thisSession, boolean allowDuplicates) {
    Vector items = new Vector();
    try {
      thisSession.date_time = ServerSession.updateDateCustomizations(thisSession.date_time, thisSession.user);
      if (listing == null) {
        listing = new Vector();
        Log.log("HTTP_SERVER", 3, "Getting dir listing for:" + thisSession.uiSG("user_name") + " with VFS from:" + tmpVFS);
        Log.log("HTTP_SERVER", 3, Thread.currentThread().getName());
        tmpVFS.getListing(listing, the_dir);
        if (listing.size() > 10000) {
          Log.log("HTTP_SERVER", 0, "Found " + listing.size() + " items in " + the_dir + ".");
        } else {
          Log.log("HTTP_SERVER", 3, "Found " + listing.size() + " items in " + the_dir + ".");
        } 
      } 
      Properties pp = new Properties();
      pp.put("listing", listing);
      thisSession.runPlugin("list", pp);
      Properties name_hash = new Properties();
      if (privs.toUpperCase().indexOf("(VIEW)") >= 0) {
        Log.log("HTTP_SERVER", 3, "Going through listing checking filters...");
        if (thisSession.check_access_privs(the_dir, "LIST"))
          for (int i = 0; i < listing.size(); i++) {
            Properties list_item = listing.elementAt(i);
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
                  list_item.put("sizeFormatted", Common.format_bytes(list_item.getProperty("size")));
                  list_item.put("date", String.valueOf(list_item.getProperty("month", "")) + " " + list_item.getProperty("day", "") + " " + list_item.getProperty("time_or_year", ""));
                  Date d = new Date(Long.parseLong(list_item.getProperty("modified", "0")));
                  if (d.getTime() < 30000000L) {
                    list_item.put("dateFormatted", "");
                  } else {
                    list_item.put("dateFormatted", thisSession.date_time.format(d));
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
    if (contentType.equals("application/binary"))
      write_command_http("Content-Disposition: attachment; filename=\"" + ((this.thisSessionHTTP.userAgent.toUpperCase().indexOf("MSIE") >= 0 || this.thisSessionHTTP.userAgent.toUpperCase().indexOf("TRIDENT") >= 0) ? Common.url_encode(fileName) : fileName) + "\""); 
    write_command_http("X-UA-Compatible: chrome=1");
    GenericClient c = this.thisSession.uVFS.getClient(item);
    Properties stat = null;
    try {
      stat = c.stat(otherFile.getPath());
    } finally {
      c = this.thisSession.uVFS.releaseClient(c);
    } 
    write_command_http("Last-Modified: " + this.thisSessionHTTP.sdf_rfc1123.format(new Date(Long.parseLong(stat.getProperty("modified")))));
    write_command_http("ETag: " + Long.parseLong(stat.getProperty("modified")));
    String amountEnd = stat.getProperty("size");
    for (int x = 0; x < byteRanges.size(); x++) {
      Properties pp = byteRanges.elementAt(x);
      if (pp.getProperty("end", "").equals(""))
        pp.put("end", amountEnd); 
    } 
    if (stat == null && otherFile.getName().toUpperCase().endsWith(".ZIP")) {
      Common.startMultiThreadZipper(this.thisSession.uVFS, this.thisSessionHTTP.retr, this.thisSession.uiSG("current_dir"), 2000, !(!item.getProperty("url").toUpperCase().startsWith("FTP:/") && !item.getProperty("url").toUpperCase().startsWith("HTTP:/")), new Vector());
      write_command_http("Connection: close");
      this.thisSessionHTTP.done = true;
    } else {
      long l = 0L;
      try {
        l = Long.parseLong(stat.getProperty("size"));
      } catch (Exception exception) {}
      if (byteRanges.size() == 1) {
        Properties pp = byteRanges.elementAt(0);
        write_command_http("Content-Range: bytes " + pp.getProperty("start") + "-" + (Long.parseLong(pp.getProperty("end")) - 1L) + "/" + l);
        long calculatedContentLength = Long.parseLong(pp.getProperty("end")) - Long.parseLong(pp.getProperty("start"));
        if (calculatedContentLength == 0L)
          calculatedContentLength = 1L; 
        write_command_http("Content-Length: " + calculatedContentLength);
      } else if (byteRanges.size() <= 1) {
        boolean ok = true;
        if (!this.thisSession.user.getProperty("filePublicEncryptionKey", "").equals("") || ServerStatus.BG("fileEncryption"))
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
    if (this.thisSessionHTTP.userAgent.toUpperCase().indexOf("MSIE") >= 0 || this.thisSessionHTTP.userAgent.toUpperCase().indexOf("TRIDENT") >= 0)
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
      this.thisSession.uiPUT("file_transfer_mode", "BINARY");
      this.thisSessionHTTP.retr.data_os = this.thisSessionHTTP.original_os;
      this.thisSessionHTTP.retr.httpDownload = true;
      String the_dir = this.thisSession.uiSG("current_dir");
      Properties ppp = new Properties();
      ppp.put("the_dir", the_dir);
      this.thisSession.runPlugin("transfer_path", ppp);
      the_dir = ppp.getProperty("the_dir", the_dir);
      this.thisSessionHTTP.retr.init_vars(the_dir, Long.parseLong(pp.getProperty("start")), Long.parseLong(pp.getProperty("end")) + 1L, this.thisSession, item, false, "", otherFile);
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
  
  public boolean buildPostItem(Properties request, long http_len_max, Vector headers) throws Exception {
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
      if (!this.thisSession.uiBG("user_logged_in"))
        return false; 
      if (write100Continue) {
        this.thisSessionHTTP.writeCookieAuth = false;
        write_command_http("HTTP/1.1 100 Continue");
        write_command_http("");
      } 
      String data0 = headers.elementAt(0).toString();
      data0 = data0.substring(data0.indexOf(" ") + 1, data0.lastIndexOf(" "));
      if (!data0.endsWith("/"))
        data0 = String.valueOf(data0) + "/"; 
      this.thisSession.uiPUT("current_dir", data0);
      this.thisSessionHTTP.setupSession();
      String the_dir = this.thisSession.uiSG("current_dir");
      if (!the_dir.startsWith(this.thisSession.SG("root_dir")))
        the_dir = String.valueOf(this.thisSession.SG("root_dir")) + the_dir.substring(1); 
      Properties site_item = this.thisSession.uVFS.get_item(the_dir);
      try {
        GenericClient genericClient = this.thisSession.uVFS.getClient(site_item);
        try {
          genericClient.doCommand("CWD " + this.thisSession.uiSG("current_dir"));
        } finally {
          genericClient = this.thisSession.uVFS.releaseClient(genericClient);
        } 
        getUserInfo("USERINFO", site_item, the_dir);
      } catch (Exception e) {
        Log.log("HTTP_SERVER", 2, e);
      } 
      this.thisSession.uiPUT("current_dir", String.valueOf(the_dir) + as2Info.getProperty("as2Filename"));
      Object inData = null;
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      this.thisSessionHTTP.doPutFile(http_len_max, this.thisSessionHTTP.done, headers, baos, String.valueOf(the_dir) + as2Info.getProperty("as2Filename"), false, 0L);
      inData = baos.toByteArray();
      String tmpFilename = String.valueOf(System.currentTimeMillis()) + ".as2dump";
      if (Log.log("HTTP_SERVER", 4, "Raw File Data Dumped to disk:" + tmpFilename)) {
        RandomAccessFile tmp = new RandomAccessFile(tmpFilename, "rw");
        tmp.write((byte[])inData);
        tmp.close();
      } 
      this.thisSessionHTTP.done = true;
      this.thisSessionHTTP.keepGoing = false;
      As2Msg m = new As2Msg();
      Properties user = this.thisSessionHTTP.thisSession.user;
      Properties info = new Properties();
      Properties session = (Properties)SharedSession.find("crushftp.sessions").get(this.thisSession.getId());
      String keystorePass = user.getProperty("as2EncryptKeystorePassword", session.getProperty("as2EncryptKeystorePassword", ""));
      String keyPass = user.getProperty("as2EncryptKeystorePassword", session.getProperty("as2EncryptKeystorePassword", ""));
      if (user.getProperty("userVersion", "4").equals("4") || user.getProperty("userVersion", "4").equals("5")) {
        keystorePass = ServerStatus.thisObj.common_code.encode_pass(keystorePass, "DES");
        keyPass = ServerStatus.thisObj.common_code.encode_pass(keyPass, "DES");
      } 
      Object outData = m.decryptData(info, inData, as2Info.getProperty("contentType"), user.getProperty("as2EncryptKeystoreFormat", session.getProperty("as2EncryptKeystoreFormat", "PKCS12")), user.getProperty("as2EncryptKeystorePath", session.getProperty("as2EncryptKeystorePath", ".keystore")), keystorePass, keyPass, user.getProperty("as2EncryptKeyAlias", session.getProperty("as2EncryptKeyAlias", "")));
      info.put("content-disposition", as2Info.getProperty("content-disposition", ""));
      Vector payloads = m.getPayloadsAndMic(info, outData);
      String messageId = as2Info.getProperty("message-id", "-NONE-");
      messageId = messageId.substring(1, messageId.length() - 1);
      String mdn = null;
      String boundary = "";
      boolean mdnResponse = false;
      String contentType = as2Info.getProperty("contentType");
      if (as2Info.getProperty("contentType").toLowerCase().indexOf("disposition-notification") >= 0 || info.getProperty("contentType", "").toLowerCase().indexOf("disposition-notification") >= 0 || as2Info.getProperty("subject", "").toUpperCase().indexOf("DELIVERY NOTIFICATION") >= 0) {
        mdnResponse = true;
        if (contentType.toLowerCase().indexOf("disposition-notification") < 0 && info.getProperty("contentType", "").toLowerCase().indexOf("disposition-notification") >= 0)
          contentType = info.getProperty("contentType", ""); 
      } else {
        Log.log("AS2_SERVER", 1, "AS2:Payloads:" + payloads.size() + ":" + payloads);
        Log.log("AS2_SERVER", 1, "AS2:info:" + info);
        Log.log("AS2_SERVER", 2, "AS2:as2Info:" + as2Info);
        mdn = m.createMDN(info.getProperty("mic", ""), as2Info.getProperty("signMdn", "false").equals("true"), as2Info.getProperty("as2-to", ""), messageId, "automatic-action/MDN-sent-automatically; processed", "Received AS2 message.", user.getProperty("as2EncryptKeystoreFormat", session.getProperty("as2EncryptKeystoreFormat", "PKCS12")), user.getProperty("as2EncryptKeystorePath", session.getProperty("as2EncryptKeystorePath", ".keystore")), keystorePass, keyPass, user.getProperty("as2EncryptKeyAlias", session.getProperty("as2EncryptKeyAlias", "")));
        Log.log("AS2_SERVER", 1, "AS2:MDN:" + mdn);
        BufferedReader sr = new BufferedReader(new StringReader(mdn));
        while (boundary.equals(""))
          boundary = sr.readLine().trim(); 
        sr.close();
      } 
      for (int i = 0; i < payloads.size(); i++) {
        boolean ok = false;
        if (this.thisSession.check_access_privs(this.thisSession.uiSG("current_dir"), "STOR") && Common.filter_check("U", Common.last(this.thisSession.uiSG("current_dir")), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + this.thisSession.SG("file_filter")))
          ok = true; 
        if (ok) {
          Properties uploadStat1 = null;
          Properties uploadStat2 = null;
          Object o = payloads.elementAt(i);
          if (o instanceof File) {
            File f = (File)o;
            this.thisSession.uiPUT("current_dir", String.valueOf(Common.all_but_last(this.thisSession.uiSG("current_dir"))) + f.getName());
            OutputStream of_stream = this.thisSessionHTTP.getStorOutputStream(String.valueOf(Common.all_but_last(this.thisSession.uiSG("current_dir"))) + f.getName(), 0L, false);
            if (this.thisSessionHTTP.stor != null)
              try {
                while (!this.thisSessionHTTP.stor.active)
                  Thread.sleep(100L); 
              } catch (Exception e) {
                Log.log("HTTP_SERVER", 1, e);
              }  
            Common.copyStreams(new FileInputStream(f), of_stream, true, true);
            while (this.thisSessionHTTP.stor != null && this.thisSessionHTTP.stor.active)
              Thread.sleep(100L); 
            uploadStat1 = this.thisSession.uiPG("lastUploadStat");
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
                Log.log("HTTP_SERVER", 1, e);
              } 
            } 
            String originalDir = Common.all_but_last(this.thisSession.uiSG("current_dir"));
            String filename = String.valueOf(originalDir) + payload.getProperty("name", "");
            if (filename.endsWith(".") && ext.startsWith(".")) {
              filename = String.valueOf(filename) + ext.substring(1);
            } else {
              filename = String.valueOf(filename) + ext;
            } 
            this.thisSession.uiPUT("current_dir", filename);
            OutputStream of_stream = this.thisSessionHTTP.getStorOutputStream(filename, 0L, false);
            while (this.thisSessionHTTP.stor != null && !this.thisSessionHTTP.stor.active)
              Thread.sleep(100L); 
            Common.copyStreams(new ByteArrayInputStream(b), of_stream, true, true);
            while (this.thisSessionHTTP.stor != null && this.thisSessionHTTP.stor.active)
              Thread.sleep(100L); 
            try {
              if (mdnInfo != null) {
                mdnInfo.put("b", b);
                mdnInfo.put("item", this.thisSession.uVFS.get_item(filename));
                As2Msg.mdnResponses.put(mdnInfo.getProperty("Original-Message-ID".toLowerCase()), mdnInfo);
                Thread.sleep(5000L);
                As2Msg.mdnResponses.remove(mdnInfo.getProperty("Original-Message-ID".toLowerCase()));
              } 
            } catch (Exception e) {
              Log.log("HTTP_SERVER", 1, e);
            } 
            uploadStat1 = this.thisSession.uiPG("lastUploadStat");
            if (!mdnResponse) {
              filename = String.valueOf(originalDir) + messageId.replace('<', '_').replace('>', '_').replace('/', '_') + ".out.mdn";
              this.thisSession.uiPUT("current_dir", filename);
              of_stream = this.thisSessionHTTP.getStorOutputStream(filename, 0L, false);
              while (this.thisSessionHTTP.stor != null && !this.thisSessionHTTP.stor.active)
                Thread.sleep(100L); 
              Common.copyStreams(new ByteArrayInputStream(mdn.getBytes("UTF8")), of_stream, true, true);
              while (this.thisSessionHTTP.stor != null && this.thisSessionHTTP.stor.active)
                Thread.sleep(100L); 
              uploadStat2 = this.thisSession.uiPG("lastUploadStat");
            } 
          } 
          try {
            if (uploadStat1 != null)
              uploadStat1.putAll(as2Info); 
            if (uploadStat2 != null)
              uploadStat2.putAll(as2Info); 
            if (this.thisSession.uiPG("lastUploadStat") == null) {
              this.thisSession.user_info.putAll(as2Info);
              write_command_http("HTTP/1.1 500 Error");
              write_command_http("Pragma: no-cache");
              this.thisSessionHTTP.write_standard_headers();
              write_command_http("Content-Length: " + ("file transfer error".length() + 2));
              write_command_http("");
              write_command_http("file transfer error");
              return false;
            } 
          } catch (Exception e) {
            Log.log("HTTP_SERVER", 0, e);
          } 
        } 
      } 
      GenericClient c = this.thisSession.uVFS.getClient(site_item);
      try {
        c.doCommand("SITE ACK");
      } finally {
        c = this.thisSession.uVFS.releaseClient(c);
      } 
      this.thisSession.uVFS.disconnect();
      this.thisSession.uVFS.reset();
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
          write_command_http("Content-Type: multipart/signed; boundary=\"" + boundary.substring(2) + "\"; protocol=\"application/pkcs7-signature\"; report-type=disposition-notification; micalg=SHA1; charset=utf-8");
        } else {
          write_command_http("Content-Type: multipart/report; boundary=\"" + boundary.substring(2) + "\"; report-type=disposition-notification; micalg=SHA1; charset=utf-8");
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
          String results = m.doAsyncMDNPost("", "", as2Info, false, mdn, boundary, destUrl, user.getProperty("as2EncryptKeystorePath", session.getProperty("as2EncryptKeystorePath", ".keystore")), keystorePass, keyPass, true);
          this.thisSession.add_log("[" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] DATA: *" + results.trim() + "*", "HTTP");
        } 
      } 
      if (outData instanceof File) {
        int loops = 0;
        while (!((File)outData).delete() && loops++ < 10)
          Thread.sleep(100L); 
      } 
      return false;
    } 
    if (http_len_max < 10240000L) {
      if (as2Info.getProperty("content-type", "").indexOf("multipart") >= 0) {
        String boundary = as2Info.getProperty("content-type", "").substring(as2Info.getProperty("content-type", "").toUpperCase().indexOf("BOUNDARY=") + "BOUNDARY=".length()).trim();
        Vector items = this.thisSessionHTTP.parsePostArguments(boundary, http_len_max, false);
        for (int i = 0; i < items.size(); i++) {
          Properties pp = items.elementAt(i);
          request.putAll(pp);
        } 
      } else {
        String postData = this.thisSessionHTTP.get_raw_http_command((int)http_len_max);
        String[] postItems = postData.split("&");
        request.put("type", "text");
        boolean noResult = false;
        for (int i = 0; i < postItems.length; i++) {
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
          if (noResult && name.equals("result")) {
            this.thisSession.add_log_formatted(String.valueOf(name) + ": len=" + data_item.length(), "POST");
          } else {
            this.thisSession.add_log_formatted(String.valueOf(name) + ":" + data_item, "POST");
          } 
        } 
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
    String vfsRootItem = this.thisSession.uVFS.getRootVFS(the_dir, -1);
    if (the_dir.startsWith(vfsRootItem))
      the_dir = the_dir.substring(vfsRootItem.length() - 1); 
    if ((new VRL(site_item.getProperty("url"))).getProtocol().equalsIgnoreCase("FTP")) {
      GenericClient c = this.thisSession.uVFS.getClient(site_item);
      try {
        String linesStr = c.doCommand("SITE " + command + " " + the_dir);
        if (linesStr != null)
          lines = linesStr.substring(4).split(";;;"); 
        Properties session = (Properties)SharedSession.find("crushftp.sessions").get(this.thisSession.getId());
        Properties extraCustomizations = (Properties)session.get("extraCustomizations");
        if (extraCustomizations == null)
          extraCustomizations = new Properties(); 
        for (int x = 0; x < lines.length; x++) {
          String key = lines[x].substring(0, lines[x].indexOf("=")).trim();
          String val = lines[x].substring(lines[x].indexOf("=") + 1).trim();
          extraCustomizations.put(key, val);
        } 
        session.put("extraCustomizations", extraCustomizations);
      } finally {
        c = this.thisSession.uVFS.releaseClient(c);
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
    return writeResponse(response, true, 200, true, false);
  }
  
  public boolean writeResponse(String response, boolean json) throws Exception {
    return writeResponse(response, true, 200, true, json);
  }
  
  public boolean writeResponse(String response, boolean log, int code, boolean convertVars, boolean json) throws Exception {
    boolean acceptsGZIP = (ServerStatus.BG("allow_gzip") && this.thisSessionHTTP.headerLookup.getProperty("Accept-Encoding".toUpperCase(), "").toUpperCase().indexOf("GZIP") >= 0);
    return writeResponse(response, log, code, convertVars, json, acceptsGZIP);
  }
  
  public boolean writeResponse(String response, boolean log, int code, boolean convertVars, boolean json, boolean acceptsGZIP) throws Exception {
    if (convertVars)
      response = ServerStatus.thisObj.change_vars_to_values(response, this.thisSession); 
    write_command_http("HTTP/1.1 " + code + " OK");
    write_command_http("Cache-Control: no-store");
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
      if (!ServerStatus.SG("Access-Control-Allow-Origin").equals(""))
        for (int x = 0; x < (ServerStatus.SG("Access-Control-Allow-Origin").split(",")).length; x++)
          this.thisSessionHTTP.write_command_http("Access-Control-Allow-Origin: " + ServerStatus.SG("Access-Control-Allow-Origin").split(",")[x].trim());  
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
      write_command_http("Content-Length: " + len);
      write_command_http("", log);
      if (len > 0)
        this.thisSessionHTTP.write_command_http(response, log, convertVars); 
    } 
    return true;
  }
  
  public static String processKeywords(ServerSession thisSession, String[] names, String keywords_raw) throws Exception {
    String response = "";
    String keyword = keywords_raw.trim();
    response = String.valueOf(response) + "<commandResult><response>";
    boolean ok = false;
    for (int x = 0; x < names.length; x++) {
      String the_dir = Common.url_decode(Common.all_but_last(names[x]));
      if (the_dir.startsWith(thisSession.SG("root_dir")))
        the_dir = the_dir.substring(thisSession.SG("root_dir").length() - 1); 
      thisSession.setupCurrentDir(the_dir);
      Properties item = thisSession.uVFS.get_item(String.valueOf(thisSession.uiSG("current_dir")) + Common.last(names[x]));
      the_dir = SearchHandler.getPreviewPath(item, "1", 1);
      String index = String.valueOf(ServerStatus.SG("previews_path")) + the_dir.substring(1);
      (new File(Common.all_but_last(index))).mkdirs();
      if (ServerStatus.BG("exif_keywords")) {
        String srcFile = Common.dots((new VRL(item.getProperty("url"))).getPath());
        for (int xx = 0; xx < ServerStatus.thisObj.previewWorkers.size(); xx++) {
          PreviewWorker preview = ServerStatus.thisObj.previewWorkers.elementAt(xx);
          if (preview.prefs.getProperty("preview_enabled", "false").equalsIgnoreCase("true") && preview.checkExtension(Common.last(the_dir), new File(srcFile))) {
            preview.setExifInfo(srcFile, PreviewWorker.getDestPath(String.valueOf(srcFile) + "/p1/"), "keywords", Common.url_decode(keywords_raw).trim());
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
  
  public Properties urlDecodePost(Properties p) {
    Enumeration keys = p.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      if (p.get(key) instanceof String)
        p.put(key, Common.url_decode(p.get(key).toString().replace('+', ' '))); 
    } 
    return p;
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
        button.put("value", "/WebInterface/login.html"); 
      if (button.getProperty("value").indexOf("Logout") >= 0)
        button.put("value", "javascript:doLogout();"); 
    } 
  }
  
  public static String createShare(Vector path_items, Properties request, Vector web_customizations, String user_name, String linkedServer, Properties user, SimpleDateFormat date_time) throws Exception {
    if (date_time == null)
      date_time = new SimpleDateFormat("MM/dd/yy"); 
    String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
    String[] paths = (String[])null;
    if (request.getProperty("paths").indexOf(";") >= 0) {
      paths = Common.url_decode(request.getProperty("paths")).split(";");
    } else {
      paths = Common.url_decode(request.getProperty("paths")).split("\r\n");
    } 
    String tempUsername = Common.makeBoundary(ServerStatus.IG("temp_accounts_length"));
    String tempPassword = Common.makeBoundary(ServerStatus.IG("temp_accounts_length"));
    SimpleDateFormat ex1 = new SimpleDateFormat("MM/dd/yyyy HH:mm");
    SimpleDateFormat ex2 = new SimpleDateFormat("MMddyyyyHHmm");
    String expire_date = ex2.format(ex1.parse(request.getProperty("expire").replace('+', ' ')));
    String folderName = "u=" + tempUsername + ",,p=" + tempPassword + ",,m=" + user_name + ",,t=TempAccount,,ex=" + expire_date;
    String shareToDomain = "";
    String shareFromDomain = "";
    int maxLen = 255;
    if (Common.machine_is_windows())
      maxLen -= (new File(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/")).getCanonicalPath().length(); 
    if (web_customizations != null)
      for (int i = 0; i < web_customizations.size(); i++) {
        Properties cust = web_customizations.elementAt(i);
        if (cust.getProperty("key").equals("shareToDomain"))
          shareToDomain = cust.getProperty("value"); 
        if (cust.getProperty("key").equals("shareFromDomain"))
          shareFromDomain = cust.getProperty("value"); 
      }  
    String userHome = String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + folderName + "/";
    String userStorage = String.valueOf(ServerStatus.SG("temp_accounts_path")) + "storage/" + tempUsername + tempPassword + "/";
    Properties permissions = new Properties();
    permissions.put("/", "(read)(view)(resume)(slideshow)");
    String msg = "";
    String webLink = String.valueOf(Common.url_decode(request.getProperty("baseUrl"))) + "?u=" + tempUsername + "&p=" + tempPassword;
    if (request.getProperty("direct_link", "false").equalsIgnoreCase("true") && paths.length == 1 && !paths[0].trim().endsWith("/"))
      webLink = String.valueOf(webLink) + "&path=/" + Common.url_encode(Common.last(paths[0])); 
    request.put("master", user_name);
    request.put("created", ex1.format(new Date()));
    request.put("username", tempUsername);
    request.put("password", tempPassword);
    request.put("web_link", webLink);
    if (!shareToDomain.equals("")) {
      String emails = String.valueOf(request.getProperty("emailTo")) + "," + request.getProperty("emailCc") + "," + request.getProperty("emailBcc");
      String[] tos = emails.replace('+', ' ').trim().replace(';', ',').split(",");
      boolean ok = true;
      for (int i = 0; i < tos.length && ok; i++) {
        String to = tos[i];
        if (!to.trim().equals("") && 
          !to.toUpperCase().endsWith(shareToDomain.toUpperCase()))
          ok = false; 
      } 
      if (!ok)
        msg = "The To, Cc, or Bcc email does not end with the required domain: " + shareToDomain; 
    } 
    if (!shareFromDomain.equals("")) {
      String from = request.getProperty("emailFrom").replace('+', ' ').trim();
      if (!from.toUpperCase().endsWith(shareFromDomain.toUpperCase()))
        msg = "The FROM: email does not end with the required domain: " + shareFromDomain + "."; 
    } 
    String last_name = "";
    Vector files = new Vector();
    for (int x = 0; x < path_items.size() && msg.equals(""); x++) {
      Properties stat = path_items.elementAt(x);
      VRL vrl = new VRL(stat.getProperty("url"));
      String privs = stat.getProperty("privs", "(read)(share)(delete)(view)").toLowerCase();
      if (privs.indexOf("(share)") < 0)
        msg = "Not allowed to share these files:" + stat.getProperty("root_dir") + stat.getProperty("name") + "\r\n"; 
      if (request.getProperty("shareUsername", "false").equalsIgnoreCase("true")) {
        if (ServerStatus.siIG("enterprise_level") <= 0) {
          msg = String.valueOf(msg) + "The server does not have an enterprise license, so sharing to usernames is not allowed.\r\n";
        } else {
          String[] shareUsernames = Common.url_decode(request.getProperty("shareUsernames").replace('+', ' ').trim()).split(",");
          for (int xx = 0; xx < shareUsernames.length; xx++) {
            String toUser = String.valueOf(shareUsernames[xx].trim()) + ".SHARED";
            toUser = toUser.replace('/', '_').replace('\\', '_').replace('%', '_').replace(':', '_').replace(';', '_');
            VFS sharedVFS = null;
            try {
              if (!UserTools.ut.getUser(linkedServer, toUser, false).getProperty("user_name").equals(toUser))
                throw new NullPointerException(); 
              sharedVFS = UserTools.ut.getVFS(linkedServer, toUser);
            } catch (NullPointerException e) {
              Properties sharedUser = new Properties();
              sharedUser.put("password", "");
              sharedUser.put("version", "1.0");
              sharedUser.put("root_dir", "/");
              sharedUser.put("userVersion", "5");
              sharedUser.put("max_logins", "-1");
              UserTools.writeUser(linkedServer, toUser, sharedUser);
              sharedVFS = UserTools.ut.getVFS(linkedServer, toUser);
            } 
            UserTools.addPriv(linkedServer, toUser, "/Shares/", "(view)(read)", 0, sharedVFS);
            String sPrivs1 = request.getProperty("shareUsernamePermissions").toLowerCase();
            String sPrivs2 = "";
            String[] priv_parts = stat.getProperty("privs").toLowerCase().split("\\(");
            for (int loop = 0; loop < priv_parts.length; loop++) {
              String priv = priv_parts[loop];
              priv = priv.trim();
              priv = priv.substring(0, priv.lastIndexOf(")"));
              if (!priv.equals("") && sPrivs1.indexOf("(" + priv + ")") >= 0)
                sPrivs2 = String.valueOf(sPrivs2) + "(" + priv + ")"; 
            } 
            Log.log("HTTP_SERVER", 2, "Requested privs:" + sPrivs1);
            Log.log("HTTP_SERVER", 2, "Adding privs:" + sPrivs2);
            Log.log("HTTP_SERVER", 2, "Adding priv to path:/Shares/" + user_name + "/" + stat.getProperty("name"));
            UserTools.addPriv(linkedServer, toUser, "/Shares/" + user_name + "/" + stat.getProperty("name") + (stat.getProperty("type", "DIR").equalsIgnoreCase("DIR") ? "/" : ""), sPrivs2, 0, sharedVFS);
            Properties existingItem = sharedVFS.get_item("/Shares/" + user_name + "/" + stat.getProperty("name"));
            if (existingItem == null || !existingItem.getProperty("name").equals(stat.getProperty("name"))) {
              UserTools.ut.addFolder(linkedServer, toUser, "/", "Shares");
              UserTools.ut.addFolder(linkedServer, toUser, "/Shares/", user_name);
              UserTools.addItem(linkedServer, toUser, "/Shares/" + user_name + "/", stat.getProperty("name"), stat.getProperty("url"), stat.getProperty("type"), new Properties(), false, "");
            } else {
              msg = String.valueOf(msg) + "An item with this name has already been shared to " + shareUsernames[xx].trim() + " : " + stat.getProperty("name") + ".  \r\n";
            } 
          } 
        } 
        request.put("sendEmail", "false");
        request.put("publishType", "directShare");
      } 
      if (!msg.equals(""))
        break; 
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
      } 
      if (request.getProperty("publishType").equalsIgnoreCase("copy")) {
        if (privs.indexOf("(read)") >= 0) {
          (new File(String.valueOf(userHome) + "VFS/")).mkdirs();
          (new File(userStorage)).mkdirs();
          String itemName = vrl.getName(), itemName = itemName;
          if (itemName.endsWith("/"))
            itemName = itemName.substring(0, itemName.length() - 1); 
          if (request.getProperty("allowUploads", "false").equals("true"))
            permissions.put("/" + itemName.toUpperCase() + "/", "(read)(view)(resume)(write)(delete)(slideshow)(rename)(makedir)(deletedir)"); 
          try {
            ServerStatus.thisObj.common_code.writeXMLObject(String.valueOf(userHome) + "VFS.XML", permissions, "VFS");
          } catch (Exception exception) {}
          try {
            ServerStatus.thisObj.common_code.writeXMLObject(String.valueOf(userHome) + "INFO.XML", request, "INFO");
          } catch (Exception exception) {}
          Common.recurseCopyThreaded(vrl.getPath(), String.valueOf(userStorage) + vrl.getName() + (stat.getProperty("type", "FILE").equalsIgnoreCase("DIR") ? "/" : ""), true, false);
          Properties vItem = new Properties();
          vItem.put("url", String.valueOf((new File(String.valueOf(userStorage) + vrl.getName())).toURI().toURL().toExternalForm()) + (stat.getProperty("type", "FILE").equalsIgnoreCase("DIR") ? "/" : ""));
          vItem.put("type", stat.getProperty("type", "FILE").toLowerCase());
          Vector v = new Vector();
          v.addElement(vItem);
          try {
            ServerStatus.thisObj.common_code.writeXMLObject(String.valueOf(userHome) + "VFS/" + itemName, v, "VFS");
          } catch (Exception exception) {}
        } else {
          msg = "Not allowed to read from this location.";
        } 
      } else if (request.getProperty("publishType").equalsIgnoreCase("move")) {
        if (privs.indexOf("(read)") >= 0 && privs.indexOf("(delete)") >= 0) {
          (new File(String.valueOf(userHome) + "VFS/")).mkdirs();
          (new File(userStorage)).mkdirs();
          String itemName = "storage";
          if (paths.length > 1 || stat.getProperty("type").equalsIgnoreCase("FILE"))
            itemName = vrl.getName(); 
          if (itemName.endsWith("/"))
            itemName = itemName.substring(0, itemName.length() - 1); 
          if (request.getProperty("allowUploads", "false").equals("true"))
            permissions.put("/" + itemName.toUpperCase() + "/", "(read)(view)(resume)(write)(delete)(slideshow)(rename)(makedir)(deletedir)"); 
          try {
            ServerStatus.thisObj.common_code.writeXMLObject(String.valueOf(userHome) + "VFS.XML", permissions, "VFS");
          } catch (Exception exception) {}
          try {
            ServerStatus.thisObj.common_code.writeXMLObject(String.valueOf(userHome) + "INFO.XML", request, "INFO");
          } catch (Exception exception) {}
          Common.recurseCopyThreaded(vrl.getPath(), String.valueOf(userStorage) + vrl.getName(), true, true);
          Properties vItem = new Properties();
          vItem.put("url", (new File(String.valueOf(userStorage) + vrl.getName())).toURI().toURL().toExternalForm());
          vItem.put("type", stat.getProperty("type").toLowerCase());
          Vector v = new Vector();
          v.addElement(vItem);
          try {
            ServerStatus.thisObj.common_code.writeXMLObject(String.valueOf(userHome) + "VFS/" + itemName, v, "VFS");
          } catch (Exception exception) {}
        } else {
          msg = "Not allowed to read and delete from this location.";
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
            String privs3 = "read,view,resume,write,delete,slideshow,rename,makedir,deletedir";
            for (int xx = 0; xx < (privs3.split(",")).length; xx++) {
              String s = "(" + privs3.split(",")[xx] + ")";
              if (privs.indexOf(s) >= 0)
                privs2 = String.valueOf(privs2) + s; 
            } 
            permissions.put("/" + itemName.toUpperCase() + "/", privs2);
          } 
          try {
            ServerStatus.thisObj.common_code.writeXMLObject(String.valueOf(userHome) + "VFS.XML", permissions, "VFS");
          } catch (Exception exception) {}
          try {
            ServerStatus.thisObj.common_code.writeXMLObject(String.valueOf(userHome) + "INFO.XML", request, "INFO");
          } catch (Exception exception) {}
          Properties vItem = new Properties();
          vItem.put("url", vrl.toString());
          vItem.put("type", "FILE");
          Vector v = new Vector();
          v.addElement(vItem);
          try {
            ServerStatus.thisObj.common_code.writeXMLObject(String.valueOf(userHome) + "VFS/" + itemName, v, "VFS");
          } catch (Exception exception) {}
        } else {
          msg = "Not allowed to read from this location.";
        } 
      } 
      if (!msg.equals(""))
        break; 
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
    if (msg.equals("")) {
      File[] files2 = (File[])null;
      if (files.size() > 0)
        files2 = new File[files.size()]; 
      for (int i = 0; i < files.size(); i++)
        files2[i] = files.elementAt(i); 
      String emailFrom = Common.url_decode(request.getProperty("emailFrom").replace('+', ' ').trim());
      String emailTo = Common.url_decode(request.getProperty("emailTo").replace('+', ' ').trim()).replace(';', ',');
      String emailCc = Common.url_decode(request.getProperty("emailCc").replace('+', ' ').trim()).replace(';', ',');
      String emailBcc = Common.url_decode(request.getProperty("emailBcc").replace('+', ' ').trim()).replace(';', ',');
      String emailSubject = Common.url_decode(request.getProperty("emailSubject").replace('+', ' ').trim());
      String emailBody = String.valueOf(Common.url_decode(request.getProperty("emailBody"))) + "\r\n\r\n";
      int j;
      for (j = 0; j < 100; j++) {
        String s = "";
        if ((paths[0].split("/")).length > j)
          s = paths[0].split("/")[j]; 
        emailBody = Common.replace_str(emailBody, "{" + j + "path}", s);
      } 
      for (j = 0; j < 100; j++) {
        String s = "";
        int m = (paths[0].split("/")).length - 1 - j;
        if (m >= 0)
          s = paths[0].split("/")[m]; 
        emailBody = Common.replace_str(emailBody, "{path" + j + "}", s);
      } 
      SimpleDateFormat sdf_time = new SimpleDateFormat("HH:mm");
      Date d = ex2.parse(expire_date);
      while (emailBody.indexOf("<LINE>") >= 0 && emailBody.indexOf("</LINE>") >= 0) {
        String line = emailBody.substring(emailBody.indexOf("<LINE>") + "<LINE>".length(), emailBody.indexOf("</LINE>"));
        String lines = "";
        for (int m = 0; m < paths.length; m++) {
          String line2 = line;
          if (!paths[m].trim().equals("")) {
            line2 = Common.replace_str(line2, "{web_link}", webLink);
            line2 = Common.replace_str(line2, "{username}", tempUsername);
            line2 = Common.replace_str(line2, "{password}", tempPassword);
            line2 = Common.replace_str(line2, "{user}", tempUsername);
            line2 = Common.replace_str(line2, "{pass}", tempPassword);
            line2 = Common.replace_str(line2, "{url}", request.getProperty("baseUrl"));
            line2 = Common.replace_str(line2, "{to}", emailTo);
            line2 = Common.replace_str(line2, "{from}", emailFrom);
            line2 = Common.replace_str(line2, "{cc}", emailCc);
            line2 = Common.replace_str(line2, "{bcc}", emailBcc);
            line2 = Common.replace_str(line2, "{subject}", emailSubject);
            line2 = Common.replace_str(line2, "{paths}", Common.url_decode(request.getProperty("paths")));
            line2 = Common.replace_str(line2, "{path}", Common.all_but_last(paths[m].trim()));
            line2 = Common.replace_str(line2, "{name}", Common.last(paths[m].trim()));
            line2 = Common.replace_str(line2, "{datetime}", ex1.format(d));
            line2 = Common.replace_str(line2, "{date}", date_time.format(d));
            line2 = Common.replace_str(line2, "{time}", sdf_time.format(d));
            lines = String.valueOf(lines) + line2;
          } 
        } 
        emailBody = Common.replace_str(emailBody, "<LINE>" + line + "</LINE>", lines);
      } 
      emailBody = Common.replace_str(emailBody, "<web_link>", webLink);
      emailBody = Common.replace_str(emailBody, "<username>", tempUsername);
      emailBody = Common.replace_str(emailBody, "<password>", tempPassword);
      emailBody = Common.replace_str(emailBody, "%user%", tempUsername);
      emailBody = Common.replace_str(emailBody, "%pass%", tempPassword);
      emailBody = Common.replace_str(emailBody, "{user}", tempUsername);
      emailBody = Common.replace_str(emailBody, "{pass}", tempPassword);
      emailBody = Common.replace_str(emailBody, "<url>", request.getProperty("baseUrl"));
      emailBody = Common.replace_str(emailBody, "{web_link}", webLink);
      emailBody = Common.replace_str(emailBody, "{username}", tempUsername);
      emailBody = Common.replace_str(emailBody, "{password}", tempPassword);
      emailBody = Common.replace_str(emailBody, "{url}", request.getProperty("baseUrl"));
      emailBody = Common.replace_str(emailBody, "{to}", emailTo);
      emailBody = Common.replace_str(emailBody, "{from}", emailFrom);
      emailBody = Common.replace_str(emailBody, "{cc}", emailCc);
      emailBody = Common.replace_str(emailBody, "{bcc}", emailBcc);
      emailBody = Common.replace_str(emailBody, "{subject}", emailSubject);
      emailBody = Common.replace_str(emailBody, "{paths}", Common.url_decode(request.getProperty("paths")));
      emailBody = Common.replace_str(emailBody, "{name}", last_name);
      int k;
      for (k = 0; k < 100; k++) {
        String s = "";
        if ((paths[0].split("/")).length > k)
          s = paths[0].split("/")[k]; 
        emailSubject = Common.replace_str(emailSubject, "{" + k + "path}", s);
      } 
      for (k = 0; k < 100; k++) {
        String s = "";
        int m = (paths[0].split("/")).length - 1 - k;
        if (m >= 0)
          s = paths[0].split("/")[m]; 
        emailSubject = Common.replace_str(emailSubject, "{path" + k + "}", s);
      } 
      emailSubject = Common.replace_str(emailSubject, "{username}", tempUsername);
      emailSubject = Common.replace_str(emailSubject, "{password}", tempPassword);
      emailSubject = Common.replace_str(emailSubject, "{web_link}", webLink);
      emailSubject = Common.replace_str(emailSubject, "{to}", emailTo);
      emailSubject = Common.replace_str(emailSubject, "{from}", emailFrom);
      emailSubject = Common.replace_str(emailSubject, "{cc}", emailCc);
      emailSubject = Common.replace_str(emailSubject, "{bcc}", emailBcc);
      try {
        if (user != null)
          date_time = ServerSession.updateDateCustomizations(date_time, user); 
        emailBody = Common.replace_str(emailBody, "<datetime>", ex1.format(d));
        emailBody = Common.replace_str(emailBody, "<date>", date_time.format(d));
        emailBody = Common.replace_str(emailBody, "<time>", sdf_time.format(d));
        emailBody = Common.replace_str(emailBody, "{datetime}", ex1.format(d));
        emailBody = Common.replace_str(emailBody, "{date}", date_time.format(d));
        emailBody = Common.replace_str(emailBody, "{time}", sdf_time.format(d));
      } catch (Exception e) {
        Log.log("HTTP_SERVER", 1, e);
      } 
      if (request.getProperty("sendEmail", "").equals("true")) {
        Common.send_mail(ServerStatus.SG("discovered_ip"), emailTo, emailCc, emailBcc, emailFrom, emailSubject, emailBody, ServerStatus.SG("smtp_server"), ServerStatus.SG("smtp_user"), ServerStatus.SG("smtp_pass"), ServerStatus.SG("smtp_ssl").equals("true"), ServerStatus.SG("smtp_html").equals("true"), files2);
        msg = "{share_complete}  {share_email_sent}. &nbsp;&nbsp;&nbsp;";
      } else {
        msg = "{share_complete} &nbsp;&nbsp;&nbsp;";
      } 
      if (request.getProperty("shareUsername", "false").equalsIgnoreCase("false")) {
        msg = String.valueOf(msg) + "<a href=\"mailto:" + emailTo + "?ignore=false";
        if (!emailCc.trim().equals(""))
          msg = String.valueOf(msg) + "&cc=" + emailCc; 
        if (!emailBcc.trim().equals(""))
          msg = String.valueOf(msg) + "&bcc=" + emailBcc; 
        if (!emailSubject.trim().equals(""))
          msg = String.valueOf(msg) + "&subject=" + Common.url_encode(emailSubject); 
        if (!emailBody.trim().equals(""))
          msg = String.valueOf(msg) + "&body=" + Common.url_encode(emailBody); 
        msg = String.valueOf(msg) + "\">{share_open_in_email_client}</a>";
      } else {
        webLink = "";
        tempUsername = "";
        tempPassword = "";
      } 
    } else if (request.getProperty("shareUsername", "false").equalsIgnoreCase("false")) {
      msg = "ERROR: " + msg;
    } 
    response = String.valueOf(response) + "<username>" + tempUsername + "</username>";
    response = String.valueOf(response) + "<password>" + tempPassword + "</password>";
    response = String.valueOf(response) + "<message>" + Common.url_encode(msg) + "</message>";
    response = String.valueOf(response) + "<url>" + Common.url_encode(webLink) + "</url>";
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String manageShares(ServerSession thisSession) throws Exception {
    Vector listing = new Vector();
    try {
      String tempAccountsPath = ServerStatus.SG("temp_accounts_path");
      File[] accounts = (new File(String.valueOf(tempAccountsPath) + "accounts/")).listFiles();
      thisSession.date_time = ServerSession.updateDateCustomizations(thisSession.date_time, thisSession.user);
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
                SimpleDateFormat sdf1 = new SimpleDateFormat("MMddyyyyHHmm");
                SimpleDateFormat sdf2 = new SimpleDateFormat("MM/dd/yyyy HH:mm");
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
                try {
                  info.put("downloads", (new StringBuffer(String.valueOf(ServerStatus.thisObj.statTools.getUserDownloadCount(info.getProperty("username"))))).toString());
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
        VFS tempVFS = UserTools.ut.getVFS(thisSession.server_item.getProperty("linkedServer"), newUser);
        if (tempVFS != null) {
          Vector items = new Vector();
          tempVFS.getListing(items, "/Shares/" + thisSession.uiSG("user_name") + "/");
          for (int xx = 0; xx < items.size(); xx++) {
            Properties item_info2 = items.elementAt(xx);
            Properties item_info = tempVFS.get_item(String.valueOf(item_info2.getProperty("root_dir")) + item_info2.getProperty("name"));
            if (item_info == null)
              item_info = item_info2; 
            Properties sharedUser = new Properties();
            sharedUser.put("web_link", "");
            sharedUser.put("username", newUser.substring(0, newUser.lastIndexOf(".")));
            sharedUser.put("password", "");
            sharedUser.put("emailFrom", "");
            sharedUser.put("emailTo", "Username Share : " + newUser.substring(0, newUser.lastIndexOf(".")));
            sharedUser.put("emailCc", "");
            sharedUser.put("emailBcc", "");
            sharedUser.put("emailSubject", "");
            sharedUser.put("emailBody", "");
            sharedUser.put("paths", item_info.getProperty("url", ""));
            sharedUser.put("emailFrom", "");
            sharedUser.put("expire", "never");
            sharedUser.put("details", "");
            sharedUser.put("attach", "false");
            sharedUser.put("usernameShare", "true");
            sharedUser.put("publishType", "Internal Username Share");
            sharedUser.put("created", thisSession.date_time.format(new Date(Long.parseLong(item_info.getProperty("modified")))));
            sharedUser.put("createdMillis", item_info.getProperty("modified"));
            sharedUser.put("details", "");
            listing.addElement(sharedUser);
          } 
        } 
      } 
    } 
    String response = "";
    try {
      response = ServerStatus.thisObj.common_code.getXMLString(listing, "listingInfo", null);
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
    return response;
  }
  
  public static String deleteShare(Properties request, ServerSession thisSession) {
    try {
      String tempAccountsPath = ServerStatus.SG("temp_accounts_path");
      File[] accounts = (new File(String.valueOf(tempAccountsPath) + "accounts/")).listFiles();
      if (accounts != null) {
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
            Log.log("HTTP_SERVER", 2, "Removing entry:/Shares/" + thisSession.uiSG("user_name") + "/" + paths);
            Vector tempList = new Vector();
            tempVFS.getListing(tempList, "/Shares/" + thisSession.uiSG("user_name") + "/");
            if (tempList.size() == 0) {
              virtual.remove("/Shares/" + thisSession.uiSG("user_name") + "/");
              virtual.remove("/Shares/" + thisSession.uiSG("user_name"));
            } 
            tempList = new Vector();
            tempVFS.getListing(tempList, "/Shares/");
            if (tempList.size() == 0) {
              virtual.remove("/Shares/");
              virtual.remove("/Shares");
            } 
            UserTools.writeVFS(thisSession.server_item.getProperty("linkedServer"), String.valueOf(userid) + ".SHARED", tempVFS);
          } else {
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
      } 
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
    String response = "";
    try {
      response = ServerStatus.thisObj.common_code.getXMLString(request, "listingInfo", null);
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
    return response;
  }
}
