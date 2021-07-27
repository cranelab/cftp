package crushftp.server.ssh;

import com.crushftp.client.Common;
import com.maverick.ssh.SshException;
import com.maverick.ssh.components.SshPublicKey;
import com.maverick.sshd.Connection;
import com.maverick.sshd.auth.AbstractPublicKeyAuthenticationProvider;
import com.maverick.sshd.platform.PermissionDeniedException;
import com.sshtools.publickey.SshPublicKeyFileFactory;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.SessionCrush;
import crushftp.handlers.UserTools;
import crushftp.server.ServerStatus;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

public class PublicKeyVerifier extends AbstractPublicKeyAuthenticationProvider {
  public boolean isAuthorizedKey(SshPublicKey key, Connection conn) {
    try {
      SessionCrush thisSession = SSHCrushAuthentication8.getSession(conn.getSessionId());
      String username = conn.getUsername();
      thisSession.add_log_formatted("Verifying username and public key " + username + ".", "ACCEPT");
      Properties user = null;
      try {
        user = UserTools.ut.getUser(thisSession.server_item.getProperty("linkedServer"), username, true);
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
        if (!pp.getProperty("action", "").equalsIgnoreCase("success")) {
          pp.put("publickey_lookup", "true");
          pp.put("ssh_fingerprint", key.getFingerprint());
          pp = thisSession.runPlugin("login", pp);
          user = u;
          if (!pp.getProperty("templateUser", "").equals("")) {
            Vector extraLinkedVfs = (Vector)pp.get("linked_vfs");
            Vector ichain = new Vector();
            ichain.addElement("default");
            int i;
            for (i = 0; i < (pp.getProperty("templateUser", "").split(";")).length; i++)
              ichain.addElement(pp.getProperty("templateUser", "").split(";")[i].trim()); 
            if (extraLinkedVfs != null)
              ichain.addAll(extraLinkedVfs); 
            for (i = 0; i < ichain.size(); i++) {
              Properties tempUser = UserTools.ut.getUser(((Properties)pp.get("user_info")).getProperty("listen_ip_port"), ichain.elementAt(i).toString(), ServerStatus.BG("resolve_inheritance"));
              if (tempUser != null && 
                !tempUser.getProperty("ssh_public_keys", "").equals(""))
                u.put("ssh_public_keys", (String.valueOf(u.getProperty("ssh_public_keys", "")) + "\r\n" + tempUser.getProperty("ssh_public_keys", "")).trim()); 
            } 
          } 
          if (u.getProperty("ssh_public_keys", "").equals("")) {
            thisSession.add_log_formatted(String.valueOf(username) + " didn't have any public keys references, checking default.", "ACCEPT");
            Properties tmp_user = UserTools.ut.getUser(thisSession.server_item.getProperty("linkedServer"), "default", true);
            u.put("ssh_public_keys", tmp_user.getProperty("ssh_public_keys", ""));
          } 
        } 
      } 
      if (user == null) {
        thisSession.add_log_formatted(String.valueOf(username) + " not found.", "ACCEPT");
        if (System.getProperty("crushftp.ssh_auth_alerts", "false").equals("true"))
          try {
            Properties info = new Properties();
            info.put("alert_type", "bad_login");
            info.put("alert_sub_type", "username");
            info.put("alert_timeout", "0");
            info.put("alert_max", "0");
            info.put("alert_msg", String.valueOf(username) + " not found, public key auth ignored.");
            ServerStatus.thisObj.runAlerts("security_alert", info, thisSession.user_info, thisSession);
          } catch (Exception ee) {
            Log.log("BAN", 1, ee);
          }  
        return false;
      } 
      Vector keysVec = UserTools.buildPublicKeys(username, user, thisSession.server_item.getProperty("linkedServer"));
      Log.log("SSH_SERVER", 2, "public_keys found:" + keysVec.toString());
      for (int x = 0; x < keysVec.size(); x++) {
        String data = keysVec.elementAt(x).toString();
        if (data != null) {
          data = data.trim();
          InputStream in = null;
          try {
            StringTokenizer st = new StringTokenizer(data);
            int parts = 0;
            while (st.hasMoreElements()) {
              parts++;
              st.nextElement();
            } 
            if (parts <= 2)
              data = String.valueOf(data.trim()) + " nouser@domain.com"; 
            in = new ByteArrayInputStream(data.getBytes());
            if (SshPublicKeyFileFactory.parse(in).toPublicKey().getFingerprint().equals(key.getFingerprint())) {
              thisSession.add_log_formatted("Accepted public key for " + username + ":" + data, "ACCEPT");
              thisSession.uiPUT("publickey_auth_ok", "true");
              return logonUser(conn, username, key);
            } 
          } catch (Exception e) {
            Log.log("SSH_SERVER", 0, (new StringBuffer(String.valueOf(e.getMessage()))).toString());
            Log.log("SSH_SERVER", 1, e);
          } finally {
            if (in != null)
              in.close(); 
          } 
        } 
      } 
      if (keysVec.size() > 0)
        if (System.getProperty("crushftp.ssh_auth_alerts", "false").equals("true"))
          try {
            Properties info = new Properties();
            info.put("alert_type", "bad_login");
            info.put("alert_sub_type", "username");
            info.put("alert_timeout", "0");
            info.put("alert_max", "0");
            info.put("alert_msg", String.valueOf(username) + " failed public key auth, no matching fingerprints (" + keysVec.size() + " keys checked.");
            ServerStatus.thisObj.runAlerts("security_alert", info, thisSession.user_info, thisSession);
          } catch (Exception ee) {
            Log.log("BAN", 1, ee);
          }   
    } catch (Exception e) {
      Log.log("SSH_SERVER", 1, e);
    } 
    return false;
  }
  
  public void add(SshPublicKey arg0, String arg1, Connection arg2) throws IOException, PermissionDeniedException, SshException {}
  
  public Iterator getKeys(Connection arg0) throws PermissionDeniedException, IOException {
    return null;
  }
  
  public void remove(SshPublicKey arg0, Connection arg1) throws IOException, PermissionDeniedException, SshException {}
  
  public boolean logonUser(Connection conn, String username, SshPublicKey key) {
    try {
      if (ServerStatus.BG("username_uppercase"))
        username = username.toUpperCase(); 
      if (ServerStatus.BG("lowercase_usernames"))
        username = username.toLowerCase(); 
      SessionCrush thisSession = SSHCrushAuthentication8.getSession(conn.getSessionId());
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
        SessionCrush.checkTempAccounts(pp, thisSession.server_item.getProperty("linkedServer", ""));
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
      if (pass.startsWith("SHA:") || pass.startsWith("SHA512:") || pass.startsWith("SHA256:") || pass.startsWith("SHA3:") || pass.startsWith("MD5:") || pass.startsWith("CRYPT3:") || pass.startsWith("BCRYPT:") || pass.startsWith("MD5CRYPT:") || pass.startsWith("PBKDF2SHA256:") || pass.startsWith("SHA512CRYPT:")) {
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
      boolean publickey_password = user.getProperty("publickey_password", "false").equalsIgnoreCase("true");
      if (Common.dmz_mode) {
        Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
        Properties action = new Properties();
        action.put("type", "GET:USER");
        action.put("id", Common.makeBoundary());
        action.put("username", username);
        action.put("need_response", "true");
        queue.addElement(action);
        action = UserTools.waitResponse(action, 60);
        if (action != null && action.get("user") != null)
          publickey_password = ((Properties)action.get("user")).getProperty("publickey_password", "false").equalsIgnoreCase("true"); 
      } 
      if (!publickey_password)
        thisSession.login_user_pass(anyPass); 
      if (thisSession.uiBG("user_logged_in")) {
        thisSession.uiPUT("publickey_auth_ok", "true");
        if (!publickey_password)
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
}
