package crushftp.server;

import com.crushftp.client.Common;
import com.crushftp.client.VRL;
import crushftp.gui.LOC;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

class LIST_handler implements Runnable {
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
  
  ServerSession theSession = null;
  
  boolean showListing = true;
  
  boolean fullPaths = false;
  
  Thread fullPathLister = null;
  
  public boolean justStreamListData = false;
  
  int scanDepth = 9999;
  
  long end1 = 0L;
  
  long end2 = 0L;
  
  Socket data_sock = null;
  
  boolean mlstFormat = false;
  
  public void init_vars(String the_dir, boolean names_only, ServerSession theSession, String search_file, boolean showListing, boolean mlstFormat) {
    this.the_dir = the_dir;
    this.names_only = names_only;
    this.theSession = theSession;
    this.fullPaths = search_file.startsWith("-Q");
    if (search_file.startsWith("-"))
      search_file = ""; 
    this.search_file = search_file;
    this.showListing = showListing;
    this.mlstFormat = mlstFormat;
    if (this.search_file.equals("*"))
      this.search_file = ""; 
    if (this.search_file.equals("."))
      this.search_file = ""; 
    this.scanDepth = 999;
  }
  
  public void run() {
    this.thisThread = Thread.currentThread();
    try {
      if (this.theSession != null) {
        this.active = true;
        this.stop_message = "";
        try {
          this.message_data = "";
          String parentPath = this.theSession.uVFS.getRootVFS(this.the_dir, -1);
          Properties dir_item = this.theSession.uVFS.get_item(parentPath, -1);
          Properties check_item = this.theSession.uVFS.get_fake_item(this.the_dir, "DIR");
          if (dir_item.getProperty("protocol", "file").equals("file")) {
            File temp_dir = new File(String.valueOf((new VRL(check_item.getProperty("url"))).getPath()) + "/.message");
            if (temp_dir.exists()) {
              RandomAccessFile message_is = new RandomAccessFile(temp_dir.getAbsolutePath(), "r");
              byte[] temp_array = new byte[(int)message_is.length()];
              message_is.readFully(temp_array);
              message_is.close();
              this.message_data = new String(temp_array);
            } 
          } 
          if (check_item.getProperty("privs").indexOf("(comment") >= 0) {
            String comment = ServerStatus.thisObj.change_vars_to_values(Common.url_decode(check_item.getProperty("privs").substring(check_item.getProperty("privs").indexOf("(comment") + 8, check_item.getProperty("privs").indexOf(")", check_item.getProperty("privs").indexOf("(comment")))), this.theSession);
            this.message_data = String.valueOf(this.message_data) + comment.trim();
          } 
        } catch (Exception exception) {}
        if (this.message_data.length() > 0)
          this.message_data = String.valueOf(this.message_data.trim()) + "\r\n."; 
        if (this.search_file.startsWith("-"))
          this.search_file = ""; 
        int loop_times = 0;
        while (!this.justStreamListData && this.theSession.data_socks.size() == 0 && loop_times++ < 200)
          Thread.sleep(100L); 
        if (!this.justStreamListData && this.theSession.data_socks.size() == 0) {
          if (this.theSession.uiBG("pasv_connect")) {
            this.theSession.not_done = this.theSession.write_command("550", LOC.G("No connection received on PASV ip:port that was specified in 20 seconds."));
          } else {
            this.theSession.not_done = this.theSession.write_command("550", "%PORT-fail_question%" + this.CRLF + "%PORT-no_data_connection%");
          } 
          this.theSession.uiVG("failed_commands").addElement((new Date()).getTime());
        } else {
          this.data_sock = this.theSession.data_socks.remove(0);
          this.data_sock.setSoTimeout(((this.theSession.IG("max_idle_time") == 0) ? 5 : this.theSession.IG("max_idle_time")) * 1000 * 60);
          long quota = this.theSession.get_quota(this.the_dir);
          String responseCode226 = "226";
          try {
            if (!this.justStreamListData)
              this.data_osw = new BufferedOutputStream(this.data_sock.getOutputStream()); 
            if (this.theSession.uiBG("modez")) {
              Deflater def = new Deflater();
              def.setLevel(Integer.parseInt(this.theSession.uiSG("zlibLevel")));
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
              this.fullPathLister = new Thread(new Lister(this.theSession, this.the_dir, listing, this.scanDepth, status));
              this.fullPathLister.start();
            } else if (ServerStatus.BG("listing_multithreaded")) {
              Runnable r = new Runnable(this, listing, status) {
                  final LIST_handler this$0;
                  
                  private final Vector val$listing;
                  
                  private final Properties val$status;
                  
                  public void run() {
                    try {
                      this.this$0.theSession.uVFS.getListing(this.val$listing, this.this$0.the_dir);
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
                this.theSession.uVFS.getListing(listing, this.the_dir);
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
            this.theSession.runPlugin("list", pp);
            int bufferCount = 0;
            long totalListingItems = 0L;
            Pattern pattern = null;
            if (this.search_file.length() > 0)
              pattern = Common.getPattern(this.search_file, ServerStatus.BG("case_sensitive_list_search")); 
            while (status.getProperty("done", "false").equalsIgnoreCase("false") || listing.size() > 0) {
              while (status.getProperty("done", "false").equalsIgnoreCase("false") && listing.size() == 0)
                Thread.sleep(100L); 
              if (listing.size() == 0)
                break; 
              totalListingItems++;
              bufferCount++;
              Properties item = listing.remove(0);
              item_str.setLength(0);
              if (item == null)
                continue; 
              generateLineEntry(item, item_str, false, this.the_dir, this.fullPaths, this.theSession, this.mlstFormat);
              if (this.fullPaths)
                this.theSession.uiPUT("list_filetree_status", String.valueOf(totalListingItems) + ":" + listing.size() + ":" + item.getProperty("root_dir") + item.getProperty("name")); 
              if (this.fullPaths || name_hash.get(String.valueOf(item.getProperty("root_dir")) + item.getProperty("name")) == null) {
                if (!this.fullPaths)
                  name_hash.put(String.valueOf(item.getProperty("root_dir")) + item.getProperty("name"), "DONE"); 
                if (checkName(item, this.theSession, this.fullPaths, false))
                  if (this.search_file.equals("") || this.search_file.equals("*") || this.search_file.endsWith("/")) {
                    if (this.names_only) {
                      if (this.theSession.uiBG("list_zip_app") && item.getProperty("type", "").equals("DIR") && item.getProperty("name", "").toUpperCase().endsWith(".APP") && !this.the_dir.equals("/")) {
                        item.put("permissions", "-rwxrwxrwx");
                        item.put("name", String.valueOf(item.getProperty("name")) + ".zip");
                      } else if (this.theSession.uiBG("list_zip_only") && item.getProperty("type", "").equals("DIR") && !this.the_dir.equals("/")) {
                        item.put("permissions", "-rwxrwxrwx");
                        item.put("name", String.valueOf(item.getProperty("name")) + ".zip");
                      } else if (this.theSession.uiBG("list_zip_dir") && item.getProperty("type", "").equals("DIR") && !this.the_dir.equals("/")) {
                        item.put("permissions", "-rwxrwxrwx");
                        item.put("name", String.valueOf(item.getProperty("name")) + ".zip");
                        c.append(String.valueOf(item.getProperty("name")) + ((item.getProperty("type", "").equals("DIR") && !ServerStatus.BG("disable_dir_filter")) ? "/" : "") + this.CRLF);
                      } 
                      if (this.theSession.uiBG("list_zip_file") && item.getProperty("type", "").equals("FILE") && !this.the_dir.equals("/"))
                        item.put("name", String.valueOf(item.getProperty("name")) + ".zip"); 
                      c.append(String.valueOf(item.getProperty("name")) + ((item.getProperty("type", "").equals("DIR") && !ServerStatus.BG("disable_dir_filter")) ? "/" : "") + this.CRLF);
                    } else {
                      if (this.theSession.uiBG("list_zip_app") && item.getProperty("type", "").equals("DIR") && item.getProperty("name", "").toUpperCase().endsWith(".APP") && !this.the_dir.equals("/")) {
                        item.put("permissions", "-rwxrwxrwx");
                        item.put("name", String.valueOf(item.getProperty("name")) + ".zip");
                        item_str.setLength(0);
                        generateLineEntry(item, item_str, true, this.the_dir, this.fullPaths, this.theSession, this.mlstFormat);
                      } else if (this.theSession.uiBG("list_zip_only") && item.getProperty("type", "").equals("DIR") && !this.the_dir.equals("/")) {
                        item.put("permissions", "-rwxrwxrwx");
                        item.put("name", String.valueOf(item.getProperty("name")) + ".zip");
                        item_str.setLength(0);
                        generateLineEntry(item, item_str, true, this.the_dir, this.fullPaths, this.theSession, this.mlstFormat);
                      } else if (this.theSession.uiBG("list_zip_dir") && item.getProperty("type", "").equals("DIR") && !this.the_dir.equals("/")) {
                        item.put("permissions", "-rwxrwxrwx");
                        item.put("name", String.valueOf(item.getProperty("name")) + ".zip");
                        generateLineEntry(item, item_str, true, this.the_dir, this.fullPaths, this.theSession, this.mlstFormat);
                      } 
                      if (this.theSession.uiBG("list_zip_file") && item.getProperty("type", "").equals("FILE") && !this.the_dir.equals("/")) {
                        item.put("name", String.valueOf(item.getProperty("name")) + ".zip");
                        item_str.setLength(0);
                        generateLineEntry(item, item_str, true, this.the_dir, this.fullPaths, this.theSession, this.mlstFormat);
                      } 
                      c.append(item_str.toString());
                    } 
                  } else if ((this.search_file.equals(item.getProperty("name", "")) && ServerStatus.BG("case_sensitive_list_search")) || (this.search_file.equalsIgnoreCase(item.getProperty("name", "")) && !ServerStatus.BG("case_sensitive_list_search"))) {
                    if (this.names_only) {
                      c.append(String.valueOf(item.getProperty("name")) + ((item.getProperty("type", "").equals("DIR") && !ServerStatus.BG("disable_dir_filter")) ? "/" : "") + this.CRLF);
                    } else {
                      c.append(item_str.toString());
                    } 
                  } else if ((this.search_file.indexOf("*") >= 0 || this.search_file.indexOf("?") >= 0 || this.search_file.indexOf("$") >= 0) && Common.doFilter(pattern, item.getProperty("name", ""))) {
                    if (this.names_only) {
                      c.append(String.valueOf(item.getProperty("name")) + ((item.getProperty("type", "").equals("DIR") && !ServerStatus.BG("disable_dir_filter")) ? "/" : "") + this.CRLF);
                    } else {
                      c.append(item_str.toString());
                    } 
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
                    this.theSession.uiPPUT("bytes_sent", c.length());
                    ServerStatus.thisObj.total_server_bytes_sent += c.length();
                    if (this.theSession.server_item.containsKey("bytes_sent"))
                      synchronized (this.theSession.server_item) {
                        this.theSession.server_item.put("bytes_sent", (new StringBuffer(String.valueOf(Long.parseLong(this.theSession.server_item.getProperty("bytes_sent", "0")) + c.length()))).toString());
                      }  
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
            this.theSession.add_log(new String(c.toString()), "DIR_LIST");
            if (this.fullPaths)
              this.theSession.uiPUT("list_filetree_status", String.valueOf(totalListingItems) + ":Finished"); 
            if (c.length() > 0) {
              if (this.showListing) {
                if (this.zlibing) {
                  this.data_osz.write((new String(c.toString())).getBytes(SG("char_encoding")));
                } else {
                  this.data_osw.write(Common.normalize2(new String(c.toString())).getBytes(SG("char_encoding")));
                } 
                this.theSession.uiPPUT("bytes_sent", c.length());
                ServerStatus.thisObj.total_server_bytes_sent += c.length();
                if (this.theSession.server_item.containsKey("bytes_sent"))
                  synchronized (this.theSession.server_item) {
                    this.theSession.server_item.put("bytes_sent", (new StringBuffer(String.valueOf(Long.parseLong(this.theSession.server_item.getProperty("bytes_sent", "0")) + c.length()))).toString());
                  }  
              } 
              c.setLength(0);
            } 
            if (totalListingItems == 0L && this.theSession.uiBG("dataSecure")) {
              c.setLength(0);
              c.append("total 0" + this.CRLF);
              if (this.zlibing) {
                this.data_osz.write((new String(c.toString())).getBytes(SG("char_encoding")));
              } else {
                this.data_osw.write(Common.normalize2(new String(c.toString())).getBytes(SG("char_encoding")));
              } 
            } 
            if (this.zlibing) {
              this.data_osz.finish();
              this.data_osz.close();
            } else {
              this.data_osw.close();
            } 
            this.end2 = (new Date()).getTime();
            this.theSession.uiPUT("listing_files", "false");
            if (!this.justStreamListData)
              try {
                this.data_sock.close();
              } catch (Exception exception) {} 
            if (this.data_sock != null)
              this.theSession.old_data_socks.remove(this.data_sock); 
            if (!this.justStreamListData) {
              if (quota != -12345L)
                this.theSession.not_done = this.theSession.write_command("226-" + LOC.G("Quota space available") + ": " + Common.format_bytes_short(quota)); 
              if (this.theSession.IG("max_download_amount") != 0)
                this.theSession.not_done = this.theSession.write_command("226-" + LOC.G("Max Download") + ".  " + LOC.G("Sent") + ":" + (this.theSession.uiLG("bytes_sent") / 1024L) + "k.  " + LOC.G("Max") + ":" + this.theSession.IG("max_download_amount") + "k.  " + LOC.G("Available") + ":" + (this.theSession.IG("max_download_amount") - this.theSession.uiLG("bytes_sent") / 1024L) + "k."); 
              if (this.theSession.IG("ratio") != 0)
                this.theSession.not_done = this.theSession.write_command("226-" + LOC.G("Ratio is") + " " + this.theSession.IG("ratio") + " to 1. " + LOC.G("Received") + ":" + ((this.theSession.uiLG("bytes_received") + this.theSession.uiLG("ratio_bytes_received")) / 1024L) + "k " + LOC.G("Sent") + ":" + ((this.theSession.uiLG("bytes_sent") + this.theSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.  " + LOC.G("Available") + ":" + (((this.theSession.uiLG("bytes_received") + this.theSession.uiLG("ratio_bytes_received")) * this.theSession.IG("ratio") - this.theSession.uiLG("bytes_sent") + this.theSession.uiLG("ratio_bytes_sent")) / 1024L) + "k."); 
              this.theSession.not_done = this.theSession.write_command("226", "%LIST-end% " + LOC.G("(generate:$0ms)(send:$1ms)", (new StringBuffer(String.valueOf(this.end1 - start))).toString(), (new StringBuffer(String.valueOf(this.end2 - start))).toString()));
            } 
          } catch (Exception e) {
            Log.log("LIST", 1, e);
            if (quota != -12345L)
              this.theSession.not_done = this.theSession.write_command("226-" + LOC.G("Quota space available") + ": " + Common.format_bytes_short(quota)); 
            if (this.theSession.IG("max_download_amount") != 0)
              this.theSession.not_done = this.theSession.write_command("226-" + LOC.G("Max Download") + ".  " + LOC.G("Sent") + ":" + (this.theSession.uiLG("bytes_sent") / 1024L) + "k.  " + LOC.G("Max") + ":" + this.theSession.IG("max_download_amount") + "k.  " + LOC.G("Available") + ":" + (this.theSession.IG("max_download_amount") - this.theSession.uiLG("bytes_sent") / 1024L) + "k."); 
            if (this.theSession.IG("ratio") != 0)
              this.theSession.not_done = this.theSession.write_command("226-" + LOC.G("Ratio is") + " " + this.theSession.IG("ratio") + " to 1. " + LOC.G("Received") + ":" + ((this.theSession.uiLG("bytes_received") + this.theSession.uiLG("ratio_bytes_received")) / 1024L) + "k " + LOC.G("Sent") + ":" + ((this.theSession.uiLG("bytes_sent") + this.theSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.  " + LOC.G("Available") + ":" + (((this.theSession.uiLG("bytes_received") + this.theSession.uiLG("ratio_bytes_received")) * this.theSession.IG("ratio") - this.theSession.uiLG("bytes_sent") + this.theSession.uiLG("ratio_bytes_sent")) / 1024L) + "k."); 
            this.stop_message = String.valueOf(this.stop_message) + LOC.G("Transfer failed") + "!";
            this.theSession.not_done = this.theSession.write_command(responseCode226, this.stop_message);
          } 
        } 
        this.theSession.start_idle_timer();
      } 
      if (this.fullPathLister != null)
        try {
          this.fullPathLister.interrupt();
        } catch (Exception exception) {} 
      this.fullPathLister = null;
      try {
        this.theSession.uiPUT("last_action", String.valueOf(this.theSession.uiSG("last_action")) + "-Done.");
        if (!this.justStreamListData && this.data_sock != null)
          this.data_sock.close(); 
        this.theSession.uiPUT("listing_files", "false");
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
      this.theSession.old_data_socks.remove(this.data_sock); 
  }
  
  public void write150(long quota) throws Exception {
    if (this.justStreamListData)
      return; 
    if (this.message_data.length() > 0)
      this.theSession.not_done = this.theSession.write_command(String.valueOf(ServerStatus.thisObj.common_code.format_message("150-", this.message_data)) + "150-" + this.CRLF); 
    if (quota != -12345L)
      this.theSession.not_done = this.theSession.write_command("150-" + LOC.G("Quota space available") + ": " + Common.format_bytes_short(quota)); 
    if (this.theSession.IG("max_download_amount") != 0)
      this.theSession.not_done = this.theSession.write_command("150-" + LOC.G("Max Download") + ".  " + LOC.G("Sent") + ":" + (this.theSession.uiLG("bytes_sent") / 1024L) + "k.  " + LOC.G("Max") + ":" + this.theSession.IG("max_download_amount") + "k.  " + LOC.G("Available") + ":" + (this.theSession.IG("max_download_amount") - this.theSession.uiLG("bytes_sent") / 1024L) + "k."); 
    if (this.theSession.IG("ratio") != 0)
      this.theSession.not_done = this.theSession.write_command("150-" + LOC.G("Ratio is") + " " + this.theSession.IG("ratio") + " to 1. " + LOC.G("Received") + ":" + ((this.theSession.uiLG("bytes_received") + this.theSession.uiLG("ratio_bytes_received")) / 1024L) + "k " + LOC.G("Sent") + ":" + ((this.theSession.uiLG("bytes_sent") + this.theSession.uiLG("ratio_bytes_sent")) / 1024L) + "k.  " + LOC.G("Available") + ":" + (((this.theSession.uiLG("bytes_received") + this.theSession.uiLG("ratio_bytes_received")) * this.theSession.IG("ratio") - this.theSession.uiLG("bytes_sent") + this.theSession.uiLG("ratio_bytes_sent")) / 1024L) + "k."); 
    this.theSession.not_done = this.theSession.write_command("150", "%LIST-start%");
  }
  
  public static boolean checkName(Properties item, ServerSession theSession, boolean fullPaths, boolean ignoreFilter) throws Exception {
    String filter_str = String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + theSession.SG("file_filter");
    if (filter_str.indexOf("%") >= 0)
      filter_str = ServerStatus.thisObj.change_vars_to_values(filter_str, theSession); 
    if (ignoreFilter || Common.filter_check("L", item.getProperty("name"), filter_str)) {
      Log.log("LIST", 2, (String)item);
      if (fullPaths && !theSession.check_access_privs(item.getProperty("root_dir"), "RETR", item))
        return false; 
      if ((item.getProperty("privs", "").indexOf("(view)") >= 0 && item.getProperty("privs", "").indexOf("*") <= 0) || !item.getProperty("name", "").equals("Icon\r"))
        if ((item.getProperty("privs", "").indexOf("(invisible)") >= 0 && item.getProperty("privs", "").indexOf("(inherited)") >= 0) || item.getProperty("privs", "").indexOf("(invisible)") < 0)
          return true;  
    } 
    return false;
  }
  
  public static void generateLineEntry(Properties item, StringBuffer item_str, boolean makeupZipSize, String the_dir, boolean fullPaths, ServerSession theSession, boolean mlstFormat) {
    try {
      if (item.getProperty("type", "").equals("DIR") && makeupZipSize && item.getProperty("protocol").equalsIgnoreCase("file")) {
        Vector v = new Vector();
        Common.getAllFileListing(v, (new VRL(item.getProperty("url"))).getPath(), 20, false);
        long totalSize = 0L;
        for (int x = 0; x < v.size(); x++) {
          File f = v.elementAt(x);
          totalSize += f.length();
        } 
        item.put("size", (new StringBuffer(String.valueOf(totalSize))).toString());
      } else if (item.getProperty("type", "").equals("DIR") && theSession.BG("dir_calc")) {
        Vector inside_a_dir_list = new Vector();
        theSession.uVFS.getListing(inside_a_dir_list, String.valueOf(the_dir) + item.getProperty("name") + "/");
        for (int xx = 0; xx < inside_a_dir_list.size(); xx++) {
          Properties adder = inside_a_dir_list.elementAt(xx);
          item.put("size", Long.parseLong(item.getProperty("size", "")) + Long.parseLong(adder.getProperty("size")));
        } 
        item.put("num_items", inside_a_dir_list.size());
      } 
    } catch (Exception exception) {}
    if (!mlstFormat) {
      item_str.append(item.getProperty("permissions"));
      item_str.append(String.valueOf(Common.lpad(item.getProperty("num_items", ""), 4)) + " ");
      if (fullPaths && (theSession == null || !theSession.server_item.getProperty("serverType", "http").equalsIgnoreCase("SFTP"))) {
        VRL vrl = new VRL(item.getProperty("url"));
        if (vrl.getProtocol().equalsIgnoreCase("file")) {
          File f = new File(vrl.getPath());
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
        d = new Date(d.getTime() + (theSession.IG("timezone_offset") * 1000 * 60 * 60));
        item_str.append(String.valueOf(Common.lpad(theSession.sdf_yyyyMMddHHmmss.format(d), 15)) + " ");
        item_str.append(String.valueOf(Common.lpad(item.getProperty("day", "").trim(), 2)) + " ");
        item_str.append(String.valueOf(Common.lpad(item.getProperty("time_or_year", ""), 5)) + " ");
        String dir = item.getProperty("root_dir");
        if (dir.startsWith(theSession.SG("root_dir")))
          dir = dir.substring(theSession.SG("root_dir").length() - 1); 
        item_str.append(String.valueOf(dir) + item.getProperty("name") + "\r\n");
      } else {
        item_str.append(String.valueOf(Common.lpad(item.getProperty("month", "").trim(), 3)) + " ");
        item_str.append(String.valueOf(Common.lpad(item.getProperty("day", "").trim(), 2)) + " ");
        item_str.append(String.valueOf(Common.lpad(item.getProperty("time_or_year", ""), 5)) + " ");
        item_str.append(String.valueOf(item.getProperty("name", "")) + "\r\n");
      } 
    } else {
      Date d = new Date(Long.parseLong(item.getProperty("modified", "0")));
      d = new Date(d.getTime() + (theSession.IG("timezone_offset") * 1000 * 60 * 60));
      String privs = item.getProperty("privs", "");
      item_str.append("Type=").append(item.getProperty("type", "").toLowerCase()).append(";");
      item_str.append("Modify=").append(theSession.sdf_yyyyMMddHHmmssGMT.format(d)).append(";");
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
      item_str.append("Perm=").append(newprivs).append(";");
      item_str.append("Size=").append(item.getProperty("size")).append(";");
      item_str.append("UNIX.owner=").append(item.getProperty("owner").trim()).append(";");
      item_str.append("UNIX.group=").append(item.getProperty("group").trim()).append(";");
      item_str.append(" ").append(item.getProperty("name", ""));
      item_str.append("\r\n");
    } 
  }
  
  public String SG(String data) {
    return this.theSession.SG(data);
  }
}
