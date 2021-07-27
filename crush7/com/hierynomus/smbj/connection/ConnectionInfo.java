package com.hierynomus.smbj.connection;

import com.hierynomus.mssmb2.SMB2GlobalCapability;
import com.hierynomus.mssmb2.messages.SMB2NegotiateResponse;
import com.hierynomus.ntlm.messages.WindowsVersion;
import com.hierynomus.protocol.commons.EnumWithValue;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.UUID;

public class ConnectionInfo {
  private WindowsVersion windowsVersion;
  
  private byte[] gssNegotiateToken;
  
  private UUID serverGuid;
  
  private String serverName;
  
  private NegotiatedProtocol negotiatedProtocol;
  
  private UUID clientGuid = UUID.randomUUID();
  
  private EnumSet<SMB2GlobalCapability> clientCapabilities;
  
  private EnumSet<SMB2GlobalCapability> serverCapabilities;
  
  private int clientSecurityMode;
  
  private int serverSecurityMode;
  
  private String server;
  
  private String preauthIntegrityHashId;
  
  private byte[] preauthIntegrityHashValue;
  
  private String cipherId;
  
  ConnectionInfo(UUID clientGuid, String serverName) {
    this.clientGuid = clientGuid;
    this.gssNegotiateToken = new byte[0];
    this.serverName = serverName;
    this.clientCapabilities = EnumSet.of(SMB2GlobalCapability.SMB2_GLOBAL_CAP_DFS);
  }
  
  void negotiated(SMB2NegotiateResponse response) {
    this.serverGuid = response.getServerGuid();
    this.serverCapabilities = EnumWithValue.EnumUtils.toEnumSet(response.getCapabilities(), SMB2GlobalCapability.class);
    this.negotiatedProtocol = new NegotiatedProtocol(response.getDialect(), response.getMaxTransactSize(), response.getMaxReadSize(), response.getMaxWriteSize(), this.serverCapabilities.contains(SMB2GlobalCapability.SMB2_GLOBAL_CAP_LARGE_MTU));
    this.serverSecurityMode = response.getSecurityMode();
  }
  
  public UUID getClientGuid() {
    return this.clientGuid;
  }
  
  public boolean isServerRequiresSigning() {
    return ((this.serverSecurityMode & 0x2) > 0);
  }
  
  public boolean isServerSigningEnabled() {
    return ((this.serverSecurityMode & 0x1) > 0);
  }
  
  public NegotiatedProtocol getNegotiatedProtocol() {
    return this.negotiatedProtocol;
  }
  
  public byte[] getGssNegotiateToken() {
    return Arrays.copyOf(this.gssNegotiateToken, this.gssNegotiateToken.length);
  }
  
  public UUID getServerGuid() {
    return this.serverGuid;
  }
  
  public String getServerName() {
    return this.serverName;
  }
  
  public boolean supports(SMB2GlobalCapability capability) {
    return this.serverCapabilities.contains(capability);
  }
  
  public EnumSet<SMB2GlobalCapability> getClientCapabilities() {
    return this.clientCapabilities;
  }
  
  public WindowsVersion getWindowsVersion() {
    return this.windowsVersion;
  }
  
  public void setWindowsVersion(WindowsVersion windowsVersion) {
    this.windowsVersion = windowsVersion;
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder("ConnectionInfo{\n");
    sb.append("  serverGuid=").append(this.serverGuid).append(",\n");
    sb.append("  serverName='").append(this.serverName).append("',\n");
    sb.append("  negotiatedProtocol=").append(this.negotiatedProtocol).append(",\n");
    sb.append("  clientGuid=").append(this.clientGuid).append(",\n");
    sb.append("  clientCapabilities=").append(this.clientCapabilities).append(",\n");
    sb.append("  serverCapabilities=").append(this.serverCapabilities).append(",\n");
    sb.append("  clientSecurityMode=").append(this.clientSecurityMode).append(",\n");
    sb.append("  serverSecurityMode=").append(this.serverSecurityMode).append(",\n");
    sb.append("  server='").append(this.server).append("'\n");
    sb.append('}');
    return sb.toString();
  }
}
