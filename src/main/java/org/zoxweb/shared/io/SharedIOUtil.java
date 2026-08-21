package org.zoxweb.shared.io;

import org.zoxweb.shared.util.Const;

import java.io.Closeable;
import java.io.IOException;

public class SharedIOUtil {
    private SharedIOUtil() {}


    public static final int K_1 = Const.SizeInBytes.K.mult(1);
    public static final int K_2 = Const.SizeInBytes.K.mult(2);
    public static final int K_4 = Const.SizeInBytes.K.mult(4);
    public static final int K_8 = Const.SizeInBytes.K.mult(8);
    public static final int K_16 = Const.SizeInBytes.K.mult(16);
    public static final int K_32 = Const.SizeInBytes.K.mult(32);
    public static final int K_128 = Const.SizeInBytes.K.mult(128);
    public static final int SSL_BUFFER_SIZE = 16709;


    public enum IOType
    {
        // incoming data, event, trigger
        IN,
        // outgoing data, event, trigger
        OUT
    }


    /**
     * Null-tolerant, exception-swallowing bulk close — the cleanup/teardown idiom.
     * <p>
     * <b>Contract: this method NEVER throws, for any input.</b> A {@code null} array and
     * {@code null} entries are silently skipped; every non-null entry is closed in order;
     * any {@code Exception} thrown by an individual {@code close()} is suppressed and the
     * remaining entries are still closed. It is therefore always safe to call with
     * possibly-null references and from error-handling paths — e.g.
     * {@code SSLUtil._finished}'s catch relies on it to fully tear a session down even
     * when its callback is null.
     * </p>
     *
     * @param acs the closeables; the array itself or any entry may be null
     */
    public static void close(AutoCloseable... acs) {
        if (acs != null) {
            for (AutoCloseable c : acs) {
                if (c != null) {
                    try {
                        c.close();
                    } catch (Exception e) {
                        // Intentionally suppressed - close() should not throw during cleanup
                    }
                }
            }
        }
    }

    /**
     * Single-target close that reports instead of throwing: null is ignored, and an
     * {@code IOException} from {@code close()} is returned to the caller rather than
     * thrown. Like {@link #close(AutoCloseable...)}, this method never throws.
     *
     * @param c the closeable; may be null (ignored)
     * @return the IOException raised by {@code c.close()}, or null if none
     */
    public static IOException close(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                return e;
            }
        }

        return null;
    }
}
