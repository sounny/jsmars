package edu.asu.jmars.util;

import java.util.*;

/**
 ** An enhanced version of the {@link java.util.List} interface that
 ** includes atomic facilities for moving elements of the list
 ** around. Basically, this interface is identical to List in every
 ** regard, except for the addition of the {@link #move move} method.
 **/
public interface MovableList<E> extends List<E>
 {
	/**
	 ** Moves an element from one position in the list to another.
	 ** Has the exact same effect as the following sequence of calls:
	 **
	 ** <p><code>Object movedElement = remove(srcIndex);<br>
	 ** add(dstIndex, movedElement);</code>
	 **
	 ** <p>The net effect is that the element at position
	 ** <code>srcIndex</code> is moved to <code>dstIndex</code>, with
	 ** all intervening elements being shifted up or down to make
	 ** room.
	 **
	 ** @param srcIndex the original index of the element to move
	 ** @param dstIndex the desired new index of the element
	 **/
	public void move(int srcIndex, int dstIndex);
 }
