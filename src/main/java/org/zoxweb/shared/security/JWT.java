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


import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.data.SetNameDescriptionDAO;

import org.zoxweb.shared.util.AppID;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.LongIDGenerator;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.SharedUtil;

import java.util.Date;

@SuppressWarnings("serial")
public class JWT
        extends SetNameDescriptionDAO {

    public enum JWTField {
        HEADER,
        PAYLOAD,
        HASH
    }


    public enum Param
            implements GetNVConfig {
        JWT_HEADER(NVConfigManager.createNVConfig("header", "Header", "Header", true, true, NVGenericMap.class)),
        JWT_PAYLOAD(NVConfigManager.createNVConfig("payload", "Payload", "Payload", false, false, NVGenericMap.class)),
        JWT_HASH(NVConfigManager.createNVConfig("hash", "hash", "Hash", false, false, String.class)),
        ;

        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_JWT = new NVConfigEntityPortable("jwt",
            null,
            "JWT",
            true,
            false,
            false,
            false,
            JWT.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO);


    private final JWTPayload payload;
    private final JWTHeader header;

    public JWT() {
        super(NVC_JWT);
        payload = new JWTPayload(lookup(Param.JWT_PAYLOAD));
        header = new JWTHeader(lookup(Param.JWT_HEADER));
    }


    public JWT(JWTHeader header, JWTPayload payload) {
        this();
        this.header.setProperties(header.getProperties());
        this.payload.setProperties(payload.getProperties());


    }

    public JWTHeader getHeader() {
        return header;
    }


    public JWTPayload getPayload() {
        return payload;
    }

    /**
     *
     * @return string URL encoded base64
     */
    public String getHash() {
        return lookupValue(Param.JWT_HASH);
    }


    public void setHash(String hash) {
        setValue(Param.JWT_HASH, hash);
    }

    public static JWT createJWT(CryptoConst.JWTAlgo algorithm, String subjectID, AppID<String> appID) {
        return createJWT(algorithm, subjectID, appID.getDomainID(), appID.getAppID());
    }

    public static JWT createJWT(CryptoConst.JWTAlgo algorithm, String subjectID, String domainID, String appID) {

        JWT jwt = new JWT();
        JWTHeader jwtHeader = jwt.getHeader();
        jwtHeader.setJWTAlgorithm(algorithm);
        jwtHeader.setTokenType("JWT");

        JWTPayload jwtPayload = jwt.getPayload();
        jwtPayload.setDomainID(domainID);
        jwtPayload.setAppID(appID);
        jwtPayload.setSubjectID(subjectID);

        jwtPayload.setIssuedAt(new Date());
        jwtPayload.setNonce(LongIDGenerator.DEFAULT.nextID());
        return jwt;
    }
}