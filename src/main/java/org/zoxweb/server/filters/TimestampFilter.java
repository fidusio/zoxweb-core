package org.zoxweb.server.filters;

import org.zoxweb.server.util.DateUtil;
import org.zoxweb.shared.filters.ValueFilter;
import org.zoxweb.shared.util.Const;

import java.text.ParseException;
import java.text.SimpleDateFormat;

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
    private static SimpleDateFormat[] sdf =
            {
                    DateUtil.DEFAULT_GMT_MILLIS,
                    DateUtil.DEFAULT_GMT,
                    DateUtil.DEFAULT_JAVA_FORMAT,
                    DateUtil.createSDF("yyyy-MM-dd'T'HH:mm:ss.SSSZ", "UTC"),
                    DateUtil.createSDF("yyyy-MM-dd'T'HH:mm:ssZ", "UTC"),
                    DateUtil.createSDF("yyyy-MM-dd hh:mm:ss", "UTC"),
                    DateUtil.createSDF("yyyy-MM-dd", "UTC"),

                    DateUtil.createSDF("MM-yy", "UTC"),
                    DateUtil.createSDF("MM-yyyy", "UTC"),

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

        for (SimpleDateFormat format : sdf) {
            try {
                return format.parse(in).getTime();
            } catch (ParseException e) {

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