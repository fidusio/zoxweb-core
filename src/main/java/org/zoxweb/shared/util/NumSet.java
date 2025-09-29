package org.zoxweb.shared.util;


public abstract class NumSet {

    public static final Base27 BASE_27 = new Base27();


    private NumSet() {
    }


    /**
     * Base 27 is numbering scheme to represent long value using alphanumeric set with removed character visual collision excluded.
     * The included set is: ABCDEFGHJKMNPQRTUVWXY3456789 with numeric values starting with A=0 and ending with 9=26
     * The excluded set is: 0O1IL2Z5S
     */
    static class Base27
            extends NumSet {

        private Base27() {
        }

        public static final byte[][] SETS = {
                {
                        //"abcdefghjkmnpqrtuvwxy3456789"
                        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 't', 'u', 'v', 'w', 'x', 'y',
                        '3', '4', '5', '6', '7', '8', '9'
                },
                {
                        //"ABCDEFGHJKMNPQRTUVWXY3456789"
                        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'M', 'N', 'P', 'Q', 'R', 'T', 'U', 'V', 'W', 'X', 'Y',
                        '3', '4', '5', '6', '7', '8', '9'
                }
        };


        public String toString(long... vals) {
            StringBuilder sb = new StringBuilder();
            for (long l : vals) {
                sb.append(toString(l, SETS, true, 14, true));
            }
            return sb.toString();
        }


        public long getLong(String str) {
            return getLong(str, SETS);
        }
    }


    protected static long getLong(String str, byte[][] sets) {
        int radix = sets[0].length;
        long ret = 0;
        byte[] strBytes = SharedStringUtil.getBytes(str);
        for (int i = 0; i < strBytes.length; i++) {
            byte b = strBytes[strBytes.length - 1 - i];
            int val = val(b, sets);
            ret += val * Math.pow(radix, i);
        }

        return ret;
    }


    private static int val(byte b, byte[][] sets) {
        for (int i = 0; i < sets[0].length; i++) {
            if (b == sets[0][i] || b == sets[1][i])
                return i;
        }
        throw new NumberFormatException();
    }

    public abstract String toString(long... val);

    protected static String toString(long val, byte[][] sets, boolean addLeadingZero, int maxLength, boolean absoluteValue) {
        int radix = sets[1].length;
        long result = absoluteValue ? Math.abs(val) : val;
        long rest;
        StringBuilder ret = new StringBuilder();
        do {
            rest = result % radix;
            result = result / radix;
            ret.insert(0, (char) sets[1][(int) rest]);
        }
        while (result != 0);


        if (addLeadingZero) {
            while (ret.length() < maxLength) {
                ret.insert(0, (char) sets[1][0]);
            }
        }

        return ret.toString();
    }
}
