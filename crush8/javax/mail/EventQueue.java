package javax.mail;

import java.util.EventListener;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import javax.mail.event.MailEvent;

class EventQueue implements Runnable {
  private volatile BlockingQueue<QueueElement> q;
  
  private Executor executor;
  
  private static WeakHashMap<ClassLoader, EventQueue> appq;
  
  static class TerminatorEvent extends MailEvent {
    private static final long serialVersionUID = -2481895000841664111L;
    
    TerminatorEvent() {
      super(new Object());
    }
    
    public void dispatch(Object listener) {
      Thread.currentThread().interrupt();
    }
  }
  
  static class QueueElement {
    MailEvent event = null;
    
    Vector<? extends EventListener> vector = null;
    
    QueueElement(MailEvent event, Vector<? extends EventListener> vector) {
      this.event = event;
      this.vector = vector;
    }
  }
  
  EventQueue(Executor ex) {
    this.executor = ex;
  }
  
  synchronized void enqueue(MailEvent event, Vector<? extends EventListener> vector) {
    if (this.q == null) {
      this.q = new LinkedBlockingQueue<QueueElement>();
      if (this.executor != null) {
        this.executor.execute(this);
      } else {
        Thread qThread = new Thread(this, "JavaMail-EventQueue");
        qThread.setDaemon(true);
        qThread.start();
      } 
    } 
    this.q.add(new QueueElement(event, vector));
  }
  
  synchronized void terminateQueue() {
    if (this.q != null) {
      Vector<EventListener> dummyListeners = new Vector<EventListener>();
      dummyListeners.setSize(1);
      this.q.add(new QueueElement(new TerminatorEvent(), dummyListeners));
      this.q = null;
    } 
  }
  
  static synchronized EventQueue getApplicationEventQueue(Executor ex) {
    ClassLoader cl = Session.getContextClassLoader();
    if (appq == null)
      appq = new WeakHashMap<ClassLoader, EventQueue>(); 
    EventQueue q = appq.get(cl);
    if (q == null) {
      q = new EventQueue(ex);
      appq.put(cl, q);
    } 
    return q;
  }
  
  public void run() {
    BlockingQueue<QueueElement> bq = this.q;
    if (bq == null)
      return; 
    try {
      while (true) {
        QueueElement qe = bq.take();
        MailEvent e = qe.event;
        Vector<? extends EventListener> v = qe.vector;
        for (int i = 0; i < v.size(); i++) {
          try {
            e.dispatch(v.elementAt(i));
          } catch (Throwable t) {
            if (t instanceof InterruptedException)
              return; 
          } 
        } 
        qe = null;
        e = null;
        v = null;
      } 
    } catch (InterruptedException interruptedException) {}
  }
}
