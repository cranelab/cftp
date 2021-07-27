package org.apache.http.impl.cookie;

import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.conn.util.PublicSuffixMatcher;
import org.apache.http.cookie.CommonCookieAttributeHandler;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.protocol.HttpContext;

@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class DefaultCookieSpecProvider implements CookieSpecProvider {
  private final CompatibilityLevel compatibilityLevel;
  
  private final PublicSuffixMatcher publicSuffixMatcher;
  
  private final String[] datepatterns;
  
  private final boolean oneHeader;
  
  private volatile CookieSpec cookieSpec;
  
  public enum CompatibilityLevel {
    DEFAULT, IE_MEDIUM_SECURITY;
  }
  
  public DefaultCookieSpecProvider(CompatibilityLevel compatibilityLevel, PublicSuffixMatcher publicSuffixMatcher, String[] datepatterns, boolean oneHeader) {
    this.compatibilityLevel = (compatibilityLevel != null) ? compatibilityLevel : CompatibilityLevel.DEFAULT;
    this.publicSuffixMatcher = publicSuffixMatcher;
    this.datepatterns = datepatterns;
    this.oneHeader = oneHeader;
  }
  
  public DefaultCookieSpecProvider(CompatibilityLevel compatibilityLevel, PublicSuffixMatcher publicSuffixMatcher) {
    this(compatibilityLevel, publicSuffixMatcher, null, false);
  }
  
  public DefaultCookieSpecProvider(PublicSuffixMatcher publicSuffixMatcher) {
    this(CompatibilityLevel.DEFAULT, publicSuffixMatcher, null, false);
  }
  
  public DefaultCookieSpecProvider() {
    this(CompatibilityLevel.DEFAULT, null, null, false);
  }
  
  public CookieSpec create(HttpContext context) {
    if (this.cookieSpec == null)
      synchronized (this) {
        if (this.cookieSpec == null) {
          RFC2965Spec strict = new RFC2965Spec(this.oneHeader, new CommonCookieAttributeHandler[] { new RFC2965VersionAttributeHandler(), new BasicPathHandler(), PublicSuffixDomainFilter.decorate(new RFC2965DomainAttributeHandler(), this.publicSuffixMatcher), new RFC2965PortAttributeHandler(), new BasicMaxAgeHandler(), new BasicSecureHandler(), new BasicCommentHandler(), new RFC2965CommentUrlAttributeHandler(), new RFC2965DiscardAttributeHandler() });
          RFC2109Spec obsoleteStrict = new RFC2109Spec(this.oneHeader, new CommonCookieAttributeHandler[] { new RFC2109VersionHandler(), new BasicPathHandler(), PublicSuffixDomainFilter.decorate(new RFC2109DomainHandler(), this.publicSuffixMatcher), new BasicMaxAgeHandler(), new BasicSecureHandler(), new BasicCommentHandler() });
          (new CommonCookieAttributeHandler[5])[0] = PublicSuffixDomainFilter.decorate(new BasicDomainHandler(), this.publicSuffixMatcher);
          (new CommonCookieAttributeHandler[5])[1] = (this.compatibilityLevel == CompatibilityLevel.IE_MEDIUM_SECURITY) ? new BasicPathHandler() {
              public void validate(Cookie cookie, CookieOrigin origin) throws MalformedCookieException {}
            } : new BasicPathHandler();
          (new CommonCookieAttributeHandler[5])[2] = new BasicSecureHandler();
          (new CommonCookieAttributeHandler[5])[3] = new BasicCommentHandler();
          (new String[1])[0] = "EEE, dd-MMM-yy HH:mm:ss z";
          NetscapeDraftSpec netscapeDraft = new NetscapeDraftSpec(new CommonCookieAttributeHandler[] { null, null, null, null, new BasicExpiresHandler((this.datepatterns != null) ? (String[])this.datepatterns.clone() : new String[1]) });
          this.cookieSpec = new DefaultCookieSpec(strict, obsoleteStrict, netscapeDraft);
        } 
      }  
    return this.cookieSpec;
  }
}
