package org.zoxweb.shared.security;

import org.zoxweb.shared.util.CanonicalID;
import org.zoxweb.shared.util.SharedStringUtil;

public class BCryptHash
        implements CanonicalID
{
    public final String prefix;
    public final int logRound;
    public final String salt;
    public final String hash;
    //public final String bCryptHash;

    public BCryptHash(String fullHash)
    {
        //bCryptHash = fullHash;

        String[] tokens = SharedStringUtil.parseString(fullHash, "\\$", true);
        prefix = tokens[0];
        logRound = Integer.parseInt(tokens[1]);
        salt = tokens[2].substring(0, 22);
        hash = tokens[2].substring(22);
    }

    public String toString()
    {
        return toCanonicalID();
    }

    public String toCanonicalID()
    {
        return "$" + prefix + "$" + logRound + "$" + salt + hash;
    }
}
