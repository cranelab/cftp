package com.hierynomus.spnego;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.iana.IANAObjectIdentifiers;

public class ObjectIdentifiers {
  public static final ASN1ObjectIdentifier SPNEGO = IANAObjectIdentifiers.security_mechanisms.branch("2");
}
