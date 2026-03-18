package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.data.DeviceDAO;
import org.zoxweb.shared.filters.FilterType;

import java.math.BigDecimal;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class MetaToInstanceTest {

    private enum TestEnum { A, B, C }

    // ==================== Scalar type validation ====================

    @Test
    public void testBooleanConfig() {
        NVConfig config = NVConfigManager.createNVConfig("flag", null, null, false, false, Boolean.class);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVBoolean.class, result);
        assertEquals("flag", result.getName());
        assertEquals(false, result.getValue());
    }

    @Test
    public void testIntegerConfig() {
        NVConfig config = NVConfigManager.createNVConfig("count", null, null, false, false, Integer.class);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVInt.class, result);
        assertEquals("count", result.getName());
        assertEquals(0, result.getValue());
    }

    @Test
    public void testLongConfig() {
        NVConfig config = NVConfigManager.createNVConfig("timestamp", null, null, false, false, Long.class);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVLong.class, result);
        assertEquals("timestamp", result.getName());
        assertEquals(0L, result.getValue());
    }

    @Test
    public void testFloatConfig() {
        NVConfig config = NVConfigManager.createNVConfig("ratio", null, null, false, false, Float.class);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVFloat.class, result);
        assertEquals("ratio", result.getName());
    }

    @Test
    public void testDoubleConfig() {
        NVConfig config = NVConfigManager.createNVConfig("amount", null, null, false, false, Double.class);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVDouble.class, result);
        assertEquals("amount", result.getName());
    }

    @Test
    public void testStringConfig() {
        NVConfig config = NVConfigManager.createNVConfig("name", null, null, false, false, String.class);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVPair.class, result);
        assertEquals("name", result.getName());
        assertNull(result.getValue());
    }

    @Test
    public void testStringWithValueFilter() {
        NVConfig config = NVConfigManager.createNVConfig("email", null, null, false, false, false, String.class, FilterType.EMAIL);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVPair.class, result);
        NVPair nvp = (NVPair) result;
        assertEquals("email", nvp.getName());
        assertNotNull(nvp.getValueFilter());
        assertEquals(FilterType.EMAIL, nvp.getValueFilter());
    }

    @Test
    public void testDateConfig() {
        NVConfig config = NVConfigManager.createNVConfig("created", null, null, false, false, Date.class);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVLong.class, result);
        assertEquals("created", result.getName());
    }

    @Test
    public void testBigDecimalConfig() {
        NVConfig config = NVConfigManager.createNVConfig("price", null, null, false, false, BigDecimal.class);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVBigDecimal.class, result);
        assertEquals("price", result.getName());
    }

    @Test
    public void testNumberConfig() {
        NVConfig config = NVConfigManager.createNVConfig("num", null, null, false, false, Number.class);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVNumber.class, result);
        assertEquals("num", result.getName());
    }

    @Test
    public void testEnumConfig() {
        NVConfig config = NVConfigManager.createNVConfig("status", null, null, false, false, TestEnum.class);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVEnum.class, result);
        assertEquals("status", result.getName());
    }

    // ==================== Composite scalar types ====================

    @Test
    public void testNVGenericMapConfig() {
        NVConfig config = NVConfigManager.createNVConfig("map", null, null, false, false, NVGenericMap.class);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVGenericMap.class, result);
        assertEquals("map", result.getName());
    }

    @Test
    public void testNVGenericMapListConfig() {
        NVConfig config = NVConfigManager.createNVConfig("maps", null, null, false, false, NVGenericMapList.class);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVGenericMapList.class, result);
        assertEquals("maps", result.getName());
    }

    @Test
    public void testNVStringListConfig() {
        NVConfig config = NVConfigManager.createNVConfig("tags", null, null, false, false, NVStringList.class);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVStringList.class, result);
        assertEquals("tags", result.getName());
    }

    @Test
    public void testNVStringSetConfig() {
        NVConfig config = NVConfigManager.createNVConfig("unique_tags", null, null, false, false, NVStringSet.class);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVStringSet.class, result);
        assertEquals("unique_tags", result.getName());
    }

    @Test
    public void testNamedValueConfig() {
        NVConfig config = NVConfigManager.createNVConfig("nv", null, null, false, false, NamedValue.class);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NamedValue.class, result);
        assertEquals("nv", result.getName());
    }

    // ==================== Array type validation ====================

    @Test
    public void testStringArrayConfig() {
        NVConfig config = NVConfigManager.createNVConfig("items", null, null, false, false, String[].class);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVPairList.class, result);
        assertEquals("items", result.getName());
    }

    @Test
    public void testStringArrayUniqueConfig() {
        NVConfig config = NVConfigManager.createNVConfig("unique_items", null, null, false, false, true, String[].class, null);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVPairGetNameMap.class, result);
        assertEquals("unique_items", result.getName());
    }

    @Test
    public void testLongArrayConfig() {
        NVConfig config = NVConfigManager.createNVConfig("ids", null, null, false, false, Long[].class);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVLongList.class, result);
        assertEquals("ids", result.getName());
    }

    @Test
    public void testByteArrayConfig() {
        NVConfig config = NVConfigManager.createNVConfig("data", null, null, false, false, byte[].class);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVBlob.class, result);
        assertEquals("data", result.getName());
    }

    @Test
    public void testIntegerArrayConfig() {
        NVConfig config = NVConfigManager.createNVConfig("numbers", null, null, false, false, Integer[].class);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVIntList.class, result);
        assertEquals("numbers", result.getName());
    }

    @Test
    public void testFloatArrayConfig() {
        NVConfig config = NVConfigManager.createNVConfig("ratios", null, null, false, false, Float[].class);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVFloatList.class, result);
        assertEquals("ratios", result.getName());
    }

    @Test
    public void testDoubleArrayConfig() {
        NVConfig config = NVConfigManager.createNVConfig("amounts", null, null, false, false, Double[].class);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVDoubleList.class, result);
        assertEquals("amounts", result.getName());
    }

    @Test
    public void testDateArrayConfig() {
        NVConfig config = NVConfigManager.createNVConfig("dates", null, null, false, false, Date[].class);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVLongList.class, result);
        assertEquals("dates", result.getName());
    }

    @Test
    public void testBigDecimalArrayConfig() {
        NVConfig config = NVConfigManager.createNVConfig("prices", null, null, false, false, BigDecimal[].class);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVBigDecimalList.class, result);
        assertEquals("prices", result.getName());
    }

    @Test
    public void testEnumArrayConfig() {
        NVConfig config = NVConfigManager.createNVConfig("statuses", null, null, false, false, TestEnum[].class);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVEnumList.class, result);
        assertEquals("statuses", result.getName());
    }

    // ==================== NVConfigEntity validation ====================

    @Test
    public void testNVConfigEntityScalar() {
        NVConfig config = NVConfigManager.createNVConfigEntity("device", null, null, false, false, DeviceDAO.NVC_DEVICE_DAO);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVEntityReference.class, result);
        assertEquals("device", result.getName());
    }

    @Test
    public void testNVConfigEntityArrayList() {
        NVConfig config = NVConfigManager.createNVConfigEntity("devices", null, null, false, false, DeviceDAO.NVC_DEVICE_DAO, NVConfigEntity.ArrayType.LIST);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVEntityReferenceList.class, result);
        assertEquals("devices", result.getName());
    }

    @Test
    public void testNVConfigEntityArrayGetNameMap() {
        NVConfig config = NVConfigManager.createNVConfigEntity("device_map", null, null, false, false, DeviceDAO.NVC_DEVICE_DAO, NVConfigEntity.ArrayType.GET_NAME_MAP);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVEntityGetNameMap.class, result);
        assertEquals("device_map", result.getName());
    }

    @Test
    public void testNVConfigEntityArrayReferenceIDMap() {
        NVConfig config = NVConfigManager.createNVConfigEntity("device_ref_map", null, null, false, false, DeviceDAO.NVC_DEVICE_DAO, NVConfigEntity.ArrayType.REFERENCE_ID_MAP);
        NVBase<?> result = SharedMetaUtil.metaConfigToNVBase(config);
        assertInstanceOf(NVEntityReferenceIDMap.class, result);
        assertEquals("device_ref_map", result.getName());
    }

    // ==================== Unsupported type ====================

    @Test
    public void testUnsupportedType() {
        NVConfig config = NVConfigManager.createNVConfig("bad", null, null, false, false, Object.class);
        assertThrows(IllegalArgumentException.class, () -> SharedMetaUtil.metaConfigToNVBase(config));
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
                SharedMetaUtil.SINGLETON.toNVBase(config);
                SharedMetaUtil.metaConfigToNVBase(config);
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
                        SharedMetaUtil.SINGLETON.toNVBase(config);
                    }
                }
                mapTotal += System.nanoTime() - start;

                start = System.nanoTime();
                for (int i = 0; i < iterations; i++) {
                    for (NVConfig config : configs) {
                        SharedMetaUtil.metaConfigToNVBase(config);
                    }
                }
                delegateTotal += System.nanoTime() - start;
            } else {
                // Delegate first
                long start = System.nanoTime();
                for (int i = 0; i < iterations; i++) {
                    for (NVConfig config : configs) {
                        SharedMetaUtil.metaConfigToNVBase(config);
                    }
                }
                delegateTotal += System.nanoTime() - start;

                start = System.nanoTime();
                for (int i = 0; i < iterations; i++) {
                    for (NVConfig config : configs) {
                        SharedMetaUtil.SINGLETON.toNVBase(config);
                    }
                }
                mapTotal += System.nanoTime() - start;
            }
        }

        long totalCalls = (long) iterations * configs.length * rounds;

        System.out.println("===== MetaUtil Performance =====");
        System.out.println("Configs tested: " + configs.length);
        System.out.println("Iterations per round: " + iterations);
        System.out.println("Rounds: " + rounds);
        System.out.println("Total calls per method: " + totalCalls);
        System.out.println();
        System.out.println("toNVBase() [IdentityHashMap-based]:");
        System.out.println("  Total: " + (mapTotal / 1_000_000) + " ms");
        System.out.println("  Per call: " + (mapTotal / totalCalls) + " ns");
        System.out.println();
        System.out.println("metaConfigToNVBase() [if-else chain]:");
        System.out.println("  Total: " + (delegateTotal / 1_000_000) + " ms");
        System.out.println("  Per call: " + (delegateTotal / totalCalls) + " ns");
        System.out.println();

        double ratio = (double) delegateTotal / mapTotal;
        System.out.printf("Ratio (if-else / map): %.2fx%n", ratio);
    }

    @Test
    public void testFunctionVsParamCreator() {
        // Build two identical maps: one using Function, one using ParamCreator
        Map<Class<?>, Function<NVConfig, NVBase<?>>> functionMap = new IdentityHashMap<>();
        Map<Class<?>, InstanceFactory.ParamCreator<NVBase<?>, NVConfig>> paramCreatorMap = new IdentityHashMap<>();

        // Register the same lambdas in both
        functionMap.put(Boolean.class, (nvc) -> new NVBoolean(nvc.getName(), false));
        functionMap.put(Integer.class, (nvc) -> new NVInt(nvc.getName(), 0));
        functionMap.put(Long.class, (nvc) -> new NVLong(nvc.getName(), 0));
        functionMap.put(Float.class, (nvc) -> new NVFloat(nvc.getName(), 0));
        functionMap.put(Double.class, (nvc) -> new NVDouble(nvc.getName(), 0));
        functionMap.put(String.class, (nvc) -> new NVPair(nvc.getName(), (String) null));
        functionMap.put(Number.class, (nvc) -> new NVNumber(nvc.getName(), null));

        paramCreatorMap.put(Boolean.class, (nvc) -> new NVBoolean(nvc.getName(), false));
        paramCreatorMap.put(Integer.class, (nvc) -> new NVInt(nvc.getName(), 0));
        paramCreatorMap.put(Long.class, (nvc) -> new NVLong(nvc.getName(), 0));
        paramCreatorMap.put(Float.class, (nvc) -> new NVFloat(nvc.getName(), 0));
        paramCreatorMap.put(Double.class, (nvc) -> new NVDouble(nvc.getName(), 0));
        paramCreatorMap.put(String.class, (nvc) -> new NVPair(nvc.getName(), (String) null));
        paramCreatorMap.put(Number.class, (nvc) -> new NVNumber(nvc.getName(), null));

        NVConfig[] configs = {
                NVConfigManager.createNVConfig("b", null, null, false, false, Boolean.class),
                NVConfigManager.createNVConfig("i", null, null, false, false, Integer.class),
                NVConfigManager.createNVConfig("l", null, null, false, false, Long.class),
                NVConfigManager.createNVConfig("f", null, null, false, false, Float.class),
                NVConfigManager.createNVConfig("d", null, null, false, false, Double.class),
                NVConfigManager.createNVConfig("s", null, null, false, false, String.class),
                NVConfigManager.createNVConfig("n", null, null, false, false, Number.class),
        };

        int iterations = 1_000_000;
        int warmupIterations = 100_000;

        // Warmup
        for (int w = 0; w < warmupIterations; w++) {
            for (NVConfig config : configs) {
                Class<?> type = config.getMetaType();
                functionMap.get(type).apply(config);
                paramCreatorMap.get(type).newInstance(config);
            }
        }

        // Alternating rounds
        long functionTotal = 0;
        long paramCreatorTotal = 0;
        int rounds = 5;

        for (int r = 0; r < rounds; r++) {
            if (r % 2 == 0) {
                long start = System.nanoTime();
                for (int i = 0; i < iterations; i++) {
                    for (NVConfig config : configs) {
                        functionMap.get(config.getMetaType()).apply(config);
                    }
                }
                functionTotal += System.nanoTime() - start;

                start = System.nanoTime();
                for (int i = 0; i < iterations; i++) {
                    for (NVConfig config : configs) {
                        paramCreatorMap.get(config.getMetaType()).newInstance(config);
                    }
                }
                paramCreatorTotal += System.nanoTime() - start;
            } else {
                long start = System.nanoTime();
                for (int i = 0; i < iterations; i++) {
                    for (NVConfig config : configs) {
                        paramCreatorMap.get(config.getMetaType()).newInstance(config);
                    }
                }
                paramCreatorTotal += System.nanoTime() - start;

                start = System.nanoTime();
                for (int i = 0; i < iterations; i++) {
                    for (NVConfig config : configs) {
                        functionMap.get(config.getMetaType()).apply(config);
                    }
                }
                functionTotal += System.nanoTime() - start;
            }
        }

        long totalCalls = (long) iterations * configs.length * rounds;

        System.out.println("===== Function vs ParamCreator =====");
        System.out.println("Configs: " + configs.length + ", Iterations/round: " + iterations + ", Rounds: " + rounds);
        System.out.println("Total calls per interface: " + totalCalls);
        System.out.println();
        System.out.println("java.util.function.Function.apply():");
        System.out.println("  Total: " + (functionTotal / 1_000_000) + " ms");
        System.out.println("  Per call: " + (functionTotal / totalCalls) + " ns");
        System.out.println();
        System.out.println("InstanceFactory.ParamCreator.newInstance():");
        System.out.println("  Total: " + (paramCreatorTotal / 1_000_000) + " ms");
        System.out.println("  Per call: " + (paramCreatorTotal / totalCalls) + " ns");
        System.out.println();

        double r = (double) paramCreatorTotal / functionTotal;
        System.out.printf("Ratio (ParamCreator / Function): %.2fx%n", r);
    }
}
