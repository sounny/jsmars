package edu.asu.jmars.util;

import edu.asu.jmars.util.*;

/**
 * Classes that need to participate in the versioning provided by History
 * must implement this interface. The application must call setHistory()
 * to enable the participant to send change Objects to the History log.
 * Each such change must contain the <i>transition</i> from old state to
 * new state. The undo() and redo() methods must have the transition
 * to move the implementor's state forward or backward in revision.
 */
public interface Versionable {
	void setHistory (History history);
	void undo (Object obj);
	void redo (Object obj);
}
