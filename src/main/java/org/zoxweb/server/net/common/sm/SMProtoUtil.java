package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.fsm.StateMachineInt;
import org.zoxweb.shared.util.BiDataDecoder;
import org.zoxweb.shared.util.BiDataEncoder;
import org.zoxweb.shared.util.DataDecoder;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.NamedValue;
import org.zoxweb.shared.util.SharedBase64;
import org.zoxweb.shared.util.SharedStringUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared utilities for the {@code sm} package's declarative protocol layer: the {@code exchange}
 * data encoding and its {@code ${var}} parameter substitution. Any new encoder/decoder or common
 * helper for the package belongs here.
 * <p>
 * Every {@code send}/{@code expect} value in the JSON config is a string with a one-word encoding
 * prefix so binary and text are expressed uniformly:
 * <ul>
 * <li>{@code txt:...} — UTF-8 text, taken verbatim (control bytes via JSON escapes like {@code \r\n})</li>
 * <li>{@code hex:...} — hexadecimal, whitespace ignored</li>
 * <li>{@code base64:...} — Base64</li>
 * </ul>
 * A value with no recognized prefix is treated as {@code txt:} (UTF-8 verbatim).
 * <p>
 * The body may contain {@code ${name}} placeholders resolved against a caller-supplied variable bag
 * — so a protocol config stays generic (it never hardcodes a caller/environment value such as a
 * client HELO name); the caller injects the value when the connection is created. Substitution is
 * exposed as {@link #STRING_VARS_TO_STRING} (an encoder: template + vars → resolved string) and the
 * variable-aware byte decode as {@link #STRING_VARS_TO_DATA} (a decoder: literal + vars → bytes).
 */
public final class SMProtoUtil {

    private SMProtoUtil() {
    }

    /** {@code ${name}} placeholder — name is any run of characters other than '}'. */
    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    /**
     * Decodes a prefixed literal ({@code txt:} / {@code hex:} / {@code base64:}) to bytes; a null or
     * empty literal yields an empty array. Throws {@link IllegalArgumentException} if a declared
     * hex/base64 body fails to decode.
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
     * value is empty) is fatal — a generic protocol config that expects an injected value must not
     * silently ship the literal {@code ${name}} or an empty field.
     * <p>
     * {@code encode(template, vars)}: {@code vars} may be null (then any placeholder is unresolved →
     * fatal); a template with no placeholder is returned unchanged. Throws
     * {@link IllegalArgumentException} if a referenced variable is missing or empty.
     */
    public static final BiDataEncoder<String, NVGenericMap, String> STRING_VARS_TO_STRING = (s, vars)->{
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
     * Variable-aware data decoder: resolves {@code ${var}} placeholders in the body of the literal
     * against the variable bag (via {@link #STRING_VARS_TO_STRING}), then decodes the result via
     * {@link #STRING_TO_DATA}. Substitution is confined to the body so a variable value containing a
     * colon can never be mistaken for the encoding prefix.
     * <p>
     * {@code decode(encoded, vars)}: null/empty literal yields an empty array. Throws
     * {@link IllegalArgumentException} on an unresolved variable or a body that fails to decode.
     */
    public static final BiDataDecoder<String, NVGenericMap, byte[]> STRING_VARS_TO_DATA = (encoded, vars)->{
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
     * final bytes depend on the runtime variable bag and cannot be decoded at build time)
     */
    public static boolean hasVars(String literal) {
        return literal != null && VAR_PATTERN.matcher(literal).find();
    }

    /**
     * Name of the machine-wide results bag in the state machine's properties, see
     * {@link #results(StateMachineInt)}.
     */
    public static final String RESULTS = "results";

    /**
     * The machine's shared result-accumulation bag: an {@link NVGenericMap} stored in the state
     * machine's properties as a {@code NamedValue<NVGenericMap>} under {@link #RESULTS} — every
     * state and TriggerConsumer reaches it via {@code getStateMachine().getProperties()}, and the
     * caller reads the accumulated outcome (banner, negotiated TLS, ready flag, ...) after the
     * session closes. Lazily registered on first access; {@code ClientConSMBuilder} pre-registers
     * it at build time.
     *
     * @param smi the state machine
     * @return the machine's results bag (never null)
     */
    public static NVGenericMap results(StateMachineInt<?> smi) {
        NVGenericMap properties = smi.getProperties();
        synchronized (properties) {
            NamedValue<NVGenericMap> nv = properties.getNV(RESULTS);
            if (nv == null) {
                nv = new NamedValue<NVGenericMap>(RESULTS, new NVGenericMap(RESULTS));
                properties.add(nv);
            }
            return nv.getValue();
        }
    }

    public enum BasicEvent {
        CONNECTED,
        DATAGRAM,
        CLOSED,
        IN_RAW_DATA,
    }
}
