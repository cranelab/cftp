package crushftp.handlers;

import com.crushftp.client.Worker;
import java.net.Socket;
import java.util.Date;
import java.util.Vector;

public class IdleMonitor {
  public static Vector children = new Vector();
  
  public int sleep_interval = 10000;
  
  public long last_activity = 0L;
  
  public long timeout = 0L;
  
  Thread the_thread = null;
  
  public SessionCrush calling_session = null;
  
  public boolean enabled = true;
  
  public boolean die_now = false;
  
  Socket sock = null;
  
  String type = "";
  
  public IdleMonitor(SessionCrush calling_session, long last_activity, long timeout, Thread the_thread) {
    this.calling_session = calling_session;
    this.the_thread = the_thread;
    this.last_activity = last_activity;
    this.timeout = timeout;
    children.addElement(this);
  }
  
  public IdleMonitor(SessionCrush calling_session, long last_activity, long timeout, Thread the_thread, Socket sock) {
    this.calling_session = calling_session;
    this.the_thread = the_thread;
    this.last_activity = last_activity;
    this.timeout = timeout;
    this.sock = sock;
    children.addElement(this);
  }
  
  public boolean finished() {
    boolean exit = this.die_now;
    try {
      if (this.timeout < 0L)
        exit = true; 
      if (this.die_now || (this.calling_session != null && this.calling_session.uiBG("dieing")) || (this.calling_session != null && this.calling_session.session_socks.size() == 0))
        exit = true; 
      long timeout2 = this.timeout;
      if (timeout2 < 0L) {
        timeout2 *= -1L;
      } else {
        timeout2 *= 60L;
      } 
      Thread.currentThread().setName("Global Idle Monitor:" + children.size() + ":" + timeout2 + ":" + ((new Date()).getTime() - this.last_activity) + ":" + exit + ":" + this.timeout);
      if ((new Date()).getTime() - this.last_activity > timeout2 * 1000L && this.enabled) {
        exit = true;
        Worker.startWorker(new Runnable(this) {
              final IdleMonitor this$0;
              
              public void run() {
                try {
                  if (this.this$0.calling_session != null)
                    this.this$0.calling_session.uiPUT("termination_message", "TIMEOUT"); 
                } catch (Exception exception) {}
                try {
                  if (this.this$0.calling_session != null)
                    this.this$0.calling_session.not_done = false; 
                } catch (Exception exception) {}
                try {
                  if (this.this$0.the_thread != null)
                    this.this$0.the_thread.interrupt(); 
                } catch (Exception exception) {}
                try {
                  if (this.this$0.calling_session != null) {
                    Log.log("SERVER", 1, "Closing idle session:" + Thread.currentThread().getName());
                    this.this$0.calling_session.do_kill(null);
                  } else if (this.this$0.sock != null) {
                    Log.log("SERVER", 1, "Closing idle HTTP socket before login:" + this.this$0.sock);
                    this.this$0.sock.close();
                  } 
                } catch (Exception exception) {}
              }
            }"IdleMonitor closer");
      } 
    } catch (Exception exception) {}
    return exit;
  }
  
  public static void init() throws Exception {
    Worker.startWorker(new Runnable() {
          public void run() {
            while (true) {
              Thread.currentThread().setName("Global Idle Monitor:" + IdleMonitor.children.size());
              try {
                Thread.sleep(1000L);
              } catch (InterruptedException interruptedException) {}
              for (int x = IdleMonitor.children.size() - 1; x >= 0; x--) {
                if (((IdleMonitor)IdleMonitor.children.elementAt(x)).finished())
                  IdleMonitor.children.remove(x); 
              } 
              try {
                Thread.sleep(1000L);
              } catch (InterruptedException interruptedException) {}
            } 
          }
        }"Global Idle Monitor");
  }
}
