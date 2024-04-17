package org.fife.ui.rsyntaxtextarea;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.text.TabExpander;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

public class AbstractTokenViewModelConverterTest {

	@Test
	void testGetListOffset() {
		String text = "0123456789";
		float xOffset = 0;

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

		// click on character ("truncate")
		xOffset = 0;
		assertGetListOffset(text, 0, xOffset, 5f);
		assertGetListOffset(text, 0, xOffset, 5.9f);
		assertGetListOffset(text, 9, xOffset, 95f);
		assertGetListOffset(text, 9, xOffset, 95.9f);

		// click close to end of character ("round"") - NOT IMPLEMENTED
		xOffset = 0;
//		assertGetListOffset(1, 5.01f);
//		assertGetListOffset(-1, 95.01f);
	}

	private static void assertGetListOffset(String text, int expected, float x0, float x) {
		int tokenStartOffset = 0;
		int tokenEndOffset = text.length();
		FontMetrics fm = new MyFontMetrics(10f);

		int actual = AbstractTokenViewModelConverter.getListOffset(fm, text.toCharArray(), tokenStartOffset, tokenStartOffset, tokenEndOffset, x0, x);
		assertListOffsetEquals(text, x, expected, actual);
	}

	static void assertListOffsetEquals(String text, float x, int expectedOffset, int actualOffset, Object... extra) {
		String expectedCharacter = expectedOffset < 0 || expectedOffset >= text.length()
			? null
			: escape(text.substring(expectedOffset, expectedOffset + 1));
		String actualCharacter = actualOffset < 0 || actualOffset >= text.length()
			? null
			: escape(text.substring(actualOffset, actualOffset + 1));

		String truncatedText = text.length() < 40
			? text
			: String.format("'%s...%s'[%,d]", text.substring(0, 15), text.substring(text.length() - 15), text.length());
		String moreInfo = extra.length<1 ? "" : Arrays.toString(extra);
		String msg = String.format("x=%.3f - Expected: %,d ('%s') | Actual: %,d ('%s') | Text: '%s' %s",
			x, expectedOffset, expectedCharacter, actualOffset, actualCharacter, escape(truncatedText), moreInfo);
		Assertions.assertEquals(expectedOffset, actualOffset, msg);
	}

	private static String escape(String s) {
		return s.replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
	}


	static class MyFontMetrics extends FontMetrics {
		private final float averageCharacterWidth;
		private final boolean proportionalFont;

		protected MyFontMetrics(float fixedCharacterWidth) {
			this(fixedCharacterWidth, false);
		}

		protected MyFontMetrics(float averageCharacterWidth, boolean proportionalFont) {
			super(null);
			this.averageCharacterWidth = averageCharacterWidth;
			this.proportionalFont = proportionalFont;
			this.font = new MyFont(10);
		}

		@Override
		public Font getFont() {
			return font;
		}

		@Override
		public int charWidth(char ch) {
			float w = proportionalFont ? randomize(averageCharacterWidth) : averageCharacterWidth;
			return (int) (ch > 256 ? w * 1.5 : w);
		}

		@Override
		public int charsWidth(char[] data, int off, int len) {
			int charsWidth = 0;
			for (int i = off; i < off + len; i++) {
				char c = data[i];
				charsWidth += charWidth(c);
			}
			return charsWidth;
		}

		private int randomize(float w) {
			return (int) (w + 1 / Math.random());
		}

		private class MyFont extends Font {
			public MyFont(int size) {
				super("TEST", PLAIN, size);
			}

			@Override
			public Rectangle2D getStringBounds(char[] chars, int beginIndex, int limit, FontRenderContext frc) {
				int x = 0;
				int y = size;
				int width = charsWidth(chars, beginIndex, limit - beginIndex);
				int height = size;
				return new Rectangle2D.Float(x, y, width, height);
			}
		}
	}

	static class MyTabExpander implements TabExpander {
		private final float relativeDistanceToNextTabStop;

		public MyTabExpander(float fixedDistanceToNextTabStop) {
			this.relativeDistanceToNextTabStop = fixedDistanceToNextTabStop;
		}

		@Override
		public float nextTabStop(float x, int tabOffset) {
			return x + relativeDistanceToNextTabStop;
		}
	}

	static class MyRSyntaxTextArea extends RSyntaxTextArea {
		public MyRSyntaxTextArea(String text) {
			super(text);
		}

		@Override
		public FontMetrics getFontMetricsForTokenType(int type) {
			return new MyFontMetrics(10);
		}
	}

}
