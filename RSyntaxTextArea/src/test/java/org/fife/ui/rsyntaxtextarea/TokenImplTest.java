/*
 * 12/10/2016
 *
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
    void testGetListOffsetForToken() {
		float xOffset = 0;

		// ideal click before character (token is first one the line)
		assertGetListOffsetForToken(0, xOffset, 0f);
		assertGetListOffsetForToken(1, xOffset, 10f);
		assertGetListOffsetForToken(2, xOffset, 20f);
		assertGetListOffsetForToken(3, xOffset, 30f);
		assertGetListOffsetForToken(4, xOffset, 40f);
		assertGetListOffsetForToken(5, xOffset, 50f);
		assertGetListOffsetForToken(6, xOffset, 60f);
		assertGetListOffsetForToken(7, xOffset, 70f);
		assertGetListOffsetForToken(8, xOffset, 80f);
		assertGetListOffsetForToken(9, xOffset, 90f);

		// with offset (token is not first on the line)
		xOffset = 100;
		assertGetListOffsetForToken(0, xOffset, 100f);
		assertGetListOffsetForToken(1, xOffset, 110f);
		assertGetListOffsetForToken(2, xOffset, 120f);
		assertGetListOffsetForToken(3, xOffset, 130f);
		assertGetListOffsetForToken(4, xOffset, 140f);
		assertGetListOffsetForToken(5, xOffset, 150f);
		assertGetListOffsetForToken(6, xOffset, 160f);
		assertGetListOffsetForToken(7, xOffset, 170f);
		assertGetListOffsetForToken(8, xOffset, 180f);
		assertGetListOffsetForToken(9, xOffset, 190f);

		// click on character ("truncate")
		xOffset = 0;
		assertGetListOffsetForToken(0, xOffset, 5f);
		assertGetListOffsetForToken(0, xOffset, 5.9f);
		assertGetListOffsetForToken(9, xOffset, 95f);
		assertGetListOffsetForToken(9, xOffset, 95.9f);

		// click close to end of character ("round"") - NOT IMPLEMENTED
		xOffset = 0;
//		assertGetLargeListOffset(1, 5.01f);
//		assertGetLargeListOffset(-1, 95.01f);
	}

	private static void assertGetListOffsetForToken(int expected, float x0, float x) {
		char[] text = "0123456789".toCharArray();
		int tokenStartOffset = 0;
		int tokenEndOffset = text.length;
		FontMetrics fm = new MyFontMetrics(10f);

		TokenImpl token = new TokenImpl();
		int actual = token.getListOffsetForToken(fm, text, tokenStartOffset, tokenEndOffset, x0, x);
		String msg = String.format("Clicked=%s - Expected: \"%s\"[%s]='%s' | Actual: %s='%s'", x, String.copyValueOf(text), expected, (expected<0 ? null : text[expected]), actual, (actual < 0 || actual >= text.length ? null : text[actual]));
		Assertions.assertEquals(expected, actual, msg);
	}

	@Test
	void testTabConverterFindStartOfLine() {
		assertTabConverterFindStartOfLine("text", 0);
		assertTabConverterFindStartOfLine("a\ntext", 2);
		assertTabConverterFindStartOfLine("a\rtext", 2);
		assertTabConverterFindStartOfLine("a\ntext", 2);
		assertTabConverterFindStartOfLine("a\r\ntext", 3);
		assertTabConverterFindStartOfLine("a\nb\ntext", 4);
	}

	private void assertTabConverterFindStartOfLine(String text, int expected) {
		int actual = TokenImpl.MyTabConverter.findStartOfLine(text.toCharArray(), text.length()-1);
		Assertions.assertEquals(expected, actual, text.substring(actual));
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
		char[] text = tokenLine.toCharArray();
		int length = tokenLine.length() - offset;

		TokenImpl token = new TokenImpl();
		token.textOffset=offset;
		token.textCount= length;
		token.text = text;

		// initialize converter to expand tabs in text
		TokenImpl.MyTabConverter converter = new TokenImpl.MyTabConverter(tabSize, token);

		// verify expanded text
		char[] expanded = converter.getConvertedLine();
		int cvtOffset = converter.getTokenTextOffset();
		int cvtCount = converter.getTokenTextCount();

		String actualTokenText = String.copyValueOf(expanded, cvtOffset, cvtCount);
		Assertions.assertEquals(expectedTokenText, actualTokenText);

		// verify original offset, simulating a click on the token start (cvtOffset)
		int actualOffset = converter.toTabbedOffset(cvtOffset);
		Assertions.assertEquals(offset, actualOffset, String.copyValueOf(text));
	}

	private static class MyFontMetrics extends FontMetrics {
		private final float fixedCharacterWidth;

		protected MyFontMetrics(float fixedCharacterWidth) {
			super(null);
			this.fixedCharacterWidth = fixedCharacterWidth;
		}

		@Override
		public int charsWidth(char[] data, int off, int len) {
			return (int) (len * fixedCharacterWidth);
		}
	}
}
