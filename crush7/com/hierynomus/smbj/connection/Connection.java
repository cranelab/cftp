package com.hierynomus.smbj.connection;

import com.hierynomus.mserref.NtStatus;
import com.hierynomus.mssmb.SMB1MessageConverter;
import com.hierynomus.mssmb.SMB1NotSupportedException;
import com.hierynomus.mssmb.SMB1Packet;
import com.hierynomus.mssmb.messages.SMB1ComNegotiateRequest;
import com.hierynomus.mssmb2.SMB2Dialect;
import com.hierynomus.mssmb2.SMB2GlobalCapability;
import com.hierynomus.mssmb2.SMB2MessageCommandCode;
import com.hierynomus.mssmb2.SMB2MessageFlag;
import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.mssmb2.messages.SMB2CancelRequest;
import com.hierynomus.mssmb2.messages.SMB2MessageConverter;
import com.hierynomus.mssmb2.messages.SMB2NegotiateRequest;
import com.hierynomus.mssmb2.messages.SMB2NegotiateResponse;
import com.hierynomus.mssmb2.messages.SMB2SessionSetup;
import com.hierynomus.protocol.Packet;
import com.hierynomus.protocol.commons.Factory;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.protocol.commons.concurrent.CancellableFuture;
import com.hierynomus.protocol.commons.concurrent.Futures;
import com.hierynomus.protocol.transport.PacketFactory;
import com.hierynomus.protocol.transport.PacketHandlers;
import com.hierynomus.protocol.transport.PacketReceiver;
import com.hierynomus.protocol.transport.TransportException;
import com.hierynomus.protocol.transport.TransportLayer;
import com.hierynomus.smb.SMBPacket;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.auth.AuthenticateResponse;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.auth.Authenticator;
import com.hierynomus.smbj.common.SMBRuntimeException;
import com.hierynomus.smbj.event.ConnectionClosed;
import com.hierynomus.smbj.event.SMBEventBus;
import com.hierynomus.smbj.event.SessionLoggedOff;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.spnego.NegTokenInit;
import com.hierynomus.spnego.SpnegoException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import net.engio.mbassy.listener.Handler;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Connection implements PacketReceiver<SMBPacket<?>> {
  private static final Logger logger = LoggerFactory.getLogger(Connection.class);
  
  private static final DelegatingSMBMessageConverter converter = new DelegatingSMBMessageConverter((PacketFactory<?>[])new PacketFactory[] { new SMB2MessageConverter(), new SMB1MessageConverter() });
  
  private ConnectionInfo connectionInfo;
  
  private SessionTable sessionTable = new SessionTable();
  
  private SessionTable preauthSessionTable = new SessionTable();
  
  private OutstandingRequests outstandingRequests = new OutstandingRequests();
  
  private SequenceWindow sequenceWindow;
  
  private String remoteName;
  
  private SMBClient client;
  
  private SmbConfig config;
  
  private TransportLayer<SMBPacket<?>> transport;
  
  private final SMBEventBus bus;
  
  public SMBClient getClient() {
    return this.client;
  }
  
  private final ReentrantLock lock = new ReentrantLock();
  
  private int remotePort;
  
  public Connection(SmbConfig config, SMBClient client, SMBEventBus bus) {
    this.config = config;
    this.client = client;
    this.transport = config.getTransportLayerFactory().createTransportLayer(new PacketHandlers<SMBPacket<?>>(new SMBPacketSerializer<SMBPacket<?>>(), this, converter), config);
    this.bus = bus;
    bus.subscribe(this);
  }
  
  public Connection(Connection connection) {
    this.client = connection.client;
    this.config = connection.config;
    this.transport = connection.transport;
    this.bus = connection.bus;
    this.bus.subscribe(this);
  }
  
  public void connect(String hostname, int port) throws IOException {
    if (isConnected())
      throw new IllegalStateException(String.format("This connection is already connected to %s", new Object[] { getRemoteHostname() })); 
    this.remoteName = hostname;
    this.remotePort = port;
    this.transport.connect(new InetSocketAddress(hostname, port));
    this.sequenceWindow = new SequenceWindow();
    this.connectionInfo = new ConnectionInfo(this.config.getClientGuid(), hostname);
    negotiateDialect();
    logger.info("Successfully connected to: {}", getRemoteHostname());
  }
  
  public void close() throws Exception {
    close(false);
  }
  
  public void close(boolean force) throws Exception {
    try {
      if (!force)
        for (Session session : this.sessionTable.activeSessions()) {
          try {
            session.close();
          } catch (IOException e) {
            logger.warn("Exception while closing session {}", Long.valueOf(session.getSessionId()), e);
          } 
        }  
    } finally {
      this.transport.disconnect();
      logger.info("Closed connection to {}", getRemoteHostname());
      this.bus.publish(new ConnectionClosed(this.remoteName, this.remotePort));
    } 
  }
  
  public SmbConfig getConfig() {
    return this.config;
  }
  
  public Session authenticate(AuthenticationContext authContext) {
    try {
      Authenticator authenticator = getAuthenticator(authContext);
      authenticator.init(this.config.getSecurityProvider(), this.config.getRandomProvider());
      Session session = getSession(authContext);
      SMB2SessionSetup receive = authenticationRound(authenticator, authContext, this.connectionInfo.getGssNegotiateToken(), session);
      long sessionId = receive.getHeader().getSessionId();
      session.setSessionId(sessionId);
      this.preauthSessionTable.registerSession(Long.valueOf(sessionId), session);
      try {
        while (receive.getHeader().getStatus() == NtStatus.STATUS_MORE_PROCESSING_REQUIRED) {
          logger.debug("More processing required for authentication of {} using {}", authContext.getUsername(), authenticator);
          receive = authenticationRound(authenticator, authContext, receive.getSecurityBuffer(), session);
        } 
        if (receive.getHeader().getStatus() != NtStatus.STATUS_SUCCESS)
          throw new SMBApiException(receive.getHeader(), String.format("Authentication failed for '%s' using %s", new Object[] { authContext.getUsername(), authenticator })); 
        if (receive.getSecurityBuffer() != null)
          authenticator.authenticate(authContext, receive.getSecurityBuffer(), session); 
        session.init(receive);
        logger.info("Successfully authenticated {} on {}, session is {}", new Object[] { authContext.getUsername(), this.remoteName, Long.valueOf(session.getSessionId()) });
        this.sessionTable.registerSession(Long.valueOf(session.getSessionId()), session);
        return session;
      } finally {
        this.preauthSessionTable.sessionClosed(Long.valueOf(sessionId));
      } 
    } catch (SpnegoException e) {
      throw new SMBRuntimeException(e);
    } catch (IOException e) {
      throw new SMBRuntimeException(e);
    } 
  }
  
  private Session getSession(AuthenticationContext authContext) {
    return new Session(this, authContext, this.bus, this.config.isDfsEnabled(), this.config.getSecurityProvider());
  }
  
  private SMB2SessionSetup authenticationRound(Authenticator authenticator, AuthenticationContext authContext, byte[] inputToken, Session session) throws IOException {
    AuthenticateResponse resp = authenticator.authenticate(authContext, inputToken, session);
    this.connectionInfo.setWindowsVersion(resp.getWindowsVersion());
    byte[] securityContext = resp.getNegToken();
    if (resp.getSigningKey() != null)
      session.setSigningKey(resp.getSigningKey()); 
    SMB2SessionSetup req = new SMB2SessionSetup(this.connectionInfo.getNegotiatedProtocol().getDialect(), EnumSet.of(SMB2SessionSetup.SMB2SecurityMode.SMB2_NEGOTIATE_SIGNING_ENABLED), 
        this.connectionInfo.getClientCapabilities());
    req.setSecurityBuffer(securityContext);
    req.getHeader().setSessionId(session.getSessionId());
    return sendAndReceive(req);
  }
  
  private Authenticator getAuthenticator(AuthenticationContext context) throws IOException, SpnegoException {
    List<Factory.Named<Authenticator>> supportedAuthenticators = new ArrayList<Factory.Named<Authenticator>>(this.config.getSupportedAuthenticators());
    List<ASN1ObjectIdentifier> mechTypes = new ArrayList<ASN1ObjectIdentifier>();
    if ((this.connectionInfo.getGssNegotiateToken()).length > 0) {
      NegTokenInit negTokenInit = (new NegTokenInit()).read(this.connectionInfo.getGssNegotiateToken());
      mechTypes = negTokenInit.getSupportedMechTypes();
    } 
    for (Factory.Named<Authenticator> factory : (Iterable<Factory.Named<Authenticator>>)new ArrayList(supportedAuthenticators)) {
      if (mechTypes.isEmpty() || mechTypes.contains(new ASN1ObjectIdentifier(factory.getName()))) {
        Authenticator authenticator = factory.create();
        if (authenticator.supports(context))
          return authenticator; 
      } 
    } 
    throw new SMBRuntimeException("Could not find a configured authenticator for mechtypes: " + mechTypes + " and authentication context: " + context);
  }
  
  public <T extends SMB2Packet> Future<T> send(SMB2Packet packet) throws TransportException {
    this.lock.lock();
    try {
      int availableCredits = this.sequenceWindow.available();
      int grantCredits = calculateGrantedCredits(packet, availableCredits);
      if (availableCredits == 0)
        logger.warn("There are no credits left to send {}, will block until there are more credits available.", packet.getHeader().getMessage()); 
      long[] messageIds = this.sequenceWindow.get(grantCredits);
      packet.getHeader().setMessageId(messageIds[0]);
      logger.debug("Granted {} (out of {}) credits to {}", new Object[] { Integer.valueOf(grantCredits), Integer.valueOf(availableCredits), packet });
      packet.getHeader().setCreditRequest(Math.max(512 - availableCredits - grantCredits, grantCredits));
      Request request = new Request(packet.getHeader().getMessageId(), UUID.randomUUID());
      this.outstandingRequests.registerOutstanding(request);
      this.transport.write(packet);
      return (Future)request.getFuture(new CancelRequest(request));
    } finally {
      this.lock.unlock();
    } 
  }
  
  private <T extends SMB2Packet> T sendAndReceive(SMB2Packet packet) throws TransportException {
    return (T)Futures.<SMB2Packet, TransportException>get(send(packet), getConfig().getTransactTimeout(), TimeUnit.MILLISECONDS, TransportException.Wrapper);
  }
  
  private int calculateGrantedCredits(SMB2Packet packet, int availableCredits) {
    int grantCredits, maxPayloadSize = packet.getMaxPayloadSize();
    int creditsNeeded = creditsNeeded(maxPayloadSize);
    if (creditsNeeded > 1 && !this.connectionInfo.supports(SMB2GlobalCapability.SMB2_GLOBAL_CAP_LARGE_MTU)) {
      logger.trace("Connection to {} does not support multi-credit requests.", getRemoteHostname());
      grantCredits = 1;
    } else if (creditsNeeded < availableCredits) {
      grantCredits = creditsNeeded;
    } else if (creditsNeeded > 1 && availableCredits > 1) {
      grantCredits = availableCredits - 1;
    } else {
      grantCredits = 1;
    } 
    packet.setCreditsAssigned(grantCredits);
    return grantCredits;
  }
  
  private void negotiateDialect() throws TransportException {
    SMB2Packet resp;
    logger.debug("Negotiating dialects {} with server {}", this.config.getSupportedDialects(), getRemoteHostname());
    if (this.config.isUseMultiProtocolNegotiate()) {
      resp = multiProtocolNegotiate();
    } else {
      resp = smb2OnlyNegotiate();
    } 
    if (!(resp instanceof SMB2NegotiateResponse))
      throw new IllegalStateException("Expected a SMB2 NEGOTIATE Response, but got: " + resp); 
    SMB2NegotiateResponse negotiateResponse = (SMB2NegotiateResponse)resp;
    if (!negotiateResponse.getHeader().getStatus().isSuccess())
      throw new SMBApiException(negotiateResponse.getHeader(), "Failure during dialect negotiation"); 
    this.connectionInfo.negotiated(negotiateResponse);
    logger.debug("Negotiated the following connection settings: {}", this.connectionInfo);
  }
  
  private SMB2Packet smb2OnlyNegotiate() throws TransportException {
    SMB2Packet negotiatePacket = new SMB2NegotiateRequest(this.config.getSupportedDialects(), this.connectionInfo.getClientGuid(), this.config.isSigningRequired());
    return sendAndReceive(negotiatePacket);
  }
  
  private SMB2Packet multiProtocolNegotiate() throws TransportException {
    SMB1Packet negotiatePacket = new SMB1ComNegotiateRequest(this.config.getSupportedDialects());
    long l = this.sequenceWindow.get();
    if (l != 0L)
      throw new IllegalStateException("The SMBv1 SMB_COM_NEGOTIATE packet needs to be the first packet sent."); 
    Request request = new Request(l, UUID.randomUUID());
    this.outstandingRequests.registerOutstanding(request);
    this.transport.write(negotiatePacket);
    Future<SMB2Packet> future = request.getFuture(null);
    SMB2Packet packet = Futures.<SMB2Packet, TransportException>get(future, getConfig().getTransactTimeout(), TimeUnit.MILLISECONDS, TransportException.Wrapper);
    if (!(packet instanceof SMB2NegotiateResponse))
      throw new IllegalStateException("Expected a SMB2 NEGOTIATE Response to our SMB_COM_NEGOTIATE, but got: " + packet); 
    SMB2NegotiateResponse negotiateResponse = (SMB2NegotiateResponse)packet;
    if (negotiateResponse.getDialect() == SMB2Dialect.SMB_2XX)
      return smb2OnlyNegotiate(); 
    return negotiateResponse;
  }
  
  private int creditsNeeded(int payloadSize) {
    return Math.abs((payloadSize - 1) / 65536) + 1;
  }
  
  public NegotiatedProtocol getNegotiatedProtocol() {
    return this.connectionInfo.getNegotiatedProtocol();
  }
  
  public void handle(SMBPacket uncheckedPacket) throws TransportException {
    if (!(uncheckedPacket instanceof SMB2Packet))
      throw new SMB1NotSupportedException(); 
    SMB2Packet packet = (SMB2Packet)uncheckedPacket;
    long messageId = packet.getSequenceNumber();
    if (!this.outstandingRequests.isOutstanding(Long.valueOf(messageId)))
      throw new TransportException("Received response with unknown sequence number <<" + messageId + ">>"); 
    this.sequenceWindow.creditsGranted(packet.getHeader().getCreditResponse());
    logger.debug("Server granted us {} credits for {}, now available: {} credits", new Object[] { Integer.valueOf(packet.getHeader().getCreditResponse()), packet, Integer.valueOf(this.sequenceWindow.available()) });
    Request request = this.outstandingRequests.getRequestByMessageId(Long.valueOf(messageId));
    logger.trace("Send/Recv of packet {} took << {} ms >>", packet, Long.valueOf(System.currentTimeMillis() - request.getTimestamp().getTime()));
    if (packet.isIntermediateAsyncResponse()) {
      logger.debug("Received ASYNC packet {} with AsyncId << {} >>", packet, Long.valueOf(packet.getHeader().getAsyncId()));
      request.setAsyncId(packet.getHeader().getAsyncId());
      return;
    } 
    if (packet.getHeader().getStatus() == NtStatus.STATUS_NETWORK_SESSION_EXPIRED)
      return; 
    if (packet.getHeader().getSessionId() != 0L && packet.getHeader().getMessage() != SMB2MessageCommandCode.SMB2_SESSION_SETUP) {
      Session session = this.sessionTable.find(Long.valueOf(packet.getHeader().getSessionId()));
      if (session == null) {
        session = this.preauthSessionTable.find(Long.valueOf(packet.getHeader().getSessionId()));
        if (session == null) {
          logger.warn("Illegal request, no session matching the sessionId: {}", Long.valueOf(packet.getHeader().getSessionId()));
          return;
        } 
      } 
      verifyPacketSignature(packet, session);
    } 
    this.outstandingRequests.receivedResponseFor(Long.valueOf(messageId)).getPromise().deliver(packet);
  }
  
  private void verifyPacketSignature(SMB2Packet packet, Session session) throws TransportException {
    if (packet.getHeader().isFlagSet(SMB2MessageFlag.SMB2_FLAGS_SIGNED)) {
      if (!session.getPacketSignatory().verify(packet)) {
        logger.warn("Invalid packet signature for packet {}", packet);
        if (session.isSigningRequired())
          throw new TransportException("Packet signature for packet " + packet + " was not correct"); 
      } 
    } else if (session.isSigningRequired()) {
      logger.warn("Illegal request, session requires message signing, but packet {} is not signed.", packet);
      throw new TransportException("Session requires signing, but packet " + packet + " was not signed");
    } 
  }
  
  public void handleError(Throwable t) {
    this.outstandingRequests.handleError(t);
    try {
      close();
    } catch (Exception e) {
      String exceptionClass = e.getClass().getSimpleName();
      logger.debug("{} while closing connection on error, ignoring: {}", exceptionClass, e.getMessage());
    } 
  }
  
  public String getRemoteHostname() {
    return this.remoteName;
  }
  
  public boolean isConnected() {
    return this.transport.isConnected();
  }
  
  public ConnectionInfo getConnectionInfo() {
    return this.connectionInfo;
  }
  
  @Handler
  private void sessionLogoff(SessionLoggedOff loggedOff) {
    this.sessionTable.sessionClosed(Long.valueOf(loggedOff.getSessionId()));
    logger.debug("Session << {} >> logged off", Long.valueOf(loggedOff.getSessionId()));
  }
  
  private static class DelegatingSMBMessageConverter implements PacketFactory<SMBPacket<?>> {
    private PacketFactory<?>[] packetFactories;
    
    public DelegatingSMBMessageConverter(PacketFactory... packetFactories) {
      this.packetFactories = (PacketFactory<?>[])packetFactories;
    }
    
    public SMBPacket<?> read(byte[] data) throws Buffer.BufferException, IOException {
      byte b;
      int i;
      PacketFactory<?>[] arrayOfPacketFactory;
      for (i = (arrayOfPacketFactory = this.packetFactories).length, b = 0; b < i; ) {
        PacketFactory<?> packetFactory = arrayOfPacketFactory[b];
        if (packetFactory.canHandle(data))
          return (SMBPacket)packetFactory.read(data); 
        b++;
      } 
      throw new IOException("Unknown packet format received.");
    }
    
    public boolean canHandle(byte[] data) {
      byte b;
      int i;
      PacketFactory<?>[] arrayOfPacketFactory;
      for (i = (arrayOfPacketFactory = this.packetFactories).length, b = 0; b < i; ) {
        PacketFactory<?> packetFactory = arrayOfPacketFactory[b];
        if (packetFactory.canHandle(data))
          return true; 
        b++;
      } 
      return false;
    }
  }
  
  private class CancelRequest implements CancellableFuture.CancelCallback {
    private Request request;
    
    public CancelRequest(Request request) {
      this.request = request;
    }
    
    public void cancel() {
      SMB2CancelRequest cancel = new SMB2CancelRequest(Connection.this.connectionInfo.getNegotiatedProtocol().getDialect(), 
          this.request.getMessageId(), 
          this.request.getAsyncId());
      try {
        Connection.this.transport.write(cancel);
      } catch (TransportException e) {
        Connection.logger.error("Failed to send {}", cancel);
      } 
    }
  }
}
