package org.zoxweb.server.fsm;

import org.zoxweb.server.logging.LogWrapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link StateMachineListener} that keeps a bounded, ordered history of machine
 * activity as <b>log lines</b> for traceability.
 * <p>
 * <b>GC-safe by construction:</b> each event is converted to its {@link StateMachineEvent#toLog()}
 * string immediately in {@link #handleEvent} and the event object is never retained — so the
 * history cannot pin states (including deregistered ones), consumers, triggers, or payloads
 * against garbage collection. Memory is bounded: when the capacity is reached the oldest
 * line is evicted.
 * </p>
 * <pre>{@code
 * StateMachineEventHistory history = new StateMachineEventHistory(256);
 * sm.addListener(history);
 * ...
 * for (String line : history.history())   // oldest-first trace
 *     logger.info(line);
 * }</pre>
 */
public class StateMachineEventHistory
        implements StateMachineListener {

    public final static LogWrapper log = new LogWrapper(StateMachineEventHistory.class).setEnabled(false);

    public static final int DEFAULT_CAPACITY = 512;

    private final ArrayDeque<String> ring;
    private final int capacity;
    private volatile boolean liveLog = false;

    public StateMachineEventHistory() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * @param capacity maximum number of log lines retained; oldest evicted beyond it
     * @throws IllegalArgumentException if capacity < 1
     */
    public StateMachineEventHistory(int capacity) {
        if (capacity < 1)
            throw new IllegalArgumentException("capacity must be > 0: " + capacity);
        this.capacity = capacity;
        this.ring = new ArrayDeque<String>(capacity);
    }

    /**
     * Converts the event to its log line immediately and appends it to the bounded
     * history; the event object is NOT retained (see class contract).
     */
    @Override
    public void handleEvent(StateMachineEvent event) {
        String line = event.toLog();
        synchronized (ring) {
            if (ring.size() == capacity)
                ring.pollFirst();
            ring.addLast(line);
        }
        if (liveLog && log.isEnabled())
            log.getLogger().info(line);
    }

    /**
     * @return snapshot copy of the history log lines, oldest first
     */
    public List<String> history() {
        synchronized (ring) {
            return new ArrayList<String>(ring);
        }
    }

    /**
     * @param separator joined between lines (e.g. "\n")
     * @return the whole history as one traceability report
     */
    public String toLog(String separator) {
        List<String> snapshot = history();
        StringBuilder sb = new StringBuilder();
        for (String line : snapshot) {
            if (sb.length() > 0)
                sb.append(separator);
            sb.append(line);
        }
        return sb.toString();
    }

    public int size() {
        synchronized (ring) {
            return ring.size();
        }
    }

    public int capacity() {
        return capacity;
    }

    public void clear() {
        synchronized (ring) {
            ring.clear();
        }
    }

    /**
     * @param on if true, each line is also echoed to this class's LogWrapper as it arrives
     *           (the wrapper must be enabled)
     * @return this for chaining
     */
    public StateMachineEventHistory liveLog(boolean on) {
        this.liveLog = on;
        return this;
    }
}
