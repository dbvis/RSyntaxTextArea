package org.fife.ui.rsyntaxtextarea;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.swing.text.TabExpander;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;

import static org.fife.ui.rsyntaxtextarea.TokenViewModelConverterTest.assertListOffsetEquals;

public class BufferedTokenViewModelConverterTest {

	@Test
	@Disabled("Not yet implemented (fails for some reason)")
	void testGetListOffsetProportionalChunks() {
		String text = "0123456789";
		float xOffset = 0;

		// ideal click before character (token is first one the line)
		assertGetListOffsetProportionalChunks(text, 0, xOffset, 0f);
		assertGetListOffsetProportionalChunks(text, 5, xOffset, 50f);
		assertGetListOffsetProportionalChunks(text, 6, xOffset, 60f);


		// ideal click before character (token is first one the line)
		assertGetListOffsetProportionalChunks(text, 0, xOffset, 0f);
		assertGetListOffsetProportionalChunks(text, 1, xOffset, 10f);
		assertGetListOffsetProportionalChunks(text, 2, xOffset, 20f);
		assertGetListOffsetProportionalChunks(text, 3, xOffset, 30f);
		assertGetListOffsetProportionalChunks(text, 4, xOffset, 40f);
		assertGetListOffsetProportionalChunks(text, 5, xOffset, 50f);
		assertGetListOffsetProportionalChunks(text, 6, xOffset, 60f);
		assertGetListOffsetProportionalChunks(text, 7, xOffset, 70f);
		assertGetListOffsetProportionalChunks(text, 8, xOffset, 80f);
		assertGetListOffsetProportionalChunks(text, 9, xOffset, 90f);

		// with offset (token is not first on the line)
		xOffset = 100;
		assertGetListOffsetProportionalChunks(text, 0, xOffset, 100f);
		assertGetListOffsetProportionalChunks(text, 1, xOffset, 110f);
		assertGetListOffsetProportionalChunks(text, 2, xOffset, 120f);
		assertGetListOffsetProportionalChunks(text, 3, xOffset, 130f);
		assertGetListOffsetProportionalChunks(text, 4, xOffset, 140f);
		assertGetListOffsetProportionalChunks(text, 5, xOffset, 150f);
		assertGetListOffsetProportionalChunks(text, 6, xOffset, 160f);
		assertGetListOffsetProportionalChunks(text, 7, xOffset, 170f);
		assertGetListOffsetProportionalChunks(text, 8, xOffset, 180f);
		assertGetListOffsetProportionalChunks(text, 9, xOffset, 190f);

		// click on character ("truncate")
		xOffset = 0;
		assertGetListOffsetProportionalChunks(text, 0, xOffset, 5f);
		assertGetListOffsetProportionalChunks(text, 0, xOffset, 5.9f);
		assertGetListOffsetProportionalChunks(text, 9, xOffset, 95f);
		assertGetListOffsetProportionalChunks(text, 9, xOffset, 95.9f);
	}

	private static void assertGetListOffsetProportionalChunks(String text, int expected, float x0, float x) {
		TabExpander te = new TokenViewModelConverterTest.MyTabExpander(20);
		RSyntaxTextArea rsta = new TokenViewModelConverterTest.MyRSyntaxTextArea(text);

		BufferedTokenViewModelConverter testee = new BufferedTokenViewModelConverter(rsta, te);
		testee.listOffsetChunkSize = 5;

		TokenImpl token = new TokenImpl(text.toCharArray(), 0, text.length() - 1, 0, TokenTypes.IDENTIFIER, 0);
		int actual = testee.getListOffset(token, x0, x);
		assertListOffsetEquals(text, x, expected, actual);
	}
}
