package com.crushftp.tunnel2;

import com.crushftp.client.Common;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.Vector;

public class ConnectionHandler implements Runnable {
  Tunnel2 t = null;
  
  String bindip = null;
  
  int bindport = 0;
  
  Vector threads = new Vector();
  
  static long lastUpdateSend = System.currentTimeMillis();
  
  static long lastUpdateReceive = System.currentTimeMillis();
  
  public ConnectionHandler(Tunnel2 t, String bindip, int bindport) {
    this.t = t;
    this.bindip = bindip;
    this.bindport = bindport;
    if (t.tunnel.getProperty("validate_cert", "false").equals("false"))
      Common.trustEverything(); 
    System.getProperties().put("sun.net.http.retryPost", "false");
  }
  
  public ConnectionHandler(Tunnel2 t) {
    this.t = t;
  }
  
  public void run() {
    ServerSocket ss1 = null;
    ServerSocket ss2 = null;
    Thread.currentThread().setName("Tunnel:ConnectionHandler:bindip:" + this.bindip + ":" + this.bindport);
    try {
      if (this.t.tunnel.getProperty("reverse", "false").equals("true") && !this.t.allowReverseMode)
        this.t.startStopTunnel(true); 
      ServerSocket ss0 = null;
      int sock_num = 0;
      while (this.t.isActive()) {
        if (this.t.tunnel.getProperty("reverse", "false").equals("false") || this.t.allowReverseMode) {
          try {
            if (ss1 == null) {
              ss1 = new ServerSocket(this.bindport, 1000, InetAddress.getByName(this.bindip));
              Tunnel2.msg("Tunnel2:ConnectionHandler:bound port:" + this.bindport);
              ss1.setSoTimeout(100);
              if (!this.t.allowReverseMode)
                this.t.startStopTunnel(true); 
              Tunnel2.msg("Tunnel2:ConnectionHandler:tunnel started.");
            } 
            if ((new StringBuffer(String.valueOf(this.bindport))).toString().startsWith("444") && ss2 == null) {
              ss2 = new ServerSocket(this.bindport + 10, 1000, InetAddress.getByName(this.bindip));
              Tunnel2.msg("Tunnel2:ConnectionHandler:bound port:" + (this.bindport + 10));
              ss2.setSoTimeout(100);
            } 
            this.t.markAvailable();
            sock_num++;
            if (sock_num == 1) {
              ss0 = ss1;
            } else {
              ss0 = ss2;
              sock_num = 0;
            } 
            if (ss0 == null)
              ss0 = ss1; 
            Socket proxy = ss0.accept();
            Tunnel2.msg("Tunnel2:ConnectionHandler:received connection:" + proxy);
            Socket control = proxy;
            boolean ftp = (new StringBuffer(String.valueOf(this.bindport))).toString().endsWith("21");
            try {
              if ((new StringBuffer(String.valueOf(this.bindport))).toString().startsWith("444")) {
                if (ss0.getLocalPort() == ss2.getLocalPort())
                  ftp = true; 
                if (ftp) {
                  this.t.tunnel.put("destPort", "55521");
                } else {
                  this.t.tunnel.put("destPort", "55580");
                } 
              } else if (this.bindport == 55555 || this.t.tunnel.getProperty("destPort").equals("55555") || this.t.tunnel.getProperty("destPort").equals("55580") || this.t.tunnel.getProperty("destPort").equals("55521") || this.t.tunnel.getProperty("destPort").equals("0")) {
                ftp = true;
                for (int x = 0; x < 50 && ftp; x++) {
                  if (proxy.getInputStream().available() > 0)
                    ftp = false; 
                  if (ftp)
                    Thread.sleep(10L); 
                } 
                if (ftp) {
                  this.t.tunnel.put("destPort", "55521");
                } else {
                  this.t.tunnel.put("destPort", "55580");
                } 
              } 
            } catch (Exception e) {
              Tunnel2.msg(e);
              try {
                proxy.close();
              } catch (Exception ee) {
                Common.log("TUNNEL", 1, ee);
              } 
              if (this.t.onlyOnce)
                break; 
            } 
            Tunnel2.msg("Tunnel2:ConnectionHandler:ftp=" + ftp);
            if (ftp) {
              ServerSocket ssProxyControl = new ServerSocket(0);
              int localPort = ssProxyControl.getLocalPort();
              control = new Socket("127.0.0.1", localPort);
              Queue q = process(ssProxyControl.accept(), this.t.tunnel.getProperty("destIp"), Integer.parseInt(this.t.tunnel.getProperty("destPort")), false, true, 0);
              ssProxyControl.close();
              FTPProxy ftpp = new FTPProxy(this.t, q);
              ftpp.proxyNATs(control, proxy);
              Tunnel2.msg("Tunnel2:Started FTP NAT:control=" + control + " proxy=" + proxy);
            } else {
              process(control, this.t.tunnel.getProperty("destIp"), Integer.parseInt(this.t.tunnel.getProperty("destPort")), false, true, 0);
            } 
          } catch (SocketTimeoutException socketTimeoutException) {
          
          } catch (Exception e) {
            Tunnel2.msg(e);
            try {
              Thread.sleep(1000L);
            } catch (Exception ee) {
              Common.log("TUNNEL", 1, ee);
            } 
          } 
          if (this.t.allowReverseMode)
            if (System.currentTimeMillis() - this.t.lastActivity > 30000L) {
              Tunnel2.msg("Tunnel is apparently inactive..." + (System.currentTimeMillis() - this.t.lastActivity));
              this.t.stopThisTunnel();
            }  
        } else {
          this.t.markAvailable();
          Thread.sleep(1000L);
        } 
        if (this.t.onlyOnce)
          break; 
      } 
      if (ss1 != null)
        ss1.close(); 
      if (ss2 != null)
        ss2.close(); 
      this.t.waitForShutdown();
    } catch (Exception e) {
      Common.log("TUNNEL", 1, e);
    } finally {
      try {
        if (ss1 != null)
          ss1.close(); 
      } catch (Exception exception) {}
      try {
        if (ss2 != null)
          ss2.close(); 
      } catch (Exception exception) {}
    } 
    try {
      this.t.startStopTunnel(false);
    } catch (Exception e) {
      Common.log("TUNNEL", 1, e);
    } 
  }
  
  public Queue process(Socket sock, String host, int port, boolean startLocal, boolean startRemote, int qid2) throws Exception {
    if (qid2 == 0)
      qid2 = (int)(Math.random() * 1.0E9D); 
    int qid = qid2;
    if (startRemote)
      this.t.doConnect(qid, host, port); 
    Queue q = new Queue(this.t, qid);
    this.t.addQueue(q);
    (new Thread(new Runnable(this, qid, sock, q) {
          final ConnectionHandler this$0;
          
          private final int val$qid;
          
          private final Socket val$sock;
          
          private final Queue val$q;
          
          public void run() {
            this.this$0.threads.addElement(Thread.currentThread());
            Thread.currentThread().setName("Tunnel:qid=" + this.val$qid + ":socket read=" + this.val$sock);
            Tunnel2.msg("Tunnel2:process:read:qid=" + this.val$qid + " :socket read=" + this.val$sock);
            BufferedInputStream in = null;
            try {
              this.val$sock.setSoTimeout(10000);
              in = new BufferedInputStream(this.val$sock.getInputStream());
              int bytesRead = 0;
              int num = 1;
              while (bytesRead >= 0 && !this.val$q.isClosedLocal()) {
                byte[] b = DProperties.getArray();
                try {
                  bytesRead = in.read(b, 0, 65500);
                  if (bytesRead > 0) {
                    Chunk c = new Chunk(this.val$qid, b, bytesRead, num++);
                    this.val$q.writeLocal(c, -1);
                    this.this$0.t.addBytesOut(bytesRead);
                    Tunnel2.writeAck(c, this.val$q, this.this$0.t);
                  } 
                } catch (SocketTimeoutException socketTimeoutException) {}
                if (System.currentTimeMillis() - ConnectionHandler.lastUpdateSend > 10000L) {
                  ConnectionHandler.lastUpdateSend = System.currentTimeMillis();
                  Tunnel2.msg("Tunnel out stats: remoteNum:" + this.val$q.remoteNum + " remoteSize:" + this.val$q.remote.size() + " localSize:" + this.val$q.localNum + " lastNum:" + num + " max:" + this.val$q.max + " localBytes:" + this.this$0.t.getLocal().getBytes() + " waitingAcks:" + this.this$0.t.getWaitingAckCount() + " qid:" + this.val$qid + " Free JVM Memory:" + Common.format_bytes_short(Common.getFreeRam()));
                } 
                while (this.this$0.t.getWaitingAckCount() > 100 || this.this$0.t.getLocal().getBytes() > (1048576 * Integer.parseInt(this.this$0.t.tunnel.getProperty("sendBuffer", "1"))))
                  Thread.sleep(1L); 
              } 
            } catch (Exception e) {
              if (!this.val$q.isClosedRemote())
                Tunnel2.msg(e); 
            } finally {
              try {
                this.val$q.closeLocal();
                Tunnel2.msg("Closing queue:" + this.val$qid + " end:" + this.val$q.localNum);
                if (in != null)
                  in.close(); 
                this.val$q.waitForClose(30);
              } catch (Exception e) {
                Common.log("TUNNEL", 1, e);
                this.val$q.remote.close();
              } 
            } 
            this.this$0.threads.remove(Thread.currentThread());
            this.this$0.t.removeQueue(this.val$qid);
          }
        })).start();
    (new Thread(new Runnable(this, qid, sock, q) {
          final ConnectionHandler this$0;
          
          private final int val$qid;
          
          private final Socket val$sock;
          
          private final Queue val$q;
          
          public void run() {
            this.this$0.threads.addElement(Thread.currentThread());
            Thread.currentThread().setName("Tunnel:qid=" + this.val$qid + ":socket write=" + this.val$sock);
            Tunnel2.msg("Tunnel2:process:write:qid=" + this.val$qid + ":socket write=" + this.val$sock);
            OutputStream out = null;
            try {
              out = this.val$sock.getOutputStream();
              int num = 0;
              while (!this.val$q.isClosedRemote() && (this.val$q.max < 0 || num < this.val$q.max)) {
                Chunk c = this.val$q.readRemote();
                if (c == null) {
                  Thread.sleep(100L);
                } else if (!c.isCommand()) {
                  num = c.num;
                  out.write(c.b);
                  this.this$0.t.addBytesIn(c.b.length);
                  c.b = DProperties.releaseArray(c.b);
                } 
                if (System.currentTimeMillis() - ConnectionHandler.lastUpdateReceive > 10000L) {
                  ConnectionHandler.lastUpdateReceive = System.currentTimeMillis();
                  Tunnel2.msg("Tunnel in stats: remoteNum:" + this.val$q.remoteNum + " remoteSize:" + this.val$q.remote.size() + " localSize:" + this.val$q.localNum + " lastNum:" + num + " max:" + this.val$q.max + " localBytes:" + this.this$0.t.getLocal().getBytes() + " waitingAcks:" + this.this$0.t.getWaitingAckCount() + " qid:" + this.val$qid + " Free JVM Memory:" + Common.format_bytes_short(Common.getFreeRam()));
                } 
              } 
              this.val$sock.close();
            } catch (Exception e) {
              Tunnel2.msg(e);
            } finally {
              try {
                this.val$q.closeRemote();
                out.close();
              } catch (Exception e) {
                Common.log("TUNNEL", 1, e);
              } 
              Enumeration keys = this.this$0.t.localAck.keys();
              while (keys.hasMoreElements()) {
                String key = keys.nextElement().toString();
                try {
                  Chunk c = this.this$0.t.localAck.get(key);
                  if (c != null && c.id == this.val$q.id) {
                    this.this$0.t.localAck.remove(key);
                    c.b = DProperties.releaseArray(c.b);
                  } 
                } catch (IOException e) {
                  Tunnel2.msg(e);
                } 
              } 
            } 
            this.this$0.threads.remove(Thread.currentThread());
          }
        })).start();
    return q;
  }
}
