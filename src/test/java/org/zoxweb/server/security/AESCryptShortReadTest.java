package org.zoxweb.server.security;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * AESCrypt must not assume that InputStream.read fills the buffer it is handed. These tests feed it
 * streams that return a bounded number of bytes per read, the way a socket or a chunked HTTP body does,
 * and check every combination of plaintext size, segment size and read size round-trips exactly.
 */
public class AESCryptShortReadTest {

    /** Never returns more than {@code max} bytes per read; {@code skip} also advances at most {@code max}. */
    static class ChunkyInputStream extends InputStream {
        private final byte[] data;
        private final int max;
        private int pos = 0;

        ChunkyInputStream(byte[] data, int max) { this.data = data; this.max = max; }

        @Override
        public int read() { return pos < data.length ? (data[pos++] & 0xff) : -1; }

        @Override
        public int read(byte[] b, int off, int len) {
            if (pos >= data.length) return -1;
            int n = Math.min(Math.min(len, max), data.length - pos);
            System.arraycopy(data, pos, b, off, n);
            pos += n;
            return n;
        }

        @Override
        public long skip(long n) {
            long s = Math.min(Math.min(n, max), data.length - pos);
            pos += s;
            return s;
        }
    }

    private static final byte[] KEY = new byte[32];
    // small segment so multi-segment paths are exercised with little data
    private static final int SEGMENT = 100;
    // sizes around the segment boundary
    private static final int[] SIZES = {0, 1, 99, 100, 101, 199, 200, 201, 1000, 2048, 5000};
    private static final int[] CHUNKS = {1, 7, 16, 17, 100, 116, 117, 1000, 3000};

    static {
        Arrays.fill(KEY, (byte) 7);
    }

    static byte[] plain(int size) {
        byte[] p = new byte[size];
        for (int i = 0; i < size; i++) p[i] = (byte) (i * 31 + 7);
        return p;
    }

    static byte[] encrypt(InputStream in) throws Exception {
        AESCrypt c = new AESCrypt(KEY);
        c.setSegmentSize(SEGMENT);
        ByteArrayOutputStream ct = new ByteArrayOutputStream();
        c.encrypt(in, ct);
        return ct.toByteArray();
    }

    static byte[] decrypt(InputStream in, long length) throws Exception {
        ByteArrayOutputStream pt = new ByteArrayOutputStream();
        new AESCrypt(KEY).decrypt(length, in, pt);
        return pt.toByteArray();
    }

    @Test
    public void arrayRoundTrip() throws Exception {
        for (int size : SIZES) {
            byte[] plain = plain(size);
            byte[] ct = encrypt(new ByteArrayInputStream(plain));
            assertArrayEquals(plain, decrypt(new ByteArrayInputStream(ct), ct.length), "size=" + size);
            assertArrayEquals(plain, decrypt(new ByteArrayInputStream(ct), -1), "size=" + size + " no length");
        }
    }

    @Test
    public void encryptFromShortReadingStream() throws Exception {
        for (int size : SIZES) {
            byte[] plain = plain(size);
            for (int chunk : CHUNKS) {
                byte[] ct = encrypt(new ChunkyInputStream(plain, chunk));
                assertArrayEquals(plain, decrypt(new ByteArrayInputStream(ct), ct.length), "size=" + size + " chunk=" + chunk);
            }
        }
    }

    @Test
    public void decryptFromShortReadingStream() throws Exception {
        for (int size : SIZES) {
            byte[] plain = plain(size);
            byte[] ct = encrypt(new ByteArrayInputStream(plain));
            for (int chunk : CHUNKS) {
                assertArrayEquals(plain, decrypt(new ChunkyInputStream(ct, chunk), ct.length), "length-driven size=" + size + " chunk=" + chunk);
                assertArrayEquals(plain, decrypt(new ChunkyInputStream(ct, chunk), -1), "stream-driven size=" + size + " chunk=" + chunk);
            }
        }
    }

    @Test
    public void shortReadsBothSides() throws Exception {
        for (int size : SIZES) {
            byte[] plain = plain(size);
            for (int chunk : CHUNKS) {
                byte[] ct = encrypt(new ChunkyInputStream(plain, chunk));
                assertArrayEquals(plain, decrypt(new ChunkyInputStream(ct, chunk), -1), "size=" + size + " chunk=" + chunk);
            }
        }
    }

    /** Ciphertext size must not depend on how the input was chunked. */
    @Test
    public void ciphertextLengthIndependentOfChunking() throws Exception {
        for (int size : SIZES) {
            byte[] plain = plain(size);
            int expected = encrypt(new ByteArrayInputStream(plain)).length;
            for (int chunk : CHUNKS) {
                assertEquals(expected, encrypt(new ChunkyInputStream(plain, chunk)).length, "size=" + size + " chunk=" + chunk);
            }
        }
    }

    /** Legacy reader on a short-reading stream. */
    @Test
    public void legacyDecryptFromShortReadingStream() throws Exception {
        byte[] ct = AESCryptTest.resource("aescrypt/v2-key-1000.aes");
        for (int chunk : CHUNKS) {
            assertArrayEquals(plain(1000), decrypt(new ChunkyInputStream(ct, chunk), ct.length), "chunk=" + chunk);
        }
    }
}
