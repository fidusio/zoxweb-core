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

/**
 * This class declares NVBase of byte array type.
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class NVBlob
    extends NVBase<byte[]>
{
	
	/**
	 * The default constructor.
	 */
	public NVBlob() 
	{

	}
	
	/**
	 * This constructor instantiates NVBlob based on name and byte array.
	 * @param name
	 * @param value
	 */
	public NVBlob(String name, byte[] value)
    {
		super(name, value);
	}

}