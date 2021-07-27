package net.engio.mbassy.bus.publication;

import java.util.concurrent.TimeUnit;
import net.engio.mbassy.bus.IMessagePublication;

public interface ISyncAsyncPublicationCommand extends IPublicationCommand {
  IMessagePublication asynchronously();
  
  IMessagePublication asynchronously(long paramLong, TimeUnit paramTimeUnit);
}
