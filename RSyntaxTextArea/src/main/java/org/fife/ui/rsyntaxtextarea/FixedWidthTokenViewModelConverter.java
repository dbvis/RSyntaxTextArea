package org.fife.ui.rsyntaxtextarea;

import org.fife.util.SwingUtils;

import java.awt.*;

/**
 * An implementation that buffers consecutive text and postpones the computation as long as possible and then
 * calculates the offset using a relative approach, essentially <code>x / textWidth * textSize</code>.
 * <p/>
 * <b>NOTE!</b> this approach only makes sense with monospace fonts, ie where all character have the same width!
 * If any "wide" characters (eg Kanji) are encountered, the text buffer is processed and the "wide" character
 * measured before resuming operation. Alas, the performance gain will be essentially lost if the text holds many
 * such characters.
 */
public class FixedWidthTokenViewModelConverter extends AbstractTokenViewModelConverter {

	private final int tabSize;
	private final float charWidth;
	private final float tabWidth;
	private final FontMetrics fm;
	private char currChar;

	FixedWidthTokenViewModelConverter(RSyntaxTextArea textArea, FontMetrics fm) {
		super(textArea, null);
		this.fm = fm;
		this.tabSize = textArea.getTabSize();
		this.charWidth = SwingUtils.charWidth(fm, ' ');
		this.tabWidth = charWidth * tabSize;
	}

	@Override
	protected int getTokenListOffset() {
		// loop over text in token
		int begin = token.textOffset;
		int end = begin + token.textCount;
		for (int i = begin; i < end; i++) {
			currX = nextX;
			currChar = text[i];

			// TAB?
			if (currChar == '\t') {
				int offset = tabCharacterFound(i);
				if (offset!=UNDEFINED) {
					return offset;
				}

			// WIDE (eg Kanji)?
			} else if (isWideCharacter(currChar)) {
				int offset = wideCharacterFound(i);
				if (offset!=UNDEFINED) {
					return offset;
				}

			// REGULAR CHARACTER
			} else {
				charCount++;
			}
		}

		// process chunk remaining after last breakpoint (if any)
		if (charCount > 0) {
			float width = chunkWidth();
			float lastX = nextX + width;

			// x inside text?
			if (x<=lastX) {
				return getOffsetFromChunk("Tail", token.textCount, width);
			} else {
				nextX += width; // add width and continue to next token
			}
		}

		// no hit
		return UNDEFINED;
	}

	private int wideCharacterFound(int i) {
		// x in buffered chunk?
		int chunkOffset = processChunk("Chunk before wide character", i);
		if (chunkOffset!=UNDEFINED) {
			return chunkOffset;
		}

		// do regular calculation
		float charWidth = SwingUtils.charWidth(fm, currChar);
		nextX += charWidth;
		charCount = 0;

		// done?
		return x >= currX && x < nextX ? toOffsetInDocument(i) : UNDEFINED;
	}

	private int tabCharacterFound(int i) {
		// x in buffered chunk?
		int chunkOffset = processChunk("Chunk before tab character", i);
		if (chunkOffset!=UNDEFINED) {
			return chunkOffset;
		}

		// calculate tabstop
		int nofTabs = (int) (currX / tabWidth);
		int nextTab = nofTabs + 1;
		float nextTabX = nextTab * tabWidth;
		nextX = nextTabX;
		stableX = nextX; // Cache ending x-coord. of tab.

		// done?
		return x >= currX && x < nextX ? toOffsetInDocument(i) : UNDEFINED;
	}

	/**
	 * Convert the supplied offset in the token text to the corresponding offset in the document.
	 *
 	 * @param offsetInToken the position of the current character ({@link #currChar}) in the token text array
	 * @return offset in document
	 */
	private int toOffsetInDocument(int offsetInToken) {
		int offsetInDocument = last + offsetInToken - token.textOffset;
		boolean xBeforeMiddle = x - currX < nextX - x;
		int result = xBeforeMiddle ? offsetInDocument : offsetInDocument + 1;
		logConversion(currChar, result);
		return result;
	}


	/**
	 * Check the text chunk since last breakpoint. If x is inside chunk, return the offset, else {@link #UNDEFINED}.
	 * @param debugInfo for the log
	 * @param chunkEnd	where in the text array the text chunk ends (exclusive)
	 * @return offset or {@link #UNDEFINED}
	 */
	private int processChunk(String debugInfo, int chunkEnd) {
		if (charCount > 0) {
			float charsWidth = charCount > 0 ? chunkWidth() : 0;
			nextX = currX + charsWidth;

			// done?
			if (x < nextX) {
				return getOffsetFromChunk(debugInfo, chunkEnd, charsWidth);
			}
			currX = nextX;
			charCount = 0;
		}

		// Not found
		return UNDEFINED;
	}

	/**
	 * Get the width of the text chunk. Since all characters are the same width, we can simply multiply.
	 * @return the number of pixels the text occupies
	 */
	private float chunkWidth() {
		return charCount * charWidth;
	}

	/**
	 * Get the offset from the current text chunk. {@link #x} must already be verified to be inside the chunk.
	 *
	 * @param info	for debug
	 * @param chunkEnd	where the text chunk ends
	 * @param chunkWidth	the number of pixels the chunk occupies
	 * @return offset non-negative integer
	 */
	private int getOffsetFromChunk(String info, int chunkEnd, float chunkWidth) {
		// in chunk
		float xInChunk = x - currX;
		float relativeX = xInChunk / chunkWidth;
		int xOffsetInChunk = Math.round(charCount * relativeX);

		// in token
		int chunkOffsetInToken = chunkEnd - charCount;
		int xOffsetInToken = chunkOffsetInToken + xOffsetInChunk;

		// in document
		int result = token.getOffset() + xOffsetInToken;
		logConversion(info, xOffsetInChunk, xOffsetInToken, result);
		return result;
	}

}
