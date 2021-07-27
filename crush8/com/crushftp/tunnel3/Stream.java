package com.crushftp.tunnel3;

import com.crushftp.client.Common;
import com.crushftp.client.Worker;
import com.crushftp.tunnel2.Chunk;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class Stream implements Runnable {
  Socket sock = null;
  
  int id = -1;
  
  StreamController sc = null;
  
  Stream thisObj = this;
  
  int parent_stream_id = 0;
  
  int last_num = -1;
  
  int local_num = 1;
  
  int remote_num = 1;
  
  long last_activity = System.currentTimeMillis();
  
  OutputStream out = null;
  
  InputStream in = null;
  
  Properties tunnel = null;
  
  boolean remote_killed = false;
  
  boolean local_killed = false;
  
  public long ram_allocated_out = (1048576 * Integer.parseInt(System.getProperty("crushftp.tunnel_ram_cache", "1")));
  
  StringBuffer buffer_tuner_status = new StringBuffer();
  
  public Stream(Socket sock, int id, StreamController sc, Properties tunnel, int parent_stream_id) {
    this.sock = sock;
    this.id = id;
    this.sc = sc;
    this.tunnel = tunnel;
    this.parent_stream_id = parent_stream_id;
  }
  
  public void run() {
    StreamController.addRamAllocated(this.ram_allocated_out);
    try {
      Worker.startWorker(new Runnable(this) {
            final Stream this$0;
            
            public void run() {
              Thread.currentThread().setName("Tunnel3 stream " + this.this$0.id + " writer to " + this.this$0.sock);
              try {
                Stream parent_st = null;
                if (this.this$0.parent_stream_id > 0)
                  parent_st = (Stream)this.this$0.sc.streams.get((new StringBuffer(String.valueOf(this.this$0.parent_stream_id))).toString()); 
                Properties queue = (Properties)this.this$0.sc.in_queues.get((new StringBuffer(String.valueOf(this.this$0.id))).toString());
                this.this$0.out = this.this$0.sock.getOutputStream();
                long last_master_ack = System.currentTimeMillis();
                int last_ack_num = 0;
                long bytes_pending_ack = 0L;
                long ram_max_out_window = (1048576 * Integer.parseInt(this.this$0.tunnel.getProperty("sendBuffer", "1")));
                while ((this.this$0.remote_num < this.this$0.last_num || this.this$0.last_num < 0) && !this.this$0.sock.isClosed() && this.this$0.sc.isActive()) {
                  if (queue.containsKey((new StringBuffer(String.valueOf(this.this$0.remote_num))).toString())) {
                    Chunk c = (Chunk)queue.remove((new StringBuffer(String.valueOf(this.this$0.remote_num++))).toString());
                    bytes_pending_ack += c.len;
                    this.this$0.out.write(c.b, 0, c.len);
                    this.this$0.out.flush();
                    this.this$0.last_activity = System.currentTimeMillis();
                    if (parent_st != null)
                      parent_st.last_activity = this.this$0.last_activity; 
                  } else {
                    Thread.sleep(100L);
                  } 
                  if (System.currentTimeMillis() - last_master_ack > 1000L || bytes_pending_ack > ram_max_out_window) {
                    if (last_ack_num < this.this$0.remote_num)
                      this.this$0.sc.out_queue_commands.insertElementAt(this.this$0.sc.makeCommand(this.this$0.id, "A:M:" + this.this$0.remote_num), 0); 
                    last_ack_num = this.this$0.remote_num;
                    bytes_pending_ack = 0L;
                    last_master_ack = System.currentTimeMillis();
                  } 
                } 
                this.this$0.out.close();
                if (this.this$0.remote_num >= this.this$0.last_num)
                  this.this$0.remote_killed = true; 
              } catch (Exception e) {
                this.this$0.sc.msg(e);
                this.this$0.sc.out_queue_commands.insertElementAt(this.this$0.sc.makeCommand(this.this$0.id, "KILL:"), 0);
              } 
              this.this$0.sc.out_queue_commands.insertElementAt(this.this$0.sc.makeCommand(this.this$0.id, "A:M:" + this.this$0.remote_num), 0);
            }
          });
      Thread.currentThread().setName("Tunnel3 stream " + this.id + " reader from " + this.sock);
      this.sc.out_queues.put((new StringBuffer(String.valueOf(this.id))).toString(), new Vector());
      this.in = this.sock.getInputStream();
      this.sock.setSoTimeout(1000);
      int bytes_read = 0;
      Vector q = this.sc.getQueue((new StringBuffer(String.valueOf(this.id))).toString());
      Worker.startWorker(new Runnable(this) {
            final Stream this$0;
            
            public void run() {
              Thread.currentThread().setName("Tunnel3 buffer tuner " + this.this$0.id + " to " + this.this$0.sock);
              while (this.this$0.sc.isActive() && !this.this$0.sock.isClosed() && this.this$0.buffer_tuner_status.length() == 0) {
                if (this.this$0.ram_allocated_out < 67108864L)
                  if (StreamController.total_ram_used < StreamController.ram_max_total) {
                    int byte_speed = this.this$0.sc.getSpeedAndReset(this.this$0.id);
                    if (byte_speed > 524288 && byte_speed > this.this$0.ram_allocated_out / 2L) {
                      long new_bytes = this.this$0.ram_allocated_out;
                      if (this.this$0.ram_allocated_out > 14680064L)
                        new_bytes = 67108864L - this.this$0.ram_allocated_out; 
                      StreamController.addRamAllocated(new_bytes);
                      this.this$0.ram_allocated_out += new_bytes;
                      this.this$0.sc.msg("++++++++++++++Stream " + this.this$0.id + " max ram set to:" + Common.format_bytes_short(this.this$0.ram_allocated_out));
                    } 
                  }  
                try {
                  Thread.sleep(2000L);
                } catch (InterruptedException interruptedException) {}
              } 
            }
          });
      Stream parent_st = null;
      if (this.parent_stream_id > 0)
        parent_st = (Stream)this.sc.streams.get((new StringBuffer(String.valueOf(this.parent_stream_id))).toString()); 
      while (bytes_read >= 0 && this.sc.isActive() && !this.sock.isClosed()) {
        byte[] b = new byte[16384];
        bytes_read = -1;
        try {
          bytes_read = this.in.read(b);
          this.last_activity = System.currentTimeMillis();
          if (parent_st != null)
            parent_st.last_activity = this.last_activity; 
        } catch (SocketTimeoutException e) {
          bytes_read = 0;
        } 
        if (bytes_read > 0) {
          Chunk c = new Chunk(this.id, b, bytes_read, this.local_num++);
          if (c.len > 0)
            StreamController.addRam(this.id, c.len); 
          q.addElement(c);
          int loops = 0;
          while (StreamController.getRam(this.id) > this.ram_allocated_out && loops++ < 1000 && this.sc.isActive()) {
            if (loops == 100)
              this.sc.msg("Slowing " + this.id + " transfer due to full buffer: q.size=" + q.size() + " ram_used_out=" + StreamController.getRam(this.id) + " ram_allocated_out=" + this.ram_allocated_out + " sc.last_cache_ram=" + this.sc.last_cache_ram); 
            Thread.sleep(1L);
          } 
          loops = 0;
          while (q.size() > 20000 && loops++ < 60 && this.sc.isActive())
            Thread.sleep(1000L); 
          if (loops >= 60)
            this.sock.close(); 
          if (StreamController.getRam(this.id) > this.ram_allocated_out && this.sc.last_cache_ram < 1048576L)
            this.sc.msg("RAM " + this.id + " buffer full:" + Common.format_bytes_short(StreamController.getRam(this.id)) + " of " + Common.format_bytes_short(this.ram_allocated_out) + " with last localCache of " + Common.format_bytes_short(this.sc.last_cache_ram)); 
        } 
        if (System.currentTimeMillis() - this.last_activity > 600000L) {
          this.sock.close();
          this.sc.msg("Closing idle stream..." + this.sock + ":" + Thread.currentThread().getName());
        } 
      } 
    } catch (Exception e) {
      if (!this.sock.isClosed())
        this.sc.msg(e); 
    } finally {
      this.buffer_tuner_status.append("done");
      if (!this.remote_killed)
        this.sc.out_queue_commands.addElement(this.sc.makeCommand(this.id, "END:" + this.local_num)); 
      try {
        int loops = 0;
        while (this.remote_num < this.last_num || this.last_num < 0) {
          Thread.sleep(100L);
          if (loops++ > 100)
            break; 
        } 
        this.in.close();
        this.sock.close();
      } catch (Exception e) {
        this.sc.msg(e);
      } 
      this.sc.in_queues.remove((new StringBuffer(String.valueOf(this.id))).toString());
      this.sc.streams.remove((new StringBuffer(String.valueOf(this.id))).toString());
      Vector q = null;
      synchronized (this.sc.out_queue_remove) {
        q = (Vector)this.sc.out_queues.remove((new StringBuffer(String.valueOf(this.id))).toString());
      } 
      if (q != null && !this.local_killed) {
        this.sc.getQueue("unknown").addAll(q);
      } else if (q != null && this.local_killed) {
        while (q.size() > 0)
          q.remove(0); 
      } 
      Enumeration keys = this.sc.localCache.keys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement().toString();
        Chunk c2 = (Chunk)this.sc.localCache.get(key);
        if (c2 != null && c2.id == this.id)
          this.sc.localCache.remove(key); 
      } 
    } 
    StreamController.memory.remove((new StringBuffer(String.valueOf(this.id))).toString());
    this.sc.last_bytes_sent.remove((new StringBuffer(String.valueOf(this.id))).toString());
    this.sc.last_bytes_sent_time.remove((new StringBuffer(String.valueOf(this.id))).toString());
    StreamController.addRamAllocated(this.ram_allocated_out * -1L);
  }
  
  public void kill() throws IOException {
    this.local_killed = true;
    this.sock.close();
  }
}
