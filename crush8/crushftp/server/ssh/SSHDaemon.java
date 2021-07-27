package crushftp.server.ssh;

import com.crushftp.client.Common;
import com.crushftp.client.File_S;
import com.maverick.events.Event;
import com.maverick.events.EventListener;
import com.maverick.nio.Daemon;
import com.maverick.nio.DaemonContext;
import com.maverick.nio.ProtocolContext;
import com.maverick.sshd.Authenticator;
import com.maverick.sshd.Connection;
import com.maverick.sshd.KeyboardInteractiveAuthenticationProvider;
import com.maverick.sshd.PasswordAuthenticationProvider;
import com.maverick.sshd.PasswordKeyboardInteractiveProvider;
import com.maverick.sshd.SshContext;
import com.maverick.sshd.events.EventServiceImplementation;
import com.maverick.sshd.platform.KeyboardInteractiveProvider;
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
import org.apache.log4j.PropertyConfigurator;

public class SSHDaemon extends Daemon {
  static Object maverickConfiguredLock = new Object();
  
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
  
  static Class class$13;
  
  static Class class$14;
  
  public SSHDaemon(Properties server_item) {
    this.server_item = server_item;
    System.setProperty("ssh.maxWindowSpace", (new StringBuffer(String.valueOf(1024 * Integer.parseInt(server_item.getProperty("window_space", "4096"))))).toString());
    System.setProperty("maverick.disableProtocolViolation", "true");
    try {
      synchronized (lock) {
        ServerSocket ss = new ServerSocket(0, 100, InetAddress.getByName("127.0.0.1"));
        this.localSSHPort = ss.getLocalPort();
        ss.close();
        server_item.put("ssh_local_port", (new StringBuffer(String.valueOf(this.localSSHPort))).toString());
        if (!sshLoaded) {
          sshLoaded = true;
          EventServiceImplementation.getInstance().addListener(new EventListener(this) {
                final SSHDaemon this$0;
                
                public void processEvent(Event evt) {
                  try {
                    if (evt == null)
                      return; 
                    Throwable t = null;
                    String s = "";
                    if (evt.getAttribute("LOG_MESSAGE") != null)
                      s = evt.getAttribute("LOG_MESSAGE").toString(); 
                    if (evt.getAttribute("IP") != null)
                      s = String.valueOf(s) + ":" + evt.getAttribute("IP").toString(); 
                    if (evt.getAttribute("THROWABLE") != null)
                      t = (Throwable)evt.getAttribute("THROWABLE"); 
                    if (t != null)
                      Log.log("SSH_SERVER", 0, t); 
                    if (s.equals(""))
                      return; 
                    if (s.toUpperCase().indexOf("Failed to create".toUpperCase()) >= 0)
                      s = String.valueOf(s) + "#######Do you have the Java Strong cryptography policy files installed?"; 
                    s = String.valueOf(ServerStatus.thisObj.logDateFormat.format(new Date())) + "|" + s;
                    ServerStatus.thisObj.append_log(s, "ACCEPT");
                  } catch (Exception e) {
                    e.printStackTrace();
                  } 
                }
              });
        } 
      } 
    } catch (Exception e) {
      Log.log("SSH_SERVER", 0, e);
    } 
  }
  
  protected void configure(DaemonContext context) throws IOException {
    synchronized (maverickConfiguredLock) {
      if ((new File("maverick_log4j.properties")).exists())
        PropertyConfigurator.configure("maverick_log4j.properties"); 
      Log.log("SSH_SERVER", 0, "Configuring SSHD");
      this.sshContext = new SshContext(this);
      if (!this.server_item.getProperty("max_dh_size", "").trim().equals("") && !this.server_item.getProperty("max_dh_size", "").trim().equals("0"))
        this.sshContext.setMaxDHGroupExchangeKeySize(Integer.parseInt(this.server_item.getProperty("max_dh_size", "1024"))); 
      if (!this.server_item.getProperty("min_dh_size", "").trim().equals("") && !this.server_item.getProperty("min_dh_size", "").trim().equals("0"))
        this.sshContext.setMinDHGroupExchangeKeySize(Integer.parseInt(this.server_item.getProperty("min_dh_size", "1024"))); 
      if (this.sshContext.getMinDHGroupExchangeKeySize() > this.sshContext.getMaxDHGroupExchangeKeySize())
        this.sshContext.setMinDHGroupExchangeKeySize(this.sshContext.getMaxDHGroupExchangeKeySize()); 
      this.sshContext.setForwardingPolicy(new SSHForwardingPolicy());
      this.sshContext.setLocale(Locale.US);
      int max_packet_size = Integer.parseInt(this.server_item.getProperty("max_packet_length", "70000"));
      if (max_packet_size < 32000)
        max_packet_size = 70000; 
      this.server_item.put("max_packet_length", (new StringBuffer(String.valueOf(max_packet_size))).toString());
      this.sshContext.setMaximumPacketLength(max_packet_size);
      this.sshContext.setMaxAuthentications(30);
      int max_channels = Integer.parseInt(this.server_item.getProperty("max_channels", "5"));
      if (max_channels < 1)
        max_channels = 1; 
      this.sshContext.setChannelLimit(max_channels);
      try {
        String[] ciphers = this.server_item.getProperty("ssh_cipher_list", "aes128-ctr,aes192-ctr,aes256-ctr,3des-ctr,3des-cbc,blowfish-cbc,arcfour,arcfour128,arcfour256,aes128-gcm@openssh.com,aes256-gcm@openssh.com").split(",");
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
          } else if (c.equalsIgnoreCase("3des-ctr")) {
            if (class$11 == null)
              try {
              
              } catch (ClassNotFoundException classNotFoundException) {
                throw new NoClassDefFoundError(null.getMessage());
              }  
            "3des-ctr".add((String)class$11, class$11 = Class.forName("com.maverick.ssh.components.jce.TripleDesCtr"));
          } else if (c.equalsIgnoreCase("aes128-gcm@openssh.com")) {
            if (class$12 == null)
              try {
              
              } catch (ClassNotFoundException classNotFoundException) {
                throw new NoClassDefFoundError(null.getMessage());
              }  
            "aes128-gcm@openssh.com".add((String)class$12, class$12 = Class.forName("com.maverick.ssh.components.jce.AES128Gcm"));
          } else if (c.equalsIgnoreCase("aes256-gcm@openssh.com")) {
            if (class$13 == null)
              try {
              
              } catch (ClassNotFoundException classNotFoundException) {
                throw new NoClassDefFoundError(null.getMessage());
              }  
            "aes256-gcm@openssh.com".add((String)class$13, class$13 = Class.forName("com.maverick.ssh.components.jce.AES256Gcm"));
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
          } else if (c.equalsIgnoreCase("3des-ctr")) {
            if (class$11 == null)
              try {
              
              } catch (ClassNotFoundException classNotFoundException) {
                throw new NoClassDefFoundError(null.getMessage());
              }  
            "3des-ctr".add((String)class$11, class$11 = Class.forName("com.maverick.ssh.components.jce.TripleDesCtr"));
          } else if (c.equalsIgnoreCase("aes128-gcm@openssh.com")) {
            if (class$12 == null)
              try {
              
              } catch (ClassNotFoundException classNotFoundException) {
                throw new NoClassDefFoundError(null.getMessage());
              }  
            "aes128-gcm@openssh.com".add((String)class$12, class$12 = Class.forName("com.maverick.ssh.components.jce.AES128Gcm"));
          } else if (c.equalsIgnoreCase("aes256-gcm@openssh.com")) {
            if (class$13 == null)
              try {
              
              } catch (ClassNotFoundException classNotFoundException) {
                throw new NoClassDefFoundError(null.getMessage());
              }  
            "aes256-gcm@openssh.com".add((String)class$13, class$13 = Class.forName("com.maverick.ssh.components.jce.AES256Gcm"));
          } 
        } 
        String kex = this.server_item.getProperty("key_exchanges", "curve25519-sha256@libssh.org,diffie-hellman-group-exchange-sha256,diffie-hellman-group18-sha512,diffie-hellman-group17-sha512,diffie-hellman-group16-sha512,diffie-hellman-group15-sha512,diffie-hellman-group14-sha256,diffie-hellman-group14-sha1,ecdh-sha2-nistp256,ecdh-sha2-nistp384,ecdh-sha2-nistp521,diffie-hellman-group-exchange-sha1,rsa2048-sha256,rsa1024-sha1");
        String[] supported_key = this.sshContext.supportedKeyExchanges().toArray();
        for (int i = 0; i < supported_key.length; i++) {
          if (kex.indexOf(supported_key[i].toLowerCase()) < 0)
            this.sshContext.supportedKeyExchanges().remove(supported_key[i]); 
        } 
        try {
          this.sshContext.setPreferredCipherCS("aes128-ctr");
        } catch (Exception e) {
          Log.log("SSH_SERVER", 2, e);
        } 
        try {
          this.sshContext.setPreferredCipherSC("aes128-ctr");
        } catch (Exception e) {
          Log.log("SSH_SERVER", 2, e);
        } 
        try {
          this.sshContext.setPreferredCipherCS("aes256-ctr");
        } catch (Exception e) {
          Log.log("SSH_SERVER", 3, e);
        } 
        try {
          this.sshContext.setPreferredCipherSC("aes256-ctr");
        } catch (Exception e) {
          Log.log("SSH_SERVER", 3, e);
        } 
        this.sshContext.setMaximumNumberofAsyncSFTPRequests(Integer.parseInt(this.server_item.getProperty("max_async_req", "200")));
        String macs_list = this.server_item.getProperty("ssh_mac_list", "hmac-sha256,hmac-sha2-256,hmac-sha256@ssh.com,hmac-sha2-256-etm@openssh.com,hmac-sha2-256-96,hmac-sha512,hmac-sha2-512,hmac-sha512@ssh.com,hmac-sha2-512-etm@openssh.com,hmac-sha2-512-96,hmac-sha1,hmac-sha1-etm@openssh.com,hmac-sha1-96,hmac-ripemd160,hmac-ripemd160@openssh.com,hmac-ripemd160-etm@openssh.com,hmac-md5,hmac-md5-etm@openssh.com,hmac-md5-96").toLowerCase();
        String[] macs = this.sshContext.supportedMacsCS().toArray();
        for (int j = 0; j < macs.length; j++) {
          String c = macs[j].trim().toLowerCase();
          if (macs_list.indexOf(c) < 0) {
            this.sshContext.supportedMacsCS().remove(c);
            this.sshContext.supportedMacsSC().remove(c);
          } 
        } 
        try {
          this.sshContext.setPreferredMacCS("hmac-md5");
          this.sshContext.setPreferredMacSC("hmac-md5");
        } catch (Exception exception) {}
        String welcome_msg = this.server_item.getProperty("ftp_welcome_message", "").trim();
        if (!welcome_msg.equals(""))
          welcome_msg = String.valueOf(welcome_msg) + "\r\n"; 
        this.sshContext.setBannerMessage(welcome_msg);
        String rsa_key = this.server_item.getProperty("ssh_rsa_key", String.valueOf(System.getProperty("crushftp.prefs")) + "ssh_host_rsa_key");
        if (this.server_item.getProperty("ssh_rsa_enabled", "true").equals("true") && !rsa_key.equals(""))
          if (Common.System2.containsKey("crushftp.keystores." + rsa_key.toUpperCase().replace('\\', '/'))) {
            Properties p = (Properties)Common.System2.get("crushftp.keystores." + rsa_key.toUpperCase().replace('\\', '/'));
            try {
              this.sshContext.loadHostKey(new ByteArrayInputStream((byte[])p.get("bytes")), "RSA", 1024);
            } catch (Exception e) {
              try {
                this.sshContext.loadHostKey(new ByteArrayInputStream((byte[])p.get("bytes")), "RSA", 2048);
              } catch (Exception e1) {
                this.sshContext.loadHostKey(new ByteArrayInputStream((byte[])p.get("bytes")), "RSA", 4096);
              } 
            } 
          } else {
            try {
              this.sshContext.loadOrGenerateHostKey(new File_S(rsa_key), "ssh-rsa", 4096);
            } catch (Exception e) {
              try {
                this.sshContext.loadOrGenerateHostKey(new File_S(rsa_key), "ssh-rsa", 2048);
              } catch (Exception e1) {
                this.sshContext.loadOrGenerateHostKey(new File_S(rsa_key), "ssh-rsa", 1024);
              } 
            } 
          }  
        String dsa_key = this.server_item.getProperty("ssh_dsa_key", String.valueOf(System.getProperty("crushftp.prefs")) + "ssh_host_dsa_key");
        if (this.server_item.getProperty("ssh_dsa_enabled", "true").equals("true") && !dsa_key.equals(""))
          if (Common.System2.containsKey("crushftp.keystores." + dsa_key.toUpperCase().replace('\\', '/'))) {
            Properties p = (Properties)Common.System2.get("crushftp.keystores." + dsa_key.toUpperCase().replace('\\', '/'));
            try {
              this.sshContext.loadHostKey(new ByteArrayInputStream((byte[])p.get("bytes")), "DSA", 1024);
            } catch (Exception e) {
              Log.log("SERVER", 0, e);
              try {
                this.sshContext.loadHostKey(new ByteArrayInputStream((byte[])p.get("bytes")), "DSA", 2048);
              } catch (Exception e1) {
                Log.log("SERVER", 0, e1);
                this.sshContext.loadHostKey(new ByteArrayInputStream((byte[])p.get("bytes")), "DSA", 4096);
              } 
            } 
          } else {
            try {
              this.sshContext.loadOrGenerateHostKey(new File_S(dsa_key), "ssh-dss", 4096);
            } catch (Exception e) {
              Log.log("SERVER", 0, e);
              try {
                this.sshContext.loadOrGenerateHostKey(new File_S(dsa_key), "ssh-dss", 2048);
              } catch (Exception e1) {
                Log.log("SERVER", 0, e1);
                this.sshContext.loadOrGenerateHostKey(new File_S(dsa_key), "ssh-dss", 1024);
              } 
            } 
          }  
        this.server_item.put("ssh_rsa_key", rsa_key);
        this.server_item.put("ssh_dsa_key", dsa_key);
        LimitedAuthProvider authFactory = new LimitedAuthProvider();
        authFactory.addProvider((Authenticator)new PasswordAuthenticationProviderImpl());
        authFactory.addProvider((Authenticator)new PublicKeyVerifier());
        if (this.server_item.getProperty("ssh_require_password", "false").equals("false"))
          authFactory.addProvider((Authenticator)new KeyboardInteractiveAuthenticationProvider(this) {
                final SSHDaemon this$0;
                
                public KeyboardInteractiveProvider createInstance(Connection con) {
                  return (KeyboardInteractiveProvider)new PasswordKeyboardInteractiveProvider(new PasswordAuthenticationProvider[] { new PasswordAuthenticationProviderImpl() }, con);
                }
              }); 
        this.sshContext.setAuthenicationMechanismFactory(authFactory);
        this.sshContext.setFileSystemProvider(new SSHServerSessionFactory());
        if (class$14 == null)
          try {
          
          } catch (ClassNotFoundException classNotFoundException) {
            throw new NoClassDefFoundError(null.getMessage());
          }  
        "scp".addCommand((String)class$14, class$14 = Class.forName("com.maverick.sshd.scp.ScpCommand"));
      } catch (Exception e) {
        e.printStackTrace();
        Log.log("SSH_SERVER", 0, e);
      } 
      this.sshContext.setRequiredAuthenticationMethods(0);
      if (this.server_item.getProperty("ssh_require_password", "false").equals("true"))
        this.sshContext.addRequiredAuthentication("password"); 
      if (this.server_item.getProperty("ssh_require_publickey", "false").equals("true"))
        this.sshContext.addRequiredAuthentication("publickey"); 
      this.sshContext.setChannelLimit(1000);
      context.addListeningInterface("127.0.0.1", this.localSSHPort, (ProtocolContext)this.sshContext);
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
      Log.log("SSH_SERVER", 0, "SSHD Configuration complete.");
    } 
  }
  
  public void stop() {
    shutdownAsync(false, 1000L);
  }
  
  public static void setupDaemon(Properties server_item) {
    if (!server_item.containsKey("ssh_rsa_enabled")) {
      server_item.put("ssh_rsa_enabled", "false");
      server_item.put("ssh_dsa_enabled", "false");
      server_item.put("ssh_rsa_key", "ssh_host_rsa_key");
      server_item.put("ssh_dsa_key", "ssh_host_dsa_key");
      server_item.put("ssh_cipher_list", "aes128-ctr,3des-cbc,blowfish-cbc,arcfour128,arcfour");
      server_item.put("ssh_debug_logging", "false");
      server_item.put("ssh_text_encoding", "UTF8");
      server_item.put("ssh_session_timeout", "300");
      server_item.put("ssh_async", "false");
      server_item.put("ssh_require_password", "false");
      server_item.put("ssh_require_publickey", "false");
      try {
        String home = System.getProperty("crushftp.home");
        String filename = String.valueOf((new File_S(home)).getCanonicalPath()) + "/conf/server_host_key";
        if ((new File_S(filename)).exists()) {
          RandomAccessFile ra = new RandomAccessFile(new File_S(filename), "r");
          byte[] b = new byte[(int)ra.length()];
          ra.readFully(b);
          ra.close();
          String key = new String(b);
          if (key.indexOf("bit dsa") >= 0) {
            Common.copy(filename, String.valueOf((new File_S(home)).getCanonicalPath()) + "/ssh_host_dsa_key", false);
            server_item.put("ssh_dsa_enabled", "true");
          } else {
            Common.copy(filename, String.valueOf((new File_S(home)).getCanonicalPath()) + "/ssh_host_rsa_key", false);
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
