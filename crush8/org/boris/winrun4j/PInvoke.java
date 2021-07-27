package org.boris.winrun4j;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PInvoke {
  private static boolean is64 = Native.IS_64;
  
  public static void bind(Class clazz) {
    NativeBinder.bind(clazz);
  }
  
  public static void bind(Class clazz, String library) {
    NativeBinder.bind(clazz, library);
  }
  
  public static int sizeOf(Class struct, boolean wideChar) {
    NativeStruct ns = NativeStruct.fromClass(struct, wideChar);
    return (ns == null) ? 0 : ns.sizeOf();
  }
  
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface DllImport {
    String value() default "";
    
    String lib() default "";
    
    String entryPoint() default "";
    
    boolean wideChar() default true;
    
    boolean setLastError() default false;
    
    boolean internal() default false;
  }
  
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface Out {}
  
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface Delegate {}
  
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface MarshalAs {
    int sizeConst() default 0;
    
    boolean isPointer() default false;
  }
  
  public static class IntPtr {
    public long value;
    
    public IntPtr() {}
    
    public IntPtr(long value) {
      this.value = value;
    }
    
    public int intValue() {
      return (int)this.value;
    }
    
    public String toString() {
      return Long.toString(this.value);
    }
  }
  
  public static class UIntPtr extends IntPtr {
    public UIntPtr() {}
    
    public UIntPtr(long value) {
      this.value = value;
    }
  }
  
  public static class ByteArrayBuilder {
    private byte[] array;
    
    public void set(byte[] array) {
      this.array = array;
    }
    
    public byte[] toArray() {
      return this.array;
    }
  }
  
  public static interface Callback {}
  
  public static interface Struct {}
  
  public static interface Union {}
  
  public static class NativeStruct {
    private boolean wideChar;
    
    private Field[] fields;
    
    private int[] fieldTypes;
    
    private int[] fieldSizes;
    
    private Map<Field, NativeStruct> childStructs = new HashMap<Field, NativeStruct>();
    
    private int size;
    
    public NativeStruct(boolean wideChar) {
      this.wideChar = wideChar;
    }
    
    public static NativeStruct fromClass(Class<?> struct, boolean wideChar) {
      if (struct == null)
        return null; 
      if (!PInvoke.Struct.class.isAssignableFrom(struct))
        throw new RuntimeException("Invalid class used as struct: " + struct.getSimpleName()); 
      NativeStruct ns = new NativeStruct(wideChar);
      ns.parse(struct);
      return ns;
    }
    
    public void parse(Class struct) {
      Field[] fields = struct.getFields();
      List<Field> fieldList = new ArrayList<Field>();
      List<Integer> fieldTypes = new ArrayList<Integer>();
      List<Integer> fieldSizes = new ArrayList<Integer>();
      int size = 0;
      int i;
      for (i = 0; i < fields.length; i++) {
        Field f = fields[i];
        if (!Modifier.isStatic(f.getModifiers()) && Modifier.isPublic(f.getModifiers())) {
          int ft = NativeBinder.getArgType(f.getType(), struct.getSimpleName());
          if (ft == 6) {
            PInvoke.MarshalAs ma = f.<PInvoke.MarshalAs>getAnnotation(PInvoke.MarshalAs.class);
            if (ma == null)
              ft = 15; 
          } 
          fieldList.add(f);
          fieldTypes.add(Integer.valueOf(ft));
          if (ft == 8)
            this.childStructs.put(f, fromClass(f.getType(), this.wideChar)); 
          int sz = sizeOf(ft, f);
          fieldSizes.add(Integer.valueOf(sz));
          size += sz;
        } 
      } 
      this.fields = fieldList.<Field>toArray(new Field[0]);
      this.fieldTypes = new int[fields.length];
      this.fieldSizes = new int[fields.length];
      for (i = 0; i < this.fields.length; i++) {
        this.fieldTypes[i] = ((Integer)fieldTypes.get(i)).intValue();
        this.fieldSizes[i] = ((Integer)fieldSizes.get(i)).intValue();
      } 
      this.size = size;
    }
    
    private int sizeOf(int fieldType, Field field) {
      PInvoke.MarshalAs ma;
      int nativeSize = Native.IS_64 ? 8 : 4;
      int size = 0;
      switch (fieldType) {
        case 2:
        case 13:
          size++;
          return size;
        case 11:
          size += 2;
          return size;
        case 1:
          size += 4;
          return size;
        case 4:
        case 5:
        case 9:
        case 14:
        case 15:
          size += nativeSize;
          return size;
        case 6:
          ma = field.<PInvoke.MarshalAs>getAnnotation(PInvoke.MarshalAs.class);
          if (ma == null)
            throw new RuntimeException("Invalid string arg type: " + field.getName()); 
          size += ma.sizeConst();
          if (this.wideChar)
            size <<= 1; 
          return size;
        case 3:
          throw new RuntimeException("StringBuilder not supported in structs - " + field.getName());
        case 8:
          size += ((NativeStruct)this.childStructs.get(field)).sizeOf();
          return size;
      } 
      throw new RuntimeException("Unsupported struct type: " + field.getName());
    }
    
    public int sizeOf() {
      return this.size;
    }
    
    public long toNative(Object obj) throws IllegalArgumentException, IllegalAccessException {
      long ptr = Native.malloc(this.size);
      toNative(ptr, obj);
      return ptr;
    }
    
    public void toNative(long ptr, Object obj) throws IllegalArgumentException, IllegalAccessException {
      ByteBuffer bb = NativeHelper.getBuffer(ptr, this.size);
      for (int i = 0; i < this.fieldTypes.length; i++)
        toNative(obj, this.fieldTypes[i], this.fieldSizes[i], this.fields[i], bb); 
    }
    
    private void toNative(Object obj, int fieldType, int fieldSize, Field field, ByteBuffer bb) throws IllegalArgumentException, IllegalAccessException {
      boolean b;
      Closure cl;
      long l;
      String s;
      int bytesWritten;
      int i;
      switch (fieldType) {
        case 2:
          b = field.getBoolean(obj);
          bb.put(b ? 1 : 0);
          break;
        case 13:
          bb.put(field.getByte(obj));
          break;
        case 1:
          bb.putInt(field.getInt(obj));
          break;
        case 11:
          bb.putShort(field.getShort(obj));
          break;
        case 14:
          cl = (Closure)field.get(obj);
          if (PInvoke.is64) {
            bb.putLong(cl.getPointer());
            break;
          } 
          bb.putInt((int)(cl.getPointer() & 0xFFFFFFL));
          break;
        case 9:
          l = field.getLong(obj);
          if (PInvoke.is64) {
            bb.putLong(l);
            break;
          } 
          bb.putInt((int)(l & 0xFFFFFFL));
          break;
        case 6:
          s = (String)field.get(obj);
          bytesWritten = 0;
          if (s != null)
            if (this.wideChar) {
              char[] c = s.toCharArray();
              for (int j = 0; j < c.length && 
                bytesWritten < fieldSize; j++) {
                bb.putChar(c[j]);
                bytesWritten += 2;
              } 
            } else {
              byte[] bs = s.getBytes();
              for (int j = 0; j < bs.length && 
                bytesWritten < fieldSize; j++) {
                bb.put(bs[j]);
                bytesWritten++;
              } 
            }  
          for (i = bytesWritten; i < fieldSize; i++)
            bb.put((byte)0); 
          break;
      } 
    }
    
    public void fromNative(long ptr, Object obj) throws IllegalArgumentException, IllegalAccessException {
      ByteBuffer bb = NativeHelper.getBuffer(ptr, this.size);
      fromNative(bb, obj);
    }
    
    private void fromNative(ByteBuffer bb, Object obj) throws IllegalArgumentException, IllegalAccessException {
      for (int i = 0; i < this.fieldTypes.length; i++)
        fromNative(bb, this.fieldTypes[i], this.fieldSizes[i], this.fields[i], obj); 
    }
    
    private void fromNative(ByteBuffer bb, int fieldType, int fieldSize, Field field, Object obj) throws IllegalArgumentException, IllegalAccessException {
      byte[] b;
      switch (fieldType) {
        case 2:
          field.set(obj, Boolean.valueOf((bb.get() != 0)));
          break;
        case 13:
          field.set(obj, Byte.valueOf(bb.get()));
          break;
        case 1:
          field.set(obj, Integer.valueOf(bb.getInt()));
          break;
        case 9:
          field.set(obj, Long.valueOf(PInvoke.is64 ? bb.getLong() : bb.getInt()));
          break;
        case 11:
          field.set(obj, Short.valueOf(bb.getShort()));
          break;
        case 6:
          b = new byte[fieldSize];
          bb.get(b);
          field.set(obj, NativeHelper.getString(b, this.wideChar));
          break;
      } 
    }
  }
}
