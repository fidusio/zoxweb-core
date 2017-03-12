/*
 * Copyright (c) 2012-May 27, 2014 ZoxWeb.com LLC.
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
package org.zoxweb.shared.api;

import java.io.IOException;
import java.util.List;

import org.zoxweb.shared.security.AccessException;

/**
 * The API search interface.
 * @author mzebib
 *
 * @param <I>
 * @param <O>
 */
public interface APISearch<I, O>
{
	/**
	 * This method is used to search and return list of outputs based on input arguments.
	 * @param args
	 * @return list of matching the search criteria
	 * @throws IllegalArgumentException
	 * @throws IOException
	 * @throws NullPointerException
	 */
	@SuppressWarnings("unchecked")
	List<O> search(I... args) 
			throws IllegalArgumentException, IOException, NullPointerException, AccessException, APIException;
}
