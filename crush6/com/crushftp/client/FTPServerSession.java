package com.crushftp.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

public class FTPServerSession {
  public static void main(String[] args) {
    Properties t = new Properties();
    for (int x = 0; x < args.length; x++) {
      String[] s = args[x].split(";");
      for (int xx = 0; xx < s.length; xx++) {
        String key = s[xx].split("=")[0].trim();
        String val = "";
        try {
          val = s[xx].split("=")[1].trim();
        } catch (Exception exception) {}
        while (key.startsWith("-"))
          key = key.substring(1); 
        if (val.startsWith("\"") && val.endsWith("\""))
          val = val.substring(1, val.length() - 1); 
        t.put(key.toUpperCase(), val);
        t.put(key, val);
      } 
    } 
    if (!t.containsKey("URL") && t.containsKey("PROTOCOL"))
      t.put("URL", String.valueOf(t.getProperty("PROTOCOL")) + "://" + t.getProperty("HOST") + ":" + t.getProperty("PORT") + t.getProperty("PATH", "/")); 
    if (t.getProperty("trustall", "true").equals("true"))
      Common.trustEverything(); 
    FTPServerSession f = new FTPServerSession();
    f.go(t);
  }
  
  Properties t = null;
  
  SimpleDateFormat sdf_yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss");
  
  SimpleDateFormat MMMddHHmm = new SimpleDateFormat("MMM dd HH:mm");
  
  SimpleDateFormat ddHHmmyyyy = new SimpleDateFormat("MMM dd  yyyy");
  
  SimpleDateFormat yyyySDF = new SimpleDateFormat("yyyy");
  
  public void go(Properties t) {
    this.t = t;
    ServerSocket ss = null;
    try {
      ss = new ServerSocket(Integer.parseInt(t.getProperty("port", "55555")));
      while (true) {
        Socket sock = ss.accept();
        (new Thread(new FTPSession(this, sock))).start();
      } 
    } catch (Exception e) {
      e.printStackTrace();
      return;
    } 
  }
  
  public class FTPSession implements Runnable {
    Socket sock;
    
    BufferedReader in;
    
    OutputStream out;
    
    Properties ui;
    
    HTTPClient http;
    
    final FTPServerSession this$0;
    
    public FTPSession(FTPServerSession this$0, Socket sock) {
      this.this$0 = this$0;
      this.sock = null;
      this.in = null;
      this.out = null;
      this.ui = new Properties();
      this.http = new HTTPClient(this$0.t.getProperty("URL", "http://127.0.0.1:8080/"), "", null);
      this.sock = sock;
    }
    
    public void run() {
      Thread.currentThread().setName("Tunnel FTPSession:" + this.sock);
      try {
        this.in = new BufferedReader(new InputStreamReader(this.sock.getInputStream(), "UTF8"));
        this.out = this.sock.getOutputStream();
        this.ui.put("dir", "/");
        this.ui.put("rest", "0");
        write("220 Welcome.");
        while (this.in != null) {
          String command = this.in.readLine();
          System.out.println(command);
          String data = "";
          if (command.indexOf(" ") >= 0) {
            data = command.substring(command.indexOf(" ") + 1);
            command = command.substring(0, command.indexOf(" "));
          } 
          command = command.toUpperCase();
          if (!processCommand(command, data))
            break; 
        } 
        this.sock.close();
      } catch (Exception e) {
        e.printStackTrace();
      } 
      try {
        finishUploads();
      } catch (Exception e) {
        e.printStackTrace();
      } 
    }
    
    public boolean processCommand(String command, String data) throws Exception {
      if (this.sock.isClosed())
        return false; 
      if (command.equals("QUIT")) {
        write("221 Goodbye.");
        return false;
      } 
      if (command.equals("USER")) {
        this.ui.put("user", data);
        write("331 Username OK.  Need password.");
      } else if (command.equals("PASS")) {
        this.ui.put("pass", data);
        if (this.http.login(G("user"), G("pass"), "").toUpperCase().indexOf("SUCCESS") >= 0) {
          write("230 Password OK.  Connected.");
        } else {
          write("550 Login failed.");
        } 
      } else if (command.equals("SYST")) {
        write("215 UNIX Type: L8");
      } else if (command.equals("FEAT")) {
        write("211-Extensions supported:");
        write(" EPSV");
        write(" EPRT");
        write(" SIZE");
        write(" MDTM");
        write(" REST STREAM");
        write("211 END");
      } else if (command.equals("NOOP")) {
        write("200 Command OK. (NOOP)");
      } else if (command.equals("PWD")) {
        write("257 \"" + G("dir") + "\" PWD command successful.");
      } else if (command.equals("CWD")) {
        if (!data.endsWith("/"))
          data = String.valueOf(data) + "/"; 
        data = dir(data);
        Properties stat = this.http.stat(data);
        if (stat != null && stat.getProperty("type").equals("DIR")) {
          this.ui.put("dir", data);
          write("250 \"" + G("dir") + "\" CWD command successful.");
        } else {
          write("550 No such directory.");
        } 
      } else if (command.equals("TYPE")) {
        write("200 Command OK : " + (data.toUpperCase().startsWith("A") ? "ASCII" : "BINARY") + " type selected.");
      } else if (command.equals("PASV") || command.equals("EPSV")) {
        this.ui.remove("port_ip");
        this.ui.remove("port_port");
        if (this.ui.containsKey("pasv"))
          ((ServerSocket)this.ui.remove("pasv")).close(); 
        ServerSocket pasv = new ServerSocket(0);
        int the_port = pasv.getLocalPort();
        this.ui.put("pasv", pasv);
        if (command.equals("EPSV")) {
          write("229 Entering Extended Passive Mode (|||" + the_port + "|)");
        } else {
          write("227 Entering Passive Mode (127,0,0,1," + (the_port / 256) + "," + (the_port - the_port / 256 * 256) + ")");
        } 
      } else if (command.equals("PORT")) {
        if (this.ui.containsKey("pasv"))
          ((ServerSocket)this.ui.remove("pasv")).close(); 
        String port_ip = data.substring(0, data.lastIndexOf(","));
        port_ip = port_ip.substring(0, port_ip.lastIndexOf(",")).replace(',', '.');
        String port_port = data.substring(port_ip.length() + 1);
        port_port = (new StringBuffer(String.valueOf(Integer.parseInt(port_port.split(",")[0]) * 256 + Integer.parseInt(port_port.split(",")[1])))).toString();
        this.ui.put("port_ip", port_ip);
        this.ui.put("port_port", port_port);
        write("200 PORT command successful. " + port_ip + ":" + port_port);
      } else if (command.equals("EPRT")) {
        if (this.ui.containsKey("pasv"))
          ((ServerSocket)this.ui.remove("pasv")).close(); 
        String port_ip = data.split("|")[2];
        String port_port = data.split("|")[3];
        this.ui.put("port_ip", port_ip);
        this.ui.put("port_port", port_port);
        write("200 PORT command successful. " + port_ip + ":" + port_port);
      } else if (command.equals("SIZE")) {
        Properties stat = this.http.stat(dir(data));
        if (stat != null && stat.getProperty("type").equals("FILE")) {
          write("213 " + stat.getProperty("size"));
        } else if (stat != null) {
          write("550 " + data + ": not a plain file.");
        } else {
          write("550 No such file.");
        } 
      } else if (command.equals("REST")) {
        long pos = Long.parseLong(data);
        this.ui.put("rest", (new StringBuffer(String.valueOf(pos))).toString());
        write("350 Restarting at " + pos + ". Send STORE or RETRIEVE to initiate transfer.");
      } else if (command.equals("DELE")) {
        Properties stat = this.http.stat(dir(data));
        if (stat != null && stat.getProperty("type").equals("FILE")) {
          this.http.delete(dir(data));
          write("250 \"" + data + "\" delete successful.");
        } else if (stat != null) {
          write("550 " + data + ": not a plain file.");
        } else {
          write("550 No such file.");
        } 
      } else if (command.equals("RMD")) {
        Properties stat = this.http.stat(dir(data));
        if (stat != null && stat.getProperty("type").equals("DIR")) {
          this.http.delete(dir(data));
          stat = this.http.stat(dir(data));
          if (stat == null) {
            write("250 \"" + data + "\" deleted.");
          } else {
            write("550 Directory not empty, or directory is locked.");
          } 
        } else {
          write("550 No such file.");
        } 
      } else if (command.equals("MKD")) {
        Properties stat = this.http.stat(dir(data));
        if (stat != null) {
          write("521 \"" + data + "\" already exists.");
        } else {
          this.http.makedir(dir(data));
          write("257 \"" + data + "\" directory created.");
        } 
      } else if (command.equals("MDTM")) {
        String dateNumber = "";
        if (!data.equals("")) {
          if (data.lastIndexOf(" ") >= 0)
            dateNumber = data.substring(data.lastIndexOf(" ")).trim(); 
          try {
            Long.parseLong(dateNumber);
            if (dateNumber.length() > 5) {
              dateNumber = (new StringBuffer(String.valueOf(Long.parseLong(dateNumber.trim())))).toString();
              data = data.substring(0, data.length() - dateNumber.length()).trim();
            } else {
              dateNumber = "";
            } 
          } catch (Exception e) {
            if (data.indexOf(" ") >= 0)
              dateNumber = data.substring(0, data.indexOf(" ")).trim(); 
            try {
              Long.parseLong(dateNumber);
              if (dateNumber.length() > 5) {
                dateNumber = (new StringBuffer(String.valueOf(Long.parseLong(dateNumber.trim())))).toString();
                data = data.substring(dateNumber.length() + 1);
              } else {
                dateNumber = "";
              } 
            } catch (Exception ee) {
              dateNumber = "";
            } 
          } 
        } 
        boolean ok = false;
        if (dateNumber.trim().length() > 0 && this.http.mdtm(dir(data), this.this$0.sdf_yyyyMMddHHmmss.parse(dateNumber).getTime()))
          ok = true; 
        if (dateNumber.trim().length() == 0)
          ok = true; 
        if (ok) {
          Properties stat = this.http.stat(dir(data));
          if (stat != null) {
            write("213 " + this.this$0.sdf_yyyyMMddHHmmss.format(new Date(Long.parseLong(stat.getProperty("modified")))));
          } else {
            write("550 No such file.");
          } 
        } else {
          write("550 Unable to set last modified date and time.");
        } 
      } else if (command.equals("RNFR")) {
        Properties stat = this.http.stat(dir(data));
        if (stat != null) {
          this.ui.put("rnfr", dir(data));
          write("350 File exists, ready for new name.");
        } else {
          write("550 No such file.");
        } 
      } else if (command.equals("RNTO")) {
        Properties stat = this.http.stat(dir(data));
        if (stat == null) {
          if (this.http.rename(G("rnfr"), dir(data))) {
            write("250 File renamed OK.");
          } else {
            write("550 Rename failed. (File locked or bad path?)");
          } 
        } else {
          write("550 File already exists.");
        } 
      } else if (command.trim().endsWith("ABOR")) {
        if (this.ui.containsKey("data_sock"))
          ((Socket)this.ui.remove("data_sock")).close(); 
        write("225 ABOR command successful.");
      } else if (command.equals("LIST")) {
        if (!data.startsWith("/"))
          data = ""; 
        StringBuffer item_str = new StringBuffer();
        finishUploads();
        Vector v = new Vector();
        this.http.list(dir(data), v);
        for (int x = 0; x < v.size(); x++) {
          Properties item = v.elementAt(x);
          item_str.append(item.getProperty("privs"));
          item_str.append(String.valueOf(Common.lpad(item.getProperty("count"), 4)) + " ");
          item_str.append(String.valueOf(Common.rpad(item.getProperty("owner"), 8)) + " ");
          item_str.append(String.valueOf(Common.rpad(item.getProperty("group"), 8)) + " ");
          item_str.append(String.valueOf(Common.lpad((new StringBuffer(String.valueOf(item.getProperty("size")))).toString(), 13)) + " ");
          Date d = new Date(Long.parseLong(item.getProperty("modified")));
          if (this.this$0.yyyySDF.format(d).equals(this.this$0.yyyySDF.format(new Date()))) {
            item_str.append(String.valueOf(this.this$0.MMMddHHmm.format(d)) + " ");
          } else {
            item_str.append(String.valueOf(this.this$0.ddHHmmyyyy.format(d)) + " ");
          } 
          item_str.append(String.valueOf(item.getProperty("name")) + "\r\n");
        } 
        String dot = "drwxrwxrwx" + Common.lpad("1", 4) + " " + Common.rpad("user", 8) + " " + Common.rpad("group", 8) + " " + Common.lpad("0", 13) + " " + this.this$0.MMMddHHmm.format(new Date()) + " ";
        item_str.insert(0, String.valueOf(dot) + "..\r\n");
        item_str.insert(0, String.valueOf(dot) + ".\r\n");
        write("150 Opening data connection for file list.");
        Socket data_sock = getDataSock();
        System.out.println(item_str.toString().trim());
        data_sock.getOutputStream().write(item_str.toString().getBytes("UTF8"));
        data_sock.close();
        this.ui.remove("data_sock");
        write("226 Directory transfer complete.");
      } else if (command.equals("STOR") || command.equals("APPE")) {
        boolean ok = true;
        if (!this.ui.containsKey("zipUpload")) {
          ok = false;
          Properties stat = this.http.stat(Common.all_but_last(dir(data)));
          if (stat == null) {
            write("550 No such directory.");
          } else {
            ok = true;
          } 
        } 
        if (ok) {
          String data2 = data;
          String command2 = command;
          (new Thread((Runnable)new Object(this, data2, command2)))






























































            
            .start();
        } 
      } else if (command.equals("RETR")) {
        Properties stat = this.http.stat(dir(data));
        if (stat == null) {
          write("550 No such file.");
        } else {
          String data2 = data;
          (new Thread((Runnable)new Object(this, data2)))
























































            
            .start();
        } 
      } else {
        write("550 Unknown Command.");
      } 
      return true;
    }
    
    public String dir(String data) {
      if (!data.startsWith("/"))
        data = String.valueOf(G("dir")) + data; 
      data = Common.dots(data);
      return data;
    }
    
    public void finishUploads() throws Exception {
      ZipTransfer zip = (ZipTransfer)this.ui.get("zipUpload");
      if (zip != null) {
        zip.closeUpload();
        this.ui.remove("zipUpload");
      } 
    }
    
    public Socket getDataSock() throws Exception {
      Socket data_sock = null;
      if (this.ui.containsKey("pasv")) {
        ServerSocket pasv = (ServerSocket)this.ui.remove("pasv");
        data_sock = pasv.accept();
        pasv.close();
      } else {
        data_sock = new Socket(this.ui.remove("port_ip").toString(), Integer.parseInt(this.ui.remove("port_port").toString()));
      } 
      this.ui.put("data_sock", data_sock);
      return data_sock;
    }
    
    public String G(String key) {
      return this.ui.getProperty(key, "");
    }
    
    public void write(String s) throws Exception {
      System.out.println(s);
      this.out.write((String.valueOf(s) + "\r\n").getBytes("UTF8"));
    }
  }
}
