package org.zoxweb.server.security;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.server.util.ReflectionUtil;
import org.zoxweb.shared.annotation.SecurityProp;
import org.zoxweb.shared.crypto.CIPassword;
import org.zoxweb.shared.crypto.CredentialHasher;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.security.*;
import org.zoxweb.shared.util.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.lang.reflect.Method;
import java.security.*;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SecUtil {
    public static CryptoConst.SecureRandomType SECURE_RANDOM_ALGO = null;
    public static final SecUtil SINGLETON = new SecUtil();
    private final Map<Method, ResourceSecurity> methodResourceSecurityMap = new LinkedHashMap<>();
    private final Map<String, CredentialHasher<?>> credentialHasherMap = new LinkedHashMap<>();
    private volatile SecureRandom defaultSecureRandom = null;

    private SecUtil() {
        addCredentialHasher(new BCryptPasswordHasher(10));
        addCredentialHasher(new SHAPasswordHasher(8196));
    }


    public JWT parseJWT(String token)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException, NullPointerException, IllegalArgumentException {
        SUS.checkIfNulls("Null token", token);
        String[] tokens = token.trim().split("\\.");

        if (tokens.length < 2 || tokens.length > 3) {
            throw new IllegalArgumentException("Invalid token JWT token");
        }

        NVGenericMap nvgmHeader = GSONUtil.fromJSONGenericMap(
                SharedBase64.decodeAsString(SharedBase64.Base64Type.URL, tokens[JWT.JWTField.HEADER.ordinal()]),
                JWTHeader.NVC_JWT_HEADER,
                SharedBase64.Base64Type.URL);//GSONUtil.fromJSON(SharedBase64.decodeAsString(Base64Type.URL,tokens[JWTField.HEADER.ordinal()]), JWTHeader.class);
        NVGenericMap nvgmPayload = GSONUtil.fromJSONGenericMap(
                SharedBase64.decodeAsString(SharedBase64.Base64Type.URL, tokens[JWT.JWTField.PAYLOAD.ordinal()]),
                JWTPayload.NVC_JWT_PAYLOAD, SharedBase64.Base64Type.URL);
        if (nvgmPayload == null) {
            throw new SecurityException("Invalid JWT");
        }
        JWT ret = new JWT();

        //jwtPayload = GSONUtil.fromJSON(SharedStringUtil.toString(SharedBase64.decode(Base64Type.URL,tokens[JWTToken.PAYLOAD.ordinal()])), JWTPayload.class);
        JWTPayload jwtPayload = ret.getPayload();
        JWTHeader jwtHeader = ret.getHeader();
        if (jwtHeader == null || jwtPayload == null) {
            throw new SecurityException("Invalid JWT");
        }
        jwtPayload.setProperties(nvgmPayload);
        jwtHeader.setProperties(nvgmHeader);

        SUS.checkIfNulls("Null jwt header or parameters", jwtHeader, jwtHeader.getJWTAlgorithm());
        //		JWT ret = new JWT();
        //ret.setHeader(jwtHeader);
        //ret.setPayload(jwtPayload);
        switch (jwtHeader.getJWTAlgorithm()) {
            case HS256:
            case HS512:
            case RS256:
            case RS512:
            case ES256:
            case ES512:
                if (tokens.length != JWT.JWTField.values().length) {
                    throw new IllegalArgumentException("Invalid token JWT token length expected 3");
                }
                ret.setHash(tokens[JWT.JWTField.HASH.ordinal()]);
                break;
            case none:
                if (tokens.length != JWT.JWTField.values().length - 1) {
                    throw new IllegalArgumentException("Invalid token JWT token length expected 2");
                }
                break;
        }

        return ret;
    }

    public JWT decodeJWT(String key, String token)
            throws IOException,
            SecurityException, NullPointerException, IllegalArgumentException, GeneralSecurityException {
        return decodeJWT(key != null ? SharedStringUtil.getBytes(key) : null, token);
    }

    public JWT decodeJWT(byte[] key, String token)
            throws IOException,
            SecurityException, GeneralSecurityException {

        JWT jwt;
        try {
            jwt = parseJWT(token);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new SecurityException();
        }


        String[] tokens = token.trim().split("\\.");
        CryptoConst.JWTAlgo jwtAlgo = jwt.getHeader().getJWTAlgorithm();
        switch (jwtAlgo) {
            case HS256:
            case HS384:
            case HS512:
                SUS.checkIfNulls("Null key", key);
                if (tokens.length != JWT.JWTField.values().length) {
                    throw new SecurityException("Invalid token");
                }
                Mac shaHMAC = HashUtil.getMac(jwtAlgo.getSignatureAlgo());
                SecretKeySpec secret_key = new SecretKeySpec(key, jwtAlgo.getSignatureAlgo().getName());
                shaHMAC.init(secret_key);
                shaHMAC.update(SharedStringUtil.getBytes(tokens[JWT.JWTField.HEADER.ordinal()]));

                shaHMAC.update((byte) '.');
                byte[] b64Hash = shaHMAC.doFinal(SharedStringUtil.getBytes(tokens[JWT.JWTField.PAYLOAD.ordinal()]));

                if (!SharedBase64.encodeAsString(SharedBase64.Base64Type.URL, b64Hash).equals(jwt.getHash())) {
                    throw new SecurityException("Invalid token");
                }
                break;

            case none:
                if (tokens.length != JWT.JWTField.values().length - 1) {
                    throw new SecurityException("Invalid token");
                }
                break;
            case RS256:
            case RS384:
            case RS512:
            case ES256:
            case ES384:
            case ES512:
                SUS.checkIfNulls("Null key", key);
                if (tokens.length != JWT.JWTField.values().length) {
                    throw new SecurityException("Invalid token");
                }
                PublicKey publicKey = CryptoUtil.generatePublicKey(jwtAlgo.getSignatureAlgo().getCryptoAlgo().getName(), key);

                if (!CryptoUtil.verify(jwtAlgo.getSignatureAlgo(), publicKey,
                        SharedStringUtil.getBytes(
                                tokens[JWT.JWTField.HEADER.ordinal()] + "." + tokens[JWT.JWTField.PAYLOAD.ordinal()]),
                        SharedBase64.decode(SharedBase64.Base64Type.URL, jwt.getHash()))) {
                    throw new SecurityException("Invalid token");
                }
                break;

        }

        return jwt;
    }


    public CIPassword fromCanonicalID(String passwordCanID) throws NoSuchAlgorithmException {
        SUS.checkIfNulls("null passwordCanID", passwordCanID);
        CredentialHasher<CIPassword> ch = findCredentialHasherByCanID(passwordCanID);
        if (ch == null)
            throw new NoSuchAlgorithmException("Not found");

        return ch.fromCanonicalID(passwordCanID);
    }

    public synchronized CredentialHasher<CIPassword> findCredentialHasherByCanID(String passwordCanID) {
        String[] tokens = SharedStringUtil.parseString(passwordCanID, "\\$", true);
        if (tokens.length > 1)
            return lookupCredentialHasher(tokens[0]);
        return null;
    }



    public synchronized SecUtil addCredentialHasher(CredentialHasher<?> credentialHasher) {
        credentialHasherMap.put(DataEncoder.StringLower.encode(credentialHasher.getName()), credentialHasher);
        for (String algo : credentialHasher.supportedAlgorithms())
            credentialHasherMap.put(DataEncoder.StringLower.encode(algo), credentialHasher);
        return this;
    }

    public String[] credentialHasherAlgorithms() {
        return credentialHasherMap.keySet().toArray(new String[0]);
    }

    public synchronized void removeCredentialHasher(CredentialHasher<?> credentialHasher) {
        credentialHasherMap.remove(DataEncoder.StringLower.encode(credentialHasher.getName()));
        for (String algo : credentialHasher.supportedAlgorithms())
            credentialHasherMap.remove(DataEncoder.StringLower.encode(algo));
    }

    public synchronized <T> CredentialHasher<T> lookupCredentialHasher(String name) {
        return (CredentialHasher<T>) credentialHasherMap.get(DataEncoder.StringLower.encode(name));
    }

    public synchronized <T> CredentialHasher<T> lookupCredentialHasher(CryptoConst.HashType hashType) {
        return lookupCredentialHasher(hashType.getName());
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


    public boolean isPasswordValid(final CIPassword ciPassword, String password)
            throws NullPointerException, IllegalArgumentException {
        SUS.checkIfNulls("Null values", ciPassword, password);
        return isPasswordValid(ciPassword, SharedStringUtil.getBytes(password));
    }

    public boolean isPasswordValid(final CIPassword ciPassword, byte[] password)
            throws NullPointerException, IllegalArgumentException {
        SUS.checkIfNulls("Null values", ciPassword, password);
        CredentialHasher<CIPassword> passwordHasher = lookupCredentialHasher(ciPassword.getAlgorithm());
        if (passwordHasher == null)
            throw new AccessSecurityException("no credential hasher found for: " + ciPassword.getAlgorithm());
        return passwordHasher.isPasswordValid(ciPassword, password);
    }

    public boolean isPasswordValid(final CIPassword ciPassword, char[] password)
            throws NullPointerException, IllegalArgumentException {
        SUS.checkIfNulls("Null values", ciPassword, password);
        CredentialHasher<CIPassword> passwordHasher = lookupCredentialHasher(ciPassword.getAlgorithm());
        if (passwordHasher == null)
            throw new AccessSecurityException("no credential hasher found for: " + ciPassword.getAlgorithm());
        return passwordHasher.isPasswordValid(ciPassword, password);
    }

    public void validatePassword(final CIPassword ciPassword, final char[] password)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException {

        SUS.checkIfNulls("Null values", ciPassword, password);
        if (isPasswordValid(ciPassword, password))
            return; // we hava a valid password
        // password validation failed,
        throw new AccessSecurityException("Invalid Credentials");
    }


    public void validatePassword(final CIPassword ciPassword, final byte[] password)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException {

        SUS.checkIfNulls("Null values", ciPassword, password);
        if (isPasswordValid(ciPassword, password))
            return; // we hava a valid password
        // password validation failed,
        throw new AccessSecurityException("Invalid Credentials");
    }

    public void validatePassword(final CIPassword ciPassword, String password)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException {
        SUS.checkIfNulls("Null values", ciPassword, password);
        if (isPasswordValid(ciPassword, password))
            return; // we hava a valid password
        // password validation failed,
        throw new AccessSecurityException("Invalid Credentials");
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

    public SecureRandom defaultSecureRandom() {
        if (SECURE_RANDOM_ALGO == null && defaultSecureRandom == null) {

            synchronized (this) {
                if (SECURE_RANDOM_ALGO == null && defaultSecureRandom == null) {
                    for (CryptoConst.SecureRandomType srt : CryptoConst.SecureRandomType.values()) {
                        try {
                            defaultSecureRandom = newSecureRandom(srt);
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

        return defaultSecureRandom;
    }

    public byte[] generateRandomBytes(int size)
            throws NullPointerException, IllegalArgumentException {
        return generateRandomBytes(null, size);
    }

    public byte[] generateRandomBytes(SecureRandom sr, int size)
            throws NullPointerException, IllegalArgumentException {
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


    public String secProvidersToString(boolean detailed) {
        StringBuilder sb = new StringBuilder();

        // Loop over each provider and print its details
        for (Provider provider : Security.getProviders()) {
            if (sb.length() > 0)
                sb.append('\n');
            sb.append(secProviderToString(provider, detailed));
        }
        return sb.toString();
    }

    public String secProviderToString(Provider provider, boolean detailed) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);


        // Loop over each provider and print its details

        pw.println("Provider: " + provider.getName());
        pw.println("Version: " + provider.getVersion());
        pw.println("Info: " + provider.getInfo());

        // Print the services offered by this provider
        if (detailed)
            provider.getServices().forEach(service -> {
                pw.println("\t" + service);
            });


        return sw.toString();
    }

    /**
     * Add a security resource provider
     *
     * @param provider to be added
     * @return the index on the provider in the array of providers
     */
    public synchronized int addProvider(Provider provider) {
        return addProviderAt(provider, 0);
    }

    public synchronized int addProviderAt(Provider provider, int position) {
        SUS.checkIfNulls("Null provider", provider);
        Provider[] providers = Security.getProviders();
        for (int i = 0; i < providers.length; i++) {
            if (providers[i].equals(provider)) {
                return i;
            }
        }
        return Security.insertProviderAt(provider, position);
    }

    public synchronized boolean removeProvider(String name) {
        Security.removeProvider(name);
        return Security.getProvider(name) == null;
    }

    public synchronized Provider getProvider(String name) {
        return Security.getProvider(name);
    }

}
