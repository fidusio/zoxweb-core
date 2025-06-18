package org.zoxweb.server.security;

import org.zoxweb.shared.crypto.CIPassword;
import org.zoxweb.shared.crypto.CredentialHasher;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.security.AccessException;
import org.zoxweb.shared.util.SUS;

import java.security.NoSuchAlgorithmException;

public class CIPasswordHasher
        implements CredentialHasher<CIPassword> {
    private CryptoConst.HASHType hashType;

    private int iteration;

    public CIPasswordHasher() {
    }

    public CIPasswordHasher(CryptoConst.HASHType hashType, int iteration) {
        setHashType(hashType);
        setIteration(iteration);
    }

    @Override
    public CIPassword hash(String password) {
        try {
            return HashUtil.toPassword(hashType, 0, iteration, password);
        } catch (NoSuchAlgorithmException e) {
            throw new AccessException(e.getMessage());
        }
    }

    @Override
    public CIPassword hash(byte[] password) {
        try {
            return HashUtil.toPassword(hashType, 0, iteration, password);
        } catch (NoSuchAlgorithmException e) {
            throw new AccessException(e.getMessage());
        }
    }

    @Override
    public CIPassword hash(char[] password) {
        try {
            return HashUtil.toPassword(hashType, 0, iteration, new String(password));
        } catch (NoSuchAlgorithmException e) {
            throw new AccessException(e.getMessage());
        }
    }


    public int getIteration() {
        return iteration;
    }

    public CIPasswordHasher setIteration(int iteration) {
        if (getHashType() == CryptoConst.HASHType.BCRYPT) {
            if (iteration < 4 || iteration > 31)
                throw new IllegalArgumentException("Invalid Bcrypt cost factor " + iteration);
        }
        if (iteration < 1 || iteration > 8196)
            throw new IllegalArgumentException("Iteration out of range " + iteration);


        this.iteration = iteration;
        return this;
    }

    public CryptoConst.HASHType getHashType() {
        return hashType;
    }

    public CIPasswordHasher setHashType(CryptoConst.HASHType hashType) {
        SUS.checkIfNulls("Null hash type", hashType);
        this.hashType = hashType;
        return this;
    }

}
