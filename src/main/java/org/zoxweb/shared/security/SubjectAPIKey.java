
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


import org.zoxweb.shared.app.AppIDDefault;
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
 * URL-safe Base64 string; use {@link #setAPIKeyAsBytes(byte[])} / {@link #getAPIKeyAsBytes()}
 * to write and read the raw bytes.</p>
 *
 * <p><b>Purpose (2026-10-03).</b> A key serves exactly one of two purposes, carried by
 * {@link #getCredentialType()}:</p>
 * <ul>
 * <li>{@link CredentialInfo.Type#API_KEY} (the default) - a subject's credential for a
 * <b>third-party</b> API (an AI provider, a cloud service, ...). It never authenticates
 * anyone to this system: the owning subject logs in by other means, reads the key from
 * the datastore, which serves it in clear to its owner, and presents it to the third
 * party.</li>
 * <li>{@link CredentialInfo.Type#SYMMETRIC_KEY} - a <b>signing key</b>: the HMAC secret of
 * the JWT bearer tokens that log a subject in to this system. Only such a key may verify
 * a JWT ({@link #isSigningKey()}).</li>
 * </ul>
 * <p>A raw key is never a login credential and is never looked up by its secret.</p>
 *
 * <p>It is a {@link CredentialInfo}, so it
 * can be stored and resolved through the same credential machinery as passwords and
 * other credential kinds (see {@link DomainSecurityManager}). Its
 * {@link #getSubjectID() subject ID} is an alias of its {@link #getPrincipalID()
 * principal ID} - the two are the same value.</p>
 *
 * <p>This is a concrete DAO but also serves as a base for richer key types (e.g.
 * {@code AppDeviceInfo}) through the {@link #SubjectAPIKey(NVConfigEntity) protected
 * constructor}.</p>
 *
 * Created on 7/13/17
 */
@SuppressWarnings("serial")
public class SubjectAPIKey
        extends PropertyDAO
        implements SubjectID<String>, APIKey<String>,
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
        CI_STATUS(NVConfigManager.createNVConfig("api_key_status", "API key status", "APIKeyStatus", true, true, SecConst.SecStatus.class)),
        APP_ID(NVConfigManager.createNVConfigEntity("app_id", "App ID", "AppID", true, false, AppIDDefault.NVC_APP_ID_DEFAULT, NVConfigEntity.ArrayType.NOT_ARRAY)),
        CREDENTIAL_TYPE(NVConfigManager.createNVConfig("credential_type", "What the key is for: API_KEY (third-party API) or SYMMETRIC_KEY (JWT signing)", "CredentialType", false, true, CredentialInfo.Type.class)),
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
            SUS.extractNVConfigs(Param.values()),
            null,
            false,
            PropertyDAO.NVC_PROPERTY_DAO
    );

    /** Creates an empty API key backed by {@link #NVC_SUBJECT_API_KEY}. */
    public SubjectAPIKey() {
        super(NVC_SUBJECT_API_KEY);
        setCredentialStatus(SecConst.SecStatus.ACTIVE);
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
     * @return what the key is for: {@link CredentialInfo.Type#SYMMETRIC_KEY} for a JWT signing
     * key, otherwise {@link CredentialInfo.Type#API_KEY} - a third-party API key, which is also
     * what a key with no stored purpose is, so that only a key explicitly made a signing key can
     * ever verify a login
     */
    @Override
    public Type getCredentialType() {
        Type ret = lookupValue(Param.CREDENTIAL_TYPE);
        return ret != null ? ret : Type.API_KEY;
    }

    /**
     * Sets what the key is for.
     *
     * @param type {@link CredentialInfo.Type#API_KEY} (third-party API key) or
     *             {@link CredentialInfo.Type#SYMMETRIC_KEY} (JWT signing key)
     * @throws IllegalArgumentException for any other type
     */
    public void setCredentialType(Type type) {
        if (type != Type.API_KEY && type != Type.SYMMETRIC_KEY) {
            throw new IllegalArgumentException("A SubjectAPIKey is an API_KEY or a SYMMETRIC_KEY, not " + type);
        }
        setValue(Param.CREDENTIAL_TYPE, type);
    }

    /**
     * @return true when this key is a JWT signing key ({@link CredentialInfo.Type#SYMMETRIC_KEY}),
     * the only kind that may verify a login to this system
     */
    public boolean isSigningKey() {
        return getCredentialType() == Type.SYMMETRIC_KEY;
    }

    /**
     * Sets the key material, storing it as a URL-safe Base64 string (encrypted at rest).
     *
     * @param secret the raw key bytes
     */
    public void setAPIKeyAsBytes(byte[] secret) {
        setAPIKey(SharedBase64.encodeAsString(Base64Type.URL, secret));
    }

    /**
     * Sets the key material in its string form, as stored (URL-safe Base64 by
     * convention; encrypted at rest).
     *
     * @param apiKey the key material as a string
     */
    public void setAPIKey(String apiKey) {
        setValue(Param.API_KEY, apiKey);
    }

    /**
     * @return the key material as its stored URL-safe Base64 string, or {@code null}
     * if unset
     */
    public String getAPIKey() {
        return lookupValue(Param.API_KEY);
    }

    @Override
    public void setAppID(AppID<String> appID) {
        setValue(Param.APP_ID, appID);
    }

    @Override
    public AppID<String> getAppID() {
        return lookupValue(Param.APP_ID);
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
        ret.setAPIKeyAsBytes(subjectAPIKey.getAPIKeyAsBytes());

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

    /**
     * Returns the credential lifecycle status ({@code subject_status} attribute),
     * set to {@link SecConst.SecStatus#ACTIVE} on construction.
     *
     * @return the credential status, or {@code null} if unset
     */
    @Override
    public SecConst.SecStatus getCredentialStatus() {
        return lookupValue(Param.CI_STATUS);
    }

    /**
     * Sets the credential lifecycle status.
     *
     * @param status the status to apply
     */
    @Override
    public void setCredentialStatus(SecConst.SecStatus status) {
        setValue(Param.CI_STATUS, status);
    }
}
