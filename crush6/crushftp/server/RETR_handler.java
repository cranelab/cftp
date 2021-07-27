package crushftp.server;

import com.crushftp.client.Common;
import com.crushftp.client.GenericClient;
import com.crushftp.client.VRL;
import com.crushftp.tunnel.FileArchiveEntry;
import com.crushftp.tunnel.FileArchiveOutputStream;
import crushftp.gui.LOC;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.SharedSession;
import crushftp.handlers.TransferSpeedometer;
import java.io.BufferedOutputStream;
import java.io.File;
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
  
  ServerSession thisSession = null;
  
  static String CRLF = "\r\n";
  
  int user_down_count = 0;
  
  int new_user_down_count = 0;
  
  Properties item;
  
  boolean pasv_connect = false;
  
  String encode_on_fly = "";
  
  public boolean active = false;
  
  public MessageDigest md5 = null;
  
  LIST_handler filetree_list = null;
  
  public long current_loc = 0L;
  
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
  
  SimpleDateFormat proxySDF = new SimpleDateFormat("MMddyyHHmmss");
  
  RandomAccessFile proxy = null;
  
  InputStream proxy_remote_in = null;
  
  public String streamOpenStatus = "PENDING";
  
  Socket data_sock = null;
  
  public String stop_message = "";
  
  public boolean inError = false;
  
  String threadName = "";
  
  public void init_vars(String the_dir, long current_loc, long max_loc, ServerSession thisSession, Properties item, boolean pasv_connect, String encode_on_fly, VRL otherFile) {
    this.the_dir = the_dir;
    this.thisSession = thisSession;
    this.current_loc = current_loc;
    thisSession.uiPUT("max_loc", (new StringBuffer(String.valueOf(max_loc))).toString());
    this.item = item;
    this.pasv_connect = pasv_connect;
    this.encode_on_fly = encode_on_fly;
    this.otherFile = otherFile;
    try {
      if (this.md5 == null)
        this.md5 = MessageDigest.getInstance("MD5"); 
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
        this.thisSession.uiPUT("sending_file", "true");
        this.active = true;
        this.streamOpenStatus = "PENDING";
        this.proxy_remote_in = null;
        updateTransferStats(this.thisSession, -1, false, null, this.md5);
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
          (new File(String.valueOf(ServerStatus.SG("proxyDownloadRepository")) + this.thisSession.uiSG("user_name") + the_file_path)).mkdirs();
          this.proxy = new RandomAccessFile(String.valueOf(ServerStatus.SG("proxyDownloadRepository")) + this.thisSession.uiSG("user_name") + the_file_path + this.proxySDF.format(new Date()) + "_" + the_file_name, "rw");
          if (this.proxy.length() > this.current_loc)
            this.proxy.setLength(this.current_loc); 
        } 
        the_file_name = Common.url_decode(the_file_name);
        this.thisSession.uiPUT("last_file_real_path", this.item.getProperty("url", ""));
        this.thisSession.uiPUT("last_file_name", Common.last(this.item.getProperty("url", "")));
        long start_transfer_time = (new Date()).getTime();
        long max_download_amount = this.thisSession.IG("max_download_amount");
        long max_download_amount_day = this.thisSession.IG("max_download_amount_day");
        long max_download_amount_month = this.thisSession.IG("max_download_amount_month");
        long start_download_amount_day = 0L;
        if (max_download_amount_day > 0L)
          start_download_amount_day = ServerStatus.thisObj.statTools.getTransferAmountToday(this.thisSession.uiSG("user_ip"), this.thisSession.uiSG("user_name"), this.thisSession.uiPG("stat"), "downloads", this.thisSession); 
        long start_download_amount_month = 0L;
        if (max_download_amount_month > 0L)
          start_download_amount_month = ServerStatus.thisObj.statTools.getTransferAmountThisMonth(this.thisSession.uiSG("user_ip"), this.thisSession.uiSG("user_name"), this.thisSession.uiPG("stat"), "downloads", this.thisSession); 
        if (this.thisSession.IG("ratio") > 0 && !free_ratio_item && (this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) * this.thisSession.IG("ratio") <= this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) {
          this.stop_message = "550-" + LOC.G("WARNING!!! Ratio reached.") + "  " + LOC.G("Ratio is") + " " + this.thisSession.IG("ratio") + " to 1.  " + LOC.G("Received") + ":" + ((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) / 1024L) + "k  " + LOC.G("Sent") + ":" + ((this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.  " + LOC.G("Available") + ":" + (((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) * this.thisSession.IG("ratio") - this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.";
          this.thisSession.not_done = this.thisSession.write_command(this.stop_message);
          this.thisSession.not_done = this.thisSession.write_command("550", "%RETR-ratio exceeded%");
          this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
          this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
        } else if (max_download_amount > 0L && this.thisSession.uiLG("bytes_sent") > max_download_amount * 1024L) {
          this.stop_message = "550-" + LOC.G("WARNING!!! Maximum download amount reached.") + "  " + LOC.G("Sent") + ":" + (this.thisSession.uiLG("bytes_sent") / 1024L) + "k.  " + LOC.G("Max") + ":" + max_download_amount + "k.  " + LOC.G("Available") + ":" + (max_download_amount - this.thisSession.uiLG("bytes_sent") / 1024L) + "k.";
          this.thisSession.not_done = this.thisSession.write_command(this.stop_message);
          this.thisSession.not_done = this.thisSession.write_command("550", "%RETR-max reached%");
          this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
          this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
          ServerStatus.thisObj.runAlerts("user_download_session", this.thisSession);
        } else if ((max_download_amount_day > 0L && start_download_amount_day > max_download_amount_day * 1024L) || (max_download_amount_month > 0L && start_download_amount_month > max_download_amount_month * 1024L)) {
          if (max_download_amount_day > 0L && start_download_amount_day > max_download_amount_day * 1024L) {
            this.stop_message = "550-" + LOC.G("WARNING!!! Maximum download amount today reached.") + "  " + LOC.G("Sent") + ":" + (start_download_amount_day / 1024L) + "k.  " + LOC.G("Max") + ":" + max_download_amount_day + "k.  ";
            this.thisSession.not_done = this.thisSession.write_command(this.stop_message);
            ServerStatus.thisObj.runAlerts("user_download_day", this.thisSession);
          } 
          if (max_download_amount_month > 0L && start_download_amount_month > max_download_amount_month * 1024L) {
            this.stop_message = "550-" + LOC.G("WARNING!!! Maximum download amount last 30 days reached.") + "  " + LOC.G("Sent") + ":" + (start_download_amount_month / 1024L) + "k.  " + LOC.G("Max") + ":" + max_download_amount_month + "k.  ";
            this.thisSession.not_done = this.thisSession.write_command(this.stop_message);
            ServerStatus.thisObj.runAlerts("user_download_month", this.thisSession);
          } 
          this.thisSession.not_done = this.thisSession.write_command("550", "%RETR-max reached%");
          this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
          this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
        } else {
          if (stat != null)
            this.thisSession.uiPUT("file_length", stat.getProperty("size")); 
          if (this.thisSession.BG("partial_download") && this.thisSession.IG("ratio") > 0 && !free_ratio_item && this.thisSession.uiLG("file_length") - this.current_loc > (this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) * this.thisSession.IG("ratio") - this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) {
            String response_str = "%RETR-ratio will be exceeded%" + CRLF + LOC.G("Ratio is $0 to 1.", (new StringBuffer(String.valueOf(this.thisSession.IG("ratio")))).toString()) + "  " + LOC.G("Received") + ":" + ((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) / 1024L) + "k  " + LOC.G("Sent") + ":" + ((this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.  " + LOC.G("Available") + ":" + (((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) * this.thisSession.IG("ratio") - this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.  " + LOC.G("Attempting to download") + ":" + ((this.thisSession.uiLG("file_length") - this.current_loc) / 1024L) + "k." + CRLF;
            response_str = String.valueOf(response_str) + "%RETR-ratio will be exceeded abort%";
            this.stop_message = response_str;
            this.thisSession.not_done = this.thisSession.write_command("550", response_str);
            this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
            this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
          } else if (this.thisSession.BG("partial_download") && this.thisSession.IG("max_download_amount") > 0 && this.thisSession.uiLG("file_length") - this.current_loc > (this.thisSession.IG("max_download_amount") * 1024) - this.thisSession.uiLG("bytes_sent")) {
            String response_str = "%RETR-max download will be exceeded%" + CRLF + LOC.G("Sent") + ":" + (this.thisSession.uiLG("bytes_sent") / 1024L) + "k.  " + LOC.G("Max") + ":" + this.thisSession.IG("max_download_amount") + "k.  " + LOC.G("Available") + ":" + (this.thisSession.IG("max_download_amount") - this.thisSession.uiLG("bytes_sent") / 1024L) + "k.  " + LOC.G("Attempting to download") + ":" + ((this.thisSession.uiLG("file_length") - this.current_loc) / 1024L) + "k." + CRLF;
            response_str = String.valueOf(response_str) + "%RETR-max download will be exceeded abort%";
            this.stop_message = response_str;
            this.thisSession.not_done = this.thisSession.write_command("550", response_str);
            this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
            this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
          } else {
            long resume_loc = this.current_loc;
            int loop_times = 0;
            while (this.otherFile == null && this.thisSession.data_socks.size() == 0 && loop_times++ < 10000)
              Thread.sleep(1L); 
            if (this.otherFile == null && this.thisSession.data_socks.size() == 0) {
              this.thisSession.not_done = this.thisSession.write_command("550", "%PORT-fail_question%" + CRLF + "%PORT-no_data_connection%");
              this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
              this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
            } else {
              try {
                if (this.thisSession.data_socks.size() > 0)
                  this.data_sock = this.thisSession.data_socks.remove(0); 
                if (this.data_sock != null)
                  this.data_sock.setSoTimeout(((this.thisSession.IG("max_idle_time") == 0) ? 5 : this.thisSession.IG("max_idle_time")) * 1000 * 60); 
                if (this.otherFile == null)
                  this.data_os = new BufferedOutputStream(this.data_sock.getOutputStream()); 
                this.streamOpenStatus = "OPEN";
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
                  this.thisSession.not_done = this.thisSession.write_command(String.valueOf(responseNumber) + "-");
                  this.thisSession.not_done = this.thisSession.write_command(String.valueOf(responseNumber) + "-" + message_string);
                  this.thisSession.not_done = this.thisSession.write_command(String.valueOf(responseNumber) + "-");
                } 
                if (ServerStatus.BG("server_download_queueing")) {
                  long wait_time = (new Date()).getTime();
                  this.thisSession.not_done = this.thisSession.write_command(String.valueOf(responseNumber) + "-" + LOC.G("You are in a queue.  There are $0 downloads allowed with a maximum queue of size $1.", (new StringBuffer(String.valueOf(ServerStatus.IG("server_download_queue_size")))).toString(), (new StringBuffer(String.valueOf(ServerStatus.IG("server_download_queue_size_max")))).toString()));
                  while (this.thisSession.uiBG("pause_now")) {
                    this.thisSession.uiPPUT("bytes_received", 1L);
                    this.thisSession.uiPPUT("bytes_sent", 1L);
                    this.thisSession.not_done = this.thisSession.write_command(String.valueOf(responseNumber) + "-" + LOC.G("You are in currently in position $0 out of a queue of size $1.", (new StringBuffer(String.valueOf(ServerStatus.thisObj.server_download_queue.indexOf(this.thisSession) + 1))).toString(), (new StringBuffer(String.valueOf(ServerStatus.thisObj.server_download_queue.size()))).toString()));
                    long cur_wait = (new Date()).getTime() - wait_time;
                    String wait_formatted = String.valueOf(cur_wait / 1000L) + " " + LOC.G("seconds") + ".";
                    if (cur_wait > 60000L)
                      wait_formatted = String.valueOf(cur_wait / 60000L) + " " + LOC.G("minutes") + ", " + ((cur_wait - cur_wait / 60000L * 60000L) / 1000L) + " " + LOC.G("seconds") + "."; 
                    this.thisSession.not_done = this.thisSession.write_command(String.valueOf(responseNumber) + "-" + LOC.G("You have been waiting") + " " + wait_formatted);
                    Vector t_times = ServerStatus.get_transfer_times();
                    cur_wait = 0L;
                    try {
                      try {
                        cur_wait = Integer.parseInt(t_times.elementAt(ServerStatus.thisObj.server_download_queue.indexOf(this.thisSession)).toString());
                      } catch (Exception e) {
                        cur_wait = Integer.parseInt(t_times.elementAt(t_times.size() - 1).toString());
                      } 
                    } catch (Exception exception) {}
                    cur_wait *= 1000L;
                    wait_formatted = String.valueOf(cur_wait / 1000L) + " " + LOC.G("seconds") + ".";
                    if (cur_wait > 60000L)
                      wait_formatted = String.valueOf(cur_wait / 60000L) + " " + LOC.G("minutes") + ", " + ((cur_wait - cur_wait / 60000L * 60000L) / 1000L) + " " + LOC.G("seconds") + "."; 
                    this.thisSession.not_done = this.thisSession.write_command(String.valueOf(responseNumber) + "-" + LOC.G("Estimated wait is") + " " + wait_formatted);
                    int iterations = 0;
                    while (iterations++ < 30) {
                      Thread.sleep(1000L);
                      if (!this.thisSession.uiBG("pause_now"))
                        break; 
                    } 
                  } 
                } 
                if (this.thisSession.IG("max_download_amount") != 0)
                  this.thisSession.not_done = this.thisSession.write_command(String.valueOf(responseNumber) + "-" + LOC.G("Max Download") + ".  " + LOC.G("Sent") + ":" + (this.thisSession.uiLG("bytes_sent") / 1024L) + "k.  " + LOC.G("Max") + ":" + this.thisSession.IG("max_download_amount") + "k.  " + LOC.G("Available") + ":" + (this.thisSession.IG("max_download_amount") - this.thisSession.uiLG("bytes_sent") / 1024L) + "k."); 
                if (this.thisSession.IG("max_upload_amount") != 0)
                  this.thisSession.not_done = this.thisSession.write_command(String.valueOf(responseNumber) + "-" + LOC.G("Max Upload") + ".  " + LOC.G("Sent") + ":" + (this.thisSession.uiLG("bytes_sent") / 1024L) + "k.  " + LOC.G("Max") + ":" + this.thisSession.IG("max_upload_amount") + "k.  " + LOC.G("Available") + ":" + (this.thisSession.IG("max_upload_amount") - this.thisSession.uiLG("bytes_sent") / 1024L) + "k."); 
                if (this.thisSession.IG("ratio") != 0)
                  this.thisSession.not_done = this.thisSession.write_command(String.valueOf(responseNumber) + "-" + LOC.G("Ratio is $0 to 1.", (new StringBuffer(String.valueOf(this.thisSession.IG("ratio")))).toString()) + " " + LOC.G("Received") + ":" + ((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) / 1024L) + "k " + LOC.G("Sent") + ":" + ((this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.  " + LOC.G("Available") + ":" + (((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) * this.thisSession.IG("ratio") - this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k."); 
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
                    }  
                  if (!Common.System2.containsKey("crushftp.dmz.queue.sock"))
                    if (!this.thisSession.user.getProperty("fileEncryptionKey", "").equals("") || !this.thisSession.user.getProperty("fileDecryptionKey", "").equals("")) {
                      this.in = Common.getDecryptedStream(this.in, this.thisSession.user.getProperty("fileEncryptionKey", ""), this.thisSession.user.getProperty("fileDecryptionKey", ""), this.thisSession.user.getProperty("fileDecryptionKeyPass", ""));
                    } else if (!ServerStatus.SG("fileEncryptionKey").equals("") || ServerStatus.BG("fileDecryption")) {
                      this.in = Common.getDecryptedStream(this.in, ServerStatus.SG("fileEncryptionKey"), ServerStatus.SG("fileDecryptionKey"), ServerStatus.SG("fileDecryptionKeyPass"));
                    }  
                } 
                this.thisSession.not_done = this.thisSession.write_command((new StringBuffer(String.valueOf(responseNumber))).toString(), "%RETR-start% " + LOC.G("(\"$0$1\") R E T R", the_file_path, the_file_name));
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
                            while (this.this$0.active && bytesRead >= 0) {
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
                LineReader inASCII = null;
                while (data_read >= 0) {
                  Date new_date = new Date();
                  if (new_date.getTime() - speedometerCheckInterval > 10000L) {
                    lesserSpeed = reloadBandwidthLimits();
                    speedometerCheckInterval = new_date.getTime();
                  } 
                  if (lesserSpeed > 0 && !speedController.bandwidth_immune_server)
                    this.slow_transfer = speedController.getDelayAmount(data_read, this.startLoop, this.endLoop, temp_array.length, this.slow_transfer, lesserSpeed); 
                  while (this.pause_transfer)
                    Thread.sleep(100L); 
                  this.startLoop = (new Date()).getTime();
                  if (this.slow_transfer > 0.0F)
                    Thread.sleep((int)this.slow_transfer); 
                  float packPercent = 0.0F;
                  packPercent = (float)this.thisSession.uiLG("overall_transfer_speed") * 1024.0F / 10.0F / temp_array.length;
                  if ((packPercent > 1.5D || packPercent < 0.5D) && packPercent > 0.0F) {
                    int newSize = (int)this.thisSession.uiLG("overall_transfer_speed") * 1024 / 10;
                    if (newSize > 1000 && newSize < maxPackSize)
                      temp_array = new byte[newSize]; 
                  } 
                  if (this.thisSession.uiLG("max_loc") > 0L && this.thisSession.uiLG("max_loc") - this.current_loc < temp_array.length)
                    if ((int)(this.thisSession.uiLG("max_loc") - this.current_loc) >= 0)
                      temp_array = new byte[(int)(this.thisSession.uiLG("max_loc") - this.current_loc)];  
                  if (max_download_amount > 0L && this.thisSession.uiLG("bytes_sent") > max_download_amount * 1024L) {
                    data_read = -1;
                    this.thisSession.not_done = this.thisSession.write_command("226-%RETR-max reached%");
                    ServerStatus.thisObj.runAlerts("user_download_session", this.thisSession);
                  } else if (max_download_amount_day > 0L && this.thisSession.uiLG("bytes_sent") + start_download_amount_day > max_download_amount_day * 1024L) {
                    data_read = -1;
                    this.thisSession.not_done = this.thisSession.write_command("226-" + LOC.G("WARNING!!! Maximum download amount today reached.") + "  " + LOC.G("Sent") + ":" + ((this.thisSession.uiLG("bytes_sent") + start_download_amount_day) / 1024L) + "k.  " + LOC.G("Max") + ":" + max_download_amount_day + "k.  ");
                    this.thisSession.not_done = this.thisSession.write_command("226-%RETR-max reached%");
                    ServerStatus.thisObj.runAlerts("user_download_day", this.thisSession);
                  } else if (max_download_amount_month > 0L && this.thisSession.uiLG("bytes_sent") + start_download_amount_month > max_download_amount_month * 1024L) {
                    data_read = -1;
                    this.thisSession.not_done = this.thisSession.write_command("226-" + LOC.G("WARNING!!! Maximum download amount last 30 days reached.") + "  " + LOC.G("Sent") + ":" + ((this.thisSession.uiLG("bytes_sent") + start_download_amount_month) / 1024L) + "k.  " + LOC.G("Max") + ":" + max_download_amount_month + "k.  ");
                    this.thisSession.not_done = this.thisSession.write_command("226-%RETR-max reached%");
                    ServerStatus.thisObj.runAlerts("user_download_month", this.thisSession);
                  } else if (ratio == 0 || free_ratio_item || (this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) * ratio > this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) {
                    if (this.zipping) {
                      if (Zin == null) {
                        while (this.zipFiles.size() > 0) {
                          Properties zipItem = this.zipFiles.elementAt(0);
                          if (zipItem.getProperty("type", "").equalsIgnoreCase("DIR") && zipItem.getProperty("privs").toUpperCase().indexOf("(VIEW)") < 0) {
                            this.zipFiles.removeElementAt(0);
                            continue;
                          } 
                          if (!Common.filter_check("L", zipItem.getProperty("name"), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + this.thisSession.SG("file_filter"))) {
                            this.zipFiles.removeElementAt(0);
                            continue;
                          } 
                          if (!this.thisSession.check_access_privs(zipItem.getProperty("root_dir"), "RETR", zipItem) || !this.thisSession.check_access_privs(String.valueOf(zipItem.getProperty("root_dir")) + zipItem.getProperty("name"), "RETR", zipItem)) {
                            this.zipFiles.removeElementAt(0);
                            continue;
                          } 
                          break;
                        } 
                        if (this.zipFiles.size() > 0) {
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
                          while (zipItem.getProperty("privs").indexOf("(read)") < 0 || zipItem.getProperty("privs").indexOf("(invisible)") >= 0 || (zipItem.getProperty("type", "").equalsIgnoreCase("DIR") && zipItem.getProperty("privs").indexOf("(view)") < 0)) {
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
                              zipItem.put("zipPath", String.valueOf(zipItem.getProperty("root_dir", "").substring(the_file_path.length())) + zipItem.getProperty("name") + "/");
                              FileArchiveEntry ze = new FileArchiveEntry(zipItem.getProperty("zipPath"));
                              ze.setTime(Long.parseLong(zipItem.getProperty("modified", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString())));
                              ((FileArchiveOutputStream)this.data_os).putArchiveEntry(ze);
                              ((FileArchiveOutputStream)this.data_os).closeArchiveEntry();
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
                                  ze = new FileArchiveEntry(zipItem.getProperty("zipPath"));
                                  ze.setTime(Long.parseLong(zipItem.getProperty("modified", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString())));
                                  ((FileArchiveOutputStream)this.data_os).putArchiveEntry(ze);
                                  ((FileArchiveOutputStream)this.data_os).closeArchiveEntry();
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
                            FileArchiveEntry ze = new FileArchiveEntry(zipItem.getProperty("zipPath"));
                            ze.setTime(Long.parseLong(zipItem.getProperty("modified", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString())));
                            try {
                              ((FileArchiveOutputStream)this.data_os).putArchiveEntry(ze);
                              this.in = this.c.download(Zin.getPath(), rest, -1L, binary_mode);
                              if (!Common.System2.containsKey("crushftp.dmz.queue.sock"))
                                if (!this.thisSession.user.getProperty("fileEncryptionKey", "").equals("") || !this.thisSession.user.getProperty("fileDecryptionKey", "").equals("")) {
                                  this.in = Common.getDecryptedStream(this.in, this.thisSession.user.getProperty("fileEncryptionKey", ""), this.thisSession.user.getProperty("fileDecryptionKey", ""), this.thisSession.user.getProperty("fileDecryptionKeyPass", ""));
                                } else if (!ServerStatus.SG("fileEncryptionKey").equals("") || ServerStatus.BG("fileDecryption")) {
                                  this.in = Common.getDecryptedStream(this.in, ServerStatus.SG("fileEncryptionKey"), ServerStatus.SG("fileDecryptionKeyPass"), ServerStatus.SG("fileDecryptionKeyPass"));
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
                            if (cur_file_size > start_file_size) {
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
                      updateTransferStats(this.thisSession, data_read, free_ratio_item, temp_array, this.md5);
                      this.current_loc += ((data_read > 0) ? data_read : 0L);
                      file_changing_loop_intervals = 0;
                    } 
                    if (this.thisSession.uiLG("max_loc") > 0L && this.thisSession.uiLG("max_loc") - this.current_loc == 0L)
                      data_read = -1; 
                  } else {
                    data_read = -1;
                    this.thisSession.not_done = this.thisSession.write_command("226-%RETR-ratio exceeded%");
                  } 
                  if (this.thisSession.uiLG("overall_transfer_speed") < this.thisSession.IG("min_download_speed") && this.thisSession.IG("min_download_speed") > 0 && new_date.getTime() - this.thisSession.uiLG("start_transfer_time") > 10000L)
                    throw new Exception(LOC.G("Transfer speed is less than minimum required after at least 10 seconds.") + "  " + this.thisSession.uiLG("overall_transfer_speed") + "K/sec < " + this.thisSession.IG("min_download_speed") + "K/sec."); 
                  this.endLoop = (new Date()).getTime();
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
                if ((this.otherFile == null || this.zipping || this.zlibing) && !this.httpDownload)
                  try {
                    this.data_os.close();
                  } catch (Exception exception) {} 
                try {
                  if (this.data_sock != null)
                    this.data_sock.close(); 
                } catch (Exception exception) {}
                if (this.data_sock != null)
                  this.thisSession.old_data_socks.remove(this.data_sock); 
                downloadFinishedSuccess(this.thisSession, the_file_path, the_file_name, this.item, this.thisSession.uiLG("start_resume_loc"), this.httpDownload, this.zippedFiles, start_transfer_time, this.md5, resume_loc, this.current_loc);
              } catch (Exception e) {
                this.stop_message = e.toString();
                this.inError = true;
                Log.log("DOWNLOAD", 1, e);
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
                Properties fileItem = this.thisSession.uiVG("session_commands").elementAt(this.thisSession.uiVG("session_commands").size() - 1);
                fileItem.put("url", this.item.getProperty("url", ""));
                fileItem.put("the_file_path", the_file_path);
                fileItem.put("the_file_name", the_file_name);
                fileItem.put("the_file_size", (new StringBuffer(String.valueOf(this.thisSession.uiLG("file_length")))).toString());
                fileItem.put("the_file_speed", (new StringBuffer(String.valueOf(this.thisSession.uiLG("overall_transfer_speed")))).toString());
                fileItem.put("the_file_error", this.stop_message);
                fileItem.put("the_file_status", "FAILED");
                this.thisSession.uiPUT("session_downloads", String.valueOf(this.thisSession.uiSG("session_downloads")) + the_file_path + the_file_name + ":" + this.thisSession.uiLG("file_length") + LOC.G("bytes") + " @ " + this.thisSession.uiLG("overall_transfer_speed") + "k/sec. " + LOC.G("FAILED") + CRLF);
                if (this.thisSession.IG("max_download_amount") != 0)
                  this.thisSession.not_done = this.thisSession.write_command("226-" + SG("Max Download") + ".  " + LOC.G("Sent") + ":" + (this.thisSession.uiLG("bytes_sent") / 1024L) + "k.  " + SG("Max") + ":" + this.thisSession.IG("max_download_amount") + "k.  " + LOC.G("Available") + ":" + (this.thisSession.IG("max_download_amount") - this.thisSession.uiLG("bytes_sent") / 1024L) + "k."); 
                if (this.thisSession.IG("ratio") != 0)
                  this.thisSession.not_done = this.thisSession.write_command("226-" + LOC.G("Ratio is $0 to 1.", (new StringBuffer(String.valueOf(this.thisSession.IG("ratio")))).toString()) + " " + LOC.G("Received") + ":" + ((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) / 1024L) + "k " + LOC.G("Sent") + ":" + ((this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.  " + LOC.G("Available") + ":" + (((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) * this.thisSession.IG("ratio") - this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k."); 
                if (ServerStatus.BG("rfc_proxy") && e.getMessage() != null && e.getMessage().length() > 3 && e.getMessage().charAt(3) == ' ') {
                  this.thisSession.not_done = this.thisSession.write_command(e.getMessage());
                } else {
                  this.thisSession.not_done = this.thisSession.write_command("550", String.valueOf(e.getMessage()) + " " + LOC.G("(\"$0$1\") RETR", the_file_path, the_file_name));
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
      this.resumed_file = false;
      this.zipping = false;
      this.zlibing = false;
      try {
        this.thisSession.uiPUT("last_time_remaining", "<" + LOC.G("None Active") + ">");
      } catch (Exception exception) {}
      try {
        this.thisSession.uiPUT("last_action", String.valueOf(this.thisSession.uiSG("last_action")) + "-Done.");
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
      this.thisSession.start_idle_timer();
      this.streamOpenStatus = "CLOSED";
      this.active = false;
      if (this.runOnce)
        return; 
    } catch (Exception e) {
      Log.log("DOWNLOAD", 1, e);
    } finally {
      this.thisThread = null;
    } 
    kill();
  }
  
  public void kill() {
    this.active = false;
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
  
  public static void downloadFinishedSuccess(ServerSession thisSession, String the_file_path, String the_file_name, Properties item, long starting_loc, boolean httpDownload, Vector zippedFiles, long start_transfer_time, MessageDigest md5, long resume_loc, long current_loc) throws Exception {
    String md5Str = (new BigInteger(1, md5.digest())).toString(16).toLowerCase();
    thisSession.uiPUT("md5", md5Str);
    thisSession.uiPUT("sfv", md5Str);
    if (thisSession.uiLG("overall_transfer_speed") == 0L)
      thisSession.uiPUT("overall_transfer_speed", (new StringBuffer(String.valueOf((thisSession.uiLG("bytes_sent") - thisSession.uiLG("start_transfer_byte_amount")) / 1024L))).toString()); 
    ServerStatus.thisObj.server_info.put("downloaded_files", ServerStatus.siIG("downloaded_files") + 1);
    Properties fileItem = STOR_handler.findLastItem(httpDownload, thisSession);
    fileItem.put("the_command", "RETR");
    String the_correct_path = String.valueOf(the_file_path) + the_file_name;
    if (!the_correct_path.toUpperCase().startsWith(thisSession.SG("root_dir").toUpperCase()))
      the_correct_path = String.valueOf(thisSession.SG("root_dir")) + the_correct_path.substring(1); 
    fileItem.put("the_command_data", the_correct_path);
    fileItem.put("url", item.getProperty("url", ""));
    fileItem.put("the_file_path", the_file_path);
    fileItem.put("the_file_name", the_file_name);
    fileItem.put("the_file_size", (new StringBuffer(String.valueOf(thisSession.uiLG("file_length")))).toString());
    fileItem.put("the_file_speed", (new StringBuffer(String.valueOf(thisSession.uiLG("overall_transfer_speed")))).toString());
    fileItem.put("the_file_start", (new StringBuffer(String.valueOf(start_transfer_time))).toString());
    fileItem.put("the_file_end", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
    fileItem.put("the_file_error", "");
    fileItem.put("the_file_status", "SUCCESS");
    fileItem.put("the_file_resume_loc", (new StringBuffer(String.valueOf(resume_loc))).toString());
    fileItem.put("the_file_md5", md5Str);
    Vector downloadItems = new Vector();
    downloadItems.addElement(fileItem);
    boolean skipEvent = false;
    if (fileItem.getProperty("the_file_path", "").indexOf("/WebInterface/") >= 0 || fileItem.getProperty("the_file_name", "").equals(".DS_Store") || fileItem.getProperty("the_file_name", "").equals("custom.js") || fileItem.getProperty("the_file_name", "").equals("custom.css") || (resume_loc > 0L && httpDownload)) {
      thisSession.uiVG("session_commands").remove(fileItem);
      skipEvent = true;
    } else if (zippedFiles.size() > 0) {
      downloadItems.removeAllElements();
      for (int x = 0; x < zippedFiles.size(); x++) {
        Properties properties = zippedFiles.elementAt(x);
        if (!properties.getProperty("zipPath", "").equals("")) {
          fileItem = (Properties)fileItem.clone();
          fileItem.put("the_file_path", Common.all_but_last(String.valueOf(the_file_path) + properties.getProperty("zipPath")));
          fileItem.put("the_file_name", Common.last(properties.getProperty("zipPath")));
          fileItem.put("url", properties.getProperty("url", ""));
          fileItem.put("the_file_size", properties.getProperty("size"));
          thisSession.uiPUT("session_downloads", String.valueOf(thisSession.uiSG("session_downloads")) + the_file_path + the_file_name + ":" + thisSession.uiLG("file_length") + LOC.G("bytes") + " @ " + thisSession.uiLG("overall_transfer_speed") + "k/sec." + CRLF);
          thisSession.uiPUT("session_download_count", (new StringBuffer(String.valueOf(thisSession.uiIG("session_download_count") + 1))).toString());
          ServerStatus.thisObj.statTools.add_download_stat(thisSession, fileItem);
          downloadItems.addElement(fileItem);
          thisSession.uiVG("session_commands").addElement(fileItem);
        } 
      } 
    } else {
      thisSession.uiPUT("session_downloads", String.valueOf(thisSession.uiSG("session_downloads")) + the_file_path + the_file_name + ":" + thisSession.uiLG("file_length") + LOC.G("bytes") + " @ " + thisSession.uiLG("overall_transfer_speed") + "k/sec." + CRLF);
      thisSession.uiPUT("session_download_count", (new StringBuffer(String.valueOf(thisSession.uiIG("session_download_count") + 1))).toString());
      ServerStatus.thisObj.statTools.add_download_stat(thisSession, fileItem);
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
      thisSession.not_done = thisSession.write_command(String.valueOf(responseNumber) + "-");
      thisSession.not_done = thisSession.write_command(String.valueOf(responseNumber) + "-" + message_string);
      thisSession.not_done = thisSession.write_command(String.valueOf(responseNumber) + "-");
    } 
    String stop_message = "Failure!";
    if (!responseNumber.equals("226"))
      stop_message = message_string; 
    if (thisSession.IG("max_download_amount") != 0)
      thisSession.not_done = thisSession.write_command(String.valueOf(responseNumber) + "-" + LOC.G("Max Download") + ".  " + LOC.G("Sent") + ":" + (thisSession.uiLG("bytes_sent") / 1024L) + "k.  " + LOC.G("Max") + ":" + thisSession.IG("max_download_amount") + "k.  " + LOC.G("Available") + ":" + (thisSession.IG("max_download_amount") - thisSession.uiLG("bytes_sent") / 1024L) + "k."); 
    if (thisSession.IG("ratio") != 0)
      thisSession.not_done = thisSession.write_command(String.valueOf(responseNumber) + "-" + LOC.G("Ratio is $0 to 1.", (new StringBuffer(String.valueOf(thisSession.IG("ratio")))).toString()) + " " + LOC.G("Received") + ":" + ((thisSession.uiLG("bytes_received") + thisSession.uiLG("ratio_bytes_received")) / 1024L) + "k " + LOC.G("Sent") + ":" + ((thisSession.uiLG("bytes_sent") + thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.  " + LOC.G("Available") + ":" + (((thisSession.uiLG("bytes_received") + thisSession.uiLG("ratio_bytes_received")) * thisSession.IG("ratio") - thisSession.uiLG("bytes_sent") + thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k."); 
    if (ServerStatus.BG("log_transfer_speeds"))
      thisSession.not_done = thisSession.write_command(String.valueOf(responseNumber) + "-%RETR-speed%"); 
    if (responseNumber.equals("226")) {
      thisSession.not_done = thisSession.write_command((new StringBuffer(String.valueOf(responseNumber))).toString(), "%RETR-end% " + LOC.G("(\"$0$1\") RETR", the_file_path, the_file_name));
    } else {
      thisSession.not_done = thisSession.write_command((new StringBuffer(String.valueOf(responseNumber))).toString(), String.valueOf(stop_message) + " " + LOC.G("(\"$0$1\") RETR", the_file_path, the_file_name));
    } 
    boolean pgp = false;
    if (!thisSession.user.getProperty("fileEncryptionKey", "").equals("") || !thisSession.user.getProperty("fileDecryptionKey", "").equals("")) {
      pgp = true;
    } else if (!ServerStatus.SG("fileEncryptionKey").equals("") || ServerStatus.BG("fileDecryption")) {
      pgp = true;
    } else if (item.getProperty("privs", "").indexOf("(pgpDecryptDownload") >= 0) {
      pgp = true;
    } 
    if ((current_loc == thisSession.uiLG("file_length") || pgp) && !skipEvent) {
      for (int x = 0; x < downloadItems.size(); x++) {
        Properties fi = downloadItems.elementAt(x);
        thisSession.add_log("[" + thisSession.uiSG("user_number") + ":" + thisSession.uiSG("user_name") + ":" + thisSession.uiSG("user_ip") + "] WROTE: *Adding " + fi.getProperty("the_file_name") + " to event tracking.*", "RETR");
        thisSession.do_event5("DOWNLOAD", fi);
      } 
    } else {
      thisSession.add_log("[" + thisSession.uiSG("user_number") + ":" + thisSession.uiSG("user_name") + ":" + thisSession.uiSG("user_ip") + "] WROTE: *Event skipped since file download size didn't match:" + current_loc + "!=" + thisSession.uiLG("file_length"), "RETR");
    } 
  }
  
  public static void updateTransferStats(ServerSession thisSession, int data_read, boolean free_ratio_item, byte[] temp_array, MessageDigest md5) {
    SharedSession.find("crushftp.usernames.activity").put(thisSession.getId(), (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
    if (data_read < 0)
      return; 
    md5.update(temp_array, 0, data_read);
    thisSession.uiPPUT("bytes_sent", data_read);
    thisSession.uiPUT("bytes_sent_formatted", Common.format_bytes_short(Long.parseLong(thisSession.uiSG("bytes_sent"))));
    ServerStatus.thisObj.total_server_bytes_sent += data_read;
    if (free_ratio_item)
      thisSession.uiPPUT("ratio_bytes_received", data_read); 
    if (thisSession.server_item.containsKey("bytes_sent"))
      synchronized (thisSession.server_item) {
        thisSession.server_item.put("bytes_sent", (new StringBuffer(String.valueOf(Long.parseLong(thisSession.server_item.getProperty("bytes_sent", "0")) + data_read))).toString());
      }  
  }
}
