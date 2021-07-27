package org.boris.winrun4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

public class Closure {
  private static boolean is64 = Native.IS_64;
  
  private static long invokeId = Native.getMethodId(Closure.class, "invoke", "(JJ)V", false);
  
  private Object callbackObj;
  
  private Method callbackMethod;
  
  private boolean wideChar;
  
  private Class[] params;
  
  private int[] argTypes;
  
  private int returnType;
  
  private FFI.CIF cif;
  
  private long objectId;
  
  private long methodId;
  
  private long handle;
  
  private long callback;
  
  public static Closure build(Class clazz, Object callback, boolean wideChar) {
    if (callback == null)
      return null; 
    Method[] methods = clazz.getMethods();
    if (methods.length > 1) {
      for (Method m : methods) {
        PInvoke.Delegate d = m.<PInvoke.Delegate>getAnnotation(PInvoke.Delegate.class);
        if (d != null)
          return build(callback, m, wideChar); 
      } 
    } else {
      return build(callback, methods[0], wideChar);
    } 
    return null;
  }
  
  public static Closure build(Object obj, Method m, boolean wideChar) {
    Closure c = new Closure();
    c.callbackObj = obj;
    c.callbackMethod = m;
    c.wideChar = wideChar;
    c.params = m.getParameterTypes();
    Class<?> returnType = m.getReturnType();
    c.argTypes = new int[c.params.length];
    for (int i = 0; i < c.params.length; i++)
      c.argTypes[i] = NativeBinder.getArgType(c.params[i], m.getName()); 
    c.returnType = NativeBinder.getArgType(returnType, m.getName());
    c.cif = FFI.CIF.prepare(is64 ? 1 : 2, c.params.length);
    c.objectId = Native.newGlobalRef(c);
    c.methodId = invokeId;
    c.handle = FFI.prepareClosure(c.cif.get(), c.objectId, c.methodId);
    c.callback = NativeHelper.getPointer(c.handle);
    return c;
  }
  
  public long getPointer() {
    return this.callback;
  }
  
  public void invoke(long resp, long args) throws Throwable {
    Object[] jargs = new Object[this.argTypes.length];
    if (jargs.length > 0) {
      ByteBuffer argp = NativeHelper.getBuffer(args, NativeHelper.PTR_SIZE * this.argTypes.length);
      for (int i = 0; i < jargs.length; i++) {
        Object ptr;
        long pValue = is64 ? argp.getLong() : argp.getInt();
        long aValue = 0L;
        if (pValue != 0L)
          aValue = NativeHelper.getPointer(pValue); 
        switch (this.argTypes[i]) {
          case 2:
            jargs[i] = Boolean.valueOf((aValue != 0L));
            break;
          case 1:
            jargs[i] = Integer.valueOf((int)aValue);
            break;
          case 9:
            jargs[i] = Long.valueOf(aValue);
            break;
          case 4:
            ptr = null;
            if (aValue != 0L)
              ptr = new PInvoke.UIntPtr(NativeHelper.getInt(aValue)); 
            jargs[i] = ptr;
            break;
          case 5:
            ptr = null;
            if (aValue != 0L)
              ptr = new PInvoke.IntPtr(NativeHelper.getInt(aValue)); 
            jargs[i] = ptr;
            break;
          case 8:
            if (aValue != 0L) {
              Object so = this.params[i].newInstance();
              PInvoke.NativeStruct ns = NativeBinder.getStruct(this.params[i], this.wideChar);
              ns.fromNative(aValue, so);
              jargs[i] = so;
            } 
            break;
          case 6:
            if (aValue != 0L)
              jargs[i] = NativeHelper.getString(aValue, 4096L, this.wideChar); 
            break;
        } 
      } 
    } 
    Object res = null;
    try {
      res = this.callbackMethod.invoke(this.callbackObj, jargs);
    } catch (InvocationTargetException e) {
      throw e.getTargetException();
    } 
    long resv = 0L;
    if (res != null)
      switch (this.returnType) {
        case 2:
          resv = ((Boolean)res).booleanValue() ? 1L : 0L;
          break;
        case 1:
          resv = ((Integer)res).intValue();
          break;
        case 9:
          resv = ((Long)res).longValue();
          break;
      }  
    NativeHelper.setPointer(resp, resv);
    if (jargs.length > 0) {
      ByteBuffer argp = NativeHelper.getBuffer(args, NativeHelper.PTR_SIZE * this.argTypes.length);
      for (int i = 0; i < jargs.length; i++) {
        long pValue = is64 ? argp.getLong() : argp.getInt();
        long aValue = 0L;
        if (pValue != 0L)
          aValue = NativeHelper.getPointer(pValue); 
        Object o = jargs[i];
        switch (this.argTypes[i]) {
          case 4:
          case 5:
            if (aValue != 0L && o != null) {
              int value = (int)((PInvoke.IntPtr)o).value;
              if (aValue != 0L)
                NativeHelper.setInt(aValue, value); 
            } 
            break;
          case 8:
            if (aValue != 0L && o != null) {
              PInvoke.NativeStruct ns = NativeBinder.getStruct(this.params[i], this.wideChar);
              ns.toNative(aValue, o);
            } 
            break;
        } 
      } 
    } 
  }
  
  public synchronized void destroy() {
    if (this.cif != null) {
      this.cif.destroy();
      this.cif = null;
    } 
    if (this.objectId != 0L) {
      Native.deleteGlobalRef(this.objectId);
      this.objectId = 0L;
    } 
  }
}
