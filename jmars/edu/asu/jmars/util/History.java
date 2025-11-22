package edu.asu.jmars.util;

import java.util.*;

public class History {
	public static final int DEFAULT_HISTORY_SIZE = -1;
	
	// List of List of Object
	private LinkedList<LinkedList<Change>> versions = new LinkedList<LinkedList<Change>>();
	private int version;
	private boolean busy = false;
	private int maxFrames;

	/**
	 * Creates a History log with an unbounded number of history
	 * frames.
	 */
	public History () {
		this(DEFAULT_HISTORY_SIZE);
	}
	
	/**
	 * Creates a History log with the specified maximum number of 
	 * history frames. If the maximum size is negative the log is
	 * unbounded in size.
	 * 
	 * @param maxHistoryFrames Maximum number of history frames,
	 *        negative means unbounded. 
	 */
	public History(int maxHistoryFrames){
		version = -1;
		maxFrames = maxHistoryFrames;
	}

	/**
	 * Returns true if an undo or redo operation is currently going on.
	 */
	 public boolean versionChanging () {
		 return busy;
	 }

	/**
	 * Mark the end of the current version and the start of another.
	 * Kills any forward versions. This does nothing if there are no
	 * changes since the last mark.
	 */
	public void mark () {
		if (busy)
			return;
		
		// trim any successive versions
		while (version < versions.size ()-1)
			versions.removeLast ();

		// create new bin if no current bin, or bin has at least one change
		if (version == -1 || versions.getLast().size() > 0) {
			versions.add(new LinkedList<Change>());
			if ((maxFrames >= 0) && (versions.size() > maxFrames))
				versions.removeFirst();
			else
				version ++;
		}
	}

	/**
	 * Callback used by each Versionable to add an Object to the current
	 * version's changes. If the current version is not the latest, this
	 * method will call mark() to start a new version to hold the change,
	 * disposing any newer versions.
	 * <b>CAUTION:</b> If any Versionable calling this method does so during
	 * an undo() or redo(), the change will not be logged. It is considered
	 * poor form to attempt to log changes during an undo() or redo(), but
	 * doing so is ignored silently.
	 */
	public void addChange (Versionable versioned, Object data) {
		if (busy)
			return;
		
		if (version == -1 || version < versions.size()-1)
			mark ();

		// add changes at front so forward iterators undo in the right order
		if (version > -1){
			LinkedList<Change> current = versions.get(version);
			current.addFirst(new Change(versioned, data));
		}
	}
	
	/** @return true if there are changes to be processed in a call to undo() */
	public boolean canUndo() {
		return version > 0;
	}
	
	/**
	 * Undo all changes back to the previous mark
	 */
	public void undo () {
		// can only undo when there is more to undo
		if (version <= 0)
			return;

		// undo all changes in order
		try {
			busy = true;
			for (Change change: versions.get(version)) {
				change.versionable.undo (change.data);
			}
			version --;
		} finally {
			busy = false;
		}
	}
	
	/** @return true if there are changes to be processed in a call to redo */
	public boolean canRedo() {
		return version < versions.size()-1;
	}
	
	/**
	 * Redo all changes up to the next mark
	 */
	public void redo  () {
		// can only redo when there is more to redo
		if (version >= versions.size ()-1)
			return;
		
		try {
			busy = true;
			version ++;
			LinkedList<Change> current = versions.get(version);
			// redo all changes in reverse order
			ListIterator<Change> it = current.listIterator (current.size());
			while (it.hasPrevious()) {
				Change change = it.previous ();
				change.versionable.redo (change.data);
			}
		}
		finally {
			busy = false;
		}
	}
	
	/**
	 * Programmatically frees object resources to make it ready for garbage 
	 * collection. Object should not be used after calling this method.
	 */
	public void dispose(){
		versions.clear();
	}

	private static class Change {
		public final Versionable versionable;
		public final Object data;
		public Change (Versionable versionable, Object data) {
			this.versionable = versionable;
			this.data = data;
		}
	}
}
