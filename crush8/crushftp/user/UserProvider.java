package crushftp.user;

import java.util.Properties;
import java.util.Vector;

public class UserProvider {
  public Vector loadUserList(String serverGroup) {
    return null;
  }
  
  public Vector findUserEmail(String serverGroup, String email) {
    return null;
  }
  
  public Properties loadUser(String serverGroup, String username, Properties inheritanceProp, boolean flattenUser) {
    return null;
  }
  
  public Properties loadInheritance(String serverGroup) {
    return null;
  }
  
  public void writeInheritance(String serverGroup, Properties inheritance) {}
  
  public Properties loadGroups(String serverGroup) {
    return null;
  }
  
  public void writeGroups(String serverGroup, Properties groups) {}
  
  public void writeUser(String serverGroup, String username, Properties user, boolean backup) {}
  
  public void updateUser(String serverGroup, String username1, String username2, String password) {}
  
  public void deleteUser(String serverGroup, String username) {}
  
  public void addFolder(String serverGroup, String username, String path, String name) {}
  
  public void addItem(String serverGroup, String username, String path, String name, String url, String type, Properties moreItems, boolean encrypted, String encrypted_class) throws Exception {}
  
  public Properties buildVFS(String serverGroup, String username) {
    return null;
  }
  
  public void writeVFS(String serverGroup, String username, Properties virtual) {}
}
