package crushftp.server;

import com.crushftp.client.Common;
import com.crushftp.client.GenericClient;
import com.crushftp.client.VRL;
import com.crushftp.tunnel.FileArchiveInputStream;
import crushftp.db.SearchHandler;
import crushftp.gui.LOC;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.SharedSession;
import crushftp.handlers.TransferSpeedometer;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
  
  public int packet_size = 32768;
  
  long quota = 0L;
  
  InputStream data_is = null;
  
  public GenericClient c = null;
  
  OutputStream out = null;
  
  String the_file_path = "";
  
  String the_file_name = "";
  
  ServerSession thisSession = null;
  
  static String CRLF = "\r\n";
  
  int user_up_count = 0;
  
  Properties item;
  
  String realAction = "STOR";
  
  public boolean active = false;
  
  boolean zlibing = false;
  
  public MessageDigest md5 = null;
  
  public Properties metaInfo = null;
  
  public long current_loc = 0L;
  
  public ServerSocket s_sock = null;
  
  public Socket streamer = null;
  
  RandomAccessFile proxy = null;
  
  OutputStream proxy_remote_out = null;
  
  public boolean httpUpload = false;
  
  public boolean inError = false;
  
  public boolean allowTempExtensions = true;
  
  public String streamOpenStatus = "PENDING";
  
  public boolean zipstream = false;
  
  public boolean zipchunkstream = false;
  
  public Socket data_sock = null;
  
  boolean unique = false;
  
  boolean random_access = false;
  
  public long fileModifiedDate = System.currentTimeMillis();
  
  String priorMd5 = "";
  
  String threadName = "";
  
  Vector pendingSyncs = new Vector();
  
  long asciiLineCount = 0L;
  
  SimpleDateFormat proxySDF = new SimpleDateFormat("MMddyyHHmmss");
  
  public void init_vars(String the_dir, long current_loc, ServerSession thisSession, Properties item, String realAction, boolean unique, boolean random_access) {
    this.the_dir = the_dir;
    this.thisSession = thisSession;
    this.current_loc = current_loc;
    this.item = item;
    this.realAction = realAction;
    this.random_access = random_access;
    try {
      if (this.md5 == null)
        this.md5 = MessageDigest.getInstance("MD5"); 
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
      if (this.thisSession != null) {
        this.pendingSyncs.removeAllElements();
        this.thisSession.uiPUT("receiving_file", "true");
        this.metaInfo = null;
        this.active = true;
        this.streamOpenStatus = "PENDING";
        this.proxy_remote_out = null;
        this.priorMd5 = "";
        updateTransferStats(this.thisSession, -1, this.httpUpload, null, this.md5);
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
        VRL vrl = new VRL(this.item.getProperty("url"));
        Properties stat = this.c.stat(vrl.getPath());
        this.priorMd5 = getPriorMd5(this.item, stat);
        if (!vrl.getProtocol().equalsIgnoreCase("file") && ServerStatus.BG("proxyKeepUploads")) {
          (new File(String.valueOf(ServerStatus.SG("proxyUploadRepository")) + this.thisSession.uiSG("user_name") + this.the_file_path)).mkdirs();
          this.proxy = new RandomAccessFile(String.valueOf(ServerStatus.SG("proxyUploadRepository")) + this.thisSession.uiSG("user_name") + this.the_file_path + this.proxySDF.format(new Date()) + "_" + this.the_file_name, "rw");
          if (this.proxy.length() > this.current_loc)
            this.proxy.setLength(this.current_loc); 
        } 
        long max_upload_size = this.thisSession.IG("max_upload_size");
        long max_upload_amount = this.thisSession.IG("max_upload_amount");
        long max_upload_amount_day = this.thisSession.IG("max_upload_amount_day");
        long max_upload_amount_month = this.thisSession.IG("max_upload_amount_month");
        long start_upload_amount_day = 0L;
        if (max_upload_amount_day > 0L)
          start_upload_amount_day = ServerStatus.thisObj.statTools.getTransferAmountToday(this.thisSession.uiSG("user_ip"), this.thisSession.uiSG("user_name"), this.thisSession.uiPG("stat"), "uploads", this.thisSession); 
        long start_upload_amount_month = 0L;
        if (max_upload_amount_month > 0L)
          start_upload_amount_month = ServerStatus.thisObj.statTools.getTransferAmountThisMonth(this.thisSession.uiSG("user_ip"), this.thisSession.uiSG("user_name"), this.thisSession.uiPG("stat"), "uploads", this.thisSession); 
        long start_transfer_time = (new Date()).getTime();
        if (max_upload_amount > 0L && this.thisSession.uiLG("bytes_received") > max_upload_amount * 1024L) {
          this.thisSession.not_done = this.thisSession.write_command("550-" + LOC.G("WARNING!!! Maximum upload amount reached.") + "  " + LOC.G("Received") + ":" + (this.thisSession.uiLG("bytes_received") / 1024L) + "k.  " + LOC.G("Max") + ":" + max_upload_amount + "k.  " + LOC.G("Available") + ":" + (max_upload_amount - this.thisSession.uiLG("bytes_received") / 1024L) + "k.");
          this.thisSession.not_done = this.thisSession.write_command("550", "%STOR-max reached%");
          this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
          this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
          ServerStatus.thisObj.runAlerts("user_upload_session", this.thisSession);
          if (this.thisSession.data_socks.size() > 0)
            ((Socket)this.thisSession.data_socks.remove(0)).close(); 
        } else if (max_upload_size > 0L && this.current_loc > max_upload_size * 1024L) {
          this.thisSession.not_done = this.thisSession.write_command("550-" + LOC.G("WARNING!!! Maximum upload amount file size reached.") + "  " + LOC.G("Max") + ":" + max_upload_size + "k.");
          this.thisSession.not_done = this.thisSession.write_command("550", "%STOR-max reached%");
          this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
          this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
          ServerStatus.thisObj.runAlerts("user_upload_session", this.thisSession);
          if (this.thisSession.data_socks.size() > 0)
            ((Socket)this.thisSession.data_socks.remove(0)).close(); 
        } else if ((max_upload_amount_day > 0L && start_upload_amount_day > max_upload_amount_day * 1024L) || (max_upload_amount_month > 0L && start_upload_amount_month > max_upload_amount_month * 1024L)) {
          if (max_upload_amount_day > 0L && start_upload_amount_day > max_upload_amount_day * 1024L) {
            this.stop_message = String.valueOf(LOC.G("WARNING!!! Maximum upload amount today reached.")) + "  " + LOC.G("Received") + ":" + (start_upload_amount_day / 1024L) + "k.  " + LOC.G("Max") + ":" + max_upload_amount_day + "k.  ";
            this.thisSession.not_done = this.thisSession.write_command("550-" + this.stop_message);
            ServerStatus.thisObj.runAlerts("user_upload_day", this.thisSession);
          } 
          if (max_upload_amount_month > 0L && start_upload_amount_month > max_upload_amount_month * 1024L) {
            this.stop_message = String.valueOf(LOC.G("WARNING!!! Maximum upload amount last 30 days reached.")) + "  " + LOC.G("Received") + ":" + (start_upload_amount_month / 1024L) + "k.  " + LOC.G("Max") + ":" + max_upload_amount_month + "k.  ";
            this.thisSession.not_done = this.thisSession.write_command("550-" + this.stop_message);
            ServerStatus.thisObj.runAlerts("user_upload_month", this.thisSession);
          } 
          this.thisSession.not_done = this.thisSession.write_command("550", "%STOR-max reached%");
          this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
          this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
          if (this.thisSession.data_socks.size() > 0)
            ((Socket)this.thisSession.data_socks.remove(0)).close(); 
        } else if (this.httpUpload && this.thisSession.uiLG("file_length") > (this.thisSession.IG("max_upload_size") * 1024) && this.thisSession.IG("max_upload_size") != 0) {
          this.thisSession.not_done = this.thisSession.write_command("550", "%STOR-max reached%");
          this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
          this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
          this.stop_message = String.valueOf(LOC.G("Upload file size is too large.")) + this.thisSession.IG("max_upload_size") + "k max.";
          if (this.thisSession.data_socks.size() > 0)
            ((Socket)this.thisSession.data_socks.remove(0)).close(); 
        } else if (this.current_loc == 0L && this.item.getProperty("privs").indexOf("(delete)") < 0 && stat != null && Long.parseLong(stat.getProperty("size")) > 0L && this.item.getProperty("privs").indexOf("(view)") >= 0) {
          this.thisSession.not_done = this.thisSession.write_command("550", "Cannot overwrite a file.");
          this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
          this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
          this.stop_message = LOC.G("Cannot overwrite a file.");
          if (this.thisSession.data_socks.size() > 0)
            ((Socket)this.thisSession.data_socks.remove(0)).close(); 
        } else {
          long resume_loc = this.current_loc;
          boolean binary_mode = this.thisSession.uiSG("file_transfer_mode").equals("BINARY");
          int loop_times = 0;
          while (this.thisSession.data_socks.size() == 0 && loop_times++ < 10000)
            Thread.sleep(1L); 
          if (this.thisSession.data_socks.size() == 0) {
            this.thisSession.not_done = this.thisSession.write_command("550", "%PORT-fail_question%" + CRLF + "%PORT-no_data_connection%");
            this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
            this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
          } else {
            this.data_sock = this.thisSession.data_socks.remove(0);
            if (this.data_sock != null)
              this.data_sock.setSoTimeout(((this.thisSession.IG("max_idle_time") == 0) ? 5 : this.thisSession.IG("max_idle_time")) * 1000 * 60); 
            if (this.thisSession.uiBG("modez")) {
              this.data_is = new InflaterInputStream(this.data_sock.getInputStream());
              this.zlibing = true;
            } else {
              this.data_is = new BufferedInputStream(this.data_sock.getInputStream());
            } 
            this.streamOpenStatus = "OPEN";
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
            if (!message_string.equals("")) {
              this.thisSession.not_done = this.thisSession.write_command(String.valueOf(responseNumber) + "-");
              this.thisSession.not_done = this.thisSession.write_command(String.valueOf(responseNumber) + "-" + message_string);
              this.thisSession.not_done = this.thisSession.write_command(String.valueOf(responseNumber) + "-");
            } 
            if (this.quota != -12345L)
              this.thisSession.not_done = this.thisSession.write_command(String.valueOf(responseNumber) + "-" + LOC.G("Quota space available") + ": " + Common.format_bytes_short(this.quota)); 
            if (this.thisSession.IG("max_download_amount") != 0)
              this.thisSession.not_done = this.thisSession.write_command(String.valueOf(responseNumber) + "-" + LOC.G("Max Download") + ".  " + LOC.G("Sent") + ":" + (this.thisSession.uiLG("bytes_sent") / 1024L) + "k.  " + LOC.G("Max") + ":" + this.thisSession.IG("max_download_amount") + "k.  " + LOC.G("Available") + ":" + (this.thisSession.IG("max_download_amount") - this.thisSession.uiLG("bytes_sent") / 1024L) + "k."); 
            if (this.thisSession.IG("max_upload_amount") != 0)
              this.thisSession.not_done = this.thisSession.write_command(String.valueOf(responseNumber) + "-" + LOC.G("Max Upload") + ".  " + LOC.G("Received") + ":" + (this.thisSession.uiLG("bytes_received") / 1024L) + "k.  " + LOC.G("Max") + ":" + this.thisSession.IG("max_upload_amount") + "k.  " + LOC.G("Available") + ":" + (this.thisSession.IG("max_upload_amount") - this.thisSession.uiLG("bytes_received") / 1024L) + "k."); 
            if (this.thisSession.IG("max_upload_size") != 0)
              this.thisSession.not_done = this.thisSession.write_command(String.valueOf(responseNumber) + "-" + LOC.G("Max Upload File Size: ") + max_upload_size + "k."); 
            if (this.thisSession.IG("ratio") != 0)
              this.thisSession.not_done = this.thisSession.write_command(String.valueOf(responseNumber) + "-" + LOC.G("Ratio is $0 to 1.", (new StringBuffer(String.valueOf(this.thisSession.IG("ratio")))).toString()) + " " + LOC.G("Received") + ":" + ((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) / 1024L) + "k " + LOC.G("Sent") + ":" + ((this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.  " + LOC.G("Available") + ":" + (((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) * this.thisSession.IG("ratio") - this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k."); 
            if (ServerStatus.BG("server_upload_queueing")) {
              long wait_time = (new Date()).getTime();
              int position = ServerStatus.thisObj.server_upload_queue.indexOf(this.thisSession) + 1;
              this.thisSession.not_done = this.thisSession.write_command(String.valueOf(responseNumber) + "-" + LOC.G("You are in a queue.  There are $0 downloads allowed with a maximum queue of size $1.", (new StringBuffer(String.valueOf(ServerStatus.IG("server_upload_queue_size")))).toString(), (new StringBuffer(String.valueOf(ServerStatus.IG("server_upload_queue_size_max")))).toString()));
              while (this.thisSession.uiBG("pause_now")) {
                this.thisSession.uiPPUT("bytes_received", 1L);
                this.thisSession.uiPPUT("bytes_sent", 1L);
                this.thisSession.not_done = this.thisSession.write_command(String.valueOf(responseNumber) + "-" + LOC.G("You are in currently in position $0 out of a queue of size $1.", (new StringBuffer(String.valueOf(ServerStatus.thisObj.server_upload_queue.indexOf(this.thisSession) + 1))).toString(), (new StringBuffer(String.valueOf(ServerStatus.thisObj.server_upload_queue.size()))).toString()));
                long cur_wait = (new Date()).getTime() - wait_time;
                String wait_formatted = String.valueOf(cur_wait / 1000L) + " " + LOC.G("seconds") + ".";
                if (cur_wait > 60000L)
                  wait_formatted = String.valueOf(cur_wait / 60000L) + " " + LOC.G("minutes") + ", " + ((cur_wait - cur_wait / 60000L * 60000L) / 1000L) + " " + LOC.G("seconds") + "."; 
                this.thisSession.not_done = this.thisSession.write_command(String.valueOf(responseNumber) + "-" + LOC.G("You have been waiting") + " " + wait_formatted);
                cur_wait = (ServerStatus.IG("last_upload_wait") * (ServerStatus.thisObj.server_upload_queue.indexOf(this.thisSession) + 1));
                wait_formatted = String.valueOf(cur_wait / 1000L) + " " + LOC.G("seconds") + ".";
                if (cur_wait > 60000L)
                  wait_formatted = String.valueOf(cur_wait / 60000L) + " " + LOC.G("minutes") + ", " + ((cur_wait - cur_wait / 60000L * 60000L) / 1000L) + " " + LOC.G("seconds") + "."; 
                this.thisSession.not_done = this.thisSession.write_command(String.valueOf(responseNumber) + "-" + LOC.G("Estimated wait is") + " " + wait_formatted);
                int iterations = 0;
                while (iterations++ < 60) {
                  Thread.sleep(1000L);
                  if (!this.thisSession.uiBG("pause_now"))
                    break; 
                } 
              } 
              ServerStatus.put_in("last_upload_wait", ((new Date()).getTime() - wait_time) / position);
            } 
            try {
              if (!responseNumber.equals("150")) {
                Thread.sleep(100L);
                throw new Exception(message_string);
              } 
              checkZipstream(this.item);
              if (this.current_loc == 0L && !this.random_access) {
                if ((this.item.getProperty("privs").indexOf("(view)") < 0 || this.unique) && stat != null)
                  try {
                    int fileNameInt = 1;
                    String itemName = this.item.getProperty("url");
                    String itemExt = "";
                    if (itemName.lastIndexOf(".") > 0 && (itemName.lastIndexOf(".") == itemName.length() - 4 || itemName.lastIndexOf(".") == itemName.length() - 5)) {
                      itemExt = itemName.substring(itemName.lastIndexOf("."));
                      itemName = itemName.substring(0, itemName.lastIndexOf("."));
                    } 
                    VRL vrl2 = vrl;
                    Properties stat2 = stat;
                    while (stat2 != null) {
                      fileNameInt++;
                      this.item.put("url", String.valueOf(itemName) + fileNameInt + itemExt);
                      this.item.put("name", this.the_file_name);
                      vrl2 = new VRL(this.item.getProperty("url"));
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
                } else if (vrl.getProtocol().equalsIgnoreCase("file") && stat != null) {
                  if (!this.thisSession.uiSG("proxy_mode").equalsIgnoreCase("socket")) {
                    if (!this.the_file_name.endsWith(".zipstream"))
                      Common.trackSyncRevision(new File(vrl.getCanonicalPath()), String.valueOf(this.the_file_path) + this.the_file_name, this.thisSession.SG("root_dir"), this.item.getProperty("privs"), true); 
                    this.c.delete(vrl.getPath());
                  } 
                } 
              } 
              int data_read = 0;
              byte[] temp_array = new byte[this.packet_size];
              if (!this.httpUpload)
                this.thisSession.uiPUT("file_length", "0"); 
              vrl = doTempUploadRename(vrl);
              if (!this.thisSession.uiSG("proxy_mode").equalsIgnoreCase("socket") && !this.zipstream) {
                this.out = this.c.upload(vrl.getPath(), this.current_loc, !this.random_access, binary_mode);
                if (!Common.System2.containsKey("crushftp.dmz.queue.sock"))
                  if (!this.thisSession.user.getProperty("filePublicEncryptionKey", "").equals("")) {
                    this.out = Common.getEncryptedStream(this.out, this.thisSession.user.getProperty("filePublicEncryptionKey", ""), this.current_loc);
                  } else if (ServerStatus.BG("fileEncryption") && !ServerStatus.SG("filePublicEncryptionKey").equals("")) {
                    this.out = Common.getEncryptedStream(this.out, ServerStatus.SG("filePublicEncryptionKey"), this.current_loc);
                  } else if (!this.thisSession.user.getProperty("fileEncryptionKey", "").equals("")) {
                    this.out = Common.getEncryptedStream(this.out, this.thisSession.user.getProperty("fileEncryptionKey", ""), this.current_loc);
                  } else if (ServerStatus.BG("fileEncryption")) {
                    this.out = Common.getEncryptedStream(this.out, ServerStatus.SG("fileEncryptionKey"), this.current_loc);
                  }  
              } 
              this.thisSession.not_done = this.thisSession.write_command(responseNumber, "%STOR-start% " + LOC.G("\"$0$1\") S T O R", this.thisSession.stripRoot(this.the_file_path), this.the_file_name));
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
                          while (this.this$0.active && bytesRead >= 0) {
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
                for (int x = 0; x < crs.length; x++) {
                  String[] parts = crs[x].split(";");
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
                          int i = Integer.parseInt(bytes[xx].split("=")[0].trim());
                          int v = Integer.parseInt(bytes[xx].split("=")[1].trim());
                          if (b[i] != v)
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
              Properties masterPostItem = null;
              while (data_read >= 0 && !this.quota_exceeded) {
                Date new_date = new Date();
                if (new_date.getTime() - speedometerCheckInterval > 10000L) {
                  lesserSpeed = reloadBandwidthLimits(false);
                  speedometerCheckInterval = new_date.getTime();
                } 
                if (lesserSpeed > 0 && !speedController.bandwidth_immune_server)
                  this.slow_transfer = speedController.getDelayAmount(data_read, this.startLoop, this.endLoop, temp_array.length, this.slow_transfer, lesserSpeed); 
                while (this.pause_transfer)
                  Thread.sleep(100L); 
                this.startLoop = new_date.getTime();
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
                    throw new Exception("HTTP Aborted");
                  } 
                  vrl = doTempUploadRenameDone(vrl);
                  try {
                    if (entry != null && entry.getTime() > 0L && !ServerStatus.BG("disable_mdtm_modifications"))
                      this.c.mdtm(vrl.getPath(), entry.getTime()); 
                  } catch (Exception e) {
                    Log.log("UPLOAD", 1, e);
                  } 
                  if (masterPostItem == null)
                    masterPostItem = findLastItem(this.httpUpload, this.thisSession); 
                  Properties properties1 = (Properties)masterPostItem.clone();
                  this.thisSession.uiVG("session_commands").addElement(properties1);
                  Properties item2 = (Properties)this.item.clone();
                  if (entry != null) {
                    item2.put("url", String.valueOf(Common.all_but_last(this.item.getProperty("url"))) + entry.getName());
                    String md5Str = (new BigInteger(1, this.md5.digest())).toString(16).toLowerCase();
                    String path2 = entry.getName();
                    if (path2.indexOf(":") >= 0)
                      path2 = path2.substring(0, path2.indexOf(":")); 
                    stat = this.c.stat(vrl.getPath());
                    String filePath = String.valueOf(this.the_file_path) + path2;
                    if (SG("temp_upload_ext").length() > 0 && filePath.endsWith(SG("temp_upload_ext")))
                      filePath = filePath.substring(0, filePath.length() - SG("temp_upload_ext").length()); 
                    Common.publishPendingSyncs(this.pendingSyncs);
                    Common.trackSync("CHANGE", filePath, null, entry.isDirectory(), Long.parseLong(stat.getProperty("size")), entry.getTime(), this.thisSession.SG("root_dir"), this.item.getProperty("privs"), this.thisSession.uiSG("clientid"), this.priorMd5);
                    finishedUpload(this.c, this.thisSession, item2, String.valueOf(this.the_file_path) + Common.all_but_last(entry.getName()), vrl.getName(), this.stop_message, this.quota_exceeded, this.quota, this.httpUpload, vrl, this.asciiLineCount, binary_mode, start_transfer_time, md5Str, properties1, 0L, false, this.realAction, this.metaInfo);
                    if (ServerStatus.BG("posix"))
                      setDefaultsPOSIX(item2, vrl); 
                    while (this.thisSession.uiVG("session_commands").size() > ServerStatus.IG("user_log_buffer"))
                      this.thisSession.uiVG("session_commands").removeElementAt(0); 
                    while (this.thisSession.uiSG("session_uploads").length() / 160 > ServerStatus.IG("user_log_buffer"))
                      this.thisSession.uiPUT("session_uploads", this.thisSession.uiSG("session_uploads").substring(160)); 
                  } 
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
                    } 
                    if (data_read < 0)
                      data_read = -1; 
                  } 
                  if (data_read >= 0) {
                    if (this.proxy_remote_out != null) {
                      this.proxy_remote_out.write(temp_array, 0, data_read);
                    } else if (!this.zipstream || entry != null) {
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
                            for (int i = 0; i < parts.length; i++) {
                              String part = parts[i].split("=")[0];
                              if (part.equalsIgnoreCase("REST")) {
                                rest = Long.parseLong(parts[i].split("=")[1]);
                                this.thisSession.add_log("[" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: *REST " + rest + "*", "STOR");
                              } else if (part.equalsIgnoreCase("RANGE")) {
                                this.random_access = true;
                                rest = Long.parseLong(parts[i].split("=")[1]);
                                this.thisSession.add_log("[" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: *REST " + rest + " (randomaccess)*", "STOR");
                              } 
                            } 
                          } 
                        } catch (NumberFormatException e) {
                          Log.log("UPLOAD", 2, e);
                        } 
                        path2 = path2.replace('\\', '/');
                        path2 = Common.dots(path2);
                        Properties zipItem = this.thisSession.uVFS.get_item_parent(String.valueOf(this.the_file_path) + path2);
                        if (zipItem == null) {
                          if (!this.thisSession.check_access_privs(this.the_dir, "MKD", this.item)) {
                            this.thisSession.add_log("[" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: *UNZIP:MKD " + this.thisSession.stripRoot(this.the_file_path) + path2 + " DENIED!  Make dir not allowed.*", "MKD");
                            throw new Exception("Not allowed to create folder.");
                          } 
                          String buildPath = "";
                          String[] parts = Common.all_but_last(String.valueOf(this.the_file_path) + path2).split("/");
                          for (int i = 0; i < parts.length; i++) {
                            buildPath = String.valueOf(buildPath) + parts[i] + "/";
                            zipItem = this.thisSession.uVFS.get_item(buildPath);
                            if (zipItem == null) {
                              this.thisSession.add_log("[" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: *UNZIP:MKD " + this.thisSession.stripRoot(this.the_file_path) + buildPath + "*", "MKD");
                              zipItem = this.thisSession.uVFS.get_item_parent(buildPath);
                              this.c.makedir((new VRL(zipItem.getProperty("url"))).getPath());
                              this.thisSession.setFolderPrivs(this.c, zipItem);
                            } 
                          } 
                          zipItem = this.thisSession.uVFS.get_item_parent(String.valueOf(this.the_file_path) + path2);
                        } 
                        String urlChange = zipItem.getProperty("url");
                        for (int x = 0; x < ServerStatus.SG("unsafe_filename_chars").length(); x++) {
                          char c = ServerStatus.SG("unsafe_filename_chars").charAt(x);
                          if (c != '/' && c != '\\' && c != ':')
                            urlChange = urlChange.replace(c, '_'); 
                        } 
                        zipItem.put("url", urlChange);
                        vrl = new VRL(zipItem.getProperty("url"));
                        this.priorMd5 = "";
                        this.thisSession.add_log("[" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: *UNZIP " + this.thisSession.stripRoot(this.the_file_path) + path2 + "*", "STOR");
                        if (entry.isDirectory() || entry.getName().equals("")) {
                          if (this.thisSession.check_access_privs(this.the_dir, "MKD", this.item)) {
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
                              String md5Str = (new BigInteger(1, this.md5.digest())).toString(16).toLowerCase();
                              long statSize = 0L;
                              if (stat != null)
                                statSize = Long.parseLong(stat.getProperty("size")); 
                              if (this.item.getProperty("privs").indexOf("(sync") >= 0) {
                                Common.trackSyncRevision(new File(vrl.getCanonicalPath()), String.valueOf(this.the_file_path) + path2, this.thisSession.SG("root_dir"), this.item.getProperty("privs"), true);
                                Common.trackPendingSync(this.pendingSyncs, "CHANGE", String.valueOf(this.the_file_path) + path2, null, entry.isDirectory(), statSize, entry.getTime(), this.thisSession.SG("root_dir"), this.item.getProperty("privs"), this.thisSession.uiSG("clientid"), this.priorMd5);
                              } 
                              this.c.delete(vrl.getPath());
                            } 
                            vrl = doTempUploadRename(vrl);
                            this.out = this.c.upload(vrl.getPath(), rest, !this.random_access, binary_mode);
                            if (!Common.System2.containsKey("crushftp.dmz.queue.sock"))
                              if (!this.thisSession.user.getProperty("filePublicEncryptionKey", "").equals("")) {
                                this.out = Common.getEncryptedStream(this.out, this.thisSession.user.getProperty("filePublicEncryptionKey", ""), this.current_loc);
                              } else if (ServerStatus.BG("fileEncryption") && !ServerStatus.SG("filePublicEncryptionKey").equals("")) {
                                this.out = Common.getEncryptedStream(this.out, ServerStatus.SG("filePublicEncryptionKey"), this.current_loc);
                              } else if (!this.thisSession.user.getProperty("fileEncryptionKey", "").equals("")) {
                                this.out = Common.getEncryptedStream(this.out, this.thisSession.user.getProperty("fileEncryptionKey", ""), this.current_loc);
                              } else if (ServerStatus.BG("fileEncryption")) {
                                this.out = Common.getEncryptedStream(this.out, ServerStatus.SG("fileEncryptionKey"), this.current_loc);
                              }  
                            this.the_file_name = vrl.getName();
                            this.thisSession.uiPUT("start_transfer_byte_amount", (new StringBuffer(String.valueOf(this.thisSession.uiLG("bytes_received")))).toString());
                          } 
                        } 
                      } 
                    } 
                    if (!vrl.getProtocol().equalsIgnoreCase("file") && this.proxy != null)
                      this.proxy.write(temp_array, 0, data_read); 
                    updateTransferStats(this.thisSession, data_read, this.httpUpload, temp_array, this.md5);
                    this.current_loc += ((data_read > 0) ? data_read : 0L);
                  } 
                } else {
                  data_read = -1;
                } 
                if (max_upload_amount > 0L && this.thisSession.uiLG("bytes_received") > max_upload_amount * 1024L) {
                  data_read = -1;
                  String msg = String.valueOf(LOC.G("WARNING!!! Maximum upload amount reached.")) + "  " + LOC.G("Received") + ":" + (this.thisSession.uiLG("bytes_received") / 1024L) + "k.  " + LOC.G("Max") + ":" + max_upload_amount + "k.  " + LOC.G("Available") + ":" + (max_upload_amount - this.thisSession.uiLG("bytes_received") / 1024L) + "k.";
                  this.thisSession.not_done = this.thisSession.write_command("226-" + msg);
                  this.thisSession.not_done = this.thisSession.write_command("226-%STOR-max reached%");
                  ServerStatus.thisObj.runAlerts("user_upload_session", this.thisSession);
                  if (ServerStatus.BG("delete_partial_uploads")) {
                    this.stop_message = msg;
                    throw new Exception(msg);
                  } 
                } else if (max_upload_size > 0L && this.current_loc > max_upload_size * 1024L) {
                  data_read = -1;
                  String msg = String.valueOf(LOC.G("WARNING!!! Maximum upload amount file size reached.")) + "  " + LOC.G("Max") + ":" + max_upload_size + "k.  ";
                  this.thisSession.not_done = this.thisSession.write_command("226-" + msg);
                  this.thisSession.not_done = this.thisSession.write_command("226-%STOR-max reached%");
                  ServerStatus.thisObj.runAlerts("user_upload_session", this.thisSession);
                  if (ServerStatus.BG("delete_partial_uploads")) {
                    this.stop_message = msg;
                    throw new Exception(msg);
                  } 
                } else if (max_upload_amount_day > 0L && this.thisSession.uiLG("bytes_received") + start_upload_amount_day > max_upload_amount_day * 1024L) {
                  data_read = -1;
                  String msg = String.valueOf(LOC.G("WARNING!!! Maximum upload amount today reached.")) + "  " + LOC.G("Received") + ":" + ((this.thisSession.uiLG("bytes_received") + start_upload_amount_day) / 1024L) + "k.  " + LOC.G("Max") + ":" + max_upload_amount_day + "k.  ";
                  this.thisSession.not_done = this.thisSession.write_command("226-" + msg);
                  this.thisSession.not_done = this.thisSession.write_command("226-%STOR-max reached%");
                  ServerStatus.thisObj.runAlerts("user_upload_day", this.thisSession);
                  if (ServerStatus.BG("delete_partial_uploads")) {
                    this.stop_message = msg;
                    throw new Exception(msg);
                  } 
                } else if (max_upload_amount_month > 0L && this.thisSession.uiLG("bytes_received") + start_upload_amount_month > max_upload_amount_month * 1024L) {
                  data_read = -1;
                  String msg = String.valueOf(LOC.G("WARNING!!! Maximum upload amount last 30 days reached.")) + "  " + LOC.G("Received") + ":" + ((this.thisSession.uiLG("bytes_received") + start_upload_amount_month) / 1024L) + "k.  " + LOC.G("Max") + ":" + max_upload_amount_month + "k.  ";
                  this.thisSession.not_done = this.thisSession.write_command("226-" + msg);
                  this.thisSession.not_done = this.thisSession.write_command("226-%STOR-max reached%");
                  ServerStatus.thisObj.runAlerts("user_upload_month", this.thisSession);
                  if (ServerStatus.BG("delete_partial_uploads")) {
                    this.stop_message = msg;
                    throw new Exception(msg);
                  } 
                } 
                this.endLoop = (new Date()).getTime();
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
                Log.log("UPLOAD", 1, "An error occurred during the storing of file:" + vrl);
                throw new Exception("HTTP Aborted");
              } 
              vrl = doTempUploadRenameDone(vrl);
              Properties fileItem = findLastItem(this.httpUpload, this.thisSession);
              if (this.zipstream) {
                this.stop_message = "";
                this.thisSession.not_done = this.thisSession.write_command("226", "%STOR-end% " + LOC.G("(\"$0$1\" $2) " + this.realAction, "", "", ""));
              } else {
                String md5Str = (new BigInteger(1, this.md5.digest())).toString(16).toLowerCase();
                if (this.httpUpload && !ServerStatus.BG("disable_mdtm_modifications"))
                  this.c.mdtm(vrl.getPath(), this.fileModifiedDate); 
                stat = this.c.stat(vrl.getPath());
                String filePath = String.valueOf(this.the_file_path) + this.the_file_name;
                if (SG("temp_upload_ext").length() > 0 && filePath.endsWith(SG("temp_upload_ext")))
                  filePath = filePath.substring(0, filePath.length() - SG("temp_upload_ext").length()); 
                Common.publishPendingSyncs(this.pendingSyncs);
                if (!this.the_file_name.endsWith(".zipstream") && stat != null)
                  Common.trackSync("CHANGE", filePath, null, false, Long.parseLong(stat.getProperty("size")), Long.parseLong(stat.getProperty("modified")), this.thisSession.SG("root_dir"), this.item.getProperty("privs"), this.thisSession.uiSG("clientid"), this.priorMd5); 
                this.stop_message = finishedUpload(this.c, this.thisSession, this.item, this.the_file_path, this.the_file_name, this.stop_message, this.quota_exceeded, this.quota, this.httpUpload, vrl, this.asciiLineCount, binary_mode, start_transfer_time, md5Str, fileItem, resume_loc, true, this.realAction, this.metaInfo);
              } 
              while (this.thisSession.uiVG("session_commands").size() > ServerStatus.IG("user_log_buffer"))
                this.thisSession.uiVG("session_commands").removeElementAt(0); 
              if (!this.stop_message.equals(""))
                this.inError = true; 
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
              try {
                this.thisSession.uiPUT("receiving_file", "false");
              } catch (Exception exception) {}
              try {
                this.thisSession.uiPUT("overall_transfer_speed", "0");
              } catch (Exception exception) {}
              try {
                this.thisSession.uiPUT("current_transfer_speed", "0");
              } catch (Exception exception) {}
              if (this.c != null)
                this.c.doCommand("ABORT"); 
              try {
                this.data_is.close();
              } catch (Exception exception) {}
              try {
                this.data_sock.close();
              } catch (Exception exception) {}
              if (this.data_sock != null)
                this.thisSession.old_data_socks.remove(this.data_sock); 
              try {
                if (this.out != null) {
                  this.out.close();
                  writeEncryptedHeaderSize(this.c, vrl, this.current_loc);
                } 
              } catch (Exception exception) {}
              stat = this.c.stat(vrl.getPath());
              try {
                if (this.allowTempExtensions && !SG("temp_upload_ext").equals("") && !SG("temp_upload_ext").equals("temp_upload_ext") && vrl.getName().endsWith(SG("temp_upload_ext"))) {
                  Log.log("UPLOAD", 2, "Temp uploaded file still exist:" + vrl);
                  if (ServerStatus.BG("delete_partial_uploads")) {
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
              } catch (Exception exception) {}
              this.out = null;
              if (this.thisSession.uVFS != null)
                this.c = this.thisSession.uVFS.releaseClient(this.c); 
              try {
                this.proxy.close();
              } catch (Exception exception) {}
              if (this.stop_message.equals(""))
                this.stop_message = LOC.G("Transfer failed") + "!"; 
              Common.publishPendingSyncs(this.pendingSyncs);
              if (!this.the_file_name.endsWith(".zipstream"))
                Common.trackSync("CHANGE", String.valueOf(this.the_file_path) + this.the_file_name, null, false, Long.parseLong(stat.getProperty("size")), Long.parseLong(stat.getProperty("modified")), this.thisSession.SG("root_dir"), this.item.getProperty("privs"), this.thisSession.uiSG("clientid"), ""); 
              Properties fileItem = this.thisSession.uiVG("session_commands").elementAt(this.thisSession.uiVG("session_commands").size() - 1);
              fileItem.put("the_command", this.realAction);
              fileItem.put("url", this.item.getProperty("url"));
              if (this.item.getProperty("url").endsWith(".zipstream"))
                fileItem.put("url", String.valueOf(Common.all_but_last(this.item.getProperty("url"))) + this.the_file_name); 
              fileItem.put("the_file_path", this.the_file_path);
              fileItem.put("the_file_name", this.the_file_name);
              fileItem.put("the_file_size", (new StringBuffer(String.valueOf(this.thisSession.uiLG("bytes_received") - this.thisSession.uiLG("start_transfer_byte_amount")))).toString());
              fileItem.put("the_file_speed", (new StringBuffer(String.valueOf(this.thisSession.uiLG("overall_transfer_speed")))).toString());
              fileItem.put("the_file_error", this.stop_message);
              fileItem.put("the_file_status", "FAILED");
              if (this.metaInfo != null)
                fileItem.put("metaInfo", this.metaInfo); 
              this.thisSession.uiPUT("session_uploads", String.valueOf(this.thisSession.uiSG("session_uploads")) + this.thisSession.stripRoot(this.the_file_path) + this.the_file_name + ":" + this.thisSession.uiLG("bytes_received") + LOC.G("bytes") + " @ " + this.thisSession.uiLG("overall_transfer_speed") + "k/sec. " + LOC.G("FAILED") + CRLF);
              if (this.thisSession.IG("ratio") != 0)
                this.thisSession.not_done = this.thisSession.write_command("550-" + LOC.G("Ratio is $0 to 1.", (new StringBuffer(String.valueOf(this.thisSession.IG("ratio")))).toString()) + " " + LOC.G("Received") + ":" + ((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) / 1024L) + "k " + LOC.G("Sent") + ":" + ((this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.  " + LOC.G("Available") + ":" + (((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) * this.thisSession.IG("ratio") - this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k."); 
              if (ServerStatus.BG("rfc_proxy") && e.getMessage() != null && e.getMessage().length() > 3 && e.getMessage().charAt(3) == ' ') {
                this.thisSession.not_done = this.thisSession.write_command(e.getMessage());
              } else {
                this.thisSession.not_done = this.thisSession.write_command("550", String.valueOf(e.getMessage()) + " " + LOC.G("(\"$0$1\") " + this.realAction, this.thisSession.stripRoot(this.the_file_path), this.the_file_name));
              } 
              this.thisSession.doErrorEvent(new Exception(this.thisSession.uiSG("lastLog")));
              if (this.the_file_name.indexOf(".DS_Store") < 0 && this.thisSession.uiLG("bytes_received") > 0L) {
                Properties lastUploadStat = ServerStatus.thisObj.statTools.add_upload_stat(this.thisSession, fileItem);
                if (this.metaInfo != null)
                  lastUploadStat.put("metaInfo", this.metaInfo); 
                this.thisSession.uiPUT("lastUploadStat", lastUploadStat);
                Vector lastUploadStats = this.thisSession.uiVG("lastUploadStats");
                lastUploadStats.addElement(this.thisSession.uiPG("lastUploadStat"));
                fileItem.put("uploadStats", this.thisSession.uiPG("lastUploadStat"));
                while (lastUploadStats.size() > ServerStatus.IG("user_log_buffer"))
                  lastUploadStats.removeElementAt(0); 
              } 
              this.thisSession.do_event5("UPLOAD", fileItem);
              this.thisSession.add_log("[" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: " + LOC.G("Error") + ":" + e, "STOR");
              this.thisSession.stop_idle_timer();
            } 
            if (ServerStatus.BG("posix"))
              setDefaultsPOSIX(this.item, vrl); 
            if (this.quota != -12345L)
              this.thisSession.set_quota(this.the_file_path, this.quota); 
            ServerStatus.thisObj.runAlerts("user_upload_session", this.thisSession);
            this.thisSession.uiPUT("receiving_file", "false");
          } 
        } 
      } 
      while (this.thisSession.uiSG("session_uploads").length() / 160 > ServerStatus.IG("user_log_buffer") && this.thisSession.uiSG("session_uploads").length() > 160)
        this.thisSession.uiPUT("session_uploads", this.thisSession.uiSG("session_uploads").substring(160)); 
      try {
        this.thisSession.uVFS.reset();
      } catch (Exception exception) {}
      try {
        this.thisSession.uiPUT("last_action", String.valueOf(this.thisSession.uiSG("last_action")) + "-" + LOC.G("Done."));
      } catch (Exception exception) {}
      try {
        if (this.out != null)
          this.out.close(); 
      } catch (Exception exception) {}
      try {
        if (this.c != null)
          this.c.close(); 
      } catch (Exception exception) {}
      if (this.thisSession.uVFS != null)
        this.c = this.thisSession.uVFS.releaseClient(this.c); 
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
      this.thisSession.start_idle_timer();
      this.streamOpenStatus = "CLOSED";
      this.zlibing = false;
      this.metaInfo = null;
    } catch (Exception e) {
      Log.log("UPLOAD", 1, e);
    } finally {
      this.thisThread = null;
    } 
    try {
      Thread.sleep(100L);
    } catch (Exception exception) {}
    kill();
  }
  
  public VRL doTempUploadRename(VRL vrl) throws Exception {
    if (this.allowTempExtensions && !SG("temp_upload_ext").equals("") && !SG("temp_upload_ext").equals("temp_upload_ext") && !vrl.getName().endsWith(SG("temp_upload_ext")) && Common.System2.get("crushftp.dmz.queue") == null) {
      String u = vrl.toString();
      if (u.endsWith("/"))
        u = u.substring(0, u.length() - 1); 
      VRL vrl2 = new VRL(String.valueOf(u) + SG("temp_upload_ext"));
      try {
        this.c.rename(vrl.getPath(), vrl2.getPath());
      } catch (Exception e) {
        Log.log("UPLOAD", 1, e);
      } 
      vrl = vrl2;
    } 
    return vrl;
  }
  
  public VRL doTempUploadRenameDone(VRL vrl) throws Exception {
    if (this.allowTempExtensions && !SG("temp_upload_ext").equals("") && !SG("temp_upload_ext").equals("temp_upload_ext") && vrl.getName().endsWith(SG("temp_upload_ext")) && !this.quota_exceeded) {
      String u = vrl.toString();
      if (u.endsWith("/"))
        u = u.substring(0, u.length() - 1); 
      VRL vrl2 = new VRL(u.substring(0, vrl.toString().length() - SG("temp_upload_ext").length()));
      this.c.rename(vrl.getPath(), vrl2.getPath());
      vrl = vrl2;
    } 
    return vrl;
  }
  
  public void writeEncryptedHeaderSize(GenericClient c, VRL vrl, long loc) throws Exception {
    if (!Common.System2.containsKey("crushftp.dmz.queue.sock"))
      if (!this.thisSession.user.getProperty("filePublicEncryptionKey", "").equals("") || ServerStatus.BG("fileEncryption")) {
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
    if (!vrl.getProtocol().equalsIgnoreCase("file"))
      return; 
    if (this.thisSession.uVFS != null)
      try {
        Properties parentItem = null;
        try {
          parentItem = this.thisSession.uVFS.get_fake_item(this.the_file_path, "FILE");
        } catch (Exception exception) {}
        if (item.getProperty("type").equalsIgnoreCase("DIR")) {
          this.thisSession.setFolderPrivs(this.c, item);
        } else {
          if (!SG("default_owner_command").equals("")) {
            this.c.setOwner(vrl.getPath(), ServerStatus.change_vars_to_values_static(SG("default_owner_command"), this.thisSession.user, this.thisSession.user_info, this.thisSession), "");
          } else if (parentItem.getProperty("protocol", "").equalsIgnoreCase("file") && !parentItem.getProperty("owner", "").trim().equals("") && !parentItem.getProperty("owner", "").trim().equals("user")) {
            try {
              this.c.setOwner(vrl.getPath(), parentItem.getProperty("owner", "").trim(), "");
            } catch (Exception exception) {}
          } 
          if (!SG("default_group_command").equals("")) {
            this.c.setGroup(vrl.getPath(), SG("default_group_command"), "");
          } else if (parentItem.getProperty("protocol", "").equalsIgnoreCase("file") && !parentItem.getProperty("group", "").trim().equals("") && !parentItem.getProperty("group", "").trim().equals("group")) {
            try {
              this.c.setGroup(vrl.getPath(), parentItem.getProperty("group", "").trim(), "");
            } catch (Exception exception) {}
          } 
          if (!SG("default_privs_command").equals("") && item.getProperty("type").equalsIgnoreCase("FILE")) {
            this.c.setMod(vrl.getPath(), SG("default_privs_command"), "");
          } else if (parentItem.getProperty("protocol", "").equalsIgnoreCase("file")) {
            try {
              if (!parentItem.getProperty("permissionsNum", "").equals(""))
                this.c.setMod(vrl.getPath(), parentItem.getProperty("permissionsNum", "").trim(), ""); 
            } catch (Exception exception) {}
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
    if (ServerStatus.BG("allow_zipstream") && Common.System2.get("crushftp.dmz.queue") == null && this.the_file_name.endsWith(".zipstream") && item.getProperty("privs").indexOf("(write)") >= 0) {
      this.data_is = (InputStream)new FileArchiveInputStream(new BufferedInputStream(this.data_is));
      this.zipstream = true;
    } else if (ServerStatus.BG("allow_zipstream") && Common.System2.get("crushftp.dmz.queue") == null && this.the_file_name.endsWith(".zipchunkstream") && item.getProperty("privs").indexOf("(write)") >= 0) {
      this.data_is = (InputStream)new FileArchiveInputStream(new BufferedInputStream(this.data_is));
      this.zipstream = true;
      this.zipchunkstream = true;
    } 
  }
  
  public void kill() {
    this.active = false;
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
  }
  
  public static String finishedUpload(GenericClient c, ServerSession thisSession, Properties item, String the_file_path, String the_file_name, String stop_message, boolean quota_exceeded, long quota, boolean httpUpload, VRL vrl, long asciiLineCount, boolean binary_mode, long start_transfer_time, String md5Str, Properties fileItem, long resume_loc, boolean writeMessages, String realAction, Properties metaInfo) throws Exception {
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
      fileItem.put("the_file_path", the_file_path);
      fileItem.put("the_file_name", the_file_name);
      fileItem.put("the_file_size", (new StringBuffer(String.valueOf(thisSession.uiLG("bytes_received") - thisSession.uiLG("start_transfer_byte_amount")))).toString());
      fileItem.put("the_file_speed", (new StringBuffer(String.valueOf(thisSession.uiLG("overall_transfer_speed")))).toString());
      fileItem.put("the_file_start", (new StringBuffer(String.valueOf(start_transfer_time))).toString());
      fileItem.put("the_file_end", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
      fileItem.put("the_file_error", LOC.G("QUOTA EXCEEDED"));
      fileItem.put("the_file_status", "FAILED");
      fileItem.put("the_file_resume_loc", (new StringBuffer(String.valueOf(resume_loc))).toString());
      fileItem.put("the_file_md5", md5Str);
      if (metaInfo != null)
        fileItem.put("metaInfo", metaInfo); 
      thisSession.uiPUT("session_uploads", String.valueOf(thisSession.uiSG("session_uploads")) + thisSession.stripRoot(the_file_path) + the_file_name + ":" + thisSession.uiLG("bytes_received") + LOC.G("bytes") + " @ " + thisSession.uiLG("overall_transfer_speed") + LOC.G("k/sec.") + " " + LOC.G("QUOTA EXCEEDED") + CRLF);
      stop_message = String.valueOf(LOC.G("Your quota has been exceeded")) + ".  " + LOC.G("Available") + ": 0k.";
      thisSession.not_done = thisSession.write_command("226-" + stop_message);
      if (writeMessages)
        thisSession.not_done = thisSession.write_command("226", "%STOR-quota exceeded% (\"" + thisSession.stripRoot(the_file_path) + the_file_name + "\") STOR"); 
      if (the_file_name.indexOf(".DS_Store") < 0 && thisSession.uiLG("bytes_received") > 0L) {
        Properties lastUploadStat = ServerStatus.thisObj.statTools.add_upload_stat(thisSession, fileItem);
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
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(vrl.getCanonicalPath()))));
        String data = "";
        SimpleDateFormat sdf_yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss");
        while ((data = br.readLine()) != null) {
          String modified = data.substring(0, data.indexOf(" "));
          String path = data.substring(data.indexOf(" ") + 1);
          if (!path.toUpperCase().startsWith(thisSession.SG("root_dir").toUpperCase()))
            path = String.valueOf(thisSession.SG("root_dir")) + path.substring(1); 
          boolean ok = false;
          ok = thisSession.check_access_privs(path, "STOR", item);
          if (ok) {
            VRL vrl2 = new VRL(thisSession.uVFS.get_item(path).getProperty("url"));
            if (!ServerStatus.BG("disable_mdtm_modifications"))
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
      fileItem.put("the_file_path", the_file_path);
      fileItem.put("the_file_name", the_file_name);
      fileItem.put("the_file_size", (new StringBuffer(String.valueOf(thisSession.uiLG("bytes_received") - thisSession.uiLG("start_transfer_byte_amount")))).toString());
      fileItem.put("the_file_speed", (new StringBuffer(String.valueOf(thisSession.uiLG("overall_transfer_speed")))).toString());
      fileItem.put("the_file_start", (new StringBuffer(String.valueOf(start_transfer_time))).toString());
      fileItem.put("the_file_end", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
      fileItem.put("the_file_error", fileItem.getProperty("the_file_error", ""));
      fileItem.put("the_file_status", "SUCCESS");
      fileItem.put("the_file_resume_loc", (new StringBuffer(String.valueOf(resume_loc))).toString());
      fileItem.put("the_file_md5", md5Str);
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
          thisSession.not_done = thisSession.write_command(String.valueOf(responseNumber) + "-" + LOC.G("Quota space available") + ": " + Common.format_bytes_short(quota)); 
        if (thisSession.IG("ratio") != 0)
          thisSession.not_done = thisSession.write_command(String.valueOf(responseNumber) + "-" + LOC.G("Ratio is $0 to 1.", (new StringBuffer(String.valueOf(thisSession.IG("ratio")))).toString()) + " " + LOC.G("Received") + ":" + ((thisSession.uiLG("bytes_received") + thisSession.uiLG("ratio_bytes_received")) / 1024L) + "k " + LOC.G("Sent") + ":" + ((thisSession.uiLG("bytes_sent") + thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.  " + LOC.G("Available") + ":" + (((thisSession.uiLG("bytes_received") + thisSession.uiLG("ratio_bytes_received")) * thisSession.IG("ratio") - thisSession.uiLG("bytes_sent") + thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k."); 
        if (!message_string.equals("") && ServerStatus.BG("log_transfer_speeds")) {
          thisSession.not_done = thisSession.write_command(String.valueOf(responseNumber) + "-");
          thisSession.not_done = thisSession.write_command(String.valueOf(responseNumber) + "-" + message_string);
          thisSession.not_done = thisSession.write_command(String.valueOf(responseNumber) + "-");
        } 
        if (!responseNumber.equals("226"))
          stop_message = message_string; 
        if (ServerStatus.BG("log_transfer_speeds"))
          thisSession.not_done = thisSession.write_command(String.valueOf(responseNumber) + "-" + LOC.G("Upload File Size:$0 bytes @ $1K/sec.", fileItem.getProperty("the_file_size"), fileItem.getProperty("the_file_speed")) + " MD5=" + md5Str); 
      } catch (IOException e) {
        Log.log("UPLOAD", 1, e);
      } 
      if (writeMessages && responseNumber.equals("226")) {
        thisSession.not_done = thisSession.write_command((new StringBuffer(String.valueOf(responseNumber))).toString(), "%STOR-end% " + LOC.G("(\"$0$1\" $2) " + realAction, thisSession.stripRoot(the_file_path), the_file_name, vrl.getProtocol().equalsIgnoreCase("file") ? (new StringBuffer(String.valueOf((new File(vrl.getCanonicalPath())).length()))).toString() : ""));
      } else if (writeMessages) {
        thisSession.not_done = thisSession.write_command((new StringBuffer(String.valueOf(responseNumber))).toString(), String.valueOf(stop_message) + " " + LOC.G("(\"$0$1\" $2) " + realAction, thisSession.stripRoot(the_file_path), the_file_name, vrl.getProtocol().equalsIgnoreCase("file") ? (new StringBuffer(String.valueOf((new File(vrl.getCanonicalPath())).length()))).toString() : ""));
      } 
      if (the_file_name.indexOf(".DS_Store") < 0 && (thisSession.uiLG("bytes_received") > 0L || ((httpUpload || ServerStatus.BG("event_empty_files")) && thisSession.uiLG("bytes_received") == 0L))) {
        Properties lastUploadStat = ServerStatus.thisObj.statTools.add_upload_stat(thisSession, fileItem);
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
            ServerSessionAJAX5_2.processKeywords(thisSession, new String[] { the_correct_path }, keywords); 
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
  
  public static Properties findLastItem(boolean httpUsed, ServerSession thisSession) {
    Properties fileItem = null;
    if (httpUsed) {
      int offset = 1;
      do {
        fileItem = thisSession.uiVG("session_commands").elementAt(thisSession.uiVG("session_commands").size() - offset);
        offset++;
      } while (thisSession.uiVG("session_commands").size() - offset >= 0 && 
        !fileItem.getProperty("the_command", "").startsWith("POST") && 
        !fileItem.getProperty("the_command", "").startsWith("GET") && 
        !fileItem.getProperty("the_command", "").startsWith("PUT"));
    } else {
      fileItem = thisSession.uiVG("session_commands").elementAt(thisSession.uiVG("session_commands").size() - 1);
    } 
    return fileItem;
  }
  
  public static void updateTransferStats(ServerSession thisSession, int data_read, boolean httpUpload, byte[] temp_array, MessageDigest md5) {
    SharedSession.find("crushftp.usernames.activity").put(thisSession.getId(), (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
    if (data_read < 0)
      return; 
    md5.update(temp_array, 0, data_read);
    thisSession.uiPPUT("bytes_received", data_read);
    thisSession.uiPUT("bytes_received_formatted", Common.format_bytes_short(Long.parseLong(thisSession.uiSG("bytes_received"))));
    ServerStatus.thisObj.total_server_bytes_received += data_read;
    if (!httpUpload)
      thisSession.uiPPUT("file_length", data_read); 
    if (thisSession.server_item.containsKey("bytes_received"))
      synchronized (thisSession.server_item) {
        thisSession.server_item.put("bytes_received", (new StringBuffer(String.valueOf(Long.parseLong(thisSession.server_item.getProperty("bytes_received", "0")) + data_read))).toString());
      }  
  }
}
