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

import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.JarTool;
import org.zoxweb.shared.util.QuickLZ;
import org.zoxweb.shared.util.SharedStringUtil;

import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;

public class QuickLZTest {


  @Test
  public void testLZ(){

    try {
      String str = "MarioMarioMarioMarioMarioMarioMarioMarioMarioMarioMarioMarioMarioMarioMarioMarioMarioMarioMarioMarioMarioNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAELNAEL";

      byte compressed[] = QuickLZ.compress(str.getBytes(), 1);
      System.out.println("size " + compressed.length + " original " + str.length());
      byte decompress[] = QuickLZ.decompress(compressed);
      String newStr = new String(decompress);
      System.out.println(newStr.equals(str) + " " + newStr);

      ByteArrayOutputStream output = new ByteArrayOutputStream(str.length());
      GZIPOutputStream gzipOutputStream = new GZIPOutputStream(output);
      gzipOutputStream.write(str.getBytes());
      gzipOutputStream.finish();
      gzipOutputStream.flush();

      byte[] responseBytes = output.toByteArray();

      System.out.println("size " + responseBytes.length + " original " + str.length());

      decompress = QuickLZ.decompress(responseBytes);

      newStr = new String(decompress);
      System.out.println(newStr.equals(str) + " new str " + newStr);

      // gzip test
      byte[] zipBuffer = JarTool.gzip(str);
      String gunzipStr = SharedStringUtil.toString(JarTool.gunzip(zipBuffer));
      System.out.println(str.equals(gunzipStr) + " zipBuffer " + zipBuffer.length);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
