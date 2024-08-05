package org.zoxweb.shared.security;



import org.zoxweb.server.util.GSONUtil;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class KeyStoreInfoTest {

    public static final String KEYSTORE = "xlogistx-store-mk.jck";
    public static final String KEYSTORE_PASSWORD = "xlogistx-pwd";
    public static final String ALIAS = "xlogistx-store-mk";
    public static final String ALIAS_PASSWORD = "xlogistx-pwd";

    @Test
    public void testKeyStoreInfoDAO()
    {
        KeyStoreInfo keyStoreInfo = new KeyStoreInfo();
        keyStoreInfo.setKeyStore(KEYSTORE);
        keyStoreInfo.setKeyStorePassword(KEYSTORE_PASSWORD.getBytes());
        keyStoreInfo.setAlias(ALIAS);
        keyStoreInfo.setAliasPassword(ALIAS_PASSWORD.getBytes());

        assertEquals(KEYSTORE, keyStoreInfo.getKeyStore());
        assertEquals(KEYSTORE_PASSWORD, new String(keyStoreInfo.getKeyStorePasswordAsBytes()));
        assertEquals(ALIAS, keyStoreInfo.getAlias());
        assertEquals(ALIAS_PASSWORD, new String(keyStoreInfo.getAliasPasswordAsBytes()));
    }


    @Test
    public void testKeyStoreJSON(){

        KeyStoreInfo keyStoreInfo = new KeyStoreInfo();
        keyStoreInfo.setKeyStore(KEYSTORE);
        keyStoreInfo.setKeyStorePassword(KEYSTORE_PASSWORD.getBytes());
        keyStoreInfo.setAlias(ALIAS);
        keyStoreInfo.setAliasPassword(ALIAS_PASSWORD.getBytes());
        keyStoreInfo.setKeyStoreType("jsk");
        keyStoreInfo.setTrustStore("truststore.jsk");
        keyStoreInfo.setTrustStorePassword("tsPassword");
        keyStoreInfo.getProtocols().add("TLSv1");
        String json1 = GSONUtil.toJSONDefault(keyStoreInfo);
        System.out.println(json1);
        KeyStoreInfo temp = GSONUtil.fromJSON(json1, KeyStoreInfo.class);
        String json2 = GSONUtil.toJSONDefault(temp);
        System.out.println(json2);
        assertEquals(json1, json2);
    }
}