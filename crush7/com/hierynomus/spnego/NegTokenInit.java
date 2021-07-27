package com.hierynomus.spnego;

import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.protocol.commons.buffer.Endian;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.bouncycastle.asn1.ASN1ApplicationSpecific;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;

public class NegTokenInit extends SpnegoToken {
  private static final String ADS_IGNORE_PRINCIPAL = "not_defined_in_RFC4178@please_ignore";
  
  private List<ASN1ObjectIdentifier> mechTypes = new ArrayList<ASN1ObjectIdentifier>();
  
  private byte[] mechToken;
  
  public NegTokenInit() {
    super(0, "NegTokenInit");
  }
  
  public void write(Buffer<?> buffer) throws SpnegoException {
    try {
      ASN1EncodableVector negTokenInit = new ASN1EncodableVector();
      addMechTypeList(negTokenInit);
      addMechToken(negTokenInit);
      writeGss(buffer, negTokenInit);
    } catch (IOException e) {
      throw new SpnegoException("Unable to write NegTokenInit", e);
    } 
  }
  
  public NegTokenInit read(byte[] bytes) throws SpnegoException {
    return read(new Buffer.PlainBuffer(bytes, Endian.LE));
  }
  
  private NegTokenInit read(Buffer<?> buffer) throws SpnegoException {
    try {
      ASN1InputStream is = new ASN1InputStream(buffer.asInputStream());
      ASN1Primitive applicationSpecific = is.readObject();
      if (!(applicationSpecific instanceof org.bouncycastle.asn1.BERApplicationSpecific) && !(applicationSpecific instanceof org.bouncycastle.asn1.DERApplicationSpecific))
        throw new SpnegoException("Incorrect GSS-API ASN.1 token received, expected to find an [APPLICATION 0], not: " + applicationSpecific); 
      ASN1Sequence implicitSequence = (ASN1Sequence)((ASN1ApplicationSpecific)applicationSpecific).getObject(16);
      ASN1Encodable spnegoOid = implicitSequence.getObjectAt(0);
      if (!(spnegoOid instanceof ASN1ObjectIdentifier))
        throw new SpnegoException("Expected to find the SPNEGO OID (" + ObjectIdentifiers.SPNEGO + "), not: " + spnegoOid); 
      parseSpnegoToken(implicitSequence.getObjectAt(1));
    } catch (IOException ioe) {
      throw new SpnegoException("Could not read NegTokenInit from buffer", ioe);
    } 
    return this;
  }
  
  protected void parseTagged(ASN1TaggedObject asn1TaggedObject) throws SpnegoException {
    if (asn1TaggedObject.getObject().toString().contains("not_defined_in_RFC4178@please_ignore"))
      return; 
    switch (asn1TaggedObject.getTagNo()) {
      case 0:
        readMechTypeList(asn1TaggedObject.getObject());
      case 1:
        return;
      case 2:
        readMechToken(asn1TaggedObject.getObject());
      case 3:
        return;
    } 
    throw new SpnegoException("Unknown Object Tag " + asn1TaggedObject.getTagNo() + " encountered.");
  }
  
  private void readMechToken(ASN1Primitive mechToken) throws SpnegoException {
    if (!(mechToken instanceof ASN1OctetString))
      throw new SpnegoException("Expected the MechToken (OCTET_STRING) contents, not: " + mechToken); 
    this.mechToken = ((ASN1OctetString)mechToken).getOctets();
  }
  
  private void readMechTypeList(ASN1Primitive sequence) throws SpnegoException {
    if (!(sequence instanceof ASN1Sequence))
      throw new SpnegoException("Expected the MechTypeList (SEQUENCE) contents, not: " + sequence); 
    Enumeration<ASN1Encodable> mechTypeElems = ((ASN1Sequence)sequence).getObjects();
    while (mechTypeElems.hasMoreElements()) {
      ASN1Encodable mechType = mechTypeElems.nextElement();
      if (!(mechType instanceof ASN1ObjectIdentifier))
        throw new SpnegoException("Expected a MechType (OBJECT IDENTIFIER) as contents of the MechTypeList, not: " + mechType); 
      this.mechTypes.add((ASN1ObjectIdentifier)mechType);
    } 
  }
  
  private void addMechToken(ASN1EncodableVector negTokenInit) {
    if (this.mechToken != null && this.mechToken.length > 0) {
      DERTaggedObject dERTaggedObject = new DERTaggedObject(true, 2, (ASN1Encodable)new DEROctetString(this.mechToken));
      negTokenInit.add((ASN1Encodable)dERTaggedObject);
    } 
  }
  
  private void addMechTypeList(ASN1EncodableVector negTokenInit) {
    if (this.mechTypes.size() > 0) {
      ASN1EncodableVector supportedMechVector = new ASN1EncodableVector();
      for (ASN1ObjectIdentifier mechType : this.mechTypes)
        supportedMechVector.add((ASN1Encodable)mechType); 
      DERTaggedObject dERTaggedObject = new DERTaggedObject(true, 0, (ASN1Encodable)new DERSequence(supportedMechVector));
      negTokenInit.add((ASN1Encodable)dERTaggedObject);
    } 
  }
  
  public void addSupportedMech(ASN1ObjectIdentifier oid) {
    this.mechTypes.add(oid);
  }
  
  public void setMechToken(byte[] mechToken) {
    this.mechToken = mechToken;
  }
  
  public List<ASN1ObjectIdentifier> getSupportedMechTypes() {
    return this.mechTypes;
  }
}
