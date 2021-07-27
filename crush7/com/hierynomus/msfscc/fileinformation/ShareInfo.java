package com.hierynomus.msfscc.fileinformation;

import com.hierynomus.protocol.commons.buffer.Buffer;

public class ShareInfo {
  private final long totalAllocationUnits;
  
  private final long callerAvailableAllocationUnits;
  
  private final long actualAvailableAllocationUnits;
  
  private final long sectorsPerAllocationUnit;
  
  private final long bytesPerSector;
  
  private final long totalSpace;
  
  private final long callerFreeSpace;
  
  private final long actualFreeSpace;
  
  ShareInfo(long totalAllocationUnits, long callerAvailableAllocationUnits, long actualAvailableAllocationUnits, long sectorsPerAllocationUnit, long bytesPerSector) {
    this.totalAllocationUnits = totalAllocationUnits;
    this.callerAvailableAllocationUnits = callerAvailableAllocationUnits;
    this.actualAvailableAllocationUnits = actualAvailableAllocationUnits;
    this.sectorsPerAllocationUnit = sectorsPerAllocationUnit;
    this.bytesPerSector = bytesPerSector;
    long bytesPerAllocationUnit = sectorsPerAllocationUnit * bytesPerSector;
    this.totalSpace = totalAllocationUnits * bytesPerAllocationUnit;
    this.callerFreeSpace = callerAvailableAllocationUnits * bytesPerAllocationUnit;
    this.actualFreeSpace = actualAvailableAllocationUnits * bytesPerAllocationUnit;
  }
  
  public long getFreeSpace() {
    return this.actualFreeSpace;
  }
  
  public long getCallerFreeSpace() {
    return this.callerFreeSpace;
  }
  
  public long getTotalSpace() {
    return this.totalSpace;
  }
  
  public long getTotalAllocationUnits() {
    return this.totalAllocationUnits;
  }
  
  public long getAvailableAllocationUnits() {
    return this.actualAvailableAllocationUnits;
  }
  
  public long getCallerAvailableAllocationUnits() {
    return this.callerAvailableAllocationUnits;
  }
  
  public long getSectorsPerAllocationUnit() {
    return this.sectorsPerAllocationUnit;
  }
  
  public long getBytesPerSector() {
    return this.bytesPerSector;
  }
  
  public static ShareInfo parseFsFullSizeInformation(Buffer.PlainBuffer response) throws Buffer.BufferException {
    long totalAllocationUnits = response.readLong();
    long callerAvailableAllocationUnits = response.readLong();
    long actualAvailableAllocationUnits = response.readLong();
    long sectorsPerAllocationUnit = response.readUInt32();
    long bytesPerSector = response.readUInt32();
    return new ShareInfo(totalAllocationUnits, callerAvailableAllocationUnits, 
        actualAvailableAllocationUnits, sectorsPerAllocationUnit, bytesPerSector);
  }
}
