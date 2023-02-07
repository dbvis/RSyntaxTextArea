/*
 * 02/21/2004
 *
 * Token.java - A token used in syntax highlighting.
 *
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import org.fife.util.SwingUtils;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.geom.Rectangle2D;
import javax.swing.text.*;
import java.util.function.Supplier;
import java.util.logging.Logger;


/**
 * The default implementation of {@link Token}.<p>
 *
 * <b>Note:</b> The instances of <code>Token</code> returned by
 * {@link RSyntaxDocument}s are pooled and should always be treated as
 * immutable.  They should not be cast to <code>TokenImpl</code> and modified.
 * Modifying tokens you did not create yourself can and will result in
 * rendering issues and/or runtime exceptions. You have been warned!
 *
 * @author Robert Futrell
 * @version 0.3
 */
@SuppressWarnings("checkstyle:visibilitymodifier")
public class TokenImpl implements Token {
	private static final Logger LOG = Logger.getLogger(TokenImpl.class.getName());

	/**
	 * The text this token represents.  This is implemented as a segment so we
	 * can point directly to the text in the document without having to make a
	 * copy of it.
	 */
	public char[] text;
	public int textOffset;
	public int textCount;

	/**
	 * The offset into the document at which this token resides.
	 */
	private int offset;

	/**
	 * The type of token this is; for example, {@link #FUNCTION}.
	 */
	private int type;

	/**
	 * Whether this token is a hyperlink.
	 */
	private boolean hyperlink;

	/**
	 * The next token in this linked list.
	 */
	private Token nextToken;

	/**
	 * The language this token is in, <code>&gt;= 0</code>.
	 */
	private int languageIndex;

	/** Max size of text strings to process when converting x coordinate to model offset. */
	int listOffsetChunkSize = -1; // 1000; // Disabled; yields offset errors unless token is first on line (?)


	/**
	 * Creates a "null" token.  The token itself is not null; rather, it
	 * signifies that it is the last token in a linked list of tokens and
	 * that it is not part of a "multi-line token."
	 */
	public TokenImpl() {
		this.text = null;
		this.textOffset = -1;
		this.textCount = -1;
		this.setType(NULL);
		setOffset(-1);
		hyperlink = false;
		nextToken = null;
	}


	/**
	 * Constructor.
	 *
	 * @param line The segment from which to get the token.
	 * @param beg The first character's position in <code>line</code>.
	 * @param end The last character's position in <code>line</code>.
	 * @param startOffset The offset into the document at which this
	 *        token begins.
	 * @param type A token type listed as "generic" above.
	 * @param languageIndex The language index for this token.
	 */
	public TokenImpl(Segment line, int beg, int end, int startOffset, int type,
			int languageIndex) {
		this(line.array, beg,end, startOffset, type, languageIndex);
	}


	/**
	 * Constructor.
	 *
	 * @param line The segment from which to get the token.
	 * @param beg The first character's position in <code>line</code>.
	 * @param end The last character's position in <code>line</code>.
	 * @param startOffset The offset into the document at which this
	 *        token begins.
	 * @param type A token type listed as "generic" above.
	 * @param languageIndex The language index for this token.
	 */
	public TokenImpl(char[] line, int beg, int end, int startOffset, int type,
			int languageIndex) {
		this();
		set(line, beg,end, startOffset, type);
		setLanguageIndex(languageIndex);
	}


	/**
	 * Creates this token as a copy of the passed-in token.
	 *
	 * @param t2 The token from which to make a copy.
	 */
	public TokenImpl(Token t2) {
		this();
		copyFrom(t2);
	}


	@Override
	public StringBuilder appendHTMLRepresentation(StringBuilder sb,
											RSyntaxTextArea textArea,
											boolean fontFamily) {
		return appendHTMLRepresentation(sb, textArea, fontFamily, false);
	}


	@Override
	public StringBuilder appendHTMLRepresentation(StringBuilder sb,
								RSyntaxTextArea textArea, boolean fontFamily,
								boolean tabsToSpaces) {

		SyntaxScheme colorScheme = textArea.getSyntaxScheme();
		Style scheme = colorScheme.getStyle(getType());
		Font font = textArea.getFontForTokenType(getType());//scheme.font;

		if (font.isBold()) {
			sb.append("<b>");
		}
		if (font.isItalic()) {
			sb.append("<em>");
		}
		if (scheme.underline || isHyperlink()) {
			sb.append("<u>");
		}

		boolean needsFontTag = fontFamily || !isWhitespace();
		if (needsFontTag) {
			sb.append("<font");
			if (fontFamily) {
				sb.append(" face=\"").append(font.getFamily()).append('"');
			}
			if (!isWhitespace()) {
				sb.append(" color=\"").append(
						getHTMLFormatForColor(scheme.foreground)).append('"');
			}
			sb.append('>');
		}

		// NOTE: Don't use getLexeme().trim() because whitespace tokens will
		// be turned into NOTHING.
		appendHtmlLexeme(textArea, sb, tabsToSpaces);

		if (needsFontTag) {
			sb.append("</font>");
		}
		if (scheme.underline || isHyperlink()) {
			sb.append("</u>");
		}
		if (font.isItalic()) {
			sb.append("</em>");
		}
		if (font.isBold()) {
			sb.append("</b>");
		}

		return sb;

	}


	/**
	 * Appends an HTML version of the lexeme of this token (i.e. no style
	 * HTML, but replacing chars such as <code>\t</code>, <code>&lt;</code>
	 * and <code>&gt;</code> with their escapes).
	 *
	 * @param textArea The text area.
	 * @param sb The buffer to append to.
	 * @param tabsToSpaces Whether to convert tabs into spaces.
	 * @return The same buffer.
	 */
	private StringBuilder appendHtmlLexeme(RSyntaxTextArea textArea,
								StringBuilder sb, boolean tabsToSpaces) {

		boolean lastWasSpace = false;
		int i = textOffset;
		int lastI = i;
		String tabStr = null;

		while (i<textOffset+textCount) {
			char ch = text[i];
			switch (ch) {
				case ' ':
					sb.append(text, lastI, i-lastI);
					lastI = i+1;
					sb.append(lastWasSpace ? "&nbsp;" : " ");
					lastWasSpace = true;
					break;
				case '\t':
					sb.append(text, lastI, i-lastI);
					lastI = i+1;
					if (tabsToSpaces && tabStr==null) {
                        StringBuilder stringBuilder = new StringBuilder();
						for (int j=0; j<textArea.getTabSize(); j++) {
                            stringBuilder.append("&nbsp;");
						}
                        tabStr = stringBuilder.toString();
					}
					sb.append(tabsToSpaces ? tabStr : "&#09;");
					lastWasSpace = false;
					break;
				case '&':
					sb.append(text, lastI, i-lastI);
					lastI = i+1;
					sb.append("&amp;");
					lastWasSpace = false;
					break;
				case '<':
					sb.append(text, lastI, i-lastI);
					lastI = i+1;
					sb.append("&lt;");
					lastWasSpace = false;
					break;
				case '>':
					sb.append(text, lastI, i-lastI);
					lastI = i+1;
					sb.append("&gt;");
					lastWasSpace = false;
					break;
				case '\'':
					sb.append(text, lastI, i-lastI);
					lastI = i+1;
					sb.append("&#39;");
					lastWasSpace = false;
					break;
				case '"':
					sb.append(text, lastI, i-lastI);
					lastI = i+1;
					sb.append("&#34;");
					lastWasSpace = false;
					break;
				case '/': // OWASP-recommended to escape even though unnecessary
					sb.append(text, lastI, i-lastI);
					lastI = i+1;
					sb.append("&#47;");
					lastWasSpace = false;
					break;
				default:
					lastWasSpace = false;
					break;
			}
			i++;
		}
		if (lastI<textOffset+textCount) {
			sb.append(text, lastI, textOffset+textCount-lastI);
		}
		return sb;
	}


	@Override
	public char charAt(int index) {
		return text[textOffset + index];
	}


	@Override
	public boolean containsPosition(int pos) {
		return pos>=getOffset() && pos<getOffset()+textCount;
	}


	/**
	 * Makes one token point to the same text segment, and have the same value
	 * as another token.
	 *
	 * @param t2 The token from which to copy.
	 */
	public void copyFrom(Token t2) {
		text = t2.getTextArray();
		textOffset = t2.getTextOffset();
		textCount = t2.length();
		setOffset(t2.getOffset());
		setType(t2.getType());
		hyperlink = t2.isHyperlink();
		languageIndex = t2.getLanguageIndex();
		nextToken = t2.getNextToken();
	}


	@Override
	public int documentToToken(int pos) {
		return pos + (textOffset-getOffset());
	}


	@Override
	public boolean endsWith(char[] ch) {
		if (ch==null || ch.length>textCount) {
			return false;
		}
		final int start = textOffset + textCount - ch.length;
		for (int i=0; i<ch.length; i++) {
			if (text[start+i]!=ch[i]) {
				return false;
			}
		}
		return true;
	}


	@Override
	public boolean equals(Object obj) {

		if (obj==this) {
			return true;
		}
		if (!(obj instanceof Token)) {
			return false;
		}

		Token t2 = (Token)obj;
		return offset==t2.getOffset() &&
				type==t2.getType() &&
				languageIndex==t2.getLanguageIndex() &&
				hyperlink==t2.isHyperlink() &&
				((getLexeme()==null && t2.getLexeme()==null) ||
					(getLexeme()!=null && getLexeme().equals(t2.getLexeme())));

	}


	@Override
	public int getEndOffset() {
		return offset + textCount;
	}


	/**
	 * Returns a <code>String</code> of the form "#xxxxxx" good for use
	 * in HTML, representing the given color.
	 *
	 * @param color The color to get a string for.
	 * @return The HTML form of the color.  If <code>color</code> is
	 *         <code>null</code>, <code>#000000</code> is returned.
	 */
	private static String getHTMLFormatForColor(Color color) {
		if (color==null) {
			return "black";
		}
		String hexRed = Integer.toHexString(color.getRed());
		if (hexRed.length()==1) {
			hexRed = "0" + hexRed;
		}
		String hexGreen = Integer.toHexString(color.getGreen());
		if (hexGreen.length()==1) {
			hexGreen = "0" + hexGreen;
		}
		String hexBlue = Integer.toHexString(color.getBlue());
		if (hexBlue.length()==1) {
			hexBlue = "0" + hexBlue;
		}
		return "#" + hexRed + hexGreen + hexBlue;
	}


	@Override
	public String getHTMLRepresentation(RSyntaxTextArea textArea) {
		StringBuilder buf = new StringBuilder();
		appendHTMLRepresentation(buf, textArea, true);
		return buf.toString();
	}


	@Override
	public int getLanguageIndex() {
		return languageIndex;
	}


	@Override
	public Token getLastNonCommentNonWhitespaceToken() {

		Token last = null;

		for (Token t=this; t!=null && t.isPaintable(); t=t.getNextToken()) {
			switch (t.getType()) {
				case COMMENT_DOCUMENTATION:
				case COMMENT_EOL:
				case COMMENT_MULTILINE:
				case COMMENT_KEYWORD:
				case COMMENT_MARKUP:
				case WHITESPACE:
					break;
				default:
					last = t;
					break;
			}
		}

		return last;

	}


	@Override
	public Token getLastPaintableToken() {
		Token t = this;
		while (t.isPaintable()) {
			Token next = t.getNextToken();
			if (next==null || !next.isPaintable()) {
				return t;
			}
			t = next;
		}
		return null;
	}


	@Override
	public String getLexeme() {
		if (text == null) {
			return null;
		}
		return isPaintable() ? new String(text, textOffset, textCount) : null;
	}


	@Override
	public int getListOffset(RSyntaxTextArea textArea, TabExpander e,
			float x0, float x) {

		// If the coordinate in question is before this line's start, quit.
		if (x0 >= x) {
			return getOffset();
		}
		FontMetrics fm = textArea.getFontMetrics(textArea.getFont());
		return SwingUtils.isMonospaced(fm)
			? getListOffsetMonospace(textArea, fm, x0, x)
			: getListOffsetProportional(textArea, e, x0, x);
	}

	private int getListOffsetMonospace(RSyntaxTextArea textArea, FontMetrics fm, float x0, float x) {
		int tabSize = textArea.getTabSize();
		float currX = x0; // x-coordinate of current char.
		float nextX = x0; // x-coordinate of next char.
		float stableX = x0; // Cached ending x-coord. of last tab or token.
		TokenImpl token = this;
		int last = getOffset();
		int offsetOnLine = 0;
		long started = System.currentTimeMillis();
		float spaceWidth = SwingUtils.charWidth(fm, ' ');

		while (token != null && token.isPaintable()) {

			char[] text = token.text;
			int start = token.textOffset;
			int end = start + token.textCount;
			int charCount = 0;

			for (int i = start; i < end; i++) {
				currX = nextX;
				if (text[i] == '\t') {
					// add width of characters before the tab and reset counter
					float charsWidth = SwingUtils.charsWidth(fm, text, i, charCount);
					nextX = stableX + charsWidth;
					charCount = 0;

					// tabstop
					int remainder = offsetOnLine % tabSize;
					int filler = tabSize - remainder;
					nextX += filler * spaceWidth;
					offsetOnLine += filler;

					// done?
					if (x >= currX && x < nextX) {
						int result = x - currX < nextX - x ? last + i - token.textOffset : last + i + 1 - token.textOffset;
						LOG.fine(() -> String.format("%,d ms: Found in tab: x=%.3f => offset=%,d",
							System.currentTimeMillis() - started, x, result));
						return result;
					}

				} else {

					// regular character - increment counter
					charCount++;
					offsetOnLine ++;
				}
			}

			// process remaining text after last tab (if any)
			if (charCount > 0) {
				int begin = end - charCount;
				float width = SwingUtils.charsWidth(fm, text, begin, charCount);
				float lastX = nextX + width;

				// x inside text?
				if (x<=lastX) {
					float relativeX = (x - currX) / width;
					int xOffsetInText = Math.round(charCount * relativeX);
					int xOffsetInToken = token.textCount - charCount + xOffsetInText;
					int tokenOffsetInDocument = token.getOffset();
					int xOffsetInDocument = tokenOffsetInDocument + xOffsetInToken;
					int tokenTextCount = token.textCount;
					LOG.fine(() -> debugListOffset("Monospaced tail",
						started, textArea, text, tokenOffsetInDocument, xOffsetInText, xOffsetInToken, xOffsetInDocument, tokenTextCount));
					return xOffsetInDocument;
				} else {
					nextX += width; // add width and continue to next token
				}
			}

			// no match in token - continue to next
			stableX = nextX; // Cache ending x-coordinate of token.
			last += token.textCount;
			token = (TokenImpl)token.getNextToken();

		}

		// If we didn't find anything, return the end position of the text.
		LOG.fine("Monospaced EOL");
		return last;

	}

	int getListOffsetProportional(RSyntaxTextArea textArea, TabExpander e,
										  float x0, float x) {
		float currX = x0; // x-coordinate of current char.
		float nextX = x0; // x-coordinate of next char.
		float stableX = x0; // Cached ending x-coord. of last tab or token.
		TokenImpl token = this;
		int last = getOffset();
		long started = System.currentTimeMillis();

		while (token != null && token.isPaintable()) {

			FontMetrics fm = textArea.getFontMetricsForTokenType(token.getType());
			char[] text = token.text;
			int start = token.textOffset;
			int end = start + token.textCount;
			int charCount = 0;

			for (int i = start; i < end; i++) {
				currX = nextX;
				if (text[i] == '\t') {
					// add width of characters before the tab and reset counter
					float charsWidth = SwingUtils.charsWidth(fm, text, i-charCount, charCount);
					nextX = stableX + charsWidth;
					charCount = 0;

					// tabstop
					nextX = e.nextTabStop(nextX, 0);
					stableX = nextX; // Cache ending x-coord. of tab.

					// done?
					if (x >= currX && x < nextX) {
						int result = x-currX < nextX-x ?  last+i-token.textOffset : last+i+1-token.textOffset;
						LOG.fine(()->String.format("%,d ms: Found in tab: x=%.3f => offset=%,d",
							System.currentTimeMillis()-started, x, result));
						return result;
					}

				} else if (listOffsetChunkSize>0 && charCount>0 && charCount % listOffsetChunkSize == 0) {
					// check chunk (improves performance by reducing max length of string to measure width for)
					int begin = i - charCount;
					float charsWidth = SwingUtils.charsWidth(fm, text, begin, charCount);
					nextX = stableX + charsWidth;

					// x inside chunk?
					if (x < nextX) {
						int xOffsetInText = getListOffset(fm, text, begin, begin, charCount, stableX, x);
						int xOffsetInToken = xOffsetInText - token.textOffset;
						int tokenOffsetInDocument = token.getOffset();
						int xOffsetInDocument = tokenOffsetInDocument + xOffsetInToken;
						LOG.fine(() -> debugListOffset("Proportional Chunk",
							started, textArea, text, tokenOffsetInDocument, xOffsetInText, xOffsetInToken, xOffsetInDocument, textCount));
						return xOffsetInDocument;
					}

					// x beyond end of chunk
					stableX = nextX;
					charCount = 1;

				} else {

					// regular character - increment counter
					charCount++;
				}
			}

			// process remaining text after last tab or chunk (if any)
			if (charCount > 0) {
				int begin = end - charCount;
				float width = SwingUtils.charsWidth(fm, text, begin, charCount);
				float lastX = nextX + width;
				// x inside text?
				if (x<=lastX) {
					int xOffsetInText = getListOffset(fm, text, begin, begin, charCount, nextX, x);
					int xOffsetInToken = xOffsetInText - token.textOffset;
					int tokenOffsetInDocument = token.getOffset();
					int xOffsetInDocument = tokenOffsetInDocument + xOffsetInToken;
					int textCount = token.textCount;
					LOG.fine(() -> debugListOffset("Proportional Tail",
						started, textArea, text, tokenOffsetInDocument, xOffsetInText, xOffsetInToken, xOffsetInDocument, textCount));
					return xOffsetInDocument;
				} else {
					nextX += width; // add width and continue to next token
				}
			}

			stableX = nextX; // Cache ending x-coordinate of token.
			last += token.textCount;
			token = (TokenImpl)token.getNextToken();

		}

		// If we didn't find anything, return the end position of the text.
		return last;

	}

	private static String debugListOffset(String info, long started, RSyntaxTextArea textArea, char[] text, int tokenOffset,
										  int offsetInText, int offsetInToken, int offsetInDocument, int tokenTextCount) {
		long elapsed = System.currentTimeMillis() - started;
		int length = textArea.getText().length();
		String character = new String(text, offsetInText, 1);
		String substring = textArea.getText().substring(offsetInDocument, offsetInDocument + 1);
		return String.format(
			"%s: %,d ms: Total text length: %,d | Token Offset=%,d Count=%,d | " +
				"offsetInText=%,d (%s) | offsetInToken=%,d => offsetInDocument=%,d ('%s') %n",
			info, elapsed, length, tokenOffset, tokenTextCount,
				offsetInText, character, offsetInToken, offsetInDocument, substring);
	}


	/**
	 * <b>Internal, exposed for testing.</b>
	 * <p/>
	 * Determines the offset into the supplied text block that covers pixel location <code>x</code> using a faster
	 * algorithm that doesn't check the width of each character, presuming that the text block does not contain any tab
	 * characters (<code>'\t'</code>).
	 * <p/>
	 * Designed as subroutine of {@link #getListOffset(RSyntaxTextArea, TabExpander, float, float)} to improve
	 * performance on very long text strings (eg hex dumps of images).
	 *
	 * @param fm    FontMetrics for the token font
	 * @param chars the array of text to process
	 * @param off0 index of first character of the text block in the array
	 * @param off   index of first character in the array fragment to process in this run
	 * @param len   number of characters to process
	 * @param x0    The pixel x-location that is the beginning of the text segment
	 * @param x     The pixel-position for which you want to get the corresponding offset.
	 * @return the offset in the text corresponding to pixel x
	 */
	int getListOffset(FontMetrics fm, char[]  chars, int off0, int off, int len, float x0, float x) {
		assert !String.copyValueOf(chars, off, len).contains("\t") :
			"Text must not contain any tab characters: " + new String(chars, off, len);

		// found exact position?
		if (len<2) {
			LOG.finest(()-> String.format("Found: x=%,.3f => offset=%,d ('%s')", x, off, chars[off]));
			return off;
		}

		// search again - clicked before or after middle of text?
		// divide current segment in half and measure width from original start;
		// adding width of segments cause rounding errors on long (+2k) text blocks in fractionally scaled context
		int halfLen = len / 2; // split the current segment in two
		int zeroToHalfLen = off + halfLen - off0; // length from start of text to half of current segment
		float xMid = x0 + SwingUtils.charsWidth(fm, chars, off0, zeroToHalfLen);
		LOG.finest(() -> debugListOffsetRecursiveEntry(chars, off, len, x0, x, halfLen, zeroToHalfLen, xMid));

		// search first half?
		if (x < xMid) {
			LOG.finest(() -> debugListOffsetRecursiveCall(chars, "FIRST half", off, off + halfLen - 1, halfLen));
			return getListOffset(fm, chars, off0, off, halfLen, x0, x);
		}

		// search second half
		int nextOff = off + halfLen;
		int nextLen = len - halfLen;
		LOG.finest(() -> debugListOffsetRecursiveCall(chars, "SECOND half", nextOff, nextOff + nextLen - 1, nextLen));
		return getListOffset(fm,  chars, off0, nextOff, nextLen, x0, x);
	}

	private static String debugListOffsetRecursiveCall(char[] chars, String info, int first, int last, int nextLen) {
		return String.format("Recursive call: %s: %,d-%,d ('%s'-'%s') | nextOff=%,3d, nextLen=%,d%n", // intentional %n
				info, first, last, chars[first], chars[last], first, nextLen);
	}

	private static String debugListOffsetRecursiveEntry(
		char[] chars, int off, int len, float x0, float x, int halfLen, int zeroToHalfLen, float xMid) {
		return String.format(
			"Analyzing: x=%.3f | x0=%.3f | off=%,d | len=%,d | %,d-%,d ('%s'-'%s') | " +
				"halfLen=%,d | zeroToHalfLen=%,d => xMid=%.3f",
			x, x0, off, len, off, off + len - 1, chars[off], chars[off + len - 1],
				halfLen, zeroToHalfLen, xMid);
	}


	@Override
	public Token getNextToken() {
		return nextToken;
	}


	@Override
	public int getOffset() {
		return offset;
	}


	@Override
	public int getOffsetBeforeX(RSyntaxTextArea textArea, TabExpander e,
							float startX, float endBeforeX) {

		FontMetrics fm = textArea.getFontMetricsForTokenType(getType());
		int i = textOffset;
		int stop = i + textCount;
		float x = startX;

		while (i<stop) {
			if (text[i]=='\t') {
				x = e.nextTabStop(x, 0);
			}
			else {
				x += SwingUtils.charWidth(fm, text[i]);
			}
			if (x>endBeforeX) {
				// If not even the first character fits into the space, go
				// ahead and say the first char does fit so we don't go into
				// an infinite loop.
				int intoToken = Math.max(i-textOffset, 1);
				return getOffset() + intoToken;
			}
			i++;
		}

		// If we got here, the whole token fit in (endBeforeX-startX) pixels.
		return getOffset() + textCount - 1;

	}


	@Override
	public char[] getTextArray() {
		return text;
	}


	@Override
	public int getTextOffset() {
		return textOffset;
	}


	@Override
	public int getType() {
		return type;
	}


	@Override
	public float getWidth(RSyntaxTextArea textArea, TabExpander e, float x0) {
		return getWidthUpTo(textCount, textArea, e, x0);
	}


	@Override
	public float getWidthUpTo(int numChars, RSyntaxTextArea textArea,
			TabExpander e, float x0) {
		float width = x0;
		FontMetrics fm = textArea.getFontMetricsForTokenType(getType());
		if (fm != null) {
			int w;
			int currentStart = textOffset;
			int endBefore = textOffset + numChars;
			for (int i = currentStart; i < endBefore; i++) {
				if (text[i] == '\t') {
					// Since TokenMaker implementations usually group all
					// adjacent whitespace into a single token, there
					// aren't usually any characters to compute a width
					// for here, so we check before calling.
					w = i - currentStart;
					if (w > 0) {
						width += SwingUtils.charsWidth(fm, text, currentStart, w);
					}
					currentStart = i + 1;
					width = e.nextTabStop(width, 0);
				}
			}
			// Most (non-whitespace) tokens will have characters at this
			// point to get the widths for, so we don't check for w>0 (mini-
			// optimization).
			w = endBefore - currentStart;
			width += SwingUtils.charsWidth(fm, text, currentStart, w);
		}
		return width - x0;
	}


	@Override
	public int hashCode() {
		return offset + (getLexeme()==null ? 0 : getLexeme().hashCode());
	}


	@Override
	public boolean is(char[] lexeme) {
		if (textCount==lexeme.length) {
			for (int i=0; i<textCount; i++) {
				if (text[textOffset+i]!=lexeme[i]) {
					return false;
				}
			}
			return true;
		}
		return false;
	}


	@Override
	public boolean is(int type, char[] lexeme) {
		if (this.getType()==type && textCount==lexeme.length) {
			for (int i=0; i<textCount; i++) {
				if (text[textOffset+i]!=lexeme[i]) {
					return false;
				}
			}
			return true;
		}
		return false;
	}


	@Override
	public boolean is(int type, String lexeme) {
		return this.getType()==type && textCount==lexeme.length() &&
				lexeme.equals(getLexeme());
	}


	@Override
	public boolean isComment() {
		int type = getType();
		return (type >= TokenTypes.COMMENT_EOL && type <= TokenTypes.COMMENT_MARKUP) ||
			type == TokenTypes.MARKUP_COMMENT;
	}


	@Override
	public boolean isCommentOrWhitespace() {
		return isComment() || isWhitespace();
	}


	@Override
	public boolean isHyperlink() {
		return hyperlink;
	}


	@Override
	public boolean isIdentifier() {
		return getType()==IDENTIFIER;
	}


	@Override
	public boolean isLeftCurly() {
		return getType()==SEPARATOR && isSingleChar('{');
	}


	@Override
	public boolean isRightCurly() {
		return getType()==SEPARATOR && isSingleChar('}');
	}


	@Override
	public boolean isPaintable() {
		return getType()>Token.NULL;
	}


	@Override
	public boolean isSingleChar(char ch) {
		return textCount==1 && text[textOffset]==ch;
	}


	@Override
	public boolean isSingleChar(int type, char ch) {
		return getType()==type && isSingleChar(ch);
	}


	@Override
	public boolean isWhitespace() {
		return getType()==WHITESPACE;
	}


	@Override
	public int length() {
		return textCount;
	}


	@Override
	public Rectangle2D listOffsetToView(RSyntaxTextArea textArea, TabExpander e,
			int pos, float x0, Rectangle2D rect) {

		LOG.finest(()->"pos=" + pos + ", x0=" + x0);
		float stableX = x0; // Cached ending x-coord. of last tab or token.
		TokenImpl token = this;
		FontMetrics fm;
		Segment s = new Segment();

		while (token != null && token.isPaintable()) {

			fm = textArea.getFontMetricsForTokenType(token.getType());
			if (fm == null) {
				return rect; // Don't return null as things will error.
			}
			char[] text = token.text;
			int start = token.textOffset;
			int end = start + token.textCount;

			// If this token contains the position for which to get the
			// bounding box...
			if (token.containsPosition(pos)) {

				s.array = token.text;
				s.offset = token.textOffset;
				s.count = pos - token.getOffset();

				// Must use this (actually fm.charWidth()), and not
				// fm.charsWidth() for returned value to match up with where
				// text is actually painted on OS X!
				float w = Utilities.getTabbedTextWidth(s, fm, stableX, e,
					token.getOffset());
				SwingUtils.setX(rect, stableX + w);
				end = token.documentToToken(pos);

				if (text[end] == '\t') {
					SwingUtils.setWidth(rect, SwingUtils.charWidth(fm, ' '));
				}
				else {
					SwingUtils.setWidth(rect, SwingUtils.charWidth(fm, text[end]));
				}

				return rect;

			}

			// If this token does not contain the position for which to get
			// the bounding box...
			else {
				s.array = token.text;
				s.offset = token.textOffset;
				s.count = token.textCount;
				stableX += Utilities.getTabbedTextWidth(s, fm, stableX, e,
						token.getOffset());
			}

			token = (TokenImpl)token.getNextToken();

		}

		// If we didn't find anything, we're at the end of the line. Return
		// a width of 1 (so selection highlights don't extend way past line's
		// text). A ConfigurableCaret will know to paint itself with a larger
		// width.
		SwingUtils.setX(rect, stableX);
		SwingUtils.setWidth(rect, 1);
		return rect;

	}


	/**
	 * Makes this token start at the specified offset into the document.<p>
	 *
	 * <b>Note:</b> You should not modify <code>Token</code> instances you
	 * did not create yourself (e.g., came from an
	 * <code>RSyntaxDocument</code>).  If you do, rendering issues and/or
	 * runtime exceptions will likely occur.  You have been warned!
	 *
	 * @param pos The offset into the document this token should start at.
	 *        Note that this token must already contain this position; if
	 *        it doesn't, an exception is thrown.
	 * @throws IllegalArgumentException If pos is not already contained by
	 *         this token.
	 * @see #moveOffset(int)
	 */
	public void makeStartAt(int pos) {
		if (pos<getOffset() || pos>=(getOffset()+textCount)) {
			throw new IllegalArgumentException("pos " + pos +
				" is not in range " + getOffset() + "-" + (getOffset()+textCount-1));
		}
		int shift = pos - getOffset();
		setOffset(pos);
		textOffset += shift;
		textCount -= shift;
	}


	/**
	 * Moves the starting offset of this token.<p>
	 *
	 * <b>Note:</b> You should not modify <code>Token</code> instances you
	 * did not create yourself (e.g., came from an
	 * <code>RSyntaxDocument</code>).  If you do, rendering issues and/or
	 * runtime exceptions will likely occur.  You have been warned!
	 *
	 * @param amt The amount to move the starting offset.  This should be
	 *        between <code>0</code> and <code>textCount</code>, inclusive.
	 * @throws IllegalArgumentException If <code>amt</code> is an invalid value.
	 * @see #makeStartAt(int)
	 */
	public void moveOffset(int amt) {
		if (amt<0 || amt>textCount) {
			throw new IllegalArgumentException("amt " + amt +
					" is not in range 0-" + textCount);
		}
		setOffset(getOffset() + amt);
		textOffset += amt;
		textCount -= amt;
	}


	/**
	 * Sets the value of this token to a particular segment of a document.
	 * The "next token" value is cleared.
	 *
	 * @param line The segment from which to get the token.
	 * @param beg The first character's position in <code>line</code>.
	 * @param end The last character's position in <code>line</code>.
	 * @param offset The offset into the document at which this token begins.
	 * @param type A token type listed as "generic" above.
	 */
	public void set(final char[] line, final int beg, final int end,
							final int offset, final int type) {
		this.text = line;
		this.textOffset = beg;
		this.textCount = end - beg + 1;
		this.setType(type);
		this.setOffset(offset);
		nextToken = null;
	}


	/**
	 * Sets whether this token is a hyperlink.
	 *
	 * @param hyperlink Whether this token is a hyperlink.
	 * @see #isHyperlink()
	 */
	@Override
	public void setHyperlink(boolean hyperlink) {
		this.hyperlink = hyperlink;
	}


	/**
	 * Sets the language index for this token.  If this value is positive, it
	 * denotes a specific "secondary" language this token represents (such as
	 * JavaScript code or CSS embedded in an HTML file).  If this value is
	 * <code>0</code>, this token is in the "main" language being edited.
	 * Negative values are invalid and treated as <code>0</code>.
	 *
	 * @param languageIndex The new language index.  A value of
	 *        <code>0</code> denotes the "main" language, any positive value
	 *        denotes a specific secondary language.  Negative values will
	 *        be treated as <code>0</code>.
	 * @see #getLanguageIndex()
	 */
	@Override
	public void setLanguageIndex(int languageIndex) {
		this.languageIndex = languageIndex;
	}


	/**
	 * Sets the "next token" pointer of this token to point to the specified
	 * token.
	 *
	 * @param nextToken The new next token.
	 * @see #getNextToken()
	 */
	public void setNextToken(Token nextToken) {
		this.nextToken = nextToken;
	}


	/**
	 * Sets the offset into the document at which this token resides.
	 *
	 * @param offset The new offset into the document.
	 * @see #getOffset()
	 */
	public void setOffset(int offset) {
		this.offset = offset;
	}


	@Override
	public void setType(int type) {
		this.type = type;
	}


	@Override
	public boolean startsWith(char[] chars) {
		if (chars.length<=textCount){
			for (int i=0; i<chars.length; i++) {
				if (text[textOffset+i]!=chars[i]) {
					return false;
				}
			}
			return true;
		}
		return false;
	}


	@Override
	public int tokenToDocument(int pos) {
		return pos + (getOffset()-textOffset);
	}


	/**
	 * Returns this token as a <code>String</code>, which is useful for
	 * debugging.
	 *
	 * @return A string describing this token.
	 */
	@Override
	public String toString() {
		return "[Token: " +
			(getType()==Token.NULL ? "<null token>" :
				"text: '" +
					(text==null ? "<null>" : getLexeme() + "'; " +
	       		"offset: " + getOffset() + "; type: " + getType() + "; " +
		   		"isPaintable: " + isPaintable() +
		   		"; nextToken==null: " + (nextToken==null))) +
		   "]";
	}


}
