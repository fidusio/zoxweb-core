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
package org.zoxweb;


import org.zoxweb.server.util.RuntimeUtil;
import org.zoxweb.server.util.ThresholdQueue;
import org.zoxweb.shared.data.VMInfoDAO;
import org.zoxweb.shared.util.ArrayQueue;
import org.zoxweb.shared.util.SharedUtil;
import org.zoxweb.shared.util.SimpleQueueInterface;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingQueue;

public class QueueTest {

//	static void add(Map<Long, String> map, long k, String v)
//	{
//		while(map.get(k) != null)
//		{
//			k++;
//		}
//		map.put(k, v);
//	}


  public static void aQueueTest(int cap) {

    ArrayQueue<Integer> aq = new ArrayQueue<Integer>(cap);
    System.out.println("Empty:" + aq);
    int index = 0;
    for (; index < aq.capacity(); index++) {
      aq.queue(Integer.valueOf(index));
      System.out.println(aq);
    }

    for (int i = 0; i < 3; i++) {

      System.out.println(aq.dequeue() + ":" + aq);
    }

    aq.queue(Integer.valueOf(index));
    System.out.println(aq);
    int size = aq.size();
    for (int i = 0; i < size; i++) {

      System.out.println(aq.dequeue() + ":" + aq);
    }

    index = 0;
    for (; index < aq.capacity() + 1; index++) {
      boolean result = aq.queue(Integer.valueOf(index));
      System.out.println(aq + "," + result);
    }


  }


  public static void main(String[] args) {
    aQueueTest(10);
    int limit = 10000;
    SimpleQueueInterface<Object> uQueue = new ThresholdQueue<Object>(limit, limit * 2);
    ConcurrentLinkedQueue<Object> clQueue = new ConcurrentLinkedQueue<Object>();
    LinkedBlockingQueue<Object> lbQueue = new LinkedBlockingQueue<Object>();
    ArrayBlockingQueue<Object> abQueue = new ArrayBlockingQueue<Object>(limit);
    ArrayQueue<Object> aq = new ArrayQueue<Object>(limit);

    VMInfoDAO startVMID = RuntimeUtil.vmSnapshot();
    Object o = new Object();

    for (int j = 0; j < 10; j++) {
      ConcurrentSkipListMap<Long, String> results = new ConcurrentSkipListMap<Long, String>();
      System.out.println("\nTest run " + j);
      for (int i = 0; i < limit; i++) {
        uQueue.queue(new Object());
        clQueue.add(new Object());
        lbQueue.add(new Object());
        abQueue.add(new Object());
        aq.queue(new Object());

      }

      long ts = System.nanoTime();

      while (uQueue.dequeue() != null) {

      }

      ts = System.nanoTime() - ts;
      String message = ts + " nanos SimpleQueue took to dequeue " + limit + ":" + uQueue.size();
      SharedUtil.putUnique(results, ts, message);
      //System.out.println( ts + " nanos SimpleQueue took  sec to dequeue " + limit + ":" + uQueue.size());

      ts = System.nanoTime();

      while (clQueue.poll() != null) {

      }

      ts = System.nanoTime() - ts;
      message = ts + " nanos ConcurrentLinkedQueue took to dequeue " + limit + ":" + clQueue.size();
      SharedUtil.putUnique(results, ts, message);
      //System.out.println( ts + " nanos ConcurrentLinkedQueue took  sec to dequeue " + limit + ":" + clQueue.size());

      ts = System.nanoTime();

      while (lbQueue.poll() != null) {

      }

      ts = System.nanoTime() - ts;
      message = ts + " nanos LinkedBlockingQueue took to dequeue " + limit + ":" + lbQueue.size();
      SharedUtil.putUnique(results, ts, message);
      //System.out.println( ts + " nanos LinkedBlockingQueue took  sec to dequeue " + limit + ":" + lbQueue.size());

      ts = System.nanoTime();

      while (abQueue.poll() != null) {

      }

      ts = System.nanoTime() - ts;
      message = ts + " nanos ArrayBlockingQueue took to dequeue " + limit + ":" + abQueue.size();
      SharedUtil.putUnique(results, ts, message);

      ts = System.nanoTime();

      while (aq.dequeue() != null) {

      }

      ts = System.nanoTime() - ts;
      message = ts + " nanos ArrayQueue took to dequeue " + limit + ":" + abQueue.size();
      SharedUtil.putUnique(results, ts, message);

      //System.out.println( ts + " nanos ArrayBlockingQueue took  sec to dequeue " + limit + ":" + abQueue.size());

      for (String msg : results.values()) {
        System.out.println(msg);
      }
    }

    System.out.println();
    System.out.println(startVMID);
    System.out.println(RuntimeUtil.vmSnapshot());

    uQueue.queue(new Object());
    uQueue.queue(new Object());
    uQueue.queue(o);
    uQueue.queue(new Object());
    uQueue.queue(new Object());

    System.out.println(uQueue.size() + ":" + uQueue.contains(new Object()));
    System.out.println(uQueue.size() + ":" + uQueue.contains(o));
    System.out.println(uQueue.size() + ":" + uQueue.contains(new Object()));
    System.out.println(uQueue.size() + ":" + uQueue.contains(o));
    System.out.println(uQueue.size() + ":" + uQueue.contains(new Object()));
    System.out.println(uQueue.size() + ":" + uQueue.contains(o));

    int size = uQueue.size();
    int counter = 0;

    while (uQueue.dequeue() != null) {
      counter++;
    }

    System.out.println(size + "," + counter + "," + uQueue.size());
    System.out.println(uQueue.size() + ":" + uQueue.contains(o));
    uQueue.queue(o);
    System.out.println(uQueue.size() + ":" + uQueue.contains(o));
    uQueue.dequeue();
    System.out.println(uQueue.size() + ":" + uQueue.contains(o));
    uQueue.queue(new Object());
    uQueue.queue(new Object());
    uQueue.queue(o);
    System.out.println(uQueue.size() + ":" + uQueue.contains(o));

  }

}