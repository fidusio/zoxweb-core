package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.filters.GetValueFilter;
import org.zoxweb.shared.filters.ValueFilter;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link SUS#toNVGenericMap(Enum[])}: an enum constant becomes the NV matching its filter type,
 * its String default parsed into that type.
 */
public class SUSEnumToNVGenericMapTest {

    private enum Param implements GetNameValue<String>, GetValueFilter<String, String> {
        PORT("port", "8080", FilterType.INTEGER),
        TIMEOUT("timeout", "5000", FilterType.LONG),
        RATIO("ratio", "0.5", FilterType.DOUBLE),
        SCALE("scale", "1.5", FilterType.FLOAT),
        SECURE("secure", "true", FilterType.BOOLEAN),
        PRICE("price", "19.99", FilterType.BIG_DECIMAL),
        HOST("host", "localhost", FilterType.TEXT_NOT_EMPTY),
        NOTE("note", null, FilterType.CLEAR),
        BAD_PORT("bad_port", "not-a-number", FilterType.INTEGER),
        ;

        private final String name;
        private final String value;
        private final FilterType filter;

        Param(String name, String value, FilterType filter) {
            this.name = name;
            this.value = value;
            this.filter = filter;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public ValueFilter<String, String> getValueFilter() {
            return filter;
        }
    }

    private enum Plain {
        LEFT, RIGHT
    }

    private enum Defaulted implements GetNameValue<String> {
        LEVEL("level", "debug");

        private final String name;
        private final String value;

        Defaulted(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    @Test
    public void typedFiltersProduceTypedNVs() {
        NVGenericMap map = SUS.toNVGenericMap(Param.PORT, Param.TIMEOUT, Param.RATIO, Param.SCALE, Param.SECURE, Param.PRICE);

        assertEquals(6, map.size());

        assertTrue(map.get("port") instanceof NVInt);
        assertEquals(Integer.valueOf(8080), map.<Integer>getValue("port"));

        assertTrue(map.get("timeout") instanceof NVLong);
        assertEquals(Long.valueOf(5000L), map.<Long>getValue("timeout"));

        assertTrue(map.get("ratio") instanceof NVDouble);
        assertEquals(Double.valueOf(0.5), map.<Double>getValue("ratio"));

        assertTrue(map.get("scale") instanceof NVFloat);
        assertEquals(Float.valueOf(1.5f), map.<Float>getValue("scale"));

        assertTrue(map.get("secure") instanceof NVBoolean);
        assertEquals(Boolean.TRUE, map.<Boolean>getValue("secure"));

        assertTrue(map.get("price") instanceof NVBigDecimal);
        assertEquals(new BigDecimal("19.99"), map.<BigDecimal>getValue("price"));
    }

    @Test
    public void textFiltersProduceNVPairCarryingTheFilter() {
        NVGenericMap map = SUS.toNVGenericMap(Param.HOST, Param.NOTE);

        GetNameValue<?> host = map.get("host");
        assertTrue(host instanceof NVPair);
        assertEquals("localhost", host.getValue());
        assertSame(FilterType.TEXT_NOT_EMPTY, ((NVPair) host).getValueFilter());

        GetNameValue<?> note = map.get("note");
        assertTrue(note instanceof NVPair);
        assertNull(note.getValue());
        assertSame(FilterType.CLEAR, ((NVPair) note).getValueFilter());
    }

    @Test
    public void enumWithoutFilterOrValueBecomesPlainNVPair() {
        NVGenericMap map = SUS.toNVGenericMap(Plain.LEFT, Plain.RIGHT, Defaulted.LEVEL);

        GetNameValue<?> left = map.get("LEFT");
        assertTrue(left instanceof NVPair);
        assertEquals("LEFT", left.getName());
        assertNull(left.getValue());
        assertSame(FilterType.CLEAR, ((NVPair) left).getValueFilter(), "NVPair's default filter");

        assertNotNull(map.get("RIGHT"));

        GetNameValue<?> level = map.get("level");
        assertTrue(level instanceof NVPair);
        assertEquals("debug", level.getValue());
    }

    @Test
    public void unparsableDefaultForTypedFilterIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> SUS.toNVGenericMap(Param.BAD_PORT));
    }

    @Test
    public void emptyInputGivesEmptyMap() {
        assertEquals(0, SUS.toNVGenericMap().size());
    }
}
