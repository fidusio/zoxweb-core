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

import org.zoxweb.shared.util.ReferenceID;

/**
 * This dynamic value filter is to declare the validation pattern.
 * @author mzebib
 *
 * @param <I>
 * @param <O>
 */
public interface DynamicValueFilter<I, O>
    extends ValueFilter<I, O>, ReferenceID<String>
{
	
	public String getValidationPattern();
	
	public void setValidationPattern( String str);
	
}