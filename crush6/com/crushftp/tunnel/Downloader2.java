package com.crushftp.tunnel;

import com.crushftp.client.Common;
import com.crushftp.client.GenericClient;
import com.crushftp.tunnel2.Tunnel2;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;

public class Downloader2 {
  public Properties controller = new Properties();
  
  long totalBytes = 1L;
  
  long transferedBytes = 0L;
  
  int totalItems = 0;
  
  int transferedItems = 0;
  
  String status2 = "";
  
  StringBuffer action = new StringBuffer();
  
  boolean standAlone = false;
  
  int totalByteCalls = 0;
  
  public Properties statusInfo = new Properties();
  
  String remoteRoot = "/";
  
  StringBuffer CrushAuth = new StringBuffer();
  
  byte[] downloadTempBytes = new byte[32768];
  
  Vector ask = new Vector();
  
  Properties ask_response = new Properties();
  
  Vector clientPool = new Vector();
  
  public Downloader2(StringBuffer CrushAuth) {
    this.CrushAuth = CrushAuth;
  }
  
  public long getTotalBytes() {
    this.totalByteCalls++;
    return this.totalBytes;
  }
  
  public long getTransferedBytes() {
    return this.transferedBytes;
  }
  
  public int getTotalItems() {
    return this.totalItems;
  }
  
  public int getTransferedItems() {
    return this.transferedItems;
  }
  
  public void refreshStatusInfo() {
    this.statusInfo.put("totalBytes", (new StringBuffer(String.valueOf(this.totalBytes))).toString());
    this.statusInfo.put("transferedBytes", (new StringBuffer(String.valueOf(this.transferedBytes))).toString());
    this.statusInfo.put("totalItems", (new StringBuffer(String.valueOf(this.totalItems))).toString());
    this.statusInfo.put("transferedItems", (new StringBuffer(String.valueOf(this.transferedItems))).toString());
  }
  
  public String getStatus() {
    return String.valueOf(this.statusInfo.getProperty("downloadStatus", "")) + this.statusInfo.getProperty("tunnelInfo", "");
  }
  
  public void pause() {
    Tunnel2.msg("PAUSE");
    this.action.setLength(0);
    this.action.append("pause");
    this.status2 = getStatus();
    try {
      Thread.sleep(200L);
    } catch (Exception exception) {}
    this.statusInfo.put("downloadStatus", "Paused:" + this.status2);
  }
  
  public void resume() {
    Tunnel2.msg("RESUME");
    this.statusInfo.put("downloadStatus", this.status2);
    this.action.setLength(0);
  }
  
  public void cancel() {
    Tunnel2.msg("CANCEL");
    this.status2 = getStatus();
    this.action.setLength(0);
    this.action.append("cancel");
    try {
      Thread.sleep(5000L);
    } catch (InterruptedException interruptedException) {}
    this.statusInfo.put("downloadStatus", "Cancelled:" + this.status2);
  }
  
  public String getAsk() {
    if (this.ask.size() > 0) {
      Properties p = this.ask.elementAt(0);
      this.ask_response.put(p.getProperty("uid"), p);
      this.ask.remove(0);
      return ":::ask=true:::path=" + p.getProperty("path") + ":::modified=" + p.getProperty("modified") + ":::size=" + p.getProperty("size") + ":::uid=" + p.getProperty("uid");
    } 
    return "";
  }
  
  public void go() {
    System.getProperties().put("sun.net.http.retryPost", "false");
    this.totalBytes = 1L;
    this.transferedBytes = 0L;
    this.totalItems = 0;
    this.transferedItems = 0;
    this.statusInfo.put("downloadStatus", "");
    this.status2 = "";
    this.action.setLength(0);
    this.controller.put("statusInfo", this.statusInfo);
    refreshStatusInfo();
    Tunnel2.msg("Using Server URL:" + this.controller.getProperty("URL"));
    if (this.CrushAuth.toString().equals(""))
      try {
        this.CrushAuth.setLength(0);
        this.CrushAuth.append(Common.login(this.controller.getProperty("URL_REAL", this.controller.getProperty("URL")), this.controller.getProperty("USERNAME"), this.controller.getProperty("PASSWORD"), null));
      } catch (Exception e) {
        Tunnel2.msg(e);
        if (this.standAlone)
          System.exit(1); 
      }  
    this.statusInfo.put("downloadStatus", "Download:Finding files...");
    if (this.controller.getProperty("LISTFILES", "true").equals("true")) {
      this.controller.put("serverFileList", new Vector());
      this.controller.put("serverFolderList", new Vector());
      Vector files = new Vector();
      int loop = 1;
      while (this.controller.containsKey("P" + loop)) {
        files.addElement(this.controller.getProperty("P" + loop));
        loop++;
      } 
      Tunnel2.msg("Files:" + files.size());
      for (int x = 0; x < files.size(); x++) {
        this.statusInfo.put("downloadStatus", "Download:Finding file size " + files.elementAt(x));
        getServerFileInfo(files.elementAt(x).toString());
        if (!Tunnel2.checkAction(this.action))
          return; 
      } 
    } 
    Vector serverFileList = (Vector)this.controller.get("serverFileList");
    Vector serverFolderList = (Vector)this.controller.get("serverFolderList");
    this.totalItems = serverFileList.size() + serverFolderList.size();
    long totalBytes2 = 1L;
    String commonStartPath = "";
    if (this.controller.getProperty("ALLOWPATHFIX", "true").equals("true")) {
      for (int x = 0; x < serverFileList.size(); x++) {
        Properties p = serverFileList.elementAt(x);
        totalBytes2 += Long.parseLong(p.getProperty("size", "0"));
        if ((p.getProperty("path").split("\\/")).length > (commonStartPath.split("\\/")).length)
          commonStartPath = p.getProperty("path"); 
      } 
      while (!commonStartPath.equals("")) {
        commonStartPath = Common.all_but_last(commonStartPath);
        boolean ok = true;
        for (int i = 0; i < serverFileList.size() && ok; i++) {
          Properties p = serverFileList.elementAt(i);
          if (!p.getProperty("path").startsWith(commonStartPath))
            ok = false; 
        } 
        if (ok)
          break; 
      } 
      if (commonStartPath.length() > 2)
        commonStartPath = Common.all_but_last(commonStartPath); 
      if (commonStartPath.equals("/"))
        commonStartPath = ""; 
    } 
    if (totalBytes2 != 1L)
      this.totalBytes = totalBytes2; 
    if (this.totalBytes == 1L) {
      totalBytes2 = 0L;
      for (int x = 0; x < serverFileList.size(); x++) {
        Properties p = serverFileList.elementAt(x);
        totalBytes2 += Long.parseLong(p.getProperty("size", "0"));
      } 
      if (totalBytes2 != 0L)
        this.totalBytes = totalBytes2; 
    } 
    refreshStatusInfo();
    Tunnel2.msg("Free JVM Memory:" + Common.format_bytes_short(Common.getFreeRam()));
    Object t = null;
    try {
      try {
        if (this.totalBytes > 1048576L && this.controller.getProperty("ALLOWTUNNEL", "true").equals("true"))
          t = AutoChannelProxy.enableAppletTunnel(this.controller, false, this.CrushAuth); 
      } catch (Exception e) {
        Tunnel2.msg("Error checking for tunnel.");
        Tunnel2.msg(e);
      } 
      if (!Tunnel2.checkAction(this.action))
        return; 
      doDownloadZip(this.controller, commonStartPath);
    } catch (Exception e) {
      Tunnel2.msg(e);
      if (this.standAlone)
        System.exit(1); 
    } finally {
      this.controller.put("stopTunnel", "true");
      if (t != null)
        while (this.controller.containsKey("stopTunnel")) {
          try {
            Thread.sleep(100L);
          } catch (InterruptedException e) {
            e.printStackTrace();
          } 
        }  
      this.action.setLength(0);
    } 
    while (this.clientPool.size() > 0) {
      try {
        ((GenericClient)this.clientPool.remove(0)).close();
      } catch (Exception e) {
        e.printStackTrace();
      } 
    } 
  }
  
  public void getServerFileInfo(String f) {
    HttpURLConnection urlc = null;
    int loops = 0;
    while (loops++ < 5) {
      try {
        Vector serverFileList = (Vector)this.controller.get("serverFileList");
        Vector serverFolderList = (Vector)this.controller.get("serverFolderList");
        if (f.startsWith("/") && this.controller.getProperty("URL_REAL", this.controller.getProperty("URL")).endsWith("/"))
          f = f.substring(1); 
        f = Common.replace_str(f, "#", "%23");
        URL u = new URL(String.valueOf(this.controller.getProperty("URL_REAL", this.controller.getProperty("URL"))) + f + "/:filetree");
        Tunnel2.msg("Getting folder contents information " + u.toExternalForm());
        urlc = (HttpURLConnection)u.openConnection();
        urlc.setReadTimeout(70000);
        urlc.setRequestMethod("GET");
        urlc.setRequestProperty("Cookie", "CrushAuth=" + this.CrushAuth.toString() + ";");
        urlc.setUseCaches(false);
        urlc.setDoInput(true);
        BufferedReader br = null;
        this.statusInfo.put("downloadStatus", "Download:Getting folder contents info " + u.toExternalForm());
        String data = "";
        try {
          br = new BufferedReader(new InputStreamReader(urlc.getInputStream(), "UTF8"));
          while ((data = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(data);
            Properties p = new Properties();
            p.put("permissions", st.nextToken());
            if (p.getProperty("permissions").startsWith("d")) {
              p.put("type", "DIR");
            } else {
              p.put("type", "FILE");
            } 
            st.nextToken();
            st.nextToken();
            st.nextToken();
            long tempfileSize = Long.parseLong(st.nextToken());
            st.nextToken();
            st.nextToken();
            String year = st.nextToken();
            String rootdir = data.substring(data.indexOf(String.valueOf(year) + " /") + (String.valueOf(year) + " ").length()).trim();
            p.put("path", rootdir);
            p.put("size", (new StringBuffer(String.valueOf(tempfileSize))).toString());
            if (p.getProperty("type").equals("DIR")) {
              serverFolderList.addElement(p);
            } else {
              serverFileList.addElement(p);
            } 
            if (!Tunnel2.checkAction(this.action))
              return; 
            this.totalItems++;
            this.totalBytes += tempfileSize;
          } 
        } finally {
          br.close();
          urlc.getResponseCode();
        } 
        break;
      } catch (Exception e) {
        Tunnel2.msg(e);
        Tunnel2.msg(String.valueOf(f) + " not found:" + e.toString());
      } finally {
        if (urlc != null)
          urlc.disconnect(); 
      } 
      try {
        Thread.sleep(1000L);
      } catch (InterruptedException interruptedException) {}
    } 
  }
  
  public synchronized void addBytes(long bytes) {
    this.transferedBytes += bytes;
  }
  
  public void doDownloadZip(Properties params, String commonStartPath) {
    if (commonStartPath.equals(""))
      commonStartPath = "/"; 
    Common.trustEverything();
    boolean loggedIn = true;
    int errorDelay = 1;
    String boundary = "--" + Common.makeBoundary(11);
    if (!this.controller.getProperty("PATH").endsWith("/"))
      this.controller.put("PATH", String.valueOf(this.controller.getProperty("PATH")) + "/"); 
    String abs = String.valueOf((new File(this.controller.getProperty("PATH"))).getAbsolutePath()) + "/";
    Vector serverFileList = (Vector)this.controller.get("serverFileList");
    Vector individualItems = new Vector();
    try {
      Thread.sleep(500L);
    } catch (Exception exception) {}
    Vector serverFolderList = (Vector)this.controller.get("serverFolderList");
    for (int x = 0; x < serverFolderList.size(); x++) {
      Properties p = serverFolderList.elementAt(x);
      if (p.getProperty("path").length() > commonStartPath.length() - 1)
        (new File(String.valueOf(abs) + p.getProperty("path").substring(commonStartPath.length() - 1))).mkdirs(); 
    } 
    HttpURLConnection urlc = null;
    Properties fileRetries = new Properties();
    String path2 = "";
    Vector downloadedItems = new Vector();
    while (loggedIn && Tunnel2.checkAction(this.action) && (serverFileList.size() > 0 || individualItems.size() > 0)) {
      refreshStatusInfo();
      StringBuffer downloadPath = new StringBuffer();
      int maxItems = 200;
      if (Common.getFreeRam() > 134217728L) {
        maxItems = 1000;
      } else if (Common.getFreeRam() > 67108864L) {
        maxItems = 700;
      } else if (Common.getFreeRam() > 33554432L) {
        maxItems = 400;
      } 
      for (int i = serverFileList.size() - 1; i >= 0 && downloadedItems.size() < maxItems; i--) {
        Properties p = serverFileList.elementAt(i);
        if (!(new File(String.valueOf(abs) + p.getProperty("path"))).exists() && !this.controller.containsKey("META_downloadRevision")) {
          downloadPath.append(p.getProperty("path")).append(":");
          downloadedItems.addElement(serverFileList.remove(i));
        } else {
          individualItems.addElement(serverFileList.remove(i));
        } 
      } 
      long lastTransferredBytes = this.transferedBytes;
      try {
        if (downloadPath.length() != 0) {
          this.statusInfo.put("downloadStatus", "Download:Zipstream downloading items...(" + serverFileList.size() + ")");
          urlc = (HttpURLConnection)(new URL(this.controller.getProperty("URL"))).openConnection();
          HttpURLConnection.setFollowRedirects(false);
          urlc.setReadTimeout(70000);
          urlc.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary.substring(2, boundary.length()));
          urlc.setRequestMethod("POST");
          urlc.setRequestProperty("Cookie", "CrushAuth=" + this.CrushAuth.toString() + ";");
          urlc.setUseCaches(false);
          urlc.setDoInput(true);
          urlc.setDoOutput(true);
          BufferedOutputStream out = new BufferedOutputStream(urlc.getOutputStream());
          if (params != null)
            writeParams(params, out, boundary); 
          Common.writeEntry("command", "downloadAsZip", out, boundary);
          Common.writeEntry("c2f", this.CrushAuth.toString().substring(this.CrushAuth.toString().length() - 4), out, boundary);
          Common.writeEntry("no_zip_compression", (new StringBuffer(String.valueOf(this.controller.getProperty("NOCOMPRESSION", "false").equals("true")))).toString(), out, boundary);
          Common.writeEntry("zip64", "true", out, boundary);
          out.flush();
          Common.writeEntry("paths", downloadPath.toString(), out, boundary);
          downloadPath.setLength(0);
          out.flush();
          Common.writeEntry("path_shortening", this.controller.getProperty("ALLOWPATHFIX", "false"), out, boundary);
          out.flush();
          Common.writeEnd(out, boundary);
          int code = urlc.getResponseCode();
          if (code == 302) {
            loggedIn = false;
            this.CrushAuth.setLength(0);
            throw new Exception("Login session has expired, or the server was rebooted.");
          } 
          InputStream in = urlc.getInputStream();
          FileArchiveInputStream zin = new FileArchiveInputStream(new BufferedInputStream(in));
          while (true) {
            refreshStatusInfo();
            if (!Tunnel2.checkAction(this.action))
              break; 
            ZipArchiveEntry entry = zin.getNextZipEntry();
            if (entry == null)
              break; 
            path2 = "/" + entry.getName();
            this.statusInfo.put("downloadStatus", "Download:" + path2 + "...");
            Tunnel2.msg("path2:" + path2 + "  commonStartPath:" + commonStartPath + "   remoteRoot:" + this.remoteRoot);
            String itemName = (new File(String.valueOf(abs) + path2.substring(commonStartPath.length() + this.remoteRoot.length() - 1))).getCanonicalPath();
            Tunnel2.msg("Zipstream downloading:" + path2);
            if (entry.isDirectory()) {
              (new File(itemName)).mkdirs();
            } else {
              (new File(Common.all_but_last(itemName))).mkdirs();
              if ((new File(itemName)).exists())
                (new File(itemName)).renameTo(new File(String.valueOf(itemName) + ".downloading")); 
              RandomAccessFile ra = new RandomAccessFile(String.valueOf(itemName) + ".downloading", "rw");
              ra.setLength(0L);
              ra.close();
              lastTransferredBytes = this.transferedBytes;
              try {
                writeFile((InputStream)zin, String.valueOf(itemName) + ".downloading", path2, 0L, entry.getSize());
              } finally {
                if (Integer.parseInt(fileRetries.getProperty(path2, "0")) > 10)
                  cancel(); 
              } 
              lastTransferredBytes = this.transferedBytes;
              (new File(itemName)).delete();
              (new File(String.valueOf(itemName) + ".downloading")).renameTo(new File(itemName));
            } 
            if (entry.getTime() > 0L)
              (new File(itemName)).setLastModified(entry.getTime()); 
            this.transferedItems++;
            errorDelay = 1;
            boolean foundOne = false;
            for (int j = 0; j < downloadedItems.size(); j++) {
              Properties p = downloadedItems.elementAt(j);
              if (p.getProperty("path").replace('\\', '/').equals(path2.replace('\\', '/'))) {
                downloadedItems.remove(j);
                foundOne = true;
                break;
              } 
            } 
            if (!foundOne && downloadedItems.size() > 0) {
              downloadedItems.remove(0);
              Tunnel2.msg("Unable to verify downloaded file:" + path2);
            } 
          } 
          zin.close();
          in.close();
          urlc.disconnect();
        } 
        downloadedItems.clear();
        refreshStatusInfo();
        while (individualItems.size() > 0) {
          refreshStatusInfo();
          if (!Tunnel2.checkAction(this.action))
            break; 
          Properties p = individualItems.elementAt(0);
          long size = Long.parseLong(p.getProperty("size", "0"));
          path2 = p.getProperty("path");
          this.statusInfo.put("downloadStatus", "Download:" + path2 + "...");
          Tunnel2.msg(getStatus());
          String itemName = (new File(String.valueOf(abs) + path2.substring(commonStartPath.length() + this.remoteRoot.length() - 1))).getCanonicalPath();
          (new File(Common.all_but_last(itemName))).mkdirs();
          if ((new File(itemName)).exists() && (new File(itemName)).isDirectory()) {
            this.transferedItems++;
            individualItems.remove(0);
            errorDelay = 1;
            continue;
          } 
          long startPos = 0L;
          boolean skip = false;
          if (!params.getProperty("OVERWRITE", "").equals("OVERWRITE")) {
            Properties stat = new Properties();
            File f = new File(itemName);
            if (f.exists()) {
              stat.put("size", (new StringBuffer(String.valueOf(f.length()))).toString());
              stat.put("modified", (new StringBuffer(String.valueOf(f.lastModified()))).toString());
              if (params.getProperty("OVERWRITE", "").equals("I'l")) {
                startPos = Long.parseLong(stat.getProperty("size"));
              } else if (params.getProperty("OVERWRITE", "").equals("SKIP")) {
                skip = true;
              } else if (params.getProperty("OVERWRITE", "").equals("ASK")) {
                stat.put("uid", Common.makeBoundary(8).toUpperCase());
                stat.put("path", itemName);
                this.ask.addElement(stat);
                while (this.ask.indexOf(stat) >= 0) {
                  if (!Tunnel2.checkAction(this.action))
                    throw new Exception("Cancelled"); 
                  Thread.sleep(300L);
                } 
                while (this.ask_response.containsKey(stat.getProperty("uid"))) {
                  if (!Tunnel2.checkAction(this.action))
                    throw new Exception("Cancelled"); 
                  Thread.sleep(300L);
                } 
                if (params.getProperty("OVERWRITE", "").equals("RESUME")) {
                  startPos = Long.parseLong(stat.getProperty("size"));
                } else if (params.getProperty("OVERWRITE", "").equals("OVERWRITE")) {
                  startPos = 0L;
                } else if (params.getProperty("OVERWRITE", "").equals("SKIP")) {
                  skip = true;
                } else if (params.getProperty("OVERWRITE", "").equals("ASK")) {
                  if (stat.getProperty("response", "overwrite").equalsIgnoreCase("overwrite")) {
                    startPos = 0L;
                  } else if (stat.getProperty("response").equalsIgnoreCase("overwrite_all")) {
                    params.put("OVERWRITE", "OVERWRITE");
                    startPos = 0L;
                    this.ask.removeAllElements();
                    this.ask_response.clear();
                  } else if (stat.getProperty("response", "").equalsIgnoreCase("resume")) {
                    startPos = Long.parseLong(stat.getProperty("size"));
                  } else if (stat.getProperty("response", "").equalsIgnoreCase("resume_all")) {
                    params.put("OVERWRITE", "RESUME");
                    startPos = Long.parseLong(stat.getProperty("size"));
                    this.ask.removeAllElements();
                    this.ask_response.clear();
                  } else if (stat.getProperty("response", "").equalsIgnoreCase("skip")) {
                    skip = true;
                  } else if (stat.getProperty("response", "").equalsIgnoreCase("skip_all")) {
                    params.put("OVERWRITE", "SKIP");
                    skip = true;
                    this.ask.removeAllElements();
                    this.ask_response.clear();
                  } 
                } 
              } 
            } 
          } 
          if (!skip) {
            if ((new File(itemName)).exists())
              (new File(itemName)).renameTo(new File(String.valueOf(itemName) + ".downloading")); 
            File f = new File(String.valueOf(itemName) + ".downloading");
            Vector byteRanges = new Vector();
            Vector byteRangesRemote = new Vector();
            boolean fileInfoFound = false;
            if (f.exists() && f.length() < size && startPos > 0L) {
              byteRanges.addElement(String.valueOf(startPos) + "-");
              addBytes(startPos);
              fileInfoFound = true;
            } 
            if (!fileInfoFound && startPos == 0L) {
              byteRanges.addElement("0-");
              RandomAccessFile ra = new RandomAccessFile(f.getPath(), "rw");
              ra.setLength(0L);
              ra.close();
            } 
            StringBuffer byteRangesSb = new StringBuffer();
            for (int j = 0; j < byteRangesRemote.size(); j++) {
              byteRangesSb.append("bytes=" + byteRangesRemote.elementAt(j).toString());
              if (j < byteRanges.size() - 1)
                byteRangesSb.append(", "); 
            } 
            urlc = (HttpURLConnection)(new URL(this.controller.getProperty("URL"))).openConnection();
            urlc.setReadTimeout(70000);
            HttpURLConnection.setFollowRedirects(false);
            urlc.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary.substring(2, boundary.length()));
            urlc.setRequestMethod("POST");
            urlc.setRequestProperty("Cookie", "CrushAuth=" + this.CrushAuth.toString() + ";");
            urlc.setUseCaches(false);
            urlc.setDoInput(true);
            urlc.setDoOutput(true);
            BufferedOutputStream out = new BufferedOutputStream(urlc.getOutputStream());
            if (params != null)
              writeParams(params, out, boundary); 
            Common.writeEntry("command", "download", out, boundary);
            Common.writeEntry("c2f", this.CrushAuth.toString().substring(this.CrushAuth.toString().length() - 4), out, boundary);
            Common.writeEntry("range", byteRangesSb.toString(), out, boundary);
            out.flush();
            Common.writeEntry("path", path2, out, boundary);
            out.flush();
            Common.writeEnd(out, boundary);
            int code = urlc.getResponseCode();
            if (code == 302) {
              loggedIn = false;
              this.CrushAuth.setLength(0);
              throw new Exception("Login session has expired, or the server was rebooted.");
            } 
            lastTransferredBytes = this.transferedBytes;
            for (int k = 0; k < byteRanges.size() && code >= 200 && code < 300; k++) {
              long startPos2 = Long.parseLong(byteRanges.elementAt(k).toString().substring(0, byteRanges.elementAt(k).toString().indexOf("-")));
              String endStr = byteRanges.elementAt(k).toString().substring(byteRanges.elementAt(k).toString().indexOf("-") + 1);
              if (endStr.equals(""))
                endStr = "-1"; 
              long endPos = Long.parseLong(endStr);
              if (size <= 0L && urlc.getHeaderField("Content-Length") != null)
                size = Long.parseLong(urlc.getHeaderField("Content-Length")); 
              if (endPos < 0L)
                endPos = size - 1L; 
              if (endPos >= 0L)
                endPos++; 
              InputStream in = urlc.getInputStream();
              writeFile(in, f.getCanonicalPath(), path2, startPos2, endPos);
            } 
            lastTransferredBytes = this.transferedBytes;
            if (urlc != null) {
              (new File(itemName)).delete();
              (new File(String.valueOf(itemName) + ".downloading")).renameTo(new File(itemName));
              if (code < 200 || code > 300)
                (new File(itemName)).delete(); 
              (new File(itemName)).setLastModified(urlc.getLastModified());
              urlc.disconnect();
            } 
          } else {
            addBytes(size);
          } 
          this.transferedItems++;
          individualItems.remove(0);
          errorDelay = 1;
        } 
      } catch (Exception e) {
        fileRetries.put(path2, (new StringBuffer(String.valueOf(Integer.parseInt(fileRetries.getProperty(path2, "0")) + 1))).toString());
        this.transferedBytes = lastTransferredBytes;
        if (urlc != null)
          urlc.disconnect(); 
        this.controller.put("resume", "true");
        this.statusInfo.put("downloadStatus", "Download:WARN:" + path2 + ":" + e.getMessage());
        Tunnel2.msg(e);
        try {
          Thread.sleep((errorDelay * 1000));
          errorDelay *= 2;
          if (errorDelay > 60) {
            this.CrushAuth.setLength(0);
            Uploader.login(this.controller, this.standAlone, this.CrushAuth);
          } 
        } catch (Exception exception) {}
      } 
    } 
    this.action.setLength(0);
    if (getStatus().indexOf("WARN:") >= 0)
      this.statusInfo.put("downloadStatus", "ERROR:" + getStatus()); 
    if (getStatus().indexOf("ERROR:") >= 0 && this.standAlone) {
      System.exit(1);
    } else if (getStatus().indexOf("ERROR:") < 0) {
      this.statusInfo.put("downloadStatus", "");
    } 
    this.transferedBytes = this.totalBytes;
    refreshStatusInfo();
  }
  
  public void writeParams(Properties params, BufferedOutputStream out, String boundary) throws Exception {
    Enumeration en = params.keys();
    while (en.hasMoreElements()) {
      String key = en.nextElement().toString();
      if (key.toUpperCase().startsWith("META_")) {
        String val = params.getProperty(key, "");
        while (key.toUpperCase().startsWith("META_"))
          key = key.substring("META_".length()); 
        Common.writeEntry("META_" + key, val, out, boundary);
      } 
    } 
  }
  
  public void writeFile(InputStream in, String itemName, String path2, long pos, long endPos) throws Exception {
    RandomAccessFile fout = new RandomAccessFile(itemName, "rw");
    try {
      fout.seek(pos);
      int bytesRead = 1;
      long start = System.currentTimeMillis();
      byte[] b = this.downloadTempBytes;
      while (bytesRead > 0) {
        if (endPos > 0L && b.length > endPos - pos)
          b = new byte[(int)(endPos - pos)]; 
        bytesRead = in.read(b);
        if (bytesRead > 0) {
          fout.write(b, 0, bytesRead);
          addBytes(bytesRead);
          pos += bytesRead;
          if (System.currentTimeMillis() - start > 1000L) {
            this.statusInfo.put("downloadStatus", "Download:" + path2 + "..." + Common.format_bytes_short(pos) + ((endPos >= 0L) ? ("/" + Common.format_bytes_short(endPos)) : "."));
            start = System.currentTimeMillis();
          } 
        } 
        refreshStatusInfo();
        if (!Tunnel2.checkAction(this.action) || (
          endPos > 0L && endPos == pos + 1L))
          break; 
      } 
    } finally {
      fout.close();
    } 
  }
}
