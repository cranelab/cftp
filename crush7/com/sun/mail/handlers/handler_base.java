package com.sun.mail.handlers;

import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import javax.activation.ActivationDataFlavor;
import javax.activation.DataContentHandler;
import javax.activation.DataSource;

public abstract class handler_base implements DataContentHandler {
  protected abstract ActivationDataFlavor[] getDataFlavors();
  
  protected Object getData(ActivationDataFlavor aFlavor, DataSource ds) throws IOException {
    return getContent(ds);
  }
  
  public DataFlavor[] getTransferDataFlavors() {
    ActivationDataFlavor[] adf = getDataFlavors();
    if (adf.length == 1)
      return new DataFlavor[] { (DataFlavor)adf[0] }; 
    DataFlavor[] df = new DataFlavor[adf.length];
    System.arraycopy(adf, 0, df, 0, adf.length);
    return df;
  }
  
  public Object getTransferData(DataFlavor df, DataSource ds) throws IOException {
    ActivationDataFlavor[] adf = getDataFlavors();
    for (int i = 0; i < adf.length; i++) {
      if (adf[i].equals(df))
        return getData(adf[i], ds); 
    } 
    return null;
  }
}
