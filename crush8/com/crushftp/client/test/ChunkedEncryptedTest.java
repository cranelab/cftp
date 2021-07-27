package com.crushftp.client.test;

import com.crushftp.client.ChunkedEncryptedInputStream;
import com.crushftp.client.Common;
import com.crushftp.client.GenericClient;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ChunkedEncryptedTest implements Runnable {
  final long MB4 = 4194304L;
  
  public static void main(String[] args) {
    (new ChunkedEncryptedTest()).run();
  }
  
  public void run() {
    Common.trustEverything();
    performTest("file:///Users/spinkb/Desktop/ftp_testing/");
    System.out.println("#################################################################################");
    System.out.println("All tests completed.");
  }
  
  public void performTest(String url) {
    System.out.println("Performing test with url:" + url);
    GenericClient client = Common.getClient(url, "", null);
    try {
      client.login("test2", "test2", null);
      InputStream in = client.download("/sync_easy2/MB32.txt", 0L, -1L, true);
      in = new ChunkedEncryptedInputStream(in, -1L, "privatekey.asc", "changeit", true, true);
      Common.streamCopier(in, new FileOutputStream("/Users/spinkb/Desktop/MB32_decrypted.txt"), false);
      client.logout();
      System.out.println("Test succeeded for url:" + url);
    } catch (Exception e) {
      e.printStackTrace();
    } 
    try {
      Thread.sleep(500L);
    } catch (Exception exception) {}
  }
}
