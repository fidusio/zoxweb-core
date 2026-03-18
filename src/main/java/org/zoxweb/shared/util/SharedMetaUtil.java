package org.zoxweb.shared.util;

import java.math.BigDecimal;
import java.util.*;

/**
 * Utility class that converts {@link NVConfig} metadata descriptors into concrete {@link NVBase} data instances.
 * <p>
 * Uses an {@link IdentityHashMap} keyed by {@code Class<?>} to map meta types to
 * {@link InstanceFactory.ParamCreator} lambdas for O(1) instance creation.
 * The map is extensible at runtime via {@link #addM2I(Class, InstanceFactory.ParamCreator)}.
 * <p>
 * Supported scalar types: Boolean, Integer, Long, Float, Double, String, Date, BigDecimal,
 * Number, Enum, NVGenericMap, NVGenericMapList, NVStringList, NVStringSet, NamedValue.
 * <br>
 * Supported array types: Enum[], String[], Long[], byte[], Integer[], Float[], Double[],
 * Date[], BigDecimal[].
 * <br>
 * {@link NVConfigEntity} types are dispatched by {@link NVConfigEntity.ArrayType}.
 */
public class SharedMetaUtil {
    private static final Map<Class<?>, InstanceFactory.ParamCreator<NVBase<?>, NVConfig>> metaToInstance = new IdentityHashMap<>();
    /** Singleton instance with all default type mappings registered. */
    public static final SharedMetaUtil SINGLETON = new SharedMetaUtil();

    private SharedMetaUtil() {
        init();
    }

    /**
     * Converts a list of {@link NVConfig} descriptors into a name-keyed map of {@link NVBase} instances.
     *
     * @param params the list of NVConfig metadata descriptors
     * @return a {@link LinkedHashMap} of name to NVBase instances preserving insertion order
     */
    public static Map<String, NVBase<?>> toData(List<NVConfig> params) {
        HashMap<String, NVBase<?>> ret = new LinkedHashMap<String, NVBase<?>>();

        for (NVConfig config : params) {
            ret.put(config.getName(), SINGLETON.toNVBase(config));
        }

        return ret;
    }


    /**
     * Converts an array of {@link NVConfig} descriptors into a list of {@link NVBase} instances.
     *
     * @param params the NVConfig metadata descriptors
     * @return list of NVBase instances
     */
    public static ArrayList<NVBase<?>> toData(NVConfig[] params) {
        ArrayList<NVBase<?>> ret = new ArrayList<NVBase<?>>();

        for (NVConfig config : params) {
            ret.add(SINGLETON.toNVBase(config));
        }

        return ret;
    }


    /**
     * Converts an array of {@link GetNVConfig} wrappers into a list of {@link NVBase} instances.
     *
     * @param params the GetNVConfig metadata wrappers
     * @return list of NVBase instances
     */
    public static ArrayList<NVBase<?>> toData(GetNVConfig[] params) {
        ArrayList<NVBase<?>> ret = new ArrayList<NVBase<?>>();

        for (GetNVConfig config : params) {
            ret.add(SINGLETON.toNVBase(config.getNVConfig()));
        }

        return ret;
    }


    /**
     * Converts the attributes of an {@link NVConfigEntity} into {@link NVBase} instances
     * and appends them to the given list.
     *
     * @param nvce   the NVConfigEntity whose attributes to convert
     * @param values the list to append to, or null to create a new list
     * @return the list of NVBase instances
     */
    public static ArrayList<NVBase<?>> toData(NVConfigEntity nvce, ArrayList<NVBase<?>> values) {
        if (values == null) {
            values = new ArrayList<NVBase<?>>();
        }

        for (NVConfig config : nvce.getAttributes()) {
            values.add(SINGLETON.toNVBase(config));
        }

        return values;
    }

    /**
     * Converts NVConfig to NVBase based on
     * the variable name shared between both objects.
     * <br>Primitive object conversion list:
     * <ul>
     * <li>Enum type class to NVEnum</li>
     * <li>String type class to NVPair</li>
     * <li>Long type class to NVLong</li>
     * <li>Integer type class to NVInt</li>
     * <li>Boolean type class to NVBoolean</li>
     * <li>Float type class to NVFloat</li>
     * <li>Double type class to NVDouble</li>
     * </ul>
     * Array object conversion list:
     * <ul>
     * <li>Enum type class to NVEnumList</li>
     * <li>String array type class to NVPairList</li>
     * <li>Long array type class to NVLongList</li>
     * <li>Byte array type class to NVBlob</li>
     * <li>Integer array type class to NVIntList</li>
     * <li>Float array type class to NVFloatList</li>
     * <li>Double array type class to NVDoubleList</li>
     * </ul>
     *
     * @param config meta data descriptor
     * @return nvbase based on nvconfig
     */
    public static NVBase<?> metaConfigToNVBase(NVConfig config) {
        Class<?> c = config.getMetaType();

        if (config.isArray()) {
            if (config instanceof NVConfigEntity) {
                NVConfigEntity nvce = (NVConfigEntity) config;
                //System.out.println(""+config);

                switch (nvce.getArrayType()) {
                    case GET_NAME_MAP:
                        return new NVEntityGetNameMap(config.getName());
                    case LIST:
                        return new NVEntityReferenceList(config.getName());
                    case REFERENCE_ID_MAP:
                        return new NVEntityReferenceIDMap(config.getName());
                    case NOT_ARRAY:
                    default:
                        break;

                }

                //return new NVEntityReferenceList(config.getName());
            }

            // enum must be checked first
            if (config.isEnum()) {
                return (new NVEnumList(config.getName(), new ArrayList<Enum<?>>()));
            } else if (String[].class.equals(c)) {
                if (config.isUnique()) {
                    return (new NVPairGetNameMap(config.getName(), new LinkedHashMap<GetName, GetNameValue<String>>()));
                }

                return (new NVPairList(config.getName(), new ArrayList<NVPair>()));
            } else if (Long[].class.equals(c)) {
                return (new NVLongList(config.getName(), new ArrayList<Long>()));
            } else if (byte[].class.equals(c)) {
                return (new NVBlob(config.getName(), null));
            } else if (Integer[].class.equals(c)) {
                return (new NVIntList(config.getName(), new ArrayList<Integer>()));
            } else if (Float[].class.equals(c)) {
                return (new NVFloatList(config.getName(), new ArrayList<Float>()));
            } else if (Double[].class.equals(c)) {
                return (new NVDoubleList(config.getName(), new ArrayList<Double>()));
            } else if (Date[].class.equals(c)) {
                return (new NVLongList(config.getName(), new ArrayList<Long>()));
            } else if (BigDecimal[].class.equals(c)) {
                return (new NVBigDecimalList(config.getName(), new ArrayList<BigDecimal>()));
            }
        } else {
            // Not array
            if (config instanceof NVConfigEntity) {
                return new NVEntityReference(config);
            }

            if (config.isEnum()) {
                return (new NVEnum(config.getName(), null));
            } else if (String.class.equals(c)) {
                NVPair nvp = new NVPair(config.getName(), (String) null);
                nvp.setValueFilter(config.getValueFilter());
                return nvp;
            } else if (Long.class.equals(c)) {
                return new NVLong(config.getName(), 0);
            } else if (Integer.class.equals(c)) {
                return new NVInt(config.getName(), 0);
            } else if (Boolean.class.equals(c)) {
                return (new NVBoolean(config.getName(), false));
            } else if (Float.class.equals(c)) {
                return new NVFloat(config.getName(), 0);
            } else if (Double.class.equals(c)) {
                return new NVDouble(config.getName(), 0);
            } else if (Date.class.equals(c)) {
                return new NVLong(config.getName(), 0);
            } else if (BigDecimal.class.equals(c)) {
                return new NVBigDecimal(config.getName(), new BigDecimal(0));
            } else if (Number.class.equals(c)) {
                return new NVNumber(config.getName(), null);
            } else if (NVGenericMap.class.equals(c)) {
                return new NVGenericMap(config.getName());
            } else if (NVGenericMapList.class.equals(c)) {
                return new NVGenericMapList(config.getName());
            } else if (NVStringList.class.equals(c)) {
                return new NVStringList(config.getName());
            } else if (NVStringSet.class.equals(c)) {
                return new NVStringSet(config.getName());
            } else if (NamedValue.class.equals(c)) {
                return new NamedValue(config.getName());
            }
        }

        throw new IllegalArgumentException("Unsupported type " + config + " class:" + c);
    }

    private void init() {
        // Scalar types
        addM2I(Boolean.class, (nvc) -> new NVBoolean(nvc.getName(), false))
                .addM2I(Integer.class, (nvc) -> new NVInt(nvc.getName(), 0))
                .addM2I(Long.class, (nvc) -> new NVLong(nvc.getName(), 0))
                .addM2I(Float.class, (nvc) -> new NVFloat(nvc.getName(), 0))
                .addM2I(Double.class, (nvc) -> new NVDouble(nvc.getName(), 0))
                .addM2I(String.class, (nvc) -> {
                    NVPair ret = new NVPair(nvc.getName(), (String) null);
                    ret.setValueFilter(nvc.getValueFilter());
                    return ret;
                })
                .addM2I(Date.class, (nvc) -> new NVLong(nvc.getName(), 0))
                .addM2I(BigDecimal.class, (nvc) -> new NVBigDecimal(nvc.getName(), new BigDecimal(0)))
                .addM2I(Number.class, (nvc) -> new NVNumber(nvc.getName(), null))
                .addM2I(Enum.class, (nvc) -> new NVEnum(nvc.getName(), null))
                // Composite scalar types
                .addM2I(NVGenericMap.class, (nvc) -> new NVGenericMap(nvc.getName()))
                .addM2I(NVGenericMapList.class, (nvc) -> new NVGenericMapList(nvc.getName()))
                .addM2I(NVStringList.class, (nvc) -> new NVStringList(nvc.getName()))
                .addM2I(NVStringSet.class, (nvc) -> new NVStringSet(nvc.getName()))
                .addM2I(NamedValue.class, (nvc) -> new NamedValue(nvc.getName()))
                // Array types
                .addM2I(Enum[].class, (nvc) -> new NVEnumList(nvc.getName(), new ArrayList<>()))
                .addM2I(String[].class, (nvc) -> {
                    if (nvc.isUnique()) {
                        return new NVPairGetNameMap(nvc.getName(), new LinkedHashMap<GetName, GetNameValue<String>>());
                    }
                    return new NVPairList(nvc.getName(), new ArrayList<>());
                })
                .addM2I(Long[].class, (nvc) -> new NVLongList(nvc.getName(), new ArrayList<>()))
                .addM2I(byte[].class, (nvc) -> new NVBlob(nvc.getName(), null))
                .addM2I(Integer[].class, (nvc) -> new NVIntList(nvc.getName(), new ArrayList<>()))
                .addM2I(Float[].class, (nvc) -> new NVFloatList(nvc.getName(), new ArrayList<>()))
                .addM2I(Double[].class, (nvc) -> new NVDoubleList(nvc.getName(), new ArrayList<>()))
                .addM2I(Date[].class, (nvc) -> new NVLongList(nvc.getName(), new ArrayList<>()))
                .addM2I(BigDecimal[].class, (nvc) -> new NVBigDecimalList(nvc.getName(), new ArrayList<>()))
        ;
    }


    /**
     * Registers a type-to-instance mapping. Can be used to extend or override default mappings.
     *
     * @param clazz   the meta type class key (e.g., {@code Integer.class}, {@code String[].class})
     * @param creator the factory lambda that creates an {@link NVBase} from an {@link NVConfig}
     * @return this instance for method chaining
     */
    public SharedMetaUtil addM2I(Class<?> clazz, InstanceFactory.ParamCreator<NVBase<?>, NVConfig> creator) {
        metaToInstance.put(clazz, creator);
        return this;
    }

    /**
     * Converts an {@link NVConfig} metadata descriptor into a concrete {@link NVBase} data instance
     * using the registered type mappings.
     * <p>
     * {@link NVConfigEntity} types are handled separately via {@link NVConfigEntity.ArrayType} dispatch.
     * Enum types are normalized to {@code Enum.class} / {@code Enum[].class} canonical keys.
     *
     * @param nvc the NVConfig metadata descriptor
     * @param <I> the expected NVBase subtype
     * @return a new NVBase instance matching the config's meta type
     * @throws IllegalArgumentException if the meta type is not supported
     */
    public <I extends NVBase<?>> I toNVBase(NVConfig nvc) {
        // Handle NVConfigEntity separately — requires config-specific dispatch
        if (nvc instanceof NVConfigEntity) {
            NVConfigEntity nvce = (NVConfigEntity) nvc;
            if (nvc.isArray()) {
                switch (nvce.getArrayType()) {
                    case GET_NAME_MAP:
                        return (I) new NVEntityGetNameMap(nvc.getName());
                    case LIST:
                        return (I) new NVEntityReferenceList(nvc.getName());
                    case REFERENCE_ID_MAP:
                        return (I) new NVEntityReferenceIDMap(nvc.getName());
                    case NOT_ARRAY:
                    default:
                        break;
                }
            } else {
                return (I) new NVEntityReference(nvc);
            }
        }

        Class<?> type = nvc.getMetaType();
        // Normalize enum types to canonical keys
        if (type.isArray() && type.getComponentType().isEnum())
            type = Enum[].class;
        else if (type.isEnum())
            type = Enum.class;

        InstanceFactory.ParamCreator<NVBase<?>, NVConfig> instanceCreator = metaToInstance.get(type);
        if (instanceCreator != null) {
            return (I) instanceCreator.newInstance(nvc);
        }

        throw new IllegalArgumentException("Unsupported type " + nvc + " class:" + type);
    }

}
