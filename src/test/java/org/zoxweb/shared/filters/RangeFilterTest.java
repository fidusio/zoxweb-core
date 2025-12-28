package org.zoxweb.shared.filters;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RangeFilterTest {
	
	private IntRangeFilter intOneToHundred = new IntRangeFilter(1, true, 100, false);
    private FloatRangeFilter floatOneToHundred = new FloatRangeFilter(1, false, 100, false);
    private DoubleRangeFilter doubleOneToHundred = new DoubleRangeFilter(1, true, 100, true);

	@Test
	public void testIntFilterIsValidForValidValue()
	{
		int values [] = {
				1, 99, 50, 35
		};

		for (int v : values) {
            assertTrue(intOneToHundred.isValid(v));
        }
	}

    @Test
    public void testIntFilterIsValidForInvalidValue()
    {
        int values [] = {
                 200, 100, 0, 1000, -1
        };

        for (int v : values) {
            assertFalse(intOneToHundred.isValid(v));
        }
    }

    @Test
	public void testFloatFilterIsValidForValidValue()
	{
		float values [] = {
				2, (float) 99.99, 50, 35
        };

		for (float v : values) {
            assertTrue(floatOneToHundred.isValid(v));
        }
	}

    @Test
    public void testFloatFilterIsValidForInvalidValue()
    {
        float values [] = {
                200, 100, 0, 1
        };

        for (float v : values) {
            assertFalse(floatOneToHundred.isValid(v));
        }
    }
	
	@Test
	public void testDoubleFilterIsValidForValidValue()
	{
		double values [] = {
		        1, 99.99, 100, 50, 35
        };

		for (double v : values) {
		    assertTrue(doubleOneToHundred.isValid(v));
        }
	}

    @Test
    public void testDoubleFilterIsValidForInvalidValue()
    {
        double values [] = {
                -1, 0, 200
        };

        for (double v : values) {
            assertFalse(doubleOneToHundred.isValid(v));
        }
    }
}
