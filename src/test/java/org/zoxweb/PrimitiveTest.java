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
package org.zoxweb;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.BytesValue;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.SharedBase64;

import java.nio.ByteBuffer;

public class PrimitiveTest {

    @Test
    public void test() {

        int[] intVals = {
                1,
                0,
                123456789,
                -1,
                1000,
                Integer.MAX_VALUE,
                Integer.MIN_VALUE
        };

        for (int v : intVals) {
            byte[] bytes = BytesValue.INT.toBytes(v);
            System.out.println("INT: " + BytesValue.INT.toValue(bytes) + ":" + v);
            System.out.println(new String(SharedBase64.encode(bytes)));
        }

        long[] longVals =
                {
                        1,
                        0,
                        123456789,
                        -1,
                        1000,
                        Integer.MAX_VALUE,
                        Integer.MIN_VALUE,
                        (long) Long.MAX_VALUE,
                        (long) Long.MIN_VALUE
                };

        for (long v : longVals) {
            Long value = v;
            long delta = System.nanoTime();
            byte[] bytesJ = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(value).array();
            delta = System.nanoTime() - delta;

            long deltaZW = System.nanoTime();
            byte[] bytes = BytesValue.LONG.toBytes(value);
            deltaZW = System.nanoTime() - deltaZW;

            System.out.println(
                    "LONG:  " + BytesValue.LONG.toValue(bytes) + ":" + v + " zw:" + Const.TimeInMillis
                            .nanosToString(deltaZW) + ":" + Const.TimeInMillis.nanosToString(delta) + ":"
                            + Const.TimeInMillis.nanosToString(delta - deltaZW));
            System.out.println(
                    "LONG: " + BytesValue.LONG.toValue(bytesJ) + ":" + v + " zw:" + Const.TimeInMillis
                            .nanosToString(deltaZW) + ":" + Const.TimeInMillis.nanosToString(delta) + ":"
                            + Const.TimeInMillis.nanosToString(delta - deltaZW));
            System.out.println(new String(SharedBase64.encode(bytes)));
        }

        float[] floatVals =
                {
                        1,
                        0,
                        123456789,
                        -1,
                        1000,
                        Float.MAX_VALUE,
                        Float.MIN_VALUE,
                };

        for (float v : floatVals) {
            Float value = Float.valueOf(v);
            long delta = System.nanoTime();
            byte[] bytesJ = ByteBuffer.allocate(Float.SIZE / Byte.SIZE).putFloat(value).array();
            delta = System.nanoTime() - delta;

            long deltaZW = System.nanoTime();
            byte[] bytes = BytesValue.FLOAT.toBytes(value);
            deltaZW = System.nanoTime() - deltaZW;

            System.out.println(
                    "Float:  " + BytesValue.FLOAT.toValue(bytes) + ":" + v + " zw:" + Const.TimeInMillis
                            .nanosToString(deltaZW) + ":" + Const.TimeInMillis.nanosToString(delta) + ":"
                            + Const.TimeInMillis.nanosToString(delta - deltaZW));
            System.out.println(
                    "FloatJ: " + BytesValue.FLOAT.toValue(bytesJ) + ":" + v + " zw:" + Const.TimeInMillis
                            .nanosToString(deltaZW) + ":" + Const.TimeInMillis.nanosToString(delta) + ":"
                            + Const.TimeInMillis.nanosToString(delta - deltaZW));
            System.out.println(new String(SharedBase64.encode(bytes)));
        }
    }
}
