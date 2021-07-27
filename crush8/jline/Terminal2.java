package jline;

public interface Terminal2 extends Terminal {
  boolean getBooleanCapability(String paramString);
  
  Integer getNumericCapability(String paramString);
  
  String getStringCapability(String paramString);
}
