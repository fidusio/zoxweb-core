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
package org.zoxweb.shared.data.events;


import java.util.EventListener;
import java.util.HashSet;
import java.util.Set;

public abstract class EventListenerManager<E extends BaseEventObject<?>, L extends EventHandlerListener<E>>
implements AutoCloseable
{
	protected Set<L> set = new HashSet<L>();

	public synchronized void addEventListener(L listener)
	{
		if (listener != null)
        {
            set.add(listener);
        }
	}

	public synchronized void removeEventListener(L listener)
	{
		if (listener != null)
        {
            set.remove(listener);
        }
	}
	
	@SuppressWarnings("unchecked")
	public synchronized L[] getAllListeners()
	{
		return (L[]) set.toArray(new EventHandlerListener[0]);
	}
	
	public abstract void dispatch(E event, boolean async);
//	{
//		EventListener[] all = getAllListeners();
//
//		for (EventListener el : all)
//		{
//			((EventHandlerListener)el).handleEvent(event);
//		}
//	}
	
	public abstract void close();

}