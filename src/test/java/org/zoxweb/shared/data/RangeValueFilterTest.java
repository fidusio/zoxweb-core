package org.zoxweb.shared.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.zoxweb.shared.data.Range.Inclusivity;
import org.zoxweb.shared.filters.ValueFilter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Range} as a {@link ValueFilter}: the cases that used to live in RangeFilterTest for the
 * four numeric RangeFilter subclasses, now run against Range over the same four boxed types.
 */
public class RangeValueFilterTest {

    enum Kind {
        INT {
            @Override Comparable<?> box(double v) { return (int) v; }
        },
        LONG {
            @Override Comparable<?> box(double v) { return (long) v; }
        },
        FLOAT {
            @Override Comparable<?> box(double v) { return (float) v; }
        },
        DOUBLE {
            @Override Comparable<?> box(double v) { return v; }
        };

        abstract Comparable<?> box(double v);

        boolean isIntegral() {
            return this == INT || this == LONG;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        Range<?> create(double lo, boolean loInc, double hi, boolean hiInc) {
            return new Range((Comparable) box(lo), (Comparable) box(hi), inclusivity(loInc, hiInc));
        }
    }

    static Inclusivity inclusivity(boolean loInc, boolean hiInc) {
        if (loInc && hiInc) return Inclusivity.BOTH;
        if (loInc) return Inclusivity.LEFT;
        if (hiInc) return Inclusivity.RIGHT;
        return Inclusivity.NONE;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean isValid(Range<?> range, Comparable<?> value) {
        return ((Range) range).isValid(value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object validate(Range<?> range, Comparable<?> value) {
        return ((Range) range).validate(value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object encode(Range<?> range, Comparable<?> value) {
        return ((Range) range).encode(value);
    }

    private static void assertValid(Range<?> range, Comparable<?> value) {
        assertTrue(isValid(range, value), range + " should accept " + value);
        assertEquals(value, validate(range, value), range + " validate should echo " + value);
        assertEquals(value, encode(range, value), range + " encode should echo " + value);
    }

    private static void assertInvalid(Range<?> range, Comparable<?> value) {
        assertFalse(isValid(range, value), range + " should reject " + value);
        assertThrows(IllegalArgumentException.class, () -> validate(range, value),
                range + " validate should reject " + value);
        assertThrows(IllegalArgumentException.class, () -> encode(range, value),
                range + " encode should reject " + value);
    }

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

    @ParameterizedTest(name = "{0} lowerInclusive={1} upperInclusive={2}")
    @MethodSource("kindsAndInclusivity")
    public void boundaries(Kind kind, boolean loInc, boolean hiInc) {
        Range<?> f = kind.create(1, loInc, 100, hiInc);
        assertEquals(inclusivity(loInc, hiInc), f.getInclusivity());
        assertEquals(kind.box(1), f.getStart());
        assertEquals(kind.box(100), f.getEnd());

        assertValid(f, kind.box(2));
        assertValid(f, kind.box(50));
        assertValid(f, kind.box(99));

        if (loInc) assertValid(f, kind.box(1)); else assertInvalid(f, kind.box(1));
        if (hiInc) assertValid(f, kind.box(100)); else assertInvalid(f, kind.box(100));

        assertInvalid(f, kind.box(0));
        assertInvalid(f, kind.box(-1));
        assertInvalid(f, kind.box(101));
        assertInvalid(f, kind.box(200));
        assertInvalid(f, kind.box(1000));

        if (!kind.isIntegral()) {
            assertValid(f, kind.box(1.5));
            assertValid(f, kind.box(99.5));
            assertInvalid(f, kind.box(0.5));
            assertInvalid(f, kind.box(100.5));
        }
    }

    @ParameterizedTest(name = "{0} lowerInclusive={1} upperInclusive={2}")
    @MethodSource("kindsAndInclusivity")
    public void negativeRange(Kind kind, boolean loInc, boolean hiInc) {
        Range<?> f = kind.create(-100, loInc, -1, hiInc);
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
    public void rangeSpanningZero(Kind kind, boolean loInc, boolean hiInc) {
        Range<?> f = kind.create(-10, loInc, 10, hiInc);
        assertValid(f, kind.box(0));
        assertValid(f, kind.box(-9));
        assertValid(f, kind.box(9));
        assertEquals(loInc, isValid(f, kind.box(-10)));
        assertEquals(hiInc, isValid(f, kind.box(10)));
        assertInvalid(f, kind.box(-11));
        assertInvalid(f, kind.box(11));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    public void singlePointRangeBothInclusive(Kind kind) {
        Range<?> f = kind.create(5, true, 5, true);
        assertValid(f, kind.box(5));
        assertInvalid(f, kind.box(4));
        assertInvalid(f, kind.box(6));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    public void singlePointRangeAnyExclusiveIsEmpty(Kind kind) {
        Range<?>[] empties = {
                kind.create(5, false, 5, true),
                kind.create(5, true, 5, false),
                kind.create(5, false, 5, false),
        };
        for (Range<?> f : empties) {
            assertInvalid(f, kind.box(5));
            assertInvalid(f, kind.box(4));
            assertInvalid(f, kind.box(6));
        }
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    public void invertedBoundsAreSwitchedByTheConstructor(Kind kind) {
        // RangeFilter rejected everything for an inverted range; Range normalizes it instead
        Range<?> f = kind.create(100, true, 1, true);
        assertEquals(kind.box(1), f.getStart());
        assertEquals(kind.box(100), f.getEnd());
        assertValid(f, kind.box(50));
    }

    @ParameterizedTest
    @EnumSource(Kind.class)
    public void adjacentIntegersExclusiveBothSides(Kind kind) {
        Range<?> f = kind.create(1, false, 2, false);
        assertInvalid(f, kind.box(1));
        assertInvalid(f, kind.box(2));
        if (!kind.isIntegral()) {
            assertValid(f, kind.box(1.5));
        }
    }

    @ParameterizedTest(name = "{0} lowerInclusive={1} upperInclusive={2}")
    @MethodSource("kindsAndInclusivity")
    public void nullIsNeverValid(Kind kind, boolean loInc, boolean hiInc) {
        Range<?> f = kind.create(1, loInc, 100, hiInc);
        assertFalse(isValid(f, null), f + " isValid(null)");
        NullPointerException e = assertThrows(NullPointerException.class, () -> validate(f, null));
        assertTrue(e.getMessage().contains("Null"), e.getMessage());
        assertThrows(NullPointerException.class, () -> encode(f, null));
    }

    @Test
    public void intExtremes() {
        Range<Integer> all = new Range<>(Integer.MIN_VALUE, Integer.MAX_VALUE, Inclusivity.BOTH);
        assertTrue(all.isValid(Integer.MIN_VALUE));
        assertTrue(all.isValid(Integer.MAX_VALUE));
        assertTrue(all.isValid(0));

        Range<Integer> open = new Range<>(Integer.MIN_VALUE, Integer.MAX_VALUE, Inclusivity.NONE);
        assertFalse(open.isValid(Integer.MIN_VALUE));
        assertFalse(open.isValid(Integer.MAX_VALUE));
        assertTrue(open.isValid(Integer.MIN_VALUE + 1));
        assertTrue(open.isValid(Integer.MAX_VALUE - 1));
    }

    @Test
    public void longExtremes() {
        Range<Long> all = new Range<>(Long.MIN_VALUE, Long.MAX_VALUE, Inclusivity.BOTH);
        assertTrue(all.isValid(Long.MIN_VALUE));
        assertTrue(all.isValid(Long.MAX_VALUE));
        assertTrue(all.isValid(0L));

        Range<Long> open = new Range<>(Long.MIN_VALUE, Long.MAX_VALUE, Inclusivity.NONE);
        assertFalse(open.isValid(Long.MIN_VALUE));
        assertFalse(open.isValid(Long.MAX_VALUE));
        assertTrue(open.isValid(Long.MIN_VALUE + 1));
        assertTrue(open.isValid(Long.MAX_VALUE - 1));

        Range<Long> big = new Range<>(1L << 40, 1L << 41, Inclusivity.BOTH);
        assertTrue(big.isValid(1L << 40));
        assertTrue(big.isValid((1L << 40) + 12345L));
        assertFalse(big.isValid((1L << 40) - 1));
        assertFalse(big.isValid((1L << 41) + 1));
    }

    @Test
    public void floatExtremesAndSpecials() {
        Range<Float> all = new Range<>(-Float.MAX_VALUE, Float.MAX_VALUE, Inclusivity.BOTH);
        assertTrue(all.isValid(-Float.MAX_VALUE));
        assertTrue(all.isValid(Float.MAX_VALUE));
        assertTrue(all.isValid(Float.MIN_VALUE));
        assertTrue(all.isValid(0f));
        assertTrue(all.isValid(-0f));
        assertFalse(all.isValid(Float.POSITIVE_INFINITY));
        assertFalse(all.isValid(Float.NEGATIVE_INFINITY));
        assertFalse(all.isValid(Float.NaN));

        Range<Float> infinite = new Range<>(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Inclusivity.BOTH);
        assertTrue(infinite.isValid(Float.POSITIVE_INFINITY));
        assertTrue(infinite.isValid(Float.NEGATIVE_INFINITY));
        assertTrue(infinite.isValid(Float.MAX_VALUE));
        assertFalse(infinite.isValid(Float.NaN));

        Range<Float> infiniteOpen = new Range<>(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Inclusivity.NONE);
        assertFalse(infiniteOpen.isValid(Float.POSITIVE_INFINITY));
        assertFalse(infiniteOpen.isValid(Float.NEGATIVE_INFINITY));
        assertTrue(infiniteOpen.isValid(Float.MAX_VALUE));
    }

    @Test
    public void doubleExtremesAndSpecials() {
        Range<Double> all = new Range<>(-Double.MAX_VALUE, Double.MAX_VALUE, Inclusivity.BOTH);
        assertTrue(all.isValid(-Double.MAX_VALUE));
        assertTrue(all.isValid(Double.MAX_VALUE));
        assertTrue(all.isValid(Double.MIN_VALUE));
        assertTrue(all.isValid(0d));
        assertTrue(all.isValid(-0d));
        assertFalse(all.isValid(Double.POSITIVE_INFINITY));
        assertFalse(all.isValid(Double.NEGATIVE_INFINITY));
        assertFalse(all.isValid(Double.NaN));

        Range<Double> infinite = new Range<>(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Inclusivity.BOTH);
        assertTrue(infinite.isValid(Double.POSITIVE_INFINITY));
        assertTrue(infinite.isValid(Double.NEGATIVE_INFINITY));
        assertTrue(infinite.isValid(Double.MAX_VALUE));
        assertFalse(infinite.isValid(Double.NaN));

        Range<Double> infiniteOpen = new Range<>(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Inclusivity.NONE);
        assertFalse(infiniteOpen.isValid(Double.POSITIVE_INFINITY));
        assertFalse(infiniteOpen.isValid(Double.NEGATIVE_INFINITY));
        assertTrue(infiniteOpen.isValid(Double.MAX_VALUE));
    }

    @Test
    public void floatingPrecisionNearBoundaries() {
        Range<Double> d = new Range<>(1d, 100d, Inclusivity.NONE);
        assertTrue(d.isValid(Math.nextUp(1d)));
        assertTrue(d.isValid(Math.nextDown(100d)));
        assertFalse(d.isValid(1d));
        assertFalse(d.isValid(100d));
        assertFalse(d.isValid(Math.nextDown(1d)));
        assertFalse(d.isValid(Math.nextUp(100d)));

        Range<Float> f = new Range<>(1f, 100f, Inclusivity.NONE);
        assertTrue(f.isValid(Math.nextUp(1f)));
        assertTrue(f.isValid(Math.nextDown(100f)));
        assertFalse(f.isValid(1f));
        assertFalse(f.isValid(100f));
        assertFalse(f.isValid(Math.nextDown(1f)));
        assertFalse(f.isValid(Math.nextUp(100f)));

        Range<Double> frac = new Range<>(0.1, 0.3, Inclusivity.BOTH);
        assertTrue(frac.isValid(0.1));
        assertTrue(frac.isValid(0.2));
        assertTrue(frac.isValid(0.3));
        assertFalse(frac.isValid(0.1 + 0.2 + 1e-9)); // 0.1 + 0.2 is already > 0.3 in binary
        assertFalse(frac.isValid(0.0999999));
    }

    @Test
    public void originalFixtures() {
        Range<Integer> intOneToHundred = new Range<>(1, 100, Inclusivity.LEFT);
        for (int v : new int[]{1, 99, 50, 35}) {
            assertTrue(intOneToHundred.isValid(v), "" + v);
            assertEquals(v, intOneToHundred.validate(v).intValue());
        }
        for (int v : new int[]{200, 100, 0, 1000, -1}) {
            assertFalse(intOneToHundred.isValid(v), "" + v);
            assertThrows(IllegalArgumentException.class, () -> intOneToHundred.validate(v));
        }

        Range<Float> floatOneToHundred = new Range<>(1f, 100f, Inclusivity.NONE);
        for (float v : new float[]{2, 99.99f, 50, 35, 1.0001f}) {
            assertTrue(floatOneToHundred.isValid(v), "" + v);
            assertEquals(v, floatOneToHundred.validate(v).floatValue());
        }
        for (float v : new float[]{200, 100, 0, 1, -1}) {
            assertFalse(floatOneToHundred.isValid(v), "" + v);
            assertThrows(IllegalArgumentException.class, () -> floatOneToHundred.validate(v));
        }

        Range<Double> doubleOneToHundred = new Range<>(1d, 100d, Inclusivity.BOTH);
        for (double v : new double[]{1, 99.99, 100, 50, 35}) {
            assertTrue(doubleOneToHundred.isValid(v), "" + v);
            assertEquals(v, doubleOneToHundred.validate(v).doubleValue());
        }
        for (double v : new double[]{-1, 0, 200, 100.0001, 0.9999}) {
            assertFalse(doubleOneToHundred.isValid(v), "" + v);
            assertThrows(IllegalArgumentException.class, () -> doubleOneToHundred.validate(v));
        }

        Range<Long> longOneToHundred = new Range<>(1L, 100L, Inclusivity.RIGHT);
        for (long v : new long[]{2, 99, 100, 50, 35}) {
            assertTrue(longOneToHundred.isValid(v), "" + v);
            assertEquals(v, longOneToHundred.validate(v).longValue());
        }
        for (long v : new long[]{1, 0, -1, 101, 200, Long.MAX_VALUE, Long.MIN_VALUE}) {
            assertFalse(longOneToHundred.isValid(v), "" + v);
            assertThrows(IllegalArgumentException.class, () -> longOneToHundred.validate(v));
        }
    }

    @Test
    public void accessorsReturnExactBoxedTypes() {
        assertEquals(Integer.valueOf(1), new Range<>(1, 100, Inclusivity.LEFT).getStart());
        assertEquals(Integer.valueOf(100), new Range<>(1, 100, Inclusivity.LEFT).getEnd());
        assertEquals(Long.valueOf(1), new Range<>(1L, 100L, Inclusivity.LEFT).getStart());
        assertEquals(Long.valueOf(100), new Range<>(1L, 100L, Inclusivity.LEFT).getEnd());
        assertEquals(Float.valueOf(1.5f), new Range<>(1.5f, 100.25f, Inclusivity.LEFT).getStart());
        assertEquals(Float.valueOf(100.25f), new Range<>(1.5f, 100.25f, Inclusivity.LEFT).getEnd());
        assertEquals(Double.valueOf(1.5), new Range<>(1.5, 100.25, Inclusivity.LEFT).getStart());
        assertEquals(Double.valueOf(100.25), new Range<>(1.5, 100.25, Inclusivity.LEFT).getEnd());
    }

    @Test
    public void canonicalIDAndToStringUseIntervalNotation() {
        assertEquals("[1, 100]", new Range<>(1, 100, Inclusivity.BOTH).toString());
        assertEquals("[1, 100)", new Range<>(1, 100, Inclusivity.LEFT).toString());
        assertEquals("(1, 100]", new Range<>(1, 100, Inclusivity.RIGHT).toString());
        assertEquals("(1, 100)", new Range<>(1, 100, Inclusivity.NONE).toString());
        assertEquals("[-5, 5]", new Range<>(-5L, 5L, Inclusivity.BOTH).toString());
        assertEquals("[1.5, 2.5)", new Range<>(1.5f, 2.5f, Inclusivity.LEFT).toString());
        assertEquals("(0.25, 0.75]", new Range<>(0.25, 0.75, Inclusivity.RIGHT).toString());
        // the filter's canonical id is the same notation, and it parses back
        Range<Integer> r = new Range<>(1, 100, Inclusivity.LEFT);
        assertEquals("[1, 100)", r.toCanonicalID());
        assertEquals(r.toString(), Range.toRange(r.toCanonicalID()).toString());
    }

    @Test
    public void validateErrorMessageContainsValue() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new Range<>(1, 100, Inclusivity.BOTH).validate(500));
        assertTrue(e.getMessage().contains("500"), e.getMessage());
    }

    @ParameterizedTest(name = "{0} lowerInclusive={1} upperInclusive={2}")
    @MethodSource("kindsAndInclusivity")
    public void javaSerializationRoundTrip(Kind kind, boolean loInc, boolean hiInc) throws Exception {
        Range<?> original = kind.create(-7, loInc, 42, hiInc);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }
        Range<?> copy;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copy = (Range<?>) in.readObject();
        }

        assertEquals(original.getStart(), copy.getStart());
        assertEquals(original.getEnd(), copy.getEnd());
        assertEquals(original.getInclusivity(), copy.getInclusivity());
        assertEquals(original.toString(), copy.toString());
        for (double v : new double[]{-8, -7, -6, 0, 41, 42, 43}) {
            assertEquals(isValid(original, kind.box(v)), isValid(copy, kind.box(v)), "value " + v);
        }
    }
}
