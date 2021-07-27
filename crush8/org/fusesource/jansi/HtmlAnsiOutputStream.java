package org.fusesource.jansi;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class HtmlAnsiOutputStream extends AnsiOutputStream {
  private boolean concealOn = false;
  
  public void close() throws IOException {
    closeAttributes();
    super.close();
  }
  
  private static final String[] ANSI_COLOR_MAP = new String[] { "black", "red", "green", "yellow", "blue", "magenta", "cyan", "white" };
  
  private static final byte[] BYTES_QUOT = "&quot;".getBytes();
  
  private static final byte[] BYTES_AMP = "&amp;".getBytes();
  
  private static final byte[] BYTES_LT = "&lt;".getBytes();
  
  private static final byte[] BYTES_GT = "&gt;".getBytes();
  
  private List<String> closingAttributes;
  
  public HtmlAnsiOutputStream(OutputStream os) {
    super(os);
    this.closingAttributes = new ArrayList<String>();
  }
  
  private void write(String s) throws IOException {
    this.out.write(s.getBytes());
  }
  
  private void writeAttribute(String s) throws IOException {
    write("<" + s + ">");
    this.closingAttributes.add(0, s.split(" ", 2)[0]);
  }
  
  private void closeAttributes() throws IOException {
    for (String attr : this.closingAttributes)
      write("</" + attr + ">"); 
    this.closingAttributes.clear();
  }
  
  public void write(int data) throws IOException {
    switch (data) {
      case 34:
        this.out.write(BYTES_QUOT);
        return;
      case 38:
        this.out.write(BYTES_AMP);
        return;
      case 60:
        this.out.write(BYTES_LT);
        return;
      case 62:
        this.out.write(BYTES_GT);
        return;
    } 
    super.write(data);
  }
  
  public void writeLine(byte[] buf, int offset, int len) throws IOException {
    write(buf, offset, len);
    closeAttributes();
  }
  
  protected void processSetAttribute(int attribute) throws IOException {
    switch (attribute) {
      case 8:
        write("\033[8m");
        this.concealOn = true;
        break;
      case 1:
        writeAttribute("b");
        break;
      case 22:
        closeAttributes();
        break;
      case 4:
        writeAttribute("u");
        break;
      case 24:
        closeAttributes();
        break;
    } 
  }
  
  protected void processAttributeRest() throws IOException {
    if (this.concealOn) {
      write("\033[0m");
      this.concealOn = false;
    } 
    closeAttributes();
  }
  
  protected void processSetForegroundColor(int color, boolean bright) throws IOException {
    writeAttribute("span style=\"color: " + ANSI_COLOR_MAP[color] + ";\"");
  }
  
  protected void processSetBackgroundColor(int color, boolean bright) throws IOException {
    writeAttribute("span style=\"background-color: " + ANSI_COLOR_MAP[color] + ";\"");
  }
}
