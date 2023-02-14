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
	int listOffsetChunkSize = Integer.valueOf(System.getProperty("chunkSize", "-1")); // Disabled; yields offset errors unless token is first on line (?)
	private FontMetrics fm;
	private char currChar;

	public BufferedTokenViewModelConverter(RSyntaxTextArea textArea, TabExpander e) {
		super(textArea, e);
	}

	@Override
	protected int getTokenListOffset() {
		this.fm = textArea.getFontMetricsForTokenType(token.getType());

		// loop over text in token
		int begin = token.textOffset;
		int end = begin + token.textCount;
		for (int i = begin; i < end; i++) {
			currX = nextX;
			currChar = text[i];

			// TAB?
			if (currChar == '\t') {
				int offset = tabCharacterFound(i);
				if (offset != UNDEFINED) {
					return offset;
				}
			}

			// CHUNK LIMIT?
			else if (listOffsetChunkSize>0 && charCount>0 && charCount % listOffsetChunkSize == 0) {
				// TODO FIX! calculation yields false offset
				int offset = processChunk("Chunksize", i);
				if (offset != UNDEFINED) {
					return offset;
				}
			}

			// REGULAR CHARACTER
			else {
				charCount++;
			}
		}

		// process remaining text after last tab or chunk (if any)
		if (charCount > 0) {
			int offset = processChunk("Tail", end);
			if (offset != UNDEFINED) {
				return offset;
			}
		}
		return UNDEFINED;
	}

	private int processChunk(String logMessage, int chunkEnd) {
		// check chunk (improves performance by reducing max length of string to measure width for)
		int begin = chunkEnd - charCount;
		float charsWidth = chunkWidth(begin);
		nextX = stableX + charsWidth;
		stableX = nextX; // Cache ending x-coord. of chunk.

		// x inside chunk?
		if (x >= currX && x < nextX) {
			return getOffsetFromChunk(logMessage, begin);
		}
		currX = nextX;
		charCount = 0;
		return UNDEFINED;
	}

	private int tabCharacterFound(int i) {
		// add width of characters before the tab and reset counter
		if (charCount>0) {
			int chunkOffset = processChunk("Before tab", i);
			if (chunkOffset!=UNDEFINED) {
				return chunkOffset;
			}
		}

		// tabstop
		nextX = tabExpander.nextTabStop(stableX, 0);
		stableX = nextX; // Cache ending x-coord. of tab.

		// x in tab?
		if (x >= currX && x < nextX) {
			int tabOffset = last + i - token.textOffset;
			boolean beforeMiddleOfTab = x - currX < nextX - x;
			int result = beforeMiddleOfTab ? tabOffset : tabOffset+1;
			logConversion(currChar, result);
			return result;
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
	private int getOffsetFromChunk(String logMessage, int begin) {

		// in chunk
		int xOffsetInChunk = getListOffset(fm, token.text, begin, begin, charCount, currX, x);

		// in token
		int xOffsetInToken = xOffsetInChunk - token.textOffset;

		// in document
		int tokenOffsetInDocument = token.getOffset();
		int result = tokenOffsetInDocument + xOffsetInToken;

		logConversion(logMessage, xOffsetInChunk, xOffsetInToken, result);
		assert result>=0 && result<=token.getEndOffset() : "Invalid result. Is x inside text segment?";
		return result;
	}

}
