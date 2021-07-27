package com.crushftp.tunnel;

import java.util.Date;

public class TransferSpeedometer implements Runnable {
  int sleep_interval = 100;
  
  int samples = 5 * 1000 / this.sleep_interval;
  
  long[] rollingAmounts = new long[this.samples];
  
  long[] rollingAmountsTime = new long[this.samples];
  
  int rollingAmountsIndex = 0;
  
  long startByte = 0L;
  
  long startTime = 0L;
  
  String directionLabel = "";
  
  long max_speed = 0L;
  
  long size = 0L;
  
  long current_transfer_speed = 0L;
  
  long overall_transfer_speed = 0L;
  
  long seconds_remaining = 0L;
  
  public long current_loc = 0L;
  
  String the_dir = "";
  
  StringBuffer pause = null;
  
  public TransferSpeedometer(long max_speed, long size, StringBuffer pause) {
    this.max_speed = max_speed;
    this.size = size;
    this.pause = pause;
    this.startTime = (new Date()).getTime();
    this.startByte = 0L;
    for (int x = 0; x < this.rollingAmounts.length; x++) {
      this.rollingAmounts[x] = this.startByte;
      this.rollingAmountsTime[x] = this.startTime;
    } 
  }
  
  public void run() {
    try {
      Thread.currentThread().setName("TransferSpeedometer");
      while (true) {
        Thread.sleep(this.sleep_interval);
        calcOverall();
        calcCurrent();
        if (this.max_speed > 0L && this.current_transfer_speed >= this.max_speed * 1.0D) {
          this.pause.setLength(0);
          this.pause.append("true");
          continue;
        } 
        this.pause.setLength(0);
      } 
    } catch (Exception e) {
      e.printStackTrace();
      return;
    } 
  }
  
  public void calcCurrent() throws Exception {
    int rollingAmountsIndexStart = this.rollingAmountsIndex - this.samples - 1;
    int rollingAmountsIndexEnd = this.rollingAmountsIndex - 1;
    if (rollingAmountsIndexStart < 0)
      rollingAmountsIndexStart = this.samples + rollingAmountsIndexStart; 
    if (rollingAmountsIndexEnd < 0)
      rollingAmountsIndexEnd = this.samples + rollingAmountsIndexEnd; 
    long bytes = this.rollingAmounts[rollingAmountsIndexEnd] - this.rollingAmounts[rollingAmountsIndexStart];
    long time = this.rollingAmountsTime[rollingAmountsIndexEnd] - this.rollingAmountsTime[rollingAmountsIndexStart];
    float speed = (float)bytes / (float)time;
    try {
      this.current_transfer_speed = (int)speed;
    } catch (Exception e) {
      if (e.indexOf("Interrupted") >= 0)
        throw e; 
    } 
  }
  
  public void calcOverall() throws Exception {
    long now = (new Date()).getTime();
    this.rollingAmounts[this.rollingAmountsIndex] = this.current_loc;
    this.rollingAmountsTime[this.rollingAmountsIndex] = now;
    if (++this.rollingAmountsIndex == this.samples)
      this.rollingAmountsIndex = 0; 
    try {
      this.overall_transfer_speed = (this.current_loc - this.startByte) / (now - this.startTime) / 1000L / 1024L;
    } catch (Exception e) {
      if (e.indexOf("Interrupted") >= 0)
        throw e; 
    } 
    try {
      this.seconds_remaining = (this.size - this.current_loc) / 1024L / this.current_transfer_speed;
    } catch (Exception e) {
      if (e.indexOf("Interrupted") >= 0)
        throw e; 
    } 
  }
  
  public static float getDelayAmount(int data_read, long startLoop, long endLoop, float slow_transfer, float speed_limit) {
    if (speed_limit == 0.0F || data_read == 0)
      return 0.0F; 
    if ((endLoop - startLoop) < 1000.0D / (speed_limit / data_read)) {
      slow_transfer = (float)(slow_transfer + 1000.0D / (speed_limit / data_read) - (endLoop - startLoop));
    } else {
      slow_transfer = (float)(slow_transfer - slow_transfer * 0.1D);
    } 
    if (slow_transfer < 0.0F)
      slow_transfer = 0.0F; 
    if (slow_transfer > 10000.0F)
      slow_transfer = 500.0F; 
    return slow_transfer;
  }
}
