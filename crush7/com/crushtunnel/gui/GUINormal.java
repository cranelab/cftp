package com.crushtunnel.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class GUINormal extends JPanel {
  private static final long serialVersionUID = 1L;
  
  Properties p = null;
  
  GUI gui = null;
  
  public GUINormal(Properties p, GUI gui) {
    this.p = p;
    this.gui = gui;
    try {
      jbInit();
    } catch (Exception e) {
      e.printStackTrace();
    } 
  }
  
  private void jbInit() throws Exception {
    JLabel tunnelInfoLabel = new JLabel();
    JButton toggleTunnelButton = new JButton();
    JLabel statusLabel = new JLabel();
    JLabel infoLabel = new JLabel("");
    Properties guiItem = new Properties();
    guiItem.put("tunnelItemPanel", this);
    guiItem.put("tunnelInfoLabel", tunnelInfoLabel);
    guiItem.put("toggleTunnelButton", toggleTunnelButton);
    guiItem.put("statusLabel", statusLabel);
    guiItem.put("infoLabel", infoLabel);
    this.p.put("gui", guiItem);
    setBorder(BorderFactory.createEtchedBorder());
    setPreferredSize(new Dimension(600, 170));
    tunnelInfoLabel.setPreferredSize(new Dimension(350, 30));
    tunnelInfoLabel.setText(this.p.getProperty("name", ""));
    toggleTunnelButton.setText(this.p.getProperty("buttonConnect", ""));
    toggleTunnelButton.setPreferredSize(new Dimension(150, 30));
    toggleTunnelButton.addActionListener(new ActionListener(this) {
          final GUINormal this$0;
          
          public void actionPerformed(ActionEvent e) {
            this.this$0.gui.startStopTunnel(this.this$0.p);
          }
        });
    statusLabel.setText(this.p.getProperty("localPort", "0"));
    statusLabel.setPreferredSize(new Dimension(65, 30));
    try {
      statusLabel.setIcon(new ImageIcon(getClass().getResource("/assets/red.gif")));
    } catch (Exception exception) {}
    statusLabel.setFont(new Font("Arial", 0, 10));
    add(tunnelInfoLabel, (Object)null);
    add(toggleTunnelButton, (Object)null);
    add(statusLabel, (Object)null);
    infoLabel.setHorizontalAlignment(2);
    infoLabel.setPreferredSize(new Dimension(600, 100));
    add(infoLabel);
    setVisible(true);
  }
}
