package crushftp.server;

import com.crushftp.client.Common;
import com.crushftp.client.VRL;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

public class ServerSessionHTTPWI {
  public static File serveFile(ServerSessionHTTP sessionHTTP, Vector header, OutputStream original_os, boolean onlyGetFile, String downloadFilename) throws Exception {
    String data = "";
    boolean acceptsGZIP = false;
    File file = null;
    String theFile = "";
    String ifnonematch = "0";
    String domain = "";
    boolean headerOnly = false;
    for (int x = 0; x < header.size(); x++) {
      data = header.elementAt(x).toString();
      if (data.startsWith("Accept-Encoding: ")) {
        data = data.substring(data.indexOf(" ") + 1);
        if (data.toUpperCase().indexOf("GZIP") >= 0 && ServerStatus.BG("allow_gzip"))
          acceptsGZIP = true; 
      } else if ((data.startsWith("HEAD ") || data.startsWith("GET ")) && data.indexOf(" /WebInterface/CrushFTPDrive/win_service.jar") >= 0) {
        if (data.toUpperCase().startsWith("HEAD "))
          headerOnly = true; 
        theFile = "plugins/lib/win_service.jar";
        file = new File(String.valueOf(System.getProperty("crushftp.plugins")) + theFile);
        if (file.getCanonicalPath().indexOf("WebInterface") < 0)
          file = null; 
      } else if ((data.startsWith("HEAD ") || data.startsWith("GET ")) && (data.indexOf(" /WebInterface/") >= 0 || data.toUpperCase().indexOf(" /FAVICON.ICO") >= 0)) {
        if (data.toUpperCase().startsWith("HEAD "))
          headerOnly = true; 
        theFile = data.substring(data.indexOf(" ") + 1, data.lastIndexOf(" HTTP"));
        if (data.toUpperCase().indexOf(" /FAVICON.ICO") >= 0)
          theFile = "/WebInterface" + theFile; 
        theFile = Common.url_decode(theFile);
        theFile = Common.replace_str(theFile, "..", "");
        theFile = Common.replace_str(theFile, "\\", "/");
        theFile = Common.replace_str(theFile, "//", "/");
        if (theFile.indexOf("?") >= 0)
          theFile = theFile.substring(0, theFile.indexOf("?")); 
        if (data.indexOf(" /WebInterface/images/Preview/") >= 0) {
          file = new File(String.valueOf(ServerStatus.SG("previews_path")) + theFile.substring("/WebInterface/images/".length()));
        } else {
          file = new File(Common.dots((new File(String.valueOf(System.getProperty("crushftp.web")) + theFile.substring(1))).getCanonicalPath()));
          if (file.getCanonicalPath().indexOf("WebInterface") < 0)
            file = null; 
        } 
      } else if (data.toUpperCase().startsWith("IF-MODIFIED-SINCE: ")) {
        ifnonematch = data.substring(data.toUpperCase().indexOf("IF-MODIFIED-SINCE:") + "IF-MODIFIED-SINCE:".length()).trim();
        try {
          ifnonematch = (new StringBuffer(String.valueOf(sessionHTTP.sdf_rfc1123.parse(ifnonematch).getTime()))).toString();
        } catch (Exception exception) {}
      } else if (data.toUpperCase().startsWith("IF-NONE-MATCH: ")) {
        ifnonematch = data.substring(data.toUpperCase().indexOf("IF-NONE-MATCH:") + "IF-NONE-MATCH:".length()).trim();
      } else if (data.toUpperCase().startsWith("HOST: ") || data.toUpperCase().startsWith("X-FORWARDED-HOST: ")) {
        domain = data.substring(data.toUpperCase().indexOf(" ") + 1).trim().toUpperCase();
        if (domain.indexOf(":") >= 0)
          domain = domain.substring(0, domain.indexOf(":")); 
      } 
    } 
    if (theFile.toUpperCase().endsWith(".JPG") || theFile.toUpperCase().endsWith(".GIF") || theFile.toUpperCase().endsWith(".JAR") || theFile.toUpperCase().endsWith(".SWF"))
      acceptsGZIP = false; 
    Common.updateMimes();
    Properties mimes = Common.mimes;
    String ext = "";
    if (theFile.toString().lastIndexOf(".") >= 0)
      ext = theFile.toString().substring(theFile.toString().lastIndexOf(".")).toUpperCase(); 
    if (mimes.getProperty(ext, "").equals(""))
      ext = "*"; 
    if (file.getName().equalsIgnoreCase("login.html"))
      try {
        Vector v = ServerStatus.VG("login_page_list");
        for (int i = 0; i < v.size(); i++) {
          Properties p = v.elementAt(i);
          if (Common.do_search(p.getProperty("domain"), domain, false, 0)) {
            File tmpLogin = new File(String.valueOf(file.getCanonicalFile().getParentFile().getPath()) + "/" + p.getProperty("page"));
            if (tmpLogin.exists())
              file = tmpLogin; 
            break;
          } 
        } 
      } catch (Exception e) {
        Log.log("HTTP_SERVER", 1, e);
      }  
    if (file.getName().equalsIgnoreCase("favicon.ico"))
      try {
        Vector v = ServerStatus.VG("login_page_list");
        for (int i = 0; i < v.size(); i++) {
          Properties p = v.elementAt(i);
          if (Common.do_search(p.getProperty("domain"), domain, false, 0)) {
            file = new File(String.valueOf(file.getCanonicalFile().getParentFile().getPath()) + "/" + p.getProperty("favicon", "favicon.ico"));
            break;
          } 
        } 
      } catch (Exception e) {
        Log.log("HTTP_SERVER", 1, e);
      }  
    if (onlyGetFile)
      return file; 
    long lastModified = System.currentTimeMillis();
    if (file.exists())
      lastModified = file.lastModified(); 
    ByteArrayOutputStream tempBytes = new ByteArrayOutputStream();
    String template = "";
    if (!file.exists() && file.getPath().indexOf(".zip") >= 0) {
      String zipFile = file.getPath().substring(0, file.getPath().indexOf(".zip") + 4);
      ZipFile zf = new ZipFile(zipFile);
      byte[] b = new byte[32768];
      int bytesRead = 0;
      ZipArchiveEntry zae = zf.getEntry(file.getPath().substring(zipFile.length() + 1).replace('\\', '/'));
      lastModified = zae.getTime();
      InputStream otherFile = zf.getInputStream(zae);
      while (bytesRead >= 0) {
        bytesRead = otherFile.read(b);
        if (bytesRead > 0)
          tempBytes.write(b, 0, bytesRead); 
      } 
      otherFile.close();
      zf.close();
      if ((mimes.getProperty(ext, "").toUpperCase().endsWith("HTML") || mimes.getProperty(ext, "").toUpperCase().endsWith("/CSS") || mimes.getProperty(ext, "").toUpperCase().endsWith("/JAVASCRIPT") || mimes.getProperty(ext, "").toUpperCase().endsWith("/X-JAVA-JNLP-FILE")) && theFile.toUpperCase().startsWith("/WEBINTERFACE/")) {
        template = new String(tempBytes.toByteArray(), "UTF8");
        String template2 = Common.replace_str(template, "/WebInterface/", String.valueOf(sessionHTTP.proxy) + "WebInterface/");
        if (!template.equals(template2))
          lastModified = System.currentTimeMillis(); 
        template = template2;
      } 
    } 
    if (file.exists() && (mimes.getProperty(ext, "").toUpperCase().endsWith("HTML") || mimes.getProperty(ext, "").toUpperCase().endsWith("/CSS") || mimes.getProperty(ext, "").toUpperCase().endsWith("/JAVASCRIPT") || mimes.getProperty(ext, "").toUpperCase().endsWith("/X-JAVA-JNLP-FILE")) && theFile.toUpperCase().startsWith("/WEBINTERFACE/")) {
      byte[] b = new byte[(int)file.length()];
      int bytesRead = 0;
      FileInputStream otherFile = new FileInputStream(file);
      while (bytesRead >= 0) {
        bytesRead = otherFile.read(b);
        if (bytesRead > 0)
          template = String.valueOf(template) + new String(b, 0, bytesRead, "UTF8"); 
      } 
      otherFile.close();
      String template2 = Common.replace_str(template, "/WebInterface/", String.valueOf(sessionHTTP.proxy) + "WebInterface/");
      if (!template.equals(template2))
        lastModified = System.currentTimeMillis(); 
      template = template2;
    } 
    if (file.getName().toUpperCase().endsWith(".JNLP")) {
      String baseUrl = sessionHTTP.getBaseUrl(sessionHTTP.hostString);
      template = Common.replace_str(template, "%base_url%", baseUrl);
      template = Common.replace_str(template, "%user_protocol%", sessionHTTP.server_item.getProperty("serverType", "ftp"));
      template = Common.replace_str(template, "%user_listen_ip%", (new VRL(baseUrl)).getHost());
      template = Common.replace_str(template, "%user_port%", (new StringBuffer(String.valueOf((new VRL(baseUrl)).getPort()))).toString());
      template = Common.replace_str(template, "%user_port%", (new StringBuffer(String.valueOf((new VRL(baseUrl)).getPort()))).toString());
      if (file.getName().equalsIgnoreCase("CrushTunnel.jnlp"))
        template = Common.replace_str(template, "max-heap-size=\"256m\"", "max-heap-size=\"1024m\""); 
    } 
    if (mimes.getProperty(ext, "").toUpperCase().endsWith("/HTML") && theFile.toUpperCase().startsWith("/WEBINTERFACE/")) {
      if (ServerStatus.SG("default_logo").equals("logo.gif"))
        ServerStatus.server_settings.put("default_logo", "logo.png"); 
      template = ServerStatus.thisObj.change_vars_to_values(template, null);
      if (!ServerStatus.SG("default_logo").equals("") && !ServerStatus.SG("default_logo").equalsIgnoreCase("logo.png")) {
        template = Common.replace_str(template, "<a id=\"defaultLogoLink\" href=\"http://www.crushftp.com/\">", "<a id=\"defaultLogoLink\" href=\"javascript:void();\">");
        template = Common.replace_str(template, "<div id=\"headerdiv\"><a href=\"http://www.crushftp.com/\">", "<div id=\"headerdiv\">");
        template = Common.replace_str(template, "/logo.png\" /></a>", "/logo.png\" />");
        template = Common.replace_str(template, "/logo.gif\" /></a>", "/logo.gif\" />");
        template = Common.replace_str(template, "/logo.png", "/" + ServerStatus.SG("default_logo"));
        template = Common.replace_str(template, "/logo.gif", "/" + ServerStatus.SG("default_logo"));
      } 
      String script = ServerStatus.SG("login_custom_script");
      if (script.toLowerCase().indexOf("<script>") < 0)
        script = "<script>" + script + "</script>"; 
      template = Common.replace_str(template, "<!--##CUSTOMSCRIPT##-->", script);
      template = Common.replace_str(template, "<!--##HEADER##-->", ServerStatus.SG("login_header"));
      template = Common.replace_str(template, "<!--##FOOTER##-->", ServerStatus.SG("login_footer"));
      if (ServerStatus.BG("hide_email_password")) {
        template = Common.replace_str(template, "forgotPasswordDiv\" style=\"width:250;\"", "forgotPasswordDiv\" style=\"position:absolute;visibility:hidden;\"");
        template = Common.replace_str(template, "<p class=\"lostpassword\">", "<p class=\"lostpassword\" style=\"visibility:hidden;\">");
      } 
      if (ServerStatus.BG("email_reset_token")) {
        template = Common.replace_str(template, "function emailPassword()", "function emailPassword() {location.href = " + sessionHTTP.proxy + "'WebInterface/jQuery/reset.html';}\r\nfunction emailPassword2()");
        template = Common.replace_str(template, "javascript:emailPassword();", String.valueOf(sessionHTTP.proxy) + "WebInterface/jQuery/reset.html");
      } 
      template = Common.replace_str(template, "id=\"flashHtmlRow\" style=\"visibility:visible;\"", "id=\"flashHtmlRow\" style=\"position:absolute;left:-5000px;\"");
      template = Common.replace_str(template, "<title>CrushFTP</title>", "<title>" + ServerStatus.SG("default_title") + "</title>");
      template = Common.replace_str(template, "<title>CrushFTP WebInterface :: Login</title>", "<title>" + ServerStatus.SG("default_title") + "</title>");
      template = Common.replace_str(template, "<title>WebInterface</title>", "<title>" + ServerStatus.SG("default_title") + "</title>");
      template = Common.replace_str(template, "\"/WebInterface/custom.js\"", "\"" + sessionHTTP.proxy + "WebInterface/custom.js?random=" + System.currentTimeMillis() + "\"");
      template = Common.replace_str(template, "\"/WebInterface/custom.css\"", "\"" + sessionHTTP.proxy + "WebInterface/custom.css?random=" + System.currentTimeMillis() + "\"");
      if (sessionHTTP.server_item.getProperty("recaptcha_enabled", "false").equals("true")) {
        if (sessionHTTP.server_item.getProperty("recaptcha_version", "1").equals("1")) {
          template = Common.replace_str(template, "/*RECAPTCHA*/", "showRecaptcha('recaptcha_div');");
          template = Common.replace_str(template, "/*RECAPTCHA_PUBLIC_KEY*/", sessionHTTP.server_item.getProperty("recaptcha_public_key", ""));
        } else if (sessionHTTP.server_item.getProperty("recaptcha_version", "1").equals("2")) {
          template = Common.replace_str(template, "<div id=\"recaptcha_div\"></div>", "<div class=\"g-recaptcha\" data-sitekey=\"/*RECAPTCHA_PUBLIC_KEY*/\"></div>");
          template = Common.replace_str(template, "<script type=\"text/javascript\" src=\"https://www.google.com/recaptcha/api/js/recaptcha_ajax.js\"></script>", "<script type=\"text/javascript\" src=\"https://www.google.com/recaptcha/api.js\"></script>");
          template = Common.replace_str(template, "/*RECAPTCHA*/", "showRecaptcha('recaptcha_div');");
          template = Common.replace_str(template, "/*RECAPTCHA_PUBLIC_KEY*/", sessionHTTP.server_item.getProperty("recaptcha_public_key", ""));
        } 
      } else {
        template = Common.replace_str(template, "<script type=\"text/javascript\" src=\"https://www.google.com/recaptcha/api/js/recaptcha_ajax.js\"></script>", "");
      } 
      if (template.indexOf("var passwordRule =") >= 0) {
        String search_part = template.substring(template.indexOf("var passwordRule ="), template.indexOf(";", template.indexOf("var passwordRule =")));
        String replace_part = "var passwordRule = {";
        replace_part = String.valueOf(replace_part) + "random_password_length:" + ServerStatus.IG("random_password_length") + ",";
        replace_part = String.valueOf(replace_part) + "min_password_numbers:" + ServerStatus.IG("min_password_numbers") + ",";
        replace_part = String.valueOf(replace_part) + "min_password_lowers:" + ServerStatus.IG("min_password_lowers") + ",";
        replace_part = String.valueOf(replace_part) + "min_password_uppers:" + ServerStatus.IG("min_password_uppers") + ",";
        replace_part = String.valueOf(replace_part) + "min_password_specials:" + ServerStatus.IG("min_password_specials");
        replace_part = String.valueOf(replace_part) + "}";
        template = Common.replace_str(template, search_part, replace_part);
      } 
    } 
    long checkDate = (new Date()).getTime();
    try {
      checkDate = Long.parseLong(ifnonematch);
    } catch (Exception exception) {}
    sessionHTTP.writeCookieAuth = false;
    if (!file.exists() && tempBytes.size() == 0) {
      sessionHTTP.write_command_http("HTTP/1.1 404 Not Found");
      sessionHTTP.write_standard_headers();
      sessionHTTP.write_command_http("Content-Length: 0");
      sessionHTTP.write_command_http("");
    } else if (checkDate > 0L && checkDate >= lastModified && !file.getName().endsWith(".html")) {
      sessionHTTP.write_command_http("HTTP/1.1 304 Not Modified");
      sessionHTTP.write_standard_headers();
      sessionHTTP.write_command_http("Last-Modified: " + sessionHTTP.sdf_rfc1123.format(new Date(lastModified)));
      sessionHTTP.write_command_http("ETag: " + lastModified);
      sessionHTTP.write_command_http("Content-Length: 0");
      sessionHTTP.write_command_http("");
    } else {
      sessionHTTP.write_command_http("HTTP/1.1 200 OK");
      if (acceptsGZIP)
        sessionHTTP.write_command_http("Transfer-Encoding: chunked"); 
      sessionHTTP.write_standard_headers();
      if (!template.equals(""))
        sessionHTTP.write_command_http("Pragma: no-cache"); 
      String mime = mimes.getProperty(ext, "");
      if (downloadFilename != null) {
        mime = "application/binary";
        sessionHTTP.write_command_http("Content-Disposition: attachment; filename=\"" + Common.replace_str(Common.url_decode(downloadFilename), "\r", "_") + "\"");
      } 
      sessionHTTP.write_command_http("Content-type: " + mime);
      sessionHTTP.write_command_http("Last-Modified: " + sessionHTTP.sdf_rfc1123.format(new Date(lastModified)));
      sessionHTTP.write_command_http("ETag: " + lastModified);
      sessionHTTP.write_command_http("X-UA-Compatible: chrome=1");
      sessionHTTP.write_command_http("Accept-Ranges: bytes");
      if (acceptsGZIP) {
        sessionHTTP.write_command_http("Vary: Accept-Encoding");
        sessionHTTP.write_command_http("Content-Encoding: gzip");
      } else if (template.length() != 0) {
        sessionHTTP.write_command_http("Content-Length: " + (template.getBytes("UTF8")).length);
      } else {
        sessionHTTP.write_command_http("Content-Length: " + ((tempBytes.size() > 0) ? tempBytes.size() : file.length()));
      } 
      sessionHTTP.write_command_http("");
      if (!headerOnly) {
        InputStream in = null;
        try {
          byte[] rawBytes = new byte[65535];
          int bytesRead = 0;
          if (template.length() != 0) {
            in = new ByteArrayInputStream(template.getBytes("UTF8"));
          } else if (tempBytes.size() != 0) {
            in = new ByteArrayInputStream(tempBytes.toByteArray());
          } else {
            in = new FileInputStream(file);
          } 
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          OutputStream out = baos;
          if (acceptsGZIP)
            out = new GZIPOutputStream(baos); 
          while (bytesRead >= 0) {
            bytesRead = in.read(rawBytes);
            if (bytesRead > 0) {
              out.write(rawBytes, 0, bytesRead);
            } else if (acceptsGZIP) {
              ((GZIPOutputStream)out).finish();
            } 
            if (baos.size() > 0) {
              if (acceptsGZIP)
                original_os.write((String.valueOf(Long.toHexString(baos.size())) + "\r\n").getBytes()); 
              baos.writeTo(original_os);
              if (acceptsGZIP)
                original_os.write("\r\n".getBytes()); 
              baos.reset();
            } 
          } 
          in.close();
          out.close();
          if (acceptsGZIP)
            original_os.write("0\r\n\r\n".getBytes()); 
          original_os.flush();
        } catch (Exception e) {
          Log.log("HTTP_SERVER", 1, e);
        } 
        if (in != null)
          in.close(); 
      } 
    } 
    original_os.flush();
    return null;
  }
}
