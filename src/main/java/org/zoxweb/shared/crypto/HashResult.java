package org.zoxweb.shared.crypto;

import org.zoxweb.shared.util.BytesArray;
import org.zoxweb.shared.util.SharedBase64;

public class HashResult
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

    public HashResult(CryptoConst.HashType hashType, byte[] hash, long dataLength)
    {
        this.hashType = hashType;
        this.hash = new BytesArray(null, hash);
        this.dataLength = dataLength;
    }

    public String base64()
    {
        return SharedBase64.encodeAsString(SharedBase64.Base64Type.DEFAULT, hash.asBytes());
    }

}
