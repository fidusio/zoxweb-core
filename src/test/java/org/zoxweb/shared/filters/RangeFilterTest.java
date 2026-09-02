package org.zoxweb.shared.filters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises every {@link RangeFilter} implementation ({@link IntRangeFilter}, {@link LongRangeFilter},
 * {@link FloatRangeFilter}, {@link DoubleRangeFilter}) against the same contract:
 * <ul>
 *   <li>all four inclusive/exclusive limit combinations,</li>
 *   <li>exact boundary values, values just inside and just outside,</li>
 *   <li>{@code validate}/{@code encode} returning the input or throwing {@link IllegalArgumentException},</li>
 *   <li>null handling, degenerate (empty / single point) ranges, negative ranges, type extremes,</li>
 *   <li>float/double specials (NaN, infinities),</li>
 *   <li>metadata accessors, {@code toString}, {@code toCanonicalID} and Java serialization.</li>
 * </ul>
 */
public class RangeFilterTest {

    /**
     * Adapter over the four concrete filters so the same assertions can be run for each numeric type.
     */
    enum Kind {
        INT {
            @Override
            RangeFilter<?> create(double lo, boolean loInc, double hi, boolean hiInc) {
                return new IntRangeFilter((int) lo, loInc, (int) hi, hiInc);
            }

            @Override
            Number box(double v) {
                return (int) v;
            }
        },
        LONG {
            @Override
            RangeFilter<?> create(double lo, boolean loInc, double hi, boolean hiInc) {
                return new LongRangeFilter((long) lo, loInc, (long) hi, hiInc);
            }

            @Override
            Number box(double v) {
                return (long) v;
            }
        },
        FLOAT {
            @Override
            RangeFilter<?> create(double lo, boolean loInc, double hi, boolean hiInc) {
                return new FloatRangeFilter((float) lo, loInc, (float) hi, hiInc);
            }

            @Override
            Number box(double v) {
                return (float) v;
            }
        },
        DOUBLE {
            @Override
            RangeFilter<?> create(double lo, boolean loInc, double hi, boolean hiInc) {
                return new DoubleRangeFilter(lo, loInc, hi, hiInc);
            }

            @Override
            Number box(double v) {
                return v;
            }
        };

        abstract RangeFilter<?> create(double lo, boolean loInc, double hi, boolean hiInc);

        /** Boxes {@code v} into the exact wrapper type the filter expects. */
        abstract Number box(double v);

        boolean isIntegral() {
            return this == INT || this == LONG;
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean isValid(RangeFilter<?> filter, Number value) {
        return ((RangeFilter<Number>) filter).isValid(value);
    }

    @SuppressWarnings("unchecked")
    private static Number validate(RangeFilter<?> filter, Number value) {
        return ((RangeFilter<Number>) filter).validate(value);
    }

    @SuppressWarnings("unchecked")
    private static Number encode(RangeFilter<?> filter, Number value) {
        return ((RangeFilter<Number>) filter).encode(value);
    }

    private static void assertValid(RangeFilter<?> filter, Number value) {
        assertTrue(isValid(filter, value), filter + " should accept " + value);
        assertEquals(value, validate(filter, value), filter + " validate should echo " + value);
        assertEquals(value, encode(filter, value), filter + " encode should echo " + value);
    }

    private static void assertInvalid(RangeFilter<?> filter, Number value) {
        assertFalse(isValid(filter, value), filter + " should reject " + value);
        assertThrows(IllegalArgumentException.class, () -> validate(filter, value),
                filter + " validate should reject " + value);
        assertThrows(IllegalArgumentException.class, () -> encode(filter, value),
                filter + " encode should reject " + value);
    }

    /** Every Kind crossed with every inclusive/exclusive combination: 16 cases. */
    static Stream<Arguments> kindsAndInclusivity() {
        List<Arguments> args = new ArrayList<>();
        for (Kind kind : Kind.values()) {
            for (boolean loInc : new boolean[]{true, false}) {
                for (boolean hiInc : new boolean[]{true, false}) {
                    args.add(Arguments.of(kind, loInc, hiInc));
                }
            }
        }
        return args.stream();
    }

    // ------------------------------------------------------------------
    // Boundary semantics, all types x all inclusivity combinations
    // ------------------------------------------------------------------

    @ParameterizedTest(name = "{0} lowerInclusive={1} upperInclusive={2}")
    @MethodSource("kindsAndInclusivity")
    public void testBoundaries(Kind kind, boolean loInc, boolean hiInc) {
        RangeFilter<?> f = kind.create(1, loInc, 100, hiInc);

        // metadata reflects construction
        assertEquals(loInc, f.isLowerLimitInclusive());
        assertEquals(hiInc, f.isUpperLimitInclusive());
        assertEquals(kind.box(1), f.getLowerLimit());
        assertEquals(kind.box(100), f.getUpperLimit());

        // strictly inside is always valid
        assertValid(f, kind.box(2));
        assertValid(f, kind.box(50));
        assertValid(f, kind.box(99));

        // exact boundaries follow the inclusivity flags
        if (loInc) {
            assertValid(f, kind.box(1));
        } else {
            assertInvalid(f, kind.box(1));
        }
        if (hiInc) {
            assertValid(f, kind.box(100));
        } else {
            assertInvalid(f, kind.box(100));
        }

        // outside is always invalid, regardless of inclusivity
        assertInvalid(f, kind.box(0));
        assertInvalid(f, kind.box(-1));
        assertInvalid(f, kind.box(101));
        assertInvalid(f, kind.box(200));
        assertInvalid(f, kind.box(1000));

        // fractional neighbours of the boundaries only matter for floating types
        if (!kind.isIntegral()) {
            assertValid(f, kind.box(1.5));
            assertValid(f, kind.box(99.5));
            assertInvalid(f, kind.box(0.5));
            assertInvalid(f, kind.box(100.5));
        }
    }

    @ParameterizedTest(name = "{0} lowerInclusive={1} upperInclusive={2}")
    @MethodSource("kindsAndInclusivity")
    public void testNegativeRange(Kind kind, boolean loInc, boolean hiInc) {
        RangeFilter<?> f = kind.create(-100, loInc, -1, hiInc);

        assertValid(f, kind.box(-50));
        assertValid(f, kind.box(-99));
        assertValid(f, kind.box(-2));

        assertEquals(loInc, isValid(f, kind.box(-100)));
        assertEquals(hiInc, isValid(f, kind.box(-1)));

        assertInvalid(f, kind.box(-101));
        assertInvalid(f, kind.box(0));
        assertInvalid(f, kind.box(1));
    }

    @ParameterizedTest(name = "{0} lowerInclusive={1} upperInclusive={2}")
    @MethodSource("kindsAndInclusivity")
    public void testRangeSpanningZero(Kind kind, boolean loInc, boolean hiInc) {
        RangeFilter<?> f = kind.create(-10, loInc, 10, hiInc);

        assertValid(f, kind.box(0));
        assertValid(f, kind.box(-9));
        assertValid(f, kind.box(9));

        assertEquals(loInc, isValid(f, kind.box(-10)));
        assertEquals(hiInc, isValid(f, kind.box(10)));

        assertInvalid(f, kind.box(-11));
        assertInvalid(f, kind.box(11));
    }

    // ------------------------------------------------------------------
    // Degenerate ranges
    // ------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(Kind.class)
    public void testSinglePointRangeBothInclusive(Kind kind) {
        RangeFilter<?> f = kind.create(5, true, 5, true);

        assertValid(f, kind.box(5));
        assertInvalid(f, kind.box(4));
        assertInvalid(f, kind.box(6));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    public void testSinglePointRangeAnyExclusiveIsEmpty(Kind kind) {
        RangeFilter<?>[] empties = {
                kind.create(5, false, 5, true),
                kind.create(5, true, 5, false),
                kind.create(5, false, 5, false),
        };

        for (RangeFilter<?> f : empties) {
            assertInvalid(f, kind.box(5));
            assertInvalid(f, kind.box(4));
            assertInvalid(f, kind.box(6));
        }
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    public void testInvertedRangeRejectsEverything(Kind kind) {
        // lower > upper: no value can satisfy both limits
        RangeFilter<?> f = kind.create(100, true, 1, true);

        for (double v : new double[]{0, 1, 2, 50, 99, 100, 101}) {
            assertInvalid(f, kind.box(v));
        }
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    public void testAdjacentIntegersExclusiveBothSides(Kind kind) {
        // ]1, 2[ contains no integer but does contain fractions for floating types
        RangeFilter<?> f = kind.create(1, false, 2, false);

        assertInvalid(f, kind.box(1));
        assertInvalid(f, kind.box(2));
        if (!kind.isIntegral()) {
            assertValid(f, kind.box(1.5));
        }
    }

    // ------------------------------------------------------------------
    // Null handling
    // ------------------------------------------------------------------

    @ParameterizedTest(name = "{0} lowerInclusive={1} upperInclusive={2}")
    @MethodSource("kindsAndInclusivity")
    public void testNullIsNeverValid(Kind kind, boolean loInc, boolean hiInc) {
        RangeFilter<?> f = kind.create(1, loInc, 100, hiInc);

        // isValid must answer false for null rather than blow up on unboxing
        assertFalse(isValid(f, null), f + " isValid(null)");

        // validate/encode reject null as an out-of-range value
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> validate(f, null));
        assertTrue(e.getMessage().contains("null"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> encode(f, null));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    public void testNullIsRejectedEvenByAllEncompassingRange(Kind kind) {
        RangeFilter<?> f;
        switch (kind) {
            case INT:
                f = new IntRangeFilter(Integer.MIN_VALUE, true, Integer.MAX_VALUE, true);
                break;
            case LONG:
                f = new LongRangeFilter(Long.MIN_VALUE, true, Long.MAX_VALUE, true);
                break;
            case FLOAT:
                f = new FloatRangeFilter(Float.NEGATIVE_INFINITY, true, Float.POSITIVE_INFINITY, true);
                break;
            default:
                f = new DoubleRangeFilter(Double.NEGATIVE_INFINITY, true, Double.POSITIVE_INFINITY, true);
        }
        assertFalse(isValid(f, null), f + " isValid(null)");
        assertThrows(IllegalArgumentException.class, () -> validate(f, null));
    }

    // ------------------------------------------------------------------
    // Type extremes
    // ------------------------------------------------------------------

    @Test
    public void testIntExtremes() {
        IntRangeFilter all = new IntRangeFilter(Integer.MIN_VALUE, true, Integer.MAX_VALUE, true);
        assertTrue(all.isValid(Integer.MIN_VALUE));
        assertTrue(all.isValid(Integer.MAX_VALUE));
        assertTrue(all.isValid(0));

        IntRangeFilter open = new IntRangeFilter(Integer.MIN_VALUE, false, Integer.MAX_VALUE, false);
        assertFalse(open.isValid(Integer.MIN_VALUE));
        assertFalse(open.isValid(Integer.MAX_VALUE));
        assertTrue(open.isValid(Integer.MIN_VALUE + 1));
        assertTrue(open.isValid(Integer.MAX_VALUE - 1));
    }

    @Test
    public void testLongExtremes() {
        LongRangeFilter all = new LongRangeFilter(Long.MIN_VALUE, true, Long.MAX_VALUE, true);
        assertTrue(all.isValid(Long.MIN_VALUE));
        assertTrue(all.isValid(Long.MAX_VALUE));
        assertTrue(all.isValid(0L));

        LongRangeFilter open = new LongRangeFilter(Long.MIN_VALUE, false, Long.MAX_VALUE, false);
        assertFalse(open.isValid(Long.MIN_VALUE));
        assertFalse(open.isValid(Long.MAX_VALUE));
        assertTrue(open.isValid(Long.MIN_VALUE + 1));
        assertTrue(open.isValid(Long.MAX_VALUE - 1));

        // values beyond int range must not be truncated
        LongRangeFilter big = new LongRangeFilter(1L << 40, true, 1L << 41, true);
        assertTrue(big.isValid(1L << 40));
        assertTrue(big.isValid((1L << 40) + 12345L));
        assertFalse(big.isValid((1L << 40) - 1));
        assertFalse(big.isValid((1L << 41) + 1));
    }

    @Test
    public void testFloatExtremesAndSpecials() {
        FloatRangeFilter all = new FloatRangeFilter(-Float.MAX_VALUE, true, Float.MAX_VALUE, true);
        assertTrue(all.isValid(-Float.MAX_VALUE));
        assertTrue(all.isValid(Float.MAX_VALUE));
        assertTrue(all.isValid(Float.MIN_VALUE));
        assertTrue(all.isValid(0f));
        assertTrue(all.isValid(-0f));
        // infinities lie outside any finite range
        assertFalse(all.isValid(Float.POSITIVE_INFINITY));
        assertFalse(all.isValid(Float.NEGATIVE_INFINITY));
        // NaN compares false against everything, so it is never in range
        assertFalse(all.isValid(Float.NaN));

        FloatRangeFilter infinite = new FloatRangeFilter(Float.NEGATIVE_INFINITY, true, Float.POSITIVE_INFINITY, true);
        assertTrue(infinite.isValid(Float.POSITIVE_INFINITY));
        assertTrue(infinite.isValid(Float.NEGATIVE_INFINITY));
        assertTrue(infinite.isValid(Float.MAX_VALUE));
        assertFalse(infinite.isValid(Float.NaN));

        FloatRangeFilter infiniteOpen = new FloatRangeFilter(Float.NEGATIVE_INFINITY, false, Float.POSITIVE_INFINITY, false);
        assertFalse(infiniteOpen.isValid(Float.POSITIVE_INFINITY));
        assertFalse(infiniteOpen.isValid(Float.NEGATIVE_INFINITY));
        assertTrue(infiniteOpen.isValid(Float.MAX_VALUE));
    }

    @Test
    public void testDoubleExtremesAndSpecials() {
        DoubleRangeFilter all = new DoubleRangeFilter(-Double.MAX_VALUE, true, Double.MAX_VALUE, true);
        assertTrue(all.isValid(-Double.MAX_VALUE));
        assertTrue(all.isValid(Double.MAX_VALUE));
        assertTrue(all.isValid(Double.MIN_VALUE));
        assertTrue(all.isValid(0d));
        assertTrue(all.isValid(-0d));
        assertFalse(all.isValid(Double.POSITIVE_INFINITY));
        assertFalse(all.isValid(Double.NEGATIVE_INFINITY));
        assertFalse(all.isValid(Double.NaN));

        DoubleRangeFilter infinite = new DoubleRangeFilter(Double.NEGATIVE_INFINITY, true, Double.POSITIVE_INFINITY, true);
        assertTrue(infinite.isValid(Double.POSITIVE_INFINITY));
        assertTrue(infinite.isValid(Double.NEGATIVE_INFINITY));
        assertTrue(infinite.isValid(Double.MAX_VALUE));
        assertFalse(infinite.isValid(Double.NaN));

        DoubleRangeFilter infiniteOpen = new DoubleRangeFilter(Double.NEGATIVE_INFINITY, false, Double.POSITIVE_INFINITY, false);
        assertFalse(infiniteOpen.isValid(Double.POSITIVE_INFINITY));
        assertFalse(infiniteOpen.isValid(Double.NEGATIVE_INFINITY));
        assertTrue(infiniteOpen.isValid(Double.MAX_VALUE));
    }

    @Test
    public void testFloatingPrecisionNearBoundaries() {
        DoubleRangeFilter d = new DoubleRangeFilter(1, false, 100, false);
        assertTrue(d.isValid(Math.nextUp(1d)));
        assertTrue(d.isValid(Math.nextDown(100d)));
        assertFalse(d.isValid(1d));
        assertFalse(d.isValid(100d));
        assertFalse(d.isValid(Math.nextDown(1d)));
        assertFalse(d.isValid(Math.nextUp(100d)));

        FloatRangeFilter f = new FloatRangeFilter(1, false, 100, false);
        assertTrue(f.isValid(Math.nextUp(1f)));
        assertTrue(f.isValid(Math.nextDown(100f)));
        assertFalse(f.isValid(1f));
        assertFalse(f.isValid(100f));
        assertFalse(f.isValid(Math.nextDown(1f)));
        assertFalse(f.isValid(Math.nextUp(100f)));

        // fractional limits
        DoubleRangeFilter frac = new DoubleRangeFilter(0.1, true, 0.3, true);
        assertTrue(frac.isValid(0.1));
        assertTrue(frac.isValid(0.2));
        assertTrue(frac.isValid(0.3));
        assertFalse(frac.isValid(0.1 + 0.2 + 1e-9)); // 0.1 + 0.2 is already > 0.3 in binary
        assertFalse(frac.isValid(0.0999999));
    }

    // ------------------------------------------------------------------
    // Original fixtures kept for regression parity
    // ------------------------------------------------------------------

    private final IntRangeFilter intOneToHundred = new IntRangeFilter(1, true, 100, false);
    private final FloatRangeFilter floatOneToHundred = new FloatRangeFilter(1, false, 100, false);
    private final DoubleRangeFilter doubleOneToHundred = new DoubleRangeFilter(1, true, 100, true);
    private final LongRangeFilter longOneToHundred = new LongRangeFilter(1, false, 100, true);

    @Test
    public void testIntFilterOriginalFixture() {
        for (int v : new int[]{1, 99, 50, 35}) {
            assertTrue(intOneToHundred.isValid(v), "" + v);
            assertEquals(v, intOneToHundred.validate(v).intValue());
        }
        for (int v : new int[]{200, 100, 0, 1000, -1}) {
            assertFalse(intOneToHundred.isValid(v), "" + v);
            assertThrows(IllegalArgumentException.class, () -> intOneToHundred.validate(v));
        }
    }

    @Test
    public void testFloatFilterOriginalFixture() {
        for (float v : new float[]{2, 99.99f, 50, 35, 1.0001f}) {
            assertTrue(floatOneToHundred.isValid(v), "" + v);
            assertEquals(v, floatOneToHundred.validate(v).floatValue());
        }
        for (float v : new float[]{200, 100, 0, 1, -1}) {
            assertFalse(floatOneToHundred.isValid(v), "" + v);
            assertThrows(IllegalArgumentException.class, () -> floatOneToHundred.validate(v));
        }
    }

    @Test
    public void testDoubleFilterOriginalFixture() {
        for (double v : new double[]{1, 99.99, 100, 50, 35}) {
            assertTrue(doubleOneToHundred.isValid(v), "" + v);
            assertEquals(v, doubleOneToHundred.validate(v).doubleValue());
        }
        for (double v : new double[]{-1, 0, 200, 100.0001, 0.9999}) {
            assertFalse(doubleOneToHundred.isValid(v), "" + v);
            assertThrows(IllegalArgumentException.class, () -> doubleOneToHundred.validate(v));
        }
    }

    @Test
    public void testLongFilterFixture() {
        for (long v : new long[]{2, 99, 100, 50, 35}) {
            assertTrue(longOneToHundred.isValid(v), "" + v);
            assertEquals(v, longOneToHundred.validate(v).longValue());
        }
        for (long v : new long[]{1, 0, -1, 101, 200, Long.MAX_VALUE, Long.MIN_VALUE}) {
            assertFalse(longOneToHundred.isValid(v), "" + v);
            assertThrows(IllegalArgumentException.class, () -> longOneToHundred.validate(v));
        }
    }

    // ------------------------------------------------------------------
    // Metadata: accessors, toString, toCanonicalID, serialization
    // ------------------------------------------------------------------

    @Test
    public void testAccessorsReturnExactBoxedTypes() {
        assertEquals(Integer.valueOf(1), new IntRangeFilter(1, true, 100, false).getLowerLimit());
        assertEquals(Integer.valueOf(100), new IntRangeFilter(1, true, 100, false).getUpperLimit());
        assertEquals(Long.valueOf(1), new LongRangeFilter(1, true, 100, false).getLowerLimit());
        assertEquals(Long.valueOf(100), new LongRangeFilter(1, true, 100, false).getUpperLimit());
        assertEquals(Float.valueOf(1.5f), new FloatRangeFilter(1.5f, true, 100.25f, false).getLowerLimit());
        assertEquals(Float.valueOf(100.25f), new FloatRangeFilter(1.5f, true, 100.25f, false).getUpperLimit());
        assertEquals(Double.valueOf(1.5), new DoubleRangeFilter(1.5, true, 100.25, false).getLowerLimit());
        assertEquals(Double.valueOf(100.25), new DoubleRangeFilter(1.5, true, 100.25, false).getUpperLimit());
    }

    @Test
    public void testToCanonicalIDIsSimpleClassName() {
        assertEquals("IntRangeFilter", new IntRangeFilter(1, true, 2, true).toCanonicalID());
        assertEquals("LongRangeFilter", new LongRangeFilter(1, true, 2, true).toCanonicalID());
        assertEquals("FloatRangeFilter", new FloatRangeFilter(1, true, 2, true).toCanonicalID());
        assertEquals("DoubleRangeFilter", new DoubleRangeFilter(1, true, 2, true).toCanonicalID());
    }

    @Test
    public void testToStringUsesIntervalNotation() {
        // "[" / "]" on the left means inclusive / exclusive lower bound; mirrored on the right
        assertEquals("IntRangeFilter:[1, 100]", new IntRangeFilter(1, true, 100, true).toString());
        assertEquals("IntRangeFilter:[1, 100[", new IntRangeFilter(1, true, 100, false).toString());
        assertEquals("IntRangeFilter:]1, 100]", new IntRangeFilter(1, false, 100, true).toString());
        assertEquals("IntRangeFilter:]1, 100[", new IntRangeFilter(1, false, 100, false).toString());
        assertEquals("LongRangeFilter:[-5, 5]", new LongRangeFilter(-5, true, 5, true).toString());
        assertEquals("FloatRangeFilter:[1.5, 2.5[", new FloatRangeFilter(1.5f, true, 2.5f, false).toString());
        assertEquals("DoubleRangeFilter:]0.25, 0.75]", new DoubleRangeFilter(0.25, false, 0.75, true).toString());
    }

    @Test
    public void testValidateErrorMessageContainsValue() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new IntRangeFilter(1, true, 100, true).validate(500));
        assertTrue(e.getMessage().contains("500"), e.getMessage());
    }

    @ParameterizedTest(name = "{0} lowerInclusive={1} upperInclusive={2}")
    @MethodSource("kindsAndInclusivity")
    public void testSerializationRoundTrip(Kind kind, boolean loInc, boolean hiInc) throws Exception {
        RangeFilter<?> original = kind.create(-7, loInc, 42, hiInc);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }
        RangeFilter<?> copy;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copy = (RangeFilter<?>) in.readObject();
        }

        assertEquals(original.getClass(), copy.getClass());
        assertEquals(original.getLowerLimit(), copy.getLowerLimit());
        assertEquals(original.getUpperLimit(), copy.getUpperLimit());
        assertEquals(original.isLowerLimitInclusive(), copy.isLowerLimitInclusive());
        assertEquals(original.isUpperLimitInclusive(), copy.isUpperLimitInclusive());
        assertEquals(original.toString(), copy.toString());

        // the copy behaves identically
        for (double v : new double[]{-8, -7, -6, 0, 41, 42, 43}) {
            assertEquals(isValid(original, kind.box(v)), isValid(copy, kind.box(v)), "value " + v);
        }
    }
}
