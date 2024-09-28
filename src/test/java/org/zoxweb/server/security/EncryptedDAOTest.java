package org.zoxweb.server.security;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zoxweb.shared.crypto.EncryptedData;
import org.zoxweb.shared.util.SharedStringUtil;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.UUID;

public class EncryptedDAOTest {

  public final static byte[] KEY = SharedStringUtil.getBytes("PASSWORD");
  public final static byte[] DATA = SharedStringUtil
      .getBytes("The quick brown fox jumps over the lazy dog.");


  @BeforeAll
  public static void setUpBeforeClass() throws Exception {
  }

  @Test
  public void testED()
      throws InvalidKeyException, NullPointerException, IllegalArgumentException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, SignatureException {
    EncryptedData ed = new EncryptedData();
    ed = CryptoUtil.encryptData(new EncryptedData(), KEY, DATA);
    ed.setGUID(UUID.randomUUID().toString());
    byte[] dataDecrypted = CryptoUtil.decryptEncryptedData(ed, KEY);
    assert (Arrays.equals(DATA, dataDecrypted));

  }

  @Test
  public void testEDHmacAll()
      throws InvalidKeyException, NullPointerException, IllegalArgumentException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, SignatureException {
    EncryptedData ed = new EncryptedData();
    ed.setHMACAll(true);
    ed.setGUID(UUID.randomUUID().toString());
    ed.setSubjectGUID(UUID.randomUUID().toString());
    ed = CryptoUtil.encryptData(ed, KEY, DATA);

    byte[] dataDecrypted = CryptoUtil.decryptEncryptedData(ed, KEY);
    assert (Arrays.equals(DATA, dataDecrypted));
  }


  @Test//(expected = SignatureException.class)
  public void testEDFailedSignature()
      throws InvalidKeyException, NullPointerException, IllegalArgumentException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, SignatureException {
    EncryptedData ed = new EncryptedData();
    ed.setHMACAll(true);
    ed.setGUID(UUID.randomUUID().toString());
    ed.setSubjectGUID(UUID.randomUUID().toString());
    ed = CryptoUtil.encryptData(ed, KEY, DATA);
    ed.setSubjectGUID(null);
    EncryptedData finalEd = ed;
    Assertions.assertThrows(SignatureException.class, ()->CryptoUtil.decryptEncryptedData(finalEd, KEY));
  }

}
