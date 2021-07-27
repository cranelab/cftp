package crushftp.server.ssh;

import com.maverick.sshd.Connection;
import com.maverick.sshd.platform.FileSystem;
import com.maverick.sshd.platform.FileSystemFactory;
import com.maverick.sshd.platform.PermissionDeniedException;
import crushftp.server.ServerSessionSSH;
import java.io.IOException;

public class SSHServerSessionFactory implements FileSystemFactory {
  public FileSystem createInstance(Connection conn, String protocolInUse) throws PermissionDeniedException, IOException {
    ServerSessionSSH fs = new ServerSessionSSH();
    fs.init(conn, protocolInUse);
    return fs;
  }
}
