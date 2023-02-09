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
		int last = tokenList.getOffset();

		// loop over tokens
		long started = System.currentTimeMillis();
		while (tokenList != null && tokenList.isPaintable()) {

			FontMetrics fm = textArea.getFontMetricsForTokenType(tokenList.getType());
			char[] text = tokenList.text;
			int start = tokenList.textOffset;
			int end = start + tokenList.textCount;
			int charCount = 0;

			// loop over text in token
			for (int i = start; i < end; i++) {
				currX = nextX;
				if (text[i] == '\t') {

					// add width of characters before the tab and reset counter
					if (charCount>0) {
						int begin = i - charCount;
						float charsWidth = SwingUtils.charsWidth(fm, text, begin, charCount);
						nextX = stableX + charsWidth;
						// x inside chunk?
						if (x < nextX) {
							int xOffsetInText = getListOffset(fm, text, begin, begin, charCount, stableX, x);
							int xOffsetInToken = xOffsetInText - tokenList.textOffset;
							int tokenOffsetInDocument = tokenList.getOffset();
							int xOffsetInDocument = tokenOffsetInDocument + xOffsetInToken;
							int textCount = tokenList.textCount;
							LOG.fine(() -> debugListOffset("Proportional Chunk Before Tab",
								started, textArea, text, x, tokenOffsetInDocument, xOffsetInText, xOffsetInToken, xOffsetInDocument, textCount));
							return xOffsetInDocument;

						}
						currX = nextX;
						charCount = 0;
					}

					// tabstop
					nextX = tabExpander.nextTabStop(nextX, 0);
					stableX = nextX; // Cache ending x-coord. of tab.

					// done?
					if (x >= currX && x < nextX) {
						int tabOffset = last + i - tokenList.textOffset;
						int result = x-currX < nextX-x ? tabOffset : tabOffset+1;
						LOG.fine(()->String.format("%,d ms: Found in tab: x=%.3f => offset=%,d",
							System.currentTimeMillis()-started, x, result));
						return result;
					}

				} else if (listOffsetChunkSize>0 && charCount>0 && charCount % listOffsetChunkSize == 0) {
					// TODO FIX! calculation yields false offset
					// check chunk (improves performance by reducing max length of string to measure width for)
					int begin = i - charCount;
					float charsWidth = SwingUtils.charsWidth(fm, text, begin, charCount);
					nextX += charsWidth;

					// x inside chunk?
					if (x < nextX) {
						int xOffsetInText = getListOffset(fm, text, begin, begin, charCount, stableX, x);
						int xOffsetInToken = xOffsetInText - tokenList.textOffset;
						int tokenOffsetInDocument = tokenList.getOffset();
						int xOffsetInDocument = tokenOffsetInDocument + xOffsetInToken;
						int tokenTextCount = tokenList.textCount;
						LOG.fine(() -> debugListOffset("Proportional Chunk",
							started, textArea, text, x, tokenOffsetInDocument, xOffsetInText, xOffsetInToken, xOffsetInDocument, tokenTextCount));
						return xOffsetInDocument;
					}

					// x beyond end of chunk
					charCount = 0;
					stableX = nextX; // Cache ending x-coord. of chunk.

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
					int xOffsetInToken = xOffsetInText - tokenList.textOffset;
					int tokenOffsetInDocument = tokenList.getOffset();
					int xOffsetInDocument = tokenOffsetInDocument + xOffsetInToken;
					int textCount = tokenList.textCount;
					LOG.fine(() -> debugListOffset("Proportional Tail",
						started, textArea, text, x, tokenOffsetInDocument, xOffsetInText, xOffsetInToken, xOffsetInDocument, textCount));
					return xOffsetInDocument;
				} else {
					nextX += width; // add width and continue to next token
				}
			}

			stableX = nextX; // Cache ending x-coordinate of token.
			last += tokenList.textCount;
			tokenList = (TokenImpl) tokenList.getNextToken();

		}

		// If we didn't find anything, return the end position of the text.
		return last;

	}

}
