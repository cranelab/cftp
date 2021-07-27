package com.hierynomus.asn1;

import com.hierynomus.asn1.encodingrules.ASN1Encoder;
import com.hierynomus.asn1.types.ASN1Object;
import com.hierynomus.asn1.types.ASN1Tag;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ASN1OutputStream extends FilterOutputStream {
  private final ASN1Encoder encoder;
  
  public ASN1OutputStream(ASN1Encoder encoder, OutputStream out) {
    super(out);
    this.encoder = encoder;
  }
  
  public void writeObject(ASN1Object asn1Object) throws IOException {
    ASN1Tag tag = asn1Object.getTag();
    writeTag(tag);
    ASN1Serializer<ASN1Object> asn1Serializer = asn1Object.getTag().newSerializer(this.encoder);
    writeLength(asn1Serializer.serializedLength(asn1Object));
    asn1Serializer.serialize(asn1Object, this);
  }
  
  private void writeLength(int length) throws IOException {
    if (length < 127) {
      write(length);
    } else {
      int nrBytes = lengthBytes(length);
      write(0x80 | nrBytes);
      for (; nrBytes > 0; nrBytes--)
        write(length >> (nrBytes - 1) * 8); 
    } 
  }
  
  private int lengthBytes(int length) {
    int l = length;
    int nrBytes = 1;
    while (l > 255) {
      nrBytes++;
      l >>= 8;
    } 
    return nrBytes;
  }
  
  private void writeTag(ASN1Tag tag) throws IOException {
    byte tagByte = (byte)(tag.getAsn1TagClass().getValue() | tag.getAsn1Encoding().getValue() | tag.getTag());
    write(tagByte);
  }
}
