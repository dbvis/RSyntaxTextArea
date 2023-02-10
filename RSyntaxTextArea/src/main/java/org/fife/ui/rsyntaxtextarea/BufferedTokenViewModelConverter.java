package org.fife.ui.rsyntaxtextarea;

import org.fife.util.SwingUtils;

import javax.swing.text.TabExpander;
import java.awt.*;

/**
 * An implementation that buffers consecutive text in "chunks" and postpones the computation as long as possible and
 * then calculates the offset using a "divide and conquer" approach where the text chunk is recursively divided in
 * halves and compared to the x coordinates until the exact offset is found.
 */
class BufferedTokenViewModelConverter extends AbstractTokenViewModelConverter {

	/**
	 * Max size of text strings to process when converting x coordinate to model offset.
	 */
	int listOffsetChunkSize = -1; // 1000; // Disabled; yields offset errors unless token is first on line (?)
	private FontMetrics fm;

	public BufferedTokenViewModelConverter(RSyntaxTextArea textArea, TabExpander e) {
		super(textArea, e);
	}

	@Override
	protected int getTokenListOffset() {
		this.fm = textArea.getFontMetricsForTokenType(token.getType());

		// loop over text in token
		for (int i = start; i < end; i++) {
			currX = nextX;
			char currChar = text[i];

			// TAB?
			if (currChar == '\t') {

				// add width of characters before the tab and reset counter
				if (charCount>0) {
					int begin = i - charCount;
					float charsWidth = chunkWidth(begin);
					nextX = stableX + charsWidth;
					// x inside chunk?
					if (x < nextX) {
						return getChunkOffset("Chunk before Tab", begin);
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
					logConversion(currChar, result);
					return result;
				}

			}

			// CHUNK LIMIT?
			else if (listOffsetChunkSize>0 && charCount>0 && charCount % listOffsetChunkSize == 0) {
				// TODO FIX! calculation yields false offset
				// check chunk (improves performance by reducing max length of string to measure width for)
				int begin = i - charCount;
				float charsWidth = chunkWidth(begin);
				nextX += charsWidth;

				// x inside chunk?
				if (x < nextX) {
					return getChunkOffset("Chunk", begin);
				}
				charCount = 0;
				stableX = nextX; // Cache ending x-coord. of chunk.

			} else {
				// REGULAR CHARACTER
				charCount++;
			}
		}

		// process remaining text after last tab or chunk (if any)
		if (charCount > 0) {
			int begin = end - charCount;
			float width = chunkWidth(begin);
			float lastX = nextX + width;
			// x inside text?
			if (x<=lastX) {
				return getChunkOffset("Tail", begin);
			} else {
				nextX += width; // add width and continue to next token
			}
		}
		return UNDEFINED;
	}

	/**
	 * Get the width of the text chunk starting at the specified offset.
	 * @param begin where to start measuring
	 * @return the number of pixels the text occupies
	 */
	private float chunkWidth(int begin) {
		return SwingUtils.charsWidth(fm, text, begin, charCount);
	}

	/**
	 * Find the offset in the specified chunk. The x coordinate must already be verified to match the segment.
	 *
	 * @param logMessage where did we exit? (debug)
	 * @param begin      where to start looking in the text
	 * @return offset for the corresponding character in the document
	 */
	private int getChunkOffset(String logMessage, int begin) {
		int xOffsetInChunk = getListOffset(fm, token.text, begin, begin, charCount, x0, x);

		int xOffsetInToken = xOffsetInChunk - token.textOffset;
		int tokenOffsetInDocument = token.getOffset();
		int result = tokenOffsetInDocument + xOffsetInToken;

		logConversion(logMessage, xOffsetInChunk, xOffsetInToken, result);
		assert result>=0 && result<=token.getEndOffset() : "Invalid result. Is x inside text segment?";
		return result;
	}

}
