package org.zoxweb.server.filters;

import org.zoxweb.server.util.DateUtil;
import org.zoxweb.shared.filters.ValueFilter;
import org.zoxweb.shared.util.Const;

import java.time.format.DateTimeParseException;

/**
 * The date filter validates date formats.
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class TimestampFilter
        implements ValueFilter<String, Long> {


    /**
     * Defines an array of support date formats.
     */
    private static DateUtil.ExtendedDTF[] sdf =
            {
                    DateUtil.DEFAULT_GMT_MILLIS,
                    DateUtil.DEFAULT_GMT,
                    DateUtil.DEFAULT_JAVA_FORMAT,
                    DateUtil.createDTF("yyyy-MM-dd'T'HH:mm:ss.SSSZ", "UTC"),
                    DateUtil.createDTF("yyyy-MM-dd'T'HH:mm:ssZ", "UTC"),
                    DateUtil.createDTF("yyyy-MM-dd hh:mm:ss", "UTC"),
                    DateUtil.createDTF("yyyy-MM-dd", "UTC"),

                    DateUtil.createDTF("MM-yy", "UTC"),
                    DateUtil.createDTF("MM-yyyy", "UTC"),

            };

    /**
     * Declares that only one instance of this class can be created.
     */
    public static final TimestampFilter SINGLETON = new TimestampFilter();

    /**
     * The default constructor is declared private to prevent
     * outside instantiation of this class.
     */
    private TimestampFilter() {

    }

    /**
     * Gets the string representation of this class.
     */
    public String toCanonicalID() {
        return null;
    }

    /**
     * Validates a string input and returns a long value.
     * @param in
     */
    public Long validate(String in)
            throws NullPointerException, IllegalArgumentException {

        for (DateUtil.ExtendedDTF format : sdf) {
            try {
                return format.toTimeInMillis(in);//Instant.from(format.parse(in)).toEpochMilli();
//                return format.parse(in).getTime();
            } catch (DateTimeParseException e) {

            }
        }

        try {
            return Const.TimeInMillis.toMillis(in);
        } catch (IllegalArgumentException e) {

        }


        try {
            return Long.parseLong(in);
        } catch (NumberFormatException e) {

        }


        throw new IllegalArgumentException("Invalid format: " + in);
    }

    /**
     * Checks whether the input is valid or not.
     * @param in
     */
    public boolean isValid(String in) {
        try {
            validate(in);

            return true;
        } catch (Exception e) {

        }

        return false;
    }

}