package com.hierynomus.msdtyp;

import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.protocol.commons.buffer.Endian;
import java.util.UUID;

public class MsDataTypes {
  public static void putGuid(UUID guid, Buffer<?> buffer) {
    long leastSignificantBits = guid.getLeastSignificantBits();
    long mostSignificantBits = guid.getMostSignificantBits();
    buffer.putUInt32(mostSignificantBits >>> 32L);
    buffer.putUInt16((int)(mostSignificantBits >>> 16L & 0xFFFFL));
    buffer.putUInt16((int)(mostSignificantBits & 0xFFFFL));
    buffer.putLong(leastSignificantBits, Endian.BE);
  }
  
  public static UUID readGuid(Buffer<?> buffer) throws Buffer.BufferException {
    long mostSigBits = buffer.readUInt32();
    mostSigBits <<= 16L;
    mostSigBits |= buffer.readUInt16();
    mostSigBits <<= 16L;
    mostSigBits |= buffer.readUInt16();
    long leastSigBits = buffer.readLong(Endian.BE);
    return new UUID(mostSigBits, leastSigBits);
  }
  
  public static FileTime readFileTime(Buffer<?> buffer) throws Buffer.BufferException {
    long lowOrder = buffer.readUInt32();
    long highOrder = buffer.readUInt32();
    long windowsTimeStamp = highOrder << 32L | lowOrder;
    return new FileTime(windowsTimeStamp);
  }
  
  public static void putFileTime(FileTime fileTime, Buffer<?> buffer) {
    long timestamp = fileTime.getWindowsTimeStamp();
    buffer.putUInt32(timestamp & 0xFFFFFFFFL);
    buffer.putUInt32(timestamp >> 32L & 0xFFFFFFFFL);
  }
  
  public static long nowAsFileTime() {
    return FileTime.now().getWindowsTimeStamp();
  }
}
