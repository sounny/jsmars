package edu.asu.jmars.layer.profile;

import java.beans.PropertyChangeEvent;

/**
 * Model will pass events notifications to registered views via this interface
 * 
 */

public interface IProfileModelEventListener {
	void modelPropertyChange(final PropertyChangeEvent evt);
}
