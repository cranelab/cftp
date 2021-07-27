package crushftp.server.ssh;

import com.maverick.sshd.Connection;
import com.maverick.sshd.ForwardingPolicy;
import crushftp.handlers.SessionCrush;
import crushftp.server.ServerStatus;
import java.util.Properties;
import java.util.Vector;

public class SSHForwardingPolicy extends ForwardingPolicy {
  public boolean checkHostPermitted(Connection arg0, String arg1, int arg2) {
    String ip = arg1;
    if (arg1.equals("localhost"))
      ip = "127.0.0.1"; 
    Vector tunnelConfigs = getTunnels(arg0.getSessionId(), arg0.getUsername());
    for (int x = 0; x < tunnelConfigs.size(); x++) {
      Properties tunnel = tunnelConfigs.elementAt(x);
      if (tunnel.getProperty("tunnelType", "").equalsIgnoreCase("SSH")) {
        if (tunnel.getProperty("configurable", "false").equals("true"))
          return true; 
        if (tunnel.getProperty("destIp", "").equals(ip) && tunnel.getProperty("destPort", "").equals((new StringBuffer(String.valueOf(arg2))).toString()))
          return true; 
      } 
    } 
    return false;
  }
  
  public boolean checkInterfacePermitted(Connection arg0, String arg1, int arg2) {
    String ip = arg1;
    if (arg1.equals("localhost"))
      ip = "127.0.0.1"; 
    Vector tunnelConfigs = getTunnels(arg0.getSessionId(), arg0.getUsername());
    for (int x = 0; x < tunnelConfigs.size(); x++) {
      Properties tunnel = tunnelConfigs.elementAt(x);
      if (tunnel.getProperty("tunnelType", "").equalsIgnoreCase("SSH")) {
        if (tunnel.getProperty("configurable", "false").equals("true"))
          return true; 
        if (tunnel.getProperty("bindIp", "").equals(ip) && tunnel.getProperty("localPort", "").equals((new StringBuffer(String.valueOf(arg2))).toString()))
          return true; 
      } 
    } 
    return false;
  }
  
  private Vector getTunnels(String sessionid, String username) {
    SessionCrush thisSession = SSHCrushAuthentication8.getSession(sessionid);
    Vector tunnelConfigs = new Vector();
    Vector tunnels = ServerStatus.VG("tunnels");
    for (int x = 0; x < tunnels.size(); x++) {
      Properties tunnel = null;
      Properties p = tunnels.elementAt(x);
      String[] userTunnels = thisSession.SG("tunnels").split(",");
      for (int xx = 0; xx < userTunnels.length; xx++) {
        if (p.getProperty("id").equals(userTunnels[xx].trim()))
          tunnel = (Properties)p.clone(); 
      } 
      if (tunnel != null)
        tunnelConfigs.addElement(tunnel); 
    } 
    return tunnelConfigs;
  }
}
