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
import org.zoxweb.shared.util.DataEncoder;
import org.zoxweb.shared.util.SUS;

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
    public static final String[] MAC_ADDRESS_SEPS = {
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
        return SUS.hexToBytes(normalize(macAddress));
    }


    /**
     * Normalizes a MAC address string to bare upper-case hex digits.
     * <p>
     * Three transformations are applied, in order:
     * <ol>
     *   <li>every whitespace character is removed (regex {@code \s}: space, tab, CR, LF,
     *       form feed and vertical tab), so leading, trailing and embedded whitespace all
     *       disappear;</li>
     *   <li>every separator listed in {@link #MAC_ADDRESS_SEPS} ({@code -}, {@code :} and
     *       {@code .}) is removed, by literal replacement;</li>
     *   <li>the remainder is upper-cased.</li>
     * </ol>
     * Any separator style is therefore accepted: {@code 00:1b:c5:00:1f:01},
     * {@code 00-1B-C5-00-1F-01}, {@code 001b.c500.1f01}, {@code 001bc5001f01} and
     * {@code " 00 1b c5 00 1f 01 "} all normalize to {@code 001BC5001F01}.
     * <p>
     * No validation is performed: the input may be a partial prefix (e.g. {@code 00:1B:C5}
     * gives {@code 001BC5}) and characters that are neither whitespace, separators nor hex
     * digits are passed through unchanged. Callers that need a valid address convert the
     * result with {@link #macAddressToBytes(String)} or check it with {@link #isValid(String)}.
     *
     * @param macAddress the MAC address, or prefix, in any separator style; may be null
     * @return the normalized hex string, possibly empty, or null if the input is null
     */
    public static String normalize(String macAddress) {
        if (macAddress == null) {
            return null;
        }
        return DataEncoder.StringUpper.encode(
                SUS.filterString(macAddress.replaceAll("\\s", ""), MAC_ADDRESS_SEPS)
        );
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
        return SUS.bytesToHex(null, address, 0, address.length, sep);
    }

}
