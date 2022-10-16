package org.zoxweb.shared.security;



import org.zoxweb.server.util.GSONUtil;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class KeyStoreInfoDAOTest {

    public static final String KEYSTORE = "xlogistx-store-mk.jck";
    public static final String KEYSTORE_PASSWORD = "xlogistx-pwd";
    public static final String ALIAS = "xlogistx-store-mk";
    public static final String ALIAS_PASSWORD = "xlogistx-pwd";

    @Test
    public void testKeyStoreInfoDAO()
    {
        KeyStoreInfoDAO keyStoreInfoDAO = new KeyStoreInfoDAO();
        keyStoreInfoDAO.setKeyStore(KEYSTORE);
        keyStoreInfoDAO.setKeyStorePassword(KEYSTORE_PASSWORD.getBytes());
        keyStoreInfoDAO.setAlias(ALIAS);
        keyStoreInfoDAO.setAliasPassword(ALIAS_PASSWORD.getBytes());

        assertEquals(KEYSTORE, keyStoreInfoDAO.getKeyStore());
        assertEquals(KEYSTORE_PASSWORD, new String(keyStoreInfoDAO.getKeyStorePasswordAsBytes()));
        assertEquals(ALIAS, keyStoreInfoDAO.getAlias());
        assertEquals(ALIAS_PASSWORD, new String(keyStoreInfoDAO.getAliasPasswordAsBytes()));
    }


    @Test
    public void testKeyStoreJSON(){

        KeyStoreInfoDAO keyStoreInfoDAO = new KeyStoreInfoDAO();
        keyStoreInfoDAO.setKeyStore(KEYSTORE);
        keyStoreInfoDAO.setKeyStorePassword(KEYSTORE_PASSWORD.getBytes());
        keyStoreInfoDAO.setAlias(ALIAS);
        keyStoreInfoDAO.setAliasPassword(ALIAS_PASSWORD.getBytes());
        keyStoreInfoDAO.setKeyStoreType("jsk");
        keyStoreInfoDAO.setTrustStore("truststore.jsk");
        keyStoreInfoDAO.setTrustStorePassword("tsPassword");
        keyStoreInfoDAO.getProtocols().add("TLSv1");
        String json1 = GSONUtil.toJSONDefault(keyStoreInfoDAO);
        System.out.println(json1);
        KeyStoreInfoDAO temp = GSONUtil.fromJSON(json1, KeyStoreInfoDAO.class);
        String json2 = GSONUtil.toJSONDefault(temp);
        System.out.println(json2);
        assertEquals(json1, json2);
    }
}