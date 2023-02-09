package org.fife.ui.rsyntaxtextarea;

import org.fife.util.SwingUtils;

import javax.swing.text.TabExpander;
import java.awt.*;
import java.util.logging.Logger;

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
	private static final Logger LOG = Logger.getLogger(FixedWidthTokenViewModelConverter.class.getName());

	private final FontMetrics fm;

	public FixedWidthTokenViewModelConverter(RSyntaxTextArea textArea, FontMetrics fm, TabExpander e) {
		super(textArea, e);
		this.fm = fm;
	}

	@Override
	public int getListOffset(TokenImpl tokenList, float x0, float x) {
	// TODO REFACTOR! Lots of duplicated code
		// for calculating tab stops
		int tabSize = textArea.getTabSize();
		float spaceWidth = SwingUtils.charWidth(fm, ' ');
		float tabWidth = spaceWidth * tabSize;

		// loop params
		float currX = x0; // x-coordinate of current char.
		float nextX = x0; // x-coordinate of next char.
		float stableX = x0; // Cached ending x-coord. of last tab or token.
		int last = tokenList.getOffset();

		// loop over tokens
		long started = System.currentTimeMillis();
		while (tokenList != null && tokenList.isPaintable()) {

			char[] text = tokenList.text;
			int start = tokenList.textOffset;
			int end = start + tokenList.textCount;
			int charCount = 0;

			// loop over text in token
			for (int i = start; i < end; i++) {
				currX = nextX;
				char currChar = text[i];

				// TAB?
				if (currChar == '\t') {
					int begin = i - charCount;
					float charsWidth = charCount>0 ? SwingUtils.charsWidth(fm, text, begin, charCount) : 0;

					// add width of characters before the tab and reset counter
					if (charCount>0) {
						nextX = stableX + charsWidth;

						// done?
						if (x < nextX) {
							float relativeX = (x - currX) / charsWidth;
							int xOffsetInText = Math.round(charCount * relativeX);
							int iOffset = last + i - tokenList.textOffset;
							int xOffsetInDocument = iOffset-charCount + xOffsetInText;

							int xOffsetInToken = i - charCount + xOffsetInText;
							int tokenOffsetInDocument = tokenList.getOffset();

							int tokenTextCount = tokenList.textCount;
							LOG.fine(() -> debugListOffset("Before monospaced tab",
								started, textArea, text, x, tokenOffsetInDocument, xOffsetInText, xOffsetInToken, xOffsetInDocument, tokenTextCount));
							return xOffsetInDocument;
						}
						// nope - add width and continue
						currX = nextX;
						charCount = 0;
					}

					// tabstop
					int nofTabs = (int) ( currX / tabWidth);
					int nextTab = nofTabs + 1;
					float nextTabX = nextTab * tabWidth;
					nextX = nextTabX;
					stableX = nextX; // Cache ending x-coord. of tab.

					// done?
					if (x >= currX && x < nextX) {
						int tabOffset = last + i - tokenList.textOffset;
						int result = x - currX < nextX - x ? tabOffset : tabOffset + 1;
						LOG.fine(() -> String.format("%,d ms: Found in tab: x=%.3f => offset=%,d",
							System.currentTimeMillis() - started, x, result));
						return result;
					}

				}
				else if (isWideCharacter(currChar)) {
					if (charCount>0) {
						int begin = i - charCount;
						float charsWidth = charCount>0 ? SwingUtils.charsWidth(fm, text, begin, charCount) : 0;

						// add width of characters before the wide char and reset counter
						nextX = currX + charsWidth;

						// done?
						if (x < nextX) {
							float relativeX = (x - currX) / charsWidth;
							int xOffsetInText = Math.round(charCount * relativeX);
							int iOffset = last + i - tokenList.textOffset;
							int xOffsetInDocument = iOffset-charCount + xOffsetInText;

							int xOffsetInToken = i - charCount + xOffsetInText;
							int tokenOffsetInDocument = tokenList.getOffset();

							int tokenTextCount = tokenList.textCount;
							LOG.fine(() -> debugListOffset("Found in chunk before wide char",
								started, textArea, text, x, tokenOffsetInDocument, xOffsetInText, xOffsetInToken, xOffsetInDocument, tokenTextCount));
							return xOffsetInDocument;
						}
						// nope - add cumulated width and continue
						currX = nextX;
						charCount = 0;


					}
					// do regular calculation
					float charWidth = SwingUtils.charWidth(fm, currChar);
					nextX += charWidth;
					charCount = 0;

					// done?
					if (x < nextX) {
						int result = last + i - tokenList.textOffset;
						LOG.fine(() -> String.format("%,d ms: Found in wide character '%s':  => result=%,d",
							System.currentTimeMillis() - started, currChar, result));
						return result;
					}
				}
				else {

					// regular character - increment counter
					charCount++;
				}
			}

			// process remaining text after last breakpoint (if any)
			if (charCount > 0) {
				int begin = end - charCount;
				float width = SwingUtils.charsWidth(fm, text, begin, charCount);
				float lastX = nextX + width;

				// x inside text?
				if (x<=lastX) {
					float xInText = x - currX;
					float relativeX = xInText / width;
					int xOffsetInText = Math.round(charCount * relativeX);
					int xOffsetInToken = tokenList.textCount - charCount + xOffsetInText;
					int tokenOffsetInDocument = tokenList.getOffset();
					int xOffsetInDocument = tokenOffsetInDocument + xOffsetInToken;
					int tokenTextCount = tokenList.textCount;
					LOG.fine(() -> debugListOffset("Monospaced tail",
						started, textArea, text, x, tokenOffsetInDocument, xOffsetInText, xOffsetInToken, xOffsetInDocument, tokenTextCount));
					return xOffsetInDocument;
				} else {
					nextX += width; // add width and continue to next token
				}
			}

			// no match in token - continue to next
			stableX = nextX; // Cache ending x-coordinate of token.
			last += tokenList.textCount;
			tokenList = (TokenImpl) tokenList.getNextToken();

		}

		// If we didn't find anything, return the end position of the text.
		LOG.fine("Monospaced EOL");
		return last;

	}

}
