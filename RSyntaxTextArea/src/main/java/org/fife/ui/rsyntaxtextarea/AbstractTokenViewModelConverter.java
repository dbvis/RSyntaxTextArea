package org.fife.ui.rsyntaxtextarea;

import org.fife.util.SwingUtils;

import javax.swing.text.TabExpander;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An abstraction of features for converting x-coordinates in the view to the corresponding offset in the text model,
 * extracted from {@link TokenImpl#getListOffset(RSyntaxTextArea, TabExpander, float, float)} to separate concerns.
 * The main design objective is to improve performance when converting very long (+100k) strings.
 */
public abstract class AbstractTokenViewModelConverter implements TokenViewModelConverter {
	private static final Logger LOG = Logger.getLogger(AbstractTokenViewModelConverter.class.getName());

	protected final RSyntaxTextArea textArea;
	protected final TabExpander tabExpander;

	protected TokenImpl token;
	protected float x0;
	protected float x;

	protected AbstractTokenViewModelConverter(RSyntaxTextArea textArea, TabExpander e) {
		this.textArea = textArea;
		this.tabExpander = e;
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
	 * @param off0 index of first character of the text block in the array
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


	/**
	 * Does the supplied character occupy a single character space or is it wider than the latin characters
	 * also in a monospace font? For performance reasons, the method checks {@link java.lang.Character.UnicodeScript}
	 * rather than the actual width. See {@link Font} and the internal method
	 * <code>sun.font.FontUtilities.isComplexCharCode(int code)</code>
	 * @param c the character to check
	 * @return boolean
	 */
	protected boolean isWideCharacter(char c) {
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

	protected static void logConversion(long started, float x, char foundInCharacter, int resultingOffset) {
		String escaped = foundInCharacter=='\t' ? "\\t" : String.valueOf(foundInCharacter);
		LOG.fine(() -> String.format("[%,d ms] x=%.0f found in character '%s' => offset=%,d",
			System.currentTimeMillis() - started, x, escaped, resultingOffset));
	}


	protected static void logConversion(String info, long started, RSyntaxTextArea textArea, TokenImpl token, float x,
										int offsetInChunk, int offsetInToken, int offsetInDocument) {

		if (LOG.isLoggable(Level.FINE)) {
			long elapsed = System.currentTimeMillis() - started;

			String docText = textArea.getText();
			int length = docText.length();
			String character = offsetInChunk < token.textCount
				? new String(token.text, offsetInChunk, 1)
				: null;
			String substring = offsetInDocument < length
				? docText.substring(offsetInDocument, offsetInDocument + 1)
				: null;

			String logMessage = String.format(
				"[%,d ms] %s: Total text length: %,d | Token Offset=%,d | Token Count=%,d | x=%.3f | " +
					"offsetInChunk=%,d ('%s') | offsetInToken=%,d => offsetInDocument=%,d ('%s') %n",
				elapsed, info, length, token.getOffset(), token.textCount, x,
				offsetInChunk, character, offsetInToken, offsetInDocument, substring);
			LOG.fine(logMessage);
		}
	}

}
