package com.hierynomus.asn1.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface ASN1 {
  @Target({ElementType.FIELD, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  @ParsedBy(parser = com.hierynomus.asn1.types.primitive.ASN1Integer.Parser.class)
  public static @interface ASN1Integer {
    String defaultValue() default "0";
  }
}
