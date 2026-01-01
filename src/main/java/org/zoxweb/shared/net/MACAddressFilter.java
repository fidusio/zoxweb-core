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

import org.zoxweb.shared.filters.ValueFilter;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedStringUtil;

/**
 * Filter for validating and converting MAC addresses.
 * Implements the singleton pattern and provides methods to validate
 * MAC address strings and convert between string and byte array formats.
 *
 * @author mnael
 * @see ValueFilter
 */
@SuppressWarnings("serial")
public class MACAddressFilter
        implements ValueFilter<String, byte[]> {

    /** Supported MAC address separator characters */
    public static final String MAC_ADDRESS_SEPS[] = {
            "-", ":", "."
    };

    /** Standard MAC address length in bytes */
    public static final int MAC_ADDRESS_LENGTH = 6;

    /** Singleton instance */
    public static MACAddressFilter SINGLETON = new MACAddressFilter();

    /**
     * Private constructor for singleton pattern.
     */
    private MACAddressFilter() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toCanonicalID() {
        return null;
    }

    /**
     * Validates a MAC address string and converts it to bytes.
     *
     * @param in the MAC address string to validate
     * @return the MAC address as a byte array
     * @throws NullPointerException if the input is null
     * @throws IllegalArgumentException if the MAC address is invalid
     */
    @Override
    public byte[] validate(String in) throws NullPointerException, IllegalArgumentException {
        SUS.checkIfNulls("MAC is null", in);
        byte[] ret = macAddressToBytes(in);
        if (ret.length != MAC_ADDRESS_LENGTH) {
            throw new IllegalArgumentException("Invalid MAC Address " + in);
        }
        return ret;
    }

    /**
     * Checks if a MAC address string is valid.
     *
     * @param in the MAC address string to check
     * @return true if valid, false otherwise
     */
    @Override
    public boolean isValid(String in) {
        try {
            validate(in);
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    /**
     * Converts a MAC address string to a byte array.
     *
     * @param macAddress the MAC address string
     * @return the MAC address as bytes
     */
    public static byte[] macAddressToBytes(String macAddress) {
        return SharedStringUtil.hexToBytes(SharedStringUtil.filterString(macAddress, MAC_ADDRESS_SEPS));
    }

    /**
     * Converts a MAC address byte array to a string.
     *
     * @param address the MAC address bytes
     * @param sep the separator to use between bytes
     * @return the formatted MAC address string
     * @throws NullPointerException if address is null
     */
    public static String toString(byte[] address, String sep) {
        SUS.checkIfNulls("MAC is null", address);
        if (address.length != MAC_ADDRESS_LENGTH) {
            //throw new IllegalArgumentException("Invalid MAC Address length " + address.length);
        }
        return SharedStringUtil.bytesToHex(null, address, 0, address.length, sep);
    }

}
