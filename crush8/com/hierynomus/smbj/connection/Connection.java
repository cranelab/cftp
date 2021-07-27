package com.hierynomus.smbj.connection;

import com.hierynomus.asn1.types.primitive.ASN1ObjectIdentifier;
import com.hierynomus.mserref.NtStatus;
import com.hierynomus.mssmb.SMB1NotSupportedException;
import com.hierynomus.mssmb.SMB1Packet;
import com.hierynomus.mssmb.SMB1PacketFactory;
import com.hierynomus.mssmb.messages.SMB1ComNegotiateRequest;
import com.hierynomus.mssmb2.SMB2Dialect;
import com.hierynomus.mssmb2.SMB2GlobalCapability;
import com.hierynomus.mssmb2.SMB2MessageCommandCode;
import com.hierynomus.mssmb2.SMB2MessageConverter;
import com.hierynomus.mssmb2.SMB2MessageFlag;
import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.mssmb2.SMB2PacketData;
import com.hierynomus.mssmb2.SMB2PacketFactory;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.mssmb2.messages.SMB2CancelRequest;
import com.hierynomus.mssmb2.messages.SMB2NegotiateRequest;
import com.hierynomus.mssmb2.messages.SMB2NegotiateResponse;
import com.hierynomus.mssmb2.messages.SMB2SessionSetup;
import com.hierynomus.protocol.PacketData;
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
import com.hierynomus.smb.SMBPacketData;
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
import java.io.Closeable;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Connection implements Closeable, PacketReceiver<SMBPacketData<?>> {
  private static final Logger logger = LoggerFactory.getLogger(Connection.class);
  
  private static final DelegatingSMBMessageConverter converter = new DelegatingSMBMessageConverter((PacketFactory<?>[])new PacketFactory[] { new SMB2PacketFactory(), new SMB1PacketFactory() });
  
  private ConnectionInfo connectionInfo;
  
  private SessionTable sessionTable = new SessionTable();
  
  private SessionTable preauthSessionTable = new SessionTable();
  
  private OutstandingRequests outstandingRequests = new OutstandingRequests();
  
  private SequenceWindow sequenceWindow;
  
  private SMB2MessageConverter smb2Converter = new SMB2MessageConverter();
  
  private String remoteName;
  
  private SMBClient client;
  
  private SmbConfig config;
  
  private TransportLayer<SMBPacket<?, ?>> transport;
  
  private final SMBEventBus bus;
  
  public SMBClient getClient() {
    return this.client;
  }
  
  private final ReentrantLock lock = new ReentrantLock();
  
  private int remotePort;
  
  public Connection(SmbConfig config, SMBClient client, SMBEventBus bus) {
    this.config = config;
    this.client = client;
    this.transport = config.getTransportLayerFactory().createTransportLayer(new PacketHandlers<>(new SMBPacketSerializer(), this, converter), config);
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
  
  public void close() throws IOException {
    close(false);
  }
  
  public void close(boolean force) throws IOException {
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
    // Byte code:
    //   0: aload_0
    //   1: aload_1
    //   2: invokespecial getAuthenticator : (Lcom/hierynomus/smbj/auth/AuthenticationContext;)Lcom/hierynomus/smbj/auth/Authenticator;
    //   5: astore_2
    //   6: aload_2
    //   7: aload_0
    //   8: getfield config : Lcom/hierynomus/smbj/SmbConfig;
    //   11: invokeinterface init : (Lcom/hierynomus/smbj/SmbConfig;)V
    //   16: aload_0
    //   17: aload_1
    //   18: invokespecial getSession : (Lcom/hierynomus/smbj/auth/AuthenticationContext;)Lcom/hierynomus/smbj/session/Session;
    //   21: astore_3
    //   22: aload_0
    //   23: aload_2
    //   24: aload_1
    //   25: aload_0
    //   26: getfield connectionInfo : Lcom/hierynomus/smbj/connection/ConnectionInfo;
    //   29: invokevirtual getGssNegotiateToken : ()[B
    //   32: aload_3
    //   33: invokespecial processAuthenticationToken : (Lcom/hierynomus/smbj/auth/Authenticator;Lcom/hierynomus/smbj/auth/AuthenticationContext;[BLcom/hierynomus/smbj/session/Session;)[B
    //   36: astore #4
    //   38: aload_0
    //   39: aload #4
    //   41: lconst_0
    //   42: invokespecial initiateSessionSetup : ([BJ)Lcom/hierynomus/mssmb2/messages/SMB2SessionSetup;
    //   45: astore #5
    //   47: aload #5
    //   49: invokevirtual getHeader : ()Lcom/hierynomus/smb/SMBHeader;
    //   52: checkcast com/hierynomus/mssmb2/SMB2Header
    //   55: invokevirtual getSessionId : ()J
    //   58: lstore #6
    //   60: lload #6
    //   62: lconst_0
    //   63: lcmp
    //   64: ifeq -> 80
    //   67: aload_0
    //   68: getfield preauthSessionTable : Lcom/hierynomus/smbj/connection/SessionTable;
    //   71: lload #6
    //   73: invokestatic valueOf : (J)Ljava/lang/Long;
    //   76: aload_3
    //   77: invokevirtual registerSession : (Ljava/lang/Long;Lcom/hierynomus/smbj/session/Session;)V
    //   80: aload #5
    //   82: invokevirtual getHeader : ()Lcom/hierynomus/smb/SMBHeader;
    //   85: checkcast com/hierynomus/mssmb2/SMB2Header
    //   88: invokevirtual getStatusCode : ()J
    //   91: getstatic com/hierynomus/mserref/NtStatus.STATUS_MORE_PROCESSING_REQUIRED : Lcom/hierynomus/mserref/NtStatus;
    //   94: invokevirtual getValue : ()J
    //   97: lcmp
    //   98: ifne -> 143
    //   101: getstatic com/hierynomus/smbj/connection/Connection.logger : Lorg/slf4j/Logger;
    //   104: ldc 'More processing required for authentication of {} using {}'
    //   106: aload_1
    //   107: invokevirtual getUsername : ()Ljava/lang/String;
    //   110: aload_2
    //   111: invokeinterface debug : (Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V
    //   116: aload_0
    //   117: aload_2
    //   118: aload_1
    //   119: aload #5
    //   121: invokevirtual getSecurityBuffer : ()[B
    //   124: aload_3
    //   125: invokespecial processAuthenticationToken : (Lcom/hierynomus/smbj/auth/Authenticator;Lcom/hierynomus/smbj/auth/AuthenticationContext;[BLcom/hierynomus/smbj/session/Session;)[B
    //   128: astore #4
    //   130: aload_0
    //   131: aload #4
    //   133: lload #6
    //   135: invokespecial initiateSessionSetup : ([BJ)Lcom/hierynomus/mssmb2/messages/SMB2SessionSetup;
    //   138: astore #5
    //   140: goto -> 80
    //   143: aload #5
    //   145: invokevirtual getHeader : ()Lcom/hierynomus/smb/SMBHeader;
    //   148: checkcast com/hierynomus/mssmb2/SMB2Header
    //   151: invokevirtual getStatusCode : ()J
    //   154: getstatic com/hierynomus/mserref/NtStatus.STATUS_SUCCESS : Lcom/hierynomus/mserref/NtStatus;
    //   157: invokevirtual getValue : ()J
    //   160: lcmp
    //   161: ifeq -> 200
    //   164: new com/hierynomus/mssmb2/SMBApiException
    //   167: dup
    //   168: aload #5
    //   170: invokevirtual getHeader : ()Lcom/hierynomus/smb/SMBHeader;
    //   173: checkcast com/hierynomus/mssmb2/SMB2Header
    //   176: ldc 'Authentication failed for '%s' using %s'
    //   178: iconst_2
    //   179: anewarray java/lang/Object
    //   182: dup
    //   183: iconst_0
    //   184: aload_1
    //   185: invokevirtual getUsername : ()Ljava/lang/String;
    //   188: aastore
    //   189: dup
    //   190: iconst_1
    //   191: aload_2
    //   192: aastore
    //   193: invokestatic format : (Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
    //   196: invokespecial <init> : (Lcom/hierynomus/mssmb2/SMB2Header;Ljava/lang/String;)V
    //   199: athrow
    //   200: aload_3
    //   201: aload #5
    //   203: invokevirtual getHeader : ()Lcom/hierynomus/smb/SMBHeader;
    //   206: checkcast com/hierynomus/mssmb2/SMB2Header
    //   209: invokevirtual getSessionId : ()J
    //   212: invokevirtual setSessionId : (J)V
    //   215: aload #5
    //   217: invokevirtual getSecurityBuffer : ()[B
    //   220: ifnull -> 236
    //   223: aload_0
    //   224: aload_2
    //   225: aload_1
    //   226: aload #5
    //   228: invokevirtual getSecurityBuffer : ()[B
    //   231: aload_3
    //   232: invokespecial processAuthenticationToken : (Lcom/hierynomus/smbj/auth/Authenticator;Lcom/hierynomus/smbj/auth/AuthenticationContext;[BLcom/hierynomus/smbj/session/Session;)[B
    //   235: pop
    //   236: aload_3
    //   237: aload #5
    //   239: invokevirtual init : (Lcom/hierynomus/mssmb2/messages/SMB2SessionSetup;)V
    //   242: getstatic com/hierynomus/smbj/connection/Connection.logger : Lorg/slf4j/Logger;
    //   245: ldc 'Successfully authenticated {} on {}, session is {}'
    //   247: iconst_3
    //   248: anewarray java/lang/Object
    //   251: dup
    //   252: iconst_0
    //   253: aload_1
    //   254: invokevirtual getUsername : ()Ljava/lang/String;
    //   257: aastore
    //   258: dup
    //   259: iconst_1
    //   260: aload_0
    //   261: getfield remoteName : Ljava/lang/String;
    //   264: aastore
    //   265: dup
    //   266: iconst_2
    //   267: aload_3
    //   268: invokevirtual getSessionId : ()J
    //   271: invokestatic valueOf : (J)Ljava/lang/Long;
    //   274: aastore
    //   275: invokeinterface info : (Ljava/lang/String;[Ljava/lang/Object;)V
    //   280: aload_0
    //   281: getfield sessionTable : Lcom/hierynomus/smbj/connection/SessionTable;
    //   284: aload_3
    //   285: invokevirtual getSessionId : ()J
    //   288: invokestatic valueOf : (J)Ljava/lang/Long;
    //   291: aload_3
    //   292: invokevirtual registerSession : (Ljava/lang/Long;Lcom/hierynomus/smbj/session/Session;)V
    //   295: aload_3
    //   296: astore #8
    //   298: lload #6
    //   300: lconst_0
    //   301: lcmp
    //   302: ifeq -> 318
    //   305: aload_0
    //   306: getfield preauthSessionTable : Lcom/hierynomus/smbj/connection/SessionTable;
    //   309: lload #6
    //   311: invokestatic valueOf : (J)Ljava/lang/Long;
    //   314: invokevirtual sessionClosed : (Ljava/lang/Long;)Lcom/hierynomus/smbj/session/Session;
    //   317: pop
    //   318: aload #8
    //   320: areturn
    //   321: astore #9
    //   323: lload #6
    //   325: lconst_0
    //   326: lcmp
    //   327: ifeq -> 343
    //   330: aload_0
    //   331: getfield preauthSessionTable : Lcom/hierynomus/smbj/connection/SessionTable;
    //   334: lload #6
    //   336: invokestatic valueOf : (J)Ljava/lang/Long;
    //   339: invokevirtual sessionClosed : (Ljava/lang/Long;)Lcom/hierynomus/smbj/session/Session;
    //   342: pop
    //   343: aload #9
    //   345: athrow
    //   346: astore_2
    //   347: new com/hierynomus/smbj/common/SMBRuntimeException
    //   350: dup
    //   351: aload_2
    //   352: invokespecial <init> : (Ljava/lang/Throwable;)V
    //   355: athrow
    // Line number table:
    //   Java source line number -> byte code offset
    //   #165	-> 0
    //   #166	-> 6
    //   #167	-> 16
    //   #168	-> 22
    //   #169	-> 38
    //   #170	-> 47
    //   #171	-> 60
    //   #172	-> 67
    //   #175	-> 80
    //   #176	-> 101
    //   #177	-> 116
    //   #178	-> 130
    //   #181	-> 143
    //   #182	-> 164
    //   #187	-> 200
    //   #189	-> 215
    //   #191	-> 223
    //   #193	-> 236
    //   #194	-> 242
    //   #195	-> 280
    //   #196	-> 295
    //   #198	-> 298
    //   #199	-> 305
    //   #196	-> 318
    //   #198	-> 321
    //   #199	-> 330
    //   #202	-> 346
    //   #203	-> 347
    // Local variable table:
    //   start	length	slot	name	descriptor
    //   6	340	2	authenticator	Lcom/hierynomus/smbj/auth/Authenticator;
    //   22	324	3	session	Lcom/hierynomus/smbj/session/Session;
    //   38	308	4	securityContext	[B
    //   47	299	5	receive	Lcom/hierynomus/mssmb2/messages/SMB2SessionSetup;
    //   60	286	6	preauthSessionId	J
    //   347	9	2	e	Ljava/lang/Exception;
    //   0	356	0	this	Lcom/hierynomus/smbj/connection/Connection;
    //   0	356	1	authContext	Lcom/hierynomus/smbj/auth/AuthenticationContext;
    // Exception table:
    //   from	to	target	type
    //   0	318	346	com/hierynomus/spnego/SpnegoException
    //   0	318	346	java/io/IOException
    //   80	298	321	finally
    //   321	323	321	finally
    //   321	346	346	com/hierynomus/spnego/SpnegoException
    //   321	346	346	java/io/IOException
  }
  
  private Session getSession(AuthenticationContext authContext) {
    return new Session(this, authContext, this.bus, this.client.getPathResolver(), this.config.getSecurityProvider());
  }
  
  private byte[] processAuthenticationToken(Authenticator authenticator, AuthenticationContext authContext, byte[] inputToken, Session session) throws IOException {
    AuthenticateResponse resp = authenticator.authenticate(authContext, inputToken, session);
    if (resp == null)
      return null; 
    this.connectionInfo.setWindowsVersion(resp.getWindowsVersion());
    this.connectionInfo.setNetBiosName(resp.getNetBiosName());
    byte[] securityContext = resp.getNegToken();
    if (resp.getSigningKey() != null)
      session.setSigningKey(resp.getSigningKey()); 
    return securityContext;
  }
  
  private SMB2SessionSetup initiateSessionSetup(byte[] securityContext, long sessionId) throws TransportException {
    SMB2SessionSetup req = new SMB2SessionSetup(this.connectionInfo.getNegotiatedProtocol().getDialect(), EnumSet.of(SMB2SessionSetup.SMB2SecurityMode.SMB2_NEGOTIATE_SIGNING_ENABLED), this.connectionInfo.getClientCapabilities());
    req.setSecurityBuffer(securityContext);
    req.getHeader().setSessionId(sessionId);
    return sendAndReceive(req);
  }
  
  private Authenticator getAuthenticator(AuthenticationContext context) throws SpnegoException {
    List<Factory.Named<Authenticator>> supportedAuthenticators = new ArrayList<>(this.config.getSupportedAuthenticators());
    List<ASN1ObjectIdentifier> mechTypes = new ArrayList<>();
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
      Request request = new Request(packet.getPacket(), messageIds[0], UUID.randomUUID());
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
    if (!NtStatus.isSuccess(negotiateResponse.getHeader().getStatusCode()))
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
    Request request = new Request(negotiatePacket, l, UUID.randomUUID());
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
  
  public void handle(SMBPacketData uncheckedPacket) throws TransportException {
    if (!(uncheckedPacket instanceof SMB2PacketData))
      throw new SMB1NotSupportedException(); 
    SMB2PacketData packetData = (SMB2PacketData)uncheckedPacket;
    long messageId = packetData.getSequenceNumber();
    if (!this.outstandingRequests.isOutstanding(Long.valueOf(messageId)))
      throw new TransportException("Received response with unknown sequence number <<" + messageId + ">>"); 
    this.sequenceWindow.creditsGranted(packetData.getHeader().getCreditResponse());
    logger.debug("Server granted us {} credits for {}, now available: {} credits", new Object[] { Integer.valueOf(packetData.getHeader().getCreditResponse()), packetData, Integer.valueOf(this.sequenceWindow.available()) });
    Request request = this.outstandingRequests.getRequestByMessageId(Long.valueOf(messageId));
    logger.trace("Send/Recv of packet {} took << {} ms >>", packetData, Long.valueOf(System.currentTimeMillis() - request.getTimestamp().getTime()));
    if (packetData.isIntermediateAsyncResponse()) {
      logger.debug("Received ASYNC packet {} with AsyncId << {} >>", packetData, Long.valueOf(packetData.getHeader().getAsyncId()));
      request.setAsyncId(packetData.getHeader().getAsyncId());
      return;
    } 
    SMB2Packet packet = null;
    try {
      packet = this.smb2Converter.readPacket(request.getPacket(), packetData);
    } catch (com.hierynomus.protocol.commons.buffer.Buffer.BufferException e) {
      throw new TransportException("Unable to deserialize SMB2 Packet Data.", e);
    } 
    long sessionId = packetData.getHeader().getSessionId();
    if (sessionId != 0L && packetData.getHeader().getMessage() != SMB2MessageCommandCode.SMB2_SESSION_SETUP) {
      Session session = this.sessionTable.find(Long.valueOf(sessionId));
      if (session == null) {
        session = this.preauthSessionTable.find(Long.valueOf(sessionId));
        if (session == null) {
          logger.warn("Illegal request, no session matching the sessionId: {}", Long.valueOf(sessionId));
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
  
  private static class DelegatingSMBMessageConverter implements PacketFactory<SMBPacketData<?>> {
    private PacketFactory<?>[] packetFactories;
    
    public DelegatingSMBMessageConverter(PacketFactory<?>... packetFactories) {
      this.packetFactories = packetFactories;
    }
    
    public SMBPacketData<?> read(byte[] data) throws Buffer.BufferException, IOException {
      for (PacketFactory<?> packetFactory : this.packetFactories) {
        if (packetFactory.canHandle(data))
          return (SMBPacketData)packetFactory.read(data); 
      } 
      throw new IOException("Unknown packet format received.");
    }
    
    public boolean canHandle(byte[] data) {
      for (PacketFactory<?> packetFactory : this.packetFactories) {
        if (packetFactory.canHandle(data))
          return true; 
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
      SMB2CancelRequest cancel = new SMB2CancelRequest(Connection.this.connectionInfo.getNegotiatedProtocol().getDialect(), this.request.getMessageId(), this.request.getAsyncId());
      try {
        Connection.this.transport.write(cancel);
      } catch (TransportException e) {
        Connection.logger.error("Failed to send {}", cancel);
      } 
    }
  }
}
