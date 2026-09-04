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
package org.zoxweb.shared.crypto;

import org.zoxweb.shared.data.PropertyDAO;
import org.zoxweb.shared.security.AccessException;
import org.zoxweb.shared.util.*;

/**
 * A wrapped key row: a random 32-byte key, sealed as an {@link EncryptedData} record under a
 * wrapping key, together with the fields that say what it protects and who owns it.
 * <ul>
 * <li>{@code guid}: the row's own identity and the {@code kid} every record under this key carries.</li>
 * <li>{@code subject_guid}: owning subject.</li>
 * <li>{@code reference_guid}, {@code reference_type}: the entity this key protects, or the subject
 * itself for a subject key.</li>
 * <li>{@code key_lock_type}: {@link KeyLockType#USER_ID} for a subject key, {@link KeyLockType#NVENTITY}
 * for an entity key.</li>
 * <li>{@code wrapped_key}: the sealed record as canonical JSON, whose {@code ref} is this row's GUID.</li>
 * </ul>
 * The wrapped record's associated data covers the four binding fields above through
 * {@link #toBindingData()}, so a row re-pointed at another subject or entity fails to unwrap.
 * The class no longer extends {@link EncryptedData}; it contains one.
 */
@SuppressWarnings("serial")
public class EncapsulatedKey
        extends PropertyDAO
        implements CryptoBase, DoNotExpose {

    protected enum Param
            implements GetNVConfig {

        // represent the GUID of NVEntity that this key will be used
        REFERENCE_GUID(NVConfigManager.createNVConfig(MetaToken.REFERENCE_GUID.getName(), "The reference guid of the NVEntity that needs encryption", "ReferenceGUID", true, true, String.class)),
        // represent the class name type of the NVEntity
        REFERENCE_TYPE(NVConfigManager.createNVConfig(MetaToken.REFERENCE_TYPE.getName(), "Class name of the object reference", "ReferenceType", true, true, String.class)),
        KEY_LOCK_TYPE(NVConfigManager.createNVConfig("key_lock_type", "Key lock type", "KeyLockType", true, true, KeyLockType.class)),
        WRAPPED_KEY(NVConfigManager.createNVConfig("wrapped_key", "The wrapped key as an EncryptedData record", "WrappedKey", true, true, String.class)),
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

    public final static NVConfigEntity NVCE_ENCAPSULATED_KEY = new NVConfigEntityPortable("encapsulated_key", null, "EncryptedKey", false, true, false, false, EncapsulatedKey.class, SharedUtil.extractNVConfigs(Param.values()), null, false, PropertyDAO.NVC_PROPERTY_DAO);

    public EncapsulatedKey() {
        super(NVCE_ENCAPSULATED_KEY);
    }

    protected EncapsulatedKey(NVConfigEntity nvce) {
        super(nvce);
    }

    public KeyLockType getKeyLockType() {
        return lookupValue(Param.KEY_LOCK_TYPE);
    }

    public void setKeyLockType(KeyLockType klt) {
        setValue(Param.KEY_LOCK_TYPE, klt);
    }

    /**
     * Points this key at {@code nve}: its GUID becomes the reference GUID and its class name the
     * reference type.
     *
     * @throws AccessException if the entity has no GUID yet
     */
    public void setObjectReference(NVEntity nve) {
        if (nve.getGUID() == null) {
            throw new AccessException("NVEntity GUID not set.");
        }
        setReferenceGUID(nve.getGUID());
        setReferenceType(nve.getClass().getName());
    }

    public String getReferenceType() {
        return lookupValue(Param.REFERENCE_TYPE);
    }

    public void setReferenceType(String className) {
        setValue(Param.REFERENCE_TYPE, className);
    }

    public String getReferenceGUID() {
        return lookupValue(Param.REFERENCE_GUID);
    }

    public void setReferenceGUID(String resourceGUID) {
        setValue(Param.REFERENCE_GUID, resourceGUID);
    }

    /**
     * @return the sealed record as stored, canonical JSON, or null.
     */
    public String getWrappedKey() {
        return lookupValue(Param.WRAPPED_KEY);
    }

    public void setWrappedKey(String wrappedKeyJSON) {
        setValue(Param.WRAPPED_KEY, wrappedKeyJSON);
    }

    /**
     * @return the sealed record, parsed, or null when none is set.
     */
    public EncryptedData getWrapped() {
        String json = getWrappedKey();
        return json != null ? EncryptedData.fromCanonicalID(json) : null;
    }

    public void setWrapped(EncryptedData wrapped) {
        setWrappedKey(wrapped != null ? wrapped.toCanonicalID() : null);
    }

    /**
     * The binding fields in fixed order joined by {@code |}: GUID, subject GUID, reference GUID,
     * reference type and key lock type, absent as empty. It is the extra associated data of the
     * wrapped record, so all five must be set before wrapping and must not change afterwards.
     */
    public String toBindingData() {
        StringBuilder sb = new StringBuilder(160);
        EncryptedData.append(sb, getGUID()).append(EncryptedData.SEP);
        EncryptedData.append(sb, getSubjectGUID()).append(EncryptedData.SEP);
        EncryptedData.append(sb, getReferenceGUID()).append(EncryptedData.SEP);
        EncryptedData.append(sb, getReferenceType()).append(EncryptedData.SEP);
        KeyLockType klt = getKeyLockType();
        EncryptedData.append(sb, klt != null ? klt.name() : null);
        return sb.toString();
    }
}
