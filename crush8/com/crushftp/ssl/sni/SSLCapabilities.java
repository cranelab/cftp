package com.crushftp.ssl.sni;

import java.util.List;

public abstract class SSLCapabilities {
  public abstract String getRecordVersion();
  
  public abstract String getHelloVersion();
  
  public abstract List getServerNames();
}
