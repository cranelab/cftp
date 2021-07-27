package com.crushftp.tunnel2;

import java.io.IOException;
import java.util.Properties;
import java.util.Vector;

public class ConnectionOptimizer implements Runnable {
  Tunnel2 t = null;
  
  Vector outgoing = new Vector();
  
  Vector incoming = new Vector();
  
  Vector speedHistory = new Vector();
  
  int stableSeconds = 5;
  
  int channelRampUp = 1;
  
  int minFastSpeed = 100;
  
  int minSlowSpeed = 10;
  
  double speedThreshold = 0.6D;
  
  int closeInRequests = 0;
  
  long baseSpeedInIntervals = 0L;
  
  long baseSpeedOutIntervals = 0L;
  
  public ConnectionOptimizer(Tunnel2 t) {
    this.t = t;
    this.stableSeconds = Integer.parseInt(t.tunnel.getProperty("stableSeconds", "5"));
    this.channelRampUp = Integer.parseInt(t.tunnel.getProperty("channelRampUp", "1"));
    this.minFastSpeed = Integer.parseInt(t.tunnel.getProperty("minFastSpeed", "100"));
    this.minSlowSpeed = Integer.parseInt(t.tunnel.getProperty("minSlowSpeed", "10"));
    this.speedThreshold = Integer.parseInt(t.tunnel.getProperty("speedThreshold", "60")) / 100.0D;
  }
  
  public void run() {
    Thread.currentThread().setName("Tunnel Connection Optimizer");
    int noIncomingCount = 0;
    while (this.t.isActive()) {
      try {
        addOut();
        addIn();
        Thread.sleep(1000L);
        long intervals = 0L;
        while (this.t.isActive()) {
          intervals++;
          calcSpeed();
          if (this.baseSpeedInIntervals++ == this.stableSeconds) {
            float speedIn = 0.0F;
            for (int x = 0; x < this.speedHistory.size(); x++) {
              Properties p = this.speedHistory.elementAt(x);
              speedIn += Float.parseFloat(p.getProperty("speedIn"));
            } 
            this.t.baseSpeedIn = speedIn / this.speedHistory.size();
            Tunnel2.msg("Tunnel2:stableSeconds=" + this.stableSeconds + ":channelRampUp=" + this.channelRampUp + ":minFastSpeed=" + this.minFastSpeed + ":minSlowSpeed=" + this.minSlowSpeed + ":speedThreshold=" + this.speedThreshold + ":Base Speed In=" + (this.t.baseSpeedIn / 1024.0F) + "K/sec  Out=" + (this.t.baseSpeedOut / 1024.0F) + "K/sec");
            if (this.t.baseSpeedIn < 5.0F)
              this.baseSpeedInIntervals = 0L; 
          } 
          if (this.baseSpeedOutIntervals++ == this.stableSeconds) {
            float speedOut = 0.0F;
            for (int x = 0; x < this.speedHistory.size(); x++) {
              Properties p = this.speedHistory.elementAt(x);
              speedOut += Float.parseFloat(p.getProperty("speedOut"));
            } 
            this.t.baseSpeedOut = speedOut / this.speedHistory.size();
            Tunnel2.msg("Tunnel2:stableSeconds=" + this.stableSeconds + ":channelRampUp=" + this.channelRampUp + ":minFastSpeed=" + this.minFastSpeed + ":minSlowSpeed=" + this.minSlowSpeed + ":speedThreshold=" + this.speedThreshold + ":Base Speed In=" + (this.t.baseSpeedIn / 1024.0F) + "K/sec  Out=" + (this.t.baseSpeedOut / 1024.0F) + "K/sec");
            if (this.t.baseSpeedOut < 5.0F)
              this.baseSpeedOutIntervals = 0L; 
          } 
          int i = this.channelRampUp;
          if (intervals == (this.stableSeconds + 1))
            i *= 2; 
          if (this.t.baseSpeedIn > 0.0F) {
            float speed = getLast10("speedIn", "countIn");
            if (speed / 1024.0F > this.minFastSpeed && speed > this.t.baseSpeedIn * this.speedThreshold) {
              if (this.incoming.size() < Math.abs(Integer.parseInt(this.t.tunnel.getProperty("channelsInMax", "1")))) {
                Tunnel2.msg("Tunnel2:Fast incoming speed:" + (speed / 1024.0F) + "K/sec versus base:" + (this.t.baseSpeedIn / 1024.0F) + "K/sec, adding " + i + " channels.");
                for (int x = 0; x < i; x++)
                  addIn(); 
              } 
            } else if (speed / 1024.0F < this.minSlowSpeed && this.incoming.size() > 1) {
              if (Integer.parseInt(this.t.tunnel.getProperty("channelsInMax", "1")) >= 0) {
                Tunnel2.msg("Tunnel2:Slow incoming speed:" + (speed / 1024.0F) + "K/sec versus base:" + (this.t.baseSpeedIn / 1024.0F) + "K/sec, removing channel.");
                removeIn(null);
              } 
            } 
            if (this.speedHistory.size() > 10 && speed > this.t.baseSpeedIn)
              this.t.baseSpeedIn = speed; 
          } 
          if (this.t.baseSpeedOut > 0.0F) {
            float speed = getLast10("speedOut", "countOut");
            if (speed / 1024.0F > this.minFastSpeed && speed > this.t.baseSpeedOut * this.speedThreshold) {
              if (this.outgoing.size() < Math.abs(Integer.parseInt(this.t.tunnel.getProperty("channelsOutMax", "1")))) {
                Tunnel2.msg("Tunnel2:Fast outgoing speed:" + (speed / 1024.0F) + "K/sec versus base:" + (this.t.baseSpeedOut / 1024.0F) + "K/sec, adding " + i + " channels.");
                for (int x = 0; x < i; x++)
                  addOut(); 
              } 
            } else if (speed / 1024.0F < this.minSlowSpeed && this.outgoing.size() > 1) {
              if (Integer.parseInt(this.t.tunnel.getProperty("channelsOutMax", "1")) >= 0) {
                Tunnel2.msg("Tunnel2:Slow outgoing speed:" + (speed / 1024.0F) + "K/sec versus base:" + (this.t.baseSpeedOut / 1024.0F) + "K/sec, removing channel.");
                removeOut(null);
              } 
            } 
            if (this.speedHistory.size() > 10 && speed > this.t.baseSpeedOut)
              this.t.baseSpeedOut = speed; 
          } 
          if (this.incoming.size() == 0) {
            addIn();
            if (noIncomingCount++ > 10) {
              if (this.t.username == null || this.t.username.equals(""))
                break; 
              (new Thread(new Runnable(this) {
                    final ConnectionOptimizer this$0;
                    
                    public void run() {
                      Tunnel2.msg("Tunnel2:Tunnel on server appears to be disconnecting us, resetting tunnel.");
                      this.this$0.t.reset();
                    }
                  })).start();
              noIncomingCount = 0;
            } 
          } else {
            noIncomingCount = 0;
          } 
          if (this.outgoing.size() == 0)
            addOut(); 
          Thread.sleep(1000L);
        } 
      } catch (Exception e) {
        Tunnel2.msg(e);
      } 
      try {
        Thread.sleep(5000L);
      } catch (Exception exception) {}
    } 
    while (this.outgoing.size() > 1) {
      QueueTransfer qt = this.outgoing.remove(this.outgoing.size() - 1);
      qt.close();
    } 
    int loops = 0;
    while (this.incoming.size() > 0 && loops++ < 10) {
      removeIn(null);
      try {
        Thread.sleep(1000L);
      } catch (Exception exception) {}
      if (this.outgoing.size() == 0)
        addOut(); 
    } 
    while (this.outgoing.size() > 0) {
      QueueTransfer qt = this.outgoing.remove(this.outgoing.size() - 1);
      qt.close();
    } 
    this.t.stopThisTunnel();
    this.t.setShutdown(true);
    Tunnel2.msg("Tunnel2:Tunnel shutdown.");
  }
  
  public float getLast10(String type1, String type2) {
    float average = 0.0F;
    int loops = 0;
    for (int x = this.speedHistory.size() - 1; x >= 0 && loops++ < 10; x--) {
      Properties p = this.speedHistory.elementAt(x);
      try {
        average += Float.parseFloat(p.getProperty(type1)) / Float.parseFloat(p.getProperty(type2));
      } catch (Exception exception) {}
    } 
    average /= loops;
    return average;
  }
  
  public void calcSpeed() {
    long bOut = 0L;
    long bIn = 0L;
    int x;
    for (x = 0; x < this.outgoing.size(); x++) {
      QueueTransfer q = this.outgoing.elementAt(x);
      long temp = q.getTransferred();
      bOut += temp;
    } 
    for (x = 0; x < this.incoming.size(); x++) {
      QueueTransfer q = this.incoming.elementAt(x);
      long temp = q.getTransferred();
      bIn += temp;
    } 
    Properties p = new Properties();
    p.put("speedOut", (new StringBuffer(String.valueOf(bOut))).toString());
    p.put("speedIn", (new StringBuffer(String.valueOf(bIn))).toString());
    p.put("countOut", (new StringBuffer(String.valueOf(this.outgoing.size()))).toString());
    p.put("countIn", (new StringBuffer(String.valueOf(this.incoming.size()))).toString());
    this.speedHistory.addElement(p);
    while (this.speedHistory.size() > 60)
      this.speedHistory.remove(0); 
  }
  
  public void addOut() {
    if (this.outgoing.size() >= Math.abs(Integer.parseInt(this.t.tunnel.getProperty("channelsOutMax", "1"))))
      return; 
    QueueTransfer qt = new QueueTransfer(this.t, "send", this.outgoing);
    this.outgoing.addElement(qt);
    Tunnel2.msg("Tunnel2:Adding outgoing channel. incoming:" + this.incoming.size() + " outgoing:" + this.outgoing.size());
    (new Thread(qt)).start();
  }
  
  public void addIn() {
    if (this.incoming.size() >= Math.abs(Integer.parseInt(this.t.tunnel.getProperty("channelsInMax", "1"))))
      return; 
    QueueTransfer qt = new QueueTransfer(this.t, "get", this.incoming);
    this.incoming.addElement(qt);
    Tunnel2.msg("Tunnel2:Adding incoming channel. incoming:" + this.incoming.size() + " outgoing:" + this.outgoing.size());
    (new Thread(qt)).start();
  }
  
  public void removeOut(QueueTransfer qt) {
    if (qt != null) {
      this.outgoing.removeElement(qt);
      Tunnel2.msg("Tunnel2:Removing outgoing channel. incoming:" + this.incoming.size() + " outgoing:" + this.outgoing.size());
    } else {
      qt = this.outgoing.get(this.outgoing.size() - 1);
      qt.close();
      Tunnel2.msg("Tunnel2:Request remove outgoing channel. incoming:" + this.incoming.size() + " outgoing:" + this.outgoing.size());
    } 
  }
  
  public void removeIn(QueueTransfer qt) {
    if (this.closeInRequests > this.incoming.size() && this.closeInRequests > 10) {
      while (this.incoming.size() > 0) {
        QueueTransfer qt2 = this.incoming.remove(0);
        qt2.close();
        try {
          qt2.urlc.disconnect();
        } catch (IOException e) {
          Tunnel2.msg(e);
        } 
      } 
      addIn();
      this.closeInRequests = 0;
    } 
    if (qt != null) {
      this.incoming.removeElement(qt);
      Tunnel2.msg("Tunnel2:Removing incoming channel. incoming:" + this.incoming.size() + " outgoing:" + this.outgoing.size());
      this.closeInRequests = 0;
    } else {
      try {
        this.t.writeLocal(this.t.makeCommand(0, "CLOSEIN:0"), 0);
        this.closeInRequests++;
      } catch (Exception exception) {}
      Tunnel2.msg("Tunnel2:Request remove incoming channel. incoming:" + this.incoming.size() + " outgoing:" + this.outgoing.size() + " closeInRequests:" + this.closeInRequests);
    } 
  }
}
