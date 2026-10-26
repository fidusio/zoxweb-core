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
package org.zoxweb.shared.net;

import org.zoxweb.shared.data.SetNameDescriptionImpl;
import org.zoxweb.shared.util.*;

/**
 * One OUI (Organizationally Unique Identifier) registry entry, as published in the
 * Wireshark manuf file: the registered MAC prefix, its mask length in bits, the short
 * manufacturer name and the long manufacturer name.
 * <p>
 * The names are carried by the inherited {@link SetNameDescriptionImpl} fields:
 * {@link #getName()} is the short name (e.g. {@code Cisco}) and {@link #getDescription()}
 * the long name (e.g. {@code Cisco Systems, Inc}). The entry's own fields are the prefix,
 * the mask length and the locally administered flag, declared in {@link Param}.
 * <p>
 * The prefix and mask length identify an entry: {@code prefix} holds the upper-case hex
 * digits of the registered block with separators removed, and {@code mask_bits} says how
 * many of its leading bits are significant (24 for a classic OUI, 28 or 36 for the smaller
 * MA-M and MA-S blocks). A prefix is expected to hold at least {@code mask_bits / 4} digits.
 * <p>
 * Being an {@code NVEntity}, an entry round-trips through JSON and can be persisted in and
 * loaded from an {@code APIDataStore} under {@link #NVC_OUI_VENDOR}; the in-memory lookup
 * table ({@code org.zoxweb.server.net.OUIDB}) can be built either from the manuf file or
 * from a list of persisted entries.
 * <p>
 * {@link #newLocallyAdministered()} mints a synthetic entry, flagged by
 * {@link #isLocallyAdministered()}, for MAC addresses whose U/L bit is set; such addresses
 * are randomized or software-assigned and belong to no registry block. Synthetic entries are
 * not meant to be persisted or indexed.
 */
@SuppressWarnings("serial")
public class OUIVendor
        extends SetNameDescriptionImpl {

    /**
     * Meta configuration of the fields this entry adds on top of
     * {@link SetNameDescriptionImpl}.
     */
    public enum Param
            implements GetNVConfig {
        /** {@code prefix}: the registered MAC prefix as upper-case hex digits without separators. */
        PREFIX(NVConfigManager.createNVConfig("prefix", "Normalized hex MAC prefix", "Prefix", false, true, String.class)),
        /** {@code mask_bits}: the number of significant leading bits of the prefix (24, 28, 36 ...). */
        MASK_BITS(NVConfigManager.createNVConfig("mask_bits", "Prefix mask length in bits (24, 28, 36 ...)", "MaskBits", false, true, int.class)),
        /** {@code locally_administered}: true for the synthetic entry describing a U/L-bit MAC address. */
        LOCALLY_ADMINISTERED(NVConfigManager.createNVConfig("locally_administered", "True if the MAC has the U/L bit set", "LocallyAdministered", false, true, boolean.class)),
        ;

        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        /**
         * Returns the meta configuration of this field.
         *
         * @return the field's NVConfig
         */
        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    /**
     * Entity meta configuration ({@code oui_vendor}): the {@link Param} fields plus the
     * inherited name and description. Use it as the collection schema for datastore
     * operations and for meta-aware JSON conversion.
     */
    public static final NVConfigEntity NVC_OUI_VENDOR = new NVConfigEntityPortable(
            "oui_vendor",
            null,
            "OUIVendor",
            true,
            false,
            false,
            false,
            OUIVendor.class,
            SUS.extractNVConfigs(Param.values()),
            null,
            false,
            SetNameDescriptionImpl.NVC_NAME_DESCRIPTION_IMPL);

    /** Short name of the synthetic entry returned by {@link #newLocallyAdministered()}. */
    public static final String LOCALLY_ADMINISTERED_NAME = "Local";
    /** Long name of the synthetic entry returned by {@link #newLocallyAdministered()}. */
    public static final String LOCALLY_ADMINISTERED_DESCRIPTION = "Locally administered (randomized)";

    /**
     * Creates an empty entry; needed by the meta-model for JSON and datastore materialization.
     */
    public OUIVendor() {
        super(NVC_OUI_VENDOR);
    }

    /**
     * Creates a registry entry.
     *
     * @param prefix      the registered MAC prefix, hex digits without separators
     * @param maskBits    the number of significant leading bits (24, 28, 36 ...)
     * @param name        the short manufacturer name
     * @param description the long manufacturer name
     */
    public OUIVendor(String prefix, int maskBits, String name, String description) {
        this();
        setPrefix(prefix);
        setMaskBits(maskBits);
        setName(name);
        setDescription(description);
    }

    /**
     * Creates the synthetic entry for a locally administered (U/L bit set) MAC address:
     * empty prefix, mask length zero, {@link #LOCALLY_ADMINISTERED_NAME} and
     * {@link #LOCALLY_ADMINISTERED_DESCRIPTION} as names and
     * {@link #isLocallyAdministered()} true. A new instance is returned on every call.
     *
     * @return a new locally administered entry
     */
    public static OUIVendor newLocallyAdministered() {
        OUIVendor ret = new OUIVendor("", 0, LOCALLY_ADMINISTERED_NAME, LOCALLY_ADMINISTERED_DESCRIPTION);
        ret.setLocallyAdministered(true);
        return ret;
    }

    /**
     * Returns the registered MAC prefix.
     *
     * @return the prefix as hex digits without separators, or null if unset
     */
    public String getPrefix() {
        return lookupValue(Param.PREFIX);
    }

    /**
     * Sets the registered MAC prefix. Stored as given; consumers normalize it
     * (separators removed, upper case) before indexing.
     *
     * @param prefix the hex prefix
     */
    public void setPrefix(String prefix) {
        setValue(Param.PREFIX, prefix);
    }

    /**
     * Returns the mask length of the prefix.
     *
     * @return the number of significant leading bits (24, 28, 36 ...), or 0 if unset
     */
    public int getMaskBits() {
        return lookupValue(Param.MASK_BITS);
    }

    /**
     * Sets the number of significant leading bits of the prefix.
     *
     * @param maskBits the mask length in bits, normally a multiple of 4
     */
    public void setMaskBits(int maskBits) {
        setValue(Param.MASK_BITS, maskBits);
    }

    /**
     * Tells whether this is the synthetic locally administered entry.
     *
     * @return true for the entry describing a locally administered MAC address,
     *         false for a registry entry
     */
    public boolean isLocallyAdministered() {
        return lookupValue(Param.LOCALLY_ADMINISTERED);
    }

    /**
     * Flags this entry as the synthetic locally administered one.
     *
     * @param locallyAdministered true for a locally administered entry
     */
    public void setLocallyAdministered(boolean locallyAdministered) {
        setValue(Param.LOCALLY_ADMINISTERED, locallyAdministered);
    }

    /**
     * Returns a one-line rendering of the entry.
     *
     * @return {@code prefix/maskBits name (description)}
     */
    @Override
    public String toString() {
        return getPrefix() + "/" + getMaskBits() + " " + getName() + " (" + getDescription() + ")";
    }
}
