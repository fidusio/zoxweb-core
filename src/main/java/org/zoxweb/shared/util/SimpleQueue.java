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
 * The SimpleQueue is a FIFO queue class. It is used to store none null Objects only.
 * <br>
 * The following code illustrate how to use this class: <xmp> ... SimpleQueue q = new
 * SimpleQueue(); // to add an object to the queue q.queue( element); ... // to
 * remove an object from the queue Object obj = q.dequeue() </xmp>
 * 
 */
public class SimpleQueue<O>
    implements SimpleQueueInterface<O> {
	
	private int size;
	private QueueNode<O> head;
	private long totalQueued = 0;
	private boolean equalityEnabled = true;

	
	/**
	 * Create a empty queue.
	 */
	public SimpleQueue()
	{
		head = new QueueNode<O>(null, null, null);
		// make the last node point to the head node
		size = 0;
	}

	public SimpleQueue(boolean equalityEnabled)
	{
		this();
		this.equalityEnabled = equalityEnabled;
	}
		

	/**
	 * Clears the queue. It removes all the elements from it.
	 */
	public synchronized void clear()
	{
		while (!(isEmpty()))
		{
			dequeue();
		}
	}

	/**
	 * This QueuceNode is a linked list class.
	 */
	 static class QueueNode<O>
	 {
		QueueNode(O o, QueueNode<O> p, QueueNode<O> n)
		{
			obj = o;
			previous = p;
			next = n;
		}

		O obj;

		QueueNode<O> next;

		QueueNode<O> previous;
	}

	/**
	 * Returns the size of the queue.
	 * @return the size of the queue.
	 */
	public int size() 
	{
		return size;
	}

	/**
	 * This will queue an object, the object can be null.
	 * 
	 * @param toQueue the object to add in the queue
	 * 
	 */
	public synchronized boolean queue(O toQueue)
	{

		/**
		 * The implementation of this queue is based on the algorithm of a
		 * circular double link list.
		 * 
		 */
		if (toQueue == null)
		{
			throw new IllegalArgumentException("Can't queue a null object");
		}

		QueueNode<O> newNode = new QueueNode<O>(toQueue, null, null);

		// empty queue
		if (head.next == null && head.previous == null)
		{
			// we have
			head.next = newNode;
			head.previous = newNode;
			newNode.next = head;
			newNode.previous = head;
		}
		else
		{
			newNode.next = head.next;
			newNode.previous = head;
			head.next.previous = newNode;
			head.next = newNode;
		}

		// increment the queue size
		size++;
		totalQueued++;
		return true;
	} // end queue()

	/**
	 * Dequeue an object, if the queue is empty it will return null.
	 * @return first object or null 
	 */
	public synchronized O dequeue()
	{
		O retval = null;

		// if the queue is not empty
		if (head.next != null && head.previous != null)
		{
			
			QueueNode<O> toRemove = head.previous;
			retval = toRemove.obj;

			if (toRemove.previous == head)
			{
				// this the last element in the queue
				head.next = null;
				head.previous = null;
			} 
			else 
			{
				// remove the node from the
				head.previous = toRemove.previous;
				head.previous.next = head;	
			}
			// decrement the queue size;
			toRemove.next = null;
			toRemove.previous = null;
			toRemove.obj = null;
			size--;
			
		}

		return retval;

	} // end dequeue()

	/**
	 * Checks if queue contains the given object.
	 * @param o the object to check
	 * @return true if found, false if not
	 */
	public synchronized boolean contains(O o)
	{
		QueueNode<O> temp = head;

		while (temp.next != null)
		{
			if (temp.obj == o)
			{
				return true;
			}
			else if (equalityEnabled && temp.obj != null && temp.obj.equals(o))
			{
				return true;
			}

			temp = temp.next;

			if (temp == head)
			{
				break;
			}
		}
		
		return false;
	}

	/**
	 * Check if the queue is empty.
	 * @return true if empty. false otherwise.
	 */
	public synchronized boolean isEmpty()
	{
		return (size == 0);
	}
	
	/**
	 * Returns the total number of objects queued.
	 * @return the total number of object queued
	 */
	public long totalQueued() 
	{
		return totalQueued;
	}

	/**
	 *
	 * @return -1 unlimited
	 */
	public int capacity()
	{
		return -1;
	}
	
	/**
	 * Returns the total number of objects dequeued.
	 * @return the total number of object dequeued
	 */
	public synchronized long totalDequeued()
	{
		return totalQueued - size;
	}

}