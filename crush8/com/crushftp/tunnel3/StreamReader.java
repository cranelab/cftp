package com.crushftp.tunnel3;

import com.crushftp.client.HttpURLConnection;
import com.crushftp.tunnel2.Chunk;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Vector;

public class StreamReader implements Runnable {
  StreamController sc = null;
  
  HttpURLConnection urlc = null;
  
  InputStream in = null;
  
  String channel_id = null;
  
  long bytes = 0L;
  
  public Object byte_lock = new Object();
  
  Vector history = new Vector();
  
  public StreamReader(StreamController sc, HttpURLConnection urlc, String channel_id) {
    this.sc = sc;
    this.urlc = urlc;
    this.channel_id = channel_id;
  }
  
  public void run() {
    while (this.sc.isActive()) {
      Thread.currentThread().setName(this.sc + ":StreamReader:" + this.channel_id);
      boolean new_stream = false;
      if (this.urlc == null) {
        this.urlc = this.sc.addTransport(false, this.channel_id);
        new_stream = true;
      } 
      if (this.urlc == null)
        break; 
      this.urlc.setAllowPool(false);
      try {
        if (this.in == null)
          this.in = this.urlc.getInputStream(); 
        Chunk c = Chunk.parse(this.in);
        if (c != null) {
          synchronized (this.byte_lock) {
            this.bytes += c.len;
          } 
          if (processIncomingChunk(this.sc, c))
            break; 
        } else if (new_stream) {
          if (this.urlc.getResponseCode() == 302 && this.sc.isActive()) {
            this.sc.st.noIncomingCount += 100;
          } else {
            this.sc.st.noIncomingCount++;
          } 
        } else {
          throw new IOException("Channel stream closed:" + this.channel_id);
        } 
        this.sc.updateStats(c, this.channel_id, this.history, "read", this.sc.incoming.indexOf(this));
      } catch (Exception e) {
        this.sc.msg(e);
        break;
      } 
    } 
    try {
      this.urlc.disconnect();
    } catch (Exception exception) {}
    synchronized (this.sc.bind_lock) {
      if (this.urlc != null && this.urlc.getBindIp() != null)
        this.sc.in_binds.put(this.urlc.getBindIp(), (new StringBuffer(String.valueOf(Integer.parseInt(this.sc.in_binds.getProperty(this.urlc.getBindIp(), "1")) - 1))).toString()); 
    } 
    this.urlc = null;
    this.in = null;
    this.sc.incoming.remove(this);
    this.sc.stats.remove(String.valueOf(this.channel_id) + ":read");
  }
  
  public static boolean processIncomingChunk(StreamController sc, Chunk c) throws InterruptedException {
    if (c.isCommand()) {
      sc.msg("RECEIVED CMD :" + c);
      return sc.processCommand(c, (Stream)sc.streams.get((new StringBuffer(String.valueOf(c.id))).toString()));
    } 
    sc.last_receive_activity = System.currentTimeMillis();
    Properties queue = null;
    for (int x = 0; x < 500 && queue == null; x++) {
      queue = (Properties)sc.in_queues.get((new StringBuffer(String.valueOf(c.id))).toString());
      if (queue == null && sc.bad_queues.containsKey((new StringBuffer(String.valueOf(c.id))).toString()))
        break; 
      if (queue == null)
        Thread.sleep(10L); 
    } 
    if (queue != null) {
      queue.put((new StringBuffer(String.valueOf(c.num))).toString(), c);
    } else {
      if (sc.bad_queues.size() > 100)
        sc.bad_queues.clear(); 
      sc.bad_queues.put((new StringBuffer(String.valueOf(c.id))).toString(), "");
      sc.out_queue_commands.insertElementAt(sc.makeCommand(c.id, "A:" + c.num), 0);
    } 
    return false;
  }
  
  public void close() {
    this.sc.getQueue("unknown").insertElementAt(this.sc.makeCommand(0, "CLOSE:" + this.channel_id), 0);
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
}
