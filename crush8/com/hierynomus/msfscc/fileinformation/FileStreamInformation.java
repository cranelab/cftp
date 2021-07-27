package com.hierynomus.msfscc.fileinformation;

import java.util.ArrayList;
import java.util.List;

public class FileStreamInformation implements FileQueryableInformation {
  private List<FileStreamInformationItem> streamList;
  
  FileStreamInformation(List<FileStreamInformationItem> streamList) {
    this.streamList = streamList;
  }
  
  public List<FileStreamInformationItem> getStreamList() {
    return this.streamList;
  }
  
  public List<String> getStreamNames() {
    List<String> nameList = new ArrayList<>();
    for (FileStreamInformationItem s : this.streamList)
      nameList.add(s.getName()); 
    return nameList;
  }
}
