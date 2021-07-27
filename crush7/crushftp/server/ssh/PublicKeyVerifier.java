package crushftp.server.ssh;

import com.maverick.ssh.components.SshPublicKey;
import com.maverick.sshd.PublicKeyStore;
import com.maverick.sshd.platform.AuthenticationProvider;
import com.sshtools.publickey.SshPublicKeyFileFactory;
import crushftp.handlers.Log;
import crushftp.handlers.SessionCrush;
import crushftp.handlers.UserTools;
import crushftp.server.ServerStatus;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.SocketAddress;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

public class PublicKeyVerifier implements PublicKeyStore {
  public boolean isAuthorizedKey(SshPublicKey key, byte[] sessionid, SocketAddress ipAddress, AuthenticationProvider authenticationProvider) {
    try {
      String username = AuthenticationProvider.getUsername(sessionid);
      SessionCrush thisSession = SSHCrushAuthentication.getSession(sessionid, username);
      thisSession.add_log_formatted("Verifying username and public key " + username + ".", "ACCEPT");
      String key_fingerprint = key.getFingerprint();
      Properties user = null;
      try {
        user = UserTools.ut.getUser(thisSession.server_item.getProperty("linkedServer"), username, true);
      } catch (Exception exception) {}
      Log.log("SSH_SERVER", 2, "SSH public key user lookup success (User Manager:" + username + "):" + ((user == null) ? 1 : 0));
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
          if (u.getProperty("ssh_public_keys", "").equals("")) {
            thisSession.add_log_formatted(String.valueOf(username) + " didn't have any public keys references, checking default.", "ACCEPT");
            Properties tmp_user = UserTools.ut.getUser(thisSession.server_item.getProperty("linkedServer"), "default", true);
            u.put("ssh_public_keys", tmp_user.getProperty("ssh_public_keys", ""));
          } 
        } 
        Log.log("SSH_SERVER", 2, "SSH public key user lookup success (plugins:" + username + "):" + ((user == null) ? 1 : 0));
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
      Vector keysVec = UserTools.buildPublicKeys(username, user);
      thisSession.add_log_formatted(String.valueOf(username) + " has " + keysVec.size() + " public keys.  Validating...", "ACCEPT");
      Log.log("SSH_SERVER", 2, "public_keys found for user:" + username + ":" + keysVec.toString());
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
            String f1 = SshPublicKeyFileFactory.parse(in).toPublicKey().getFingerprint();
            if (f1.equals(key_fingerprint)) {
              thisSession.add_log_formatted("Accepted public key for " + username + ":" + data, "ACCEPT");
              thisSession.uiPUT("publickey_auth_ok", "true");
              return true;
            } 
          } catch (Exception e) {
            StringTokenizer st;
            Log.log("SSH_SERVER", 0, (new StringBuffer(String.valueOf(st.getMessage()))).toString());
            Log.log("SSH_SERVER", 1, (Exception)st);
          } finally {
            if (in != null)
              in.close(); 
          } 
          if (in != null)
            in.close(); 
        } 
      } 
      if (keysVec.size() > 0) {
        thisSession.add_log_formatted(String.valueOf(username) + " has " + keysVec.size() + " public keys, but none matched the key presented.", "ACCEPT");
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
      } 
    } catch (Exception e) {
      Log.log("SSH_SERVER", 1, e);
    } 
    return false;
  }
}
