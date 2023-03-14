package org.fife.ui.rsyntaxtextarea;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;

/**
 * Cache for font widths of characters in the Latin-1 range.
 */
public class FontCache {
	private final double[] width = new double[255];
	private final Font font;
	private final FontRenderContext frc;

	public FontCache(FontMetrics fm) {
		frc = fm.getFontRenderContext();
		font = fm.getFont();
		for (char c = 0; c < 255; c++) {
			char[] chars = {c};
			Rectangle2D bounds = font.getStringBounds(chars, 0, 1, frc);
			width[c] = bounds.getWidth();
		}
	}

	public double getWidth(char[] text, int start, int count) {
		double result = 0;
		for (int i = start; i < start+count; i++) {
			char c = text[i];
			result += getWidth(c);
		}
		return result;
	}

	public double getWidth(char c) {
		return c < 255 ? width[c] : uncachedWidth(c);
	}

	private double uncachedWidth(char c) {
		assert c>=255 : "Character should be cached: " + c;
		char[] chars = {c};
		Rectangle2D bounds = font.getStringBounds(chars, 0, 1, frc);
		return bounds.getWidth();
	}

}
