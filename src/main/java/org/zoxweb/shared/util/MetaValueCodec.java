package org.zoxweb.shared.util;

import java.util.LinkedHashMap;

public class MetaValueCodec {
    public static final MetaValueCodec SINGLETON = new MetaValueCodec();
    private final RegistrarMap<Class<?>, DataCodec, RegistrarMap> codecMap = new RegistrarMap<>(new LinkedHashMap<>());

    private MetaValueCodec() {
        init();
    }

    private void init() {
        DataCodec<byte[], String> dataCodec = new DataCodec<byte[], String>() {
            /**
             *
             * @param input as base64
             * @return byte array
             */
            @Override
            public byte[] decode(String input) {
                return input != null ? SharedBase64.decode(SharedBase64.Base64Type.DEFAULT, input) : null;
            }

            /**
             *
             * @param input byte array
             * @return String as base64
             */
            @Override
            public String encode(byte[] input) {
                return input != null ? SharedBase64.encodeAsString(SharedBase64.Base64Type.DEFAULT, input) : null;
            }
        };
        codecMap.register(byte[].class, dataCodec);
        codecMap.register(NVBlob.class, dataCodec);


        codecMap.register(String.class, new DataCodec<String, String>() {
            @Override
            public String decode(String input) {
                return input;
            }

            @Override
            public String encode(String input) {
                return input;
            }
        });

    }

    public <I, V> DataCodec<I, V> lookupCodec(Object o) {
        Class<?> c;
        if (o instanceof Class)
            c = (Class<?>) o;
        else
            c = o.getClass();

        return codecMap.lookup(c);
    }

}
