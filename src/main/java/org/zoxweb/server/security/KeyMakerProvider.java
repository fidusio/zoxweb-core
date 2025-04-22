package org.zoxweb.server.security;


import org.zoxweb.shared.api.APIDataStore;
import org.zoxweb.shared.crypto.EncryptedKey;
import org.zoxweb.shared.crypto.KeyLockType;
import org.zoxweb.shared.db.QueryMatchString;
import org.zoxweb.shared.security.AccessException;
import org.zoxweb.shared.security.KeyMaker;
import org.zoxweb.shared.security.SubjectIdentifier;
import org.zoxweb.shared.util.*;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.security.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public final class KeyMakerProvider
    implements KeyMaker {

  public final static KeyMakerProvider SINGLETON = new KeyMakerProvider();
  private static final Logger log = Logger.getLogger(KeyMakerProvider.class.getSimpleName());

  private volatile SecretKey masterKey = null;
  private HashMap<String, EncryptedKey> keyMap = new HashMap<String, EncryptedKey>();


  private KeyMakerProvider()
  {
    //log.info("key maker created");
  }


  public synchronized void setMasterKey(KeyStore keystore, String alias, String aliasPassword)
      throws NullPointerException, IllegalArgumentException, AccessException {
    SUS.checkIfNulls("Null parameters", keystore, alias);
    try {
      if (!keystore.containsAlias(alias)) {
        throw new IllegalArgumentException("Alias for key not found");
      }
      setMasterKey((SecretKey) CryptoUtil.getKeyFromKeyStore(keystore, alias, aliasPassword));
      log.info("MK loaded");
    } catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
      throw new AccessException(e.getMessage());
    }

  }

  public synchronized void setMasterKey(SecretKey key)
      throws NullPointerException, IllegalArgumentException, AccessException {
    masterKey = key;
  }


  public byte[] getMasterKey()
      throws NullPointerException,
      IllegalArgumentException,
      AccessException {
    return getMasterSecretKey().getEncoded();
  }

  public SecretKey getMasterSecretKey()
          throws NullPointerException,
          IllegalArgumentException,
          AccessException {
    if (masterKey == null) {
      throw new AccessException("MasterKey not set");
    }
    return masterKey;
  }



  public EncryptedKey createSubjectIDKey(SubjectIdentifier subjectID, final byte[] encryptionKey)
      throws NullPointerException, IllegalArgumentException, AccessException {
    SUS.checkIfNulls("subjectID is null or encryptionKey is null.", subjectID, encryptionKey);

    if (subjectID.getGUID() == null || subjectID.getSubjectGUID() == null) {
      throw new IllegalArgumentException("Get user ID is null.");
    }

    EncryptedKey ekd;
    try {
      ekd = CryptoUtil.createEncryptedKey(encryptionKey);
    } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
      throw new AccessException(e.getMessage());
    }
    ekd.setObjectReference(subjectID);
    ekd.setKeyLockType(KeyLockType.USER_ID);
    ekd.setSubjectGUID(subjectID.getSubjectGUID());
    ekd.setGUID(subjectID.getGUID());
    return ekd;
  }


  public EncryptedKey createNVEntityKey(APIDataStore<?, ?> dataStore, NVEntity nve, final byte[] key)
      throws NullPointerException, IllegalArgumentException, AccessException {
    SUS.checkIfNulls("User ID is null.", nve, key);
    if (nve.getSubjectGUID() == null || nve.getGUID() == null) {
      throw new IllegalArgumentException("NVE SubjectGUID or GUID is null.");
    }

    EncryptedKey ekd = lookupEncryptedKeyDOA(dataStore, nve);
    try {
      if (ekd == null) {
        ekd = CryptoUtil.createEncryptedKey(key);
        ekd.setObjectReference(nve);
        ekd.setKeyLockType(KeyLockType.USER_ID);
        ekd.setSubjectGUID(nve.getSubjectGUID());
        ekd.setGUID(nve.getGUID());
        ekd = dataStore.insert(ekd);
      }
    } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
      throw new AccessException(e.getMessage());
    }
    return ekd;
  }

  public byte[] getKey(APIDataStore<?, ?> dataStore, final byte[] key, String... chainedIDs)
      throws NullPointerException, IllegalArgumentException, AccessException
  {
    SUS.checkIfNulls("Null decryption key parameters", dataStore, chainedIDs);

    byte[] tempKey = key != null ? key : getMasterKey();
    System.out.println(Arrays.toString(chainedIDs));

    for (int i = 0; i < chainedIDs.length; i++)
    {
      String id = chainedIDs[i];
      try
      {
        EncryptedKey ekd = lookupEncryptedKeyDOA(dataStore, id);
        tempKey = CryptoUtil.decryptEncryptedData(ekd, tempKey);
      } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | SignatureException e) {
        e.printStackTrace();
        throw new AccessException(e.getMessage());
      }
    }

    return tempKey;
  }


  public EncryptedKey lookupEncryptedKeyDOA(APIDataStore<?, ?> dataStore, NVEntity nve)
      throws NullPointerException, IllegalArgumentException, AccessException {
    SUS.checkIfNulls("Null parameters", dataStore, nve);

    return lookupEncryptedKeyDOA(dataStore, nve.getGUID(), nve.getSubjectGUID());
  }

  public synchronized EncryptedKey lookupEncryptedKeyDOA(APIDataStore<?, ?> dataStore,
                                                         String dataRefGUID, String subjectGUID)
      throws NullPointerException, IllegalArgumentException, AccessException {
    SUS.checkIfNulls("Null parameters", dataStore, dataRefGUID);
    EncryptedKey ekd = keyMap.get(SharedUtil.toCanonicalID(':', dataRefGUID, subjectGUID));

    if (ekd == null) {
      List<EncryptedKey> keyMatches = dataStore
          .search(EncryptedKey.NVCE_ENCRYPTED_KEY, null,
                  new QueryMatchString(MetaToken.SUBJECT_GUID, subjectGUID, Const.RelationalOperator.EQUAL),
                  Const.LogicalOperator.AND,
                  new QueryMatchString(MetaToken.REFERENCE_GUID, dataRefGUID, Const.RelationalOperator.EQUAL) );
      if (keyMatches == null || keyMatches.size() != 1) {
        return null;
      }
      ekd = keyMatches.get(0);
      keyMap.put(SharedUtil.toCanonicalID(':', dataRefGUID, subjectGUID), ekd);
    }

    return ekd;
  }

  public synchronized final EncryptedKey lookupEncryptedKeyDOA(APIDataStore<?, ?> dataStore,
                                                               String dataRefGUID)
          throws NullPointerException, IllegalArgumentException, AccessException {
    SUS.checkIfNulls("Null parameters", dataStore, dataRefGUID);
    EncryptedKey ekd = keyMap.get(dataRefGUID);

    if (ekd == null) {
      List<EncryptedKey> keyMatches = dataStore
              .search(EncryptedKey.NVCE_ENCRYPTED_KEY, null,
              new QueryMatchString(MetaToken.REFERENCE_GUID, dataRefGUID, Const.RelationalOperator.EQUAL) );
      if (keyMatches == null || keyMatches.size() != 1) {
        return null;
      }
      ekd = keyMatches.get(0);
      keyMap.put(dataRefGUID, ekd);
    }

    return ekd;
  }
}
