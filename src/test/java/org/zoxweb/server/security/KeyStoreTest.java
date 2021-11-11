package org.zoxweb.server.security;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import javax.crypto.SecretKey;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.security.KeyStoreInfoDAO;
import org.zoxweb.shared.util.SharedStringUtil;

public class KeyStoreTest {

  public static void main(String... args) {
    try {
      int index = 0;
      KeyStoreInfoDAO ksid = GSONUtil
          .fromJSON(IOUtil.inputStreamToString(args[index++]), KeyStoreInfoDAO.class);
      KeyStore ks = CryptoUtil.loadKeyStore(new FileInputStream(ksid.getKeyStore()),
          ksid.getKeyStoreType(),
          ksid.getKeyStorePassword().toCharArray());

      SecretKey sk = (SecretKey) CryptoUtil
          .getKeyFromKeyStore(ks, ksid.getAlias(), ksid.getAliasPassword());
      System.out.println(SharedStringUtil.bytesToHex(sk.getEncoded()));

      KeyStore ksPkcs12 = KeyStore.getInstance("pkcs12");
      ksPkcs12.load(null, ksid.getKeyStorePassword().toCharArray());
      ksPkcs12.setKeyEntry(ksid.getAlias(), sk, null, null);

      System.out.println(
          SharedStringUtil.bytesToHex(ksPkcs12.getKey(ksid.getAlias(), null).getEncoded()));
      if (args.length > index) {
        FileOutputStream fos = new FileOutputStream(args[index++]);
        ksPkcs12.store(fos, ksid.getKeyStorePassword().toCharArray());
        IOUtil.close(fos);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
