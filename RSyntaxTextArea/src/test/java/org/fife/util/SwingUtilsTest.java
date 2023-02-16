package org.fife.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SwingUtilsTest {

	private char[] chars1k;

	@BeforeEach
	void setUp() {
		chars1k = "0123456789".repeat(1000).toCharArray();
	}

	@Test
	void testNeedsTextLayoutDefault() {
		assertFalse(SwingUtils.needsTextLayout(chars1k, 0, 1000));
		assertTrue(SwingUtils.needsTextLayout(chars1k, 0, 1001));
	}

	@Test
	@Disabled("Need to reload the SwingUtils class to make this work")
	void testNeedsTextLayoutModified() {
		System.setProperty(SwingUtils.class.getName()+".textLayoutThreshold", "100");
		assertFalse(SwingUtils.needsTextLayout(chars1k, 0, 100));
		assertTrue(SwingUtils.needsTextLayout(chars1k, 0, 101));
	}
}