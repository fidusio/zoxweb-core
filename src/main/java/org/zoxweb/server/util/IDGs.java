package org.zoxweb.server.util;

import org.zoxweb.server.security.CryptoUtil;
import org.zoxweb.server.security.HashUtil;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.IDGenerator;
import org.zoxweb.shared.util.SharedBase64;
import org.zoxweb.shared.util.SharedBase64.Base64Type;

import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class IDGs {
    public static final IDGenerator<String, byte[]> UUIDSHA256Base64 = new IDGenerator<String, byte[]>() {

        /**
         *
         * @param input
         * @return
         */
        @Override
        public byte[] decode(String input) {
            return SharedBase64.decode(Base64Type.URL, input);
        }

        /**
         *
         * @param input
         * @return
         */
        @Override
        public String encode(byte[] input) {
            return SharedBase64.encodeAsString(Base64Type.URL, input);
        }

        @Override
        public String generateID() {

            try {
                String ret;
                do {
                    ret = SharedBase64.encodeAsString(Base64Type.URL, HashUtil.hashSequence("SHA-256", UUID.randomUUID().toString()));
                } while (!valide(ret));

                return ret;
            } catch (NoSuchAlgorithmException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            throw new IllegalArgumentException("ID generation failed");


        }

        @Override
        public String getName() {
            // TODO Auto-generated method stub
            return "UUIDSHA256Base64";
        }

        public byte[] generateNativeID() {
            byte[] ret = null;

            do {
                try {
                    ret = SharedBase64.encode(Base64Type.URL, HashUtil.hashSequence("SHA-256", UUID.randomUUID().toString()));
                } catch (NoSuchAlgorithmException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } while (!valide(ret));

            return ret;

        }
    };


    public static final IDGenerator<String, byte[]> SHA256Base64 = new IDGenerator<String, byte[]>() {

        /**
         *
         * @param input
         * @return
         */
        @Override
        public byte[] decode(String input) {
            return SharedBase64.decode(Base64Type.URL, input);
        }

        /**
         *
         * @param input
         * @return
         */
        @Override
        public String encode(byte[] input) {
            return SharedBase64.encodeAsString(Base64Type.URL, input);
        }

        @Override
        public String generateID() {
            String ret = null;

            do {
                try {
                    ret = SharedBase64.encodeAsString(Base64Type.URL, CryptoUtil.generateKey(CryptoConst.CryptoAlgo.AES, (Const.TypeInBytes.BYTE.sizeInBits(CryptoConst.AES_256_KEY_SIZE))).getEncoded());
                } catch (NoSuchAlgorithmException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } while (!valide(ret));

            return ret;
        }

        @Override
        public String getName() {
            // TODO Auto-generated method stub
            return "SHA256Base64";
        }

        public byte[] generateNativeID() {

            byte[] ret = null;

            do {
                try {
                    ret = SharedBase64.encode(Base64Type.URL, CryptoUtil.generateKey(CryptoConst.CryptoAlgo.AES, (Const.TypeInBytes.BYTE.sizeInBits(CryptoConst.AES_256_KEY_SIZE))).getEncoded());
                } catch (NoSuchAlgorithmException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } while (!valide(ret));

            return ret;

        }
    };

    public static final IDGenerator<String, byte[]> UUIDBase64 = new IDGenerator<String, byte[]>() {

        @Override
        public String generateID() {
            String ret;

            do {
                ret = SharedBase64.encodeAsString(Base64Type.URL, UUID.randomUUID().toString());
            } while (!valide(ret));

            return ret;
        }

        public byte[] generateNativeID() {
            byte[] ret;

            do {
                ret = SharedBase64.encode(Base64Type.URL, UUID.randomUUID().toString());
            } while (!valide(ret));

            return ret;
        }

        /**
         *
         * @param input
         * @return
         */
        @Override
        public byte[] decode(String input) {
            return SharedBase64.decode(Base64Type.URL, input);
        }

        /**
         *
         * @param input
         * @return
         */
        @Override
        public String encode(byte[] input) {
            return SharedBase64.encodeAsString(Base64Type.URL, input);
        }


        @Override
        public String getName() {
            // TODO Auto-generated method stub
            return "UUIDBase64";
        }
    };

    public static final IDGenerator<String, UUID> UUIDV4 = new IDGenerator<String, UUID>() {

        /**
         *
         * @param input
         * @return
         */
        @Override
        public UUID decode(String input) {
            return UUID.fromString(input);
        }

        /**
         *
         * @param input
         * @return
         */
        @Override
        public String encode(UUID input) {
            return input.toString();
        }

        @Override
        public String getName() {
            // TODO Auto-generated method stub
            return "UUID-V4";
        }

        @Override
        public String generateID() {
            // TODO Auto-generated method stub
            return generateNativeID().toString();
        }

        public UUID generateNativeID() {
            // TODO Auto-generated method stub
            return UUID.randomUUID();
        }


    };


    private IDGs() {
    }


    private static boolean valide(String str) {

        if (str == null || str.charAt(0) == '_' || str.charAt(0) == '-') {
            return false;
        }
        return true;
    }

    private static boolean valide(byte[] bytes) {

        if (bytes == null || bytes[0] == '_' || bytes[0] == '-') {
            return false;
        }
        return true;
    }

}
