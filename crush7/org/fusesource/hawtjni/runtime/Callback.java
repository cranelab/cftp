package org.fusesource.hawtjni.runtime;

public class Callback {
  Object object;
  
  String method;
  
  String signature;
  
  int argCount;
  
  long address;
  
  long errorResult;
  
  boolean isStatic;
  
  boolean isArrayBased;
  
  static final String PTR_SIGNATURE = "J";
  
  static final String SIGNATURE_0 = getSignature(0);
  
  static final String SIGNATURE_1 = getSignature(1);
  
  static final String SIGNATURE_2 = getSignature(2);
  
  static final String SIGNATURE_3 = getSignature(3);
  
  static final String SIGNATURE_4 = getSignature(4);
  
  static final String SIGNATURE_N = "([J)J";
  
  public Callback(Object object, String method, int argCount) {
    this(object, method, argCount, false);
  }
  
  public Callback(Object object, String method, int argCount, boolean isArrayBased) {
    this(object, method, argCount, isArrayBased, 0L);
  }
  
  public Callback(Object object, String method, int argCount, boolean isArrayBased, long errorResult) {
    this.object = object;
    this.method = method;
    this.argCount = argCount;
    this.isStatic = object instanceof Class;
    this.isArrayBased = isArrayBased;
    this.errorResult = errorResult;
    if (isArrayBased) {
      this.signature = "([J)J";
    } else {
      switch (argCount) {
        case 0:
          this.signature = SIGNATURE_0;
          break;
        case 1:
          this.signature = SIGNATURE_1;
          break;
        case 2:
          this.signature = SIGNATURE_2;
          break;
        case 3:
          this.signature = SIGNATURE_3;
          break;
        case 4:
          this.signature = SIGNATURE_4;
          break;
        default:
          this.signature = getSignature(argCount);
          break;
      } 
    } 
    this.address = bind(this, object, method, this.signature, argCount, this.isStatic, isArrayBased, errorResult);
  }
  
  static synchronized native long bind(Callback paramCallback, Object paramObject, String paramString1, String paramString2, int paramInt, boolean paramBoolean1, boolean paramBoolean2, long paramLong);
  
  public void dispose() {
    if (this.object == null)
      return; 
    unbind(this);
    this.object = this.method = this.signature = null;
    this.address = 0L;
  }
  
  public long getAddress() {
    return this.address;
  }
  
  public static native String getPlatform();
  
  public static native int getEntryCount();
  
  static String getSignature(int argCount) {
    String signature = "(";
    for (int i = 0; i < argCount; i++)
      signature = signature + "J"; 
    signature = signature + ")J";
    return signature;
  }
  
  public static final synchronized native void setEnabled(boolean paramBoolean);
  
  public static final synchronized native boolean getEnabled();
  
  public static final synchronized native void reset();
  
  static final synchronized native void unbind(Callback paramCallback);
}
