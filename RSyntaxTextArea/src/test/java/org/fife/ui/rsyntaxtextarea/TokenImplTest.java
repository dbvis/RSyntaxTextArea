/*
 * 12/10/2016
 *
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.text.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.StringJoiner;

import static java.awt.Font.PLAIN;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import javax.swing.text.StringContent;
import java.awt.*;


/**
 * Unit tests for the {@link TokenImpl} class.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class TokenImplTest {


	@Test
	void testGetHTMLRepresentation_happyPath() {

		RSyntaxTextArea textArea = new RSyntaxTextArea();

		char[] ch = "for".toCharArray();
		TokenImpl token = new TokenImpl(ch, 0, 2, 0, TokenTypes.IDENTIFIER, 0);

		// Don't bother checking font and other styles since it may be host-specific
		String actual = token.getHTMLRepresentation(textArea);
		Assertions.assertTrue(actual.startsWith("<font"));
		Assertions.assertTrue(actual.endsWith(">for</font>"));

	}


	@Test
	void testGetHTMLRepresentation_problemChars() {

		RSyntaxTextArea textArea = new RSyntaxTextArea();

		char[] ch = " &\t<>'\"/".toCharArray();
		TokenImpl token = new TokenImpl(ch, 0, ch.length - 1, 0, TokenTypes.IDENTIFIER, 0);

		// Don't bother checking font and other styles since it may be host-specific
		String actual = token.getHTMLRepresentation(textArea);
		System.out.println(actual);
		Assertions.assertTrue(actual.startsWith("<font"));
		Assertions.assertTrue(actual.endsWith("> &amp;&#09;&lt;&gt;&#39;&#34;&#47;</font>"));

	}


	@Test
	void testIs_1arg_charArray_differentLengths() {

		char[] ch = "for".toCharArray();
		TokenImpl token = new TokenImpl(ch, 0, 2, 0, TokenTypes.IDENTIFIER, 0);

		Assertions.assertFalse(token.is("while".toCharArray()));
	}


	@Test
	void testIs_1arg_charArray_sameLengthsButDifferent() {

		char[] ch = "for".toCharArray();
		TokenImpl token = new TokenImpl(ch, 0, 2, 0, TokenTypes.IDENTIFIER, 0);

		Assertions.assertFalse(token.is("foo".toCharArray()));
	}


	@Test
	void testIs_1arg_charArray_same() {

		char[] ch = "for".toCharArray();
		TokenImpl token = new TokenImpl(ch, 0, 2, 0, TokenTypes.IDENTIFIER, 0);

		Assertions.assertTrue(token.is("for".toCharArray()));
	}


	@Test
	void testIs_2arg_charArray_differentLengths() {

		char[] ch = "for".toCharArray();
		TokenImpl token = new TokenImpl(ch, 0, 2, 0, TokenTypes.IDENTIFIER, 0);

		Assertions.assertFalse(token.is(TokenTypes.IDENTIFIER, "while".toCharArray()));
	}


	@Test
	void testIs_2arg_charArray_sameLengthsButDifferent() {

		char[] ch = "for".toCharArray();
		TokenImpl token = new TokenImpl(ch, 0, 2, 0, TokenTypes.IDENTIFIER, 0);

		Assertions.assertFalse(token.is(TokenTypes.IDENTIFIER, "foo".toCharArray()));
	}


	@Test
	void testIs_2arg_charArray_sameLexemeButDifferentType() {

		char[] ch = "for".toCharArray();
		TokenImpl token = new TokenImpl(ch, 0, 2, 0, TokenTypes.IDENTIFIER, 0);

		Assertions.assertFalse(token.is(TokenTypes.RESERVED_WORD, "for".toCharArray()));
	}


	@Test
	void testIs_2arg_charArray_same() {

		char[] ch = "for".toCharArray();
		TokenImpl token = new TokenImpl(ch, 0, 2, 0, TokenTypes.IDENTIFIER, 0);

		Assertions.assertTrue(token.is(token.getType(), "for".toCharArray()));
	}


	@Test
	void testIs_2arg_string_differentLengths() {

		char[] ch = "for".toCharArray();
		TokenImpl token = new TokenImpl(ch, 0, 2, 0, TokenTypes.IDENTIFIER, 0);

		Assertions.assertFalse(token.is(TokenTypes.IDENTIFIER, "while"));
	}


	@Test
	void testIs_2arg_string_sameLengthsButDifferent() {

		char[] ch = "for".toCharArray();
		TokenImpl token = new TokenImpl(ch, 0, 2, 0, TokenTypes.IDENTIFIER, 0);

		Assertions.assertFalse(token.is(TokenTypes.IDENTIFIER, "foo"));
	}


	@Test
	void testIs_2arg_string_sameLexemeButDifferentType() {

		char[] ch = "for".toCharArray();
		TokenImpl token = new TokenImpl(ch, 0, 2, 0, TokenTypes.IDENTIFIER, 0);

		Assertions.assertFalse(token.is(TokenTypes.RESERVED_WORD, "for"));
	}


	@Test
	void testIs_2arg_string_same() {

		char[] ch = "for".toCharArray();
		TokenImpl token = new TokenImpl(ch, 0, 2, 0, TokenTypes.IDENTIFIER, 0);

		Assertions.assertTrue(token.is(token.getType(), "for"));
	}


	@Test
	void testIsComment_true() {

		char[] ch = "for".toCharArray();

		TokenImpl token = new TokenImpl(ch, 0, 2, 0, TokenTypes.COMMENT_EOL, 0);
		Assertions.assertTrue(token.isComment());

		token = new TokenImpl(ch, 0, 2, 0, TokenTypes.COMMENT_MULTILINE, 0);
		Assertions.assertTrue(token.isComment());

		token = new TokenImpl(ch, 0, 2, 0, TokenTypes.COMMENT_DOCUMENTATION, 0);
		Assertions.assertTrue(token.isComment());

		token = new TokenImpl(ch, 0, 2, 0, TokenTypes.COMMENT_KEYWORD, 0);
		Assertions.assertTrue(token.isComment());

		token = new TokenImpl(ch, 0, 2, 0, TokenTypes.COMMENT_MARKUP, 0);
		Assertions.assertTrue(token.isComment());

		token = new TokenImpl(ch, 0, 2, 0, TokenTypes.MARKUP_COMMENT, 0);
		Assertions.assertTrue(token.isComment());
	}


	@Test
	void testComment_false() {
		char[] ch = "for".toCharArray();
		TokenImpl token = new TokenImpl(ch, 0, 2, 0, TokenTypes.RESERVED_WORD, 0);
		Assertions.assertFalse(token.isComment());
	}

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

		TokenImpl token = new TokenImpl();
		int actual = token.getListOffset(fm, text.toCharArray(), tokenStartOffset, tokenEndOffset, x0, x);
		String msg = String.format("Clicked=%s - Expected: \"%s\"[%s]='%s' | Actual: %s='%s'", x, text, expected, (expected < 0 ? null : text.substring(expected, expected + 1)), actual, (actual < 0 || actual >= text.length() ? null : text.substring(actual, actual + 1)));
		Assertions.assertEquals(expected, actual, msg);
	}

	private static class MyFontMetrics extends FontMetrics {
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
			return proportionalFont ? randomize(averageCharacterWidth) : (int) averageCharacterWidth;
		}

		@Override
		public int charsWidth(char[] data, int off, int len) {
			float w = len * averageCharacterWidth;
			return proportionalFont ? randomize(w) : (int) w;
	}

		private int randomize(float w) {
			return (int) (w + 1/Math.random());
		}

		private class MyFont extends Font {
			public MyFont(int size) {
				super("TEST", PLAIN, size);
			}

			@Override
			public Rectangle2D getStringBounds(char[] chars, int beginIndex, int limit, FontRenderContext frc) {
				int x = 0;
				int y = size;
				int width = charsWidth(chars, beginIndex, limit-beginIndex);
				int height = size;
				return new Rectangle2D.Float(x, y, width, height);
			}
		}
	}
}