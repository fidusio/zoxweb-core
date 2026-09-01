package org.zoxweb.server.net.protocols;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.util.NVGenericMap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The engine, hermetic — no sockets: compile fail-fast matrix, the four boundary strategies,
 * expect/skip semantics, validation verdicts, the STARTTLS seam, variable resolution, and the
 * completion rule (META-PROTOCOL.md §3).
 */
public class ExchangeScriptTest {

    /** Captures every host-seam interaction; {@code startTLS} leaves resumption to the test. */
    static final class FakeHost implements ExchangeScript.Host {
        final ByteArrayOutputStream wire = new ByteArrayOutputStream();
        Throwable failure;
        int completes;
        int startTLSCalls;

        @Override
        public void write(byte[] data) throws IOException {
            wire.write(data);
        }

        @Override
        public void startTLS() {
            startTLSCalls++;
        }

        @Override
        public void fail(Throwable cause) {
            if (failure == null)
                failure = cause;
        }

        @Override
        public void complete() {
            completes++;
        }

        String wireText() {
            return new String(wire.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static ExchangeScript script(String json, FakeHost host) {
        return new ExchangeScript(GSONUtil.fromJSONDefault(json, NVGenericMap.class), host);
    }

    private static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    // ---- compile fail-fast matrix ----

    @Test
    public void compileFailFast() {
        FakeHost h = new FakeHost();
        // unknown transport
        assertThrows(IllegalArgumentException.class, () -> script("{\"transport\": \"carrier-pigeon\"}", h));
        // unknown tls mode
        assertThrows(IllegalArgumentException.class, () -> script("{\"tls\": {\"mode\": \"maybe\"}}", h));
        // tls over udp
        assertThrows(IllegalArgumentException.class, () -> script("{\"transport\": \"udp\", \"tls\": {}}", h));
        // start_tls without a tls block
        assertThrows(IllegalArgumentException.class, () -> script("{\"exchange\": [{\"start_tls\": true}]}", h));
        // start_tls with immediate tls: session already secure
        assertThrows(IllegalArgumentException.class, () -> script(
                "{\"tls\": {\"mode\": \"immediate\"}, \"exchange\": [{\"start_tls\": true}]}", h));
        // retired sm-era keys are rejected
        assertThrows(IllegalArgumentException.class, () -> script("{\"protocol\": \"ssh\"}", h));
        assertThrows(IllegalArgumentException.class, () -> script("{\"ssh\": {\"banner_prefix\": \"SSH-\"}}", h));
        assertThrows(IllegalArgumentException.class, () -> script("{\"states\": []}", h));
        // malformed data literal fails at compile, never mid-session
        assertThrows(IllegalArgumentException.class, () -> script("{\"exchange\": [{\"expect\": \"hex:XYZ\"}]}", h));
        // unknown exchange op
        assertThrows(IllegalArgumentException.class, () -> script("{\"exchange\": [{\"transmogrify\": \"txt:x\"}]}", h));
        // unknown boundary
        assertThrows(IllegalArgumentException.class, () -> script("{\"assembler\": {\"boundary\": \"telepathic\"}}", h));
        // invalid length_prefixed size (1/2/3/4 are legal)
        assertThrows(IllegalArgumentException.class, () -> script(
                "{\"assembler\": {\"boundary\": \"length_prefixed\", \"length\": {\"size\": 5}}}", h));
        // boundary step over udp (one datagram = one message)
        assertThrows(IllegalArgumentException.class, () -> script(
                "{\"transport\": \"udp\", \"exchange\": [{\"boundary\": {\"boundary\": \"stream\"}}]}", h));
        // boundary step with an invalid framing block fails at compile
        assertThrows(IllegalArgumentException.class, () -> script(
                "{\"exchange\": [{\"boundary\": {\"boundary\": \"length_prefixed\", \"length\": {\"size\": 5}}}]}", h));
        // invalid extract size
        assertThrows(IllegalArgumentException.class, () -> script(
                "{\"exchange\": [{\"validate\": {\"extract\": {\"size\": 5}}}]}", h));
    }

    // ---- port hints: single value or well-known-port list ----

    @Test
    public void portHintsSingleAndList() {
        ExchangeScript single = script("{\"port\": 22}", new FakeHost());
        assertEquals(22, single.getPort());
        assertArrayEquals(new int[]{22}, single.getPorts());

        ExchangeScript multi = script("{\"port\": [25, 587]}", new FakeHost());
        assertEquals(25, multi.getPort(), "first declared port is the default hint");
        assertArrayEquals(new int[]{25, 587}, multi.getPorts());

        ExchangeScript none = script("{\"name\": \"no-port\"}", new FakeHost());
        assertEquals(-1, none.getPort());
        assertEquals(0, none.getPorts().length);

        // fail-fast: out-of-range and empty declarations
        assertThrows(IllegalArgumentException.class, () -> script("{\"port\": [25, 70000]}", new FakeHost()));
        assertThrows(IllegalArgumentException.class, () -> script("{\"port\": 0}", new FakeHost()));
        assertThrows(IllegalArgumentException.class, () -> script("{\"port\": []}", new FakeHost()));
    }

    // ---- shared assembler: host appends, parse() tokenizes via shiftLeft ----

    @Test
    public void sharedAssemblerParseTokenizes() {
        FakeHost h = new FakeHost();
        org.zoxweb.server.io.UByteArrayOutputStream assembler =
                new org.zoxweb.server.io.UByteArrayOutputStream(64);
        ExchangeScript s = new ExchangeScript(GSONUtil.fromJSONDefault(
                "{\"assembler\": {\"boundary\": \"delimited\"},"
                        + " \"exchange\": [{\"expect\": \"txt:250 \"}, {\"validate\": {\"contains\": \"txt:STARTTLS\"}}]}",
                NVGenericMap.class), h, assembler);
        s.start();
        // the host appends straight into the shared buffer — no chunk hand-off
        assembler.write(utf8("250-smtp.example\r\n250"));
        s.parse();
        assertFalse(s.isDone(), "partial line must wait");
        assembler.write(utf8(" STARTTLS\r\n"));
        s.parse();
        assertTrue(s.isDone());
        assertNull(h.failure);
        assertEquals(Boolean.TRUE, s.getResults().getValue("validated"));
        assertEquals(0, assembler.size(), "all tokens consumed off the assembler via shiftLeft");
    }

    // ---- mid-script boundary switch + extract ----

    @Test
    public void boundarySwitchAndExtractCaptureField() {
        FakeHost h = new FakeHost();
        // delimited line phase, then binary length-prefixed frames; the validate extracts the
        // length-prefixed ASCII field at offset 2 of the frame and matches/reports it
        ExchangeScript s = script(
                "{\"assembler\": {\"boundary\": \"delimited\"},"
                        + " \"exchange\": ["
                        + "  {\"expect\": \"txt:HELLO\"},"
                        + "  {\"send\": \"txt:GO\\r\\n\"},"
                        + "  {\"boundary\": {\"boundary\": \"length_prefixed\", \"length\": {\"offset\": 0, \"size\": 2}}},"
                        + "  {\"validate\": {\"extract\": {\"offset\": 2, \"size\": 2}, \"contains\": \"txt:alpha\", \"report\": \"algos\"}}"
                        + " ] }", h);
        s.start();
        assertEquals(ExchangeScript.Boundary.DELIMITED, s.getBoundary());
        s.feed(utf8("HELLO\r\n"));
        assertEquals("GO\r\n", h.wireText());
        assertEquals(ExchangeScript.Boundary.LENGTH_PREFIXED, s.getBoundary(), "boundary step switched framing");

        // frame: uint16 body length | uint16 field length | field bytes — split across feeds
        byte[] field = utf8("alpha,beta");
        byte[] frame = new byte[4 + field.length];
        frame[0] = 0;
        frame[1] = (byte) (2 + field.length); // body = field length header + field
        frame[2] = 0;
        frame[3] = (byte) field.length;
        System.arraycopy(field, 0, frame, 4, field.length);
        s.feed(Arrays.copyOfRange(frame, 0, 3));
        assertFalse(s.isDone(), "incomplete frame must wait");
        s.feed(Arrays.copyOfRange(frame, 3, frame.length));

        assertTrue(s.isDone());
        assertNull(h.failure);
        assertEquals(Boolean.TRUE, s.getResults().getValue("validated"));
        assertEquals("alpha,beta", s.getResults().getValue("algos"), "extracted field reported");
    }

    @Test
    public void extractOutOfBoundsFailsValidation() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script(
                "{\"assembler\": {\"boundary\": \"delimited\"},"
                        + " \"exchange\": [{\"validate\": {\"extract\": {\"offset\": 4, \"size\": 4}, \"report\": \"x\"}}]}", h);
        s.start();
        s.feed(utf8("ab\r\n")); // 2-byte framed message, extract header needs 8
        assertTrue(s.isFailed());
        assertEquals(Boolean.FALSE, s.getResults().getValue("validated"));
        assertTrue(((String) s.getResults().getValue("reason")).contains("out of bounds"));
    }

    @Test
    public void defaultsByTransport() {
        ExchangeScript tcp = script("{\"name\": \"t\"}", new FakeHost());
        assertEquals(ExchangeScript.Boundary.STREAM, tcp.getBoundary());
        assertFalse(tcp.isUDP());
        assertFalse(tcp.isCloseOnReady());
        assertEquals(ExchangeScript.TLSMode.NONE, tcp.getTLSMode());
        assertTrue(tcp.isCertValidation());
        assertEquals(-1, tcp.getPort());
        assertEquals(ExchangeScript.DEFAULT_TIMEOUT_SEC, tcp.getTimeoutSec());

        ExchangeScript udp = script("{\"transport\": \"udp\", \"port\": 53, \"timeout_sec\": 3}", new FakeHost());
        assertEquals(ExchangeScript.Boundary.DATAGRAM, udp.getBoundary());
        assertTrue(udp.isUDP());
        assertTrue(udp.isCloseOnReady(), "close_on_ready defaults true for udp");
        assertEquals(53, udp.getPort());
        assertEquals(3, udp.getTimeoutSec());
    }

    // ---- stream boundary ----

    @Test
    public void streamExchangeToCompletion() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script(
                "{\"exchange\": ["
                        // stream semantics: consume-through-match — expect through the full
                        // greeting line so no residue precedes the next reply
                        + " {\"expect\": \"txt:ready\\r\\n\"},"
                        + " {\"send\": \"txt:EHLO probe\\r\\n\"},"
                        + " {\"expect\": \"txt:250 ok\"},"
                        + " {\"validate\": {\"prefix\": \"txt:250\", \"report\": \"greeting\"}}"
                        + "]}", h);
        s.start();
        assertEquals("", h.wireText(), "first send waits for the greeting expect");

        // greeting arrives split across chunks — the match spans the seam
        s.feed(utf8("220 smtp.exam"));
        s.feed(utf8("ple ready\r\n"));
        assertEquals("EHLO probe\r\n", h.wireText(), "greeting matched, EHLO sent");
        assertFalse(s.isDone());

        s.feed(utf8("250 ok\r\n"));
        assertTrue(s.isDone());
        assertEquals(1, h.completes);
        assertNull(h.failure);
        assertEquals(Boolean.TRUE, s.getResults().getValue("validated"));
        assertEquals(Boolean.TRUE, s.getResults().getValue("ready"));
        assertEquals("250 ok", s.getResults().getValue("greeting"), "report stores through the match");
    }

    @Test
    public void streamMaxMessageBreachFails() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script(
                "{\"assembler\": {\"max_message\": 8}, \"exchange\": [{\"expect\": \"txt:NEVER\"}]}", h);
        s.start();
        s.feed(utf8("0123456789"));
        assertNotNull(h.failure);
        assertTrue(s.isFailed());
        assertEquals(Boolean.FALSE, s.getResults().getValue("validated"));
        assertEquals(0, h.completes);
    }

    // ---- delimited boundary ----

    @Test
    public void delimitedFramesAndSkipsToMatch() {
        FakeHost h = new FakeHost();
        // SMTP 250- continuation idiom: non-matching complete lines are skipped
        ExchangeScript s = script(
                "{\"assembler\": {\"boundary\": \"delimited\"},"
                        + " \"exchange\": [{\"expect\": \"txt:250 \"}, {\"validate\": {\"contains\": \"txt:STARTTLS\"}}]}", h);
        s.start();
        s.feed(utf8("250-smtp.example\r\n250-PIPELINING\r\n250 STARTTLS\r\n"));
        assertTrue(s.isDone());
        assertNull(h.failure);
        assertEquals(Boolean.TRUE, s.getResults().getValue("validated"));
    }

    @Test
    public void delimitedStripCR() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script(
                "{\"assembler\": {\"boundary\": \"delimited\", \"terminator\": \"txt:\\n\", \"strip_cr\": true},"
                        + " \"exchange\": [{\"expect\": \"txt:SSH-\"},"
                        + " {\"validate\": {\"exact\": \"txt:SSH-2.0-OpenSSH_9.6\", \"report\": \"banner\"}}]}", h);
        s.start();
        s.feed(utf8("SSH-2.0-OpenSSH_9.6\r\n"));
        assertTrue(s.isDone());
        assertEquals("SSH-2.0-OpenSSH_9.6", s.getResults().getValue("banner"), "trailing CR stripped before matching");
    }

    @Test
    public void framedMaxSkipBreachFails() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script(
                "{\"assembler\": {\"boundary\": \"delimited\", \"max_skip\": 10},"
                        + " \"exchange\": [{\"expect\": \"txt:NEVER\"}]}", h);
        s.start();
        s.feed(utf8("aaaaaa\r\nbbbbbb\r\n"));
        assertNotNull(h.failure);
        assertTrue(h.failure.getMessage().contains("max_skip"));
    }

    // ---- length_prefixed boundary ----

    @Test
    public void lengthPrefixedFrames() {
        FakeHost h = new FakeHost();
        // 2-byte big-endian length after a 1-byte type field; frame = 1 + 2 + payload
        ExchangeScript s = script(
                "{\"assembler\": {\"boundary\": \"length_prefixed\", \"length\": {\"offset\": 1, \"size\": 2}},"
                        + " \"exchange\": [{\"expect\": \"txt:PONG\"}]}", h);
        s.start();
        byte[] frame = new byte[]{0x01, 0x00, 0x04, 'P', 'O', 'N', 'G'};
        // split mid-header to prove cross-chunk assembly
        s.feed(new byte[]{frame[0], frame[1]});
        assertFalse(s.isDone());
        s.feed(new byte[]{frame[2], frame[3], frame[4], frame[5], frame[6]});
        assertTrue(s.isDone());
        assertNull(h.failure);
    }

    @Test
    public void lengthPrefixedLittleEndianAndAdjust() {
        FakeHost h = new FakeHost();
        // 2-byte little-endian length that counts the whole frame: offset 0, adjust -2
        ExchangeScript s = script(
                "{\"assembler\": {\"boundary\": \"length_prefixed\","
                        + " \"length\": {\"offset\": 0, \"size\": 2, \"endian\": \"little\", \"adjust\": -2}},"
                        + " \"exchange\": [{\"expect\": \"txt:OK\"}]}", h);
        s.start();
        s.feed(new byte[]{0x04, 0x00, 'O', 'K'}); // total frame length 4, little-endian
        assertTrue(s.isDone());
        assertNull(h.failure);
    }

    @Test
    public void lengthPrefixedOversizeFrameFails() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script(
                "{\"assembler\": {\"boundary\": \"length_prefixed\", \"max_message\": 16, \"length\": {\"size\": 2}},"
                        + " \"exchange\": [{\"expect\": \"txt:x\"}]}", h);
        s.start();
        s.feed(new byte[]{0x7F, (byte) 0xFF, 0});
        assertNotNull(h.failure);
    }

    // ---- datagram boundary ----

    @Test
    public void datagramExchange() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script(
                "{\"transport\": \"udp\","
                        + " \"exchange\": [{\"send\": \"txt:PING\"}, {\"expect\": \"txt:PONG\"},"
                        + " {\"validate\": {\"exact\": \"txt:PONG\"}}]}", h);
        s.start();
        assertEquals("PING", h.wireText(), "first datagram out on start");
        s.feed(utf8("PONG"));
        assertTrue(s.isDone());
        assertEquals(Boolean.TRUE, s.getResults().getValue("validated"));
    }

    @Test
    public void datagramOversizeFails() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script(
                "{\"transport\": \"udp\", \"assembler\": {\"max_message\": 4},"
                        + " \"exchange\": [{\"expect\": \"txt:x\"}]}", h);
        s.start();
        s.feed(utf8("toolarge"));
        assertNotNull(h.failure);
    }

    @Test
    public void threeByteLengthPrefixedFrames() {
        FakeHost h = new FakeHost();
        // the HTTP/2 frame-header shape: 3-byte big-endian payload length
        ExchangeScript s = script(
                "{\"assembler\": {\"boundary\": \"length_prefixed\", \"length\": {\"offset\": 0, \"size\": 3}},"
                        + " \"exchange\": [{\"expect\": \"txt:PONG\"}, {\"validate\": {\"contains\": \"txt:PONG\", \"report\": \"frame\"}}]}", h);
        s.start();
        s.feed(new byte[]{0x00, 0x00});                       // split header across chunks
        s.feed(new byte[]{0x04, 'P', 'O', 'N', 'G'});
        assertTrue(s.isDone());
        assertEquals(Boolean.TRUE, s.getResults().getValue("validated"));
    }

    @Test
    public void latencyRecordedOnCompletionAndFailure() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script("{\"exchange\": [{\"expect\": \"txt:OK\"}]}", h);
        s.start();
        s.feed(utf8("OK"));
        assertTrue(s.isDone());
        Object latency = s.getResults().getValue("latency_ms");
        assertTrue(latency instanceof Long && (Long) latency >= 0, "latency on completion: " + latency);

        FakeHost h2 = new FakeHost();
        ExchangeScript f = script(
                "{\"exchange\": [{\"expect\": \"txt:HELLO\"}, {\"validate\": {\"prefix\": \"txt:BYE\"}}]}", h2);
        f.start();
        f.feed(utf8("HELLO"));
        assertTrue(f.isFailed());
        Object failLatency = f.getResults().getValue("latency_ms");
        assertTrue(failLatency instanceof Long && (Long) failLatency >= 0, "latency on failure: " + failLatency);
    }

    // ---- validation verdicts ----

    @Test
    public void validateMismatchFailsSessionAndRecordsVerdict() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script(
                "{\"exchange\": [{\"expect\": \"txt:HELLO\"}, {\"validate\": {\"prefix\": \"txt:GOODBYE\"}}]}", h);
        s.start();
        s.feed(utf8("HELLO there"));
        assertTrue(s.isFailed());
        assertNotNull(h.failure);
        assertEquals(Boolean.FALSE, s.getResults().getValue("validated"));
        String reason = (String) s.getResults().getValue("reason");
        assertTrue(reason.contains("prefix mismatch"));
        assertEquals(0, h.completes, "a failed session never completes");
    }

    @Test
    public void validateIgnoreCaseMatchesAndReportsOriginal() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script(
                "{\"exchange\": [{\"expect\": \"txt:\\r\\n\\r\\n\"},"
                        + " {\"validate\": {\"contains\": \"txt:Server:\", \"ignore_case\": true, \"report\": \"headers\"}}]}", h);
        s.start();
        s.feed(utf8("HTTP/1.1 200 OK\r\nserver: nginx\r\n\r\n"));
        assertTrue(s.isDone());
        assertEquals(Boolean.TRUE, s.getResults().getValue("validated"));
        assertEquals("HTTP/1.1 200 OK\r\nserver: nginx\r\n\r\n", s.getResults().getValue("headers"));
    }

    @Test
    public void validateIgnoreCaseOffStaysByteExact() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script(
                "{\"exchange\": [{\"expect\": \"txt:\\r\\n\\r\\n\"},"
                        + " {\"validate\": {\"contains\": \"txt:Server:\"}}]}", h);
        s.start();
        s.feed(utf8("HTTP/1.1 200 OK\r\nserver: nginx\r\n\r\n"));
        assertTrue(s.isFailed());
        assertEquals(Boolean.FALSE, s.getResults().getValue("validated"));
    }

    @Test
    public void optionalValidateReportsSupportMatrixWithoutFailing() {
        FakeHost h = new FakeHost();
        // capability probe: V1 present, V2 absent — both reported, neither fails the run
        ExchangeScript s = script(
                "{\"exchange\": [{\"expect\": \"txt:\\r\\n\"},"
                        + " {\"validate\": {\"contains\": \"txt:V1\", \"optional\": true, \"report\": \"v1_supported\"}},"
                        + " {\"validate\": {\"contains\": \"txt:V2\", \"optional\": true, \"report\": \"v2_supported\"}}]}", h);
        s.start();
        s.feed(utf8("CAP V1\r\n"));
        assertTrue(s.isDone());
        assertEquals(Boolean.TRUE, s.getResults().getValue("v1_supported"));
        assertEquals(Boolean.FALSE, s.getResults().getValue("v2_supported"));
        // probes never touch the verdict: completion rule reports the run itself
        assertEquals(Boolean.TRUE, s.getResults().getValue("validated"));
        assertEquals("script completed", s.getResults().getValue("reason"));
        assertEquals(Boolean.TRUE, s.getResults().getValue("ready"));
        assertEquals(1, h.completes);
    }

    @Test
    public void optionalValidateNoneSupportedStillCompletes() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script(
                "{\"exchange\": [{\"expect\": \"txt:\\r\\n\"},"
                        + " {\"validate\": {\"contains\": \"txt:V1\", \"optional\": true, \"report\": \"v1_supported\"}},"
                        + " {\"validate\": {\"contains\": \"txt:V2\", \"optional\": true, \"report\": \"v2_supported\"}}]}", h);
        s.start();
        s.feed(utf8("CAP legacy\r\n"));
        assertTrue(s.isDone());
        assertEquals(Boolean.FALSE, s.getResults().getValue("v1_supported"));
        assertEquals(Boolean.FALSE, s.getResults().getValue("v2_supported"));
        assertEquals(Boolean.TRUE, s.getResults().getValue("validated"));
    }

    @Test
    public void optionalValidateWithoutReportIsCompileError() {
        FakeHost h = new FakeHost();
        assertThrows(IllegalArgumentException.class, () -> script(
                "{\"exchange\": [{\"validate\": {\"contains\": \"txt:V1\", \"optional\": true}}]}", h));
    }

    @Test
    public void completionRuleWithoutValidate() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script("{\"exchange\": [{\"send\": \"txt:LOG ping\\r\\n\"}]}", h);
        s.start();
        assertTrue(s.isDone());
        assertEquals(1, h.completes);
        assertEquals(Boolean.TRUE, s.getResults().getValue("validated"));
        assertEquals("script completed", s.getResults().getValue("reason"));
        assertEquals(Boolean.TRUE, s.getResults().getValue("ready"));
    }

    @Test
    public void emptyScriptCompletesOnStart() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script("{\"name\": \"bare\"}", h);
        s.start();
        assertTrue(s.isDone());
        assertEquals(Boolean.TRUE, s.getResults().getValue("validated"));
    }

    @Test
    public void recordFailureYieldsVerdictForExternalFailures() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script("{\"exchange\": [{\"expect\": \"txt:never\"}]}", h);
        s.start();
        s.recordFailure(new IOException("connection reset"));
        assertTrue(s.isFailed());
        assertEquals(Boolean.FALSE, s.getResults().getValue("validated"));
        assertEquals("connection reset", s.getResults().getValue("reason"));
    }

    // ---- the STARTTLS seam ----

    @Test
    public void startTLSPausesAndSecuredResumes() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script(
                "{\"tls\": {\"mode\": \"on_demand\", \"cert_validation\": false},"
                        + " \"exchange\": ["
                        + " {\"send\": \"txt:STARTTLS\\r\\n\"},"
                        // consume through the whole go-ahead line: stream residue past the
                        // match would (correctly) trip the injection check
                        + " {\"expect\": \"txt:220 go ahead\\r\\n\"},"
                        + " {\"start_tls\": true},"
                        + " {\"send\": \"txt:EHLO secure\\r\\n\"},"
                        + " {\"expect\": \"txt:250\"}"
                        + "]}", h);
        s.start();
        s.feed(utf8("220 go ahead\r\n"));
        assertEquals(1, h.startTLSCalls, "upgrade requested after the go-ahead");
        assertEquals("STARTTLS\r\n", h.wireText(), "post-upgrade sends held until secure");
        assertFalse(s.isDone());

        s.secured();
        assertEquals("STARTTLS\r\nEHLO secure\r\n", h.wireText(), "script resumed after the handshake");
        s.feed(utf8("250 hello\r\n"));
        assertTrue(s.isDone());
        assertNull(h.failure);
    }

    @Test
    public void startTLSResidueIsInjectionFailure() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script(
                "{\"tls\": {\"mode\": \"on_demand\", \"cert_validation\": false},"
                        + " \"exchange\": [{\"expect\": \"txt:220 go ahead\\r\\n\"}, {\"start_tls\": true}]}", h);
        s.start();
        // plaintext trailing the go-ahead — the classic STARTTLS injection shape
        s.feed(utf8("220 go ahead\r\nEVIL PIPELINED COMMAND\r\n"));
        assertNotNull(h.failure);
        assertTrue(h.failure.getMessage().contains("STARTTLS injection"));
        assertEquals(0, h.startTLSCalls, "no handshake on residue");
        assertEquals(Boolean.FALSE, s.getResults().getValue("validated"));
    }

    // ---- TLS immediate deferral ----

    @Test
    public void immediateModeStartsOnSecured() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script(
                "{\"tls\": {\"mode\": \"immediate\", \"cert_validation\": false},"
                        + " \"exchange\": [{\"send\": \"txt:hello\"}]}", h);
        assertEquals(ExchangeScript.TLSMode.IMMEDIATE, s.getTLSMode());
        assertFalse(s.isCertValidation());
        // the host never calls start() pre-handshake; secured() runs the deferred script
        s.secured();
        assertEquals("hello", h.wireText());
        assertTrue(s.isDone());
    }

    // ---- variables ----

    @Test
    public void varsResolveAtSendTimeWithCallerOverride() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script(
                "{\"vars\": {\"helo\": \"default.local\"},"
                        + " \"exchange\": [{\"send\": \"txt:EHLO ${helo}\\r\\n\"}]}", h);
        s.setVar("helo", "probe.example");
        s.start();
        assertEquals("EHLO probe.example\r\n", h.wireText());
    }

    @Test
    public void unresolvedVarFailsTheSessionAtSendTime() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script("{\"exchange\": [{\"send\": \"txt:EHLO ${missing}\\r\\n\"}]}", h);
        s.start();
        assertTrue(s.isFailed());
        assertNotNull(h.failure);
        assertTrue(h.failure.getMessage().contains("${missing}"));
    }

    // ---- guarded linear script: routing fail-fast matrix ----

    @Test
    public void compileFailFastRouting() {
        FakeHost h = new FakeHost();
        // duplicate label
        assertThrows(IllegalArgumentException.class, () -> script(
                "{\"exchange\": [{\"label\": \"a\"}, {\"label\": \"a\"}]}", h));
        // reserved label names
        assertThrows(IllegalArgumentException.class, () -> script("{\"exchange\": [{\"label\": \"done\"}]}", h));
        assertThrows(IllegalArgumentException.class, () -> script("{\"exchange\": [{\"label\": \"fail\"}]}", h));
        // empty label / non-string jump
        assertThrows(IllegalArgumentException.class, () -> script("{\"exchange\": [{\"label\": \"\"}]}", h));
        assertThrows(IllegalArgumentException.class, () -> script("{\"exchange\": [{\"jump\": 5}]}", h));
        // unknown route target
        assertThrows(IllegalArgumentException.class, () -> script("{\"exchange\": [{\"jump\": \"nowhere\"}]}", h));
        // backward/self route: forward-only, loops impossible by construction
        assertThrows(IllegalArgumentException.class, () -> script(
                "{\"exchange\": [{\"label\": \"a\"}, {\"jump\": \"a\"}]}", h));
        // expect block without a match literal
        assertThrows(IllegalArgumentException.class, () -> script(
                "{\"exchange\": [{\"expect\": {\"alt\": [{\"match\": \"txt:x\", \"goto\": \"done\"}]}}]}", h));
        // a '{'-leading expect that is not valid JSON needs the txt: prefix
        assertThrows(IllegalArgumentException.class, () -> script(
                "{\"exchange\": [{\"expect\": \"{notjson\"}]}", h));
        // empty alt list / alt entry missing goto
        assertThrows(IllegalArgumentException.class, () -> script(
                "{\"exchange\": [{\"expect\": {\"match\": \"txt:x\", \"alt\": []}}]}", h));
        assertThrows(IllegalArgumentException.class, () -> script(
                "{\"exchange\": [{\"expect\": {\"match\": \"txt:x\", \"alt\": [{\"match\": \"txt:y\"}]}}]}", h));
        // on_timeout is reserved for phase 2
        assertThrows(IllegalArgumentException.class, () -> script(
                "{\"exchange\": [{\"expect\": {\"match\": \"txt:x\", \"on_timeout\": \"done\"}}]}", h));
        // optional and on_mismatch conflict
        assertThrows(IllegalArgumentException.class, () -> script(
                "{\"exchange\": [{\"validate\": {\"contains\": \"txt:x\", \"optional\": true,"
                        + " \"report\": \"p\", \"on_mismatch\": \"done\"}}]}", h));
        // malformed regex / ${var} in regex
        assertThrows(IllegalArgumentException.class, () -> script(
                "{\"exchange\": [{\"validate\": {\"regex\": \"([\"}}]}", h));
        assertThrows(IllegalArgumentException.class, () -> script(
                "{\"exchange\": [{\"validate\": {\"regex\": \"${v}\\\\d+\"}}]}", h));
        // group without a regex / group out of range
        assertThrows(IllegalArgumentException.class, () -> script(
                "{\"exchange\": [{\"validate\": {\"contains\": \"txt:x\", \"group\": 1}}]}", h));
        assertThrows(IllegalArgumentException.class, () -> script(
                "{\"exchange\": [{\"validate\": {\"regex\": \"(a)\", \"group\": 2}}]}", h));
        // empty record block / reserved record key
        assertThrows(IllegalArgumentException.class, () -> script("{\"exchange\": [{\"record\": {}}]}", h));
        assertThrows(IllegalArgumentException.class, () -> script(
                "{\"exchange\": [{\"record\": {\"validated\": true}}]}", h));
    }

    // ---- guarded linear script: routing at run time ----

    @Test
    public void altRoutesStreamAndRecordsBranch() {
        String def = "{\"exchange\": ["
                + "{\"expect\": {\"match\": \"txt:220 \", \"alt\": [{\"match\": \"txt:421 \", \"goto\": \"refused\"}]}},"
                + "{\"expect\": \"txt:\\r\\n\"},"
                + "{\"send\": \"txt:MAIN\\r\\n\"},"
                + "{\"record\": {\"branch\": \"main\"}},"
                + "{\"jump\": \"done\"},"
                + "{\"label\": \"refused\"},"
                + "{\"expect\": \"txt:\\r\\n\"},"
                + "{\"record\": {\"branch\": \"refused\"}}]}";

        // alt path: refusal routes, main-path send never goes out, run still completes
        FakeHost h1 = new FakeHost();
        ExchangeScript s1 = script(def, h1);
        s1.start();
        s1.feed(utf8("421 busy\r\n"));
        assertTrue(s1.isDone());
        assertEquals("", h1.wireText(), "main-path send must not run on the alt route");
        assertEquals("refused", s1.getResults().getValue("branch"));
        assertEquals(Boolean.TRUE, s1.getResults().getValue("validated"));
        assertEquals("script completed", s1.getResults().getValue("reason"));

        // main path: jump done skips the alt tail
        FakeHost h2 = new FakeHost();
        ExchangeScript s2 = script(def, h2);
        s2.start();
        s2.feed(utf8("220 ok\r\n"));
        assertTrue(s2.isDone());
        assertEquals("MAIN\r\n", h2.wireText());
        assertEquals("main", s2.getResults().getValue("branch"));
    }

    @Test
    public void altMainWinsOverEarlierAltPosition() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script("{\"exchange\": ["
                + "{\"expect\": {\"match\": \"txt:B\", \"alt\": [{\"match\": \"txt:A\", \"goto\": \"alt_path\"}]}},"
                + "{\"record\": {\"path\": \"main\"}},"
                + "{\"jump\": \"done\"},"
                + "{\"label\": \"alt_path\"},"
                + "{\"record\": {\"path\": \"alt\"}}]}", h);
        s.start();
        // the alt token arrives first in the stream, but the main match has priority
        s.feed(utf8("AB"));
        assertTrue(s.isDone());
        assertEquals("main", s.getResults().getValue("path"));
    }

    @Test
    public void altRoutesFramedPreservesSkip() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script("{\"assembler\": {\"boundary\": \"delimited\"}, \"exchange\": ["
                + "{\"expect\": {\"match\": \"txt:250 \", \"alt\": [{\"match\": \"txt:421\", \"goto\": \"refused\"}]}},"
                + "{\"record\": {\"branch\": \"main\"}},"
                + "{\"jump\": \"done\"},"
                + "{\"label\": \"refused\"},"
                + "{\"record\": {\"branch\": \"refused\"}}]}", h);
        s.start();
        // continuation lines match neither the main pattern nor the alt — skipped as today
        s.feed(utf8("250-first\r\n250-second\r\n421 refused\r\n"));
        assertTrue(s.isDone());
        assertEquals("refused", s.getResults().getValue("branch"));
    }

    @Test
    public void framedMaxSkipStillEnforcedWithAlts() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script(
                "{\"assembler\": {\"boundary\": \"delimited\", \"max_skip\": 8}, \"exchange\": ["
                        + "{\"expect\": {\"match\": \"txt:NEVER\", \"alt\": [{\"match\": \"txt:ALSONEVER\", \"goto\": \"done\"}]}}]}", h);
        s.start();
        s.feed(utf8("junk one\r\njunk two\r\n"));
        assertTrue(s.isFailed());
        assertTrue(h.failure.getMessage().contains("max_skip"));
    }

    @Test
    public void altGotoFailFailsSession() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script("{\"exchange\": ["
                + "{\"expect\": {\"match\": \"txt:220 \", \"alt\": [{\"match\": \"txt:421\", \"goto\": \"fail\"}]}}]}", h);
        s.start();
        s.feed(utf8("421 go away\r\n"));
        assertTrue(s.isFailed());
        assertEquals(Boolean.FALSE, s.getResults().getValue("validated"));
        assertTrue(h.failure.getMessage().contains("routed to fail"));
    }

    @Test
    public void jumpToDoneCompletesEarly() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script("{\"exchange\": ["
                + "{\"send\": \"txt:A\"}, {\"jump\": \"done\"}, {\"send\": \"txt:B\"}]}", h);
        s.start();
        assertTrue(s.isDone());
        assertEquals("A", h.wireText(), "steps after a jump to done must not run");
        assertEquals(1, h.completes);
        assertEquals(Boolean.TRUE, s.getResults().getValue("validated"));
    }

    @Test
    public void onMismatchRoutesInsteadOfFailing() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script("{\"exchange\": ["
                + "{\"expect\": \"txt:\\r\\n\"},"
                + "{\"validate\": {\"contains\": \"txt:250\", \"on_mismatch\": \"fallback\", \"report\": \"smtp_reply\"}},"
                + "{\"record\": {\"proto\": \"smtp\"}},"
                + "{\"jump\": \"done\"},"
                + "{\"label\": \"fallback\"},"
                + "{\"validate\": {\"contains\": \"txt:HTTP\", \"report\": \"http_reply\"}}]}", h);
        s.start();
        s.feed(utf8("HTTP/1.1 200 OK\r\n"));
        assertTrue(s.isDone());
        assertNull(s.getResults().getValue("smtp_reply"), "mismatched validate must not report");
        assertNull(s.getResults().getValue("proto"));
        assertEquals("HTTP/1.1 200 OK\r\n", s.getResults().getValue("http_reply"),
                "the routed path examines the same current message");
        assertEquals(Boolean.TRUE, s.getResults().getValue("validated"));
        assertNull(h.failure);
    }

    @Test
    public void onMismatchToFailMatchesLegacyFailure() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script("{\"exchange\": ["
                + "{\"expect\": \"txt:\\r\\n\"},"
                + "{\"validate\": {\"contains\": \"txt:250\", \"on_mismatch\": \"fail\"}}]}", h);
        s.start();
        s.feed(utf8("HTTP/1.1 200 OK\r\n"));
        assertTrue(s.isFailed());
        assertEquals(Boolean.FALSE, s.getResults().getValue("validated"));
        assertTrue(((String) s.getResults().getValue("reason")).startsWith("validation failed:"),
                "on_mismatch to fail keeps the legacy failure verdict");
    }

    @Test
    public void recordMergesConstantsAndCaseInsensitiveCollision() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script("{\"exchange\": ["
                + "{\"record\": {\"tag\": \"one\", \"flag\": true}},"
                + "{\"record\": {\"TAG\": \"two\"}}]}", h);
        s.start();
        assertTrue(s.isDone());
        assertEquals(Boolean.TRUE, s.getResults().getValue("flag"));
        // results keys are case-insensitive: TAG and tag collide, last writer wins
        assertEquals("two", s.getResults().getValue("tag"));
    }

    // ---- regex capture in validate ----

    @Test
    public void regexCaptureDefaultGroupOne() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script("{\"exchange\": ["
                + "{\"expect\": \"txt:\\r\\n\"},"
                + "{\"validate\": {\"regex\": \"SSH-2\\\\.0-([\\\\w.]+)\", \"report\": \"version\"}}]}", h);
        s.start();
        s.feed(utf8("SSH-2.0-OpenSSH_9.6\r\n"));
        assertTrue(s.isDone());
        assertEquals("OpenSSH_9.6", s.getResults().getValue("version"),
                "a pattern with groups reports group 1 by default");
        assertEquals(Boolean.TRUE, s.getResults().getValue("validated"));
    }

    @Test
    public void regexGroupZeroWholeMatchAndGrouplessDefault() {
        // explicit group 0 reports the whole match even when the pattern captures
        FakeHost h1 = new FakeHost();
        ExchangeScript s1 = script("{\"exchange\": ["
                + "{\"expect\": \"txt:\\r\\n\"},"
                + "{\"validate\": {\"regex\": \"HTTP/1\\\\.1 (\\\\d{3})\", \"group\": 0, \"report\": \"status_line\"}}]}", h1);
        s1.start();
        s1.feed(utf8("HTTP/1.1 200 OK\r\n"));
        assertEquals("HTTP/1.1 200", s1.getResults().getValue("status_line"));

        // a pattern without groups defaults to the whole match
        FakeHost h2 = new FakeHost();
        ExchangeScript s2 = script("{\"exchange\": ["
                + "{\"expect\": \"txt:\\r\\n\"},"
                + "{\"validate\": {\"regex\": \"\\\\d{3}\", \"report\": \"status\"}}]}", h2);
        s2.start();
        s2.feed(utf8("HTTP/1.1 200 OK\r\n"));
        assertEquals("200", s2.getResults().getValue("status"));
    }

    @Test
    public void regexMismatchFailsSession() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script("{\"exchange\": ["
                + "{\"expect\": \"txt:\\r\\n\"},"
                + "{\"validate\": {\"regex\": \"\\\\d{4}\", \"report\": \"code\"}}]}", h);
        s.start();
        s.feed(utf8("no digits here\r\n"));
        assertTrue(s.isFailed());
        assertEquals(Boolean.FALSE, s.getResults().getValue("validated"));
        assertTrue(((String) s.getResults().getValue("reason")).contains("regex mismatch"));
    }

    @Test
    public void regexWithOnMismatchRoutes() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script("{\"exchange\": ["
                + "{\"expect\": \"txt:\\r\\n\"},"
                + "{\"validate\": {\"regex\": \"\\\\d{4}\", \"report\": \"code\", \"on_mismatch\": \"nomatch\"}},"
                + "{\"jump\": \"done\"},"
                + "{\"label\": \"nomatch\"},"
                + "{\"record\": {\"code_found\": false}}]}", h);
        s.start();
        s.feed(utf8("no digits here\r\n"));
        assertTrue(s.isDone());
        assertEquals(Boolean.FALSE, s.getResults().getValue("code_found"));
        assertEquals(Boolean.TRUE, s.getResults().getValue("validated"));
        assertNull(h.failure);
    }

    @Test
    public void regexAfterExtract() {
        FakeHost h = new FakeHost();
        // datagram message: 1-byte length then the field; the regex sees the extracted field only
        ExchangeScript s = script("{\"transport\": \"udp\", \"exchange\": ["
                + "{\"validate\": {\"extract\": {\"offset\": 0, \"size\": 1},"
                + " \"regex\": \"([0-9]+)\", \"report\": \"build\"}}]}", h);
        s.start();
        byte[] datagram = new byte[6];
        datagram[0] = 5;
        System.arraycopy(utf8("ab123"), 0, datagram, 1, 5);
        s.feed(datagram);
        assertTrue(s.isDone());
        assertEquals("123", s.getResults().getValue("build"));
    }

    @Test
    public void regexBinarySafeHighBytes() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script("{\"transport\": \"udp\", \"exchange\": ["
                + "{\"validate\": {\"regex\": \"V(\\\\d)\", \"report\": \"proto_version\"}}]}", h);
        s.start();
        // bytes above 0x7F must not derail the match (ISO-8859-1 maps bytes 1:1)
        s.feed(new byte[]{0x01, (byte) 0xC3, (byte) 0x9F, 'V', '2', (byte) 0xFF});
        assertTrue(s.isDone());
        assertEquals("2", s.getResults().getValue("proto_version"));
    }

    @Test
    public void regexOptionalRecordsBooleanOnly() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script("{\"exchange\": ["
                + "{\"expect\": \"txt:\\r\\n\"},"
                + "{\"validate\": {\"regex\": \"(\\\\d{3})\", \"optional\": true, \"report\": \"has_status\"}}]}", h);
        s.start();
        s.feed(utf8("HTTP/1.1 200 OK\r\n"));
        assertTrue(s.isDone());
        // an optional probe keeps the boolean contract — never the capture
        assertEquals(Boolean.TRUE, s.getResults().getValue("has_status"));
    }

    // ---- STARTTLS residue on routed paths ----

    @Test
    public void startTLSResidueEnforcedOnRoutedPath() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script("{\"tls\": {\"mode\": \"on_demand\"}, \"exchange\": ["
                + "{\"expect\": {\"match\": \"txt:NEVER\\r\\n\", \"alt\": [{\"match\": \"txt:ALT \", \"goto\": \"upgrade\"}]}},"
                + "{\"label\": \"upgrade\"},"
                + "{\"start_tls\": true}]}", h);
        s.start();
        // the alt consumes only through its own match — the leftover line is injection residue
        s.feed(utf8("ALT go\r\n"));
        assertTrue(s.isFailed());
        assertTrue(h.failure.getMessage().contains("STARTTLS injection"));
        assertEquals(0, h.startTLSCalls);
    }

    @Test
    public void startTLSRoutedPathCleanWhenAltConsumesTheLine() {
        FakeHost h = new FakeHost();
        ExchangeScript s = script("{\"tls\": {\"mode\": \"on_demand\"}, \"exchange\": ["
                + "{\"expect\": {\"match\": \"txt:NEVER\\r\\n\", \"alt\": [{\"match\": \"txt:ALT go\\r\\n\", \"goto\": \"upgrade\"}]}},"
                + "{\"label\": \"upgrade\"},"
                + "{\"start_tls\": true}]}", h);
        s.start();
        s.feed(utf8("ALT go\r\n"));
        assertFalse(s.isFailed());
        assertEquals(1, h.startTLSCalls);
        s.secured();
        assertTrue(s.isDone());
    }

    // ---- expect block detection escape hatch ----

    @Test
    public void expectBraceLiteralNeedsTxtPrefix() {
        FakeHost h = new FakeHost();
        // with the txt: prefix, a '{'-leading byte literal stays a literal
        ExchangeScript s = script("{\"exchange\": [{\"expect\": \"txt:{\\\"status\\\"\"}]}", h);
        s.start();
        s.feed(utf8("{\"status\""));
        assertTrue(s.isDone());
    }
}
