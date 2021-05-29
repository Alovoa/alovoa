package com.nonononoki.alovoa;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

class ToolsTest {

	@Test
	void test() throws Exception {
		int dist = (int)Math.round(Tools.calcDistance(0, 0, 0, 0));
		Assert.assertEquals(0, dist);
		
		int dist2 = (int)Math.round(Tools.calcDistance(0.45, 0, 0, 0));
		Assert.assertEquals(49, dist2);
		
		int dist3 = (int)Math.round(Tools.calcDistance(0.46, 0, 0, 0));
		Assert.assertEquals(50, dist3);
	}
}
