package edu.asu.jmars.util;

import java.util.*;

/**
 ** An event that characterizes a change in the current selection. The
 ** change is limited to a particular set of objects.
 ** SetSelectionListeners will generally query the source of the event
 ** for the new selected status of each potentially [un]selected
 ** object.
 **/
public class SetSelectionEvent<E> extends EventObject
{
	private Set<E> changedObjects;
	private boolean isAdjusting;
	
	/**
	 ** Represents a change in selection status for the objects in the
	 ** given set.
	 **
	 ** @param changedObjects The objects whose status changed (a
	 ** shallow copy is made of the set).
	 ** @param isAdjusting An indication that this is one of a rapid
	 ** series of events.
	 **/
	public SetSelectionEvent(Object source, Set<E> changedObjects, boolean isAdjusting)
	{
		super(source);
		this.changedObjects = Collections.unmodifiableSet( new HashSet<E>(changedObjects) );
		this.isAdjusting = isAdjusting;
	}
	
	/**
	 ** Returns a list of the objects whose selection status
	 ** changed. The returned set is immutable.
	 **/
	public Set<E> getChanges()
	{
		return  changedObjects;
	}
	
	/**
	 ** Returns true if this is one of a multiple of change events.
	 **/
	public boolean getValueIsAdjusting()
	{
		return  isAdjusting;
	}
}
