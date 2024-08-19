package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AppIDURITest
{
	@Test
	public void testPositive()
	{
		AppIDURI aiu = AppIDURI.parse("///zoxweb.com/zoxweb/batata");
		System.out.println(aiu.getAppIDDAO().toCanonicalID());
		assertTrue("batata".equals(aiu.getRest()[0]));
	}
}
