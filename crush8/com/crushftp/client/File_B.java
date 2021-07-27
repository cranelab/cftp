package com.crushftp.client;

import java.io.File;
import java.net.URI;

public class File_B extends File {
  private static final long serialVersionUID = 1L;
  
  String root1 = "";
  
  String root2 = "";
  
  public File_B(File f, String root1, String root2) {
    super(f.getPath());
    this.root1 = root1;
    this.root2 = root2;
    boolean ok = false;
    RuntimeException e2 = null;
    try {
      File_S.validate(root1, this);
      ok = true;
    } catch (RuntimeException e) {
      e2 = e;
    } 
    try {
      File_S.validate(root2, this);
      ok = true;
    } catch (RuntimeException e) {
      e2 = e;
    } 
    if (!ok)
      throw e2; 
  }
  
  public File_B(String s, String root1, String root2) {
    super(s);
    this.root1 = root1;
    this.root2 = root2;
  }
  
  public File_B(URI u, String root1, String root2) {
    super(u);
    this.root1 = root1;
    this.root2 = root2;
  }
  
  public File_B(File f) {
    super(f.getPath());
    this.root1 = System.getProperty("crushftp.user.root", "");
    this.root2 = System.getProperty("crushftp.system.root", "");
  }
  
  public File_B(File_S f) {
    super(f.getPath());
    this.root1 = System.getProperty("crushftp.user.root", "");
    this.root2 = System.getProperty("crushftp.system.root", "");
  }
  
  public File_B(File_U f) {
    super(f.getPath());
    this.root1 = System.getProperty("crushftp.user.root", "");
    this.root2 = System.getProperty("crushftp.system.root", "");
  }
  
  public File_B(String s) {
    super(s);
    this.root1 = System.getProperty("crushftp.user.root", "");
    this.root2 = System.getProperty("crushftp.system.root", "");
  }
  
  public File_B(URI u) {
    super(u);
    this.root1 = System.getProperty("crushftp.user.root", "");
    this.root2 = System.getProperty("crushftp.system.root", "");
  }
  
  public File[] listFiles() {
    File[] files = super.listFiles();
    File_B[] files2 = new File_B[files.length];
    for (int x = 0; x < files.length; x++)
      files2[x] = new File_B(files[x], this.root1, this.root2); 
    return (File[])files2;
  }
  
  public static File[] listRoots() {
    File[] files = File.listRoots();
    File_B[] files2 = new File_B[files.length];
    for (int x = 0; x < files.length; x++)
      files2[x] = new File_B(files[x], System.getProperty("crushftp.user.root", ""), System.getProperty("crushftp.system.root", "")); 
    return (File[])files2;
  }
  
  public File getParentFile() {
    return new File_B(getParent(), this.root1, this.root2);
  }
  
  public boolean equals(Object o) {
    return super.equals(o);
  }
  
  public int hashCode() {
    return super.hashCode();
  }
}
