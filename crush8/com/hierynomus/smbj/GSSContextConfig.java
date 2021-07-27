package com.hierynomus.smbj;

public final class GSSContextConfig {
  private boolean requestMutualAuth;
  
  private boolean requestCredDeleg;
  
  public static GSSContextConfig createDefaultConfig() {
    return builder().build();
  }
  
  public static Builder builder() {
    return (new Builder())
      .withRequestMutualAuth(true)
      .withRequestCredDeleg(false);
  }
  
  private GSSContextConfig() {}
  
  private GSSContextConfig(GSSContextConfig other) {
    this();
    this.requestMutualAuth = other.requestMutualAuth;
    this.requestCredDeleg = other.requestCredDeleg;
  }
  
  public boolean isRequestMutualAuth() {
    return this.requestMutualAuth;
  }
  
  public boolean isRequestCredDeleg() {
    return this.requestCredDeleg;
  }
  
  public static class Builder {
    private GSSContextConfig config = new GSSContextConfig();
    
    public Builder withRequestMutualAuth(boolean requestMutualAuth) {
      this.config.requestMutualAuth = requestMutualAuth;
      return this;
    }
    
    public Builder withRequestCredDeleg(boolean requestCredDeleg) {
      this.config.requestCredDeleg = requestCredDeleg;
      return this;
    }
    
    public GSSContextConfig build() {
      return new GSSContextConfig(this.config);
    }
  }
}
