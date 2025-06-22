package org.zoxweb.server.security;

import org.zoxweb.shared.crypto.CIPassword;
import org.zoxweb.shared.crypto.CredentialHasher;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.security.AccessException;

import java.security.NoSuchAlgorithmException;

public class CIPasswordHasher
        implements CredentialHasher<CIPassword> {
    private final CryptoConst.HashType hashType;

    private int iterations;
    private final String name;

//    public CIPasswordHasher() {
//    }

    public CIPasswordHasher(String name, CryptoConst.HashType hashType, int iterations) {
        this.name = name;
        this.hashType = hashType;
        setIterations(iterations);
    }

    @Override
    public CIPassword hash(String password) {
        try {
            return HashUtil.toPassword(hashType, 0, iterations, password);
        } catch (NoSuchAlgorithmException e) {
            throw new AccessException(e.getMessage());
        }
    }

    @Override
    public CIPassword hash(byte[] password) {
        try {
            return HashUtil.toPassword(hashType, 0, iterations, password);
        } catch (NoSuchAlgorithmException e) {
            throw new AccessException(e.getMessage());
        }
    }

    @Override
    public CIPassword hash(char[] password) {
        try {
            return HashUtil.toPassword(hashType, 0, iterations, new String(password));
        } catch (NoSuchAlgorithmException e) {
            throw new AccessException(e.getMessage());
        }
    }


    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iteration) {
        if (getHashType() == CryptoConst.HashType.BCRYPT) {
            if (iteration < 4 || iteration > 31)
                throw new IllegalArgumentException("Invalid Bcrypt cost factor " + iteration);
        }
        if (iteration < 1 || iteration > 8196)
            throw new IllegalArgumentException("Iteration out of range " + iteration);


        this.iterations = iteration;

    }

    public CryptoConst.HashType getHashType() {
        return hashType;
    }

    /**
     * @return the name of the object
     */
    @Override
    public String getName() {
        return name;
    }
}
