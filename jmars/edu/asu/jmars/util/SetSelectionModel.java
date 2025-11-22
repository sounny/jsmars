package edu.asu.jmars.util;

import java.util.Collection;
import java.util.Set;

/**
 ** This interface represents the current state of the selection,
 ** without regard to row numbers or indices. The selection is modeled
 ** as a set of object references... calls which result in changes to
 ** the Set trigger notifications to the list of registered listeners.
 * @param <T>
 **/
public interface SetSelectionModel<T> extends Set<T>
{
	/**
	 ** This property is true if upcoming changes to the set should be
	 ** considered a single event. For example if the set is being
	 ** updated in response to a user drag, the value of the
	 ** valueIsAdjusting property will be set to true when the drag is
	 ** initiated and be set to false when the drag is finished.  This
	 ** property allows listeners to update only when a change has
	 ** been finalized, rather than always handling all of the
	 ** intermediate changes.
	 ** 
	 ** @param valueIsAdjusting The new value of the property.
	 ** @see #getValueIsAdjusting
	 **/
	void setValueIsAdjusting(boolean valueIsAdjusting);
	
	/**
	 ** Returns true if the set is undergoing a series of changes.
	 ** @return true if the set is currently adjusting
	 ** @see #setValueIsAdjusting
	 **/
	boolean getValueIsAdjusting();
	
	/**
	 ** Add a listener to the list that's notified each time a change
	 ** to the selection occurs.
	 ** 
	 ** @param l the SetSelectionListener
	 ** @see #removeSetSelectionListener
	 ** @see #clearSelection
	 **/  
	void addSetSelectionListener(SetSelectionListener x);
	
	/**
	 ** Remove a listener from the list that's notified each time a
	 ** change to the selection occurs.
	 ** 
	 ** @param l the SetSelectionListener
	 ** @see #addSetSelectionListener
	 **/  
	void removeSetSelectionListener(SetSelectionListener x);
	
	/**
	 ** Convenience method for resetting the contents of the entire
	 ** Set. Atomic version of the sequence of calls:
	 **
	 ** <p><code>
	 ** clear();
	 ** addAll(c);
	 ** </code>
	 **/
	boolean setTo(Collection<T> c);
}
