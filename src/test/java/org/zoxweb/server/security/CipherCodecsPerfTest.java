package org.zoxweb.server.security;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.crypto.EncryptedData;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Throughput of {@link CipherCodecs#EDEncoder} and {@link CipherCodecs#EDDecoder} on sealed
 * records of several plaintext sizes, single thread, with the entity JSON path measured alongside
 * for scale. Prints a table; asserts only that every round trip is faithful.
 */
public class CipherCodecsPerfTest {

    private static final byte[] KEY = SecUtil.randomBytes(32);
    private static final int[] SIZES = {64, 1024, 16 * 1024, 256 * 1024};
    private static final int WARMUP = 20_000;
    private static final int ITERATIONS = 200_000;

    private static EncryptedData sealed(int size) throws Exception {
        EncryptedData ed = new EncryptedData();
        ed.setDataType("org.zoxweb.shared.data.PropertyDAO");
        ed.setMask("****1234");
        ed.setExpiry(1_900_000_000_000L);
        ed.setHint("perf");
        byte[] plain = new byte[size];
        new java.util.Random(size).nextBytes(plain);
        return CryptoUtil.encryptData(ed, KEY, plain);
    }

    private static String toJson(EncryptedData ed) {
        try {
            return GSONUtil.toJSON(ed, false, false, false);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static long nsPerOp(Runnable op, int iterations) {
        long t0 = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            op.run();
        }
        return (System.nanoTime() - t0) / iterations;
    }

    private static String row(String label, int payload, int bytes, long encodeNs, long decodeNs) {
        double mbEnc = bytes / (encodeNs / 1e9) / (1024 * 1024);
        double mbDec = bytes / (decodeNs / 1e9) / (1024 * 1024);
        return String.format(Locale.ROOT, "%-8s %9d %9d %12d %12d %10.0f %10.0f",
                label, payload, bytes, encodeNs, decodeNs, mbEnc, mbDec);
    }

    @Test
    public void throughput() throws Exception {
        StringBuilder out = new StringBuilder();
        out.append(String.format(Locale.ROOT, "%n%-8s %9s %9s %12s %12s %10s %10s%n",
                "form", "plain B", "stored B", "encode ns", "decode ns", "enc MB/s", "dec MB/s"));

        for (int size : SIZES) {
            EncryptedData ed = sealed(size);
            int iterations = size >= 64 * 1024 ? ITERATIONS / 20 : ITERATIONS;

            // packed
            byte[] packed = CipherCodecs.EDEncoder.encode(ed);
            EncryptedData back = CipherCodecs.EDDecoder.decode(packed);
            assertArrayEquals(ed.getEncryptedData(), back.getEncryptedData());
            assertEquals(ed.getDataType(), back.getDataType());
            nsPerOp(() -> CipherCodecs.EDEncoder.encode(ed), WARMUP);
            nsPerOp(() -> CipherCodecs.EDDecoder.decode(packed), WARMUP);
            long encNs = nsPerOp(() -> CipherCodecs.EDEncoder.encode(ed), iterations);
            long decNs = nsPerOp(() -> CipherCodecs.EDDecoder.decode(packed), iterations);
            out.append(row("packed", size, packed.length, encNs, decNs)).append('\n');

            // entity JSON, for scale: what the store would write without the codec
            String json = toJson(ed);
            EncryptedData fromJson = GSONUtil.fromJSON(json, EncryptedData.class);
            assertArrayEquals(ed.getEncryptedData(), fromJson.getEncryptedData());
            int jsonIterations = iterations / 10;
            nsPerOp(() -> toJson(ed), WARMUP / 10);
            nsPerOp(() -> GSONUtil.fromJSON(json, EncryptedData.class), WARMUP / 10);
            long jEncNs = nsPerOp(() -> toJson(ed), jsonIterations);
            long jDecNs = nsPerOp(() -> GSONUtil.fromJSON(json, EncryptedData.class), jsonIterations);
            out.append(row("json", size, json.length(), jEncNs, jDecNs)).append('\n');
        }
        System.out.println(out);
    }
}
