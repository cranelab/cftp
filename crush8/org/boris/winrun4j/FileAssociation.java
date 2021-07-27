package org.boris.winrun4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileAssociation {
  private String extension;
  
  private String name;
  
  private String contentType;
  
  private String perceivedType;
  
  private String description;
  
  private String icon;
  
  private Map verbs = new HashMap<Object, Object>();
  
  private List openWithList = new ArrayList();
  
  public FileAssociation(String extension) {
    this.extension = extension;
  }
  
  public String getPerceivedType() {
    return this.perceivedType;
  }
  
  public void setPerceivedType(String perceivedType) {
    this.perceivedType = perceivedType;
  }
  
  public String getName() {
    return this.name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getContentType() {
    return this.contentType;
  }
  
  public void setContentType(String contentType) {
    this.contentType = contentType;
  }
  
  public String getDescription() {
    return this.description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public String getIcon() {
    return this.icon;
  }
  
  public void setIcon(String icon) {
    this.icon = icon;
  }
  
  public String getExtension() {
    return this.extension;
  }
  
  public void put(FileVerb fv) {
    this.verbs.put(fv.getVerb(), fv);
  }
  
  public String[] getVerbs() {
    return (String[])this.verbs.keySet().toArray((Object[])new String[0]);
  }
  
  public FileVerb getVerb(String v) {
    return (FileVerb)this.verbs.get(v);
  }
  
  public void addOpenWith(String ow) {
    this.openWithList.add(ow);
  }
  
  public int getOpenWithCount() {
    return this.openWithList.size();
  }
  
  public String getOpenWith(int index) {
    return this.openWithList.get(index);
  }
  
  public void addFileVerb(String verb, String command) {
    FileVerb fv = new FileVerb(verb);
    fv.setCommand(command);
    put(fv);
  }
}
