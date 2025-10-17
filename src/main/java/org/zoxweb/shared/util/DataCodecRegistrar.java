package org.zoxweb.shared.util;

public class DataCodecRegistrar
        extends RegistrarMapDefault<Object, DataCodec<?, ?>> {
    public static final DataCodecRegistrar SINGLETON = new DataCodecRegistrar();
    public static class BinaryCodec
            implements DataCodec<byte[], String> {
        public static final BinaryCodec SINGLETON = new BinaryCodec();

        private BinaryCodec() {
        }

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
    }

    private DataCodecRegistrar() {
        super(k -> {
                    if (k instanceof Class)
                        return k;
                    else return k.getClass();
                },
                null);
        init();
    }

    private void init() {
        register(byte[].class, BinaryCodec.SINGLETON);
        register(NVBlob.class, BinaryCodec.SINGLETON);


        register(String.class, new DataCodec<String, String>() {
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

}
