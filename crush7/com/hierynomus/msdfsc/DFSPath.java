package com.hierynomus.msdfsc;

import com.hierynomus.smbj.common.SmbPath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DFSPath {
  private final List<String> pathComponents;
  
  public DFSPath(String uncPath) {
    this.pathComponents = splitPath(uncPath);
  }
  
  public DFSPath(List<String> pathComponents) {
    this.pathComponents = pathComponents;
  }
  
  public List<String> getPathComponents() {
    return this.pathComponents;
  }
  
  public DFSPath replacePrefix(String prefixToReplace, String target) {
    List<String> componentsToReplace = splitPath(prefixToReplace);
    List<String> replacedComponents = new ArrayList<String>();
    replacedComponents.addAll(splitPath(target));
    for (int i = componentsToReplace.size(); i < this.pathComponents.size(); i++)
      replacedComponents.add(this.pathComponents.get(i)); 
    return new DFSPath(replacedComponents);
  }
  
  public boolean hasOnlyOnePathComponent() {
    return (this.pathComponents.size() == 1);
  }
  
  public boolean isSysVolOrNetLogon() {
    if (this.pathComponents.size() > 1) {
      String second = this.pathComponents.get(1);
      return !(!"SYSVOL".equals(second) && !"NETLOGON".equals(second));
    } 
    return false;
  }
  
  public boolean isIpc() {
    if (this.pathComponents.size() > 1)
      return "IPC$".equals(this.pathComponents.get(1)); 
    return false;
  }
  
  static DFSPath from(SmbPath path) {
    List<String> pathComponents = new ArrayList<String>();
    pathComponents.add(path.getHostname());
    if (path.getShareName() != null)
      pathComponents.add(path.getShareName()); 
    if (path.getPath() != null)
      pathComponents.addAll(splitPath(path.getPath())); 
    return new DFSPath(pathComponents);
  }
  
  private static List<String> splitPath(String pathPart) {
    String splitPath = pathPart;
    if (pathPart.charAt(0) == '\\')
      if (pathPart.charAt(1) == '\\') {
        splitPath = pathPart.substring(2);
      } else {
        splitPath = pathPart.substring(1);
      }  
    return Arrays.asList(splitPath.split("\\\\"));
  }
  
  public String toPath() {
    StringBuilder sb = new StringBuilder();
    for (String pathComponent : this.pathComponents)
      sb.append("\\").append(pathComponent); 
    return sb.toString();
  }
  
  public String toString() {
    return "DFSPath{" + this.pathComponents + "}";
  }
}
