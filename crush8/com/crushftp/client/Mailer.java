package com.crushftp.client;

import java.lang.reflect.Method;
import java.security.Security;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

public class Mailer {
  public static Object used_lock = new Object();
  
  static Class class$0;
  
  static Class class$1;
  
  public static String send_mail(String server_ip, String to_user, String cc_user, String bcc_user, String from_user, String reply_to_user, String subject, String body, String smtp_server, String smtp_user, String smtp_pass, boolean smtp_ssl, boolean html, File_B[] attachments) {
    return send_mail(server_ip, to_user, cc_user, bcc_user, from_user, reply_to_user, subject, body, smtp_server, smtp_user, smtp_pass, smtp_ssl, html, attachments, new Vector());
  }
  
  public static String send_mail(String server_ip, String to_user, String cc_user, String bcc_user, String from_user, String reply_to_user, String subject, String body, String smtp_server, String smtp_user, String smtp_pass, boolean smtp_ssl, boolean html, File_B[] attachments, Vector fileMimeTypes) {
    return send_mail(server_ip, to_user, cc_user, bcc_user, from_user, reply_to_user, subject, body, smtp_server, smtp_user, smtp_pass, smtp_ssl, html, attachments, new Vector(), new Vector());
  }
  
  public static String send_mail(String server_ip, String to_user, String cc_user, String bcc_user, String from_user, String reply_to_user, String subject, String body, String smtp_server, String smtp_user, String smtp_pass, boolean smtp_ssl, boolean html, File_B[] attachments, Vector fileMimeTypes, Vector remoteFiles) {
    try {
      smtp_pass = Common.encryptDecrypt(smtp_pass, false);
    } catch (Exception e) {
      Common.log("SMTP", 0, e);
    } 
    String plain_body = body;
    if (html && (body.indexOf("\r") >= 0 || body.indexOf("\n") >= 0) && body.toUpperCase().indexOf("<BR") < 0 && body.toUpperCase().indexOf("<SPAN") < 0) {
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
      boolean google_oauth2 = false;
      if ((smtp_server.endsWith("gmail.com") && smtp_user.contains("~")) || System.getProperty("crushftp.smtp_xoauth2", "false").equals("true"))
        google_oauth2 = true; 
      Properties props = System.getProperties();
      props.put("mail.transport.protocol", "smtp");
      props.put("mail.smtp.host", smtp_server);
      props.put("mail.smtp.port", (new StringBuffer(String.valueOf(smtp_port))).toString());
      props.put("mail.smtp.auth", "false");
      if (Integer.parseInt(System.getProperty("crushftp.log_debug_level", "0")) >= 2)
        props.put("mail.debug", "true"); 
      props.put("mail.smtp.ssl.socketFactory", new UnsafeSSLSocketFactory());
      if (!System.getProperty("crushftp.smtp_helo_ip", "").equals(""))
        props.put("mail.smtp.localhost", System.getProperty("crushftp.smtp_helo_ip", "")); 
      if (smtp_ssl)
        props.put("mail.smtp.starttls.required", "true"); 
      props.put("mail.smtp.starttls.enable", System.getProperty("crushftp.smtp_tls", "true"));
      Common.log("SMTP", 2, "SMTP STARTTLS ENABLED:" + System.getProperty("crushftp.smtp_tls", "true"));
      if (smtp_port == 465)
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); 
      props.put("mail.smtp.socketFactory.port", (new StringBuffer(String.valueOf(smtp_port))).toString());
      props.put("mail.smtp.sasl.enable", System.getProperty("crushftp.smtp.sasl", "false"));
      String google_client_id = "";
      String google_client_secret = "";
      String google_refresh_token = "";
      String oauth_client_id = "";
      String oauth_client_secret = "";
      String oauth_refresh_token = "";
      String oauth_url = "";
      if (google_oauth2) {
        Security.addProvider(new CrushOAuth2Provider());
        props.put("mail.smtp.sasl.enable", "true");
        props.put("mail.smtp.sasl.mechanisms", "XOAUTH2");
        if (System.getProperty("crushftp.smtp_xoauth2", "false").equals("true")) {
          oauth_client_id = smtp_user.split("~")[0];
          oauth_client_secret = Common.encryptDecrypt(smtp_user.split("~")[1], false);
          oauth_refresh_token = smtp_pass;
          oauth_url = smtp_user.split("~")[2];
        } else {
          google_refresh_token = smtp_pass;
          google_client_id = smtp_user.split("~")[1];
          google_client_secret = Common.encryptDecrypt(smtp_user.split("~")[2], false);
          if (google_refresh_token.equals(""));
          if (google_client_id.equals(""));
          if (google_client_secret.equals(""));
        } 
        smtp_pass = "";
        smtp_user = smtp_user.split("~")[0];
      } 
      Session session = null;
      if (!smtp_user.trim().equals("") && !google_oauth2) {
        props.put("mail.smtp.auth", "true");
        Authenticator auth = new Mailer$1$SMTPAuthenticator(smtp_user, smtp_pass);
        session = Session.getInstance(props, auth);
      } else {
        session = Session.getInstance(props, null);
      } 
      MimeMessage msg = new MimeMessage(session);
      InternetAddress[] to = new InternetAddress[toList.size()];
      for (int x = 0; x < toList.size(); x++) {
        if (!toList.elementAt(x).toString().trim().equals(""))
          to[x] = new InternetAddress(toList.elementAt(x).toString().trim()); 
      } 
      InternetAddress[] cc = new InternetAddress[ccList.size()];
      for (int i = 0; i < ccList.size(); i++) {
        if (!ccList.elementAt(i).toString().trim().equals(""))
          cc[i] = new InternetAddress(ccList.elementAt(i).toString().trim()); 
      } 
      InternetAddress[] bcc = new InternetAddress[bccList.size()];
      for (int j = 0; j < bccList.size(); j++) {
        if (!bccList.elementAt(j).toString().trim().equals(""))
          bcc[j] = new InternetAddress(bccList.elementAt(j).toString().trim()); 
      } 
      InternetAddress from = null;
      if (from_user.indexOf("<") >= 0 && from_user.indexOf(">") >= 0 && from_user.indexOf(">") >= from_user.indexOf("<")) {
        from = new InternetAddress(emailStripperOnly(from_user.trim()), from_user.substring(0, from_user.indexOf("<")).trim());
      } else {
        from = new InternetAddress(emailStripper(from_user.trim()));
      } 
      InternetAddress reply_to = null;
      if (reply_to_user != null && !reply_to_user.trim().equals(""))
        if (reply_to_user.indexOf("<") >= 0 && reply_to_user.indexOf(">") >= 0 && reply_to_user.indexOf(">") >= reply_to_user.indexOf("<")) {
          reply_to = new InternetAddress(emailStripperOnly(reply_to_user.trim()), reply_to_user.substring(0, reply_to_user.indexOf("<")).trim());
        } else {
          reply_to = new InternetAddress(emailStripper(reply_to_user.trim()));
        }  
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
            if (attachments[k] != null) {
              File_B attachment = attachments[k];
              if (!attachment.isDirectory()) {
                BodyPart messageBodyPart = new MimeBodyPart();
                FileDataSource source = new FileDataSource(attachment);
                if (fileMimeTypes != null && fileMimeTypes.size() > 0)
                  source.setFileTypeMap((FileTypeMap)getCustomMimetypesFileTypeMap(fileMimeTypes)); 
                messageBodyPart.setDataHandler(new DataHandler((DataSource)source));
                messageBodyPart.setFileName(attachment.getName());
                moreItems.addElement(messageBodyPart);
              } 
            } 
          }  
        if (remoteFiles != null && remoteFiles.size() > 0)
          for (int k = 0; k < remoteFiles.size(); k++) {
            Properties p = remoteFiles.get(k);
            BodyPart messageBodyPart = new MimeBodyPart();
            if (p.get("vrl") != null && p.get("prefs") != null) {
              ClientDataSource source = new ClientDataSource((VRL)p.get("vrl"), (Properties)p.get("prefs"));
              p.put("dataSource", source);
              if (fileMimeTypes != null && fileMimeTypes.size() > 0)
                source.setFileTypeMap(getCustomMimetypesFileTypeMap(fileMimeTypes)); 
              messageBodyPart.setDataHandler(new DataHandler(source));
              messageBodyPart.setFileName(((VRL)p.get("vrl")).getName());
              moreItems.addElement(messageBodyPart);
            } 
          }  
        if (moreItems.size() > 0) {
          Multipart mp2 = new MimeMultipart("mixed");
          mp2.addBodyPart(htmlpart);
          for (int k = 0; k < moreItems.size(); k++)
            mp2.addBodyPart(moreItems.elementAt(k)); 
          msg.setContent(mp2);
        } else {
          Multipart mp2 = new MimeMultipart("alternative");
          mp2.addBodyPart(textpart);
          mp2.addBodyPart(htmlpart);
          msg.setContent(mp2);
        } 
      } else if (attachments != null || (remoteFiles != null && remoteFiles.size() > 0)) {
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText(body);
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);
        if (attachments != null)
          for (int m = 0; m < attachments.length; m++) {
            if (attachments[m] != null) {
              File_B attachment = attachments[m];
              if (!attachment.isDirectory()) {
                messageBodyPart = new MimeBodyPart();
                FileDataSource source = new FileDataSource(attachment);
                if (fileMimeTypes != null && fileMimeTypes.size() > 0)
                  source.setFileTypeMap((FileTypeMap)getCustomMimetypesFileTypeMap(fileMimeTypes)); 
                messageBodyPart.setDataHandler(new DataHandler((DataSource)source));
                messageBodyPart.setFileName(attachment.getName());
                multipart.addBodyPart(messageBodyPart);
              } 
            } 
          }  
        for (int k = 0; remoteFiles != null && k < remoteFiles.size(); k++) {
          Properties p = remoteFiles.get(k);
          messageBodyPart = new MimeBodyPart();
          if (p.get("vrl") != null && p.get("prefs") != null) {
            ClientDataSource source = new ClientDataSource((VRL)p.get("vrl"), (Properties)p.get("prefs"));
            p.put("dataSource", source);
            if (fileMimeTypes != null && fileMimeTypes.size() > 0)
              source.setFileTypeMap(getCustomMimetypesFileTypeMap(fileMimeTypes)); 
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(((VRL)p.get("vrl")).getName());
            multipart.addBodyPart(messageBodyPart);
          } 
        } 
        msg.setContent(multipart);
      } else {
        msg.setText(body, "UTF-8");
      } 
      msg.setFrom(from);
      if (reply_to != null)
        msg.setReplyTo(new Address[] { reply_to }); 
      if (to.length > 0)
        msg.setRecipients(MimeMessage.RecipientType.TO, (Address[])to); 
      if (cc.length > 0)
        msg.setRecipients(MimeMessage.RecipientType.CC, (Address[])cc); 
      if (bcc.length > 0)
        msg.setRecipients(MimeMessage.RecipientType.BCC, (Address[])bcc); 
      if (System.getProperty("crushftp.smtp_subject_utf8", "false").equals("true")) {
        msg.setSubject(subject, "UTF-8");
      } else if (System.getProperty("crushftp.smtp_subject_encoded", "false").equals("true")) {
        msg.setSubject(MimeUtility.encodeText(subject, "UTF-8", "B"));
      } else {
        msg.setSubject(subject);
      } 
      msg.setSentDate(new Date());
      msg.saveChanges();
      if (google_oauth2) {
        Properties token = new Properties();
        if (System.getProperty("crushftp.smtp_xoauth2", "false").equals("true")) {
          Common.log("SMTP", 2, "Client ID : " + oauth_client_id);
          Common.log("SMTP", 2, "Client Secret : " + oauth_client_secret);
          Common.log("SMTP", 2, "Redirect URL : " + oauth_url);
          token = Common.smtp_oauth_renew_tokens(oauth_refresh_token, oauth_client_id, oauth_client_secret, oauth_url);
          smtp_pass = token.getProperty("access_token", "");
        } else {
          token = Common.google_renew_tokens(google_refresh_token, google_client_id, google_client_secret);
        } 
        props.put("mail.sasl.mechanisms.oauth2.oauthToken", token.getProperty("access_token", ""));
        if (!token.getProperty("access_token", "").equals(""))
          Common.log("SMTP", 2, "XOAUTH : Got the access toeken."); 
      } 
      Transport transport = session.getTransport("smtp");
      if (smtp_user.trim().equals("")) {
        transport.connect();
      } else {
        transport.connect(smtp_user, smtp_pass);
      } 
      transport.sendMessage(msg, msg.getAllRecipients());
      resultMessage = "Success!";
      try {
        transport.close();
      } catch (Exception exception) {}
    } catch (Exception e) {
      e.printStackTrace();
      Common.log("SMTP", 1, e);
      Common.log("SMTP", 1, "to:" + to_user);
      Common.log("SMTP", 1, "from:" + from_user);
      Common.log("SMTP", 1, "subject:" + subject);
      Common.log("SMTP", 1, "body:" + body);
      resultMessage = String.valueOf(resultMessage) + "Server:" + smtp_server + "\r\n" + "Port" + ":" + smtp_port + "\r\n" + "User" + ":" + smtp_user + "\r\n" + "Error" + ":" + e;
    } finally {
      if (remoteFiles != null && remoteFiles.size() > 0)
        for (int x = 0; x < remoteFiles.size(); x++) {
          Properties p = remoteFiles.get(x);
          if (p.get("dataSource") != null)
            try {
              ((ClientDataSource)p.get("dataSource")).logout();
            } catch (Exception e) {
              Common.log("SMTP", 1, e);
            }  
        }  
    } 
    Common.log("SMTP", 2, "to:" + to_user);
    Common.log("SMTP", 2, "from:" + from_user);
    Common.log("SMTP", 2, "subject:" + subject);
    Common.log("SMTP", 2, "body:" + body);
    return resultMessage;
  }
  
  public static void emailParser(String emails, Vector retList) {
    try {
      emails = Common.replace_str(emails, ";", ",");
      String[] items = emails.split(",");
      for (int x = 0; x < items.length; x++) {
        if (items[x].toUpperCase().indexOf("@G.") >= 0) {
          String serverGroup = Common.dots(items[x].substring(items[x].indexOf("@G.") + 3).trim());
          Common.log("SMTP", 2, "Mailer:Looking up server group:" + serverGroup);
          Class ut = Class.forName("crushftp.handlers.UserTools");
          if (class$0 == null)
            try {
            
            } catch (ClassNotFoundException classNotFoundException) {
              throw new NoClassDefFoundError(null.getMessage());
            }  
          false[class$0] = class$0 = Class.forName("java.lang.String");
          Method getGroups = "getGroups".getDeclaredMethod((String)new Class[1], new Class[1]);
          Properties groups = (Properties)getGroups.invoke(null, new Object[] { serverGroup });
          if (groups != null) {
            Common.log("SMTP", 2, "Mailer:Got groups:" + groups.size());
            Vector group = (Vector)groups.get(items[x].substring(0, items[x].toUpperCase().indexOf("@G.")).trim());
            if (group != null) {
              Common.log("SMTP", 2, "Mailer:Got group:" + group.size());
              for (int xx = 0; xx < group.size(); xx++) {
                String username = group.elementAt(xx).toString();
                Common.log("SMTP", 2, "Mailer:Finding user:" + username);
                if (class$0 == null)
                  try {
                  
                  } catch (ClassNotFoundException classNotFoundException) {
                    throw new NoClassDefFoundError(null.getMessage());
                  }  
                false[class$0] = class$0 = Class.forName("java.lang.String");
                if (class$0 == null)
                  try {
                  
                  } catch (ClassNotFoundException classNotFoundException) {
                    throw new NoClassDefFoundError(null.getMessage());
                  }  
                true[class$0] = class$0 = Class.forName("java.lang.String");
                if (class$1 == null)
                  try {
                  
                  } catch (ClassNotFoundException classNotFoundException) {
                    throw new NoClassDefFoundError(null.getMessage());
                  }  
                2[class$1] = class$1 = Class.forName("java.lang.Boolean");
                Method getUser = (new Class[3]).getDeclaredMethod((String)new Class[3], new Class[3]);
                Properties user = (Properties)getUser.invoke(null, new Object[] { serverGroup, username, new Boolean(true) });
                if (user != null && !user.getProperty("email", "").equals("")) {
                  String tmp_email = emailStripper(user.getProperty("email", ""));
                  if (!tmp_email.trim().equals(""))
                    retList.addElement(tmp_email); 
                } 
              } 
            } 
          } 
        } else {
          String tmp_email = emailStripper(items[x]);
          if (!tmp_email.trim().equals(""))
            retList.addElement(tmp_email); 
        } 
      } 
    } catch (Exception e) {
      Common.log("SMTP", 1, e);
    } 
  }
  
  public static String emailStripper(String email) {
    if (email.indexOf("<") >= 0 && email.indexOf(">") >= 0 && email.indexOf(">") >= email.indexOf("<"))
      email = email.replace(',', ' '); 
    return email;
  }
  
  public static String emailStripperOnly(String email) {
    if (email.indexOf("<") >= 0 && email.indexOf(">") >= 0 && email.indexOf(">") >= email.indexOf("<"))
      email = email.substring(email.indexOf("<") + 1, email.indexOf(">")); 
    return email;
  }
  
  private static MimetypesFileTypeMap getCustomMimetypesFileTypeMap(Vector v) {
    MimetypesFileTypeMap mimeFileTypes = (MimetypesFileTypeMap)FileTypeMap.getDefaultFileTypeMap();
    for (int x = 0; x < v.size(); x++)
      mimeFileTypes.addMimeTypes(v.get(x)); 
    return mimeFileTypes;
  }
}
