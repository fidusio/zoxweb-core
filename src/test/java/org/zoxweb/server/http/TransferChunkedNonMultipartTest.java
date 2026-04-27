package org.zoxweb.server.http;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.io.UByteArrayInputStream;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.shared.http.HTTPConst;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.util.NamedValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests TRANSFER_CHUNKED's non-multipart "content" emission under the NIO
 * sync-drain runtime contract: each tick = one socket read appended to ubaos,
 * one parse() call, the handler drains and closes the stream before the next
 * tick.
 */
public class TransferChunkedNonMultipartTest {

    private static final String CHUNKED_JSON_HEADERS =
            "POST /upload HTTP/1.1\r\n" +
            "Transfer-Encoding: chunked\r\n" +
            "Content-Type: application/json\r\n" +
            "\r\n";

    private HTTPRawMessage hrm;

    private void setUpChunkedRequest() {
        hrm = new HTTPRawMessage();
        hrm.getDataStream().write(CHUNKED_JSON_HEADERS.getBytes(StandardCharsets.UTF_8), 0,
                CHUNKED_JSON_HEADERS.length());
        hrm.parse();
        assertTrue(hrm.areHeadersParsed());
        assertTrue(hrm.getHTTPMessageConfig().isTransferChunked());
        assertFalse(hrm.getHTTPMessageConfig().isMultiPartEncoding());
    }

    private HTTPMessageConfigInterface tick(String bodyBytes) {
        byte[] b = bodyBytes.getBytes(StandardCharsets.UTF_8);
        hrm.getDataStream().write(b, 0, b.length);
        return hrm.parse();
    }

    @SuppressWarnings("unchecked")
    private NamedValue<InputStream> contentEntry() {
        return (NamedValue<InputStream>) hrm.getHTTPMessageConfig()
                .attachment().getNV(HTTPConst.Token.CONTENT);
    }

    private static long lengthOf(NamedValue<InputStream> body) {
        return (Long) body.getProperties().getValue("length");
    }

    private static boolean isCompleted(NamedValue<InputStream> body) {
        return (Boolean) body.getProperties().getValue(HTTPConst.Token.IS_COMPLETED);
    }

    /**
     * Drain the current "content" stream into a String and close it so the
     * close-callback compacts the buffer.
     */
    private String drainAndClose() throws IOException {
        NamedValue<InputStream> body = contentEntry();
        assertNotNull(body, "no content entry to drain");
        InputStream is = body.getValue();
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        IOUtil.relayStreams(is, sink, true);   // closes is, fires close-callback
        return new String(sink.toByteArray(), StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------------------
    // Test 1 — whole body + terminator in one tick
    // ---------------------------------------------------------------------
    @Test
    public void wholeBodyAndTerminatorInOneTick() throws IOException {
        setUpChunkedRequest();

        tick("5\r\nhello\r\n5\r\nworld\r\n0\r\n\r\n");

        NamedValue<InputStream> body = contentEntry();
        assertNotNull(body);
        assertEquals(10L, lengthOf(body));
        assertTrue(isCompleted(body));
        assertTrue(hrm.isEndOfChunkedContentReached());

        assertEquals("helloworld", drainAndClose());

        // close-callback collapsed buffer, removed entry
        assertEquals(0, hrm.getDataStream().size());
        assertNull(contentEntry());
    }

    // ---------------------------------------------------------------------
    // Test 2 — one chunk per tick, terminator in its own tick (3 ticks)
    // ---------------------------------------------------------------------
    @Test
    public void chunksAcrossThreeTicksTerminatorAlone() throws IOException {
        setUpChunkedRequest();

        // tick 1 — "hello"
        tick("5\r\nhello\r\n");
        NamedValue<InputStream> b1 = contentEntry();
        assertNotNull(b1);
        assertEquals(5L, lengthOf(b1));
        assertFalse(isCompleted(b1));
        assertEquals("hello", drainAndClose());
        assertEquals(0, hrm.getDataStream().size());
        assertNull(contentEntry());

        // tick 2 — "world"
        tick("5\r\nworld\r\n");
        NamedValue<InputStream> b2 = contentEntry();
        assertNotNull(b2);
        assertEquals(5L, lengthOf(b2));
        assertFalse(isCompleted(b2));
        assertEquals("world", drainAndClose());
        assertEquals(0, hrm.getDataStream().size());
        assertNull(contentEntry());

        // tick 3 — terminator only
        tick("0\r\n\r\n");
        NamedValue<InputStream> b3 = contentEntry();
        assertNotNull(b3, "terminator-only tick must emit a zero-length completion stream");
        assertEquals(0L, lengthOf(b3));
        assertTrue(isCompleted(b3));
        assertTrue(hrm.isEndOfChunkedContentReached());

        assertEquals("", drainAndClose());
        assertNull(contentEntry());
    }

    // ---------------------------------------------------------------------
    // Test 3 — last data chunk + terminator in same tick
    // ---------------------------------------------------------------------
    @Test
    public void lastChunkAndTerminatorInSameTick() throws IOException {
        setUpChunkedRequest();

        tick("5\r\nhello\r\n");
        assertEquals("hello", drainAndClose());

        tick("5\r\nworld\r\n0\r\n\r\n");
        NamedValue<InputStream> b = contentEntry();
        assertNotNull(b);
        assertEquals(5L, lengthOf(b));
        assertTrue(isCompleted(b), "terminator in same tick must set IS_COMPLETED");
        assertTrue(hrm.isEndOfChunkedContentReached());

        assertEquals("world", drainAndClose());
    }

    // ---------------------------------------------------------------------
    // Test 4 — hex-size split across ticks (no CRLF in tick 1)
    // ---------------------------------------------------------------------
    @Test
    public void hexSizeSplitAcrossTicks() throws IOException {
        setUpChunkedRequest();

        // tick 1 — only the hex digit, no CRLF
        tick("5");
        assertNull(contentEntry(), "no emission while hex-size CRLF is missing");

        // tick 2 — rest of hex line + body + terminator
        tick("\r\nhello\r\n0\r\n\r\n");
        NamedValue<InputStream> b = contentEntry();
        assertNotNull(b);
        assertEquals(5L, lengthOf(b));
        assertTrue(isCompleted(b));
        assertEquals("hello", drainAndClose());
        assertTrue(hrm.isEndOfChunkedContentReached());
    }

    // ---------------------------------------------------------------------
    // Test 5 — chunk body split across ticks
    // ---------------------------------------------------------------------
    @Test
    public void chunkBodySplitAcrossTicks() throws IOException {
        setUpChunkedRequest();

        // tick 1 — declares 10-byte chunk (hex A) but only sends 5 bytes
        tick("A\r\n01234");
        assertNull(contentEntry(), "no emission until the full chunk + trailing CRLF arrive");

        // tick 2 — remaining body bytes + trailing CRLF + terminator
        tick("56789\r\n0\r\n\r\n");
        NamedValue<InputStream> b = contentEntry();
        assertNotNull(b);
        assertEquals(10L, lengthOf(b));
        assertTrue(isCompleted(b));
        assertEquals("0123456789", drainAndClose());
        assertTrue(hrm.isEndOfChunkedContentReached());
    }

    // ---------------------------------------------------------------------
    // Test 6 — multiple chunks unwrapped in one tick, terminator next tick
    // ---------------------------------------------------------------------
    @Test
    public void multipleChunksOneTickThenTerminator() throws IOException {
        setUpChunkedRequest();

        tick("5\r\nhello\r\n5\r\nworld\r\n");
        NamedValue<InputStream> b = contentEntry();
        assertNotNull(b);
        assertEquals(10L, lengthOf(b));
        assertFalse(isCompleted(b));
        assertEquals("helloworld", drainAndClose());

        tick("0\r\n\r\n");
        NamedValue<InputStream> bEnd = contentEntry();
        assertNotNull(bEnd);
        assertEquals(0L, lengthOf(bEnd));
        assertTrue(isCompleted(bEnd));
        assertEquals("", drainAndClose());
        assertTrue(hrm.isEndOfChunkedContentReached());
    }

    // ---------------------------------------------------------------------
    // Test 7 — empty body (terminator only, single tick)
    // ---------------------------------------------------------------------
    @Test
    public void emptyBodyTerminatorOnly() throws IOException {
        setUpChunkedRequest();

        tick("0\r\n\r\n");
        NamedValue<InputStream> b = contentEntry();
        assertNotNull(b);
        assertEquals(0L, lengthOf(b));
        assertTrue(isCompleted(b));
        assertEquals("", drainAndClose());
        assertTrue(hrm.isEndOfChunkedContentReached());
    }

    // ---------------------------------------------------------------------
    // Test 9 — slice non-overlap when handler does NOT close between ticks
    // (Pins the line 359 setLastProcessedDataIndex(dataMark) invariant.
    //  This is NOT the contract callers should rely on, but the slicing
    //  must still be correct so a future bug here gets caught.)
    // ---------------------------------------------------------------------
    @Test
    public void sliceNonOverlapWithoutCloseBetweenTicks() throws IOException {
        setUpChunkedRequest();

        tick("5\r\nhello\r\n");
        NamedValue<InputStream> b1 = contentEntry();
        assertNotNull(b1);
        assertEquals(5L, lengthOf(b1));
        // intentionally do NOT close b1's stream

        tick("5\r\nworld\r\n");
        NamedValue<InputStream> b2 = contentEntry();
        assertNotNull(b2);
        assertNotSame(b1, b2, "second tick must replace the attachment entry");
        assertEquals(5L, lengthOf(b2),
                "second slice must cover only the new bytes, not [0, dataMark)");

        // both streams point into the same live buffer; the second slice
        // sits at an offset past the first
        assertEquals("hello", readAll(b1.getValue()));
        assertEquals("world", readAll(b2.getValue()));
    }

    // ---------------------------------------------------------------------
    // Test 10 — buffer compaction after close
    // ---------------------------------------------------------------------
    @Test
    public void bufferCompactionAfterClose() throws IOException {
        setUpChunkedRequest();

        // chunk "hello" plus the start of a second chunk's hex (no CRLF) —
        // forces the loop to break with an unparsed tail
        tick("5\r\nhello\r\n5");
        NamedValue<InputStream> b = contentEntry();
        assertNotNull(b);
        assertEquals(5L, lengthOf(b));
        assertEquals("hello", drainAndClose());

        // after the close-callback fired: the consumed body was shifted
        // out, only the unparsed "5" remains
        UByteArrayOutputStream tail = hrm.getDataStream();
        assertEquals(1, tail.size(), "only the unparsed chunk-size byte should remain");
        assertEquals((byte) '5', tail.byteAt(0));
        assertNull(contentEntry());

        // and the next tick can resume parsing from position 0
        tick("\r\nworld\r\n0\r\n\r\n");
        NamedValue<InputStream> b2 = contentEntry();
        assertNotNull(b2);
        assertEquals(5L, lengthOf(b2));
        assertTrue(isCompleted(b2));
        assertEquals("world", drainAndClose());
        assertTrue(hrm.isEndOfChunkedContentReached());
    }

    // ---------------------------------------------------------------------
    private static String readAll(InputStream is) throws IOException {
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        byte[] buf = new byte[256];
        int n;
        while ((n = is.read(buf)) != -1) sink.write(buf, 0, n);
        return new String(sink.toByteArray(), StandardCharsets.UTF_8);
    }
}
