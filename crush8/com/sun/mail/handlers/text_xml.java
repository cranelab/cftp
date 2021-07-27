package com.sun.mail.handlers;

import java.io.IOException;
import java.io.OutputStream;
import javax.activation.ActivationDataFlavor;
import javax.activation.DataSource;
import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class text_xml extends text_plain {
  private static final ActivationDataFlavor[] flavors = new ActivationDataFlavor[] { new ActivationDataFlavor(String.class, "text/xml", "XML String"), new ActivationDataFlavor(String.class, "application/xml", "XML String"), new ActivationDataFlavor(StreamSource.class, "text/xml", "XML"), new ActivationDataFlavor(StreamSource.class, "application/xml", "XML") };
  
  protected ActivationDataFlavor[] getDataFlavors() {
    return flavors;
  }
  
  protected Object getData(ActivationDataFlavor aFlavor, DataSource ds) throws IOException {
    if (aFlavor.getRepresentationClass() == String.class)
      return getContent(ds); 
    if (aFlavor.getRepresentationClass() == StreamSource.class)
      return new StreamSource(ds.getInputStream()); 
    return null;
  }
  
  public void writeTo(Object obj, String mimeType, OutputStream os) throws IOException {
    if (!isXmlType(mimeType))
      throw new IOException("Invalid content type \"" + mimeType + "\" for text/xml DCH"); 
    if (obj instanceof String) {
      super.writeTo(obj, mimeType, os);
      return;
    } 
    if (!(obj instanceof DataSource) && !(obj instanceof Source))
      throw new IOException("Invalid Object type = " + obj.getClass() + ". XmlDCH can only convert DataSource or Source to XML."); 
    try {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      StreamResult result = new StreamResult(os);
      if (obj instanceof DataSource) {
        transformer.transform(new StreamSource(((DataSource)obj)
              .getInputStream()), result);
      } else {
        transformer.transform((Source)obj, result);
      } 
    } catch (TransformerException ex) {
      IOException ioex = new IOException("Unable to run the JAXP transformer on a stream " + ex.getMessage());
      ioex.initCause(ex);
      throw ioex;
    } catch (RuntimeException ex) {
      IOException ioex = new IOException("Unable to run the JAXP transformer on a stream " + ex.getMessage());
      ioex.initCause(ex);
      throw ioex;
    } 
  }
  
  private boolean isXmlType(String type) {
    try {
      ContentType ct = new ContentType(type);
      return (ct.getSubType().equals("xml") && (ct
        .getPrimaryType().equals("text") || ct
        .getPrimaryType().equals("application")));
    } catch (ParseException ex) {
      return false;
    } catch (RuntimeException ex) {
      return false;
    } 
  }
}
