package org.apache.http.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509TrustManager;
import org.apache.http.util.Args;

public class SSLContextBuilder {
  static final String TLS = "TLS";
  
  private String protocol;
  
  private final Set<KeyManager> keymanagers;
  
  private final Set<TrustManager> trustmanagers;
  
  private SecureRandom secureRandom;
  
  public static SSLContextBuilder create() {
    return new SSLContextBuilder();
  }
  
  public SSLContextBuilder() {
    this.keymanagers = new LinkedHashSet<KeyManager>();
    this.trustmanagers = new LinkedHashSet<TrustManager>();
  }
  
  public SSLContextBuilder useProtocol(String protocol) {
    this.protocol = protocol;
    return this;
  }
  
  public SSLContextBuilder setSecureRandom(SecureRandom secureRandom) {
    this.secureRandom = secureRandom;
    return this;
  }
  
  public SSLContextBuilder loadTrustMaterial(KeyStore truststore, TrustStrategy trustStrategy) throws NoSuchAlgorithmException, KeyStoreException {
    TrustManagerFactory tmfactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmfactory.init(truststore);
    TrustManager[] tms = tmfactory.getTrustManagers();
    if (tms != null) {
      if (trustStrategy != null)
        for (int i = 0; i < tms.length; i++) {
          TrustManager tm = tms[i];
          if (tm instanceof X509TrustManager)
            tms[i] = new TrustManagerDelegate((X509TrustManager)tm, trustStrategy); 
        }  
      for (TrustManager tm : tms)
        this.trustmanagers.add(tm); 
    } 
    return this;
  }
  
  public SSLContextBuilder loadTrustMaterial(TrustStrategy trustStrategy) throws NoSuchAlgorithmException, KeyStoreException {
    return loadTrustMaterial((KeyStore)null, trustStrategy);
  }
  
  public SSLContextBuilder loadTrustMaterial(File file, char[] storePassword, TrustStrategy trustStrategy) throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
    Args.notNull(file, "Truststore file");
    KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
    FileInputStream instream = new FileInputStream(file);
    try {
      trustStore.load(instream, storePassword);
    } finally {
      instream.close();
    } 
    return loadTrustMaterial(trustStore, trustStrategy);
  }
  
  public SSLContextBuilder loadTrustMaterial(File file, char[] storePassword) throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
    return loadTrustMaterial(file, storePassword, (TrustStrategy)null);
  }
  
  public SSLContextBuilder loadTrustMaterial(File file) throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
    return loadTrustMaterial(file, (char[])null);
  }
  
  public SSLContextBuilder loadTrustMaterial(URL url, char[] storePassword, TrustStrategy trustStrategy) throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
    Args.notNull(url, "Truststore URL");
    KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
    InputStream instream = url.openStream();
    try {
      trustStore.load(instream, storePassword);
    } finally {
      instream.close();
    } 
    return loadTrustMaterial(trustStore, trustStrategy);
  }
  
  public SSLContextBuilder loadTrustMaterial(URL url, char[] storePassword) throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
    return loadTrustMaterial(url, storePassword, (TrustStrategy)null);
  }
  
  public SSLContextBuilder loadKeyMaterial(KeyStore keystore, char[] keyPassword, PrivateKeyStrategy aliasStrategy) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
    KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmfactory.init(keystore, keyPassword);
    KeyManager[] kms = kmfactory.getKeyManagers();
    if (kms != null) {
      if (aliasStrategy != null)
        for (int i = 0; i < kms.length; i++) {
          KeyManager km = kms[i];
          if (km instanceof X509ExtendedKeyManager)
            kms[i] = new KeyManagerDelegate((X509ExtendedKeyManager)km, aliasStrategy); 
        }  
      for (KeyManager km : kms)
        this.keymanagers.add(km); 
    } 
    return this;
  }
  
  public SSLContextBuilder loadKeyMaterial(KeyStore keystore, char[] keyPassword) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
    return loadKeyMaterial(keystore, keyPassword, (PrivateKeyStrategy)null);
  }
  
  public SSLContextBuilder loadKeyMaterial(File file, char[] storePassword, char[] keyPassword, PrivateKeyStrategy aliasStrategy) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, CertificateException, IOException {
    Args.notNull(file, "Keystore file");
    KeyStore identityStore = KeyStore.getInstance(KeyStore.getDefaultType());
    FileInputStream instream = new FileInputStream(file);
    try {
      identityStore.load(instream, storePassword);
    } finally {
      instream.close();
    } 
    return loadKeyMaterial(identityStore, keyPassword, aliasStrategy);
  }
  
  public SSLContextBuilder loadKeyMaterial(File file, char[] storePassword, char[] keyPassword) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, CertificateException, IOException {
    return loadKeyMaterial(file, storePassword, keyPassword, (PrivateKeyStrategy)null);
  }
  
  public SSLContextBuilder loadKeyMaterial(URL url, char[] storePassword, char[] keyPassword, PrivateKeyStrategy aliasStrategy) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, CertificateException, IOException {
    Args.notNull(url, "Keystore URL");
    KeyStore identityStore = KeyStore.getInstance(KeyStore.getDefaultType());
    InputStream instream = url.openStream();
    try {
      identityStore.load(instream, storePassword);
    } finally {
      instream.close();
    } 
    return loadKeyMaterial(identityStore, keyPassword, aliasStrategy);
  }
  
  public SSLContextBuilder loadKeyMaterial(URL url, char[] storePassword, char[] keyPassword) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, CertificateException, IOException {
    return loadKeyMaterial(url, storePassword, keyPassword, (PrivateKeyStrategy)null);
  }
  
  protected void initSSLContext(SSLContext sslcontext, Collection<KeyManager> keyManagers, Collection<TrustManager> trustManagers, SecureRandom secureRandom) throws KeyManagementException {
    sslcontext.init(!keyManagers.isEmpty() ? keyManagers.<KeyManager>toArray(new KeyManager[keyManagers.size()]) : null, !trustManagers.isEmpty() ? trustManagers.<TrustManager>toArray(new TrustManager[trustManagers.size()]) : null, secureRandom);
  }
  
  public SSLContext build() throws NoSuchAlgorithmException, KeyManagementException {
    SSLContext sslcontext = SSLContext.getInstance((this.protocol != null) ? this.protocol : "TLS");
    initSSLContext(sslcontext, this.keymanagers, this.trustmanagers, this.secureRandom);
    return sslcontext;
  }
  
  static class TrustManagerDelegate implements X509TrustManager {
    private final X509TrustManager trustManager;
    
    private final TrustStrategy trustStrategy;
    
    TrustManagerDelegate(X509TrustManager trustManager, TrustStrategy trustStrategy) {
      this.trustManager = trustManager;
      this.trustStrategy = trustStrategy;
    }
    
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
      this.trustManager.checkClientTrusted(chain, authType);
    }
    
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
      if (!this.trustStrategy.isTrusted(chain, authType))
        this.trustManager.checkServerTrusted(chain, authType); 
    }
    
    public X509Certificate[] getAcceptedIssuers() {
      return this.trustManager.getAcceptedIssuers();
    }
  }
  
  static class KeyManagerDelegate extends X509ExtendedKeyManager {
    private final X509ExtendedKeyManager keyManager;
    
    private final PrivateKeyStrategy aliasStrategy;
    
    KeyManagerDelegate(X509ExtendedKeyManager keyManager, PrivateKeyStrategy aliasStrategy) {
      this.keyManager = keyManager;
      this.aliasStrategy = aliasStrategy;
    }
    
    public String[] getClientAliases(String keyType, Principal[] issuers) {
      return this.keyManager.getClientAliases(keyType, issuers);
    }
    
    public Map<String, PrivateKeyDetails> getClientAliasMap(String[] keyTypes, Principal[] issuers) {
      Map<String, PrivateKeyDetails> validAliases = new HashMap<String, PrivateKeyDetails>();
      for (String keyType : keyTypes) {
        String[] aliases = this.keyManager.getClientAliases(keyType, issuers);
        if (aliases != null)
          for (String alias : aliases)
            validAliases.put(alias, new PrivateKeyDetails(keyType, this.keyManager.getCertificateChain(alias)));  
      } 
      return validAliases;
    }
    
    public Map<String, PrivateKeyDetails> getServerAliasMap(String keyType, Principal[] issuers) {
      Map<String, PrivateKeyDetails> validAliases = new HashMap<String, PrivateKeyDetails>();
      String[] aliases = this.keyManager.getServerAliases(keyType, issuers);
      if (aliases != null)
        for (String alias : aliases)
          validAliases.put(alias, new PrivateKeyDetails(keyType, this.keyManager.getCertificateChain(alias)));  
      return validAliases;
    }
    
    public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket socket) {
      Map<String, PrivateKeyDetails> validAliases = getClientAliasMap(keyTypes, issuers);
      return this.aliasStrategy.chooseAlias(validAliases, socket);
    }
    
    public String[] getServerAliases(String keyType, Principal[] issuers) {
      return this.keyManager.getServerAliases(keyType, issuers);
    }
    
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
      Map<String, PrivateKeyDetails> validAliases = getServerAliasMap(keyType, issuers);
      return this.aliasStrategy.chooseAlias(validAliases, socket);
    }
    
    public X509Certificate[] getCertificateChain(String alias) {
      return this.keyManager.getCertificateChain(alias);
    }
    
    public PrivateKey getPrivateKey(String alias) {
      return this.keyManager.getPrivateKey(alias);
    }
    
    public String chooseEngineClientAlias(String[] keyTypes, Principal[] issuers, SSLEngine sslEngine) {
      Map<String, PrivateKeyDetails> validAliases = getClientAliasMap(keyTypes, issuers);
      return this.aliasStrategy.chooseAlias(validAliases, null);
    }
    
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine sslEngine) {
      Map<String, PrivateKeyDetails> validAliases = getServerAliasMap(keyType, issuers);
      return this.aliasStrategy.chooseAlias(validAliases, null);
    }
  }
}
