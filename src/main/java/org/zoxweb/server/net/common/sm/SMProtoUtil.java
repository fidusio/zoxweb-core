package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.fsm.State;
import org.zoxweb.server.fsm.StateMachine;
import org.zoxweb.server.fsm.StateMachineEvent;
import org.zoxweb.server.fsm.StateMachineInt;
import org.zoxweb.server.fsm.StateMachineListener;
import org.zoxweb.shared.util.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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

    /**
     * Pull-style completion wait built on the machine's native completion signal
     * (META-SM-PROTO-DESIGN.md §10): arms a latch behind a {@code MACHINE_CLOSED}
     * {@link StateMachineListener} and awaits it — push and pull are one implementation.
     * Teardown closes the machine <b>last</b>, after the {@code CLOSED} publish, so when this
     * returns true the report ({@link #results}) and the close cause ({@link #closeCause}) are
     * final. The listener is removed before returning.
     *
     * @param smi           the session machine
     * @param timeoutMillis maximum wait; {@code <= 0} polls {@code isClosed()} without waiting
     * @return true if the machine is closed, false if the wait timed out
     */
    public static boolean waitForClose(StateMachine<?> smi, long timeoutMillis) {
        SUS.checkIfNulls("state machine null", smi);
        if (smi.isClosed() || timeoutMillis <= 0)
            return smi.isClosed();
        final CountDownLatch latch = new CountDownLatch(1);
        StateMachineListener listener = event -> {
            if (event.getType() == StateMachineEvent.Type.MACHINE_CLOSED)
                latch.countDown();
        };
        smi.addListener(listener);
        try {
            // the machine may have closed between the check above and the registration —
            // the listener would never fire for that close
            if (smi.isClosed())
                return true;
            return latch.await(timeoutMillis, TimeUnit.MILLISECONDS) || smi.isClosed();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return smi.isClosed();
        } finally {
            smi.removeListener(listener);
        }
    }

    /**
     * The session's terminating cause: the Throwable the callback error path stashed in the
     * machine properties under the canonical {@code EXCEPTION} name (identical for both
     * transports) and republished as the {@code CLOSED} payload by teardown.
     *
     * @param smi the session machine
     * @return the stashed cause, or null (clean close — or a session still running)
     */
    public static Throwable closeCause(StateMachine<?> smi) {
        SUS.checkIfNulls("state machine null", smi);
        NamedValue<Throwable> cause = smi.getProperties().getNV(TCPSMCallback.Params.EXCEPTION.name());
        return cause != null ? cause.getValue() : null;
    }

    /**
     * Name of the assembler's accumulation holder in the machine properties (the shared
     * blackboard): the {@code assembler} state installs a
     * {@link MessageAssemblerState.Assembly} there when it is registered, and the
     * {@code controller} state coordinates with it — the {@code stream} consume-through-match
     * and the {@code start_tls} residue check — through the bag, never through state instances.
     */
    public static final String ASSEMBLY = "assembly";

    /**
     * @param smi the session machine
     * @return the assembler's accumulation holder from the machine bag, or null when no
     * assembler state is registered (treated as an empty accumulation by consumers)
     */
    public static MessageAssemblerState.Assembly assembly(StateMachineInt<?> smi) {
        NamedValue<MessageAssemblerState.Assembly> nv = smi.getProperties().getNV(ASSEMBLY);
        return nv != null ? nv.getValue() : null;
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
     * @return the string value of {@code name} in the bag ({@code def} when the bag is null, the
     * entry is absent, empty, or not a string). Bag-reading primitive for state configuration.
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
     * Seeds a config block into a state's properties bag — the composition step: the state's
     * bag <b>is</b> its configuration; its consumers read behavior from the bag.
     *
     * @param state  the catalog state under construction
     * @param config the JSON {@code config} block (null = all defaults)
     */
    public static void seed(State<?> state, NVGenericMap config) {
        if (config != null) {
            for (GetNameValue<?> gnv : config.values())
                state.getProperties().add(gnv);
        }
    }
}
