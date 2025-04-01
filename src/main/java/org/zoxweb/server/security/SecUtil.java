package org.zoxweb.server.security;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.util.ReflectionUtil;
import org.zoxweb.shared.annotation.SecurityProp;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.security.ResourceSecurity;
import org.zoxweb.shared.security.SecurityProfile;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedStringUtil;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class SecUtil {
    public static CryptoConst.SecureRandomType SECURE_RANDOM_ALGO = null;

    public static final SecUtil SINGLETON = new SecUtil();
    private final Map<Method, ResourceSecurity> methodResourceSecurityMap = new LinkedHashMap<>();

    private SecUtil() {
    }

    /**
     * This method read the assigned SecurityProp of method and if it exists it will apply it to the security profile
     *
     * @param method          to inspect can't be null
     * @param securityProfile if null SecurityProfile will be created
     * @return ResourceSecurity if applicable or null
     */
    public synchronized ResourceSecurity applyAndCacheSecurityProfile(Method method, SecurityProfile securityProfile) {
        SUS.checkIfNulls("Method null", method);
        SecurityProp sp = ReflectionUtil.getAnnotationFromMethod(method, SecurityProp.class);
        if (sp != null) {
            ResourceSecurity ret = applySecurityProp(securityProfile != null ? securityProfile : new SecurityProfile(), sp);
            methodResourceSecurityMap.put(method, ret);
            return ret;
        }
        return null;
    }

    /**
     * Apply the SecurityProp to security profile and return it as ResourceSecurity
     *
     * @param securityProfile to be applied to
     * @param securityProp    to be applied
     * @return ResourceSecurity or null
     */
    public ResourceSecurity applySecurityProp(SecurityProfile securityProfile, SecurityProp securityProp) {
        if (securityProfile != null && securityProp != null) {
            String[] roles = SUS.isEmpty(securityProp.roles()) ? null : SharedStringUtil.parseString(securityProp.roles(), ",", " ", "\t");
            String[] permissions = SUS.isEmpty(securityProp.permissions()) ? null : SharedStringUtil.parseString(securityProp.permissions(), ",", " ", "\t");
            CryptoConst.AuthenticationType[] authTypes = securityProp.authentications();
            String[] restrictions = securityProp.restrictions().length > 0 ? securityProp.restrictions() : null;
            securityProfile.setPermissions(permissions);
            securityProfile.setRoles(roles);
            securityProfile.setAuthenticationTypes(authTypes);
            securityProfile.setRestrictions(restrictions);
            securityProfile.setProtocols(securityProp.protocols());
            return securityProfile;
        }
        return null;
    }

    /**
     * Lookup cached ResourceSecurity associated with method
     *
     * @param method to look for
     * @return associated ResourceSecurity if it exists
     */
    public ResourceSecurity lookupCachedResourceSecurity(Method method) {
        return methodResourceSecurityMap.get(method);
    }

    /**
     * Remove cached resource security from a method
     *
     * @param method that has been cached
     * @return ResourceSecurity if it existed
     */
    public synchronized ResourceSecurity removeCachedResourceSecurity(Method method) {
        return methodResourceSecurityMap.remove(method);
    }

    /**
     * @return all cached methods
     */
    public synchronized Method[] getAllCachedMethods() {
        return methodResourceSecurityMap.keySet().toArray(new Method[0]);
    }


    public SSLContext initSSLContext(String keyStoreFilename,
                                     String keyStoreType,
                                     final char[] keyStorePassword,
                                     final char[] crtPassword,
                                     String trustStoreFilename,
                                     final char[] trustStorePassword)
            throws GeneralSecurityException, IOException {

        return initSSLContext("TLS", null, new File(keyStoreFilename),
                keyStoreType,
                keyStorePassword,
                crtPassword,
                trustStoreFilename != null ? new File(trustStoreFilename) : null, trustStorePassword);

    }


    public SSLContext initSSLContext(String protocol,
                                     final Provider provider,
                                     final File keyStoreFilename,
                                     String keyStoreType,
                                     final char[] keyStorePassword,
                                     final char[] crtPassword,
                                     final File trustStoreFilename,
                                     final char[] trustStorePassword)
            throws GeneralSecurityException, IOException {
        FileInputStream ksfis = null;
        FileInputStream tsfis = null;

        try {
            ksfis = new FileInputStream(keyStoreFilename);
            tsfis = trustStoreFilename != null ? new FileInputStream(trustStoreFilename) : null;
            return initSSLContext(protocol, provider, ksfis, keyStoreType, keyStorePassword, crtPassword, tsfis, trustStorePassword);
        } finally {
            IOUtil.close(ksfis);
            IOUtil.close(tsfis);
        }

    }

    public SSLContext initSSLContext(String protocol,
                                     final Provider provider,
                                     final InputStream keyStoreIS,
                                     String keyStoreType,
                                     final char[] keyStorePassword,
                                     final char[] crtPassword,
                                     final InputStream trustStoreIS,
                                     final char[] trustStorePassword)
            throws GeneralSecurityException, IOException {
        KeyStore ks = CryptoUtil.loadKeyStore(keyStoreIS, keyStoreType, keyStorePassword);
        KeyStore ts = null;
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");

        if (trustStoreIS != null) {
            ts = CryptoUtil.loadKeyStore(trustStoreIS, keyStoreType, trustStorePassword);
        }

        if (crtPassword != null) {
            kmf.init(ks, crtPassword);
            tmf.init(ts != null ? ts : ks);
        } else {
            kmf.init(ks, keyStorePassword);
            tmf.init(ts != null ? ts : ks);
        }

        SSLContext sslContext = provider != null ? SSLContext.getInstance(protocol != null ? protocol : "TLS", provider) : SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, defaultSecureRandom());
        return sslContext;
    }

    public SSLContext initSSLContext(final String protocol,
                                     final Provider provider,
                                     final KeyStore keyStore,
                                     final char[] keyStorePassword,
                                     final char[] crtPassword,
                                     final KeyStore trustStore)
            throws GeneralSecurityException {


        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");


        if (crtPassword != null) {
            kmf.init(keyStore, crtPassword);
            tmf.init(keyStore != null ? trustStore : keyStore);
        } else {
            kmf.init(keyStore, keyStorePassword);
            tmf.init(trustStore != null ? trustStore : keyStore);
        }

        SSLContext sslContext = provider != null ? SSLContext.getInstance(protocol != null ? protocol : "TLS", provider) : SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, defaultSecureRandom());
        return sslContext;
    }

    public SecureRandom newSecureRandom(CryptoConst.SecureRandomType srt)
            throws NoSuchAlgorithmException {
        switch (srt) {
            case SECURE_RANDOM_VM_STRONG:
                // very bad and blocking on linux
                // not recommended yet
                return SecureRandom.getInstanceStrong();
            case SECURE_RANDOM_VM_DEFAULT:
                return new SecureRandom();
            case NATIVE:
                return SecureRandom.getInstance(CryptoConst.SecureRandomType.NATIVE.getName());
            case SHA1PRNG:
                return SecureRandom.getInstance(CryptoConst.SecureRandomType.SHA1PRNG.getName());
            default:
                return SecureRandom.getInstance(SECURE_RANDOM_ALGO.getName());
        }
    }

    public SecureRandom defaultSecureRandom()
            throws NoSuchAlgorithmException
    {
        if (SECURE_RANDOM_ALGO == null) {

            synchronized (this) {
                if (SECURE_RANDOM_ALGO == null) {
                    for (CryptoConst.SecureRandomType srt : CryptoConst.SecureRandomType.values()) {
                        try {
                            newSecureRandom(srt);
                            SECURE_RANDOM_ALGO = srt;
                            //System.out.println("Default secure algorithm:"+srt);
                            break;
                        } catch (NoSuchAlgorithmException e) {
                            //e.printStackTrace();
                        }
                    }
                }
            }

        }

        return newSecureRandom(SECURE_RANDOM_ALGO);
    }

    public byte[] generateRandomBytes(SecureRandom sr, int size)
            throws NullPointerException, IllegalArgumentException, NoSuchAlgorithmException {
        if (size < 1) {
            throw new IllegalArgumentException("invalid size " + size + " must be greater than zero.");
        }

        if (sr == null) {
            sr = defaultSecureRandom();
        }

        byte[] ret = new byte[size];
        sr.nextBytes(ret);

        return ret;
    }
}
