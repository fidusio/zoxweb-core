package org.zoxweb.server.util;

import java.io.IOException;
import java.util.logging.Logger;

import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.data.RuntimeResultDAO;
import org.zoxweb.shared.util.SharedUtil;
import org.zoxweb.shared.util.TaskListener;
import org.zoxweb.shared.util.Const.TimeInMillis;
import org.zoxweb.shared.util.SharedStringUtil;

public class CronCommand 
implements Runnable
{

  private static final transient Logger log = Logger.getLogger(CronCommand.class.getName());
  public static final TaskListener<CronCommand, String> NOOP_TASK = new TaskListener<CronCommand, String>() 
  {

    @Override
    public void started(CronCommand t) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void terminated(CronCommand t) {
        // TODO Auto-generated method stub
    }

    @Override
    public void executionResult(int status, long counter, long timestamp,  String result) {
        // TODO Auto-generated method stub
    }
    
  }; 
  
		
	private int repeatCount;
	private long delay;
	private String command;
	private long executionCounter = 0;
	private volatile TaskListener<CronCommand, String> taskListener;
	
	
	
	public CronCommand(TaskListener<CronCommand, String> taskListener, int repeatCount, long delay, String command)
	{
		SharedUtil.checkIfNulls("Command cannot be null", SharedStringUtil.trimOrNull(command), taskListener);
		if (repeatCount < 0 || delay < 0)
		{
			throw new IllegalArgumentException("Invalid dealy or repeatCount value");
		}
			
		this.repeatCount = repeatCount;
		this.delay = delay;
		this.command = command;
		
		this.taskListener = taskListener;
		TaskUtil.defaultTaskScheduler().queue(delay, this);
		
		taskListener.started(this);
	}
	

	@Override
	public void run() 
	{
		// TODO Auto-generated method stub
		executionCounter++;
		try 
		{
			log.info("Executing command:" + command);
			RuntimeResultDAO rr = RuntimeUtil.runAndFinish(command);
			taskListener.executionResult(rr.getExitCode(), executionCounter, System.currentTimeMillis(), rr.getOutputData());
			
		} 
		catch (InterruptedException | IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 
		if (repeatCount == executionCounter)
		{
			taskListener.terminated(this);
			return;
		}
		//Appointment a = new AppointmentDefault(delay);
		TaskUtil.defaultTaskScheduler().queue(delay, this);
	}
		
	
	
	
	public static void main(String ...args)
	{
		try
		{
		
			int repeatCount = 0;
			long delay = 0;
			String command = null;
			
			for (int i=0; i < args.length; i++)
			{
				String arg = args[i];
				switch(arg)
				{
				case "-r":
				case "-R":
					try
					{
						repeatCount = Integer.parseInt(args[i+1]);
						i++;
					}
					catch(NumberFormatException e)
					{
						repeatCount = 0;
					}
					break;
				case "-d":
				case "-D":
					delay = TimeInMillis.toMillis(args[++i]);
					break;
				default:
					command = arg;
				}
				if (command != null)
					break;
			}
			
			new CronCommand(new TaskListener<CronCommand, String>() {

				@Override
				public void started(CronCommand t) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void terminated(CronCommand t) {
					// TODO Auto-generated method stub
					System.exit(0);
				}

				@Override
				public void executionResult(int status, long counter, long timestamp,  String result) {
					// TODO Auto-generated method stub
					System.out.println(result);
				}
				
			},repeatCount, delay, command);
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.err.println("Usage: [-r counter] [-d delay_time] command");
		}
	}
}
