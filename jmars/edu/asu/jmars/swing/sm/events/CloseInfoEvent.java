package edu.asu.jmars.swing.sm.events;

import java.util.EventObject;
import fr.lri.swingstates.sm.StateMachine;

public class CloseInfoEvent extends EventObject {
	
	private boolean isInfoDockedInsideFocusPanel = true;

	public CloseInfoEvent(StateMachine sm, boolean isInfoDetached) {
			super(sm);		
			isInfoDockedInsideFocusPanel = isInfoDetached;
	}
	
	public boolean getisInfoDockedFlag()
	{
		return isInfoDockedInsideFocusPanel;
	}
}