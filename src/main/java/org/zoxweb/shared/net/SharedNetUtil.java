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

import org.zoxweb.shared.data.Range;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

import java.io.IOException;
import java.util.Arrays;

/**
 * Utility class for shared network operations.
 * Provides methods for IPv4 address manipulation, netmask validation,
 * network calculations, and network interface configuration validation.
 *
 * @author mnael
 */
public class SharedNetUtil {

    /** Valid netmask byte values for IPv4 */
    public static final byte[] MASK_VALS = {0, (byte) 128, (byte) 192, (byte) 224, (byte) 240, (byte) 248, (byte) 252, (byte) 254, (byte) 255};

    public static final Range<Integer> PORTS_RANGE = Range.toRange("[0,65535]");

    /**
     * Private constructor to prevent instantiation.
     */
    private SharedNetUtil() {
    }

    /**
     * Parses an IPv4 address string to a byte array.
     *
     * @param ipV4 the IPv4 address string (e.g., "192.168.1.1")
     * @return the address as a 4-byte array
     * @throws IOException if the address is invalid
     * @throws IllegalArgumentException if the format is incorrect
     */
    public static byte[] getV4Address(String ipV4)
            throws IOException {
        String bytes[] = ipV4.split("\\.");
        if (bytes.length != 4) {
            throw new IllegalArgumentException("Invalid ip address:" + ipV4 + " length:" + bytes.length);
        }
        byte ret[] = new byte[4];
        for (int i = 0; i < bytes.length; i++) {
            int val = Integer.parseInt(bytes[i]);
            if (val < 0 || val > 255) {
                throw new IOException("Invalid ip address:" + ipV4);
            }
            ret[i] = (byte) val;
        }
        return ret;
    }

    /**
     * Validates an IPv4 netmask.
     *
     * @param netmask the netmask as a byte array
     * @return true if valid, false otherwise
     * @throws IllegalArgumentException if netmask length is not 4
     */
    public static boolean validateV4Netmask(byte netmask[]) {
        if (netmask.length != 4) {
            throw new IllegalArgumentException("Invalid netmask length:" + netmask.length);
        }

        for (int i = 0; i < netmask.length; i++) {
            if (i != 0) {
                if (SharedUtil.toUnsignedInt(netmask[i - 1]) < 255) {
                    if (netmask[i] != 0) {
                        return false;
                    } else {
                        continue;
                    }
                }
            }

            boolean test = false;
            for (int j = 0; j < MASK_VALS.length; j++) {

                if (netmask[i] == MASK_VALS[j]) {
                    test = true;
                    break;
                }
            }
            if (!test)
                return false;
        }


        return true;
    }

    /**
     * Calculates the network address from an IP address and netmask.
     *
     * @param address the IP address bytes
     * @param netmask the netmask bytes
     * @return the network address bytes
     * @throws IOException if calculation fails
     */
    public static byte[] getNetwork(byte[] address, byte[] netmask)
            throws IOException {
        byte[] networkBytes = new byte[address.length];

        for (int i = 0; i < networkBytes.length; i++) {
            networkBytes[i] = (byte) (address[i] & netmask[i]);
        }
        return networkBytes;
    }

    /**
     * Converts a byte array to an IPv4 address string.
     *
     * @param address the address bytes
     * @return the formatted IPv4 string
     * @throws IOException if the address is invalid
     * @throws NullPointerException if address is null
     */
    public static String toV4Address(byte[] address)
            throws IOException {
        SUS.checkIfNulls("Null address", address);
        if (address.length != 4)
            throw new IOException("Invalid address length:" + address.length);

        StringBuilder sb = new StringBuilder();
        int index = 0;

        sb.append(SharedStringUtil.toString(address[index++]));
        sb.append('.');
        sb.append(SharedStringUtil.toString(address[index++]));
        sb.append('.');
        sb.append(SharedStringUtil.toString(address[index++]));
        sb.append('.');
        sb.append(SharedStringUtil.toString(address[index++]));

        return sb.toString();
    }

    /**
     * Checks if an IP address belongs to a specific network.
     *
     * @param address the IP address to check
     * @param netmask the network mask (can be null)
     * @param network the network address
     * @return true if the address belongs to the network
     * @throws IOException if calculation fails
     * @throws NullPointerException if network or address is null
     */
    public static boolean belongsToNetwork(byte[] address, byte[] netmask, byte[] network)
            throws IOException {
        SUS.checkIfNulls("Network or IP adress can't be null", network, address);
        byte[] tempNetwork = null;
        if (netmask != null) {
            tempNetwork = getNetwork(address, netmask);
        } else {
            tempNetwork = address;
        }
        return Arrays.equals(network, tempNetwork);
    }

    /**
     * Converts a CIDR prefix to an IPv4 netmask byte array.
     *
     * @param netPrefix the CIDR prefix (0-32)
     * @return the netmask as a byte array
     * @throws IllegalArgumentException if prefix is greater than 32
     */
    public static byte[] toNetmaskIPV4(short netPrefix) {

        if (netPrefix > 32) {
            throw new IllegalArgumentException("Invalid mask " + netPrefix + " > 32");
        }

        long maskLong = 0xffffffffL;
        maskLong = maskLong << (32 - netPrefix);
        byte[] maskAddress = new byte[4];

        for (int i = 0; i < maskAddress.length; i++) {
            maskAddress[maskAddress.length - (1 + i)] = (byte) maskLong;

            maskLong = maskLong >> 8;
        }
        return maskAddress;
    }

    /**
     * Converts an IPv4 netmask byte array to a CIDR prefix.
     *
     * @param netmask the netmask bytes
     * @return the CIDR prefix (0-32)
     * @throws IOException if the netmask is invalid
     */
    public static short toNetmaskIPV4(byte[] netmask) throws IOException {
        if (!validateV4Netmask(netmask))
            throw new IOException("Invalid netmaks");

        short ret = 0;

        for (byte b : netmask) {
            for (int i = 0; i < 8; i++) {
                byte res = (byte) (b & 0x01);


                if (res == 1)
                    ret++;


                b = (byte) (b >> 1);
            }
        }


        return ret;

    }

    /**
     * Validates a network interface configuration.
     *
     * @param nicd the network interface configuration to validate
     * @return true if the configuration is valid
     * @throws IOException if validation fails
     * @throws NullPointerException if nicd or protocol is null
     * @throws IllegalArgumentException if interface name is invalid
     */
    public static boolean validateNIConfig(NIConfigDAO nicd) throws IOException {
        SUS.checkIfNulls("NIConfigDAO null", nicd, nicd.getInetProtocol());
        if (SUS.isEmpty(nicd.getNIName())) {
            throw new IllegalArgumentException("Network Interface name invalid:" + nicd.getNIName());
        }


        switch (nicd.getInetProtocol()) {
            case DHCP:
                break;
            case STATIC:
                if (SUS.isEmpty(nicd.getAddress()) || SUS.isEmpty(nicd.getNetmask())) {
                    throw new IOException("Network missing:" + SharedUtil.toCanonicalID(',', nicd.getAddress(), nicd.getNetmask()));
                }
                byte[] address = getV4Address(nicd.getAddress());
                byte[] netmask = getV4Address(nicd.getNetmask());
                if (!validateV4Netmask(netmask)) {
                    return false;
                }
                byte[] gateway = nicd.getGateway() != null ? getV4Address(nicd.getGateway()) : null;
                byte[] network = getNetwork(address, netmask);
                if (gateway != null) {
                    // gateway must not be equal to address
                    if (Arrays.equals(address, gateway)) {
                        throw new IOException("address and gateway equals:" + toV4Address(address));
                    }
                    if (!(belongsToNetwork(address, netmask, network) &&
                            belongsToNetwork(gateway, netmask, network))) {
                        throw new IOException("BAD Network config:" + SharedUtil.toCanonicalID(',', nicd.getNIName(), nicd.getAddress(), nicd.getNetmask(), nicd.getGateway()));
                    }
                }

                break;
            default:
                return false;
        }

        return true;
    }

    /**
     * Check is IPAddress is a private ip
     * @param input to checked
     * @return true private ip, false otherwise
     */
    public static boolean isPrivateIP(IPAddress input) {
        return isPrivateIP(input.getInetAddress());
    }

    /**
     * Check if the input is a private IP address and return true or false.
     * Private IP ranges: 10.x.x.x, 192.168.x.x, 172.16-31.x.x
     *
     * @param input the string to check, if null return false
     * @return true if it's a private IP, false otherwise
     */
    public static boolean isPrivateIP(String input) {
        input = SUS.trimOrNull(input);
        if (input == null) {
            return false;
        }

        // Quick check - must start with digit and contain dots
        char first = input.charAt(0);
        if (!Character.isDigit(first) || input.indexOf('.') == -1) {
            return false;
        }

        return input.startsWith("10.") ||
                input.startsWith("192.168.") ||
                isPrivate172Range(input);

    }

    /**
     * Check if IP is in 172.16.0.0 - 172.31.255.255 range
     */
    private static boolean isPrivate172Range(String input) {
        if (!input.startsWith("172.")) {
            return false;
        }
        try {
            int dotIndex = input.indexOf('.', 4);
            if (dotIndex > 4) {
                int secondOctet = Integer.parseInt(input.substring(4, dotIndex));
                return secondOctet >= 16 && secondOctet <= 31;
            }
        } catch (NumberFormatException e) {
            // Not a valid number
        }
        return false;
    }

}
