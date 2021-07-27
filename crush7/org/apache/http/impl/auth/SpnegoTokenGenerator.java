package org.apache.http.impl.auth;

import java.io.IOException;

@Deprecated
public interface SpnegoTokenGenerator {
  byte[] generateSpnegoDERObject(byte[] paramArrayOfbyte) throws IOException;
}
