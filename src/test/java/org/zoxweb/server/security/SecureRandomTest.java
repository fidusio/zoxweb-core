/*
 * Copyright (c) 2012-2017 ZoxWeb.com LLC.
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

import java.security.SecureRandom;
import org.zoxweb.shared.crypto.CryptoConst.SecureRandomType;
import org.zoxweb.shared.util.SharedUtil;

public class SecureRandomTest {

  public static void main(String[] args) {
    for (SecureRandomType srt : SecureRandomType.values()) {
      try {
        SecureRandom sr = CryptoUtil.newSecureRandom(srt);
        System.out.println(
            SharedUtil.toCanonicalID(':', srt.getName(), sr.getAlgorithm(), sr.getProvider()));

      } catch (Exception e) {
        System.out.println(srt + " Not Supported");
      }
    }
  }
}
