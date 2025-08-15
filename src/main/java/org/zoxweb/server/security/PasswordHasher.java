package org.zoxweb.server.security;

import org.zoxweb.shared.crypto.CIPassword;
import org.zoxweb.shared.crypto.CredentialHasher;
import org.zoxweb.shared.crypto.CryptoConst;

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
}
