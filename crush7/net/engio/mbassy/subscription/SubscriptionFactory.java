package net.engio.mbassy.subscription;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Collection;
import net.engio.mbassy.bus.BusRuntime;
import net.engio.mbassy.bus.error.IPublicationErrorHandler;
import net.engio.mbassy.bus.error.MessageBusException;
import net.engio.mbassy.common.StrongConcurrentSet;
import net.engio.mbassy.common.WeakConcurrentSet;
import net.engio.mbassy.dispatch.AsynchronousHandlerInvocation;
import net.engio.mbassy.dispatch.EnvelopedMessageDispatcher;
import net.engio.mbassy.dispatch.FilteredMessageDispatcher;
import net.engio.mbassy.dispatch.HandlerInvocation;
import net.engio.mbassy.dispatch.IHandlerInvocation;
import net.engio.mbassy.dispatch.IMessageDispatcher;
import net.engio.mbassy.dispatch.MessageDispatcher;
import net.engio.mbassy.dispatch.SynchronizedHandlerInvocation;
import net.engio.mbassy.listener.MessageHandler;

public class SubscriptionFactory {
  public Subscription createSubscription(BusRuntime runtime, MessageHandler handlerMetadata) throws MessageBusException {
    try {
      Collection<IPublicationErrorHandler> errorHandlers = runtime.<Collection<IPublicationErrorHandler>>get("bus.handlers.error");
      SubscriptionContext context = new SubscriptionContext(runtime, handlerMetadata, errorHandlers);
      IHandlerInvocation invocation = buildInvocationForHandler(context);
      IMessageDispatcher dispatcher = buildDispatcher(context, invocation);
      return new Subscription(context, dispatcher, handlerMetadata.useStrongReferences() ? new StrongConcurrentSet() : new WeakConcurrentSet());
    } catch (MessageBusException e) {
      throw e;
    } catch (Exception e) {
      throw new MessageBusException(e);
    } 
  }
  
  protected IHandlerInvocation buildInvocationForHandler(SubscriptionContext context) throws MessageBusException {
    IHandlerInvocation invocation = createBaseHandlerInvocation(context);
    if (context.getHandler().isSynchronized())
      invocation = new SynchronizedHandlerInvocation(invocation); 
    if (context.getHandler().isAsynchronous())
      invocation = new AsynchronousHandlerInvocation(invocation); 
    return invocation;
  }
  
  protected IMessageDispatcher buildDispatcher(SubscriptionContext context, IHandlerInvocation invocation) throws MessageBusException {
    IMessageDispatcher dispatcher = new MessageDispatcher(context, invocation);
    if (context.getHandler().isEnveloped())
      dispatcher = new EnvelopedMessageDispatcher(dispatcher); 
    if (context.getHandler().isFiltered())
      dispatcher = new FilteredMessageDispatcher(dispatcher); 
    return dispatcher;
  }
  
  protected IHandlerInvocation createBaseHandlerInvocation(SubscriptionContext context) throws MessageBusException {
    Class<? extends HandlerInvocation> invocation = context.getHandler().getHandlerInvocation();
    if (invocation.isMemberClass() && !Modifier.isStatic(invocation.getModifiers()))
      throw new MessageBusException("The handler invocation must be top level class or nested STATIC inner class"); 
    try {
      Constructor<? extends IHandlerInvocation> constructor = invocation.getConstructor(new Class[] { SubscriptionContext.class });
      return constructor.newInstance(new Object[] { context });
    } catch (NoSuchMethodException e) {
      throw new MessageBusException("The provided handler invocation did not specify the necessary constructor " + invocation
          .getSimpleName() + "(SubscriptionContext);", e);
    } catch (Exception e) {
      throw new MessageBusException("Could not instantiate the provided handler invocation " + invocation
          .getSimpleName(), e);
    } 
  }
}
