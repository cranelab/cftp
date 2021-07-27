package com.crushftp.client;

import com.crushftp.tunnel2.Tunnel2;
import com.didisoft.pgp.KeyStore;
import com.didisoft.pgp.PGPLib;
import com.didisoft.pgp.inspect.PGPInspectLib;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

public class GenericClient {
  GenericClient thisObj = null;
  
  InputStream in = null;
  
  OutputStream out = null;
  
  InputStream in3 = null;
  
  OutputStream out3 = null;
  
  String url = null;
  
  Properties config = new Properties();
  
  Properties transfer_info = new Properties();
  
  public static final String[] months = new String[] { 
      "not zero based", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", 
      "Oct", "Nov", "Dec" };
  
  public final SimpleDateFormat yyyymmddHHmm = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
  
  public final SimpleDateFormat rfc1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
  
  public final SimpleDateFormat mm = new SimpleDateFormat("MM", Locale.US);
  
  public final SimpleDateFormat mmm = new SimpleDateFormat("MMM", Locale.US);
  
  public final SimpleDateFormat dd = new SimpleDateFormat("dd", Locale.US);
  
  public final SimpleDateFormat yyyy = new SimpleDateFormat("yyyy", Locale.US);
  
  public final SimpleDateFormat hhmm = new SimpleDateFormat("HH:mm", Locale.US);
  
  static Common common_code = null;
  
  PGPLib pgp = null;
  
  Vector logQueue = null;
  
  String logHeader = "";
  
  Properties statCache = null;
  
  public static Object tunnelLock = new Object();
  
  public static Object socketCleanerLock = new Object();
  
  static Thread socketCleaner = null;
  
  public GenericClient(String logHeader, Vector logQueue) {
    this.thisObj = this;
    this.logHeader = logHeader;
    this.logQueue = logQueue;
    startSocketCleaner();
  }
  
  public String log(String s) {
    String s2 = String.valueOf(Common.last(getClass().getName().replace('.', '/'))) + ":" + this.logHeader + s;
    if (this.logQueue != null) {
      this.logQueue.addElement(String.valueOf(Common.last(getClass().getName().replace('.', '/'))) + ":" + this.logHeader + s);
    } else {
      System.out.print(String.valueOf(Common.last(getClass().getName().replace('.', '/'))) + ":" + new Date() + ":" + this.logHeader + s + "\r\n");
    } 
    return s2;
  }
  
  public void setCache(Properties statCache) {
    this.statCache = statCache;
  }
  
  public void setUrl(String url) {
    this.url = url;
  }
  
  public Properties getCache() {
    return this.statCache;
  }
  
  public String log(Throwable e) {
    String s2 = String.valueOf(log(Thread.currentThread().getName())) + "\r\n";
    s2 = String.valueOf(s2) + log((String)e) + "\r\n";
    StackTraceElement[] ste = e.getStackTrace();
    for (int x = 0; x < ste.length; x++)
      s2 = String.valueOf(s2) + log(String.valueOf(ste[x].getClassName()) + "." + ste[x].getMethodName() + ":" + ste[x].getLineNumber()) + "\r\n"; 
    return s2;
  }
  
  public String log(Exception e) {
    String s2 = String.valueOf(log(Thread.currentThread().getName())) + "\r\n";
    s2 = String.valueOf(s2) + log((String)e) + "\r\n";
    StackTraceElement[] ste = e.getStackTrace();
    for (int x = 0; x < ste.length; x++)
      s2 = String.valueOf(s2) + log(String.valueOf(ste[x].getClassName()) + "." + ste[x].getMethodName() + ":" + ste[x].getLineNumber()) + "\r\n"; 
    return s2;
  }
  
  public String login(String username, String password, String clientid) throws Exception {
    if (password.startsWith("DES:"))
      try {
        password = Common.encryptDecrypt(password.substring(4), false);
      } catch (Exception e) {
        password = Common.encryptDecrypt(password.replace('\\', '/').substring(4), false);
      }  
    if (this.config.getProperty("timeout", "").equals("") || this.config.getProperty("timeout", "").equals("60"))
      this.config.put("timeout", "600000"); 
    return login2(username, password, clientid);
  }
  
  public String login2(String username, String password, String clientid) throws Exception {
    return "";
  }
  
  public void setupConfig(Properties prefs, Properties item) {
    Enumeration keys = prefs.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      if (key.startsWith("config_")) {
        setConfig(key.substring("config_".length()), prefs.get(key));
        continue;
      } 
      if (!this.config.containsKey(key))
        setConfig(key, prefs.get(key)); 
    } 
    if (!prefs.getProperty("keystore_path", "").equals(""))
      setConfig("keystore_path", prefs.getProperty("keystore_path", "")); 
    if (!prefs.getProperty("trustore_path", "").equals(""))
      setConfig("trustore_path", prefs.getProperty("trustore_path", "")); 
    if (!prefs.getProperty("keystore_pass", "").equals(""))
      setConfig("keystore_pass", prefs.getProperty("keystore_pass", "")); 
    if (!prefs.getProperty("key_pass", "").equals(""))
      setConfig("key_pass", prefs.getProperty("key_pass", "")); 
    if (!prefs.getProperty("acceptAnyCert", "").equals(""))
      setConfig("acceptAnyCert", prefs.getProperty("acceptAnyCert", "false")); 
    if (!prefs.getProperty("ssh_private_key", "").equals(""))
      setConfig("ssh_private_key", prefs.getProperty("ssh_private_key", "")); 
    if (!prefs.getProperty("ssh_private_key_pass", "").equals(""))
      setConfig("ssh_private_key_pass", prefs.getProperty("ssh_private_key_pass", "")); 
    if (!prefs.getProperty("ssh_two_factor", "").equals(""))
      setConfig("ssh_two_factor", prefs.getProperty("ssh_two_factor", "")); 
    if (item != null) {
      if (item.containsKey("vItem"))
        setupConfig(prefs, (Properties)item.get("vItem")); 
      setConfig("use_dmz", item.getProperty("use_dmz", "false"));
      setConfig("pasv", item.getProperty("pasv", "true"));
      setConfig("ascii", item.getProperty("ascii", "false"));
      setConfig("no_os400", item.getProperty("no_os400", "false"));
      setConfig("simple", item.getProperty("simple", "false"));
      setConfig("cwd_list", item.getProperty("cwd_list", "false"));
      setConfig("no_stat", item.getProperty("no_stat", "false"));
      setConfig("secure_data", item.getProperty("secure_data", "true"));
      setConfig("server_side_encrypt", item.getProperty("server_side_encrypt", "false"));
      setConfig("s3_accelerate", item.getProperty("s3_accelerate", "false"));
      setConfig("s3_bucket_in_path", item.getProperty("s3_bucket_in_path", "false"));
      setConfig("s3_stat_head_calls_double", item.getProperty("s3_stat_head_calls_double", "false"));
      setConfig("s3_stat_head_calls", item.getProperty("s3_stat_head_calls", "true"));
      setConfig("multithreaded_s3", item.getProperty("multithreaded_s3", "false"));
      setConfig("server_side_encrypt_kms", item.getProperty("server_side_encrypt_kms", ""));
      if (!item.getProperty("s3_sha256", "").equals(""))
        setConfig("s3_sha256", item.getProperty("s3_sha256", "")); 
      if (!item.getProperty("s3_partial", "").equals(""))
        setConfig("s3_partial", item.getProperty("s3_partial", "")); 
      if (!item.getProperty("before_login_script", "").equals(""))
        setConfig("before_login_script", item.getProperty("before_login_script", "")); 
      if (!item.getProperty("after_login_script", "").equals(""))
        setConfig("after_login_script", item.getProperty("after_login_script", "")); 
      if (!item.getProperty("before_logout_script", "").equals(""))
        setConfig("before_logout_script", item.getProperty("before_logout_script", "")); 
      if (!item.getProperty("before_download_script", "").equals(""))
        setConfig("before_download_script", item.getProperty("before_download_script", "")); 
      if (!item.getProperty("after_download_script", "").equals(""))
        setConfig("after_download_script", item.getProperty("after_download_script", "")); 
      if (!item.getProperty("before_upload_script", "").equals(""))
        setConfig("before_upload_script", item.getProperty("before_upload_script", "")); 
      if (!item.getProperty("after_upload_script", "").equals(""))
        setConfig("after_upload_script", item.getProperty("after_upload_script", "")); 
      if (!item.getProperty("before_dir_script", "").equals(""))
        setConfig("before_dir_script", item.getProperty("before_dir_script", "")); 
      if (!item.getProperty("after_dir_script", "").equals(""))
        setConfig("after_dir_script", item.getProperty("after_dir_script", "")); 
      if (!item.getProperty("custom_setKeyReExchangeDisabled", "").equals(""))
        setConfig("custom_setKeyReExchangeDisabled", item.getProperty("custom_setKeyReExchangeDisabled", "")); 
      if (!item.getProperty("custom_setIdleConnectionTimeoutSeconds", "").equals(""))
        setConfig("custom_setIdleConnectionTimeoutSeconds", item.getProperty("custom_setIdleConnectionTimeoutSeconds", "")); 
      if (!item.getProperty("custom_enableCompression", "").equals(""))
        setConfig("custom_enableCompression", item.getProperty("custom_enableCompression", "")); 
      if (!item.getProperty("custom_enableFIPSMode", "").equals(""))
        setConfig("custom_enableFIPSMode", item.getProperty("custom_enableFIPSMode", "")); 
      if (!item.getProperty("custom_setDHGroupExchangeBackwardsCompatible", "").equals(""))
        setConfig("custom_setDHGroupExchangeBackwardsCompatible", item.getProperty("custom_setDHGroupExchangeBackwardsCompatible", "")); 
      if (!item.getProperty("custom_preferredCipher", "").equals(""))
        setConfig("custom_preferredCipher", item.getProperty("custom_preferredCipher", "")); 
      if (!item.getProperty("custom_setDHGroupExchangeKeySize", "").equals(""))
        setConfig("custom_setDHGroupExchangeKeySize", item.getProperty("custom_setDHGroupExchangeKeySize", "")); 
      if (!item.getProperty("custom_preferredKex", "").equals(""))
        setConfig("custom_preferredKex", item.getProperty("custom_preferredKex", "")); 
      if (!item.getProperty("custom_setUseRSAKey", "").equals(""))
        setConfig("custom_setUseRSAKey", item.getProperty("custom_setUseRSAKey", "")); 
      if (!item.getProperty("custom_preferredMac", "").equals(""))
        setConfig("custom_preferredMac", item.getProperty("custom_preferredMac", "")); 
      if (!item.getProperty("custom_setMaximumPacketLength", "").equals(""))
        setConfig("custom_setMaximumPacketLength", item.getProperty("custom_setMaximumPacketLength", "")); 
      if (!item.getProperty("custom_setSessionMaxPacketSize", "").equals(""))
        setConfig("custom_setSessionMaxPacketSize", item.getProperty("custom_setSessionMaxPacketSize", "")); 
      if (!item.getProperty("custom_setSessionMaxWindowSpace", "").equals(""))
        setConfig("custom_setSessionMaxWindowSpace", item.getProperty("custom_setSessionMaxWindowSpace", "")); 
      if (!item.getProperty("custom_enableETM", "").equals(""))
        setConfig("custom_enableETM", item.getProperty("custom_enableETM", "")); 
      if (!item.getProperty("custom_enableNonStandardAlgorithms", "").equals(""))
        setConfig("custom_enableNonStandardAlgorithms", item.getProperty("custom_enableNonStandardAlgorithms", "")); 
      if (!item.getProperty("delete_xml_representation_files", "").equals(""))
        setConfig("delete_xml_representation_files", item.getProperty("delete_xml_representation_files", "")); 
      if (!item.getProperty("dfs_enabled", "").equals(""))
        setConfig("dfs_enabled", item.getProperty("dfs_enabled", "")); 
      if (!item.getProperty("write_timeout", "").equals(""))
        setConfig("write_timeout", item.getProperty("write_timeout", "")); 
      if (!item.getProperty("read_timeout", "").equals(""))
        setConfig("read_timeout", item.getProperty("read_timeout", "")); 
      if (!item.getProperty("smb3_required_signing", "").equals(""))
        setConfig("smb3_required_signing", item.getProperty("smb3_required_signing", "")); 
      if (!item.getProperty("sas_token", "").equals(""))
        setConfig("sas_token", item.getProperty("sas_token", "")); 
      if (!item.getProperty("upload_blob_type", "").equals(""))
        setConfig("upload_blob_type", item.getProperty("upload_blob_type", "")); 
      if (!item.getProperty("team_drive", "").equals(""))
        setConfig("team_drive", item.getProperty("team_drive", "")); 
    } else {
      if (prefs.containsKey("vItem"))
        setupConfig(prefs, (Properties)prefs.get("vItem")); 
      if (!prefs.getProperty("s3_sha256", "").equals(""))
        setConfig("s3_sha256", prefs.getProperty("s3_sha256", "")); 
      if (!prefs.getProperty("s3_partial", "").equals(""))
        setConfig("s3_partial", prefs.getProperty("s3_partial", "")); 
      if (!prefs.getProperty("before_login_script", "").equals(""))
        setConfig("before_login_script", prefs.getProperty("before_login_script", "")); 
      if (!prefs.getProperty("after_login_script", "").equals(""))
        setConfig("after_login_script", prefs.getProperty("after_login_script", "")); 
      if (!prefs.getProperty("before_logout_script", "").equals(""))
        setConfig("before_logout_script", prefs.getProperty("before_logout_script", "")); 
      if (!prefs.getProperty("before_download_script", "").equals(""))
        setConfig("before_download_script", prefs.getProperty("before_download_script", "")); 
      if (!prefs.getProperty("after_download_script", "").equals(""))
        setConfig("after_download_script", prefs.getProperty("after_download_script", "")); 
      if (!prefs.getProperty("before_upload_script", "").equals(""))
        setConfig("before_upload_script", prefs.getProperty("before_upload_script", "")); 
      if (!prefs.getProperty("after_upload_script", "").equals(""))
        setConfig("after_upload_script", prefs.getProperty("after_upload_script", "")); 
      if (!prefs.getProperty("before_dir_script", "").equals(""))
        setConfig("before_dir_script", prefs.getProperty("before_dir_script", "")); 
      if (!prefs.getProperty("after_dir_script", "").equals(""))
        setConfig("after_dir_script", prefs.getProperty("after_dir_script", "")); 
      if (!prefs.getProperty("custom_setKeyReExchangeDisabled", "").equals(""))
        setConfig("custom_setKeyReExchangeDisabled", prefs.getProperty("custom_setKeyReExchangeDisabled", "")); 
      if (!prefs.getProperty("custom_setIdleConnectionTimeoutSeconds", "").equals(""))
        setConfig("custom_setIdleConnectionTimeoutSeconds", prefs.getProperty("custom_setIdleConnectionTimeoutSeconds", "")); 
      if (!prefs.getProperty("custom_enableCompression", "").equals(""))
        setConfig("custom_enableCompression", prefs.getProperty("custom_enableCompression", "")); 
      if (!prefs.getProperty("custom_enableFIPSMode", "").equals(""))
        setConfig("custom_enableFIPSMode", prefs.getProperty("custom_enableFIPSMode", "")); 
      if (!prefs.getProperty("custom_setDHGroupExchangeBackwardsCompatible", "").equals(""))
        setConfig("custom_setDHGroupExchangeBackwardsCompatible", prefs.getProperty("custom_setDHGroupExchangeBackwardsCompatible", "")); 
      if (!prefs.getProperty("custom_preferredCipher", "").equals(""))
        setConfig("custom_preferredCipher", prefs.getProperty("custom_preferredCipher", "")); 
      if (!prefs.getProperty("custom_setDHGroupExchangeKeySize", "").equals(""))
        setConfig("custom_setDHGroupExchangeKeySize", prefs.getProperty("custom_setDHGroupExchangeKeySize", "")); 
      if (!prefs.getProperty("custom_preferredKex", "").equals(""))
        setConfig("custom_preferredKex", prefs.getProperty("custom_preferredKex", "")); 
      if (!prefs.getProperty("custom_setUseRSAKey", "").equals(""))
        setConfig("custom_setUseRSAKey", prefs.getProperty("custom_setUseRSAKey", "")); 
      if (!prefs.getProperty("custom_preferredMac", "").equals(""))
        setConfig("custom_preferredMac", prefs.getProperty("custom_preferredMac", "")); 
      if (!prefs.getProperty("custom_setMaximumPacketLength", "").equals(""))
        setConfig("custom_setMaximumPacketLength", prefs.getProperty("custom_setMaximumPacketLength", "")); 
      if (!prefs.getProperty("custom_setSessionMaxPacketSize", "").equals(""))
        setConfig("custom_setSessionMaxPacketSize", prefs.getProperty("custom_setSessionMaxPacketSize", "")); 
      if (!prefs.getProperty("custom_setSessionMaxWindowSpace", "").equals(""))
        setConfig("custom_setSessionMaxWindowSpace", prefs.getProperty("custom_setSessionMaxWindowSpace", "")); 
      if (!prefs.getProperty("custom_enableETM", "").equals(""))
        setConfig("custom_enableETM", prefs.getProperty("custom_enableETM", "")); 
      if (!prefs.getProperty("custom_enableNonStandardAlgorithms", "").equals(""))
        setConfig("custom_enableNonStandardAlgorithms", prefs.getProperty("custom_enableNonStandardAlgorithms", "")); 
      if (!prefs.getProperty("delete_xml_representation_files", "").equals(""))
        setConfig("delete_xml_representation_files", prefs.getProperty("delete_xml_representation_files", "")); 
      if (!prefs.getProperty("dfs_enabled", "").equals(""))
        setConfig("dfs_enabled", prefs.getProperty("dfs_enabled", "")); 
      if (!prefs.getProperty("write_timeout", "").equals(""))
        setConfig("write_timeout", prefs.getProperty("write_timeout", "")); 
      if (!prefs.getProperty("read_timeout", "").equals(""))
        setConfig("read_timeout", prefs.getProperty("read_timeout", "")); 
      if (!prefs.getProperty("smb3_required_signing", "").equals(""))
        setConfig("smb3_required_signing", prefs.getProperty("smb3_required_signing", "")); 
      if (!prefs.getProperty("sas_token", "").equals(""))
        setConfig("sas_token", prefs.getProperty("sas_token", "")); 
      if (!prefs.getProperty("upload_blob_type", "").equals(""))
        setConfig("upload_blob_type", prefs.getProperty("upload_blob_type", "")); 
      if (!prefs.getProperty("team_drive", "").equals(""))
        setConfig("team_drive", prefs.getProperty("team_drive", "")); 
    } 
  }
  
  public static Properties copyConnectionItems(Properties item) {
    Properties tempPrefs = new Properties();
    String ssh_private_key = item.getProperty("ssh_private_key", "");
    String ssh_private_key_pass = item.getProperty("ssh_private_key_pass", "");
    if (ssh_private_key.equals(""))
      if (item.get("vItem") != null && item.get("vItem") instanceof Properties) {
        Properties vItem = (Properties)item.get("vItem");
        ssh_private_key = vItem.getProperty("ssh_private_key", "");
        ssh_private_key_pass = vItem.getProperty("ssh_private_key_pass", "");
      }  
    tempPrefs.put("ssh_private_key", ssh_private_key);
    tempPrefs.put("ssh_private_key_pass", ssh_private_key_pass);
    tempPrefs.put("ssh_two_factor", item.getProperty("ssh_two_factor", "false"));
    tempPrefs.put("keystore_path", item.getProperty("keystore_path", ""));
    tempPrefs.put("trustore_path", item.getProperty("trustore_path", ""));
    tempPrefs.put("keystore_pass", item.getProperty("keystore_pass", ""));
    tempPrefs.put("key_pass", item.getProperty("key_pass", ""));
    tempPrefs.put("acceptAnyCert", item.getProperty("acceptAnyCert", "false"));
    tempPrefs.put("use_dmz", item.getProperty("use_dmz", "false"));
    tempPrefs.put("pasv", item.getProperty("pasv", "true"));
    tempPrefs.put("ascii", item.getProperty("ascii", "false"));
    tempPrefs.put("no_os400", item.getProperty("no_os400", "false"));
    tempPrefs.put("simple", item.getProperty("simple", "false"));
    tempPrefs.put("cwd_list", item.getProperty("cwd_list", "false"));
    tempPrefs.put("no_stat", item.getProperty("no_stat", "false"));
    tempPrefs.put("secure_data", item.getProperty("secure_data", "true"));
    tempPrefs.put("server_side_encrypt", item.getProperty("server_side_encrypt", "false"));
    tempPrefs.put("s3_accelerate", item.getProperty("s3_accelerate", "false"));
    tempPrefs.put("s3_bucket_in_path", item.getProperty("s3_bucket_in_path", "false"));
    tempPrefs.put("s3_stat_head_calls_double", item.getProperty("s3_stat_head_calls_double", "false"));
    tempPrefs.put("s3_stat_head_calls", item.getProperty("s3_stat_head_calls", "true"));
    tempPrefs.put("multithreaded_s3", item.getProperty("multithreaded_s3", "false"));
    tempPrefs.put("server_side_encrypt_kms", item.getProperty("server_side_encrypt_kms", ""));
    if (!item.getProperty("s3_sha256", "").equals(""))
      tempPrefs.put("s3_sha256", item.getProperty("s3_sha256", "")); 
    if (!item.getProperty("s3_partial", "").equals(""))
      tempPrefs.put("s3_partial", item.getProperty("s3_partial", "")); 
    if (!item.getProperty("before_login_script", "").equals(""))
      tempPrefs.put("before_login_script", item.getProperty("before_login_script", "")); 
    if (!item.getProperty("after_login_script", "").equals(""))
      tempPrefs.put("after_login_script", item.getProperty("after_login_script", "")); 
    if (!item.getProperty("before_logout_script", "").equals(""))
      tempPrefs.put("before_logout_script", item.getProperty("before_logout_script", "")); 
    if (!item.getProperty("before_download_script", "").equals(""))
      tempPrefs.put("before_download_script", item.getProperty("before_download_script", "")); 
    if (!item.getProperty("after_download_script", "").equals(""))
      tempPrefs.put("after_download_script", item.getProperty("after_download_script", "")); 
    if (!item.getProperty("before_upload_script", "").equals(""))
      tempPrefs.put("before_upload_script", item.getProperty("before_upload_script", "")); 
    if (!item.getProperty("after_upload_script", "").equals(""))
      tempPrefs.put("after_upload_script", item.getProperty("after_upload_script", "")); 
    if (!item.getProperty("before_dir_script", "").equals(""))
      tempPrefs.put("before_dir_script", item.getProperty("before_dir_script", "")); 
    if (!item.getProperty("after_dir_script", "").equals(""))
      tempPrefs.put("after_dir_script", item.getProperty("after_dir_script", "")); 
    if (!item.getProperty("custom_setKeyReExchangeDisabled", "").equals(""))
      tempPrefs.put("custom_setKeyReExchangeDisabled", item.getProperty("custom_setKeyReExchangeDisabled", "")); 
    if (!item.getProperty("custom_setIdleConnectionTimeoutSeconds", "").equals(""))
      tempPrefs.put("custom_setIdleConnectionTimeoutSeconds", item.getProperty("custom_setIdleConnectionTimeoutSeconds", "")); 
    if (!item.getProperty("custom_enableCompression", "").equals(""))
      tempPrefs.put("custom_enableCompression", item.getProperty("custom_enableCompression", "")); 
    if (!item.getProperty("custom_enableFIPSMode", "").equals(""))
      tempPrefs.put("custom_enableFIPSMode", item.getProperty("custom_enableFIPSMode", "")); 
    if (!item.getProperty("custom_setDHGroupExchangeBackwardsCompatible", "").equals(""))
      tempPrefs.put("custom_setDHGroupExchangeBackwardsCompatible", item.getProperty("custom_setDHGroupExchangeBackwardsCompatible", "")); 
    if (!item.getProperty("custom_preferredCipher", "").equals(""))
      tempPrefs.put("custom_preferredCipher", item.getProperty("custom_preferredCipher", "")); 
    if (!item.getProperty("custom_setDHGroupExchangeKeySize", "").equals(""))
      tempPrefs.put("custom_setDHGroupExchangeKeySize", item.getProperty("custom_setDHGroupExchangeKeySize", "")); 
    if (!item.getProperty("custom_preferredKex", "").equals(""))
      tempPrefs.put("custom_preferredKex", item.getProperty("custom_preferredKex", "")); 
    if (!item.getProperty("custom_setUseRSAKey", "").equals(""))
      tempPrefs.put("custom_setUseRSAKey", item.getProperty("custom_setUseRSAKey", "")); 
    if (!item.getProperty("custom_preferredMac", "").equals(""))
      tempPrefs.put("custom_preferredMac", item.getProperty("custom_preferredMac", "")); 
    if (!item.getProperty("custom_setMaximumPacketLength", "").equals(""))
      tempPrefs.put("custom_setMaximumPacketLength", item.getProperty("custom_setMaximumPacketLength", "")); 
    if (!item.getProperty("custom_setSessionMaxPacketSize", "").equals(""))
      tempPrefs.put("custom_setSessionMaxPacketSize", item.getProperty("custom_setSessionMaxPacketSize", "")); 
    if (!item.getProperty("custom_setSessionMaxWindowSpace", "").equals(""))
      tempPrefs.put("custom_setSessionMaxWindowSpace", item.getProperty("custom_setSessionMaxWindowSpace", "")); 
    if (!item.getProperty("custom_enableETM", "").equals(""))
      tempPrefs.put("custom_enableETM", item.getProperty("custom_enableETM", "")); 
    if (!item.getProperty("custom_enableNonStandardAlgorithms", "").equals(""))
      tempPrefs.put("custom_enableNonStandardAlgorithms", item.getProperty("custom_enableNonStandardAlgorithms", "")); 
    if (!item.getProperty("delete_xml_representation_files", "").equals(""))
      tempPrefs.put("delete_xml_representation_files", item.getProperty("delete_xml_representation_files", "")); 
    if (!item.getProperty("dfs_enabled", "").equals(""))
      tempPrefs.put("dfs_enabled", item.getProperty("dfs_enabled", "")); 
    if (!item.getProperty("write_timeout", "").equals(""))
      tempPrefs.put("write_timeout", item.getProperty("write_timeout", "")); 
    if (!item.getProperty("read_timeout", "").equals(""))
      tempPrefs.put("read_timeout", item.getProperty("read_timeout", "")); 
    if (!item.getProperty("smb3_required_signing", "").equals(""))
      tempPrefs.put("smb3_required_signing", item.getProperty("smb3_required_signing", "")); 
    if (!item.getProperty("sas_token", "").equals(""))
      tempPrefs.put("sas_token", item.getProperty("sas_token", "")); 
    if (!item.getProperty("upload_blob_type", "").equals(""))
      tempPrefs.put("upload_blob_type", item.getProperty("upload_blob_type", "")); 
    if (!item.getProperty("team_drive", "").equals("false"))
      tempPrefs.put("team_drive", item.getProperty("team_drive", "")); 
    return tempPrefs;
  }
  
  public void logout() throws Exception {
    close();
  }
  
  public Vector list(String path, Vector list) throws Exception {
    return null;
  }
  
  public InputStream download(String path, long startPos, long endPos, boolean binary) throws Exception {
    checkTunnel();
    String originalUrl = this.url;
    if (getConfig("tunnel_active", "false").equals("true")) {
      log("Using tunnel:" + this.url + "  -->  " + getConfig("tunnel_url", this.url));
      this.url = getConfig("tunnel_url", this.url);
    } 
    try {
      if (getConfig("haDownload", "false").equals("true")) {
        this.in = new HADownload(this, path, startPos, endPos, binary, Integer.parseInt(getConfig("haDownloadDelay", "10")));
      } else {
        this.in = download2(path, startPos, endPos, binary);
      } 
    } finally {
      this.url = originalUrl;
    } 
    return this.in;
  }
  
  public InputStream getLimitedInputStream(InputStream in_tmp1, long startPos, long endPos) {
    return new null.InputWrapper(this, in_tmp1, startPos, endPos);
  }
  
  public void checkTunnel() {
    if (getConfig("use_tunnel", "false").equals("true") && getConfig("no_tunnels", "false").equals("false") && getConfig("tunnel_active", "false").equals("false"))
      synchronized (tunnelLock) {
        Tunnel2.setLog(this.logQueue);
        try {
          VRL vrl = new VRL(this.url);
          if (vrl.getProtocol().equalsIgnoreCase("http") || vrl.getProtocol().equalsIgnoreCase("https")) {
            Tunnel2 t = new Tunnel2(this.url, vrl.getUsername(), vrl.getPassword(), false);
            t.setAuth(this.config.getProperty("crushAuth", ""));
            Properties serverTunnel = (Properties)getConfig("serverTunnel");
            if (serverTunnel == null)
              serverTunnel = t.getTunnel(); 
            if (serverTunnel == null || serverTunnel.size() == 0)
              setConfig("no_tunnels", "true"); 
            if (serverTunnel != null && serverTunnel.size() > 0) {
              setConfig("tunnel_active", "true");
              setConfig("serverTunnel", serverTunnel);
              Properties tunnelInfo = new Properties();
              setConfig("tunnelInfo", tunnelInfo);
              if (serverTunnel != null && serverTunnel.size() > 0) {
                tunnelInfo.put("tunnelActive", "true");
                tunnelInfo.put("serverTunnel", serverTunnel);
                t.setTunnel(serverTunnel);
                ServerSocket ss = new ServerSocket(0);
                int tunnelPort = ss.getLocalPort();
                serverTunnel.put("localPort", (new StringBuffer(String.valueOf(tunnelPort))).toString());
                tunnelInfo.put("localPort", (new StringBuffer(String.valueOf(tunnelPort))).toString());
                setConfig("tunnel_url", "http://127.0.0.1:" + tunnelPort + "/");
                ss.close();
                log("Starting CrushTunnel:" + serverTunnel);
                (new Thread(new Runnable(this, tunnelInfo, t) {
                      final GenericClient this$0;
                      
                      private final Properties val$tunnelInfo;
                      
                      private final Tunnel2 val$t;
                      
                      public void run() {
                        this.val$tunnelInfo.put("emptyTunnelCount", "0");
                        Thread.currentThread().setName(String.valueOf(System.getProperty("crushsync.appname", "CrushSync")) + ":TunnelManager");
                        try {
                          this.val$t.startThreads();
                          while (this.val$t.isActive() && Integer.parseInt(this.val$tunnelInfo.getProperty("emptyTunnelCount", "0")) < 30) {
                            Thread.sleep(1000L);
                            if (this.val$t.getQueueCount() <= 1) {
                              this.val$tunnelInfo.put("emptyTunnelCount", (new StringBuffer(String.valueOf(Integer.parseInt(this.val$tunnelInfo.getProperty("emptyTunnelCount", "0")) + 1))).toString());
                            } else {
                              this.val$tunnelInfo.put("emptyTunnelCount", "0");
                            } 
                            this.this$0.setConfig("tunnel_status", "(Tunnel is Active: Out=" + this.val$t.getSends() + ", In=" + this.val$t.getGets() + ")");
                          } 
                        } catch (Exception e) {
                          this.this$0.log(e);
                        } finally {
                          this.this$0.setConfig("tunnel_status", "");
                          this.this$0.setConfig("tunnel_active", "false");
                          this.this$0.log("Closing idle CrushTunnel:active=" + this.val$t.isActive());
                          if (this.val$t.isActive())
                            this.val$t.stopThisTunnel(); 
                          this.this$0.log("Closing idle CrushTunnel:closed");
                          this.val$tunnelInfo.put("tunnelActive", "false");
                        } 
                      }
                    })).start();
                log("Waiting for tunnel to start...");
                while (!t.isReady() && t.isActive())
                  Thread.sleep(100L); 
                log("Tunnel started:ready=" + t.isReady() + ":active=" + t.isActive());
              } 
            } 
          } 
        } catch (Exception e) {
          log(e);
        } 
      }  
  }
  
  protected InputStream download2(String path, long startPos, long endPos, boolean binary) throws Exception {
    String path2 = path;
    this.in3 = download3(path, startPos, endPos, binary);
    if (!Common.dmz_mode && getConfig("pgpDecryptDownload", "false").equals("true")) {
      boolean isPgpData = true;
      boolean check_downloads = System.getProperty("crushftp.pgp_check_downloads", "true").equals("true");
      if (!getConfig("decrypt_pgp_check_downloads", "").equals(""))
        check_downloads = getConfig("decrypt_pgp_check_downloads", "false").equals("true"); 
      if (check_downloads) {
        byte[] b_header = new byte["CRUSHFTP_ENCRYPTED_kHBeMxiWj7Sb4PdqJ8".length()];
        this.in3 = new BufferedInputStream(this.in3);
        this.in3.mark(b_header.length + 1);
        int bytesRead = this.in3.read(b_header);
        this.in3.reset();
        String header = "";
        if (bytesRead > 0)
          header = new String(b_header, 0, bytesRead, "UTF8"); 
        isPgpData = !(!(new PGPInspectLib()).isPGPData(b_header) && !header.toUpperCase().startsWith("-----BEGIN PGP MESSAGE-----"));
      } 
      log("pgpDecryptDownload:" + (new VRL(getConfig("pgpPrivateKeyDownloadPath", ""))).safe() + " isPgp:" + isPgpData + " checkedIsPgp:" + check_downloads);
      if (isPgpData || getConfig("pgpPrivateKeyDownloadPath").toString().toLowerCase().startsWith("password:")) {
        if (this.pgp == null)
          this.pgp = new PGPLib(); 
        this.pgp.setUseExpiredKeys(true);
        Properties socks = Common.getConnectedSocks(false);
        Socket sock1 = (Socket)socks.get("sock1");
        Socket sock2 = (Socket)socks.get("sock2");
        InputStream in3f1 = this.in3;
        System.setProperty("crushtunnel.debug", System.getProperty("crushftp.debug", "2"));
        (new Thread(new Runnable(this, sock1, in3f1) {
              final GenericClient this$0;
              
              private final Socket val$sock1;
              
              private final InputStream val$in3f1;
              
              public void run() {
                try {
                  ByteArrayOutputStream baos_key = new ByteArrayOutputStream();
                  boolean pbe = false;
                  String keyLocation = this.this$0.getConfig("pgpPrivateKeyDownloadPath").toString();
                  if (keyLocation.toLowerCase().startsWith("password:")) {
                    pbe = true;
                  } else {
                    try {
                      if (keyLocation.indexOf(":") < 0 || keyLocation.indexOf(":") < 3)
                        keyLocation = "FILE://" + keyLocation; 
                      VRL key_vrl = new VRL(keyLocation);
                      GenericClient c_key = Common.getClient(Common.getBaseUrl(key_vrl.toString()), "CrushFTP", new Vector());
                      Common.streamCopier(new BufferedInputStream(c_key.download(key_vrl.getPath(), 0L, -1L, true)), baos_key, false, true, true);
                      c_key.logout();
                    } catch (Exception e) {
                      if (e instanceof java.io.FileNotFoundException)
                        throw new Exception(" PGP ERROR : Could not find the pgp private key! Path : " + keyLocation + " File Not Found Error message : " + e.getMessage()); 
                      throw e;
                    } 
                  } 
                  ByteArrayInputStream bytesIn1 = new ByteArrayInputStream(baos_key.toByteArray());
                  ByteArrayInputStream bytesIn2 = new ByteArrayInputStream(baos_key.toByteArray());
                  this.this$0.pgp.setCompression("UNCOMPRESSED");
                  OutputStream out4 = this.val$sock1.getOutputStream();
                  if (pbe) {
                    this.this$0.pgp.decryptStreamPBE(this.val$in3f1, Common.encryptDecrypt(this.this$0.getConfig("pgpPrivateKeyDownloadPassword", ""), false), out4);
                  } else if ((new KeyStore()).importPrivateKey(bytesIn1)[0].checkPassword(this.this$0.getConfig("pgpPrivateKeyDownloadPassword", "").toString())) {
                    this.this$0.pgp.decryptStream(this.val$in3f1, bytesIn2, this.this$0.getConfig("pgpPrivateKeyDownloadPassword", "").toString(), out4);
                  } else {
                    this.this$0.pgp.decryptStream(this.val$in3f1, bytesIn2, Common.encryptDecrypt(this.this$0.getConfig("pgpPrivateKeyDownloadPassword", "").toString(), false), out4);
                  } 
                  bytesIn1.close();
                  bytesIn2.close();
                  out4.close();
                } catch (Exception e) {
                  this.this$0.log(e);
                  Common.log("SERVER", 1, e);
                  try {
                    this.val$in3f1.close();
                  } catch (IOException e1) {
                    this.this$0.log(e1);
                    Common.log("SERVER", 1, e1);
                  } 
                } 
              }
            })).start();
        this.in3 = sock2.getInputStream();
      } 
    } 
    if (getConfig("pgpEncryptDownload", "").equals("true"))
      log("pgpEncryptDownload:" + (new VRL(getConfig("pgpPublicKeyDownloadPath", ""))).safe()); 
    if (!Common.dmz_mode && getConfig("pgpEncryptDownload", "false").equals("true")) {
      System.setProperty("crushtunnel.debug", System.getProperty("crushftp.debug", "2"));
      boolean isPgpData = true;
      boolean check_downloads = System.getProperty("crushftp.pgp_check_downloads", "true").equals("true");
      if (!getConfig("encrypt_pgp_check_downloads", "").equals(""))
        check_downloads = getConfig("encrypt_pgp_check_downloads", "false").equals("true"); 
      if (check_downloads) {
        byte[] b_header = new byte["CRUSHFTP_ENCRYPTED_kHBeMxiWj7Sb4PdqJ8".length()];
        this.in3 = new BufferedInputStream(this.in3);
        this.in3.mark(b_header.length + 1);
        int bytesRead = this.in3.read(b_header);
        this.in3.reset();
        String header = "";
        if (bytesRead > 0)
          header = new String(b_header, 0, bytesRead, "UTF8"); 
        isPgpData = !(!(new PGPInspectLib()).isPGPData(b_header) && !header.toUpperCase().startsWith("-----BEGIN PGP MESSAGE-----"));
      } 
      log("pgpEncryptDownload:" + (new VRL(getConfig("pgpPublicKeyDownloadPath", ""))).safe() + " isPgp:" + isPgpData + " checkedIsPgp:" + check_downloads);
      if (!isPgpData) {
        if (this.pgp == null)
          this.pgp = new PGPLib(); 
        this.pgp.setUseExpiredKeys(true);
        Properties socks = Common.getConnectedSocks(false);
        Socket sock1 = (Socket)socks.get("sock1");
        Socket sock2 = (Socket)socks.get("sock2");
        InputStream in3f1 = this.in3;
        (new Thread(new Runnable(this, sock1, path2, in3f1) {
              final GenericClient this$0;
              
              private final Socket val$sock1;
              
              private final String val$path2;
              
              private final InputStream val$in3f1;
              
              public void run() {
                try {
                  ByteArrayOutputStream baos_key = new ByteArrayOutputStream();
                  boolean pbe = false;
                  String keyLocation = this.this$0.getConfig("pgpPublicKeyDownloadPath").toString();
                  if (keyLocation.toLowerCase().startsWith("password:")) {
                    pbe = true;
                  } else {
                    try {
                      if (keyLocation.indexOf(":") < 0 || keyLocation.indexOf(":") < 3)
                        keyLocation = "FILE://" + keyLocation; 
                      VRL key_vrl = new VRL(keyLocation);
                      GenericClient c_key = Common.getClient(Common.getBaseUrl(key_vrl.toString()), "CrushFTP", new Vector());
                      Common.streamCopier(c_key.download(key_vrl.getPath(), 0L, -1L, true), baos_key, false, true, true);
                      c_key.logout();
                    } catch (Exception e) {
                      if (e instanceof java.io.FileNotFoundException)
                        throw new Exception(" PGP ERROR : Could not found the pgp public key! Path : " + keyLocation + " File not Found Error message : " + e.getMessage()); 
                      throw e;
                    } 
                  } 
                  ByteArrayInputStream bytesIn = new ByteArrayInputStream(baos_key.toByteArray());
                  this.this$0.pgp.setCompression("UNCOMPRESSED");
                  OutputStream out4 = this.val$sock1.getOutputStream();
                  String filename = Common.last(this.val$path2);
                  if (filename.endsWith(".filepart"))
                    filename = filename.substring(0, filename.lastIndexOf(".")); 
                  if (!this.this$0.getConfig("encryption_cypher_download", "").equals("")) {
                    if (!this.this$0.pgp.isOverrideKeyAlgorithmPreferences())
                      this.this$0.pgp.setOverrideKeyAlgorithmPreferences(true); 
                    this.this$0.pgp.setCypher(this.this$0.getConfig("encryption_cypher_download", "").trim());
                  } 
                  if (pbe) {
                    this.this$0.pgp.encryptStreamPBE(this.val$in3f1, filename, Common.encryptDecrypt(keyLocation.substring(keyLocation.indexOf(":") + 1), false), out4, this.this$0.getConfig("pgpAsciiDownload", "false").equals("true"), System.getProperty("crushftp.pgp_integrity_protect", "false").equals("true"));
                  } else {
                    this.this$0.pgp.encryptStream(this.val$in3f1, filename, bytesIn, out4, this.this$0.getConfig("pgpAsciiDownload", "false").equals("true"), System.getProperty("crushftp.pgp_integrity_protect", "false").equals("true"));
                  } 
                  bytesIn.close();
                  out4.close();
                } catch (Exception e) {
                  this.this$0.log(e);
                  Common.log("SERVER", 1, e);
                  try {
                    this.val$in3f1.close();
                  } catch (IOException e1) {
                    this.this$0.log(e);
                    Common.log("SERVER", 1, e);
                  } 
                } 
              }
            })).start();
        this.in3 = sock2.getInputStream();
      } 
    } 
    return this.in3;
  }
  
  public static Vector ftp_client_sockets = new Vector();
  
  protected void setupTimeout(Properties byteCount, Socket transfer_socket) {
    int secs = Integer.parseInt(this.config.getProperty("timeout", "600"));
    if (secs != 0) {
      byteCount.put("t", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      byteCount.put("b", "0");
      byteCount.put("secs", (new StringBuffer(String.valueOf(secs))).toString());
      byteCount.put("transfer_socket", transfer_socket);
      try {
        transfer_socket.setSoTimeout(secs * 1000 + 10000);
      } catch (Exception exception) {}
      ftp_client_sockets.addElement(byteCount);
    } 
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary) throws Exception {
    return null;
  }
  
  public OutputStream upload(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    checkTunnel();
    String originalUrl = this.url;
    if (getConfig("tunnel_active", "false").equals("true")) {
      log("Using tunnel:" + this.url + "  -->  " + getConfig("tunnel_url", this.url));
      this.url = getConfig("tunnel_url", this.url);
    } 
    try {
      if (getConfig("haUpload", "false").equals("true")) {
        this.out = new HAUpload(this, path, startPos, truncate, binary, Integer.parseInt(getConfig("haUploadPriorWriteCount", "130")), Integer.parseInt(getConfig("haUploadDelay", "10")));
      } else {
        this.out = upload2(path, startPos, truncate, binary);
      } 
    } finally {
      this.url = originalUrl;
    } 
    return this.out;
  }
  
  public boolean upload_0_byte(String path) throws Exception {
    upload3(path, 0L, true, true).close();
    return true;
  }
  
  protected OutputStream upload2(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    String path2 = path;
    ByteArrayOutputStream decrypted_file_bytes = null;
    if (startPos > 0L && !Common.dmz_mode && getConfig("pgpEncryptUpload", "false").equals("true") && this instanceof FileClient) {
      Properties stat = stat(path);
      if (stat != null && Long.parseLong(stat.getProperty("size")) < 52428800L) {
        decrypted_file_bytes = new ByteArrayOutputStream();
        Common.copyStreams(download(path, 0L, -1L, binary), decrypted_file_bytes, true, true);
        this.out3 = upload3(path, 0L, true, binary);
      } else {
        this.out3 = upload3(path, startPos, truncate, binary);
      } 
    } else {
      this.out3 = upload3(path, startPos, truncate, binary);
    } 
    OutputStream out3f1 = this.out3;
    if (getConfig("pgpDecryptUpload", "").equals("true"))
      log("pgpDecryptUpload:" + (new VRL(getConfig("pgpPrivateKeyUploadPath", ""))).safe()); 
    if (getConfig("pgpEncryptUpload", "").equals("true"))
      log("pgpEncryptUpload:" + (new VRL(getConfig("pgpPublicKeyUploadPath", ""))).safe()); 
    if (!Common.dmz_mode && getConfig("pgpEncryptUpload", "false").equals("true")) {
      System.setProperty("crushtunnel.debug", System.getProperty("crushftp.debug", "2"));
      if (this.pgp == null)
        this.pgp = new PGPLib(); 
      this.pgp.setUseExpiredKeys(true);
      Properties socks = Common.getConnectedSocks(false);
      Socket sock1 = (Socket)socks.remove("sock1");
      Socket sock2 = (Socket)socks.remove("sock2");
      Properties status = new Properties();
      (new Thread(new Runnable(this, path2, sock1, out3f1, status) {
            final GenericClient this$0;
            
            private final String val$path2;
            
            private final Socket val$sock1;
            
            private final OutputStream val$out3f1;
            
            private final Properties val$status;
            
            public void run() {
              try {
                ByteArrayOutputStream baos_key = new ByteArrayOutputStream();
                boolean pbe = false;
                String keyLocation = this.this$0.getConfig("pgpPublicKeyUploadPath").toString();
                if (keyLocation.toLowerCase().startsWith("password:")) {
                  pbe = true;
                } else {
                  try {
                    if (keyLocation.indexOf(":") < 0 || keyLocation.indexOf(":") < 3)
                      keyLocation = "FILE://" + keyLocation; 
                    VRL key_vrl = new VRL(keyLocation);
                    GenericClient c_key = Common.getClient(Common.getBaseUrl(key_vrl.toString()), "CrushFTP", new Vector());
                    Common.streamCopier(c_key.download(key_vrl.getPath(), 0L, -1L, true), baos_key, false, true, true);
                    c_key.logout();
                  } catch (Exception e) {
                    if (e instanceof java.io.FileNotFoundException)
                      throw new Exception(" PGP ERROR : Could not found the pgp public key! Path : " + keyLocation + " File not Found Error message : " + e.getMessage()); 
                    throw e;
                  } 
                } 
                ByteArrayInputStream bytesIn = new ByteArrayInputStream(baos_key.toByteArray());
                this.this$0.pgp.setCompression("UNCOMPRESSED");
                if (this.this$0.getConfig("pgpAsciiUpload", "false").equals("true"))
                  this.this$0.pgp.setAsciiVersionHeader("CRUSHFTP#                                        "); 
                String filename = Common.last(this.val$path2);
                if (filename.endsWith(".filepart"))
                  filename = filename.substring(0, filename.lastIndexOf(".")); 
                if (!this.this$0.getConfig("encryption_cypher_upload", "").equals("")) {
                  if (!this.this$0.pgp.isOverrideKeyAlgorithmPreferences())
                    this.this$0.pgp.setOverrideKeyAlgorithmPreferences(true); 
                  this.this$0.pgp.setCypher(this.this$0.getConfig("encryption_cypher_upload", "").trim());
                } 
                if (pbe) {
                  this.this$0.pgp.encryptStreamPBE(this.val$sock1.getInputStream(), filename, Common.encryptDecrypt(keyLocation.substring(keyLocation.indexOf(":") + 1), false), this.val$out3f1, this.this$0.getConfig("pgpAsciiUpload", "false").equals("true"), System.getProperty("crushftp.pgp_integrity_protect", "false").equals("true"));
                } else {
                  this.this$0.pgp.encryptStream(this.val$sock1.getInputStream(), filename, bytesIn, this.val$out3f1, this.this$0.getConfig("pgpAsciiUpload", "false").equals("true"), System.getProperty("crushftp.pgp_integrity_protect", "false").equals("true"));
                } 
                bytesIn.close();
                this.val$status.put("status", "SUCCESS");
              } catch (Exception e) {
                this.val$status.put("error", e);
                this.val$status.put("status", "ERROR");
                Common.log("SERVER", 1, e);
                try {
                  this.val$out3f1.close();
                } catch (IOException e1) {
                  Common.log("SERVER", 1, e1);
                } 
              } 
            }
          })).start();
      boolean write_pgp_size_footer = getConfig("pgpAsciiUpload", "false").equals("false");
      if (!getConfig("hint_decrypted_size", "").equals(""))
        write_pgp_size_footer = getConfig("hint_decrypted_size", "false").equals("true"); 
      this.out3 = new OutputStreamCloser(sock2.getOutputStream(), status, this.thisObj, path, write_pgp_size_footer, getConfig("pgpAsciiUpload", "false").equals("true"), out3f1);
      if (decrypted_file_bytes != null) {
        this.out3.write(decrypted_file_bytes.toByteArray());
        decrypted_file_bytes.reset();
        decrypted_file_bytes = null;
      } 
    } 
    OutputStream out3f2 = this.out3;
    if (!Common.dmz_mode && getConfig("pgpDecryptUpload", "false").equals("true")) {
      System.setProperty("crushtunnel.debug", System.getProperty("crushftp.debug", "2"));
      if (this.pgp == null)
        this.pgp = new PGPLib(); 
      this.pgp.setUseExpiredKeys(true);
      Properties socks = Common.getConnectedSocks(false);
      Socket sock1 = (Socket)socks.get("sock1");
      Socket sock2 = (Socket)socks.get("sock2");
      Properties status = new Properties();
      (new Thread(new Runnable(this, sock1, out3f2, status) {
            final GenericClient this$0;
            
            private final Socket val$sock1;
            
            private final OutputStream val$out3f2;
            
            private final Properties val$status;
            
            public void run() {
              try {
                ByteArrayOutputStream baos_key = new ByteArrayOutputStream();
                boolean pbe = false;
                String keyLocation = this.this$0.getConfig("pgpPrivateKeyUploadPath").toString();
                if (keyLocation.toLowerCase().startsWith("password:")) {
                  pbe = true;
                } else {
                  try {
                    if (keyLocation.indexOf(":") < 0 || keyLocation.indexOf(":") < 3)
                      keyLocation = "FILE://" + keyLocation; 
                    VRL key_vrl = new VRL(keyLocation);
                    GenericClient c_key = Common.getClient(Common.getBaseUrl(key_vrl.toString()), "CrushFTP", new Vector());
                    Common.streamCopier(c_key.download(key_vrl.getPath(), 0L, -1L, true), baos_key, false, true, true);
                    c_key.logout();
                  } catch (Exception e) {
                    if (e instanceof java.io.FileNotFoundException)
                      throw new Exception(" PGP ERROR : Could not found the pgp private key! Path : " + keyLocation + " File not Found Error message : " + e.getMessage()); 
                    throw e;
                  } 
                } 
                ByteArrayInputStream bytesIn1 = new ByteArrayInputStream(baos_key.toByteArray());
                ByteArrayInputStream bytesIn2 = new ByteArrayInputStream(baos_key.toByteArray());
                this.this$0.pgp.setCompression("UNCOMPRESSED");
                if (pbe) {
                  this.this$0.pgp.decryptStreamPBE(this.val$sock1.getInputStream(), Common.encryptDecrypt(this.this$0.getConfig("pgpPrivateKeyUploadPassword", ""), false), this.val$out3f2);
                } else if ((new KeyStore()).importPrivateKey(bytesIn1)[0].checkPassword(this.this$0.getConfig("pgpPrivateKeyUploadPassword", "").toString())) {
                  this.this$0.pgp.decryptStream(this.val$sock1.getInputStream(), bytesIn2, this.this$0.getConfig("pgpPrivateKeyUploadPassword", "").toString(), this.val$out3f2);
                } else {
                  this.this$0.pgp.decryptStream(this.val$sock1.getInputStream(), bytesIn2, Common.encryptDecrypt(this.this$0.getConfig("pgpPrivateKeyUploadPassword", "").toString(), false), this.val$out3f2);
                } 
                bytesIn1.close();
                bytesIn2.close();
                this.val$status.put("status", "SUCCESS");
              } catch (Exception e) {
                this.val$status.put("error", e);
                this.val$status.put("status", "ERROR");
                Common.log("SERVER", 1, e);
                try {
                  this.val$out3f2.close();
                } catch (IOException e1) {
                  Common.log("SERVER", 1, e1);
                } 
              } 
            }
          })).start();
      this.out3 = new OutputStreamCloser(sock2.getOutputStream(), status, this.thisObj, path, false, false, out3f2);
    } 
    return this.out3;
  }
  
  protected OutputStream upload3(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    return null;
  }
  
  public boolean delete(String path) throws Exception {
    return false;
  }
  
  public boolean makedir(String path) throws Exception {
    return false;
  }
  
  public boolean makedirs(String path) throws Exception {
    return false;
  }
  
  public boolean rename(String rnfr, String rnto) throws Exception {
    return false;
  }
  
  public Properties stat(String path) throws Exception {
    return null;
  }
  
  public void setMod(String path, String val, String param) throws Exception {}
  
  public void setOwner(String path, String val, String param) throws Exception {}
  
  public void setGroup(String path, String val, String param) throws Exception {}
  
  public boolean mdtm(String path, long modified) throws Exception {
    return false;
  }
  
  public void close() throws Exception {
    if (this.in != null) {
      this.in.close();
      this.in = null;
    } 
    if (this.out != null) {
      this.out.close();
      this.out = null;
    } 
  }
  
  public String doCommand(String command) throws Exception {
    return "";
  }
  
  public void setConfigObj(Properties config) {
    config.putAll(this.config);
    this.config = config;
  }
  
  public void setConfig(String key, Object o) {
    Properties p = key.startsWith("transfer_") ? this.transfer_info : this.config;
    if (o == null) {
      p.remove(key);
    } else {
      p.put(key, o);
    } 
  }
  
  public String getConfig(String key, String s) {
    Properties p = key.startsWith("transfer_") ? this.transfer_info : this.config;
    return p.getProperty(key, s);
  }
  
  public Object getConfig(String key) {
    Properties p = key.startsWith("transfer_") ? this.transfer_info : this.config;
    return p.get(key);
  }
  
  public long getLength(String path) throws Exception {
    Properties p = stat(path);
    if (p == null)
      return 0L; 
    return Long.parseLong(p.getProperty("size", "0"));
  }
  
  public long getLastModified(String path) throws Exception {
    Properties p = stat(path);
    return Long.parseLong(p.getProperty("modified", "0"));
  }
  
  public static String u(String s) throws Exception {
    String r = Common.makeBoundary(20);
    s = s.replaceAll(" ", r);
    s = URLEncoder.encode(s, "utf-8");
    s = s.replaceAll(r, "%20");
    return s;
  }
  
  public static void writeEntry(String key, String val, BufferedOutputStream dos, String boundary) throws Exception {
    dos.write((String.valueOf(boundary) + "\r\n").getBytes("UTF8"));
    dos.write(("Content-Disposition: form-data; name=\"" + key + "\"\r\n").getBytes("UTF8"));
    dos.write("\r\n".getBytes("UTF8"));
    dos.write((String.valueOf(val) + "\r\n").getBytes("UTF8"));
  }
  
  public static void writeEnd(BufferedOutputStream dos, String boundary) throws Exception {
    dos.write((String.valueOf(boundary) + "--\r\n").getBytes("UTF8"));
    dos.flush();
    dos.close();
  }
  
  public String getLastMd5() {
    return "not calculated";
  }
  
  public static Properties parseStat(String data) throws Exception {
    data = data.trim();
    if (data.equals(""))
      return null; 
    StringTokenizer st = new StringTokenizer(data);
    Properties item = new Properties();
    item.put("privs", st.nextToken());
    if (item.getProperty("privs").toUpperCase().startsWith("D")) {
      item.put("permissions", "drwxrwxrwx");
      item.put("type", "DIR");
    } else {
      item.put("permissions", "-rwxrwxrwx");
      item.put("type", "FILE");
    } 
    item.put("count", st.nextToken());
    item.put("num_items", item.getProperty("count"));
    item.put("owner", st.nextToken());
    item.put("group", st.nextToken());
    item.put("size", st.nextToken());
    String dateStr = st.nextToken();
    item.put("modified", dateStr);
    SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
    Date d = yyyyMMddHHmmss.parse(dateStr);
    SimpleDateFormat mmm = new SimpleDateFormat("MMM", Locale.US);
    item.put("month", mmm.format(d));
    item.put("day", st.nextToken());
    String year = st.nextToken();
    item.put("time_or_year", year);
    String root_dir = st.nextToken();
    root_dir = data.substring(data.indexOf(" " + year + " " + root_dir) + (" " + year + " ").length());
    root_dir = root_dir.replaceAll("&amp;", "&").replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("%5C", "\\");
    item.put("path", Common.all_but_last(root_dir));
    item.put("name", Common.last(root_dir));
    setFileDateInfo(item, d);
    if (item.getProperty("type").equalsIgnoreCase("DIR"))
      item.put("size", "1"); 
    return item;
  }
  
  public static Properties parseDmzStat(String data) throws Exception {
    data = data.trim();
    if (data.equals("") || data.indexOf(";") < 0)
      return null; 
    String[] items = data.split(";");
    Properties item = new Properties();
    for (int x = 0; x < items.length; x++) {
      if (!items[x].trim().equals("")) {
        String[] parts = items[x].split("=");
        String key = parts[0];
        String val = "";
        if (parts.length > 1)
          val = parts[1]; 
        item.put(key, Common.url_decode(val));
      } 
    } 
    return item;
  }
  
  public static void setFileDateInfo(Properties dir_item, Date itemDate) {
    SimpleDateFormat mm = new SimpleDateFormat("MM", Locale.US);
    SimpleDateFormat dd = new SimpleDateFormat("dd", Locale.US);
    SimpleDateFormat yyyy = new SimpleDateFormat("yyyy", Locale.US);
    SimpleDateFormat hhmm = new SimpleDateFormat("HH:mm", Locale.US);
    dir_item.put("modified", (new StringBuffer(String.valueOf(itemDate.getTime()))).toString());
    dir_item.put("month", months[Integer.parseInt(mm.format(itemDate))]);
    dir_item.put("day", dd.format(itemDate));
    String time_or_year = hhmm.format(itemDate);
    if (!yyyy.format(itemDate).equals(yyyy.format(new Date())))
      time_or_year = yyyy.format(itemDate); 
    dir_item.put("time_or_year", time_or_year);
  }
  
  public void set_MD5_and_upload_id(String path) throws Exception {}
  
  public String getUploadedByMetadata(String path) {
    return "";
  }
  
  public void startSocketCleaner() {
    synchronized (socketCleanerLock) {
      if (socketCleaner == null) {
        socketCleaner = new Thread(new Runnable(this) {
              final GenericClient this$0;
              
              public void run() {
                while (true) {
                  try {
                    Vector v = (Vector)GenericClient.ftp_client_sockets.clone();
                    while (v.size() > 20)
                      v.remove(0); 
                    GenericClient.checkIdleClientSockets();
                    Thread.currentThread().setName("FTPClient socket cleanup thread:" + v);
                  } catch (Exception e) {
                    Common.log("SERVER", 1, e);
                  } 
                  try {
                    Thread.sleep(30000L);
                  } catch (Exception exception) {}
                } 
              }
            });
        socketCleaner.start();
      } 
    } 
  }
  
  public static void checkIdleClientSockets() {
    Vector v = (Vector)ftp_client_sockets.clone();
    for (int xx = 0; xx < v.size(); xx++) {
      Properties byteCount = v.elementAt(xx);
      boolean kill = false;
      Socket transfer_socket = (Socket)byteCount.get("transfer_socket");
      if (transfer_socket != null) {
        long secs = Long.parseLong(byteCount.getProperty("secs", "600"));
        if (byteCount.getProperty("status", "").equals("") && !transfer_socket.isClosed()) {
          Thread.currentThread().setName("FTPClientTimeout::" + System.currentTimeMillis() + ":" + byteCount);
          byteCount.put("idle", (new StringBuffer(String.valueOf(System.currentTimeMillis() - Long.parseLong(byteCount.getProperty("t"))))).toString());
          if (System.currentTimeMillis() - Long.parseLong(byteCount.getProperty("t")) > secs)
            kill = true; 
        } else {
          kill = true;
        } 
        if (kill) {
          ftp_client_sockets.remove(byteCount);
          Properties byteCount2 = byteCount;
          byteCount.remove("transfer_socket");
          try {
            Worker.startWorker(new Runnable(byteCount2) {
                  private final Properties val$byteCount2;
                  
                  public void run() {
                    try {
                      Thread.currentThread().setName("FTPClient:closing socket for being done or idle:" + this.val$byteCount2);
                      ((Socket)this.val$byteCount2.remove("transfer_socket")).close();
                      Thread.currentThread().setName("FTPClient:Idle");
                    } catch (IOException iOException) {}
                  }
                });
          } catch (IOException iOException) {}
        } 
      } 
    } 
  }
}
