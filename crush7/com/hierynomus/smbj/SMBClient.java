package com.hierynomus.smbj;

import com.hierynomus.protocol.commons.IOUtils;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.event.ConnectionClosed;
import com.hierynomus.smbj.event.SMBEventBus;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.engio.mbassy.listener.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SMBClient {
  public static final int DEFAULT_PORT = 445;
  
  private Map<String, Connection> connectionTable = new ConcurrentHashMap<String, Connection>();
  
  private SmbConfig config;
  
  private SMBEventBus bus;
  
  public SMBClient() {
    this(SmbConfig.createDefaultConfig());
  }
  
  public SMBClient(SmbConfig config) {
    this(config, new SMBEventBus());
  }
  
  public SMBClient(SmbConfig config, SMBEventBus bus) {
    this.config = config;
    this.bus = bus;
    bus.subscribe(this);
  }
  
  public Connection connect(String hostname) throws IOException {
    return getEstablishedOrConnect(hostname, 445);
  }
  
  public Connection connect(String hostname, int port) throws IOException {
    return getEstablishedOrConnect(hostname, port);
  }
  
  private Connection getEstablishedOrConnect(String hostname, int port) throws IOException {
    synchronized (this) {
      String hostPort = String.valueOf(hostname) + ":" + port;
      Connection cachedConnection = this.connectionTable.get(hostPort);
      if (cachedConnection == null || !cachedConnection.isConnected()) {
        Connection connection = new Connection(this.config, this, this.bus);
        try {
          connection.connect(hostname, port);
        } catch (IOException e) {
          IOUtils.closeSilently(connection);
          throw e;
        } 
        this.connectionTable.put(hostPort, connection);
        return connection;
      } 
      return this.connectionTable.get(hostPort);
    } 
  }
  
  @Handler
  private void connectionClosed(ConnectionClosed event) {
    synchronized (this) {
      String hostPort = String.valueOf(event.getHostname()) + ":" + event.getPort();
      this.connectionTable.remove(hostPort);
      log.debug("Connection to << {} >> closed", hostPort);
    } 
  }
  
  private static final Logger log = LoggerFactory.getLogger(SMBClient.class);
}
