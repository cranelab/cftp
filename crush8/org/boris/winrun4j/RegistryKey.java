package org.boris.winrun4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class RegistryKey {
  public static final RegistryKey HKEY_CLASSES_ROOT = new RegistryKey(-2147483648L);
  
  public static final RegistryKey HKEY_CURRENT_USER = new RegistryKey(-2147483647L);
  
  public static final RegistryKey HKEY_LOCAL_MACHINE = new RegistryKey(-2147483646L);
  
  public static final RegistryKey HKEY_USERS = new RegistryKey(-2147483645L);
  
  public static final RegistryKey HKEY_CURRENT_CONFIG = new RegistryKey(-2147483643L);
  
  private static Map rootNames = new HashMap<Object, Object>();
  
  public static final int TYPE_NONE = 1;
  
  public static final int TYPE_SZ = 2;
  
  public static final int TYPE_EXPAND_SZ = 5;
  
  public static final int TYPE_BINARY = 1;
  
  public static final int TYPE_DWORD = 2;
  
  public static final int TYPE_DWORD_LITTLE_ENDIAN = 3;
  
  public static final int TYPE_DWORD_BIG_ENDIAN = 4;
  
  public static final int TYPE_LINK = 6;
  
  public static final int TYPE_MULTI_SZ = 7;
  
  public static final int TYPE_QWORD = 9;
  
  public static final int TYPE_QWORD_LITTLE_ENDIAN = 10;
  
  private RegistryKey parent;
  
  private boolean isRoot;
  
  private long handle;
  
  private String[] path;
  
  static {
    rootNames.put("HKEY_CLASSES_ROOT", HKEY_CLASSES_ROOT);
    rootNames.put("HKEY_CURRENT_USER", HKEY_CURRENT_USER);
    rootNames.put("HKEY_LOCAL_MACHINE", HKEY_LOCAL_MACHINE);
    rootNames.put("HKEY_USERS", HKEY_USERS);
    rootNames.put("HKEY_CURRENT_CONFIG", HKEY_CURRENT_CONFIG);
  }
  
  public static RegistryKey getRootKey(String name) {
    return (RegistryKey)rootNames.get(name);
  }
  
  private RegistryKey(long key) {
    this.isRoot = true;
    this.handle = key;
  }
  
  public RegistryKey(RegistryKey parent, String path) {
    this.parent = parent;
    this.handle = parent.handle;
    if (parent.isRoot) {
      this.path = new String[] { path };
    } else {
      this.path = new String[parent.path.length + 1];
      System.arraycopy(parent.path, 0, this.path, 0, parent.path.length);
      this.path[parent.path.length] = path;
    } 
  }
  
  public RegistryKey(RegistryKey parent, String[] path) {
    this.parent = parent;
    this.handle = parent.handle;
    if (parent.isRoot) {
      this.path = path;
    } else {
      this.path = new String[parent.path.length + path.length];
      System.arraycopy(parent.path, 0, this.path, 0, parent.path.length);
      System.arraycopy(path, 0, this.path, parent.path.length, path.length);
    } 
  }
  
  public boolean exists() {
    if (this.isRoot)
      return true; 
    long h = openKeyHandle(this.handle, this.path, true);
    Registry.closeKey(h);
    return (h != 0L);
  }
  
  public String get(String path) {
    if (path == null)
      return getString(null); 
    boolean defaultValue = path.endsWith("/");
    String[] p = path.split("/");
    if (p.length == 0)
      return getString(path); 
    RegistryKey k = this;
    int len = defaultValue ? p.length : (p.length - 1);
    for (int i = 0; i < len; i++)
      k = k.getSubKey(p[i]); 
    if (k.exists())
      return defaultValue ? k.getString(null) : k.getString(p[p.length - 1]); 
    return null;
  }
  
  private long openKeyHandle(long handle, String[] path, boolean readOnly) {
    long h = handle;
    if (path == null)
      return h; 
    for (int i = 0; i < path.length; i++) {
      long nh = openKeyHandle(h, path[i], readOnly);
      if (h != handle)
        Registry.closeKey(h); 
      h = nh;
    } 
    return h;
  }
  
  public String[] getSubKeyNames() {
    long h = openKeyHandle(this.handle, this.path, true);
    Registry.QUERY_INFO qi = Registry.queryInfoKey(h);
    if (qi == null)
      return null; 
    String[] keys = new String[qi.subKeyCount];
    for (int i = 0; i < keys.length; i++) {
      StringBuilder name = new StringBuilder();
      PInvoke.UIntPtr cbName = new PInvoke.UIntPtr((qi.maxSubkeyLen + 1));
      if (Registry.enumKeyEx(h, i, name, cbName, 0L, 0L, 0L, null) == 0)
        keys[i] = name.toString(); 
    } 
    Registry.closeKey(h);
    return keys;
  }
  
  public String[] getValueNames() {
    long h = openKeyHandle(this.handle, this.path, true);
    Registry.QUERY_INFO qi = Registry.queryInfoKey(h);
    if (qi == null)
      return null; 
    String[] res = new String[qi.valueCount];
    for (int i = 0; i < res.length; i++) {
      StringBuilder sb = new StringBuilder();
      PInvoke.UIntPtr valueLen = new PInvoke.UIntPtr((qi.maxValueNameLen + 1));
      if (Registry.enumValue(h, i, sb, valueLen, 0L, null, null, null) == 0)
        res[i] = sb.toString(); 
    } 
    Registry.closeKey(h);
    return res;
  }
  
  public RegistryKey getSubKey(String name) {
    return new RegistryKey(this, name);
  }
  
  public RegistryKey createSubKey(String name) {
    long h = openKeyHandle(this.handle, this.path, false);
    if (h != 0L) {
      PInvoke.UIntPtr phkResult = new PInvoke.UIntPtr();
      int res = Registry.createKey(h, name, phkResult);
      Registry.closeKey(h);
      if (res != 0)
        return null; 
      if (phkResult.value != 0L) {
        Registry.closeKey(phkResult.value);
        return new RegistryKey(this, name);
      } 
    } 
    return null;
  }
  
  public String[] getPath() {
    return this.path;
  }
  
  public RegistryKey getParent() {
    return this.parent;
  }
  
  public void deleteSubKey(String subKey) {
    if (!this.isRoot) {
      long h = openKeyHandle(this.handle, this.path, false);
      Registry.deleteKey(h, subKey);
      Registry.closeKey(h);
    } 
  }
  
  public long getType(String name) {
    long h = openKeyHandle(this.handle, this.path, true);
    PInvoke.UIntPtr type = new PInvoke.UIntPtr();
    int res = Registry.queryValueEx(h, name, 0L, type, (StringBuilder)null, (PInvoke.UIntPtr)null);
    Registry.closeKey(h);
    if (res == 0)
      return type.value; 
    return -1L;
  }
  
  public String getString(String name) {
    long h = openKeyHandle(this.handle, this.path, true);
    byte[] data = Registry.queryValueEx(h, name, 512);
    Registry.closeKey(h);
    if (data != null)
      return NativeHelper.getString(data, true); 
    return null;
  }
  
  public byte[] getBinary(String name) {
    long h = openKeyHandle(this.handle, this.path, true);
    byte[] res = Registry.queryValueEx(h, name, 4096);
    Registry.closeKey(h);
    return res;
  }
  
  public int getDoubleWord(String name, int defaultValue) {
    long h = openKeyHandle(this.handle, this.path, true);
    byte[] b = Registry.queryValueEx(h, name, 4);
    int res = defaultValue;
    if (b != null && b.length == 4)
      res = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getInt(); 
    Registry.closeKey(h);
    return res;
  }
  
  public String[] getMultiString(String name) {
    long h = openKeyHandle(this.handle, this.path, true);
    byte[] b = Registry.queryValueEx(h, name, 4096);
    String[] res = null;
    if (b != null)
      res = NativeHelper.getMultiString(ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN), true); 
    Registry.closeKey(h);
    return res;
  }
  
  public void setString(String name, String value) {
    long h = openKeyHandle(this.handle, this.path, false);
    byte[] b = NativeHelper.toBytes(value, true);
    Registry.setValueEx(h, name, 0, 1, b, b.length);
    Registry.closeKey(h);
  }
  
  public void setExpandedString(String name, String value) {
    long h = openKeyHandle(this.handle, this.path, false);
    byte[] b = NativeHelper.toBytes(value, true);
    Registry.setValueEx(h, name, 0, 2, b, b.length);
    Registry.closeKey(h);
  }
  
  public void setBinary(String name, byte[] value) {
    long h = openKeyHandle(this.handle, this.path, false);
    Registry.setValueEx(h, name, 0, 3, value, value.length);
    Registry.closeKey(h);
  }
  
  public void setDoubleWord(String name, int value) {
    long h = openKeyHandle(this.handle, this.path, false);
    byte[] b = new byte[4];
    ByteBuffer bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
    bb.putInt(value);
    Registry.setValueEx(h, name, 0, 4, b, b.length);
    Registry.closeKey(h);
  }
  
  public void setMultiString(String name, String[] value) {
    long h = openKeyHandle(this.handle, this.path, false);
    byte[] b = null;
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      for (int i = 0; i < value.length; i++)
        bos.write(NativeHelper.toBytes(value[i], true)); 
      bos.write(new byte[] { 0, 0 });
      b = bos.toByteArray();
    } catch (IOException e) {}
    Registry.setValueEx(h, name, 0, 7, b, b.length);
    Registry.closeKey(h);
  }
  
  public void deleteValue(String name) {
    long h = openKeyHandle(this.handle, this.path, false);
    Registry.deleteKey(h, name);
    Registry.closeKey(h);
  }
  
  private long openKeyHandle(long rootKey, String keyPath, boolean readOnly) {
    PInvoke.UIntPtr key = new PInvoke.UIntPtr();
    Registry.openKeyEx(rootKey, keyPath, 0, readOnly ? 131097L : 983103L, key);
    return key.value;
  }
}
