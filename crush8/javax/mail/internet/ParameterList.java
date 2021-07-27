package javax.mail.internet;

import com.sun.mail.util.ASCIIUtility;
import com.sun.mail.util.PropUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ParameterList {
  private Map<String, Object> list = new LinkedHashMap<String, Object>();
  
  private Set<String> multisegmentNames;
  
  private Map<String, Object> slist;
  
  private String lastName = null;
  
  private static final boolean encodeParameters = PropUtil.getBooleanSystemProperty("mail.mime.encodeparameters", true);
  
  private static final boolean decodeParameters = PropUtil.getBooleanSystemProperty("mail.mime.decodeparameters", true);
  
  private static final boolean decodeParametersStrict = PropUtil.getBooleanSystemProperty("mail.mime.decodeparameters.strict", false);
  
  private static final boolean applehack = PropUtil.getBooleanSystemProperty("mail.mime.applefilenames", false);
  
  private static final boolean windowshack = PropUtil.getBooleanSystemProperty("mail.mime.windowsfilenames", false);
  
  private static final boolean parametersStrict = PropUtil.getBooleanSystemProperty("mail.mime.parameters.strict", true);
  
  private static final boolean splitLongParameters = PropUtil.getBooleanSystemProperty("mail.mime.splitlongparameters", true);
  
  private static class Value {
    String value;
    
    String charset;
    
    String encodedValue;
    
    private Value() {}
  }
  
  private static class LiteralValue {
    String value;
    
    private LiteralValue() {}
  }
  
  private static class MultiValue extends ArrayList<Object> {
    private static final long serialVersionUID = 699561094618751023L;
    
    String value;
    
    private MultiValue() {}
  }
  
  private static class ParamEnum implements Enumeration<String> {
    private Iterator<String> it;
    
    ParamEnum(Iterator<String> it) {
      this.it = it;
    }
    
    public boolean hasMoreElements() {
      return this.it.hasNext();
    }
    
    public String nextElement() {
      return this.it.next();
    }
  }
  
  public ParameterList() {
    if (decodeParameters) {
      this.multisegmentNames = new HashSet<String>();
      this.slist = new HashMap<String, Object>();
    } 
  }
  
  public ParameterList(String s) throws ParseException {
    this();
    HeaderTokenizer h = new HeaderTokenizer(s, "()<>@,;:\\\"\t []/?=");
    while (true) {
      HeaderTokenizer.Token tk = h.next();
      int type = tk.getType();
      if (type == -4)
        break; 
      if ((char)type == ';') {
        tk = h.next();
        if (tk.getType() == -4)
          break; 
        if (tk.getType() != -1)
          throw new ParseException("In parameter list <" + s + ">, expected parameter name, got \"" + tk
              
              .getValue() + "\""); 
        String name = tk.getValue().toLowerCase(Locale.ENGLISH);
        tk = h.next();
        if ((char)tk.getType() != '=')
          throw new ParseException("In parameter list <" + s + ">, expected '=', got \"" + tk
              
              .getValue() + "\""); 
        if (windowshack && (name
          .equals("name") || name.equals("filename"))) {
          tk = h.next(';', true);
        } else if (parametersStrict) {
          tk = h.next();
        } else {
          tk = h.next(';');
        } 
        type = tk.getType();
        if (type != -1 && type != -2)
          throw new ParseException("In parameter list <" + s + ">, expected parameter value, got \"" + tk
              
              .getValue() + "\""); 
        String value = tk.getValue();
        this.lastName = name;
        if (decodeParameters) {
          putEncodedName(name, value);
          continue;
        } 
        this.list.put(name, value);
        continue;
      } 
      if (type == -1 && this.lastName != null && ((applehack && (this.lastName
        
        .equals("name") || this.lastName
        .equals("filename"))) || !parametersStrict)) {
        String lastValue = (String)this.list.get(this.lastName);
        String value = lastValue + " " + tk.getValue();
        this.list.put(this.lastName, value);
        continue;
      } 
      throw new ParseException("In parameter list <" + s + ">, expected ';', got \"" + tk
          
          .getValue() + "\"");
    } 
    if (decodeParameters)
      combineMultisegmentNames(false); 
  }
  
  public void combineSegments() {
    if (decodeParameters && this.multisegmentNames.size() > 0)
      try {
        combineMultisegmentNames(true);
      } catch (ParseException parseException) {} 
  }
  
  private void putEncodedName(String name, String value) throws ParseException {
    int star = name.indexOf('*');
    if (star < 0) {
      this.list.put(name, value);
    } else if (star == name.length() - 1) {
      name = name.substring(0, star);
      Value v = extractCharset(value);
      try {
        v.value = decodeBytes(v.value, v.charset);
      } catch (UnsupportedEncodingException ex) {
        if (decodeParametersStrict)
          throw new ParseException(ex.toString()); 
      } 
      this.list.put(name, v);
    } else {
      Object v;
      String rname = name.substring(0, star);
      this.multisegmentNames.add(rname);
      this.list.put(rname, "");
      if (name.endsWith("*")) {
        if (name.endsWith("*0*")) {
          v = extractCharset(value);
        } else {
          v = new Value();
          ((Value)v).encodedValue = value;
          ((Value)v).value = value;
        } 
        name = name.substring(0, name.length() - 1);
      } else {
        v = value;
      } 
      this.slist.put(name, v);
    } 
  }
  
  private void combineMultisegmentNames(boolean keepConsistentOnFailure) throws ParseException {
    boolean success = false;
    try {
      Iterator<String> it = this.multisegmentNames.iterator();
      while (it.hasNext()) {
        String name = it.next();
        MultiValue mv = new MultiValue();
        String charset = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int segment;
        for (segment = 0;; segment++) {
          String sname = name + "*" + segment;
          Object v = this.slist.get(sname);
          if (v == null)
            break; 
          mv.add(v);
          try {
            if (v instanceof Value) {
              Value vv = (Value)v;
              if (segment == 0) {
                charset = vv.charset;
              } else if (charset == null) {
                this.multisegmentNames.remove(name);
                break;
              } 
              decodeBytes(vv.value, bos);
            } else {
              bos.write(ASCIIUtility.getBytes((String)v));
            } 
          } catch (IOException iOException) {}
          this.slist.remove(sname);
        } 
        if (segment == 0) {
          this.list.remove(name);
          continue;
        } 
        try {
          if (charset != null)
            charset = MimeUtility.javaCharset(charset); 
          if (charset == null || charset.length() == 0)
            charset = MimeUtility.getDefaultJavaCharset(); 
          if (charset != null) {
            mv.value = bos.toString(charset);
          } else {
            mv.value = bos.toString();
          } 
        } catch (UnsupportedEncodingException uex) {
          if (decodeParametersStrict)
            throw new ParseException(uex.toString()); 
          try {
            mv.value = bos.toString("iso-8859-1");
          } catch (UnsupportedEncodingException unsupportedEncodingException) {}
        } 
        this.list.put(name, mv);
      } 
      success = true;
    } finally {
      if (keepConsistentOnFailure || success) {
        if (this.slist.size() > 0) {
          Iterator<Object> sit = this.slist.values().iterator();
          while (sit.hasNext()) {
            Object v = sit.next();
            if (v instanceof Value) {
              Value vv = (Value)v;
              try {
                vv
                  .value = decodeBytes(vv.value, vv.charset);
              } catch (UnsupportedEncodingException ex) {
                if (decodeParametersStrict)
                  throw new ParseException(ex.toString()); 
              } 
            } 
          } 
          this.list.putAll(this.slist);
        } 
        this.multisegmentNames.clear();
        this.slist.clear();
      } 
    } 
  }
  
  public int size() {
    return this.list.size();
  }
  
  public String get(String name) {
    String value;
    Object v = this.list.get(name.trim().toLowerCase(Locale.ENGLISH));
    if (v instanceof MultiValue) {
      value = ((MultiValue)v).value;
    } else if (v instanceof LiteralValue) {
      value = ((LiteralValue)v).value;
    } else if (v instanceof Value) {
      value = ((Value)v).value;
    } else {
      value = (String)v;
    } 
    return value;
  }
  
  public void set(String name, String value) {
    name = name.trim().toLowerCase(Locale.ENGLISH);
    if (decodeParameters) {
      try {
        putEncodedName(name, value);
      } catch (ParseException pex) {
        this.list.put(name, value);
      } 
    } else {
      this.list.put(name, value);
    } 
  }
  
  public void set(String name, String value, String charset) {
    if (encodeParameters) {
      Value ev = encodeValue(value, charset);
      if (ev != null) {
        this.list.put(name.trim().toLowerCase(Locale.ENGLISH), ev);
      } else {
        set(name, value);
      } 
    } else {
      set(name, value);
    } 
  }
  
  void setLiteral(String name, String value) {
    LiteralValue lv = new LiteralValue();
    lv.value = value;
    this.list.put(name, lv);
  }
  
  public void remove(String name) {
    this.list.remove(name.trim().toLowerCase(Locale.ENGLISH));
  }
  
  public Enumeration getNames() {
    return new ParamEnum(this.list.keySet().iterator());
  }
  
  public String toString() {
    return toString(0);
  }
  
  public String toString(int used) {
    ToStringBuffer sb = new ToStringBuffer(used);
    Iterator<Map.Entry<String, Object>> e = this.list.entrySet().iterator();
    while (e.hasNext()) {
      Map.Entry<String, Object> ent = e.next();
      String name = ent.getKey();
      Object v = ent.getValue();
      if (v instanceof MultiValue) {
        MultiValue vv = (MultiValue)v;
        name = name + "*";
        for (int i = 0; i < vv.size(); i++) {
          String str1, ns;
          Object va = vv.get(i);
          if (va instanceof Value) {
            ns = name + i + "*";
            str1 = ((Value)va).encodedValue;
          } else {
            ns = name + i;
            str1 = (String)va;
          } 
          sb.addNV(ns, quote(str1));
        } 
        continue;
      } 
      if (v instanceof LiteralValue) {
        String str = ((LiteralValue)v).value;
        sb.addNV(name, quote(str));
        continue;
      } 
      if (v instanceof Value) {
        name = name + "*";
        String str = ((Value)v).encodedValue;
        sb.addNV(name, quote(str));
        continue;
      } 
      String value = (String)v;
      if (value.length() > 60 && splitLongParameters && encodeParameters) {
        int seg = 0;
        name = name + "*";
        while (value.length() > 60) {
          sb.addNV(name + seg, quote(value.substring(0, 60)));
          value = value.substring(60);
          seg++;
        } 
        if (value.length() > 0)
          sb.addNV(name + seg, quote(value)); 
        continue;
      } 
      sb.addNV(name, quote(value));
    } 
    return sb.toString();
  }
  
  private static class ToStringBuffer {
    private int used;
    
    private StringBuffer sb = new StringBuffer();
    
    public ToStringBuffer(int used) {
      this.used = used;
    }
    
    public void addNV(String name, String value) {
      this.sb.append("; ");
      this.used += 2;
      int len = name.length() + value.length() + 1;
      if (this.used + len > 76) {
        this.sb.append("\r\n\t");
        this.used = 8;
      } 
      this.sb.append(name).append('=');
      this.used += name.length() + 1;
      if (this.used + value.length() > 76) {
        String s = MimeUtility.fold(this.used, value);
        this.sb.append(s);
        int lastlf = s.lastIndexOf('\n');
        if (lastlf >= 0) {
          this.used += s.length() - lastlf - 1;
        } else {
          this.used += s.length();
        } 
      } else {
        this.sb.append(value);
        this.used += value.length();
      } 
    }
    
    public String toString() {
      return this.sb.toString();
    }
  }
  
  private static String quote(String value) {
    return MimeUtility.quote(value, "()<>@,;:\\\"\t []/?=");
  }
  
  private static final char[] hex = new char[] { 
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
      'A', 'B', 'C', 'D', 'E', 'F' };
  
  private static Value encodeValue(String value, String charset) {
    byte[] b;
    if (MimeUtility.checkAscii(value) == 1)
      return null; 
    try {
      b = value.getBytes(MimeUtility.javaCharset(charset));
    } catch (UnsupportedEncodingException ex) {
      return null;
    } 
    StringBuffer sb = new StringBuffer(b.length + charset.length() + 2);
    sb.append(charset).append("''");
    for (int i = 0; i < b.length; i++) {
      char c = (char)(b[i] & 0xFF);
      if (c <= ' ' || c >= '' || c == '*' || c == '\'' || c == '%' || "()<>@,;:\\\"\t []/?="
        .indexOf(c) >= 0) {
        sb.append('%').append(hex[c >> 4]).append(hex[c & 0xF]);
      } else {
        sb.append(c);
      } 
    } 
    Value v = new Value();
    v.charset = charset;
    v.value = value;
    v.encodedValue = sb.toString();
    return v;
  }
  
  private static Value extractCharset(String value) throws ParseException {
    Value v = new Value();
    v.value = v.encodedValue = value;
    try {
      int i = value.indexOf('\'');
      if (i < 0) {
        if (decodeParametersStrict)
          throw new ParseException("Missing charset in encoded value: " + value); 
        return v;
      } 
      String charset = value.substring(0, i);
      int li = value.indexOf('\'', i + 1);
      if (li < 0) {
        if (decodeParametersStrict)
          throw new ParseException("Missing language in encoded value: " + value); 
        return v;
      } 
      v.value = value.substring(li + 1);
      v.charset = charset;
    } catch (NumberFormatException nex) {
      if (decodeParametersStrict)
        throw new ParseException(nex.toString()); 
    } catch (StringIndexOutOfBoundsException ex) {
      if (decodeParametersStrict)
        throw new ParseException(ex.toString()); 
    } 
    return v;
  }
  
  private static String decodeBytes(String value, String charset) throws ParseException, UnsupportedEncodingException {
    byte[] b = new byte[value.length()];
    int bi;
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c == '%')
        try {
          String hex = value.substring(i + 1, i + 3);
          c = (char)Integer.parseInt(hex, 16);
          i += 2;
        } catch (NumberFormatException ex) {
          if (decodeParametersStrict)
            throw new ParseException(ex.toString()); 
        } catch (StringIndexOutOfBoundsException ex) {
          if (decodeParametersStrict)
            throw new ParseException(ex.toString()); 
        }  
      b[bi++] = (byte)c;
    } 
    if (charset != null)
      charset = MimeUtility.javaCharset(charset); 
    if (charset == null || charset.length() == 0)
      charset = MimeUtility.getDefaultJavaCharset(); 
    return new String(b, 0, bi, charset);
  }
  
  private static void decodeBytes(String value, OutputStream os) throws ParseException, IOException {
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c == '%')
        try {
          String hex = value.substring(i + 1, i + 3);
          c = (char)Integer.parseInt(hex, 16);
          i += 2;
        } catch (NumberFormatException ex) {
          if (decodeParametersStrict)
            throw new ParseException(ex.toString()); 
        } catch (StringIndexOutOfBoundsException ex) {
          if (decodeParametersStrict)
            throw new ParseException(ex.toString()); 
        }  
      os.write((byte)c);
    } 
  }
}
