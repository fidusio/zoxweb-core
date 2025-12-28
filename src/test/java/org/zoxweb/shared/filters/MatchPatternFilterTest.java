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
package org.zoxweb.shared.filters;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.SharedStringUtil;

import java.io.FileInputStream;

public class MatchPatternFilterTest
{


	public static void main(String[] args) {

		try {
			String str = IOUtil.inputStreamToString(new FileInputStream("D:\\temp\\match_test.txt"), "utf-8", true);
			String[] values = str.split("\r\n");
			
			for (String value : values) {
				System.out.println("Hex Value: " + SharedStringUtil.bytesToHex(value.getBytes("utf-8")));
			}
			
			String temp = values[values.length - 1];

			String arabicText = new String(
			        new byte[]{(byte) 0xD9,(byte) 0x82,(byte) 0xD8,(byte) 0xB6,(byte) 0xD9,(byte) 0x8A,(byte) 0xD8,(byte) 0xA8},
								"utf-8");
			
			System.out.println("Text (in Arabic): " + arabicText);
					
			String[] patterns = {"-r", "-i", "*.java", "*.pdf", "*.java.*", "D?c?m?nts.pdf", "*" + temp + "*", "*.txt", "*"};
			MatchPatternFilter mpf = MatchPatternFilter.createMatchFilter(patterns);
			
			System.out.println("Recrusive: " + mpf.isRecursive());
			System.out.println("Case Sensitive: " + mpf.isCaseSensitive());
			
			for (String tempStr : mpf.getMatchPatterns()) {
				System.out.println("Pattern: " + tempStr);
			}
			
			for (String val : values) {
				long delta = System.nanoTime();
				
				boolean stat = mpf.match(val);
				
				delta = System.nanoTime() - delta;
				
				System.out.println("Check: " + val + "  Match Found: " + stat + " " + Const.TimeInMillis.nanosToString(delta));
			}
	
//			String[] test = {
//								"classname.java", 
//								"H4llo.cafe", 
//								"*.pdf", 
//								"\\*.java", 
//								"fidus-store\\*.java", 
//								"\\fidus-store\\fidus-store\\*.java.aes", 
//								"D1c2m3nts.pdf", 
//								"/*.java", 
//								"hello.exe", 
//								"\\fidus-store\\doc.java",
//								"\\fidus-store\\My Documents\\test.pdf",
//							};
//			
//			for (String str : test)
//			{
//				System.out.println("Check: " + str + "  Match Found: " + mpf.match(str));
//			}

			for (int i = 0; i < temp.length(); i++) {
				System.out.println(Character.valueOf(temp.charAt(i)));
			}
			
			System.out.println("TEMP: " + SharedStringUtil.bytesToHex(temp.getBytes("utf-8")) + " Lenght: " + temp.length());
			System.out.println("Text Hex Value (in Arabic): " + SharedStringUtil.bytesToHex(arabicText.getBytes("utf-8")));
			System.out.println("Equal: " + arabicText.equals(temp));
			System.out.println("Check: " + "\uD982\uD8B6\uD98A\uD8A8" + "  Match Found: " + mpf.match(arabicText));
			
//			System.out.println("\nPatterns:");
//			System.out.println(MatchPatternFilter.ASTERIK_CHARACTER_PATTERN);
//			System.out.println(MatchPatternFilter.DEFAULT_PATTERN_PREFIX);
//			System.out.println(MatchPatternFilter.SINGLE_CHARACTER_PATTERN);
//			System.out.println(MatchPatternFilter.SINGLE_PATTERN_OCCURANCE_VALUE);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}