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

//    public static void close(AutoCloseable... acs) {
//        if (acs != null) {
//            for (AutoCloseable c : acs) {
//                if (c != null) {
//                    try {
//                        c.close();
//                    } catch (Exception e) {
//                        // Intentionally suppressed - close() should not throw during cleanup
//                    }
//                }
//            }
//        }
//    }

//    /**
//     * @param c closable
//     * @return IOException or null if none generated
//     */
//    public static IOException close(Closeable c) {
//        if (c != null) {
//            try {
//                c.close();
//            } catch (IOException e) {
//                return e;
//            }
//        }
//
//        return null;
//    }

    /**
     * Close an AutoCloseable object if c is null the action is discarded, while closing catch any exception silently
     *
     * @param acs auto closable array
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
     * @param c closable
     * @return IOException or null if none generated
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
