package org.zoxweb.shared.crypto;

import org.zoxweb.shared.util.BytesArray;

public class HashResult
{

    public final CryptoConst.HASHType hashType;
    public final BytesArray hash;
    public final long dataLength;

    public HashResult(CryptoConst.HASHType hashType, byte[] hash, long dataLength)
    {
        this.hashType = hashType;
        this.hash = new BytesArray(null, hash);
        this.dataLength = dataLength;
    }

}
