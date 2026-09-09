package org.zoxweb.shared.data;


import org.zoxweb.shared.filters.ValueFilter;
import org.zoxweb.shared.util.*;

/**
 * A closed, open or half-open interval over a comparable type, with a start, an end and an
 * {@link Inclusivity} that says which of the two ends belong to the interval. The inclusivity
 * defaults to {@link Inclusivity#BOTH} when none is set.
 * <p>
 * A {@code Range} is also a {@link ValueFilter} over its own type: {@link #validate(Comparable)}
 * returns the value when it lies {@link #within(Comparable)} the range and throws otherwise, so a
 * range can be handed directly to {@code NVConfigManager.createNVConfig(...)} as the field's filter,
 * for example {@code Range.toRange("[5, 14]")}.
 * <p>
 * Numeric ranges have a text form, parsed by {@link #toRange(String)} and produced by
 * {@link #toString()}: two numbers separated by a comma inside a pair of brackets, where the
 * bracket shape gives the inclusivity, see {@link Inclusivity}.
 */
@SuppressWarnings("serial")
public class Range<T extends Comparable<T>>
        extends CanonicalIDDAO
        implements ValueFilter<T, T> {

    /**
     * One bound in the text form: an optional minus sign, digits, an optional decimal part.
     */
    public static final String NUMBER_PATTERN = "[-]?[0-9]*\\.?[0-9]+";

    /**
     * Which ends of the interval are included.
     * <p>
     * Two notations are accepted for an excluded end: the round bracket
     * ({@code (a, b)}) and the reversed square bracket of ISO 31-11 ({@code ]a, b[}).
     * {@link #START_TOKEN} and {@link #END_TOKEN} hold the round-bracket form, which is
     * what {@link Range#toString()} emits; {@link #ALT_START_TOKEN} and
     * {@link #ALT_END_TOKEN} hold the reversed form. {@link #match(String)} recognizes both.
     */
    public enum Inclusivity {

        /**
         * Left-closed, right-open: the start is included, the end is not. {@code [a, b)} or {@code [a, b[}
         */
        LEFT("[", ")", "[", "["),

        /**
         * Left-open, right-closed: the end is included, the start is not. {@code (a, b]} or {@code ]a, b]}
         */
        RIGHT("(", "]", "]", "]"),

        /**
         * Closed: both ends included. {@code [a, b]}. The default.
         */
        BOTH("[", "]", "[", "]"),

        /**
         * Open: neither end included. {@code (a, b)} or {@code ]a, b[}
         */
        NONE("(", ")", "]", "[");

        /** Opening bracket in the round-bracket notation, the form {@link Range#toString()} emits. */
        public final String START_TOKEN;
        /** Closing bracket in the round-bracket notation. */
        public final String END_TOKEN;
        /** Opening bracket in the reversed square-bracket notation. */
        public final String ALT_START_TOKEN;
        /** Closing bracket in the reversed square-bracket notation. */
        public final String ALT_END_TOKEN;
        /** Regex matching a whitespace-free numeric range in either notation for this inclusivity. */
        public final String PATTERN;

        Inclusivity(String startToken, String endToken, String altStartToken, String altEndToken) {
            START_TOKEN = startToken;
            END_TOKEN = endToken;
            ALT_START_TOKEN = altStartToken;
            ALT_END_TOKEN = altEndToken;
            String body = NUMBER_PATTERN + "," + NUMBER_PATTERN;
            PATTERN = "^(?:\\" + START_TOKEN + body + "\\" + END_TOKEN
                    + "|\\" + ALT_START_TOKEN + body + "\\" + ALT_END_TOKEN + ")$";
        }


        /**
         * Identifies the inclusivity of a numeric range in text form from its brackets, in either
         * notation. Whitespace is ignored.
         *
         * @param token the text form, for example {@code "[1, 5)"} or {@code "]1, 5]"}
         * @return the matching inclusivity, or null when the text is not a well-formed range
         */
        public static Inclusivity match(String token) {
            token = token.replaceAll("\\s+", "");
            for (Inclusivity i : Inclusivity.values()) {
                if (token.matches(i.PATTERN)) {
                    return i;
                }
            }
            return null;
        }


    }


    public enum Param
            implements GetNVConfig {
        START(NVConfigManager.createNVConfig("r_start", "Start range", "Start", true, true, Number.class)),
        END(NVConfigManager.createNVConfig("r_end", "End range", "End", true, true, Number.class)),
        INCLUSIVITY(NVConfigManager
                .createNVConfig("inclusivity", "Which ends are included; BOTH when unset", "Inclusivity", false, true,
                        Inclusivity.class)),
        UNIT(NVConfigManager.createNVConfig("unit", "Range Unit", "Unit", false, true, String.class)),

        ;

        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        @Override
        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_RANGE = new NVConfigEntityPortable(
            "range",
            null,
            Range.class.getSimpleName(),
            true,
            false,
            false,
            false,
            Range.class,
            SharedUtil.extractNVConfigs(Range.Param.values()),
            null,
            false,
            CanonicalIDDAO.NVC_CANONICAL_ID_DAO
    );


    public Range() {
        super(NVC_RANGE);
    }


    /**
     * When true, {@link #setStart(Comparable)} and {@link #setEnd(Comparable)} swap the two ends
     * if the new value would leave start greater than end. The constructors always swap,
     * regardless of this flag. Not part of the entity meta, so it is neither serialized nor
     * persisted. Default false.
     */
    private boolean isAutoSwitch = false;

    // ///////////////////////////////////////////////////////////
    // ////////////////// Constructor ////////////////////////////
    // ///////////////////////////////////////////////////////////

    /**
     * Creates a closed range, {@link Inclusivity#BOTH}. The two ends are swapped if start is
     * greater than end.
     *
     * @param start one end, not null
     * @param end   the other end, not null
     * @throws NullPointerException if either end is null
     */
    public Range(T start, T end) {

        this(start, end, null);
    }

    /**
     * Creates a range. The two ends are swapped if start is greater than end.
     *
     * @param start       one end, not null
     * @param end         the other end, not null
     * @param inclusivity which ends are included; null means {@link Inclusivity#BOTH}
     * @throws NullPointerException if either end is null
     */
    public Range(T start, T end, Inclusivity inclusivity) {
        this();

        if ((start == null) || (end == null)) {

            throw new NullPointerException("Invalid null start / end value");
        }
        setInclusivity(inclusivity);

        if (isBigger(start, end)) {
            setStart(end);
            setEnd(start);
        } else {
            setStart(start);
            setEnd(end);
        }

    }

    // ///////////////////////////////////////////////////////////
    // ///////////////////// Methods /////////////////////////////
    // ///////////////////////////////////////////////////////////

    /**
     * Whether t lies inside this range under the range's own inclusivity.
     * A single-point range (start equals end) contains its point only under
     * {@link Inclusivity#BOTH}.
     *
     * @param t the value to test, not null
     * @return true when t is inside the range
     * @throws NullPointerException if t is null
     */
    public boolean within(T t) {

        return within(t, getInclusivity());
    }

    // ///////////////////////////////////////////////////////////
    // ///////////////////// ValueFilter /////////////////////////
    // ///////////////////////////////////////////////////////////

    /**
     * {@link ValueFilter} contract: the value itself when it is {@link #within(Comparable)} this
     * range, using the range's own inclusivity.
     *
     * @param in the value to check
     * @return in
     * @throws NullPointerException     if in is null
     * @throws IllegalArgumentException if in is outside the range
     */
    @Override
    public T validate(T in)
            throws NullPointerException, IllegalArgumentException {
        if (in == null) {
            throw new NullPointerException("Null value for range " + this);
        }
        if (!within(in)) {
            throw new IllegalArgumentException(in + " is out of range " + this);
        }
        return in;
    }

    /**
     * {@link ValueFilter} contract: true when in is non-null and {@link #within(Comparable)} this range.
     */
    @Override
    public boolean isValid(T in) {
        return in != null && within(in);
    }

    /**
     * The explicit canonical id when one was set, otherwise the interval notation
     * ({@code [5, 14]}), which {@link #toRange(String)} reads back. This is what the meta layer
     * records under {@code value_filter} when the range is used as a field filter.
     */
    @Override
    public String toCanonicalID() {
        String id = getCanonicalID();
        return id != null ? id : toString();
    }

    /**
     * Whether t lies inside this range under the given inclusivity, which overrides the
     * range's own for this test only. A single-point range (start equals end) contains its
     * point only under {@link Inclusivity#BOTH}.
     *
     * @param t           the value to test, not null
     * @param inclusivity the inclusivity to apply; null means the range's own
     * @return true when t is inside the range
     * @throws NullPointerException if t is null
     */
    public boolean within(T t, Inclusivity inclusivity) {

        if (t == null) {
            throw new NullPointerException("Invalid null value");
        }

        inclusivity = (inclusivity == null) ? getInclusivity() : inclusivity;

        switch (inclusivity) {

            case NONE:
                return (isBigger(t, getStart()) && isSmaller(t, getEnd()));

            case RIGHT:
                return (isBigger(t, getStart()) && !isBigger(t, getEnd()));

            case LEFT:
                return (!isBigger(getStart(), t) && isBigger(getEnd(), t));

            case BOTH:
            default:
                return (!isBigger(getStart(), t) && !isBigger(t, getEnd()));


        }
    }

    /**
     * Whether both ends of the other range lie inside this one. The test is on the end values
     * under this range's inclusivity; the other range's own inclusivity is not considered, so
     * {@code [1, 5)} does not contain {@code [1, 5)} by this test because 5 is excluded here.
     *
     * @param range the other range, not null
     * @return true when both of its ends are within this range
     */
    public boolean within(Range<T> range) {

        return within(range.getStart()) && within(range.getEnd());
    }

    /**
     * Whether the two ranges share at least one point: an end of the other range lies inside
     * this one, or the other range contains this one's start. Inclusivity is honoured on the
     * shared ends, so {@code [1, 5)} and {@code [5, 9]} do not intersect.
     *
     * @param range the other range, not null
     * @return true when the ranges overlap
     */
    public boolean intersects(Range<T> range) {

        // an endpoint of the other range lies in this one, or the other range contains this one
        return within(range.getStart()) || within(range.getEnd()) || range.within(getStart());
    }

    /**
     * @return true when t1 is strictly greater than t2 by {@link Comparable#compareTo}
     */
    public static <T extends Comparable<T>> boolean isBigger(T t1, T t2) {

        return t1.compareTo(t2) > 0;
    }

    /**
     * @return true when t1 is strictly smaller than t2 by {@link Comparable#compareTo}
     */
    public static <T extends Comparable<T>> boolean isSmaller(T t1, T t2) {

        return t1.compareTo(t2) < 0;
    }

    /**
     * Shrinks range in place, if needed, so that its start is not below into's start and its
     * end is not above into's end. Only the end values move; inclusivity is untouched, so
     * {@code into.within(range)} is not guaranteed afterwards.
     *
     * @param range the range to fit, modified and returned; not null
     * @param into  the bounding range; not null
     * @param <T>   the comparable type
     * @return range
     */
    public static <T extends Comparable<T>> Range<T> fit(Range<T> range,
                                                         Range<T> into) {
        if (isBigger(into.getStart(), range.getStart()) //start too small
                || isBigger(range.getStart(), into.getEnd())) { //start too big
            range.setStart(into.getStart());
        }

        if (isBigger(into.getStart(), range.getEnd()) //end too small
                || isBigger(range.getEnd(), into.getEnd())) { //end too big
            range.setEnd(into.getEnd());
        }

        return range;
    }

    /**
     * Grows range in place, if needed, so that its start is not above toContain's start and
     * its end is not below toContain's end. Only the end values move; inclusivity is
     * untouched, so {@code range.within(toContain)} is not guaranteed afterwards.
     *
     * @param range     the range to expand, modified and returned; not null
     * @param toContain the range to cover; not null
     * @param <T>       the comparable type
     * @return range
     */
    public static <T extends Comparable<T>> Range<T> expand(Range<T> range,
                                                            Range<T> toContain) {
        if (isBigger(range.getStart(), toContain.getStart()) //start too big
                || isBigger(range.getStart(), toContain.getEnd())) { //start too big
            range.setStart(toContain.getStart());
        }

        if (isBigger(toContain.getStart(), range.getEnd()) //end too small
                || isBigger(toContain.getEnd(), range.getEnd())) { //end too small
            range.setEnd(toContain.getEnd());
        }

        return range;
    }

    /**
     * Clamps value to the closed interval [min, max]. If min is greater than max they are
     * swapped first.
     *
     * @param value the value to clamp, not null
     * @param min   one bound, not null
     * @param max   the other bound, not null
     * @param <T>   the comparable type
     * @return min when value is below it, max when value is above it, value otherwise
     */
    public static <T extends Comparable<T>> T setWithin(T value, T min, T max) {
        Range<T> range = new Range<>(min, max, Inclusivity.BOTH);
        if (Range.isBigger(value, range.getEnd())) {

            value = range.getEnd();

        } else if (Range.isBigger(range.getStart(), value)) {

            value = range.getStart();
        }

        return value;
    }

    /**
     * The text form in round-bracket notation, for example {@code [1, 5)}; {@link #toRange(String)}
     * reads it back.
     */
    @Override
    public String toString() {
        return getInclusivity().START_TOKEN + getStart() + ", " + getEnd() + getInclusivity().END_TOKEN;
    }

    // ///////////////////////////////////////////////////////////
    // ////////////////// Getters, Setters ///////////////////////
    // ///////////////////////////////////////////////////////////

    /**
     * @return the start value
     */
    public T getStart() {
        return lookupValue(Param.START);
    }

    /**
     * Sets the start. With auto-switch on, if the new start is greater than the current end the
     * two are swapped; that comparison requires the end to be set already.
     *
     * @param start the new start, not null
     * @return this
     */
    public Range<T> setStart(T start) {

        if (isAutoSwitch && (start.compareTo(getEnd()) > 0)) {
            setValue(Param.START, getEnd());
            setValue(Param.END, start);
            //this.start = end;
            //this.end  = start;
        } else {
            //this.start = start;
            setValue(Param.START, start);
        }


        return this;
    }


    /**
     * First index of the half-open integer loop {@code [getLoopStart(), getLoopEnd())} that
     * visits every integer inside this range: the start as an int, plus one when the start is
     * excluded. Numeric ranges only.
     */
    public int getLoopStart() {
        int loopStart = ((Number) getStart()).intValue();
        switch (getInclusivity()) {
            case RIGHT:
            case NONE:
                loopStart++;
                break;
        }
        return loopStart;
    }

    /**
     * Exclusive upper index of the half-open integer loop {@code [getLoopStart(), getLoopEnd())}:
     * the end as an int, plus one when the end is included. Numeric ranges only.
     */
    public int getLoopEnd() {
        int loopEnd = ((Number) getEnd()).intValue();
        switch (getInclusivity()) {
            case BOTH:
            case RIGHT:
                loopEnd++;
                break;
        }
        return loopEnd;
    }

    /**
     * @return the end value
     */
    public T getEnd() {
        return lookupValue(Param.END);
    }

    /**
     * Sets the end. With auto-switch on, if the new end is smaller than the current start the
     * two are swapped; that comparison requires the start to be set already.
     *
     * @param end the new end, not null
     * @return this
     */
    public Range<T> setEnd(T end) {

        if (isAutoSwitch && (getStart().compareTo(end) > 0)) {
            setValue(Param.END, getStart());
            setValue(Param.START, end);
            //this.end  = start;
            //this.start = end;
        } else {
            setValue(Param.END, end);
            //this.end = end;
        }

        return this;
    }

    /**
     * @return the free-text unit label, for example "ms" or "%", or null
     */
    public String getUnit() {
        return lookupValue(Param.UNIT);
    }

    /**
     * @param unit a free-text unit label, or null
     */
    public void setUnit(String unit) {
        setValue(Param.UNIT, unit);
    }

    /**
     * @return which ends are included; {@link Inclusivity#BOTH} when none has been set
     */
    public Inclusivity getInclusivity() {
        Inclusivity ret =  lookupValue(Param.INCLUSIVITY);
        return ret != null ? ret : Inclusivity.BOTH;
    }

    /**
     * Sets which ends are included. Null clears the stored value, after which
     * {@link #getInclusivity()} reports {@link Inclusivity#BOTH}.
     *
     * @param inclusivity the inclusivity, or null
     * @return this
     */
    public Range<T> setInclusivity(Inclusivity inclusivity) {
        setValue(Param.INCLUSIVITY, inclusivity);
        return this;
    }

    /**
     * @return whether {@link #setStart(Comparable)} and {@link #setEnd(Comparable)} swap the
     * ends to keep start below end
     */
    public boolean isAutoSwitch() {
        return isAutoSwitch;
    }

    /**
     * @param isAutoSwitch whether {@link #setStart(Comparable)} and {@link #setEnd(Comparable)}
     *                     swap the ends to keep start below end
     * @return this
     */
    public Range<T> setAutoSwitch(boolean isAutoSwitch) {

        this.isAutoSwitch = isAutoSwitch;
        return this;
    }


    /**
     * Parses a numeric range from its text form, see {@link #toRange(String, Class, String, String)}.
     *
     * @param rangeToken the text form, for example {@code "[1, 5)"}
     * @return the range, typed from the numbers
     * @throws IllegalArgumentException if the text is not a well-formed range
     */
    public static  Range toRange(String rangeToken) {
        return toRange(rangeToken, null, null, null);
    }

    /**
     * Parses a numeric range from its text form and labels it, see
     * {@link #toRange(String, Class, String, String)}.
     *
     * @param rangeToken the text form, for example {@code "[1, 5)"}
     * @param name       the range's name, or null
     * @param unit       the range's unit label, or null
     * @return the range, typed from the numbers
     * @throws IllegalArgumentException if the text is not a well-formed range
     */
    public static Range toRange(String rangeToken, String name, String unit) {
        return toRange(rangeToken, null, name, unit);
    }


    /**
     * Parses a numeric range from its text form: two numbers separated by a comma inside a pair
     * of brackets, whitespace ignored. The bracket shape gives the inclusivity in either notation,
     * see {@link Inclusivity}: {@code [1, 5]}, {@code (1, 5)}, {@code [1, 5)}, {@code (1, 5]},
     * and the reversed forms {@code ]1, 5[}, {@code [1, 5[}, {@code ]1, 5]}.
     * <p>
     * Without an override the element type comes from the numbers: {@code Integer} when both
     * fit, else {@code Long}; {@code Float} when either has a decimal part, else {@code Double}
     * when it exceeds float magnitude. A decimal number kept as {@code Float} loses precision
     * beyond seven digits; pass {@code Double.class} to keep it.
     *
     * @param token    the text form
     * @param override the element type to produce: {@code Integer}, {@code Long}, {@code Float}
     *                 or {@code Double}; null to infer it from the numbers
     * @param name     the range's name, or null
     * @param unit     the range's unit label, or null
     * @return the range
     * @throws IllegalArgumentException if the text is not a well-formed range, or the override
     *                                  is not one of the four supported types
     */
    public static Range toRange(String token, Class<? extends Number> override, String name, String unit) {
        token = token.replaceAll("\\s+", "");
        Inclusivity type = Inclusivity.match(token);
        if (type == null)
            throw new IllegalArgumentException("Invalid range type:" + token);

        // match() guaranteed the shape: one bracket on each side, whichever notation was used
        String[] tokens = token.substring(1, token.length() - 1).split(",");

        if (tokens.length != 2) {
            throw new IllegalArgumentException("Invalid range:" + token);
        }
        Number start = SharedUtil.parseNumber(tokens[0]);
        Number end = SharedUtil.parseNumber(tokens[1]);
        Number[] vals = SharedUtil.normalizeNumbers(start, end);

        if (override == null)
            override = vals[0].getClass();


        Range ret = null;
        if (override == Integer.class) {
            ret = new Range<Integer>(vals[0].intValue(), vals[1].intValue(), type);
        } else if (override == Long.class) {
            ret = new Range<Long>(vals[0].longValue(), vals[1].longValue(), type);
        } else if (override == Float.class) {
            ret = new Range<Float>(vals[0].floatValue(), vals[1].floatValue(), type);
        } else if (override == Double.class) {
            ret = new Range<Double>(vals[0].doubleValue(), vals[1].doubleValue(), type);
        } else {
            throw new IllegalArgumentException("Unsupported range type: " + override.getName()
                    + " (Integer, Long, Float or Double)");
        }

        ret.setName(name);
        ret.setUnit(unit);

        return ret;
    }


}
