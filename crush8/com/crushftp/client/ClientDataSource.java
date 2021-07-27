package com.crushftp.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.Vector;
import javax.activation.DataSource;
import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;

public class ClientDataSource implements DataSource {
  GenericClient c;
  
  MimetypesFileTypeMap mimeTypes = (MimetypesFileTypeMap)FileTypeMap.getDefaultFileTypeMap();
  
  VRL vrl = null;
  
  public ClientDataSource(VRL vrl, Properties prefs) {
    try {
      this.vrl = vrl;
      this.c = Common.getClientReplication(vrl.toString(), "ClientDataSource", new Vector());
      this.c.setupConfig(prefs, null);
      this.c.login(vrl.getUsername(), vrl.getPassword(), "ClientDataSource");
    } catch (Exception e) {
      Common.log("SMTP", 1, e);
    } 
  }
  
  public String getContentType() {
    return this.mimeTypes.getContentType(getName());
  }
  
  public InputStream getInputStream() throws IOException {
    try {
      return this.c.download(this.vrl.getPath(), -1L, -1L, true);
    } catch (IOException e) {
      Common.log("SMTP", 1, e);
      throw e;
    } catch (Exception e) {
      Common.log("SMTP", 1, e);
      return null;
    } 
  }
  
  public String getName() {
    return this.vrl.getName();
  }
  
  public OutputStream getOutputStream() throws IOException {
    return null;
  }
  
  public void setFileTypeMap(MimetypesFileTypeMap mimeTypes) {
    this.mimeTypes = mimeTypes;
  }
  
  public void logout() throws Exception {
    this.c.logout();
  }
}
