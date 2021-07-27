package crushftp.server.ssh;

import com.crushftp.client.Common;
import com.maverick.events.Event;
import com.maverick.events.EventListener;
import com.maverick.nio.Daemon;
import com.maverick.nio.DaemonContext;
import com.maverick.nio.ProtocolContext;
import com.maverick.sshd.SshContext;
import com.maverick.sshd.events.EventLog;
import com.maverick.sshd.platform.AuthenticationProvider;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.server.ServerStatus;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import javax.crypto.KeyGenerator;

public class SSHDaemon extends Daemon {
  public int localSSHPort = 0;
  
  public static boolean sshLoaded = false;
  
  public static Object lock = new Object();
  
  Properties server_item = null;
  
  SshContext sshContext = null;
  
  static Class class$0;
  
  static Class class$1;
  
  static Class class$2;
  
  static Class class$3;
  
  static Class class$4;
  
  static Class class$5;
  
  static Class class$6;
  
  static Class class$7;
  
  static Class class$8;
  
  static Class class$9;
  
  static Class class$10;
  
  static Class class$11;
  
  static Class class$12;
  
  public SSHDaemon(Properties server_item) {
    this.server_item = server_item;
    try {
      synchronized (lock) {
        ServerSocket ss = new ServerSocket(0, 100, InetAddress.getByName("127.0.0.1"));
        this.localSSHPort = ss.getLocalPort();
        ss.close();
        server_item.put("ssh_local_port", (new StringBuffer(String.valueOf(this.localSSHPort))).toString());
        if (!sshLoaded) {
          sshLoaded = true;
          DaemonContext.addEventListener(new SSHDaemon$1$myLogger(this));
        } 
      } 
    } catch (Exception e) {
      Log.log("SSH_SERVER", 0, e);
    } 
  }
  
  protected void configure(DaemonContext context) throws IOException {
    EventLog.LogEvent(this, "Configuring SSHD");
    this.sshContext = new SshContext(this);
    this.sshContext.setAccessManager(new SSHPortForwarderController());
    this.sshContext.setLocale(Locale.US);
    this.sshContext.setMaximumPacketLength(133120);
    try {
      try {
        KeyGenerator.getInstance("AES").init(256);
      } catch (Exception e) {
        EventLog.LogEvent(this, "WARNING: Max encryption strength is 128bit.");
        EventLog.LogEvent(this, "Strong cryptography extensions are not installed.  Some SSH clients may fail to connect as they expect AES256 to be available.");
        EventLog.LogEvent(this, "The files must be downloaded manually and installed in your Java lib/security folder.");
        EventLog.LogEvent(this, "Find from Google: https://www.google.com/search?q=java+jce+policy");
        EventLog.LogEvent(this, "Java6 result:http://www.oracle.com/technetwork/java/javase/downloads/jce-6-download-429243.html");
        EventLog.LogEvent(this, "Java7 result:http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html");
        EventLog.LogEvent(this, "OS X install location: /System/Library/Frameworks/JavaVM.framework/Versions/1.6.0/Home/lib/security/");
        EventLog.LogEvent(this, "Windows install location: C:\\Program Files\\Java\\jre6\\lib\\security\\");
      } 
      String[] ciphers = this.server_item.getProperty("ssh_cipher_list", "aes128-cbc,aes128-ctr,3des-cbc,blowfish-cbc,arcfour128,arcfour").split(",");
      this.sshContext.supportedCiphersCS().clear();
      this.sshContext.supportedCiphersSC().clear();
      for (int x = 0; x < ciphers.length; x++) {
        String c = ciphers[x].trim();
        if (c.equalsIgnoreCase("blowfish-cbc")) {
          if (class$0 == null)
            try {
            
            } catch (ClassNotFoundException classNotFoundException) {
              throw new NoClassDefFoundError(null.getMessage());
            }  
          "blowfish-cbc".add((String)class$0, class$0 = Class.forName("com.maverick.ssh.components.jce.BlowfishCbc"));
        } else if (c.equalsIgnoreCase("3des-cbc")) {
          if (class$1 == null)
            try {
            
            } catch (ClassNotFoundException classNotFoundException) {
              throw new NoClassDefFoundError(null.getMessage());
            }  
          "3des-cbc".add((String)class$1, class$1 = Class.forName("com.maverick.ssh.components.jce.TripleDesCbc"));
        } else if (c.equalsIgnoreCase("aes128-ctr")) {
          if (class$2 == null)
            try {
            
            } catch (ClassNotFoundException classNotFoundException) {
              throw new NoClassDefFoundError(null.getMessage());
            }  
          "aes128-ctr".add((String)class$2, class$2 = Class.forName("com.maverick.ssh.components.jce.AES128Ctr"));
        } else if (c.equalsIgnoreCase("aes128-cbc")) {
          if (class$3 == null)
            try {
            
            } catch (ClassNotFoundException classNotFoundException) {
              throw new NoClassDefFoundError(null.getMessage());
            }  
          "aes128-cbc".add((String)class$3, class$3 = Class.forName("com.maverick.ssh.components.jce.AES128Cbc"));
        } else if (c.equalsIgnoreCase("aes192-ctr")) {
          if (class$4 == null)
            try {
            
            } catch (ClassNotFoundException classNotFoundException) {
              throw new NoClassDefFoundError(null.getMessage());
            }  
          "aes192-ctr".add((String)class$4, class$4 = Class.forName("com.maverick.ssh.components.jce.AES192Ctr"));
        } else if (c.equalsIgnoreCase("aes192-cbc")) {
          if (class$5 == null)
            try {
            
            } catch (ClassNotFoundException classNotFoundException) {
              throw new NoClassDefFoundError(null.getMessage());
            }  
          "aes192-cbc".add((String)class$5, class$5 = Class.forName("com.maverick.ssh.components.jce.AES192Cbc"));
        } else if (c.equalsIgnoreCase("aes256-ctr")) {
          if (class$6 == null)
            try {
            
            } catch (ClassNotFoundException classNotFoundException) {
              throw new NoClassDefFoundError(null.getMessage());
            }  
          "aes256-ctr".add((String)class$6, class$6 = Class.forName("com.maverick.ssh.components.jce.AES256Ctr"));
        } else if (c.equalsIgnoreCase("aes256-cbc")) {
          if (class$7 == null)
            try {
            
            } catch (ClassNotFoundException classNotFoundException) {
              throw new NoClassDefFoundError(null.getMessage());
            }  
          "aes256-cbc".add((String)class$7, class$7 = Class.forName("com.maverick.ssh.components.jce.AES256Cbc"));
        } else if (c.equalsIgnoreCase("arcfour")) {
          if (class$8 == null)
            try {
            
            } catch (ClassNotFoundException classNotFoundException) {
              throw new NoClassDefFoundError(null.getMessage());
            }  
          "arcfour".add((String)class$8, class$8 = Class.forName("com.maverick.ssh.components.jce.ArcFour"));
        } else if (c.equalsIgnoreCase("arcfour128")) {
          if (class$9 == null)
            try {
            
            } catch (ClassNotFoundException classNotFoundException) {
              throw new NoClassDefFoundError(null.getMessage());
            }  
          "arcfour128".add((String)class$9, class$9 = Class.forName("com.maverick.ssh.components.jce.ArcFour128"));
        } else if (c.equalsIgnoreCase("arcfour256")) {
          if (class$10 == null)
            try {
            
            } catch (ClassNotFoundException classNotFoundException) {
              throw new NoClassDefFoundError(null.getMessage());
            }  
          "arcfour256".add((String)class$10, class$10 = Class.forName("com.maverick.ssh.components.jce.ArcFour256"));
        } 
        if (c.equalsIgnoreCase("blowfish-cbc")) {
          if (class$0 == null)
            try {
            
            } catch (ClassNotFoundException classNotFoundException) {
              throw new NoClassDefFoundError(null.getMessage());
            }  
          "blowfish-cbc".add((String)class$0, class$0 = Class.forName("com.maverick.ssh.components.jce.BlowfishCbc"));
        } else if (c.equalsIgnoreCase("3des-cbc")) {
          if (class$1 == null)
            try {
            
            } catch (ClassNotFoundException classNotFoundException) {
              throw new NoClassDefFoundError(null.getMessage());
            }  
          "3des-cbc".add((String)class$1, class$1 = Class.forName("com.maverick.ssh.components.jce.TripleDesCbc"));
        } else if (c.equalsIgnoreCase("aes128-ctr")) {
          if (class$2 == null)
            try {
            
            } catch (ClassNotFoundException classNotFoundException) {
              throw new NoClassDefFoundError(null.getMessage());
            }  
          "aes128-ctr".add((String)class$2, class$2 = Class.forName("com.maverick.ssh.components.jce.AES128Ctr"));
        } else if (c.equalsIgnoreCase("aes128-cbc")) {
          if (class$3 == null)
            try {
            
            } catch (ClassNotFoundException classNotFoundException) {
              throw new NoClassDefFoundError(null.getMessage());
            }  
          "aes128-cbc".add((String)class$3, class$3 = Class.forName("com.maverick.ssh.components.jce.AES128Cbc"));
        } else if (c.equalsIgnoreCase("aes192-ctr")) {
          if (class$4 == null)
            try {
            
            } catch (ClassNotFoundException classNotFoundException) {
              throw new NoClassDefFoundError(null.getMessage());
            }  
          "aes192-ctr".add((String)class$4, class$4 = Class.forName("com.maverick.ssh.components.jce.AES192Ctr"));
        } else if (c.equalsIgnoreCase("aes192-cbc")) {
          if (class$5 == null)
            try {
            
            } catch (ClassNotFoundException classNotFoundException) {
              throw new NoClassDefFoundError(null.getMessage());
            }  
          "aes192-cbc".add((String)class$5, class$5 = Class.forName("com.maverick.ssh.components.jce.AES192Cbc"));
        } else if (c.equalsIgnoreCase("aes256-ctr")) {
          if (class$6 == null)
            try {
            
            } catch (ClassNotFoundException classNotFoundException) {
              throw new NoClassDefFoundError(null.getMessage());
            }  
          "aes256-ctr".add((String)class$6, class$6 = Class.forName("com.maverick.ssh.components.jce.AES256Ctr"));
        } else if (c.equalsIgnoreCase("aes256-cbc")) {
          if (class$7 == null)
            try {
            
            } catch (ClassNotFoundException classNotFoundException) {
              throw new NoClassDefFoundError(null.getMessage());
            }  
          "aes256-cbc".add((String)class$7, class$7 = Class.forName("com.maverick.ssh.components.jce.AES256Cbc"));
        } else if (c.equalsIgnoreCase("arcfour")) {
          if (class$8 == null)
            try {
            
            } catch (ClassNotFoundException classNotFoundException) {
              throw new NoClassDefFoundError(null.getMessage());
            }  
          "arcfour".add((String)class$8, class$8 = Class.forName("com.maverick.ssh.components.jce.ArcFour"));
        } else if (c.equalsIgnoreCase("arcfour128")) {
          if (class$9 == null)
            try {
            
            } catch (ClassNotFoundException classNotFoundException) {
              throw new NoClassDefFoundError(null.getMessage());
            }  
          "arcfour128".add((String)class$9, class$9 = Class.forName("com.maverick.ssh.components.jce.ArcFour128"));
        } else if (c.equalsIgnoreCase("arcfour256")) {
          if (class$10 == null)
            try {
            
            } catch (ClassNotFoundException classNotFoundException) {
              throw new NoClassDefFoundError(null.getMessage());
            }  
          "arcfour256".add((String)class$10, class$10 = Class.forName("com.maverick.ssh.components.jce.ArcFour256"));
        } 
      } 
      this.sshContext.setPreferredKeyExchange("diffie-hellman-group1-sha1");
      try {
        this.sshContext.setPreferredCipherCS("aes128-cbc");
      } catch (Exception e) {
        Log.log("SSH_SERVER", 0, e);
      } 
      try {
        this.sshContext.setPreferredCipherSC("aes128-cbc");
      } catch (Exception e) {
        Log.log("SSH_SERVER", 0, e);
      } 
      this.sshContext.setPreferredMacCS("hmac-md5");
      this.sshContext.setPreferredMacSC("hmac-md5");
      String welcome_msg = this.server_item.getProperty("ftp_welcome_message", "").trim();
      if (!welcome_msg.equals(""))
        welcome_msg = String.valueOf(welcome_msg) + "\r\n"; 
      this.sshContext.setBannerMessage(welcome_msg);
      String rsa_key = this.server_item.getProperty("ssh_rsa_key", String.valueOf(System.getProperty("crushftp.prefs")) + "ssh_host_rsa_key");
      if (this.server_item.getProperty("ssh_rsa_enabled", "true").equals("true") && !rsa_key.equals(""))
        if (Common.System2.containsKey("crushftp.keystores." + rsa_key.toUpperCase().replace('\\', '/'))) {
          Properties p = (Properties)Common.System2.get("crushftp.keystores." + rsa_key.toUpperCase().replace('\\', '/'));
          this.sshContext.loadHostKey(new ByteArrayInputStream((byte[])p.get("bytes")), "RSA", 1024);
        } else {
          this.sshContext.loadOrGenerateHostKey(new File(rsa_key), "ssh-rsa", 1024);
        }  
      String dsa_key = this.server_item.getProperty("ssh_dsa_key", String.valueOf(System.getProperty("crushftp.prefs")) + "ssh_host_dsa_key");
      if (this.server_item.getProperty("ssh_dsa_enabled", "true").equals("true") && !dsa_key.equals(""))
        if (Common.System2.containsKey("crushftp.keystores." + dsa_key.toUpperCase().replace('\\', '/'))) {
          Properties p = (Properties)Common.System2.get("crushftp.keystores." + dsa_key.toUpperCase().replace('\\', '/'));
          this.sshContext.loadHostKey(new ByteArrayInputStream((byte[])p.get("bytes")), "DSA", 1024);
        } else {
          this.sshContext.loadOrGenerateHostKey(new File(dsa_key), "ssh-dss", 1024);
        }  
      this.server_item.put("ssh_rsa_key", rsa_key);
      this.server_item.put("ssh_dsa_key", dsa_key);
      AuthenticationProvider authProv = new AuthenticationProvider(new SSHCrushAuthentication(), this.sshContext);
      this.sshContext.setAuthenticationProvider(authProv);
      if (class$11 == null)
        try {
        
        } catch (ClassNotFoundException classNotFoundException) {
          throw new NoClassDefFoundError(null.getMessage());
        }  
      class$11.setFileSystemProvider(class$11 = Class.forName("crushftp.server.ssh.ServerSessionSSH"));
      if (class$12 == null)
        try {
        
        } catch (ClassNotFoundException classNotFoundException) {
          throw new NoClassDefFoundError(null.getMessage());
        }  
      "scp".addCommand((String)class$12, class$12 = Class.forName("com.maverick.sshd.scp.ScpCommand"));
      if (class$11 == null)
        try {
        
        } catch (ClassNotFoundException classNotFoundException) {
          throw new NoClassDefFoundError(null.getMessage());
        }  
      "sftp".addCommand((String)class$11, class$11 = Class.forName("crushftp.server.ssh.ServerSessionSSH"));
    } catch (Exception e) {
      Log.log("SSH_SERVER", 0, e);
    } 
    this.sshContext.setPublicKeyStore(new PublicKeyVerifier());
    this.sshContext.setRequiredAuthenticationMethods(0);
    if (this.server_item.getProperty("ssh_require_password", "false").equals("true"))
      this.sshContext.addRequiredAuthentication("password"); 
    if (this.server_item.getProperty("ssh_require_publickey", "false").equals("true"))
      this.sshContext.addRequiredAuthentication("publickey"); 
    this.sshContext.setChannelLimit(1000);
    context.addListeningInterface("127.0.0.1", this.localSSHPort, (ProtocolContext)this.sshContext);
    this.sshContext.setAsynchronousFileOperations(this.server_item.getProperty("ssh_async", "false").equals("true"));
    this.sshContext.setRemoteForwardingCancelKillsTunnels(true);
    context.setPermanentTransferThreads(Integer.parseInt(this.server_item.getProperty("ssh_transfer_threads", "10")));
    context.setPermanentAcceptThreads(Integer.parseInt(this.server_item.getProperty("ssh_accept_threads", "10")));
    context.setPermanentConnectThreads(Integer.parseInt(this.server_item.getProperty("ssh_connect_threads", "10")));
    this.sshContext.setSoftwareVersionComments(ServerStatus.SG("ssh_header"));
    this.sshContext.setSocketOptionKeepAlive(true);
    this.sshContext.setSocketOptionTcpNoDelay(true);
    this.sshContext.setSocketOptionReuseAddress(true);
    this.sshContext.setSFTPCharsetEncoding(this.server_item.getProperty("ssh_text_encoding", "UTF8"));
    this.sshContext.setAllowDeniedKEX(true);
    this.sshContext.setSessionTimeout(Integer.parseInt(this.server_item.getProperty("ssh_session_timeout", "300")));
    EventLog.LogEvent(this, "Configuration complete.");
  }
  
  public void stop() {
    shutdown();
  }
  
  public static void setupDaemon(Properties server_item) {
    if (!server_item.containsKey("ssh_rsa_enabled")) {
      server_item.put("ssh_rsa_enabled", "false");
      server_item.put("ssh_dsa_enabled", "false");
      server_item.put("ssh_rsa_key", "ssh_host_rsa_key");
      server_item.put("ssh_dsa_key", "ssh_host_dsa_key");
      server_item.put("ssh_cipher_list", "aes128-cbc,aes128-ctr,3des-cbc,blowfish-cbc,arcfour128,arcfour");
      server_item.put("ssh_debug_logging", "false");
      server_item.put("ssh_text_encoding", "UTF8");
      server_item.put("ssh_session_timeout", "300");
      server_item.put("ssh_async", "false");
      server_item.put("ssh_require_password", "false");
      server_item.put("ssh_require_publickey", "false");
      try {
        String home = System.getProperty("crushftp.home");
        String filename = String.valueOf((new File(home)).getCanonicalPath()) + "/conf/server_host_key";
        if ((new File(filename)).exists()) {
          RandomAccessFile ra = new RandomAccessFile(filename, "r");
          byte[] b = new byte[(int)ra.length()];
          ra.readFully(b);
          ra.close();
          String key = new String(b);
          if (key.indexOf("bit dsa") >= 0) {
            Common.copy(filename, String.valueOf((new File(home)).getCanonicalPath()) + "/ssh_host_dsa_key", false);
            server_item.put("ssh_dsa_enabled", "true");
          } else {
            Common.copy(filename, String.valueOf((new File(home)).getCanonicalPath()) + "/ssh_host_rsa_key", false);
            server_item.put("ssh_rsa_enabled", "true");
          } 
        } else {
          server_item.put("ssh_dsa_enabled", "true");
          server_item.put("ssh_rsa_enabled", "true");
        } 
      } catch (Exception ee) {
        Log.log("SSH_SERVER", 0, ee);
      } 
    } 
  }
}
