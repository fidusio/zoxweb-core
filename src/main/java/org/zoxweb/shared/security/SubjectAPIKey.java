
/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
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


import org.zoxweb.shared.data.PropertyDAO;
import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.util.Const.Status;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.SharedBase64.Base64Type;

import java.util.Date;


/**
 * A persistable API-key credential that identifies a subject to a system without a
 * username/password exchange. It holds the key material itself plus the metadata that
 * governs its use: the owning {@link #getPrincipalID() principal}, the
 * {@link #getSystemID() system} the key is scoped to, a lifecycle {@link Status}, an
 * optional {@link #getExpiryDate() expiry}, and a flag requesting per-request
 * {@link #isTimeStampRequired() timestamp} validation (a replay-protection measure).
 *
 * <p>The {@code api_key} attribute is declared with {@link FilterType#ENCRYPT}, so the
 * secret is encrypted at rest by the persistence layer. In memory the key is held as a
 * URL-safe Base64 string; use {@link #setAPIKey(byte[])} / {@link #getAPIKeyAsBytes()}
 * to write and read the raw bytes.</p>
 *
 * <p>It is a {@link CredentialInfo} of type {@link CredentialInfo.Type#API_KEY}, so it
 * can be stored and resolved through the same credential machinery as passwords and
 * other credential kinds (see {@link DomainSecurityManager}). Its
 * {@link #getSubjectID() subject ID} is an alias of its {@link #getPrincipalID()
 * principal ID} - the two are the same value.</p>
 *
 * <p>This is a concrete DAO but also serves as a base for richer key types (e.g.
 * {@code AppDeviceDAO}) through the {@link #SubjectAPIKey(NVConfigEntity) protected
 * constructor}.</p>
 *
 * Created on 7/13/17
 */
@SuppressWarnings("serial")
public class SubjectAPIKey
        extends PropertyDAO
        implements SubjectID<String>,
        SystemID<String>, PrincipalID<String>, CredentialInfo {



    /**
     * The persisted attributes of a {@code SubjectAPIKey}, each backed by an
     * {@link NVConfig}.
     */
    public enum Param
            implements GetNVConfig {
        PRINCIPAL_ID(NVConfigManager.createNVConfig("principal_id", "Principal ID", "PrincipalID", false, false, String.class)),
        SYSTEM_ID(NVConfigManager.createNVConfig("system_id", "System ID", "SystemID", true, false, String.class)),
        API_KEY(NVConfigManager.createNVConfig("api_key", "API Key", "APIKey", true, false, false, String.class, FilterType.ENCRYPT)),
        STATUS(NVConfigManager.createNVConfig("status", "Status", "Status", true, false, Status.class)),
        TS_REQUIRED(NVConfigManager.createNVConfig("ts_required", "The timestamp is required", "TimeStampRequired", false, false, Boolean.class)),
        EXPIRY_DATE(NVConfigManager.createNVConfig("expiry_date", "The expiry timestamp", "Expired", false, false, false, true, Date.class, null)),
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

    /** The {@link NVConfigEntity} metadata describing this DAO's persisted shape. */
    public static final NVConfigEntity NVC_SUBJECT_API_KEY = new NVConfigEntityPortable(
            "subject_api_key",
            null,
            SubjectAPIKey.class.getSimpleName(),
            true,
            false,
            false,
            false,
            SubjectAPIKey.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            PropertyDAO.NVC_PROPERTY_DAO
    );

    /** Creates an empty API key backed by {@link #NVC_SUBJECT_API_KEY}. */
    public SubjectAPIKey() {
        super(NVC_SUBJECT_API_KEY);
    }

    /**
     * Constructor for subclasses that extend the API-key schema.
     *
     * @param nvce the metadata of the extending type
     */
    protected SubjectAPIKey(NVConfigEntity nvce) {
        super(nvce);
    }



    /**
     * @return the principal that owns this key, or {@code null} if unset
     */
    @Override
    public String getPrincipalID() {
        return lookupValue(Param.PRINCIPAL_ID);
    }

    /**
     * Sets the principal that owns this key.
     *
     * @param id the owning principal identifier
     */
    @Override
    public void setPrincipalID(String id) {
        setValue(Param.PRINCIPAL_ID, id);
    }

    /**
     * Returns the subject ID.
     *
     * @return The subject ID.
     */
    @Override
    public String getSubjectID() {
        return getPrincipalID();
    }

    /**
     * Sets the subject ID.
     *
     * @param id to be set.
     */
    @Override
    public void setSubjectID(String id) {
        setPrincipalID(id);
    }

    /**
     * @return always {@link CredentialInfo.Type#API_KEY}
     */
    @Override
    public Type getCredentialType() {
        return Type.API_KEY;
    }

    /**
     * Sets the key material, storing it as a URL-safe Base64 string (encrypted at rest).
     *
     * @param secret the raw key bytes
     */
    public void setAPIKey(byte[] secret) {
        setValue(Param.API_KEY, SharedBase64.encodeAsString(Base64Type.URL, secret));
    }

    /**
     * @return the key material as its stored URL-safe Base64 string, or {@code null}
     * if unset
     */
    public String getAPIKey() {
        return lookupValue(Param.API_KEY);
    }

    /**
     * @return the raw key bytes decoded from the stored Base64 form, or {@code null}
     * if no key is set
     */
    public byte[] getAPIKeyAsBytes() {
        String secret = getAPIKey();
        if (secret != null) {
            return SharedBase64.decode(Base64Type.URL, secret);
        }

        return null;
    }

    /**
     * @return the lifecycle status of this key (e.g. {@link Status#ACTIVE},
     * {@link Status#EXPIRED}), or {@code null} if unset
     */
    public Status getStatus() {
        return lookupValue(Param.STATUS);
    }

    /**
     * Sets the lifecycle status of this key.
     *
     * @param status the status to apply
     */
    public void setStatus(Status status) {
        setValue(Param.STATUS, status);
    }

    /**
     * Creates a new key carrying only the source key's secret material. Note that no
     * other metadata (principal, system, status, expiry, timestamp flag) is copied.
     *
     * @param subjectAPIKey the key to copy from; must not be {@code null}
     * @return a new {@code SubjectAPIKey} holding the same key bytes
     * @throws NullPointerException if {@code subjectAPIKey} is {@code null}
     */
    public static SubjectAPIKey copy(SubjectAPIKey subjectAPIKey) {
        SUS.checkIfNulls("SubjectAPIKey is null.", subjectAPIKey);

        SubjectAPIKey ret = new SubjectAPIKey();
        //ret.setSubjectID(subjectAPIKey.getSubjectID());
        ret.setAPIKey(subjectAPIKey.getAPIKeyAsBytes());

        return ret;
    }


    /**
     * @return the expiry timestamp in milliseconds since the epoch, after which the key
     * is no longer valid
     */
    public long getExpiryDate() {
        return lookupValue(Param.EXPIRY_DATE);
    }

    /**
     * Sets the expiry timestamp.
     *
     * @param ts the expiry time in milliseconds since the epoch
     */
    public void setExpiryDate(long ts) {
        setValue(Param.EXPIRY_DATE, ts);
    }

    /**
     * @return {@code true} if requests presenting this key must include a timestamp
     * (used for replay protection)
     */
    public boolean isTimeStampRequired() {
        return lookupValue(Param.TS_REQUIRED);
    }


    /**
     * Sets whether requests presenting this key must include a timestamp.
     *
     * @param tsReq {@code true} to require a per-request timestamp
     */
    public void setTimeStampRequired(boolean tsReq) {
        setValue(Param.TS_REQUIRED, tsReq);
    }

    /**
     * @return the system this key is scoped to, or {@code null} if unset
     */
    @Override
    public String getSystemID() {
        return lookupValue(Param.SYSTEM_ID);
    }

    /**
     * Sets the system this key is scoped to.
     *
     * @param systemID the system identifier
     */
    @Override
    public void setSystemID(String systemID) {
        setValue(Param.SYSTEM_ID, systemID);
    }
}
