package crushftp.server;

import com.crushftp.client.Common;
import com.crushftp.client.File_S;
import com.crushftp.client.File_U;
import com.crushftp.client.GenericClient;
import com.crushftp.client.HTTPClient;
import com.crushftp.client.LineReader;
import com.crushftp.client.S3Client;
import com.crushftp.client.VRL;
import com.crushftp.client.Worker;
import com.crushftp.tunnel.FileArchiveInputStream;
import crushftp.db.SearchHandler;
import crushftp.gui.LOC;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.SessionCrush;
import crushftp.handlers.TransferSpeedometer;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.InflaterInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

public class STOR_handler implements Runnable {
  public boolean die_now = false;
  
  public boolean quota_exceeded = false;
  
  public boolean pause_transfer = false;
  
  public float slow_transfer = 0.0F;
  
  public long startLoop = 0L;
  
  public long endLoop = 1000L;
  
  public String stop_message = "";
  
  public Thread thisThread = null;
  
  public String the_dir;
  
  public int packet_size = 131072;
  
  long quota = 0L;
  
  public InputStream data_is = null;
  
  public GenericClient c = null;
  
  OutputStream out = null;
  
  String the_file_path = "";
  
  String the_file_name = "";
  
  SessionCrush thisSession = null;
  
  static String CRLF = "\r\n";
  
  int user_up_count = 0;
  
  Properties item;
  
  String realAction = "STOR";
  
  public Properties active2 = new Properties();
  
  boolean zlibing = false;
  
  public MessageDigest md5 = null;
  
  public Properties metaInfo = null;
  
  public String user_agent = null;
  
  public long current_loc = 0L;
  
  public ServerSocket s_sock = null;
  
  public Socket streamer = null;
  
  RandomAccessFile proxy = null;
  
  OutputStream proxy_remote_out = null;
  
  public boolean httpUpload = false;
  
  public boolean inError = false;
  
  public boolean allowTempExtensions = true;
  
  public boolean zipstream = false;
  
  public boolean zipchunkstream = false;
  
  public Socket data_sock = null;
  
  boolean unique = false;
  
  boolean random_access = false;
  
  public long fileModifiedDate = System.currentTimeMillis();
  
  String priorMd5 = "";
  
  String threadName = "";
  
  Vector pendingSyncs = new Vector();
  
  boolean wait_for_parent_free = false;
  
  boolean normal_writes = true;
  
  boolean block_ftp_fix = false;
  
  Vector last_md5s = new Vector();
  
  long asciiLineCount = 0L;
  
  SimpleDateFormat proxySDF = new SimpleDateFormat("MMddyyHHmmss");
  
  public STOR_handler() {
    this.active2.put("streamOpenStatus", "PENDING");
  }
  
  public void init_vars(String the_dir, long current_loc, SessionCrush thisSession, Properties item, String realAction, boolean unique, boolean random_access, Properties metaInfo, Socket data_sock) {
    this.data_sock = data_sock;
    this.the_dir = the_dir;
    this.thisSession = thisSession;
    this.current_loc = current_loc;
    this.item = item;
    this.realAction = realAction;
    this.random_access = random_access;
    this.metaInfo = metaInfo;
    this.inError = false;
    try {
      if (this.md5 == null)
        this.md5 = MessageDigest.getInstance(ServerStatus.SG("hash_algorithm")); 
    } catch (Exception exception) {}
    this.md5.reset();
    thisSession.uiPUT("md5", "");
    thisSession.uiPUT("sfv", "");
    this.asciiLineCount = 0L;
    try {
      this.user_up_count = ServerStatus.count_users_up();
    } catch (Exception exception) {}
    this.die_now = false;
    this.unique = unique;
    this.fileModifiedDate = System.currentTimeMillis();
    this.threadName = String.valueOf(Thread.currentThread().getName()) + ":STOR";
    this.block_ftp_fix = false;
  }
  
  public void setThreadName(String threadName) {
    this.threadName = threadName;
  }
  
  public void run() {
    this.thisThread = Thread.currentThread();
    this.thisThread.setName(this.threadName);
    try {
      this.inError = false;
      this.stop_message = "";
      Properties current_upload_item = null;
      if (this.thisSession != null) {
        if (this.thisSession.user != null)
          this.normal_writes = this.thisSession.user.getProperty("block_writes", "false").equals("false"); 
        this.pendingSyncs.removeAllElements();
        this.thisSession.uiPUT("receiving_file", "true");
        this.user_agent = null;
        this.active2.put("active", "true");
        this.active2.put("streamOpenStatus", "PENDING");
        this.proxy_remote_out = null;
        this.priorMd5 = "";
        String md5_str = "";
        updateTransferStats(this.thisSession, -1, this.httpUpload, null, this.md5, current_upload_item);
        String the_file = Common.last(this.the_dir);
        for (int x = 0; x < ServerStatus.SG("unsafe_filename_chars").length(); x++)
          the_file = the_file.replace(ServerStatus.SG("unsafe_filename_chars").charAt(x), '_'); 
        this.the_dir = String.valueOf(Common.all_but_last(this.the_dir)) + the_file;
        this.the_file_path = this.the_dir;
        this.the_file_name = this.the_file_path.substring(this.the_file_path.lastIndexOf("/") + 1, this.the_file_path.length()).trim();
        this.the_file_path = this.the_file_path.substring(0, this.the_file_path.lastIndexOf("/") + 1);
        this.quota = this.thisSession.get_quota(this.the_file_path);
        this.quota_exceeded = false;
        this.the_file_name = Common.replace_str(this.the_file_name, "%2F", "2F");
        this.the_file_name = Common.replace_str(this.the_file_name, "%2f", "2f");
        this.the_file_name = Common.url_decode(this.the_file_name);
        this.thisSession.uiPUT("last_file_real_path", this.item.getProperty("url"));
        this.thisSession.uiPUT("last_file_name", Common.last(this.item.getProperty("url")));
        this.c = this.thisSession.uVFS.getClient(this.item);
        String vrl_str = this.item.getProperty("url");
        the_file = Common.last(vrl_str);
        for (int i = 0; i < ServerStatus.SG("unsafe_filename_chars").length(); i++)
          the_file = the_file.replace(ServerStatus.SG("unsafe_filename_chars").charAt(i), '_'); 
        vrl_str = String.valueOf(Common.all_but_last(vrl_str)) + the_file;
        this.item.put("url", vrl_str);
        VRL vrl = new VRL(vrl_str);
        if (ServerStatus.BG("make_upload_parent_folders"))
          this.c.makedirs(Common.all_but_last(vrl.getPath())); 
        Properties stat = this.c.stat(vrl.getPath());
        this.priorMd5 = getPriorMd5(this.item, stat);
        if (!vrl.getProtocol().equalsIgnoreCase("file") && ServerStatus.BG("proxyKeepUploads")) {
          (new File_S(String.valueOf(ServerStatus.SG("proxyUploadRepository")) + this.thisSession.uiSG("user_name") + this.the_file_path)).mkdirs();
          this.proxy = new RandomAccessFile(new File_S(String.valueOf(ServerStatus.SG("proxyUploadRepository")) + this.thisSession.uiSG("user_name") + this.the_file_path + this.proxySDF.format(new Date()) + "_" + this.the_file_name), "rw");
          if (this.proxy.length() > this.current_loc)
            this.proxy.setLength(this.current_loc); 
        } 
        if (vrl.toString().length() > ServerStatus.IG("max_url_length"))
          throw new IOException("File url length too long:" + vrl.toString().length() + " vs. " + ServerStatus.IG("max_url_length")); 
        long max_upload_size = this.thisSession.LG("max_upload_size");
        long max_upload_amount = this.thisSession.LG("max_upload_amount");
        long max_upload_amount_day = this.thisSession.LG("max_upload_amount_day");
        long max_upload_amount_month = this.thisSession.LG("max_upload_amount_month");
        long max_upload_count = this.thisSession.LG("max_upload_count");
        long max_upload_count_day = this.thisSession.LG("max_upload_count_day");
        long max_upload_count_month = this.thisSession.LG("max_upload_count_month");
        long start_upload_amount_day = 0L;
        long start_upload_amount_month = 0L;
        long start_upload_count_day = 0L;
        long start_upload_count_month = 0L;
        if (this.thisSession.LG("max_upload_amount_day") > 0L)
          start_upload_amount_day = ServerStatus.thisObj.statTools.getTransferAmountToday(this.thisSession.uiSG("user_ip"), this.thisSession.uiSG("user_name"), this.thisSession.uiPG("stat"), "uploads", this.thisSession); 
        if (this.thisSession.LG("max_upload_amount_month") > 0L)
          start_upload_amount_month = ServerStatus.thisObj.statTools.getTransferAmountThisMonth(this.thisSession.uiSG("user_ip"), this.thisSession.uiSG("user_name"), this.thisSession.uiPG("stat"), "uploads", this.thisSession); 
        if (this.thisSession.LG("max_upload_count_day") > 0L)
          start_upload_count_day = ServerStatus.thisObj.statTools.getTransferCountToday(this.thisSession.uiSG("user_ip"), this.thisSession.uiSG("user_name"), this.thisSession.uiPG("stat"), "uploads", this.thisSession); 
        if (this.thisSession.LG("max_upload_count_month") > 0L)
          start_upload_count_month = ServerStatus.thisObj.statTools.getTransferCountThisMonth(this.thisSession.uiSG("user_ip"), this.thisSession.uiSG("user_name"), this.thisSession.uiPG("stat"), "uploads", this.thisSession); 
        long start_transfer_time = (new Date()).getTime();
        if (ServerStatus.count_users_ip(this.thisSession, this.thisSession.uiSG("user_protocol")) > Common.check_protocol(this.thisSession.uiSG("user_protocol"), SG("allowed_protocols"))) {
          this.inError = true;
          if (this.data_sock != null)
            this.data_sock.close(); 
          this.thisSession.do_kill();
        } else if (max_upload_amount > 0L && this.thisSession.uiLG("bytes_received") > max_upload_amount * 1024L) {
          this.thisSession.not_done = this.thisSession.ftp_write_command_logged("550-" + LOC.G("WARNING!!! Maximum upload amount reached.") + "  " + LOC.G("Received") + ":" + (this.thisSession.uiLG("bytes_received") / 1024L) + "k.  " + LOC.G("Max") + ":" + max_upload_amount + "k.  " + LOC.G("Available") + ":" + (max_upload_amount - this.thisSession.uiLG("bytes_received") / 1024L) + "k.", "STOR");
          this.thisSession.not_done = this.thisSession.ftp_write_command_logged("550", "%STOR-max reached%", "STOR");
          this.inError = true;
          this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
          this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
          ServerStatus.thisObj.runAlerts("user_upload_session", this.thisSession);
          if (this.data_sock != null) {
            this.data_sock.close();
          } else if (this.thisSession.data_socks.size() > 0) {
            ((Socket)this.thisSession.data_socks.remove(0)).close();
          } 
        } else if (max_upload_count > 0L && this.thisSession.uiLG("session_upload_count") > max_upload_count) {
          this.thisSession.not_done = this.thisSession.ftp_write_command_logged("550-" + LOC.G("WARNING!!! Maximum upload count reached.") + "  " + LOC.G("Received") + ":" + this.thisSession.uiLG("session_upload_count") + ".  " + LOC.G("Max") + ":" + max_upload_count + ".  " + LOC.G("Available") + ":" + (max_upload_count - this.thisSession.uiLG("session_upload_count")) + ".", "STOR");
          this.thisSession.not_done = this.thisSession.ftp_write_command_logged("550", "%STOR-max reached%", "STOR");
          this.inError = true;
          this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
          this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
          ServerStatus.thisObj.runAlerts("user_upload_session", this.thisSession);
          if (this.data_sock != null) {
            this.data_sock.close();
          } else if (this.thisSession.data_socks.size() > 0) {
            ((Socket)this.thisSession.data_socks.remove(0)).close();
          } 
        } else if (max_upload_size > 0L && this.current_loc > max_upload_size * 1024L) {
          this.thisSession.not_done = this.thisSession.ftp_write_command_logged("550-" + LOC.G("WARNING!!! Maximum upload amount file size reached.") + "  " + LOC.G("Max") + ":" + max_upload_size + "k.", "STOR");
          this.thisSession.not_done = this.thisSession.ftp_write_command_logged("550", "%STOR-max reached%", "STOR");
          this.inError = true;
          this.stop_message = String.valueOf(LOC.G("WARNING!!! Maximum upload amount file size reached.")) + "  " + LOC.G("Max") + ":" + max_upload_size + "k.";
          this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
          this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
          ServerStatus.thisObj.runAlerts("user_upload_session", this.thisSession);
          if (this.data_sock != null) {
            this.data_sock.close();
          } else if (this.thisSession.data_socks.size() > 0) {
            ((Socket)this.thisSession.data_socks.remove(0)).close();
          } 
        } else if ((max_upload_amount_day > 0L && start_upload_amount_day > max_upload_amount_day * 1024L) || (max_upload_amount_month > 0L && start_upload_amount_month > max_upload_amount_month * 1024L)) {
          this.inError = true;
          if (max_upload_amount_day > 0L && start_upload_amount_day > max_upload_amount_day * 1024L) {
            this.stop_message = String.valueOf(LOC.G("WARNING!!! Maximum upload amount today reached.")) + "  " + LOC.G("Received") + ":" + (start_upload_amount_day / 1024L) + "k.  " + LOC.G("Max") + ":" + max_upload_amount_day + "k.  ";
            this.thisSession.not_done = this.thisSession.ftp_write_command_logged("550-" + this.stop_message, "STOR");
            ServerStatus.thisObj.runAlerts("user_upload_day", this.thisSession);
          } 
          if (max_upload_amount_month > 0L && start_upload_amount_month > max_upload_amount_month * 1024L) {
            this.stop_message = String.valueOf(LOC.G("WARNING!!! Maximum upload amount last 30 days reached.")) + "  " + LOC.G("Received") + ":" + (start_upload_amount_month / 1024L) + "k.  " + LOC.G("Max") + ":" + max_upload_amount_month + "k.  ";
            this.thisSession.not_done = this.thisSession.ftp_write_command_logged("550-" + this.stop_message, "STOR");
            ServerStatus.thisObj.runAlerts("user_upload_month", this.thisSession);
          } 
          this.thisSession.not_done = this.thisSession.ftp_write_command_logged("550", "%STOR-max reached%", "STOR");
          this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
          this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
          if (this.data_sock != null) {
            this.data_sock.close();
          } else if (this.thisSession.data_socks.size() > 0) {
            ((Socket)this.thisSession.data_socks.remove(0)).close();
          } 
        } else if ((max_upload_count_day > 0L && start_upload_count_day > max_upload_count_day) || (max_upload_count_month > 0L && start_upload_count_month > max_upload_count_month)) {
          this.inError = true;
          if (max_upload_count_day > 0L && start_upload_count_day > max_upload_count_day) {
            this.stop_message = String.valueOf(LOC.G("WARNING!!! Maximum upload count today reached.")) + "  " + LOC.G("Received") + ":" + start_upload_count_day + ".  " + LOC.G("Max") + ":" + max_upload_count_day + ".  ";
            this.thisSession.not_done = this.thisSession.ftp_write_command_logged("550-" + this.stop_message, "STOR");
            ServerStatus.thisObj.runAlerts("user_upload_day", this.thisSession);
          } 
          if (max_upload_count_month > 0L && start_upload_count_month > max_upload_count_month) {
            this.stop_message = String.valueOf(LOC.G("WARNING!!! Maximum upload count last 30 days reached.")) + "  " + LOC.G("Received") + ":" + start_upload_count_month + ".  " + LOC.G("Max") + ":" + max_upload_count_month + ".  ";
            this.thisSession.not_done = this.thisSession.ftp_write_command_logged("550-" + this.stop_message, "STOR");
            ServerStatus.thisObj.runAlerts("user_upload_month", this.thisSession);
          } 
          this.thisSession.not_done = this.thisSession.ftp_write_command_logged("550", "%STOR-max reached%", "STOR");
          this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
          this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
          if (this.data_sock != null) {
            this.data_sock.close();
          } else if (this.thisSession.data_socks.size() > 0) {
            ((Socket)this.thisSession.data_socks.remove(0)).close();
          } 
        } else if (this.httpUpload && this.thisSession.uiLG("file_length") > this.thisSession.LG("max_upload_size") * 1024L && this.thisSession.LG("max_upload_size") != 0L) {
          this.thisSession.not_done = this.thisSession.ftp_write_command_logged("550", "%STOR-max reached%", "STOR");
          this.inError = true;
          this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
          this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
          this.stop_message = String.valueOf(LOC.G("Upload file size is too large.")) + this.thisSession.LG("max_upload_size") + "k max.";
          if (this.data_sock != null) {
            this.data_sock.close();
          } else if (this.thisSession.data_socks.size() > 0) {
            ((Socket)this.thisSession.data_socks.remove(0)).close();
          } 
        } else if (this.current_loc == 0L && this.item.getProperty("privs").indexOf("(delete)") < 0 && stat != null && Long.parseLong(stat.getProperty("size")) > 0L && this.item.getProperty("privs").indexOf("(view)") >= 0) {
          this.thisSession.not_done = this.thisSession.ftp_write_command_logged("550", "Cannot overwrite a file.", "STOR");
          this.inError = true;
          this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
          this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
          this.stop_message = LOC.G("Cannot overwrite a file.");
          if (this.data_sock != null) {
            this.data_sock.close();
          } else if (this.thisSession.data_socks.size() > 0) {
            ((Socket)this.thisSession.data_socks.remove(0)).close();
          } 
        } else {
          long resume_loc = this.current_loc;
          boolean binary_mode = this.thisSession.uiSG("file_transfer_mode").equals("BINARY");
          int loop_times = 0;
          while (this.data_sock == null && this.thisSession.data_socks.size() == 0 && loop_times++ < 10000)
            Thread.sleep(1L); 
          if (this.thisSession.data_socks.size() == 0 && this.data_sock == null) {
            this.thisSession.not_done = this.thisSession.ftp_write_command_logged("550", "%PORT-fail_question%" + CRLF + "%PORT-no_data_connection%", "STOR");
            this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
            this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
          } else {
            try {
              if (this.data_sock == null)
                this.data_sock = this.thisSession.data_socks.remove(0); 
              if (this.data_sock != null)
                this.data_sock.setSoTimeout(((this.thisSession.IG("max_idle_time") <= 0) ? 5 : this.thisSession.IG("max_idle_time")) * 1000 * 60); 
              if (this.thisSession.uiBG("modez")) {
                this.data_is = new InflaterInputStream(this.data_sock.getInputStream());
                this.zlibing = true;
              } else {
                this.data_is = new BufferedInputStream(this.data_sock.getInputStream());
              } 
              Properties pp = new Properties();
              String message_string = "";
              String responseNumber = "150";
              pp.put("the_file_path", this.the_file_path);
              pp.put("the_file_name", this.the_file_name);
              pp.put("message_string", message_string);
              pp.put("responseNumber", responseNumber);
              this.thisSession.runPlugin("before_upload", pp);
              message_string = pp.getProperty("message_string", message_string);
              responseNumber = pp.getProperty("responseNumber", responseNumber);
              String last_part = this.thisSession.uiSG("the_command_data");
              if (last_part.indexOf("/") >= 0)
                last_part = Common.last(last_part); 
              for (int j = 0; j < ServerStatus.SG("unsafe_filename_chars").length(); j++)
                last_part = last_part.replace(ServerStatus.SG("unsafe_filename_chars").charAt(j), '_'); 
              if (this.thisSession.uiSG("user_protocol").startsWith("FTP") && !this.block_ftp_fix && !last_part.trim().equals(this.the_file_name.trim()))
                if (ServerStatus.BG("block_bad_ftp_socket_paths"))
                  if (!this.thisSession.uiSG("the_command_data").trim().equals("")) {
                    responseNumber = "550";
                    message_string = String.valueOf(this.thisSession.uiSG("the_command_data")) + " does not equal " + this.the_file_name + ".  Bad FTP client.";
                  }   
              if (!message_string.equals("")) {
                this.thisSession.not_done = this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-", "STOR");
                this.thisSession.not_done = this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-" + message_string, "STOR");
                this.thisSession.not_done = this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-", "STOR");
              } 
              if (this.quota != -12345L && !ServerStatus.BG("hide_ftp_quota_log"))
                this.thisSession.not_done = this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-" + LOC.G("Quota space available") + ": " + Common.format_bytes_short2(this.quota), "STOR"); 
              if (this.thisSession.LG("max_download_amount") != 0L)
                this.thisSession.not_done = this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-" + LOC.G("Max Download") + ".  " + LOC.G("Sent") + ":" + (this.thisSession.uiLG("bytes_sent") / 1024L) + "k.  " + LOC.G("Max") + ":" + this.thisSession.LG("max_download_amount") + "k.  " + LOC.G("Available") + ":" + (this.thisSession.LG("max_download_amount") - this.thisSession.uiLG("bytes_sent") / 1024L) + "k.", "STOR"); 
              if (this.thisSession.LG("max_upload_amount") != 0L)
                this.thisSession.not_done = this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-" + LOC.G("Max Upload") + ".  " + LOC.G("Received") + ":" + (this.thisSession.uiLG("bytes_received") / 1024L) + "k.  " + LOC.G("Max") + ":" + this.thisSession.LG("max_upload_amount") + "k.  " + LOC.G("Available") + ":" + (this.thisSession.LG("max_upload_amount") - this.thisSession.uiLG("bytes_received") / 1024L) + "k.", "STOR"); 
              if (this.thisSession.LG("max_download_count") != 0L)
                this.thisSession.not_done = this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-" + LOC.G("Max Download") + ".  " + LOC.G("Sent") + ":" + this.thisSession.uiLG("session_download_count") + ".  " + LOC.G("Max") + ":" + this.thisSession.LG("max_download_count") + ".  " + LOC.G("Available") + ":" + (this.thisSession.LG("max_download_count") - this.thisSession.uiLG("session_download_count")) + ".", "STOR"); 
              if (this.thisSession.LG("max_upload_count") != 0L)
                this.thisSession.not_done = this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-" + LOC.G("Max Upload") + ".  " + LOC.G("Received") + ":" + this.thisSession.uiLG("session_upload_count") + ".  " + LOC.G("Max") + ":" + this.thisSession.LG("max_upload_count") + ".  " + LOC.G("Available") + ":" + (this.thisSession.LG("max_upload_count") - this.thisSession.uiLG("session_upload_count")) + ".", "STOR"); 
              if (this.thisSession.IG("max_upload_size") != 0)
                this.thisSession.not_done = this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-" + LOC.G("Max Upload File Size: ") + max_upload_size + "k.", "STOR"); 
              if (this.thisSession.IG("ratio") != 0)
                this.thisSession.not_done = this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-" + LOC.G("Ratio is $0 to 1.", (new StringBuffer(String.valueOf(this.thisSession.IG("ratio")))).toString()) + " " + LOC.G("Received") + ":" + ((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) / 1024L) + "k " + LOC.G("Sent") + ":" + ((this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.  " + LOC.G("Available") + ":" + (((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) * this.thisSession.IG("ratio") - this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.", "STOR"); 
              if (this.c.getConfig("pgpDecryptDownload", "").equals("true"))
                this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-pgpDecryptDownload:" + (new VRL(this.c.getConfig("pgpPrivateKeyDownloadPath", ""))).safe(), "STOR"); 
              if (this.c.getConfig("pgpEncryptDownload", "").equals("true"))
                this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-pgpEncryptDownload:" + (new VRL(this.c.getConfig("pgpPublicKeyDownloadPath", ""))).safe(), "STOR"); 
              if (this.c.getConfig("pgpDecryptUpload", "").equals("true"))
                this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-pgpDecryptUpload:" + (new VRL(this.c.getConfig("pgpPrivateKeyUploadPath", ""))).safe(), "STOR"); 
              if (this.c.getConfig("pgpEncryptUpload", "").equals("true"))
                this.thisSession.not_done = this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-pgpEncryptUpload:" + (new VRL(this.c.getConfig("pgpPublicKeyUploadPath", ""))).safe(), "STOR"); 
              if (this.thisSession.user != null && !this.thisSession.user.getProperty("filePublicEncryptionKey", "").equals(""))
                this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-pgpUserEncryption:" + (new VRL(this.thisSession.user.getProperty("filePublicEncryptionKey", ""))).safe(), "STOR"); 
              if (this.thisSession.user != null && !this.thisSession.user.getProperty("fileDecryptionKey", "").equals(""))
                this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-pgpUserDecryption:" + (new VRL(this.thisSession.user.getProperty("fileDecryptionKey", ""))).safe(), "STOR"); 
              if (!responseNumber.equals("150")) {
                this.thisSession.not_done = this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + " ABORTED", "STOR");
                Thread.sleep(100L);
                throw new Exception(message_string);
              } 
              checkZipstream(this.item);
              if (this.current_loc == 0L && !this.random_access) {
                if ((this.item.getProperty("privs").indexOf("(view)") < 0 || this.unique) && stat != null) {
                  Properties result = do_unique_rename(this.item, vrl, this.c, this.unique, this.the_file_name, stat);
                  vrl = (VRL)result.get("vrl");
                  stat = null;
                  this.the_file_name = (String)result.get("the_file_name");
                } 
                try {
                  if (this.quota != -12345L && stat != null) {
                    this.quota += Long.parseLong(stat.getProperty("size"));
                    this.thisSession.set_quota(this.the_dir, this.quota);
                  } 
                } catch (Exception e) {
                  if (e.indexOf("Interrupted") >= 0)
                    throw e; 
                } 
                if (ServerStatus.BG("recycle") && stat != null) {
                  this.thisSession.do_Recycle(vrl);
                  if (this.c instanceof com.crushftp.client.GenericClientMulti) {
                    this.c.setConfig("skip_first_client", "true");
                    this.c.delete(vrl.getPath());
                    this.c.setConfig("skip_first_client", "false");
                  } 
                } else if ((vrl.getProtocol().equalsIgnoreCase("file") || vrl.getProtocol().equalsIgnoreCase("s3crush")) && stat != null) {
                  if (!this.thisSession.uiSG("proxy_mode").equalsIgnoreCase("socket")) {
                    if (!this.the_file_name.endsWith(".zipstream")) {
                      Properties info = new Properties();
                      info.put("crushftp_user_name", this.thisSession.uiSG("user_name"));
                      Common.trackSyncRevision(this.c, vrl, String.valueOf(this.the_file_path) + this.the_file_name, this.thisSession.SG("root_dir"), this.item.getProperty("privs"), true, info);
                    } 
                    this.c.delete(vrl.getPath());
                  } 
                } 
              } 
              int data_read = 0;
              byte[] temp_array = new byte[this.packet_size];
              if (!this.httpUpload)
                this.thisSession.uiPUT("file_length", "0"); 
              vrl = doTempUploadRename(vrl, (stat == null));
              if (!this.thisSession.uiSG("proxy_mode").equalsIgnoreCase("socket") && !this.zipstream) {
                start_upload(vrl, binary_mode);
                if (current_upload_item != null)
                  ServerStatus.siVG("incoming_transfers").remove(current_upload_item); 
                current_upload_item = make_current_item(start_transfer_time);
                if (!Common.dmz_mode)
                  if (!this.thisSession.user.getProperty("filePublicEncryptionKey", "").equals("")) {
                    this.out = Common.getEncryptedStream(this.out, this.thisSession.user.getProperty("filePublicEncryptionKey", ""), this.current_loc, false);
                  } else if (ServerStatus.BG("fileEncryption") && !ServerStatus.SG("filePublicEncryptionKey").equals("")) {
                    this.out = Common.getEncryptedStream(this.out, ServerStatus.SG("filePublicEncryptionKey"), this.current_loc, ServerStatus.BG("file_encrypt_ascii"));
                  } else if (!this.thisSession.user.getProperty("fileEncryptionKey", "").equals("")) {
                    this.out = Common.getEncryptedStream(this.out, this.thisSession.user.getProperty("fileEncryptionKey", ""), this.current_loc, false);
                  } else if (ServerStatus.BG("fileEncryption")) {
                    this.out = Common.getEncryptedStream(this.out, ServerStatus.SG("fileEncryptionKey"), this.current_loc, false);
                  }  
              } 
              this.thisSession.not_done = this.thisSession.ftp_write_command_logged(responseNumber, "%STOR-start% " + LOC.G("\"$0$1\") S T O R", this.thisSession.stripRoot(this.the_file_path), this.the_file_name), "STOR");
              this.active2.put("streamOpenStatus", "OPEN");
              if (ServerStatus.BG("posix"))
                setDefaultsPOSIX(this.item, vrl); 
              this.thisSession.uiPUT("start_transfer_time", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
              this.thisSession.uiPUT("start_transfer_byte_amount", (new StringBuffer(String.valueOf(this.thisSession.uiLG("bytes_received")))).toString());
              TransferSpeedometer speedController = new TransferSpeedometer(this.thisSession, null, this);
              Worker.startWorker(speedController, String.valueOf(this.thisSession.uiSG("user_name")) + ":(" + this.thisSession.uiSG("user_number") + ")-" + this.thisSession.uiSG("user_ip") + " (speedometer stor)");
              if (this.thisSession.uiSG("proxy_mode").equalsIgnoreCase("socket") && SG("site").toUpperCase().indexOf("(SITE_PROXY)") >= 0) {
                Socket sock = new Socket(this.thisSession.uiSG("proxy_ip_address"), this.thisSession.uiIG("proxy_remote_port"));
                this.proxy_remote_out = sock.getOutputStream();
                BufferedInputStream proxy_remote_in = new BufferedInputStream(sock.getInputStream());
                OutputStream proxy_out = this.data_sock.getOutputStream();
                Thread t = new Thread(new Runnable(this, proxy_remote_in, proxy_out, sock) {
                      final STOR_handler this$0;
                      
                      private final BufferedInputStream val$proxy_remote_in;
                      
                      private final OutputStream val$proxy_out;
                      
                      private final Socket val$sock;
                      
                      public void run() {
                        byte[] b = new byte[32768];
                        int bytesRead = 0;
                        try {
                          while (this.this$0.active2.getProperty("active", "").equals("true") && bytesRead >= 0) {
                            bytesRead = this.val$proxy_remote_in.read(b);
                            Log.log("UPLOAD", 2, "proxy bytes:" + bytesRead);
                            if (bytesRead > 0)
                              this.val$proxy_out.write(b, 0, bytesRead); 
                          } 
                        } catch (Exception e) {
                          Log.log("UPLOAD", 1, e);
                        } 
                        try {
                          this.val$sock.close();
                          this.this$0.data_sock.close();
                        } catch (Exception e) {
                          Log.log("UPLOAD", 1, e);
                        } 
                        if (this.this$0.data_sock != null)
                          this.this$0.thisSession.old_data_socks.remove(this.this$0.data_sock); 
                      }
                    });
                t.setName(String.valueOf(this.thisSession.uiSG("user_name")) + ":(" + this.thisSession.uiSG("user_number") + ")-" + this.thisSession.uiSG("user_ip") + " STOR_handler_proxy_connector");
                t.start();
              } 
              if (!this.thisSession.SG("content_restriction").equals("") && !this.thisSession.SG("content_restriction").equals("content_restriction")) {
                boolean restrictions_ok = false;
                this.data_is.mark(32769);
                int bytes_read = 0;
                int read_max = 32768;
                byte[] b = (byte[])null;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                while (bytes_read >= 0 && read_max > 0) {
                  b = new byte[read_max];
                  bytes_read = this.data_is.read(b);
                  if (bytes_read > 0) {
                    baos.write(b, 0, bytes_read);
                    read_max -= bytes_read;
                  } 
                } 
                this.data_is.reset();
                b = baos.toByteArray();
                String[] crs = this.thisSession.SG("content_restriction").replace('\n', '\r').split("\r");
                for (int k = 0; k < crs.length; k++) {
                  String[] parts = crs[k].split(";");
                  if (this.the_file_path.toUpperCase().startsWith(parts[1].toUpperCase()) && Common.do_search(parts[2], this.the_file_name, false, 0))
                    if (parts[0].startsWith("aeszip")) {
                      boolean aes = false;
                      try {
                        ZipArchiveInputStream zais = new ZipArchiveInputStream(new ByteArrayInputStream(b));
                        while (!aes) {
                          ZipArchiveEntry ze = zais.getNextZipEntry();
                          if (ze == null)
                            break; 
                          if (ze.getMethod() == 99)
                            aes = true; 
                        } 
                        zais.close();
                      } catch (IOException iOException) {}
                      if (!aes)
                        throw new IOException("550 Error: " + parts[2] + " must be AES Encrypted."); 
                      restrictions_ok = true;
                    } else if (parts[0].startsWith("byte")) {
                      boolean ok = true;
                      if (parts.length >= 4) {
                        String[] bytes = parts[3].split(",");
                        for (int xx = 0; ok && xx < bytes.length; xx++) {
                          int m = Integer.parseInt(bytes[xx].split("=")[0].trim());
                          int v = Integer.parseInt(bytes[xx].split("=")[1].trim());
                          if (b[m] != v)
                            ok = false; 
                        } 
                      } 
                      if (!ok)
                        throw new IOException("550 Error: File content not allowed."); 
                      restrictions_ok = true;
                    }  
                } 
                if (!restrictions_ok)
                  throw new IOException("550 Error: File content restrictions failed."); 
              } 
              LineReader data_is_ascii = null;
              byte[] read_str = (byte[])null;
              if (!binary_mode) {
                data_is_ascii = new LineReader(this.data_is);
                this.asciiLineCount = 0L;
              } 
              this.startLoop = 0L;
              this.endLoop = 1000L;
              int lesserSpeed = reloadBandwidthLimits(true);
              long speedometerCheckInterval = (new Date()).getTime();
              ZipArchiveEntry entry = null;
              boolean user_speed_notified = false;
              this.thisSession.uiPUT("current_file", this.the_file_name);
              while (data_read >= 0 && !this.quota_exceeded) {
                if (this.thisSession.getProperty("blockUploads", "false").equals("true")) {
                  GenericClient c2 = this.thisSession.uVFS.getClient(this.item);
                  try {
                    if (this.c instanceof HTTPClient)
                      c2.setConfig("crushAuth", this.c.getConfig("crushAuth")); 
                    c2.doCommand("SITE BLOCK_UPLOADS");
                    c2.close();
                  } finally {
                    this.thisSession.uVFS.releaseClient(c2);
                  } 
                  throw new Exception("User cancelled!");
                } 
                Date new_date = new Date();
                if (new_date.getTime() - speedometerCheckInterval > 10000L) {
                  lesserSpeed = reloadBandwidthLimits(false);
                  speedometerCheckInterval = new_date.getTime();
                } 
                if (lesserSpeed > 0 && !speedController.bandwidth_immune_server) {
                  this.slow_transfer = speedController.getDelayAmount(data_read, this.startLoop, this.endLoop, temp_array.length, this.slow_transfer, lesserSpeed);
                  speedController.reloadBandwidthLimits();
                } 
                while (this.pause_transfer)
                  Thread.sleep(100L); 
                this.startLoop = System.currentTimeMillis();
                if (this.slow_transfer > 0.0F)
                  Thread.sleep((int)this.slow_transfer); 
                if (binary_mode) {
                  if (this.zlibing) {
                    data_read = ((InflaterInputStream)this.data_is).read(temp_array);
                  } else if (this.zipstream) {
                    if (entry != null) {
                      data_read = ((FileArchiveInputStream)this.data_is).read(temp_array);
                    } else {
                      data_read = 0;
                    } 
                  } else {
                    data_read = this.data_is.read(temp_array);
                  } 
                } else if ((read_str = data_is_ascii.readLineOS()) != null) {
                  data_read = read_str.length;
                  temp_array = read_str;
                  this.asciiLineCount = data_is_ascii.lineCount;
                } else {
                  data_read = -1;
                } 
                if (this.zipstream && data_read < 0) {
                  if (this.out != null) {
                    this.out.close();
                    writeEncryptedHeaderSize(this.c, vrl, this.current_loc);
                    this.c.close();
                  } 
                  this.out = null;
                  if (this.inError) {
                    Log.log("UPLOAD", 1, "An error occurred during the storing of file:" + vrl);
                    throw new Exception(this.stop_message.equals("") ? "HTTP Aborted" : this.stop_message);
                  } 
                  vrl = doTempUploadRenameDone(vrl);
                  try {
                    boolean disable_mdtm_modifications = ServerStatus.BG("disable_mdtm_modifications");
                    if (!this.thisSession.SG("disable_mdtm_modifications").equals("") && ServerStatus.BG("v9_beta"))
                      disable_mdtm_modifications = this.thisSession.BG("disable_mdtm_modifications"); 
                    if (entry != null && entry.getTime() > 0L && !disable_mdtm_modifications) {
                      if (this.c instanceof S3Client || this.c instanceof com.crushftp.client.S3CrushClient)
                        this.c.setConfig("uploaded_md5", md5_str); 
                      if (this.c instanceof S3Client || this.c instanceof com.crushftp.client.S3CrushClient)
                        this.c.setConfig("uploaded_by", "CrushFTP:" + this.thisSession.user.getProperty("username")); 
                      this.c.mdtm(vrl.getPath(), entry.getTime());
                    } else if (entry != null && entry.getTime() > 0L && (this.c instanceof S3Client || this.c instanceof com.crushftp.client.S3CrushClient)) {
                      this.c.setConfig("uploaded_md5", md5_str);
                      this.c.setConfig("uploaded_by", "CrushFTP:" + this.thisSession.user.getProperty("username"));
                      ((S3Client)this.c).set_MD5_and_upload_id(vrl.getPath());
                    } 
                  } catch (Exception e) {
                    Log.log("UPLOAD", 1, e);
                  } 
                  Properties properties1 = (Properties)this.item.clone();
                  Properties item2 = (Properties)this.item.clone();
                  if (entry != null) {
                    item2.put("url", String.valueOf(Common.all_but_last(this.item.getProperty("url"))) + entry.getName());
                    md5_str = (new BigInteger(1, this.md5.digest())).toString(16).toLowerCase();
                    while (md5_str.length() < 32)
                      md5_str = "0" + md5_str; 
                    String path2 = entry.getName();
                    if (path2.indexOf(":") >= 0)
                      path2 = path2.substring(0, path2.indexOf(":")); 
                    stat = this.c.stat(vrl.getPath());
                    String filePath = String.valueOf(this.the_file_path) + path2;
                    if (SG("temp_upload_ext").length() > 0 && filePath.endsWith(SG("temp_upload_ext")))
                      filePath = filePath.substring(0, filePath.length() - SG("temp_upload_ext").length()); 
                    Common.publishPendingSyncs(this.pendingSyncs);
                    if (!filePath.startsWith(this.thisSession.SG("root_dir")))
                      filePath = String.valueOf(this.thisSession.SG("root_dir")) + filePath; 
                    Common.trackSync("CHANGE", filePath, null, entry.isDirectory(), Long.parseLong(stat.getProperty("size")), entry.getTime(), this.thisSession.SG("root_dir"), this.item.getProperty("privs"), this.thisSession.uiSG("clientid"), this.priorMd5);
                    finishedUpload(this.c, this.thisSession, item2, String.valueOf(this.the_file_path) + Common.all_but_last(entry.getName()), vrl.getName(), this.stop_message, this.quota_exceeded, this.quota, this.httpUpload, vrl, this.asciiLineCount, binary_mode, start_transfer_time, md5_str, properties1, 0L, false, this.realAction, this.metaInfo, this.user_agent, this.current_loc);
                    this.last_md5s.addElement(md5_str);
                    while (this.last_md5s.size() > 100)
                      this.last_md5s.remove(0); 
                    if (ServerStatus.BG("posix"))
                      setDefaultsPOSIX(item2, vrl); 
                    while (this.thisSession.uiSG("session_uploads").length() / 160 > ServerStatus.IG("user_log_buffer"))
                      this.thisSession.uiPUT("session_uploads", this.thisSession.uiSG("session_uploads").substring(160)); 
                  } 
                  md5_str = "";
                  this.md5.reset();
                  entry = null;
                  data_read = 0;
                } 
                if (data_read >= 0) {
                  if (this.quota != -12345L) {
                    this.quota -= data_read;
                    if (this.quota < 0L) {
                      data_read = (int)(data_read + this.quota);
                      this.quota = 0L;
                      this.quota_exceeded = true;
                      this.stop_message = String.valueOf(LOC.G("ERROR: Your quota has been exceeded")) + ".  " + LOC.G("Available") + ": 0k.";
                      throw new Exception(this.stop_message.equals("") ? "HTTP Aborted" : this.stop_message);
                    } 
                    if (data_read < 0)
                      data_read = -1; 
                  } 
                  if (data_read >= 0) {
                    if (this.proxy_remote_out != null) {
                      this.proxy_remote_out.write(temp_array, 0, data_read);
                    } else if (!this.zipstream || entry != null) {
                      if (this.normal_writes)
                        this.out.write(temp_array, 0, data_read); 
                    } else if (entry == null) {
                      entry = ((FileArchiveInputStream)this.data_is).getNextZipEntry();
                      if (entry == null) {
                        data_read = -1;
                      } else {
                        String path2 = entry.getName();
                        long rest = -1L;
                        try {
                          if (path2.indexOf(":") >= 0) {
                            String[] parts = path2.substring(path2.indexOf(":") + 1).split(";");
                            path2 = path2.substring(0, path2.indexOf(":"));
                            for (int m = 0; m < parts.length; m++) {
                              String part = parts[m].split("=")[0];
                              if (part.equalsIgnoreCase("REST")) {
                                rest = Long.parseLong(parts[m].split("=")[1]);
                                this.thisSession.add_log("[" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: *REST " + rest + "*", "STOR");
                              } else if (part.equalsIgnoreCase("RANGE")) {
                                this.random_access = true;
                                rest = Long.parseLong(parts[m].split("=")[1]);
                                this.thisSession.add_log("[" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: *REST " + rest + " (randomaccess)*", "STOR");
                              } 
                            } 
                          } 
                        } catch (NumberFormatException e) {
                          Log.log("UPLOAD", 2, e);
                        } 
                        path2 = path2.replace('\\', '/');
                        path2 = Common.dots(path2);
                        if (path2.startsWith("/") && this.the_file_path.endsWith("/"))
                          path2 = path2.substring(1); 
                        Properties zipItem = this.thisSession.uVFS.get_item_parent(String.valueOf(this.the_file_path) + path2);
                        if (zipItem == null) {
                          if (!this.thisSession.check_access_privs(this.the_dir, "MKD", this.item)) {
                            this.thisSession.add_log("[" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: *UNZIP:MKD " + this.thisSession.stripRoot(this.the_file_path) + path2 + " DENIED!  Make dir not allowed.*", "MKD");
                            throw new Exception("Not allowed to create folder.");
                          } 
                          String buildPath = "";
                          String[] parts = Common.all_but_last(String.valueOf(this.the_file_path) + path2).split("/");
                          for (int m = 0; m < parts.length; m++) {
                            buildPath = String.valueOf(buildPath) + parts[m] + "/";
                            zipItem = this.thisSession.uVFS.get_item(buildPath);
                            if (zipItem == null) {
                              this.thisSession.add_log("[" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: *UNZIP:MKD " + this.thisSession.stripRoot(this.the_file_path) + buildPath + "*", "MKD");
                              zipItem = this.thisSession.uVFS.get_item_parent(buildPath);
                              if (zipItem.getProperty("url").length() > ServerStatus.IG("max_url_length"))
                                throw new IOException("File url length too long:" + zipItem.getProperty("url").length() + " vs. " + ServerStatus.IG("max_url_length")); 
                              this.c.makedir((new VRL(zipItem.getProperty("url"))).getPath());
                              this.thisSession.setFolderPrivs(this.c, zipItem);
                            } 
                          } 
                          zipItem = this.thisSession.uVFS.get_item_parent(String.valueOf(this.the_file_path) + path2);
                          if (zipItem.getProperty("url").length() > ServerStatus.IG("max_url_length"))
                            throw new IOException("File url length too long:" + zipItem.getProperty("url").length() + " vs. " + ServerStatus.IG("max_url_length")); 
                        } 
                        String urlChange = zipItem.getProperty("url");
                        the_file = Common.last(urlChange);
                        for (int k = 0; k < ServerStatus.SG("unsafe_filename_chars").length(); k++) {
                          char c = ServerStatus.SG("unsafe_filename_chars").charAt(k);
                          if (c != '/' && c != '\\' && c != ':')
                            the_file = the_file.replace(c, '_'); 
                        } 
                        urlChange = String.valueOf(Common.all_but_last(urlChange)) + the_file;
                        zipItem.put("url", urlChange);
                        vrl = new VRL(zipItem.getProperty("url"));
                        this.priorMd5 = "";
                        this.thisSession.add_log("[" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: *UNZIP " + this.thisSession.stripRoot(this.the_file_path) + path2 + "*", "STOR");
                        if (entry.isDirectory() || entry.getName().equals("")) {
                          if (this.thisSession.check_access_privs(this.the_dir, "MKD", this.item)) {
                            if (vrl.toString().length() > ServerStatus.IG("max_url_length"))
                              throw new IOException("File url length too long:" + vrl.toString().length() + " vs. " + ServerStatus.IG("max_url_length")); 
                            Common.verifyOSXVolumeMounted(vrl.toString());
                            this.c.makedirs(vrl.getPath());
                            this.thisSession.setFolderPrivs(this.c, zipItem);
                            Log.log("UPLOAD", 3, "Creating zip directory:" + vrl.getPath());
                            this.thisSession.add_log("[" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: *UNZIP:MKD " + this.thisSession.stripRoot(this.the_file_path) + path2 + "*", "MKD");
                          } else {
                            this.thisSession.add_log("[" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: *UNZIP:MKD " + this.thisSession.stripRoot(this.the_file_path) + path2 + " DENIED!  Make dir not allowed.*", "MKD");
                            if (!this.thisSession.check_access_privs(this.the_dir, "MKD", this.item))
                              throw new Exception("Not allowed to create folder."); 
                          } 
                        } else {
                          if (this.out != null) {
                            this.out.close();
                            writeEncryptedHeaderSize(this.c, vrl, this.current_loc);
                          } 
                          this.c.close();
                          this.out = null;
                          responseNumber = "150";
                          pp.put("the_file_path", Common.all_but_last(String.valueOf(this.the_file_path) + path2));
                          pp.put("the_file_name", Common.last(String.valueOf(this.the_file_path) + path2));
                          pp.put("message_string", message_string);
                          pp.put("responseNumber", responseNumber);
                          this.thisSession.runPlugin("before_upload", pp);
                          message_string = pp.getProperty("message_string", message_string);
                          responseNumber = pp.getProperty("responseNumber", responseNumber);
                          if (!responseNumber.equals("150")) {
                            Thread.sleep(100L);
                            throw new Exception(message_string);
                          } 
                          if (!this.thisSession.SG("user_name").equals("template") && this.thisSession.check_access_privs(this.the_dir, "MKD", this.item)) {
                            Common.verifyOSXVolumeMounted(vrl.toString());
                            this.c.makedirs(Common.all_but_last(vrl.getPath()));
                            this.thisSession.setFolderPrivs(this.c, this.thisSession.uVFS.get_item_parent(Common.all_but_last(vrl.getPath())));
                          } 
                          if (vrl.toString().length() > ServerStatus.IG("max_url_length"))
                            throw new IOException("File url length too long:" + vrl.toString().length() + " vs. " + ServerStatus.IG("max_url_length")); 
                          stat = this.c.stat(vrl.getPath());
                          if ((zipItem.getProperty("privs").indexOf("(view)") < 0 || this.unique) && stat != null) {
                            try {
                              int fileNameInt = 1;
                              String itemName = zipItem.getProperty("url");
                              String itemExt = "";
                              if (itemName.lastIndexOf(".") > 0 && (itemName.lastIndexOf(".") == itemName.length() - 4 || itemName.lastIndexOf(".") == itemName.length() - 5)) {
                                itemExt = itemName.substring(itemName.lastIndexOf("."));
                                itemName = itemName.substring(0, itemName.lastIndexOf("."));
                              } 
                              VRL vrl2 = vrl;
                              Properties stat2 = stat;
                              while (stat2 != null) {
                                fileNameInt++;
                                zipItem.put("url", String.valueOf(itemName) + fileNameInt + itemExt);
                                zipItem.put("name", this.the_file_name);
                                vrl2 = new VRL(zipItem.getProperty("url"));
                                stat2 = this.c.stat(vrl2.getPath());
                              } 
                              if (this.unique) {
                                this.the_file_name = Common.last(String.valueOf(itemName) + fileNameInt + itemExt);
                                stat = stat2;
                                vrl = vrl2;
                              } else if (!stat.getProperty("size").equals("0")) {
                                this.c.rename(vrl.getPath(), vrl2.getPath());
                              } 
                            } catch (Exception e) {
                              if (e.indexOf("Interrupted") >= 0)
                                throw e; 
                            } 
                          } else if (zipItem.getProperty("privs").indexOf("(delete)") < 0 && stat != null) {
                            entry = null;
                          } 
                          this.priorMd5 = getPriorMd5(zipItem, stat);
                          if (entry != null) {
                            boolean ok = false;
                            if (this.thisSession.check_access_privs(String.valueOf(this.the_file_path) + path2, "STOR", zipItem) && Common.filter_check("U", Common.last(String.valueOf(this.the_file_path) + path2), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + this.thisSession.SG("file_filter")))
                              ok = true; 
                            if (!ok)
                              throw new IOException("Access not allowed for:" + this.the_file_path + path2); 
                            if (rest == -1L) {
                              md5_str = (new BigInteger(1, this.md5.digest())).toString(16).toLowerCase();
                              while (md5_str.length() < 32)
                                md5_str = "0" + md5_str; 
                              long statSize = 0L;
                              if (stat != null)
                                statSize = Long.parseLong(stat.getProperty("size")); 
                              if (this.item.getProperty("privs").indexOf("(sync") >= 0) {
                                String filePath = String.valueOf(this.the_file_path) + path2;
                                if (!filePath.startsWith(this.thisSession.SG("root_dir")))
                                  filePath = String.valueOf(this.thisSession.SG("root_dir")) + filePath; 
                                Properties info = new Properties();
                                info.put("crushftp_user_name", this.thisSession.uiSG("user_name"));
                                Common.trackSyncRevision(this.c, vrl, filePath, this.thisSession.SG("root_dir"), this.item.getProperty("privs"), true, info);
                                Common.trackPendingSync(this.pendingSyncs, "CHANGE", filePath, null, entry.isDirectory(), statSize, entry.getTime(), this.thisSession.SG("root_dir"), this.item.getProperty("privs"), this.thisSession.uiSG("clientid"), this.priorMd5);
                              } 
                              this.c.delete(vrl.getPath());
                            } 
                            vrl = doTempUploadRename(vrl, (stat == null));
                            this.current_loc = rest;
                            start_upload(vrl, binary_mode);
                            if (current_upload_item != null)
                              ServerStatus.siVG("incoming_transfers").remove(current_upload_item); 
                            current_upload_item = make_current_item(start_transfer_time);
                            if (!Common.dmz_mode)
                              if (!this.thisSession.user.getProperty("filePublicEncryptionKey", "").equals("")) {
                                this.out = Common.getEncryptedStream(this.out, this.thisSession.user.getProperty("filePublicEncryptionKey", ""), this.current_loc, false);
                              } else if (ServerStatus.BG("fileEncryption") && !ServerStatus.SG("filePublicEncryptionKey").equals("")) {
                                this.out = Common.getEncryptedStream(this.out, ServerStatus.SG("filePublicEncryptionKey"), this.current_loc, ServerStatus.BG("file_encrypt_ascii"));
                              } else if (!this.thisSession.user.getProperty("fileEncryptionKey", "").equals("")) {
                                this.out = Common.getEncryptedStream(this.out, this.thisSession.user.getProperty("fileEncryptionKey", ""), this.current_loc, false);
                              } else if (ServerStatus.BG("fileEncryption")) {
                                this.out = Common.getEncryptedStream(this.out, ServerStatus.SG("fileEncryptionKey"), this.current_loc, false);
                              }  
                            this.the_file_name = vrl.getName();
                            this.thisSession.uiPUT("current_file", this.the_file_name);
                            this.thisSession.uiPUT("start_transfer_byte_amount", (new StringBuffer(String.valueOf(this.thisSession.uiLG("bytes_received")))).toString());
                          } 
                        } 
                      } 
                    } 
                    if (!vrl.getProtocol().equalsIgnoreCase("file") && this.proxy != null)
                      this.proxy.write(temp_array, 0, data_read); 
                    updateTransferStats(this.thisSession, data_read, this.httpUpload, temp_array, this.md5, current_upload_item);
                    this.current_loc += ((data_read > 0) ? data_read : 0L);
                    if (current_upload_item != null && this.current_loc > Long.parseLong(current_upload_item.getProperty("size", "0"))) {
                      current_upload_item.put("size", (new StringBuffer(String.valueOf(this.current_loc))).toString());
                      current_upload_item.put("the_file_size", (new StringBuffer(String.valueOf(this.current_loc))).toString());
                    } 
                  } 
                } else {
                  data_read = -1;
                } 
                if (max_upload_amount > 0L && this.thisSession.uiLG("bytes_received") > max_upload_amount * 1024L) {
                  data_read = -1;
                  String msg = String.valueOf(LOC.G("WARNING!!! Maximum upload amount reached.")) + "  " + LOC.G("Received") + ":" + (this.thisSession.uiLG("bytes_received") / 1024L) + "k.  " + LOC.G("Max") + ":" + max_upload_amount + "k.  " + LOC.G("Available") + ":" + (max_upload_amount - this.thisSession.uiLG("bytes_received") / 1024L) + "k.";
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-" + msg, "STOR");
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-%STOR-max reached%", "STOR");
                  ServerStatus.thisObj.runAlerts("user_upload_session", this.thisSession);
                  this.stop_message = msg;
                  if (ServerStatus.BG("delete_partial_uploads"))
                    throw new Exception(msg); 
                } else if (max_upload_count > 0L && this.thisSession.uiLG("session_upload_count") > max_upload_count) {
                  data_read = -1;
                  String msg = String.valueOf(LOC.G("WARNING!!! Maximum upload count reached.")) + "  " + LOC.G("Received") + ":" + this.thisSession.uiLG("session_upload_count") + ".  " + LOC.G("Max") + ":" + max_upload_count + ".  " + LOC.G("Available") + ":" + (max_upload_count - this.thisSession.uiLG("session_upload_count")) + ".";
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-" + msg, "STOR");
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-%STOR-max reached%", "STOR");
                  ServerStatus.thisObj.runAlerts("user_upload_session", this.thisSession);
                  this.stop_message = msg;
                  if (ServerStatus.BG("delete_partial_uploads"))
                    throw new Exception(msg); 
                } else if (max_upload_size > 0L && this.current_loc > max_upload_size * 1024L) {
                  data_read = -1;
                  String msg = String.valueOf(LOC.G("WARNING!!! Maximum upload amount file size reached.")) + "  " + LOC.G("Max") + ":" + max_upload_size + "k.  ";
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-" + msg, "STOR");
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-%STOR-max reached%", "STOR");
                  ServerStatus.thisObj.runAlerts("user_upload_session", this.thisSession);
                  this.stop_message = msg;
                  if (ServerStatus.BG("delete_partial_uploads"))
                    throw new Exception(msg); 
                } else if (max_upload_amount_day > 0L && this.thisSession.uiLG("bytes_received") + start_upload_amount_day > max_upload_amount_day * 1024L) {
                  data_read = -1;
                  String msg = String.valueOf(LOC.G("WARNING!!! Maximum upload amount today reached.")) + "  " + LOC.G("Received") + ":" + ((this.thisSession.uiLG("bytes_received") + start_upload_amount_day) / 1024L) + "k.  " + LOC.G("Max") + ":" + max_upload_amount_day + "k.  ";
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-" + msg, "STOR");
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-%STOR-max reached%", "STOR");
                  ServerStatus.thisObj.runAlerts("user_upload_day", this.thisSession);
                  this.stop_message = msg;
                  if (ServerStatus.BG("delete_partial_uploads"))
                    throw new Exception(msg); 
                } else if (max_upload_amount_month > 0L && this.thisSession.uiLG("bytes_received") + start_upload_amount_month > max_upload_amount_month * 1024L) {
                  data_read = -1;
                  String msg = String.valueOf(LOC.G("WARNING!!! Maximum upload amount last 30 days reached.")) + "  " + LOC.G("Received") + ":" + ((this.thisSession.uiLG("bytes_received") + start_upload_amount_month) / 1024L) + "k.  " + LOC.G("Max") + ":" + max_upload_amount_month + "k.  ";
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-" + msg, "STOR");
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-%STOR-max reached%", "STOR");
                  ServerStatus.thisObj.runAlerts("user_upload_month", this.thisSession);
                  this.stop_message = msg;
                  if (ServerStatus.BG("delete_partial_uploads"))
                    throw new Exception(msg); 
                } else if (max_upload_count_day > 0L && this.thisSession.uiLG("session_upload_count") + start_upload_count_day > max_upload_count_day) {
                  data_read = -1;
                  String msg = String.valueOf(LOC.G("WARNING!!! Maximum upload count today reached.")) + "  " + LOC.G("Received") + ":" + (this.thisSession.uiLG("session_upload_count") + start_upload_count_day) + ".  " + LOC.G("Max") + ":" + max_upload_count_day + ".  ";
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-" + msg, "STOR");
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-%STOR-max reached%", "STOR");
                  ServerStatus.thisObj.runAlerts("user_upload_day", this.thisSession);
                  this.stop_message = msg;
                  if (ServerStatus.BG("delete_partial_uploads"))
                    throw new Exception(msg); 
                } else if (max_upload_count_month > 0L && this.thisSession.uiLG("bytes_received") + start_upload_count_month > max_upload_count_month) {
                  data_read = -1;
                  String msg = String.valueOf(LOC.G("WARNING!!! Maximum upload count last 30 days reached.")) + "  " + LOC.G("Received") + ":" + (this.thisSession.uiLG("session_upload_count") + start_upload_count_month) + ".  " + LOC.G("Max") + ":" + max_upload_count_month + ".  ";
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-" + msg, "STOR");
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-%STOR-max reached%", "STOR");
                  ServerStatus.thisObj.runAlerts("user_upload_month", this.thisSession);
                  this.stop_message = msg;
                  if (ServerStatus.BG("delete_partial_uploads"))
                    throw new Exception(msg); 
                } 
                if (this.thisSession.uiLG("overall_transfer_speed") < this.thisSession.IG("min_upload_speed") && this.thisSession.IG("min_upload_speed") > 0 && new_date.getTime() - this.thisSession.uiLG("start_transfer_time") > (ServerStatus.IG("minimum_speed_warn_seconds") * 1000))
                  throw new Exception(LOC.G("Transfer speed is less than minimum required after at least 10 seconds.") + "  " + this.thisSession.uiLG("overall_transfer_speed") + "K/sec < " + this.thisSession.IG("min_upload_speed") + "K/sec."); 
                if (this.thisSession.uiLG("overall_transfer_speed") < Math.abs(this.thisSession.IG("min_upload_speed")) && this.thisSession.IG("min_upload_speed") < 0 && new_date.getTime() - this.thisSession.uiLG("start_transfer_time") > (ServerStatus.IG("minimum_speed_alert_seconds") * 1000)) {
                  if (!user_speed_notified)
                    ServerStatus.thisObj.runAlerts("user_upload_speed", this.thisSession); 
                  user_speed_notified = true;
                } 
                this.endLoop = System.currentTimeMillis();
              } 
              if (this.thisSession.getProperty("blockUploads", "false").equals("true")) {
                GenericClient c2 = this.thisSession.uVFS.getClient(this.item);
                try {
                  c2.doCommand("SITE BLOCK_UPLOADS");
                  c2.close();
                } finally {
                  this.thisSession.uVFS.releaseClient(c2);
                } 
                throw new Exception("ERROR:Upload cancelled!");
              } 
              try {
                this.thisSession.uiPUT("receiving_file", "false");
              } catch (Exception exception) {}
              if (this.zlibing) {
                ((InflaterInputStream)this.data_is).close();
              } else {
                this.data_is.close();
              } 
              try {
                this.data_sock.close();
              } catch (Exception e) {
                if (e.indexOf("Interrupted") >= 0)
                  throw e; 
              } 
              if (this.data_sock != null)
                this.thisSession.old_data_socks.remove(this.data_sock); 
              if (this.out != null) {
                this.out.close();
                writeEncryptedHeaderSize(this.c, vrl, this.current_loc);
                this.c.close();
              } 
              this.out = null;
              try {
                if (this.proxy != null)
                  this.proxy.close(); 
              } catch (Exception exception) {}
              if (this.inError) {
                Log.log("UPLOAD", 1, "An error occurred during the storing of file:" + vrl.safe());
                throw new Exception(this.stop_message.equals("") ? "HTTP Aborted" : this.stop_message);
              } 
              vrl = doTempUploadRenameDone(vrl);
              Properties fileItem = (Properties)this.item.clone();
              if (this.zipstream) {
                this.stop_message = "";
                this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226", "%STOR-end% " + LOC.G("(\"$0$1\" $2) " + this.realAction, "", "", ""), "STOR");
              } else {
                md5_str = (new BigInteger(1, this.md5.digest())).toString(16).toLowerCase();
                while (md5_str.length() < 32)
                  md5_str = "0" + md5_str; 
                boolean disable_mdtm_modifications = ServerStatus.BG("disable_mdtm_modifications");
                if (!this.thisSession.SG("disable_mdtm_modifications").equals("") && ServerStatus.BG("v9_beta"))
                  disable_mdtm_modifications = this.thisSession.BG("disable_mdtm_modifications"); 
                if (!disable_mdtm_modifications) {
                  if (this.c instanceof S3Client || this.c instanceof com.crushftp.client.S3CrushClient)
                    this.c.setConfig("uploaded_md5", md5_str); 
                  if (this.c instanceof S3Client || this.c instanceof com.crushftp.client.S3CrushClient)
                    this.c.setConfig("uploaded_by", "CrushFTP:" + this.thisSession.user.getProperty("username")); 
                  if (this.httpUpload) {
                    this.c.mdtm(vrl.getPath(), this.fileModifiedDate);
                  } else if (this.c instanceof S3Client || this.c instanceof com.crushftp.client.S3CrushClient) {
                    ((S3Client)this.c).set_MD5_and_upload_id(vrl.getPath());
                  } 
                } else if (this.c instanceof S3Client || this.c instanceof com.crushftp.client.S3CrushClient) {
                  this.c.setConfig("uploaded_md5", md5_str);
                  this.c.setConfig("uploaded_by", "CrushFTP:" + this.thisSession.user.getProperty("username"));
                  ((S3Client)this.c).set_MD5_and_upload_id(vrl.getPath());
                } 
                stat = this.c.stat(vrl.getPath());
                String filePath = String.valueOf(this.the_file_path) + this.the_file_name;
                if (SG("temp_upload_ext").length() > 0 && filePath.endsWith(SG("temp_upload_ext")))
                  filePath = filePath.substring(0, filePath.length() - SG("temp_upload_ext").length()); 
                Common.publishPendingSyncs(this.pendingSyncs);
                if (!filePath.startsWith(this.thisSession.SG("root_dir")))
                  filePath = String.valueOf(this.thisSession.SG("root_dir")) + filePath; 
                if (!this.the_file_name.endsWith(".zipstream") && stat != null)
                  Common.trackSync("CHANGE", filePath, null, false, Long.parseLong(stat.getProperty("size")), Long.parseLong(stat.getProperty("modified")), this.thisSession.SG("root_dir"), this.item.getProperty("privs"), this.thisSession.uiSG("clientid"), this.priorMd5); 
                this.stop_message = finishedUpload(this.c, this.thisSession, this.item, this.the_file_path, this.the_file_name, this.stop_message, this.quota_exceeded, this.quota, this.httpUpload, vrl, this.asciiLineCount, binary_mode, start_transfer_time, md5_str, fileItem, resume_loc, true, this.realAction, this.metaInfo, this.user_agent, this.current_loc);
                this.last_md5s.addElement(md5_str);
                while (this.last_md5s.size() > 100)
                  this.last_md5s.remove(0); 
              } 
              if (!this.stop_message.equals(""))
                this.inError = true; 
              if (current_upload_item != null)
                ServerStatus.siVG("incoming_transfers").remove(current_upload_item); 
            } catch (Exception e) {
              this.inError = true;
              this.stop_message = e.getMessage();
              if (this.stop_message == null)
                this.stop_message = "null"; 
              Log.log("UPLOAD", 1, e);
              Log.log("UPLOAD", 1, this.the_file_path);
              Log.log("UPLOAD", 1, this.the_file_name);
              this.thisSession.add_log("[" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: Error with files (path):" + this.thisSession.stripRoot(this.the_file_path), "STOR");
              this.thisSession.add_log("[" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: Error with files (name):" + this.the_file_name, "STOR");
              if (current_upload_item != null)
                ServerStatus.siVG("incoming_transfers").remove(current_upload_item); 
              try {
                this.thisSession.uiPUT("receiving_file", "false");
              } catch (Exception exception) {}
              try {
                this.thisSession.uiPUT("overall_transfer_speed", "0");
              } catch (Exception exception) {}
              try {
                this.thisSession.uiPUT("current_transfer_speed", "0");
              } catch (Exception exception) {}
              if (this.c != null && this.thisSession.getProperty("blockUploads", "false").equals("false")) {
                String str = String.valueOf(this.the_file_path) + this.the_file_name;
                if (str.startsWith(this.thisSession.SG("root_dir")))
                  str = str.substring(this.thisSession.SG("root_dir").length()); 
                if (!str.startsWith("/"))
                  str = "/" + str; 
                this.c.doCommand("ABORT " + str);
              } 
              try {
                this.data_is.close();
              } catch (Exception ee) {
                Log.log("UPLOAD", 1, ee);
              } 
              try {
                this.data_sock.close();
              } catch (Exception ee) {
                Log.log("UPLOAD", 1, ee);
              } 
              if (this.data_sock != null)
                this.thisSession.old_data_socks.remove(this.data_sock); 
              try {
                if (this.out != null) {
                  this.out.close();
                  writeEncryptedHeaderSize(this.c, vrl, this.current_loc);
                } 
              } catch (Exception ee) {
                Log.log("UPLOAD", 1, ee);
              } 
              stat = this.c.stat(vrl.getPath());
              try {
                if (this.allowTempExtensions && !SG("temp_upload_ext").equals("") && !SG("temp_upload_ext").equals("temp_upload_ext") && (vrl.getName().endsWith(SG("temp_upload_ext")) || SG("temp_upload_ext").startsWith("!!"))) {
                  Log.log("UPLOAD", 2, "Temp uploaded file still exist:" + vrl);
                  if (ServerStatus.BG("delete_partial_uploads") || vrl.getProtocol().equalsIgnoreCase("HADOOP") || vrl.getProtocol().equalsIgnoreCase("S3")) {
                    Log.log("UPLOAD", 0, "Deleting partial uploaded file:" + vrl);
                    if (this.quota != -12345L)
                      this.quota += Long.parseLong(stat.getProperty("size")); 
                    this.c.delete(vrl.getPath());
                  } else if (!this.httpUpload || this.thisSession.uiLG("file_length") == Long.parseLong(stat.getProperty("size"))) {
                    String u = vrl.toString();
                    if (u.endsWith("/"))
                      u = u.substring(0, u.length() - 1); 
                    VRL vrl2 = new VRL(u.substring(0, vrl.toString().length() - SG("temp_upload_ext").length()));
                    this.c.rename(vrl.getPath(), vrl2.getPath());
                    vrl = vrl2;
                  } 
                } 
              } catch (Exception ee) {
                Log.log("UPLOAD", 1, ee);
              } 
              try {
                this.c.close();
              } catch (Exception ee) {
                Log.log("UPLOAD", 1, ee);
              } 
              this.out = null;
              if (this.thisSession.uVFS != null)
                this.c = this.thisSession.uVFS.releaseClient(this.c); 
              try {
                if (this.proxy != null)
                  this.proxy.close(); 
              } catch (Exception ee) {
                Log.log("UPLOAD", 1, ee);
              } 
              if (!this.stop_message.equals(e.getMessage()))
                this.stop_message = e.getMessage(); 
              if (this.stop_message.equals(""))
                this.stop_message = LOC.G("Transfer failed") + "!"; 
              Common.publishPendingSyncs(this.pendingSyncs);
              String filePath = String.valueOf(this.the_file_path) + this.the_file_name;
              if (!filePath.startsWith(this.thisSession.SG("root_dir")))
                filePath = String.valueOf(this.thisSession.SG("root_dir")) + filePath; 
              if (!this.the_file_name.endsWith(".zipstream") && stat != null)
                Common.trackSync("CHANGE", filePath, null, false, Long.parseLong(stat.getProperty("size")), Long.parseLong(stat.getProperty("modified")), this.thisSession.SG("root_dir"), this.item.getProperty("privs"), this.thisSession.uiSG("clientid"), ""); 
              Properties fileItem = (Properties)this.item.clone();
              try {
                fileItem.put("the_command", this.realAction);
                fileItem.put("url", this.item.getProperty("url"));
                if (this.item.getProperty("url").endsWith(".zipstream"))
                  fileItem.put("url", String.valueOf(Common.all_but_last(this.item.getProperty("url"))) + this.the_file_name); 
                fileItem.put("the_file_path", String.valueOf(this.the_file_path) + this.the_file_name);
                fileItem.put("the_file_name", this.the_file_name);
                fileItem.put("name", this.the_file_name);
                fileItem.put("the_file_size", (new StringBuffer(String.valueOf(this.current_loc))).toString());
                fileItem.put("size", fileItem.getProperty("the_file_size"));
                fileItem.put("the_file_speed", (new StringBuffer(String.valueOf(this.thisSession.uiLG("overall_transfer_speed")))).toString());
                fileItem.put("the_file_error", this.stop_message);
                fileItem.put("the_file_status", "FAILED");
                fileItem.put("the_file_type", "FILE");
                if (this.metaInfo != null)
                  fileItem.put("metaInfo", this.metaInfo); 
              } catch (Exception ee) {
                Log.log("SERVER", 1, ee);
              } 
              this.thisSession.uiPUT("session_uploads", String.valueOf(this.thisSession.uiSG("session_uploads")) + this.thisSession.stripRoot(this.the_file_path) + this.the_file_name + ":" + this.thisSession.uiLG("bytes_received") + LOC.G("bytes") + " @ " + this.thisSession.uiLG("overall_transfer_speed") + "k/sec. " + LOC.G("FAILED") + CRLF);
              if (this.thisSession.IG("ratio") != 0)
                this.thisSession.not_done = this.thisSession.ftp_write_command_logged("550-" + LOC.G("Ratio is $0 to 1.", (new StringBuffer(String.valueOf(this.thisSession.IG("ratio")))).toString()) + " " + LOC.G("Received") + ":" + ((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) / 1024L) + "k " + LOC.G("Sent") + ":" + ((this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.  " + LOC.G("Available") + ":" + (((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) * this.thisSession.IG("ratio") - this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.", "STOR"); 
              if (ServerStatus.BG("rfc_proxy") && e.getMessage() != null && e.getMessage().length() > 3 && e.getMessage().charAt(3) == ' ') {
                this.thisSession.not_done = this.thisSession.ftp_write_command_logged(e.getMessage(), "STOR");
              } else {
                this.thisSession.not_done = this.thisSession.ftp_write_command_logged("550", String.valueOf(e.getMessage()) + " " + LOC.G("(\"$0$1\") " + this.realAction, this.thisSession.stripRoot(this.the_file_path), this.the_file_name), "STOR");
              } 
              this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
              if (this.the_file_name.indexOf(".DS_Store") < 0 && this.thisSession.uiLG("bytes_received") > 0L) {
                Properties lastUploadStat = ServerStatus.thisObj.statTools.add_item_stat(this.thisSession, fileItem, "UPLOAD");
                if (this.metaInfo != null)
                  lastUploadStat.put("metaInfo", this.metaInfo); 
                this.thisSession.uiPUT("lastUploadStat", lastUploadStat);
                Vector lastUploadStats = this.thisSession.uiVG("lastUploadStats");
                lastUploadStats.addElement(this.thisSession.uiPG("lastUploadStat"));
                if (fileItem != null)
                  fileItem.put("uploadStats", this.thisSession.uiPG("lastUploadStat")); 
                while (lastUploadStats.size() > ServerStatus.IG("user_log_buffer"))
                  lastUploadStats.removeElementAt(0); 
              } 
              if (fileItem != null) {
                if (!fileItem.containsKey("the_command_data"))
                  fileItem.put("the_command_data", this.the_file_path); 
                if (!fileItem.containsKey("the_command"))
                  fileItem.put("the_command", "STOR"); 
                this.thisSession.do_event5("UPLOAD", fileItem);
              } 
              this.thisSession.add_log("[" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: " + LOC.G("Error") + ":" + e, "STOR");
              if (this.thisSession.ftp != null)
                this.thisSession.ftp.stop_idle_timer(); 
            } 
            if (ServerStatus.BG("posix"))
              setDefaultsPOSIX(this.item, vrl); 
            if (this.quota != -12345L)
              this.thisSession.set_quota(this.the_file_path, this.quota); 
            this.thisSession.uiPUT("receiving_file", "false");
          } 
        } 
      } 
      this.thisSession.uiPUT("current_file", "N/A");
      while (this.thisSession.uiSG("session_uploads").length() / 160 > ServerStatus.IG("user_log_buffer") && this.thisSession.uiSG("session_uploads").length() > 160)
        this.thisSession.uiPUT("session_uploads", this.thisSession.uiSG("session_uploads").substring(160)); 
      try {
        this.thisSession.uVFS.reset();
      } catch (Exception exception) {}
      try {
        this.thisSession.uiPUT("last_action", "STOR-" + LOC.G("Done."));
      } catch (Exception exception) {}
      try {
        if (this.out != null)
          this.out.close(); 
      } catch (Exception exception) {}
      try {
        if (this.c != null)
          this.c.close(); 
      } catch (Exception exception) {}
      try {
        if (this.thisSession.uVFS != null) {
          this.c = this.thisSession.uVFS.releaseClient(this.c);
        } else if (this.c != null) {
          this.c.logout();
        } 
      } catch (Exception exception) {}
      try {
        this.proxy.close();
      } catch (Exception exception) {}
      try {
        this.data_is.close();
      } catch (Exception exception) {}
      try {
        this.data_sock.close();
      } catch (Exception exception) {}
      if (this.data_sock != null)
        this.thisSession.old_data_socks.remove(this.data_sock); 
      try {
        this.proxy_remote_out.close();
      } catch (Exception exception) {}
      try {
        this.thisSession.uiPUT("overall_transfer_speed", "0");
      } catch (Exception exception) {}
      try {
        this.thisSession.uiPUT("current_transfer_speed", "0");
      } catch (Exception exception) {}
      try {
        this.thisSession.uiPUT("receiving_file", "false");
      } catch (Exception exception) {}
      if (this.thisSession.ftp != null)
        this.thisSession.ftp.start_idle_timer(); 
      this.active2.put("streamOpenStatus", "CLOSED");
      this.zlibing = false;
    } catch (Exception e) {
      Log.log("UPLOAD", 1, e);
    } finally {
      this.thisThread = null;
      this.active2.put("active", "false");
    } 
    kill();
    while (this.thisSession.stor_files_pool_used.indexOf(this) >= 0)
      this.thisSession.stor_files_pool_used.removeElement(this); 
    if (!this.wait_for_parent_free && this.thisSession.stor_files_pool_free.indexOf(this) < 0)
      this.thisSession.stor_files_pool_free.addElement(this); 
  }
  
  public void freeStor() {
    int loops = 0;
    while (this.thisSession.stor_files_pool_used.indexOf(this) >= 0 && loops++ < 10000) {
      try {
        Thread.sleep(1L);
      } catch (InterruptedException interruptedException) {}
    } 
    if (ServerStatus.BG("stor_pooling") && this.thisSession.stor_files_pool_free.indexOf(this) < 0 && loops < 9999)
      this.thisSession.stor_files_pool_free.addElement(this); 
  }
  
  public void start_upload(VRL vrl, boolean binary_mode) throws Exception {
    try {
      int loops = 0;
      while (loops++ < 2) {
        try {
          if (this.c instanceof HTTPClient)
            ((HTTPClient)this.c).sendMetaInfo(this.metaInfo); 
          this.out = this.c.upload(vrl.getPath(), this.current_loc, !this.random_access, binary_mode);
          if (this.out == null)
            throw new Exception("550 Upload denied by VFS client:" + this.the_file_path + this.the_file_name); 
        } catch (IOException e) {
          Log.log("SERVER", 1, e);
          this.c.close();
          if (e.indexOf("File in use:") >= 0) {
            Enumeration keys = this.thisSession.uVFS.clientCacheUsed.keys();
            while (keys.hasMoreElements()) {
              String key = keys.nextElement().toString();
              Vector v = (Vector)this.thisSession.uVFS.clientCacheUsed.get(key);
              if (v != null)
                for (int x = v.size() - 1; x >= 0; x--) {
                  GenericClient ctmp = v.elementAt(x);
                  if (ctmp != this.c && ctmp instanceof com.crushftp.client.FileClient) {
                    ctmp.close();
                    this.thisSession.uVFS.releaseClient(ctmp);
                  } 
                }  
            } 
            Thread.sleep(1000L);
            continue;
          } 
          throw e;
        } 
        break;
      } 
    } catch (FileNotFoundException e) {
      Log.log("SERVER", 0, String.valueOf(Thread.currentThread().getName()) + ":" + e);
      Log.log("SERVER", 1, e);
      throw new FileNotFoundException(String.valueOf(this.the_file_path) + this.the_file_name);
    } 
  }
  
  public VRL doTempUploadRename(VRL vrl, boolean zero_byte_first) throws Exception {
    if (this.allowTempExtensions && !SG("temp_upload_ext").equals("") && !SG("temp_upload_ext").equals("temp_upload_ext") && (!vrl.getName().endsWith(SG("temp_upload_ext")) || SG("temp_upload_ext").startsWith("!!")) && !Common.dmz_mode) {
      if (vrl.toString().toLowerCase().startsWith("s3"))
        return vrl; 
      if (vrl.toString().toLowerCase().endsWith(".zipstream"))
        return vrl; 
      String u = vrl.toString();
      if (u.endsWith("/"))
        u = u.substring(0, u.length() - 1); 
      if (SG("temp_upload_ext").startsWith("!!")) {
        VRL vrl2 = new VRL(String.valueOf(Common.all_but_last(u)) + SG("temp_upload_ext").substring(2));
        try {
          this.c.makedirs(vrl2.getPath());
          vrl2 = new VRL(String.valueOf(Common.all_but_last(u)) + SG("temp_upload_ext").substring(2) + "/" + Common.last(u));
          this.c.rename(vrl2.getPath(), vrl.getPath());
          if (zero_byte_first)
            this.c.upload_0_byte(vrl.getPath()); 
          this.c.rename(vrl.getPath(), vrl2.getPath());
          VRL vrl_temp = new VRL(String.valueOf(Common.all_but_last(u)) + SG("temp_upload_ext").substring(2));
          this.c.delete(vrl_temp.getPath());
        } catch (Exception e) {
          Log.log("UPLOAD", 1, e);
        } 
        vrl = vrl2;
      } else {
        VRL vrl2 = new VRL(String.valueOf(u) + SG("temp_upload_ext"));
        try {
          if (zero_byte_first)
            this.c.upload_0_byte(vrl.getPath()); 
          if (this.c.stat(vrl2.getPath()) != null)
            this.c.delete(vrl2.getPath()); 
          this.c.rename(vrl.getPath(), vrl2.getPath());
        } catch (Exception e) {
          Log.log("UPLOAD", 1, e);
        } 
        vrl = vrl2;
      } 
    } 
    return vrl;
  }
  
  public VRL doTempUploadRenameDone(VRL vrl) throws Exception {
    if (this.allowTempExtensions && !SG("temp_upload_ext").equals("") && !SG("temp_upload_ext").equals("temp_upload_ext") && (vrl.getName().endsWith(SG("temp_upload_ext")) || SG("temp_upload_ext").startsWith("!!")) && !this.quota_exceeded) {
      if (vrl.toString().toLowerCase().startsWith("s3"))
        return vrl; 
      if (vrl.toString().toLowerCase().endsWith(".zipstream"))
        return vrl; 
      String u = vrl.toString();
      if (u.endsWith("/"))
        u = u.substring(0, u.length() - 1); 
      if (SG("temp_upload_ext").startsWith("!!")) {
        VRL vrl2 = new VRL(String.valueOf(Common.all_but_last(Common.all_but_last(u))) + Common.last(u));
        this.c.rename(vrl.getPath(), vrl2.getPath());
        vrl = vrl2;
      } else {
        VRL vrl2 = new VRL(u.substring(0, vrl.toString().length() - SG("temp_upload_ext").length()));
        this.c.rename(vrl.getPath(), vrl2.getPath());
        vrl = vrl2;
      } 
    } 
    return vrl;
  }
  
  public void writeEncryptedHeaderSize(GenericClient c, VRL vrl, long loc) throws Exception {
    if (!Common.dmz_mode)
      if (ServerStatus.BG("fileEncryption") && !ServerStatus.SG("filePublicEncryptionKey").equals("") && ServerStatus.BG("file_encrypt_ascii")) {
        String keyLocation = "";
        if (!keyLocation.replace('\\', '/').endsWith("/")) {
          c.doCommand("SITE PGP_HEADER_SIZE " + loc + " " + vrl.getPath());
        } else {
          this.out = c.upload(vrl.getPath(), "CRUSHFTP_ENCRYPTED_kHBeMxiWj7Sb4PdqJ8".length(), false, true);
          String realSize = String.valueOf(loc) + "0                                        ".substring(1);
          realSize = realSize.substring(0, "0                                        ".length());
          this.out.write(realSize.getBytes("UTF8"));
          this.out.close();
          this.out = null;
        } 
      }  
  }
  
  public void setDefaultsPOSIX(Properties item, VRL vrl) throws Exception {
    Log.log("SERVER", 2, "POSIX1:" + vrl.safe());
    if (!vrl.getProtocol().equalsIgnoreCase("file"))
      return; 
    if (this.thisSession.uVFS != null)
      try {
        Properties parentItem = null;
        try {
          parentItem = this.thisSession.uVFS.get_fake_item(this.the_file_path, "FILE");
        } catch (Exception e) {
          Log.log("SERVER", 2, e);
        } 
        Log.log("SERVER", 2, "POSIX2:" + item.getProperty("type") + ":" + vrl.safe());
        if (item.getProperty("type").equalsIgnoreCase("DIR")) {
          this.thisSession.setFolderPrivs(this.c, item);
        } else {
          Properties vfs_posix_settings = Common.get_vfs_posix_settings(item.getProperty("privs", ""), true);
          if (!vfs_posix_settings.getProperty("vfs_owner", "").equals("")) {
            Log.log("SERVER", 2, "POSIX5: VFS permmission setting for owner: " + vfs_posix_settings.getProperty("vfs_owner", "") + ":" + vrl.safe());
            this.c.setOwner(vrl.getPath(), ServerStatus.change_vars_to_values_static(vfs_posix_settings.getProperty("vfs_owner", ""), this.thisSession.user, this.thisSession.user_info, this.thisSession), "");
          } else if (!SG("default_owner_command").equals("")) {
            Log.log("SERVER", 2, "POSIX3:" + SG("default_owner_command") + ":" + vrl.safe());
            this.c.setOwner(vrl.getPath(), ServerStatus.change_vars_to_values_static(SG("default_owner_command"), this.thisSession.user, this.thisSession.user_info, this.thisSession), "");
          } else if (parentItem.getProperty("protocol", "").equalsIgnoreCase("file") && !parentItem.getProperty("owner", "").trim().equals("") && !parentItem.getProperty("owner", "").trim().equals("user")) {
            try {
              this.c.setOwner(vrl.getPath(), parentItem.getProperty("owner", "").trim(), "");
            } catch (Exception e) {
              Log.log("SERVER", 2, e);
            } 
          } 
          if (!vfs_posix_settings.getProperty("vfs_group", "").equals("")) {
            Log.log("SERVER", 2, "POSIX5: VFS permmission setting for group: " + vfs_posix_settings.getProperty("vfs_group", "") + ":" + vrl.safe());
            this.c.setGroup(vrl.getPath(), ServerStatus.change_vars_to_values_static(vfs_posix_settings.getProperty("vfs_group", ""), this.thisSession.user, this.thisSession.user_info, this.thisSession), "");
          } else if (!SG("default_group_command").equals("")) {
            Log.log("SERVER", 2, "POSIX4:" + SG("default_group_command") + ":" + vrl.safe());
            this.c.setGroup(vrl.getPath(), ServerStatus.change_vars_to_values_static(SG("default_group_command"), this.thisSession.user, this.thisSession.user_info, this.thisSession), "");
          } else if (parentItem.getProperty("protocol", "").equalsIgnoreCase("file") && !parentItem.getProperty("group", "").trim().equals("") && !parentItem.getProperty("group", "").trim().equals("group")) {
            try {
              this.c.setGroup(vrl.getPath(), parentItem.getProperty("group", "").trim(), "");
            } catch (Exception e) {
              Log.log("SERVER", 2, e);
            } 
          } 
          if (!vfs_posix_settings.getProperty("vfs_privs", "").equals("")) {
            Log.log("SERVER", 2, "POSIX5: VFS permmission setting for privs: " + vfs_posix_settings.getProperty("vfs_privs", "") + ":" + vrl.safe());
            this.c.setMod(vrl.getPath(), vfs_posix_settings.getProperty("vfs_privs", ""), "");
          } else if (!SG("default_privs_command").equals("") && item.getProperty("type").equalsIgnoreCase("FILE")) {
            Log.log("SERVER", 2, "POSIX5:" + SG("default_privs_command") + ":" + vrl.safe());
            this.c.setMod(vrl.getPath(), SG("default_privs_command"), "");
          } else if (parentItem.getProperty("protocol", "").equalsIgnoreCase("file")) {
            try {
              if (!parentItem.getProperty("permissionsNum", "").equals(""))
                this.c.setMod(vrl.getPath(), parentItem.getProperty("permissionsNum", "").trim(), ""); 
            } catch (Exception e) {
              Log.log("SERVER", 2, e);
            } 
          } 
        } 
      } catch (Exception e) {
        Log.log("UPLOAD", 2, e);
      }  
  }
  
  public int reloadBandwidthLimits(boolean logSpeed) {
    int lesserSpeed = this.thisSession.IG("speed_limit_upload");
    if ((lesserSpeed > 0 && lesserSpeed > ServerStatus.IG("max_server_upload_speed") && ServerStatus.IG("max_server_upload_speed") > 0) || lesserSpeed == 0)
      lesserSpeed = ServerStatus.IG("max_server_upload_speed"); 
    if (lesserSpeed != 0 && logSpeed)
      this.thisSession.add_log("[" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: Bandwidth is being limited:" + lesserSpeed, "STOR"); 
    if (lesserSpeed > 0 && (this.thisSession.server_item.getProperty("port", "0").startsWith("55580") || this.thisSession.server_item.getProperty("port", "0").startsWith("55521")))
      lesserSpeed = 0; 
    return lesserSpeed;
  }
  
  public String SG(String data) {
    return this.thisSession.SG(data);
  }
  
  public void checkZipstream(Properties item) throws IOException {
    this.zipstream = false;
    this.zipchunkstream = false;
    if (ServerStatus.BG("allow_zipstream") && !Common.dmz_mode && this.the_file_name.endsWith(".zipstream") && item.getProperty("privs").indexOf("(write)") >= 0) {
      this.data_is = (InputStream)new FileArchiveInputStream(new BufferedInputStream(this.data_is));
      this.zipstream = true;
    } else if (ServerStatus.BG("allow_zipstream") && !Common.dmz_mode && this.the_file_name.endsWith(".zipchunkstream") && item.getProperty("privs").indexOf("(write)") >= 0) {
      this.data_is = (InputStream)new FileArchiveInputStream(new BufferedInputStream(this.data_is));
      this.zipstream = true;
      this.zipchunkstream = true;
    } 
  }
  
  public void kill() {
    this.die_now = true;
    try {
      this.data_is.close();
    } catch (Exception exception) {}
    try {
      this.data_sock.close();
    } catch (Exception exception) {}
    if (this.data_sock != null)
      this.thisSession.old_data_socks.remove(this.data_sock); 
    try {
      if (this.out != null)
        this.out.close(); 
    } catch (Exception exception) {}
    try {
      if (this.c != null)
        this.c.close(); 
    } catch (Exception exception) {}
    try {
      this.c = this.thisSession.uVFS.releaseClient(this.c);
    } catch (Exception exception) {}
    try {
      this.proxy.close();
    } catch (Exception exception) {}
    this.active2.put("active", "false");
  }
  
  public static String finishedUpload(GenericClient c, SessionCrush thisSession, Properties item, String the_file_path, String the_file_name, String stop_message, boolean quota_exceeded, long quota, boolean httpUpload, VRL vrl, long asciiLineCount, boolean binary_mode, long start_transfer_time, String md5Str, Properties fileItem, long resume_loc, boolean writeMessages, String realAction, Properties metaInfo, String user_agent, long current_loc) throws Exception {
    if (fileItem == null)
      fileItem = new Properties(); 
    thisSession.uiPUT("md5", md5Str);
    thisSession.uiPUT("sfv", md5Str);
    String message_string = "";
    if (!binary_mode && ServerStatus.BG("log_transfer_speeds"))
      message_string = String.valueOf(message_string) + asciiLineCount + " lines received."; 
    if (quota_exceeded) {
      if (thisSession.uiLG("overall_transfer_speed") == 0L)
        thisSession.uiPUT("overall_transfer_speed", (new StringBuffer(String.valueOf((thisSession.uiLG("bytes_received") - thisSession.uiLG("start_transfer_byte_amount")) / 1024L))).toString()); 
      fileItem.put("url", item.getProperty("url"));
      fileItem.put("the_file_path", String.valueOf(the_file_path) + the_file_name);
      fileItem.put("the_file_name", the_file_name);
      fileItem.put("name", the_file_name);
      fileItem.put("the_file_size", (new StringBuffer(String.valueOf(current_loc))).toString());
      fileItem.put("size", fileItem.getProperty("the_file_size"));
      fileItem.put("the_file_speed", (new StringBuffer(String.valueOf(thisSession.uiLG("overall_transfer_speed")))).toString());
      fileItem.put("the_file_start", (new StringBuffer(String.valueOf(start_transfer_time))).toString());
      fileItem.put("the_file_end", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
      fileItem.put("the_file_error", LOC.G("QUOTA EXCEEDED"));
      fileItem.put("the_file_status", "FAILED");
      fileItem.put("the_file_resume_loc", (new StringBuffer(String.valueOf(resume_loc))).toString());
      fileItem.put("the_file_md5", md5Str);
      fileItem.put("the_file_type", "FILE");
      if (metaInfo != null)
        fileItem.put("metaInfo", metaInfo); 
      thisSession.uiPUT("session_uploads", String.valueOf(thisSession.uiSG("session_uploads")) + thisSession.stripRoot(the_file_path) + the_file_name + ":" + thisSession.uiLG("bytes_received") + LOC.G("bytes") + " @ " + thisSession.uiLG("overall_transfer_speed") + LOC.G("k/sec.") + " " + LOC.G("QUOTA EXCEEDED") + CRLF);
      stop_message = String.valueOf(LOC.G("Your quota has been exceeded")) + ".  " + LOC.G("Available") + ": 0k.";
      thisSession.not_done = thisSession.ftp_write_command_logged("550-" + stop_message, "STOR");
      if (writeMessages && !ServerStatus.BG("delete_partial_uploads"))
        thisSession.not_done = thisSession.ftp_write_command_logged("550", "%STOR-quota exceeded% (\"" + thisSession.stripRoot(the_file_path) + the_file_name + "\") STOR", "STOR"); 
      if (the_file_name.indexOf(".DS_Store") < 0 && thisSession.uiLG("bytes_received") > 0L) {
        Properties lastUploadStat = ServerStatus.thisObj.statTools.add_item_stat(thisSession, fileItem, "UPLOAD");
        if (metaInfo != null)
          lastUploadStat.put("metaInfo", metaInfo); 
        thisSession.uiPUT("lastUploadStat", lastUploadStat);
        Vector lastUploadStats = thisSession.uiVG("lastUploadStats");
        lastUploadStats.addElement(thisSession.uiPG("lastUploadStat"));
        fileItem.put("uploadStats", thisSession.uiPG("lastUploadStat"));
        while (lastUploadStats.size() > ServerStatus.IG("user_log_buffer"))
          lastUploadStats.removeElementAt(0); 
      } 
      thisSession.do_event5("UPLOAD", fileItem);
      ServerStatus.thisObj.runAlerts("user_upload_session", thisSession);
      if (ServerStatus.BG("delete_partial_uploads")) {
        stop_message = LOC.G("Your quota would have been exceeded, so the upload was cancelled.");
        throw new Exception(stop_message);
      } 
    } else {
      if (thisSession.uiLG("overall_transfer_speed") == 0L)
        thisSession.uiPUT("overall_transfer_speed", (new StringBuffer(String.valueOf((thisSession.uiLG("bytes_received") - thisSession.uiLG("start_transfer_byte_amount")) / 1024L))).toString()); 
      ServerStatus.thisObj.server_info.put("uploaded_files", ServerStatus.siIG("uploaded_files") + 1);
      if (the_file_name.equals("CrushSyncMDTMMassUpdate.txt")) {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File_S(vrl.getCanonicalPath()))));
        String data = "";
        SimpleDateFormat sdf_yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        while ((data = br.readLine()) != null) {
          String modified = data.substring(0, data.indexOf(" "));
          String path = data.substring(data.indexOf(" ") + 1);
          if (!path.toUpperCase().startsWith(thisSession.SG("root_dir").toUpperCase()))
            path = String.valueOf(thisSession.SG("root_dir")) + path.substring(1); 
          boolean ok = false;
          ok = thisSession.check_access_privs(path, "STOR", item);
          if (ok) {
            VRL vrl2 = new VRL(thisSession.uVFS.get_item(path).getProperty("url"));
            boolean disable_mdtm_modifications = ServerStatus.BG("disable_mdtm_modifications");
            if (!thisSession.SG("disable_mdtm_modifications").equals("") && ServerStatus.BG("v9_beta"))
              disable_mdtm_modifications = thisSession.BG("disable_mdtm_modifications"); 
            if (!disable_mdtm_modifications)
              c.mdtm(vrl2.getPath(), sdf_yyyyMMddHHmmss.parse(modified).getTime()); 
          } 
        } 
        br.close();
        c.delete(vrl.getPath());
      } 
      fileItem.put("the_command", realAction);
      String the_correct_path = String.valueOf(the_file_path) + the_file_name;
      if (!the_correct_path.toUpperCase().startsWith(thisSession.SG("root_dir").toUpperCase()))
        the_correct_path = String.valueOf(thisSession.SG("root_dir")) + the_correct_path.substring(1); 
      fileItem.put("the_command_data", the_correct_path);
      fileItem.put("url", item.getProperty("url"));
      fileItem.put("the_file_path", String.valueOf(the_file_path) + the_file_name);
      fileItem.put("the_file_name", the_file_name);
      fileItem.put("name", the_file_name);
      fileItem.put("the_file_size", (new StringBuffer(String.valueOf(current_loc))).toString());
      fileItem.put("size", fileItem.getProperty("the_file_size"));
      fileItem.put("the_file_speed", (new StringBuffer(String.valueOf(thisSession.uiLG("overall_transfer_speed")))).toString());
      fileItem.put("the_file_start", (new StringBuffer(String.valueOf(start_transfer_time))).toString());
      fileItem.put("the_file_end", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
      fileItem.put("the_file_error", fileItem.getProperty("the_file_error", ""));
      fileItem.put("the_file_status", "SUCCESS");
      fileItem.put("the_file_resume_loc", (new StringBuffer(String.valueOf(resume_loc))).toString());
      fileItem.put("the_file_md5", md5Str);
      fileItem.put("the_file_type", "FILE");
      fileItem.put("user_agent", (user_agent == null) ? "" : user_agent);
      fileItem.put("modified", item.getProperty("modified", "0"));
      if (metaInfo != null)
        fileItem.put("metaInfo", metaInfo); 
      Log.log("UPLOAD", 2, fileItem.toString());
      thisSession.uiPUT("session_uploads", String.valueOf(thisSession.uiSG("session_uploads")) + thisSession.stripRoot(the_file_path) + the_file_name + ":" + thisSession.uiLG("bytes_received") + LOC.G("bytes") + " @ " + thisSession.uiLG("overall_transfer_speed") + LOC.G("k/sec.") + CRLF);
      thisSession.uiPUT("session_upload_count", (new StringBuffer(String.valueOf(thisSession.uiIG("session_upload_count") + 1))).toString());
      item.put("type", "FILE");
      SearchHandler.buildEntry(item, thisSession.uVFS, false, false);
      String responseNumber = "226";
      Properties p = new Properties();
      p.put("responseNumber", responseNumber);
      p.put("message_string", message_string);
      thisSession.runPlugin("after_upload", p);
      responseNumber = p.getProperty("responseNumber", responseNumber);
      message_string = p.getProperty("message_string", message_string);
      try {
        if (quota != -12345L)
          ServerStatus.thisObj.runAlerts("user_upload_session", thisSession); 
        if (quota != -12345L && !ServerStatus.BG("hide_ftp_quota_log"))
          thisSession.not_done = thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-" + LOC.G("Quota space available") + ": " + Common.format_bytes_short2(quota), "STOR"); 
        if (thisSession.IG("ratio") != 0)
          thisSession.not_done = thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-" + LOC.G("Ratio is $0 to 1.", (new StringBuffer(String.valueOf(thisSession.IG("ratio")))).toString()) + " " + LOC.G("Received") + ":" + ((thisSession.uiLG("bytes_received") + thisSession.uiLG("ratio_bytes_received")) / 1024L) + "k " + LOC.G("Sent") + ":" + ((thisSession.uiLG("bytes_sent") + thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.  " + LOC.G("Available") + ":" + (((thisSession.uiLG("bytes_received") + thisSession.uiLG("ratio_bytes_received")) * thisSession.IG("ratio") - thisSession.uiLG("bytes_sent") + thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.", "STOR"); 
        if (!message_string.equals("") && ServerStatus.BG("log_transfer_speeds")) {
          thisSession.not_done = thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-", "STOR");
          thisSession.not_done = thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-" + message_string, "STOR");
          thisSession.not_done = thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-", "STOR");
        } 
        if (!responseNumber.equals("226"))
          stop_message = message_string; 
        if (ServerStatus.BG("log_transfer_speeds"))
          thisSession.not_done = thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-" + LOC.G("Upload File Size:$0 bytes @ $1K/sec.", fileItem.getProperty("the_file_size"), fileItem.getProperty("the_file_speed")) + " MD5=" + md5Str, "STOR"); 
      } catch (IOException e) {
        Log.log("UPLOAD", 1, e);
      } 
      if (writeMessages && responseNumber.equals("226")) {
        if (ServerStatus.BG("generic_ftp_responses")) {
          thisSession.not_done = thisSession.ftp_write_command_logged("226", "Transfer complete.", "STOR");
        } else {
          thisSession.not_done = thisSession.ftp_write_command_logged((new StringBuffer(String.valueOf(responseNumber))).toString(), "%STOR-end% " + LOC.G("(\"$0$1\" $2) " + realAction, thisSession.stripRoot(the_file_path), the_file_name, vrl.getProtocol().equalsIgnoreCase("file") ? (new StringBuffer(String.valueOf((new File_U(vrl.getCanonicalPath())).length()))).toString() : ""), "STOR");
        } 
      } else if (writeMessages) {
        thisSession.not_done = thisSession.ftp_write_command_logged((new StringBuffer(String.valueOf(responseNumber))).toString(), String.valueOf(stop_message) + " " + LOC.G("(\"$0$1\" $2) " + realAction, thisSession.stripRoot(the_file_path), the_file_name, vrl.getProtocol().equalsIgnoreCase("file") ? (new StringBuffer(String.valueOf((new File_U(vrl.getCanonicalPath())).length()))).toString() : ""), "STOR");
      } 
      if (the_file_name.indexOf(".DS_Store") < 0 && (thisSession.uiLG("bytes_received") > 0L || ((httpUpload || ServerStatus.BG("event_empty_files")) && thisSession.uiLG("bytes_received") == 0L))) {
        Properties lastUploadStat = ServerStatus.thisObj.statTools.add_item_stat(thisSession, fileItem, "UPLOAD");
        ServerStatus.thisObj.statTools.insertMetaInfo(thisSession.uiSG("SESSION_RID"), metaInfo, lastUploadStat.getProperty("TRANSFER_RID"));
        if (metaInfo != null) {
          lastUploadStat.put("metaInfo", metaInfo);
          String keywords = metaInfo.getProperty("META_KEYWORDS", "");
          if (keywords.equals(""))
            keywords = metaInfo.getProperty("meta_keywords", ""); 
          if (keywords.equals(""))
            keywords = metaInfo.getProperty("keywords", ""); 
          if (keywords.equals(""))
            keywords = metaInfo.getProperty("KEYWORDS", ""); 
          if (!keywords.equals(""))
            ServerSessionAJAX.processKeywords(thisSession, new String[] { the_correct_path }, keywords); 
        } 
        thisSession.uiPUT("lastUploadStat", lastUploadStat);
        Vector lastUploadStats = thisSession.uiVG("lastUploadStats");
        lastUploadStats.addElement(thisSession.uiPG("lastUploadStat"));
        fileItem.put("uploadStats", thisSession.uiPG("lastUploadStat"));
        while (lastUploadStats.size() > ServerStatus.IG("user_log_buffer"))
          lastUploadStats.removeElementAt(0); 
        thisSession.add_log("[" + thisSession.uiSG("user_number") + ":" + thisSession.uiSG("user_name") + ":" + thisSession.uiSG("user_ip") + "] WROTE: *Adding " + fileItem.getProperty("the_file_name") + " to event tracking.*", "STOR");
        thisSession.do_event5("UPLOAD", fileItem);
      } 
      if (responseNumber.equals("226"))
        stop_message = ""; 
    } 
    return stop_message;
  }
  
  public String getPriorMd5(Properties item2, Properties stat) throws Exception {
    if (item2 != null && item2.getProperty("privs", "").indexOf("(sync") >= 0 && stat != null && stat.getProperty("type", "").equalsIgnoreCase("FILE"))
      return Common.getMD5(this.c.download((new VRL(item2.getProperty("url"))).getPath(), 0L, -1L, true)); 
    return "";
  }
  
  public String getLastMd5() {
    try {
      for (int x = 0; x < 10000 && this.last_md5s.size() == 0; x++)
        Thread.sleep(1L); 
      return this.last_md5s.remove(0).toString();
    } catch (Exception e) {
      Log.log("SERVER", 1, e);
      return (String)e;
    } 
  }
  
  public Properties make_current_item(long start_transfer_time) {
    Properties current_item = new Properties();
    current_item.put("name", this.item.getProperty("name"));
    current_item.put("modified", this.item.getProperty("modified", "0"));
    if (Long.parseLong(this.thisSession.uiSG("file_length")) > 0L) {
      current_item.put("the_file_size", this.thisSession.uiSG("file_length"));
    } else {
      current_item.put("the_file_size", this.item.getProperty("size", "0"));
    } 
    current_item.put("size", current_item.getProperty("the_file_size"));
    current_item.put("the_file_speed", "0");
    current_item.put("current_loc", (new StringBuffer(String.valueOf(this.current_loc))).toString());
    current_item.put("user_name", this.thisSession.uiSG("user_name"));
    current_item.put("user_ip", this.thisSession.uiSG("user_ip"));
    current_item.put("user_protocol", this.thisSession.uiSG("user_protocol"));
    current_item.put("the_file_start", (new StringBuffer(String.valueOf(start_transfer_time))).toString());
    ServerStatus.siVG("incoming_transfers").addElement(current_item);
    return current_item;
  }
  
  public static void updateTransferStats(SessionCrush thisSession, int data_read, boolean httpUpload, byte[] temp_array, MessageDigest md5, Properties current_upload_item) {
    thisSession.active_transfer();
    if (data_read < 0)
      return; 
    if (System.getProperty("crushftp.stor_md5", "true").equals("true"))
      md5.update(temp_array, 0, data_read); 
    thisSession.uiPPUT("bytes_received", data_read);
    thisSession.uiPUT("bytes_received_formatted", Common.format_bytes_short2(Long.parseLong(thisSession.uiSG("bytes_received"))));
    ServerStatus.thisObj.total_server_bytes_received += data_read;
    if (!httpUpload)
      thisSession.uiPPUT("file_length", data_read); 
    if (current_upload_item != null) {
      current_upload_item.put("current_loc", (new StringBuffer(String.valueOf(Long.parseLong(current_upload_item.getProperty("current_loc", "0")) + data_read))).toString());
      current_upload_item.put("the_file_speed", (new StringBuffer(String.valueOf(thisSession.uiLG("current_transfer_speed")))).toString());
      current_upload_item.put("now", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
    } 
    if (thisSession.server_item.containsKey("bytes_received"))
      synchronized (thisSession.server_item) {
        thisSession.server_item.put("bytes_received", (new StringBuffer(String.valueOf(Long.parseLong(thisSession.server_item.getProperty("bytes_received", "0")) + data_read))).toString());
      }  
  }
  
  public static Properties do_unique_rename(Properties item, VRL vrl, GenericClient c, boolean unique, String the_file_name, Properties stat) throws Exception {
    Properties result = new Properties();
    try {
      int fileNameInt = 1;
      String itemName = item.getProperty("url");
      String itemExt = "";
      if (itemName.lastIndexOf(".") > 0 && (itemName.lastIndexOf(".") == itemName.length() - 4 || itemName.lastIndexOf(".") == itemName.length() - 5)) {
        itemExt = itemName.substring(itemName.lastIndexOf("."));
        itemName = itemName.substring(0, itemName.lastIndexOf("."));
      } 
      VRL vrl2 = vrl;
      Properties stat2 = stat;
      while (stat2 != null) {
        fileNameInt++;
        item.put("url", String.valueOf(itemName) + fileNameInt + itemExt);
        item.put("name", the_file_name);
        vrl2 = new VRL(item.getProperty("url"));
        stat2 = c.stat(vrl2.getPath());
      } 
      if (unique) {
        the_file_name = Common.last(String.valueOf(itemName) + fileNameInt + itemExt);
        vrl = vrl2;
        item.put("name", the_file_name);
      } else if (!stat.getProperty("size").equals("0")) {
        if (ServerStatus.BG("drop_folder_rename_new")) {
          the_file_name = vrl2.getName();
          vrl = vrl2;
          item.put("name", the_file_name);
        } else {
          c.rename(vrl.getPath(), vrl2.getPath());
        } 
      } 
    } catch (Exception e) {
      if (e.indexOf("Interrupted") >= 0)
        throw e; 
    } 
    result.put("the_file_name", the_file_name);
    result.put("name", the_file_name);
    result.put("vrl", vrl);
    return result;
  }
}
