package com.hierynomus.protocol.commons;

public class ByteArrayUtils {
  static final char[] digits = new char[] { 
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
      'a', 'b', 'c', 'd', 'e', 'f' };
  
  public static boolean equals(byte[] a1, int a1Offset, byte[] a2, int a2Offset, int length) {
    if (a1.length < a1Offset + length || a2.length < a2Offset + length)
      return false; 
    for (int l = 0; l < length; l++) {
      if (a1[a1Offset + l] != a2[a2Offset + l])
        return false; 
    } 
    return true;
  }
  
  public static String printHex(byte[] array) {
    return printHex(array, 0, array.length);
  }
  
  public static String printHex(byte[] array, int offset, int len) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < len; i++) {
      byte b = array[offset + i];
      if (sb.length() > 0)
        sb.append(' '); 
      sb.append(digits[b >> 4 & 0xF]);
      sb.append(digits[b & 0xF]);
    } 
    return sb.toString();
  }
  
  public static String toHex(byte[] array) {
    return toHex(array, 0, array.length);
  }
  
  public static String toHex(byte[] array, int offset, int len) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < len; i++) {
      byte b = array[offset + i];
      sb.append(digits[b >> 4 & 0xF]);
      sb.append(digits[b & 0xF]);
    } 
    return sb.toString();
  }
  
  public static byte[] parseHex(String hex) {
    if (hex == null)
      throw new IllegalArgumentException("Hex string is null"); 
    if (hex.length() % 2 != 0)
      throw new IllegalArgumentException("Hex string '" + hex + "' should have even length."); 
    byte[] result = new byte[hex.length() / 2];
    for (int i = 0; i < result.length; i++) {
      int hi = parseHexDigit(hex.charAt(i * 2)) << 4;
      int lo = parseHexDigit(hex.charAt(i * 2 + 1));
      result[i] = (byte)(hi + lo);
    } 
    return result;
  }
  
  private static int parseHexDigit(char c) {
    if (c >= '0' && c <= '9')
      return c - 48; 
    if (c >= 'a' && c <= 'f')
      return c - 97 + 10; 
    if (c >= 'A' && c <= 'F')
      return c - 65 + 10; 
    throw new IllegalArgumentException("Digit '" + c + "' out of bounds [0-9a-fA-F]");
  }
}
