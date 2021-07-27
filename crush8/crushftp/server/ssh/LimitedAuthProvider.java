package crushftp.server.ssh;

import com.maverick.sshd.AuthenticationMechanism;
import com.maverick.sshd.AuthenticationMechanismFactory;
import com.maverick.sshd.AuthenticationProtocol;
import com.maverick.sshd.Authenticator;
import com.maverick.sshd.Connection;
import com.maverick.sshd.KeyboardInteractiveAuthentication;
import com.maverick.sshd.KeyboardInteractiveAuthenticationProvider;
import com.maverick.sshd.PasswordAuthentication;
import com.maverick.sshd.PasswordAuthenticationProvider;
import com.maverick.sshd.PasswordKeyboardInteractiveProvider;
import com.maverick.sshd.PublicKeyAuthentication;
import com.maverick.sshd.PublicKeyAuthenticationProvider;
import com.maverick.sshd.TransportProtocol;
import com.maverick.sshd.UnsupportedChannelException;
import com.maverick.sshd.platform.KeyboardInteractiveProvider;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LimitedAuthProvider implements AuthenticationMechanismFactory {
  static Logger log = LoggerFactory.getLogger(class$0 = Class.forName("com.maverick.sshd.auth.DefaultAuthenticationMechanismFactory"));
  
  static {
    if (class$0 == null)
      try {
      
      } catch (ClassNotFoundException classNotFoundException) {
        throw new NoClassDefFoundError(null.getMessage());
      }  
  }
  
  protected Set supportedMechanisms = new HashSet();
  
  protected List passwordProviders = new ArrayList();
  
  protected List publickeyProviders = new ArrayList();
  
  protected List keyboardInteractiveProviders = new ArrayList();
  
  static Class class$0;
  
  public void addPasswordAuthenticationProvider(PasswordAuthenticationProvider provider) {
    this.passwordProviders.add(provider);
    this.supportedMechanisms.add("password");
  }
  
  public void addPublicKeyAuthenticationProvider(PublicKeyAuthenticationProvider provider) {
    this.publickeyProviders.add(provider);
    this.supportedMechanisms.add("publickey");
  }
  
  public void addKeyboardInteractiveProvider(KeyboardInteractiveAuthenticationProvider provider) {
    this.keyboardInteractiveProviders.add(provider);
    this.supportedMechanisms.add("keyboard-interactive");
  }
  
  public void removePasswordAuthenticationProvider(PasswordAuthenticationProvider provider) {
    this.passwordProviders.remove(provider);
    if (this.passwordProviders.size() == 0)
      this.supportedMechanisms.remove("password"); 
  }
  
  public void removePublicKeyAuthenticationProvider(PublicKeyAuthenticationProvider provider) {
    this.publickeyProviders.remove(provider);
    if (this.publickeyProviders.size() == 0)
      this.supportedMechanisms.remove("publickey"); 
  }
  
  public void removeKeyboardInteractiveProvider(KeyboardInteractiveProvider provider) {
    this.keyboardInteractiveProviders.remove(provider);
  }
  
  public void addProvider(Authenticator provider) {
    if (provider instanceof PasswordAuthenticationProvider) {
      addPasswordAuthenticationProvider((PasswordAuthenticationProvider)provider);
    } else if (provider instanceof PublicKeyAuthenticationProvider) {
      addPublicKeyAuthenticationProvider((PublicKeyAuthenticationProvider)provider);
    } else if (provider instanceof KeyboardInteractiveAuthenticationProvider) {
      addKeyboardInteractiveProvider((KeyboardInteractiveAuthenticationProvider)provider);
    } else {
      throw new IllegalArgumentException(String.valueOf(provider.getClass().getName()) + " is not a supported AuthenticationProvider");
    } 
  }
  
  public AuthenticationMechanism createInstance(String name, TransportProtocol transport, AuthenticationProtocol authentication, Connection con) throws UnsupportedChannelException {
    if (name.equals("password"))
      return (AuthenticationMechanism)new PasswordAuthentication(transport, authentication, con, getPasswordAuthenticationProviders(con)); 
    if (name.equals("publickey"))
      return (AuthenticationMechanism)new PublicKeyAuthentication(transport, authentication, con, getPublicKeyAuthenticationProviders(con)); 
    if (name.equals("keyboard-interactive"))
      return (AuthenticationMechanism)new KeyboardInteractiveAuthentication(transport, authentication, con, getKeyboardInteractiveProviders(con)); 
    throw new UnsupportedChannelException();
  }
  
  public KeyboardInteractiveAuthenticationProvider[] getKeyboardInteractiveProviders(Connection con) {
    if (this.keyboardInteractiveProviders.size() == 0)
      return new KeyboardInteractiveAuthenticationProvider[] { new KeyboardInteractiveAuthenticationProvider(this) {
            final LimitedAuthProvider this$0;
            
            public KeyboardInteractiveProvider createInstance(Connection con) {
              return (KeyboardInteractiveProvider)new PasswordKeyboardInteractiveProvider((PasswordAuthenticationProvider[])this.this$0.passwordProviders.toArray((Object[])new PasswordAuthenticationProvider[0]), con);
            }
          } }; 
    return (KeyboardInteractiveAuthenticationProvider[])this.keyboardInteractiveProviders.toArray((Object[])new KeyboardInteractiveAuthenticationProvider[0]);
  }
  
  public String[] getRequiredMechanisms(Connection con) {
    return con.getContext().getRequiredAuthentications();
  }
  
  public String[] getSupportedMechanisms() {
    return (String[])this.supportedMechanisms.toArray((Object[])new String[0]);
  }
  
  public PublicKeyAuthenticationProvider[] getPublicKeyAuthenticationProviders(Connection con) {
    return (PublicKeyAuthenticationProvider[])this.publickeyProviders.toArray((Object[])new PublicKeyAuthenticationProvider[0]);
  }
  
  public PasswordAuthenticationProvider[] getPasswordAuthenticationProviders(Connection con) {
    return (PasswordAuthenticationProvider[])this.passwordProviders.toArray((Object[])new PasswordAuthenticationProvider[0]);
  }
  
  public Authenticator[] getProviders(String name, Connection con) {
    if (name.equals("password"))
      return (Authenticator[])getPasswordAuthenticationProviders(con); 
    if (name.equals("publickey"))
      return (Authenticator[])getPublicKeyAuthenticationProviders(con); 
    if (name.equals("keyboard-interactive"))
      return (Authenticator[])getKeyboardInteractiveProviders(con); 
    throw new IllegalArgumentException("Unknown provider type");
  }
}
