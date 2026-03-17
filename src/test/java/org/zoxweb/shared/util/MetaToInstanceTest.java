package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.data.DeviceDAO;
import org.zoxweb.shared.filters.FilterType;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class MetaToInstanceTest {

    private enum TestEnum { A, B, C }

    // ==================== Scalar type validation ====================

    @Test
    public void testBooleanConfig() {
        NVConfig config = NVConfigManager.createNVConfig("flag", null, null, false, false, Boolean.class);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVBoolean.class, result);
        assertEquals("flag", result.getName());
        assertEquals(false, result.getValue());
    }

    @Test
    public void testIntegerConfig() {
        NVConfig config = NVConfigManager.createNVConfig("count", null, null, false, false, Integer.class);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVInt.class, result);
        assertEquals("count", result.getName());
        assertEquals(0, result.getValue());
    }

    @Test
    public void testLongConfig() {
        NVConfig config = NVConfigManager.createNVConfig("timestamp", null, null, false, false, Long.class);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVLong.class, result);
        assertEquals("timestamp", result.getName());
        assertEquals(0L, result.getValue());
    }

    @Test
    public void testFloatConfig() {
        NVConfig config = NVConfigManager.createNVConfig("ratio", null, null, false, false, Float.class);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVFloat.class, result);
        assertEquals("ratio", result.getName());
    }

    @Test
    public void testDoubleConfig() {
        NVConfig config = NVConfigManager.createNVConfig("amount", null, null, false, false, Double.class);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVDouble.class, result);
        assertEquals("amount", result.getName());
    }

    @Test
    public void testStringConfig() {
        NVConfig config = NVConfigManager.createNVConfig("name", null, null, false, false, String.class);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVPair.class, result);
        assertEquals("name", result.getName());
        assertNull(result.getValue());
    }

    @Test
    public void testStringWithValueFilter() {
        NVConfig config = NVConfigManager.createNVConfig("email", null, null, false, false, false, String.class, FilterType.EMAIL);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVPair.class, result);
        NVPair nvp = (NVPair) result;
        assertEquals("email", nvp.getName());
        assertNotNull(nvp.getValueFilter());
        assertEquals(FilterType.EMAIL, nvp.getValueFilter());
    }

    @Test
    public void testDateConfig() {
        NVConfig config = NVConfigManager.createNVConfig("created", null, null, false, false, Date.class);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVLong.class, result);
        assertEquals("created", result.getName());
    }

    @Test
    public void testBigDecimalConfig() {
        NVConfig config = NVConfigManager.createNVConfig("price", null, null, false, false, BigDecimal.class);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVBigDecimal.class, result);
        assertEquals("price", result.getName());
    }

    @Test
    public void testNumberConfig() {
        NVConfig config = NVConfigManager.createNVConfig("num", null, null, false, false, Number.class);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVNumber.class, result);
        assertEquals("num", result.getName());
    }

    @Test
    public void testEnumConfig() {
        NVConfig config = NVConfigManager.createNVConfig("status", null, null, false, false, TestEnum.class);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVEnum.class, result);
        assertEquals("status", result.getName());
    }

    // ==================== Composite scalar types ====================

    @Test
    public void testNVGenericMapConfig() {
        NVConfig config = NVConfigManager.createNVConfig("map", null, null, false, false, NVGenericMap.class);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVGenericMap.class, result);
        assertEquals("map", result.getName());
    }

    @Test
    public void testNVGenericMapListConfig() {
        NVConfig config = NVConfigManager.createNVConfig("maps", null, null, false, false, NVGenericMapList.class);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVGenericMapList.class, result);
        assertEquals("maps", result.getName());
    }

    @Test
    public void testNVStringListConfig() {
        NVConfig config = NVConfigManager.createNVConfig("tags", null, null, false, false, NVStringList.class);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVStringList.class, result);
        assertEquals("tags", result.getName());
    }

    @Test
    public void testNVStringSetConfig() {
        NVConfig config = NVConfigManager.createNVConfig("unique_tags", null, null, false, false, NVStringSet.class);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVStringSet.class, result);
        assertEquals("unique_tags", result.getName());
    }

    @Test
    public void testNamedValueConfig() {
        NVConfig config = NVConfigManager.createNVConfig("nv", null, null, false, false, NamedValue.class);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NamedValue.class, result);
        assertEquals("nv", result.getName());
    }

    // ==================== Array type validation ====================

    @Test
    public void testStringArrayConfig() {
        NVConfig config = NVConfigManager.createNVConfig("items", null, null, false, false, String[].class);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVPairList.class, result);
        assertEquals("items", result.getName());
    }

    @Test
    public void testStringArrayUniqueConfig() {
        NVConfig config = NVConfigManager.createNVConfig("unique_items", null, null, false, false, true, String[].class, null);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVPairGetNameMap.class, result);
        assertEquals("unique_items", result.getName());
    }

    @Test
    public void testLongArrayConfig() {
        NVConfig config = NVConfigManager.createNVConfig("ids", null, null, false, false, Long[].class);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVLongList.class, result);
        assertEquals("ids", result.getName());
    }

    @Test
    public void testByteArrayConfig() {
        NVConfig config = NVConfigManager.createNVConfig("data", null, null, false, false, byte[].class);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVBlob.class, result);
        assertEquals("data", result.getName());
    }

    @Test
    public void testIntegerArrayConfig() {
        NVConfig config = NVConfigManager.createNVConfig("numbers", null, null, false, false, Integer[].class);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVIntList.class, result);
        assertEquals("numbers", result.getName());
    }

    @Test
    public void testFloatArrayConfig() {
        NVConfig config = NVConfigManager.createNVConfig("ratios", null, null, false, false, Float[].class);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVFloatList.class, result);
        assertEquals("ratios", result.getName());
    }

    @Test
    public void testDoubleArrayConfig() {
        NVConfig config = NVConfigManager.createNVConfig("amounts", null, null, false, false, Double[].class);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVDoubleList.class, result);
        assertEquals("amounts", result.getName());
    }

    @Test
    public void testDateArrayConfig() {
        NVConfig config = NVConfigManager.createNVConfig("dates", null, null, false, false, Date[].class);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVLongList.class, result);
        assertEquals("dates", result.getName());
    }

    @Test
    public void testBigDecimalArrayConfig() {
        NVConfig config = NVConfigManager.createNVConfig("prices", null, null, false, false, BigDecimal[].class);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVBigDecimalList.class, result);
        assertEquals("prices", result.getName());
    }

    @Test
    public void testEnumArrayConfig() {
        NVConfig config = NVConfigManager.createNVConfig("statuses", null, null, false, false, TestEnum[].class);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVEnumList.class, result);
        assertEquals("statuses", result.getName());
    }

    // ==================== NVConfigEntity validation ====================

    @Test
    public void testNVConfigEntityScalar() {
        NVConfig config = NVConfigManager.createNVConfigEntity("device", null, null, false, false, DeviceDAO.NVC_DEVICE_DAO);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVEntityReference.class, result);
        assertEquals("device", result.getName());
    }

    @Test
    public void testNVConfigEntityArrayList() {
        NVConfig config = NVConfigManager.createNVConfigEntity("devices", null, null, false, false, DeviceDAO.NVC_DEVICE_DAO, NVConfigEntity.ArrayType.LIST);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVEntityReferenceList.class, result);
        assertEquals("devices", result.getName());
    }

    @Test
    public void testNVConfigEntityArrayGetNameMap() {
        NVConfig config = NVConfigManager.createNVConfigEntity("device_map", null, null, false, false, DeviceDAO.NVC_DEVICE_DAO, NVConfigEntity.ArrayType.GET_NAME_MAP);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVEntityGetNameMap.class, result);
        assertEquals("device_map", result.getName());
    }

    @Test
    public void testNVConfigEntityArrayReferenceIDMap() {
        NVConfig config = NVConfigManager.createNVConfigEntity("device_ref_map", null, null, false, false, DeviceDAO.NVC_DEVICE_DAO, NVConfigEntity.ArrayType.REFERENCE_ID_MAP);
        NVBase<?> result = SharedUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVEntityReferenceIDMap.class, result);
        assertEquals("device_ref_map", result.getName());
    }

    // ==================== Unsupported type ====================

    @Test
    public void testUnsupportedType() {
        NVConfig config = NVConfigManager.createNVConfig("bad", null, null, false, false, Object.class);
        assertThrows(IllegalArgumentException.class, () -> SharedUtil.metaConfigToNVBase(config));
    }

    // ==================== Performance benchmark ====================

    @Test
    public void testPerformanceComparison() {
        // Build a representative set of NVConfig covering all types
        NVConfig[] configs = {
                NVConfigManager.createNVConfig("b", null, null, false, false, Boolean.class),
                NVConfigManager.createNVConfig("i", null, null, false, false, Integer.class),
                NVConfigManager.createNVConfig("l", null, null, false, false, Long.class),
                NVConfigManager.createNVConfig("f", null, null, false, false, Float.class),
                NVConfigManager.createNVConfig("d", null, null, false, false, Double.class),
                NVConfigManager.createNVConfig("s", null, null, false, false, String.class),
                NVConfigManager.createNVConfig("dt", null, null, false, false, Date.class),
                NVConfigManager.createNVConfig("bd", null, null, false, false, BigDecimal.class),
                NVConfigManager.createNVConfig("n", null, null, false, false, Number.class),
                NVConfigManager.createNVConfig("e", null, null, false, false, TestEnum.class),
                NVConfigManager.createNVConfig("gm", null, null, false, false, NVGenericMap.class),
                NVConfigManager.createNVConfig("sl", null, null, false, false, NVStringList.class),
                NVConfigManager.createNVConfig("sa", null, null, false, false, String[].class),
                NVConfigManager.createNVConfig("la", null, null, false, false, Long[].class),
                NVConfigManager.createNVConfig("ba", null, null, false, false, byte[].class),
                NVConfigManager.createNVConfig("ia", null, null, false, false, Integer[].class),
                NVConfigManager.createNVConfig("fa", null, null, false, false, Float[].class),
                NVConfigManager.createNVConfig("da", null, null, false, false, Double[].class),
                NVConfigManager.createNVConfig("ea", null, null, false, false, TestEnum[].class),
        };

        int iterations = 1_000_000;
        int warmupIterations = 100_000;

        // Warmup both paths thoroughly so JIT compiles them
        for (int w = 0; w < warmupIterations; w++) {
            for (NVConfig config : configs) {
                MetaToInstance.SINGLETON.toNVBase(config);
                SharedUtil.metaConfigToNVBase(config);
            }
        }

        // Run multiple rounds, alternating order to reduce bias
        long mapTotal = 0;
        long delegateTotal = 0;
        int rounds = 5;

        for (int r = 0; r < rounds; r++) {
            if (r % 2 == 0) {
                // Direct first
                long start = System.nanoTime();
                for (int i = 0; i < iterations; i++) {
                    for (NVConfig config : configs) {
                        MetaToInstance.SINGLETON.toNVBase(config);
                    }
                }
                mapTotal += System.nanoTime() - start;

                start = System.nanoTime();
                for (int i = 0; i < iterations; i++) {
                    for (NVConfig config : configs) {
                        SharedUtil.metaConfigToNVBase(config);
                    }
                }
                delegateTotal += System.nanoTime() - start;
            } else {
                // Delegate first
                long start = System.nanoTime();
                for (int i = 0; i < iterations; i++) {
                    for (NVConfig config : configs) {
                        SharedUtil.metaConfigToNVBase(config);
                    }
                }
                delegateTotal += System.nanoTime() - start;

                start = System.nanoTime();
                for (int i = 0; i < iterations; i++) {
                    for (NVConfig config : configs) {
                        MetaToInstance.SINGLETON.toNVBase(config);
                    }
                }
                mapTotal += System.nanoTime() - start;
            }
        }

        long totalCalls = (long) iterations * configs.length * rounds;

        System.out.println("===== MetaToInstance Performance (IdentityHashMap) =====");
        System.out.println("Configs tested: " + configs.length);
        System.out.println("Iterations per round: " + iterations);
        System.out.println("Rounds: " + rounds);
        System.out.println("Total calls per method: " + totalCalls);
        System.out.println();
        System.out.println("MetaToInstance.SINGLETON.toNVBase() direct:");
        System.out.println("  Total: " + (mapTotal / 1_000_000) + " ms");
        System.out.println("  Per call: " + (mapTotal / totalCalls) + " ns");
        System.out.println();
        System.out.println("SharedUtil.metaConfigToNVBase() (delegate):");
        System.out.println("  Total: " + (delegateTotal / 1_000_000) + " ms");
        System.out.println("  Per call: " + (delegateTotal / totalCalls) + " ns");
        System.out.println();

        double ratio = (double) delegateTotal / mapTotal;
        System.out.printf("Ratio (delegate/direct): %.2fx%n", ratio);
    }
}
