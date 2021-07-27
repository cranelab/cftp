package org.fusesource.jansi.internal;

import org.fusesource.hawtjni.runtime.ArgFlag;
import org.fusesource.hawtjni.runtime.ClassFlag;
import org.fusesource.hawtjni.runtime.FieldFlag;
import org.fusesource.hawtjni.runtime.JniArg;
import org.fusesource.hawtjni.runtime.JniClass;
import org.fusesource.hawtjni.runtime.JniField;
import org.fusesource.hawtjni.runtime.JniMethod;
import org.fusesource.hawtjni.runtime.Library;
import org.fusesource.hawtjni.runtime.MethodFlag;

@JniClass
public class CLibrary {
  @JniMethod(flags = {MethodFlag.CONSTANT_INITIALIZER})
  private static native void init();
  
  @JniMethod(conditional = "defined(HAVE_ISATTY)")
  public static native int isatty(@JniArg int paramInt);
  
  @JniMethod(conditional = "defined(HAVE_TTYNAME)")
  public static native String ttyname(@JniArg int paramInt);
  
  @JniMethod(conditional = "defined(HAVE_TTYSLOT)")
  public static native int ttyslot();
  
  @JniMethod(conditional = "defined(HAVE_OPENPTY)")
  public static native int openpty(@JniArg(cast = "int *", flags = {ArgFlag.NO_IN}) int[] paramArrayOfint1, @JniArg(cast = "int *", flags = {ArgFlag.NO_IN}) int[] paramArrayOfint2, @JniArg(cast = "char *", flags = {ArgFlag.NO_IN}) byte[] paramArrayOfbyte, @JniArg(cast = "struct termios *", flags = {ArgFlag.NO_OUT}) Termios paramTermios, @JniArg(cast = "struct winsize *", flags = {ArgFlag.NO_OUT}) WinSize paramWinSize);
  
  @JniMethod(conditional = "defined(HAVE_TCGETATTR)")
  public static native int tcgetattr(@JniArg int paramInt, @JniArg(cast = "struct termios *", flags = {ArgFlag.NO_IN}) Termios paramTermios);
  
  @JniMethod(conditional = "defined(HAVE_TCSETATTR)")
  public static native int tcsetattr(@JniArg int paramInt1, @JniArg int paramInt2, @JniArg(cast = "struct termios *", flags = {ArgFlag.NO_OUT}) Termios paramTermios);
  
  @JniMethod(conditional = "defined(HAVE_IOCTL)")
  public static native int ioctl(@JniArg int paramInt, @JniArg long paramLong, @JniArg int[] paramArrayOfint);
  
  @JniMethod(conditional = "defined(HAVE_IOCTL) && defined(HAVE_OPENPTY)")
  public static native int ioctl(@JniArg int paramInt, @JniArg long paramLong, @JniArg(flags = {ArgFlag.POINTER_ARG}) WinSize paramWinSize);
  
  private static final Library LIBRARY = new Library("jansi", CLibrary.class);
  
  @JniField(flags = {FieldFlag.CONSTANT}, conditional = "defined(STDIN_FILENO)")
  public static int STDIN_FILENO;
  
  @JniField(flags = {FieldFlag.CONSTANT}, conditional = "defined(STDOUT_FILENO)")
  public static int STDOUT_FILENO;
  
  @JniField(flags = {FieldFlag.CONSTANT}, conditional = "defined(STDERR_FILENO)")
  public static int STDERR_FILENO;
  
  @JniField(flags = {FieldFlag.CONSTANT}, accessor = "1", conditional = "defined(HAVE_ISATTY)")
  public static boolean HAVE_ISATTY;
  
  @JniField(flags = {FieldFlag.CONSTANT}, conditional = "defined(TCSANOW)")
  public static int TCSANOW;
  
  @JniField(flags = {FieldFlag.CONSTANT}, conditional = "defined(TCSADRAIN)")
  public static int TCSADRAIN;
  
  @JniField(flags = {FieldFlag.CONSTANT}, conditional = "defined(TCSAFLUSH)")
  public static int TCSAFLUSH;
  
  @JniField(flags = {FieldFlag.CONSTANT}, conditional = "defined(TIOCGETA)")
  public static long TIOCGETA;
  
  @JniField(flags = {FieldFlag.CONSTANT}, conditional = "defined(TIOCSETA)")
  public static long TIOCSETA;
  
  @JniField(flags = {FieldFlag.CONSTANT}, conditional = "defined(TIOCGETD)")
  public static long TIOCGETD;
  
  @JniField(flags = {FieldFlag.CONSTANT}, conditional = "defined(TIOCSETD)")
  public static long TIOCSETD;
  
  @JniField(flags = {FieldFlag.CONSTANT}, conditional = "defined(TIOCGWINSZ)")
  public static long TIOCGWINSZ;
  
  @JniField(flags = {FieldFlag.CONSTANT}, conditional = "defined(TIOCSWINSZ)")
  public static long TIOCSWINSZ;
  
  public static final int VEOF = 0;
  
  public static final int VEOL = 1;
  
  public static final int VEOL2 = 2;
  
  public static final int VERASE = 3;
  
  public static final int VWERASE = 4;
  
  public static final int VKILL = 5;
  
  public static final int VREPRINT = 6;
  
  public static final int VINTR = 8;
  
  public static final int VQUIT = 9;
  
  public static final int VSUSP = 10;
  
  public static final int VDSUSP = 11;
  
  public static final int VSTART = 12;
  
  public static final int VSTOP = 13;
  
  public static final int VLNEXT = 14;
  
  public static final int VDISCARD = 15;
  
  public static final int VMIN = 16;
  
  public static final int VTIME = 17;
  
  public static final int VSTATUS = 18;
  
  public static final int IGNBRK = 1;
  
  public static final int BRKINT = 2;
  
  public static final int IGNPAR = 4;
  
  public static final int PARMRK = 8;
  
  public static final int INPCK = 16;
  
  public static final int ISTRIP = 32;
  
  public static final int INLCR = 64;
  
  public static final int IGNCR = 128;
  
  public static final int ICRNL = 256;
  
  public static final int IXON = 512;
  
  public static final int IXOFF = 1024;
  
  public static final int IXANY = 2048;
  
  public static final int OPOST = 1;
  
  public static final int ONLCR = 2;
  
  public static final int CCTS_OFLOW = 65536;
  
  public static final int CRTS_IFLOW = 131072;
  
  public static final int CRTSCTS = 196608;
  
  public static final int CDTR_IFLOW = 262144;
  
  public static final int CDSR_OFLOW = 524288;
  
  public static final int CCAR_OFLOW = 1048576;
  
  public static final int ECHOE = 2;
  
  public static final int ECHOK = 4;
  
  public static final int ECHO = 8;
  
  public static final int ECHONL = 16;
  
  public static final int ISIG = 128;
  
  public static final int ICANON = 256;
  
  public static final int IEXTEN = 1024;
  
  public static final int TOSTOP = 4194304;
  
  public static final int NOFLSH = -2147483648;
  
  static {
    LIBRARY.load();
    init();
  }
  
  @JniClass(flags = {ClassFlag.STRUCT}, name = "winsize", conditional = "defined(HAVE_OPENPTY)")
  public static class WinSize {
    @JniField(flags = {FieldFlag.CONSTANT}, accessor = "sizeof(struct winsize)")
    public static int SIZEOF;
    
    @JniField(accessor = "ws_row")
    public short ws_row;
    
    @JniField(accessor = "ws_col")
    public short ws_col;
    
    @JniField(accessor = "ws_xpixel")
    public short ws_xpixel;
    
    @JniField(accessor = "ws_ypixel")
    public short ws_ypixel;
    
    static {
      CLibrary.LIBRARY.load();
      init();
    }
    
    public WinSize() {}
    
    public WinSize(short ws_row, short ws_col) {
      this.ws_row = ws_row;
      this.ws_col = ws_col;
    }
    
    @JniMethod(flags = {MethodFlag.CONSTANT_INITIALIZER})
    private static native void init();
  }
  
  @JniClass(flags = {ClassFlag.STRUCT}, name = "termios", conditional = "defined(HAVE_OPENPTY)")
  public static class Termios {
    @JniField(flags = {FieldFlag.CONSTANT}, accessor = "sizeof(struct termios)")
    public static int SIZEOF;
    
    @JniField(accessor = "c_iflag")
    public long c_iflag;
    
    @JniField(accessor = "c_oflag")
    public long c_oflag;
    
    @JniField(accessor = "c_cflag")
    public long c_cflag;
    
    @JniField(accessor = "c_lflag")
    public long c_lflag;
    
    @JniMethod(flags = {MethodFlag.CONSTANT_INITIALIZER})
    private static native void init();
    
    static {
      CLibrary.LIBRARY.load();
      init();
    }
    
    @JniField(accessor = "c_cc")
    public byte[] c_cc = new byte[20];
    
    @JniField(accessor = "c_ispeed")
    public long c_ispeed;
    
    @JniField(accessor = "c_ospeed")
    public long c_ospeed;
  }
}
