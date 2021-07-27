package com.crushftp.tunnel2;

import java.io.IOException;

public class Queue {
  public int id = 0;
  
  DProperties remote = null;
  
  boolean closedLocal = false;
  
  boolean closedRemote = false;
  
  Tunnel2 t = null;
  
  int remoteNum = 1;
  
  int localNum = 0;
  
  int max = -1;
  
  public Queue(Tunnel2 t, int id) {
    this.t = t;
    this.id = id;
    this.remote = new DProperties();
    t.addRemote(id, this.remote);
  }
  
  public void writeRemote(Chunk c) throws IOException {
    this.t.lastActivity = System.currentTimeMillis();
    if (c.isCommand()) {
      try {
        String command = c.getCommand();
        if (!command.startsWith("A:")) {
          if (command.indexOf("PING") < 0)
            Tunnel2.msg("Tunnel2:" + command); 
          if (command.startsWith("PINGREADY:")) {
            this.t.writeLocal(this.t.makeCommand(0, "PINGSEND:" + System.currentTimeMillis()), 0);
          } else if (command.startsWith("PINGSEND:")) {
            this.t.writeLocal(this.t.makeCommand(0, "PINGREPLY:" + command.split(":")[1]), 0);
          } else if (command.startsWith("PINGREPLY:")) {
            this.t.setPing((int)(System.currentTimeMillis() - Long.parseLong(command.split(":")[1].trim())));
          } else if (command.startsWith("CLOSEIN:")) {
            this.t.addWantClose();
          } else if (command.startsWith("END:")) {
            this.max = Integer.parseInt(command.split(":")[1].trim());
          } else if (command.startsWith("CONNECT:")) {
            this.t.connect(Integer.parseInt(command.split(":")[1]), command.split(":")[2], Integer.parseInt(command.split(":")[3]));
          } 
        } 
      } catch (Exception e) {
        Tunnel2.msg(e);
      } 
    } else {
      DProperties theRemote = this.t.getRemote(c.id);
      if (theRemote != null)
        theRemote.put((new StringBuffer(String.valueOf(c.num))).toString(), c); 
    } 
  }
  
  public Chunk readRemote() throws IOException {
    synchronized (this.remote) {
      if (this.remote.containsKey((new StringBuffer(String.valueOf(this.remoteNum))).toString())) {
        this.remoteNum++;
        return this.remote.remove((new StringBuffer(String.valueOf(this.remoteNum - 1))).toString());
      } 
      return null;
    } 
  }
  
  public synchronized void writeLocal(Chunk c, int i) throws IOException {
    if (c.num > this.localNum)
      this.localNum = c.num; 
    this.t.writeLocal(c, i);
  }
  
  public synchronized void closeLocal() throws Exception {
    writeLocal(this.t.makeCommand(this.id, "END:" + this.localNum), -1);
    this.closedLocal = true;
  }
  
  public boolean isClosedLocal() {
    return !(!this.closedLocal && this.t.isActive());
  }
  
  public void closeRemote() {
    this.closedRemote = true;
  }
  
  public boolean isClosedRemote() {
    return !(!this.closedRemote && this.t.isActive());
  }
  
  public void waitForClose(int secs) throws Exception {
    for (int x = 0; x < secs; x++) {
      Thread.sleep(1000L);
      if (isClosedRemote())
        break; 
    } 
    if (!isClosedRemote())
      closeRemote(); 
    this.remote.close();
  }
}
