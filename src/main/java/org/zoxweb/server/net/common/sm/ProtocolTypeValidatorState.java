package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.fsm.State;
import org.zoxweb.server.fsm.TriggerConsumer;
import org.zoxweb.shared.util.NVBoolean;
import org.zoxweb.shared.util.NVGenericMap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Catalog state {@code validator} (META-SM-PROTO-DESIGN.md §9): consumes
 * {@link ClientEvent#VALIDATE} — the current message plus the {@code validate} match meta —
 * applies the match and writes the verdict into the machine's results bag
 * ({@link SMProtoUtil#results}). There is no event back ({@code VALIDATED} was rejected by
 * design): what happens after a verdict is the controller's decision or end-of-life.
 * <ul>
 * <li>{@code prefix} / {@code contains} / {@code exact} — any combination of
 * {@link SMProtoUtil} literals ({@code ${var}}s resolved at validate time), matched
 * byte-level against the message.</li>
 * <li>{@code report} — optional results key: on success, stores the matched message text
 * (UTF-8) under it (e.g. {@code report: "banner"} → {@code results.banner}).</li>
 * <li>Verdict: {@code validated=true}, or {@code validated=false} + {@code reason} — the
 * report is complete on both the pass and the fail path. A false verdict also fails the
 * session, so {@code CLOSED} carries the cause and the report agrees with
 * {@code Params.EXCEPTION}.</li>
 * </ul>
 * Stateless: match meta travels in the {@link Validation} payload; the verdict goes to the
 * machine results bag.
 */
public class ProtocolTypeValidatorState extends State<Object> {

    public static final String NAME = "validator";

    /**
     * The {@link ClientEvent#VALIDATE} payload: the current message plus the {@code validate}
     * step's match meta.
     */
    public static final class Validation {
        public final byte[] message;
        public final NVGenericMap meta;

        public Validation(byte[] message, NVGenericMap meta) {
            this.message = message;
            this.meta = meta;
        }
    }

    public ProtocolTypeValidatorState() {
        super(NAME);
        register(new Validate());
    }

    private class Validate extends TriggerConsumer<Validation> {

        Validate() {
            super(ClientEvent.VALIDATE);
        }

        @Override
        public void accept(Validation validation) {
            ClientSessionContext ctx = (ClientSessionContext) getStateMachine().getConfig();
            NVGenericMap results = SMProtoUtil.results(ctx.getStateMachine());
            NVGenericMap meta = validation.meta;
            byte[] message = validation.message;

            String reason = null;
            byte[] prefix = literal(meta, "prefix", ctx);
            if (prefix != null && !startsWith(message, prefix))
                reason = "prefix mismatch";
            byte[] contains = reason == null ? literal(meta, "contains", ctx) : null;
            if (contains != null && SMProtoUtil.indexOf(message, contains) < 0)
                reason = "does not contain expected sequence";
            byte[] exact = reason == null ? literal(meta, "exact", ctx) : null;
            if (exact != null && !Arrays.equals(message, exact))
                reason = "exact mismatch";

            if (reason == null) {
                results.build(new NVBoolean("validated", true));
                String report = SMProtoUtil.stringValue(meta, "report", null);
                if (report != null)
                    results.build(report, new String(message, StandardCharsets.UTF_8));
            } else {
                reason = "validation failed: " + reason + ": "
                        + new String(message, StandardCharsets.UTF_8);
                results.build(new NVBoolean("validated", false)).build("reason", reason);
                ctx.fail(new IOException(reason));
            }
        }

        /** Decodes a match literal from the meta, {@code ${var}}s resolved; null when absent. */
        private byte[] literal(NVGenericMap meta, String key, ClientSessionContext ctx) {
            String raw = SMProtoUtil.stringValue(meta, key, null);
            if (raw == null)
                return null;
            return SMProtoUtil.hasVars(raw)
                    ? SMProtoUtil.STRING_VARS_TO_DATA.decode(raw, ctx.getVars())
                    : SMProtoUtil.STRING_TO_DATA.decode(raw);
        }

        private boolean startsWith(byte[] message, byte[] prefix) {
            if (message.length < prefix.length)
                return false;
            for (int i = 0; i < prefix.length; i++) {
                if (message[i] != prefix[i])
                    return false;
            }
            return true;
        }
    }
}
