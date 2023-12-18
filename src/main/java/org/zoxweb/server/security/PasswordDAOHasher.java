package org.zoxweb.server.security;

import org.zoxweb.shared.crypto.CredentialHasher;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.crypto.PasswordDAO;
import org.zoxweb.shared.security.AccessException;
import org.zoxweb.shared.util.SharedUtil;

import java.security.NoSuchAlgorithmException;

public class PasswordDAOHasher
implements CredentialHasher<PasswordDAO>
{
    private CryptoConst.HASHType hashType;

    private int iteration;

    public PasswordDAOHasher(){}

    public PasswordDAOHasher(CryptoConst.HASHType hashType, int iteration)
    {
        setHashType(hashType);
        setIteration(iteration);
    }

    @Override
    public PasswordDAO hash(String password)
    {
        try
        {
            return HashUtil.toPassword(hashType, 0, iteration, password);
        }
        catch(NoSuchAlgorithmException e)
        {
            throw new AccessException(e.getMessage());
        }
    }

    @Override
    public PasswordDAO hash(byte[] password)
    {
        try
        {
            return HashUtil.toPassword(hashType, 0, iteration, password);
        }
        catch(NoSuchAlgorithmException e)
        {
            throw new AccessException(e.getMessage());
        }
    }

    @Override
    public PasswordDAO hash(char[] password) {
        try
        {
            return HashUtil.toPassword(hashType, 0, iteration, new String(password));
        }
        catch(NoSuchAlgorithmException e)
        {
            throw new AccessException(e.getMessage());
        }
    }


    public int getIteration() {
        return iteration;
    }

    public PasswordDAOHasher setIteration(int iteration)
    {
        if(getHashType() == CryptoConst.HASHType.BCRYPT)
        {
            if(iteration < 4 || iteration > 31)
                throw new IllegalArgumentException("Invalid Bcrypt cost factor " + iteration);
        }
        if(iteration < 64 || iteration > 8196)
            throw new IllegalArgumentException("Iteration out of range " + iteration);


        this.iteration = iteration;
        return this;
    }

    public CryptoConst.HASHType getHashType() {
        return hashType;
    }

    public PasswordDAOHasher setHashType(CryptoConst.HASHType hashType) {
        SharedUtil.checkIfNulls("Null hashtype", hashType);
        this.hashType = hashType;
        return this;
    }

}
