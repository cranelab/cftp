package crushftp.server;

import com.crushftp.client.Common;
import com.crushftp.client.GenericClient;
import com.crushftp.client.VRL;
import com.sun.mail.dsn.DispositionNotification;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.SessionCrush;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.net.Socket;
import java.security.DigestInputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.mail.util.ByteArrayDataSource;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle.asn1.smime.SMIMECapability;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSEnvelopedDataStreamGenerator;
import org.bouncycastle.cms.KeyTransRecipientId;
import org.bouncycastle.cms.RecipientId;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.RecipientInformationStore;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.ZlibExpanderProvider;
import org.bouncycastle.mail.smime.SMIMECompressed;
import org.bouncycastle.mail.smime.SMIMECompressedGenerator;
import org.bouncycastle.mail.smime.SMIMEEnveloped;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.mail.smime.SMIMESignedGenerator;
import org.bouncycastle.operator.InputExpanderProvider;
import org.bouncycastle.util.encoders.Base64;

public class As2Msg {
  public static final String A_RC2 = "rc2";
  
  public static final String A_RC4 = "rc4";
  
  public static final String A_AES_128 = "aes128";
  
  public static final String A_AES_192 = "aes192";
  
  public static final String A_AES_256 = "aes256";
  
  public static final String A_3DES = "3des";
  
  public static final String A_DES = "des";
  
  public static final String A_SHA1 = "sha1";
  
  public static final String A_SHA256 = "sha-256";
  
  public static final String A_MD5 = "md5";
  
  public static final String A_IDEA = "idea";
  
  public static final String A_CAST5 = "cast5";
  
  public static final int E_NONE = 1;
  
  public static final int E_3DES = 2;
  
  public static final int E_RC2_40 = 3;
  
  public static final int E_RC2_64 = 4;
  
  public static final int E_RC2_128 = 5;
  
  public static final int E_RC2_192 = 6;
  
  public static final int E_AES_128 = 8;
  
  public static final int E_AES_192 = 9;
  
  public static final int E_AES_256 = 10;
  
  public static final int E_RC4_40 = 11;
  
  public static final int E_RC4_56 = 12;
  
  public static final int E_RC4_128 = 13;
  
  public static final int E_DES = 15;
  
  public static final int SIG_NONE = 1;
  
  public static final int SIG_SHA1 = 2;
  
  public static final int SIG_MD5 = 3;
  
  public static final int SIG_SHA256 = 4;
  
  public static Properties algorithmLookup = new Properties();
  
  static {
    algorithmLookup.put("none", "1");
    algorithmLookup.put("sha1", "2");
    algorithmLookup.put("md5", "3");
    algorithmLookup.put("sha-256", "4");
    algorithmLookup.put("3des", "2");
    algorithmLookup.put("rc2_40", "3");
    algorithmLookup.put("rc2_64", "4");
    algorithmLookup.put("rc2_128", "5");
    algorithmLookup.put("rc2_192", "6");
    algorithmLookup.put("rc2_unknown", "7");
    algorithmLookup.put("aes_128", "8");
    algorithmLookup.put("aes_192", "9");
    algorithmLookup.put("aes_256", "10");
    algorithmLookup.put("rc4_40", "11");
    algorithmLookup.put("rc4_56", "12");
    algorithmLookup.put("rc4_128", "13");
    algorithmLookup.put("rc4_unknown", "14");
    algorithmLookup.put("des", "15");
  }
  
  public static Properties mdnResponses = new Properties();
  
  public Object createMessage(Properties info, Object inData, boolean compress, int as2SignType, String as2SignKeystoreFormat, String as2SignKeystorePath, String as2SignKeystorePassword, String as2SignKeyPassword, String as2SignKeyAlias, int as2EncryptType, String as2EncryptKeystoreFormat, String as2EncryptKeystorePath, String as2EncryptKeystorePassword, String as2EncryptKeyAlias, String preservedFilename, Properties otherParams) throws Exception {
    MimeBodyPart bodyPart = new MimeBodyPart();
    if (inData instanceof File) {
      bodyPart.setDataHandler(new DataHandler((DataSource)new FileDataSource((File)inData)));
    } else {
      bodyPart.setDataHandler(new DataHandler(new ByteArrayDataSource((byte[])inData, "application/binary")));
    } 
    bodyPart.addHeader("Content-Type", otherParams.getProperty("CONTENT-TYPE", "plain/text"));
    bodyPart.addHeader("Content-Transfer-Encoding", otherParams.getProperty("CONTENT-TRANSFER-ENCODING", "binary"));
    Enumeration keys = otherParams.keys();
    while (keys.hasMoreElements()) {
      String key = (String)keys.nextElement();
      if (key.toUpperCase().startsWith("X-"))
        bodyPart.addHeader(key, otherParams.getProperty(key)); 
    } 
    String newFilename = "";
    if (inData instanceof File) {
      newFilename = ((File)inData).getName().replace(' ', '_').replace('@', '_').replace(':', '_').replace(';', '_').replace('(', '_').replace(')', '_');
    } else if (preservedFilename == null || preservedFilename.equals("")) {
      newFilename = "as2_" + Common.makeBoundary() + ".temp";
    } else {
      newFilename = preservedFilename;
    } 
    if (otherParams.containsKey("Content-Disposition")) {
      bodyPart.addHeader("Content-Disposition", otherParams.getProperty("Content-Disposition", ""));
    } else {
      bodyPart.addHeader("Content-Disposition", "attachment; filename=" + newFilename);
    } 
    String digestOID = null;
    if (as2SignType == 3) {
      digestOID = getAlgNameToId("md5");
    } else if (as2SignType == 4) {
      digestOID = getAlgNameToId("sha-256");
    } else if (as2SignType == 2 || digestOID == null) {
      digestOID = getAlgNameToId("sha1");
    } 
    if (compress)
      bodyPart = compressData(bodyPart); 
    if (digestOID != null) {
      ByteArrayOutputStream bOut = new ByteArrayOutputStream();
      bodyPart.writeTo(bOut);
      bOut.close();
      String mic = calculateMIC(new ByteArrayInputStream(bOut.toByteArray()), digestOID);
      String signtype = "sha1";
      if (as2SignType == 3)
        signtype = "md5"; 
      if (as2SignType == 4)
        signtype = "sha-256"; 
      info.put("mic", String.valueOf(mic) + ", " + signtype);
    } 
    MimeMessage messagePart = new MimeMessage(Session.getInstance(System.getProperties(), null));
    if (as2SignType != 1) {
      MimeMultipart signedPart = signMessage(as2SignKeystoreFormat, as2SignKeystorePath, as2SignKeystorePassword, as2SignKeyPassword, as2SignKeyAlias, bodyPart, as2SignType);
      messagePart.setContent(signedPart);
      messagePart.saveChanges();
    } else {
      MimeMultipart unsignedPart = new MimeMultipart();
      unsignedPart.addBodyPart(bodyPart);
      messagePart.setContent(unsignedPart);
      messagePart.saveChanges();
    } 
    ByteArrayOutputStream signedOut = new ByteArrayOutputStream();
    ByteArrayOutputStream completedOut = null;
    try {
      if (as2EncryptType != 1) {
        Enumeration hdrLines = messagePart.getMatchingHeaderLines(new String[] { "Content-Type" });
        while (hdrLines.hasMoreElements())
          signedOut.write((String.valueOf(MimeUtility.unfold(hdrLines.nextElement())) + "\r\n").getBytes()); 
      } 
      messagePart.writeTo(signedOut, new String[] { "Message-ID", "Mime-Version", "Content-Type" });
    } catch (Exception e) {
      Log.log("AS2_SERVER", 1, e);
    } 
    String tmpFilename = String.valueOf(System.currentTimeMillis()) + ".as2dump_signed";
    if (Log.log("AS2_SERVER", 4, "Raw File Data Dumped to disk:" + tmpFilename)) {
      RandomAccessFile tmpOut = new RandomAccessFile(tmpFilename, "rw");
      tmpOut.write(signedOut.toByteArray());
      tmpOut.close();
    } 
    if (as2EncryptType != 1) {
      completedOut = new ByteArrayOutputStream();
      OutputStream converter = encryptData(as2EncryptKeystoreFormat, as2EncryptKeystorePath, as2EncryptKeystorePassword, as2EncryptKeyAlias, as2EncryptType, completedOut);
      converter.write(signedOut.toByteArray());
      converter.close();
    } else {
      completedOut = signedOut;
    } 
    if (inData instanceof File) {
      File f = new File(String.valueOf(Common.all_but_last(((File)inData).getPath())) + "/as2_" + Common.makeBoundary() + ".encrypted");
      FileOutputStream fos = new FileOutputStream(f);
      fos.write(completedOut.toByteArray());
      fos.close();
      return f;
    } 
    return completedOut.toByteArray();
  }
  
  public OutputStream encryptData(String keystoreType, String keystorePath, String keystorePassword, String alias, int encryptionType, OutputStream encryptedFile) throws Exception {
    keystorePassword = ServerStatus.thisObj.common_code.decode_pass(keystorePassword);
    if (keystorePath.toUpperCase().endsWith(".PFX"))
      keystoreType = "pkcs12"; 
    KeyStore keystore = KeyStore.getInstance(keystoreType);
    if (Common.System2.containsKey("crushftp.keystores." + keystorePath.toUpperCase().replace('\\', '/'))) {
      Properties p = (Properties)Common.System2.get("crushftp.keystores." + keystorePath.toUpperCase().replace('\\', '/'));
      keystore.load(new ByteArrayInputStream((byte[])p.get("bytes")), keystorePassword.toCharArray());
    } else {
      keystore.load(new FileInputStream(keystorePath), keystorePassword.toCharArray());
    } 
    X509Certificate certificate = (X509Certificate)keystore.getCertificate(alias);
    if (certificate == null)
      throw new Exception("Could not load key alias " + alias + " from specified keystore:" + keystorePath); 
    CMSEnvelopedDataStreamGenerator dataGenerator = new CMSEnvelopedDataStreamGenerator();
    dataGenerator.addKeyTransRecipient(certificate);
    OutputStream envelopedData = null;
    if (encryptionType == 2) {
      envelopedData = dataGenerator.open(encryptedFile, CMSEnvelopedDataGenerator.DES_EDE3_CBC, "BC");
    } else if (encryptionType == 15) {
      envelopedData = dataGenerator.open(encryptedFile, getAlgNameToId("des"), 56, "BC");
    } else if (encryptionType == 3) {
      envelopedData = dataGenerator.open(encryptedFile, CMSEnvelopedDataGenerator.RC2_CBC, 40, "BC");
    } else if (encryptionType == 4) {
      envelopedData = dataGenerator.open(encryptedFile, CMSEnvelopedDataGenerator.RC2_CBC, 64, "BC");
    } else if (encryptionType == 5) {
      envelopedData = dataGenerator.open(encryptedFile, CMSEnvelopedDataGenerator.RC2_CBC, 128, "BC");
    } else if (encryptionType == 6) {
      envelopedData = dataGenerator.open(encryptedFile, CMSEnvelopedDataGenerator.RC2_CBC, 192, "BC");
    } else if (encryptionType == 8) {
      envelopedData = dataGenerator.open(encryptedFile, CMSEnvelopedDataGenerator.AES128_CBC, "BC");
    } else if (encryptionType == 9) {
      envelopedData = dataGenerator.open(encryptedFile, CMSEnvelopedDataGenerator.AES192_CBC, "BC");
    } else if (encryptionType == 10) {
      envelopedData = dataGenerator.open(encryptedFile, CMSEnvelopedDataGenerator.AES256_CBC, "BC");
    } else if (encryptionType == 11) {
      envelopedData = dataGenerator.open(encryptedFile, getAlgNameToId("rc4"), 40, "BC");
    } else if (encryptionType == 12) {
      envelopedData = dataGenerator.open(encryptedFile, getAlgNameToId("rc4"), 56, "BC");
    } else if (encryptionType == 13) {
      envelopedData = dataGenerator.open(encryptedFile, getAlgNameToId("rc4"), 128, "BC");
    } 
    if (envelopedData == null)
      throw new Exception("Unsupported encryption type " + encryptionType); 
    return envelopedData;
  }
  
  public MimeMultipart signMessage(String keystoreType, String keystorePath, String keystorePassword, String keyPassword, String alias, MimeBodyPart body, int signType) throws Exception {
    keystorePassword = ServerStatus.thisObj.common_code.decode_pass(keystorePassword);
    keyPassword = ServerStatus.thisObj.common_code.decode_pass(keyPassword);
    if (keystorePath.toUpperCase().endsWith(".PFX"))
      keystoreType = "pkcs12"; 
    KeyStore keystore = KeyStore.getInstance(keystoreType);
    if (Common.System2.containsKey("crushftp.keystores." + keystorePath.toUpperCase().replace('\\', '/'))) {
      Properties p = (Properties)Common.System2.get("crushftp.keystores." + keystorePath.toUpperCase().replace('\\', '/'));
      keystore.load(new ByteArrayInputStream((byte[])p.get("bytes")), keystorePassword.toCharArray());
    } else {
      keystore.load(new FileInputStream(keystorePath), keystorePassword.toCharArray());
    } 
    PrivateKey senderKey = (PrivateKey)keystore.getKey(alias, keyPassword.toCharArray());
    Certificate[] chain = keystore.getCertificateChain(alias);
    if (chain == null)
      throw new Exception("Could not load key alias " + alias + " from specified keystore:" + keystorePath); 
    String digest = null;
    if (signType == 2) {
      digest = "sha1";
    } else if (signType == 3) {
      digest = "md5";
    } else if (signType == 4) {
      digest = "sha-256";
    } else {
      throw new Exception("Unsupported sign type " + signType);
    } 
    return sign(body, chain, senderKey, digest);
  }
  
  public String getAlgNameToId(String a) throws NoSuchAlgorithmException {
    String aid = "";
    if (a.equalsIgnoreCase("md5")) {
      aid = "1.2.840.113549.2.5";
    } else if (a.equalsIgnoreCase("sha1")) {
      aid = "1.3.14.3.2.26";
    } else if (a.equalsIgnoreCase("sha-256")) {
      aid = "2.16.840.1.101.3.4.2.1";
    } else if (a.equalsIgnoreCase("aes128")) {
      aid = CMSEnvelopedDataGenerator.AES128_CBC;
    } else if (a.equalsIgnoreCase("aes192")) {
      aid = CMSEnvelopedDataGenerator.AES192_CBC;
    } else if (a.equalsIgnoreCase("aes256")) {
      aid = CMSEnvelopedDataGenerator.AES256_CBC;
    } else if (a.equalsIgnoreCase("3des")) {
      aid = "1.2.840.113549.3.7";
    } else if (a.equalsIgnoreCase("des")) {
      aid = "1.3.14.3.2.7";
    } else if (a.equalsIgnoreCase("cast5")) {
      aid = "1.2.840.113533.7.66.10";
    } else if (a.equalsIgnoreCase("idea")) {
      aid = "1.3.6.1.4.1.188.7.1.1.2";
    } else if (a.equalsIgnoreCase("rc2")) {
      aid = CMSEnvelopedDataGenerator.RC2_CBC;
    } else if (a.equalsIgnoreCase("rc4")) {
      aid = "1.2.840.113549.3.4";
    } else {
      throw new NoSuchAlgorithmException("Unknown algorithm: " + a);
    } 
    return aid;
  }
  
  public String getAlgIdToName(String id) throws NoSuchAlgorithmException {
    String aname = "";
    if (id.equalsIgnoreCase("1.2.840.113549.2.5")) {
      aname = "md5";
    } else if (id.equalsIgnoreCase("1.3.14.3.2.26")) {
      aname = "sha1";
    } else if (id.equalsIgnoreCase("2.16.840.1.101.3.4.2.1")) {
      aname = "sha-256";
    } else if (id.equalsIgnoreCase(CMSEnvelopedDataGenerator.AES128_CBC)) {
      aname = "aes128";
    } else if (id.equalsIgnoreCase(CMSEnvelopedDataGenerator.AES192_CBC)) {
      aname = "aes192";
    } else if (id.equalsIgnoreCase(CMSEnvelopedDataGenerator.AES256_CBC)) {
      aname = "aes256";
    } else if (id.equalsIgnoreCase("1.2.840.113533.7.66.10")) {
      aname = "cast5";
    } else if (id.equalsIgnoreCase(CMSEnvelopedDataGenerator.DES_EDE3_CBC)) {
      aname = "3des";
    } else if (id.equalsIgnoreCase("1.3.14.3.2.7")) {
      aname = "des";
    } else if (id.equalsIgnoreCase("1.3.6.1.4.1.188.7.1.1.2")) {
      aname = "idea";
    } else if (id.equalsIgnoreCase(CMSEnvelopedDataGenerator.RC2_CBC)) {
      aname = "rc2";
    } else if (id.equalsIgnoreCase("1.2.840.113549.3.4")) {
      aname = "rc4";
    } else {
      throw new NoSuchAlgorithmException("Unknown algorithm: " + id);
    } 
    return aname;
  }
  
  public MimeMultipart sign(Object body, Certificate[] chain, Key key, String digest) throws Exception {
    SMIMECapabilityVector allowedSigns = new SMIMECapabilityVector();
    allowedSigns.addCapability(SMIMECapability.rC2_CBC, 128);
    allowedSigns.addCapability(SMIMECapability.dES_CBC);
    allowedSigns.addCapability(SMIMECapability.dES_EDE3_CBC);
    ASN1EncodableVector attrs = new ASN1EncodableVector();
    attrs.add((ASN1Encodable)new SMIMECapabilitiesAttribute(allowedSigns));
    SMIMESignedGenerator gen = new SMIMESignedGenerator();
    if (digest.equalsIgnoreCase("sha1")) {
      gen.addSigner((PrivateKey)key, (X509Certificate)chain[0], SMIMESignedGenerator.DIGEST_SHA1, new AttributeTable(attrs), null);
    } else if (digest.equalsIgnoreCase("md5")) {
      gen.addSigner((PrivateKey)key, (X509Certificate)chain[0], SMIMESignedGenerator.DIGEST_MD5, new AttributeTable(attrs), null);
    } else if (digest.equalsIgnoreCase("sha-256")) {
      gen.addSigner((PrivateKey)key, (X509Certificate)chain[0], SMIMESignedGenerator.DIGEST_SHA256, new AttributeTable(attrs), null);
    } else {
      throw new Exception("Signing digest " + digest + " not supported.");
    } 
    CertStore certStore = CertStore.getInstance("Collection", new CollectionCertStoreParameters(new ArrayList(Arrays.asList((Object[])chain))), "BC");
    gen.addCertificatesAndCRLs(certStore);
    if (body instanceof MimeBodyPart)
      return gen.generate((MimeBodyPart)body, "BC"); 
    if (body instanceof MimeMessage)
      return gen.generate((MimeMessage)body, "BC"); 
    return null;
  }
  
  public String calculateMIC(InputStream data, String digestAlgOID) throws GeneralSecurityException, MessagingException, IOException {
    DigestInputStream digestInputStream = new DigestInputStream(data, MessageDigest.getInstance(digestAlgOID, "BC"));
    byte[] b = new byte[32768];
    do {
    
    } while (digestInputStream.read(b) >= 0);
    digestInputStream.close();
    return new String(Base64.encode(digestInputStream.getMessageDigest().digest()));
  }
  
  public String calculateMIC(Part part, String digestAlgOID) throws GeneralSecurityException, MessagingException, IOException {
    ByteArrayOutputStream bOut = new ByteArrayOutputStream();
    part.writeTo(bOut);
    bOut.close();
    return calculateMIC(new ByteArrayInputStream(bOut.toByteArray()), digestAlgOID);
  }
  
  public Object decryptData(Properties info, Object inData, String contentType, String keystoreType, String keystorePath, String keystorePassword, String keyPassword, String alias) throws Exception {
    Log.log("AS2_SERVER", 2, "decryptData:info:" + info);
    Log.log("AS2_SERVER", 2, "decryptData:inData:" + inData);
    Log.log("AS2_SERVER", 2, "decryptData:contentType:" + contentType);
    Log.log("AS2_SERVER", 2, "decryptData:keystoreType:" + keystoreType);
    Log.log("AS2_SERVER", 2, "decryptData:keystorePath:" + keystorePath);
    Log.log("AS2_SERVER", 2, "decryptData:alias:" + alias);
    Log.log("AS2_SERVER", 3, "decryptData:keystorePassword:" + keystorePassword);
    Log.log("AS2_SERVER", 3, "decryptData:keyPassword:" + keyPassword);
    info.put("encryptType", "1");
    info.put("contentType", (new StringBuffer(String.valueOf(contentType))).toString());
    if (contentType.toLowerCase().indexOf("application/pkcs7-mime") < 0) {
      if (inData instanceof File) {
        File file2 = new File(String.valueOf(((File)inData).getPath()) + ".decrypted");
        ((File)inData).renameTo(file2);
        return file2;
      } 
      return inData;
    } 
    keystorePassword = ServerStatus.thisObj.common_code.decode_pass(keystorePassword);
    keyPassword = ServerStatus.thisObj.common_code.decode_pass(keyPassword);
    if (keystorePath.toUpperCase().endsWith(".PFX"))
      keystoreType = "pkcs12"; 
    KeyStore keystore = KeyStore.getInstance(keystoreType);
    if (Common.System2.containsKey("crushftp.keystores." + keystorePath.toUpperCase().replace('\\', '/'))) {
      Properties p = (Properties)Common.System2.get("crushftp.keystores." + keystorePath.toUpperCase().replace('\\', '/'));
      keystore.load(new ByteArrayInputStream((byte[])p.get("bytes")), keystorePassword.toCharArray());
    } else {
      keystore.load(new FileInputStream(keystorePath), keystorePassword.toCharArray());
    } 
    X509Certificate certificate = (X509Certificate)keystore.getCertificate(alias);
    PrivateKey privateKey = (PrivateKey)keystore.getKey(alias, keyPassword.toCharArray());
    if (privateKey == null)
      throw new Exception("Could not load private key alias " + alias + " from specified keystore:" + keystorePath); 
    MimeBodyPart encryptedBody = new MimeBodyPart();
    encryptedBody.setHeader("content-type", contentType);
    if (inData instanceof File) {
      encryptedBody.setDataHandler(new DataHandler((DataSource)new FileDataSource((File)inData)));
    } else {
      if (((byte[])inData).length == 0)
        throw new Exception("No data was received!"); 
      encryptedBody.setDataHandler(new DataHandler(new ByteArrayDataSource((byte[])inData, "application/binary")));
    } 
    KeyTransRecipientId recipientId = new KeyTransRecipientId(new X500Name(certificate.getIssuerX500Principal().getName()), certificate.getSerialNumber());
    SMIMEEnveloped enveloped = new SMIMEEnveloped(encryptedBody);
    int encryptType = 0;
    String algorithm = getAlgIdToName(enveloped.getEncryptionAlgOID());
    if (algorithm.equals("aes128")) {
      encryptType = 8;
    } else if (algorithm.equals("aes192")) {
      encryptType = 9;
    } else if (algorithm.equals("aes256")) {
      encryptType = 10;
    } else if (algorithm.equals("3des")) {
      encryptType = 2;
    } else if (algorithm.equals("des")) {
      encryptType = 15;
    } else if (algorithm.equals("rc2")) {
      encryptType = 7;
    } else if (algorithm.equals("rc4")) {
      encryptType = 14;
    } else {
      encryptType = 99;
    } 
    Log.log("AS2_SERVER", 2, "decryptData:algorithm:" + algorithm);
    RecipientInformationStore recipients = enveloped.getRecipientInfos();
    RecipientInformation recipient = recipients.get((RecipientId)recipientId);
    if (recipient == null)
      throw new Exception("It seems an invalid cert was used, so we can't continue."); 
    info.put("encryptType", (new StringBuffer(String.valueOf(encryptType))).toString());
    info.put("contentType", (new StringBuffer(String.valueOf(contentType))).toString());
    if (inData instanceof File) {
      Common.copyStreams(recipient.getContentStream(privateKey, "BC").getContentStream(), new FileOutputStream(String.valueOf(((File)inData).getPath()) + ".decrypted"), true, true);
      return new File(String.valueOf(((File)inData).getPath()) + ".decrypted");
    } 
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Common.copyStreams(recipient.getContentStream(privateKey, "BC").getContentStream(), baos, true, true);
    return baos.toByteArray();
  }
  
  public Vector getPayloadsAndMic(Properties info, Object inData, String keystoreType, String keystorePath, String keystorePassword, String alias, String mic_alg) throws Exception {
    Vector attachments = new Vector();
    MimeMultipart multipart = null;
    if (inData instanceof File) {
      multipart = new MimeMultipart((DataSource)new FileDataSource((File)inData));
    } else {
      multipart = new MimeMultipart(new ByteArrayDataSource((byte[])inData, "application/binary"));
    } 
    String contentType = info.getProperty("contentType", "");
    try {
      if (info.getProperty("signType", "").equals("1"))
        throw new Exception("NO SIGNATURE"); 
      try {
        multipart.getCount();
      } catch (Exception e) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SMIMECompressed compressed = new SMIMECompressed(new MimeBodyPart(new ByteArrayInputStream((byte[])inData)));
        baos.write(compressed.getContent((InputExpanderProvider)new ZlibExpanderProvider()));
        inData = baos.toByteArray();
        multipart = new MimeMultipart(new ByteArrayDataSource((byte[])inData, "application/binary"));
        multipart.getCount();
      } 
    } catch (Exception e) {
      Object newData = inData;
      InputStream in = null;
      MimeBodyPart mb = null;
      try {
        if (inData instanceof File) {
          in = new FileInputStream((File)inData);
        } else {
          in = new ByteArrayInputStream((byte[])inData);
        } 
        mb = new MimeBodyPart(in);
      } catch (Exception ee) {
        Log.log("AS2_SERVER", 2, ee);
      } 
      if (info.getProperty("user-agent", "").toUpperCase().indexOf("mendelson") >= 0 || mb.getHeader("Content-Disposition") == null) {
        String newFilename = info.getProperty("content-disposition", "");
        if (newFilename.endsWith(".as2") && info.containsKey("as2Filename"))
          newFilename = info.getProperty("as2Filename"); 
        if (!newFilename.equals("") && newFilename.indexOf("filename=\"") > 0) {
          newFilename = newFilename.substring(newFilename.indexOf("filename=\""));
          newFilename = newFilename.substring(newFilename.indexOf("\"") + 1, newFilename.lastIndexOf("\""));
          if (inData instanceof File) {
            newData = new File(String.valueOf(((File)inData).getParent()) + "/" + newFilename);
            ((File)inData).renameTo((File)newData);
          } else {
            Properties p = new Properties();
            p.put("name", newFilename);
            p.put("data", inData);
            newData = p;
          } 
        } else if (inData instanceof File) {
          newData = new File(String.valueOf(((File)inData).getParent()) + "/" + ((File)inData).getName() + ".as2");
          ((File)inData).renameTo((File)newData);
        } else {
          Properties p = new Properties();
          p.put("name", String.valueOf(Common.makeBoundary()) + ".as2");
          p.put("data", inData);
          newData = p;
        } 
      } else {
        Enumeration heads = mb.getAllHeaderLines();
        while (heads.hasMoreElements())
          Log.log("AS2_SERVER", 2, "HEADER:" + heads.nextElement().toString()); 
        String newFilename = mb.getHeader("Content-Disposition")[0];
        if (newFilename.endsWith(".as2") && info.containsKey("as2Filename"))
          newFilename = info.getProperty("as2Filename"); 
        if (newFilename.indexOf("filename=\"") >= 0) {
          newFilename = newFilename.substring(newFilename.indexOf("filename=\""));
          newFilename = newFilename.substring(newFilename.indexOf("\"") + 1, newFilename.lastIndexOf("\""));
        } else if (newFilename.indexOf("filename=") >= 0) {
          newFilename = newFilename.substring(newFilename.indexOf("filename="));
          newFilename = newFilename.substring(newFilename.indexOf("=") + 1).trim();
        } 
        if (inData instanceof File) {
          newData = new File(String.valueOf(((File)newData).getParent()) + "/" + newFilename);
          Common.copyStreams((InputStream)mb.getContent(), new FileOutputStream((File)newData), true, true);
        } else {
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          if (mb.getHeader("Content-Type")[0].toLowerCase().indexOf("compressed-data") >= 0) {
            SMIMECompressed compressed = new SMIMECompressed(mb);
            baos.write(compressed.getContent((InputExpanderProvider)new ZlibExpanderProvider()));
          } else {
            Common.copyStreams((InputStream)mb.getContent(), baos, true, true);
          } 
          Properties p = new Properties();
          p.put("name", newFilename);
          p.put("data", baos.toByteArray());
          newData = p;
        } 
      } 
      attachments.addElement(newData);
      if (inData instanceof File) {
        FileInputStream fin = new FileInputStream((File)newData);
        info.put("mic", String.valueOf(calculateMIC(fin, getAlgNameToId(mic_alg))) + ", " + mic_alg);
        fin.close();
      } else {
        Properties p = (Properties)newData;
        info.put("mic", String.valueOf(calculateMIC(new ByteArrayInputStream((byte[])p.get("data")), getAlgNameToId(mic_alg))) + ", " + mic_alg);
      } 
      return attachments;
    } 
    keystorePassword = ServerStatus.thisObj.common_code.decode_pass(keystorePassword);
    if (!keystorePath.trim().equals("")) {
      if (keystorePath.toUpperCase().endsWith(".PFX"))
        keystoreType = "pkcs12"; 
      KeyStore keystore = KeyStore.getInstance(keystoreType);
      if (Common.System2.containsKey("crushftp.keystores." + keystorePath.toUpperCase().replace('\\', '/'))) {
        Properties p = (Properties)Common.System2.get("crushftp.keystores." + keystorePath.toUpperCase().replace('\\', '/'));
        keystore.load(new ByteArrayInputStream((byte[])p.get("bytes")), keystorePassword.toCharArray());
      } else {
        keystore.load(new FileInputStream(keystorePath), keystorePassword.toCharArray());
      } 
      boolean ok = false;
      try {
        SignerInformation signer = (new SMIMESigned(multipart)).getSignerInfos().getSigners().iterator().next();
        ok = signer.verify(((X509Certificate)keystore.getCertificate(alias)).getPublicKey(), "BC");
      } catch (Exception e) {
        Log.log("AS2_SERVER", 1, e);
      } 
      if (!ok && keystorePath.indexOf("no_validate") < 0) {
        Log.log("AS2_SERVER", 2, "Response failed signature validation, aborting.");
        throw new Exception("Signature valdiation failed.");
      } 
    } 
    for (int x = 0; x < multipart.getCount(); x++) {
      Log.log("AS2_SERVER", 2, "BodyPart ContentType1:" + multipart.getBodyPart(x).getContentType());
      if (multipart.getBodyPart(x).getContentType().toLowerCase().indexOf("pkcs7-signature") < 0) {
        contentType = multipart.getBodyPart(x).getContentType();
        Log.log("AS2_SERVER", 2, "BodyPart ContentType2:" + multipart.getBodyPart(x).getContentType());
        BodyPart bp = multipart.getBodyPart(x);
        File decompressedPayload = null;
        Log.log("AS2_SERVER", 2, "BodyPart ContentType3:" + bp.getContentType());
        BodyPart bpOriginal = bp;
        if (bp.getContentType().toLowerCase().indexOf("compressed-data") >= 0) {
          MimeBodyPart compressedPart = (MimeBodyPart)multipart.getBodyPart(x);
          SMIMECompressed compressed = new SMIMECompressed(compressedPart);
          if (inData instanceof File) {
            decompressedPayload = new File(String.valueOf(((File)inData).getPath()) + ".decompressed");
            Common.copyStreams(new ByteArrayInputStream(compressed.getContent()), new FileOutputStream(decompressedPayload), true, true);
            bp = new MimeBodyPart(new FileInputStream(decompressedPayload));
          } else {
            bp = new MimeBodyPart(new ByteArrayInputStream(compressed.getContent()));
          } 
        } 
        if (inData instanceof File) {
          File f = new File(String.valueOf(((File)inData).getParentFile().getPath()) + Common.dots("/" + bp.getFileName()));
          attachments.addElement(f);
          Common.copyStreams(bp.getInputStream(), new FileOutputStream(f), true, true);
          if (decompressedPayload != null)
            decompressedPayload.delete(); 
          info.put("mic", calculateContentMIC(inData, Integer.parseInt(info.getProperty("encryptType", "0")), Integer.parseInt(info.getProperty("signType", "0")), bpOriginal, contentType));
          int loops = 0;
          while (!((File)inData).delete() && loops++ < 100)
            Thread.sleep(10L); 
        } else {
          ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
          multipart.writeTo(baos2);
          info.put("mic", calculateContentMIC(baos2.toByteArray(), Integer.parseInt(info.getProperty("encryptType", "0")), Integer.parseInt(info.getProperty("signType", "0")), bpOriginal, contentType));
          baos2.close();
          baos2 = null;
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          Common.copyStreams(bp.getInputStream(), baos, true, true);
          Properties p = new Properties();
          if (contentType.toLowerCase().indexOf("disposition-notification") >= 0) {
            p.put("name", "response.mdn");
            info.put("contentType", contentType);
            String mdn = new String(baos.toByteArray());
            String oid = mdn.substring(mdn.toUpperCase().indexOf("Original-Message-ID:".toUpperCase()));
            oid = oid.substring(oid.indexOf(":") + 1, oid.indexOf("\r")).trim();
            Properties mdnInfo = parseMDN(baos.toByteArray(), contentType);
            if (!Common.System2.containsKey("crushftp.as2_async_mdn"))
              Common.System2.put("crushftp.as2_async_mdn", new Properties()); 
            ((Properties)Common.System2.get("crushftp.as2_async_mdn")).put(oid, mdnInfo);
          } else {
            String filename = bp.getFileName();
            if (filename != null) {
              info.put("as2_real_filename", Common.dots(filename));
              p.put("as2_real_filename", info.getProperty("as2_real_filename"));
            } else {
              if (filename == null)
                filename = "data_" + Common.makeBoundary() + ".as2"; 
              if (info.containsKey("as2Filename"))
                filename = info.getProperty("as2Filename"); 
            } 
            p.put("name", Common.dots(filename));
          } 
          p.put("data", baos.toByteArray());
          attachments.addElement(p);
        } 
      } 
    } 
    return attachments;
  }
  
  public String calculateContentMIC(Object inData, int encryptType, int signType, Part partWithHeader, String contentType) throws Exception {
    MimeBodyPart signedPart = new MimeBodyPart();
    InputStream in = null;
    if (inData instanceof File) {
      signedPart.setDataHandler(new DataHandler((DataSource)new FileDataSource((File)inData)));
      in = new FileInputStream((File)inData);
    } else {
      signedPart.setDataHandler(new DataHandler(new ByteArrayDataSource((byte[])inData, contentType)));
      in = new ByteArrayInputStream((byte[])inData);
    } 
    if (encryptType == 1) {
      signedPart.setHeader("Content-Type", contentType);
    } else {
      signedPart.setHeader("Content-Type", (new MimeBodyPart(in)).getContentType());
    } 
    try {
      if (signType == 1)
        return String.valueOf(calculateMIC(in, getAlgNameToId("sha1"))) + ", sha1"; 
    } finally {
      in.close();
    } 
  }
  
  public String getDigestAlgOIDFromSignature(Part part) throws Exception {
    MimeMultipart signedMultiPart = null;
    if (part.getContent() instanceof MimeMultipart) {
      signedMultiPart = (MimeMultipart)part.getContent();
    } else {
      signedMultiPart = new MimeMultipart(part.getDataHandler().getDataSource());
    } 
    SMIMESigned signed = new SMIMESigned(signedMultiPart);
    SignerInformationStore signerStore = signed.getSignerInfos();
    Iterator iterator = signerStore.getSigners().iterator();
    if (iterator.hasNext())
      return ((SignerInformation)iterator.next()).getDigestAlgOID(); 
    throw new GeneralSecurityException("Can't understand signature algorithm.");
  }
  
  public String createMDN(String receivedMIC, String mic_alg, boolean signMDN, String receiverId, String messageId, String dispositionState, String additionalText, String keystoreType, String keystorePath, String keystorePassword, String keyPassword, String alias) throws Exception {
    MimeMultipart multiPart = new MimeMultipart();
    MimeBodyPart body = new MimeBodyPart();
    body.setText(additionalText);
    body.setHeader("Content-Type", "text/plain");
    body.setHeader("Content-Transfer-Encoding", "7bit");
    multiPart.addBodyPart(body);
    MimeBodyPart bodyDisposition = new MimeBodyPart();
    StringBuffer buffer = new StringBuffer();
    buffer.append("Reporting-UA: test\r\n");
    buffer.append("Original-Recipient: rfc822; " + receiverId + "\r\n");
    buffer.append("Final-Recipient: rfc822; " + receiverId + "\r\n");
    buffer.append("Original-Message-ID: <" + messageId + ">\r\n");
    buffer.append("Disposition: " + dispositionState + "\r\n");
    if (receivedMIC != null)
      buffer.append("Received-Content-MIC: " + receivedMIC + "\r\n"); 
    bodyDisposition.setText(buffer.toString());
    bodyDisposition.setHeader("Content-Type", "message/disposition-notification");
    bodyDisposition.setHeader("Content-Transfer-Encoding", "7bit");
    multiPart.addBodyPart(bodyDisposition);
    multiPart.setSubType("report; report-type=disposition-notification");
    MimeMessage messagePart = new MimeMessage(Session.getInstance(System.getProperties(), null));
    messagePart.setContent(multiPart, MimeUtility.unfold(multiPart.getContentType()));
    messagePart.saveChanges();
    if (signMDN || System.getProperty("crushftp.as2.alwayssign", "true").equals("true")) {
      if (!signMDN)
        Log.log("AS2_SERVER", 0, "Signed MDN not requested, but we are signing anyway."); 
      MimeMessage signedMessage = signMDN(mic_alg, messagePart, keystoreType, keystorePath, keystorePassword, keyPassword, alias);
      ByteArrayOutputStream memOutSigned = new ByteArrayOutputStream();
      signedMessage.writeTo(memOutSigned, new String[] { "Message-ID", "Mime-Version", "Content-Type" });
      memOutSigned.flush();
      memOutSigned.close();
      String str = new String(memOutSigned.toByteArray());
      if (str.startsWith("\r\n"))
        str = str.substring(2); 
      return str;
    } 
    ByteArrayOutputStream memOut = new ByteArrayOutputStream();
    messagePart.writeTo(memOut, new String[] { "Message-ID", "Mime-Version", "Content-Type" });
    memOut.flush();
    memOut.close();
    String s = new String(memOut.toByteArray());
    if (s.startsWith("\r\n"))
      s = s.substring(2); 
    return s;
  }
  
  public MimeMessage signMDN(String mic_alg, MimeMessage mimeMessage, String keystoreType, String keystorePath, String keystorePassword, String keyPassword, String alias) throws Exception {
    if (mic_alg == null)
      return mimeMessage; 
    keystorePassword = ServerStatus.thisObj.common_code.decode_pass(keystorePassword);
    keyPassword = ServerStatus.thisObj.common_code.decode_pass(keyPassword);
    if (keystorePath.toUpperCase().endsWith(".PFX"))
      keystoreType = "pkcs12"; 
    KeyStore keystore = KeyStore.getInstance(keystoreType);
    if (Common.System2.containsKey("crushftp.keystores." + keystorePath.toUpperCase().replace('\\', '/'))) {
      Properties p = (Properties)Common.System2.get("crushftp.keystores." + keystorePath.toUpperCase().replace('\\', '/'));
      keystore.load(new ByteArrayInputStream((byte[])p.get("bytes")), keystorePassword.toCharArray());
    } else {
      keystore.load(new FileInputStream(keystorePath), keystorePassword.toCharArray());
    } 
    PrivateKey senderKey = (PrivateKey)keystore.getKey(alias, keyPassword.toCharArray());
    Certificate[] chain = keystore.getCertificateChain(alias);
    MimeMultipart signedPart = sign(mimeMessage, chain, senderKey, mic_alg.toUpperCase());
    MimeMessage signedMessage = new MimeMessage(Session.getInstance(System.getProperties(), null));
    signedMessage.setContent(signedPart, MimeUtility.unfold(signedPart.getContentType()));
    signedMessage.saveChanges();
    return signedMessage;
  }
  
  public String doAsyncMDNPost(Socket sock, String username, String password, Properties as2Info, boolean expect100, String mdn, String boundary, String destUrl, String keystore_path, String keystore_pass, String cert_pass, boolean acceptAnyCert, String use_dmz) {
    SimpleDateFormat sdf_rfc1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    StringBuffer data_log = new StringBuffer();
    try {
      StringBuffer sb = new StringBuffer();
      sb.append("POST " + (new VRL(destUrl)).getPath() + " HTTP/1.1\r\n");
      if (!username.equals("") && !username.equalsIgnoreCase("anonymous") && !username.equalsIgnoreCase("template"))
        sb.append("Authorization: Basic " + Common.encode64(String.valueOf(username) + ":" + password).trim() + "\r\n"); 
      sb.append("AS2-Version: 1.1\r\n");
      sb.append("Mime-Version: 1.0\r\n");
      sb.append("Message-ID: <AS2-" + (new Date()).getTime() + "-" + Common.makeBoundary(3) + "@" + as2Info.getProperty("as2-to", "") + "_" + as2Info.getProperty("as2-from", "") + ">\r\n");
      Thread.sleep(1L);
      sb.append("AS2-To: " + as2Info.getProperty("as2-from", "").trim() + "\r\n");
      if (as2Info.getProperty("as2-to", "").trim().indexOf(" ") >= 0) {
        sb.append("AS2-From: \"" + as2Info.getProperty("as2-to", "").trim() + "\"\r\n");
      } else {
        sb.append("AS2-From: " + as2Info.getProperty("as2-to", "").trim() + "\r\n");
      } 
      sb.append("Subject: Message Delivery Notification\r\n");
      sb.append("Connection: close\r\n");
      sb.append("Date: " + sdf_rfc1123.format(new Date()) + "\r\n");
      if (expect100)
        sb.append("Expect: 100-continue\r\n"); 
      sb.append("Host: " + (new VRL(destUrl)).getHost() + ":" + (new VRL(destUrl)).getPort() + "\r\n");
      if (as2Info.getProperty("signMdn", "false").equals("true")) {
        sb.append("Content-Type: multipart/signed; boundary=\"" + boundary.substring(2) + "\"; protocol=\"application/pkcs7-signature\"; micalg=SHA1; charset=utf-8\r\n");
      } else {
        sb.append("Content-Type: multipart/report; boundary=\"" + boundary.substring(2) + "\"; report-type=disposition-notification; micalg=SHA1; charset=utf-8\r\n");
      } 
      sb.append("Content-Length: " + mdn.length() + "\r\n");
      sb.append("\r\n");
      if (sock == null)
        sock = new Socket((new VRL(destUrl)).getHost(), (new VRL(destUrl)).getPort()); 
      if ((new VRL(destUrl)).getProtocol().equalsIgnoreCase("HTTPS")) {
        SSLSocketFactory factory = (new Common()).getSSLContext(keystore_path, null, keystore_pass, cert_pass, "TLS", false, acceptAnyCert).getSocketFactory();
        SSLSocket ss = (SSLSocket)factory.createSocket(sock, (new VRL(destUrl)).getHost(), (new VRL(destUrl)).getPort(), true);
        Common.configureSSLTLSSocket(ss);
        ss.setUseClientMode(true);
        ss.startHandshake();
        sock = ss;
      } 
      OutputStream os = sock.getOutputStream();
      BufferedInputStream bis = new BufferedInputStream(sock.getInputStream());
      BufferedReader br = new BufferedReader(new InputStreamReader(bis));
      os.write(sb.toString().getBytes("UTF-8"));
      Log.log("AS2_SERVER", 2, sb.toString());
      data_log.append("WROTE:" + sb.toString().trim() + "\r\n");
      os.flush();
      String data = "";
      if (expect100)
        while ((data = br.readLine()) != null) {
          Log.log("AS2_SERVER", 2, data);
          data_log.append("READ:" + data + "\r\n");
          if (data.equals(""))
            break; 
        }  
      data = "";
      data_log.append("WROTE:" + mdn.trim() + "\r\n");
      os.write(mdn.getBytes());
      Log.log("AS2_SERVER", 2, mdn);
      os.flush();
      while ((data = br.readLine()) != null) {
        Log.log("AS2_SERVER", 2, data);
        data_log.append("READ:" + data + "\r\n");
      } 
      sock.close();
    } catch (Exception e) {
      Log.log("AS2_SERVER", 1, e);
    } 
    return data_log.toString();
  }
  
  public void processAs3File(String fileheader, SessionCrush thisSession, ByteArrayOutputStream as3Out, OutputStream outObject) throws Exception {
    Properties as2Info = getAS3headers(fileheader);
    Properties user = thisSession.user;
    Properties info = new Properties();
    ByteArrayInputStream inAs3 = new ByteArrayInputStream(as3Out.toByteArray());
    for (int x = 0; x < fileheader.indexOf("\r\n\r\n") + 4; x++)
      inAs3.read(); 
    as3Out = new ByteArrayOutputStream();
    Common.copyStreams(inAs3, as3Out, true, false);
    Object outData = decryptData(info, as3Out.toByteArray(), as2Info.getProperty("contentType"), user.getProperty("as2EncryptKeystoreFormat", thisSession.getProperty("as2EncryptKeystoreFormat", "PKCS12")), user.getProperty("as2EncryptKeystorePath", thisSession.getProperty("as2EncryptKeystorePath", ".keystore")), user.getProperty("as2EncryptKeystorePassword", thisSession.getProperty("as2EncryptKeystorePassword", "")), user.getProperty("as2EncryptKeyPassword", thisSession.getProperty("as2EncryptKeyPassword", "")), user.getProperty("as2EncryptKeyAlias", thisSession.getProperty("as2EncryptKeyAlias", "")));
    info.put("content-disposition", as2Info.getProperty("content-disposition", ""));
    Vector payloads = getPayloadsAndMic(info, outData, user.getProperty("as2SignKeystoreFormat", thisSession.getProperty("as2SignKeystoreFormat", "PKCS12")), user.getProperty("as2SignKeystorePath", thisSession.getProperty("as2SignKeystorePath", ".keystore")), user.getProperty("as2SignKeystorePassword", thisSession.getProperty("as2SignKeystorePassword", "")), user.getProperty("as2SignKeyAlias", thisSession.getProperty("as2SignKeyAlias", "")), "sha1");
    String messageId = as2Info.getProperty("message-id", "");
    messageId = messageId.substring(1, messageId.length() - 1);
    String mdn = createMDN(info.getProperty("mic", ""), info.getProperty("mic_alg", "sha1"), as2Info.getProperty("signMdn", "false").equals("true"), as2Info.getProperty("as2-to", ""), messageId, "automatic-action/MDN-sent-automatically; processed", "Received AS3 message.", user.getProperty("as2EncryptKeystoreFormat", thisSession.getProperty("as2EncryptKeystoreFormat", "PKCS12")), user.getProperty("as2EncryptKeystorePath", thisSession.getProperty("as2EncryptKeystorePath", ".keystore")), user.getProperty("as2EncryptKeystorePassword", thisSession.getProperty("as2EncryptKeystorePassword", "")), user.getProperty("as2EncryptKeyPassword", thisSession.getProperty("as2EncryptKeyPassword", "")), user.getProperty("as2EncryptKeyAlias", thisSession.getProperty("as2EncryptKeyAlias", "")));
    BufferedReader sr = new BufferedReader(new StringReader(mdn));
    String boundary = "";
    while (boundary.equals(""))
      boundary = sr.readLine().trim(); 
    sr.close();
    Properties payload = null;
    for (int i = 0; i < payloads.size(); i++) {
      Object o = payloads.elementAt(i);
      payload = (Properties)o;
      byte[] arrayOfByte = (byte[])payload.get("data");
      Common.copyStreams(new ByteArrayInputStream(arrayOfByte), outObject, true, false);
    } 
    String destUrl = null;
    if (!as2Info.getProperty("receipt-delivery-option", "").equals(""))
      destUrl = as2Info.getProperty("receipt-delivery-option", ""); 
    if (destUrl == null)
      destUrl = user.getProperty("as3ResponseUrl", thisSession.getProperty("as3ResponseUrl")); 
    StringBuffer sb = new StringBuffer();
    SimpleDateFormat sdf_rfc1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    sb.append("Date: " + sdf_rfc1123.format(new Date()) + "\r\n");
    sb.append("From: AS3\r\n");
    sb.append("Message-ID: <AS3-" + (new Date()).getTime() + "-" + Common.makeBoundary(3) + "@" + as2Info.getProperty("as2-to", "") + "_" + as2Info.getProperty("as2-from", "") + ">" + "\r\n");
    Thread.sleep(1L);
    sb.append("Mime-Version: 1.0\r\n");
    sb.append("AS3-To: " + as2Info.getProperty("as2-from", "") + "\r\n");
    if (as2Info.getProperty("as2-to", "").trim().indexOf(" ") >= 0) {
      sb.append("AS2-From: \"" + as2Info.getProperty("as2-to", "").trim() + "\"\r\n");
    } else {
      sb.append("AS2-From: " + as2Info.getProperty("as2-to", "").trim() + "\r\n");
    } 
    sb.append("Subject: Message Delivery Notification\r\n");
    sb.append("AS3-Version: 1.1\r\n");
    if (as2Info.getProperty("signMdn", "false").equals("true")) {
      sb.append("Content-Type: multipart/signed; boundary=\"" + boundary.substring(2) + "\"; protocol=\"application/pkcs7-signature\"; micalg=SHA1; charset=utf-8" + "\r\n");
    } else {
      sb.append("Content-Type: multipart/report; boundary=\"" + boundary.substring(2) + "\"; report-type=disposition-notification; micalg=SHA1; charset=utf-8" + "\r\n");
    } 
    sb.append("\r\n");
    sb.append(mdn);
    byte[] b = sb.toString().getBytes();
    if (destUrl != null) {
      if (destUrl.endsWith("/"))
        destUrl = String.valueOf(destUrl) + "/"; 
      destUrl = String.valueOf(destUrl) + payload.getProperty("name") + ".mdn";
      Properties item = new Properties();
      item.put("url", destUrl);
      GenericClient c = thisSession.uVFS.getClient(item);
      try {
        OutputStream v_out = c.upload((new VRL(item.getProperty("url"))).getPath(), 0L, true, true);
        Common.copyStreams(new ByteArrayInputStream(b), v_out, true, true);
      } finally {
        thisSession.uVFS.releaseClient(c);
      } 
    } 
    Thread.sleep(1000L);
  }
  
  public Properties getAS3headers(String fileheader) {
    try {
      if (fileheader.toUpperCase().indexOf("AS3-TO:") >= 0) {
        Log.log("AS2_SERVER", 2, fileheader);
        Properties as2Info = new Properties();
        BufferedReader sr = new BufferedReader(new StringReader(fileheader));
        String data = "data";
        String previousKey = "";
        while (!data.equals("")) {
          data = sr.readLine();
          if (!data.trim().equals("")) {
            if (data.indexOf(":") >= 0) {
              previousKey = data.toLowerCase().substring(0, data.indexOf(":")).trim();
              previousKey = Common.replace_str(previousKey, "as3", "as2");
              String str = data.substring(data.indexOf(":") + 1).trim();
              as2Info.put(previousKey, str);
              ServerSessionAJAX.processAs2HeaderLine(previousKey, str, data, as2Info);
              continue;
            } 
            String val = as2Info.getProperty(previousKey);
            val = String.valueOf(val) + " " + data;
            as2Info.put(previousKey, val);
            ServerSessionAJAX.processAs2HeaderLine(previousKey, val, String.valueOf(previousKey) + ": " + val, as2Info);
          } 
        } 
        return as2Info;
      } 
    } catch (Exception e) {
      Log.log("AS2_SERVER", 1, e);
    } 
    return null;
  }
  
  public Properties doPost(Socket sock, VFS uVFS, String username, String password, boolean expect100, Object inData, String fromPartner, String toPartner, String subject, String from, String recipientUrl, String responseUrl, boolean asyncMDN, String keystore_path, String keystore_pass, String cert_pass, boolean acceptAnyCert, String truststore_path, String truststore_pass, String trust_pass, String keystoreType, String keystorePath, String keystorePassword, String keyPassword, String alias, Properties otherParams, String use_dmz) throws Exception {
    SimpleDateFormat sdf_rfc1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    try {
      String ASX = "AS2";
      if (!recipientUrl.toUpperCase().startsWith("HTTP"))
        ASX = "AS3"; 
      StringBuffer sb = new StringBuffer();
      if (ASX.equals("AS2"))
        sb.append("POST " + (new VRL(recipientUrl)).getPath() + " HTTP/1.1\r\n"); 
      if (ASX.equals("AS2") && !(new VRL(recipientUrl)).getUsername().equals("")) {
        sb.append("Authorization: Basic " + Common.encode64(String.valueOf((new VRL(recipientUrl)).getUsername()) + ":" + (new VRL(recipientUrl)).getPassword()).trim() + "\r\n");
      } else if (ASX.equals("AS2") && !username.equals("") && !password.equals("") && !username.equalsIgnoreCase("anonymous") && !username.equalsIgnoreCase("template")) {
        sb.append("Authorization: Basic " + Common.encode64(String.valueOf(username) + ":" + password).trim() + "\r\n");
      } 
      sb.append(String.valueOf(ASX) + "-Version: 1.1\r\n");
      sb.append("MIME-Version: 1.0\r\n");
      sb.append("Accept: image/gif, */*\r\n");
      sb.append("User-Agent: CrushClient/6.0 (Generic OS 6.0) Java\r\n");
      sb.append("EDIINT-Features: multiple-attachments\r\n");
      if (recipientUrl.toUpperCase().startsWith("HTTP")) {
        sb.append("Recipient-Address: \r\n");
      } else {
        sb.append("Recipient-Address: " + recipientUrl + "\r\n");
      } 
      String oid = "<" + ASX + "-" + (new Date()).getTime() + "-" + Common.makeBoundary(3) + "@" + fromPartner + "_" + toPartner + ">";
      sb.append("Message-ID: " + oid + "\r\n");
      Thread.sleep(1L);
      if (fromPartner.trim().indexOf(" ") >= 0) {
        sb.append(String.valueOf(ASX) + "-From: \"" + fromPartner.trim() + "\"\r\n");
      } else {
        sb.append(String.valueOf(ASX) + "-From: " + fromPartner.trim() + "\r\n");
      } 
      if (toPartner.trim().indexOf(" ") >= 0) {
        sb.append(String.valueOf(ASX) + "-To: \"" + toPartner.trim() + "\"\r\n");
      } else {
        sb.append(String.valueOf(ASX) + "-To: " + toPartner.trim() + "\r\n");
      } 
      sb.append("Subject: " + subject + "\r\n");
      sb.append("From: " + from + "\r\n");
      sb.append("Connection: close\r\n");
      sb.append("Date: " + sdf_rfc1123.format(new Date()) + "\r\n");
      sb.append("Content-Type: application/pkcs7-mime; smime-type=enveloped-data; name=smime.p7m\r\n");
      if (asyncMDN || (responseUrl != null && responseUrl.trim().length() > 0 && responseUrl.toUpperCase().startsWith("HTTP")))
        sb.append("receipt-delivery-option: " + responseUrl + "\r\n"); 
      sb.append("Disposition-notification-to: " + responseUrl + "\r\n");
      sb.append("Disposition-Notification-Options: signed-receipt-protocol=optional, pkcs7-signature; signed-receipt-micalg=" + ServerStatus.SG("as2_mic_alg") + "\r\n");
      if (otherParams.containsKey("Content-Disposition")) {
        sb.append("Content-Disposition: " + otherParams.getProperty("Content-Disposition", "") + "\r\n");
      } else {
        sb.append("Content-Disposition: attachment; filename=smime.p7m\r\n");
      } 
      if (expect100)
        sb.append("Expect: 100-continue\r\n"); 
      sb.append("Host: " + (new VRL(recipientUrl)).getHost() + ":" + (new VRL(recipientUrl)).getPort() + "\r\n");
      if (inData instanceof File) {
        sb.append("Content-Length: " + ((File)inData).length() + "\r\n");
      } else {
        sb.append("Content-Length: " + ((byte[])inData).length + "\r\n");
      } 
      Enumeration keys = otherParams.keys();
      while (keys.hasMoreElements()) {
        String key = (String)keys.nextElement();
        if (key.toUpperCase().startsWith("X-"))
          sb.append(key).append(": ").append(otherParams.getProperty(key)).append("\r\n"); 
      } 
      sb.append("\r\n");
      if (ASX.equals("AS2")) {
        if (sock == null)
          sock = new Socket((new VRL(recipientUrl)).getHost(), (new VRL(recipientUrl)).getPort()); 
        if ((new VRL(recipientUrl)).getProtocol().equalsIgnoreCase("HTTPS")) {
          SSLSocket ss = Common.getSSLSocket(truststore_path, keystore_pass, truststore_pass, true, sock, (new VRL(recipientUrl)).getHost(), (new VRL(recipientUrl)).getPort());
          ss.setUseClientMode(true);
          ss.startHandshake();
          sock = ss;
        } 
        OutputStream os = sock.getOutputStream();
        BufferedInputStream bis = new BufferedInputStream(sock.getInputStream());
        bis.mark(65536);
        int skipBytes = 0;
        BufferedReader br = new BufferedReader(new InputStreamReader(bis));
        os.write(sb.toString().getBytes("UTF-8"));
        Log.log("AS2_SERVER", 2, sb.toString());
        os.flush();
        String data = "";
        if (expect100)
          while ((data = br.readLine()) != null) {
            skipBytes += data.length() + 2;
            Log.log("AS2_SERVER", 2, data);
            if (data.equals(""))
              break; 
          }  
        if (inData instanceof File) {
          Common.copyStreams(new FileInputStream((File)inData), os, true, false);
        } else {
          Common.copyStreams(new ByteArrayInputStream((byte[])inData), os, true, false);
        } 
        os.flush();
        data = "";
        int contentLength = 0;
        String contentType = "";
        boolean chunked = false;
        while ((data = br.readLine()) != null) {
          skipBytes += data.length() + 2;
          if (data.toUpperCase().startsWith("CONTENT-LENGTH:")) {
            contentLength = Integer.parseInt(data.substring(data.indexOf(":") + 1).trim());
          } else if (data.toUpperCase().startsWith("CONTENT-TYPE:")) {
            contentType = data.substring(data.indexOf(":") + 1).trim();
          } 
          if (data.toUpperCase().startsWith("Transfer-Encoding:".toUpperCase()) && data.toUpperCase().indexOf("CHUNKED") > 0)
            chunked = true; 
          Log.log("AS2_SERVER", 2, data);
          if (data.equals(""))
            break; 
        } 
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (chunked) {
          try {
            byte[] b = new byte[32768];
            int bytes_read = 0;
            long content_length = Common.getChunkSize(bis);
            while (content_length > 0L) {
              while (content_length > 0L && bytes_read >= 0) {
                if (content_length < b.length)
                  b = new byte[(int)content_length]; 
                bytes_read = bis.read(b);
                if (bytes_read > 0) {
                  content_length -= bytes_read;
                  baos.write(b, 0, bytes_read);
                } 
              } 
              Common.getChunkSize(bis);
              content_length = Common.getChunkSize(bis);
            } 
            baos.flush();
          } catch (Exception e) {
            Log.log("AS2_SERVER", 1, e);
          } 
        } else {
          int bytesRead = 0;
          InputStream in = bis;
          bis.reset();
          bis.skip(skipBytes);
          byte[] b = new byte[32768];
          while (contentLength > 0 && bytesRead >= 0) {
            bytesRead = in.read(b);
            if (bytesRead >= 0) {
              contentLength -= bytesRead;
              baos.write(b, 0, bytesRead);
            } 
          } 
          in.close();
        } 
        sock.close();
        Log.log("AS2_SERVER", 2, "MDNBytes:" + baos.size());
        Log.log("AS2_SERVER", 2, "oid:" + oid);
        Log.log("AS2_SERVER", 2, "contentType:" + contentType);
        if ((baos.toByteArray()).length == 0 || asyncMDN || (responseUrl != null && responseUrl.trim().length() > 0 && responseUrl.toUpperCase().startsWith("HTTP"))) {
          Properties properties = new Properties();
          properties.put("oid", oid);
          properties.put("async", "true");
          return properties;
        } 
        Log.log("AS2_SERVER", 2, new String(baos.toByteArray()));
        if (keystorePath.toUpperCase().endsWith(".PFX"))
          keystoreType = "pkcs12"; 
        if (!keystorePath.equals(""))
          getPayloadsAndMic(new Properties(), baos.toByteArray(), keystoreType, keystorePath, keystorePassword, alias, "sha1"); 
        Properties mdnInfo = parseMDN(baos.toByteArray(), contentType);
        mdnInfo.put("oid", oid);
        mdnInfo.put("async", "false");
        return mdnInfo;
      } 
      if (ASX.equals("AS3")) {
        Properties item = new Properties();
        item.put("url", recipientUrl);
        GenericClient c = uVFS.getClient(item);
        try {
          OutputStream v_out = c.upload((new VRL(item.getProperty("url"))).getPath(), 0L, true, true);
          v_out.write(sb.toString().getBytes("UTF8"));
          Log.log("AS2_SERVER", 2, sb.toString());
          if (inData instanceof File) {
            Common.copyStreams(new FileInputStream((File)inData), v_out, true, true);
          } else {
            Common.copyStreams(new ByteArrayInputStream((byte[])inData), v_out, true, true);
          } 
        } finally {
          uVFS.releaseClient(c);
        } 
        Properties p = new Properties();
        p.put("text", "File uploaded.");
        return p;
      } 
    } catch (Exception e) {
      Log.log("AS2_SERVER", 0, e);
      throw e;
    } 
    return null;
  }
  
  public Properties parseMDN(byte[] data, String contentType) throws Exception {
    MimeMultipart multipart = new MimeMultipart(new ByteArrayDataSource(data, contentType));
    MimeMessage message = new MimeMessage(Session.getInstance(System.getProperties(), null));
    message.setContent(multipart, MimeUtility.unfold(multipart.getContentType()));
    message.saveChanges();
    return getMDNInfo(parseReports(message));
  }
  
  public Part parseReports(Part part) throws Exception {
    if (part.getContentType().toLowerCase().startsWith("multipart/report"))
      return part; 
    if (part.isMimeType("multipart/*")) {
      Multipart multiPart = (Multipart)part.getContent();
      for (int x = 0; x < multiPart.getCount(); x++) {
        Part foundPart = parseReports(multiPart.getBodyPart(x));
        if (foundPart != null)
          return foundPart; 
      } 
    } 
    Log.log("AS2_SERVER", 2, "Multipart report now found.");
    return null;
  }
  
  public Properties getMDNInfo(Part report) throws Exception {
    if (report == null) {
      Log.log("AS2_SERVER", 2, "Report was null, returning null.");
      return null;
    } 
    Properties mdnInfo = new Properties();
    if (report.isMimeType("multipart/*")) {
      Multipart multiPart = (Multipart)report.getContent();
      for (int x = 0; x < multiPart.getCount(); x++) {
        BodyPart body = multiPart.getBodyPart(x);
        if (body.getContentType().toLowerCase().startsWith("text/plain")) {
          mdnInfo.put("text", body.getContent().toString().trim());
        } else if (body.getContentType().toLowerCase().startsWith("message/disposition-notification")) {
          if (body.getContent() instanceof InputStream) {
            InputStream inStream = (InputStream)body.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
            String line = "";
            while ((line = reader.readLine()) != null) {
              if (line.indexOf(':') >= 0) {
                String key = line.substring(0, line.indexOf(':')).toLowerCase();
                String value = line.substring(line.indexOf(':') + 1).trim();
                mdnInfo.put(key, value);
              } 
            } 
            inStream.close();
          } else if (body.getContent() instanceof DispositionNotification) {
            try {
              DispositionNotification dn = (DispositionNotification)body.getContent();
              InternetHeaders ih = dn.getNotifications();
              Enumeration heads = ih.getAllHeaders();
              while (heads.hasMoreElements()) {
                Header h = heads.nextElement();
                mdnInfo.put(h.getName().toLowerCase(), h.getValue());
              } 
            } catch (Exception e) {
              Log.log("AS2_SERVER", 1, e);
            } 
          } 
        } 
      } 
    } 
    return mdnInfo;
  }
  
  public byte[] decompressData(byte[] data, String contentType) throws Exception {
    MimeBodyPart compressedPart = new MimeBodyPart();
    compressedPart.setDataHandler(new DataHandler(new ByteArrayDataSource(data, contentType)));
    compressedPart.setHeader("content-type", contentType);
    SMIMECompressed compressed = new SMIMECompressed(compressedPart);
    return compressed.getContent();
  }
  
  public MimeBodyPart compressData(byte[] data, String contentType) throws Exception {
    MimeBodyPart body = new MimeBodyPart();
    body.setDataHandler(new DataHandler(new ByteArrayDataSource(data, contentType)));
    body.addHeader("Content-Type", contentType);
    body.addHeader("Content-Transfer-Encoding", "binary");
    return compressData(body);
  }
  
  public MimeBodyPart compressData(MimeBodyPart body) throws Exception {
    SMIMECompressedGenerator gen = new SMIMECompressedGenerator();
    gen.setContentTransferEncoding("binary");
    return gen.generate(body, "1.2.840.113549.1.9.16.3.8");
  }
}
