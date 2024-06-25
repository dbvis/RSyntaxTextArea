/*
 * 02/15/23
 *
 * DefaultTokenViewModelConverter.java - default implementation.
 *
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import org.fife.util.SwingUtils;

import javax.swing.text.Segment;
import javax.swing.text.TabExpander;
import javax.swing.text.Utilities;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.logging.Logger;

/**
 * The default implementation, extracted from {@link TokenImpl}.
 *
 * @see TokenImpl#getListOffset(RSyntaxTextArea, TabExpander, float, float)
 * @see TokenImpl#listOffsetToView(RSyntaxTextArea, TabExpander, int, float, Rectangle2D)
 */
public class DefaultTokenViewModelConverter implements TokenViewModelConverter {
	private static final Logger LOG = Logger.getLogger(DefaultTokenViewModelConverter.class.getName());

	private final RSyntaxTextArea textArea;
	private final TabExpander e;

	public DefaultTokenViewModelConverter(RSyntaxTextArea textArea, TabExpander e) {
		this.textArea = textArea;
		this.e = e;
	}

	@Override
	public int getListOffset(TokenImpl tokenList, float x0, float x) {
		TokenImpl token = tokenList;

		// If the coordinate in question is before this line's start, quit.
		if (x0 >= x) {
			return token.getOffset();
		}

		float currX = x0; // x-coordinate of current char.
		float nextX = x0; // x-coordinate of next char.
		float stableX = x0; // Cached ending x-coord. of last tab or token.
		int last = token.getOffset();
		FontMetrics fm;

		while (token != null && token.isPaintable()) {

		        fm = textArea.getFontMetricsForToken(token);
			char[] text = token.text;
			int start = token.textOffset;
			int end = start + token.textCount;

			for (int i = start; i < end; i++) {
				currX = nextX;
				if (text[i] == '\t') {
					nextX = e.nextTabStop(nextX, 0);
					stableX = nextX; // Cache ending x-coord. of tab.
					start = i + 1; // Do charsWidth() from next char.
				}
				else {
					nextX = stableX + SwingUtils.charsWidth(fm, text, start, i - start + 1);
				}
				if (x >= currX && x < nextX) {
					int offset = last + i - token.textOffset;
					boolean beforeMiddle = x - currX < nextX - x;
					float finalCurrX = currX;
					int result = beforeMiddle ? offset : offset + 1;
					LOG.fine(()->String.format("x=%.3f | currX=%.3f => offset=%,d", x, finalCurrX, result));
					return result;
				}
			}

			stableX = nextX; // Cache ending x-coordinate of token.
			last += token.textCount;
			token = (TokenImpl)token.getNextToken();

		}

		// If we didn't find anything, return the end position of the text.
		return last;

	}

	@Override
	public Rectangle2D listOffsetToView(TokenImpl token, TabExpander e, int pos, float x0, Rectangle2D rect) {
		float stableX = x0; // Cached ending x-coord. of last tab or token.
		FontMetrics fm;
		Segment s = new Segment();

		while (token != null && token.isPaintable()) {

		        fm = textArea.getFontMetricsForToken(token);
			if (fm == null) {
				return rect; // Don't return null as things will error.
			}
			char[] text = token.text;
			int start = token.textOffset;
			int end = start + token.textCount;

			// If this token contains the position for which to get the
			// bounding box...
			if (token.containsPosition(pos)) {

				s.array = token.text;
				s.offset = token.textOffset;
				s.count = pos - token.getOffset();

				// Must use this (actually fm.charWidth()), and not
				// fm.charsWidth() for returned value to match up with where
				// text is actually painted on OS X!
				float w = Utilities.getTabbedTextWidth(s, fm, stableX, e,
					token.getOffset());
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

			// If this token does not contain the position for which to get
			// the bounding box...
			else {
				s.array = token.text;
				s.offset = token.textOffset;
				s.count = token.textCount;
				stableX += Utilities.getTabbedTextWidth(s, fm, stableX, e,
					token.getOffset());
			}

			token = (TokenImpl)token.getNextToken();

		}

		// If we didn't find anything, we're at the end of the line. Return
		// a width of 1 (so selection highlights don't extend way past line's
		// text). A ConfigurableCaret will know to paint itself with a larger
		// width.
		SwingUtils.setX(rect, stableX);
		SwingUtils.setWidth(rect, 1);
		return rect;
	}

}
