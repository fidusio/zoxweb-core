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
package org.zoxweb.shared.queue;

import java.io.IOException;
import org.zoxweb.shared.util.NVGenericMap;

/**
 * The queue session interface.
 */
public interface QueueSession
    extends AutoCloseable
{
	
	
	void connect(NVGenericMap config)
			throws NullPointerException, IllegalArgumentException, IOException;
	
	
	
	
	/**
	 * Adds consumer listener.
	 * @param ql
	 */
	void addListener(String tag, QueueListener<QueueEvent<?>> ql);

	/**
	 * Removes consumer listener.
	 * @param ql
	 */
	void removeListener(QueueListener<QueueEvent<?>> ql);
	//void removeListener(String tag);

	/**
	 * Dispatches producer event.
	 * @param qe
	 */
	void queueEvent(QueueEvent<?> qe)
			throws NullPointerException, IllegalArgumentException, IOException;
	
	QueueListener<QueueEvent<?>> [] getAllListeners();
	
	/**
	 * Get the connection configuration
	 * @return
	 */
	NVGenericMap getConfig();

}