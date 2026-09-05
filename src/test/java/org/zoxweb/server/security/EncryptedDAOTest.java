package org.zoxweb.server.security;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.crypto.EncapsulatedKey;
import org.zoxweb.shared.crypto.EncryptedData;
import org.zoxweb.shared.crypto.KeyLockType;
import org.zoxweb.shared.util.SharedStringUtil;

import java.security.SignatureException;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The AES-256-GCM record ({@link EncryptedData}) and the wrapped key ({@link EncapsulatedKey}, an
 * EncryptedData whose plaintext is a key): round trips, the canonical form, tamper detection on
 * every authenticated attribute, and refusal of a wrapped key re-pointed at another subject,
 * reference or wrapping key.
 */
public class EncryptedDAOTest {

    static final byte[] KEY = SecUtil.randomBytes(32);
    static final byte[] OTHER_KEY = SecUtil.randomBytes(32);
    static final byte[] DATA = SharedStringUtil.getBytes("The quick brown fox jumps over the lazy dog.");

    // field positions in the canonical form
    static final int V = 0, ALG = 1, KDF = 2, IV = 3, LEN = 4, MASK = 5, EXP = 6, HINT = 7, CT = 8;

    private static EncryptedData sealed(byte[] data) throws Exception {
        EncryptedData ed = new EncryptedData();
        ed.setHint("unit test");
        ed.setExpiry(1_900_000_000_000L);
        ed.setMask("****1234");
        return CryptoUtil.encryptData(ed, KEY, data);
    }

    private static String[] fields(String canonical) {
        return canonical.split("\\|", -1);
    }

    private static String join(String[] fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append('|');
            sb.append(fields[i]);
        }
        return sb.toString();
    }

    /* --------------------------------------------------------- EncryptedData */

    @Test
    public void roundTrip() throws Exception {
        EncryptedData ed = sealed(DATA);
        assertEquals(EncryptedData.VERSION, ed.getVersion());
        assertEquals(CryptoConst.ALG_A256GCM, ed.getAlgorithm());
        assertEquals(CryptoConst.KDF_HKDF_SHA256, ed.getKDF());
        assertEquals(EncryptedData.IV_SIZE, ed.getIV().length);
        assertEquals(DATA.length, ed.getDataLength());
        assertEquals(DATA.length + EncryptedData.TAG_SIZE, ed.getEncryptedData().length);
        assertArrayEquals(DATA, CryptoUtil.decryptEncryptedData(ed, KEY));
    }

    @Test
    public void emptyAndLargeData() throws Exception {
        EncryptedData empty = sealed(new byte[0]);
        assertEquals(0, empty.getDataLength());
        assertEquals(EncryptedData.TAG_SIZE, empty.getEncryptedData().length);
        assertArrayEquals(new byte[0], CryptoUtil.decryptEncryptedData(empty, KEY));

        byte[] big = new byte[1 << 20];
        for (int i = 0; i < big.length; i++) big[i] = (byte) (i * 7);
        assertArrayEquals(big, CryptoUtil.decryptEncryptedData(sealed(big), KEY));
    }

    @Test
    public void freshNonceAndKeyPerRecord() throws Exception {
        EncryptedData a = sealed(DATA);
        EncryptedData b = sealed(DATA);
        assertFalse(Arrays.equals(a.getIV(), b.getIV()));
        assertFalse(Arrays.equals(a.getEncryptedData(), b.getEncryptedData()));
    }

    @Test
    public void wrongAndShortKey() throws Exception {
        EncryptedData ed = sealed(DATA);
        assertThrows(SignatureException.class, () -> CryptoUtil.decryptEncryptedData(ed, OTHER_KEY));
        byte[] shortKey = new byte[16];
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.encryptData(new EncryptedData(), shortKey, DATA));
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.decryptEncryptedData(ed, shortKey));
    }

    @Test
    public void canonicalFormRoundTrip() throws Exception {
        EncryptedData ed = sealed(DATA);
        String canonical = ed.toCanonicalID();
        String[] f = fields(canonical);
        assertEquals(9, f.length);
        assertEquals("1", f[V]);
        assertEquals("A256GCM", f[ALG]);
        assertEquals("HKDF-SHA256", f[KDF]);
        assertEquals(String.valueOf(DATA.length), f[LEN]);
        assertEquals("****1234", f[MASK]);
        assertEquals("1900000000000", f[EXP]);
        assertEquals("unit test", f[HINT]);
        // associated data is the canonical form without the trailing ciphertext
        assertEquals(canonical.substring(0, canonical.lastIndexOf('|')), ed.toAssociatedData());

        EncryptedData parsed = EncryptedData.fromCanonicalID(canonical);
        assertEquals(canonical, parsed.toCanonicalID());
        assertEquals(ed.toAssociatedData(), parsed.toAssociatedData());
        assertArrayEquals(DATA, CryptoUtil.decryptEncryptedData(parsed, KEY));

        // optional attributes absent: empty fields, still round-trips
        EncryptedData bare = CryptoUtil.encryptData(new EncryptedData(), KEY, DATA);
        String[] b = fields(bare.toCanonicalID());
        assertEquals("", b[MASK]);
        assertEquals("", b[EXP]);
        assertEquals("", b[HINT]);
        assertArrayEquals(DATA, CryptoUtil.decryptEncryptedData(EncryptedData.fromCanonicalID(bare.toCanonicalID()), KEY));

        // malformed input
        assertThrows(IllegalArgumentException.class, () -> EncryptedData.fromCanonicalID(""));
        assertThrows(IllegalArgumentException.class, () -> EncryptedData.fromCanonicalID(canonical.substring(1)), "no version");
        assertThrows(IllegalArgumentException.class, () -> EncryptedData.fromCanonicalID(canonical + "|extra"));
        assertThrows(IllegalArgumentException.class, () -> EncryptedData.fromCanonicalID(canonical.substring(0, canonical.lastIndexOf('|'))));
        assertThrows(IllegalArgumentException.class, () -> EncryptedData.fromCanonicalID("x|A256GCM|HKDF-SHA256||0||||"));
        // the separator is refused in text attributes
        assertThrows(IllegalArgumentException.class, () -> new EncryptedData().setHint("a|b"));
        assertThrows(IllegalArgumentException.class, () -> new EncryptedData().setMask("a|b"));
    }

    @Test
    public void entityJsonRoundTrip() throws Exception {
        // the record still travels as an NVEntity through GSONUtil
        EncryptedData ed = sealed(DATA);
        ed.setGUID(UUID.randomUUID().toString());
        String json = GSONUtil.toJSON(ed, false, false, false);
        EncryptedData back = GSONUtil.fromJSON(json, EncryptedData.class);
        assertEquals(ed.getGUID(), back.getGUID());
        assertEquals(ed.toCanonicalID(), back.toCanonicalID());
        assertArrayEquals(DATA, CryptoUtil.decryptEncryptedData(back, KEY));
    }

    @Test
    public void everyAuthenticatedAttributeIsBound() throws Exception {
        EncryptedData ed = sealed(DATA);
        String[] base = fields(ed.toCanonicalID());

        // change each attribute in turn, rebuild the record, expect refusal
        String[][] changes = {
                {String.valueOf(HINT), "other hint"},
                {String.valueOf(MASK), "****9999"},
                {String.valueOf(EXP), "1900000000001"},
                {String.valueOf(LEN), String.valueOf(DATA.length - 1)},
                // dropping an optional attribute also fails
                {String.valueOf(HINT), ""},
                {String.valueOf(MASK), ""},
                {String.valueOf(EXP), ""},
        };
        for (String[] change : changes) {
            String[] f = base.clone();
            f[Integer.parseInt(change[0])] = change[1];
            EncryptedData tampered = EncryptedData.fromCanonicalID(join(f));
            assertThrows(SignatureException.class, () -> CryptoUtil.decryptEncryptedData(tampered, KEY), "field " + change[0] + " -> " + change[1]);
        }
        // iv, ciphertext and tag bit flips
        String canonical = ed.toCanonicalID();
        EncryptedData ivFlip = EncryptedData.fromCanonicalID(canonical);
        ivFlip.getIV()[3] ^= 1;
        assertThrows(SignatureException.class, () -> CryptoUtil.decryptEncryptedData(ivFlip, KEY));
        EncryptedData ctFlip = EncryptedData.fromCanonicalID(canonical);
        ctFlip.getEncryptedData()[5] ^= 1;
        assertThrows(SignatureException.class, () -> CryptoUtil.decryptEncryptedData(ctFlip, KEY));
        EncryptedData tagFlip = EncryptedData.fromCanonicalID(canonical);
        tagFlip.getEncryptedData()[tagFlip.getEncryptedData().length - 1] ^= 1;
        assertThrows(SignatureException.class, () -> CryptoUtil.decryptEncryptedData(tagFlip, KEY));
        // version, cipher and kdf are checked before any cryptography
        EncryptedData v2 = EncryptedData.fromCanonicalID(canonical);
        v2.setVersion(2);
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.decryptEncryptedData(v2, KEY));
        EncryptedData alg = EncryptedData.fromCanonicalID(canonical);
        alg.setAlgorithm("A128GCM");
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.decryptEncryptedData(alg, KEY));
    }

    /* ------------------------------------------------------- EncapsulatedKey */

    private static EncapsulatedKey keyRow() throws Exception {
        EncapsulatedKey ek = new EncapsulatedKey();
        ek.setSubjectGUID(UUID.randomUUID().toString());
        ek.setReferenceGUID(UUID.randomUUID().toString());
        ek.setReferenceType("org.zoxweb.shared.data.FileInfoDAO");
        ek.setKeyLockType(KeyLockType.NVENTITY);
        ek.setKeyGUID(UUID.randomUUID().toString());
        return CryptoUtil.createEncryptedKey(ek, KEY);
    }

    /** Copies the sealed record attributes of one key into another, leaving the binding fields alone. */
    private static void copyRecord(EncapsulatedKey from, EncapsulatedKey to) {
        to.setVersion(from.getVersion());
        to.setAlgorithm(from.getAlgorithm());
        to.setKDF(from.getKDF());
        to.setIV(from.getIV());
        to.setDataLength(from.getDataLength());
        to.setMask(from.getMask());
        to.setExpiry(from.getExpiry());
        to.setHint(from.getHint());
        to.setEncryptedData(from.getEncryptedData());
    }

    @Test
    public void wrappedKeyRoundTrip() throws Exception {
        EncapsulatedKey ek = keyRow();
        // the key row is itself the record
        assertEquals(EncryptedData.VERSION, ek.getVersion());
        assertEquals(CryptoConst.ALG_A256GCM, ek.getAlgorithm());
        assertEquals(32, ek.getDataLength());
        assertEquals(32 + EncryptedData.TAG_SIZE, ek.getEncryptedData().length);
        assertEquals(9, fields(ek.toCanonicalID()).length, "inherited canonical form");
        assertEquals(32, ek.getKeySize(), "key_size is the outer key size in bytes");
        assertEquals(ek.getSubjectGUID() + "|" + ek.getReferenceGUID() + "|" + ek.getKeyGUID() + "|32", ek.toBindingData());
        assertFalse(ek.isKEMWrapped());
        assertNull(ek.getKEMCiphertext());
        assertNull(ek.getGUID(), "the entity GUID plays no part");
        // the datastore may assign or change the entity GUID at any time without effect
        ek.setGUID(UUID.randomUUID().toString());
        assertArrayEquals(CryptoUtil.unwrapKey(ek, KEY), CryptoUtil.unwrapKey(ek, KEY));
        ek.setGUID(UUID.randomUUID().toString());
        byte[] material = CryptoUtil.unwrapKey(ek, KEY);
        assertEquals(32, material.length);
        assertArrayEquals(material, CryptoUtil.unwrapKey(ek, KEY), "stable across calls");
        assertThrows(SignatureException.class, () -> CryptoUtil.unwrapKey(ek, OTHER_KEY));
        // handing the row to the plain record decrypt, without the binding, fails closed
        assertThrows(SignatureException.class, () -> CryptoUtil.decryptEncryptedData(ek, KEY));

        // the wrapped material can itself seal a record: the two-level chain
        EncryptedData ed = new EncryptedData();
        CryptoUtil.encryptData(ed, material, DATA);
        assertArrayEquals(DATA, CryptoUtil.decryptEncryptedData(ed, CryptoUtil.unwrapKey(ek, KEY)));
    }

    @Test
    public void bareWrappedKey() throws Exception {
        EncapsulatedKey ek = CryptoUtil.createEncryptedKey(KEY);
        assertNull(ek.getGUID());
        assertNull(ek.getKeyGUID(), "under the master key there is no parent key");
        assertEquals("|||32", ek.toBindingData());
        assertEquals(32, CryptoUtil.unwrapKey(ek, KEY).length);
        // binding fields set after wrapping break it, as they must
        ek.setReferenceGUID(UUID.randomUUID().toString());
        assertThrows(SignatureException.class, () -> CryptoUtil.unwrapKey(ek, KEY));
    }

    @Test
    public void bindingFieldsAreAuthenticatedLabelsAreNot() throws Exception {
        EncapsulatedKey ek = keyRow();
        String json = GSONUtil.toJSON(ek, false, false, false);
        byte[] material = CryptoUtil.unwrapKey(ek, KEY);

        // identity: subject, reference and wrapping key are bound
        EncapsulatedKey subject = GSONUtil.fromJSON(json, EncapsulatedKey.class);
        subject.setSubjectGUID(UUID.randomUUID().toString());
        assertThrows(SignatureException.class, () -> CryptoUtil.unwrapKey(subject, KEY));

        EncapsulatedKey reference = GSONUtil.fromJSON(json, EncapsulatedKey.class);
        reference.setReferenceGUID(UUID.randomUUID().toString());
        assertThrows(SignatureException.class, () -> CryptoUtil.unwrapKey(reference, KEY));

        EncapsulatedKey keyGuid = GSONUtil.fromJSON(json, EncapsulatedKey.class);
        keyGuid.setKeyGUID(UUID.randomUUID().toString());
        assertThrows(SignatureException.class, () -> CryptoUtil.unwrapKey(keyGuid, KEY));

        // key size is bound
        EncapsulatedKey size = GSONUtil.fromJSON(json, EncapsulatedKey.class);
        size.setKeySize(16);
        assertThrows(SignatureException.class, () -> CryptoUtil.unwrapKey(size, KEY));

        // labels: a renamed class or a corrected lock type must not invalidate stored keys
        EncapsulatedKey type = GSONUtil.fromJSON(json, EncapsulatedKey.class);
        type.setReferenceType("org.zoxweb.shared.data.RenamedFileInfoDAO");
        assertArrayEquals(material, CryptoUtil.unwrapKey(type, KEY));

        EncapsulatedKey lock = GSONUtil.fromJSON(json, EncapsulatedKey.class);
        lock.setKeyLockType(KeyLockType.SUBJECT_ID);
        assertArrayEquals(material, CryptoUtil.unwrapKey(lock, KEY));

        // the sealed record copied into a key row with another identity
        EncapsulatedKey other = keyRow();
        copyRecord(ek, other);
        assertThrows(SignatureException.class, () -> CryptoUtil.unwrapKey(other, KEY));

        // untouched copy still opens
        EncapsulatedKey same = GSONUtil.fromJSON(json, EncapsulatedKey.class);
        assertArrayEquals(material, CryptoUtil.unwrapKey(same, KEY));
    }

    @Test
    public void rekeyKeepsMaterial() throws Exception {
        EncapsulatedKey ek = keyRow();
        byte[] before = CryptoUtil.unwrapKey(ek, KEY);
        CryptoUtil.rekeyEncryptedKey(ek, KEY, OTHER_KEY);
        assertArrayEquals(before, CryptoUtil.unwrapKey(ek, OTHER_KEY));
        assertThrows(SignatureException.class, () -> CryptoUtil.unwrapKey(ek, KEY));
        assertThrows(SignatureException.class, () -> CryptoUtil.rekeyEncryptedKey(ek, KEY, OTHER_KEY));
    }

    @Test
    public void wrapRequiresFullKey() throws Exception {
        EncapsulatedKey ek = new EncapsulatedKey();
        assertThrows(IllegalArgumentException.class, () -> CryptoUtil.wrapKey(ek, KEY, new byte[16]));
        assertThrows(SignatureException.class, () -> CryptoUtil.unwrapKey(ek, KEY), "nothing wrapped yet");
    }

    @Test
    public void hkdfKnownAnswer() throws Exception {
        // RFC 5869 test case 1
        byte[] ikm = new byte[22];
        Arrays.fill(ikm, (byte) 0x0b);
        byte[] salt = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c};
        byte[] info = {(byte) 0xf0, (byte) 0xf1, (byte) 0xf2, (byte) 0xf3, (byte) 0xf4, (byte) 0xf5, (byte) 0xf6, (byte) 0xf7, (byte) 0xf8, (byte) 0xf9};
        byte[] okm = CryptoUtil.hkdfSHA256(ikm, salt, info, 42);
        assertEquals("3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865",
                SharedStringUtil.bytesToHex(okm).toLowerCase());
    }
}
