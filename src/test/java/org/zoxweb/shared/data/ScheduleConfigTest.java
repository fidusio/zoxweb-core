package org.zoxweb.shared.data;

import java.util.Arrays;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.util.GSONUtil;

public class ScheduleConfigTest {
	
	public static void main(String ...args)
	{
		try
		{
			int index = 0;
			String filename = args[index++];
			ScheduleConfigDAO config = GSONUtil.fromJSON(IOUtil.inputStreamToString(filename), ScheduleConfigDAO.class);
			System.out.println(config);
			String json = GSONUtil.toJSON(config, true, false, false);
			System.out.println(json);
			config = GSONUtil.fromJSON(json, ScheduleConfigDAO.class);
			System.out.println(config.getSchedules());
			System.out.println(config.getSchedules().getClass());
			System.out.println(Arrays.toString(config.getOffCommands()) + "," +Arrays.toString(config.getOnCommands()));
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

}
