package crushftp.server.ssh;

import com.crushftp.client.Common;
import com.maverick.sshd.Connection;
import com.maverick.sshd.PasswordAuthenticationProvider;
import com.maverick.sshd.platform.PasswordChangeException;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.SessionCrush;
import crushftp.handlers.UserTools;
import crushftp.server.ServerStatus;
import java.util.Properties;
import java.util.Vector;

public class PasswordAuthenticationProviderImpl extends PasswordAuthenticationProvider {
  public boolean verifyPassword(Connection conn, String username, String password) throws PasswordChangeException {
    if (ServerStatus.BG("username_uppercase"))
      username = username.toUpperCase(); 
    if (ServerStatus.BG("lowercase_usernames"))
      username = username.toLowerCase(); 
    SessionCrush thisSession = SSHCrushAuthentication8.getSession(conn.getSessionId());
    thisSession.runPlugin("beforeLogin", null);
    thisSession.add_log_formatted("Verifying password for " + username + ".", "ACCEPT");
    thisSession.uiPUT("current_password", password);
    thisSession.add_log_formatted("USER " + username, "USER");
    thisSession.add_log_formatted("PASS " + (username.toUpperCase().equals("ANONYMOUS") ? password : ""), "PASS");
    try {
      thisSession.login_user_pass();
      if (thisSession.uiBG("user_logged_in")) {
        if (thisSession.uiBG("password_expired")) {
          if (thisSession.SG("site").toUpperCase().indexOf("(SITE_PASS)") < 0) {
            thisSession.add_log_formatted("USER " + username, "Password expired. SITE PASS (Allows changing the password.) is not allowed.");
            return false;
          } 
          throw new PasswordChangeException("Password expired.");
        } 
        boolean publickey_password = thisSession.BG("publickey_password");
        if (Common.dmz_mode) {
          Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
          Properties action = new Properties();
          action.put("type", "GET:USER");
          action.put("id", Common.makeBoundary());
          action.put("username", username);
          action.put("need_response", "true");
          queue.addElement(action);
          action = UserTools.waitResponse(action, 60);
          if (action != null && (Properties)action.get("user") != null)
            publickey_password = ((Properties)action.get("user")).getProperty("publickey_password", "false").equalsIgnoreCase("true"); 
        } 
        if (!publickey_password || (publickey_password && thisSession.uiBG("publickey_auth_ok"))) {
          thisSession.do_event5("LOGIN", null);
          return true;
        } 
        thisSession.add_log_formatted("Public key was rejected, so password will not be accepted.", "USER");
        try {
          Properties info = new Properties();
          info.put("alert_type", "bad_login");
          info.put("alert_sub_type", "username");
          info.put("alert_timeout", "0");
          info.put("alert_max", "0");
          info.put("alert_msg", String.valueOf(username) + " failed public key auth, password not accepted for 2nd factor.");
          ServerStatus.thisObj.runAlerts("security_alert", info, thisSession.user_info, thisSession);
        } catch (Exception ee) {
          Log.log("BAN", 1, ee);
        } 
      } else {
        try {
          Properties info = new Properties();
          info.put("alert_type", "bad_login");
          info.put("alert_sub_type", "username");
          info.put("alert_timeout", "0");
          info.put("alert_max", "0");
          info.put("alert_msg", String.valueOf(username) + " failed auth, password or username not accepted.");
          ServerStatus.thisObj.runAlerts("security_alert", info, thisSession.user_info, thisSession);
        } catch (Exception ee) {
          Log.log("BAN", 1, ee);
        } 
      } 
    } catch (PasswordChangeException e) {
      throw e;
    } catch (Exception e) {
      Log.log("SSH_SERVER", 0, e);
    } 
    return false;
  }
  
  public boolean changePassword(Connection conn, String username, String oldpassword, String newpassword) {
    SessionCrush thisSession = SSHCrushAuthentication8.getSession(conn.getSessionId());
    Properties tempUser = UserTools.ut.getUser(thisSession.server_item.getProperty("linkedServer", ""), username, false);
    thisSession.uiPUT("current_password", oldpassword);
    try {
      if (thisSession.login_user_pass()) {
        String result = thisSession.do_ChangePass(username, newpassword);
        if (result.startsWith("ERROR:"))
          throw new Exception(result); 
        return true;
      } 
    } catch (Exception e) {
      Log.log("SSH_SERVER", 0, e);
    } 
    return false;
  }
}
