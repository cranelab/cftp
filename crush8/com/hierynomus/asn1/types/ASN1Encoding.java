package com.hierynomus.asn1.types;

public enum ASN1Encoding {
  Primitive(0),
  Constructed(32);
  
  private int value;
  
  ASN1Encoding(int value) {
    this.value = value;
  }
  
  public int getValue() {
    return this.value;
  }
  
  public static ASN1Encoding parseEncoding(byte tagByte) {
    if ((tagByte & 0x20) == 0)
      return Primitive; 
    return Constructed;
  }
}
