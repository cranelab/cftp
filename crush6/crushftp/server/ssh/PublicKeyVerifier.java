package crushftp.server.ssh;

import com.maverick.ssh.components.SshPublicKey;
import com.maverick.sshd.PublicKeyStore;
import com.maverick.sshd.platform.AuthenticationProvider;
import com.sshtools.publickey.SshPublicKeyFileFactory;
import crushftp.handlers.Log;
import crushftp.handlers.UserTools;
import crushftp.server.ServerSession;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.SocketAddress;
import java.util.Properties;
import java.util.Vector;

public class PublicKeyVerifier implements PublicKeyStore {
  public boolean isAuthorizedKey(SshPublicKey key, byte[] sessionid, SocketAddress ipAddress, AuthenticationProvider authenticationProvider) {
    try {
      String username = AuthenticationProvider.getUsername(sessionid);
      ServerSession thisSession = SSHCrushAuthentication.getSession(sessionid, username);
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
          pp = thisSession.runPlugin("login", pp);
          user = u;
        } 
      } 
      if (user == null) {
        thisSession.add_log_formatted(String.valueOf(username) + " not found.", "ACCEPT");
        return false;
      } 
      Vector keysVec = UserTools.buildPublicKeys(username, user);
      Log.log("SSH_SERVER", 2, "public_keys found:" + keysVec.toString());
      for (int x = 0; x < keysVec.size(); x++) {
        String data = keysVec.elementAt(x).toString();
        InputStream in = null;
        try {
          in = new ByteArrayInputStream(data.getBytes());
          if (SshPublicKeyFileFactory.parse(in).toPublicKey().getFingerprint().equals(key.getFingerprint())) {
            thisSession.add_log_formatted("Accepted public key for " + username + ":" + data, "ACCEPT");
            thisSession.uiPUT("publickey_auth_ok", "true");
            return true;
          } 
        } catch (Exception e) {
          Log.log("SSH_SERVER", 0, e.getMessage());
          Log.log("SSH_SERVER", 1, e);
        } finally {
          if (in != null)
            in.close(); 
        } 
      } 
    } catch (Exception e) {
      e.printStackTrace();
    } 
    return false;
  }
}
