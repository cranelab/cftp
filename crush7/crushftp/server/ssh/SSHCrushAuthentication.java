package crushftp.server.ssh;

import com.crushftp.client.Common;
import com.crushftp.client.Worker;
import com.maverick.ssh.components.SshPublicKey;
import com.maverick.sshd.ConnectionManager;
import com.maverick.sshd.TransportProtocol;
import com.maverick.sshd.platform.NativeAuthenticationProvider;
import com.maverick.sshd.platform.PasswordChangeException;
import crushftp.gui.LOC;
import crushftp.handlers.Log;
import crushftp.handlers.SessionCrush;
import crushftp.handlers.UserTools;
import crushftp.server.ServerSessionSSH;
import crushftp.server.ServerStatus;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Properties;

public class SSHCrushAuthentication implements NativeAuthenticationProvider {
  public int getAuthenticationStatus() {
    return -1;
  }
  
  public boolean changePassword(byte[] sessionid, String username, String oldpassword, String newpassword) {
    SessionCrush thisSession = getSession(sessionid, username);
    Properties tempUser = UserTools.ut.getUser(thisSession.server_item.getProperty("linkedServer", ""), username, false);
    thisSession.uiPUT("current_password", oldpassword);
    try {
      if (thisSession.login_user_pass()) {
        thisSession.do_ChangePass(username, newpassword);
        return true;
      } 
    } catch (Exception e) {
      Log.log("SSH_SERVER", 0, e);
    } 
    return false;
  }
  
  public boolean logonUser(byte[] sessionid, String username, String password, SocketAddress ipAddress) throws PasswordChangeException {
    if (ServerStatus.BG("username_uppercase"))
      username = username.toUpperCase(); 
    if (ServerStatus.BG("lowercase_usernames"))
      username = username.toLowerCase(); 
    SessionCrush thisSession = getSession(sessionid, username);
    thisSession.runPlugin("beforeLogin", null);
    thisSession.add_log_formatted("Verifying password for " + username + ".", "ACCEPT");
    thisSession.uiPUT("current_password", password);
    thisSession.add_log_formatted("USER " + username, "USER");
    thisSession.add_log_formatted("PASS " + (username.toUpperCase().equals("ANONYMOUS") ? password : ""), "PASS");
    try {
      thisSession.login_user_pass();
      if (thisSession.uiBG("user_logged_in")) {
        if (thisSession.uiBG("password_expired"))
          throw new PasswordChangeException("Password expired."); 
        if (!thisSession.BG("publickey_password") || (thisSession.BG("publickey_password") && thisSession.uiBG("publickey_auth_ok"))) {
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
  
  public static SessionCrush getSession(byte[] sessionid, String username) {
    if (ServerStatus.BG("username_uppercase"))
      username = username.toUpperCase(); 
    if (ServerStatus.BG("lowercase_usernames"))
      username = username.toLowerCase(); 
    TransportProtocol transport = ConnectionManager.getInstance().getTransport(sessionid);
    int loops = 0;
    Log.log("SSH_SERVER", 2, "SSH PORT CONNECTOR LOOKUP:" + transport.getRemotePort());
    while (!ServerSessionSSH.connectionLookup.containsKey((new StringBuffer(String.valueOf(transport.getRemotePort()))).toString()) && loops++ < 10000) {
      try {
        Thread.sleep(1L);
      } catch (Exception exception) {}
    } 
    Properties p = (Properties)ServerSessionSSH.connectionLookup.get((new StringBuffer(String.valueOf(transport.getRemotePort()))).toString());
    Log.log("SSH_SERVER", 2, "SSH PORT CONNECTOR LOOKUP:" + transport.getRemotePort() + ":" + p);
    Properties server_item = (Properties)p.get("server_item");
    SessionCrush thisSession = (SessionCrush)ServerSessionSSH.sessionLookup.get(sessionid);
    if (thisSession == null) {
      thisSession = new SessionCrush((Socket)p.get("socket"), Integer.parseInt(p.getProperty("user_number")), p.getProperty("user_ip"), Integer.parseInt(p.getProperty("user_port")), p.getProperty("listen_ip"), p.getProperty("listen_ip_port"), server_item);
      ServerSessionSSH.sessionLookup.put(sessionid, thisSession);
      p.put("session", thisSession);
      p.remove("socket");
      thisSession.uiPUT("dont_read", "true");
      thisSession.uiPUT("dont_write", "true");
      thisSession.uiPUT("ssh_remote_port", (new StringBuffer(String.valueOf(transport.getRemotePort()))).toString());
      ServerStatus.thisObj.hold_user_pointer(thisSession.user_info);
      thisSession.add_log("[" + server_item.getProperty("serverType", "ftp") + ":" + server_item.getProperty("ip", "0.0.0.0") + ":" + server_item.getProperty("port", "21") + "][" + thisSession.uiSG("user_number") + "] " + LOC.G("Accepting connection from") + ": " + thisSession.uiSG("user_ip") + ":" + transport.getRemotePort() + "\r\n", "SSH_SESSION_ACCEPT");
      if (ServerStatus.BG("block_hack_username_immediately")) {
        String[] hack_users = ServerStatus.SG("hack_usernames").split(",");
        for (int x = 0; x < hack_users.length; x++) {
          if (!username.trim().equals("") && username.trim().equalsIgnoreCase(hack_users[x].trim())) {
            ServerStatus.thisObj.ban(thisSession.user_info, ServerStatus.IG("hban_timeout"), "hack username:" + username);
            ServerStatus.thisObj.kick(thisSession.user_info);
          } 
        } 
      } 
    } 
    thisSession.uiPUT("user_name", username);
    return thisSession;
  }
  
  public boolean logonUser(byte[] sessionid, String username, SocketAddress ipAddress, SshPublicKey key, boolean verifyOnly) {
    try {
      if (ServerStatus.BG("username_uppercase"))
        username = username.toUpperCase(); 
      if (ServerStatus.BG("lowercase_usernames"))
        username = username.toLowerCase(); 
      SessionCrush thisSession = getSession(sessionid, username);
      thisSession.runPlugin("beforeLogin", null);
      Properties user = null;
      try {
        user = UserTools.ut.getUser(thisSession.uiSG("listen_ip_port"), thisSession.uiSG("user_name"), true);
      } catch (Exception exception) {}
      if (user == null) {
        thisSession.add_log_formatted(String.valueOf(username) + " not found, checking plugins.", "ACCEPT");
        Properties u = new Properties();
        Properties pp = new Properties();
        pp.put("user", u);
        pp.put("username", thisSession.uiSG("user_name"));
        pp.put("password", "");
        pp.put("anyPass", "true");
        if (thisSession.uiSG("user_name").equalsIgnoreCase("default"))
          return false; 
        thisSession.checkTempAccounts(pp);
        if (!pp.getProperty("action", "").equalsIgnoreCase("success")) {
          pp = thisSession.runPlugin("login", pp);
          user = u;
        } 
      } 
      if (user == null) {
        thisSession.add_log_formatted(String.valueOf(username) + " not found.", "ACCEPT");
        return false;
      } 
      String pass = user.getProperty("password");
      boolean anyPass = true;
      thisSession.uiPUT("current_password", "");
      if (pass.startsWith("SHA:") || pass.startsWith("SHA512:") || pass.startsWith("SHA3:") || pass.startsWith("MD5:") || pass.startsWith("CRYPT3:") || pass.startsWith("BCRYPT:") || pass.startsWith("MD5CRYPT:") || pass.startsWith("SHA512CRYPT:")) {
        if (thisSession.uiBG("publickey_auth_ok"))
          anyPass = true; 
        if (!Common.System2.getProperty("crushftp.proxy.anyPassToken", "").equals("")) {
          anyPass = false;
          Log.log("SSH_SERVER", 2, "Logging in via proxy magic token 1.");
          if (!user.getProperty("ssh_public_keys", "").trim().equals(""))
            thisSession.uiPUT("current_password", Common.System2.getProperty("crushftp.proxy.anyPassToken", "")); 
        } 
      } else if (!Common.System2.getProperty("crushftp.proxy.anyPassToken", "").equals("")) {
        anyPass = false;
        Log.log("SSH_SERVER", 2, "Logging in via proxy magic token 2.");
        if (!user.getProperty("ssh_public_keys", "").trim().equals(""))
          thisSession.uiPUT("current_password", Common.System2.getProperty("crushftp.proxy.anyPassToken", "")); 
      } else {
        thisSession.uiPUT("current_password", ServerStatus.thisObj.common_code.decode_pass(pass));
      } 
      thisSession.add_log_formatted("USER " + thisSession.uiSG("user_name"), "USER");
      thisSession.add_log_formatted("PASS PublicKeyAuthentication", "PASS");
      if (!user.getProperty("publickey_password", "false").equalsIgnoreCase("true"))
        thisSession.login_user_pass(anyPass); 
      if (thisSession.uiBG("user_logged_in")) {
        thisSession.uiPUT("publickey_auth_ok", "true");
        if (!thisSession.BG("publickey_password"))
          thisSession.do_event5("LOGIN", null); 
        return true;
      } 
      try {
        Properties info = new Properties();
        info.put("alert_type", "bad_login");
        info.put("alert_sub_type", "username");
        info.put("alert_timeout", "0");
        info.put("alert_max", "0");
        info.put("alert_msg", String.valueOf(username) + " failed public key auth.");
        ServerStatus.thisObj.runAlerts("security_alert", info, thisSession.user_info, thisSession);
      } catch (Exception ee) {
        Log.log("BAN", 1, ee);
      } 
      return false;
    } catch (Exception e) {
      Log.log("SSH_SERVER", 1, e);
      return false;
    } 
  }
  
  public boolean logonUser(byte[] sessionid, String username, SocketAddress ipAddress) {
    return true;
  }
  
  public void logoffUser(byte[] sessionid, String username, SocketAddress ipAddress) {
    try {
      if (ServerStatus.BG("username_uppercase"))
        username = username.toUpperCase(); 
      if (ServerStatus.BG("lowercase_usernames"))
        username = username.toLowerCase(); 
      Thread.sleep(Integer.parseInt(System.getProperty("crushftp.sftp.logout.delay", "100")));
      SessionCrush thisSession = (SessionCrush)ServerSessionSSH.sessionLookup.remove(sessionid);
      if (thisSession != null) {
        Log.log("SSH_SERVER", 2, "SSH PORT CONNECTOR logoff RELEASE:" + thisSession.user_info.getProperty("ssh_remote_port", "0"));
        ServerSessionSSH.connectionLookup.remove(thisSession.user_info.getProperty("ssh_remote_port", "0"));
        ServerStatus.thisObj.remove_user(thisSession.user_info, false);
        thisSession.session_socks.removeAllElements();
        Worker.startWorker(new Runnable(this, thisSession) {
              final SSHCrushAuthentication this$0;
              
              private final SessionCrush val$thisSession;
              
              public void run() {
                this.val$thisSession.do_kill(null);
              }
            });
      } 
    } catch (Exception e) {
      Log.log("SSH_SERVER", 0, e);
    } 
  }
  
  public String getUserGroup(byte[] sessionid, String username) {
    return "users";
  }
  
  public String getHomeDirectory(byte[] sessionid, String username) {
    return ".";
  }
}
