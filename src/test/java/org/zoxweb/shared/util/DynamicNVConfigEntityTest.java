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
package org.zoxweb.shared.util;

import org.zoxweb.shared.data.FileInfoDAO;
import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

import java.util.ArrayList;
import java.util.List;

public class DynamicNVConfigEntityTest {

    public static String metaWalker(NVConfig nvc) {

        StringBuilder sb = new StringBuilder();

        sb.append("name: ");
        sb.append(nvc.getName());
        sb.append(", ");

        sb.append("description: ");
        sb.append(nvc.getDescription());
        sb.append(", ");

        sb.append("display name: ");
        sb.append(nvc.getDisplayName());
        sb.append(", ");

        sb.append("property mandatory: ");
        sb.append(nvc.isMandatory());
        sb.append(", ");

        sb.append("property editable: ");
        sb.append(nvc.isEditable());
        sb.append(", ");

        sb.append("Meta type: ");
        sb.append(nvc.getMetaType());
        sb.append(", ");

        sb.append("Value filter: ");
        sb.append(nvc.getValueFilter());
        sb.append("\n");


        if (nvc instanceof NVConfigEntity) {
            List<NVConfig> list = ((NVConfigEntity) nvc).getAttributes();

            for (NVConfig nv : list) {
                sb.append(metaWalker(nv));
            }
        }

        return sb.toString();
    }

    public static void main(String[] args) {
        ArrayList<NVConfig> attr = new ArrayList<NVConfig>();
        NVConfigEntity nvConfigEntity = new NVConfigEntityLocal();

        ArrayList<NVConfig> display = new ArrayList<NVConfig>();
        nvConfigEntity.setDisplayAttributes(display);
        nvConfigEntity.setAttributes(attr);
        nvConfigEntity.setName("dynamic_nvconfigentity");
        nvConfigEntity.setDisplayName("Dynamic NVConfigEntity Test");
        nvConfigEntity.setDescription("DynamicNVConfigEntity");
        nvConfigEntity.setEditable(false);
        nvConfigEntity.setMandatory(true);

        //FileInfoDAO
        NVConfigEntity nvcFileInfoDAO = new NVConfigEntityLocal("file_info", "File info data access object", "FileInfoDAO", true, false, false, false, FileInfoDAO.NVC_FILE_INFO_DAO, false, ArrayType.NOT_ARRAY);
        nvConfigEntity.getAttributes().add(nvcFileInfoDAO);

        //String
        NVConfig nvconfigAttribute1 = new NVConfigPortable("string", "This is a string type.", "String", true, false, false, false, false, String.class, null);
        nvConfigEntity.getAttributes().add(nvconfigAttribute1);
        display.add(nvconfigAttribute1);

        //Integer
        NVConfig nvconfigAttribute2 = new NVConfigPortable();
        nvconfigAttribute2.setMetaType(Integer.class);
        nvconfigAttribute2.setName("integer");
        nvconfigAttribute2.setDisplayName("Integer");
        nvconfigAttribute2.setDescription("This is an integer type.");
        nvconfigAttribute2.setEditable(false);
        nvconfigAttribute2.setMandatory(true);
        nvConfigEntity.getAttributes().add(nvconfigAttribute2);

        //Float
        NVConfig nvconfigAttribute3 = NVConfigManager.createNVConfig("float", "This is a float type.", "Float", true, false, Float.class);
        nvConfigEntity.getAttributes().add(nvconfigAttribute3);

        //Double
        NVConfig nvconfigAttribute4 = NVConfigManager.createNVConfig("double", "This is a double type.", "Double", true, false, Double.class);
        nvConfigEntity.getAttributes().add(nvconfigAttribute4);
        display.add(nvconfigAttribute4);

        //Long
        NVConfig nvconfigAttribute5 = new NVConfigPortable();
        nvconfigAttribute5.setMetaType(Long.class);
        nvconfigAttribute5.setName("long");
        nvconfigAttribute5.setDisplayName("Long");
        nvconfigAttribute5.setDescription("This is a long type.");
        nvconfigAttribute5.setEditable(false);
        nvconfigAttribute5.setMandatory(true);
        nvConfigEntity.getAttributes().add(nvconfigAttribute5);
        display.add(nvconfigAttribute5);

        //Boolean
        NVConfig nvconfigAttribute6 = new NVConfigPortable("boolean", "This is a boolean type.", "Boolean", true, false, false, false, false, Boolean.class, null);
        nvConfigEntity.getAttributes().add(nvconfigAttribute6);

        //Email
        NVConfig nvconfigAttribute7 = NVConfigManager.createNVConfig("email", "Enter your email: ", "Email", true, false, true, String.class, FilterType.EMAIL);
        nvConfigEntity.getAttributes().add(nvconfigAttribute7);

        //String Array
        NVConfig nvconfigAttribute8 = NVConfigManager.createNVConfig("string_array", "This is a string array type.", "StringArray", true, false, String[].class);
        nvConfigEntity.getAttributes().add(nvconfigAttribute8);

        //Integer Array
        NVConfig nvconfigAttribute9 = NVConfigManager.createNVConfig("integer_array", "This is an integer array type.", "IntegerArray", true, false, Integer[].class);
        nvConfigEntity.getAttributes().add(nvconfigAttribute9);
        display.add(nvconfigAttribute5);

        //Float Array
        NVConfig nvconfigAttribute10 = NVConfigManager.createNVConfig("float_array", "This is a float array type.", "FloatArray", true, false, Float[].class);
        nvConfigEntity.getAttributes().add(nvconfigAttribute10);

        //Boolean Array
        NVConfig nvconfigAttribute11 = NVConfigManager.createNVConfig("boolean_array", "This is a boolean array type.", "BooleanArray", true, false, Boolean[].class);
        nvConfigEntity.getAttributes().add(nvconfigAttribute11);

        //Double Array
        NVConfig nvconfigAttribute12 = NVConfigManager.createNVConfig("double_array", "This is a double array type.", "DoubleArray", true, false, Double[].class);
        nvConfigEntity.getAttributes().add(nvconfigAttribute12);

        //Long Array
        NVConfig nvconfigAttribute13 = NVConfigManager.createNVConfig("long_array", "This is a long array type.", "LongArray", true, false, Long[].class);
        nvConfigEntity.getAttributes().add(nvconfigAttribute13);

        //Byte Array
        NVConfig nvconfigAttribute14 = NVConfigManager.createNVConfig("byte_array", "This is a byte array type.", "ByteArray", true, false, Byte[].class);
        nvConfigEntity.getAttributes().add(nvconfigAttribute14);

        System.out.println("Meta Walker: ");
        System.out.println(metaWalker(nvConfigEntity));


        //Print list of attributes
//		System.out.println("List of Attributes:");
//		for(int i = 0; i < attr.size(); i++)
//		{
//			System.out.println("[" + i + "]" + nvConfigEntity.getAttributes().get(i));
//		}
//		
//		//Print list of display attributes
//		System.out.println("List of Display Attributes:");
//		for(int j = 0; j < display.size(); j++)
//		{
//			System.out.println(nvConfigEntity.getDisplayAttributes().get(j));
//		}
//		
//		System.out.println("Walk Attributes List: ");
//		for(NVConfig nvc : attr)
//		{
//			System.out.println("NVConfig " + attr.indexOf(nvc) + ": [" + metaWalker(nvc) + " ]");
//			
//		}
//		
//		System.out.println("Walk Display Attributes List: ");
//		for(NVConfig nvc : display)
//		{
//			System.out.println("NVConfig " + display.indexOf(nvc) + ": [" + metaWalker(nvc) + " ]");
//			
//		}

//		System.out.println("Look up NVConfig: ");
//		System.out.println(nvConfigEntity.lookup("integer"));
//		System.out.println(nvConfigEntity.lookup("byte_array"));
//		System.out.println(nvConfigEntity.lookup("test"));
    }

}