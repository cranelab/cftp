package org.boris.winrun4j;

public class FileVerb {
  private String verb;
  
  private String label;
  
  private String command;
  
  private String ddeCommand;
  
  private String ddeApplication;
  
  private String ddeTopic;
  
  public FileVerb(String verb) {
    this.verb = verb;
  }
  
  public String getVerb() {
    return this.verb;
  }
  
  public String getLabel() {
    return this.label;
  }
  
  public void setLabel(String label) {
    this.label = label;
  }
  
  public String getCommand() {
    return this.command;
  }
  
  public void setCommand(String command) {
    this.command = command;
  }
  
  public String getDDECommand() {
    return this.ddeCommand;
  }
  
  public void setDDECommand(String ddeCommand) {
    this.ddeCommand = ddeCommand;
  }
  
  public String getDDEApplication() {
    return this.ddeApplication;
  }
  
  public void setDDEApplication(String ddeApplication) {
    this.ddeApplication = ddeApplication;
  }
  
  public String getDDETopic() {
    return this.ddeTopic;
  }
  
  public void setDDETopic(String ddeTopic) {
    this.ddeTopic = ddeTopic;
  }
}
