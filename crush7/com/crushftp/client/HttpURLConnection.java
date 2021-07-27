package com.crushftp.client;

import java.util.Properties;

public class HttpURLConnection extends URLConnection {
  public HttpURLConnection(VRL u, Properties config) {
    super(u, config);
  }
}
