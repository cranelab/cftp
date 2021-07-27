package com.hierynomus.smbj;

import com.hierynomus.mssmb2.SMB2Dialect;
import com.hierynomus.protocol.commons.Factory;
import com.hierynomus.protocol.commons.socket.ProxySocketFactory;
import com.hierynomus.security.SecurityProvider;
import com.hierynomus.security.jce.JceSecurityProvider;
import com.hierynomus.smb.SMBPacket;
import com.hierynomus.smbj.auth.Authenticator;
import com.hierynomus.smbj.auth.NtlmAuthenticator;
import com.hierynomus.smbj.transport.TransportLayerFactory;
import com.hierynomus.smbj.transport.tcp.direct.DirectTcpTransportFactory;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.net.SocketFactory;

public final class SmbConfig {
  private static final int DEFAULT_BUFFER_SIZE = 1048576;
  
  private static final int DEFAULT_SO_TIMEOUT = 0;
  
  private static final TimeUnit DEFAULT_SO_TIMEOUT_UNIT = TimeUnit.SECONDS;
  
  private static final int DEFAULT_TIMEOUT = 60;
  
  private static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS;
  
  private static final TransportLayerFactory<SMBPacket<?>> DEFAULT_TRANSPORT_LAYER_FACTORY = new DirectTcpTransportFactory<SMBPacket<?>>();
  
  private Set<SMB2Dialect> dialects;
  
  private List<Factory.Named<Authenticator>> authenticators;
  
  private SocketFactory socketFactory;
  
  private Random random;
  
  private UUID clientGuid;
  
  private boolean signingRequired;
  
  private boolean dfsEnabled;
  
  private boolean useMultiProtocolNegotiate;
  
  private SecurityProvider securityProvider;
  
  private int readBufferSize;
  
  private long readTimeout;
  
  private int writeBufferSize;
  
  private long writeTimeout;
  
  private int transactBufferSize;
  
  private TransportLayerFactory<SMBPacket<?>> transportLayerFactory;
  
  private long transactTimeout;
  
  private int soTimeout;
  
  public static SmbConfig createDefaultConfig() {
    return builder().build();
  }
  
  public static Builder builder() {
    return (new Builder())
      .withClientGuid(UUID.randomUUID())
      .withRandomProvider(new SecureRandom())
      .withSecurityProvider(new JceSecurityProvider())
      .withSocketFactory(new ProxySocketFactory())
      .withSigningRequired(false)
      .withDfsEnabled(false)
      .withMultiProtocolNegotiate(false)
      .withBufferSize(1048576)
      .withTransportLayerFactory(DEFAULT_TRANSPORT_LAYER_FACTORY)
      .withSoTimeout(0L, DEFAULT_SO_TIMEOUT_UNIT)
      .withDialects(new SMB2Dialect[] { SMB2Dialect.SMB_2_1, SMB2Dialect.SMB_2_0_2 }).withAuthenticators((Factory.Named<Authenticator>[])new Factory.Named[] { new NtlmAuthenticator.Factory() }).withTimeout(60L, DEFAULT_TIMEOUT_UNIT);
  }
  
  private SmbConfig() {
    this.dialects = EnumSet.noneOf(SMB2Dialect.class);
    this.authenticators = new ArrayList<Factory.Named<Authenticator>>();
  }
  
  private SmbConfig(SmbConfig other) {
    this();
    this.dialects.addAll(other.dialects);
    this.authenticators.addAll(other.authenticators);
    this.socketFactory = other.socketFactory;
    this.random = other.random;
    this.clientGuid = other.clientGuid;
    this.signingRequired = other.signingRequired;
    this.dfsEnabled = other.dfsEnabled;
    this.securityProvider = other.securityProvider;
    this.readBufferSize = other.readBufferSize;
    this.readTimeout = other.readTimeout;
    this.writeBufferSize = other.writeBufferSize;
    this.writeTimeout = other.writeTimeout;
    this.transactBufferSize = other.transactBufferSize;
    this.transactTimeout = other.transactTimeout;
    this.transportLayerFactory = other.transportLayerFactory;
    this.soTimeout = other.soTimeout;
    this.useMultiProtocolNegotiate = other.useMultiProtocolNegotiate;
  }
  
  public Random getRandomProvider() {
    return this.random;
  }
  
  public SecurityProvider getSecurityProvider() {
    return this.securityProvider;
  }
  
  public Set<SMB2Dialect> getSupportedDialects() {
    return EnumSet.copyOf(this.dialects);
  }
  
  public UUID getClientGuid() {
    return this.clientGuid;
  }
  
  public List<Factory.Named<Authenticator>> getSupportedAuthenticators() {
    return new ArrayList<Factory.Named<Authenticator>>(this.authenticators);
  }
  
  public boolean isSigningRequired() {
    return this.signingRequired;
  }
  
  public boolean isDfsEnabled() {
    return this.dfsEnabled;
  }
  
  public boolean isUseMultiProtocolNegotiate() {
    return this.useMultiProtocolNegotiate;
  }
  
  public int getReadBufferSize() {
    return this.readBufferSize;
  }
  
  public long getReadTimeout() {
    return this.readTimeout;
  }
  
  public int getWriteBufferSize() {
    return this.writeBufferSize;
  }
  
  public long getWriteTimeout() {
    return this.writeTimeout;
  }
  
  public int getTransactBufferSize() {
    return this.transactBufferSize;
  }
  
  public long getTransactTimeout() {
    return this.transactTimeout;
  }
  
  public TransportLayerFactory<SMBPacket<?>> getTransportLayerFactory() {
    return this.transportLayerFactory;
  }
  
  public int getSoTimeout() {
    return this.soTimeout;
  }
  
  public SocketFactory getSocketFactory() {
    return this.socketFactory;
  }
  
  public static class Builder {
    private SmbConfig config = new SmbConfig(null, null);
    
    public Builder withRandomProvider(Random random) {
      if (random == null)
        throw new IllegalArgumentException("Random provider may not be null"); 
      this.config.random = random;
      return this;
    }
    
    public Builder withSecurityProvider(SecurityProvider securityProvider) {
      if (securityProvider == null)
        throw new IllegalArgumentException("Security provider may not be null"); 
      this.config.securityProvider = securityProvider;
      return this;
    }
    
    public Builder withSocketFactory(SocketFactory socketFactory) {
      if (socketFactory == null)
        throw new IllegalArgumentException("Socket factory may not be null"); 
      this.config.socketFactory = socketFactory;
      return this;
    }
    
    public Builder withDialects(SMB2Dialect... dialects) {
      return withDialects(Arrays.asList(dialects));
    }
    
    public Builder withDialects(Iterable<SMB2Dialect> dialects) {
      if (dialects == null)
        throw new IllegalArgumentException("Dialects may not be null"); 
      this.config.dialects.clear();
      for (SMB2Dialect dialect : dialects) {
        if (dialect == null)
          throw new IllegalArgumentException("Dialect may not be null"); 
        this.config.dialects.add(dialect);
      } 
      return this;
    }
    
    public Builder withClientGuid(UUID clientGuid) {
      if (clientGuid == null)
        throw new IllegalArgumentException("Client GUID may not be null"); 
      this.config.clientGuid = clientGuid;
      return this;
    }
    
    public Builder withAuthenticators(Factory.Named... authenticators) {
      return withAuthenticators(Arrays.asList((Factory.Named<Authenticator>[])authenticators));
    }
    
    public Builder withAuthenticators(Iterable<Factory.Named<Authenticator>> authenticators) {
      if (authenticators == null)
        throw new IllegalArgumentException("Authenticators may not be null"); 
      this.config.authenticators.clear();
      for (Factory.Named<Authenticator> authenticator : authenticators) {
        if (authenticator == null)
          throw new IllegalArgumentException("Authenticator may not be null"); 
        this.config.authenticators.add(authenticator);
      } 
      return this;
    }
    
    public Builder withSigningRequired(boolean signingRequired) {
      this.config.signingRequired = signingRequired;
      return this;
    }
    
    public Builder withReadBufferSize(int readBufferSize) {
      if (readBufferSize <= 0)
        throw new IllegalArgumentException("Read buffer size must be greater than zero"); 
      this.config.readBufferSize = readBufferSize;
      return this;
    }
    
    public Builder withReadTimeout(long timeout, TimeUnit timeoutUnit) {
      this.config.readTimeout = timeoutUnit.toMillis(timeout);
      return this;
    }
    
    public Builder withWriteBufferSize(int writeBufferSize) {
      if (writeBufferSize <= 0)
        throw new IllegalArgumentException("Write buffer size must be greater than zero"); 
      this.config.writeBufferSize = writeBufferSize;
      return this;
    }
    
    public Builder withWriteTimeout(long timeout, TimeUnit timeoutUnit) {
      this.config.writeTimeout = timeoutUnit.toMillis(timeout);
      return this;
    }
    
    public Builder withTransactBufferSize(int transactBufferSize) {
      if (transactBufferSize <= 0)
        throw new IllegalArgumentException("Transact buffer size must be greater than zero"); 
      this.config.transactBufferSize = transactBufferSize;
      return this;
    }
    
    public Builder withTransactTimeout(long timeout, TimeUnit timeoutUnit) {
      this.config.transactTimeout = timeoutUnit.toMillis(timeout);
      return this;
    }
    
    public Builder withNegotiatedBufferSize() {
      return withBufferSize(2147483647);
    }
    
    public Builder withBufferSize(int bufferSize) {
      if (bufferSize <= 0)
        throw new IllegalArgumentException("Buffer size must be greater than zero"); 
      return withReadBufferSize(bufferSize).withWriteBufferSize(bufferSize).withTransactBufferSize(bufferSize);
    }
    
    public Builder withTransportLayerFactory(TransportLayerFactory<SMBPacket<?>> transportLayerFactory) {
      if (transportLayerFactory == null)
        throw new IllegalArgumentException("Transport layer factory may not be null"); 
      this.config.transportLayerFactory = transportLayerFactory;
      return this;
    }
    
    public Builder withTimeout(long timeout, TimeUnit timeoutUnit) {
      return withReadTimeout(timeout, timeoutUnit).withWriteTimeout(timeout, timeoutUnit).withTransactTimeout(timeout, timeoutUnit);
    }
    
    public Builder withSoTimeout(int timeout) {
      return withSoTimeout(timeout, TimeUnit.MILLISECONDS);
    }
    
    public Builder withSoTimeout(long timeout, TimeUnit timeoutUnit) {
      if (timeout < 0L)
        throw new IllegalArgumentException("Socket timeout should be either 0 (no timeout) or a positive value"); 
      long timeoutMillis = timeoutUnit.toMillis(timeout);
      if (timeoutMillis > 2147483647L)
        throw new IllegalArgumentException("Socket timeout should be less than 2147483647ms"); 
      this.config.soTimeout = (int)timeoutMillis;
      return this;
    }
    
    public SmbConfig build() {
      if (this.config.dialects.isEmpty())
        throw new IllegalStateException("At least one SMB dialect should be specified"); 
      return new SmbConfig(this.config, null, null);
    }
    
    public Builder withDfsEnabled(boolean dfsEnabled) {
      this.config.dfsEnabled = dfsEnabled;
      return this;
    }
    
    public Builder withMultiProtocolNegotiate(boolean useMultiProtocolNegotiate) {
      this.config.useMultiProtocolNegotiate = useMultiProtocolNegotiate;
      return this;
    }
  }
}
