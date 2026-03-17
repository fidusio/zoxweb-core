package org.zoxweb.shared.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class MetaToInstance {
    private final Map<Class<?>, Function<String, NVBase<?>>> metaToInstance = new IdentityHashMap<>();
    public static final MetaToInstance SINGLETON = new MetaToInstance();

    private MetaToInstance() {
        init();
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


    public MetaToInstance addM2I(Class<?> clazz,Function<String, NVBase<?>> creator) {
        metaToInstance.put(clazz, creator);
        return this;
    }

    public <I extends NVBase<?>> I toNVBase(NVConfig nvc)
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
