package crushftp.server.ssh;

import com.maverick.sshd.AccessManager;
import com.maverick.sshd.Channel;
import com.maverick.sshd.ForwardingChannel;
import crushftp.server.ServerSession;
import crushftp.server.ServerStatus;
import java.net.SocketAddress;
import java.util.Properties;
import java.util.Vector;

public class SSHPortForwarderController implements AccessManager {
  public boolean canConnect(String username) {
    return true;
  }
  
  public boolean canConnect(SocketAddress address) {
    return true;
  }
  
  public boolean canExecuteCommand(byte[] sessionid, String username, String command) {
    return true;
  }
  
  public boolean canForward(byte[] sessionid, String username, ForwardingChannel channel, boolean isLocal) {
    if (channel.getOriginatingPort() == 0)
      return true; 
    Vector tunnelConfigs = getTunnels(sessionid, username);
    for (int x = 0; x < tunnelConfigs.size(); x++) {
      Properties tunnel = tunnelConfigs.elementAt(x);
      if (tunnel.getProperty("tunnelType", "").equalsIgnoreCase("SSH")) {
        if (tunnel.getProperty("configurable", "false").equals("true"))
          return true; 
        if (tunnel.getProperty("destIp", "").equals(channel.getHost()) && tunnel.getProperty("destPort", "").equals((new StringBuffer(String.valueOf(channel.getPort()))).toString()))
          return true; 
      } 
    } 
    return false;
  }
  
  public boolean canListen(byte[] sessionid, String username, String bindAddress, int bindPort) {
    Vector tunnelConfigs = getTunnels(sessionid, username);
    for (int x = 0; x < tunnelConfigs.size(); x++) {
      Properties tunnel = tunnelConfigs.elementAt(x);
      if (tunnel.getProperty("tunnelType", "").equalsIgnoreCase("SSH") && 
        !tunnel.getProperty("reverse", "false").equals("false")) {
        if (tunnel.getProperty("configurable", "false").equals("true"))
          return true; 
        if ((tunnel.getProperty("bindIp", "").equals("") || tunnel.getProperty("bindIp", "").equals("0.0.0.0") || tunnel.getProperty("bindIp", "").equals(bindAddress) || (tunnel.getProperty("bindIp", "").equals("127.0.0.1") && bindAddress.equalsIgnoreCase("localhost"))) && tunnel.getProperty("localPort", "").equals((new StringBuffer(String.valueOf(bindPort))).toString()))
          return true; 
      } 
    } 
    return false;
  }
  
  private Vector getTunnels(byte[] sessionid, String username) {
    ServerSession thisSession = SSHCrushAuthentication.getSession(sessionid, username);
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
  
  public boolean canOpenChannel(byte[] sessionid, String username, Channel channel) {
    return true;
  }
  
  public boolean canStartShell(byte[] sessionid, String username) {
    return false;
  }
  
  public boolean canStartSubsystem(byte[] sessionid, String username, String subsystem) {
    return true;
  }
  
  public String[] getRequiredAuthentications(byte[] sessionid, String username) {
    return null;
  }
}
