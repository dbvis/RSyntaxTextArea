package org.fife.ui.rsyntaxtextarea;

import org.fife.util.SwingUtils;

import javax.swing.text.TabExpander;
import java.awt.*;
import java.util.logging.Logger;

/**
 * An implementation that buffers consecutive text and postpones the computation as long as possible and then
 * calculates the offset using a "divide and conquer" approach where the text block is recursively divided in halves
 * and compared to the x coordinates until the exact offset is found.
 */
class BufferedTokenViewModelConverter extends AbstractTokenViewModelConverter {

	private static final Logger LOG = Logger.getLogger(BufferedTokenViewModelConverter.class.getName());

	/** Max size of text strings to process when converting x coordinate to model offset. */
	int listOffsetChunkSize = -1; // 1000; // Disabled; yields offset errors unless token is first on line (?)

	public BufferedTokenViewModelConverter(RSyntaxTextArea textArea, TabExpander e) {
		super(textArea, e);
	}

	@Override
	public int getListOffset(TokenImpl tokenList, float x0, float x) {
		float currX = x0; // x-coordinate of current char.
		float nextX = x0; // x-coordinate of next char.
		float stableX = x0; // Cached ending x-coord. of last tab or token.
		TokenImpl token = tokenList;
		int last = token.getOffset();

		// loop over tokens
		long started = System.currentTimeMillis();
		while (token != null && token.isPaintable()) {

			FontMetrics fm = textArea.getFontMetricsForTokenType(token.getType());
			char[] text = token.text;
			int start = token.textOffset;
			int end = start + token.textCount;
			int charCount = 0;

			// loop over text in token
			for (int i = start; i < end; i++) {
				currX = nextX;

				// TAB?
				if (text[i] == '\t') {

					// add width of characters before the tab and reset counter
					if (charCount>0) {
						int begin = i - charCount;
						float charsWidth = SwingUtils.charsWidth(fm, text, begin, charCount);
						nextX = stableX + charsWidth;
						// x inside chunk?
						if (x < nextX) {
							return getListOffset(started, "Chunk before Tab", token, fm, begin, charCount, stableX, x);
						}
						currX = nextX;
						charCount = 0;
					}

					// tabstop
					nextX = tabExpander.nextTabStop(nextX, 0);
					stableX = nextX; // Cache ending x-coord. of tab.

					// done?
					if (x >= currX && x < nextX) {
						int tabOffset = last + i - token.textOffset;
						int result = x-currX < nextX-x ? tabOffset : tabOffset+1;
						LOG.fine(()->String.format("%,d ms: Found in tab: x=%.3f => offset=%,d",
							System.currentTimeMillis()-started, x, result));
						return result;
					}

				}

				// CHUNK LIMIT?
				else if (listOffsetChunkSize>0 && charCount>0 && charCount % listOffsetChunkSize == 0) {
					// TODO FIX! calculation yields false offset
					// check chunk (improves performance by reducing max length of string to measure width for)
					int begin = i - charCount;
					float charsWidth = SwingUtils.charsWidth(fm, text, begin, charCount);
					nextX += charsWidth;

					// x inside chunk?
					if (x < nextX) {
						return getListOffset(started, "Chunk", token, fm, begin, charCount, stableX, x);
					}
					charCount = 0;
					stableX = nextX; // Cache ending x-coord. of chunk.

				// REGULAR CHARACTER
				} else {
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
					return getListOffset(started, "Tail", token, fm, begin, charCount, stableX, x);
				} else {
					nextX += width; // add width and continue to next token
				}
			}

			// nope - next token
			stableX = nextX; // Cache ending x-coordinate of token.
			last += token.textCount;
			token = (TokenImpl) token.getNextToken();
		}

		// If we didn't find anything, return the end position of the text.
		return last;

	}

	/**
	 * Find the offset in the specified text segment. The x coordinate must already be verified to match the segment.
	 *
	 * @param started   for reporting elapsed time (debug)
	 * @param logMessage where did we exit? (debug)
	 * @param token     the token holding the text
	 * @param fm        metric for the font
	 * @param begin     where to start looking in the text
	 * @param charCount how many characters we should look for
	 * @param x0        initial coordinate of the text
	 * @param x         the coordinate to map
	 * @return offset for the corresponding character in the documetn
	 */
	private int getListOffset(long started, String logMessage, TokenImpl token, FontMetrics fm, int begin, int charCount, float x0, float x) {
		int xOffsetInText = getListOffset(fm, token.text, begin, begin, charCount, x0, x);

		int xOffsetInToken = xOffsetInText - token.textOffset;
		int tokenOffsetInDocument = token.getOffset();
		int result = tokenOffsetInDocument + xOffsetInToken;

		LOG.fine(() -> debugListOffset(logMessage, started, textArea, token.text, x, tokenOffsetInDocument,
			xOffsetInText, xOffsetInToken, result, token.textCount));
		assert result>=0 && result<=token.getEndOffset() : "Invalid result. Is x inside text segment?";
		return result;
	}

}
