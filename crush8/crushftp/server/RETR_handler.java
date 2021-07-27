package crushftp.server;

import com.crushftp.client.Common;
import com.crushftp.client.File_S;
import com.crushftp.client.GenericClient;
import com.crushftp.client.LineReader;
import com.crushftp.client.VRL;
import com.crushftp.client.Worker;
import com.crushftp.tunnel.FileArchiveEntry;
import com.crushftp.tunnel.FileArchiveOutputStream;
import crushftp.gui.LOC;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.SessionCrush;
import crushftp.handlers.TransferSpeedometer;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import javax.net.ssl.SSLSocket;
import org.apache.commons.compress.archivers.zip.Zip64Mode;

public class RETR_handler implements Runnable {
  public boolean die_now = false;
  
  public boolean pause_transfer = false;
  
  public float slow_transfer = 0.0F;
  
  public long startLoop = 0L;
  
  public long endLoop = 1000L;
  
  public Thread thisThread = null;
  
  public String the_dir;
  
  int packet_size = 32768;
  
  public OutputStream data_os = null;
  
  public GenericClient c = null;
  
  InputStream in = null;
  
  SessionCrush thisSession = null;
  
  static String CRLF = "\r\n";
  
  int user_down_count = 0;
  
  int new_user_down_count = 0;
  
  Properties item;
  
  boolean pasv_connect = false;
  
  String encode_on_fly = "";
  
  public Properties active2 = new Properties();
  
  public MessageDigest md5 = null;
  
  LIST_handler filetree_list = null;
  
  public long current_loc = 0L;
  
  public long max_loc = 0L;
  
  public ServerSocket s_sock = null;
  
  public Socket streamer = null;
  
  boolean resumed_file = false;
  
  public boolean zipping = false;
  
  public Vector activeZipThreads = new Vector();
  
  boolean zlibing = false;
  
  VRL otherFile = null;
  
  boolean runOnce = false;
  
  public boolean httpDownload = false;
  
  public Vector zipFiles = new Vector();
  
  public Vector zippedFiles = new Vector();
  
  private Vector zippedPaths = new Vector();
  
  SimpleDateFormat proxySDF = new SimpleDateFormat("MMddyyHHmmss");
  
  RandomAccessFile proxy = null;
  
  InputStream proxy_remote_in = null;
  
  Socket data_sock = null;
  
  public String stop_message = "";
  
  public boolean inError = false;
  
  String threadName = "";
  
  public RETR_handler() {
    this.active2.put("streamOpenStatus", "PENDING");
  }
  
  public void init_vars(String the_dir, long current_loc, long max_loc, SessionCrush thisSession, Properties item, boolean pasv_connect, String encode_on_fly, VRL otherFile, Socket data_sock) {
    this.data_sock = data_sock;
    this.the_dir = the_dir;
    this.thisSession = thisSession;
    this.current_loc = current_loc;
    this.max_loc = max_loc;
    this.item = item;
    this.pasv_connect = pasv_connect;
    this.encode_on_fly = encode_on_fly;
    this.otherFile = otherFile;
    try {
      if (this.md5 == null)
        this.md5 = MessageDigest.getInstance(ServerStatus.SG("hash_algorithm")); 
    } catch (Exception exception) {}
    this.md5.reset();
    thisSession.uiPUT("md5", "");
    thisSession.uiPUT("sfv", "");
    if (current_loc > 0L)
      this.resumed_file = true; 
    try {
      this.user_down_count = ServerStatus.count_users_down();
    } catch (Exception exception) {}
    this.threadName = String.valueOf(Thread.currentThread().getName()) + ":RETR";
  }
  
  public void setThreadName(String threadName) {
    this.threadName = threadName;
  }
  
  public void run() {
    this.thisThread = Thread.currentThread();
    this.thisThread.setName(this.threadName);
    try {
      this.stop_message = "";
      this.inError = false;
      String the_file_name = "";
      String the_file_path = "";
      if (this.thisSession != null) {
        Properties current_download_item = null;
        this.thisSession.uiPUT("sending_file", "true");
        this.active2.put("active", "true");
        this.active2.put("streamOpenStatus", "PENDING");
        this.proxy_remote_in = null;
        updateTransferStats(this.thisSession, -1, false, null, this.md5, current_download_item);
        the_file_path = this.the_dir;
        the_file_name = the_file_path.substring(the_file_path.lastIndexOf("/") + 1, the_file_path.length()).trim();
        the_file_path = the_file_path.substring(0, the_file_path.lastIndexOf("/") + 1);
        boolean free_ratio_item = false;
        if (this.item.getProperty("privs", "").indexOf("(ratio)") >= 0)
          free_ratio_item = true; 
        Properties stat = null;
        VRL vrl = new VRL(this.item.getProperty("url"));
        if (!vrl.getProtocol().equalsIgnoreCase("virtual"))
          this.c = this.thisSession.uVFS.getClient(this.item); 
        if (this.c != null || !this.zipping)
          stat = this.c.stat(vrl.getPath()); 
        if (!vrl.getProtocol().equalsIgnoreCase("file") && ServerStatus.BG("proxyKeepDownloads")) {
          (new File_S(String.valueOf(ServerStatus.SG("proxyDownloadRepository")) + this.thisSession.uiSG("user_name") + the_file_path)).mkdirs();
          this.proxy = new RandomAccessFile(new File_S(String.valueOf(ServerStatus.SG("proxyDownloadRepository")) + this.thisSession.uiSG("user_name") + the_file_path + this.proxySDF.format(new Date()) + "_" + the_file_name), "rw");
          if (this.proxy.length() > this.current_loc)
            this.proxy.setLength(this.current_loc); 
        } 
        the_file_name = Common.url_decode(the_file_name);
        this.thisSession.uiPUT("last_file_real_path", this.item.getProperty("url", ""));
        this.thisSession.uiPUT("last_file_name", Common.last(this.item.getProperty("url", "")));
        long start_transfer_time = (new Date()).getTime();
        long max_download_amount = this.thisSession.LG("max_download_amount");
        long max_download_amount_day = this.thisSession.LG("max_download_amount_day");
        long max_download_amount_month = this.thisSession.LG("max_download_amount_month");
        long max_download_count = this.thisSession.LG("max_download_count");
        long max_download_count_day = this.thisSession.LG("max_download_count_day");
        long max_download_count_month = this.thisSession.LG("max_download_count_month");
        long start_download_amount_day = 0L;
        long start_download_amount_month = 0L;
        long start_download_count_day = 0L;
        long start_download_count_month = 0L;
        if (max_download_amount_day > 0L)
          start_download_amount_day = ServerStatus.thisObj.statTools.getTransferAmountToday(this.thisSession.uiSG("user_ip"), this.thisSession.uiSG("user_name"), this.thisSession.uiPG("stat"), "downloads", this.thisSession); 
        if (max_download_amount_month > 0L)
          start_download_amount_month = ServerStatus.thisObj.statTools.getTransferAmountThisMonth(this.thisSession.uiSG("user_ip"), this.thisSession.uiSG("user_name"), this.thisSession.uiPG("stat"), "downloads", this.thisSession); 
        if (max_download_count_day > 0L)
          start_download_count_day = ServerStatus.thisObj.statTools.getTransferCountToday(this.thisSession.uiSG("user_ip"), this.thisSession.uiSG("user_name"), this.thisSession.uiPG("stat"), "downloads", this.thisSession); 
        if (max_download_count_month > 0L)
          start_download_count_month = ServerStatus.thisObj.statTools.getTransferCountThisMonth(this.thisSession.uiSG("user_ip"), this.thisSession.uiSG("user_name"), this.thisSession.uiPG("stat"), "downloads", this.thisSession); 
        if (ServerStatus.count_users_ip(this.thisSession, this.thisSession.uiSG("user_protocol")) > Common.check_protocol(this.thisSession.uiSG("user_protocol"), SG("allowed_protocols"))) {
          if (this.data_sock != null)
            this.data_sock.close(); 
          this.thisSession.do_kill();
        } else if (this.thisSession.IG("ratio") > 0 && !free_ratio_item && (this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) * this.thisSession.IG("ratio") <= this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) {
          this.stop_message = "550-" + LOC.G("WARNING!!! Ratio reached.") + "  " + LOC.G("Ratio is") + " " + this.thisSession.IG("ratio") + " to 1.  " + LOC.G("Received") + ":" + ((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) / 1024L) + "k  " + LOC.G("Sent") + ":" + ((this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.  " + LOC.G("Available") + ":" + (((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) * this.thisSession.IG("ratio") - this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.";
          this.thisSession.not_done = this.thisSession.ftp_write_command_logged(this.stop_message, "RETR");
          this.thisSession.not_done = this.thisSession.ftp_write_command_logged("550", "%RETR-ratio exceeded%", "RETR");
          this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
          this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
        } else if (max_download_amount > 0L && this.thisSession.uiLG("bytes_sent") > max_download_amount * 1024L) {
          this.stop_message = "550-" + LOC.G("WARNING!!! Maximum download amount reached.") + "  " + LOC.G("Sent") + ":" + (this.thisSession.uiLG("bytes_sent") / 1024L) + "k.  " + LOC.G("Max") + ":" + max_download_amount + "k.  " + LOC.G("Available") + ":" + (max_download_amount - this.thisSession.uiLG("bytes_sent") / 1024L) + "k.";
          this.thisSession.not_done = this.thisSession.ftp_write_command_logged(this.stop_message, "RETR");
          this.thisSession.not_done = this.thisSession.ftp_write_command_logged("550", "%RETR-max reached%", "RETR");
          this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
          this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
          ServerStatus.thisObj.runAlerts("user_download_session", this.thisSession);
        } else if (max_download_count > 0L && this.thisSession.uiLG("session_download_count") > max_download_count) {
          this.stop_message = "550-" + LOC.G("WARNING!!! Maximum download count reached.") + "  " + LOC.G("Sent") + ":" + this.thisSession.uiLG("session_download_count") + ".  " + LOC.G("Max") + ":" + max_download_count + ".  " + LOC.G("Available") + ":" + (max_download_count - this.thisSession.uiLG("session_download_count")) + ".";
          this.thisSession.not_done = this.thisSession.ftp_write_command_logged(this.stop_message, "RETR");
          this.thisSession.not_done = this.thisSession.ftp_write_command_logged("550", "%RETR-max reached%", "RETR");
          this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
          this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
          ServerStatus.thisObj.runAlerts("user_download_session", this.thisSession);
        } else if ((max_download_amount_day > 0L && start_download_amount_day > max_download_amount_day * 1024L) || (max_download_amount_month > 0L && start_download_amount_month > max_download_amount_month * 1024L)) {
          if (max_download_amount_day > 0L && start_download_amount_day > max_download_amount_day * 1024L) {
            this.stop_message = "550-" + LOC.G("WARNING!!! Maximum download amount today reached.") + "  " + LOC.G("Sent") + ":" + (start_download_amount_day / 1024L) + "k.  " + LOC.G("Max") + ":" + max_download_amount_day + "k.  ";
            this.thisSession.not_done = this.thisSession.ftp_write_command_logged(this.stop_message, "RETR");
            ServerStatus.thisObj.runAlerts("user_download_day", this.thisSession);
          } 
          if (max_download_amount_month > 0L && start_download_amount_month > max_download_amount_month * 1024L) {
            this.stop_message = "550-" + LOC.G("WARNING!!! Maximum download amount last 30 days reached.") + "  " + LOC.G("Sent") + ":" + (start_download_amount_month / 1024L) + "k.  " + LOC.G("Max") + ":" + max_download_amount_month + "k.  ";
            this.thisSession.not_done = this.thisSession.ftp_write_command_logged(this.stop_message, "RETR");
            ServerStatus.thisObj.runAlerts("user_download_month", this.thisSession);
          } 
          this.thisSession.not_done = this.thisSession.ftp_write_command_logged("550", "%RETR-max reached%", "RETR");
          this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
          this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
        } else if ((max_download_count_day > 0L && start_download_count_day > max_download_count_day) || (max_download_count_month > 0L && start_download_count_month > max_download_count_month)) {
          if (max_download_count_day > 0L && start_download_count_day > max_download_count_day) {
            this.stop_message = "550-" + LOC.G("WARNING!!! Maximum download count today reached.") + "  " + LOC.G("Sent") + ":" + start_download_count_day + ".  " + LOC.G("Max") + ":" + max_download_count_day + "k.  ";
            this.thisSession.not_done = this.thisSession.ftp_write_command_logged(this.stop_message, "RETR");
            ServerStatus.thisObj.runAlerts("user_download_day", this.thisSession);
          } 
          if (max_download_count_month > 0L && start_download_count_month > max_download_count_month) {
            this.stop_message = "550-" + LOC.G("WARNING!!! Maximum download count last 30 days reached.") + "  " + LOC.G("Sent") + ":" + start_download_count_month + ".  " + LOC.G("Max") + ":" + max_download_count_month + ".  ";
            this.thisSession.not_done = this.thisSession.ftp_write_command_logged(this.stop_message, "RETR");
            ServerStatus.thisObj.runAlerts("user_download_month", this.thisSession);
          } 
          this.thisSession.not_done = this.thisSession.ftp_write_command_logged("550", "%RETR-max reached%", "RETR");
          this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
          this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
        } else {
          if (stat != null)
            this.thisSession.uiPUT("file_length", stat.getProperty("size")); 
          if (this.thisSession.BG("partial_download") && this.thisSession.IG("ratio") > 0 && !free_ratio_item && this.thisSession.uiLG("file_length") - this.current_loc > (this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) * this.thisSession.IG("ratio") - this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) {
            String response_str = "%RETR-ratio will be exceeded%" + CRLF + LOC.G("Ratio is $0 to 1.", (new StringBuffer(String.valueOf(this.thisSession.IG("ratio")))).toString()) + "  " + LOC.G("Received") + ":" + ((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) / 1024L) + "k  " + LOC.G("Sent") + ":" + ((this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.  " + LOC.G("Available") + ":" + (((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) * this.thisSession.IG("ratio") - this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.  " + LOC.G("Attempting to download") + ":" + ((this.thisSession.uiLG("file_length") - this.current_loc) / 1024L) + "k." + CRLF;
            response_str = String.valueOf(response_str) + "%RETR-ratio will be exceeded abort%";
            this.stop_message = response_str;
            this.thisSession.not_done = this.thisSession.ftp_write_command_logged("550", response_str, "RETR");
            this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
            this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
          } else if (this.thisSession.BG("partial_download") && this.thisSession.LG("max_download_amount") > 0L && this.thisSession.uiLG("file_length") - this.current_loc > this.thisSession.LG("max_download_amount") * 1024L - this.thisSession.uiLG("bytes_sent")) {
            String response_str = "%RETR-max download will be exceeded%" + CRLF + LOC.G("Sent") + ":" + (this.thisSession.uiLG("bytes_sent") / 1024L) + "k.  " + LOC.G("Max") + ":" + this.thisSession.LG("max_download_amount") + "k.  " + LOC.G("Available") + ":" + (this.thisSession.LG("max_download_amount") - this.thisSession.uiLG("bytes_sent") / 1024L) + "k.  " + LOC.G("Attempting to download") + ":" + ((this.thisSession.uiLG("file_length") - this.current_loc) / 1024L) + "k." + CRLF;
            response_str = String.valueOf(response_str) + "%RETR-max download will be exceeded abort%";
            this.stop_message = response_str;
            this.thisSession.not_done = this.thisSession.ftp_write_command_logged("550", response_str, "RETR");
            this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
            this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
          } else {
            long resume_loc = this.current_loc;
            int loop_times = 0;
            while (this.data_sock == null && this.otherFile == null && this.thisSession.data_socks.size() == 0 && loop_times++ < 10000)
              Thread.sleep(1L); 
            if (this.data_sock == null && this.otherFile == null && this.thisSession.data_socks.size() == 0) {
              this.thisSession.not_done = this.thisSession.ftp_write_command_logged("550", "%PORT-fail_question%" + CRLF + "%PORT-no_data_connection%", "RETR");
              this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
              this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
            } else {
              try {
                boolean has_bytes = false;
                if (this.thisSession.data_socks.size() > 0 && this.data_sock == null)
                  this.data_sock = this.thisSession.data_socks.remove(0); 
                if (this.data_sock != null)
                  this.data_sock.setSoTimeout(((this.thisSession.IG("max_idle_time") <= 0) ? 5 : this.thisSession.IG("max_idle_time")) * 1000 * 60); 
                if (this.otherFile == null)
                  this.data_os = new BufferedOutputStream(this.data_sock.getOutputStream()); 
                this.active2.put("streamOpenStatus", "OPEN");
                Properties pp = new Properties();
                String message_string = "";
                String responseNumber = "150";
                pp.put("message_string", message_string);
                pp.put("responseNumber", responseNumber);
                this.thisSession.runPlugin("before_download", pp);
                message_string = pp.getProperty("message_string", message_string);
                responseNumber = pp.getProperty("responseNumber", responseNumber);
                if (!responseNumber.equals("150")) {
                  Thread.sleep(100L);
                  throw new Exception(message_string);
                } 
                if (!message_string.equals("")) {
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-", "RETR");
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-" + message_string, "RETR");
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-", "RETR");
                } 
                if (this.thisSession.LG("max_download_amount") != 0L)
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-" + LOC.G("Max Download") + ".  " + LOC.G("Sent") + ":" + (this.thisSession.uiLG("bytes_sent") / 1024L) + "k.  " + LOC.G("Max") + ":" + this.thisSession.LG("max_download_amount") + "k.  " + LOC.G("Available") + ":" + (this.thisSession.LG("max_download_amount") - this.thisSession.uiLG("bytes_sent") / 1024L) + "k.", "RETR"); 
                if (this.thisSession.LG("max_download_amount_month") != 0L)
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-" + LOC.G("Max Download Month") + ".  " + LOC.G("Sent") + ":" + (start_download_amount_month / 1024L) + "k.  " + LOC.G("Max") + ":" + this.thisSession.LG("max_download_amount_month") + "k.  " + LOC.G("Available") + ":" + (this.thisSession.LG("max_download_amount_month") - start_download_amount_month / 1024L) + "k.", "RETR"); 
                if (this.thisSession.LG("max_download_amount_day") != 0L)
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-" + LOC.G("Max Download Day") + ".  " + LOC.G("Sent") + ":" + (start_download_amount_day / 1024L) + "k.  " + LOC.G("Max") + ":" + this.thisSession.LG("max_download_amount_day") + "k.  " + LOC.G("Available") + ":" + (this.thisSession.LG("max_download_amount_day") - start_download_amount_day / 1024L) + "k.", "RETR"); 
                if (this.thisSession.LG("max_upload_amount") != 0L)
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-" + LOC.G("Max Upload") + ".  " + LOC.G("Sent") + ":" + (this.thisSession.uiLG("bytes_sent") / 1024L) + "k.  " + LOC.G("Max") + ":" + this.thisSession.LG("max_upload_amount") + "k.  " + LOC.G("Available") + ":" + (this.thisSession.LG("max_upload_amount") - this.thisSession.uiLG("bytes_sent") / 1024L) + "k.", "RETR"); 
                if (this.thisSession.IG("max_download_count") != 0)
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-" + LOC.G("Max Download") + ".  " + LOC.G("Sent") + ":" + this.thisSession.uiLG("session_download_count") + ".  " + LOC.G("Max") + ":" + this.thisSession.IG("max_download_count") + ".  " + LOC.G("Available") + ":" + (this.thisSession.IG("max_download_count") - this.thisSession.uiLG("session_download_count")) + ".", "RETR"); 
                if (this.thisSession.IG("max_upload_count") != 0)
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-" + LOC.G("Max Upload") + ".  " + LOC.G("Sent") + ":" + this.thisSession.uiLG("session_upload_count") + ".  " + LOC.G("Max") + ":" + this.thisSession.IG("max_upload_count") + ".  " + LOC.G("Available") + ":" + (this.thisSession.IG("max_upload_count") - this.thisSession.uiLG("session_upload_count")) + ".", "RETR"); 
                if (this.thisSession.IG("ratio") != 0)
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-" + LOC.G("Ratio is $0 to 1.", (new StringBuffer(String.valueOf(this.thisSession.IG("ratio")))).toString()) + " " + LOC.G("Received") + ":" + ((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) / 1024L) + "k " + LOC.G("Sent") + ":" + ((this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.  " + LOC.G("Available") + ":" + (((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) * this.thisSession.IG("ratio") - this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.", "RETR"); 
                if (this.c != null && this.c.getConfig("pgpDecryptDownload", "").equals("true"))
                  this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-pgpDecryptDownload:" + (new VRL(this.c.getConfig("pgpPrivateKeyDownloadPath", ""))).safe(), "RETR"); 
                if (this.c != null && this.c.getConfig("pgpEncryptDownload", "").equals("true"))
                  this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-pgpEncryptDownload:" + (new VRL(this.c.getConfig("pgpPublicKeyDownloadPath", ""))).safe(), "RETR"); 
                if (this.c != null && this.c.getConfig("pgpDecryptUpload", "").equals("true"))
                  this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-pgpDecryptUpload:" + (new VRL(this.c.getConfig("pgpPrivateKeyUploadPath", ""))).safe(), "RETR"); 
                if (this.c != null && this.c.getConfig("pgpEncryptUpload", "").equals("true"))
                  this.thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-pgpEncryptUpload:" + (new VRL(this.c.getConfig("pgpPublicKeyUploadPath", ""))).safe(), "RETR"); 
                this.thisSession.uiPUT("start_transfer_time", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
                this.thisSession.uiPUT("start_transfer_byte_amount", (new StringBuffer(String.valueOf(this.thisSession.uiLG("bytes_sent")))).toString());
                TransferSpeedometer speedController = new TransferSpeedometer(this.thisSession, this, null);
                Worker.startWorker(speedController, String.valueOf(this.thisSession.uiSG("user_name")) + ":(" + this.thisSession.uiSG("user_number") + ")-" + this.thisSession.uiSG("user_ip") + " (speedometer retr)");
                VRL Zin = null;
                byte[] temp_array = new byte[this.packet_size];
                long start_file_size = this.thisSession.uiLG("file_length");
                long file_changing_loc = 0L;
                int file_changing_loop_intervals = 0;
                boolean binary_mode = !this.thisSession.uiSG("file_transfer_mode").equals("ASCII");
                if (this.thisSession.uiBG("modez")) {
                  Deflater def = new Deflater();
                  def.setLevel(Integer.parseInt(this.thisSession.uiSG("zlibLevel")));
                  if (this.httpDownload) {
                    this.data_os = new DeflaterOutputStream(this.data_os, def);
                  } else {
                    this.data_os = new DeflaterOutputStream(this.data_os, def);
                  } 
                  this.zlibing = true;
                } 
                if (this.zipFiles.size() > 0 || this.zipping) {
                  this.data_os = (OutputStream)new FileArchiveOutputStream(new BufferedOutputStream(this.data_os), !this.thisSession.uiBG("no_zip_compression"));
                  ((FileArchiveOutputStream)this.data_os).setEncoding(this.thisSession.SG("char_encoding"));
                  if (ServerStatus.BG("zip64"))
                    ((FileArchiveOutputStream)this.data_os).setUseZip64(Zip64Mode.AsNeeded); 
                  if (ServerStatus.BG("zip64_always"))
                    ((FileArchiveOutputStream)this.data_os).setUseZip64(Zip64Mode.Always); 
                  if (this.thisSession.uiBG("zip64"))
                    ((FileArchiveOutputStream)this.data_os).setUseZip64(Zip64Mode.Always); 
                  if (ServerStatus.SG("zipCompressionLevel").equalsIgnoreCase("None")) {
                    ((FileArchiveOutputStream)this.data_os).setLevel(0);
                  } else if (ServerStatus.SG("zipCompressionLevel").equalsIgnoreCase("Fast")) {
                    ((FileArchiveOutputStream)this.data_os).setLevel(1);
                  } else {
                    ((FileArchiveOutputStream)this.data_os).setLevel(9);
                  } 
                  if (this.thisSession.uiBG("no_zip_compression"))
                    ((FileArchiveOutputStream)this.data_os).setLevel(0); 
                  this.zipping = true;
                } else {
                  if (!this.thisSession.uiSG("proxy_mode").equalsIgnoreCase("socket"))
                    if (the_file_name.startsWith(":filetree") && ServerStatus.BG("allow_filetree")) {
                      int scanDepth = 999;
                      if (!the_file_name.equals(":filetree"))
                        try {
                          scanDepth = Integer.parseInt(the_file_name.substring(":filetree".length()));
                        } catch (Exception exception) {} 
                      this.thisSession.add_log("Building file tree listing..." + scanDepth, "RETR");
                      if (this.filetree_list == null)
                        this.filetree_list = new LIST_handler(); 
                      this.filetree_list.init_vars(the_file_path, false, this.thisSession, "", true, false);
                      if (scanDepth < 0) {
                        scanDepth *= -1;
                        this.thisSession.uiPUT("modez", "true");
                      } 
                      this.filetree_list.scanDepth = scanDepth;
                      this.filetree_list.fullPaths = true;
                      this.filetree_list.justStreamListData = true;
                      Properties connectedSockets = Common.getConnectedSockets();
                      Socket sock1 = (Socket)connectedSockets.get("sock1");
                      Socket sock2 = (Socket)connectedSockets.get("sock2");
                      this.filetree_list.data_osw = sock1.getOutputStream();
                      this.thisSession.data_socks.addElement(sock1);
                      this.in = sock2.getInputStream();
                      Thread t = new Thread(this.filetree_list);
                      t.setName(String.valueOf(this.thisSession.uiSG("user_name")) + ":(" + this.thisSession.uiSG("user_number") + ")-" + this.thisSession.uiSG("user_ip") + " RETR_handler_filetree");
                      t.start();
                    } else {
                      this.in = this.c.download(vrl.getPath(), this.current_loc, -1L, binary_mode);
                      if (current_download_item != null)
                        ServerStatus.siVG("outgoing_transfers").remove(current_download_item); 
                      if (this.item != null)
                        current_download_item = make_current_item(start_transfer_time, this.item); 
                    }  
                  if (!Common.dmz_mode)
                    if (!this.thisSession.user.getProperty("fileEncryptionKey", "").equals("") || !this.thisSession.user.getProperty("fileDecryptionKey", "").equals("")) {
                      this.in = Common.getDecryptedStream(this.in, this.thisSession.user.getProperty("fileEncryptionKey", ""), this.thisSession.user.getProperty("fileDecryptionKey", ""), this.thisSession.user.getProperty("fileDecryptionKeyPass", ""));
                    } else if (!ServerStatus.SG("fileEncryptionKey").equals("") || ServerStatus.BG("fileDecryption")) {
                      this.in = Common.getDecryptedStream(this.in, ServerStatus.SG("fileEncryptionKey"), ServerStatus.SG("fileDecryptionKey"), ServerStatus.SG("fileDecryptionKeyPass"));
                    }  
                } 
                this.thisSession.not_done = this.thisSession.ftp_write_command_logged((new StringBuffer(String.valueOf(responseNumber))).toString(), "%RETR-start% " + LOC.G("(\"$0$1\") R E T R", the_file_path, the_file_name), "RETR");
                byte[] read_string = (byte[])null;
                int data_read = 0;
                int ratio = this.thisSession.IG("ratio");
                int maxPackSize = 1000000;
                if (Common.machine_is_mac())
                  maxPackSize = 200000; 
                this.startLoop = 0L;
                this.endLoop = 1000L;
                int lesserSpeed = reloadBandwidthLimits();
                long speedometerCheckInterval = (new Date()).getTime();
                if (this.thisSession.uiSG("proxy_mode").equalsIgnoreCase("socket") && SG("site").toUpperCase().indexOf("(SITE_PROXY)") >= 0) {
                  Socket sock = new Socket(this.thisSession.uiSG("proxy_ip_address"), this.thisSession.uiIG("proxy_remote_port"));
                  this.proxy_remote_in = sock.getInputStream();
                  BufferedOutputStream proxy_remote_out = new BufferedOutputStream(sock.getOutputStream());
                  InputStream proxy_in = this.data_sock.getInputStream();
                  Thread t = new Thread(new Runnable(this, proxy_in, proxy_remote_out, sock) {
                        final RETR_handler this$0;
                        
                        private final InputStream val$proxy_in;
                        
                        private final BufferedOutputStream val$proxy_remote_out;
                        
                        private final Socket val$sock;
                        
                        public void run() {
                          byte[] b = new byte[32768];
                          int bytesRead = 0;
                          try {
                            while (this.this$0.active2.getProperty("active", "").equals("true") && bytesRead >= 0) {
                              bytesRead = this.val$proxy_in.read(b);
                              Log.log("DOWNLOAD", 2, "proxy bytes:" + bytesRead);
                              if (bytesRead > 0)
                                this.val$proxy_remote_out.write(b, 0, bytesRead); 
                              this.val$proxy_remote_out.flush();
                            } 
                          } catch (Exception e) {
                            Log.log("DOWNLOAD", 1, e);
                          } 
                          try {
                            this.val$sock.close();
                            this.this$0.data_sock.close();
                          } catch (Exception e) {
                            Log.log("DOWNLOAD", 1, e);
                          } 
                          if (this.this$0.data_sock != null)
                            this.this$0.thisSession.old_data_socks.remove(this.this$0.data_sock); 
                        }
                      });
                  t.setName(String.valueOf(this.thisSession.uiSG("user_name")) + ":(" + this.thisSession.uiSG("user_number") + ")-" + this.thisSession.uiSG("user_ip") + " RETR_handler_proxy_connector");
                  t.start();
                } 
                if (lesserSpeed != 0)
                  this.thisSession.add_log("Bandwidth is being limited:" + lesserSpeed, "RETR"); 
                boolean pgp = checkPgp(this.thisSession, this.item);
                LineReader inASCII = null;
                boolean user_speed_notified = false;
                while (data_read >= 0) {
                  Date new_date = new Date();
                  if (new_date.getTime() - speedometerCheckInterval > 10000L) {
                    lesserSpeed = reloadBandwidthLimits();
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
                  float packPercent = 0.0F;
                  packPercent = (float)this.thisSession.uiLG("overall_transfer_speed") * 1024.0F / 10.0F / temp_array.length;
                  if ((packPercent > 1.5D || packPercent < 0.5D) && packPercent > 0.0F) {
                    int newSize = (int)this.thisSession.uiLG("overall_transfer_speed") * 1024 / 10;
                    if (newSize > 1000 && newSize < maxPackSize)
                      temp_array = new byte[newSize]; 
                  } 
                  if (this.max_loc > 0L && this.max_loc - this.current_loc < temp_array.length)
                    if ((int)(this.max_loc - this.current_loc) >= 0)
                      temp_array = new byte[(int)(this.max_loc - this.current_loc)];  
                  if (max_download_amount > 0L && this.thisSession.uiLG("bytes_sent") > max_download_amount * 1024L) {
                    data_read = -1;
                    this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-%RETR-max reached%", "RETR");
                    ServerStatus.thisObj.runAlerts("user_download_session", this.thisSession);
                  } else if (max_download_count > 0L && this.thisSession.uiLG("session_download_count") > max_download_count) {
                    data_read = -1;
                    this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-%RETR-max reached%", "RETR");
                    ServerStatus.thisObj.runAlerts("user_download_session", this.thisSession);
                  } else if (max_download_amount_day > 0L && this.thisSession.uiLG("bytes_sent") + start_download_amount_day > max_download_amount_day * 1024L) {
                    data_read = -1;
                    this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-" + LOC.G("WARNING!!! Maximum download amount today reached.") + "  " + LOC.G("Sent") + ":" + ((this.thisSession.uiLG("bytes_sent") + start_download_amount_day) / 1024L) + "k.  " + LOC.G("Max") + ":" + max_download_amount_day + "k.  ", "RETR");
                    this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-%RETR-max reached%", "RETR");
                    ServerStatus.thisObj.runAlerts("user_download_day", this.thisSession);
                  } else if (max_download_count_day > 0L && this.thisSession.uiLG("session_download_count") + start_download_count_day > max_download_count_day) {
                    data_read = -1;
                    this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-" + LOC.G("WARNING!!! Maximum download count today reached.") + "  " + LOC.G("Sent") + ":" + (this.thisSession.uiLG("session_download_count") + start_download_count_day) + ".  " + LOC.G("Max") + ":" + max_download_count_day + ".  ", "RETR");
                    this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-%RETR-max reached%", "RETR");
                    ServerStatus.thisObj.runAlerts("user_download_day", this.thisSession);
                  } else if (max_download_amount_month > 0L && this.thisSession.uiLG("bytes_sent") + start_download_amount_month > max_download_amount_month * 1024L) {
                    data_read = -1;
                    this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-" + LOC.G("WARNING!!! Maximum download amount last 30 days reached.") + "  " + LOC.G("Sent") + ":" + ((this.thisSession.uiLG("bytes_sent") + start_download_amount_month) / 1024L) + "k.  " + LOC.G("Max") + ":" + max_download_amount_month + "k.  ", "RETR");
                    this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-%RETR-max reached%", "RETR");
                    ServerStatus.thisObj.runAlerts("user_download_month", this.thisSession);
                  } else if (max_download_count_month > 0L && this.thisSession.uiLG("session_download_count") + start_download_count_month > max_download_count_month) {
                    data_read = -1;
                    this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-" + LOC.G("WARNING!!! Maximum download count last 30 days reached.") + "  " + LOC.G("Sent") + ":" + (this.thisSession.uiLG("session_download_count") + start_download_count_month) + ".  " + LOC.G("Max") + ":" + max_download_count_month + ".  ", "RETR");
                    this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-%RETR-max reached%", "RETR");
                    ServerStatus.thisObj.runAlerts("user_download_month", this.thisSession);
                  } else if (ratio == 0 || free_ratio_item || (this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) * ratio > this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) {
                    if (this.zipping) {
                      if (Zin == null) {
                        for (int xx = this.zipFiles.size() - 1; xx >= 0; xx--) {
                          Properties zipItem = this.zipFiles.elementAt(xx);
                          if (zipItem.getProperty("type", "").equalsIgnoreCase("DIR") && zipItem.getProperty("privs").toUpperCase().indexOf("(VIEW)") < 0) {
                            this.zipFiles.removeElementAt(xx);
                          } else if (!Common.filter_check("L", zipItem.getProperty("name"), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + this.thisSession.SG("file_filter"))) {
                            this.zipFiles.removeElementAt(xx);
                          } else if (!this.thisSession.check_access_privs(zipItem.getProperty("root_dir"), "RETR", zipItem) || !this.thisSession.check_access_privs(String.valueOf(zipItem.getProperty("root_dir")) + zipItem.getProperty("name"), "RETR", zipItem)) {
                            this.zipFiles.removeElementAt(xx);
                          } 
                        } 
                        if (this.zipFiles.size() > 0) {
                          if (current_download_item != null) {
                            if (current_download_item != null)
                              ServerStatus.siVG("outgoing_transfers").remove(current_download_item); 
                            current_download_item = null;
                          } 
                          Properties zipItem = this.zipFiles.elementAt(0);
                          this.zipFiles.removeElementAt(0);
                          if (zipItem.getProperty("type", "").equalsIgnoreCase("DIR") && zipItem.getProperty("privs").toUpperCase().indexOf("(VIEW)") < 0) {
                            zipItem = null;
                          } else if (!Common.filter_check("L", zipItem.getProperty("name"), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + this.thisSession.SG("file_filter"))) {
                            zipItem = null;
                          } else if (!this.thisSession.check_access_privs(zipItem.getProperty("root_dir"), "RETR", zipItem)) {
                            zipItem = null;
                          } else if (zipItem.getProperty("url").startsWith("virtual:")) {
                            VRL vrl2 = new VRL(zipItem.getProperty("url"));
                            Properties temp_zip_item = this.thisSession.uVFS.get_item(vrl2.getPath());
                            if (temp_zip_item != null)
                              zipItem.put("url", temp_zip_item.getProperty("url")); 
                          } 
                          this.zippedFiles.addElement(zipItem);
                          long rest = Long.parseLong(zipItem.getProperty("rest", "-1"));
                          while (zipItem.getProperty("privs").indexOf("(read)") < 0 || (zipItem.getProperty("privs").indexOf("(invisible)") >= 0 && zipItem.getProperty("privs").indexOf("(inherited)") < 0) || (zipItem.getProperty("type", "").equalsIgnoreCase("DIR") && zipItem.getProperty("privs").indexOf("(view)") < 0)) {
                            zipItem = this.zipFiles.elementAt(0);
                            this.zipFiles.removeElementAt(0);
                            if (this.zipFiles.size() == 0 && (zipItem.getProperty("privs").indexOf("(read)") < 0 || zipItem.getProperty("privs").indexOf("(invisible)") >= 0 || zipItem.getProperty("privs").indexOf("(view)") < 0)) {
                              zipItem = null;
                              break;
                            } 
                          } 
                          if (zipItem != null) {
                            Zin = new VRL(zipItem.getProperty("url"));
                            if (this.c != null) {
                              this.c.close();
                              this.thisSession.uVFS.releaseClient(this.c);
                            } 
                            this.c = this.thisSession.uVFS.getClient(zipItem);
                            zipItem.put("url", Zin.toString());
                            while (true) {
                              stat = this.c.stat(Zin.getPath());
                              if (!stat.getProperty("type").equals("DIR"))
                                break; 
                              FileArchiveEntry ze = null;
                              zipItem.put("zipPath", String.valueOf(zipItem.getProperty("root_dir", "").substring(the_file_path.length())) + zipItem.getProperty("name") + "/");
                              if (!this.zippedPaths.contains(zipItem.getProperty("zipPath"))) {
                                this.zippedPaths.add(zipItem.getProperty("zipPath"));
                                ze = new FileArchiveEntry(zipItem.getProperty("zipPath"));
                                ze.setTime(Long.parseLong(zipItem.getProperty("modified", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString())));
                                ((FileArchiveOutputStream)this.data_os).putArchiveEntry(ze);
                                ((FileArchiveOutputStream)this.data_os).closeArchiveEntry();
                              } 
                              if (this.zipFiles.size() == 0 && this.activeZipThreads.size() == 0) {
                                Thread.sleep(1000L);
                                if (this.zipFiles.size() > 0)
                                  continue; 
                                Zin = null;
                                break;
                              } 
                              if (this.zipFiles.size() > 0) {
                                Thread.sleep(100L);
                                zipItem = this.zipFiles.elementAt(0);
                                this.zipFiles.removeElementAt(0);
                                Zin = new VRL(zipItem.getProperty("url"));
                                stat = this.c.stat(Zin.getPath());
                                if (this.zipFiles.size() == 0 && stat.getProperty("type").equalsIgnoreCase("DIR")) {
                                  zipItem.put("zipPath", String.valueOf(zipItem.getProperty("root_dir", "").substring(the_file_path.length())) + zipItem.getProperty("name") + "/");
                                  if (!this.zippedPaths.contains(zipItem.getProperty("zipPath"))) {
                                    this.zippedPaths.add(zipItem.getProperty("zipPath"));
                                    ze = new FileArchiveEntry(zipItem.getProperty("zipPath"));
                                    ze.setTime(Long.parseLong(zipItem.getProperty("modified", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString())));
                                    ((FileArchiveOutputStream)this.data_os).putArchiveEntry(ze);
                                    ((FileArchiveOutputStream)this.data_os).closeArchiveEntry();
                                  } 
                                  Zin = null;
                                  break;
                                } 
                                continue;
                              } 
                              Thread.sleep(100L);
                            } 
                          } 
                          if (zipItem == null || Zin == null || this.c.stat(Zin.getPath()) == null) {
                            Zin = null;
                            data_read = 0;
                          } 
                          if (this.zipFiles.size() == 0 && Zin == null) {
                            data_read = -1;
                            Zin = null;
                          } else if (Zin != null) {
                            String extra = "";
                            if (rest != -1L)
                              extra = ":REST=" + rest + ";"; 
                            zipItem.put("zipPath", String.valueOf(zipItem.getProperty("root_dir", "").substring(the_file_path.length())) + zipItem.getProperty("name") + extra);
                            if (!this.zippedPaths.contains(zipItem.getProperty("zipPath"))) {
                              this.zippedPaths.add(zipItem.getProperty("zipPath"));
                              FileArchiveEntry ze = new FileArchiveEntry(zipItem.getProperty("zipPath"));
                              ze.setTime(Long.parseLong(zipItem.getProperty("modified", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString())));
                              try {
                                ((FileArchiveOutputStream)this.data_os).putArchiveEntry(ze);
                                this.in = this.c.download(Zin.getPath(), rest, -1L, binary_mode);
                                if (current_download_item != null)
                                  ServerStatus.siVG("outgoing_transfers").remove(current_download_item); 
                                if (zipItem != null)
                                  current_download_item = make_current_item(start_transfer_time, zipItem); 
                                if (!Common.dmz_mode)
                                  if (!this.thisSession.user.getProperty("fileEncryptionKey", "").equals("") || !this.thisSession.user.getProperty("fileDecryptionKey", "").equals("")) {
                                    this.in = Common.getDecryptedStream(this.in, this.thisSession.user.getProperty("fileEncryptionKey", ""), this.thisSession.user.getProperty("fileDecryptionKey", ""), this.thisSession.user.getProperty("fileDecryptionKeyPass", ""));
                                  } else if (!ServerStatus.SG("fileEncryptionKey").equals("") || ServerStatus.BG("fileDecryption")) {
                                    this.in = Common.getDecryptedStream(this.in, ServerStatus.SG("fileEncryptionKey"), ServerStatus.SG("fileDecryptionKey"), ServerStatus.SG("fileDecryptionKeyPass"));
                                  }  
                                data_read = 0;
                              } catch (IOException e) {
                                if (e.toString().toLowerCase().indexOf("duplicate") >= 0) {
                                  Zin = null;
                                  data_read = 0;
                                } else {
                                  throw e;
                                } 
                              } 
                            } 
                          } 
                        } else if (this.activeZipThreads.size() == 0) {
                          data_read = -1;
                          Zin = null;
                        } else {
                          Thread.sleep(100L);
                        } 
                      } else {
                        data_read = this.in.read(temp_array);
                        if (data_read < 0) {
                          this.in.close();
                          this.in = null;
                          Zin = null;
                          data_read = 0;
                          ((FileArchiveOutputStream)this.data_os).closeArchiveEntry();
                          this.data_os.flush();
                        } 
                      } 
                    } else if (!binary_mode) {
                      if (inASCII == null)
                        inASCII = new LineReader(this.in); 
                      read_string = inASCII.readLineCRLF();
                      if (read_string != null) {
                        data_read = read_string.length;
                        temp_array = read_string;
                      } else {
                        data_read = -1;
                      } 
                    } else if (this.proxy_remote_in != null) {
                      data_read = this.proxy_remote_in.read(temp_array);
                    } else {
                      data_read = this.in.read(temp_array);
                    } 
                    if (data_read <= 0 && !this.zipping)
                      if (the_file_path.indexOf("/WebInterface/") < 0 && !the_file_name.equals("CrushFTP.jar"))
                        if (this.otherFile == null || this.otherFile.getPath().indexOf("/WebInterface/") < 0)
                          if (vrl.getProtocol().equalsIgnoreCase("file")) {
                            stat = this.c.stat(vrl.getPath());
                            long cur_file_size = Long.parseLong(stat.getProperty("size"));
                            if (cur_file_size > start_file_size && !pgp) {
                              data_read = 0;
                              if (file_changing_loc == this.current_loc)
                                file_changing_loop_intervals++; 
                              file_changing_loc = this.current_loc;
                              if (file_changing_loop_intervals > 20)
                                data_read = -1; 
                              start_file_size = cur_file_size - 1L;
                              Thread.sleep(1000L);
                            } 
                          }    
                    if (data_read > 0) {
                      has_bytes = true;
                      if (this.zlibing) {
                        ((DeflaterOutputStream)this.data_os).write(temp_array, 0, data_read);
                        if (!vrl.getProtocol().equalsIgnoreCase("file") && this.proxy != null)
                          this.proxy.write(temp_array, 0, data_read); 
                      } else {
                        this.data_os.write(temp_array, 0, data_read);
                        if (this.proxy_remote_in != null)
                          this.data_os.flush(); 
                        if (!vrl.getProtocol().equalsIgnoreCase("file") && this.proxy != null)
                          this.proxy.write(temp_array, 0, data_read); 
                      } 
                      updateTransferStats(this.thisSession, data_read, free_ratio_item, temp_array, this.md5, current_download_item);
                      this.current_loc += ((data_read > 0) ? data_read : 0L);
                      file_changing_loop_intervals = 0;
                    } 
                    if (this.max_loc > 0L && this.max_loc - this.current_loc == 0L)
                      data_read = -1; 
                  } else {
                    data_read = -1;
                    this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-%RETR-ratio exceeded%", "RETR");
                  } 
                  if (this.thisSession.uiLG("overall_transfer_speed") < this.thisSession.IG("min_download_speed") && this.thisSession.IG("min_download_speed") > 0 && new_date.getTime() - this.thisSession.uiLG("start_transfer_time") > (ServerStatus.IG("minimum_speed_warn_seconds") * 1000))
                    throw new Exception(LOC.G("Transfer speed is less than minimum required after at least 10 seconds.") + "  " + this.thisSession.uiLG("overall_transfer_speed") + "K/sec < " + this.thisSession.IG("min_download_speed") + "K/sec."); 
                  if (this.thisSession.uiLG("overall_transfer_speed") < Math.abs(this.thisSession.IG("min_download_speed")) && this.thisSession.IG("min_download_speed") < 0 && new_date.getTime() - this.thisSession.uiLG("start_transfer_time") > (ServerStatus.IG("minimum_speed_alert_seconds") * 1000)) {
                    if (!user_speed_notified)
                      ServerStatus.thisObj.runAlerts("user_download_speed", this.thisSession); 
                    user_speed_notified = true;
                  } 
                  this.endLoop = (new Date()).getTime();
                } 
                if (current_download_item != null) {
                  if (current_download_item != null)
                    ServerStatus.siVG("outgoing_transfers").remove(current_download_item); 
                  current_download_item = null;
                } 
                if (this.filetree_list != null && this.filetree_list.stop_message.toUpperCase().indexOf("FAILED") >= 0)
                  throw new Exception("Filetree list failed!"); 
                try {
                  this.thisSession.uiPUT("sending_file", "false");
                } catch (Exception exception) {}
                if (this.zipping)
                  this.thisSession.uiPUT("file_length", (new StringBuffer(String.valueOf(this.thisSession.uiLG("bytes_sent") - this.thisSession.uiLG("start_transfer_byte_amount")))).toString()); 
                try {
                  if (this.in != null)
                    this.in.close(); 
                } catch (Exception exception) {}
                if (this.c != null)
                  this.c.close(); 
                try {
                  this.proxy.close();
                } catch (Exception exception) {}
                if (this.zipping)
                  ((FileArchiveOutputStream)this.data_os).finish(); 
                if (this.zlibing)
                  ((DeflaterOutputStream)this.data_os).finish(); 
                try {
                  this.data_os.flush();
                } catch (Exception exception) {}
                try {
                  if (!has_bytes && this.data_sock != null && this.data_sock instanceof SSLSocket) {
                    Common.debug(1, this.data_sock + ":" + "Forcing SSL handshake to start...");
                    Common.configureSSLTLSSocket(this.data_sock);
                    ((SSLSocket)this.data_sock).startHandshake();
                    Common.debug(1, this.data_sock + ":" + "Forced SSL handshake complete.");
                  } 
                  if ((this.otherFile == null || this.zipping || this.zlibing) && !this.httpDownload)
                    try {
                      this.data_os.close();
                    } catch (Exception exception) {} 
                  if (this.data_sock != null)
                    this.data_sock.close(); 
                } catch (Exception e) {
                  Log.log("SERVER", 1, e);
                } 
                if (this.data_sock != null)
                  this.thisSession.old_data_socks.remove(this.data_sock); 
                downloadFinishedSuccess(this.thisSession, the_file_path, the_file_name, this.item, this.thisSession.uiLG("start_resume_loc"), this.httpDownload, this.zippedFiles, start_transfer_time, this.md5, resume_loc, this.current_loc, binary_mode);
              } catch (Exception e) {
                this.stop_message = e.toString();
                this.inError = true;
                Log.log("DOWNLOAD", 1, e);
                if (current_download_item != null) {
                  if (current_download_item != null)
                    ServerStatus.siVG("outgoing_transfers").remove(current_download_item); 
                  current_download_item = null;
                } 
                try {
                  this.thisSession.uiPUT("overall_transfer_speed", "0");
                } catch (Exception exception) {}
                try {
                  this.thisSession.uiPUT("current_transfer_speed", "0");
                } catch (Exception exception) {}
                try {
                  this.thisSession.uiPUT("sending_file", "false");
                } catch (Exception exception) {}
                try {
                  this.c.close();
                } catch (Exception exception) {}
                this.c = this.thisSession.uVFS.releaseClient(this.c);
                try {
                  this.proxy.close();
                } catch (Exception exception) {}
                try {
                  this.data_os.flush();
                } catch (Exception exception) {}
                try {
                  this.data_os.close();
                } catch (Exception exception) {}
                try {
                  this.data_sock.close();
                } catch (Exception exception) {}
                if (this.data_sock != null)
                  this.thisSession.old_data_socks.remove(this.data_sock); 
                Properties fileItem = (Properties)this.item.clone();
                fileItem.put("url", this.item.getProperty("url", ""));
                fileItem.put("the_file_path", String.valueOf(the_file_path) + the_file_name);
                fileItem.put("the_file_name", the_file_name);
                fileItem.put("the_file_size", (new StringBuffer(String.valueOf(this.thisSession.uiLG("file_length")))).toString());
                fileItem.put("the_file_speed", (new StringBuffer(String.valueOf(this.thisSession.uiLG("overall_transfer_speed")))).toString());
                fileItem.put("the_file_error", this.stop_message);
                fileItem.put("the_file_type", this.item.getProperty("type", "FILE"));
                fileItem.put("the_file_status", "FAILED");
                this.thisSession.uiPUT("session_downloads", String.valueOf(this.thisSession.uiSG("session_downloads")) + the_file_path + the_file_name + ":" + this.thisSession.uiLG("file_length") + LOC.G("bytes") + " @ " + this.thisSession.uiLG("overall_transfer_speed") + "k/sec. " + LOC.G("FAILED") + CRLF);
                if (this.thisSession.LG("max_download_amount") != 0L)
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-" + SG("Max Download") + ".  " + LOC.G("Sent") + ":" + (this.thisSession.uiLG("bytes_sent") / 1024L) + "k.  " + SG("Max") + ":" + this.thisSession.LG("max_download_amount") + "k.  " + LOC.G("Available") + ":" + (this.thisSession.LG("max_download_amount") - this.thisSession.uiLG("bytes_sent") / 1024L) + "k.", "RETR"); 
                if (this.thisSession.LG("max_download_count") != 0L)
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-" + SG("Max Download") + ".  " + LOC.G("Sent") + ":" + this.thisSession.uiLG("session_download_count") + ".  " + SG("Max") + ":" + this.thisSession.LG("max_download_count") + ".  " + LOC.G("Available") + ":" + (this.thisSession.LG("max_download_count") - this.thisSession.uiLG("session_download_count")) + ".", "RETR"); 
                if (this.thisSession.IG("ratio") != 0)
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged("226-" + LOC.G("Ratio is $0 to 1.", (new StringBuffer(String.valueOf(this.thisSession.IG("ratio")))).toString()) + " " + LOC.G("Received") + ":" + ((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) / 1024L) + "k " + LOC.G("Sent") + ":" + ((this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.  " + LOC.G("Available") + ":" + (((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) * this.thisSession.IG("ratio") - this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.", "RETR"); 
                if (ServerStatus.BG("rfc_proxy") && e.getMessage() != null && e.getMessage().length() > 3 && e.getMessage().charAt(3) == ' ') {
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged(e.getMessage(), "RETR");
                } else {
                  this.thisSession.not_done = this.thisSession.ftp_write_command_logged("550", String.valueOf(e.getMessage()) + " " + LOC.G("(\"$0$1\") RETR", the_file_path, the_file_name), "RETR");
                  this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
                } 
                if (the_file_path.indexOf("/WebInterface/") < 0)
                  this.thisSession.do_event5("DOWNLOAD", fileItem); 
              } 
            } 
          } 
        } 
      } 
      while (this.thisSession.uiSG("session_downloads").length() / 160 > ServerStatus.IG("user_log_buffer") && this.thisSession.uiSG("session_downloads").length() > 160)
        this.thisSession.uiPUT("session_downloads", this.thisSession.uiSG("session_downloads").substring(160)); 
      this.zipFiles.removeAllElements();
      this.zippedFiles.removeAllElements();
      this.zippedPaths.removeAllElements();
      this.resumed_file = false;
      this.zipping = false;
      this.zlibing = false;
      try {
        this.thisSession.uiPUT("last_time_remaining", "<" + LOC.G("None Active") + ">");
      } catch (Exception exception) {}
      try {
        this.thisSession.uiPUT("last_action", "RETR-Done.");
      } catch (Exception exception) {}
      if (this.otherFile == null)
        try {
          this.data_os.close();
        } catch (Exception exception) {} 
      try {
        if (this.data_sock != null)
          this.data_sock.close(); 
      } catch (Exception exception) {}
      if (this.data_sock != null)
        this.thisSession.old_data_socks.remove(this.data_sock); 
      try {
        this.proxy_remote_in.close();
      } catch (Exception exception) {}
      try {
        this.thisSession.uiPUT("overall_transfer_speed", "0");
      } catch (Exception exception) {}
      try {
        this.thisSession.uiPUT("current_transfer_speed", "0");
      } catch (Exception exception) {}
      try {
        this.thisSession.uiPUT("sending_file", "false");
      } catch (Exception exception) {}
      this.thisSession.uiPUT("sending_file", "false");
      if (this.thisSession.ftp != null)
        this.thisSession.ftp.start_idle_timer(); 
      this.active2.put("streamOpenStatus", "CLOSED");
      try {
        this.c = this.thisSession.uVFS.releaseClient(this.c);
      } catch (Exception exception) {}
      if (this.runOnce)
        return; 
    } catch (Exception e) {
      Log.log("DOWNLOAD", 1, e);
    } finally {
      this.thisThread = null;
      this.active2.put("active", "false");
    } 
    kill();
    while (this.thisSession.retr_files_pool_used.indexOf(this) >= 0)
      this.thisSession.retr_files_pool_used.removeElement(this); 
    if (this.thisSession.retr_files_pool_free.indexOf(this) < 0)
      this.thisSession.retr_files_pool_free.addElement(this); 
  }
  
  public void kill() {
    this.die_now = true;
    try {
      this.data_os.close();
    } catch (Exception exception) {}
    try {
      this.data_sock.close();
    } catch (Exception exception) {}
    if (this.data_sock != null)
      this.thisSession.old_data_socks.remove(this.data_sock); 
    try {
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
  
  public String SG(String data) {
    return this.thisSession.SG(data);
  }
  
  public int reloadBandwidthLimits() {
    int lesserSpeed = this.thisSession.IG("speed_limit_download");
    if ((lesserSpeed > 0 && lesserSpeed > ServerStatus.IG("max_server_download_speed") && ServerStatus.IG("max_server_download_speed") > 0) || lesserSpeed == 0)
      lesserSpeed = ServerStatus.IG("max_server_download_speed"); 
    return lesserSpeed;
  }
  
  public static void downloadFinishedSuccess(SessionCrush thisSession, String the_file_path, String the_file_name, Properties item, long starting_loc, boolean httpDownload, Vector zippedFiles, long start_transfer_time, MessageDigest md5, long resume_loc, long current_loc, boolean binary_mode) throws Exception {
    String md5Str = (new BigInteger(1, md5.digest())).toString(16).toLowerCase();
    while (md5Str.length() < 32)
      md5Str = "0" + md5Str; 
    thisSession.uiPUT("md5", md5Str);
    thisSession.uiPUT("sfv", md5Str);
    if (thisSession.uiLG("overall_transfer_speed") == 0L)
      thisSession.uiPUT("overall_transfer_speed", (new StringBuffer(String.valueOf((thisSession.uiLG("bytes_sent") - thisSession.uiLG("start_transfer_byte_amount")) / 1024L))).toString()); 
    ServerStatus.thisObj.server_info.put("downloaded_files", ServerStatus.siIG("downloaded_files") + 1);
    Properties fileItem = (Properties)item.clone();
    fileItem.put("the_command", "RETR");
    String the_correct_path = String.valueOf(the_file_path) + the_file_name;
    if (!the_correct_path.toUpperCase().startsWith(thisSession.SG("root_dir").toUpperCase()))
      the_correct_path = String.valueOf(thisSession.SG("root_dir")) + the_correct_path.substring(1); 
    fileItem.put("the_command_data", the_correct_path);
    fileItem.put("url", item.getProperty("url", ""));
    fileItem.put("the_file_path", String.valueOf(the_file_path) + the_file_name);
    fileItem.put("the_file_name", the_file_name);
    fileItem.put("the_file_size", (new StringBuffer(String.valueOf(thisSession.uiLG("file_length")))).toString());
    fileItem.put("the_file_speed", (new StringBuffer(String.valueOf(thisSession.uiLG("overall_transfer_speed")))).toString());
    fileItem.put("the_file_start", (new StringBuffer(String.valueOf(start_transfer_time))).toString());
    fileItem.put("the_file_end", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
    fileItem.put("the_file_error", "");
    fileItem.put("the_file_status", "SUCCESS");
    fileItem.put("the_file_resume_loc", (new StringBuffer(String.valueOf(resume_loc))).toString());
    fileItem.put("the_file_type", item.getProperty("type", "FILE"));
    fileItem.put("the_file_md5", md5Str);
    Vector downloadItems = new Vector();
    downloadItems.addElement(fileItem);
    boolean skipEvent = false;
    if (fileItem.getProperty("the_file_path", "").indexOf("/WebInterface/") >= 0 || fileItem.getProperty("the_file_name", "").equals(".DS_Store") || fileItem.getProperty("the_file_name", "").equals("custom.js") || fileItem.getProperty("the_file_name", "").equals("custom.css") || (resume_loc > 0L && httpDownload)) {
      skipEvent = true;
    } else if (zippedFiles.size() > 0) {
      downloadItems.removeAllElements();
      for (int x = 0; x < zippedFiles.size(); x++) {
        Properties properties = zippedFiles.elementAt(x);
        if (!properties.getProperty("zipPath", "").equals("")) {
          fileItem = (Properties)fileItem.clone();
          fileItem.put("the_file_path", String.valueOf(the_file_path) + properties.getProperty("zipPath"));
          fileItem.put("the_file_name", Common.last(properties.getProperty("zipPath")));
          fileItem.put("url", properties.getProperty("url", ""));
          fileItem.put("the_file_size", properties.getProperty("size"));
          fileItem.put("the_file_type", properties.getProperty("type", "FILE"));
          thisSession.uiPUT("session_downloads", String.valueOf(thisSession.uiSG("session_downloads")) + the_file_path + the_file_name + ":" + thisSession.uiLG("file_length") + LOC.G("bytes") + " @ " + thisSession.uiLG("overall_transfer_speed") + "k/sec." + CRLF);
          thisSession.uiPUT("session_download_count", (new StringBuffer(String.valueOf(thisSession.uiIG("session_download_count") + 1))).toString());
          ServerStatus.thisObj.statTools.add_item_stat(thisSession, fileItem, "DOWNLOAD");
          downloadItems.addElement(fileItem);
        } 
      } 
    } else {
      thisSession.uiPUT("session_downloads", String.valueOf(thisSession.uiSG("session_downloads")) + the_file_path + the_file_name + ":" + thisSession.uiLG("file_length") + LOC.G("bytes") + " @ " + thisSession.uiLG("overall_transfer_speed") + "k/sec." + CRLF);
      thisSession.uiPUT("session_download_count", (new StringBuffer(String.valueOf(thisSession.uiIG("session_download_count") + 1))).toString());
      ServerStatus.thisObj.statTools.add_item_stat(thisSession, fileItem, "DOWNLOAD");
    } 
    String responseNumber = "226";
    String message_string = "";
    Properties p = new Properties();
    p.put("responseNumber", responseNumber);
    p.put("message_string", message_string);
    if (!skipEvent)
      thisSession.runPlugin("after_download", p); 
    responseNumber = p.getProperty("responseNumber", responseNumber);
    message_string = p.getProperty("message_string", message_string);
    if (!message_string.equals("") && ServerStatus.BG("log_transfer_speeds")) {
      thisSession.not_done = thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-", "RETR");
      thisSession.not_done = thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-" + message_string, "RETR");
      thisSession.not_done = thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-", "RETR");
    } 
    String stop_message = "Failure!";
    if (!responseNumber.equals("226"))
      stop_message = message_string; 
    if (thisSession.LG("max_download_amount") != 0L)
      thisSession.not_done = thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-" + LOC.G("Max Download") + ".  " + LOC.G("Sent") + ":" + (thisSession.uiLG("bytes_sent") / 1024L) + "k.  " + LOC.G("Max") + ":" + thisSession.LG("max_download_amount") + "k.  " + LOC.G("Available") + ":" + (thisSession.LG("max_download_amount") - thisSession.uiLG("bytes_sent") / 1024L) + "k.", "RETR"); 
    if (thisSession.IG("max_download_count") != 0)
      thisSession.not_done = thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-" + LOC.G("Max Download") + ".  " + LOC.G("Sent") + ":" + thisSession.uiLG("session_download_count") + ".  " + LOC.G("Max") + ":" + thisSession.IG("max_download_count") + ".  " + LOC.G("Available") + ":" + (thisSession.IG("max_download_count") - thisSession.uiLG("session_download_count")) + ".", "RETR"); 
    if (thisSession.IG("ratio") != 0)
      thisSession.not_done = thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-" + LOC.G("Ratio is $0 to 1.", (new StringBuffer(String.valueOf(thisSession.IG("ratio")))).toString()) + " " + LOC.G("Received") + ":" + ((thisSession.uiLG("bytes_received") + thisSession.uiLG("ratio_bytes_received")) / 1024L) + "k " + LOC.G("Sent") + ":" + ((thisSession.uiLG("bytes_sent") + thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.  " + LOC.G("Available") + ":" + (((thisSession.uiLG("bytes_received") + thisSession.uiLG("ratio_bytes_received")) * thisSession.IG("ratio") - thisSession.uiLG("bytes_sent") + thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.", "RETR"); 
    if (ServerStatus.BG("log_transfer_speeds"))
      thisSession.not_done = thisSession.ftp_write_command_logged(String.valueOf(responseNumber) + "-%RETR-speed%", "RETR"); 
    if (responseNumber.equals("226")) {
      if (ServerStatus.BG("generic_ftp_responses")) {
        thisSession.not_done = thisSession.ftp_write_command_logged("226", "Transfer complete.", "RETR");
      } else {
        thisSession.not_done = thisSession.ftp_write_command_logged((new StringBuffer(String.valueOf(responseNumber))).toString(), "%RETR-end% " + LOC.G("(\"$0$1\") RETR", the_file_path, the_file_name), "RETR");
      } 
    } else {
      thisSession.not_done = thisSession.ftp_write_command_logged((new StringBuffer(String.valueOf(responseNumber))).toString(), String.valueOf(stop_message) + " " + LOC.G("(\"$0$1\") RETR", the_file_path, the_file_name), "RETR");
    } 
    boolean pgp = checkPgp(thisSession, item);
    if ((current_loc == thisSession.uiLG("file_length") || pgp || !binary_mode) && !skipEvent) {
      for (int x = 0; x < downloadItems.size(); x++) {
        Properties fi = downloadItems.elementAt(x);
        thisSession.add_log("[" + thisSession.uiSG("user_number") + ":" + thisSession.uiSG("user_name") + ":" + thisSession.uiSG("user_ip") + "] WROTE: *Adding " + fi.getProperty("the_file_name") + " to event tracking.*", "RETR");
        thisSession.do_event5("DOWNLOAD", fi);
      } 
    } else {
      thisSession.add_log("[" + thisSession.uiSG("user_number") + ":" + thisSession.uiSG("user_name") + ":" + thisSession.uiSG("user_ip") + "] WROTE: *Event skipped since file download size didn't match:" + current_loc + "!=" + thisSession.uiLG("file_length"), "RETR");
    } 
  }
  
  public static boolean checkPgp(SessionCrush thisSession, Properties item) {
    boolean pgp = false;
    if (!thisSession.user.getProperty("fileEncryptionKey", "").equals("") || !thisSession.user.getProperty("fileDecryptionKey", "").equals("")) {
      pgp = true;
    } else if (!ServerStatus.SG("fileEncryptionKey").equals("") || ServerStatus.BG("fileDecryption")) {
      pgp = true;
    } else if (item != null && (item.getProperty("privs", "").indexOf("(pgpDecryptDownload") >= 0 || item.getProperty("privs", "").indexOf("(pgpEncryptDownload") >= 0)) {
      pgp = true;
    } 
    return pgp;
  }
  
  public Properties make_current_item(long start_transfer_time, Properties tmp) {
    Properties current_item = new Properties();
    current_item.put("name", tmp.getProperty("name"));
    current_item.put("root_dir", tmp.getProperty("root_dir"));
    current_item.put("modified", tmp.getProperty("modified"));
    current_item.put("the_file_size", tmp.getProperty("size", "0"));
    current_item.put("the_file_speed", "0");
    current_item.put("current_loc", (new StringBuffer(String.valueOf(this.current_loc))).toString());
    current_item.put("user_name", this.thisSession.uiSG("user_name"));
    current_item.put("user_ip", this.thisSession.uiSG("user_ip"));
    current_item.put("user_protocol", this.thisSession.uiSG("user_protocol"));
    current_item.put("the_file_start", (new StringBuffer(String.valueOf(start_transfer_time))).toString());
    current_item.put("the_file_type", tmp.getProperty("type", "FILE"));
    ServerStatus.siVG("outgoing_transfers").addElement(current_item);
    return current_item;
  }
  
  public static void updateTransferStats(SessionCrush thisSession, int data_read, boolean free_ratio_item, byte[] temp_array, MessageDigest md5, Properties current_download_item) {
    thisSession.active_transfer();
    if (data_read < 0)
      return; 
    if (System.getProperty("crushftp.retr_md5", "true").equals("true"))
      md5.update(temp_array, 0, data_read); 
    thisSession.uiPPUT("bytes_sent", data_read);
    thisSession.uiPUT("bytes_sent_formatted", Common.format_bytes_short(Long.parseLong(thisSession.uiSG("bytes_sent"))));
    ServerStatus.thisObj.total_server_bytes_sent += data_read;
    if (free_ratio_item)
      thisSession.uiPPUT("ratio_bytes_received", data_read); 
    if (current_download_item != null) {
      current_download_item.put("current_loc", (new StringBuffer(String.valueOf(Long.parseLong(current_download_item.getProperty("current_loc", "0")) + data_read))).toString());
      current_download_item.put("the_file_speed", (new StringBuffer(String.valueOf(thisSession.uiLG("current_transfer_speed")))).toString());
      current_download_item.put("now", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
    } 
    if (thisSession.server_item.containsKey("bytes_sent"))
      synchronized (thisSession.server_item) {
        thisSession.server_item.put("bytes_sent", (new StringBuffer(String.valueOf(Long.parseLong(thisSession.server_item.getProperty("bytes_sent", "0")) + data_read))).toString());
      }  
  }
}
