package org.fife.ui.rsyntaxtextarea;

import javax.swing.text.Segment;
import javax.swing.text.TabExpander;
import javax.swing.text.Utilities;
import java.awt.*;
import java.util.logging.Logger;

/**
 * <h1>EXPERIMENTAL - DO NOT USE - MISCALCULATES!</h1>
 * Implemented as a quick experiment to see if caching character widths yield performance improvements.
 */
public class CachedTokenViewModelConverter extends AbstractTokenViewModelConverter {

	private static final Logger LOG = Logger.getLogger(CachedTokenViewModelConverter.class.getName());

	protected CachedTokenViewModelConverter(RSyntaxTextArea textArea, TabExpander e) {
		super(textArea, e);
	}

	@Override
	protected int getTokenListOffset() {
		FontCache cache = textArea.getFontCacheForTokenType(token.getType());
		char[] text = token.text;
		int start = token.textOffset;
		int end = start + token.textCount;

		for (int i = start; i < end; i++) {
			currX = nextX;
			if (text[i] == '\t') {
				nextX = tabExpander.nextTabStop(nextX, 0);
				stableX = nextX; // Cache ending x-coord. of tab.
				start = i + 1; // Do charsWidth() from next char.
			} else {
				nextX = stableX + (float) cache.getWidth(text, start, i - start + 1);
			}
			if (x >= currX && x < nextX) {
				int offset = last + i - token.textOffset;
				boolean beforeMiddle = x - currX < nextX - x;
				float finalCurrX = currX;
				int result = beforeMiddle ? offset : offset + 1;
				LOG.fine(() -> String.format("x=%.3f | currX=%.3f => offset=%,d", x, finalCurrX, result));
					return result;
				}
			}

			stableX = nextX; // Cache ending x-coordinate of token.
			last += token.textCount;
			token = (TokenImpl)token.getNextToken();

		// If we didn't find anything, return the end position of the text.
		return last;
	}

	/**
	 * Placeholder to reimplement and make use of cached font widths.
	 * FIXME must eliminate call to Utils.getTabbedTextWidth() in order to make use of cache
	 * @return The bounding box for the specified position in the model.
	 * @see Utilities#getTabbedTextWidth(Segment, FontMetrics, float, TabExpander, int)
	 */
	@Override
	protected float getTabbedTextWidth(Segment s, FontMetrics fm) {
		return super.getTabbedTextWidth(s, fm);
	}

	@Override
	protected float charWidth(FontMetrics fm, char ch) {
		// FIXME - modify superclass to  remove redundant FontMetrics from signature
		return (float) textArea.getFontCacheForTokenType(token.getType()).getWidth(ch);
	}

}
