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
package org.zoxweb;

import org.zoxweb.server.util.ServerUtil;

public class TimerTest {

    public static void runTest(long nanos) {
        long delta = System.nanoTime();

        long error = ServerUtil.delay(nanos);

        delta = System.nanoTime() - delta;

        System.out.println("Total time in nanos:" + delta + " requeued time:" + nanos + " error:" + (error));
    }

    public static void main(String[] args) {
        for (int i = 0; i < 3; i++)
            for (String param : args) {
                try {
                    runTest(Long.parseLong(param));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

    }

}
