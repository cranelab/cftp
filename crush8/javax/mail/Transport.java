package javax.mail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.mail.event.TransportEvent;
import javax.mail.event.TransportListener;

public abstract class Transport extends Service {
  private volatile Vector<TransportListener> transportListeners;
  
  public Transport(Session session, URLName urlname) {
    super(session, urlname);
    this.transportListeners = null;
  }
  
  public static void send(Message msg) throws MessagingException {
    msg.saveChanges();
    send0(msg, msg.getAllRecipients(), (String)null, (String)null);
  }
  
  public static void send(Message msg, Address[] addresses) throws MessagingException {
    msg.saveChanges();
    send0(msg, addresses, (String)null, (String)null);
  }
  
  public static void send(Message msg, String user, String password) throws MessagingException {
    msg.saveChanges();
    send0(msg, msg.getAllRecipients(), user, password);
  }
  
  public static void send(Message msg, Address[] addresses, String user, String password) throws MessagingException {
    msg.saveChanges();
    send0(msg, addresses, user, password);
  }
  
  private static void send0(Message msg, Address[] addresses, String user, String password) throws MessagingException {
    if (addresses == null || addresses.length == 0)
      throw new SendFailedException("No recipient addresses"); 
    Map<String, List<Address>> protocols = new HashMap<String, List<Address>>();
    List<Address> invalid = new ArrayList<Address>();
    List<Address> validSent = new ArrayList<Address>();
    List<Address> validUnsent = new ArrayList<Address>();
    for (int i = 0; i < addresses.length; i++) {
      if (protocols.containsKey(addresses[i].getType())) {
        List<Address> v = protocols.get(addresses[i].getType());
        v.add(addresses[i]);
      } else {
        List<Address> w = new ArrayList<Address>();
        w.add(addresses[i]);
        protocols.put(addresses[i].getType(), w);
      } 
    } 
    int dsize = protocols.size();
    if (dsize == 0)
      throw new SendFailedException("No recipient addresses"); 
    Session s = (msg.session != null) ? msg.session : Session.getDefaultInstance(System.getProperties(), null);
    if (dsize == 1) {
      Transport transport = s.getTransport(addresses[0]);
      try {
        if (user != null) {
          transport.connect(user, password);
        } else {
          transport.connect();
        } 
        transport.sendMessage(msg, addresses);
      } finally {
        transport.close();
      } 
      return;
    } 
    MessagingException chainedEx = null;
    boolean sendFailed = false;
    for (List<Address> v : protocols.values()) {
      Address[] protaddresses = new Address[v.size()];
      v.toArray(protaddresses);
      Transport transport;
      if ((transport = s.getTransport(protaddresses[0])) == null) {
        for (int j = 0; j < protaddresses.length; j++)
          invalid.add(protaddresses[j]); 
        continue;
      } 
      try {
        transport.connect();
        transport.sendMessage(msg, protaddresses);
      } catch (SendFailedException sex) {
        sendFailed = true;
        if (chainedEx == null) {
          chainedEx = sex;
        } else {
          chainedEx.setNextException(sex);
        } 
        Address[] a = sex.getInvalidAddresses();
        if (a != null)
          for (int j = 0; j < a.length; j++)
            invalid.add(a[j]);  
        a = sex.getValidSentAddresses();
        if (a != null)
          for (int k = 0; k < a.length; k++)
            validSent.add(a[k]);  
        Address[] c = sex.getValidUnsentAddresses();
        if (c != null)
          for (int l = 0; l < c.length; l++)
            validUnsent.add(c[l]);  
      } catch (MessagingException mex) {
        sendFailed = true;
        if (chainedEx == null) {
          chainedEx = mex;
        } else {
          chainedEx.setNextException(mex);
        } 
      } finally {
        transport.close();
      } 
    } 
    if (sendFailed || invalid.size() != 0 || validUnsent.size() != 0) {
      Address[] a = null, b = null, c = null;
      if (validSent.size() > 0) {
        a = new Address[validSent.size()];
        validSent.toArray(a);
      } 
      if (validUnsent.size() > 0) {
        b = new Address[validUnsent.size()];
        validUnsent.toArray(b);
      } 
      if (invalid.size() > 0) {
        c = new Address[invalid.size()];
        invalid.toArray(c);
      } 
      throw new SendFailedException("Sending failed", chainedEx, a, b, c);
    } 
  }
  
  public abstract void sendMessage(Message paramMessage, Address[] paramArrayOfAddress) throws MessagingException;
  
  public synchronized void addTransportListener(TransportListener l) {
    if (this.transportListeners == null)
      this.transportListeners = new Vector<TransportListener>(); 
    this.transportListeners.addElement(l);
  }
  
  public synchronized void removeTransportListener(TransportListener l) {
    if (this.transportListeners != null)
      this.transportListeners.removeElement(l); 
  }
  
  protected void notifyTransportListeners(int type, Address[] validSent, Address[] validUnsent, Address[] invalid, Message msg) {
    if (this.transportListeners == null)
      return; 
    TransportEvent e = new TransportEvent(this, type, validSent, validUnsent, invalid, msg);
    queueEvent(e, this.transportListeners);
  }
}
