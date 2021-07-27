package net.engio.mbassy.dispatch;

import java.util.concurrent.ExecutorService;
import net.engio.mbassy.bus.MessagePublication;
import net.engio.mbassy.subscription.AbstractSubscriptionContextAware;

public class AsynchronousHandlerInvocation extends AbstractSubscriptionContextAware implements IHandlerInvocation {
  private final IHandlerInvocation delegate;
  
  private final ExecutorService executor;
  
  public AsynchronousHandlerInvocation(IHandlerInvocation delegate) {
    super(delegate.getContext());
    this.delegate = delegate;
    this.executor = delegate.getContext().getRuntime().<ExecutorService>get("bus.handlers.async-executor");
  }
  
  public void invoke(final Object listener, final Object message, final MessagePublication publication) {
    this.executor.execute(new Runnable() {
          public void run() {
            AsynchronousHandlerInvocation.this.delegate.invoke(listener, message, publication);
          }
        });
  }
}
