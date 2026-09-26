package org.zoxweb.server.security;

import org.zoxweb.shared.crypto.EncapsulatedKey;
import org.zoxweb.shared.crypto.EncryptedData;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.DataDecoder;
import org.zoxweb.shared.util.DataEncoder;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * Binary storage form of a sealed {@link EncryptedData}: {@link #EDEncoder} packs the record into a
 * {@code byte[]} for a datastore column, {@link #EDDecoder} unpacks it. Both are content-blind.
 * They move the record's attributes between object and bytes and nothing else: no key, no
 * decryption, no base64, no canonical string. The ciphertext travels untouched.
 *
 * <h3>Layout</h3>
 * Fields follow in this order, big-endian, no padding, no markers between them. Every text field
 * is UTF-8 with an unsigned length prefix; a length of 0 means the attribute is absent and decodes
 * to null.
 * <pre>
 * #   field         type       bytes    encode (getter -> bytes)                decode (bytes -> setter)
 * 1   v             u8         1        (byte) getVersion()                     must equal EncryptedData.VERSION, else refused
 * 2   alg           u8 + utf8  1 + n    getAlgorithm(), n <= 255                setAlgorithm
 * 3   kdf           u8 + utf8  1 + n    getKDF(), n <= 255                      setKDF
 * 4   iv            u8 + raw   1 + n    getIV(), n <= 255, null -> 0            setIV
 * 5   data_length   i64        8        getDataLength()                         setDataLength
 * 6   data_type     u16 + utf8 2 + n    getDataType(), 1 <= n <= 65535          setDataType; n == 0 refused
 * 7   mask          u16 + utf8 2 + n    getMask(), n <= 65535, null -> 0        setMask
 * 8   exp           i64        8        getExpiry(), 0 when absent              setExpiry
 * 9   hint          u16 + utf8 2 + n    getHint(), n <= 65535, null -> 0        setHint
 * 10  cipher_data   raw        n        getEncryptedData(), to end of buffer    setEncryptedData; n == 0 refused
 * </pre>
 * {@code data_type} says what the plaintext is. By default it is the class name of the plaintext's
 * Java type, which is why it gets a two-byte length. It is required: the encoder refuses a record
 * without one and the decoder refuses a zero length. {@code cipher_data} is last and carries no
 * length, so the decoder takes everything that remains.
 *
 * <h3>What is and is not checked</h3>
 * The decoder checks the layout only: the version byte, that each length fits in the buffer, that
 * {@code data_type} is present and that at least one ciphertext byte follows. Every failure is an
 * {@link IllegalArgumentException}. It does not check that the algorithm is known, that the IV is
 * 12 bytes, that {@code data_length} matches the plaintext or that the ciphertext is genuine.
 * Those are decided when the record is opened: every attribute is under the GCM tag, so a byte
 * changed in storage decodes fine and then fails at open with a {@code SignatureException}. This
 * packed form is never the associated data.
 *
 * <h3>Refusals</h3>
 * The encoder throws {@link IllegalArgumentException} for a record with no ciphertext, a record
 * with no {@code data_type}, a text attribute over its length limit, and any
 * {@link EncapsulatedKey}. A key row is persisted as an entity, field by field, and packing it here
 * would silently drop its binding fields. Null in gives null out on both sides.
 */
public interface CipherCodecs {
    /**
     * Unpacks bytes written by {@link #EDEncoder} into a new {@link EncryptedData}, following the
     * layout in the class description. Layout checks only; the content is judged at open time.
     *
     * @throws IllegalArgumentException on a wrong version, a missing {@code data_type}, no
     *                                  ciphertext, or a buffer that ends before a field does
     */
    DataDecoder<byte[], EncryptedData> EDDecoder = encoded -> {
        if (encoded == null) {
            return null;
        }
        try {
            ByteBuffer bb = ByteBuffer.wrap(encoded);
            int version = bb.get() & 0xFF;
            if (version != EncryptedData.VERSION) {
                throw new IllegalArgumentException("Unsupported record version " + version);
            }
            EncryptedData ret = new EncryptedData();
            ret.setVersion(version);
            ret.setAlgorithm(CryptoUtil.packedText(bb, bb.get() & 0xFF));
            ret.setKDF(CryptoUtil.packedText(bb, bb.get() & 0xFF));
            ret.setIV(CryptoUtil.packedBytes(bb, bb.get() & 0xFF));
            ret.setDataLength(bb.getLong());
            String dataType = CryptoUtil.packedText(bb, bb.getShort() & 0xFFFF);
            if (dataType == null) {
                throw new IllegalArgumentException("record has no data_type");
            }
            ret.setDataType(dataType);
            ret.setMask(CryptoUtil.packedText(bb, bb.getShort() & 0xFFFF));
            ret.setExpiry(bb.getLong());
            ret.setHint(CryptoUtil.packedText(bb, bb.getShort() & 0xFFFF));
            if (!bb.hasRemaining()) {
                throw new IllegalArgumentException("record has no ciphertext");
            }
            ret.setEncryptedData(CryptoUtil.packedBytes(bb, bb.remaining()));
            return ret;
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("truncated record", e);
        }
    };
    /**
     * Packs a sealed {@link EncryptedData} into bytes for storage, following the layout in the
     * class description; {@link #EDDecoder} unpacks it. The buffer is sized exactly once from the
     * field lengths, so the result has no slack.
     *
     * @throws IllegalArgumentException for a record with no ciphertext or no {@code data_type}, a
     *                                  text attribute over its length limit, or an {@link EncapsulatedKey}
     */
    DataEncoder<EncryptedData, byte[]> EDEncoder = record -> {
        if (record == null) {
            return null;
        }
        if (record instanceof EncapsulatedKey) {
            throw new IllegalArgumentException("EncapsulatedKey is persisted as an entity, not packed");
        }
        byte[] cipherText = record.getEncryptedData();
        if (cipherText == null || cipherText.length == 0) {
            throw new IllegalArgumentException("record is not sealed");
        }
        byte[] alg = CryptoUtil.packedText("alg", record.getAlgorithm(), 0xFF);
        byte[] kdf = CryptoUtil.packedText("kdf", record.getKDF(), 0xFF);
        byte[] iv = record.getIV() != null ? record.getIV() : Const.EMPTY_BYTE_ARRAY;
        byte[] dataType = CryptoUtil.packedText("data_type", record.getDataType(), 0xFFFF);
        if (dataType.length == 0) {
            throw new IllegalArgumentException("record has no data_type");
        }
        byte[] mask = CryptoUtil.packedText("mask", record.getMask(), 0xFFFF);
        byte[] hint = CryptoUtil.packedText("hint", record.getHint(), 0xFFFF);
        if (iv.length > 0xFF) {
            throw new IllegalArgumentException("iv too long: " + iv.length);
        }
        ByteBuffer bb = ByteBuffer.allocate(1
                + 1 + alg.length
                + 1 + kdf.length
                + 1 + iv.length
                + 8
                + 2 + dataType.length
                + 2 + mask.length
                + 8
                + 2 + hint.length
                + cipherText.length);
        bb.put((byte) record.getVersion());
        bb.put((byte) alg.length).put(alg);
        bb.put((byte) kdf.length).put(kdf);
        bb.put((byte) iv.length).put(iv);
        bb.putLong(record.getDataLength());
        bb.putShort((short) dataType.length).put(dataType);
        bb.putShort((short) mask.length).put(mask);
        bb.putLong(record.getExpiry());
        bb.putShort((short) hint.length).put(hint);
        bb.put(cipherText);
        return bb.array();
    };
}
