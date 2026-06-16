package org.zoxweb.shared.security;

import org.zoxweb.shared.filters.ValueFilter;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.GetValue;

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

    public final static class SubjectIDFilter
            implements ValueFilter<String, String> {
        public static final SubjectIDFilter SINGLETON = new SubjectIDFilter();

        private SubjectIDFilter() {
        }

        /**
         * Validate the object
         *
         * @param in value to be validated
         * @return validated acceptable value
         * @throws NullPointerException     if in is null
         * @throws IllegalArgumentException if in is invalid
         */
        @Override
        public String validate(String in) throws NullPointerException, IllegalArgumentException {
            return in;
        }

        /**
         * Check if the value is valid
         *
         * @param in value to be checked
         * @return true if valid false  not
         */
        @Override
        public boolean isValid(String in) {
            return true;
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
    }
}
