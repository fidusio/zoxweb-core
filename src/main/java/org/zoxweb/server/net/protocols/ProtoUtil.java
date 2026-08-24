package org.zoxweb.server.net.protocols;

import org.zoxweb.shared.util.BiDataDecoder;
import org.zoxweb.shared.util.BiDataEncoder;
import org.zoxweb.shared.util.DataDecoder;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.NVIntList;
import org.zoxweb.shared.util.NVLongList;
import org.zoxweb.shared.util.NVStringList;
import org.zoxweb.shared.util.SharedBase64;
import org.zoxweb.shared.util.SharedStringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stateless utilities for the protocol-validator subsystem (META-PROTOCOL.md §2.1): the
 * data-literal encoding, its {@code ${var}} parameter substitution, the binary-safe byte search,
 * and the {@link NVGenericMap} config readers.
 * <p>
 * Every data literal in a definition ({@code send}/{@code expect}/{@code terminator}/
 * {@code validate} match values) is a string with a one-word encoding prefix so binary and text
 * are expressed uniformly:
 * <ul>
 * <li>{@code txt:...} — UTF-8 text, taken verbatim (control bytes via JSON escapes like {@code \r\n})</li>
 * <li>{@code hex:...} — hexadecimal, whitespace ignored</li>
 * <li>{@code base64:...} — Base64</li>
 * </ul>
 * A value with no recognized prefix is treated as UTF-8 text in full — colon included.
 * <p>
 * The body may contain {@code ${name}} placeholders resolved against a variable bag — so a
 * definition stays generic (it never hardcodes a caller/environment value such as a client HELO
 * name); the caller injects the value when the session is created. The encoding prefix and the
 * variable layer never mix: the prefix is identified first, substitution is confined to the body,
 * and a variable value can never become an encoding directive.
 */
public final class ProtoUtil {

    private ProtoUtil() {
    }

    /** {@code ${name}} placeholder — name is any run of characters other than '}'. */
    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    /**
     * Decodes a prefixed literal ({@code txt:} / {@code hex:} / {@code base64:}) to bytes; a null
     * or empty literal yields an empty array. Throws {@link IllegalArgumentException} if a
     * declared hex/base64 body fails to decode.
     */
    public static final DataDecoder<String, byte[]> STRING_TO_DATA = (encoded) -> {
        if (encoded == null || encoded.isEmpty())
            return new byte[0];
        int c = encoded.indexOf(':');
        String prefix = c > 0 ? encoded.substring(0, c).toLowerCase() : "";
        String body = c > 0 ? encoded.substring(c + 1) : encoded;
        try {
            switch (prefix) {
                case "hex":
                    return SharedStringUtil.hexToBytes(body.replaceAll("\\s", ""));
                case "base64":
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

    /**
     * Variable substitution encoder: replaces every {@code ${name}} in the input string with the
     * variable bag's string value for {@code name}. A referenced variable that is absent (or whose
     * value is empty) is fatal — a generic definition that expects an injected value must not
     * silently ship the literal {@code ${name}} or an empty field.
     * <p>
     * {@code encode(template, vars)}: {@code vars} may be null (then any placeholder is
     * unresolved → fatal); a template with no placeholder is returned unchanged. Throws
     * {@link IllegalArgumentException} if a referenced variable is missing or empty.
     */
    public static final BiDataEncoder<String, NVGenericMap, String> STRING_VARS_TO_STRING = (s, vars) -> {
        if (s == null)
            return null;
        Matcher m = VAR_PATTERN.matcher(s);
        if (!m.find())
            return s;
        StringBuffer sb = new StringBuffer(s.length() + 16);
        do {
            String name = m.group(1);
            Object v = vars != null ? vars.getValue(name) : null;
            String value = v != null ? v.toString() : null;
            if (value == null || value.isEmpty())
                throw new IllegalArgumentException("unresolved exchange variable ${" + name + "}");
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        } while (m.find());
        m.appendTail(sb);
        return sb.toString();
    };

    /**
     * Variable-aware data decoder: resolves {@code ${var}} placeholders in the body of the
     * literal against the variable bag (via {@link #STRING_VARS_TO_STRING}), then decodes the
     * result via {@link #STRING_TO_DATA}. Substitution is confined to the body so a variable
     * value containing a colon can never be mistaken for the encoding prefix.
     * <p>
     * {@code decode(encoded, vars)}: null/empty literal yields an empty array. Throws
     * {@link IllegalArgumentException} on an unresolved variable or a body that fails to decode.
     */
    public static final BiDataDecoder<String, NVGenericMap, byte[]> STRING_VARS_TO_DATA = (encoded, vars) -> {
        if (encoded == null || encoded.isEmpty())
            return new byte[0];
        int c = encoded.indexOf(':');
        if (c <= 0)
            // prefix-less literal = UTF-8 text; the resolved string is taken verbatim, never
            // re-scanned for a prefix — a var value starting "hex:"/"base64:"/"txt:" is data,
            // not an encoding directive
            return SharedStringUtil.getBytes(STRING_VARS_TO_STRING.encode(encoded, vars));
        String prefix = encoded.substring(0, c);
        String body = STRING_VARS_TO_STRING.encode(encoded.substring(c + 1), vars);
        return STRING_TO_DATA.decode(prefix + ":" + body);
    };

    /**
     * @return true if {@code literal} contains at least one {@code ${var}} placeholder (so its
     * final bytes depend on the runtime variable bag and cannot be decoded at compile time)
     */
    public static boolean hasVars(String literal) {
        return literal != null && VAR_PATTERN.matcher(literal).find();
    }

    /**
     * @return the first index of {@code needle} in {@code haystack}, or -1; an empty needle
     * matches at 0. Binary-safe — the byte-sequence primitive behind {@code expect} and the
     * {@code validate} contains-match.
     */
    public static int indexOf(byte[] haystack, byte[] needle) {
        if (needle.length == 0)
            return 0;
        outer:
        for (int i = 0; i + needle.length <= haystack.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j])
                    continue outer;
            }
            return i;
        }
        return -1;
    }

    /**
     * @return true if {@code message} starts with {@code prefix} — the byte-level primitive
     * behind the {@code validate} prefix match
     */
    public static boolean startsWith(byte[] message, byte[] prefix) {
        if (message.length < prefix.length)
            return false;
        for (int i = 0; i < prefix.length; i++) {
            if (message[i] != prefix[i])
                return false;
        }
        return true;
    }

    /**
     * @return the string value of {@code name} in the bag ({@code def} when the bag is null, the
     * entry is absent, empty, or not a string). Config-reading primitive.
     */
    public static String stringValue(NVGenericMap map, String name, String def) {
        if (map == null)
            return def;
        Object v = map.getValue(name);
        return v instanceof String && !((String) v).isEmpty() ? (String) v : def;
    }

    /**
     * @return the int value of {@code name} in the bag ({@code def} when the bag is null, the
     * entry is absent or not numeric). JSON numbers may parse as int/long/double — any Number
     * is accepted.
     */
    public static int intValue(NVGenericMap map, String name, int def) {
        if (map == null)
            return def;
        Object v = map.getValue(name);
        return v instanceof Number ? ((Number) v).intValue() : def;
    }

    /**
     * @return the boolean value of {@code name} in the bag ({@code def} when the bag is null,
     * the entry is absent or not a boolean)
     */
    public static boolean booleanValue(NVGenericMap map, String name, boolean def) {
        if (map == null)
            return def;
        Object v = map.getValue(name);
        return v instanceof Boolean ? (Boolean) v : def;
    }

    /**
     * The definition's {@code port} declaration — a single number or an array of the protocol's
     * well-known ports ({@code "port": [25, 587]} for SMTP STARTTLS). Always a <b>hint</b>,
     * never an endpoint: the first entry is the default used when the caller's endpoint omits a
     * port; the full list documents where the protocol is expected to live.
     *
     * @param config the parsed definition (null tolerated)
     * @return the declared ports in declaration order, empty when absent
     * @throws IllegalArgumentException on an empty list, a non-numeric entry, or a port outside
     *                                  [1, 65535]
     */
    public static int[] ports(NVGenericMap config) {
        Object nv = config != null ? config.getNV("port") : null;
        if (nv == null)
            return new int[0];
        List<Integer> values = new ArrayList<Integer>();
        if (nv instanceof NVIntList) {
            for (Integer v : ((NVIntList) nv).getValue())
                values.add(v);
        } else if (nv instanceof NVLongList) {
            for (Long v : ((NVLongList) nv).getValue())
                values.add(v.intValue());
        } else if (nv instanceof NVStringList) {
            // a JSON string array — or the parser's fallback for an empty array
            for (String s : ((NVStringList) nv).getValue()) {
                try {
                    values.add(Integer.parseInt(s.trim()));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("invalid port: " + s);
                }
            }
        } else if (nv instanceof GetNameValue && ((GetNameValue<?>) nv).getValue() instanceof Number) {
            values.add(((Number) ((GetNameValue<?>) nv).getValue()).intValue());
        } else {
            throw new IllegalArgumentException("unsupported port declaration: " + nv);
        }
        if (values.isEmpty())
            throw new IllegalArgumentException("empty port list");
        int[] ret = new int[values.size()];
        for (int i = 0; i < ret.length; i++) {
            int p = values.get(i);
            if (p < 1 || p > 65535)
                throw new IllegalArgumentException("port out of range: " + p);
            ret[i] = p;
        }
        return ret;
    }
}
