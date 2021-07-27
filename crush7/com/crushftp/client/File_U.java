package com.crushftp.client;

import java.io.File;
import java.net.URI;

public class File_U extends File {
  private static final long serialVersionUID = 1L;
  
  String root = "";
  
  public File_U(File f, String root) {
    super(f.getPath());
    this.root = root;
    File_S.validate(root, this);
  }
  
  public File_U(String s, String root) {
    super(s);
    this.root = root;
    File_S.validate(root, this);
  }
  
  public File_U(URI u, String root) {
    super(u);
    this.root = root;
    File_S.validate(root, this);
  }
  
  public File_U(File f) {
    super(f.getPath());
    this.root = System.getProperty("crushftp.user.root", "");
    File_S.validate(this.root, this);
  }
  
  public File_U(String s) {
    super(s);
    this.root = System.getProperty("crushftp.user.root", "");
    File_S.validate(this.root, this);
  }
  
  public File_U(URI u) {
    super(u);
    this.root = System.getProperty("crushftp.user.root", "");
    File_S.validate(this.root, this);
  }
  
  public File[] listFiles() {
    File[] files = super.listFiles();
    if (files == null)
      return null; 
    File_U[] files2 = new File_U[files.length];
    for (int x = 0; x < files.length; x++)
      files2[x] = new File_U(files[x], this.root); 
    return (File[])files2;
  }
  
  public static File[] listRoots() {
    File[] files = File.listRoots();
    if (files == null)
      return null; 
    File_U[] files2 = new File_U[files.length];
    for (int x = 0; x < files.length; x++)
      files2[x] = new File_U(files[x], System.getProperty("crushftp.user.root", "")); 
    return (File[])files2;
  }
  
  public File getParentFile() {
    File f = super.getParentFile();
    if (f == null)
      return f; 
    return new File_U(f, this.root);
  }
  
  public boolean equals(Object o) {
    return super.equals(o);
  }
  
  public int hashCode() {
    return super.hashCode();
  }
}
