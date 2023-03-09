/*
 * 02/15/23
 *
 * SectionedTokenViewModelConverter.java - improves performance
 *
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import javax.swing.text.TabExpander;
import java.awt.geom.Rectangle2D;
import java.util.logging.Logger;

/**
 * <b>NOT FUNCTIONAL!</b>
 * <p/>
 * An <b>EXPERIMENTAL</b> implementation that splits tokens in smaller chunks before calling a regular converter.
 * The general idea is to address the performance problems by splitting tokens to restrict the size of each token.
 * <p/>
 * <b>Rationale:</b><br/>
 * We get good performance even on very long lines as long as each token is reasonably small.
 * By splitting long tokens in a sequence of identical but smaller tokens, we should overcome this issue.
 * We do it locally before conversion rather than in the {@link TokenMaker} to avoid issues with language-aware
 * text parsers (eg auto-completion) that expect atomic tokens.
 */
public class SectionedTokenViewModelConverter extends BufferedTokenViewModelConverter {

	/**
	 * Name of property to define size of chunks to improve performance.
	 * <b>EXPERIMENTAL:</b> processing in chunks yields better performance on long strings (+50k), but
	 * yields bad offsets if size is too small, or if running with fractionally scaled fonts on Windows.
	 */
	public static final String PROPERTY_SECTION_SIZE = SectionedTokenViewModelConverter.class.getName() + ".sectionSize";
	private static final Logger LOG = Logger.getLogger(SectionedTokenViewModelConverter.class.getName());
	private final int sectionSize;

	/**
	 * Max size of text strings to buffer when converting x coordinate to model offset.
	 */
	public SectionedTokenViewModelConverter(RSyntaxTextArea textArea, TabExpander e) {
		super(textArea, e);
		this.sectionSize = Integer.valueOf(System.getProperty(PROPERTY_SECTION_SIZE, "10000"));
	}

	@Override
	public Rectangle2D listOffsetToView(TokenImpl mainToken, TabExpander e, int pos, float x0, Rectangle2D rectangle) {
		TokenImpl sections = toSections(mainToken);
		return super.listOffsetToView(sections, e, pos, x0, rectangle);
	}

	@Override
	public int getListOffset(TokenImpl tokenList, float x0, float x) {
		TokenImpl sections = toSections(tokenList);
		return super.getListOffset(sections, x0, x);
	}

	private TokenImpl toSections(TokenImpl tokenList) {
		TokenImpl all = null;
		TokenImpl current = null;
		for (Token t = tokenList; t != null; t = t.getNextToken()) {

			if (t instanceof TokenImpl) {
				TokenImpl nextToken = nextToken((TokenImpl) t);
				if (current == null) {
					all = nextToken;
					current = (TokenImpl) nextToken.getLastPaintableToken();
				} else {
					current.setNextToken(nextToken);
					current = (TokenImpl) nextToken.getLastPaintableToken();
				}


			} else {
				LOG.fine("Incompatible Token class: " + t);
				return tokenList;
			}
		}
		assert assertConsecutiveTokens(all);
		return all;
	}

	private boolean assertConsecutiveTokens(TokenImpl tokens) {
		int lastEndOffset = tokens.getEndOffset();
		int i = 0;
		for (Token t = tokens.getNextToken(); t != null && t.getType()!=TokenTypes.NULL; t = t.getNextToken(), i++) {
			int textCount = ((TokenImpl) t).textCount;
			assert t.getOffset() == lastEndOffset : assertionMessage(tokens, lastEndOffset, i, t, textCount);
			assert t.getEndOffset()- textCount == t.getOffset() : assertionMessage(tokens, lastEndOffset, i, t, textCount);
			lastEndOffset = t.getEndOffset();
		}
		return true;
	}

	private String assertionMessage(TokenImpl tokens, int last, int i, Token t, int textCount) {
		return String.format("token#%d | last=%d | offset=%d, endOffset=%d, textCount=%d | All tokens:%n%s",
			i, last, t.getOffset(), t.getEndOffset(), textCount, dumpTokens(tokens));
	}

	private TokenImpl nextToken(TokenImpl t) {
		return t.length() < sectionSize ? newToken(t) : splitToken(t);
	}

	private TokenImpl splitToken(TokenImpl t) {
		assert t.textCount >= sectionSize : "Too small token: " + t;
		TokenImpl first = newToken(t);
		first.textCount = sectionSize;
		TokenImpl current = first;

		for (int s = sectionSize; s < t.textCount; s += sectionSize) {
			TokenImpl next = newToken(t);
			next.makeStartAt(current.getOffset() + sectionSize);
			current.setNextToken(next);
			current = next;
		}

		LOG.fine(()->dumpTokens(first));
		return first;
	}

	private TokenImpl newToken(TokenImpl t) {
		TokenImpl newToken = new TokenImpl(t);
		newToken.setNextToken(null);
		return newToken;
	}

}