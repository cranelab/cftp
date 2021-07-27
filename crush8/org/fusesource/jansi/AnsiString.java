package org.fusesource.jansi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AnsiString implements CharSequence {
  private final CharSequence encoded;
  
  private final CharSequence plain;
  
  public AnsiString(CharSequence str) {
    assert str != null;
    this.encoded = str;
    this.plain = chew(str);
  }
  
  private CharSequence chew(CharSequence str) {
    assert str != null;
    ByteArrayOutputStream buff = new ByteArrayOutputStream();
    AnsiOutputStream out = new AnsiOutputStream(buff);
    try {
      out.write(str.toString().getBytes());
      out.flush();
      out.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } 
    return new String(buff.toByteArray());
  }
  
  public CharSequence getEncoded() {
    return this.encoded;
  }
  
  public CharSequence getPlain() {
    return this.plain;
  }
  
  public char charAt(int index) {
    return getEncoded().charAt(index);
  }
  
  public CharSequence subSequence(int start, int end) {
    return getEncoded().subSequence(start, end);
  }
  
  public int length() {
    return getPlain().length();
  }
  
  public boolean equals(Object obj) {
    return getEncoded().equals(obj);
  }
  
  public int hashCode() {
    return getEncoded().hashCode();
  }
  
  public String toString() {
    return getEncoded().toString();
  }
}
