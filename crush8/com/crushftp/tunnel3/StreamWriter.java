package com.crushftp.tunnel3;

import com.crushftp.client.HttpURLConnection;
import com.crushftp.tunnel2.Chunk;
import java.io.OutputStream;
import java.util.Vector;

public class StreamWriter implements Runnable {
  StreamController sc = null;
  
  HttpURLConnection urlc = null;
  
  OutputStream out = null;
  
  public boolean close = false;
  
  String channel_id = null;
  
  long bytes = 0L;
  
  public Object byte_lock = new Object();
  
  public long last_write = System.currentTimeMillis();
  
  Vector history = new Vector();
  
  public StreamWriter(StreamController sc, HttpURLConnection urlc, String channel_id) {
    this.sc = sc;
    this.urlc = urlc;
    this.channel_id = channel_id;
  }
  
  public void run() {
    Chunk c = null;
    while (!this.close && this.sc.isActive()) {
      Thread.currentThread().setName(this.sc + ":StreamWriter:" + this.channel_id);
      if (this.urlc == null)
        this.urlc = this.sc.addTransport(true, this.channel_id); 
      if (this.urlc == null)
        break; 
      this.urlc.setAllowPool(false);
      try {
        if (this.out == null)
          this.out = this.urlc.getOutputStream(); 
        if (this.close)
          break; 
        c = processOutgoingChunk();
        if (this.close)
          break; 
        if (c != null) {
          if (c.isCommand())
            this.sc.msg("SENDING CMD :" + c); 
          c.sw = this;
          this.out.write(c.toBytes());
          this.out.flush();
          synchronized (this.byte_lock) {
            this.bytes += c.len;
          } 
          this.sc.addBytes(c.id, c.len);
          this.last_write = System.currentTimeMillis();
          this.sc.updateStats(c, this.channel_id, this.history, "write", this.sc.outgoing.indexOf(this));
          this.sc.last_send_activity = System.currentTimeMillis();
        } else {
          Thread.sleep(100L);
        } 
        if (this.close)
          break; 
      } catch (Exception e) {
        if (c != null && !c.getCommand().startsWith("PING")) {
          c.time = System.currentTimeMillis();
          this.sc.localCache.remove(String.valueOf(c.id) + ":" + c.num);
          if (c.isCommand()) {
            this.sc.out_queue_commands.insertElementAt(c, 0);
          } else {
            this.sc.getQueue((new StringBuffer(String.valueOf(c.id))).toString()).insertElementAt(c, 0);
          } 
        } 
        this.sc.msg(e);
        reset();
      } 
    } 
    this.sc.outgoing.remove(this);
    reset();
    this.sc.stats.remove(String.valueOf(this.channel_id) + ":write");
  }
  
  public void close() {
    this.close = true;
  }
  
  private void reset() {
    try {
      this.out.close();
    } catch (Exception exception) {}
    try {
      this.urlc.disconnect();
    } catch (Exception exception) {}
    try {
      synchronized (this.sc.bind_lock) {
        if (this.urlc != null && this.urlc.getBindIp() != null)
          this.sc.out_binds.put(this.urlc.getBindIp(), (new StringBuffer(String.valueOf(Integer.parseInt(this.sc.out_binds.getProperty(this.urlc.getBindIp(), "1")) - 1))).toString()); 
      } 
    } catch (Exception e) {
      this.sc.msg(e);
    } 
    this.urlc = null;
    this.out = null;
  }
  
  public long getTransferred() {
    synchronized (this.byte_lock) {
      long bytes2 = this.bytes;
      this.bytes = 0L;
      return bytes2;
    } 
  }
  
  public String getBindIp() {
    if (this.urlc != null)
      return this.urlc.getBindIp(); 
    return "0.0.0.0";
  }
  
  public Chunk processOutgoingChunk() throws InterruptedException {
    Chunk c = null;
    if (this.sc == null)
      return null; 
    if (this.sc.closeRequests.containsKey(this.channel_id)) {
      c = this.sc.makeCommand(0, "CLOSE:" + this.channel_id);
    } else {
      synchronized (this.sc.out_queue_remove) {
        c = null;
        int loops = 0;
        int queue_count = this.sc.getQueueCount();
        while (queue_count == 0 && this.sc.out_queue_commands.size() == 0 && c == null && !this.close) {
          Thread.sleep(100L);
          if (loops++ > 10)
            break; 
          if (System.currentTimeMillis() - this.last_write > 10000L)
            c = this.sc.makeCommand(0, "PINGSEND:" + System.currentTimeMillis()); 
          queue_count = this.sc.getQueueCount();
        } 
        if (this.close)
          return null; 
        if (this.sc.out_queue_commands.size() > 0 && c == null) {
          c = this.sc.out_queue_commands.remove(0);
          if (!c.getCommand().startsWith("A:"))
            this.sc.localCache.put(String.valueOf(c.id) + ":" + c.num, c); 
        } 
        if (queue_count > 0 && c == null) {
          c = this.sc.popOut();
          if (c != null)
            this.sc.localCache.put(String.valueOf(c.id) + ":" + c.num, c); 
        } 
        if (c != null)
          c.time = System.currentTimeMillis(); 
      } 
    } 
    return c;
  }
}
