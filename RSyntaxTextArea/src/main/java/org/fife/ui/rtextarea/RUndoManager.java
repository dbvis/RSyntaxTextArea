/*
 * 12/06/2008
 *
 * RUndoManager.java - Handles undo/redo behavior for RTextArea.
 *
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.rtextarea;

import java.util.ResourceBundle;

import javax.swing.Action;
import javax.swing.UIManager;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;


/**
 * This class manages undos/redos for a particular editor pane.  It groups
 * all undos that occur one character position apart together, to avoid
 * Java's horrible "one character at a time" undo behavior.  It also
 * recognizes "replace" actions (i.e., text is selected, then the user
 * types), and treats it as a single action, instead of a remove/insert
 * action pair.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class RUndoManager extends UndoManager {

	private RCompoundEdit compoundEdit;
	private RTextArea textArea;
	private int lastOffset;
	private String cantUndoText;
	private String cantRedoText;

	private int internalAtomicEditDepth;

	private static final String MSG	= "org.fife.ui.rtextarea.RTextArea";


	/**
	 * Constructor.
	 *
	 * @param textArea The parent text area.
	 */
	public RUndoManager(RTextArea textArea) {
		this.textArea = textArea;
		ResourceBundle msg = ResourceBundle.getBundle(MSG);
		cantUndoText = msg.getString("Action.CantUndo.Name");
		cantRedoText = msg.getString("Action.CantRedo.Name");
	}


	/**
	 * Begins an "atomic" edit.  This method is called when RTextArea
	 * KNOWS that some edits should be compound automatically, such as
	 * when the user is typing in overwrite mode (the deletion of the
	 * current char + insertion of the new one) or the playing back of a
	 * macro.
	 *
	 * @see #endInternalAtomicEdit()
	 */
	public void beginInternalAtomicEdit() {
		if (++internalAtomicEditDepth==1) {
			if (compoundEdit!=null) {
				compoundEdit.end();
			}
			compoundEdit = new RCompoundEdit();
		}
	}


	/**
	 * Ends an "atomic" edit.
	 *
	 * @see #beginInternalAtomicEdit()
	 */
	public void endInternalAtomicEdit() {
		if (internalAtomicEditDepth>0 && --internalAtomicEditDepth==0) {
			commitAtomicEdit();
			compoundEdit = null;
			updateActions();	// Needed to show the new display name.
		}
	}


	/**
	 * Adds the edit collected by the open atomic edit to the undo list, and
	 * ends it.  An edit that changed nothing is not added, since adding any
	 * edit discards the redo list; it may still hold edits that are not
	 * significant, such as a caret position to restore.
	 */
	private void commitAtomicEdit() {
		if (compoundEdit.isSignificant()) {
			addEdit(compoundEdit);
		}
		compoundEdit.end();
	}


	/**
	 * Commits what the open atomic edit has collected so far, and starts a
	 * new compound edit for whatever it collects afterwards.  Called before an
	 * undo inside an atomic edit, which would otherwise skip the text the
	 * atomic edit inserted.
	 */
	private void commitOpenAtomicEdit() {
		commitAtomicEdit();
		compoundEdit = new RCompoundEdit();
	}


	private boolean atomicEditHasChanges() {
		return internalAtomicEditDepth>0 && compoundEdit.isSignificant();
	}


	/**
	 * Adds an edit to the undo list, unless the list already holds it.  An
	 * edit added a second time while still in progress is added to itself,
	 * and that cycle overflows the stack once the undo limit trims it.
	 *
	 * @param anEdit The edit to add.
	 * @return Whether the edit was added or is already in the list.
	 */
	@Override
	public synchronized boolean addEdit(UndoableEdit anEdit) {
		if (edits.contains(anEdit)) {
			return true;
		}
		return super.addEdit(anEdit);
	}


	/**
	 * Returns whether an edit can be redone.  Not while the open atomic edit
	 * has changed the document: those changes supersede the redo list, as
	 * typing does.
	 *
	 * @return Whether an edit can be redone.
	 */
	@Override
	public synchronized boolean canRedo() {
		return !atomicEditHasChanges() && super.canRedo();
	}


	/**
	 * Returns the localized "Can't Redo" string.
	 *
	 * @return The localized "Can't Redo" string.
	 * @see #getCantUndoText()
	 */
	public String getCantRedoText() {
		return cantRedoText;
	}


	/**
	 * Returns the localized "Can't Undo" string.
	 *
	 * @return The localized "Can't Undo" string.
	 * @see #getCantRedoText()
	 */
	public String getCantUndoText() {
		return cantUndoText;
	}


	@Override
	public void redo() {
		if (atomicEditHasChanges()) {
			throw new CannotRedoException();
		}
		super.redo();
		updateActions();
	}


	private RCompoundEdit startCompoundEdit(UndoableEdit edit) {
		lastOffset = textArea.getCaretPosition();
		compoundEdit = new RCompoundEdit();
		compoundEdit.addEdit(edit);
		addEdit(compoundEdit);
		return compoundEdit;
	}


	@Override
	public void undo() {
		if (internalAtomicEditDepth>0) {
			commitOpenAtomicEdit();
		}
		super.undo();
		updateActions();
	}


	@Override
	public void undoableEditHappened(UndoableEditEvent e) {

		// This happens when the first undoable edit occurs, and
		// just after an undo.  So, we need to update our actions.
		if (compoundEdit==null) {
			compoundEdit = startCompoundEdit(e.getEdit());
			updateActions();
			return;
		}

		else if (internalAtomicEditDepth>0) {
			compoundEdit.addEdit(e.getEdit());
			return;
		}

		// This happens when there's already an undo that has occurred.
		// Test to see if these undos are on back-to-back characters,
		// and if they are, group them as a single edit.  Since an
		// undo has already occurred, there is no need to update our
		// actions here.
		int diff = textArea.getCaretPosition() - lastOffset;
		// "<=1" allows contiguous "overwrite mode" key presses to be
		// grouped together.
		if (Math.abs(diff)<=1) {//==1) {
			compoundEdit.addEdit(e.getEdit());
			lastOffset += diff;
			//updateActions();
			return;
		}

		// This happens when this UndoableEdit didn't occur at the
		// character just after the previous undoable edit.  Since an
		// undo has already occurred, there is no need to update our
		// actions here either.
		compoundEdit.end();
		compoundEdit = startCompoundEdit(e.getEdit());
		//updateActions();

	}


	/**
	 * Ensures that undo/redo actions are enabled appropriately and have
	 * descriptive text at all times.
	 */
	public void updateActions() {

		String text;

		Action a = RTextArea.getAction(RTextArea.UNDO_ACTION);
		if (canUndo()) {
			a.setEnabled(true);
			text = getUndoPresentationName();
			a.putValue(Action.NAME, text);
			a.putValue(Action.SHORT_DESCRIPTION, text);
		}
		else {
			if (a.isEnabled()) {
				a.setEnabled(false);
				text = cantUndoText;
				a.putValue(Action.NAME, text);
				a.putValue(Action.SHORT_DESCRIPTION, text);
			}
		}

		a = RTextArea.getAction(RTextArea.REDO_ACTION);
		if (canRedo()) {
			a.setEnabled(true);
			text = getRedoPresentationName();
			a.putValue(Action.NAME, text);
			a.putValue(Action.SHORT_DESCRIPTION, text);
		}
		else {
			if (a.isEnabled()) {
				a.setEnabled(false);
				text = cantRedoText;
				a.putValue(Action.NAME, text);
				a.putValue(Action.SHORT_DESCRIPTION, text);
			}
		}

	}

	/**
	 * The edit used by {@link RUndoManager}.
	 */
	class RCompoundEdit extends CompoundEdit {

		@Override
		public String getUndoPresentationName() {
			return UIManager.getString("AbstractUndoableEdit.undoText");
		}

		@Override
		public String getRedoPresentationName() {
			return UIManager.getString("AbstractUndoableEdit.redoText");
		}

		@Override
		public boolean isInProgress() {
			return false;
		}

		@Override
		public void undo() {
			if (compoundEdit!=null) {
				compoundEdit.end();
			}
			super.undo();
			// Edits after an undo start a new compound edit, which inside an
			// atomic edit must not be in the undo list until the atomic edit ends
			compoundEdit = internalAtomicEditDepth>0 ? new RCompoundEdit() : null;
		}

	}


}
