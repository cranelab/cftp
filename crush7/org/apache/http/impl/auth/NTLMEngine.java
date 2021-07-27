package org.apache.http.impl.auth;

public interface NTLMEngine {
  String generateType1Msg(String paramString1, String paramString2) throws NTLMEngineException;
  
  String generateType3Msg(String paramString1, String paramString2, String paramString3, String paramString4, String paramString5) throws NTLMEngineException;
}
