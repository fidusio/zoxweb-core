package org.zoxweb.shared.util;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;

public class MetaUtil {
    private static final Map<Class<?>, Function<String, NVBase<?>>> metaToInstance = new IdentityHashMap<>();
    public static final MetaUtil SINGLETON = new MetaUtil();

    private MetaUtil() {
        init();
    }

    /**
     * @param params
     * @return create map nvbase based on the meta params
     */
    public static Map<String, NVBase<?>> toData(List<NVConfig> params) {
        HashMap<String, NVBase<?>> ret = new LinkedHashMap<String, NVBase<?>>();

        for (NVConfig config : params) {
            ret.put(config.getName(), SINGLETON.toNVBase(config));
        }

        return ret;
    }

    /**
     * @param params
     * @return create list nvbased on nvconfig array
     */
    public static ArrayList<NVBase<?>> toData(NVConfig[] params) {
        ArrayList<NVBase<?>> ret = new ArrayList<NVBase<?>>();

        for (NVConfig config : params) {
            ret.add(SINGLETON.toNVBase(config));
        }

        return ret;
    }

    /**
     * @param params
     * @return create list nvbased on nvconfig array
     */
    public static ArrayList<NVBase<?>> toData(GetNVConfig[] params) {
        ArrayList<NVBase<?>> ret = new ArrayList<NVBase<?>>();

        for (GetNVConfig config : params) {
            ret.add(SINGLETON.toNVBase(config.getNVConfig()));
        }

        return ret;
    }

    /**
     * @param nvce
     * @param values
     * @return create list nvbased on nvce
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
     * @param config
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
        addM2I(Boolean.class, (name) -> new NVBoolean(name, false))
                .addM2I(Integer.class, (name) -> new NVInt(name, 0))
                .addM2I(Long.class, (name) -> new NVLong(name, 0))
                .addM2I(Float.class, (name) -> new NVFloat(name, 0))
                .addM2I(Double.class, (name) -> new NVDouble(name, 0))
                .addM2I(String.class, (name) -> new NVPair(name, (String) null))
                .addM2I(Date.class, (name) -> new NVLong(name, 0))
                .addM2I(BigDecimal.class, (name) -> new NVBigDecimal(name, new BigDecimal(0)))
                .addM2I(Number.class, (name) -> new NVNumber(name, null))
                .addM2I(Enum.class, (name) -> new NVEnum(name, null))
                // Composite scalar types
                .addM2I(NVGenericMap.class, (name) -> new NVGenericMap(name))
                .addM2I(NVGenericMapList.class, (name) -> new NVGenericMapList(name))
                .addM2I(NVStringList.class, (name) -> new NVStringList(name))
                .addM2I(NVStringSet.class, (name) -> new NVStringSet(name))
                .addM2I(NamedValue.class, (name) -> new NamedValue(name))
                // Array types
                .addM2I(Enum[].class, (name) -> new NVEnumList(name, new ArrayList<>()))
                .addM2I(String[].class, (name) -> new NVPairList(name, new ArrayList<>()))
                .addM2I(Long[].class, (name) -> new NVLongList(name, new ArrayList<>()))
                .addM2I(byte[].class, (name) -> new NVBlob(name, null))
                .addM2I(Integer[].class, (name) -> new NVIntList(name, new ArrayList<>()))
                .addM2I(Float[].class, (name) -> new NVFloatList(name, new ArrayList<>()))
                .addM2I(Double[].class, (name) -> new NVDoubleList(name, new ArrayList<>()))
                .addM2I(Date[].class, (name) -> new NVLongList(name, new ArrayList<>()))
                .addM2I(BigDecimal[].class, (name) -> new NVBigDecimalList(name, new ArrayList<>()))
        ;
    }


    public MetaUtil addM2I(Class<?> clazz, Function<String, NVBase<?>> creator) {
        metaToInstance.put(clazz, creator);
        return this;
    }

    public  <I extends NVBase<?>> I toNVBase(NVConfig nvc)
    {
        // Handle NVConfigEntity separately — requires config-specific dispatch
        if (nvc instanceof NVConfigEntity) {
            NVConfigEntity nvce = (NVConfigEntity) nvc;
            if (nvc.isArray()) {
                switch (nvce.getArrayType()) {
                    case GET_NAME_MAP:
                        return (I)new NVEntityGetNameMap(nvc.getName());
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

        // String[] with unique flag returns NVPairGetNameMap instead of NVPairList
        if (type == String[].class && nvc.isUnique()) {
            return (I)new NVPairGetNameMap(nvc.getName(), new LinkedHashMap<GetName, GetNameValue<String>>());
        }

        Function<String, NVBase<?>> instanceCreator = metaToInstance.get(type);
        if (instanceCreator != null) {
            NVBase<?> result = instanceCreator.apply(nvc.getName());
            // String scalar: apply value filter from config
            if (type == String.class && nvc.getValueFilter() != null) {
                ((NVPair) result).setValueFilter(nvc.getValueFilter());
            }
            return (I) result;
        }

        throw new IllegalArgumentException("Unsupported type " + nvc + " class:" + type);
    }

}
