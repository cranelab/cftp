package com.hierynomus.asn1.types;

public enum ASN1TagClass {
  Universal(0),
  Application(64),
  ContextSpecific(128),
  Private(192);
  
  private int value;
  
  ASN1TagClass(int value) {
    this.value = value;
  }
  
  public static ASN1TagClass parseClass(byte tagByte) {
    int classValue = tagByte & 0xC0;
    byte b;
    int i;
    ASN1TagClass[] arrayOfASN1TagClass;
    for (i = (arrayOfASN1TagClass = values()).length, b = 0; b < i; ) {
      ASN1TagClass asn1TagClass = arrayOfASN1TagClass[b];
      if (asn1TagClass.value == classValue)
        return asn1TagClass; 
      b++;
    } 
    throw new IllegalStateException("Could not parse ASN.1 Tag Class (should be impossible)");
  }
  
  public int getValue() {
    return this.value;
  }
}
