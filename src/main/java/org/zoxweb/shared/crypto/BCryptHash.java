package org.zoxweb.shared.crypto;

import org.zoxweb.shared.util.CanonicalID;
import org.zoxweb.shared.util.SharedStringUtil;

public class BCryptHash
        implements CanonicalID {

    public final String algorithm;
    public final int rounds;
    public final String salt;
    public final String hash;
    public final String bCryptHash;


    public BCryptHash(String fullHash) {
        bCryptHash = fullHash;

        String[] tokens = SharedStringUtil.parseString(fullHash, "\\$", true);
        if (CryptoConst.HashType.lookup(tokens[0]) != CryptoConst.HashType.BCRYPT)
            throw new IllegalArgumentException("Invalid bcrypt algorithm " + tokens[0]);

        algorithm = tokens[0];
        rounds = Integer.parseInt(tokens[1]);
        salt = tokens[2].substring(0, 22);
        hash = tokens[2].substring(22);
    }

    public String toString() {
        return toCanonicalID();
    }

    public String toCanonicalID() {
        return bCryptHash;
    }
}
