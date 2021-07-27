package com.sun.mail.imap;

import com.sun.mail.util.MailLogger;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;

public class IdleManager {
  private Executor es;
  
  private Selector selector;
  
  private MailLogger logger;
  
  private volatile boolean die = false;
  
  private volatile boolean running;
  
  private Queue<IMAPFolder> toWatch = new ConcurrentLinkedQueue<IMAPFolder>();
  
  private Queue<IMAPFolder> toAbort = new ConcurrentLinkedQueue<IMAPFolder>();
  
  public IdleManager(Session session, Executor es) throws IOException {
    this.es = es;
    this.logger = new MailLogger(getClass(), "DEBUG IMAP", session);
    this.selector = Selector.open();
    es.execute(new Runnable() {
          public void run() {
            IdleManager.this.logger.fine("IdleManager select starting");
            try {
              IdleManager.this.running = true;
              IdleManager.this.select();
            } finally {
              IdleManager.this.running = false;
              IdleManager.this.logger.fine("IdleManager select terminating");
            } 
          }
        });
  }
  
  public boolean isRunning() {
    return this.running;
  }
  
  public void watch(Folder folder) throws MessagingException {
    if (this.die)
      throw new MessagingException("IdleManager is not running"); 
    if (!(folder instanceof IMAPFolder))
      throw new MessagingException("Can only watch IMAP folders"); 
    IMAPFolder ifolder = (IMAPFolder)folder;
    SocketChannel sc = ifolder.getChannel();
    if (sc == null)
      throw new MessagingException("Folder is not using SocketChannels"); 
    if (this.logger.isLoggable(Level.FINEST))
      this.logger.log(Level.FINEST, "IdleManager watching {0}", 
          folderName(ifolder)); 
    int tries = 0;
    while (!ifolder.startIdle(this)) {
      if (this.logger.isLoggable(Level.FINEST))
        this.logger.log(Level.FINEST, "IdleManager.watch startIdle failed for {0}", 
            
            folderName(ifolder)); 
      tries++;
    } 
    if (this.logger.isLoggable(Level.FINEST))
      if (tries > 0) {
        this.logger.log(Level.FINEST, "IdleManager.watch startIdle succeeded for {0} after " + tries + " tries", 

            
            folderName(ifolder));
      } else {
        this.logger.log(Level.FINEST, "IdleManager.watch startIdle succeeded for {0}", 
            
            folderName(ifolder));
      }  
    synchronized (this) {
      this.toWatch.add(ifolder);
      this.selector.wakeup();
    } 
  }
  
  void requestAbort(IMAPFolder folder) {
    this.toAbort.add(folder);
    this.selector.wakeup();
  }
  
  private void select() {
    this.die = false;
    try {
      label39: while (true) {
        try {
          if (!this.die) {
            watchAll();
            this.logger.finest("IdleManager waiting...");
            int ns = this.selector.select();
            if (this.logger.isLoggable(Level.FINEST))
              this.logger.log(Level.FINEST, "IdleManager selected {0} channels", 
                  Integer.valueOf(ns)); 
            if (!this.die && !Thread.currentThread().isInterrupted()) {
              while (true) {
                processKeys();
                if (this.selector.selectNow() <= 0 && this.toAbort.isEmpty())
                  continue label39; 
              } 
              break;
            } 
          } 
        } catch (InterruptedIOException ex) {
          this.logger.log(Level.FINEST, "IdleManager interrupted", ex);
        } catch (IOException ex) {
          this.logger.log(Level.FINEST, "IdleManager got I/O exception", ex);
        } catch (Exception ex) {
          this.logger.log(Level.FINEST, "IdleManager got exception", ex);
        } 
        return;
      } 
    } finally {
      this.die = true;
      this.logger.finest("IdleManager unwatchAll");
      try {
        unwatchAll();
        this.selector.close();
      } catch (IOException ex2) {
        this.logger.log(Level.FINEST, "IdleManager unwatch exception", ex2);
      } 
      this.logger.fine("IdleManager exiting");
    } 
  }
  
  private void watchAll() {
    IMAPFolder folder;
    while ((folder = this.toWatch.poll()) != null) {
      if (this.logger.isLoggable(Level.FINEST))
        this.logger.log(Level.FINEST, "IdleManager adding {0} to selector", 
            folderName(folder)); 
      try {
        SocketChannel sc = folder.getChannel();
        if (sc == null)
          continue; 
        sc.configureBlocking(false);
        sc.register(this.selector, 1, folder);
      } catch (IOException ex) {
        this.logger.log(Level.FINEST, "IdleManager can't register folder", ex);
      } catch (CancelledKeyException ex) {
        this.logger.log(Level.FINEST, "IdleManager can't register folder", ex);
      } 
    } 
  }
  
  private void processKeys() throws IOException {
    Set<SelectionKey> selectedKeys = this.selector.selectedKeys();
    Iterator<SelectionKey> it = selectedKeys.iterator();
    while (it.hasNext()) {
      SelectionKey sk = it.next();
      it.remove();
      sk.cancel();
      IMAPFolder iMAPFolder = (IMAPFolder)sk.attachment();
      if (this.logger.isLoggable(Level.FINEST))
        this.logger.log(Level.FINEST, "IdleManager selected folder: {0}", 
            folderName(iMAPFolder)); 
      SelectableChannel sc = sk.channel();
      sc.configureBlocking(true);
      try {
        if (iMAPFolder.handleIdle(false)) {
          if (this.logger.isLoggable(Level.FINEST))
            this.logger.log(Level.FINEST, "IdleManager continue watching folder {0}", 
                
                folderName(iMAPFolder)); 
          this.toWatch.add(iMAPFolder);
          continue;
        } 
        if (this.logger.isLoggable(Level.FINEST))
          this.logger.log(Level.FINEST, "IdleManager done watching folder {0}", 
              
              folderName(iMAPFolder)); 
      } catch (MessagingException ex) {
        this.logger.log(Level.FINEST, "IdleManager got exception for folder: " + 
            
            folderName(iMAPFolder), ex);
      } 
    } 
    IMAPFolder folder;
    while ((folder = this.toAbort.poll()) != null) {
      if (this.logger.isLoggable(Level.FINEST))
        this.logger.log(Level.FINEST, "IdleManager aborting IDLE for folder: {0}", 
            
            folderName(folder)); 
      SocketChannel sc = folder.getChannel();
      if (sc == null)
        continue; 
      SelectionKey sk = sc.keyFor(this.selector);
      if (sk != null)
        sk.cancel(); 
      sc.configureBlocking(true);
      Socket sock = sc.socket();
      if (sock != null && sock.getSoTimeout() > 0) {
        this.logger.finest("IdleManager requesting DONE with timeout");
        this.toWatch.remove(folder);
        final IMAPFolder folder0 = folder;
        this.es.execute(new Runnable() {
              public void run() {
                folder0.idleAbortWait();
              }
            });
        continue;
      } 
      folder.idleAbort();
      this.toWatch.add(folder);
    } 
  }
  
  private void unwatchAll() {
    Set<SelectionKey> keys = this.selector.keys();
    for (SelectionKey sk : keys) {
      sk.cancel();
      IMAPFolder iMAPFolder = (IMAPFolder)sk.attachment();
      if (this.logger.isLoggable(Level.FINEST))
        this.logger.log(Level.FINEST, "IdleManager no longer watching folder: {0}", 
            
            folderName(iMAPFolder)); 
      SelectableChannel sc = sk.channel();
      try {
        sc.configureBlocking(true);
        iMAPFolder.idleAbortWait();
      } catch (IOException ex) {
        this.logger.log(Level.FINEST, "IdleManager exception while aborting idle for folder: " + 
            
            folderName(iMAPFolder), ex);
      } 
    } 
    IMAPFolder folder;
    while ((folder = this.toWatch.poll()) != null) {
      if (this.logger.isLoggable(Level.FINEST))
        this.logger.log(Level.FINEST, "IdleManager aborting IDLE for unwatched folder: {0}", 
            
            folderName(folder)); 
      SocketChannel sc = folder.getChannel();
      if (sc == null)
        continue; 
      try {
        sc.configureBlocking(true);
        folder.idleAbortWait();
      } catch (IOException ex) {
        this.logger.log(Level.FINEST, "IdleManager exception while aborting idle for folder: " + 
            
            folderName(folder), ex);
      } 
    } 
  }
  
  public synchronized void stop() {
    this.die = true;
    this.logger.fine("IdleManager stopping");
    this.selector.wakeup();
  }
  
  private static String folderName(Folder folder) {
    try {
      return folder.getURLName().toString();
    } catch (MessagingException mex) {
      return folder.getStore().toString() + "/" + folder.toString();
    } 
  }
}
