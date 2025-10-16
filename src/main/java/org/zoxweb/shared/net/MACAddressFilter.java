package org.zoxweb.shared.net;

import org.zoxweb.shared.filters.ValueFilter;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedStringUtil;

@SuppressWarnings("serial")
public class MACAddressFilter
        implements ValueFilter<String, byte[]> {

    public static final String MAC_ADDRESS_SEPS[] = {
            "-", ":", "."
    };

    public static final int MAC_ADDRESS_LENGTH = 6;

    public static MACAddressFilter SINGLETON = new MACAddressFilter();


    private MACAddressFilter() {
    }

    @Override
    public String toCanonicalID() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public byte[] validate(String in) throws NullPointerException, IllegalArgumentException {
        SUS.checkIfNulls("MAC is null", in);
        byte[] ret = macAddressToBytes(in);
        if (ret.length != MAC_ADDRESS_LENGTH) {
            throw new IllegalArgumentException("Invalid MAC Address " + in);
        }
        // TODO Auto-generated method stub
        return ret;
    }

    @Override
    public boolean isValid(String in) {
        try {
            validate(in);
            return true;
        } catch (Exception e) {
        }
        return false;
    }


    public static byte[] macAddressToBytes(String macAddress) {
        return SharedStringUtil.hexToBytes(SharedStringUtil.filterString(macAddress, MAC_ADDRESS_SEPS));
    }

    public static String toString(byte[] address, String sep) {
        SUS.checkIfNulls("MAC is null", address);
        if (address.length != MAC_ADDRESS_LENGTH) {
            //throw new IllegalArgumentException("Invalid MAC Address length " + address.length);
        }
        return SharedStringUtil.bytesToHex(null, address, 0, address.length, sep);
    }

}
