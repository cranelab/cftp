package com.crushtunnel.gui;

import com.crushftp.client.Common;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.net.URI;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class GUIFrame extends JFrame {
  private static final long serialVersionUID = 1L;
  
  public static MenuItem itemStatus = new MenuItem();
  
  public TrayIcon trayIcon = null;
  
  public static GUIFrame thisObj = null;
  
  public GUIFrame() {
    GUI gui = new GUI();
    thisObj = this;
    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(gui, "Center");
    setDefaultCloseOperation(1);
    setSize(new Dimension(1050, 576));
    if (Common.machine_is_x() || Common.machine_is_windows()) {
      setVisible(false);
    } else {
      setVisible(true);
    } 
    setTitle("Tunnels");
    setupSysTray();
  }
  
  public static void main(String[] args) {
    if (args != null && args.length > 0 && args[0].startsWith("appname")) {
      String[] args2 = Common.url_decode(args[0]).split(":::");
      for (int x = 0; x < args2.length; x++) {
        String key = args2[x].substring(0, args2[x].indexOf("="));
        String val = args2[x].substring(args2[x].indexOf("=") + 1);
        System.getProperties().put("crushtunnel." + key, val);
      } 
    } 
  }
  
  public void setupSysTray() {
    PopupMenu popup = new PopupMenu();
    try {
      BufferedImage image = ImageIO.read(getClass().getResourceAsStream("/com/crushtunnel/gui/icon.png"));
      BufferedImage resizedImage = new BufferedImage(20, 20, (image.getType() == 0) ? 2 : image.getType());
      Graphics2D g = resizedImage.createGraphics();
      g.drawImage(image, 0, 0, 20, 20, null);
      g.dispose();
      this.trayIcon = new TrayIcon(resizedImage, System.getProperty("crushtunnel.appname", "Tunnel"));
      this.trayIcon.setImageAutoSize(true);
      SystemTray tray = SystemTray.getSystemTray();
      this.trayIcon.setPopupMenu(popup);
      this.trayIcon.addMouseListener(new MouseListener(this) {
            final GUIFrame this$0;
            
            public void mouseClicked(MouseEvent e) {}
            
            public void mouseEntered(MouseEvent e) {}
            
            public void mouseExited(MouseEvent e) {}
            
            public void mousePressed(MouseEvent e) {}
            
            public void mouseReleased(MouseEvent e) {}
          });
      tray.add(this.trayIcon);
    } catch (Exception e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(null, e);
    } 
    try {
      if (Common.machine_is_windows()) {
        addMenuItem(popup, l("Exit"), "exit");
      } else {
        addMenuItem(popup, l("Quit"), "exit");
      } 
      addMenuItem(popup, l("About..."), "about");
      addMenuItem(popup, l("Launch browser..."), "launch");
      addMenuItem(popup, l("Tunnels"), "tunnels");
      itemStatus = addMenuItem(popup, l("Status"), "status");
      itemStatus.setEnabled(false);
    } catch (Exception e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(null, e);
    } 
  }
  
  public void showMessage(String s) {
    if (this.trayIcon != null)
      this.trayIcon.displayMessage(System.getProperty("crushtunnel.appname", "Tunnel"), s, TrayIcon.MessageType.INFO); 
    Common.activateFront();
  }
  
  public void menuItemSelected(String action) {
    if (action.equals("exit")) {
      System.exit(0);
    } else if (action.equals("about")) {
      JOptionPane.showMessageDialog(null, String.valueOf(System.getProperty("crushtunnel.appname", "Tunnel")) + " Version : " + "3.1.16");
    } else if (action.equals("tunnels")) {
      setVisible(false);
      Common.activateFront();
      setVisible(true);
    } else if (action.equals("launch")) {
      try {
        Desktop.getDesktop().browse(new URI("http://127.0.0.1:" + GUI.default_local_port + "/"));
      } catch (Exception e) {
        e.printStackTrace();
      } 
    } else {
      setVisible(true);
    } 
  }
  
  public MenuItem addMenuItem(PopupMenu popup, String label, String action) {
    MenuItem mi = new MenuItem(label);
    popup.insert(mi, 0);
    mi.addActionListener(new ActionListener(this, action) {
          final GUIFrame this$0;
          
          private final String val$action;
          
          public void actionPerformed(ActionEvent e) {
            this.this$0.menuItemSelected(this.val$action);
          }
        });
    return mi;
  }
  
  public static String l(String key) {
    String s = System.getProperties().getProperty("crushtunnel.localization." + key, key);
    s = Common.replace_str(s, "%appname%", System.getProperty("crushtunnel.appname", "Tunnel"));
    return s;
  }
}
