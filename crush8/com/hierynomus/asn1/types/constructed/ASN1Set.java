package com.hierynomus.asn1.types.constructed;

import com.hierynomus.asn1.ASN1OutputStream;
import com.hierynomus.asn1.ASN1ParseException;
import com.hierynomus.asn1.ASN1Parser;
import com.hierynomus.asn1.ASN1Serializer;
import com.hierynomus.asn1.encodingrules.ASN1Decoder;
import com.hierynomus.asn1.encodingrules.ASN1Encoder;
import com.hierynomus.asn1.types.ASN1Constructed;
import com.hierynomus.asn1.types.ASN1Object;
import com.hierynomus.asn1.types.ASN1Tag;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ASN1Set extends ASN1Object<Set<ASN1Object>> implements ASN1Constructed {
  private final Set<ASN1Object> objects;
  
  private byte[] bytes;
  
  private ASN1Set(Set<ASN1Object> objects, byte[] bytes) {
    super(ASN1Tag.SET);
    this.objects = objects;
    this.bytes = bytes;
  }
  
  public ASN1Set(Set<ASN1Object> objects) {
    super(ASN1Tag.SET);
    this.objects = new HashSet<>(objects);
  }
  
  public Set<ASN1Object> getValue() {
    return new HashSet<>(this.objects);
  }
  
  public Iterator<ASN1Object> iterator() {
    return (new HashSet<>(this.objects)).iterator();
  }
  
  public static class Parser extends ASN1Parser<ASN1Set> {
    public Parser(ASN1Decoder decoder) {
      super(decoder);
    }
    
    public ASN1Set parse(ASN1Tag<ASN1Set> asn1Tag, byte[] value) throws ASN1ParseException {
      HashSet<ASN1Object> asn1Objects = new HashSet<>();
      try {
        Exception exception2, exception1 = null;
      } catch (IOException e) {
        throw new ASN1ParseException(e, "Could not parse ASN.1 SET contents.", new Object[0]);
      } 
      return new ASN1Set(asn1Objects, value, null);
    }
  }
  
  public static class Serializer extends ASN1Serializer<ASN1Set> {
    public Serializer(ASN1Encoder encoder) {
      super(encoder);
    }
    
    public int serializedLength(ASN1Set asn1Object) throws IOException {
      if (asn1Object.bytes == null)
        calculateBytes(asn1Object); 
      return asn1Object.bytes.length;
    }
    
    private void calculateBytes(ASN1Set asn1Object) throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ASN1OutputStream asn1OutputStream = new ASN1OutputStream(this.encoder, out);
      for (ASN1Object object : asn1Object)
        asn1OutputStream.writeObject(object); 
      asn1Object.bytes = out.toByteArray();
    }
    
    public void serialize(ASN1Set asn1Object, ASN1OutputStream stream) throws IOException {
      if (asn1Object.bytes != null) {
        stream.write(asn1Object.bytes);
      } else {
        for (ASN1Object object : asn1Object)
          stream.writeObject(object); 
      } 
    }
  }
}
