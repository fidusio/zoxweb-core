/*
 * Copyright (c) 2012-2017 ZoxWeb.com LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.zoxweb.shared.security;

import org.zoxweb.shared.data.SetNameDescriptionDAO;
import org.zoxweb.shared.util.*;

import java.util.List;

@SuppressWarnings("serial")
public class KeyStoreInfo
    extends SetNameDescriptionDAO {

    public enum Param
        implements GetNVConfig
    {
        KEY_STORE(NVConfigManager.createNVConfig("key_store", "Key store", "KeyStore", true, true, false, String.class, null)),
        KEY_STORE_TYPE(NVConfigManager.createNVConfig("key_store_type", "Key store type", "KeyStoreType", true, true, false, String.class, null)),
        KEY_STORE_PASSWORD(NVConfigManager.createNVConfig("key_store_password", "Key store password", "KeyStorePassword", true, true, false, String.class, null)),
        ALIAS(NVConfigManager.createNVConfig("alias", "Alias", "Alias", true, true, false, String.class, null)),
        ALIAS_PASSWORD(NVConfigManager.createNVConfig("alias_password", "key password", "KeyPassword", true, true, false, String.class, null)),
        TRUST_STORE(NVConfigManager.createNVConfig("trust_store", "TrustStore  source", "TrustStoreSource", false, true, String.class)),
        TRUST_STORE_PASSWORD(NVConfigManager.createNVConfig("trust_store_password", "TrustStore Password", "TrustStorePassword", false, true, String.class)),
        PROTOCOLS(NVConfigManager.createNVConfig("protocols", "TrustStore Password", "TrustStorePassword", false, true, NVStringList.class)),
        CIPHERS(NVConfigManager.createNVConfig("ciphers", "TrustStore Password", "TrustStorePassword", false, true, NVStringList.class)),
        ;

        private final NVConfig nvc;

        Param(NVConfig nvc)
        {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig()
        {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_KEY_STORE_INFO = new NVConfigEntityPortable(
            "key_store_info",
            null ,
            KeyStoreInfo.class.getSimpleName(),
            true,
            false,
            false,
            false,
            KeyStoreInfo.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
    );

    public KeyStoreInfo()
    {
        super(NVC_KEY_STORE_INFO);
    }

    public String getKeyStore()
    {
        return lookupValue(Param.KEY_STORE);
    }

    public void setKeyStore(String keyStore)
    {
        setValue(Param.KEY_STORE, keyStore);
    }
    
    
    public String getKeyStoreType()
    {
        return lookupValue(Param.KEY_STORE_TYPE);
    }

    public void setKeyStoreType(String keyStoreType)
    {
        setValue(Param.KEY_STORE_TYPE, keyStoreType);
    }

    public String getKeyStorePassword()
    {
        return lookupValue(Param.KEY_STORE_PASSWORD);
    }

    public byte[] getKeyStorePasswordAsBytes()
    {
        return SharedStringUtil.hexToBytes(getKeyStorePassword());
    }

    public void setKeyStorePassword(String keyStorePassword)
    {
        setValue(Param.KEY_STORE_PASSWORD, keyStorePassword);
    }

    public void setKeyStorePassword(byte[] keyStorePassword)
    {
        setValue(Param.KEY_STORE_PASSWORD, SharedStringUtil.bytesToHex(keyStorePassword));
    }

    public String getAlias()
    {
        return lookupValue(Param.ALIAS);
    }

    public void setAlias(String alias)
    {
        setValue(Param.ALIAS, alias);
    }

    public String getAliasPassword()
    {
        return lookupValue(Param.ALIAS_PASSWORD);
    }

    public byte[] getAliasPasswordAsBytes()
    {
        return SharedStringUtil.hexToBytes(getAliasPassword());
    }

    public void setAliasPassword(String aliasPassword)
    {
        setValue(Param.ALIAS_PASSWORD, aliasPassword);
    }

    public void setAliasPassword(byte[] aliasPassword)
    {
        setValue(Param.ALIAS_PASSWORD, SharedStringUtil.bytesToHex(aliasPassword));
    }

    public String getTrustStore()
    {
        return lookupValue(Param.TRUST_STORE);
    }
    public void setTrustStore(String src)
    {
        setValue(Param.TRUST_STORE, src);
    }


    public String getTrustStorePassword()
    {
        return lookupValue(Param.TRUST_STORE_PASSWORD);
    }
    public void setTrustStorePassword(String tsPassword)
    {
        setValue(Param.TRUST_STORE_PASSWORD, tsPassword);
    }

    public void setTrustStorePassword(byte[] aliasPassword)
    {
        setValue(Param.TRUST_STORE_PASSWORD, SharedStringUtil.bytesToHex(aliasPassword));
    }

    public byte[] getTrustStorePasswordAsBytes()
    {
        return SharedStringUtil.hexToBytes(getTrustStorePassword());
    }

    /**
     * Supported protocols TLSv1 ...
     * @return
     */
    public List<String> getProtocols()
    {
        return lookupValue(Param.PROTOCOLS);
    }

    /**
     * Supported ciphers
     * @return
     */
    public List<String> getCiphers()
    {
        return lookupValue(Param.CIPHERS);
    }
}