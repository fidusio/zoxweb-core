package org.zoxweb.shared.data;


import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SUS;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RangeTest {
    @Test
    public void simpleRange() throws IOException {
        Range<Integer> intRange = new Range<>(1, 100, Range.Inclusivity.BOTH);
        String json = GSONUtil.toJSON(intRange, true);
        System.out.println(json);
        intRange = GSONUtil.fromJSON(json, Range.class);
        json = GSONUtil.toJSON(intRange, true);
        System.out.println(json);
        assert(intRange.getStart() instanceof Integer);
        assert(intRange.getEnd() instanceof Integer);
        assert(!intRange.within(500));
        assert(!intRange.within(0));
        assert(intRange.within(50));
        assert(intRange.within(99));
        assert(intRange.within(1));

        System.out.println(intRange);
    }

    @Test
    public void rangeMatch()
    {
        String[] values =
                {
                  "(1,2)",
                  "[3,4]",
                  "(-5,6]",
                  "[ 10,    20.45  )  "
                };


        for (String value : values)
        {
            Range r = Range.toRange(value);
            Range rr = Range.toRange(r.toString());
            System.out.println(value+":" + Range.Inclusivity.match(value) + " " + r +","  + rr);
            System.out.println(SUS.toCanonicalID(':',rr.getInclusivity(), rr, rr.getStart(), rr.getEnd(), rr.getLoopStart(), rr.getLoopEnd()));

        }
    }

    @Test
    public void rangeFail()
    {

        String[] values =
                {
                        "((1,2)",
                        "3,4",
                        "(toto,6]",
                        "[true, false]"
                };

        for(String val: values)
            assertThrows(IllegalArgumentException.class, ()->Range.toRange(val));
    }

    @Test
    public void testTypes()
    {
        Range<Integer> intRange = Range.toRange("[4, 4]");
        assert(intRange.getStart().getClass() == Integer.class);
        Range<Long> longRange = Range.toRange("(1, 5000000000)");
        assert(longRange.getStart().getClass() == Long.class);
        Range<Float> floatRange = Range.toRange("(1, 50.45)");
        assert(floatRange.getStart().getClass() == Float.class);

        floatRange = Range.toRange("[10, -50.10]");
        assert(floatRange.getStart().getClass() == Float.class);

        intRange = Range.toRange("[4.6, 4.5]", Integer.class, "toto", "nounit");
        assert(intRange.getStart().getClass() == Integer.class);

    }

    @Test
    public void bothExclusionNotationsParseToTheSameRange()
    {
        String[][] pairs =
                {
                        {"(1,5)", "]1,5[", "NONE"},
                        {"(1,5]", "]1,5]", "END"},
                        {"[1,5)", "[1,5[", "START"},
                        {"[1,5]", "[1,5]", "BOTH"},
                        {"( -2.5 , 7 )", "] -2.5 , 7 [", "NONE"},
                };
        for (String[] p : pairs)
        {
            Range.Inclusivity expected = Range.Inclusivity.valueOf(p[2]);
            assertEquals(expected, Range.Inclusivity.match(p[0]), p[0]);
            assertEquals(expected, Range.Inclusivity.match(p[1]), p[1]);

            Range round = Range.toRange(p[0]);
            Range reversed = Range.toRange(p[1]);
            assertEquals(round.getInclusivity(), reversed.getInclusivity(), p[1]);
            assertEquals(round.getStart(), reversed.getStart(), p[1]);
            assertEquals(round.getEnd(), reversed.getEnd(), p[1]);
            // output stays in the round-bracket form and round-trips
            assertEquals(round.toString(), reversed.toString(), p[1]);
            assertEquals(reversed.toString(), Range.toRange(reversed.toString()).toString(), p[1]);
        }
    }

    @Test
    public void mixedNotationIsRejected()
    {
        String[] values = {"]1,5)", "(1,5[", "[1,5(", ")1,5]", "((1,5]]", "]]1,5[["};
        for (String val : values)
        {
            assertNull(Range.Inclusivity.match(val), val);
            assertThrows(IllegalArgumentException.class, () -> Range.toRange(val), val);
        }
    }

    @Test
    public void unsupportedOverrideThrows()
    {
        assertThrows(IllegalArgumentException.class, () -> Range.toRange("[1, 5]", Short.class, null, null));
        assertThrows(IllegalArgumentException.class, () -> Range.toRange("[1, 5]", java.math.BigDecimal.class, null, null));
    }

    @Test
    public void rangeIsAValueFilter()
    {
        Range<Long> r = Range.toRange("[5, 14]", Long.class, null, null);
        org.zoxweb.shared.filters.ValueFilter<Long, Long> vf = r;

        assertEquals(Long.valueOf(5), vf.validate(5L));
        assertEquals(Long.valueOf(14), vf.validate(14L));
        assertEquals(Long.valueOf(9), vf.encode(9L));
        assertTrue(vf.isValid(5L));
        assertTrue(vf.isValid(14L));
        assertFalse(vf.isValid(4L));
        assertFalse(vf.isValid(15L));
        assertFalse(vf.isValid(null));
        assertThrows(IllegalArgumentException.class, () -> vf.validate(4L));
        assertThrows(IllegalArgumentException.class, () -> vf.validate(15L));
        assertThrows(NullPointerException.class, () -> vf.validate(null));

        // exclusive bounds honoured through the filter face
        Range<Integer> open = Range.toRange("(1, 5)");
        assertFalse(open.isValid(1));
        assertFalse(open.isValid(5));
        assertTrue(open.isValid(3));

        // canonical id: explicit when set, otherwise the re-parsable interval notation
        assertEquals("[5, 14]", r.toCanonicalID());
        assertEquals(r.toString(), Range.toRange(r.toCanonicalID()).toString());
        r.setCanonicalID("trigger-window");
        assertEquals("trigger-window", r.toCanonicalID());

        // usable as an NVConfig filter: setValue runs validate
        NVConfig nvc = NVConfigManager.createNVConfig("hits", "hits", "Hits", true, true, false, Long.class,
                Range.toRange("[5, 14]", Long.class, null, null));
        assertEquals("[5, 14]", nvc.getValueFilter().toCanonicalID());
    }

    @Test
    public void intersectsCoversContainmentBothWays()
    {
        Range<Integer> inner = new Range<>(5, 10, Range.Inclusivity.BOTH);
        Range<Integer> outer = new Range<>(0, 20, Range.Inclusivity.BOTH);
        Range<Integer> overlap = new Range<>(8, 30, Range.Inclusivity.BOTH);
        Range<Integer> apart = new Range<>(11, 30, Range.Inclusivity.BOTH);

        // other range contains this one: no endpoint of outer lies inside inner
        assertTrue(inner.intersects(outer));
        // this range contains the other one
        assertTrue(outer.intersects(inner));
        // partial overlap, both directions
        assertTrue(inner.intersects(overlap));
        assertTrue(overlap.intersects(inner));
        // disjoint
        assertFalse(inner.intersects(apart));
        assertFalse(apart.intersects(inner));
        // touching endpoint, inclusive on both sides
        assertTrue(inner.intersects(new Range<>(10, 12, Range.Inclusivity.BOTH)));
        // touching endpoint, exclusive on this side
        assertFalse(new Range<>(5, 10, Range.Inclusivity.START).intersects(new Range<>(10, 12, Range.Inclusivity.BOTH)));
    }
}
