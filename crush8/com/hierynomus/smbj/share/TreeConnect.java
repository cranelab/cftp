package com.hierynomus.smbj.share;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.mserref.NtStatus;
import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.mssmb2.SMB2ShareCapabilities;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.mssmb2.messages.SMB2TreeDisconnect;
import com.hierynomus.protocol.commons.concurrent.Futures;
import com.hierynomus.protocol.transport.TransportException;
import com.hierynomus.smbj.common.SmbPath;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.event.SMBEventBus;
import com.hierynomus.smbj.event.TreeDisconnected;
import com.hierynomus.smbj.session.Session;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TreeConnect {
  private long treeId;
  
  private SmbPath smbPath;
  
  private Session session;
  
  private final Set<SMB2ShareCapabilities> capabilities;
  
  private Connection connection;
  
  private final SMBEventBus bus;
  
  private final Set<AccessMask> maximalAccess;
  
  public TreeConnect(long treeId, SmbPath smbPath, Session session, Set<SMB2ShareCapabilities> capabilities, Connection connection, SMBEventBus bus, Set<AccessMask> maximalAccess) {
    this.treeId = treeId;
    this.smbPath = smbPath;
    this.session = session;
    this.capabilities = capabilities;
    this.connection = connection;
    this.bus = bus;
    this.maximalAccess = maximalAccess;
  }
  
  Connection getConnection() {
    return this.connection;
  }
  
  void close() throws TransportException {
    try {
      SMB2TreeDisconnect disconnect = new SMB2TreeDisconnect(this.connection.getNegotiatedProtocol().getDialect(), this.session.getSessionId(), this.treeId);
      Future<SMB2Packet> send = this.session.send(disconnect);
      SMB2Packet smb2Packet = Futures.<SMB2Packet, TransportException>get(send, this.connection.getConfig().getTransactTimeout(), TimeUnit.MILLISECONDS, TransportException.Wrapper);
      if (!NtStatus.isSuccess(smb2Packet.getHeader().getStatusCode()))
        throw new SMBApiException(smb2Packet.getHeader(), "Error closing connection to " + this.smbPath); 
    } finally {
      this.bus.publish(new TreeDisconnected(this.session.getSessionId(), this.treeId));
    } 
  }
  
  public String getShareName() {
    return this.smbPath.getShareName();
  }
  
  public long getTreeId() {
    return this.treeId;
  }
  
  public Session getSession() {
    return this.session;
  }
  
  public Set<AccessMask> getMaximalAccess() {
    return this.maximalAccess;
  }
  
  public boolean isDfsShare() {
    return this.capabilities.contains(SMB2ShareCapabilities.SMB2_SHARE_CAP_DFS);
  }
  
  public boolean isCAShare() {
    return this.capabilities.contains(SMB2ShareCapabilities.SMB2_SHARE_CAP_CONTINUOUS_AVAILABILITY);
  }
  
  public boolean isScaleoutShare() {
    return this.capabilities.contains(SMB2ShareCapabilities.SMB2_SHARE_CAP_SCALEOUT);
  }
  
  public String toString() {
    return String.format("TreeConnect[%s](%s)", new Object[] { Long.valueOf(this.treeId), this.smbPath });
  }
}
