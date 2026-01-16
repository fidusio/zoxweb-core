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
import org.zoxweb.shared.util.SharedBase64;
import org.zoxweb.shared.util.SharedStringUtil;

public class CryptoTest {
    @Test
    public void secureRandom() {
        System.out.println(SecUtil.defaultSecureRandom().getAlgorithm());
        byte[] randomBytes = new byte[768 / 8];
        SecUtil.defaultSecureRandom().nextBytes(randomBytes);

        for (int i = 0; i < 20; i++) {
            long ts = System.nanoTime();
            SecUtil.defaultSecureRandom().nextBytes(randomBytes);
            ts = System.nanoTime() - ts;
            System.out.println(
                    ts + "\tnanos\t" + new String(SharedBase64.encode(randomBytes)) + ":" + SharedStringUtil
                            .bytesToHex(randomBytes));
        }
    }

}