package com.hierynomus.msfscc;

import com.hierynomus.protocol.commons.EnumWithValue;

public enum FileSystemInformationClass implements EnumWithValue<FileSystemInformationClass> {
  FileFsVolumeInformation(1L),
  FileFsLabelInformation(2L),
  FileFsSizeInformation(3L),
  FileFsDeviceInformation(4L),
  FileFsAttributeInformation(5L),
  FileFsControlInformation(6L),
  FileFsFullSizeInformation(7L),
  FileFsObjectIdInformation(8L),
  FileFsDriverPathInformation(9L),
  FileFsVolumeFlagsInformation(10L),
  FileFsSectorSizeInformation(11L);
  
  private long value;
  
  FileSystemInformationClass(long value) {
    this.value = value;
  }
  
  public long getValue() {
    return this.value;
  }
}
