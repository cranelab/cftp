package org.boris.winrun4j;

public interface Service {
  public static final int SERVICE_CONTROL_STOP = 1;
  
  public static final int SERVICE_CONTROL_PAUSE = 2;
  
  public static final int SERVICE_CONTROL_CONTINUE = 3;
  
  public static final int SERVICE_CONTROL_INTERROGATE = 4;
  
  public static final int SERVICE_CONTROL_SHUTDOWN = 5;
  
  public static final int SERVICE_CONTROL_PARAMCHANGE = 6;
  
  public static final int SERVICE_CONTROL_NETBINDADD = 7;
  
  public static final int SERVICE_CONTROL_NETBINDREMOVE = 8;
  
  public static final int SERVICE_CONTROL_NETBINDENABLE = 9;
  
  public static final int SERVICE_CONTROL_NETBINDDISABLE = 10;
  
  public static final int SERVICE_CONTROL_DEVICEEVENT = 11;
  
  public static final int SERVICE_CONTROL_HARDWAREPROFILECHANGE = 12;
  
  public static final int SERVICE_CONTROL_POWEREVENT = 13;
  
  public static final int SERVICE_CONTROL_SESSIONCHANGE = 14;
  
  int serviceRequest(int paramInt) throws ServiceException;
  
  int serviceMain(String[] paramArrayOfString) throws ServiceException;
}
