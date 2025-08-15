package org.zoxweb.server.security;

import org.zoxweb.shared.crypto.BCryptHash;
import org.zoxweb.shared.crypto.CIPassword;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.util.SharedBase64;

public class BCryptPasswordHasher
        extends PasswordHasher {
    public BCryptPasswordHasher(int rounds) {
        super("bcrypt", CryptoConst.HashType.BCRYPT,  CryptoConst.HashType.BCRYPT.VARIANCES, rounds);
    }

    /**
     * @param password
     * @return
     */
    @Override
    public CIPassword hash(String password) {
        return HashUtil.toBCryptPassword(password, getRounds());
    }

    /**
     * @param password
     * @return
     */
    @Override
    public CIPassword hash(byte[] password) {
        return HashUtil.toBCryptPassword(password, getRounds());
    }

    /**
     * @param password
     * @return
     */
    @Override
    public CIPassword hash(char[] password) {
        return HashUtil.toBCryptPassword(new String(password), getRounds());
    }

    /**
     * @param passwordCanonicalID
     * @return
     */
    @Override
    public CIPassword fromCanonicalID(String passwordCanonicalID) {
        // special case to process BCrypt
        BCryptHash bCryptHash = new BCryptHash(passwordCanonicalID);
        CIPassword ret = new CIPassword();
        ret.setVersion(bCryptHash.algorithm);
        ret.setSalt(SharedBase64.decode(SharedBase64.Base64Type.DEFAULT_NP, bCryptHash.salt));
        ret.setSalt(SharedBase64.decode(SharedBase64.Base64Type.DEFAULT_NP, bCryptHash.hash));
        ret.setRounds(bCryptHash.rounds);
        ret.setCanonicalID(bCryptHash.toCanonicalID());
        ret.setName(CryptoConst.HashType.BCRYPT);
        return ret;
    }

    /**
     * @param ci
     * @param password
     * @return
     */
    @Override
    public boolean isPasswordValid(CIPassword ci, String password) {
        return HashUtil.isBCryptPasswordValid(password, ci.getCanonicalID());
    }

    /**
     * @param ci
     * @param password
     * @return
     */
    @Override
    public boolean isPasswordValid(CIPassword ci, byte[] password) {
        return HashUtil.isBCryptPasswordValid(password, ci.getCanonicalID());
    }

    /**
     * @param ci
     * @param password
     * @return
     */
    @Override
    public boolean isPasswordValid(CIPassword ci, char[] password) {
        return HashUtil.isBCryptPasswordValid(new String(password), ci.getCanonicalID());
    }
}
