package com.hierynomus.mssmb2;

import com.hierynomus.msdfsc.messages.StandardCharsets;
import com.hierynomus.utils.Strings;
import java.util.List;

public class SMB2Functions {
  private static final byte[] EMPTY_BYTES = new byte[0];
  
  public static byte[] unicode(String s) {
    if (s == null)
      return EMPTY_BYTES; 
    return s.getBytes(StandardCharsets.UTF_16LE);
  }
  
  public static String resolveSymlinkTarget(String originalFileName, SMB2Error.SymbolicLinkError symlinkData) {
    String target;
    int unparsedPathLength = symlinkData.getUnparsedPathLength();
    String unparsedPath = getSymlinkUnparsedPath(originalFileName, unparsedPathLength);
    String substituteName = symlinkData.getSubstituteName();
    if (symlinkData.isAbsolute()) {
      target = String.valueOf(substituteName) + unparsedPath;
    } else {
      String parsedPath = getSymlinkParsedPath(originalFileName, unparsedPathLength);
      StringBuilder b = new StringBuilder();
      int startIndex = parsedPath.lastIndexOf("\\");
      if (startIndex != -1) {
        b.append(parsedPath, 0, startIndex);
        b.append('\\');
      } 
      b.append(substituteName);
      b.append(unparsedPath);
      target = b.toString();
    } 
    return normalizePath(target);
  }
  
  private static String getSymlinkParsedPath(String fileName, int unparsedPathLength) {
    byte[] fileNameBytes = unicode(fileName);
    return new String(fileNameBytes, 0, fileNameBytes.length - unparsedPathLength, StandardCharsets.UTF_16LE);
  }
  
  private static String getSymlinkUnparsedPath(String fileName, int unparsedPathLength) {
    byte[] fileNameBytes = unicode(fileName);
    return new String(fileNameBytes, fileNameBytes.length - unparsedPathLength, unparsedPathLength, StandardCharsets.UTF_16LE);
  }
  
  private static String normalizePath(String path) {
    List<String> parts = Strings.split(path, '\\');
    for (int i = 0; i < parts.size(); ) {
      String s = parts.get(i);
      if (".".equals(s)) {
        parts.remove(i);
        continue;
      } 
      if ("..".equals(s)) {
        if (i > 0)
          parts.remove(i--); 
        parts.remove(i);
        continue;
      } 
      i++;
    } 
    return Strings.join(parts, '\\');
  }
}
