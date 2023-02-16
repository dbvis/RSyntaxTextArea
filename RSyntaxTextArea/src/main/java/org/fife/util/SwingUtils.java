/*
 * 04/23/2014
 *
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.util;

import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 * Utilities to handle fractional scaling on Windows by leveraging the floating point API.
 * Compare to deprecated <code>javax.swing.SwingUtilities2</code>.
 */
public final class SwingUtils {

	/**
	 * The length of the text that requires {@link TextLayout} to paint properly.
	 * The default is just a guess based on visual observations when running fractionally scaled fonts on Windows.
	 * It certainly breaks at 70k.
	 * <p/>
	 * Example: <code>-Dorg.fife.util.SwingUtils.textLayoutThreshold=10000</code>
	 *
	 * @see #needsTextLayout(char[], int, int)
	 */
	public static final int TEXT_LAYOUT_THRESHOLD =
		Integer.valueOf(System.getProperty(SwingUtils.class.getName()+".textLayoutThreshold", "1000"));

	private SwingUtils() {
	}

	/**
	 * A floating point adaption of {@link Graphics#drawChars(char[], int, int, int, int)}.
	 *
	 * @param g the graphics to use for drawing
	 * @param x the X coordinate of the start point
	 * @param y the Y coordinate of the start point
	 * @param chars an array of characters
	 * @param beginIndex the initial offset in the array of characters
	 * @param length the number of characters to read from the array
	 */
	public static void drawChars(Graphics2D g, float x, float y, char[] chars, int beginIndex, int length) {
		String string = new String(chars, beginIndex, length);
		if (needsTextLayout(chars, beginIndex, length)) {
			TextLayout layout = new TextLayout(string, g.getFont(), g.getFontRenderContext());
			layout.draw(g, x, y);
		} else {
			g.drawString(string, x, y);
		}
	}

	/**
	 * Check the intrinsic details of <code>SwingUtilities2.drawString(...)</code>,
	 * <code>SwingUtilities2.stringWidth(...)</code>, and <code>FontUtils.isComplexText(...)</code>.
	 *
	 * @param chars the text to analyze
	 * @param beginIndex where to start
	 * @param length the number of characters to check
	 */
	static boolean needsTextLayout(char[] chars, int beginIndex, int length) {
		return length > TEXT_LAYOUT_THRESHOLD || Font.textRequiresLayout(chars, beginIndex, beginIndex + length) ;
	}

	/**
	 * A floating point adaption of {@link Graphics#drawLine(int, int, int, int)}.
	 *
	 * @param g the graphics to use for drawing
	 * @param x1 the X coordinate of the start point
	 * @param y1 the Y coordinate of the start point
	 * @param x2 the X coordinate of the end point
	 * @param y2 the Y coordinate of the end point
	 */
	public static void drawLine(Graphics2D g, double x1, double y1, double x2, double y2) {
		g.draw(new Line2D.Double(x1, y1, x2, y2));
	}

	/**
	 * A floating point adaption of {@link FontMetrics#charWidth(char)}.
	 *
	 * @param fm font metrics to use for the calculation
	 * @param c the character to measure
	 * @return the width of the supplied character
	 * @see #charsWidth(FontMetrics, char[], int, int)
	 */
	public static float charWidth(FontMetrics fm, char c) {
		return charsWidth(fm, new char[]{c}, 0, 1);
	}

	/**
	 * A floating point adaption of {@link FontMetrics#charsWidth(char[], int, int)}.
	 *
	 * @param fm font metrics to use for the calculation
	 * @param chars an array of characters
	 * @param beginIndex the initial offset in the array of characters
	 * @param length the number of characters to read from the array
	 * @return the total width of the supplied characters
	 */
	public static float charsWidth(FontMetrics fm, char[] chars, int beginIndex, int length) {
		FontRenderContext frc = fm.getFontRenderContext();
		Font font = fm.getFont();
		int limit = beginIndex + length;
		Rectangle2D bounds = font.getStringBounds(chars, beginIndex, limit, frc);
		return (float) bounds.getWidth();
	}

	/**
	 * A floating point adaption of {@link FontMetrics#stringWidth(String)}.
	 *
	 * @param fm         font metrics to use for the calculation
	 * @param string     an array of characters
	 * @param beginIndex the initial offset in the array of characters
	 * @param length the number of characters to read from the array
	 * @return the total width of the supplied characters
	 */
	@SuppressWarnings("unused")
	public static float stringWidth(FontMetrics fm, String string, int beginIndex, int length) {
		Rectangle2D bounds =
			fm.getFont().getStringBounds(string, beginIndex, beginIndex+length, fm.getFontRenderContext());
		return (float) bounds.getWidth();
	}

	/**
	 * Convenience method since there is no single method to set the x coordinate.
	 *
	 * @param r the rectangle to modify
	 * @param x the x coordinate to set
	 */
	public static void setX(Rectangle2D r, double x) {
		r.setRect(x, r.getY(), r.getWidth(), r.getHeight());
	}

	/**
	 * Convenience method since there is no single method to set the y coordinate.
	 *
	 * @param r the rectangle to modify
	 * @param y the y coordinate to set
	 */
	public static void setY(Rectangle2D r, double y) {
		r.setRect(r.getX(), y, r.getWidth(), r.getHeight());
	}

	/**
	 * Convenience method since there is no single method to set the height.
	 *
	 * @param r the rectangle to modify
	 * @param height the height to set
	 */
	public static void setHeight(Rectangle2D r, double height) {
		r.setRect(r.getX(), r.getY(), r.getWidth(), height);
	}

	/**
	 * Convenience method since there is no single method to set the width.
	 *
	 * @param r the rectangle to modify
	 * @param width the width to set
	 */
	public static void setWidth(Rectangle2D r, double width) {
		r.setRect(r.getX(), r.getY(), width, r.getHeight());
	}

	/**
	 * Convenience method to convert {@link Rectangle2D} to {@link Rectangle} observing <code>null</code> returns.
	 *
	 * @param c the text component
	 * @param pos the position (0 or positive)
	 * @return Rectangle or <code>null</code>
	 * @throws BadLocationException if the given position does not represent a valid location in the associated document
	 */
	public static Rectangle getBounds(JTextComponent c, int pos) throws BadLocationException {
		Rectangle2D rectangle2D = c.modelToView2D(pos);
		return rectangle2D==null ? null : rectangle2D.getBounds();
	}


    /**
     * Does the supplied metrics represent a monospaced font, ie a font where all characters are equally wide?
     *
     * @param fontMetrics the metrics to use for checking character widths
     * @return boolean
     */
    public static boolean isMonospaced(FontMetrics fontMetrics) {
        boolean isMonospaced = true;
        int firstCharacterWidth = 0;
        boolean hasFirstCharacterWidth = false;
        for (int cp = 0; cp < 128; cp++) {
            if (Character.isValidCodePoint(cp) &&  (Character.isLetter(cp) || Character.isDigit(cp))) {
                char character = (char) cp;
                int characterWidth = fontMetrics.charWidth(character);
                if (hasFirstCharacterWidth) {
                    if (characterWidth != firstCharacterWidth) {
                        isMonospaced = false;
                        break;
                    }
                }
                else {
                    firstCharacterWidth = characterWidth;
                    hasFirstCharacterWidth = true;
                }
            }
        }
        return isMonospaced;
    }
}
