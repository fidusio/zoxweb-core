package org.zoxweb.server.security;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.io.IOUtil;

import javax.crypto.AEADBadTagException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.zoxweb.server.security.AESCryptShortReadTest.plain;

/**
 * VX container: layout, sizes, tamper detection, the length parameter, and the legacy v1/v2 read path
 * pinned by fixtures written by the pre-VX writer.
 */
public class AESCryptTest {

    private static final byte[] KEY = new byte[32];
    private static final int SEGMENT = 100;
    /** magic 4 + version 1 + cipher 1 + kdf 1 + kdf params 4 */
    private static final int SALT_OFFSET = 11;

    static {
        Arrays.fill(KEY, (byte) 7);
    }

    static byte[] resource(String name) throws IOException {
        InputStream is = AESCryptTest.class.getClassLoader().getResourceAsStream(name);
        assertNotNull(is, "missing test resource " + name);
        return IOUtil.inputStreamToByteArray(is, true).toByteArray();
    }

    private static byte[] encrypt(byte[] plain, int segment) throws Exception {
        AESCrypt c = new AESCrypt(KEY);
        c.setSegmentSize(segment);
        ByteArrayOutputStream ct = new ByteArrayOutputStream();
        c.encrypt(new ByteArrayInputStream(plain), ct);
        return ct.toByteArray();
    }

    private static byte[] decrypt(byte[] ct, long length) throws Exception {
        ByteArrayOutputStream pt = new ByteArrayOutputStream();
        new AESCrypt(KEY).decrypt(length, new ByteArrayInputStream(ct), pt);
        return pt.toByteArray();
    }

    private static void assertAltered(byte[] ct, long length) throws Exception {
        assertThrows(AEADBadTagException.class, () -> decrypt(ct, length), "length=" + length);
        assertThrows(AEADBadTagException.class, () -> new AESCrypt(KEY).verify(length, new ByteArrayInputStream(ct)));
    }

    /* ------------------------------------------------------------- legacy */

    @Test
    public void legacyFixturesDecrypt() throws Exception {
        for (int size : new int[]{0, 1000, 2048}) {
            byte[] ct = resource("aescrypt/v2-key-" + size + ".aes");
            assertEquals(103 + ((size + 15) / 16) * 16 + 33, ct.length, "v2 container size for " + size);
            assertArrayEquals(plain(size), AESCrypt.decryptBuffer(KEY, ct).toByteArray(), "size=" + size);
            new AESCrypt(KEY).verify(ct.length, new ByteArrayInputStream(ct));
        }
    }

    @Test
    public void legacyPasswordFixtureDecrypts() throws Exception {
        byte[] ct = resource("aescrypt/v2-password-1000.aes");
        ByteArrayOutputStream pt = new ByteArrayOutputStream();
        new AESCrypt("correct horse").decrypt(ct.length, new ByteArrayInputStream(ct), pt);
        assertArrayEquals(plain(1000), pt.toByteArray());
    }

    @Test
    public void legacyRequiresLength() throws Exception {
        byte[] ct = resource("aescrypt/v2-key-1000.aes");
        assertThrows(IllegalArgumentException.class, () -> decrypt(ct, -1));
        assertThrows(IllegalArgumentException.class, () -> decrypt(ct, 0));
    }

    @Test
    public void legacyTamperAndWrongPassword() throws Exception {
        byte[] ct = resource("aescrypt/v2-key-1000.aes");
        byte[] t = ct.clone();
        t[200] ^= 1;  // inside the payload
        assertThrows(IOException.class, () -> decrypt(t, t.length));
        byte[] wrong = new byte[32];
        assertThrows(IOException.class, () -> AESCrypt.decryptBuffer(wrong, ct));
    }

    /* ----------------------------------------------------------------- VX */

    @Test
    public void vxHeaderAndSize() throws Exception {
        byte[] ct = encrypt(plain(1000), AESCrypt.VX_DEFAULT_SEGMENT_SIZE);
        assertEquals(AESCrypt.VX_HEADER_SIZE + 1000 + AESCrypt.VX_TAG_SIZE, ct.length);
        assertEquals(1054, ct.length);
        assertEquals(1078, encrypt(plain(1024), AESCrypt.VX_DEFAULT_SEGMENT_SIZE).length);
        assertEquals(AESCrypt.VX_HEADER_SIZE + AESCrypt.VX_TAG_SIZE, encrypt(plain(0), AESCrypt.VX_DEFAULT_SEGMENT_SIZE).length);

        ByteBuffer bb = ByteBuffer.wrap(ct);
        byte[] magic = new byte[4];
        bb.get(magic);
        assertArrayEquals(AESCrypt.VX_MAGIC, magic);
        assertEquals(AESCrypt.VX_VERSION, bb.get());
        assertEquals(AESCrypt.VX_CIPHER_AES_256_GCM, bb.get());
        assertEquals(AESCrypt.VX_KDF_HKDF_SHA256, bb.get());
        assertEquals(0, bb.getInt());
        bb.position(bb.position() + AESCrypt.VX_SALT_SIZE + AESCrypt.VX_NONCE_PREFIX_SIZE);
        assertEquals(AESCrypt.VX_DEFAULT_SEGMENT_SIZE, bb.getInt());
        assertEquals(AESCrypt.VX_HEADER_SIZE, bb.position());

        // multi-segment: 250 bytes in 100-byte segments = 3 segments
        byte[] multi = encrypt(plain(250), SEGMENT);
        assertEquals(AESCrypt.VX_HEADER_SIZE + 250 + 3 * AESCrypt.VX_TAG_SIZE, multi.length);
        // exact multiple: 200 bytes = 2 segments, no empty trailer
        assertEquals(AESCrypt.VX_HEADER_SIZE + 200 + 2 * AESCrypt.VX_TAG_SIZE, encrypt(plain(200), SEGMENT).length);
    }

    @Test
    public void vxLastHeaderRoundTrips() throws Exception {
        AESCrypt c = new AESCrypt(KEY);
        ByteArrayOutputStream ct = new ByteArrayOutputStream();
        c.encrypt(new ByteArrayInputStream(plain(10)), ct);
        byte[] written = c.getLastHeader();
        assertArrayEquals(Arrays.copyOf(ct.toByteArray(), AESCrypt.VX_HEADER_SIZE), written);
        AESCrypt d = new AESCrypt(KEY);
        assertNull(d.getLastHeader());
        d.verify(-1, new ByteArrayInputStream(ct.toByteArray()));
        assertArrayEquals(written, d.getLastHeader());
    }

    @Test
    public void vxFreshRandomnessPerFile() throws Exception {
        byte[] a = encrypt(plain(100), SEGMENT);
        byte[] b = encrypt(plain(100), SEGMENT);
        assertFalse(Arrays.equals(Arrays.copyOfRange(a, SALT_OFFSET, AESCrypt.VX_HEADER_SIZE - 4), Arrays.copyOfRange(b, SALT_OFFSET, AESCrypt.VX_HEADER_SIZE - 4)), "salt and prefix");
        assertFalse(Arrays.equals(Arrays.copyOfRange(a, AESCrypt.VX_HEADER_SIZE, a.length), Arrays.copyOfRange(b, AESCrypt.VX_HEADER_SIZE, b.length)), "ciphertext");
    }

    @Test
    public void vxWrongKeyAndShortKey() throws Exception {
        byte[] ct = encrypt(plain(250), SEGMENT);
        byte[] wrong = new byte[32];
        assertThrows(AEADBadTagException.class, () -> AESCrypt.decryptBuffer(wrong, ct));
        byte[] shortKey = new byte[8];
        assertThrows(IllegalStateException.class, () -> AESCrypt.encryptBuffer(shortKey, plain(10)));
    }

    @Test
    public void vxTypedPasswordRoundTrip() throws Exception {
        AESCrypt c = new AESCrypt("correct horse");
        c.setPbkdf2Iterations(AESCrypt.VX_MIN_PBKDF2_ITERATIONS);
        ByteArrayOutputStream ct = new ByteArrayOutputStream();
        c.encrypt(new ByteArrayInputStream(plain(300)), ct);
        byte[] container = ct.toByteArray();

        // header says PBKDF2 with the writer's iteration count
        ByteBuffer bb = ByteBuffer.wrap(container, 6, 5);
        assertEquals(AESCrypt.VX_KDF_PBKDF2_HMAC_SHA256, bb.get());
        assertEquals(AESCrypt.VX_MIN_PBKDF2_ITERATIONS, bb.getInt());

        // the reader takes the count from the header, not from its own setting
        ByteArrayOutputStream pt = new ByteArrayOutputStream();
        new AESCrypt("correct horse").decrypt(-1, new ByteArrayInputStream(container), pt);
        assertArrayEquals(plain(300), pt.toByteArray());
        assertThrows(AEADBadTagException.class, () ->
                new AESCrypt("correct horsf").decrypt(-1, new ByteArrayInputStream(container), new ByteArrayOutputStream()));

        // a raw key never selects PBKDF2
        byte[] rawKeyContainer = encrypt(plain(10), SEGMENT);
        assertEquals(AESCrypt.VX_KDF_HKDF_SHA256, rawKeyContainer[6]);

        // tampering with the iteration count changes the derived key, or is refused outright
        byte[] fewer = container.clone();
        fewer[10] ^= 1;
        assertThrows(AEADBadTagException.class, () ->
                new AESCrypt("correct horse").decrypt(-1, new ByteArrayInputStream(fewer), new ByteArrayOutputStream()));
        byte[] huge = container.clone();
        ByteBuffer.wrap(huge, 7, 4).putInt(AESCrypt.VX_MAX_PBKDF2_ITERATIONS + 1);
        assertThrows(IOException.class, () ->
                new AESCrypt("correct horse").decrypt(-1, new ByteArrayInputStream(huge), new ByteArrayOutputStream()));
        byte[] zero = container.clone();
        ByteBuffer.wrap(zero, 7, 4).putInt(0);
        assertThrows(IOException.class, () ->
                new AESCrypt("correct horse").decrypt(-1, new ByteArrayInputStream(zero), new ByteArrayOutputStream()));
    }

    @Test
    public void vxTypedPasswordDefaultIterations() throws Exception {
        AESCrypt c = new AESCrypt("correct horse");
        assertEquals(AESCrypt.VX_DEFAULT_PBKDF2_ITERATIONS, c.getPbkdf2Iterations());
        ByteArrayOutputStream ct = new ByteArrayOutputStream();
        long t = System.nanoTime();
        c.encrypt(new ByteArrayInputStream(plain(100)), ct);
        long encryptMillis = (System.nanoTime() - t) / 1_000_000;
        assertEquals(AESCrypt.VX_DEFAULT_PBKDF2_ITERATIONS, ByteBuffer.wrap(ct.toByteArray(), 7, 4).getInt());
        ByteArrayOutputStream pt = new ByteArrayOutputStream();
        new AESCrypt("correct horse").decrypt(-1, new ByteArrayInputStream(ct.toByteArray()), pt);
        assertArrayEquals(plain(100), pt.toByteArray());
        System.out.println("PBKDF2 " + AESCrypt.VX_DEFAULT_PBKDF2_ITERATIONS + " iterations: " + encryptMillis + " ms");
    }

    @Test
    public void pbkdf2IterationValidation() throws Exception {
        AESCrypt c = new AESCrypt("correct horse");
        assertThrows(IllegalArgumentException.class, () -> c.setPbkdf2Iterations(AESCrypt.VX_MIN_PBKDF2_ITERATIONS - 1));
        assertThrows(IllegalArgumentException.class, () -> c.setPbkdf2Iterations(AESCrypt.VX_MAX_PBKDF2_ITERATIONS + 1));
        assertThrows(IllegalArgumentException.class, () -> new AESCrypt(""));
    }

    /**
     * The in-class PBKDF2 must agree with the JDK's PBKDF2WithHmacSHA256. The JDK encodes the
     * char[] password as UTF-8, so the comparison uses an ASCII password and feeds the same bytes to
     * a byte-array-keyed instance, whose HKDF path is bypassed by reading the derived key back out of
     * a known container: two writers, one raw and one JDK-derived, must agree on the content key.
     */
    @Test
    public void pbkdf2MatchesJdk() throws Exception {
        String pw = "password";
        byte[] salt = "salt-salt-salt-1".getBytes("US-ASCII");  // 16 bytes
        int iterations = 4096;
        javax.crypto.SecretKeyFactory f = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] expected = f.generateSecret(new javax.crypto.spec.PBEKeySpec(pw.toCharArray(), salt, iterations, 256)).getEncoded();
        byte[] actual = pbkdf2ViaReflection(pw.getBytes("US-ASCII"), salt, iterations);
        assertArrayEquals(expected, actual);

        // RFC-style known answer, c = 1: PBKDF2-HMAC-SHA256("password", "salt", 1, 32)
        byte[] kat = pbkdf2ViaReflection("password".getBytes("US-ASCII"), "salt".getBytes("US-ASCII"), 1);
        assertEquals("120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b", hex(kat));
    }

    private static byte[] pbkdf2ViaReflection(byte[] password, byte[] salt, int iterations) throws Exception {
        AESCrypt c = new AESCrypt(new byte[32]);
        java.lang.reflect.Method m = AESCrypt.class.getDeclaredMethod("pbkdf2", byte[].class, byte[].class, int.class, int.class);
        m.setAccessible(true);
        return (byte[]) m.invoke(c, password, salt, iterations, 32);
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    @Test
    public void vxHeaderTamper() throws Exception {
        byte[] ct = encrypt(plain(250), SEGMENT);
        for (int i = SALT_OFFSET; i < AESCrypt.VX_HEADER_SIZE - 4; i++) {  // salt and prefix bytes
            byte[] t = ct.clone();
            t[i] ^= 0x40;
            assertAltered(t, t.length);
            assertAltered(t, -1);
        }
        byte[] kdf = ct.clone();
        kdf[7] = 1;  // reserved kdf params: rejected before any cryptography
        assertThrows(IOException.class, () -> decrypt(kdf, kdf.length));
        byte[] seg = ct.clone();
        seg[AESCrypt.VX_HEADER_SIZE - 1] = 50;  // segment size 100 -> 50; segments no longer line up
        assertAltered(seg, seg.length);
        byte[] ver = ct.clone();
        ver[4] = 2;
        assertThrows(IOException.class, () -> decrypt(ver, ver.length));
        byte[] magic = ct.clone();
        magic[0] = 'X';
        assertThrows(IOException.class, () -> decrypt(magic, magic.length));
    }

    @Test
    public void vxSegmentTamper() throws Exception {
        byte[] ct = encrypt(plain(250), SEGMENT);
        int seg = SEGMENT + AESCrypt.VX_TAG_SIZE;
        int[] offsets = {
                AESCrypt.VX_HEADER_SIZE,                              // first byte of segment 0
                AESCrypt.VX_HEADER_SIZE + seg - 1,                    // tag of segment 0
                AESCrypt.VX_HEADER_SIZE + seg + 10,                   // segment 1
                AESCrypt.VX_HEADER_SIZE + 2 * seg + 3,                // segment 2 (last, short)
                ct.length - 1,                                        // last tag byte
        };
        for (int off : offsets) {
            byte[] t = ct.clone();
            t[off] ^= 1;
            assertAltered(t, t.length);
            assertAltered(t, -1);
        }
    }

    @Test
    public void vxTruncationAndReorder() throws Exception {
        byte[] ct = encrypt(plain(250), SEGMENT);
        int seg = SEGMENT + AESCrypt.VX_TAG_SIZE;

        // drop the whole last segment: segment 1 is not flagged last
        byte[] drop = Arrays.copyOf(ct, AESCrypt.VX_HEADER_SIZE + 2 * seg);
        assertAltered(drop, drop.length);
        assertAltered(drop, -1);

        // cut the last segment short
        byte[] cut = Arrays.copyOf(ct, ct.length - 5);
        assertAltered(cut, cut.length);
        assertAltered(cut, -1);

        // cut inside a middle segment, stream mode and length mode
        byte[] mid = Arrays.copyOf(ct, AESCrypt.VX_HEADER_SIZE + seg + 20);
        assertAltered(mid, mid.length);
        assertAltered(mid, -1);

        // swap segments 0 and 1
        byte[] swap = ct.clone();
        System.arraycopy(ct, AESCrypt.VX_HEADER_SIZE + seg, swap, AESCrypt.VX_HEADER_SIZE, seg);
        System.arraycopy(ct, AESCrypt.VX_HEADER_SIZE, swap, AESCrypt.VX_HEADER_SIZE + seg, seg);
        assertAltered(swap, swap.length);
        assertAltered(swap, -1);

        // header only: shorter than a container
        byte[] headerOnly = Arrays.copyOf(ct, AESCrypt.VX_HEADER_SIZE);
        assertThrows(IOException.class, () -> decrypt(headerOnly, headerOnly.length));
        assertThrows(IOException.class, () -> decrypt(headerOnly, -1));
    }

    /** With a positive length the reader consumes exactly the container and leaves the rest of the stream. */
    @Test
    public void vxLengthDrivenReadFromLongerStream() throws Exception {
        byte[] ct = encrypt(plain(250), SEGMENT);
        byte[] trailer = "TRAILER".getBytes("UTF-8");
        byte[] stream = new byte[ct.length + trailer.length];
        System.arraycopy(ct, 0, stream, 0, ct.length);
        System.arraycopy(trailer, 0, stream, ct.length, trailer.length);

        ByteArrayInputStream in = new ByteArrayInputStream(stream);
        ByteArrayOutputStream pt = new ByteArrayOutputStream();
        new AESCrypt(KEY).decrypt(ct.length, in, pt, false, true);
        assertArrayEquals(plain(250), pt.toByteArray());
        byte[] rest = new byte[in.available()];
        assertEquals(trailer.length, in.read(rest));
        assertArrayEquals(trailer, rest);

        // stream-driven on the same bytes must refuse: the trailer breaks the last-segment rule
        assertAltered(stream, -1);
        // and a wrong length is refused too
        assertAltered(stream, ct.length + 3);
        assertAltered(ct, ct.length - 1);
    }

    @Test
    public void vxVerifyConsumesExactlyTheContainer() throws Exception {
        byte[] ct = encrypt(plain(250), SEGMENT);
        ByteArrayInputStream in = new ByteArrayInputStream(ct);
        new AESCrypt(KEY).verify(ct.length, in, false);
        assertEquals(0, in.available(), "verify consumed the container");
    }

    @Test
    public void vxFileRoundTrip() throws Exception {
        File dir = Files.createTempDirectory("aescrypt").toFile();
        File plainFile = new File(dir, "plain.bin");
        File encFile = new File(dir, "plain.bin.aes");
        File outFile = new File(dir, "plain.out");
        try {
            byte[] plain = plain(200_000);  // several default segments
            Files.write(plainFile.toPath(), plain);
            AESCrypt c = new AESCrypt(KEY);
            c.encrypt(plainFile, encFile);
            assertEquals(AESCrypt.VX_HEADER_SIZE + plain.length + 4 * AESCrypt.VX_TAG_SIZE, encFile.length());
            new AESCrypt(KEY).decrypt(encFile, outFile);
            assertArrayEquals(plain, Files.readAllBytes(outFile.toPath()));
        } finally {
            plainFile.delete();
            encFile.delete();
            outFile.delete();
            dir.delete();
        }
    }

    @Test
    public void staticHelpers() throws Exception {
        String msg = "hello VX";
        byte[] ct = AESCrypt.encryptMessage(KEY, msg).toByteArray();
        assertEquals(msg, new String(AESCrypt.decryptBuffer(KEY, ct).toByteArray(), "UTF-8"));
        byte[] buf = plain(3000);
        assertArrayEquals(buf, AESCrypt.decryptBuffer(KEY, AESCrypt.encryptBuffer(KEY, buf).toByteArray()).toByteArray());
    }

    @Test
    public void segmentSizeValidation() throws Exception {
        AESCrypt c = new AESCrypt(KEY);
        assertThrows(IllegalArgumentException.class, () -> c.setSegmentSize(0));
        assertThrows(IllegalArgumentException.class, () -> c.setSegmentSize(AESCrypt.VX_MAX_SEGMENT_SIZE + 1));
        c.setSegmentSize(1);
        ByteArrayOutputStream ct = new ByteArrayOutputStream();
        c.encrypt(new ByteArrayInputStream(plain(5)), ct);
        assertEquals(AESCrypt.VX_HEADER_SIZE + 5 + 5 * AESCrypt.VX_TAG_SIZE, ct.size());
        assertArrayEquals(plain(5), decrypt(ct.toByteArray(), -1));
    }
}
