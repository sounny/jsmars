package edu.asu.jmars.util;

import java.util.Collection;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.event.EventListenerList;


/**
 ** A simple {@link SetSelectionModel} implementation.
 **
 ** @author: Michael Weiss-Malik and James Winburn  MSFF-ASU
 **/
public class DefaultSetSelectionModel implements SetSelectionModel
{
	private static final DebugLog log = DebugLog.instance();

	//  The internal selection set. Declared as a HashSet (instead of
	//  just Set) to expose clone().
	protected HashSet values = new HashSet();

	// The indicator that the cursor is being dragged. 
	protected static boolean isAdjusting;
	
	// The list of registered listeners.
	protected static EventListenerList listenerList = new EventListenerList();


	/** required by {@link SetSelectionModel} */
	public void setValueIsAdjusting(boolean isAdjusting)
	{
		if(isAdjusting != this.isAdjusting)
		{
			this.isAdjusting = isAdjusting;
		}
	}
	
	/** required by {@link SetSelectionModel} */
	public boolean getValueIsAdjusting()
	{
		return  isAdjusting;
	}
	
	/** required by {@link SetSelectionModel} */
	public void addSetSelectionListener(SetSelectionListener l)
	{
		listenerList.add(SetSelectionListener.class, l);
	}
	
	/** required by {@link SetSelectionModel} */
	public void removeSetSelectionListener(SetSelectionListener l)
	{
		listenerList.remove(SetSelectionListener.class, l);
	}
	

	// notifies any registered listeners that a change to the internal set has occured.
	protected void fireValueChanged( final Set changes)
	{
		final EventListener [] listeners = listenerList.getListeners( SetSelectionListener.class);

			if (listeners != null){
				for(int i=0; i<listeners.length; i++) {
					SetSelectionEvent e = new SetSelectionEvent(this, changes, isAdjusting);
					((SetSelectionListener)listeners[i]).valueChanged(e);
				}
			}
	}


	// Query Operations

	public int size()
	{
		return  values.size();
	}
	
	public boolean isEmpty()
	{
		return  values.isEmpty();
	}
	
	public boolean contains(Object o)
	{
		return  values.contains(o);
	}
	
	public boolean containsAll(Collection c)
	{
		return  values.containsAll(c);
	}

	public boolean equals(Object o)
	{
		return  values.equals(o);
	}

	public Iterator iterator()
	{
		return  values.iterator();
	}
	
	public Object[] toArray()
	{
		return  values.toArray();
	}
	
	public Object[] toArray(Object a[])
	{
		return  values.toArray(a);
	}
	
	/** converts the elements of the internal set to a comma-delimited string.  
	 *  The elements are assumed to have a toString() method defined for 
	 * their class.
	 */
	public String toString()
	{
		String str = "";
		for (Iterator i = values.iterator(); i.hasNext(); ){
			str += "," + i.next();
		}
		return str;
	}



	// The following methods make simple modifications to the internal set.
	// In order to cut down on unneccesary processing, listeners are NOT 
	// notified that the set has changed when these methods are called.   
	// This means that the updateSet() method should be called at the end 
	// of any series of calls to let the listeners know that we are finally
	// finished mucking around with the set and they may now do whatever 
	// processing they see fit to do.
	
	/**
	 * adds an element to the internal set if it was not already a member.
	 *  
	 *  @return true  - the element was NOT already a member of the set and was added.
	 *          false - the element was already a member of the set. no change.
	 */
	public boolean add(Object o)
	{
		return values.add(o);
	}
	
	/**
	 * removes an element from the internal set if it was a member.
	 *  
	 *  @return true  - the element WAS a member of the set and was removed.
	 *          false - the element was NOT a member of the set. no change.
	 */
	public boolean remove(Object o)
	{
		return values.remove(o);
	}
	
	/**
	 * removes all elements from the internal set.
	 */
	public void clear()
	{
		values = new HashSet();
	}
	
	/**
	 * notifies any registered listeners that the internal set has been changed.
	 */
	public void updateSet(){
		fireValueChanged( values);
	}




	// The following methods deal with set calculus.
		
	/** 
	 *  If the inputted set is not contained in the internal set, the internal set 
	 *  is changed to the UNION ( a OR b) of the two and the listeners are notified 
	 *  that a change has occured.
	 *
	 * @return true  - Internal set changed to union and listeners notified.
	 *         false - Inputted set is contained in the internal set. No change.
	 */
	public boolean addAll(Collection c)
	{
		Set changes = new HashSet(c);
		changes.removeAll(values);
		if(changes.isEmpty()) {
			return  false;
		}
		
		values.addAll(c);
		fireValueChanged(changes);
		return  true;
	}
	
	/** 
	 *  If the inputted set is not contained in the internal set, the internal set 
	 *  is changed to the INTERSECTION (a AND b) of the two and the listeners are notified 
	 *  that a change has occured.
	 *
	 * @return true  - Internal set changed to intersection and listeners notified.
	 *         false - Inputted set is contained in the internal set. No change.
	 */
	public boolean retainAll(Collection c)
	{
		Set changes = (Set) values.clone();
		changes.removeAll(c);
		if(changes.isEmpty()){
			return  false;
		}
		
		values.retainAll(c);
		fireValueChanged(changes);
		return  true;
	}
	
	/** 
	 *  If the inputted set is contained in the internal set, the inputted set is
	 *  removed from the internal set ( A not B) and the listeners are notified 
	 *  that a change has occured.
	 *
	 * @return true  - Inputted set removed from the internal set and listeners notified.
	 *         false - Input and internal sets are disjoint. No change.
	 */
	public boolean removeAll(Collection c)
	{
		Set changes = new HashSet(c);
		changes.retainAll(values);
		if(changes.isEmpty()) {
			return  false;
		}

		values.removeAll(c);
		fireValueChanged(changes);
		return  true;
	}


	/** 
	 *  required by {@link SetSelectionModel} (but a pretty nifty method in its own right). 
	 *  If the inputted set and the internal set are not equivalent, the internal set to 
	 *  the inputted set and the listeners are notified that a change has occured. 
	 *  
	 *  @return  true  - Internal set is changed to the input set and listeners notified.
	 *           false - The internal set and the input set ARE the same. No change.
	 */
	public boolean setTo(Collection c)
	{
		// If the new objects and the curent set are the same,
		// there isn't much use in changing anything.
		if (values.equals(c)){
			return false;
		}

		// Effect the change, then fire up the listeners.
		values = new HashSet(c);
		fireValueChanged(values);
		return  true;
	}


	// hashing
	public int hashCode()
	{
		return  values.hashCode();
	}
	

} // end: DefaultSetSelectionModel.java
