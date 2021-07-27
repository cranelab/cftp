package crushftp.handlers;

import com.crushftp.client.Common;
import crushftp.server.RETR_handler;
import crushftp.server.STOR_handler;
import crushftp.server.ServerStatus;
import java.util.Date;

public class TransferSpeedometer implements Runnable {
  SessionCrush theSession = null;
  
  RETR_handler retr = null;
  
  STOR_handler stor = null;
  
  int sleep_interval = 100;
  
  int samples = 5 * 1000 / this.sleep_interval;
  
  long[] rollingAmounts = new long[this.samples];
  
  long[] rollingAmountsTime = new long[this.samples];
  
  int rollingAmountsIndex = 0;
  
  long startByte = 0L;
  
  long startTime = 0L;
  
  String directionLabel = "";
  
  int max_speed = 0;
  
  long max_server_upload_speed = 0L;
  
  long max_server_download_speed = 0L;
  
  String the_dir = "";
  
  public boolean bandwidth_immune_server = false;
  
  public TransferSpeedometer(SessionCrush theSession, RETR_handler retr, STOR_handler stor) {
    this.theSession = theSession;
    this.retr = retr;
    this.stor = stor;
    this.startTime = theSession.uiLG("start_transfer_time");
    this.startByte = theSession.uiLG("start_transfer_byte_amount");
    reloadBandwidthLimits();
    if (stor != null)
      this.the_dir = stor.the_dir; 
    if (retr != null)
      this.the_dir = retr.the_dir; 
    for (int x = 0; x < this.rollingAmounts.length; x++) {
      this.rollingAmounts[x] = this.startByte;
      this.rollingAmountsTime[x] = this.startTime;
    } 
  }
  
  public void run() {
    try {
      Thread.currentThread().setName("TransferSpeedometer");
      reloadBandwidthLimits();
      while ((this.stor != null && this.stor.active2.getProperty("active", "").equals("true") && this.stor.the_dir.equals(this.the_dir)) || (this.retr != null && this.retr.active2.getProperty("active", "").equals("true") && this.retr.the_dir.equals(this.the_dir))) {
        Thread.sleep(this.sleep_interval);
        calcCurrent();
        calcOverall();
        String ip = this.theSession.uiSG("user_ip");
        if (!ServerStatus.BG("separate_speeds_by_username_ip"))
          ip = null; 
        if ((this.max_speed > 0 && ServerStatus.calc_server_speeds(this.theSession.uiSG("user_name"), ip) >= this.max_speed * 1.0D) || (this.stor != null && this.max_server_upload_speed > 0L && ServerStatus.calc_server_up_speeds(null, null) > this.max_server_upload_speed * 0.93D) || (this.retr != null && this.max_server_download_speed > 0L && ServerStatus.calc_server_down_speeds(null, null) > this.max_server_download_speed * 0.93D) || this.theSession.uiBG("pause_now")) {
          if (this.stor != null)
            this.stor.pause_transfer = true; 
          if (this.retr != null)
            this.retr.pause_transfer = true; 
          continue;
        } 
        if (this.stor != null)
          this.stor.pause_transfer = false; 
        if (this.retr != null)
          this.retr.pause_transfer = false; 
      } 
    } catch (Exception e) {
      Log.log("SERVER", 2, e);
    } 
  }
  
  public void reloadBandwidthLimits() {
    this.max_server_upload_speed = ServerStatus.IG("max_server_upload_speed");
    this.max_server_download_speed = ServerStatus.IG("max_server_download_speed");
    if (!ipLimited(this.theSession.uiSG("user_ip"), ServerStatus.SG("bandwidth_immune_ips"))) {
      this.max_server_download_speed = 0L;
      this.max_server_upload_speed = 0L;
      this.bandwidth_immune_server = true;
    } 
    if (this.stor != null) {
      this.directionLabel = "bytes_received";
      this.max_speed = this.theSession.IG("speed_limit_upload");
      if ((this.max_server_upload_speed < this.max_speed && this.max_server_upload_speed > 0L) || this.max_speed == 0)
        this.max_speed = (int)this.max_server_upload_speed; 
    } 
    if (this.retr != null) {
      this.directionLabel = "bytes_sent";
      this.max_speed = this.theSession.IG("speed_limit_download");
      if ((this.max_server_download_speed < this.max_speed && this.max_server_download_speed > 0L) || this.max_speed == 0)
        this.max_speed = (int)this.max_server_download_speed; 
    } 
    if (this.max_speed != 0)
      this.bandwidth_immune_server = false; 
  }
  
  public boolean ipLimited(String ip, String limited_ips) {
    String[] limited = limited_ips.split(",");
    for (int x = 0; x < limited.length; x++) {
      if (!limited[x].trim().equals("") && 
        Common.do_search(limited[x].trim(), ip, false, 0))
        return false; 
    } 
    return true;
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
    if (!this.bandwidth_immune_server)
      try {
        this.theSession.uiPUT("current_transfer_speed", (new StringBuffer(String.valueOf((int)speed))).toString());
      } catch (Exception e) {
        if (e.indexOf("Interrupted") >= 0)
          throw e; 
      }  
  }
  
  public void calcOverall() throws Exception {
    long now = (new Date()).getTime();
    this.rollingAmounts[this.rollingAmountsIndex] = this.theSession.uiLG(this.directionLabel);
    this.rollingAmountsTime[this.rollingAmountsIndex] = now;
    if (++this.rollingAmountsIndex == this.samples)
      this.rollingAmountsIndex = 0; 
    try {
      this.theSession.uiPUT("overall_transfer_speed", (new StringBuffer(String.valueOf((this.theSession.uiLG(this.directionLabel) - this.startByte) / (now - this.startTime) / 1000L / 1024L))).toString());
    } catch (Exception e) {
      if (e.indexOf("Interrupted") >= 0)
        throw e; 
    } 
    long speedForCalc = this.theSession.uiLG("current_transfer_speed");
    if (this.bandwidth_immune_server)
      speedForCalc = this.theSession.uiLG("overall_transfer_speed"); 
    try {
      if (this.retr != null) {
        this.theSession.uiPUT("seconds_remaining", (new StringBuffer(String.valueOf((this.theSession.uiLG("file_length") - this.retr.current_loc) / 1024L / speedForCalc))).toString());
      } else if (this.stor != null) {
        this.theSession.uiPUT("seconds_remaining", (new StringBuffer(String.valueOf((this.theSession.uiLG("file_length") - this.stor.current_loc) / 1024L / speedForCalc))).toString());
      } 
    } catch (Exception e) {
      if (e.indexOf("Interrupted") >= 0)
        throw e; 
    } 
  }
  
  public float getDelayAmount(int data_read, long startLoop, long endLoop, int packet_size, float slow_transfer, float speed_limit) {
    if ((endLoop - startLoop) < 1000.0D / (speed_limit * 1024.0F / data_read)) {
      slow_transfer = (float)(slow_transfer + 1000.0D / (this.max_speed * 1024.0F / data_read) - (endLoop - startLoop));
    } else {
      slow_transfer -= 100.0F;
    } 
    if (slow_transfer < 0.0F)
      slow_transfer = 0.0F; 
    if (slow_transfer > 10000.0F)
      slow_transfer = 500.0F; 
    return slow_transfer;
  }
}
