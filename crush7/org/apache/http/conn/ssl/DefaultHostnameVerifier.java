package org.apache.http.conn.ssl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.auth.x500.X500Principal;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.conn.util.DomainType;
import org.apache.http.conn.util.InetAddressUtils;
import org.apache.http.conn.util.PublicSuffixMatcher;

@Contract(threading = ThreadingBehavior.IMMUTABLE_CONDITIONAL)
public final class DefaultHostnameVerifier implements HostnameVerifier {
  enum HostNameType {
    IPv4(7),
    IPv6(7),
    DNS(2);
    
    final int subjectType;
    
    HostNameType(int subjectType) {
      this.subjectType = subjectType;
    }
  }
  
  private final Log log = LogFactory.getLog(getClass());
  
  private final PublicSuffixMatcher publicSuffixMatcher;
  
  public DefaultHostnameVerifier(PublicSuffixMatcher publicSuffixMatcher) {
    this.publicSuffixMatcher = publicSuffixMatcher;
  }
  
  public DefaultHostnameVerifier() {
    this(null);
  }
  
  public boolean verify(String host, SSLSession session) {
    try {
      Certificate[] certs = session.getPeerCertificates();
      X509Certificate x509 = (X509Certificate)certs[0];
      verify(host, x509);
      return true;
    } catch (SSLException ex) {
      if (this.log.isDebugEnabled())
        this.log.debug(ex.getMessage(), ex); 
      return false;
    } 
  }
  
  public void verify(String host, X509Certificate cert) throws SSLException {
    HostNameType hostType = determineHostFormat(host);
    List<SubjectName> subjectAlts = getSubjectAltNames(cert);
    if (subjectAlts != null && !subjectAlts.isEmpty()) {
      switch (hostType) {
        case IPv4:
          matchIPAddress(host, subjectAlts);
          return;
        case IPv6:
          matchIPv6Address(host, subjectAlts);
          return;
      } 
      matchDNSName(host, subjectAlts, this.publicSuffixMatcher);
    } else {
      X500Principal subjectPrincipal = cert.getSubjectX500Principal();
      String cn = extractCN(subjectPrincipal.getName("RFC2253"));
      if (cn == null)
        throw new SSLException("Certificate subject for <" + host + "> doesn't contain " + "a common name and does not have alternative names"); 
      matchCN(host, cn, this.publicSuffixMatcher);
    } 
  }
  
  static void matchIPAddress(String host, List<SubjectName> subjectAlts) throws SSLException {
    for (int i = 0; i < subjectAlts.size(); i++) {
      SubjectName subjectAlt = subjectAlts.get(i);
      if (subjectAlt.getType() == 7 && 
        host.equals(subjectAlt.getValue()))
        return; 
    } 
    throw new SSLPeerUnverifiedException("Certificate for <" + host + "> doesn't match any " + "of the subject alternative names: " + subjectAlts);
  }
  
  static void matchIPv6Address(String host, List<SubjectName> subjectAlts) throws SSLException {
    String normalisedHost = normaliseAddress(host);
    for (int i = 0; i < subjectAlts.size(); i++) {
      SubjectName subjectAlt = subjectAlts.get(i);
      if (subjectAlt.getType() == 7) {
        String normalizedSubjectAlt = normaliseAddress(subjectAlt.getValue());
        if (normalisedHost.equals(normalizedSubjectAlt))
          return; 
      } 
    } 
    throw new SSLPeerUnverifiedException("Certificate for <" + host + "> doesn't match any " + "of the subject alternative names: " + subjectAlts);
  }
  
  static void matchDNSName(String host, List<SubjectName> subjectAlts, PublicSuffixMatcher publicSuffixMatcher) throws SSLException {
    String normalizedHost = host.toLowerCase(Locale.ROOT);
    for (int i = 0; i < subjectAlts.size(); i++) {
      SubjectName subjectAlt = subjectAlts.get(i);
      if (subjectAlt.getType() == 2) {
        String normalizedSubjectAlt = subjectAlt.getValue().toLowerCase(Locale.ROOT);
        if (matchIdentityStrict(normalizedHost, normalizedSubjectAlt, publicSuffixMatcher))
          return; 
      } 
    } 
    throw new SSLPeerUnverifiedException("Certificate for <" + host + "> doesn't match any " + "of the subject alternative names: " + subjectAlts);
  }
  
  static void matchCN(String host, String cn, PublicSuffixMatcher publicSuffixMatcher) throws SSLException {
    String normalizedHost = host.toLowerCase(Locale.ROOT);
    String normalizedCn = cn.toLowerCase(Locale.ROOT);
    if (!matchIdentityStrict(normalizedHost, normalizedCn, publicSuffixMatcher))
      throw new SSLPeerUnverifiedException("Certificate for <" + host + "> doesn't match " + "common name of the certificate subject: " + cn); 
  }
  
  static boolean matchDomainRoot(String host, String domainRoot) {
    if (domainRoot == null)
      return false; 
    return (host.endsWith(domainRoot) && (host.length() == domainRoot.length() || host.charAt(host.length() - domainRoot.length() - 1) == '.'));
  }
  
  private static boolean matchIdentity(String host, String identity, PublicSuffixMatcher publicSuffixMatcher, boolean strict) {
    if (publicSuffixMatcher != null && host.contains(".") && 
      !matchDomainRoot(host, publicSuffixMatcher.getDomainRoot(identity, DomainType.ICANN)))
      return false; 
    int asteriskIdx = identity.indexOf('*');
    if (asteriskIdx != -1) {
      String prefix = identity.substring(0, asteriskIdx);
      String suffix = identity.substring(asteriskIdx + 1);
      if (!prefix.isEmpty() && !host.startsWith(prefix))
        return false; 
      if (!suffix.isEmpty() && !host.endsWith(suffix))
        return false; 
      if (strict) {
        String remainder = host.substring(prefix.length(), host.length() - suffix.length());
        if (remainder.contains("."))
          return false; 
      } 
      return true;
    } 
    return host.equalsIgnoreCase(identity);
  }
  
  static boolean matchIdentity(String host, String identity, PublicSuffixMatcher publicSuffixMatcher) {
    return matchIdentity(host, identity, publicSuffixMatcher, false);
  }
  
  static boolean matchIdentity(String host, String identity) {
    return matchIdentity(host, identity, null, false);
  }
  
  static boolean matchIdentityStrict(String host, String identity, PublicSuffixMatcher publicSuffixMatcher) {
    return matchIdentity(host, identity, publicSuffixMatcher, true);
  }
  
  static boolean matchIdentityStrict(String host, String identity) {
    return matchIdentity(host, identity, null, true);
  }
  
  static String extractCN(String subjectPrincipal) throws SSLException {
    if (subjectPrincipal == null)
      return null; 
    try {
      LdapName subjectDN = new LdapName(subjectPrincipal);
      List<Rdn> rdns = subjectDN.getRdns();
      for (int i = rdns.size() - 1; i >= 0; i--) {
        Rdn rds = rdns.get(i);
        Attributes attributes = rds.toAttributes();
        Attribute cn = attributes.get("cn");
        if (cn != null)
          try {
            Object value = cn.get();
            if (value != null)
              return value.toString(); 
          } catch (NoSuchElementException ignore) {
          
          } catch (NamingException ignore) {} 
      } 
      return null;
    } catch (InvalidNameException e) {
      throw new SSLException(subjectPrincipal + " is not a valid X500 distinguished name");
    } 
  }
  
  static HostNameType determineHostFormat(String host) {
    if (InetAddressUtils.isIPv4Address(host))
      return HostNameType.IPv4; 
    String s = host;
    if (s.startsWith("[") && s.endsWith("]"))
      s = host.substring(1, host.length() - 1); 
    if (InetAddressUtils.isIPv6Address(s))
      return HostNameType.IPv6; 
    return HostNameType.DNS;
  }
  
  static List<SubjectName> getSubjectAltNames(X509Certificate cert) {
    try {
      Collection<List<?>> entries = cert.getSubjectAlternativeNames();
      if (entries == null)
        return Collections.emptyList(); 
      List<SubjectName> result = new ArrayList<SubjectName>();
      for (List<?> entry : entries) {
        Integer type = (entry.size() >= 2) ? (Integer)entry.get(0) : null;
        if (type != null) {
          String s = (String)entry.get(1);
          result.add(new SubjectName(s, type.intValue()));
        } 
      } 
      return result;
    } catch (CertificateParsingException ignore) {
      return Collections.emptyList();
    } 
  }
  
  static String normaliseAddress(String hostname) {
    if (hostname == null)
      return hostname; 
    try {
      InetAddress inetAddress = InetAddress.getByName(hostname);
      return inetAddress.getHostAddress();
    } catch (UnknownHostException unexpected) {
      return hostname;
    } 
  }
}
