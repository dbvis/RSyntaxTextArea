/*
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.rtextarea;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.undo.AbstractUndoableEdit;

import org.fife.ui.SwingRunnerExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Unit tests for the {@link RUndoManager} class.
 */
@ExtendWith(SwingRunnerExtension.class)
class RUndoManagerTest {


	@Test
	void testUndo_insideAtomicEdit_undoesTheAtomicEditsText() throws BadLocationException {

		RTextArea textArea = new RTextArea();
		insertAtEnd(textArea, "a");

		textArea.beginAtomicEdit();
		insertAtEnd(textArea, "b");
		textArea.undoLastAction();
		Assertions.assertEquals("a", textArea.getText());
		insertAtEnd(textArea, "c");
		textArea.endAtomicEdit();

		Assertions.assertEquals("ac", textArea.getText());
		textArea.undoLastAction();
		Assertions.assertEquals("a", textArea.getText());
		textArea.undoLastAction();
		Assertions.assertEquals("", textArea.getText());
	}


	@Test
	void testUndo_insideAtomicEdit_undoHistorySurvivesTheUndoLimit() throws BadLocationException {

		RTextArea textArea = new RTextArea();
		insertAtEnd(textArea, "a");

		textArea.beginAtomicEdit();
		insertAtEnd(textArea, "b");
		textArea.undoLastAction();
		insertAtEnd(textArea, "c");
		textArea.endAtomicEdit();

		// Two characters inserted away from the caret move it by more than
		// one, so each insert is its own undo entry, and the entries above are
		// trimmed once the undo limit is reached
		Assertions.assertDoesNotThrow(() -> {
			for (int i = 0; i < 150; i++) {
				textArea.getDocument().insertString(0, "xx", null);
				textArea.setCaretPosition(textArea.getDocument().getLength());
			}
		});
		Assertions.assertTrue(textArea.canUndo());
		int length = textArea.getDocument().getLength();
		textArea.undoLastAction();
		Assertions.assertEquals(length - 2, textArea.getDocument().getLength());
	}


	@Test
	void testRedo_insideAtomicEditWithChanges_isUnavailable() throws BadLocationException {

		RTextArea textArea = new RTextArea();
		insertAtEnd(textArea, "a");
		textArea.undoLastAction();

		textArea.beginAtomicEdit();
		insertAtEnd(textArea, "b");
		Assertions.assertFalse(textArea.canRedo());
		textArea.redoLastAction();
		Assertions.assertEquals("b", textArea.getText());
		textArea.endAtomicEdit();

		Assertions.assertFalse(textArea.canRedo());
		textArea.undoLastAction();
		Assertions.assertEquals("", textArea.getText());
	}


	@Test
	void testRedo_insideAtomicEditWithoutChanges_redoesTheUndoneEdit() throws BadLocationException {

		RTextArea textArea = new RTextArea();
		insertAtEnd(textArea, "a");
		textArea.undoLastAction();

		textArea.beginAtomicEdit();
		textArea.redoLastAction();
		Assertions.assertEquals("a", textArea.getText());
		textArea.endAtomicEdit();

		textArea.undoLastAction();
		Assertions.assertEquals("", textArea.getText());
	}


	@Test
	void testUndo_repeatedInsideAtomicEdit_keepsTheRedoList() throws BadLocationException {

		RTextArea textArea = new RTextArea();
		insertAtEnd(textArea, "aaa");
		insertAt(textArea, 0, "b");

		textArea.beginAtomicEdit();
		textArea.undoLastAction();
		textArea.undoLastAction();
		textArea.endAtomicEdit();
		Assertions.assertEquals("", textArea.getText());

		textArea.redoLastAction();
		Assertions.assertEquals("aaa", textArea.getText());
		textArea.redoLastAction();
		Assertions.assertEquals("baaa", textArea.getText());
	}


	@Test
	void testEndAtomicEdit_undoesAsOneEntry() throws BadLocationException {

		RTextArea textArea = new RTextArea();
		insertAtEnd(textArea, "abc");

		textArea.beginAtomicEdit();
		insertAt(textArea, 0, "1");
		insertAtEnd(textArea, "2");
		textArea.endAtomicEdit();
		textArea.undoLastAction();

		Assertions.assertEquals("abc", textArea.getText());
	}


	@Test
	void testUndo_insideNestedAtomicEdit_undoesTheAtomicEditsText() throws BadLocationException {

		RTextArea textArea = new RTextArea();
		insertAtEnd(textArea, "base");

		textArea.beginAtomicEdit();
		textArea.beginAtomicEdit();
		insertAtEnd(textArea, " one");
		textArea.undoLastAction();
		Assertions.assertEquals("base", textArea.getText());
		insertAtEnd(textArea, " two");
		textArea.endAtomicEdit();
		insertAt(textArea, 0, "X");
		textArea.endAtomicEdit();

		Assertions.assertEquals("Xbase two", textArea.getText());
		textArea.undoLastAction();
		Assertions.assertEquals("base", textArea.getText(), "the outer atomic edit must undo as one entry");
		textArea.undoLastAction();
		Assertions.assertEquals("", textArea.getText());
	}


	@Test
	void testEndAtomicEdit_onlyInsignificantEdits_keepsTheRedoList() throws BadLocationException {

		RTextArea textArea = new RTextArea();
		insertAtEnd(textArea, "a");
		textArea.undoLastAction();

		textArea.beginAtomicEdit();
		for (UndoableEditListener l : ((AbstractDocument)textArea.getDocument()).getUndoableEditListeners()) {
			l.undoableEditHappened(new UndoableEditEvent(textArea.getDocument(), new InsignificantEdit()));
		}
		Assertions.assertTrue(textArea.canRedo());
		textArea.endAtomicEdit();

		Assertions.assertTrue(textArea.canRedo());
		textArea.redoLastAction();
		Assertions.assertEquals("a", textArea.getText());
	}


	@Test
	void testEndAtomicEdit_emptyAtomicEdit_keepsTheRedoList() throws BadLocationException {

		RTextArea textArea = new RTextArea();
		insertAtEnd(textArea, "a");
		textArea.undoLastAction();

		textArea.beginAtomicEdit();
		textArea.endAtomicEdit();

		Assertions.assertTrue(textArea.canRedo());
		textArea.redoLastAction();
		Assertions.assertEquals("a", textArea.getText());
	}


	private static void insertAtEnd(RTextArea textArea, String text) throws BadLocationException {
		insertAt(textArea, textArea.getDocument().getLength(), text);
	}


	/**
	 * Moves the caret away first, so that each insert is its own undo entry.
	 */
	private static void insertAt(RTextArea textArea, int offset, String text) throws BadLocationException {
		textArea.setCaretPosition(offset);
		textArea.getDocument().insertString(offset, text, null);
		textArea.setCaretPosition(offset + text.length());
	}


	/**
	 * An edit that changes nothing, like one that only restores the caret.
	 */
	private static final class InsignificantEdit extends AbstractUndoableEdit {

		@Override
		public boolean isSignificant() {
			return false;
		}
	}


}
