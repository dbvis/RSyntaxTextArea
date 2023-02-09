package org.fife.ui.rsyntaxtextarea;

import javax.swing.text.TabExpander;

public interface TokenViewModelConverter {
	/**
	 * Get the offset for the character in the document that maps to the specified x coordinate.
	 * Subclasses are expected to implement the behavior in the best and fastest way according to the context
	 * (typically the type of font or fonts used).
	 *
	 * @param tokenList the first token on a text line
	 * @param x0        The pixel x-location that is the beginning of <code>tokenList</code>.
	 * @param x         The pixel-position for which you want to get the corresponding offset.
	 * @return The position (in the document, NOT into the token list!) that covers the pixel location.
	 * @see Token#getListOffset(RSyntaxTextArea, TabExpander, float, float)
	 */
	int getListOffset(TokenImpl tokenList, float x0, float x);
}