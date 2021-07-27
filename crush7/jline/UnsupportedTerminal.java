package jline;

public class UnsupportedTerminal extends TerminalSupport {
  public UnsupportedTerminal() {
    super(false);
    setAnsiSupported(false);
    setEchoEnabled(true);
  }
}
