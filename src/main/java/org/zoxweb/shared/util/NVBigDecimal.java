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

import java.math.BigDecimal;

/**
 * This class declares NVBase of BigDecimal type.
 * @author mzebib
 */
@SuppressWarnings("serial")
public class NVBigDecimal
    extends NVBase<BigDecimal>
{

	/**
	 * The default constructor.
	 */
	public NVBigDecimal()
    {
		super();
	}
	
	/**
	 * This constructor instantiates NVBigDecimal based on name and value. 
	 * @param name
	 * @param value
	 */
	public NVBigDecimal(String name, BigDecimal value)
    {
		super(name, value);
	}
	public NVBigDecimal(GetName name, BigDecimal value)
	{
		super(name, value);
	}

}
