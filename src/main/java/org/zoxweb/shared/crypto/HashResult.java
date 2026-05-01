package org.zoxweb.shared.crypto;

import org.zoxweb.shared.io.BytesArray;
import org.zoxweb.shared.util.*;

public class HashResult
    implements GetNVProperties
{

    /**
     * The hash type
     */
    public final CryptoConst.HashType hashType;
    /**
     * Read only BytesArray of the hash
     */
    public final BytesArray hash;
    /**
     * The length od data that was hashed
     */
    public final long dataLength;
    public final String formatType;

    public HashResult(CryptoConst.HashType hashType, byte[] hash, long dataLength) {
        this(hashType, hash, dataLength, "base64");
    }

    public HashResult(CryptoConst.HashType hashType, byte[] hash, long dataLength, String formatType) {
        this.hashType = hashType;
        this.hash = new BytesArray(null, hash);
        this.dataLength = dataLength;
        this.formatType = parseFormatType(formatType);
    }

    public static String parseFormatType(String formatType) {
        switch (formatType.toLowerCase()) {
            case "hex":
                return "hex";
            case "base64":
            case "default":
                return "base64";
            case "base64-url":
            case "url":
                return "base64-url";
            default:
                throw new IllegalArgumentException("Unknown format type: " + formatType);
        }
    }

    public String result() {
        switch (formatType) {
            case "base64":
                return SharedBase64.encodeAsString(SharedBase64.Base64Type.DEFAULT, hash.asBytes());
            case "base64-url":
                return SharedBase64.encodeAsString(SharedBase64.Base64Type.URL, hash.asBytes());
            default:
                return SUS.fastBytesToHex(hash.asBytes());
        }
    }

    @Override
    public NVGenericMap getProperties() {
        return new NVGenericMap(hashType.getName().toLowerCase()).build(new NVLong("data-length", dataLength)).build(formatType, result());
    }
}
