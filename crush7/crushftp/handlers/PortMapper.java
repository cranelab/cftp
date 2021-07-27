package crushftp.handlers;

import com.crushftp.client.Common;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;
import net.tomp2p.natpmp.Gateway;
import net.tomp2p.natpmp.MapRequestMessage;
import net.tomp2p.natpmp.Message;
import net.tomp2p.natpmp.NatPmpDevice;
import net.tomp2p.upnp.InternetGatewayDevice;

public class PortMapper implements Runnable {
  NatPmpDevice pmp = null;
  
  Properties gateways = new Properties();
  
  boolean added = false;
  
  boolean skip_pmp = false;
  
  boolean skip_upnp = false;
  
  public void run() {
    clearAll();
  }
  
  public synchronized boolean mapPort(String ip, int port, int secs) throws Exception {
    if (!this.added) {
      Runtime.getRuntime().addShutdownHook(new Thread(this));
      this.added = true;
    } 
    if (ip == null || ip.equals("lookup") || ip.equals("auto") || ip.equals("0.0.0.0"))
      ip = Common.getLocalIP(); 
    boolean ok = false;
    if (!this.skip_pmp) {
      if (this.pmp == null)
        this.pmp = new NatPmpDevice(Gateway.getIP()); 
      MapRequestMessage tcp = new MapRequestMessage(true, port, port, secs, null);
      this.pmp.enqueueMessage((Message)tcp);
      for (int x = 0; x < 25 && !ok; x++) {
        Thread.sleep(100L);
        if (tcp.getResultCode().equalsIgnoreCase("Success"))
          ok = true; 
      } 
      if (ok) {
        Log.log("SERVER", 0, "Mapped external port using PMP:" + port);
      } else {
        this.skip_pmp = true;
      } 
    } 
    if (!this.skip_upnp) {
      Object[] IGDs = new Object[0];
      Collection col = InternetGatewayDevice.getDevices(1500);
      if (col != null)
        IGDs = col.toArray(); 
      for (int x = 0; x < IGDs.length; x++) {
        InternetGatewayDevice igd = (InternetGatewayDevice)IGDs[x];
        try {
          if (igd.addPortMapping("CrushFTP", "TCP", ip, port, "TCP", port, secs)) {
            this.gateways.put((new StringBuffer(String.valueOf(port))).toString(), igd);
            ok = true;
            Log.log("SERVER", 0, "Mapped external port using UPNP:" + port);
          } else {
            this.skip_upnp = true;
          } 
        } catch (Exception e) {
          e.printStackTrace();
          Log.log("SERVER", 1, e);
        } 
      } 
    } 
    return ok;
  }
  
  public synchronized void clearAll() {
    if (this.pmp != null)
      this.pmp.shutdown(); 
    this.pmp = null;
    Enumeration keys = this.gateways.keys();
    while (keys.hasMoreElements()) {
      String port = keys.nextElement().toString();
      InternetGatewayDevice igd = (InternetGatewayDevice)this.gateways.remove(port);
      try {
        igd.deletePortMapping(null, Integer.parseInt(port), "TCP");
      } catch (Exception exception) {}
    } 
  }
}
