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
	void testIsTabConversionFriendly() {
		// ok if we have regular text or no tabs
		assertIsTabConversionFriendly(true, "az\t", false);
		assertIsTabConversionFriendly(true, "åäöüé", false);
		assertIsTabConversionFriendly(true, "はユケンルマ", false);
		assertIsTabConversionFriendly(true, "aは", false);

		// not possible if we mix tabs with wide characters
		assertIsTabConversionFriendly(false, "は\t", false);
		assertIsTabConversionFriendly(false, "\tは", false);

		// not possible if we have tabs in proportional fonts
		assertIsTabConversionFriendly(false, "a\tb", true);
	}

	private static void assertIsTabConversionFriendly(boolean expected, String string, boolean proportionalFont) {
		char[] line = string.toCharArray();
		TokenImpl token = new TokenImpl(line, 0, string.length()-1, 0, TokenTypes.IDENTIFIER, 0);
		MyFontMetrics fm = new MyFontMetrics(10, proportionalFont);

		String msg = "'" + string.replace('\t', '>') + "'";
		boolean actual = TokenImpl.isTabConversionFriendly(fm, token);
		Assertions.assertEquals(expected, actual, msg);
	}

	@Test
	void testGetListOffset() {
		String[] lines = {
			"a\tb",
			"c\td",
			"efghij",
			"k\t"
		};
		assertGetListOffset(1 + 4, 1, 10f, lines);

		// first line
		int lineIndex = 0;
		assertGetListOffset(0, lineIndex, 0f, lines);  // before 'a'
		assertGetListOffset(1, lineIndex, 10f, lines); // before tab
		assertGetListOffset(1, lineIndex, 20f, lines); // on tab
		assertGetListOffset(1, lineIndex, 30f, lines); // on tab
		assertGetListOffset(2, lineIndex, 40f, lines); // before 'b'

		// second line
		lineIndex = 1;
		int lineOffset = 4;  // line 0=3 + EOL=1
		assertGetListOffset(0 + lineOffset, lineIndex, 0f, lines);
		assertGetListOffset(1 + lineOffset, lineIndex, 10f, lines);
		assertGetListOffset(1 + lineOffset, lineIndex, 20f, lines);
		assertGetListOffset(1 + lineOffset, lineIndex, 30f, lines);
		assertGetListOffset(2 + lineOffset, lineIndex, 40f, lines);
	}

	private void assertGetListOffset(int expectedOffset, int lineIndex, float x, String[] lines) {
		int offset = 0;
		for (int i = 0; i < lineIndex; i++) {
			offset += 1 + lines[i].length();
		}
		String tokenLine = lines[lineIndex];
		String escapedText = escape(String.join("\n", lines));
		String escapedTokenLine = escape(tokenLine);

		float x0 = 0f;
		int endOffset = offset + tokenLine.length();
		TabExpander e = Mockito.mock(TabExpander.class);
		RSyntaxTextArea rsta = new MyRSyntaxTextArea(4, 10f, lines);
		Token tokens = rsta.getTokenListFor(offset, endOffset);

		StringJoiner chars = new StringJoiner("\n");
		for (int i = 0; i < escapedTokenLine.length(); i++) {
			chars.add(String.format("%d='%s'", i, escapedTokenLine.charAt(i)));
		}
		int actualOffset = tokens.getListOffset(rsta, e, x0, x);
		String msg = String.format("x=%.1f | line %d [%s]%n" +
				"expected='%s' | actual='%s'%n" +
				"%s%n",
			x, lineIndex, escapedTokenLine,
			escapedText.charAt(expectedOffset), escapedText.charAt(actualOffset),
			chars);
		Assertions.assertEquals(expectedOffset, actualOffset, msg);

	}

	private static String escape(String tokenLine) {
		return tokenLine.replace('\n', '\\').replace('\t', '>');
	}

	@Test
    void testGetListOffsetForToken() {
		String text = "0123456789";
		float xOffset = 0;

		// ideal click before character (token is first one the line)
		assertGetListOffsetForToken(text, 0, xOffset, 0f);
		assertGetListOffsetForToken(text, 1, xOffset, 10f);
		assertGetListOffsetForToken(text, 2, xOffset, 20f);
		assertGetListOffsetForToken(text, 3, xOffset, 30f);
		assertGetListOffsetForToken(text, 4, xOffset, 40f);
		assertGetListOffsetForToken(text, 5, xOffset, 50f);
		assertGetListOffsetForToken(text, 6, xOffset, 60f);
		assertGetListOffsetForToken(text, 7, xOffset, 70f);
		assertGetListOffsetForToken(text, 8, xOffset, 80f);
		assertGetListOffsetForToken(text, 9, xOffset, 90f);

		// with offset (token is not first on the line)
		xOffset = 100;
		assertGetListOffsetForToken(text, 0, xOffset, 100f);
		assertGetListOffsetForToken(text, 1, xOffset, 110f);
		assertGetListOffsetForToken(text, 2, xOffset, 120f);
		assertGetListOffsetForToken(text, 3, xOffset, 130f);
		assertGetListOffsetForToken(text, 4, xOffset, 140f);
		assertGetListOffsetForToken(text, 5, xOffset, 150f);
		assertGetListOffsetForToken(text, 6, xOffset, 160f);
		assertGetListOffsetForToken(text, 7, xOffset, 170f);
		assertGetListOffsetForToken(text, 8, xOffset, 180f);
		assertGetListOffsetForToken(text, 9, xOffset, 190f);

		// click on character ("truncate")
		xOffset = 0;
		assertGetListOffsetForToken(text, 0, xOffset, 5f);
		assertGetListOffsetForToken(text, 0, xOffset, 5.9f);
		assertGetListOffsetForToken(text, 9, xOffset, 95f);
		assertGetListOffsetForToken(text, 9, xOffset, 95.9f);

		// click close to end of character ("round"") - NOT IMPLEMENTED
		xOffset = 0;
//		assertGetLargeListOffset(1, 5.01f);
//		assertGetLargeListOffset(-1, 95.01f);
	}

	private static void assertGetListOffsetForToken(String text, int expected, float x0, float x) {
		int tokenStartOffset = 0;
		int tokenEndOffset = text.length();
		FontMetrics fm = new MyFontMetrics(10f);

		TokenImpl token = new TokenImpl();
		int actual = token.getListOffsetForToken(fm, text, tokenStartOffset, tokenEndOffset, x0, x);
		String msg = String.format("Clicked=%s - Expected: \"%s\"[%s]='%s' | Actual: %s='%s'", x, text, expected, (expected<0 ? null : text.substring(expected, expected+1)), actual, (actual < 0 || actual >= text.length() ? null : text.substring(actual, actual+1)));
		Assertions.assertEquals(expected, actual, msg);
	}

	private static Document newDocument(String text) {
		try {
			StringContent c = new StringContent();
			c.insertString(0, text);
			Document doc = new PlainDocument(c);
			return doc;
		} catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void testTabConverterOffset() {
		String text = "a\tab\tabc\tabcd\t";
		assertTabConverterOffset(text, 0, 0);
		assertTabConverterOffset(text, 1, 1); // first tab
		assertTabConverterOffset(text, 2, 4);
		assertTabConverterOffset(text, 3, 5);
		assertTabConverterOffset(text, 4, 6); // second tab
		assertTabConverterOffset(text, 5, 8);
		assertTabConverterOffset(text, 6, 9);
		assertTabConverterOffset(text, 7, 10);
		assertTabConverterOffset(text, 8, 11); // third tab
		assertTabConverterOffset(text, 9, 12);
		assertTabConverterOffset(text, 10, 13);
		assertTabConverterOffset(text, 11, 14);
		assertTabConverterOffset(text, 12, 15);
		assertTabConverterOffset(text, 13, 16); // fourth tab
	}

	private void assertTabConverterOffset(String tokenLine, int offset, int expectedConverterOffset) {
		Document document = newDocument(tokenLine);
		TokenImpl.MyTabConverter converter = new TokenImpl.MyTabConverter(4, newToken(offset, tokenLine), document, 0);
		int cvtOffset = converter.getTokenOffset();
		Assertions.assertEquals(expectedConverterOffset, cvtOffset, "ConverterOffset");
		Assertions.assertEquals(offset, converter.toTabbedOffset(cvtOffset));
	}

	@Test
	void testTabConverter() {
		String text = "a\tab\tabc\tabcd\t";
		assertTabConverter(text, 0, "a   ab  abc abcd    ");
		assertTabConverter(text, 1, "   ab  abc abcd    ");
		assertTabConverter(text, 2, "ab  abc abcd    ");
		assertTabConverter(text, 3, "b  abc abcd    ");
		assertTabConverter(text, 4, "  abc abcd    ");

		text = "\t\ta";
		assertTabConverter(text, 2, "a");
	}

	private static void assertTabConverter(String tokenLine, int offset, String expectedTokenText) {
		int tabSize = 4;
		TokenImpl token = newToken(offset, tokenLine);

		// initialize converter to expand tabs in text
		TokenImpl.MyTabConverter converter = new TokenImpl.MyTabConverter(tabSize, token, newDocument(tokenLine), 0);

		// verify expanded text
		String expanded = converter.getConvertedLine();
		int cvtOffset = converter.getTokenOffset();
		int cvtCount = converter.getTokenCount();

		String actualTokenText = expanded.substring(cvtOffset, cvtOffset+cvtCount);
		Assertions.assertEquals(expectedTokenText, actualTokenText);

		// verify original offset, simulating a click on the token start (cvtOffset)
		int actualOffset = converter.toTabbedOffset(cvtOffset);
		Assertions.assertEquals(offset, actualOffset, tokenLine);
	}

	/**
	 * Create a simple token for testing text on a single line.
	 * Token starts at the specified offset and ends at the end of the supplied line.
	 *
	 * @param offset in document and text
	 * @param tokenLine a single line of text where the token sits
	 * @return a Token that works for simple cases.
	 */
	private static TokenImpl newToken(int offset, String tokenLine) {
		char[] text = tokenLine.toCharArray();
		int begin = offset;
		int end = tokenLine.length() - 1;
		int startOffset = offset;
		int languageIndex = 0;
		TokenImpl token = new TokenImpl(text, begin, end, startOffset, TokenTypes.IDENTIFIER, languageIndex);
		Assertions.assertEquals(token.getLexeme(), tokenLine.substring(offset), "Bad test setup");
		return token;
	}

	private static class MyFontMetrics extends FontMetrics {
		private final float averageCharacterWidth;
		private final boolean proportionalFont;
		private final Font font;

		protected MyFontMetrics(float fixedCharacterWidth) {
			this(fixedCharacterWidth, false);
		}

		protected MyFontMetrics(float averageCharacterWidth, boolean proportionalFont) {
			super(null);
			this.averageCharacterWidth = averageCharacterWidth;
			this.proportionalFont = proportionalFont;
			this.font = new MyFont(averageCharacterWidth);
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

		private static class MyFont extends Font {
			private final float averageCharacterWidth;

			public MyFont(float averageCharacterWidth) {
				super("TestFont", Font.PLAIN, 12);
				this.averageCharacterWidth = averageCharacterWidth;
			}

			@Override
			public Rectangle2D getStringBounds(String str, int beginIndex, int limit, FontRenderContext frc) {
				float x = 0;
				float y = 0;
				float w = (limit - beginIndex) * averageCharacterWidth;
				float h = getSize2D();
				return new Rectangle2D.Float(x, y, w, h);
			}
		}
	}

	private static class MyRSyntaxTextArea extends RSyntaxTextArea {
		private final int tabSize;
		private final MyFontMetrics fontMetrics;

		private MyRSyntaxTextArea(int tabSize, float charWidth, String[] textLines) {
			this.tabSize = tabSize;
			this.fontMetrics = new MyFontMetrics(charWidth);
			setText(String.join("\n", textLines));
		}

		@Override
		public int getTabSize() {
			return tabSize;
		}

		@Override
		public FontMetrics getFontMetricsForTokenType(int type) {
			return fontMetrics;
		}
	}
}