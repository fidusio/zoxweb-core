/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.zoxweb.shared.filters;

import org.zoxweb.shared.util.DataEncoder;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.SUS;

import java.math.BigDecimal;

/**
 * The filter type enum that implements ValueFilter which is has string set as 
 * both the input and output.
 * @author mzebib
 */
public enum FilterType
        implements ValueFilter<String, String> {

    /**
     * Binary (byte array) filter
     */
    BINARY {
        /**
         * Validates the given value.
         * @param in value to be validated
         * @return validated acceptable value
         * @throws NullPointerException if in is null
         * @throws IllegalArgumentException if in is invalid
         */
        public String validate(String in)
                throws NullPointerException, IllegalArgumentException {
            return in;
        }
    },
    /**
     * BigDecimal filter
     */
    BIG_DECIMAL {
        /**
         * Validates the given value.
         * @param in value to be validated
         * @return validated acceptable value
         * @throws NullPointerException if in is null
         * @throws IllegalArgumentException if in is invalid
         */
        public String validate(String in)
                throws NullPointerException, IllegalArgumentException {
            return "" + BigDecimalFilter.SINGLETON.validate(in);
        }
    },
    /**
     * Boolean filter
     */
    BOOLEAN {
        /**
         * Validates the given value.
         * @param in value to be validated
         * @return validated acceptable value
         * @throws NullPointerException if in is null
         * @throws IllegalArgumentException if in is invalid
         */
        public String validate(String in)
                throws NullPointerException, IllegalArgumentException {
            return "" + Boolean.valueOf(in);
        }
    },
    CLEAR {

    },
    DOMAIN {
        /**
         * Validates a bare hostname and returns it lower-cased with a leading {@code www}
         * label ({@code www.}, {@code www2.}) removed. The hostname is otherwise returned
         * as given: this filter never computes a registrable domain, never coerces a URL
         * or an email address into a domain, and does not restrict the top-level domain.
         * {@code www.zoxweb.com} and {@code zoxweb.com} are therefore the same value, while
         * {@code admin.zoxweb.com} is a different one; the row an operator registers is the
         * scope boundary, not this filter.
         * @param in value to be validated
         * @return validated acceptable value
         * @throws NullPointerException if in is null
         * @throws IllegalArgumentException if in is invalid
         */
        public String validate(String in)
                throws NullPointerException, IllegalArgumentException {
            String str = SUS.trimOrNull(in);
            SUS.checkIfNulls("Null or empty input.", str);
            str = str.toLowerCase();

            if (str.length() > DOMAIN_MAX_LENGTH || !str.matches(DOMAIN_REGEX)) {
                throw new IllegalArgumentException("Invalid domain: " + in);
            }

            // remove a leading "www." or "www<digits>." label only
            str = str.replaceFirst("^www\\d*\\.", "");

            // "www.com" would collapse to a single label
            if (str.indexOf('.') == -1) {
                throw new IllegalArgumentException("Invalid domain: " + in);
            }

            return str;
        }
    },

    /**
     * Double filter
     */
    DOUBLE {
        /**
         * Validates the given value.
         * @param in value to be validated
         * @return validated acceptable value
         * @throws NullPointerException if in is null
         * @throws IllegalArgumentException if in is invalid
         */
        public String validate(String in)
                throws NullPointerException, IllegalArgumentException {
            return "" + Double.valueOf(in);
        }
    },

    /**
     * Email filter
     */
    EMAIL {
        //public static final String REGEX ="^[_a-zA-Z0-9-]+(\\.[_a-zA-Z0-9-]+)*@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*\\.(([0-9]{1,3})|([a-zA-Z]{2,3})|(aero|coop|info|museum|name))$";
        //public static final String REGEX ="\\b[\\w.!#$%&’*+\\/=?^`{|}~-]+@[\\w-]+(?:\\.[\\w-]+)*\\b";
        // local part per RFC 5322 atext; the domain part is the same rule as FilterType.DOMAIN
        public static final String REGEX = "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:" + DOMAIN_LABEL + "\\.)+" + DOMAIN_TLD + "$";
        public static final int MAX_LENGTH = 254;

        /**
         * Validates the given value.
         * @param in value to be validated
         * @return validated acceptable value
         * @throws NullPointerException if in is null
         * @throws IllegalArgumentException if in is invalid
         */
        public String validate(String in)
                throws NullPointerException, IllegalArgumentException {
            in = SUS.trimOrNull(in);
            SUS.checkIfNulls("Email address null or empty", in);

            if (in.matches(REGEX)) {
                if (in.length() > MAX_LENGTH) {
                    throw new IllegalArgumentException("Email length > max length " + in.length() + ":" + in);
                }

                return in.toLowerCase();
            } else {
                throw new IllegalArgumentException("Invalid email: " + in);
            }
        }
    },


    /**
     * Encrypt filter
     */
    ENCRYPT,

    /**
     * Encrypt mask filter
     */
    ENCRYPT_MASK,

    /**
     * File filter
     */
    FILE,

    /**
     * Float filter
     */
    FLOAT {
        /**
         * Validates the given value.
         * @param in value to be validated
         * @return validated acceptable value
         * @throws NullPointerException if in is null
         * @throws IllegalArgumentException if in is invalid
         */
        public String validate(String in)
                throws NullPointerException, IllegalArgumentException {
            return "" + Float.valueOf(in);
        }
    },
    /**
     * Hashed filter
     */
    HASHED,

    HIDDEN {
        public String validate(String in)
                throws NullPointerException, IllegalArgumentException {
            return null;
        }

        public boolean isValid(String in) {
            return SUS.isEmpty(in);
        }
    },
    /**
     * Integer filter
     */
    INTEGER {
        /**
         * Validates the given value.
         * @param in value to be validated
         * @return validated acceptable value
         * @throws NullPointerException if in is null
         * @throws IllegalArgumentException if in is invalid
         */
        public String validate(String in)
                throws NullPointerException, IllegalArgumentException {
            return "" + Integer.valueOf(in);
        }
    },
    /**
     * Long filter
     */
    LONG {
        /**
         * Validates the given value.
         * @param in value to be validated
         * @return validated acceptable value
         * @throws NullPointerException if in is null
         * @throws IllegalArgumentException if in is invalid
         */
        public String validate(String in)
                throws NullPointerException, IllegalArgumentException {
            return "" + Long.valueOf(in);
        }
    },
    LOWERCASE {
        public String validate(String in)
                throws NullPointerException, IllegalArgumentException {
            return DataEncoder.StringLower.encode(in);
        }

        public boolean isValid(String in) {
            if (in != null)
                for (char c : in.toCharArray()) {
                    if (!Character.isLowerCase(c))
                        return false;
                }

            return true;
        }
    },
    /**
     * Password filter
     */
    PASSWORD {
        //public static final String REGEXP ="((?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%]).{6,20})";
        //public static final String REGEX ="((?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{8,64})";
        public static final String REGEX = "^(?=.*[A-Z])(?=.*\\d)(?=.*[\\p{P}\\p{S}])(?=.*[\\p{L}]).{8,}$";

        public static final int MIN_LENGTH = 8;

        /**
         * Validates the given value.
         * @param in value to be validated
         * @return validated acceptable value
         * @throws NullPointerException if in is null
         * @throws IllegalArgumentException if in is invalid
         */
        public String validate(String in)
                throws NullPointerException, IllegalArgumentException {
            in = SUS.trimOrNull(in);
            SUS.checkIfNulls("Password null or empty", in);

            if (in.matches(REGEX)) {
                return in;
            } else {
                throw new IllegalArgumentException("Invalid password did mot pass requirements length < 8, contains special characters");
            }
        }
    },
    /**
     * Prime number filter
     */
    PRIME {
        // /^1?$|^(11+?)\1+$/
        // prime number detector to be tested
        public static final String


                REGEX = "^1?$|^(11+?)\\1+$";


        /**
         * Validates the given value.
         * @param in value to be validated
         * @return validated acceptable value
         * @throws NullPointerException if in is null
         * @throws IllegalArgumentException if in is invalid
         */
        public String validate(String in)
                throws NullPointerException, IllegalArgumentException {
            in = SUS.trimOrNull(in);
            SUS.checkIfNulls("Password null or empty", in);

            if (in.matches(REGEX)) {
                return in;
            } else {
                throw new IllegalArgumentException("Invalid password: " + in);
            }
        }
    },
    TEXT_NOT_EMPTY {
        public String validate(String in)
                throws NullPointerException, IllegalArgumentException {
            in = SUS.trimOrNull(in);
            if (in == null) {
                throw new NullPointerException("Null or empty");
            }
            return in;
        }
    },
    UPPERCASE {
        public String validate(String in)
                throws NullPointerException, IllegalArgumentException {
            return DataEncoder.StringUpper.encode(in);
        }

        public boolean isValid(String in) {
            if (in != null)
                for (char c : in.toCharArray()) {
                    if (!Character.isUpperCase(c))
                        return false;
                }
            return true;
        }
    },
    /**
     * URL filter
     */
    URL {
        public static final String REGEX = "^(https?|wss?|ftp|file)://[-a-zA-Z0-9][-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
        //"^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
        //"(ftp|http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?"
        //"^(?:(?:https?|ftp)://)(?:\\S+(?::\\S*)?@)?(?:(?!10(?:\\.\\d{1,3}){3})(?!127(?:\\.\\d{1,3}){3})(?!169\\.254(?:\\.\\d{1,3}){2})(?!192\\.168(?:\\.\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}-\\x{ffff}0-9]+)(?:\\.(?:[a-z\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}-\\x{ffff}0-9]+)*(?:\\.(?:[a-z\\x{00a1}-\\x{ffff}]{2,})))(?::\\d{2,5})?(?:/[^\\s]*)?$_iuS";
        //"_^(?:(?:https?|ftp)://)(?:\\S+(?::\\S*)?@)?(?:(?!10(?:\\.\\d{1,3}){3})(?!127(?:\\.\\d{1,3}){3})(?!169\\.254(?:\\.\\d{1,3}){2})(?!192\\.168(?:\\.\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}-\\x{ffff}0-9]+)(?:\\.(?:[a-z\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}-\\x{ffff}0-9]+)*(?:\\.(?:[a-z\\x{00a1}-\\x{ffff}]{2,})))(?::\\d{2,5})?(?:/[^\\s]*)?$_iuS";
        public static final int MAX_LENGTH = 4096;

        /**
         * Validates the given value.
         * @param in value to be validated
         * @return validated acceptable value
         * @throws NullPointerException if in is null
         * @throws IllegalArgumentException if in is invalid
         */
        public String validate(String in)
                throws NullPointerException, IllegalArgumentException {
            in = SUS.trimOrNull(in);
            SUS.checkIfNulls("URL address null or empty", in);

            if (in.matches(REGEX)) {
                if (in.length() > MAX_LENGTH) {
                    throw new IllegalArgumentException("URL length > max length " + in.length() + ":" + in);
                }

                return in.toLowerCase();
            } else {
                throw new IllegalArgumentException("Invalid URL: " + in);
            }
        }
    },
    ;

    /**
     * One DNS label: 1 to 63 letters, digits or hyphens, no hyphen at either end.
     * Shared by {@link #DOMAIN} and the domain part of {@link #EMAIL}.
     */
    public static final String DOMAIN_LABEL = "[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?";
    /**
     * Top-level label: 2 to 63 letters, or a punycode (IDN) label.
     */
    public static final String DOMAIN_TLD = "(?:[a-zA-Z]{2,63}|[xX][nN]--[a-zA-Z0-9-]{1,59})";
    /**
     * A bare hostname of two or more labels.
     */
    public static final String DOMAIN_REGEX = "^(?:" + DOMAIN_LABEL + "\\.)+" + DOMAIN_TLD + "$";
    /**
     * Maximum hostname length per RFC 1035.
     */
    public static final int DOMAIN_MAX_LENGTH = 253;

    /**
     * Validates the given value.
     * @param in value to be validated
     * @return validated acceptable value
     * @throws NullPointerException if in is null
     * @throws IllegalArgumentException if in is invalid
     */
    public String validate(String in)
            throws NullPointerException, IllegalArgumentException {
        return in;
    }

    @Override
    public String toCanonicalID() {
        return name();
    }

    /**
     * Maps the given class to the applicable primitive type if found, otherwise returns null.
     * @param clazz
     * @return filter type
     */
    public static FilterType mapPrimitiveFilterType(Class<?> clazz) {
        if (String.class.equals(clazz)) {
            return FilterType.CLEAR;
        }

        if (Integer.class.equals(clazz) || int.class.equals(clazz)) {
            return FilterType.INTEGER;
        }

        if (Long.class.equals(clazz) || long.class.equals(clazz)) {
            return FilterType.LONG;
        }

        if (Double.class.equals(clazz) || double.class.equals(clazz)) {
            return FilterType.DOUBLE;
        }

        if (Float.class.equals(clazz) || float.class.equals(clazz)) {
            return FilterType.FLOAT;
        }

        if (Boolean.class.equals(clazz) || boolean.class.equals(clazz)) {
            return FilterType.BOOLEAN;
        }

        if (BigDecimal.class.equals(clazz)) {
            return FilterType.BIG_DECIMAL;
        }

        return null;
    }

    /**
     * Maps the given NVConfig to the applicable primitive type. Otherwise, returns null.
     * @param nvc
     * @return filter type
     */
    public static FilterType mapPrimitiveFilterType(NVConfig nvc) {
        return mapPrimitiveFilterType(nvc.getMetaType());
    }

    /**
     * Converts the string value to given NVConfig type.
     * @param nvc
     * @param value
     * @return object value
     */
    public static Object stringToValue(NVConfig nvc, String value) {
        return stringToValue(nvc.getMetaType(), value);
    }

    /**
     * Converts the string value to given class type.
     * @param clazz
     * @param value
     * @return object value
     */
    public static Object stringToValue(Class<?> clazz, String value) {
        if (value != null) {
            if (Integer.class.equals(clazz) || int.class.equals(clazz)) {
                return Integer.valueOf(value);
            }

            if (Long.class.equals(clazz) || long.class.equals(clazz)) {
                return Long.valueOf(value);
            }

            if (Double.class.equals(clazz) || double.class.equals(clazz)) {
                return Double.valueOf(value);
            }

            if (Float.class.equals(clazz) || float.class.equals(clazz)) {
                return Float.valueOf(value);
            }

            if (Boolean.class.equals(clazz) || boolean.class.equals(clazz)) {
                return Boolean.valueOf(value);
            }

            if (BigDecimal.class.equals(clazz)) {
                return new BigDecimal(value);
            }
        }

        return value;
    }

}