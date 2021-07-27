package crushftp.handlers;

import crushftp.server.ServerSession;
import java.util.Date;

public class IdlerKiller implements Runnable {
  public int sleep_interval = 10000;
  
  public long last_activity = 0L;
  
  public long timeout = 0L;
  
  Thread the_thread = null;
  
  ServerSession calling_session = null;
  
  public boolean enabled = true;
  
  public boolean die_now = false;
  
  String type = "";
  
  public IdlerKiller(ServerSession calling_session, long last_activity, long timeout, Thread the_thread) {
    this.calling_session = calling_session;
    this.the_thread = the_thread;
    this.last_activity = last_activity;
    this.timeout = timeout;
  }
  
  public void run() {
    try {
      while (!this.die_now) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < this.sleep_interval && !this.die_now && !this.calling_session.uiBG("dieing")) {
          Thread.sleep(1000L);
          Thread.currentThread().setName(String.valueOf(this.calling_session.uiSG("user_name")) + ":(" + this.calling_session.uiSG("user_number") + ")-" + this.calling_session.uiSG("user_ip") + " (idle_time)");
        } 
        if ((new Date()).getTime() - this.last_activity > this.timeout * 1000L * 60L && this.enabled) {
          try {
            this.calling_session.uiPUT("termination_message", "TIMEOUT");
          } catch (Exception exception) {}
          try {
            this.calling_session.not_done = false;
          } catch (Exception exception) {}
          try {
            if (this.the_thread != null)
              this.the_thread.interrupt(); 
          } catch (Exception exception) {}
          try {
            this.calling_session.do_kill();
          } catch (Exception exception) {}
          break;
        } 
      } 
    } catch (Exception exception) {}
    this.calling_session = null;
    this.the_thread = null;
  }
}
