package com.crushftp.client;

import com.maverick.events.Event;
import com.maverick.events.EventListener;
import com.maverick.events.J2SSHEventCodes;
import com.maverick.sftp.SftpFile;
import com.maverick.sftp.SftpFileAttributes;
import com.maverick.sftp.SftpStatusException;
import com.maverick.ssh.LicenseManager;
import com.maverick.ssh.PasswordAuthentication;
import com.maverick.ssh.PublicKeyAuthentication;
import com.maverick.ssh.SshAuthentication;
import com.maverick.ssh.SshClient;
import com.maverick.ssh.SshConnector;
import com.maverick.ssh.SshException;
import com.maverick.ssh.SshTransport;
import com.maverick.ssh.components.SshKeyPair;
import com.maverick.ssh.components.jce.JCEProvider;
import com.maverick.ssh2.Ssh2Client;
import com.maverick.ssh2.Ssh2Context;
import com.maverick.util.UnsignedInteger64;
import com.sshtools.net.SocketWrapper;
import com.sshtools.publickey.SshPrivateKeyFile;
import com.sshtools.publickey.SshPrivateKeyFileFactory;
import com.sshtools.sftp.SftpClient;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

public class SFTPClient extends GenericClient {
  static {
    LicenseManager.addLicense("----BEGIN 3SP LICENSE----\r\nProduct : Maverick Legacy Client\r\nLicensee: Ben Spink\r\nComments: Standard Support\r\nType    : Standard Support (Runtime License)\r\nCreated : 23-Jul-2019\r\n\r\n378720655FA0CA5BA600F3CF9CCEF4C807D6D7B3612824E6\r\nFE847F6B068ADD473496E8B34BA0FE6AB937EF1C7A9B1690\r\n6528050451ACA334D8399F56F7F3F997B53B985FD7390442\r\n09DA900DBC96721D3556844F9A17842B708C8FF055985808\r\nF44C56B640BA13674080CDBD19DDBA86B8286513F34B5944\r\nB8F46E8047769AA72F41151B4192862579E1ABE8B4B8C97E\r\n----END 3SP LICENSE----\r\n");
  }
  
  private SftpClient sftp = null;
  
  private Ssh2Client session = null;
  
  Ssh2Context ssh2Context = null;
  
  SftpLogger sftpLog = new SftpLogger(this);
  
  String uniqueId = Common.makeBoundary(10);
  
  Vector recent_mkdirs = new Vector();
  
  public static Object ssh_bug_lock = new Object();
  
  static boolean added_bc = false;
  
  Socket ssh_socket = null;
  
  public SFTPClient(String url, String header, Vector log) {
    super(header, log);
    this.url = url;
    if (!added_bc && System.getProperty("crushftp.ssh_bouncycastle", "true").equals("true")) {
      added_bc = true;
      try {
        JCEProvider.enableBouncyCastle(System.getProperty("crushftp.ssh_bouncycastle", "true").equals("true"));
      } catch (Exception e) {
        if (log != null)
          log.addElement(e); 
      } 
    } 
  }
  
  private void setThreadName() {
    String cur_name = Thread.currentThread().getName();
    if (cur_name.indexOf("|") >= 0)
      cur_name = cur_name.substring(cur_name.indexOf("|") + 1); 
    if (!Thread.currentThread().getName().startsWith(this.uniqueId))
      Thread.currentThread().setName(String.valueOf(this.uniqueId) + "|" + cur_name); 
  }
  
  public String login2(String username, String password, String clientid) throws Exception {
    setThreadName();
    this.config.put("username", username);
    this.config.put("password", password);
    if (clientid != null)
      this.config.put("clientid", clientid); 
    reconnect();
    SshConnector.addEventListener(this.uniqueId, this.sftpLog);
    return "";
  }
  
  public void reconnect() throws Exception {
    try {
      if (this.sftp != null && !this.sftp.isClosed())
        return; 
      if (this.sftp != null) {
        Common.log("SSH_CLIENT", 0, "Reconnecting disconnected SFTP connection...");
      } else {
        Common.log("SSH_CLIENT", 0, "Connecting new SFTP connection...");
      } 
      SshConnector con = null;
      synchronized (ssh_bug_lock) {
        con = SshConnector.createInstance();
        con.setSoftwareVersionComments("CrushFTP-java");
        this.ssh2Context = (Ssh2Context)con.getContext(2);
        if (System.getProperty("crushftp.fips140_sftp_client", "false").equals("true"))
          this.ssh2Context.enableFIPSMode(); 
      } 
      if (this.config.getProperty("sftp_compress", "false").equals("true"))
        this.ssh2Context.enableCompression(); 
      this.ssh2Context.setHostKeyVerification(new SFTPHostKeyVerifier(this.config.getProperty("knownHostFile"), this.config.getProperty("verifyHost", "false").equalsIgnoreCase("true"), this.config.getProperty("addNewHost", "false").equalsIgnoreCase("true")));
      this.ssh2Context.setPreferredPublicKey("ssh-dss");
      this.ssh2Context.setCipherPreferredPositionCS("aes128-ctr", 0);
      this.ssh2Context.setCipherPreferredPositionCS("aes128-cbc", 1);
      this.ssh2Context.setCipherPreferredPositionSC("aes128-ctr", 0);
      this.ssh2Context.setCipherPreferredPositionSC("aes128-cbc", 1);
      this.ssh2Context.setSocketTimeout(600000);
      this.ssh2Context.setIdleConnectionTimeoutSeconds(600);
      this.ssh2Context.setPreferredPublicKey("ssh-rsa");
      this.ssh2Context.setPublicKeyPreferredPosition("ssh-rsa", 0);
      this.ssh2Context.setPreferredKeyExchange("diffie-hellman-group14-sha1");
      this.ssh2Context.setKeyExchangePreferredPosition("diffie-hellman-group14-sha1", 0);
      if (this.config.containsKey("custom_setKeyReExchangeDisabled"))
        this.ssh2Context.setKeyReExchangeDisabled(this.config.getProperty("custom_setKeyReExchangeDisabled", "").equals("true")); 
      if (this.config.containsKey("custom_setIdleConnectionTimeoutSeconds")) {
        this.ssh2Context.setIdleConnectionTimeoutSeconds(Integer.parseInt(this.config.getProperty("custom_setIdleConnectionTimeoutSeconds", "600")));
        this.ssh2Context.setSocketTimeout(this.ssh2Context.getIdleConnectionTimeoutSeconds() * 1000);
      } 
      if (this.config.getProperty("custom_enableCompression", "false").equals("true"))
        this.ssh2Context.enableCompression(); 
      if (this.config.getProperty("custom_enableFIPSMode", "false").equals("true"))
        this.ssh2Context.enableFIPSMode(); 
      if (this.config.containsKey("custom_setDHGroupExchangeBackwardsCompatible"))
        this.ssh2Context.setDHGroupExchangeBackwardsCompatible(this.config.getProperty("custom_setDHGroupExchangeBackwardsCompatible", "").equals("true")); 
      if (this.config.containsKey("custom_preferredCipher")) {
        this.ssh2Context.setPreferredCipherCS(this.config.getProperty("custom_preferredCipher", ""));
        this.ssh2Context.setPreferredCipherSC(this.config.getProperty("custom_preferredCipher", ""));
        this.ssh2Context.setCipherPreferredPositionCS(this.config.getProperty("custom_preferredCipher", ""), 0);
        this.ssh2Context.setCipherPreferredPositionSC(this.config.getProperty("custom_preferredCipher", ""), 0);
      } 
      if (this.config.containsKey("custom_setDHGroupExchangeKeySize"))
        this.ssh2Context.setDHGroupExchangeKeySize(Integer.parseInt(this.config.getProperty("custom_setDHGroupExchangeKeySize", ""))); 
      if (this.config.containsKey("custom_preferredKex")) {
        this.ssh2Context.setPreferredKeyExchange(this.config.getProperty("custom_preferredKex", ""));
        this.ssh2Context.setKeyExchangePreferredPosition(this.config.getProperty("custom_preferredKex", ""), 0);
      } 
      if (this.config.containsKey("custom_setUseRSAKey") && this.config.getProperty("custom_setUseRSAKey", "true").equals("false")) {
        this.ssh2Context.setPreferredPublicKey("ssh-dss");
        this.ssh2Context.setPublicKeyPreferredPosition("ssh-dss", 0);
      } 
      if (this.config.containsKey("custom_preferredMac")) {
        this.ssh2Context.setPreferredMacCS(this.config.getProperty("custom_preferredMac", ""));
        this.ssh2Context.setPreferredMacSC(this.config.getProperty("custom_preferredMac", ""));
      } 
      if (this.config.containsKey("custom_setMaximumPacketLength"))
        this.ssh2Context.setMaximumPacketLength(Integer.parseInt(this.config.getProperty("custom_setMaximumPacketLength", "35000"))); 
      if (this.config.containsKey("custom_setSessionMaxPacketSize")) {
        this.ssh2Context.setSessionMaxPacketSize(Integer.parseInt(this.config.getProperty("custom_setSessionMaxPacketSize", "35000")));
        this.ssh2Context.setSftpMaxPacketSize(Integer.parseInt(this.config.getProperty("custom_setSessionMaxPacketSize", "35000")));
      } 
      if (this.config.containsKey("custom_setSessionMaxWindowSpace")) {
        this.ssh2Context.setSessionMaxWindowSpace(Integer.parseInt(this.config.getProperty("custom_setSessionMaxWindowSpace", "3000000")));
        this.ssh2Context.setSftpMaxWindowSpace(Integer.parseInt(this.config.getProperty("custom_setSessionMaxWindowSpace", "3000000")));
      } 
      if (this.config.containsKey("custom_enableETM"))
        if (this.config.getProperty("custom_enableETM", "false").equals("true")) {
          this.ssh2Context.enableETM();
        } else {
          this.ssh2Context.disableETM();
        }  
      if (this.config.containsKey("custom_enableNonStandardAlgorithms"))
        if (this.config.getProperty("custom_enableNonStandardAlgorithms", "false").equals("true")) {
          this.ssh2Context.enableNonStandardAlgorithms();
        } else {
          this.ssh2Context.disableNonStandardAlgorithms();
        }  
      VRL u = new VRL(this.url);
      Common.log("SSH_CLIENT", 0, "Connecting to:" + u.getHost() + ":" + u.getPort());
      this.ssh_socket = Common.getSocket("SFTP", u, this.config.getProperty("use_dmz", "false"), "", Integer.parseInt(this.config.getProperty("timeout", "30000")));
      this.ssh_socket.setSoTimeout(600000);
      if (Integer.parseInt(this.config.getProperty("timeout", "0")) > 0) {
        this.ssh2Context.setSocketTimeout(Integer.parseInt(this.config.getProperty("timeout", "0")));
        this.ssh2Context.setIdleConnectionTimeoutSeconds(Integer.parseInt(this.config.getProperty("timeout", "0")) / 1000);
        this.ssh_socket.setSoTimeout(Integer.parseInt(this.config.getProperty("timeout", "0")));
      } 
      this.session = (Ssh2Client)con.connect((SshTransport)new SocketWrapper(this.ssh_socket), this.config.getProperty("username"));
      if (!this.config.getProperty("ssh_private_key", this.config.getProperty("privateKeyFilePath", "")).equals("") && !this.config.getProperty("ssh_private_key", this.config.getProperty("privateKeyFilePath", "")).equalsIgnoreCase("NONE")) {
        String private_key_path = Common.replace_str(this.config.getProperty("ssh_private_key", this.config.getProperty("privateKeyFilePath", "")), "{username}", this.config.getProperty("username"));
        private_key_path = Common.replace_str(private_key_path, "{user_name}", this.config.getProperty("username"));
        Common.log("SSH_CLIENT", 2, log("Using SSH KEY:" + private_key_path));
        PublicKeyAuthentication auth = new PublicKeyAuthentication();
        SshPrivateKeyFile pkfile = SshPrivateKeyFileFactory.parse(new FileInputStream(private_key_path));
        SshKeyPair pair = null;
        if (pkfile.isPassphraseProtected()) {
          try {
            pair = pkfile.toKeyPair(this.config.getProperty("ssh_private_key_pass", this.config.getProperty("privateKeyFilePass", "")));
          } catch (Exception e) {
            if (e.toUpperCase().indexOf("AES256") >= 0) {
              Common.log("SSH_CLIENT", 2, log("WARNING: Max encryption strength is 128bit."));
              log("Strong cryptography extensions are not installed.  Some SSH clients may fail to connect as they expect AES256 to be available.");
              log("The files must be downloaded manually and installed in your Java lib/security folder.");
              log("Find from Google: https://www.google.com/search?q=java+jce+policy");
              log("Java6 result:http://www.oracle.com/technetwork/java/javase/downloads/jce-6-download-429243.html");
              log("Java7 result:http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html");
              log("OS X install location: /System/Library/Frameworks/JavaVM.framework/Versions/1.6.0/Home/lib/security/");
              log("Windows install location: C:\\Program Files\\Java\\jre6\\lib\\security\\");
            } 
            throw e;
          } 
        } else {
          pair = pkfile.toKeyPair(null);
        } 
        auth.setPrivateKey(pair.getPrivateKey());
        auth.setPublicKey(pair.getPublicKey());
        auth.setUsername(this.config.getProperty("username"));
        this.session.authenticate((SshAuthentication)auth);
      } 
      if (this.config.getProperty("ssh_private_key", this.config.getProperty("privateKeyFilePath", "")).equals("") || this.config.getProperty("ssh_private_key", this.config.getProperty("privateKeyFilePath", "")).equalsIgnoreCase("NONE") || this.config.getProperty("ssh_two_factor", this.config.getProperty("twoFactorAuthentication", "")).equals("true")) {
        PasswordAuthentication auth = new PasswordAuthentication();
        auth.setPassword(this.config.getProperty("password"));
        auth.setUsername(this.config.getProperty("username"));
        this.session.authenticate((SshAuthentication)auth);
      } 
      if (!this.session.isAuthenticated())
        throw new Exception("SFTP login failed."); 
      if (this.config.getProperty("dot_default_dir", "false").equals("true"))
        System.getProperties().put("maverick.globalscapeDefaultDirWorkaround", "true"); 
      this.sftp = new SftpClient((SshClient)this.session);
      if (this.session.getRemoteIdentification().indexOf("GXSSSHD") >= 0)
        this.config.put("sftp_7_token", "true"); 
      Common.log("SSH_CLIENT", 0, "Remote server vendor:" + this.session.getRemoteIdentification());
      this.sftp.getSubsystemChannel().setCharsetEncoding("UTF-8");
      if (!System.getProperty("crushftp.client.sftp_max_async", "").equals(""))
        this.sftp.setMaxAsyncRequests(Integer.parseInt(System.getProperty("crushftp.client.sftp_max_async", ""))); 
      executeScript(this.config.getProperty("after_login_script", ""), "");
      this.config.put("default_dir", "/");
      String pwdStr = this.sftp.pwd();
      this.config.put("default_pwd", pwdStr);
      if (pwdStr.startsWith("/"))
        this.config.put("default_dir", String.valueOf(pwdStr) + (pwdStr.endsWith("/") ? "" : "/")); 
    } catch (SshException e) {
      Common.log("SSH_CLIENT", 2, log((Exception)e));
      if (e.getCause() != null)
        Common.log("SSH_CLIENT", 2, log(e.getCause())); 
      Common.log("SSH_CLIENT", 2, log("Msg1:" + e.getMessage()));
      Common.log("SSH_CLIENT", 2, log("Msg2:" + e.getLocalizedMessage()));
      Common.log("SSH_CLIENT", 2, log("Reason:" + e.getReason()));
    } 
  }
  
  public Properties stat(String path) throws Exception {
    reconnect();
    this.config.put("simple", "true");
    setThreadName();
    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1); 
    Properties dir_item = null;
    try {
      String path2 = path;
      if (!path2.startsWith("/"))
        path2 = "/" + path2; 
      SftpFileAttributes attrs = this.sftp.stat(path2);
      dir_item = new Properties();
      if (this.url.endsWith("/") && path.startsWith("/")) {
        dir_item.put("url", String.valueOf(this.url) + path.substring(1));
      } else {
        dir_item.put("url", String.valueOf(this.url) + path);
      } 
      dir_item.put("local", "false");
      dir_item.put("protocol", "sftp");
      dir_item.put("local", "false");
      dir_item.put("dir", Common.all_but_last(path));
      dir_item.put("name", Common.last(path));
      dir_item.put("root_dir", Common.all_but_last(path));
      dir_item.put("type", attrs.isDirectory() ? "DIR" : "FILE");
      String perms = attrs.getPermissionsString();
      if (perms.trim().equals(""))
        perms = String.valueOf(attrs.isDirectory() ? "d" : "-") + "rwxrwxrwx"; 
      while (perms.length() < 10)
        perms = String.valueOf(perms) + "-"; 
      dir_item.put("permissions", perms);
      dir_item.put("num_items", "1");
      String gid = attrs.getGID();
      if (gid.trim().equals(""))
        gid = "0"; 
      String uid = attrs.getUID();
      if (uid.trim().equals(""))
        uid = "0"; 
      dir_item.put("owner", (new StringBuffer(String.valueOf(uid))).toString());
      dir_item.put("group", (new StringBuffer(String.valueOf(gid))).toString());
      dir_item.put("size", attrs.getSize());
      Date d = attrs.getModifiedDateTime();
      dir_item.put("modified", (new StringBuffer(String.valueOf(d.getTime()))).toString());
      dir_item.put("month", this.mmm.format(d));
      dir_item.put("day", this.dd.format(d));
      dir_item.put("time_or_year", this.yyyy.format(d));
      if (dir_item.getProperty("type").equalsIgnoreCase("DIR"))
        dir_item.put("size", "1"); 
      dir_item.put("simple", (new StringBuffer(String.valueOf(this.config.getProperty("simple", "").equals("true")))).toString());
      if (this.config.getProperty("sftp_7_token", "false").equals("true")) {
        Vector v = new Vector();
        list(Common.all_but_last(path), v);
        for (int x = 0; x < v.size(); x++) {
          Properties p = v.elementAt(x);
          if (p.getProperty("name").equals(dir_item.getProperty("name"))) {
            dir_item.put("size", p.getProperty("size"));
            break;
          } 
        } 
      } 
    } catch (SftpStatusException e) {
      if (System.getProperty("crushftp.isTestCall" + Thread.currentThread().getId(), "false").equals("true"))
        throw e; 
      return null;
    } 
    return dir_item;
  }
  
  public Vector list(String path, Vector list) throws Exception {
    reconnect();
    executeScript(this.config.getProperty("before_dir_script", ""), path.trim());
    setThreadName();
    if (path.startsWith("/"))
      path = path.substring(1); 
    VRL vrl = new VRL(this.url);
    Common.log("SSH_CLIENT", 2, "ls " + vrl.safe() + path);
    if ((String.valueOf(vrl.getPath()) + path).endsWith("/")) {
      this.sftp.cd(String.valueOf(vrl.getPath()) + path);
    } else {
      this.sftp.cd(Common.all_but_last(String.valueOf(vrl.getPath()) + path));
    } 
    SftpFile[] v = new SftpFile[0];
    try {
      v = this.sftp.ls(".");
    } catch (SftpStatusException ee) {
      try {
        if (ee.indexOf("No such file") >= 0) {
          Common.log("SSH_CLIENT", 2, log((String)ee));
          Common.log("SSH_CLIENT", 2, "Trying blank ls param...");
          v = this.sftp.ls();
        } 
      } catch (SftpStatusException e) {
        if (e.indexOf("Failed to open") >= 0) {
          Common.log("SSH_CLIENT", 2, log((String)e));
        } else {
          throw e;
        } 
      } 
    } 
    Common.log("SSH_CLIENT", 2, "DIR Count:" + v.length);
    int x;
    for (x = 0; x < v.length; x++) {
      Properties dir_item = new Properties();
      String data = v[x].getLongname();
      if (data == null) {
        Date d = v[x].getAttributes().getModifiedDateTime();
        data = SftpClient.formatLongname(v[x]);
        Common.log("SSH_CLIENT", 2, "RAW long name (" + v[x].isDirectory() + "):" + data);
        if (data.indexOf("                        ") >= 0 || (data.trim().startsWith("-") && v[x].isDirectory())) {
          String perms = v[x].getAttributes().getPermissionsString();
          if (perms.trim().equals(""))
            perms = "rwxrwxrwx"; 
          while (perms.length() < 10)
            perms = String.valueOf(perms) + "-"; 
          String designator = "";
          if (v[x].isDirectory()) {
            if (!perms.trim().startsWith("d"))
              designator = "d"; 
          } else if (!perms.trim().startsWith("-")) {
            designator = "-";
          } 
          data = String.valueOf(designator) + perms + " ";
          data = String.valueOf(data) + "1 ";
          String gid = v[x].getAttributes().getGID();
          if (gid.trim().equals(""))
            gid = "0"; 
          String uid = v[x].getAttributes().getUID();
          if (uid.trim().equals(""))
            uid = "0"; 
          data = String.valueOf(data) + gid + " ";
          data = String.valueOf(data) + uid + " ";
          data = String.valueOf(data) + v[x].getAttributes().getSize() + " ";
          data = String.valueOf(data) + this.mmm.format(d) + " ";
          data = String.valueOf(data) + this.dd.format(d) + " ";
          data = String.valueOf(data) + this.yyyy.format(d) + " ";
          data = String.valueOf(data) + Common.last(v[x].getFilename());
        } 
        dir_item.put("modified", (new StringBuffer(String.valueOf(d.getTime()))).toString());
      } 
      if (data != null) {
        data = data.replaceAll(" domain users ", " domain_users ");
        Common.log("SSH_CLIENT", 2, data);
        try {
          if ((String.valueOf(vrl.getPath()) + path).endsWith("/")) {
            dir_item.put("root_dir", String.valueOf(vrl.getPath()) + path);
          } else {
            dir_item.put("root_dir", Common.all_but_last(String.valueOf(vrl.getPath()) + path));
          } 
          dir_item.put("local", "false");
          dir_item.put("protocol", "sftp");
          if (!data.toUpperCase().startsWith("TOTAL ")) {
            StringTokenizer get_em = new StringTokenizer(data, " ");
            boolean normalMode = true;
            if (data.toUpperCase().startsWith("D") || data.toUpperCase().startsWith("L")) {
              dir_item.put("type", "DIR");
            } else if (data.toUpperCase().startsWith("-")) {
              dir_item.put("type", "FILE");
            } else {
              normalMode = false;
              dir_item.put("type", v[x].isDirectory() ? "DIR" : "FILE");
              dir_item.put("permissions", String.valueOf(v[x].isDirectory() ? "d" : "-") + "rwxrwxrwx");
              dir_item.put("num_items", "1");
              dir_item.put("owner", "user");
              dir_item.put("group", "group");
              dir_item.put("size", get_em.nextToken().trim());
              dir_item.put("month", get_em.nextToken().trim());
              String day = get_em.nextToken().trim();
              day = day.substring(0, day.length() - 1);
              dir_item.put("day", day);
              dir_item.put("year", get_em.nextToken().trim());
              dir_item.put("time_or_year", get_em.nextToken().trim());
            } 
            if (normalMode) {
              String perms = get_em.nextToken().trim();
              while (perms.length() < 10)
                perms = String.valueOf(perms) + "-"; 
              dir_item.put("permissions", perms);
              dir_item.put("num_items", get_em.nextToken().trim());
              String user_part = "owner";
              String group_part = "group";
              if (data.indexOf("                   ") < 0) {
                user_part = get_em.nextToken().trim();
                group_part = get_em.nextToken().trim();
                dir_item.put("owner", user_part.replace('\\', '_'));
                dir_item.put("group", group_part.replace('\\', '_'));
              } else {
                dir_item.put("owner", "owner");
                dir_item.put("group", "group");
              } 
              boolean skip_size = false;
              if (data.indexOf("       root      ") >= 0)
                try {
                  Long.parseLong(dir_item.getProperty("group"));
                  try {
                    Long.parseLong(dir_item.getProperty("owner"));
                  } catch (Exception e) {
                    skip_size = true;
                    dir_item.put("size", dir_item.getProperty("group"));
                    dir_item.put("group", dir_item.getProperty("owner"));
                  } 
                } catch (Exception exception) {} 
              String size_part = dir_item.getProperty("size", "0");
              if (!skip_size) {
                size_part = get_em.nextToken();
                while (!Common.isNumeric(size_part))
                  size_part = get_em.nextToken().trim(); 
              } 
              if (!skip_size)
                dir_item.put("size", size_part.trim()); 
              dir_item.put("month", get_em.nextToken().trim());
              dir_item.put("day", get_em.nextToken().trim());
              dir_item.put("time_or_year", get_em.nextToken().trim());
            } 
            String name_data = get_em.nextToken();
            String searchName = String.valueOf(dir_item.getProperty("time_or_year")) + " " + name_data;
            name_data = data.substring(data.indexOf(name_data, data.indexOf(searchName) + dir_item.getProperty("time_or_year").length() + 1));
            dir_item.put("name", name_data);
            if (data.toUpperCase().startsWith("L") && name_data.indexOf(" ->") >= 0) {
              dir_item.put("name", name_data.substring(0, name_data.indexOf(" ->")));
              dir_item.put("permissions", "drwxrwxrwx");
            } 
            if (!normalMode && name_data.endsWith("/")) {
              name_data = name_data.substring(0, name_data.length() - 1);
              dir_item.put("type", "DIR");
              dir_item.put("permissions", "drwxrwxrwx");
              dir_item.put("name", name_data);
            } else if (normalMode && dir_item.getProperty("type", "").equalsIgnoreCase("DIR") && name_data.endsWith("/")) {
              name_data = name_data.substring(0, name_data.length() - 1);
              dir_item.put("name", name_data);
            } 
            if (this.url.endsWith("/") && path.startsWith("/")) {
              dir_item.put("url", String.valueOf(this.url) + path.substring(1) + dir_item.getProperty("name"));
            } else {
              dir_item.put("url", String.valueOf(this.url) + path + dir_item.getProperty("name"));
            } 
            dir_item.put("local", "false");
            dir_item.put("dir", path);
            if (!dir_item.getProperty("name").equals(".") && !dir_item.getProperty("name").equals("..") && !dir_item.getProperty("name").equals("./") && !dir_item.getProperty("name").equals("../"))
              list.addElement(dir_item); 
          } 
          if (dir_item.getProperty("type").equalsIgnoreCase("DIR"))
            dir_item.put("size", "1"); 
        } catch (Exception eee) {
          if (eee.indexOf("Interrupted") >= 0)
            throw eee; 
        } 
      } 
    } 
    for (x = 0; x < list.size(); x++) {
      Properties dir_item = list.elementAt(x);
      if (dir_item != null) {
        SimpleDateFormat mmddyyyy = new SimpleDateFormat("MMM dd yyyy HH:mm", Locale.US);
        SimpleDateFormat yyyy = new SimpleDateFormat("yyyy");
        Date modified = new Date();
        String time_or_year = dir_item.getProperty("time_or_year", "");
        String year = yyyy.format(new Date(Long.parseLong(dir_item.getProperty("modified", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString()))));
        String time = "00:00";
        if (time_or_year.indexOf(":") < 0) {
          year = time_or_year;
        } else {
          time = time_or_year;
        } 
        if (dir_item.containsKey("year"))
          year = dir_item.getProperty("year"); 
        try {
          modified = mmddyyyy.parse(String.valueOf(dir_item.getProperty("month", "")) + " " + dir_item.getProperty("day", "") + " " + year + " " + time);
        } catch (Exception e) {
          Common.log("SSH_CLIENT", 1, e);
        } 
        if (modified.getTime() > System.currentTimeMillis() + 172800000L) {
          Calendar calendar = new GregorianCalendar();
          calendar.setTime(modified);
          calendar.add(1, -1);
          modified = calendar.getTime();
        } 
        if (!dir_item.containsKey("modified"))
          dir_item.put("modified", (new StringBuffer(String.valueOf(modified.getTime()))).toString()); 
      } 
    } 
    executeScript(this.config.getProperty("after_dir_script", ""), path.trim());
    return list;
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary) throws Exception {
    reconnect();
    executeScript(this.config.getProperty("before_download_script", ""), path.trim());
    setThreadName();
    Common.log("SSH_CLIENT", 2, "get " + path);
    if (startPos < 0L)
      startPos = 0L; 
    if (path.indexOf("$") >= 0 && path.indexOf(";") >= 0)
      path = path.substring(0, path.lastIndexOf(";")); 
    if (this.config.getProperty("before_download_script", "").toUpperCase().startsWith("SIMPLE"))
      path = path.substring(path.lastIndexOf("/") + 1); 
    this.in = this.sftp.getInputStream(path, startPos);
    if (endPos > 0L)
      this.in = getLimitedInputStream(this.in, startPos, endPos); 
    return this.in;
  }
  
  protected OutputStream upload3(String path2, long startPos, boolean truncate, boolean binary) throws Exception {
    reconnect();
    executeScript(this.config.getProperty("before_upload_script", ""), path2.trim());
    setThreadName();
    Common.log("SSH_CLIENT", 2, "put " + path2);
    if (path2.endsWith("/"))
      path2 = path2.substring(0, path2.length() - 1); 
    String path = path2;
    try {
      if (truncate && startPos <= 0L)
        this.sftp.rm(path); 
    } catch (Exception exception) {}
    Properties sockets = Common.getConnectedSocks(false);
    Socket sock1 = (Socket)sockets.remove("sock1");
    Socket sock2 = (Socket)sockets.remove("sock2");
    Properties upload_status = new Properties();
    upload_status.put("status", "");
    Worker.startWorker(new Runnable(this, sock1, path, startPos, upload_status) {
          final SFTPClient this$0;
          
          private final Socket val$sock1;
          
          private final String val$path;
          
          private final long val$startPos;
          
          private final Properties val$upload_status;
          
          public void run() {
            try {
              this.this$0.sftp.put(this.val$sock1.getInputStream(), this.val$path, (this.val$startPos < 0L) ? 0L : this.val$startPos);
              this.val$upload_status.put("status", "complete");
            } catch (Exception e) {
              Common.log("SSH_CLIENT", 0, this.this$0.log(e));
              this.val$upload_status.put("status", "ERROR:" + e);
            } 
          }
        });
    return new null.OutputWrapper(this, sock2.getOutputStream(), upload_status, path);
  }
  
  public boolean rename(String path1, String path2) throws Exception {
    reconnect();
    this.recent_mkdirs.removeAllElements();
    setThreadName();
    Common.log("SSH_CLIENT", 2, "rename " + path1 + "   to   " + path2);
    try {
      this.sftp.rename(path1, path2);
    } catch (Exception e) {
      Common.log("SSH_CLIENT", 1, "Rename failed:" + path1 + " -->" + path2);
      Common.log("SSH_CLIENT", 1, e);
      return false;
    } 
    return true;
  }
  
  public boolean delete(String path) throws Exception {
    reconnect();
    this.recent_mkdirs.removeAllElements();
    setThreadName();
    try {
      this.sftp.rm(path);
    } catch (SftpStatusException e) {
      if (e.toUpperCase().indexOf("NO SUCH FILE") >= 0 || e.toUpperCase().indexOf("NOT A VALID FILE PATH") >= 0)
        return true; 
      Common.log("SSH_CLIENT", 1, (Exception)e);
      return false;
    } 
    return true;
  }
  
  public void logout() throws Exception {
    executeScript(this.config.getProperty("before_logout_script", ""), "");
    setThreadName();
    try {
      if (this.ssh2Context != null)
        this.ssh2Context.setSocketTimeout(2000); 
    } catch (Exception exception) {}
    try {
      if (this.sftp != null) {
        Thread t = new Thread(new Runnable(this) {
              final SFTPClient this$0;
              
              public void run() {
                try {
                  this.this$0.sftp.quit();
                } catch (Exception exception) {}
              }
            });
        t.start();
        t.join(3000L);
        t.interrupt();
      } 
      this.sftp = null;
    } catch (Exception exception) {}
    try {
      if (this.session != null) {
        Thread t = new Thread(new Runnable(this) {
              final SFTPClient this$0;
              
              public void run() {
                try {
                  this.this$0.session.disconnect();
                } catch (Exception exception) {}
              }
            });
        t.start();
        t.join(3000L);
        t.interrupt();
      } 
    } catch (Exception exception) {}
    SshConnector.removeEventListener(this.uniqueId);
    try {
      close();
    } catch (Exception exception) {}
    try {
      if (this.ssh_socket != null)
        this.ssh_socket.close(); 
    } catch (Exception exception) {}
  }
  
  public boolean makedir(String path) throws Exception {
    reconnect();
    setThreadName();
    try {
      this.sftp.mkdir(path);
    } catch (Exception e) {
      if (e.indexOf("exists") >= 0)
        return true; 
      throw e;
    } 
    return true;
  }
  
  public boolean makedirs(String path) throws Exception {
    reconnect();
    boolean ok = false;
    try {
      ok = makedir(path);
    } catch (Exception e) {
      Common.log("SSH_CLIENT", 1, "MKDIR recursive failed:" + path + " so we will try recursive. (" + e + ")");
      if (e.indexOf("already exists") >= 0)
        ok = true; 
      if (!ok) {
        String[] parts = path.split("/");
        String path2 = "";
        for (int x = 0; x < parts.length; x++) {
          path2 = String.valueOf(path2) + parts[x] + "/";
          if (x >= 1)
            if (this.recent_mkdirs.indexOf(path2) < 0) {
              this.recent_mkdirs.addElement(path2);
              if (stat(path2) == null) {
                try {
                  ok = makedir(path2);
                } catch (Exception ee) {
                  Common.log("SSH_CLIENT", 1, "MKDIR individual:" + path2 + " failed, moving to next. (" + ee + ")");
                } 
              } else {
                ok = true;
              } 
            }  
        } 
      } 
    } 
    return ok;
  }
  
  public boolean mdtm(String path, long modified) throws Exception {
    reconnect();
    setThreadName();
    try {
      SftpFileAttributes attrs = this.sftp.stat(path);
      attrs.setTimes(new UnsignedInteger64(modified / 1000L), new UnsignedInteger64(modified / 1000L));
      this.sftp.getSubsystemChannel().setAttributes(path, attrs);
    } catch (Exception e) {
      Common.log("SSH_CLIENT", 1, e);
      return false;
    } 
    return true;
  }
  
  public void executeScript(String script, String verb_data) throws Exception {
    if (script == null || script.trim().equals(""))
      return; 
    BufferedReader br = new BufferedReader(new StringReader(script));
    while (true) {
      String command = br.readLine();
      if (command == null)
        break; 
      Common.log("SSH_CLIENT", 2, log("SCRIPT:" + command));
      if (command.startsWith("ls ") || command.startsWith("dir ") || command.startsWith("list ")) {
        Common.log("SSH_CLIENT", 2, log("Trying openDirectory:" + command.substring(command.indexOf(" ") + 1)));
        SftpFile sf = this.sftp.getSubsystemChannel().openDirectory(command.substring(command.indexOf(" ") + 1), true);
        this.sftp.getSubsystemChannel().closeFile(sf);
        continue;
      } 
      if (command.startsWith("rm ") || command.startsWith("del ")) {
        this.sftp.rm(command.substring(command.indexOf(" ") + 1));
        continue;
      } 
      if (command.startsWith("mv ") || command.startsWith("rename ")) {
        this.sftp.rename(command.substring(3).split(":")[0], command.substring(3).split(":")[1]);
        continue;
      } 
      if (command.startsWith("cd ") || command.startsWith("cwd ")) {
        this.sftp.cd(command.substring(command.indexOf(" ") + 1));
        continue;
      } 
      if (command.startsWith("chgrp ")) {
        this.sftp.chgrp(command.substring(6).split(":")[0], verb_data);
        continue;
      } 
      if (command.startsWith("chown ")) {
        this.sftp.chown(command.substring(6).split(":")[0], verb_data);
        continue;
      } 
      if (command.startsWith("chmod ")) {
        this.sftp.chmod(Integer.parseInt(command.substring(6).split(":")[0], 8), verb_data);
        continue;
      } 
      if (command.startsWith("mkd "))
        this.sftp.mkdir(command.substring(command.indexOf(" ") + 1)); 
    } 
  }
  
  class SftpLogger implements EventListener {
    final int EVENT_LOG = 110;
    
    final int EVENT_DEBUG_LOG = 111;
    
    final int EVENT_EXCEPTION_LOG = 112;
    
    final String ATTRIBUTE_LOG_MESSAGE = "LOG_MESSAGE";
    
    final String ATTRIBUTE_THROWABLE = "THROWABLE";
    
    boolean ignoreLogEvents;
    
    final SFTPClient this$0;
    
    public void setProduct(String product) {}
    
    public String getProduct() {
      return "";
    }
    
    public SftpLogger(SFTPClient this$0) {
      this.this$0 = this$0;
      this.EVENT_LOG = 110;
      this.EVENT_DEBUG_LOG = 111;
      this.EVENT_EXCEPTION_LOG = 112;
      this.ATTRIBUTE_LOG_MESSAGE = "LOG_MESSAGE";
      this.ATTRIBUTE_THROWABLE = "THROWABLE";
      this.ignoreLogEvents = false;
    }
    
    public SftpLogger(SFTPClient this$0, boolean ignoreLogEvents) {
      this.this$0 = this$0;
      this.EVENT_LOG = 110;
      this.EVENT_DEBUG_LOG = 111;
      this.EVENT_EXCEPTION_LOG = 112;
      this.ATTRIBUTE_LOG_MESSAGE = "LOG_MESSAGE";
      this.ATTRIBUTE_THROWABLE = "THROWABLE";
      this.ignoreLogEvents = false;
      setIgnoreLogEvents(ignoreLogEvents);
    }
    
    public void setIgnoreLogEvents(boolean ignore) {
      this.ignoreLogEvents = ignore;
    }
    
    public void processEvent(Event evt) {
      if ((evt.getId() == 110 || evt.getId() == 111 || evt.getId() == 112) && !this.ignoreLogEvents) {
        Common.log("SSH_CLIENT", 0, (new StringBuffer(String.valueOf(evt.getAllAttributes()))).toString());
        if (evt.getId() == 110) {
          Common.log("SSH_CLIENT", 0, (String)evt.getAttribute("LOG_MESSAGE"));
        } else if (evt.getId() == 111) {
          Common.log("SSH_CLIENT", 2, (String)evt.getAttribute("LOG_MESSAGE"));
        } else if (evt.getId() == 112) {
          Common.log("SSH_CLIENT", 1, this.this$0.log((String)evt.getAttribute("LOG_MESSAGE")));
          Common.log("SSH_CLIENT", 1, this.this$0.log((Throwable)evt.getAttribute("THROWABLE")));
        } 
      } else {
        Common.log("SSH_CLIENT", 0, (new StringBuffer()).append(J2SSHEventCodes.messageCodes.get(new Integer(evt.getId()))).append(evt.getAllAttributes()).toString());
      } 
    }
  }
}
