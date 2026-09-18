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
package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CollectionAsArrayTest {

    private static CollectionAsArray<String> of(String... initial) {
        return new CollectionAsArray<>(new ArrayList<>(Arrays.asList(initial)), new String[0]);
    }

    @Test
    public void constructorRejectsNulls() {
        assertThrows(NullPointerException.class, () -> new CollectionAsArray<>(null, new String[0]));
        assertThrows(NullPointerException.class, () -> new CollectionAsArray<>(new ArrayList<>(), null));
    }

    @Test
    public void constructorRejectsNonEmptyTypeToken() {
        assertThrows(IllegalArgumentException.class,
                () -> new CollectionAsArray<>(new ArrayList<>(), new String[1]));
    }

    @Test
    public void constructorTakesInitialSnapshot() {
        CollectionAsArray<String> caa = of("a", "b");
        assertArrayEquals(new String[]{"a", "b"}, caa.asArray());
    }

    @Test
    public void emptyCollectionReturnsTheEmptyTokenInstance() {
        String[] empty = new String[0];
        CollectionAsArray<String> caa = new CollectionAsArray<>(new ArrayList<>(), empty);
        assertSame(empty, caa.asArray());
        caa.add("x").remove("x");
        assertSame(empty, caa.asArray());
        caa.add("y");
        caa.clear();
        assertSame(empty, caa.asArray());
    }

    @Test
    public void addRefreshesSnapshotAndBackingCollection() {
        List<String> backing = new ArrayList<>();
        CollectionAsArray<String> caa = new CollectionAsArray<>(backing, new String[0]);
        String[] before = caa.asArray();

        assertSame(caa, caa.add("a", "b"));

        assertArrayEquals(new String[]{"a", "b"}, caa.asArray());
        assertEquals(Arrays.asList("a", "b"), backing);
        assertNotSame(before, caa.asArray());
    }

    @Test
    public void addWithNoElementsStillPublishesFreshSnapshot() {
        CollectionAsArray<String> caa = of("a");
        String[] before = caa.asArray();
        caa.add();
        assertArrayEquals(before, caa.asArray());
    }

    @Test
    public void removeRefreshesSnapshotAndBackingCollection() {
        List<String> backing = new ArrayList<>(Arrays.asList("a", "b", "c"));
        CollectionAsArray<String> caa = new CollectionAsArray<>(backing, new String[0]);

        assertSame(caa, caa.remove("b", "zzz"));

        assertArrayEquals(new String[]{"a", "c"}, caa.asArray());
        assertEquals(Arrays.asList("a", "c"), backing);
    }

    @Test
    public void clearEmptiesSnapshotAndBackingCollection() {
        List<String> backing = new ArrayList<>(Arrays.asList("a", "b"));
        CollectionAsArray<String> caa = new CollectionAsArray<>(backing, new String[0]);
        caa.clear();
        assertEquals(0, caa.asArray().length);
        assertTrue(backing.isEmpty());
    }

    @Test
    public void snapshotIsImmutableAcrossMutations() {
        CollectionAsArray<String> caa = of("a", "b");
        String[] old = caa.asArray();
        caa.add("c");
        assertArrayEquals(new String[]{"a", "b"}, old);
        assertArrayEquals(new String[]{"a", "b", "c"}, caa.asArray());
    }

    @Test
    public void worksWithSetSemantics() {
        CollectionAsArray<String> caa =
                new CollectionAsArray<>(new LinkedHashSet<>(), new String[0]);
        caa.add("a", "a", "b");
        assertArrayEquals(new String[]{"a", "b"}, caa.asArray());
    }

    @Test
    public void containsUsesEquals() {
        CollectionAsArray<String> caa = of("Alpha", "beta");
        assertTrue(caa.contains("Alpha"));
        assertTrue(caa.contains("beta"));
        assertFalse(caa.contains("alpha"));
        assertFalse(caa.contains("gamma"));
    }

    @Test
    public void containsHandlesNullProbeAndNullElements() {
        CollectionAsArray<String> caa = of("a");
        assertFalse(caa.contains(null));
        caa.add((String) null);
        assertTrue(caa.contains(null));
        assertTrue(caa.contains("a"));
    }

    @Test
    public void containsReflectsMutations() {
        CollectionAsArray<String> caa = of();
        assertFalse(caa.contains("a"));
        caa.add("a");
        assertTrue(caa.contains("a"));
        caa.remove("a");
        assertFalse(caa.contains("a"));
    }

    @Test
    public void containsWithNullMatcherFallsBackToEquals() {
        CollectionAsArray<String> caa = of("Alpha");
        assertTrue(caa.contains("Alpha", null));
        assertFalse(caa.contains("alpha", null));
    }

    @Test
    public void containsWithIgnoreCaseMatcher() {
        CollectionAsArray<String> caa = of("Alpha", "beta");
        assertTrue(caa.contains("alpha", RefMatcher.IgnoreCase));
        assertTrue(caa.contains("BETA", RefMatcher.IgnoreCase));
        assertFalse(caa.contains("gamma", RefMatcher.IgnoreCase));
        assertFalse(caa.contains(null, RefMatcher.IgnoreCase));
    }

    @Test
    public void ignoreCaseMatcherToleratesNullElements() {
        CollectionAsArray<String> caa = of("a");
        caa.add((String) null);
        assertTrue(caa.contains("A", RefMatcher.IgnoreCase));
        assertFalse(caa.contains("b", RefMatcher.IgnoreCase));
        assertFalse(caa.contains(null, RefMatcher.IgnoreCase));
    }

    @Test
    public void matcherReceivesElementAsRefAndProbeAsTo() {
        final List<String> refs = new ArrayList<>();
        final List<String> tos = new ArrayList<>();
        RefMatcher<String, String> recorder = (ref, to) -> {
            refs.add(ref);
            tos.add(to);
            return false;
        };
        CollectionAsArray<String> caa = of("e1", "e2");

        assertFalse(caa.contains("probe", recorder));

        assertEquals(Arrays.asList("e1", "e2"), refs);
        assertEquals(Arrays.asList("probe", "probe"), tos);
    }

    @Test
    public void containsStopsAtFirstMatch() {
        final int[] calls = {0};
        RefMatcher<String, String> counting = (ref, to) -> {
            calls[0]++;
            return ref.equals(to);
        };
        CollectionAsArray<String> caa = of("a", "b", "c");
        assertTrue(caa.contains("a", counting));
        assertEquals(1, calls[0]);
    }

    @Test
    public void trimIgnoreCaseMatcherTrimsBothSides() {
        assertTrue(RefMatcher.TrimIgnoreCase.matches(" Foo ", "foo"));
        assertTrue(RefMatcher.TrimIgnoreCase.matches("foo", "\tFOO\n"));
        assertFalse(RefMatcher.TrimIgnoreCase.matches("foo", "bar"));
        assertFalse(RefMatcher.TrimIgnoreCase.matches("foo bar", "foobar"));
    }

    @Test
    public void trimIgnoreCaseMatcherBlankAndNullAreFalse() {
        assertFalse(RefMatcher.TrimIgnoreCase.matches("   ", "   "));
        assertFalse(RefMatcher.TrimIgnoreCase.matches("", ""));
        assertFalse(RefMatcher.TrimIgnoreCase.matches("foo", "   "));
        assertFalse(RefMatcher.TrimIgnoreCase.matches("   ", "foo"));
        assertFalse(RefMatcher.TrimIgnoreCase.matches(null, "foo"));
        assertFalse(RefMatcher.TrimIgnoreCase.matches("foo", null));
        assertFalse(RefMatcher.TrimIgnoreCase.matches(null, null));
    }

    @Test
    public void containsWithTrimIgnoreCaseMatcher() {
        CollectionAsArray<String> caa = of(" Alpha ", "beta");
        assertTrue(caa.contains("alpha", RefMatcher.TrimIgnoreCase));
        assertTrue(caa.contains("  BETA", RefMatcher.TrimIgnoreCase));
        assertFalse(caa.contains("gamma", RefMatcher.TrimIgnoreCase));
        assertFalse(caa.contains("   ", RefMatcher.TrimIgnoreCase));
    }
}
