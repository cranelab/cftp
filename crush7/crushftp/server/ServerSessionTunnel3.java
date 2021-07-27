package crushftp.server;

import com.crushftp.client.Worker;
import com.crushftp.tunnel3.Chunk;
import com.crushftp.tunnel3.StreamController;
import com.crushftp.tunnel3.StreamReader;
import com.crushftp.tunnel3.StreamWriter;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.SessionCrush;
import crushftp.handlers.SharedSession;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Properties;
import java.util.Vector;

public class ServerSessionTunnel3 {
  public static Properties running_tunnels = new Properties();
  
  public static StreamController getStreamController(String id, String tunnelId) {
    return (StreamController)running_tunnels.get(String.valueOf(id) + "_" + tunnelId);
  }
  
  public static String stopTunnel(String id, String tunnelId) throws Exception {
    StreamController sc = getStreamController(id, tunnelId);
    return sc.startStopTunnel(false);
  }
  
  public static boolean process(Properties request, SessionCrush thisSession, Socket sock, ServerSessionHTTP thisSessionHttp) throws Exception {
    String channel_id = request.getProperty("channel_id");
    StreamController.ram_max_total = ServerStatus.LG("tunnel_ram_cache") * 1024L * 1024L;
    Vector history = new Vector();
    if (request.getProperty("writing").equals("true")) {
      StreamController sc = null;
      Thread.currentThread().setName(String.valueOf(thisSession.uiSG("user_name")) + ":(" + thisSession.uiSG("user_number") + ")-" + thisSession.uiSG("user_ip") + " CRUSH_STREAMING_HTTP_PROXY:HTTPReader");
      sock.setSoTimeout(60000);
      long chunkTimer = System.currentTimeMillis();
      int chunkCount = 0;
      int chunkCountTotal = 0;
      while (SharedSession.find("crushftp.usernames").containsKey(String.valueOf(Common.getPartialIp(thisSession.uiSG("user_ip"))) + "_" + thisSession.getId() + "_user")) {
        if (sc == null)
          sc = (StreamController)running_tunnels.get(String.valueOf(thisSession.getId()) + "_" + request.getProperty("tunnelId")); 
        Thread.currentThread().setName(sc + ":StreamReader:" + channel_id);
        try {
          Chunk c = Chunk.parse(thisSessionHttp.original_is);
          if (c == null) {
            Log.log("TUNNEL", 0, String.valueOf(channel_id) + ":Read null chunk.  Exiting.");
            thisSessionHttp.done = true;
            break;
          } 
          if (c.isCommand() && !c.getCommand().startsWith("A:")) {
            thisSession.add_log_formatted("Chunk Command read:" + c.getCommand(), "POST");
            if (c.getCommand().startsWith("VERSION_CHECK:")) {
              String client_version = c.getCommand().split(":")[1].trim();
              if (Integer.parseInt(Common.replace_str(client_version, ".", "")) < Integer.parseInt(Common.replace_str(ServerStatus.SG("tunnel_minimum_version"), ".", ""))) {
                Log.log("SERVER", 0, "Blocking old tunnel client.");
                sc.out_queue_commands.insertElementAt(sc.makeCommand(0, "VERSION_OLD:" + ServerStatus.SG("tunnel_minimum_version")), 0);
                StreamController sc2 = sc;
                Worker.startWorker(new Runnable(sc2) {
                      private final StreamController val$sc2;
                      
                      public void run() {
                        try {
                          Thread.sleep(2000L);
                          this.val$sc2.startStopTunnel(false);
                        } catch (Exception e) {
                          Log.log("SERVER", 0, e);
                        } 
                      }
                    });
              } 
            } 
          } 
          if (sc == null)
            sc = (StreamController)running_tunnels.get(String.valueOf(thisSession.getId()) + "_" + request.getProperty("tunnelId")); 
          if (sc != null) {
            if (StreamReader.processIncomingChunk(sc, c)) {
              Log.log("TUNNEL", 0, String.valueOf(channel_id) + ":Process incoming indicated friendly exit.  Exiting.");
              thisSessionHttp.done = true;
              break;
            } 
            sc.updateStats(c, channel_id, history, "read", 0);
          } 
          chunkCountTotal++;
          chunkCount++;
          thisSession.active();
          if (System.currentTimeMillis() - chunkTimer > 10000L) {
            chunkTimer = System.currentTimeMillis();
            chunkCount = 0;
            if (getStreamController(thisSession.getId(), request.getProperty("tunnelId")) == null) {
              Log.log("TUNNEL", 0, "Closing channel, no StreamController exists for tunnelid:" + request.getProperty("tunnelId"));
              thisSessionHttp.done = true;
              break;
            } 
          } 
        } catch (SocketTimeoutException e) {
          Log.log("TUNNEL", 0, String.valueOf(channel_id) + ":SocketTimeoutException occurred.  Exiting.");
          thisSessionHttp.done = true;
          break;
        } 
      } 
      if (sc != null)
        sc.stats.remove(String.valueOf(channel_id) + ":read"); 
      thisSession.add_log_formatted("read total chunks:" + chunkCountTotal + " user exists:" + SharedSession.find("crushftp.usernames").containsKey(String.valueOf(Common.getPartialIp(thisSession.uiSG("user_ip"))) + "_" + thisSession.getId() + "_user"), "POST");
    } else {
      StreamController sc = (StreamController)running_tunnels.get(String.valueOf(thisSession.getId()) + "_" + request.getProperty("tunnelId"));
      Thread.currentThread().setName(String.valueOf(thisSession.uiSG("user_name")) + ":(" + thisSession.uiSG("user_number") + ")-" + thisSession.uiSG("user_ip") + " CRUSH_STREAMING_HTTP_PROXY:HTTPWriter");
      thisSessionHttp.done = true;
      thisSessionHttp.write_command_http("HTTP/1.1 200 OK");
      boolean doChunked = false;
      if (doChunked)
        thisSessionHttp.write_command_http("Transfer-Encoding: chunked"); 
      thisSessionHttp.write_standard_headers();
      thisSessionHttp.write_command_http("Pragma: no-cache");
      thisSessionHttp.write_command_http("Content-type: application/binary");
      thisSessionHttp.write_command_http("");
      int chunkCount = 0;
      int chunkCountTotal = 0;
      long chunkTimer = System.currentTimeMillis();
      long restartTimer = System.currentTimeMillis();
      StreamWriter sw = null;
      boolean wrote_close = false;
      while (SharedSession.find("crushftp.usernames").containsKey(String.valueOf(Common.getPartialIp(thisSession.uiSG("user_ip"))) + "_" + thisSession.getId() + "_user")) {
        if (sc == null)
          sc = (StreamController)running_tunnels.get(String.valueOf(thisSession.getId()) + "_" + request.getProperty("tunnelId")); 
        Thread.currentThread().setName(sc + ":StreamWriter:" + channel_id);
        Chunk c = null;
        if (sw == null && sc != null)
          sw = new StreamWriter(sc, null, channel_id); 
        if (sw != null) {
          if (sw.close) {
            Log.log("TUNNEL", 0, String.valueOf(channel_id) + ":StreamWriter asked for close.  Exiting.");
            break;
          } 
          c = sw.processOutgoingChunk();
          if (sw.close) {
            Log.log("TUNNEL", 0, String.valueOf(channel_id) + ":StreamWriter asked for close.  Exiting.");
            break;
          } 
        } 
        if (sc == null && System.currentTimeMillis() - restartTimer > 9000L) {
          Log.log("TUNNEL", 0, "Restarting tunnel since no request to start tunnel id:" + request.getProperty("tunnelId") + " has been received. (Server was likely restarted.)");
          restartTimer = System.currentTimeMillis();
          String command = "RESET:" + request.getProperty("tunnelId");
          c = new Chunk(0, command.getBytes("UTF8"), command.length(), -1);
        } 
        if (c != null) {
          if (c.getCommand().startsWith("CLOSE"))
            wrote_close = true; 
          if (!c.getCommand().startsWith("A:") && sc != null)
            sc.localCache.put(String.valueOf(c.id) + ":" + c.num, c); 
          if (c.isCommand() && !c.getCommand().startsWith("A:") && !c.getCommand().startsWith("PING")) {
            Log.log("TUNNEL", 0, "SENDING CMD :" + c);
          } else if (c.isCommand()) {
            Log.log("TUNNEL", 2, "SENDING CMD :" + c);
          } else {
            Log.log("TUNNEL", 2, "SENDING DATA:" + c);
          } 
          byte[] b = c.toBytes();
          if (doChunked)
            thisSessionHttp.original_os.write((String.valueOf(Long.toHexString(b.length)) + "\r\n").getBytes()); 
          thisSessionHttp.original_os.write(b);
          if (doChunked)
            thisSessionHttp.original_os.write("\r\n".getBytes()); 
          chunkCountTotal++;
          chunkCount++;
          if (sc != null)
            sc.addBytes(c.id, c.len); 
          thisSession.active();
          sw.last_write = System.currentTimeMillis();
          if (sc != null)
            sc.updateStats(c, channel_id, history, "write", 0); 
          if (sc != null)
            sc.lastActivity = System.currentTimeMillis(); 
        } else {
          Thread.sleep(100L);
        } 
        if (System.currentTimeMillis() - chunkTimer > 10000L) {
          chunkTimer = System.currentTimeMillis();
          chunkCount = 0;
          if (getStreamController(thisSession.getId(), request.getProperty("tunnelId")) == null) {
            Log.log("TUNNEL", 0, "Closing channel, no StreamController exists for tunnelid:" + request.getProperty("tunnelId"));
            thisSessionHttp.done = true;
            break;
          } 
        } 
        if (sc != null && sc.closeRequests.containsKey(channel_id) && wrote_close)
          break; 
      } 
      if (doChunked)
        thisSessionHttp.original_os.write("0\r\n\r\n".getBytes()); 
      thisSession.add_log_formatted("wrote total chunks:" + chunkCountTotal + " user exists:" + SharedSession.find("crushftp.usernames").containsKey(String.valueOf(Common.getPartialIp(thisSession.uiSG("user_ip"))) + "_" + thisSession.getId() + "_user"), "POST");
      if (sc != null)
        sc.closeRequests.remove(channel_id); 
      if (sc != null)
        sc.stats.remove(String.valueOf(channel_id) + ":write"); 
      return true;
    } 
    return false;
  }
}
