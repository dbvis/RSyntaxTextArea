package org.fife.ui.rsyntaxtextarea;

import org.junit.jupiter.api.Test;

import java.awt.*;

public class FixedWithTokenViewModelConverterTest {

	@Test
	void testGetListOffset() {
		String text = "0123456789";
		float xOffset = 0;

		// ideal click before character (token is first one the line)
		assertGetListOffset(text, 0, xOffset, 0f);
		assertGetListOffset(text, 5, xOffset, 50f);
		assertGetListOffset(text, 6, xOffset, 60f);


		// ideal click before character (token is first one the line)
		assertGetListOffset(text, 0, xOffset, 0f);
		assertGetListOffset(text, 1, xOffset, 10f);
		assertGetListOffset(text, 2, xOffset, 20f);
		assertGetListOffset(text, 3, xOffset, 30f);
		assertGetListOffset(text, 4, xOffset, 40f);
		assertGetListOffset(text, 5, xOffset, 50f);
		assertGetListOffset(text, 6, xOffset, 60f);
		assertGetListOffset(text, 7, xOffset, 70f);
		assertGetListOffset(text, 8, xOffset, 80f);
		assertGetListOffset(text, 9, xOffset, 90f);

		// with offset (token is not first on the line)
		xOffset = 100;
		assertGetListOffset(text, 0, xOffset, 100f);
		assertGetListOffset(text, 1, xOffset, 110f);
		assertGetListOffset(text, 2, xOffset, 120f);
		assertGetListOffset(text, 3, xOffset, 130f);
		assertGetListOffset(text, 4, xOffset, 140f);
		assertGetListOffset(text, 5, xOffset, 150f);
		assertGetListOffset(text, 6, xOffset, 160f);
		assertGetListOffset(text, 7, xOffset, 170f);
		assertGetListOffset(text, 8, xOffset, 180f);
		assertGetListOffset(text, 9, xOffset, 190f);

		// click on character ("round")
		xOffset = 0;
		assertGetListOffset(text, 0, xOffset, 4.9f);
		assertGetListOffset(text, 1, xOffset, 5f);
		assertGetListOffset(text, 1, xOffset, 5.9f);
		assertGetListOffset(text, 10, xOffset, 95f);
		assertGetListOffset(text, 10, xOffset, 95.9f);
	}

	@Test
	void testGetListOffsetWithTabs() {
		float xOffset = 0;

		{
			// only tabs
			String text = "\t\t\t\t\t";

			assertGetListOffset(text, 0, xOffset, 0f);
			assertGetListOffset(text, 1, xOffset, 40f);
			assertGetListOffset(text, 2, xOffset, 80f);
			assertGetListOffset(text, 3, xOffset, 120f);
			assertGetListOffset(text, 4, xOffset, 160f);

		}
		{
			// regular chars
			String text = "\tA\tB";

			assertGetListOffset(text, 0, xOffset, 0f);
			assertGetListOffset(text, 1, xOffset, 40f);
			assertGetListOffset(text, 2, xOffset, 50f);
			assertGetListOffset(text, 3, xOffset, 80f);
		}
		{
			// variable tab expansion
			String text = "\tA\tAB\tABC\tABCD\tABCDE";

			assertGetListOffset(text, 0, xOffset, 0f);
			assertGetListOffset(text, 1, xOffset, 40f); // A
			assertGetListOffset(text, 2, xOffset, 50f);
			assertGetListOffset(text, 3, xOffset, 80f); // AB
			assertGetListOffset(text, 4, xOffset, 90f);
			assertGetListOffset(text, 5, xOffset, 100f);
			assertGetListOffset(text, 6, xOffset, 120f); // ABC
			assertGetListOffset(text, 7, xOffset, 130f);
			assertGetListOffset(text, 8, xOffset, 140f);
			assertGetListOffset(text, 9, xOffset, 150f);
			assertGetListOffset(text, 10, xOffset, 160f); // ABCD
			assertGetListOffset(text, 11, xOffset, 170f); // B
			assertGetListOffset(text, 12, xOffset, 180f); // C
			assertGetListOffset(text, 13, xOffset, 190f); // D
			assertGetListOffset(text, 14, xOffset, 200f); // \t
			assertGetListOffset(text, 15, xOffset, 240f); // ABCDE
		}
	}

	@Test
	void testGetListOffsetWithWideChars() {
		float xOffset = 0;
		String text = "\tは\tユ\tはユ\tはユ";

		assertGetListOffset(text, 0, xOffset, 0f);
		assertGetListOffset(text, 1, xOffset, 40f);
		assertGetListOffset(text, 2, xOffset, 55f);
		assertGetListOffset(text, 3, xOffset, 80f);
		assertGetListOffset(text, 4, xOffset, 95f);
		assertGetListOffset(text, 5, xOffset, 120f);
		assertGetListOffset(text, 6, xOffset, 135f);
		assertGetListOffset(text, 7, xOffset, 150f);
		assertGetListOffset(text, 8, xOffset, 160f);
		assertGetListOffset(text, 9, xOffset, 175f);
		assertGetListOffset(text, 10, xOffset, 190f);
	}

	private void assertGetListOffset(String text, int expected, float x0, float x) {
		RSyntaxTextArea rsta = new AbstractTokenViewModelConverterTest.MyRSyntaxTextArea(text);
		rsta.setTabSize(4);
		TokenImpl token = new TokenImpl(text.toCharArray(), 0, text.length() - 1, 0, TokenTypes.IDENTIFIER, 0);

		AbstractTokenViewModelConverterTest.MyFontMetrics fm = new AbstractTokenViewModelConverterTest.MyFontMetrics(10f);
		FixedWidthTokenViewModelConverter testee = new FixedWidthTokenViewModelConverter(rsta, fm);
		int actual = testee.getListOffset(token, x0, x);
		AbstractTokenViewModelConverterTest.assertListOffsetEquals(text, x, expected, actual);
	}

}