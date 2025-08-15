package org.zoxweb.server.security;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.shared.crypto.CIPassword;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.security.AccessException;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedBase64;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;




public class SHAPasswordHasher
        extends PasswordHasher {

    private static final String[] ALGORITHMS = {"sha-256", "sha-512"};

    public SHAPasswordHasher(int rounds) {
        super("sha", CryptoConst.HashType.SHA_256, ALGORITHMS, rounds);
    }


    @Override
    public CIPassword hash(String password) {
        try {
            return HashUtil.toPassword(getHashType(), 0, getRounds(), password);
        } catch (NoSuchAlgorithmException e) {
            throw new AccessException(e.getMessage());
        }
    }

    @Override
    public CIPassword hash(byte[] password) {
        try {
            return HashUtil.toPassword(getHashType(), 0, getRounds(), password);
        } catch (NoSuchAlgorithmException e) {
            throw new AccessException(e.getMessage());
        }
    }

    /**
     * @param passwordCanonicalID
     * @return
     */
    @Override
    public CIPassword fromCanonicalID(String passwordCanonicalID) {
        String[] tokens = SharedStringUtil.parseString(passwordCanonicalID, "\\$", true);
        CIPassword ret = new CIPassword();

        switch (tokens.length) {
            case 3: {
                int index = 0;
                ret.setRounds(Integer.parseInt(tokens[index++]));
                ret.setSalt(SharedBase64.decode(SharedBase64.Base64Type.DEFAULT_NP, tokens[index++]));
                ret.setHash(SharedBase64.decode(SharedBase64.Base64Type.DEFAULT_NP, tokens[index++]));
                ret.setName(CryptoConst.HashType.SHA_256.getName().toLowerCase());
            }
            break;
            case 4: {
                int index = 0;
                ret.setName(tokens[index++].toLowerCase());
                ret.setRounds(Integer.parseInt(tokens[index++]));
                ret.setSalt(SharedBase64.decode(SharedBase64.Base64Type.DEFAULT_NP, tokens[index++]));
                ret.setHash(SharedBase64.decode(SharedBase64.Base64Type.DEFAULT_NP, tokens[index++]));
                break;
            }
            default:
                throw new IllegalArgumentException("Invalid password format");
        }
        ret.setCanonicalID(SharedUtil.toCanonicalID('$',
                "$" + ret.getName().toLowerCase(),
                ret.getRounds(),
                SharedBase64.encodeAsString(SharedBase64.Base64Type.DEFAULT_NP, ret.getSalt()),
                SharedBase64.encodeAsString(SharedBase64.Base64Type.DEFAULT_NP, ret.getHash())));

        return ret;
    }

    @Override
    public CIPassword hash(char[] password) {
        try {
            return HashUtil.toPassword(getHashType(), 0, getRounds(), ByteBufferUtil.toBytes(password));
        } catch (NoSuchAlgorithmException e) {
            throw new AccessException(e.getMessage());
        }
    }

    /**
     * @param ci
     * @param password
     * @return
     */
    @Override
    public boolean isPasswordValid(CIPassword ci, String password) {
        return isPasswordValid(ci, SharedStringUtil.getBytes(password));
    }

    /**
     * @param ci
     * @param password
     * @return
     */
    @Override
    public boolean isPasswordValid(CIPassword ci, byte[] password) {
        try {
            byte[] genHash = HashUtil.hashWithIterations(MessageDigest.getInstance(ci.getName()),
                    ci.getSalt(), password, ci.getRounds(),
                    false);

            return SUS.slowEquals(genHash, ci.getHash());
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }

    /**
     * @param ci
     * @param password
     * @return
     */
    @Override
    public boolean isPasswordValid(CIPassword ci, char[] password) {
        return isPasswordValid(ci, ByteBufferUtil.toBytes(password));
    }


//    public void setRounds(int rounds) {
//        if (getHashType() == CryptoConst.HashType.BCRYPT) {
//            if (rounds < 4 || rounds > 31)
//                throw new IllegalArgumentException("Invalid Bcrypt cost factor " + rounds);
//        }
//        if (rounds < 1 || rounds > 8196)
//            throw new IllegalArgumentException("Iteration out of range " + rounds);
//
//
//        this.rounds = rounds;
//
//    }


}
