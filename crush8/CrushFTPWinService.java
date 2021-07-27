import org.boris.winrun4j.AbstractService;
import org.boris.winrun4j.ServiceException;

public class CrushFTPWinService extends AbstractService {
  public int serviceMain(String[] args) throws ServiceException {
    CrushFTP.main(args);
    while (!this.shutdown) {
      try {
        Thread.sleep(1000L);
      } catch (InterruptedException interruptedException) {}
    } 
    System.exit(0);
    return 0;
  }
}
