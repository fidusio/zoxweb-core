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

import org.zoxweb.shared.security.AccessException;
import org.zoxweb.shared.util.*;

/**
 * A wrapped key: an {@link EncryptedData} whose plaintext is a random 32-byte AES key, sealed under
 * an outer key, plus the fields that say what it protects and who owns it.
 * <ul>
 * <li>{@code subject_guid}: owning subject.</li>
 * <li>{@code reference_guid}: the entity this key protects, or the subject itself for a subject key.</li>
 * <li>{@code reference_type}: class name of the referenced entity; a label, not authenticated.</li>
 * <li>{@code key_lock_type}: {@link KeyLockType#SUBJECT_ID} for a subject key, {@link KeyLockType#NVENTITY}
 * for an entity key; a label, not authenticated.</li>
 * <li>{@code key_guid}: the key that wrapped this one. For a KEM wrap the GUID of the public key
 * in the registry; for a symmetric wrap the parent key, empty under the master key.</li>
 * <li>{@code alg}, inherited: how the outer key was obtained. {@link CryptoConst#ALG_A256GCM} means a
 * symmetric outer key, a parent key or the master key. A KEM name such as {@link CryptoConst#ML_KEM_768}
 * means the outer key was encapsulated to a public key; then {@code cipher_data} is the encapsulated
 * key followed by the sealed key, and only the private-key holder can open it.</li>
 * <li>{@code key_size}, in bytes: for a symmetric wrap the size of the outer key, 32; for a KEM
 * wrap the size of the encapsulated key, the part of {@code cipher_data} that the private-key holder
 * decapsulates. ML-KEM-512, 768 and 1024 give 768, 1088 and 1568. Readers split
 * {@code cipher_data} at this value, so any parameter set the KEM library knows works without a
 * change here.</li>
 * </ul>
 * {@link #toBindingData()}, subject GUID, reference GUID, key GUID and key size, is the extra
 * associated data of the sealed key, so a key re-pointed at another subject, entity or wrapping
 * key, or split at another offset, fails to unwrap. Those must be set before wrapping and never
 * change afterwards. Where the key is stored is not its concern; the entity GUID is the
 * datastore's identity and plays no part in the crypto.
 */
@SuppressWarnings("serial")
public class EncapsulatedKey
        extends EncryptedData
        implements DoNotExpose {

    protected enum Param
            implements GetNVConfig {

        // represent the GUID of NVEntity that this key will be used
        REFERENCE_GUID(NVConfigManager.createNVConfig(MetaToken.REFERENCE_GUID.getName(), "The reference guid of the NVEntity that needs encryption", "ReferenceGUID", true, true, String.class)),
        // represent the class name type of the NVEntity
        REFERENCE_TYPE(NVConfigManager.createNVConfig(MetaToken.REFERENCE_TYPE.getName(), "Class name of the object reference", "ReferenceType", true, true, String.class)),
        KEY_LOCK_TYPE(NVConfigManager.createNVConfig("key_lock_type", "Key lock type", "KeyLockType", true, true, KeyLockType.class)),
        KEY_GUID(NVConfigManager.createNVConfig(MetaToken.KEY_GUID.getName(), "GUID of the key that wrapped this one: the public key for a KEM wrap, the parent key otherwise", "KeyGUID", false, true, String.class)),
        KEY_SIZE(NVConfigManager.createNVConfig("key_size", "Outer key size in bytes; for a KEM wrap the encapsulated key size", "KeySize", true, true, Integer.class)),
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

    public final static NVConfigEntity NVCE_ENCAPSULATED_KEY = new NVConfigEntityPortable("encapsulated_key", null, "EncryptedKey", false, true, false, false, EncapsulatedKey.class, SharedUtil.extractNVConfigs(Param.values()), null, false, EncryptedData.NVCE_ENCRYPTED_DATA);

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
     * @return GUID of the key that wrapped this one: the registry entry of the public key for a
     * KEM wrap, the parent key for a symmetric wrap, null under the master key.
     */
    public String getKeyGUID() {
        return lookupValue(Param.KEY_GUID);
    }

    public void setKeyGUID(String keyGUID) {
        setValue(Param.KEY_GUID, checkText("key_guid", keyGUID));
    }

    /**
     * @return the outer key size in bytes, or for a KEM wrap the encapsulated key size in bytes;
     * 0 before wrapping.
     */
    public int getKeySize() {
        Integer v = lookupValue(Param.KEY_SIZE);
        return v != null ? v : 0;
    }

    public void setKeySize(int keySizeInBytes) {
        if (keySizeInBytes < 0) {
            throw new IllegalArgumentException("Illegal key size " + keySizeInBytes);
        }
        setValue(Param.KEY_SIZE, keySizeInBytes);
    }

    /**
     * @return true when {@code alg} names a KEM rather than the symmetric cipher, so unwrapping
     * needs the private key.
     */
    public boolean isKEMWrapped() {
        String alg = getAlgorithm();
        return alg != null && !CryptoConst.ALG_A256GCM.equals(alg);
    }

    /**
     * @return a copy of the encapsulated key, the first {@code key_size} bytes of {@code cipher_data},
     * or null for a symmetric wrap or a malformed row.
     */
    public byte[] getKEMCiphertext() {
        byte[] combined = getEncryptedData();
        int size = getKeySize();
        if (!isKEMWrapped() || combined == null || size <= 0 || combined.length < size) {
            return null;
        }
        byte[] ret = new byte[size];
        System.arraycopy(combined, 0, ret, 0, size);
        return ret;
    }

    /**
     * The binding fields joined by {@code |}: subject GUID, reference GUID, key GUID and key size,
     * absent as empty. It is the extra associated data of the sealed key, so all four must be set
     * before wrapping and must not change afterwards. Labels such as the reference type and lock
     * type are deliberately not included, so they can be renamed or corrected without invalidating
     * stored keys. The algorithm needs no place here: it is a record attribute and already
     * authenticated.
     */
    public String toBindingData() {
        StringBuilder sb = new StringBuilder(96);
        append(sb, getSubjectGUID()).append(SEP);
        append(sb, getReferenceGUID()).append(SEP);
        append(sb, getKeyGUID()).append(SEP);
        sb.append(getKeySize());
        return sb.toString();
    }
}
