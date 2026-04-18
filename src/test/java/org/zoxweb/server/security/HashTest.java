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

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.RateCounter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashTest {

//  public static void main(String[] args) {
//    try {
//      String alorigthm = null;
//      String[] seqs = null;
//      int index = 0;
//
//      for (int i = 0; i < args.length; i++) {
//        if (i == 0) {
//          alorigthm = args[i];
//        } else {
//          if (seqs == null) {
//            seqs = new String[args.length - 1];
//          }
//
//          seqs[index++] = args[i];
//        }
//      }
//
//      System.out.println(new String(SharedBase64.encode(SharedBase64.Base64Type.URL,
//          CryptoUtil.hmacSHA256("secret".getBytes(), "secret".getBytes()))));
//      System.out.println(SharedStringUtil
//          .bytesToHex(CryptoUtil.hmacSHA256("secret".getBytes(), "secret".getBytes())));
//      byte[] hash = HashUtil.hashSequence(alorigthm, seqs);
//      System.out.println(SharedStringUtil.bytesToHex(hash).toLowerCase());
//
//
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//
//  }

  @Test
  public void testHash256_1M() throws NoSuchAlgorithmException {
    RateCounter rateCounter = new RateCounter();

    int count = 100_000_000;
    byte[][] result = new byte[count][];

    MessageDigest md = MessageDigest.getInstance("sha256");
    //warmup
    md.digest(SecUtil.randomBytes(32));
    md.digest(SecUtil.randomBytes(32));
    md.digest(SecUtil.randomBytes(32));
    // warmup end
    rateCounter.start();
    for (int i = 0; i < count; i++) {
      //byte[] array = SecUtil.randomBytes(32);
      result[i] = md.digest(SecUtil.randomBytes(32));
    }
    rateCounter.stop(count);
    System.out.println("it took: " + rateCounter);
  }
}