package org.zoxweb.shared.filters;


import org.junit.jupiter.api.Test;

public class FilenameFilterTest {


	@Test
	public void filterTest()
	{
		
		
		String toTest[] = {
			"/dir/fname",
			"/dir1/dir2/dir3/fname",
			"dir1/dir2/dir3/fname",
			"/fname",
			"fname"
		};
		for(String str : toTest)
		{
			System.out.println(str + " validated " + FilenameFilter.SINGLETON.validate(str));
		}
	}
}
