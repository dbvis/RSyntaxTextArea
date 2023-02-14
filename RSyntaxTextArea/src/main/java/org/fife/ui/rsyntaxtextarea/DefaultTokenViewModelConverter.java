package org.fife.ui.rsyntaxtextarea;

import org.fife.util.SwingUtils;

import javax.swing.text.TabExpander;
import java.awt.*;

public class DefaultTokenViewModelConverter implements TokenViewModelConverter {
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

			fm = textArea.getFontMetricsForTokenType(token.getType());
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
					AbstractTokenViewModelConverter.fine(()->String.format("x=%.3f | currX=%.3f => offset=%,d", x, finalCurrX, result));
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


}
