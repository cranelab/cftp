package com.hierynomus.smbj;

import com.hierynomus.protocol.commons.IOUtils;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.event.ConnectionClosed;
import com.hierynomus.smbj.event.SMBEventBus;
import com.hierynomus.smbj.paths.DFSPathResolver;
import com.hierynomus.smbj.paths.PathResolver;
import com.hierynomus.smbj.paths.SymlinkPathResolver;
import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.engio.mbassy.listener.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SMBClient implements Closeable {
  public static final int DEFAULT_PORT = 445;
  
  private Map<String, Connection> connectionTable = new ConcurrentHashMap<>();
  
  private SmbConfig config;
  
  private SMBEventBus bus;
  
  private PathResolver pathResolver;
  
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
    this.pathResolver = new SymlinkPathResolver(PathResolver.LOCAL);
    if (config.isDfsEnabled())
      this.pathResolver = new DFSPathResolver(this.pathResolver); 
  }
  
  public Connection connect(String hostname) throws IOException {
    return getEstablishedOrConnect(hostname, 445);
  }
  
  public Connection connect(String hostname, int port) throws IOException {
    return getEstablishedOrConnect(hostname, port);
  }
  
  public PathResolver getPathResolver() {
    return this.pathResolver;
  }
  
  private Connection getEstablishedOrConnect(String hostname, int port) throws IOException {
    synchronized (this) {
      String hostPort = hostname + ":" + port;
      Connection cachedConnection = this.connectionTable.get(hostPort);
      if (cachedConnection == null || !cachedConnection.isConnected()) {
        Connection connection = new Connection(this.config, this, this.bus);
        try {
          connection.connect(hostname, port);
        } catch (IOException e) {
          IOUtils.closeSilently(new AutoCloseable[] { connection });
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
      String hostPort = event.getHostname() + ":" + event.getPort();
      this.connectionTable.remove(hostPort);
      logger.debug("Connection to << {} >> closed", hostPort);
    } 
  }
  
  private static final Logger logger = LoggerFactory.getLogger(SMBClient.class);
  
  public void close() {
    logger.info("Going to close all remaining connections");
    for (Connection connection : this.connectionTable.values()) {
      try {
        connection.close();
      } catch (Exception e) {
        logger.debug("Error closing connection to host {}", connection.getRemoteHostname());
        logger.debug("Exception was: ", e);
      } 
    } 
  }
}
