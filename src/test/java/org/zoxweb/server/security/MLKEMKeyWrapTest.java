package org.zoxweb.server.security;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.SecretWithEncapsulation;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMExtractor;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMGenerator;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyPairGenerator;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.crypto.EncapsulatedKey;
import org.zoxweb.shared.crypto.EncryptedData;
import org.zoxweb.shared.crypto.KeyLockType;
import org.zoxweb.shared.util.SharedStringUtil;

import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KEM-DEM with ML-KEM in front of the existing {@link EncryptedData} sealing, first by hand
 * against a plain record, then through {@link EncapsulatedKey} and the {@link CryptoUtil} ML-KEM
 * wrap and unwrap for every parameter set. Uses BouncyCastle's ML-KEM classes, which compile at
 * the Java 8 source level.
 */
public class MLKEMKeyWrapTest {

    static final int PUBLIC_KEY_SIZE_768 = 1184;
    static final int PRIVATE_KEY_SIZE_768 = 2400;
    static final int KEM_CIPHERTEXT_SIZE_768 = 1088;
    static final int SHARED_SECRET_SIZE = 32;

    /** parameter set name, public key bytes, private key bytes, encapsulated key bytes */
    static final Object[][] SETS = {
            {CryptoConst.ML_KEM_512, 800, 1632, 768},
            {CryptoConst.ML_KEM_768, 1184, 2400, 1088},
            {CryptoConst.ML_KEM_1024, 1568, 3168, 1568},
    };

    static SecureRandom rnd;
    static MLKEMKeyPairGenerator kpg;

    @BeforeAll
    static void setUp() {
        SecUtil.defaultSecureRandom();
        rnd = new SecureRandom();
        kpg = new MLKEMKeyPairGenerator();
        kpg.init(new MLKEMKeyGenerationParameters(rnd, MLKEMParameters.ml_kem_768));
    }

    static MLKEMPublicKeyParameters pub(AsymmetricCipherKeyPair kp) {
        return (MLKEMPublicKeyParameters) kp.getPublic();
    }

    static MLKEMPrivateKeyParameters priv(AsymmetricCipherKeyPair kp) {
        return (MLKEMPrivateKeyParameters) kp.getPrivate();
    }

    /* ---------------------------------------------- the KEM by hand, DEM as is */

    @Test
    public void sizes() {
        AsymmetricCipherKeyPair kp = kpg.generateKeyPair();
        SecretWithEncapsulation enc = new MLKEMGenerator(rnd).generateEncapsulated(pub(kp));
        assertEquals(PUBLIC_KEY_SIZE_768, pub(kp).getEncoded().length);
        assertEquals(PRIVATE_KEY_SIZE_768, priv(kp).getEncoded().length);
        assertEquals(KEM_CIPHERTEXT_SIZE_768, enc.getEncapsulation().length);
        assertEquals(SHARED_SECRET_SIZE, enc.getSecret().length);
        assertEquals(CryptoUtil.MIN_KEY_BYTES, enc.getSecret().length, "the KEM secret is a full-size outer key");
        assertEquals(CryptoConst.ML_KEM_768, MLKEMParameters.ml_kem_768.getName(), "our alg value is the library's own name");
        assertEquals(KEM_CIPHERTEXT_SIZE_768, MLKEMParameters.ml_kem_768.getEncapsulationLength());
    }

    @Test
    public void kemSecretSealsAndOpensAnEncryptedData() throws Exception {
        AsymmetricCipherKeyPair recipient = kpg.generateKeyPair();

        // sender: KEM then DEM
        SecretWithEncapsulation enc = new MLKEMGenerator(rnd).generateEncapsulated(pub(recipient));
        byte[] kemCiphertext = enc.getEncapsulation();
        byte[] material = SecUtil.randomBytes(32);  // the AES-256 key being protected
        EncryptedData record = new EncryptedData();
        record.setHint("ML-KEM-768 wrapped");
        CryptoUtil.encryptData(record, enc.getSecret(), material);

        // recipient: decapsulate, then open
        byte[] secret = new MLKEMExtractor(priv(recipient)).extractSecret(kemCiphertext);
        assertArrayEquals(enc.getSecret(), secret);
        assertArrayEquals(material, CryptoUtil.decryptEncryptedData(record, secret));

        // the record survives its canonical form like any other
        EncryptedData stored = EncryptedData.fromCanonicalID(record.toCanonicalID());
        assertArrayEquals(material, CryptoUtil.decryptEncryptedData(stored, secret));

        // and the protected material is itself a working outer key: the chain continues below it
        EncryptedData leaf = new EncryptedData();
        byte[] payload = SharedStringUtil.getBytes("field value under the KEM-wrapped key");
        CryptoUtil.encryptData(leaf, material, payload);
        assertArrayEquals(payload, CryptoUtil.decryptEncryptedData(leaf, CryptoUtil.decryptEncryptedData(stored, secret)));
    }

    @Test
    public void wrongPrivateKeyIsRefused() throws Exception {
        AsymmetricCipherKeyPair recipient = kpg.generateKeyPair();
        AsymmetricCipherKeyPair intruder = kpg.generateKeyPair();
        SecretWithEncapsulation enc = new MLKEMGenerator(rnd).generateEncapsulated(pub(recipient));
        EncryptedData record = CryptoUtil.encryptData(new EncryptedData(), enc.getSecret(), SecUtil.randomBytes(32));

        // ML-KEM never fails to decapsulate: a wrong key yields a different secret, and the tag catches it
        byte[] wrongSecret = new MLKEMExtractor(priv(intruder)).extractSecret(enc.getEncapsulation());
        assertEquals(SHARED_SECRET_SIZE, wrongSecret.length);
        assertFalse(Arrays.equals(enc.getSecret(), wrongSecret));
        assertThrows(SignatureException.class, () -> CryptoUtil.decryptEncryptedData(record, wrongSecret));
    }

    @Test
    public void tamperedKemCiphertextIsRefused() throws Exception {
        AsymmetricCipherKeyPair recipient = kpg.generateKeyPair();
        SecretWithEncapsulation enc = new MLKEMGenerator(rnd).generateEncapsulated(pub(recipient));
        EncryptedData record = CryptoUtil.encryptData(new EncryptedData(), enc.getSecret(), SecUtil.randomBytes(32));

        byte[] tampered = enc.getEncapsulation().clone();
        tampered[100] ^= 1;
        byte[] secret = new MLKEMExtractor(priv(recipient)).extractSecret(tampered);
        assertFalse(Arrays.equals(enc.getSecret(), secret), "implicit rejection yields an unrelated secret");
        assertThrows(SignatureException.class, () -> CryptoUtil.decryptEncryptedData(record, secret));
    }

    @Test
    public void freshSecretPerEncapsulation() {
        AsymmetricCipherKeyPair recipient = kpg.generateKeyPair();
        SecretWithEncapsulation a = new MLKEMGenerator(rnd).generateEncapsulated(pub(recipient));
        SecretWithEncapsulation b = new MLKEMGenerator(rnd).generateEncapsulated(pub(recipient));
        assertFalse(Arrays.equals(a.getSecret(), b.getSecret()));
        assertFalse(Arrays.equals(a.getEncapsulation(), b.getEncapsulation()));
    }

    /* ------------------------------------- EncapsulatedKey through CryptoUtil */

    private static EncapsulatedKey keyRowFor(String publicKeyRegistryId) {
        EncapsulatedKey ek = new EncapsulatedKey();
        ek.setSubjectGUID(UUID.randomUUID().toString());
        ek.setReferenceGUID(UUID.randomUUID().toString());
        ek.setReferenceType("org.zoxweb.shared.data.FileInfoDAO");
        ek.setKeyLockType(KeyLockType.NVENTITY);
        ek.setKeyGUID(publicKeyRegistryId);
        return ek;
    }

    /** Every parameter set goes through the same code: only the name and the sizes differ. */
    @Test
    public void everyParameterSet() throws Exception {
        for (Object[] set : SETS) {
            String alg = (String) set[0];
            int publicKeySize = (Integer) set[1];
            int privateKeySize = (Integer) set[2];
            int encapsulatedKeySize = (Integer) set[3];

            CryptoUtil.MLKEMKeyPair kp = CryptoUtil.generateMLKEMKeyPair(alg);
            assertEquals(alg, kp.getAlgorithm());
            assertEquals(publicKeySize, kp.getPublicKey().length, alg);
            assertEquals(privateKeySize, kp.getPrivateKey().length, alg);

            EncapsulatedKey ek = CryptoUtil.createEncryptedKeyMLKEM(keyRowFor("pk"), alg, kp.getPublicKey());
            assertEquals(alg, ek.getAlgorithm());
            assertTrue(ek.isKEMWrapped());
            assertEquals(encapsulatedKeySize, ek.getKeySize(), alg + " key_size is the encapsulated key size in bytes");
            assertEquals(encapsulatedKeySize + 32 + EncryptedData.TAG_SIZE, ek.getEncryptedData().length, alg);
            assertEquals(encapsulatedKeySize, ek.getKEMCiphertext().length, alg);
            assertArrayEquals(Arrays.copyOf(ek.getEncryptedData(), encapsulatedKeySize), ek.getKEMCiphertext());
            assertEquals(ek.getSubjectGUID() + "|" + ek.getReferenceGUID() + "|pk|" + encapsulatedKeySize, ek.toBindingData());

            byte[] material = CryptoUtil.unwrapKeyMLKEM(ek, kp.getPrivateKey());
            assertEquals(32, material.length, alg);
            assertArrayEquals(material, CryptoUtil.unwrapKeyMLKEM(ek, kp.getPrivateKey()));

            // survives JSON and the canonical form
            EncapsulatedKey viaJson = GSONUtil.fromJSON(GSONUtil.toJSON(ek, false, false, false), EncapsulatedKey.class);
            assertArrayEquals(material, CryptoUtil.unwrapKeyMLKEM(viaJson, kp.getPrivateKey()), alg);
            assertEquals(ek.toCanonicalID(), viaJson.toCanonicalID());

            // the wrong parameter set's private key is refused as malformed
            for (Object[] other : SETS) {
                if (other != set) {
                    CryptoUtil.MLKEMKeyPair otherKp = CryptoUtil.generateMLKEMKeyPair((String) other[0]);
                    assertThrows(IllegalArgumentException.class, () -> CryptoUtil.unwrapKeyMLKEM(ek, otherKp.getPrivateKey()), alg + " vs " + other[0]);
                }
            }
        }
    }

    @Test
    public void unknownParameterSetIsRefused() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.generateMLKEMKeyPair("ML-KEM-2048"));
        CryptoUtil.MLKEMKeyPair kp = CryptoUtil.generateMLKEMKeyPair(CryptoConst.ML_KEM_768);
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.createEncryptedKeyMLKEM(keyRowFor("pk"), "ML-KEM-2048", kp.getPublicKey()));
        // a row relabeled with a KEM the library does not know
        EncapsulatedKey ek = CryptoUtil.createEncryptedKeyMLKEM(keyRowFor("pk"), CryptoConst.ML_KEM_768, kp.getPublicKey());
        ek.setAlgorithm("ML-KEM-2048");
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.unwrapKeyMLKEM(ek, kp.getPrivateKey()));
    }

    @Test
    public void encapsulatedKeyWrappedForAPublicKey() throws Exception {
        CryptoUtil.MLKEMKeyPair recipient = CryptoUtil.generateMLKEMKeyPair(CryptoConst.ML_KEM_768);
        String registryId = UUID.randomUUID().toString();

        // the server side: needs only the public key
        EncapsulatedKey ek = keyRowFor(registryId);
        CryptoUtil.createEncryptedKeyMLKEM(ek, CryptoConst.ML_KEM_768, recipient.getPublicKey());
        assertTrue(ek.isKEMWrapped());
        assertEquals(CryptoConst.ML_KEM_768, ek.getAlgorithm(), "alg names the KEM that produced the outer key");
        assertEquals(KEM_CIPHERTEXT_SIZE_768, ek.getKeySize());
        assertEquals(KEM_CIPHERTEXT_SIZE_768 + 32 + EncryptedData.TAG_SIZE, ek.getEncryptedData().length);
        assertEquals(registryId, ek.getKeyGUID(), "key_guid is the public key's registry entry");
        assertEquals(32, ek.getDataLength());
        assertEquals(CryptoConst.KDF_HKDF_SHA256, ek.getKDF(), "the DEM is unchanged");

        // the private-key holder opens it; the wrapper cannot
        byte[] material = CryptoUtil.unwrapKeyMLKEM(ek, recipient.getPrivateKey());
        assertEquals(32, material.length);
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.unwrapKey(ek, SecUtil.randomBytes(32)),
                "a KEM row is refused by the symmetric unwrap before any cryptography");

        // the material seals data below it like any key
        EncryptedData leaf = CryptoUtil.encryptData(new EncryptedData(), material, SharedStringUtil.getBytes("shared field"));
        assertArrayEquals(SharedStringUtil.getBytes("shared field"),
                CryptoUtil.decryptEncryptedData(leaf, CryptoUtil.unwrapKeyMLKEM(ek, recipient.getPrivateKey())));
    }

    @Test
    public void encapsulatedKeyRefusals() throws Exception {
        CryptoUtil.MLKEMKeyPair recipient = CryptoUtil.generateMLKEMKeyPair(CryptoConst.ML_KEM_768);
        CryptoUtil.MLKEMKeyPair intruder = CryptoUtil.generateMLKEMKeyPair(CryptoConst.ML_KEM_768);
        EncapsulatedKey ek = CryptoUtil.createEncryptedKeyMLKEM(keyRowFor("pk-1"), CryptoConst.ML_KEM_768, recipient.getPublicKey());
        String json = GSONUtil.toJSON(ek, false, false, false);

        // wrong private key of the right set: unrelated secret, tag refuses
        assertThrows(SignatureException.class, () -> CryptoUtil.unwrapKeyMLKEM(ek, intruder.getPrivateKey()));
        // malformed private key
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.unwrapKeyMLKEM(ek, new byte[16]));

        // encapsulated key altered in place: the decapsulated secret changes and the tag refuses it
        EncapsulatedKey ct = GSONUtil.fromJSON(json, EncapsulatedKey.class);
        ct.getEncryptedData()[7] ^= 1;
        assertThrows(SignatureException.class, () -> CryptoUtil.unwrapKeyMLKEM(ct, recipient.getPrivateKey()));

        // sealed part altered
        EncapsulatedKey sealed = GSONUtil.fromJSON(json, EncapsulatedKey.class);
        sealed.getEncryptedData()[KEM_CIPHERTEXT_SIZE_768 + 3] ^= 1;
        assertThrows(SignatureException.class, () -> CryptoUtil.unwrapKeyMLKEM(sealed, recipient.getPrivateKey()));

        // encapsulated key swapped for another wrap to the same key: different secret, tag refuses it
        EncapsulatedKey other = CryptoUtil.createEncryptedKeyMLKEM(keyRowFor("pk-1"), CryptoConst.ML_KEM_768, recipient.getPublicKey());
        EncapsulatedKey swapped = GSONUtil.fromJSON(json, EncapsulatedKey.class);
        System.arraycopy(other.getEncryptedData(), 0, swapped.getEncryptedData(), 0, KEM_CIPHERTEXT_SIZE_768);
        assertThrows(SignatureException.class, () -> CryptoUtil.unwrapKeyMLKEM(swapped, recipient.getPrivateKey()));

        // truncated below an encapsulated key plus a tag
        EncapsulatedKey truncated = GSONUtil.fromJSON(json, EncapsulatedKey.class);
        truncated.setEncryptedData(Arrays.copyOf(truncated.getEncryptedData(), KEM_CIPHERTEXT_SIZE_768 + 8));
        assertThrows(SignatureException.class, () -> CryptoUtil.unwrapKeyMLKEM(truncated, recipient.getPrivateKey()));

        // key_size moved: the split would land elsewhere, and the size is authenticated
        EncapsulatedKey moved = GSONUtil.fromJSON(json, EncapsulatedKey.class);
        moved.setKeySize(KEM_CIPHERTEXT_SIZE_768 - 16);
        assertThrows(SignatureException.class, () -> CryptoUtil.unwrapKeyMLKEM(moved, recipient.getPrivateKey()));

        // alg relabeled as a plain record, so it looks like a symmetric wrap: refused before any cryptography,
        // and the relabel is itself authenticated so the symmetric path cannot open it either
        EncapsulatedKey stripped = GSONUtil.fromJSON(json, EncapsulatedKey.class);
        stripped.setAlgorithm(CryptoConst.ALG_A256GCM);
        assertFalse(stripped.isKEMWrapped());
        assertNull(stripped.getKEMCiphertext());
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.unwrapKeyMLKEM(stripped, recipient.getPrivateKey()));
        assertThrows(SignatureException.class, () -> CryptoUtil.unwrapKey(stripped, recipient.getPrivateKey()));

        // alg relabeled as another parameter set: the row's key_size no longer matches that set
        EncapsulatedKey relabeled = GSONUtil.fromJSON(json, EncapsulatedKey.class);
        relabeled.setAlgorithm(CryptoConst.ML_KEM_1024);
        assertThrows(SignatureException.class, () -> CryptoUtil.unwrapKeyMLKEM(relabeled, recipient.getPrivateKey()));

        // identity re-pointed
        EncapsulatedKey subject = GSONUtil.fromJSON(json, EncapsulatedKey.class);
        subject.setSubjectGUID(UUID.randomUUID().toString());
        assertThrows(SignatureException.class, () -> CryptoUtil.unwrapKeyMLKEM(subject, recipient.getPrivateKey()));

        // public key registry entry re-pointed
        EncapsulatedKey keyGuid = GSONUtil.fromJSON(json, EncapsulatedKey.class);
        keyGuid.setKeyGUID("pk-2");
        assertThrows(SignatureException.class, () -> CryptoUtil.unwrapKeyMLKEM(keyGuid, recipient.getPrivateKey()));

        // a symmetric row handed to the KEM unwrap
        EncapsulatedKey symmetric = CryptoUtil.createEncryptedKey(keyRowFor(null), SecUtil.randomBytes(32));
        assertFalse(symmetric.isKEMWrapped());
        assertEquals(CryptoConst.ALG_A256GCM, symmetric.getAlgorithm());
        assertEquals(32, symmetric.getKeySize());
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.unwrapKeyMLKEM(symmetric, recipient.getPrivateKey()));

        // wrong public key size at wrap time
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.createEncryptedKeyMLKEM(keyRowFor("pk-1"), CryptoConst.ML_KEM_768, new byte[100]));
    }

    @Test
    public void rekeyToAnotherPublicKeyAndSet() throws Exception {
        CryptoUtil.MLKEMKeyPair first = CryptoUtil.generateMLKEMKeyPair(CryptoConst.ML_KEM_768);
        CryptoUtil.MLKEMKeyPair second = CryptoUtil.generateMLKEMKeyPair(CryptoConst.ML_KEM_1024);
        EncapsulatedKey ek = CryptoUtil.createEncryptedKeyMLKEM(keyRowFor("pk-1"), CryptoConst.ML_KEM_768, first.getPublicKey());
        byte[] material = CryptoUtil.unwrapKeyMLKEM(ek, first.getPrivateKey());

        CryptoUtil.rekeyEncryptedKeyMLKEM(ek, first.getPrivateKey(), CryptoConst.ML_KEM_1024, second.getPublicKey());
        assertEquals(CryptoConst.ML_KEM_1024, ek.getAlgorithm());
        assertEquals(1568, ek.getKeySize(), "key_size follows the new parameter set");
        assertArrayEquals(material, CryptoUtil.unwrapKeyMLKEM(ek, second.getPrivateKey()), "same material, new encapsulation");
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.unwrapKeyMLKEM(ek, first.getPrivateKey()), "old set's key no longer fits");
    }

    /** One entity key shared with two subjects: two rows, same material, each opened by its own private key. */
    @Test
    public void shareOneKeyWithTwoRecipients() throws Exception {
        CryptoUtil.MLKEMKeyPair alice = CryptoUtil.generateMLKEMKeyPair(CryptoConst.ML_KEM_768);
        CryptoUtil.MLKEMKeyPair bob = CryptoUtil.generateMLKEMKeyPair(CryptoConst.ML_KEM_512);
        byte[] entityKey = SecUtil.randomBytes(32);
        String entity = UUID.randomUUID().toString();

        EncapsulatedKey forAlice = keyRowFor("alice-pk");
        forAlice.setReferenceGUID(entity);
        CryptoUtil.wrapKeyMLKEM(forAlice, alice.getAlgorithm(), alice.getPublicKey(), entityKey);
        EncapsulatedKey forBob = keyRowFor("bob-pk");
        forBob.setReferenceGUID(entity);
        CryptoUtil.wrapKeyMLKEM(forBob, bob.getAlgorithm(), bob.getPublicKey(), entityKey);

        assertArrayEquals(entityKey, CryptoUtil.unwrapKeyMLKEM(forAlice, alice.getPrivateKey()));
        assertArrayEquals(entityKey, CryptoUtil.unwrapKeyMLKEM(forBob, bob.getPrivateKey()));
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.unwrapKeyMLKEM(forAlice, bob.getPrivateKey()), "512 key against a 768 row");
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.unwrapKeyMLKEM(forBob, alice.getPrivateKey()), "768 key against a 512 row");
    }
}
