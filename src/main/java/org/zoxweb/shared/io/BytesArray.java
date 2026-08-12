package org.zoxweb.shared.io;

import org.zoxweb.shared.util.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A read-only view (slice) over a byte array, defined by an immutable {@code (array, offset, length)}
 * window and an optional shared validity token.
 * <p>
 * The validity token is an {@link AtomicBoolean} shared with the producer that owns the backing
 * array (e.g. {@code UByteArrayOutputStream#toBytesArray(boolean)}). When the producer mutates or
 * recycles its buffer, it flips the token to {@code false}, invalidating every outstanding view at
 * once; accessors on an invalidated view throw {@link ProtocolException}. A {@code null} token
 * means the view owns its data and is permanently valid.
 * <p>
 * Usage notes:
 * <ul>
 * <li>Validity is best-effort, not a memory fence: a producer may mutate its buffer concurrently
 * with a read that passed the validity check. {@link #copy()} narrows that window by re-checking
 * after copying but cannot fully close it. Views over shared buffers are meant for short-lived,
 * same-dispatch parsing; call {@link #copy()} to retain data beyond that scope.</li>
 * <li>The string-oriented methods ({@link #asString()}, {@link #indexOf(String)},
 * {@link #toString(int, int)}, ...) are ASCII/HTTP-token oriented: matching compares raw bytes to
 * chars and decoding uses the platform default charset. Do not rely on them for non-ASCII text.</li>
 * </ul>
 */
public final class BytesArray
        implements IsValid {

    /** Shared permanently-valid empty view. */
    public static final BytesArray EMPTY = new BytesArray(null, Const.EMPTY_BYTE_ARRAY);
    private final byte[] array;
    public final int offset;
    public final int length;

    // volatile on purpose DO NOT CONVERT to final
    private volatile AtomicBoolean valid;
    private volatile Integer hashCode = null;

    /**
     * Creates a view over {@code array} starting at {@code offset} spanning {@code length} bytes.
     *
     * @param valid         shared validity token owned by the buffer producer, null for a permanently valid view
     * @param array         the backing byte array, never copied
     * @param offset        start of the view within array
     * @param length        number of bytes in the view
     * @param checkBoundary if true validate that the window fits within array
     * @throws NullPointerException      if array is null
     * @throws IndexOutOfBoundsException if checkBoundary and the window is invalid
     */
    public BytesArray(AtomicBoolean valid, byte[] array, int offset, int length, boolean checkBoundary) {
        SUS.checkIfNull("Byte array null", array);
        if (checkBoundary && (offset < 0 || length < 0 || (offset + length > array.length)))
            throw new IndexOutOfBoundsException("Invalid offset and length " + offset + " ," + length + " ," + (offset + length) + " ," + array.length);
        this.array = array;
        this.offset = offset;
        this.length = length;
        this.valid = valid; // valid != null ? valid : new AtomicBoolean(true);
    }

    /**
     * Creates a boundary-checked view over {@code array} starting at {@code offset} spanning {@code length} bytes.
     *
     * @param valid  shared validity token, null for a permanently valid view
     * @param array  the backing byte array, never copied
     * @param offset start of the view within array
     * @param length number of bytes in the view
     */
    public BytesArray(AtomicBoolean valid, byte[] array, int offset, int length) {
        this(valid, array, offset, length, true);
    }

    /**
     * Creates a view over the whole of {@code array}.
     *
     * @param valid shared validity token, null for a permanently valid view
     * @param array the backing byte array, never copied
     */
    public BytesArray(AtomicBoolean valid, byte[] array) {
        this(valid, array, 0, array.length, true);
    }

    /**
     * Creates a permanently valid view over the whole of {@code array}.
     *
     * @param array the backing byte array, never copied
     */
    public BytesArray(byte[] array) {
        this(null, array, 0, array.length, true);
    }

    /**
     * Returns the byte at {@code index} relative to the view.
     *
     * @param index 0 based index within the view, valid range [0, length)
     * @return the byte value as a signed int
     * @throws IndexOutOfBoundsException if index is outside the view
     * @throws ProtocolException         if the view has been invalidated
     */
    public int byteAt(int index) {
        checkValidity();
        if (index < 0)
            throw new IndexOutOfBoundsException("Invalid index " + index);
        int byteIndex = offset + index;
        if (byteIndex >= offset + length)
            throw new ArrayIndexOutOfBoundsException(index + " out of bound length: " + length);
        return array[byteIndex];
    }

    /**
     * @return the view decoded as a String using the platform default charset
     * @throws ProtocolException if the view has been invalidated
     */
    public String asString() {
        checkValidity();
        return new String(array, offset, length);
    }

    /**
     * @return a copy of the byte array if you want the direct access {@link #getArray()}
     * @throws ProtocolException if the view has been invalidated
     */
    public byte[] asBytes() {
        checkValidity();
        return Arrays.copyOfRange(array, offset, offset + length);
    }

    /**
     * Creates a permanently valid, self-owned BytesArray by copying {@code length} bytes of
     * {@code data} starting at {@code offset}; data can be reused or mutated afterwards.
     *
     * @param data   the source array, copied not wrapped
     * @param offset start of the range within data
     * @param length number of bytes to copy
     * @return a detached BytesArray with offset 0 and a null validity token
     * @throws NullPointerException      if data is null
     * @throws IndexOutOfBoundsException if the range is outside data
     */
    public static BytesArray create(byte[] data, int offset, int length)
    {
        SUS.checkIfNull("Byte array null", data);
        if (offset < 0 || length < 0 || offset + length > data.length)
            throw new IndexOutOfBoundsException("Invalid offset and length " + offset + " ," + length + " ," + data.length);
        return new BytesArray(Arrays.copyOfRange(data, offset, offset + length));
    }

    /**
     * Returns a permanently valid, self-owned copy of this view.
     * <p>
     * Validity is re-checked after copying to catch invalidation that happened mid-copy, but this
     * is best-effort: a producer that mutates its buffer before flipping the validity token can
     * still tear the copied bytes.
     *
     * @return a detached copy with offset 0 and a null validity token
     * @throws ProtocolException if the view was invalidated before or during the copy
     */
    public BytesArray copy() {
        checkValidity();
        BytesArray ret = new BytesArray(null, asBytes());
        checkValidity();// we must perform double validation
        return ret;
    }

    /**
     * @return true if the view is still valid, always true when the validity token is null
     */
    public boolean isValid() {
        return valid == null || valid.get();
    }

    @Override
    public String toString() {
        return "BytesArray{" +
                "offset=" + offset +
                ", length=" + length +
                ", valid=" + isValid() +
                '}';
    }

    /**
     * Writes the view's bytes to {@code os}.
     *
     * @param os    the destination stream
     * @param flush if true flush os after writing
     * @throws IOException if the view has been invalidated or the write fails
     */
    public void writeTo(OutputStream os, boolean flush)
            throws IOException {
        if (isValid()) {
            os.write(array, offset, length);
            if (flush)
                os.flush();
        } else
            throw new IOException("Byte buffer invalid");
    }

    /**
     * Returns an InputStream over the view. The stream reads the shared backing array directly and
     * is not protected against later invalidation.
     *
     * @return an InputStream positioned at the start of the view
     * @throws IOException if the view has been invalidated
     */
    public InputStream toInputStream()
            throws IOException {
        if (isValid())
            return new ByteArrayInputStream(array, offset, length);
        else
            throw new IOException("Byte buffer invalid");
    }

    /**
     * @throws ProtocolException if the view has been invalidated
     */
    public void checkValidity() {
        if (!isValid())
            throw new ProtocolException("Invalid BytesArray");
    }

    /**
     * @return the internal byte array use with extreme caution
     * @throws ProtocolException if the view has been invalidated
     */
    public byte[] getArray() {
        checkValidity();
        return array;
    }

    /**
     * Compares content. As a convenience, {@code o} may be a raw {@code byte[]}, compared against
     * the full view; that comparison is asymmetric by nature (an array never equals back).
     * An invalidated view compares unequal instead of throwing.
     *
     * @param o a BytesArray or byte[]
     * @return true if the contents match
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) return false;

        if (o instanceof byte[]) {
            byte[] input = (byte[]) o;
            return SharedUtil.equals(this, array, offset, offset + length, input, 0, input.length);
        }

        if (getClass() != o.getClass()) return false;

        BytesArray that = (BytesArray) o;
        return (length == that.length) && SharedUtil.equals(() -> isValid() && that.isValid(), array, offset, offset + length, that.array, that.offset, that.offset + that.length);
    }

    /**
     * Content-based hash, computed once and cached. Note: throws if the view is invalidated before
     * the first call, so hash the view (or copy it) while valid when using it as a collection key.
     *
     * @return the cached content hash
     * @throws ProtocolException if the view is invalidated and the hash was never computed
     */
    @Override
    public int hashCode() {
        if (hashCode == null) {
            synchronized (this) {
                if (hashCode == null) {
                    checkValidity();
                    hashCode = SharedUtil.hashCode(array, offset, length);
                }
            }
        }
        return hashCode;
    }

    /**
     * Return the first index of matching bytes in contained within the stream
     *
     * @param match for byte array matching
     * @return index of the match, -1 no match found
     * @throws ProtocolException if the view has been invalidated
     */
    public int indexOf(byte[] match) {
        checkValidity();
        return SUS.indexOf(array, offset, offset + length, match, 0, match.length);
    }

    /**
     * @param startAt     index inclusive, relative to the view
     * @param match       byte array to match
     * @param matchOffset offset within the match array
     * @param matchLength length of the match starting at matchOffset
     * @return index -1 not found or index of the first match, relative to the view
     * @throws IllegalArgumentException if startAt is negative
     * @throws ProtocolException        if the view has been invalidated
     */
    public int indexOf(int startAt, byte[] match, int matchOffset, int matchLength) {
        checkValidity();
        if (startAt < 0)
            throw new IllegalArgumentException("Negative start index " + startAt);

        int ret = SUS.indexOf(array, offset + startAt, offset + length, match, matchOffset, matchLength);
        return ret != -1 ? ret - offset : -1;
    }

    /**
     * @param startAt index inclusive, relative to the view
     * @param match   byte array to match
     * @return index -1 not found or index of the first match, relative to the view
     * @throws IllegalArgumentException if startAt is negative
     * @throws ProtocolException        if the view has been invalidated
     */
    public int indexOf(int startAt, byte[] match) {
        checkValidity();
        if (startAt < 0)
            throw new IllegalArgumentException("Negative start index " + startAt);
        int ret = SUS.indexOf(array, offset + startAt, offset + length, match, 0, match.length);
        return ret != -1 ? ret - offset : -1;
    }

    /**
     * @param startAt index inclusive, relative to the view
     * @param str     string to match, converted to bytes as UTF-8
     * @return index -1 not found or index of the first match, relative to the view
     * @throws IllegalArgumentException if startAt is negative
     * @throws ProtocolException        if the view has been invalidated
     */
    public int indexOf(int startAt, String str) {
        checkValidity();
        if (startAt < 0)
            throw new IllegalArgumentException("Negative start index " + startAt);
        byte[] match = SharedStringUtil.getBytes(str);
        int ret = SUS.indexOf(array, offset + startAt, offset + length, match, 0, match.length);
        return ret != -1 ? ret - offset : -1;
    }

    /**
     * @param str string to match, compared char to byte (ASCII oriented)
     * @return index -1 not found or index of the first match, relative to the view
     * @throws ProtocolException if the view has been invalidated
     */
    public int indexOf(String str) {
        checkValidity();
        int ret = SUS.indexOf(array, offset, offset + length, str, 0, str.length(), false);
        return ret != -1 ? ret - offset : -1;
    }

    /**
     * @param str string to match case-insensitively, compared char to byte (ASCII oriented)
     * @return index -1 not found or index of the first match, relative to the view
     * @throws ProtocolException if the view has been invalidated
     */
    public int indexOfIgnoreCase(String str) {
        checkValidity();
        int ret = SUS.indexOf(array, offset, offset + length, str, 0, str.length(), true);
        return ret != -1 ? ret - offset : -1;
    }

    /**
     * Decodes a sub-range of the view as a String using the platform default charset.
     *
     * @param startIndex start index relative to the view
     * @param strLength  number of bytes to decode
     * @return the decoded String
     * @throws IllegalArgumentException if the range is outside the view
     * @throws ProtocolException        if the view has been invalidated
     */
    public String toString(int startIndex, int strLength) {
        checkValidity();
        if (startIndex < 0 || strLength < 0 || strLength > (length - startIndex))
            throw new IllegalArgumentException("Invalid index " + startIndex + " or length " + strLength);
        return new String(array, offset + startIndex, strLength);
    }

    /**
     * Decodes the view from {@code startIndex} to its end as a String using the platform default charset.
     *
     * @param startIndex start index relative to the view
     * @return the decoded String
     * @throws IllegalArgumentException if startIndex is outside the view
     * @throws ProtocolException        if the view has been invalidated
     */
    public String toString(int startIndex) {
        checkValidity();
        if (startIndex < 0 || startIndex > length)
            throw new IllegalArgumentException("Negative start index " + startIndex);
        return new String(array, offset + startIndex, length - startIndex);
    }


}
