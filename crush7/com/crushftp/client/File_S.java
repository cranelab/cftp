package com.crushftp.client;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;

public class File_S extends File {
  private static final long serialVersionUID = 1L;
  
  String root = "";
  
  public static Object log_lock = new Object();
  
  public static SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy HH:mm:ss.SSS");
  
  public File_S(File f, String root) {
    super(f.getPath());
    this.root = root;
    validate(root, this);
  }
  
  public File_S(String s, String root) {
    super(s);
    this.root = root;
    validate(root, this);
  }
  
  public File_S(URI u, String root) {
    super(u);
    this.root = root;
    validate(root, this);
  }
  
  public File_S(File f) {
    super(f.getPath());
    this.root = System.getProperty("crushftp.server.root", "");
    validate(this.root, this);
  }
  
  public File_S(String s) {
    super(s);
    this.root = System.getProperty("crushftp.server.root", "");
    validate(this.root, this);
  }
  
  public File_S(URI u) {
    super(u);
    this.root = System.getProperty("crushftp.server.root", "");
    validate(this.root, this);
  }
  
  public File[] listFiles() {
    File[] files = super.listFiles();
    if (files == null)
      return null; 
    File_S[] files2 = new File_S[files.length];
    for (int x = 0; x < files.length; x++)
      files2[x] = new File_S(files[x], this.root); 
    return (File[])files2;
  }
  
  public static File[] listRoots() {
    File[] files = File.listRoots();
    if (files == null)
      return null; 
    File_S[] files2 = new File_S[files.length];
    for (int x = 0; x < files.length; x++)
      files2[x] = new File_S(files[x], System.getProperty("crushftp.server.root", "")); 
    return (File[])files2;
  }
  
  public File getParentFile() {
    File f = super.getParentFile();
    if (f == null)
      return f; 
    return new File_S(f, this.root);
  }
  
  public boolean equals(Object o) {
    return super.equals(o);
  }
  
  public int hashCode() {
    return super.hashCode();
  }
  
  public static void validate(String root, File f) {
    if (root.equals(""))
      return; 
    String s = f.getPath().replace('\\', '/');
    if (s.startsWith("./") || s.indexOf("../") >= 0 || !s.startsWith("/"))
      try {
        s = f.getCanonicalPath().replace('\\', '/');
      } catch (Exception exception) {} 
    String[] roots = root.split(";");
    boolean found = false;
    for (int x = 0; x < roots.length; x++) {
      if (s.startsWith(roots[x]) || (String.valueOf(s) + "/").startsWith(roots[x]))
        found = true; 
    } 
    if (!found) {
      RuntimeException e = new RuntimeException("Invalid file location:" + s);
      if (System.getProperty("crushftp.server.file.warn", "true").equals("true"))
        e.printStackTrace(); 
      if (System.getProperty("crushftp.server.file.log", "false").equals("true"))
        synchronized (log_lock) {
          RandomAccessFile raf = null;
          try {
            raf = new RandomAccessFile("file_audit.log", "rw");
            if (raf.length() > 104857600L) {
              System.out.println((String)e);
            } else {
              raf.seek(raf.length());
              raf.write((String.valueOf(sdf.format(new Date())) + "|" + e + "\r\n").getBytes());
            } 
          } catch (Exception e1) {
            e1.printStackTrace();
          } finally {
            try {
              raf.close();
            } catch (IOException iOException) {}
          } 
        }  
      if (System.getProperty("crushftp.server.file.strict", "false").equals("true"))
        throw e; 
    } 
  }
}
