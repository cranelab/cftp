package com.sun.mail.imap.protocol;

import com.sun.mail.iap.Argument;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ID {
  private Map<String, String> serverParams = null;
  
  public ID(Response r) throws ProtocolException {
    r.skipSpaces();
    int c = r.peekByte();
    if (c == 78 || c == 110)
      return; 
    if (c != 40)
      throw new ProtocolException("Missing '(' at start of ID"); 
    this.serverParams = new HashMap<String, String>();
    String[] v = r.readStringList();
    if (v != null)
      for (int i = 0; i < v.length; i += 2) {
        String name = v[i];
        if (name == null)
          throw new ProtocolException("ID field name null"); 
        if (i + 1 >= v.length)
          throw new ProtocolException("ID field without value: " + name); 
        String value = v[i + 1];
        this.serverParams.put(name, value);
      }  
    this.serverParams = Collections.unmodifiableMap(this.serverParams);
  }
  
  Map<String, String> getServerParams() {
    return this.serverParams;
  }
  
  static Argument getArgumentList(Map<String, String> clientParams) {
    Argument arg = new Argument();
    if (clientParams == null) {
      arg.writeAtom("NIL");
      return arg;
    } 
    Argument list = new Argument();
    for (Map.Entry<String, String> e : clientParams.entrySet()) {
      list.writeNString(e.getKey());
      list.writeNString(e.getValue());
    } 
    arg.writeArgument(list);
    return arg;
  }
}
