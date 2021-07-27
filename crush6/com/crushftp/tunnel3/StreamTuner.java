package com.crushftp.tunnel3;

import com.crushftp.client.Common;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class StreamTuner implements Runnable {
  StreamController sc = null;
  
  public Vector speedHistory = new Vector();
  
  public Properties speedHistoryIp = new Properties();
  
  int stableSeconds = 5;
  
  int channelRampUp = 1;
  
  int minFastSpeed = 100;
  
  int minSlowSpeed = 10;
  
  double speedThreshold = 0.6D;
  
  int closeInRequests = 0;
  
  long baseSpeedInIntervals = 0L;
  
  long baseSpeedOutIntervals = 0L;
  
  float baseSpeedOut = 0.0F;
  
  float baseSpeedIn = 0.0F;
  
  boolean active = true;
  
  int noIncomingCount = 0;
  
  public StreamTuner(StreamController sc) {
    this.sc = sc;
  }
  
  public void run() {
    this.active = true;
    Thread.currentThread().setName("Tunnel Connection Tunner:" + this.sc);
    this.noIncomingCount = 0;
    while (this.sc.isActive()) {
      this.stableSeconds = Integer.parseInt(this.sc.tunnel.getProperty("stableSeconds", "5"));
      this.channelRampUp = Integer.parseInt(this.sc.tunnel.getProperty("channelRampUp", "1"));
      this.minFastSpeed = Integer.parseInt(this.sc.tunnel.getProperty("minFastSpeed", "100"));
      this.minSlowSpeed = Integer.parseInt(this.sc.tunnel.getProperty("minSlowSpeed", "10"));
      this.speedThreshold = Integer.parseInt(this.sc.tunnel.getProperty("speedThreshold", "60")) / 100.0D;
      try {
        addOut();
        addIn();
        Thread.sleep(1000L);
        long intervals = 0L;
        while (this.sc.isActive()) {
          intervals++;
          calcSpeed();
          if (this.baseSpeedInIntervals++ == this.stableSeconds) {
            float speedIn = 0.0F;
            for (int x = 0; x < this.speedHistory.size(); x++) {
              Properties p = this.speedHistory.elementAt(x);
              speedIn += Float.parseFloat(p.getProperty("speedIn"));
            } 
            this.baseSpeedIn = speedIn / this.speedHistory.size();
            this.sc.msg("Tunnel3:stableSeconds=" + this.stableSeconds + ":channelRampUp=" + this.channelRampUp + ":minFastSpeed=" + this.minFastSpeed + ":minSlowSpeed=" + this.minSlowSpeed + ":speedThreshold=" + this.speedThreshold + ":Base Speed In=" + (this.baseSpeedIn / 1024.0F) + "K/sec  Out=" + (this.baseSpeedOut / 1024.0F) + "K/sec");
            if (this.baseSpeedIn < 5.0F)
              this.baseSpeedInIntervals = 0L; 
          } 
          if (this.baseSpeedOutIntervals++ == this.stableSeconds) {
            float speedOut = 0.0F;
            for (int x = 0; x < this.speedHistory.size(); x++) {
              Properties p = this.speedHistory.elementAt(x);
              speedOut += Float.parseFloat(p.getProperty("speedOut"));
            } 
            this.baseSpeedOut = speedOut / this.speedHistory.size();
            this.sc.msg("Tunnel3:stableSeconds=" + this.stableSeconds + ":channelRampUp=" + this.channelRampUp + ":minFastSpeed=" + this.minFastSpeed + ":minSlowSpeed=" + this.minSlowSpeed + ":speedThreshold=" + this.speedThreshold + ":Base Speed In=" + (this.baseSpeedIn / 1024.0F) + "K/sec  Out=" + (this.baseSpeedOut / 1024.0F) + "K/sec");
            if (this.baseSpeedOut < 5.0F)
              this.baseSpeedOutIntervals = 0L; 
          } 
          int i = this.channelRampUp;
          if (intervals == (this.stableSeconds + 1))
            i *= 2; 
          if (this.baseSpeedIn > 0.0F) {
            float speed = getSpeedAverage(this.speedHistory, "speedIn", "countIn", 10);
            if (speed / 1024.0F > this.minFastSpeed && speed > this.baseSpeedIn * this.speedThreshold) {
              if (this.sc.incoming.size() < Math.abs(Integer.parseInt(this.sc.tunnel.getProperty("channelsInMax", "1")))) {
                this.sc.msg("Tunnel3:Fast incoming speed:" + (speed / 1024.0F) + "K/sec versus base:" + (this.baseSpeedIn / 1024.0F) + "K/sec, adding " + i + " channels.");
                for (int x = 0; x < i; x++)
                  addIn(); 
              } 
            } else if (speed / 1024.0F < this.minSlowSpeed && this.sc.incoming.size() > 1) {
              if (Integer.parseInt(this.sc.tunnel.getProperty("channelsInMax", "1")) >= 0) {
                this.sc.msg("Tunnel3:Slow incoming speed:" + (speed / 1024.0F) + "K/sec versus base:" + (this.baseSpeedIn / 1024.0F) + "K/sec, removing channel.");
                for (int x = 0; x < i; x++)
                  removeIn(null); 
              } 
            } 
            if (this.sc.incoming.size() < Integer.parseInt(this.sc.tunnel.getProperty("channelsInMax", "1")) * -1) {
              this.sc.msg("Tunnel3:Fast incoming speed:" + (speed / 1024.0F) + "K/sec versus base:" + (this.baseSpeedIn / 1024.0F) + "K/sec, adding " + i + " channels.");
              for (int x = 0; x < i; x++)
                addIn(); 
            } 
            if (this.speedHistory.size() > 10 && speed > this.baseSpeedIn)
              this.baseSpeedIn = speed; 
          } 
          if (this.baseSpeedOut > 0.0F) {
            float speed = getSpeedAverage(this.speedHistory, "speedOut", "countOut", 10);
            if (speed / 1024.0F > this.minFastSpeed && speed > this.baseSpeedOut * this.speedThreshold) {
              if (this.sc.outgoing.size() < Math.abs(Integer.parseInt(this.sc.tunnel.getProperty("channelsOutMax", "1")))) {
                this.sc.msg("Tunnel3:Fast outgoing speed:" + (speed / 1024.0F) + "K/sec versus base:" + (this.baseSpeedOut / 1024.0F) + "K/sec, adding " + i + " channels.");
                for (int x = 0; x < i; x++)
                  addOut(); 
              } 
            } else if (speed / 1024.0F < this.minSlowSpeed && this.sc.outgoing.size() > 1) {
              if (Integer.parseInt(this.sc.tunnel.getProperty("channelsOutMax", "1")) >= 0) {
                this.sc.msg("Tunnel3:Slow outgoing speed:" + (speed / 1024.0F) + "K/sec versus base:" + (this.baseSpeedOut / 1024.0F) + "K/sec, removing channel.");
                for (int x = 0; x < i; x++)
                  removeOut(null); 
              } 
            } 
            if (this.sc.outgoing.size() < Integer.parseInt(this.sc.tunnel.getProperty("channelsOutMax", "1")) * -1) {
              this.sc.msg("Tunnel3:Fast outgoing speed:" + (speed / 1024.0F) + "K/sec versus base:" + (this.baseSpeedOut / 1024.0F) + "K/sec, adding " + i + " channels.");
              for (int x = 0; x < i; x++)
                addOut(); 
            } 
            if (this.speedHistory.size() > 10 && speed > this.baseSpeedOut)
              this.baseSpeedOut = speed; 
          } 
          if (this.sc.incoming.size() == 0) {
            addIn();
            if (this.noIncomingCount++ > 10) {
              (new Thread(new Runnable(this) {
                    final StreamTuner this$0;
                    
                    public void run() {
                      this.this$0.sc.msg("Tunnel3:Tunnel on server appears to be disconnecting us, resetting tunnel.");
                      this.this$0.sc.reset();
                    }
                  })).start();
              Thread.sleep(1000L);
              this.noIncomingCount = 0;
            } 
          } else {
            this.noIncomingCount = 0;
          } 
          if (this.sc.outgoing.size() == 0)
            addOut(); 
          Thread.sleep(1000L);
        } 
      } catch (Exception e) {
        this.sc.msg(e);
      } 
      try {
        Thread.sleep(5000L);
      } catch (Exception exception) {}
    } 
    while (this.sc.outgoing.size() > 1) {
      StreamWriter sw = this.sc.outgoing.remove(this.sc.outgoing.size() - 1);
      sw.close();
    } 
    int loops = 0;
    while (this.sc.incoming.size() > 0 && loops++ < 10) {
      removeIn(null);
      try {
        Thread.sleep(1000L);
      } catch (Exception exception) {}
      if (this.sc.outgoing.size() == 0)
        addOut(); 
    } 
    while (this.sc.outgoing.size() > 0) {
      StreamWriter sw = this.sc.outgoing.remove(this.sc.outgoing.size() - 1);
      sw.close();
    } 
    try {
      this.sc.startStopTunnel(false);
    } catch (Exception e) {
      this.sc.msg(e);
    } 
    this.active = false;
    this.sc.msg("Tunnel3:Tunnel shutdown.");
  }
  
  public boolean isActive() {
    return this.active;
  }
  
  public float getSpeedAverage(Vector v, String type1, String type2, int loops_max) {
    float average = 0.0F;
    int loops = 0;
    synchronized (v) {
      for (int x = v.size() - 1; x >= 0 && loops++ < loops_max; x--) {
        Properties p = v.elementAt(x);
        try {
          average += Float.parseFloat(p.getProperty(type1)) / Float.parseFloat(p.getProperty(type2));
        } catch (Exception exception) {}
      } 
    } 
    average /= (loops - 1);
    return average;
  }
  
  public void calcSpeed() {
    long bOut = 0L;
    long bIn = 0L;
    Properties bOut_ip = new Properties();
    Properties bIn_ip = new Properties();
    int x;
    for (x = 0; x < this.sc.outgoing.size(); x++) {
      StreamWriter sw = this.sc.outgoing.elementAt(x);
      long temp = sw.getTransferred();
      bOut += temp;
      bOut_ip.put(sw.getBindIp(), (new StringBuffer(String.valueOf(Long.parseLong(bOut_ip.getProperty(sw.getBindIp(), "0")) + temp))).toString());
      bOut_ip.put(String.valueOf(sw.getBindIp()) + "_count", (new StringBuffer(String.valueOf(Long.parseLong(bOut_ip.getProperty(String.valueOf(sw.getBindIp()) + "_count", "0")) + 1L))).toString());
      Vector by_ip = (Vector)this.speedHistoryIp.get(sw.getBindIp());
      if (by_ip == null)
        this.speedHistoryIp.put(sw.getBindIp(), new Vector()); 
    } 
    for (x = 0; x < this.sc.incoming.size(); x++) {
      StreamReader sr = this.sc.incoming.elementAt(x);
      long temp = sr.getTransferred();
      bIn += temp;
      bIn_ip.put(sr.getBindIp(), (new StringBuffer(String.valueOf(Long.parseLong(bIn_ip.getProperty(sr.getBindIp(), "0")) + temp))).toString());
      bIn_ip.put(String.valueOf(sr.getBindIp()) + "_count", (new StringBuffer(String.valueOf(Long.parseLong(bIn_ip.getProperty(String.valueOf(sr.getBindIp()) + "_count", "0")) + 1L))).toString());
      Vector by_ip = (Vector)this.speedHistoryIp.get(sr.getBindIp());
      if (by_ip == null)
        this.speedHistoryIp.put(sr.getBindIp(), new Vector()); 
    } 
    Properties p = new Properties();
    p.put("speedOut", (new StringBuffer(String.valueOf(bOut))).toString());
    p.put("speedIn", (new StringBuffer(String.valueOf(bIn))).toString());
    p.put("countOut", (new StringBuffer(String.valueOf(this.sc.outgoing.size()))).toString());
    p.put("countIn", (new StringBuffer(String.valueOf(this.sc.incoming.size()))).toString());
    this.speedHistory.addElement(p);
    synchronized (this.speedHistory) {
      while (this.speedHistory.size() > 60)
        this.speedHistory.remove(0); 
    } 
    synchronized (this.speedHistoryIp) {
      Enumeration keys = this.speedHistoryIp.keys();
      while (keys.hasMoreElements()) {
        String bind_ip = keys.nextElement().toString();
        Vector by_ip = (Vector)this.speedHistoryIp.get(bind_ip);
        p = new Properties();
        p.put("speedOut", bOut_ip.getProperty(bind_ip, "0"));
        p.put("speedIn", bIn_ip.getProperty(bind_ip, "0"));
        p.put("countOut", bOut_ip.getProperty(String.valueOf(bind_ip) + "_count", "0"));
        p.put("countIn", bIn_ip.getProperty(String.valueOf(bind_ip) + "_count", "0"));
        by_ip.addElement(p);
        while (by_ip.size() > 60)
          by_ip.remove(0); 
      } 
    } 
  }
  
  public void addOut() {
    if (this.sc.outgoing.size() >= Math.abs(Integer.parseInt(this.sc.tunnel.getProperty("channelsOutMax", "1"))))
      return; 
    StreamWriter sw = new StreamWriter(this.sc, null, Common.makeBoundary(11));
    this.sc.outgoing.addElement(sw);
    this.sc.msg("Tunnel3:Adding outgoing channel. incoming:" + this.sc.incoming.size() + " outgoing:" + this.sc.outgoing.size());
    (new Thread(sw)).start();
  }
  
  public void addIn() {
    if (this.sc.incoming.size() >= Math.abs(Integer.parseInt(this.sc.tunnel.getProperty("channelsInMax", "1"))))
      return; 
    StreamReader sr = new StreamReader(this.sc, null, Common.makeBoundary(11));
    this.sc.incoming.addElement(sr);
    this.sc.msg("Tunnel3:Adding incoming channel. incoming:" + this.sc.incoming.size() + " outgoing:" + this.sc.outgoing.size());
    (new Thread(sr)).start();
  }
  
  public void removeOut(StreamWriter sw) {
    if (sw != null) {
      this.sc.outgoing.removeElement(sw);
      this.sc.msg("Tunnel3:Removing outgoing channel. incoming:" + this.sc.incoming.size() + " outgoing:" + this.sc.outgoing.size());
    } else if (this.sc.outgoing.size() > 0) {
      sw = this.sc.outgoing.get(this.sc.outgoing.size() - 1);
      sw.close();
      this.sc.outgoing.removeElement(sw);
      this.sc.msg("Tunnel3:Request remove outgoing channel. incoming:" + this.sc.incoming.size() + " outgoing:" + this.sc.outgoing.size());
    } 
  }
  
  public void removeIn(StreamReader sr) {
    if (sr != null) {
      sr.close();
      this.sc.incoming.removeElement(sr);
      this.sc.msg("Tunnel3:Removing incoming channel. incoming:" + this.sc.incoming.size() + " outgoing:" + this.sc.outgoing.size());
    } else {
      sr = this.sc.incoming.get(this.sc.incoming.size() - 1);
      sr.close();
      this.sc.incoming.removeElement(sr);
      this.sc.msg("Tunnel3:Request remove incoming channel. incoming:" + this.sc.incoming.size() + " outgoing:" + this.sc.outgoing.size());
    } 
  }
}
