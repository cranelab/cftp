package com.crushftp.client;

import com.didisoft.pgp.KeyPairInformation;
import com.didisoft.pgp.KeyStore;

public class PgpUtil {
  public static void main(String[] args) {
    try {
      if (args[0].equals("clear")) {
        KeyStore store = new KeyStore();
        store.importPublicKey(args[1]);
        store.importPrivateKey(args[2], args[3]);
        KeyPairInformation[] kpi = store.listKeys();
        if (kpi[0].getValidDays() == 0) {
          System.out.println("No Expiration");
        } else {
          System.out.println("Current expiration:" + kpi[0].getExpirationDate());
        } 
        store.clearKeyExpirationTime(kpi[0].getKeyID(), args[3]);
        store.exportPublicKey(args[1], kpi[0].getUserIDs()[0], true);
        kpi = store.listKeys();
        System.out.println("Expiration cleared on " + args[1]);
      } 
    } catch (Exception e) {
      e.printStackTrace();
    } 
  }
}
