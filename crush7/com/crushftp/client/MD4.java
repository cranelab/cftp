package com.crushftp.client;

public class MD4 {
  private static final int BLOCK_SIZE = 64;
  
  private static final int HASH_SIZE = 16;
  
  private final byte[] buffer = new byte[64];
  
  private int bufferOffset;
  
  private long byteCount;
  
  private int[] state = new int[4];
  
  private int[] tmp = new int[16];
  
  public MD4() {
    reset();
  }
  
  public void reset() {
    this.bufferOffset = 0;
    this.byteCount = 0L;
    this.state[0] = 1732584193;
    this.state[1] = -271733879;
    this.state[2] = -1732584194;
    this.state[3] = 271733878;
  }
  
  public byte[] digest() {
    byte[] resBuf = new byte[16];
    digest(resBuf, 0, 16);
    return resBuf;
  }
  
  private void digest(byte[] buffer, int off) {
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++)
        buffer[off + i * 4 + j] = (byte)(this.state[i] >>> 8 * j); 
    } 
  }
  
  private void digest(byte[] buffer, int offset, int len) {
    this.buffer[this.bufferOffset++] = Byte.MIN_VALUE;
    int lenOfBitLen = 8;
    int C = 64 - lenOfBitLen;
    if (this.bufferOffset > C) {
      while (this.bufferOffset < 64)
        this.buffer[this.bufferOffset++] = 0; 
      update(this.buffer, 0);
      this.bufferOffset = 0;
    } 
    while (this.bufferOffset < C)
      this.buffer[this.bufferOffset++] = 0; 
    long bitCount = this.byteCount * 8L;
    for (int i = 0; i < 64; i += 8)
      this.buffer[this.bufferOffset++] = (byte)(int)(bitCount >>> i); 
    update(this.buffer, 0);
    digest(buffer, offset);
  }
  
  public void update(byte[] input, int offset, int length) {
    this.byteCount += length;
    int todo;
    while (length >= (todo = 64 - this.bufferOffset)) {
      System.arraycopy(input, offset, this.buffer, this.bufferOffset, todo);
      update(this.buffer, 0);
      length -= todo;
      offset += todo;
      this.bufferOffset = 0;
    } 
    System.arraycopy(input, offset, this.buffer, this.bufferOffset, length);
    this.bufferOffset += length;
  }
  
  private void update(byte[] block, int offset) {
    for (int i = 0; i < 16; i++)
      this.tmp[i] = block[offset++] & 0xFF | (block[offset++] & 0xFF) << 8 | (block[offset++] & 0xFF) << 16 | (block[offset++] & 0xFF) << 24; 
    int A = this.state[0];
    int B = this.state[1];
    int C = this.state[2];
    int D = this.state[3];
    A = FF(A, B, C, D, this.tmp[0], 3);
    D = FF(D, A, B, C, this.tmp[1], 7);
    C = FF(C, D, A, B, this.tmp[2], 11);
    B = FF(B, C, D, A, this.tmp[3], 19);
    A = FF(A, B, C, D, this.tmp[4], 3);
    D = FF(D, A, B, C, this.tmp[5], 7);
    C = FF(C, D, A, B, this.tmp[6], 11);
    B = FF(B, C, D, A, this.tmp[7], 19);
    A = FF(A, B, C, D, this.tmp[8], 3);
    D = FF(D, A, B, C, this.tmp[9], 7);
    C = FF(C, D, A, B, this.tmp[10], 11);
    B = FF(B, C, D, A, this.tmp[11], 19);
    A = FF(A, B, C, D, this.tmp[12], 3);
    D = FF(D, A, B, C, this.tmp[13], 7);
    C = FF(C, D, A, B, this.tmp[14], 11);
    B = FF(B, C, D, A, this.tmp[15], 19);
    A = GG(A, B, C, D, this.tmp[0], 3);
    D = GG(D, A, B, C, this.tmp[4], 5);
    C = GG(C, D, A, B, this.tmp[8], 9);
    B = GG(B, C, D, A, this.tmp[12], 13);
    A = GG(A, B, C, D, this.tmp[1], 3);
    D = GG(D, A, B, C, this.tmp[5], 5);
    C = GG(C, D, A, B, this.tmp[9], 9);
    B = GG(B, C, D, A, this.tmp[13], 13);
    A = GG(A, B, C, D, this.tmp[2], 3);
    D = GG(D, A, B, C, this.tmp[6], 5);
    C = GG(C, D, A, B, this.tmp[10], 9);
    B = GG(B, C, D, A, this.tmp[14], 13);
    A = GG(A, B, C, D, this.tmp[3], 3);
    D = GG(D, A, B, C, this.tmp[7], 5);
    C = GG(C, D, A, B, this.tmp[11], 9);
    B = GG(B, C, D, A, this.tmp[15], 13);
    A = HH(A, B, C, D, this.tmp[0], 3);
    D = HH(D, A, B, C, this.tmp[8], 9);
    C = HH(C, D, A, B, this.tmp[4], 11);
    B = HH(B, C, D, A, this.tmp[12], 15);
    A = HH(A, B, C, D, this.tmp[2], 3);
    D = HH(D, A, B, C, this.tmp[10], 9);
    C = HH(C, D, A, B, this.tmp[6], 11);
    B = HH(B, C, D, A, this.tmp[14], 15);
    A = HH(A, B, C, D, this.tmp[1], 3);
    D = HH(D, A, B, C, this.tmp[9], 9);
    C = HH(C, D, A, B, this.tmp[5], 11);
    B = HH(B, C, D, A, this.tmp[13], 15);
    A = HH(A, B, C, D, this.tmp[3], 3);
    D = HH(D, A, B, C, this.tmp[11], 9);
    C = HH(C, D, A, B, this.tmp[7], 11);
    B = HH(B, C, D, A, this.tmp[15], 15);
    this.state[0] = this.state[0] + A;
    this.state[1] = this.state[1] + B;
    this.state[2] = this.state[2] + C;
    this.state[3] = this.state[3] + D;
  }
  
  private int FF(int a, int b, int c, int d, int x, int s) {
    int t = a + (b & c | (b ^ 0xFFFFFFFF) & d) + x;
    return t << s | t >>> 32 - s;
  }
  
  private int GG(int a, int b, int c, int d, int x, int s) {
    int t = a + (b & (c | d) | c & d) + x + 1518500249;
    return t << s | t >>> 32 - s;
  }
  
  private int HH(int a, int b, int c, int d, int x, int s) {
    int t = a + (b ^ c ^ d) + x + 1859775393;
    return t << s | t >>> 32 - s;
  }
}
