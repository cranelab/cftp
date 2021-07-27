package javax.mail;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Vector;

public class Flags implements Cloneable, Serializable {
  private int system_flags = 0;
  
  private Hashtable<String, String> user_flags = null;
  
  private static final int ANSWERED_BIT = 1;
  
  private static final int DELETED_BIT = 2;
  
  private static final int DRAFT_BIT = 4;
  
  private static final int FLAGGED_BIT = 8;
  
  private static final int RECENT_BIT = 16;
  
  private static final int SEEN_BIT = 32;
  
  private static final int USER_BIT = -2147483648;
  
  private static final long serialVersionUID = 6243590407214169028L;
  
  public Flags() {}
  
  public static final class Flag {
    public static final Flag ANSWERED = new Flag(1);
    
    public static final Flag DELETED = new Flag(2);
    
    public static final Flag DRAFT = new Flag(4);
    
    public static final Flag FLAGGED = new Flag(8);
    
    public static final Flag RECENT = new Flag(16);
    
    public static final Flag SEEN = new Flag(32);
    
    public static final Flag USER = new Flag(-2147483648);
    
    private int bit;
    
    private Flag(int bit) {
      this.bit = bit;
    }
  }
  
  public Flags(Flags flags) {
    this.system_flags = flags.system_flags;
    if (flags.user_flags != null)
      this.user_flags = (Hashtable<String, String>)flags.user_flags.clone(); 
  }
  
  public Flags(Flag flag) {
    this.system_flags |= flag.bit;
  }
  
  public Flags(String flag) {
    this.user_flags = new Hashtable<String, String>(1);
    this.user_flags.put(flag.toLowerCase(Locale.ENGLISH), flag);
  }
  
  public void add(Flag flag) {
    this.system_flags |= flag.bit;
  }
  
  public void add(String flag) {
    if (this.user_flags == null)
      this.user_flags = new Hashtable<String, String>(1); 
    this.user_flags.put(flag.toLowerCase(Locale.ENGLISH), flag);
  }
  
  public void add(Flags f) {
    this.system_flags |= f.system_flags;
    if (f.user_flags != null) {
      if (this.user_flags == null)
        this.user_flags = new Hashtable<String, String>(1); 
      Enumeration<String> e = f.user_flags.keys();
      while (e.hasMoreElements()) {
        String s = e.nextElement();
        this.user_flags.put(s, f.user_flags.get(s));
      } 
    } 
  }
  
  public void remove(Flag flag) {
    this.system_flags &= flag.bit ^ 0xFFFFFFFF;
  }
  
  public void remove(String flag) {
    if (this.user_flags != null)
      this.user_flags.remove(flag.toLowerCase(Locale.ENGLISH)); 
  }
  
  public void remove(Flags f) {
    this.system_flags &= f.system_flags ^ 0xFFFFFFFF;
    if (f.user_flags != null) {
      if (this.user_flags == null)
        return; 
      Enumeration<String> e = f.user_flags.keys();
      while (e.hasMoreElements())
        this.user_flags.remove(e.nextElement()); 
    } 
  }
  
  public boolean contains(Flag flag) {
    return ((this.system_flags & flag.bit) != 0);
  }
  
  public boolean contains(String flag) {
    if (this.user_flags == null)
      return false; 
    return this.user_flags.containsKey(flag.toLowerCase(Locale.ENGLISH));
  }
  
  public boolean contains(Flags f) {
    if ((f.system_flags & this.system_flags) != f.system_flags)
      return false; 
    if (f.user_flags != null) {
      if (this.user_flags == null)
        return false; 
      Enumeration<String> e = f.user_flags.keys();
      while (e.hasMoreElements()) {
        if (!this.user_flags.containsKey(e.nextElement()))
          return false; 
      } 
    } 
    return true;
  }
  
  public boolean equals(Object obj) {
    if (!(obj instanceof Flags))
      return false; 
    Flags f = (Flags)obj;
    if (f.system_flags != this.system_flags)
      return false; 
    if (f.user_flags == null && this.user_flags == null)
      return true; 
    if (f.user_flags != null && this.user_flags != null && f.user_flags
      .size() == this.user_flags.size()) {
      Enumeration<String> e = f.user_flags.keys();
      while (e.hasMoreElements()) {
        if (!this.user_flags.containsKey(e.nextElement()))
          return false; 
      } 
      return true;
    } 
    return false;
  }
  
  public int hashCode() {
    int hash = this.system_flags;
    if (this.user_flags != null) {
      Enumeration<String> e = this.user_flags.keys();
      while (e.hasMoreElements())
        hash += ((String)e.nextElement()).hashCode(); 
    } 
    return hash;
  }
  
  public Flag[] getSystemFlags() {
    Vector<Flag> v = new Vector<Flag>();
    if ((this.system_flags & 0x1) != 0)
      v.addElement(Flag.ANSWERED); 
    if ((this.system_flags & 0x2) != 0)
      v.addElement(Flag.DELETED); 
    if ((this.system_flags & 0x4) != 0)
      v.addElement(Flag.DRAFT); 
    if ((this.system_flags & 0x8) != 0)
      v.addElement(Flag.FLAGGED); 
    if ((this.system_flags & 0x10) != 0)
      v.addElement(Flag.RECENT); 
    if ((this.system_flags & 0x20) != 0)
      v.addElement(Flag.SEEN); 
    if ((this.system_flags & Integer.MIN_VALUE) != 0)
      v.addElement(Flag.USER); 
    Flag[] f = new Flag[v.size()];
    v.copyInto((Object[])f);
    return f;
  }
  
  public String[] getUserFlags() {
    Vector<String> v = new Vector<String>();
    if (this.user_flags != null) {
      Enumeration<String> e = this.user_flags.elements();
      while (e.hasMoreElements())
        v.addElement(e.nextElement()); 
    } 
    String[] f = new String[v.size()];
    v.copyInto((Object[])f);
    return f;
  }
  
  public Object clone() {
    Flags f = null;
    try {
      f = (Flags)super.clone();
    } catch (CloneNotSupportedException cloneNotSupportedException) {}
    if (this.user_flags != null)
      f.user_flags = (Hashtable<String, String>)this.user_flags.clone(); 
    return f;
  }
}
