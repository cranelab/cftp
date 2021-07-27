package net.engio.mbassy.bus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import net.engio.mbassy.bus.common.IMessageBus;
import net.engio.mbassy.bus.config.ConfigurationError;
import net.engio.mbassy.bus.config.Feature;
import net.engio.mbassy.bus.config.IBusConfiguration;
import net.engio.mbassy.bus.error.InternalPublicationError;
import net.engio.mbassy.bus.publication.ISyncAsyncPublicationCommand;

public abstract class AbstractSyncAsyncMessageBus<T, P extends ISyncAsyncPublicationCommand> extends AbstractPubSubSupport<T> implements IMessageBus<T, P> {
  private final ExecutorService executor;
  
  private final List<Thread> dispatchers;
  
  private final BlockingQueue<IMessagePublication> pendingMessages;
  
  protected AbstractSyncAsyncMessageBus(IBusConfiguration configuration) {
    super(configuration);
    Feature.AsynchronousMessageDispatch asyncDispatch = configuration.<Feature.AsynchronousMessageDispatch>getFeature(Feature.AsynchronousMessageDispatch.class);
    if (asyncDispatch == null)
      throw ConfigurationError.MissingFeature(Feature.AsynchronousMessageDispatch.class); 
    this.pendingMessages = asyncDispatch.getMessageQueue();
    this.dispatchers = new ArrayList<Thread>(asyncDispatch.getNumberOfMessageDispatchers());
    initDispatcherThreads(asyncDispatch);
    Feature.AsynchronousHandlerInvocation asyncInvocation = configuration.<Feature.AsynchronousHandlerInvocation>getFeature(Feature.AsynchronousHandlerInvocation.class);
    if (asyncInvocation == null)
      throw ConfigurationError.MissingFeature(Feature.AsynchronousHandlerInvocation.class); 
    this.executor = asyncInvocation.getExecutor();
    getRuntime().add("bus.handlers.async-executor", this.executor);
  }
  
  private void initDispatcherThreads(Feature.AsynchronousMessageDispatch configuration) {
    for (int i = 0; i < configuration.getNumberOfMessageDispatchers(); i++) {
      Thread dispatcher = configuration.getDispatcherThreadFactory().newThread(new Runnable() {
            public void run() {
              while (true) {
                IMessagePublication publication = null;
                try {
                  publication = AbstractSyncAsyncMessageBus.this.pendingMessages.take();
                  publication.execute();
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  return;
                } catch (Throwable t) {
                  AbstractSyncAsyncMessageBus.this.handlePublicationError(new InternalPublicationError(t, "Error in asynchronous dispatch", publication));
                } 
              } 
            }
          });
      dispatcher.setName("MsgDispatcher-" + i);
      this.dispatchers.add(dispatcher);
      dispatcher.start();
    } 
  }
  
  protected IMessagePublication addAsynchronousPublication(MessagePublication publication) {
    try {
      this.pendingMessages.put(publication);
      return publication.markScheduled();
    } catch (InterruptedException e) {
      handlePublicationError(new InternalPublicationError(e, "Error while adding an asynchronous message publication", publication));
      return publication;
    } 
  }
  
  protected IMessagePublication addAsynchronousPublication(MessagePublication publication, long timeout, TimeUnit unit) {
    try {
      return this.pendingMessages.offer(publication, timeout, unit) ? publication
        .markScheduled() : publication;
    } catch (InterruptedException e) {
      handlePublicationError(new InternalPublicationError(e, "Error while adding an asynchronous message publication", publication));
      return publication;
    } 
  }
  
  protected void finalize() throws Throwable {
    super.finalize();
    shutdown();
  }
  
  public void shutdown() {
    for (Thread dispatcher : this.dispatchers)
      dispatcher.interrupt(); 
    if (this.executor != null)
      this.executor.shutdown(); 
  }
  
  public boolean hasPendingMessages() {
    return (this.pendingMessages.size() > 0);
  }
}
