package jline;

public class NoInterruptUnixTerminal extends UnixTerminal {
  private String intr;
  
  public void init() throws Exception {
    super.init();
    this.intr = getSettings().getPropertyAsString("intr");
    if ("<undef>".equals(this.intr))
      this.intr = null; 
    if (this.intr != null)
      getSettings().undef("intr"); 
  }
  
  public void restore() throws Exception {
    if (this.intr != null)
      getSettings().set(new String[] { "intr", this.intr }); 
    super.restore();
  }
}
