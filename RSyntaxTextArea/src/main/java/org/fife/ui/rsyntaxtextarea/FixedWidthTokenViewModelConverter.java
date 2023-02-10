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
class FixedWidthTokenViewModelConverter extends AbstractTokenViewModelConverter {

	private static final int UNDEFINED = -1;
	private final int tabSize;
	private final float charWidth;
	private final float tabWidth;
	private final FontMetrics fm;

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
		for (int i = start; i < end; i++) {
			currX = nextX;
			char currChar = text[i];

			// TAB?
			if (currChar == '\t') {
				int result = tabCharacterFound(i, currChar);
				if (result!=UNDEFINED) {
					return result;
				}

			// WIDE (eg Kanji)?
			} else if (isWideCharacter(currChar)) {
				int result = wideCharacterFound(i, currChar);
				if (result!=UNDEFINED) {
					return result;
				}

			// REGULAR CHARACTER
			} else {
				charCount++;
			}
		}

		// process remaining text after last breakpoint (if any)
		if (charCount > 0) {
			float width = chunkWidth();
			float lastX = nextX + width;

			// x inside text?
			if (x<=lastX) {
				return getChunkOffset("Tail", started, token, token.textCount, charCount, currX, x, width);
			} else {
				nextX += width; // add width and continue to next token
			}
		}

		// no hit
		return UNDEFINED;
	}

	private Integer wideCharacterFound(int i, char currChar) {
		// process buffered chunk?
		if (charCount>0) {
			float charsWidth = charCount>0 ? chunkWidth() : 0;
			nextX = currX + charsWidth;

			// done?
			if (x < nextX) {
				return getChunkOffset("In chunk before wide char",
					started, token, i, charCount, currX, x, charsWidth);
			}
			currX = nextX;
			charCount = 0;
		}

		// do regular calculation
		float charWidth = SwingUtils.charWidth(fm, currChar);
		nextX += charWidth;
		charCount = 0;

		// done?
		int result = UNDEFINED;
		if (x < nextX) {
			result = last + i - token.textOffset;
			logConversion(started, x, currChar, result);
		}
		return result;
	}

	private Integer tabCharacterFound(int i, char currChar) {
		// process buffered chunk?
		if (charCount>0) {
			float charsWidth = charCount>0 ? chunkWidth() : 0;
			nextX = stableX + charsWidth;

			// done?
			if (x < nextX) {
				return getChunkOffset("In chunk before tab", started, token, i, charCount, currX, x, charsWidth);
			}
			currX = nextX;
			charCount = 0;
		}

		// tabstop
		int nofTabs = (int) (currX / tabWidth);
		int nextTab = nofTabs + 1;
		float nextTabX = nextTab * tabWidth;
		nextX = nextTabX;
		stableX = nextX; // Cache ending x-coord. of tab.

		// done?
		int result = UNDEFINED;
		if (x >= currX && x < nextX) {
			int tabOffset = last + i - token.textOffset;
			result = x - currX < nextX - x ? tabOffset : tabOffset + 1;
			logConversion(started, x, currChar, result);
		}
		return result;
	}

	/**
	 * Get the width of the text chunk. Since all characters are the same width, we can simply multiply.
	 * @return the number of pixels the text occupies
	 */
	private float chunkWidth() {
		return charCount * charWidth;
	}


	private int getChunkOffset(String info, long started, TokenImpl token, int chunkEnd, int chunkSize, float x0, float x, float chunkWidth) {
		float xInChunk = x - x0;
		float relativeX = xInChunk / chunkWidth;
		int xOffsetInChunk = Math.round(chunkSize * relativeX);

		int chunkOffsetInToken = chunkEnd - chunkSize;
		int result = token.getOffset() + chunkOffsetInToken + xOffsetInChunk;

		int offsetInToken = chunkEnd - chunkSize + xOffsetInChunk;
		logConversion(info, started, textArea, token, x, xOffsetInChunk, offsetInToken, result);
		return result;
	}

}
