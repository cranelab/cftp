package org.apache.http.impl.bootstrap;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.ExceptionLogger;
import org.apache.http.HttpConnectionFactory;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseFactory;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.protocol.HttpExpectationVerifier;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerMapper;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;

public class ServerBootstrap {
  private int listenerPort;
  
  private InetAddress localAddress;
  
  private SocketConfig socketConfig;
  
  private ConnectionConfig connectionConfig;
  
  private LinkedList<HttpRequestInterceptor> requestFirst;
  
  private LinkedList<HttpRequestInterceptor> requestLast;
  
  private LinkedList<HttpResponseInterceptor> responseFirst;
  
  private LinkedList<HttpResponseInterceptor> responseLast;
  
  private String serverInfo;
  
  private HttpProcessor httpProcessor;
  
  private ConnectionReuseStrategy connStrategy;
  
  private HttpResponseFactory responseFactory;
  
  private HttpRequestHandlerMapper handlerMapper;
  
  private Map<String, HttpRequestHandler> handlerMap;
  
  private HttpExpectationVerifier expectationVerifier;
  
  private ServerSocketFactory serverSocketFactory;
  
  private SSLContext sslContext;
  
  private SSLServerSetupHandler sslSetupHandler;
  
  private HttpConnectionFactory<? extends DefaultBHttpServerConnection> connectionFactory;
  
  private ExceptionLogger exceptionLogger;
  
  public static ServerBootstrap bootstrap() {
    return new ServerBootstrap();
  }
  
  public final ServerBootstrap setListenerPort(int listenerPort) {
    this.listenerPort = listenerPort;
    return this;
  }
  
  public final ServerBootstrap setLocalAddress(InetAddress localAddress) {
    this.localAddress = localAddress;
    return this;
  }
  
  public final ServerBootstrap setSocketConfig(SocketConfig socketConfig) {
    this.socketConfig = socketConfig;
    return this;
  }
  
  public final ServerBootstrap setConnectionConfig(ConnectionConfig connectionConfig) {
    this.connectionConfig = connectionConfig;
    return this;
  }
  
  public final ServerBootstrap setHttpProcessor(HttpProcessor httpProcessor) {
    this.httpProcessor = httpProcessor;
    return this;
  }
  
  public final ServerBootstrap addInterceptorFirst(HttpResponseInterceptor itcp) {
    if (itcp == null)
      return this; 
    if (this.responseFirst == null)
      this.responseFirst = new LinkedList<HttpResponseInterceptor>(); 
    this.responseFirst.addFirst(itcp);
    return this;
  }
  
  public final ServerBootstrap addInterceptorLast(HttpResponseInterceptor itcp) {
    if (itcp == null)
      return this; 
    if (this.responseLast == null)
      this.responseLast = new LinkedList<HttpResponseInterceptor>(); 
    this.responseLast.addLast(itcp);
    return this;
  }
  
  public final ServerBootstrap addInterceptorFirst(HttpRequestInterceptor itcp) {
    if (itcp == null)
      return this; 
    if (this.requestFirst == null)
      this.requestFirst = new LinkedList<HttpRequestInterceptor>(); 
    this.requestFirst.addFirst(itcp);
    return this;
  }
  
  public final ServerBootstrap addInterceptorLast(HttpRequestInterceptor itcp) {
    if (itcp == null)
      return this; 
    if (this.requestLast == null)
      this.requestLast = new LinkedList<HttpRequestInterceptor>(); 
    this.requestLast.addLast(itcp);
    return this;
  }
  
  public final ServerBootstrap setServerInfo(String serverInfo) {
    this.serverInfo = serverInfo;
    return this;
  }
  
  public final ServerBootstrap setConnectionReuseStrategy(ConnectionReuseStrategy connStrategy) {
    this.connStrategy = connStrategy;
    return this;
  }
  
  public final ServerBootstrap setResponseFactory(HttpResponseFactory responseFactory) {
    this.responseFactory = responseFactory;
    return this;
  }
  
  public final ServerBootstrap setHandlerMapper(HttpRequestHandlerMapper handlerMapper) {
    this.handlerMapper = handlerMapper;
    return this;
  }
  
  public final ServerBootstrap registerHandler(String pattern, HttpRequestHandler handler) {
    if (pattern == null || handler == null)
      return this; 
    if (this.handlerMap == null)
      this.handlerMap = new HashMap<String, HttpRequestHandler>(); 
    this.handlerMap.put(pattern, handler);
    return this;
  }
  
  public final ServerBootstrap setExpectationVerifier(HttpExpectationVerifier expectationVerifier) {
    this.expectationVerifier = expectationVerifier;
    return this;
  }
  
  public final ServerBootstrap setConnectionFactory(HttpConnectionFactory<? extends DefaultBHttpServerConnection> connectionFactory) {
    this.connectionFactory = connectionFactory;
    return this;
  }
  
  public final ServerBootstrap setSslSetupHandler(SSLServerSetupHandler sslSetupHandler) {
    this.sslSetupHandler = sslSetupHandler;
    return this;
  }
  
  public final ServerBootstrap setServerSocketFactory(ServerSocketFactory serverSocketFactory) {
    this.serverSocketFactory = serverSocketFactory;
    return this;
  }
  
  public final ServerBootstrap setSslContext(SSLContext sslContext) {
    this.sslContext = sslContext;
    return this;
  }
  
  public final ServerBootstrap setExceptionLogger(ExceptionLogger exceptionLogger) {
    this.exceptionLogger = exceptionLogger;
    return this;
  }
  
  public HttpServer create() {
    HttpProcessor httpProcessorCopy = this.httpProcessor;
    if (httpProcessorCopy == null) {
      HttpProcessorBuilder b = HttpProcessorBuilder.create();
      if (this.requestFirst != null)
        for (HttpRequestInterceptor i : this.requestFirst)
          b.addFirst(i);  
      if (this.responseFirst != null)
        for (HttpResponseInterceptor i : this.responseFirst)
          b.addFirst(i);  
      String serverInfoCopy = this.serverInfo;
      if (serverInfoCopy == null)
        serverInfoCopy = "Apache-HttpCore/1.1"; 
      b.addAll(new HttpResponseInterceptor[] { new ResponseDate(), new ResponseServer(serverInfoCopy), new ResponseContent(), new ResponseConnControl() });
      if (this.requestLast != null)
        for (HttpRequestInterceptor i : this.requestLast)
          b.addLast(i);  
      if (this.responseLast != null)
        for (HttpResponseInterceptor i : this.responseLast)
          b.addLast(i);  
      httpProcessorCopy = b.build();
    } 
    HttpRequestHandlerMapper handlerMapperCopy = this.handlerMapper;
    if (handlerMapperCopy == null) {
      UriHttpRequestHandlerMapper reqistry = new UriHttpRequestHandlerMapper();
      if (this.handlerMap != null)
        for (Map.Entry<String, HttpRequestHandler> entry : this.handlerMap.entrySet())
          reqistry.register(entry.getKey(), entry.getValue());  
      handlerMapperCopy = reqistry;
    } 
    ConnectionReuseStrategy connStrategyCopy = this.connStrategy;
    if (connStrategyCopy == null)
      connStrategyCopy = DefaultConnectionReuseStrategy.INSTANCE; 
    HttpResponseFactory responseFactoryCopy = this.responseFactory;
    if (responseFactoryCopy == null)
      responseFactoryCopy = DefaultHttpResponseFactory.INSTANCE; 
    HttpService httpService = new HttpService(httpProcessorCopy, connStrategyCopy, responseFactoryCopy, handlerMapperCopy, this.expectationVerifier);
    ServerSocketFactory serverSocketFactoryCopy = this.serverSocketFactory;
    if (serverSocketFactoryCopy == null)
      if (this.sslContext != null) {
        serverSocketFactoryCopy = this.sslContext.getServerSocketFactory();
      } else {
        serverSocketFactoryCopy = ServerSocketFactory.getDefault();
      }  
    HttpConnectionFactory<? extends DefaultBHttpServerConnection> connectionFactoryCopy = this.connectionFactory;
    if (connectionFactoryCopy == null)
      if (this.connectionConfig != null) {
        connectionFactoryCopy = new DefaultBHttpServerConnectionFactory(this.connectionConfig);
      } else {
        connectionFactoryCopy = DefaultBHttpServerConnectionFactory.INSTANCE;
      }  
    ExceptionLogger exceptionLoggerCopy = this.exceptionLogger;
    if (exceptionLoggerCopy == null)
      exceptionLoggerCopy = ExceptionLogger.NO_OP; 
    return new HttpServer((this.listenerPort > 0) ? this.listenerPort : 0, this.localAddress, (this.socketConfig != null) ? this.socketConfig : SocketConfig.DEFAULT, serverSocketFactoryCopy, httpService, connectionFactoryCopy, this.sslSetupHandler, exceptionLoggerCopy);
  }
}
