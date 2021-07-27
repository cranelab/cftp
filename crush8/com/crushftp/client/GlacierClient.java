package com.crushftp.client;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.AmazonGlacierClientBuilder;
import com.amazonaws.services.glacier.TreeHashGenerator;
import com.amazonaws.services.glacier.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.glacier.model.CompleteMultipartUploadResult;
import com.amazonaws.services.glacier.model.DeleteArchiveRequest;
import com.amazonaws.services.glacier.model.DescribeVaultOutput;
import com.amazonaws.services.glacier.model.GetJobOutputRequest;
import com.amazonaws.services.glacier.model.GetJobOutputResult;
import com.amazonaws.services.glacier.model.GlacierJobDescription;
import com.amazonaws.services.glacier.model.InitiateJobRequest;
import com.amazonaws.services.glacier.model.InitiateJobResult;
import com.amazonaws.services.glacier.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.glacier.model.InitiateMultipartUploadResult;
import com.amazonaws.services.glacier.model.JobParameters;
import com.amazonaws.services.glacier.model.ListJobsRequest;
import com.amazonaws.services.glacier.model.ListJobsResult;
import com.amazonaws.services.glacier.model.ListVaultsRequest;
import com.amazonaws.services.glacier.model.ListVaultsResult;
import com.amazonaws.services.glacier.model.UploadMultipartPartRequest;
import com.amazonaws.services.glacier.model.UploadMultipartPartResult;
import com.amazonaws.util.BinaryUtils;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

public class GlacierClient extends GenericClient {
  private String region_host = "glacier.us-east-1.amazonaws.com";
  
  private String region = "us-east-1";
  
  private String glacier_root = "./glacier/";
  
  private String vaultName0 = null;
  
  private Vector replicating = null;
  
  private String partSize = "1048576";
  
  private SimpleDateFormat yyyyMMddtHHmmssSSS = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S", Locale.US);
  
  private SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
  
  private AmazonGlacier sdk_client;
  
  private AWSStaticCredentialsProvider credential_provider;
  
  private int folder_delete_depth = 3;
  
  private Vector delete_xml = new Vector();
  
  public GlacierClient(String url, String header, Vector log) {
    super(header, log);
    if (!url.endsWith("/"))
      url = String.valueOf(url) + "/"; 
    this.url = url;
    VRL glacier_vrl = new VRL(url);
    this.region_host = glacier_vrl.getHost().toLowerCase();
    this.glacier_root = System.getProperty("crushftp.glacier_root", "./glacier/");
    this.replicating = (Vector)System.getProperties().get("crushftp.glacier_replicated");
    this.folder_delete_depth = Integer.parseInt(System.getProperty("crushftp.glacier_folder_delete_depth", "3"));
  }
  
  public String getRawXmlPath(String path) throws Exception {
    path = getPath(path);
    return String.valueOf(this.glacier_root) + this.vaultName0 + path;
  }
  
  private String getPath(String path) throws Exception {
    if (this.config.getProperty("delete_xml_representation_files", "false").equals("true"))
      synchronized (this.delete_xml) {
        Vector temp = new Vector();
        for (int x = 0; x < this.delete_xml.size(); x++) {
          Properties p = this.delete_xml.get(x);
          if (System.currentTimeMillis() - Long.parseLong(p.getProperty("time")) < 1000L) {
            Common.recurseDelete(String.valueOf(this.glacier_root) + this.vaultName0 + p.getProperty("path"), false);
            temp.add(p);
          } 
        } 
        if (temp.size() > 0)
          this.delete_xml.removeAll(temp); 
      }  
    if (this.vaultName0 == null) {
      String vault_name = path.substring(1, path.indexOf("/", 1));
      Properties vault = stat("/" + vault_name);
      if (vault == null)
        throw new Exception("Could not found vault! Path : " + path); 
      this.vaultName0 = vault_name;
      (new File_S(String.valueOf(this.glacier_root) + this.vaultName0)).mkdirs();
    } 
    return Common.dots(path.substring(path.indexOf("/", 1)));
  }
  
  public static void writeFs(String glacier_root, String vaultName0, Vector replicating, String path0, Properties p) throws Exception {
    String path = Common.dots(path0);
    (new File_S(String.valueOf(glacier_root) + vaultName0 + Common.all_but_last(path))).mkdirs();
    Common.writeXMLObject(String.valueOf(glacier_root) + vaultName0 + path, p, "glacier");
    if (replicating != null) {
      Properties p2 = new Properties();
      p2.put("vaultName0", vaultName0);
      p2.put("path", path0);
      p2.put("data", Common.CLONE(p));
      replicating.addElement(p2);
    } 
  }
  
  public String login2(String username, String password, String clientid) throws Exception {
    this.config.put("username", username.trim());
    this.config.put("password", password.trim());
    try {
      AmazonGlacierClientBuilder builder = AmazonGlacierClientBuilder.standard();
      BasicAWSCredentials awsCreds = new BasicAWSCredentials(this.config.getProperty("username"), this.config.getProperty("password"));
      AWSStaticCredentialsProvider credential_provider = new AWSStaticCredentialsProvider((AWSCredentials)awsCreds);
      ProfileCredentialsProvider credentials = new ProfileCredentialsProvider();
      this.region = this.region_host.substring(8).substring(0, this.region_host.substring(8).indexOf("."));
      this.sdk_client = (AmazonGlacier)builder.withCredentials((AWSCredentialsProvider)credential_provider).withRegion(this.region).build();
    } catch (Exception e) {
      log(e);
      throw e;
    } 
    return "Success";
  }
  
  public Vector list(String path, Vector list) throws Exception {
    this.vaultName0 = null;
    if (path.equals("/")) {
      try {
        ListVaultsRequest lvr = new ListVaultsRequest();
        lvr.withRequestCredentialsProvider((AWSCredentialsProvider)this.credential_provider);
        lvr.setLimit("10");
        ListVaultsResult result = this.sdk_client.listVaults(lvr);
        listVaults(list, result);
        while (result.getMarker() != null) {
          lvr.setMarker(result.getMarker());
          result = this.sdk_client.listVaults(lvr);
          listVaults(list, result);
        } 
      } catch (Exception e) {
        log(e);
        throw e;
      } 
    } else {
      path = getPath(path);
      if (!(new File_S(String.valueOf(this.glacier_root) + this.vaultName0 + path)).exists())
        throw new Exception("No such folder: \"" + this.glacier_root + this.vaultName0 + path + "\""); 
      File_S[] f = (File_S[])(new File_S(String.valueOf(this.glacier_root) + this.vaultName0 + path)).listFiles();
      for (int x = 0; f != null && x < f.length; x++) {
        if (!f[x].getName().equals(".DS_Store")) {
          Date d = new Date(f[x].lastModified());
          Properties p = null;
          if (f[x].isFile()) {
            p = (Properties)Common.readXMLObject(f[x]);
          } else {
            p = new Properties();
          } 
          String line = String.valueOf(f[x].isDirectory() ? "d" : "-") + "rwxrwxrwx   1    owner   group   " + p.getProperty("size", "0") + "   " + this.yyyyMMddHHmmss.format(d) + "   " + this.dd.format(d) + " " + this.yyyy.format(d) + " /" + f[x].getName();
          Properties stat = parseStat(line);
          stat.put("url", String.valueOf(this.url) + this.vaultName0 + path + stat.getProperty("name") + (f[x].isDirectory() ? "/" : ""));
          if (p.containsKey("archiveId"))
            stat.put("archiveId", p.getProperty("archiveId")); 
          list.addElement(stat);
        } 
      } 
    } 
    return list;
  }
  
  private void listVaults(Vector list, ListVaultsResult result) throws Exception {
    List vaults = result.getVaultList();
    for (int x = 0; x < vaults.size(); x++) {
      DescribeVaultOutput dVO = vaults.get(x);
      Date d = this.yyyyMMddtHHmmssSSS.parse(dVO.getCreationDate());
      String line = "drwxrwxrwx   1    owner   group   " + dVO.getSizeInBytes() + "   " + this.yyyyMMddHHmmss.format(d) + "   " + this.dd.format(d) + " " + this.yyyy.format(d) + " /" + dVO.getVaultName();
      Properties stat = parseStat(line);
      System.out.println(line);
      stat.put("url", String.valueOf(this.url) + stat.getProperty("name"));
      stat.put("arn", dVO.getVaultARN());
      stat.put("numberOfArchives", dVO.getNumberOfArchives());
      list.add(stat);
    } 
  }
  
  public Properties stat(String path) throws Exception {
    if (path.endsWith(":filetree"))
      path = path.substring(0, path.indexOf(":filetree") - 1); 
    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1); 
    Vector v = new Vector();
    list(Common.all_but_last(path), v);
    for (int x = 0; x < v.size(); x++) {
      Properties p = v.elementAt(x);
      if (p.getProperty("name").equals(Common.last(path)))
        return p; 
    } 
    return null;
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary) throws Exception {
    throw new Exception("Download not supported!");
  }
  
  protected OutputStream upload3(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    String path_0 = getPath(path);
    if (this.vaultName0.equals(""))
      throw new Exception("Missing Vault!"); 
    SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US);
    String archive_id = "<m><v>4</v><p>" + Base64.encodeBytes(path_0.substring(1).getBytes("UTF8")) + "</p><lm>" + df.format(new Date()) + "</lm></m>";
    InitiateMultipartUploadRequest request = (InitiateMultipartUploadRequest)(new InitiateMultipartUploadRequest()).withVaultName(this.vaultName0).withArchiveDescription(archive_id).withPartSize(this.config.getProperty("partSize", this.partSize)).withRequestCredentialsProvider((AWSCredentialsProvider)this.credential_provider);
    InitiateMultipartUploadResult result = this.sdk_client.initiateMultipartUpload(request);
    int part_size = Integer.parseInt(this.config.getProperty("partSize", this.partSize));
    String uploadId = result.getUploadId();
    this.out = new GlacierClient$1$OutputWrapper(this, part_size, uploadId, path_0);
    return this.out;
  }
  
  public boolean delete(String path) throws Exception {
    Properties p = stat(path);
    if (p == null);
    if (this.vaultName0 == null)
      throw new Exception("Vault cannot be deleted!"); 
    if (p.getProperty("type").equalsIgnoreCase("FILE")) {
      try {
        DeleteArchiveRequest dar = (new DeleteArchiveRequest()).withVaultName(this.vaultName0).withArchiveId(p.getProperty("archiveId"));
        dar.withRequestCredentialsProvider((AWSCredentialsProvider)this.credential_provider);
        this.sdk_client.deleteArchive(dar);
        Common.recurseDelete(String.valueOf(this.glacier_root) + this.vaultName0 + getPath(path), false);
      } catch (Exception e) {
        log(e);
        throw e;
      } 
    } else {
      String path0 = getPath(path);
      Vector items = new Vector();
      Common.appendListing_S(String.valueOf(this.glacier_root) + this.vaultName0 + path0, items, "", this.folder_delete_depth, false);
      for (int x = 0; x < items.size(); x++) {
        File_S f = items.elementAt(x);
        if (!f.getName().equals(".DS_Store") && 
          !f.isDirectory()) {
          Properties file = (Properties)Common.readXMLObject(f);
          try {
            DeleteArchiveRequest dar = (new DeleteArchiveRequest()).withVaultName(this.vaultName0).withArchiveId(file.getProperty("archiveId"));
            dar.withRequestCredentialsProvider((AWSCredentialsProvider)this.credential_provider);
            this.sdk_client.deleteArchive(dar);
            Common.recurseDelete(f.getAbsolutePath(), false);
          } catch (Exception e) {
            log(e);
            throw e;
          } 
        } 
      } 
      Common.recurseDelete(String.valueOf(this.glacier_root) + this.vaultName0 + getPath(path), false);
    } 
    return true;
  }
  
  public boolean rename(String rnfr, String rnto) throws Exception {
    rnfr = getPath(rnfr);
    rnto = getPath(rnto);
    if (this.vaultName0 == null)
      throw new Exception("Vault cannot be renamed!"); 
    return (new File_S(String.valueOf(this.glacier_root) + this.vaultName0 + rnfr)).renameTo(new File_S(String.valueOf(this.glacier_root) + this.vaultName0 + rnto));
  }
  
  public boolean makedir(String path0) throws Exception {
    path0 = getPath(path0);
    if (!path0.endsWith("/"))
      path0 = String.valueOf(path0) + "/"; 
    if (path0.equals("/"))
      return true; 
    return (new File_S(String.valueOf(this.glacier_root) + this.vaultName0 + path0)).mkdirs();
  }
  
  public boolean makedirs(String path0) throws Exception {
    return makedir(path0);
  }
  
  public boolean mdtm(String path, long modified) throws Exception {
    String path0 = getPath(path);
    return (new File_S(String.valueOf(this.glacier_root) + this.vaultName0 + path0)).setLastModified(modified);
  }
  
  public String createGetInventoryJob() throws Exception {
    if (this.vaultName0 == null || this.vaultName0.equals(""))
      throw new Exception("Vault must be specified!"); 
    InitiateJobRequest initJobRequest = (new InitiateJobRequest()).withVaultName(this.vaultName0).withJobParameters((new JobParameters()).withType("inventory-retrieval"));
    InitiateJobResult initJobResult = this.sdk_client.initiateJob(initJobRequest);
    return initJobResult.getJobId();
  }
  
  public String getJobStatus(String job_id) throws Exception {
    if (this.vaultName0 == null || this.vaultName0.equals(""))
      throw new Exception("Vault must be specified!"); 
    ListJobsRequest ljr = new ListJobsRequest();
    ljr.withVaultName(this.vaultName0);
    ListJobsResult listJobResult = this.sdk_client.listJobs(ljr);
    List jobs = listJobResult.getJobList();
    for (int x = 0; x < jobs.size(); x++) {
      GlacierJobDescription gjd = jobs.get(x);
      if (gjd.getJobId().equals(job_id))
        return gjd.getStatusCode(); 
    } 
    return "";
  }
  
  public String downloadInventory(String job_id) throws Exception {
    GetJobOutputRequest jobOutputRequest = (new GetJobOutputRequest()).withVaultName(this.vaultName0).withJobId(job_id);
    GetJobOutputResult jobOutputResult = this.sdk_client.getJobOutput(jobOutputRequest);
    return URLConnection.consumeResponse(jobOutputResult.getBody());
  }
  
  public String getVaultName() {
    return this.vaultName0;
  }
  
  public void logout() throws Exception {
    if (this.config.getProperty("delete_xml_representation_files", "false").equals("true"))
      synchronized (this.delete_xml) {
        for (int x = 0; x < this.delete_xml.size(); x++) {
          Properties p = this.delete_xml.get(x);
          Common.recurseDelete(String.valueOf(this.glacier_root) + this.vaultName0 + p.getProperty("path"), false);
        } 
        this.delete_xml.clear();
      }  
  }
  
  private String calculateTreeHash(Vector checksums) throws Exception {
    Vector hashes = new Vector();
    hashes.addAll(checksums);
    while (hashes.size() > 1) {
      Vector treeHashes = new Vector();
      for (int i = 0; i < hashes.size() / 2; i++) {
        byte[] firstPart = hashes.get(2 * i);
        byte[] secondPart = hashes.get(2 * i + 1);
        byte[] concatenation = new byte[firstPart.length + secondPart.length];
        System.arraycopy(firstPart, 0, concatenation, 0, firstPart.length);
        System.arraycopy(secondPart, 0, concatenation, firstPart.length, secondPart.length);
        try {
          treeHashes.add(computeSHA256Hash(concatenation));
        } catch (Exception e) {
          log(e);
          throw new Exception("Unable to compute hash", e);
        } 
      } 
      if (hashes.size() % 2 == 1)
        treeHashes.add(hashes.get(hashes.size() - 1)); 
      hashes = treeHashes;
    } 
    return BinaryUtils.toHex(hashes.get(0));
  }
  
  private byte[] computeSHA256Hash(byte[] data) throws Exception {
    BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(data));
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      byte[] buffer = new byte[16384];
      int bytesRead = -1;
      while ((bytesRead = bis.read(buffer, 0, buffer.length)) != -1)
        messageDigest.update(buffer, 0, bytesRead); 
      return messageDigest.digest();
    } finally {
      try {
        bis.close();
      } catch (Exception e) {
        log(e);
      } 
    } 
  }
}
