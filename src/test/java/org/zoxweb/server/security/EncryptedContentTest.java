/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.zoxweb.server.security;

import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.crypto.CryptoConst.HashType;
import org.zoxweb.shared.crypto.CryptoConst.SecureRandomType;
import org.zoxweb.shared.crypto.EncryptedData;
import org.zoxweb.shared.crypto.EncapsulatedKey;
import org.zoxweb.shared.crypto.CIPassword;
import org.zoxweb.shared.util.SharedStringUtil;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.concurrent.atomic.AtomicLong;

public class EncryptedContentTest {

    private static AtomicLong totalTime = new AtomicLong();

    public static void jckTest()
            throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        File f = new File("/home/fidus-store/ssl/fidus-store.jck");

        if (f.exists() && f.canRead()) {
            SecretKeySpec k = (SecretKeySpec) CryptoUtil
                    .getKeyFromKeyStore(new FileInputStream("/home/fidus-store/ssl/fidus-store-test.jck"),
                            CryptoConst.KEY_STORE_TYPE, "changeit", "fidus-store-mk", "changeit");

            System.out.println(k.getAlgorithm() + ":" + k.getFormat() + ":" + SharedStringUtil
                    .bytesToHex(k.getEncoded()));
            File fNewStore = new File("d:/home/fidus-store/ssl/update-fidus-store.jck");
            fNewStore.createNewFile();

            CryptoUtil.updateKeyPasswordInKeyStore(
                    new FileInputStream("/home/fidus-store/ssl/fidus-store-test.jck"),
                    CryptoConst.KEY_STORE_TYPE,
                    "changeit", "fidus-store-mk", "changeit",
                    new FileOutputStream(fNewStore), "password", "fidus-store-mk", "password");

            k = (SecretKeySpec) CryptoUtil
                    .getKeyFromKeyStore(new FileInputStream(fNewStore), CryptoConst.KEY_STORE_TYPE, "password",
                            "fidus-store-mk", "password");
            System.out.println(k.getAlgorithm() + ":" + k.getFormat() + ":" + SharedStringUtil
                    .bytesToHex(k.getEncoded()));

            KeyStore ks = CryptoUtil
                    .loadKeyStore(new FileInputStream(fNewStore), CryptoConst.KEY_STORE_TYPE,
                            "password".toCharArray());

            char[] aliasPassword = "password".toCharArray();

            for (int i = 0; i < 10; i++) {
                long tempTS = System.nanoTime();
                Key tempKey = ks.getKey("fidus-store-mk", aliasPassword);
                tempTS = System.nanoTime() - tempTS;
                System.out.println("[" + i + "] took " + tempTS + " nanos. " + SharedStringUtil
                        .bytesToHex(tempKey.getEncoded()));
            }
        }
    }

    public static void test(int repeat) {

        for (int r = 0; r < repeat; r++) {
            long delta = System.currentTimeMillis();
            //		EncryptedDAO ec = new EncryptedDAO();
            //		ArrayValues<NVPair> subjectTokens = ec.getSubjectProperties();
            //		System.out.println(subjectTokens.getClass().getCanonicalName());
            //		subjectTokens.add( new NVPair("message","id"));
            //
            //		ec.setName("aes-256");
            //		ec.setAlgoProperties(null);
            //		ec.setIV( "IV".getBytes());
            //		ec.setEncryptedData( "data".getBytes());
            //		ec.setExpirationTime( ""+System.currentTimeMillis());
            //		ec.setHint(null);
            //		ec.setHMACAlgoName("HmacSHA256");
            //		ec.setHMAC(  "hmac".getBytes());

            //		GsonBuilder builder = new GsonBuilder();
            //		builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
            //		builder.setPrettyPrinting();
            //
            //		Gson gson = builder.create();
            //		String gsonString = GSONUtil.toJSON(ec, true);
            //		System.out.println(gsonString);
            //		System.out.println(ec.toCanonicalID());
            //
            //		EncryptedDAO fromJson = GSONUtil.fromJSON(gsonString, EncryptedDAO.class);
            //		System.out.println(fromJson.toCanonicalID());

            try {

                //			String gsonString = GSONUtil.toJSON(ec, true, false, false);
                //			System.out.println(gsonString);
                //			System.out.println(ec.toCanonicalID());

                //			EncryptedDAO fromJson = GSONUtil.fromJSON(gsonString, EncryptedDAO.class);
                //			System.out.println(fromJson.toCanonicalID());
                CIPassword passwordDAO = null;
                for (int i = 0; i < 10; i++) {
                    long ts = System.nanoTime();
                    passwordDAO = HashUtil.toPassword("sha-256", 0, 8196, "password");
                    ts = System.nanoTime() - ts;
                    System.out.println("it took " + ts + " nanos for interation " + i);
                }

                //System.out.println( passwordDAO.toCanonicalID());
                System.out.println("passwordDAO:" + SecUtil.isPasswordValid(passwordDAO, "password"));
                CIPassword passwordFromCanonicalID = SecUtil.findCredentialHasherByCanID(passwordDAO.getCanonicalID())
                        .fromCanonicalID(passwordDAO.toCanonicalID());
                System.out.println("passwordFromCanonicalID:" + SecUtil
                        .isPasswordValid(passwordFromCanonicalID, "password"));
                System.out.println(passwordFromCanonicalID.toCanonicalID());
                String json = GSONUtil.toJSON(passwordDAO, true, false, false);
                System.out.println(json);
                System.out.println("json length:" + json.length());

                CIPassword fromJSON = GSONUtil.fromJSON(json, CIPassword.class);
                System.out.println(passwordDAO.toCanonicalID());
                System.out.println(fromJSON.toCanonicalID());

                System.out.println("password is equal " + SecUtil.isPasswordValid(fromJSON, "password"));

                EncapsulatedKey ekd = CryptoUtil.createEncryptedKey("password");
                String ekdJSON = GSONUtil.toJSON(ekd, true, false, false);
                System.out.println(ekdJSON);
                byte[] key = CryptoUtil.decryptEncryptedData(ekd, "password");
                System.out.println(key.length + " " + SharedStringUtil.bytesToHex(key));
                System.out.println(key.length + " " + SharedStringUtil.bytesToHex(ekd.getEncryptedData()));

                EncapsulatedKey ekdFromJSON = GSONUtil.fromJSON(ekdJSON, EncapsulatedKey.class);

                key = CryptoUtil.decryptEncryptedData(ekdFromJSON, "password");
                System.out.println("from json       key:" + SharedStringUtil.bytesToHex(key));
                System.out.println(
                        "from json encrypted:" + SharedStringUtil.bytesToHex(ekdFromJSON.getEncryptedData()));

                ekd = CryptoUtil.rekeyEncryptedKey(ekd, "password", "newpassword");
                key = CryptoUtil.decryptEncryptedData(ekd, "newpassword");

                System.out.println("rekeyed         key:" + SharedStringUtil.bytesToHex(key));

                MessageDigest digest = HashUtil.getMessageDigest(HashType.SHA_256);

                for (int i = 0; i < 10; i++) {
                    long ts = System.nanoTime();
                    SecretKey sk = CryptoUtil.generateKey(CryptoConst.CryptoAlgo.AES, 256);
                    ts = System.nanoTime() - ts;
                    System.out.print("[" + ts + " ns]");
                    System.out.println(sk.getAlgorithm() + "," + sk.getFormat() + "," + SharedStringUtil
                            .bytesToHex(sk.getEncoded()));

                    ts = System.nanoTime();
                    byte[] randomBytes = CryptoUtil
                            .generateRandomHashedBytes(digest, CryptoConst.AES_256_KEY_SIZE,
                                    CryptoUtil.DEFAULT_ITERATION);
                    ts = System.nanoTime() - ts;
                    System.out.print("[" + ts + " ns]");
                    System.out.println(
                            "generateRandomHashedBytes" + "," + SharedStringUtil.bytesToHex(randomBytes));

                    ts = System.nanoTime();
                    EncapsulatedKey ekdTest = CryptoUtil.createEncryptedKey("password");
                    ts = System.nanoTime() - ts;
                    System.out.print("[" + ts + " ns]");
                    System.out.println(ekdTest.getName() + "\t,Encrypted data " + SharedStringUtil
                            .bytesToHex(ekdTest.getEncryptedData()));

                    ts = System.nanoTime();
                    byte[] drecryptedKey = CryptoUtil.decryptEncryptedData(ekdTest, ("password"));
                    ts = System.nanoTime() - ts;
                    System.out.print("[" + ts + " ns]");
                    System.out.println(ekdTest.getName() + "\t,Decrypted data " + SharedStringUtil
                            .bytesToHex(drecryptedKey));
                }

                //key = CryptoUtil.decryptKey(ekd, "password");
                // C8FC42534A6D00F82DEC5CE76FBD9C57E9DB38C6A486D42AD722179B6F86C953
                // C8FC42534A6D00F82DEC5CE76FBD9C57E9DB38C6A486D42AD722179B6F86C953

                String[] passwordCanonicalIDS =
                        {
                                "sha-256:8196:D4EF1750E7168583FF15A7A56E95071D04EE81FB8B36665B2A30DD0828146DC5:E70FC787EEC4AB925A1418FDB2DEFBCD91DD1D56DE7B0F25C4BD43BEB751279E",
                                "8196:D4EF1750E7168583FF15A7A56E95071D04EE81FB8B36665B2A30DD0828146DC5:E70FC787EEC4AB925A1418FDB2DEFBCD91DD1D56DE7B0F25C4BD43BEB751279E",
                                "sha-256::D4EF1750E7168583FF15A7A56E95071D04EE81FB8B36665B2A30DD0828146DC5:E70FC787EEC4AB925A1418FDB2DEFBCD91DD1D56DE7B0F25C4BD43BEB751279E",
                                "sha-256:54:D4EF1750E7168583FF15A7A56E95071D04EE81FB8B36665B2A30DD0828146DC5:E70FC787EEC4AB925A1418FDB2DEFBCD91DD1D56fereDE7B0F25C4BD43BEB751279E",
                                ":::",
                                "   		",
                                null,
                                "-1:D4EF1750E7168583FF15A7A56E95071D04EE81FB8B36665B2A30DD0828146DC5:E70FC787EEC4AB925A1418FDB2DEFBCD91DD1D56DE7B0F25C4BD43BEB751279E",
                                "sha-256:8196:D4EF1750E7168583FF15A7A56E95071D04EE81FB8B36665B2A30DD0828146DC5:E70FC787EEC4AB925A1418FDB2DEFBCD91DD1D56DE7B0F25C4BD43BEB751279E:",

                        };

                System.out.println("\n\n\nTesting canonical conversion");

                for (String canId : passwordCanonicalIDS) {
                    System.out.println(
                            "==============================================================================");
                    CIPassword pd = null;
                    try {
                        pd = SecUtil.fromCanonicalID(canId);

                    } catch (Exception e) {
                        System.out.println(e);
                    }

                    System.out.println(canId);
                    System.out.println("conversion:" + (pd != null ? pd.toCanonicalID() : "FAILED"));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            for (HashType mdt : HashType.values()) {
                try {
                    CIPassword pDAO = HashUtil.toPassword(mdt.getName(), 0, -1, "password");
                    System.out.println(SecUtil
                            .isPasswordValid(SecUtil.fromCanonicalID(pDAO.toCanonicalID()), "password")
                            + "::::" + pDAO.toCanonicalID());
                } catch (NullPointerException | IllegalArgumentException
                         | NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }

            try {
                SecretKey sk = CryptoUtil.generateKey(CryptoConst.CryptoAlgo.AES, 256);

                byte[] data = "Marwan NAEL is the best of the b".getBytes(StandardCharsets.UTF_8);

                EncryptedData ed = CryptoUtil.encryptData(new EncryptedData(), sk.getEncoded(), data);
                //ed.setDataLength(16);
                System.out.println(ed.toCanonicalID());
                System.out.println(new String(CryptoUtil.decryptEncryptedData(ed, sk.getEncoded())));

                System.out.println(GSONUtil.toJSON(ed, true));

                ed = CryptoUtil.encryptData(new EncryptedData(), sk.getEncoded(), new byte[0]);
                //ed.setDataLength(16);
                System.out.println(ed.toCanonicalID());
                System.out.println(new String(CryptoUtil.decryptEncryptedData(ed, sk.getEncoded())));

                System.out.println(GSONUtil.toJSON(ed, true, true, true));
                ed = CryptoUtil.encryptData(new EncryptedData(), sk.getEncoded(), data);
                EncryptedData fromCanID = EncryptedData.fromCanonicalID(ed.toCanonicalID());
                System.out.println(new String(CryptoUtil.decryptEncryptedData(fromCanID, sk.getEncoded())));
            } catch (Exception e) {
                e.printStackTrace();
            }

            delta = System.currentTimeMillis() - delta;
            totalTime.addAndGet(delta);
            System.out.println(
                    "[" + r + "] " + "full time " + delta + " millis, total " + totalTime.get() + " thead"
                            + Thread.currentThread());
        }
    }

    static int repeat = 1;

    public static void main(String... args) {
        //CryptoUtil.SECURE_RANDOM_ALGO = SecureRandomType.SHA1PRNG;
        int threadCount = 1;
        int i = 0;

        if (args.length > 0) {
            repeat = Integer.parseInt(args[i++]);
        }

        if (args.length > 1) {
            threadCount = Integer.parseInt(args[i++]);
        }

        if (args.length > 2) {
            SecUtil.SECURE_RANDOM_ALGO = SecureRandomType.lookup(args[i++]);
        }

        for (int t = 0; t < threadCount; t++) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    test(repeat);
                }

            }).start();
        }
    }
}
