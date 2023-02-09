package org.fife.ui.rsyntaxtextarea;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.swing.text.TabExpander;

import static org.fife.ui.rsyntaxtextarea.AbstractTokenViewModelConverterTest.assertListOffsetEquals;

public class BufferedTokenViewModelConverterTest {

	@Test
	void testGetListOffset() {
		doTestGetListOffset(-1);
	}

	@Test
	@Disabled("Known bug - not yet fully implemented")
	void testGetListOffsetChunks() {
		doTestGetListOffset(5);
	}

	private static float doTestGetListOffset(int chunkSize) {
		String text = "0123456789";
		float xOffset = 0;

		// ideal click before character (token is first one the line)
		assertGetListOffset(text, 0, xOffset, 0f, chunkSize);
		assertGetListOffset(text, 5, xOffset, 50f, chunkSize);
		assertGetListOffset(text, 6, xOffset, 60f, chunkSize);

		// ideal click before character (token is first one the line)
		assertGetListOffset(text, 0, xOffset, 0f, chunkSize);
		assertGetListOffset(text, 1, xOffset, 10f, chunkSize);
		assertGetListOffset(text, 2, xOffset, 20f, chunkSize);
		assertGetListOffset(text, 3, xOffset, 30f, chunkSize);
		assertGetListOffset(text, 4, xOffset, 40f, chunkSize);
		assertGetListOffset(text, 5, xOffset, 50f, chunkSize);
		assertGetListOffset(text, 6, xOffset, 60f, chunkSize);
		assertGetListOffset(text, 7, xOffset, 70f, chunkSize);
		assertGetListOffset(text, 8, xOffset, 80f, chunkSize);
		assertGetListOffset(text, 9, xOffset, 90f, chunkSize);

		// with offset (token is not first on the line)
		xOffset = 100;
		assertGetListOffset(text, 0, xOffset, 100f, chunkSize);
		assertGetListOffset(text, 1, xOffset, 110f, chunkSize);
		assertGetListOffset(text, 2, xOffset, 120f, chunkSize);
		assertGetListOffset(text, 3, xOffset, 130f, chunkSize);
		assertGetListOffset(text, 4, xOffset, 140f, chunkSize);
		assertGetListOffset(text, 5, xOffset, 150f, chunkSize);
		assertGetListOffset(text, 6, xOffset, 160f, chunkSize);
		assertGetListOffset(text, 7, xOffset, 170f, chunkSize);
		assertGetListOffset(text, 8, xOffset, 180f, chunkSize);
		assertGetListOffset(text, 9, xOffset, 190f, chunkSize);

		// click on character ("truncate")
		xOffset = 0;
		assertGetListOffset(text, 0, xOffset, 5f, chunkSize);
		assertGetListOffset(text, 0, xOffset, 5.9f, chunkSize);
		assertGetListOffset(text, 9, xOffset, 95f, chunkSize);
		assertGetListOffset(text, 9, xOffset, 95.9f, chunkSize);
		return xOffset;
	}

	private static void assertGetListOffset(String text, int expected, float x0, float x, int chunkSize) {
		TabExpander te = new AbstractTokenViewModelConverterTest.MyTabExpander(20);
		RSyntaxTextArea rsta = new AbstractTokenViewModelConverterTest.MyRSyntaxTextArea(text);

		BufferedTokenViewModelConverter testee = new BufferedTokenViewModelConverter(rsta, te);
		if (chunkSize>0) {
			testee.listOffsetChunkSize = chunkSize;
		}

		TokenImpl token = new TokenImpl(text.toCharArray(), 0, text.length() - 1, 0, TokenTypes.IDENTIFIER, 0);
		int actual = testee.getListOffset(token, x0, x);
		assertListOffsetEquals(text, x, expected, actual, "ChunkSize="+chunkSize);
	}
}
