package org.zoxweb.shared.security;

import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.filters.ValueFilter;
import org.zoxweb.shared.http.HTTPMethod;
import org.zoxweb.shared.util.*;

public final class SecConst {
    private  SecConst() {
    }


    public enum AuthenticationType
            implements GetName {
        ALL("All"),
        API_KEY("ApiKey"), // custom authentication like opaque key SSWS etc
        BASIC("Basic"),
        BEARER("Bearer"),
        DIGEST("Digest"),
        DOMAIN("Domain"),
        JWT("JWT"),
        LDAP("LDAP"),
        HOBA("HOBA"),
        NONE("None"),
        OAUTH("OAuth"),
        //SSWS("SSWS"),
        ;

        private final String name;

        AuthenticationType(String val) {
            name = val;
        }

        @Override
        public String getName() {
            // TODO Auto-generated method stub
            return name;
        }
    }

    /**
     * This enum contains user status with a specified status
     * expiration time.
     */
    public enum SecStatus
            implements GetValue<Long> {
        // Note:
        //	0 = no expiration time
        // -1 = expiration time is irrelevant
        ACTIVE(0),
        DEACTIVATED(0),
        INACTIVE(-1),
        PENDING_RESET_PASSWORD(Const.TimeInMillis.DAY.MILLIS * 2),
        PENDING_ACCOUNT_ACTIVATION(Const.TimeInMillis.DAY.MILLIS * 2),
        PENDING_VALIDATION(0);

        private final long EXPIRATION_TIME;

        SecStatus(long time) {
            EXPIRATION_TIME = time;
        }

        @Override
        public Long getValue() {
            return EXPIRATION_TIME;
        }
    }

    public enum SecAction
    {
        ALLOW,
        DENY,
        REJECT
    }

    public enum SystemURI
            implements GetValue<String> {
        REGISTER(HTTPMethod.POST, "register"),
        DEREGISTER(HTTPMethod.POST, "deregister"),
        VALIDATE_ACCESS_CODE(HTTPMethod.POST, "validate-access-code"),
        GENERATE_ACCESS_CODE(HTTPMethod.POST, "generate-access-code"),
        ;

        private final HTTPMethod method;
        private final String value;

        SystemURI(HTTPMethod method, String value) {
            this.method = method;
            this.value = value;
        }

        /* (non-Javadoc)
         * @see org.zoxweb.shared.util.GetValue#getValue()
         */
        @Override
        public String getValue() {
            return value;
        }

        public final HTTPMethod getHTTPMethod() {
            return method;
        }

    }

    /**
     * Normalizes and validates a subject / principal identifier (login handle, email, ...).
     * <p>
     * {@link #validate(String)} trims the surrounding whitespace, rejects any remaining invisible character
     * (ASCII controls, DEL, C1 controls, every Unicode whitespace, zero width and bidi format characters,
     * byte order mark, ...), lowercases the value and finally enforces {@link #MIN_LENGTH}. The returned value is
     * the normalized identifier that must be stored and compared.
     * </p>
     */
    public final static class SubjectIDFilter
            implements ValueFilter<String, String> {
        public static final SubjectIDFilter SINGLETON = new SubjectIDFilter();

        /**
         * Minimum length of a normalized identifier.
         */
        public static final int MIN_LENGTH = 8;

        private SubjectIDFilter() {
        }

        /**
         * Validate and normalize the identifier.
         * <p>
         * A syntactically valid email address (per {@link FilterType#EMAIL}) is accepted as is, trimmed and
         * lowercased, whatever its length. Any other value is trimmed, lowercased, must be at least
         * {@link #MIN_LENGTH} characters long and must not contain an invisible character
         * (see {@link #isInvisible(char)}).
         * </p>
         *
         * @param in value to be validated
         * @return the trimmed, lowercased identifier
         * @throws NullPointerException     if in is null or blank
         * @throws IllegalArgumentException if in is not an email and is shorter than {@link #MIN_LENGTH} or
         *                                  contains an invisible character
         */
        @Override
        public String validate(String in) throws NullPointerException, IllegalArgumentException {
            in = SUS.trimOrNull(in);
            SUS.checkIfNull("value null", in);
            try {
                // a syntactically valid email is already trimmed and lowercased by the email filter
                return FilterType.EMAIL.validate(in);
            } catch (IllegalArgumentException e) {
                // not an email: fall through to the plain identifier rules
            }
            String ret = DataEncoder.StringLower.encode(in);
            if (ret.length() < MIN_LENGTH) {
                throw new IllegalArgumentException("value length " + ret.length() + " is less than the minimum " + MIN_LENGTH);
            }

            for (int i = 0; i < ret.length(); i++) {
                char c = ret.charAt(i);
                if (isInvisible(c)) {
                    throw new IllegalArgumentException("value contains invisible character U+"
                            + Integer.toHexString(0x10000 | c).substring(1).toUpperCase() + " at index " + i);
                }
            }


            return ret;
        }



        /**
         * Converts the implementing object in its canonical form.
         *
         * @return text identification of the object
         */
        @Override
        public String toCanonicalID() {
            return "SUBJECT_ID_FILTER";
        }

        /**
         * Tells whether a character renders as nothing (or as blank) and therefore has no place inside an
         * identifier: ASCII controls and space, DEL, C1 controls, no-break space, every Java whitespace,
         * Unicode space separators, zero width / joiner / bidi / word joiner format characters, the byte order
         * mark, the soft hyphen and the Hangul fillers.
         *
         * @param c character to test
         * @return true if c is invisible
         */
        public static boolean isInvisible(char c) {
            if (c <= 0x20 || (c >= 0x7F && c <= 0xA0) || Character.isWhitespace(c)) {
                return true;
            }
            if ((c >= 0x2000 && c <= 0x200F) || (c >= 0x2028 && c <= 0x202F)
                    || (c >= 0x205F && c <= 0x2064) || (c >= 0x2066 && c <= 0x206F)) {
                return true;
            }
            switch (c) {
                case 0x00AD: // soft hyphen
                case 0x034F: // combining grapheme joiner
                case 0x061C: // arabic letter mark
                case 0x115F: // hangul choseong filler
                case 0x1160: // hangul jungseong filler
                case 0x1680: // ogham space mark
                case 0x180E: // mongolian vowel separator
                case 0x3000: // ideographic space
                case 0x3164: // hangul filler
                case 0xFEFF: // zero width no-break space / BOM
                case 0xFFA0: // halfwidth hangul filler
                    return true;
                default:
                    return false;
            }
        }
    }
}
