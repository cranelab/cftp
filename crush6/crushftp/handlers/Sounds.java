package crushftp.handlers;

import java.io.File;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;

public class Sounds {
  static Class class$0;
  
  public void loadSound(Object object) {
    Object currentSound = null;
    if (object instanceof File)
      try {
        DataLine.Info info;
        currentSound = AudioSystem.getAudioInputStream((File)object);
        AudioInputStream stream = (AudioInputStream)currentSound;
        AudioFormat format = stream.getFormat();
        if (class$0 == null)
          try {
          
          } catch (ClassNotFoundException classNotFoundException) {
            throw new NoClassDefFoundError(null.getMessage());
          }  
        super(class$0 = Class.forName("javax.sound.sampled.Clip"), stream.getFormat(), (int)stream.getFrameLength() * format.getFrameSize());
        (new Thread(new Sounds$1$playTheClip(this, (Clip)AudioSystem.getLine(info), stream))).start();
      } catch (Exception exception) {} 
    currentSound = null;
  }
}
