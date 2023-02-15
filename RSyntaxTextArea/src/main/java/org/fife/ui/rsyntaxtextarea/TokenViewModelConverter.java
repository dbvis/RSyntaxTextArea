/*
 * 02/15/23
 *
 * TokenViewModelConverter.java - extracted from TokenImpl for flexibility.
 *
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */

package org.fife.ui.rsyntaxtextarea;

import javax.swing.text.TabExpander;
import java.awt.geom.Rectangle2D;

/**
 * Defines a protocol to allow different implementations for use by
 * {@link TokenImpl#getListOffset(RSyntaxTextArea, TabExpander, float, float)} and
 * {@link TokenImpl#listOffsetToView(RSyntaxTextArea, TabExpander, int, float, Rectangle2D)}.
 */
public interface TokenViewModelConverter {
	/**
	 * Option to define an implementation of {@link TokenViewModelConverter} by means of a JVM property.
	 * The class must implement a constructor that accepts a {@link RSyntaxTextArea} and a {@link TabExpander}
	 * as arguments.
	 *
	 * @see TokenImpl#getTokenViewModelConverter(RSyntaxTextArea, TabExpander)
	 */
	String PROPERTY_CONVERTER_CLASS = "org.fife.ui.rsyntaxtextarea.TokenViewModelConverter";

	/**
	 * Get the offset for the character in the document that maps to the specified x coordinate.
	 * Subclasses are expected to implement the behavior in the best and fastest way according to the context
	 * (typically the type of font or fonts used).
	 *
	 * @param tokenList the first token on a text line.
	 * @param x0        The pixel x-location that is the beginning of <code>tokenList</code>.
	 * @param x         The pixel-position for which you want to get the corresponding offset.
	 * @return The position (in the document, NOT into the token list!) that covers the pixel location.
	 * @see Token#getListOffset(RSyntaxTextArea, TabExpander, float, float)
	 */
	int getListOffset(TokenImpl tokenList, float x0, float x);

	/**
	 * Returns the bounding box for the specified document location.  The
	 * location must be in the specified token list; if it isn't,
	 * <code>null</code> is returned.
	 *
	 * @param token    the first token on a text line.
	 * @param e        How to expand tabs.
	 * @param pos      The position in the document for which to get the bounding
	 *                 box in the view.
	 * @param x0       The pixel x-location that is the beginning of
	 *                 <code>tokenList</code>.
	 * @param rect     The rectangle in which we'll be returning the results.  This
	 *                 object is reused to keep from frequent memory allocations.
	 * @return The bounding box for the specified position in the model.
	 * @see Token#listOffsetToView(RSyntaxTextArea, TabExpander, int, float, Rectangle2D)
	 */
	Rectangle2D listOffsetToView(TokenImpl token, TabExpander e, int pos, float x0, Rectangle2D rect);
}
