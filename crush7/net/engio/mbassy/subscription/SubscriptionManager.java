package net.engio.mbassy.subscription;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.engio.mbassy.bus.BusRuntime;
import net.engio.mbassy.common.ReflectionUtils;
import net.engio.mbassy.common.StrongConcurrentSet;
import net.engio.mbassy.listener.MessageHandler;
import net.engio.mbassy.listener.MetadataReader;

public class SubscriptionManager {
  private final MetadataReader metadataReader;
  
  private final Map<Class, ArrayList<Subscription>> subscriptionsPerMessage;
  
  private final Map<Class, Subscription[]> subscriptionsPerListener;
  
  private final StrongConcurrentSet<Class> nonListeners = new StrongConcurrentSet<Class<?>>();
  
  private final SubscriptionFactory subscriptionFactory;
  
  private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  
  private final BusRuntime runtime;
  
  public SubscriptionManager(MetadataReader metadataReader, SubscriptionFactory subscriptionFactory, BusRuntime runtime) {
    this.metadataReader = metadataReader;
    this.subscriptionFactory = subscriptionFactory;
    this.runtime = runtime;
    this.subscriptionsPerMessage = (Map)new HashMap<Class<?>, ArrayList<Subscription>>(256);
    this.subscriptionsPerListener = (Map)new HashMap<Class<?>, Subscription>(256);
  }
  
  public boolean unsubscribe(Object listener) {
    if (listener == null)
      return false; 
    Subscription[] subscriptions = getSubscriptionsByListener(listener);
    if (subscriptions == null)
      return false; 
    boolean isRemoved = true;
    for (Subscription subscription : subscriptions)
      isRemoved &= subscription.unsubscribe(listener); 
    return isRemoved;
  }
  
  private Subscription[] getSubscriptionsByListener(Object listener) {
    Subscription[] subscriptions;
    ReentrantReadWriteLock.ReadLock readLock = this.readWriteLock.readLock();
    try {
      readLock.lock();
      subscriptions = this.subscriptionsPerListener.get(listener.getClass());
    } finally {
      readLock.unlock();
    } 
    return subscriptions;
  }
  
  public void subscribe(Object listener) {
    try {
      Class<?> listenerClass = listener.getClass();
      if (this.nonListeners.contains(listenerClass))
        return; 
      Subscription[] subscriptionsByListener = getSubscriptionsByListener(listener);
      if (subscriptionsByListener == null) {
        MessageHandler[] messageHandlers = this.metadataReader.getMessageListener(listenerClass).getHandlers();
        int length = messageHandlers.length;
        if (length == 0) {
          this.nonListeners.add(listenerClass);
          return;
        } 
        subscriptionsByListener = new Subscription[length];
        for (int i = 0; i < length; i++) {
          MessageHandler messageHandler = messageHandlers[i];
          subscriptionsByListener[i] = this.subscriptionFactory.createSubscription(this.runtime, messageHandler);
        } 
        subscribe(listener, subscriptionsByListener);
      } else {
        for (Subscription sub : subscriptionsByListener)
          sub.subscribe(listener); 
      } 
    } catch (Exception e) {
      throw new RuntimeException(e);
    } 
  }
  
  private void subscribe(Object listener, Subscription[] subscriptions) {
    ReentrantReadWriteLock.WriteLock writeLock = this.readWriteLock.writeLock();
    try {
      writeLock.lock();
      Subscription[] subscriptionsByListener = getSubscriptionsByListener(listener);
      if (subscriptionsByListener == null) {
        for (int i = 0, n = subscriptions.length; i < n; i++) {
          Subscription subscription = subscriptions[i];
          subscription.subscribe(listener);
          for (Class<?> messageType : subscription.getHandledMessageTypes()) {
            ArrayList<Subscription> subscriptions2 = this.subscriptionsPerMessage.get(messageType);
            if (subscriptions2 == null) {
              subscriptions2 = new ArrayList<Subscription>(8);
              this.subscriptionsPerMessage.put(messageType, subscriptions2);
            } 
            subscriptions2.add(subscription);
          } 
        } 
        this.subscriptionsPerListener.put(listener.getClass(), subscriptions);
      } else {
        for (int i = 0, n = subscriptionsByListener.length; i < n; i++) {
          Subscription existingSubscription = subscriptionsByListener[i];
          existingSubscription.subscribe(listener);
        } 
      } 
    } finally {
      writeLock.unlock();
    } 
  }
  
  public Collection<Subscription> getSubscriptionsByMessageType(Class<?> messageType) {
    Set<Subscription> subscriptions = new TreeSet<Subscription>(Subscription.SubscriptionByPriorityDesc);
    ReentrantReadWriteLock.ReadLock readLock = this.readWriteLock.readLock();
    try {
      readLock.lock();
      ArrayList<Subscription> subsPerMessage = this.subscriptionsPerMessage.get(messageType);
      if (subsPerMessage != null)
        subscriptions.addAll(subsPerMessage); 
      Class[] types = ReflectionUtils.getSuperTypes(messageType);
      for (int i = 0, n = types.length; i < n; i++) {
        Class eventSuperType = types[i];
        ArrayList<Subscription> subs = this.subscriptionsPerMessage.get(eventSuperType);
        if (subs != null)
          for (int j = 0, m = subs.size(); j < m; j++) {
            Subscription subscription = subs.get(j);
            if (subscription.handlesMessageType(messageType))
              subscriptions.add(subscription); 
          }  
      } 
    } finally {
      readLock.unlock();
    } 
    return subscriptions;
  }
}
