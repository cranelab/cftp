package com.sun.mail.smtp;

import javax.mail.MessagingException;

public interface SaslAuthenticator {
  boolean authenticate(String[] paramArrayOfString, String paramString1, String paramString2, String paramString3, String paramString4) throws MessagingException;
}
