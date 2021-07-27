package org.apache.http.client;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;

public interface CredentialsProvider {
  void setCredentials(AuthScope paramAuthScope, Credentials paramCredentials);
  
  Credentials getCredentials(AuthScope paramAuthScope);
  
  void clear();
}
