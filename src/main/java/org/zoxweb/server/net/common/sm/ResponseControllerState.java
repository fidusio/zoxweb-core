package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.fsm.State;
import org.zoxweb.server.fsm.TriggerConsumer;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Catalog state {@code responder} (META-SM-PROTO-DESIGN.md §5): the machine's single writer.
 * Consumes {@link CommonTrigger#OUT_MESSAGE} (decoded bytes, {@code ${var}}s already resolved by
 * the controller) and writes them to the session via
 * {@link ClientSessionContext#write(ByteBuffer)} — over TCP through the session output stream
 * (plaintext before the upgrade, encrypted after {@code SECURE}), over UDP as one datagram.
 * <p>
 * Stateless by design: no configuration keys, no working memory. A write failure fails the
 * session ({@link ClientSessionContext#fail}), so teardown publishes {@code CLOSED} with the
 * cause.
 */
public class ResponseControllerState extends State<Object> {

    public static final String NAME = "responder";

    public ResponseControllerState() {
        super(NAME);
        register(new OutMessage());
    }

    private class OutMessage extends TriggerConsumer<byte[]> {

        OutMessage() {
            super(CommonTrigger.OUT_MESSAGE);
        }

        @Override
        public void accept(byte[] message) {
            ClientSessionContext ctx = (ClientSessionContext) getStateMachine().getConfig();
            try {
                ctx.write(ByteBuffer.wrap(message));
            } catch (IOException e) {
                ctx.fail(e);
            }
        }
    }
}
