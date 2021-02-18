package com.nonononoki.alovoa;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

class ToolsTests {

	@Test
	void test() throws Exception {
		int dist = (int)Math.round(Tools.calcDistance(0, 0, 0, 0));
		Assert.assertEquals(dist, 0);
		
		int dist2 = (int)Math.round(Tools.calcDistance(0.45, 0, 0, 0));
		Assert.assertEquals(dist2, 49);
		
		int dist3 = (int)Math.round(Tools.calcDistance(0.46, 0, 0, 0));
		Assert.assertEquals(dist3, 50);
	}
}
