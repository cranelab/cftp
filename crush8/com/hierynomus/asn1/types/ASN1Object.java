package com.hierynomus.asn1.types;

public abstract class ASN1Object<T> {
  protected ASN1Tag tag;
  
  protected ASN1Object(ASN1Tag tag) {
    this.tag = tag;
  }
  
  public abstract T getValue();
  
  public boolean equals(Object o) {
    if (this == o)
      return true; 
    if (o == null || getClass() != o.getClass())
      return false; 
    ASN1Object that = (ASN1Object)o;
    if (this.tag != that.tag)
      return false; 
    return (getValue() != null) ? getValue().equals(that.getValue()) : ((that.getValue() == null));
  }
  
  public int hashCode() {
    int result = this.tag.getTag();
    return result;
  }
  
  public String toString() {
    return String.valueOf(getClass().getSimpleName()) + "[" + getValue() + "]";
  }
  
  public ASN1Tag getTag() {
    return this.tag;
  }
}
