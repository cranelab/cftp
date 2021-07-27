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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ASN1Sequence extends ASN1Object<List<ASN1Object>> implements ASN1Constructed {
  private final List<ASN1Object> objects;
  
  private byte[] bytes;
  
  private ASN1Sequence(List<ASN1Object> objects, byte[] bytes) {
    super(ASN1Tag.SEQUENCE);
    this.objects = objects;
    this.bytes = bytes;
  }
  
  public ASN1Sequence(List<ASN1Object> objects) {
    super(ASN1Tag.SEQUENCE);
    this.objects = objects;
  }
  
  public List<ASN1Object> getValue() {
    return new ArrayList<>(this.objects);
  }
  
  public Iterator<ASN1Object> iterator() {
    return (new ArrayList<>(this.objects)).iterator();
  }
  
  public int size() {
    return this.objects.size();
  }
  
  public ASN1Object get(int i) {
    return this.objects.get(i);
  }
  
  public static class Parser extends ASN1Parser<ASN1Sequence> {
    public Parser(ASN1Decoder decoder) {
      super(decoder);
    }
    
    public ASN1Sequence parse(ASN1Tag<ASN1Sequence> asn1Tag, byte[] value) throws ASN1ParseException {
      List<ASN1Object> list = new ArrayList<>();
      try {
        Exception exception2, exception1 = null;
      } catch (IOException e) {
        throw new ASN1ParseException(e, "Unable to parse the ASN.1 SEQUENCE contents.", new Object[0]);
      } 
      return new ASN1Sequence(list, value, null);
    }
  }
  
  public static class Serializer extends ASN1Serializer<ASN1Sequence> {
    public Serializer(ASN1Encoder encoder) {
      super(encoder);
    }
    
    public int serializedLength(ASN1Sequence asn1Object) throws IOException {
      if (asn1Object.bytes == null)
        calculateBytes(asn1Object); 
      return asn1Object.bytes.length;
    }
    
    private void calculateBytes(ASN1Sequence asn1Object) throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ASN1OutputStream asn1OutputStream = new ASN1OutputStream(this.encoder, out);
      for (ASN1Object object : asn1Object)
        asn1OutputStream.writeObject(object); 
      asn1Object.bytes = out.toByteArray();
    }
    
    public void serialize(ASN1Sequence asn1Object, ASN1OutputStream stream) throws IOException {
      if (asn1Object.bytes != null) {
        stream.write(asn1Object.bytes);
      } else {
        for (ASN1Object object : asn1Object)
          stream.writeObject(object); 
      } 
    }
  }
}
