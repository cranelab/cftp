package com.crushftp.client;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class DuoSend {
  private String method;
  
  private String host;
  
  private String uri;
  
  private String ikey;
  
  private String skey = null;
  
  Properties params = new Properties();
  
  SimpleDateFormat sdf_rfc2822 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
  
  public static void main(String[] args) {
    System.exit(0);
  }
  
  public DuoSend(String method, String host, String uri, String ikey, String skey) {
    this.method = method.toUpperCase();
    this.host = host;
    this.uri = uri;
    this.ikey = ikey;
    this.skey = skey;
  }
  
  public void addParam(String key, String val) {
    this.params.put(key, val);
  }
  
  public String executeRequest() throws Exception {
    JSONObject result = (JSONObject)JSONValue.parse(executeHttpRequest());
    if (!result.get("stat").toString().equals("OK"))
      throw new Exception("Duo error code (" + result.get("code") + "): " + result.get("message")); 
    return ((JSONObject)result.get("response")).get("result").toString();
  }
  
  public String executeHttpRequest() throws Exception {
    String url = "https://" + this.host + this.uri;
    String root_url = url;
    String queryString = createQueryString();
    boolean doOutput = false;
    if ((this.method.equals("GET") || this.method.equals("DELETE")) && queryString.length() > 0) {
      url = String.valueOf(url) + "?" + queryString;
    } else if (this.method.equals("POST") || this.method.equals("PUT")) {
      doOutput = (queryString.length() > 0);
    } 
    if (this.method.equalsIgnoreCase("GET") || this.method.equalsIgnoreCase("POST")) {
      if (this.method.equalsIgnoreCase("POST") && doOutput)
        url = root_url; 
      HttpURLConnection httpURLConnection = (HttpURLConnection)(new URL(url)).openConnection();
      httpURLConnection.setRequestMethod(this.method);
      Date d = new Date();
      httpURLConnection.setRequestProperty("Authorization", getSignRequest(d));
      httpURLConnection.setRequestProperty("Date", this.sdf_rfc2822.format(d));
      if (doOutput) {
        httpURLConnection.setDoOutput(true);
        httpURLConnection.getOutputStream().write(queryString.getBytes());
        httpURLConnection.getOutputStream().close();
      } 
      return Common.consumeResponse(httpURLConnection.getInputStream());
    } 
    HttpURLConnection urlc = new HttpURLConnection(new VRL(url), new Properties());
    urlc.requestProps.put("Authorization", getSignRequest(urlc.date));
    urlc.setRequestMethod(this.method);
    if (doOutput) {
      urlc.setDoOutput(true);
      urlc.getOutputStream().write(queryString.getBytes());
      urlc.getOutputStream().close();
    } 
    return Common.consumeResponse(urlc.getInputStream());
  }
  
  public String getSignRequest(Date d) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
    return "Basic " + Base64.encodeBytes((String.valueOf(this.ikey) + ":" + signHMAC(this.skey, canonRequest(this.sdf_rfc2822.format(d)))).getBytes("UTF8"));
  }
  
  protected String signHMAC(String skey, String msg) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
    return bytes_to_hex(hmacSha1(skey.getBytes("UTF8"), msg.getBytes("UTF8")));
  }
  
  protected String canonRequest(String date) throws UnsupportedEncodingException {
    return String.valueOf(date) + "\n" + this.method.toUpperCase() + "\n" + this.host.toLowerCase() + "\n" + this.uri + "\n" + createQueryString();
  }
  
  private String createQueryString() throws UnsupportedEncodingException {
    ArrayList key_order = new ArrayList();
    Enumeration keys_enum = this.params.keys();
    while (keys_enum.hasMoreElements())
      key_order.add(keys_enum.nextElement()); 
    Collections.sort(key_order);
    String args = "";
    for (int x = 0; x < key_order.size(); x++)
      args = String.valueOf(args) + ((x > 0) ? "&" : "") + URLEncoder.encode((String)key_order.get(x), "UTF-8").replace("+", "%20").replace("*", "%2A").replace("%7E", "~") + "=" + URLEncoder.encode(this.params.getProperty((String)key_order.get(x)), "UTF-8").replace("+", "%20").replace("*", "%2A").replace("%7E", "~"); 
    return args.trim();
  }
  
  public static byte[] hmacSha1(byte[] key_bytes, byte[] text_bytes) throws NoSuchAlgorithmException, InvalidKeyException {
    Mac hmacSha1 = Mac.getInstance("HmacSHA1");
    hmacSha1.init(new SecretKeySpec(key_bytes, "RAW"));
    return hmacSha1.doFinal(text_bytes);
  }
  
  public static String bytes_to_hex(byte[] b) {
    String result = "";
    for (int i = 0; i < b.length; i++)
      result = String.valueOf(result) + Integer.toString((b[i] & 0xFF) + 256, 16).substring(1); 
    return result;
  }
}
