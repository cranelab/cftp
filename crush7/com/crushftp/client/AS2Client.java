package com.crushftp.client;

import crushftp.server.As2Msg;
import crushftp.server.VFS;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class AS2Client extends GenericClient {
  Properties as2Info = new Properties();
  
  public AS2Client(String url, String header, Vector log) {
    super(header, log);
    this.url = url;
  }
  
  public Properties stat(String path) throws Exception {
    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1); 
    Properties dir_item = new Properties();
    dir_item.put("url", String.valueOf(this.url) + path);
    dir_item.put("local", "false");
    dir_item.put("protocol", "as2");
    dir_item.put("type", "FILE");
    dir_item.put("permissions", "drwxrwxrwx");
    dir_item.put("num_items", "0");
    dir_item.put("owner", "owner");
    dir_item.put("group", "group");
    dir_item.put("size", "0");
    dir_item.put("modified", "0");
    dir_item.put("month", "1");
    dir_item.put("day", "1");
    dir_item.put("time_or_year", "12:00");
    dir_item.put("name", "test");
    dir_item.put("local", "false");
    dir_item.put("dir", path);
    return dir_item;
  }
  
  public Vector list(String path, Vector list) throws Exception {
    return list;
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary) throws Exception {
    return null;
  }
  
  protected OutputStream upload3(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    String filePath = path;
    try {
      null.OO outStream = new null.OO(this);
      Thread t = new Thread(new Runnable(this, outStream, filePath) {
            final AS2Client this$0;
            
            private final OO val$outStream;
            
            private final String val$filePath;
            
            class OO extends ByteArrayOutputStream {
              boolean isOpen;
              
              final AS2Client this$0;
              
              public OO(AS2Client.null this$0) throws IOException {
                this.this$0 = (AS2Client)this$0;
                this.isOpen = true;
              }
              
              public void close() throws IOException {
                super.close();
                this.isOpen = false;
              }
              
              public Object getData() {
                return toByteArray();
              }
            }
            
            public void run() {
              try {
                VFS uVFS = (VFS)this.this$0.config.get("uVFS");
                Properties vItem2 = (Properties)this.this$0.config.clone();
                Properties user_info = uVFS.user_info;
                while (this.val$outStream.isOpen)
                  Thread.sleep(100L); 
                As2Msg m = new As2Msg();
                Properties msgInfo = new Properties();
                String recipientUrl = user_info.getProperty("proxy_as2RecipientUrl".toLowerCase(), "http://127.0.0.1/");
                Properties otherParams = new Properties();
                if (recipientUrl.indexOf("#") >= 0) {
                  String[] s = recipientUrl.substring(recipientUrl.indexOf("#") + 1).split("&");
                  recipientUrl = recipientUrl.substring(0, recipientUrl.indexOf("#"));
                  for (int x = 0; x < s.length; x++)
                    otherParams.put(s[x].split("=")[0].toUpperCase().trim(), s[x].split("=")[1].trim()); 
                } 
                VRL vrl = new VRL(vItem2.getProperty("as2RecipientUrl", recipientUrl));
                Socket sock = Common.getSocket("HTTP", vrl, this.this$0.config.getProperty("use_dmz", "false"), "");
                Object outData = m.createMessage(msgInfo, this.val$outStream.getData(), vItem2.getProperty("as2Compress", user_info.getProperty("proxy_as2Compress".toLowerCase(), "false")).equals("true"), Integer.parseInt(As2Msg.algorithmLookup.getProperty(vItem2.getProperty("as2SignType", user_info.getProperty("proxy_as2SignType".toLowerCase(), "sha1")).toLowerCase())), vItem2.getProperty("as2SignKeystoreFormat", user_info.getProperty("proxy_as2SignKeystoreFormat".toLowerCase(), "JKS")), vItem2.getProperty("as2SignKeystorePath", user_info.getProperty("proxy_as2SignKeystorePath".toLowerCase(), "keystore")), vItem2.getProperty("as2SignKeystorePassword", user_info.getProperty("proxy_as2SignKeystorePassword".toLowerCase(), "changeit")), vItem2.getProperty("as2SignKeyPassword", user_info.getProperty("proxy_as2SignKeyPassword".toLowerCase(), "changeit")), vItem2.getProperty("as2SignKeyAlias", user_info.getProperty("proxy_as2SignKeyAlias".toLowerCase(), "myKey")), Integer.parseInt(As2Msg.algorithmLookup.getProperty(vItem2.getProperty("as2EncryptType", user_info.getProperty("proxy_as2EncryptType".toLowerCase(), "3des")).toLowerCase())), vItem2.getProperty("as2EncryptKeystoreFormat", user_info.getProperty("proxy_as2EncryptKeystoreFormat".toLowerCase(), "JKS")), vItem2.getProperty("as2EncryptKeystorePath", user_info.getProperty("proxy_as2EncryptKeystorePath".toLowerCase(), "keystore")), vItem2.getProperty("as2EncryptKeystorePassword", user_info.getProperty("proxy_as2EncryptKeystorePassword".toLowerCase(), "changeit")), vItem2.getProperty("as2EncryptKeyAlias", user_info.getProperty("proxy_as2EncryptKeyAlias".toLowerCase(), "myKey")), Common.last(this.val$filePath), otherParams);
                Properties mdnInfo = m.doPost(sock, uVFS, user_info.getProperty("user_name"), user_info.getProperty("current_password"), vItem2.getProperty("expect100", user_info.getProperty("proxy_expect100".toLowerCase(), "false")).equals("true"), outData, vItem2.getProperty("as2FromPartner", user_info.getProperty("proxy_as2FromPartner".toLowerCase(), "")), vItem2.getProperty("as2ToPartner", user_info.getProperty("proxy_as2ToPartner".toLowerCase(), "")), vItem2.getProperty("as2Subject", user_info.getProperty("proxy_as2Subject".toLowerCase(), "AS2 Message")), vItem2.getProperty("as2From", user_info.getProperty("proxy_as2From".toLowerCase(), "AS2 User")), vItem2.getProperty("as2RecipientUrl", recipientUrl), vItem2.getProperty("as2ResponseUrl", user_info.getProperty("proxy_as2ResponseUrl".toLowerCase(), "http://127.0.0.1/")), vItem2.getProperty("as2AsyncMdn", user_info.getProperty("proxy_as2AsyncMdn".toLowerCase(), "false")).equals("true"), vItem2.getProperty("as2SignKeystorePath", user_info.getProperty("proxy_as2SignKeystorePath".toLowerCase(), "keystore")), vItem2.getProperty("as2SignKeystorePassword", user_info.getProperty("proxy_as2SignKeystorePassword".toLowerCase(), "changeit")), vItem2.getProperty("as2SignKeyPassword", user_info.getProperty("proxy_as2SignKeyPassword".toLowerCase(), "changeit")), true, null, null, null, "", "", "", "", "", otherParams, "");
                if (mdnInfo != null) {
                  this.this$0.as2Info.put("mdnInfo", mdnInfo);
                  this.this$0.log(mdnInfo.toString());
                  Enumeration keys = mdnInfo.keys();
                  while (keys.hasMoreElements()) {
                    String key = keys.nextElement().toString();
                    String val = mdnInfo.get(key).toString();
                    user_info.put("mdn_" + key, val);
                  } 
                } 
                this.this$0.as2Info.put("msgInfo", msgInfo);
              } catch (Exception e) {
                this.this$0.as2Info.put("error", e.toString());
                this.this$0.log(e);
              } 
            }
          });
      t.setName(String.valueOf(Thread.currentThread().getName()) + ":asyncSendAS2");
      t.start();
      this.out = outStream;
      return outStream;
    } catch (Exception e) {
      log(e);
      return null;
    } 
  }
  
  public boolean mdtm(String path, long modified) throws Exception {
    return false;
  }
  
  public boolean rename(String rnfr, String rnto) throws Exception {
    return false;
  }
  
  public boolean delete(String path) {
    return false;
  }
  
  public boolean makedir(String path) {
    return false;
  }
  
  public boolean makedirs(String path) throws Exception {
    return false;
  }
  
  public String doCommand(String command) throws Exception {
    String s = "";
    if (command.toUpperCase().startsWith("SITE PROXY_CONFIRM")) {
      log("as2Info:" + this.as2Info);
      if (this.as2Info.containsKey("mdnInfo")) {
        Properties mdnInfo = (Properties)this.as2Info.get("mdnInfo");
        log("mdnInfo:" + mdnInfo);
        log("Waiting for AS2 OID:" + mdnInfo.getProperty("oid", ""));
        log("Current pending AS2 OIDs:" + Common.System2.get("crushftp.as2_async_mdn"));
        if (mdnInfo.getProperty("async", "").equals("true") && !mdnInfo.containsKey("received-content-mic") && Common.System2.containsKey("crushftp.as2_async_mdn"))
          if (((Properties)Common.System2.get("crushftp.as2_async_mdn")).containsKey(mdnInfo.getProperty("oid", "")))
            mdnInfo.putAll((Properties)((Properties)Common.System2.get("crushftp.as2_async_mdn")).remove(mdnInfo.getProperty("oid", "")));  
        if (mdnInfo.getProperty("async", "false").equals("false") || (mdnInfo.getProperty("async", "").equals("true") && mdnInfo.containsKey("received-content-mic"))) {
          Properties msgInfo = (Properties)this.as2Info.get("msgInfo");
          String result = "SUCCESS";
          if (!msgInfo.containsKey("mic")) {
            result = "SUCCESS (MIC was not calculated because the signature is set to NONE.)";
          } else if (msgInfo.getProperty("mic").equals(mdnInfo.getProperty("received-content-mic"))) {
            result = "SUCCESS (Server's calculated MIC matches our calculated MIC.)";
          } else {
            result = "WARNING (Server calculated MIC does not match our calculated MIC.)";
          } 
          s = "200 " + result + ";;;";
          String text = mdnInfo.remove("text").toString();
          Enumeration keys = mdnInfo.keys();
          while (keys.hasMoreElements()) {
            String key = keys.nextElement().toString();
            String val = mdnInfo.getProperty(key);
            BufferedReader bufferedReader = new BufferedReader(new StringReader(val));
            String str1 = "";
            while ((str1 = bufferedReader.readLine()) != null)
              s = String.valueOf(s) + key + "=" + str1 + ";;;"; 
          } 
          String data = "";
          BufferedReader sr = new BufferedReader(new StringReader(text));
          while ((data = sr.readLine()) != null)
            s = String.valueOf(s) + data + ";;;"; 
          mdnInfo.put("text", text);
        } else {
          s = "200 WAIT";
        } 
      } else if (this.as2Info.containsKey("error")) {
        BufferedReader sr = new BufferedReader(new StringReader(this.as2Info.getProperty("error")));
        String data = "";
        s = "200 ERROR:";
        while ((data = sr.readLine()) != null)
          s = String.valueOf(s) + data + ";;;"; 
      } else {
        s = "200 WAIT";
      } 
    } else {
      s = "550 UNKNOWN FILE";
    } 
    return s;
  }
  
  public void close() throws Exception {
    if (this.in != null)
      this.in.close(); 
    if (this.out != null)
      this.out.close(); 
  }
}
