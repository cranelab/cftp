package crushftp.server;

import com.crushftp.client.Common;
import com.crushftp.client.File_S;
import com.crushftp.client.File_U;
import com.crushftp.client.VRL;
import com.crushftp.client.Worker;
import crushftp.gui.LOC;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.SessionCrush;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import javax.net.ssl.SSLSocket;

public class LIST_handler implements Runnable {
  public static Properties md5Hash = new Properties();
  
  public boolean die_now = false;
  
  public boolean active = false;
  
  public String stop_message = LOC.G("Transfer failed!");
  
  public Thread thisThread = null;
  
  String the_dir;
  
  boolean names_only = false;
  
  boolean zlibing = false;
  
  public OutputStream data_osw = null;
  
  DeflaterOutputStream data_osz = null;
  
  String CRLF = "\r\n";
  
  String message_data = "";
  
  String search_file = "";
  
  SessionCrush thisSession = null;
  
  boolean showListing = true;
  
  boolean fullPaths = false;
  
  Thread fullPathLister = null;
  
  public boolean justStreamListData = false;
  
  int scanDepth = 9999;
  
  long end1 = 0L;
  
  long end2 = 0L;
  
  Socket data_sock = null;
  
  boolean mlstFormat = false;
  
  boolean was_star = false;
  
  public void init_vars(String the_dir, boolean names_only, SessionCrush thisSession, String search_file, boolean showListing, boolean mlstFormat) {
    this.the_dir = the_dir;
    this.names_only = names_only;
    this.thisSession = thisSession;
    this.fullPaths = search_file.startsWith("-Q");
    if (search_file.startsWith("-"))
      search_file = ""; 
    this.search_file = search_file;
    this.showListing = showListing;
    this.mlstFormat = mlstFormat;
    this.was_star = this.search_file.equals("*");
    if (this.search_file.equals("*"))
      this.search_file = ""; 
    if (this.search_file.equals("."))
      this.search_file = ""; 
    this.scanDepth = 999;
  }
  
  public void run() {
    this.thisThread = Thread.currentThread();
    try {
      if (this.thisSession != null) {
        this.active = true;
        this.stop_message = "";
        try {
          this.message_data = "";
          String parentPath = this.thisSession.uVFS.getRootVFS(this.the_dir, -1);
          Properties dir_item = this.thisSession.uVFS.get_item(parentPath, -1);
          Properties check_item = null;
          if (this.the_dir.equals(this.thisSession.uVFS.getPrivPath(this.the_dir)))
            check_item = this.thisSession.uVFS.get_fake_item(this.the_dir, "DIR"); 
          if (dir_item.getProperty("protocol", "file").equals("file")) {
            File_U temp_dir = new File_U(String.valueOf((new VRL(check_item.getProperty("url"))).getPath()) + "/.message");
            if (temp_dir.exists()) {
              RandomAccessFile message_is = new RandomAccessFile(new File_U(temp_dir), "r");
              byte[] temp_array = new byte[(int)message_is.length()];
              message_is.readFully(temp_array);
              message_is.close();
              this.message_data = new String(temp_array);
            } 
          } 
          if (check_item != null && check_item.getProperty("privs").indexOf("(comment") >= 0) {
            String comment = ServerStatus.thisObj.change_vars_to_values(Common.url_decode(check_item.getProperty("privs").substring(check_item.getProperty("privs").indexOf("(comment") + 8, check_item.getProperty("privs").indexOf(")", check_item.getProperty("privs").indexOf("(comment")))), this.thisSession);
            this.message_data = String.valueOf(this.message_data) + comment.trim();
          } 
        } catch (Exception exception) {}
        if (this.message_data.length() > 0)
          this.message_data = String.valueOf(this.message_data.trim()) + "\r\n."; 
        if (this.search_file.startsWith("-"))
          this.search_file = ""; 
        int loop_times = 0;
        while (!this.justStreamListData && this.thisSession.data_socks.size() == 0 && loop_times++ < 200)
          Thread.sleep(100L); 
        if (!this.justStreamListData && this.thisSession.data_socks.size() == 0) {
          if (this.thisSession.uiBG("pasv_connect")) {
            this.thisSession.not_done = this.thisSession.ftp_write_command("550", LOC.G("No connection received on PASV ip:port that was specified in 20 seconds."));
          } else {
            this.thisSession.not_done = this.thisSession.ftp_write_command("550", "%PORT-fail_question%" + this.CRLF + "%PORT-no_data_connection%");
          } 
          this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
        } else {
          this.data_sock = this.thisSession.data_socks.remove(0);
          this.data_sock.setSoTimeout(((this.thisSession.IG("max_idle_time") <= 0) ? 5 : this.thisSession.IG("max_idle_time")) * 1000 * 60);
          long quota = this.thisSession.get_quota(this.the_dir);
          String responseCode226 = "226";
          try {
            if (!this.justStreamListData)
              this.data_osw = new BufferedOutputStream(this.data_sock.getOutputStream()); 
            if (this.thisSession.uiBG("modez")) {
              Deflater def = new Deflater();
              def.setLevel(Integer.parseInt(this.thisSession.uiSG("zlibLevel")));
              this.data_osz = new DeflaterOutputStream(this.data_osw, def);
              this.zlibing = true;
            } 
            boolean wrote150 = false;
            if (!this.names_only) {
              write150(quota);
              wrote150 = true;
            } 
            StringBuffer c = new StringBuffer();
            Vector listing = new Vector();
            Properties status = new Properties();
            status.put("done", "false");
            long start = (new Date()).getTime();
            if (this.fullPaths) {
              this.fullPathLister = new Thread(new Lister(this.thisSession, this.the_dir, listing, this.scanDepth, status));
              this.fullPathLister.start();
            } else if (ServerStatus.BG("listing_multithreaded")) {
              Runnable r = new Runnable(this, listing, status) {
                  final LIST_handler this$0;
                  
                  private final Vector val$listing;
                  
                  private final Properties val$status;
                  
                  public void run() {
                    try {
                      this.this$0.thisSession.uVFS.getListing(this.val$listing, this.this$0.the_dir);
                    } catch (Exception e) {
                      Log.log("LIST", 0, e);
                      this.this$0.stop_message = "FAILED:" + e.getMessage();
                    } 
                    this.this$0.end1 = (new Date()).getTime();
                    this.val$status.put("done", "true");
                  }
                };
              Worker.startWorker(r, String.valueOf(Thread.currentThread().getName()) + ":Listing...");
            } else {
              try {
                this.thisSession.uVFS.getListing(listing, this.the_dir);
              } catch (Exception e) {
                Log.log("LIST", 0, e);
                this.stop_message = "FAILED:" + e.getMessage();
              } 
              this.end1 = (new Date()).getTime();
              if (ServerStatus.BG("sort_listings"))
                Common.do_sort(listing, "name"); 
              status.put("done", "true");
            } 
            StringBuffer item_str = new StringBuffer();
            Properties name_hash = new Properties();
            Properties pp = new Properties();
            pp.put("listing", listing);
            this.thisSession.runPlugin("list", pp);
            int bufferCount = 0;
            long totalListingItems = 0L;
            boolean has_bytes = false;
            Pattern pattern = null;
            if (this.search_file.length() > 0)
              pattern = Common.getPattern(this.search_file, ServerStatus.BG("case_sensitive_list_search")); 
            if (this.data_sock != null && this.data_sock instanceof SSLSocket)
              if (ServerStatus.BG("send_dot_dot_list_secure")) {
                Properties dir_item = new Properties();
                dir_item.put("name", ".");
                dir_item.put("type", "DIR");
                dir_item.put("permissions", "drwxrwxrwx");
                dir_item.put("size", "0");
                dir_item.put("url", ".");
                dir_item.put("root_dir", ".");
                dir_item.put("sftp_path", ".");
                dir_item.put("link", "false");
                dir_item.put("num_items", "1");
                dir_item.put("owner", "user");
                dir_item.put("group", "group");
                dir_item.put("protocol", "file");
                dir_item.put("month", "Jan");
                dir_item.put("day", "01");
                dir_item.put("time_or_year", "1970");
                listing.insertElementAt(dir_item.clone(), 0);
                dir_item.put("name", "..");
                dir_item.put("url", ".");
                dir_item.put("root_dir", ".");
                dir_item.put("sftp_path", "..");
                listing.insertElementAt(dir_item.clone(), 0);
              }  
            while (status.getProperty("done", "false").equalsIgnoreCase("false") || listing.size() > 0) {
              while (status.getProperty("done", "false").equalsIgnoreCase("false") && listing.size() == 0)
                Thread.sleep(100L); 
              if (listing.size() == 0)
                break; 
              bufferCount++;
              Properties item = (Properties)listing.remove(0);
              if (item.getProperty("hide_smb", "").equals("true"))
                continue; 
              item_str.setLength(0);
              if (item == null)
                continue; 
              generateLineEntry(item, item_str, false, this.the_dir, this.fullPaths, this.thisSession, this.mlstFormat);
              if (this.fullPaths)
                this.thisSession.uiPUT("list_filetree_status", String.valueOf(totalListingItems) + ":" + listing.size() + ":" + item.getProperty("root_dir") + item.getProperty("name")); 
              if (this.fullPaths || name_hash.get(String.valueOf(item.getProperty("root_dir")) + item.getProperty("name")) == null) {
                if (!this.fullPaths)
                  name_hash.put(String.valueOf(item.getProperty("root_dir")) + item.getProperty("name"), "DONE"); 
                if (checkName(item, this.thisSession, this.fullPaths, false))
                  if (this.search_file.equals("") || this.search_file.equals("*") || this.search_file.endsWith("/")) {
                    if (this.names_only) {
                      String prefix = "";
                      if (ServerStatus.BG("include_ftp_nlst_path"))
                        if (this.search_file.endsWith("/") || this.was_star) {
                          String dir = item.getProperty("root_dir");
                          if (dir.startsWith(this.thisSession.SG("root_dir")))
                            dir = dir.substring(this.thisSession.SG("root_dir").length() - 1); 
                          prefix = dir.substring(1);
                          String tmp1 = this.thisSession.uiSG("the_command");
                          String tmp2 = this.thisSession.uiSG("the_command_data");
                          this.thisSession.uiPUT("the_command", "CWD");
                          this.thisSession.uiPUT("the_command_data", dir);
                          String tmp3 = this.thisSession.uiSG("dont_write");
                          this.thisSession.uiPUT("dont_write", "true");
                          this.thisSession.do_CWD();
                          this.thisSession.uiPUT("the_command", tmp1);
                          this.thisSession.uiPUT("the_command_data", tmp2);
                          this.thisSession.uiPUT("dont_write", tmp3);
                          prefix = "";
                        }  
                      if (this.thisSession.uiBG("list_zip_app") && item.getProperty("type", "").equals("DIR") && item.getProperty("name", "").toUpperCase().endsWith(".APP") && !this.the_dir.equals("/")) {
                        item.put("permissions", "-rwxrwxrwx");
                        item.put("name", String.valueOf(item.getProperty("name")) + ".zip");
                      } else if (this.thisSession.uiBG("list_zip_only") && item.getProperty("type", "").equals("DIR") && !this.the_dir.equals("/")) {
                        item.put("permissions", "-rwxrwxrwx");
                        item.put("name", String.valueOf(item.getProperty("name")) + ".zip");
                      } else if (this.thisSession.uiBG("list_zip_dir") && item.getProperty("type", "").equals("DIR") && !this.the_dir.equals("/")) {
                        item.put("permissions", "-rwxrwxrwx");
                        item.put("name", String.valueOf(item.getProperty("name")) + ".zip");
                        c.append(String.valueOf(item.getProperty("name")) + ((item.getProperty("type", "").equals("DIR") && !ServerStatus.BG("disable_dir_filter")) ? "/" : "") + this.CRLF);
                      } 
                      if (this.thisSession.uiBG("list_zip_file") && item.getProperty("type", "").equals("FILE") && !this.the_dir.equals("/"))
                        item.put("name", String.valueOf(item.getProperty("name")) + ".zip"); 
                      totalListingItems++;
                      c.append(String.valueOf(prefix) + item.getProperty("name") + ((item.getProperty("type", "").equals("DIR") && !ServerStatus.BG("disable_dir_filter")) ? "/" : "") + this.CRLF);
                    } else {
                      if (this.thisSession.uiBG("list_zip_app") && item.getProperty("type", "").equals("DIR") && item.getProperty("name", "").toUpperCase().endsWith(".APP") && !this.the_dir.equals("/")) {
                        item.put("permissions", "-rwxrwxrwx");
                        item.put("name", String.valueOf(item.getProperty("name")) + ".zip");
                        item_str.setLength(0);
                        generateLineEntry(item, item_str, true, this.the_dir, this.fullPaths, this.thisSession, this.mlstFormat);
                      } else if (this.thisSession.uiBG("list_zip_only") && item.getProperty("type", "").equals("DIR") && !this.the_dir.equals("/")) {
                        item.put("permissions", "-rwxrwxrwx");
                        item.put("name", String.valueOf(item.getProperty("name")) + ".zip");
                        item_str.setLength(0);
                        generateLineEntry(item, item_str, true, this.the_dir, this.fullPaths, this.thisSession, this.mlstFormat);
                      } else if (this.thisSession.uiBG("list_zip_dir") && item.getProperty("type", "").equals("DIR") && !this.the_dir.equals("/")) {
                        item.put("permissions", "-rwxrwxrwx");
                        item.put("name", String.valueOf(item.getProperty("name")) + ".zip");
                        generateLineEntry(item, item_str, true, this.the_dir, this.fullPaths, this.thisSession, this.mlstFormat);
                      } 
                      if (this.thisSession.uiBG("list_zip_file") && item.getProperty("type", "").equals("FILE") && !this.the_dir.equals("/")) {
                        item.put("name", String.valueOf(item.getProperty("name")) + ".zip");
                        item_str.setLength(0);
                        generateLineEntry(item, item_str, true, this.the_dir, this.fullPaths, this.thisSession, this.mlstFormat);
                      } 
                      totalListingItems++;
                      c.append(item_str.toString());
                    } 
                  } else if ((this.search_file.equals(item.getProperty("name", "")) && ServerStatus.BG("case_sensitive_list_search")) || (this.search_file.equalsIgnoreCase(item.getProperty("name", "")) && !ServerStatus.BG("case_sensitive_list_search"))) {
                    if (this.names_only) {
                      c.append(String.valueOf(item.getProperty("name")) + ((item.getProperty("type", "").equals("DIR") && !ServerStatus.BG("disable_dir_filter")) ? "/" : "") + this.CRLF);
                    } else {
                      c.append(item_str.toString());
                    } 
                    totalListingItems++;
                  } else if ((this.search_file.indexOf("*") >= 0 || this.search_file.indexOf("?") >= 0 || this.search_file.indexOf("$") >= 0) && Common.doFilter(pattern, item.getProperty("name", ""))) {
                    if (this.names_only) {
                      String prefix = "";
                      if (ServerStatus.BG("include_ftp_nlst_path")) {
                        String dir = item.getProperty("root_dir");
                        if (dir.startsWith(this.thisSession.SG("root_dir")))
                          dir = dir.substring(this.thisSession.SG("root_dir").length() - 1); 
                        prefix = dir.substring(1);
                        String tmp1 = this.thisSession.uiSG("the_command");
                        String tmp2 = this.thisSession.uiSG("the_command_data");
                        this.thisSession.uiPUT("the_command", "CWD");
                        this.thisSession.uiPUT("the_command_data", dir);
                        String tmp3 = this.thisSession.uiSG("dont_write");
                        this.thisSession.uiPUT("dont_write", "true");
                        this.thisSession.do_CWD();
                        this.thisSession.uiPUT("the_command", tmp1);
                        this.thisSession.uiPUT("the_command_data", tmp2);
                        this.thisSession.uiPUT("dont_write", tmp3);
                        prefix = "";
                      } 
                      c.append(String.valueOf(prefix) + item.getProperty("name") + ((item.getProperty("type", "").equals("DIR") && !ServerStatus.BG("disable_dir_filter")) ? "/" : "") + this.CRLF);
                    } else {
                      c.append(item_str.toString());
                    } 
                    totalListingItems++;
                  }  
              } 
              if (bufferCount > ServerStatus.IG("listing_buffer_count")) {
                if (!wrote150)
                  write150(quota); 
                wrote150 = true;
                bufferCount = 0;
                if (c.length() > 0) {
                  if (this.showListing) {
                    if (this.zlibing) {
                      this.data_osz.write((new String(c.toString())).getBytes(SG("char_encoding")));
                    } else {
                      this.data_osw.write(Common.normalize2(new String(c.toString())).getBytes(SG("char_encoding")));
                    } 
                    this.thisSession.uiPPUT("bytes_sent", c.length());
                    ServerStatus.thisObj.total_server_bytes_sent += c.length();
                    if (this.thisSession.server_item.containsKey("bytes_sent"))
                      synchronized (this.thisSession.server_item) {
                        this.thisSession.server_item.put("bytes_sent", (new StringBuffer(String.valueOf(Long.parseLong(this.thisSession.server_item.getProperty("bytes_sent", "0")) + c.length()))).toString());
                      }  
                    has_bytes = true;
                  } 
                  c.setLength(0);
                } 
              } 
            } 
            if (status.containsKey("stop_message"))
              this.stop_message = status.getProperty("stop_message"); 
            if (this.names_only && c.length() == 0 && !ServerStatus.BG("allow_nlst_empty")) {
              responseCode226 = "450";
              this.stop_message = "No such file or directory: ";
              throw new Exception(this.stop_message);
            } 
            if (!wrote150)
              write150(quota); 
            wrote150 = true;
            if (this.stop_message.toUpperCase().indexOf("FAILED") >= 0)
              throw new Exception(this.stop_message); 
            this.thisSession.add_log(new String(c.toString()), "DIR_LIST");
            if (this.fullPaths)
              this.thisSession.uiPUT("list_filetree_status", String.valueOf(totalListingItems) + ":Finished"); 
            if (c.length() > 0) {
              if (this.showListing) {
                if (this.zlibing) {
                  this.data_osz.write((new String(c.toString())).getBytes(SG("char_encoding")));
                } else {
                  this.data_osw.write(Common.normalize2(new String(c.toString())).getBytes(SG("char_encoding")));
                } 
                this.thisSession.uiPPUT("bytes_sent", c.length());
                ServerStatus.thisObj.total_server_bytes_sent += c.length();
                if (this.thisSession.server_item.containsKey("bytes_sent"))
                  synchronized (this.thisSession.server_item) {
                    this.thisSession.server_item.put("bytes_sent", (new StringBuffer(String.valueOf(Long.parseLong(this.thisSession.server_item.getProperty("bytes_sent", "0")) + c.length()))).toString());
                  }  
                has_bytes = true;
              } 
              c.setLength(0);
            } 
            if (!this.justStreamListData)
              try {
                if (!has_bytes && this.data_sock != null && this.data_sock instanceof SSLSocket) {
                  Common.debug(1, this.data_sock + ":" + "Forcing SSL handshake to start...");
                  Common.configureSSLTLSSocket(this.data_sock);
                  ((SSLSocket)this.data_sock).startHandshake();
                  Common.debug(1, this.data_sock + ":" + "Forced SSL handshake complete.");
                } 
              } catch (Exception e) {
                Log.log("SERVER", 1, e);
              }  
            if (this.zlibing) {
              this.data_osz.finish();
              this.data_osz.close();
            } else {
              this.data_osw.close();
            } 
            if (this.data_sock != null)
              this.data_sock.close(); 
            this.end2 = (new Date()).getTime();
            this.thisSession.uiPUT("listing_files", "false");
            if (this.data_sock != null)
              this.thisSession.old_data_socks.remove(this.data_sock); 
            if (!this.justStreamListData) {
              if (quota != -12345L && !ServerStatus.BG("hide_ftp_quota_log"))
                this.thisSession.not_done = this.thisSession.ftp_write_command("226-" + LOC.G("Quota space available") + ": " + Common.format_bytes_short(quota)); 
              if (this.thisSession.IG("max_download_amount") != 0)
                this.thisSession.not_done = this.thisSession.ftp_write_command("226-" + LOC.G("Max Download") + ".  " + LOC.G("Sent") + ":" + (this.thisSession.uiLG("bytes_sent") / 1024L) + "k.  " + LOC.G("Max") + ":" + this.thisSession.IG("max_download_amount") + "k.  " + LOC.G("Available") + ":" + (this.thisSession.IG("max_download_amount") - this.thisSession.uiLG("bytes_sent") / 1024L) + "k."); 
              if (this.thisSession.IG("ratio") != 0)
                this.thisSession.not_done = this.thisSession.ftp_write_command("226-" + LOC.G("Ratio is") + " " + this.thisSession.IG("ratio") + " to 1. " + LOC.G("Received") + ":" + ((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) / 1024L) + "k " + LOC.G("Sent") + ":" + ((this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.  " + LOC.G("Available") + ":" + (((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) * this.thisSession.IG("ratio") - this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k."); 
              if (ServerStatus.BG("generic_ftp_responses")) {
                this.thisSession.not_done = this.thisSession.ftp_write_command("226", "Directory send OK.");
              } else {
                this.thisSession.not_done = this.thisSession.ftp_write_command("226", "%LIST-end% " + LOC.G("(generate:$0ms)(send:$1ms)", (new StringBuffer(String.valueOf(this.end1 - start))).toString(), (new StringBuffer(String.valueOf(this.end2 - start))).toString()));
              } 
            } 
          } catch (Exception e) {
            Log.log("LIST", 1, e);
            if (quota != -12345L && !ServerStatus.BG("hide_ftp_quota_log"))
              this.thisSession.not_done = this.thisSession.ftp_write_command("226-" + LOC.G("Quota space available") + ": " + Common.format_bytes_short(quota)); 
            if (this.thisSession.IG("max_download_amount") != 0)
              this.thisSession.not_done = this.thisSession.ftp_write_command("226-" + LOC.G("Max Download") + ".  " + LOC.G("Sent") + ":" + (this.thisSession.uiLG("bytes_sent") / 1024L) + "k.  " + LOC.G("Max") + ":" + this.thisSession.IG("max_download_amount") + "k.  " + LOC.G("Available") + ":" + (this.thisSession.IG("max_download_amount") - this.thisSession.uiLG("bytes_sent") / 1024L) + "k."); 
            if (this.thisSession.IG("ratio") != 0)
              this.thisSession.not_done = this.thisSession.ftp_write_command("226-" + LOC.G("Ratio is") + " " + this.thisSession.IG("ratio") + " to 1. " + LOC.G("Received") + ":" + ((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) / 1024L) + "k " + LOC.G("Sent") + ":" + ((this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.  " + LOC.G("Available") + ":" + (((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) * this.thisSession.IG("ratio") - this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k."); 
            this.stop_message = String.valueOf(this.stop_message) + LOC.G("Transfer failed") + "!";
            responseCode226 = "550";
            this.thisSession.not_done = this.thisSession.ftp_write_command(responseCode226, this.stop_message);
          } 
        } 
        if (this.thisSession.ftp != null)
          this.thisSession.ftp.start_idle_timer(); 
      } 
      if (this.fullPathLister != null)
        try {
          this.fullPathLister.interrupt();
        } catch (Exception exception) {} 
      this.fullPathLister = null;
      try {
        this.thisSession.uiPUT("last_action", "LIST-Done.");
        if (!this.justStreamListData && this.data_sock != null)
          this.data_sock.close(); 
        this.thisSession.uiPUT("listing_files", "false");
      } catch (Exception exception) {}
      this.zlibing = false;
      this.active = false;
    } catch (Exception e) {
      Log.log("LIST", 1, e);
    } finally {
      this.thisThread = null;
    } 
    kill(null);
  }
  
  public void kill(Thread this_thread) {
    this.die_now = true;
    try {
      if (this.data_osw != null)
        this.data_osw.close(); 
    } catch (Exception exception) {}
    try {
      if (this.data_osz != null)
        this.data_osz.close(); 
    } catch (Exception exception) {}
    try {
      if (!this.justStreamListData && this.data_sock != null)
        this.data_sock.close(); 
    } catch (Exception exception) {}
    if (this.data_sock != null)
      this.thisSession.old_data_socks.remove(this.data_sock); 
  }
  
  public void write150(long quota) throws Exception {
    if (this.justStreamListData)
      return; 
    if (this.message_data.length() > 0)
      this.thisSession.not_done = this.thisSession.ftp_write_command(String.valueOf(ServerStatus.thisObj.common_code.format_message("150-", this.message_data)) + "150-" + this.CRLF); 
    if (quota != -12345L && !ServerStatus.BG("hide_ftp_quota_log"))
      this.thisSession.not_done = this.thisSession.ftp_write_command("150-" + LOC.G("Quota space available") + ": " + Common.format_bytes_short(quota)); 
    if (this.thisSession.IG("max_download_amount") != 0)
      this.thisSession.not_done = this.thisSession.ftp_write_command("150-" + LOC.G("Max Download") + ".  " + LOC.G("Sent") + ":" + (this.thisSession.uiLG("bytes_sent") / 1024L) + "k.  " + LOC.G("Max") + ":" + this.thisSession.IG("max_download_amount") + "k.  " + LOC.G("Available") + ":" + (this.thisSession.IG("max_download_amount") - this.thisSession.uiLG("bytes_sent") / 1024L) + "k."); 
    if (this.thisSession.IG("ratio") != 0)
      this.thisSession.not_done = this.thisSession.ftp_write_command("150-" + LOC.G("Ratio is") + " " + this.thisSession.IG("ratio") + " to 1. " + LOC.G("Received") + ":" + ((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) / 1024L) + "k " + LOC.G("Sent") + ":" + ((this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.  " + LOC.G("Available") + ":" + (((this.thisSession.uiLG("bytes_received") + this.thisSession.uiLG("ratio_bytes_received")) * this.thisSession.IG("ratio") - this.thisSession.uiLG("bytes_sent") + this.thisSession.uiLG("ratio_bytes_sent")) / 1024L) + "k."); 
    this.thisSession.not_done = this.thisSession.ftp_write_command("150", "%LIST-start%");
  }
  
  public static boolean checkName(Properties item, SessionCrush thisSession, boolean fullPaths, boolean ignoreFilter) throws Exception {
    return checkName(item, fullPaths, ignoreFilter, thisSession.SG("file_filter"), thisSession);
  }
  
  public static boolean checkName(Properties item, boolean fullPaths, boolean ignoreFilter, String file_filter, SessionCrush thisSession) throws Exception {
    if (item.getProperty("name").equals(".") || item.getProperty("name").equals(".."))
      return true; 
    String filter_str = String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + file_filter;
    if (thisSession != null && filter_str.indexOf("%") >= 0)
      filter_str = ServerStatus.thisObj.change_vars_to_values(filter_str, thisSession); 
    if (ignoreFilter || Common.filter_check("L", item.getProperty("name"), filter_str)) {
      if (Log.log("LIST", 2, ""))
        Log.log("LIST", 2, (String)VRL.safe(item)); 
      boolean is_single_root = (thisSession == null) ? false : (!thisSession.SG("root_dir").equals("/"));
      boolean is_root_dir = (thisSession == null) ? false : item.getProperty("root_dir", "").equals(thisSession.SG("root_dir"));
      boolean is_invisible = (item.getProperty("privs").toLowerCase().indexOf("(invisible)") >= 0);
      boolean is_inherited = (item.getProperty("privs").toLowerCase().indexOf("(inherited)") >= 0);
      if (fullPaths && (thisSession == null || !thisSession.check_access_privs(item.getProperty("root_dir"), "RETR", item)))
        return false; 
      if ((item.getProperty("privs", "").indexOf("(view)") >= 0 && item.getProperty("privs", "").indexOf("*") <= 0) || (item.getProperty("privs", "").indexOf("(view)") < 0 && !is_inherited) || (item.getProperty("privs", "").indexOf("(view)") >= 0 && !item.getProperty("name", "").equals("Icon\r")))
        if (!is_invisible || (is_invisible && is_inherited && (!is_single_root || !is_root_dir)))
          return true;  
    } 
    return false;
  }
  
  public static void generateLineEntry(Properties item, StringBuffer item_str, boolean makeupZipSize, String the_dir, boolean fullPaths, SessionCrush thisSession, boolean mlstFormat) {
    try {
      if (item.getProperty("type", "").equals("DIR") && makeupZipSize && item.getProperty("protocol").equalsIgnoreCase("file")) {
        Vector v = new Vector();
        Common.getAllFileListing(v, (new VRL(item.getProperty("url"))).getPath(), 20, false);
        long totalSize = 0L;
        for (int x = 0; x < v.size(); x++) {
          File_S f = v.elementAt(x);
          totalSize += f.length();
        } 
        item.put("size", (new StringBuffer(String.valueOf(totalSize))).toString());
      } else if (item.getProperty("type", "").equals("DIR") && thisSession.BG("dir_calc")) {
        Log.log("SERVER", 1, "Calculating directory size..." + the_dir);
        Vector inside_a_dir_list = new Vector();
        thisSession.uVFS.getListing(inside_a_dir_list, String.valueOf(the_dir) + item.getProperty("name") + "/");
        for (int xx = 0; xx < inside_a_dir_list.size(); xx++) {
          Properties adder = inside_a_dir_list.elementAt(xx);
          item.put("size", Long.parseLong(item.getProperty("size", "")) + Long.parseLong(adder.getProperty("size")));
        } 
        item.put("num_items", inside_a_dir_list.size());
      } else if (item.getProperty("type", "").equals("DIR") && thisSession.BG("dir_calc_count")) {
        Log.log("SERVER", 1, "Calculating directory count..." + the_dir);
        Vector inside_a_dir_list = new Vector();
        Properties status = new Properties();
        Worker.startWorker(new Runnable(thisSession, inside_a_dir_list, the_dir, item, status) {
              private final SessionCrush val$thisSession;
              
              private final Vector val$inside_a_dir_list;
              
              private final String val$the_dir;
              
              private final Properties val$item;
              
              private final Properties val$status;
              
              public void run() {
                try {
                  this.val$thisSession.uVFS.getListing(this.val$inside_a_dir_list, String.valueOf(this.val$the_dir) + this.val$item.getProperty("name") + "/", 1, 1000, true);
                } catch (Exception e) {
                  Log.log("SERVER", 1, e);
                } 
                this.val$status.put("done", "true");
              }
            });
        int count = 0;
        while (inside_a_dir_list.size() > 0 || !status.containsKey("done")) {
          if (inside_a_dir_list.size() > 0) {
            Properties p = inside_a_dir_list.remove(0);
            if (checkName(p, thisSession, fullPaths, false))
              count++; 
            continue;
          } 
          Thread.sleep(100L);
        } 
        item.put("num_items", count);
      } 
    } catch (Exception exception) {}
    if (thisSession.BG("dos_ftp_listing")) {
      Date d = new Date(Long.parseLong(item.getProperty("modified", "0")));
      d = new Date(d.getTime() + (long)(thisSession.DG("timezone_offset") * 1000.0D * 60.0D * 60.0D));
      SimpleDateFormat MM_dd_yy = new SimpleDateFormat("MM-dd-yy", Locale.US);
      SimpleDateFormat hh_mmaa = new SimpleDateFormat("hh:mmaa", Locale.US);
      item_str.append(MM_dd_yy.format(d));
      item_str.append("  ");
      item_str.append(hh_mmaa.format(d));
      if (item.getProperty("type", "").equals("DIR")) {
        item_str.append("       <DIR>          ");
      } else {
        item_str.append(("                     " + item.getProperty("size", "")).substring(20 - 20 - item.getProperty("size", "").length())).append(" ");
      } 
      item_str.append(String.valueOf(item.getProperty("name", "")) + "\r\n");
    } else if (!mlstFormat) {
      item_str.append(item.getProperty("permissions"));
      item_str.append(String.valueOf(Common.lpad(item.getProperty("num_items", "1"), 4)) + " ");
      if (fullPaths && (thisSession == null || !thisSession.server_item.getProperty("serverType", "http").equalsIgnoreCase("SFTP"))) {
        VRL vrl = new VRL(item.getProperty("url"));
        if (vrl.getProtocol().equalsIgnoreCase("file")) {
          File_S f = new File_S(vrl.getPath());
          if (f.isFile() && f.length() < 52428800L) {
            item.put("owner", "MD5");
            try {
              String md5str = "";
              if (md5Hash.containsKey(f.getCanonicalPath())) {
                Properties p = (Properties)md5Hash.get(f.getCanonicalPath());
                if (p.getProperty("modified").equals((new StringBuffer(String.valueOf(f.lastModified()))).toString()) && p.getProperty("size").equals((new StringBuffer(String.valueOf(f.length()))).toString()))
                  md5str = p.getProperty("md5"); 
              } 
              if (md5str.equals("")) {
                md5str = Common.getMD5(new FileInputStream(f)).substring(24);
                Properties p = new Properties();
                p.put("md5", md5str);
                p.put("size", (new StringBuffer(String.valueOf(f.length()))).toString());
                p.put("modified", (new StringBuffer(String.valueOf(f.lastModified()))).toString());
                md5Hash.put(f.getCanonicalPath(), p);
              } 
              item.put("group", md5str);
              if (md5Hash.size() > 3000)
                md5Hash.clear(); 
            } catch (Exception e) {
              Log.log("SERVER", 1, e);
              item.put("group", "0");
            } 
          } 
        } 
      } 
      item_str.append(String.valueOf(Common.rpad(item.getProperty("owner", ""), 8)) + " ");
      item_str.append(String.valueOf(Common.rpad(item.getProperty("group", ""), 8)) + " ");
      item_str.append(String.valueOf(Common.lpad(item.getProperty("size", ""), 13)) + " ");
      if (fullPaths) {
        Date d = new Date(Long.parseLong(item.getProperty("modified", "0")));
        d = new Date(d.getTime() + (long)(thisSession.DG("timezone_offset") * 1000.0D * 60.0D * 60.0D));
        item_str.append(String.valueOf(Common.lpad(thisSession.sdf_yyyyMMddHHmmss.format(d), 15)) + " ");
        item_str.append(String.valueOf(Common.lpad(item.getProperty("day", "").trim(), 2)) + " ");
        item_str.append(String.valueOf(Common.lpad(item.getProperty("time_or_year", ""), 5)) + " ");
        String dir = item.getProperty("root_dir");
        if (dir.startsWith(thisSession.SG("root_dir")))
          dir = dir.substring(thisSession.SG("root_dir").length() - 1); 
        item_str.append(String.valueOf(dir) + item.getProperty("name") + "\r\n");
      } else {
        item_str.append(String.valueOf(Common.lpad(item.getProperty("month", "").trim(), 3)) + " ");
        item_str.append(String.valueOf(Common.lpad(item.getProperty("day", "").trim(), 2)) + " ");
        item_str.append(String.valueOf(Common.lpad(item.getProperty("time_or_year", ""), 5)) + " ");
        item_str.append(String.valueOf(item.getProperty("name", "")) + "\r\n");
      } 
    } else {
      Date d = new Date(Long.parseLong(item.getProperty("modified", "0")));
      d = new Date((long)(d.getTime() + thisSession.DG("timezone_offset") * 1000.0D * 60.0D * 60.0D));
      String privs = item.getProperty("privs", "");
      String newprivs = "";
      newprivs = String.valueOf(newprivs) + ((privs.indexOf("(read)") >= 0) ? "r," : "");
      if (item.getProperty("type", "").equals("DIR")) {
        newprivs = String.valueOf(newprivs) + "e,l,";
        newprivs = String.valueOf(newprivs) + ((privs.indexOf("(makedir)") >= 0) ? "m," : "");
        newprivs = String.valueOf(newprivs) + ((privs.indexOf("(read)") >= 0) ? "c," : "");
        newprivs = String.valueOf(newprivs) + ((privs.indexOf("(deletedir)") >= 0) ? "d," : "");
        newprivs = String.valueOf(newprivs) + ((privs.indexOf("(rename)") >= 0) ? "f," : "");
        newprivs = String.valueOf(newprivs) + ((privs.indexOf("(delete)") >= 0) ? "p," : "");
      } else {
        newprivs = String.valueOf(newprivs) + ((privs.indexOf("(write)") >= 0) ? "w,a," : "");
        newprivs = String.valueOf(newprivs) + ((privs.indexOf("(delete)") >= 0) ? "d," : "");
        newprivs = String.valueOf(newprivs) + ((privs.indexOf("(rename)") >= 0) ? "f," : "");
      } 
      if (newprivs.length() > 0)
        newprivs = newprivs.substring(0, newprivs.length() - 1); 
      String[] mlst_format = thisSession.uiSG("mlst_format").split(";");
      for (int x = 0; x < mlst_format.length; x++) {
        if (mlst_format[x].equalsIgnoreCase("Type*")) {
          item_str.append("Type=").append(item.getProperty("type", "").toLowerCase()).append(";");
        } else if (mlst_format[x].equalsIgnoreCase("Modify*")) {
          item_str.append("Modify=").append(thisSession.sdf_yyyyMMddHHmmssGMT.format(d)).append(";");
        } else if (mlst_format[x].equalsIgnoreCase("Perm*")) {
          item_str.append("Perm=").append(newprivs).append(";");
        } else if (mlst_format[x].equalsIgnoreCase("Size*")) {
          item_str.append("Size=").append(item.getProperty("size")).append(";");
        } else if (mlst_format[x].equalsIgnoreCase("UNIX.owner*")) {
          item_str.append("UNIX.owner=").append(item.getProperty("owner").trim()).append(";");
        } else if (mlst_format[x].equalsIgnoreCase("UNIX.group*")) {
          item_str.append("UNIX.group=").append(item.getProperty("group").trim()).append(";");
        } 
      } 
      String dir = the_dir;
      if (dir.startsWith(thisSession.SG("root_dir")))
        dir = dir.substring(thisSession.SG("root_dir").length() - 1); 
      item_str.append(" ").append(item.getProperty("name"));
      item_str.append("\r\n");
    } 
  }
  
  public String SG(String data) {
    return this.thisSession.SG(data);
  }
}
