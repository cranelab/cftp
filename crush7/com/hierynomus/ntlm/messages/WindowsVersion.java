package com.hierynomus.ntlm.messages;

import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.protocol.commons.buffer.Buffer;

public class WindowsVersion {
  private ProductMajorVersion majorVersion;
  
  private ProductMinorVersion minorVersion;
  
  private int productBuild;
  
  private NtlmRevisionCurrent ntlmRevision;
  
  enum ProductMajorVersion implements EnumWithValue<ProductMajorVersion> {
    WINDOWS_MAJOR_VERSION_5(5),
    WINDOWS_MAJOR_VERSION_6(6),
    WINDOWS_MAJOR_VERSION_10(10);
    
    private long value;
    
    ProductMajorVersion(int value) {
      this.value = value;
    }
    
    public long getValue() {
      return this.value;
    }
  }
  
  enum ProductMinorVersion implements EnumWithValue<ProductMinorVersion> {
    WINDOWS_MINOR_VERSION_0(0),
    WINDOWS_MINOR_VERSION_1(1),
    WINDOWS_MINOR_VERSION_2(2),
    WINDOWS_MINOR_VERSION_3(3);
    
    private long value;
    
    ProductMinorVersion(int value) {
      this.value = value;
    }
    
    public long getValue() {
      return this.value;
    }
  }
  
  enum NtlmRevisionCurrent implements EnumWithValue<NtlmRevisionCurrent> {
    NTLMSSP_REVISION_W2K3(15);
    
    private long value;
    
    NtlmRevisionCurrent(int value) {
      this.value = value;
    }
    
    public long getValue() {
      return this.value;
    }
  }
  
  WindowsVersion readFrom(Buffer.PlainBuffer buffer) throws Buffer.BufferException {
    this.majorVersion = EnumWithValue.EnumUtils.<ProductMajorVersion>valueOf(buffer.readByte(), ProductMajorVersion.class, null);
    this.minorVersion = EnumWithValue.EnumUtils.<ProductMinorVersion>valueOf(buffer.readByte(), ProductMinorVersion.class, null);
    this.productBuild = buffer.readUInt16();
    buffer.skip(3);
    this.ntlmRevision = EnumWithValue.EnumUtils.<NtlmRevisionCurrent>valueOf(buffer.readByte(), NtlmRevisionCurrent.class, null);
    return this;
  }
  
  public String toString() {
    return String.format("WindowsVersion[%s, %s, %d, %s]", new Object[] { this.majorVersion, this.minorVersion, Integer.valueOf(this.productBuild), this.ntlmRevision });
  }
}
