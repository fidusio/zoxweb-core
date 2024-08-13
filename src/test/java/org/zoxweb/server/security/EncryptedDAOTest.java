package org.zoxweb.server.security;


import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.UUID;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
//import org.junit.BeforeClass;
//import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zoxweb.shared.crypto.EncryptedDAO;
import org.zoxweb.shared.util.SharedStringUtil;

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
    EncryptedDAO ed = new EncryptedDAO();
    ed = CryptoUtil.encryptDAO(new EncryptedDAO(), KEY, DATA);
    ed.setGUID(UUID.randomUUID().toString());
    byte[] dataDecrypted = CryptoUtil.decryptEncryptedDAO(ed, KEY);
    assert (Arrays.equals(DATA, dataDecrypted));

  }

  @Test
  public void testEDHmacAll()
      throws InvalidKeyException, NullPointerException, IllegalArgumentException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, SignatureException {
    EncryptedDAO ed = new EncryptedDAO();
    ed.setHMACAll(true);
    ed.setGUID(UUID.randomUUID().toString());
    ed.setSubjectID(UUID.randomUUID().toString());
    ed = CryptoUtil.encryptDAO(ed, KEY, DATA);

    byte[] dataDecrypted = CryptoUtil.decryptEncryptedDAO(ed, KEY);
    assert (Arrays.equals(DATA, dataDecrypted));
  }


  @Test//(expected = SignatureException.class)
  public void testEDFailedSignature()
      throws InvalidKeyException, NullPointerException, IllegalArgumentException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, SignatureException {
    EncryptedDAO ed = new EncryptedDAO();
    ed.setHMACAll(true);
    ed.setGUID(UUID.randomUUID().toString());
    ed.setSubjectID(UUID.randomUUID().toString());
    ed = CryptoUtil.encryptDAO(ed, KEY, DATA);
    ed.setSubjectID(null);
    EncryptedDAO finalEd = ed;
    Assertions.assertThrows(SignatureException.class, ()->CryptoUtil.decryptEncryptedDAO(finalEd, KEY));
  }

}
