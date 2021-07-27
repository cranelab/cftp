package jline;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import jline.internal.Configuration;
import jline.internal.InfoCmp;
import jline.internal.Log;
import jline.internal.Preconditions;
import jline.internal.TerminalLineSettings;

public class UnixTerminal extends TerminalSupport implements Terminal2 {
  private final TerminalLineSettings settings;
  
  private final String type;
  
  private String intr;
  
  private String lnext;
  
  private Set<String> bools = new HashSet<String>();
  
  private Map<String, Integer> ints = new HashMap<String, Integer>();
  
  private Map<String, String> strings = new HashMap<String, String>();
  
  public UnixTerminal() throws Exception {
    this("/dev/tty", null);
  }
  
  public UnixTerminal(String ttyDevice) throws Exception {
    this(ttyDevice, null);
  }
  
  public UnixTerminal(String ttyDevice, String type) throws Exception {
    super(true);
    Preconditions.checkNotNull(ttyDevice);
    this.settings = TerminalLineSettings.getSettings(ttyDevice);
    if (type == null)
      type = System.getenv("TERM"); 
    this.type = type;
    parseInfoCmp();
  }
  
  public TerminalLineSettings getSettings() {
    return this.settings;
  }
  
  public void init() throws Exception {
    super.init();
    setAnsiSupported(true);
    if (Configuration.getOsName().contains("freebsd")) {
      this.settings.set("-icanon min 1 -inlcr -ixon");
    } else {
      this.settings.set("-icanon min 1 -icrnl -inlcr -ixon");
    } 
    this.settings.undef("dsusp");
    setEchoEnabled(false);
    parseInfoCmp();
  }
  
  public void restore() throws Exception {
    this.settings.restore();
    super.restore();
  }
  
  public int getWidth() {
    int w = this.settings.getProperty("columns");
    return (w < 1) ? 80 : w;
  }
  
  public int getHeight() {
    int h = this.settings.getProperty("rows");
    return (h < 1) ? 24 : h;
  }
  
  public boolean hasWeirdWrap() {
    return (getBooleanCapability("auto_right_margin") && 
      getBooleanCapability("eat_newline_glitch"));
  }
  
  public synchronized void setEchoEnabled(boolean enabled) {
    try {
      if (enabled) {
        this.settings.set("echo");
      } else {
        this.settings.set("-echo");
      } 
      super.setEchoEnabled(enabled);
    } catch (Exception e) {
      if (e instanceof InterruptedException)
        Thread.currentThread().interrupt(); 
      Log.error(new Object[] { "Failed to ", enabled ? "enable" : "disable", " echo", e });
    } 
  }
  
  public void disableInterruptCharacter() {
    try {
      this.intr = getSettings().getPropertyAsString("intr");
      if ("<undef>".equals(this.intr))
        this.intr = null; 
      this.settings.undef("intr");
    } catch (Exception e) {
      if (e instanceof InterruptedException)
        Thread.currentThread().interrupt(); 
      Log.error(new Object[] { "Failed to disable interrupt character", e });
    } 
  }
  
  public void enableInterruptCharacter() {
    try {
      if (this.intr != null)
        this.settings.set(new String[] { "intr", this.intr }); 
    } catch (Exception e) {
      if (e instanceof InterruptedException)
        Thread.currentThread().interrupt(); 
      Log.error(new Object[] { "Failed to enable interrupt character", e });
    } 
  }
  
  public void disableLitteralNextCharacter() {
    try {
      this.lnext = getSettings().getPropertyAsString("lnext");
      if ("<undef>".equals(this.lnext))
        this.lnext = null; 
      this.settings.undef("lnext");
    } catch (Exception e) {
      if (e instanceof InterruptedException)
        Thread.currentThread().interrupt(); 
      Log.error(new Object[] { "Failed to disable litteral next character", e });
    } 
  }
  
  public void enableLitteralNextCharacter() {
    try {
      if (this.lnext != null)
        this.settings.set(new String[] { "lnext", this.lnext }); 
    } catch (Exception e) {
      if (e instanceof InterruptedException)
        Thread.currentThread().interrupt(); 
      Log.error(new Object[] { "Failed to enable litteral next character", e });
    } 
  }
  
  public boolean getBooleanCapability(String capability) {
    return this.bools.contains(capability);
  }
  
  public Integer getNumericCapability(String capability) {
    return this.ints.get(capability);
  }
  
  public String getStringCapability(String capability) {
    return this.strings.get(capability);
  }
  
  private void parseInfoCmp() {
    String capabilities = null;
    if (this.type != null)
      try {
        capabilities = InfoCmp.getInfoCmp(this.type);
      } catch (Exception exception) {} 
    if (capabilities == null)
      capabilities = InfoCmp.getAnsiCaps(); 
    InfoCmp.parseInfoCmp(capabilities, this.bools, this.ints, this.strings);
  }
}
