package com.crushftp.tunnel;

import com.crushftp.client.Common;
import com.crushftp.tunnel2.Tunnel2;
import com.crushftp.tunnel3.StreamController;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.util.Properties;

public class AutoChannelProxy {
  public static Object oneActiveTunnel = new Object();
  
  public static boolean v3 = false;
  
  public static Object enableAppletTunnel(Properties controller, boolean onlyOne, StringBuffer CrushAuth) throws Exception {
    controller.remove("stopTunnel");
    URL u = new URL(controller.getProperty("URL"));
    Properties tunnel = getTunnel(u, CrushAuth);
    if (tunnel != null && tunnel.size() > 0) {
      synchronized (oneActiveTunnel) {
        controller.put("tunnelInitialized", "false");
        System.out.println("Starting tunnel:" + tunnel);
        (new Thread(new Runnable(tunnel, controller, CrushAuth, onlyOne) {
              private final Properties val$tunnel;
              
              private final Properties val$controller;
              
              private final StringBuffer val$CrushAuth;
              
              private final boolean val$onlyOne;
              
              public void run() {
                Thread.currentThread().setName("Applet Tunnel Thread:" + this.val$tunnel);
                try {
                  if (this.val$tunnel.getProperty("tunnel_version", "").equalsIgnoreCase("Tunnel3"))
                    AutoChannelProxy.v3 = true; 
                  this.val$tunnel.put("url", this.val$controller.getProperty("URL"));
                  this.val$controller.put("URL", "http://127.0.0.1:" + this.val$tunnel.getProperty("localPort") + "/");
                  String url = (String.valueOf(this.val$tunnel.getProperty("url")) + "#").substring(0, (String.valueOf(this.val$tunnel.getProperty("url")) + "#").indexOf("#"));
                  if (AutoChannelProxy.v3) {
                    StreamController t = new StreamController(url, this.val$tunnel.getProperty("username", ""), this.val$tunnel.getProperty("password", ""), null);
                    this.val$controller.put("tunnelObj", t);
                    t.setTunnel(this.val$tunnel);
                    t.setAuth(this.val$CrushAuth.toString());
                    t.startThreads();
                    this.val$controller.put("tunnelInitialized", "true");
                    Properties statusInfo = (Properties)this.val$controller.get("statusInfo");
                    while (!this.val$controller.containsKey("stopTunnel")) {
                      Thread.sleep(100L);
                      statusInfo.put("tunnelInfo", " (Out=" + t.outgoing.size() + ", In=" + t.incoming.size() + ")");
                    } 
                    t.startStopTunnel(false);
                  } else {
                    Tunnel2 t = new Tunnel2(url, this.val$tunnel.getProperty("username", ""), this.val$tunnel.getProperty("password", ""), this.val$onlyOne);
                    this.val$controller.put("tunnelObj", t);
                    t.setTunnel(this.val$tunnel);
                    t.setAuth(this.val$CrushAuth.toString());
                    t.startThreads();
                    this.val$controller.put("tunnelInitialized", "true");
                    Properties statusInfo = (Properties)this.val$controller.get("statusInfo");
                    while (!this.val$controller.containsKey("stopTunnel")) {
                      Thread.sleep(100L);
                      statusInfo.put("tunnelInfo", " (Out=" + t.getSends() + ", In=" + t.getGets() + ")");
                    } 
                    t.setActive(false);
                    t.waitForShutdown();
                    while (!t.isShutdown())
                      Thread.sleep(100L); 
                  } 
                } catch (Exception e) {
                  System.out.println("Unable to load tunnels.");
                  e.printStackTrace();
                  this.val$controller.put("tunnelInitialized", "true");
                } 
                this.val$controller.remove("stopTunnel");
              }
            })).start();
      } 
      while (!controller.getProperty("tunnelInitialized", "false").equals("true"))
        Thread.sleep(100L); 
    } 
    return controller.remove("tunnelObj");
  }
  
  public static Properties getTunnel(URL u, StringBuffer CrushAuth) throws IOException {
    HttpURLConnection urlc = (HttpURLConnection)u.openConnection();
    urlc.setRequestMethod("POST");
    urlc.setRequestProperty("Cookie", "CrushAuth=" + CrushAuth.toString() + ";");
    urlc.setUseCaches(false);
    urlc.setDoOutput(true);
    urlc.getOutputStream().write(("c2f=" + CrushAuth.toString().substring(CrushAuth.toString().length() - 4) + "&command=getTunnels").getBytes("UTF8"));
    urlc.getResponseCode();
    InputStream in = urlc.getInputStream();
    String data = "";
    int bytesRead = 0;
    byte[] b = new byte[32768];
    while (bytesRead >= 0) {
      bytesRead = in.read(b);
      if (bytesRead > 0)
        data = String.valueOf(data) + new String(b, 0, bytesRead, "UTF8"); 
    } 
    in.close();
    urlc.disconnect();
    Properties tunnel = null;
    if (data.indexOf("<response>") > 0) {
      data = data.substring(data.indexOf("<response>") + "<response>".length(), data.indexOf("</response"));
      String[] tunnelsStr = Common.url_decode(data.replace('~', '%')).split(";;;");
      for (int x = 0; x < tunnelsStr.length; x++) {
        tunnel = new Properties();
        try {
          tunnel.load(new ByteArrayInputStream(tunnelsStr[x].getBytes("UTF8")));
          if (tunnel.getProperty("localPort", "0").equals(System.getProperty("crushtunnel.magicport", "55555"))) {
            ServerSocket ss = new ServerSocket(0);
            tunnel.put("localPort", (new StringBuffer(String.valueOf(ss.getLocalPort()))).toString());
            ss.close();
            break;
          } 
        } catch (Exception e) {
          e.printStackTrace();
        } 
      } 
    } 
    return tunnel;
  }
}
