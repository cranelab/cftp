package crushftp.handlers;

import crushftp.gui.LOC;
import crushftp.server.ServerStatus;
import java.io.File;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class Mailer {
  public static String send_mail(String server_ip, String to_user, String cc_user, String bcc_user, String from_user, String subject, String body, String smtp_server, String smtp_user, String smtp_pass, boolean smtp_ssl, boolean html, File[] attachments) {
    smtp_pass = ServerStatus.thisObj.common_code.decode_pass(smtp_pass);
    String plain_body = body;
    if (html && (body.indexOf("\r") >= 0 || body.indexOf("\n") >= 0) && body.toUpperCase().indexOf("<BR") < 0) {
      body = Common.replace_str(body, "\r\n", "<br/>");
      body = Common.replace_str(body, "\r", "<br/>");
      body = Common.replace_str(body, "\n", "<br/>");
      if (!body.toUpperCase().startsWith("<HTML>") && body.toUpperCase().indexOf("<BODY") < 0) {
        body = "<HTML><BODY>" + body + "</BODY></HTML>";
      } else if (!body.toUpperCase().startsWith("<HTML>")) {
        body = "<HTML>" + body + "</BODY></HTML>";
      } 
    } 
    if (html && body.indexOf("<") >= 0 && body.toUpperCase().indexOf("<HTML") < 0)
      body = "<html><body>" + body + "</body></html>"; 
    String resultMessage = "";
    int smtp_port = 25;
    try {
      Vector toList = new Vector();
      Vector ccList = new Vector();
      Vector bccList = new Vector();
      if (to_user != null && !to_user.trim().equals(""))
        emailParser(to_user, toList); 
      if (cc_user != null && !cc_user.trim().equals(""))
        emailParser(cc_user, ccList); 
      if (bcc_user != null && !bcc_user.trim().equals(""))
        emailParser(bcc_user, bccList); 
      if (smtp_server.indexOf(":") >= 0) {
        try {
          smtp_port = Integer.parseInt(smtp_server.substring(smtp_server.indexOf(":") + 1));
        } catch (Exception exception) {}
        smtp_server = smtp_server.substring(0, smtp_server.indexOf(":"));
      } 
      Properties props = System.getProperties();
      props.put("mail.transport.protocol", "smtp");
      props.put("mail.smtp.host", smtp_server);
      props.put("mail.smtp.port", (new StringBuffer(String.valueOf(smtp_port))).toString());
      props.put("mail.smtp.auth", "false");
      props.put("mail.smtp.sasl.enable", "true");
      if (ServerStatus.IG("log_debug_level") >= 1)
        props.put("mail.debug", "true"); 
      props.put("mail.smtp.ssl.socketFactory", new UnsafeSSLSocketFactory());
      if (!ServerStatus.SG("smtp_helo_ip").equals(""))
        props.put("mail.smtp.localhost", ServerStatus.SG("smtp_helo_ip")); 
      Session session = null;
      if (!smtp_user.trim().equals("")) {
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.socketFactory.port", (new StringBuffer(String.valueOf(smtp_port))).toString());
        if (smtp_port == 465)
          props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); 
        if (smtp_ssl)
          props.put("mail.smtp.starttls.enable", "true"); 
        Authenticator auth = new Mailer$1$SMTPAuthenticator(smtp_user, smtp_pass);
        session = Session.getDefaultInstance(props, auth);
      } else {
        session = Session.getInstance(props, null);
      } 
      MimeMessage msg = new MimeMessage(session);
      InternetAddress[] to = new InternetAddress[toList.size()];
      for (int x = 0; x < toList.size(); x++)
        to[x] = new InternetAddress(toList.elementAt(x).toString().trim()); 
      InternetAddress[] cc = new InternetAddress[ccList.size()];
      for (int i = 0; i < ccList.size(); i++)
        cc[i] = new InternetAddress(ccList.elementAt(i).toString().trim()); 
      InternetAddress[] bcc = new InternetAddress[bccList.size()];
      for (int j = 0; j < bccList.size(); j++)
        bcc[j] = new InternetAddress(bccList.elementAt(j).toString().trim()); 
      InternetAddress from = new InternetAddress(from_user.trim());
      if (html) {
        MimeBodyPart textpart = new MimeBodyPart();
        textpart.setText(plain_body, "UTF-8");
        textpart.addHeaderLine("Content-Type: text/plain; charset=\"UTF-8\"");
        textpart.addHeaderLine("Content-Transfer-Encoding: quoted-printable");
        MimeBodyPart htmlpart = new MimeBodyPart();
        htmlpart.setText(body, "UTF-8");
        htmlpart.addHeaderLine("Content-Type: text/html; charset=\"UTF-8\"");
        htmlpart.addHeaderLine("Content-Transfer-Encoding: quoted-printable");
        Vector moreItems = new Vector();
        if (attachments != null)
          for (int k = 0; k < attachments.length; k++) {
            File attachment = attachments[k];
            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            FileDataSource fileDataSource = new FileDataSource(attachment);
            mimeBodyPart.setDataHandler(new DataHandler((DataSource)fileDataSource));
            mimeBodyPart.setFileName(attachment.getName());
            moreItems.addElement(mimeBodyPart);
          }  
        if (moreItems.size() > 0) {
          MimeMultipart mimeMultipart = new MimeMultipart("mixed");
          mimeMultipart.addBodyPart((BodyPart)htmlpart);
          for (int k = 0; k < moreItems.size(); k++)
            mimeMultipart.addBodyPart((BodyPart)moreItems.elementAt(k)); 
          msg.setContent((Multipart)mimeMultipart);
        } else {
          MimeMultipart mimeMultipart = new MimeMultipart("alternative");
          mimeMultipart.addBodyPart((BodyPart)textpart);
          mimeMultipart.addBodyPart((BodyPart)htmlpart);
          msg.setContent((Multipart)mimeMultipart);
        } 
      } else if (attachments != null) {
        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setText(body);
        MimeMultipart mimeMultipart = new MimeMultipart();
        mimeMultipart.addBodyPart((BodyPart)mimeBodyPart);
        for (int k = 0; k < attachments.length; k++) {
          File attachment = attachments[k];
          mimeBodyPart = new MimeBodyPart();
          FileDataSource fileDataSource = new FileDataSource(attachment);
          mimeBodyPart.setDataHandler(new DataHandler((DataSource)fileDataSource));
          mimeBodyPart.setFileName(attachment.getName());
          mimeMultipart.addBodyPart((BodyPart)mimeBodyPart);
        } 
        msg.setContent((Multipart)mimeMultipart);
      } else {
        msg.setText(body, "UTF-8");
      } 
      msg.setFrom((Address)from);
      if (to.length > 0)
        msg.setRecipients(MimeMessage.RecipientType.TO, (Address[])to); 
      if (cc.length > 0)
        msg.setRecipients(MimeMessage.RecipientType.CC, (Address[])cc); 
      if (bcc.length > 0)
        msg.setRecipients(MimeMessage.RecipientType.BCC, (Address[])bcc); 
      if (ServerStatus.BG("smtp_subject_utf8")) {
        msg.setSubject(subject, "UTF8");
      } else {
        msg.setSubject(subject);
      } 
      msg.setSentDate(new Date());
      msg.saveChanges();
      Transport transport = session.getTransport("smtp");
      if (smtp_user.trim().equals("")) {
        transport.connect();
      } else {
        transport.connect(smtp_user, smtp_pass);
      } 
      transport.sendMessage((Message)msg, msg.getAllRecipients());
      resultMessage = "Success!";
      try {
        transport.close();
      } catch (Exception exception) {}
    } catch (Exception e) {
      Log.log("SMTP", 1, e);
      Log.log("SMTP", 1, "to:" + to_user);
      Log.log("SMTP", 1, "from:" + from_user);
      Log.log("SMTP", 1, "subject:" + subject);
      Log.log("SMTP", 1, "body:" + body);
      resultMessage = String.valueOf(resultMessage) + LOC.G("Server") + ":" + smtp_server + "\r\n" + LOC.G("Port") + ":" + smtp_port + "\r\n" + LOC.G("User") + ":" + smtp_user + "\r\n" + LOC.G("Error") + ":" + e;
    } 
    Log.log("SMTP", 2, "to:" + to_user);
    Log.log("SMTP", 2, "from:" + from_user);
    Log.log("SMTP", 2, "subject:" + subject);
    Log.log("SMTP", 2, "body:" + body);
    return resultMessage;
  }
  
  public static void emailParser(String emails, Vector retList) {
    try {
      emails = Common.replace_str(emails, ";", ",");
      String[] items = emails.split(",");
      for (int x = 0; x < items.length; x++) {
        if (items[x].toUpperCase().indexOf("@G.") >= 0) {
          String serverGroup = Common.dots(items[x].substring(items[x].indexOf("@G.") + 3).trim());
          Common.debug(2, "Mailer:Looking up server group:" + serverGroup);
          Properties groups = UserTools.getGroups(serverGroup);
          if (groups != null) {
            Common.debug(2, "Mailer:Got groups:" + groups.size());
            Vector group = (Vector)groups.get(items[x].substring(0, items[x].toUpperCase().indexOf("@G.")).trim());
            if (group != null) {
              Common.debug(2, "Mailer:Got group:" + group.size());
              for (int xx = 0; xx < group.size(); xx++) {
                String username = group.elementAt(xx).toString();
                Common.debug(2, "Mailer:Finding user:" + username);
                Properties user = UserTools.ut.getUser(serverGroup, username, true);
                if (user != null && !user.getProperty("email", "").equals(""))
                  retList.addElement(user.getProperty("email", "")); 
              } 
            } 
          } 
        } else {
          retList.addElement(items[x]);
        } 
      } 
    } catch (Exception e) {
      Common.debug(1, e);
    } 
  }
}
