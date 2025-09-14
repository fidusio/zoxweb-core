package org.zoxweb.server.security;

import org.zoxweb.shared.crypto.CIPassword;
import org.zoxweb.shared.crypto.CredentialHasher;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.security.AccessSecurityException;

public abstract class PasswordHasher
        implements CredentialHasher<CIPassword> {

    private final CryptoConst.HashType hashType;
    private final int rounds;
    private final String name;
    private final String[] algorithms;


    protected PasswordHasher(String name, CryptoConst.HashType hashType, String[] algorithms, int rounds) {
        this.name = name;
        this.hashType = hashType;
        this.rounds = rounds;
        this.algorithms = algorithms;
    }

    /**
     * @return
     */
    @Override
    public String[] supportedAlgorithms() {
        return algorithms;
    }

    /**
     * @param algoName
     * @return
     */
    @Override
    public boolean isAlgorithmSupported(String algoName) {
        for (String s : algorithms) {
            if (s.equalsIgnoreCase(algoName))
                return true;
        }
        return false;
    }

    @Override
    public int getRounds() {
        return rounds;
    }

    @Override
    public String getName() {
        return name;
    }

    public CryptoConst.HashType getHashType() {
        return hashType;
    }


    /**
     *
     * @param oldCI
     * @param password
     * @param newPassword
     * @return
     * @throws AccessSecurityException
     */
    @Override
    public CIPassword update(CIPassword oldCI, String password, String newPassword) throws AccessSecurityException {
        if (SecUtil.SINGLETON.lookupCredentialHasher(oldCI.getAlgorithm()).validate(oldCI, password)) {
            return merge(oldCI, hash(newPassword));
        }
        throw new AccessSecurityException("Invalid password update failed");
    }

    /**
     *
     * @param oldCI
     * @param password
     * @param newPassword
     * @return
     * @throws AccessSecurityException
     */
    @Override
    public CIPassword update(CIPassword oldCI, byte[] password, byte[] newPassword) throws AccessSecurityException {
        if (SecUtil.SINGLETON.lookupCredentialHasher(oldCI.getAlgorithm()).validate(oldCI, password)) {
            return merge(oldCI, hash(newPassword));
        }
        throw new AccessSecurityException("Invalid password update failed");
    }

    /**
     *
     * @param oldCI
     * @param password
     * @param newPassword
     * @return
     * @throws AccessSecurityException
     */
    @Override
    public CIPassword update(CIPassword oldCI, char[] password, char[] newPassword) throws AccessSecurityException {
        if (SecUtil.SINGLETON.lookupCredentialHasher(oldCI.getAlgorithm()).validate(oldCI, password)) {
            return merge(oldCI, hash(newPassword));
        }
        throw new AccessSecurityException("Invalid password update failed");
    }

    protected static CIPassword merge(CIPassword oldCI, CIPassword newCI) {
        newCI.setDescription(oldCI.getDescription());
        newCI.setName(oldCI.getName());
        newCI.setSubjectGUID(oldCI.getSubjectGUID());
        newCI.setReferenceID(oldCI.getReferenceID());
        newCI.setGUID(oldCI.getGUID());
        return newCI;
    }
}
