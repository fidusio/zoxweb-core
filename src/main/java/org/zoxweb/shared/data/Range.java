package org.zoxweb.shared.data;


import org.zoxweb.shared.filters.ValueFilter;
import org.zoxweb.shared.util.*;

/**
 * A closed, open or half-open interval over a comparable type.
 * <p>
 * A {@code Range} is also a {@link ValueFilter} over its own type: {@link #validate(Comparable)}
 * returns the value when it lies {@link #within(Comparable)} the range and throws otherwise, so a
 * range can be handed directly to {@code NVConfigManager.createNVConfig(...)} as the field's filter,
 * for example {@code Range.toRange("[5, 14]")}.
 */
@SuppressWarnings("serial")
public class Range<T extends Comparable<T>>
        extends CanonicalIDDAO
        implements ValueFilter<T, T> {

    public static final String NUMBER_PATTERN = "[-]?[0-9]*\\.?[0-9]+";

    /**
     * Include start, end in {@link Range}.
     * <p>
     * Two notations are accepted for an exclusive bound: the round bracket
     * ({@code (a, b)}) and the reversed square bracket of ISO 31-11 ({@code ]a, b[}).
     * {@link #START_TOKEN} and {@link #END_TOKEN} hold the round-bracket form, which is
     * what {@link Range#toString()} emits; {@link #ALT_START_TOKEN} and
     * {@link #ALT_END_TOKEN} hold the reversed form.
     */
    public enum Inclusivity {

        /**
         * {@link Range} inclusive of start, exclusive of end: {@code [a, b)} or {@code [a, b[}
         */
        START("[", ")", "[", "["),

        /**
         * {@link Range} inclusive of end, exclusive of start: {@code (a, b]} or {@code ]a, b]}
         */
        END("(", "]", "]", "]"),

        /**
         * {@link Range} inclusive of start and end: {@code [a, b]}
         */
        BOTH("[", "]", "[", "]"),

        /**
         * {@link Range} exclusive of start and end: {@code (a, b)} or {@code ]a, b[}
         */
        NONE("(", ")", "]", "[");

        public final String START_TOKEN;
        public final String END_TOKEN;
        public final String ALT_START_TOKEN;
        public final String ALT_END_TOKEN;
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
                .createNVConfig("inclusivity", "Inclusivity (default) or exclusive", "Inclusivity", false, true,
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
     * Auto switch if start > end
     * <br>It effects {@link #setStart(Comparable)} and {@link #setEnd(Comparable)}
     * <br>It does not affect constructor
     * <br>Default is set to false.
     */
    private boolean isAutoSwitch = false;

    // ///////////////////////////////////////////////////////////
    // ////////////////// Constructor ////////////////////////////
    // ///////////////////////////////////////////////////////////

    /**
     * Create a range with {@link Inclusivity#START}
     *
     * @param start <br/> Not null safe
     * @param end   <br/> Not null safe
     *              <br/>Auto switched if start > end
     */
    public Range(T start, T end) {

        this(start, end, null);
    }

    /**
     * @param start     <br/> Not null safe
     * @param end       <br/> Not null safe
     *                  <br/>Auto switched if start > end
     * @param inclusivity <br/>If null {@link Inclusivity#START} used
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
     * Check if this {@link Range} contains t
     *
     * @param t <br/>Not null safe
     * @return false for any value of t, if this.start equals this.end
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
     * Check if this {@link Range} contains t
     *
     * @param t         <br/>Not null safe
     * @param inclusivity <br/>If null  used
     * @return false for any value of t, if this.start equals this.end
     */
    public boolean within(T t, Inclusivity inclusivity) {

        if (t == null) {
            throw new NullPointerException("Invalid null value");
        }

        inclusivity = (inclusivity == null) ? getInclusivity() : inclusivity;

        switch (inclusivity) {

            case NONE:
                return (isBigger(t, getStart()) && isSmaller(t, getEnd()));

            case BOTH:
                return (!isBigger(getStart(), t) && !isBigger(t, getEnd()));

            case END:
                return (isBigger(t, getStart()) && !isBigger(t, getEnd()));

            case START:
            default:
                return (!isBigger(getStart(), t) && isBigger(getEnd(), t));


        }
    }

    /**
     * Check if this {@link Range} contains other range
     *
     * @return false for any value of range, if this.start equals this.end
     */
    public boolean within(Range<T> range) {

        return within(range.getStart()) && within(range.getEnd());
    }

    /**
     * Check if this {@link Range} intersects with other range
     *
     * @return false for any value of range, if this.start equals this.end
     */
    public boolean intersects(Range<T> range) {

        // an endpoint of the other range lies in this one, or the other range contains this one
        return within(range.getStart()) || within(range.getEnd()) || range.within(getStart());
    }

    /**
     * Convenience method
     */
    public static <T extends Comparable<T>> boolean isBigger(T t1, T t2) {

        return t1.compareTo(t2) > 0;
    }

    /**
     * Convenience method
     */
    public static <T extends Comparable<T>> boolean isSmaller(T t1, T t2) {

        return t1.compareTo(t2) < 0;
    }

    /**
     * Modifies range, if needed, so
     * range.getStart() is greater or equal to intoOtherRange.getStart() and
     * range.getEnd() is less or equal to intoOtherRange.getEnd().
     * It does not guarantee into.contains(range)==true which depends also on inclusivity.
     *
     * @param range the range to fit
     * @param into the target range. Both are not null safe
     * @param <T> the comparable type
     * @return the fitted range
     */
    public static <T extends Comparable<T>> Range<T> fit(Range<T> range,
                                                         Range<T> into) {
        if (isBigger(into.getStart(), range.getStart()) //start too small
                || isBigger(range.getStart(), into.getEnd())) { //start too big
            range.setStart(into.getStart());
        }

        if (isBigger(into.getStart(), range.getEnd()) //end too small
                || isBigger(range.getEnd(), into.getEnd())) { //start too big
            range.setEnd(into.getEnd());
        }

        return range;
    }

    /**
     * Modifies range, if needed, so
     * range.getStart() is less or equal to intoOtherRange.getStart() and
     * range.getEnd() is greater or equal to intoOtherRange.getEnd().
     * It does not guarantee range.contains(toContain)==true which depends also on inclusivity.
     *
     * @param range the range to expand
     * @param toContain the range to contain. Both are not null safe
     * @param <T> the comparable type
     * @return the expanded range
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
     * Returns T which is within min (inclusive) , max (inclusive)
     *
     * @param value, min, max
     *               <br/>Not null safe
     *               <br/> if min > max they are switched
     * @return min if value is smaller than min
     * <br/>max if value is bigger than max
     * <br/>value otherwise
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
     * @see java.lang.Object#toString()
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
     * Set the start value
     * <br/>Not null safe
     * <br/>If {@link #isAutoSwitch} is set to true, and  start > end
     * they are switched
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


    public int getLoopStart() {
        int loopStart = ((Number) getStart()).intValue();
        switch (getInclusivity()) {
            case END:
            case NONE:
                loopStart++;
                break;
        }
        return loopStart;
    }

    public int getLoopEnd() {
        int loopEnd = ((Number) getEnd()).intValue();
        switch (getInclusivity()) {
            case BOTH:
            case END:
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
     * Set the end value
     * <br/>Not null safe
     * <br/>If {@link #isAutoSwitch} is set to true, and  start > end
     * they are switched
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

    public String getUnit() {
        return lookupValue(Param.UNIT);
    }

    public void setUnit(String unit) {
        setValue(Param.UNIT, unit);
    }

    /**
     * @return the inclusive type
     */
    public Inclusivity getInclusivity() {
        return lookupValue(Param.INCLUSIVITY);
    }

    /**
     * Set the inclusive type
     *
     * @param inclusivity <br/>If null {@link Inclusivity#BOTH} used
     */
    public Range<T> setInclusivity(Inclusivity inclusivity) {

        inclusivity = (inclusivity == null) ? Inclusivity.BOTH : inclusivity;
        setValue(Param.INCLUSIVITY, inclusivity);
        return this;
    }

    /**
     * Get {@link #isAutoSwitch}
     */
    public boolean isAutoSwitch() {
        return isAutoSwitch;
    }

    /**
     * Set {@link #isAutoSwitch}
     */
    public Range<T> setAutoSwitch(boolean isAutoSwitch) {

        this.isAutoSwitch = isAutoSwitch;
        return this;
    }


    public static  Range toRange(String rangeToken) {
        return toRange(rangeToken, null, null, null);
    }

    public static Range toRange(String rangeToken, String name, String unit) {
        return toRange(rangeToken, null, name, unit);
    }


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
