package com.crushftp.tunnel2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

public class FTPProxy {
  static int pasv_port = 0;
  
  Tunnel2 t = null;
  
  Queue q = null;
  
  Properties ftpPortTweaker = new Properties();
  
  public FTPProxy(Tunnel2 t, Queue q) {
    this.t = t;
    this.q = q;
  }
  
  static ServerSocket getNextPasvSocket() {
    int port1 = Integer.parseInt(System.getProperty("crushtunnel.pasv.port.start", "0"));
    int port2 = Integer.parseInt(System.getProperty("crushtunnel.pasv.port.stop", "0"));
    int loops = 0;
    while (loops++ < 1000) {
      try {
        if (port1 >= 0 && port1 < port2) {
          if (pasv_port < port1) {
            pasv_port = port1;
          } else if (pasv_port > port2) {
            pasv_port = port1;
          } 
          pasv_port++;
        } 
        return new ServerSocket(pasv_port);
      } catch (IOException iOException) {
        try {
          Thread.sleep(100L);
        } catch (InterruptedException interruptedException) {}
      } 
    } 
    return null;
  }
  
  public void proxyNATs(Socket control, Socket proxy) throws Exception {
    proxyNAT(control, proxy);
    proxyNAT(proxy, control);
  }
  
  private Thread proxyNAT(Socket control, Socket proxy) throws Exception {
    Thread thread = new Thread(new Runnable(this, control, proxy) {
          final FTPProxy this$0;
          
          private final Socket val$control;
          
          private final Socket val$proxy;
          
          public void run() {
            Thread.currentThread().setName("Tunnel FTP Proxy:" + this.val$control);
            try {
              BufferedReader br = new BufferedReader(new InputStreamReader(this.val$control.getInputStream(), "UTF8"));
              BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(this.val$proxy.getOutputStream(), "UTF8"));
              String line = "";
              while ((line = br.readLine()) != null && !this.this$0.q.isClosedLocal()) {
                if (System.getProperty("tunnel.debug.ftp", "false").equals("true"))
                  Tunnel2.msg("IN:" + this.val$control + ":" + line); 
                if (line.toUpperCase().startsWith("LIST ") || line.toUpperCase().startsWith("STOR ") || line.toUpperCase().startsWith("APPE ") || line.toUpperCase().startsWith("RETR ") || line.startsWith("150 ") || line.startsWith("125 ")) {
                  this.val$control.setSoTimeout(0);
                  this.val$proxy.setSoTimeout(0);
                } else {
                  this.val$control.setSoTimeout(3600000);
                  this.val$proxy.setSoTimeout(3600000);
                } 
                if (line.startsWith("227 ")) {
                  String pasvIpTmp = line.substring(line.lastIndexOf("(") + 1, line.lastIndexOf(")"));
                  int pasvPort = Integer.parseInt(pasvIpTmp.split(",")[4].trim()) * 256 + Integer.parseInt(pasvIpTmp.split(",")[5].trim());
                  pasvIpTmp = pasvIpTmp.substring(0, pasvIpTmp.lastIndexOf(","));
                  pasvIpTmp = pasvIpTmp.substring(0, pasvIpTmp.lastIndexOf(","));
                  pasvIpTmp = pasvIpTmp.replace(',', '.').trim();
                  String pasvIp = pasvIpTmp;
                  ServerSocket ssProxyData = FTPProxy.getNextPasvSocket();
                  line = "227 Entering Passive Mode (" + System.getProperty("crushtunnel.pasv.ip", "127.0.0.1").replace('.', ',') + "," + (ssProxyData.getLocalPort() / 256) + "," + (ssProxyData.getLocalPort() - ssProxyData.getLocalPort() / 256 * 256) + ")";
                  ConnectionHandler ch = new ConnectionHandler(this.this$0.t);
                  if (this.this$0.ftpPortTweaker.getProperty("activePassiveSwap", "").equals("true")) {
                    this.this$0.ftpPortTweaker.put("activePassiveSwap", "false");
                    line = "200 PORT command successful.";
                    Socket sock1 = new Socket(this.this$0.ftpPortTweaker.getProperty("lastActiveIp"), Integer.parseInt(this.this$0.ftpPortTweaker.getProperty("lastActivePort")));
                    ch.process(sock1, pasvIp, pasvPort, false, true, 0);
                    ssProxyData.close();
                  } else {
                    (new Thread((Runnable)new Object(this, this.val$control, pasvIp, pasvPort, ch, ssProxyData)))


















                      
                      .start();
                  } 
                } else if (line.startsWith("229 ")) {
                  line = line.substring(line.lastIndexOf("(") + 1, line.lastIndexOf(")"));
                  int pasvPort = Integer.parseInt(line.split("\\|")[3]);
                  ServerSocket ssProxyData = FTPProxy.getNextPasvSocket();
                  ConnectionHandler ch = new ConnectionHandler(this.this$0.t);
                  line = "229 Entering Extended Passive Mode (|||" + ssProxyData.getLocalPort() + "|)";
                  (new Thread((Runnable)new Object(this, this.val$control, pasvPort, ch, ssProxyData)))


















                    
                    .start();
                } else if (line.startsWith("PORT ")) {
                  String activeIp = line.substring(line.indexOf(" ") + 1).trim();
                  int activePort = Integer.parseInt(activeIp.split(",")[4].trim()) * 256 + Integer.parseInt(activeIp.split(",")[5].trim());
                  activeIp = activeIp.substring(0, activeIp.lastIndexOf(","));
                  activeIp = activeIp.substring(0, activeIp.lastIndexOf(","));
                  activeIp = activeIp.replace(',', '.').trim();
                  this.this$0.ftpPortTweaker.put("activePassiveSwap", "true");
                  this.this$0.ftpPortTweaker.put("lastActiveIp", activeIp);
                  this.this$0.ftpPortTweaker.put("lastActivePort", (new StringBuffer(String.valueOf(activePort))).toString());
                  line = "PASV";
                } else if (line.startsWith("EPRT ")) {
                  String activeIp = line.split("\\|")[2].trim();
                  int activePort = Integer.parseInt(line.split("\\|")[3].trim());
                  this.this$0.ftpPortTweaker.put("activePassiveSwap", "true");
                  this.this$0.ftpPortTweaker.put("lastActiveIp", activeIp);
                  this.this$0.ftpPortTweaker.put("lastActivePort", (new StringBuffer(String.valueOf(activePort))).toString());
                  line = "PASV";
                } 
                if (System.getProperty("tunnel.debug.ftp", "false").equals("true"))
                  Tunnel2.msg("OUT:" + this.val$control + ":" + line); 
                bw.write(String.valueOf(line) + "\r\n");
                bw.flush();
              } 
            } catch (Exception e) {
              Tunnel2.msg(e);
            } finally {
              try {
                this.val$control.close();
              } catch (Exception exception) {}
              try {
                this.val$proxy.close();
              } catch (Exception exception) {}
            } 
          }
        });
    thread.start();
    return thread;
  }
}
