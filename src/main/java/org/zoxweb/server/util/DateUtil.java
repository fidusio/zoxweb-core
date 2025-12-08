package org.zoxweb.server.util;

import org.zoxweb.shared.util.Const.DayOfWeek;
import org.zoxweb.shared.util.Const.TimeInMillis;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class DateUtil {

    public static class ExtendedDTF {

        private final DateTimeFormatter formatter;
        private final ZoneId zone;

        private ExtendedDTF(DateTimeFormatter formatter) {
            this(formatter, ZoneId.systemDefault());
        }

        private ExtendedDTF(DateTimeFormatter formatter, ZoneId zone) {
            this.formatter = formatter;
            this.zone = zone != null ? zone : ZoneId.systemDefault();
        }

        /** Format java.time types normally */
        public String format(TemporalAccessor temporal) {
            return formatter.format(temporal);
        }

        /** Format java.util.Date */
        public String format(Date date) {
            Instant instant = date.toInstant();
            ZonedDateTime zdt = instant.atZone(zone);
            return formatter.format(zdt);
        }

        /** Format milliseconds since epoch */
        public String format(long millis) {
            Instant instant = Instant.ofEpochMilli(millis);
            ZonedDateTime zdt = instant.atZone(zone);
            return formatter.format(zdt);
        }

        public TemporalAccessor parse(String text) {
            return formatter.parse(text);
        }

        public long toTimeInMillis(String text)
        {
            return Instant.from(formatter.parse(text)).toEpochMilli();
        }

        public Date toDate(String text)
        {
            return new Date(toTimeInMillis(text));
        }

        public DateTimeFormatter unwrap() {
            return formatter;
        }
    }
    public static final ExtendedDTF DEFAULT_DATE_FORMAT = createDTF("yyyy-MM-dd HH:mm:ss.SSS");
    public static final ExtendedDTF DEFAULT_DATE_FORMAT_TZ = createDTF("yyyy-MM-dd HH:mm:ss.SSS zzz");
    public static final ExtendedDTF DEFAULT_JAVA_FORMAT = createDTF("EEE MMM dd HH:mm:ss zzz yyyy");
    public static final ExtendedDTF DEFAULT_GMT_MILLIS = createDTF("yyyy-MM-dd'T'HH:mm:ss.SSSX", "UTC");
    public static final ExtendedDTF DEFAULT_ZULU_MILLIS = createDTF("yyyy-MM-dd'T'HH:mm:ss.SSSZ", "UTC");
    public static final ExtendedDTF ISO_8601 = createDTF("yyyy-MM-dd'T'HH:mm:ssXXX");
    public static final ExtendedDTF DEFAULT_GMT = createDTF("yyyy-MM-dd'T'HH:mm:ssX", "UTC");
    /**
     * Today Local TimeZone format 'yyyy-MM-dd'
     */
    public static final ExtendedDTF TODAY_LTZ = createDTF("yyyy-MM-dd");
    public static final ExtendedDTF FILE_DATE_FORMAT = createDTF("yyyy-MM-dd_HH-mm-ss-SSS");

    private DateUtil() {
    }


    public static SimpleDateFormat createSDF(String pattern, String timezone) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        sdf.setTimeZone(TimeZone.getTimeZone(timezone));
        return sdf;
    }

    public static ExtendedDTF createDTF(String pattern ) {
        return new ExtendedDTF(DateTimeFormatter.ofPattern(pattern),null);
    }
    public static ExtendedDTF createDTF(String pattern, String zoneId) {
        return new ExtendedDTF(DateTimeFormatter.ofPattern(pattern), zoneId != null ? ZoneId.of(zoneId) : null);
    }

    /**
     * Return date in normal format jan=1... dec=12
     *
     * @param date in millis since 1970
     * @return value 1-12
     */
    public static int getNormalizedMonth(long date) {
        return getNormalizedMonth(new Date(date));
    }


    /**
     * Return date in normal format jan=1... dec=12
     *
     * @param date in millis since 1970
     * @return value 1-12
     */
    public static int getNormalizedMonth(Date date) {
        return getCalendar(date).get(Calendar.MONTH) + 1;
    }


    public static int getNormalizedYear(long date) {
        return getNormalizedYear(new Date(date));
    }

    public static int getNormalizedYear(Date date) {
        return getCalendar(date).get(Calendar.YEAR);
    }

    /**
     * Return gregorian calendar
     */
    public static Calendar getCalendar(long date) {
        return getCalendar(new Date(date));
    }

    /**
     * Return gregorian calendar
     */
    public static Calendar getCalendar(Date date) {
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(date);
        return cal;
    }


    public static long timeInMillisRelativeToDay() {
        return timeInMillisRelativeToDay(new Date());
    }

    public static long timeInMillisRelativeToDay(long date) {
        return timeInMillisRelativeToDay(new Date(date));
    }

    public static long timeInMillisRelativeToDay(Date date) {
        Calendar calendar = getCalendar(date);
        return calendar.get(Calendar.HOUR_OF_DAY) * TimeInMillis.HOUR.MILLIS +
                calendar.get(Calendar.MINUTE) * TimeInMillis.MINUTE.MILLIS +
                calendar.get(Calendar.SECOND) * TimeInMillis.SECOND.MILLIS +
                calendar.get(Calendar.MILLISECOND);
    }

    public static long timeInMillisRelativeToWeek(Date date) {
        return timeInMillisRelativeToWeek(getCalendar(date));
    }

    public static long timeInMillisRelativeToWeek(long date) {
        return timeInMillisRelativeToWeek(getCalendar(date));
    }

    public static long timeInMillisRelativeToWeek(Calendar calendar) {
        return DayOfWeek.lookup(calendar.get(Calendar.DAY_OF_WEEK) - 1).getValue()
                * TimeInMillis.DAY.MILLIS +
                calendar.get(Calendar.HOUR_OF_DAY) * TimeInMillis.HOUR.MILLIS +
                calendar.get(Calendar.MINUTE) * TimeInMillis.MINUTE.MILLIS +
                calendar.get(Calendar.SECOND) * TimeInMillis.SECOND.MILLIS +
                calendar.get(Calendar.MILLISECOND);
    }

    public static DayOfWeek dayOfWeek(Date date) {
        Calendar calendar = getCalendar(date);
        return DayOfWeek.lookup(calendar.get(Calendar.DAY_OF_WEEK) - 1);
    }

    public static DayOfWeek dayOfWeek(long millis) {
        return dayOfWeek(new Date(millis));
    }

    public static String format(Date date, DateTimeFormatter formatter) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .format(formatter);
    }


}
