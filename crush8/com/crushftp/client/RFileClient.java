package com.crushftp.client;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

public class RFileClient extends GenericClient {
  Socket sock = null;
  
  ObjectOutputStream oos = null;
  
  ObjectInputStream ois = null;
  
  Process proc = null;
  
  VRL vrl = null;
  
  static long last_activity = System.currentTimeMillis();
  
  static Object log_lock = new Object();
  
  static String server_port = "";
  
  public static void main(String[] args) {
    server_port = args[0];
    try {
      Worker.startWorker(new Runnable() {
            public void run() {
              while (true) {
                try {
                  if (System.currentTimeMillis() - RFileClient.last_activity > 60000L) {
                    RFileClient.last_activity = System.currentTimeMillis();
                    RFileClient.write_log(Common.dumpStack("ServerPort:" + RFileClient.server_port + " Date:" + new Date()));
                    RFileClient.last_activity = System.currentTimeMillis();
                  } 
                  Thread.sleep(1000L);
                } catch (Exception e) {
                  e.printStackTrace();
                } 
              } 
            }
          });
      Socket sock = new Socket("127.0.0.1", Integer.parseInt(server_port));
      ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
      ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
      String url = ois.readObject().toString();
      System.out.println((new VRL(url)).getUsername());
      String header = ois.readObject().toString();
      Vector log = new Vector();
      FileClient fc = new FileClient(url, header, log);
      while (true) {
        Properties p = (Properties)ois.readObject();
        if (p == null)
          break; 
        last_activity = System.currentTimeMillis();
        try {
          p.put("response", "");
          if (p.getProperty("command").equals("list")) {
            write_log(new Date() + ":Command:" + p.getProperty("command") + " " + p.getProperty("path"));
            p.put("response", fc.list(p.getProperty("path"), (Vector)p.get("list")));
          } else if (p.getProperty("command").equals("stat")) {
            write_log(new Date() + ":Command:" + p.getProperty("command") + " " + p.getProperty("path"));
            Object o = fc.stat(p.getProperty("path"));
            p.put("response", (o == null) ? "null" : o);
          } else if (p.getProperty("command").equals("mdtm")) {
            write_log(new Date() + ":Command:" + p.getProperty("command") + " " + p.getProperty("path") + " " + p.getProperty("modified"));
            p.put("response", (new StringBuffer(String.valueOf(fc.mdtm(p.getProperty("path"), Long.parseLong(p.getProperty("modified")))))).toString());
          } else if (p.getProperty("command").equals("rename")) {
            write_log(new Date() + ":Command:" + p.getProperty("command") + " RNFR:" + p.getProperty("rnfr0") + " RNTO:" + p.getProperty("rnto0"));
            p.put("response", (new StringBuffer(String.valueOf(fc.rename(p.getProperty("rnfr0"), p.getProperty("rnto0"))))).toString());
          } else if (p.getProperty("command").equals("delete")) {
            write_log(new Date() + ":Command:" + p.getProperty("command") + " " + p.getProperty("path"));
            p.put("response", (new StringBuffer(String.valueOf(fc.delete(p.getProperty("path"))))).toString());
          } else if (p.getProperty("command").equals("makedir")) {
            write_log(new Date() + ":Command:" + p.getProperty("command") + " " + p.getProperty("path"));
            p.put("response", (new StringBuffer(String.valueOf(fc.makedir(p.getProperty("path"))))).toString());
          } else if (p.getProperty("command").equals("makedirs")) {
            write_log(new Date() + ":Command:" + p.getProperty("command") + " " + p.getProperty("path"));
            p.put("response", (new StringBuffer(String.valueOf(fc.makedirs(p.getProperty("path"))))).toString());
          } else if (p.getProperty("command").equals("setMod")) {
            write_log(new Date() + ":Command:" + p.getProperty("command") + " " + p.getProperty("path") + " " + p.getProperty("val") + " " + p.getProperty("param"));
            fc.setMod(p.getProperty("path"), p.getProperty("val"), p.getProperty("param"));
          } else if (p.getProperty("command").equals("setOwner")) {
            write_log(new Date() + ":Command:" + p.getProperty("command") + " " + p.getProperty("path") + " " + p.getProperty("val") + " " + p.getProperty("param"));
            fc.setOwner(p.getProperty("path"), p.getProperty("val"), p.getProperty("param"));
          } else if (p.getProperty("command").equals("setGroup")) {
            write_log(new Date() + ":Command:" + p.getProperty("command") + " " + p.getProperty("path") + " " + p.getProperty("val") + " " + p.getProperty("param"));
            fc.setGroup(p.getProperty("path"), p.getProperty("val"), p.getProperty("param"));
          } else if (p.getProperty("command").equals("doCommand")) {
            write_log(new Date() + ":Command:" + p.getProperty("command") + " " + p.getProperty("command2"));
            p.put("response", fc.doCommand(p.getProperty("command2")));
          } else if (p.getProperty("command").equals("download3")) {
            write_log(new Date() + ":Command:" + p.getProperty("command") + " " + p.getProperty("path"));
            InputStream in_tmp = fc.download3(p.getProperty("path"), Long.parseLong(p.getProperty("startPos")), Long.parseLong(p.getProperty("endPos")), p.getProperty("binary").equals("true"));
            Socket sock_tmp = new Socket("127.0.0.1", Integer.parseInt(p.getProperty("port")));
            Common.streamCopier(in_tmp, sock_tmp.getOutputStream(), true, true, true);
            p.put("response", "");
          } else if (p.getProperty("command").equals("upload3")) {
            write_log(new Date() + ":Command:" + p.getProperty("command") + " " + p.getProperty("path"));
            OutputStream out_tmp = fc.upload3(p.getProperty("path"), Long.parseLong(p.getProperty("startPos")), p.getProperty("truncate").equals("true"), p.getProperty("binary").equals("true"));
            Socket sock_tmp = new Socket("127.0.0.1", Integer.parseInt(p.getProperty("port")));
            Common.streamCopier(sock_tmp.getInputStream(), out_tmp, true, true, true);
            p.put("response", "");
          } else if (p.getProperty("command").equals("logout")) {
            write_log(new Date() + ":Command:" + p.getProperty("command"));
            fc.logout();
          } else if (p.getProperty("command").equals("freeCache")) {
            write_log(new Date() + ":Command:" + p.getProperty("command"));
            fc.freeCache();
          } 
        } catch (Exception e) {
          write_log(new Date() + ":ERROR:" + e);
          p.put("error", e);
          e.printStackTrace();
        } 
        p.put("log", log);
        oos.reset();
        oos.writeObject(p);
        log.removeAllElements();
      } 
      sock.close();
    } catch (EOFException eOFException) {
    
    } catch (Exception e) {
      e.printStackTrace();
    } 
    try {
      Thread.sleep(5000L);
    } catch (InterruptedException interruptedException) {}
  }
  
  static void write_log(String s) throws Exception {
    synchronized (log_lock) {
      (new File("./RFILE/")).mkdir();
      String log_file = "./RFILE/" + server_port + ".txt";
      if ((new File(log_file)).exists() && System.currentTimeMillis() - (new File(log_file)).lastModified() > 70000L)
        (new File(log_file)).delete(); 
      RandomAccessFile raf = new RandomAccessFile(log_file, "rw");
      if (raf.length() > 5242880L)
        raf.setLength(0L); 
      try {
        raf.seek(raf.length());
        raf.write(s.getBytes());
      } finally {
        raf.close();
      } 
    } 
  }
  
  public RFileClient(String url, String header, Vector log) {
    super(header, log);
    this.url = url;
    this.vrl = new VRL(url);
    ServerSocket ss = null;
    try {
      ss = new ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"));
      ss.setSoTimeout(30000);
      File f = (new File((new StringBuffer(String.valueOf(System.getProperty("crushftp.home")))).toString())).getCanonicalFile();
      this.proc = Runtime.getRuntime().exec(new String[] { 
            "psexec.exe", "-accepteula", "-i", "-x", "-e", "-d", "-u", this.vrl.getUsername(), "-p", this.vrl.getPassword(), 
            "-w", f.getCanonicalPath(), "java", "-Xmx32M", "-cp", "WebInterface/CrushTunnel.jar", "com.crushftp.client.RFileClient", (new StringBuffer(String.valueOf(ss.getLocalPort()))).toString() }, (String[])null, f);
      if (this.proc.getErrorStream().available() > 0) {
        int bytes_read = 1;
        byte[] b = new byte[32768];
        while (bytes_read > 0) {
          bytes_read = this.proc.getErrorStream().read(b);
          if (bytes_read > 0)
            System.out.println(new String(b, 0, bytes_read)); 
        } 
      } 
      if (this.proc.getInputStream().available() > 0) {
        int bytes_read = 1;
        byte[] b = new byte[32768];
        while (bytes_read > 0) {
          bytes_read = this.proc.getInputStream().read(b);
          if (bytes_read > 0)
            System.out.println(new String(b, 0, bytes_read)); 
        } 
      } 
      this.sock = ss.accept();
      log("Got RFile socket connection:" + ss.getLocalPort() + " for user:" + this.vrl.getUsername());
      this.oos = new ObjectOutputStream(this.sock.getOutputStream());
      this.oos.writeObject(url);
      this.oos.writeObject(header);
      this.oos.flush();
      this.ois = new ObjectInputStream(this.sock.getInputStream());
    } catch (Exception e) {
      try {
        ss.close();
      } catch (IOException iOException) {}
      e.printStackTrace();
    } 
    try {
      ss.close();
    } catch (IOException iOException) {}
  }
  
  public void logout() throws Exception {
    Properties p = new Properties();
    try {
      p.put("command", "logout");
      this.oos.writeObject(p);
      log("Issued command:" + p);
      p = getResult();
    } catch (Exception exception) {}
    if (this.oos != null)
      this.oos.close(); 
    if (this.ois != null)
      this.ois.close(); 
    if (this.sock != null)
      this.sock.close(); 
  }
  
  public void freeCache() {
    Properties p = new Properties();
    try {
      p.put("command", "freeCache");
      this.oos.writeObject(p);
      log("Issued command:" + p);
      p = getResult();
    } catch (Exception exception) {}
  }
  
  public Properties stat(String path) throws Exception {
    Properties p = new Properties();
    p.put("command", "stat");
    p.put("path", path);
    this.oos.writeObject(p);
    log("Issued command:" + p);
    p = getResult();
    Object o = p.get("response");
    if (o instanceof String && o.toString().equals("null"))
      o = null; 
    return (Properties)o;
  }
  
  public Vector list(String path, Vector list) throws Exception {
    Properties p = new Properties();
    p.put("command", "list");
    p.put("path", path);
    p.put("list", list);
    this.oos.writeObject(p);
    log("Issued command:" + p);
    p = getResult();
    Vector v = (Vector)p.get("response");
    if (list != null && list.size() == 0) {
      list.addAll(v);
    } else {
      list = v;
    } 
    for (int x = list.size() - 1; x >= 0; x--) {
      Properties dir_item = list.elementAt(x);
      if (dir_item.getProperty("modified", "0").equals("0"))
        list.remove(x); 
    } 
    return list;
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary) throws Exception {
    Properties p = new Properties();
    p.put("command", "download3");
    p.put("path", path);
    p.put("startPos", (new StringBuffer(String.valueOf(startPos))).toString());
    p.put("endPos", (new StringBuffer(String.valueOf(endPos))).toString());
    p.put("binary", (new StringBuffer(String.valueOf(binary))).toString());
    ServerSocket ss = new ServerSocket(0);
    ss.setSoTimeout(10000);
    p.put("port", (new StringBuffer(String.valueOf(ss.getLocalPort()))).toString());
    Socket sock_tmp = null;
    try {
      this.oos.writeObject(p);
      log("Issued command:" + p);
      sock_tmp = ss.accept();
      ss.close();
      p = getResult();
    } catch (Exception e) {
      if (sock_tmp != null)
        sock_tmp.close(); 
      throw e;
    } finally {
      ss.close();
    } 
    return sock_tmp.getInputStream();
  }
  
  public boolean mdtm(String path, long modified) throws Exception {
    Properties p = new Properties();
    p.put("command", "mdtm");
    p.put("path", path);
    p.put("modified", (new StringBuffer(String.valueOf(modified))).toString());
    this.oos.writeObject(p);
    log("Issued command:" + p);
    p = getResult();
    return p.get("response").toString().equals("true");
  }
  
  public boolean rename(String rnfr0, String rnto0) throws Exception {
    Properties p = new Properties();
    p.put("command", "rename");
    p.put("rnfr0", rnfr0);
    p.put("rnto0", rnto0);
    this.oos.writeObject(p);
    log("Issued command:" + p);
    p = getResult();
    return p.get("response").toString().equals("true");
  }
  
  protected OutputStream upload3(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    Properties p = new Properties();
    p.put("command", "upload3");
    p.put("path", path);
    p.put("startPos", (new StringBuffer(String.valueOf(startPos))).toString());
    p.put("truncate", (new StringBuffer(String.valueOf(truncate))).toString());
    p.put("binary", (new StringBuffer(String.valueOf(binary))).toString());
    ServerSocket ss = new ServerSocket(0);
    ss.setSoTimeout(10000);
    p.put("port", (new StringBuffer(String.valueOf(ss.getLocalPort()))).toString());
    Socket sock_tmp = null;
    try {
      this.oos.writeObject(p);
      log("Issued command:" + p);
      sock_tmp = ss.accept();
      ss.close();
      p = getResult();
    } catch (Exception e) {
      if (sock_tmp != null)
        sock_tmp.close(); 
      throw e;
    } finally {
      ss.close();
    } 
    return sock_tmp.getOutputStream();
  }
  
  public boolean delete(String path) {
    Properties p = new Properties();
    try {
      p.put("command", "delete");
      p.put("path", path);
      this.oos.writeObject(p);
      log("Issued command:" + p);
      p = getResult();
    } catch (Exception exception) {}
    return p.get("response").toString().equals("true");
  }
  
  public boolean makedir(String path0) {
    Properties p = new Properties();
    try {
      p.put("command", "makedir");
      p.put("path", path0);
      this.oos.writeObject(p);
      log("Issued command:" + p);
      p = getResult();
    } catch (Exception exception) {}
    return p.get("response").toString().equals("true");
  }
  
  public boolean makedirs(String path) {
    Properties p = new Properties();
    try {
      p.put("command", "makedirs");
      p.put("path", path);
      this.oos.writeObject(p);
      log("Issued command:" + p);
      p = getResult();
    } catch (Exception exception) {}
    return p.get("response").toString().equals("true");
  }
  
  public void setMod(String path, String val, String param) {
    Properties p = new Properties();
    try {
      p.put("command", "setMod");
      p.put("path", path);
      p.put("val", val);
      p.put("param", param);
      this.oos.writeObject(p);
      log("Issued command:" + p);
      p = getResult();
    } catch (Exception exception) {}
  }
  
  public void setOwner(String path, String val, String param) {
    Properties p = new Properties();
    try {
      p.put("command", "setOwner");
      p.put("path", path);
      p.put("val", val);
      p.put("param", param);
      this.oos.writeObject(p);
      log("Issued command:" + p);
      p = getResult();
    } catch (Exception exception) {}
  }
  
  public void setGroup(String path, String val, String param) {
    Properties p = new Properties();
    try {
      p.put("command", "setGroup");
      p.put("path", path);
      p.put("val", val);
      p.put("param", param);
      this.oos.writeObject(p);
      log("Issued command:" + p);
      p = getResult();
    } catch (Exception exception) {}
  }
  
  public void doOSCommand(String app, String param, String val, String path) {}
  
  public String doCommand(String command) throws Exception {
    Properties p = new Properties();
    p.put("command", "doCommand");
    p.put("command2", command);
    this.oos.writeObject(p);
    log("Issued command:" + p);
    p = getResult();
    return p.getProperty("response", "");
  }
  
  private Properties getResult() throws Exception {
    Properties p = (Properties)this.ois.readObject();
    Vector log = (Vector)p.remove("log");
    for (int x = 0; x < log.size(); x++)
      log(log.remove(0).toString()); 
    if (p.get("error") != null)
      throw new Exception((Exception)p.get("error")); 
    return p;
  }
}
