package org.zoxweb.server.task;

import java.util.UUID;
import java.util.concurrent.Executor;

import org.zoxweb.shared.util.GetDescription;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.LifeCycleMonitor;
import org.zoxweb.shared.util.SharedUtil;

/**
 * 
 * @author mnael
 *
 * @param <E> Executor
 */
public class ExecutorHolder<E
		extends Executor>
		implements Executor, GetName, GetDescription, AutoCloseable {

	protected final E es;
	private final String name;
	private final String description;
	protected final LifeCycleMonitor<ExecutorHolder<?>> lcm;
	private volatile boolean closed = false;

	public ExecutorHolder(Executor es, LifeCycleMonitor<ExecutorHolder<?>> lcm)
			throws NullPointerException, IllegalArgumentException {
		this(es, lcm, null, null);
	}

	public ExecutorHolder(Executor es, LifeCycleMonitor<ExecutorHolder<?>> lcm, String name)
			throws NullPointerException, IllegalArgumentException {
		this(es, lcm, name, null);
	}

	/**
	 * 
	 * @param es Executor to be registered, mandatory
	 * @param lcm LifeCycleMonitor that will monitor creation and termination, mandatory
	 * @param name of the Executor if null an automatic UUID name will be generated
	 * @param description optional
	 * @throws NullPointerException if es or lcm null
	 * @throws IllegalArgumentException if name of executor already exist, Executor instance of ExecutorHolder
	 */
	@SuppressWarnings("unchecked")
	public ExecutorHolder(Executor es, LifeCycleMonitor<ExecutorHolder<?>> lcm, String name, String description)
		throws NullPointerException, IllegalArgumentException {

		SharedUtil.checkIfNulls("Executor or LifeCycleMonitor null", es, lcm);
		this.es = (E) es;
		this.name = name != null ? name : UUID.randomUUID().toString();
		this.description = description;
		this.lcm = lcm;

		if (!lcm.created(this)) {
			throw new IllegalArgumentException(name + " already registered.");
		}
		
	}

	@Override
	public void execute(Runnable command) {
		es.execute(command);
	}
	
	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void close() {
		if (!closed) {
			synchronized(this) {
				if (!closed) {
					if (es instanceof AutoCloseable) {
						try {
							((AutoCloseable)es).close();
						} catch (Exception e) {

						}
					}
					lcm.terminated(this);
				}
				
				closed = true;
			}
		}
	}

	@Override
	public String toString() {
		return getName() + ", " + es;
	}
}
