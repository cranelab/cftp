package crushftp.handlers;

import com.crushftp.client.Base64;
import com.crushftp.client.Common;
import com.crushftp.client.File_S;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

public class SSLKeyManager {
  static Class class$0;
  
  static Class class$1;
  
  public static void main(String[] args) {
    try {
      Security.addProvider((Provider)new BouncyCastleProvider());
    } catch (Exception ex) {
      ex.printStackTrace();
    } 
  }
  
  public static String importReply(String keystore_path, String keystore_pass, String key_pass, String import_path, String trusted_paths) throws Exception {
    String result = "";
    Vector trusted_certs_used = new Vector();
    Vector trusted_certificates = new Vector();
    X509Certificate import_cert = null;
    String private_alias = null;
    String keystore_format = (keystore_path.toUpperCase().trim().indexOf("PFX") >= 0 || keystore_path.toUpperCase().trim().indexOf("PKCS12") >= 0) ? "PKCS12" : "JKS";
    KeyStore jks = KeyStore.getInstance(keystore_format);
    jks.load(new FileInputStream(new File_S(keystore_path)), keystore_pass.toCharArray());
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    FileInputStream cert_in = new FileInputStream(new File_S(import_path));
    Collection c = cf.generateCertificates(cert_in);
    cert_in.close();
    import_cert = (X509Certificate)c.toArray()[0];
    for (int x = 1; x < c.size(); x++)
      trusted_certificates.addElement(c.toArray()[x]); 
    trusted_certs_used.addElement(import_cert);
    String[] trusted_items = trusted_paths.split(";");
    for (int i = 0; i < trusted_items.length; i++) {
      if (!trusted_items[i].trim().equals(""))
        try {
          cert_in = new FileInputStream(new File_S(trusted_items[i].trim()));
          c = cf.generateCertificates(cert_in);
          cert_in.close();
          for (int xx = 0; xx < c.size(); xx++)
            trusted_certificates.addElement(c.toArray()[xx]); 
        } catch (IOException e) {
          result = String.valueOf(result) + "ERROR:" + trusted_items[i].trim() + ":" + e + "\r\n";
        }  
    } 
    Properties trusted_user_certs = new Properties();
    Enumeration keys = jks.aliases();
    while (keys.hasMoreElements()) {
      String alias = keys.nextElement().toString();
      if (class$0 == null)
        try {
        
        } catch (ClassNotFoundException classNotFoundException) {
          throw new NoClassDefFoundError(null.getMessage());
        }  
      if (alias.entryInstanceOf((String)class$0, class$0 = Class.forName("java.security.KeyStore$PrivateKeyEntry"))) {
        private_alias = alias;
        continue;
      } 
      if (class$1 == null)
        try {
        
        } catch (ClassNotFoundException classNotFoundException) {
          throw new NoClassDefFoundError(null.getMessage());
        }  
      if (alias.entryInstanceOf((String)class$1, class$1 = Class.forName("java.security.KeyStore$TrustedCertificateEntry"))) {
        trusted_user_certs.put(alias, jks.getCertificate(alias));
        trusted_certificates.addElement(jks.getCertificate(alias));
      } 
    } 
    KeyStore cacerts = KeyStore.getInstance("JKS");
    cacerts.load(new FileInputStream(String.valueOf(System.getProperty("java.home")) + "/lib/security/cacerts"), "changeit".toCharArray());
    keys = cacerts.aliases();
    while (keys.hasMoreElements()) {
      String alias = keys.nextElement().toString();
      if (class$1 == null)
        try {
        
        } catch (ClassNotFoundException classNotFoundException) {
          throw new NoClassDefFoundError(null.getMessage());
        }  
      if (alias.entryInstanceOf((String)class$1, class$1 = Class.forName("java.security.KeyStore$TrustedCertificateEntry")))
        trusted_certificates.addElement(cacerts.getCertificate(alias)); 
    } 
    X509Certificate private_cert = (X509Certificate)jks.getCertificate(private_alias);
    if ((new String(Base64.encodeBytes(private_cert.getPublicKey().getEncoded()))).equals(new String(Base64.encodeBytes(import_cert.getPublicKey().getEncoded())))) {
      int j = 0;
      while (j < trusted_certs_used.size()) {
        X509Certificate cert = trusted_certs_used.elementAt(j);
        j++;
        for (int m = 0; m < trusted_certificates.size(); m++) {
          X509Certificate cert2 = (X509Certificate)trusted_certificates.elementAt(m);
          if (findCN(cert2.getSubjectX500Principal().getName()).equalsIgnoreCase(findCN(cert.getIssuerX500Principal().getName()))) {
            if (!findCN(cert2.getSubjectX500Principal().getName()).equalsIgnoreCase(findCN(cert.getSubjectX500Principal().getName())))
              trusted_certs_used.addElement(cert2); 
            break;
          } 
        } 
      } 
      X509Certificate[] certs2 = new X509Certificate[trusted_certs_used.size()];
      for (int k = 0; k < trusted_certs_used.size(); k++)
        certs2[k] = trusted_certs_used.elementAt(k); 
      jks.setKeyEntry(private_alias, jks.getKey(private_alias, key_pass.toCharArray()), key_pass.toCharArray(), (Certificate[])certs2);
      Enumeration keys2 = trusted_user_certs.keys();
      while (keys2.hasMoreElements()) {
        String key = keys2.nextElement().toString();
        jks.setCertificateEntry(key, (Certificate)trusted_user_certs.get(key));
      } 
      jks.store(new FileOutputStream(new File_S(keystore_path)), key_pass.toCharArray());
      return "SUCCESS:" + result;
    } 
    return "ERROR:The private key (" + findCN(private_cert.getSubjectX500Principal().getName()) + ", SERIALNUMBER=" + private_cert.getSerialNumber().toString(16) + ") does not match import reply key (" + findCN(import_cert.getSubjectX500Principal().getName()) + ", SERIALNUMBER=" + import_cert.getSerialNumber().toString(16) + ").";
  }
  
  public static String findCN(String name) {
    name = name.toUpperCase();
    if (name.indexOf("CN=") < 0)
      return name; 
    name = name.substring(name.indexOf("CN="));
    if (name.indexOf(",") >= 0)
      name = name.substring(0, name.indexOf(",")); 
    return name.trim();
  }
  
  public static String buildNew(String key_alg, int key_size, String sig_alg, int days, String cn, String ou, String o, String l, String st, String c, String e, String keystore_path, String keystore_pass, String key_pass) throws Exception {
    if (c.equalsIgnoreCase("UK"))
      c = "GB"; 
    if ((new File_S(keystore_path)).exists())
      throw new IOException("Cannot overwrite an existing keystore file."); 
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance(key_alg);
    keyGen.initialize(key_size, SecureRandom.getInstance("SHA1PRNG"));
    KeyPair pair = keyGen.generateKeyPair();
    PrivateKey priv = pair.getPrivate();
    PublicKey pub = pair.getPublic();
    X509Certificate new_cert = generateCert(cn, ou, o, l, st, c, e, days, pub, priv, sig_alg);
    KeyStore jks = KeyStore.getInstance("JKS");
    jks.load(null, keystore_pass.toCharArray());
    Certificate[] certs = { new_cert };
    jks.setKeyEntry(cn, priv, key_pass.toCharArray(), certs);
    jks.store(new FileOutputStream(new File_S(keystore_path)), keystore_pass.toCharArray());
    return "SUCCESS:Created";
  }
  
  public static String makeCSR(String keystore_path, String keystore_pass, String key_pass) throws Exception {
    String private_alias = null;
    String keystore_format = (keystore_path.toUpperCase().trim().indexOf("PFX") >= 0 || keystore_path.toUpperCase().trim().indexOf("PKCS12") >= 0) ? "PKCS12" : "JKS";
    KeyStore jks = KeyStore.getInstance(keystore_format);
    jks.load(new FileInputStream(new File_S(keystore_path)), keystore_pass.toCharArray());
    Enumeration keys = jks.aliases();
    while (keys.hasMoreElements()) {
      String alias = keys.nextElement().toString();
      if (class$0 == null)
        try {
        
        } catch (ClassNotFoundException classNotFoundException) {
          throw new NoClassDefFoundError(null.getMessage());
        }  
      if (alias.entryInstanceOf((String)class$0, class$0 = Class.forName("java.security.KeyStore$PrivateKeyEntry")))
        private_alias = alias; 
    } 
    X509Certificate private_cert = (X509Certificate)((KeyStore.PrivateKeyEntry)jks.getEntry(private_alias, new KeyStore.PasswordProtection(key_pass.toCharArray()))).getCertificate();
    AlgorithmIdentifier signatureAlgorithm = (new DefaultSignatureAlgorithmIdentifierFinder()).find(private_cert.getSigAlgName());
    AlgorithmIdentifier digestAlgorithm = (new DefaultDigestAlgorithmIdentifierFinder()).find("SHA-256");
    ContentSigner signer = (new BcRSAContentSignerBuilder(signatureAlgorithm, digestAlgorithm)).build(PrivateKeyFactory.createKey(jks.getKey(private_alias, key_pass.toCharArray()).getEncoded()));
    JcaPKCS10CertificationRequestBuilder jcaPKCS10CertificationRequestBuilder = new JcaPKCS10CertificationRequestBuilder(new X500Name(private_cert.getSubjectDN().getName()), private_cert.getPublicKey());
    ExtensionsGenerator extensionsGenerator = new ExtensionsGenerator();
    extensionsGenerator.addExtension(X509Extension.basicConstraints, true, (ASN1Encodable)new BasicConstraints(true));
    extensionsGenerator.addExtension(X509Extension.keyUsage, true, (ASN1Encodable)new KeyUsage(6));
    jcaPKCS10CertificationRequestBuilder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, (ASN1Encodable)extensionsGenerator.generate());
    PKCS10CertificationRequest csr = jcaPKCS10CertificationRequestBuilder.build(signer);
    String csr64 = "-----BEGIN CERTIFICATE REQUEST-----\r\n";
    String csr_encoded = new String(Base64.encodeBytes(csr.getEncoded()));
    int loc = 0;
    while (loc < csr_encoded.length()) {
      if (csr_encoded.length() - loc > 64) {
        csr64 = String.valueOf(csr64) + csr_encoded.substring(loc, loc + 64) + "\r\n";
      } else {
        csr64 = String.valueOf(csr64) + csr_encoded.substring(loc) + "\r\n";
      } 
      loc += 64;
    } 
    csr64 = String.valueOf(csr64) + "-----END CERTIFICATE REQUEST-----\r\n";
    return csr64;
  }
  
  public static X509Certificate generateCert(String sCommonName, String sOrganisationUnit, String sOrganisation, String sLocality, String sState, String sCountryCode, String sEmailAddress, int iValidity, PublicKey publicKey, PrivateKey privateKey, String sig_alg) throws Exception {
    String cn = "";
    cn = String.valueOf(cn) + "  OU=\"" + sOrganisationUnit + "\"";
    cn = String.valueOf(cn) + ", O=\"" + sOrganisation + "\"";
    cn = String.valueOf(cn) + ", L=\"" + sLocality + "\"";
    cn = String.valueOf(cn) + ", ST=" + sState;
    cn = String.valueOf(cn) + ", C=" + sCountryCode;
    cn = String.valueOf(cn) + ", EMAILADDRESS=" + sEmailAddress;
    cn = String.valueOf(cn) + ", CN=" + sCommonName;
    X509v1CertificateBuilder certbuild = new X509v1CertificateBuilder(new X500Name(cn), new BigInteger(Long.toString(System.currentTimeMillis() / 1000L)), new Date(), new Date(System.currentTimeMillis() + iValidity * 24L * 60L * 60L * 1000L), new X500Name(cn), new SubjectPublicKeyInfo(ASN1Sequence.getInstance(publicKey.getEncoded())));
    return (new JcaX509CertificateConverter()).setProvider("BC").getCertificate(certbuild.build((new JcaContentSignerBuilder(sig_alg)).setProvider("BC").build(privateKey)));
  }
  
  public static Vector list(String keystore_path, String keystore_pass) throws Exception {
    Vector v = new Vector();
    String keystore_format = (keystore_path.toUpperCase().trim().indexOf("PFX") >= 0 || keystore_path.toUpperCase().trim().indexOf("PKCS12") >= 0) ? "PKCS12" : "JKS";
    KeyStore jks = KeyStore.getInstance(keystore_format);
    if (!(new File_S(keystore_path)).exists()) {
      jks.load(null, keystore_pass.toCharArray());
      FileOutputStream out = new FileOutputStream(new File_S(keystore_path));
      jks.store(out, keystore_pass.toCharArray());
      out.close();
    } else {
      jks.load(new FileInputStream(new File_S(keystore_path)), keystore_pass.toCharArray());
    } 
    Enumeration keys = jks.aliases();
    while (keys.hasMoreElements()) {
      String alias = keys.nextElement().toString();
      Certificate entry = jks.getCertificate(alias);
      Properties p = new Properties();
      p.put("alias", alias);
      p.put("private", "false");
      p.put("public", "false");
      if (class$0 == null)
        try {
        
        } catch (ClassNotFoundException classNotFoundException) {
          throw new NoClassDefFoundError(null.getMessage());
        }  
      if (alias.entryInstanceOf((String)class$0, class$0 = Class.forName("java.security.KeyStore$PrivateKeyEntry")))
        p.put("private", "true"); 
      if (class$0 == null)
        try {
        
        } catch (ClassNotFoundException classNotFoundException) {
          throw new NoClassDefFoundError(null.getMessage());
        }  
      if (alias.entryInstanceOf((String)class$0, class$0 = Class.forName("java.security.KeyStore$PrivateKeyEntry")))
        p.put("public", "true"); 
      p.put("type", entry.getType());
      p.put("format", entry.getPublicKey().getFormat());
      p.put("algorithm", entry.getPublicKey().getAlgorithm());
      if (entry instanceof X509Certificate) {
        X509Certificate x509 = (X509Certificate)entry;
        p.put("sigAlg", x509.getSigAlgName());
        p.put("issuerDN", x509.getIssuerDN().toString());
        p.put("expires", (new StringBuffer(String.valueOf(x509.getNotAfter().getTime()))).toString());
        p.put("subjectDN", x509.getSubjectDN().toString());
        p.put("version", (new StringBuffer(String.valueOf(x509.getVersion()))).toString());
        p.put("serial", x509.getSerialNumber().toString());
        Collection col = x509.getSubjectAlternativeNames();
        if (col != null) {
          Object[] o = col.toArray();
          for (int x = 0; x < o.length; x++)
            p.put("subjectDN" + (x + 1), o[x].toString()); 
        } 
      } 
      v.addElement(p);
    } 
    return v;
  }
  
  public static boolean delete(String keystore_path, String keystore_pass, String alias) throws Exception {
    String keystore_format = (keystore_path.toUpperCase().trim().indexOf("PFX") >= 0 || keystore_path.toUpperCase().trim().indexOf("PKCS12") >= 0) ? "PKCS12" : "JKS";
    KeyStore jks = KeyStore.getInstance(keystore_format);
    jks.load(new FileInputStream(new File_S(keystore_path)), keystore_pass.toCharArray());
    jks.deleteEntry(alias);
    FileOutputStream out = new FileOutputStream(new File_S(keystore_path));
    jks.store(out, keystore_pass.toCharArray());
    out.close();
    return true;
  }
  
  public static boolean rename(String keystore_path, String keystore_pass, String alias1, String alias2) throws Exception {
    String keystore_format = (keystore_path.toUpperCase().trim().indexOf("PFX") >= 0 || keystore_path.toUpperCase().trim().indexOf("PKCS12") >= 0) ? "PKCS12" : "JKS";
    KeyStore jks = KeyStore.getInstance(keystore_format);
    jks.load(new FileInputStream(new File_S(keystore_path)), keystore_pass.toCharArray());
    KeyStore.Entry entry = jks.getEntry(alias1, null);
    jks.setEntry(alias2, entry, null);
    jks.deleteEntry(alias1);
    FileOutputStream out = new FileOutputStream(new File_S(keystore_path));
    jks.store(out, keystore_pass.toCharArray());
    out.close();
    return true;
  }
  
  public static String export(String keystore_path, String keystore_pass, String alias) throws Exception {
    String keystore_format = (keystore_path.toUpperCase().trim().indexOf("PFX") >= 0 || keystore_path.toUpperCase().trim().indexOf("PKCS12") >= 0) ? "PKCS12" : "JKS";
    KeyStore jks = KeyStore.getInstance(keystore_format);
    jks.load(new FileInputStream(new File_S(keystore_path)), keystore_pass.toCharArray());
    Key key = jks.getKey(alias, keystore_pass.toCharArray());
    if (key == null)
      throw new Exception("No such alias in keystore."); 
    String s = "";
    if (key instanceof PrivateKey)
      s = String.valueOf(s) + "-----BEGIN PRIVATE KEY-----\r\n" + Base64.encodeBytes(key.getEncoded()) + "\r\n-----END PRIVATE KEY-----"; 
    if (jks.getCertificate(alias) != null)
      s = String.valueOf(s) + "\r\n-----BEGIN PUBLIC KEY-----\r\n" + Base64.encodeBytes(jks.getCertificate(alias).getPublicKey().getEncoded()) + "\r\n-----END PUBLIC KEY-----"; 
    return s.trim();
  }
  
  public static void addPrivate(String keystore_path, String keystore_pass, String alias, String key_path, String key_pass) throws Exception {
    String keystore_format = (keystore_path.toUpperCase().trim().indexOf("PFX") >= 0 || keystore_path.toUpperCase().trim().indexOf("PKCS12") >= 0) ? "PKCS12" : "JKS";
    KeyStore jks = KeyStore.getInstance(keystore_format);
    jks.load(new FileInputStream(new File_S(keystore_path)), keystore_pass.toCharArray());
    ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
    Common.streamCopier(new FileInputStream(new File_S(key_path)), baos1, false);
    String keydata = new String(baos1.toByteArray());
    String other_keys = "";
    if (keydata.indexOf("-----BEGIN RSA PRIVATE KEY-----") < 0) {
      baos1 = new ByteArrayOutputStream();
      Common.streamCopier(new FileInputStream(new File_S(String.valueOf(key_path) + ".pem")), baos1, false);
      other_keys = new String(baos1.toByteArray());
    } else {
      other_keys = String.valueOf(keydata.substring(0, keydata.indexOf("-----BEGIN RSA PRIVATE KEY-----"))) + keydata.substring(keydata.indexOf("-----END RSA PRIVATE KEY-----") + "-----END RSA PRIVATE KEY-----".length()).trim();
    } 
    PEMParser pp = new PEMParser(new BufferedReader(new StringReader(keydata)));
    Object o = pp.readObject();
    if (o instanceof PEMEncryptedKeyPair)
      o = ((PEMEncryptedKeyPair)o).decryptKeyPair((new JcePEMDecryptorProviderBuilder()).build(key_pass.toCharArray())); 
    PEMKeyPair pemKeyPair = (PEMKeyPair)o;
    KeyPair kp = (new JcaPEMKeyConverter()).getKeyPair(pemKeyPair);
    pp.close();
    PrivateKey priv = kp.getPrivate();
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    Collection c = cf.generateCertificates(new ByteArrayInputStream(other_keys.getBytes()));
    Certificate[] certs = new Certificate[1];
    if (c.size() == 1) {
      Certificate cert = cf.generateCertificate(new ByteArrayInputStream(other_keys.getBytes()));
      certs[0] = cert;
    } else {
      certs = new Certificate[(c.toArray()).length];
      for (int i = 0; i < certs.length; i++)
        certs[i] = (Certificate)c.toArray()[i]; 
    } 
    jks.setKeyEntry(alias, priv, key_pass.toCharArray(), certs);
    for (int x = 0; x < certs.length; x++) {
      String s = "";
      try {
        s = ((X509Certificate)certs[x]).getSubjectDN().getName();
        s = s.substring(s.toUpperCase().indexOf("CN=") + 3);
        s = s.substring(0, s.lastIndexOf(",", s.indexOf("="))).trim();
        if (s.startsWith("\""))
          s = s.substring(1, s.length() - 1); 
      } catch (Exception e) {
        Log.log("SERVER", 1, e);
        s = "unknown_cn_" + x;
      } 
      jks.setCertificateEntry(s, certs[x]);
    } 
    jks.store(new FileOutputStream(new File_S(keystore_path)), keystore_pass.toCharArray());
  }
  
  public static boolean downloadCertificates(String host, int port) {
    boolean ok = false;
    try {
      KeyStore cacerts = KeyStore.getInstance("JKS");
      cacerts.load(new FileInputStream(String.valueOf(System.getProperty("java.home")) + "/lib/security/cacerts"), "changeit".toCharArray());
      Vector last_chain = new Vector();
      TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmf.init(cacerts);
      SSLContext context = SSLContext.getInstance("TLS");
      context.init(null, new TrustManager[] { new X509TrustManager(last_chain, tmf) {
              private final Vector val$last_chain;
              
              private final TrustManagerFactory val$tmf;
              
              public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
              }
              
              public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                throw new UnsupportedOperationException();
              }
              
              public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                for (int x = 0; x < chain.length; x++)
                  this.val$last_chain.addElement(chain[x]); 
                ((X509TrustManager)this.val$tmf.getTrustManagers()[0]).checkServerTrusted(chain, authType);
              }
            } }null);
      Log.log("SERVER", 0, "Connecting to:" + host + ":" + port);
      SSLSocket socket = (SSLSocket)context.getSocketFactory().createSocket(host, port);
      try {
        socket.setSoTimeout(10000);
        socket.startHandshake();
        ok = true;
      } catch (SSLException e) {
        Log.log("SERVER", 1, e);
      } finally {
        socket.close();
      } 
      if (!ok)
        if (last_chain.size() > 0) {
          KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
          InputStream in = new FileInputStream("localcacerts");
          ks.load(in, "changeit".toCharArray());
          in.close();
          for (int x = 0; x < last_chain.size(); x++) {
            X509Certificate cert = last_chain.elementAt(x);
            Log.log("SERVER", 0, String.valueOf(x) + ":Common Name / Subject: " + cert.getSubjectDN());
            Log.log("SERVER", 0, String.valueOf(x) + ":Issuer: " + cert.getIssuerDN());
            ks.setCertificateEntry(String.valueOf(host) + "-" + x, cert);
          } 
          OutputStream out = new FileOutputStream("localcacerts");
          ks.store(out, "changeit".toCharArray());
          out.close();
        } else {
          Log.log("SERVER", 0, "Cert chain couldn't be downloaded...?");
        }  
    } catch (Exception e) {
      Log.log("SERVER", 0, e);
    } 
    return ok;
  }
  
  public static boolean addPublic(String keystore_path, String keystore_pass, String alias, String key_path) throws Exception {
    String keystore_format = (keystore_path.toUpperCase().trim().indexOf("PFX") >= 0 || keystore_path.toUpperCase().trim().indexOf("PKCS12") >= 0) ? "PKCS12" : "JKS";
    KeyStore jks = KeyStore.getInstance(keystore_format);
    jks.load(new FileInputStream(new File_S(keystore_path)), keystore_pass.toCharArray());
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    if ((new File_S(key_path)).length() > 1048576L)
      throw new Exception("File too big."); 
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Common.copyStreams(new FileInputStream(new File_S(key_path)), baos, true, true);
    Certificate[] certs = new Certificate[1];
    Collection c = cf.generateCertificates(new ByteArrayInputStream(baos.toByteArray()));
    if (c.size() == 1) {
      Certificate cert = cf.generateCertificate(new ByteArrayInputStream(baos.toByteArray()));
      certs[0] = cert;
    } else {
      certs = new Certificate[(c.toArray()).length];
      for (int i = 0; i < certs.length; i++)
        certs[i] = (Certificate)c.toArray()[i]; 
    } 
    for (int x = 0; x < certs.length; x++) {
      String s = alias;
      if (certs.length > 1)
        s = String.valueOf(s) + x; 
      jks.setCertificateEntry(s, certs[x]);
    } 
    jks.store(new FileOutputStream(new File_S(keystore_path)), keystore_pass.toCharArray());
    return true;
  }
}
