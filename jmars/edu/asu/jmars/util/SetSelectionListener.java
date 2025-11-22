package edu.asu.jmars.util;

import java.util.*;

/**
 ** The listener that's notified when a selection set changes.
 **
 ** @see SetSelection
 **/
public interface SetSelectionListener<E> extends EventListener
 {
	/**
	 ** Called whenever the value of the selection changes.
	 **
	 ** @param e the event that characterizes the change.
	 **/
	public void valueChanged(SetSelectionEvent<E> e);
 }
