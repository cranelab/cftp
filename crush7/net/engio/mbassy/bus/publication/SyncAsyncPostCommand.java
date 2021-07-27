package net.engio.mbassy.bus.publication;

import java.util.concurrent.TimeUnit;
import net.engio.mbassy.bus.IMessagePublication;
import net.engio.mbassy.bus.MBassador;

public class SyncAsyncPostCommand<T> implements ISyncAsyncPublicationCommand {
  private T message;
  
  private MBassador<T> mBassador;
  
  public SyncAsyncPostCommand(MBassador<T> mBassador, T message) {
    this.mBassador = mBassador;
    this.message = message;
  }
  
  public IMessagePublication now() {
    return this.mBassador.publish(this.message);
  }
  
  public IMessagePublication asynchronously() {
    return this.mBassador.publishAsync(this.message);
  }
  
  public IMessagePublication asynchronously(long timeout, TimeUnit unit) {
    return this.mBassador.publishAsync(this.message, timeout, unit);
  }
}
