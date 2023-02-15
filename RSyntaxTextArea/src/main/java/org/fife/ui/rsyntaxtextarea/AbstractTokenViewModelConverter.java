package org.fife.ui.rsyntaxtextarea;

import org.fife.util.SwingUtils;

import javax.swing.text.Segment;
import javax.swing.text.TabExpander;
import javax.swing.text.Utilities;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An abstraction of features for converting x-coordinates in the view to the corresponding offset in the text model,
 * extracted from {@link TokenImpl#getListOffset(RSyntaxTextArea, TabExpander, float, float)} to separate concerns.
 * The main design objective is to improve performance when converting very long (+100k) strings.
 * <p/>
 * @implNote this class is <b>not</b> thread-safe; {@link #getListOffset(TokenImpl, float, float)} stores loop variables
 * as fields. The instance must not be kept and reused, but should be instantiated on each invocation.
 */
public abstract class AbstractTokenViewModelConverter implements TokenViewModelConverter {
	private static final Logger LOG = Logger.getLogger(AbstractTokenViewModelConverter.class.getName());

	protected static final int UNDEFINED = -1; // undefined offset

	protected final RSyntaxTextArea textArea;
	protected final TabExpander tabExpander;

	protected TokenImpl token;	// the current token being analyzed
	protected char[] text;		// text of current token

	// view to model (getListOffset())
	protected float x;			// the x-coordinate to convert to an offset
	protected float currX;  	// x-coordinate of the character or text chunk currently analyzed
	protected float nextX;  	// x-coordinate of next char.
	protected float stableX; 	// Cached ending x-coordinate of analyzed text

	protected int last; 		// offset of most recent token
	protected int charCount;	// size of current text chunk

	protected long started;  	// for logging elapsed time

	// model to view (listOffsetToView())
	protected int pos;			// the model offset
	protected Rectangle2D rect;	// for returning results (reused to minimize memory allocations)

	protected AbstractTokenViewModelConverter(RSyntaxTextArea textArea, TabExpander e) {
		this.textArea = textArea;
		this.tabExpander = e;
	}

	@Override
	public int getListOffset(TokenImpl tokenList, float x0, float x) {
		this.x=x;
		currX = x0;
		nextX = x0;
		stableX = x0;
		token = tokenList;
		last = token.getOffset();

		// loop over tokens
		started = System.currentTimeMillis();
		while (token != null && token.isPaintable()) {

			// setup token data
			text = token.text;
			charCount = 0;

			// process token
			int result = getTokenListOffset();
			if (result != UNDEFINED) {
				return result;
			}

			// no match in token - continue to next
			stableX = nextX; // Cache ending x-coordinate of token.
			last += token.textCount;
			token = (TokenImpl) token.getNextToken();
		}

		// If we didn't find anything, return the end position of the text.
		LOG.fine(()->"EOL");
		return last;
	}

	protected abstract int getTokenListOffset();

	@Override
	public Rectangle2D listOffsetToView(TokenImpl mainToken, TabExpander e, int pos, float x0, Rectangle2D originalRectangle) {
		this.token = mainToken;
		this.stableX = x0; // Cached ending x-coord. of last tab or token.
		this.pos = pos;
		this.rect = originalRectangle;

		// loop over tokens
		started = System.currentTimeMillis();
		while (token != null && token.isPaintable()) {
			Rectangle2D result = tokenListOffsetToView();
			if (result != null) {
				return result;
			}
			token = (TokenImpl) token.getNextToken();
		}

		// If we didn't find anything, we're at the end of the line.
		// Return a width of 1 (so selection highlights don't extend way past line's text).
		// Don't return null as things will error.
		// A ConfigurableCaret will know to paint itself with a larger width.
		SwingUtils.setX(rect, stableX);
		SwingUtils.setWidth(rect, 1);
		return rect;
	}

	/**
	 * Place-holder to let subclasses override as necessary.
	 * The default implementation is identical to the default implementation in {@link DefaultTokenViewModelConverter}.
	 *
	 * @return The position (in the document, NOT into the token list!) that covers the pixel location.
	 * @see DefaultTokenViewModelConverter#listOffsetToView(TokenImpl, TabExpander, int, float, Rectangle2D)
	 */
	protected Rectangle2D tokenListOffsetToView() {
		Segment s = new Segment();

		while (token != null && token.isPaintable()) {

			FontMetrics fm = textArea.getFontMetricsForTokenType(token.getType());
			if (fm == null) {
				return rect; // Don't return null as things will error.
			}
			char[] text = token.text;
			int start = token.textOffset;
			int end = start + token.textCount;

			// If this token contains the position for which to get the bounding box...
			if (token.containsPosition(pos)) {

				s.array = token.text;
				s.offset = token.textOffset;
				s.count = pos - token.getOffset();

				// Must use this (actually fm.charWidth()), and not
				// fm.charsWidth() for returned value to match up with where
				// text is actually painted on OS X!
				float w = Utilities.getTabbedTextWidth(s, fm, stableX, tabExpander, token.getOffset());
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

			// If this token does not contain the position for which to get the bounding box...
			else {
				s.array = token.text;
				s.offset = token.textOffset;
				s.count = token.textCount;
				stableX += Utilities.getTabbedTextWidth(s, fm, stableX, tabExpander,
					token.getOffset());
			}

			token = (TokenImpl)token.getNextToken();

		}

		// If we didn't find anything, we're at the end of the line.
		// Return a width of 1 (so selection highlights don't extend way past line's text).
		// A ConfigurableCaret will know to paint itself with a larger width.
		SwingUtils.setX(rect, stableX);
		SwingUtils.setWidth(rect, 1);
		return rect;
	}

	/**
	 * Determines the offset into the supplied text block that covers pixel location <code>x</code> using a faster
	 * algorithm that doesn't check the width of each character, presuming that the text block does not contain any tab
	 * characters (<code>'\t'</code>).
	 * <p/>
	 * Designed for {@link TokenImpl#getListOffset(RSyntaxTextArea, TabExpander, float, float)} to improve performance
	 * on very long text strings (eg hex dumps of images or single line JSON documents).
	 *
	 * @param fm    FontMetrics for the token font
	 * @param chars the array of text to process
	 * @param off0  index of first character of the text block in the array
	 * @param off   index of first character in the array fragment to process in this run
	 * @param len   number of characters to process
	 * @param x0    The pixel x-location that is the beginning of the text segment
	 * @param x     The pixel-position for which you want to get the corresponding offset.
	 * @return the offset in the text corresponding to pixel x
	 */
	protected static int getListOffset(FontMetrics fm, char[]  chars, int off0, int off, int len, float x0, float x) {
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
		float xMid = x0 + charsWidth(fm, chars, off0, zeroToHalfLen);
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

	protected static float charWidth(FontMetrics fm, char currChar) {
		return charsWidth(fm, new char[]{currChar}, 0, 1);
	}

	protected static float charsWidth(FontMetrics fm, char[] chars, int begin, int count) {
		float fw = SwingUtils.charsWidth(fm, chars, begin, count);
		assert fw==doubleCharsWidth(fm, chars, begin, count) : "float value doesn't match double value";
		return fw;
	}

	private static double doubleCharsWidth(FontMetrics fm, char[] chars, int begin, int count) {

		FontRenderContext frc = fm.getFontRenderContext();
		Font font = fm.getFont();
		int limit = begin + count;
		Rectangle2D bounds = font.getStringBounds(chars, begin, limit, frc);
		double dw = bounds.getWidth();
		return dw;
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


	/**
	 * Does the supplied character occupy a single character space or is it wider than the latin characters
	 * also in a monospace font? For performance reasons, the method checks {@link java.lang.Character.UnicodeScript}
	 * rather than the actual width. See {@link Font} and the internal method
	 * <code>sun.font.FontUtilities.isComplexCharCode(int code)</code>
	 *
	 * @param c the character to check
	 * @return boolean
	 */
	protected static boolean isWideCharacter(char c) {
		Character.UnicodeScript script = Character.UnicodeScript.of(c);

		// TODO improve - is there a robust approach to detect wide characters without calling FontMetrics?
		switch (script) {
			case CYRILLIC:
			case GREEK:
			case LATIN:
				return false;
			case COMMON:
				return (int)c>255;

			default:
				LOG.finest(()->String.format("'%c' wide (%s), value=%,d%n", c, script, (int)c));
				return true;
		}
	}

	protected void logConversion(char foundInCharacter, int resultingOffset) {
		String escaped = foundInCharacter=='\t' ? "\\t" : String.valueOf(foundInCharacter);
		LOG.fine(() -> String.format("[%,d ms] x=%.0f found in character '%s' => offset=%,d",
			System.currentTimeMillis() - started, x, escaped, resultingOffset));
	}


	protected void logConversion(String info, int offsetInChunk, int offsetInToken, int offsetInDocument) {
		Level level = Level.FINE;
		if (LOG.isLoggable(level)) {
			long elapsed = System.currentTimeMillis() - started;

			String docText = textArea.getText();
			int length = docText.length();
			String docCharacter = offsetInDocument < length
				? docText.substring(offsetInDocument, offsetInDocument + 1)
				: null;
			String tokenString = token.textCount<10 ? token.getLexeme() : token.getLexeme().substring(0, 10)+"...";

			String logMessage = String.format(
				"[%,d ms] %s: Total text length: %,d | Token [%s]: offset=%,d, count=%,d | x=%.3f | " +
					"offsetInChunk=%,d | offsetInToken=%,d => offsetInDocument=%,d ('%s')",
				elapsed, info, length, tokenString, token.getOffset(), token.textCount, x,
				offsetInChunk, offsetInToken, offsetInDocument, docCharacter);
			LOG.log(level, logMessage);
		}
	}

}
