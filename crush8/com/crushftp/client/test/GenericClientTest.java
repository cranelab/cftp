package com.crushftp.client.test;

import com.crushftp.client.Common;
import com.crushftp.client.GenericClient;
import com.crushftp.client.HADownload;
import com.crushftp.client.HAUpload;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

public class GenericClientTest implements Runnable {
  final long MB4 = 4194304L;
  
  public static void main(String[] args) {
    (new GenericClientTest()).run();
  }
  
  public void run() {
    Common.trustEverything();
    performTest("file:///Users/spinkb/Desktop/ftp_testing/");
    performTest("ftp://127.0.0.1:2121/");
    performTest("ftps://127.0.0.1:9990/");
    performTest("ftpes://127.0.0.1:2121/");
    performTest("sftp://127.0.0.1:2222/");
    performTest("http://127.0.0.1:8080/");
    performTest("https://127.0.0.1:4443/");
    performTest("webdav://127.0.0.1:8080/");
    performTest("webdavs://127.0.0.1:4443/");
    System.out.println("#################################################################################");
    System.out.println("All tests completed.");
  }
  
  public void performTest(String url) {
    System.out.println("Performing test with url:" + url);
    GenericClient client = Common.getClient(url, "", null);
    try {
      client.login("test2", "test2", null);
      client.setConfig("pasv", "false");
      try {
        client.delete("/testDir");
      } catch (Exception exception) {}
      try {
        client.delete("/MB5.txt");
      } catch (Exception exception) {}
      OutputStream out = client.upload("/MB4.txt", 0L, true, true);
      copyFile("/A_TestFiles/MB4.txt", 0L, out);
      Date date = new Date(1310000000000L);
      client.mdtm("/MB4.txt", date.getTime());
      out = client.upload("/MB5.txt", 0L, true, true);
      copyFile("/A_TestFiles/MB4.txt", 2097152L, out);
      Vector names = getItemNames(client);
      if (names.size() == 0)
        throw new Exception("Failed to get proper dir listing, size was 0."); 
      if (names.indexOf("MB4.txt") < 0)
        throw new Exception("Transfer must have failed as a dir listing doesn't show the file."); 
      if (names.indexOf("testDir") >= 0)
        throw new Exception("Delete must have failed as listing shows the testDir still exists."); 
      Properties itemInfo = client.stat("/MB4.txt");
      if (Long.parseLong(itemInfo.getProperty("size")) != 4194304L)
        throw new Exception("Server reported a different file size received:4194304 vs. " + itemInfo.getProperty("size")); 
      long diff = Math.abs(Long.parseLong(itemInfo.getProperty("modified")) - date.getTime());
      if (diff > 14000000L)
        throw new Exception("Server reported a different file time:" + diff + "    " + date.getTime() + " vs. " + itemInfo.getProperty("modified") + "   " + date + " vs. " + new Date(Long.parseLong(itemInfo.getProperty("modified")))); 
      itemInfo = client.stat("/MB5.txt");
      if (Long.parseLong(itemInfo.getProperty("size")) != 2097152L)
        throw new Exception("Server reported a different file size received:2097152 vs. " + itemInfo.getProperty("size")); 
      out = client.upload("/MB5.txt", 2097152L, false, true);
      copyFile("/A_TestFiles/MB4.txt", 2097152L, out);
      itemInfo = client.stat("/MB5.txt");
      if (Long.parseLong(itemInfo.getProperty("size")) != 4194304L)
        throw new Exception("Server reported a different file size received:4194304 vs. " + itemInfo.getProperty("size")); 
      (new File("test.txt")).delete();
      InputStream in = client.download("/MB4.txt", 0L, -1L, true);
      Common.streamCopier(in, new FileOutputStream("test.txt"), false);
      if ((new File("test.txt")).length() != 4194304L)
        throw new Exception("We got a file that was not the same size as expected:4194304 vs. " + (new File("test.txt")).length()); 
      (new File("test.txt")).delete();
      in = client.download("/MB4.txt", 2097152L, -1L, true);
      Common.streamCopier(in, new FileOutputStream("test.txt"), false);
      if ((new File("test.txt")).length() != 2097152L)
        throw new Exception("We got a file that was not the same size as expected:2097152 vs. " + (new File("test.txt")).length()); 
      in = client.download("/MB4.txt", 2097152L, -1L, true);
      Common.streamCopier(in, new FileOutputStream("test.txt", true), false);
      if ((new File("test.txt")).length() != 4194304L)
        throw new Exception("We got a file that was not the same size as expected:4194304 vs. " + (new File("test.txt")).length()); 
      try {
        client.delete("/MB5.txt");
      } catch (Exception exception) {}
      client.rename("/MB4.txt", "/MB5.txt");
      names = getItemNames(client);
      if (names.indexOf("MB4.txt") >= 0)
        throw new Exception("Rename failed, found MB4.txt still."); 
      if (names.indexOf("MB5.txt") < 0)
        throw new Exception("Rename failed, didn't find MB5.txt."); 
      client.delete("/MB5.txt");
      client.makedir("/testDir");
      names = getItemNames(client);
      if (names.indexOf("MB5.txt") >= 0)
        throw new Exception("Delete must have failed as a dir listing shows the file still exists."); 
      if (names.indexOf("testDir") < 0)
        throw new Exception("Make dir must have failed as testDir does not exist."); 
      System.out.println("Performing HA tests.");
      killClientDelayed(client, 6);
      out = new HAUpload(client, "/GB1_2.txt", -1L, false, true, 30, 1);
      copyFile("/A_TestFiles/GB1.txt", 0L, out);
      killClientDelayed(client, 4);
      in = new HADownload(client, "/GB1_2.txt", 0L, -1L, true, 1);
      Common.streamCopier(in, new FileOutputStream("test.txt", false), false);
      if ((new File("test.txt")).length() != (new File("/A_TestFiles/GB1.txt")).length())
        throw new Exception("We got a file that was not the same size as expected:" + (new File("/A_TestFiles/GB1.txt")).length() + " vs. " + (new File("test.txt")).length()); 
      client.logout();
      System.out.println("Test succeeded for url:" + url);
    } catch (Exception e) {
      e.printStackTrace();
    } 
    try {
      Thread.sleep(500L);
    } catch (Exception exception) {}
  }
  
  public void killClientDelayed(GenericClient c, int delay) {
    (new Thread(new Runnable(this, delay, c) {
          final GenericClientTest this$0;
          
          private final int val$delay;
          
          private final GenericClient val$c;
          
          public void run() {
            try {
              Thread.sleep((this.val$delay * 1000));
              System.out.println("Killing connection midstream to verify HA.");
              this.val$c.logout();
            } catch (Exception exception) {}
          }
        })).start();
  }
  
  public Vector getItemNames(GenericClient client) throws Exception {
    Vector itemNames = new Vector();
    Vector v = client.list("/", new Vector());
    for (int x = 0; x < v.size(); x++) {
      Properties p = v.elementAt(x);
      itemNames.addElement(p.getProperty("name"));
    } 
    return itemNames;
  }
  
  public static void copyFile(String path, long startPos, OutputStream out) throws Exception {
    RandomAccessFile in = new RandomAccessFile(path, "r");
    in.seek(startPos);
    byte[] b = new byte[32768];
    int bytesRead = 0;
    while (bytesRead >= 0) {
      bytesRead = in.read(b);
      if (bytesRead > 0)
        out.write(b, 0, bytesRead); 
    } 
    in.close();
    out.close();
  }
}
