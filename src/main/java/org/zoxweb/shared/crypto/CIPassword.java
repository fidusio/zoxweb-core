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
package org.zoxweb.shared.crypto;

import org.zoxweb.shared.crypto.CryptoConst.HashType;
import org.zoxweb.shared.data.PropertyDAO;
import org.zoxweb.shared.security.CredentialInfo;
import org.zoxweb.shared.util.*;

/**
 * PasswordDAO
 *
 * @author mnael
 *
 */
@SuppressWarnings("serial")
public class CIPassword
        extends PropertyDAO
        implements CryptoBase, CredentialInfo {


    public enum CIProp
            implements GetName {
        SALT("salt"),
        HASH("hash"),
        ROUNDS("rounds"),
        VERSION("version"),
        MEMORY("memory"),
        PARALLELISM("parallelism"),
        ALGORITHM("algorithm"),
        ;
        private final String name;

        CIProp(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public enum Param
            implements GetNVConfig {
        ALGORITHM(NVConfigManager.createNVConfig(CIProp.ALGORITHM.getName(), "Algorithm name", "AlgorithmName", false, true, String.class)),
        ROUNDS(NVConfigManager.createNVConfig(CIProp.ROUNDS.getName(), "Hash algorithm rounds or iterations", "HashRounds", false, true, Integer.class)),
        SALT(NVConfigManager.createNVConfig(CIProp.SALT.getName(), "The password salt", "Salt", false, true, byte[].class)),
        HASH(NVConfigManager.createNVConfig(CIProp.HASH.getName(), "The password hash", "Hash", false, true, byte[].class)),


        ;

        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        @Override
        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public final static NVConfigEntity NVCE_CI_PASSWORD = new NVConfigEntityPortable("ci_password", null, "Password", false, true, false, false, CIPassword.class, SharedUtil.extractNVConfigs(Param.values()), null, false, PropertyDAO.NVC_PROPERTY_DAO);

    /**
     * The default constructor.
     */
    public CIPassword() {
        super(NVCE_CI_PASSWORD);
    }

    public synchronized void setAlgorithm(HashType hashType) {
        SUS.checkIfNulls("Null Message Digest", hashType);
        setAlgorithm(hashType.getName());
    }

    public synchronized void setAlgorithm(String algoName) {

        SUS.checkIfNulls("Null Message Digest", algoName);
        setValue(Param.ALGORITHM, DataEncoder.StringLower.encode(algoName));
    }


    public String getAlgorithm() {
        return lookupValue(Param.ALGORITHM);
    }


    public String getVersion() {
        return getProperties().getValue(CIProp.VERSION);
    }

    public void setVersion(String version) {
        getProperties().build(CIProp.VERSION, version);
    }

    public int getRounds() {

        return lookupValue(Param.ROUNDS);
    }

    public synchronized void setRounds(int rounds) {
        if (rounds < 1) {
            throw new IllegalArgumentException("Invalid iteration value:" + rounds);
        }

        setValue(Param.ROUNDS, rounds);
    }

    public synchronized byte[] getSalt() {
        return lookupValue(Param.SALT);
    }

    public synchronized void setSalt(byte[] salt) {
       setValue(Param.SALT, salt);
    }

    public synchronized byte[] getHash() {
        return lookupValue(Param.HASH);
    }

    public synchronized void setHash(byte[] hash) {
        setValue(Param.HASH, hash);
    }


    @Override
    public String toCanonicalID() {
        return getCanonicalID();
    }


    /**
     * @return
     */
    @Override
    public CredentialType getCredentialType() {
        return CredentialType.PASSWORD;
    }

}