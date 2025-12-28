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
package org.zoxweb.shared.util;

import java.util.HashMap;

public class SharedUtilTest {

	private enum enumList{LEFT, RIGHT}

	@SuppressWarnings("rawtypes")
	public static void main(String[] arg) {
		String stringArray = "hi";
		
		for (int i = 0; i < enumList.values().length; i++) {
			stringArray = SharedUtil.toCanonicalID('/', enumList.values()[i]);
		}
		
		System.out.println(stringArray);
		
		Class[] classType = {int.class, String.class, Float.class, Double.class, Exception.class, void.class, char.class};
		
		for (int i = 0; i < classType.length; i++) {
			System.out.println(classType[i] + ": " + SharedUtil.isPrimitive(classType[i]));
		}

		String prefix = "Mr. ";
		String name = "Mr. Mustapha";
		System.out.println(SharedUtil.removePrefix(prefix, name));
		
		
		String str[] = new String[3];
		str[0] = "Zox";
		str[1] = "Web";
		str[2] = "Core";
		
		System.out.println(SharedUtil.toString(str));
		System.out.println(SharedUtil.toString(str,"-"));	
		System.out.println(SharedUtil.toString(str,".", false));
		System.out.println(SharedUtil.toString(str,",",true));
		
		
		String test = "Java";
		int value = 3;
		
		System.out.println(SharedStringUtil.format(test, value));
		
		
//		System.out.println(Const.SizeInBytes.B);
//		System.out.println(Const.SizeInBytes.K.length);
//		System.out.println(Const.SizeInBytes.parse("hello"));
//		
//		
//		for(int i = 0; i < SharedBase64.REVERSE_BASE_64.length; i++){
//			
//			System.out.print( "[" +i+"]:" +(char) i + ":" + SharedBase64.REVERSE_BASE_64[i] + ",");
//		}
//		
//		System.out.println("\nLength of REVERSE_BASE_64 Array: " + SharedBase64.REVERSE_BASE_64.length);
//		
//		
		NVBase<Integer> nvTest1 = new NVBase<Integer>("Hello", 125);
		nvTest1.setReferenceID(""+125555);
		System.out.println(nvTest1.getReferenceID());
		nvTest1.setName("Mustapha");
		System.out.println(nvTest1.getName());
		nvTest1.setValue(125);
		System.out.println(nvTest1.getValue());
				
		NVBase<Integer> nvTest2 = new NVBase<Integer>("Marwan", 100);
		nvTest2.setReferenceID(""+125556);
		System.out.println(nvTest2.getReferenceID());
		System.out.println(nvTest2.getName());
		System.out.println(nvTest2.getValue());
		
		
		NVBase<String> nvTest3 = new NVBase<String>("Marwan", "Nael");
		nvTest3.setReferenceID(""+125556);
		System.out.println(nvTest3.getReferenceID());
		System.out.println(nvTest3.getName());
		System.out.println(nvTest3.getValue());
		System.out.println(nvTest3.toString());
		
		NVPairGetNameMap nvpgnm = new NVPairGetNameMap("test", new HashMap<GetName, GetNameValue<String>>());
		
		nvpgnm.add( new NVPair("marwan", "nael"));
		System.out.println(nvpgnm.get("MarwAna"));

		System.out.println(SharedStringUtil.bytesToHex("%", SharedStringUtil.getBytes("Marwan NAEL")));
		
		System.out.println(SharedStringUtil.stringToHex("%", "Marwan NAEL"));
	}

}