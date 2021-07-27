package com.hierynomus.smbj.auth;

import java.util.Arrays;

public class AuthenticationContext {
  private final String username;
  
  private final char[] password;
  
  private final String domain;
  
  public AuthenticationContext(String username, char[] password, String domain) {
    this.username = username;
    this.password = Arrays.copyOf(password, password.length);
    this.domain = domain;
  }
  
  public static AuthenticationContext anonymous() {
    return new AuthenticationContext("", new char[0], null);
  }
  
  public static AuthenticationContext guest() {
    return new AuthenticationContext("Guest", new char[0], null);
  }
  
  public String getUsername() {
    return this.username;
  }
  
  public char[] getPassword() {
    return this.password;
  }
  
  public String getDomain() {
    return this.domain;
  }
  
  public String toString() {
    return "AuthenticationContext[" + this.username + '@' + this.domain + ']';
  }
}
