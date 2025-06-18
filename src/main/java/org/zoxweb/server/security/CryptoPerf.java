package org.zoxweb.server.security;

import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.SharedUtil;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashSet;

public class CryptoPerf {
    public static class Result {
        final long delta;
        final String message;
        final int count;
        final float tps;

        Result(long delta, int count, String message) {
            this.delta = delta;
            this.message = message;
            this.count = count;
            tps = (((float) count) / (float) delta) * 1000;
        }

        public String toString() {
            return "It took " + Const.TimeInMillis.toString(delta) + " " + message + " count: " + count + ", " + tps + " TPS";
        }
    }

    public static Result generateRandomNumber(CryptoConst.SecureRandomType randomType, int byteSize, int count) throws NoSuchAlgorithmException {
        SecureRandom sr = SecUtil.SINGLETON.newSecureRandom(randomType);
        byte buffer[] = new byte[byteSize];
        //HashSet<Object> set = new HashSet<>();

        long ts = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            sr.nextBytes(buffer);
        }
        ts = System.currentTimeMillis() - ts;
        return new Result(ts, count, "random generation " + randomType + " size in bytes: " + byteSize);
    }


    public static Result generateKeys(String keyName, int keySizeInBits, int count, boolean verify) throws NoSuchAlgorithmException {
        HashSet<Object> set = new HashSet<>();
        keyName = keyName.toLowerCase();
        long ts = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            switch (keyName) {
                case "aes":
                case "des":
                    if (verify) {
                        set.add(CryptoUtil.generateKey(keyName, keySizeInBits));
                    } else
                        assert CryptoUtil.generateKey(keyName, keySizeInBits) != null;
                    break;
                case "ec":
                case "rsa":
                    if (verify) {
                        set.add(CryptoUtil.generateKeyPair(keyName, keySizeInBits));
                    } else
                        assert CryptoUtil.generateKeyPair(keyName, keySizeInBits) != null;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported key type " + keyName);
            }
        }
        ts = System.currentTimeMillis() - ts;
        if (verify && set.size() != count)
            throw new IllegalArgumentException("size do not match " + set.size() + "!=" + count);

        return new Result(ts, count, "key generation " + keyName + " size in bits: " + keySizeInBits);

    }

    public static void main(String... args) {
        try {
            long delta = System.currentTimeMillis();
            int index = 0;
            String command = args[index++].toLowerCase();
            int repeat;
            int count;
            int sizeInBytes;
            boolean verify = false;
            switch (command) {
                case "gen-rnds":
                    CryptoConst.SecureRandomType srt = SharedUtil.lookupEnum(args[index++], CryptoConst.SecureRandomType.values());
                    sizeInBytes = Integer.parseInt(args[index++]);
                    count = Integer.parseInt(args[index++]);
                    repeat = Integer.parseInt(args[index++]);
                    for (int i = 0; i < repeat; i++) {
                        System.out.println(generateRandomNumber(srt, sizeInBytes, count));
                    }
                    break;
                case "gen-keys":
                    String keyName = args[index++];
                    int keySizeInBits = Integer.parseInt(args[index++]);
                    count = Integer.parseInt(args[index++]);
                    ;
                    repeat = Integer.parseInt(args[index++]);
                    verify = args.length > index && "-v".equalsIgnoreCase(args[index++]) ? true : false;
                    for (int i = 0; i < repeat; i++) {
                        System.out.println(generateKeys(keyName, keySizeInBits, count, verify));
                    }
                    break;
                default:
                    throw new IllegalArgumentException(command + " invalid command");
            }

            delta = System.currentTimeMillis() - delta;
            System.out.println("Over all it took: " + Const.TimeInMillis.toString(delta));
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println("Usage: command ...");
            System.err.println("generate random: gen-rnds [" + SharedUtil.toCanonicalID(',', (Object[]) CryptoConst.SecureRandomType.values()) + "] size-in-bytes count repeat");
            System.err.println("generate random: gen-keys [aes,des,rsa,ec]  key-size-in-bits count repeat");
        }
    }
}
