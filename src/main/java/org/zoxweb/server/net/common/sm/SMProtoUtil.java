package org.zoxweb.server.net.common.sm;

import org.zoxweb.shared.util.DataDecoder;
import org.zoxweb.shared.util.SharedBase64;
import org.zoxweb.shared.util.SharedStringUtil;

/**
 * Decodes a prefixed {@code exchange} data literal into raw bytes. Every {@code send}/{@code expect}
 * value in the JSON config is a string with a one-word encoding prefix so binary and text are
 * expressed uniformly:
 * <ul>
 * <li>{@code txt:...} — UTF-8 text, taken verbatim (control bytes via JSON escapes like {@code \r\n})</li>
 * <li>{@code hex:...} — hexadecimal, whitespace ignored</li>
 * <li>{@code bin:...} — Base64</li>
 * </ul>
 * A value with no recognized prefix is treated as {@code txt:} (UTF-8 verbatim).
 */
public final class SMProtoUtil {

    private SMProtoUtil() {
    }

    /**
     * @param encoded a prefixed literal ({@code txt:} / {@code hex:} / {@code bin:}); null or empty
     *                yields an empty array
     * @return the decoded bytes
     * @throws IllegalArgumentException if a declared hex/bin body fails to decode
     */
    public static final DataDecoder<String, byte[]> STRING_TO_DATA = (encoded)->{
        if (encoded == null || encoded.isEmpty())
            return new byte[0];
        int c = encoded.indexOf(':');
        String prefix = c > 0 ? encoded.substring(0, c).toLowerCase() : "";
        String body = c > 0 ? encoded.substring(c + 1) : encoded;
        try {
            switch (prefix) {
                case "hex":
                    return SharedStringUtil.hexToBytes(body.replaceAll("\\s", ""));
                case "bin":
                    return SharedBase64.decode(body);
                case "txt":
                    return SharedStringUtil.getBytes(body);
                default:
                    // no recognized prefix: whole string is UTF-8 text
                    return SharedStringUtil.getBytes(encoded);
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("cannot decode " + prefix + ": data: " + e.getMessage(), e);
        }
    };
}
