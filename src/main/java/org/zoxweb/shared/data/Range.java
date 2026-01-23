package org.zoxweb.shared.data;


import org.zoxweb.shared.util.*;

@SuppressWarnings("serial")
public class Range<T extends Comparable<T>>
        extends CanonicalIDDAO {

    public static final String NUMBER_PATTERN = "[-]?[0-9]*\\.?[0-9]+";

    /**
     * Include start, end in {@link Range}
     */
    public enum Inclusive {

        /**
         * {@link Range} inclusive of start, exclusive of end
         */
        START("[", ")"),

        /**
         * {@link Range} inclusive of end, exclusive of start
         */
        END("(", "]"),

        /**
         * {@link Range} inclusive of start and end
         */
        BOTH("[", "]"),

        /**
         * {@link Range} exclusive of start and end
         */
        NONE("(", ")");

        public final String START_TOKEN;
        public final String END_TOKEN;
        public final String PATTERN;

        Inclusive(String startToken, String endToken) {
            START_TOKEN = startToken;
            END_TOKEN = endToken;
            PATTERN = "^\\" + START_TOKEN + NUMBER_PATTERN + "," + NUMBER_PATTERN + "\\" + END_TOKEN + "$";
        }


        public static Inclusive match(String token) {
            token = token.replaceAll("\\s+", "");
            for (Inclusive i : Inclusive.values()) {
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
        INCLUSIVE(NVConfigManager
                .createNVConfig("inclusive", "Inclusive (default) or exclusive", "inclusive", false, true,
                        Inclusive.class)),
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
     * Create a range with {@link Inclusive#START}
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
     * @param inclusive <br/>If null {@link Inclusive#START} used
     */
    public Range(T start, T end, Inclusive inclusive) {
        this();

        if ((start == null) || (end == null)) {

            throw new NullPointerException("Invalid null start / end value");
        }
        setInclusive(inclusive);

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

        return within(t, getInclusive());
    }

    /**
     * Check if this {@link Range} contains t
     *
     * @param t         <br/>Not null safe
     * @param inclusive <br/>If null  used
     * @return false for any value of t, if this.start equals this.end
     */
    public boolean within(T t, Inclusive inclusive) {

        if (t == null) {

            throw new NullPointerException("Invalid null value");
        }

        inclusive = (inclusive == null) ? getInclusive() : inclusive;

        switch (inclusive) {

            case NONE:
                return (isBigger(t, getStart()) && isSmaller(t, getEnd()));

            case BOTH:
                return (!isBigger(getStart(), t) && !isBigger(t, getEnd()));

            case START:
            default:
                return (!isBigger(getStart(), t) && isBigger(getEnd(), t));

            case END:
                return (isBigger(t, getStart()) && !isBigger(t, getEnd()));
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

        return within(range.getStart()) || within(range.getEnd());
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
        Range<T> range = new Range<>(min, max, Inclusive.BOTH);
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
        return getInclusive().START_TOKEN + getStart() + ", " + getEnd() + getInclusive().END_TOKEN;
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
        switch (getInclusive()) {
            case END:
            case NONE:
                loopStart++;
                break;
        }
        return loopStart;
    }

    public int getLoopEnd() {
        int loopEnd = ((Number) getEnd()).intValue();
        switch (getInclusive()) {
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
    public Inclusive getInclusive() {
        return lookupValue(Param.INCLUSIVE);
    }

    /**
     * Set the inclusive type
     *
     * @param inclusive <br/>If null {@link Inclusive#START} used
     */
    public Range<T> setInclusive(Inclusive inclusive) {

        inclusive = (inclusive == null) ? Inclusive.BOTH : inclusive;
        setValue(Param.INCLUSIVE, inclusive);
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
        Inclusive type = Inclusive.match(token);
        if (type == null)
            throw new IllegalArgumentException("Invalid range type:" + token);

        String[] tokens = SharedStringUtil.parseString(token, ",", type.START_TOKEN, type.END_TOKEN);

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
        }

        if (ret != null) {
            ret.setName(name);
            ret.setUnit(unit);
        }

        return ret;
    }


}
